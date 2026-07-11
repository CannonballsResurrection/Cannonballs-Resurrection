class_name Projectile
extends RefCounted
# Port of macos/Sources/Cannonballs/Projectile.swift (which cites Weapon.java).
# A live weapon in flight (SPEC §2 integration, §6 behaviors).
#
# GDScript port notes (windows/PORTING.md):
# - Plain class owning a Node3D subtree; the game loop calls update(dt)
#   explicitly per substep (deterministic turn ordering, no _process).
# - `game`/`owner` stay duck-typed; World's prop methods (prop_hit,
#   collide_segment, standable_surface, object_above) are the concurrently
#   ported world.gd surface and are called duck-typed through `game.world`.
# - Where the decompiled Java disagrees with the Swift, the Java wins
#   (PORTING.md rule 4); every such spot is marked "Java outranks" inline.
#   The biggest structural one: the Swift folded Weapon.java's checkForHit
#   codes (1 cannon / 2 chest / 3 prop / 4 collideable / 5 standable) into
#   booleans and gave every type the same direct-hit behavior; this port keeps
#   the Java code and routes each weapon's ladder exactly (so crater weapons
#   detonate their terrain deformation on a cannon proximity hit, X-Shot digs
#   its X on any direct hit, chests/props are only killed by PROJECTILEIMPACT
#   weapons, etc.).

var type: int                  # G.Weapon
var owner                      # Cannon (untyped: cyclic ref safety)
var game = null                # GameController (duck-typed)

var position: Vector3
var last_position: Vector3
var traj: Vector3
var time_alive := 0.0
var _shadow_blob: MeshInstance3D = null
var alive := true
var rolling := false           # SpikeRoller ground mode
var node: Node3D
var hit_something := false     # for miss stat
var _tumble_axis := Vector3.ZERO   # Bouncer setConstantRotation axis (Weapon.java:1292/860)
var _grinding := false             # SpikeRoller Sound_GrindLoop state (Weapon.java:250-263)

# Java increments StatMiss only in the cannonball / xshot / bouncer / dumbfire /
# spikeroller branches (Weapon.java:83-85, 103-105, 221-224, 526-528, 692-694,
# 707-710, 836-839, 870-873); molehill, crater, supercrater, TNT and
# targetTeleport never count a miss. (The Swift counted every unhit death —
# Java outranks.) Indexed by G.Weapon.
const _COUNTS_MISS := [true, false, false, false, false, true,
		true, true, true, false, false, false]


func _init(p_type: int, p_owner, p_game, p_position: Vector3, p_velocity: Vector3) -> void:
	type = p_type
	owner = p_owner
	game = p_game
	position = p_position
	last_position = p_position
	traj = p_velocity
	node = Projectile.make_node(type, owner.color_index)
	node.position = position
	if type == G.Weapon.BOUNCER:
		# the bouncer tumbles from launch: setConstantRotation(random axis,
		# 300 deg/s) — Weapon.java:1292
		_tumble_axis = Vector3(randf() - 0.5, randf() - 0.5, randf() - 0.5).normalized()


## Load a decoded projectile model, centered and scaled to a target diameter.
static func _decoded_model(model_name: String, span_target: float) -> Node3D:
	# prefer the solved-pipeline export (correct UVs/materials)
	var actor := SkinnedModel.load_actor(model_name)
	if actor != null:
		var aabb := _tree_aabb(actor.root)
		var span0 := maxf(aabb.size.x, maxf(aabb.size.y, aabb.size.z))
		var holder := Node3D.new()
		holder.add_child(actor.root)
		if span0 > 0.001:
			holder.scale = Vector3.ONE * (span_target / span0)
		return holder
	var model := ModelLibrary.node_for(model_name)
	if model == null:
		return null
	Cannon.style_cannon_materials(model)     # unlit + opaque, like the original
	var holder := Node3D.new()
	holder.add_child(model)
	var aabb := _tree_aabb(model)
	var c := aabb.get_center()
	var s0 := model.scale.x
	model.position = Vector3(-c.x * s0, -c.y * s0, -c.z * s0)
	var span := aabb.size.length() * s0
	if span > 0.001:
		holder.scale = Vector3.ONE * (span_target / span)
	return holder


static func _tree_aabb(root: Node3D) -> AABB:
	var out := AABB()
	var found := false
	var stack: Array = [root]
	while not stack.is_empty():
		var n: Node = stack.pop_back()
		if n is MeshInstance3D and n.mesh != null:
			var a: AABB = (n as MeshInstance3D).mesh.get_aabb()
			if found:
				out = out.merge(a)
			else:
				out = a
				found = true
		for child in n.get_children():
			stack.push_back(child)
	return out


