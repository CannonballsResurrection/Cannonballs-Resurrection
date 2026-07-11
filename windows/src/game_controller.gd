class_name GameController
extends RefCounted
# Port of macos/Sources/Cannonballs/GameController.swift (which cites
# Game_Loop.java / Packet_Manager.java / Cannon.java / HUD.java).
# Central match driver: turn state machine, physics stepping, economy (SPEC §4, §5).
#
# GDScript port notes (windows/PORTING.md):
# - Plain RefCounted owning the match; boot.gd (the GameViewController port)
#   adds world.root / hud to the tree, makes the camera current, and drives
#   update(frame_dt) from _process — the render-thread command queue of the
#   macOS build drops out (Godot input and _process run on one thread).
# - `hud` stays duck-typed: src/ui/hud_scene.gd is a concurrent sibling port
#   of HUDScene.swift (same Swift surface, snake_case). Every hud call is
#   null-guarded so the 3D match runs headless-clean before the HUD lands.
# - The Swift's `weak var game` back-references become plain refs; tear_down()
#   clears them to break the RefCounted cycles (cannon.gd header note).
# - Where the decompiled Java disagrees with the Swift, the Java wins
#   (PORTING.md rule 4); every such spot is marked "Java outranks" inline.

var options: G.GameOptions
var world: World
var players: Array = []          # of Cannon
var projectiles: Array = []      # of Projectile
var chests: Array = []           # of Chest
var camera := CameraController.new()
var _cloud_offset := Vector2.ZERO
var _lens_flare: WorldDressing.LensFlare = null
var hud = null                   # HUDScene port (duck-typed concurrent sibling)

var on_exit := Callable()

# wind (rolled once per match)
var wind_direction := 0.0        # deg
var wind_velocity := 0.0         # "mph" 0..79
var wind: Vector3:
	get:
		return Vector3(sin(deg_to_rad(wind_direction)) * wind_velocity * 0.2, 0,
				cos(deg_to_rad(wind_direction)) * wind_velocity * 0.2)

# turn state
var current_player_index := 0
var waiting_timer = null         # float or null — 3 s after on-turn shot resolves → switch
var hot_seat_remaining := 0.0    # seconds left; only if option set
var game_over := false
var game_over_timer := 0.0
var winner_index = null          # int or null
# GameState 15: the local player is out of lives; 5 s of "You Lose!" before
# spectator mode (Cannon.WaitingTimer, Packet_Manager.java:267)
var death_wait_timer = null      # float or null
var spectating := false          # GameState 11
# local forfeit: 10 s (15 s destroy state entered with 5 s elapsed —
# Packet_Manager.java:246-247) and the match is left
var forfeit_exit_timer = null    # float or null
var game_time := 0.0
var _last_tick_second := -1
var _minimap_refresh := 0.0

var current_player:
	get:
		return players[current_player_index]

var camera_focus_cannon:
	get:
		if winner_index != null:
			return players[winner_index]
		var cp = players[current_player_index]
		if cp.active and not cp.dying:
			return cp
		for p in players:
			if p.active and not p.dying:
				return p
		return null

## First human player (for HUD display even while dead/respawning).
var local_human:
	get:
		for p in players:
			if not p.is_bot:
				return p
		return null

## The cannon the local keyboard drives right now.
var controlled_cannon:
	get:
		var cp = players[current_player_index]
		if not cp.is_bot and cp.active and not cp.dying:
			return cp
		for p in players:
			if not p.is_bot and p.active and not p.dying:
				return p
		return null


