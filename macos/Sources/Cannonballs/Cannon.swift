import Foundation
import SceneKit

/// One player entity: cannon on the island. Owns aiming state, power bar, cash, lives.
final class Cannon {
    let index: Int
    let name: String
    let colorIndex: Int
    let botType: Int              // 0 = human
    var isBot: Bool { botType > 0 }

    weak var game: GameController?

    // world state
    var position = SIMD3<Float>(0, 6, 0)
    var active = true             // still in the game
    var dying = false             // death animation / waiting respawn
    var respawnsUsed = 0
    var respawnTimer: Float = 0

    // economy / stats
    var cash = 0
    var kills = 0, misses = 0, deaths = 0, drownings = 0, goldSpent = 0

    // aiming (SPEC §3)
    var spinAngle: Float = 0          // azimuth degrees
    var currentSpinTarget: Float = 0  // -1..1 held-key ramp
    var currentTiltTarget: Float = 0  // -1..1
    var tiltAcceleration: Float = 0
    var activeTilt: Float = 0         // eased barrel angle; -60(up)..+30(down)
    var lastTiltMarker: Float = -1000   // -1000 sentinel = no shot yet, marker hidden (Cannon.java:44)
    var tiltAngle: Float = 0            // un-eased tilt target (Cannon.java TiltAngle); activeTilt chases it

    // power bar
    var powerBarActive = false
    var powerAscending = true
    var powerLevel: Float = 0
    var lastPowerLevel: Float = -1000   // -1000 sentinel = no shot yet, marker hidden (Cannon.java:53)

    var weaponIndex = 0
    var weapon: WeaponType { WeaponType(rawValue: weaponIndex)! }

    // turn bookkeeping
    var firedOnTurn = false           // a live shot that should pass the turn when it dies
    var hasFiredThisTurn = false      // offensive shot already spent this turn

    // input flags (only wired for human-controlled)
    var keyLeft = false, keyRight = false, keyUp = false, keyDown = false
    private var wasSpinning = false, wasTilting = false

    // bot AI state (BotAI.swift)
    var bot = BotState()

    // scene
    let node = SCNNode()
    let barrelPivot = SCNNode()
    private let barrelNode: SCNNode
    private let nameTag: SCNNode
    // skinned barrel actor + decoded original "fire" recoil motion (squash/recoil)
    private var barrelActor: SkinnedModel.Actor?
    private static let fireMotion = SkinnedModel.loadMotion("MODELS/CANNON/fire_motion.json")

    // Fit the decoded CANNON model to the game's cannon rig (tuned by rendering).
    static let cannonScale: Float = 0.85
    static let cannonYOffset: Float = -2.2

    /// The 2002 engine lit models with flat Gouraud + heavy ambient, so the decoded
    /// textures carry their shading baked in. Render the cannon unlit (constant) with
    /// nearest filtering: no modern smooth-plastic gloss, exactly the original look.
    static func styleCannonMaterials(_ root: SCNNode) {
        root.enumerateHierarchy { n, _ in
            guard let geo = n.geometry else { return }
            for m in geo.materials {
                m.lightingModel = .constant
                m.diffuse.magnificationFilter = .nearest
                m.ambient.contents = NSColor.black
                m.specular.contents = NSColor.black
                m.emission.contents = NSColor.black
                m.transparency = 1
                m.isDoubleSided = false
                m.writesToDepthBuffer = true
                m.readsFromDepthBuffer = true
            }
        }
    }

