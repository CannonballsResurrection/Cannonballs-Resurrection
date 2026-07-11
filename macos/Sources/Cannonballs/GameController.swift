import Foundation
import SceneKit
import SpriteKit
import AppKit

/// Central match driver: turn state machine, physics stepping, economy (SPEC §4, §5).
final class GameController {
    let options: GameOptions
    let world: World
    var players: [Cannon] = []
    var projectiles: [Projectile] = []
    var chests: [Chest] = []
    let camera = CameraController()
    private var cloudOffset = SIMD2<Float>(0, 0)
    private var lensFlare: WorldDressing.LensFlare?
    let hud: HUDScene

    var onExit: (() -> Void)?

    // wind (rolled once per match)
    var windDirection: Float = 0   // deg
    var windVelocity: Float = 0    // "mph" 0..79
    var wind: SIMD3<Float> {
        SIMD3(sin(G.deg2rad(windDirection)) * windVelocity * 0.2, 0,
              cos(G.deg2rad(windDirection)) * windVelocity * 0.2)
    }

    // turn state
    var currentPlayerIndex = 0
    var waitingTimer: Float? = nil        // 3 s after on-turn shot resolves → switch
    var hotSeatRemaining: Float = 0       // seconds left; only if option set
    var gameOver = false
    var gameOverTimer: Float = 0
    var winnerIndex: Int? = nil
    // GameState 15: the local player is out of lives; 5 s of "You Lose!" before
    // spectator mode (Cannon.WaitingTimer, Packet_Manager.java:267)
    var deathWaitTimer: Float? = nil
    var spectating = false                // GameState 11
    // local forfeit: 10 s (15 s destroy state entered with 5 s elapsed —
    // Packet_Manager.java:246-247) and the match is left
    var forfeitExitTimer: Float? = nil
    var gameTime: TimeInterval = 0
    private var lastTickSecond = -1
    private var minimapRefresh: Float = 0

    var currentPlayer: Cannon { players[currentPlayerIndex] }
    var cameraFocusCannon: Cannon? {
        if let w = winnerIndex { return players[w] }
        return currentPlayer.active && !currentPlayer.dying ? currentPlayer : players.first { $0.active && !$0.dying }
    }
    /// First human player (for HUD display even while dead/respawning).
    var localHuman: Cannon? { players.first { !$0.isBot } }

    /// The cannon the local keyboard drives right now.
    var controlledCannon: Cannon? {
        if !currentPlayer.isBot && currentPlayer.active && !currentPlayer.dying { return currentPlayer }
        return players.first { !$0.isBot && $0.active && !$0.dying }
    }

    init(options: GameOptions, viewSize: CGSize) {
        self.options = options
        world = World(map: MapCatalog.maps[options.mapIndex])
        hud = HUDScene(size: viewSize)
        camera.attach(to: self)
        hud.game = self

        // players
        for (i, cfg) in options.players.enumerated() {
            let c = Cannon(index: i, config: cfg, game: self)
            players.append(c)
            world.scene.rootNode.addChildNode(c.node)
        }
        for c in players { c.place(); c.toGround(); c.spinAngle = Float.random(in: 0..<360); c.syncNode() }

        // chests: 3–7
        let chestCount = Int.random(in: 3...7)
        for _ in 0..<chestCount { spawnChest() }

        // wind
        windDirection = Float.random(in: 0..<360)
        windVelocity = Float(Int.random(in: 0..<80))

        // lens flare on sunny maps (Entity_Object_LensFlare)
        if world.map.hasSun {
            lensFlare = WorldDressing.LensFlare(sunVector: world.map.sunVector, camera: camera.node)
        }

        // first player
        currentPlayerIndex = Int.random(in: 0..<players.count)
        beginTurn(announce: true)
        hud.rebuildStatic()
    }

    func tearDown() {
        Audio.shared.stopLoops()
    }

    // MARK: - Chests

