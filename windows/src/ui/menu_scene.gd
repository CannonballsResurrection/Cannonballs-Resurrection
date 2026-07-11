class_name MenuScene
extends CanvasLayer
# Port of macos/Sources/Cannonballs/MenuScene.swift.
#
# Main menu, rebuilt as a faithful clone of the original screens. Everything is
# laid out in the original's 800x600 screen space (coordinates extracted from
# the game's UI layout); project stretch mode `canvas_items` scales the canvas
# to the window, so the art stays chunky and proportional exactly like 2002.
#
# Screens:
#  - Main: MAIN backdrop (logo art) + centered stack of purple text buttons.
#  - New Game Settings: centered 512x256 popup with map thumb + option rows.
#
# COORDINATES (PORTING.md "HUD / menus"): the Swift lays out in SpriteKit
# (y-up, bottom-left origin) and converts source top-left coords through
# `P(x,y) = (x, 600-y)`. Godot's canvas is ALREADY y-down top-left, so the
# original source coordinates are used directly and P() drops out. The one
# place SpriteKit coords still appear is the public click()/hover()/test
# surface (see click() below), converted in that ONE visible place. Relative
# child offsets flip their y sign (SK +y is up): each flip is marked "BL->TL".
#
# Delegate surface (the Swift closures; GameController/boot wires these):
#   on_start          -> called with a G.GameOptions      (Swift onStart)
#   on_cannon_preview -> called with an int color index to show/tint the
#                        slow-spinning 3D cannon preview behind the lobby
#                        screen, or null to remove it     (Swift onCannonPreview)
#   on_quit           -> optional; if unset, "Quit" quits the SceneTree
#                        (Swift: NSApp.terminate(nil))


var on_start := Callable()
var on_cannon_preview := Callable()
var on_quit := Callable()

# UserDefaults analogue for "playerName" / "cannonColor" (separate file from
# Audio's user://options.cfg so neither module clobbers the other).
const PLAYER_CFG := "user://player.cfg"

# ---- State ----

enum Screen { MAIN, NAME_ENTRY, SETTINGS, LOBBY }

var screen: int = Screen.MAIN
var player_name := ""
var selected_map := 0
var player_count := 4         # "Maximum # of Players" (You + bots)
var lives := 2
var gold_idx := 4
var hotseat_idx := 0
var treasure := true

# lobby state
var color_index := 0
var _color_open := false
const COLOR_NAMES := ["Blue", "Purple", "Red", "Green"]

# One AI type per slot (slot 0 = You). 0=none, 1=Dummy, 2=Aggressive, 3=Thinker, 4=Crazy —
# exactly the original's per-slot "Add AI Player" dropdown options.
const AI_OPTIONS := ["none", "Dummy", "Aggressive", "Thinker", "Crazy"]
var _slot_types: Array = []
var _slot_names := {}         # slot -> roster name rolled at assignment (Network.java:204 random pick)
var _open_slot: Variant = null   # int slot index, or null

var _root := Node2D.new()
var _modal := Node2D.new()

# Hoverable buttons: name -> {node, normal, hover, size, z}
# (z = layer-global z used for topmost-first hit ordering, the analogue of
# SKScene.nodes(at:) returning the frontmost named node first.)
var _hover_buttons := {}
var _hover_arrows := {}       # name -> {node, size, z}
var _extra_click := {}        # bare named text nodes ("quit") -> {node, size, z}

var _name_field: Sprite2D = null
var _map_thumb: Sprite2D = null
var _value_sprites := {}
var _last_hovered: Variant = null


## The original color square (Button_3DDropColor.createColor): a black 32x32
## bitmap with the color filled at (2,2)-(20,14), shown cropped to 23x17 —
## i.e. the color sits inside a small black border.
static func color_swatch(p_color_index: int) -> ImageTexture:
	var img := Image.create(23, 17, false, Image.FORMAT_RGBA8)
	img.fill(Color.BLACK)
	# Swift fills the BL rect (2, 17-14, 18, 12); in top-left coords that is
	# exactly the source's (2,2)-(20,14) (BL->TL).
	img.fill_rect(Rect2i(2, 2, 18, 12), G.COLOR_RGB[p_color_index % 4])
	return ImageTexture.create_from_image(img)


func _init() -> void:
	var cfg := ConfigFile.new()
	if cfg.load(PLAYER_CFG) == OK:
		player_name = cfg.get_value("player", "playerName", "")
		color_index = cfg.get_value("player", "cannonColor", 0)
	add_child(_root)
	_build_main()
	_modal.visible = false
	_modal.z_index = 50
	add_child(_modal)


func _ready() -> void:
	# GameViewController.swift:150 — the title track starts when the menu shows.
	if Audio.shared != null:
		Audio.shared.play_music("title")


func _save_player() -> void:
	var cfg := ConfigFile.new()
	cfg.load(PLAYER_CFG)   # keep whatever else is there
	cfg.set_value("player", "playerName", player_name)
	cfg.set_value("player", "cannonColor", color_index)
	cfg.save(PLAYER_CFG)


## The window scales the fixed 800x600 canvas; nothing to relayout.
func relayout(_size: Vector2) -> void:
	pass


