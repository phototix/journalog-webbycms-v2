import JournalogCore
import SwiftUI

public struct PostCardView: View {
    let post: PostDto

    public var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if let user = post.user {
                HStack(spacing: 10) {
                    AsyncImage(url: URL(string: user.avatar ?? "")) { phase in
                        switch phase {
                        case .success(let image):
                            image.resizable().scaledToFill()
                        default:
                            Image(systemName: "person.circle.fill")
                                .font(.title2)
                                .foregroundColor(.secondary)
                        }
                    }
                    .frame(width: 36, height: 36)
                    .clipShape(Circle())

                    VStack(alignment: .leading, spacing: 1) {
                        Text(user.name)
                            .fontWeight(.semibold)
                            .font(.subheadline)
                        if let date = post.createdAt {
                            Text(date)
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                    }

                    Spacer()

                    if post.price > 0 {
                        Text("$\(String(format: "%.2f", post.price))")
                            .font(.caption)
                            .fontWeight(.bold)
                            .foregroundColor(AppTheme.instagramPink)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(AppTheme.instagramPink.opacity(0.1))
                            .cornerRadius(6)
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
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
                .frame(height: 350)
                .tabViewStyle(.page)
            }

            if let text = post.text {
                Text(text)
                    .font(.body)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
            }

            if let poll = post.poll {
                PollView(poll: poll, onVote: { _ in })
                    .padding(.horizontal, 12)
                    .padding(.bottom, 8)
            }

            HStack(spacing: 24) {
                Label("\(post.likesCount)", systemImage: post.hasLiked ? "heart.fill" : "heart")
                    .foregroundColor(post.hasLiked ? .red : .primary)

                Label("\(post.commentsCount)", systemImage: "bubble.right")

                if post.hasBookmarked == true {
                    Image(systemName: "bookmark.fill")
                        .foregroundColor(AppTheme.instagramPink)
                }

                Spacer()

                if let giftsCount = post.giftsCount, giftsCount > 0 {
                    Label("\(giftsCount)", systemImage: "gift")
                        .foregroundColor(.secondary)
                }
            }
            .font(.subheadline)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
        }
        .background(Color(.systemBackground))
    }
}
