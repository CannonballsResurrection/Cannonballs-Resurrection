class_name WorldDressing
# Port of macos/Sources/Cannonballs/WorldDressing.swift.
#
# World-dressing layers from the original engine (Island.java + Entity_Object_LensFlare.java):
# - WATERANIMATION: the water tile cycles through 32 64px frames at 0.08s/frame
# - Shoreline: two slightly tilted, bobbing, scale-pulsing additive water planes
#   lapping the island edge (SHORELINE base is pure black; the visible shimmer
#   is the water layer at 10x tiling)
# - Cloud shadows: CLOUDSHADOW multiplied over the terrain at 4x tiling,
#   drifting with the wind (offset -= wind * 0.01 * dt)
# - Lens flare: SUN + FLARE + CIRCLE1/2 + DOT1/2 camera-space elements along
#   the sun-to-screen-center axis on maps with hasSun
#
# GDScript port notes (windows/PORTING.md):
# - The Swift's Timer / SCNAction closures become self-driving nodes or
#   World.update per-frame math (the CABasicAnimation rule).
# - Where the decompiled Java disagrees with the Swift, the Java wins
#   (PORTING.md rule 4); every such spot is marked "Java outranks" inline.


# MARK: - Animated water (Island.setWaterFrame; Island.java:313-318, 32 frames @ 0.08s)

static var _water_tile_cache: Array = []


## The 32 original 64px WATERANIMATION frames as textures (8 cols x 4 rows,
## rows counted from the top). Image is top-left origin, so the Swift's
## AppKit bottom-left flip drops out. The Swift's animateWater Timer becomes
## World.update cycling these at 0.08 s/frame.
static func water_tiles() -> Array:
	if not _water_tile_cache.is_empty():
		return _water_tile_cache
	var sheet := Assets.image("IMAGES/FX/WATERANIMATION.png")
	if sheet == null:
		return []
	var tiles: Array = []
	for n in 32:
		var col := n % 8
		@warning_ignore("integer_division")
		var row := n / 8
		var tile := Image.create(64, 64, false, sheet.get_format())
		tile.blit_rect(sheet, Rect2i(col * 64, row * 64, 64, 64), Vector2i.ZERO)
		tiles.append(ImageTexture.create_from_image(tile))
	_water_tile_cache = tiles
	return tiles


# MARK: - Shoreline planes (Island.java:735-753, 332-336)

## One bobbing, scale-pulsing shoreline plane. Island.java:332-336: the two
## planes bob on SinTable[3] and pulse on SinTable[4] / SinTable[7]; the
## SinTable rates are transcribed from Main.java:307-324 (SinTable[3] = 1.0/s,
## SinTable[4] = 1.5/s, SinTable[7] = sin(SinPosition[3]) = 1.0/s — a
## decompile-visible quirk, Main.java:323). The Swift approximated these with
## sin(t*1.6 + index) — Java outranks, PORTING.md rule 4.
class ShorelinePlane extends MeshInstance3D:
	var base_y := 0.0
	var pulse_rate := 1.0
	var pulse_x := 0.1
	var pulse_z := 0.1
	var _t := 0.0

	func _process(dt: float) -> void:
		_t += dt
		position.y = base_y + sin(_t) * 0.5          # baseY + SinTable[3] * 0.5
		var px := 1.0 + sin(pulse_rate * _t) * pulse_x
		var pz := 1.0 + sin(pulse_rate * _t) * pulse_z
		# SCN scaled the vertical plane (s, s, 1); PlaneMesh is already flat,
		# so the plane axes are local x/z
		scale = Vector3(px, 1, pz)


