import JournalogCore
import SwiftUI
import PhotosUI

public struct StoryCreateScreen: View {
    @State private var selectedItem: PhotosPickerItem?
    @State private var selectedImageData: Data?
    @State private var storyText = ""
    @State private var isUploading = false
    @State private var selectedColor = AppTheme.instagramPink

    let onBack: () -> Void
    let onStoryCreated: () -> Void

    private let settingsService = SettingsService()

    private let colors: [Color] = [
        .black, .white, .red, .orange, .yellow, .green, .blue, .purple,
        AppTheme.instagramPink, AppTheme.instagramPurple,
    ]

    public var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                if let selectedImageData, let uiImage = UIImage(data: selectedImageData) {
                    Image(uiImage: uiImage)
                        .resizable()
                        .scaledToFit()
                        .frame(maxHeight: 300)
                        .cornerRadius(12)
                } else {
                    PhotosPicker(selection: $selectedItem, matching: .images) {
                        VStack(spacing: 12) {
                            Image(systemName: "photo.on.rectangle")
                                .font(.system(size: 48))
                            Text("Select Image")
                                .font(.headline)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 200)
                        .background(Color(.systemGray6))
                        .cornerRadius(12)
                    }
                }

                TextField("Add text...", text: $storyText)
                    .textFieldStyle(.roundedBorder)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(colors, id: \.self) { color in
                            Circle()
                                .fill(color)
                                .frame(width: 36, height: 36)
                                .overlay(
                                    Circle()
                                        .stroke(color == selectedColor ? Color.primary : Color.clear, lineWidth: 2)
                                )
                                .onTapGesture {
                                    selectedColor = color
                                }
                        }
                    }
                }

                Spacer()
            }
            .padding()
            .navigationTitle("New Story")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel", action: onBack)
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Share") {
                        uploadStory()
                    }
                    .disabled(isUploading)
                }
            }
            .onChange(of: selectedItem) { _, newItem in
                Task {
                    if let data = try? await newItem?.loadTransferable(type: Data.self) {
                        selectedImageData = data
                    }
                }
            }
        }
    }

    private func uploadStory() {
        guard let imageData = selectedImageData else { return }
        isUploading = true

        Task {
            do {
                let upload = try await settingsService.uploadAttachment(
                    type: "story", data: imageData,
                    fileName: "story.jpg", mimeType: "image/jpeg"
                )
                var body: [String: Any] = [
                    "type": "image",
                    "url": upload.path,
                    "text": storyText,
                ]
                try await StoryService().createStory(body: body)
                await MainActor.run {
                    isUploading = false
                    onStoryCreated()
                }
            } catch {
                await MainActor.run {
                    isUploading = false
                    print("Upload error: \(error)")
                }
            }
        }
    }
}
