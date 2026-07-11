import Foundation
import AppKit
import simd

/// Test harness for the software rasterizer: loads a decoded model (OBJ parts +
/// textures via its model.json) and renders it through SoftRaster to a PNG, so the
/// DX7-faithful look can be compared to SceneKit and the original.
enum RasterDemo {

    struct Part { var verts: [SoftRaster.Vertex]; var indices: [Int]; var tex: SoftRaster.Texture? }

    static func render(model name: String, to outPath: String, size: Int = 480) {
        guard let parts = loadModel(name) else {
            FileHandle.standardError.write("raster: no model \(name)\n".data(using: .utf8)!); return
        }
        // Frame the whole model.
        var lo = SIMD3<Float>(repeating: .greatestFiniteMagnitude)
        var hi = SIMD3<Float>(repeating: -.greatestFiniteMagnitude)
        for p in parts { for v in p.verts { lo = simd_min(lo, v.position); hi = simd_max(hi, v.position) } }
        let center = (lo + hi) * 0.5
        let radius = simd_length(hi - lo) * 0.5

        let raster = SoftRaster(width: size, height: size)
        raster.clear(SIMD3(0.36, 0.55, 0.78))          // sky-ish backdrop

        // 3/4 view, like the --model snapshot.
        let dir = simd_normalize(SIMD3<Float>(0.55, 0.45, 1.0))
        let eye = center + dir * (radius * 2.7)
        let view = lookAt(eye: eye, center: center, up: SIMD3(0, 1, 0))
        let proj = perspective(fovY: 0.6, aspect: 1, near: radius * 0.05, far: radius * 12)
        let vp = proj * view

        let light = SoftRaster.Light(ambient: SIMD3(repeating: 0.42),
                                     sunDir: simd_normalize(SIMD3(0.4, 0.8, 0.5)),
                                     sunColor: SIMD3(repeating: 0.85))
        for p in parts {
            raster.draw(vertices: p.verts, indices: p.indices, texture: p.tex,
                        mvp: vp, normalMatrix: matrix_identity_float3x3,
                        light: light, fog: .none)
        }
        writePNG(raster.rgba8(background: SIMD3(0.36, 0.55, 0.78)), width: size, height: size, to: outPath)
        print("raster wrote \(outPath) (\(name), \(parts.count) parts)")
    }

    // MARK: model loading (reuses the model.json manifest)

    private static func loadModel(_ name: String) -> [Part]? {
        let dir = "MODELS/\(name)"
        guard let data = Assets.data("\(dir)/model.json"),
              let man = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let partList = man["parts"] as? [[String: Any]] else { return nil }
        let scale = Float((man["scale"] as? NSNumber)?.floatValue ?? 1)
        var out: [Part] = []
        for pm in partList {
            guard let mesh = pm["mesh"] as? String else { continue }
            let objURL = Assets.url("\(dir)/\(mesh)")
            guard let text = try? String(contentsOf: objURL, encoding: .utf8) else { continue }
            var (verts, indices) = parseOBJ(text)
            for i in verts.indices { verts[i].position *= scale }
            var tex: SoftRaster.Texture? = nil
            let texList = pm["textures"] as? [String]
            let texName = texList?.first { !$0.lowercased().contains("reflection") } ?? texList?.first
            if let texName,
               let img = Assets.image("\(dir)/textures/\(texName)") ?? Assets.image("\(dir)/\(texName)") {
                tex = loadTexture(img)
            }
            out.append(Part(verts: verts, indices: indices, tex: tex))
        }
        return out.isEmpty ? nil : out
    }

