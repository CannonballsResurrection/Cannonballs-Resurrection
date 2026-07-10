import Foundation
import simd

/// Bot AI per SPEC §9. Types: 1 Dummy, 2 Aggressive, 3 Thinker, 4 Crazy.
final class BotState {
    var targetIndex: Int?
    var hasGreeted = false                  // original: greeting posted once, gates insults
    var losArmed = false                    // two-phase LineOfSight trick (Cannon.java:808-815)
    var lastImpact: SIMD3<Float>?           // own last shot impact point
    var firstShotAtTarget = true
    var recentEnemyImpacts: [(pos: SIMD3<Float>, shooter: Int)] = []

    var solutionAzimuth: Float = 0          // deg
    var solutionElev: Float = 0             // deg barrel elevation
    var solutionPower: Float = 0.5
    var haveSolution = false

    var fireDelay: Float = 0                // thinking time before acting
    var defenseTick: Float = 0

    // the Java move-reset memory (Cannon.java:672-694)
    private var lastTargetPosition = SIMD3<Float>.zero
    private var lastBotPosition = SIMD3<Float>.zero

    /// GameController calls this whenever any projectile dies so bots can dodge
    /// (Weapon.java hide() stores LastHitX/Z per cannon; the shooter index keeps
    /// the attribution the Java threat scan reads, Cannon.java:1394-1431).
    func noteEnemyImpact(_ p: SIMD3<Float>, shooter: Int = -1) {
        recentEnemyImpacts.append((pos: p, shooter: shooter))
        if recentEnemyImpacts.count > 4 { recentEnemyImpacts.removeFirst() }
    }

    // MARK: - Turn start

    func beginTurn(for c: Cannon, game: GameController) {
        BotChat.maybeGreet(c, game: game)
        pickTarget(for: c, game: game)
        computeSolution(for: c, game: game)
        chooseWeapon(for: c, game: game)
        switch c.botType {
        case 1: fireDelay = Float.random(in: 3...8)      // Dummy
        case 2: fireDelay = Float.random(in: 1...5)      // Aggressive
        case 3: fireDelay = Float.random(in: 2...6)      // Thinker
        default: fireDelay = Float.random(in: 1...4)     // Crazy
        }
    }

    private func pickTarget(for c: Cannon, game: GameController) {
        let enemies = game.players.filter { $0 !== c && $0.active }
        guard !enemies.isEmpty else { targetIndex = nil; return }
        let previous = targetIndex
        let nearest = enemies.min { dist2D($0.position, c.position) < dist2D($1.position, c.position) }!
        switch c.botType {
        case 4:  targetIndex = enemies.randomElement()!.index                  // Crazy: random each shot
        case 1:  targetIndex = Bool.random() ? nearest.index : enemies.randomElement()!.index
        default: targetIndex = nearest.index
        }
        if targetIndex != previous {
            firstShotAtTarget = true
            lastImpact = nil
            if let t = targetIndex { BotChat.maybeInsult(c, target: game.players[t], game: game) }
        }
        // dead target safety
        if let t = targetIndex, !game.players[t].active { targetIndex = nearest.index; firstShotAtTarget = true }
    }

    // MARK: - Ballistic solution

