import AppKit
import QuartzCore

/// The original menu/game screen transition (Menu_Manager.java). The MENUS/
/// TRANSITION actor is NOT a solid disc: its mesh (actor.wsgo, probed
/// 2026-07-09 with wsgo_decode_SOLVED — a flat z=0 plane spanning ±1397 units
/// with the hole-rim vertices at r≈2.5) is a huge black sheet with a small
/// circular hole, and IMAGES/FX/TRANSITION.png carries the soft alpha of that
/// hole's rim. showDissolve() attaches it camera-space at z=2 with scale 1.0 =
/// hole wider than the view frustum = screen fully visible; the exit ramp
/// shrinks the scale to 0.001 over 1s = iris CLOSES to black (the sheet still
/// covers the frustum), the loading hourglass shows, then the entry ramp grows
/// it back to 1.0 = iris OPENS onto the new screen
/// (Menu_Manager.java:246-292, 432-445).
enum IrisTransition {
    /// 512x512 black sheet with the original disc alpha punched out (the hole
    /// + its soft rim) — a mechanical inversion matching the actor mesh's
    /// sheet-with-hole construction, not a creative edit.
    static func sheetImage() -> NSImage? {
        guard let disc = Assets.image("IMAGES/FX/TRANSITION.png") else { return nil }
        let img = NSImage(size: NSSize(width: 512, height: 512))
        img.lockFocus()
        NSColor.black.setFill()
        NSRect(x: 0, y: 0, width: 512, height: 512).fill()
        disc.draw(in: NSRect(x: 0, y: 0, width: 512, height: 512),
                  from: .zero, operation: .destinationOut, fraction: 1.0)
        img.unlockFocus()
        return img
    }

    /// Arm half-extent in local (unscaled) points. The original mesh reaches
    /// ~560x beyond the hole rim (±1397 vs r≈2.5); 300k pt keeps the screen
    /// covered even at the fully-closed scale of 0.002.
    static let armExtent: CGFloat = 300_000

    /// Local rects (hole centered at origin, 512x512) for the four black arms
    /// that stand in for the mesh's giant quad around the punched sheet.
    static func armRects() -> [CGRect] {
        let e = armExtent
        return [CGRect(x: -e, y: 256, width: 2 * e, height: e),        // top
                CGRect(x: -e, y: -256 - e, width: 2 * e, height: e),   // bottom
                CGRect(x: -256 - e, y: -256, width: e, height: 512),   // left
                CGRect(x: 256, y: -256, width: e, height: 512)]        // right
    }

    /// Fully-open scale for a given view size: the hole rim (~250px of the
    /// 512 art) must clear every screen corner, matching scale 1.0's
    /// hole-beyond-frustum framing.
    static func openScale(for size: CGSize) -> CGFloat {
        max(size.width, size.height) * 2.6 / 512
    }

    static func run(over view: NSView, midpoint: @escaping () -> Void) {
        guard Thread.isMainThread else {
            DispatchQueue.main.async { run(over: view, midpoint: midpoint) }
            return
        }
        guard let sheetImg = sheetImage(), view.layer != nil else {
            midpoint()
            return
        }
        let overlay = CALayer()
        overlay.frame = view.bounds
        overlay.zPosition = 10_000
        // Scaling container: punched sheet + four black arms.
        let iris = CALayer()
        iris.position = CGPoint(x: view.bounds.midX, y: view.bounds.midY)
        let hole = CALayer()
        hole.contents = sheetImg
        hole.frame = CGRect(x: -256, y: -256, width: 512, height: 512)
        iris.addSublayer(hole)
        for r in armRects() {
            let arm = CALayer()
            arm.backgroundColor = NSColor.black.cgColor
            arm.frame = r
            iris.addSublayer(arm)
        }
        overlay.addSublayer(iris)
        view.layer?.addSublayer(overlay)

        let open = openScale(for: view.bounds.size)
        iris.transform = CATransform3DMakeScale(open, open, 1)

        // Iris CLOSES over the old screen (Menu_Manager case 102/104: scale
        // 1.0 -> 0.001 over 1s), the screen switches under black, then OPENS
        // (case 101/103 / dissolveOffToGame).
        let close = CABasicAnimation(keyPath: "transform.scale")
        close.fromValue = open; close.toValue = 0.002
        close.duration = 1.0
        close.fillMode = .forwards; close.isRemovedOnCompletion = false
        CATransaction.begin()
        CATransaction.setCompletionBlock {
            midpoint()
            let openAnim = CABasicAnimation(keyPath: "transform.scale")
            openAnim.fromValue = 0.002; openAnim.toValue = open
            openAnim.duration = 1.0
            openAnim.fillMode = .forwards; openAnim.isRemovedOnCompletion = false
            CATransaction.begin()
            CATransaction.setCompletionBlock { overlay.removeFromSuperlayer() }
            iris.add(openAnim, forKey: "open")
            CATransaction.commit()
        }
        iris.add(close, forKey: "close")
        CATransaction.commit()
    }
}
