extends Node3D
# The app entry: port of macos/Sources/Cannonballs/main.swift (bootstrap +
# --uitest harness) and GameViewController.swift (menu → iris → game →
# game-over → menu flow). AppDelegate.swift's window/menu-bar glue does not
# port: the 800x600 window is project.godot, and the menu-bar Game/Camera
# actions live on the HUD's own top menu + keyboard (game_controller.gd).
#
# GDScript port notes (windows/PORTING.md):
# - The macOS render-thread command queue (runOnRenderThread) drops out:
#   Godot input and _process run on one thread.
# - The lobby's spinning cannon preview renders in a SubViewport composited
#   between the JOIN backdrop and the menu CanvasLayer (SceneKit swapped the
#   whole SCNView scene instead — Godot-forced convention change, one place).
# - CLI: `--uitest` runs the full main.swift:289-430 snapshot sequence (same
#   click coords, same wall-clock seconds — tools/menu_probe.gd probed that
#   seconds, not frame counts, keep the iris/hourglass tweens in sync);
#   `--uitest-terrain [--map=N]` keeps the old terrain-only snapshot mode.

enum Mode { MENU, GAME }
var mode: int = Mode.MENU

var game: GameController = null
var menu: MenuScene = null

# menu backdrop (GameViewController.showMenu)
var backdrop_world: World = null
var backdrop_cam: Camera3D = null
var orbit_angle := 0.0

# lobby cannon preview (GameViewController.setMenuCannonPreview)
var _preview_layer: CanvasLayer = null
var _preview_viewport: SubViewport = null
var _preview_group: Node3D = null
var _preview_barrel: Node3D = null

# Software-rasterizer mode (the reimplemented DX7 pipeline), toggled with 'R'
# (GameViewController.swift:289-297). SceneRasterizer.Live owns the whole
# extract/thread/display pipeline (its documented wiring contract:
# attach(world, camera) once, update(dt) per frame, toggle() on 'R').
var software_render := false:
	set(value):
		software_render = value
		if _soft != null:
			_soft.set_enabled(value)
var _soft: SceneRasterizer.Live = null

# --uitest-terrain (the pre-menu render harness)
var _terrain_test := false
var _terrain_world: World = null
var _frames := 0


func _ready() -> void:
	# Audio.shared publishes in _init but playback needs the tree — add the
	# singleton node exactly once at boot (audio.gd header).
	add_child(Audio.new())

	_apply_cutlass_cursor()

	var args := OS.get_cmdline_user_args()
	var map_index := 0
	var uitest := false
	var donetest := false
	for a in args:
		if a.begins_with("--map="):
			map_index = a.substr(6).to_int()
		elif a == "--uitest":
			uitest = true
		elif a == "--uitest-terrain":
			_terrain_test = true
		elif a == "--donetest":
			donetest = true

	if MapCatalog.maps.is_empty():
		push_error("no maps loaded — assets root: %s" % Assets.root)
		get_tree().quit(1)
		return

	if _terrain_test:
		_start_terrain_test(map_index)
		return

	if donetest:
		_run_donetest(map_index)
		return

	show_menu()
	if uitest:
		_run_uitest()


# --donetest: force the results screen, then push a REAL mouse click at the Done
# button through the full input path (viewport GUI phase → _unhandled_input →
# game.click → hud.click), and confirm it returns to the menu. NOT a hud.click()
# call — that would hide an interception bug like the name-entry Enter one.
func _run_donetest(map_index: int) -> void:
	var opts := G.GameOptions.new()
	opts.map_index = clampi(map_index, 0, MapCatalog.maps.size() - 1)
	opts.players = [G.PlayerConfig.new("Tester", 0, 0),
			G.PlayerConfig.new("Bot", 1, 2)]
	show_menu()
	for i in 3:
		await get_tree().process_frame
	_start_game(opts)
	for i in 5:
		await get_tree().process_frame
	# Reach the REAL end state (not just show_results): eliminate the bot so the
	# match is decided → game_over, success camera, "You Win!"; then let the real
	# update loop surface the results screen after the 15 s destroy clock.
	game.players[1].active = false
	game._check_win_condition()
	game.game_over_timer = 16.0
	for i in 60:
		await get_tree().process_frame
		if game.hud.results_shown:
			break
	print("mode=", mode, " game_over=", game.game_over,
			" results_shown=", game.hud.results_shown, " (want GAME/true/true)")

	# a real left click at Done: canvas (400, 575) — pushed through the viewport
	# so it runs the GUI/Control phase first (catches any STOP-filter interceptor)
	var ev := InputEventMouseButton.new()
	ev.button_index = MOUSE_BUTTON_LEFT
	ev.pressed = true
	ev.position = Vector2(400, 575)
	get_viewport().push_input(ev)

	var returned := false
	for i in 240:
		await get_tree().process_frame
		if mode == Mode.MENU:
			returned = true
			break
	print("mode after Done CLICK = ", mode, " returned_to_menu=", returned)
	print(returned and "DONE-CLICK PASS" or "DONE-CLICK FAIL")
	get_tree().quit(0 if returned else 1)


