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
# guardando a sud), con il fronte a nord, verso il giocatore; dal passo 5 al passo 12 il telaio sta sopra la
# principale, con i piedi in 0,5 -59 1,5.
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
#                   guardare: la schermata scura al centro; a sinistra il pannello del telaio, i sei pezzi con il bordo
#                   verde (integri), l'elmo, la scelta dell'apertura, con il secondo bordo bianco, e la cella con il
#                   bordo rosso; al centro "Scrap Helmet", "Intact" in verde, "180 / 180" e la barra piena; a destra
#                   la tuta dipinta di magenta, di fronte, in piedi, tagliata dal ritaglio se ne esce; sotto
#                   l'inventario (la schermata e' opaca: il telaio dipinto dietro, nel mondo, si vede solo attorno)
#   7. pulsante     clic sinistro al centro del pulsante di prova (punto 748,426: il pulsante e' 283..301 x 186..202
#                   nella schermata, 36x32+730+410 negli scatti): il numero 102 va al server con il pacchetto dei
#                   pulsanti del gioco base, il server conta la pressione e il gioco base rimanda il contatore; il
#                   puntatore di nuovo nel punto neutro; scatto premuto, da guardare: il pulsante in basso a destra
#                   verde con 1
#   8. rotazione    trascinando con il tasto sinistro nel ritaglio dell'anteprima da 620,250 a 740,250 (60 pixel
#                   dell'interfaccia, 1,5 gradi per pixel) la tuta gira di 90 gradi; l'angolo segue la distanza dal
#                   punto premuto, quindi non conta il salto del puntatore da 10,10 a 620,250 che su 1.21.1, a pochi
#                   fotogrammi al secondo, arriva alla schermata nel primo trascinamento; il puntatore di nuovo nel
#                   punto neutro; scatto girata, da guardare: la tuta di fianco, nient'altro cambiato
#   9. suggerimento il puntatore sul posto del torso nel pannello del telaio (punto 244,144); scatto
#                   suggerimento-torso, da guardare: il suggerimento con le quattro righe del torso, "Torso", "Scrap
#                   Torso", "Durability 180 / 180" e "Intact" in verde; poi il puntatore di nuovo nel punto neutro
#  10. inventario   tre lingotti di ferro nel primo slot dell'inventario principale (inventory.0, lo slot 9
#                   dell'inventario) e un clic sinistro con le maiuscole al suo centro (punto 196,354): lo spostamento
#                   rapido del menu li porta nel primo slot della barra; "execute if data" li vuole nello slot 0 e
#                   "execute unless data" vuole vuoto lo slot 9. Il lingotto sblocca ricette, ma dopo l'ultimo scatto
#                   misurato; da qui il giocatore li tiene in mano, e la rottura in creativa resta la stessa
#  11. vista        prova di fumo del pacchetto station_view: con lo sguardo sul telaio (inclinazione -40; la schermata
#                   resta aperta) /exoframe unlock lo sblocca, la vista del telaio cambia e al tick dopo il server la
#                   manda al client, che la mette nel menu aperto; dopo 2 s il giocatore e' ancora nel server (un
#                   pacchetto che il client non legge lo farebbe uscire, e il passo dopo fallirebbe) e nessuna riga del
#                   mod finisce in log-check.txt. Nella 4a-1 la vista nuova non cambia nulla che si veda (il lucchetto
#                   e' della 4a-2): il contenuto lo provano i JUnit del codec e i test in mondo di StationMenuGameTests
#  12. chiusura     con lo sguardo sul telaio (inclinazione -40; la schermata resta aperta) /exoframe remove lo toglie,
#                   e il server chiude la schermata al tick dopo; scatto telaio-tolto mezzo secondo dopo, da guardare:
#                   nessuna schermata e, sulla barra delle azioni, "The exoframe is no longer available at the station";
#                   "execute unless entity" vuole nessun telaio rimasto, e kill toglie i pezzi e la cella che il
#                   comando lascia a terra (prima dei 2 s del ritardo di raccolta)
#  13. rottura      con il mirino sul fianco nord della laterale (rotazione 63,4, inclinazione 45) un clic sinistro,
#                   in creativa, rompe la laterale: la principale sparisce con lei, senza bottino ("execute if block"
#                   vuole l'aria nelle due meta', "execute unless entity" nessun oggetto a terra)
#  14. fine         tp in 0,5 -60 -5,5 per i tre scatti di client_shot.sh
#
# Controlli sugli scatti (arm_measure.sh), solo dentro il riquadro della schermata o di una sua parte: attorno si vede
# il mondo. "Dentro R" vuol dire un riquadro con X >= Rx, Y >= Ry, X + L <= Rx + RL e Y + A <= Ry + RA.
#   anteprima       magenta di stazione-aperta nel ritaglio dell'anteprima (184x232+596+74) almeno MAGENTA_MIN, e
#                   uguale al magenta di tutta la schermata: la schermata si e' aperta, l'anteprima disegna con SuitDraw
#                   la tuta della vista del telaio (sei pezzi magenta) e nella schermata il magenta sta solo nel
#                   ritaglio (nessun colore della schermata e' magenta: lo controlla StationLayoutTest)
#   pulsante        sagoma fra premuto e stazione-aperta: almeno BUTTON_MIN pixel diversi, tutti dentro il pulsante
#                   (36x32+730+410): il giro del pulsante e' tornato, e non e' cambiato altro
#   rotazione       sagoma fra girata e premuto: almeno ROT_MIN pixel diversi, tutti dentro il ritaglio; il magenta di
#                   girata in tutta la schermata uguale a quello del ritaglio, non zero e in un riquadro piu' stretto
#                   di quello di stazione-aperta: il trascinamento ha girato la tuta di fianco (di fronte e di spalle
#                   e' larga uguale: misurati 167 e 173 pixel, di fianco 111), la tuta c'e' ancora e non e' cambiato
#                   altro (il verso dell'angolo 0 l'ha provato il Task 1 con la tuta fissa, e la trasformazione non e'
#                   cambiata)
# Le regole degli scatti misurati: il primo almeno 12 s dopo il primo passo; nessun oggetto che sblocca ricette nei 12 s
# prima di uno scatto misurato (la sequenza ne da' due: il lingotto di rame della fase 2, seguito da "@attendi 12" dopo
# "recipe take" e lontano dal primo scatto misurato, stazione-aperta, che viene dopo la posa e il telaio; i lingotti di
# ferro della fase 10, dopo l'ultimo); il puntatore nel punto neutro in tutti e due gli scatti di una coppia (sopra il
# pulsante lo schiarisce e ne mostra il suggerimento, sopra l'anteprima e il pannello del telaio mostra il loro).
#
# Non prova: il jar di produzione (il client gira nell'ambiente di sviluppo); la rottura in sopravvivenza, con e senza
# piccone, i pistoni e i comandi che tolgono una meta' (li provano i test in mondo di StationBlockGameTests); la scelta
# fra piu' telai, i telai bloccati, la portata misurata al bordo e le altre ragioni che chiudono la schermata (le
# provano i test in mondo di StationMenuGameTests); un cambio della vista del telaio che si veda (nella 4a-1 nessun
# comando cambia pezzi o cella di un telaio piazzato: il contenuto lo provano i JUnit del codec e i test in mondo di
# StationMenuGameTests, il cambio visibile arriva con il lucchetto della 4a-2); i colori e i testi del pannello del
# telaio e dei suggerimenti, che si guardano negli scatti e non si misurano; pezzi danneggiati, critici, rotti o
# mancanti (il comando di debug monta sei pezzi intatti); lo spostamento rapido nell'altro verso e con la barra piena
# (li prova il test in mondo quickMoveSwapsInventoryAndHotbar); la schermata con altre scale dell'interfaccia e altre
# misure della finestra; una ricarica delle risorse (F3+T) fra due aperture della schermata (con una schermata aperta
# il gioco non prende F3+T); un'esplosione vera; i suoni (il client non ha audio); un secondo giocatore; la morte e
# l'uscita dal server con la schermata aperta e una pila sul cursore. Sono nella checklist per il PC della parte,
# docs/test-in-gioco/m4a1-banco.md.
#
# Dura poco piu' di due minuti a cache calde: misurati 152 s su 1.20.1-forge, 132 s su 1.20.1-fabric, 135 s su
# 1.21.1-fabric e 137 s su 1.21.1-neoforge, di cui 144, 125, 128 e 130 s l'esecuzione di client_shot.sh. Dentro la
# schermata gli scatti delle quattro varianti sono uguali al pixel, anteprima di fronte compresa: la scala della tuta
# nell'interfaccia passa da GuiEntityScale, che la applica come il gioco base di ogni versione (senza, su 1.20.1 la
# tuta usciva piu' scura). Di fianco, in girata e suggerimento-torso, le due varianti di una versione restano uguali
# al pixel, mentre fra le due versioni la luce della tuta differisce di 1/255 al piu' (in 11906 pixel), e la sagoma
# della rotazione, che conta i pixel vicini alla soglia del 4 %, cambia di poco (i valori accanto alle soglie, piu'
# sotto). Il kill degli oggetti a terra della fase 12 deve arrivare prima che finiscano i 2 s del ritardo di raccolta
# (poi il giocatore, a un blocco, li raccoglierebbe e kill fallirebbe senza bersagli): sulle quattro varianti arriva
# nel secondo dopo "/exoframe remove" (orari del log di client_shot.sh) e toglie i sette oggetti.
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
# Ciascuna con la ragione e, fra parentesi, il valore misurato: lo stesso sulle quattro varianti dove non e' detto
# altro. La soglia e' la meta' del valore della prima variante misurata, 1.21.1-neoforge.

