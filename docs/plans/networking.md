# Networking — scope & re-implementation research

> Research + options memo for reviving online play. **Not scheduled work** — this
> is the scoping study behind ROADMAP item "Networking". No code has changed.
> Every ported behavior cites `source/*.java` per the Fidelity Doctrine.

## Context

*Cannonballs!* (2002) shipped with full online play: a matchmaking lobby, hosted
internet games, in-game chat, accounts, and leaderboards. All of that is **gone** —
it ran on WildTangent's proprietary native runtime and servers
(`cannonballs.wildtangent.com`), both dead and unrecoverable. The current
resurrection (Swift/SceneKit on macOS, plus the parallel Godot Windows port) is
**single-player only**; the online menu buttons are deliberately dimmed dead
buttons ("The 2002 online services are gone — Single Player works!").

This document maps exactly what the original networking did (so a revival stays
faithful to observable behavior), assesses what the current code gives us to build
on, and lays out re-implementation approaches with a recommended path.

**Fidelity note (important):** The Fidelity Doctrine binds *original assets* and
*observable gameplay behavior*. The original **transport** (WildTangent DMMP +
DirectPlay + ASMX web services) is native code on lost servers — it cannot be
resurrected as-was. This is exactly the "native runtime is gone, pin the
convention by its correct visual/behavioral outcome" situation the Doctrine
anticipates. So the rule for netcode is: **faithfully reproduce the player-visible
multiplayer (host/join, lobby, turn model, chat, disconnect handling, player
caps, the 34-opcode event semantics), but the wire transport underneath is
necessarily new.** Cite `source/*.java` for every behavior we port.

---

## Part 1 — How the original worked (what we'd be reviving)

Three independent subsystems. Primary source files:
`source/Network.java` (the 1363-line orchestrator), `source/Packet_Manager.java`
(opcode router), `source/Packet.java` / `source/PacketSmall.java` (wire structs),
`source/Game_Loop.java` (sync phases), `source/Chat.java`,
`source/Menu_Lobby_Screen.java`, `source/Main.java` (config).

### A. Accounts + web services (dead ASMX servers)
Plain HTTP GETs via `WTFile`. `Network.java:134-135`:
- **UMS** (`.../UserManagement.asmx/`): `VerifyUser`, `VerifyGuest`, `NewUser`,
  `SendEmail` → returns `UserGUID` + `UserCredentials` used to auth into the lobby
  (`Network.java:398, 1092, 1349`). Guest names are backtick-prefixed.
- **Leaderboard** (`.../LeaderBoard.asmx/`): `Submit`, `RetrieveTable`,
  `RetrieveStats` — 6 tables (Most Kills, Water Baby, Most Deaths, Wildest Shot,
  Moneybags, Cheapskate), top-10 each (`Network.java:139-142, 160, 376, 742`).

### B. Lobby / matchmaking (WildTangent DMMP, client-server)
`com.wildtangent.dmmp.*` — a "distributed object" (DObject) community server.
- Connect: `createNetContext()` → `NativeSystemInitializer.createNetworkClient(host, port, user, creds)` to `cannonballs.wildtangent.com`, lobby port **4000** (`Main.java:50`; `Network.java:1336-1347`).
- **Rooms**: request listing (`requestRoomListing`, `Network.java:973`), auto-pick
  the fullest non-overflowing room, enter/leave, "room_full" → `tryNextRoom`
  (`Network.java:1000-1037`). Guests segregated to a guest room.
- **Matchmaking lobby**: each hosted game is advertised as a `DObject` entry —
  `GameCreationMessage.createGame` / `GameRemovalMessage.removeGame`
  (`Network.java:155, 371`); browse via `retrieveGameList` which parses a
  `IP|HostName`-plus-settings string (`Network.java:681-696`).
- **Lobby chat**: `ChatEvent.sendSpeakEvent` / receive in `handleEvent`
  (`Network.java:803-816, 1159`), with mute + guest-mute + `Text.doFilter`.
- **NAT**: `PlayerAttributeMessage.getPlayerAttribute("ip")` fetches the player's
  public IP; `isConnectionValid` compares private ranges to decide reachability
  (`Network.java:390, 593-609, 996`).