# The original uses a cutlass pointer (MENUS/POINTER → HUDART/pointer.png), the
# 64x64 art drawn at native size with the hotspot at the blade tip (2,2), same
# as GameViewController.KeyView.cutlass on macOS. Input.set_custom_mouse_cursor
# is global and persists across every screen/game.
func _apply_cutlass_cursor() -> void:
	if DisplayServer.get_name() == "headless":
		return
	var img := Assets.image("HUDART/pointer.png")
	if img == null:
		return
	var tex := ImageTexture.create_from_image(img)
	Input.set_custom_mouse_cursor(tex, Input.CURSOR_ARROW, Vector2(2, 2))


# MARK: - Menu (GameViewController.showMenu)

func show_menu() -> void:
	mode = Mode.MENU
	_set_menu_cannon_preview(null)
	if game != null:
		game.tear_down()
		if game.hud != null and game.hud is Node:
			_free_now(game.hud)
		_free_now(game.world.root)
		game = null
	software_render = false
	if _soft != null:
		_free_now(_soft)   # note: waits out any in-flight raster thread
		_soft = null
	if Audio.shared != null:
		Audio.shared.stop_loops()   # ocean / aiming loops end with the game
	var map_idx := randi_range(0, maxi(1, MapCatalog.maps.size()) - 1)
	backdrop_world = World.new(MapCatalog.maps[map_idx])
	add_child(backdrop_world.root)
	backdrop_cam = Camera3D.new()
	# SCNCamera zFar 4000, fieldOfView 55 (GameViewController.swift:134)
	backdrop_cam.far = 4000
	backdrop_cam.fov = 55
	backdrop_world.root.add_child(backdrop_cam)
	backdrop_cam.current = true
	_orbit_backdrop(0.0)

	menu = MenuScene.new()
	menu.on_start = func(opts) -> void:
		# original iris-wipe transition (Menu_Manager dissolve)
		IrisTransition.run(self, func() -> void: _start_game(opts))
	menu.on_cannon_preview = Callable(self, "_set_menu_cannon_preview")
	menu.on_quit = func() -> void: get_tree().quit()
	add_child(menu)
	# menu._ready plays the title track (GameViewController.swift:150)


func _orbit_backdrop(dt: float) -> void:
	orbit_angle += dt * 0.08
	if backdrop_world == null or backdrop_cam == null:
		return
	var c := backdrop_world.center()
	var r := 380.0
	backdrop_cam.look_at_from_position(
			Vector3(c.x + cos(orbit_angle) * r, 170.0, c.z + sin(orbit_angle) * r),
			Vector3(c.x, 12.0, c.z), Vector3.UP)


