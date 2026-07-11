import Foundation
import SceneKit

// MARK: - Global constants (SPEC Global.*)

enum G {
    static let gravity: Float = -32.0
    static let maxSpinSpeed: Float = 100          // deg/s
    static let minTiltAngle: Float = 30
    static let maxTiltAngle: Float = 60
    static let killRadiusCannon: Float = 8
    static let killRadiusChest: Float = 6
    static let propPad: Float = 2
    static let startingCashTable = [0, 100, 200, 500, 1000, 2000, 3000, 5000]
    static let hotSeatTimes = [0, 20, 30, 60, 90, 120]   // 0 = NA
    static let colorRGB: [(Float, Float, Float)] = [
        (38 / 255.0, 164 / 255.0, 255 / 255.0),   // blue
        (255 / 255.0, 77 / 255.0, 224 / 255.0),   // purple
        (255 / 255.0, 53 / 255.0, 53 / 255.0),    // red
        (102 / 255.0, 255 / 255.0, 113 / 255.0)   // green
    ]
    static let botNames = ["CptBligh", "Patches", "MastahP", "CptHook", "LJSilver", "Mutiny",
                           "ParrotBob", "SquareJaw", "Loot", "Digger", "BountyTom", "OneEye"]
    static let deathVerbs = ["Beats", "Clobbers", "Whacks", "Wastes", "Mangles", "Trounces", "Crushes", "Destroys"]

    static func deg2rad(_ d: Float) -> Float { d * .pi / 180 }
    static func rad2deg(_ r: Float) -> Float { r * 180 / .pi }
}

extension SCNVector3 {
    init(_ v: SIMD3<Float>) { self.init(CGFloat(v.x), CGFloat(v.y), CGFloat(v.z)) }
}

func dist2D(_ a: SIMD3<Float>, _ b: SIMD3<Float>) -> Float {
    let dx = a.x - b.x, dz = a.z - b.z
    return sqrt(dx * dx + dz * dz)
}

// MARK: - Weapons (SPEC §6)

enum WeaponType: Int, CaseIterable {
    case cannonball = 0, molehill, crater, tower, supercrater, xshot, bouncer, dumbfire, spikeroller, tnt, teleport, targetTeleport

    var displayName: String {
        switch self {
        case .cannonball: return "Cannonballs"
        case .molehill: return "Molehill"
        case .crater: return "Crater"
        case .tower: return "Tower"
        case .supercrater: return "Supercrater"
        case .xshot: return "X-Shot"
        case .bouncer: return "Bouncer"
        case .dumbfire: return "Dumbfire"
        case .spikeroller: return "SpikeRoller"
        case .tnt: return "TNT"
        case .teleport: return "Teleport"
        case .targetTeleport: return "TargetTeleport"
        }
    }
    var cost: Int {
        switch self {
        case .cannonball: return 0
        case .molehill, .crater: return 20
        case .tower, .supercrater: return 200
        case .xshot, .bouncer: return 300
        case .dumbfire, .spikeroller: return 400
        case .tnt, .teleport: return 500
        case .targetTeleport: return 800
        }
    }
    var offensive: Bool {
        switch self {
        case .molehill, .tower, .teleport, .targetTeleport: return false
        default: return true
        }
    }
    var isProjectile: Bool {
        switch self {
        case .tower, .teleport: return false
        default: return true
        }
    }
    /// Can kill a cannon by direct hit (Global.PROJECTILEIMPACT: indices 0,5,6,7,8,9).
    var impactKill: Bool {
        switch self {
        case .cannonball, .xshot, .bouncer, .dumbfire, .spikeroller, .tnt: return true
        default: return false   // crater/supercrater/molehill deform terrain, don't direct-kill
        }
    }
}

// MARK: - Game options

struct PlayerConfig {
    var name: String
    var colorIndex: Int
    var botType: Int    // 0 human, 1 dummy, 2 aggressive, 3 thinker, 4 crazy
}

struct GameOptions {
    var mapIndex: Int = 0
    var players: [PlayerConfig] = []
    var startingCashIndex: Int = 4    // 1000
    var maxRespawns: Int = 2
    var hotSeatIndex: Int = 0         // NA
    var treasureRespawn: Bool = true
}

enum DeathType {
    case killed(by: Int)      // killer index
    case drowned
    case detonated(by: Int)
    case forfeit
}
