class_name FXSprites
# Port of macos/Sources/Cannonballs/FXSprites.swift.
# The ORIGINAL effect sprites (MEDIA/IMAGES sheets, decoded from the WLD3
# container) played back with the original per-frame physics transcribed
# from the decompiled Particle_Object_*.java classes.
#
# - EXPLOSION1: 4x4 sheet, 16 frames @ 20 fps, additive billboard
# - SMOKEPUFF:  4x2 sheet of 128px puffs; Smoke(type 0) = cols 2-3,
#   SmokeBlack(1) = cols 0-1; grows with decaying accel, rises, damped drift
# - COIN:       4x4 sheet @ 20 fps looping spin, gravity -32, bounce /1.25,
#   ~5s life then a smoke puff; water: splash burst + rings
# - SPLASH:     droplet emitter (gravity -32) shedding SPLASH sprites,
#   SplashRing on the water when it lands
#
# GDScript port notes (windows/PORTING.md):
# - Effects are SELF-DRIVING nodes: the macOS build drove them with
#   SCNAction.customAction closures; here each is a Driven whose _process
#   advances the same transcribed math (the CABasicAnimation rule).
# - GDScript requires default arguments to be trailing, so the Swift's
#   `in world: World` parameter moves BEFORE optional flags in some
#   signatures. `world` stays an untyped (duck-typed) param.
# - Where the decompiled Java disagrees with the Swift, the Java wins
#   (PORTING.md rule 4); every such spot is marked "Java outranks" inline.


static func image(name: String) -> Texture2D:
	# (the Swift kept its own NSImage cache; Assets.texture already caches)
	return Assets.texture("IMAGES/FX/%s.png" % name)


## Per-frame stepped update (mirrors updateTimeSlice); step returns false to
## remove. The Swift's drive() ran a 30 s SCNAction.customAction and then
## removed the node; this node is the Godot equivalent, driving itself.
class Driven extends MeshInstance3D:
	var step: Callable              # func(n: Driven, dt: float) -> bool
	# SCNBillboardConstraint + a screen-plane roll (setBitmapOrientation):
	# Godot's material billboard discards node rotation, so rolled sprites
	# re-orient here manually per frame (PORTING.md's manual-billboard clause).
	var manual_billboard := false
	var roll := 0.0
	var _elapsed := 0.0

	func _process(dt: float) -> void:
		if manual_billboard:
			var cam := get_viewport().get_camera_3d()
			if cam != null:
				global_basis = cam.global_transform.basis * Basis(Vector3(0, 0, 1), roll)
		if not step.is_valid():
			return                              # static billboard (cloud puffs)
		_elapsed += dt
		if _elapsed > 30.0:                     # SCNAction.customAction(duration: 30) cap
			queue_free()
			return
		if dt > 0.0 and not step.call(self, minf(dt, 0.1)):
			queue_free()

	## SCNNode.opacity equivalent: GeometryInstance3D.transparency is not
	## supported by the gl_compatibility renderer, so fade the (per-effect,
	## never shared) material's albedo alpha instead. Under BLEND_MODE_ADD the
	## alpha scales the added intensity, matching SceneKit .add under opacity.
	func set_opacity(a: float) -> void:
		var m := material_override as StandardMaterial3D
		if m != null:
			m.albedo_color.a = a


## A camera-facing sprite plane showing one cell of a sheet.
static func _billboard(sheet: String, cols: int, rows: int, size: float, additive: bool) -> Driven:
	var quad := QuadMesh.new()
	quad.size = Vector2(size, size)                           # SCNPlane(width:height:)
	var m := StandardMaterial3D.new()
	m.albedo_texture = image(sheet)
	m.texture_repeat = false                                  # wrapS/wrapT = .clamp
	m.uv1_scale = Vector3(1.0 / cols, 1.0 / rows, 1)          # contentsTransform scale
	m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED     # lightingModel = .constant
	m.cull_mode = BaseMaterial3D.CULL_DISABLED                # isDoubleSided = true
	m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	m.depth_draw_mode = BaseMaterial3D.DEPTH_DRAW_DISABLED    # writesToDepthBuffer = false
	if additive:
		m.blend_mode = BaseMaterial3D.BLEND_MODE_ADD          # blendMode = .add
	# (else: transparencyMode = .dualLayer -> plain alpha blending)
	m.billboard_mode = BaseMaterial3D.BILLBOARD_ENABLED       # SCNBillboardConstraint
	m.billboard_keep_scale = true                             # effects animate node scale
	var node := Driven.new()
	node.mesh = quad
	node.material_override = m
	node.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF   # castsShadow = false
	return node


