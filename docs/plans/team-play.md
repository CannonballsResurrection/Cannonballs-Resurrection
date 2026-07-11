# Team Play — implementation plan

## Context

The "Team Play" row in the New Game Settings modal is present but hardcoded to a
dimmed **"NA"** (`MenuScene.swift:567`), and the engine has zero team support, so
selecting it does nothing. That is the "isn't working yet" the user hit.

The original 2002 game shipped a full Team Play mode: 2/3/4 teams (Blue, Purple,
Red, Green), team-based turn order and win condition, team-colored cannons, a
"<Color> Team" turn banner, and team flags on the target name-plates. All of it
is transcribable from `source/*.java` (single source of truth per the Fidelity
Doctrine). The port is **local-only** (hotseat humans + bots); the original's
networking (`Network.java`/`Packet_Manager.java` packet round-trips) is not
ported, so team logic is adapted to the local model by reusing the Java **rules**
and dropping the packet plumbing. Goal: make Team Play selectable and make the
match actually play as teams, faithfully.

Verified prerequisites: color table `G.colorRGB[0..3]` is already
blue/purple/red/green in the exact `TEAMNAMES` order (`Types.swift:16-21` ==
`Global.java:292`), so **team index == color index** (no mapping table). The
flag sheet exists: `shared/Resources/HUDART/flags.png` (48×48, four 24×24
subrects). Friendly fire is deliberately ON (see Stage 9).

## Key source citations (all verified)

- Constants: `Global.java:23-26,127-131,292-299` — `TEAMNAMES=["Blue","Purple","Red","Green"]`,
  `TEAMCOUNT=[_,2,3,4]`, `TEAMDESCRIPTION=["NA","2","3","4"]`, `TEAMPLAYERREQUIREMENT=[1,2,3,4]`.
- Cannon color = team in team mode: `Game_Loop.java:57-58` (`cannon[n].Color = PlayerTeam[n]`).
- Team assignment/balancing: host→team 0 (`Network.java:651-653`); each added player→`findLowestTeam()` (`Network.java:450-466`, empty team wins else fewest); `addBot` increment (`Network.java:1112-1114`).
- Turn cycling: `Game_Loop.java:388-415` (advance `ActiveTeam` to next team with active players, then `CurrentPlayer` to next active cannon on that team after `LastTeamPlayer`).
- Win: `< 2` teams have active players — `countTeamsWithActivePlayers()` (`Network.java:934-945`), used in `Game_Loop.java:388-390` + `Packet_Manager.java:417`. Winner text `TEAMNAMES[findWinningTeam()] + " Team Wins!"` (`Packet_Manager.java:422`, `Network.java:869-884`).
- Turn banner: second queued message `TEAMNAMES[ActiveTeam] + " Team"` (`HUD.java:715-717,757-759`).
- Target-plate flags: `HUD.java:266,450-467` — 24×24 crop from `flags.png` at team subrects (0,0)/(0,24)/(24,0)/(24,24), left of the name bar, hides with the plate.
- Settings gating: team-up raises max players to `TEAMPLAYERREQUIREMENT[sel]` (`Menu_Lobby_Screen.java:786-792`).
- **Friendly-fire asymmetry (the critical fidelity point):**
  - Bot *targeting* is team-aware: `Cannon.java:155` (`PlayerTeam[n] != PlayerTeam[Owner]`), `findDeathThreat` breaks on same-team shooter (`Cannon.java:1435`).
  - *Damage* is team-blind: `Weapon.java:1042` / `:1102` gate only on `n != Owner` — **no team check**. A shot landing on a teammate kills it. Preserve this.

## Approach & staged changes

### Stage 1 — Data model (`Types.swift`)
- `PlayerConfig` (`:94-98`): add `var teamIndex: Int = 0`.
- `GameOptions` (`:100-107`): add `var teamCount: Int = 1`; computed `var teamGame: Bool { teamCount > 1 }` (mirrors `Menu_Lobby_Screen.java:92`).
- `enum G`: add `teamNames`, `teamDescription`, `teamPlayerRequirement` verbatim from `Global.java:292/298/299`, each cited.

