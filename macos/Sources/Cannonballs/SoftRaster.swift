import Foundation
import AppKit
import simd

/// A software rasterizer that reproduces WebDriver / Genesis3D's DirectX 7
/// fixed-function pipeline (see format-research/RASTERIZER_SPEC.md), so the clone
/// renders the 3D world with the exact 2002 look instead of SceneKit's modern one:
///
///  - Gouraud-interpolated, engine-baked vertex color (no per-pixel lighting, no specular)
///  - texel × vertexColor MODULATE
///  - bilinear, perspective-correct texture sampling
///  - color-key transparency, no backface culling
///  - z-buffer (LESSEQUAL), per-pixel linear fog
///  - RGB565 output with 4×4 ordered dither, native res then nearest upscale
///
/// It renders into an RGBA8 buffer you can hand to CoreGraphics / a texture.
final class SoftRaster {

    // MARK: types

    struct Vertex {                 // model space
        var position: SIMD3<Float>
        var normal: SIMD3<Float>
        var uv: SIMD2<Float>
    }

    struct Texture {
        let width: Int
        let height: Int
        let rgba: [SIMD4<Float>]    // 0..1, row-major, top-left origin
        var hasAlpha: Bool = false

        /// Fast, format-safe loader: draw the image once into a known RGBA8
        /// (premultiplied-last) buffer, then read straight bytes. (colorAt() per
        /// pixel is ~100x slower; reading raw NSBitmapImageRep risks channel-order bugs.)
        static func load(_ img: NSImage) -> Texture? {
            guard let cg = img.cgImage(forProposedRect: nil, context: nil, hints: nil) else { return nil }
            let w = cg.width, h = cg.height
            guard w > 0, h > 0 else { return nil }
            var buf = [UInt8](repeating: 0, count: w * h * 4)
            let cs = CGColorSpaceCreateDeviceRGB()
            guard let ctx = CGContext(data: &buf, width: w, height: h, bitsPerComponent: 8,
                                      bytesPerRow: w * 4, space: cs,
                                      bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue) else { return nil }
            ctx.draw(cg, in: CGRect(x: 0, y: 0, width: w, height: h))
            let inv: Float = 1.0 / 255.0
            var rgba = [SIMD4<Float>](repeating: .one, count: w * h)
            var hasAlpha = false
            for i in 0..<(w * h) {
                let o = i * 4
                let a = Float(buf[o + 3]) * inv
                if a < 0.99 { hasAlpha = true }
                // un-premultiply so MODULATE gets straight color (a==0 pixels are discarded anyway)
                let s = a > 0.001 ? inv / a : inv
                rgba[i] = SIMD4(min(1, Float(buf[o]) * s), min(1, Float(buf[o + 1]) * s),
                                min(1, Float(buf[o + 2]) * s), a)
            }
            return Texture(width: w, height: h, rgba: rgba, hasAlpha: hasAlpha)
        }

        @inline(__always) func texel(_ x: Int, _ y: Int) -> SIMD4<Float> {
            rgba[(y % height + height) % height * width + (x % width + width) % width]
        }
        /// Bilinear sample; uv in [0,1], v top-down.
        @inline(__always) func sample(_ u: Float, _ v: Float) -> SIMD4<Float> {
            let fx = u * Float(width) - 0.5, fy = v * Float(height) - 0.5
            let x0 = Int(floor(fx)), y0 = Int(floor(fy))
            let dx = fx - Float(x0), dy = fy - Float(y0)
            let c00 = texel(x0, y0), c10 = texel(x0 + 1, y0)
            let c01 = texel(x0, y0 + 1), c11 = texel(x0 + 1, y0 + 1)
            let top = c00 * (1 - dx) + c10 * dx
            let bot = c01 * (1 - dx) + c11 * dx
            return top * (1 - dy) + bot * dy
        }
    }

    struct Light {                  // engine-baked vertex lighting inputs
        var ambient: SIMD3<Float>
        var sunDir: SIMD3<Float>    // toward the sun, normalized
        var sunColor: SIMD3<Float>
    }

