class_name Cannon
extends RefCounted
# Port of macos/Sources/Cannonballs/Cannon.swift (which cites Cannon.java).
# One player entity: cannon on the island. Owns aiming state, power bar, cash, lives.
#
# GDScript port notes (windows/PORTING.md):
# - Gameplay objects are plain classes owning a Node3D subtree; the game loop
#   calls spin_input/update_power_bar/update/sync_node explicitly (deterministic
#   turn ordering). Only FX are self-driving (fx_sprites.gd).
# - `game` (GameController) stays untyped until src/game_controller.gd lands
#   (PORTING.md typing rule). The Swift's `weak var game` drops; the GC port
#   must clear players' game refs at teardown to break the ref cycle.
# - Where the decompiled Java disagrees with the Swift, the Java wins
#   (PORTING.md rule 4); every such spot is marked "Java outranks" inline.

var index: int
var name: String
var color_index: int
var bot_type: int              # 0 = human
var is_bot: bool:
	get:
		return bot_type > 0

var game = null                # GameController (duck-typed)

# world state
var position := Vector3(0, 6, 0)
var active := true             # still in the game
var dying := false             # death animation / waiting respawn
var respawns_used := 0
var respawn_timer := 0.0

# economy / stats
var cash := 0
var kills := 0
var misses := 0
var deaths := 0
var drownings := 0
var gold_spent := 0

# aiming (SPEC §3)
var spin_angle := 0.0          # azimuth degrees
var current_spin_target := 0.0 # -1..1 held-key ramp
var current_tilt_target := 0.0 # -1..1
var tilt_acceleration := 0.0
var active_tilt := 0.0         # eased barrel angle; -60(up)..+30(down)
var last_tilt_marker := -1000.0  # -1000 sentinel = no shot yet, marker hidden (Cannon.java:44)
var tilt_angle := 0.0            # un-eased tilt target (Cannon.java TiltAngle); active_tilt chases it

# power bar
var power_bar_active := false
var power_ascending := true
var power_level := 0.0
var last_power_level := -1000.0  # -1000 sentinel = no shot yet, marker hidden (Cannon.java:53)

var weapon_index: int = G.Weapon.CANNONBALL

# turn bookkeeping
var fired_on_turn := false         # a live shot that should pass the turn when it dies
var has_fired_this_turn := false   # offensive shot already spent this turn

# input flags (only wired for human-controlled)
var key_left := false
var key_right := false
var key_up := false
var key_down := false
var _was_spinning := false
var _was_tilting := false

# bot AI state (bot_ai.gd)
var bot := BotState.new()

# scene
var node := Node3D.new()
var barrel_pivot := Node3D.new()
var _barrel_node: Node3D = null
# skinned barrel actor + decoded original "fire" recoil motion (squash/recoil)
var _barrel_actor: SkinnedModel.Actor = null
static var _fire_motion: SkinnedModel.Motion = null
static var _fire_motion_loaded := false


static func fire_motion() -> SkinnedModel.Motion:
	if not _fire_motion_loaded:
		_fire_motion_loaded = true
		_fire_motion = SkinnedModel.load_motion("MODELS/CANNON/fire_motion.json")
	return _fire_motion


# Fit the decoded CANNON model to the game's cannon rig (tuned by rendering).
const CANNON_SCALE := 0.85
const CANNON_Y_OFFSET := -2.2


## The 2002 engine lit models with flat Gouraud + heavy ambient, so the decoded
## textures carry their shading baked in. Render the cannon unlit (constant) with
## nearest filtering: no modern smooth-plastic gloss, exactly the original look.
## (fx_sprites.gd's debris_chunk duck-loads this static by this exact name.)
static func style_cannon_materials(root: Node3D) -> void:
	if root == null:
		return
	for m in _materials_in_tree(root):
		var mat := m as StandardMaterial3D
		if mat == null:
			continue
		mat.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED          # lightingModel = .constant
		mat.texture_filter = BaseMaterial3D.TEXTURE_FILTER_NEAREST_WITH_MIPMAPS  # magnificationFilter = .nearest
		# specular black -> metallic_specular 0; emission black is the default.
		# NOT PORTED: ambient.contents = black — Godot StandardMaterial3D has no
		# per-material ambient reflectance knob (same omission as model_library).
		mat.metallic_specular = 0.0
		mat.roughness = 1.0
		mat.emission_enabled = false
		mat.transparency = BaseMaterial3D.TRANSPARENCY_DISABLED         # transparency = 1
		mat.albedo_color.a = 1.0
		mat.cull_mode = BaseMaterial3D.CULL_BACK                        # isDoubleSided = false
		mat.depth_draw_mode = BaseMaterial3D.DEPTH_DRAW_OPAQUE_ONLY     # writesToDepthBuffer = true
		mat.no_depth_test = false                                       # readsFromDepthBuffer = true