# ---- helpers ----

func _sprite(tex: Texture2D, pos: Vector2, z: int = 1) -> Sprite2D:
	var n := Sprite2D.new()
	n.texture = tex
	# Swift: SKTexture.filteringMode = .nearest — a CanvasItem property in Godot.
	n.texture_filter = CanvasItem.TEXTURE_FILTER_NEAREST
	n.position = pos
	n.z_index = z
	return n


func _text(s: String, cap: float, tint: Variant = null) -> Sprite2D:
	var n := Sprite2D.new()
	n.texture = HUDArt.text(s, cap, tint)
	return n


## SKSpriteNode.size analogue: Sprite2D has no size, so size becomes scale.
## Always called right after the texture is set (project memory: size before
## any scale animation).
static func _set_size(n: Sprite2D, size: Vector2) -> void:
	if n.texture != null:
		n.scale = size / Vector2(n.texture.get_size())


func _layer_z(parent: Node2D, z: int) -> int:
	return (_modal.z_index if parent == _modal else 0) + z


## Original-style 252x28 text button with hover state.
func _add_text_button(label: String, src: Vector2, btn_name: String,
		parent: Node2D, dimmed: bool = false, z: int = 2) -> Sprite2D:
	var normal := HUDArt.menu_button(false)
	var hover := HUDArt.menu_button(true)
	var b := _sprite(normal, src, z)
	_set_size(b, Vector2(256, 32))    # Button_3D plane 0.700416x0.087552 NDC
	if dimmed:
		b.modulate.a = 0.55
	var l := _text(label, 24)         # Button_3D label = Message_3D f=1.0
	# Button_3D.show: centered label at (TopLeftX+128, TopLeftY+16) = center +2,+2
	l.position = Vector2(2, 2)        # Swift (2,-2) in y-up child space (BL->TL)
	b.add_child(l)
	parent.add_child(b)
	_hover_buttons[btn_name] = {
		"node": b, "normal": normal, "hover": hover,
		"size": Vector2(256, 32), "z": _layer_z(parent, z),
	}
	return b


## 26x27 controls-sheet arrow button with hover state.
func _add_arrow(s: HUDArt.ControlSprite, src: Vector2, btn_name: String, parent: Node2D) -> void:
	# Swift centers the sprite at (P.x+13, P.y-13): half the 26x27 crop right
	# and DOWN from the source top-left coord (BL->TL: -13 up becomes +13 down).
	var b := _sprite(HUDArt.control(s), Vector2(src.x + 13, src.y + 13), 3)
	parent.add_child(b)
	_hover_arrows[btn_name] = {"node": b, "size": Vector2(26, 27), "z": _layer_z(parent, 3)}


## Drop stale registry entries when a layer rebuilds (Swift filters
## hoverButtons by prefix; arrows are filtered too here because their nodes
## are freed with the modal and Godot would error writing to freed nodes).
func _drop_entries(prefixes: Array) -> void:
	for dict in [_hover_buttons, _hover_arrows]:
		for key in dict.keys().duplicate():
			for p in prefixes:
				if String(key).begins_with(p):
					dict.erase(key)
					break


static func _clear_children(n: Node2D) -> void:
	for c in n.get_children():
		n.remove_child(c)
		c.queue_free()


# ---- Main screen ----

func _build_main() -> void:
	# Backdrop: the decoded MAIN .wjp art is natively 800x600 — 1:1, pixel exact.
	var bg := Assets.texture("MENUS/MAIN.png")
	if bg != null:
		var n := _sprite(bg, Vector2(400, 300), -2)
		_set_size(n, Vector2(800, 600))
		_root.add_child(n)

	# Top magenta menu bar is baked into the MAIN.png backdrop art (~26px tall);
	# drawing topbar.png over it left the baked bar peeking out below as a
	# "duplicate". Only the menu text is ours, on the art's own bar.
	var quit := _text("Quit", 24)
	quit.position = Vector2(10 + quit.texture.get_width() / 2.0, 14)   # Swift y = 600-14 (BL->TL)
	quit.z_index = 6
	_root.add_child(quit)
	_extra_click["quit"] = {"node": quit, "size": Vector2(quit.texture.get_size()), "z": 6}
	var title := _text("Cannonballs! v1.869", 18)   # ref frame_001   # Main.VersionNumber; f=0.75
	title.position = Vector2(400, 14)
	title.z_index = 6
	_root.add_child(title)

	# Original button stack (centered x=400). Online-only entries are shown for
	# authenticity but dimmed — the 2002 services are gone.
	_add_text_button("Guest Login", Vector2(400, 380), "dead-guest", _root, true)
	_add_text_button("Single Player", Vector2(400, 420), "single", _root)
	_add_text_button("View Leaderboards", Vector2(400, 460), "dead-stats", _root, true)
	_add_text_button("Register Now!", Vector2(400, 520), "dead-register", _root, true)


# ---- "Your Name" popup (shown before New Game Settings) ----