static func _set_frame(node: MeshInstance3D, col: int, row: int, cols: int, rows: int, mirrored := false) -> void:
	var m := node.material_override as StandardMaterial3D
	if m == null:
		return
	var sx := 1.0 / cols
	var sy := 1.0 / rows
	# SceneKit contentsTransform (scale, then translate) == uv1_scale/uv1_offset:
	# both apply uv * scale + offset with v = 0 at the image TOP (convention
	# probe: EXPLOSION1 is authored frame 0 top-left and plays correctly with
	# row = f/4 counted from the top).
	m.uv1_scale = Vector3(-sx if mirrored else sx, sy, 1)
	m.uv1_offset = Vector3((col + (1 if mirrored else 0)) * sx, row * sy, 0)


## Vector helper for the Swift's SIMD3.random(in:) / random component triples.
static func _rand_v3(lo: float, hi: float) -> Vector3:
	return Vector3(randf_range(lo, hi), randf_range(lo, hi), randf_range(lo, hi))


# MARK: - Explosion1 (Particle_Object_Explosion1.java)

static func explosion1(pos: Vector3, trajectory: Vector3, size: float, world) -> void:
	var node := _billboard("EXPLOSION1", 4, 4, 1.0 + size, true)
	node.position = pos
	world.effects_root.add_child(node)
	var s := {"frame": 0.0, "last_frame": -1, "p": pos}
	node.step = func(n: Driven, dt: float) -> bool:
		s.frame += dt * 20
		if s.frame > 15.0:
			return false
		var p: Vector3 = s.p
		p += trajectory * dt
		s.p = p
		n.position = p
		var f := int(s.frame)
		if f != s.last_frame:
			s.last_frame = f
			@warning_ignore("integer_division")
			FXSprites._set_frame(n, f % 4, f / 4, 4, 4)
		return true


# MARK: - Smoke (Particle_Object_Smoke.java)

static func smoke(pos: Vector3, trajectory: Vector3, scale: float, world, black := false) -> void:
	var node := _billboard("SMOKEPUFF", 4, 2, 1, false)
	node.position = pos
	# random puff variant: 4 cells x mirror; the Java UV math wraps so
	# Smoke(0) lands on the white cols 0-1, SmokeBlack(1) on dark cols 2-3
	var v := randi_range(0, 7)
	var col := (v % 2) + (2 if black else 0)
	@warning_ignore("integer_division")
	_set_frame(node, col, (v / 2) % 2, 4, 2, v > 3)
	world.effects_root.add_child(node)
	var s := {
		"s": scale,
		"accel": randf_range(10.0, 20.0),
		"rise": randf_range(1.0, 4.0),
		"traj": trajectory,
		"p": pos,
	}
	node.step = func(n: Driven, dt: float) -> bool:
		s.s += dt * s.accel
		if s.accel > -8.0:
			s.accel -= dt * 20
		if s.s < 0.05:
			return false
		var damp := pow(0.7, dt)
		var traj: Vector3 = s.traj
		var p: Vector3 = s.p
		traj *= damp
		p += traj * dt
		p.y += s.rise * dt
		s.traj = traj
		s.p = p
		n.position = p
		n.scale = Vector3(s.s, s.s, s.s)
		return true


# MARK: - Coin (Particle_Object_Coin.java)

