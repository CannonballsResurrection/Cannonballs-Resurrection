import Foundation
import SceneKit
import SpriteKit
import Metal
import CoreGraphics
import AppKit

// Offline cinematic trailer recorder.
//
// Runs a full 8-player bot match headless (the `--simulate` pattern — no window,
// no NSApplication) and captures a PNG frame sequence through an offscreen
// SCNRenderer (the `Snapshot` pattern). A small shot director drives the camera
// each frame — sweeping orbits, dollies, crane reveals, and live-cannonball
// chases, plus cuts to the game's own camera modes — so the footage is real
// in-engine gameplay filmed cinematically. `tools/build_trailer.py` stitches the
// per-island sequences into the final scored trailer.
//
// The camera is overridden AFTER GameController.update (which runs its own
// camera.update + sky-follow), so we re-sync the sky dome to the overridden
// camera each frame, mirroring GameController.swift:431-433 and Snapshot.swift:99.

// MARK: - Easing

enum Easing {
    case linear, easeIn, easeOut, easeInOut
    func apply(_ u: Float) -> Float {
        let t = min(1, max(0, u))
        switch self {
        case .linear:    return t
        case .easeIn:    return t * t
        case .easeOut:   return 1 - (1 - t) * (1 - t)
        case .easeInOut: return t < 0.5 ? 2 * t * t : 1 - pow(-2 * t + 2, 2) / 2
        }
    }
}

// MARK: - Shots

/// A camera behavior. Numeric radii/heights are resolved at apply-time from the
/// island's world size so shots scale to any map.
enum ShotBehavior {
    case craneReveal(sweepDeg: Float)              // rise over the island, small azimuth swing
    case craneDown(sweepDeg: Float)                // descend from high to low
    case orbitIsland(startDeg: Float, sweepDeg: Float)   // wide high dolly around the whole island
    case lowOrbitIsland(startDeg: Float, sweepDeg: Float) // low skimming orbit near the waterline
    case lateralTrack(dir: Float)                  // low linear truck across the island front (±1)
    case orbitHero(startDeg: Float, sweepDeg: Float)     // tight orbit around the active cannon
    case dollyInHero                               // linear push-in toward the active cannon
    case pushOutHero                               // reverse dolly: close → wide reveal
    case chase                                     // follow the live cannonball (falls back to a hero orbit)
    case gameCam(CameraController.Mode)            // delegate to the engine's own camera mode
}

struct TrailerShot {
    var duration: Double
    var behavior: ShotBehavior
    var easing: Easing = .easeInOut
}

// MARK: - Director

/// Sequences shots over time and drives `gc.camera.node` each frame. In `finale`
/// mode it does nothing — the engine's `.success` orbit owns the closing shot.
final class TrailerDirector {
    private let gc: GameController
    private let shots: [TrailerShot]
    private let starts: [Double]
    private let total: Double
    private let finale: Bool
    private var lastShotIndex = -1
    private var chaseInit = false

    init(gc: GameController, seconds: Double, variety: Int, finale: Bool) {
        self.gc = gc
        self.finale = finale
        if finale {
            shots = []; starts = []; total = seconds
            return
        }
        let list = TrailerDirector.defaultShots(seconds: seconds, variety: variety)
        var acc = 0.0
        var st: [Double] = []
        for s in list { st.append(acc); acc += s.duration }
        shots = list
        starts = st
        total = acc
    }

    /// Build a repeating, varied sequence that fills `seconds`. Short island cuts
    /// (a couple of seconds) get a single dramatic shot; the long Tropicali clip
    /// gets the full rotation. `variety` rotates the starting shot so successive
    /// islands don't all open the same way.
    static func defaultShots(seconds: Double, variety: Int) -> [TrailerShot] {
        // Deliberately alternate motion TYPE (push / wide orbit / chase / tight
        // orbit / lateral / low skim / crane) and DIRECTION (+/- sweep, L/R
        // track) so no two adjacent shots read as the same slow pan.
        let palette: [TrailerShot] = [
            TrailerShot(duration: 4.5, behavior: .dollyInHero),
            TrailerShot(duration: 5.5, behavior: .orbitIsland(startDeg: 30, sweepDeg: 85)),
            TrailerShot(duration: 4.5, behavior: .chase),
            TrailerShot(duration: 4.5, behavior: .orbitHero(startDeg: 210, sweepDeg: -170)),
            TrailerShot(duration: 4.0, behavior: .lateralTrack(dir: 1)),
            TrailerShot(duration: 5.0, behavior: .lowOrbitIsland(startDeg: 250, sweepDeg: -70)),
            TrailerShot(duration: 4.5, behavior: .chase),
            TrailerShot(duration: 4.5, behavior: .craneDown(sweepDeg: -45)),
            TrailerShot(duration: 4.5, behavior: .pushOutHero),
            TrailerShot(duration: 5.5, behavior: .orbitIsland(startDeg: 300, sweepDeg: -95)),
            TrailerShot(duration: 3.5, behavior: .gameCam(.shot)),
            TrailerShot(duration: 4.5, behavior: .orbitHero(startDeg: 60, sweepDeg: 190)),
            TrailerShot(duration: 4.0, behavior: .lateralTrack(dir: -1)),
        ]
        // Short island cut: one dramatic shot, picked from a diverse set so
        // successive islands don't repeat a look.
        if seconds <= 4.5 {
            let short: [ShotBehavior] = [
                .orbitHero(startDeg: Float((variety * 47) % 360), sweepDeg: 120),
                .chase,
                .lowOrbitIsland(startDeg: Float((variety * 61) % 360), sweepDeg: 60),
                .craneReveal(sweepDeg: 35),
                .lateralTrack(dir: variety % 2 == 0 ? 1 : -1),
                .dollyInHero,
                .orbitIsland(startDeg: Float((variety * 83) % 360), sweepDeg: -55),
                .pushOutHero,
                .craneDown(sweepDeg: -40),
                .orbitHero(startDeg: Float((variety * 113) % 360), sweepDeg: -130),
            ]
            let b = short[((variety % short.count) + short.count) % short.count]
            return [TrailerShot(duration: seconds, behavior: b)]
        }
        // Long clip: rotate the palette by variety and loop to fill.
        var out: [TrailerShot] = []
        var acc = 0.0
        var i = ((variety % palette.count) + palette.count) % palette.count
        while acc < seconds {
            var s = palette[i % palette.count]
            if acc + s.duration > seconds { s.duration = seconds - acc }   // trim last
            out.append(s)
            acc += s.duration
            i += 1
        }
        return out
    }

