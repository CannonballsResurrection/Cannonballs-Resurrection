#!/bin/bash
# Build the self-contained macOS installer.
#
# Produces dist/Cannonballs-macOS.dmg: a drag-to-install disk image holding
# Cannonballs!.app (release binary + the full shared/Resources asset tree,
# ad-hoc codesigned) beside a /Applications symlink. The app is standalone —
# no separate asset download, no runtime dependency on this repo.
#
# Also refreshes the two installed copies (~/Documents/Cannonballs and
# /Applications) from the SAME staged bundle, so the playable local copy is
# always in sync with what the .dmg ships. Run after any code/asset change.
set -euo pipefail

MACOS_DIR="$(cd "$(dirname "$0")/.." && pwd)"
REPO_DIR="$(dirname "$MACOS_DIR")"
DIST="$REPO_DIR/dist"
STAGE="$DIST/dmg-stage"
APPNAME="Cannonballs!.app"
DMG="$DIST/Cannonballs-macOS.dmg"

echo "== building release binary =="
( cd "$MACOS_DIR" && swift build -c release )

echo "== staging $APPNAME =="
rm -rf "$STAGE"
mkdir -p "$STAGE"
# package_app.sh assembles a fully self-contained bundle (binary + rsync of
# shared/Resources + icns + ad-hoc codesign). Stage one, then also refresh the
# two installed copies from the exact same recipe so local == shipped.
"$MACOS_DIR/tools/package_app.sh" \
    "$STAGE/$APPNAME" \
    "$HOME/Documents/Cannonballs/$APPNAME" \
    "/Applications/$APPNAME"

echo "== building $DMG =="
ln -sf /Applications "$STAGE/Applications"
rm -f "$DMG"
hdiutil create -volname "Cannonballs!" -srcfolder "$STAGE" \
    -fs HFS+ -format UDZO -ov "$DMG" >/dev/null

rm -rf "$STAGE"
echo "== done =="
du -sh "$DMG"
echo "packaged $DMG  (also refreshed ~/Documents/Cannonballs and /Applications)"
