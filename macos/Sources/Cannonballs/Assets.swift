import Foundation
import AppKit

/// Locates the platform-neutral game assets (repo: shared/Resources — one asset
/// tree consumed by both the macOS and Windows builds).
enum Assets {
    static let root: URL = {
        let fm = FileManager.default
        func hasAssets(_ url: URL) -> Bool {
            fm.fileExists(atPath: url.appendingPathComponent("maplist.dat").path)
        }
        // packaged .app: Contents/Resources/Resources (copied from shared/Resources)
        if let r = Bundle.main.resourceURL?.appendingPathComponent("Resources"), hasAssets(r) {
            return r
        }
        // dev runs (swift run): walk up from the executable to the repo's shared/Resources
        var dir = Bundle.main.bundleURL
        for _ in 0..<8 {
            let cand = dir.appendingPathComponent("shared/Resources")
            if hasAssets(cand) { return cand }
            dir.deleteLastPathComponent()
        }
        fatalError("Game assets not found: expected Resources in the app bundle or shared/Resources up-tree")
    }()

    static func url(_ relative: String) -> URL {
        root.appendingPathComponent(relative)
    }

    static func data(_ relative: String) -> Data? {
        try? Data(contentsOf: url(relative))
    }

    static func text(_ relative: String) -> String? {
        guard let d = data(relative) else { return nil }
        return String(data: d, encoding: .utf8) ?? String(data: d, encoding: .isoLatin1)
    }

    static func image(_ relative: String) -> NSImage? {
        NSImage(contentsOf: url(relative))
    }
}