    func apply(at t: Double, dt: Float) {
        if finale {
            // Cinematic orbit around the sole survivor (chosen on flat, low ground
            // in the recorder so the cannon reads against water/sky, not a slope).
            let w = gc.players.first { $0.active } ?? gc.players.first!
            let c = w.position
            let a = G.deg2rad(Float(t) * 16 + 40)      // ~16°/s
            let node = gc.camera.node
            aim(node, from: c + SIMD3<Float>(sin(a) * 44, 20, cos(a) * 44), to: c + SIMD3<Float>(0, 3, 0))
            return
        }
        guard !shots.isEmpty else { return }
        // find active shot
        var idx = shots.count - 1
        for k in 0..<shots.count where t < starts[k] + shots[k].duration { idx = k; break }
        let shot = shots[idx]
        let localStart = starts[idx]
        let u = shot.duration > 0 ? Float((t - localStart) / shot.duration) : 1
        let e = shot.easing.apply(u)

        if idx != lastShotIndex {
            lastShotIndex = idx
            chaseInit = false
            if case .gameCam(let m) = shot.behavior { gc.camera.setMode(m) }
        }

        let ws = gc.world.terrain.worldSize
        let node = gc.camera.node
        let islandCenter = gc.world.center
        let lookIsland = islandCenter + SIMD3<Float>(0, 18, 0)

        switch shot.behavior {
        case .gameCam:
            return   // don't override — GameController.update already positioned the camera

        case .craneReveal(let sweep):
            let r = max(260, ws * 0.45)
            let a = G.deg2rad(sweep * e)
            let y = mix(45, max(190, ws * 0.34), t: e)   // rise
            aim(node, from: islandCenter + SIMD3<Float>(sin(a) * r, y, cos(a) * r), to: lookIsland)

        case .craneDown(let sweep):
            let r = max(260, ws * 0.45)
            let a = G.deg2rad(sweep * e)
            let y = mix(max(190, ws * 0.34), 55, t: e)   // descend
            aim(node, from: islandCenter + SIMD3<Float>(sin(a) * r, y, cos(a) * r), to: lookIsland)

        case .orbitIsland(let start, let sweep):
            let r = max(320, ws * 0.55)
            let h = max(150, ws * 0.30)
            let a = G.deg2rad(start + sweep * e)
            aim(node, from: islandCenter + SIMD3<Float>(sin(a) * r, h, cos(a) * r), to: lookIsland)

        case .lowOrbitIsland(let start, let sweep):
            let r = max(230, ws * 0.40)
            let h = max(45, ws * 0.10)                   // skim the waterline
            let a = G.deg2rad(start + sweep * e)
            aim(node, from: islandCenter + SIMD3<Float>(sin(a) * r, h, cos(a) * r),
                to: islandCenter + SIMD3<Float>(0, 10, 0))

        case .lateralTrack(let dir):
            let r = max(300, ws * 0.5)
            let h = max(40, ws * 0.09)
            let off = max(260, ws * 0.42)
            let x = mix(-dir * r, dir * r, t: e)          // low truck across the front
            aim(node, from: islandCenter + SIMD3<Float>(x, h, off), to: islandCenter + SIMD3<Float>(0, 14, 0))

        case .orbitHero(let start, let sweep):
            let hero = heroTarget()
            let a = G.deg2rad(start + sweep * e)
            aim(node, from: hero + SIMD3<Float>(sin(a) * 36, 16, cos(a) * 36), to: hero + SIMD3<Float>(0, 4, 0))

        case .dollyInHero:
            let hero = heroTarget()
            let pos = mix(hero + SIMD3<Float>(0, 34, -78), hero + SIMD3<Float>(0, 9, -24), t: e)
            aim(node, from: pos, to: hero + SIMD3<Float>(0, 5, 0))

        case .pushOutHero:
            let hero = heroTarget()
            let pos = mix(hero + SIMD3<Float>(0, 10, -22), hero + SIMD3<Float>(0, 44, -96), t: e)
            aim(node, from: pos, to: hero + SIMD3<Float>(0, 5, 0))

        case .chase:
            if let proj = liveProjectile() {
                let back = simd_length(proj.traj) > 0.1 ? simd_normalize(proj.traj) : SIMD3<Float>(0, 0, 1)
                let desired = proj.position - back * 13 + SIMD3<Float>(0, 5, 0)
                if !chaseInit { node.simdPosition = desired; chaseInit = true }
                else { node.simdPosition += (desired - node.simdPosition) * min(1, 7 * dt) }
                aimLook(node, to: proj.position)
            } else {
                // no live shot this frame: a tight hero orbit keeps it dynamic
                let hero = heroTarget()
                let a = G.deg2rad(40 + 90 * e)
                aim(node, from: hero + SIMD3<Float>(sin(a) * 34, 15, cos(a) * 34), to: hero + SIMD3<Float>(0, 4, 0))
            }
        }
    }

