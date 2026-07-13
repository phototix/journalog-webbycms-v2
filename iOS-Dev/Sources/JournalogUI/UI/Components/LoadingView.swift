import JournalogCore
import SwiftUI

public struct LoadingView: View {
    let message: String

    public init(_ message: String = "Loading...") {
        self.message = message
    }

    public var body: some View {
        VStack(spacing: 16) {
            ProgressView()
                .tint(AppTheme.instagramPink)
            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
