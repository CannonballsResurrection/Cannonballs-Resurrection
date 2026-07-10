class_name Props
# Port of macos/Sources/Cannonballs/Props.swift.
# Prop stats (PROPS/<NAME>/prop.dat), the runtime Prop, the original decoded
# prop textures, and the geometry builders: the SOLVED skinned exports first,
# then OBJ models, then procedural stand-ins skinned with original textures.
#
# The Swift's top-level types stay inner classes here (one class_name per
# file): Props.PropSpec / Props.Prop / Props.PropTex / Props.PropDims /
# Props.PropGeometry.
#
# GDScript port notes (windows/PORTING.md):
# - CABasicAnimation / CAKeyframeAnimation → self-driving Animated nodes whose
#   tick Callable advances the transcribed math per frame.
# - hitTestWithSegment (prop mesh collision) → per-prop StaticBody3D +
#   ConcavePolygonShape3D, built ONLY for wtCollideable / standable specs and
#   queried by World.collide_segment / World.standable_surface.
# - Where the decompiled Java disagrees with the Swift, the Java wins
#   (PORTING.md rule 4); every such spot is marked "Java outranks" inline.


## Self-driving per-frame animation node (the macOS build used SCNAction /
## CAAnimation; each owner here advances its own transcribed math — the
## CABasicAnimation rule, PORTING.md). `tick` captures its own state.
class Animated extends Node3D:
	var tick: Callable            # func(n: Node3D, dt: float) -> void

	func _process(dt: float) -> void:
		if tick.is_valid():
			tick.call(self, dt)


static func _floats(s: String) -> Array:
	var out: Array = []
	for part in s.split(","):
		if part.strip_edges().is_valid_float():
			out.append(part.to_float())
	return out


## Cannon.styleCannonMaterials — cannon.gd is a concurrent sibling port;
## duck-load it so this file parses standalone (PORTING.md typing rule; the
## same pattern as fx_sprites.gd debris_chunk). Assumed API:
## static style_cannon_materials(node: Node3D).
static func style_cannon_materials(node: Node3D) -> void:
	if ResourceLoader.exists("res://src/cannon.gd"):
		var cannon_script = load("res://src/cannon.gd")
		if cannon_script != null:
			cannon_script.style_cannon_materials(node)


# MARK: - Prop stats (from PROPS/<NAME>/prop.dat)

class PropSpec:
	var name: String
	var radius := 5.0
	var height := 20.0
	var destructible := false
	var explosive := false
	var standable := false
	## <WTCOLLIDEABLE> — the actor mesh joins collision mask 2 (Prop.java:534-536),
	## the mask projectiles sweep against (Weapon.java:1031).
	var wt_collideable := false
	## <FIRE>:x,y,z — a persistent fire smoke column at this prop-local offset
	## (Prop.java:501-512 parse; :628-629 Particle_Object_SmokeColumn(fire)).
	## Vector3, or null when the prop has no fire.
	var fire = null
	## <DEBRIS> pieces: [MODELS/DEBRIS key, local offset Vector3] — tumble on
	## destruction.
	var debris: Array = []

	static var cache := {}

	static func load_spec(spec_name: String) -> PropSpec:
		if cache.has(spec_name):
			return cache[spec_name]
		var spec := PropSpec.new()
		spec.name = spec_name
		var text := Assets.text("PROPS/%s/prop.dat" % spec_name)
		for raw_line in text.split("\n", false):
			var f := raw_line.strip_edges().split(":")
			if f.size() < 2:
				continue
			match f[0]:
				"<RADIUS>":
					if f[1].is_valid_float():
						spec.radius = f[1].to_float()
				"<HEIGHT>":
					if f[1].is_valid_float():
						spec.height = f[1].to_float()
				"<DESTRUCTIBLE>":
					spec.destructible = f[1] == "YES"
				"<EXPLOSIVE>":
					spec.explosive = f[1] == "YES"
				"<STANDABLE>":
					spec.standable = f[1] == "YES"
				"<WTCOLLIDEABLE>":
					spec.wt_collideable = f[1] == "YES"
				"<FIRE>":
					var v := Props._floats(f[1])
					if v.size() == 3:
						spec.fire = Vector3(v[0], v[1], v[2])
				"<DEBRIS>":
					# <DEBRIS>:MEDIA/OBJECTS/X/debrisN.wsad:x,y,z
					if f.size() >= 3:
						var stem := f[1].replace("MEDIA/OBJECTS/", "") \
								.replace(".wsad", "").replace("/", "_")
						var v := Props._floats(f[2])
						if v.size() == 3:
							spec.debris.append(["DEBRIS/%s" % stem,
									Vector3(v[0], v[1], v[2])])
				_:
					pass
		cache[spec_name] = spec
		return spec


# MARK: - Texture loading + real bounding boxes

