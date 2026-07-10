import Foundation
import SceneKit

/// A live weapon in flight (SPEC §2 integration, §6 behaviors).
final class Projectile {
    let type: WeaponType
    unowned let owner: Cannon
    weak var game: GameController?

    var position: SIMD3<Float>
    var lastPosition: SIMD3<Float>
    var traj: SIMD3<Float>
    var timeAlive: Float = 0
    private var shadowBlob: SCNNode?
    var alive = true
    var rolling = false            // SpikeRoller ground mode
    let node: SCNNode
    var hitSomething = false       // for miss stat
    private var tumbleAxis: SIMD3<Float>?   // Bouncer setConstantRotation axis (Weapon.java:1292/860)
    private var grinding = false            // SpikeRoller Sound_GrindLoop state (Weapon.java:250-263)

    /// Java increments StatMiss only in the cannonball / xshot / bouncer /
    /// dumbfire / spikeroller branches (Weapon.java:83-85, 103-105, 221-224,
    /// 526-528, 692-694, 707-710, 836-839, 870-873); molehill, crater,
    /// supercrater, TNT and targetTeleport never count a miss.
    private var countsMiss: Bool {
        switch type {
        case .cannonball, .xshot, .bouncer, .dumbfire, .spikeroller: return true
        default: return false
        }
    }

    init(type: WeaponType, owner: Cannon, game: GameController, position: SIMD3<Float>, velocity: SIMD3<Float>) {
        self.type = type
        self.owner = owner
        self.game = game
        self.position = position
        self.lastPosition = position
        self.traj = velocity
        node = Projectile.makeNode(type: type, ownerColor: owner.colorIndex)
        node.simdPosition = position
        if type == .bouncer {
            // the bouncer tumbles from launch: setConstantRotation(random axis,
            // 300 deg/s) — Weapon.java:1292
            tumbleAxis = simd_normalize(SIMD3(Float.random(in: -0.5...0.5),
                                              Float.random(in: -0.5...0.5),
                                              Float.random(in: -0.5...0.5)))
        }
    }

    /// Load a decoded projectile model, centered and scaled to a target diameter.
    private static func decodedModel(_ name: String, span target: Float) -> SCNNode? {
        // prefer the solved-pipeline export (correct UVs/materials)
        if let actor = SkinnedModel.load(name) {
            let inner = SCNNode()
            inner.addChildNode(actor.root)
            let (minB, maxB) = inner.boundingBox
            let span0 = Float(max(maxB.x - minB.x, max(maxB.y - minB.y, maxB.z - minB.z)))
            let holder = SCNNode()
            holder.addChildNode(inner)
            if span0 > 0.001 { holder.simdScale = SIMD3(repeating: target / span0) }
            return holder
        }
        guard let model = ModelLibrary.node(for: name) else { return nil }
        Cannon.styleCannonMaterials(model)     // unlit + opaque, like the original
        let holder = SCNNode()
        holder.addChildNode(model)
        let (lo, hi) = model.boundingBox
        let cx = Float(lo.x + hi.x) / 2, cy = Float(lo.y + hi.y) / 2, cz = Float(lo.z + hi.z) / 2
        let s0 = model.simdScale.x
        model.simdPosition = SIMD3(-cx * s0, -cy * s0, -cz * s0)
        let span = simd_length(SIMD3(Float(hi.x - lo.x), Float(hi.y - lo.y), Float(hi.z - lo.z))) * s0
        if span > 0.001 { holder.simdScale = SIMD3(repeating: target / span) }
        return holder
    }

    /// Cannonball-family shots are 2x2 BILLBOARD SPRITES off the WEAPONS sheet
    /// (Weapon.java fireProjectile: cannonball rect (0,0)-(.5,.5) black ball,
    /// molehill (.5,0)-(1,.5) blue, crater+supercrater (0,.5)-(.5,1) purple).
    /// The MORTAR shell model is only used by X-Shot (5) and Dumbfire (7).
    private static func weaponSprite(col: Int, row: Int) -> SCNNode {
        let plane = SCNPlane(width: 4, height: 4)   // setBitmapSize 2,2 at half-res -> ~4 world units
        let m = SCNMaterial()
        m.diffuse.contents = FXSprites.image("WEAPONS")
        m.diffuse.contentsTransform = SCNMatrix4Mult(
            SCNMatrix4MakeScale(0.5, 0.5, 1),
            SCNMatrix4MakeTranslation(CGFloat(col) * 0.5, CGFloat(row) * 0.5, 0))
        m.lightingModel = .constant
        m.transparencyMode = .aOne
        m.isDoubleSided = true
        plane.materials = [m]
        let n = SCNNode(geometry: plane)
        n.constraints = [SCNBillboardConstraint()]
        n.castsShadow = false
        return n
    }

