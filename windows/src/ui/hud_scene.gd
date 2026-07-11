class_name HUDScene
extends CanvasLayer
# Port of macos/Sources/Cannonballs/HUDScene.swift (which cites HUD.java).
#
# In-game HUD, rebuilt as a faithful clone of the original. Everything is laid
# out in the original's 800x600 screen space; project stretch mode
# `canvas_items` scales the canvas to the window (PORTING.md "HUD / menus").
#
# COORDINATES: HUD.java lays these pieces out in WT screen units: 0.002736 per
# pixel, origin at screen center, +y up (positions are quad centers, matching
# WTGroup.setPosition semantics). In this port the original PIXEL values (the
# wt() inputs) ARE the coordinates: the SpriteKit conversions (`P(x,y) =
# (x, 600-y)`, wt() y-up) drop out because Godot's canvas is already y-down
# top-left in source pixels. Verbatim WT constants are converted in ONE place,
# _wt() below. The one spot SpriteKit coords still appear is the public
# click()/hover() surface (see click()), for parity with the macOS uitest
# harness. z_index = Swift zPosition * 10 (its half-steps become ints).
#
# ROTATIONS: WT setBitmapOrientation(t) ≡ SpriteKit zRotation -t (pinned by
# the minimap arrow, HUDScene.swift:517); the canvas y-flip negates again, so
# in Godot it is rotation_degrees = +t. Every use site cites this.
#
# JAVA-OVER-SWIFT CORRECTIONS (PORTING.md rule 4: source outranks the Swift);
# each is cited at its use site:
#  - top menus sit at fixed x 10/70/165/260 (HUD.java:1060-1075), open on
#    HOVER (Button_3DMenu.checkBounds), dropdown rows are 254x32 rollout art
#    at 32px pitch with the red Buttons-sheet rollover highlight
#  - Quit menu item is "Forfeit Game" (HUD.java:1061); while the game runs it
#    forfeits (HUD.java:922-935 QUIT → forfeit packet), only afterwards exits
#  - title bar is the TopBar art 9-sliced to 800x30 at centerline y 15
#    (HUD.java:1057 Button_Bar(TopBar, 800, 30, 0, 15, 50))
#  - weapon button is the 256x32 GiltButtonsDrop art with NO ball icon:
#    name left at x+10, cost RIGHT-aligned at x+220, colored white/blue/gray
#    by the Enabled state (Button_3DWeapon.java; HUD.java:597-609); rows are
#    32px pitch f=1.0 text over the GiltRollout backdrop
#  - cash / lives / wind values center at x=721 / 721 / 719 (HUD.java:588,
#    :610, :321), not 715
#  - YOUR TURN and DEFENSE banners are PERSISTENT 128x128 quads at (400,480)
#    that pulse 1-|sin|*0.1 (HUD.java:277-283, showYourTurn/showDefense
#    setPosition(0,-0.49248)), not one-shot fades
#  - name tags ride a FIXED row (y=105; 45 in spectator) following only the
#    projected screen x; bar width = text+20; the 32x32 arrow hangs 23px
#    below and BOBS 5px on the current player (HUD.java:247-277)
#  - the turn list has the bobbing TurnArrow pointer at (92-5sin,160)
#    ((32-5sin,100) in spectator) (HUD.java:284-288)
#  - spectator: hideBar() pulls ALL FOUR bar quads + markers + weapon button
#    + coin + cash/lives bars, and the turn list moves to (55,100)
#    (HUD.java:94-113, updateNextUp); the spectator NAME is blue
#    (Camera.java:432 Message_3D(...,1))
#  - success message renders on the BLUE font sheet (HUD.java:165
#    Message_3D(string,1,1.0,80,1))
#  - hot-seat timer digits sit at (495,525) (Timer.java:68)
#  - chat line pitch is 28*0.75*0.75 = 15.75px (Message_3D.java:76 newline
#    advance x the group's absolute scale 0.75)
#  - chat entry line gets the 770px Button_Bar backdrop while typing
#    (Chat.java:92-99 showChatBar)
#  - chat has scroll up/down + min/max controls and the 26px scroll knob
#    (HUD.java:1041-1053 show(), :818-846 swapChatSize); minimized chat is
#    the 295x88 short popup at (5,486) with 3 lines from y 528
#  - results screen text is f=1.0 (cap 24), Done is the standard 256x32
#    button at (400,575), and the title's name is blue between backticks
#    (Menu_Results_Screen.java:76-138, :133)
#
# NOT PORTED (explicit): FULLSCREEN toggle button at (767,1) (windowing is the
# OS's here); scroll-button hold-to-repeat (HoldChatUp/Down) — one step per
# click; scroll-knob dragging (click-to-jump on the rail is ported,
# HUD.java:858-869); team-game flags/team chat; the hot-seat hourglass
# (MenuManager.showLoading); results-screen version line + FULLSCREEN button.
#
# The wind arrow weathervane is CameraController's 3D child (already ported
# there) — the HUD keeps only the "N mph" speed bar, like the Swift.

var game = null                    # GameController (duck-typed, PORTING.md)
var results_shown := false
var chatting := false

const WT_S := 0.002736             # HUD.java WT screen units per pixel


## The ONE visible conversion of a verbatim WT coordinate pair from the source
## into canvas pixels (WT: origin center, +y up → canvas: top-left, y down).
static func _wt(x: float, y: float) -> Vector2:
	return Vector2(400.0 + x / WT_S, 300.0 - y / WT_S)


# ---- scene nodes ----

var _root := Node2D.new()

# power / pitch (PowerBarModel patch + PowerMarker1 / PitchMarker1 / PitchMarker2)
var _hud_bits: Array = []          # HUDBits[0..3] frame quads (HUD.java:342-354)
var _power_fill_body: Sprite2D
var _power_fill_cap: Sprite2D
var _power_bone: Sprite2D
var _pitch_live: Sprite2D          # PitchMarker1: small marker, live tilt
var _pitch_last_bone: Sprite2D     # PitchMarker2: bone, last shot's tilt
var bar_visible := true            # HUD.BarVisible — false in spectator mode

# weapon dropdown (Button_3DWeapon at 677,69)
var _weapon_button: Sprite2D
var _weapon_title: Sprite2D
var _weapon_cost: Sprite2D
var _weapon_dropdown := Node2D.new()
var _weapon_hover_row: Sprite2D
var _dropdown_open := false
var _weapon_rollover := -1

# top menus (Button_3DMenu; fixed x per HUD.java:1060-1075)
const MENU_X := {"Quit": 10.0, "Options": 70.0, "Camera": 165.0, "Help!": 260.0}
const MENU_Y := 14.0
const MENU_ORDER := ["Quit", "Options", "Camera", "Help!"]
var _menu_titles := {}             # name -> Sprite2D (title text)
var _menu_dropdown := Node2D.new()
var _menu_hover_row: Sprite2D
var _open_menu: Variant = null     # String menu name, or null
var _open_menu_items: Array = []   # [[label, action], ...]
var _menu_rollover := -1
var _camera_menu_removed := false  # hideBar(): Camera menu gone, Help! at 165

# right column
var _coin: Sprite2D
var _coin_frame := 0
var _coin_timer := 0.0
var _gold_text: Sprite2D
var _lives_label: Sprite2D
var _lives_text: Sprite2D
var _cash_bar: Sprite2D
var _lives_bar: Sprite2D
var _minimap: Sprite2D
var _mini_arrow: Sprite2D          # live MiniArrowGroup equivalent (Island.placeMiniMapPlayers)
var _minimap_noise: Array = []     # per-cell color mottle, baked per island
var _minimap_alpha_noise: Array = []
var _minimap_dirty := true
var _wind_text: Sprite2D

# turn list + pointer
var _queue := Node2D.new()
var _turn_arrow: Sprite2D          # HUD.TurnArrow (HUD.java:284-288)

# floating name tags
var _tag_nodes: Array = []
var _tag_bars: Array = []
var _tag_labels: Array = []
var _tag_arrows: Array = []
var _tag_players: Array = []       # player refs matching the arrays

# chat — each visual line is a list of colored segments (bot lines are
# blue name + white tail, exactly the original's backtick color toggling)
var _chat_root := Node2D.new()     # panel art + controls (rebuilt on min/max)
var _chat_block := Node2D.new()    # the wrapped text lines
var _chat_messages: Array = []     # raw entries: Array of [text, FontColor]; cap 20 (Chat.java:15)
var chat_top_line := 0             # Chat.ChatTopLine (top visible wrapped line)
var chat_minimized := false        # Main.ChatMinimized
var _chat_message := ""
var _chat_blink := 0.0
var _chat_cursor_on := true
var _chat_entry: Sprite2D
var _chat_entry_bar: Sprite2D      # Button_Bar(770,5,587) shown while typing (Chat.java:92-99)
var _chat_hint: Sprite2D
var _chat_buttons := {}            # "chatup"/"chatdown"/"minmax" -> {node, rect, sprite_kind}
var _scroll_knob: Sprite2D = null  # Scroll_Bar knob (expanded chat only)

# fade-up game messages (HUD.java: addMessage/checkMessageQueue/updateGameMessage)
var _message_queue: Array = []     # of [text, FontColor]
var _messages_in_flight: Array = []  # of {node, pos}

# banners / timer / reticle
var _your_turn: Sprite2D
var _defense: Sprite2D
var _timer_text: Sprite2D
var _reticle: Sprite2D

# game-over / spectator (HUD.SuccessMessage, Camera spectator labels)
var _success_message: Sprite2D = null
var _spectator_label: Sprite2D = null
var _spectator_name: Sprite2D = null
var _results_node := Node2D.new()

# Main.java SinTable[1]: SinPosition[1] -= 2.75 * dt; SinTable[1] = sin(...)
# (Main.java:310-311) — drives the banner pulse and the arrow bobs.
var _sin_pos1 := 0.0
var _sin1 := 0.0

