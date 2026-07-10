class_name ModelLibrary
# Port of macos/Sources/Cannonballs/ModelLibrary.swift.
#
# Loads the ORIGINAL decoded WildTangent models (extracted from .wsad/.wsgo/.wsbm
# via the cracked WLD3 container) from Resources/MODELS/<NAME>/model.json.
#
# Manifest schema (produced by the extraction pipeline):
# {
#   "name": "CANNON",
#   "upAxis": "Y" | "Z",      // native axis that should map to SceneKit +Y (optional, default Y)
#   "scale": 1.0,              // uniform scale to apply (optional, default 1)
#   "parts": [
#     { "mesh": "barrel.obj",
#       "textures": ["cannonbase.jpg"],   // optional; MTL is used if present
#       "transform": [16 floats row-major] // optional 4x4; identity if absent
#     }, ...
#   ]
# }
#
# Returns null when no manifest exists for a name, so callers fall back to
# procedural geometry. Results are cached and returned as clones.
#
# The Swift reads the OBJ parts through MDLAsset; Godot has no runtime OBJ
# loader (and assets never enter res://, PORTING.md), so a minimal OBJ parser
# lives at the bottom of this file. MTL material loading is NOT ported: every
# shipped manifest lists its textures, and the manifest texture is
# authoritative (see below).


static var _cache := {}   # name -> Node3D or null


## Real model node for a prop/object name, or null if not available.
static func node_for(name: String) -> Node3D:
	if _cache.has(name):
		var cached: Node3D = _cache[name]
		return cached.duplicate() if cached != null else null
	var built := _build(name)
	_cache[name] = built
	return built.duplicate() if built != null else null


static func has_model(name: String) -> bool:
	return FileAccess.file_exists(Assets.path("MODELS/%s/model.json" % name))


static func _build(name: String) -> Node3D:
	var dir := "MODELS/%s" % name
	var data := Assets.data("%s/model.json" % dir)
	if data.is_empty():
		return null
	var manifest = JSON.parse_string(data.get_string_from_utf8())
	if not (manifest is Dictionary) or not (manifest.get("parts") is Array) \
			or manifest["parts"].is_empty():
		return null

	var root := Node3D.new()
	root.name = "model-%s" % name

	var loaded_any := false
	for part in manifest["parts"]:
		var mesh_path := "%s/%s" % [dir, part["mesh"]]
		if not FileAccess.file_exists(Assets.path(mesh_path)):
			continue

		var part_node := _load_obj(mesh_path)
		if part_node == null:
			continue

		# The manifest texture is authoritative; foliage textures carry a chroma
		# key (now baked to alpha) and must cutout-render.
		# Prefer the diffuse texture: some manifests list the engine's environment
		# 'reflection' map first, which renders near-black without env mapping.
		var tex_name = null
		if part.get("textures") is Array:
			for t in part["textures"]:
				if not ("reflection" in String(t).to_lower()):
					tex_name = t
					break
			if tex_name == null and not part["textures"].is_empty():
				tex_name = part["textures"][0]
		if tex_name != null:
			var img := Assets.image("%s/textures/%s" % [dir, tex_name])
			if img == null:
				img = Assets.image("%s/%s" % [dir, tex_name])
			if img != null:
				_apply_texture(img, part_node, _has_transparency(img))
		_de_wash(part_node)   # stop the tropical ambient from washing the mesh pale

		if part.get("transform") is Array and part["transform"].size() == 16:
			part_node.transform = _matrix_from_row_major(part["transform"])
		# Name by mesh stem (e.g. "barrel") so callers can rig individual parts.
		part_node.name = String(part["mesh"]).get_basename()
		root.add_child(part_node)
		loaded_any = true
	if not loaded_any:
		root.free()
		return null

	# Axis / scale normalization so importers only reason about SceneKit space once.
	if String(manifest.get("upAxis", "Y")).to_upper() == "Z":
		# WildTangent models are commonly Z-up; rotate into SceneKit's Y-up.
		root.rotation.x = -PI / 2
	# CAUTION: the shipped model.json "scale" values are a fabricated heuristic,
	# not source data — the original engine never scales prop actors
	# (Prop.java:388 → 1-arg Media_Object_Actor ctor, finalscale stays -1); the
	# skinned.json route in Props.swift:228 bypasses this entirely. Ported
	# verbatim from the Swift, which still applies the field when != 1.
	if manifest.get("scale") is float:
		var s: float = manifest["scale"]
		if s != 0 and s != 1:
			root.scale = Vector3(s, s, s)
	return root


