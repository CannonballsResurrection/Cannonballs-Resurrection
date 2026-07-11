extends SceneTree
# Parse-check every script in src/ (recursively). Run:
#   godot --headless --path windows --script tools/check_parse.gd
# Exit code 0 = all scripts parse.


func _init() -> void:
	var errs := _check_dir("res://src")
	if errs == 0:
		print("parse OK")
	quit(1 if errs > 0 else 0)


func _check_dir(dir: String) -> int:
	var errs := 0
	for f in DirAccess.get_files_at(dir):
		if f.ends_with(".gd"):
			var s = load(dir + "/" + f)
			if s == null or not (s as GDScript).can_instantiate():
				# load() already printed the parse errors
				printerr("FAILED: ", dir + "/" + f)
				errs += 1
	for d in DirAccess.get_directories_at(dir):
		errs += _check_dir(dir + "/" + d)
	return errs