### C. In-game session (WildTangent WebDriver MP = DirectPlay)
`wildtangent.webdrivermp.*` (`WTMultiplayer`/`WTMPSession`) wraps Microsoft
DirectPlay — note the classic DirectPlay app GUID
`5BFDB060-06A4-11D0-9C4F-00A0C905425E` (`Network.java:629, 1129`).
- **Topology**: star. Host `hostSession("cannonballs", GUID, …, maxPlayers)`
  (`createNewMatch`, `Network.java:627`); each client
  `connectToSession(hostIP, GUID, …, 20000ms)` (`joinGame`, `Network.java:1126`),
  session port default **8000** (`Network.java:127`).
- **Authority**: **host-authoritative** with client-side prediction. Host owns
  slot assignment, team balancing, game-start, terrain-of-record, death
  resolution, and turn advancement. Clients autonomously send their own fire /
  aim / cash / chat and the host+peers replicate.
- **Delivery**: two lanes. Reliable `sendPacket` (guaranteed flag `1`) serializes
  a Java `Packet` object (`Network.java:1272`). Unreliable
  `sendPacketUnGuaranteed` (flag `0`) sends a tiny `PacketSmall` for aim/orient
  streaming, tagged with a leading int `1` so the receiver routes it separately
  (`Network.java:899, 770-794`). Both use Java `ObjectOutputStream`.
- **Timing**: aim/orientation streamed unreliably every ~200ms; keepalive ping
  (opcode 31) every ~1000ms; a peer silent >20s triggers "Connection Lost"
  (`Packet_Manager.java:547-560`), and >25s of packet-silence marks a cannon
  `Disconnected` and forfeits it (opcode 7, `Var1=-3`).
- **Players**: up to 32 slots (arrays sized 32 throughout `Network.java`), with
  free-for-all and team modes (`PlayerTeam`, team balancing, `findWinningTeam`).
- **Bots in MP**: a bot has a `BotOwner`; the owning human simulates it and
  broadcasts its actions (`Network.java:617, 345`).

### The in-game protocol (ground truth: `Packet_Manager.java:17-561`)
`Packet` fields: `Code, Id, type, Name, X1..Z2, Var1..Var3, conditional`.
`PacketSmall` fields: `Id, Var1(=spin·100), Var2(=tilt·100)`.

| Code | Meaning | Direction |
|---|---|---|
| 0 | begin game (map, player/chest counts, all rules) | host→all |
| 1 | begin location: a cannon's start pos, name, wind | host→client |
| 3 | fire (spin, tilt, power, weapon) → `remoteClientFire` | player→all |
| 4 | switch turn / end turn | →all |
| 5 | crater (terrain destroy) | →all |
| 6 | molehill (terrain raise) | →all |
| 7 | death (drown/detonate/kill/forfeit/disconnect via `Var1`) | →all |
| 10 | setup complete / sync ack | client→host |
| 11 | assign player id (+team) to joiner | host→joiner |
| 12 | confirm id / check-in (team) | joiner→host |
| 13 | re-request begin location | client→host |
| 14 | reject all clients / game cancelled | host→all |
| 15 | reject this client (by name) | host→joiner |
| 16 | remove-from-join-list (pre-game leave, slot compaction) | →host |
| 17 | receive player-list line | host→client |
| 18 | chest location + treasure type | host→all |
| 19 | chest destroy / respawn | →all |
| 20 | x-shot multi-crater | →all |
| 21 | TNT / prop drop | →all |
| 22 | in-game chat (`Var1=-1` all / team id) | any→all |
| 23 | respawn | →all |
| 24 | cash update | →all |
| 25 | teleport | →all |
| 26 | destroy prop | →all |
| 27 | confirm game + chosen color | joiner→host |
| 28 | swap team | →all |
| 29 | notify join | joiner→host |
| 31 | keepalive ping (+connection state in `type`) | bidir |
| 32 | swap color | →all |
| 33 | hide weapon (post-fire) | →all |
| 34 | auto-timeout / leaving | →all |
| (small) | orientation stream → `remoteClientOrient` | player→all |

