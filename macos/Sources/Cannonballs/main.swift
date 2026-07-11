import AppKit
import SceneKit

// MARK: - CLI: --snapshot <MAP> <out.png> [--cam x,y,z]

let args = CommandLine.arguments
if let idx = args.firstIndex(of: "--snapshot"), args.count > idx + 2 {
    let mapName = args[idx + 1]
    let outPath = args[idx + 2]
    guard let map = MapCatalog.byName(mapName) else {
        FileHandle.standardError.write("Unknown map '\(mapName)'. Maps: \(MapCatalog.maps.map(\.name).joined(separator: ", "))\n".data(using: .utf8)!)
        exit(1)
    }
    func vec(_ flag: String) -> SCNVector3? {
        guard let i = args.firstIndex(of: flag), args.count > i + 1 else { return nil }
        let p = args[i + 1].split(separator: ",").compactMap { Double($0) }
        return p.count == 3 ? SCNVector3(p[0], p[1], p[2]) : nil
    }
    Snapshot.render(map: map, to: outPath, camPos: vec("--cam"), camLook: vec("--look"))
    exit(0)
}

// MARK: - CLI: --model <NAME> <out.png> — render one model on a neutral backdrop
if let idx = args.firstIndex(of: "--model"), args.count > idx + 2 {
    Snapshot.renderModel(name: args[idx + 1], to: args[idx + 2])
    exit(0)
}

// MARK: - CLI: --skinned <NAME> <motionTime> <out.png> — render a skinned actor posed at time t
if let idx = args.firstIndex(of: "--skinned"), args.count > idx + 3 {
    Snapshot.renderSkinned(name: args[idx + 1], time: Double(args[idx + 2]) ?? 0, to: args[idx + 3])
    exit(0)
}

// MARK: - CLI: --tintdump <dir> — write the four player-tinted cannon skins
if let idx = args.firstIndex(of: "--tintdump"), args.count > idx + 1 {
    for i in 0..<4 {
        guard let img = Cannon.tintedSkin(i), let tiff = img.tiffRepresentation,
              let rep = NSBitmapImageRep(data: tiff),
              let png = rep.representation(using: .png, properties: [:]) else { continue }
        try? png.write(to: URL(fileURLWithPath: "\(args[idx + 1])/tint\(i).png"))
        print("wrote tint\(i).png")
    }
    exit(0)
}

// MARK: - CLI: --fxtest — spawn the sprite effects in a live view, snapshot frames
if args.contains("--fxtest") {
    Audio.shared.sfxVolume = 0; Audio.shared.musicVolume = 0
    let app = NSApplication.shared; app.setActivationPolicy(.accessory)
    guard let map = MapCatalog.byName("Tropicali") ?? MapCatalog.maps.first else { exit(1) }
    let world = World(map: map)
    _ = world.terrain.update(dt: 0.01)
    let c = world.center
    let fxPos = SIMD3<Float>(c.x, world.terrain.height(x: c.x, z: c.z) + 4, c.z)
    let camNode = SCNNode(); let cam = SCNCamera()
    cam.zFar = 4000; cam.fieldOfView = 50; camNode.camera = cam
    camNode.position = SCNVector3(CGFloat(fxPos.x), CGFloat(fxPos.y + 18), CGFloat(fxPos.z + 90))
    camNode.look(at: SCNVector3(CGFloat(fxPos.x), CGFloat(fxPos.y + 6), CGFloat(fxPos.z)))
    world.scene.rootNode.addChildNode(camNode)

    let view = SCNView(frame: NSRect(x: 0, y: 0, width: 640, height: 480))
    view.scene = world.scene; view.pointOfView = camNode
    view.rendersContinuously = true; view.isPlaying = true
    let win = NSWindow(contentRect: view.frame, styleMask: [.titled], backing: .buffered, defer: false)
    win.contentView = view
    win.orderFrontRegardless()

    DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
        Particles.explosion(at: fxPos, in: world, big: true)
        Particles.coins(at: fxPos + SIMD3(-25, 0, 0), count: 30, in: world)
        var wx = fxPos.x
        while world.terrain.height(x: wx, z: fxPos.z) > 0.3 && wx < fxPos.x + 300 { wx += 5 }
        Particles.splash(at: SIMD3(wx + 10, 0, fxPos.z), in: world)
        Particles.smokePuff(at: fxPos + SIMD3(25, 0, 0), in: world)
        var shots = 0
        func snap() {
            let names = ["early", "mid", "late"]
            let img = view.snapshot()
            if let t = img.tiffRepresentation, let r = NSBitmapImageRep(data: t),
               let p = r.representation(using: .png, properties: [:]) {
                try? p.write(to: URL(fileURLWithPath: "snapshots/fx-\(names[shots]).png"))
                print("fxtest wrote snapshots/fx-\(names[shots]).png")
            }
            shots += 1
            if shots < 3 {
                DispatchQueue.main.asyncAfter(deadline: .now() + (shots == 1 ? 0.35 : 0.9)) { snap() }
            } else { app.terminate(nil) }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) { snap() }
    }
    app.run()
    exit(0)
}

