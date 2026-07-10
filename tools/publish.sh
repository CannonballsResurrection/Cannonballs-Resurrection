#!/bin/bash
# Ship the installers: (re)build both platforms and publish them to the GitHub
# Release the README download links point at, so those links always serve the
# newest build.
#
# The README links are stable: releases/latest/download/Cannonballs-macOS.dmg
# and .../Cannonballs-Windows.zip. This script keeps them in sync by clobbering
# the same-named assets on the `latest` release with freshly built ones — the
# filenames never change, so the links never rot.
#
# Run this whenever you want the download links to reflect current code.
#   ./tools/publish.sh            # build both, upload to the existing latest release
#   TAG=v1.1 ./tools/publish.sh   # cut a new release tag instead (becomes latest)
set -euo pipefail

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DIST="$REPO_DIR/dist"
DMG="$DIST/Cannonballs-macOS.dmg"
ZIP="$DIST/Cannonballs-Windows.zip"

command -v gh >/dev/null || { echo "gh CLI not found" >&2; exit 1; }

echo "== [1/3] building macOS .dmg (also syncs the installed local copies) =="
"$REPO_DIR/macos/tools/make_dmg.sh"

echo "== [2/3] building Windows .zip =="
"$REPO_DIR/windows/tools/package_win.sh"
rm -f "$ZIP"
( cd "$REPO_DIR/windows/dist" && zip -rqX "$ZIP" Cannonballs )

echo "== [3/3] publishing to GitHub Release =="
# Default: refresh the current latest release. Fall back to the pinned v1.0 if
# no release exists yet. Override with TAG=... to cut a new one.
if [ -n "${TAG:-}" ]; then
    REL="$TAG"
elif REL="$(gh release view --json tagName --jq .tagName 2>/dev/null)"; then
    :
else
    REL="v1.0"
fi

if gh release view "$REL" >/dev/null 2>&1; then
    echo "-- clobbering assets on existing release $REL"
    gh release upload "$REL" "$DMG" "$ZIP" --clobber
else
    echo "-- creating release $REL (marked latest)"
    gh release create "$REL" "$DMG" "$ZIP" \
        --title "Cannonballs! Resurrection $REL" --latest \
        --notes "Self-contained macOS (.dmg) and Windows (.zip) builds of the pixel-faithful Cannonballs! resurrection."
fi

echo "== done — README download links now serve $REL =="
gh release view "$REL" --json tagName,assets \
    --jq '"\(.tagName): " + ([.assets[] | "\(.name) (\(.size) bytes)"] | join(", "))'
