import Foundation

public struct SearchService: Sendable {
    private let client = APIClient.shared

    public init() {}


    public func search(query: String, type: String = "all") async throws -> [SearchResultDto] {
        let items = [
            URLQueryItem(name: "q", value: query),
            URLQueryItem(name: "type", value: type),
        ]
        let response: ApiResponse<[String: AnyCodable]> = try await client.request(
            "/search", queryItems: items
        )
        guard let data = response.data,
              let resultsData = data["results"],
              let jsonData = try? JSONSerialization.data(withJSONObject: resultsData.value),
              let results = try? JSONDecoder().decode([SearchResultDto].self, from: jsonData) else {
            return []
        }
        return results
    }

    public func getTrending() async throws -> [TrendingUserDto] {
        let response: ApiResponse<[String: [TrendingUserDto]]> = try await client.request("/trending")
        guard let data = response.data, let users = data["users"] else {
            throw APIError.unknown("Failed to load trending")
        }
        return users
    }
}