---

## Part 2 — What the current code gives us

From `macos/Sources/Cannonballs/` (Swift/SceneKit; Godot Windows port mirrors it):
- **Deterministic turn machine** in `GameController.swift`: `players: [Cannon]`,
  `projectiles`, `chests`, `currentPlayerIndex`, timers, `gameOver`,
  per-match `windDirection/windVelocity` rolled once. Given seed + ordered
  inputs, it is reproducible — the key enabler.
- **State model** in `Types.swift`: `GameOptions` (map, roster, cash, respawns,
  hotseat, treasure-respawn) and `PlayerConfig { name, colorIndex, botType }`
  where `botType==0` already means "human". `Cannon.swift` holds position,
  economy, aim (`spinAngle/tiltAngle/activeTilt`), fire state, `bot: BotState`.
- **The exact replication seams already exist as method names** the source used:
  Swift `Cannon`/`Projectile` mirror `remoteClientFire`, `remoteClientOrient`,
  `clientRespawn`, `clientTeleport`, `Island.crater/molehill` — i.e. the
  receive-side handlers are essentially already the local-apply code paths.
- **Menu hooks**: `MenuScene.swift` has the (dimmed) online buttons and a lobby
  slot roster to repurpose. Local lobby already assembles a `PlayerConfig` list.
- **Gaps**: no serialization (no `Codable` packet structs), no transport, no
  host/join flow, no authority gating, RNG is local (wind/chests/bots/craters).
- **Test harness**: headless `--uitest` / `--snapshot` / `--gameovertest` modes
  write PNG snapshots (`macos/snapshots/`) — extensible to a `--nettest` loopback.

---

## Part 3 — Re-implementation options (the brainstorm)

Four independent axes. Recommendation in **bold** at each.

### Axis 1 — Netcode model
- **A. Event replication (port the original protocol).** Turn the 34 opcodes into
  a Swift enum, host-authoritative, replicate events at the same seams the source
  does. **Recommended.** It is the *faithful* option, the protocol is already
  fully reverse-engineered, and turn-based play means latency is a non-issue.
  Downside — it trusts clients (cheatable); irrelevant for friends.
- B. Deterministic lockstep (exchange only inputs). Tiny bandwidth, auto-sync,
  but demands bit-identical float physics across peers — fragile, and a
  cross-play killer between SceneKit and Godot. Rejected.
- C. Full host-authoritative state streaming. Heaviest; overkill for a turn game.
  Rejected.

### Axis 2 — Transport
- Direct IP / LAN: simplest, zero infra, but port-forwarding pain over the net.
- **Small relay server (WebSocket).** Host and clients both dial *out* to one
  cheap service that relays frames — sidesteps NAT entirely and can double as the
  lobby. **Recommended** as the internet path; keep a direct-IP/loopback path for
  LAN and tests.
- Platform SDK (Steam/GameKit): NAT+lobby for free but heavy lock-in and doesn't
  serve the Godot side. Not now.

### Axis 3 — Lobby / matchmaking scope
- **Minimal first: "Host → get a room code → friends join by code."** No accounts.
  **Recommended for Phase 1.**
- Full: browsable room list + game listings + lobby chat + leaderboards (the DMMP
  feature set). Phase it in later; the relay server is the natural home.

### Axis 4 — Cross-play with the Godot Windows port
Because both ports share game constants and logic, event replication over a
**versioned, platform-neutral wire format (JSON or compact binary — NOT Java
serialization)** makes Swift↔Godot cross-play very achievable. Even if Windows
lands later, design the protocol platform-neutral from day one.

---

## Part 4 — Recommended phased approach

**Phase 0 — Protocol + transport abstraction (no gameplay change).**
- New `NetProtocol.swift`: a `NetPacket` enum/struct mirroring `Packet`/
  `PacketSmall`, `Codable`, versioned, one opcode per source opcode. Document each
  case against its `Packet_Manager.java` line.