    static func makeNode(type: WeaponType, ownerColor: Int) -> SCNNode {
        // The original's projectile visuals (Weapon.java fireProjectile).
        let real: SCNNode?
        switch type {
        case .cannonball:
            real = weaponSprite(col: 0, row: 0)           // black cannonball
        case .molehill:
            real = weaponSprite(col: 1, row: 0)           // blue ball
        case .crater, .supercrater:
            real = weaponSprite(col: 0, row: 1)           // purple ball
        case .xshot:
            real = decodedModel("MORTAR", span: 4.2)      // the finned mortar shell
        case .bouncer:
            real = decodedModel("BOUNCEBALL", span: 4.4)
        case .spikeroller:
            real = decodedModel("SPIKEBALL", span: 5.2)
        case .tnt:
            real = decodedModel("TNT", span: 6.0)
        case .dumbfire:
            real = decodedModel("MORTAR", span: 4.6)
        default:
            real = nil
        }
        if let real {
            real.name = "projectile"
            return real
        }
        let n: SCNNode
        switch type {
        case .molehill:
            n = SCNNode(geometry: SCNSphere(radius: 1.6))
            n.geometry?.materials = [PropGeometry.material(NSColor(calibratedRed: 0.2, green: 0.65, blue: 0.25, alpha: 1))]
        case .crater, .supercrater:
            let r: CGFloat = type == .supercrater ? 2.4 : 1.7
            n = SCNNode(geometry: SCNSphere(radius: r))
            n.geometry?.materials = [PropGeometry.material(NSColor(calibratedRed: 0.5, green: 0.32, blue: 0.5, alpha: 1))]
        case .xshot:
            n = SCNNode(geometry: SCNSphere(radius: 1.8))
            n.geometry?.materials = [PropGeometry.material(.orange, emissive: .orange)]
        case .bouncer:
            n = SCNNode(geometry: SCNSphere(radius: 2.0))
            n.geometry?.materials = [PropGeometry.material(NSColor(calibratedWhite: 0.75, alpha: 1))]
        case .dumbfire:
            n = SCNNode()
            let body = SCNNode(geometry: SCNCapsule(capRadius: 0.9, height: 4.5))
            body.geometry?.materials = [PropGeometry.material(NSColor(calibratedWhite: 0.8, alpha: 1))]
            body.eulerAngles.x = .pi / 2
            n.addChildNode(body)
            let exhaust = SCNNode(geometry: SCNSphere(radius: 0.7))
            exhaust.geometry?.materials = [PropGeometry.material(.orange, emissive: .orange)]
            exhaust.position.z = -2.6
            n.addChildNode(exhaust)
        case .spikeroller:
            n = SCNNode()
            let ball = SCNNode(geometry: SCNSphere(radius: 1.9))
            ball.geometry?.materials = [PropGeometry.material(NSColor(calibratedWhite: 0.35, alpha: 1))]
            n.addChildNode(ball)
            for i in 0..<8 {
                let spike = SCNNode(geometry: SCNCone(topRadius: 0, bottomRadius: 0.4, height: 1.6))
                spike.geometry?.materials = [PropGeometry.material(NSColor(calibratedWhite: 0.3, alpha: 1))]
                let a = Float(i) / 8 * 2 * .pi
                spike.simdPosition = SIMD3(cos(a) * 2.2, sin(a) * 2.2, 0)
                spike.simdEulerAngles = SIMD3(0, 0, a - .pi / 2)
                n.addChildNode(spike)
            }
        case .tnt:
            n = PropGeometry.tntBarrel(dims: PropDims.dims("TNT"), height: 4.5, radius: 2.5)
        case .targetTeleport:
            n = SCNNode(geometry: SCNSphere(radius: 1.4))
            n.geometry?.materials = [PropGeometry.material(.cyan, emissive: .cyan)]
        default:
            n = SCNNode(geometry: SCNSphere(radius: 1.6))
            n.geometry?.materials = [PropGeometry.material(NSColor(calibratedWhite: 0.12, alpha: 1))]
        }
        n.name = "projectile"
        return n
    }

    // MARK: - Per-substep update

