import JournalogCore
import SwiftUI

public struct MainTabView: View {
    @State private var selectedTab = 0
    @State private var feedRefreshTrigger = 0
    @EnvironmentObject var sessionManager: SessionManager

    public var body: some View {
        TabView(selection: $selectedTab) {
            FeedScreen(
                onPostClick: { _ in },
                onProfileClick: { _ in },
                onStoryClick: { _ in },
                refreshTrigger: feedRefreshTrigger
            )
            .tabItem {
                Label("Feed", systemImage: selectedTab == 0 ? "house.fill" : "house")
            }
            .tag(0)

            ExploreScreen(
                onPostClick: { _ in },
                onProfileClick: { _ in }
            )
            .tabItem {
                Label("Explore", systemImage: selectedTab == 1 ? "magnifyingglass" : "magnifyingglass")
            }
            .tag(1)

            CreatePostScreen(onPostCreated: {
                feedRefreshTrigger += 1
            })
            .tabItem {
                Label("Create", systemImage: "plus.square")
            }
            .tag(2)

            MessengerScreen(onConversationClick: { _, _ in })
                .tabItem {
                    Label("Messages", systemImage: selectedTab == 3 ? "message.fill" : "message")
                }
                .tag(3)

            ProfileScreen(
                username: sessionManager.currentUser?.username ?? "",
                currentUsername: sessionManager.currentUser?.username ?? "",
                onBack: {},
                onSettingsClick: {},
                onNotificationsClick: {},
                onPostClick: { _ in },
                onSubscribeClick: { _ in }
            )
            .tabItem {
                Label("Profile", systemImage: selectedTab == 4 ? "person.fill" : "person")
            }
            .tag(4)
        }
        .tint(AppTheme.instagramPink)
    }
}