# The lobby's spinning tinted cannon. The ORIGINAL renders it camera-space
# over the static lobby background (Menu_Lobby_Screen.java:400-416): barrel
# + stand only, player-tinted, yaw -135, barrel pitch -20, spinning 50 deg/s.
# The scene is built ONCE per lobby; color changes only retint the barrel
# material (GameViewController.swift:153-222). Godot composition: JOIN art
# sprite + transparent SubViewport with the 3D cannon, both on a CanvasLayer
# BELOW the menu's own layer, whose lobby screen punches its scenery window
# transparent (menu_scene.gd _build_lobby) so the preview shows through.
func _set_menu_cannon_preview(color_index) -> void:
	if color_index == null:
		if _preview_layer != null:
			_free_now(_preview_layer)
			_preview_layer = null
		if _preview_viewport != null:
			_free_now(_preview_viewport)
			_preview_viewport = null
		_preview_group = null
		_preview_barrel = null
		# (No backdrop restore needed: the menu backdrop world never leaves the
		# main viewport — the Swift's scene-swap race, GameViewController.swift
		# :168-175, cannot happen here.)
		return
	if mode != Mode.MENU:
		return
	if _preview_viewport != null:
		# color change: retint only
		var skin := Cannon.tinted_skin(int(color_index))
		if skin != null and _preview_barrel != null:
			for m in Cannon._materials_in_tree(_preview_barrel):
				if m is StandardMaterial3D:
					m.albedo_texture = skin
		return

	# ref frame_002: the preview cannon floats OVER the lobby artwork, not in
	# a dark window — the JOIN art backs the transparent preview viewport so
	# the menu's punched circle is seamless (scene.background.contents =
	# MENUS/JOIN.png in the Swift).
	_preview_viewport = SubViewport.new()
	_preview_viewport.size = Vector2i(800, 600)
	_preview_viewport.own_world_3d = true
	_preview_viewport.transparent_bg = true
	_preview_viewport.render_target_update_mode = SubViewport.UPDATE_ALWAYS
	add_child(_preview_viewport)

	var parts := Cannon.build_parts(int(color_index))
	var statics: Node3D = parts[0]
	var barrel = parts[1]   # SkinnedModel.Actor or null
	var group := Node3D.new()
	for child in statics.get_children().duplicate():
		if child.name != "stone":
			statics.remove_child(child)
			group.add_child(child)
	if barrel != null:
		# original: CannonBarrelActor.Model.setOrientation(1,0,0,-20) — the
		# barrel rides 20 deg above horizontal while the group spins
		var tilt := Node3D.new()
		tilt.rotation.x = 20.0 * PI / 180.0
		tilt.add_child(barrel.root)
		group.add_child(tilt)
		_preview_barrel = barrel.root
	group.rotation.y = -135.0 * PI / 180.0
	# .repeatForever(.rotateBy(y: 2π·(50/360), duration: 1)) = 50 deg/s,
	# advanced in _process (the CABasicAnimation rule, PORTING.md)
	_preview_group = group
	_preview_viewport.add_child(group)

	var cam := Camera3D.new()
	cam.far = 200
	cam.fov = 60
	# frame the cannon at the original's screen spot (~612,206 of 800x600)
	cam.position = Vector3(-9.8, -4.4, 24)
	# ambient SCNLight intensity 700 → energy 0.7 (PORTING.md ÷1000); the
	# cannon materials are unshaded (styleCannonMaterials), kept for parity
	var env := Environment.new()
	env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	env.ambient_light_color = Color.WHITE
	env.ambient_light_energy = 0.7
	cam.environment = env
	_preview_viewport.add_child(cam)
	cam.current = true

	_preview_layer = CanvasLayer.new()
	_preview_layer.layer = 0   # below the menu CanvasLayer (default layer 1)
	# The JOIN art carries semi-transparent regions; the macOS build composites
	# them over the black SCNView (scnView.backgroundColor = .black,
	# GameViewController.swift:95) — back the art with black here too, or the
	# 3D backdrop world bleeds through the lobby.
	var black := ColorRect.new()
	black.color = Color.BLACK
	black.position = Vector2.ZERO
	black.size = Vector2(800, 600)
	# Menu/lobby clicks are hit-tested manually in MenuScene._unhandled_input; a
	# default-STOP full-screen Control here consumes them in the GUI phase first,
	# so the lobby buttons would go dead to the mouse. Never intercept.
	black.mouse_filter = Control.MOUSE_FILTER_IGNORE
	_preview_layer.add_child(black)
	var join := Assets.texture("MENUS/JOIN.png")
	if join != null:
		var bg := Sprite2D.new()
		bg.texture = join
		bg.texture_filter = CanvasItem.TEXTURE_FILTER_NEAREST
		bg.position = Vector2(400, 300)
		bg.scale = Vector2(800, 600) / Vector2(join.get_size())
		_preview_layer.add_child(bg)
	var view := Sprite2D.new()
	view.texture = _preview_viewport.get_texture()
	view.position = Vector2(400, 300)
	_preview_layer.add_child(view)
	add_child(_preview_layer)


# MARK: - Game (GameViewController.startGame)

func _start_game(opts) -> void:
	mode = Mode.GAME
	_set_menu_cannon_preview(null)
	if menu != null:
		_free_now(menu)
		menu = null
	var g := GameController.new(opts)
	g.on_exit = func() -> void:
		IrisTransition.run(self, show_menu)
	game = g
	add_child(g.world.root)
	# Make the game camera the viewport camera BEFORE the menu backdrop leaves,
	# so no frame can render with the backdrop's orbit camera still current (the
	# menu island showing behind the HUD) or with no current camera at all (a
	# grey view). `make_current()` is the explicit, unambiguous form; the backdrop
	# was previously freed first, which left that ordering window open. (Rarely /
	# environment-specifically reproduced — this closes the window defensively.)
	g.camera.node.current = true
	g.camera.node.make_current()
	if backdrop_world != null:
		_free_now(backdrop_world.root)
		backdrop_world = null
		backdrop_cam = null
	if g.hud != null and g.hud is Node:
		add_child(g.hud)
	_soft = SceneRasterizer.Live.new()
	add_child(_soft)
	_soft.attach(g.world, g.camera.node)
	if Audio.shared != null:
		Audio.shared.play_music(g.world.map.music_track)
		Audio.shared.start_loop("ocean", 0.35)   # island ambience (Island.SoundOcean loop)