## Cannonball-family shots are 2x2 BILLBOARD SPRITES off the WEAPONS sheet
## (Weapon.java fireProjectile: cannonball rect (0,0)-(.5,.5) black ball,
## molehill (.5,0)-(1,.5) blue, crater+supercrater (0,.5)-(.5,1) purple).
## The MORTAR shell model is only used by X-Shot (5) and Dumbfire (7).
static func _weapon_sprite(col: int, row: int) -> Node3D:
	var quad := QuadMesh.new()
	quad.size = Vector2(4, 4)   # setBitmapSize 2,2 at half-res -> ~4 world units
	var m := StandardMaterial3D.new()
	m.albedo_texture = FXSprites.image("WEAPONS")
	m.texture_repeat = false
	# contentsTransform scale+translate == uv1_scale/uv1_offset, v=0 at the
	# image top (convention probe: fx_sprites._set_frame)
	m.uv1_scale = Vector3(0.5, 0.5, 1)
	m.uv1_offset = Vector3(col * 0.5, row * 0.5, 0)
	m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED     # lightingModel = .constant
	m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA        # transparencyMode = .aOne
	m.cull_mode = BaseMaterial3D.CULL_DISABLED                # isDoubleSided = true
	m.billboard_mode = BaseMaterial3D.BILLBOARD_ENABLED       # SCNBillboardConstraint
	var n := MeshInstance3D.new()
	n.mesh = quad
	n.material_override = m
	n.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	return n


static func make_node(p_type: int, owner_color: int) -> Node3D:
	# The original's projectile visuals (Weapon.java fireProjectile).
	var real: Node3D = null
	match p_type:
		G.Weapon.CANNONBALL:
			real = _weapon_sprite(0, 0)              # black cannonball
		G.Weapon.MOLEHILL:
			real = _weapon_sprite(1, 0)              # blue ball
		G.Weapon.CRATER, G.Weapon.SUPERCRATER:
			real = _weapon_sprite(0, 1)              # purple ball
		G.Weapon.XSHOT:
			real = _decoded_model("MORTAR", 4.2)     # the finned mortar shell
		G.Weapon.BOUNCER:
			real = _decoded_model("BOUNCEBALL", 4.4)
		G.Weapon.SPIKEROLLER:
			real = _decoded_model("SPIKEBALL", 5.2)
		G.Weapon.TNT:
			real = _decoded_model("TNT", 6.0)
		G.Weapon.DUMBFIRE:
			real = _decoded_model("MORTAR", 4.6)
		_:
			real = null
	if real != null:
		real.name = "projectile"
		return real
	# Procedural fallbacks (Swift used PropGeometry primitives; local
	# equivalents here — props.gd is a concurrent sibling).
	var n := Node3D.new()
	match p_type:
		G.Weapon.MOLEHILL:
			n.add_child(_ball(1.6, Color(0.2, 0.65, 0.25)))
		G.Weapon.CRATER, G.Weapon.SUPERCRATER:
			var r := 2.4 if p_type == G.Weapon.SUPERCRATER else 1.7
			n.add_child(_ball(r, Color(0.5, 0.32, 0.5)))
		G.Weapon.XSHOT:
			n.add_child(_ball(1.8, Color(1, 0.5, 0), true))
		G.Weapon.BOUNCER:
			n.add_child(_ball(2.0, Color(0.75, 0.75, 0.75)))
		G.Weapon.DUMBFIRE:
			var body := MeshInstance3D.new()
			var cap := CapsuleMesh.new()
			cap.radius = 0.9       # SCNCapsule(capRadius:0.9 height:4.5)
			cap.height = 4.5
			body.mesh = cap
			body.material_override = Cannon._flat_material(Color(0.8, 0.8, 0.8))
			body.rotation.x = PI / 2
			n.add_child(body)
			var exhaust := _ball(0.7, Color(1, 0.5, 0), true)
			exhaust.position.z = -2.6
			n.add_child(exhaust)
		G.Weapon.SPIKEROLLER:
			n.add_child(_ball(1.9, Color(0.35, 0.35, 0.35)))
			for i in 8:
				var spike := MeshInstance3D.new()
				var cone := CylinderMesh.new()
				cone.top_radius = 0.0     # SCNCone(topRadius:0 bottomRadius:0.4 height:1.6)
				cone.bottom_radius = 0.4
				cone.height = 1.6
				spike.mesh = cone
				spike.material_override = Cannon._flat_material(Color(0.3, 0.3, 0.3))
				var a := float(i) / 8 * 2 * PI
				spike.position = Vector3(cos(a) * 2.2, sin(a) * 2.2, 0)
				spike.rotation = Vector3(0, 0, a - PI / 2)
				n.add_child(spike)
		G.Weapon.TNT:
			var barrel := MeshInstance3D.new()
			var cyl := CylinderMesh.new()
			cyl.top_radius = 2.5      # PropGeometry.tntBarrel(height:4.5 radius:2.5) stand-in
			cyl.bottom_radius = 2.5
			cyl.height = 4.5
			barrel.mesh = cyl
			barrel.material_override = Cannon._flat_material(Color(0.7, 0.15, 0.12))
			n.add_child(barrel)
		G.Weapon.TARGET_TELEPORT:
			n.add_child(_ball(1.4, Color(0, 1, 1), true))
		_:
			n.add_child(_ball(1.6, Color(0.12, 0.12, 0.12)))
	n.name = "projectile"
	return n


