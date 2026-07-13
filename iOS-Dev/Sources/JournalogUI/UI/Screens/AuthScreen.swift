import JournalogCore
import SwiftUI

public struct AuthScreen: View {
    @State private var isLogin = true
    @State private var email = ""
    @State private var password = ""
    @State private var name = ""
    @State private var username = ""
    @State private var birthdate = Date()
    @State private var isLoading = false
    @State private var errorMessage: String?

    let onLoggedIn: () -> Void

    private let authService = AuthService()

    public var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                Spacer().frame(height: 40)

                Image(systemName: "camera.viewfinder")
                    .font(.system(size: 56))
                    .foregroundStyle(AppTheme.storyGradient)

                Text("Journalog")
                    .font(.title)
                    .fontWeight(.bold)

                Picker("Mode", selection: $isLogin) {
                    Text("Log In").tag(true)
                    Text("Register").tag(false)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal)

                VStack(spacing: 16) {
                    if !isLogin {
                        TextField("Name", text: $name)
                            .textContentType(.name)
                            .autocorrectionDisabled()
                            .textFieldStyle(.roundedBorder)

                        TextField("Username", text: $username)
                            .textContentType(.username)
                            .autocapitalization(.none)
                            .autocorrectionDisabled()
                            .textFieldStyle(.roundedBorder)
                    }

                    TextField("Email", text: $email)
                        .textContentType(.emailAddress)
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
                        .autocorrectionDisabled()
                        .textFieldStyle(.roundedBorder)

                    SecureField("Password", text: $password)
                        .textContentType(isLogin ? .password : .newPassword)
                        .textFieldStyle(.roundedBorder)

                    if !isLogin {
                        DatePicker("Birthdate", selection: $birthdate, displayedComponents: .date)
                            .datePickerStyle(.compact)
                    }

                    if let errorMessage {
                        Text(errorMessage)
                            .font(.caption)
                            .foregroundColor(.red)
                            .multilineTextAlignment(.center)
                    }

                    Button(action: handleAuth) {
                        if isLoading {
                            ProgressView()
                                .tint(.white)
                        } else {
                            Text(isLogin ? "Log In" : "Create Account")
                                .fontWeight(.semibold)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
                    .background(AppTheme.instagramPink)
                    .foregroundColor(.white)
                    .cornerRadius(10)
                    .disabled(isLoading)
                }
                .padding(.horizontal, 24)
            }
        }
        .background(Color(.systemBackground))
    }

    private func handleAuth() {
        isLoading = true
        errorMessage = nil

        Task {
            do {
                if isLogin {
                    let authData = try await authService.login(email: email, password: password)
                    await SessionManager.shared.saveSession(user: authData.user, token: authData.token)
                } else {
                    let formatter = DateFormatter()
                    formatter.dateFormat = "yyyy-MM-dd"
                    let birthdateStr = formatter.string(from: birthdate)
                    let authData = try await authService.register(
                        name: name, username: username, email: email,
                        password: password, birthdate: birthdateStr
                    )
                    await SessionManager.shared.saveSession(user: authData.user, token: authData.token)
                }
                await MainActor.run {
                    isLoading = false
                    onLoggedIn()
                }
            } catch {
                await MainActor.run {
                    isLoading = false
                    errorMessage = error.localizedDescription
                }
            }
        }
    }
}