# MARK: - Frame driver (GameViewController.renderer(_:updateAtTime:))

func _process(dt: float) -> void:
	if _terrain_test:
		if _terrain_world != null:
			_terrain_world.update(dt)
			_frames += 1
			if _frames == 30:
				_snap("terrain-%s" % _terrain_world.map.dir_name.to_lower())
				get_tree().quit()
		return
	match mode:
		Mode.MENU:
			_orbit_backdrop(dt)
			if backdrop_world != null:
				backdrop_world.update(dt)
			if _preview_group != null:
				_preview_group.rotation.y += deg_to_rad(50.0) * dt   # 50 deg/s spin
		Mode.GAME:
			if game != null:
				game.update(minf(dt, 0.25))   # Swift clamps dt to 0.25
				if _soft != null:
					_soft.update(dt)   # renderer(_:updateAtTime:) raster block


func toggle_software_render() -> void:
	software_render = not software_render   # the setter drives SceneRasterizer.Live
	if game != null and game.hud != null:
		game.hud.flash_message("Software Rasterizer: ON" if software_render
				else "Software Rasterizer: OFF")


# MARK: - Input routing (GameViewController.handleKeyDown/-Up/-Click/-Move;
# KeyView converts clicks to the overlay's SpriteKit y-up space — the same
# conversion menu_scene.gd's public click()/hover() expect).
# Menu mode input is handled by MenuScene's own _unhandled_input (its interim
# direct-input path, which is exactly this routing for the menu case).

func _unhandled_input(event: InputEvent) -> void:
	if mode != Mode.GAME or game == null:
		return
	if event is InputEventKey:
		if event.pressed:
			# 'R' toggles the software rasterizer (GameViewController.swift:302)
			if event.keycode == KEY_R and not (game.hud != null and game.hud.chatting):
				toggle_software_render()
				get_viewport().set_input_as_handled()
				return
			if game.key_down(event):
				get_viewport().set_input_as_handled()
		else:
			if game.key_up(event):
				get_viewport().set_input_as_handled()
	elif event is InputEventMouseButton \
			and event.button_index == MOUSE_BUTTON_LEFT and event.pressed:
		if game.click(Vector2(event.position.x, 600.0 - event.position.y)):
			get_viewport().set_input_as_handled()
	elif event is InputEventMouseMotion:
		if game.hud != null:
			game.hud.hover(Vector2(event.position.x, 600.0 - event.position.y))


# MARK: - --uitest-terrain (the pre-menu render harness, kept as-is)

func _start_terrain_test(map_index: int) -> void:
	_terrain_world = World.new(MapCatalog.maps[clampi(map_index, 0, MapCatalog.maps.size() - 1)])
	add_child(_terrain_world.root)
	var cam := Camera3D.new()
	# CameraController.init: SCNCamera zFar 4000, fieldOfView 55 (vertical)
	cam.far = 4000
	cam.fov = 55
	add_child(cam)
	# Whole-island framing for the render harness (OURS, not source data).
	var c := _terrain_world.center()
	cam.look_at_from_position(c + Vector3(0, 250, -400), c, Vector3.UP)
	cam.current = true


# MARK: - --uitest (main.swift:289-430: open the app, snapshot menu + in-game
# HUD to PNGs, then quit — self-verification). Same SpriteKit y-up click
# coords, same WALL-CLOCK seconds (the nested asyncAfter deadlines converted
# to sequential waits, the menu_probe.gd approach).

