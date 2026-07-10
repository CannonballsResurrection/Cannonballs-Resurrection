class_name SceneRasterizer
extends RefCounted
# Port of macos/Sources/Cannonballs/SceneRasterizer.swift, plus the live-mode
# pieces that live elsewhere on macOS (see `Live` at the bottom):
#  - the softwareRender frame driver from GameViewController.swift:267-297
#  - the full-screen display sprite from HUDScene.swift:350-368
# Those land here because src/game_controller.gd and src/ui/hud_scene.gd are
# owned by other port tasks; game_controller only has to wire `Live` (see the
# class comment there for the expected calls).
#
# Bridges the live Godot world into the software rasterizer: walks the world's
# Node3D tree, extracts every MeshInstance3D's vertices/normals/uvs/indices +
# albedo texture and world transform, builds the camera matrices, and renders
# one frame through SoftRaster so the 3D world gets the exact DX7/Genesis3D
# look. (SceneKit's SCNGeometry visitors map to MeshInstance3D — every drawable
# in this clone is a MeshInstance3D, including the FX quads and sky parts.)


## Per-map lighting/fog/sky for the rasterizer, from the same data the world uses.
class Settings:
	var light: SoftRaster.Light
	var fog: SoftRaster.Fog
	var sky_top: Vector3
	var sky_bottom: Vector3
	var gamma: float

	func _init(p_light: SoftRaster.Light, p_fog: SoftRaster.Fog,
			p_sky_top: Vector3, p_sky_bottom: Vector3, p_gamma: float) -> void:
		light = p_light
		fog = p_fog
		sky_top = p_sky_top
		sky_bottom = p_sky_bottom
		gamma = p_gamma


static func settings(map: MapCatalog.MapInfo) -> Settings:
	var amb := Vector3(map.ambient_rgb.r, map.ambient_rgb.g, map.ambient_rgb.b)
	var light := SoftRaster.Light.new(
			amb * 0.7 + Vector3(0.18, 0.18, 0.18),
			map.sun_vector.normalized(),
			Vector3(map.sun_rgb.r, map.sun_rgb.g, map.sun_rgb.b))
	var horizon := amb * 0.6 + Vector3(0.4, 0.4, 0.4)          # fog + lower sky
	var fog := SoftRaster.Fog.new(true, 420.0, 1800.0, horizon)
	var sky_top := amb * 0.30 + Vector3(0.30, 0.5, 0.85) * 0.70  # zenith blue
	return Settings.new(light, fog, sky_top, horizon, 1.15)


## An extracted, thread-safe snapshot of the scene's drawable geometry + camera.
## Built on the main thread (reads the live scene); rasterized on any thread.
class Snapshot:
	var calls: Array = []     # of DrawCall
	var size: Vector2i


class DrawCall:
	var mesh: MeshData        # Swift `Mesh` (renamed: hides the native class)
	var mat: Mat
	var mvp: Projection
	var nrm: Basis


class MeshData:
	var positions := PackedVector3Array()
	var normals := PackedVector3Array()
	var uvs := PackedVector2Array()
	var indices := PackedInt32Array()


class Mat:
	var texture: SoftRaster.RasterTexture = null
	var tint := Vector3.ONE
	var unlit := false
	# UV1 transform (BaseMaterial3D: UV = UV * uv1_scale.xy + uv1_offset.xy),
	# baked into the snapshot's uvs — see the note in _material().
	var uv_scale := Vector2.ONE
	var uv_offset := Vector2.ZERO


## Main-thread step: pull geometry + camera into plain value types (textures
## are immutable after load), so the heavy fill can run off-thread without
## racing the scene.
static func snapshot(scene_root: Node3D, pov: Camera3D, size: Vector2i) -> Snapshot:
	var w := size.x
	var h := size.y
	var view := Projection(pov.global_transform.affine_inverse())
	# Camera3D.fov is vertical, like SCNCamera.fieldOfView (PORTING.md)
	var fov_y := deg_to_rad(pov.fov)
	var proj := gl_perspective(fov_y, float(w) / float(h), maxf(0.05, pov.near), pov.far)
	var view_proj := proj * view
	var snap := Snapshot.new()
	snap.size = size
	_walk(scene_root, view_proj, snap.calls)
	return snap