## Loads and caches the ORIGINAL decoded prop textures staged under
## Resources/PROPTEX/<MODEL>/. Foliage cutouts are pre-keyed RGBA PNGs.
class PropTex:
	## `key` is "<MODEL>/<file>", e.g. "PALM2/frond.png".
	## (the Swift kept its own NSImage cache; Assets.texture already caches)
	static func image(key: String) -> Texture2D:
		return Assets.texture("PROPTEX/%s" % key)


## Real per-model bounding-box dims [W, H, D] in world units, taken from the
## decoded models (bbox_tex.json). Used to size procedural stand-ins to the
## originals' true proportions. objects.dat name -> model.
class PropDims:
	# [width(X), height(Y), depth(Z)]
	const TABLE := {
		"PALM":        Vector3(109.3, 112.0, 107.2),   # PALM2
		"FERNTREE":    Vector3(55.3, 33.3, 65.6),
		"BRUSH2":      Vector3(41.2, 27.5, 37.3),
		"TAILS":       Vector3(12.9, 32.6, 12.7),
		"TIKKI1":      Vector3(15.8, 56.6, 15.6),
		"TIKKI2":      Vector3(15.8, 56.6, 15.6),
		"TIKKI3":      Vector3(15.8, 56.6, 15.6),
		"TNT":         Vector3(10.6, 13.2, 11.2),
		"CHEST":       Vector3(15.2, 10.4, 11.1),
		"HUT":         Vector3(43.3, 40.8, 43.3),
		"TORCH":       Vector3(2.3, 33.1, 2.2),
		"TORCHBEARER": Vector3(10.0, 14.1, 9.1),
		"OBELISK":     Vector3(27.7, 88.7, 28.0),
		"LIGHTHOUSE":  Vector3(104.6, 65.3, 103.0),    # H from max.y (bbox min.y is high)
		"MOUND":       Vector3(151.8, 110.9, 200.7),
		"FIREHEAD":    Vector3(72.9, 123.4, 76.6),
		"SHIP":        Vector3(160.1, 226.7, 350.9),
		"MAST":        Vector3(160.1, 226.7, 350.9),   # uses ship's height for the mast
		"BRIDGE":      Vector3(243.9, 44.4, 315.2),
		"LIGHTBEAM":   Vector3(372.1, 107.7, 107.7),
		"MOUNDBEAM":   Vector3(372.1, 107.7, 107.7),
	}

	## Vector3, or null when the model has no measured bbox.
	static func dims(name: String):
		return TABLE.get(name)


# MARK: - Procedural geometry (skinned with original decoded textures)

