import Foundation
import SceneKit

/// World-dressing layers from the original engine (Island.java + Entity_Object_LensFlare.java):
/// - WATERANIMATION: the water tile cycles through 32 64px frames at 0.08s/frame
/// - Shoreline: two slightly tilted, bobbing, scale-pulsing additive water planes
///   (both 1.2x island size — Island.java:750 attaches WaterMesh2 to both levels)
///   lapping the island edge (SHORELINE base is pure black; the visible shimmer
///   is the water layer at 10x tiling)
/// - Cloud shadows: CLOUDSHADOW multiplied over the terrain at 4x tiling,
///   drifting with the wind (offset -= wind * 0.01 * dt)
/// - Lens flare: SUN + FLARE + CIRCLE1/2 + DOT1/2 camera-space elements along
///   the sun-to-screen-center axis on maps with hasSun
enum WorldDressing {

    // MARK: - Animated water (Island.setWaterFrame, 32 frames @ 0.08s)

    static func waterTiles() -> [NSImage] {
        guard let sheet = FXSprites.image("WATERANIMATION") else { return [] }
        var tiles: [NSImage] = []
        for n in 0..<32 {
            let col = n % 8, row = n / 8
            let tile = NSImage(size: NSSize(width: 64, height: 64))
            tile.lockFocus()
            // NSImage y-origin is bottom-left; sheet rows count from the top
            sheet.draw(in: NSRect(x: 0, y: 0, width: 64, height: 64),
                       from: NSRect(x: col * 64, y: Int(sheet.size.height) - (row + 1) * 64,
                                    width: 64, height: 64),
                       operation: .copy, fraction: 1)
            tile.unlockFocus()
            tiles.append(tile)
        }
        return tiles
    }

    /// Cycle the water material through the original animation frames.
    static func animateWater(_ material: SCNMaterial) {
        let tiles = waterTiles()
        guard tiles.count == 32 else { return }
        var frame = 0
        let timer = Timer(timeInterval: 0.08, repeats: true) { [weak material] t in
            guard let material else { t.invalidate(); return }
            frame = (frame + 1) % 32
            material.diffuse.contents = tiles[frame]
        }
        RunLoop.main.add(timer, forMode: .common)
    }

    // MARK: - Shoreline planes (Island.java:735-753, 332-336)

    static func addShoreline(to world: World) {
        let size = CGFloat(world.terrain.worldSize)
        let cx = CGFloat(world.center.x), cz = CGFloat(world.center.z)
        // BOTH planes render the 1.2x mesh: Island.java:750 attaches WaterMesh2
        // (Width * 1.2, :737) to WaterLevel3 as well; the 1.15x WaterMesh3
        // (:740) is created but never attached.
        // Per-frame (Island.java:332-336): each plane bobs baseY + SinTable[3] *
        // 0.5; WaterLevel2 (tilt -3°, baseY 0.75) pulses its plane axes by
        // SinTable[4] * (0.1, 0.09), WaterLevel3 (tilt 5°, baseY 0.65) by
        // SinTable[7] * (0.095, 0.1). SinTable rates from Main.java:307-324:
        // SinPosition[3] += 1.0/s, SinPosition[4] += 1.5/s, and SinTable[7] =
        // sin(SinPosition[3]) — a decompile-visible quirk (Main.java:323) that
        // runs [7] at 1.0/s.
        let params: [(tilt: CGFloat, baseY: CGFloat, rate: CGFloat, pulseX: CGFloat, pulseY: CGFloat)] =
            [(-3.0, 0.75, 1.5, 0.1, 0.09), (5.0, 0.65, 1.0, 0.095, 0.1)]
        for p in params {
            let plane = SCNPlane(width: size * 1.2, height: size * 1.2)
            let m = SCNMaterial()
            m.diffuse.contents = PropTex.image("water.png") ?? Assets.image("PROPTEX/water.png")
            m.diffuse.wrapS = .repeat; m.diffuse.wrapT = .repeat
            m.diffuse.contentsTransform = SCNMatrix4MakeScale(10, 10, 1)   // layer-1 10x tiling
            m.lightingModel = .constant
            m.blendMode = .add
            // add-blend ignores material transparency: darken the source instead
            m.multiply.contents = NSColor(calibratedWhite: 0.13, alpha: 1)
            m.writesToDepthBuffer = false
            plane.materials = [m]
            let node = SCNNode(geometry: plane)
            node.name = "shoreline"
            node.castsShadow = false
            node.renderingOrder = 5
            node.eulerAngles = SCNVector3(-CGFloat.pi / 2, 0, p.tilt * .pi / 180)
            node.position = SCNVector3(cx, p.baseY, cz)
            world.scene.rootNode.addChildNode(node)
            // customAction's elapsed time is real seconds, so the SinTable
            // positions advance by dt like the original's updateSinTable
            node.runAction(.repeatForever(.customAction(duration: 1e9) { n, t in
                n.position.y = p.baseY + sin(t) * 0.5          // baseY + SinTable[3] * 0.5
                let s = sin(p.rate * t)                        // SinTable[4] / [7]
                n.scale = SCNVector3(1 + s * p.pulseX, 1 + s * p.pulseY, 1)
            }))
        }
    }

    // MARK: - Cloud shadows (terrain layer 2: multiply, 4x tiling, wind drift)

