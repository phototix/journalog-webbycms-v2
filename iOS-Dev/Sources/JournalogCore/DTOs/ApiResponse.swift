public struct ApiResponse<T: Sendable & Codable>: Sendable, Codable {
    public let ok: Bool
    public let message: String?
    public let data: T?
}