func _build_name_entry() -> void:
	_clear_children(_modal)
	_drop_entries(["m-"])

	var veil := ColorRect.new()
	veil.color = Color(0, 0, 0, 0.55)
	veil.position = Vector2(400 - 410, 300 - 310)   # 820x620 centered at (400,300)
	veil.size = Vector2(820, 620)
	veil.z_index = -1
	# All menu interaction is hit-tested manually in click()/_unhandled_input;
	# a default-STOP Control veil would eat mouse clicks in the GUI phase before
	# _unhandled_input runs (keyboard still works), leaving Enter/Cancel dead.
	veil.mouse_filter = Control.MOUSE_FILTER_IGNORE
	_modal.add_child(veil)

	var panel := _sprite(HUDArt.popup_panel(), Vector2(400, 300), 0)
	_modal.add_child(panel)
	var title := _text("Your Name", 24)   # PopUp title f=1.0 at (154,188)
	title.position = Vector2(154 + title.texture.get_width() / 2.0, 188)
	title.z_index = 1
	_modal.add_child(title)

	var label := _text("Name:", 24)
	label.position = Vector2(232, 290)
	label.z_index = 1
	_modal.add_child(label)
	var field := _sprite(HUDArt.text_bar(320, 26), Vector2(440, 290), 1)
	_modal.add_child(field)
	_name_field = Sprite2D.new()
	# anchorPoint (0, 0.5): left edge at position, vertically centered.
	_name_field.centered = false
	_name_field.position = Vector2(290, 290)
	_name_field.z_index = 2
	_modal.add_child(_name_field)
	_refresh_name_field()

	_add_text_button("Cancel", Vector2(273, 412), "m-name-cancel", _modal, false, 2)
	_add_text_button("Enter", Vector2(527, 412), "m-name-enter", _modal, false, 2)


func _refresh_name_field() -> void:
	var shown := "_" if player_name.is_empty() else player_name + "_"
	var tex := HUDArt.text(shown, 24)
	_name_field.texture = tex
	_name_field.offset = Vector2(0, -tex.get_height() / 2.0)   # keep the (0, 0.5) anchor


func _confirm_name() -> void:
	var trimmed := player_name.strip_edges()
	if trimmed.is_empty():
		return
	player_name = trimmed
	_save_player()
	# Original flow: dissolve out -> loading hourglass -> dissolve in
	# (Menu_Manager cases 102/100/101 + the skull hourglass @0.4s/frame).
	_iris_transition(func() -> void:
		screen = Screen.SETTINGS
		_build_settings())


## The original telescoping screen transition (Menu_Manager.java:246-292):
## the iris CLOSES over the old screen (TRANSITION sheet scale 1.0 -> 0.001
## over 1s), the hourglass spins while the screen switches under black,
## then the iris OPENS onto the new screen. The TRANSITION actor is a huge
## black sheet with a soft-rimmed circular hole (see
## IrisTransition.sheet_image()); four black arm sprites stand in for the
## mesh's giant quad around the punched 512px center.
func _iris_transition(swap: Callable) -> void:
	var sheet_img := IrisTransition.sheet_image()
	if sheet_img == null:
		swap.call()
		return
	var iris := Node2D.new()
	iris.position = Vector2(400, 300)
	iris.z_index = 500
	var hole := Sprite2D.new()
	# compensated() is a no-op in Godot (PORTING.md "Overlay alpha") but is
	# called where the Swift calls it, to document itself.
	hole.texture = ImageTexture.create_from_image(HUDArt.compensated(sheet_img))
	# texture is natively 512x512 = the Swift hole.size; scale rides on `iris`.
	iris.add_child(hole)
	for r in IrisTransition.arm_rects():
		var arm := ColorRect.new()
		arm.color = Color.BLACK
		arm.position = r.position
		arm.size = r.size
		arm.mouse_filter = Control.MOUSE_FILTER_IGNORE   # visual only; never intercept input
		iris.add_child(arm)
	# Swift passes the SKScene size (fixed 800x600); the Godot canvas is the
	# same fixed 800x600 space regardless of window size.
	var open := IrisTransition.open_scale(Vector2(800, 600))
	iris.scale = Vector2(open, open)   # scale 1.0 = iris fully open (hole past the corners)
	add_child(iris)
	var frames: Array = []
	for i in 6:
		@warning_ignore("integer_division")
		frames.append(HUDArt.crop_texture("hourglass.png",
				Rect2((i % 4) * 64, (i / 4) * 128, 64, 128)))
	var tw := create_tween()
	tw.tween_property(iris, "scale", Vector2(0.002, 0.002), 1.0)   # iris closes (case 102)
	tw.tween_callback(func() -> void:
		swap.call()
		var hg := Sprite2D.new()
		hg.texture = frames[0]
		hg.position = Vector2(400, 300)
		hg.z_index = 600
		_set_size(hg, Vector2(64, 128))
		add_child(hg)
		var f := [0]
		var htw := hg.create_tween()
		htw.set_loops(3)   # .repeat(..., count: 3): 3 frame steps of 0.4s
		htw.tween_callback(func() -> void:
			f[0] = (f[0] + 1) % 6
			hg.texture = frames[f[0]])
		htw.tween_interval(0.4)
		htw.finished.connect(hg.queue_free))
	tw.tween_interval(1.2)
	tw.tween_property(iris, "scale", Vector2(open, open), 1.0)     # iris opens (case 101)
	tw.tween_callback(iris.queue_free)