func _init(p_options: G.GameOptions) -> void:
	options = p_options
	world = World.new(MapCatalog.maps[options.map_index])
	camera.attach_to(self)
	# HUDScene (the Swift creates it here too, GameController.swift:63);
	# concurrent sibling port — created duck-typed so this file stands alone.
	if ResourceLoader.exists("res://src/ui/hud_scene.gd"):
		var hud_script = load("res://src/ui/hud_scene.gd")
		if hud_script != null:
			hud = hud_script.new()
			hud.game = self

	# players
	for i in options.players.size():
		var c := Cannon.new(i, options.players[i], self)
		players.append(c)
		world.root.add_child(c.node)
	for c in players:
		c.place()
		c.to_ground()
		c.spin_angle = randf_range(0.0, 360.0)
		c.sync_node()

	# chests: 3–7
	var chest_count := randi_range(3, 7)
	for i in chest_count:
		spawn_chest()

	# wind
	wind_direction = randf_range(0.0, 360.0)
	wind_velocity = float(randi_range(0, 79))

	# lens flare on sunny maps (Entity_Object_LensFlare)
	if world.map.has_sun:
		_lens_flare = WorldDressing.LensFlare.new(world.map.sun_vector, camera.node)

	# first player
	current_player_index = randi_range(0, players.size() - 1)
	begin_turn(true)
	if hud != null:
		hud.rebuild_static()


func tear_down() -> void:
	if Audio.shared != null:
		Audio.shared.stop_loops()
	# break the RefCounted back-reference cycles (the Swift's `weak var game`;
	# cannon.gd header note)
	for c in players:
		c.game = null
	for p in projectiles:
		p.game = null
	camera.game = null
	if hud != null:
		hud.game = null


# MARK: - Chests

func spawn_chest() -> void:
	var terrain := world.terrain
	var size := Terrain.world_size()
	for attempt in 200:
		var x := randf_range(size * 0.06, size * 0.94)
		var z := randf_range(size * 0.06, size * 0.94)
		if terrain.target_height(x, z) <= 2.0:
			continue
		var p := Vector3(x, 0, z)
		var blocked := false
		for c in players:
			if c.active and G.dist_2d(c.position, p) < 10:
				blocked = true
				break
		if blocked:
			continue
		for ch in chests:
			if ch.alive and G.dist_2d(ch.position, p) < 30:
				blocked = true
				break
		if blocked:
			continue
		for pr in world.props:
			if pr.alive and G.dist_2d(pr.position, p) < 10 + pr.spec.radius:
				blocked = true
				break
		if blocked:
			continue
		var chest := Chest.new(Vector3(x, Chest.rest_height(terrain, x, z), z))
		chests.append(chest)
		world.root.add_child(chest.node)
		return


func collect_chest(chest, by) -> void:
	chest.alive = false
	chest.node.queue_free()   # removeFromParentNode — chests never re-attach
	chests.erase(chest)
	var treasure := Chest.roll_treasure()
	if treasure.kind == "gold":
		by.cash += treasure.amount
		if Audio.shared != null:
			Audio.shared.play("cash")
		Particles.coins(chest.position, 30, world)
		if hud != null:
			hud.flash_message("%s finds %d gold!" % [by.name, treasure.amount])
	else:   # teleport
		if hud != null:
			hud.flash_message("Cursed chest! %s is teleported!" % by.name)
		by.player_teleport()
	if options.treasure_respawn:
		spawn_chest()
	if hud != null:
		hud.mark_minimap_dirty()


# MARK: - Firing / projectiles

func launch(projectile, _cannon) -> void:
	projectiles.append(projectile)
	world.root.add_child(projectile.node)
	projectile.update(0.0001)   # spec: immediate tiny tick


## Tower / Teleport (no projectile): consumes the turn if used on-turn.
func instant_weapon_used(cannon) -> void:
	if current_player_index == cannon.index:
		cannon.has_fired_this_turn = true
		cannon.fired_on_turn = false
		waiting_timer = 3.0
	if hud != null:
		hud.refresh_dynamic()


func projectile_died(projectile, impact: Vector3) -> void:
	projectiles.erase(projectile)
	projectile.owner.bot.last_impact = impact
	# record threat for defensive AI (the shooter index keeps the attribution
	# the Java threat scan reads, Cannon.java:1394-1431 — bot_ai.gd)
	for p in players:
		if p != projectile.owner:
			p.bot.note_enemy_impact(impact, projectile.owner.index)
	if projectile.owner.fired_on_turn and projectile.owner.index == current_player_index:
		projectile.owner.fired_on_turn = false
		waiting_timer = 3.0
	if hud != null:
		hud.refresh_dynamic()


