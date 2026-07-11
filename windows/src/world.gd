class_name World
extends RefCounted
# Port of macos/Sources/Cannonballs/World.swift.
# A fully assembled island world: scene graph + terrain + props.
#
# PORT STATUS: terrain / water / sky background / fog / lights are live.
# Props, decorations, clouds, fireflies, the SKIES actor, shoreline and
# cloud shadows are parsed but stubbed pending src/props.gd, src/fx_sprites.gd,
# src/sky_actor.gd, src/world_dressing.gd (tasks #4/#5). Every stub is marked
# TODO(port) with the Swift reference — fill the bodies, keep the parse.

var map: MapCatalog.MapInfo
var terrain: Terrain
var root := Node3D.new()             # SCNScene.rootNode equivalent
var props: Array = []
var _decorations: Array = []         # terrain-following (Particle_Object_Decoration)
var sky_actor: Node3D = null         # original sky dome + horizon islands
var props_root := Node3D.new()
var effects_root := Node3D.new()     # splashes, smoke, etc.
var water_color: Color

var _water_material: StandardMaterial3D = null
var _water_node: MeshInstance3D = null
var _time := 0.0
var _water_tiles: Array = []         # 32 WATERANIMATION frames (WorldDressing)
var _water_frame := -1


func _init(p_map: MapCatalog.MapInfo) -> void:
	map = p_map
	terrain = Terrain.new(map)
	root.name = "world"
	water_color = map.ambient_rgb

	root.add_child(terrain.node)
	props_root.name = "props"
	root.add_child(props_root)
	root.add_child(effects_root)

	_build_water()
	_build_sky_and_lights()
	_load_objects()
	WorldDressing.add_shoreline(self)              # World.swift:30
	WorldDressing.add_cloud_shadows(self)          # World.swift:31


func center() -> Vector3:
	var half := Terrain.world_size() / 2
	return Vector3(half, 0, half)


# MARK: - Water

func _build_water() -> void:
	var plane := PlaneMesh.new()
	plane.size = Vector2(4000, 4000)   # SCNPlane 4000x4000, rotated flat in Swift; PlaneMesh is already horizontal (+Y normal)
	var m := StandardMaterial3D.new()
	# Original: tiled WATER texture, tinted by the map's water color, with the
	# WATERANIMATION overlay scrolling. Tile + slow UV scroll approximates the
	# engine's animated water using the real texture.
	var water_tex := Assets.texture("PROPTEX/water.png")
	if water_tex != null:
		m.albedo_texture = water_tex
		m.texture_repeat = true
		m.uv1_scale = Vector3(70, 70, 1)     # original coord scale 70
		m.albedo_color = water_color          # full map tint (no lightening) — SCNMaterial.multiply
		# scroll the tiles slowly (Island.java: offsets -0.1/-0.088 per sec):
		# CABasicAnimation contentsTransform.translation.x 0→1 over 14 s repeat,
		# advanced per-frame in update() below.
		# the ORIGINAL animated water: cycle the 32 WATERANIMATION tiles @0.08s
		# (WorldDressing.animateWater; the Swift's Timer becomes per-frame
		# math in update() below — PORTING.md CABasicAnimation rule)
		_water_tiles = WorldDressing.water_tiles()
	else:
		m.albedo_color = water_color
	m.albedo_color.a = 0.9                    # transparency = 0.9
	m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED   # original: WaterAnimation.setFullbright()
	var water := MeshInstance3D.new()
	water.mesh = plane
	water.material_override = m
	water.position = Vector3(center().x, 0.15, center().z)
	water.name = "water"
	# subtle bob: CABasicAnimation position.y 0.05→0.55, 2.6 s, autoreverse,
	# easeInEaseOut — advanced per-frame in update() below.
	root.add_child(water)
	_water_material = m
	_water_node = water
	# opaque deep-water disk far below so ocean isn't see-through to sky
	var deep := MeshInstance3D.new()
	var deep_mesh := PlaneMesh.new()
	deep_mesh.size = Vector2(4000, 4000)
	var dm := StandardMaterial3D.new()
	dm.albedo_color = water_color.lerp(Color.BLACK, 0.55)
	dm.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	deep.mesh = deep_mesh
	deep.material_override = dm
	deep.position = Vector3(center().x, -3.5, center().z)
	root.add_child(deep)


# MARK: - Sky, fog, lights

