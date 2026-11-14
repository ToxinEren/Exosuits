#!/usr/bin/env bash
# Controllo della grafica generata (dalla radice del repository): bash tools/check_art.sh
# Esegue le prove negative dei controlli (tools/art/selftest.py), poi rigenera geometria, atlante, maschera luminosa
# e animazioni in una cartella temporanea e li confronta con i file committati in
# src/main/resources/assets/redstone_exoframes: byte per byte i JSON; per le PNG (atlante e maschera) blocchi, IHDR e
# pixel decompressi (tools/art/compare_png.py), perche' i byte compressi dipendono dalla build di zlib. Poi rigenera
# allo stesso modo le texture placeholder (tools/gen_placeholders.py --out) e confronta ognuna con quella committata
# sotto src/main.
# Servono python3 e numpy (tools/art/requirements.txt).
set -eu

cd "$(dirname "$0")/.."

assets=src/main/resources/assets/redstone_exoframes
files=(geo/exoframe.geo.json textures/entity/exoframe.png textures/entity/exoframe_glowmask.png
       animations/exoframe.animation.json)

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

# prove negative dei controlli del generatore (lavorano in memoria, non scrivono nel repository)
if ! python3 tools/art/selftest.py >"$tmp/selftest.log" 2>&1; then
    cat "$tmp/selftest.log" >&2
    echo "FAIL: tools/art/selftest.py: un difetto iniettato non e' stato segnalato" >&2
    exit 1
fi

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

# texture placeholder (oggetti, icona del mod, layer armatura trasparenti di 1.20.1): stessi percorsi relativi a
# src/main
if ! python3 -I tools/gen_placeholders.py --out "$tmp/placeholders" >"$tmp/placeholders.log" 2>&1; then
    cat "$tmp/placeholders.log" >&2
    echo "FAIL: tools/gen_placeholders.py non e' riuscito" >&2
    exit 1
fi
count=0
while IFS= read -r -d '' g; do
    rel=${g#"$tmp/placeholders/"}
    count=$((count + 1))
    if [ ! -f "src/main/$rel" ]; then
        echo "FAIL: manca src/main/$rel (rigenerare con python3 -I tools/gen_placeholders.py)" >&2
        fail=1
    elif ! python3 tools/art/compare_png.py "$g" "src/main/$rel"; then
        echo "FAIL: src/main/$rel diverso da quello generato (python3 -I tools/gen_placeholders.py)" >&2
        fail=1
    fi
done < <(find "$tmp/placeholders" -type f -name '*.png' -print0)
if [ "$count" -eq 0 ]; then
    echo "FAIL: tools/gen_placeholders.py --out non ha scritto nessuna texture" >&2
    fail=1
fi
[ "$fail" -eq 0 ] || exit 1
echo "OK art"