## X-Shot line kill (SPEC checkForHitLine).
func check_for_hit_line(x1: float, z1: float, x2: float, z2: float,
		width: float, killer) -> void:
	for c in players:
		if c != killer and c.active and not c.dying:
			var d := Terrain.distance_to_segment(c.position.x, c.position.z, x1, z1, x2, z2)
			if d < width:
				kill(c, {"kind": "killed", "by": killer.index})
	for chest in chests.duplicate():   # collect_chest erases from the array
		if chest.alive:
			var d := Terrain.distance_to_segment(chest.position.x, chest.position.z, x1, z1, x2, z2)
			if d < width:
				collect_chest(chest, killer)
	for prop in world.props:
		if prop.alive and prop.spec.destructible:
			var d := Terrain.distance_to_segment(prop.position.x, prop.position.z, x1, z1, x2, z2)
			if d < width:
				destroy_prop(prop, killer)


# MARK: - Props / TNT

func destroy_prop(prop, killer) -> void:
	if not prop.alive:
		return
	if prop.spec.explosive:
		_detonate(prop, killer if killer != null else prop.detonator)
		return
	world.destroy_prop(prop)
	world.terrain.crater(prop.position.x, prop.position.z, 4, 20)
	Particles.explosion(prop.position + Vector3(0, prop.spec.height / 2, 0), world)
	# the prop.dat <DEBRIS> pieces tumble away (Prop.java:154-163 /
	# Chunk_Object). Offsets are actor-local and the debris actors spawn
	# unscaled, like every actor (Prop.java:388 — native scale).
	for entry in prop.spec.debris:
		FXSprites.debris_chunk(entry[0],
				prop.position + entry[1],
				Vector3(randf_range(-12.0, 12.0),
						randf_range(15.0, 30.0),
						randf_range(-12.0, 12.0)),
				1.0, world)
	# STAR burst off the falling prop (Prop.java:113)
	for i in 4:
		FXSprites.star(prop.position + Vector3(0, 2 + randf_range(0.0, 4.0), 0),
				Vector3(randf_range(-15.0, 15.0), 30 + randf_range(-15.0, 15.0),
						randf_range(-15.0, 15.0)),
				randf_range(1.0, 4.0), world)
	if Audio.shared != null:
		Audio.shared.play("explosion1")


## The planted TNT barrel (Weapon.java updateTNT:944-954 fires packet 21;
## Packet_Manager.java:463-478 builds the prop). The TNT Prop ctor
## (Prop.java:198-233) hard-sets onGround; <ONGROUND>:YES snaps Y to the
## terrain below (Prop.java:543-545), yaw is a fresh random 0..360
## (Packet_Manager.java:465).
func plant_tnt(p_position: Vector3, p_owner) -> void:
	var spec := Props.PropSpec.load_spec("TNT")
	var y := world.terrain.height(p_position.x, p_position.z)
	var prop := Props.Prop.new(spec, Vector3(p_position.x, y, p_position.z),
			randf() * 360.0, game_time)
	prop.detonator = p_owner
	world.add_prop(prop)
	# landing FX (Packet_Manager.java:469-478): 6 CHUNKS frame-2/3 pieces off
	# Y-2 (velocity ±30 / 30±10 / ±30, size 0.1+r), a 0.5 s WHITE smoke column
	# sunk 4 under, and Sound_Puff. (The Swift's Particles.dirt approximation —
	# Java outranks.)
	for i in 6:
		var frame := 2 if randf() < 0.5 else 3
		FXSprites.chunk(frame,
				Vector3(prop.position.x, prop.position.y - 2.0, prop.position.z),
				Vector3((randf() - 0.5) * 30.0, 30.0 + (randf() - 0.5) * 10.0,
						(randf() - 0.5) * 30.0),
				0.1 + randf(), world)
	FXSprites.smoke_column(Vector3(prop.position.x, prop.position.y - 4.0, prop.position.z),
			0.5, world, false, false)
	if Audio.shared != null:
		Audio.shared.play("puff")


