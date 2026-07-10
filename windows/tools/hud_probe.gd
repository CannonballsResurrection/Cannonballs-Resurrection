extends SceneTree
# THROWAWAY probe for hud_scene.gd (delete after verification).
# Fakes a minimal GameController surface, adds the HUD, charges the power bar
# to 0.6, flashes a message, chats, and snapshots; then opens the Options and
# Help! menus via the SpriteKit-coords click() API (main.swift:380-395 parity).
# Run: godot --path windows --script tools/hud_probe.gd
# Compare: macos/snapshots/uitest-game2.png / uitest-game.png / uitest-help.png


class FakePlayer:
	var index := 0
	var name := "Tester"
	var color_index := 0
	var is_bot := false
	var active := true
	var dying := false
	var cash := 1000
	var respawns_used := 0
	var position := Vector3(320, 10, 320)
	var spin_angle := 40.0
	var tilt_angle := -20.0
	var last_tilt_marker := -12.0
	var power_bar_active := false
	var power_level := 0.0
	var last_power_level := 0.35
	var weapon_index := 0
	var kills := 0
	var misses := 0
	var deaths := 0
	var drownings := 0
	var gold_spent := 0


class FakeCamera:
	var mode: int = CameraController.Mode.SHOT
	var node: Camera3D = null
	func set_mode(m: int) -> void:
		mode = m


class FakeOptions:
	var max_respawns := 2
	var hot_seat_index := 0
	var starting_cash_index := 4


class FakeMap:
	var name := "Tropicali"
	var map_scale := 64.0


class FakeTerrain:
	var current := PackedFloat32Array()
	func _init() -> void:
		var g: int = Terrain.GRID
		current.resize(g * g)
		for j in g:
			for i in g:
				# round island blob so the minimap stain has something to show
				var dx := (i - 48) / 30.0
				var dz := (j - 48) / 30.0
				var d := sqrt(dx * dx + dz * dz)
				current[j * g + i] = maxf(-4.0, (1.0 - d) * 20.0)


class FakeWorld:
	var map := FakeMap.new()
	var terrain := FakeTerrain.new()


class FakeChest:
	var alive := true
	var position: Vector3
	func _init(p: Vector3) -> void:
		position = p


class FakeGame:
	var players: Array = []
	var current_player_index := 0
	var world := FakeWorld.new()
	var wind_direction := 120.0
	var wind_velocity := 43.0
	var options := FakeOptions.new()
	var chests: Array = []
	var hot_seat_remaining := 0.0
	var game_over := false
	var winner_index = null
	var camera := FakeCamera.new()
	var on_exit := Callable()
	var local_human = null
	var controlled_cannon = null
	func select_weapon(idx: int) -> void:
		controlled_cannon.weapon_index = idx
	func set_shadows_visible(_on: bool) -> void:
		pass
	func menu_forfeit() -> void:
		print("probe: menu_forfeit()")


var hud: HUDScene
var game := FakeGame.new()
var _driver: Node


func _init() -> void:
	var you := FakePlayer.new()
	var bot := FakePlayer.new()
	bot.index = 1
	bot.name = "CptHook"
	bot.is_bot = true
	bot.cash = 400
	bot.position = Vector3(430, 12, 280)
	game.players = [you, bot]
	game.local_human = you
	game.controlled_cannon = you
	game.chests = [FakeChest.new(Vector3(250, 4, 400)), FakeChest.new(Vector3(420, 4, 420))]

	hud = HUDScene.new()
	hud.game = game
	root.add_child(hud)
	hud.rebuild_static()

	# drive hud.update(dt) every frame the way game_controller will
	_driver = Node.new()
	_driver.set_script(null)
	root.add_child(_driver)
	root.process_frame.connect(func() -> void: hud.update(1.0 / 60.0))

	_run()


func _run() -> void:
	await _wait(0.5)
	# power bar charging at 0.6 (main.swift:344-351 game2 regression)
	game.players[0].power_bar_active = true
	game.players[0].power_level = 0.6
	hud.flash_message("Tester finds 250 gold!")
	hud.bot_chat("Tester", "this long chat line should wrap neatly inside the chat box instead of running off the right edge of the panel")
	hud.begin_chat_entry()
	for ch in "ahoy there":
		var ev := InputEventKey.new()
		ev.unicode = ch.unicode_at(0)
		hud.chat_key_down(ev)
	await _wait(0.6)
	_snap("hud-probe")
	# cancel chat, open Options (SK y-up coords, main.swift:380)
	var esc := InputEventKey.new()
	esc.keycode = KEY_ESCAPE
	hud.chat_key_down(esc)
	game.players[0].power_bar_active = false
	hud.click(Vector2(100, 589))
	await _wait(0.4)
	_snap("hud-options")
	hud.click(Vector2(400, 300))    # close
	hud.click(Vector2(290, 589))    # open Help! (title x 272..326)
	await _wait(0.4)
	_snap("hud-help")
	hud.click(Vector2(400, 300))
	# weapon dropdown open (button center source (677,69) -> SK y 531)
	hud.click(Vector2(677, 531))
	await _wait(0.4)
	_snap("hud-weapons")
	hud.click(Vector2(677, 531 - 32 * 6))   # pick a row
	# spectator + success flows
	hud.show_success_message("You Lose!")
	hud.enter_spectator_mode("CptHook")
	game.current_player_index = 1
	hud.refresh_dynamic()
	await _wait(0.4)
	_snap("hud-spectator")
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
