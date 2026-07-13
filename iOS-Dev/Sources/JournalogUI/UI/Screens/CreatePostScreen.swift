import JournalogCore
import SwiftUI
import PhotosUI

public struct CreatePostScreen: View {
    @State private var postText = ""
    @State private var selectedItem: PhotosPickerItem?
    @State private var selectedImageData: Data?
    @State private var price: String = ""
    @State private var isUploading = false

    let onPostCreated: () -> Void

    private let postService = PostService()
    private let settingsService = SettingsService()

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    TextEditor(text: $postText)
                        .frame(minHeight: 120)
                        .overlay(
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(Color(.systemGray4), lineWidth: 1)
                        )
                        .cornerRadius(8)

                    if let selectedImageData, let uiImage = UIImage(data: selectedImageData) {
                        Image(uiImage: uiImage)
                            .resizable()
                            .scaledToFit()
                            .frame(maxHeight: 200)
                            .cornerRadius(12)
                    }

                    PhotosPicker(selection: $selectedItem, matching: .images) {
                        Label("Add Image", systemImage: "photo")
                            .frame(maxWidth: .infinity)
                            .frame(height: 44)
                            .background(Color(.systemGray6))
                            .cornerRadius(8)
                    }

                    HStack {
                        Text("Price (optional)")
                            .font(.subheadline)
                        Spacer()
                        TextField("0.00", text: $price)
                            .keyboardType(.decimalPad)
                            .textFieldStyle(.roundedBorder)
                            .frame(width: 120)
                    }

                    Button(action: createPost) {
                        if isUploading {
                            ProgressView()
                                .tint(.white)
                        } else {
                            Text("Post")
                                .fontWeight(.semibold)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
                    .background(AppTheme.instagramPink)
                    .foregroundColor(.white)
                    .cornerRadius(10)
                    .disabled(isUploading || postText.trimmingCharacters(in: .whitespaces).isEmpty)
                }
                .padding()
            }
            .navigationTitle("New Post")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private func createPost() {
        isUploading = true

        Task {
            var body: [String: Any] = [
                "text": postText,
            ]
            if let priceVal = Double(price), priceVal > 0 {
                body["price"] = priceVal
            }

            if let imageData = selectedImageData {
                let upload = try? await settingsService.uploadAttachment(
                    type: "post", data: imageData,
                    fileName: "post.jpg", mimeType: "image/jpeg"
                )
                if let upload {
                    body["media_id"] = upload.attachmentID
                }
            }

            do {
                try await postService.createPost(body: body)
                await MainActor.run {
                    isUploading = false
                    onPostCreated()
                }
            } catch {
                await MainActor.run {
                    isUploading = false
                    print("Create post error: \(error)")
                }
            }
        }
    }
}
