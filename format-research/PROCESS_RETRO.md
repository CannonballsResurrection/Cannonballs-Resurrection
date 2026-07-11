# Process retrospective — why a playtest found 14 bugs in "all-green" work

Written 2026-07-09 after Alex's playtest against an original screenshot
(cannonballs.jpg) surfaced 14 fidelity bugs, and the follow-up audit found 8
more corrupted models. Every one of those items sat behind a ✅ in the matrix.

## Root causes, honestly

1. **✅ meant "wired", not "matches".** Rows were marked done when the decoded
   asset flowed into the clone, not when the rendered result was compared to
   the original. Until the screenshot arrived there was NO original reference
   in hand, so "checked against source" quietly degraded to "reads the right
   file". The matrix's own process rule required checking "against
   source/video" — with no video, that clause was unenforceable and ignored.

2. **Superseded-pipeline artifacts were never re-audited.** The pre-solved-era
   OBJ exporter used *documented heuristics* (auto-detected UV/normal offsets,
   guessed material routing). When the format got SOLVED, only new work used
   the solved parser; the ~40 already-exported models kept their heuristic
   output and their ✅. One family bug (UV flips / wrong material) shipped
   across PALM, ARROW, CANNON(base+stone), BRUSH2, FERNTREE, TIKKI1-3, BRIDGE,
   MOUND, TORCHBEARER, BOUNCEBALL, LIGHTHOUSE.

3. **UI screens were skimmed for constants, not transcribed widget-by-widget.**
   Menu_Lobby_Screen.java is ~2000 decompiled lines; layout numbers were pulled
   out, but mid-function behaviors were paraphrased: the IconCheck, the
   kick-only-on-checked-in-rows gating, the 256x32 natural dropdown size, the
   names-only row content, the barrel's IMAGES/CANNON player-tinted shader.

4. **Gaps were filled with inventions, silently.** Lobby color swatches, gold/
   orange text tints, the x7 chest scale — each violated the matrix's "any
   stand-in must be logged as ~" rule without being logged.

5. **A latent helper bug styled everything.** `tinted()` flattened glyphs with
   a sourceAtop fill, destroying the font's baked black outline — every tinted
   (and even .white-tinted) string rendered as a bold outline-less blob. Font
   *metrics* were validated; font *rendering* never was.

## Process now in force

1. **Visual-reference rule.** A row is ✅ only with a side-by-side artifact in
   `format-research/refchecks/` (clone render vs original screenshot/asset/
   texture). Where no original reference exists, the row is marked "✅ (source-
   derived, no visual reference)" — an explicit weaker claim.
2. **Superseding a pipeline triggers a full re-audit.** Re-export EVERY
   artifact through the new pipeline and diff renders (done: model_audit_grid
   + audit_before_after in refchecks/; 8 props fixed).
3. **Widget-complete UI transcription.** Per screen, enumerate every
   Button_*/Message_3D/Button_Static construction in the source with its args
   and gating condition; check each off in the clone. Lobby done; Main,
   Settings, Results screens queued for the same pass.
4. **No silent invention.** Anything not derivable from source or assets gets
   a ~ matrix row BEFORE the code is written.
5. **Hunt references.** One original screenshot yielded 14 bugs. Collect more
   originals (archive.org captures, review-site screenshots, gameplay videos)
   into `format-research/originals/`.
