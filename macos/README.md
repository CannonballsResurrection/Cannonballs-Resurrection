# Cannonballs! (native macOS remake)

A native macOS clone of **Cannonballs!** (WildTangent, 2002) — the turn-based pirate
artillery game. Built as a Swift Package executable using AppKit + SceneKit, with a
SpriteKit overlay for the HUD, from a full reverse-engineering of the original's
decompiled Java source (see the gameplay spec the constants were taken from).

## Run

Requires macOS 13+ and a Swift 5.9+ toolchain (Xcode command line tools).

```sh
cd CannonballsMac
swift run
```

Extra CLI modes (used for development/verification):

```sh
swift run Cannonballs --snapshot "Skull Isle" out.png   # render one frame of a map to PNG
swift run Cannonballs --simulate Tropicali 600          # headless bot-vs-bot match, prints stats
swift run Cannonballs --uitest                          # opens app, saves menu/HUD screenshots, quits
python3 tools/gen_sfx.py                                # regenerate the synthesized SFX WAVs
```

## Controls

| Key | Action |
|---|---|
| ← / → | Spin cannon (ramped, 100°/s max) |
| ↑ / ↓ | Tilt barrel (−30° … +60°) |
| SPACE | 1st press: start oscillating power bar · 2nd press: fire (bar hitting 0 auto-fires) |
| + / − | Next / previous weapon |
| V | Cycle camera (Cannon / Shot / Medium / High / Barrel) |
| Q | Forfeit |
| Esc | Back to menu |
| Mouse | Weapon dropdown, menus |

Off-turn you are in **DEFENSE MODE**: you can aim and fire, but only defensive
items (Molehill, Tower, Teleport, TargetTeleport).

## Faithful to the original (constants from the decompiled source)

- Physics: gravity −32, wind as constant acceleration (`sin/cos(dir)·vel·0.2`,
  vel 0–79 "mph", rolled once per match), launch speed `(power+0.5)·100` from
  muzzle+5u, substeps ≤ 40 ms (max 5).
- Terrain: 96×96 heightmap (640×640 world units), deformation as linear cones
  eased at 30 u/s, per-weapon crater depths/radii, molehills, X-Shot grooves.
- All 12 weapons with exact prices and behaviors (Bouncer restitution −0.85,
  Dumbfire 7 s no-gravity rocket, SpikeRoller ground-roll at 20% wind, plantable
  TNT with chaining shockwave, Teleport/TargetTeleport, Tower, etc.).
- Rules: one-hit kills (8 u radius), drowning when the ground under you reaches
  sea level, killer loots the victim's full purse, death resets cash to half the
  *starting* stake, lives/respawns (4 s), 3 s turn-end wait, hotseat timers
  (NA/20/30/60/90/120 s), chests 3–7 per island with the original 30/30/20/10/5/5
  loot table (including the cursed teleport chest), last-cannon-standing wins.
- The 4 bot personalities (Dummy / Aggressive / Thinker / Crazy) with wind-lead
  aiming (−wind·dist·0.02), 45°-lob first solutions, walking-fire bracketing,
  weapon-purchase heuristics and defensive teleports.
- All 11 original islands: heightmaps, terrain textures, object layouts, water/
  fog/sun colors from `maplist.dat`, original map thumbnails in the menu.
- Original soundtrack MP3s (fan-ripped): title/lobby theme on the menu, per-map
  TRACK1/TRACK2 in game.
- **Original sound effects** — the 16 in-game SFX are the real decoded
  originals from the `.wwv` assets (cannon fire, explosions, cash, splash,
  teleport, quake, tilt/turn servos, timer, UI blips), not synthesized. The
  WLD3 container codec was cracked; see `../WLD3_CODEC_CRACKED.md`. Full decoded
  set (26 SFX + 3 music) is in `../decoded_original_assets/`.

## Recreated (originals unrecoverable)

- **3D models** (`.wsad`): cannons, palms, tikis, huts, lighthouse, obelisks,
  ship, chest, projectiles are procedural SceneKit geometry sized from each
  prop's original `prop.dat` radius/height. (These *are* decodable to the PWT
  geometry format via the same cracked container — see the codec writeup — but
  are not yet imported; procedural stand-ins remain for now.)
- **Sky domes**: original sky textures were proprietary; skies are per-map
  gradients using the map's ambient/sun colors (+ sun/moon billboards).
- HUD art is redrawn in the original's purple/gold palette with system fonts.

## Known gaps

- Local play only: no networking, lobby, accounts, chat window, tips ticker, or
  online leaderboards. Multiplayer = hotseat humans + bots (max 4 players; the
  original allowed 8 + team play).
- Standable props (Bridge, Mound, Ship deck) block shots but you can't stand a
  cannon on top of them (original return-code-5 "Standable" behavior).
- Bot canned chat lines are not shown (no chat window); off-turn bot molehill
  building is simplified away (teleport + tower are implemented).
- Barrel-camera reticle, pause, fullscreen toggle, spectator mode, and the
  in-game menu bar (Quit/Options/Camera/Help) are omitted.
- TNT's double shockwave is approximated by a single expanding kill radius
  (grows to the same scale-50 reach, same loot semantics).
- The Shot camera chase is simplified (single chase speed ramp).

## Layout

```
Package.swift
Sources/Cannonballs/            Swift sources (app, engine, game rules, AI, UI)
../shared/Resources/            platform-neutral game assets (maplist.dat,
                                MAPS/<11 islands>, MODELS/, MUSIC/, SFX/,
                                IMAGES/, HELP/ …) shared with the Windows build;
                                Assets.swift finds them at runtime (app bundle
                                first, then walking up to shared/Resources)
tools/package_app.sh            assemble Cannonballs.app (binary + shared/Resources)
                                (SFX synthesizer moved to ../tools/gen_sfx.py)
snapshots/                      dev renders (gitignored)
```
