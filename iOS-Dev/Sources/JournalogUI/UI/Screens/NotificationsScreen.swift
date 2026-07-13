import JournalogCore
import SwiftUI

public struct NotificationsScreen: View {
    let onBack: () -> Void

    @State private var notifications: [NotificationDto] = []
    @State private var isLoading = true

    private let notificationService = NotificationService()

    public var body: some View {
        NavigationStack {
            Group {
                if isLoading {
                    ProgressView()
                } else if notifications.isEmpty {
                    VStack(spacing: 16) {
                        Image(systemName: "bell")
                            .font(.system(size: 48))
                            .foregroundColor(.secondary)
                        Text("No notifications")
                            .font(.headline)
                            .foregroundColor(.secondary)
                    }
                } else {
                    List(notifications) { notification in
                        HStack(spacing: 12) {
                            AsyncImage(url: URL(string: notification.actor?.avatar ?? "")) { phase in
                                switch phase {
                                case .success(let image):
                                    image.resizable().scaledToFill()
                                default:
                                    Image(systemName: "person.circle.fill")
                                        .font(.title)
                                }
                            }
                            .frame(width: 44, height: 44)
                            .clipShape(Circle())

                            VStack(alignment: .leading, spacing: 4) {
                                if let message = notification.message {
                                    Text(message)
                                        .font(.subheadline)
                                }
                                if let date = notification.createdAt {
                                    Text(date)
                                        .font(.caption2)
                                        .foregroundColor(.secondary)
                                }
                            }

                            Spacer()

                            if !notification.read {
                                Circle()
                                    .fill(AppTheme.instagramPink)
                                    .frame(width: 8, height: 8)
                            }
                        }
                        .padding(.vertical, 4)
                        .opacity(notification.read ? 0.6 : 1.0)
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("Notifications")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Mark All Read") {
                        Task {
                            try? await notificationService.markAllRead()
                            await loadNotifications()
                        }
                    }
                    .font(.caption)
                }
            }
            .task {
                await loadNotifications()
            }
        }
    }

    private func loadNotifications() async {
        isLoading = true
        do {
            notifications = try await notificationService.getNotifications()
        } catch {
            print("Notifications error: \(error)")
        }
        isLoading = false
    }
}
