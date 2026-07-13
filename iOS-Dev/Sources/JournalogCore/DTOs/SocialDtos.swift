public struct TrendingUserDto: Sendable, Codable, Identifiable {
    public let id: Int
    public let name: String
    public let username: String
    public let avatar: String?
    public let bio: String?
    public let subscribersCount: Int

    enum CodingKeys: String, CodingKey {
        case id, name, username, avatar, bio
        case subscribersCount = "subscribers_count"
    }
}

public struct SearchResultDto: Sendable, Codable, Identifiable {
    public let id: Int
    public let name: String?
    public let username: String?
    public let avatar: String?
    public let bio: String?
    public let text: String?
    public let thumbnail: String?
    public let type: String
}

public struct StoryGroupDto: Sendable, Codable, Identifiable {
    public var id: String { "\(user.id)" }
    public let user: UserBriefDto
    public let hasUnseen: Bool
    public let stories: [StoryItemDto]

    enum CodingKeys: String, CodingKey {
        case user, stories
        case hasUnseen = "has_unseen"
    }
}

public struct StoryItemDto: Sendable, Codable, Identifiable {
    public let id: String
    public let type: String
    public let url: String
    public let thumbnail: String?
    public let text: String?
    public let length: Int?
    public let overlay: [String: Double]?
    public let bgPreset: String?
    public let rawTime: Int?
    public let createdAt: String?

    enum CodingKeys: String, CodingKey {
        case id, type, url, thumbnail, text, length, overlay
        case bgPreset = "bg_preset"
        case rawTime, createdAt = "created_at"
    }
}

public struct NotificationDto: Sendable, Codable, Identifiable {
    public let id: String
    public let type: String
    public let message: String?
    public let read: Bool
    public let createdAt: String?
    public let actor: UserBriefDto?

    enum CodingKeys: String, CodingKey {
        case id, type, message, read, actor
        case createdAt = "created_at"
    }
}

public struct PaginatedNotifications: Sendable, Codable {
    public let data: [NotificationDto]
}