static func _ball(radius: float, color: Color, emissive := false) -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	var sphere := SphereMesh.new()
	sphere.radius = radius
	sphere.height = radius * 2
	mi.mesh = sphere
	var m := Cannon._flat_material(color)
	if emissive:
		m.emission_enabled = true
		m.emission = color
	mi.material_override = m
	return mi


# MARK: - Per-substep update

func update(dt: float) -> void:
	if not alive or game == null:
		return
	time_alive += dt
	last_position = position
	var terrain = game.world.terrain
	var wind: Vector3 = game.wind

	# ---- integration + trails per type ----
	match type:
		G.Weapon.DUMBFIRE:
			# no gravity, wind only; 7 s life (Weapon.java:669-675)
			position += traj * dt
			traj.x += wind.x * dt
			traj.z += wind.z * dt
			_missile_trail()
		G.Weapon.SPIKEROLLER:
			position += traj * dt
			if rolling:
				# grounded roller: 20% wind, gravity flattened (Weapon.java:243-248)
				traj.x += wind.x * dt * 0.2
				traj.z += wind.z * dt * 0.2
				traj.y = 0
			else:
				traj.x += wind.x * dt
				traj.y += G.GRAVITY * dt
				traj.z += wind.z * dt
		_:
			position += traj * dt
			traj.x += wind.x * dt
			traj.y += G.GRAVITY * dt
			traj.z += wind.z * dt
			if type == G.Weapon.XSHOT:
				# X-Shot sputters the same smoke + mini-fireball trail as
				# Dumbfire while flying ballistic (Weapon.java updateXShot:70-79)
				_missile_trail()
			elif type == G.Weapon.TARGET_TELEPORT:
				# 40% sparkle trail per frame (Weapon.java:311-313; dropped by
				# the Swift port — Java outranks)
				if randf() < 0.4:
					FXSprites.sparkle(position,
							Vector3((randf() - 0.5) * 2.0, randf() * 2.0, (randf() - 0.5) * 2.0),
							game.world)

	node.position = position
	# SHADOW blob tracks the ground under the shot (original per-projectile
	# patch, Weapon.java:766-781 updateShadow — runs for EVERY type; the Swift
	# excluded dumbfire — Java outranks)
	if _shadow_blob == null:
		var b := FXSprites.blob_shadow(2)
		game.world.effects_root.add_child(b)
		_shadow_blob = b
	if _shadow_blob != null:
		var gh: float = terrain.height(position.x, position.z)
		if gh > 0 and position.y > gh:
			_shadow_blob.visible = true
			_shadow_blob.position = Vector3(position.x, gh + 0.2, position.z)
		else:
			_shadow_blob.visible = false
	# mortar-shell weapons AND the TNT barrel fly nose-first (Weapon.java
	# setOrientationVector from the normalized trajectory, updateXShot:68 /
	# updateDumbfire:678 / updateTNT:925 — the Swift dropped TNT; Java outranks)
	if (type == G.Weapon.DUMBFIRE or type == G.Weapon.XSHOT or type == G.Weapon.TNT) \
			and traj.length() > 0.01:
		var dir := traj.normalized()
		var up := Vector3.UP
		if absf(dir.y) > 0.999:
			up = Vector3(0, 0, 1)
		node.basis = Basis.looking_at(dir, up)   # simdLook(at: position + traj)
	if type == G.Weapon.SPIKEROLLER:
		node.rotation.x += Vector3(traj.x, 0, traj.z).length() * dt / 2.0
	if _tumble_axis != Vector3.ZERO:
		node.rotate(_tumble_axis, deg_to_rad(300.0) * dt)   # setConstantRotation 300 (Weapon.java:1292/860)

	# ---- hit tests (Weapon.java:1026 checkForHit) ----
	var n := _check_for_hit()
	var ground: float = terrain.height(position.x, position.z)

	# ---- per-weapon resolution ladder, transcribed branch-for-branch ----
	match type:
		G.Weapon.CANNONBALL:
			# updateCannonBall (Weapon.java:497-557); no n==1..3 branch — the
			# kill/collect happened inside checkForHit, the shot just hides
			if position.y <= 0 and ground <= 0:
				splash()
			elif position.y <= ground:
				if Audio.shared != null:
					Audio.shared.play("explosion2")   # Weapon.java:529
				terrain.crater(position.x, position.z, 4, 20)   # Weapon.java:531 (scorch bake)
				Projectile.crater_burst_fx(position.x, position.z, game.world)  # Island.crater bl=true
				game.camera.shock(position, 70.0)
				die()
			elif n >= 4:
				prop_burst()

		G.Weapon.MOLEHILL:
			# updateMoleHill (Weapon.java:559-613)
			if position.y <= 0 and ground <= 0:
				_splash_gentle()   # Weapon.java:569-582 — the gentle jet params
			elif position.y <= ground or n == 1:
				# splat + 20 CHUNKS frame 0 at ground+2 on every landing AND on
				# a cannon proximity hit (Weapon.java:583-588; the Swift used a
				# 2-chunk dirt puff and skipped the n==1 raise — Java outranks)
				if Audio.shared != null:
					Audio.shared.play("splat")
				_splat_chunks(0, ground + 2.0, 0.5, 2.0)
				die()
				# only the terrain raise is suppressed, and only by a STANDABLE
				# deck overhead (the mask-8 probe at Weapon.java:590-591, drop 22)
				if game.world.standable_surface(position.x, position.y, position.z, 22) == null:
					terrain.molehill(position.x, position.z, 10, 20,
							Color(32 / 255.0, 40 / 255.0, 135 / 255.0, 0.45))   # Weapon.java:593
					Projectile.quake_rays_fx(position.x, position.z, 20.0, game.world)  # Island.molehill:623-631
			elif n > 0:
				# codes 2/3/4/5: splat + chunks, shot done (Weapon.java:604-611)
				die()
				if Audio.shared != null:
					Audio.shared.play("splat")
				_splat_chunks(0, position.y + 2.0, 0.5, 2.0)

		G.Weapon.CRATER:
			# updateCrater (Weapon.java:615-667)
			if position.y <= 0 and ground <= 0:
				_splash_gentle()   # Weapon.java:625-638
			elif position.y <= ground or n == 1:
				# 20 chunks frame 1 + splat + the colored crater; a cannon
				# proximity hit (n==1) detonates it too (Weapon.java:639-658;
				# the Swift's smoke-puff-and-die on cannon hits — Java outranks)
				_splat_chunks(1, ground + 2.0, 0.5, 2.0)
				if Audio.shared != null:
					Audio.shared.play("splat")
				terrain.crater(position.x, position.z, 10, 30,
						Color(130 / 255.0, 31 / 255.0, 115 / 255.0, 0.45))   # Weapon.java:646
				# quake craters: Sound_Quake + 30 rays (Island.crater bl2,
				# Island.java:504-513 — offsets ±radius*0.75, not ±45; Java outranks)
				Projectile.quake_rays_fx(position.x, position.z, 30.0, game.world)
				die()
			elif n > 0:
				# splat + 20 chunks — and the shot KEEPS FLYING on a standable
				# deck (no hide in that branch, Weapon.java:659-664)
				if Audio.shared != null:
					Audio.shared.play("splat")
				_splat_chunks(1, position.y + 2.0, 0.5, 2.0)

		G.Weapon.SUPERCRATER:
			# updateSuperCrater (Weapon.java:971-1024)
			if position.y <= 0 and ground <= 0:
				splash()   # Weapon.java:981-995 uses the strong jet params
			elif position.y <= ground or n == 1:
				if Audio.shared != null:
					Audio.shared.play("splat")
				_splat_chunks(1, ground + 2.0, 0.5, 2.0)
				terrain.crater(position.x, position.z, 20, 50,
						Color(130 / 255.0, 31 / 255.0, 115 / 255.0, 0.5))    # Weapon.java:1003
				Projectile.quake_rays_fx(position.x, position.z, 50.0, game.world)
				# NO explosion: Island.crater is called with bl=false
				# (Weapon.java:1003 — the Swift's big explode() — Java outranks)
				die()
			elif n > 0:
				if Audio.shared != null:
					Audio.shared.play("splat")
				_splat_chunks(1, position.y + 2.0, 0.5, 2.0)

		G.Weapon.XSHOT:
			# updateXShot (Weapon.java:59-151): the X detonates on landing AND
			# on any direct hit code 1/2/3 (Weapon.java:100; the Swift only dug
			# the X on terrain — Java outranks)
			if position.y <= 0 and ground <= 0:
				splash()
			elif position.y <= ground or n == 1 or n == 2 or n == 3:
				if Audio.shared != null:
					Audio.shared.play("explosion2")   # Weapon.java:101
				_x_crater()
				game.camera.shock(position, 70.0)
				die()
			elif n >= 4:
				prop_burst()

		G.Weapon.BOUNCER:
			_update_bouncer(n, ground)

		G.Weapon.DUMBFIRE:
			# updateDumbfire (Weapon.java:669-757)
			if time_alive > 7.0:
				# timeout: miss counted + the n>=4-style burst FX
				# (Weapon.java:691-705; the Swift's explode() — Java outranks)
				_burst_fx()
				die()
			if position.y <= 0 and ground <= 0:
				splash()
			elif position.y <= ground or n == 1 or n == 2 or n == 3:
				if Audio.shared != null:
					Audio.shared.play("explosion3")   # Weapon.java:729 (Swift played explosion2 — Java outranks)
				terrain.crater(position.x, position.z, 6, 20)   # Weapon.java:731
				Projectile.crater_burst_fx(position.x, position.z, game.world)  # bl=true
				game.camera.shock(position, 120.0)
				die()
			elif n >= 4:
				prop_burst()

		G.Weapon.SPIKEROLLER:
			_update_spike_roller(n, ground)

		G.Weapon.TNT:
			# updateTNT (Weapon.java:915-969)
			if n == 0 or n == 5:
				if n != 5 and position.y <= 0 and ground <= 0:
					splash()
				elif position.y <= ground or n == 5:
					# plant the barrel; packet 21 + <ONGROUND>:YES snap it to
					# the TERRAIN below (Prop.java:543-545). Prop construction
					# lives in the GameController port (props.gd is concurrent).
					game.plant_tnt(Vector3(position.x, ground, position.z), owner)
					hit_something = true
					die()
			else:
				# ANY hit code 1-4 bursts the barrel (Weapon.java:956-966; the
				# Swift exploded cannon hits with explosion2 — Java outranks)
				prop_burst()

		G.Weapon.TARGET_TELEPORT:
			# updateTargetTeleport (Weapon.java:302-356)
			if position.y <= 0 and ground <= 0:
				_splash_gentle()   # Weapon.java:316-329
			elif n == 5 or (position.y <= ground and n == 0):
				if Audio.shared != null:
					Audio.shared.play("splat")   # Weapon.java:331
				# 20 CHUNKS frames 2/3 (50/50), size 0.1+r (Weapon.java:333-339;
				# dropped by the Swift — Java outranks)
				_splat_chunks(2, position.y + 2.0, 0.1, 1.0, true)
				owner.player_teleport(position.x, position.z)
				hit_something = true
				die()
			elif n > 0:
				# any other hit: Sound_Puff fizzle, 10 flat smoke puffs
				# (Weapon.java:344-349; the Swift's 3-puff smokePuff — Java outranks)
				if Audio.shared != null:
					Audio.shared.play("puff")
				for i in 10:
					FXSprites.smoke(position,
							Vector3((randf() - 0.5) * 10.0, 0, (randf() - 0.5) * 10.0),
							1.0, game.world)
				die()

		_:
			if position.y <= 0 and ground <= 0:
				splash()
			elif position.y <= ground:
				explode(true)


