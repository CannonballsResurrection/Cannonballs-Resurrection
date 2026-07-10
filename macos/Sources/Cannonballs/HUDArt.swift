import AppKit
import SpriteKit

/// Loads and slices the original Cannonballs HUD sprite art (Resources/HUDART),
/// and renders text with the game's Trebuchet bitmap font.
enum HUDArt {

    // MARK: - Sprite loading / caching

    private static var cache: [String: NSImage] = [:]
    private static var texCache: [String: SKTexture] = [:]

    static func image(_ name: String) -> NSImage {
        if let img = cache[name] { return img }
        let raw = Assets.image("HUDART/\(name)") ?? NSImage(size: NSSize(width: 1, height: 1))
        let img = overlayAlphaCompensate(raw)
        cache[name] = img
        return img
    }

    /// Same overlay alpha compensation for one-off images (e.g. the iris
    /// sheet) that render inside the overlaySKScene but aren't HUDART assets.
    static func compensated(_ img: NSImage) -> NSImage { overlayAlphaCompensate(img) }

    /// SceneKit composites the overlaySKScene as if the texture's alpha channel were
    /// sRGB-encoded (measured: out = encode(decode(dst) * (1 - decode(a)))), which
    /// nearly erases the translucent black bar interiors — the pitch/power bars render
    /// as hollow gold frames floating over the scene. The 2002 engine alpha-blended in
    /// sRGB space (out = dst * (1-a)), so pre-distort the stored alpha: the sRGB-space
    /// multiply (1-a) needs linear factor (1-a)^2.2, and the compositor decodes stored
    /// alpha with the sRGB curve, so store a' = srgbEncode(1 - (1-a)^2.2).
    private static func overlayAlphaCompensate(_ img: NSImage) -> NSImage {
        // take the rep's native-resolution pixels; cgImage(forProposedRect:) would
        // hand back a 2x backing image on retina and double pixelSize downstream
        let repCG = (img.representations.first as? NSBitmapImageRep)?.cgImage
        guard let cg = repCG ?? img.cgImage(forProposedRect: nil, context: nil, hints: nil),
              let ctx = CGContext(data: nil, width: cg.width, height: cg.height,
                                  bitsPerComponent: 8, bytesPerRow: cg.width * 4,
                                  space: CGColorSpace(name: CGColorSpace.sRGB)!,
                                  bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)
        else { return img }
        ctx.draw(cg, in: CGRect(x: 0, y: 0, width: cg.width, height: cg.height))
        guard let data = ctx.data else { return img }
        let p = data.bindMemory(to: UInt8.self, capacity: cg.width * cg.height * 4)
        for i in 0..<(cg.width * cg.height) {
            let a = p[i * 4 + 3]
            if a == 0 || a == 255 { continue }
            let linA = 1.0 - pow(1.0 - Double(a) / 255.0, 2.2)
            let enc = linA <= 0.0031308 ? linA * 12.92 : 1.055 * pow(linA, 1.0 / 2.4) - 0.055
            let na = UInt8(max(0, min(255, enc * 255.0 + 0.5)))
            let scale = Double(na) / Double(a)   // keep premultiplied color consistent
            for c in 0..<3 {
                p[i * 4 + c] = UInt8(max(0, min(255, Double(p[i * 4 + c]) * scale + 0.5)))
            }
            p[i * 4 + 3] = na
        }
        guard let out = ctx.makeImage() else { return img }
        // wrap in an explicit bitmap rep: NSImage(cgImage:) snapshots at screen
        // scale and misreports pixelsWide, which breaks crop()'s pixelSize math
        let outRep = NSBitmapImageRep(cgImage: out)
        outRep.size = img.size
        let result = NSImage(size: img.size)
        result.addRepresentation(outRep)
        return result
    }

    /// A whole-sheet texture (nearest filtering off; sprites are already sized art).
    static func texture(_ name: String) -> SKTexture {
        if let t = texCache[name] { return t }
        let t = SKTexture(image: image(name))
        texCache[name] = t
        texCache[name] = t
        return t
    }