# ---- Lobby screen (Joining Players + color picker + spinning cannon) ----

func open_lobby() -> void:
	screen = Screen.LOBBY
	_root.visible = false      # full-screen lobby replaces the main screen entirely
	_color_open = false
	_open_slot = null
	# Slots: index 0 = You; 1..player_count-1 = AI slots. (The Swift carries an
	# unused `defaults = [3, 2, 4, 1, 3, 2, 4]` mix; slots start empty until an
	# AI is assigned, exactly as it actually behaves.)
	_slot_types = []
	for i in maxi(1, player_count):
		_slot_types.append(-1 if i == 0 else 0)   # empty until AI assigned
	_slot_names = {}
	_build_lobby()
	_modal.visible = true
	_cannon_preview(color_index)


func _cannon_preview(color: Variant) -> void:
	if on_cannon_preview.is_valid():
		on_cannon_preview.call(color)


func _build_lobby() -> void:
	_clear_children(_modal)
	_drop_entries(["l-", "m-"])

	# Full-screen JOIN backdrop with the right-hand scenery window punched
	# transparent so the 3D spinning cannon preview (rendered behind this
	# CanvasLayer, on a scene whose background is this same art) shows through.
	# The original renders the cannon camera-space over the whole screen, so the
	# punch must cover the entire window frame or the cannon clips: art frame is
	# x 406..800, y 29..369 (source top-left coords).
	var bg := Assets.image("MENUS/JOIN.png")
	if bg != null:
		bg.convert(Image.FORMAT_RGBA8)
		# destinationOut over an opaque fill zeroes the rect. Swift punches the
		# BL rect (406, 600-369, 394, 340); top-left coords: (406, 29) (BL->TL).
		bg.fill_rect(Rect2i(406, 29, 394, 340), Color(0, 0, 0, 0))
		var n := _sprite(ImageTexture.create_from_image(bg), Vector2(400, 300), -1)
		_set_size(n, Vector2(800, 600))
		_modal.add_child(n)

	# Top bar: the lobby re-shows the version centered (showCreateJoinMenu:
	# Message_3D(VersionNumber, 1, 1.0).show(400, 16)).
	var ver := _text("Cannonballs! v1.869", 24)
	ver.position = Vector2(400, 16)
	ver.z_index = 1
	_modal.add_child(ver)

	# ---- Joining Players panel (left) ----
	# Source layout (Menu_Lobby_Screen / Button_3D semantics — Message_3D.show(x,y)
	# is LEFT edge + vertical CENTERLINE): rows share the centerline y=112+i*32;
	# names (f=0.75) at x=10; IconCheck 24px top-left (330,100+i*32); Kick is the
	# Controls-sheet LINE button, 45x26 top-left (355,100+i*32); empty slots carry
	# a 256x32 Button_3DDrop centered at x=200 — rows TOUCH (pitch = art height).
	# The JOIN background art already CONTAINS the lobby windows (frame_002);
	# drawing our own panel doubled it. Only the title text is ours.
	var title := _text("Joining Players", 24)   # TitleRoom f=1.0 at (14,82)
	title.position = Vector2(14 + title.texture.get_width() / 2.0, 82)
	title.z_index = 1
	_modal.add_child(title)

	var bot_name_idx := 0
	for slot in _slot_types.size():
		var cy := 112.0 + slot * 32.0             # row centerline (source y)
		if slot == 0:
			var l := _text("You" if player_name.is_empty() else player_name, 18)
			l.position = Vector2(10 + l.texture.get_width() / 2.0, cy)
			l.z_index = 1
			_modal.add_child(l)
			_add_check(cy)                        # host is checked in
		elif _slot_types[slot] > 0:
			# assigned bot: blue roster name + check + Kick (the LINE button)
			var bn: String = _slot_names.get(slot, G.BOT_NAMES[bot_name_idx % G.BOT_NAMES.size()])
			bot_name_idx += 1
			var l := Sprite2D.new()
			l.texture = HUDArt.text(bn, 18, null, 0.0, HUDArt.FontColor.BLUE)
			l.position = Vector2(10 + l.texture.get_width() / 2.0, cy)
			l.z_index = 1
			_modal.add_child(l)
			_add_check(cy)
			# original Kick art: controls sheet (57,154) normal / (57,128) hover
			var kick_normal := HUDArt.crop_texture("controls.png", Rect2(57, 154, 45, 26))
			var kick_hover := HUDArt.crop_texture("controls.png", Rect2(57, 128, 45, 26))
			var kb := _sprite(kick_normal, Vector2(377.5, cy + 1), 2)
			_set_size(kb, Vector2(45, 26))
			_modal.add_child(kb)
			_hover_buttons["l-kick-%d" % slot] = {
				"node": kb, "normal": kick_normal, "hover": kick_hover,
				"size": Vector2(45, 26), "z": _layer_z(_modal, 2),
			}
		else:
			# empty slot: 'Add AI Player' dropdown (Button_3DDrop at x=200,
			# label f=1.0 left-aligned at TopLeftX+10 = center-116, +2 down).
			var is_open: bool = _open_slot == slot
			var normal := HUDArt.drop_row(false)
			var hovered := HUDArt.drop_row(true)
			var btn := _sprite(hovered if is_open else normal, Vector2(200, cy), 2)
			_set_size(btn, Vector2(256, 32))
			_modal.add_child(btn)
			# an open dropdown stays red until it rerolls (original setState never resets)
			_hover_buttons["l-slot-%d" % slot] = {
				"node": btn, "normal": hovered if is_open else normal, "hover": hovered,
				"size": Vector2(256, 32), "z": _layer_z(_modal, 2),
			}
			var lbl := _text("Add AI Player", 24)
			lbl.position = Vector2(-116 + lbl.texture.get_width() / 2.0, 2)   # Swift y -2 (BL->TL)
			btn.add_child(lbl)
	# Expanded AI-type menu for the open slot (drawn last so it overlays).
	# Original rollout: rows on the arrow-less rollout art, pitch 32, first row
	# at button center +31; item text f=1.0 at TopLeftX+10, centerline +34+32n;
	# the hovered row shows the red button-art overlay (Buttons bottom half).
	if _open_slot != null and int(_open_slot) < _slot_types.size():
		var s: int = _open_slot
		var cy := 112.0 + s * 32.0
		var row_normal := HUDArt.rollout_row()
		var row_hover := HUDArt.crop_texture("buttons_button.png", Rect2(0, 32, 256, 30))
		for opt in AI_OPTIONS.size():
			var row := _sprite(row_normal, Vector2(200, cy + 31 + opt * 32.0), 6)
			_set_size(row, Vector2(256, 32))
			_modal.add_child(row)
			_hover_buttons["l-settype-%d-%d" % [s, opt]] = {
				"node": row, "normal": row_normal, "hover": row_hover,
				"size": Vector2(256, 32), "z": _layer_z(_modal, 6),
			}
			var l := _text(AI_OPTIONS[opt], 24)
			l.position = Vector2(-116 + l.texture.get_width() / 2.0, 3)   # Swift y -3 (BL->TL)
			row.add_child(l)

	# ---- right column ----
	_add_text_button("Abandon This Game", Vector2(610, 70), "l-abandon", _modal, false, 2)

	# Color dropdown (Button_3DDropColor at 610,320): full 256x32 dropdown art,
	# black-bordered color swatch at center-108, "Color" label left-aligned at
	# TopLeftX+50 = center-76. The rollout rows carry ONLY swatches (no labels).
	var drop_normal := HUDArt.drop_row(false)
	var drop_hover := HUDArt.drop_row(true)
	var col_btn := _sprite(drop_hover if _color_open else drop_normal, Vector2(610, 320), 2)
	_set_size(col_btn, Vector2(256, 32))
	_modal.add_child(col_btn)
	_hover_buttons["l-color"] = {
		"node": col_btn, "normal": drop_hover if _color_open else drop_normal, "hover": drop_hover,
		"size": Vector2(256, 32), "z": _layer_z(_modal, 2),
	}
	var swatch := Sprite2D.new()
	swatch.texture = color_swatch(color_index % 4)
	swatch.position = Vector2(-108, 0)
	col_btn.add_child(swatch)
	var col_label := _text("Color", 24)
	col_label.position = Vector2(-76 + col_label.texture.get_width() / 2.0, 2)   # Swift y -2 (BL->TL)
	col_btn.add_child(col_label)

	if _color_open:
		var row_normal := HUDArt.rollout_row()
		var row_hover := HUDArt.crop_texture("buttons_button.png", Rect2(0, 32, 256, 30))
		for i in 4:
			var row := _sprite(row_normal, Vector2(610, 320 + 31 + i * 32.0), 5)
			_set_size(row, Vector2(256, 32))
			_modal.add_child(row)
			_hover_buttons["l-pick-%d" % i] = {
				"node": row, "normal": row_normal, "hover": row_hover,
				"size": Vector2(256, 32), "z": _layer_z(_modal, 5),
			}
			var sw := Sprite2D.new()
			sw.texture = color_swatch(i)
			sw.position = Vector2(-108, 4)   # Swift y -4 (BL->TL)
			row.add_child(sw)

	if not _color_open:
		_add_text_button("Begin The Game!", Vector2(610, 352), "l-begin", _modal, false, 2)