    /// The original barrel texture (IMAGES/CANNON): gray body + gold trim. The
    /// engine tints it by player color (CannonTex.tint) — the low-saturation gray
    /// takes the color, the saturated gold trim keeps its shine.
    private static var skinCache: [Int: NSImage] = [:]
    static func tintedSkin(_ colorIndex: Int) -> NSImage? {
        if let c = skinCache[colorIndex] { return c }
        guard let base = Assets.image("MODELS/CANNON/textures/cannonskin.png"),
              let cg = base.cgImage(forProposedRect: nil, context: nil, hints: nil) else { return nil }
        let w = cg.width, h = cg.height
        let cs = CGColorSpaceCreateDeviceRGB()
        guard let ctx = CGContext(data: nil, width: w, height: h, bitsPerComponent: 8,
                                  bytesPerRow: w * 4, space: cs,
                                  bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue),
              let buf = { ctx.draw(cg, in: CGRect(x: 0, y: 0, width: w, height: h)); return ctx.data }() else { return nil }
        let px = buf.bindMemory(to: UInt8.self, capacity: w * h * 4)
        let rgb = G.colorRGB[colorIndex % 4]
        let (tr, tg, tb) = (rgb.0, rgb.1, rgb.2)   // colorRGB is already 0-1
        for i in stride(from: 0, to: w * h * 4, by: 4) {
            let r = Float(px[i]), g = Float(px[i + 1]), b = Float(px[i + 2])
            let mx = max(r, g, b), mn = min(r, g, b)
            let sat = mx > 0 ? (mx - mn) / mx : 0
            guard sat < 0.35 else { continue }          // gold trim keeps its shine
            // straight modulate: body = texel * player color. Fitted to the
            // references (red barrel (73,26,26) in cannonballs.jpg, blue
            // (11,52,80) in video frame_007 — plain multiply predicts (78,16,16)
            // and (12,50,78)).
            let luma = (r + g + b) / 3
            px[i]     = UInt8(min(255, tr * luma))
            px[i + 1] = UInt8(min(255, tg * luma))
            px[i + 2] = UInt8(min(255, tb * luma))
        }
        guard let outCG = ctx.makeImage() else { return nil }
        let out = NSImage(cgImage: outCG, size: base.size)
        skinCache[colorIndex] = out
        return out
    }

    /// Assemble the cannon from the four decoded parts (native scale, shared
    /// origin at the barrel pivot; the stone column extends into the terrain).
    /// Returns (staticParts, barrelActor).
    static func buildParts(colorIndex: Int) -> (SCNNode, SkinnedModel.Actor?) {
        let statics = SCNNode()
        for file in ["stand_skinned.json", "platform_skinned.json", "stone_skinned.json"] {
            guard let part = SkinnedModel.load("CANNON", file: file) else { continue }
            styleCannonMaterials(part.root)
            part.root.name = file.replacingOccurrences(of: "_skinned.json", with: "")
            statics.addChildNode(part.root)
        }
        applyReflection(statics)
        let barrel = SkinnedModel.load("CANNON", file: "barrel_skinned.json")
        if let barrel {
            styleCannonMaterials(barrel.root)
            if let skin = tintedSkin(colorIndex) {
                barrel.root.enumerateHierarchy { n, _ in
                    n.geometry?.firstMaterial?.diffuse.contents = skin
                }
            }
            // the barrel carries the strong additive REFLECTION layer
            barrel.root.enumerateHierarchy { n, _ in
                guard let m = n.geometry?.firstMaterial else { return }
                m.reflective.contents = FXSprites.image("REFLECTION")
                m.reflective.intensity = 0.45
            }
        }
        return (statics, barrel)
    }

    /// The original cannon shader carried a REFLECTION environment layer; apply
    /// it as a subtle spherical reflective map so the metal keeps its 2002 sheen.
    static func applyReflection(_ root: SCNNode) {
        guard let img = FXSprites.image("REFLECTION") else { return }
        root.enumerateHierarchy { n, _ in
            guard let geo = n.geometry else { return }
            for m in geo.materials {
                m.reflective.contents = img
                m.reflective.intensity = 0.18
            }
        }
    }