    /// Crop a pixel rect (top-left origin) out of a sheet and return an NSImage.
    static func crop(_ name: String, _ rect: NSRect) -> NSImage {
        let src = image(name)
        let px = src.pixelSize
        let out = NSImage(size: NSSize(width: rect.width, height: rect.height))
        out.lockFocus()
        NSGraphicsContext.current?.imageInterpolation = .high
        // NSImage draws bottom-left origin; flip the source Y.
        let srcRect = NSRect(x: rect.origin.x,
                             y: CGFloat(px.height) - rect.origin.y - rect.height,
                             width: rect.width, height: rect.height)
        src.draw(in: NSRect(x: 0, y: 0, width: rect.width, height: rect.height),
                 from: srcRect, operation: .copy, fraction: 1)
        out.unlockFocus()
        return out
    }

    static func cropTexture(_ name: String, _ rect: NSRect) -> SKTexture {
        SKTexture(image: crop(name, rect))
    }

    // MARK: - Trebuchet bitmap font (exact metrics from Text.java / Message_3D.java)

    /// Per-glyph ink width for ASCII 32..127 (index = char - 32), verbatim from
    /// `Text.CharacterWidthTrebuchet`. Each glyph is drawn `width x 24` from a 10x10
    /// grid of 24px cells in the 256px sheet; the pen advances `width * 0.75` per
    /// glyph, so consecutive glyphs overlap 25% (the original's tight kerning).
    static let charWidths: [CGFloat] = [
        12, 12, 13, 16, 14, 18, 14, 11, 12, 12, 12, 16, 11, 12, 12, 14,
        16, 15, 16, 16, 15, 16, 17, 17, 16, 16, 13, 13, 16, 16, 15, 13,
        20, 18, 16, 17, 17, 18, 16, 18, 17, 11, 15, 17, 16, 21, 17, 20,
        16, 20, 17, 16, 17, 17, 17, 22, 17, 18, 16, 11, 13, 12, 12, 14,
        10, 16, 15, 15, 16, 17, 13, 15, 15, 11, 11, 16, 11, 20, 16, 17,
        16, 15, 13, 14, 13, 16, 16, 19, 16, 17, 16, 13, 12, 13, 14, 16,
        14, 18, 14, 11
    ]
    private static let cellSize: CGFloat = 24        // Message_3D.FontSize; grid cell (px)
    private static let advanceScale: CGFloat = 0.75  // pen advance = width * 0.75

    /// The three (and only three) text colors the engine had.
    enum FontColor {
        case white, blue, gray
        var sheet: String {
            switch self {
            case .white: return "trebuchet_white.png"
            case .blue:  return "trebuchet_blue.png"
            case .gray:  return "trebuchet_gray.png"
            }
        }
    }

    private static func glyphIndex(_ ch: Character) -> Int {
        guard let s = ch.unicodeScalars.first?.value else { return 0 }
        let i = Int(s) - 32
        return (i < 0 || i > 95) ? 0 : i
    }

    /// String advance width at scale 1.0 — matches Message_3D.PixelWidth = round(sum(w*0.75)*f).
    static func textAdvanceWidth(_ str: String, scale: CGFloat = 1) -> CGFloat {
        var w: CGFloat = 0
        for ch in str { w += charWidths[glyphIndex(ch)] * advanceScale }
        return w * scale
    }

    // MARK: - Original UI building blocks (spec extracted from the decompiled HUD/menu code)
    // The whole UI is laid out in the original's 800x600 screen space and the scene
    // scales to the window, so all sizes below are native source pixels.

    /// Full-size popup panel (512x256): popup_left + popup_right side by side.
    static func popupPanel() -> NSImage {
        let out = NSImage(size: NSSize(width: 512, height: 256))
        out.lockFocus()
        NSGraphicsContext.current?.imageInterpolation = .none
        image("popup_left.png").draw(in: NSRect(x: 0, y: 0, width: 256, height: 256))
        image("popup_right.png").draw(in: NSRect(x: 256, y: 0, width: 256, height: 256))
        out.unlockFocus()
        return out
    }

    /// Narrow popup at NATIVE 256px height, the original PopUp.java 3-arg build:
    /// left w/2 columns of popup_left + right w/2 columns of popup_right,
    /// cropped — never stretched (setBitmapTextureRect 0..w/512 and 1-w/512..1).
    static func popupPanel(width: CGFloat) -> NSImage {
        let half = width / 2
        let out = NSImage(size: NSSize(width: width, height: 256))
        out.lockFocus()
        NSGraphicsContext.current?.imageInterpolation = .none
        image("popup_left.png").draw(in: NSRect(x: 0, y: 0, width: half, height: 256),
                                     from: NSRect(x: 0, y: 0, width: half, height: 256),
                                     operation: .copy, fraction: 1)
        image("popup_right.png").draw(in: NSRect(x: half, y: 0, width: half, height: 256),
                                      from: NSRect(x: 256 - half, y: 0, width: half, height: 256),
                                      operation: .sourceOver, fraction: 1)
        out.unlockFocus()
        return out
    }