- New `NetTransport` protocol with two impls: `LoopbackTransport` (in-process, for
  tests) and `SocketTransport` (TCP/WebSocket). Reliable + unreliable lanes.

**Phase 1 — LAN / loopback 2-player, event replication.**
- New `NetSession.swift`: mirrors `Network.java` — host `hostSession`, client
  `joinGame`, the check-in handshake (opcodes 29→11→12→27→0), send/receive loop,
  keepalive + timeout, disconnect handling.
- `GameController.swift`: add `isHost`, `localPlayerIndex`, `myTurn` gating; emit
  net events at the existing fire/turn-switch/death/crater/respawn points and
  consume them through the already-present `remoteClient*`/`client*` handlers.
- `Cannon.swift`: wire the aim stream (unreliable, ~200ms) and reliable fire.
- **Seed/RNG sync** — the crux: wind, chest positions, prop drops, crater
  randomness, bot decisions must agree. The original mostly **broadcasts results**
  (host sends wind in opcode 1, chest positions in opcode 18) rather than sharing
  a seed. Port that faithfully: host is the source of truth, replicate outcomes.
- `MenuScene.swift`: repurpose the dimmed online buttons into a Host / Join-by-IP
  path; extend the lobby roster so a slot can be a **remote human** (alongside
  human/bot in `PlayerConfig`).
- Verify with a new `--nettest`: two in-process sessions play a scripted match
  over `LoopbackTransport`; snapshot both and diff.

**Phase 2 — Internet via relay + minimal lobby.**
- Stand up the small WebSocket relay (host/clients dial out; NAT-free). Room-code
  matchmaking. `SocketTransport` points at it. Optional lobby chat (opcode 22
  already exists) and a room list (DMMP `retrieveGameList` shape).

**Phase 3 (optional) — accounts + leaderboards.**
- Re-home the 6 leaderboard tables and stat submission on the relay server. Only
  if there's appetite; purely additive.

**Phase 4 (optional) — cross-play** with the Godot port by implementing the same
`NetProtocol` there. Feasible precisely because Phase 0 kept the format neutral.

Representative files: **new** `NetProtocol.swift`, `NetTransport.swift`,
`NetSession.swift`, `NetLobby.swift`; **changed** `GameController.swift`,
`Cannon.swift`, `MenuScene.swift`, `Types.swift`, `main.swift` (`--nettest`).

---

## Part 5 — Key risks & decisions to flag

1. **RNG/seed sync** — replicate *results* (faithful to source: host broadcasts
   wind + chest + prop outcomes) vs. share a seed and re-derive. Recommend
   results-replication; it matches the opcodes we already have.
2. **Trust/cheating** — original trusts clients. Fine for friends; note it.
3. **Bot ownership** — port the `BotOwner` model (owning human drives the bot and
   broadcasts its shots) rather than host-simulating all bots.
4. **Reconnect** — original does not reconnect; it marks a cannon `Disconnected`
   and continues (opcode 7 `Var1=-3`, opcode 34). Port that; true reconnect is a
   later nicety.
5. **Player cap** — original supports up to 32. Recommend targeting the same 2–8
   range the local lobby exposes first, keep arrays at 32 for fidelity.

## Open decisions (the forks)
- **Fidelity of netcode:** port the original event/opcode protocol nearly verbatim
  (recommended), or modernize the model while keeping gameplay identical?
- **Ambition of matchmaking:** room-code direct-join first (recommended), or go
  straight for a hosted lobby with a browsable game list + chat?
- **Cross-play:** design the wire format platform-neutral for eventual Swift↔Godot
  cross-play from day one (recommended, low cost), or macOS-only for now?
- **Accounts/leaderboards:** in scope at all, or gameplay-only?

---

## Verification (when built)
- `--nettest` loopback harness: two in-process `NetSession`s run a scripted
  2-player match; assert identical end-state and snapshot both HUDs.
- Two app instances over `127.0.0.1` for a real socket smoke test.
- Compare a replicated match's HUD/turn-order against a local single-player run of
  the same seeded inputs to confirm the replication seams are faithful.
- Regression: existing `--uitest` PNG suite must still pass (single-player
  untouched).