class PropGeometry:

	static func material(color: Color, emissive = null) -> StandardMaterial3D:
		var m := StandardMaterial3D.new()
		m.albedo_color = color
		if emissive != null:
			m.emission_enabled = true
			m.emission = emissive
		# lightingModel = .lambert → shaded + metallic_specular 0, roughness 1
		# (PORTING.md)
		m.roughness = 1.0
		m.metallic_specular = 0.0
		return m

	## A material skinned with a decoded texture. `crop` ([x, y, w, h] in unit
	## UV space) selects an atlas sub-region — SceneKit contentsTransform ==
	## uv1_scale/uv1_offset with v = 0 at the image TOP (convention probe:
	## fx_sprites.gd _set_frame). `keyed` = cutout with alpha (double-sided;
	## transparencyMode .aOne + depth write → the established alpha-scissor
	## mapping, skinned_model.gd).
	static func tex_material(key: String, crop: Array = [], keyed := false,
			fallback := Color(0.55, 0.55, 0.55)) -> StandardMaterial3D:
		var m := StandardMaterial3D.new()
		var tex := PropTex.image(key)
		if tex != null:
			m.albedo_texture = tex
		else:
			m.albedo_color = fallback
		m.texture_repeat = false          # wrapS/wrapT = .clamp
		if crop.size() == 4:
			m.uv1_scale = Vector3(crop[2], crop[3], 1)
			m.uv1_offset = Vector3(crop[0], crop[1], 0)
		m.roughness = 1.0                 # lightingModel = .lambert (PORTING.md)
		m.metallic_specular = 0.0
		if keyed:
			m.cull_mode = BaseMaterial3D.CULL_DISABLED   # isDoubleSided
			m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA_SCISSOR
			m.alpha_scissor_threshold = 0.5
			# magnificationFilter = .linear is Godot's default filter
		return m

	static func _quad(width: float, height: float, mat: StandardMaterial3D) -> MeshInstance3D:
		var plane := QuadMesh.new()                      # SCNPlane(width:height:), faces +Z
		plane.size = Vector2(width, height)
		plane.material = mat
		var n := MeshInstance3D.new()
		n.mesh = plane
		return n

	## A vertical crossed-billboard cutout (2-3 intersecting quads) using a keyed
	## texture. Faces are double-sided; the Y-axis billboard option turns the
	## root toward the camera per frame (SCNBillboardConstraint freeAxes .Y —
	## PORTING.md's manual-billboard clause; unused by the shipped callers).
	static func crossed_billboard(key: String, width: float, height: float,
			planes := 2, y_offset := 0.0, billboard := false) -> Node3D:
		var root := Animated.new()
		var mat := tex_material(key, [], true)
		for i in planes:
			var node := _quad(width, height, mat)
			node.position.y = height / 2 + y_offset
			node.rotation.y = float(i) * (PI / float(planes))
			root.add_child(node)
		if billboard:
			root.tick = func(n: Node3D, _dt: float) -> void:
				var cam := n.get_viewport().get_camera_3d()
				if cam != null:
					var to_cam := cam.global_position - n.global_position
					n.rotation.y = atan2(to_cam.x, to_cam.z)
		return root

	# (the old model.json "scale" reader is gone: that number was a fabricated
	# heuristic — prop.dat HEIGHT / corrupted-legacy-OBJ height — with no
	# counterpart in the original engine, which never scales actors)

	static func node_for(spec: PropSpec) -> Node3D:
		# LIGHTBEAM needs its animated additive build (the OBJ route renders the
		# pure-black base texture as an opaque black cross).
		if spec.name == "LIGHTBEAM":
			var lb := light_beam(PropDims.dims(spec.name), 120.0)
			lb.name = "prop-LIGHTBEAM"
			return lb
		# MOUNDBEAM (Old Gods + Ziggurat) is the other light-beam actor; it must
		# not fall through to the lit-opaque skinned path below.
		if spec.name == "MOUNDBEAM":
			var mb := mound_beam(PropDims.dims(spec.name), 120.0)
			mb.name = "prop-MOUNDBEAM"
			return mb
		# Prefer the SOLVED-pipeline export (correct UVs/materials — the 2026-07-09
		# audit showed the old heuristic OBJ route corrupted many props), then the
		# OBJ model, then the procedural stand-in.
		var actor := SkinnedModel.load_actor(spec.name)
		if actor != null:
			# NATIVE scale. The original never scales prop actors: Prop.java:388
			# uses the 1-arg Media_Object_Actor ctor, whose finalscale stays -1
			# so onLoadComplete never calls setAbsoluteScale
			# (Media_Object_Actor.java:20-32); the scaled 4-arg ctor is dead
			# code game-wide. The old model.json "scale" (prop.dat HEIGHT /
			# legacy-OBJ height) was a fabricated heuristic — it shrank SHIP
			# 0.32x and blew LIGHTHOUSE up 5.69x.
			var actor_root := Node3D.new()
			actor_root.add_child(actor.root)
			actor_root.name = "prop-%s" % spec.name
			return actor_root
		var real := ModelLibrary.node_for(spec.name)
		if real != null:
			real.name = "prop-%s" % spec.name
			return real
		var n: Node3D
		# Prefer the real bbox dims; fall back to prop.dat radius/height.
		var d = PropDims.dims(spec.name)
		match spec.name:
			"PALM": n = palm(d)
			"FERNTREE": n = fern(d)
			"TNT": n = tnt_barrel(d, spec.height, spec.radius)
			"TIKKI1", "TIKKI2", "TIKKI3": n = tiki(d, spec.height, spec.name)
			"TORCH": n = torch(d, spec.height)
			"TORCHBEARER": n = torch_bearer(d, spec.height)
			"HUT": n = hut(d, spec.radius, spec.height)
			"OBELISK": n = obelisk(d, spec.height, spec.radius)
			"MAST": n = mast(d, 60.0)
			"BRIDGE": n = bridge(d)
			"LIGHTHOUSE": n = lighthouse(d, spec.height)
			"FIREHEAD": n = firehead(d, spec.radius, spec.height)
			"MOUND": n = mound(d, 14.0, 30.0)
			"LIGHTBEAM": n = light_beam(d, 120.0)
			"MOUNDBEAM": n = procedural_beam(d, 120.0)
			"SHIP": n = ship_hull(d)
			_: n = generic_prop(spec.radius, spec.height)
		n.name = "prop-%s" % spec.name
		return n

	## World height to render a prop at, from the game's prop.dat HEIGHT.
	## The real bbox provides proportions; this provides scale. Returns (w, h, d).
	static func _aspect(dims, height: float) -> Vector3:
		if dims == null or dims.y <= 0:
			return Vector3(height * 0.6, height, height * 0.6)
		var s: float = height / dims.y
		return Vector3(dims.x * s, height, dims.z * s)

	static func _cylinder(radius_top: float, radius_bottom: float, height: float,
			mat: StandardMaterial3D, radial_segments := 0) -> MeshInstance3D:
		var g := CylinderMesh.new()
		g.top_radius = radius_top
		g.bottom_radius = radius_bottom
		g.height = height
		if radial_segments > 0:
			g.radial_segments = radial_segments
		g.material = mat
		var n := MeshInstance3D.new()
		n.mesh = g
		return n

	static func _box(w: float, h: float, d: float, mat: StandardMaterial3D) -> MeshInstance3D:
		# SCNBox chamferRadius has no BoxMesh equivalent — stand-ins only, dropped
		var g := BoxMesh.new()
		g.size = Vector3(w, h, d)
		g.material = mat
		var n := MeshInstance3D.new()
		n.mesh = g
		return n

	# Trees — crossed keyed billboards using the decoded foliage cutouts.

	static func palm(dims) -> Node3D:
		# Real PALM2 is ~112 tall; render at world height 45 (prop.dat).
		var a := _aspect(dims, 45.0)
		var root := Node3D.new()
		# Trunk: a slim tapered cylinder wrapped with the bark texture.
		var trunk_h := a.y * 0.7
		var trunk_r := maxf(a.x * 0.03, 1.4)
		# repeat the bark vertically so it reads as bark, not one stretched cell.
		var bark := tex_material("PALM2/trunk.png", [], false, Color(0.55, 0.42, 0.15))
		bark.texture_repeat = true                       # wrapT = .repeat
		bark.uv1_scale = Vector3(1, 4, 1)
		var trunk := _cylinder(trunk_r, trunk_r, trunk_h, bark)
		trunk.position.y = trunk_h / 2
		trunk.rotation.z = 0.05
		root.add_child(trunk)
		# Crown: drooping frond billboards fanning out from the top.
		var frond_l := a.x * 0.55         # frond length
		var frond_h := frond_l * 0.5      # frond crop is ~2:1 wide
		for i in 6:
			var fr := _quad(frond_l, frond_h, tex_material("PALM2/frond.png", [], true))
			# pivot at inner edge so fronds radiate outward from the crown
			# (SCN pivot → offset the quad under a rotated holder)
			fr.position.x = frond_l / 2
			var holder := Node3D.new()
			holder.position = Vector3(0, trunk_h, 0)
			holder.rotation = Vector3(-0.45, float(i) * (TAU / 6), 0)
			holder.add_child(fr)
			root.add_child(holder)
		return root

	static func fern(dims) -> Node3D:
		# FERNTREE bbox is squat/wide; render short and bushy.
		var a := _aspect(dims, 14.0)
		var root := Node3D.new()
		var w := maxf(a.x, 12.0)
		root.add_child(crossed_billboard("FERNTREE/frond.png", w, a.y * 1.4, 3))
		return root

	# Barrels, tikis, torches

	static func tnt_barrel(dims, height: float, _radius: float) -> Node3D:
		var a := _aspect(dims, height)
		var root := Node3D.new()
		var r := a.x / 2
		# barrelo.jpg: left half = side (TNT), right half = top. Godot primitives
		# are single-surface — the side material wraps the caps too (SceneKit
		# listed [side, top, top]).
		var side := tex_material("TNT/barrelo.jpg", [0.0, 0.0, 0.5, 1.0])
		var node := _cylinder(r, r, a.y, side)
		node.position.y = a.y / 2
		root.add_child(node)
		Props.style_cannon_materials(root)   # unlit + opaque — no ambient-wash ghosting
		return root

	static func tiki(dims, height: float, variant: String) -> Node3D:
		var a := _aspect(dims, height)
		var root := Node3D.new()
		var tex := "TIKKI/%so.jpg" % variant.to_lower()   # TIKKI1 -> tikki1o.jpg
		# tikki1o/tikki2o/tikki3o — carving faces the front; wrap on all sides.
		var body := _box(a.x, a.y, a.z, tex_material(tex))
		body.position.y = a.y / 2
		root.add_child(body)
		return root

	static func torch(dims, height: float) -> Node3D:
		var a := _aspect(dims, height)
		var root := Node3D.new()
		var h := a.y
		var r := maxf(a.x / 2, 0.4)
		var pole := _cylinder(r, r, h, tex_material("TORCH/torch.png"))
		pole.position.y = h / 2
		root.add_child(pole)
		root.add_child(flame(Vector3(0, h + 0.6, 0), 1.2))
		return root

	static func torch_bearer(dims, height: float) -> Node3D:
		var a := _aspect(dims, height)
		var root := Node3D.new()
		var body := _box(a.x, a.y, a.z, tex_material("TORCHBEARER/torch_bearer.jpg"))
		body.position.y = a.y / 2
		root.add_child(body)
		root.add_child(flame(Vector3(0, a.y + 1, 0), 1.1))
		return root

	static func flame(pos: Vector3, size: float) -> Node3D:
		var f := Animated.new()
		var sphere := SphereMesh.new()
		sphere.radius = size
		sphere.height = size * 2
		sphere.material = material(Color.ORANGE, Color.ORANGE)
		var mi := MeshInstance3D.new()
		mi.mesh = sphere
		f.add_child(mi)
		f.position = pos
		var light := OmniLight3D.new()
		light.light_color = Color.ORANGE
		light.light_energy = 0.25          # SCN intensity 250 lumens ÷ 1000 (PORTING.md)
		light.omni_range = 40.0            # attenuationEndDistance = 40
		f.add_child(light)
		# CABasicAnimation "scale": (1,1,1)→(1.25,1.5,1.25), 0.35 s, autoreverse ∞
		var s := {"t": 0.0}
		f.tick = func(n: Node3D, dt: float) -> void:
			s.t = fmod(s.t + dt, 0.7)
			var k: float = s.t / 0.35
			if k > 1.0:
				k = 2.0 - k
			n.scale = Vector3.ONE.lerp(Vector3(1.25, 1.5, 1.25), k)
		return f

	## A firefly: the FIREFLY sprite as a small glowing billboard (night maps
	## place these via <FIREFLY> entries). The macOS build used a simplified
	## fixed 6-unit orbit; the original wanders, blinks and size-pulses —
	## ported from Particle_Object_Firefly.java verbatim (Java outranks,
	## PORTING.md rule 4).
	static func firefly() -> Node3D:
		var quad := QuadMesh.new()
		quad.size = Vector2(1, 1)          # unit quad; setBitmapSize drives node scale
		var m := StandardMaterial3D.new()
		var tex := PropTex.image("firefly.png")
		if tex != null:
			m.albedo_texture = tex
		else:
			m.albedo_color = Color.YELLOW
		m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED   # lightingModel = .constant
		m.blend_mode = BaseMaterial3D.BLEND_MODE_ADD            # glow
		m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
		m.depth_draw_mode = BaseMaterial3D.DEPTH_DRAW_DISABLED  # writesToDepthBuffer = false
		m.billboard_mode = BaseMaterial3D.BILLBOARD_ENABLED     # SCNBillboardConstraint
		m.billboard_keep_scale = true
		quad.material = m
		var n := Animated.new()
		var mi := MeshInstance3D.new()
		mi.mesh = quad
		mi.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
		n.add_child(mi)
		# Particle_Object_Firefly.java ctor state: size 2-3, one of six SinTable
		# pulse phases, ±10-unit anchor jitter, a 10-30 unit orbit arm, 0-5 s
		# until the first blink. The anchor jitter is applied lazily on the
		# first tick (World positions the node after this builder returns).
		var s := {
			"init": false,
			"anchor": Vector3.ZERO,
			"arm": Vector3.ZERO,
			"scale": 2.0 + randf(),
			"sin_val": int(randf() * 6.0),
			"angle": 0.0,
			"angle1": 0.0,
			"left": randf() < 0.5,
			"up": randf() < 0.5,
			"hid_timer": randf() * 5.0,
			"t": 0.0,
		}
		n.tick = func(node: Node3D, dt: float) -> void:
			if not s.init:
				s.init = true
				s.anchor = node.position + Vector3((randf() - 0.5) * 20.0,
						(randf() - 0.5) * 20.0, (randf() - 0.5) * 20.0)
				s.arm = Vector3(randf() - 0.5, randf() - 0.5, randf() - 0.5) \
						.normalized() * (10.0 + randf() * 20.0)
			s.t += dt
			# wandering turn rates, clamped to ±1°; 10% direction flips and the
			# per-frame arm rotation follow the original frame cadence
			# (Particle_Object_Firefly.java updateTimeSlice)
			s.angle = clampf(s.angle + (1.0 if s.left else -1.0) * dt, -1.0, 1.0)
			s.angle1 = clampf(s.angle1 + (1.0 if s.up else -1.0) * dt, -1.0, 1.0)
			if randf() < 0.1:
				s.left = not s.left
			if randf() < 0.1:
				s.up = not s.up
			var arm: Vector3 = s.arm
			arm = arm.rotated(Vector3.UP, deg_to_rad(s.angle))
			arm = arm.rotated(Vector3.RIGHT, deg_to_rad(s.angle1))
			s.arm = arm
			# blink: HidTimer counts down; dark 1-3 s, lit 1-7 s
			s.hid_timer -= dt
			if node.visible and s.hid_timer <= 0.0:
				node.visible = false
				s.hid_timer = 1.0 + randf() * 2.0
			elif not node.visible and s.hid_timer <= 0.0:
				node.visible = true
				s.hid_timer = 1.0 + randf() * 6.0
			if node.visible:
				node.position = s.anchor + Vector3(arm.x, arm.y * 0.2, arm.z)
				# setBitmapSize(Scale + SinTable[SinVal]); SinTable rates
				# transcribed from Main.java:307-324
				var rate: float = [2.0, -2.75, 3.5, 1.0, 1.5, -2.5][s.sin_val]
				var size: float = s.scale + sin(rate * s.t)
				node.scale = Vector3(size, size, size)
		return n

	# Buildings / stone

	static func hut(dims, _radius: float, height: float) -> Node3D:
		var a := _aspect(dims, height)
		var root := Node3D.new()
		var wall_h := a.y * 0.55
		# hut.jpg atlas: right-side panels = wall thatch; octagon = roof.
		var wall := _cylinder(a.x / 2, a.x / 2, wall_h,
				tex_material("HUT/hut.jpg", [0.55, 0.0, 0.45, 0.5]), 8)
		wall.position.y = wall_h / 2
		root.add_child(wall)
		var roof := _cylinder(0.0, a.x * 0.62, a.y * 0.5,
				tex_material("HUT/hut.jpg", [0.0, 0.0, 0.5, 0.5]), 8)  # octagon roof region
		roof.position.y = wall_h + a.y * 0.25
		root.add_child(roof)
		return root

	static func obelisk(dims, height: float, _radius: float) -> Node3D:
		var a := _aspect(dims, height)
		var root := Node3D.new()
		# obelisk.jpg is a tall carved slab; use a slightly tapered box.
		var shaft := _box(a.x, a.y, a.z, tex_material("OBELISK/obelisk.jpg"))
		shaft.position.y = a.y / 2
		root.add_child(shaft)
		# small capstone pyramid (SCNPyramid → 4-segment cone; pyramid pivot is
		# at its base, the cone's at its center)
		var cap_h := a.y * 0.14
		var cap := _cylinder(0.0, a.x * 0.7071, cap_h,
				tex_material("OBELISK/obelisk.jpg", [0.0, 0.85, 1.0, 0.15]), 4)
		cap.position.y = a.y + cap_h / 2
		root.add_child(cap)
		return root

	static func mast(_dims, height: float) -> Node3D:
		var root := Node3D.new()
		var h := height
		var pole := _cylinder(1.0, 1.0, h,
				tex_material("SHIP/boato.jpg", [0.86, 0.1, 0.08, 0.7], false,
						Color(0.42, 0.3, 0.16)))
		pole.position.y = h / 2
		root.add_child(pole)
		var sail_mat := tex_material("SHIP/boato.jpg", [0.68, 0.35, 0.16, 0.35])
		sail_mat.cull_mode = BaseMaterial3D.CULL_DISABLED   # isDoubleSided
		var sail := _quad(22.0, h * 0.5, sail_mat)
		sail.position = Vector3(0, h * 0.6, 2)
		root.add_child(sail)
		return root

	static func bridge(_dims) -> Node3D:
		var root := Node3D.new()
		var deck := _box(16.0, 2.5, 44.0,
				tex_material("BRIDGE/bridge.jpg", [0.0, 0.55, 1.0, 0.45]))  # stone brick region
		deck.position.y = 20
		root.add_child(deck)
		for dz in [-16.0, 16.0]:
			var pillar := _cylinder(3.2, 3.2, 20.0,
					tex_material("BRIDGE/bridge.jpg", [0.0, 0.0, 1.0, 0.5]))
			pillar.position = Vector3(0, 10, dz)
			root.add_child(pillar)
		return root

	static func lighthouse(_dims, height: float) -> Node3D:
		var root := Node3D.new()
		var h := height
		var tower := _cylinder(3.0, 5.5, h * 0.85, tex_material("LIGHTHOUSE/lighthouseo.jpg"))
		tower.position.y = h * 0.425
		root.add_child(tower)
		var lamp := _cylinder(2.6, 2.6, h * 0.12, material(Color.YELLOW, Color.YELLOW))
		lamp.position.y = h * 0.91
		root.add_child(lamp)
		var cap := _cylinder(0.0, 3.2, h * 0.1,
				tex_material("LIGHTHOUSE/lighthouseo.jpg", [0.0, 0.85, 1.0, 0.15]))
		cap.position.y = h
		root.add_child(cap)
		return root

	static func firehead(dims, _radius: float, height: float) -> Node3D:
		var a := _aspect(dims, height)
		var root := Node3D.new()
		var h := a.y
		# firehovelo.jpg: the face (eyes + open mouth) is the lower-right region.
		# BoxMesh is single-surface — the face crop wraps all sides (SceneKit
		# used [face, side, side, side, side, side]).
		var head := _box(a.x, h, a.z,
				tex_material("FIREHEAD/firehovelo.jpg", [0.35, 0.0, 0.65, 0.6]))
		head.position.y = h / 2
		root.add_child(head)
		root.add_child(flame(Vector3(0, h * 0.25, a.z * 0.5), 2.5))
		return root

	static func mound(dims, _radius: float, _height: float) -> Node3D:
		# MOUND bbox is large (~152x111x201). Render as a broad textured dome.
		var a := _aspect(dims, 40.0)
		var root := Node3D.new()
		var cone := _cylinder(a.x * 0.15, a.x / 2, a.y,
				tex_material("MOUND/moundo.jpg", [], false, Color(0.45, 0.45, 0.45)))
		cone.position.y = a.y / 2
		root.add_child(cone)
		return root

	## Additive light-beam material pass, shared by LIGHTBEAM and MOUNDBEAM.
	## The render style comes from the actors' own material records (the gMat
	## chunk inside each .wsad): LIGHTBEAM's material carries style 2, and its
	## correct visual outcome is beyond doubt (a lighthouse beam is light, not
	## structure) — solving style 2 = additive. beam.wsad (MOUNDBEAM) carries
	## the same style 2 plus emissive 255,255,255, where every opaque structure
	## actor checked (mound.wsad, PALM2, TORCH) carries style 0 and emissive 0.
	## (SCN also mirrored diffuse into emission under .constant; Godot unshaded
	## additive already renders the texture full-bright, so that doubling is
	## not reproduced.)
	static func _apply_beam_light_materials(node: Node) -> void:
		if node is MeshInstance3D and node.mesh != null:
			for i in node.mesh.get_surface_count():
				var m = node.mesh.surface_get_material(i)
				if m is StandardMaterial3D:
					m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
					m.blend_mode = BaseMaterial3D.BLEND_MODE_ADD  # beam of light: additive
					m.cull_mode = BaseMaterial3D.CULL_DISABLED    # isDoubleSided
					m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
					m.depth_draw_mode = BaseMaterial3D.DEPTH_DRAW_DISABLED
					m.albedo_color.a = 0.55                       # transparency = 0.55
		for c in node.get_children():
			_apply_beam_light_materials(c)

	static func light_beam(dims, height: float) -> Node3D:
		# The real LIGHTBEAM actor: a 194-unit textured beam plane playing its
		# original decoded "loop" motion — a continuous 13.3s sweep around the
		# vertical (from resources/loop.wsmo, 400 uniform quat keys).
		var actor := SkinnedModel.load_actor("LIGHTBEAM")
		var motion := SkinnedModel.load_motion("MODELS/LIGHTBEAM/loop_motion_full.json")
		if actor != null and motion != null:
			_apply_beam_light_materials(actor.root)
			# native geom scale — the original never scales actors (Prop.java:388)
			SkinnedModel.animate(actor, motion, randf_range(0.0, motion.duration))
			# the owner drives Actor.update per frame (PORTING.md)
			var root := Animated.new()
			root.tick = func(_n: Node3D, dt: float) -> void:
				actor.update(dt)
			root.add_child(actor.root)
			return root
		return procedural_beam(dims, height)

	static func mound_beam(dims, height: float) -> Node3D:
		# The real MOUNDBEAM actor (MEDIA/OBJECTS/MOUND/beam.wsad, per
		# PROPS/MOUNDBEAM/prop.dat): a static beam mesh UV-mapped to the green
		# glow strip of the mound atlas (moundo.jpg). Unlike LIGHTBEAM it ships
		# no motion resource, so it does not animate.
		var actor := SkinnedModel.load_actor("MOUNDBEAM")
		if actor != null:
			_apply_beam_light_materials(actor.root)
			var root := Node3D.new()
			root.add_child(actor.root)
			return root
		return procedural_beam(dims, height)

	static func procedural_beam(_dims, height: float) -> Node3D:
		var m := material(Color(0.7, 0.9, 1), Color(0.6, 0.85, 1))
		var tex := PropTex.image("LIGHTBEAM/lightbeamo.jpg")
		if tex != null:
			m.albedo_texture = tex
			m.emission_enabled = true
			m.emission_texture = tex
		m.albedo_color.a = 0.35                                   # transparency = 0.35
		m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
		m.depth_draw_mode = BaseMaterial3D.DEPTH_DRAW_DISABLED    # writesToDepthBuffer = false
		var beam := _cylinder(2.2, 2.2, height, m)
		beam.position.y = height / 2
		return beam

	static func ship_hull(_dims) -> Node3D:
		var root := Node3D.new()
		# boato.jpg is a full ship atlas; map the hull-side region to the hull box.
		var hull := _box(26.0, 14.0, 70.0,
				tex_material("SHIP/boato.jpg", [0.0, 0.0, 0.45, 0.4]))
		hull.position.y = 7
		root.add_child(hull)
		var deck_rail := _box(28.0, 2.0, 72.0,
				tex_material("SHIP/boato.jpg", [0.4, 0.85, 0.3, 0.12]))
		deck_rail.position.y = 14
		root.add_child(deck_rail)
		return root

	static func generic_prop(radius: float, height: float) -> Node3D:
		var n := _cylinder(radius * 0.7, radius * 0.7, height,
				material(Color(0.5, 0.5, 0.5)))
		n.position.y = height / 2
		var root := Node3D.new()
		root.add_child(n)
		return root

	# Decorations
	# (PropGeometry.cloud is NOT ported: dead code — the live <CLOUD>
	# implementation is FXSprites.cloud, which World already calls.)

	## <DECORATION>/<DECORATIONWATER> actors. Particle_Object_Decoration.java:24:
	## a path ending ".wsad" loads that actor file, a bare directory loads its
	## actor.wsad. The originals: OBJECTS/BRUSH2 = the purple fern bush,
	## OBJECTS/PALM2/brush1.wsad = the GREEN bush (palm-atlas leaves),
	## OBJECTS/TAILS/tail1-3.wsad = cattails. All decode through the solved
	## pipeline; no tinting anywhere in the Java — color is all texture.
	static func decoration(path: String, scale: Vector2) -> Node3D:
		var root := Animated.new()
		var stem := path.get_file().get_basename().to_upper()
		var model: String
		match stem:
			"BRUSH1": model = "BRUSH1"
			"TAIL1": model = "TAILS"
			"TAIL2": model = "TAILS2"
			"TAIL3": model = "TAILS3"
			_: model = stem                       # bare dir, e.g. BRUSH2
		var actor := SkinnedModel.load_actor(model)
		if actor != null:
			root.add_child(actor.root)
			# Particle_Object_Decoration.java:41-44: a decoration whose actor has
			# a motion plays "loop" at a random 0-8 s phase (cattail sway); the
			# Swift port left decorations static — Java outranks, PORTING.md
			# rule 4. No decoration loop motion is exported yet, so this arms
			# only once MODELS/<model>/loop_motion_full.json appears.
			var motion := SkinnedModel.load_motion("MODELS/%s/loop_motion_full.json" % model)
			if motion != null:
				SkinnedModel.animate(actor, motion, randf() * 8.0)
				root.tick = func(_n: Node3D, dt: float) -> void:
					actor.update(dt)
		elif path.to_lower().contains("tail"):
			# fallback stand-ins if an export is missing
			root.add_child(crossed_billboard("TAILS/cattails.png", 16.0, 20.0, 2))
		else:
			root.add_child(crossed_billboard("BRUSH2/brush.png", 18.0, 16.0, 3))
		# Particle_Object_Decoration.java:26: setAbsoluteScale(sx, sy, sx) —
		# first file float scales X and Z, second scales Y
		root.scale = Vector3(scale.x, scale.y, scale.x)
		return root

	# Collision (the hitTestWithSegment replacement, PORTING.md)

	## One StaticBody3D per collidable prop: a ConcavePolygonShape3D of every
	## mesh triangle under the node (relative transforms applied), with
	## backface_collision matching the Swift's backFaceCulling: false. Only
	## wtCollideable / standable specs get bodies; `layer` mirrors the original
	## masks — 2 (Prop.java:534-536), standable 10 = 2|8 (Prop.java:538-540).
	## `prop` rides along as metadata so ray hits map back to the Prop.
	static func add_collision_body(root: Node3D, layer: int, prop) -> void:
		var faces := PackedVector3Array()
		for c in root.get_children():
			_collect_faces(c, Transform3D.IDENTITY, faces)
		if faces.is_empty():
			return
		var shape := ConcavePolygonShape3D.new()
		shape.backface_collision = true
		shape.set_faces(faces)
		var body := StaticBody3D.new()
		body.collision_layer = layer
		body.collision_mask = 0
		body.set_meta("prop", prop)
		var cs := CollisionShape3D.new()
		cs.shape = shape
		body.add_child(cs)
		root.add_child(body)

	static func _collect_faces(node: Node, xf: Transform3D, out: PackedVector3Array) -> void:
		var local_xf := xf
		if node is Node3D:
			local_xf = xf * (node as Node3D).transform
		if node is MeshInstance3D and (node as MeshInstance3D).mesh != null:
			# skinned meshes contribute their bind-pose triangles (the rest pose
			# equals the bind pose — skinned_model.gd)
			for v in (node as MeshInstance3D).mesh.get_faces():
				out.append(local_xf * v)
		for c in node.get_children():
			_collect_faces(c, local_xf, out)