    init(index: Int, config: PlayerConfig, game: GameController) {
        self.index = index
        self.name = config.name
        self.colorIndex = config.colorIndex
        self.botType = config.botType
        self.game = game
        self.cash = G.startingCashTable[game.options.startingCashIndex]

        let rgb = G.colorRGB[colorIndex % 4]
        let tint = NSColor(rgb: rgb)

        barrelPivot.position = SCNVector3(0, -0.5, 0)
        node.addChildNode(barrelPivot)
        let blob = FXSprites.blobShadow(radius: 7)   // original SHADOW patch under the cannon
        blob.position.y = -2.0
        node.addChildNode(blob)
        var builtBarrel: SCNNode? = nil

        // The four decoded cannon parts at NATIVE scale (the engine loads the
        // actors unscaled): stand + platform + buried stone under the node,
        // the player-tinted 10-bone barrel on the tilt pivot (fire recoil).
        let (statics, barrel) = Cannon.buildParts(colorIndex: colorIndex)
        // shared part origin = barrel pivot, ~4.3 above the platform bottom;
        // node origin sits 6 above ground (toGround), so parts go at -1.7
        statics.position = SCNVector3(0, -1.7, 0)
        statics.eulerAngles.y = .pi                    // face +Z (fire direction)
        node.addChildNode(statics)
        if let barrel {
            barrel.root.position = SCNVector3(0, -1.2, 0)
            barrel.root.eulerAngles.y = .pi
            barrelPivot.addChildNode(barrel.root)
            barrelActor = barrel
            builtBarrel = barrel.root
        }

        // Fallback: procedural cannon if the model failed to load.
        if builtBarrel == nil {
            let carriage = SCNNode(geometry: SCNBox(width: 7, height: 3, length: 9, chamferRadius: 0.5))
            carriage.geometry?.materials = [PropGeometry.material(NSColor(calibratedRed: 0.35, green: 0.22, blue: 0.1, alpha: 1))]
            carriage.position = SCNVector3(0, -3.5, 0)
            node.addChildNode(carriage)
            let wheelMat = PropGeometry.material(NSColor(calibratedRed: 0.25, green: 0.16, blue: 0.08, alpha: 1))
            for (dx, dz) in [(-3.8, -2.5), (3.8, -2.5), (-3.8, 2.5), (3.8, 2.5)] {
                let wheel = SCNNode(geometry: SCNTorus(ringRadius: 1.7, pipeRadius: 0.55))
                wheel.geometry?.materials = [wheelMat]
                wheel.eulerAngles.z = .pi / 2
                wheel.position = SCNVector3(CGFloat(dx), -4.2, CGFloat(dz))
                node.addChildNode(wheel)
            }
            let barrel = SCNNode(geometry: SCNCylinder(radius: 2.1, height: 9.5))
            barrel.geometry?.materials = [PropGeometry.material(tint.blended(withFraction: 0.35, of: .black) ?? tint)]
            barrel.eulerAngles.x = .pi / 2
            barrel.position = SCNVector3(0, 0, 2.2)
            barrelPivot.addChildNode(barrel)
            let muzzleRing = SCNNode(geometry: SCNTorus(ringRadius: 2.1, pipeRadius: 0.45))
            muzzleRing.geometry?.materials = [PropGeometry.material(NSColor(calibratedRed: 0.85, green: 0.7, blue: 0.2, alpha: 1))]
            muzzleRing.eulerAngles.x = .pi / 2
            muzzleRing.position = SCNVector3(0, 0, 6.6)
            barrelPivot.addChildNode(muzzleRing)
            builtBarrel = barrel
        }
        barrelNode = builtBarrel!

        // Name labels are drawn by the HUD (screen-space targetbar tags projected
        // from the cannon position, like the original). No 3D banner in the scene.
        _ = tint
        nameTag = SCNNode()

        node.name = "cannon-\(index)"
    }

    // MARK: - Aim math

    /// Fire direction unit vector from spin + tilt (elevation = -activeTilt).
    var fireDirection: SIMD3<Float> {
        let elev = G.deg2rad(-activeTilt)
        let yaw = G.deg2rad(spinAngle)
        let h = cos(elev)
        return SIMD3(sin(yaw) * h, sin(elev), cos(yaw) * h)
    }

    var muzzlePosition: SIMD3<Float> {
        position + fireDirection * 5.0
    }

    /// Barrel elevation in degrees for HUD (-30 down … +60 up)
    var elevationDeg: Float { -activeTilt }

    // MARK: - Input (SPEC §3)

    func spinInput(dt: Float) {
        // +spin is a CCW (leftward) turn on screen, so left arrow ramps positive
        if keyLeft { currentSpinTarget = min(1, currentSpinTarget + 1.3 * dt) }
        if keyRight { currentSpinTarget = max(-1, currentSpinTarget - 1.3 * dt) }
        if !keyLeft && !keyRight {
            currentSpinTarget *= pow(0.001, dt)   // fast damp
            if abs(currentSpinTarget) < 0.001 { currentSpinTarget = 0 }
        }
        spinAngle += currentSpinTarget * G.maxSpinSpeed * dt
        spinAngle = spinAngle.truncatingRemainder(dividingBy: 360)

        // aiming audio (human only): spin servo loop + stop clank; tilt servo loop
        if !isBot {
            let spinning = keyLeft || keyRight
            if spinning { Audio.shared.startLoop("turn_loop", volume: 0.4) }
            else if wasSpinning { Audio.shared.stopLoop("turn_loop"); Audio.shared.play("turn_stop", volume: 0.5) }
            wasSpinning = spinning
            let tilting = keyUp || keyDown
            if tilting { Audio.shared.startLoop("tilt", volume: 0.4) }
            else if wasTilting { Audio.shared.stopLoop("tilt") }
            wasTilting = tilting
        }

        // tilt: up key drives target negative (barrel up)
        if keyUp { tiltAcceleration = max(-3, tiltAcceleration - 3 * dt) }
        if keyDown { tiltAcceleration = min(3, tiltAcceleration + 3 * dt) }
        if !keyUp && !keyDown {
            // Java damps the acceleration, it doesn't zero it (dampAngles,
            // Cannon.java:635-637)
            tiltAcceleration *= pow(0.001, dt)
        }
        currentTiltTarget += tiltAcceleration * dt
        // hitting either end of the ramp kills the acceleration (Cannon.java:622-631)
        if currentTiltTarget > 1 { currentTiltTarget = 1; tiltAcceleration = 0 }
        if currentTiltTarget < -1 { currentTiltTarget = -1; tiltAcceleration = 0 }
        tiltAngle = currentTiltTarget > 0 ? G.minTiltAngle * currentTiltTarget : G.maxTiltAngle * currentTiltTarget   // Cannon.java:406
        activeTilt += (tiltAngle - activeTilt) / 5         // per-call ease (Cannon.java:407-408)
    }

