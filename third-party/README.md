# third-party

External code this project builds on, kept as a pinned submodule of **our own fork**
so it survives even if the original upstream repo disappears.

## WTExtractor

The community toolchain for WildTangent WLD3 / WebDriver files — the WLD3 container
parser (`libwld3/`, `pywttools/`), the WebDriver Java API vendored into
[`../source/engine/`](../source/engine/), and viewers. Our own decoders in
[`../tools/`](../tools/) build on it.

- **Submodule:** `third-party/WTExtractor` → **our fork** `diamondman/WTExtractor`
  (forked from `diamondman/WTExtractor`), pinned at commit `a1812e7`.
- **Why a fork:** an independent copy, so this material is preserved even if the
  original upstream is deleted. The submodule pins an exact commit for reproducibility.
- **License:** upstream ships no license file, so rights are reserved by its authors.
  Included for private, non-commercial preservation. We contributed the v200 container
  patch upstream (kept locally at [`../tools/wtextractor-v200-patch`](../tools/wtextractor-v200-patch)).

### Getting the contents

A plain `git clone` leaves the submodule empty. Populate it with:

```sh
git clone --recurse-submodules <this repo>
# or, in an existing clone:
git submodule update --init --recursive
```
