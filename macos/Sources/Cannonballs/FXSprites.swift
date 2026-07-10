import Foundation
import SceneKit

/// The ORIGINAL effect sprites (MEDIA/IMAGES sheets, decoded from the WLD3
/// container) played back with the original per-frame physics transcribed
/// from the decompiled Particle_Object_*.java classes.
///
/// - EXPLOSION1: 4x4 sheet, 16 frames @ 20 fps, additive billboard
/// - SMOKEPUFF:  4x2 sheet of 128px puffs; Smoke(type 0) = cols 2-3,
///   SmokeBlack(1) = cols 0-1; grows with decaying accel, rises, damped drift
/// - COIN:       4x4 sheet @ 20 fps looping spin, gravity -32, bounce /1.25,
///   ~5s life then a smoke puff; water: splash burst + rings
/// - SPLASH:     droplet emitter (gravity -32) shedding SPLASH sprites,
///   SplashRing on the water when it lands
enum FXSprites {

    private static var imageCache: [String: NSImage] = [:]
    static func image(_ name: String) -> NSImage? {
        if let c = imageCache[name] { return c }
        let img = Assets.image("IMAGES/FX/\(name).png")
        imageCache[name] = img
        return img
    }

    /// A camera-facing sprite plane showing one cell of a sheet.
    private static func billboard(_ sheet: String, cols: Int, rows: Int,
                                  size: CGFloat, additive: Bool) -> SCNNode {
        let plane = SCNPlane(width: size, height: size)
        let m = SCNMaterial()
        m.diffuse.contents = image(sheet)
        m.diffuse.wrapS = .clamp; m.diffuse.wrapT = .clamp
        m.diffuse.contentsTransform = SCNMatrix4MakeScale(1 / CGFloat(cols), 1 / CGFloat(rows), 1)
        m.lightingModel = .constant
        m.isDoubleSided = true
        m.writesToDepthBuffer = false
        if additive { m.blendMode = .add } else { m.transparencyMode = .dualLayer }
        plane.materials = [m]
        let node = SCNNode(geometry: plane)
        node.constraints = [SCNBillboardConstraint()]
        node.castsShadow = false
        return node
    }

    private static func setFrame(_ node: SCNNode, col: Int, row: Int, cols: Int, rows: Int, mirrored: Bool = false) {
        guard let m = node.geometry?.materials.first else { return }
        let sx = 1 / CGFloat(cols), sy = 1 / CGFloat(rows)
        var t = SCNMatrix4MakeScale(mirrored ? -sx : sx, sy, 1)
        t = SCNMatrix4Mult(t, SCNMatrix4MakeTranslation(CGFloat(col + (mirrored ? 1 : 0)) * sx, CGFloat(row) * sy, 0))
        m.diffuse.contentsTransform = t
    }

    /// Per-frame stepped update (mirrors updateTimeSlice); returns false to remove.
    private static func drive(_ node: SCNNode, _ step: @escaping (SCNNode, Float) -> Bool) {
        var last: CGFloat = 0
        let act = SCNAction.customAction(duration: 30) { n, elapsed in
            let dt = Float(elapsed - last); last = elapsed
            if dt > 0, !step(n, min(dt, 0.1)) {
                n.removeAllActions()
                n.removeFromParentNode()
            }
        }
        node.runAction(.sequence([act, .removeFromParentNode()]))
    }

    // MARK: - Explosion1 (Particle_Object_Explosion1.java)

    static func explosion1(at pos: SIMD3<Float>, trajectory: SIMD3<Float> = .zero,
                           size: Float, in world: World) {
        let node = billboard("EXPLOSION1", cols: 4, rows: 4, size: CGFloat(1 + size), additive: true)
        node.simdPosition = pos
        world.effectsRoot.addChildNode(node)
        var frame: Float = 0, lastFrame = -1
        var p = pos
        drive(node) { n, dt in
            frame += dt * 20
            if frame > 15 { return false }
            p += trajectory * dt
            n.simdPosition = p
            let f = Int(frame)
            if f != lastFrame {
                lastFrame = f
                setFrame(n, col: f % 4, row: f / 4, cols: 4, rows: 4)
            }
            return true
        }
    }

