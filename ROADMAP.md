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
1. **VIPM LODs** — the meshes are progressive; could honor collapse records for
   distance LOD (cosmetic; not needed for correctness — parked by choice).
2. **Polish queue** — whatever the next playtest surfaces. The fidelity matrix is
   the work queue; keep the "known approximations" table honest.
3. **Networking (out of scope)** — the original WT matchmaking service is dead;
   local hotseat + bots stand in. Revisit only if a LAN protocol reimplementation
   ever becomes interesting.

## Repo structure decision (2026-07-09 — do not relitigate)
One monorepo, multiple build directories (`macos/`, `windows/`), with all
research, specs, tools, and the decoded `shared/Resources/` asset tree at the
top level serving every build. Builds share no code — they share the
reverse-engineering corpus, and splitting repos would make every spec fix and
asset re-export a multi-repo chore. If a concrete forcing reason ever appears
(open-sourcing one build separately, a collaborator who shouldn't get the whole
archive), split then with `git filter-repo`, which preserves history.
