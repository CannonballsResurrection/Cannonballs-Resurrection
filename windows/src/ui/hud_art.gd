class_name HUDArt
# Port of macos/Sources/Cannonballs/HUDArt.swift.
# Loads and slices the original Cannonballs HUD sprite art (Resources/HUDART),
# and renders text with the game's Trebuchet bitmap font.
#
# Godot conversions (PORTING.md "HUD / menus", "Overlay alpha"):
# - Swift crop()/cropTexture() collapse into crop_texture() -> AtlasTexture
#   (sheet + Rect2 region, top-left-origin pixels, exactly the Swift rect);
#   crop_image() serves the call sites that composite Images (minimap bake).
# - Composite builders (popup panels, 9-slices, text) bake an Image and return
#   an ImageTexture, mirroring the Swift NSImage lockFocus composites.
# - Texture filtering is a CanvasItem property in Godot, not a texture one:
#   consumers set texture_filter = TEXTURE_FILTER_NEAREST wherever the Swift
#   sets SKTexture.filteringMode = .nearest.
# - All bottom-left-origin NSImage draw math is converted to Godot's
#   top-left-origin Image coords at each use site (marked "BL->TL").


# ---- Sprite loading / caching ----

static var _img_cache := {}    # name -> Image
static var _tex_cache := {}    # name -> ImageTexture
static var _crop_cache := {}   # "name|rect" -> AtlasTexture


static func image(name: String) -> Image:
	if _img_cache.has(name):
		return _img_cache[name]
	var raw := Assets.image("HUDART/" + name)
	if raw == null:
		raw = Image.create(1, 1, false, Image.FORMAT_RGBA8)   # Swift fallback: 1x1 empty
	raw.convert(Image.FORMAT_RGBA8)
	# NOTE: the macOS build runs overlayAlphaCompensate() here — SceneKit
	# sRGB-decodes the overlaySKScene's texture alpha, so the Swift pre-distorts
	# stored alpha (a' = srgbEncode(1 - (1-a)^2.2)) to survive it (HANDOFF.md
	# 2026-07-09 "OVERLAY ALPHA BUG ROOT-CAUSED"). Godot's gl_compatibility
	# canvas blends in sRGB/gamma space like the 2002 WT engine and has no such
	# decode, so the compensation is intentionally NOT ported (PORTING.md
	# "Overlay alpha"). The art's stored alpha is used as-is.
	_img_cache[name] = raw
	return raw


## Same pass-through for one-off images (e.g. the iris sheet) that render in
## the overlay but aren't HUDART assets. On macOS this applies the alpha
## compensation; in Godot it is an identity (see the note in image() above).
## Kept so ported call sites read 1:1 against the Swift.
static func compensated(img: Image) -> Image:
	return img


## A whole-sheet texture (nearest filtering off; sprites are already sized art).
static func texture(name: String) -> ImageTexture:
	if _tex_cache.has(name):
		return _tex_cache[name]
	var t := ImageTexture.create_from_image(image(name))
	_tex_cache[name] = t
	return t


## Crop a pixel rect (top-left origin) out of a sheet — the Swift
## crop()/cropTexture() pair. Returns an AtlasTexture referencing the sheet;
## no pixels are copied. filter_clip mirrors the standalone-image semantics of
## the Swift crop (no bleed from neighboring sprites when filtered).
static func crop_texture(name: String, rect: Rect2) -> AtlasTexture:
	var key := name + "|" + str(rect)
	if _crop_cache.has(key):
		return _crop_cache[key]
	var at := AtlasTexture.new()
	at.atlas = texture(name)
	at.region = rect
	at.filter_clip = true
	_crop_cache[key] = at
	return at


## Pixel-copy crop for call sites that composite Images (e.g. the minimap
## bake draws mapbits.png crops into a canvas).
static func crop_image(name: String, rect: Rect2i) -> Image:
	return image(name).get_region(rect)


# Draw src_rect of src into dst_rect of dst, stretching if the sizes differ —
# the analogue of NSImage.draw(in:from:). `nearest` mirrors
# imageInterpolation = .none, `blend` mirrors .sourceOver (vs .copy).
static func _draw(dst: Image, src: Image, dst_rect: Rect2i, src_rect: Rect2i,
		nearest: bool, blend: bool) -> void:
	if dst_rect.size.x <= 0 or dst_rect.size.y <= 0 \
			or src_rect.size.x <= 0 or src_rect.size.y <= 0:
		return
	var piece := src.get_region(src_rect)
	if piece.get_size() != dst_rect.size:
		piece.resize(dst_rect.size.x, dst_rect.size.y,
				Image.INTERPOLATE_NEAREST if nearest else Image.INTERPOLATE_BILINEAR)
	if blend:
		dst.blend_rect(piece, Rect2i(Vector2i.ZERO, dst_rect.size), dst_rect.position)
	else:
		dst.blit_rect(piece, Rect2i(Vector2i.ZERO, dst_rect.size), dst_rect.position)