## enumerateHierarchy over every surface material (mirror of the private
## model_library._materials helper).
static func _materials_in_tree(node_in: Node) -> Array:
	var out: Array = []
	if node_in is MeshInstance3D:
		var mi: MeshInstance3D = node_in
		if mi.material_override != null:
			out.append(mi.material_override)
		if mi.mesh != null:
			for s in mi.mesh.get_surface_count():
				var m := mi.mesh.surface_get_material(s)
				if m != null:
					out.append(m)
	for c in node_in.get_children():
		out.append_array(_materials_in_tree(c))
	return out


## The original barrel texture (IMAGES/CANNON): gray body + gold trim. The
## engine tints it by player color (CannonTex.tint) — the low-saturation gray
## takes the color, the saturated gold trim keeps its shine.
static var _skin_cache := {}

static func tinted_skin(p_color_index: int) -> ImageTexture:
	if _skin_cache.has(p_color_index):
		return _skin_cache[p_color_index]
	var base := Assets.image("MODELS/CANNON/textures/cannonskin.png")
	if base == null:
		return null
	base = base.duplicate()
	base.convert(Image.FORMAT_RGBA8)
	var rgb: Color = G.COLOR_RGB[p_color_index % 4]   # COLORRGB is already 0-1
	for y in base.get_height():
		for x in base.get_width():
			var c := base.get_pixel(x, y)
			var mx := maxf(c.r, maxf(c.g, c.b))
			var mn := minf(c.r, minf(c.g, c.b))
			var sat := (mx - mn) / mx if mx > 0 else 0.0
			if sat >= 0.35:
				continue                       # gold trim keeps its shine
			# straight modulate: body = texel * player color. Fitted to the
			# references (red barrel (73,26,26) in cannonballs.jpg, blue
			# (11,52,80) in video frame_007 — plain multiply predicts (78,16,16)
			# and (12,50,78)).
			var luma := (c.r + c.g + c.b) / 3
			base.set_pixel(x, y, Color(minf(1.0, rgb.r * luma), minf(1.0, rgb.g * luma),
					minf(1.0, rgb.b * luma), c.a))
	var out := ImageTexture.create_from_image(base)
	_skin_cache[p_color_index] = out
	return out


## Assemble the cannon from the four decoded parts (native scale, shared
## origin at the barrel pivot; the stone column extends into the terrain).
## Returns [statics: Node3D, barrel_actor: SkinnedModel.Actor or null].
static func build_parts(p_color_index: int) -> Array:
	var statics := Node3D.new()
	for file in ["stand_skinned.json", "platform_skinned.json", "stone_skinned.json"]:
		var part := SkinnedModel.load_actor("CANNON", file)
		if part == null:
			continue
		style_cannon_materials(part.root)
		part.root.name = String(file).replace("_skinned.json", "")
		statics.add_child(part.root)
	apply_reflection(statics)
	var barrel := SkinnedModel.load_actor("CANNON", "barrel_skinned.json")
	if barrel != null:
		style_cannon_materials(barrel.root)
		var skin := tinted_skin(p_color_index)
		if skin != null:
			for m in _materials_in_tree(barrel.root):
				if m is StandardMaterial3D:
					m.albedo_texture = skin
		# NOT PORTED: the barrel's strong additive REFLECTION layer
		# (m.reflective, intensity 0.45) — StandardMaterial3D has no spherical
		# reflection-map slot; would need a custom shader. See apply_reflection.
	return [statics, barrel]