func _detonate(prop, detonator) -> void:
	if not prop.alive or prop.detonating:
		return
	prop.detonating = true
	prop.shock_scale = 0.0
	if detonator != null:
		prop.detonator = detonator
	prop.node.visible = false   # node.isHidden = true
	Particles.explosion(prop.position + Vector3(0, 4, 0), world, true)
	# visible SHOCKWAVE ring grows with the kill radius (60 u/s to 50)
	FXSprites.shockwave(prop.position, 2, 30, world)
	if Audio.shared != null:
		Audio.shared.play("explosion3")
	camera.shock(prop.position, 150.0)
	world.terrain.crater(prop.position.x, prop.position.z, 4, 20)


func _update_shockwaves(dt: float) -> void:
	for prop in world.props:
		if not (prop.detonating and prop.alive):
			continue
		prop.shock_scale += dt * 60.0
		var r: float = prop.shock_scale
		# kill cannons inside the shockwave
		for c in players:
			if c.active and not c.dying:
				var d: float = c.position.distance_to(prop.position + Vector3(0, 4, 0))
				if d < r + 10.0:
					var by: int = prop.detonator.index if prop.detonator != null else -1
					kill(c, {"kind": "detonated", "by": by})
		# chain other props / chests
		for other in world.props:
			if other != prop and other.alive \
					and other.position.distance_to(prop.position) < r:
				destroy_prop(other, prop.detonator)
		for chest in chests.duplicate():   # collect_chest erases from the array
			if chest.alive and chest.position.distance_to(prop.position) < r \
					and prop.detonator != null:
				collect_chest(chest, prop.detonator)
		if r >= 50.0:
			prop.detonating = false
			world.destroy_prop(prop)


# MARK: - Death & loot (SPEC §5, §8)

## `how` is the DeathType enum shape: {"kind":"killed","by":idx} |
## {"kind":"drowned"} | {"kind":"detonated","by":idx} | {"kind":"forfeit"}.
func kill(victim, how: Dictionary) -> void:
	if not victim.active or victim.dying:
		return
	victim.dying = true
	victim.deaths += 1
	victim.respawn_timer = 4.0

	var kind: String = how.get("kind", "")
	var message := ""
	match kind:
		"killed":
			var killer = players[how["by"]]
			killer.kills += 1
			killer.cash += victim.cash
			if Audio.shared != null:
				Audio.shared.play("cash")
			# "<killer> <verb> You!" / "<killer> <verb> <name>" (Packet_Manager.java:312/401)
			message = "%s %s %s!" % [killer.name, G.DEATH_VERBS.pick_random(),
					"You" if victim == local_human else victim.name]
			if killer.is_bot:
				BotState.maybe_boast(killer, self)
			else:
				BotState.poll_compliments(killer, self)
		"detonated":
			var idx: int = how.get("by", -1)
			if idx >= 0:
				players[idx].kills += 1
				players[idx].cash += victim.cash
				if Audio.shared != null:
					Audio.shared.play("cash")
			# Packet_Manager.java:281/380
			message = "You Were Detonated!" if victim == local_human \
					else "%s Was Detonated!" % victim.name
		"drowned":
			victim.drownings += 1
			# Packet_Manager.java:256/352
			message = "You Were Drowned!" if victim == local_human \
					else "%s Was Drowned!" % victim.name
		"forfeit":
			# Packet_Manager.java:245/350
			message = "You Forfeit The Game!" if victim == local_human \
					else "%s Forfeits The Game!" % victim.name
	if victim.is_bot:
		BotState.maybe_death_cry(victim, self)
	# death penalty: cash resets to half STARTING cash
	@warning_ignore("integer_division")
	victim.cash = G.STARTING_CASH_TABLE[options.starting_cash_index] / 2

	Particles.death_blast(victim.position, world)
	@warning_ignore("integer_division")
	Particles.coins(victim.position, mini(victim.cash / 20, 75), world)
	if Audio.shared != null:
		Audio.shared.play("explosion1")
	camera.shock(victim.position, 120.0)
	if kind != "drowned":
		world.terrain.crater(victim.position.x, victim.position.z, 8, 30)

	victim.respawns_used += 1
	# forfeit skips remaining respawns (Cannon.java:1099 — death(bl=true))
	var is_forfeit := kind == "forfeit"
	var eliminated: bool = is_forfeit or victim.respawns_used > options.max_respawns
	if eliminated:
		victim.active = false
	victim.sync_node()
	if hud != null:
		hud.flash_message(message)
	# "You Lose!" / "<name> Loses!" chases the death message once the lives
	# run out (Packet_Manager.java:262/354); forfeits don't get one (:245)
	if eliminated and not is_forfeit and hud != null:
		hud.flash_message("You Lose!" if victim == local_human else "%s Loses!" % victim.name)
	if hud != null:
		hud.refresh_dynamic()
		hud.mark_minimap_dirty()

	_check_win_condition()
	# Local player out while the game goes on:
	if eliminated and not game_over and victim == local_human:
		if is_forfeit:
			# Forfeit goes straight to the end state with 5 s already on the
			# 15 s clock (GameState 13, GameStateTimeOut = 5000 —
			# Packet_Manager.java:246-247): 10 s more, then out. No spectating.
			forfeit_exit_timer = 10.0
		else:
			# GameState 15 (Packet_Manager.java:262-268): persistent
			# "You Lose!" center message, weapon put away, and a 5 s wait
			# before spectator mode kicks in.
			if hud != null:
				hud.show_success_message("You Lose!")
			victim.power_bar_active = false
			death_wait_timer = 5.0             # Cannon.WaitingTimer = 5.0f
	if not game_over and victim.index == current_player_index:
		var w: float = 3.0 if waiting_timer == null else float(waiting_timer)
		waiting_timer = minf(w, 3.0)