### Stage 2 — Cannon carries a team (`Cannon.swift`)
- Add `let teamIndex: Int` from `config.teamIndex` in `init` (`:167`).
- Color = team in team mode (`Game_Loop.java:57-58`): handled by Stage 4 setting `colorIndex = teamIndex` for all players when `teamGame`, so `tintedSkin`/minimap/swatches follow with no branching. Comment the equivalence at the use site (flag it as our single-field representation of the source's `Color = PlayerTeam`).

### Stage 3 — Settings row (`MenuScene.swift`)
- Add `private var teamSelection = 0` (`Menu_Lobby_Screen.java:53`).
- `refreshSettings()` (`:567`): replace hardcoded NA with `setValue("team", G.teamDescription[teamSelection], dim: teamSelection == 0)`.
- `handleSettingsClick` (`:672-688`): the `m-team-up`/`m-team-down` arrows already exist (currently dead). Add:
  - `m-team-up`: `teamSelection = min(3, teamSelection+1)`; if `teamSelection>0 && playerCount < G.teamPlayerRequirement[teamSelection] { playerCount = G.teamPlayerRequirement[teamSelection] }` (`Menu_Lobby_Screen.java:786-792`).
  - `m-team-down`: `teamSelection = max(0, teamSelection-1)`.

### Stage 4 — Team assignment at match start (`MenuScene.startGame`, `:712-733`)
- If `teamSelection > 0`: set `opts.teamCount = G.teamCount[teamSelection]`; port `findLowestTeam()` over a `memberCount` array; walk players in slot order (host index 0 → team 0), each next player → `findLowestTeam()` (`Network.java:450-466,651,1112`). Set both `teamIndex` and `colorIndex = teamIndex`.
- **Interpolation (flag in comment):** defensively `opts.teamCount = min(opts.teamCount, players.count)` so no team is empty — the original prevents this via the lobby player-count bump; our local fallback path (`:728-730`) makes it robust. Not in source.

### Stage 5 — Turn cycling (`GameController.swift`)
- New match state: `activeTeam`, `lastTeamPlayer: [Int]`, `activeTeams: [Bool]`.
- `init` (~`:89`): if `teamGame`, seed `activeTeams` from each player's team, `activeTeam = currentPlayer.teamIndex`, `lastTeamPlayer[activeTeam] = currentPlayerIndex` (`Game_Loop.java:294-305`).
- `switchPlayers` (`:390-404`): add a `teamGame` branch transcribing `Game_Loop.java:392-415`, plus the entry guard `Game_Loop.java:388-390`. Add helper `countPlayersInTeam(_:)` (`Network.java:918-928`). Non-team branch unchanged.

### Stage 6 — Win condition (`GameController.checkWinCondition`, `:349-363`)
- Add `countTeamsWithActivePlayers()` (`Network.java:934-945`) and `findWinningTeam()` (`Network.java:869-884`).
- Team branch: `gameOver` when `< 2` teams have active members; success text `"\(G.teamNames[findWinningTeam()]) Team Wins!"` via existing `hud.showSuccessMessage` (`Packet_Manager.java:422`). Camera `setSuccessMode()` unchanged.

### Stage 7 — Turn banner (`GameController.beginTurn`, `:376-385`)
- If `teamGame`, after the existing banner, queue `hud.flashMessage("\(G.teamNames[activeTeam]) Team")` (`HUD.java:716/758`).

### Stage 8 — HUD target-plate flags (`HUDScene.swift`, `rebuildNameTags` ~`:500-528`)
- When `teamGame`, add a 24×24 sprite cropped from `flags.png` per team (subrects per `HUD.java:451-466`; mirror Y like existing `HUDArt.crop` callers — a coordinate-convention solve pinned against the already-correct `mapbits.png`/`controls.png` crops). Position left of the bar (`x ≈ -32`, `HUD.java:266`); hides with the holder (`updateNameTags`).
- Minimap X marks stay red for all (no team branch in the source minimap loop) — no change.

### Stage 9 — Bot targeting team-awareness (`BotAI.swift`)
- `pickTarget` (~`:48-65`): exclude teammates when `teamGame` (`Cannon.java:154-155,182-183`).
- `findDeathThreat` (~`:318-333`): in team mode skip impacts from teammate shooters (`Cannon.java:1435`).
- **Leave damage paths untouched** — `GameController.kill`/`checkForHitLine`/`updateShockwaves`/`Projectile` impact stay team-blind (`Weapon.java:1042,1102`). Add a comment there noting friendly fire is deliberate.

## Design decisions

- **Auto-balance teams, no team-picker UI.** Create-game in the original has no per-player team dropdown (the human could only swap his own team from the networked lobby list, which doesn't exist locally). Auto-balancing via `findLowestTeam` is the original's actual behavior and adds zero invented UI — the faithful minimal choice.
- **HUD flags + team win banner are in scope** (asset present, layout fully specified in `HUD.java`), not a follow-up.
- **Team chat (R key, `CHAT_TEAM`)** is intentionally out of scope: it only matters for networked multiplayer routing; in local hotseat all chat is already visible to the one human.

## Verification

1. `cd macos && swift build` — must compile clean.
2. `./.build/debug/Cannonballs --uitest` → `macos/snapshots/uitest-*.png`. Confirm the Team Play row shows `2`/`3`/`4` (not dimmed NA) after `m-team-up`, and player-count auto-raises to meet `teamPlayerRequirement`.
3. Start a 4-player / 2-team game and visually confirm: (a) cannons tinted by team (two blue, two purple); (b) turn banner shows "<Color> Team" after the turn message; (c) target plates carry the 24×24 team flag; (d) match ends "<Color> Team Wins!" when one team remains.
4. Behavioral: a human shot landing on a teammate **kills** it (fidelity to `Weapon.java:1042`); bots never *aim* at teammates over several bot turns.
5. Compare banner/plate frames against `format-research/originals/video_frames/` as a check, not a ruler. If no team-mode reference frame exists, note that flags/banner come from `HUD.java` layout constants (the source of truth).
6. After sign-off, repackage the release app per `macos/tools/package_app.sh` (the `swift build` debug binary is for verification; `/Applications` ships from the release build).

## Genuine interpolations (flagged in code comments)
- `teamCount = min(teamCount, players.count)` defensive clamp (Stage 4) — not in source.
- Single `colorIndex`/`teamIndex` field collapse (Stage 2) — equivalent to `Game_Loop.java:57-58`, our representation choice.
- Flag Y-mirror in the AppKit crop (Stage 8) — convention solve, pinned to existing correct crops.

## Critical files
- `macos/Sources/Cannonballs/GameController.swift` — turn cycling, win check, banner, init team state
- `macos/Sources/Cannonballs/MenuScene.swift` — team settings row + gating, assignment in `startGame`
- `macos/Sources/Cannonballs/Types.swift` — `PlayerConfig`/`GameOptions` team fields, `G` team constants
- `macos/Sources/Cannonballs/BotAI.swift` — team-aware targeting (friendly fire stays on)
- `macos/Sources/Cannonballs/HUDScene.swift` — target-plate team flags from `flags.png`
- `macos/Sources/Cannonballs/Cannon.swift` — `teamIndex` field + team tint

---

*This document is the archived planning copy for the Team Play feature, linked
from `ROADMAP.md`. Source of the plan: the read-only scoping pass over
`source/*.java` and `macos/Sources/Cannonballs/` (2026-07-11).*
