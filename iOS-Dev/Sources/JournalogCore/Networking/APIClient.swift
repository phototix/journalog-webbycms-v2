import Foundation

public actor APIClient {
    public static let shared = APIClient()

    private let session: URLSession
    private let decoder: JSONDecoder
    private let encoder: JSONEncoder
    private var baseURL = "https://journalog.webbypage.com/api/v1"

    private init() {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        config.timeoutIntervalForResource = 60
        self.session = URLSession(configuration: config)
        self.decoder = JSONDecoder()
        self.encoder = JSONEncoder()
    }

    public func setBaseURL(_ url: String) {
        baseURL = url
    }

    public func request<T: Decodable & Sendable>(
        _ path: String,
        method: String = "GET",
        queryItems: [URLQueryItem]? = nil
    ) async throws -> T {
        try await _request(path: path, method: method, body: Optional<AnyCodable>.none, queryItems: queryItems)
    }

    public func request<T: Decodable & Sendable, B: Encodable & Sendable>(
        _ path: String,
        method: String = "GET",
        body: B,
        queryItems: [URLQueryItem]? = nil
    ) async throws -> T {
        try await _request(path: path, method: method, body: body, queryItems: queryItems)
    }

    private func _request<T: Decodable & Sendable, B: Encodable & Sendable>(
        path: String,
        method: String = "GET",
        body: B?,
        queryItems: [URLQueryItem]? = nil
    ) async throws -> T {
        guard var components = URLComponents(string: "\(baseURL)\(path)") else {
            throw APIError.invalidURL
        }

        if let queryItems {
            components.queryItems = queryItems
        }

        guard let url = components.url else {
            throw APIError.invalidURL
        }

        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = method
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")

        await AuthInterceptor.shared.apply(to: &urlRequest)

        if let body {
            urlRequest.httpBody = try encoder.encode(body)
        }

        let (data, response): (Data, URLResponse)

        do {
            (data, response) = try await session.data(for: urlRequest)
        } catch {
            throw APIError.networkError(error)
        }

        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.unknown("Invalid response")
        }

        switch httpResponse.statusCode {
        case 200...299:
            do {
                return try decoder.decode(T.self, from: data)
            } catch {
                throw APIError.decodingError(error)
            }
        case 401:
            throw APIError.unauthorized
        case 404:
            throw APIError.notFound
        default:
            let errorResponse = try? decoder.decode(
                ErrorResponse.self, from: data
            )
            throw APIError.serverError(
                statusCode: httpResponse.statusCode,
                message: errorResponse?.message
            )
        }
    }

    public func uploadMultipart(
        _ path: String,
        multipartBody: Data,
        boundary: String
    ) async throws -> Data {
        guard let url = URL(string: "\(baseURL)\(path)") else {
            throw APIError.invalidURL
        }

        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        urlRequest.httpBody = multipartBody

        await AuthInterceptor.shared.apply(to: &urlRequest)

        let (data, response): (Data, URLResponse)

        do {
            (data, response) = try await session.data(for: urlRequest)
        } catch {
            throw APIError.networkError(error)
        }

        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.unknown("Invalid response")
        }

        switch httpResponse.statusCode {
        case 200...299:
            return data
        case 401:
            throw APIError.unauthorized
        case 404:
            throw APIError.notFound
        default:
            let errorResponse = try? decoder.decode(
                ErrorResponse.self, from: data
            )
            throw APIError.serverError(
                statusCode: httpResponse.statusCode,
                message: errorResponse?.message
            )
        }
    }

    public func requestRaw(
        _ path: String,
        method: String = "GET",
        queryItems: [URLQueryItem]? = nil
    ) async throws -> Data {
        try await _requestRaw(path: path, method: method, body: Optional<AnyCodable>.none, queryItems: queryItems)
    }

    public func requestRaw<B: Encodable & Sendable>(
        _ path: String,
        method: String = "GET",
        body: B,
        queryItems: [URLQueryItem]? = nil
    ) async throws -> Data {
        try await _requestRaw(path: path, method: method, body: body, queryItems: queryItems)
    }

    private func _requestRaw<B: Encodable & Sendable>(
        path: String,
        method: String = "GET",
        body: B?,
        queryItems: [URLQueryItem]? = nil
    ) async throws -> Data {
        guard var components = URLComponents(string: "\(baseURL)\(path)") else {
            throw APIError.invalidURL
        }

        if let queryItems {
            components.queryItems = queryItems
        }

        guard let url = components.url else {
            throw APIError.invalidURL
        }

        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = method
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")

        await AuthInterceptor.shared.apply(to: &urlRequest)

        if let body {
            urlRequest.httpBody = try encoder.encode(body)
        }

        let (data, response): (Data, URLResponse)

        do {
            (data, response) = try await session.data(for: urlRequest)
        } catch {
            throw APIError.networkError(error)
        }

        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.unknown("Invalid response")
        }

        switch httpResponse.statusCode {
        case 200...299:
            return data
        case 401:
            throw APIError.unauthorized
        case 404:
            throw APIError.notFound
        default:
            let errorResponse = try? decoder.decode(
                ErrorResponse.self, from: data
            )
            throw APIError.serverError(
                statusCode: httpResponse.statusCode,
                message: errorResponse?.message
            )
        }
    }
}

private struct ErrorResponse: Decodable {
    let ok: Bool?
    let message: String?
}
