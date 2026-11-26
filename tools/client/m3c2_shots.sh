#!/usr/bin/env bash
# La sequenza della parte 3c-2 ("ricaricare") nel client vero, in sopravvivenza, in una sola esecuzione di
# client_shot.sh (ogni passo parte dallo stato lasciato dal precedente):
#
#   EXO_ACCEPT_EULA=true bash tools/client/m3c2_shots.sh <variante> <cartella>
#
# Il giocatore passa in sopravvivenza e resta in 0,5 -60 0,5 rivolto a sud. Con l'inclinazione 55 il mirino cade
# sull'erba del blocco 0 -61 1 (a z 1,63): il caricatore posato li' finisce in 0 -60 1, con il fronte a nord, verso il
# giocatore; con l'inclinazione 60 il mirino e' sul fianco nord di quel caricatore. I controlli sono comandi del server
# (execute if, recipe take, clear ... 0): uno che non torna ferma l'esecuzione con uscita 1. I numeri letti dal blocco
# (data get block ... cell_energy) restano in rcon.log.
#
#   1. sblocco        give di un lingotto di rame; dopo 2 s "recipe take" riesce per ciascuna delle sette ricette
#                     della mod (riesce solo se il giocatore la conosce: la ha sbloccata il lingotto)
#   2. bottino        loot give della tabella blocks/charger: nell'inventario c'e' un caricatore
#   3. posa           con il caricatore in mano, clic destro sull'erba: in 0 -60 1 c'e' il caricatore, fronte a nord,
#                     senza cella e spento; scatto caricatore-posato (il fronte vuoto verso il giocatore)
#   4. cella          clic destro con una cella da 3 000 RE: il blocco tiene la cella con 3 000 RE (has_cell,
#                     cell_energy) e mostra lo stato cell=true, lit=false
#   5. polvere        clic destro con 8 polveri di redstone: scatto caricatore-acceso mezzo secondo dopo il clic (il
#                     fronte acceso e la riga dello stato "Charger: charging 30%"); poi lo stato cell=true, lit=true,
#                     e la carica letta due volte a 4 s di distanza: la seconda e' maggiore
#   6. ritiro         clic destro a mano vuota: scatto caricatore-svuotato un secondo dopo ("Charger: no cell", la
#                     cella in mano); lo stato torna cell=false, lit=false e nell'inventario c'e' una cella
#   7. rottura        la cella torna dentro (clic destro), poi in creativa un clic sinistro rompe il blocco: al suo
#                     posto c'e' aria e a terra ci sono la cella e la polvere rimasta. Il giocatore si sposta subito a
#                     sei blocchi: gli oggetti cadono fermi al centro del blocco, a un blocco da lui, e dopo il ritardo
#                     di raccolta (40 tick) li prenderebbe, anche in creativa, prima dei controlli
#   8. all'aperto     di nuovo in 0,5 -60 -5,5 per i tre scatti di client_shot.sh
#
# Non prova: il clic da accovacciati (la lettura dello stato a mani vuote) e il banco da lavoro, perche' lo strumento
# non clicca con un tasto premuto e non trascina oggetti; i suoni (non c'e' un dispositivo audio); le tramogge, che
# provano i test in mondo con tramogge vere (ChargerGameTests). Sono nella checklist per il PC,
# docs/test-in-gioco/m3c-giocarci.md. Dura circa un minuto e mezzo a cache calde (misurati 80-100 s sulle quattro
# varianti). Le variabili d'ambiente sono quelle di client_shot.sh (EXO_ACCEPT_EULA obbligatoria). Istruzioni in
# tools/client/README.md.
set -u
# Soglie valide solo con la config predefinita: una EXO_SHOT_SERVER_CONFIG ereditata dall'ambiente le falserebbe.
unset EXO_SHOT_SERVER_CONFIG
# E con le preferenze del client predefinite: un EXO_SHOT_CLIENT_PREFS ereditato potrebbe nascondere l'HUD degli scatti.
unset EXO_SHOT_CLIENT_PREFS

here=$(cd "$(dirname "$0")" && pwd)

if [ "$#" -ne 2 ]; then
    echo "Uso: EXO_ACCEPT_EULA=true bash tools/client/m3c2_shots.sh <variante> <cartella>" >&2
    exit 2
fi
variant=$1
out=$2
u=${EXO_SHOT_USER:-Exo}