## The Dumbfire/X-Shot flight trail (Weapon.java:70-79 / 679-688): 30% smoke,
## 40% mini-fireball per frame.
func _missile_trail() -> void:
	if randf() < 0.3:
		FXSprites.smoke(position,
				Vector3((randf() - 0.5) / 3.0, 0, (randf() - 0.5) / 3.0),
				1.0, game.world)
	if randf() < 0.4:
		FXSprites.explosion1(position, Vector3.ZERO, 3.0 + randf() * 2.0, game.world)


# MARK: - Bouncer / SpikeRoller (their Java ladders keep local floor state)

## updateBouncer (Weapon.java:816-904).
func _update_bouncer(n: int, ground: float) -> void:
	var f2 := ground + 1.5   # Weapon.java:825 — the bounce floor sits at terrain+1.5
	if n == 0 or n == 5:
		if n == 5:
			# the mask-8 deck ray is the bounce floor on a standable prop
			# (Weapon.java:827-835)
			var s = game.world.standable_surface(position.x, position.y, position.z, 5)
			if s != null:
				f2 = s.height
		if n != 5 and position.y <= 2.0 and f2 <= 1.5:
			# water (Weapon.java:836: Y<=2 with the terrain at/below sea level;
			# the Swift used the shared y<=0 splash — Java outranks)
			splash()
		elif position.y <= f2:
			if Audio.shared != null:
				Audio.shared.play("bounce")           # Sound_Clang on every contact (Weapon.java:855)
			position.y = f2 + 1.0                     # Weapon.java:856
			if traj.y < 0:
				traj.y *= -0.85                       # Weapon.java:858
				# black smoke + fresh tumble + 4 chunk splats on each reflection
				# (Weapon.java:859-868; dropped by the Swift — Java outranks)
				FXSprites.smoke(position - Vector3(0, 2, 0),
						Vector3((randf() - 0.5) / 3.0, 0, (randf() - 0.5) / 3.0),
						1.0, game.world, true)
				_tumble_axis = Vector3(randf() - 0.5, randf() - 0.5, randf() - 0.5).normalized()
				_splat_chunks(2, position.y + 2.0, 0.1, 1.0, true, 4)
			if traj.y < 0.01:
				# detonate once the bounce decays (Weapon.java:870-888)
				if Audio.shared != null:
					Audio.shared.play("explosion2")
				game.world.terrain.crater(position.x, position.z, 4, 20)   # Weapon.java:876
				Projectile.crater_burst_fx(position.x, position.z, game.world)  # bl=true
				game.camera.shock(position, 70.0)
				die()
			node.position = position
	elif n >= 4:
		prop_burst()
	# codes 1/2/3: kill/collect happened inside checkForHit; the Java ladder
	# has no extra branch for them (Weapon.java:826/891)


