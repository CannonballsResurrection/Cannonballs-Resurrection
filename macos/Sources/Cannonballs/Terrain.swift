import Foundation
import SceneKit

/// The island terrain: 96x96 heightmap, live deformation, texture decals.
/// World: 640x640 units, vertex spacing 640/96; sea level y = 0.
final class Terrain {
    static let grid = 96
    static let vertexScale: Float = 640.0 / 96.0   // ≈ 6.667
    static let maxHeight: Float = 100
    static let easeRate: Float = 30                // units/sec toward target

    private(set) var current: [Float]   // animated heights
    private(set) var target: [Float]    // deformation target
    private var dirty = true
    private var textureDirty = false

    let node = SCNNode()
    let material = SCNMaterial()
    private var textureCanvas: NSImage

    let map: MapInfo

    init(map: MapInfo) {
        self.map = map
        let g = Terrain.grid
        var heights = [Float](repeating: -4, count: g * g)
        if let data = Assets.data(map.heightmapPath), data.count >= g * g {
            for i in 0..<(g * g) {
                heights[i] = Float(data[i]) / 256.0 * map.mapScale - 4.0
            }
        }
        current = heights
        target = heights

        let base = Assets.image(map.texturePath) ?? NSImage(size: NSSize(width: 512, height: 512))
        let canvas = NSImage(size: NSSize(width: 512, height: 512))
        canvas.lockFocus()
        base.draw(in: NSRect(x: 0, y: 0, width: 512, height: 512))
        canvas.unlockFocus()
        textureCanvas = canvas

        material.diffuse.contents = textureCanvas
        material.diffuse.wrapS = .clamp
        material.diffuse.wrapT = .clamp
        material.lightingModel = .lambert
        material.isDoubleSided = false
        node.name = "terrain"
        rebuildGeometry()
    }

    // MARK: - Sampling

    /// Bilinear height at world coordinates. Outside island → -4 (deep water).
    func height(x: Float, z: Float) -> Float {
        let g = Terrain.grid
        let fx = x / Terrain.vertexScale
        let fz = z / Terrain.vertexScale
        if fx < 0 || fz < 0 || fx > Float(g - 1) || fz > Float(g - 1) { return -4 }
        let x0 = min(Int(fx), g - 2), z0 = min(Int(fz), g - 2)
        let tx = fx - Float(x0), tz = fz - Float(z0)
        let h00 = current[z0 * g + x0], h10 = current[z0 * g + x0 + 1]
        let h01 = current[(z0 + 1) * g + x0], h11 = current[(z0 + 1) * g + x0 + 1]
        return (h00 * (1 - tx) + h10 * tx) * (1 - tz) + (h01 * (1 - tx) + h11 * tx) * tz
    }

    /// Target (post-deformation) height, used for spawn validity checks.
    func targetHeight(x: Float, z: Float) -> Float {
        let g = Terrain.grid
        let gx = Int((x / Terrain.vertexScale).rounded()), gz = Int((z / Terrain.vertexScale).rounded())
        if gx < 0 || gz < 0 || gx >= g || gz >= g { return -4 }
        return target[gz * g + gx]
    }

    var worldSize: Float { Float(Terrain.grid - 1) * Terrain.vertexScale }

    // MARK: - Deformation (SPEC §2: linear cone falloff, ease 30 u/s)

    func crater(x: Float, z: Float, depth: Float, radius: Float,
                splat: NSColor? = nil) {
        forEachVertex(near: x, z: z, radius: radius) { idx, dist in
            target[idx] -= (1 - dist / radius) * depth
        }
        if let c = splat {
            stampSplat(x: x, z: z, color: c)   // SPLAT texture bake (crater weapons)
        } else {
            stampScorch(x: x, z: z)   // the ORIGINAL 32x32 SCORCH burn bake (Island.java:524)
        }
    }

    func molehill(x: Float, z: Float, height: Float, radius: Float,
                  splat: NSColor? = nil) {
        forEachVertex(near: x, z: z, radius: radius) { idx, dist in
            target[idx] = min(Terrain.maxHeight, max(0, target[idx] + (1 - dist / radius) * height))
        }
        if let c = splat { paintDecal(x: x, z: z, radius: radius, color: c) }
    }

    /// Raise terrain to at least `height` (spawn platform fallback).
    func molehillAbsolute(x: Float, z: Float, height: Float, radius: Float) {
        forEachVertex(near: x, z: z, radius: radius) { idx, dist in
            let lift = (1 - dist / radius) * height
            target[idx] = max(target[idx], lift)
        }
    }