    // MARK: - Smoke (Particle_Object_Smoke.java)

    static func smoke(at pos: SIMD3<Float>, trajectory: SIMD3<Float>,
                      scale: Float, black: Bool = false, in world: World) {
        let node = billboard("SMOKEPUFF", cols: 4, rows: 2, size: 1, additive: false)
        node.simdPosition = pos
        // random puff variant: 4 cells x mirror; the Java UV math wraps so
        // Smoke(0) lands on the white cols 0-1, SmokeBlack(1) on dark cols 2-3
        let v = Int.random(in: 0..<8)
        let col = (v % 2) + (black ? 2 : 0)
        setFrame(node, col: col, row: (v / 2) % 2, cols: 4, rows: 2, mirrored: v > 3)
        world.effectsRoot.addChildNode(node)
        var s = scale
        var accel = Float.random(in: 10...20)
        let rise = Float.random(in: 1...4)
        var traj = trajectory
        var p = pos
        drive(node) { n, dt in
            s += dt * accel
            if accel > -8 { accel -= dt * 20 }
            if s < 0.05 { return false }
            let damp = pow(0.7, dt)
            traj *= damp
            p += traj * dt
            p.y += rise * dt
            n.simdPosition = p
            n.scale = SCNVector3(CGFloat(s), CGFloat(s), CGFloat(s))
            return true
        }
    }

    // MARK: - Coin (Particle_Object_Coin.java)

    static func coin(at pos: SIMD3<Float>, velocity: SIMD3<Float>, in world: World) {
        let node = billboard("COIN", cols: 4, rows: 4, size: 2, additive: false)
        node.simdPosition = pos
        node.eulerAngles.z = CGFloat.random(in: 0..<(2 * .pi))
        world.effectsRoot.addChildNode(node)
        var frame: Float = 0, lastFrame = -1
        var life = Float.random(in: 0..<1)
        var p = pos, v = velocity
        drive(node) { n, dt in
            frame += dt * 20
            while frame > 15.5 { frame -= 15 }
            life += dt
            if life > 5 {
                smoke(at: p, trajectory: SIMD3.random(in: -0.5...0.5),
                      scale: .random(in: 0.3...0.8), in: world)
                return false
            }
            p += v * dt
            v.y -= 32 * dt
            let ground = world.terrain.height(x: p.x, z: p.z)
            if p.y <= ground {
                if ground < 1 {                       // fell in the water
                    if Float.random(in: 0..<1) < 0.25 {
                        // 9 splash jets alongside the rings (Particle_Object_Coin.java:40-48)
                        for _ in 0..<9 {
                            splashJet(at: SIMD3(p.x, 0, p.z),
                                      velocity: SIMD3(Float.random(in: -7.5...7.5),
                                                      20 + Float.random(in: -7.5...7.5),
                                                      Float.random(in: -5...5)), in: world)
                        }
                        splashRing(at: SIMD3(p.x, 0.11, p.z), scale: 3, rate: 2, in: world)
                        splashRing(at: SIMD3(p.x, 0.111, p.z), scale: 6, rate: 1, in: world)
                        splashRing(at: SIMD3(p.x, 0.1115, p.z), scale: 4, rate: 4, in: world)
                    }
                    return false
                }
                p.y = ground
                if v.y < 0 { v.y = -v.y / 1.25 }      // bounce
            }
            n.simdPosition = p
            let f = Int(frame)
            if f != lastFrame {
                lastFrame = f
                setFrame(n, col: f / 4, row: f % 4, cols: 4, rows: 4)   // column-major spin
            }
            return true
        }
    }

    /// Chest burst: 30 coins, the original spread (Chest.java:46).
    static func coinBurst(at pos: SIMD3<Float>, count: Int = 30, in world: World) {
        for _ in 0..<count {
            coin(at: pos + SIMD3(Float.random(in: -0.5...0.5), 2, Float.random(in: -0.5...0.5)),
                 velocity: SIMD3(Float.random(in: -10...10), 30 + Float.random(in: -5...5),
                                 Float.random(in: -10...10)),
                 in: world)
        }
    }

    // MARK: - Splash (Particle_Object_Splash + SplashRing .java)

