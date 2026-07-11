class_name CameraController
extends RefCounted
# Port of macos/Sources/Cannonballs/CameraController.swift.
# Camera modes per SPEC §3 table.


enum Mode {
	CANNON = 0, SHOT, MEDIUM, HIGH, BARREL,
	# views the player can pick — the originals 0..4 (Camera.java CameraViews = 5)
	SUCCESS,   # view 6: end-of-game orbit around the winner (Camera.java:97)
	SPECTATOR, # view 99: follow whoever is playing (Camera.java:284)
}

const MODE_LABELS := ["Cannon", "Shot", "Medium", "High", "Barrel", "Success", "Spectator"]

## The cycle/menu set (cycleCamera wraps at CameraViews=5; 6/99 are set
## only by the game-over and death flows).
const SELECTABLE := [Mode.CANNON, Mode.SHOT, Mode.MEDIUM, Mode.HIGH, Mode.BARREL]


var node := Camera3D.new()
var mode: int = Mode.SHOT
var game = null   # GameController — untyped until src/game_controller.gd lands
                  # (PORTING.md); the Swift's `weak` drops (Godot Nodes aren't
                  # refcounted, no retain cycle through the scene tree)

var _shock_amplitude := 0.0
var _chase_speed := 0.1
var _initialized := false
var _success_angle := 0.0   # Camera.SuccessAngle (degrees)

var _wind_arrow: Node3D = null


func _init() -> void:
	# SCNCamera zFar 4000, fieldOfView 55 (vertical — matches Camera3D.fov,
	# PORTING.md)
	node.far = 4000
	node.fov = 55


func attach_to(p_game) -> void:
	game = p_game
	p_game.world.root.add_child(node)   # scene.rootNode → World.root

	# Wind indicator: the golden ornate arrow (arrowo.png) on a flat quad lying
	# horizontal at the lower-right of the view, spun around world-Y by the wind
	# direction (ArrowGroup setAbsoluteOrientation(0,1,0, WindDirection)). Child of
	# the camera for position, world-oriented for direction → true 3D weathervane.
	# the REAL decoded ARROW mesh (fixed by the solved-format exporter; the old
	# OBJ route mis-assigned its material and rendered black)
	var actor := SkinnedModel.load_actor("ARROW")
	if actor != null:
		# renderingOrder is per-node (not inherited): set it on every mesh
		# node, above WorldDressing's 1000, so with depth reads off the
		# arrow always draws in front of the world.
		_overlay_materials(actor.root)
		# arrow mesh spans ~7 units; scale to HUD weathervane size
		actor.root.scale = Vector3(0.78, 0.78, 0.78)   # a bit bigger than the 0.62 first pass
		# decoded WT meshes rest facing -Z (the barrel gets the same PI yaw in
		# Cannon.buildParts); face +Z so R_y(WindDirection) points the tip along
		# the wind drift, matching the original engine's setAbsoluteOrientation.
		actor.root.quaternion = Quaternion(Vector3(0, 1, 0), PI)
		var holder := Node3D.new()
		holder.add_child(actor.root)
		holder.position = Vector3(2.6, -1.4, -5)         # lower-right, in front (camera -Z)
		node.add_child(holder)
		_wind_arrow = holder
	else:
		var gold := Assets.texture("MODELS/ARROW/textures/arrowo_keyed.png")
		if gold != null:
			var plane := QuadMesh.new()                    # SCNPlane (faces +Z)
			plane.size = Vector2(3.25, 1.625)              # 256x128 = 2:1, sized to match the mesh arrow
			var m := StandardMaterial3D.new()
			m.albedo_texture = gold
			# magnificationFilter .nearest (PORTING.md)
			m.texture_filter = BaseMaterial3D.TEXTURE_FILTER_NEAREST_WITH_MIPMAPS
			m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED   # .constant
			m.cull_mode = BaseMaterial3D.CULL_DISABLED              # isDoubleSided
			m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA      # .aOne — black bg keyed to alpha
			m.depth_draw_mode = BaseMaterial3D.DEPTH_DRAW_DISABLED  # writesToDepthBuffer = false
			m.no_depth_test = true                                  # readsFromDepthBuffer = false
			m.render_priority = 127   # renderingOrder 2000 → always in front (see mesh path)
			plane.material = m
			var card := MeshInstance3D.new()
			card.mesh = plane
			card.rotation.x = -PI / 2                      # lie flat (weathervane)
			var holder := Node3D.new()
			holder.add_child(card)
			holder.position = Vector3(2.6, -1.4, -5)       # lower-right, in front (camera -Z)
			node.add_child(holder)
			_wind_arrow = holder