    func update(dt: Float) {
        guard alive, let game else { return }
        timeAlive += dt
        lastPosition = position
        let terrain = game.world.terrain
        let wind = game.wind

        // ---- integration + trails per type ----
        switch type {
        case .dumbfire:
            // no gravity, wind only; 7 s life (Weapon.java:669-675)
            position += traj * dt
            traj.x += wind.x * dt
            traj.z += wind.z * dt
            missileTrail()
        case .spikeroller:
            position += traj * dt
            if rolling {
                // grounded roller: 20% wind, gravity flattened (Weapon.java:243-248)
                traj.x += wind.x * dt * 0.2
                traj.z += wind.z * dt * 0.2
                traj.y = 0
            } else {
                traj.x += wind.x * dt
                traj.y += G.gravity * dt
                traj.z += wind.z * dt
            }
        default:
            position += traj * dt
            traj.x += wind.x * dt
            traj.y += G.gravity * dt
            traj.z += wind.z * dt
            if type == .xshot {
                // X-Shot sputters the same smoke + mini-fireball trail as
                // Dumbfire while flying ballistic (Weapon.java updateXShot:70-79)
                missileTrail()
            } else if type == .targetTeleport {
                // 40% sparkle trail per frame (Weapon.java:311-313)
                if Float.random(in: 0..<1) < 0.4 {
                    FXSprites.sparkle(at: position,
                                      velocity: SIMD3(Float.random(in: -1...1),
                                                      Float.random(in: 0..<1) * 2,
                                                      Float.random(in: -1...1)),
                                      in: game.world)
                }
            }
        }

        node.simdPosition = position
        // SHADOW blob tracks the ground under the shot (original per-projectile
        // patch, Weapon.java:766-781 updateShadow — runs for EVERY type,
        // dumbfire included)
        if shadowBlob == nil {
            let b = FXSprites.blobShadow(radius: 2)
            game.world.effectsRoot.addChildNode(b)
            shadowBlob = b
        }
        if let b = shadowBlob {
            let gh = terrain.height(x: position.x, z: position.z)
            if gh > 0 && position.y > gh {
                b.isHidden = false
                b.simdPosition = SIMD3(position.x, gh + 0.2, position.z)
            } else { b.isHidden = true }
        }
        // mortar-shell weapons AND the TNT barrel fly nose-first (Weapon.java
        // setOrientationVector from the normalized trajectory, updateXShot:68 /
        // updateDumbfire:678 / updateTNT:923-925)
        if type == .dumbfire || type == .xshot || type == .tnt, simd_length(traj) > 0.01 {
            node.simdLook(at: position + traj)
        }
        if type == .spikeroller {
            node.simdEulerAngles.x += simd_length(SIMD3(traj.x, 0, traj.z)) * dt / 2.0
        }
        if let axis = tumbleAxis {
            // setConstantRotation 300 deg/s (Weapon.java:1292/860)
            node.simdOrientation = simd_quatf(angle: G.deg2rad(300) * dt, axis: axis) * node.simdOrientation
        }

        // ---- hit tests (Weapon.java:1026 checkForHit) ----
        let n = checkForHit()
        let ground = terrain.height(x: position.x, z: position.z)

        // ---- per-weapon resolution ladder, transcribed branch-for-branch ----
        switch type {
        case .cannonball:
            // updateCannonBall (Weapon.java:497-557); no n==1..3 branch — the
            // kill/collect happened inside checkForHit, the shot just hides
            if position.y <= 0 && ground <= 0 {
                splash()
            } else if position.y <= ground {
                Audio.shared.play("explosion2")   // Weapon.java:529
                terrain.crater(x: position.x, z: position.z, depth: 4, radius: 20)   // Weapon.java:531 (scorch bake)
                Projectile.craterBurstFX(x: position.x, z: position.z, in: game.world)  // Island.crater bl=true
                game.camera.shock(at: position, radius: 70)
                die()
            } else if n >= 4 {
                propBurst()
            }

        case .molehill:
            // updateMoleHill (Weapon.java:559-613)
            if position.y <= 0 && ground <= 0 {
                splashGentle()   // Weapon.java:569-582 — the gentle jet params
            } else if position.y <= ground || n == 1 {
                // splat + 20 CHUNKS frame 0 at ground+2 on every landing AND on
                // a cannon proximity hit (Weapon.java:583-588)
                Audio.shared.play("splat")
                splatChunks(frame: 0, y: ground + 2, sizeBase: 0.5, sizeRand: 2)
                die()
                // only the terrain raise is suppressed, and only by a STANDABLE
                // deck overhead (the mask-8 probe at Weapon.java:590-591, drop 22)
                if game.world.standableSurface(x: position.x, y: position.y,
                                               z: position.z, drop: 22) == nil {
                    terrain.molehill(x: position.x, z: position.z, height: 10, radius: 20,
                                     splat: NSColor(calibratedRed: 32 / 255, green: 40 / 255, blue: 135 / 255, alpha: 0.45))   // Weapon.java:593
                    Projectile.quakeRaysFX(x: position.x, z: position.z, radius: 20, in: game.world)  // Island.molehill:623-631
                }
            } else if n > 0 {
                // codes 2/3/4/5: splat + chunks, shot done (Weapon.java:604-611)
                die()
                Audio.shared.play("splat")
                splatChunks(frame: 0, y: position.y + 2, sizeBase: 0.5, sizeRand: 2)
            }

        case .crater:
            // updateCrater (Weapon.java:615-667)
            if position.y <= 0 && ground <= 0 {
                splashGentle()   // Weapon.java:625-638
            } else if position.y <= ground || n == 1 {
                // 20 chunks frame 1 + splat + the colored crater; a cannon
                // proximity hit (n==1) detonates it too (Weapon.java:639-658)
                splatChunks(frame: 1, y: ground + 2, sizeBase: 0.5, sizeRand: 2)
                Audio.shared.play("splat")
                terrain.crater(x: position.x, z: position.z, depth: 10, radius: 30,
                               splat: NSColor(calibratedRed: 130 / 255, green: 31 / 255, blue: 115 / 255, alpha: 0.45))   // Weapon.java:646
                // quake craters: Sound_Quake + 30 rays (Island.crater bl2, Island.java:504-513)
                Projectile.quakeRaysFX(x: position.x, z: position.z, radius: 30, in: game.world)
                die()
            } else if n > 0 {
                // splat + 20 chunks — and the shot KEEPS FLYING on a standable
                // deck (no hide in that branch, Weapon.java:659-664)
                Audio.shared.play("splat")
                splatChunks(frame: 1, y: position.y + 2, sizeBase: 0.5, sizeRand: 2)
            }

        case .supercrater:
            // updateSuperCrater (Weapon.java:971-1024)
            if position.y <= 0 && ground <= 0 {
                splash()   // Weapon.java:981-995 uses the strong jet params
            } else if position.y <= ground || n == 1 {
                Audio.shared.play("splat")
                splatChunks(frame: 1, y: ground + 2, sizeBase: 0.5, sizeRand: 2)
                terrain.crater(x: position.x, z: position.z, depth: 20, radius: 50,
                               splat: NSColor(calibratedRed: 130 / 255, green: 31 / 255, blue: 115 / 255, alpha: 0.5))    // Weapon.java:1003
                Projectile.quakeRaysFX(x: position.x, z: position.z, radius: 50, in: game.world)
                // NO explosion: Island.crater is called with bl=false (Weapon.java:1003)
                die()
            } else if n > 0 {
                Audio.shared.play("splat")
                splatChunks(frame: 1, y: position.y + 2, sizeBase: 0.5, sizeRand: 2)
            }

        case .xshot:
            // updateXShot (Weapon.java:59-151): the X detonates on landing AND
            // on any direct hit code 1/2/3 (Weapon.java:100)
            if position.y <= 0 && ground <= 0 {
                splash()
            } else if position.y <= ground || n == 1 || n == 2 || n == 3 {
                Audio.shared.play("explosion2")   // Weapon.java:101
                xCrater()
                game.camera.shock(at: position, radius: 70)
                die()
            } else if n >= 4 {
                propBurst()
            }

        case .bouncer:
            updateBouncer(n: n, ground: ground)

        case .dumbfire:
            // updateDumbfire (Weapon.java:669-757)
            if timeAlive > 7 {
                // timeout: miss counted + the n>=4-style burst FX (Weapon.java:691-705)
                burstFX()
                die()
            }
            if position.y <= 0 && ground <= 0 {
                splash()
            } else if position.y <= ground || n == 1 || n == 2 || n == 3 {
                Audio.shared.play("explosion3")   // Weapon.java:729
                terrain.crater(x: position.x, z: position.z, depth: 6, radius: 20)   // Weapon.java:731
                Projectile.craterBurstFX(x: position.x, z: position.z, in: game.world)  // bl=true
                game.camera.shock(at: position, radius: 120)
                die()
            } else if n >= 4 {
                propBurst()
            }

        case .spikeroller:
            updateSpikeRoller(n: n, ground: ground)

        case .tnt:
            // updateTNT (Weapon.java:915-969)
            if n == 0 || n == 5 {
                if n != 5 && position.y <= 0 && ground <= 0 {
                    splash()
                } else if position.y <= ground || n == 5 {
                    // plant the barrel; packet 21 + <ONGROUND>:YES snap it to
                    // the TERRAIN below (Prop.java:543-545)
                    plantTNT(groundHeight: ground)
                    hitSomething = true
                    die()
                }
            } else {
                // ANY hit code 1-4 bursts the barrel (Weapon.java:956-966)
                propBurst()
            }

        case .targetTeleport:
            // updateTargetTeleport (Weapon.java:302-356)
            if position.y <= 0 && ground <= 0 {
                splashGentle()   // Weapon.java:316-329
            } else if n == 5 || (position.y <= ground && n == 0) {
                Audio.shared.play("splat")   // Weapon.java:331
                // 20 CHUNKS frames 2/3 (50/50), size 0.1+r (Weapon.java:333-339)
                splatChunks(frame: 2, y: position.y + 2, sizeBase: 0.1, sizeRand: 1, mixed: true)
                owner.playerTeleport(x: position.x, z: position.z)
                hitSomething = true
                die()
            } else if n > 0 {
                // any other hit: Sound_Puff fizzle, 10 flat smoke puffs (Weapon.java:344-349)
                Audio.shared.play("puff")
                for _ in 0..<10 {
                    FXSprites.smoke(at: position,
                                    trajectory: SIMD3(Float.random(in: -5...5), 0, Float.random(in: -5...5)),
                                    scale: 1.0, in: game.world)
                }
                die()
            }

        default:
            if position.y <= 0 && ground <= 0 {
                splash()
            } else if position.y <= ground {
                explode(terrainHit: true)
            }
        }
    }