    struct Fog {
        var enabled: Bool
        var start: Float
        var end: Float
        var color: SIMD3<Float>
        static let none = Fog(enabled: false, start: 0, end: 1, color: .zero)
    }

    // MARK: framebuffer

    let width: Int
    let height: Int
    private var color: [SIMD3<Float>]   // linear RGB accumulation
    private var depth: [Float]          // stored as 1/w (bigger = nearer); LESSEQUAL on w
    private var covered: [Bool]

    init(width: Int, height: Int) {
        self.width = width; self.height = height
        color = .init(repeating: .zero, count: width * height)
        depth = .init(repeating: .greatestFiniteMagnitude, count: width * height)
        covered = .init(repeating: false, count: width * height)
    }

    func clear(_ c: SIMD3<Float>) {
        for i in 0..<color.count { color[i] = c; depth[i] = .greatestFiniteMagnitude; covered[i] = false }
    }

    /// Vertical sky gradient (zenith at top → horizon at bottom), like the engine's sky dome.
    func clearSky(top: SIMD3<Float>, bottom: SIMD3<Float>) {
        for py in 0..<height {
            let t = Float(py) / Float(max(1, height - 1))
            let c = top + (bottom - top) * t
            let row = py * width
            for px in 0..<width { color[row + px] = c; depth[row + px] = .greatestFiniteMagnitude; covered[row + px] = false }
        }
    }

    // MARK: draw

    /// Draw a mesh. `mvp` = projection * view * model (column-major, clip = mvp * [pos,1]).
    /// `normalMatrix` transforms normals to world space for lighting.
    func draw(vertices: [Vertex], indices: [Int], texture: Texture?,
              mvp: simd_float4x4, normalMatrix: simd_float3x3,
              light: Light, fog: Fog, tint: SIMD3<Float> = .one, unlit: Bool = false) {
        // Transform to clip space + bake vertex color once (engine pre-lighting).
        var clip = [SIMD4<Float>](repeating: .zero, count: vertices.count)
        var lit = [SIMD3<Float>](repeating: .zero, count: vertices.count)
        for i in 0..<vertices.count {
            let v = vertices[i]
            clip[i] = mvp * SIMD4(v.position, 1)
            if unlit {
                lit[i] = tint                          // .constant materials: fullbright
            } else {
                let n = simd_normalize(normalMatrix * v.normal)
                let ndl = max(0, simd_dot(n, light.sunDir))
                lit[i] = simd_clamp(light.ambient + ndl * light.sunColor, .zero, .one) * tint
            }
        }
        var i = 0
        while i + 2 < indices.count {
            triangle(clip[indices[i]], clip[indices[i + 1]], clip[indices[i + 2]],
                     vertices[indices[i]].uv, vertices[indices[i + 1]].uv, vertices[indices[i + 2]].uv,
                     lit[indices[i]], lit[indices[i + 1]], lit[indices[i + 2]],
                     texture, fog)
            i += 3
        }
    }

    // One attribute-bundle per vertex for clipping.
    private struct CV { var clip: SIMD4<Float>; var uv: SIMD2<Float>; var col: SIMD3<Float> }

    private func triangle(_ a: SIMD4<Float>, _ b: SIMD4<Float>, _ c: SIMD4<Float>,
                          _ uva: SIMD2<Float>, _ uvb: SIMD2<Float>, _ uvc: SIMD2<Float>,
                          _ ca: SIMD3<Float>, _ cb: SIMD3<Float>, _ cc: SIMD3<Float>,
                          _ tex: Texture?, _ fog: Fog) {
        // Near-plane clip (w > epsilon) via Sutherland-Hodgman, then fan-triangulate.
        let near: Float = 1e-4
        var poly = [CV(clip: a, uv: uva, col: ca), CV(clip: b, uv: uvb, col: cb), CV(clip: c, uv: uvc, col: cc)]
        var out: [CV] = []
        for k in 0..<poly.count {
            let cur = poly[k], prv = poly[(k + poly.count - 1) % poly.count]
            let curIn = cur.clip.w > near, prvIn = prv.clip.w > near
            if curIn != prvIn {
                let t = (near - prv.clip.w) / (cur.clip.w - prv.clip.w)
                out.append(CV(clip: mix(prv.clip, cur.clip, t: t),
                              uv: mix(prv.uv, cur.uv, t: t), col: mix(prv.col, cur.col, t: t)))
            }
            if curIn { out.append(cur) }
        }
        poly = out
        if poly.count < 3 { return }
        for k in 1..<(poly.count - 1) {
            fill(poly[0], poly[k], poly[k + 1], tex, fog)
        }
    }