## The Swift's enumerateHierarchy over the ARROW actor: renderingOrder 2000 +
## unlit + nearest + depth read/write off on every mesh node's materials.
## Godot render_priority caps at 127 (vs SceneKit's free integer 2000) and only
## orders the transparent pass, so the materials also go TRANSPARENCY_ALPHA;
## with no_depth_test that reproduces "always drawn in front of the world".
func _overlay_materials(n: Node3D) -> void:
	if n is MeshInstance3D and n.mesh != null:
		for s in n.mesh.get_surface_count():
			var m = n.mesh.surface_get_material(s)
			if m is StandardMaterial3D:
				m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED   # .constant
				m.texture_filter = BaseMaterial3D.TEXTURE_FILTER_NEAREST_WITH_MIPMAPS
				m.depth_draw_mode = BaseMaterial3D.DEPTH_DRAW_DISABLED  # writesToDepthBuffer = false
				m.no_depth_test = true                                  # readsFromDepthBuffer = false
				m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
				m.render_priority = 127
	for child in n.get_children():
		_overlay_materials(child)


func cycle() -> void:
	if mode == Mode.SUCCESS or mode == Mode.SPECTATOR:
		return   # no cycling once dead/won
	mode = (mode + 1) % SELECTABLE.size()
	if game != null and game.hud != null:
		game.hud.flash_message("Camera: %s" % MODE_LABELS[mode])


## setCamera(6, ...) — the end-of-game orbit. applyCameraView case 6 resets
## SuccessAngle to 0 (Camera.java:97-99).
func set_success_mode() -> void:
	mode = Mode.SUCCESS
	_success_angle = 0


## setSpectatorCamera (Camera.java:278-290): announces itself and switches to
## view 99. The HUD side (bar removal, "Spectator Mode" labels) is
## HUDScene.enterSpectatorMode().
func set_spectator_mode() -> void:
	mode = Mode.SPECTATOR
	if game != null and game.hud != null:
		game.hud.flash_message("Activating Spectator Camera")   # Camera.java:279


func shock(at_pos: Vector3, radius: float) -> void:
	var d := node.position.distance_to(at_pos)
	if d < radius:
		_shock_amplitude = maxf(_shock_amplitude, (radius - d) * 100 / radius / 1000 * 8)