// MARK: - CLI: --raster <NAME> <out.png> — render a model through the software rasterizer
if let idx = args.firstIndex(of: "--raster"), args.count > idx + 2 {
    RasterDemo.render(model: args[idx + 1], to: args[idx + 2])
    exit(0)
}

// MARK: - CLI: --rasterscene <MAP> <out.png> — render a full game world via SoftRaster
if let idx = args.firstIndex(of: "--rasterscene"), args.count > idx + 2 {
    guard let map = MapCatalog.byName(args[idx + 1]) else {
        FileHandle.standardError.write("unknown map\n".data(using: .utf8)!); exit(1)
    }
    let world = World(map: map)
    _ = world.terrain.update(dt: 0.01)
    let camNode = SCNNode()
    let cam = SCNCamera(); cam.zFar = 4000; cam.fieldOfView = 55; camNode.camera = cam
    let c = world.center
    camNode.position = SCNVector3(CGFloat(c.x - 280), 190, CGFloat(c.z + 340))
    camNode.look(at: SCNVector3(CGFloat(c.x), 18, CGFloat(c.z)))
    world.scene.rootNode.addChildNode(camNode)

    let s = SceneRasterizer.settings(for: map)
    let raster = SceneRasterizer.renderFrame(scene: world.scene, pov: camNode,
                                             size: CGSize(width: 640, height: 480), settings: s)
    RasterDemo.writePNG(raster.rgba8(background: s.skyBottom, gamma: s.gamma), width: 640, height: 480, to: args[idx + 2])
    print("rasterscene wrote \(args[idx + 2]) (\(map.name))")
    exit(0)
}

// MARK: - CLI: --camtest — snapshot the in-game view in each of the 5 camera modes
if args.contains("--camtest") {
    Audio.shared.sfxVolume = 0; Audio.shared.musicVolume = 0
    let app = NSApplication.shared; app.setActivationPolicy(.accessory)
    let del = AppDelegate(); app.delegate = del
    DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
        var opts = GameOptions(); opts.mapIndex = 0
        opts.players = [PlayerConfig(name: "You", colorIndex: 0, botType: 0),
                        PlayerConfig(name: "Bot", colorIndex: 1, botType: 3)]
        del.controller.runOnRenderThread { del.controller.startGame(opts) }
        let modes = CameraController.Mode.selectable
        func shoot(_ i: Int) {
            guard i < modes.count else { NSApp.terminate(nil); return }
            del.controller.runOnRenderThread { del.controller.game?.camera.setMode(modes[i]) }
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                let img = del.controller.scnView.snapshot()
                if let t = img.tiffRepresentation, let r = NSBitmapImageRep(data: t),
                   let p = r.representation(using: .png, properties: [:]) {
                    try? p.write(to: URL(fileURLWithPath: "snapshots/cam-\(modes[i].label).png"))
                    print("camtest wrote cam-\(modes[i].label).png")
                }
                shoot(i + 1)
            }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) { shoot(0) }
    }
    app.run(); exit(0)
}

// MARK: - CLI: --gameovershots — snapshot the defeat/spectator/success/results screens