    /// The Dumbfire/X-Shot flight trail (Weapon.java:70-79 / 679-688): 30% smoke,
    /// 40% mini-fireball per frame.
    private func missileTrail() {
        guard let game else { return }
        if Float.random(in: 0..<1) < 0.3 {
            FXSprites.smoke(at: position,
                            trajectory: SIMD3(Float.random(in: -0.17...0.17), 0, Float.random(in: -0.17...0.17)),
                            scale: 1.0, in: game.world)
        }
        if Float.random(in: 0..<1) < 0.4 {
            FXSprites.explosion1(at: position, size: 3 + .random(in: 0...2), in: game.world)
        }
    }

    // MARK: - Bouncer / SpikeRoller (their Java ladders keep local floor state)

    /// updateBouncer (Weapon.java:816-904).
    private func updateBouncer(n: Int, ground: Float) {
        guard let game else { return }
        var f2 = ground + 1.5   // Weapon.java:825 — the bounce floor sits at terrain+1.5
        if n == 0 || n == 5 {
            if n == 5 {
                // the mask-8 deck ray is the bounce floor on a standable prop
                // (Weapon.java:827-835)
                if let s = game.world.standableSurface(x: position.x, y: position.y,
                                                       z: position.z, drop: 5) {
                    f2 = s.height
                }
            }
            if n != 5 && position.y <= 2.0 && f2 <= 1.5 {
                // water (Weapon.java:836: Y<=2 with the terrain at/below sea level)
                splash()
            } else if position.y <= f2 {
                Audio.shared.play("bounce")           // Sound_Clang on every contact (Weapon.java:855)
                position.y = f2 + 1.0                 // Weapon.java:856
                if traj.y < 0 {
                    traj.y *= -0.85                   // Weapon.java:858
                    // black smoke + fresh tumble + 4 chunk splats on each
                    // reflection (Weapon.java:859-868)
                    FXSprites.smoke(at: position - SIMD3(0, 2, 0),
                                    trajectory: SIMD3(Float.random(in: -0.17...0.17), 0, Float.random(in: -0.17...0.17)),
                                    scale: 1.0, black: true, in: game.world)
                    tumbleAxis = simd_normalize(SIMD3(Float.random(in: -0.5...0.5),
                                                      Float.random(in: -0.5...0.5),
                                                      Float.random(in: -0.5...0.5)))
                    splatChunks(frame: 2, y: position.y + 2, sizeBase: 0.1, sizeRand: 1, mixed: true, count: 4)
                }
                if traj.y < 0.01 {
                    // detonate once the bounce decays (Weapon.java:870-888)
                    Audio.shared.play("explosion2")
                    game.world.terrain.crater(x: position.x, z: position.z, depth: 4, radius: 20)   // Weapon.java:876
                    Projectile.craterBurstFX(x: position.x, z: position.z, in: game.world)  // bl=true
                    game.camera.shock(at: position, radius: 70)
                    die()
                }
                node.simdPosition = position
            }
        } else if n >= 4 {
            propBurst()
        }
        // codes 1/2/3: kill/collect happened inside checkForHit; the Java ladder
        // has no extra branch for them (Weapon.java:826/891)
    }

