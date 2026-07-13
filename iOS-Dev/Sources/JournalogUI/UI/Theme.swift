import JournalogCore
import SwiftUI

public enum AppTheme {
    public static let instagramPink = Color(red: 0.882, green: 0.188, blue: 0.424)
    public static let instagramPurple = Color(red: 0.514, green: 0.227, blue: 0.706)
    public static let instagramOrange = Color(red: 0.969, green: 0.467, blue: 0.216)
    public static let instagramYellow = Color(red: 0.988, green: 0.686, blue: 0.271)
    public static let instagramBlue = Color(red: 0.251, green: 0.302, blue: 0.902)

    public static let storyGradient = LinearGradient(
        colors: [
            Color(red: 0.961, green: 0.522, blue: 0.161),
            Color(red: 0.867, green: 0.165, blue: 0.482),
            Color(red: 0.506, green: 0.204, blue: 0.686),
        ],
        startPoint: .bottomLeading,
        endPoint: .topTrailing
    )

    public static let surfaceLight = Color(red: 0.98, green: 0.98, blue: 0.98)
    public static let surfaceDark = Color(red: 0.12, green: 0.12, blue: 0.12)
    public static let cardLight = Color.white
    public static let cardDark = Color(red: 0.18, green: 0.18, blue: 0.18)
    public static let textPrimaryLight = Color(red: 0.1, green: 0.1, blue: 0.1)
    public static let textPrimaryDark = Color(red: 0.94, green: 0.94, blue: 0.94)
    public static let textSecondary = Color(red: 0.39, green: 0.39, blue: 0.39)
    public static let outline = Color(red: 0.86, green: 0.86, blue: 0.86)
}

extension View {
    public func journalogStyle() -> some View {
        self.tint(AppTheme.instagramPink)
    }
}
