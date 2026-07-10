import Foundation
import SceneKit

/// Treasure chest (SPEC §5): contents rolled when collected.
final class Chest {
    var position: SIMD3<Float>
    var alive = true
    let node: SCNNode

    init(position: SIMD3<Float>) {
        self.position = position
        node = Chest.makeNode()
        node.simdPosition = position
    }

    /// Rest height on the terrain. Chest.java settled the chest upward out of the
    /// ground via a collision check after placing it; sampling the footprint's
    /// highest point does the same job — on a slope the downhill edge can no
    /// longer bury (the radius-6 collider ≈ ±4 visual footprint).
    static func restHeight(on terrain: Terrain, x: Float, z: Float) -> Float {
        var y = terrain.height(x: x, z: z)
        for (dx, dz): (Float, Float) in [(4, 0), (-4, 0), (0, 4), (0, -4)] {
            y = max(y, terrain.height(x: x + dx, z: z + dz))
        }
        return y
    }

    func reground(on terrain: Terrain) {
        position.y = Chest.restHeight(on: terrain, x: position.x, z: position.z)
        node.simdPosition = position
    }

    static func makeNode() -> SCNNode {
        let root = SCNNode()
        root.name = "chest"
        // The original chest: decoded CHEST actor with its full DECODED skeleton
        // (7 bones, matrix-palette skin from actor.wsgo) playing the original
        // "loop" hop motion — all 6 bone paths (root hop translation + lid/body
        // rotation rattle) from resources/loop.wsmo. See MOTION_FORMAT.md and
        // GEOM_MESH_FORMAT_SOLVED.md; exported by tools/wsgo_export_skinned.py.
        if let actor = SkinnedModel.load("CHEST"),
           let motion = SkinnedModel.loadMotion("MODELS/CHEST/loop_motion_full.json") {
            Cannon.styleCannonMaterials(actor.root)   // solid, crisp — no ghosting
            // NATIVE scale: the original loads the CHEST actor unscaled (Chest.java,
            // Radius=6 collision matches the ~7.6-unit mesh). The old x7 was wrong.
            // original starts each chest at a random phase (playMotion random offset)
            SkinnedModel.animate(actor, motion: motion, phase: Double.random(in: 0..<motion.duration))
            root.addChildNode(actor.root)
            let blob = FXSprites.blobShadow(radius: 5)   // original SHADOW patch (Chest.java:138)
            blob.position.y = 0.15
            root.addChildNode(blob)
        } else if let model = ModelLibrary.node(for: "CHEST") {
            // fallback: static decoded mesh + root-translation hop only
            Cannon.styleCannonMaterials(model)
            let modelScale: Float = 1
            let bob = SCNNode()
            bob.addChildNode(model)
            root.addChildNode(bob)
            if let data = Assets.data("MODELS/CHEST/loop_motion.json"),
               let j = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let times = j["times"] as? [Double], let height = j["height"] as? [Double],
               let duration = j["duration"] as? Double, duration > 0, times.count == height.count {
                let anim = CAKeyframeAnimation(keyPath: "position.y")
                anim.keyTimes = times.map { NSNumber(value: $0 / duration) }
                anim.values = height.map { CGFloat($0) * CGFloat(modelScale) }
                anim.duration = duration
                anim.repeatCount = .infinity
                anim.calculationMode = .linear
                anim.timeOffset = Double.random(in: 0..<duration)
                bob.addAnimation(anim, forKey: "loop")
            }
        } else {
            let front = PropGeometry.texMaterial("CHEST/textures/chesto.jpg", crop: (0.0, 0.0, 0.5, 0.5),
                                                 fallback: NSColor(calibratedRed: 0.7, green: 0.14, blue: 0.1, alpha: 1))
            let lidTex = PropGeometry.texMaterial("CHEST/textures/chesto.jpg", crop: (0.0, 0.5, 0.5, 0.5),
                                                  fallback: NSColor(calibratedRed: 0.85, green: 0.7, blue: 0.2, alpha: 1))
            let body = SCNNode(geometry: SCNBox(width: 6, height: 3.6, length: 4, chamferRadius: 0.3))
            body.geometry?.materials = [front]; body.position = SCNVector3(0, 1.8, 0); root.addChildNode(body)
            let lid = SCNNode(geometry: SCNBox(width: 6, height: 1.8, length: 4, chamferRadius: 0.8))
            lid.geometry?.materials = [lidTex]; lid.position = SCNVector3(0, 4.2, 0); root.addChildNode(lid)
        }
        return root
    }

    /// Roll contents: 30% 100g, 30% 250g, 20% 500g, 10% 1000g, 5% 1500g, 5% teleport shooter.
    enum Treasure { case gold(Int); case teleport }
    static func rollTreasure() -> Treasure {
        let r = Double.random(in: 0..<1)
        switch r {
        case ..<0.30: return .gold(100)
        case ..<0.60: return .gold(250)
        case ..<0.80: return .gold(500)
        case ..<0.90: return .gold(1000)
        case ..<0.95: return .gold(1500)
        default: return .teleport
        }
    }
}