## The original cannon shader carried a REFLECTION environment layer; the macOS
## build applies it as SCNMaterial.reflective (intensity 0.18 statics / 0.45
## barrel). NOT PORTED: Godot's StandardMaterial3D has no sphere-map reflection
## slot (metallic reflections sample the sky, not the decoded REFLECTION image);
## a faithful port needs a ShaderMaterial. Kept as a documented no-op so the
## call structure survives for that later pass.
static func apply_reflection(_root: Node3D) -> void:
	pass


func _init(p_index: int, config: G.PlayerConfig, p_game) -> void:
	index = p_index
	name = config.name
	color_index = config.color_index
	bot_type = config.bot_type
	game = p_game
	cash = G.STARTING_CASH_TABLE[game.options.starting_cash_index]

	var tint: Color = G.COLOR_RGB[color_index % 4]

	barrel_pivot.position = Vector3(0, -0.5, 0)
	node.add_child(barrel_pivot)
	var blob := FXSprites.blob_shadow(7)   # original SHADOW patch under the cannon
	blob.position.y = -2.0
	node.add_child(blob)
	var built_barrel: Node3D = null

	# The four decoded cannon parts at NATIVE scale (the engine loads the
	# actors unscaled): stand + platform + buried stone under the node,
	# the player-tinted 10-bone barrel on the tilt pivot (fire recoil).
	var parts := Cannon.build_parts(color_index)
	var statics: Node3D = parts[0]
	var barrel: SkinnedModel.Actor = parts[1]
	# shared part origin = barrel pivot, ~4.3 above the platform bottom;
	# node origin sits 6 above ground (toGround), so parts go at -1.7
	statics.position = Vector3(0, -1.7, 0)
	# face +Z (fire direction). Mesh convention: decoded WT meshes rest facing
	# -Z — the cannon barrel pins setOrientation(0,1,0,θ) for meshes (rest
	# facing -Z, π yaw compensation, Cannon.buildParts probe).
	statics.rotation.y = PI
	node.add_child(statics)
	if barrel != null:
		barrel.root.position = Vector3(0, -1.2, 0)
		barrel.root.rotation.y = PI
		barrel_pivot.add_child(barrel.root)
		_barrel_actor = barrel
		built_barrel = barrel.root

	# Fallback: procedural cannon if the model failed to load. (The Swift's
	# PropGeometry.material lives in the concurrently-ported props.gd, so the
	# equivalent flat material is built locally — _flat_material.)
	if built_barrel == null:
		var carriage := MeshInstance3D.new()
		var box := BoxMesh.new()
		box.size = Vector3(7, 3, 9)   # SCNBox(width:7 height:3 length:9)
		carriage.mesh = box
		carriage.material_override = _flat_material(Color(0.35, 0.22, 0.1))
		carriage.position = Vector3(0, -3.5, 0)
		node.add_child(carriage)
		var wheel_mat := _flat_material(Color(0.25, 0.16, 0.08))
		for dxz in [[-3.8, -2.5], [3.8, -2.5], [-3.8, 2.5], [3.8, 2.5]]:
			var wheel := MeshInstance3D.new()
			var torus := TorusMesh.new()
			torus.inner_radius = 1.7 - 0.55   # SCNTorus(ringRadius:1.7 pipeRadius:0.55)
			torus.outer_radius = 1.7 + 0.55
			wheel.mesh = torus
			wheel.material_override = wheel_mat
			wheel.rotation.z = PI / 2
			wheel.position = Vector3(dxz[0], -4.2, dxz[1])
			node.add_child(wheel)
		var fb_barrel := MeshInstance3D.new()
		var cyl := CylinderMesh.new()
		cyl.top_radius = 2.1
		cyl.bottom_radius = 2.1
		cyl.height = 9.5
		fb_barrel.mesh = cyl
		fb_barrel.material_override = _flat_material(tint.darkened(0.35))
		fb_barrel.rotation.x = PI / 2
		fb_barrel.position = Vector3(0, 0, 2.2)
		barrel_pivot.add_child(fb_barrel)
		var muzzle_ring := MeshInstance3D.new()
		var ring := TorusMesh.new()
		ring.inner_radius = 2.1 - 0.45
		ring.outer_radius = 2.1 + 0.45
		muzzle_ring.mesh = ring
		muzzle_ring.material_override = _flat_material(Color(0.85, 0.7, 0.2))
		muzzle_ring.rotation.x = PI / 2
		muzzle_ring.position = Vector3(0, 0, 6.6)
		barrel_pivot.add_child(muzzle_ring)
		built_barrel = fb_barrel
	_barrel_node = built_barrel

	# Name labels are drawn by the HUD (screen-space targetbar tags projected
	# from the cannon position, like the original). No 3D banner in the scene.

	node.name = "cannon-%d" % index


