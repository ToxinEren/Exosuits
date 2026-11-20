#!/usr/bin/env bash
# La sequenza della parte 4a-1 ("il banco") nel client vero: un'esecuzione di client_shot.sh con i passi qui sotto, e
# poi i controlli che lo script fa da se' sugli scatti con arm_measure.sh.
#
#   EXO_ACCEPT_EULA=true bash tools/client/m4a1_shots.sh <variante> <cartella>
#
# Il giocatore resta in creativa in 0,5 -60 0,5 rivolto a sud; finestra 960x540 e scala dell'interfaccia 2, quindi
# interfaccia 480x270 e schermata della stazione (316x232) con l'angolo nel punto 82,19 dell'interfaccia, cioe' il
# riquadro 632x464+164+38 negli scatti. Un punto (x, y) della schermata e' il punto (2 (82 + x), 2 (19 + y)) degli
# scatti. La stazione sta in 0 -60 1 (la meta' principale) e -1 -60 1 (la laterale, alla destra di chi la posa
# guardando a sud), con il fronte a nord, verso il giocatore; dal passo 5 al passo 9 il telaio sta sopra la principale,
# con i piedi in 0,5 -59 1,5.
#
#   1. attesa       "@attendi 12": la riga in chat del "gamemode creative" di client_shot.sh e l'avviso "Chat
#                   messages can't be verified" (460x110+500+0, sopra il ritaglio dell'anteprima) spariscono prima
#                   del primo scatto misurato (vedi tools/client/README.md, regole degli scatti misurati)
#   2. ricetta      un lingotto di rame nell'inventario sblocca la ricetta della stazione, come le altre della mod:
#                   "recipe take" riesce solo se il giocatore la conosce; poi "@attendi 12", perche' l'avviso delle
#                   ricette sbloccate sparisca prima del primo scatto misurato
#   3. posa         con la stazione in mano e il mirino sul pavimento davanti (inclinazione 55) un clic destro la posa
#                   con l'oggetto: "execute if block" vuole la principale in 0 -60 1 e la laterale in -1 -60 1, con
#                   il fronte a nord; la mano torna vuota, e dopo un secondo, quando l'oggetto che scende non copre piu'
#                   la laterale, scatto stazione-posata, da guardare: le due meta' affiancate e scoperte, con le
#                   texture segnaposto e i fronti verso il giocatore (l'estremo ovest della laterale esce dal bordo
#                   destro dello scatto), nessun blocco viola e nero (un modello o una texture mancante)
#   4. senza telaio con l'inclinazione 60 il mirino sta sul fianco nord della principale: un clic destro, senza telai
#                   a portata, non apre nulla; scatto nessun-telaio mezzo secondo dopo, da guardare: nessuna
#                   schermata e, sulla barra delle azioni, "No exoframe nearby: place one on the station or within 2
#                   blocks"
#   5. telaio       in piedi sopra la principale il giocatore indossa la tuta Rottame dipinta di magenta (/exoframe
#                   debug wear: sei pezzi e una cella carica) e ne esce tenendo V: il telaio resta piazzato dove
#                   stavano i piedi, sopra la principale ("execute if entity" lo vuole in 0,5 -59 1,5); poi il
#                   giocatore torna in 0,5 -60 0,5 con l'inclinazione 60
#   6. apertura     il mirino sul fianco nord della principale, sotto il telaio: un clic destro la apre sul telaio, con
#                   i dati extra; la schermata si apre solo se la stazione trova il telaio, se il tipo del menu porta i
#                   dati e se la schermata e' registrata (su NeoForge un errore della registrazione sarebbe
#                   silenzioso); il puntatore nel punto neutro 10,10, fuori dalla schermata; scatto stazione-aperta, da
#                   guardare: la schermata scura al centro e, nel riquadro di destra, la tuta fissa di fronte, in
#                   piedi, con il braccio destro magenta sul lato sinistro dello schermo, tagliata dal ritaglio se ne
#                   esce (la schermata e' opaca: il telaio dipinto dietro, nel mondo, si vede solo attorno)
#   7. pulsante     clic sinistro al centro del pulsante di prova (punto 748,426: il pulsante e' 283..301 x 186..202
#                   nella schermata, 36x32+730+410 negli scatti): il numero 102 va al server con il pacchetto dei
#                   pulsanti del gioco base, il server conta la pressione e il gioco base rimanda il contatore; il
#                   puntatore di nuovo nel punto neutro; scatto premuto, da guardare: il pulsante in basso a destra
#                   verde con 1
#   8. vista        prova di fumo del pacchetto station_view: con lo sguardo sul telaio (inclinazione -40; la schermata
#                   resta aperta) /exoframe unlock lo sblocca, la vista del telaio cambia e al tick dopo il server la
#                   manda al client, che la mette nel menu aperto; dopo 2 s il giocatore e' ancora nel server (un
#                   pacchetto che il client non legge lo farebbe uscire, e il passo dopo fallirebbe) e nessuna riga del
#                   mod finisce in log-check.txt. Nella 4a-1 la vista nuova non cambia nulla che si veda (il lucchetto
#                   e' della 4a-2): il contenuto lo provano i JUnit del codec e i test in mondo di StationMenuGameTests
#   9. chiusura     con lo sguardo sul telaio (inclinazione -40; la schermata resta aperta) /exoframe remove lo toglie,
#                   e il server chiude la schermata al tick dopo; scatto telaio-tolto mezzo secondo dopo, da guardare:
#                   nessuna schermata e, sulla barra delle azioni, "The exoframe is no longer available at the station";
#                   "execute unless entity" vuole nessun telaio rimasto, e kill toglie i pezzi e la cella che il
#                   comando lascia a terra (prima dei 2 s del ritardo di raccolta)
#  10. rottura      con il mirino sul fianco nord della laterale (rotazione 63,4, inclinazione 45) un clic sinistro,
#                   in creativa, rompe la laterale: la principale sparisce con lei, senza bottino ("execute if block"
#                   vuole l'aria nelle due meta', "execute unless entity" nessun oggetto a terra)
#  11. fine         tp in 0,5 -60 -5,5 per i tre scatti di client_shot.sh
#
# Controlli sugli scatti (arm_measure.sh), solo dentro il riquadro della schermata o di una sua parte: attorno si vede
# il mondo. "Dentro R" vuol dire un riquadro con X >= Rx, Y >= Ry, X + L <= Rx + RL e Y + A <= Ry + RA.
#   anteprima       magenta di stazione-aperta nel ritaglio dell'anteprima (184x232+596+74) almeno MAGENTA_MIN, e
#                   uguale al magenta di tutta la schermata: la schermata si e' aperta, l'anteprima disegna la tuta
#                   con SuitDraw e nella schermata il magenta sta solo nel ritaglio
#   orientamento    magenta di stazione-aperta nella meta' destra del ritaglio (92x232+688+74) zero: la tuta fissa ha
#                   magenta il solo braccio destro, che di fronte sta a sinistra (di spalle starebbe a destra; a testa
#                   in giu' resterebbe a sinistra, e lo dice lo scatto da guardare)
#   pulsante        sagoma fra premuto e stazione-aperta: almeno BUTTON_MIN pixel diversi, tutti dentro il pulsante
#                   (36x32+730+410): il giro del pulsante e' tornato, e non e' cambiato altro
# Le regole degli scatti misurati: il primo almeno 12 s dopo il primo passo; nessun oggetto che sblocca ricette nei 12 s
# prima di uno scatto misurato (la sequenza non ne da'); il puntatore nel punto neutro in tutti e due gli scatti della
# coppia del pulsante (sopra il pulsante lo schiarisce e ne mostra il suggerimento).
#
# Non prova: il jar di produzione (il client gira nell'ambiente di sviluppo); la rottura in sopravvivenza, con e senza
# piccone, i pistoni e i comandi che tolgono una meta' (li provano i test in mondo di StationBlockGameTests); la scelta
# fra piu' telai, i telai bloccati, la portata misurata al bordo e le altre ragioni che chiudono la schermata (le
# provano i test in mondo di StationMenuGameTests); il contenuto della vista del telaio e un suo cambio che si veda (la
# schermata non la disegna ancora: il contenuto lo provano i JUnit del codec e i test in mondo di StationMenuGameTests,
# il cambio visibile arriva con il lucchetto della 4a-2), l'anteprima girevole e il pannello del telaio, che arrivano
# con i task dopo; la schermata con altre scale dell'interfaccia e altre misure della finestra; una
# ricarica delle risorse (F3+T) con la schermata aperta; un'esplosione vera. Sono nella checklist per il PC della parte.
#
# Dura poco piu' di due minuti a cache calde: misurati 135 s su 1.21.1-neoforge e 134 s su 1.20.1-forge, di cui 130 s
# l'esecuzione di client_shot.sh (prima della fase della vista 133 s su 1.21.1-neoforge, di cui 128 s client_shot.sh;
# prima delle fasi del telaio 126 s, di cui 121 s client_shot.sh; senza la ricetta, la posa con l'oggetto e la rottura
# la sequenza misurava 88-107 s sulle quattro varianti, di cui 83-102 s client_shot.sh). Le misure sono uguali al
# pixel sulle quattro varianti (i valori accanto alle soglie, piu' sotto; le stesse nelle esecuzioni con le fasi nuove
# su 1.21.1-neoforge e 1.20.1-forge, anche con il telaio dipinto dietro la schermata), e cosi' il
# ritaglio dell'anteprima: la scala della tuta nell'interfaccia passa da GuiEntityScale, che la applica come il gioco
# base di ogni versione (senza, su 1.20.1 la tuta usciva piu' scura, magenta 999).
#
# Uscita: una riga "ok <nome>: <misure>" o "KO <nome>: <misure> (atteso ...)" per controllo, tutti valutati anche dopo
# un fallimento; l'ultima riga e' "OK <variante>: scatti in <cartella>" (uscita 0) se client_shot.sh e' uscito con 0 e
# ogni controllo e' ok, altrimenti "FAIL <variante>: ..." su stderr (uscita 1). I controlli girano solo se ci sono
# tutti gli scatti. Con M4A1_SOLO_CONTROLLI=1 lo script salta l'esecuzione e rifa' i controlli sugli scatti che la
# cartella ha gia' (per fissare le soglie senza riavviare il client). Uscita 2: uso sbagliato. Le variabili d'ambiente
# sono quelle di client_shot.sh (EXO_ACCEPT_EULA obbligatoria). Istruzioni in tools/client/README.md.
set -u
# Soglie valide solo con la config predefinita: una EXO_SHOT_SERVER_CONFIG ereditata dall'ambiente le falserebbe.
unset EXO_SHOT_SERVER_CONFIG