func update(dt: float) -> void:
	if game == null:
		return
	var focus = game.camera_focus_cannon
	if focus == null:
		focus = game.players[0]   # players.first! — empty roster is a crash in the Swift too
	var terrain = game.world.terrain

	var desired: Vector3
	var look_target: Vector3

	# live shot chase (Shot mode; spectator view 99 is updateShotCamera on the
	# current player — Camera.java:379-381 — so it chases shots the same way)
	var proj = null
	if mode == Mode.SHOT or mode == Mode.SPECTATOR:
		for p in game.projectiles:
			if p.alive and p.owner.fired_on_turn:
				proj = p
				break
		if proj == null:
			for p in game.projectiles:
				if p.alive:
					proj = p
					break
	if proj != null:
		_chase_speed = minf(7.0, _chase_speed + dt * 3)
		var back: Vector3 = proj.traj.normalized() if proj.traj.length() > 0.1 else Vector3(0, 0, 1)
		desired = proj.position - back * 10 + Vector3(0, 4, 0)
		look_target = proj.position
	else:
		_chase_speed = 0.1
		var yaw: float = deg_to_rad(focus.spin_angle)   # G.deg2rad (Types.swift:26)
		var rot := func(v: Vector3) -> Vector3:
			return Vector3(v.x * cos(yaw) + v.z * sin(yaw), v.y, -v.x * sin(yaw) + v.z * cos(yaw))
		match mode:
			Mode.CANNON, Mode.SHOT, Mode.SPECTATOR:
				desired = focus.position + rot.call(Vector3(0, 6, -20))
				look_target = focus.position + rot.call(Vector3(0, 4, 30))
			Mode.MEDIUM:
				desired = focus.position + rot.call(Vector3(0, 50, -50))
				look_target = focus.position
			Mode.HIGH:
				desired = focus.position + rot.call(Vector3(0, 100, -100))
				look_target = focus.position
			Mode.BARREL:
				desired = focus.muzzle_position + Vector3(0, 1.5, 0)
				look_target = focus.muzzle_position + focus.fire_direction * 40
			Mode.SUCCESS:
				# updateSuccessCamera (Camera.java:292-342): offset (0,10,-60)
				# swung around Y by SuccessAngle (+10 deg/s), aimed at the first
				# Active cannon (the winner). Chase (9.5·dt) and the terrain+4 /
				# min-8 floor reuse the shared clamps below, same constants.
				var winner = null
				for p in game.players:
					if p.active:
						winner = p
						break
				if winner == null:
					winner = focus
				var a: float = deg_to_rad(_success_angle)   # G.deg2rad
				_success_angle += dt * 10
				# VEC3D.rotateY: x' = x·cos + z·sin, z' = -x·sin + z·cos
				var off := Vector3(0, 10, -60)
				desired = winner.position + Vector3(off.x * cos(a) + off.z * sin(a), off.y,
						-off.x * sin(a) + off.z * cos(a))
				look_target = winner.position

	# floor clamps
	var min_y: float = maxf(terrain.height(desired.x, desired.z) + 4, 8)
	if desired.y < min_y and mode != Mode.BARREL:
		desired.y = min_y

	if not _initialized:
		node.position = desired
		_initialized = true
	else:
		var k: float = minf(1, (maxf(_chase_speed, 9.5) if mode == Mode.SHOT else 9.5) * dt)
		node.position += (desired - node.position) * k

	# keep the interpolated camera out of the terrain too
	if mode != Mode.BARREL:
		var floor_y: float = maxf(terrain.height(node.position.x, node.position.z) + 4, 8)
		if node.position.y < floor_y:
			node.position.y = floor_y

	# shock rattle
	if _shock_amplitude > 0.0005:
		node.position += Vector3(randf_range(-1, 1), randf_range(-1, 1), randf_range(-1, 1)) * _shock_amplitude
		_shock_amplitude *= pow(0.9, dt * 60)
	else:
		_shock_amplitude = 0

	# Stable orientation: force world-up so the view never rolls sideways or
	# flips upside down. Guard the degenerate near-vertical case (gimbal).
	var fwd := look_target - node.position
	if fwd.length() < 1e-4:
		fwd = Vector3(0, 0, 1)
	fwd = fwd.normalized()
	var up := Vector3(0, 1, 0)
	if absf(fwd.dot(up)) > 0.995:
		up = Vector3(0, 0, 1)
	# node.look(at:up:localFront:(0,0,-1)) → look_at (Node3D -Z is the same
	# local front; position was set above). Aim along the GUARDED fwd so the
	# degenerate look_target == position case stays safe.
	node.look_at(node.position + fwd, up)

	# Point the wind arrow in the world wind direction, regardless of camera yaw
	# (setAbsoluteOrientation(0,1,0, WindDirection)). Absolute world orientation.
	if _wind_arrow != null:
		var dir: float = deg_to_rad(game.wind_direction)   # G.deg2rad
		_wind_arrow.global_basis = Basis(Quaternion(Vector3(0, 1, 0), dir))


## Set a specific camera mode (used by the Camera menu). Inert once the
## spectator/success cameras own the view (the original removes the Camera
## menu in spectator mode and stops UI processing in the end state).
func set_mode(m: int) -> void:
	if mode == Mode.SUCCESS or mode == Mode.SPECTATOR:
		return
	mode = m
	if game != null and game.hud != null:
		game.hud.flash_message("Camera: %s" % MODE_LABELS[m])
