class_name SoftRaster
extends RefCounted
# Port of macos/Sources/Cannonballs/SoftRaster.swift.
#
# A software rasterizer that reproduces WebDriver / Genesis3D's DirectX 7
# fixed-function pipeline (see format-research/RASTERIZER_SPEC.md), so the clone
# renders the 3D world with the exact 2002 look instead of the GPU's modern one:
#
#  - Gouraud-interpolated, engine-baked vertex color (no per-pixel lighting, no specular)
#  - texel × vertexColor MODULATE
#  - bilinear, perspective-correct texture sampling
#  - color-key transparency, no backface culling
#  - z-buffer (LESSEQUAL), per-pixel linear fog
#  - RGB565 output with 4×4 ordered dither, native res then nearest upscale
#
# It renders into an RGBA8 buffer you can hand to an ImageTexture.
#
# GDScript port conventions (PORTING.md — each solved once, here):
#  - Swift `struct Vertex {position, normal, uv}` → three PARALLEL packed
#    arrays (PackedVector3Array positions/normals, PackedVector2Array uvs, all
#    the same length). Godot meshes already ship attributes as parallel packed
#    arrays and per-vertex objects are ruinously slow in GDScript. Same data,
#    same order, one visible representation change.
#  - Inner `Texture` → `RasterTexture` (GDScript forbids shadowing the native
#    Texture class); SIMD3/4<Float> → Vector3/Vector4; simd_float4x4 →
#    Projection; simd_float3x3 → Basis.
#  - `Texture.sample()`/`texel()` and `edge()` are additionally hand-inlined
#    into the per-pixel loop (`_fill`) — a GDScript method call per pixel costs
#    more than the math itself. The formulas there are verbatim copies of the
#    methods, which are kept below for API parity.
#
# PERFORMANCE: this is a per-pixel interpreter loop at the original 640x480.
# The algorithm and resolution are kept EXACTLY (fidelity beats framerate for
# this easter egg); see the gating note on SceneRasterizer.Live for measured
# cost and the optimizations deliberately NOT applied.


# MARK: types

# (Swift `struct Vertex` — replaced by parallel arrays, see header.)

class RasterTexture:
	var width: int
	var height: int
	var rgba := PackedFloat32Array()   # 0..1, RGBA interleaved, row-major, top-left origin
	var has_alpha := false

	## Fast, format-safe loader. (The Swift draws through a premultiplied
	## CGContext and un-premultiplies so MODULATE gets straight color —
	## SoftRaster.swift:33-58. Godot's Image is already straight
	## (non-premultiplied) RGBA, so only the 1/255 normalization remains.)
	static func from_image(img: Image) -> RasterTexture:
		if img == null:
			return null
		var src := img
		if src.is_compressed() or src.get_format() != Image.FORMAT_RGBA8:
			src = img.duplicate()
			if src.is_compressed():
				src.decompress()
			src.convert(Image.FORMAT_RGBA8)
		var w := src.get_width()
		var h := src.get_height()
		if w <= 0 or h <= 0:
			return null
		var bytes := src.get_data()   # base mip level comes first
		var inv := 1.0 / 255.0
		var out := PackedFloat32Array()
		out.resize(w * h * 4)
		var alpha_seen := false
		for i in w * h:
			var o := i * 4
			var a := float(bytes[o + 3]) * inv
			if a < 0.99:
				alpha_seen = true
			out[o] = float(bytes[o]) * inv
			out[o + 1] = float(bytes[o + 1]) * inv
			out[o + 2] = float(bytes[o + 2]) * inv
			out[o + 3] = a
		var t := RasterTexture.new()
		t.width = w
		t.height = h
		t.rgba = out
		t.has_alpha = alpha_seen
		return t

	func texel(x: int, y: int) -> Vector4:
		var o := (posmod(y, height) * width + posmod(x, width)) * 4
		return Vector4(rgba[o], rgba[o + 1], rgba[o + 2], rgba[o + 3])

	## Bilinear sample; uv in [0,1], v top-down.
	func sample(u: float, v: float) -> Vector4:
		var fx := u * float(width) - 0.5
		var fy := v * float(height) - 0.5
		var x0 := int(floorf(fx))
		var y0 := int(floorf(fy))
		var dx := fx - float(x0)
		var dy := fy - float(y0)
		var c00 := texel(x0, y0)
		var c10 := texel(x0 + 1, y0)
		var c01 := texel(x0, y0 + 1)
		var c11 := texel(x0 + 1, y0 + 1)
		var top := c00 * (1.0 - dx) + c10 * dx
		var bot := c01 * (1.0 - dx) + c11 * dx
		return top * (1.0 - dy) + bot * dy