## updateSpikeRoller (Weapon.java:153-278). The +1.9/+2.0 rest offsets are the
## clone's model-pivot compensation (macOS-verified); the Java floor math is
## terrain+1.0 with a +0.5 grind threshold.
func _update_spike_roller(n: int, ground: float) -> void:
	if n != 0 and n != 5:
		_stop_grind()   # Weapon.java:177-180
	if n == 0 or n == 5:
		var deck = game.world.standable_surface(position.x, position.y, position.z, 5) \
				if n == 5 else null
		if rolling:
			# Weapon.java:164-174 — on a STANDABLE deck the roller rides the
			# mask-8 ray height instead of the terrain (and doesn't splash
			# over water while the deck holds it up)
			if deck != null and deck.height >= ground:
				position.y = deck.height + 0.1 + 1.9
				_grind_spray()
			else:
				if ground <= 0:
					# Weapon.java:221-242 water splash (f2 <= 1.0 ⇔ terrain <= 0)
					splash()
					return
				position.y = ground + 0.1 + 1.9
				_grind_spray()
			if Audio.shared != null and not _grinding:
				_grinding = true
				Audio.shared.start_loop("grind", 0.6)   # Sound_GrindLoop (Weapon.java:250-252)
			if time_alive > 25:
				# clone failsafe: Weapon.java has no roller timeout; without it
				# an unobstructed roller never passes the turn
				explode(true)
				return
		else:
			# airborne over open water: Y <= 1.8 with the terrain at/below sea
			# level splashes directly (Weapon.java:221)
			if deck == null and position.y <= 1.8 and ground <= 0:
				splash()
				return
			# a flying roller grinds on once Y reaches the floor: deck+0.5
			# (Weapon.java:164-171 ray + :182 threshold) or terrain contact
			var floor_y := ground + 2.1   # clone pivot compensation (see header)
			if deck != null:
				floor_y = deck.height + 0.5
			if position.y <= floor_y:
				rolling = true
				traj.y = 0
				position.y = (deck.height + 2.0) if deck != null else (ground + 2.0)
				if Audio.shared != null and not _grinding:
					_grinding = true
					Audio.shared.start_loop("grind", 0.6)
	elif n >= 4:
		prop_burst()
	# codes 1/2/3: the kill happened inside checkForHit; the Java roller adds
	# nothing but the grind-loop stop (no explosion — the Swift's explode()
	# on direct hits — Java outranks)