static func coin(pos: Vector3, velocity: Vector3, world) -> void:
	var node := _billboard("COIN", 4, 4, 2, false)
	node.position = pos
	# setBitmapOrientation(random * 360) — Particle_Object_Coin.java:88; the
	# material billboard discards roll, so this sprite billboards manually.
	node.roll = randf_range(0.0, TAU)
	node.manual_billboard = true
	(node.material_override as StandardMaterial3D).billboard_mode = BaseMaterial3D.BILLBOARD_DISABLED
	world.effects_root.add_child(node)
	var s := {"frame": 0.0, "last_frame": -1, "life": randf(), "p": pos, "v": velocity}
	node.step = func(n: Driven, dt: float) -> bool:
		s.frame += dt * 20
		while s.frame > 15.5:
			s.frame -= 15
		s.life += dt
		if s.life > 5.0:
			FXSprites.smoke(s.p, FXSprites._rand_v3(-0.5, 0.5), randf_range(0.3, 0.8), world)
			return false
		var p: Vector3 = s.p
		var v: Vector3 = s.v
		p += v * dt
		v.y -= 32 * dt
		var ground: float = world.terrain.height(p.x, p.z)
		if p.y <= ground:
			if ground < 1.0:                      # fell in the water
				if randf() < 0.25:
					# the original throws 9 splash jets WITH the rings
					# (Particle_Object_Coin.java:40-51; the Swift port dropped
					# the jets — Java outranks, PORTING.md rule 4)
					for i in 9:
						FXSprites.splash_jet(Vector3(p.x, 0, p.z),
								Vector3(randf_range(-7.5, 7.5), 20 + randf_range(-7.5, 7.5),
										randf_range(-5, 5)), world)
					FXSprites.splash_ring(Vector3(p.x, 0.11, p.z), 3, 2, world)
					FXSprites.splash_ring(Vector3(p.x, 0.111, p.z), 6, 1, world)
					FXSprites.splash_ring(Vector3(p.x, 0.1115, p.z), 4, 4, world)
				return false
			p.y = ground
			if v.y < 0:
				v.y = -v.y / 1.25                 # bounce
		s.p = p
		s.v = v
		n.position = p
		var f := int(s.frame)
		if f != s.last_frame:
			s.last_frame = f
			@warning_ignore("integer_division")
			FXSprites._set_frame(n, f / 4, f % 4, 4, 4)   # column-major spin
		return true


## Chest burst: 30 coins, the original spread (Chest.java:46).
static func coin_burst(pos: Vector3, world, count := 30) -> void:
	for i in count:
		coin(pos + Vector3(randf_range(-0.5, 0.5), 2, randf_range(-0.5, 0.5)),
				Vector3(randf_range(-10, 10), 30 + randf_range(-5, 5), randf_range(-10, 10)),
				world)


# MARK: - Splash (Particle_Object_Splash + SplashRing .java)

## Invisible ballistic emitter that sheds droplet sprites and rings the water.
static func splash_jet(pos: Vector3, velocity: Vector3, world) -> void:
	var node := Driven.new()
	node.position = pos
	world.effects_root.add_child(node)
	var s := {"p": pos, "v": velocity}
	node.step = func(_n: Driven, dt: float) -> bool:
		if randf() < 0.5:
			FXSprites._droplet(s.p,
					Vector3(randf_range(-1.5, 1.5), randf_range(-0.5, 0), randf_range(-1.5, 1.5)),
					randf_range(0.1, 1.1), world)
		var p: Vector3 = s.p
		var v: Vector3 = s.v
		p += v * dt
		v.y -= 32 * dt
		s.p = p
		s.v = v
		if p.y <= 1.0 and v.y < 0:
			FXSprites.splash_ring(Vector3(p.x, 0.4, p.z), 3, 2, world)
			return false
		return true


static func _droplet(pos: Vector3, velocity: Vector3, size: float, world) -> void:
	# the original picks SPLASH or SPLASH2 art per droplet, 50/50
	# (Particle_Object_SplashDrop.java:16-22; the Swift port always used
	# SPLASH — Java outranks, PORTING.md rule 4)
	var node := _billboard("SPLASH" if randf() <= 0.5 else "SPLASH2", 1, 1, 0.4 + size, true)
	node.position = pos
	world.effects_root.add_child(node)
	var s := {"p": pos, "v": velocity, "life": 0.0}
	node.step = func(n: Driven, dt: float) -> bool:
		s.life += dt
		var p: Vector3 = s.p
		if s.life > 1.2 or p.y < 0:
			return false
		var v: Vector3 = s.v
		p += v * dt
		v.y -= 32 * dt
		s.p = p
		s.v = v
		n.position = p
		return true


# MARK: - Debris chunk (Chunk_Object.java: tumbling model piece)

