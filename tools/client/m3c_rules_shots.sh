#!/usr/bin/env bash
# I valori del server ricevuti dal client vero (pacchetto client_rules, design par. 12.4), in una sola esecuzione di
# client_shot.sh (ogni passo parte dallo stato lasciato dal precedente):
#
#   EXO_ACCEPT_EULA=true bash tools/client/m3c_rules_shots.sh <variante> <cartella>
#
# Il server parte con una config diversa dai predefiniti (EXO_SHOT_SERVER_CONFIG, scritta in
# <cartella>/server-config.json): energy.cellCapacity 20 000 (predefinito 10 000), energy.cellSwapSeconds 4 (2),
# frame.exitHoldSeconds 3 (1). Il client resta ai predefiniti: client_shot.sh toglie il file di config dalla sua
# cartella, quindi tutto cio' che il client mostra o conta con i valori del server lo ha ricevuto con client_rules
# all'ingresso; con i predefiniti i controlli qui sotto non tornano. I due tasti da tenere contano sul client
# (ExitKey, CellSwapKey) e la barra della cella la disegna il client: sono i tre valori del pacchetto. I controlli sono
# comandi execute if/unless (uno che non torna ferma l'esecuzione con uscita 1); la barra va guardata nello scatto.
#
#   1. capacita'      give di una cella da 5 000 RE, inventario aperto: scatto regole-barra, la barra rossa della
#                     cella a un quarto (3 pixel su 13: 5 000 su 20 000), non a meta' (7 pixel: 5 000 su 10 000). Poi
#                     /exoframe debug wear rottame: la cella indossata e' piena alla capacita' del server, fra 19 900
#                     e 20 000 RE (il controllo dice che il server ha davvero la config cambiata)
#   2. uscita         V tenuto 2 s: il giocatore e' ancora nella tuta (nessun telaio piazzato); con il secondo del
#                     predefinito sarebbe uscito. Scatto regole-uscita a 1,5 s: l'avanzamento circa al 50 %
#   3. cambio cella   carica a 1 000 RE; R tenuto 3 s: nessun cambio (carica ancora sotto 1 000; con i 2 s del
#                     predefinito sarebbe entrata la cella da 5 000); R tenuto 5 s: il cambio c'e', carica fra 4 900 e
#                     5 000 RE
#   4. uscita vera    V tenuto 4 s: con i secondi del server si esce, e il telaio resta piazzato
#
# Alla fine il giocatore torna in 0,5 -60 -5,5 per i tre scatti di client_shot.sh. Dura circa due minuti a cache
# calde (misurati 95-128 s sulle quattro varianti). Le variabili d'ambiente sono quelle di client_shot.sh
# (EXO_ACCEPT_EULA obbligatoria; EXO_SHOT_SERVER_CONFIG la imposta questo script). Istruzioni in tools/client/README.md.
set -u

here=$(cd "$(dirname "$0")" && pwd)

if [ "$#" -ne 2 ]; then
    echo "Uso: EXO_ACCEPT_EULA=true bash tools/client/m3c_rules_shots.sh <variante> <cartella>" >&2
    exit 2
fi
variant=$1
out=$2
u=${EXO_SHOT_USER:-Exo}

# Cella: dati dello stack come NBT su 1.20.1, nel componente custom_data su 1.21.1. Una variante sconosciuta la rifiuta
# client_shot.sh.
case "$variant" in
    1.20.1-*) cell='redstone_exoframes:redstone_cell{exoframes:{energy:5000}}' ;;
    *) cell='redstone_exoframes:redstone_cell[minecraft:custom_data={exoframes:{energy:5000}}]' ;;
esac

mkdir -p "$out" || { echo "ERRORE: non riesco a creare la cartella $out" >&2; exit 2; }
# solo interi nei campi interi: un decimale renderebbe non valido tutto il file (design par. 12.9)
printf '%s\n' '{"energy": {"cellCapacity": 20000, "cellSwapSeconds": 4}, "frame": {"exitHoldSeconds": 3}}' \
    >"$out/server-config.json"

energy() { echo "execute store result score #$1 exo_shot run execute as $u run exoframe debug energy"; }
worn="execute unless entity @e[type=redstone_exoframes:exoframe]" # nessun telaio piazzato: la tuta e' ancora addosso

steps=(
    "gamemode survival $u"
    # capacita': la cella da 5 000 RE mostra la barra a un quarto (20 000 del server), non a meta' (10 000 del client)
    "give $u $cell" "@attendi 2" "@tieni e 0.2" "@attendi 1" "@scatto regole-barra" "@tieni Escape 0.2"
    "execute as $u run exoframe debug wear rottame" "@attendi 2"
    "$(energy cap)" "execute if score #cap exo_shot matches 19900..20000"
    # uscita: 3 s sul server; con il predefinito del client (1 s) V tenuto 2 s farebbe uscire
    "$worn" "@tieni v 2 regole-uscita 1.5" "@attendi 1" "$worn"
    # cambio cella: 4 s sul server; con il predefinito del client (2 s) R tenuto 3 s cambierebbe la cella
    "execute as $u run exoframe debug energy 1000" "@attendi 1"
    "@tieni r 3" "@attendi 1" "$(energy prima)" "execute if score #prima exo_shot matches ..1000"
    "@tieni r 5" "@attendi 1" "$(energy dopo)" "execute if score #dopo exo_shot matches 4900..5000"
    # e con i secondi del server si esce davvero: V tenuto 4 s lascia il telaio piazzato
    "@tieni v 4" "@attendi 1" "execute if entity @e[type=redstone_exoframes:exoframe]"
    "tp $u 0.5 -60 -5.5 0 15"
)

echo "=== $variant regole"
# un'uscita anticipata di client_shot.sh (uso o ambiente sbagliati) non lascia leggere gli avvisi di un'altra esecuzione
rm -f "$out/log-check.txt"
EXO_SHOT_SERVER_CONFIG="$out/server-config.json" bash "$here/client_shot.sh" "$variant" "$out" "${steps[@]}"
code=$?
if [ "$code" -eq 0 ]; then
    echo "OK $variant: scatti in $out"
elif [ -s "$out/log-check.txt" ]; then
    echo "FAIL $variant: avvisi del mod fuori elenco in $out/log-check.txt" >&2
else
    echo "FAIL $variant: client_shot.sh e' uscito con $code" >&2
fi
exit "$code"
