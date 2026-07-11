class_name Assets
# Port of macos/Sources/Cannonballs/Assets.swift.
# Locates the platform-neutral game assets (repo: shared/Resources — one asset
# tree consumed by both the macOS and Windows builds). Assets are read at
# runtime from the OS filesystem, never imported into res:// (PORTING.md).


static var root: String = _locate_root()

static var _tex_cache := {}


static func _locate_root() -> String:
	# single-file build: the asset tree is embedded as a RAW pack (res://assets.pck,
	# built by tools/pack_assets.gd — byte-identical originals, no import re-encode).
	# Mount it, then read from res://Resources. (Absent in dev/folder builds.)
	if FileAccess.file_exists("res://assets.pck"):
		ProjectSettings.load_resource_pack("res://assets.pck")
	if FileAccess.file_exists("res://Resources/maplist.dat"):
		return "res://Resources"
	# folder build: Resources/ copied next to the executable
	var exe_dir := OS.get_executable_path().get_base_dir()
	if FileAccess.file_exists(exe_dir.path_join("Resources/maplist.dat")):
		return exe_dir.path_join("Resources")
	# dev runs: walk up from the project dir (res:// = repo windows/) to shared/Resources
	var dir := ProjectSettings.globalize_path("res://").rstrip("/")
	for i in 8:
		var cand := dir.path_join("shared/Resources")
		if FileAccess.file_exists(cand.path_join("maplist.dat")):
			return cand
		dir = dir.get_base_dir()
	push_error("Game assets not found: expected Resources beside the exe or shared/Resources up-tree")
	return ""


static func path(relative: String) -> String:
	return root.path_join(relative)


static func data(relative: String) -> PackedByteArray:
	return FileAccess.get_file_as_bytes(path(relative))


static func text(relative: String) -> String:
	return FileAccess.get_file_as_string(path(relative))


static func image(relative: String) -> Image:
	var p := path(relative)
	if not FileAccess.file_exists(p):
		return null
	return Image.load_from_file(p)


## Cached GPU texture for an asset image (most callers want a texture).
static func texture(relative: String) -> ImageTexture:
	if _tex_cache.has(relative):
		return _tex_cache[relative]
	var img := image(relative)
	var tex: ImageTexture = null
	if img != null:
		tex = ImageTexture.create_from_image(img)
	_tex_cache[relative] = tex
	return tex
