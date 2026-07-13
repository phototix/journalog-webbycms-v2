import JournalogCore
import SwiftUI

public struct PostDetailScreen: View {
    let postId: Int
    let onBack: () -> Void

    @State private var post: PostDto?
    @State private var comments: [CommentDto] = []
    @State private var newCommentText = ""
    @State private var isLoading = true
    @State private var errorMessage: String?

    private let postService = PostService()

    public var body: some View {
        Group {
            if isLoading {
                ProgressView("Loading post...")
            } else if let errorMessage {
                VStack(spacing: 12) {
                    Text("Error")
                        .font(.headline)
                    Text(errorMessage)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Button("Try Again") {
                        Task { await loadPost() }
                    }
                    .buttonStyle(.bordered)
                }
            } else if let post {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        if let user = post.user {
                            HStack(spacing: 12) {
                                AsyncImage(url: URL(string: user.avatar ?? "")) { phase in
                                    switch phase {
                                    case .success(let image):
                                        image.resizable().scaledToFill()
                                    default:
                                        Image(systemName: "person.circle.fill")
                                            .font(.title2)
                                    }
                                }
                                .frame(width: 40, height: 40)
                                .clipShape(Circle())

                                VStack(alignment: .leading, spacing: 2) {
                                    Text(user.name)
                                        .fontWeight(.semibold)
                                    Text(user.username)
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                            }
                        }

                        if let media = post.media, !media.isEmpty {
                            TabView {
                                ForEach(media) { item in
                                    AsyncImage(url: URL(string: item.url)) { phase in
                                        switch phase {
                                        case .success(let image):
                                            image.resizable().scaledToFit()
                                        case .failure:
                                            Image(systemName: "photo")
                                                .font(.largeTitle)
                                                .foregroundColor(.secondary)
                                        case .empty:
                                            ProgressView()
                                        @unknown default:
                                            Color.clear
                                        }
                                    }
                                }
                            }
                            .frame(height: 300)
                            .tabViewStyle(.page)
                        }

                        if let text = post.text {
                            Text(text)
                                .font(.body)
                        }

                        if let poll = post.poll {
                            PollView(poll: poll, onVote: { answerId in
                                Task {
                                    try? await postService.votePoll(postId: post.id, answerId: answerId)
                                }
                            })
                        }

                        HStack(spacing: 24) {
                            Button(action: { Task { try? await postService.toggleLike(id: post.id) } }) {
                                Label("\(post.likesCount)", systemImage: post.hasLiked ? "heart.fill" : "heart")
                                    .foregroundColor(post.hasLiked ? .red : .primary)
                            }

                            Label("\(post.commentsCount)", systemImage: "bubble.right")
                                .foregroundColor(.primary)
                        }
                        .font(.subheadline)

                        Divider()

                        Text("Comments")
                            .font(.headline)

                        ForEach(comments) { comment in
                            VStack(alignment: .leading, spacing: 4) {
                                HStack(spacing: 8) {
                                    AsyncImage(url: URL(string: comment.user.avatar ?? "")) { phase in
                                        if case .success(let image) = phase {
                                            image.resizable().scaledToFill()
                                        } else {
                                            Image(systemName: "person.circle.fill")
                                        }
                                    }
                                    .frame(width: 28, height: 28)
                                    .clipShape(Circle())

                                    Text(comment.user.name)
                                        .fontWeight(.semibold)
                                        .font(.caption)

                                    Spacer()

                                    if let date = comment.createdAt {
                                        Text(date)
                                            .font(.caption2)
                                            .foregroundColor(.secondary)
                                    }
                                }
                                Text(comment.text)
                                    .font(.subheadline)
                            }
                            .padding(.vertical, 4)
                        }

                        HStack(spacing: 12) {
                            TextField("Add a comment...", text: $newCommentText)
                                .textFieldStyle(.roundedBorder)

                            Button("Post") {
                                guard !newCommentText.trimmingCharacters(in: .whitespaces).isEmpty else { return }
                                Task {
                                    let comment = try? await postService.addComment(
                                        postId: post.id, text: newCommentText
                                    )
                                    if let comment {
                                        comments.append(comment)
                                        newCommentText = ""
                                    }
                                }
                            }
                            .disabled(newCommentText.trimmingCharacters(in: .whitespaces).isEmpty)
                        }
                    }
                    .padding()
                }
            }
        }
        .navigationTitle("Post")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                }
            }
        }
        .task {
            await loadPost()
        }
    }

    private func loadPost() async {
        isLoading = true
        errorMessage = nil
        do {
            post = try await postService.getPost(id: postId)
            comments = try await postService.getComments(postId: postId)
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }
}
