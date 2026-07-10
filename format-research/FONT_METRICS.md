# Trebuchet bitmap font — exact metrics (from source)

Transcribed from `source/Text.java` + `source/Message_3D.java`, not eyeballed.

- **Sheets:** `IMAGES/FONT/TREBUCHET/{font,bluefont,grayfont}.png` (RGB) composited
  with a shared `alpha.png`. Three colors only: white, blue, gray.
- **Grid:** 256x256, 10x10 cells of **24px** (`Message_3D.FontSize = 24`). Glyph
  index = `char - 32`, clamped 0..95 (ASCII 32..127), left/top-aligned in its cell.
- **Per-glyph width:** `Text.CharacterWidthTrebuchet[96]` (verbatim in `HUDArt.charWidths`).
  Each glyph quad is drawn `width x 24`; the texture samples `width` px from the cell.
- **Advance / kerning:** pen advances `width * 0.75` per glyph — consecutive glyphs
  **overlap 25%** (the original's tight look). String width = `sum(width*0.75)`.
- **Sizes:** the engine uses only `f = 1.0` (24px, ~95 call sites: titles, buttons,
  cash, current turn name, labels) and `f = 0.75` (18px, ~66 sites: waiting turn
  names, table/secondary text, chat, hints).
- **Turn list color:** current player = white `f=1.0`; waiting players = blue
  `f=0.75` (the backtick toggle in `updateNextUp`). Not player-colored.

The clone's `HUDArt.text()` now reproduces this exactly instead of the old
heuristic glyph-cropper, which is why menu/HUD text finally matches the original's
width, spacing, and weight.