class Light:
	# engine-baked vertex lighting inputs
	var ambient: Vector3
	var sun_dir: Vector3    # toward the sun, normalized
	var sun_color: Vector3

	func _init(p_ambient: Vector3, p_sun_dir: Vector3, p_sun_color: Vector3) -> void:
		ambient = p_ambient
		sun_dir = p_sun_dir
		sun_color = p_sun_color


class Fog:
	var enabled: bool
	var start: float
	var end: float
	var color: Vector3

	func _init(p_enabled := false, p_start := 0.0, p_end := 1.0,
			p_color := Vector3.ZERO) -> void:
		enabled = p_enabled
		start = p_start
		end = p_end
		color = p_color

	static func none() -> Fog:   # Swift: `static let none`
		return Fog.new(false, 0.0, 1.0, Vector3.ZERO)


# MARK: framebuffer

var width: int
var height: int
var _color := PackedFloat32Array()   # linear RGB accumulation, 3 floats/pixel
var _depth := PackedFloat32Array()   # stored as 1/w (bigger = nearer); LESSEQUAL on w
var _covered := PackedByteArray()


func _init(p_width: int, p_height: int) -> void:
	width = p_width
	height = p_height
	_color.resize(width * height * 3)
	_depth.resize(width * height)
	_depth.fill(INF)                 # Swift: .greatestFiniteMagnitude
	_covered.resize(width * height)


func clear(c: Vector3) -> void:
	for i in width * height:
		var o := i * 3
		_color[o] = c.x
		_color[o + 1] = c.y
		_color[o + 2] = c.z
	_depth.fill(INF)
	_covered.fill(0)


## Vertical sky gradient (zenith at top → horizon at bottom), like the engine's sky dome.
func clear_sky(top: Vector3, bottom: Vector3) -> void:
	for py in height:
		var t := float(py) / float(maxi(1, height - 1))
		var c := top + (bottom - top) * t
		var row := py * width
		for px in width:
			var o := (row + px) * 3
			_color[o] = c.x
			_color[o + 1] = c.y
			_color[o + 2] = c.z
	_depth.fill(INF)
	_covered.fill(0)


# MARK: draw

## Draw a mesh. `mvp` = projection * view * model (column-major, clip = mvp * [pos,1]).
## `normal_matrix` transforms normals to world space for lighting.
## positions/normals/uvs are the parallel per-vertex attribute arrays (see header).
func draw(positions: PackedVector3Array, normals: PackedVector3Array,
		uvs: PackedVector2Array, indices: PackedInt32Array,
		texture: RasterTexture, mvp: Projection, normal_matrix: Basis,
		light: Light, fog: Fog, tint := Vector3.ONE, unlit := false) -> void:
	# Transform to clip space + bake vertex color once (engine pre-lighting).
	var n := positions.size()
	var clip := PackedFloat32Array()
	clip.resize(n * 4)
	var lit := PackedFloat32Array()
	lit.resize(n * 3)
	for i in n:
		var p := positions[i]
		var c := mvp * Vector4(p.x, p.y, p.z, 1.0)
		var o4 := i * 4
		clip[o4] = c.x
		clip[o4 + 1] = c.y
		clip[o4 + 2] = c.z
		clip[o4 + 3] = c.w
		var col: Vector3
		if unlit:
			col = tint                          # .constant materials: fullbright
		else:
			var nw := (normal_matrix * normals[i]).normalized()
			var ndl := maxf(0.0, nw.dot(light.sun_dir))
			col = (light.ambient + light.sun_color * ndl).clamp(Vector3.ZERO, Vector3.ONE) * tint
		var o3 := i * 3
		lit[o3] = col.x
		lit[o3 + 1] = col.y
		lit[o3 + 2] = col.z
	var i := 0
	var count := indices.size()
	while i + 2 < count:
		var ia := indices[i]
		var ib := indices[i + 1]
		var ic := indices[i + 2]
		_triangle(
				Vector4(clip[ia * 4], clip[ia * 4 + 1], clip[ia * 4 + 2], clip[ia * 4 + 3]),
				Vector4(clip[ib * 4], clip[ib * 4 + 1], clip[ib * 4 + 2], clip[ib * 4 + 3]),
				Vector4(clip[ic * 4], clip[ic * 4 + 1], clip[ic * 4 + 2], clip[ic * 4 + 3]),
				uvs[ia], uvs[ib], uvs[ic],
				Vector3(lit[ia * 3], lit[ia * 3 + 1], lit[ia * 3 + 2]),
				Vector3(lit[ib * 3], lit[ib * 3 + 1], lit[ib * 3 + 2]),
				Vector3(lit[ic * 3], lit[ic * 3 + 1], lit[ic * 3 + 2]),
				texture, fog)
		i += 3