func _build_sky_and_lights() -> void:
	var horizon := map.ambient_rgb
	var zenith: Color
	if map.sky_name == "NIGHT":
		zenith = Color(0.05, 0.04, 0.18)
	elif map.sky_name == "PURPLE":
		zenith = Color(0.45, 0.25, 0.7)
	elif map.sky_name == "DESERT":
		zenith = Color(0.55, 0.65, 0.95)
	else:
		zenith = Color(0.35, 0.55, 0.95)

	var env := Environment.new()
	# gradient fallback (Swift: World.gradientImage zenith→horizon+35% white)
	env.background_mode = Environment.BG_COLOR
	env.background_color = horizon.lerp(Color.WHITE, 0.35)

	# Real sky: the decoded panorama as the wrap-around background, PLUS the
	# original SKIES actor geometry (dome + horizon-island billboards + moon/
	# stars) following the camera. (Software rasterizer keeps its gradient sky.)
	var sky_img := Assets.texture("SKIES/%s.png" % map.sky_name)
	if sky_img != null:
		var sky_mat := PanoramaSkyMaterial.new()
		sky_mat.panorama = sky_img
		var sky := Sky.new()
		sky.sky_material = sky_mat
		env.background_mode = Environment.BG_SKY
		env.sky = sky
	# the original SKIES actor geometry (SkyActor follows the camera itself
	# in _process — see sky_actor.gd) — World.swift:110-114
	var sky := SkyActor.load_sky(map.sky_name)
	if sky != null:
		sky.position = Vector3(center().x, 0, center().z)
		root.add_child(sky)
		sky_actor = sky

	# scene.fog* — SceneKit fogStartDistance 420 / fogEndDistance 1600 /
	# fogDensityExponent 1.5 → Godot depth fog begin/end + curve
	env.fog_enabled = true
	env.fog_mode = Environment.FOG_MODE_DEPTH
	env.fog_light_color = horizon.lerp(Color.WHITE, 0.3)
	env.fog_depth_begin = 420
	env.fog_depth_end = 1600
	env.fog_depth_curve = 1.5

	# ambient light: color blended 50% white; intensity 380 (night) / 520 → energy /1000
	env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	env.ambient_light_color = map.ambient_rgb.lerp(Color.WHITE, 0.5)
	env.ambient_light_energy = 0.38 if map.sky_name == "NIGHT" else 0.52

	var world_env := WorldEnvironment.new()
	world_env.environment = env
	root.add_child(world_env)

	var sun := DirectionalLight3D.new()
	sun.light_color = map.sun_rgb
	sun.light_energy = 0.62 if map.sky_name == "NIGHT" else 0.95   # SCN intensity 620/950 lumens ÷1000
	# the 2002 engine had NO shadow mapping: gameplay objects carry SHADOW
	# blob patches instead (FXSprites.blobShadow)
	sun.shadow_enabled = false
	var dir := map.sun_vector.normalized()
	sun.position = center() + dir * 400
	root.add_child(sun)
	sun.look_at_from_position(sun.position, center(), Vector3.UP)

	# No procedural sun/moon balls: the original's ONLY celestial visuals
	# are the sky actor's authored parts (the NIGHT actor's `moon`
	# billboard, Island.java:758/1208-1213) and, on HasSun maps, the
	# Entity_Object_LensFlare (Island.java:1214-1215). The old world-space
	# moon sphere parallaxed across the camera-following horizon-island
	# billboards and painted over them.


# MARK: - objects.dat