    private func computeSolution(for c: Cannon, game: GameController) {
        guard let ti = targetIndex else { haveSolution = false; return }
        let target = game.players[ti]
        let dist = dist2D(target.position, c.position)

        // Java resets to a fresh solve whenever the target or the bot itself
        // has moved since the last adjustment (Cannon.java:672-694 zeroes
        // LastHit and the offsets on any position change).
        if !firstShotAtTarget, lastImpact != nil {
            let tm = simd_length(SIMD2(target.position.x - lastTargetPosition.x,
                                       target.position.z - lastTargetPosition.z))
            let bm = simd_length(SIMD2(c.position.x - lastBotPosition.x,
                                       c.position.z - lastBotPosition.z))
            if tm > 0.1 || bm > 0.1 {
                firstShotAtTarget = true
                lastImpact = nil
            }
        }
        lastTargetPosition = target.position
        lastBotPosition = c.position

        if firstShotAtTarget || lastImpact == nil {
            // wind lead: aim point shifted by -Wind * dist * 0.02 (Cannon.java:700-703)
            let aimPoint = target.position - game.wind * dist * 0.02
            let dx = aimPoint.x - c.position.x, dz = aimPoint.z - c.position.z
            solutionAzimuth = G.rad2deg(atan2(dx, dz))
            let dy = target.position.y - c.position.y
            let tiltOffset = (Float.random(in: 0..<1) - 0.1) * 20      // Cannon.java:737-739
            // elevation = line to target + the 45° lob baseline + offset
            // (Cannon.java:731-743; the clamp happens at the slew, Java-style)
            solutionElev = G.rad2deg(atan2(dy, dist)) + 45 + tiltOffset
            solutionPower = min(1, dist / 700)                         // Cannon.java:757-761
            firstShotAtTarget = false
        } else if let impact = lastImpact {
            // walking fire: compare last impact to target (Cannon.java:704-729
            // azimuth, :860-881 power)
            var errFactor = min(dist / 300, 1)                         // Cannon.java:708-711
            // lateral sign: is impact left or right of the firing line?
            let toTarget = SIMD3<Float>(target.position.x - c.position.x, 0, target.position.z - c.position.z)
            let toImpact = SIMD3<Float>(impact.x - c.position.x, 0, impact.z - c.position.z)
            let crossY = toTarget.z * toImpact.x - toTarget.x * toImpact.z
            let lateral = abs(crossY) / max(dist, 1)
            if lateral < 5 { errFactor *= 0.5 }   // |Temp2.X| < 5 → fine azimuth (Cannon.java:712-714)
            // per-type scatter joins the FACTOR, not the step (Cannon.java:715-723)
            if c.botType == 1 { errFactor += Float.random(in: 0..<2) }
            if c.botType == 4 { errFactor += Float.random(in: 0..<1) }
            let azStep = (1 + Float.random(in: 0..<1)) * 4 * errFactor   // Cannon.java:724-728
            solutionAzimuth += crossY > 0 ? -azStep : azStep
            // range: overshoot vs undershoot, measured bot→impact vs
            // bot→target; fine steps once the RANGE error is inside 15
            // (Cannon.java:860-881)
            let impactRange = simd_length(toImpact)   // f5
            var f8: Float = 1
            if impactRange != 0 && abs(impactRange - dist) < 15 {
                f8 = 0.2                                    // Cannon.java:861-864
            }
            if c.botType == 1 {
                solutionPower += (Float.random(in: 0..<1) - 0.5) * 0.2   // Dummy power jitter (Cannon.java:865-868)
            }
            let pStep = (0.05 + Float.random(in: 0..<0.1)) * f8          // Cannon.java:870-873
            solutionPower = min(1, max(0, solutionPower + (impactRange > dist ? -pStep : pStep)))
        }
        haveSolution = true
    }

    // MARK: - Weapon choice (SPEC §9)

    private func chooseWeapon(for c: Cannon, game: GameController) {
        c.weaponIndex = WeaponType.cannonball.rawValue
        guard let ti = targetIndex else { return }
        let target = game.players[ti]
        let dist = dist2D(target.position, c.position)
        func afford(_ w: WeaponType) -> Bool { c.cash >= w.cost }

        let f6: Float = c.botType == 1 ? 0.5 : 1.0   // source: Dummy halves special-weapon odds
        // clear line of sight → Dumbfire. TWO-PHASE like the source: the first
        // pass only ARMS the shot (LineOfSight=true, return and wait a think
        // cycle); the next pass takes it.
        if afford(.dumbfire), lineOfSightClear(from: c, to: target, game: game),
           losArmed || Float.random(in: 0..<1) < 0.75 * f6 {
            if !losArmed {
                losArmed = true
                fireDelay += Float.random(in: 0..<4)   // the extra BotTimeTarget cycle (Cannon.java:766/810-812)
                return
            }
            c.weaponIndex = WeaponType.dumbfire.rawValue
            // LineOfSight drops the 45° lob and the tilt offset: aim straight
            // at the target (Cannon.java:734-742). Power stays on the walked
            // solution (Cannon.java:860-881 still runs for weapon 7).
            solutionElev = G.rad2deg(atan2(target.position.y - c.position.y, dist))
            return
        }
        losArmed = false

        // the special-weapon picks all require a previous impact (Cannon.java:772)
        guard let impact = lastImpact else { return }
        let toTarget = SIMD3<Float>(target.position.x - c.position.x, 0, target.position.z - c.position.z)
        let toImpact = SIMD3<Float>(impact.x - c.position.x, 0, impact.z - c.position.z)
        let crossY = toTarget.z * toImpact.x - toTarget.x * toImpact.z
        let lateral = abs(crossY) / max(dist, 1)
        let impactRange = simd_length(toImpact)
        let lastErr = dist2D(impact, target.position)

        // rolling weapons when nearly on line AND undershooting
        // (Cannon.java:828-841: |Temp2.X| < 10 && f5 < TargetDistance)
        if lateral < 10, impactRange < dist, Float.random(in: 0..<1) < 0.5 * f6 {
            if c.botType != 2, Float.random(in: 0..<1) < 0.1, afford(.bouncer) {
                c.weaponIndex = WeaponType.bouncer.rawValue
                return
            }
            if afford(.spikeroller) {
                c.weaponIndex = WeaponType.spikeroller.rawValue
                return
            }
        }
        // near-miss heavy hitters (Cannon.java:842-858; f7 bias per type :779-799)
        let bias: Float = [4: 30, 2: 10, 1: -5].first { $0.key == c.botType }?.value ?? 0
        if lastErr < 35 + bias, Float.random(in: 0..<1) < 0.5 * f6 {
            if target.position.y < 20, c.botType != 4, afford(.supercrater), Float.random(in: 0..<1) < 0.5 {
                c.weaponIndex = WeaponType.supercrater.rawValue
                return
            }
            if afford(.tnt), Float.random(in: 0..<1) < 0.25 {
                c.weaponIndex = WeaponType.tnt.rawValue
                return
            }
            if afford(.xshot) {
                c.weaponIndex = WeaponType.xshot.rawValue
                return
            }
        }
    }

