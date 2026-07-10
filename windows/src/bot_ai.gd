class_name BotState
extends RefCounted
# Port of macos/Sources/Cannonballs/BotAI.swift + BotChat.swift (which cite
# Cannon.java AIThink and Network.java).
# Bot AI per SPEC §9. Types: 1 Dummy, 2 Aggressive, 3 Thinker, 4 Crazy.
#
# GDScript port notes (windows/PORTING.md):
# - `c` (Cannon) / `game` (GameController) params stay duck-typed until
#   game_controller.gd lands. The game loop calls slew(c, dt) per substep and
#   think(c, game, dt) per frame explicitly (no _process).
# - The BotChat tables live here as statics (BotChat.swift has no own file in
#   the file map): the GameController port calls BotState.maybe_death_cry /
#   maybe_boast / poll_compliments.
# - Where the decompiled Java disagrees with the Swift, the Java wins
#   (PORTING.md rule 4); every such spot is marked "Java outranks" inline.

var target_index := -1                  # Java CurrentTarget (-1 = none)
var has_greeted := false                # original: greeting posted once, gates insults
var los_armed := false                  # two-phase LineOfSight trick (Cannon.java:808-815)
var last_impact = null                  # own last shot impact point (Vector3 or null)
var first_shot_at_target := true
var recent_enemy_impacts: Array = []    # of {pos: Vector3, shooter: int}

var solution_azimuth := 0.0             # deg
var solution_elev := 0.0                # deg barrel elevation
var solution_power := 0.5
var have_solution := false

var fire_delay := 0.0                   # thinking time before acting
var defense_tick := 0.0

# the Java move-reset memory (Cannon.java:672-694)
var _last_target_position := Vector3.ZERO
var _last_bot_position := Vector3.ZERO


## GameController calls this whenever any projectile dies so bots can dodge
## (Weapon.java hide() stores LastHitX/Z per cannon; the shooter index keeps
## the attribution the Java threat scan reads, Cannon.java:1394-1431).
func note_enemy_impact(p: Vector3, shooter_index := -1) -> void:
	recent_enemy_impacts.append({"pos": p, "shooter": shooter_index})
	if recent_enemy_impacts.size() > 4:
		recent_enemy_impacts.pop_front()


# MARK: - Turn start

func begin_turn(c, game) -> void:
	BotState.maybe_greet(c, game)
	_pick_target(c, game)
	_compute_solution(c, game)
	_choose_weapon(c, game)
	# per-type think time (the Java BotTimeTarget rerolls, Cannon.java:981-996)
	match c.bot_type:
		1: fire_delay = randf_range(3, 8)      # Dummy
		2: fire_delay = randf_range(1, 5)      # Aggressive
		3: fire_delay = randf_range(2, 6)      # Thinker
		_: fire_delay = randf_range(1, 4)      # Crazy


func _pick_target(c, game) -> void:
	var enemies: Array = []
	for p in game.players:
		if p != c and p.active:
			enemies.append(p)
	if enemies.is_empty():
		target_index = -1
		return
	var previous := target_index
	var nearest = enemies[0]
	for e in enemies:
		if G.dist_2d(e.position, c.position) < G.dist_2d(nearest.position, c.position):
			nearest = e
	match c.bot_type:
		4:
			target_index = enemies.pick_random().index         # Crazy: random each shot (Cannon.java:646-648)
		1:
			target_index = nearest.index if randf() < 0.5 else enemies.pick_random().index  # Cannon.java:654-660
		_:
			target_index = nearest.index                       # findNearestTarget (Cannon.java:650-663)
	if target_index != previous:
		first_shot_at_target = true
		last_impact = null
		if target_index >= 0:
			BotState.maybe_insult(c, game.players[target_index], game)
	# dead target safety
	if target_index >= 0 and not game.players[target_index].active:
		target_index = nearest.index
		first_shot_at_target = true


# MARK: - Ballistic solution