    /// Arbitrary-size popup panel: 9-slice of the 512x256 composite so the magenta
    /// header and gold trim stay at native thickness at any size.
    static func panelImage(width: CGFloat, height: CGFloat) -> NSImage {
        let src = popupPanel()               // 512x256, header at top
        let sw: CGFloat = 512, sh: CGFloat = 256
        let l: CGFloat = 12, r: CGFloat = 12, t: CGFloat = 28, b: CGFloat = 10
        let out = NSImage(size: NSSize(width: width, height: height))
        out.lockFocus()
        NSGraphicsContext.current?.imageInterpolation = .none
        // src rects are bottom-left origin: header band is at the TOP of the image.
        let rows: [(dstY: CGFloat, dstH: CGFloat, srcY: CGFloat, srcH: CGFloat)] = [
            (0, b, 0, b),                                  // bottom edge
            (b, height - t - b, b, sh - t - b),            // middle stretch
            (height - t, t, sh - t, t)                     // header
        ]
        let cols: [(dstX: CGFloat, dstW: CGFloat, srcX: CGFloat, srcW: CGFloat)] = [
            (0, l, 0, l),
            (l, width - l - r, l, sw - l - r),
            (width - r, r, sw - r, r)
        ]
        for row in rows {
            for col in cols {
                src.draw(in: NSRect(x: col.dstX, y: row.dstY, width: col.dstW, height: row.dstH),
                         from: NSRect(x: col.srcX, y: row.srcY, width: col.srcW, height: row.srcH),
                         operation: .sourceOver, fraction: 1)
            }
        }
        out.unlockFocus()
        return out
    }

    /// Short popup panel (e.g. Chat, 295x88 in the original): sleft + sright, 9-sliced
    /// horizontally (12px caps) and vertically clipped to the requested height.
    static func shortPopupPanel(width: CGFloat, height: CGFloat) -> NSImage {
        let out = NSImage(size: NSSize(width: width, height: height))
        let l = image("popup_sleft.png"), r = image("popup_sright.png")
        out.lockFocus()
        NSGraphicsContext.current?.imageInterpolation = .none
        // Body: stretch the left panel's middle for the body, right panel's edge for the scroll rail.
        let cap: CGFloat = 16
        // left cap
        l.draw(in: NSRect(x: 0, y: 0, width: cap, height: height),
               from: NSRect(x: 0, y: 0, width: 16, height: 128), operation: .copy, fraction: 1)
        // middle stretch
        l.draw(in: NSRect(x: cap, y: 0, width: width - cap * 2, height: height),
               from: NSRect(x: 24, y: 0, width: 200, height: 128), operation: .sourceOver, fraction: 1)
        // right cap
        r.draw(in: NSRect(x: width - cap, y: 0, width: cap, height: height),
               from: NSRect(x: 240, y: 0, width: 16, height: 128), operation: .sourceOver, fraction: 1)
        out.unlockFocus()
        return out
    }

    /// textbar.png 9-slice (10px caps) at a given width — the original Button_Bar.
    /// The bar art occupies only the TOP 22-row band of the 64px sheet (the original
    /// samples texture rows 0..24); stretching the whole sheet collapses the art
    /// into a thin sliver, so slice just that band.
    static func textBar(width: CGFloat, height: CGFloat = 24) -> NSImage {
        let sheet = image("textbar.png")
        let sw = CGFloat(sheet.pixelSize.width), sh = CGFloat(sheet.pixelSize.height)
        let bandH: CGFloat = 22
        let bandY = sh - bandH        // top band in bottom-left-origin image coords
        let out = NSImage(size: NSSize(width: width, height: height))
        out.lockFocus()
        NSGraphicsContext.current?.imageInterpolation = .none
        sheet.draw(in: NSRect(x: 10, y: 0, width: width - 20, height: height),
                   from: NSRect(x: 20, y: bandY, width: 20, height: bandH), operation: .copy, fraction: 1)
        sheet.draw(in: NSRect(x: 0, y: 0, width: 10, height: height),
                   from: NSRect(x: 0, y: bandY, width: 10, height: bandH), operation: .copy, fraction: 1)
        sheet.draw(in: NSRect(x: width - 10, y: 0, width: 10, height: height),
                   from: NSRect(x: sw - 10, y: bandY, width: 10, height: bandH), operation: .copy, fraction: 1)
        out.unlockFocus()
        return out
    }

