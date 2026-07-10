#!/bin/bash
# Package the Windows build: headless export + the shared asset tree copied
# beside the exe (the runtime looks for Resources/maplist.dat there —
# src/assets.gd). Run from anywhere; writes windows/dist/.
set -euo pipefail

WINDOWS_DIR="$(cd "$(dirname "$0")/.." && pwd)"
REPO_DIR="$(dirname "$WINDOWS_DIR")"
DIST="$WINDOWS_DIR/dist/Cannonballs"

GODOT="${GODOT:-godot}"
command -v "$GODOT" >/dev/null || { echo "godot not on PATH (brew install --cask godot)"; exit 1; }

rm -rf "$DIST"
mkdir -p "$DIST"

echo "== exporting Windows release =="
"$GODOT" --headless --path "$WINDOWS_DIR" --export-release "Windows Desktop" \
    "$DIST/Cannonballs.exe"

echo "== copying shared/Resources (real copy — dist must be self-contained) =="
rsync -a --exclude '.gdignore' --exclude '*.import' \
    "$REPO_DIR/shared/Resources/" "$DIST/Resources/"

echo "== done =="
du -sh "$DIST"
echo "packaged $DIST  (zip it or copy the folder to a Windows machine)"