# MARK: - Runtime prop

class Prop:
	var spec: PropSpec
	var position: Vector3
	var node: Node3D
	var alive := true
	var age_at_spawn: float       # game time when created (3 s grace vs collisions)
	# Explosive shockwave state
	var detonating := false
	var shock_scale := 0.0
	var detonator = null          # Cannon — untyped, cannon.gd is a sibling port

	func _init(p_spec: PropSpec, p_position: Vector3, rotation_deg: float,
			game_time: float) -> void:
		spec = p_spec
		position = p_position
		age_at_spawn = game_time
		node = PropGeometry.node_for(spec)
		node.position = position
		# Prop.java:541 applies setOrientation(0,1,0, -Angle); the clone keeps
		# objects.dat z in file-row space (no Height - z flip — see the
		# <FIREFLY> note in world.gd), and the z-mirror flips yaw handedness,
		# cancelling the sign. +rotationDeg carried from the visually verified
		# macOS build.
		node.rotation.y = deg_to_rad(rotation_deg)
		if spec.wt_collideable or spec.standable:
			var layer := 2                # Prop.java:534-536 setCollisionMask(2)
			if spec.standable:
				layer |= 8                # Prop.java:538-540 setCollisionMask(10)
			PropGeometry.add_collision_body(node, layer, self)

	func check_collision(x: float, y: float, z: float, pad := 2.0) -> bool:
		# Prop.java:303-309 — the proximity cylinder only exists for destructible
		# props; non-destructible ones collide via their mesh (mask 2) instead.
		if not (alive and spec.destructible):
			return false
		var dx := x - position.x
		var dz := z - position.z
		var r := spec.radius + pad
		if dx * dx + dz * dz > r * r:
			return false
		# Prop.java:308-309: horizontal distance ≤ Radius+pad AND local height
		# ≤ Height+pad — the cylinder has NO lower bound (the Swift added
		# y >= position.y - 2; Java outranks, PORTING.md rule 4).
		return y - position.y <= spec.height + pad