    private func fill(_ v0: CV, _ v1: CV, _ v2: CV, _ tex: Texture?, _ fog: Fog) {
        // Perspective divide + viewport (no backface cull → draw both windings).
        func screen(_ v: CV) -> (x: Float, y: Float, invW: Float, z: Float) {
            let iw = 1 / v.clip.w
            let x = (v.clip.x * iw * 0.5 + 0.5) * Float(width)
            let y = (1 - (v.clip.y * iw * 0.5 + 0.5)) * Float(height)
            return (x, y, iw, v.clip.z * iw)          // z in NDC for reference; depth uses view w
        }
        let s0 = screen(v0), s1 = screen(v1), s2 = screen(v2)
        // Skip degenerate/non-finite triangles (guards Int(floor(NaN)) crashes).
        guard s0.x.isFinite, s0.y.isFinite, s1.x.isFinite, s1.y.isFinite,
              s2.x.isFinite, s2.y.isFinite else { return }
        let minX = max(0, Int(floor(min(s0.x, s1.x, s2.x))))
        let maxX = min(width - 1, Int(ceil(max(s0.x, s1.x, s2.x))))
        let minY = max(0, Int(floor(min(s0.y, s1.y, s2.y))))
        let maxY = min(height - 1, Int(ceil(max(s0.y, s1.y, s2.y))))
        if minX > maxX || minY > maxY { return }

        let area = edge(s0.x, s0.y, s1.x, s1.y, s2.x, s2.y)
        if abs(area) < 1e-7 { return }
        let invArea = 1 / area

        // Perspective-correct: interpolate attr*invW and invW, divide per pixel.
        let iw0 = s0.invW, iw1 = s1.invW, iw2 = s2.invW
        let uv0 = v0.uv * iw0, uv1 = v1.uv * iw1, uv2 = v2.uv * iw2
        let col0 = v0.col * iw0, col1 = v1.col * iw1, col2 = v2.col * iw2

        for py in minY...maxY {
            let yc = Float(py) + 0.5
            for px in minX...maxX {
                let xc = Float(px) + 0.5
                // Signed edge functions; inside (either winding) = all share the area's sign.
                let e0 = edge(s1.x, s1.y, s2.x, s2.y, xc, yc)
                let e1 = edge(s2.x, s2.y, s0.x, s0.y, xc, yc)
                let e2 = edge(s0.x, s0.y, s1.x, s1.y, xc, yc)
                if area > 0 { if e0 < 0 || e1 < 0 || e2 < 0 { continue } }
                else        { if e0 > 0 || e1 > 0 || e2 > 0 { continue } }
                // Normalized barycentrics (sum to 1, all >= 0 inside).
                let bw0 = e0 * invArea, bw1 = e1 * invArea, bw2 = e2 * invArea
                let invW = bw0 * iw0 + bw1 * iw1 + bw2 * iw2
                if invW <= 0 { continue }
                let wpix = 1 / invW                       // ~ view-space w (depth)
                let idx = py * width + px
                if wpix > depth[idx] { continue }         // LESSEQUAL depth on w

                let u = (bw0 * uv0.x + bw1 * uv1.x + bw2 * uv2.x) * wpix
                let v = (bw0 * uv0.y + bw1 * uv1.y + bw2 * uv2.y) * wpix
                var rgb = (bw0 * col0 + bw1 * col1 + bw2 * col2) * wpix

                if let tex {
                    let t = tex.sample(u, v)
                    if t.w < 0.5 { continue }             // color-key / alpha-test cutout
                    rgb *= SIMD3(t.x, t.y, t.z)           // MODULATE texel × vertex color
                }
                if fog.enabled {
                    let f = simd_clamp((fog.end - wpix) / (fog.end - fog.start), 0, 1)
                    rgb = mix(fog.color, rgb, t: f)
                }
                color[idx] = rgb
                depth[idx] = wpix
                covered[idx] = true
            }
        }
    }