# ---- Trebuchet bitmap font (exact metrics from Text.java / Message_3D.java) ----

## Per-glyph ink width for ASCII 32..127 (index = char - 32), verbatim from
## `Text.CharacterWidthTrebuchet`. Each glyph is drawn `width x 24` from a 10x10
## grid of 24px cells in the 256px sheet; the pen advances `width * 0.75` per
## glyph, so consecutive glyphs overlap 25% (the original's tight kerning).
const CHAR_WIDTHS := [
	12, 12, 13, 16, 14, 18, 14, 11, 12, 12, 12, 16, 11, 12, 12, 14,
	16, 15, 16, 16, 15, 16, 17, 17, 16, 16, 13, 13, 16, 16, 15, 13,
	20, 18, 16, 17, 17, 18, 16, 18, 17, 11, 15, 17, 16, 21, 17, 20,
	16, 20, 17, 16, 17, 17, 17, 22, 17, 18, 16, 11, 13, 12, 12, 14,
	10, 16, 15, 15, 16, 17, 13, 15, 15, 11, 11, 16, 11, 20, 16, 17,
	16, 15, 13, 14, 13, 16, 16, 19, 16, 17, 16, 13, 12, 13, 14, 16,
	14, 18, 14, 11,
]
const CELL_SIZE := 24.0        # Message_3D.FontSize; grid cell (px)
const ADVANCE_SCALE := 0.75    # pen advance = width * 0.75

## The three (and only three) text colors the engine had.
enum FontColor { WHITE, BLUE, GRAY }
const _FONT_SHEETS := ["trebuchet_white.png", "trebuchet_blue.png", "trebuchet_gray.png"]


static func font_sheet(color: FontColor) -> String:
	return _FONT_SHEETS[color]


static func glyph_index(ch: String) -> int:
	if ch.is_empty():
		return 0
	var i := ch.unicode_at(0) - 32
	return 0 if (i < 0 or i > 95) else i


## String advance width at scale 1.0 — matches Message_3D.PixelWidth = round(sum(w*0.75)*f).
static func text_advance_width(s: String, scale: float = 1.0) -> float:
	var w := 0.0
	for ch in s:
		w += CHAR_WIDTHS[glyph_index(ch)] * ADVANCE_SCALE
	return w * scale


## Render a string in the Trebuchet bitmap font exactly as the engine laid it out:
## 24px cells, per-glyph widths, `width * 0.75` pen advance (25% overlap). `scale`
## follows the original Message_3D `f` (1.0 = 24px tall). `cap_height` is kept for
## existing call sites and maps to `scale = cap_height / 24`. `color` picks one of
## the three real font sheets; `tint` (a Color, or null) recolors on top when a
## call genuinely needs it. `tracking` is accepted for signature parity with the
## Swift, which also ignores it.
static func text(s: String, cap_height: float = 16.0, tint: Variant = null,
		tracking: float = 0.0, color: FontColor = FontColor.WHITE) -> ImageTexture:
	var _unused := tracking
	var scale := cap_height / CELL_SIZE
	var sheet := image(font_sheet(color))

	# Lay out pen positions (native pixels, pre-scale).
	var placed: Array = []   # of [idx, x, w]
	var pen := 0.0
	var visual_right := 1.0
	for ch in s:
		var idx := glyph_index(ch)
		var w: float = CHAR_WIDTHS[idx]
		placed.append([idx, pen, w])
		visual_right = pen + w         # last glyph draws its full width
		pen += w * ADVANCE_SCALE

	var out_w := maxi(1, ceili(visual_right * scale))
	var out_h := maxi(1, ceili(CELL_SIZE * scale))
	var out := Image.create(out_w, out_h, false, Image.FORMAT_RGBA8)
	for p in placed:
		var col: int = p[0] % 10
		var row: int = p[0] / 10
		# Swift computes the source y bottom-left; glyph rows count from the
		# top of the sheet, so in Godot's top-left origin it is just row*24 (BL->TL).
		var src := Rect2i(col * int(CELL_SIZE), row * int(CELL_SIZE), int(p[2]), int(CELL_SIZE))
		# Godot Image blits at integer pixels; the Swift draws at fractional
		# positions, so pen positions round to the nearest pixel here.
		var dst := Rect2i(roundi(p[1] * scale), 0,
				maxi(1, roundi(p[2] * scale)), maxi(1, roundi(CELL_SIZE * scale)))
		_draw(out, sheet, dst, src, false, true)   # .high interpolation, .sourceOver
	if tint is Color:
		# NSImage.tinted(): sourceAtop fill — replace RGB where inked, keep alpha.
		var t: Color = tint
		for py in out_h:
			for px in out_w:
				var c := out.get_pixel(px, py)
				if c.a > 0.0:
					out.set_pixel(px, py, Color(t.r, t.g, t.b, c.a))
	return ImageTexture.create_from_image(out)


