import Foundation

public struct GiftService: Sendable {
    private let client = APIClient.shared

    public init() {}


    public func getGifts() async throws -> GiftListData {
        let response: ApiResponse<GiftListData> = try await client.request("/gifts")
        guard let data = response.data else {
            throw APIError.unknown("Failed to load gifts")
        }
        return data
    }

    public func sendGift(giftId: Int, postId: Int) async throws -> SendGiftResponse {
        let body = ["gift_id": giftId, "post_id": postId]
        let response: ApiResponse<SendGiftResponse> = try await client.request(
            "/gifts/send", method: "POST", body: AnyCodable(body)
        )
        guard let data = response.data else {
            throw APIError.unknown("Failed to send gift")
        }
        return data
    }

    public func getPostGifts(postId: Int) async throws -> [PostGiftDto] {
        let response: ApiResponse<[String: [PostGiftDto]]> = try await client.request("/posts/\(postId)/gifts")
        guard let data = response.data, let gifts = data["gifts"] else {
            throw APIError.unknown("Failed to load gifts")
        }
        return gifts
    }

    public func getUserGiftStats(username: String) async throws -> GiftStatsData {
        let response: ApiResponse<GiftStatsData> = try await client.request("/users/\(username)/gift-stats")
        guard let data = response.data else {
            throw APIError.unknown("Failed to load gift stats")
        }
        return data
    }
}
