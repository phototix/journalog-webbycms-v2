import SwiftUI

@main
struct JournalogApp: App {
    @StateObject private var sessionManager = SessionManager.shared
    @State private var isReady = false
    @State private var isAuthenticated = false
    @State private var showSplash = true

    var body: some Scene {
        WindowGroup {
            if showSplash {
                SplashScreen { _ in
                    showSplash = false
                }
            } else {
                ContentView()
                    .environmentObject(sessionManager)
                    .environmentObject(NavigationCoordinator.shared)
                    .journalogStyle()
            }
        }
    }
}