static func add_shoreline(world) -> void:
	var size := Terrain.world_size()
	var c: Vector3 = world.center()
	# (plane scale, tilt deg, baseY, pulse SinTable rate, x pulse, z pulse).
	# BOTH planes render the 1.2x mesh: Island.java:750 attaches WaterMesh2
	# (Width * 1.2, :737) to WaterLevel3 as well; the 1.15x WaterMesh3 (:740)
	# is created but never attached. The Swift port sized the second plane
	# 1.15x — Java outranks, PORTING.md rule 4.
	for params in [[1.2, -3.0, 0.75, 1.5, 0.1, 0.09],
			[1.2, 5.0, 0.65, 1.0, 0.095, 0.1]]:
		var plane_scale: float = params[0]
		var tilt: float = params[1]
		var base_y: float = params[2]
		var plane := PlaneMesh.new()
		plane.size = Vector2(size * plane_scale, size * plane_scale)
		var m := StandardMaterial3D.new()
		m.albedo_texture = Props.PropTex.image("water.png")
		m.texture_repeat = true                                   # wrapS/wrapT = .repeat
		m.uv1_scale = Vector3(10, 10, 1)                          # layer-1 10x tiling
		m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED     # lightingModel = .constant
		m.blend_mode = BaseMaterial3D.BLEND_MODE_ADD              # blendMode = .add
		# add-blend ignores material transparency: darken the source instead
		# (SCN m.multiply.contents 0.13 white → albedo tint, PORTING.md)
		m.albedo_color = Color(0.13, 0.13, 0.13)
		m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
		m.depth_draw_mode = BaseMaterial3D.DEPTH_DRAW_DISABLED    # writesToDepthBuffer = false
		m.render_priority = 5                                     # renderingOrder = 5
		var node := ShorelinePlane.new()
		node.mesh = plane
		node.material_override = m
		node.name = "shoreline"
		node.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
		# SCN euler (-pi/2, 0, tilt): PlaneMesh is already horizontal, only the
		# tilt roll remains
		node.rotation.z = deg_to_rad(tilt)
		node.position = Vector3(c.x, base_y, c.z)
		node.base_y = base_y
		node.pulse_rate = params[3]
		node.pulse_x = params[4]
		node.pulse_z = params[5]
		world.root.add_child(node)


# MARK: - Cloud shadows (terrain layer 2: multiply, 4x tiling, wind drift)

# layer 1: GRIT detail at 40x tiling (normalized by its mean 0.647 to keep
# brightness); layer 2: CLOUDSHADOW multiply at 4x, wind-drifting. The
# SceneKit surface shader-modifier becomes a spatial shader replacing the
# terrain's StandardMaterial3D (Godot has no modifier hooks); it reproduces
# that material's setup — lambert shading (roughness 1, specular 0), clamped
# albedo — and shares the terrain's live decal canvas texture, so bakes keep
# appearing through ImageTexture.update.
const _TERRAIN_SHADER := "
shader_type spatial;
uniform sampler2D albedo_tex : source_color, filter_linear_mipmap, repeat_disable;
uniform sampler2D grit_tex : source_color, filter_linear_mipmap, repeat_enable;
uniform sampler2D cloud_tex : source_color, filter_linear_mipmap, repeat_enable;
uniform vec2 cloud_offset = vec2(0.0, 0.0);

void fragment() {
	vec3 albedo = texture(albedo_tex, UV).rgb;
	albedo *= texture(grit_tex, UV * 40.0).rgb / 0.647;
	albedo *= texture(cloud_tex, UV * 4.0 + cloud_offset).rgb;
	ALBEDO = albedo;
	ROUGHNESS = 1.0;
	SPECULAR = 0.0;
}
"


static func add_cloud_shadows(world) -> void:
	var cloud := FXSprites.image("CLOUDSHADOW")
	var grit := FXSprites.image("GRIT")
	if cloud == null or grit == null:
		return
	var sh := Shader.new()
	sh.code = _TERRAIN_SHADER
	var mat := ShaderMaterial.new()
	mat.shader = sh
	mat.set_shader_parameter("albedo_tex", world.terrain.material.albedo_texture)
	mat.set_shader_parameter("grit_tex", grit)
	mat.set_shader_parameter("cloud_tex", cloud)
	world.terrain.node.material_override = mat


## Advance the cloud-shadow drift (Island.java:337-347: CloudOffset -=
## wind * 0.01 * dt, wrapped to 0..1). GDScript has no inout — the caller
## (GameController, per frame) keeps the returned offset. `material` is the
## ShaderMaterial add_cloud_shadows installed on world.terrain.node.
static func drift_clouds(material: ShaderMaterial, wind: Vector3, dt: float,
		offset: Vector2) -> Vector2:
	offset.x -= wind.x * dt * 0.01
	offset.y += wind.z * dt * 0.01
	offset.x -= floorf(offset.x)
	offset.y -= floorf(offset.y)
	material.set_shader_parameter("cloud_offset", offset)
	return offset