    /// updateSpikeRoller (Weapon.java:153-278). The +1.9/+2.0 rest offsets are
    /// the clone's model-pivot compensation (macOS-verified); the Java floor
    /// math is terrain+1.0 with a +0.5 grind threshold.
    private func updateSpikeRoller(n: Int, ground: Float) {
        guard let game else { return }
        if n != 0 && n != 5 { stopGrind() }   // Weapon.java:177-180
        if n == 0 || n == 5 {
            let deck = n == 5 ? game.world.standableSurface(x: position.x, y: position.y,
                                                            z: position.z, drop: 5) : nil
            if rolling {
                // Weapon.java:164-174 — on a STANDABLE deck the roller rides the
                // mask-8 ray height instead of the terrain (and doesn't splash
                // over water while the deck holds it up)
                if let deck, deck.height >= ground {
                    position.y = deck.height + 0.1 + 1.9
                    grindSpray()
                } else {
                    if ground <= 0 {
                        // Weapon.java:221-242 water splash (f2 <= 1.0 ⇔ terrain <= 0)
                        splash()
                        return
                    }
                    position.y = ground + 0.1 + 1.9
                    grindSpray()
                }
                if !grinding {
                    grinding = true
                    Audio.shared.startLoop("grind", volume: 0.6)   // Sound_GrindLoop (Weapon.java:250-252)
                }
                if timeAlive > 25 {
                    // clone failsafe: Weapon.java has no roller timeout; without
                    // it an unobstructed roller never passes the turn
                    explode(terrainHit: true)
                    return
                }
            } else {
                // airborne over open water: Y <= 1.8 with the terrain at/below
                // sea level splashes directly (Weapon.java:221)
                if deck == nil && position.y <= 1.8 && ground <= 0 {
                    splash()
                    return
                }
                // a flying roller grinds on once Y reaches the floor: deck+0.5
                // (Weapon.java:164-171 ray + :182 threshold) or terrain contact
                var floorY = ground + 2.1   // clone pivot compensation (see header)
                if let deck { floorY = deck.height + 0.5 }
                if position.y <= floorY {
                    rolling = true
                    traj.y = 0
                    position.y = (deck?.height ?? ground) + 2.0
                    if !grinding {
                        grinding = true
                        Audio.shared.startLoop("grind", volume: 0.6)
                    }
                }
            }
        } else if n >= 4 {
            propBurst()
        }
        // codes 1/2/3: the kill happened inside checkForHit; the Java roller
        // adds nothing but the grind-loop stop (no explosion)
    }