# full-screen software-rasterized 3D, drawn behind the whole HUD
var _raster_bg: Sprite2D


# ---- small helpers ----

func _sprite(tex: Texture2D, pos: Vector2, z: int) -> Sprite2D:
	var n := Sprite2D.new()
	n.texture = tex
	# Swift: SKTexture.filteringMode = .nearest — a CanvasItem property in Godot.
	n.texture_filter = CanvasItem.TEXTURE_FILTER_NEAREST
	n.position = pos
	n.z_index = z
	return n


## SKSpriteNode.size analogue (project memory: size before any scale change).
static func _set_size(n: Sprite2D, size: Vector2) -> void:
	if n.texture != null:
		n.scale = size / Vector2(n.texture.get_size())


## Text sprite. `left` anchors the LEFT edge + vertical centerline (the
## Message_3D type-0 show(x,y) convention); default is centered (type 1).
func _text(s: String, cap: float, color: int = HUDArt.FontColor.WHITE,
		left: bool = false) -> Sprite2D:
	var n := Sprite2D.new()
	_set_text(n, s, cap, color, left)
	return n


func _set_text(n: Sprite2D, s: String, cap: float,
		color: int = HUDArt.FontColor.WHITE, left: bool = false) -> void:
	var tex := HUDArt.text(s, cap, null, 0.0, color)
	n.texture = tex
	n.scale = Vector2.ONE
	if left:
		n.centered = false
		n.offset = Vector2(0, -tex.get_height() / 2.0)   # left edge + centerline
	else:
		n.centered = true
		n.offset = Vector2.ZERO


func _advance(s: String, cap: float) -> float:
	# Pen advance in HUD pixels (Text.java: width * 0.75 per glyph, x cap/24)
	return HUDArt.text_advance_width(s, cap / HUDArt.CELL_SIZE)


## A WT surface-shader quad, built verbatim from the source's numbers:
## setBitmapTextureRect fractions (u1,v1,u2,v2; v measured from the top)
## into a sheet, at the attachSurfaceShader WT dimensions. Godot's Rect2 is
## top-left like the fractions, so the Swift's `1 - v2` SpriteKit flip drops
## out (BL->TL).
func _wt_slice(sheet: String, u1: float, v1: float, u2: float, v2: float,
		w: float, h: float, pos: Vector2, z: int) -> Sprite2D:
	var img := HUDArt.image(sheet)
	var sw := float(img.get_width())
	var sh := float(img.get_height())
	var tex := HUDArt.crop_texture(sheet, Rect2(u1 * sw, v1 * sh, (u2 - u1) * sw, (v2 - v1) * sh))
	var n := _sprite(tex, pos, z)
	_set_size(n, Vector2(w / WT_S, h / WT_S))
	return n


func _play(sfx: String, volume: float = 1.0) -> void:
	if Audio.shared != null:
		Audio.shared.play(sfx, volume)


# ---- persistence (UserDefaults analogue; HUDScene.swift:1064-1081) ----

static func _save_option(key: String, value: Variant) -> void:
	var cfg := ConfigFile.new()
	cfg.load(Audio.OPTIONS_PATH)   # keep whatever else is there
	cfg.set_value("options", key, value)
	cfg.save(Audio.OPTIONS_PATH)


const CAREER_PATH := "user://career.cfg"


# ---- build ----

func _init() -> void:
	add_child(_root)
	_build_nodes()