if args.contains("--gameovershots") {
    Audio.shared.sfxVolume = 0; Audio.shared.musicVolume = 0
    let app = NSApplication.shared; app.setActivationPolicy(.accessory)
    let del = AppDelegate(); app.delegate = del
    func snap(_ name: String) {
        let img = del.controller.scnView.snapshot()
        if let t = img.tiffRepresentation, let r = NSBitmapImageRep(data: t),
           let p = r.representation(using: .png, properties: [:]) {
            try? p.write(to: URL(fileURLWithPath: "snapshots/gameover-\(name).png"))
            print("gameovershots wrote gameover-\(name).png")
        }
    }
    DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
        var opts = GameOptions(); opts.mapIndex = 0; opts.maxRespawns = 0
        opts.players = [PlayerConfig(name: "You", colorIndex: 0, botType: 0),
                        PlayerConfig(name: "AggroBot", colorIndex: 1, botType: 2),
                        PlayerConfig(name: "ThinkBot", colorIndex: 2, botType: 3)]
        del.controller.runOnRenderThread { del.controller.startGame(opts) }
        DispatchQueue.main.asyncAfter(deadline: .now() + 4.0) {
            del.controller.runOnRenderThread {
                if let g = del.controller.game, let h = g.localHuman { g.kill(h, how: .drowned) }
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { snap("lose") }        // "You Lose!" up, pre-spectator
            DispatchQueue.main.asyncAfter(deadline: .now() + 8.0) { snap("spectator") }   // labels + bar removed
            DispatchQueue.main.asyncAfter(deadline: .now() + 9.0) {
                del.controller.runOnRenderThread {
                    if let g = del.controller.game { g.kill(g.players[1], how: .drowned) }  // ends the match
                }
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 12.0) { snap("success") }    // winner msg + orbit cam
            DispatchQueue.main.asyncAfter(deadline: .now() + 26.0) { snap("results"); NSApp.terminate(nil) }
        }
    }
    app.run(); exit(0)
}

// MARK: - CLI: --simulate <MAP> [seconds] — headless bot-vs-bot game for balance testing

if let idx = args.firstIndex(of: "--simulate"), args.count > idx + 1 {
    let mapName = args[idx + 1]
    let maxSeconds = args.count > idx + 2 ? (Double(args[idx + 2]) ?? 600) : 600
    guard let map = MapCatalog.byName(mapName), let mapIndex = MapCatalog.maps.firstIndex(where: { $0.name == map.name }) else {
        FileHandle.standardError.write("Unknown map '\(mapName)'\n".data(using: .utf8)!)
        exit(1)
    }
    Audio.shared.sfxVolume = 0
    Audio.shared.musicVolume = 0
    var opts = GameOptions()
    opts.mapIndex = mapIndex
    opts.players = [
        PlayerConfig(name: "DummyBot", colorIndex: 0, botType: 1),
        PlayerConfig(name: "AggroBot", colorIndex: 1, botType: 2),
        PlayerConfig(name: "ThinkBot", colorIndex: 2, botType: 3),
        PlayerConfig(name: "CrazyBot", colorIndex: 3, botType: 4)
    ]
    let game = GameController(options: opts, viewSize: CGSize(width: 1024, height: 768))
    print("simulate: \(map.name), wind \(Int(game.windVelocity)) mph @ \(Int(game.windDirection))°")
    let step = 1.0 / 30.0
    var t = 0.0
    var lastReport = 0.0
    while t < maxSeconds && !(game.gameOver && game.gameOverTimer > 6) {
        game.update(frameDt: step)
        t += step
        if t - lastReport >= 30 {
            lastReport = t
            let status = game.players.map { "\($0.name): \($0.active ? "cash \($0.cash) deaths \($0.deaths)" : "OUT")" }.joined(separator: " | ")
            print(String(format: "t=%3.0fs turn=%@ | %@", t, game.players[game.currentPlayerIndex].name, status))
        }
    }
    if let w = game.winnerIndex {
        print("WINNER after \(Int(t))s: \(game.players[w].name)")
    } else {
        print("no winner after \(Int(t))s (gameOver=\(game.gameOver))")
    }
    for p in game.players {
        print("  \(p.name): kills \(p.kills) misses \(p.misses) deaths \(p.deaths) drown \(p.drownings) spent \(p.goldSpent)")
    }
    exit(0)
}

