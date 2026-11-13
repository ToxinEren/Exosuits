#!/usr/bin/env bash
# Controlli sui jar finali (dalla radice del repository): bash tools/check_jars.sh
set -eu

cd "$(dirname "$0")/.."

fail=0
err() { echo "FAIL: $1: $2" >&2; fail=1; }

command -v python3 >/dev/null 2>&1 || { echo "FAIL: python3 non trovato: serve per leggere i metadati e per il controllo di privacy" >&2; exit 1; }

shopt -s nullglob
dir=${1:-build/libs/all}
jars=("$dir"/*.jar)

# L'insieme dei jar deve essere esattamente quello atteso.
expected=(
    redstone-exoframes-fabric-0.1.0+mc1.20.1.jar
    redstone-exoframes-fabric-0.1.0+mc1.21.1.jar
    redstone-exoframes-forge-0.1.0+mc1.20.1.jar
    redstone-exoframes-neoforge-0.1.0+mc1.21.1.jar
)
for e in "${expected[@]}"; do
    [ -f "$dir/$e" ] || err "$e" "jar atteso mancante in $dir"
done
for jar in "${jars[@]}"; do
    n=$(basename "$jar")
    found=0
    for e in "${expected[@]}"; do [ "$n" = "$e" ] && found=1; done
    [ "$found" -eq 1 ] || err "$n" "jar inatteso in $dir"
done
[ "$fail" -eq 0 ] || exit 1

for jar in "${jars[@]}"; do
    name=$(basename "$jar")
    case "$name" in
        *-fabric-*) loader=fabric ;;
        *-neoforge-*) loader=neoforge ;;
        *-forge-*) loader=forge ;;
        *) err "$name" "loader non riconoscibile dal nome"; continue ;;
    esac
    case "$name" in
        *+mc1.20.1.jar) mc=1.20.1; pack=15 ;;
        *+mc1.21.1.jar) mc=1.21.1; pack=34 ;;
        *) err "$name" "versione Minecraft non riconoscibile dal nome"; continue ;;
    esac

    listing=$(unzip -Z1 "$jar")

    # (a) un solo file di metadati, quello del loader del nome.
    case "$loader" in
        fabric) meta=fabric.mod.json ;;
        forge) meta=META-INF/mods.toml ;;
        neoforge) meta=META-INF/neoforge.mods.toml ;;
    esac
    for f in fabric.mod.json META-INF/mods.toml META-INF/neoforge.mods.toml; do
        if [ "$f" = "$meta" ]; then
            echo "$listing" | grep -qx "$f" || err "$name" "manca $f"
        else
            ! echo "$listing" | grep -qx "$f" || err "$name" "file di metadati estraneo $f"
        fi
    done

    # (b) pack_format.
    if ! unzip -p "$jar" pack.mcmeta 2>/dev/null | tr -d '[:space:]' | grep -q "\"pack_format\":$pack[,}]"; then
        err "$name" "pack_format diverso da $pack"
    fi

    # (c) intervallo Minecraft.
    case "$loader-$mc" in
        fabric-1.20.1) want='"minecraft": ">=1.20.1 <1.21"' ;;
        fabric-1.21.1) want='"minecraft": ">=1.21 <1.22"' ;;
        forge-1.20.1) want='versionRange="[1.20.1,1.21)"' ;;
        neoforge-1.21.1) want='versionRange="[1.21.1,1.22)"' ;;
        *) err "$name" "combinazione loader/mc inattesa $loader-$mc"; continue ;;
    esac
    if ! unzip -p "$jar" "$meta" 2>/dev/null | grep -qF "$want"; then
        err "$name" "intervallo Minecraft atteso $want assente in $meta"
    fi

    # (d) classe principale.
    echo "$listing" | grep -qx 'com/toxineren/exoframes/RedstoneExoframes.class' \
        || err "$name" "manca com/toxineren/exoframes/RedstoneExoframes.class"

    # (d2) le classi d'ingresso dichiarate nei metadati sono nel jar (anche l'entrypoint client di Fabric,
    #      che un avvio del server dedicato non carica mai).
    case "$loader" in
        fabric)
            entries=$(unzip -p "$jar" fabric.mod.json | python3 -c 'import json, sys
for value in json.load(sys.stdin).get("entrypoints", {}).values():
    for item in (value if isinstance(value, list) else [value]):
        print(item if isinstance(item, str) else item.get("value", ""))' 2>/dev/null) \
                || { err "$name" "fabric.mod.json assente o non leggibile"; entries=""; } ;;
        forge) entries=com.toxineren.exoframes.platform.RedstoneExoframesForge ;;
        neoforge) entries=com.toxineren.exoframes.platform.RedstoneExoframesNeoForge ;;
    esac
    [ -n "$entries" ] || err "$name" "nessuna classe d'ingresso dichiarata"
    for cls in $entries; do
        echo "$listing" | grep -qx "$(echo "$cls" | tr . /).class" \
            || err "$name" "classe d'ingresso $cls dichiarata nei metadati ma assente dal jar"
    done

    # (e) nessun refmap.
    refmaps=$(echo "$listing" | grep -E '\.refmap\.json$' || true)
    [ -z "$refmaps" ] || err "$name" "refmap presente: $refmaps"

    # (f) icona del mod alla radice del jar (referenziata da icon/logoFile).
    echo "$listing" | grep -qx 'icon.png' || err "$name" "manca icon.png alla radice"
done

# (g) nessun dato personale nei jar da pubblicare: percorsi locali, e-mail, metadati di immagini e suoni.
python3 tools/check_jar_privacy.py "${jars[@]}" || fail=1

[ "$fail" -eq 0 ] || exit 1
echo "OK jars"