    static func addCloudShadows(to material: SCNMaterial) {
        guard let img = FXSprites.image("CLOUDSHADOW"), let grit = FXSprites.image("GRIT") else { return }
        let prop = SCNMaterialProperty(contents: img)
        prop.wrapS = .repeat; prop.wrapT = .repeat
        material.setValue(prop, forKey: "cloudShadow")
        let gritProp = SCNMaterialProperty(contents: grit)
        gritProp.wrapS = .repeat; gritProp.wrapT = .repeat
        material.setValue(gritProp, forKey: "gritTex")
        material.setValue(NSValue(point: .zero), forKey: "cloudOffset")
        // layer 1: GRIT detail at 40x tiling (normalized by its mean 0.647 to keep
        // brightness); layer 2: CLOUDSHADOW multiply at 4x, wind-drifting
        let mod = """
        #pragma arguments
        texture2d<float> cloudShadow;
        texture2d<float> gritTex;
        float2 cloudOffset;
        #pragma body
        constexpr sampler dressSampler(filter::linear, address::repeat);
        _surface.diffuse.rgb *= gritTex.sample(dressSampler, _surface.diffuseTexcoord * 40.0).rgb / 0.647;
        _surface.diffuse.rgb *= cloudShadow.sample(dressSampler, _surface.diffuseTexcoord * 4.0 + cloudOffset).rgb;
        """
        var mods = material.shaderModifiers ?? [:]
        mods[.surface] = (mods[.surface] ?? "") + mod
        material.shaderModifiers = mods
    }

    static func driftClouds(_ material: SCNMaterial, wind: SIMD3<Float>, dt: Float,
                            offset: inout SIMD2<Float>) {
        offset.x -= wind.x * dt * 0.01
        offset.y += wind.z * dt * 0.01
        offset.x -= floor(offset.x); offset.y -= floor(offset.y)
        material.setValue(NSValue(point: CGPoint(x: CGFloat(offset.x), y: CGFloat(offset.y))),
                          forKey: "cloudOffset")
    }

    // MARK: - Lens flare (Entity_Object_LensFlare.java)

    final class LensFlare {
        private let group = SCNNode()          // camera-space container at depth D
        private var elements: [SCNNode] = []   // [sun, flare, circle1, circle2, dot1, dot2]
        private var fade: Float = 0
        private let sunDir: SIMD3<Float>
        private static let depth: CGFloat = 20

        // (image, WT size, offset factor along sun->center axis)
        private static let spec: [(String, CGFloat, CGFloat)] = [
            ("FX_SUN", 0.219, 1.0), ("FX_FLARE", 1.368, 1.0),
            ("FX_CIRCLE1", 0.383, 0.3), ("FX_CIRCLE2", 1.368, 0.0),
            ("FX_DOT1", 0.068, 0.5), ("FX_DOT2", 0.082, -0.5),
        ]

        init(sunVector: SIMD3<Float>, camera: SCNNode) {
            sunDir = simd_normalize(sunVector)
            for (img, size, _) in LensFlare.spec {
                let s = size * LensFlare.depth / 2
                let plane = SCNPlane(width: s, height: s)
                let m = SCNMaterial()
                m.diffuse.contents = FXSprites.image(img)
                m.lightingModel = .constant
                m.blendMode = .add
                m.writesToDepthBuffer = false
                m.readsFromDepthBuffer = false
                plane.materials = [m]
                let n = SCNNode(geometry: plane)
                n.castsShadow = false
                n.renderingOrder = 1000
                group.addChildNode(n)
                elements.append(n)
            }
            group.position = SCNVector3(0, 0, -LensFlare.depth)
            camera.addChildNode(group)
            group.opacity = 0
        }

        func update(pov: SCNNode, dt: Float, terrain: Terrain? = nil) {
            guard let cam = pov.camera else { return }
            // sun direction in camera space (SceneKit camera looks down -z)
            let d = simd_normalize(pov.presentation.simdConvertVector(sunDir, from: nil))
            var visible: Bool
            var sx: Float = 0, sy: Float = 0
            if d.z < 0 {                                   // in front (SceneKit -z forward)
                let fov = Float(cam.fieldOfView) * .pi / 180
                let halfH = tan(fov / 2)
                // note: fieldOfView is vertical by default projectionDirection
                sx = (d.x / -d.z) / halfH
                sy = (d.y / -d.z) / halfH
                visible = abs(sx) < 1.4 && abs(sy) < 1.1
            } else {
                visible = false
            }
            // terrain occlusion (original: collision ray toward the sun)
            if visible, let terrain {
                let camPos = pov.presentation.simdWorldPosition
                var t: Float = 10
                while t < 400 {
                    let p = camPos + sunDir * t
                    if terrain.height(x: p.x, z: p.z) > p.y { visible = false; break }
                    t += 20
                }
            }
            fade += (visible ? 4 : -4) * dt
            fade = max(0, min(1, fade))
            group.opacity = CGFloat(fade)
            guard fade > 0 else { return }
            let halfH = LensFlare.depth * CGFloat(tan(Float(cam.fieldOfView) * .pi / 360))
            let px = CGFloat(sx) * halfH, py = CGFloat(sy) * halfH
            let dist = min(50, Int(sqrt(sx * sx * 4 + sy * sy * 4) * 25))
            let elementAlpha = CGFloat(max(0, 200 - dist * 4)) / 255
            for (i, (_, _, k)) in LensFlare.spec.enumerated() {
                elements[i].position = SCNVector3(px * k, py * k, 0)
                elements[i].opacity = i == 0 ? CGFloat(max(0, 250 - dist * 2)) / 255 : elementAlpha
            }
            elements[0].eulerAngles.z = CGFloat(sx) * 20 * .pi / 180
        }
    }
}
