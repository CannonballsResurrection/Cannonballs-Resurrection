class_name HelpViewer
# Port of macos/Sources/Cannonballs/HelpViewer.swift.
#
# Viewer for the original bundled Help! pages (Resources/HELP).
#
# The 2002 game ran as an applet inside its host web page; the Help! menu
# called out through that page's JavaScript bridge (Main.java:248
# `HostPage.call("launchHelpPage", ...)`, Main.java:219 `launchTutorial`),
# so the pages appeared in the same browser hosting the game. The macOS
# build keeps them inside the game window with a WKWebView panel — that
# in-window panel is macOS-specific (Godot has no WebView), so this port
# restores the ORIGINAL behavior instead: the bundled pages open in the
# host OS browser via OS.shell_open. There is consequently no dismiss() /
# key monitor / navigation policy to port — the browser owns the page, as
# it did in 2002.


## Open a bundled help page (e.g. "howtoplay.htm"), optionally at an anchor
## fragment (e.g. "playing" -> howtoplay.htm#playing, the tutorial1-5 pages'
## in-page targets). Mirrors the Swift present(over:file:fragment:), minus
## the host view.
static func present(file: String, fragment: Variant = null) -> void:
	# OS.shell_open hands a file:// URL to the host browser, which can only read
	# REAL files on disk. In the single-file Windows build the HELP pages live
	# inside the embedded assets.pck (Assets.root == "res://Resources"), a virtual
	# path with no on-disk file, so the browser opened nothing. Materialize the
	# whole HELP tree (pages cross-link and pull local images/) to a real writable
	# dir first, then open from there. Folder/dev builds already sit on disk and
	# open in place.
	var url := _file_url(_disk_help_dir().path_join(file))
	if fragment != null:
		url += "#" + str(fragment)
	OS.shell_open(url)


## Real filesystem directory holding the HELP pages. For a folder/dev build
## (Assets.root is a real path) that's the HELP dir as-is. For the packed
## single-file build (res://) the tree is extracted once to user:// (which
## globalizes to a real, writable OS path) and that location is returned.
static func _disk_help_dir() -> String:
	var src := Assets.path("HELP")
	if not src.begins_with("res://"):
		return src
	var dst := "user://help"
	# Marker guards against a half-finished extraction being treated as complete.
	if not FileAccess.file_exists(dst.path_join(".extracted")):
		_copy_tree(src, dst)
		var mark := FileAccess.open(dst.path_join(".extracted"), FileAccess.WRITE)
		if mark != null:
			mark.close()
	return ProjectSettings.globalize_path(dst)


## Recursively copy a directory tree, reading through FileAccess so it works
## whether the source is a real folder or a mounted resource pack.
static func _copy_tree(src_dir: String, dst_dir: String) -> void:
	DirAccess.make_dir_recursive_absolute(dst_dir)
	var d := DirAccess.open(src_dir)
	if d == null:
		push_error("HelpViewer: cannot open " + src_dir)
		return
	d.list_dir_begin()
	var name := d.get_next()
	while name != "":
		var s := src_dir.path_join(name)
		var t := dst_dir.path_join(name)
		if d.current_is_dir():
			_copy_tree(s, t)
		else:
			var bytes := FileAccess.get_file_as_bytes(s)
			var f := FileAccess.open(t, FileAccess.WRITE)
			if f != null:
				f.store_buffer(bytes)
				f.close()
		name = d.get_next()
	d.list_dir_end()


## file:// URL for an absolute filesystem path, with each segment
## percent-encoded (the repo path contains a space). A Windows drive segment
## ("C:") stays raw — encoding its colon breaks the URL.
static func _file_url(path: String) -> String:
	var segs := PackedStringArray()
	for seg in path.split("/"):
		if seg.length() == 2 and seg[1] == ":":
			segs.append(seg)
		else:
			segs.append(seg.uri_encode())
	var joined := "/".join(segs)
	# POSIX paths start with "/" (file:///Users/...); Windows paths need the
	# third slash added (file:///C:/...).
	return "file://" + joined if joined.begins_with("/") else "file:///" + joined