## Match decided — any death that leaves fewer than 2 active players
## (Packet_Manager.java:416-448): GameState 13, success camera (setCamera(6)),
## and the persistent winner message.
func _check_win_condition() -> void:
	var alive_players: Array = players.filter(func(p) -> bool: return p.active)
	if alive_players.size() > 1 or game_over:
		return
	game_over = true
	game_over_timer = 0.0                    # GameStateTimeOut = 0 (:420/429/438)
	winner_index = alive_players[0].index if not alive_players.is_empty() else null
	death_wait_timer = null
	forfeit_exit_timer = null                # a winner restarts the full 15 s clock
	camera.set_success_mode()                # Packet_Manager.java:430/439
	# Packet_Manager.java:426-433 — "You Win!" when the local player is the
	# survivor; :435-447 — "<name> Wins!" for whoever else is left standing.
	if not alive_players.is_empty() and hud != null:
		var w = alive_players[0]
		hud.show_success_message("You Win!" if w == local_human else "%s Wins!" % w.name)


# MARK: - Turn flow (SPEC §4)

func begin_turn(announce: bool) -> void:
	var p = current_player
	p.has_fired_this_turn = false
	p.fired_on_turn = false
	hot_seat_remaining = float(G.HOT_SEAT_TIMES[options.hot_seat_index])
	_last_tick_second = -1
	if p.is_bot:
		p.bot.begin_turn(p, self)
	if announce:
		if hud != null:
			if p.is_bot:
				hud.show_banner("%s's Turn" % p.name)
			else:
				var humans: Array = players.filter(func(q) -> bool: return not q.is_bot)
				hud.show_banner("%s — YOUR TURN" % p.name if humans.size() > 1 else "YOUR TURN")
		# every turn change opens on the drum roll (HUD.showMyTurn /
		# showOtherTurn — HUD.java:718, 760)
		if Audio.shared != null:
			Audio.shared.play("drumroll")
	if hud != null:
		hud.refresh_dynamic()
		hud.mark_minimap_dirty()


func switch_players() -> void:
	if game_over:
		return
	var idx := current_player_index
	for i in players.size():
		idx = (idx + 1) % players.size()
		if players[idx].active:
			break
	current_player_index = idx
	begin_turn(true)
	# spectator camera follows the new current player and re-labels itself
	# (Game_Loop.switchPlayers, Game_Loop.java:435-437)
	if camera.mode == CameraController.Mode.SPECTATOR and hud != null:
		hud.refresh_spectator_name(current_player.name)


