import JournalogCore
import SwiftUI

public struct FeedScreen: View {
    @State private var posts: [PostDto] = []
    @State private var isLoading = false
    @State private var hasMore = true
    @State private var currentPage = 1

    let onPostClick: (Int) -> Void
    let onProfileClick: (String) -> Void
    let onStoryClick: (Int) -> Void
    let refreshTrigger: Int

    private let feedService = FeedService()

    public var body: some View {
        NavigationStack {
            List {
                if posts.isEmpty && !isLoading {
                    VStack(spacing: 16) {
                        Image(systemName: "newspaper")
                            .font(.system(size: 48))
                            .foregroundColor(.secondary)
                        Text("No posts yet")
                            .font(.headline)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 60)
                    .listRowSeparator(.hidden)
                }

                ForEach(posts) { post in
                    PostCardView(post: post)
                        .onTapGesture {
                            onPostClick(post.id)
                        }
                        .listRowSeparator(.hidden)
                        .listRowInsets(EdgeInsets())
                        .onAppear {
                            if post == posts.last && hasMore && !isLoading {
                                loadMore()
                            }
                        }
                }

                if isLoading {
                    HStack {
                        Spacer()
                        ProgressView()
                        Spacer()
                    }
                    .listRowSeparator(.hidden)
                }
            }
            .listStyle(.plain)
            .refreshable {
                await refresh()
            }
            .navigationTitle("Feed")
            .navigationBarTitleDisplayMode(.inline)
        }
        .task {
            await loadFeed()
        }
        .onChange(of: refreshTrigger) { _, _ in
            Task { await refresh() }
        }
    }

    private func loadFeed() async {
        isLoading = true
        defer { isLoading = false }

        do {
            let data = try await feedService.getFeed(page: currentPage)
            posts = data.posts
            hasMore = data.hasMore
            currentPage = data.nextPage ?? 1
        } catch {
            print("Feed error: \(error)")
        }
    }

    private func refresh() async {
        currentPage = 1
        await loadFeed()
    }

    private func loadMore() {
        Task {
            isLoading = true
            do {
                let data = try await feedService.getFeed(page: currentPage)
                posts.append(contentsOf: data.posts)
                hasMore = data.hasMore
                currentPage = data.nextPage ?? currentPage
            } catch {
                print("Feed load more error: \(error)")
            }
            isLoading = false
        }
    }
}