    /// Grinding debris: 2x 20% CHUNKS (frames 2/3) thrown sideways + 30% black
    /// smoke (Weapon.java:204-218). Java offsets along the surface Right vector;
    /// with the clone's flat-ground simplification Right = up × forward.
    /// (Java's Z velocity reuses Right.X — original bug, kept.)
    private func grindSpray() {
        guard let game else { return }
        let flat = SIMD3<Float>(traj.x, 0, traj.z)
        guard simd_length(flat) > 0.001 else { return }
        let fwd = simd_normalize(flat)
        let right = SIMD3<Float>(fwd.z, 0, -fwd.x)
        for _ in 0..<2 where Float.random(in: 0..<1) < 0.2 {
            var f4 = Float.random(in: -1...1)
            f4 = f4 > 0 ? f4 + 1 : f4 - 1
            let frame = Float.random(in: 0..<1) < 0.5 ? 2 : 3
            FXSprites.chunk(frame: frame,
                            at: position + right * f4 - SIMD3(0, 2, 0),
                            velocity: SIMD3(Float.random(in: -2.5...2.5) + right.x * f4 * 10,
                                            10 + Float.random(in: -2.5...2.5) + right.y * f4 * 10,
                                            Float.random(in: -2.5...2.5) + right.x * f4 * 10),
                            size: 0.25 + Float.random(in: 0..<1), in: game.world)
        }
        if Float.random(in: 0..<1) < 0.3 {
            FXSprites.smoke(at: position - SIMD3(0, 2, 0),
                            trajectory: SIMD3(Float.random(in: -0.17...0.17), 0, Float.random(in: -0.17...0.17)),
                            scale: 1.0, black: true, in: game.world)
        }
    }

    private func stopGrind() {
        if grinding {
            grinding = false
            Audio.shared.stopLoop("grind")
        }
    }

    // MARK: - checkForHit (Weapon.java:1026-1095)

    /// Returns the Java hit code: 0 none, 1 cannon, 2 chest, 3 prop,
    /// 4 collideable mesh (shot hidden on the spot), 5 standable mesh (shot NOT
    /// hidden). Kills/collections are gated on Global.PROJECTILEIMPACT
    /// (WeaponType.impactKill) exactly like the Java (Weapon.java:1044/1069/1081).
    private func checkForHit() -> Int {
        guard let game else { return 0 }
        // The WTCOLLIDEABLE mesh sweep runs FIRST and short-circuits every
        // proximity test (Weapon.java:1029-1040): standable props -> code 5,
        // others -> code 4 (shot hidden on the spot).
        if let meshProp = game.world.collideSegment(from: lastPosition, to: position) {
            if meshProp.spec.standable { return 5 }
            hitSomething = true
            die()                      // Weapon.java:1038-1039
            return 4
        }
        // targetTeleport probes with widened radii (Weapon.java:314); everything
        // else uses the kill radii (Weapon.java:80/163/505/...)
        let cannonR: Float = type == .targetTeleport ? 12 : G.killRadiusCannon
        let chestR: Float = type == .targetTeleport ? 10 : G.killRadiusChest
        let propPad: Float = type == .targetTeleport ? 6 : G.propPad
        // cannons (Weapon.java:1042-1065) — 3D distance
        for c in game.players where c !== owner && c.active && !c.dying {
            if simd_distance(c.position, position) < cannonR {
                hitSomething = true
                if type.impactKill {
                    game.kill(c, how: .killed(by: owner.index))
                }
                die()
                return 1
            }
        }
        // chests (Weapon.java:1066-1076)
        for chest in game.chests where chest.alive {
            if simd_distance(chest.position, position) < chestR {
                hitSomething = true
                if type.impactKill {
                    game.collectChest(chest, by: owner)
                }
                die()
                return 2
            }
        }
        // props (Weapon.java:1077-1092)
        if let prop = game.world.propHit(x: position.x, y: position.y, z: position.z,
                                         gameTime: game.gameTime, pad: propPad) {
            hitSomething = true
            die()
            if type.impactKill {
                game.destroyProp(prop, by: owner)
            }
            return 3
        }
        return 0
    }

    // MARK: - Shared FX

