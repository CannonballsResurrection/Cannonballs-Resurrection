# Fidelity matrix — every source element vs the clone

The systematic audit: every asset the game loads (grepped from all
`Media_Object_*` instantiations in `source/`), every particle class, every sound,
and every subsystem — each with its clone status. **Nothing counts as "done"
unless it is ✅ here against the source, not against intent.** Placeholders must
appear here as ~ or ✗, never silently.

Legend: ✅ exact/derived from source · ~ approximated (works, not faithful) ·
✗ missing · ◐ partial · n/a (multiplayer/online only)

**Evidence levels** (PROCESS_RETRO rule 1): rows verified against an ORIGINAL
image/video carry (V); source-derived-only rows carry (S). References live in
`originals/` (60 video frames + the 800x600 screenshot); comparison artifacts
in `refchecks/`. Currently (V)-verified: cannons, palms, chest, water, sky,
HUD (topbar/gold/lives/minimap/queue/tags/chat/wind mph), text rendering,
lobby, settings popup, explosion/smoke/coin sprites (visible in video), the
wind arrow, cursor scale, iris transition. Everything else is (S) — correct
per decompiled source + decoded assets, no original image showing it yet
(notably: night maps, most weapon projectiles in flight, teleport/quake
effects, results screen, prop destruction debris, lens flare).

## 3D actors (models)