func _load_objects() -> void:
	var raw := Assets.text(map.objects_path())
	if raw.is_empty():
		return
	var vs := Terrain.VERTEX_SCALE
	for line in raw.split("\n", false):
		var f := line.strip_edges().split(":")
		if f.size() == 0:
			continue
		var tag := f[0]
		match tag:
			"<PROP>":
				# <PROP>:MEDIA/PROPS/N:x,z:rotDeg
				if f.size() < 4:
					continue
				var c := _floats(f[2])
				if c.size() < 2:
					continue
				var wx: float = c[0] * vs
				var wz: float = c[1] * vs
				var wy := terrain.height(wx, wz)
				var spec := Props.PropSpec.load_spec(f[1].get_file())
				add_prop(Props.Prop.new(spec, Vector3(wx, wy, wz), f[3].to_float(), 0.0))
			"<PROPPOS>":
				# <PROPPOS>:MEDIA/PROPS/N:x,y,z:rot — explicit height
				if f.size() < 4:
					continue
				var v := _floats(f[2])
				if v.size() != 3:
					continue
				var spec := Props.PropSpec.load_spec(f[1].get_file())
				add_prop(Props.Prop.new(spec, Vector3(v[0] * vs, v[1], v[2] * vs),
						f[3].to_float(), 0.0))
			"<DECORATION>", "<DECORATIONWATER>":
				# :path:x,z:rotDeg:sx,sy
				if f.size() < 5:
					continue
				var c := _floats(f[2])
				if c.size() < 2:
					continue
				var wx: float = c[0] * vs
				var wz: float = c[1] * vs
				var sc := _floats(f[4])
				var deco := Props.PropGeometry.decoration(f[1],
						Vector2(sc[0], sc[1]) if sc.size() >= 2 else Vector2(1, 1))
				# water decorations pin Y = 0.0 (Particle_Object_Decoration.java:50)
				var y: float = 0.0 if tag == "<DECORATIONWATER>" else terrain.height(wx, wz)
				deco.position = Vector3(wx, y, wz)
				deco.rotation.y = deg_to_rad(f[3].to_float())
				props_root.add_child(deco)
				if tag == "<DECORATION>":
					_decorations.append(deco)
			"<CLOUD>":
				# :layer:x,h,z:radius,xMul,yMul (Particle_Object_Cloud ctor args)
				if f.size() < 4:
					continue
				var v := _floats(f[2])
				if v.size() != 3:
					continue
				var params := _floats(f[3])
				FXSprites.cloud(Vector3(v[0] * vs, v[1], v[2] * vs),
						params[0] if params.size() > 0 else 40.0,
						params[1] if params.size() > 1 else 1.0,
						params[2] if params.size() > 2 else 1.0,
						self)
			"<FIREFLY>":
				# <FIREFLY>:x,y,z — a drifting glowing mote (night maps).
				# NO z flip: objects.dat coordinates are heightmap-ROW space
				# for every tag. Island.java:882-927 flips them all
				# (Height - z) into world space, and getTerrainHeight flips
				# world z back (Island.java: f2 = Width*VertexScale - f2), so
				# the flips cancel; the clone keeps terrain AND objects in
				# file-row space directly. The old (grid - z) here was a
				# double flip that mirrored fireflies across the map.
				var v := _floats(f[1])
				if v.size() != 3:
					continue
				var moth := Props.PropGeometry.firefly()
				moth.position = Vector3(v[0] * vs, v[1], v[2] * vs)
				root.add_child(moth)   # Swift parents fireflies to scene.rootNode
			# NOTE: VOLCANO/objects.dat contains <FIRE> lines, but the
			# original's objects.dat parser has no <FIRE> handler
			# (Island.java:879-927; only Prop.java:501 reads <FIRE>, from
			# prop.dat) — those lines are dead data the 2002 game ignored,
			# so the clone ignores them too.
			_:
				pass


func _floats(s: String) -> Array:
	var out: Array = []
	for part in s.split(","):
		if part.is_valid_float():
			out.append(part.to_float())
	return out


# MARK: - Per-frame update (water animations were CAAnimations on macOS)

func update(dt: float) -> void:
	_time += dt
	terrain.update(dt)
	if _water_material != null:
		# scroll BOTH axes at the original rates (Island.java:327-331: offsets
		# -0.1/-0.088 per sec; the Swift's x-only 14 s cycle was an
		# approximation — Java wins, back-port tracked)
		_water_material.uv1_offset.x = fmod(-0.1 * _time, 1.0)
		_water_material.uv1_offset.y = fmod(-0.088 * _time, 1.0)
		# the ORIGINAL animated water: the frame advances every 0.08 s and
		# wraps at 32 (Island.java:313-318; WorldDressing.animateWater)
		if not _water_tiles.is_empty():
			var frame := int(_time / 0.08) % 32
			if frame != _water_frame:
				_water_frame = frame
				_water_material.albedo_texture = _water_tiles[frame]
	if _water_node != null:
		# bob: position.y 0.05→0.55 over 2.6 s, autoreverse, easeInEaseOut
		var phase := fmod(_time, 5.2) / 2.6
		var tri := phase if phase < 1.0 else 2.0 - phase
		var eased := tri * tri * (3.0 - 2.0 * tri)   # smoothstep ≈ CA easeInEaseOut
		_water_node.position.y = lerpf(0.05, 0.55, eased)


# MARK: - Props (World.swift:238-328)

