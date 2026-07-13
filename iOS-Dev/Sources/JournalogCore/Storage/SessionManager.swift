import Foundation

@MainActor
public final class SessionManager: ObservableObject, @unchecked Sendable {
    public static let shared = SessionManager()

    @Published public private(set) var isLoggedIn = false
    @Published public private(set) var currentUser: UserDto?

    private let keychain = KeychainManager.shared
    private let authInterceptor = AuthInterceptor.shared

    private init() {}

    public func restoreSession() async {
        let token = await keychain.read(key: "auth_token")
        if let token {
            await authInterceptor.setToken(token)
            isLoggedIn = true
        }
    }

    public func saveSession(user: UserDto, token: String) async {
        currentUser = user
        await keychain.save(key: "auth_token", value: token)
        await keychain.save(key: "user_id", value: String(user.id))
        await keychain.save(key: "username", value: user.username)
        await keychain.save(key: "name", value: user.name)
        await keychain.save(key: "avatar", value: user.avatar ?? "")
        await keychain.save(key: "role_id", value: String(user.roleId ?? 2))
        await authInterceptor.setToken(token)
        isLoggedIn = true
    }

    public func clearSession() async {
        currentUser = nil
        await keychain.clearAll()
        await authInterceptor.setToken(nil)
        isLoggedIn = false
    }

    public func getStoredToken() async -> String? {
        await keychain.read(key: "auth_token")
    }
}