    /// targetbar active/inactive 9-slice (10px caps, 28px tall) — floating name bars.
    static func targetBar(width: CGFloat, active: Bool) -> NSImage {
        let sheet = image(active ? "targetbar_active.png" : "targetbar_inactive.png")
        let sw = CGFloat(sheet.pixelSize.width), sh = CGFloat(sheet.pixelSize.height)
        let h: CGFloat = 28
        let out = NSImage(size: NSSize(width: width, height: h))
        out.lockFocus()
        NSGraphicsContext.current?.imageInterpolation = .none
        sheet.draw(in: NSRect(x: 10, y: 0, width: width - 20, height: h),
                   from: NSRect(x: 20, y: 0, width: 20, height: sh), operation: .copy, fraction: 1)
        sheet.draw(in: NSRect(x: 0, y: 0, width: 10, height: h),
                   from: NSRect(x: 0, y: 0, width: 10, height: sh), operation: .copy, fraction: 1)
        sheet.draw(in: NSRect(x: width - 10, y: 0, width: 10, height: h),
                   from: NSRect(x: sw - 10, y: 0, width: 10, height: sh), operation: .copy, fraction: 1)
        out.unlockFocus()
        return out
    }

    /// button.png two-state text button (256x64 sheet: top half normal, bottom half hover).
    static func menuButton(hover: Bool) -> NSImage {
        crop("buttons_button.png", NSRect(x: 0, y: hover ? 32 : 0, width: 256, height: 32))
    }
    static func giltButton(hover: Bool) -> NSImage {
        crop("buttons_giltbutton.png", NSRect(x: 0, y: hover ? 32 : 0, width: 256, height: 32))
    }
    static func giltDropRow(hover: Bool) -> NSImage {
        crop("buttons_giltdropdown.png", NSRect(x: 0, y: hover ? 32 : 0, width: 256, height: 32))
    }
    static func dropRow(hover: Bool) -> NSImage {
        crop("buttons_dropdown.png", NSRect(x: 0, y: hover ? 32 : 0, width: 256, height: 32))
    }
    static func rolloutRow() -> NSImage {
        crop("buttons_rollout.png", NSRect(x: 0, y: 0, width: 256, height: 32))
    }

    /// controls.png sheet crops (26x27 arrows etc, hover state 27px below).
    enum ControlSprite {
        case chatUp, chatDown, arrowRight, arrowLeft, minMax
        var rect: NSRect {
            switch self {
            case .chatUp:     return NSRect(x: 0, y: 0, width: 26, height: 27)
            case .chatDown:   return NSRect(x: 0, y: 27, width: 26, height: 27)
            case .arrowRight: return NSRect(x: 52, y: 0, width: 26, height: 27)
            case .arrowLeft:  return NSRect(x: 78, y: 0, width: 26, height: 27)
            case .minMax:     return NSRect(x: 0, y: 70, width: 28, height: 26)
            }
        }
    }
    static func control(_ s: ControlSprite, hover: Bool = false) -> NSImage {
        var r = s.rect
        if hover, s != .minMax { r.origin.y += 27 }
        return crop("controls.png", r)
    }

    /// ui.png power/pitch assembly slices (native sizes from the original layout).
    static func powerLeft() -> NSImage { crop("ui.png", NSRect(x: 0, y: 0, width: 256, height: 61)) }
    static func powerMid() -> NSImage { crop("ui.png", NSRect(x: 0, y: 62, width: 256, height: 61)) }
    static func powerCap() -> NSImage { crop("ui.png", NSRect(x: 220, y: 124, width: 35, height: 61)) }
    static func pitchBarSlice() -> NSImage { crop("ui.png", NSRect(x: 0, y: 132, width: 212, height: 53)) }
    static func boneMarker() -> NSImage { crop("ui.png", NSRect(x: 0, y: 228, width: 79, height: 27)) }
    static func smallMarker() -> NSImage { crop("ui.png", NSRect(x: 0, y: 208, width: 51, height: 18)) }