func _compute_solution(c, game) -> void:
	if target_index < 0:
		have_solution = false
		return
	var target = game.players[target_index]
	var dist: float = G.dist_2d(target.position, c.position)

	# Java resets to a fresh solve whenever the target or the bot itself has
	# moved since the last adjustment (Cannon.java:672-694 zeroes LastHit and
	# the offsets on any position change — replaces the Swift's err>150 rule;
	# Java outranks).
	if not first_shot_at_target and last_impact != null:
		var tm := Vector2(target.position.x - _last_target_position.x,
				target.position.z - _last_target_position.z).length()
		var bm := Vector2(c.position.x - _last_bot_position.x,
				c.position.z - _last_bot_position.z).length()
		if tm > 0.1 or bm > 0.1:
			first_shot_at_target = true
			last_impact = null
	_last_target_position = target.position
	_last_bot_position = c.position

	if first_shot_at_target or last_impact == null:
		# wind lead: aim point shifted by -Wind * dist * 0.02 (Cannon.java:700-703)
		var aim_point: Vector3 = target.position - game.wind * dist * 0.02
		var dx: float = aim_point.x - c.position.x
		var dz: float = aim_point.z - c.position.z
		solution_azimuth = rad_to_deg(atan2(dx, dz))   # G.rad2deg (Types.swift:26)
		var dy: float = target.position.y - c.position.y
		var tilt_offset := (randf() - 0.1) * 20.0      # Cannon.java:737-739
		# elevation = line to target + the 45° lob baseline + offset
		# (Cannon.java:731-743; the clamp happens at the slew, Java-style)
		solution_elev = rad_to_deg(atan2(dy, dist)) + 45.0 + tilt_offset
		solution_power = minf(1, dist / 700.0)         # Cannon.java:757-761
		first_shot_at_target = false
	else:
		# walking fire: compare last impact to target (Cannon.java:704-729 az,
		# :860-881 power)
		var impact: Vector3 = last_impact
		var err_factor := minf(dist / 300.0, 1.0)      # Cannon.java:708-711
		# lateral sign: is impact left or right of the firing line?
		var to_target := Vector3(target.position.x - c.position.x, 0,
				target.position.z - c.position.z)
		var to_impact := Vector3(impact.x - c.position.x, 0, impact.z - c.position.z)
		var cross_y := to_target.z * to_impact.x - to_target.x * to_impact.z
		var lateral := absf(cross_y) / maxf(dist, 1)
		if lateral < 5.0:
			err_factor *= 0.5   # |Temp2.X| < 5 → fine azimuth (Cannon.java:712-714; the Swift used err*0.3 — Java outranks)
		# per-type scatter joins the FACTOR, not the step (Cannon.java:715-723;
		# the Swift added it to the step after the multiply — Java outranks)
		if c.bot_type == 1:
			err_factor += randf() * 2.0
		if c.bot_type == 4:
			err_factor += randf() * 1.0
		var az_step := (1.0 + randf()) * 4.0 * err_factor   # Cannon.java:724-728
		solution_azimuth += -az_step if cross_y > 0 else az_step
		# range: overshoot vs undershoot, measured bot→impact vs bot→target;
		# fine steps once the RANGE error is inside 15 (Cannon.java:860-881 —
		# the Swift keyed both off the impact-to-target distance and added a
		# 1.8x amplifier that isn't in the source; Java outranks)
		var impact_range := to_impact.length()   # f5
		var f8 := 1.0
		if impact_range != 0.0 and absf(impact_range - dist) < 15.0:
			f8 = 0.2                                    # Cannon.java:861-864
		if c.bot_type == 1:
			solution_power += (randf() - 0.5) * 0.2     # Dummy power jitter (Cannon.java:865-868)
		var p_step := (0.05 + randf() * 0.1) * f8       # Cannon.java:870-873
		solution_power += -p_step if impact_range > dist else p_step
		solution_power = clampf(solution_power, 0, 1)
	have_solution = true


# MARK: - Weapon choice (SPEC §9)

