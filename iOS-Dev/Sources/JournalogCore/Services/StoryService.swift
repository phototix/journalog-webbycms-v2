import Foundation

public struct StoryService: Sendable {
    private let client = APIClient.shared

    public init() {}


    public func getStoriesFeed() async throws -> [StoryGroupDto] {
        let response: ApiResponse<[String: AnyCodable]> = try await client.request("/stories/feed")
        guard let data = response.data else { return [] }
        let rawData = data["stories"]?.value ?? []
        guard let storiesData = try? JSONSerialization.data(withJSONObject: rawData),
              let stories = try? JSONDecoder().decode([StoryGroupDto].self, from: storiesData) else {
            return []
        }
        return stories
    }

    public func createStory(body: [String: Any]) async throws {
        let _: ApiResponse<[String: AnyCodable]> = try await client.request(
            "/stories", method: "POST", body: AnyCodable(body)
        )
    }

    public func createStoryCodable(body: AnyCodable) async throws {
        let _: ApiResponse<[String: AnyCodable]> = try await client.request(
            "/stories", method: "POST", body: body
        )
    }

    public func viewStory(id: String) async throws {
        let _: ApiResponse<EmptyData> = try await client.request(
            "/stories/\(id)/view", method: "POST"
        )
    }

    public func deleteStory(id: String) async throws {
        let _: ApiResponse<EmptyData> = try await client.request(
            "/stories/\(id)", method: "DELETE"
        )
    }
}