    /// The n >= 4 burst-against-a-prop (updateCannonBall:544-554 and its twins
    /// in updateXShot/updateDumbfire/updateBouncer/updateSpikeRoller/updateTNT):
    /// Sound_Explosion1 + one explosion(40) + 4 smoke puffs + 3 fire-trail
    /// embers. No crater, no kill radius, and no miss counted.
    private func propBurst() {
        hitSomething = true
        burstFX()
        die()
    }

    private func burstFX() {
        guard let game else { return }
        Audio.shared.play("explosion1")
        func j() -> Float { Float.random(in: -0.5...0.5) }   // (rand - 0.5)
        FXSprites.explosion1(at: position + SIMD3(j(), 2, j()),
                             trajectory: SIMD3(j() / 3, 1 + j() / 3, j() / 3),
                             size: 40, in: game.world)
        for _ in 0..<4 {
            FXSprites.smoke(at: position + SIMD3(j(), 2, j()),
                            trajectory: SIMD3(j() / 3, 1 + j() / 3, j() / 3),
                            scale: 1.0, in: game.world)
        }
        for _ in 0..<3 {
            FXSprites.fireTrailEmber(at: position + SIMD3(j(), 2, j()),
                                     velocity: SIMD3(j() * 20, 30 + j() * 10, j() * 20),
                                     in: game.world)
        }
    }

    /// The 20-piece chunk splat (updateMoleHill:585-588 / updateCrater:640-643 /
    /// updateTargetTeleport:333-339): CHUNKS pieces sprayed at the impact point.
    /// Landing branches spray at ground+2 (f2+2), proximity branches at Y+2 —
    /// the caller passes `y`. mixed=true picks frames 2/3 per piece 50/50.
    private func splatChunks(frame: Int, y: Float, sizeBase: Float, sizeRand: Float,
                             mixed: Bool = false, count: Int = 20) {
        guard let game else { return }
        func j() -> Float { Float.random(in: -0.5...0.5) }
        for _ in 0..<count {
            let f = mixed ? (Float.random(in: 0..<1) < 0.5 ? 2 : 3) : frame
            FXSprites.chunk(frame: f,
                            at: SIMD3(position.x + j(), y, position.z + j()),
                            velocity: SIMD3(j() * 30, 30 + j() * 10, j() * 30),
                            size: sizeBase + Float.random(in: 0..<1) * sizeRand, in: game.world)
        }
    }

    /// Island.crater's bl=true impact plume (Island.java:559-566): one
    /// explosion(60) + a black 8 s smoke column sunk 4 under the surface +
    /// 7 fire-trail embers. Fired by the scorch craters: cannonball, dumbfire,
    /// bouncer, X-Shot center.
    static func craterBurstFX(x: Float, z: Float, in world: World) {
        func j() -> Float { Float.random(in: -0.5...0.5) }
        let g = world.terrain.height(x: x, z: z)
        FXSprites.explosion1(at: SIMD3(x + j(), g + 2, z + j()),
                             trajectory: SIMD3(j() / 3, 1 + j() / 3, j() / 3),
                             size: 60, in: world)
        // Particle_Object_SmokeColumn(1, x, y-4, z, false, 8): type 1 = black
        // smoke, Fire=false, 8 s (Particle_Object_SmokeColumn.java ctor)
        FXSprites.smokeColumn(at: SIMD3(x, g - 4, z), duration: 8, black: true, in: world)
        for _ in 0..<7 {
            FXSprites.fireTrailEmber(at: SIMD3(x + j(), g + 2, z + j()),
                                     velocity: SIMD3(j() * 20, 30 + j() * 10, j() * 20),
                                     in: world)
        }
    }

    /// Island's quake FX: Sound_Quake + 30 delayed light rays scattered
    /// ±radius*0.75 around the deformation (Island.java:504-513 crater bl2 /
    /// :623-631 molehill / :386-394 molehillAbsolute — the offset is
    /// (rand-0.5)*radius*1.5).
    static func quakeRaysFX(x: Float, z: Float, radius: Float, in world: World) {
        Audio.shared.play("quake")
        func j() -> Float { Float.random(in: -0.5...0.5) }
        for _ in 0..<30 {
            FXSprites.ray(atX: x + j() * radius * 1.5, z: z + j() * radius * 1.5,
                          scale: 0.5 + Float.random(in: 0..<1) * 2, delayed: true, in: world)
        }
    }

    // MARK: - Ground behavior helpers (SPEC §6)