// MARK: - CLI: --record <MAP> <out-dir> — capture a cinematic PNG frame sequence
// for the trailer. 8-player bot match, offscreen render, scripted camera.
//   [--seconds N] [--fps F] [--size WxH] [--warmup S] [--variety K] [--finale]

if let idx = args.firstIndex(of: "--record"), args.count > idx + 2 {
    let mapName = args[idx + 1]
    let outDir = args[idx + 2]
    guard let map = MapCatalog.byName(mapName),
          let mapIndex = MapCatalog.maps.firstIndex(where: { $0.name == map.name }) else {
        FileHandle.standardError.write("Unknown map '\(mapName)'. Maps: \(MapCatalog.maps.map(\.name).joined(separator: ", "))\n".data(using: .utf8)!)
        exit(1)
    }
    func flagStr(_ f: String) -> String? {
        guard let i = args.firstIndex(of: f), args.count > i + 1 else { return nil }
        return args[i + 1]
    }
    let seconds = flagStr("--seconds").flatMap(Double.init) ?? 20
    let fps = flagStr("--fps").flatMap(Int.init) ?? 30
    var size = CGSize(width: 1920, height: 1080)
    if let s = flagStr("--size") {
        let p = s.lowercased().split(separator: "x").compactMap { Int($0) }
        if p.count == 2 { size = CGSize(width: p[0], height: p[1]) }
    }
    let warmup = flagStr("--warmup").flatMap(Double.init) ?? 2.0
    let variety = flagStr("--variety").flatMap(Int.init) ?? mapIndex
    let finale = args.contains("--finale")
    let showcase = args.contains("--showcase")
    let hud = !args.contains("--no-hud")
    TrailerRecorder.record(mapIndex: mapIndex, outDir: outDir, seconds: seconds,
                           fps: fps, size: size, warmup: warmup, variety: variety,
                           finale: finale, showcase: showcase, hud: hud)
    exit(0)
}

// MARK: - CLI: --gameovertest — headless run of the defeat→spectator→winner→results flow

if args.contains("--gameovertest") {
    Audio.shared.sfxVolume = 0
    Audio.shared.musicVolume = 0
    var opts = GameOptions()
    opts.mapIndex = 0
    opts.maxRespawns = 0
    opts.hotSeatIndex = 1    // 20 s turns so the idle "human" forfeits its turns
    opts.players = [
        PlayerConfig(name: "Human", colorIndex: 0, botType: 0),   // never aims or fires
        PlayerConfig(name: "AggroBot", colorIndex: 1, botType: 2),
        PlayerConfig(name: "ThinkBot", colorIndex: 2, botType: 3)
    ]
    let game = GameController(options: opts, viewSize: CGSize(width: 1024, height: 768))
    let step = 1.0 / 30.0
    var t = 0.0
    var sawLose = false, sawSpectate = false, sawWin = false
    while t < 1200 && !game.hud.resultsShown {
        game.update(frameDt: step)
        t += step
        if let h = game.localHuman, !h.active, !sawLose {
            sawLose = true
            print(String(format: "t=%3.0fs human eliminated (deathWaitTimer=%@)", t,
                         game.deathWaitTimer.map { String(format: "%.1f", $0) } ?? "nil"))
        }
        if game.spectating && !sawSpectate {
            sawSpectate = true
            print(String(format: "t=%3.0fs spectator mode entered (camera=%@)", t, game.camera.mode.label))
        }
        if game.gameOver && !sawWin {
            sawWin = true
            let w = game.winnerIndex.map { game.players[$0].name } ?? "nobody"
            print(String(format: "t=%3.0fs game over, winner=%@ (camera=%@)", t, w, game.camera.mode.label))
        }
    }
    print("resultsShown=\(game.hud.resultsShown) after \(Int(t))s "
          + "(gameOverTimer=\(String(format: "%.1f", game.gameOverTimer)))")
    // the results screen's Done button must dismiss (regression: dead click)
    var exited = false
    game.onExit = { exited = true }
    let doneHit = game.click(at: CGPoint(x: 400, y: 25))   // Done center: (400, 600-575)
    print("done-click hit=\(doneHit) exited=\(exited)")
    let ok = sawLose && game.gameOver && game.hud.resultsShown && exited
    print(ok ? "GAMEOVERTEST PASS" : "GAMEOVERTEST FAIL (lose=\(sawLose) spectate=\(sawSpectate) over=\(game.gameOver) results=\(game.hud.resultsShown) doneExit=\(exited))")
    exit(ok ? 0 : 1)
}