    // MARK: helpers

    private func heroTarget() -> SIMD3<Float> {
        // feature the presenter (localHuman) so the on-camera cannon is the one
        // whose HUD (power bar, angle, gold) is live
        (gc.localHuman ?? gc.cameraFocusCannon ?? gc.players.first { $0.active } ?? gc.players.first!).position
    }

    private func liveProjectile() -> Projectile? {
        gc.projectiles.first { $0.alive && !$0.owner.isBot }         // the presenter's shot first
            ?? gc.projectiles.first { $0.alive && $0.owner.firedOnTurn }
            ?? gc.projectiles.first { $0.alive }
    }

    private func aim(_ node: SCNNode, from pos: SIMD3<Float>, to look: SIMD3<Float>) {
        node.simdPosition = pos
        aimLook(node, to: look)
    }

    /// Look-at with the same world-up / gimbal guard the engine camera uses
    /// (CameraController.swift:206-211).
    private func aimLook(_ node: SCNNode, to look: SIMD3<Float>) {
        var fwd = look - node.simdPosition
        if simd_length(fwd) < 1e-4 { fwd = SIMD3(0, 0, 1) }
        fwd = simd_normalize(fwd)
        var up = SIMD3<Float>(0, 1, 0)
        if abs(simd_dot(fwd, up)) > 0.995 { up = SIMD3<Float>(0, 0, 1) }
        node.look(at: SCNVector3(look), up: SCNVector3(up), localFront: SCNVector3(0, 0, -1))
    }
}

private func mix(_ a: SIMD3<Float>, _ b: SIMD3<Float>, t: Float) -> SIMD3<Float> { a + (b - a) * t }
private func mix(_ a: Float, _ b: Float, t: Float) -> Float { a + (b - a) * t }

/// Point `node` from `pos` at `look` (shared world-up / gimbal guard).
func camAim(_ node: SCNNode, from pos: SIMD3<Float>, to look: SIMD3<Float>) {
    node.simdPosition = pos
    camLook(node, to: look)
}
func camLook(_ node: SCNNode, to look: SIMD3<Float>) {
    var fwd = look - node.simdPosition
    if simd_length(fwd) < 1e-4 { fwd = SIMD3(0, 0, 1) }
    fwd = simd_normalize(fwd)
    var up = SIMD3<Float>(0, 1, 0)
    if abs(simd_dot(fwd, up)) > 0.995 { up = SIMD3<Float>(0, 0, 1) }
    node.look(at: SCNVector3(look), up: SCNVector3(up), localFront: SCNVector3(0, 0, -1))
}

// MARK: - HUD compositor

/// Renders the game's HUD (an 800x600 SKScene, scaleMode .fill) offscreen via
/// SKRenderer so it can be composited over each 3D frame — the same overlay the
/// live game shows on top of the scene.
final class HUDCompositor {
    private let queue: MTLCommandQueue
    private let renderer: SKRenderer
    private let tex: MTLTexture
    private let w: Int, h: Int

    init?(device: MTLDevice, scene: SKScene, size: CGSize) {
        guard let q = device.makeCommandQueue() else { return nil }
        queue = q
        w = Int(size.width); h = Int(size.height)
        renderer = SKRenderer(device: device)
        renderer.scene = scene
        scene.isPaused = false          // else SKRenderer won't advance actions (spinning coin)
        let d = MTLTextureDescriptor.texture2DDescriptor(pixelFormat: .bgra8Unorm, width: w, height: h, mipmapped: false)
        d.usage = [.renderTarget, .shaderRead]
        d.storageMode = .managed
        guard let t = device.makeTexture(descriptor: d) else { return nil }
        tex = t
    }

