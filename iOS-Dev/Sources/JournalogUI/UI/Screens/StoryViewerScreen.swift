import JournalogCore
import SwiftUI

public struct StoryViewerScreen: View {
    let userId: Int
    let onBack: () -> Void

    @State private var stories: [StoryItemDto] = []
    @State private var currentIndex = 0
    @State private var progress: CGFloat = 0
    @State private var isLoading = true

    private let storyService = StoryService()
    private let timer = Timer.publish(every: 0.05, on: .main, in: .common).autoconnect()

    public var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .top) {
                Color.black.ignoresSafeArea()

                if isLoading {
                    ProgressView()
                        .tint(.white)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if !stories.isEmpty {
                    let story = stories[currentIndex]

                    AsyncImage(url: URL(string: story.url)) { phase in
                        switch phase {
                        case .success(let image):
                            image.resizable().scaledToFit()
                        case .failure:
                            if story.type == "video" {
                                Image(systemName: "play.circle")
                                    .font(.system(size: 60))
                                    .foregroundColor(.white)
                            } else {
                                Color.gray
                            }
                        case .empty:
                            Color.black
                        @unknown default:
                            Color.black
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .contentShape(Rectangle())
                    .onTapGesture { location in
                        if location.x > geometry.size.width / 2 {
                            goToNext()
                        } else {
                            goToPrevious()
                        }
                    }

                    VStack(spacing: 4) {
                        HStack(spacing: 4) {
                            ForEach(stories.indices, id: \.self) { index in
                                ProgressView(value: index == currentIndex ? progress : (index < currentIndex ? 1 : 0))
                                    .tint(.white)
                                    .frame(height: 2)
                            }
                        }
                        .padding(.top, 60)
                        .padding(.horizontal, 8)

                        if let text = story.text {
                            Text(text)
                                .foregroundColor(.white)
                                .font(.body)
                                .padding()
                        }
                    }

                    HStack {
                        Color.clear
                            .contentShape(Rectangle())
                            .onTapGesture { goToPrevious() }
                        Color.clear
                            .contentShape(Rectangle())
                            .onTapGesture { goToNext() }
                    }

                    VStack {
                        HStack {
                            Button(action: onBack) {
                                Image(systemName: "xmark")
                                    .foregroundColor(.white)
                                    .font(.title2)
                            }
                            Spacer()
                        }
                        .padding()
                        Spacer()
                    }
                }
            }
        }
        .onReceive(timer) { _ in
            guard !stories.isEmpty else { return }
            progress += 0.005
            if progress >= 1 {
                goToNext()
            }
        }
        .task {
            await loadStories()
        }
    }

    private func loadStories() async {
        isLoading = true
        do {
            let feed = try await storyService.getStoriesFeed()
            stories = feed.first(where: { $0.user.id == userId })?.stories ?? []
        } catch {
            print("Stories error: \(error)")
        }
        isLoading = false
    }

    private func goToNext() {
        guard !stories.isEmpty else { return }
        if currentIndex < stories.count - 1 {
            currentIndex += 1
            progress = 0
        } else {
            onBack()
        }
    }

    private func goToPrevious() {
        guard currentIndex > 0 else { return }
        currentIndex -= 1
        progress = 0
    }
}