func _build_nodes() -> void:
	# ---- top magenta bar + menu titles ----
	# TitleBar = Button_Bar(TopBar, 800, 30, 0, 15, 50) (HUD.java:1057):
	# 10px end caps + stretched px 20..40 center band of the 64x32 sheet,
	# 30 rows tall, left edge x=0, centerline y=15. (The Swift stretched the
	# whole sheet to 800x22 — Java outranks.)
	var bar := _sprite(_bar_from_sheet("topbar.png", 800, 30), Vector2(400, 15), 200)
	_root.add_child(bar)
	# Button_3DMenu(x,14): title text left-aligned at (x+12, 16)
	# (Button_3DMenu.java: TopLeftX = x+2, Title.show(TopLeftX+10, TopLeftY+16)).
	for item in MENU_ORDER:
		var t := _text(item, 24, HUDArt.FontColor.WHITE, true)
		t.position = Vector2(MENU_X[item] + 12.0, 16.0)
		t.z_index = 210
		_root.add_child(t)
		_menu_titles[item] = t
	_menu_dropdown.z_index = 600
	_menu_dropdown.visible = false
	_root.add_child(_menu_dropdown)

	# ---- power + pitch bar frames: HUDBits[0..3] (HUD.java:342-354, positions
	# :546-549). All numbers are the source's own: attachSurfaceShader dims,
	# setBitmapTextureRect fractions, setPosition WT coords. Draw order options:
	# frame 71 < fill 72 < PowerMarker1/PitchMarker1 73 < PitchMarker2 74
	# -> z 120 / 125 / 130 / 135. Kept referenced so hide_bar can pull them
	# (HUD.java:107-110 removes ALL FOUR — the Swift kept [3]; Java outranks).
	_hud_bits = [
		_wt_slice("ui.png", 0.0, 0.0, 0.99609375, 0.23828125,
				0.700416, 0.169632, _wt(-0.730512, 0.648432), 120),
		_wt_slice("ui.png", 0.0, 0.2421875, 0.99609375, 0.48046875,
				0.700416, 0.169632, _wt(-0.030096, 0.648432), 120),
		_wt_slice("ui.png", 0.859375, 0.484375, 0.99609375, 0.72265625,
				0.098496, 0.169632, _wt(0.36936, 0.648432), 120),
		_wt_slice("ui.png", 0.0, 0.515625, 0.83203125, 0.72265625,
				0.580032, 0.147744, _wt(-0.987696, 0.2736), 120),
	]
	# HUDBits[3].setBitmapOrientation(-90): 212 wide -> 212 tall.
	# setBitmapOrientation(t) ≡ SK -t ≡ Godot rotation_degrees +t (see header).
	_hud_bits[3].rotation_degrees = -90.0
	for f in _hud_bits:
		_root.add_child(f)

	# PowerBar fill: a 3x2 patch, 440x24 px (1.20384x0.065664 WT) hung from its
	# top-left corner at (-0.84816, 0.667584) (HUD.java:1010-1014, :1123). Tile 0
	# stretches POWERBAR texture u 0.1..0.5 over the body; tile 1 holds u 0.5..1.0
	# in the leading 12 px cap (Cannon.java updatePowerBar patch-point math).
	# powerbar.png is IMAGES/POWERBAR image.png+alpha.png merged, pixel-identical.
	# (Tile 0's bottom-left UV corner is u=0.0, a slight skew the quad can't
	# express; the top edge's 0.1..0.5 mapping is used for the whole quad.)
	var pb := HUDArt.image("powerbar.png")
	var pbw := float(pb.get_width())
	var pbh := float(pb.get_height())
	_power_fill_body = _sprite(HUDArt.crop_texture("powerbar.png",
			Rect2(0.1 * pbw, 0, 0.4 * pbw, pbh)), _wt(-0.84816, 0.667584), 125)
	_power_fill_cap = _sprite(HUDArt.crop_texture("powerbar.png",
			Rect2(0.5 * pbw, 0, 0.5 * pbw, pbh)), _wt(-0.84816, 0.667584), 125)
	for n: Sprite2D in [_power_fill_body, _power_fill_cap]:
		n.centered = false           # patch origin corner; rows extend downward (z 0 -> -0.065664)
		n.visible = false            # shown only while charging (showPowerBar/hidePowerBar)
		_root.add_child(n)

	_power_bone = _wt_slice("ui.png", 0.0, 0.890625, 0.30859375, 0.99609375,
			0.21888, 0.079344, _wt(-0.84816, 0.634752), 130)
	_power_bone.rotation_degrees = -90.0   # PowerMarker1.setBitmapOrientation(-90): upright across the bar
	_power_bone.visible = false            # hidden until a first shot (LastPowerLevel -1000 sentinel)
	_root.add_child(_power_bone)

	# Pitch markers (no bitmapOrientation in source: both lie horizontal).
	# PitchMarker1 = small marker, live tilt (z 73); PitchMarker2 = bone, last shot (z 74).
	_pitch_live = _wt_slice("ui.png", 0.0, 0.8125, 0.19921875, 0.8828125,
			0.142272, 0.051984, _wt(-0.990432, 0.05472), 130)
	_root.add_child(_pitch_live)
	_pitch_last_bone = _wt_slice("ui.png", 0.0, 0.890625, 0.30859375, 0.99609375,
			0.21888, 0.079344, _wt(-0.990432, 0.05472), 135)
	_pitch_last_bone.visible = false       # hidden until a first shot (LastTiltMarker -1000 sentinel)
	_root.add_child(_pitch_last_bone)

	# ---- weapon dropdown (Button_3DWeapon(677,69), gilt) ----
	# 256x32 GiltButtonsDrop plane; name LEFT at TopLeftX+10=561, cost RIGHT-
	# aligned at TopLeftX+220=771, both f=1.0 (Button_3DWeapon.java:66-99;
	# no ball icon in the source — the Swift's weaponBall dropped, Java outranks).
	_weapon_button = _sprite(HUDArt.gilt_drop_row(false), Vector2(677, 69), 150)
	_set_size(_weapon_button, Vector2(256, 32))
	_root.add_child(_weapon_button)
	_weapon_title = _text("", 24, HUDArt.FontColor.WHITE, true)
	_weapon_title.position = Vector2(561, 71)
	_weapon_title.z_index = 151
	_root.add_child(_weapon_title)
	_weapon_cost = _text("", 24)
	_weapon_cost.position = Vector2(771, 71)
	_weapon_cost.z_index = 151
	_root.add_child(_weapon_cost)
	_weapon_dropdown.z_index = 600
	_weapon_dropdown.visible = false
	_root.add_child(_weapon_dropdown)

	# ---- right column ----
	# HUD.java:289 — the gold coin spins: 4x4 sheet, 16 frames, 20 fps,
	# column-major (same traversal as the coin particle in FXSprites).
	# CoinIcon top-left (684,135) 64x64 (HUD.java:551) -> center (716,167).
	_coin = _sprite(_coin_tex(0), Vector2(716, 167), 120)
	_set_size(_coin, Vector2(64, 64))
	_root.add_child(_coin)
	# CashBar = Button_Bar(120, 655, 210) — left edge 655, centerline 210
	# (HUD.java:552); value text centers at x=721 (HUD.java:588 — Java outranks
	# the Swift's 715).
	_cash_bar = _sprite(HUDArt.text_bar(120), Vector2(715, 210), 120)
	_root.add_child(_cash_bar)
	_gold_text = _text("0", 24)
	_gold_text.position = Vector2(721, 210)
	_gold_text.z_index = 130
	_root.add_child(_gold_text)
	_lives_label = _text("Lives", 24)   # RespawnsMessage.show(721,236) (HUD.java:550)
	_lives_label.position = Vector2(721, 236)
	_lives_label.z_index = 120
	_root.add_child(_lives_label)
	_lives_bar = _sprite(HUDArt.text_bar(120), Vector2(715, 260), 120)
	_root.add_child(_lives_bar)
	_lives_text = _text("2", 24)        # RespawnMessage.show(721,260) (HUD.java:610)
	_lives_text.position = Vector2(721, 260)
	_lives_text.z_index = 130
	_root.add_child(_lives_text)
	# MiniMap.setPosition(0.87552,-0.1368) = center (720,350), 128px
	# (HUD.java:1115-1117).
	_minimap = Sprite2D.new()
	_minimap.texture_filter = CanvasItem.TEXTURE_FILTER_NEAREST
	_minimap.position = Vector2(720, 350)
	_minimap.z_index = 120
	_root.add_child(_minimap)
	# Native player marker: the MapBits arrow sprite as a LIVE child (the
	# original MiniArrowGroup is its own quad over the map, Island.java
	# placeMiniMapPlayers; UV 0.03125,0.28125..0.25,0.828125 of the 64px sheet
	# = px (2,18)-(16,53), drawn 15x36 (0.04104x0.098496 WT)).
	_mini_arrow = _sprite(HUDArt.crop_texture("mapbits.png", Rect2(2, 18, 14, 35)),
			Vector2.ZERO, 1)
	_set_size(_mini_arrow, Vector2(15, 36))
	_mini_arrow.visible = false
	_minimap.add_child(_mini_arrow)
	# WindBar = Button_Bar(120, 655, 540) (HUD.java:1058); Wind text centered
	# at (719,540) f=1.0 (HUD.java:317-321 showWindSpeed — Java outranks the
	# Swift's 715/cap-14 first build).
	var wind_bar := _sprite(HUDArt.text_bar(120), Vector2(715, 540), 120)
	_root.add_child(wind_bar)
	_wind_text = _text("0 mph", 24)
	_wind_text.position = Vector2(719, 540)
	_wind_text.z_index = 130
	_root.add_child(_wind_text)

	# ---- turn list + its bobbing pointer ----
	_queue.z_index = 140
	_root.add_child(_queue)
	# TurnArrow: GlobalMedia.Arrow 32x32, h-flipped (setBitmapTextureRect(1,0,0,1))
	# and setBitmapOrientation(-90) -> Godot rotation -90 (HUD.java:1112,:346-347).
	# Bobs at (92-5sin,160) / ((32-5sin,100) spectator) (HUD.java:284-288).
	_turn_arrow = _sprite(HUDArt.texture("menuarrow.png"), Vector2(92, 160), 890)
	_set_size(_turn_arrow, Vector2(32, 32))
	_turn_arrow.flip_h = true
	_turn_arrow.rotation_degrees = -90.0
	_root.add_child(_turn_arrow)

	# ---- chat popup ----
	_chat_root.z_index = 140
	_root.add_child(_chat_root)
	_chat_block.z_index = 150
	_root.add_child(_chat_block)
	_build_chat_panel()
	# entry line / hint at (14,587), Message_3D scale 0.75 → cap 18 (Chat.java:279)
	# ChatBar backdrop = Button_Bar(770, 5, 587) while typing (Chat.java:92-99).
	_chat_entry_bar = _sprite(HUDArt.text_bar(770), Vector2(390, 587), 149)
	_chat_entry_bar.visible = false
	_root.add_child(_chat_entry_bar)
	_chat_hint = _text("Press 'C' to Chat", 18, HUDArt.FontColor.WHITE, true)
	_chat_hint.position = Vector2(14, 587)
	_chat_hint.z_index = 150
	_root.add_child(_chat_hint)
	_chat_entry = _text("", 18, HUDArt.FontColor.WHITE, true)
	_chat_entry.position = Vector2(14, 587)
	_chat_entry.z_index = 150
	_chat_entry.visible = false
	_root.add_child(_chat_entry)

	# ---- banners ----
	# Barrel-camera crossbones reticle (ReticleTex, 64px 0.175104 WT, centered
	# (400,300), order 75). Shown only in barrel cam.
	_reticle = _sprite(HUDArt.texture("reticle.png"), Vector2(400, 300), 350)
	_set_size(_reticle, Vector2(64, 64))
	_reticle.visible = false
	_root.add_child(_reticle)

	# YOUR TURN / DEFENSE MODE: persistent 128x128 quads (0.350208 WT) at
	# (400,480) (setPosition(0,-0.49248), HUD.java:546/700), pulsing
	# 1-|sin1|*0.1 (HUD.java:277-283). The Swift's 200/150px one-shot fades —
	# Java outranks.
	_your_turn = _sprite(HUDArt.texture("yourturn.png"), Vector2(400, 480), 400)
	_set_size(_your_turn, Vector2(128, 128))
	_your_turn.visible = false
	_root.add_child(_your_turn)
	_defense = _sprite(HUDArt.texture("defense.png"), Vector2(400, 480), 400)
	_set_size(_defense, Vector2(128, 128))
	_defense.visible = false
	_root.add_child(_defense)
	# Hot-seat timer digits centered at (495,525) f=1.0 (Timer.java:68 —
	# Java outranks the Swift's (400,80)).
	_timer_text = _text("", 24)
	_timer_text.position = Vector2(495, 525)
	_timer_text.z_index = 400
	_timer_text.visible = false
	_root.add_child(_timer_text)

	_results_node.visible = false
	_results_node.z_index = 1000
	_root.add_child(_results_node)

	# Full-screen software-rasterized 3D, drawn behind the whole HUD.
	_raster_bg = Sprite2D.new()
	_raster_bg.texture_filter = CanvasItem.TEXTURE_FILTER_NEAREST
	_raster_bg.position = Vector2(400, 300)
	_raster_bg.z_index = -1000
	_raster_bg.visible = false
	_root.add_child(_raster_bg)


## Button_Bar media 9-slice (10px caps, `h` rows of the sheet; Button_Bar.java
## media constructor): used for the TopBar title strip.
static func _bar_from_sheet(sheet: String, w: int, h: int) -> ImageTexture:
	var src := HUDArt.image(sheet)
	var sw := src.get_width()
	var out := Image.create(w, h, false, Image.FORMAT_RGBA8)
	# center band px 20..40, caps 0..10 / sw-10..sw, rows 0..h (v from top; BL->TL no-op)
	var mid := src.get_region(Rect2i(20, 0, 20, h))
	mid.resize(w - 20, h, Image.INTERPOLATE_NEAREST)
	out.blit_rect(mid, Rect2i(0, 0, w - 20, h), Vector2i(10, 0))
	out.blit_rect(src.get_region(Rect2i(0, 0, 10, h)), Rect2i(0, 0, 10, h), Vector2i(0, 0))
	out.blit_rect(src.get_region(Rect2i(sw - 10, 0, 10, h)), Rect2i(0, 0, 10, h), Vector2i(w - 10, 0))
	return ImageTexture.create_from_image(out)


func _coin_tex(frame: int) -> AtlasTexture:
	# column-major 4x4 traversal (HUD.java:289 setCoinFrame)
	@warning_ignore("integer_division")
	return HUDArt.crop_texture("coin.png",
			Rect2((frame / 4) * 64, (frame % 4) * 64, 64, 64))


## Show the software-rasterized frame as the 3D layer (or hide to reveal the
## real 3D viewport). Mirrors HUDScene.setRasterImage.
func set_raster_image(img: Image) -> void:
	if img == null:
		_raster_bg.visible = false
		return
	_raster_bg.texture = ImageTexture.create_from_image(img)
	_set_size(_raster_bg, Vector2(800, 600))
	_raster_bg.visible = true


func relayout(_size: Vector2) -> void:
	pass   # fixed 800x600, window scaling handles the rest


# ---- static + dynamic refresh ----

func rebuild_static() -> void:
	if game == null:
		return
	# stain mottle is randomized once per island (Island.java:294), not per repaint
	var g: int = Terrain.GRID
	_minimap_noise.resize(g * g)
	_minimap_alpha_noise.resize(g * g)
	for i in g * g:
		_minimap_noise[i] = randf_range(-20.0, 20.0)
		_minimap_alpha_noise[i] = randf_range(0.0, 0.5)
	_set_text(_wind_text, "%d mph" % int(game.wind_velocity), 24)
	_chat_messages = []
	chat_top_line = 0
	_append_chat("Welcome to %s!" % game.world.map.name, HUDArt.FontColor.BLUE)
	_rebuild_name_tags()
	refresh_dynamic()
	_minimap_dirty = true
	update_minimap()


