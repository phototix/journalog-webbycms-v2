public struct MediaDto: Sendable, Codable, Identifiable {
    public let id: String
    public let type: String
    public let url: String
    public let thumbnail: String?
    public let width: Int?
    public let height: Int?
}

public struct PollAnswerDto: Sendable, Codable, Identifiable {
    public let id: Int
    public let answer: String
    public let votesCount: Int
    public let percentage: Int

    enum CodingKeys: String, CodingKey {
        case id, answer
        case votesCount = "votes_count"
        case percentage
    }
}

public struct PollDto: Sendable, Codable, Identifiable {
    public let id: Int
    public let answers: [PollAnswerDto]?
    public let totalVotes: Int

    enum CodingKeys: String, CodingKey {
        case id, answers
        case totalVotes = "total_votes"
    }
}

public struct PostGiftDto: Sendable, Codable, Identifiable {
    public var id: Int { giftId }
    public let giftId: Int
    public let count: Int
    public let gift: GiftDto?

    enum CodingKeys: String, CodingKey {
        case giftId = "gift_id"
        case count, gift
    }
}

public struct PostDto: Sendable, Codable, Identifiable {
    public let id: Int
    public let text: String?
    public let price: Double
    public let status: Int?
    public let isPinned: Bool?
    public let releaseDate: String?
    public let expireDate: String?
    public let createdAt: String?
    public let user: UserBriefDto?
    public let media: [MediaDto]?
    public let poll: PollDto?
    public let gifts: [PostGiftDto]?
    public let giftsCount: Int?
    public let likesCount: Int
    public let commentsCount: Int
    public let hasLiked: Bool
    public let hasBookmarked: Bool?

    enum CodingKeys: String, CodingKey {
        case id, text, price, status, user, media, poll, gifts
        case isPinned = "is_pinned"
        case releaseDate = "release_date"
        case expireDate = "expire_date"
        case createdAt = "created_at"
        case giftsCount = "gifts_count"
        case likesCount = "likes_count"
        case commentsCount = "comments_count"
        case hasLiked = "has_liked"
        case hasBookmarked = "has_bookmarked"
    }
}

public struct FeedData: Sendable, Codable {
    public let posts: [PostDto]
    public let hasMore: Bool
    public let nextPage: Int?

    enum CodingKeys: String, CodingKey {
        case posts
        case hasMore = "has_more"
        case nextPage = "next_page"
    }
}

public struct ExploreData: Sendable, Codable {
    public let posts: [PostDto]
    public let hasMore: Bool
    public let nextPage: Int?

    enum CodingKeys: String, CodingKey {
        case posts
        case hasMore = "has_more"
        case nextPage = "next_page"
    }
}

public struct ExploreUserDto: Sendable, Codable, Identifiable {
    public let id: Int
    public let name: String
    public let username: String
    public let avatar: String?
    public let bio: String?
    public let lastPostedAt: String?

    enum CodingKeys: String, CodingKey {
        case id, name, username, avatar, bio
        case lastPostedAt = "last_posted_at"
    }
}

public struct ExploreUsersData: Sendable, Codable {
    public let users: [ExploreUserDto]
    public let hasMore: Bool
    public let nextPage: Int?

    enum CodingKeys: String, CodingKey {
        case users
        case hasMore = "has_more"
        case nextPage = "next_page"
    }
}

public struct CommentDto: Sendable, Codable, Identifiable {
    public let id: Int
    public let text: String
    public let createdAt: String?
    public let user: UserBriefDto

    enum CodingKeys: String, CodingKey {
        case id, text, user
        case createdAt = "created_at"
    }
}

public struct CommentsResponse: Sendable, Codable {
    public let comments: PaginatedComments
}

public struct PaginatedComments: Sendable, Codable {
    public let data: [CommentDto]
}
