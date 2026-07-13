import Foundation

public struct ApkVersionDto: Sendable, Codable {
    public let versionCode: Int
    public let versionName: String
    public let downloadUrl: String
    public let releaseDate: String?
    public let changelog: String?

    enum CodingKeys: String, CodingKey {
        case versionCode = "version_code"
        case versionName = "version_name"
        case downloadUrl = "download_url"
        case releaseDate = "release_date"
        case changelog
    }
}

public struct UploadResponse: Sendable, Codable {
    public let attachmentID: String
    public let path: String
    public let type: String
    public let thumbnail: String?

    enum CodingKeys: String, CodingKey {
        case attachmentID
        case path, type, thumbnail
    }
}
