import JournalogCore
import SwiftUI

public struct ExploreScreen: View {
    @State private var users: [ExploreUserDto] = []
    @State private var posts: [PostDto] = []
    @State private var isLoading = true
    @State private var hasMore = true
    @State private var currentPage = 1
    @State private var searchText = ""

    let onPostClick: (Int) -> Void
    let onProfileClick: (String) -> Void

    private let exploreService = ExploreService()
    private let searchService = SearchService()

    public var body: some View {
        NavigationStack {
            ScrollView {
                if !searchText.isEmpty {
                    searchResultsView
                } else {
                    VStack(alignment: .leading, spacing: 16) {
                        Text("Discover Creators")
                            .font(.title2)
                            .fontWeight(.bold)
                            .padding(.horizontal)

                        LazyVGrid(columns: [
                            GridItem(.flexible()),
                            GridItem(.flexible()),
                            GridItem(.flexible()),
                        ], spacing: 12) {
                            ForEach(users) { user in
                                VStack(spacing: 8) {
                                    AsyncImage(url: URL(string: user.avatar ?? "")) { phase in
                                        switch phase {
                                        case .success(let image):
                                            image.resizable().scaledToFill()
                                        default:
                                            Image(systemName: "person.circle.fill")
                                                .font(.system(size: 40))
                                                .foregroundColor(.secondary)
                                        }
                                    }
                                    .frame(width: 80, height: 80)
                                    .clipShape(Circle())

                                    Text(user.username)
                                        .font(.caption)
                                        .fontWeight(.medium)
                                        .lineLimit(1)

                                    if let bio = user.bio {
                                        Text(bio)
                                            .font(.caption2)
                                            .foregroundColor(.secondary)
                                            .lineLimit(2)
                                    }
                                }
                                .onTapGesture {
                                    onProfileClick(user.username)
                                }
                            }
                        }
                        .padding(.horizontal)

                        if isLoading {
                            ProgressView()
                                .frame(maxWidth: .infinity)
                                .padding()
                        }
                    }
                }
            }
            .navigationTitle("Explore")
            .navigationBarTitleDisplayMode(.inline)
            .searchable(text: $searchText, prompt: "Search users & posts")
            .task {
                await loadUsers()
            }
            .onChange(of: searchText) { _, newValue in
                guard !newValue.isEmpty else { return }
                Task {
                    let _ = try? await searchService.search(query: newValue)
                }
            }
        }
    }

    private var searchResultsView: some View {
        VStack {
            Text("Search results for \"\(searchText)\"")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .padding()
            Spacer()
        }
    }

    private func loadUsers() async {
        isLoading = true
        do {
            let data = try await exploreService.getUsers(page: currentPage)
            users = data.users
            hasMore = data.hasMore
            currentPage = data.nextPage ?? currentPage
        } catch {
            print("Explore error: \(error)")
        }
        isLoading = false
    }
}
