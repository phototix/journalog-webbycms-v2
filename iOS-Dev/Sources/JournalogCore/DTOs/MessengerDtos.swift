public struct ConversationDto: Sendable, Codable, Identifiable {
    public var id: Int { contactId }
    public let contactId: Int
    public let name: String
    public let avatar: String
    public let lastMessage: String?
    public let lastMessageDate: String?
    public let unreadCount: Int

    enum CodingKeys: String, CodingKey {
        case contactId = "contact_id"
        case name, avatar
        case lastMessage = "last_message"
        case lastMessageDate = "last_message_date"
        case unreadCount = "unread_count"
    }
}

public struct MessageDto: Sendable, Codable, Identifiable {
    public let id: Int
    public let text: String?
    public let senderId: Int
    public let receiverId: Int
    public let isMine: Bool
    public let isSeen: Bool?
    public let price: Double?
    public let createdAt: String?
    public let attachments: [MediaDto]?

    enum CodingKeys: String, CodingKey {
        case id, text, price, attachments
        case senderId = "sender_id"
        case receiverId = "receiver_id"
        case isMine = "is_mine"
        case isSeen = "is_seen"
        case createdAt = "created_at"
    }
}

public struct PaginatedMessages: Sendable, Codable {
    public let data: [MessageDto]
}
