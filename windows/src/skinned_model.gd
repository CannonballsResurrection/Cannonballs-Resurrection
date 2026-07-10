class_name SkinnedModel
# Port of macos/Sources/Cannonballs/SkinnedModel.swift.
#
# Loads skinned WildTangent actors decoded by tools/wsgo_export_skinned.py
# (MODELS/<NAME>/skinned.json) and their motions (tools/wsmo_decode.py JSON).
#
# The original engine skins with a matrix palette: each vertex carries up to
# 2 (bone, weight/255) influences; bones form a hierarchy of "chunks" whose
# inverse-bind matrices ship in the geom header. Motions are per-bone gePaths
# (translation vec3 + rotation quat WXYZ) that compose AFTER the bind-local
# transform (Genesis3D pose.c: World = ParentWorld × Attachment × MotionSample).
# We mirror that exactly: the Swift's bindNode (static bind-local) → motionNode
# (animated, identity at rest) pair collapses into one Skeleton3D bone whose
# rest = bind-local and whose pose = bind-local × motion sample — the composed
# chain is identical (root → B_parent·M_parent → B_child·M_child).
#
# SCNSkinner → ArrayMesh ARRAY_BONES/ARRAY_WEIGHTS (4 influences, zero-padded)
# + Skeleton3D + Skin (PORTING.md). CAKeyframeAnimation → per-frame math:
# callers drive Actor.update(dt) each frame (PORTING.md).


class Motion:
	class Track:
		var times: Array          # of float
		var values: Array         # of Array (vec3 or quat WXYZ)

		func _init(p_times: Array, p_values: Array) -> void:
			times = p_times
			values = p_values

	var duration: float
	var translation := {}         # bone name -> Track
	var rotation := {}            # bone name -> Track (WXYZ)


## A built skinned actor: root node plus the skeleton bones to drive
## (the Swift's motionNodes dictionary becomes bone_index name → bone idx).
class Actor:
	var root: Node3D
	var skeleton: Skeleton3D
	var doc: Dictionary
	var bone_index := {}          # bone name -> Skeleton3D bone index
	var bind_locals: Array = []   # Transform3D per bone (the bindNode transforms)

	# CAKeyframeAnimation playback state (per-frame math, PORTING.md). The
	# motionNode identity-at-rest state lives in _motion_pos/_motion_rot.
	var _loop_motion: Motion = null
	var _loop_time := 0.0
	var _once_motion: Motion = null
	var _once_time := 0.0
	var _motion_pos := {}         # bone name -> Vector3 (motionNode.position)
	var _motion_rot := {}         # bone name -> Quaternion (motionNode.orientation)

	func _init(p_root: Node3D, p_skeleton: Skeleton3D, p_doc: Dictionary) -> void:
		root = p_root
		skeleton = p_skeleton
		doc = p_doc

	## Advance any attached motions. The macOS build's CAKeyframeAnimations run
	## on the render clock; here the owner calls update(dt) every frame.
	func update(dt: float) -> void:
		if _once_motion != null:
			_once_time += dt
			if _once_time >= _once_motion.duration:
				# isRemovedOnCompletion defaults true for the playOnce
				# animations: snap back to rest (model values).
				_once_motion = null
				_reset_to_rest()
			else:
				# while a fire animation runs it owns the bones (the Swift
				# adds it under its own key on top of any loop; last-added
				# wins per keyPath, which is the play-once animation)
				SkinnedModel.pose(self, _once_motion, _once_time)
				return
		if _loop_motion != null:
			_loop_time += dt
			SkinnedModel.pose(self, _loop_motion, fmod(_loop_time, _loop_motion.duration))

	func _reset_to_rest() -> void:
		_motion_pos.clear()
		_motion_rot.clear()
		skeleton.reset_bone_poses()   # rest = bind-local = motion identity