func _choose_weapon(c, game) -> void:
	c.weapon_index = G.Weapon.CANNONBALL   # setWeapon(0) each cycle (Cannon.java:769)
	if target_index < 0:
		return
	var target = game.players[target_index]
	var dist: float = G.dist_2d(target.position, c.position)

	var f6: float = 0.5 if c.bot_type == 1 else 1.0   # source: Dummy halves special-weapon odds (Cannon.java:779-799)
	# clear line of sight → Dumbfire. TWO-PHASE like the source: the first
	# pass only ARMS the shot (LineOfSight=true, return and wait a think
	# cycle); the next pass takes it (Cannon.java:800-827).
	if c.cash >= G.WEAPON_COSTS[G.Weapon.DUMBFIRE] \
			and _line_of_sight_clear(c, target, game) \
			and (los_armed or randf() < 0.75 * f6):
		if not los_armed:
			los_armed = true
			fire_delay += randf() * 4.0   # the extra BotTimeTarget cycle (Cannon.java:766/810-812)
			return
		c.weapon_index = G.Weapon.DUMBFIRE
		# LineOfSight drops the 45° lob and the tilt offset: aim straight at
		# the target (Cannon.java:734-742). Power stays on the walked solution
		# (the Swift's dist/700+0.2 floor isn't in the source — Java outranks).
		solution_elev = rad_to_deg(atan2(target.position.y - c.position.y, dist))
		return
	los_armed = false

	if last_impact == null:
		return   # the special-weapon picks all require a previous impact (Cannon.java:772)
	var to_target := Vector3(target.position.x - c.position.x, 0,
			target.position.z - c.position.z)
	var to_impact := Vector3(last_impact.x - c.position.x, 0, last_impact.z - c.position.z)
	var cross_y := to_target.z * to_impact.x - to_target.x * to_impact.z
	var lateral := absf(cross_y) / maxf(dist, 1)
	var impact_range := to_impact.length()
	var last_err: float = G.dist_2d(last_impact, target.position)

	# rolling weapons when nearly on line AND undershooting (Cannon.java:828-841:
	# |Temp2.X| < 10 && f5 < TargetDistance — the Swift's err<10; Java outranks)
	if lateral < 10.0 and impact_range < dist and randf() < 0.5 * f6:
		if c.bot_type != 2 and randf() < 0.1 \
				and c.cash >= G.WEAPON_COSTS[G.Weapon.BOUNCER]:
			c.weapon_index = G.Weapon.BOUNCER
			return
		if c.cash >= G.WEAPON_COSTS[G.Weapon.SPIKEROLLER]:
			c.weapon_index = G.Weapon.SPIKEROLLER
			return
	# near-miss heavy hitters (Cannon.java:842-858; f7 bias per type :779-799)
	var bias := 0.0
	match c.bot_type:
		4: bias = 30.0
		2: bias = 10.0
		1: bias = -5.0
		_: bias = 0.0
	if last_err < 35.0 + bias and randf() < 0.5 * f6:
		if target.position.y < 20 and c.bot_type != 4 and randf() < 0.5 \
				and c.cash >= G.WEAPON_COSTS[G.Weapon.SUPERCRATER]:
			c.weapon_index = G.Weapon.SUPERCRATER
			return
		if randf() < 0.25 and c.cash >= G.WEAPON_COSTS[G.Weapon.TNT]:
			c.weapon_index = G.Weapon.TNT
			return
		if c.cash >= G.WEAPON_COSTS[G.Weapon.XSHOT]:
			c.weapon_index = G.Weapon.XSHOT
			return


## The Java LoS probe is a collision ray from pos+dir*6 to target-dir*6 over
## everything (mask 0xFFFFFFF, Cannon.java:800-806): terrain patches AND prop
## meshes. Sampled terrain + the prop-mesh segment sweep approximate it.
func _line_of_sight_clear(c, target, game) -> bool:
	var steps := 24
	var start: Vector3 = c.position + Vector3(0, 2, 0)
	var end: Vector3 = target.position + Vector3(0, 2, 0)
	for i in range(1, steps):
		var t := float(i) / float(steps)
		var p := start + (end - start) * t
		if game.world.terrain.height(p.x, p.z) > p.y:
			return false
	if game.world.collide_segment(start, end) != null:
		return false
	return true


# MARK: - Per-substep slew (physical aiming animation)

