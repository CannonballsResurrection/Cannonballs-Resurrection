import Foundation
import SceneKit
import AppKit
import simd

/// Bridges the live SceneKit world into the software rasterizer: walks an SCNScene,
/// extracts every geometry's vertices/normals/uvs/indices + diffuse texture and world
/// transform, builds the camera matrices, and renders one frame through SoftRaster so
/// the 3D world gets the exact DX7/Genesis3D look. Used both for offscreen snapshots
/// (--rasterscene) and, later, the live per-frame path.
enum SceneRasterizer {

    /// Per-map lighting/fog/sky for the rasterizer, from the same data the world uses.
    struct Settings { var light: SoftRaster.Light; var fog: SoftRaster.Fog
                      var skyTop: SIMD3<Float>; var skyBottom: SIMD3<Float>; var gamma: Float }
    static func settings(for map: MapInfo) -> Settings {
        func v3(_ t: (Float, Float, Float)) -> SIMD3<Float> { SIMD3(t.0, t.1, t.2) }
        let amb = v3(map.ambientRGB)
        let light = SoftRaster.Light(ambient: amb * 0.7 + SIMD3(repeating: 0.18),
                                     sunDir: simd_normalize(map.sunVector),
                                     sunColor: v3(map.sunRGB))
        let horizon = amb * 0.6 + SIMD3(repeating: 0.4)          // fog + lower sky
        let fog = SoftRaster.Fog(enabled: true, start: 420, end: 1800, color: horizon)
        let skyTop = amb * 0.30 + SIMD3(0.30, 0.5, 0.85) * 0.70  // zenith blue
        return Settings(light: light, fog: fog, skyTop: skyTop, skyBottom: horizon, gamma: 1.15)
    }

    /// An extracted, thread-safe snapshot of the scene's drawable geometry + camera.
    /// Built on the render thread (reads the live SCNScene); rasterized on any thread.
    struct Snapshot { fileprivate let calls: [DrawCall]; let size: CGSize }
    fileprivate struct DrawCall { let mesh: Mesh; let mat: Mat; let mvp: simd_float4x4; let nrm: simd_float3x3 }

    /// Render-thread step: pull geometry + camera into plain value types (textures are
    /// immutable after load), so the heavy fill can run off-thread without racing SceneKit.
    static func snapshot(scene: SCNScene, pov: SCNNode, size: CGSize) -> Snapshot {
        let w = Int(size.width), h = Int(size.height)
        let view = simd_inverse(pov.simdWorldTransform)
        let cam = pov.camera ?? SCNCamera()
        let fovY = Float(cam.fieldOfView) * .pi / 180
        let proj = glPerspective(fovY: fovY, aspect: Float(w) / Float(h),
                                 near: max(0.05, Float(cam.zNear)), far: Float(cam.zFar))
        let viewProj = proj * view
        var calls: [DrawCall] = []
        scene.rootNode.enumerateHierarchy { node, _ in
            guard let geo = node.geometry, !node.isHidden, node.opacity > 0.01 else { return }
            // additive light effects (lens flare, lightbeam, shoreline shimmer, FX
            // billboards) have no software add-blend path: skip, don't draw opaque black
            if geo.materials.first?.blendMode == .add { return }
            let model = node.simdWorldTransform
            let mvp = viewProj * model
            let nrm = normalMatrix(model)
            for (mesh, mat) in extract(geo) {
                calls.append(DrawCall(mesh: mesh, mat: mat, mvp: mvp, nrm: nrm))
            }
        }
        return Snapshot(calls: calls, size: size)
    }

    /// Pure compute: rasterize a snapshot with a sky gradient. Safe off the main thread.
    static func rasterize(_ snap: Snapshot, settings s: Settings) -> SoftRaster {
        let raster = SoftRaster(width: Int(snap.size.width), height: Int(snap.size.height))
        raster.clearSky(top: s.skyTop, bottom: s.skyBottom)
        for c in snap.calls {
            raster.draw(vertices: c.mesh.verts, indices: c.mesh.indices, texture: c.mat.texture,
                        mvp: c.mvp, normalMatrix: c.nrm, light: s.light, fog: s.fog,
                        tint: c.mat.tint, unlit: c.mat.unlit)
        }
        return raster
    }

    /// Convenience for offscreen CLIs (extract + rasterize together).
    static func renderFrame(scene: SCNScene, pov: SCNNode, size: CGSize, settings s: Settings) -> SoftRaster {
        rasterize(snapshot(scene: scene, pov: pov, size: size), settings: s)
    }

    // MARK: - geometry extraction

    fileprivate struct Mesh { var verts: [SoftRaster.Vertex]; var indices: [Int] }
    fileprivate struct Mat { var texture: SoftRaster.Texture?; var tint: SIMD3<Float>; var unlit: Bool = false }