    /// Invisible ballistic emitter that sheds droplet sprites and rings the water.
    static func splashJet(at pos: SIMD3<Float>, velocity: SIMD3<Float>, in world: World) {
        let node = SCNNode()
        node.simdPosition = pos
        world.effectsRoot.addChildNode(node)
        var p = pos, v = velocity
        drive(node) { _, dt in
            if Float.random(in: 0..<1) < 0.5 {
                droplet(at: p, velocity: SIMD3(Float.random(in: -1.5...1.5),
                                               Float.random(in: -0.5...0),
                                               Float.random(in: -1.5...1.5)),
                        size: Float.random(in: 0.1...1.1), in: world)
            }
            p += v * dt
            v.y -= 32 * dt
            if p.y <= 1 && v.y < 0 {
                splashRing(at: SIMD3(p.x, 0.4, p.z), scale: 3, rate: 2, in: world)
                return false
            }
            return true
        }
    }

    private static func droplet(at pos: SIMD3<Float>, velocity: SIMD3<Float>,
                                size: Float, in world: World) {
        // each droplet picks SPLASH or SPLASH2 art 50/50 (Particle_Object_SplashDrop.java:16-22)
        let sheet = Float.random(in: 0..<1) <= 0.5 ? "SPLASH" : "SPLASH2"
        let node = billboard(sheet, cols: 1, rows: 1, size: CGFloat(0.4 + size), additive: true)
        node.simdPosition = pos
        world.effectsRoot.addChildNode(node)
        var p = pos, v = velocity, life: Float = 0
        drive(node) { n, dt in
            life += dt
            if life > 1.2 || p.y < 0 { return false }
            p += v * dt
            v.y -= 32 * dt
            n.simdPosition = p
            return true
        }
    }

    // MARK: - Debris chunk (Chunk_Object.java: tumbling model piece)

    static func debrisChunk(model: String, at pos: SIMD3<Float>, velocity: SIMD3<Float>,
                            scale: Float, in world: World) {
        guard let actor = SkinnedModel.load(model) else { return }
        Cannon.styleCannonMaterials(actor.root)
        let node = SCNNode()
        actor.root.simdScale = SIMD3(repeating: scale)
        node.addChildNode(actor.root)
        node.simdPosition = pos
        world.effectsRoot.addChildNode(node)
        let spinAxis = simd_normalize(SIMD3<Float>.random(in: -0.5...0.5))
        let spinRate = Float.random(in: -25...25) * .pi / 180
        node.runAction(.repeatForever(.rotate(by: CGFloat(spinRate * 4), around: SCNVector3(spinAxis), duration: 1)))
        var p = pos, v = velocity
        var opacity: Float = 255
        drive(node) { n, dt in
            p += v * dt
            v.y -= 32 * dt
            if v.y < 0 {
                opacity -= dt * 100
                if opacity < 0 { return false }
                n.opacity = CGFloat(opacity / 255)
            }
            n.simdPosition = p
            let ground = world.terrain.height(x: p.x, z: p.z)
            if p.y <= ground {
                if ground < 1 {
                    for _ in 0..<7 {
                        splashJet(at: SIMD3(p.x, 0, p.z),
                                  velocity: SIMD3(Float.random(in: -7.5...7.5),
                                                  20 + Float.random(in: -7.5...7.5),
                                                  Float.random(in: -5...5)), in: world)
                    }
                    splashRing(at: SIMD3(p.x, 0.11, p.z), scale: 3, rate: 2, in: world)
                    return false
                }
                if Bool.random() { Audio.shared.play("puff", volume: 0.6) }
                smoke(at: p, trajectory: SIMD3.random(in: -0.5...0.5),
                      scale: .random(in: 4...6), in: world)
                return false
            }
            return true
        }
    }

    // MARK: - Cloud (Particle_Object_Cloud: 30 SMOKEPUFF billboards in an ellipsoid)

