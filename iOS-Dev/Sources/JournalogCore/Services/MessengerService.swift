import Foundation

public struct MessengerService: Sendable {
    private let client = APIClient.shared

    public init() {}


    public func getConversations() async throws -> [ConversationDto] {
        let response: ApiResponse<[String: [ConversationDto]]> = try await client.request("/conversations")
        guard let data = response.data, let conversations = data["conversations"] else {
            throw APIError.unknown("Failed to load conversations")
        }
        return conversations
    }

    public func getMessages(userId: Int) async throws -> [MessageDto] {
        let response: ApiResponse<[String: PaginatedMessages]> = try await client.request("/conversations/\(userId)/messages")
        guard let data = response.data, let messages = data["data"] else {
            throw APIError.unknown("Failed to load messages")
        }
        return messages.data
    }

    public func sendMessage(receiverId: Int, text: String, price: Double? = nil) async throws -> MessageDto {
        var body: [String: Any] = [
            "receiver_id": receiverId,
            "text": text,
        ]
        if let price {
            body["price"] = price
        }
        let bodyCodable = AnyCodable(body)
        let response: ApiResponse<[String: MessageDto]> = try await client.request(
            "/messages", method: "POST", body: bodyCodable
        )
        guard let data = response.data, let message = data["message"] else {
            throw APIError.unknown("Failed to send message")
        }
        return message
    }
}