    /// One (mesh, material) per geometry element (each element = one material).
    private static func extract(_ geo: SCNGeometry) -> [(Mesh, Mat)] {
        guard let vtxSrc = geo.sources(for: .vertex).first else { return [] }
        let positions = readVec3(vtxSrc)
        let normals = geo.sources(for: .normal).first.map(readVec3) ?? []
        let uvs = geo.sources(for: .texcoord).first.map(readVec2) ?? []

        var result: [(Mesh, Mat)] = []
        for (ei, element) in geo.elements.enumerated() where element.primitiveType == .triangles {
            let idx = readIndices(element)
            // Build a compact vertex list for this element.
            var verts = [SoftRaster.Vertex](); verts.reserveCapacity(positions.count)
            for i in 0..<positions.count {
                verts.append(SoftRaster.Vertex(
                    position: positions[i],
                    normal: i < normals.count ? normals[i] : SIMD3(0, 1, 0),
                    uv: i < uvs.count ? uvs[i] : .zero))
            }
            let mat = ei < geo.materials.count ? material(geo.materials[ei]) : material(geo.firstMaterial)
            result.append((Mesh(verts: verts, indices: idx), mat))
        }
        return result
    }

    private static func material(_ m: SCNMaterial?) -> Mat {
        guard let m else { return Mat(texture: nil, tint: .one) }
        let unlit = m.lightingModel == .constant     // foliage / water / sky are fullbright
        var tint = SIMD3<Float>.one
        if let mc = m.multiply.contents as? NSColor, let rc = mc.usingColorSpace(.deviceRGB) {
            tint = SIMD3(Float(rc.redComponent), Float(rc.greenComponent), Float(rc.blueComponent))
        }
        let contents = m.diffuse.contents
        if let img = contents as? NSImage {
            return Mat(texture: texture(img), tint: tint, unlit: unlit)
        }
        if let cg = contents, CFGetTypeID(cg as CFTypeRef) == CGImage.typeID {
            return Mat(texture: texture(nsImage: NSImage(cgImage: cg as! CGImage, size: .zero)), tint: tint, unlit: unlit)
        }
        if let c = contents as? NSColor, let rc = c.usingColorSpace(.deviceRGB) {
            return Mat(texture: nil,
                       tint: SIMD3(Float(rc.redComponent), Float(rc.greenComponent), Float(rc.blueComponent)),
                       unlit: unlit)
        }
        return Mat(texture: nil, tint: tint, unlit: unlit)
    }

    // texture cache (keyed by object identity of the NSImage)
    private static var texCache: [ObjectIdentifier: SoftRaster.Texture] = [:]
    private static func texture(_ img: NSImage) -> SoftRaster.Texture? {
        let key = ObjectIdentifier(img)
        if let t = texCache[key] { return t }
        let t = texture(nsImage: img)
        if let t { texCache[key] = t }
        return t
    }
    private static func texture(nsImage img: NSImage) -> SoftRaster.Texture? {
        SoftRaster.Texture.load(img)
    }

    // MARK: - SCNGeometrySource / element readers (handle stride + offset)

    private static func readVec3(_ s: SCNGeometrySource) -> [SIMD3<Float>] {
        var out = [SIMD3<Float>](repeating: .zero, count: s.vectorCount)
        let stride = s.dataStride, offset = s.dataOffset
        s.data.withUnsafeBytes { raw in
            let base = raw.baseAddress!
            for i in 0..<s.vectorCount {
                let p = base.advanced(by: offset + i * stride).assumingMemoryBound(to: Float.self)
                out[i] = SIMD3(p[0], p[1], p[2])
            }
        }
        return out
    }
    private static func readVec2(_ s: SCNGeometrySource) -> [SIMD2<Float>] {
        var out = [SIMD2<Float>](repeating: .zero, count: s.vectorCount)
        let stride = s.dataStride, offset = s.dataOffset
        s.data.withUnsafeBytes { raw in
            let base = raw.baseAddress!
            for i in 0..<s.vectorCount {
                let p = base.advanced(by: offset + i * stride).assumingMemoryBound(to: Float.self)
                out[i] = SIMD2(p[0], p[1])
            }
        }
        return out
    }
    private static func readIndices(_ e: SCNGeometryElement) -> [Int] {
        let count = e.primitiveCount * 3
        var out = [Int](repeating: 0, count: count)
        let bpi = e.bytesPerIndex
        e.data.withUnsafeBytes { raw in
            let base = raw.baseAddress!
            for i in 0..<count {
                switch bpi {
                case 2: out[i] = Int(base.advanced(by: i * 2).assumingMemoryBound(to: UInt16.self).pointee)
                case 4: out[i] = Int(base.advanced(by: i * 4).assumingMemoryBound(to: UInt32.self).pointee)
                default: out[i] = Int(base.advanced(by: i).assumingMemoryBound(to: UInt8.self).pointee)
                }
            }
        }
        return out
    }

    // MARK: - matrices

    static func glPerspective(fovY: Float, aspect: Float, near: Float, far: Float) -> simd_float4x4 {
        let f = 1 / tan(fovY * 0.5)
        return simd_float4x4(columns: (
            SIMD4(f / aspect, 0, 0, 0),
            SIMD4(0, f, 0, 0),
            SIMD4(0, 0, (far + near) / (near - far), -1),
            SIMD4(0, 0, (2 * far * near) / (near - far), 0)))
    }
    private static func normalMatrix(_ m: simd_float4x4) -> simd_float3x3 {
        let u = simd_float3x3(columns: (SIMD3(m.columns.0.x, m.columns.0.y, m.columns.0.z),
                                        SIMD3(m.columns.1.x, m.columns.1.y, m.columns.1.z),
                                        SIMD3(m.columns.2.x, m.columns.2.y, m.columns.2.z)))
        return simd_transpose(simd_inverse(u))
    }
}