static func _flat_material(color: Color) -> StandardMaterial3D:
	var m := StandardMaterial3D.new()
	m.albedo_color = color
	m.roughness = 1.0            # .lambert (PORTING.md)
	m.metallic_specular = 0.0
	return m


# MARK: - Per-frame drive

## The macOS build's fire-recoil CAKeyframeAnimation runs on the render clock;
## here the game loop drives the barrel actor explicitly.
func update(dt: float) -> void:
	if _barrel_actor != null:
		_barrel_actor.update(dt)


# MARK: - Aim math

## Fire direction unit vector from spin + tilt (elevation = -active_tilt).
var fire_direction: Vector3:
	get:
		var elev := deg_to_rad(-active_tilt)   # G.deg2rad (Types.swift:26)
		var yaw := deg_to_rad(spin_angle)
		var h := cos(elev)
		return Vector3(sin(yaw) * h, sin(elev), cos(yaw) * h)

var muzzle_position: Vector3:
	get:
		return position + fire_direction * 5.0

## Barrel elevation in degrees for HUD (-30 down … +60 up)
var elevation_deg: float:
	get:
		return -active_tilt


# MARK: - Input (SPEC §3)

func spin_input(dt: float) -> void:
	# +spin is a CCW (leftward) turn on screen, so left arrow ramps positive
	if key_left:
		current_spin_target = minf(1, current_spin_target + 1.3 * dt)
	if key_right:
		current_spin_target = maxf(-1, current_spin_target - 1.3 * dt)
	if not key_left and not key_right:
		current_spin_target *= pow(0.001, dt)   # fast damp (Cannon.java:632-634)
		if absf(current_spin_target) < 0.001:
			current_spin_target = 0
	spin_angle += current_spin_target * G.MAX_SPIN_SPEED * dt
	spin_angle = fmod(spin_angle, 360.0)

	# aiming audio (human only): spin servo loop + stop clank; tilt servo loop
	if not is_bot and Audio.shared != null:
		var spinning := key_left or key_right
		if spinning:
			Audio.shared.start_loop("turn_loop", 0.4)
		elif _was_spinning:
			Audio.shared.stop_loop("turn_loop")
			Audio.shared.play("turn_stop", 0.5)
		_was_spinning = spinning
		var tilting := key_up or key_down
		if tilting:
			Audio.shared.start_loop("tilt", 0.4)
		elif _was_tilting:
			Audio.shared.stop_loop("tilt")
		_was_tilting = tilting

	# tilt: up key drives target negative (barrel up)
	if key_up:
		tilt_acceleration = maxf(-3, tilt_acceleration - 3 * dt)
	if key_down:
		tilt_acceleration = minf(3, tilt_acceleration + 3 * dt)
	if not key_up and not key_down:
		# Java damps the acceleration, it doesn't zero it (dampAngles,
		# Cannon.java:635-637 — the Swift zeroed instantly; Java outranks)
		tilt_acceleration *= pow(0.001, dt)
	current_tilt_target += tilt_acceleration * dt
	# hitting either end of the ramp kills the acceleration (Cannon.java:622-631;
	# dropped by the Swift — Java outranks)
	if current_tilt_target > 1.0:
		current_tilt_target = 1.0
		tilt_acceleration = 0.0
	if current_tilt_target < -1.0:
		current_tilt_target = -1.0
		tilt_acceleration = 0.0
	tilt_angle = G.MIN_TILT_ANGLE * current_tilt_target if current_tilt_target > 0 \
			else G.MAX_TILT_ANGLE * current_tilt_target   # Cannon.java:406
	active_tilt += (tilt_angle - active_tilt) / 5         # per-call ease (Cannon.java:407-408)