static func _walk(node: Node, view_proj: Projection, calls: Array) -> void:
	if node is MeshInstance3D:
		var mi := node as MeshInstance3D
		# `!node.isHidden, node.opacity > 0.01` (SceneRasterizer.swift:45).
		# is_visible_in_tree matches what SceneKit actually rendered (a hidden
		# ancestor hides the subtree); GeometryInstance3D.transparency is the
		# inverse of SCNNode.opacity (0 = opaque).
		var visible := mi.is_visible_in_tree() if mi.is_inside_tree() else mi.visible
		if visible and mi.transparency <= 0.99 and mi.mesh != null:
			# additive light effects (lens flare, lightbeam, shoreline shimmer,
			# FX billboards) have no software add-blend path: skip, don't draw
			# opaque black
			var first := mi.get_active_material(0)
			var additive := first is BaseMaterial3D \
					and (first as BaseMaterial3D).blend_mode == BaseMaterial3D.BLEND_MODE_ADD
			if not additive:
				var model := mi.global_transform
				var mvp := view_proj * Projection(model)
				# normalMatrix = transpose(inverse(upper 3x3)) (SceneRasterizer.swift:193-198)
				var nrm := model.basis.inverse().transposed()
				for pair in _extract(mi):
					var call := DrawCall.new()
					call.mesh = pair[0]
					call.mat = pair[1]
					call.mvp = mvp
					call.nrm = nrm
					calls.append(call)
	for child in node.get_children():
		_walk(child, view_proj, calls)


## Pure compute: rasterize a snapshot with a sky gradient. Safe off the main thread.
static func rasterize(snap: Snapshot, s: Settings) -> SoftRaster:
	var raster := SoftRaster.new(snap.size.x, snap.size.y)
	raster.clear_sky(s.sky_top, s.sky_bottom)
	for c in snap.calls:
		var call := c as DrawCall
		raster.draw(call.mesh.positions, call.mesh.normals, call.mesh.uvs,
				call.mesh.indices, call.mat.texture, call.mvp, call.nrm,
				s.light, s.fog, call.mat.tint, call.mat.unlit)
	return raster


## Convenience for offscreen CLIs (extract + rasterize together).
static func render_frame(scene_root: Node3D, pov: Camera3D, size: Vector2i,
		s: Settings) -> SoftRaster:
	return rasterize(snapshot(scene_root, pov, size), s)


# MARK: - geometry extraction

## One [MeshData, Mat] per triangle surface (each surface = one material —
## SceneKit's "one element per material" maps to Godot surfaces).
static func _extract(mi: MeshInstance3D) -> Array:
	var mesh := mi.mesh
	var result: Array = []
	for si in mesh.get_surface_count():
		# `element.primitiveType == .triangles` — only ArrayMesh exposes the
		# surface primitive type to scripts; PrimitiveMesh (Quad/Plane/Box/...)
		# always generates triangle surfaces.
		if mesh is ArrayMesh \
				and (mesh as ArrayMesh).surface_get_primitive_type(si) != Mesh.PRIMITIVE_TRIANGLES:
			continue
		var arrays := mesh.surface_get_arrays(si)
		var md := MeshData.new()
		md.positions = arrays[Mesh.ARRAY_VERTEX]
		if arrays[Mesh.ARRAY_NORMAL] != null:
			md.normals = arrays[Mesh.ARRAY_NORMAL]
		if arrays[Mesh.ARRAY_TEX_UV] != null:
			md.uvs = arrays[Mesh.ARRAY_TEX_UV]
		# missing attributes default to normal (0,1,0) / uv .zero (SceneRasterizer.swift:94-97)
		var vcount := md.positions.size()
		if md.normals.size() < vcount:
			var from := md.normals.size()
			md.normals.resize(vcount)
			for i in range(from, vcount):
				md.normals[i] = Vector3(0, 1, 0)
		if md.uvs.size() < vcount:
			md.uvs.resize(vcount)   # zero-filled
		if arrays[Mesh.ARRAY_INDEX] != null:
			md.indices = arrays[Mesh.ARRAY_INDEX]
		else:
			# non-indexed surface → sequential triangle list
			md.indices.resize(vcount)
			for i in vcount:
				md.indices[i] = i
		# NOTE: triangle winding is irrelevant here — the DX7 pipeline draws
		# both windings (no backface culling), so the SceneKit-CCW → Godot-CW
		# index swap (PORTING.md) needs no undoing.
		var mat := _material(mi.get_active_material(si))
		if mat.uv_scale != Vector2.ONE or mat.uv_offset != Vector2.ZERO:
			for i in md.uvs.size():
				md.uvs[i] = md.uvs[i] * mat.uv_scale + mat.uv_offset
		result.append([md, mat])
	return result


