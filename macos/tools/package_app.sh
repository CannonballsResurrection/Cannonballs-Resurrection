#!/bin/bash
# Package the macOS build: release binary + shared/Resources -> Cannonballs.app.
# Usage: package_app.sh [dest.app ...]   (defaults to the two installed copies)
set -euo pipefail

MACOS_DIR="$(cd "$(dirname "$0")/.." && pwd)"
REPO_DIR="$(dirname "$MACOS_DIR")"
BIN="$MACOS_DIR/.build/release/Cannonballs"
RES="$REPO_DIR/shared/Resources"

[ -f "$BIN" ] || { echo "release binary missing — run: swift build -c release" >&2; exit 1; }
[ -d "$RES" ] || { echo "shared/Resources missing" >&2; exit 1; }

DESTS=("$@")
[ ${#DESTS[@]} -gt 0 ] || DESTS=("$HOME/Documents/Cannonballs/Cannonballs!.app" "/Applications/Cannonballs!.app")

for APP in "${DESTS[@]}"; do
    mkdir -p "$APP/Contents/MacOS" "$APP/Contents/Resources"
    # the game's official title is "Cannonballs!" (executable name stays plain)
    cat > "$APP/Contents/Info.plist" <<'PLIST'
<?xml version="1.0"?><plist version="1.0"><dict><key>CFBundleName</key><string>Cannonballs!</string><key>CFBundleDisplayName</key><string>Cannonballs!</string><key>CFBundleIdentifier</key><string>com.exobrain.cannonballs</string><key>CFBundleVersion</key><string>1.0</string><key>CFBundleShortVersionString</key><string>1.0</string><key>CFBundlePackageType</key><string>APPL</string><key>CFBundleExecutable</key><string>Cannonballs</string><key>CFBundleIconFile</key><string>AppIcon</string><key>LSMinimumSystemVersion</key><string>13.0</string><key>NSHighResolutionCapable</key><true/><key>NSPrincipalClass</key><string>NSApplication</string></dict></plist>
PLIST
    cp "$BIN" "$APP/Contents/MacOS/Cannonballs"
    # legacy SPM resource bundle superseded by the shared/Resources tree
    rm -rf "$APP/Contents/Resources/CannonballsMac_Cannonballs.bundle"
    rsync -a --delete "$RES/" "$APP/Contents/Resources/Resources/"
    if [ ! -f "$APP/Contents/Resources/AppIcon.icns" ]; then
        cp "$MACOS_DIR/Cannonballs.icns" "$APP/Contents/Resources/AppIcon.icns"
    fi
    codesign --force --deep --sign - "$APP"
    echo "packaged $APP"
done
