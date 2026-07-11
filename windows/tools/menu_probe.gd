extends SceneTree
# Standing menu uitest: renders menu_scene.gd through the original flow and
# snapshots each screen (the analogue of the macOS main.swift --uitest menu
# leg, main.swift:302-340). Same SpriteKit y-up click coords, same WALL-CLOCK
# seconds — the menu's iris/hourglass tweens run in seconds, and a --script
# run is not vsync'd to 60fps, so frame counts drift (probed 2026-07-10: the
# frame-based version snapped "settings" mid-hourglass).
# Run: godot --path windows --script tools/menu_probe.gd
# Compare: macos/snapshots/uitest-{menu,name,iris-close,transition,settings,
#          lobby,lobbyopen,lobbyready,lobbycolor}.png


var menu: MenuScene
var _preview_bg: CanvasLayer = null


func _init() -> void:
	menu = MenuScene.new()
	menu.player_name = "Tester"   # main.swift:302 UserDefaults "playerName"
	# Host stand-in for GameViewController.setMenuCannonPreview
	# (GameViewController.swift:162-222): the lobby's punched window reveals
	# the host viewport, whose scene background is the JOIN art. The spinning
	# tinted 3D cannon of that preview needs cannon.gd (game_controller task)
	# and is NOT part of this probe — known visual gap vs the mac reference.
	menu.on_cannon_preview = _set_cannon_preview
	root.add_child(menu)
	_run()


func _set_cannon_preview(color: Variant) -> void:
	if color == null:
		if _preview_bg != null:
			_preview_bg.queue_free()
			_preview_bg = null
		return
	if _preview_bg != null:
		return   # color change retints the cannon only (no background change)
	_preview_bg = CanvasLayer.new()
	_preview_bg.layer = 0   # below the menu CanvasLayer (default layer 1)
	var tex := Assets.texture("MENUS/JOIN.png")
	if tex != null:
		var n := Sprite2D.new()
		n.texture = tex
		n.texture_filter = CanvasItem.TEXTURE_FILTER_NEAREST
		n.position = Vector2(400, 300)
		n.scale = Vector2(800, 600) / Vector2(tex.get_size())
		_preview_bg.add_child(n)
	root.add_child(_preview_bg)


# Same click coordinates and delays as main.swift:308-340 (its nested
# asyncAfter deadlines converted to sequential waits).
func _run() -> void:
	await _wait(2.5)
	# (No hover: main.swift never hovers. Single Player reads brighter than its
	# neighbors in the reference because it is the only un-dimmed button; the
	# real hover state is the RED bottom half of the buttons sheet.)
	_snap("menu")
	menu.click(Vector2(400, 180))   # Single Player -> Your Name popup
	await _wait(0.8)
	_snap("name")
	menu.click(Vector2(527, 188))   # Enter -> New Game Settings
	await _wait(0.9)
	_snap("iris-close")             # iris mid-close: screen in the shrinking circle
	await _wait(0.7)                # t=1.6 after Enter
	_snap("transition")             # iris + hourglass mid-cover
	await _wait(2.4)                # t=4.0 after Enter
	_snap("settings")
	menu.click(Vector2(527, 188))   # Create -> lobby
	await _wait(1.5)
	_snap("lobby")
	menu.click(Vector2(200, 456))   # open slot 1's AI dropdown (src row center y=144)
	await _wait(0.5)
	_snap("lobbyopen")
	menu.click(Vector2(200, 329))   # pick "Thinker" for slot 1 (src y=144+31+3*32)
	await _wait(0.4)
	_snap("lobbyready")             # kick button should now show
	menu.click(Vector2(610, 280))   # open the color dropdown
	await _wait(0.4)
	menu.click(Vector2(610, 185))   # pick color 2 (src y=320+31+2*32)
	await _wait(1.2)
	_snap("lobbycolor")
	quit()


func _wait(seconds: float) -> void:
	await create_timer(seconds).timeout


func _snap(snap_name: String) -> void:
	var img := root.get_texture().get_image()
	var dir := ProjectSettings.globalize_path("res://snapshots")
	DirAccess.make_dir_recursive_absolute(dir)
	var out := dir.path_join("uitest-%s.png" % snap_name)
	img.save_png(out)
	print("probe wrote ", out)
