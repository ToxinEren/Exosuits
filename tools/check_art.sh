#!/usr/bin/env bash
# Controllo della grafica generata (dalla radice del repository): bash tools/check_art.sh
# Esegue le prove negative dei controlli (tools/art/selftest.py), poi rigenera geometria, atlante, maschera luminosa
# e animazioni in una cartella temporanea e li confronta con i file committati in
# src/main/resources/assets/redstone_exoframes: byte per byte i JSON; per le PNG (atlante e maschera) blocchi, IHDR e
# pixel decompressi (tools/art/compare_png.py), perche' i byte compressi dipendono dalla build di zlib. Poi rigenera
# allo stesso modo la stazione di manutenzione nella direzione scelta dal proprietario (tools/art/station.py
# --direction officina: i modelli delle due meta', le texture e l'icona) e la confronta con i file committati, JSON
# byte per byte e PNG con compare_png.py; ogni modello o texture della stazione committato deve essere uno di quelli
# generati. Infine rigenera le texture placeholder (tools/gen_placeholders.py --out) e confronta ognuna con quella
# committata sotto src/main.
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

# stazione di manutenzione (parte 4d): la direzione A, Officina, scelta dal proprietario il 27 novembre 2026;
# tools/art/station.py scrive gli stessi percorsi relativi agli asset del mod, in una sottocartella sua
station_direction=officina
station_regen="python3 tools/art/station.py --direction $station_direction --out $assets"
if ! python3 tools/art/station.py --direction "$station_direction" --out "$tmp/station" >"$tmp/station.log" 2>&1; then
    cat "$tmp/station.log" >&2
    echo "FAIL: tools/art/station.py non e' riuscito" >&2
    exit 1
fi
count=0
while IFS= read -r -d '' g; do
    rel=${g#"$tmp/station/"}
    count=$((count + 1))
    if [ ! -f "$assets/$rel" ]; then
        echo "FAIL: manca $assets/$rel (rigenerare con $station_regen)" >&2
        fail=1
    elif [ "${rel##*.}" = png ]; then
        if ! python3 tools/art/compare_png.py "$g" "$assets/$rel"; then
            echo "FAIL: $assets/$rel diverso da quello generato (rigenerare con $station_regen)" >&2
            fail=1
        fi
    elif ! cmp -s "$g" "$assets/$rel"; then
        echo "FAIL: $assets/$rel diverso da quello generato (rigenerare con $station_regen)" >&2
        fail=1
    fi
done < <(find "$tmp/station" -type f -print0)
if [ "$count" -eq 0 ]; then
    echo "FAIL: tools/art/station.py non ha scritto nessun file" >&2
    fail=1
fi
# il generatore e' l'unico a scrivere modelli, texture e icona della stazione: nessun file della stazione fuori da
# quelli che ha scritto (il blockstate e il modello dell'oggetto, scritti a mano, stanno in altre cartelle)
for g in "$assets"/models/block/station*.json "$assets"/textures/block/station*.png \
        "$assets"/textures/item/station*.png; do
    [ -e "$g" ] || continue
    if [ ! -f "$tmp/station/${g#"$assets/"}" ]; then
        echo "FAIL: $g non viene da tools/art/station.py (la direzione $station_direction non lo scrive): va tolto" >&2
        fail=1
    fi
done

# texture placeholder (oggetti, facce del blocco caricatore, icona del mod, layer armatura trasparenti di 1.20.1):
# stessi percorsi relativi a src/main
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