## Grinding debris: 2x 20% CHUNKS (frames 2/3, random screen roll, no smoke)
## thrown sideways + 30% black smoke (Weapon.java:204-218). Java offsets along
## the surface Right vector; with the clone's flat-ground simplification
## Right = up × forward. (Java's Z velocity reuses Right.X — original bug, kept.)
func _grind_spray() -> void:
	var fwd := Vector3(traj.x, 0, traj.z)
	if fwd.length() < 0.001:
		return
	fwd = fwd.normalized()
	var right := Vector3(fwd.z, 0, -fwd.x)
	for i in 2:
		if randf() < 0.2:
			var f4 := (randf() - 0.5) * 2.0
			f4 = f4 + 1.0 if f4 > 0 else f4 - 1.0
			var frame := 2 if randf() < 0.5 else 3
			FXSprites.chunk(frame,
					position + right * f4 - Vector3(0, 2, 0),
					Vector3((randf() - 0.5) * 5.0 + right.x * f4 * 10.0,
							10.0 + (randf() - 0.5) * 5.0 + right.y * f4 * 10.0,
							(randf() - 0.5) * 5.0 + right.x * f4 * 10.0),
					0.25 + randf(), game.world, false, true)
	if randf() < 0.3:
		FXSprites.smoke(position - Vector3(0, 2, 0),
				Vector3((randf() - 0.5) / 3.0, 0, (randf() - 0.5) / 3.0),
				1.0, game.world, true)


func _stop_grind() -> void:
	if _grinding:
		_grinding = false
		if Audio.shared != null:
			Audio.shared.stop_loop("grind")


# MARK: - checkForHit (Weapon.java:1026-1095)