func _add_check(cy: float) -> void:
	var ck := _sprite(HUDArt.texture("iconcheck.png"), Vector2(342, cy), 2)
	_set_size(ck, Vector2(24, 24))
	_modal.add_child(ck)


func _handle_lobby_click(btn_name: String) -> void:
	if btn_name == "l-abandon":
		_cannon_preview(null)
		close_modal()
	elif btn_name == "l-begin":
		_cannon_preview(null)
		_start_game()
	elif btn_name == "l-color":
		_color_open = not _color_open
		_open_slot = null
		_build_lobby()
	elif btn_name.begins_with("l-pick-"):
		var i := btn_name.substr(7).to_int()
		color_index = i
		_save_player()
		_cannon_preview(i)
		_color_open = false
		_build_lobby()
	elif btn_name.begins_with("l-slot-"):
		var s := btn_name.substr(7).to_int()
		_open_slot = null if _open_slot != null and int(_open_slot) == s else s   # toggle this slot's AI dropdown
		_color_open = false
		_build_lobby()
	elif btn_name.begins_with("l-settype-"):
		var parts := btn_name.split("-")   # ["l", "settype", slot, opt]
		if parts.size() >= 4:
			var slot := parts[2].to_int()
			var opt := parts[3].to_int()
			# roll a random unused roster name for the new bot (Network.java:204)
			if opt > 0 and not _slot_names.has(slot):
				var used := _slot_names.values()
				var free_names := G.BOT_NAMES.filter(func(n: String) -> bool: return not used.has(n))
				_slot_names[slot] = free_names.pick_random() if not free_names.is_empty() \
						else G.BOT_NAMES.pick_random()
			if slot >= 0 and slot < _slot_types.size():
				_slot_types[slot] = opt   # 0=none removes the AI; 1..4 set the difficulty
		_open_slot = null
		_build_lobby()
	elif btn_name.begins_with("l-kick-"):
		var s := btn_name.substr(7).to_int()
		_slot_names.erase(s)
		if s >= 0 and s < _slot_types.size():
			_slot_types[s] = 0
		_open_slot = null
		_build_lobby()


