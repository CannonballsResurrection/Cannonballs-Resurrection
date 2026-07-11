class_name Terrain
extends RefCounted
# Port of macos/Sources/Cannonballs/Terrain.swift (which cites Island.java).
# The island terrain: 96x96 heightmap, live deformation, texture decals.
# World: 640x640 units, vertex spacing 640/96; sea level y = 0.

const GRID := 96
const VERTEX_SCALE := 640.0 / 96.0   # ≈ 6.667
const MAX_HEIGHT := 100.0
const EASE_RATE := 30.0              # units/sec toward target

var current := PackedFloat32Array()  # animated heights
var target := PackedFloat32Array()   # deformation target
var _dirty := true
var _texture_dirty := false

var node := MeshInstance3D.new()
var material := StandardMaterial3D.new()
var _texture_canvas: Image
var _canvas_texture: ImageTexture

var map: MapCatalog.MapInfo

# lazily built decal stamps (see _scorch_decal / _splat_alpha below)
static var _scorch_cache: Image = null
static var _splat_cache = null      # Array of 32 rows of 32 floats, or null


func _init(p_map: MapCatalog.MapInfo) -> void:
	map = p_map
	var g := GRID
	current.resize(g * g)
	var data := Assets.data(map.heightmap_path())
	for i in g * g:
		if data.size() >= g * g:
			current[i] = float(data[i]) / 256.0 * map.map_scale - 4.0
		else:
			current[i] = -4.0
	target = current.duplicate()

	# 512x512 bake canvas seeded with the map texture (decals draw into it)
	_texture_canvas = Image.create(512, 512, false, Image.FORMAT_RGBA8)
	var base := Assets.image(map.texture_path())
	if base != null:
		base.convert(Image.FORMAT_RGBA8)
		if base.get_width() != 512 or base.get_height() != 512:
			base.resize(512, 512)
		_texture_canvas.blit_rect(base, Rect2i(0, 0, 512, 512), Vector2i.ZERO)
	_canvas_texture = ImageTexture.create_from_image(_texture_canvas)

	material.albedo_texture = _canvas_texture
	# SceneKit: wrapS/T = .clamp; lightingModel = .lambert (PORTING.md mapping)
	material.texture_repeat = false
	material.roughness = 1.0
	material.metallic_specular = 0.0
	material.cull_mode = BaseMaterial3D.CULL_BACK   # isDoubleSided = false
	node.name = "terrain"
	node.material_override = material
	_rebuild_geometry()


# MARK: - Sampling

## Bilinear height at world coordinates. Outside island → -4 (deep water).
func height(x: float, z: float) -> float:
	var g := GRID
	var fx := x / VERTEX_SCALE
	var fz := z / VERTEX_SCALE
	if fx < 0 or fz < 0 or fx > float(g - 1) or fz > float(g - 1):
		return -4.0
	var x0 := mini(int(fx), g - 2)
	var z0 := mini(int(fz), g - 2)
	var tx := fx - float(x0)
	var tz := fz - float(z0)
	var h00 := current[z0 * g + x0]
	var h10 := current[z0 * g + x0 + 1]
	var h01 := current[(z0 + 1) * g + x0]
	var h11 := current[(z0 + 1) * g + x0 + 1]
	return (h00 * (1 - tx) + h10 * tx) * (1 - tz) + (h01 * (1 - tx) + h11 * tx) * tz


## Target (post-deformation) height, used for spawn validity checks.
func target_height(x: float, z: float) -> float:
	var g := GRID
	var gx := int(roundf(x / VERTEX_SCALE))
	var gz := int(roundf(z / VERTEX_SCALE))
	if gx < 0 or gz < 0 or gx >= g or gz >= g:
		return -4.0
	return target[gz * g + gx]


static func world_size() -> float:
	return float(GRID - 1) * VERTEX_SCALE


# MARK: - Deformation (SPEC §2: linear cone falloff, ease 30 u/s)

func crater(x: float, z: float, depth: float, radius: float, splat = null) -> void:
	_for_each_vertex(x, z, radius, func(idx: int, dist: float) -> void:
		target[idx] -= (1 - dist / radius) * depth)
	if splat != null:
		_stamp_splat(x, z, splat)   # SPLAT texture bake (crater weapons)
	else:
		_stamp_scorch(x, z)   # the ORIGINAL 32x32 SCORCH burn bake (Island.java:524)


func molehill(x: float, z: float, height_amt: float, radius: float, splat = null) -> void:
	_for_each_vertex(x, z, radius, func(idx: int, dist: float) -> void:
		target[idx] = minf(MAX_HEIGHT, maxf(0, target[idx] + (1 - dist / radius) * height_amt)))
	if splat != null:
		_paint_decal(x, z, radius, splat)