    func spawnChest() {
        let terrain = world.terrain
        let size = terrain.worldSize
        for _ in 0..<200 {
            let x = Float.random(in: size * 0.06...(size * 0.94))
            let z = Float.random(in: size * 0.06...(size * 0.94))
            guard terrain.targetHeight(x: x, z: z) > 2.0 else { continue }
            let p = SIMD3(x, 0, z)
            if players.contains(where: { $0.active && dist2D($0.position, p) < 10 }) { continue }
            if chests.contains(where: { $0.alive && dist2D($0.position, p) < 30 }) { continue }
            if world.props.contains(where: { $0.alive && dist2D($0.position, p) < 10 + $0.spec.radius }) { continue }
            let chest = Chest(position: SIMD3(x, Chest.restHeight(on: terrain, x: x, z: z), z))
            chests.append(chest)
            world.scene.rootNode.addChildNode(chest.node)
            return
        }
    }

    func collectChest(_ chest: Chest, by shooter: Cannon) {
        chest.alive = false
        chest.node.removeFromParentNode()
        chests.removeAll { $0 === chest }
        switch Chest.rollTreasure() {
        case .gold(let amount):
            shooter.cash += amount
            Audio.shared.play("cash")
            Particles.coins(at: chest.position, count: 30, in: world)
            hud.flashMessage("\(shooter.name) finds \(amount) gold!")
        case .teleport:
            hud.flashMessage("Cursed chest! \(shooter.name) is teleported!")
            shooter.playerTeleport()
        }
        if options.treasureRespawn { spawnChest() }
        hud.markMinimapDirty()
    }

    // MARK: - Firing / projectiles

    func launch(_ projectile: Projectile, from cannon: Cannon) {
        projectiles.append(projectile)
        world.scene.rootNode.addChildNode(projectile.node)
        projectile.update(dt: 0.0001)   // spec: immediate tiny tick
    }

    /// Tower / Teleport (no projectile): consumes the turn if used on-turn.
    func instantWeaponUsed(by cannon: Cannon) {
        if currentPlayerIndex == cannon.index {
            cannon.hasFiredThisTurn = true
            cannon.firedOnTurn = false
            waitingTimer = 3.0
        }
        hud.refreshDynamic()
    }

    func projectileDied(_ projectile: Projectile, impact: SIMD3<Float>) {
        projectiles.removeAll { $0 === projectile }
        projectile.owner.bot.lastImpact = impact
        // record threat for defensive AI, with the shooter attribution the
        // Aggressive/Crazy retarget reads (Cannon.java:1410-1429)
        for p in players where p !== projectile.owner {
            p.bot.noteEnemyImpact(impact, shooter: projectile.owner.index)
        }
        if projectile.owner.firedOnTurn && projectile.owner.index == currentPlayerIndex {
            projectile.owner.firedOnTurn = false
            waitingTimer = 3.0
        }
        hud.refreshDynamic()
    }

    /// X-Shot line kill (SPEC checkForHitLine).
    func checkForHitLine(x1: Float, z1: Float, x2: Float, z2: Float, width: Float, killer: Cannon) {
        for c in players where c !== killer && c.active && !c.dying {
            let d = Terrain.distanceToSegment(px: c.position.x, pz: c.position.z, x1: x1, z1: z1, x2: x2, z2: z2)
            if d < width { kill(c, how: .killed(by: killer.index)) }
        }
        for chest in chests where chest.alive {
            let d = Terrain.distanceToSegment(px: chest.position.x, pz: chest.position.z, x1: x1, z1: z1, x2: x2, z2: z2)
            if d < width { collectChest(chest, by: killer) }
        }
        for prop in world.props where prop.alive && prop.spec.destructible {
            let d = Terrain.distanceToSegment(px: prop.position.x, pz: prop.position.z, x1: x1, z1: z1, x2: x2, z2: z2)
            if d < width { destroyProp(prop, by: killer) }
        }
    }