# ---- New Game Settings modal (popup centered at 400,300 in source space) ----

func _build_settings() -> void:
	_clear_children(_modal)
	_drop_entries(["m-"])

	# dim veil over the main screen (like the original's modal state)
	var veil := ColorRect.new()
	veil.color = Color(0, 0, 0, 0.55)
	veil.position = Vector2(400 - 410, 300 - 310)
	veil.size = Vector2(820, 620)
	veil.z_index = -1
	veil.mouse_filter = Control.MOUSE_FILTER_IGNORE   # see _build_name_entry: don't eat clicks
	_modal.add_child(veil)

	# 512x256 panel from the original popup halves, centered (400,300).
	var panel := _sprite(HUDArt.popup_panel(), Vector2(400, 300), 0)
	_modal.add_child(panel)

	# Title in the magenta header (left-aligned like the original).
	var title := _text("New Game Settings", 24)
	title.position = Vector2(152 + title.texture.get_width() / 2.0, 185)
	title.z_index = 1
	_modal.add_child(title)

	# Map thumbnail between its cycle arrows (arrows at src 148,287 / 278,287).
	_add_arrow(HUDArt.ControlSprite.ARROW_LEFT, Vector2(148, 287), "m-mapdown", _modal)
	_add_arrow(HUDArt.ControlSprite.ARROW_RIGHT, Vector2(278, 287), "m-mapup", _modal)
	_map_thumb = Sprite2D.new()
	_map_thumb.texture_filter = CanvasItem.TEXTURE_FILTER_NEAREST
	_map_thumb.position = Vector2(226, 290)
	_map_thumb.z_index = 1
	_modal.add_child(_map_thumb)

	# Option rows: label right-aligned to x=530, arrows at 540/620, value bar at 568..638.
	var rows: Array = [
		["players", "Maximum # of Players"],
		["lives", "Starting # of Lives"],
		["gold", "Starting Gold"],
		["seat", "HotSeat Mode"],
		["treasure", "Treasures Respawn"],
		["team", "Team Play"],
	]
	for i in rows.size():
		var y_src := 203.0 + i * 30.0
		var label := _text(rows[i][1], 18)
		label.position = Vector2(530 - label.texture.get_width() / 2.0, y_src + 13.5)
		label.z_index = 1
		_modal.add_child(label)
		_add_arrow(HUDArt.ControlSprite.ARROW_LEFT, Vector2(540, y_src), "m-%s-down" % rows[i][0], _modal)
		_add_arrow(HUDArt.ControlSprite.ARROW_RIGHT, Vector2(620, y_src), "m-%s-up" % rows[i][0], _modal)
		var bar := _sprite(HUDArt.text_bar(56, 24), Vector2(593, y_src + 13.5), 1)
		_modal.add_child(bar)
		var v := Sprite2D.new()
		v.position = bar.position
		v.z_index = 2
		_value_sprites[rows[i][0]] = v
		_modal.add_child(v)

	_add_text_button("Cancel", Vector2(273, 412), "m-cancel", _modal, false, 2)
	_add_text_button("Create", Vector2(527, 412), "m-create", _modal, false, 2)
	_refresh_settings()


func _refresh_settings() -> void:
	var map = MapCatalog.maps[selected_map]
	# Original thumbnails carry their stylized map name baked in — no extra label.
	var thumb := Assets.texture(map.thumb_path())
	if thumb != null:
		_map_thumb.texture = thumb
		_set_size(_map_thumb, Vector2(96, 88))

	_set_value("players", "%d" % player_count)
	_set_value("lives", "%d" % lives)
	_set_value("gold", "%d" % G.STARTING_CASH_TABLE[gold_idx])
	_set_value("seat", "NA" if hotseat_idx == 0 else "%d" % G.HOT_SEAT_TIMES[hotseat_idx])
	_set_value("treasure", "yes" if treasure else "no")
	_set_value("team", "NA", true)


