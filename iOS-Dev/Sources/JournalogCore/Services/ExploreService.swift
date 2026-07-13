import Foundation

public struct ExploreService: Sendable {
    private let client = APIClient.shared

    public init() {}


    public func getUsers(page: Int = 1) async throws -> ExploreUsersData {
        let response: ApiResponse<ExploreUsersData> = try await client.request(
            "/explore/users", queryItems: [URLQueryItem(name: "page", value: String(page))]
        )
        guard let data = response.data else {
            throw APIError.unknown("Failed to load explore users")
        }
        return data
    }
}