# ---- Original UI building blocks (spec extracted from the decompiled HUD/menu code) ----
# The whole UI is laid out in the original's 800x600 screen space and the scene
# scales to the window, so all sizes below are native source pixels.

## Popup panel at NATIVE 256px height, the original PopUp.java 3-arg build:
## left w/2 columns of popup_left + right w/2 columns of popup_right,
## cropped — never stretched (setBitmapTextureRect 0..w/512 and 1-w/512..1).
## width = 512 gives the full-size panel (Swift popupPanel()); both sheets are
## y-symmetric full-height columns, so BL->TL is a no-op here.
static func popup_panel(width: float = 512.0) -> ImageTexture:
	# The Swift draws at fractional half-widths (e.g. 295/2); Godot Images blit
	# whole pixels, so the seam column rounds — split in ONE place here.
	var w := roundi(width)
	var left_w := roundi(width / 2.0)
	var right_w := w - left_w
	var out := Image.create(w, 256, false, Image.FORMAT_RGBA8)
	_draw(out, image("popup_left.png"),
			Rect2i(0, 0, left_w, 256), Rect2i(0, 0, left_w, 256), true, false)          # .copy
	_draw(out, image("popup_right.png"),
			Rect2i(left_w, 0, right_w, 256), Rect2i(256 - right_w, 0, right_w, 256), true, true)  # .sourceOver
	return ImageTexture.create_from_image(out)


## Arbitrary-size popup panel: 9-slice of the 512x256 composite so the magenta
## header and gold trim stay at native thickness at any size.
static func panel_image(width: float, height: float) -> ImageTexture:
	var src: Image = popup_panel().get_image()   # 512x256, header at top
	var sw := 512
	var sh := 256
	var l := 12
	var r := 12
	var t := 28
	var b := 10
	var w := roundi(width)
	var h := roundi(height)
	var out := Image.create(w, h, false, Image.FORMAT_RGBA8)
	# The Swift rows are bottom-left origin; flipped to top-left the header band
	# lands at y=0 and the bottom edge at y=h-b, with identical source y (BL->TL).
	var rows := [
		[0, t, 0, t],                       # header
		[t, h - t - b, t, sh - t - b],      # middle stretch
		[h - b, b, sh - b, b],              # bottom edge
	]
	var cols := [
		[0, l, 0, l],
		[l, w - l - r, l, sw - l - r],
		[w - r, r, sw - r, r],
	]
	for row in rows:
		for col in cols:
			_draw(out, src,
					Rect2i(col[0], row[0], col[1], row[1]),
					Rect2i(col[2], row[2], col[3], row[3]), true, true)
	return ImageTexture.create_from_image(out)


## Short popup panel (e.g. Chat, 295x88 in the original): sleft + sright, 9-sliced
## horizontally (12px caps) and vertically clipped to the requested height.
## Source rects span the sheets' full 128px height, so BL->TL is a no-op.
static func short_popup_panel(width: float, height: float) -> ImageTexture:
	var w := roundi(width)
	var h := roundi(height)
	var out := Image.create(w, h, false, Image.FORMAT_RGBA8)
	var l := image("popup_sleft.png")
	var r := image("popup_sright.png")
	# Body: stretch the left panel's middle for the body, right panel's edge for the scroll rail.
	var cap := 16
	# left cap
	_draw(out, l, Rect2i(0, 0, cap, h), Rect2i(0, 0, 16, 128), true, false)
	# middle stretch
	_draw(out, l, Rect2i(cap, 0, w - cap * 2, h), Rect2i(24, 0, 200, 128), true, true)
	# right cap
	_draw(out, r, Rect2i(w - cap, 0, cap, h), Rect2i(240, 0, 16, 128), true, true)
	return ImageTexture.create_from_image(out)


