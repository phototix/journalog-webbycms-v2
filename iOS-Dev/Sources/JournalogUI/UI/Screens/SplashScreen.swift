import JournalogCore
import SwiftUI

public struct SplashScreen: View {
    @State private var isAnimating = false

    let onComplete: (Bool) -> Void

    public var body: some View {
        VStack(spacing: 16) {
            Spacer()

            Image(systemName: "camera.viewfinder")
                .font(.system(size: 72))
                .foregroundStyle(AppTheme.storyGradient)
                .scaleEffect(isAnimating ? 1.0 : 0.5)
                .opacity(isAnimating ? 1.0 : 0.0)

            Text("Journalog")
                .font(.largeTitle)
                .fontWeight(.bold)
                .foregroundColor(.primary)
                .opacity(isAnimating ? 1.0 : 0.0)
                .offset(y: isAnimating ? 0 : 20)

            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemBackground))
        .onAppear {
            withAnimation(.easeOut(duration: 0.8)) {
                isAnimating = true
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                onComplete(true)
            }
        }
    }
}