# (Swift keeps one attribute-bundle struct `CV` per vertex for clipping; the
# port carries the same bundle as three parallel local arrays.)

func _triangle(a: Vector4, b: Vector4, c: Vector4,
		uva: Vector2, uvb: Vector2, uvc: Vector2,
		ca: Vector3, cb: Vector3, cc: Vector3,
		tex: RasterTexture, fog: Fog) -> void:
	# Near-plane clip (w > epsilon) via Sutherland-Hodgman, then fan-triangulate.
	var near := 1e-4
	var poly_clip: Array[Vector4] = [a, b, c]
	var poly_uv: Array[Vector2] = [uva, uvb, uvc]
	var poly_col: Array[Vector3] = [ca, cb, cc]
	var out_clip: Array[Vector4] = []
	var out_uv: Array[Vector2] = []
	var out_col: Array[Vector3] = []
	for k in 3:
		var prv := (k + 2) % 3
		var cur_in := poly_clip[k].w > near
		var prv_in := poly_clip[prv].w > near
		if cur_in != prv_in:
			var t := (near - poly_clip[prv].w) / (poly_clip[k].w - poly_clip[prv].w)
			out_clip.append(poly_clip[prv].lerp(poly_clip[k], t))
			out_uv.append(poly_uv[prv].lerp(poly_uv[k], t))
			out_col.append(poly_col[prv].lerp(poly_col[k], t))
		if cur_in:
			out_clip.append(poly_clip[k])
			out_uv.append(poly_uv[k])
			out_col.append(poly_col[k])
	if out_clip.size() < 3:
		return
	for k in range(1, out_clip.size() - 1):
		_fill(out_clip[0], out_clip[k], out_clip[k + 1],
				out_uv[0], out_uv[k], out_uv[k + 1],
				out_col[0], out_col[k], out_col[k + 1], tex, fog)


