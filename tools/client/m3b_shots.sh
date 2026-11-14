#!/usr/bin/env bash
# La sequenza della parte 3b ("entrare e uscire") nel client vero, con clic e tasti veri, in una sola esecuzione di
# client_shot.sh (ogni passo parte dallo stato lasciato dal precedente):
#
#   EXO_ACCEPT_EULA=true bash tools/client/m3b_shots.sh <variante> <cartella>
#
# Il giocatore e' in sopravvivenza (gli oggetti lasciano davvero la mano) con elmo e corazza di ferro addosso, in
# 0,5 -60 0,5 rivolto a sud. Dopo ogni passo uno scatto (prima persona, interfaccia visibile, salvo dove detto):
#
#   01-piazzato              oggetto telaio in mano, clic destro sull'erba due blocchi avanti: il telaio vuoto in
#                            0,5 -60 2,5, rivolto verso il giocatore; la mano e' vuota
#   02-elmo                  elmo Rottame in mano, clic destro sul telaio
#   03-braccio-destro        braccio Rottame in mano, clic destro sul lato destro del telaio (il suo, non quello di chi
#                            guarda: a sinistra nello scatto)
#   04-armato                cella carica in mano, clic destro: il telaio armato
#   05-prima-di-entrare      terza persona da dietro, a mano vuota davanti al telaio
#   06-entrato-subito        subito dopo il clic destro a mano vuota (niente lampo alla misura vanilla, niente scatto
#   07-entrato-mezzo-secondo indietro), poi dopo mezzo secondo
#   08-dentro-da-dietro      dentro la tuta, da dietro; 09-dentro-davanti: davanti
#   10-uscita-tasto-tenuto   V tenuto, a 0,6 s: l'avanzamento sulla barra delle azioni
#   11-uscito                dopo l'uscita, ancora davanti: il giocatore con la sua armatura dentro il telaio rimasto
#   12-telaio-dopo-uscita    da dietro, quattro blocchi indietro: il giocatore e il telaio rimasto
#   13-rientrato-spostato    rientrato a mano vuota e portato con un tp in 3,5 -60 2,5
#   14-telaio-dopo-morte     dopo /kill e la rinascita: il telaio rimasto dove il giocatore e' morto
#   15-armatura-ritrovata    rientrato nel telaio lasciato dalla morte e riuscito con V: l'inventario aperto mostra
#                            elmo e corazza di ferro, restituiti dal telaio
#
# Fra un passo e l'altro i comandi del server controllano lo stato (execute if/unless: un controllo che non torna ferma
# l'esecuzione): dove sta l'entita' telaio e quante ce ne sono, i segnaposto negli slot armatura solo mentre il
# giocatore e' dentro, l'armatura di ferro restituita all'uscita, niente a terra dopo la morte; "data get entity"
# sull'entita' telaio scrive in rcon.log il suo stato. Alla fine la telecamera torna in prima persona e
# client_shot.sh fa i suoi tre scatti (back, front, quarter) con il giocatore otto blocchi davanti al telaio. Dura circa due minuti e mezzo a cache calde. Le
# variabili d'ambiente sono quelle di client_shot.sh (EXO_ACCEPT_EULA obbligatoria). Istruzioni in
# tools/client/README.md.
set -u

here=$(cd "$(dirname "$0")" && pwd)

if [ "$#" -ne 2 ]; then
    echo "Uso: EXO_ACCEPT_EULA=true bash tools/client/m3b_shots.sh <variante> <cartella>" >&2
    exit 2
fi
variant=$1
out=$2
u=${EXO_SHOT_USER:-Exo}

# La cella carica: dati dello stack come NBT su 1.20.1, nel componente custom_data su 1.21.1. Una variante sconosciuta
# la rifiuta client_shot.sh.
case "$variant" in
    1.20.1-*) cell='redstone_exoframes:redstone_cell{exoframes:{energy:10000}}' ;;
    *) cell='redstone_exoframes:redstone_cell[minecraft:custom_data={exoframes:{energy:10000}}]' ;;
esac

frame='@e[type=redstone_exoframes:exoframe]'
hand="item replace entity $u weapon.mainhand with"
view="tp $u 0.5 -60 -1.5 0 10" # quattro blocchi davanti al telaio, rivolto verso di lui
frame_at() { echo "execute if entity @e[type=redstone_exoframes:exoframe,x=$1,y=-60,z=$2,distance=..0.01]"; }
slot_has() { echo "execute if data entity $u Inventory[{Slot:$1b,id:\"$2\"}]"; }
has() { echo "execute if data entity $u Inventory[{id:\"$1\"}]"; }
lacks() { echo "execute unless data entity $u Inventory[{id:\"$1\"}]"; }
one_frame=("execute store result score #frames exo_shot if entity $frame" "execute if score #frames exo_shot matches 1")
dressed=("$(slot_has 103 redstone_exoframes:exoframe_helmet)" "$(slot_has 102 redstone_exoframes:exoframe_torso)"
         "$(slot_has 101 redstone_exoframes:exoframe_legs)" "$(slot_has 100 redstone_exoframes:exoframe_boots)")
undressed=("$(lacks redstone_exoframes:exoframe_helmet)" "$(lacks redstone_exoframes:exoframe_torso)"
           "$(lacks redstone_exoframes:exoframe_legs)" "$(lacks redstone_exoframes:exoframe_boots)")
empty_hand="execute unless data entity $u Inventory[{Slot:0b}]"

