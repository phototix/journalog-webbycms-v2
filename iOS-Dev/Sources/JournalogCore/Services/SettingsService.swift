import Foundation

public struct SettingsService: Sendable {
    private let client = APIClient.shared

    public init() {}


    public func getProfile() async throws -> UserDto {
        let response: ApiResponse<[String: UserDto]> = try await client.request("/settings/profile")
        guard let data = response.data, let user = data["user"] else {
            throw APIError.unknown("Failed to load profile")
        }
        return user
    }

    public func updateProfile(body: AnyCodable) async throws {
        let _: ApiResponse<EmptyData> = try await client.request(
            "/settings/profile", method: "PUT", body: body
        )
    }

    public func updatePassword(currentPassword: String, newPassword: String) async throws {
        let bodyDict: [String: String] = [
            "current_password": currentPassword,
            "password": newPassword,
            "password_confirmation": newPassword,
        ]
        let body = AnyCodable(bodyDict)
        let _: ApiResponse<EmptyData> = try await client.request(
            "/settings/password", method: "PUT", body: body
        )
    }

    public func uploadAttachment(type: String, data: Data, fileName: String, mimeType: String) async throws -> UploadResponse {
        let boundary = "Boundary-\(UUID().uuidString)"
        var body = Data()

        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"file\"; filename=\"\(fileName)\"\r\n".data(using: .utf8)!)
        body.append("Content-Type: \(mimeType)\r\n\r\n".data(using: .utf8)!)
        body.append(data)
        body.append("\r\n--\(boundary)--\r\n".data(using: .utf8)!)

        let rawData = try await APIClient.shared.uploadMultipart(
            "/attachment/upload/\(type)",
            multipartBody: body,
            boundary: boundary
        )

        let response = try JSONDecoder().decode(ApiResponse<UploadResponse>.self, from: rawData)
        guard let uploadData = response.data else {
            throw APIError.unknown(response.message ?? "Upload failed")
        }
        return uploadData
    }
}
