#!/usr/bin/env bash
# Insieme standard di scatti della parte 3a ("vedere il telaio"), una esecuzione di client_shot.sh per scatto, ognuna
# nella propria sottocartella:
#
#   EXO_ACCEPT_EULA=true bash tools/client/m3a_shots.sh <variante> <cartella>
#
#   01-nudo           telaio nudo, con una colonna di tre blocchi di lana bianca a fianco come metro (a ovest, dal
#                     lato opposto a quello dove va la telecamera nello scatto a tre quarti)
#   02-rottame-s0     sei pezzi Rottame, stato 0 (integro), colori predefiniti, con la stessa colonna
#   03-rottame-s1     stato 1 (danneggiato)
#   04-rottame-s2     stato 2 (critico)
#   05-rottame-s3     stato 3 (rotto)
#   06-colori         stato 0 verniciato con i colori di prova 3A6EA5 (primario) e D9D9D9 (secondario)
#   07-mani           Rottame stato 0 con un piccone di ferro nella mano principale e uno scudo nell'altra
#   08-sdraiato       telaio nudo da sdraiati: un blocco di vetro sopra la testa lascia un solo blocco libero e il
#                     gioco forza la posa strisciante; qui front.png non serve (la telecamera davanti finisce
#                     contro la testa), contano back.png e quarter.png
#
# Una esecuzione per scatto e non una lunga con piu' scatti: ogni scatto parte dallo stesso stato (mondo nuovo,
# corpo e testa allineati, telecamera al primo F5) e un errore resta confinato al suo scatto. Costa circa un minuto
# per scatto. Alla fine monta stati-fronte.png (front.png degli stati 0-3 affiancati) e stampa un riepilogo; esce con 1
# se uno scatto non e' riuscito, dopo aver provato tutti gli altri. Le variabili d'ambiente sono quelle di
# client_shot.sh (EXO_ACCEPT_EULA obbligatoria). Istruzioni in tools/client/README.md.
set -u

here=$(cd "$(dirname "$0")" && pwd)

if [ "$#" -ne 2 ]; then
    echo "Uso: EXO_ACCEPT_EULA=true bash tools/client/m3a_shots.sh <variante> <cartella>" >&2
    exit 2
fi
variant=$1
out=$2
u=${EXO_SHOT_USER:-Exo}

wear="execute as $u run exoframe debug wear"
ruler="execute as $u at @s run fill ~-3 ~ ~ ~-3 ~2 ~ minecraft:white_wool"

names=()
commands=()
add() { names+=("$1"); commands+=("$2"); }
# I comandi di uno scatto sono separati da "|".
add 01-nudo "$wear nudo|$ruler"
add 02-rottame-s0 "$wear rottame 0|$ruler"
add 03-rottame-s1 "$wear rottame 1"
add 04-rottame-s2 "$wear rottame 2"
add 05-rottame-s3 "$wear rottame 3"
add 06-colori "$wear rottame 0 3A6EA5 D9D9D9"
add 07-mani "$wear rottame 0|item replace entity $u weapon.mainhand with minecraft:iron_pickaxe|item replace entity $u weapon.offhand with minecraft:shield"
add 08-sdraiato "$wear nudo|execute as $u at @s run setblock ~ ~1 ~ minecraft:glass"

# La cartella la crea client_shot.sh, dopo i suoi controlli: con l'EULA non accettata o una variante sbagliata non
# resta una cartella vuota.
t0=$(date +%s)
summary=()
fail=0
for i in "${!names[@]}"; do
    name=${names[$i]}
    IFS='|' read -r -a cmds <<<"${commands[$i]}"
    echo "=== $variant $name"
    s0=$(date +%s)
    bash "$here/client_shot.sh" "$variant" "$out/$name" "${cmds[@]}"
    code=$?
    secs=$(($(date +%s) - s0))
    if [ "$code" -eq 130 ] || [ "$code" -eq 143 ]; then
        echo "interrotto durante $name: gli scatti successivi non si fanno" >&2
        exit "$code"
    elif [ "$code" -eq 2 ]; then
        # Uso o ambiente sbagliati (EULA, programmi, variante, porta): uguale per tutti gli scatti, inutile insistere.
        echo "FAIL $variant $name: client_shot.sh e' uscito con 2 (vedi il messaggio sopra)" >&2
        exit 2
    elif [ "$code" -ne 0 ]; then
        summary+=("FAIL $name (${secs} s, uscita $code)")
        fail=1
    elif [ -s "$out/$name/log-check.txt" ]; then
        summary+=("OK   $name (${secs} s), ma con righe da guardare in $out/$name/log-check.txt")
    else
        summary+=("OK   $name (${secs} s)")
    fi
done

states=("$out"/02-rottame-s0/front.png "$out"/03-rottame-s1/front.png "$out"/04-rottame-s2/front.png
        "$out"/05-rottame-s3/front.png)
if [ -f "${states[0]}" ] && [ -f "${states[1]}" ] && [ -f "${states[2]}" ] && [ -f "${states[3]}" ]; then
    convert "${states[@]}" +append "$out/stati-fronte.png" && echo "montati gli stati 0-3 in $out/stati-fronte.png"
fi

echo "=== riepilogo $variant ($(($(date +%s) - t0)) s in tutto)"
printf '%s\n' "${summary[@]}"
exit "$fail"