## SPACE handler.
func trigger_fire() -> void:
	if game == null or not active or dying:
		return
	var w := weapon_index
	# off-turn: only defensive weapons
	if game.current_player_index != index and G.WEAPON_OFFENSIVE[w]:
		if game.hud != null:
			game.hud.flash_message("Only Defensive Items Can Be Used On Your Off Turn!")
		if Audio.shared != null:
			Audio.shared.play("time_up")   # Sound_TimeUp (Cannon.java:213-217; the Swift dropped it — Java outranks)
		return
	if game.current_player_index == index and has_fired_this_turn:
		return
	if cash < G.WEAPON_COSTS[w]:
		if game.hud != null:
			game.hud.flash_message("Not Enough Gold For That Weapon!")
		if Audio.shared != null:
			Audio.shared.play("time_up")
		return
	if not G.WEAPON_IS_PROJECTILE[w]:
		_fire_instant(w)
		return
	if not power_bar_active:
		power_bar_active = true
		power_ascending = true
		power_level = 0
	else:
		fire(power_level)


func update_power_bar(dt: float) -> void:
	if not power_bar_active:
		return
	var step := minf(dt / 2, 0.01)   # Cannon.java:556-568
	if power_ascending:
		power_level += step
		if power_level >= 1:
			power_level = 1
			power_ascending = false
	else:
		power_level -= step
		if power_level <= 0:
			power_level = 0
			fire(0)   # auto-fire at 0 (Cannon.java:570-574)


# MARK: - Firing

func fire(power: float) -> void:
	if game == null:
		return
	power_bar_active = false
	var w := weapon_index
	if cash < G.WEAPON_COSTS[w]:
		return
	cash -= G.WEAPON_COSTS[w]
	gold_spent += G.WEAPON_COSTS[w]
	last_power_level = power
	last_tilt_marker = tilt_angle          # Java stores TiltAngle, not elevation (Cannon.java:602)

	var dir := fire_direction
	var mp := muzzle_position
	var projectile := Projectile.new(w, self, game, mp, dir * ((power + 0.5) * 100.0))
	game.launch(projectile, self)
	if game.current_player_index == index:
		has_fired_this_turn = true
		fired_on_turn = true
	# Launch audio per source (Weapon.java fire:1185-1234): dumbfire = missile
	# launch + hum; others = cannon blast; the cannonball family AND TNT (case 9)
	# whistle in flight; 5/6/8/11 add HUM. (The Swift dropped TNT's whistle, the
	# roller/target-teleport hums and dumbfire's hum — Java outranks.)
	if Audio.shared != null:
		if w == G.Weapon.DUMBFIRE:
			Audio.shared.play("launch_missile")
			Audio.shared.play("hum", 0.6)
		else:
			Audio.shared.play("cannon_fire")
			match w:
				G.Weapon.CANNONBALL, G.Weapon.MOLEHILL, G.Weapon.CRATER, G.Weapon.SUPERCRATER, G.Weapon.TNT:
					Audio.shared.play("whistle", 0.7)
				G.Weapon.XSHOT, G.Weapon.BOUNCER, G.Weapon.SPIKEROLLER, G.Weapon.TARGET_TELEPORT:
					Audio.shared.play("hum", 0.6)
				_:
					pass
	# Muzzle smoke: 20 puffs shot out along the fire direction, verbatim from
	# Cannon.java:1605-1609 — traj = dir*(2+8r) + jitter ±1.5/axis, scale 0.2+r.
	# (The Swift's Particles.muzzleSmoke 4-puff approximation — Java outranks.)
	for i in 20:
		var f := 2.0 + randf() * 8.0
		FXSprites.smoke(mp,
				Vector3(dir.x * f + (randf() - 0.5) * 3.0,
						dir.y * f + (randf() - 0.5) * 3.0,
						dir.z * f + (randf() - 0.5) * 3.0),
				0.2 + randf(), game.world)
	# Original barrel recoil: the decoded CANNON "fire" motion (0.73s squash).
	var motion := Cannon.fire_motion()
	if _barrel_actor != null and motion != null:
		SkinnedModel.play_once(_barrel_actor, motion)