# Cella: dati dello stack come NBT su 1.20.1, nel componente custom_data su 1.21.1. Una variante sconosciuta la rifiuta
# client_shot.sh.
case "$variant" in
    1.20.1-*) cell='redstone_exoframes:redstone_cell{exoframes:{energy:3000}}' ;;
    *) cell='redstone_exoframes:redstone_cell[minecraft:custom_data={exoframes:{energy:3000}}]' ;;
esac

c="0 -60 1" # il caricatore
block="execute if block $c redstone_exoframes:charger"
energy() { echo "execute store result score #$1 exo_shot run data get block $c cell_energy"; }
on_ground() { echo "execute if entity @e[type=minecraft:item,nbt={Item:{id:\"$1\"}}]"; }

steps=(
    "gamemode survival $u"
    # 1. sblocco: il lingotto di rame sblocca le sette ricette (recipe take riesce solo su una ricetta conosciuta)
    "give $u minecraft:copper_ingot" "@attendi 2"
    "recipe take $u redstone_exoframes:exoframe" "recipe take $u redstone_exoframes:scrap_helmet"
    "recipe take $u redstone_exoframes:scrap_torso" "recipe take $u redstone_exoframes:scrap_arm"
    "recipe take $u redstone_exoframes:scrap_leg" "recipe take $u redstone_exoframes:redstone_cell"
    "recipe take $u redstone_exoframes:charger"
    # 2. bottino: la tabella del blocco da' un caricatore
    "loot give $u loot redstone_exoframes:blocks/charger"
    "execute store result score #car exo_shot run clear $u redstone_exoframes:charger 0"
    "execute if score #car exo_shot matches 1"
    # 3. posa con l'oggetto: il fronte verso chi lo piazza
    "item replace entity $u weapon.mainhand with redstone_exoframes:charger"
    "tp $u 0.5 -60 0.5 0 55" "@attendi 1" "@click destro" "@attendi 1"
    "$block[facing=north,cell=false,lit=false]" "@scatto caricatore-posato"
    # 4. cella da 3 000 RE
    "tp $u 0.5 -60 0.5 0 60" "item replace entity $u weapon.mainhand with $cell" "@attendi 1"
    "@click destro" "@attendi 1"
    "execute if data block $c {has_cell:1b,cell_energy:3000}" "$block[cell=true,lit=false]"
    # 5. polvere e carica: lo scatto mezzo secondo dopo il clic, quando la riga dello stato c'e' ancora
    "item replace entity $u weapon.mainhand with minecraft:redstone 8" "@click destro" "@attendi 0.5"
    "@scatto caricatore-acceso" "@attendi 1.5" "$block[cell=true,lit=true]"
    "$(energy e1)" "@attendi 4" "$(energy e2)" "execute if score #e2 exo_shot > #e1 exo_shot"
    # 6. ritiro a mano vuota: lo scatto subito dopo l'attesa, prima dei controlli (la riga dello stato svanisce fra 2 e
    # 3 s dal clic)
    "@click destro" "@attendi 1" "@scatto caricatore-svuotato" "$block[cell=false,lit=false]"
    "execute store result score #celle exo_shot run clear $u redstone_exoframes:redstone_cell 0"
    "execute if score #celle exo_shot matches 1"
    # 7. rottura: la cella torna dentro, poi in creativa un clic sinistro rompe il blocco. Via dal raggio di raccolta
    # prima che finisca il ritardo: cella e polvere cadono ferme al centro del blocco, a un blocco dal giocatore, con
    # un ritardo di raccolta di 40 tick, e dopo 2 s lui le raccoglierebbe, anche in creativa
    "@click destro" "@attendi 1" "gamemode creative $u" "@click sinistro" "@attendi 0.3"
    "tp $u 0.5 -60 -5.5 0 15"
    "execute if block $c minecraft:air"
    "$(on_ground redstone_exoframes:redstone_cell)" "$(on_ground minecraft:redstone)"
    "gamemode survival $u"
    # 8. di nuovo all'aperto per i tre scatti finali
    "tp $u 0.5 -60 -5.5 0 15"
)

echo "=== $variant m3c2"
# un'uscita anticipata di client_shot.sh (uso o ambiente sbagliati) non lascia leggere gli avvisi di un'altra esecuzione
rm -f "$out/log-check.txt"
bash "$here/client_shot.sh" "$variant" "$out" "${steps[@]}"
code=$?
if [ "$code" -eq 0 ]; then
    echo "OK $variant: scatti in $out"
elif [ -s "$out/log-check.txt" ]; then
    echo "FAIL $variant: avvisi del mod fuori elenco in $out/log-check.txt" >&2
else
    echo "FAIL $variant: client_shot.sh e' uscito con $code" >&2
fi
exit "$code"