steps=(
    "gamemode survival $u" "gamerule doImmediateRespawn true" "gamerule spawnRadius 0"
    "item replace entity $u armor.head with minecraft:iron_helmet"
    "item replace entity $u armor.chest with minecraft:iron_chestplate"
    "@attendi 3"
    # 1. piazzare con l'oggetto: il mirino sull'erba del blocco 0 -61 2
    "$hand redstone_exoframes:exoframe" "tp $u 0.5 -60 0.5 0 39" "@attendi 1" "@click destro" "@attendi 1"
    "$(frame_at 0.5 2.5)" "${one_frame[@]}" "$empty_hand"
    "$view" "@attendi 2" "@scatto 01-piazzato"
    # 2. elmo, mirino al centro del telaio
    "$hand redstone_exoframes:scrap_helmet" "tp $u 0.5 -60 0.5 0 0" "@attendi 1" "@click destro" "@attendi 1"
    "$empty_hand" "$view" "@attendi 2" "@scatto 02-elmo"
    # 3. braccio, mirino 0,33 verso +X: con il telaio rivolto a nord e' il suo lato destro, all'altezza delle braccia
    "$hand redstone_exoframes:scrap_arm" "tp $u 0.83 -60 0.5 0 0" "@attendi 1" "@click destro" "@attendi 1"
    "$empty_hand" "$view" "@attendi 2" "@scatto 03-braccio-destro"
    # 4. cella carica
    "$hand $cell" "tp $u 0.5 -60 0.5 0 0" "@attendi 1" "@click destro" "@attendi 1"
    "$empty_hand" "$view" "@attendi 2" "@scatto 04-armato"
    # 5. entrare a mano vuota, in terza persona da dietro
    "tp $u 0.5 -60 1.2 0 0" "@tieni F5 0.2" "@attendi 2" "@scatto 05-prima-di-entrare"
    "@click destro" "@scatto 06-entrato-subito" "@attendi 0.5" "@scatto 07-entrato-mezzo-secondo"
    "@attendi 2" "@scatto 08-dentro-da-dietro"
    "${dressed[@]}" "execute unless entity $frame" "$(lacks minecraft:iron_helmet)" "$(lacks minecraft:iron_chestplate)"
    "@tieni F5 0.2" "@attendi 2" "@scatto 09-dentro-davanti"
    # 6. uscire tenendo V
    "@tieni v 2 10-uscita-tasto-tenuto 0.6" "@attendi 1" "@scatto 11-uscito"
    "$(frame_at 0.5 2.5)" "${one_frame[@]}" "${undressed[@]}"
    "$(slot_has 103 minecraft:iron_helmet)" "$(slot_has 102 minecraft:iron_chestplate)"
    "data get entity @e[type=redstone_exoframes:exoframe,limit=1]"
    "$view" "@tieni F5 0.2" "@tieni F5 0.2" "@attendi 2" "@scatto 12-telaio-dopo-uscita"
    # 7. rientrare, spostarsi dentro la tuta e morire
    "tp $u 0.5 -60 1.2 0 0" "@attendi 1" "@click destro" "@attendi 1"
    "${dressed[@]}" "execute unless entity $frame"
    "tp $u 3.5 -60 2.5 180 0" "@attendi 2" "@scatto 13-rientrato-spostato"
    "kill $u" "@attendi 3"
    "$(frame_at 3.5 2.5)" "${one_frame[@]}" "execute unless entity @e[type=item]"
    "execute unless data entity $u Inventory[0]"
    "data get entity @e[type=redstone_exoframes:exoframe,limit=1]"
    "tp $u 3.5 -60 -1.5 0 10" "@attendi 3" "@scatto 14-telaio-dopo-morte"
    # 8. il proprietario rientra: l'armatura rimasta nel telaio gli torna; esce con V
    "tp $u 3.5 -60 1.2 0 0" "@attendi 1" "@click destro" "@attendi 1"
    "${dressed[@]}" "$(has minecraft:iron_helmet)" "$(has minecraft:iron_chestplate)"
    "@tieni v 2" "@attendi 1"
    "$(frame_at 3.5 2.5)" "${one_frame[@]}" "${undressed[@]}"
    "$(has minecraft:iron_helmet)" "$(has minecraft:iron_chestplate)"
    "data get entity @e[type=redstone_exoframes:exoframe,limit=1]"
    "tp $u 3.5 -60 -1.5 0 10" "@attendi 2" "@tieni e 0.2" "@attendi 1" "@scatto 15-armatura-ritrovata"
    "@tieni Escape 0.2"
    # telecamera di nuovo in prima persona (da dietro -> davanti -> prima) e giocatore otto blocchi davanti al telaio,
    # per i tre scatti finali: la telecamera davanti non finisce dentro il telaio, che si vede dietro il giocatore
    # in back.png
    "@tieni F5 0.2" "@tieni F5 0.2" "tp $u 3.5 -60 -5.5 0 15"
)

echo "=== $variant m3b"
bash "$here/client_shot.sh" "$variant" "$out" "${steps[@]}"
code=$?
if [ "$code" -eq 0 ] && [ -s "$out/log-check.txt" ]; then
    echo "OK $variant, ma con righe da guardare in $out/log-check.txt"
elif [ "$code" -eq 0 ]; then
    echo "OK $variant: scatti in $out"
else
    echo "FAIL $variant: client_shot.sh e' uscito con $code" >&2
fi
exit "$code"