## Enabled state per weapon (HUD.java:597-609 createWeaponButton):
## 0 = disabled (gray), 1 = offensive (white), 2 = defensive (blue).
func _weapon_enabled(c, w: int) -> int:
	var e := 1 if G.WEAPON_OFFENSIVE[w] else 2
	if G.WEAPON_COSTS[w] > c.cash:
		e = 0
	if game.current_player_index != c.index and G.WEAPON_OFFENSIVE[w]:
		e = 0
	return e


static func _enabled_color(e: int) -> int:
	match e:
		1: return HUDArt.FontColor.WHITE
		2: return HUDArt.FontColor.BLUE
	return HUDArt.FontColor.GRAY


func refresh_dynamic() -> void:
	if game == null:
		return
	var c = game.local_human
	if c == null:
		c = game.controlled_cannon
	_set_text(_gold_text, "%d" % (c.cash if c != null else 0), 24)
	var lives: int = maxi(0, game.options.max_respawns - (c.respawns_used if c != null else 0))
	_set_text(_lives_text, "%d" % lives, 24)
	if c != null and bar_visible:
		var w: int = c.weapon_index
		var e := _weapon_enabled(c, w)
		_set_text(_weapon_title, G.WEAPON_NAMES[w], 24, _enabled_color(e), true)
		_set_text(_weapon_cost, "%d" % G.WEAPON_COSTS[w], 24, _enabled_color(e))
		# right-aligned at 771 (Message_3D type 2, Button_3DWeapon.java:270)
		_weapon_cost.position.x = 771.0 - _advance("%d" % G.WEAPON_COSTS[w], 24) / 2.0
	# showYourTurn / showDefense: persistent, mutually exclusive banners
	# (HUD.java:698-707, :566-573); both fall with the bar (hideBar).
	if c != null and bar_visible and not game.game_over:
		var my_turn: bool = game.current_player_index == c.index
		_your_turn.visible = my_turn
		_defense.visible = not my_turn
	else:
		_your_turn.visible = false
		_defense.visible = false
	_rebuild_queue()
	_rebuild_name_tags()
	if _dropdown_open:
		_layout_weapon_dropdown()


# Turn order list (updateNextUp, HUD.java:359-433): current player white f=1.0
# at (115,160); the rest blue f=0.75 in backticks, indented +10, stepping 24px.
# When the bar is hidden (spectator) the whole list moves to (55,100).
func _rebuild_queue() -> void:
	for ch in _queue.get_children():
		_queue.remove_child(ch)
		ch.queue_free()
	if game == null:
		return
	var base_x := 115.0 if bar_visible else 55.0
	var base_y := 160.0 if bar_visible else 100.0
	var order: Array = []
	var idx: int = game.current_player_index
	for i in game.players.size():
		if game.players[idx].active:
			order.append(game.players[idx])
		idx = (idx + 1) % game.players.size()
	for i in mini(order.size(), 6):
		var first := i == 0
		var l := _text(order[i].name, 24.0 if first else 18.0,
				HUDArt.FontColor.WHITE if first else HUDArt.FontColor.BLUE, true)
		l.position = Vector2(base_x if first else base_x + 10.0, base_y + i * 24.0)
		_queue.add_child(l)


# ---- floating name tags (HUD.java:247-277) ----

func _rebuild_name_tags() -> void:
	for n in _tag_nodes:
		n.queue_free()
	_tag_nodes = []
	_tag_bars = []
	_tag_labels = []
	_tag_arrows = []
	_tag_players = []
	if game == null:
		return
	for p in game.players:
		var holder := Node2D.new()
		holder.z_index = 300
		var name_w := _advance(p.name, 24)
		# bar width = PixelWidth + 20, 28 tall (HUD.java:445 — Java outranks
		# the Swift's +24)
		var active: bool = game.current_player_index == p.index
		var bar := _sprite(HUDArt.target_bar(name_w + 20.0, active), Vector2.ZERO, 0)
		holder.add_child(bar)
		var label := _text(p.name, 24)
		# TargetNames show at bar x + 0.01368 WT = +5px (HUD.java:258)
		label.position = Vector2(5, 0)
		label.z_index = 1
		holder.add_child(label)
		# TargetArrow: GlobalMedia.Arrow 32x32 hanging 23px below (0.062928 WT);
		# bobs on the current player (HUD.java:260-264)
		var arrow := _sprite(HUDArt.texture("menuarrow.png"), Vector2(0, 23), 0)
		_set_size(arrow, Vector2(32, 32))
		holder.add_child(arrow)
		holder.visible = false
		_root.add_child(holder)
		_tag_nodes.append(holder)
		_tag_bars.append(bar)
		_tag_labels.append(label)
		_tag_arrows.append(arrow)
		_tag_players.append(p)


func _update_name_tags() -> void:
	if game == null:
		return
	var cam = game.camera
	var cam_node: Camera3D = cam.get("node") if cam != null else null
	# Tags ride a FIXED screen row, following only the projected x
	# (HUD.java:256-258: TempVector.Y is overwritten with 0.53352 / 0.69768 WT
	# = source y 105 / 45 — the Swift's full 2D projection + clamps dropped;
	# Java outranks).
	var row_y := 105.0 if bar_visible else 45.0
	var spectating: bool = cam != null and cam.mode == CameraController.Mode.SPECTATOR
	for i in _tag_players.size():
		var p = _tag_players[i]
		var node: Node2D = _tag_nodes[i]
		# HUD.java:256 — your own cannon never shows a name tag; the spectator
		# camera's followed player hides his too
		if cam_node == null or not p.active or p.dying or p == game.local_human \
				or (spectating and game.current_player_index == p.index):
			node.visible = false
			continue
		var world: Vector3 = p.position
		if cam_node.is_position_behind(world):   # TempVector.Z > 0 check
			node.visible = false
			continue
		var sx := cam_node.unproject_position(world).x
		node.position = Vector2(sx, row_y)
		node.visible = true
		# arrow bob on the current player (HUD.java:260-263)
		var arrow: Sprite2D = _tag_arrows[i]
		if game.current_player_index == p.index:
			arrow.position.y = 23.0 + _sin1 * 5.0 + 5.0
		else:
			arrow.position.y = 23.0


# ---- minimap ----

func mark_minimap_dirty() -> void:
	_minimap_dirty = true


func update_minimap() -> void:
	if not _minimap_dirty or game == null:
		return
	_minimap_dirty = false
	var canvas := Image.create(128, 128, false, Image.FORMAT_RGBA8)
	var parchment := HUDArt.image("map.png")
	canvas.blit_rect(parchment, Rect2i(0, 0, 128, 128), Vector2i.ZERO)
	_draw_island_stain(canvas)
	# World → map pixel per Island.java:1008: cell (x,z)/vertexScale lands at
	# texture (16+i, 16+j); the texture's V axis flips on screen, so in
	# top-left image coords that is row = 112 - z/vs (BL->TL of the Swift's
	# bottom-up y = 16 + z/vs).
	var vs: float = Terrain.VERTEX_SCALE
	# MAPBITS icons, 15px (0.04104/0.002736): gold X = chests (Chest.java:145),
	# red X = other cannons (Cannon.java:1267), arrow 15x36 = you.
	for chest in game.chests:
		if chest.alive:
			_blend_icon(canvas, Rect2i(17, 2, 14, 14),
					16.0 + chest.position.x / vs, 112.0 - chest.position.z / vs)
	var you = game.local_human
	for c in game.players:
		if c.active and not c.dying and c != you:
			_blend_icon(canvas, Rect2i(2, 2, 14, 14),
					16.0 + c.position.x / vs, 112.0 - c.position.z / vs)
	_minimap.texture = ImageTexture.create_from_image(canvas)
	_set_size(_minimap, Vector2(128, 128))


func _blend_icon(canvas: Image, crop: Rect2i, cx: float, cy: float) -> void:
	var icon := HUDArt.crop_image("mapbits.png", crop)
	icon.resize(15, 15, Image.INTERPOLATE_NEAREST)
	canvas.blend_rect(icon, Rect2i(0, 0, 15, 15),
			Vector2i(roundi(cx - 7.5), roundi(cy - 7.5)))


## The island as a rust-brown mottled stain on the parchment, verbatim from
## Island.java:262 (createMiniMap): cells with 0 < h/mapScale < 0.4 blend
## (139,55,24)±noise at opacity fading with elevation, with a dark (20,20,20)
## shoreline ring. Recomputing from live heights also reproduces the original's
## water-crater erase (Island.java:547 restores parchment where terrain sinks).
## sourceAtop semantics: the stain never paints outside the torn parchment edge.
func _draw_island_stain(canvas: Image) -> void:
	var g: int = Terrain.GRID
	if _minimap_noise.size() != g * g:
		return
	var terrain = game.world.terrain
	var scale: float = game.world.map.map_scale
	for j in g:
		for i in g:
			var f: float = terrain.current[j * g + i] / scale
			if f >= 0.4 or f <= 0.0:
				continue
			f -= 0.04
			if f < 0:
				f *= -6
			var r := 20.0 if f < 0.04 else 139.0
			var gc := 20.0 if f < 0.04 else 55.0
			var b := 20.0 if f < 0.04 else 24.0
			var noise: float = _minimap_noise[j * g + i]
			var a: float = (1.0 - f * 2.0) - _minimap_alpha_noise[j * g + i]
			if a < 0:
				a = 0
			if f < 0.025:
				a = 0.9
			# texture row 16+j renders bottom-up on screen → image row 112-j (BL->TL)
			var px := 16 + i
			var py := 112 - j
			var dst := canvas.get_pixel(px, py)
			if dst.a <= 0.0:
				continue   # sourceAtop: keep the parchment's alpha
			# premultiplied stain over premultiplied dst, alpha kept (sourceAtop)
			var sr := clampf((r - noise) * a / 255.0, 0, 1)
			var sg := clampf((gc - noise) * a / 255.0, 0, 1)
			var sb := clampf((b - noise) * a / 255.0, 0, 1)
			var out_r := sr * dst.a + dst.r * dst.a * (1.0 - a)
			var out_g := sg * dst.a + dst.g * dst.a * (1.0 - a)
			var out_b := sb * dst.a + dst.b * dst.a * (1.0 - a)
			canvas.set_pixel(px, py, Color(out_r / dst.a, out_g / dst.a, out_b / dst.a, dst.a))