## Returns the Java hit code: 0 none, 1 cannon, 2 chest, 3 prop, 4 collideable
## mesh (shot hidden on the spot), 5 standable mesh (shot NOT hidden).
## Kills/collections are gated on Global.PROJECTILEIMPACT (G.WEAPON_IMPACT_KILL)
## exactly like the Java — the Swift collected chests / destroyed props for
## every weapon type (Java outranks).
func _check_for_hit() -> int:
	# The WTCOLLIDEABLE mesh sweep runs FIRST and short-circuits every
	# proximity test (Weapon.java:1029-1040): standable props -> code 5,
	# others -> code 4 (shot hidden on the spot).
	var mesh_prop = game.world.collide_segment(last_position, position)
	if mesh_prop != null:
		if mesh_prop.spec.standable:
			return 5
		hit_something = true
		die()                      # Weapon.java:1038-1039
		return 4
	# targetTeleport probes with widened radii (Weapon.java:314); everything
	# else uses the kill radii (Weapon.java:80/163/505/...)
	var cannon_r: float = 12.0 if type == G.Weapon.TARGET_TELEPORT else G.KILL_RADIUS_CANNON
	var chest_r: float = 10.0 if type == G.Weapon.TARGET_TELEPORT else G.KILL_RADIUS_CHEST
	var prop_pad: float = 6.0 if type == G.Weapon.TARGET_TELEPORT else G.PROP_PAD
	# cannons (Weapon.java:1042-1065) — 3D distance
	for c in game.players:
		if c != owner and c.active and not c.dying \
				and position.distance_to(c.position) < cannon_r:
			hit_something = true
			if G.WEAPON_IMPACT_KILL[type]:
				game.kill(c, {"kind": "killed", "by": owner.index})
			die()
			return 1
	# chests (Weapon.java:1066-1076)
	for chest in game.chests:
		if chest.alive and position.distance_to(chest.position) < chest_r:
			hit_something = true
			if G.WEAPON_IMPACT_KILL[type]:
				game.collect_chest(chest, owner)
			die()
			return 2
	# props (Weapon.java:1077-1092)
	var prop = game.world.prop_hit(position.x, position.y, position.z, game.game_time, prop_pad)
	if prop != null:
		hit_something = true
		die()
		if G.WEAPON_IMPACT_KILL[type]:
			game.destroy_prop(prop, owner)
		return 3
	return 0


# MARK: - Shared FX

## The n >= 4 burst-against-a-prop (updateCannonBall:544-554 and its twins in
## updateXShot/updateDumbfire/updateBouncer/updateSpikeRoller/updateTNT):
## Sound_Explosion1 + one explosion(40) + 4 smoke puffs + 3 fire-trail
## embers. No crater, no kill radius, and no miss counted.
func prop_burst() -> void:
	hit_something = true
	_burst_fx()
	die()


func _burst_fx() -> void:
	if game == null:
		return
	if Audio.shared != null:
		Audio.shared.play("explosion1")
	FXSprites.explosion1(position + Vector3(_j(), 2, _j()),
			Vector3(_j() / 3, 1 + _j() / 3, _j() / 3),
			40, game.world)
	for i in 4:
		FXSprites.smoke(position + Vector3(_j(), 2, _j()),
				Vector3(_j() / 3, 1 + _j() / 3, _j() / 3),
				1.0, game.world)
	for i in 3:
		FXSprites.fire_trail_ember(position + Vector3(_j(), 2, _j()),
				Vector3(_j() * 20, 30 + _j() * 10, _j() * 20),
				game.world)


static func _j() -> float:
	return randf() - 0.5   # (rand - 0.5)


## The 20-piece chunk splat (updateMoleHill:585-588 / updateCrater:640-643 /
## updateTargetTeleport:333-339): CHUNKS pieces sprayed at the impact point.
## Landing branches spray at ground+2 (f2+2), proximity branches at Y+2 —
## the caller passes `y`. mixed=true picks frames 2/3 per piece 50/50.
func _splat_chunks(frame: int, y: float, size_base: float, size_rand: float,
		mixed := false, count := 20) -> void:
	if game == null:
		return
	for i in count:
		var f := frame
		if mixed:
			f = 2 if randf() < 0.5 else 3
		FXSprites.chunk(f,
				Vector3(position.x + _j(), y, position.z + _j()),
				Vector3(_j() * 30, 30 + _j() * 10, _j() * 30),
				size_base + randf() * size_rand, game.world)


## Island.crater's bl=true impact plume (Island.java:559-566): one explosion(60)
## + a black 8 s smoke column sunk 4 under the surface + 7 fire-trail embers.
## Fired by the scorch craters: cannonball, dumbfire, bouncer, X-Shot center.
static func crater_burst_fx(x: float, z: float, world) -> void:
	var g: float = world.terrain.height(x, z)
	FXSprites.explosion1(Vector3(x + _j(), g + 2, z + _j()),
			Vector3(_j() / 3, 1 + _j() / 3, _j() / 3),
			60, world)
	# Particle_Object_SmokeColumn(1, x, y-4, z, false, 8): type 1 = black smoke,
	# Fire=false, 8 s (Particle_Object_SmokeColumn.java ctor)
	FXSprites.smoke_column(Vector3(x, g - 4, z), 8.0, world, false, true)
	for i in 7:
		FXSprites.fire_trail_ember(Vector3(x + _j(), g + 2, z + _j()),
				Vector3(_j() * 20, 30 + _j() * 10, _j() * 20),
				world)


## Island's quake FX: Sound_Quake + 30 delayed light rays scattered
## ±radius*0.75 around the deformation (Island.java:504-513 crater bl2 /
## :623-631 molehill / :386-394 molehillAbsolute — the offset is
## (rand-0.5)*radius*1.5; the Swift's ±45/±75 doubled it — Java outranks).
static func quake_rays_fx(x: float, z: float, radius: float, world) -> void:
	if Audio.shared != null:
		Audio.shared.play("quake")
	for i in 30:
		FXSprites.ray(x + _j() * radius * 1.5, z + _j() * radius * 1.5,
				0.5 + randf() * 2.0, true, world)


