import AppKit
import SceneKit
import SpriteKit

/// SCNView subclass that forwards keyboard + mouse to closures.
final class KeyView: SCNView {
    var onKeyDown: ((NSEvent) -> Bool)?
    var onKeyUp: ((NSEvent) -> Bool)?
    var onClick: ((CGPoint) -> Bool)?    // point in overlay scene coords
    var onMove: ((CGPoint) -> Void)?

    override var acceptsFirstResponder: Bool { true }

    // The original uses a cutlass pointer (MENUS/POINTER); hotspot at the blade tip.
    static let cutlass: NSCursor = {
        // original: 64x64 POINTER drawn at native size on the 800x600 screen
        let img = Assets.image("HUDART/pointer.png") ?? NSImage(size: NSSize(width: 64, height: 64))
        img.size = NSSize(width: 64, height: 64)
        return NSCursor(image: img, hotSpot: NSPoint(x: 2, y: 2))
    }()
    override func resetCursorRects() { addCursorRect(bounds, cursor: KeyView.cutlass) }

    override func keyDown(with event: NSEvent) {
        if onKeyDown?(event) != true { super.keyDown(with: event) }
    }
    override func keyUp(with event: NSEvent) {
        if onKeyUp?(event) != true { super.keyUp(with: event) }
    }
    override func mouseDown(with event: NSEvent) {
        if let overlay = overlaySKScene {
            let p = convert(event.locationInWindow, from: nil)
            let sp = overlay.convertPoint(fromView: p)
            if onClick?(sp) == true { return }
        }
        super.mouseDown(with: event)
    }
    override func mouseMoved(with event: NSEvent) {
        if let overlay = overlaySKScene {
            let p = convert(event.locationInWindow, from: nil)
            onMove?(overlay.convertPoint(fromView: p))
        }
        super.mouseMoved(with: event)
    }
}

final class GameViewController: NSViewController, SCNSceneRendererDelegate {
    let scnView = KeyView(frame: NSRect(x: 0, y: 0, width: 1120, height: 840),
                          options: [SCNView.Option.preferredRenderingAPI.rawValue: SCNRenderingAPI.metal.rawValue])

    enum Mode { case menu, game }
    var mode: Mode = .menu

    var game: GameController?
    var menuScene: MenuScene?

    // menu backdrop
    private var backdropWorld: World?
    private var backdropCam: SCNNode?
    private var orbitAngle: Float = 0
    private var lastTime: TimeInterval?
    private var trackingArea: NSTrackingArea?

    // All scene/HUD mutation must happen on SceneKit's render thread (inside
    // renderer(updateAtTime:)) to avoid data races with the overlay SKScene render,
    // which SpriteKit does on that same background queue. Input and menu actions
    // arrive on the main thread, so they enqueue work here to run on the render thread.
    private let cmdLock = NSLock()
    private var pendingCommands: [() -> Void] = []

    func runOnRenderThread(_ block: @escaping () -> Void) {
        cmdLock.lock(); pendingCommands.append(block); cmdLock.unlock()
    }
    /// Convenience: run a command against the live game (no-op if not in a game).
    func runGameCommand(_ block: @escaping (GameController) -> Void) {
        runOnRenderThread { [weak self] in if let g = self?.game { block(g) } }
    }
    private func drainCommands() {
        cmdLock.lock(); let cmds = pendingCommands; pendingCommands.removeAll(); cmdLock.unlock()
        for c in cmds { c() }
    }

    override func loadView() {
        view = scnView
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        scnView.delegate = self
        scnView.isPlaying = true
        // Render every frame, not just on scene change / user input. Without this the
        // render loop (and thus the game update below) stalls whenever the player isn't
        // touching the keyboard/mouse — e.g. it would freeze on a bot's turn.
        scnView.rendersContinuously = true
        scnView.antialiasingMode = .none    // hard 2002 edges (single faithful view)
        scnView.backgroundColor = .black
        scnView.autoenablesDefaultLighting = false

        // Input arrives on the main thread; defer handling to the render thread so it
        // never mutates the scene/HUD concurrently with rendering. We consume the event
        // (return true) since the game owns these keys/clicks.
        scnView.onKeyDown = { [weak self] e in self?.runOnRenderThread { _ = self?.handleKeyDown(e) }; return true }
        scnView.onKeyUp = { [weak self] e in self?.runOnRenderThread { _ = self?.handleKeyUp(e) }; return true }
        scnView.onClick = { [weak self] p in self?.runOnRenderThread { _ = self?.handleClick(p) }; return true }
        scnView.onMove = { [weak self] p in self?.runOnRenderThread { self?.handleMove(p) } }

        showMenu()
    }

