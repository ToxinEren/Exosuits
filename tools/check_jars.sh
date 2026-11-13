#!/usr/bin/env bash
# Controlli sui jar finali (dalla radice del repository): bash tools/check_jars.sh
set -e

cd "$(dirname "$0")/.."

fail=0
err() { echo "FAIL: $1: $2" >&2; fail=1; }

shopt -s nullglob
jars=(build/libs/all/*.jar)
if [ "${#jars[@]}" -ne 4 ]; then
    echo "FAIL: attesi 4 jar in build/libs/all, trovati ${#jars[@]}" >&2
    exit 1
fi

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
    esac
    if ! unzip -p "$jar" "$meta" 2>/dev/null | grep -qF "$want"; then
        err "$name" "intervallo Minecraft atteso $want assente in $meta"
    fi

    # (d) classe principale.
    echo "$listing" | grep -qx 'com/toxineren/exoframes/RedstoneExoframes.class' \
        || err "$name" "manca com/toxineren/exoframes/RedstoneExoframes.class"

    # (e) nessun refmap.
    refmaps=$(echo "$listing" | grep -E '\.refmap\.json$' || true)
    [ -z "$refmaps" ] || err "$name" "refmap presente: $refmaps"
done

[ "$fail" -eq 0 ] || exit 1
echo "OK jars"
