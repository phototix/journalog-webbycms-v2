import JournalogCore
import SwiftUI

public struct PollView: View {
    let poll: PollDto
    let onVote: (Int) -> Void

    @State private var hasVoted = false

    public var body: some View {
        VStack(spacing: 8) {
            if let answers = poll.answers {
                ForEach(answers) { answer in
                    Button(action: {
                        guard !hasVoted else { return }
                        hasVoted = true
                        onVote(answer.id)
                    }) {
                        HStack {
                            if hasVoted {
                                GeometryReader { geo in
                                    ZStack(alignment: .leading) {
                                        RoundedRectangle(cornerRadius: 8)
                                            .fill(AppTheme.instagramPink.opacity(0.15))
                                            .frame(width: geo.size.width, height: 36)

                                        RoundedRectangle(cornerRadius: 8)
                                            .fill(AppTheme.instagramPink.opacity(0.3))
                                            .frame(
                                                width: geo.size.width * CGFloat(answer.percentage) / 100,
                                                height: 36
                                            )

                                        HStack {
                                            Text(answer.answer)
                                                .font(.subheadline)
                                                .foregroundColor(.primary)
                                                .padding(.leading, 12)

                                            Spacer()

                                            if answer.votesCount > 0 {
                                                Text("\(answer.percentage)%")
                                                    .font(.caption)
                                                    .foregroundColor(.secondary)
                                                    .padding(.trailing, 12)
                                            }
                                        }
                                    }
                                }
                                .frame(height: 36)
                            } else {
                                HStack {
                                    Text(answer.answer)
                                        .font(.subheadline)
                                    Spacer()
                                }
                                .padding(.horizontal, 12)
                                .frame(height: 36)
                                .background(Color(.systemGray6))
                                .cornerRadius(8)
                            }
                        }
                    }
                    .disabled(hasVoted)
                    .foregroundColor(.primary)
                }
            }

            Text("\(poll.totalVotes) votes")
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }
}
