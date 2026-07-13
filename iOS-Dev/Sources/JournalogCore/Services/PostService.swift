import Foundation

public struct PostService: Sendable {
    private let client = APIClient.shared

    public init() {}


    public func getPost(id: Int) async throws -> PostDto {
        let response: ApiResponse<[String: PostDto]> = try await client.request("/posts/\(id)")
        guard let data = response.data, let post = data["post"] else {
            throw APIError.unknown("Post not found")
        }
        return post
    }

    public func createPost(body: AnyCodable) async throws -> PostDto {
        let response: ApiResponse<[String: PostDto]> = try await client.request(
            "/posts", method: "POST", body: body
        )
        guard let data = response.data, let post = data["post"] else {
            throw APIError.unknown("Failed to create post")
        }
        return post
    }

    public func deletePost(id: Int) async throws {
        let _: ApiResponse<EmptyData> = try await client.request("/posts/\(id)", method: "DELETE")
    }

    public func toggleLike(id: Int) async throws -> Bool {
        let response: ApiResponse<[String: AnyCodable]> = try await client.request(
            "/posts/\(id)/like", method: "POST"
        )
        guard let data = response.data,
              let liked = data["liked"]?.value as? Bool else {
            throw APIError.unknown("Failed to toggle like")
        }
        return liked
    }

    public func toggleBookmark(id: Int) async throws -> Bool {
        let response: ApiResponse<[String: Bool]> = try await client.request(
            "/posts/\(id)/bookmark", method: "POST"
        )
        guard let data = response.data, let bookmarked = data["bookmarked"] else {
            throw APIError.unknown("Failed to toggle bookmark")
        }
        return bookmarked
    }

    public func getComments(postId: Int) async throws -> [CommentDto] {
        let response: ApiResponse<CommentsResponse> = try await client.request("/posts/\(postId)/comments")
        guard let data = response.data else {
            throw APIError.unknown("Failed to load comments")
        }
        return data.comments.data
    }

    public func addComment(postId: Int, text: String) async throws -> CommentDto {
        let body = ["text": text]
        let response: ApiResponse<[String: CommentDto]> = try await client.request(
            "/posts/\(postId)/comments", method: "POST", body: AnyCodable(body)
        )
        guard let data = response.data, let comment = data["comment"] else {
            throw APIError.unknown("Failed to add comment")
        }
        return comment
    }

    public func votePoll(postId: Int, answerId: Int) async throws -> PollDto {
        let body = ["answer_id": answerId]
        let response: ApiResponse<[String: PollDto]> = try await client.request(
            "/posts/\(postId)/poll-vote", method: "POST", body: AnyCodable(body)
        )
        guard let data = response.data, let poll = data["poll"] else {
            throw APIError.unknown("Failed to vote")
        }
        return poll
    }
}