static func _material(m: Material) -> Mat:
	var mat := Mat.new()
	if m == null:
		return mat
	if m is ShaderMaterial:
		# Godot-only divergence: the clone's terrain swaps its
		# StandardMaterial3D for the cloud-shadow ShaderMaterial
		# (WorldDressing.add_cloud_shadows), whose `albedo_tex` parameter is
		# the same bake-canvas texture SceneKit kept in diffuse.contents — the
		# thing the Swift extractor read. Read it back out; the shader's
		# grit/cloud-shadow layers are GPU-only and invisible to the DX7
		# pipeline, matching the Swift's output. Lit (.lambert), white tint.
		var t = (m as ShaderMaterial).get_shader_parameter("albedo_tex")
		if t is Texture2D:
			mat.texture = _texture(t)
		return mat
	if m is BaseMaterial3D:
		var bm := m as BaseMaterial3D
		# lightingModel == .constant → foliage / water / sky are fullbright
		mat.unlit = bm.shading_mode == BaseMaterial3D.SHADING_MODE_UNSHADED
		# SceneKit carried `multiply.contents` (tint) and `diffuse.contents`
		# (texture OR flat color) separately; StandardMaterial3D folds both
		# into albedo_color × albedo_texture (PORTING.md), so albedo_color is
		# the tint in both of the Swift's branches.
		mat.tint = Vector3(bm.albedo_color.r, bm.albedo_color.g, bm.albedo_color.b)
		# DIVERGENCE from the Swift, toward the original: the Swift ignored
		# diffuse.contentsTransform (opaque SceneKit material state), so its
		# raster never saw the water's 70x tiling or the FX sheets' frame
		# crops — and it also read SCNPlane billboards as their raw UNIT-quad
		# vertex source (SceneKit applies width/height at render time), so
		# cloud puffs etc. degraded to 1-unit specks. In Godot the quad size
		# is baked into the mesh arrays and the frame crop is plain
		# uv1_scale/uv1_offset, so the port renders these the way the 2002
		# engine did: real-size billboards showing their selected frame, and
		# tiled water. (Confirmed vs `--rasterscene`: the Swift's clouds are
		# ~40 stray pixels; the sheet garble/specks were SceneKit artifacts,
		# not original behavior.)
		mat.uv_scale = Vector2(bm.uv1_scale.x, bm.uv1_scale.y)
		mat.uv_offset = Vector2(bm.uv1_offset.x, bm.uv1_offset.y)
		if bm.albedo_texture != null:
			mat.texture = _texture(bm.albedo_texture)
	return mat


# texture cache (keyed by object identity, like the Swift's ObjectIdentifier map)
static var _tex_cache := {}


static func _texture(t: Texture2D) -> SoftRaster.RasterTexture:
	var key := t.get_instance_id()
	if _tex_cache.has(key):
		return _tex_cache[key]
	var img := t.get_image()
	if img == null:
		return null
	var rt := SoftRaster.RasterTexture.from_image(img)
	if rt != null:
		_tex_cache[key] = rt
	return rt


# MARK: - matrices

static func gl_perspective(fov_y: float, aspect: float, near: float, far: float) -> Projection:
	var f := 1.0 / tan(fov_y * 0.5)
	var p := Projection.IDENTITY
	p.x = Vector4(f / aspect, 0, 0, 0)
	p.y = Vector4(0, f, 0, 0)
	p.z = Vector4(0, 0, (far + near) / (near - far), -1)
	p.w = Vector4(0, 0, (2 * far * near) / (near - far), 0)
	return p


# MARK: - live mode ('R' easter egg)

