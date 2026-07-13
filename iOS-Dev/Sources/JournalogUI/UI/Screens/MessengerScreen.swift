import JournalogCore
import SwiftUI

public struct MessengerScreen: View {
    @State private var conversations: [ConversationDto] = []
    @State private var isLoading = true

    let onConversationClick: (Int, String) -> Void

    private let messengerService = MessengerService()

    public var body: some View {
        NavigationStack {
            Group {
                if isLoading {
                    ProgressView()
                } else if conversations.isEmpty {
                    VStack(spacing: 16) {
                        Image(systemName: "message")
                            .font(.system(size: 48))
                            .foregroundColor(.secondary)
                        Text("No conversations")
                            .font(.headline)
                            .foregroundColor(.secondary)
                    }
                } else {
                    List(conversations) { conversation in
                        HStack(spacing: 12) {
                            AsyncImage(url: URL(string: conversation.avatar)) { phase in
                                switch phase {
                                case .success(let image):
                                    image.resizable().scaledToFill()
                                default:
                                    Image(systemName: "person.circle.fill")
                                        .font(.title)
                                }
                            }
                            .frame(width: 50, height: 50)
                            .clipShape(Circle())

                            VStack(alignment: .leading, spacing: 4) {
                                HStack {
                                    Text(conversation.name)
                                        .fontWeight(.semibold)
                                    Spacer()
                                    if let date = conversation.lastMessageDate {
                                        Text(date)
                                            .font(.caption2)
                                            .foregroundColor(.secondary)
                                    }
                                }
                                if let lastMsg = conversation.lastMessage {
                                    Text(lastMsg)
                                        .font(.subheadline)
                                        .foregroundColor(.secondary)
                                        .lineLimit(1)
                                }
                            }

                            if conversation.unreadCount > 0 {
                                Text("\(conversation.unreadCount)")
                                    .font(.caption)
                                    .foregroundColor(.white)
                                    .padding(6)
                                    .background(AppTheme.instagramPink)
                                    .clipShape(Circle())
                            }
                        }
                        .padding(.vertical, 4)
                        .onTapGesture {
                            onConversationClick(conversation.contactId, conversation.name)
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("Messages")
            .navigationBarTitleDisplayMode(.inline)
            .task {
                await loadConversations()
            }
        }
    }

    private func loadConversations() async {
        isLoading = true
        do {
            conversations = try await messengerService.getConversations()
        } catch {
            print("Messenger error: \(error)")
        }
        isLoading = false
    }
}