## Decorations follow the terrain like Particle_Object_Decoration: re-ground
## after deformation; sinking below the water removes them in a smoke puff.
func reground_decorations() -> void:
	var kept: Array = []
	for n in _decorations:
		var x: float = n.position.x
		var z: float = n.position.z
		var y := terrain.height(x, z)
		if y <= 0.0:
			FXSprites.smoke(Vector3(x, y, z),
					Vector3(randf_range(-0.5, 0.5), randf_range(-0.5, 0.5),
							randf_range(-0.5, 0.5)),
					randf_range(0.3, 0.8), self)
			n.queue_free()
		else:
			n.position.y = y
			kept.append(n)
	_decorations = kept


func add_prop(prop: Props.Prop) -> void:
	props.append(prop)
	props_root.add_child(prop.node)
	# prop.dat <FIRE>: a persistent fire smoke column at the prop-local
	# offset (Prop.java:628-629, SmokeColumn(1, ..., true, 999)); dies with
	# the prop (Prop.java:242-243)
	if prop.spec.fire != null:
		var fire_pos: Vector3 = prop.node.transform * prop.spec.fire
		FXSprites.smoke_column(fire_pos, 999.0, self, true, false,
				func() -> bool: return prop.alive)


func destroy_prop(prop: Props.Prop) -> void:
	prop.alive = false
	# removeFromParentNode — nothing re-attaches a destroyed prop, so free it
	# (its collision body leaves the physics space with it)
	prop.node.queue_free()


## First live prop whose collision cylinder contains the point (older than
## 3 s of game time). Returns Props.Prop or null.
func prop_hit(x: float, y: float, z: float, game_time: float, pad := 2.0):
	for p in props:
		if p.alive and game_time - p.age_at_spawn > 3.0 \
				and p.check_collision(x, y, z, pad):
			return p
	return null


## Anything solid above this XZ position (blocks Tower / Molehill)?
func object_above(x: float, z: float) -> bool:
	for p in props:
		if not p.alive:
			continue
		var dx: float = x - p.position.x
		var dz: float = z - p.position.z
		if dx * dx + dz * dz < p.spec.radius * p.spec.radius:
			return true
	return false


## Weapon.java:1029-1031 — the swept segment test from the shot's last
## position to its current one against the 3D meshes of WTCOLLIDEABLE
## props (Prop.java:534-536 setCollisionMask(2)). This — not the
## destructible-prop cylinder — is what blocked shots on the Nightbridge
## deck, the ship hull, etc.
## hitTestWithSegment → a physics ray against the per-prop trimesh bodies
## (PORTING.md); layer 2 mirrors the original mask, backface_collision on the
## shapes matches backFaceCulling: false. The single ray reports the nearest
## hit along a→b — the original engine's sweep semantics (the Swift iterated
## props in array order instead). Returns Props.Prop or null.
func collide_segment(a: Vector3, b: Vector3):
	if not root.is_inside_tree() or a.is_equal_approx(b):
		return null
	var q := PhysicsRayQueryParameters3D.create(a, b, 2)
	q.hit_from_inside = true
	q.hit_back_faces = true
	var hit := root.get_world_3d().direct_space_state.intersect_ray(q)
	if hit.is_empty():
		return null
	var collider: Object = hit.get("collider")
	if collider != null and collider.has_meta("prop"):
		var p = collider.get_meta("prop")
		if p.alive:
			return p
	return null


## Weapon.java:165-171 (also 590-591, 828-830) — the mask-8 probe: a ray
## from (x, y+400, z) straight down to (x, y-drop, z) against STANDABLE
## prop meshes (Prop.java:538-540 setCollisionMask(10)). Yields the deck
## surface height + impact normal under the point.
## The downward ray's first hit IS the highest standable surface (what the
## Swift's best-of-all-hits loop selected). Returns
## {"height": float, "normal": Vector3} or null.
func standable_surface(x: float, y: float, z: float, drop: float):
	if not root.is_inside_tree():
		return null
	var q := PhysicsRayQueryParameters3D.create(
			Vector3(x, y + 400.0, z), Vector3(x, y - drop, z), 8)
	q.hit_back_faces = true
	var hit := root.get_world_3d().direct_space_state.intersect_ray(q)
	if hit.is_empty():
		return null
	var collider: Object = hit.get("collider")
	if collider == null or not collider.has_meta("prop") \
			or not collider.get_meta("prop").alive:
		return null
	var wn: Vector3 = hit.normal
	if wn.length() > 0.0001:
		wn = wn.normalized()
	return {"height": (hit.position as Vector3).y, "normal": wn}