# MARK: - Frame update

func update(frame_dt: float) -> void:
	if frame_dt <= 0:
		return
	game_time += frame_dt
	var ms := frame_dt * 1000.0
	var n := mini(5, maxi(1, int(ceil(ms / 40.0))))
	var sub := frame_dt / float(n)

	for i in n:
		_substep(sub)

	var dt := frame_dt
	# The Swift calls world.terrain.update(dt) here (its water was
	# CAAnimation-driven); the Godot World.update folds the terrain ease AND
	# the water animations together — call it ONCE, and read "terrain moved"
	# as current != target beforehand (equivalent to terrain.update's return).
	var terrain_moving: bool = world.terrain.current != world.terrain.target
	world.update(dt)
	if terrain_moving:
		world.reground_decorations()
		for chest in chests:
			if chest.alive:
				chest.reground(world.terrain)
	for c in players:
		if c.active and not c.dying:
			c.to_ground()
			c.sync_node()
	# barrel recoil actor: the macOS fire-recoil CAKeyframeAnimation ran on the
	# render clock; here the game loop drives it explicitly (cannon.gd:update)
	for c in players:
		c.update(dt)
	_update_shockwaves(dt)
	_update_timers(dt)
	for c in players:
		if c.is_bot and c.active:
			c.bot.think(c, self, dt)
	camera.update(dt)
	# sky dome follows the camera — FULL position including Y
	# (Camera.java:583-586: Environment.setPosition(X, Y, Z)). NOT here:
	# sky_actor.gd is self-driving (its _process follows the active camera —
	# GameController.swift:428-432 dropped by design, see sky_actor.gd header).
	#
	# cloud shadows drift with the wind (Island.java: offset -= wind * 0.01 * dt)
	var terrain_mat := world.terrain.node.material_override as ShaderMaterial
	if terrain_mat != null:
		_cloud_offset = WorldDressing.drift_clouds(terrain_mat, wind, dt, _cloud_offset)
	if _lens_flare != null:
		_lens_flare.update(camera.node, dt, world.terrain)

	_minimap_refresh -= dt
	if _minimap_refresh <= 0:
		_minimap_refresh = 0.15
		if hud != null:
			hud.update_minimap()
	if hud != null:
		hud.update(dt)

	# Ambient fish: 0.0075 chance per frame to leap from open water near the
	# island center, launched ((r-0.5)*20, 30+r*10, (r-0.5)*20) with splashes.
	if randf() < 0.0075:
		var c := world.center()
		var x := c.x + randf_range(-400.0, 400.0)
		var z := c.z + randf_range(-400.0, 400.0)
		if world.terrain.height(x, z) <= 0:
			Particles.fish_jump(Vector3(x, 0, z),
					Vector3(randf_range(-10.0, 10.0),
							30 + randf_range(0.0, 10.0),
							randf_range(-10.0, 10.0)),
					world)


func _substep(dt: float) -> void:
	# aiming: only the controlled human cannon uses live keys; bots slew in BotAI
	var c = controlled_cannon
	if c != null:
		c.spin_input(dt)
		c.update_power_bar(dt)
	for p in players:
		if p.is_bot and p.active and not p.dying:
			p.bot.slew(p, dt)
	for p in projectiles.duplicate():   # projectile_died erases from the array
		p.update(dt)


