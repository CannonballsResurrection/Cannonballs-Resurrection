# Known issue: game stuck on an NPC (bot) turn

**Symptom.** The game freezes on a computer player's turn. The HUD keeps showing
"`<Bot>`'s Turn", the barrel never fires, and the turn never passes to the next
player. Not every game, so it is data/timing dependent.

## Was there a timeout in the original? (short answer: not for bots)

The original *does* have a turn timeout, but it only covers the **local human**
hotseat turn (`Timer.update`, `Timer.java:35-59`, auto-passes with
`PACKET_SWITCH_TURN` code 4 + "Time Up!"). It ticks only when the current player
is your own `PlayerNumber` (`Game_Loop.java:512`). During a bot's turn it does
not run. **The original had no watchdog on AI turns at all** — it relied on the
bot structurally always firing.

Why the original never hangs on a bot: `AIThink` runs **every frame** and

- re-acquires a target whenever it has none (`Cannon.java:644-665`),
- drops a target that has gone inactive and re-picks next frame
  (`Cannon.java:667-669`),

so a bot always converges onto a live enemy and always fires; once the shot
resolves, `Weapon.endUserTurn` sets `WaitingTimer = 3` and `updateWaitingTimer`
calls `switchPlayers` (`Weapon.java:280-282`, `Cannon.java:284-297`). No stuck
state is reachable.

## What it is NOT (ruled out)

These were checked and are sound in the macOS port — do not chase them again:

- **Aiming convergence math.** The port does *not* use the original's exact
  float `==` fire gate; it uses tolerance gates (`abs(azDelta) < 1`, tilt `< 2`,
  `BotAI.swift:255-256`) and an exact-snap slew (`BotAI.swift:229,232`).
  `solutionAzimuth`/`solutionElev` are computed once in `beginTurn` and held
  stable for the turn, and the slew snaps exactly onto them, so azimuth and tilt
  always reach the fire gate.
- **Projectiles never resolving.** Every weapon type has a guaranteed
  terminator: ballistic shots fall under gravity to `position.y <= ground`;
  dumbfire caps at 7 s (`Projectile.swift:359`); spikeroller at 25 s
  (`Projectile.swift:520`). So a fired shot always dies and always sets
  `waitingTimer` (`GameController.swift:162-165`).
- **Turn flags not resetting.** `beginTurn` clears `hasFiredThisTurn` /
  `firedOnTurn` for the new current player (`GameController.swift:369-370`), and
  `waitingTimer` always decrements to `switchPlayers` (`GameController.swift:522-529`).

## Root cause (the fidelity divergence)

The port computes the target + firing solution **once**, in
`BotState.beginTurn` (`BotAI.swift:35-46` → `pickTarget` → `computeSolution`),
and never retries. `computeSolution` sets `haveSolution = false` when there is no
target (`BotAI.swift:69-70`). If a bot's turn *begins* without a valid target — a
transient race, e.g. the intended enemy is inactive/dying on that exact frame —
`haveSolution` stays `false` forever:

- `slew()` returns immediately (`guard haveSolution`, `BotAI.swift:222`), so the
  barrel never moves;
- the on-turn fire path is guarded off (`guard fireDelay <= 0, haveSolution`);
- nothing ever fires, so `waitingTimer` is never set and the turn never switches.

The original recovers the next frame; the port had no retry. That missing
per-frame re-acquisition is the bug.

## The fix applied (fidelity-correct)

`macos/Sources/Cannonballs/BotAI.swift`, in `think(...)`, on-turn branch: before
the fire logic, mirror the original's per-frame behavior — drop an inactive
target and re-solve whenever there is no usable solution:

```swift
if let ti = targetIndex, !game.players[ti].active { targetIndex = nil; haveSolution = false }
if !haveSolution {
    pickTarget(for: c, game: game)
    computeSolution(for: c, game: game)
}
```

This is faithful (it is literally what `AIThink` does every frame,
`Cannon.java:644-669`), and it self-heals the stuck state. It runs only when
there is no usable solution, so it never disturbs the once-per-shot walking-fire
aim.

## If it still hangs: the interpolation fallback (NOT yet applied)

If stuck bot turns persist after the fix above, the cause is some *other* gate we
have not reproduced (e.g. a pathological game-over race, or `waitingTimer` wedged
from an instant weapon). The catch-all is a per-turn deadline that force-advances
the turn. **This is an INTERPOLATION** — the original had no AI-turn watchdog
(see top) — so it must be commented as such at the use site and treated as a
safety net, not fidelity.

Sketch (add to `GameController`, driven from the per-frame update):

```swift
// INTERPOLATION: the original had no bot-turn watchdog (Timer.java only guards
// the human hotseat turn, Game_Loop.java:512). This is a safety net against an
// unreproduced stuck state, not original behavior. Tune the deadline generously
// so it never clips a legitimately slow bot (max fireDelay is ~8 s + the LoS
// arming cycle up to ~4 s + slew/settle, so ≥ 20 s is safe).
private var botTurnWatchdog: Float = 0
// in update(), when the current player is a bot and no shot is in flight and
// waitingTimer == nil:
botTurnWatchdog += dt
if botTurnWatchdog > 20 {
    botTurnWatchdog = 0
    hud.flashMessage("Time Up!")   // reuse the original's timeout affordance
    currentPlayer.powerBarActive = false
    switchPlayers()                // force forward progress
}
// reset botTurnWatchdog = 0 in beginTurn() and whenever the bot fires.
```

### Diagnostic to capture the next hang

Cheapest way to learn *which* gate sticks next time: when the watchdog above
would trip, log the four fire-gate conditions before force-advancing —
`haveSolution`, `targetIndex`, `azDelta`, `abs(activeTilt - targetActiveTilt())`,
`game.waitingTimer`, and any in-flight projectile owned by the bot. That tells us
whether it is the target/solution path or the controller turn-state, so a future
fidelity fix can target the real gate instead of masking it with the watchdog.
