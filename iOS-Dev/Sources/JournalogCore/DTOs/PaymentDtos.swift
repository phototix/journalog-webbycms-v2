public struct GiftDto: Sendable, Codable, Identifiable {
    public let id: Int
    public let name: String
    public let icon: String
    public let gifEffect: String?
    public let credits: Int
    public let category: String
    public let sortOrder: Int

    enum CodingKeys: String, CodingKey {
        case id, name, icon, credits, category
        case gifEffect = "gif_effect"
        case sortOrder = "sort_order"
    }
}

public struct GiftListData: Sendable, Codable {
    public let gifts: [String: [GiftDto]]
    public let balance: Double
}

public struct SendGiftResponse: Sendable, Codable {
    public let balance: Double
    public let postGifts: [PostGiftDto]?
    public let gift: GiftDto?

    enum CodingKeys: String, CodingKey {
        case balance, gift
        case postGifts = "post_gifts"
    }
}

public struct GiftStatsData: Sendable, Codable {
    public let gifts: [PostGiftDto]?
    public let totalGifts: Int
    public let totalCredits: Int

    enum CodingKeys: String, CodingKey {
        case gifts
        case totalGifts = "total_gifts"
        case totalCredits = "total_credits"
    }
}

public struct SubscriptionPlan: Sendable, Codable {
    public let price: Double
    public let price3Months: Double
    public let price6Months: Double
    public let price12Months: Double
    public let hasSubscribed: Bool

    enum CodingKeys: String, CodingKey {
        case price
        case price3Months = "price_3_months"
        case price6Months = "price_6_months"
        case price12Months = "price_12_months"
        case hasSubscribed = "has_subscribed"
    }
}

public struct SubscribeRequest: Sendable, Codable {
    public let recipientUserId: Int
    public let plan: String

    enum CodingKeys: String, CodingKey {
        case recipientUserId = "recipient_user_id"
        case plan
    }

    public init(recipientUserId: Int, plan: String) {
        self.recipientUserId = recipientUserId
        self.plan = plan
    }
}

public struct SubscriptionData: Sendable, Codable {
    public let id: Int?
    public let status: String?
    public let expiresAt: String?

    enum CodingKeys: String, CodingKey {
        case id, status
        case expiresAt = "expires_at"
    }
}

public struct WalletBalance: Sendable, Codable {
    public let total: Double
    public let pendingBalance: Double?

    enum CodingKeys: String, CodingKey {
        case total
        case pendingBalance = "pendingBalance"
    }
}