    static func cloud(at pos: SIMD3<Float>, radius: Float, xMul: Float, yMul: Float,
                      in world: World) {
        let root = SCNNode()
        root.simdPosition = pos
        root.name = "cloud"
        for _ in 0..<30 {
            let node = billboard("SMOKEPUFF", cols: 4, rows: 2,
                                 size: CGFloat(Float.random(in: 10...30)), additive: false)
            let v = Int.random(in: 0..<8)
            setFrame(node, col: v % 2, row: (v / 2) % 2, cols: 4, rows: 2, mirrored: v > 3)
            var d = simd_normalize(SIMD3(Float.random(in: -0.5...0.5),
                                         Float.random(in: -0.5...0.5),
                                         Float.random(in: -0.5...0.5)))
            d *= radius * Float.random(in: 0.8...1.0)
            d.x *= xMul; d.y *= yMul
            node.simdPosition = d
            node.geometry?.firstMaterial?.transparency = 0.9
            root.addChildNode(node)
        }
        world.scene.rootNode.addChildNode(root)
    }

    // MARK: - Blob shadow (the original SHADOW alpha patch under game objects)

    /// Main.java ShadowsEnabled (Options menu); Island.switchShadows equivalent.
    static var shadowsEnabled: Bool =
        UserDefaults.standard.object(forKey: "opt.shadows") == nil
            ? true : UserDefaults.standard.bool(forKey: "opt.shadows")

    static func blobShadow(radius: CGFloat) -> SCNNode {
        let plane = SCNPlane(width: radius * 2, height: radius * 2)
        let m = SCNMaterial()
        m.diffuse.contents = image("SHADOW")
        m.lightingModel = .constant
        m.writesToDepthBuffer = false
        m.readsFromDepthBuffer = true
        plane.materials = [m]
        let node = SCNNode(geometry: plane)
        node.name = "blob-shadow"
        node.eulerAngles.x = -.pi / 2
        node.castsShadow = false
        node.renderingOrder = 2
        node.opacity = 0.55
        node.isHidden = !shadowsEnabled
        return node
    }

    // MARK: - Chunk debris (Particle_Object_Chunk; CHUNKS 4x4 sheet)

    /// Flag defaults follow the dirt call sites (Weapon.java:335: random
    /// orientation false, sheds-smoke true); the spike-roller furrow splat
    /// (Weapon.java:211) flips both.
    static func chunk(frame: Int, at pos: SIMD3<Float>, velocity: SIMD3<Float>,
                      size: Float, shedsSmoke: Bool = true, randomRoll: Bool = false,
                      in world: World) {
        let node = billboard("CHUNKS", cols: 4, rows: 4, size: CGFloat(size * 2), additive: false)
        // CHUNKS cell selection is COLUMN-major: u = frame/4, v = frame%4
        // (Particle_Object_Chunk.java:63-67, the same scheme as the coin spin);
        // the sheet's debris art lives in column 0, rows 0-3.
        setFrame(node, col: frame / 4, row: frame % 4, cols: 4, rows: 4)
        node.simdPosition = pos
        if randomRoll {                               // Particle_Object_Chunk.java:82-87
            node.eulerAngles.z = CGFloat.random(in: 0..<(2 * .pi))
        }
        world.effectsRoot.addChildNode(node)
        var life = Float.random(in: 0..<1)            // pre-aged (Particle_Object_Chunk.java:73)
        var p = pos, v = velocity
        drive(node) { n, dt in
            life += dt
            if life > 5 {
                // expiry sheds a smoke puff when the smoke flag is set
                // (Particle_Object_Chunk.java:26-31)
                if shedsSmoke {
                    smoke(at: p, trajectory: SIMD3.random(in: -0.5...0.5),
                          scale: .random(in: 0.3...0.8), in: world)
                }
                return false
            }
            let damp = pow(0.99, dt)
            v.x *= damp; v.z *= damp
            p += v * dt
            v.y -= 32 * dt
            let ground = world.terrain.height(x: p.x, z: p.z)
            if p.y <= ground {
                if ground < 1 { return false }        // water: sinks silently
                if v.y < 0 {
                    // landing puff (Particle_Object_Chunk.java:46-52)
                    if shedsSmoke {
                        smoke(at: p, trajectory: SIMD3.random(in: -0.5...0.5),
                              scale: .random(in: 0.3...0.8), in: world)
                    }
                    return false
                }
                p.y = ground + 1
            }
            n.simdPosition = p
            return true
        }
    }