    override func viewDidLayout() {
        super.viewDidLayout()
        if let t = trackingArea { scnView.removeTrackingArea(t) }
        let t = NSTrackingArea(rect: scnView.bounds, options: [.mouseMoved, .activeInKeyWindow], owner: scnView, userInfo: nil)
        scnView.addTrackingArea(t)
        trackingArea = t
        // SpriteKit relayout must run on the render thread (see runOnRenderThread).
        let size = scnView.bounds.size
        runOnRenderThread { [weak self] in
            self?.menuScene?.relayout(size: size)
            self?.game?.hud.relayout(size: size)
        }
    }

    // MARK: - Menu

    func showMenu() {
        mode = .menu
        game?.tearDown()
        game = nil
        Audio.shared.stopLoops()   // ocean / aiming loops end with the game
        let mapIdx = Int.random(in: 0..<max(1, MapCatalog.maps.count))
        let world = World(map: MapCatalog.maps[mapIdx])
        backdropWorld = world
        let camNode = SCNNode()
        let cam = SCNCamera(); cam.zFar = 4000; cam.fieldOfView = 55
        camNode.camera = cam
        world.scene.rootNode.addChildNode(camNode)
        backdropCam = camNode
        scnView.scene = world.scene
        scnView.pointOfView = camNode

        let m = MenuScene(size: scnView.bounds.size)
        m.onStart = { [weak self] options in
            guard let self else { return }
            // original iris-wipe transition (Menu_Manager dissolve)
            IrisTransition.run(over: self.scnView) { self.startGame(options) }
        }
        m.onCannonPreview = { [weak self] color in self?.setMenuCannonPreview(color) }
        scnView.overlaySKScene = m
        menuScene = m
        Audio.shared.playMusic(.title)
    }

    // The lobby's spinning tinted cannon. The ORIGINAL renders it camera-space
    // over the static lobby background (Menu_Lobby_Screen.java:400-416): barrel
    // + stand only, player-tinted, yaw -135, barrel pitch -20, spinning 50 deg/s.
    // The scene is built ONCE per lobby and swapped on the MAIN thread; color
    // changes only retint the barrel material (rebuilding/swapping scenes from
    // the render queue crashed SceneKit's frame prep).
    private var previewScene: SCNScene?
    private var previewBarrel: SCNNode?

