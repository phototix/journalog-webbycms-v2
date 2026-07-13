import JournalogCore
import SwiftUI

public struct UserRowView: View {
    let user: UserBriefDto

    public var body: some View {
        HStack(spacing: 12) {
            AsyncImage(url: URL(string: user.avatar ?? "")) { phase in
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

            VStack(alignment: .leading, spacing: 2) {
                Text(user.name)
                    .fontWeight(.semibold)
                    .font(.subheadline)
                Text("@\(user.username)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()
        }
    }
}