func _run_uitest() -> void:
	if Audio.shared != null:
		Audio.shared.sfx_volume = 0.0
		Audio.shared.music_volume = 0.0
	menu.player_name = "Tester"     # main.swift:302 UserDefaults "playerName"
	await _wait(2.5)
	_snap("menu")
	_menu_click(400, 180)           # Single Player -> Your Name popup
	await _wait(0.8)
	_snap("name")
	_menu_click(527, 188)           # Enter -> New Game Settings
	await _wait(0.9)
	_snap("iris-close")             # iris mid-close: screen visible in the shrinking circle, black corners
	await _wait(0.7)                # t=1.6 after Enter
	_snap("transition")             # iris + hourglass mid-cover
	await _wait(2.4)                # t=4.0 after Enter
	_snap("settings")
	_menu_click(527, 188)           # Create -> lobby
	await _wait(1.5)
	_snap("lobby")
	_menu_click(200, 456)           # open slot 1's AI dropdown (SK y-up coords; src row center y=144)
	await _wait(0.5)
	_snap("lobbyopen")
	_menu_click(200, 329)           # pick "Thinker" for slot 1 (src y=144+31+3*32)
	await _wait(0.4)
	_snap("lobbyready")             # kick button should now show
	_menu_click(610, 280)           # open the color dropdown
	await _wait(0.4)
	_menu_click(610, 185)           # pick color 2 (src y=320+31+2*32; crash regression check)
	await _wait(0.8)                # t=1.2 after lobbyready
	_snap("lobbycolor")             # retinted preview, no crash
	_menu_click(610, 248)           # Begin The Game!
	await _wait(4.3)                # t=5.5
	_snap("game")                   # after the iris uncovers
	await _wait(10.0)
	# charge the power bar so the fill color is in the frame
	# (regression check: original fill is the orange POWERBAR texture)
	if game != null and game.local_human != null:
		game.local_human.power_bar_active = true
		game.local_human.power_level = 0.6
	await _wait(0.3)
	_snap("game2")
	if game != null and game.local_human != null:
		game.local_human.power_bar_active = false
		game.local_human.power_level = 0.0
	software_render = true
	await _wait(1.0)
	_snap("software")
	software_render = false
	# chat: long line must wrap in the 250px column; entry line shows SAY : + typed text
	if game != null and game.hud != null:
		game.hud.bot_chat("Tester", "this long chat line should wrap neatly inside the chat box instead of running off the right edge of the panel")
		game.hud.begin_chat_entry()
		_type_chat("ahoy there")
	await _wait(0.5)
	_snap("chat")                   # wrapped line + "SAY : ahoy there▍"
	if game != null:
		# esc closes the chat entry — but only send it when the HUD is up:
		# with hud_scene.gd absent nothing swallows it and it would fall
		# through to "esc → back to menu" (game_controller.key_down)
		if game.hud != null:
			game.key_down(_key_event(KEY_ESCAPE))
		game.click(Vector2(100, 589))               # open Options
	await _wait(0.5)                # t=1.0 after "software"
	_snap("options")                # Shadows / Sound / Music rows
	if game != null:
		game.click(Vector2(400, 300))               # close menu
		game.click(Vector2(315, 589))               # open Help!
	await _wait(0.5)                # t=1.5
	_snap("help")                   # Controls / How To Play / Tutorial rows
	if game != null:
		game.click(Vector2(400, 300))
		# forfeit eliminates the human outright → win banner → auto results
		game.menu_forfeit()
	await _wait(1.5)                # t=3.0
	_snap("gameover")               # "<bot> Wins!" message + success-camera orbit
	await _wait(16.0)               # t=19.0
	_snap("results")                # results screen at 15 s (Game_Loop.java:100)
	get_tree().quit()


func _menu_click(x: float, y: float) -> void:
	if menu != null:
		menu.click(Vector2(x, y))


func _key_event(keycode: Key, unicode: int = 0) -> InputEventKey:
	var e := InputEventKey.new()
	e.keycode = keycode
	e.pressed = true
	e.unicode = unicode
	return e


## main.swift:363-374 fakes one NSEvent whose `characters` is the whole line;
## Godot's InputEventKey carries one codepoint, so type it per character.
func _type_chat(text: String) -> void:
	if game == null:
		return
	for i in text.length():
		game.key_down(_key_event(KEY_NONE, text.unicode_at(i)))


func _wait(seconds: float) -> void:
	await get_tree().create_timer(seconds).timeout


# MARK: - Helpers

func _free_now(n: Node) -> void:
	if n == null or not is_instance_valid(n):
		return
	if n.get_parent() != null:
		n.get_parent().remove_child(n)
	n.queue_free()


func _snap(snap_name: String) -> void:
	var img := get_viewport().get_texture().get_image()
	# exported builds: res:// is the read-only pck — write beside the exe
	var dir := OS.get_executable_path().get_base_dir().path_join("snapshots") \
			if not OS.has_feature("editor") \
			else ProjectSettings.globalize_path("res://snapshots")
	DirAccess.make_dir_recursive_absolute(dir)
	var out := dir.path_join("uitest-%s.png" % snap_name)
	img.save_png(out)
	print("uitest wrote ", out)
