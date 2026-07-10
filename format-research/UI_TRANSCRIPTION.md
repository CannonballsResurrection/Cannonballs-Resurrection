# Widget-complete UI transcription checklist

Per PROCESS_RETRO.md rule 3: every widget construction in each decompiled
screen accounted for against the clone. Evidence: (S) = source line, (V) =
original video frame (originals/video_frames), (P) = original screenshot
(cannonballs.jpg).

## Menu_Main_Screen — ✅ complete
| Widget | Source | Clone | Evidence |
|---|---|---|---|
| Guest Login (400,380), Single Player (400,420), View Leaderboards (400,460), Register Now! (400,520) | show():34-37 (single-player branch) | ✅ same coords; dead services dimmed | S |
| Quit/Options/Help 3DMenus (10/70/165,14) | show():48-53 | ✅ top menu bar | S |
| Version "Cannonballs! v1.869" (400,14) style 1 f=0.75 | show():55 | ✅ (was v1.87, fixed) | S,V frame_001 |
| FULLSCREEN control (767,1) | show():45 | n/a (macOS window manages fullscreen) | S |
| Login/password sub-popups | 217-350 | n/a (dead online services) | S |

## PopUp ("Your Name") — ✅ complete
Title (154,188) f=1.0, Name field textbar, Cancel (273,412), Enter (527,412).
Popup body = POPUP halves at (400,300). Message text at (+10,+16) f=1.0.

## New Game Settings — ✅ complete (ref frame_001)
Six rows: Maximum # of Players / Starting # of Lives / Starting Gold /
HotSeat Mode / Treasures Respawn / Team Play, each ◄ value ►; map thumbnail
left with side arrows; CANCEL (273,412) / CREATE (527,412). Defaults: cash
index 4 (=1000), lives 2, hotseat NA. Clone matches all.

## Menu_Lobby_Screen (Joining Players) — ✅ complete (ref frame_002)
| Widget | Source | Clone |
|---|---|---|
| Title "Joining Players" (14,82) | showJoinMenu | ✅ |
| Rows y=112+i*32; names x=40 (backticked blue for bots) | showMasterJoinMembers | ✅ |
| IconCheck 24px @ x=330 for checked-in rows | :1685 | ✅ decoded MENUS/ICONS check |
| Kick ("LINE") button @ x=355, only rows != 0 | :1688 | ✅ gated on AI assigned; textbar stand-in for the Controls crop (~ logged) |
| Empty slots: "Add AI Player" dropdown @ x=200, ~176px, contiguous stack | :1729 + frame_002 | ✅ |
| Team flags (10,100+i*32) | :1703-1717 | n/a (team games) |
| Cannon preview: camera-space over lobby ART, barrel+stand, tint, yaw -135, pitch -20, 50 deg/s | :400-416 | ✅ dedicated scene w/ JOIN art background |
| Color dropdown (610,320) w/ swatch | :427 | ✅ |
| Abandon (610,70), Begin (610,350) | :423, :1745 | ✅ |
| ChatBar scrollbar (765,427), CHATUP/CHATDOWN | :418-421 | n/a (multiplayer lobby chat) |

## Menu_Results_Screen — ✅ complete (rewritten this pass)
Title "Game Stats For '`name`'" (400,200); THIS GAME (400,250) / TOTAL
(600,250); blue stat names x=200 y=280..360 step 20 (Kills/Misses/Deaths/
Drownings/Gold Spent); blue "+N" this-game col at 400; white totals at 600
(career totals persisted); Done (400,575). Over the RESULTS backdrop.

## Menu_Stats_Screen — n/a (online leaderboards; services dead)

## Tips — n/a (fetched http://cannonballs.wildtangent.com/tips.txt; dead feed)

## HUD — ✅ (verified against P + V frames 007/020/023)
Topbar, weapon dropdown (white/blue/gray sheets), gold/lives, minimap +
rotating arrow, powerbar, name tags, player queue (current white 1.0, rest
blue 0.75), wind mph, Defense Mode, chat popup + two-tone bot lines.

## Chat — ✅ two-tone backtick rendering (V frames 020/023)