    private static func parseOBJ(_ text: String) -> ([SoftRaster.Vertex], [Int]) {
        var pos: [SIMD3<Float>] = [], uvs: [SIMD2<Float>] = [], nrm: [SIMD3<Float>] = []
        var verts: [SoftRaster.Vertex] = []
        var indices: [Int] = []
        var cache: [String: Int] = [:]
        func f(_ s: Substring) -> Float { Float(s) ?? 0 }
        for line in text.split(separator: "\n") {
            let t = line.split(separator: " ")
            guard let tag = t.first else { continue }
            switch tag {
            case "v"  where t.count >= 4: pos.append(SIMD3(f(t[1]), f(t[2]), f(t[3])))
            case "vt" where t.count >= 3: uvs.append(SIMD2(f(t[1]), 1 - f(t[2])))   // OBJ v is bottom-up
            case "vn" where t.count >= 4: nrm.append(SIMD3(f(t[1]), f(t[2]), f(t[3])))
            case "f":
                var face: [Int] = []
                for j in 1..<t.count {
                    let key = String(t[j])
                    if let idx = cache[key] { face.append(idx); continue }
                    let comps = key.split(separator: "/", omittingEmptySubsequences: false)
                    let pi = (Int(comps[0]) ?? 1) - 1
                    let ti = comps.count > 1 && !comps[1].isEmpty ? (Int(comps[1]) ?? 1) - 1 : -1
                    let ni = comps.count > 2 && !comps[2].isEmpty ? (Int(comps[2]) ?? 1) - 1 : -1
                    let vv = SoftRaster.Vertex(
                        position: pi >= 0 && pi < pos.count ? pos[pi] : .zero,
                        normal: ni >= 0 && ni < nrm.count ? nrm[ni] : .zero,
                        uv: ti >= 0 && ti < uvs.count ? uvs[ti] : .zero)
                    verts.append(vv)
                    cache[key] = verts.count - 1
                    face.append(verts.count - 1)
                }
                for k in 1..<(face.count - 1) { indices += [face[0], face[k], face[k + 1]] }
            default: break
            }
        }
        // Compute smooth normals if the OBJ had none.
        if nrm.isEmpty {
            var accum = [SIMD3<Float>](repeating: .zero, count: verts.count)
            var i = 0
            while i + 2 < indices.count {
                let a = verts[indices[i]].position, b = verts[indices[i+1]].position, c = verts[indices[i+2]].position
                let fn = simd_cross(b - a, c - a)
                accum[indices[i]] += fn; accum[indices[i+1]] += fn; accum[indices[i+2]] += fn
                i += 3
            }
            for j in verts.indices {
                let n = simd_length(accum[j]) > 1e-6 ? simd_normalize(accum[j]) : SIMD3(0, 1, 0)
                verts[j].normal = n
            }
        }
        return (verts, indices)
    }

    private static func loadTexture(_ img: NSImage) -> SoftRaster.Texture? { SoftRaster.Texture.load(img) }

    // MARK: matrices + PNG

    static func perspective(fovY: Float, aspect: Float, near: Float, far: Float) -> simd_float4x4 {
        let yy = 1 / tan(fovY * 0.5), xx = yy / aspect
        let zz = far / (far - near), zw = -near * far / (far - near)
        return simd_float4x4(columns: (SIMD4(xx, 0, 0, 0), SIMD4(0, yy, 0, 0),
                                       SIMD4(0, 0, zz, 1), SIMD4(0, 0, zw, 0)))
    }
    static func lookAt(eye: SIMD3<Float>, center: SIMD3<Float>, up: SIMD3<Float>) -> simd_float4x4 {
        let z = simd_normalize(center - eye)          // left-handed (D3D), +z into screen
        let x = simd_normalize(simd_cross(up, z))
        let y = simd_cross(z, x)
        let t = SIMD3<Float>(-simd_dot(x, eye), -simd_dot(y, eye), -simd_dot(z, eye))
        return simd_float4x4(columns: (SIMD4(x.x, y.x, z.x, 0), SIMD4(x.y, y.y, z.y, 0),
                                       SIMD4(x.z, y.z, z.z, 0), SIMD4(t.x, t.y, t.z, 1)))
    }

    static func writePNG(_ rgba: [UInt8], width: Int, height: Int, to path: String) {
        let cs = CGColorSpaceCreateDeviceRGB()
        var data = rgba
        guard let ctx = CGContext(data: &data, width: width, height: height, bitsPerComponent: 8,
                                  bytesPerRow: width * 4, space: cs,
                                  bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue),
              let cg = ctx.makeImage() else { return }
        let rep = NSBitmapImageRep(cgImage: cg)
        if let png = rep.representation(using: .png, properties: [:]) {
            try? png.write(to: URL(fileURLWithPath: path))
        }
    }
}