    /// Crater applied along a line (X-Shot trenches).
    func groove(x1: Float, z1: Float, x2: Float, z2: Float, radius: Float, depth: Float) {
        let g = Terrain.grid
        let vs = Terrain.vertexScale
        for gz in 0..<g {
            for gx in 0..<g {
                let px = Float(gx) * vs, pz = Float(gz) * vs
                let d = Terrain.distanceToSegment(px: px, pz: pz, x1: x1, z1: z1, x2: x2, z2: z2)
                if d < radius {
                    target[gz * g + gx] -= (1 - d / radius) * depth
                }
            }
        }
        // paint the trench
        let steps = 16
        for i in 0...steps {
            let t = Float(i) / Float(steps)
            paintDecal(x: x1 + (x2 - x1) * t, z: z1 + (z2 - z1) * t, radius: radius * 1.5,
                       color: NSColor(calibratedWhite: 0.08, alpha: 0.35))
        }
        dirty = true
    }

    static func distanceToSegment(px: Float, pz: Float, x1: Float, z1: Float, x2: Float, z2: Float) -> Float {
        let dx = x2 - x1, dz = z2 - z1
        let lenSq = dx * dx + dz * dz
        if lenSq < 0.0001 { return sqrt((px - x1) * (px - x1) + (pz - z1) * (pz - z1)) }
        var t = ((px - x1) * dx + (pz - z1) * dz) / lenSq
        t = max(0, min(1, t))
        let cx = x1 + t * dx, cz = z1 + t * dz
        return sqrt((px - cx) * (px - cx) + (pz - cz) * (pz - cz))
    }

    private func forEachVertex(near x: Float, z: Float, radius: Float, _ body: (Int, Float) -> Void) {
        let g = Terrain.grid
        let vs = Terrain.vertexScale
        let minGX = max(0, Int((x - radius) / vs)), maxGX = min(g - 1, Int((x + radius) / vs) + 1)
        let minGZ = max(0, Int((z - radius) / vs)), maxGZ = min(g - 1, Int((z + radius) / vs) + 1)
        guard minGX <= maxGX, minGZ <= maxGZ else { return }
        for gz in minGZ...maxGZ {
            for gx in minGX...maxGX {
                let dx = Float(gx) * vs - x, dz = Float(gz) * vs - z
                let dist = sqrt(dx * dx + dz * dz)
                if dist < radius { body(gz * g + gx, dist) }
            }
        }
        dirty = true
    }

    // MARK: - Texture decals

