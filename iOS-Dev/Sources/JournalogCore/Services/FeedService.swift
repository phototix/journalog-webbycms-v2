import Foundation

public struct FeedService: Sendable {
    private let client = APIClient.shared

    public init() {}


    public func getFeed(page: Int = 1) async throws -> FeedData {
        let response: ApiResponse<FeedData> = try await client.request(
            "/feed", queryItems: [URLQueryItem(name: "page", value: String(page))]
        )
        guard let data = response.data else {
            throw APIError.unknown(response.message ?? "Failed to load feed")
        }
        return data
    }

    public func getSuggestions() async throws -> [UserBriefDto] {
        let response: ApiResponse<[String: [UserBriefDto]]> = try await client.request("/feed/suggestions")
        guard let data = response.data, let users = data["users"] else {
            throw APIError.unknown("Failed to load suggestions")
        }
        return users
    }
}
