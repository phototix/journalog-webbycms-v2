import Foundation

public enum APIError: LocalizedError, Sendable {
    case invalidURL
    case decodingError(Error)
    case networkError(Error)
    case serverError(statusCode: Int, message: String?)
    case unauthorized
    case notFound
    case unknown(String)

    public var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid URL"
        case .decodingError(let error):
            return "Failed to process response: \(error.localizedDescription)"
        case .networkError(let error):
            return "Network error: \(error.localizedDescription)"
        case .serverError(let code, let message):
            return message ?? "Server error (\(code))"
        case .unauthorized:
            return "Please log in again"
        case .notFound:
            return "Resource not found"
        case .unknown(let message):
            return message
        }
    }
}