    // MARK: - Props / TNT

    func destroyProp(_ prop: Prop, by killer: Cannon?) {
        guard prop.alive else { return }
        if prop.spec.explosive {
            detonate(prop, by: killer ?? prop.detonator)
            return
        }
        world.destroyProp(prop)
        world.terrain.crater(x: prop.position.x, z: prop.position.z, depth: 4, radius: 20)
        Particles.explosion(at: prop.position + SIMD3<Float>(0, prop.spec.height / 2, 0), in: world)
        // the prop.dat <DEBRIS> pieces tumble away (Prop.java:154-163 /
        // Chunk_Object). Offsets are actor-local and the debris actors spawn
        // unscaled, like every actor (Prop.java:388 — native scale).
        for (model, offset) in prop.spec.debris {
            FXSprites.debrisChunk(model: model,
                                  at: prop.position + offset,
                                  velocity: SIMD3(Float.random(in: -12...12),
                                                  Float.random(in: 15...30),
                                                  Float.random(in: -12...12)),
                                  scale: 1, in: world)
        }
        // STAR burst off the falling prop (Prop.java:113)
        for _ in 0..<4 {
            FXSprites.star(at: prop.position + SIMD3(0, 2 + Float.random(in: 0...4), 0),
                           velocity: SIMD3(Float.random(in: -15...15), 30 + Float.random(in: -15...15),
                                           Float.random(in: -15...15)),
                           size: Float.random(in: 1...4), in: world)
        }
        Audio.shared.play("explosion1")
    }

    private func detonate(_ prop: Prop, by detonator: Cannon?) {
        guard prop.alive, !prop.detonating else { return }
        prop.detonating = true
        prop.shockScale = 0
        prop.detonator = detonator ?? prop.detonator
        prop.node.isHidden = true
        Particles.explosion(at: prop.position + SIMD3<Float>(0, 4, 0), in: world, big: true)
        // visible SHOCKWAVE ring grows with the kill radius (60 u/s to 50)
        FXSprites.shockwave(at: prop.position, scale: 2, rate: 30, in: world)
        Audio.shared.play("explosion3")
        camera.shock(at: prop.position, radius: 150)
        world.terrain.crater(x: prop.position.x, z: prop.position.z, depth: 4, radius: 20)
    }

    private func updateShockwaves(dt: Float) {
        for prop in world.props where prop.detonating && prop.alive {
            prop.shockScale += dt * 60
            let r = prop.shockScale
            // kill cannons inside the shockwave
            for c in players where c.active && !c.dying {
                let d = simd_distance(SIMD3(c.position.x, c.position.y, c.position.z),
                                      prop.position + SIMD3<Float>(0, 4, 0))
                if d < r + 10 {
                    kill(c, how: .detonated(by: prop.detonator?.index ?? -1))
                }
            }
            // chain other props / chests
            for other in world.props where other !== prop && other.alive {
                if simd_distance(other.position, prop.position) < r { destroyProp(other, by: prop.detonator) }
            }
            for chest in chests where chest.alive {
                if simd_distance(chest.position, prop.position) < r, let det = prop.detonator {
                    collectChest(chest, by: det)
                }
            }
            if r >= 50 {
                prop.detonating = false
                world.destroyProp(prop)
            }
        }
    }

    // MARK: - Death & loot (SPEC §5, §8)

