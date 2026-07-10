class_name SkyActor
extends Node3D
# Port of macos/Sources/Cannonballs/SkyActor.swift.
#
# The ORIGINAL sky actors (SKIES/<NAME>/actor.wsgo): the sky dome mesh with the
# horizon-island billboards (+ moon / star layer / outcrops) at their authored
# positions. Exported by tools/export_skies.py to MODELS/SKY/<NAME>.json.
#
# The dome is ~51 units in geom space; we scale it to sit inside the fog start
# distance and follow the camera position (the WT engine attached it to the
# camera environment), rendering with no depth write — a true skybox.
#
# GDScript port notes (windows/PORTING.md):
# - The macOS build repositions the sky from GameController.update
#   (GameController.swift:428-432; Camera.java:583-586: the WT environment
#   follows the camera's FULL position, including Y). Here the node follows
#   the active camera itself in _process — the same mechanism, self-driving,
#   and it works before the game-controller port lands.
# - renderingOrder -300+index → material render_priority -128+index (Godot's
#   floor is -128). ALL parts render in the transparent pass (alpha
#   transparency + no depth write), so priority pins the authored part order
#   within the sky and below the water/shoreline/effects priorities.
# - readsFromDepthBuffer = false is NOT ported: SceneKit drew the sky first
#   and let everything paint over it; Godot draws the (transparent) sky after
#   the opaques, so the ordinary depth test against the opaque terrain
#   produces the same visible result.


func _process(_dt: float) -> void:
	var cam := get_viewport().get_camera_3d()
	if cam != null:
		global_position = cam.global_position


static func load_sky(sky_name: String, scale_factor: float = 12.0) -> SkyActor:
	var data := Assets.data("MODELS/SKY/%s.json" % sky_name)
	if data.is_empty():
		return null
	var doc = JSON.parse_string(data.get_string_from_utf8())
	if not (doc is Dictionary) or not (doc.get("parts") is Array):
		return null
	var root := SkyActor.new()
	root.name = "sky-actor"
	var index := 0
	for part in doc["parts"]:
		var verts: Array = part["verts"]
		var part_uvs: Array = part["uvs"]
		var tris: Array = part["tris"]
		@warning_ignore("integer_division")
		var v_count := verts.size() / 3
		if v_count == 0:
			continue
		var positions := PackedVector3Array()
		positions.resize(v_count)
		var uvs := PackedVector2Array()
		uvs.resize(v_count)
		for v in v_count:
			positions[v] = Vector3(verts[v * 3], verts[v * 3 + 1], verts[v * 3 + 2])
			# raw WT texcoords sample v-down from the top-left in SceneKit and
			# Godot alike — no flip (skinned_model.gd)
			uvs[v] = Vector2(part_uvs[v * 2], part_uvs[v * 2 + 1])
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
		var arrays := []
		arrays.resize(Mesh.ARRAY_MAX)
		arrays[Mesh.ARRAY_VERTEX] = positions
		arrays[Mesh.ARRAY_TEX_UV] = uvs
		arrays[Mesh.ARRAY_INDEX] = indices
		var mesh := ArrayMesh.new()
		mesh.add_surface_from_arrays(Mesh.PRIMITIVE_TRIANGLES, arrays)

		var m := StandardMaterial3D.new()
		m.albedo_texture = Assets.texture("MODELS/SKY/textures/%s" % part["texture"])
		m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED     # lightingModel = .constant
		m.cull_mode = BaseMaterial3D.CULL_DISABLED                # isDoubleSided = true
		m.depth_draw_mode = BaseMaterial3D.DEPTH_DRAW_DISABLED    # writesToDepthBuffer = false
		# plain alpha blend for the cutout billboards — the 2002 engine
		# alpha-blended these (transparencyMode .aOne, no alpha-test prepass);
		# the opaque parts also route through alpha so priority ordering holds
		# (see the header note).
		m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
		if part["name"] == "skytop":
			m.blend_mode = BaseMaterial3D.BLEND_MODE_ADD   # star layer adds over the dome
		# The sky draws first as one unit (Island.java:1213 setOption(0,-100),
		# far below the game's option numbers). WITHIN it, layer by the wsgo's
		# authored part order (NIGHT: skydome2, skytop, isle1, moon, isle2) —
		# pinned by data, not by the renderer's distance sort, so the moon can
		# never paint over an island cutout.
		m.render_priority = -128 + index
		mesh.surface_set_material(0, m)

		var n := MeshInstance3D.new()
		n.name = "sky-%s" % part["name"]
		n.mesh = mesh
		n.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
		root.add_child(n)
		index += 1
	root.scale = Vector3(scale_factor, scale_factor, scale_factor)
	return root