    /// The HUD as a transparent-background NSImage at time `t`.
    func image(at t: TimeInterval) -> NSImage? {
        renderer.update(atTime: t)
        let rpd = MTLRenderPassDescriptor()
        rpd.colorAttachments[0].texture = tex
        rpd.colorAttachments[0].loadAction = .clear
        rpd.colorAttachments[0].clearColor = MTLClearColorMake(0, 0, 0, 0)
        rpd.colorAttachments[0].storeAction = .store
        guard let cb = queue.makeCommandBuffer() else { return nil }
        renderer.render(withViewport: CGRect(x: 0, y: 0, width: w, height: h),
                        commandBuffer: cb, renderPassDescriptor: rpd)
        if let blit = cb.makeBlitCommandEncoder() { blit.synchronize(resource: tex); blit.endEncoding() }
        cb.commit(); cb.waitUntilCompleted()

        var raw = [UInt8](repeating: 0, count: w * h * 4)
        raw.withUnsafeMutableBytes { ptr in
            tex.getBytes(ptr.baseAddress!, bytesPerRow: w * 4,
                         from: MTLRegionMake2D(0, 0, w, h), mipmapLevel: 0)
        }
        let cs = CGColorSpaceCreateDeviceRGB()
        let info = CGImageAlphaInfo.premultipliedFirst.rawValue | CGBitmapInfo.byteOrder32Little.rawValue
        guard let ctx = CGContext(data: &raw, width: w, height: h, bitsPerComponent: 8,
                                  bytesPerRow: w * 4, space: cs, bitmapInfo: info),
              let cg = ctx.makeImage() else { return nil }
        return NSImage(cgImage: cg, size: CGSize(width: w, height: h))
    }
}

/// Distinct camera framings for the weapon showcase so no two weapons repeat a look.
enum ShowShot { case behind, side, top, low, front, wide }

// MARK: - Presenter

/// Drives the human player-0 cannon so the HUD reads live: it aims (the barrel and
/// angle meter move), charges the power bar, spends gold (0–3000), and fires. Every
/// HUD stat follows `game.localHuman`, so this is what makes the HUD dynamic.
final class Presenter {
    let c: Cannon
    private unowned let gc: GameController
    private enum Phase { case aim, charge, cool }
    private var phase = Phase.aim
    private var timer: Float = 0.8
    private let plan = [0, 5, 9, 7, 6, 8, 2, 4, 3]   // a varied weapon rotation for the opening
    private var pi = 0
    private var charge: Float = 0

    init(cannon: Cannon, gc: GameController) { self.c = cannon; self.gc = gc; c.cash = 3000 }

    func nearestEnemy() -> Cannon? {
        gc.players.filter { $0 !== c && $0.active }
            .min { dist2D($0.position, c.position) < dist2D($1.position, c.position) }
    }
    func snapToNearest() {
        guard let tg = nearestEnemy() else { return }
        c.spinAngle = G.rad2deg(atan2(tg.position.x - c.position.x, tg.position.z - c.position.z))
    }
    func straightTiltDeg() -> Float {
        guard let tg = nearestEnemy() else { return -12 }
        let dist = max(1, dist2D(tg.position, c.position))
        return -G.rad2deg(atan2(tg.position.y - c.position.y, dist))
    }
    /// deg < 0 = barrel up. Drives both the un-eased tilt (angle meter) and the
    /// input target so the engine's spinInput doesn't fight it.
    func setTilt(deg: Float) {
        c.tiltAngle = deg
        c.currentTiltTarget = deg / (deg > 0 ? G.minTiltAngle : G.maxTiltAngle)
    }
    private func slewToNearest(dt: Float) {
        guard let tg = nearestEnemy() else { return }
        let az = G.rad2deg(atan2(tg.position.x - c.position.x, tg.position.z - c.position.z))
        var d = (az - c.spinAngle).truncatingRemainder(dividingBy: 360)
        if d > 180 { d -= 360 }; if d < -180 { d += 360 }
        let s = 55 * dt
        c.spinAngle += abs(d) <= s ? d : (d > 0 ? s : -s)
    }
    /// Keep aim stable and gold in range every frame (runs after gc.update).
    func hold(dt: Float) {
        c.keyLeft = false; c.keyRight = false; c.keyUp = false; c.keyDown = false; c.currentSpinTarget = 0
        c.activeTilt += (c.tiltAngle - c.activeTilt) * min(1, 8 * dt)
        if c.cash > 3000 { c.cash = 3000 }
    }
    private func topUp() { if c.cash < 900 { c.cash = 3000 } }

    /// Autonomous aim → charge → fire → cool cycle for the free-for-all clips.
    func driveOpening(dt: Float, t: Double) {
        guard c.active, !c.dying else { return }
        slewToNearest(dt: dt)
        setTilt(deg: -40 + 7 * sin(Float(t) * 1.3))   // gentle elevation sweep → the angle meter lives
        hold(dt: dt)
        switch phase {
        case .aim:
            timer -= dt
            if timer <= 0 {
                c.weaponIndex = plan[pi % plan.count]; topUp()
                c.powerBarActive = true; c.powerAscending = true; c.powerLevel = 0; charge = 0
                phase = .charge
            }
        case .charge:
            charge = min(1, charge + dt / 2.8)    // ~2.2 s to release — close to the real game's rate
            c.powerLevel = charge                 // overwrite the engine ramp so it can't freeze/flip
            if charge >= 0.78 {
                topUp(); c.fire(at: c.powerLevel)              // launches, empties the bar, spends gold
                phase = .cool; timer = 1.6; pi += 1
            }
        case .cool:
            timer -= dt
            if timer <= 0 { phase = .aim; timer = 0.7 }
        }
    }
}