func _update_timers(dt: float) -> void:
	if game_over:
		# GameState 13 (updateGameStateDestroy, Game_Loop.java:97-106): the
		# world keeps running under the success camera for 15 s, then the
		# original dissolves out and tears the game down. The clone leaves
		# the match via the results screen instead of the bare main menu.
		game_over_timer += dt
		if game_over_timer > 15.0 and hud != null and not hud.results_shown:
			hud.show_results()
		return
	# local forfeit: the remaining 10 s of the destroy state, then leave
	# (Game_Loop.updateGameStateDestroy with GameStateTimeOut preloaded to 5000)
	if forfeit_exit_timer != null:
		var f: float = forfeit_exit_timer
		if f - dt <= 0.0:
			forfeit_exit_timer = null
			if hud != null and not hud.results_shown:
				hud.show_results()
		else:
			forfeit_exit_timer = f - dt
	# GameState 15 → 11 (Cannon.updateDeathWaitingTimer, Cannon.java:1480-1497):
	# 5 s after the local "You Lose!", switch to spectator mode while the
	# match plays out. (The other-players-gone branch never reaches here —
	# checkWinCondition already flipped gameOver.)
	if death_wait_timer != null:
		var t: float = death_wait_timer
		if t - dt <= 0.0:
			death_wait_timer = null
			spectating = true
			camera.set_spectator_mode()          # Cannon.java:1487
			if hud != null:
				hud.enter_spectator_mode(current_player.name)
		else:
			death_wait_timer = t - dt
	# respawns (SPEC: 4 s)
	for c in players:
		if not c.dying:
			continue
		c.respawn_timer -= dt
		if c.respawn_timer <= 0:
			c.dying = false
			if c.active:
				c.place()
				c.to_ground()
				Particles.teleport(c.position, world)
				if Audio.shared != null:
					Audio.shared.play("teleport")
				c.sync_node()
				if hud != null:
					hud.mark_minimap_dirty()
	# after-shot wait → next turn
	if waiting_timer != null:
		var nw: float = float(waiting_timer) - dt
		if nw <= 0.0:
			waiting_timer = null
			switch_players()
		else:
			waiting_timer = nw
	# hotseat countdown (only while current player still may fire)
	if hot_seat_remaining > 0.0 and waiting_timer == null and not current_player.dying:
		var has_live_shot := false
		for p in projectiles:
			if p.owner == current_player:
				has_live_shot = true
				break
		if not has_live_shot and not current_player.has_fired_this_turn:
			hot_seat_remaining -= dt
			var s := int(ceil(hot_seat_remaining))
			if hot_seat_remaining <= 10.0 and s != _last_tick_second:
				_last_tick_second = s
				if Audio.shared != null:
					Audio.shared.play("timer_tick")
			if hot_seat_remaining <= 0.0:
				if hud != null:
					hud.flash_message("Time Up!")
				if Audio.shared != null:
					Audio.shared.play("time_up")
				current_player.power_bar_active = false
				switch_players()


# MARK: - Input
# The Swift keyCodes map to Godot keycodes here: 123/124/126/125 = arrows,
# 49 = space, 24/69 = '='/numpad-+, 27/78 = '-'/numpad--, 9 = V, 12 = Q,
# 53 = esc, 36 = return, 8 = C. Returns true when the event was consumed.

func key_down(event: InputEventKey) -> bool:
	if hud != null and hud.results_shown:
		# leave the terminal results screen on Enter / Space / Esc (any of the
		# obvious "continue" keys), matching the any-click dismiss in hud.click
		if event.keycode == KEY_ENTER or event.keycode == KEY_ESCAPE \
				or event.keycode == KEY_SPACE:
			if on_exit.is_valid():
				on_exit.call()
		return true
	# chat entry swallows everything while open (Chat.java: controls disabled)
	if hud != null and hud.chat_key_down(event):
		return true
	if event.keycode == KEY_C:   # C — open chat entry (HUD.java:501)
		var cc = controlled_cannon
		if cc != null:
			cc.key_left = false
			cc.key_right = false
			cc.key_up = false
			cc.key_down = false
			if Audio.shared != null:
				Audio.shared.stop_loop("turn_loop")
				Audio.shared.stop_loop("tilt")
		if hud != null:
			hud.begin_chat_entry()
		return true
	var c = controlled_cannon
	# NOTE: the Swift starts the turn_loop/tilt audio loops here; the GDScript
	# cannon.spin_input drives them per-substep from the key flags (Java-side
	# transcription) — starting them here too would double-drive the loops.
	match event.keycode:
		KEY_LEFT:
			if c != null:
				c.key_left = true
			return true
		KEY_RIGHT:
			if c != null:
				c.key_right = true
			return true
		KEY_UP:
			if c != null:
				c.key_up = true
			return true
		KEY_DOWN:
			if c != null:
				c.key_down = true
			return true
		KEY_SPACE:
			if c != null:
				c.trigger_fire()
			return true
		KEY_EQUAL, KEY_KP_ADD:                       # = / +
			cycle_weapon(1)
			return true
		KEY_MINUS, KEY_KP_SUBTRACT:                  # - / numpad-
			cycle_weapon(-1)
			return true
		KEY_V:
			camera.cycle()
			return true
		KEY_Q:   # Q: forfeit while playing; as a spectator it leaves the game
			if c != null:
				kill(c, {"kind": "forfeit"})
			elif spectating and not game_over:
				if on_exit.is_valid():
					on_exit.call()   # HUD.keyDownSpectator (HUD.java:695-698)
			return true
		KEY_ESCAPE:                                  # esc → menu
			if on_exit.is_valid():
				on_exit.call()
			return true
		_:
			return false


