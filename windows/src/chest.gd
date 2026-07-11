class_name Chest
extends RefCounted
# Port of macos/Sources/Cannonballs/Chest.swift.
# Treasure chest (SPEC §5): contents rolled when collected.


var position: Vector3
var alive := true
var node: Node3D


func _init(p_position: Vector3) -> void:
	position = p_position
	node = Chest.make_node()
	node.position = position


## Rest height on the terrain. Chest.java settled the chest upward out of the
## ground via a collision check after placing it; sampling the footprint's
## highest point does the same job — on a slope the downhill edge can no
## longer bury (the radius-6 collider ≈ ±4 visual footprint).
static func rest_height(terrain: Terrain, x: float, z: float) -> float:
	var y := terrain.height(x, z)
	for off in [Vector2(4, 0), Vector2(-4, 0), Vector2(0, 4), Vector2(0, -4)]:
		y = maxf(y, terrain.height(x + off.x, z + off.y))
	return y


func reground(terrain: Terrain) -> void:
	position.y = Chest.rest_height(terrain, position.x, position.z)
	node.position = position


static func make_node() -> Node3D:
	var root := Node3D.new()
	root.name = "chest"
	# The original chest: decoded CHEST actor with its full DECODED skeleton
	# (7 bones, matrix-palette skin from actor.wsgo) playing the original
	# "loop" hop motion — all 6 bone paths (root hop translation + lid/body
	# rotation rattle) from resources/loop.wsmo. See MOTION_FORMAT.md and
	# GEOM_MESH_FORMAT_SOLVED.md; exported by tools/wsgo_export_skinned.py.
	var actor := SkinnedModel.load_actor("CHEST")
	var motion := SkinnedModel.load_motion("MODELS/CHEST/loop_motion_full.json")
	if actor != null and motion != null:
		Props.style_cannon_materials(actor.root)   # solid, crisp — no ghosting
		# NATIVE scale: the original loads the CHEST actor unscaled (Chest.java,
		# Radius=6 collision matches the ~7.6-unit mesh). The old x7 was wrong.
		# original starts each chest at a random phase (playMotion random offset)
		SkinnedModel.animate(actor, motion, randf_range(0.0, motion.duration))
		# the owner drives Actor.update per frame (PORTING.md): the chest is
		# self-owned here, so a self-driving wrapper advances the loop
		var driver := Props.Animated.new()
		driver.tick = func(_n: Node3D, dt: float) -> void:
			actor.update(dt)
		driver.add_child(actor.root)
		root.add_child(driver)
		var blob := FXSprites.blob_shadow(5)   # original SHADOW patch (Chest.java:138)
		blob.position.y = 0.15
		root.add_child(blob)
		return root
	var model := ModelLibrary.node_for("CHEST")
	if model != null:
		# fallback: static decoded mesh + root-translation hop only
		Props.style_cannon_materials(model)
		var bob := Props.Animated.new()
		bob.add_child(model)
		root.add_child(bob)
		var data := Assets.data("MODELS/CHEST/loop_motion.json")
		if not data.is_empty():
			var j = JSON.parse_string(data.get_string_from_utf8())
			if j is Dictionary and j.get("times") is Array and j.get("height") is Array \
					and j.get("duration") is float and j["duration"] > 0 \
					and (j["times"] as Array).size() == (j["height"] as Array).size():
				var times: Array = j["times"]
				var heights: Array = j["height"]
				var duration: float = j["duration"]
				# CAKeyframeAnimation position.y, linear, repeat ∞,
				# timeOffset = random 0..duration → per-frame sampling
				var s := {"t": randf_range(0.0, duration)}
				bob.tick = func(n: Node3D, dt: float) -> void:
					s.t = fmod(s.t + dt, duration)
					n.position.y = Chest._sample_track(times, heights, s.t)
		return root
	# last-resort procedural stand-in
	var front := Props.PropGeometry.tex_material("CHEST/textures/chesto.jpg",
			[0.0, 0.0, 0.5, 0.5], false, Color(0.7, 0.14, 0.1))
	var lid_tex := Props.PropGeometry.tex_material("CHEST/textures/chesto.jpg",
			[0.0, 0.5, 0.5, 0.5], false, Color(0.85, 0.7, 0.2))
	var body_mesh := BoxMesh.new()
	body_mesh.size = Vector3(6, 3.6, 4)
	body_mesh.material = front
	var body := MeshInstance3D.new()
	body.mesh = body_mesh
	body.position = Vector3(0, 1.8, 0)
	root.add_child(body)
	var lid_mesh := BoxMesh.new()
	lid_mesh.size = Vector3(6, 1.8, 4)
	lid_mesh.material = lid_tex
	var lid := MeshInstance3D.new()
	lid.mesh = lid_mesh
	lid.position = Vector3(0, 4.2, 0)
	root.add_child(lid)
	return root


## Linear keyframe sample, clamped to the ends (CAKeyframeAnimation .linear).
static func _sample_track(times: Array, values: Array, t: float) -> float:
	if t <= times[0]:
		return values[0]
	for k in times.size() - 1:
		if times[k] <= t and t <= times[k + 1]:
			var a: float = (t - times[k]) / maxf(times[k + 1] - times[k], 1e-9)
			return (1 - a) * values[k] + a * values[k + 1]
	return values[values.size() - 1]


## Roll contents: 30% 100g, 30% 250g, 20% 500g, 10% 1000g, 5% 1500g, 5%
## teleport shooter. (The Swift's `enum Treasure { gold(Int); teleport }`
## becomes {"kind": "gold", "amount": n} or {"kind": "teleport"}.)
static func roll_treasure() -> Dictionary:
	var r := randf()
	if r < 0.30:
		return {"kind": "gold", "amount": 100}
	if r < 0.60:
		return {"kind": "gold", "amount": 250}
	if r < 0.80:
		return {"kind": "gold", "amount": 500}
	if r < 0.90:
		return {"kind": "gold", "amount": 1000}
	if r < 0.95:
		return {"kind": "gold", "amount": 1500}
	return {"kind": "teleport"}
