import SwiftUI

struct ContentView: View {
    @EnvironmentObject var sessionManager: SessionManager
    @EnvironmentObject var coordinator: NavigationCoordinator
    @State private var isCheckingSession = true
    @State private var isLoggedIn = false

    var body: some View {
        Group {
            if isCheckingSession {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if isLoggedIn {
                NavigationStack(path: $coordinator.path) {
                    MainTabView()
                        .environmentObject(sessionManager)
                        .navigationDestination(for: PostDestination.self) { dest in
                            switch dest {
                            case .post(let id):
                                PostDetailScreen(postId: id, onBack: { coordinator.path.removeLast() })
                            case .profile(let username):
                                ProfileScreen(
                                    username: username,
                                    currentUsername: sessionManager.currentUser?.username ?? "",
                                    onBack: { coordinator.path.removeLast() },
                                    onSettingsClick: { /* navigate to settings */ },
                                    onNotificationsClick: { /* navigate to notifications */ },
                                    onPostClick: { postId in coordinator.navigateToPost(postId) },
                                    onSubscribeClick: { user in coordinator.navigateToSubscription(user: user) }
                                )
                            case .conversation(let userId, let userName):
                                ConversationScreen(
                                    userId: userId,
                                    userName: userName,
                                    onBack: { coordinator.path.removeLast() }
                                )
                            }
                        }
                        .fullScreenCover(isPresented: $coordinator.showStoryViewer) {
                            if let userId = coordinator.storyViewerUserId {
                                StoryViewerScreen(
                                    userId: userId,
                                    onBack: {
                                        coordinator.showStoryViewer = false
                                        coordinator.storyViewerUserId = nil
                                    }
                                )
                            }
                        }
                        .sheet(isPresented: $coordinator.showSubscription) {
                            if let user = coordinator.subscriptionUser {
                                SubscriptionScreen(
                                    creator: user,
                                    onBack: { coordinator.showSubscription = false },
                                    onSubscribed: { coordinator.showSubscription = false }
                                )
                            }
                        }
                }
            } else {
                AuthScreen(onLoggedIn: {
                    isLoggedIn = true
                })
            }
        }
        .task {
            await sessionManager.restoreSession()
            isLoggedIn = sessionManager.isLoggedIn
            isCheckingSession = false
        }
    }
}
