import Foundation
import SceneKit

/// Gameplay VFX. The headline effects (explosion, smoke, coins, splashes) play
/// the ORIGINAL decoded sprite sheets via FXSprites with the decompiled
/// per-frame physics; the rest remain procedural approximations.
enum Particles {

    private static func burst(in world: World, at pos: SIMD3<Float>, configure: (SCNParticleSystem) -> Void, lifetime: TimeInterval = 3) {
        let ps = SCNParticleSystem()
        configure(ps)
        let holder = SCNNode()
        holder.simdPosition = pos
        holder.addParticleSystem(ps)
        world.effectsRoot.addChildNode(holder)
        DispatchQueue.main.asyncAfter(deadline: .now() + lifetime) {
            holder.removeFromParentNode()
        }
    }

    /// The original impact boom (Weapon.java:140-144): one EXPLOSION1 sprite
    /// (16 frames @ 20fps) + four rising smoke puffs.
    static func explosion(at pos: SIMD3<Float>, in world: World, big: Bool = false) {
        let size: Float = big ? 40 : 3 + .random(in: 0...2)
        FXSprites.explosion1(at: pos + SIMD3(Float.random(in: -0.5...0.5), 2, Float.random(in: -0.5...0.5)),
                             trajectory: SIMD3(Float.random(in: -0.17...0.17), 1, Float.random(in: -0.17...0.17)),
                             size: size, in: world)
        let puffs = big ? 4 : 2
        for _ in 0..<puffs {
            FXSprites.smoke(at: pos + SIMD3(Float.random(in: -0.5...0.5), 2, Float.random(in: -0.5...0.5)),
                            trajectory: SIMD3(Float.random(in: -0.17...0.17), 1, Float.random(in: -0.17...0.17)),
                            scale: 1.0, in: world)
        }
    }

    /// The original water impact (Weapon.java:87-98): 9 ballistic splash jets
    /// shedding droplet sprites + 3 expanding SPLASHRING planes.
    static func splash(at pos: SIMD3<Float>, in world: World) {
        for _ in 0..<9 {
            FXSprites.splashJet(at: SIMD3(pos.x, 0, pos.z),
                                velocity: SIMD3(Float.random(in: -7.5...7.5),
                                                20 + Float.random(in: -7.5...7.5),
                                                Float.random(in: -5...5)),
                                in: world)
        }
        FXSprites.splashRing(at: SIMD3(pos.x, 0.11, pos.z), scale: 3, rate: 2, in: world)
        FXSprites.splashRing(at: SIMD3(pos.x, 0.111, pos.z), scale: 6, rate: 1, in: world)
        FXSprites.splashRing(at: SIMD3(pos.x, 0.1115, pos.z), scale: 4, rate: 4, in: world)
    }

    private static func splashLegacy(at pos: SIMD3<Float>, in world: World) {
        burst(in: world, at: pos, configure: { ps in
            ps.birthRate = 250
            ps.loops = false
            ps.emissionDuration = 0.08
            ps.particleLifeSpan = 0.9
            ps.particleSize = 2.5
            ps.particleColor = NSColor(calibratedRed: 0.75, green: 0.9, blue: 1, alpha: 0.9)
            ps.particleVelocity = 38
            ps.particleVelocityVariation = 15
            ps.acceleration = SCNVector3(0, -40, 0)
            ps.emittingDirection = SCNVector3(0, 1, 0)
            ps.spreadingAngle = 35
        })
        // expanding ring
        let ring = SCNNode(geometry: SCNTorus(ringRadius: 2, pipeRadius: 0.4))
        let m = SCNMaterial()
        m.diffuse.contents = NSColor(calibratedWhite: 1, alpha: 0.8)
        m.lightingModel = .constant
        ring.geometry?.materials = [m]
        ring.simdPosition = SIMD3(pos.x, 0.4, pos.z)
        world.effectsRoot.addChildNode(ring)
        ring.runAction(.sequence([.group([.scale(to: 5, duration: 0.9), .fadeOut(duration: 0.9)]), .removeFromParentNode()]))
    }

