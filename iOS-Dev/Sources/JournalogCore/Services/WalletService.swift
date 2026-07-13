import Foundation

public struct WalletService: Sendable {
    private let client = APIClient.shared

    public init() {}


    public func getBalance() async throws -> WalletBalance {
        let response: ApiResponse<WalletBalance> = try await client.request("/wallet/balance")
        guard let data = response.data else {
            throw APIError.unknown("Failed to load balance")
        }
        return data
    }
}
