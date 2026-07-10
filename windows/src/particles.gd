class_name Particles
# Port of macos/Sources/Cannonballs/Particles.swift.
# Gameplay VFX. The headline effects (explosion, smoke, coins, splashes) play
# the ORIGINAL decoded sprite sheets via FXSprites with the decompiled
# per-frame physics; the rest remain procedural approximations.
#
# GDScript port notes (windows/PORTING.md): `world` moves before optional
# flags (defaults must trail in GDScript) and stays an untyped param.


static func _burst(world, pos: Vector3, configure: Callable, lifetime := 3.0) -> void:
	var ps := GPUParticles3D.new()
	configure.call(ps)
	var holder := FXSprites.Driven.new()          # let holder = SCNNode()
	holder.position = pos
	holder.add_child(ps)
	world.effects_root.add_child(holder)
	ps.emitting = true
	# DispatchQueue.asyncAfter(deadline: .now() + lifetime) { remove } becomes
	# a Driven countdown (works even before the world enters the tree)
	var s := {"t": 0.0}
	holder.step = func(_n: FXSprites.Driven, dt: float) -> bool:
		s.t += dt
		return s.t < lifetime


## The original impact boom (Weapon.java:140-144): one EXPLOSION1 sprite
## (16 frames @ 20fps) + four rising smoke puffs.
static func explosion(pos: Vector3, world, big := false) -> void:
	var size: float = 40.0 if big else 3.0 + randf_range(0.0, 2.0)
	FXSprites.explosion1(pos + Vector3(randf_range(-0.5, 0.5), 2, randf_range(-0.5, 0.5)),
			Vector3(randf_range(-0.17, 0.17), 1, randf_range(-0.17, 0.17)),
			size, world)
	var puffs := 4 if big else 2
	for i in puffs:
		FXSprites.smoke(pos + Vector3(randf_range(-0.5, 0.5), 2, randf_range(-0.5, 0.5)),
				Vector3(randf_range(-0.17, 0.17), 1, randf_range(-0.17, 0.17)),
				1.0, world)


## The original water impact (Weapon.java:87-96): 9 ballistic splash jets
## shedding droplet sprites + 3 expanding SPLASHRING planes.
## (Weapon.java throws NINE jets; the Swift port wrote 8 — Java outranks,
## PORTING.md rule 4.)
static func splash(pos: Vector3, world) -> void:
	for i in 9:
		FXSprites.splash_jet(Vector3(pos.x, 0, pos.z),
				Vector3(randf_range(-7.5, 7.5), 20 + randf_range(-7.5, 7.5),
						randf_range(-5, 5)), world)
	FXSprites.splash_ring(Vector3(pos.x, 0.11, pos.z), 3, 2, world)
	FXSprites.splash_ring(Vector3(pos.x, 0.111, pos.z), 6, 1, world)
	FXSprites.splash_ring(Vector3(pos.x, 0.1115, pos.z), 4, 4, world)


# Superseded by the faithful splash() above; kept, as the Swift keeps it.
# The SCNParticleSystem values are carried in comments where Godot's particle
# system has no 1:1 knob.
static func _splash_legacy(pos: Vector3, world) -> void:
	_burst(world, pos, func(ps: GPUParticles3D) -> void:
		ps.one_shot = true                       # loops = false
		ps.amount = int(250 * 0.08)              # birthRate 250 x emissionDuration 0.08
		ps.lifetime = 0.9                        # particleLifeSpan
		ps.explosiveness = 0.9                   # ~0.08 s emission window
		var pm := ParticleProcessMaterial.new()
		pm.direction = Vector3(0, 1, 0)          # emittingDirection
		pm.spread = 35.0                         # spreadingAngle
		pm.initial_velocity_min = 38.0 - 15.0    # particleVelocity - variation
		pm.initial_velocity_max = 38.0 + 15.0
		pm.gravity = Vector3(0, -40, 0)          # acceleration
		pm.color = Color(0.75, 0.9, 1, 0.9)      # particleColor
		ps.process_material = pm
		var quad := QuadMesh.new()
		quad.size = Vector2(2.5, 2.5)            # particleSize
		var m := StandardMaterial3D.new()
		m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
		m.vertex_color_use_as_albedo = true
		m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
		m.billboard_mode = BaseMaterial3D.BILLBOARD_PARTICLES
		quad.material = m
		ps.draw_pass_1 = quad
	)
	# expanding ring
	var ring := FXSprites.Driven.new()
	var torus := TorusMesh.new()
	torus.inner_radius = 2.0 - 0.4               # SCNTorus ringRadius 2, pipeRadius 0.4
	torus.outer_radius = 2.0 + 0.4
	ring.mesh = torus
	var m := StandardMaterial3D.new()
	m.albedo_color = Color(1, 1, 1, 0.8)         # calibratedWhite 1, alpha 0.8
	m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED   # lightingModel = .constant
	m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	ring.material_override = m
	ring.position = Vector3(pos.x, 0.4, pos.z)
	world.effects_root.add_child(ring)
	var s := {"t": 0.0}
	ring.step = func(n: FXSprites.Driven, dt: float) -> bool:
		# .group([.scale(to: 5, duration: 0.9), .fadeOut(duration: 0.9)])
		s.t += dt
		var k: float = minf(s.t / 0.9, 1.0)
		n.scale = Vector3.ONE * lerpf(1.0, 5.0, k)
		n.set_opacity(0.8 * (1.0 - k))
		return s.t < 0.9