| Source asset | Used for | Status |
|---|---|---|
| OBJECTS/CANNON barrel+base+stone | player cannons | ✅ all four parts via the solved exporter at native scale; barrel uses IMAGES/CANNON player-TINTED (gray body takes color, gold trim keeps shine, skull muzzle cap) + REFLECTION layer; stone column buried per authoring |
| OBJECTS/CHEST | treasure chest | ✅ skinned mesh + full DECODED `loop` motion (hop + lid rattle, all 6 bone paths via SCNSkinner) |
| OBJECTS/ARROW | wind indicator | ✅ real decoded mesh via the solved exporter (old OBJ route mis-assigned the material -> black); quad fallback kept |
| OBJECTS/MORTAR | X-Shot + Dumbfire shells | ✅ decoded model; the cannonball FAMILY is 2x2 WEAPONS-sheet billboards per Weapon.java (black/blue/purple balls) — playtest fix |
| OBJECTS/BOUNCEBALL | bouncer projectile | ✅ decoded model wired |
| OBJECTS/SPIKEBALL | spikeroller projectile | ✅ decoded model wired |
| OBJECTS/TNT | TNT projectile + prop | ✅ decoded model for both |
| MENUS/TRANSITION | screen transition anim | ✅ actor mesh probed (flat ±1397-unit sheet, hole-rim verts r≈2.5): a black sheet with a soft-rimmed HOLE, not a solid disc. Iris CLOSES (scale 1.0→0.001, Menu_Manager.java:246-292,432-445) then OPENS, 1s each (IrisTransition.swift) |
| MENUS/WT/intro.wsad | WildTangent logo intro | n/a by choice — third-party branding of a defunct company; deliberately omitted (LEGAL.md posture). The animation.wsmo is decoded and documented regardless |
| SKIES/* actors | sky dome + horizon islands | ✅ all 5 sky actors exported (dome mesh + horizon-island billboards + moon + star layer + outcrops, authored positions) rendered as camera-following skybox over the panorama |
| PROPS/* (16 props) | island props | ✅ decoded meshes; PALM re-exported via the solved pipeline (old OBJ UVs were V-flipped -> yellow fronds; now the real green layered fronds) |

## Sounds (source loads 24 + title; clone bundles 16)

| Source | Clone | Status |
|---|---|---|
| CLICK, CASH, DRUMROLL, QUAKE, SPLASH, TELEPORT, TIME_UP, TIMER, EXPLOSION1/2/3, TURN_LOOP | same (timer=timer_tick) | ✅ |
| CLANG (bouncer) | `bounce` | ✅ (renamed) |
| LAUNCH_CANNON | `cannon_fire` | ✅ (renamed) |
| LAUNCH_MISSILE (dumbfire) | `launch_missile` | ✅ wired |
| OVER (button hover) | `hover` | ✅ plays on menu rollover |
| TILT (aim pitch) | `tilt` | ✅ loops while tilting |
| TURN_STOP (spin stop) | `turn_stop` | ✅ plays when spin stops |
| OCEAN (ambient loop) | `ocean` | ✅ loops in-game |
| GRIND (spikeroller) | `grind` | ✅ (was misusing turn_loop) |
| HUM (X-shot/bouncer launch) | `hum` | ✅ wired (cases 5/6) |
| PUFF (placement), SPLAT (terrain impacts), WHISTLE (shot flight) | bundled + wired | ✅ · THUMP: loaded but NEVER PLAYED in the original source (dead asset) — n/a |

## Particles / effects (source has 18 classes)

| Source | Status |
|---|---|
| Explosion1 (animated sheet), Smoke, SmokePuff | ✅ original sheets + decompiled physics (FXSprites.swift) |
| Splash, SplashDrop, SplashRing | ✅ SPLASH droplet jets + SPLASHRING planes, Weapon.java recipes |
| Coin (chest burst) | ✅ animated COIN sheet, 20fps spin, gravity/bounce, water rings, 30-coin burst |
| Chunk (dirt chunks, 2 types) | ✅ CHUNKS sheet frames 2+3, drag 0.99, rests/drowns per source |
| Cloud | ✅ 30 SMOKEPUFF billboards in the authored ellipsoid (radius/xMul/yMul map params now read correctly) |
| Teleport | ✅ smoke column + 12-STAR ring + 12 sparkles + 1.5s ray spiral (500 deg/s) |
| Shockwave | ✅ SHOCKWAVE sprite ring, growth synced to the detonation kill radius |
| FireTrail | ✅ ballistic smoke-shedding embers (cannon death x3, misc); dumbfire in-flight trail = 30% smoke + 40% mini-fireball per frame (Weapon.java:70-79) |
| **Fish (ambient ocean leap)** | ✅ 0.0075/frame gate, transcribed launch velocity, animated FISH frames, splashes |
| Firefly (night maps) | ✅ <FIREFLY> map entries parsed; glowing drifting sprite |
| Sparkle, Star, Ray, SmokeColumn, Decoration | ✅ all five: FIREFLY-glow sparkles, STAR sheet w/ fade+smoke-out, ground light rays (quake craters + teleport), SmokeColumn emitter (fire mode incl.), decorations terrain-follow + drown in smoke |
| Entity_Object_LensFlare (SUN/FLARE/CIRCLE1/2/DOT1/2) | ✅ all 6 elements, axis offsets/opacity per source, terrain occlusion march w/ 4/s fade |

## World rendering

| Source | Status |
|---|---|
| WATER + WATERANIMATION (animated water texture) | ✅ 32-frame tile cycle @0.08s + tint + UV scroll |
| SHORELINE (animated shore band) | ✅ two tilted bobbing pulsing additive water planes (1.2x/1.15x; SHORELINE base is pure black in the assets) |
| SHADOW (blob-shadow patches under chest/cannon/projectiles) | ✅ SHADOW alpha patches under cannons/chests/projectiles; shadow-mapping OFF (2002 engine had none) |
| CLOUDSHADOW | ✅ multiply shader layer, 4x tiling, wind-driven drift |
| SCORCH / SPLAT decals | ✅ both baked into the terrain map: SCORCH burn (blend-to-black) + SPLAT colorized purple for crater weapons, with quake sound + 30 ground rays |
| REFLECTION (cannon env map) | ✅ original REFLECTION texture as a subtle spherical env layer (intensity 0.18) on base + barrel |
| GRIT | ✅ 40x-tiled detail-modulate layer in the terrain shader (mean-normalized) |
| Sky panoramas | ✅ decoded, wrapping background |
| Renderer (DX7 rules: 565+dither, bilinear, Gouraud, no-cull, fog) | ✅ as designed: the DX7-rules software rasterizer is built, hardened vs all current content (skips additive effects; skinned actors at bind pose), and shippable as an alternate view; SceneKit remains the default by project decision (performance + platform integration) |

## UI (post-transcription)

| Element | Status |
|---|---|
| Font metrics/colors/sizes, power/pitch bars + bone markers, weapon dropdown, coin/cash/lives, minimap + MapBits player arrow, chat, banners, name tags, popup/settings/lobby layouts, cutlass cursor, reticle, top menus | ✅ transcribed |
| HOURGLASS (waiting indicator) | n/a — shown only during load waits (Menu_Manager.showLoading); the clone loads instantly |
| FLAGS (team colors), ICONS (ready/away) | n/a (team/online) |
| Camera: 5 modes + barrel reticle | ✅ |

## Mechanics

Constants + weapon physics transcribed bit-exact (WEAPON_SPEC.md). Bot AI lives
in Cannon.java's AIThink (there are no separate Bot_*.java files); the clone's
BotAI carries the source constants (wind lead -0.02, tilt +45 blind-fire offset,
spin step (1+r)*4*errFactor, power step 0.05+r*0.1 with 0.2 damping, per-type
think cadences, f6=0.5 Dummy accuracy factor, off-turn molehill odds
0.01/0.25/0.125/0.15, teleport-threat odds 0.025/0.1/0.2/0.15, the two-phase
LineOfSight arm-then-fire trick, and the exact weapon cascade incl. the
Crazy-bot supercrater exclusion). No known remaining gaps.
Bot chat ✅ — all five VERBATIM tables (greeting/insult/death/kill/compliment,
Network.java:71-80) with the original trigger probabilities (BotChat.swift);
including the quirk that ComplimentCount=5 leaves "That was sweet" unreachable.

## Motion (.wsmo)

✅ SOLVED end-to-end (2026-07-09). Full format decoded (MOTION_FORMAT.md):
all bone paths (translation vec3 + quat WXYZ tracks, raw and uniform-dt
compressed), the geom skeleton (parent hierarchy + inverse-bind matrices), and
per-vertex matrix-palette skin weights. Shipped playing in the clone via
SCNSkinner: CHEST `loop` (hop + lid rattle, all 6 bones), CANNON `fire`
(barrel recoil squash on every shot), LIGHTBEAM `loop` (13.3s lighthouse
sweep on Moonlight Cove). The 4th motion (WT splash `animation`) is the
WildTangent logo, not part of gameplay. Note: the software rasterizer view
renders skinned actors at bind pose (no CPU skinning path).

## Playtest QA round (2026-07-09, from an original-vs-clone screenshot)

An original 800x600 gameplay screenshot (cannonballs.jpg) drove 14 fixes:
64px native cursor; iris+hourglass after name entry; lobby = names only
(colors were an invention — removed), kick gated on AI assignment, cannon
preview on its own dark scene per Menu_Lobby_Screen.java (not live gameplay),
doubled dropdown arrow removed; cannon rebuilt from parts with the tinted
IMAGES/CANNON skin; text tints purged (tinting flattened the baked outline —
the 3 original sheets white/blue/gray only); water full-tint constant-lit at
70x; wind arrow 2x; live rotating minimap arrow (Cannon.java:409); original
MAP parchment; WEAPONS-sheet cannonballs; solved-pipeline palms.

## Known approximations (honest ~ list, per the no-silent-invention rule)

| Stand-in | Where | Why |
|---|---|---|
| Kick button art | lobby | original uses a Controls-sheet crop (57,154,45x26); clone draws a text bar "Kick" |
| Deep-water backstop disk | World.buildWater | invention: hides see-through ocean at glancing angles; original relied on the sky dome below the horizon |
| Gradient-sky fallback | World.buildSkyAndLights | only used if SKIES assets fail to load |
| Procedural prop stand-ins | PropGeometry | now FALLBACKS only (every prop has a solved-pipeline export); kept for resilience |
| Hourglass frame grid | IrisTransition/MenuScene | 4x2 cell guess on the 256px sheet (6 frames confirmed; per-frame rects unverified) |
| Career TOTAL stats | results screen | original pulled account stats from the dead service; clone persists local career totals |
| Cannon tint gain 1.55 + reflective 0.45 | Cannon.tintedSkin | stands in for the DX7 additive reflection layer; tuned to the (V) references |
| Minimap restain on terrain deform | HUDScene.drawIslandStain | original bakes the stain once and only erases sunken cells (Island.java:547); clone recomputes from live heights with fixed per-cell noise — same water-erase result, but raised terrain restains |
| Overlay alpha compensation exponent 2.2 | HUDArt.overlayAlphaCompensate | SceneKit's overlaySKScene sRGB-decodes texture alpha (measured: out = enc(dec(dst)·(1−dec(a)))); compensation a' = enc(1−(1−a)^2.2) matches the 2002 sRGB-space blend within ~3/255 across the measured range |
| Chat always expanded, no scrollbar | HUDScene chat | original had minimized (3-line) + expanded (14-line) states with a drag scrollbar (Chat.java ChatTopLine); clone keeps the expanded panel and drops history past 14 wrapped lines |
| Help pages open in system browser | HUDScene.openHelp | original called the host page's launchHelpPage (browser embed); clone opens the same bundled HTML in the default browser |

## Process rule (why this file exists)

Fidelity work was complaint-driven; placeholders silently became "done." From now
on: (1) any stand-in must be logged here as ~/✗ when written; (2) a feature is ✅
only when checked against source/video, not against intent; (3) this matrix is
the work queue — highest-visibility ✗/~ first.