// MARK: - Recorder

enum TrailerRecorder {
    /// Record `seconds` of an 8-player bot match on the given map to a PNG
    /// sequence in `outDir`. `finale` runs the match to a win and holds on the
    /// engine's victory orbit.
    static func record(mapIndex: Int, outDir: String,
                       seconds: Double, fps: Int, size: CGSize,
                       warmup: Double, variety: Int, finale: Bool, showcase: Bool = false,
                       hud: Bool = true) {
        guard let device = MTLCreateSystemDefaultDevice() else {
            FileHandle.standardError.write("record: no Metal device (run in a GUI login session, not bare SSH)\n".data(using: .utf8)!)
            exit(3)
        }
        Audio.shared.sfxVolume = 0
        Audio.shared.musicVolume = 0

        // 8 players. Player 0 is a HUMAN "presenter" the recorder actively aims,
        // charges and fires so the HUD reads live (power bar fills, angle meter
        // moves, gold spends, lives count) — every HUD stat follows game.localHuman.
        // Players 1–7 are action-heavy bots; colors 0–3 are all present.
        var opts = GameOptions()
        opts.mapIndex = mapIndex
        opts.startingCashIndex = 6                 // 3000 gold → HUD gold scale is 0–3000
        opts.maxRespawns = 3                        // HUD "Lives" is 0–3 (native max)
        opts.hotSeatIndex = 1
        opts.treasureRespawn = true
        let botTypes = [2, 4, 3, 2, 4, 3, 2]        // aggressive/crazy heavy
        opts.players = [PlayerConfig(name: "CptBligh", colorIndex: 0, botType: 0)]   // the presenter
            + (1..<8).map { i in
                PlayerConfig(name: G.botNames[i % G.botNames.count], colorIndex: i % 4, botType: botTypes[(i - 1) % botTypes.count])
            }

        let gc = GameController(options: opts, viewSize: size)
        gc.players[0].cash = 3000
        let presenter = Presenter(cannon: gc.players[0], gc: gc)

        let renderer = SCNRenderer(device: device, options: nil)
        renderer.scene = gc.world.scene
        renderer.autoenablesDefaultLighting = false   // matches Snapshot.render
        let compositor = hud ? HUDCompositor(device: device, scene: gc.hud, size: size) : nil

        let dt = 1.0 / Double(fps)

        // Warmup (uncaptured): let bots settle and a shot get into the air.
        var warmed = 0.0
        while warmed < warmup { gc.update(frameDt: dt); warmed += dt }

        // Keep cannons out in the open so none are hidden by shrubbery. (The finale
        // relocates its winner separately, also foliage-aware.)
        if !finale { placeCannonsInOpen(gc) }

        // Finale: the human presenter wins. Move it onto flat, low, near-shore
        // ground so the cannon reads against water/sky (not a hillside), then
        // eliminate everyone else (forfeit → out regardless of respawns). The
        // engine flips to the success orbit and the HUD shows the live winner.
        if finale {
            relocateToFlatSpot(gc.players[0], gc: gc)
            for c in gc.players where c !== gc.players[0] && c.active { gc.kill(c, how: .forfeit) }
            var settle = 0.0
            while settle < 1.6 { gc.update(frameDt: dt); settle += dt }   // let death FX fade
        }

        if showcase {
            runShowcase(gc: gc, renderer: renderer, compositor: compositor, presenter: presenter,
                        outDir: outDir, seconds: seconds, fps: fps, size: size, dt: dt)
            return
        }

        let director = TrailerDirector(gc: gc, seconds: seconds, variety: variety, finale: finale)

        try? FileManager.default.createDirectory(atPath: outDir, withIntermediateDirectories: true)
        let total = Int(seconds * Double(fps))
        FileHandle.standardError.write("record: \(gc.world.map.name) → \(outDir) (\(total) frames @ \(fps)fps, \(Int(size.width))x\(Int(size.height))\(finale ? ", finale" : ""))\n".data(using: .utf8)!)

        for frame in 0..<total {
            autoreleasepool {   // drain per-frame image/texture temporaries (else OOM)
                let now = Double(frame) * dt
                gc.update(frameDt: dt)                 // physics, bots, turns, engine camera
                if !finale {
                    presenter.driveOpening(dt: Float(dt), t: now)   // live HUD: aim + charge + fire
                    keepPresenterAlive(gc)                          // never let the hero be eliminated
                }
                director.apply(at: now, dt: Float(dt)) // override camera for scripted shots
                if let sky = gc.world.skyActor {        // re-follow sky to the (overridden) camera
                    sky.simdPosition = gc.camera.node.simdPosition
                }
                captureFrame(renderer: renderer, compositor: compositor, gc: gc,
                             frame: frame, dt: dt, size: size, outDir: outDir)
            }
            if frame % 30 == 0 || frame == total - 1 {
                FileHandle.standardError.write("  frame \(frame + 1)/\(total)\r".data(using: .utf8)!)
            }
        }
        FileHandle.standardError.write("\nrecord: done (\(total) frames)\n".data(using: .utf8)!)
    }

