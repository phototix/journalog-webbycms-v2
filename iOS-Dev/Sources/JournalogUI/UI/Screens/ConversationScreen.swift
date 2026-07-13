import JournalogCore
import SwiftUI

public struct ConversationScreen: View {
    let userId: Int
    let userName: String
    let onBack: () -> Void

    @State private var messages: [MessageDto] = []
    @State private var newMessageText = ""
    @State private var isLoading = true

    private let messengerService = MessengerService()

    public var body: some View {
        VStack(spacing: 0) {
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 8) {
                        ForEach(messages) { message in
                            MessageBubbleView(message: message)
                        }
                    }
                    .padding()
                }
                .onChange(of: messages.count) { _, _ in
                    if let last = messages.last {
                        withAnimation {
                            proxy.scrollTo(last.id, anchor: .bottom)
                        }
                    }
                }
            }

            HStack(spacing: 12) {
                TextField("Message...", text: $newMessageText)
                    .textFieldStyle(.roundedBorder)

                Button(action: sendMessage) {
                    Image(systemName: "arrow.up.circle.fill")
                        .font(.title2)
                        .foregroundColor(AppTheme.instagramPink)
                }
                .disabled(newMessageText.trimmingCharacters(in: .whitespaces).isEmpty)
            }
            .padding()
            .background(Color(.systemBackground))
        }
        .navigationTitle(userName)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                }
            }
        }
        .task {
            await loadMessages()
        }
    }

    private func loadMessages() async {
        isLoading = true
        do {
            messages = try await messengerService.getMessages(userId: userId)
        } catch {
            print("Messages error: \(error)")
        }
        isLoading = false
    }

    private func sendMessage() {
        let text = newMessageText.trimmingCharacters(in: .whitespaces)
        guard !text.isEmpty else { return }
        newMessageText = ""

        Task {
            do {
                let message = try await messengerService.sendMessage(
                    receiverId: userId, text: text
                )
                messages.append(message)
            } catch {
                print("Send message error: \(error)")
            }
        }
    }
}

struct MessageBubbleView: View {
    let message: MessageDto

    var body: some View {
        HStack {
            if message.isMine { Spacer() }

            VStack(alignment: message.isMine ? .trailing : .leading, spacing: 2) {
                if let text = message.text {
                    Text(text)
                        .font(.body)
                        .padding(12)
                        .background(message.isMine ? AppTheme.instagramPink : Color(.systemGray5))
                        .foregroundColor(message.isMine ? .white : .primary)
                        .cornerRadius(16)
                }
                if let date = message.createdAt {
                    Text(date)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }

            if !message.isMine { Spacer() }
        }
    }
}