    func kill(_ victim: Cannon, how: DeathType) {
        guard victim.active, !victim.dying else { return }
        victim.dying = true
        victim.deaths += 1
        victim.respawnTimer = 4.0

        var message: String
        switch how {
        case .killed(let killerIdx):
            let killer = players[killerIdx]
            killer.kills += 1
            killer.cash += victim.cash
            Audio.shared.play("cash")
            // "<killer> <verb> You!" / "<killer> <verb> <name>" (Packet_Manager.java:312/401)
            message = "\(killer.name) \(G.deathVerbs.randomElement()!) \(victim === localHuman ? "You" : victim.name)!"
            if killer.isBot { BotChat.maybeBoast(killer, game: self) }
            else { BotChat.pollCompliments(for: killer, game: self) }
        case .detonated(let idx):
            if idx >= 0 {
                players[idx].kills += 1
                players[idx].cash += victim.cash
                Audio.shared.play("cash")
            }
            // Packet_Manager.java:281/380
            message = victim === localHuman ? "You Were Detonated!" : "\(victim.name) Was Detonated!"
        case .drowned:
            victim.drownings += 1
            // Packet_Manager.java:256/352
            message = victim === localHuman ? "You Were Drowned!" : "\(victim.name) Was Drowned!"
        case .forfeit:
            // Packet_Manager.java:245/350
            message = victim === localHuman ? "You Forfeit The Game!" : "\(victim.name) Forfeits The Game!"
        }
        if victim.isBot { BotChat.maybeDeathCry(victim, game: self) }
        // death penalty: cash resets to half STARTING cash
        victim.cash = G.startingCashTable[options.startingCashIndex] / 2

        Particles.deathBlast(at: victim.position, in: world)
        Particles.coins(at: victim.position, count: min(victim.cash / 20, 75), in: world)
        Audio.shared.play("explosion1")
        camera.shock(at: victim.position, radius: 120)
        if case .drowned = how {} else {
            world.terrain.crater(x: victim.position.x, z: victim.position.z, depth: 8, radius: 30)
        }

        victim.respawnsUsed += 1
        // forfeit skips remaining respawns (Cannon.java:1099 — death(bl=true))
        var isForfeit = false
        if case .forfeit = how { isForfeit = true }
        let eliminated = isForfeit || victim.respawnsUsed > options.maxRespawns
        if eliminated {
            victim.active = false
        }
        victim.syncNode()
        hud.flashMessage(message)
        // "You Lose!" / "<name> Loses!" chases the death message once the lives
        // run out (Packet_Manager.java:262/354); forfeits don't get one (:245)
        if eliminated && !isForfeit {
            hud.flashMessage(victim === localHuman ? "You Lose!" : "\(victim.name) Loses!")
        }
        hud.refreshDynamic()
        hud.markMinimapDirty()

        checkWinCondition()
        // Local player out while the game goes on:
        if eliminated && !gameOver && victim === localHuman {
            if isForfeit {
                // Forfeit goes straight to the end state with 5 s already on the
                // 15 s clock (GameState 13, GameStateTimeOut = 5000 —
                // Packet_Manager.java:246-247): 10 s more, then out. No spectating.
                forfeitExitTimer = 10.0
            } else {
                // GameState 15 (Packet_Manager.java:262-268): persistent
                // "You Lose!" center message, weapon put away, and a 5 s wait
                // before spectator mode kicks in.
                hud.showSuccessMessage("You Lose!")
                victim.powerBarActive = false
                deathWaitTimer = 5.0             // Cannon.WaitingTimer = 5.0f
            }
        }
        if !gameOver && victim.index == currentPlayerIndex {
            waitingTimer = min(waitingTimer ?? 3.0, 3.0)
        }
    }

    /// Match decided — any death that leaves fewer than 2 active players
    /// (Packet_Manager.java:416-448): GameState 13, success camera (setCamera(6)),
    /// and the persistent winner message.
    private func checkWinCondition() {
        let alive = players.filter { $0.active }
        guard alive.count <= 1, !gameOver else { return }
        gameOver = true
        gameOverTimer = 0                        // GameStateTimeOut = 0 (:420/429/438)
        winnerIndex = alive.first?.index
        deathWaitTimer = nil
        forfeitExitTimer = nil                   // a winner restarts the full 15 s clock
        camera.setSuccessMode()                  // Packet_Manager.java:430/439
        // Packet_Manager.java:426-433 — "You Win!" when the local player is the
        // survivor; :435-447 — "<name> Wins!" for whoever else is left standing.
        if let w = alive.first {
            hud.showSuccessMessage(w === localHuman ? "You Win!" : "\(w.name) Wins!")
        }
    }