here=$(cd "$(dirname "$0")" && pwd)
measure="$here/arm_measure.sh"

if [ "$#" -ne 2 ]; then
    echo "Uso: EXO_ACCEPT_EULA=true bash tools/client/m4a1_shots.sh <variante> <cartella>" >&2
    exit 2
fi
variant=$1
out=$2
u=${EXO_SHOT_USER:-Exo}

# --- soglie ---------------------------------------------------------------------------------------------------------
# Ciascuna con la ragione e, fra parentesi, il valore misurato (uguale sulle quattro varianti).

# Il magenta della tuta fissa nel ritaglio dell'anteprima: la meta', arrotondata per difetto alla decina, del magenta
# di stazione-aperta (misurato 1193 sulle quattro varianti, nel riquadro 41x85+604+152: il braccio destro, nella
# meta' sinistra del ritaglio).
MAGENTA_MIN=590
# I pixel del pulsante di prova che cambiano fra prima e dopo la pressione: la meta', arrotondata per difetto alla
# decina, della sagoma fra premuto e stazione-aperta (misurati 1112, nel riquadro 36x32+730+410, il pulsante intero).
BUTTON_MIN=550
# Riquadri negli scatti: la schermata, il ritaglio dell'anteprima, la sua meta' destra, il pulsante di prova.
SCREEN_RECT=632x464+164+38
CLIP_RECT=184x232+596+74
CLIP_RIGHT_RECT=92x232+688+74
BUTTON_RECT=36x32+730+410
# Il primo passo: vedi il commento di testa.
FIRST_WAIT=12