static func _apply_texture(img: Image, node: Node3D, cutout: bool) -> void:
	var tex := ImageTexture.create_from_image(img)
	for m in _materials(node):
		m.albedo_texture = tex
		# magnificationFilter = .nearest — keep the pixel-art crisp
		# (WITH_MIPMAPS keeps SceneKit's default mipmapped minification)
		m.texture_filter = BaseMaterial3D.TEXTURE_FILTER_NEAREST_WITH_MIPMAPS
		if cutout:
			# Cutout foliage sprites (fronds, rails): alpha-tested hard edges, and
			# unlit so the double-sided undersides keep their true color instead of
			# washing to purple under the map's teal ambient.
			m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED   # .constant
			m.cull_mode = BaseMaterial3D.CULL_DISABLED              # isDoubleSided
			# transparencyMode .aOne + discard at a < 0.5 → alpha scissor;
			# writes/readsFromDepthBuffer = true is the Godot scissor default
			m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA_SCISSOR
			m.alpha_scissor_threshold = 0.5
		else:
			# lightingModel = .lambert → shaded + metallic_specular 0,
			# roughness 1 (PORTING.md)
			m.roughness = 1.0
			m.metallic_specular = 0.0


## Cut ambient reflectance + specular so the game's bright ambient doesn't wash
## the decoded meshes to a pale ghost (same fix the cannon needed).
## NOT PORTED: ambient.contents = 0.3 white — Godot StandardMaterial3D has no
## per-material ambient reflectance knob. specular = black → metallic_specular 0.
static func _de_wash(node: Node3D) -> void:
	for m in _materials(node):
		m.metallic_specular = 0.0


## enumerateHierarchy over every surface material (StandardMaterial3D).
static func _materials(node: Node3D) -> Array:
	var out := []
	if node is MeshInstance3D and node.mesh != null:
		for s in node.mesh.get_surface_count():
			var m = node.mesh.surface_get_material(s)
			if m is StandardMaterial3D:
				out.append(m)
	for child in node.get_children():
		out.append_array(_materials(child))
	return out


## True if the image has any meaningfully transparent pixels (chroma-keyed
## foliage). Port of the NSImage.hasTransparency extension (HUDArt.swift:383),
## private here until src/ui/hud_art.gd lands: sample a ≤48x48 grid for a < 0.5.
static func _has_transparency(img: Image) -> bool:
	if img.detect_alpha() == Image.ALPHA_NONE:   # rep.hasAlpha guard
		return false
	var w := img.get_width()
	var h := img.get_height()
	var step_x := maxi(1, w / 48)
	var step_y := maxi(1, h / 48)
	var py := 0
	while py < h:
		var px := 0
		while px < w:
			if img.get_pixel(px, py).a < 0.5:
				return true
			px += step_x
		py += step_y
	return false


## Row-major [m00 m01 m02 m03  m10 ...] -> Transform3D. Godot is column-vector
## convention (unlike SceneKit's row-vector SCNMatrix4, which the Swift
## transposes into), so the input rows ARE the matrix rows: Basis takes the
## three COLUMNS, origin is the fourth column.
static func _matrix_from_row_major(t: Array) -> Transform3D:
	return Transform3D(
			Basis(Vector3(t[0], t[4], t[8]),
				Vector3(t[1], t[5], t[9]),
				Vector3(t[2], t[6], t[10])),
			Vector3(t[3], t[7], t[11]))


# MARK: - Minimal OBJ loader (the MDLAsset replacement; geometry only)