# MARK: - Ground behavior helpers (SPEC §6)

## X-Shot (Weapon.xCrater:794-814 + updateXShot:106-124): two ±45° grooves of
## length 32 (radius 4, depth 8) with the Island.groove ray+chunk trail, a
## central crater(depth 4, radius 20, bl=true), plus two ±45° kill-lines of
## length 30, width 10.
func _x_crater() -> void:
	if game == null:
		return
	var heading := atan2(traj.x, traj.z)
	for offset in [PI / 4, -PI / 4]:
		var a: float = heading + offset
		var dx := sin(a)
		var dz := cos(a)
		# terrain grooves: ±32
		var x1 := position.x - dx * 32
		var z1 := position.z - dz * 32
		var x2 := position.x + dx * 32
		var z2 := position.z + dz * 32
		game.world.terrain.groove(x1, z1, x2, z2, 4, 8)
		_groove_fx(x1, z1, x2, z2)
		# kill lines: ±30
		game.check_for_hit_line(position.x - dx * 30, position.z - dz * 30,
				position.x + dx * 30, position.z + dz * 30, 10.0, owner)
	game.world.terrain.crater(position.x, position.z, 4, 20)
	Projectile.crater_burst_fx(position.x, position.z, game.world)   # bl=true (Weapon.java:813)


## Island.groove's trench FX (Island.java:1048-1061): 20 steps along the line,
## each with an un-delayed light ray (scale 0.5+2r) and a CHUNKS frame-2/3
## piece (size 0.1+r) thrown off the fresh trench. (Dropped by the Swift —
## Java outranks.)
func _groove_fx(x1: float, z1: float, x2: float, z2: float) -> void:
	var dx := x2 - x1
	var dz := z2 - z1
	var len := Vector2(dx, dz).length()
	if len < 0.001:
		return
	var step := len / 20.0
	var dirx := dx / len
	var dirz := dz / len
	for i in 20:
		var px := x1 + dirx * i * step
		var pz := z1 + dirz * i * step
		FXSprites.ray(px, pz, 0.5 + randf() * 2.0, false, game.world)
		var frame := 2 if randf() < 0.5 else 3
		FXSprites.chunk(frame,
				Vector3(px + _j(), game.world.terrain.height(px, pz), pz + _j()),
				Vector3(_j() * 30, 30 + _j() * 10, _j() * 30),
				0.1 + randf(), game.world)


# MARK: - Death

## Water impact for the impact-weapon family (Weapon.java:86-98 etc.):
## Sound_Splash + 9 strong jets + 3 rings (Particles.splash carries the params).
func splash() -> void:
	if game == null:
		return
	if Audio.shared != null:
		Audio.shared.play("splash")
	Particles.splash(Vector3(position.x, 0.3, position.z), game.world)
	die()


## The gentle water impact used by molehill / crater / targetTeleport
## (Weapon.java:570-581 / 626-637 / 317-328): 9 jets at ((r-.5)*10,
## 15+(r-.5)*10, (r-.5)*10), fixed droplet size, same 3 rings. (The Swift used
## the strong splash for every type — Java outranks.)
func _splash_gentle() -> void:
	if game == null:
		return
	if Audio.shared != null:
		Audio.shared.play("splash")
	for i in 9:
		FXSprites.splash_jet(Vector3(position.x, 0, position.z),
				Vector3(randf_range(-5, 5), 15 + randf_range(-5, 5), randf_range(-5, 5)),
				game.world)
	FXSprites.splash_ring(Vector3(position.x, 0.11, position.z), 3, 2, game.world)
	FXSprites.splash_ring(Vector3(position.x, 0.111, position.z), 6, 1, game.world)
	FXSprites.splash_ring(Vector3(position.x, 0.1115, position.z), 4, 4, game.world)
	die()


## Generic boom used by the default branch and the roller failsafe (the Swift's
## explode(terrainHit:big:)): explosion2/3 + Particles.explosion + camera shock.
func explode(terrain_hit: bool, big := false) -> void:
	if game == null:
		return
	if Audio.shared != null:
		Audio.shared.play("explosion3" if big else "explosion2")
	Particles.explosion(position, game.world, big)
	game.camera.shock(position, 120.0 if big else 70.0)
	# terrain_hit kept for the Swift signature; callers dig their own craters
	# in the per-type ladder above
	var _th := terrain_hit
	die()


func die() -> void:
	if not alive:
		return
	alive = false
	_stop_grind()
	if node != null:
		if node.get_parent() != null:
			node.get_parent().remove_child(node)
		node.queue_free()
	if _shadow_blob != null:
		if _shadow_blob.get_parent() != null:
			_shadow_blob.get_parent().remove_child(_shadow_blob)
		_shadow_blob.queue_free()
		_shadow_blob = null
	if not hit_something and _COUNTS_MISS[type]:
		owner.misses += 1
	if game != null:
		game.projectile_died(self, position)
