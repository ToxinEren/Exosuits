#!/usr/bin/env bash
# Test in gioco (GameTest) senza client, dalla radice del repository: bash tools/run_gametests.sh [variante...]
# Senza argomenti prova le quattro varianti, una chiamata Gradle per variante, e continua anche se una fallisce.
# Una variante passa solo se Gradle esce con 0 E il log contiene "All N required tests passed" con N > 0: su Forge e
# NeoForge un server partito senza test esce comunque con 0. Il log di ogni variante resta in
# versions/<variante>/build/gametest/server.log.
set -u

cd "$(dirname "$0")/.."

if [ "$#" -gt 0 ]; then
    variants=("$@")
else
    variants=(1.20.1-forge 1.20.1-fabric 1.21.1-fabric 1.21.1-neoforge)
fi

fail=0
for v in "${variants[@]}"; do
    log="versions/$v/build/gametest/server.log"
    mkdir -p "$(dirname "$log")"
    ./gradlew --console=plain ":$v:runGameTestServer" 2>&1 | tee "$log"
    code=${PIPESTATUS[0]}
    passed=$(grep -oE 'All [0-9]+ required tests passed' "$log" | tail -n 1 | grep -oE '[0-9]+' || true)
    if [ "$code" -ne 0 ]; then
        echo "FAIL $v: Gradle e' uscito con $code" >&2
        fail=1
    elif [ -z "$passed" ] || [ "$passed" -eq 0 ]; then
        echo "FAIL $v: uscita 0 ma nessuna riga 'All N required tests passed' con N > 0 (i test non sono girati)" >&2
        fail=1
    else
        echo "OK $v: $passed test obbligatori passati"
    fi
done

exit "$fail"