# MARK: - Lens flare (Entity_Object_LensFlare.java)

class LensFlare:
	var _group := Node3D.new()             # camera-space container at depth D
	var _elements: Array = []              # [sun, flare, circle1, circle2, dot1, dot2]
	var _materials: Array = []
	var _fade := 0.0
	var _sun_dir: Vector3
	const DEPTH := 20.0

	# (image, WT size, offset factor along sun->center axis)
	const SPEC := [
		["FX_SUN", 0.219, 1.0], ["FX_FLARE", 1.368, 1.0],
		["FX_CIRCLE1", 0.383, 0.3], ["FX_CIRCLE2", 1.368, 0.0],
		["FX_DOT1", 0.068, 0.5], ["FX_DOT2", 0.082, -0.5],
	]

	func _init(sun_vector: Vector3, camera: Node3D) -> void:
		_sun_dir = sun_vector.normalized()
		for spec in SPEC:
			var s: float = spec[1] * DEPTH / 2
			var quad := QuadMesh.new()
			quad.size = Vector2(s, s)
			var m := StandardMaterial3D.new()
			m.albedo_texture = FXSprites.image(spec[0])
			m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED  # lightingModel = .constant
			m.blend_mode = BaseMaterial3D.BLEND_MODE_ADD           # blendMode = .add
			m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
			m.depth_draw_mode = BaseMaterial3D.DEPTH_DRAW_DISABLED # writesToDepthBuffer = false
			m.no_depth_test = true                                 # readsFromDepthBuffer = false
			m.render_priority = 127         # renderingOrder = 1000, clamped to Godot's max
			m.albedo_color.a = 0.0          # group.opacity = 0 at start
			var n := MeshInstance3D.new()
			n.mesh = quad
			n.material_override = m
			n.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
			_group.add_child(n)
			_elements.append(n)
			_materials.append(m)
		_group.position = Vector3(0, 0, -DEPTH)
		camera.add_child(_group)

	## `pov` is the Camera3D; `terrain` an optional Terrain for occlusion.
	func update(pov: Camera3D, dt: float, terrain = null) -> void:
		# sun direction in camera space (the camera looks down -z)
		var d := (pov.global_transform.basis.inverse() * _sun_dir).normalized()
		var flare_visible := false
		var sx := 0.0
		var sy := 0.0
		if d.z < 0:                                    # in front (-z forward)
			# note: Camera3D.fov is vertical, like SCNCamera.fieldOfView
			var half_h := tan(deg_to_rad(pov.fov) / 2)
			sx = (d.x / -d.z) / half_h
			sy = (d.y / -d.z) / half_h
			flare_visible = absf(sx) < 1.4 and absf(sy) < 1.1
		# terrain occlusion (original: collision ray toward the sun)
		if flare_visible and terrain != null:
			var cam_pos := pov.global_position
			var t := 10.0
			while t < 400.0:
				var p := cam_pos + _sun_dir * t
				if terrain.height(p.x, p.z) > p.y:
					flare_visible = false
					break
				t += 20.0
		_fade = clampf(_fade + (4.0 if flare_visible else -4.0) * dt, 0.0, 1.0)
		if _fade <= 0.0:
			for m in _materials:
				m.albedo_color.a = 0.0
			return
		var half := DEPTH * tan(deg_to_rad(pov.fov) / 2)
		var px := sx * half
		var py := sy * half
		var dist := mini(50, int(sqrt(sx * sx * 4 + sy * sy * 4) * 25))
		var element_alpha := maxf(0, 200.0 - dist * 4) / 255.0
		for i in _elements.size():
			var k: float = SPEC[i][2]
			_elements[i].position = Vector3(px * k, py * k, 0)
			# SCNNode.opacity → additive albedo alpha (fx_sprites.gd set_opacity
			# note); the group fade multiplies into each element
			var a := (maxf(0, 250.0 - dist * 2) / 255.0) if i == 0 else element_alpha
			_materials[i].albedo_color.a = a * _fade
		_elements[0].rotation.z = sx * 20 * PI / 180