    /// Impact dirt: pairs of CHUNKS frames 2+3 with the Weapon.java:335 spread.
    static func dirtChunks(at pos: SIMD3<Float>, count: Int = 4, in world: World) {
        for i in 0..<count {
            chunk(frame: 2 + (i % 2),
                  at: pos + SIMD3(Float.random(in: -0.5...0.5), 2, Float.random(in: -0.5...0.5)),
                  velocity: SIMD3(Float.random(in: -15...15), 30 + Float.random(in: -5...5),
                                  Float.random(in: -15...15)),
                  size: Float.random(in: 0.1...1.1), in: world)
        }
    }

    // MARK: - Star (Particle_Object_Star; STAR 4x4 sheet @10fps)

    static func star(at pos: SIMD3<Float>, velocity: SIMD3<Float>, size: Float, in world: World) {
        let node = billboard("STAR", cols: 4, rows: 4, size: CGFloat(size * 2), additive: true)
        node.simdPosition = pos
        // random screen roll (Particle_Object_Star.java:83 setBitmapOrientation)
        node.eulerAngles.z = CGFloat.random(in: 0..<(2 * .pi))
        world.effectsRoot.addChildNode(node)
        // random start frame and pre-aged life (Particle_Object_Star.java:84/:74)
        var frame = Float.random(in: 0..<1) * 15, lastFrame = -1
        var life = Float.random(in: 0..<1), opacity: Float = 255
        var p = pos, v = velocity
        drive(node) { n, dt in
            if v.y < 0 || opacity < 255 { opacity -= dt * 180 }
            frame += dt * 10
            while frame > 15.5 { frame -= 15 }
            life += dt
            if life > 5 || opacity <= 0 {
                smoke(at: p, trajectory: SIMD3.random(in: -0.5...0.5),
                      scale: .random(in: 0.3...0.8), in: world)
                return false
            }
            let damp = pow(0.99, dt)
            v.x *= damp; v.z *= damp
            p += v * dt
            v.y -= 32 * dt * 0.75
            let ground = world.terrain.height(x: p.x, z: p.z)
            if p.y <= ground {
                if ground >= 1 {
                    smoke(at: p, trajectory: SIMD3.random(in: -0.5...0.5),
                          scale: .random(in: 0.3...0.8), in: world)
                }
                return false
            }
            n.simdPosition = p
            n.opacity = CGFloat(opacity / 255)
            let f = Int(frame)
            if f != lastFrame {
                lastFrame = f
                setFrame(n, col: f / 4, row: f % 4, cols: 4, rows: 4)
            }
            return true
        }
    }

    // MARK: - Sparkle (Particle_Object_Sparkle; FIREFLY glow, wandering swirl)

    static func sparkle(at pos: SIMD3<Float>, velocity: SIMD3<Float>, in world: World) {
        let node = billboard("FIREFLY", cols: 1, rows: 1,
                             size: CGFloat(Float.random(in: 0.5...1.5) * 2), additive: true)
        node.simdPosition = pos
        world.effectsRoot.addChildNode(node)
        var life: Float = 5
        var p = pos, v = velocity
        var left = Bool.random(), up = Bool.random()
        drive(node) { n, dt in
            life -= dt
            if life <= 0 { return false }
            if Float.random(in: 0..<1) < 0.1 { left.toggle() }
            if Float.random(in: 0..<1) < 0.1 { up.toggle() }
            v.x += (left ? -1 : 1) * dt * 6
            v.y += (up ? 1 : -1) * dt * 4
            p += v * dt
            n.simdPosition = p
            n.opacity = CGFloat(min(1, life / 1.5))
            return true
        }
    }

    // MARK: - Ray (Particle_Object_Ray; vertical light shaft at the terrain)

