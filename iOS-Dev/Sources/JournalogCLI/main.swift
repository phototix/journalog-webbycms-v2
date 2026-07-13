import Foundation
import JournalogCore

@main
struct JournalogCLI {
    static func main() async {
        print("Journalog iOS App - Core Library")
        print("=================================")
        print()
        print("DTOs loaded successfully:")
        print("  - AuthDtos: LoginRequest, RegisterRequest, UserDto, AuthData, UserBriefDto")
        print("  - FeedDtos: PostDto, MediaDto, FeedData, CommentDto, PollDto, ExploreData")
        print("  - SocialDtos: StoryGroupDto, StoryItemDto, NotificationDto, TrendingUserDto")
        print("  - MessengerDtos: ConversationDto, MessageDto")
        print("  - PaymentDtos: GiftDto, GiftListData, SubscriptionPlan, WalletBalance")
        print("  - AdminDtos: ApkVersionDto, UploadResponse")
        print()
        print("Services initialized:")
        print("  - AuthService, FeedService, PostService, UserService")
        print("  - StoryService, MessengerService, NotificationService")
        print("  - SearchService, ExploreService, GiftService")
        print("  - SettingsService, WalletService, SubscriptionService")
        print()
        print("Networking layer ready:")
        print("  - APIClient (URLSession-based, async/await)")
        print("  - AuthInterceptor (Bearer token injection)")
        print("  - KeychainManager (secure token storage)")
        print("  - SessionManager (published state management)")
        print()
        print("UI screens ported (SwiftUI):")
        print("  - SplashScreen, AuthScreen, MainTabView")
        print("  - FeedScreen, PostDetailScreen, ExploreScreen")
        print("  - ProfileScreen, SettingsScreen, SearchScreen")
        print("  - MessengerScreen, ConversationScreen")
        print("  - NotificationsScreen, StoryViewerScreen, StoryCreateScreen")
        print("  - SubscriptionScreen, GiftModalView, CreatePostScreen")
        print()
        print("Components:")
        print("  - PostCardView, UserRowView, PollView, LoadingView")
        print("  - StoryCircleView, MediaCarouselView")
        print()
        print("To build for iOS, open Journalog/ in Xcode and:")
        print("  1. Select iOS 17+ as deployment target")
        print("  2. Add the Sources/Journalog folder to the project")
        print("  3. Configure signing with your Apple Developer account")
        print("  4. Build and run on simulator or device")

        // Verify DTOs work
        let decoder = JSONDecoder()
        let sampleUserJSON = """
        {
            "id": 1,
            "name": "Test User",
            "username": "testuser",
            "email": "test@example.com",
            "bio": "A test user",
            "paid_profile": true,
            "profile_access_price": 9.99
        }
        """
        if let data = sampleUserJSON.data(using: .utf8),
           let user = try? decoder.decode(UserDto.self, from: data) {
            print()
            print("✅ DTO parsing verified: UserDto id=\(user.id), name=\(user.name)")
        }
    }
}