# --- passi ----------------------------------------------------------------------------------------------------------
NEUTRAL="@punta 10 10" # il puntatore fuori dalla schermata

steps=(
    # 1. attesa
    "@attendi $FIRST_WAIT"
    # 2. ricetta: il lingotto di rame sblocca la ricetta della stazione, e solo allora "recipe take" riesce
    "give $u minecraft:copper_ingot" "@attendi 2" "recipe take $u redstone_exoframes:station" "@attendi 12"
    # 3. posa con l'oggetto: il mirino sul pavimento davanti; la principale li', la laterale a destra, fronte a nord
    "item replace entity $u weapon.mainhand with redstone_exoframes:station" "tp $u 0.5 -60 0.5 0 55" "@attendi 1"
    "@click destro" "@attendi 1"
    "execute if block 0 -60 1 redstone_exoframes:station[facing=north,part=main]"
    "execute if block -1 -60 1 redstone_exoframes:station[facing=north,part=side]"
    "item replace entity $u weapon.mainhand with minecraft:air" "@attendi 1" "@scatto stazione-posata"
    # 4. senza telaio: il clic non apre nulla e lo dice sulla barra delle azioni
    "tp $u 0.5 -60 0.5 0 60" "@attendi 1" "@click destro" "@attendi 0.5" "@scatto nessun-telaio"
    # 5. telaio: la tuta indossata sopra la principale e lasciata li' con V
    "tp $u 0.5 -59 1.5 0 15" "execute as $u run exoframe debug wear rottame 0 FF00FF FF00FF" "@attendi 2"
    "@tieni v 2" "@attendi 1"
    "execute if entity @e[type=redstone_exoframes:exoframe,x=0.5,y=-59,z=1.5,distance=..0.2]"
    "tp $u 0.5 -60 0.5 0 60" "@attendi 1"
    # 6. apertura: il mirino sul fianco nord della principale, sotto il telaio
    "tp $u 0.5 -60 0.5 0 60" "@attendi 1" "@click destro" "@attendi 2" "$NEUTRAL" "@attendi 1"
    "@scatto stazione-aperta"
    # 7. pulsante di prova, al suo centro
    "@clic 748 426" "@attendi 1" "$NEUTRAL" "@attendi 1" "@scatto premuto"
    # 8. vista: a schermata aperta /exoframe unlock cambia il telaio, e il server manda station_view (prova di fumo)
    "tp $u 0.5 -60 0.5 0 -40" "@attendi 1" "execute as $u run exoframe unlock" "@attendi 2"
    # 9. chiusura: tolto il telaio con lo sguardo su di lui, il server chiude la schermata e lo dice
    "tp $u 0.5 -60 0.5 0 -40" "@attendi 1" "execute as $u run exoframe remove" "@attendi 0.5"
    "@scatto telaio-tolto" "execute unless entity @e[type=redstone_exoframes:exoframe]"
    "kill @e[type=minecraft:item]"
    # 10. rottura: il mirino sul fianco nord della laterale; in creativa le due meta' spariscono senza bottino
    "tp $u 0.5 -60 0.5 63.4 45" "@attendi 1" "@click sinistro" "@attendi 1"
    "execute if block 0 -60 1 minecraft:air" "execute if block -1 -60 1 minecraft:air"
    "execute unless entity @e[type=minecraft:item]"
    # 11. fine, all'aperto per i tre scatti di client_shot.sh
    "tp $u 0.5 -60 -5.5 0 15"
)
shots=(stazione-posata nessun-telaio stazione-aperta premuto telaio-tolto)