static func debris_chunk(model: String, pos: Vector3, velocity: Vector3, scale: float, world) -> void:
	var actor := SkinnedModel.load_actor(model)   # guard let actor = SkinnedModel.load(model)
	if actor == null:
		return
	# Cannon.styleCannonMaterials(actor.root) — cannon.gd is not yet ported;
	# duck-load it so this file parses standalone (PORTING.md typing rule).
	# Assumed API: static style_cannon_materials(node: Node3D).
	if ResourceLoader.exists("res://src/cannon.gd"):
		load("res://src/cannon.gd").style_cannon_materials(actor.root)
	var node := Driven.new()
	actor.root.scale = Vector3(scale, scale, scale)
	node.add_child(actor.root)
	node.position = pos
	world.effects_root.add_child(node)
	var spin_axis: Vector3 = _rand_v3(-0.5, 0.5).normalized()
	var spin_rate := randf_range(-25.0, 25.0) * PI / 180
	# SCNAction.repeatForever(.rotate(by: spinRate * 4, around: spinAxis,
	# duration: 1)) -> constant tumble of spinRate * 4 rad/s, advanced below
	var s := {"p": pos, "v": velocity, "opacity": 255.0}
	node.step = func(n: Driven, dt: float) -> bool:
		n.rotate(spin_axis, spin_rate * 4 * dt)
		var p: Vector3 = s.p
		var v: Vector3 = s.v
		p += v * dt
		v.y -= 32 * dt
		if v.y < 0:
			s.opacity -= dt * 100
			if s.opacity < 0:
				return false
			FXSprites._set_tree_opacity(n, s.opacity / 255.0)
		n.position = p
		var ground: float = world.terrain.height(p.x, p.z)
		if p.y <= ground:
			if ground < 1.0:
				for i in 7:
					FXSprites.splash_jet(Vector3(p.x, 0, p.z),
							Vector3(randf_range(-7.5, 7.5), 20 + randf_range(-7.5, 7.5),
									randf_range(-5, 5)), world)
				FXSprites.splash_ring(Vector3(p.x, 0.11, p.z), 3, 2, world)
				return false
			if randf() < 0.5 and Audio.shared != null:    # Bool.random()
				Audio.shared.play("puff", 0.6)
			FXSprites.smoke(p, FXSprites._rand_v3(-0.5, 0.5), randf_range(4.0, 6.0), world)
			return false
		s.p = p
		s.v = v
		return true


## SCNNode.opacity over a model subtree (debris chunks): gl_compatibility has
## no GeometryInstance3D.transparency, so fade each mesh surface's material
## alpha; materials are duplicated once per instance so the shared model
## library materials are never touched.
static func _set_tree_opacity(node: Node, a: float) -> void:
	if node is MeshInstance3D:
		var mi: MeshInstance3D = node
		if mi.material_override is StandardMaterial3D:
			mi.material_override = _faded(mi.material_override, a)
		elif mi.mesh != null:
			for i in mi.mesh.get_surface_count():
				var m := mi.get_surface_override_material(i)
				if m == null:
					m = mi.mesh.surface_get_material(i)
				if m is StandardMaterial3D:
					mi.set_surface_override_material(i, _faded(m, a))
	for c in node.get_children():
		_set_tree_opacity(c, a)


static func _faded(m: StandardMaterial3D, a: float) -> StandardMaterial3D:
	if not m.has_meta("fx_fade"):
		m = m.duplicate()
		m.set_meta("fx_fade", true)
		m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	m.albedo_color.a = a
	return m


# MARK: - Cloud (Particle_Object_Cloud: 30 SMOKEPUFF billboards in an ellipsoid)

static func cloud(pos: Vector3, radius: float, x_mul: float, y_mul: float, world) -> void:
	var root := Node3D.new()
	root.position = pos
	root.name = "cloud"
	for i in 30:
		var node := _billboard("SMOKEPUFF", 4, 2, randf_range(10.0, 30.0), false)
		var v := randi_range(0, 7)
		@warning_ignore("integer_division")
		_set_frame(node, v % 2, (v / 2) % 2, 4, 2, v > 3)
		var d: Vector3 = _rand_v3(-0.5, 0.5).normalized()
		d *= radius * randf_range(0.8, 1.0)
		d.x *= x_mul
		d.y *= y_mul
		node.position = d
		# firstMaterial.transparency = 0.9
		(node.material_override as StandardMaterial3D).albedo_color.a = 0.9
		root.add_child(node)
	world.root.add_child(root)      # Swift parents clouds to scene.rootNode, not effectsRoot