    /// The Java LoS probe is a collision ray from pos+dir*6 to target-dir*6
    /// over everything (mask 0xFFFFFFF, Cannon.java:800-806): terrain patches
    /// AND prop meshes. Sampled terrain + the prop-mesh segment sweep
    /// approximate it.
    private func lineOfSightClear(from c: Cannon, to target: Cannon, game: GameController) -> Bool {
        let steps = 24
        let start = c.position + SIMD3<Float>(0, 2, 0)
        let end = target.position + SIMD3<Float>(0, 2, 0)
        for i in 1..<steps {
            let t = Float(i) / Float(steps)
            let p = start + (end - start) * t
            if game.world.terrain.height(x: p.x, z: p.z) > p.y { return false }
        }
        if game.world.collideSegment(from: start, to: end) != nil { return false }
        return true
    }

    // MARK: - Per-substep slew (physical aiming animation)

    func slew(for c: Cannon, dt: Float) {
        guard haveSolution else { return }
        // updateLocalClientBot (Cannon.java:496-554): BOTH axes slew linearly
        // at MaxSpinSpeed with an exact snap.
        var delta = (solutionAzimuth - c.spinAngle).truncatingRemainder(dividingBy: 360)
        if delta > 180 { delta -= 360 }
        if delta < -180 { delta += 360 }
        let step = G.maxSpinSpeed * dt
        if abs(delta) <= step { c.spinAngle = solutionAzimuth } else { c.spinAngle += delta > 0 ? step : -step }
        let want = targetActiveTilt()
        let tdelta = want - c.activeTilt
        if abs(tdelta) <= step { c.activeTilt = want } else { c.activeTilt += tdelta > 0 ? step : -step }
    }

    /// The ActiveTilt target: RemoteTiltTarget = -elevation, clamped to
    /// [-MinTiltAngle, MaxTiltAngle] (Cannon.java:750-756). NOTE the original's
    /// clamp is the MIRROR of the player's range: bots can tilt at most 30° UP
    /// and 60° DOWN (this is why the original's bots always lob at the full 30°).
    private func targetActiveTilt() -> Float {
        min(G.maxTiltAngle, max(-G.minTiltAngle, -solutionElev))
    }

    // MARK: - Per-frame think

