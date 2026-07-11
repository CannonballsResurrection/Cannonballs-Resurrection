# Headless build step: pack a directory tree into a RAW Godot .pck (PCKPacker
# stores bytes verbatim — no import/re-encode), with every file placed at
# res://Resources/<relative path>. package_win.sh runs this to build assets.pck,
# which then gets embedded into the exe (a .pck is a non-importable file, so the
# exporter ships it raw) and mounted at boot by assets.gd. This is what lets the
# Windows build be a single self-contained exe with byte-identical original art.
#
#   CB_SRC=<abs shared/Resources> CB_OUT=<abs windows/assets.pck> \
#     godot --headless --path windows --script tools/pack_assets.gd
extends SceneTree

func _init() -> void:
	var src := OS.get_environment("CB_SRC")
	var out := OS.get_environment("CB_OUT")
	if src == "" or out == "":
		push_error("pack_assets: set CB_SRC and CB_OUT")
		quit(1)
		return
	var pck := PCKPacker.new()
	if pck.pck_start(out) != OK:
		push_error("pack_assets: pck_start failed for " + out)
		quit(1)
		return
	var n := _add_dir(pck, src, "res://Resources")
	if pck.flush() != OK:
		push_error("pack_assets: flush failed")
		quit(1)
		return
	print("pack_assets: packed %d files -> %s" % [n, out])
	quit(0)

func _add_dir(pck: PCKPacker, real_dir: String, res_dir: String) -> int:
	var count := 0
	var d := DirAccess.open(real_dir)
	if d == null:
		push_error("pack_assets: cannot open " + real_dir)
		return 0
	d.list_dir_begin()
	var name := d.get_next()
	while name != "":
		if not name.begins_with("."):
			var rp := real_dir.path_join(name)
			var resp := res_dir + "/" + name
			if d.current_is_dir():
				count += _add_dir(pck, rp, resp)
			else:
				pck.add_file(resp, rp)
				count += 1
		name = d.get_next()
	d.list_dir_end()
	return count