# MARK: - Blob shadow (the original SHADOW alpha patch under game objects)

## Main.java ShadowsEnabled (Options menu); Island.switchShadows equivalent.
## The macOS build persisted this in UserDefaults "opt.shadows"; persistence
## on Windows belongs to the options-menu port — default true until then.
static var shadows_enabled := true

static func blob_shadow(radius: float) -> MeshInstance3D:
	var quad := QuadMesh.new()
	quad.size = Vector2(radius * 2, radius * 2)
	var m := StandardMaterial3D.new()
	m.albedo_texture = image("SHADOW")
	m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED     # lightingModel = .constant
	m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	m.depth_draw_mode = BaseMaterial3D.DEPTH_DRAW_DISABLED    # writesToDepthBuffer = false
	# readsFromDepthBuffer = true -> keep the default depth test
	m.render_priority = 2                                     # renderingOrder = 2
	m.albedo_color.a = 0.55                                   # node.opacity = 0.55
	var node := MeshInstance3D.new()
	node.mesh = quad
	node.material_override = m
	node.name = "blob-shadow"
	node.rotation.x = -PI / 2
	node.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	node.visible = shadows_enabled                            # isHidden = !shadowsEnabled
	return node


# MARK: - Chunk debris (Particle_Object_Chunk; CHUNKS 4x4 sheet)

# Flag defaults follow the dirt call site Weapon.java:335 (getNextChunk(...,
# bl random-orientation = false, ..., bl2 sheds-smoke = true)); the spike
# splat at Weapon.java:211 flips both, so both stay parameters.
static func chunk(frame: int, pos: Vector3, velocity: Vector3, size: float, world,
		sheds_smoke := true, random_roll := false) -> void:
	var node := _billboard("CHUNKS", 4, 4, size * 2, false)
	# CHUNKS cell selection is COLUMN-major: u = frame/4, v = frame%4
	# (Particle_Object_Chunk.java:63-67, the same scheme as the coin spin; the
	# sheet's four debris balls live in column 0, rows 0-3). The Swift port
	# transposed this (col = frame%4, row = frame/4), which lands frames 1-3
	# on empty cells — Java outranks, PORTING.md rule 4.
	@warning_ignore("integer_division")
	_set_frame(node, frame / 4, frame % 4, 4, 4)
	node.position = pos
	if random_roll:                               # Chunk.java:76-80 activate(bl)
		node.roll = randf_range(0.0, TAU)
		node.manual_billboard = true
		(node.material_override as StandardMaterial3D).billboard_mode = BaseMaterial3D.BILLBOARD_DISABLED
	world.effects_root.add_child(node)
	var s := {"life": randf(), "p": pos, "v": velocity}     # Chunk.java:73 pre-aged life
	node.step = func(n: Driven, dt: float) -> bool:
		s.life += dt
		if s.life > 5.0:
			# expiry sheds a smoke puff when the smoke flag is set
			# (Particle_Object_Chunk.java:26-31; dropped by the Swift port)
			if sheds_smoke:
				FXSprites.smoke(s.p, FXSprites._rand_v3(-0.5, 0.5), randf_range(0.3, 0.8), world)
			return false
		var damp := pow(0.99, dt)
		var p: Vector3 = s.p
		var v: Vector3 = s.v
		v.x *= damp
		v.z *= damp
		p += v * dt
		v.y -= 32 * dt
		var ground: float = world.terrain.height(p.x, p.z)
		if p.y <= ground:
			if ground < 1.0:
				return false                      # water: sinks silently
			if v.y < 0:
				# landing puff (Particle_Object_Chunk.java:46-52; dropped by
				# the Swift port)
				if sheds_smoke:
					FXSprites.smoke(p, FXSprites._rand_v3(-0.5, 0.5), randf_range(0.3, 0.8), world)
				return false
			p.y = ground + 1
		s.p = p
		s.v = v
		n.position = p
		return true


## Impact dirt: pairs of CHUNKS frames 2+3 with the Weapon.java:335 spread.
static func dirt_chunks(pos: Vector3, world, count := 4) -> void:
	for i in count:
		chunk(2 + (i % 2),
				pos + Vector3(randf_range(-0.5, 0.5), 2, randf_range(-0.5, 0.5)),
				Vector3(randf_range(-15, 15), 30 + randf_range(-5, 5), randf_range(-15, 15)),
				randf_range(0.1, 1.1), world)


