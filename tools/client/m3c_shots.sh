#!/usr/bin/env bash
# La sequenza della parte 3c-1 ("consumare") nel client vero, in sopravvivenza, in una sola esecuzione di
# client_shot.sh (ogni passo parte dallo stato lasciato dal precedente):
#
#   EXO_ACCEPT_EULA=true bash tools/client/m3c_shots.sh <variante> <cartella>
#
# Il giocatore passa in sopravvivenza e indossa la tuta Rottame con /exoframe debug wear rottame (cella carica a 10 000
# RE). Prima le costruzioni della ricognizione (docs/superpowers/specs/ricognizioni/2026-11-15-m3c-movimento-e-dati.md):
# una vasca profonda 3 dentro pareti di vetro (x 10-14, z -2..12), un canale d'acqua profondo 1 (x 20-24, y -61), una
# sorgente d'acqua per la corrente (40 -60 -20) e uno scalino di pietra alto 1 (x -6..-3, z 3..10). Le soglie sono
# quelle della ricognizione con le distanze moltiplicate per 0,85: la tuta applica frame.baseSpeedPenalty (Task 14),
# che la ricognizione non aveva. Dopo ogni passo "data get entity Pos" scrive la posizione in rcon.log, e un comando
# execute if/unless la controlla (un controllo che non torna ferma l'esecuzione):
#
#   1. affondare          tp sopra la vasca (Y -58): dopo 1,5 s la tuta e' sul fondo, entro 0,1 da 12,5 -60 0,5
#   2. Spazio sul fondo   Spazio tenuto 3 s: la tuta resta sul fondo, entro 0,1 (senza la regola dell'acqua salirebbe
#                         a galla, Y -57,5); scatto vasca-spazio a 2 s: sott'acqua sul fondo, niente posa a nuoto
#   3. avanti sul fondo   W tenuto 2 s: piu' di 6 blocchi dal punto di partenza (velocita' di terra della tuta, circa
#                         7,2: 8,5 della ricognizione x 0,85)
#   4. acqua profonda 1   Spazio tenuto 2 s nel canale: la tuta non si muove (entro 0,1); poi W 1,5 s verso est e la
#                         tuta esce dalla sponda alta 1 con il gradino: Y -60, entro 1,2 da 28 -60 0,5 (la ricognizione
#                         misura X 28,68-28,93 senza il fattore 0,85, circa 27,7-28 con; senza gradino la tuta resta a
#                         Y -61, X 24,70, a piu' di 3,4 da quel punto)
#   5. corrente           3 s nell'acqua che scorre: la tuta non si sposta (entro 0,05)
#   6. scalino            W 1,5 s verso lo scalino alto 1: la tuta ci sale senza saltare, entro 1,5 da -4,5 -59 6,8
#                         (senza gradino resta a Y -60 contro lo scalino, a piu' di 3,9 da quel punto)
#   7. corsa              Ctrl+W 2 s a 999 RE (sotto il 10 %, LOW): niente scatto, fra 6 e 8,2 blocchi; poi a 5 000 RE
#                         lo scatto c'e': piu' di 8,6 blocchi (le soglie del Task 23)
#   8. HUD dell'energia   carica letta prima e dopo 5 s di cammino (/exoframe debug energy): dopo < prima; scatti
#                         hud-energia (verde, percentuale sotto il 50 %), hud-energia-bassa (500 RE: rosso, sotto il
#                         10 %), hud-energia-esaurita (0 RE, grigio, 0 %); poi di nuovo 5 000 RE
#   9. barre              give di una cella da 5 000 RE e di un elmo Rottame a 90/180, inventario aperto: scatto
#                         barre-inventario (cella con la barra rossa a meta', elmo con la barra di durata a meta')
#  10. cambio cella       carica a 1 000 RE, R tenuto 3 s (scatto cambio-cella-tenuto a 1 s: l'avanzamento sulla barra
#                         delle azioni): installata la cella da 5 000 (carica fra 4 900 e 5 000, meno il consumo da
#                         fermi) e nell'inventario una cella sola (la vecchia al posto della nuova); scatto
#                         cambio-cella-fatto
#
# L'HUD sta in basso, nell'angolo opposto alla mano principale (parte 4c). Alla fine il giocatore torna in 0,5 -60 -5,5
# per i tre scatti di client_shot.sh. Gli avvisi "moved too quickly" del server dopo un tp sono un artefatto noto della
# ricognizione; "moved wrongly" non deve comparire in server.log: se compare (client e server non d'accordo sul
# movimento della tuta, per esempio in acqua) lo script scrive una riga ERRORE ed esce con 1, anche se client_shot.sh e'
# uscito con 0 (la riga e' del gioco e non nomina la mod, quindi check_log.py non la vede). Dura circa due minuti a
# cache calde (misurati 107-127 s sulle quattro varianti). Le variabili d'ambiente sono quelle di client_shot.sh
# (EXO_ACCEPT_EULA obbligatoria). Istruzioni in tools/client/README.md.
set -u
# Soglie valide solo con la config predefinita: una EXO_SHOT_SERVER_CONFIG ereditata dall'ambiente le falserebbe.
unset EXO_SHOT_SERVER_CONFIG
# E con le preferenze del client predefinite: un EXO_SHOT_CLIENT_PREFS ereditato potrebbe nascondere l'HUD degli scatti.
unset EXO_SHOT_CLIENT_PREFS

