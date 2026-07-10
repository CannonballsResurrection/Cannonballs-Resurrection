import Foundation
import SceneKit

/// Camera modes per SPEC §3 table.
final class CameraController {
    enum Mode: Int, CaseIterable {
        case cannon = 0, shot, medium, high, barrel
        // views the player can pick — the originals 0..4 (Camera.java CameraViews = 5)
        case success   // view 6: end-of-game orbit around the winner (Camera.java:97)
        case spectator // view 99: follow whoever is playing (Camera.java:284)
        var label: String {
            switch self {
            case .cannon: return "Cannon"
            case .shot: return "Shot"
            case .medium: return "Medium"
            case .high: return "High"
            case .barrel: return "Barrel"
            case .success: return "Success"
            case .spectator: return "Spectator"
            }
        }
        /// The cycle/menu set (cycleCamera wraps at CameraViews=5; 6/99 are set
        /// only by the game-over and death flows).
        static let selectable: [Mode] = [.cannon, .shot, .medium, .high, .barrel]
    }

    let node = SCNNode()
    var mode: Mode = .shot
    weak var game: GameController?

    private var shockAmplitude: Float = 0
    private var chaseSpeed: Float = 0.1
    private var initialized = false
    private var successAngle: Float = 0   // Camera.SuccessAngle (degrees)

    init() {
        let cam = SCNCamera()
        cam.zFar = 4000
        cam.fieldOfView = 55
        node.camera = cam
    }

    private var windArrow: SCNNode?

    func attach(to game: GameController) {
        self.game = game
        game.world.scene.rootNode.addChildNode(node)

        // Wind indicator: the golden ornate arrow (arrowo.png) on a flat quad lying
        // horizontal at the lower-right of the view, spun around world-Y by the wind
        // direction (ArrowGroup setAbsoluteOrientation(0,1,0, WindDirection)). Child of
        // the camera for position, world-oriented for direction → true 3D weathervane.
        // the REAL decoded ARROW mesh (fixed by the solved-format exporter; the old
        // OBJ route mis-assigned its material and rendered black)
        if let actor = SkinnedModel.load("ARROW") {
            actor.root.enumerateHierarchy { n, _ in
                // renderingOrder is per-node (not inherited): set it on every mesh
                // node, above WorldDressing's 1000, so with depth reads off the
                // arrow always draws in front of the world.
                n.renderingOrder = 2000
                guard let geo = n.geometry else { return }
                for m in geo.materials {
                    m.lightingModel = .constant
                    m.diffuse.magnificationFilter = .nearest
                    m.writesToDepthBuffer = false
                    m.readsFromDepthBuffer = false
                }
            }
            // arrow mesh spans ~7 units; scale to HUD weathervane size
            actor.root.simdScale = SIMD3(repeating: 0.78)   // a bit bigger than the 0.62 first pass
            // decoded WT meshes rest facing -Z (the barrel gets the same PI yaw in
            // Cannon.buildParts); face +Z so R_y(WindDirection) points the tip along
            // the wind drift, matching the original engine's setAbsoluteOrientation.
            actor.root.simdOrientation = simd_quatf(angle: .pi, axis: SIMD3(0, 1, 0))
            let holder = SCNNode()
            holder.addChildNode(actor.root)
            holder.simdPosition = SIMD3(2.6, -1.4, -5)         // lower-right, in front (SceneKit -Z)
            node.addChildNode(holder)
            windArrow = holder
        } else if let gold = Assets.image("MODELS/ARROW/textures/arrowo_keyed.png") {
            let plane = SCNPlane(width: 3.25, height: 1.625)   // 256x128 = 2:1, sized to match the mesh arrow
            let m = SCNMaterial()
            m.diffuse.contents = gold
            m.diffuse.magnificationFilter = .nearest
            m.lightingModel = .constant
            m.isDoubleSided = true
            m.transparencyMode = .aOne                         // black bg keyed to alpha
            m.writesToDepthBuffer = false
            m.readsFromDepthBuffer = false
            plane.materials = [m]
            let card = SCNNode(geometry: plane)
            card.eulerAngles.x = -.pi / 2                      // lie flat (weathervane)
            card.renderingOrder = 2000                         // always in front (see mesh path)
            let holder = SCNNode()
            holder.addChildNode(card)
            holder.simdPosition = SIMD3(2.6, -1.4, -5)         // lower-right, in front (SceneKit -Z)
            node.addChildNode(holder)
            windArrow = holder
        }
    }

    func cycle() {
        guard mode != .success && mode != .spectator else { return }  // no cycling once dead/won
        mode = Mode(rawValue: (mode.rawValue + 1) % Mode.selectable.count) ?? .cannon
        game?.hud.flashMessage("Camera: \(mode.label)")
    }

    /// setCamera(6, ...) — the end-of-game orbit. applyCameraView case 6 resets
    /// SuccessAngle to 0 (Camera.java:97-99).
    func setSuccessMode() {
        mode = .success
        successAngle = 0
    }

    /// setSpectatorCamera (Camera.java:278-290): announces itself and switches to
    /// view 99. The HUD side (bar removal, "Spectator Mode" labels) is
    /// HUDScene.enterSpectatorMode().
    func setSpectatorMode() {
        mode = .spectator
        game?.hud.flashMessage("Activating Spectator Camera")   // Camera.java:279
    }