func _set_value(key: String, s: String, dim: bool = false) -> void:
	if not _value_sprites.has(key):
		return
	var n: Sprite2D = _value_sprites[key]
	n.texture = HUDArt.text(s, 18, null, 1.0,
			HUDArt.FontColor.GRAY if dim else HUDArt.FontColor.WHITE)


# ---- Interaction ----

## Keyboard entry point (Swift keyDown, dispatched by GameViewController).
## Returns true when the event was consumed.
func key_down(event: InputEventKey) -> bool:
	match screen:
		Screen.NAME_ENTRY:
			if event.keycode == KEY_ENTER:                          # return (keyCode 36)
				_confirm_name()
				return true
			if event.keycode == KEY_ESCAPE:                         # esc (keyCode 53)
				close_modal()
				return true
			if event.keycode == KEY_BACKSPACE:                      # backspace (keyCode 51)
				if not player_name.is_empty():
					player_name = player_name.substr(0, player_name.length() - 1)
				_refresh_name_field()
				return true
			if event.unicode > 0 and player_name.length() < 12:
				var ch := char(event.unicode)
				# Swift filters isLetter || isNumber
				if ch.to_lower() != ch.to_upper() or (ch >= "0" and ch <= "9"):
					player_name += ch
					_refresh_name_field()
					return true
			return false
		Screen.SETTINGS:
			if event.keycode == KEY_ENTER:
				open_lobby()
				return true
			if event.keycode == KEY_ESCAPE:
				close_modal()
				return true
			return false
		Screen.LOBBY:
			if event.keycode == KEY_ENTER:
				_cannon_preview(null)
				_start_game()
				return true
			if event.keycode == KEY_ESCAPE:
				_cannon_preview(null)
				close_modal()
				return true
			return false
		Screen.MAIN:
			if event.keycode == KEY_ENTER:
				open_name_entry()
				return true
			return false
	return false


func _active_prefix() -> Variant:
	match screen:
		Screen.MAIN:
			return null
		Screen.LOBBY:
			return "l-"
		_:   # NAME_ENTRY, SETTINGS
			return "m-"


func _in_layer(btn_name: String, active_prefix: Variant) -> bool:
	if active_prefix == null:
		return not btn_name.begins_with("m-") and not btn_name.begins_with("l-")
	return btn_name.begins_with(active_prefix)


## Hit rect in canvas coords. Buttons live directly under root/modal, which
## both sit at the scene origin (same note as the Swift hover()).
static func _hit_rect(entry: Dictionary) -> Rect2:
	var node: Node2D = entry.node
	var size: Vector2 = entry.size
	return Rect2(node.position - size / 2.0, size)


## Pointer-move entry point (Swift hover, dispatched by GameViewController).
## `p_sk` uses the same SpriteKit y-up convention as click() — see there.
func hover(p_sk: Vector2) -> void:
	var p := Vector2(p_sk.x, 600.0 - p_sk.y)   # SpriteKit y-up -> canvas y-down (see click())
	var active_prefix: Variant = _active_prefix()
	# Sound_Over: play once when the pointer enters a button.
	var now_hovered: Variant = null
	for btn_name in _hover_buttons:
		if _in_layer(btn_name, active_prefix) and _hit_rect(_hover_buttons[btn_name]).has_point(p):
			now_hovered = btn_name
			break
	if now_hovered != null and now_hovered != _last_hovered and Audio.shared != null:
		Audio.shared.play("hover", 0.5)
	_last_hovered = now_hovered

	for btn_name in _hover_buttons:
		var entry: Dictionary = _hover_buttons[btn_name]
		var over: bool = _in_layer(btn_name, active_prefix) and _hit_rect(entry).has_point(p)
		var node: Sprite2D = entry.node
		node.texture = entry.hover if over else entry.normal
		_set_size(node, entry.size)   # keep the SKSpriteNode fixed-size semantics across swaps
	for btn_name in _hover_arrows:
		var entry: Dictionary = _hover_arrows[btn_name]
		var over := _hit_rect(entry).has_point(p)
		var base := HUDArt.ControlSprite.ARROW_LEFT if btn_name.ends_with("down") \
				else HUDArt.ControlSprite.ARROW_RIGHT
		entry.node.texture = HUDArt.control(base, over)


