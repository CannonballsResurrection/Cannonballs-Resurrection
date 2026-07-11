class_name RasterDemo
extends RefCounted
# Port of macos/Sources/Cannonballs/RasterDemo.swift.
# Test harness for the software rasterizer: loads a decoded model (OBJ parts +
# textures via its model.json) and renders it through SoftRaster to a PNG, so
# the DX7-faithful look can be compared to the GPU renderer and the original.
# (macOS CLI: `--raster <NAME> <out.png>` — main.swift:96-100; call
# RasterDemo.render from a tool script for the same result here.)


class Part:
	var positions := PackedVector3Array()
	var normals := PackedVector3Array()
	var uvs := PackedVector2Array()
	var indices := PackedInt32Array()
	var tex: SoftRaster.RasterTexture = null


static func render(model_name: String, out_path: String, size := 480) -> void:
	var parts := _load_model(model_name)
	if parts.is_empty():
		printerr("raster: no model %s" % model_name)
		return
	# Frame the whole model.
	var lo := Vector3(INF, INF, INF)   # Swift: .greatestFiniteMagnitude
	var hi := Vector3(-INF, -INF, -INF)
	for p in parts:
		for v in (p as Part).positions:
			lo = lo.min(v)
			hi = hi.max(v)
	var center := (lo + hi) * 0.5
	var radius := (hi - lo).length() * 0.5

	var raster := SoftRaster.new(size, size)
	raster.clear(Vector3(0.36, 0.55, 0.78))        # sky-ish backdrop

	# 3/4 view, like the --model snapshot.
	var dir := Vector3(0.55, 0.45, 1.0).normalized()
	var eye := center + dir * (radius * 2.7)
	var view := look_at(eye, center, Vector3(0, 1, 0))
	var proj := perspective(0.6, 1, radius * 0.05, radius * 12)
	var vp := proj * view

	var light := SoftRaster.Light.new(
			Vector3(0.42, 0.42, 0.42),
			Vector3(0.4, 0.8, 0.5).normalized(),
			Vector3(0.85, 0.85, 0.85))
	for p in parts:
		var part := p as Part
		raster.draw(part.positions, part.normals, part.uvs, part.indices,
				part.tex, vp, Basis.IDENTITY, light, SoftRaster.Fog.none())
	write_png(raster.rgba8(Vector3(0.36, 0.55, 0.78)), size, size, out_path)
	print("raster wrote %s (%s, %d parts)" % [out_path, model_name, parts.size()])


# MARK: model loading (reuses the model.json manifest)

static func _load_model(model_name: String) -> Array:
	var dir := "MODELS/%s" % model_name
	var data := Assets.data("%s/model.json" % dir)
	if data.is_empty():
		return []
	var man = JSON.parse_string(data.get_string_from_utf8())
	if not (man is Dictionary) or not (man.get("parts") is Array):
		return []
	# NOTE: model.json "scale" is fabricated dead data the game never used
	# (props render at native geom scale — see props.gd); the Swift demo
	# applies it anyway (RasterDemo.swift:52,59), and since this view frames
	# the model by its own bounding box, a uniform scale is visually a no-op.
	# Carried verbatim for parity with the Swift harness.
	var scale := float(man.get("scale", 1.0))
	var out: Array = []
	for pm in man["parts"]:
		if not (pm is Dictionary) or not (pm.get("mesh") is String):
			continue
		var text := Assets.text("%s/%s" % [dir, pm["mesh"]])
		if text.is_empty():
			continue
		var part := _parse_obj(text)
		for i in part.positions.size():
			part.positions[i] = part.positions[i] * scale
		var tex_list = pm.get("textures")
		var tex_name = null
		if tex_list is Array:
			# first texture that isn't a reflection map, else the first
			for t in tex_list:
				if not ("reflection" in String(t).to_lower()):
					tex_name = t
					break
			if tex_name == null and not (tex_list as Array).is_empty():
				tex_name = tex_list[0]
		if tex_name != null:
			var img := Assets.image("%s/textures/%s" % [dir, tex_name])
			if img == null:
				img = Assets.image("%s/%s" % [dir, tex_name])
			if img != null:
				part.tex = SoftRaster.RasterTexture.from_image(img)
		out.append(part)
	return out


