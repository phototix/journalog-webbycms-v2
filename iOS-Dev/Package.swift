// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "Journalog",
    defaultLocalization: "en",
    platforms: [
        .iOS(.v17),
        .macOS(.v14),
    ],
    products: [
        .library(name: "JournalogCore", targets: ["JournalogCore"]),
        .library(name: "JournalogUI", targets: ["JournalogUI"]),
        .executable(name: "JournalogCLI", targets: ["JournalogCLI"]),
    ],
    dependencies: [],
    targets: [
        .target(
            name: "JournalogCore",
            dependencies: [],
            path: "Sources/JournalogCore"
        ),
        .target(
            name: "JournalogUI",
            dependencies: ["JournalogCore"],
            path: "Sources/JournalogUI"
        ),
        .executableTarget(
            name: "JournalogCLI",
            dependencies: ["JournalogCore"],
            path: "Sources/JournalogCLI"
        ),
    ]
)
