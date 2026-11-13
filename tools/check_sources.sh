#!/usr/bin/env bash
# Controlli statici sui sorgenti (dalla radice del repository): bash tools/check_sources.sh
set -e

cd "$(dirname "$0")/.."

JAVA=src/main/java/com/toxineren/exoframes
fail=0

# 1. Il package core non dipende da Minecraft, dai loader o da Architectury (il package puo' non esistere ancora).
if [ -d "$JAVA/core" ]; then
    bad=$(grep -rlE '^[[:space:]]*import[[:space:]]+(static[[:space:]]+)?(net\.minecraft|net\.fabricmc|net\.minecraftforge|net\.neoforged|dev\.architectury)\.' "$JAVA/core" || true)
    if [ -n "$bad" ]; then
        echo "FAIL: import di piattaforma nel package core:" >&2
        echo "$bad" >&2
        fail=1
    fi
fi

# 2. Commenti condizionali Stonecutter solo in compat/ e platform/.
bad=$(grep -rl '//? if' "$JAVA" | grep -vE "^$JAVA/(compat|platform)/" || true)
if [ -n "$bad" ]; then
    echo "FAIL: condizioni Stonecutter fuori da compat/ e platform/:" >&2
    echo "$bad" >&2
    fail=1
fi

# 3. Forge: il bus del mod va registrato presso Architectury prima di RedstoneExoframes.init().
FORGE="$JAVA/platform/RedstoneExoframesForge.java"
bus_line=$(grep -n 'EventBuses.registerModEventBus' "$FORGE" | head -n 1 | cut -d: -f1 || true)
init_line=$(grep -n 'RedstoneExoframes.init()' "$FORGE" | head -n 1 | cut -d: -f1 || true)
if [ -z "$bus_line" ] || [ -z "$init_line" ] || [ "$bus_line" -ge "$init_line" ]; then
    echo "FAIL: $FORGE deve chiamare EventBuses.registerModEventBus prima di RedstoneExoframes.init()" >&2
    fail=1
fi

if [ "$fail" -ne 0 ]; then
    exit 1
fi
echo "OK sources"