    func shock(at pos: SIMD3<Float>, radius: Float) {
        let d = simd_distance(node.simdPosition, pos)
        if d < radius {
            shockAmplitude = max(shockAmplitude, (radius - d) * 100 / radius / 1000 * 8)
        }
    }

    func update(dt: Float) {
        guard let game else { return }
        let focus = game.cameraFocusCannon ?? game.players.first!
        let terrain = game.world.terrain

        var desired: SIMD3<Float>
        var lookAt: SIMD3<Float>

        // live shot chase (Shot mode; spectator view 99 is updateShotCamera on the
        // current player — Camera.java:379-381 — so it chases shots the same way)
        if mode == .shot || mode == .spectator, let proj = game.projectiles.first(where: { $0.alive && $0.owner.firedOnTurn }) ?? game.projectiles.first(where: { $0.alive }) {
            chaseSpeed = min(7.0, chaseSpeed + dt * 3)
            let back = simd_length(proj.traj) > 0.1 ? simd_normalize(proj.traj) : SIMD3<Float>(0, 0, 1)
            desired = proj.position - back * 10 + SIMD3<Float>(0, 4, 0)
            lookAt = proj.position
        } else {
            chaseSpeed = 0.1
            let yaw = G.deg2rad(focus.spinAngle)
            func rot(_ v: SIMD3<Float>) -> SIMD3<Float> {
                SIMD3(v.x * cos(yaw) + v.z * sin(yaw), v.y, -v.x * sin(yaw) + v.z * cos(yaw))
            }
            switch mode {
            case .cannon, .shot, .spectator:
                desired = focus.position + rot(SIMD3(0, 6, -20))
                lookAt = focus.position + rot(SIMD3(0, 4, 30))
            case .medium:
                desired = focus.position + rot(SIMD3(0, 50, -50))
                lookAt = focus.position
            case .high:
                desired = focus.position + rot(SIMD3(0, 100, -100))
                lookAt = focus.position
            case .barrel:
                desired = focus.muzzlePosition + SIMD3<Float>(0, 1.5, 0)
                lookAt = focus.muzzlePosition + focus.fireDirection * 40
            case .success:
                // updateSuccessCamera (Camera.java:292-342): offset (0,10,-60)
                // swung around Y by SuccessAngle (+10 deg/s), aimed at the first
                // Active cannon (the winner). Chase (9.5·dt) and the terrain+4 /
                // min-8 floor reuse the shared clamps below, same constants.
                let winner = game.players.first { $0.active } ?? focus
                let a = G.deg2rad(successAngle)
                successAngle += dt * 10
                // VEC3D.rotateY: x' = x·cos + z·sin, z' = -x·sin + z·cos
                let off = SIMD3<Float>(0, 10, -60)
                desired = winner.position + SIMD3(off.x * cos(a) + off.z * sin(a), off.y,
                                                  -off.x * sin(a) + off.z * cos(a))
                lookAt = winner.position
            }
        }

        // floor clamps
        let minY = max(terrain.height(x: desired.x, z: desired.z) + 4, 8)
        if desired.y < minY && mode != .barrel { desired.y = minY }

        if !initialized {
            node.simdPosition = desired
            initialized = true
        } else {
            let k = min(1, (mode == .shot ? max(chaseSpeed, 9.5) : 9.5) * dt)
            node.simdPosition += (desired - node.simdPosition) * k
        }

        // keep the interpolated camera out of the terrain too
        if mode != .barrel {
            let floorY = max(terrain.height(x: node.simdPosition.x, z: node.simdPosition.z) + 4, 8)
            if node.simdPosition.y < floorY { node.simdPosition.y = floorY }
        }

        // shock rattle
        if shockAmplitude > 0.0005 {
            node.simdPosition += SIMD3(Float.random(in: -1...1), Float.random(in: -1...1), Float.random(in: -1...1)) * shockAmplitude
            shockAmplitude *= pow(0.9, dt * 60)
        } else { shockAmplitude = 0 }

        // Stable orientation: force world-up so the view never rolls sideways or
        // flips upside down. Guard the degenerate near-vertical case (gimbal).
        var fwd = lookAt - node.simdPosition
        if simd_length(fwd) < 1e-4 { fwd = SIMD3(0, 0, 1) }
        fwd = simd_normalize(fwd)
        var up = SIMD3<Float>(0, 1, 0)
        if abs(simd_dot(fwd, up)) > 0.995 { up = SIMD3<Float>(0, 0, 1) }
        node.look(at: SCNVector3(lookAt), up: SCNVector3(up), localFront: SCNVector3(0, 0, -1))

        // Point the wind arrow in the world wind direction, regardless of camera yaw
        // (setAbsoluteOrientation(0,1,0, WindDirection)). Absolute world orientation.
        if let windArrow {
            let dir = G.deg2rad(game.windDirection)
            windArrow.simdWorldOrientation = simd_quatf(angle: dir, axis: SIMD3(0, 1, 0))
        }
    }

    /// Set a specific camera mode (used by the Camera menu). Inert once the
    /// spectator/success cameras own the view (the original removes the Camera
    /// menu in spectator mode and stops UI processing in the end state).
    func setMode(_ m: Mode) {
        guard mode != .success && mode != .spectator else { return }
        mode = m
        game?.hud.flashMessage("Camera: \(m.label)")
    }
}