func _fill(v0: Vector4, v1: Vector4, v2: Vector4,
		uva: Vector2, uvb: Vector2, uvc: Vector2,
		ca: Vector3, cb: Vector3, cc: Vector3,
		tex: RasterTexture, fog: Fog) -> void:
	# Perspective divide + viewport (no backface cull → draw both windings).
	# (The Swift's screen() also derives NDC z "for reference"; depth uses view
	# w, so the port doesn't compute it.)
	var wf := float(width)
	var hf := float(height)
	var iw0 := 1.0 / v0.w
	var iw1 := 1.0 / v1.w
	var iw2 := 1.0 / v2.w
	var x0 := (v0.x * iw0 * 0.5 + 0.5) * wf
	var y0 := (1.0 - (v0.y * iw0 * 0.5 + 0.5)) * hf
	var x1 := (v1.x * iw1 * 0.5 + 0.5) * wf
	var y1 := (1.0 - (v1.y * iw1 * 0.5 + 0.5)) * hf
	var x2 := (v2.x * iw2 * 0.5 + 0.5) * wf
	var y2 := (1.0 - (v2.y * iw2 * 0.5 + 0.5)) * hf
	# Skip degenerate/non-finite triangles (guards Int(floor(NaN)) crashes).
	if not (is_finite(x0) and is_finite(y0) and is_finite(x1) and is_finite(y1)
			and is_finite(x2) and is_finite(y2)):
		return
	var min_x := maxi(0, int(floorf(minf(x0, minf(x1, x2)))))
	var max_x := mini(width - 1, int(ceilf(maxf(x0, maxf(x1, x2)))))
	var min_y := maxi(0, int(floorf(minf(y0, minf(y1, y2)))))
	var max_y := mini(height - 1, int(ceilf(maxf(y0, maxf(y1, y2)))))
	if min_x > max_x or min_y > max_y:
		return

	var area := (x2 - x0) * (y1 - y0) - (y2 - y0) * (x1 - x0)   # edge(s0, s1, s2)
	if absf(area) < 1e-7:
		return
	var inv_area := 1.0 / area
	var area_pos := area > 0.0

	# Perspective-correct: interpolate attr*invW and invW, divide per pixel.
	var u0 := uva.x * iw0
	var v0u := uva.y * iw0
	var u1 := uvb.x * iw1
	var v1u := uvb.y * iw1
	var u2 := uvc.x * iw2
	var v2u := uvc.y * iw2
	var r0 := ca.x * iw0
	var g0 := ca.y * iw0
	var b0 := ca.z * iw0
	var r1 := cb.x * iw1
	var g1 := cb.y * iw1
	var b1 := cb.z * iw1
	var r2 := cc.x * iw2
	var g2 := cc.y * iw2
	var b2 := cc.z * iw2

	# Hoist member accesses out of the pixel loop (GDScript lookup cost;
	# values verbatim from the Texture/Fog objects).
	var has_tex := tex != null
	var tw := 0
	var th := 0
	var trgba := PackedFloat32Array()
	if has_tex:
		tw = tex.width
		th = tex.height
		trgba = tex.rgba
	var fog_on := fog.enabled
	var fog_start := fog.start
	var fog_end := fog.end
	var fog_r := fog.color.x
	var fog_g := fog.color.y
	var fog_b := fog.color.z

	for py in range(min_y, max_y + 1):
		var yc := float(py) + 0.5
		var row := py * width
		for px in range(min_x, max_x + 1):
			var xc := float(px) + 0.5
			# Signed edge functions; inside (either winding) = all share the
			# area's sign. (edge() inlined — see header.)
			var e0 := (xc - x1) * (y2 - y1) - (yc - y1) * (x2 - x1)
			var e1 := (xc - x2) * (y0 - y2) - (yc - y2) * (x0 - x2)
			var e2 := (xc - x0) * (y1 - y0) - (yc - y0) * (x1 - x0)
			if area_pos:
				if e0 < 0.0 or e1 < 0.0 or e2 < 0.0:
					continue
			elif e0 > 0.0 or e1 > 0.0 or e2 > 0.0:
				continue
			# Normalized barycentrics (sum to 1, all >= 0 inside).
			var bw0 := e0 * inv_area
			var bw1 := e1 * inv_area
			var bw2 := e2 * inv_area
			var inv_w := bw0 * iw0 + bw1 * iw1 + bw2 * iw2
			if inv_w <= 0.0:
				continue
			var wpix := 1.0 / inv_w                   # ~ view-space w (depth)
			var idx := row + px
			if wpix > _depth[idx]:
				continue                              # LESSEQUAL depth on w
			var u := (bw0 * u0 + bw1 * u1 + bw2 * u2) * wpix
			var v := (bw0 * v0u + bw1 * v1u + bw2 * v2u) * wpix
			var r := (bw0 * r0 + bw1 * r1 + bw2 * r2) * wpix
			var g := (bw0 * g0 + bw1 * g1 + bw2 * g2) * wpix
			var b := (bw0 * b0 + bw1 * b1 + bw2 * b2) * wpix

			if has_tex:
				# Bilinear sample, uv in [0,1], v top-down (Texture.sample()/
				# texel() inlined; identical weights: top/bot lerp by dx then dy).
				var fx := u * float(tw) - 0.5
				var fy := v * float(th) - 0.5
				var tx0 := int(floorf(fx))
				var ty0 := int(floorf(fy))
				var dx := fx - float(tx0)
				var dy := fy - float(ty0)
				var wx0 := posmod(tx0, tw)            # (x % w + w) % w wrap
				var wx1 := posmod(tx0 + 1, tw)
				var wy0 := posmod(ty0, th) * tw
				var wy1 := posmod(ty0 + 1, th) * tw
				var o00 := (wy0 + wx0) * 4
				var o10 := (wy0 + wx1) * 4
				var o01 := (wy1 + wx0) * 4
				var o11 := (wy1 + wx1) * 4
				var w00 := (1.0 - dx) * (1.0 - dy)
				var w10 := dx * (1.0 - dy)
				var w01 := (1.0 - dx) * dy
				var w11 := dx * dy
				var ta := trgba[o00 + 3] * w00 + trgba[o10 + 3] * w10 \
						+ trgba[o01 + 3] * w01 + trgba[o11 + 3] * w11
				if ta < 0.5:
					continue                          # color-key / alpha-test cutout
				# MODULATE texel × vertex color
				r *= trgba[o00] * w00 + trgba[o10] * w10 + trgba[o01] * w01 + trgba[o11] * w11
				g *= trgba[o00 + 1] * w00 + trgba[o10 + 1] * w10 \
						+ trgba[o01 + 1] * w01 + trgba[o11 + 1] * w11
				b *= trgba[o00 + 2] * w00 + trgba[o10 + 2] * w10 \
						+ trgba[o01 + 2] * w01 + trgba[o11 + 2] * w11
			if fog_on:
				var f := clampf((fog_end - wpix) / (fog_end - fog_start), 0.0, 1.0)
				# mix(fog.color, rgb, f)
				r = fog_r + (r - fog_r) * f
				g = fog_g + (g - fog_g) * f
				b = fog_b + (b - fog_b) * f
			var co := idx * 3
			_color[co] = r
			_color[co + 1] = g
			_color[co + 2] = b
			_depth[idx] = wpix
			_covered[idx] = 1


