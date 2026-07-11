# Roadmap — Cannonballs! native remake (macOS + Windows)

Living list of what's done and what's next. See `HANDOFF.md` for full state + dead ends.

## Done ✅
- Installer + all assets extracted; gameplay spec reverse-engineered.
- **Audio codec cracked** — all SFX + music decoded and playing.
- **Textures decoded**; props re-proportioned + skinned.
- 11 islands (real heightmaps/textures/objects), physics, weapons, bots, camera, menu bar.
- **`.wsgo` mesh format CRACKED** (byte-planar 16-bit dequant) — real meshes in the clone.
- **`.wsmo` motion + skeleton/skin CRACKED** — chest hop/rattle, cannon recoil squash,
  lighthouse beam sweep play from the real motion data (SkinnedModel/SCNSkinner).
- **`.wjp` image format CRACKED** — WLD3-wrapped JPEG; all 4 menu backdrops decoded
  and wired into the menu screens.
- **Full original menu/lobby layout** — reference-fitted from the gameplay video and
  the 800x600 screenshot (UI_TRANSCRIPTION.md; v1.869 lobby, dropdowns, preview).
- **Per-material model export** — solved-pipeline `skinned.json` "parts" format, one
  texture per geom material (lighthouse rail/light/body, tiki faces, etc.).
- **Faithful HUD from the original sprite art** — power/pitch bars, weapon dropdown,
  turn list, treasure-map minimap (rust-stain island + MAPBITS icons), coin, wind bar,
  Trebuchet bitmap font, fade-up game messages, typed chat (C key, wrap, cursor).
- **Overlay alpha bug root-caused** — SceneKit sRGB-decodes overlay texture alpha;
  HUDArt compensates at load so translucent HUD art matches the 2002 blend.
- **Game-over sequence** — elimination/You Lose!/You Win! banners, forfeit, results.
- **Help! + Options menus** — original HTML help pages (bundled, open in browser);
  Shadows/Sound/Music toggles with the original dependency rules, persisted.
- Full particle/FX suite, world dressing, skyboxes, bot AI cascade + chat tables —
  see `format-research/FIDELITY_MATRIX.md` (every row ✅ or justified n/a).
- WTExtractor v200 patch upstreamed (PR #3); decoder DLLs recovered from the XP image.
- **Windows build shipped** — full Godot 4 (GDScript, `gl_compatibility`) port,
  feature-matched file-for-file against the same Java reference and
  `shared/Resources/` tree; verified end-to-end under Wine (`windows/PORTING.md`).
- **Java-fidelity back-port to macOS** — all ~57 corrections found during the
  Windows-port audit re-verified against `source/*.java` and applied to the Swift.
- **Packaged installers + downloads** — self-contained `Cannonballs-macOS.dmg`
  (drag-to-install, syncs the local copies via `macos/tools/make_dmg.sh`) and
  `Cannonballs-Windows.zip`, published on the GitHub **Releases** page. Build
  artifacts are gitignored and shipped via Releases, not committed.

## Roadmap 🗺️
1. **Team Play mode** — the original's 2/3/4-team mode (Blue/Purple/Red/Green):
   team turn order + win condition, team-colored cannons, "<Color> Team" turn
   banner, and target-plate team flags. Currently the settings row is stubbed to
   a dimmed "NA". Fully scoped from `source/*.java` (adapted to the local
   hotseat+bots model) — see [`docs/plans/team-play.md`](docs/plans/team-play.md).
2. **VIPM LODs** — the meshes are progressive; could honor collapse records for
   distance LOD (cosmetic; not needed for correctness — parked by choice).
3. **Polish queue** — whatever the next playtest surfaces. The fidelity matrix is
   the work queue; keep the "known approximations" table honest.
4. **Networked multiplayer** — revive online/LAN play. The original WildTangent
   matchmaking + DirectPlay stack is dead and unrecoverable, so the transport is
   necessarily new, but the player-visible behavior (host/join, lobby, the
   host-authoritative turn model, the 34-opcode event protocol, chat, disconnect
   handling) is fully reverse-engineered and portable. Recommended path: event
   replication over a versioned platform-neutral format → Phase 1 LAN/room-code
   with a `--nettest` loopback harness → Phase 2 a small WebSocket relay for
   NAT-free internet play → optional accounts/leaderboards and Godot cross-play.
   Local hotseat + bots stand in until then. Fully scoped from `source/*.java`
   (`Network.java`/`Packet_Manager.java`) — see
   [`docs/plans/networking.md`](docs/plans/networking.md).

## Repo structure decision (2026-07-09 — do not relitigate)
One monorepo, multiple build directories (`macos/`, `windows/`), with all
research, specs, tools, and the decoded `shared/Resources/` asset tree at the
top level serving every build. Builds share no code — they share the
reverse-engineering corpus, and splitting repos would make every spec fix and
asset re-export a multi-repo chore. If a concrete forcing reason ever appears
(open-sourcing one build separately, a collaborator who shouldn't get the whole
archive), split then with `git filter-repo`, which preserves history.