    /// Render a string in the Trebuchet bitmap font exactly as the engine laid it out:
    /// 24px cells, per-glyph widths, `width * 0.75` pen advance (25% overlap). `scale`
    /// follows the original Message_3D `f` (1.0 = 24px tall). `capHeight` is kept for
    /// existing call sites and maps to `scale = capHeight / 24`. `color` picks one of
    /// the three real font sheets; `tint` recolors on top when a call genuinely needs it.
    static func text(_ str: String, capHeight: CGFloat = 16, tint: NSColor? = nil,
                     tracking: CGFloat = 0, color: FontColor = .white) -> NSImage {
        let scale = capHeight / cellSize
        let sheet = image(color.sheet)
        let sheetH = CGFloat(sheet.pixelSize.height)   // 256

        // Lay out pen positions (native pixels, pre-scale).
        var placed: [(idx: Int, x: CGFloat, w: CGFloat)] = []
        var pen: CGFloat = 0, visualRight: CGFloat = 1
        for ch in str {
            let idx = glyphIndex(ch)
            let w = charWidths[idx]
            placed.append((idx, pen, w))
            visualRight = pen + w            // last glyph draws its full width
            pen += w * advanceScale
        }

        let W = max(1, ceil(visualRight * scale))
        let H = max(1, ceil(cellSize * scale))
        let out = NSImage(size: NSSize(width: W, height: H))
        out.lockFocus()
        NSGraphicsContext.current?.imageInterpolation = .high
        for p in placed {
            let col = p.idx % 10, row = p.idx / 10
            let src = NSRect(x: CGFloat(col) * cellSize,
                             y: sheetH - CGFloat(row) * cellSize - cellSize,   // top-origin row
                             width: p.w, height: cellSize)
            let dst = NSRect(x: p.x * scale, y: 0, width: p.w * scale, height: cellSize * scale)
            sheet.draw(in: dst, from: src, operation: .sourceOver, fraction: 1)
        }
        out.unlockFocus()
        _ = tracking
        guard let tint else { return out }
        return out.tinted(tint)
    }
}

extension NSImage {
    var pixelSize: (width: Int, height: Int) {
        if let rep = representations.first {
            return (rep.pixelsWide, rep.pixelsHigh)
        }
        return (Int(size.width), Int(size.height))
    }

    /// Opaque bounding box (bottom-left origin, in points == px for these sheets).
    func opaqueBounds(alphaThreshold: UInt8 = 30) -> NSRect? {
        guard let tiff = tiffRepresentation, let rep = NSBitmapImageRep(data: tiff) else { return nil }
        let w = rep.pixelsWide, h = rep.pixelsHigh
        var minX = w, minY = h, maxX = -1, maxY = -1
        for py in 0..<h {
            for px in 0..<w {
                guard let c = rep.colorAt(x: px, y: py) else { continue }
                if UInt8(c.alphaComponent * 255) >= alphaThreshold {
                    if px < minX { minX = px }
                    if px > maxX { maxX = px }
                    if py < minY { minY = py }
                    if py > maxY { maxY = py }
                }
            }
        }
        if maxX < 0 { return nil }
        // rep py is top-origin; convert to bottom-left origin
        let bottom = h - 1 - maxY
        return NSRect(x: minX, y: bottom, width: maxX - minX + 1, height: maxY - minY + 1)
    }

    /// True if the image has any meaningfully transparent pixels (chroma-keyed foliage).
    var hasTransparency: Bool {
        guard let tiff = tiffRepresentation, let rep = NSBitmapImageRep(data: tiff),
              rep.hasAlpha else { return false }
        let w = rep.pixelsWide, h = rep.pixelsHigh
        let stepX = max(1, w / 48), stepY = max(1, h / 48)
        var py = 0
        while py < h {
            var px = 0
            while px < w {
                if let c = rep.colorAt(x: px, y: py), c.alphaComponent < 0.5 { return true }
                px += stepX
            }
            py += stepY
        }
        return false
    }

    /// Recolor opaque pixels to `color`, preserving alpha and a bit of shading.
    func tinted(_ color: NSColor) -> NSImage {
        let out = NSImage(size: size)
        out.lockFocus()
        draw(in: NSRect(origin: .zero, size: size), from: .zero, operation: .sourceOver, fraction: 1)
        color.set()
        NSRect(origin: .zero, size: size).fill(using: .sourceAtop)
        out.unlockFocus()
        return out
    }
}
