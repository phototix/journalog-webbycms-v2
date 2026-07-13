import Foundation

public struct NotificationService: Sendable {
    private let client = APIClient.shared

    public init() {}


    public func getNotifications() async throws -> [NotificationDto] {
        let response: ApiResponse<[String: PaginatedNotifications]> = try await client.request("/notifications")
        guard let data = response.data, let paginated = data["data"] else {
            throw APIError.unknown("Failed to load notifications")
        }
        return paginated.data
    }

    public func markAllRead() async throws {
        let _: ApiResponse<EmptyData> = try await client.request(
            "/notifications/read-all", method: "POST"
        )
    }
}