    /// Keep the presenter (game.localHuman) in the match so the HUD never flips to
    /// the "You Lose!" / spectator flow mid-clip. Lives still count down 3→0 as it
    /// dies and respawns; only the eliminating 4th death is undone.
    private static func keepPresenterAlive(_ gc: GameController) {
        let p = gc.players[0]
        if !p.active && !gc.gameOver {
            p.active = true; p.dying = false; p.respawnsUsed = 0
            p.place(); p.toGround(); p.syncNode()
        }
        gc.deathWaitTimer = nil
        if gc.spectating { gc.spectating = false }
    }

    /// XZ positions of every prop / decoration node (shrubbery, palms, tikis, …) so
    /// the recorder can keep cannons clear of anything that would occlude them.
    private static func foliageXZ(_ gc: GameController) -> [SIMD2<Float>] {
        gc.world.propsRoot.childNodes.map { let p = $0.simdWorldPosition; return SIMD2(p.x, p.z) }
    }

    /// Move a cannon onto the flattest, lowest, near-shore land the map offers so it
    /// stands out against water/sky — and clear of shrubbery. Scans a grid.
    private static func relocateToFlatSpot(_ c: Cannon, gc: GameController) {
        let terrain = gc.world.terrain
        let ws = terrain.worldSize
        let obstacles = foliageXZ(gc)
        func score(_ x: Float, _ z: Float) -> Float? {
            let h = terrain.height(x: x, z: z)
            guard h > 2, h < 22 else { return nil }        // low land, above the surf
            for o in obstacles where simd_distance(o, SIMD2(x, z)) < 16 { return nil }   // clear of foliage
            var hs: [Float] = []
            for dx in stride(from: Float(-20), through: 20, by: 10) {
                for dz in stride(from: Float(-20), through: 20, by: 10) {
                    hs.append(terrain.height(x: x + dx, z: z + dz))
                }
            }
            if hs.contains(where: { $0 <= 0 }) { return nil }   // don't straddle the water edge
            let avg = hs.reduce(0, +) / Float(hs.count)
            let variance = hs.map { ($0 - avg) * ($0 - avg) }.reduce(0, +) / Float(hs.count)
            return sqrt(variance) + 0.25 * h
        }
        var best: (SIMD3<Float>, Float)?
        let lo = ws * 0.12, hi = ws * 0.88, step = ws / 32
        var x = lo
        while x <= hi {
            var z = lo
            while z <= hi {
                if let s = score(x, z), best == nil || s < best!.1 {
                    best = (SIMD3(x, terrain.height(x: x, z: z), z), s)
                }
                z += step
            }
            x += step
        }
        if let (pos, _) = best {
            c.position = pos
            c.toGround(); c.syncNode()
        }
    }

    /// Spread all cannons onto open, flat, foliage-free land so none are hidden by
    /// shrubbery. Keeps a minimum separation so they read as distinct in wide shots.
    private static func placeCannonsInOpen(_ gc: GameController) {
        let terrain = gc.world.terrain
        let ws = terrain.worldSize
        let obstacles = foliageXZ(gc)
        func openScore(_ x: Float, _ z: Float) -> Float? {
            guard terrain.height(x: x, z: z) > 8 else { return nil }          // solid land
            for o in obstacles where simd_distance(o, SIMD2(x, z)) < 15 { return nil }   // clear of foliage/props
            var hs: [Float] = []
            for dx in stride(from: Float(-14), through: 14, by: 14) {
                for dz in stride(from: Float(-14), through: 14, by: 14) {
                    let h = terrain.height(x: x + dx, z: z + dz)
                    if h <= 2 { return nil }                                  // not at the water edge
                    hs.append(h)
                }
            }
            let avg = hs.reduce(0, +) / Float(hs.count)
            return sqrt(hs.map { ($0 - avg) * ($0 - avg) }.reduce(0, +) / Float(hs.count))
        }
        var placed: [SIMD2<Float>] = []
        for c in gc.players {
            var best: (SIMD3<Float>, Float)?
            for _ in 0..<600 {
                let x = Float.random(in: ws * 0.12...ws * 0.88)
                let z = Float.random(in: ws * 0.12...ws * 0.88)
                if placed.contains(where: { simd_distance($0, SIMD2(x, z)) < 46 }) { continue }
                guard let s = openScore(x, z) else { continue }
                if best == nil || s < best!.1 { best = (SIMD3(x, terrain.height(x: x, z: z), z), s) }
                if s < 1.2 { break }
            }
            if let (pos, _) = best {
                c.position = pos; c.toGround(); c.syncNode()
                placed.append(SIMD2(pos.x, pos.z))
            }
        }
    }