func key_up(event: InputEventKey) -> bool:
	var c = controlled_cannon
	match event.keycode:
		KEY_LEFT:
			if c != null:
				c.key_left = false
			return true
		KEY_RIGHT:
			if c != null:
				c.key_right = false
			return true
		KEY_UP:
			if c != null:
				c.key_up = false
			return true
		KEY_DOWN:
			if c != null:
				c.key_down = false
			return true
		_:
			return false


# MARK: - Menu-driven actions (mirror the keyboard controls)

func menu_fire() -> void:
	if hud != null and hud.results_shown:
		return
	var c = controlled_cannon
	if c != null:
		c.trigger_fire()


func menu_next_weapon() -> void:
	if hud != null and hud.results_shown:
		return
	cycle_weapon(1)


func menu_prev_weapon() -> void:
	if hud != null and hud.results_shown:
		return
	cycle_weapon(-1)


func menu_forfeit() -> void:
	var c = controlled_cannon
	if c != null:
		kill(c, {"kind": "forfeit"})


## Island.switchShadows: toggle every SHADOW blob patch in the scene.
func set_shadows_visible(on: bool) -> void:
	_set_shadows(world.root, on)


static func _set_shadows(node: Node, on: bool) -> void:
	if node.name == "blob-shadow" and node is Node3D:
		(node as Node3D).visible = on
	for child in node.get_children():
		_set_shadows(child, on)


func menu_back_to_menu() -> void:
	if on_exit.is_valid():
		on_exit.call()


func menu_set_camera(mode: int) -> void:
	camera.set_mode(mode)


func cycle_weapon(delta: int) -> void:
	var c = controlled_cannon
	if c == null:
		return
	var idx: int = c.weapon_index
	for i in 12:   # WeaponType.allCases.count
		idx = (idx + delta + 12) % 12
		var off_turn: bool = current_player_index != c.index
		if c.cash >= G.WEAPON_COSTS[idx] and not (off_turn and G.WEAPON_OFFENSIVE[idx]):
			break
	c.weapon_index = idx
	if Audio.shared != null:
		Audio.shared.play("click")
	if hud != null:
		hud.refresh_dynamic()


func select_weapon(idx: int) -> void:
	var c = controlled_cannon
	if c == null or idx < 0 or idx >= 12:
		return
	var off_turn: bool = current_player_index != c.index
	if c.cash < G.WEAPON_COSTS[idx]:
		if hud != null:
			hud.flash_message("Not Enough Gold For That Weapon!")
		return
	if off_turn and G.WEAPON_OFFENSIVE[idx]:
		if hud != null:
			hud.flash_message("Only Defensive Items Can Be Used On Your Off Turn!")
		return
	c.weapon_index = idx
	if Audio.shared != null:
		Audio.shared.play("click")
	if hud != null:
		hud.refresh_dynamic()


## HUD click passthrough (point in the SpriteKit y-up 800x600 convention the
## menu/HUD public surfaces share — menu_scene.gd click()).
func click(p: Vector2) -> bool:
	if hud != null:
		return hud.click(p)
	return false
