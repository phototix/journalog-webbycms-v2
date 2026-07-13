import Foundation

public actor AuthInterceptor {
    public static let shared = AuthInterceptor()

    private var token: String?

    public func setToken(_ newToken: String?) {
        token = newToken
    }

    public func getToken() -> String? {
        token
    }

    public func apply(to request: inout URLRequest) {
        if let token {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        request.setValue("application/json", forHTTPHeaderField: "Accept")
    }
}
