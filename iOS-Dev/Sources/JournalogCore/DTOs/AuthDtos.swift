public struct LoginRequest: Sendable, Codable {
    public let email: String
    public let password: String

    public init(email: String, password: String) {
        self.email = email
        self.password = password
    }
}

public struct RegisterRequest: Sendable, Codable {
    public let name: String
    public let username: String
    public let email: String
    public let password: String
    public let birthdate: String

    public init(name: String, username: String, email: String, password: String, birthdate: String) {
        self.name = name
        self.username = username
        self.email = email
        self.password = password
        self.birthdate = birthdate
    }
}

public struct UserDto: Sendable, Codable, Identifiable, Hashable {
    public let id: Int
    public let name: String
    public let username: String
    public let email: String?
    public let bio: String?
    public let avatar: String?
    public let cover: String?
    public let location: String?
    public let website: String?
    public let paidProfile: Bool
    public let profileAccessPrice: Double
    public let profileAccessPrice3Months: Double?
    public let profileAccessPrice6Months: Double?
    public let profileAccessPrice12Months: Double?
    public let publicProfile: Bool
    public let openProfile: Bool
    public let emailVerifiedAt: String?
    public let createdAt: String?
    public let postsCount: Int?
    public let subscribersCount: Int?
    public let isOnline: Bool?
    public let isVerified: Bool?
    public let hasSubscribed: Bool?
    public let hasChatAccess: Bool?
    public let roleId: Int?
    public let isFollowing: Bool?
    public let giftsReceivedCount: Int?
    public let giftsReceivedCredits: Int?

    enum CodingKeys: String, CodingKey {
        case id, name, username, email, bio, avatar, cover, location, website
        case paidProfile = "paid_profile"
        case profileAccessPrice = "profile_access_price"
        case profileAccessPrice3Months = "profile_access_price_3_months"
        case profileAccessPrice6Months = "profile_access_price_6_months"
        case profileAccessPrice12Months = "profile_access_price_12_months"
        case publicProfile = "public_profile"
        case openProfile = "open_profile"
        case emailVerifiedAt = "email_verified_at"
        case createdAt = "created_at"
        case postsCount = "posts_count"
        case subscribersCount = "subscribers_count"
        case isOnline = "is_online"
        case isVerified = "is_verified"
        case hasSubscribed = "has_subscribed"
        case hasChatAccess = "has_chat_access"
        case roleId = "role_id"
        case isFollowing = "is_following"
        case giftsReceivedCount = "gifts_received_count"
        case giftsReceivedCredits = "gifts_received_credits"
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }

    public static func == (lhs: UserDto, rhs: UserDto) -> Bool {
        lhs.id == rhs.id
    }
}

public struct AuthData: Sendable, Codable {
    public let user: UserDto
    public let token: String
}

public struct UserBriefDto: Sendable, Codable, Identifiable, Hashable {
    public let id: Int
    public let name: String
    public let username: String
    public let avatar: String?

    public func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }

    public static func == (lhs: UserBriefDto, rhs: UserBriefDto) -> Bool {
        lhs.id == rhs.id
    }
}