# ---- per-frame ----

func update(dt: float) -> void:
	if game == null:
		return
	# Main.updateSinTable (Main.java:310-311): SinPosition[1] -= 2.75/s
	_sin_pos1 -= 2.75 * dt
	_sin1 = sin(_sin_pos1)

	_reticle.visible = game.camera.mode == CameraController.Mode.BARREL   # crossbones only in barrel cam

	var c = game.local_human
	if c == null:
		c = game.controlled_cannon
	if c != null and bar_visible:
		# PowerBar fill: right edge at level*440 px (Cannon.java:579-589). The
		# source splits it into a body tile (u 0.1..0.5) + a 12 px leading cap
		# tile (u 0.5..1.0), but the decoded POWERBAR texture is a uniform orange
		# 24x24 with no distinct cap art, so both tiles paint the same color. Drawn
		# as ONE sprite: two scaled AtlasTexture sprites left a 1-2 px seam where
		# the empty bar showed through (the "chunk cut out"); one sprite is
		# pixel-identical to the source's intended solid fill and has no seam.
		var fill_px: float = c.power_level * 440.0
		_power_fill_body.visible = c.power_bar_active
		_power_fill_cap.visible = false
		_size_fill(_power_fill_body, fill_px)
		# updateLastPowerMarker (Cannon.java:1072-1078)
		_power_bone.visible = c.last_power_level > -999.0
		_power_bone.position = _wt(-0.84816 + c.last_power_level * 440.0 * WT_S, 0.634752)
		# updateTiltMarker (Cannon.java:1555-1565):
		# f = (-TiltAngle + MinTilt) / (MinTilt + MaxTilt) * 165 px above y base 0.05472
		_pitch_live.position = _tilt_marker_pos(c.tilt_angle)
		_pitch_last_bone.visible = c.last_tilt_marker > -999.0
		_pitch_last_bone.position = _tilt_marker_pos(c.last_tilt_marker)

	# coin spin: 16 frames at 0.05s (HUD.java:289-296)
	_coin_timer += dt
	if _coin_timer > 0.05:
		_coin_timer = 0.0
		_coin_frame = (_coin_frame + 1) % 16
		_coin.texture = _coin_tex(_coin_frame)
		_set_size(_coin, Vector2(64, 64))

	# banner pulse: setBitmapSize(1 - |sin1|*0.1) (HUD.java:277-283)
	var pulse := 1.0 - absf(_sin1 * 0.1)
	if _your_turn.visible:
		_set_size(_your_turn, Vector2(128, 128) * pulse)
	if _defense.visible:
		_set_size(_defense, Vector2(128, 128) * pulse)

	# TurnArrow bob (HUD.java:284-288): x offset 23 + sin*5 + 5 left of the base
	if bar_visible:
		_turn_arrow.position = Vector2(120.0 - (23.0 + _sin1 * 5.0 + 5.0), 160.0)
	else:
		_turn_arrow.position = Vector2(60.0 - (23.0 + _sin1 * 5.0 + 5.0), 100.0)

	_update_name_tags()

	# hot-seat timer digits (Timer.java:68 — (495,525))
	if game.hot_seat_remaining > 0 and G.HOT_SEAT_TIMES[game.options.hot_seat_index] > 0 \
			and not game.game_over:
		_timer_text.visible = true
		_set_text(_timer_text, "%d" % int(ceilf(game.hot_seat_remaining)), 24)
	else:
		_timer_text.visible = false

	_update_game_messages(dt)
	if chatting:                       # cursor blink, 0.9 s toggle (Chat.java:115)
		_chat_blink += dt
		if _chat_blink > 0.9:
			_chat_blink = 0.0
			_chat_cursor_on = not _chat_cursor_on
			_refresh_chat_entry()

	# live minimap arrow (the original MiniArrow follows SpinAngle live,
	# Cannon.java:409 / Island.placeMiniMapPlayers)
	var you = game.local_human
	if you != null and you.active and not you.dying:
		_mini_arrow.visible = true
		var vs: float = Terrain.VERTEX_SCALE
		_mini_arrow.position = Vector2(16.0 + you.position.x / vs - 64.0,
				112.0 - you.position.z / vs - 64.0)
		# art points up; heading 0 = +z = up-screen. AppKit rotates -spinAngle
		# in y-up (HUDScene.swift:531); the canvas y-flip negates → +spin_angle.
		_mini_arrow.rotation_degrees = you.spin_angle
	else:
		_mini_arrow.visible = false


func _size_fill(n: Sprite2D, w: float) -> void:
	if n.texture == null:
		return
	var ts := Vector2(n.texture.get_region().size)
	n.scale = Vector2(w / ts.x, (0.065664 / WT_S) / ts.y)


func _tilt_marker_pos(tilt: float) -> Vector2:
	var f := (-tilt + G.MIN_TILT_ANGLE) / (G.MIN_TILT_ANGLE + G.MAX_TILT_ANGLE) * 165.0
	return _wt(-0.990432, 0.05472 + f * WT_S)


# ---- banners / messages ----

## Turn-change announcement. HUD.java routes these through the float-up
## message queue (showMyTurn/showOtherTurn addMessage, HUD.java:708/755); the
## persistent YOUR TURN / DEFENSE art is driven by refresh_dynamic (the Swift's
## one-shot center fades — Java outranks).
func show_banner(text: String) -> void:
	flash_message(text)
	refresh_dynamic()


## System/game message: queued to float up from screen center and fade out
## (HUD.java addMessage → checkMessageQueue → updateGameMessage). NOT chat.
func flash_message(text: String, color: int = HUDArt.FontColor.WHITE) -> void:
	if _message_queue.size() < 18:     # MessageCount cap (HUD.java:573-578)
		_message_queue.append([text, color])


## The big persistent end-state message — "You Win!" / "You Lose!" /
## "<name> Wins!". HUD.showSuccessMessage (HUD.java:163-169): destroys any
## previous one, Message_3D(text, centered, scale 1.0, order 80, color 1 =
## BLUE sheet) at (400,324). It stays until the match is torn down.
func show_success_message(text: String) -> void:
	if _success_message != null:
		_success_message.queue_free()
	var node := _text(text, 24, HUDArt.FontColor.BLUE)
	node.position = Vector2(400, 324)
	node.z_index = 800
	_root.add_child(node)
	_success_message = node


## The HUD half of spectator mode. Camera.setSpectatorCamera
## (Camera.java:278-290) calls hud.hideBar() and posts the persistent
## "Spectator Mode" label at (400,550) plus the followed player's name at
## (400,570) (refreshSpectatorCamera, Camera.java:426-433).
func enter_spectator_mode(following_name: String) -> void:
	# hideBar (HUD.java:94-131): ALL FOUR bar quads come off with the three
	# markers, the weapon button, coin icon, Lives label, cash and respawn
	# bars/text; YOUR TURN/DEFENSE clear; the Camera menu is removed with
	# Help! recreated in its slot at x=165.
	bar_visible = false
	for f in _hud_bits:
		f.visible = false
	_power_fill_body.visible = false
	_power_fill_cap.visible = false
	_power_bone.visible = false
	_pitch_live.visible = false
	_pitch_last_bone.visible = false
	_weapon_button.visible = false
	_weapon_title.visible = false
	_weapon_cost.visible = false
	_dropdown_open = false
	_weapon_dropdown.visible = false
	_coin.visible = false
	_lives_label.visible = false
	_cash_bar.visible = false
	_gold_text.visible = false
	_lives_bar.visible = false
	_lives_text.visible = false
	_your_turn.visible = false
	_defense.visible = false
	_camera_menu_removed = true
	if _open_menu != null:
		_open_menu = null
		_menu_dropdown.visible = false
	var cam_title: Sprite2D = _menu_titles["Camera"]
	cam_title.visible = false
	var help: Sprite2D = _menu_titles["Help!"]
	help.position.x = 165.0 + 12.0   # Button_3DMenu(165,14,...) (HUD.java:104-107)
	_rebuild_queue()                 # updateNextUp relocates to (55,100)
	if _spectator_label == null:
		var l := _text("Spectator Mode", 24)
		l.position = Vector2(400, 550)
		l.z_index = 300
		_root.add_child(l)
		_spectator_label = l
	refresh_spectator_name(following_name)


## refreshSpectatorCamera (Camera.java:426-433): swap in the current player's
## name under the Spectator Mode label on every turn change. The name is BLUE
## (Message_3D(..., 30, 1) — the Swift rendered it white; Java outranks).
func refresh_spectator_name(p_name: String) -> void:
	if _spectator_label == null:
		return
	if _spectator_name != null:
		_spectator_name.queue_free()
	var l := _text(p_name, 24, HUDArt.FontColor.BLUE)
	l.position = Vector2(400, 570)
	l.z_index = 300
	_root.add_child(l)
	_spectator_name = l


