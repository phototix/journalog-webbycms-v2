import Foundation

public struct SubscriptionService: Sendable {
    private let client = APIClient.shared

    public init() {}


    public func getPlans(username: String) async throws -> SubscriptionPlan {
        let response: ApiResponse<SubscriptionPlan> = try await client.request("/subscriptions/plans/\(username)")
        guard let data = response.data else {
            throw APIError.unknown("Failed to load plans")
        }
        return data
    }

    public func subscribe(recipientUserId: Int, plan: String) async throws -> SubscriptionData {
        let body = SubscribeRequest(recipientUserId: recipientUserId, plan: plan)
        let response: ApiResponse<SubscriptionData> = try await client.request(
            "/subscriptions/subscribe", method: "POST", body: body
        )
        guard let data = response.data else {
            throw APIError.unknown("Failed to subscribe")
        }
        return data
    }

    public func cancelSubscription(recipientUserId: Int) async throws {
        let body = ["recipient_user_id": recipientUserId]
        let _: ApiResponse<EmptyData> = try await client.request(
            "/subscriptions/cancel", method: "POST", body: AnyCodable(body)
        )
    }
}