    /// The original scorch bake: the 32x32 SCORCH texture blends the terrain map
    /// toward black (weight 1 - scorchR/255), a fixed 32-texel footprint centered
    /// on the impact (Island.java crater()).
    private static let scorchDecal: NSImage? = {
        guard let src = FXSprites.image("SCORCH"),
              let tiff = src.tiffRepresentation,
              let rep = NSBitmapImageRep(data: tiff) else { return nil }
        let out = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: 32, pixelsHigh: 32,
                                   bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true,
                                   isPlanar: false, colorSpaceName: .deviceRGB,
                                   bytesPerRow: 0, bitsPerPixel: 0)!
        for y in 0..<32 {
            for x in 0..<32 {
                let r = rep.colorAt(x: x, y: y)?.redComponent ?? 1
                out.setColor(NSColor(deviceRed: 0, green: 0, blue: 0, alpha: 1 - r), atX: x, y: y)
            }
        }
        let img = NSImage(size: NSSize(width: 32, height: 32))
        img.addRepresentation(out)
        return img
    }()

    /// SPLAT bake: like scorch but blends toward the weapon color (Island.java
    /// crater() splat path: blendPixel(x,y, 1-R/255, r,g,b)).
    private static var splatAlpha: [[CGFloat]]? = {
        guard let src = FXSprites.image("SPLAT"), let tiff = src.tiffRepresentation,
              let rep = NSBitmapImageRep(data: tiff) else { return nil }
        return (0..<32).map { y in (0..<32).map { x in 1 - (rep.colorAt(x: x, y: y)?.redComponent ?? 1) } }
    }()

    private func stampSplat(x: Float, z: Float, color: NSColor) {
        guard let alpha = Terrain.splatAlpha else {
            paintDecal(x: x, z: z, radius: 6, color: color)
            return
        }
        let decal = NSImage(size: NSSize(width: 32, height: 32))
        decal.lockFocus()
        let c = color.usingColorSpace(.deviceRGB) ?? color
        for y in 0..<32 {
            for xx in 0..<32 where alpha[y][xx] > 0.02 {
                c.withAlphaComponent(alpha[y][xx]).setFill()
                NSRect(x: xx, y: 31 - y, width: 1, height: 1).fill()
            }
        }
        decal.unlockFocus()
        let size = self.worldSize
        let px = CGFloat(x / size) * 512
        let py = CGFloat(1 - z / size) * 512
        textureCanvas.lockFocus()
        decal.draw(in: NSRect(x: px - 16, y: py - 16, width: 32, height: 32),
                   from: .zero, operation: .sourceOver, fraction: 1)
        textureCanvas.unlockFocus()
        textureDirty = true
    }

    private func stampScorch(x: Float, z: Float) {
        guard let decal = Terrain.scorchDecal else {
            paintDecal(x: x, z: z, radius: 6, color: NSColor(calibratedWhite: 0.05, alpha: 0.55))
            return
        }
        let size = self.worldSize
        let px = CGFloat(x / size) * 512
        let py = CGFloat(1 - z / size) * 512
        textureCanvas.lockFocus()
        decal.draw(in: NSRect(x: px - 16, y: py - 16, width: 32, height: 32),
                   from: .zero, operation: .sourceOver, fraction: 1)
        textureCanvas.unlockFocus()
        textureDirty = true
    }

    private func paintDecal(x: Float, z: Float, radius: Float, color: NSColor) {
        let size = self.worldSize
        // texture u ↔ x, v(image top) ↔ z=0 — matches geometry UV below
        let px = CGFloat(x / size) * 512
        let py = CGFloat(1 - z / size) * 512
        let pr = CGFloat(radius / size) * 512
        textureCanvas.lockFocus()
        let grad = NSGradient(starting: color, ending: color.withAlphaComponent(0))
        grad?.draw(in: NSBezierPath(ovalIn: NSRect(x: px - pr, y: py - pr, width: pr * 2, height: pr * 2)),
                   relativeCenterPosition: .zero)
        textureCanvas.unlockFocus()
        textureDirty = true
    }

    // MARK: - Per-frame update

    /// Ease heights toward target; rebuild mesh when needed. Returns true if terrain moved.
    @discardableResult
    func update(dt: Float) -> Bool {
        var moved = false
        let step = Terrain.easeRate * dt
        for i in 0..<current.count where current[i] != target[i] {
            let d = target[i] - current[i]
            if abs(d) <= step { current[i] = target[i] } else { current[i] += d > 0 ? step : -step }
            moved = true
        }
        if moved { dirty = true }
        if dirty { rebuildGeometry(); dirty = false }
        if textureDirty {
            material.diffuse.contents = textureCanvas
            textureDirty = false
        }
        return moved
    }

    // MARK: - Mesh

    private func rebuildGeometry() {
        let g = Terrain.grid
        let vs = Terrain.vertexScale
        var positions = [SCNVector3](); positions.reserveCapacity(g * g)
        var normals = [SCNVector3](repeating: SCNVector3(0, 1, 0), count: g * g)
        var uvs = [CGPoint](); uvs.reserveCapacity(g * g)

        for gz in 0..<g {
            for gx in 0..<g {
                positions.append(SCNVector3(Float(gx) * vs, current[gz * g + gx], Float(gz) * vs))
                uvs.append(CGPoint(x: CGFloat(gx) / CGFloat(g - 1), y: CGFloat(gz) / CGFloat(g - 1)))
            }
        }
        // normals via central differences
        for gz in 0..<g {
            for gx in 0..<g {
                let hl = current[gz * g + max(gx - 1, 0)]
                let hr = current[gz * g + min(gx + 1, g - 1)]
                let hd = current[max(gz - 1, 0) * g + gx]
                let hu = current[min(gz + 1, g - 1) * g + gx]
                var n = SIMD3<Float>(hl - hr, 2 * vs, hd - hu)
                n = simd_normalize(n)
                normals[gz * g + gx] = SCNVector3(n.x, n.y, n.z)
            }
        }
        var indices = [Int32](); indices.reserveCapacity((g - 1) * (g - 1) * 6)
        for gz in 0..<(g - 1) {
            for gx in 0..<(g - 1) {
                let a = Int32(gz * g + gx), b = a + 1
                let c = Int32((gz + 1) * g + gx), d = c + 1
                // wind counter-clockwise when viewed from +Y
                indices.append(contentsOf: [a, c, b, b, c, d])
            }
        }
        let geo = SCNGeometry(
            sources: [
                SCNGeometrySource(vertices: positions),
                SCNGeometrySource(normals: normals),
                SCNGeometrySource(textureCoordinates: uvs)
            ],
            elements: [SCNGeometryElement(indices: indices, primitiveType: .triangles)])
        geo.materials = [material]
        node.geometry = geo
    }
}