## Raise terrain to at least `height_amt` (spawn platform fallback).
func molehill_absolute(x: float, z: float, height_amt: float, radius: float) -> void:
	_for_each_vertex(x, z, radius, func(idx: int, dist: float) -> void:
		var lift := (1 - dist / radius) * height_amt
		target[idx] = maxf(target[idx], lift))


## Crater applied along a line (X-Shot trenches).
func groove(x1: float, z1: float, x2: float, z2: float, radius: float, depth: float) -> void:
	var g := GRID
	var vs := VERTEX_SCALE
	for gz in g:
		for gx in g:
			var px := float(gx) * vs
			var pz := float(gz) * vs
			var d := Terrain.distance_to_segment(px, pz, x1, z1, x2, z2)
			if d < radius:
				target[gz * g + gx] -= (1 - d / radius) * depth
	# paint the trench
	var steps := 16
	for i in steps + 1:
		var t := float(i) / float(steps)
		_paint_decal(x1 + (x2 - x1) * t, z1 + (z2 - z1) * t, radius * 1.5,
				Color(0.08, 0.08, 0.08, 0.35))
	_dirty = true


static func distance_to_segment(px: float, pz: float, x1: float, z1: float, x2: float, z2: float) -> float:
	var dx := x2 - x1
	var dz := z2 - z1
	var len_sq := dx * dx + dz * dz
	if len_sq < 0.0001:
		return sqrt((px - x1) * (px - x1) + (pz - z1) * (pz - z1))
	var t := ((px - x1) * dx + (pz - z1) * dz) / len_sq
	t = clampf(t, 0, 1)
	var cx := x1 + t * dx
	var cz := z1 + t * dz
	return sqrt((px - cx) * (px - cx) + (pz - cz) * (pz - cz))


func _for_each_vertex(x: float, z: float, radius: float, body: Callable) -> void:
	var g := GRID
	var vs := VERTEX_SCALE
	var min_gx := maxi(0, int((x - radius) / vs))
	var max_gx := mini(g - 1, int((x + radius) / vs) + 1)
	var min_gz := maxi(0, int((z - radius) / vs))
	var max_gz := mini(g - 1, int((z + radius) / vs) + 1)
	if min_gx > max_gx or min_gz > max_gz:
		return
	for gz in range(min_gz, max_gz + 1):
		for gx in range(min_gx, max_gx + 1):
			var dx := float(gx) * vs - x
			var dz := float(gz) * vs - z
			var dist := sqrt(dx * dx + dz * dz)
			if dist < radius:
				body.call(gz * g + gx, dist)
	_dirty = true


# MARK: - Texture decals

## The original scorch bake: the 32x32 SCORCH texture blends the terrain map
## toward black (weight 1 - scorchR/255), a fixed 32-texel footprint centered
## on the impact (Island.java crater()).
static func _scorch_decal() -> Image:
	if _scorch_cache != null:
		return _scorch_cache
	var src := Assets.image("IMAGES/FX/SCORCH.png")
	if src == null:
		return null
	src.convert(Image.FORMAT_RGBA8)
	var out := Image.create(32, 32, false, Image.FORMAT_RGBA8)
	for y in 32:
		for x in 32:
			var r := src.get_pixel(x, y).r
			out.set_pixel(x, y, Color(0, 0, 0, 1 - r))
	_scorch_cache = out
	return out


## SPLAT bake: like scorch but blends toward the weapon color (Island.java
## crater() splat path: blendPixel(x,y, 1-R/255, r,g,b)).
static func _splat_alpha():
	if _splat_cache != null:
		return _splat_cache
	var src := Assets.image("IMAGES/FX/SPLAT.png")
	if src == null:
		return null
	src.convert(Image.FORMAT_RGBA8)
	var rows: Array = []
	for y in 32:
		var row := PackedFloat32Array()
		row.resize(32)
		for x in 32:
			row[x] = 1 - src.get_pixel(x, y).r
		rows.append(row)
	_splat_cache = rows
	return rows


func _stamp_splat(x: float, z: float, color: Color) -> void:
	var alpha = Terrain._splat_alpha()
	if alpha == null:
		_paint_decal(x, z, 6, color)
		return
	var decal := Image.create(32, 32, false, Image.FORMAT_RGBA8)
	for y in 32:
		for xx in 32:
			if alpha[y][xx] > 0.02:
				decal.set_pixel(xx, y, Color(color.r, color.g, color.b, alpha[y][xx]))
	_blend_stamp(decal, x, z)


