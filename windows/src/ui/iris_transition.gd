class_name IrisTransition
# Port of macos/Sources/Cannonballs/IrisTransition.swift.
#
# The original menu/game screen transition (Menu_Manager.java). The MENUS/
# TRANSITION actor is NOT a solid disc: its mesh (actor.wsgo, probed
# 2026-07-09 with wsgo_decode_SOLVED — a flat z=0 plane spanning ±1397 units
# with the hole-rim vertices at r≈2.5) is a huge black sheet with a small
# circular hole, and IMAGES/FX/TRANSITION.png carries the soft alpha of that
# hole's rim. showDissolve() attaches it camera-space at z=2 with scale 1.0 =
# hole wider than the view frustum = screen fully visible; the exit ramp
# shrinks the scale to 0.001 over 1s = iris CLOSES to black (the sheet still
# covers the frustum), the loading hourglass shows, then the entry ramp grows
# it back to 1.0 = iris OPENS onto the new screen
# (Menu_Manager.java:246-292, 432-445).
#
# The macOS build draws this two ways: MenuScene builds the punched sheet +
# arm sprites itself (with the hourglass), and IrisTransition.run() animates
# the same geometry as a CALayer overlay for the menu->game wipe. This port
# keeps both surfaces: sheet_image()/arm_rects()/open_scale() feed
# MenuScene._iris_transition(), and run() is the standalone overlay.


## 512x512 black sheet with the original disc alpha punched out (the hole
## + its soft rim) — a mechanical inversion matching the actor mesh's
## sheet-with-hole construction, not a creative edit.
## Cached: the Swift recomposites per call via NSImage (GPU-cheap); a GDScript
## per-pixel pass is not, and the sheet is immutable.
static var _sheet: Image = null


static func sheet_image() -> Image:
	if _sheet != null:
		return _sheet
	var disc := Assets.image("IMAGES/FX/TRANSITION.png")
	if disc == null:
		return null
	disc.convert(Image.FORMAT_RGBA8)
	if disc.get_size() != Vector2i(512, 512):
		disc.resize(512, 512, Image.INTERPOLATE_BILINEAR)   # Swift draws the disc into the 512 sheet
	# black fill + .destinationOut of the disc: RGB stays black, alpha = 1 - disc.a
	var src := disc.get_data()
	var out := PackedByteArray()
	out.resize(src.size())   # zero-filled = black RGB
	for i in range(3, src.size(), 4):
		out[i] = 255 - src[i]
	_sheet = Image.create_from_data(512, 512, false, Image.FORMAT_RGBA8, out)
	return _sheet


## Arm half-extent in local (unscaled) points. The original mesh reaches
## ~560x beyond the hole rim (±1397 vs r≈2.5); 300k pt keeps the screen
## covered even at the fully-closed scale of 0.002.
const ARM_EXTENT := 300_000.0


## Local rects (hole centered at origin, 512x512) for the four black arms
## that stand in for the mesh's giant quad around the punched sheet.
## Transcribed from the Swift's y-up local space; the set is symmetric about
## the x axis (top/bottom arms are mirror images), so the y-down canvas uses
## the same rects unchanged — only which one is "top" swaps, invisibly.
static func arm_rects() -> Array:
	var e := ARM_EXTENT
	return [Rect2(-e, 256, 2 * e, e),        # top
			Rect2(-e, -256 - e, 2 * e, e),   # bottom
			Rect2(-256 - e, -256, e, 512),   # left
			Rect2(256, -256, e, 512)]        # right


## Fully-open scale for a given view size: the hole rim (~250px of the
## 512 art) must clear every screen corner, matching scale 1.0's
## hole-beyond-frustum framing.
static func open_scale(size: Vector2) -> float:
	return maxf(size.x, size.y) * 2.6 / 512


## Standalone full-screen wipe (the Swift CALayer overlay used for the
## menu -> game transition): iris closes over the old screen, `midpoint` runs
## under black, iris opens onto the new one. `parent` hosts the overlay
## CanvasLayer and the tween (any node in the tree).
static func run(parent: Node, midpoint: Callable) -> void:
	var sheet := sheet_image()
	if sheet == null or not parent.is_inside_tree():
		midpoint.call()
		return
	var overlay := CanvasLayer.new()
	overlay.layer = 100   # Swift overlay.zPosition = 10_000: above everything
	# Scaling container: punched sheet + four black arms.
	var iris := Node2D.new()
	# View center: the canvas is the fixed 800x600 source space at any window
	# size (project stretch canvas_items), so the midpoint is constant.
	iris.position = Vector2(400, 300)
	var hole := Sprite2D.new()
	# The CALayer path has no overlay-alpha decode, so the Swift uses the raw
	# sheet here (no compensated() call) — matched.
	hole.texture = ImageTexture.create_from_image(sheet)   # 512x512, centered on the iris
	iris.add_child(hole)
	for r in arm_rects():
		var arm := ColorRect.new()
		arm.color = Color.BLACK
		arm.position = r.position
		arm.size = r.size
		arm.mouse_filter = Control.MOUSE_FILTER_IGNORE   # visual only; never intercept input
		iris.add_child(arm)
	overlay.add_child(iris)
	parent.add_child(overlay)

	var open := open_scale(Vector2(800, 600))
	iris.scale = Vector2(open, open)

	# Iris CLOSES over the old screen (Menu_Manager case 102/104: scale
	# 1.0 -> 0.001 over 1s), the screen switches under black, then OPENS
	# (case 101/103 / dissolveOffToGame).
	var tw := iris.create_tween()
	tw.tween_property(iris, "scale", Vector2(0.002, 0.002), 1.0)
	tw.tween_callback(midpoint)
	tw.tween_property(iris, "scale", Vector2(open, open), 1.0)
	tw.tween_callback(overlay.queue_free)
