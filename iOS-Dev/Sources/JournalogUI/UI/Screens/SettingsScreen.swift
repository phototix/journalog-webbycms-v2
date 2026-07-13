import JournalogCore
import SwiftUI

public struct SettingsScreen: View {
    let onBack: () -> Void
    let onLogout: () -> Void

    @State private var profile: UserDto?
    @State private var isLoading = true
    @State private var showingLogoutAlert = false

    private let settingsService = SettingsService()

    public var body: some View {
        NavigationStack {
            Form {
                if isLoading {
                    Section {
                        HStack {
                            Spacer()
                            ProgressView()
                            Spacer()
                        }
                    }
                } else if let profile {
                    Section("Profile") {
                        HStack {
                            AsyncImage(url: URL(string: profile.avatar ?? "")) { phase in
                                if case .success(let image) = phase {
                                    image.resizable().scaledToFill()
                                } else {
                                    Image(systemName: "person.circle.fill")
                                        .font(.title)
                                }
                            }
                            .frame(width: 60, height: 60)
                            .clipShape(Circle())

                            VStack(alignment: .leading) {
                                Text(profile.name)
                                    .fontWeight(.semibold)
                                Text("@\(profile.username)")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }

                    Section("Account") {
                        LabeledContent("Email", value: profile.email ?? "")
                        LabeledContent("Bio", value: profile.bio ?? "Not set")
                        LabeledContent("Location", value: profile.location ?? "Not set")
                        LabeledContent("Website", value: profile.website ?? "Not set")
                    }

                    Section {
                        Button("Edit Profile") { }
                        Button("Change Password") { }
                    }
                }

                Section {
                    Button("Log Out", role: .destructive) {
                        showingLogoutAlert = true
                    }
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                    }
                }
            }
            .alert("Log Out", isPresented: $showingLogoutAlert) {
                Button("Cancel", role: .cancel) {}
                Button("Log Out", role: .destructive) {
                    Task {
                        try? await AuthService().logout()
                        await SessionManager.shared.clearSession()
                        onLogout()
                    }
                }
            } message: {
                Text("Are you sure you want to log out?")
            }
            .task {
                await loadProfile()
            }
        }
    }

    private func loadProfile() async {
        isLoading = true
        do {
            profile = try await settingsService.getProfile()
        } catch {
            print("Settings error: \(error)")
        }
        isLoading = false
    }
}
