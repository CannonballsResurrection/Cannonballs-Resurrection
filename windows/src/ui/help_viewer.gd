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
	var url := _file_url(Assets.path("HELP/" + file))
	if fragment != null:
		url += "#" + str(fragment)
	OS.shell_open(url)


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