func _stamp_scorch(x: float, z: float) -> void:
	var decal := Terrain._scorch_decal()
	if decal == null:
		_paint_decal(x, z, 6, Color(0.05, 0.05, 0.05, 0.55))
		return
	_blend_stamp(decal, x, z)


## Blend a 32x32 stamp centered on world (x,z) into the bake canvas.
## texture u ↔ x, v(image top) ↔ z=0 — matches geometry UV below. (The Swift
## drew through AppKit's bottom-left origin; Image is top-left, so image row
## = z/size*512 directly, no flip — PORTING.md.)
func _blend_stamp(decal: Image, x: float, z: float) -> void:
	var size := Terrain.world_size()
	var px := int(x / size * 512)
	var py := int(z / size * 512)
	_texture_canvas.blend_rect(decal, Rect2i(0, 0, 32, 32), Vector2i(px - 16, py - 16))
	_texture_dirty = true


## Radial gradient decal: color at center fading to transparent at radius.
func _paint_decal(x: float, z: float, radius: float, color: Color) -> void:
	var size := Terrain.world_size()
	var px := x / size * 512
	var py := z / size * 512
	var pr := radius / size * 512
	var ipr := int(ceilf(pr))
	if ipr < 1:
		return
	var decal := Image.create(ipr * 2, ipr * 2, false, Image.FORMAT_RGBA8)
	for y in ipr * 2:
		for xx in ipr * 2:
			var d := Vector2(xx - ipr + 0.5, y - ipr + 0.5).length()
			if d < pr:
				var a := color.a * (1 - d / pr)
				decal.set_pixel(xx, y, Color(color.r, color.g, color.b, a))
	_texture_canvas.blend_rect(decal, Rect2i(0, 0, ipr * 2, ipr * 2),
			Vector2i(int(px) - ipr, int(py) - ipr))
	_texture_dirty = true


# MARK: - Per-frame update

## Ease heights toward target; rebuild mesh when needed. Returns true if terrain moved.
func update(dt: float) -> bool:
	var moved := false
	var step := EASE_RATE * dt
	for i in current.size():
		if current[i] != target[i]:
			var d := target[i] - current[i]
			if absf(d) <= step:
				current[i] = target[i]
			else:
				current[i] += step if d > 0 else -step
			moved = true
	if moved:
		_dirty = true
	if _dirty:
		_rebuild_geometry()
		_dirty = false
	if _texture_dirty:
		_canvas_texture.update(_texture_canvas)
		_texture_dirty = false
	return moved


# MARK: - Mesh

func _rebuild_geometry() -> void:
	var g := GRID
	var vs := VERTEX_SCALE
	var positions := PackedVector3Array()
	positions.resize(g * g)
	var normals := PackedVector3Array()
	normals.resize(g * g)
	var uvs := PackedVector2Array()
	uvs.resize(g * g)

	for gz in g:
		for gx in g:
			positions[gz * g + gx] = Vector3(float(gx) * vs, current[gz * g + gx], float(gz) * vs)
			uvs[gz * g + gx] = Vector2(float(gx) / float(g - 1), float(gz) / float(g - 1))
	# normals via central differences
	for gz in g:
		for gx in g:
			var hl := current[gz * g + maxi(gx - 1, 0)]
			var hr := current[gz * g + mini(gx + 1, g - 1)]
			var hd := current[maxi(gz - 1, 0) * g + gx]
			var hu := current[mini(gz + 1, g - 1) * g + gx]
			normals[gz * g + gx] = Vector3(hl - hr, 2 * vs, hd - hu).normalized()

	var indices := PackedInt32Array()
	indices.resize((g - 1) * (g - 1) * 6)
	var w := 0
	for gz in g - 1:
		for gx in g - 1:
			var a := gz * g + gx
			var b := a + 1
			var c := (gz + 1) * g + gx
			var d := c + 1
			# The Swift winds counter-clockwise from +Y ([a,c,b, b,c,d]) for
			# SceneKit; Godot front faces are CLOCKWISE (PORTING.md), so the
			# second/third indices swap.
			indices[w] = a; indices[w + 1] = b; indices[w + 2] = c
			indices[w + 3] = b; indices[w + 4] = d; indices[w + 5] = c
			w += 6

	var arrays := []
	arrays.resize(Mesh.ARRAY_MAX)
	arrays[Mesh.ARRAY_VERTEX] = positions
	arrays[Mesh.ARRAY_NORMAL] = normals
	arrays[Mesh.ARRAY_TEX_UV] = uvs
	arrays[Mesh.ARRAY_INDEX] = indices
	var mesh := ArrayMesh.new()
	mesh.add_surface_from_arrays(Mesh.PRIMITIVE_TRIANGLES, arrays)
	node.mesh = mesh