# --- l'esecuzione ---------------------------------------------------------------------------------------------------
problems=() # le ragioni di un FAIL, nell'ordine in cui si incontrano
if [ "${M4A1_SOLO_CONTROLLI:-}" != 1 ]; then
    echo "=== $variant m4a1"
    # un'uscita anticipata di client_shot.sh (uso o ambiente sbagliati) non lascia leggere gli avvisi o gli scatti di
    # un'altra esecuzione
    rm -f "$out/log-check.txt"
    for s in "${shots[@]}"; do rm -f "$out/$s.png"; done
    bash "$here/client_shot.sh" "$variant" "$out" "${steps[@]}"
    code=$?
    if [ "$code" -ne 0 ]; then
        if [ -s "$out/log-check.txt" ]; then
            problems+=("avvisi del mod fuori elenco in $out/log-check.txt")
        else
            problems+=("client_shot.sh e' uscito con $code")
        fi
    fi
fi

# --- controlli sugli scatti -----------------------------------------------------------------------------------------
missing=()
for s in "${shots[@]}"; do [ -f "$out/$s.png" ] || missing+=("$s"); done

# Gli aiuti delle misure (take_measure, val, box, inside, verdict) e l'elenco failed: tools/client/measure_lib.sh.
source "$here/measure_lib.sh" || exit 2

checks() {
    local m1 m2 good
    local open="$out/stazione-aperta.png" pressed="$out/premuto.png"

    m1=$(take_measure magenta "$open" "$CLIP_RECT")
    m2=$(take_measure magenta "$open" "$SCREEN_RECT")
    good=1
    [ "$(val "$m1" magenta)" -ge "$MAGENTA_MIN" ] && [ "$(val "$m2" magenta)" -eq "$(val "$m1" magenta)" ] && good=0
    verdict anteprima "$good" "ritaglio $m1; schermata $m2" \
        "magenta >= $MAGENTA_MIN nel ritaglio, e lo stesso in tutta la schermata: la tuta solo nel ritaglio"

    m1=$(take_measure magenta "$open" "$CLIP_RIGHT_RECT")
    good=1
    [ "$(val "$m1" magenta)" -eq 0 ] && good=0
    verdict orientamento "$good" "meta' destra del ritaglio $m1" \
        "magenta=0: la tuta di fronte, con il braccio destro a sinistra dello schermo"

    m1=$(take_measure sagoma "$pressed" "$open")
    box "$m1"
    good=1
    [ "$(val "$m1" pixel)" -ge "$BUTTON_MIN" ] && inside "$BUTTON_RECT" && good=0
    verdict pulsante "$good" "$m1" \
        "pixel >= $BUTTON_MIN, riquadro dentro $BUTTON_RECT: il pulsante e' cambiato, e nient'altro"
}

if [ "${#missing[@]}" -eq 0 ]; then
    checks
    [ "${#failed[@]}" -eq 0 ] || problems+=("controlli falliti: ${failed[*]}")
elif [ "${#problems[@]}" -eq 0 ]; then
    problems+=("mancano gli scatti: ${missing[*]}")
fi

if [ "${#problems[@]}" -eq 0 ]; then
    echo "OK $variant: scatti in $out"
    exit 0
fi
reason=$(printf '%s; ' "${problems[@]}")
echo "FAIL $variant: ${reason%; }" >&2
exit 1
