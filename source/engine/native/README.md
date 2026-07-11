# source/engine/native — the actual engine runtime

The native side of WildTangent WebDriver: the compiled engine the game really ran
on. The Java in [`../`](../) is a thin shim over this. Windows-only; this is what
`macos/` replaces with SceneKit.

| Folder | What it is |
|---|---|
| `dlls/` | The recovered engine DLLs — `WDENGINE.dll`, `webdriver.dll`, `actorobject.dll`, `wt3d.dll`, `dx5drv.dll`, `dx7drv.dll`. The compiled C++ every `WT*` Java call bottoms out in; also what was disassembled to crack the asset formats. See `dlls/PROVENANCE.md`. |
| `installer/` | The official **WebDriver 4.1** runtime installers (`Install_Webdriver.exe`, `WebDriverSilentInstall.exe`) — how the engine was deployed. |
| `msjvm/` | The **Microsoft JVM** (`msjavx86.zip`). WebDriver ran as a signed applet under the MS JVM, so it's a hard runtime dependency (long discontinued). |
| `genesis3d/` | Source of the **Genesis3D** engine — the open-source 3D engine WebDriver's native layer derives from (the `ge*` symbols in `WDENGINE.dll`). Read this to understand the DLLs at the source level. |

## How it fits together

```
game (source/*.java)
   → wildtangent.webdriver.* Java API  (source/engine/*.java)
      → native engine  (dlls/*.dll)  ← built on Genesis3D (genesis3d/)
         → DirectX  (dx5drv/dx7drv.dll)
   all hosted by the Microsoft JVM  (msjvm/)  as a WebDriver applet
```

## Provenance & licensing

- The **DLLs** and **installers** are © WildTangent / Gamigo (see `dlls/PROVENANCE.md`
  and [`../../../LEGAL.md`](../../../LEGAL.md)).
- The **Microsoft JVM** is © Microsoft (discontinued product; preservation copy).
- **Genesis3D** is © its authors under the **Genesis3D Public License** (source-available,
  *not* public domain) — its `g3dlicense.txt` is kept in `genesis3d/`. Upstream:
  `RealityFactory/Genesis3D` @ `f85b288`.

Everything here is included for preservation/study in a **private** repo. It is not
redistributable — see `../../../LEGAL.md`.
