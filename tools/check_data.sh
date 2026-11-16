#!/usr/bin/env bash
# Controllo dei dati generati (dalla radice del repository): bash tools/check_data.sh
# Esegue le prove negative dei controlli del generatore (tools/gen_data.py --selftest), poi rigenera ricette, sblocchi,
# bottino e tag in una cartella temporanea (--out) e li confronta byte per byte con i file committati sotto
# src/main/resources-mc1.20.1/data e src/main/resources-mc1.21.1/data. I due alberi contengono esattamente i file
# generati: uno in piu' fallisce quanto uno che manca, perche' un file scritto a mano in un solo albero, o nella
# cartella con il nome dell'altra versione, il gioco lo ignorerebbe in silenzio. Nelle risorse comuni
# (src/main/resources/data) restano solo le due strutture vuote dei test.
# Serve python3 (solo libreria standard).
set -eu

cd "$(dirname "$0")/.."

trees=(src/main/resources-mc1.20.1/data src/main/resources-mc1.21.1/data)
common=src/main/resources/data
common_allowed=(redstone_exoframes/structure/empty.nbt redstone_exoframes/structures/empty.nbt)

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

# prove negative di validate() (lavorano in memoria, non scrivono nel repository)
if ! python3 -I tools/gen_data.py --selftest >"$tmp/selftest.log" 2>&1; then
    cat "$tmp/selftest.log" >&2
    echo "FAIL: tools/gen_data.py --selftest: un difetto iniettato non e' stato segnalato" >&2
    exit 1
fi

if ! python3 -I tools/gen_data.py --out "$tmp/data" >"$tmp/gen.log" 2>&1; then
    cat "$tmp/gen.log" >&2
    echo "FAIL: tools/gen_data.py non e' riuscito" >&2
    exit 1
fi

fail=0

# ogni file generato c'e' ed e' identico
count=0
while IFS= read -r -d '' g; do
    rel=${g#"$tmp/data/"}
    count=$((count + 1))
    if [ ! -f "src/main/$rel" ]; then
        echo "FAIL: manca src/main/$rel (python3 -I tools/gen_data.py)" >&2
        fail=1
    elif ! cmp -s "$g" "src/main/$rel"; then
        echo "FAIL: src/main/$rel diverso da quello generato (python3 -I tools/gen_data.py)" >&2
        fail=1
    fi
done < <(find "$tmp/data" -type f -print0 | sort -z)
if [ "$count" -eq 0 ]; then
    echo "FAIL: tools/gen_data.py --out non ha scritto nessun file" >&2
    fail=1
fi

# nei due alberi per versione non c'e' nient'altro
for tree in "${trees[@]}"; do
    [ -d "$tree" ] || continue
    while IFS= read -r -d '' f; do
        if [ ! -f "$tmp/data/${f#src/main/}" ]; then
            echo "FAIL: $f non viene dal generatore" >&2
            fail=1
        fi
    done < <(find "$tree" ! -type d -print0 | sort -z)
done

# nelle risorse comuni nessun dato: una ricetta li' finirebbe nei jar delle due versioni, in una cartella che una delle
# due ignora
if [ -d "$common" ]; then
    while IFS= read -r -d '' f; do
        allowed=0
        for a in "${common_allowed[@]}"; do [ "$f" = "$common/$a" ] && allowed=1; done
        if [ "$allowed" -eq 0 ]; then
            echo "FAIL: $f: i dati per versione non stanno nelle risorse comuni" >&2
            fail=1
        fi
    done < <(find "$common" ! -type d -print0 | sort -z)
fi

[ "$fail" -eq 0 ] || exit 1
echo "OK data"