func _fire_instant(w: int) -> void:
	if game == null:
		return
	match w:
		G.Weapon.TOWER:
			if game.world.object_above(position.x, position.z):
				if game.hud != null:
					game.hud.flash_message("Can't Use Tower On Object!")
				if Audio.shared != null:
					Audio.shared.play("time_up")   # Sound_TimeUp (Cannon.java:226-229)
				return
			cash -= G.WEAPON_COSTS[w]
			gold_spent += G.WEAPON_COSTS[w]
			# doTower (Weapon.java:484-495): molehill(40, 30) with NO texture
			# splat (Island.molehill bl=false — the Swift's brown splat color is
			# dropped; Java outranks). Island.molehill itself quakes + rays
			# (Island.java:623-631).
			game.world.terrain.molehill(position.x, position.z, 40, 30)
			Projectile.quake_rays_fx(position.x, position.z, 30.0, game.world)
			game.instant_weapon_used(self)
		G.Weapon.TELEPORT:
			cash -= G.WEAPON_COSTS[w]
			gold_spent += G.WEAPON_COSTS[w]
			player_teleport()
			game.instant_weapon_used(self)
		_:
			pass


# MARK: - Teleport / placement (SPEC §6 place rule)

func player_teleport(x = null, z = null) -> void:
	if game == null:
		return
	Particles.teleport(position, game.world)
	if Audio.shared != null:
		Audio.shared.play("teleport")
		Audio.shared.play("puff", 0.6)   # Sound_Puff on cannon placement
	if x != null and z != null:
		position = Vector3(x, game.world.terrain.height(x, z) + 6, z)
	else:
		place(100)
	to_ground()
	Particles.teleport(position, game.world)


## Random valid placement (rejection sampling per SPEC).
func place(min_dist_from_self := 0.0) -> void:
	if game == null:
		return
	var terrain = game.world.terrain
	var size: float = Terrain.world_size()
	var origin := position
	for attempt in 200:
		var x := randf_range(size * 0.06, size * 0.94)
		var z := randf_range(size * 0.06, size * 0.94)
		if terrain.target_height(x, z) <= 2.0:
			continue
		var p := Vector3(x, 0, z)
		if min_dist_from_self > 0 and G.dist_2d(p, origin) < min_dist_from_self:
			continue
		var blocked := false
		for other in game.players:
			if other != self and other.active and not other.dying \
					and G.dist_2d(other.position, p) < 100:
				blocked = true
				break
		if blocked:
			continue
		for pr in game.world.props:
			if not pr.alive:
				continue
			var d: float = G.dist_2d(pr.position, p)
			var limit: float = 20.0 if pr.spec.destructible else 10.0 + pr.spec.radius
			if d < limit:
				blocked = true
				break
		if blocked:
			continue
		for chest in game.chests:
			if chest.alive and G.dist_2d(chest.position, p) < 10:
				blocked = true
				break
		if blocked:
			continue
		position = Vector3(x, terrain.height(x, z) + 6, z)
		return
	# fallback: raise a platform (Cannon.java:355-365 — Island.molehillAbsolute
	# plays the quake + 30 rays itself, Island.java:386-394)
	var fx := randf_range(size * 0.2, size * 0.8)
	var fz := randf_range(size * 0.2, size * 0.8)
	terrain.molehill_absolute(fx, fz, 10, 30)
	Projectile.quake_rays_fx(fx, fz, 30.0, game.world)
	position = Vector3(fx, 10 + 6, fz)


## Keep the cannon glued to the (possibly deforming) terrain; drown check.
## NOT PORTED: the Java also raises the cannon onto STANDABLE prop decks via a
## mask-8 collision probe (Cannon.java:1212-1220) — cannons never spawn on the
## Nightbridge deck in the clone (place() rejects prop-adjacent spots anyway).
func to_ground() -> void:
	if game == null or not active or dying:
		return
	var h: float = game.world.terrain.height(position.x, position.z)
	position.y = h + 6
	if position.y <= 6.0:   # ground at/below sea level (Cannon.java:1222)
		game.kill(self, {"kind": "drowned"})


# MARK: - Scene sync

func sync_node() -> void:
	node.position = position
	node.rotation = Vector3(0, deg_to_rad(spin_angle), 0)   # G.deg2rad (Types.swift:26)
	barrel_pivot.rotation = Vector3(deg_to_rad(-(-active_tilt)), 0, 0)  # rotateX(-elev): tip up for negative active_tilt
	node.visible = active and not dying