func slew(c, dt: float) -> void:
	if not have_solution:
		return
	# updateLocalClientBot (Cannon.java:496-554): BOTH axes slew linearly at
	# MaxSpinSpeed with an exact snap. (The Swift eased the tilt exponentially —
	# Java outranks.)
	var delta := fmod(solution_azimuth - c.spin_angle, 360.0)
	if delta > 180:
		delta -= 360
	if delta < -180:
		delta += 360
	var step: float = G.MAX_SPIN_SPEED * dt
	if absf(delta) <= step:
		c.spin_angle = solution_azimuth
	else:
		c.spin_angle += step if delta > 0 else -step
	var want := _target_active_tilt()
	var tdelta: float = want - c.active_tilt
	if absf(tdelta) <= step:
		c.active_tilt = want
	else:
		c.active_tilt += step if tdelta > 0 else -step


## The ActiveTilt target: RemoteTiltTarget = -elevation, clamped to
## [-MinTiltAngle, MaxTiltAngle] (Cannon.java:750-756). NOTE the original's
## clamp is the MIRROR of the player's range: bots can tilt at most 30° UP and
## 60° DOWN (the Swift flipped it to the player's -60..30 — Java outranks;
## this is why the original's bots always lob at the full 30°).
func _target_active_tilt() -> float:
	return clampf(-solution_elev, -G.MIN_TILT_ANGLE, G.MAX_TILT_ANGLE)


# MARK: - Per-frame think

func think(c, game, dt: float) -> void:
	if not c.active or c.dying or game.game_over:
		return

	if game.current_player_index == c.index:
		if c.has_fired_this_turn or game.waiting_timer != null:
			return
		fire_delay -= dt
		if fire_delay > 0 or not have_solution:
			return
		var az_delta := fmod(solution_azimuth - c.spin_angle, 360.0)
		if az_delta > 180:
			az_delta -= 360
		if az_delta < -180:
			az_delta += 360
		var tilt_ready: bool = absf(c.active_tilt - _target_active_tilt()) < 2
		if absf(az_delta) < 1 and tilt_ready:
			c.fire(solution_power)
	else:
		# Off-turn harassment/defense (Cannon.java AIThink off-turn branch).
		# Source gates the whole block on: aim fully settled, own WaitingTimer
		# zero, no own shot in flight — then rerolls a per-type 1–8 s timer.
		# Without those gates bots lobbed constantly at unsettled angles.
		defense_tick -= dt
		if defense_tick > 0:
			return
		if not have_solution or game.waiting_timer != null:
			return
		for p in game.projectiles:
			if p.owner == c:
				return
		var az_delta := fmod(solution_azimuth - c.spin_angle, 360.0)
		if az_delta > 180:
			az_delta -= 360
		if az_delta < -180:
			az_delta += 360
		var tilt_ready: bool = absf(c.active_tilt - _target_active_tilt()) < 2
		if absf(az_delta) >= 1 or not tilt_ready:
			return
		match c.bot_type:                                   # source reroll intervals (Cannon.java:981-996)
			1: defense_tick = 3 + randf() * 5
			2: defense_tick = 1 + randf() * 4
			3: defense_tick = 2 + randf() * 4
			_: defense_tick = 1 + randf() * 3
		# 1) molehill lob at the target (source fires weapon 1 with these odds,
		# gated on a 520-gold reserve — Cannon.java:897-943)
		var molehill_chance := 0.1
		match c.bot_type:
			1: molehill_chance = 0.01
			2: molehill_chance = 0.25
			3: molehill_chance = 0.125
			4: molehill_chance = 0.15
		if target_index >= 0 and game.players[target_index].active \
				and c.cash >= 520 and randf() < molehill_chance:
			var keep: int = c.weapon_index
			c.weapon_index = G.Weapon.MOLEHILL
			c.fire(solution_power)
			c.weapon_index = keep
			return
		# 2) tower if target far above (Cannon.java:944-949)
		if target_index >= 0 and game.players[target_index].active \
				and game.players[target_index].position.y - c.position.y > 30 \
				and c.cash >= G.WEAPON_COSTS[G.Weapon.TOWER] and randf() < 0.1 \
				and not game.world.object_above(c.position.x, c.position.z):
			c.cash -= G.WEAPON_COSTS[G.Weapon.TOWER]
			c.gold_spent += G.WEAPON_COSTS[G.Weapon.TOWER]
			# doTower raise, no splat; Island.molehill quakes + rays itself
			game.world.terrain.molehill(c.position.x, c.position.z, 40, 30)
			Projectile.quake_rays_fx(c.position.x, c.position.z, 30.0, game.world)
			return
		# 3) teleport away from danger (Cannon.java:950-979)
		if _find_death_threat(c, game):
			var tp_chance := 0.1
			match c.bot_type:
				1: tp_chance = 0.025
				2: tp_chance = 0.1
				3: tp_chance = 0.2
				4: tp_chance = 0.15
			if randf() < tp_chance and c.cash >= G.WEAPON_COSTS[G.Weapon.TELEPORT]:
				c.cash -= G.WEAPON_COSTS[G.Weapon.TELEPORT]
				c.gold_spent += G.WEAPON_COSTS[G.Weapon.TELEPORT]
				c.player_teleport()