    @inline(__always) private func edge(_ ax: Float, _ ay: Float, _ bx: Float, _ by: Float,
                                        _ cx: Float, _ cy: Float) -> Float {
        (cx - ax) * (by - ay) - (cy - ay) * (bx - ax)
    }

    // MARK: output — RGB565 + 4×4 ordered dither

    private static let bayer4: [Float] = {
        let m: [Int] = [0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5]
        return m.map { (Float($0) + 0.5) / 16.0 - 0.5 }   // -0.5..0.5
    }()

    /// Quantize the framebuffer to RGB565 with ordered dither and return RGBA8.
    /// `background` fills any pixel never written (when the buffer was flat-cleared);
    /// with clearSky the sky already lives in the buffer. `gamma` brightens toward the
    /// engine's slightly lifted output (≈1.15).
    func rgba8(background: SIMD3<Float>, gamma: Float = 1.0) -> [UInt8] {
        var out = [UInt8](repeating: 255, count: width * height * 4)
        let ig = gamma == 1.0 ? 1.0 : 1.0 / gamma
        for py in 0..<height {
            for px in 0..<width {
                let idx = py * width + px
                var c = covered[idx] ? color[idx] : color[idx]   // sky/gradient lives in the buffer
                if gamma != 1.0 { c = SIMD3(pow(c.x, ig), pow(c.y, ig), pow(c.z, ig)) }
                _ = background
                let d = SoftRaster.bayer4[(py & 3) * 4 + (px & 3)]
                // 565: 5 bits R (×31), 6 bits G (×63), 5 bits B (×31)
                let r = quant(c.x, levels: 31, dither: d)
                let g = quant(c.y, levels: 63, dither: d)
                let b = quant(c.z, levels: 31, dither: d)
                let o = idx * 4
                out[o] = UInt8(r * 255 / 31)
                out[o + 1] = UInt8(g * 255 / 63)
                out[o + 2] = UInt8(b * 255 / 31)
                out[o + 3] = 255
            }
        }
        return out
    }

    @inline(__always) private func quant(_ v: Float, levels: Int, dither: Float) -> Int {
        let s = v * Float(levels) + dither
        return min(levels, max(0, Int(s.rounded())))
    }

    /// The dithered RGB565 framebuffer as an NSImage (nearest-friendly; upscale chunky).
    func nsImage(background: SIMD3<Float>, gamma: Float = 1.0) -> NSImage? {
        var buf = rgba8(background: background, gamma: gamma)
        let cs = CGColorSpaceCreateDeviceRGB()
        guard let ctx = CGContext(data: &buf, width: width, height: height, bitsPerComponent: 8,
                                  bytesPerRow: width * 4, space: cs,
                                  bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue),
              let cg = ctx.makeImage() else { return nil }
        return NSImage(cgImage: cg, size: NSSize(width: width, height: height))
    }
}

@inline(__always) private func mix(_ a: SIMD4<Float>, _ b: SIMD4<Float>, t: Float) -> SIMD4<Float> { a + (b - a) * t }
@inline(__always) private func mix(_ a: SIMD3<Float>, _ b: SIMD3<Float>, t: Float) -> SIMD3<Float> { a + (b - a) * t }
@inline(__always) private func mix(_ a: SIMD2<Float>, _ b: SIMD2<Float>, t: Float) -> SIMD2<Float> { a + (b - a) * t }
