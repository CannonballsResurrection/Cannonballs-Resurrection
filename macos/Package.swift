// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "CannonballsMac",
    platforms: [.macOS(.v13)],
    targets: [
        // Game assets live in the repo's shared/Resources (one tree for both the
        // macOS and Windows builds); Assets.swift locates them at runtime, and
        // .app packaging copies them into Contents/Resources/Resources.
        .executableTarget(
            name: "Cannonballs",
            path: "Sources/Cannonballs"
        )
    ]
)