    static func ray(atX x: Float, z: Float, scale: Float, delayed: Bool, in world: World) {
        let plane = SCNPlane(width: CGFloat(scale), height: 1)
        let m = SCNMaterial()
        m.diffuse.contents = image("RAY")
        m.lightingModel = .constant
        m.blendMode = .add
        m.isDoubleSided = true
        m.writesToDepthBuffer = false
        plane.materials = [m]
        let node = SCNNode(geometry: plane)
        let ground = world.terrain.height(x: x, z: z)
        node.simdPosition = SIMD3(x, ground - 0.25, z)
        node.constraints = [SCNBillboardConstraint()]
        node.castsShadow = false
        world.effectsRoot.addChildNode(node)
        var delay: Float = delayed ? Float.random(in: 0..<1) : 0
        var h: Float = 0.001, w = scale
        var rate = Float.random(in: 140...260)
        var opacity: Float = 255
        node.opacity = 0
        drive(node) { n, dt in
            delay -= dt
            guard delay <= 0 else { return true }
            if h > 5 { opacity -= dt * 100 }
            h += dt * rate
            w += dt * rate * 0.01
            rate *= pow(0.1, dt)
            if opacity < 0 { return false }
            n.opacity = CGFloat(opacity / 255)
            n.scale = SCNVector3(CGFloat(w), CGFloat(h), 1)
            n.simdPosition.y = world.terrain.height(x: x, z: z) - 0.25 + h / 2
            return true
        }
    }

    // MARK: - Shockwave (Particle_Object_Shockwave; expanding ground ring)

    static func shockwave(at pos: SIMD3<Float>, scale: Float, rate: Float, in world: World) {
        let plane = SCNPlane(width: 2, height: 2)
        let m = SCNMaterial()
        m.diffuse.contents = image("SHOCKWAVE")
        m.lightingModel = .constant
        m.blendMode = .add
        m.isDoubleSided = true
        m.writesToDepthBuffer = false
        plane.materials = [m]
        let node = SCNNode(geometry: plane)
        node.simdPosition = SIMD3(pos.x, pos.y + 0.25, pos.z)
        node.eulerAngles.x = -.pi / 2
        node.castsShadow = false
        world.effectsRoot.addChildNode(node)
        var opacity: Float = 255, s = scale
        drive(node) { n, dt in
            opacity -= dt * 140                       // Particle_Object_Shockwave.java:55
            s += rate * dt * 2
            if opacity < 0 { return false }
            n.opacity = CGFloat(opacity / 255)
            n.scale = SCNVector3(CGFloat(s), CGFloat(s), CGFloat(s))
            return true
        }
    }

    // MARK: - Smoke column (Particle_Object_SmokeColumn; stationary emitter)

    static func smokeColumn(at pos: SIMD3<Float>, duration: Float, fire: Bool = false,
                            black: Bool = false, alive: (() -> Bool)? = nil,
                            in world: World) {
        let node = SCNNode()
        node.simdPosition = pos
        world.effectsRoot.addChildNode(node)
        var life = duration
        var tick: Float = 0
        drive(node) { _, dt in
            if let alive, !alive() { return false }   // prop fires die with the prop
            life -= dt
            if life <= 0 { return false }
            tick += dt
            guard tick > 1.0 / 30 else { return true }     // per original frame cadence
            tick = 0
            if Float.random(in: 0..<1) < 0.7 {
                smoke(at: pos, trajectory: SIMD3(Float.random(in: -1.5...1.5),
                                                 Float.random(in: 10...13),
                                                 Float.random(in: -1.5...1.5)),
                      scale: .random(in: 0.6...1.4), black: black, in: world)
            }
            if fire {
                if Float.random(in: 0..<1) < 0.1 {
                    explosion1(at: pos, trajectory: SIMD3(0, Float.random(in: 4...9), 0),
                               size: .random(in: 4...6), in: world)
                }
                if Float.random(in: 0..<1) < 0.1 {
                    sparkle(at: pos + SIMD3(Float.random(in: -1...1), 0, Float.random(in: -1...1)),
                            velocity: SIMD3(Float.random(in: -0.5...0.5),
                                            Float.random(in: 2...7),
                                            Float.random(in: -0.5...0.5)), in: world)
                }
            }
            return true
        }
    }

    // MARK: - Fire trail ember (Particle_Object_FireTrail; ballistic, sheds smoke)