    /// Weapon showcase: the human presenter fires each of the 12 weapon types in
    /// turn (one per "beat"), charging its power bar each time so the HUD reads
    /// live, with a chase cam on the shot. Every weapon gets screen time. The two
    /// instant weapons (Tower, Teleport) reproduce their in-game effects.
    private static func runShowcase(gc: GameController, renderer: SCNRenderer, compositor: HUDCompositor?,
                                    presenter: Presenter, outDir: String,
                                    seconds: Double, fps: Int, size: CGSize, dt: Double) {
        try? FileManager.default.createDirectory(atPath: outDir, withIntermediateDirectories: true)
        let weapons = WeaponType.allCases                 // 12, in HUD order
        let beatDur = seconds / Double(weapons.count)
        let total = Int(seconds * Double(fps))
        FileHandle.standardError.write("record: \(gc.world.map.name) → \(outDir) (\(total) frames, WEAPON SHOWCASE, \(weapons.count) beats)\n".data(using: .utf8)!)

        // Give each of the 12 weapons a distinct SHOT TYPE so the showcase never
        // repeats a framing: behind-chase, side-track, top-down, low ground-level,
        // front-approach (shot flies at camera), and wide arc.
        let shotType: [ShowShot] = [.behind, .side, .top, .low, .front, .wide,
                                    .side, .behind, .low, .top, .front, .wide]

        let cam = gc.camera.node
        let p = presenter.c
        var beat = -1, fired = false, chaseInit = false
        var charge: Float = 0
        var proj: Projectile?

        for frame in 0..<total {
            let now = Double(frame) * dt
            let bi = min(weapons.count - 1, Int(now / beatDur))
            let bt = now - Double(bi) * beatDur
            let w = weapons[bi]
            let instant = (w == .tower || w == .teleport)
            if bi != beat {
                beat = bi; fired = false; chaseInit = false; proj = nil; charge = 0
                // set up the beat: select weapon, aim, prime the power bar
                p.weaponIndex = w.rawValue
                p.cash = 3000
                presenter.snapToNearest()
                presenter.setTilt(deg: w == .dumbfire ? presenter.straightTiltDeg() : -38)
                p.activeTilt = p.tiltAngle
                p.powerBarActive = !instant
                p.powerAscending = true; p.powerLevel = 0
            }
            autoreleasepool {   // drain per-frame image/texture temporaries (else OOM)
                gc.update(frameDt: dt)
                keepPresenterAlive(gc)
                presenter.hold(dt: Float(dt))    // keep aim, ease activeTilt, clamp gold

                // Smooth charge with NO plateau, at roughly the real game's rate; the
                // shot fires the instant the bar reaches the release level.
                if !fired {
                    if !instant {
                        charge = min(1, charge + Float(dt) / 2.3)     // ~1.8 s to release
                        p.powerLevel = charge                         // overwrite (ignore engine ramp)
                        if charge >= 0.8 { proj = firePresenterWeapon(gc, presenter, w); fired = true }
                    } else if bt >= 1.0 {
                        proj = firePresenterWeapon(gc, presenter, w); fired = true
                    }
                }

                // While the bar charges, hold on the firing cannon (bar-fill stays in
                // sync with a cannon you can see); once the shot launches, cut to the
                // per-weapon flight framing.
                showcaseCamera(shotType[bi], charging: !fired, beat: bi, cam: cam, cannon: p,
                               proj: proj, bt: bt, dt: Float(dt), chaseInit: &chaseInit, world: gc.world)

                if let sky = gc.world.skyActor { sky.simdPosition = cam.simdPosition }
                captureFrame(renderer: renderer, compositor: compositor, gc: gc,
                             frame: frame, dt: dt, size: size, outDir: outDir)
            }
            if frame % 30 == 0 || frame == total - 1 {
                FileHandle.standardError.write("  frame \(frame + 1)/\(total) [\(w.displayName)]\r".data(using: .utf8)!)
            }
        }
        FileHandle.standardError.write("\nrecord: done (\(total) frames)\n".data(using: .utf8)!)
    }

    /// Fire the presenter's current weapon. Projectile weapons launch via
    /// Cannon.fire (empties the charged bar, spends gold); Tower/Teleport reproduce
    /// their instant effects.
    private static func firePresenterWeapon(_ gc: GameController, _ presenter: Presenter, _ w: WeaponType) -> Projectile? {
        let c = presenter.c
        c.cash = 3000
        switch w {
        case .tower:
            if !gc.world.objectAbove(x: c.position.x, z: c.position.z) {
                gc.world.terrain.molehill(x: c.position.x, z: c.position.z, height: 40, radius: 30)
                Projectile.quakeRaysFX(x: c.position.x, z: c.position.z, radius: 30, in: gc.world)
                Audio.shared.play("quake")
            }
            c.powerBarActive = false
            return nil
        case .teleport:
            c.playerTeleport()
            c.powerBarActive = false
            return nil
        default:
            c.fire(at: max(0.5, c.powerLevel))
            return gc.projectiles.last
        }
    }