# MARK: - Star (Particle_Object_Star; STAR 4x4 sheet @10fps)

static func star(pos: Vector3, velocity: Vector3, size: float, world) -> void:
	var node := _billboard("STAR", 4, 4, size * 2, true)
	node.position = pos
	# random screen roll, verbatim from Particle_Object_Star.java:83 (the
	# Swift port dropped it); manual billboard, as for the coin.
	node.roll = randf_range(0.0, TAU)
	node.manual_billboard = true
	(node.material_override as StandardMaterial3D).billboard_mode = BaseMaterial3D.BILLBOARD_DISABLED
	world.effects_root.add_child(node)
	var s := {
		# random start frame and pre-aged life, verbatim from
		# Particle_Object_Star.java:74/:84 (the Swift port started both at 0 —
		# Java outranks, PORTING.md rule 4)
		"frame": randf() * 15.0,
		"last_frame": -1,
		"life": randf(),
		"opacity": 255.0,
		"p": pos,
		"v": velocity,
	}
	node.step = func(n: Driven, dt: float) -> bool:
		if s.v.y < 0 or s.opacity < 255.0:
			s.opacity -= dt * 180
		s.frame += dt * 10
		while s.frame > 15.5:
			s.frame -= 15
		s.life += dt
		if s.life > 5.0 or s.opacity <= 0.0:
			FXSprites.smoke(s.p, FXSprites._rand_v3(-0.5, 0.5), randf_range(0.3, 0.8), world)
			return false
		var damp := pow(0.99, dt)
		var p: Vector3 = s.p
		var v: Vector3 = s.v
		v.x *= damp
		v.z *= damp
		p += v * dt
		v.y -= 32 * dt * 0.75
		s.p = p
		s.v = v
		var ground: float = world.terrain.height(p.x, p.z)
		if p.y <= ground:
			if ground >= 1.0:
				FXSprites.smoke(p, FXSprites._rand_v3(-0.5, 0.5), randf_range(0.3, 0.8), world)
			return false
		n.position = p
		n.set_opacity(s.opacity / 255.0)
		var f := int(s.frame)
		if f != s.last_frame:
			s.last_frame = f
			@warning_ignore("integer_division")
			FXSprites._set_frame(n, f / 4, f % 4, 4, 4)
		return true


# MARK: - Sparkle (Particle_Object_Sparkle; FIREFLY glow, wandering swirl)

static func sparkle(pos: Vector3, velocity: Vector3, world) -> void:
	var node := _billboard("FIREFLY", 1, 1, randf_range(0.5, 1.5) * 2, true)
	node.position = pos
	world.effects_root.add_child(node)
	var s := {"life": 5.0, "p": pos, "v": velocity, "left": randf() < 0.5, "up": randf() < 0.5}
	node.step = func(n: Driven, dt: float) -> bool:
		s.life -= dt
		if s.life <= 0:
			return false
		if randf() < 0.1:
			s.left = not s.left
		if randf() < 0.1:
			s.up = not s.up
		var v: Vector3 = s.v
		v.x += (-1.0 if s.left else 1.0) * dt * 6
		v.y += (1.0 if s.up else -1.0) * dt * 4
		var p: Vector3 = s.p
		p += v * dt
		s.p = p
		s.v = v
		n.position = p
		n.set_opacity(minf(1.0, s.life / 1.5))
		return true


# MARK: - Ray (Particle_Object_Ray; vertical light shaft at the terrain)