    /// SPACE handler. Returns true if the press was consumed.
    func triggerFire() {
        guard let game, active, !dying else { return }
        let w = weapon
        // off-turn: only defensive weapons
        if game.currentPlayerIndex != index && w.offensive {
            game.hud.flashMessage("Only Defensive Items Can Be Used On Your Off Turn!")
            Audio.shared.play("time_up")   // Sound_TimeUp (Cannon.java:213-217)
            return
        }
        if game.currentPlayerIndex == index && hasFiredThisTurn { return }
        if cash < w.cost {
            game.hud.flashMessage("Not Enough Gold For That Weapon!")
            Audio.shared.play("time_up")
            return
        }
        if !w.isProjectile {
            fireInstant(w)
            return
        }
        if !powerBarActive {
            powerBarActive = true
            powerAscending = true
            powerLevel = 0
        } else {
            fire(at: powerLevel)
        }
    }

    func updatePowerBar(dt: Float) {
        guard powerBarActive else { return }
        let step = min(dt / 2, 0.01)
        if powerAscending {
            powerLevel += step
            if powerLevel >= 1 { powerLevel = 1; powerAscending = false }
        } else {
            powerLevel -= step
            if powerLevel <= 0 { powerLevel = 0; fire(at: 0) }   // auto-fire at 0
        }
    }

    // MARK: - Firing

    func fire(at power: Float) {
        guard let game else { return }
        powerBarActive = false
        let w = weapon
        guard cash >= w.cost else { return }
        cash -= w.cost
        goldSpent += w.cost
        lastPowerLevel = power
        lastTiltMarker = tiltAngle          // Java stores TiltAngle, not elevation (Cannon.java:602)

        let dir = fireDirection
        let mp = muzzlePosition
        let projectile = Projectile(type: w, owner: self, game: game,
                                    position: mp,
                                    velocity: dir * ((power + 0.5) * 100))
        game.launch(projectile, from: self)
        if game.currentPlayerIndex == index { hasFiredThisTurn = true; firedOnTurn = true }
        // Launch audio per source (Weapon.java fire:1185-1234): dumbfire =
        // missile launch + hum; others = cannon blast; the cannonball family
        // AND TNT (case 9) whistle in flight; cases 5/6/8/11 add HUM.
        if w == .dumbfire {
            Audio.shared.play("launch_missile")
            Audio.shared.play("hum", volume: 0.6)
        } else {
            Audio.shared.play("cannon_fire")
            switch w {
            case .cannonball, .molehill, .crater, .supercrater, .tnt:
                Audio.shared.play("whistle", volume: 0.7)
            case .xshot, .bouncer, .spikeroller, .targetTeleport:
                Audio.shared.play("hum", volume: 0.6)
            default: break
            }
        }
        // Muzzle smoke: 20 puffs shot out along the fire direction, verbatim
        // from Cannon.java:1605-1609 — traj = dir*(2+8r) + jitter ±1.5/axis,
        // scale 0.2+r.
        for _ in 0..<20 {
            let f = 2 + Float.random(in: 0..<1) * 8
            FXSprites.smoke(at: mp,
                            trajectory: SIMD3(dir.x * f + Float.random(in: -1.5...1.5),
                                              dir.y * f + Float.random(in: -1.5...1.5),
                                              dir.z * f + Float.random(in: -1.5...1.5)),
                            scale: 0.2 + Float.random(in: 0..<1), in: game.world)
        }
        // Original barrel recoil: the decoded CANNON "fire" motion (0.73s squash).
        if let actor = barrelActor, let motion = Cannon.fireMotion {
            SkinnedModel.playOnce(actor, motion: motion)
        }
    }