## Parse an OBJ (v/vt/vn + triangle-or-polygon f) into one MeshInstance3D.
## The Swift wraps each MDL object in child nodes; our exporter writes a single
## object per file, so one MeshInstance3D per part suffices.
static func _load_obj(relative: String) -> MeshInstance3D:
	var text := Assets.text(relative)
	if text.is_empty():
		return null
	var obj_v := PackedVector3Array()
	var obj_vt := PackedVector2Array()
	var obj_vn := PackedVector3Array()
	var corner_index := {}        # "v/vt/vn" -> packed vertex index
	var positions := PackedVector3Array()
	var uvs := PackedVector2Array()
	var normals := PackedVector3Array()
	var indices := PackedInt32Array()
	var has_normals := false
	var has_uvs := false

	for line in text.split("\n", false):
		var fields := line.strip_edges().split(" ", false)
		if fields.is_empty():
			continue
		match fields[0]:
			"v":
				obj_v.append(Vector3(fields[1].to_float(), fields[2].to_float(), fields[3].to_float()))
			"vt":
				# OBJ vt is bottom-left origin: the exporter writes 1 - v
				# (tools/wsgo2obj_final.py:210); flip back to the raw WT
				# v-down coordinate that Godot (and SceneKit) sample.
				obj_vt.append(Vector2(fields[1].to_float(), 1.0 - fields[2].to_float()))
			"vn":
				obj_vn.append(Vector3(fields[1].to_float(), fields[2].to_float(), fields[3].to_float()))
			"f":
				var corners := PackedInt32Array()
				for c in range(1, fields.size()):
					var key := fields[c]
					if corner_index.has(key):
						corners.append(corner_index[key])
						continue
					var refs := key.split("/")
					var vi := _obj_index(refs[0], obj_v.size())
					positions.append(obj_v[vi])
					if refs.size() > 1 and refs[1] != "":
						uvs.append(obj_vt[_obj_index(refs[1], obj_vt.size())])
						has_uvs = true
					else:
						uvs.append(Vector2.ZERO)
					if refs.size() > 2 and refs[2] != "":
						normals.append(obj_vn[_obj_index(refs[2], obj_vn.size())])
						has_normals = true
					else:
						normals.append(Vector3.ZERO)
					var packed := positions.size() - 1
					corner_index[key] = packed
					corners.append(packed)
				# fan-triangulate; SceneKit/OBJ front faces are CCW, Godot's
				# are CW (PORTING.md): swap the second/third index.
				for t in range(1, corners.size() - 1):
					indices.append(corners[0])
					indices.append(corners[t + 1])
					indices.append(corners[t])
	if positions.is_empty() or indices.is_empty():
		return null

	if not has_normals:
		# Some exports carry no vn (e.g. LIGHTHOUSE, v/vt faces only):
		# synthesize smooth normals (area-weighted face-normal accumulation).
		# INTERPOLATION, not source data — the geom's packed normal blocks
		# weren't exported for these parts.
		normals.fill(Vector3.ZERO)
		for t in range(0, indices.size(), 3):
			var a := indices[t]
			var b := indices[t + 1]
			var c := indices[t + 2]
			# indices are already CW-swapped for Godot fronts, so the
			# outward normal is (c-a)x(b-a)
			var n := (positions[c] - positions[a]).cross(positions[b] - positions[a])
			normals[a] += n
			normals[b] += n
			normals[c] += n
		for i in normals.size():
			normals[i] = normals[i].normalized() if normals[i].length() > 0 else Vector3.UP

	var arrays := []
	arrays.resize(Mesh.ARRAY_MAX)
	arrays[Mesh.ARRAY_VERTEX] = positions
	arrays[Mesh.ARRAY_NORMAL] = normals
	if has_uvs:
		arrays[Mesh.ARRAY_TEX_UV] = uvs
	arrays[Mesh.ARRAY_INDEX] = indices
	var mesh := ArrayMesh.new()
	mesh.add_surface_from_arrays(Mesh.PRIMITIVE_TRIANGLES, arrays)
	mesh.surface_set_material(0, StandardMaterial3D.new())
	var node := MeshInstance3D.new()
	node.mesh = mesh
	return node


## OBJ indices are 1-based; negative counts back from the end.
static func _obj_index(s: String, count: int) -> int:
	var i := s.to_int()
	return i - 1 if i > 0 else count + i