    // MARK: - Turn flow (SPEC §4)

    func beginTurn(announce: Bool) {
        let p = currentPlayer
        p.hasFiredThisTurn = false
        p.firedOnTurn = false
        hotSeatRemaining = Float(G.hotSeatTimes[options.hotSeatIndex])
        lastTickSecond = -1
        if p.isBot {
            p.bot.beginTurn(for: p, game: self)
        }
        if announce {
            if p.isBot {
                hud.showBanner("\(p.name)'s Turn")
            } else {
                hud.showBanner(players.filter({ !$0.isBot }).count > 1 ? "\(p.name) — YOUR TURN" : "YOUR TURN")
            }
            // every turn change opens on the drum roll (HUD.showMyTurn /
            // showOtherTurn — HUD.java:718, 760)
            Audio.shared.play("drumroll")
        }
        hud.refreshDynamic()
        hud.markMinimapDirty()
    }

    func switchPlayers() {
        guard !gameOver else { return }
        var idx = currentPlayerIndex
        for _ in 0..<players.count {
            idx = (idx + 1) % players.count
            if players[idx].active { break }
        }
        currentPlayerIndex = idx
        beginTurn(announce: true)
        // spectator camera follows the new current player and re-labels itself
        // (Game_Loop.switchPlayers, Game_Loop.java:435-437)
        if camera.mode == .spectator {
            hud.refreshSpectatorName(currentPlayer.name)
        }
    }

    // MARK: - Frame update

    func update(frameDt: TimeInterval) {
        guard frameDt > 0 else { return }
        gameTime += frameDt
        let ms = frameDt * 1000
        let n = min(5, max(1, Int(ceil(ms / 40))))
        let sub = Float(frameDt) / Float(n)

        for _ in 0..<n {
            substep(dt: sub)
        }

        let dt = Float(frameDt)
        if world.terrain.update(dt: dt) {
            world.regroundDecorations()
            for chest in chests where chest.alive { chest.reground(on: world.terrain) }
        }
        for c in players where c.active && !c.dying { c.toGround(); c.syncNode() }
        updateShockwaves(dt: dt)
        updateTimers(dt: dt)
        for c in players where c.isBot && c.active { c.bot.think(for: c, game: self, dt: dt) }
        camera.update(dt: dt)
        // sky dome follows the camera — FULL position including Y
        // (Camera.java:583-586: Environment.setPosition(X, Y, Z))
        if let sky = world.skyActor {
            sky.simdPosition = camera.node.simdPosition
        }
        // cloud shadows drift with the wind (Island.java: offset -= wind * 0.01 * dt)
        WorldDressing.driftClouds(world.terrain.material, wind: wind, dt: dt, offset: &cloudOffset)
        lensFlare?.update(pov: camera.node, dt: dt, terrain: world.terrain)

        minimapRefresh -= dt
        if minimapRefresh <= 0 { minimapRefresh = 0.15; hud.updateMinimap() }
        hud.update(dt: dt)

        // Ambient fish: 0.0075 chance per frame to leap from open water near the
        // island center, launched ((r-0.5)*20, 30+r*10, (r-0.5)*20) with splashes.
        if Float.random(in: 0..<1) < 0.0075 {
            let c = world.center
            let x = c.x + Float.random(in: -400...400)
            let z = c.z + Float.random(in: -400...400)
            if world.terrain.height(x: x, z: z) <= 0 {
                Particles.fishJump(at: SIMD3(x, 0, z),
                                   velocity: SIMD3(Float.random(in: -10...10),
                                                   30 + Float.random(in: 0...10),
                                                   Float.random(in: -10...10)),
                                   in: world)
            }
        }
    }