## Rise 20 px/s from (400,300), alpha 255→0 over 100 px; up to 6 in flight,
## the next spawns once every visible message has risen past 24 px
## (HUD.java:148-160 updateGameMessage, :988-1006 checkMessageQueue).
func _update_game_messages(dt: float) -> void:
	for m in _messages_in_flight:
		m.pos += dt * 20.0
		var node: Sprite2D = m.node
		node.position = Vector2(400, 300.0 - m.pos)
		node.modulate.a = maxf(0.0, 1.0 - m.pos / 100.0)
	var i := _messages_in_flight.size() - 1
	while i >= 0:
		if _messages_in_flight[i].pos > 100.0:
			_messages_in_flight[i].node.queue_free()
			_messages_in_flight.remove_at(i)
		i -= 1
	if _message_queue.is_empty() or _messages_in_flight.size() >= 6:
		return
	for m in _messages_in_flight:
		if m.pos < 24.0:
			return
	var entry: Array = _message_queue.pop_front()
	var node := _text(entry[0], 24, entry[1])
	node.position = Vector2(400, 300)
	node.z_index = 700
	_root.add_child(node)
	_messages_in_flight.append({"node": node, "pos": 0.0})


# ---- chat lines + wrapping ----

## Text.java wordWrap: tokenize on spaces/commas (kept), accumulate pixel
## width against the 250px chat column, hard-split words wider than a line.
## (Tokens are collected up front rather than flushed via closure — GDScript
## lambdas capture outer locals by value, unlike the Swift's nested func.)
func _word_wrap(s: String, width: float, cap: float) -> Array:
	var tokens: Array = []
	var token := ""
	for ch in s:
		token += ch
		if ch == " " or ch == ",":
			tokens.append(token)
			token = ""
	if not token.is_empty():
		tokens.append(token)

	var lines: Array = [""]
	var line_w := 0.0
	for t: String in tokens:
		var tw := _advance(t, cap)
		if line_w + tw < width:
			lines[lines.size() - 1] += t
			line_w += tw
		elif tw < width:
			lines.append(t)
			line_w = tw
		else:   # single token wider than the box
			for ch in t:
				var cw := _advance(ch, cap)
				if line_w + cw >= width:
					lines.append("")
					line_w = 0.0
				lines[lines.size() - 1] += ch
				line_w += cw
	return lines


## Bot/player chat line. The original wires '`name:`msg' — the backticks toggle
## the BLUE font sheet, so the name+colon renders blue and the message white
## (ref: originals/video_frames frame_020/023). Wrapped to the 250px column.
func bot_chat(p_name: String, msg: String) -> void:
	_push_chat_message([[p_name + ":", HUDArt.FontColor.BLUE], [msg, HUDArt.FontColor.WHITE]])


func _append_chat(s: String, color: int) -> void:
	_push_chat_message([[s, color]])


var _chat_line_count := 0            # wrapped-line count before the last append


func _push_chat_message(segments: Array) -> void:
	# updateChatText stickiness (Chat.java:236-241): stay glued to the bottom
	# when already there (or when everything fit) BEFORE the new message lands.
	var n2 := _visible_line_count()
	var sticky := chat_top_line >= _chat_line_count - n2 or _chat_line_count <= n2
	_chat_messages.append(segments)
	if _chat_messages.size() > 20:     # ChatMessages holds 20 (Chat.java:15)
		_chat_messages.pop_front()
	_redraw_chat(sticky)


## Wrap all stored messages into display lines of colored segments (the
## original re-wraps the whole store per updateChatText, Chat.java:232-247).
func _wrapped_lines() -> Array:
	var out: Array = []
	for segments in _chat_messages:
		if segments.size() >= 2:       # bot line: blue head + white tail
			var head: String = segments[0][0]
			var head_w := _advance(head, 18)
			var wrapped := _word_wrap(segments[1][0], 250.0 - head_w, 18)
			for k in wrapped.size():
				if k == 0:
					out.append([[head, segments[0][1]], [wrapped[k], segments[1][1]]])
				else:
					out.append([[wrapped[k], segments[1][1]]])
		else:
			for part in _word_wrap(segments[0][0], 250.0, 18):
				out.append([[part, segments[0][1]]])
	return out


func _visible_line_count() -> int:
	return 3 if chat_minimized else 14   # Chat.java:231-234 (n2)


func _redraw_chat(stick_to_bottom: bool = false) -> void:
	var lines := _wrapped_lines()
	_chat_line_count = lines.size()
	var n2 := _visible_line_count()
	# updateChatText scroll bookkeeping (Chat.java:236-247)
	if stick_to_bottom or chat_top_line + n2 > lines.size():
		chat_top_line = lines.size() - n2
	if chat_top_line < 0:
		chat_top_line = 0
	for ch in _chat_block.get_children():
		_chat_block.remove_child(ch)
		ch.queue_free()
	# ChatBlock.show(15, 360) expanded / (15, 528) minimized; line pitch =
	# 28 x 0.75 (newline) x 0.75 (group scale) = 15.75 px (Message_3D.java:76 +
	# setAbsoluteScale — the Swift's 15 rounded; Java outranks).
	var base_y := 528.0 if chat_minimized else 360.0
	const PITCH := 28.0 * 0.75 * 0.75
	var shown := 0
	for li in range(chat_top_line, mini(lines.size(), chat_top_line + n2)):
		var x := 15.0
		for seg in lines[li]:
			var l := _text(seg[0], 18, seg[1], true)
			l.position = Vector2(x, base_y + shown * PITCH)
			_chat_block.add_child(l)
			x += _advance(seg[0], 18)
		shown += 1
	_update_scroll_knob(lines.size())


# ---- chat panel (PopUp + controls; HUD.java show()/swapChatSize) ----

func _build_chat_panel() -> void:
	for ch in _chat_root.get_children():
		_chat_root.remove_child(ch)
		ch.queue_free()
	_chat_buttons = {}
	_scroll_knob = null
	if chat_minimized:
		# PopUp(5, 486, 295, 88, "Chat") (HUD.java:1042): short popup art,
		# title at (+10,+16); controls at (270,519)/(270,546)/(269,489).
		var panel := _sprite(HUDArt.short_popup_panel(295, 88), Vector2(5 + 147.5, 486 + 44), 0)
		_chat_root.add_child(panel)
		_add_chat_title(Vector2(15, 502))
		_add_chat_button("chatup", HUDArt.ControlSprite.CHAT_UP, Vector2(270, 519))
		_add_chat_button("chatdown", HUDArt.ControlSprite.CHAT_DOWN, Vector2(270, 546))
		_add_chat_button("minmax", HUDArt.ControlSprite.MIN_MAX, Vector2(269, 489))
	else:
		# PopUp(5, 320, 295, "Chat") (HUD.java:835/1047): the full-height 256px
		# popup art cropped to 295 wide, top-left (5, 320), title at (+10,+16);
		# controls at (270,351)/(270,546)/(269,323) + Scroll_Bar(270,378,168,14).
		var panel := _sprite(HUDArt.popup_panel(295), Vector2(5 + 147.5, 320 + 128), 0)
		_chat_root.add_child(panel)
		_add_chat_title(Vector2(15, 336))
		_add_chat_button("chatup", HUDArt.ControlSprite.CHAT_UP, Vector2(270, 351))
		_add_chat_button("chatdown", HUDArt.ControlSprite.CHAT_DOWN, Vector2(270, 546))
		_add_chat_button("minmax", HUDArt.ControlSprite.MIN_MAX, Vector2(269, 323))
		# Scroll_Bar knob: 26x26 controls-sheet crop (102,102)/(102,76 hover),
		# track top center (283,391), travel 168-26 px (Scroll_Bar.java:40-57)
		_scroll_knob = _sprite(HUDArt.crop_texture("controls.png", Rect2(102, 102, 25, 25)),
				Vector2(283, 391), 2)
		_set_size(_scroll_knob, Vector2(26, 26))
		_chat_root.add_child(_scroll_knob)
	_redraw_chat(true)


func _add_chat_title(pos: Vector2) -> void:
	var t := _text("Chat", 24, HUDArt.FontColor.WHITE, true)
	t.position = pos
	t.z_index = 1
	_chat_root.add_child(t)


func _add_chat_button(btn_name: String, kind: HUDArt.ControlSprite, top_left: Vector2) -> void:
	var r := HUDArt.control_rect(kind)
	var b := _sprite(HUDArt.control(kind), top_left + r.size / 2.0, 1)
	_chat_root.add_child(b)
	_chat_buttons[btn_name] = {"node": b, "kind": kind, "rect": Rect2(top_left, r.size)}


func _update_scroll_knob(total_lines: int) -> void:
	if _scroll_knob == null:
		return
	# Scroll_Bar.update (Scroll_Bar.java:86-97)
	var denom := maxf(float(total_lines) - 14.0, 1.0)
	var y := (168.0 - 26.0) * (float(maxi(chat_top_line, 0)) / denom)
	_scroll_knob.position = Vector2(283, 391 + y)


func _scroll_chat(delta: int) -> void:
	chat_top_line += delta
	_redraw_chat(false)


func _toggle_chat_size() -> void:
	# swapChatSize (HUD.java:818-846); persisted like saveMinimizedSettings
	chat_minimized = not chat_minimized
	chat_top_line = 999999   # ChatTopLine = -999 re-sticks to the bottom
	_save_option("opt.chatmin", chat_minimized)
	_build_chat_panel()


# ---- chat entry (Chat.java enableChat/keyDownChat/updateChatType) ----

func begin_chat_entry() -> void:
	if chatting:
		return
	chatting = true
	_chat_message = ""
	_chat_blink = 0.0
	_chat_cursor_on = true
	_refresh_chat_entry()


