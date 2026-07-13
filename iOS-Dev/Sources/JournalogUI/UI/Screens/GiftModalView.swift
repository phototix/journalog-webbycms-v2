import JournalogCore
import SwiftUI

public struct GiftModalView: View {
    let postId: Int
    let onBack: () -> Void

    @State private var gifts: GiftListData?
    @State private var selectedGift: GiftDto?
    @State private var isLoading = true
    @State private var showConfirmation = false

    private let giftService = GiftService()

    public var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                if isLoading {
                    ProgressView("Loading gifts...")
                } else if let gifts {
                    if let balance = gifts.balance as Double? {
                        HStack {
                            Image(systemName: "creditcard.fill")
                                .foregroundColor(.yellow)
                            Text("Balance: $\(String(format: "%.2f", balance))")
                                .fontWeight(.semibold)
                        }
                        .padding()
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color(.systemGray6))
                        .cornerRadius(10)
                        .padding(.horizontal)
                    }

                    ScrollView {
                        ForEach(Array(gifts.gifts.keys.sorted()), id: \.self) { category in
                            if let categoryGifts = gifts.gifts[category] {
                                VStack(alignment: .leading, spacing: 8) {
                                    Text(category.capitalized)
                                        .font(.headline)
                                        .padding(.horizontal)

                                    LazyVGrid(columns: [
                                        GridItem(.flexible()),
                                        GridItem(.flexible()),
                                        GridItem(.flexible()),
                                        GridItem(.flexible()),
                                    ], spacing: 12) {
                                        ForEach(categoryGifts) { gift in
                                            GiftItemView(
                                                gift: gift,
                                                isSelected: selectedGift?.id == gift.id
                                            )
                                            .onTapGesture {
                                                selectedGift = gift
                                            }
                                        }
                                    }
                                    .padding(.horizontal)
                                }
                            }
                        }
                    }

                    if let selectedGift {
                        Button(action: sendGift) {
                            Label(
                                "Send \(selectedGift.name) (\(selectedGift.credits) credits)",
                                systemImage: "gift.fill"
                            )
                            .fontWeight(.semibold)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 44)
                        .background(AppTheme.instagramPink)
                        .foregroundColor(.white)
                        .cornerRadius(10)
                        .padding(.horizontal)
                    }
                }
            }
            .navigationTitle("Send a Gift")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                    }
                }
            }
            .task {
                await loadGifts()
            }
        }
    }

    private func loadGifts() async {
        isLoading = true
        do {
            gifts = try await giftService.getGifts()
        } catch {
            print("Gifts error: \(error)")
        }
        isLoading = false
    }

    private func sendGift() {
        guard let gift = selectedGift else { return }
        Task {
            do {
                _ = try await giftService.sendGift(giftId: gift.id, postId: postId)
                await MainActor.run {
                    onBack()
                }
            } catch {
                print("Send gift error: \(error)")
            }
        }
    }
}

struct GiftItemView: View {
    let gift: GiftDto
    let isSelected: Bool

    var body: some View {
        VStack(spacing: 4) {
            AsyncImage(url: URL(string: gift.icon)) { phase in
                switch phase {
                case .success(let image):
                    image.resizable().scaledToFit()
                default:
                    Image(systemName: "gift")
                        .font(.title)
                }
            }
            .frame(width: 44, height: 44)

            Text(gift.name)
                .font(.caption2)
                .lineLimit(1)

            Text("\(gift.credits)")
                .font(.caption2)
                .foregroundColor(.secondary)
        }
        .padding(8)
        .background(isSelected ? AppTheme.instagramPink.opacity(0.1) : Color.clear)
        .cornerRadius(8)
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(isSelected ? AppTheme.instagramPink : Color.clear, lineWidth: 2)
        )
    }
}