    private func substep(dt: Float) {
        // aiming: only the controlled human cannon uses live keys; bots slew in BotAI
        if let c = controlledCannon {
            c.spinInput(dt: dt)
            c.updatePowerBar(dt: dt)
        }
        for c in players where c.isBot && c.active && !c.dying {
            c.bot.slew(for: c, dt: dt)
        }
        for p in projectiles { p.update(dt: dt) }
    }

    private func updateTimers(dt: Float) {
        if gameOver {
            // GameState 13 (updateGameStateDestroy, Game_Loop.java:97-106): the
            // world keeps running under the success camera for 15 s, then the
            // original dissolves out and tears the game down. The clone leaves
            // the match via the results screen instead of the bare main menu.
            gameOverTimer += dt
            if gameOverTimer > 15 && !hud.resultsShown {
                hud.showResults()
            }
            return
        }
        // local forfeit: the remaining 10 s of the destroy state, then leave
        // (Game_Loop.updateGameStateDestroy with GameStateTimeOut preloaded to 5000)
        if let f = forfeitExitTimer {
            if f - dt <= 0 {
                forfeitExitTimer = nil
                if !hud.resultsShown { hud.showResults() }
            } else {
                forfeitExitTimer = f - dt
            }
        }
        // GameState 15 → 11 (Cannon.updateDeathWaitingTimer, Cannon.java:1480-1497):
        // 5 s after the local "You Lose!", switch to spectator mode while the
        // match plays out. (The other-players-gone branch never reaches here —
        // checkWinCondition already flipped gameOver.)
        if let t = deathWaitTimer {
            if t - dt <= 0 {
                deathWaitTimer = nil
                spectating = true
                camera.setSpectatorMode()        // Cannon.java:1487
                hud.enterSpectatorMode(following: currentPlayer.name)
            } else {
                deathWaitTimer = t - dt
            }
        }
        // respawns (SPEC: 4 s)
        for c in players where c.dying {
            c.respawnTimer -= dt
            if c.respawnTimer <= 0 {
                c.dying = false
                if c.active {
                    c.place()
                    c.toGround()
                    Particles.teleport(at: c.position, in: world)
                    Audio.shared.play("teleport")
                    c.syncNode()
                    hud.markMinimapDirty()
                }
            }
        }
        // after-shot wait → next turn
        if let w = waitingTimer {
            let nw = w - dt
            if nw <= 0 {
                waitingTimer = nil
                switchPlayers()
            } else {
                waitingTimer = nw
            }
        }
        // hotseat countdown (only while current player still may fire)
        if hotSeatRemaining > 0 && waitingTimer == nil && !currentPlayer.dying {
            let hasLiveShot = projectiles.contains { $0.owner === currentPlayer }
            if !hasLiveShot && !currentPlayer.hasFiredThisTurn {
                hotSeatRemaining -= dt
                let s = Int(ceil(hotSeatRemaining))
                if hotSeatRemaining <= 10 && s != lastTickSecond {
                    lastTickSecond = s
                    Audio.shared.play("timer_tick")
                }
                if hotSeatRemaining <= 0 {
                    hud.flashMessage("Time Up!")
                    Audio.shared.play("time_up")
                    currentPlayer.powerBarActive = false
                    switchPlayers()
                }
            }
        }
    }

    // MARK: - Input

