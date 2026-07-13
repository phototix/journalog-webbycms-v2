import Foundation

public struct UserService: Sendable {
    private let client = APIClient.shared

    public init() {}


    public func getProfile(username: String) async throws -> UserDto {
        let response: ApiResponse<[String: UserDto]> = try await client.request("/users/\(username)")
        guard let data = response.data, let user = data["user"] else {
            throw APIError.unknown("User not found")
        }
        return user
    }

    public func getUserPosts(username: String) async throws -> FeedData {
        let response: ApiResponse<FeedData> = try await client.request("/users/\(username)/posts")
        guard let data = response.data else {
            throw APIError.unknown("Failed to load posts")
        }
        return data
    }

    public func toggleFollow(username: String) async throws -> Bool {
        let response: ApiResponse<[String: Bool]> = try await client.request(
            "/users/\(username)/follow", method: "POST"
        )
        guard let data = response.data, let following = data["following"] else {
            throw APIError.unknown("Failed to toggle follow")
        }
        return following
    }
}