## Click entry point, and the `--uitest` harness API (macOS main.swift parity).
##
## COORDINATE CONVENTION: `p_sk` is in the ORIGINAL SpriteKit scene space the
## macOS harness uses — the 800x600 canvas with the origin at the BOTTOM-LEFT
## and +y UP (main.swift:303 menuClick passes scene points straight through;
## e.g. (400,180) hits Single Player, whose source top-left y is 420, and
## (527,188) hits Enter/Create at source y 412). It converts to the canvas'
## y-down top-left space HERE, in this one visible place (PORTING.md
## "HUD / menus"); every layout coordinate in this file is already in source =
## canvas space. Returns true when a named node was hit.
func click(p_sk: Vector2) -> bool:
	var p := Vector2(p_sk.x, 600.0 - p_sk.y)
	# SKScene.nodes(at:).first analogue: frontmost named, visible node under p.
	var hits: Array = []
	for dict in [_hover_buttons, _hover_arrows, _extra_click]:
		for btn_name in dict:
			var entry: Dictionary = dict[btn_name]
			var node: Node2D = entry.node
			if node.is_visible_in_tree() and _hit_rect(entry).has_point(p):
				hits.append({"name": btn_name, "z": entry.z})
	if hits.is_empty():
		return false
	hits.sort_custom(func(a, b) -> bool: return a.z > b.z)
	var hit_name: String = hits[0].name
	if Audio.shared != null:
		Audio.shared.play("click")
	match screen:
		Screen.NAME_ENTRY:
			if hit_name == "m-name-enter":
				_confirm_name()
			elif hit_name == "m-name-cancel":
				close_modal()
			return true
		Screen.SETTINGS:
			_handle_settings_click(hit_name)
			return true
		Screen.LOBBY:
			_handle_lobby_click(hit_name)
			return true
		Screen.MAIN:
			if hit_name == "single":
				open_name_entry()
			elif hit_name == "quit":
				# Swift: NSApp.terminate(nil)
				if on_quit.is_valid():
					on_quit.call()
				else:
					get_tree().quit()
			elif hit_name.begins_with("dead-"):
				_flash("The 2002 online services are gone - Single Player works!")
			else:
				return false
			return true
	return false


func _handle_settings_click(btn_name: String) -> void:
	match btn_name:
		"m-cancel":
			close_modal()
		"m-create":
			open_lobby()
		"m-mapdown":
			selected_map = (selected_map + MapCatalog.maps.size() - 1) % MapCatalog.maps.size()
		"m-mapup":
			selected_map = (selected_map + 1) % MapCatalog.maps.size()
		"m-players-down":
			player_count = maxi(2, player_count - 1)
		"m-players-up":
			player_count = mini(8, player_count + 1)
		"m-lives-down":
			lives = maxi(0, lives - 1)
		"m-lives-up":
			lives = mini(5, lives + 1)
		"m-gold-down":
			gold_idx = maxi(0, gold_idx - 1)
		"m-gold-up":
			gold_idx = mini(G.STARTING_CASH_TABLE.size() - 1, gold_idx + 1)
		"m-seat-down":
			hotseat_idx = maxi(0, hotseat_idx - 1)
		"m-seat-up":
			hotseat_idx = mini(G.HOT_SEAT_TIMES.size() - 1, hotseat_idx + 1)
		"m-treasure-down", "m-treasure-up":
			treasure = not treasure
	_refresh_settings()


func open_name_entry() -> void:
	screen = Screen.NAME_ENTRY
	_build_name_entry()
	_modal.visible = true


## Direct entry to the settings popup (the Swift only reaches it through the
## name-popup iris; kept public for the flow/test harness).
func open_settings() -> void:
	screen = Screen.SETTINGS
	_build_settings()
	_modal.visible = true


func close_modal() -> void:
	screen = Screen.MAIN
	_root.visible = true
	_modal.visible = false


func _flash(s: String) -> void:
	var l := _text(s, 18)
	l.position = Vector2(400, 560)   # Swift SK point (400, 40): 40 up from the bottom (BL->TL)
	l.z_index = 80
	add_child(l)
	var tw := l.create_tween()
	tw.tween_interval(2.2)
	tw.tween_property(l, "modulate:a", 0.0, 0.6)
	tw.tween_callback(l.queue_free)


func _start_game() -> void:
	var opts := G.GameOptions.new()
	opts.map_index = selected_map
	opts.starting_cash_index = gold_idx
	opts.max_respawns = lives
	opts.hot_seat_index = hotseat_idx
	opts.treasure_respawn = treasure
	# You (chosen color) + one AI per non-"none" slot. Colors repeat past 4;
	# names drawn from the original bot roster.
	var you := "You" if player_name.is_empty() else player_name
	var players: Array = [G.PlayerConfig.new(you, color_index % 4, 0)]
	for slot in range(1, _slot_types.size()):
		if _slot_types[slot] > 0:
			players.append(G.PlayerConfig.new(
					_slot_names.get(slot, G.BOT_NAMES.pick_random()),
					(color_index + slot) % 4, _slot_types[slot]))
	if players.size() < 2:   # all slots set to "none" — bring one crewmate
		players.append(G.PlayerConfig.new(G.BOT_NAMES[0], (color_index + 1) % 4, 3))
	opts.players = players
	if on_start.is_valid():
		on_start.call(opts)


# ---- Direct input (interim) ----
# The macOS build routes events through GameViewController (handleClick /
# handleMove / handleKeyDown, GameViewController.swift:306-324); until
# game_controller.gd lands, the menu listens for its own input. Mouse event
# positions arrive in canvas coords (y-down, 800x600 under the canvas_items
# stretch); click()/hover() take the SpriteKit y-up convention, so the y flips
# here and flips back inside — the round trip keeps the public API identical
# to the macOS test harness.

func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventMouseButton and event.button_index == MOUSE_BUTTON_LEFT and event.pressed:
		if click(Vector2(event.position.x, 600.0 - event.position.y)):
			get_viewport().set_input_as_handled()
	elif event is InputEventMouseMotion:
		hover(Vector2(event.position.x, 600.0 - event.position.y))
	elif event is InputEventKey and event.pressed:
		if key_down(event):
			get_viewport().set_input_as_handled()