    func keyDown(_ e: NSEvent) -> Bool {
        if hud.resultsShown {
            if e.keyCode == 36 || e.keyCode == 53 { onExit?() }  // return/esc
            return true
        }
        // chat entry swallows everything while open (Chat.java: controls disabled)
        if hud.chatKeyDown(e) { return true }
        switch e.keyCode {
        case 8:  // C — open chat entry (HUD.java:501)
            if let c = controlledCannon {
                c.keyLeft = false; c.keyRight = false; c.keyUp = false; c.keyDown = false
                Audio.shared.stopLoop("turn_loop"); Audio.shared.stopLoop("tilt")
            }
            hud.beginChatEntry()
            return true
        default: break
        }
        switch e.keyCode {
        case 123: controlledCannon?.keyLeft = true; Audio.shared.startLoop("turn_loop"); return true
        case 124: controlledCannon?.keyRight = true; Audio.shared.startLoop("turn_loop"); return true
        case 126: controlledCannon?.keyUp = true; Audio.shared.startLoop("tilt", volume: 0.3); return true
        case 125: controlledCannon?.keyDown = true; Audio.shared.startLoop("tilt", volume: 0.3); return true
        case 49:  controlledCannon?.triggerFire(); return true          // space
        case 24, 69: cycleWeapon(1); return true                        // = / +
        case 27, 78: cycleWeapon(-1); return true                       // - / numpad-
        case 9:  camera.cycle(); return true                            // V
        case 12: // Q: forfeit while playing; as a spectator it leaves the game
            if let c = controlledCannon {
                kill(c, how: .forfeit)
            } else if spectating && !gameOver {
                onExit?()       // HUD.keyDownSpectator (HUD.java:695-698)
            }
            return true
        case 53: onExit?(); return true                                  // esc → menu
        default: return false
        }
    }

    func keyUp(_ e: NSEvent) -> Bool {
        switch e.keyCode {
        case 123: controlledCannon?.keyLeft = false; Audio.shared.stopLoop("turn_loop"); return true
        case 124: controlledCannon?.keyRight = false; Audio.shared.stopLoop("turn_loop"); return true
        case 126: controlledCannon?.keyUp = false; Audio.shared.stopLoop("tilt"); return true
        case 125: controlledCannon?.keyDown = false; Audio.shared.stopLoop("tilt"); return true
        default: return false
        }
    }

    // MARK: - Menu-driven actions (mirror the keyboard controls)

    func menuFire() { guard !hud.resultsShown else { return }; controlledCannon?.triggerFire() }
    func menuNextWeapon() { guard !hud.resultsShown else { return }; cycleWeapon(1) }
    func menuPrevWeapon() { guard !hud.resultsShown else { return }; cycleWeapon(-1) }
    func menuForfeit() { if let c = controlledCannon { kill(c, how: .forfeit) } }

    /// Island.switchShadows: toggle every SHADOW blob patch in the scene.
    func setShadowsVisible(_ on: Bool) {
        world.scene.rootNode.enumerateHierarchy { node, _ in
            if node.name == "blob-shadow" { node.isHidden = !on }
        }
    }
    func menuBackToMenu() { onExit?() }
    func menuSetCamera(_ mode: CameraController.Mode) { camera.setMode(mode) }

    func cycleWeapon(_ delta: Int) {
        guard let c = controlledCannon else { return }
        var idx = c.weaponIndex
        for _ in 0..<WeaponType.allCases.count {
            idx = (idx + delta + 12) % 12
            let w = WeaponType(rawValue: idx)!
            let offTurn = currentPlayerIndex != c.index
            if c.cash >= w.cost && !(offTurn && w.offensive) { break }
        }
        c.weaponIndex = idx
        Audio.shared.play("click")
        hud.refreshDynamic()
    }

    func selectWeapon(_ idx: Int) {
        guard let c = controlledCannon, let w = WeaponType(rawValue: idx) else { return }
        let offTurn = currentPlayerIndex != c.index
        if c.cash < w.cost {
            hud.flashMessage("Not Enough Gold For That Weapon!")
            return
        }
        if offTurn && w.offensive {
            hud.flashMessage("Only Defensive Items Can Be Used On Your Off Turn!")
            return
        }
        c.weaponIndex = idx
        Audio.shared.play("click")
        hud.refreshDynamic()
    }

    func click(at point: CGPoint) -> Bool {
        hud.click(at: point)
    }
}