static func ray(x: float, z: float, scale: float, delayed: bool, world) -> void:
	var quad := QuadMesh.new()
	quad.size = Vector2(scale, 1)                             # SCNPlane(width: scale, height: 1)
	var m := _flat_additive_material("RAY")
	m.billboard_mode = BaseMaterial3D.BILLBOARD_ENABLED       # SCNBillboardConstraint
	m.billboard_keep_scale = true                             # (w, h, 1) scale animates
	var node := Driven.new()
	node.mesh = quad
	node.material_override = m
	var ground: float = world.terrain.height(x, z)
	node.position = Vector3(x, ground - 0.25, z)
	node.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	world.effects_root.add_child(node)
	var s := {
		"delay": randf() if delayed else 0.0,
		"h": 0.001,
		"w": scale,
		"rate": randf_range(140.0, 260.0),
		"opacity": 255.0,
	}
	node.set_opacity(0.0)                                     # node.opacity = 0
	node.step = func(n: Driven, dt: float) -> bool:
		s.delay -= dt
		if s.delay > 0:
			return true
		if s.h > 5.0:
			s.opacity -= dt * 100
		s.h += dt * s.rate
		s.w += dt * s.rate * 0.01
		s.rate *= pow(0.1, dt)
		if s.opacity < 0:
			return false
		n.set_opacity(s.opacity / 255.0)
		n.scale = Vector3(s.w, s.h, 1)
		n.position.y = world.terrain.height(x, z) - 0.25 + s.h / 2
		return true


## Shared material for the flat/shaft additive planes (ray, shockwave,
## splash ring): constant lighting, additive, double-sided, no depth write.
static func _flat_additive_material(sheet: String) -> StandardMaterial3D:
	var m := StandardMaterial3D.new()
	m.albedo_texture = image(sheet)
	m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED     # lightingModel = .constant
	m.blend_mode = BaseMaterial3D.BLEND_MODE_ADD              # blendMode = .add
	m.cull_mode = BaseMaterial3D.CULL_DISABLED                # isDoubleSided = true
	m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	m.depth_draw_mode = BaseMaterial3D.DEPTH_DRAW_DISABLED    # writesToDepthBuffer = false
	return m


# MARK: - Shockwave (Particle_Object_Shockwave; expanding ground ring)

static func shockwave(pos: Vector3, scale: float, rate: float, world) -> void:
	var quad := QuadMesh.new()
	quad.size = Vector2(2, 2)
	var node := Driven.new()
	node.mesh = quad
	node.material_override = _flat_additive_material("SHOCKWAVE")
	node.position = Vector3(pos.x, pos.y + 0.25, pos.z)
	node.rotation.x = -PI / 2
	node.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	world.effects_root.add_child(node)
	var s := {"opacity": 255.0, "s": scale}
	node.step = func(n: Driven, dt: float) -> bool:
		# fade 140/s (Particle_Object_Shockwave.java:55; the Swift port wrote
		# 170 — Java outranks, PORTING.md rule 4)
		s.opacity -= dt * 140
		s.s += rate * dt * 2
		if s.opacity < 0:
			return false
		n.set_opacity(s.opacity / 255.0)
		n.scale = Vector3(s.s, s.s, s.s)
		return true


# MARK: - Smoke column (Particle_Object_SmokeColumn; stationary emitter)

static func smoke_column(pos: Vector3, duration: float, world,
		fire := false, black := false, alive := Callable()) -> void:
	var node := Driven.new()
	node.position = pos
	world.effects_root.add_child(node)
	var s := {"life": duration, "tick": 0.0}
	node.step = func(_n: Driven, dt: float) -> bool:
		if alive.is_valid() and not alive.call():   # prop fires die with the prop
			return false
		s.life -= dt
		if s.life <= 0:
			return false
		s.tick += dt
		if s.tick <= 1.0 / 30:                      # per original frame cadence
			return true
		s.tick = 0.0
		if randf() < 0.7:
			FXSprites.smoke(pos, Vector3(randf_range(-1.5, 1.5), randf_range(10.0, 13.0),
					randf_range(-1.5, 1.5)), randf_range(0.6, 1.4), world, black)
		if fire:
			if randf() < 0.1:
				FXSprites.explosion1(pos, Vector3(0, randf_range(4.0, 9.0), 0),
						randf_range(4.0, 6.0), world)
			if randf() < 0.1:
				FXSprites.sparkle(pos + Vector3(randf_range(-1, 1), 0, randf_range(-1, 1)),
						Vector3(randf_range(-0.5, 0.5), randf_range(2.0, 7.0),
								randf_range(-0.5, 0.5)), world)
		return true


# MARK: - Fire trail ember (Particle_Object_FireTrail; ballistic, sheds smoke)

