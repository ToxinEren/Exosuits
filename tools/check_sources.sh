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

# 1b. Il core non usa Gson (la serializzazione resta fuori dal core).
if [ -d "$JAVA/core" ]; then
    bad=$(grep -rlE '^[[:space:]]*import[[:space:]]+(static[[:space:]]+)?com\.google\.gson[.;]' "$JAVA/core" || true)
    if [ -n "$bad" ]; then
        echo "FAIL: import di com.google.gson nel package core:" >&2
        echo "$bad" >&2
        fail=1
    fi
fi

# 1c. Ogni tipo di core/ (classe, record, enum, interfaccia) e' citato per nome in almeno un file di src/test/java.
if [ -d "$JAVA/core" ]; then
    while IFS= read -r f; do
        type=$(basename "$f" .java)
        if ! grep -rlw "$type" src/test/java >/dev/null 2>&1; then
            echo "FAIL: il tipo core $type ($f) non e' citato in nessun test di src/test/java" >&2
            fail=1
        fi
    done < <(find "$JAVA/core" -name '*.java' | sort)
fi

# Direttive Stonecutter: si riconosce il marcatore (//? //$ //~ e le stesse in un commento a blocco), non la parola
# "if", cosi' valgono anche "//?if", le condizioni senza "if" e le direttive di scambio e sostituzione.
DIRECTIVE='(//|/\*)[?$~]'
directives=$(grep -rlE "$DIRECTIVE" src/main/java src/test/java || true)

# 2. Direttive Stonecutter solo in compat/ e platform/ di src/main/java (nessuna nei test).
bad=$(printf '%s\n' "$directives" | grep -v '^$' | grep -vE "^$JAVA/(compat|platform)/" || true)
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

# 4. Le implementazioni di compat/impl si referenziano (import, anche static, o nome qualificato) solo da file di
#    compat/, in src/main/java e src/test/java (il resto passa da Compat e dalle interfacce).
bad=$(grep -rlE 'com\.toxineren\.exoframes\.compat\.impl\.' src/main/java src/test/java | grep -vE '^src/(main|test)/java/com/toxineren/exoframes/compat/' || true)
if [ -n "$bad" ]; then
    echo "FAIL: riferimenti a compat.impl fuori da compat/:" >&2
    echo "$bad" >&2
    fail=1
fi

# 5. Ogni file che registra un pacchetto C2S (tranne Net e NetImpl) referenzia Compat.frameState(): i gestori lato
#    server validano le richieste sullo stato del telaio del giocatore.
#    Le righe di commento non contano. La regola guarda solo il file che registra: registrazione e gestori stanno
#    nello stesso file.
bad=$(grep -rl 'registerC2S(' src/main/java | grep -vE "^$JAVA/compat/(Net|impl/NetImpl)\.java$" | while IFS= read -r f; do
    grep -vE '^[[:space:]]*(//|\*|/\*)' "$f" | grep -q 'Compat\.frameState()' || echo "$f"
done)
if [ -n "$bad" ]; then
    echo "FAIL: registrazione C2S senza Compat.frameState() (validazione lato server):" >&2
    echo "$bad" >&2
    fail=1
fi

# 6. Regola finale sulle direttive Stonecutter: oltre a compat/impl/ e platform/ le ammette solo compat/Ids.java,
#    la fabbrica degli id usata direttamente dal codice dei contenuti. Le interfacce di compat/ restano senza versione.
bad=$(printf '%s\n' "$directives" | grep -v '^$' | grep -vE "^$JAVA/(compat/impl|platform)/" | grep -vxF "$JAVA/compat/Ids.java" || true)
if [ -n "$bad" ]; then
    echo "FAIL: condizioni Stonecutter fuori da compat/impl/, platform/ e compat/Ids.java:" >&2
    echo "$bad" >&2
    fail=1
fi

if [ "$fail" -ne 0 ]; then
    exit 1
fi
echo "OK sources"