    func setMenuCannonPreview(_ colorIndex: Int?) {
        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            guard let colorIndex else {
                self.previewScene = nil
                self.previewBarrel = nil
                // Only restore the menu backdrop while still in the menu — this block
                // runs async, and startGame() may already have installed the game
                // scene (restoring here was stomping the freshly started game).
                if self.mode == .menu, let world = self.backdropWorld, let cam = self.backdropCam {
                    self.scnView.scene = world.scene
                    self.scnView.pointOfView = cam
                }
                return
            }
            guard self.mode == .menu else { return }
            if let scene = self.previewScene {
                // color change: retint only
                if let skin = Cannon.tintedSkin(colorIndex) {
                    self.previewBarrel?.enumerateHierarchy { n, _ in
                        n.geometry?.firstMaterial?.diffuse.contents = skin
                    }
                }
                if self.scnView.scene !== scene { self.scnView.scene = scene }
                return
            }
            let scene = SCNScene()
            // ref frame_002: the preview cannon floats OVER the lobby artwork,
            // not in a dark window — use the JOIN art as the scene background so
            // the SKScene's punched circle is seamless
            scene.background.contents = Assets.image("MENUS/JOIN.png") ?? NSColor.black
            let (statics, barrel) = Cannon.buildParts(colorIndex: colorIndex)
            let group = SCNNode()
            for child in statics.childNodes where child.name != "stone" {
                group.addChildNode(child)
            }
            if let barrel {
                // original: CannonBarrelActor.Model.setOrientation(1,0,0,-20) — the
                // barrel rides 20 deg above horizontal while the group spins
                let tilt = SCNNode()
                tilt.eulerAngles.x = 20 * .pi / 180
                tilt.addChildNode(barrel.root)
                group.addChildNode(tilt)
                self.previewBarrel = barrel.root
            }
            group.eulerAngles.y = -135 * .pi / 180
            group.runAction(.repeatForever(.rotateBy(x: 0, y: .pi * 2 * (50.0 / 360.0), z: 0, duration: 1)))
            scene.rootNode.addChildNode(group)
            let camNode = SCNNode()
            let cam = SCNCamera(); cam.zFar = 200; cam.fieldOfView = 60; camNode.camera = cam
            // frame the cannon at the original's screen spot (~612,206 of 800x600)
            camNode.position = SCNVector3(-9.8, -4.4, 24)
            scene.rootNode.addChildNode(camNode)
            let amb = SCNNode(); amb.light = SCNLight(); amb.light!.type = .ambient
            amb.light!.intensity = 700
            scene.rootNode.addChildNode(amb)
            self.scnView.scene = scene
            self.scnView.pointOfView = camNode
            self.previewScene = scene
        }
    }

    func startGame(_ options: GameOptions) {
        mode = .game
        setMenuCannonPreview(nil)
        menuScene = nil
        // Drop the menu backdrop so no stale async block can ever swap it back in.
        backdropWorld = nil
        backdropCam = nil
        let g = GameController(options: options, viewSize: scnView.bounds.size)
        g.onExit = { [weak self] in
            guard let self else { return }
            IrisTransition.run(over: self.scnView) { self.showMenu() }
        }
        game = g
        scnView.scene = g.world.scene
        scnView.pointOfView = g.camera.node
        g.hud.scnView = scnView
        scnView.overlaySKScene = g.hud
        Audio.shared.playMusic(.track(g.world.map.musicTrack))
        Audio.shared.startLoop("ocean", volume: 0.35)   // island ambience (Island.SoundOcean loop)
    }

    // MARK: - Frame driver

    // Called by SceneKit on its render thread immediately before it renders the frame.
    // All scene/HUD mutation happens here (and in drained input commands) so it is
    // sequential with rendering — never concurrent with the overlay SKScene render.
    func renderer(_ renderer: SCNSceneRenderer, updateAtTime time: TimeInterval) {
        let dt: TimeInterval
        if let last = lastTime { dt = min(time - last, 0.25) } else { dt = 1.0 / 60.0 }
        lastTime = time

        drainCommands()   // apply queued input / menu actions on this (render) thread

        switch mode {
        case .menu:
            orbitAngle += Float(dt) * 0.08
            if let world = backdropWorld, let cam = backdropCam {
                let c = world.center
                let r: Float = 380
                cam.position = SCNVector3(CGFloat(c.x + cos(orbitAngle) * r), 170,
                                          CGFloat(c.z + sin(orbitAngle) * r))
                cam.look(at: SCNVector3(CGFloat(c.x), 12, CGFloat(c.z)))
            }
        case .game:
            game?.update(frameDt: dt)
            if softwareRender, let g = game, !rasterInFlight {
                // Extract on the render thread (thread-safe read of the live scene),
                // then do the heavy pixel fill on a background queue so the HUD/input
                // stay at full framerate. One frame in flight at a time.
                let snap = SceneRasterizer.snapshot(scene: g.world.scene, pov: g.camera.node,
                                                    size: CGSize(width: 640, height: 480))
                let s = SceneRasterizer.settings(for: g.world.map)
                rasterInFlight = true
                rasterQueue.async { [weak self] in
                    let raster = SceneRasterizer.rasterize(snap, settings: s)
                    let img = raster.nsImage(background: s.skyBottom, gamma: s.gamma)
                    self?.runOnRenderThread {
                        if self?.softwareRender == true { self?.game?.hud.setRasterImage(img) }
                        self?.rasterInFlight = false
                    }
                }
            }
        }
    }

    // Software-rasterizer mode (the reimplemented DX7 pipeline), toggled with 'R'.
    var softwareRender = false
    private var rasterInFlight = false
    private let rasterQueue = DispatchQueue(label: "cannonballs.softraster", qos: .userInitiated)
    func toggleSoftwareRender() {
        softwareRender.toggle()
        if !softwareRender { game?.hud.setRasterImage(nil) }
        game?.hud.flashMessage(softwareRender ? "Software Rasterizer: ON" : "Software Rasterizer: OFF")
    }

    // MARK: - Input routing

    private func handleKeyDown(_ e: NSEvent) -> Bool {
        if mode == .game && e.keyCode == 15 && game?.hud.chatting != true {
            toggleSoftwareRender(); return true   // 'R' toggles the software rasterizer
        }
        switch mode {
        case .menu: return menuScene?.keyDown(e) ?? false
        case .game: return game?.keyDown(e) ?? false
        }
    }
    private func handleKeyUp(_ e: NSEvent) -> Bool {
        switch mode {
        case .menu: return false
        case .game: return game?.keyUp(e) ?? false
        }
    }
    private func handleClick(_ p: CGPoint) -> Bool {
        switch mode {
        case .menu: return menuScene?.click(at: p) ?? false
        case .game: return game?.click(at: p) ?? false
        }
    }
    private func handleMove(_ p: CGPoint) {
        switch mode {
        case .menu: menuScene?.hover(at: p)
        case .game: game?.hud.hover(at: p)
        }
    }
}