here=$(cd "$(dirname "$0")" && pwd)

if [ "$#" -ne 2 ]; then
    echo "Uso: EXO_ACCEPT_EULA=true bash tools/client/m3c_shots.sh <variante> <cartella>" >&2
    exit 2
fi
variant=$1
out=$2
u=${EXO_SHOT_USER:-Exo}

# Cella e pezzo: dati dello stack come NBT su 1.20.1, nel componente custom_data su 1.21.1. Una variante sconosciuta
# la rifiuta client_shot.sh.
case "$variant" in
    1.20.1-*) cell='redstone_exoframes:redstone_cell{exoframes:{energy:5000}}'
              piece='redstone_exoframes:scrap_helmet{exoframes:{tier:"scrap",durability:90,max_durability:180}}' ;;
    *) cell='redstone_exoframes:redstone_cell[minecraft:custom_data={exoframes:{energy:5000}}]'
       piece='redstone_exoframes:scrap_helmet[minecraft:custom_data={exoframes:{tier:"scrap",durability:90,max_durability:180}}]' ;;
esac

P="data get entity $u Pos"
at() { echo "execute if entity @a[name=$u,x=$1,y=$2,z=$3,distance=..$4]"; }      # entro r dal punto
beyond() { echo "execute unless entity @a[name=$u,x=$1,y=$2,z=$3,distance=..$4]"; } # oltre r dal punto
energy() { echo "execute store result score #$1 exo_shot run execute as $u run exoframe debug energy"; }
set_energy() { echo "execute as $u run exoframe debug energy $1"; }

