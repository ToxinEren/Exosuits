#!/usr/bin/env bash
# Controllo della grafica generata (dalla radice del repository): bash tools/check_art.sh
# Rigenera geometria, atlante, maschera luminosa e animazioni in una cartella temporanea e li confronta con i file
# committati in src/main/resources/assets/redstone_exoframes: byte per byte i JSON; per le PNG (atlante e maschera)
# blocchi, IHDR e pixel decompressi (tools/art/compare_png.py), perche' i byte compressi dipendono dalla build di
# zlib.
# Servono python3 e numpy (tools/art/requirements.txt).
set -eu

cd "$(dirname "$0")/.."

assets=src/main/resources/assets/redstone_exoframes
files=(geo/exoframe.geo.json textures/entity/exoframe.png textures/entity/exoframe_glowmask.png
       animations/exoframe.animation.json)

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
    elif [ "${f##*.}" = png ]; then
        # PNG: blocchi, IHDR e pixel decompressi, non i byte compressi (dipendono dalla build di zlib)
        if ! python3 tools/art/compare_png.py "$tmp/$f" "$assets/$f"; then
            echo "FAIL: $assets/$f diverso da quello generato (rigenerare con python3 tools/art/build.py)" >&2
            fail=1
        fi
    elif ! cmp -s "$tmp/$f" "$assets/$f"; then
        echo "FAIL: $assets/$f diverso da quello generato (rigenerare con python3 tools/art/build.py)" >&2
        fail=1
    fi
done
[ "$fail" -eq 0 ] || exit 1
echo "OK art"