## The in-game software-render mode. Ports:
##  - GameViewController.swift:267-297 — `softwareRender` flag, one frame in
##    flight, geometry snapshot on the render thread + pixel fill on a
##    background queue, image handed to the HUD when done.
##  - HUDScene.swift:350-368 (rasterBG / setRasterImage) — the finished frame
##    fills the screen with NEAREST filtering, drawn over the 3D view and
##    under the HUD (Swift: zPosition -1000, below the whole HUD tree).
##
## Display path (this port): a CanvasLayer at layer 50 — above the 3D
## viewport, and the HUD's CanvasLayer must use a layer > 50 to stay on top —
## holding a full-viewport TextureRect (STRETCH_SCALE, so 640x480 upscales
## chunky; TEXTURE_FILTER_NEAREST = SKTexture .nearest). The 640x480 frame is
## uploaded via ImageTexture.update each time a raster finishes.
##
## Expected wiring from game_controller.gd:
##     var soft_render := SceneRasterizer.Live.new()
##     add_child(soft_render)                    # anywhere in the tree
##     soft_render.attach(world, camera_node)    # once world + camera exist
##     soft_render.update(dt)                    # every frame (game update)
##     # 'R' keypress (GameViewController.swift:301-304, keyCode 15, only
##     # while not chatting):
##     var on := soft_render.toggle()
##     hud.flash_message("Software Rasterizer: ON" if on else "Software Rasterizer: OFF")
##
## PERFORMANCE GATE: measured on the dev Mac (M-series, Tropicali, 92 draw
## calls): snapshot ~0.17 s (main thread) + fill ~1.0 s + dither ~0.13 s
## (worker thread) ≈ 1.3 s per 640x480 frame — ~0.8 fps, under the 5 fps
## usability bar. The mode still works: gameplay/HUD run at full rate on the
## GPU renderer underneath and the retro frame refreshes every ~1.3 s.
## Fidelity beats framerate here (project instruction), so the algorithm and
## resolution are UNCHANGED. Optimizations deliberately NOT applied:
## incremental edge stepping / span scanline fill, nearest instead of
## bilinear sampling, lower internal resolution, precomputed 565 LUTs,
## caching static-geometry snapshots between frames, or moving the fill into
## C# / GDExtension.
class Live extends Node:
	var enabled := false
	var _world: World = null
	var _camera: Camera3D = null
	var _thread: Thread = null           # the Swift's rasterQueue: one frame in flight
	var _layer: CanvasLayer = null
	var _rect: TextureRect = null
	var _texture: ImageTexture = null

	func _ready() -> void:
		_layer = CanvasLayer.new()
		_layer.layer = 50    # above the 3D view, below the HUD (see class comment)
		_layer.visible = false
		_rect = TextureRect.new()
		_rect.set_anchors_preset(Control.PRESET_FULL_RECT)
		_rect.stretch_mode = TextureRect.STRETCH_SCALE
		_rect.texture_filter = CanvasItem.TEXTURE_FILTER_NEAREST   # HUDScene.swift:364
		# full-screen display surface; the HUD's own menus are hit-tested via
		# _unhandled_input, so this Control must not eat their clicks
		_rect.mouse_filter = Control.MOUSE_FILTER_IGNORE
		_layer.add_child(_rect)
		add_child(_layer)

	func _exit_tree() -> void:
		if _thread != null:
			_thread.wait_to_finish()
			_thread = null

	func attach(world: World, camera: Camera3D) -> void:
		_world = world
		_camera = camera

	## GameViewController.toggleSoftwareRender minus the HUD flash (the caller
	## owns the HUD): returns the new state so game_controller can flash
	## "Software Rasterizer: ON/OFF" (GameViewController.swift:293-297).
	func toggle() -> bool:
		set_enabled(not enabled)
		return enabled

	func set_enabled(v: bool) -> void:
		enabled = v
		if not v and _layer != null:
			_layer.visible = false   # setRasterImage(nil) — GameViewController.swift:295

	## Call once per frame. Mirrors the renderer(_:updateAtTime:) block
	## (GameViewController.swift:269-284): harvest a finished frame, then, if
	## idle, snapshot the scene on the main thread and kick the heavy fill onto
	## a background thread so the HUD/input stay at full framerate. One frame
	## in flight at a time.
	func update(_dt: float) -> void:
		if _thread != null and not _thread.is_alive():
			var img: Image = _thread.wait_to_finish()
			_thread = null
			# apply only if still enabled (the Swift re-checks softwareRender
			# on completion — GameViewController.swift:281)
			if enabled and img != null:
				_show(img)
		if enabled and _thread == null and _world != null and _camera != null:
			# 640x480 — the DX7-era render resolution (GameViewController.swift:274)
			var snap := SceneRasterizer.snapshot(_world.root, _camera, Vector2i(640, 480))
			var s := SceneRasterizer.settings(_world.map)
			_thread = Thread.new()
			_thread.start(_raster_task.bind(snap, s))

	func _raster_task(snap: Snapshot, s: Settings) -> Image:
		var t0 := Time.get_ticks_usec()
		var raster := SceneRasterizer.rasterize(snap, s)
		var t1 := Time.get_ticks_usec()
		var img := raster.image(s.sky_bottom, s.gamma)
		var t2 := Time.get_ticks_usec()
		print("softraster frame: fill %.1f ms + dither %.1f ms (%d draw calls)" %
				[(t1 - t0) / 1000.0, (t2 - t1) / 1000.0, snap.calls.size()])
		return img

	func _show(img: Image) -> void:
		if _texture == null or _texture.get_width() != img.get_width() \
				or _texture.get_height() != img.get_height():
			_texture = ImageTexture.create_from_image(img)
			_rect.texture = _texture
		else:
			_texture.update(img)
		_layer.visible = true