static func load_actor(name: String, file: String = "skinned.json") -> Actor:
	var data := Assets.data("MODELS/%s/%s" % [name, file])
	if data.is_empty():
		return null
	var doc = JSON.parse_string(data.get_string_from_utf8())
	if not (doc is Dictionary) or not doc.has("bones"):
		return null

	var root := Node3D.new()
	root.name = "skinned-%s" % name

	# Bone hierarchy: bindNode (bind-local transform) → motionNode (identity).
	# One Skeleton3D bone per doc bone: rest = bind-local (see header comment).
	var skeleton := Skeleton3D.new()
	skeleton.name = "skeleton"
	root.add_child(skeleton)
	var actor := Actor.new(root, skeleton, doc)
	var bones: Array = doc["bones"]
	for bone in bones:
		var idx := skeleton.add_bone(bone["name"])
		var bind := _transform_row_major_column_vector(bone["bindLocal"])
		skeleton.set_bone_rest(idx, bind)
		actor.bind_locals.append(bind)
		actor.bone_index[bone["name"]] = idx
	for i in bones.size():
		var parent := int(bones[i]["parent"])
		if parent >= 0:
			skeleton.set_bone_parent(i, parent)
	skeleton.reset_bone_poses()

	# Skin: the geom header's inverse world-bind matrix per chunk.
	var skin := Skin.new()
	for bone in bones:
		skin.add_bind(actor.bone_index[bone["name"]],
				_transform_row_major_column_vector(bone["invBind"]))

	# Geometry: one skinned mesh node per part (parts share the skeleton).
	for part in _all_parts(doc):
		var verts: Array = part["verts"]
		var v_count := verts.size() / 3
		if v_count == 0:
			continue
		var positions := PackedVector3Array()
		positions.resize(v_count)
		var normals := PackedVector3Array()
		normals.resize(v_count)
		var uvs := PackedVector2Array()
		uvs.resize(v_count)
		var part_normals: Array = part["normals"]
		var part_uvs: Array = part["uvs"]
		for v in v_count:
			positions[v] = Vector3(verts[v * 3], verts[v * 3 + 1], verts[v * 3 + 2])
			normals[v] = Vector3(part_normals[v * 3], part_normals[v * 3 + 1], part_normals[v * 3 + 2])
			# skinned.json carries the raw WT texcoords; SceneKit and Godot
			# both sample them v-down from the top-left, so no flip (the OBJ
			# route is the one that flips — see model_library.gd)
			uvs[v] = Vector2(part_uvs[v * 2], part_uvs[v * 2 + 1])

		var tris: Array = part["tris"]
		var indices := PackedInt32Array()
		indices.resize(tris.size())
		var w := 0
		for t in range(0, tris.size(), 3):
			# SceneKit front faces are CCW, Godot's are CW (PORTING.md):
			# swap the second/third index of every triangle.
			indices[w] = int(tris[t])
			indices[w + 1] = int(tris[t + 2])
			indices[w + 2] = int(tris[t + 1])
			w += 3

		# SCNSkinner bone weights/indices → ARRAY_BONES/ARRAY_WEIGHTS,
		# 4 influences per vertex, zero-padded (PORTING.md).
		var per := int(part["influencesPerVertex"]) if part.get("influencesPerVertex") != null else 2
		var skin_indices: Array = part["skinIndices"]
		var skin_weights: Array = part["skinWeights"]
		var bone_ids := PackedInt32Array()
		bone_ids.resize(v_count * 4)
		var weights := PackedFloat32Array()
		weights.resize(v_count * 4)
		for v in v_count:
			for k in 4:
				if k < per:
					bone_ids[v * 4 + k] = int(skin_indices[v * per + k])
					weights[v * 4 + k] = skin_weights[v * per + k]
				else:
					bone_ids[v * 4 + k] = 0
					weights[v * 4 + k] = 0.0

		var arrays := []
		arrays.resize(Mesh.ARRAY_MAX)
		arrays[Mesh.ARRAY_VERTEX] = positions
		arrays[Mesh.ARRAY_NORMAL] = normals
		arrays[Mesh.ARRAY_TEX_UV] = uvs
		arrays[Mesh.ARRAY_BONES] = bone_ids
		arrays[Mesh.ARRAY_WEIGHTS] = weights
		arrays[Mesh.ARRAY_INDEX] = indices
		var mesh := ArrayMesh.new()
		mesh.add_surface_from_arrays(Mesh.PRIMITIVE_TRIANGLES, arrays)

		var mat := StandardMaterial3D.new()
		var tex := Assets.texture("MODELS/%s/textures/%s" % [name, part["texture"]])
		if tex != null:
			mat.albedo_texture = tex
			# magnificationFilter = .nearest (PORTING.md); WITH_MIPMAPS keeps
			# SceneKit's default mipmapped minification
			mat.texture_filter = BaseMaterial3D.TEXTURE_FILTER_NEAREST_WITH_MIPMAPS
		# lightingModel = .lambert → shaded + metallic_specular 0, roughness 1
		# (PORTING.md); metallic_specular 0 also covers specular = black.
		# NOT PORTED: ambient.contents = 0.3 white — Godot StandardMaterial3D
		# has no per-material ambient reflectance knob (deWash, see
		# model_library.gd for the same omission).
		mat.roughness = 1.0
		mat.metallic_specular = 0.0
		if part.get("keyed") == true:
			mat.cull_mode = BaseMaterial3D.CULL_DISABLED   # isDoubleSided
			# transparencyMode .aOne + surface-modifier discard at a < 0.5
			# → alpha scissor at 0.5 (writes + reads depth, like the Swift)
			mat.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA_SCISSOR
			mat.alpha_scissor_threshold = 0.5
		mesh.surface_set_material(0, mat)

		var mesh_node := MeshInstance3D.new()
		mesh_node.name = "mesh-%s" % name
		mesh_node.mesh = mesh
		mesh_node.skin = skin
		skeleton.add_child(mesh_node)
		mesh_node.skeleton = NodePath("..")   # skinner.skeleton
	return actor


# MARK: - Motion