## An enemy's recent impact inside the per-type panic radius (non-team table,
## Cannon.java:1403-1431: Dummy 30, Aggressive 40, Thinker 50, Crazy 40 — the
## Swift's Aggressive 60 — Java outranks). Aggressive and Crazy additionally
## retarget the shooter 50% of the time when the attribution is known.
func _find_death_threat(c, game) -> bool:
	var threshold := 40.0
	match c.bot_type:
		1: threshold = 30.0
		2: threshold = 40.0
		3: threshold = 50.0
		4: threshold = 40.0
	for entry in recent_enemy_impacts:
		if G.dist_2d(entry.pos, c.position) < threshold:
			if (c.bot_type == 2 or c.bot_type == 4) and entry.shooter >= 0 \
					and entry.shooter != c.index and randf() < 0.5 \
					and game.players[entry.shooter].active:
				# Cannon.java:1410-1415 / 1424-1429: lock onto the attacker
				target_index = entry.shooter
				first_shot_at_target = true
				last_impact = null
				los_armed = false
			return true
	return false


# MARK: - Bot chat (BotChat.swift; tables VERBATIM from Network.java:71-80)

# Network.java string tables (counts as in source: ComplimentCount=5, so
# "That was sweet" is unreachable in the original — kept for the record).
const INSULTS := ["I'm gonna get you,", "You better watch out",
		"Here comes the pain", "I got you in my sights", "You're going down"]
const GREETINGS := ["Hi everybody", "Hello", "Hi guys!", "Greets", "Hola",
		"Hey", "Hey folks", "What's up?", "Howdy"]
const DEATHS := ["Ow!", "Ouch!", "That hurt!", "Good grief!"]
const KILLS := ["Oh yeah!", "I am the king!", "Sweet!", "Take that!",
		"Woohoo!", "I rule!", "Bow down before me!!"]
const COMPLIMENTS := ["Nice shot,", "That was a nice one,", "Good shot",
		"Nice moves", "What a shot"]   # [5] "That was sweet" never picked


static func _post(bot_cannon, msg: String, game) -> void:
	if game.hud != null:
		game.hud.bot_chat(bot_cannon.name, msg)


## First ready turn: 25% greeting (Cannon.java:891-895).
static func maybe_greet(bot_cannon, game) -> void:
	if bot_cannon.bot.has_greeted:
		return
	bot_cannon.bot.has_greeted = true
	if randf() < 0.25:
		_post(bot_cannon, GREETINGS.pick_random(), game)


## New target acquired: 10% targeted insult (Cannon.java:1199-1201 rate).
static func maybe_insult(bot_cannon, target, game) -> void:
	if not bot_cannon.bot.has_greeted or randf() >= 0.1:
		return
	_post(bot_cannon, "%s %s!" % [INSULTS.pick_random(), target.name], game)


## The bot just died: 25% pained cry (Cannon.java:1132-1135).
static func maybe_death_cry(bot_cannon, game) -> void:
	if randf() < 0.25:
		_post(bot_cannon, DEATHS.pick_random(), game)


## The bot's shot just killed someone: 25% boast (Weapon.java:1057-1059).
static func maybe_boast(bot_cannon, game) -> void:
	if randf() < 0.25:
		_post(bot_cannon, KILLS.pick_random(), game)


## The human player killed someone: each bot 10% compliment
## (Network.pollBotsForCompliments, Weapon.java:1122-1124).
static func poll_compliments(human, game) -> void:
	for b in game.players:
		if b.is_bot and b.active and randf() < 0.1:
			_post(b, "%s %s!" % [COMPLIMENTS.pick_random(), human.name], game)