static func fire_trail_ember(pos: Vector3, velocity: Vector3, world) -> void:
	var node := Driven.new()
	node.position = pos
	world.effects_root.add_child(node)
	var s := {"life": 0.0, "p": pos, "v": velocity}
	node.step = func(_n: Driven, dt: float) -> bool:
		s.life += dt
		if s.life > 6.0:
			return false
		# 50% smoke shed + 30% mini fireball per frame, verbatim from
		# Particle_Object_FireTrail.java:73-82 (the Swift port shed smoke every
		# frame and dropped the fireballs — Java outranks, PORTING.md rule 4)
		if randf() < 0.5:
			FXSprites.smoke(s.p, FXSprites._rand_v3(-0.5, 0.5), randf_range(0.3, 1.1), world)
		if randf() < 0.3:
			FXSprites.explosion1(s.p, Vector3.ZERO, randf_range(3.0, 5.0), world)
		var p: Vector3 = s.p
		var v: Vector3 = s.v
		p += v * dt
		v.y -= 32 * dt
		var ground: float = world.terrain.height(p.x, p.z)
		if p.y <= ground:
			if ground < 1.0:
				for i in 9:
					FXSprites.splash_jet(Vector3(p.x, 0, p.z),
							Vector3(randf_range(-7.5, 7.5), 20 + randf_range(-7.5, 7.5),
									randf_range(-5, 5)), world)
				FXSprites.splash_ring(Vector3(p.x, 0.4, p.z), 3, 2, world)
				return false
			p.y = ground
			if v.y < 0:
				v.y = -v.y / 1.25
		s.p = p
		s.v = v
		return true


## The teleport effect (Particle_Object_Teleport): a smoke column, a ring of
## 12 STAR sprites, 12 sparkles, and a 1.5s expanding spiral of light rays.
static func teleport_burst(pos: Vector3, world) -> void:
	smoke_column(pos - Vector3(0, 4, 0), 0.5, world)
	var ring := Vector3(0, 0, 20)
	for i in 12:
		var ca := cos(PI / 6)
		var sa := sin(PI / 6)
		ring = Vector3(ring.x * ca + ring.z * sa, 0, -ring.x * sa + ring.z * ca)
		star(pos, Vector3(ring.x, 10, ring.z), randf_range(1.0, 3.0), world)
		sparkle(pos + Vector3(randf_range(-7.5, 7.5), randf_range(0.0, 2.0),
						randf_range(-7.5, 7.5)),
				Vector3(randf_range(-2.5, 2.5), randf_range(2.0, 7.0),
						randf_range(-5.0, 5.0)), world)
	# spiral rays: angle sweeps 500 deg/s, radius grows 2.5/s, one ray each 0.01s
	var spinner := Driven.new()
	world.effects_root.add_child(spinner)
	var s := {"life": 0.0, "angle": 0.0, "radius": 0.0, "tick": 0.0}
	spinner.step = func(_n: Driven, dt: float) -> bool:
		s.life += dt
		if s.life > 1.5:
			return false
		s.angle += dt * 500 * PI / 180
		s.radius += dt * 2.5
		s.tick += dt
		if s.tick > 0.01:
			s.tick = 0.0
			FXSprites.ray(pos.x + sin(s.angle) * s.radius * 2,
					pos.z + cos(s.angle) * s.radius * 2,
					randf_range(2.0, 4.0), false, world)
		return true


## Expanding, fading ring lying flat on the water (Particle_Object_SplashRing).
static func splash_ring(pos: Vector3, scale: float, rate: float, world) -> void:
	var quad := QuadMesh.new()
	quad.size = Vector2(2, 2)
	var node := Driven.new()
	node.mesh = quad
	node.material_override = _flat_additive_material("SPLASHRING")
	# the original hangs rings off the bobbing WaterGroup at y + 0.25
	# (Particle_Object_SplashRing.java ctor); the clone pins max(y, 0.25) in
	# the static effects root — macOS-verified choice, carried as-is
	node.position = Vector3(pos.x, maxf(pos.y, 0.25), pos.z)
	node.rotation.x = -PI / 2
	node.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	world.effects_root.add_child(node)
	var s := {"opacity": 255.0, "s": scale}
	node.step = func(n: Driven, dt: float) -> bool:
		s.opacity -= dt * 70
		s.s += rate * dt * 2
		if s.opacity < 0:
			return false
		n.set_opacity(s.opacity / 255.0)
		n.scale = Vector3(s.s, s.s, s.s)
		return true