## Returns true when the event was consumed by the chat entry line.
## (Esc-cancel follows the Swift: the original applet host swallowed Esc.)
func chat_key_down(event: InputEventKey) -> bool:
	if not chatting:
		return false
	match event.keycode:
		KEY_ENTER, KEY_KP_ENTER:       # return — post and close (Chat.java:43-49)
			_post_chat_message()
			return true
		KEY_ESCAPE:                    # esc — cancel entry
			chatting = false
			_chat_message = ""
			_refresh_chat_entry()
			return true
		KEY_BACKSPACE:                 # backspace (Chat.java:55-58)
			if not _chat_message.is_empty():
				_chat_message = _chat_message.substr(0, _chat_message.length() - 1)
			_refresh_chat_entry()
			return true
	if event.unicode >= 32 and event.unicode < 127:
		# original caps the entry at 750px rendered width (Chat.java:51)
		if _advance("SAY : " + _chat_message, 18) < 750.0:
			_chat_message += char(event.unicode)
		_refresh_chat_entry()
		return true
	return true                        # chat swallows all keys while open


func _post_chat_message() -> void:
	if not _chat_message.is_empty():
		var who := "You"
		if game != null and game.local_human != null:
			who = game.local_human.name
		bot_chat(who, _chat_message)
	_chat_message = ""
	chatting = false
	_refresh_chat_entry()


func _refresh_chat_entry() -> void:
	_chat_hint.visible = not chatting
	_chat_entry.visible = chatting
	_chat_entry_bar.visible = chatting   # showChatBar/hideChatBar (Chat.java:92-99)
	if not chatting:
		return
	# cursor = the 0x7F glyph, blinking on a 0.9 s toggle (Chat.java:115)
	var cursor := char(127) if _chat_cursor_on else ""
	_set_text(_chat_entry, "SAY : " + _chat_message + cursor, 18, HUDArt.FontColor.WHITE, true)


# ---- results (Menu_Results_Screen.java:76-138) ----

func show_results() -> void:
	if game == null or results_shown:
		return
	results_shown = true
	for ch in _results_node.get_children():
		_results_node.remove_child(ch)
		ch.queue_free()
	_results_node.visible = true

	var rb := Assets.texture("MENUS/RESULTS.png")
	if rb != null:
		var back := _sprite(rb, Vector2(400, 300), 0)
		_set_size(back, Vector2(800, 600))
		_results_node.add_child(back)
	# Source layout: title with the blue backticked name centered at (400,200);
	# THIS GAME (400,250) / TOTAL (600,250); blue stat names LEFT at x=200
	# stepping 20px; blue "+N" this-game column centered at 400; white career
	# totals centered at 600; Done at (400,575). All f=1.0 (cap 24 — the
	# Swift's cap 20 corrected; Java outranks).
	var you = game.local_human
	if you == null:
		you = game.players[0]
	# "Game Stats For '`name`'": white quotes + blue name (the Swift tinted the
	# whole line blue; Java outranks). Composed as three centered segments.
	var head_a := "Game Stats For '"
	var full_w := _advance(head_a + you.name + "'", 24)
	var tx := 400.0 - full_w / 2.0
	for seg in [[head_a, HUDArt.FontColor.WHITE], [you.name, HUDArt.FontColor.BLUE],
			["'", HUDArt.FontColor.WHITE]]:
		var l := _text(seg[0], 24, seg[1], true)
		l.position = Vector2(tx, 200)
		l.z_index = 2
		_results_node.add_child(l)
		tx += _advance(seg[0], 24)
	_results_text("THIS GAME", 400, 250)
	_results_text("TOTAL", 600, 250)
	var names := ["Kills", "Misses", "Deaths", "Drownings", "Gold Spent"]
	var this_game: Array = [you.kills, you.misses, you.deaths, you.drownings, you.gold_spent]
	# career totals persisted across games (the original showed account totals)
	var cfg := ConfigFile.new()
	cfg.load(CAREER_PATH)
	for i in names.size():
		var key := "career.%s" % names[i]
		var total: int = int(cfg.get_value("career", key, 0)) + int(this_game[i])
		cfg.set_value("career", key, total)
		_results_text(names[i], 200, 280 + i * 20, HUDArt.FontColor.BLUE, true)
		_results_text("+ %d" % this_game[i], 400, 280 + i * 20, HUDArt.FontColor.BLUE)
		_results_text("%d" % total, 600, 280 + i * 20)
	cfg.save(CAREER_PATH)
	# Done: the standard 256x32 text button (Button_3D(400,575,"DONE","Done"))
	var exit_btn := _sprite(HUDArt.menu_button(false), Vector2(400, 575), 2)
	_set_size(exit_btn, Vector2(256, 32))
	_results_node.add_child(exit_btn)
	var exit_l := _text("Done", 24)
	exit_l.position = Vector2(400, 577)   # Button_3D label center +2 (menu_scene convention)
	exit_l.z_index = 3
	_results_node.add_child(exit_l)


func _results_text(s: String, x: float, y: float,
		color: int = HUDArt.FontColor.WHITE, left: bool = false) -> void:
	var l := _text(s, 24, color, left)
	l.position = Vector2(x, y)
	l.z_index = 2
	_results_node.add_child(l)


# ---- top menus + weapon dropdown ----

func _menu_items(which: String) -> Array:
	match which:
		"Quit":
			return [["Forfeit Game", "act-quit"]]   # HUD.java:1061
		"Options":
			# Main.java getSettings: Shadows, Sound, and Music only while Sound is on
			var items: Array = [
				["Shadows : On" if FXSprites.shadows_enabled else "Shadows : Off", "act-shadows"],
				["Sound : On" if Audio.shared != null and Audio.shared.sfx_volume > 0 else "Sound : Off", "act-sound"],
			]
			if Audio.shared != null and Audio.shared.sfx_volume > 0:
				items.append(["Music : On" if Audio.shared.music_volume > 0 else "Music : Off", "act-music"])
			return items
		"Camera":
			# "Cannon Camera".."Barrel Camera" (HUD.java:1066-1071)
			var items: Array = []
			for m in CameraController.SELECTABLE:
				items.append(["%s Camera" % CameraController.MODE_LABELS[m], "act-cam-%d" % m])
			return items
		"Help!":
			return [["Controls", "act-help-0"],          # HUD.java:957
					["How To Play", "act-help-1"],
					["Tutorial", "act-help-2"]]
	return []


## The Help! menu keeps its x when the Camera menu is pulled in spectator
## mode (hideBar recreates it at 165 — HUD.java:104-107).
func _menu_x(which: String) -> float:
	if which == "Help!" and _camera_menu_removed:
		return 165.0
	return MENU_X[which]


func _layout_top_menu(which: String) -> void:
	for ch in _menu_dropdown.get_children():
		_menu_dropdown.remove_child(ch)
		ch.queue_free()
	_open_menu_items = _menu_items(which)
	_menu_rollover = -1
	var x0 := _menu_x(which)
	var count := _open_menu_items.size()
	# Rollout backdrop: 254px-wide rollout art tiled per 32px row, centered
	# (x+128, y + count*16 + 16) (Button_3DMenu.java:36, :64-71); the hovered
	# row shows the red Buttons-sheet band (v 0.5..0.96875 → 254x34).
	for i in count:
		var row := _sprite(HUDArt.rollout_row(), Vector2(x0 + 128.0, 46.0 + i * 32.0), 0)
		_set_size(row, Vector2(254, 32))
		_menu_dropdown.add_child(row)
	_menu_hover_row = _sprite(HUDArt.crop_texture("buttons_button.png", Rect2(0, 32, 256, 30)),
			Vector2.ZERO, 1)
	_set_size(_menu_hover_row, Vector2(254, 34))
	_menu_hover_row.visible = false
	_menu_dropdown.add_child(_menu_hover_row)
	for i in count:
		# item text f=1.0, left at (TopLeftX+10, TopLeftY+48+i*32) = (x+12, 48+i*32)
		var l := _text(_open_menu_items[i][0], 24, HUDArt.FontColor.WHITE, true)
		l.position = Vector2(x0 + 12.0, 48.0 + i * 32.0)
		l.z_index = 2
		_menu_dropdown.add_child(l)


func _layout_weapon_dropdown() -> void:
	for ch in _weapon_dropdown.get_children():
		_weapon_dropdown.remove_child(ch)
		ch.queue_free()
	if game == null:
		return
	var c = game.controlled_cannon
	if c == null:
		return
	_weapon_rollover = -1
	# GiltRollout backdrop tiled per 32px row, 254 wide (Button_3DWeapon.java:95-103)
	for i in 12:
		var row := _sprite(HUDArt.crop_texture("buttons_giltrollout.png", Rect2(0, 0, 256, 32)),
				Vector2(677, 100.0 + i * 32.0), 0)
		_set_size(row, Vector2(254, 32))
		_weapon_dropdown.add_child(row)
	_weapon_hover_row = _sprite(HUDArt.gilt_button(true), Vector2.ZERO, 1)
	_set_size(_weapon_hover_row, Vector2(254, 32))
	_weapon_hover_row.visible = false
	_weapon_dropdown.add_child(_weapon_hover_row)
	for w in 12:
		# Items[n].show(TopLeftX+10, TopLeftY+47+n*32) = (561, 102+n*32);
		# Cost right-aligned at (771, 102+n*32); f=1.0 colored by Enabled
		var e := _weapon_enabled(c, w)
		var l := _text(G.WEAPON_NAMES[w], 24, _enabled_color(e), true)
		l.position = Vector2(561, 102.0 + w * 32.0)
		l.z_index = 2
		_weapon_dropdown.add_child(l)
		var cost := "%d" % G.WEAPON_COSTS[w]
		var cl := _text(cost, 24, _enabled_color(e))
		cl.position = Vector2(771.0 - _advance(cost, 24) / 2.0, 102.0 + w * 32.0)
		cl.z_index = 2
		_weapon_dropdown.add_child(cl)