    /// Position the camera for one showcase weapon. While `charging`, hold a clear
    /// view of the firing cannon so the power-bar fill reads on a visible cannon;
    /// once the shot is airborne, use the weapon's distinct flight framing.
    private static func showcaseCamera(_ type: ShowShot, charging: Bool, beat: Int,
                                       cam: SCNNode, cannon: Cannon, proj: Projectile?,
                                       bt: Double, dt: Float, chaseInit: inout Bool, world: World) {
        let c = cannon.position
        let yaw = G.deg2rad(cannon.spinAngle)
        let fdir = SIMD3<Float>(sin(yaw), 0, cos(yaw))            // horizontal aim direction
        var side = simd_cross(fdir, SIMD3<Float>(0, 1, 0))
        side = simd_length(side) > 1e-4 ? simd_normalize(side) : SIMD3<Float>(1, 0, 0)

        // Charge phase: a clear 3/4 view of the cannon (varied per beat), looking
        // just ahead of the barrel so the recoil + muzzle read when it fires.
        if charging {
            let dir: Float = beat % 2 == 0 ? 1 : -1
            let back = 16 + Float(beat % 3) * 3
            let sideOff = 9 + Float(beat % 2) * 4
            let up = 10 + Float(beat % 3) * 2
            var pos = c - fdir * back + side * dir * sideOff + SIMD3<Float>(0, up, 0)
            let floorY = world.terrain.height(x: pos.x, z: pos.z) + 3
            if pos.y < floorY { pos.y = floorY }
            camAim(cam, from: pos, to: c + fdir * 4 + SIMD3<Float>(0, 4, 0))
            return
        }

        // Flight phase: the weapon's distinct shot type tracks the shot.
        let live = proj?.alive == true
        let subject = live ? proj!.position : c + fdir * 12 + SIMD3<Float>(0, 8, 0)
        let ws = world.terrain.worldSize
        let center = world.center
        var pos: SIMD3<Float>
        var look = subject
        var smooth = false
        switch type {
        case .behind:
            let pd = live && simd_length(proj!.traj) > 0.1 ? simd_normalize(proj!.traj) : fdir
            pos = subject - pd * 15 + SIMD3<Float>(0, 7, 0); smooth = true
        case .side:  pos = c + side * 40 + fdir * 4 + SIMD3<Float>(0, 17, 0)
        case .top:   pos = subject + SIMD3<Float>(0, 50, 6)
        case .low:   pos = c + fdir * 34 + side * 6 + SIMD3<Float>(0, 6, 0)
        case .front: pos = c + fdir * 62 + SIMD3<Float>(0, 15, 0)
        case .wide:
            let a = G.deg2rad(30 + Float(bt) * 9)
            pos = center + SIMD3<Float>(sin(a) * max(300, ws * 0.5), max(150, ws * 0.3), cos(a) * max(300, ws * 0.5))
            look = c + SIMD3<Float>(0, 10, 0)
        }
        let floorY = world.terrain.height(x: pos.x, z: pos.z) + 3   // keep the camera out of the ground
        if pos.y < floorY { pos.y = floorY }
        if smooth {
            if !chaseInit { cam.simdPosition = pos; chaseInit = true }
            else { cam.simdPosition += (pos - cam.simdPosition) * min(1, 7 * dt) }
            camLook(cam, to: look)
        } else {
            camAim(cam, from: pos, to: look)
        }
    }

    /// Render one frame (3D + optional HUD overlay) and write it as PNG.
    private static func captureFrame(renderer: SCNRenderer, compositor: HUDCompositor?, gc: GameController,
                                     frame: Int, dt: Double, size: CGSize, outDir: String) {
        renderer.pointOfView = gc.camera.node
        let t = TimeInterval(frame) * dt
        let scn = renderer.snapshot(atTime: t, with: size, antialiasingMode: .multisampling4X)
        let out: NSImage
        if let compositor, let hud = compositor.image(at: t) {
            out = composite(base: scn, over: hud, size: size)
        } else {
            out = scn
        }
        writePNG(out, to: "\(outDir)/frame_\(String(format: "%05d", frame)).png")
    }

    /// Draw the HUD (transparent bg) over the 3D frame.
    private static func composite(base: NSImage, over hud: NSImage, size: CGSize) -> NSImage {
        let out = NSImage(size: size)
        out.lockFocus()
        let r = NSRect(origin: .zero, size: size)
        base.draw(in: r, from: .zero, operation: .copy, fraction: 1)
        hud.draw(in: r, from: .zero, operation: .sourceOver, fraction: 1)
        out.unlockFocus()
        return out
    }

    /// NSImage → PNG, the exact path from Snapshot.swift:120-125.
    private static func writePNG(_ img: NSImage, to path: String) {
        guard let tiff = img.tiffRepresentation,
              let rep = NSBitmapImageRep(data: tiff),
              let png = rep.representation(using: .png, properties: [:]) else {
            FileHandle.standardError.write("record: PNG encode failed for \(path)\n".data(using: .utf8)!)
            return
        }
        try? png.write(to: URL(fileURLWithPath: path))
    }
}
