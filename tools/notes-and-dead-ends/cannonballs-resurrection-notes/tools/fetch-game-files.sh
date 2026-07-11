#!/bin/zsh
# Fetch the third-party files Cannonballs! needs (NOT hosted in this repo for copyright reasons):
#   - the WildTangent game installer + assets
#   - the Microsoft Java VM (a Web Driver dependency)
# Source: the Internet Archive community "wildtangent-archive" collection.
#
# Kenneth's own work (his website + the all-islands patch in resurrection-archive/patch/island.exe) IS in this repo.
#
# Usage:  ./fetch-game-files.sh [dest_dir]   (default: ./game-files)
set -e
DEST="${1:-./game-files}"
mkdir -p "$DEST"; cd "$DEST"
BASE="https://archive.org/download/wildtangent-archive/Games/Original/Download/Cannonballs"

echo "Downloading Cannonballs from the Internet Archive..."
# The base game (trial installer) + the Win10-64bit compatibility .bat
curl -fL -o "Cannonballs.zip"                 "$BASE/Cannonballs.zip"
curl -fL -o "Cannonballs_unofficial_copy.zip" "$BASE/Cannonballs%20%28unofficial%20copy%29.zip"
curl -fL -o "win1064bitunlock.bat"            "$BASE/cannonballs%20win1064bitunlock.bat"

echo
echo "The 'unofficial copy' zip contains Kenneth's full distribution: the WildTangent game"
echo "(cannonballs-setup.exe), the Microsoft Java VM (msjavx86.exe), the soundtrack, the wallpaper,"
echo "and island.zip (his Access-all-Islands patch — also kept in resurrection-archive/patch/island.exe)."
echo
echo "ALWAYS scan downloaded abandonware with VirusTotal before running, and inspect any .bat in a"
echo "text editor first."
echo "Done. Files in: $DEST"
