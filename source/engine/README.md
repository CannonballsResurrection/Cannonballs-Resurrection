# source/engine — WildTangent WebDriver Java API

These are the `wildtangent.webdriver.*` engine classes the game code in
[`../`](../) imports. Adding them here makes `source/` a self-contained,
resolvable codebase to read — 56 of the 84 game classes reference these types.

## What's here

- `wildtangent/webdriver/*.java` — the public engine API (`WT`, `WTModel`,
  `WTActor`, `WTCamera`, `WTSurfaceShader`, `WTAudioClip`, `WTEvent`, …).
- `wildtangent/webdriver/impl/*.java` — the internal implementation layer.
- `com/ms/**`, `netscape/javascript/**`, `java/applet/Applet.java` — Microsoft J++
  / Netscape / applet stubs the API compiles against (WebDriver ran as a signed
  applet under the Microsoft JVM).

## Important caveats

- **Reference only — the Java API won't run on its own.** It's a thin shim; the
  real engine is the native runtime in [`native/`](native/) — the DLLs
  (`WDENGINE.dll`, `webdriver.dll`, …), the WebDriver installer, the Microsoft JVM
  it needed, and the [Genesis3D](native/genesis3d/) engine source it derives from.
  The `.java` here lets you *read* what each call does; `native/` is what executes.
- **`wildtangent.webdrivermp.*` (multiplayer) is not included** and isn't in this
  API drop — that stack talked to WildTangent's now-dead lobby servers.

## Provenance

Obtained from the **WTExtractor** project (`wtplayer/javasrc/`), vendored in this
repo at [`../../third-party/WTExtractor/`](../../third-party/WTExtractor/) — see its
`ATTRIBUTION.md`. The API classes are © WildTangent / Gamigo; see
[`../../LEGAL.md`](../../LEGAL.md).