# Il magenta della tuta del telaio nel ritaglio dell'anteprima: la meta', arrotondata per difetto alla decina, del
# magenta di stazione-aperta (misurato 18582, nel riquadro 167x198+604+96: i sei pezzi dipinti, di fronte).
MAGENTA_MIN=9290
# I pixel del pulsante di prova che cambiano fra prima e dopo la pressione: la meta', arrotondata per difetto alla
# decina, della sagoma fra premuto e stazione-aperta (misurati 1112, nel riquadro 36x32+730+410, il pulsante intero).
BUTTON_MIN=550
# I pixel dell'anteprima che cambiano girando la tuta di 90 gradi: la meta', arrotondata per difetto alla decina, della
# sagoma fra girata e premuto (misurati 24332 sulle due varianti 1.21.1 e 24587 sulle due 1.20.1, nel riquadro
# 167x198+604+96; di fianco il magenta scende a 9225, nel riquadro 111x198+629+96).
ROT_MIN=12160
# Riquadri negli scatti: la schermata, il ritaglio dell'anteprima, il pulsante di prova.
SCREEN_RECT=632x464+164+38
CLIP_RECT=184x232+596+74
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
    # 8. rotazione: 60 pixel dell'interfaccia trascinati nel ritaglio dell'anteprima, 90 gradi
    "@trascina 620 250 740 250" "@attendi 0.5" "$NEUTRAL" "@attendi 1" "@scatto girata"
    # 9. suggerimento del posto del torso, da guardare
    "@punta 244 144" "@attendi 1" "@scatto suggerimento-torso" "$NEUTRAL"
    # 10. inventario: un clic con le maiuscole sposta i lingotti dal primo slot dell'inventario alla barra
    "item replace entity $u inventory.0 with minecraft:iron_ingot 3" "@attendi 1" "@clic 196 354 sinistro maiuscole"
    "@attendi 1" "execute if data entity $u Inventory[{Slot:0b,id:\"minecraft:iron_ingot\"}]"
    "execute unless data entity $u Inventory[{Slot:9b}]"
    # 11. vista: a schermata aperta /exoframe unlock cambia il telaio, e il server manda station_view (prova di fumo)
    "tp $u 0.5 -60 0.5 0 -40" "@attendi 1" "execute as $u run exoframe unlock" "@attendi 2"
    # 12. chiusura: tolto il telaio con lo sguardo su di lui, il server chiude la schermata e lo dice
    "tp $u 0.5 -60 0.5 0 -40" "@attendi 1" "execute as $u run exoframe remove" "@attendi 0.5"
    "@scatto telaio-tolto" "execute unless entity @e[type=redstone_exoframes:exoframe]"
    "kill @e[type=minecraft:item]"
    # 13. rottura: il mirino sul fianco nord della laterale; in creativa le due meta' spariscono senza bottino
    "tp $u 0.5 -60 0.5 63.4 45" "@attendi 1" "@click sinistro" "@attendi 1"
    "execute if block 0 -60 1 minecraft:air" "execute if block -1 -60 1 minecraft:air"
    "execute unless entity @e[type=minecraft:item]"
    # 14. fine, all'aperto per i tre scatti di client_shot.sh
    "tp $u 0.5 -60 -5.5 0 15"
)
shots=(stazione-posata nessun-telaio stazione-aperta premuto girata suggerimento-torso telaio-tolto)

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
    local m1 m2 m3 good front_width side_width
    local open="$out/stazione-aperta.png" pressed="$out/premuto.png" turned="$out/girata.png"

    m1=$(take_measure magenta "$open" "$CLIP_RECT")
    m2=$(take_measure magenta "$open" "$SCREEN_RECT")
    box "$m1"
    front_width=$BL
    good=1
    [ "$(val "$m1" magenta)" -ge "$MAGENTA_MIN" ] && [ "$(val "$m2" magenta)" -eq "$(val "$m1" magenta)" ] && good=0
    verdict anteprima "$good" "ritaglio $m1; schermata $m2" \
        "magenta >= $MAGENTA_MIN nel ritaglio, e lo stesso in tutta la schermata: la tuta solo nel ritaglio"

    m1=$(take_measure sagoma "$pressed" "$open")
    box "$m1"
    good=1
    [ "$(val "$m1" pixel)" -ge "$BUTTON_MIN" ] && inside "$BUTTON_RECT" && good=0
    verdict pulsante "$good" "$m1" \
        "pixel >= $BUTTON_MIN, riquadro dentro $BUTTON_RECT: il pulsante e' cambiato, e nient'altro"

    m2=$(take_measure magenta "$turned" "$SCREEN_RECT")
    m3=$(take_measure magenta "$turned" "$CLIP_RECT")
    box "$m3"
    side_width=$BL
    m1=$(take_measure sagoma "$turned" "$pressed")
    box "$m1"
    good=1
    [ "$(val "$m1" pixel)" -ge "$ROT_MIN" ] && inside "$CLIP_RECT" && [ "$(val "$m3" magenta)" -gt 0 ] \
        && [ "$(val "$m2" magenta)" -eq "$(val "$m3" magenta)" ] && [ "$side_width" -lt "$front_width" ] && good=0
    verdict rotazione "$good" "sagoma $m1; schermata $m2; ritaglio $m3" \
        "pixel >= $ROT_MIN dentro $CLIP_RECT; magenta > 0, solo nel ritaglio, piu' stretto che di fronte: di fianco"
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