static func load_motion(path: String) -> Motion:
	var data := Assets.data(path)
	if data.is_empty():
		return null
	var j = JSON.parse_string(data.get_string_from_utf8())
	if not (j is Dictionary) or not (j.get("duration") is float) or not (j.get("bones") is Dictionary):
		return null
	var motion := Motion.new()
	motion.duration = j["duration"]
	for bone in j["bones"]:
		var b = j["bones"][bone]
		if not (b is Dictionary):
			continue
		var t = b.get("translation")
		if t is Dictionary and t.get("times") is Array and t.get("values") is Array:
			motion.translation[bone] = Motion.Track.new(t["times"], t["values"])
		var r = b.get("rotation")
		if r is Dictionary and r.get("times") is Array and r.get("values") is Array:
			var vals: Array = r["values"].duplicate(true)
			# enforce quaternion sign continuity so keyframe lerp takes the short arc
			for k in range(1, vals.size()):
				var dot := 0.0
				for c in 4:
					dot += vals[k][c] * vals[k - 1][c]
				if dot < 0:
					for c in 4:
						vals[k][c] = -vals[k][c]
			motion.rotation[bone] = Motion.Track.new(r["times"], vals)
	return motion


## Attach looping keyframe animations for the motion. One shared phase offset
## keeps all bone tracks in sync (the original starts at a random phase).
## (CAKeyframeAnimation repeat ∞ / timeOffset = phase → per-frame Actor.update.)
static func animate(actor: Actor, motion: Motion, phase: float) -> void:
	actor._loop_motion = motion
	actor._loop_time = phase


## Play the motion once (e.g. the cannon's fire recoil), then snap back to rest.
static func play_once(actor: Actor, motion: Motion) -> void:
	actor._once_motion = motion
	actor._once_time = 0.0


## Statically pose the actor at motion time t (for offscreen verification renders).
static func pose(actor: Actor, motion: Motion, at_t: float) -> void:
	for bone in actor.bone_index:
		var moved := false
		if motion.translation.has(bone):
			var v: Array = _sample(motion.translation[bone], at_t)
			actor._motion_pos[bone] = Vector3(v[0], v[1], v[2])
			moved = true
		if motion.rotation.has(bone):
			# duplicate: _sample can return a track keyframe by REFERENCE
			# (GDScript Arrays alias, unlike Swift's value arrays) and the
			# normalize below must not corrupt the motion data
			var q: Array = _sample(motion.rotation[bone], at_t).duplicate()
			var n := sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3])
			if n > 0:
				for c in 4:
					q[c] = q[c] / n
			# stored WXYZ; Godot Quaternion is (x, y, z, w) like SceneKit
			actor._motion_rot[bone] = Quaternion(q[1], q[2], q[3], q[0])
			moved = true
		if moved:
			_apply_bone(actor, bone)


## Linear keyframe sample, clamped to the ends (the Swift pose() sampler).
static func _sample(track: Motion.Track, t: float) -> Array:
	var times: Array = track.times
	var vals: Array = track.values
	if t <= times[0]:
		return vals[0]
	for k in times.size() - 1:
		if times[k] <= t and t <= times[k + 1]:
			var a: float = (t - times[k]) / maxf(times[k + 1] - times[k], 1e-9)
			var out := []
			for c in vals[k].size():
				out.append((1 - a) * vals[k][c] + a * vals[k + 1][c])
			return out
	return vals[vals.size() - 1]


## Bone pose = bind-local × motion sample (bindNode → motionNode composition;
## motionNode transform = T(position) · R(orientation), rotation about its own
## origin then translation in bind space — SCNNode transform order).
static func _apply_bone(actor: Actor, bone: String) -> void:
	var idx: int = actor.bone_index[bone]
	var p: Vector3 = actor._motion_pos.get(bone, Vector3.ZERO)
	var q: Quaternion = actor._motion_rot.get(bone, Quaternion.IDENTITY)
	actor.skeleton.set_bone_pose(idx, actor.bind_locals[idx] * Transform3D(Basis(q), p))


## Row-major column-vector-convention 4x4 (our tools' output) -> Transform3D.
## Godot is column-vector convention like the tools, so unlike the Swift
## (which transposes into SceneKit's row-vector SCNMatrix4) NO transpose:
## input rows are the matrix rows; Basis takes the three COLUMNS.
static func _transform_row_major_column_vector(t: Array) -> Transform3D:
	return Transform3D(
			Basis(Vector3(t[0], t[4], t[8]),
				Vector3(t[1], t[5], t[9]),
				Vector3(t[2], t[6], t[10])),
			Vector3(t[3], t[7], t[11]))


## Normalized part list (legacy single-mesh docs become one part).
static func _all_parts(doc: Dictionary) -> Array:
	if doc.get("parts") is Array:
		return doc["parts"]
	return [{
		"texture": doc.get("texture", ""),
		"keyed": false,
		"influencesPerVertex": doc.get("influencesPerVertex"),
		"verts": doc.get("verts", []),
		"normals": doc.get("normals", []),
		"uvs": doc.get("uvs", []),
		"tris": doc.get("tris", []),
		"skinIndices": doc.get("skinIndices", []),
		"skinWeights": doc.get("skinWeights", []),
	}]