steps=(
    "gamemode survival $u"
    # le costruzioni della ricognizione: vasca profonda 3, acqua profonda 1, corrente, scalino alto 1
    "fill 9 -60 -3 15 -57 13 glass" "fill 10 -60 -2 14 -57 12 air" "fill 10 -60 -2 14 -58 12 water"
    "fill 20 -61 -2 24 -61 12 water"
    "setblock 40 -60 -20 water"
    "fill -6 -60 3 -3 -60 10 stone"
    "@attendi 3"
    "execute as $u run exoframe debug wear rottame" "@attendi 2"
    # 1. affondare
    "tp $u 12.5 -58 0.5 0 0" "@attendi 1.5" "$P" "$(at 12.5 -60 0.5 0.1)"
    # 2. Spazio sul fondo: nessuna risalita
    "tp $u 12.5 -60 0.5 0 0" "@attendi 1" "@tieni space 3 vasca-spazio 2" "$P" "$(at 12.5 -60 0.5 0.1)"
    # 3. avanti sul fondo alla velocita' di terra
    "tp $u 12.5 -60 0.5 0 0" "@attendi 1" "@tieni w 2" "$P" "$(beyond 12.5 -60 0.5 6)"
    # 4. acqua profonda 1: niente salto; poi l'uscita dalla sponda alta 1, verso est
    "tp $u 22.5 -61 0.5 0 0" "@attendi 1" "@tieni space 2" "$P" "$(at 22.5 -61 0.5 0.1)"
    "tp $u 22.5 -61 0.5 -90 0" "@attendi 1" "@tieni w 1.5" "$P" "$(at 28 -60 0.5 1.2)"
    # 5. la corrente non sposta la tuta
    "tp $u 42.5 -60 -19.5 0 0" "@attendi 0.2" "@attendi 3" "$P" "$(at 42.5 -60 -19.5 0.05)"
    # 6. scalino alto 1 senza saltare
    "tp $u -4.5 -60 0.5 0 0" "@attendi 1" "@tieni w 1.5" "$P" "$(at -4.5 -59 6.8 1.5)"
    # 7. corsa: niente scatto sotto il 10 %, scatto a 5 000 RE
    "$(set_energy 999)" "tp $u 0.5 -60 -5.5 180 0" "@attendi 1" "@tieni ctrl+w 2" "$P"
    "$(at 0.5 -60 -5.5 8.2)" "$(beyond 0.5 -60 -5.5 6)"
    "$(set_energy 5000)" "tp $u 0.5 -60 -5.5 180 0" "@attendi 1" "@tieni ctrl+w 2" "$P"
    "$(beyond 0.5 -60 -5.5 8.6)"
    # 8. l'HUD dell'energia: la carica cala camminando; poi bassa ed esaurita
    "$(energy prima)" "tp $u 0.5 -60 -5.5 180 0" "@attendi 1" "@tieni w 5" "$(energy dopo)"
    "execute if score #dopo exo_shot < #prima exo_shot" "@scatto hud-energia"
    "$(set_energy 500)" "@attendi 1" "@scatto hud-energia-bassa"
    "$(set_energy 0)" "@attendi 1" "@scatto hud-energia-esaurita"
    "$(set_energy 5000)"
    # 9. barre della cella e del pezzo nell'inventario
    "give $u $cell" "give $u $piece" "@attendi 1" "@tieni e 0.2" "@attendi 1" "@scatto barre-inventario"
    "@tieni Escape 0.2"
    # 10. cambio cella tenendo R: entra la cella da 5 000, la vecchia torna al suo posto
    "$(set_energy 1000)" "@attendi 1" "@tieni r 3 cambio-cella-tenuto 1" "@attendi 1"
    "$(energy cella)" "execute if score #cella exo_shot matches 4900..5000"
    "execute store result score #celle exo_shot run clear $u redstone_exoframes:redstone_cell 0"
    "execute if score #celle exo_shot matches 1"
    "@scatto cambio-cella-fatto"
    # 11. di nuovo all'aperto per i tre scatti finali
    "tp $u 0.5 -60 -5.5 0 15"
)

echo "=== $variant m3c"
# un'uscita anticipata di client_shot.sh (uso o ambiente sbagliati) non lascia leggere il log di un'altra esecuzione
rm -f "$out/server.log"
bash "$here/client_shot.sh" "$variant" "$out" "${steps[@]}"
code=$?
# Client e server d'accordo sul movimento (acqua, gradino): il server scrive "moved wrongly" quando la posizione del
# client non e' quella che calcola lui. server.log c'e' anche dopo un errore (lo copia la chiusura di client_shot.sh).
wrong=$(grep -c 'moved wrongly' "$out/server.log" 2>/dev/null)
wrong=${wrong:-0}
if [ "$wrong" -gt 0 ]; then
    echo "ERRORE: $wrong righe 'moved wrongly' in $out/server.log: client e server non sono d'accordo sul movimento della tuta" >&2
    [ "$code" -eq 0 ] && code=1
fi
if [ "$code" -eq 0 ]; then
    echo "OK $variant: scatti in $out"
elif [ -s "$out/log-check.txt" ]; then
    echo "FAIL $variant: avvisi del mod fuori elenco in $out/log-check.txt" >&2
elif [ "$wrong" -gt 0 ]; then
    echo "FAIL $variant: moved wrongly in $out/server.log" >&2
else
    echo "FAIL $variant: client_shot.sh e' uscito con $code" >&2
fi
exit "$code"
