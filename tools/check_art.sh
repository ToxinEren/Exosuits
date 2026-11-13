#!/usr/bin/env bash
# Controllo della grafica generata (dalla radice del repository): bash tools/check_art.sh
# Rigenera geometria, atlante e animazioni in una cartella temporanea e li confronta byte per byte con i file
# committati in src/main/resources/assets/redstone_exoframes. Servono python3, Pillow e numpy.
set -eu

cd "$(dirname "$0")/.."

assets=src/main/resources/assets/redstone_exoframes
files=(geo/exoframe.geo.json textures/entity/exoframe.png animations/exoframe.animation.json)

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

if ! python3 tools/art/build.py --out "$tmp" >"$tmp/build.log" 2>&1; then
    cat "$tmp/build.log" >&2
    echo "FAIL: tools/art/build.py non e' riuscito" >&2
    exit 1
fi

fail=0
for f in "${files[@]}"; do
    if [ ! -f "$assets/$f" ]; then
        echo "FAIL: manca $assets/$f (rigenerare con python3 tools/art/build.py)" >&2
        fail=1
    elif ! cmp -s "$tmp/$f" "$assets/$f"; then
        echo "FAIL: $assets/$f diverso da quello generato (rigenerare con python3 tools/art/build.py)" >&2
        fail=1
    fi
done
[ "$fail" -eq 0 ] || exit 1
echo "OK art"
