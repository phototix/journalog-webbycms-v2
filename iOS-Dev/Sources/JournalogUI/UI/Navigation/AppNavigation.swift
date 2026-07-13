import JournalogCore
import SwiftUI

@MainActor
public final class NavigationCoordinator: ObservableObject {
    @Published public var path = NavigationPath()
    @Published public var showStoryViewer = false
    @Published public var storyViewerUserId: Int?
    @Published public var showSubscription = false
    @Published public var subscriptionUser: UserDto?

    public static let shared = NavigationCoordinator()

    private init() {}

    public func navigateToPost(_ id: Int) {
        path.append(PostDestination.post(id))
    }

    public func navigateToProfile(_ username: String) {
        path.append(PostDestination.profile(username))
    }

    public func navigateToConversation(userId: Int, userName: String) {
        path.append(PostDestination.conversation(userId, userName))
    }

    public func navigateToStory(userId: Int) {
        storyViewerUserId = userId
        showStoryViewer = true
    }

    public func navigateToSubscription(user: UserDto) {
        subscriptionUser = user
        showSubscription = true
    }
}

public enum PostDestination: Hashable {
    case post(Int)
    case profile(String)
    case conversation(Int, String)
}

public struct AppNavigationRoot<Content: View>: View {
    @ViewBuilder let content: Content

    public var body: some View {
        content
    }
}
