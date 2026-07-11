class_name G
# Port of macos/Sources/Cannonballs/Types.swift.
# Global constants (SPEC Global.*)

const GRAVITY := -32.0
const MAX_SPIN_SPEED := 100.0        # deg/s
const MIN_TILT_ANGLE := 30.0
const MAX_TILT_ANGLE := 60.0
const KILL_RADIUS_CANNON := 8.0
const KILL_RADIUS_CHEST := 6.0
const PROP_PAD := 2.0
const STARTING_CASH_TABLE := [0, 100, 200, 500, 1000, 2000, 3000, 5000]
const HOT_SEAT_TIMES := [0, 20, 30, 60, 90, 120]   # 0 = NA
const COLOR_RGB := [
	Color(38 / 255.0, 164 / 255.0, 255 / 255.0),   # blue
	Color(255 / 255.0, 77 / 255.0, 224 / 255.0),   # purple
	Color(255 / 255.0, 53 / 255.0, 53 / 255.0),    # red
	Color(102 / 255.0, 255 / 255.0, 113 / 255.0),  # green
]
const BOT_NAMES := ["CptBligh", "Patches", "MastahP", "CptHook", "LJSilver", "Mutiny",
					"ParrotBob", "SquareJaw", "Loot", "Digger", "BountyTom", "OneEye"]
const DEATH_VERBS := ["Beats", "Clobbers", "Whacks", "Wastes", "Mangles", "Trounces", "Crushes", "Destroys"]


static func dist_2d(a: Vector3, b: Vector3) -> float:
	var dx := a.x - b.x
	var dz := a.z - b.z
	return sqrt(dx * dx + dz * dz)


# Weapons (SPEC §6). WeaponType raw values 0..11:
enum Weapon {
	CANNONBALL = 0, MOLEHILL, CRATER, TOWER, SUPERCRATER, XSHOT,
	BOUNCER, DUMBFIRE, SPIKEROLLER, TNT, TELEPORT, TARGET_TELEPORT,
}

const WEAPON_NAMES := ["Cannonballs", "Molehill", "Crater", "Tower", "Supercrater", "X-Shot",
						"Bouncer", "Dumbfire", "SpikeRoller", "TNT", "Teleport", "TargetTeleport"]
const WEAPON_COSTS := [0, 20, 20, 200, 200, 300, 300, 400, 400, 500, 500, 800]
# offensive: false for molehill, tower, teleport, targetTeleport
const WEAPON_OFFENSIVE := [true, false, true, false, true, true, true, true, true, true, false, false]
# isProjectile: false for tower, teleport
const WEAPON_IS_PROJECTILE := [true, true, true, false, true, true, true, true, true, true, false, true]
# Can kill a cannon by direct hit (Global.PROJECTILEIMPACT: indices 0,5,6,7,8,9).
# crater/supercrater/molehill deform terrain, don't direct-kill.
const WEAPON_IMPACT_KILL := [true, false, false, false, false, true, true, true, true, true, false, false]


# Game options (PlayerConfig / GameOptions in Types.swift)

class PlayerConfig:
	var name: String
	var color_index: int
	var bot_type: int    # 0 human, 1 dummy, 2 aggressive, 3 thinker, 4 crazy

	func _init(p_name: String, p_color: int, p_bot: int) -> void:
		name = p_name
		color_index = p_color
		bot_type = p_bot


class GameOptions:
	var map_index := 0
	var players: Array = []          # of PlayerConfig
	var starting_cash_index := 4     # 1000
	var max_respawns := 2
	var hot_seat_index := 0          # NA
	var treasure_respawn := true
