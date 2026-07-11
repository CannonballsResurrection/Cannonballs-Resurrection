#!/bin/bash
# Package the Windows build. Two modes (reversible via one flag):
#
#   CB_SINGLE_FILE=1  (default)  -> a single self-contained Cannonballs.exe:
#     shared/Resources is packed RAW into assets.pck (tools/pack_assets.gd, no
#     import re-encode) and embedded in the exe (a .pck is a non-importable file,
#     so the exporter ships it verbatim). assets.gd mounts it at boot. No folder.
#
#   CB_SINGLE_FILE=0             -> the known-good FALLBACK: exe + a Resources/
#     folder copied beside it. Use this if the single-file build ever misbehaves.
#
# Run from anywhere; writes windows/dist/.
set -euo pipefail

WINDOWS_DIR="$(cd "$(dirname "$0")/.." && pwd)"
REPO_DIR="$(dirname "$WINDOWS_DIR")"
DIST="$WINDOWS_DIR/dist/Cannonballs"
PCK="$WINDOWS_DIR/assets.pck"          # transient: staged under res:// for the embed
SINGLE_FILE="${CB_SINGLE_FILE:-1}"

GODOT="${GODOT:-godot}"
command -v "$GODOT" >/dev/null || { echo "godot not on PATH (brew install --cask godot)"; exit 1; }

cleanup() { rm -f "$PCK"; }
trap cleanup EXIT

rm -rf "$DIST"; mkdir -p "$DIST"

if [ "$SINGLE_FILE" = 1 ]; then
    echo "== building raw asset pack (assets.pck) =="
    rm -f "$PCK"
    CB_SRC="$REPO_DIR/shared/Resources" CB_OUT="$PCK" \
        "$GODOT" --headless --path "$WINDOWS_DIR" --script "$WINDOWS_DIR/tools/pack_assets.gd" 2>&1 \
        | grep -E "pack_assets:" || { echo "asset pack build failed" >&2; exit 1; }

    echo "== exporting single-file release (assets.pck embedded in the exe) =="
    "$GODOT" --headless --path "$WINDOWS_DIR" --export-release "Windows Desktop" "$DIST/Cannonballs.exe"
    rm -f "$PCK"
    echo "== done =="
    du -sh "$DIST/Cannonballs.exe"
    echo "packaged $DIST/Cannonballs.exe  (single self-contained file — no Resources folder)"
else
    echo "== exporting release (code only) =="
    "$GODOT" --headless --path "$WINDOWS_DIR" --export-release "Windows Desktop" "$DIST/Cannonballs.exe"
    echo "== copying shared/Resources beside the exe (fallback folder build) =="
    rsync -a --exclude '.gdignore' --exclude '*.import' "$REPO_DIR/shared/Resources/" "$DIST/Resources/"
    echo "== done =="
    du -sh "$DIST"
    echo "packaged $DIST  (exe + Resources folder — keep them together)"
fi