static func _parse_obj(text: String) -> Part:
	var pos := PackedVector3Array()
	var src_uvs := PackedVector2Array()
	var src_nrm := PackedVector3Array()
	var part := Part.new()
	var cache := {}
	for line in text.split("\n", false):
		var t := line.split(" ", false)
		if t.is_empty():
			continue
		var tag := t[0]
		if tag == "v" and t.size() >= 4:
			pos.append(Vector3(t[1].to_float(), t[2].to_float(), t[3].to_float()))
		elif tag == "vt" and t.size() >= 3:
			src_uvs.append(Vector2(t[1].to_float(), 1 - t[2].to_float()))   # OBJ v is bottom-up
		elif tag == "vn" and t.size() >= 4:
			src_nrm.append(Vector3(t[1].to_float(), t[2].to_float(), t[3].to_float()))
		elif tag == "f":
			var face := PackedInt32Array()
			for j in range(1, t.size()):
				var key := t[j]
				if cache.has(key):
					face.append(cache[key])
					continue
				var comps := key.split("/", true)   # keep empty subsequences
				var pi := comps[0].to_int() - 1
				var ti := comps[1].to_int() - 1 if comps.size() > 1 and comps[1] != "" else -1
				var ni := comps[2].to_int() - 1 if comps.size() > 2 and comps[2] != "" else -1
				part.positions.append(pos[pi] if pi >= 0 and pi < pos.size() else Vector3.ZERO)
				part.normals.append(src_nrm[ni] if ni >= 0 and ni < src_nrm.size() else Vector3.ZERO)
				part.uvs.append(src_uvs[ti] if ti >= 0 and ti < src_uvs.size() else Vector2.ZERO)
				var vi := part.positions.size() - 1
				cache[key] = vi
				face.append(vi)
			for k in range(1, face.size() - 1):
				part.indices.append(face[0])
				part.indices.append(face[k])
				part.indices.append(face[k + 1])
	# Compute smooth normals if the OBJ had none.
	if src_nrm.is_empty():
		var accum := PackedVector3Array()
		accum.resize(part.positions.size())
		var i := 0
		while i + 2 < part.indices.size():
			var a := part.positions[part.indices[i]]
			var b := part.positions[part.indices[i + 1]]
			var c := part.positions[part.indices[i + 2]]
			var fn := (b - a).cross(c - a)
			accum[part.indices[i]] = accum[part.indices[i]] + fn
			accum[part.indices[i + 1]] = accum[part.indices[i + 1]] + fn
			accum[part.indices[i + 2]] = accum[part.indices[i + 2]] + fn
			i += 3
		for j in part.positions.size():
			var n := accum[j]
			part.normals[j] = n.normalized() if n.length() > 1e-6 else Vector3(0, 1, 0)
	return part


# MARK: matrices + PNG

## D3D-style perspective (z into [0,1], +w row) — distinct from
## SceneRasterizer.gl_perspective; transcribed from RasterDemo.swift:128-133.
static func perspective(fov_y: float, aspect: float, near: float, far: float) -> Projection:
	var yy := 1.0 / tan(fov_y * 0.5)
	var xx := yy / aspect
	var zz := far / (far - near)
	var zw := -near * far / (far - near)
	var p := Projection.IDENTITY
	p.x = Vector4(xx, 0, 0, 0)
	p.y = Vector4(0, yy, 0, 0)
	p.z = Vector4(0, 0, zz, 1)
	p.w = Vector4(0, 0, zw, 0)
	return p


static func look_at(eye: Vector3, center: Vector3, up: Vector3) -> Projection:
	var z := (center - eye).normalized()          # left-handed (D3D), +z into screen
	var x := up.cross(z).normalized()
	var y := z.cross(x)
	var t := Vector3(-x.dot(eye), -y.dot(eye), -z.dot(eye))
	var m := Projection.IDENTITY
	m.x = Vector4(x.x, y.x, z.x, 0)
	m.y = Vector4(x.y, y.y, z.y, 0)
	m.z = Vector4(x.z, y.z, z.z, 0)
	m.w = Vector4(t.x, t.y, t.z, 1)
	return m


static func write_png(rgba: PackedByteArray, width: int, height: int, path: String) -> void:
	Image.create_from_data(width, height, false, Image.FORMAT_RGBA8, rgba).save_png(path)