    static func fireTrailEmber(at pos: SIMD3<Float>, velocity: SIMD3<Float>, in world: World) {
        let node = SCNNode()
        node.simdPosition = pos
        world.effectsRoot.addChildNode(node)
        var life: Float = 0
        var p = pos, v = velocity
        drive(node) { _, dt in
            life += dt
            if life > 6 { return false }
            // 50% smoke shed + 30% mini fireball per frame
            // (Particle_Object_FireTrail.java:73-82)
            if Float.random(in: 0..<1) < 0.5 {
                smoke(at: p, trajectory: SIMD3.random(in: -0.5...0.5),
                      scale: .random(in: 0.3...1.1), in: world)
            }
            if Float.random(in: 0..<1) < 0.3 {
                explosion1(at: p, size: .random(in: 3...5), in: world)
            }
            p += v * dt
            v.y -= 32 * dt
            let ground = world.terrain.height(x: p.x, z: p.z)
            if p.y <= ground {
                if ground < 1 {
                    for _ in 0..<9 {
                        splashJet(at: SIMD3(p.x, 0, p.z),
                                  velocity: SIMD3(Float.random(in: -7.5...7.5),
                                                  20 + Float.random(in: -7.5...7.5),
                                                  Float.random(in: -5...5)), in: world)
                    }
                    splashRing(at: SIMD3(p.x, 0.4, p.z), scale: 3, rate: 2, in: world)
                    return false
                }
                p.y = ground
                if v.y < 0 { v.y = -v.y / 1.25 }
            }
            return true
        }
    }

    /// The teleport effect (Particle_Object_Teleport): a smoke column, a ring of
    /// 12 STAR sprites, 12 sparkles, and a 1.5s expanding spiral of light rays.
    static func teleportBurst(at pos: SIMD3<Float>, in world: World) {
        smokeColumn(at: pos - SIMD3(0, 4, 0), duration: 0.5, in: world)
        var ring = SIMD3<Float>(0, 0, 20)
        for _ in 0..<12 {
            let ca = cos(Float.pi / 6), sa = sin(Float.pi / 6)
            ring = SIMD3(ring.x * ca + ring.z * sa, 0, -ring.x * sa + ring.z * ca)
            star(at: pos, velocity: SIMD3(ring.x, 10, ring.z),
                 size: Float.random(in: 1...3), in: world)
            sparkle(at: pos + SIMD3(Float.random(in: -7.5...7.5), Float.random(in: 0...2),
                                    Float.random(in: -7.5...7.5)),
                    velocity: SIMD3(Float.random(in: -2.5...2.5), Float.random(in: 2...7),
                                    Float.random(in: -5...5)), in: world)
        }
        // spiral rays: angle sweeps 500 deg/s, radius grows 2.5/s, one ray each 0.01s
        let spinner = SCNNode()
        world.effectsRoot.addChildNode(spinner)
        var life: Float = 0, angle: Float = 0, radius: Float = 0, tick: Float = 0
        drive(spinner) { _, dt in
            life += dt
            if life > 1.5 { return false }
            angle += dt * 500 * .pi / 180
            radius += dt * 2.5
            tick += dt
            if tick > 0.01 {
                tick = 0
                ray(atX: pos.x + sin(angle) * radius * 2, z: pos.z + cos(angle) * radius * 2,
                    scale: Float.random(in: 2...4), delayed: false, in: world)
            }
            return true
        }
    }

    /// Expanding, fading ring lying flat on the water (Particle_Object_SplashRing).
    static func splashRing(at pos: SIMD3<Float>, scale: Float, rate: Float, in world: World) {
        let plane = SCNPlane(width: 2, height: 2)
        let m = SCNMaterial()
        m.diffuse.contents = image("SPLASHRING")
        m.lightingModel = .constant
        m.blendMode = .add
        m.isDoubleSided = true
        m.writesToDepthBuffer = false
        plane.materials = [m]
        let node = SCNNode(geometry: plane)
        node.simdPosition = SIMD3(pos.x, max(pos.y, 0.25), pos.z)
        node.eulerAngles.x = -.pi / 2
        node.castsShadow = false
        world.effectsRoot.addChildNode(node)
        var opacity: Float = 255, s = scale
        drive(node) { n, dt in
            opacity -= dt * 70
            s += rate * dt * 2
            if opacity < 0 { return false }
            n.opacity = CGFloat(opacity / 255)
            n.scale = SCNVector3(CGFloat(s), CGFloat(s), CGFloat(s))
            return true
        }
    }
}