# ---- mouse ----
# COORDINATE CONVENTION: `p_sk` is in the ORIGINAL SpriteKit scene space the
# macOS harness uses — the 800x600 canvas with the origin at the BOTTOM-LEFT
# and +y UP (main.swift:380 clicks (100,589) for Options = source y 11). It
# converts to the canvas' y-down source space HERE, in this one visible place
# (same convention as menu_scene.gd click()).

## Title hit rect: x menuX+2 .. +2+titleWidth, y 0..28 (Button_3DMenu.java
## TopLeftX/WordWidth/BottomRightY).
func _menu_title_rect(which: String) -> Rect2:
	var x0 := _menu_x(which)
	return Rect2(x0 + 2.0, 0.0, _advance(which, 24), 28.0)


func _menu_row_at(p: Vector2, hover_offset: float) -> int:
	if _open_menu == null:
		return -1
	var x0 := _menu_x(_open_menu)
	if p.x < x0 + 2.0 or p.x > x0 + 254.0:
		return -1
	for i in _open_menu_items.size():
		# click band TopLeftY+32+n*32 .. BottomRightY+32+n*32; hover band +28
		if p.y >= hover_offset + i * 32.0 and p.y <= hover_offset + 28.0 + i * 32.0:
			return i
	return -1


const WEAPON_RECT := Rect2(551, 55, 252, 28)   # Button_3DWeapon TopLeft/BottomRight


func _weapon_row_at(p: Vector2) -> int:
	if p.x < WEAPON_RECT.position.x or p.x > WEAPON_RECT.end.x:
		return -1
	for i in 12:
		if p.y >= 87.0 + i * 32.0 and p.y <= 115.0 + i * 32.0:
			return i
	return -1


## Pointer-move entry point (SpriteKit y-up coords, see above). The original
## menus unroll on ROLLOVER (Button_3DMenu.checkBounds) and reroll when the
## pointer leaves; the weapon dropdown highlights rows but opens on click.
func hover(p_sk: Vector2) -> void:
	var p := Vector2(p_sk.x, 600.0 - p_sk.y)
	if results_shown:
		return
	# weapon button hover state (setState: bottom half of the gilt sheet)
	if bar_visible:
		var over_btn := WEAPON_RECT.has_point(p)
		_weapon_button.texture = HUDArt.gilt_drop_row(over_btn or _dropdown_open)
		_set_size(_weapon_button, Vector2(256, 32))
	# weapon dropdown rollover
	if _dropdown_open:
		var row := _weapon_row_at(p)
		if row >= 0:
			_weapon_hover_row.visible = true
			_weapon_hover_row.position = Vector2(677, 100.0 + row * 32.0)
			if row != _weapon_rollover:
				_play("hover", 0.5)
			_weapon_rollover = row
		else:
			_weapon_hover_row.visible = false
			_weapon_rollover = -1
	# top menus: unroll on hover, reroll on leaving (Button_3DMenu.checkBounds)
	if _open_menu != null:
		var x0 := _menu_x(_open_menu)
		var region := Rect2(x0 + 2.0, 0.0, 252.0,
				28.0 + 32.0 + _open_menu_items.size() * 32.0)
		var title_r := _menu_title_rect(_open_menu)
		if not region.has_point(p) and not title_r.has_point(p):
			_open_menu = null
			_menu_dropdown.visible = false
		else:
			var row := _menu_row_at(p, 28.0)
			if row >= 0:
				_menu_hover_row.visible = true
				_menu_hover_row.position = Vector2(x0 + 128.0, 46.0 + row * 32.0)
				if row != _menu_rollover:
					_play("hover", 0.5)
				_menu_rollover = row
			else:
				_menu_hover_row.visible = false
				_menu_rollover = -1
	if _open_menu == null and not _dropdown_open:
		for which in MENU_ORDER:
			if which == "Camera" and _camera_menu_removed:
				continue
			if _menu_title_rect(which).has_point(p):
				_open_top_menu(which)
				_play("hover", 0.5)   # Sound_Over on unroll
				break
	# chat control hover states
	for btn_name in _chat_buttons:
		var entry: Dictionary = _chat_buttons[btn_name]
		var over: bool = entry.rect.has_point(p)
		entry.node.texture = HUDArt.control(entry.kind, over)
	if _scroll_knob != null:
		var kr := Rect2(_scroll_knob.position - Vector2(13, 13), Vector2(26, 26))
		_scroll_knob.texture = HUDArt.crop_texture("controls.png",
				Rect2(102, 76, 25, 25) if kr.has_point(p) else Rect2(102, 102, 25, 25))
		_set_size(_scroll_knob, Vector2(26, 26))


func _open_top_menu(which: String) -> void:
	_open_menu = which
	_layout_top_menu(which)
	_menu_dropdown.visible = true
	_dropdown_open = false
	_weapon_dropdown.visible = false


## Click entry point, and the `--uitest` harness API (macOS main.swift parity).
## Returns true when the click was consumed.
func click(p_sk: Vector2) -> bool:
	var p := Vector2(p_sk.x, 600.0 - p_sk.y)

	if results_shown:
		# The results screen is terminal (stats + Done). Dismiss on a click
		# ANYWHERE, not just the 256x32 Done rect at (400,575): the button is the
		# only action, and any-click is robust against a hit-area/coordinate
		# mismatch that could otherwise strand the player here. (Keyboard
		# Enter/Space/Esc also leave — GameController.key_down.)
		_exit_game()
		return true

	# open weapon dropdown: row select / anywhere else rerolls
	if _dropdown_open:
		var row := _weapon_row_at(p)
		_dropdown_open = false
		_weapon_dropdown.visible = false
		if row >= 0:
			_play("click")
			if game != null:
				game.select_weapon(row)
		return true

	# open top menu: row action / anywhere else rerolls
	if _open_menu != null:
		var row := _menu_row_at(p, 32.0)   # click band offset 32 (Button_3DMenu.java:176)
		var was: String = _open_menu
		_open_menu = null
		_menu_dropdown.visible = false
		if row >= 0:
			_play("click")
			_handle_menu_action(_menu_items(was)[row][1])
		return true

	# weapon button toggles its dropdown on click (Button_3DWeapon.checkBounds)
	if bar_visible and WEAPON_RECT.has_point(p):
		_play("click")
		_dropdown_open = true
		_layout_weapon_dropdown()
		_weapon_dropdown.visible = true
		return true

	# menu titles (click-open kept for the click-driven test harness; the
	# hover path above is the original rollover behavior)
	for which in MENU_ORDER:
		if which == "Camera" and _camera_menu_removed:
			continue
		if _menu_title_rect(which).has_point(p):
			_open_top_menu(which)
			_play("click")
			return true

	# chat controls (HUD.java:871-899 CHATUP/CHATDOWN/MINMAX)
	for btn_name in _chat_buttons:
		if _chat_buttons[btn_name].rect.has_point(p):
			_play("click")
			match btn_name:
				"chatup":
					_scroll_chat(-1)
				"chatdown":
					_scroll_chat(1)
				"minmax":
					_toggle_chat_size()
			return true
	# scroll rail click-to-jump (HUD.java:858-869)
	if not chat_minimized and p.x > 270 and p.x < 297 and p.y > 378 and p.y < 546:
		var lines := _wrapped_lines().size()
		var denom := maxi(lines - 14, 1)
		chat_top_line = int((p.y - 378.0) / 140.0 * float(denom))
		_redraw_chat(false)
		return true

	return false


func _handle_menu_action(action: String) -> void:
	match action:
		"act-quit":
			# QUIT while the game runs = forfeit (HUD.java:922-935); once the
			# match is decided (or spectating without a cannon) it leaves.
			if game != null and not game.game_over and game.controlled_cannon != null:
				game.menu_forfeit()
			else:
				_exit_game()
		"act-shadows":
			FXSprites.shadows_enabled = not FXSprites.shadows_enabled
			_save_option("opt.shadows", FXSprites.shadows_enabled)
			if game != null:
				game.set_shadows_visible(FXSprites.shadows_enabled)
		"act-sound":
			if Audio.shared != null:
				var on := Audio.shared.sfx_volume > 0
				Audio.shared.sfx_volume = 0.0 if on else Audio.DEFAULT_SFX
				if on:
					Audio.shared.music_volume = 0.0   # Main.java:394 — sound off forces music off
				_save_option("opt.sound", not on)
				_save_option("opt.music", Audio.shared.music_volume > 0)
		"act-music":
			if Audio.shared != null:
				var on := Audio.shared.music_volume > 0
				Audio.shared.music_volume = 0.0 if on else Audio.DEFAULT_MUSIC
				_save_option("opt.music", not on)
		"act-help-0":
			HelpViewer.present("controls.htm")                    # HUD.java:957
		"act-help-1":
			HelpViewer.present("gettingstarted.htm", "playing")   # HUD.java:961
		"act-help-2":
			HelpViewer.present("tutorial1.htm")                   # launchTutorial()
		_:
			if action.begins_with("act-cam-"):
				if game != null:
					game.camera.set_mode(action.substr(8).to_int())


func _exit_game() -> void:
	if game == null:
		return
	var cb = game.get("on_exit")
	if cb is Callable and cb.is_valid():
		cb.call()


# ---- direct input (menu_scene.gd convention) ----
# Mouse event positions arrive in canvas coords (y-down, 800x600 under the
# canvas_items stretch); click()/hover() take the SpriteKit y-up convention,
# so the y flips here and flips back inside — the round trip keeps the public
# API identical to the macOS test harness. Keyboard is GameController's
# (chat_key_down / begin_chat_entry are called from there, mirroring
# GameController.swift keyDown).

func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventMouseButton and event.button_index == MOUSE_BUTTON_LEFT and event.pressed:
		if click(Vector2(event.position.x, 600.0 - event.position.y)):
			get_viewport().set_input_as_handled()
	elif event is InputEventMouseMotion:
		hover(Vector2(event.position.x, 600.0 - event.position.y))