    /// Ambient fish leaping from the ocean: animated FISH sprite on a billboard,
    /// gravity arc, splash at launch and on re-entry.
    static func fishJump(at pos: SIMD3<Float>, velocity: SIMD3<Float>, in world: World) {
        splash(at: pos, in: world)
        let plane = SCNPlane(width: 5, height: 5)
        let m = SCNMaterial()
        m.diffuse.contents = HUDArt.crop("fish.png", NSRect(x: 0, y: 0, width: 64, height: 64))
        m.lightingModel = .constant
        m.isDoubleSided = true
        m.transparencyMode = .aOne
        plane.materials = [m]
        let fish = SCNNode(geometry: plane)
        fish.simdPosition = pos
        fish.constraints = [SCNBillboardConstraint()]
        world.effectsRoot.addChildNode(fish)
        // flip through the 8 sheet frames while airborne
        var frame = 0
        let animate = SCNAction.repeatForever(.sequence([
            .run { n in
                frame = (frame + 1) % 8
                let col = frame % 2, row = (frame / 2) % 4
                n.geometry?.firstMaterial?.diffuse.contents =
                    HUDArt.crop("fish.png", NSRect(x: col * 64, y: row * 64, width: 64, height: 64))
            },
            .wait(duration: 0.09)
        ]))
        fish.runAction(animate)
        // ballistic arc: solve flight time for return to y=0 (v*t - 16t^2 = 0)
        let tFlight = Double(velocity.y / 16)
        let move = SCNAction.customAction(duration: tFlight) { n, t in
            let tf = Float(t)
            n.simdPosition = SIMD3(pos.x + velocity.x * tf,
                                   pos.y + velocity.y * tf + G.gravity * 0.5 * tf * tf,
                                   pos.z + velocity.z * tf)
        }
        fish.runAction(.sequence([move, .run { _ in
            splash(at: SIMD3(pos.x + velocity.x * Float(tFlight), 0, pos.z + velocity.z * Float(tFlight)), in: world)
        }, .removeFromParentNode()]))
    }

    /// Impact dirt: the original CHUNKS debris pair (Weapon.java:335-338).
    static func dirt(at pos: SIMD3<Float>, in world: World) {
        FXSprites.dirtChunks(at: pos, count: 2, in: world)
    }

    /// Original SMOKEPUFF sprite with the decompiled growth/rise/damping physics.
    static func smokePuff(at pos: SIMD3<Float>, in world: World) {
        for _ in 0..<3 {
            FXSprites.smoke(at: pos + SIMD3(Float.random(in: -0.5...0.5), 0, Float.random(in: -0.5...0.5)),
                            trajectory: SIMD3(Float.random(in: -0.17...0.17), 1, Float.random(in: -0.17...0.17)),
                            scale: 2.0, in: world)
        }
    }

    /// Muzzle smoke along the fire direction (Cannon.java:279).
    static func muzzleSmoke(at pos: SIMD3<Float>, in world: World) {
        for _ in 0..<4 {
            FXSprites.smoke(at: pos,
                            trajectory: SIMD3(Float.random(in: -1.5...1.5),
                                              Float.random(in: -1.5...1.5),
                                              Float.random(in: -1.5...1.5)),
                            scale: .random(in: 0.2...1.2), in: world)
        }
    }

    /// The chest treasure burst: 30 spinning COIN sprites with gravity + bounce.
    static func coins(at pos: SIMD3<Float>, count: Int, in world: World) {
        FXSprites.coinBurst(at: pos, count: max(count, 8), in: world)
    }

    /// The original teleport: smoke column + STAR ring + sparkles + ray spiral.
    static func teleport(at pos: SIMD3<Float>, in world: World) {
        FXSprites.teleportBurst(at: pos, in: world)
    }

    static func deathBlast(at pos: SIMD3<Float>, in world: World) {
        explosion(at: pos, in: world, big: true)
        smokePuff(at: pos + SIMD3<Float>(0, 6, 0), in: world)
        // the original death: 3 flaming embers arc out (Cannon.java:1120-1122)
        for _ in 0..<3 {
            FXSprites.fireTrailEmber(at: pos + SIMD3(Float.random(in: -0.5...0.5), 2, Float.random(in: -0.5...0.5)),
                                     velocity: SIMD3(Float.random(in: -10...10),
                                                     30 + Float.random(in: -5...5),
                                                     Float.random(in: -10...10)), in: world)
        }
    }
}
