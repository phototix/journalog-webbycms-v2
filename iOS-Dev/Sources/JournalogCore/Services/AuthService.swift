import Foundation

public struct AuthService: Sendable {
    private let client = APIClient.shared

    public init() {}


    public func register(name: String, username: String, email: String, password: String, birthdate: String) async throws -> AuthData {
        let body = RegisterRequest(name: name, username: username, email: email, password: password, birthdate: birthdate)
        let response: ApiResponse<AuthData> = try await client.request(
            "/auth/register", method: "POST", body: body
        )
        guard let data = response.data else {
            throw APIError.unknown(response.message ?? "Registration failed")
        }
        return data
    }

    public func login(email: String, password: String) async throws -> AuthData {
        let body = LoginRequest(email: email, password: password)
        let response: ApiResponse<AuthData> = try await client.request(
            "/auth/login", method: "POST", body: body
        )
        guard let data = response.data else {
            throw APIError.unknown(response.message ?? "Login failed")
        }
        return data
    }

    public func getUser() async throws -> UserDto {
        let response: ApiResponse<[String: UserDto]> = try await client.request("/auth/user")
        guard let data = response.data, let user = data["user"] else {
            throw APIError.unknown("Failed to load user")
        }
        return user
    }

    public func logout() async throws {
        let _: ApiResponse<EmptyData> = try await client.request("/auth/logout", method: "POST")
    }
}