    private func fireInstant(_ w: WeaponType) {
        guard let game else { return }
        switch w {
        case .tower:
            if game.world.objectAbove(x: position.x, z: position.z) {
                game.hud.flashMessage("Can't Use Tower On Object!")
                Audio.shared.play("time_up")   // Sound_TimeUp (Cannon.java:226-229)
                return
            }
            cash -= w.cost; goldSpent += w.cost
            // doTower (Weapon.java:484-495): molehill(40, 30) with NO texture
            // splat (Island.molehill bl=false). Island.molehill itself quakes
            // + 30 rays (Island.java:623-631).
            game.world.terrain.molehill(x: position.x, z: position.z, height: 40, radius: 30)
            Projectile.quakeRaysFX(x: position.x, z: position.z, radius: 30, in: game.world)
            game.instantWeaponUsed(by: self)
        case .teleport:
            cash -= w.cost; goldSpent += w.cost
            playerTeleport()
            game.instantWeaponUsed(by: self)
        default:
            break
        }
    }

    // MARK: - Teleport / placement (SPEC §6 place rule)

    func playerTeleport(x: Float? = nil, z: Float? = nil) {
        guard let game else { return }
        Particles.teleport(at: position, in: game.world)
        Audio.shared.play("teleport")
        Audio.shared.play("puff", volume: 0.6)   // Sound_Puff on cannon placement
        if let x, let z {
            position = SIMD3(x, game.world.terrain.height(x: x, z: z) + 6, z)
        } else {
            place(minDistFromSelf: 100)
        }
        toGround()
        Particles.teleport(at: position, in: game.world)
    }

    /// Random valid placement (rejection sampling per SPEC).
    func place(minDistFromSelf: Float = 0) {
        guard let game else { return }
        let terrain = game.world.terrain
        let size = terrain.worldSize
        let origin = position
        for _ in 0..<200 {
            let x = Float.random(in: size * 0.06...(size * 0.94))
            let z = Float.random(in: size * 0.06...(size * 0.94))
            if terrain.targetHeight(x: x, z: z) <= 2.0 { continue }
            let p = SIMD3(x, 0, z)
            if minDistFromSelf > 0 && dist2D(p, origin) < minDistFromSelf { continue }
            if game.players.contains(where: { $0 !== self && $0.active && !$0.dying && dist2D($0.position, p) < 100 }) { continue }
            if game.world.props.contains(where: { pr in
                guard pr.alive else { return false }
                let d = dist2D(pr.position, p)
                return pr.spec.destructible ? d < 20 : d < 10 + pr.spec.radius
            }) { continue }
            if game.chests.contains(where: { $0.alive && dist2D($0.position, p) < 10 }) { continue }
            position = SIMD3(x, terrain.height(x: x, z: z) + 6, z)
            return
        }
        // fallback: raise a platform (Cannon.java:355-365 —
        // Island.molehillAbsolute plays the quake + 30 rays itself,
        // Island.java:386-394)
        let x = Float.random(in: size * 0.2...(size * 0.8))
        let z = Float.random(in: size * 0.2...(size * 0.8))
        terrain.molehillAbsolute(x: x, z: z, height: 10, radius: 30)
        Projectile.quakeRaysFX(x: x, z: z, radius: 30, in: game.world)
        position = SIMD3(x, 10 + 6, z)
    }

    /// Keep the cannon glued to the (possibly deforming) terrain; drown check.
    func toGround() {
        guard let game, active, !dying else { return }
        let h = game.world.terrain.height(x: position.x, z: position.z)
        position.y = h + 6
        if position.y <= 6.0 {   // ground at/below sea level
            game.kill(self, how: .drowned)
        }
    }

    // MARK: - Scene sync

    func syncNode() {
        node.simdPosition = position
        node.simdEulerAngles = SIMD3(0, G.deg2rad(spinAngle), 0)
        barrelPivot.simdEulerAngles = SIMD3(G.deg2rad(-(-activeTilt)), 0, 0)  // rotateX(-elev): tip up for negative activeTilt
        node.isHidden = !active || dying
    }
}
