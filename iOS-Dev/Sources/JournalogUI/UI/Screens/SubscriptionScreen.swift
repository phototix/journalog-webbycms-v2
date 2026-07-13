import JournalogCore
import SwiftUI

public struct SubscriptionScreen: View {
    let creator: UserDto
    let onBack: () -> Void
    let onSubscribed: () -> Void

    @State private var plan: SubscriptionPlan?
    @State private var selectedPlan = "monthly"
    @State private var isLoading = true
    @State private var isSubscribing = false

    private let subscriptionService = SubscriptionService()

    public var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                AsyncImage(url: URL(string: creator.avatar ?? "")) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().scaledToFill()
                    default:
                        Image(systemName: "person.circle.fill")
                            .font(.system(size: 60))
                    }
                }
                .frame(width: 100, height: 100)
                .clipShape(Circle())

                Text(creator.name)
                    .font(.title2)
                    .fontWeight(.bold)

                Text("Subscribe to \(creator.name) for exclusive content")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)

                if isLoading {
                    ProgressView()
                } else if let plan {
                    VStack(spacing: 12) {
                        PlanButton(
                            title: "Monthly",
                            price: plan.price,
                            isSelected: selectedPlan == "monthly"
                        ) { selectedPlan = "monthly" }

                        PlanButton(
                            title: "3 Months",
                            price: plan.price3Months,
                            isSelected: selectedPlan == "3months"
                        ) { selectedPlan = "3months" }

                        PlanButton(
                            title: "6 Months",
                            price: plan.price6Months,
                            isSelected: selectedPlan == "6months"
                        ) { selectedPlan = "6months" }

                        PlanButton(
                            title: "12 Months",
                            price: plan.price12Months,
                            isSelected: selectedPlan == "12months"
                        ) { selectedPlan = "12months" }
                    }
                    .padding(.horizontal)

                    Button(action: subscribe) {
                        if isSubscribing {
                            ProgressView()
                                .tint(.white)
                        } else {
                            Text("Subscribe")
                                .fontWeight(.semibold)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
                    .background(AppTheme.instagramPink)
                    .foregroundColor(.white)
                    .cornerRadius(10)
                    .padding(.horizontal)
                    .disabled(isSubscribing)
                }
            }
            .padding()
            .navigationTitle("Subscription")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                    }
                }
            }
            .task {
                await loadPlans()
            }
        }
    }

    private func loadPlans() async {
        isLoading = true
        do {
            plan = try await subscriptionService.getPlans(username: creator.username)
        } catch {
            print("Plans error: \(error)")
        }
        isLoading = false
    }

    private func subscribe() {
        isSubscribing = true
        Task {
            do {
                _ = try await subscriptionService.subscribe(
                    recipientUserId: creator.id, plan: selectedPlan
                )
                await MainActor.run {
                    isSubscribing = false
                    onSubscribed()
                }
            } catch {
                await MainActor.run {
                    isSubscribing = false
                    print("Subscribe error: \(error)")
                }
            }
        }
    }
}

struct PlanButton: View {
    let title: String
    let price: Double
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                Text(title)
                    .fontWeight(.medium)
                Spacer()
                Text(price > 0 ? "$\(String(format: "%.2f", price))" : "Free")
                    .fontWeight(.bold)
            }
            .padding()
            .background(isSelected ? AppTheme.instagramPink.opacity(0.1) : Color(.systemGray6))
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(isSelected ? AppTheme.instagramPink : Color.clear, lineWidth: 2)
            )
            .cornerRadius(10)
        }
        .foregroundColor(.primary)
    }
}
