# Weapon physics — bit-exact spec (from Weapon.java + Global.java, v1.87)

Every value below is transcribed from the decompiled source and matched in the
clone (`macos/Sources/Cannonballs/{Projectile,Types}.swift`).

## Shared trajectory (all projectiles)

```
pos   += traj * dt
traj.x += windX * dt
traj.y += -32 * dt        // GRAVITY
traj.z += windZ * dt
```
Muzzle: fired from `cannonPos + dir*5` at speed `(power + 0.5) * 100`.

## Hit model — `checkForHit(x,y,z, f4, f5, f6)`

- `f4` = cannon kill radius, `f5` = chest radius, `f6` = prop radius.
- A kill only happens when `Global.PROJECTILEIMPACT[type]` is true
  (cannonball, X-shot, bouncer, dumbfire, spikeroller, TNT). Crater/molehill/
  supercrater/tower/teleport call it but never kill — they only shape terrain.
- Normal projectiles: `(8, 6, 2)`. TargetTeleport: `(12, 10, 6)`.

## Per-weapon effects

| # | Weapon | Cost | Effect |
|---|---|---|---|
| 0 | Cannonballs | 0 | kill r8; crater(depth 4, radius 20) |
| 1 | Molehill | 20 | molehill(height 10, radius 20) — raises terrain |
| 2 | Crater | 20 | crater(10, 30) — lowers terrain |
| 3 | Tower | 200 | instant molehill(40, 30) under self |
| 4 | Supercrater | 200 | crater(20, 50) |
| 5 | X-Shot | 300 | crater(4,20) + 2 grooves ±45° len 32 (r4,d8) + **2 kill-lines ±45° len 30, width 10** |
| 6 | Bouncer | 300 | reflect `traj.y *= -0.85` at terrain+2.5; detonate crater(4,20) when `traj.y < 0.01` |
| 7 | Dumbfire | 400 | crater(6, 20) |
| 8 | SpikeRoller | 400 | rolls along terrain, xCrater(4,20) grooves |
| 9 | TNT | 500 | plants a TNT barrel prop (detonates later) |
| 10 | Teleport | 500 | instant self-teleport |
| 11 | TargetTeleport | 800 | teleport to impact; kill r12 |

`crater(x, z, depth, radius)` — verified: the source's 4th arg is the horizontal
radius (`if (dist < f4)`), 3rd is depth.

## Fixes made during transcription

- `impactKill` excluded supercrater (matches `PROJECTILEIMPACT`).
- Bouncer decay threshold `< 8` → `< 0.01` (many small bounces, like the original).
- X-shot kill-lines shortened 32 → 30 (grooves stay 32).