    func think(for c: Cannon, game: GameController, dt: Float) {
        guard c.active, !c.dying, !game.gameOver else { return }

        if game.currentPlayerIndex == c.index {
            guard !c.hasFiredThisTurn, game.waitingTimer == nil else { return }
            fireDelay -= dt
            guard fireDelay <= 0, haveSolution else { return }
            var azDelta = (solutionAzimuth - c.spinAngle).truncatingRemainder(dividingBy: 360)
            if azDelta > 180 { azDelta -= 360 }
            if azDelta < -180 { azDelta += 360 }
            let tiltReady = abs(c.activeTilt - targetActiveTilt()) < 2
            if abs(azDelta) < 1 && tiltReady {
                c.fire(at: solutionPower)
            }
        } else {
            // Off-turn harassment/defense (Cannon.java AIThink off-turn branch).
            // Source gates the whole block on: aim fully settled, own WaitingTimer
            // zero, no own shot in flight — then rerolls a per-type 1–8 s timer.
            // Without those gates bots lobbed constantly at unsettled angles.
            defenseTick -= dt
            guard defenseTick <= 0 else { return }
            guard haveSolution, game.waitingTimer == nil,
                  !game.projectiles.contains(where: { $0.owner === c }) else { return }
            var azDelta = (solutionAzimuth - c.spinAngle).truncatingRemainder(dividingBy: 360)
            if azDelta > 180 { azDelta -= 360 }
            if azDelta < -180 { azDelta += 360 }
            let tiltReady = abs(c.activeTilt - targetActiveTilt()) < 2
            guard abs(azDelta) < 1, tiltReady else { return }
            switch c.botType {                                   // source reroll intervals
            case 1: defenseTick = 3 + Float.random(in: 0..<5)
            case 2: defenseTick = 1 + Float.random(in: 0..<4)
            case 3: defenseTick = 2 + Float.random(in: 0..<4)
            default: defenseTick = 1 + Float.random(in: 0..<3)
            }
            // 1) molehill lob at the target (source fires weapon 1 with these odds)
            let molehillChance: Float = [1: 0.01, 2: 0.25, 3: 0.125, 4: 0.15][c.botType] ?? 0.1
            if let ti = targetIndex, game.players[ti].active, c.cash >= 520,
               Float.random(in: 0..<1) < molehillChance,
               c.cash >= WeaponType.molehill.cost {
                let keep = c.weaponIndex
                c.weaponIndex = WeaponType.molehill.rawValue
                c.fire(at: solutionPower)
                c.weaponIndex = keep
                return
            }
            // 2) tower if target far above (Cannon.java:944-949)
            if let ti = targetIndex, game.players[ti].active,
               game.players[ti].position.y - c.position.y > 30,
               c.cash >= WeaponType.tower.cost, Float.random(in: 0..<1) < 0.1,
               !game.world.objectAbove(x: c.position.x, z: c.position.z) {
                c.cash -= WeaponType.tower.cost
                c.goldSpent += WeaponType.tower.cost
                // doTower raise, no splat; Island.molehill quakes + rays itself
                game.world.terrain.molehill(x: c.position.x, z: c.position.z, height: 40, radius: 30)
                Projectile.quakeRaysFX(x: c.position.x, z: c.position.z, radius: 30, in: game.world)
                return
            }
            // 3) teleport away from danger (Cannon.java:950-979)
            if findDeathThreat(for: c, game: game) {
                let tpChance: Float = [1: 0.025, 2: 0.1, 3: 0.2, 4: 0.15][c.botType] ?? 0.1
                if Float.random(in: 0..<1) < tpChance, c.cash >= WeaponType.teleport.cost {
                    c.cash -= WeaponType.teleport.cost
                    c.goldSpent += WeaponType.teleport.cost
                    c.playerTeleport()
                }
            }
        }
    }

    /// An enemy's recent impact inside the per-type panic radius (non-team
    /// table, Cannon.java:1403-1431: Dummy 30, Aggressive 40, Thinker 50,
    /// Crazy 40). Aggressive and Crazy additionally retarget the shooter 50%
    /// of the time when the attribution is known.
    private func findDeathThreat(for c: Cannon, game: GameController) -> Bool {
        let threshold: Float = [1: 30, 2: 40, 3: 50, 4: 40][c.botType] ?? 40
        for entry in recentEnemyImpacts where dist2D(entry.pos, c.position) < threshold {
            if c.botType == 2 || c.botType == 4, entry.shooter >= 0,
               entry.shooter != c.index, Float.random(in: 0..<1) < 0.5,
               game.players[entry.shooter].active {
                // Cannon.java:1410-1415 / 1424-1429: lock onto the attacker
                targetIndex = entry.shooter
                firstShotAtTarget = true
                lastImpact = nil
                losArmed = false
            }
            return true
        }
        return false
    }
}