# MARK: output — RGB565 + 4×4 ordered dither

static var _bayer4: PackedFloat32Array = _make_bayer4()

static func _make_bayer4() -> PackedFloat32Array:
	var m := [0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5]
	var out := PackedFloat32Array()
	out.resize(16)
	for i in 16:
		out[i] = (float(m[i]) + 0.5) / 16.0 - 0.5   # -0.5..0.5
	return out


## Quantize the framebuffer to RGB565 with ordered dither and return RGBA8.
## `background` fills any pixel never written (when the buffer was flat-cleared);
## with clear_sky the sky already lives in the buffer. `gamma` brightens toward
## the engine's slightly lifted output (≈1.15).
## (Carried as-is from the Swift: its covered[idx] ternary picks color[idx] on
## both branches and `background` goes unused — the sky/gradient lives in the
## buffer; the parameter is kept for the call sites.)
@warning_ignore("integer_division")
func rgba8(_background: Vector3, gamma := 1.0) -> PackedByteArray:
	var out := PackedByteArray()
	out.resize(width * height * 4)
	out.fill(255)
	var ig := 1.0 if gamma == 1.0 else 1.0 / gamma
	var apply_gamma := gamma != 1.0
	var bayer := _bayer4
	for py in height:
		var drow := (py & 3) * 4
		var row := py * width
		for px in width:
			var idx := row + px
			var co := idx * 3
			var cr := _color[co]
			var cg := _color[co + 1]
			var cb := _color[co + 2]
			if apply_gamma:
				cr = pow(cr, ig)
				cg = pow(cg, ig)
				cb = pow(cb, ig)
			var d := bayer[drow + (px & 3)]
			# 565: 5 bits R (×31), 6 bits G (×63), 5 bits B (×31)
			# (quant() inlined: min(levels, max(0, round(v*levels + dither))))
			var r := clampi(int(roundf(cr * 31.0 + d)), 0, 31)
			var g := clampi(int(roundf(cg * 63.0 + d)), 0, 63)
			var b := clampi(int(roundf(cb * 31.0 + d)), 0, 31)
			var o := idx * 4
			out[o] = r * 255 / 31
			out[o + 1] = g * 255 / 63
			out[o + 2] = b * 255 / 31
			# out[o + 3] pre-filled 255
	return out


## The dithered RGB565 framebuffer as an Image (nearest-friendly; upscale
## chunky). Swift: nsImage(background:gamma:).
func image(background: Vector3, gamma := 1.0) -> Image:
	return Image.create_from_data(width, height, false, Image.FORMAT_RGBA8,
			rgba8(background, gamma))