// MARK: - App bootstrap (no xib/xcodeproj)

let app = NSApplication.shared
app.setActivationPolicy(.regular)
// Original Cannonballs app icon (the gold skull dubloon, extracted from the game).
if let icon = NSImage(contentsOf: Assets.url("AppIcon.png")) {
    app.applicationIconImage = icon
}
let delegate = AppDelegate()
app.delegate = delegate

// --uitest: open the app, snapshot menu + in-game HUD to PNGs, then quit (self-verification).
if args.contains("--uitest") {
    Audio.shared.sfxVolume = 0
    Audio.shared.musicVolume = 0
    func snap(_ name: String) {
        guard let ctrl = delegate.controller else { return }
        let img = ctrl.scnView.snapshot()
        if let tiff = img.tiffRepresentation, let rep = NSBitmapImageRep(data: tiff),
           let png = rep.representation(using: .png, properties: [:]) {
            try? png.write(to: URL(fileURLWithPath: "snapshots/uitest-\(name).png"))
            print("uitest wrote snapshots/uitest-\(name).png")
        }
    }
    UserDefaults.standard.set("Tester", forKey: "playerName")
    func menuClick(_ x: CGFloat, _ y: CGFloat) {
        delegate.controller.runOnRenderThread {
            _ = delegate.controller.menuScene?.click(at: CGPoint(x: x, y: y))
        }
    }
    DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
        snap("menu")
        menuClick(400, 180)                       // Single Player -> Your Name popup
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) {
            snap("name")
            menuClick(527, 188)                   // Enter -> New Game Settings
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.9) {
                snap("iris-close")                // iris mid-close: screen visible in the shrinking circle, black corners
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) {
                snap("transition")                // iris + hourglass mid-cover
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 4.0) {
                snap("settings")
                menuClick(527, 188)               // Create -> lobby
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                    snap("lobby")
                    menuClick(200, 456)           // open slot 1's AI dropdown (SK y-up coords; src row center y=144)
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                        snap("lobbyopen")
                        menuClick(200, 329)           // pick "Thinker" for slot 1 (src y=144+31+3*32)
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                            snap("lobbyready")    // kick button should now show
                            menuClick(610, 280)   // open the color dropdown
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                                menuClick(610, 185)   // pick color 2 (src y=320+31+2*32; crash regression check)
                            }
                            DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
                                snap("lobbycolor")    // retinted preview, no crash
                                menuClick(610, 248)   // Begin The Game!
                            }
                            DispatchQueue.main.asyncAfter(deadline: .now() + 5.5) {
                                snap("game")      // after the iris uncovers
                                DispatchQueue.main.asyncAfter(deadline: .now() + 10.0) {
                                    // charge the power bar so the fill color is in the frame
                                    // (regression check: original fill is the orange POWERBAR texture)
                                    delegate.controller.runOnRenderThread {
                                        if let p = delegate.controller.game?.localHuman {
                                            p.powerBarActive = true
                                            p.powerLevel = 0.6
                                        }
                                    }
                                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                                    snap("game2")
                                    delegate.controller.runOnRenderThread {
                                        if let p = delegate.controller.game?.localHuman {
                                            p.powerBarActive = false
                                            p.powerLevel = 0
                                        }
                                        delegate.controller.softwareRender = true
                                    }
                                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                                        snap("software")
                                        delegate.controller.runOnRenderThread { delegate.controller.softwareRender = false }
                                        // chat: long line must wrap in the 250px column; entry line shows SAY : + typed text
                                        func fakeKey(_ code: UInt16, _ chars: String) -> NSEvent? {
                                            NSEvent.keyEvent(with: .keyDown, location: .zero, modifierFlags: [],
                                                             timestamp: 0, windowNumber: 0, context: nil,
                                                             characters: chars, charactersIgnoringModifiers: chars,
                                                             isARepeat: false, keyCode: code)
                                        }
                                        delegate.controller.runOnRenderThread {
                                            guard let g = delegate.controller.game else { return }
                                            g.hud.botChat("Tester", "this long chat line should wrap neatly inside the chat box instead of running off the right edge of the panel")
                                            g.hud.beginChatEntry()
                                            if let ev = fakeKey(4, "ahoy there") { _ = g.keyDown(ev) }
                                        }
                                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                                            snap("chat")       // wrapped line + "SAY : ahoy there▍"
                                            delegate.controller.runOnRenderThread {
                                                guard let g = delegate.controller.game else { return }
                                                if let ev = fakeKey(53, "\u{1b}") { _ = g.keyDown(ev) }  // esc closes entry
                                                _ = g.hud.click(at: CGPoint(x: 100, y: 589))             // open Options
                                            }
                                        }
                                        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                                            snap("options")    // Shadows / Sound / Music rows
                                            delegate.controller.runOnRenderThread {
                                                guard let g = delegate.controller.game else { return }
                                                _ = g.hud.click(at: CGPoint(x: 400, y: 300))             // close menu
                                                _ = g.hud.click(at: CGPoint(x: 315, y: 589))             // open Help!
                                            }
                                        }
                                        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                                            snap("help")       // Controls / How To Play / Tutorial rows
                                            delegate.controller.runOnRenderThread {
                                                guard let g = delegate.controller.game else { return }
                                                _ = g.hud.click(at: CGPoint(x: 400, y: 300))
                                                // forfeit eliminates the human outright → win banner → auto results
                                                g.menuForfeit()
                                            }
                                        }
                                        DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) {
                                            snap("gameover")   // "<bot> Wins!" message + success-camera orbit
                                        }
                                        DispatchQueue.main.asyncAfter(deadline: .now() + 19.0) {
                                            snap("results")    // results screen at 15 s (Game_Loop.java:100)
                                            // regression: Done must respond to a REAL view-level
                                            // click (the full mouseDown → overlay-convert → queue
                                            // → drain path, not a direct hud.click)
                                            let view = delegate.controller.scnView
                                            if let overlay = view.overlaySKScene, let win = view.window {
                                                let vp = overlay.convertPoint(toView: CGPoint(x: 400, y: 25))
                                                let wp = view.convert(vp, to: nil)
                                                if let ev = NSEvent.mouseEvent(
                                                    with: .leftMouseDown, location: wp,
                                                    modifierFlags: [], timestamp: ProcessInfo.processInfo.systemUptime,
                                                    windowNumber: win.windowNumber, context: nil,
                                                    eventNumber: 0, clickCount: 1, pressure: 1) {
                                                    view.mouseDown(with: ev)
                                                }
                                            }
                                        }
                                        DispatchQueue.main.asyncAfter(deadline: .now() + 22.0) {
                                            snap("results-done")   // must show the main menu again
                                            NSApp.terminate(nil)
                                        }
                                    }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// minimal main menu so Cmd-Q works
let mainMenu = NSMenu()
let appItem = NSMenuItem()
mainMenu.addItem(appItem)
let appMenu = NSMenu()
appMenu.addItem(NSMenuItem(title: "About Cannonballs!", action: #selector(NSApplication.orderFrontStandardAboutPanel(_:)), keyEquivalent: ""))
appMenu.addItem(.separator())
appMenu.addItem(NSMenuItem(title: "Quit Cannonballs!", action: #selector(NSApplication.terminate(_:)), keyEquivalent: "q"))
appItem.submenu = appMenu
app.mainMenu = mainMenu

app.run()
