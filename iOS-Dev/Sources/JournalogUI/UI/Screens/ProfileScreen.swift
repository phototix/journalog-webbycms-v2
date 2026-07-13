import JournalogCore
import SwiftUI

public struct ProfileScreen: View {
    let username: String
    let currentUsername: String
    let onBack: () -> Void
    let onSettingsClick: () -> Void
    let onNotificationsClick: () -> Void
    let onPostClick: (Int) -> Void
    let onSubscribeClick: (UserDto) -> Void

    @State private var user: UserDto?
    @State private var posts: [PostDto] = []
    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var selectedSegment = 0

    private let userService = UserService()

    private var isOwnProfile: Bool {
        username == currentUsername
    }

    public var body: some View {
        Group {
            if isLoading {
                ProgressView("Loading profile...")
            } else if let errorMessage {
                VStack(spacing: 12) {
                    Text("Error")
                        .font(.headline)
                    Text(errorMessage)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Button("Try Again") {
                        Task { await loadProfile() }
                    }
                    .buttonStyle(.bordered)
                }
            } else if let user {
                ScrollView {
                    VStack(spacing: 16) {
                        AsyncImage(url: URL(string: user.avatar ?? "")) { phase in
                            switch phase {
                            case .success(let image):
                                image.resizable().scaledToFill()
                            default:
                                Image(systemName: "person.circle.fill")
                                    .font(.system(size: 80))
                                    .foregroundColor(.secondary)
                            }
                        }
                        .frame(width: 120, height: 120)
                        .clipShape(Circle())
                        .overlay(Circle().stroke(AppTheme.storyGradient, lineWidth: 3))

                        Text(user.name)
                            .font(.title2)
                            .fontWeight(.bold)

                        Text("@\(user.username)")
                            .font(.subheadline)
                            .foregroundColor(.secondary)

                        if let bio = user.bio {
                            Text(bio)
                                .font(.body)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal)
                        }

                        if let location = user.location {
                            Label(location, systemImage: "location.fill")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }

                        HStack(spacing: 32) {
                            VStack {
                                Text("\(user.postsCount ?? 0)")
                                    .fontWeight(.bold)
                                Text("Posts")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            VStack {
                                Text("\(user.subscribersCount ?? 0)")
                                    .fontWeight(.bold)
                                Text("Subscribers")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                        .padding(.vertical, 8)

                        if isOwnProfile {
                            HStack(spacing: 16) {
                                Button("Settings") { onSettingsClick() }
                                    .buttonStyle(.bordered)
                                Button("Notifications") { onNotificationsClick() }
                                    .buttonStyle(.bordered)
                            }
                        } else if !(user.hasSubscribed ?? false) {
                            Button("Subscribe") { onSubscribeClick(user) }
                                .buttonStyle(.borderedProminent)
                                .tint(AppTheme.instagramPink)
                        }

                        Divider()

                        Picker("", selection: $selectedSegment) {
                            Text("Posts").tag(0)
                            Text("Liked").tag(1)
                        }
                        .pickerStyle(.segmented)
                        .padding(.horizontal)

                        LazyVGrid(columns: [
                            GridItem(.flexible()),
                            GridItem(.flexible()),
                            GridItem(.flexible()),
                        ], spacing: 2) {
                            ForEach(posts) { post in
                                if let firstMedia = post.media?.first {
                                    AsyncImage(url: URL(string: firstMedia.thumbnail ?? firstMedia.url)) { phase in
                                        switch phase {
                                        case .success(let image):
                                            image.resizable().scaledToFill()
                                        default:
                                            Color.gray.opacity(0.2)
                                        }
                                    }
                                    .aspectRatio(1, contentMode: .fill)
                                    .clipped()
                                    .onTapGesture {
                                        onPostClick(post.id)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle(username)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if !isOwnProfile {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                    }
                }
            }
        }
        .task {
            await loadProfile()
        }
    }

    private func loadProfile() async {
        isLoading = true
        errorMessage = nil
        do {
            user = try await userService.getProfile(username: username)
            let feedData = try await userService.getUserPosts(username: username)
            posts = feedData.posts
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }
}