## Ambient fish leaping from the ocean: animated FISH sprite on a billboard,
## gravity arc, splash at launch and on re-entry.
static func fish_jump(pos: Vector3, velocity: Vector3, world) -> void:
	splash(pos, world)
	var quad := QuadMesh.new()
	quad.size = Vector2(5, 5)                    # SCNPlane(width: 5, height: 5)
	var m := StandardMaterial3D.new()
	# HUDArt.crop("fish.png", 64px cells) becomes uv1 sheet-stepping over
	# HUDART/fish.png (128x256 = 2 cols x 4 rows of 64) — the exact same
	# top-left pixel rects. HUDArt's overlay-alpha compensation is
	# deliberately NOT ported (PORTING.md: Godot's canvas has no such decode;
	# this quad isn't even an overlay).
	m.albedo_texture = Assets.texture("HUDART/fish.png")
	m.texture_repeat = false
	m.uv1_scale = Vector3(1.0 / 2, 1.0 / 4, 1)
	m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED    # lightingModel = .constant
	m.cull_mode = BaseMaterial3D.CULL_DISABLED               # isDoubleSided = true
	m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA       # transparencyMode = .aOne
	m.billboard_mode = BaseMaterial3D.BILLBOARD_ENABLED      # SCNBillboardConstraint
	var fish := FXSprites.Driven.new()
	fish.mesh = quad
	fish.material_override = m
	fish.position = pos
	world.effects_root.add_child(fish)
	# ballistic arc: solve flight time for return to y=0 (v*t - 16t^2 = 0)
	var t_flight := velocity.y / 16.0
	# anim starts primed: the Swift's repeated [.run, .wait 0.09] sequence
	# advances to frame 1 immediately at t = 0
	var s := {"t": 0.0, "anim": 0.09, "frame": 0}
	fish.step = func(n: FXSprites.Driven, dt: float) -> bool:
		# flip through the 8 sheet frames while airborne (.wait 0.09 loop)
		s.anim += dt
		while s.anim >= 0.09:
			s.anim -= 0.09
			s.frame = (s.frame + 1) % 8
			var f: int = s.frame
			@warning_ignore("integer_division")
			var cell := Vector2i(f % 2, (f / 2) % 4)
			(n.material_override as StandardMaterial3D).uv1_offset = \
					Vector3(cell.x / 2.0, cell.y / 4.0, 0)
		s.t += dt
		var tf: float = minf(s.t, t_flight)
		n.position = Vector3(pos.x + velocity.x * tf,
				pos.y + velocity.y * tf + G.GRAVITY * 0.5 * tf * tf,
				pos.z + velocity.z * tf)
		if s.t >= t_flight:
			Particles.splash(Vector3(pos.x + velocity.x * t_flight, 0,
					pos.z + velocity.z * t_flight), world)
			return false
		return true


## Impact dirt: the original CHUNKS debris pair (Weapon.java:335-338).
static func dirt(pos: Vector3, world) -> void:
	FXSprites.dirt_chunks(pos, world, 2)


## Original SMOKEPUFF sprite with the decompiled growth/rise/damping physics.
static func smoke_puff(pos: Vector3, world) -> void:
	for i in 3:
		FXSprites.smoke(pos + Vector3(randf_range(-0.5, 0.5), 0, randf_range(-0.5, 0.5)),
				Vector3(randf_range(-0.17, 0.17), 1, randf_range(-0.17, 0.17)),
				2.0, world)


## Muzzle smoke along the fire direction (Cannon.java:279).
static func muzzle_smoke(pos: Vector3, world) -> void:
	for i in 4:
		FXSprites.smoke(pos,
				Vector3(randf_range(-1.5, 1.5), randf_range(-1.5, 1.5), randf_range(-1.5, 1.5)),
				randf_range(0.2, 1.2), world)


## The chest treasure burst: 30 spinning COIN sprites with gravity + bounce.
static func coins(pos: Vector3, count: int, world) -> void:
	FXSprites.coin_burst(pos, world, maxi(count, 8))


## The original teleport: smoke column + STAR ring + sparkles + ray spiral.
static func teleport(pos: Vector3, world) -> void:
	FXSprites.teleport_burst(pos, world)


static func death_blast(pos: Vector3, world) -> void:
	explosion(pos, world, true)
	smoke_puff(pos + Vector3(0, 6, 0), world)
	# the original death: 3 flaming embers arc out (Cannon.java:1120-1122)
	for i in 3:
		FXSprites.fire_trail_ember(pos + Vector3(randf_range(-0.5, 0.5), 2, randf_range(-0.5, 0.5)),
				Vector3(randf_range(-10, 10), 30 + randf_range(-5, 5), randf_range(-10, 10)),
				world)