## textbar.png 9-slice (10px caps) at a given width — the original Button_Bar.
## The bar art occupies only the TOP 22-row band of the 64px sheet (the original
## samples texture rows 0..24); stretching the whole sheet collapses the art
## into a thin sliver, so slice just that band.
static func text_bar(width: float, height: float = 24.0) -> ImageTexture:
	var sheet := image("textbar.png")
	var sw := sheet.get_width()
	var w := roundi(width)
	var h := roundi(height)
	var band_h := 22
	var band_y := 0   # Swift: sh - bandH in BL coords = the TOP band; TL y = 0 (BL->TL)
	var out := Image.create(w, h, false, Image.FORMAT_RGBA8)
	_draw(out, sheet, Rect2i(10, 0, w - 20, h), Rect2i(20, band_y, 20, band_h), true, false)
	_draw(out, sheet, Rect2i(0, 0, 10, h), Rect2i(0, band_y, 10, band_h), true, false)
	_draw(out, sheet, Rect2i(w - 10, 0, 10, h), Rect2i(sw - 10, band_y, 10, band_h), true, false)
	return ImageTexture.create_from_image(out)


## targetbar active/inactive 9-slice (10px caps, 28px tall) — floating name bars.
## Source rects span the sheet's full height, so BL->TL is a no-op.
static func target_bar(width: float, active: bool) -> ImageTexture:
	var sheet := image("targetbar_active.png" if active else "targetbar_inactive.png")
	var sw := sheet.get_width()
	var sh := sheet.get_height()
	var w := roundi(width)
	var h := 28
	var out := Image.create(w, h, false, Image.FORMAT_RGBA8)
	_draw(out, sheet, Rect2i(10, 0, w - 20, h), Rect2i(20, 0, 20, sh), true, false)
	_draw(out, sheet, Rect2i(0, 0, 10, h), Rect2i(0, 0, 10, sh), true, false)
	_draw(out, sheet, Rect2i(w - 10, 0, 10, h), Rect2i(sw - 10, 0, 10, sh), true, false)
	return ImageTexture.create_from_image(out)


## button.png two-state text button (256x64 sheet: top half normal, bottom half hover).
static func menu_button(hover: bool) -> AtlasTexture:
	return crop_texture("buttons_button.png", Rect2(0, 32 if hover else 0, 256, 32))


static func gilt_button(hover: bool) -> AtlasTexture:
	return crop_texture("buttons_giltbutton.png", Rect2(0, 32 if hover else 0, 256, 32))


static func gilt_drop_row(hover: bool) -> AtlasTexture:
	return crop_texture("buttons_giltdropdown.png", Rect2(0, 32 if hover else 0, 256, 32))


static func drop_row(hover: bool) -> AtlasTexture:
	return crop_texture("buttons_dropdown.png", Rect2(0, 32 if hover else 0, 256, 32))


static func rollout_row() -> AtlasTexture:
	return crop_texture("buttons_rollout.png", Rect2(0, 0, 256, 32))


## controls.png sheet crops (26x27 arrows etc, hover state 27px below).
enum ControlSprite { CHAT_UP, CHAT_DOWN, ARROW_RIGHT, ARROW_LEFT, MIN_MAX }


static func control_rect(s: ControlSprite) -> Rect2:
	match s:
		ControlSprite.CHAT_UP:
			return Rect2(0, 0, 26, 27)
		ControlSprite.CHAT_DOWN:
			return Rect2(0, 27, 26, 27)
		ControlSprite.ARROW_RIGHT:
			return Rect2(52, 0, 26, 27)
		ControlSprite.ARROW_LEFT:
			return Rect2(78, 0, 26, 27)
		ControlSprite.MIN_MAX:
			return Rect2(0, 70, 28, 26)
	return Rect2()


static func control(s: ControlSprite, hover: bool = false) -> AtlasTexture:
	var r := control_rect(s)
	if hover and s != ControlSprite.MIN_MAX:
		r.position.y += 27
	return crop_texture("controls.png", r)


## ui.png power/pitch assembly slices (native sizes from the original layout).
static func power_left() -> AtlasTexture:
	return crop_texture("ui.png", Rect2(0, 0, 256, 61))


static func power_mid() -> AtlasTexture:
	return crop_texture("ui.png", Rect2(0, 62, 256, 61))


static func power_cap() -> AtlasTexture:
	return crop_texture("ui.png", Rect2(220, 124, 35, 61))


static func pitch_bar_slice() -> AtlasTexture:
	return crop_texture("ui.png", Rect2(0, 132, 212, 53))


static func bone_marker() -> AtlasTexture:
	return crop_texture("ui.png", Rect2(0, 228, 79, 27))


static func small_marker() -> AtlasTexture:
	return crop_texture("ui.png", Rect2(0, 208, 51, 18))