    /// X-Shot (Weapon.xCrater:794-814 + updateXShot:106-124): two ±45° grooves
    /// of length 32 (radius 4, depth 8) with the Island.groove ray+chunk trail,
    /// a central crater(depth 4, radius 20, bl=true), plus two ±45° kill-lines
    /// of length 30, width 10.
    private func xCrater() {
        guard let game else { return }
        let heading = atan2(traj.x, traj.z)
        for offset in [Float.pi / 4, -Float.pi / 4] {
            let a = heading + offset
            let dx = sin(a), dz = cos(a)
            // terrain grooves: ±32
            let x1 = position.x - dx * 32, z1 = position.z - dz * 32
            let x2 = position.x + dx * 32, z2 = position.z + dz * 32
            game.world.terrain.groove(x1: x1, z1: z1, x2: x2, z2: z2, radius: 4, depth: 8)
            grooveFX(x1: x1, z1: z1, x2: x2, z2: z2)
            // kill lines: ±30
            game.checkForHitLine(x1: position.x - dx * 30, z1: position.z - dz * 30,
                                 x2: position.x + dx * 30, z2: position.z + dz * 30, width: 10, killer: owner)
        }
        game.world.terrain.crater(x: position.x, z: position.z, depth: 4, radius: 20)
        Projectile.craterBurstFX(x: position.x, z: position.z, in: game.world)   // bl=true (Weapon.java:813)
    }

    /// Island.groove's trench FX (Island.java:1048-1061): 20 steps along the
    /// line, each with an un-delayed light ray (scale 0.5+2r) and a CHUNKS
    /// frame-2/3 piece (size 0.1+r) thrown off the fresh trench.
    private func grooveFX(x1: Float, z1: Float, x2: Float, z2: Float) {
        guard let game else { return }
        func j() -> Float { Float.random(in: -0.5...0.5) }
        let dx = x2 - x1, dz = z2 - z1
        let len = sqrt(dx * dx + dz * dz)
        guard len > 0.001 else { return }
        let step = len / 20
        let dirx = dx / len, dirz = dz / len
        for i in 0..<20 {
            let px = x1 + dirx * Float(i) * step
            let pz = z1 + dirz * Float(i) * step
            FXSprites.ray(atX: px, z: pz, scale: 0.5 + Float.random(in: 0..<1) * 2,
                          delayed: false, in: game.world)
            let frame = Float.random(in: 0..<1) < 0.5 ? 2 : 3
            FXSprites.chunk(frame: frame,
                            at: SIMD3(px + j(), game.world.terrain.height(x: px, z: pz), pz + j()),
                            velocity: SIMD3(j() * 30, 30 + j() * 10, j() * 30),
                            size: 0.1 + Float.random(in: 0..<1), in: game.world)
        }
    }

    /// Plant the TNT barrel prop on the terrain (updateTNT:944-955 packet 21;
    /// no particles in the Java plant branch).
    private func plantTNT(groundHeight: Float) {
        guard let game else { return }
        let spec = PropSpec.load("TNT")
        let prop = Prop(spec: spec, position: SIMD3(position.x, groundHeight, position.z),
                        rotationDeg: Float.random(in: 0..<360), gameTime: game.gameTime)
        prop.detonator = owner
        game.world.addProp(prop)
    }

    // MARK: - Death

    /// Water impact for the impact-weapon family (Weapon.java:86-98 etc.):
    /// Sound_Splash + 9 strong jets + 3 rings (Particles.splash carries the params).
    func splash() {
        guard let game else { return }
        Audio.shared.play("splash")
        Particles.splash(at: SIMD3(position.x, 0.3, position.z), in: game.world)
        die()
    }

    /// The gentle water impact used by molehill / crater / targetTeleport
    /// (Weapon.java:570-581 / 626-637 / 317-328): 9 jets at ((r-.5)*10,
    /// 15+(r-.5)*10, (r-.5)*10), fixed droplet size, same 3 rings.
    private func splashGentle() {
        guard let game else { return }
        Audio.shared.play("splash")
        for _ in 0..<9 {
            FXSprites.splashJet(at: SIMD3(position.x, 0, position.z),
                                velocity: SIMD3(Float.random(in: -5...5),
                                                15 + Float.random(in: -5...5),
                                                Float.random(in: -5...5)),
                                in: game.world)
        }
        FXSprites.splashRing(at: SIMD3(position.x, 0.11, position.z), scale: 3, rate: 2, in: game.world)
        FXSprites.splashRing(at: SIMD3(position.x, 0.111, position.z), scale: 6, rate: 1, in: game.world)
        FXSprites.splashRing(at: SIMD3(position.x, 0.1115, position.z), scale: 4, rate: 4, in: game.world)
        die()
    }

    /// Generic boom kept for the default branch and the roller 25 s failsafe:
    /// explosion2/3 + Particles.explosion + camera shock. The per-type ladders
    /// above own their own craters and FX.
    func explode(terrainHit: Bool, big: Bool = false) {
        guard let game else { return }
        Audio.shared.play(big ? "explosion3" : "explosion2")
        Particles.explosion(at: position, in: game.world, big: big)
        game.camera.shock(at: position, radius: big ? 120 : 70)
        die()
    }

    func die() {
        guard alive else { return }
        alive = false
        stopGrind()
        node.removeFromParentNode(); shadowBlob?.removeFromParentNode()
        if !hitSomething && countsMiss { owner.misses += 1 }
        game?.projectileDied(self, impact: position)
    }
}
