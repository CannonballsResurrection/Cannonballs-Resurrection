# Legal / licensing / provenance

This is a **non-commercial preservation and interoperability project** for
*Cannonballs!*, a game WildTangent shipped in 2002 and later discontinued. The
intellectual property is currently held by **Gamigo**. This project has no
affiliation with, and is not endorsed by, WildTangent or Gamigo.

## Licensing — read the split carefully

The repository contains two very different kinds of material, under two very
different terms.

### Our own work — GPLv3

Everything **we** wrote is licensed under the **GNU General Public License v3.0**
(see [`LICENSE`](LICENSE)):

- the native rebuilds in **`macos/`** (Swift/SceneKit) and **`windows/`** (Godot),
- the decode/extraction scripts in **`tools/`**,
- the reverse-engineering write-ups and specs in **`format-research/`**,
- the repo's own docs.

Copyright © the *Cannonballs! Resurrection* preservation project, licensed GPLv3.

### The original game — NOT ours, NOT GPL

The following are **original WildTangent / Gamigo material**, included only for
study, preservation, and interoperability. They are **© the rights holder** and
are **not** covered by the GPL or any grant we could make:

- **`source/`** — the game's decompiled Java.
- **`source/engine/native/dlls/`** — original WildTangent engine DLLs
  (see `source/engine/native/dlls/PROVENANCE.md`).
- **`decoded-assets/`**, **`shared/Resources/`**, and the game textures, audio,
  meshes, maps, and menu art — decoded original game assets.
- **`raw/`** — the original installer and encoded media.

Mechanical format conversions of the originals (e.g. decoding a proprietary
container to a standard PNG/WAV) do not create a new copyright; the underlying
work remains the rights holder's.

## Good-faith stance

The original material is documented and decoded to the extent needed to
understand the WildTangent formats and keep a discontinued game playable and
studyable. "Abandonware" is not a legal defense; this is a good-faith fan
preservation effort for a product that is no longer sold, distributed, or
supported. If you want to *play* the game, you should own an original copy — the
tools here can regenerate the assets from your own installer.

## Takedown

If you are the rights holder and want any copyrighted material here removed, open
an issue or contact the repo owner and it will be taken down promptly.
