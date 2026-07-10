import Foundation
import SceneKit
import ModelIO
import SceneKit.ModelIO

/// Loads the ORIGINAL decoded WildTangent models (extracted from .wsad/.wsgo/.wsbm
/// via the cracked WLD3 container) from Resources/MODELS/<NAME>/model.json.
///
/// Manifest schema (produced by the extraction pipeline):
/// {
///   "name": "CANNON",
///   "upAxis": "Y" | "Z",      // native axis that should map to SceneKit +Y (optional, default Y)
///   "scale": 1.0,              // uniform scale to apply (optional, default 1)
///   "parts": [
///     { "mesh": "barrel.obj",
///       "textures": ["cannonbase.jpg"],   // optional; MTL is used if present
///       "transform": [16 floats row-major] // optional 4x4; identity if absent
///     }, ...
///   ]
/// }
///
/// Returns nil when no manifest exists for a name, so callers fall back to
/// procedural geometry. Results are cached and returned as clones.
enum ModelLibrary {

    private static var cache: [String: SCNNode?] = [:]

    /// Real model node for a prop/object name, or nil if not available.
    static func node(for name: String) -> SCNNode? {
        if let cached = cache[name] { return cached?.clone() }
        let built = build(name)
        cache[name] = built
        return built?.clone()
    }

    static func hasModel(_ name: String) -> Bool {
        FileManager.default.fileExists(atPath: Assets.url("MODELS/\(name)/model.json").path)
    }

    private struct Manifest: Decodable {
        struct Part: Decodable {
            let mesh: String
            var textures: [String]?
            var transform: [Float]?
        }
        var name: String?
        var upAxis: String?
        var scale: Float?
        let parts: [Part]
    }

    private static func build(_ name: String) -> SCNNode? {
        let dir = "MODELS/\(name)"
        guard let data = Assets.data("\(dir)/model.json"),
              let manifest = try? JSONDecoder().decode(Manifest.self, from: data),
              !manifest.parts.isEmpty else { return nil }

        let root = SCNNode()
        root.name = "model-\(name)"

        var loadedAny = false
        for part in manifest.parts {
            let meshURL = Assets.url("\(dir)/\(part.mesh)")
            guard FileManager.default.fileExists(atPath: meshURL.path) else { continue }

            let asset = MDLAsset(url: meshURL)
            asset.loadTextures()
            let partNode = SCNNode()
            for i in 0..<asset.count {
                partNode.addChildNode(SCNNode(mdlObject: asset.object(at: i)))
            }
            guard partNode.childNodes.isEmpty == false else { continue }

            // The manifest texture is authoritative; foliage textures carry a chroma
            // key (now baked to alpha) and must cutout-render.
            // Prefer the diffuse texture: some manifests list the engine's environment
            // 'reflection' map first, which renders near-black without env mapping.
            let texName = part.textures?.first { !$0.lowercased().contains("reflection") } ?? part.textures?.first
            if let tex = texName,
               let img = Assets.image("\(dir)/textures/\(tex)") ?? Assets.image("\(dir)/\(tex)") {
                applyTexture(img, to: partNode, cutout: img.hasTransparency)
            }
            deWash(partNode)   // stop the tropical ambient from washing the mesh pale

            if let t = part.transform, t.count == 16 {
                partNode.transform = matrix(fromRowMajor: t)
            }
            // Name by mesh stem (e.g. "barrel") so callers can rig individual parts.
            partNode.name = (part.mesh as NSString).deletingPathExtension
            root.addChildNode(partNode)
            loadedAny = true
        }
        guard loadedAny else { return nil }

        // Axis / scale normalization so importers only reason about SceneKit space once.
        if (manifest.upAxis ?? "Y").uppercased() == "Z" {
            // WildTangent models are commonly Z-up; rotate into SceneKit's Y-up.
            root.eulerAngles.x = -CGFloat.pi / 2
        }
        if let s = manifest.scale, s != 0, s != 1 {
            root.scale = SCNVector3(s, s, s)
        }
        return root
    }

    private static func applyTexture(_ img: NSImage, to node: SCNNode, cutout: Bool) {
        node.enumerateHierarchy { n, _ in
            guard let geo = n.geometry else { return }
            for m in geo.materials {
                m.diffuse.contents = img
                m.diffuse.magnificationFilter = .nearest   // keep the pixel-art crisp
                if cutout {
                    // Cutout foliage sprites (fronds, rails): alpha-tested hard edges, and
                    // unlit so the double-sided undersides keep their true color instead of
                    // washing to purple under the map's teal ambient.
                    m.lightingModel = .constant
                    m.isDoubleSided = true
                    m.transparencyMode = .aOne
                    m.writesToDepthBuffer = true
                    m.readsFromDepthBuffer = true
                    m.shaderModifiers = [.surface: "if (_surface.diffuse.a < 0.5) { discard_fragment(); }"]
                } else {
                    m.lightingModel = .lambert
                }
            }
        }
    }

    /// Cut ambient reflectance + specular so the game's bright ambient doesn't wash
    /// the decoded meshes to a pale ghost (same fix the cannon needed).
    private static func deWash(_ node: SCNNode) {
        node.enumerateHierarchy { n, _ in
            guard let geo = n.geometry else { return }
            for m in geo.materials {
                m.ambient.contents = NSColor(calibratedWhite: 0.3, alpha: 1)
                m.specular.contents = NSColor.black
            }
        }
    }

    private static func matrix(fromRowMajor t: [Float]) -> SCNMatrix4 {
        // Row-major [m00 m01 m02 m03  m10 ...] -> SceneKit column-major SCNMatrix4.
        SCNMatrix4(
            m11: CGFloat(t[0]), m12: CGFloat(t[4]), m13: CGFloat(t[8]),  m14: CGFloat(t[12]),
            m21: CGFloat(t[1]), m22: CGFloat(t[5]), m23: CGFloat(t[9]),  m24: CGFloat(t[13]),
            m31: CGFloat(t[2]), m32: CGFloat(t[6]), m33: CGFloat(t[10]), m34: CGFloat(t[14]),
            m41: CGFloat(t[3]), m42: CGFloat(t[7]), m43: CGFloat(t[11]), m44: CGFloat(t[15]))
    }
}
