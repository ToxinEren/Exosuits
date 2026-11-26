#!/usr/bin/env bash
# La sequenza della parte 3d ("il braccio in prima persona") nel client vero: due esecuzioni di client_shot.sh, la
# seconda con il giocatore mancino, e poi quattordici controlli che lo script fa sugli scatti con arm_measure.sh.
#
#   EXO_ACCEPT_EULA=true bash tools/client/m3d_shots.sh <variante> <cartella>
#
# Il giocatore resta in creativa in 0,5 -60 0,5 rivolto a sud con l'inclinazione 15, l'interfaccia visibile (con F1 il
# gioco non disegna la mano). I pezzi della tuta sono dipinti di magenta (FF00FF), un colore che nient'altro sullo
# schermo ha: il magenta dice dove sono i pezzi. Da invisibile il gioco non disegna nessun braccio: lo scatto "senza
# braccio", con lo stesso mondo e la stessa interfaccia, e' quello fatto con l'effetto di invisibilita' (senza icona).
# Con la tuta lo schermo ha anche l'HUD della tuta (design M4 par. 5.12), nell'angolo in basso opposto alla mano
# principale, visibile in tutti gli scatti (la sequenza non preme H): e' uguale nei due scatti di ogni coppia misurata,
# salvo braccio-nudo (il telaio senza elmo: la sola barra) contro tuta-senza (l'HUD intero, con la sagoma), e per questo
# il telaio nudo ha il suo scatto senza braccio, nudo-senza, con lo stesso HUD di braccio-nudo. Il criterio (b) guarda
# il rettangolo dell'HUD della mano dello scatto (hud= di arm_measure.sh): il braccio non ci entra mai, per costruzione,
# perche' l'HUD sta nell'angolo opposto alla mano principale. Negli scatti con l'elmo in prima persona (braccio-rottame,
# tuta-senza, bastone, mappa, telaio-piazzato, inventario sotto la schermata, braccio-mancino e il suo tuta-senza) c'e'
# anche la vignetta del casco nei quattro angoli dello schermo, accesa dalle preferenze predefinite, uguale nei due
# scatti di ogni coppia; con il telaio nudo (senza elmo) e in terza persona non c'e' (misurato su 1.21.1-neoforge contro
# gli scatti di prima della vignetta: cambiano solo i quattro angoli degli scatti con l'elmo in prima persona). Nei due
# angoli in basso scurisce anche il braccio della tuta, ma per arm_measure.sh il magenta scurito resta magenta: il
# magenta del braccio non cambia, quello della mappa a sinistra perde 8 pixel. Il controllo solo-tuta e' un po' meno
# sensibile nell'angolo in basso a destra (144x80+816+460): li' la vignetta scurisce braccio-rottame e non base-prima,
# quindi un braccio del gioco base disegnato in quell'angolo non conterebbe fra gli "uguali"; il resto della sua sagoma
# sta fuori dall'angolo: accettato. Misurato, la perdita e' nulla: il braccio del gioco base entra nell'angolo con 762
# dei suoi 18075 pixel (22x68+816+472), sul lato interno, dove la texture ha un'opacita' di al piu' 2/255 e la vignetta
# non cambia nessun pixel (lo scuro che arm_measure.sh vede sta in 92x56+868+484).
#
# Esecuzione A, mano principale a destra, scatti in <cartella>:
#   1. senza tuta     base-prima (il braccio del gioco) e base-senza (invisibile)
#   2. tuta dipinta   braccio-rottame e tuta-senza (invisibile). E' il primo braccio della tuta di un client appena
#                     avviato, prima di ogni F5 e dell'inventario: il modello del giocatore nasce con il segno young
#                     acceso e lo spegne solo il disegno in terza persona; se il codice non lo spegnesse da se' il
#                     braccio verrebbe a meta' misura, e solo qui lo si vedrebbe (controllo criteri, copertura >= 3 %)
#   3. telaio nudo    braccio-nudo: il braccio d'acciaio del telaio, senza pezzi e senza il braccio del pilota; e
#                     nudo-senza (invisibile)
#   4. altro oggetto  bastone: un bastone in mano, nessun braccio
#   5. mappa          mappa: una mappa a due mani, guardando in basso (inclinazione 50: con 15 mappa e mani stanno
#                     sotto lo schermo, anche nel gioco base); le due braccia della tuta ai lati
#   6. terza persona  terza-persona (F5, da dietro), poi di nuovo in prima persona
#   7. senza tuta     base-dopo: tolta la tuta, lo scatto e' quello di prima
#   8. telaio         telaio-piazzato: un telaio Rottame non dipinto piazzato cinque blocchi davanti, rivolto al
#                     giocatore, con il braccio della tuta sullo schermo (un'altra tuta disegnata dallo stesso renderer
#                     negli stessi fotogrammi del braccio)
#   9. inventario     inventario: l'anteprima del giocatore nell'inventario della sopravvivenza (la schermata della
#                     creativa non la mostra), con la tuta del passo 8
# Esecuzione B, mano principale a sinistra (EXO_SHOT_MAIN_HAND=left), scatti in <cartella>/mancino: base-mancino e
# base-senza senza tuta, braccio-mancino e tuta-senza con la tuta dipinta.
#
# Le due regole degli scatti misurati (arm_measure.sh confronta pixel: una riga di chat o un avviso in uno solo dei due
# scatti di una coppia entra nella misura):
#   - nessuna coppia misurata con "sagoma" o "uguali" ha un comando gamemode negli 11 s prima di uno dei due scatti: la
#     riga che quel comando lascia in chat dura 10 s e copre x 0-664, y 442-460, dentro il rettangolo della barra.
#     client_shot.sh manda il suo "gamemode creative" circa 6 s prima del primo passo. Per questo l'inventario, l'unico
#     passo con gamemode, e' l'ultimo: il suo scatto si misura solo con "magenta" dentro 216x210+304+60, che la riga
#     in chat non tocca, e dopo non si misura piu' nulla;
#   - nessuno scatto misurato con "magenta" si fa negli 8 s dopo aver dato un oggetto che sblocca ricette: il bastone
#     ne sblocca otto, e l'avviso delle ricette, che ha il titolo magenta, dura 5 s piu' l'entrata e l'uscita. Per
#     questo dopo il bastone c'e' "@attendi 8". La mappa (filled_map) non sblocca nulla.
# Il primo passo di ogni esecuzione e' "@attendi 12" e non "@attendi 8": oltre alla riga in chat c'e' l'avviso "Chat
# messages can't be verified" che il client mostra entrando nel server di prova (in alto a destra, circa
# 460x110+500+0, un decimo dello schermo). Misurato con uno scatto al secondo su 1.21.1-neoforge, 1.20.1-fabric e
# 1.20.1-forge: la riga in chat sparisce fra 3,5 e 4,6 s dopo il primo passo, l'avviso fra 7,0 e 8,1 s. Con 8 s il
# margine sarebbe meno di un secondo; con 12 sono almeno 4. Se l'avviso restasse in uno solo dei due scatti di una
# coppia, il riquadro della sagoma partirebbe da Y 0 e i controlli "controllo" o "mancino-controllo" lo direbbero.
#
# Non prova: il jar di produzione (il client gira nell'ambiente di sviluppo, con i nomi Mojang); il pezzo montato su un
# lato solo (il comando di debug monta sei pezzi o nessuno); una ricarica delle risorse (F3+T) con il braccio sullo
# schermo; la tuta di un secondo giocatore; che la mano si veda bene (criterio (a) di design, par. 13: lo si guarda
# negli scatti braccio-rottame, braccio-nudo, mappa e mancino/braccio-mancino); l'oscillazione del colpo. Sono nella
# checklist per il PC, docs/test-in-gioco/m3d-braccio.md.
#
# Uscita: una riga "ok <nome>: <misure>" o "KO <nome>: <misure> (atteso ...)" per controllo, tutti valutati anche dopo
# un fallimento; l'ultima riga e' "OK <variante>: scatti in <cartella>" (uscita 0) se le due esecuzioni sono uscite con
# 0 e ogni controllo e' ok, altrimenti "FAIL <variante>: ..." su stderr (uscita 1). Se l'esecuzione A fallisce la B non
# parte; i controlli girano solo se ci sono tutti e sedici gli scatti. Con M3D_SOLO_CONTROLLI=1 lo script salta le
# due esecuzioni e rifa' i controlli sugli scatti che la cartella ha gia' (per fissare le soglie senza riavviare il
# client). Uscita 2: uso sbagliato.
#
# Dura circa tre minuti e mezzo a cache calde (misurati 204-225 s sulle quattro varianti: 111-119 s l'esecuzione A,
# 75-86 s la B; 232 s una volta, la prima sequenza dopo una build). Le misure sono uguali al pixel sulle quattro
# varianti (i valori accanto alle soglie, piu' sotto), salvo due, che cambiano di poco anche fra due esecuzioni della
# stessa variante: il magenta della terza persona (fra 23147 e 23745, sempre nel riquadro 318x312+321+221) e quello
# dell'anteprima dell'inventario, che 1.21.1 ritaglia nel suo riquadro (98x140+356+120, magenta fra 10782 e 10850) e
# 1.20.1 no (su Forge largo 166-176 pixel da x 318-323, magenta 18633-18831; su Fabric 166-174 da x 319-323, magenta
# 18712-18810; alto 202 da y 66). Le variabili d'ambiente sono quelle di client_shot.sh (EXO_ACCEPT_EULA obbligatoria);
# EXO_SHOT_MAIN_HAND la imposta questo script per tutte e due le esecuzioni, qualunque valore abbia nell'ambiente di chi
# lo chiama. Istruzioni in tools/client/README.md.
set -u
# Soglie valide solo con la config predefinita: una EXO_SHOT_SERVER_CONFIG ereditata dall'ambiente le falserebbe.
unset EXO_SHOT_SERVER_CONFIG
# E con le preferenze del client predefinite: con un EXO_SHOT_CLIENT_PREFS ereditato che nasconde l'HUD i controlli
# fallirebbero senza una ragione scritta.
unset EXO_SHOT_CLIENT_PREFS

here=$(cd "$(dirname "$0")" && pwd)
measure="$here/arm_measure.sh"

if [ "$#" -ne 2 ]; then
    echo "Uso: EXO_ACCEPT_EULA=true bash tools/client/m3d_shots.sh <variante> <cartella>" >&2
    exit 2
fi
variant=$1
out=$2
u=${EXO_SHOT_USER:-Exo}

# --- soglie ---------------------------------------------------------------------------------------------------------
# Ciascuna con la ragione e, fra parentesi, il valore misurato su 1.21.1-neoforge, la prima variante.

# Il braccio del gioco senza tuta, contro lo stesso scatto da invisibile: almeno tanti pixel (misurati 18075). Se sono
# meno, o fuori dal quarto in basso a destra, e' sbagliato lo strumento (lo scatto "senza braccio", un avviso rimasto
# sullo schermo), non la mod.
CONTROL_MIN_PIXEL=10000
# I pezzi dipinti sul braccio: la meta', arrotondata per difetto al centinaio, del magenta di braccio-rottame
# (misurato 39962).
MAGENTA_MIN=19900
# Criterio (c) di design, par. 13: il braccio copre fra il 3 % e il 14 % dello schermo, qui in centesimi di punto
# (misurati 8,77 con i pezzi e 7,72 con il telaio nudo). Un braccio a meta' misura (il segno young) coprirebbe un
# quarto di quei pixel, sotto il 3 %.
COVER_MIN=300
COVER_MAX=1400
# Il braccio del gioco, se fosse disegnato insieme a quello della tuta, lascerebbe uguali allo scatto senza tuta i
# pixel della sua sagoma che la tuta non copre: se ne ammette al piu' uno su venti (misurati 0 su 18075, con i pezzi
# e con il telaio nudo).
SAME_FACTOR=20
# Mappa a due mani: il magenta di ciascuna meta' dello schermo (misurati 23629 a sinistra, dove il gruppo dell'HUD
# della tuta sta sopra il braccio e ne copre una parte, come vuole il design M4 par. 5.12, e la vignetta ne scurisce
# l'angolo in basso, e 28977 a destra; a sinistra 23637 prima della vignetta e 28407 prima dell'HUD); un braccio solo
# ne lascerebbe una a zero.
MAP_MIN_HALF=300
# Una tuta intera e' larga piu' del doppio di un braccio solo, quindi la larghezza del riquadro distingue "intera" da
# "un braccio solo". In terza persona, da dietro, il magenta della tuta e' largo 318 pixel.
THIRD_PERSON_MIN_W=130
# Nell'anteprima dell'inventario il magenta della tuta e' largo 98 pixel (su 1.21.1 e' la larghezza del riquadro
# dell'anteprima, che ritaglia la tuta; su 1.20.1, che non ritaglia, fra 166 e 176). Il rettangolo misurato e' la meta'
# sinistra della finestra dell'inventario, dalla sua cima fino a sopra le caselle: non contiene ne' il braccio in
# basso a destra ne' la riga in chat.
INVENTORY_RECT=216x210+304+60
INVENTORY_MIN_W=70
# Tolta la tuta lo scatto torna quello di prima: al piu' tanti pixel diversi (misurati 0).
NO_SUIT_MAX_PIXEL=200
# Il telaio piazzato a cinque blocchi, cioe' quello che lo scatto ha in piu' di braccio-rottame: largo 258 pixel. Il
# magenta del braccio resta quello di braccio-rottame, fra il 90 % e il 110 % (misurato il 100 %): il telaio piazzato
# non e' dipinto, e disegnarlo non spegne ne' accende i pezzi del braccio.
PLACED_MIN_W=120
PLACED_MAGENTA_MIN_PCT=90
PLACED_MAGENTA_MAX_PCT=110
# Il braccio del mancino e' lo specchio del destro: il magenta fra l'80 % e il 120 % di quello del destro (misurato il
# 100 %) e il riquadro specchiato (X' = 960 - X - L) entro 12 pixel per lato (misurati 0 sui quattro lati).
MIRROR_MIN_PCT=80
MIRROR_MAX_PCT=120
MIRROR_TOL=12
# Il primo passo di ogni esecuzione: vedi il commento di testa.
FIRST_WAIT=12

# --- passi ----------------------------------------------------------------------------------------------------------
INV="effect give $u minecraft:invisibility 30 0 true"
VIS="effect clear $u minecraft:invisibility"
WEAR="execute as $u run exoframe debug wear rottame 0 FF00FF FF00FF"
QUI="tp $u 0.5 -60 0.5 0 15"

steps_a=(
    # 1. senza tuta. L'attesa: la riga in chat del "gamemode creative" di client_shot.sh e l'avviso della chat non
    # verificata devono essere spariti (commento di testa)
    "@attendi $FIRST_WAIT" "@scatto base-prima" "$INV" "@attendi 1" "@scatto base-senza" "$VIS" "@attendi 1"
    # 2. tuta dipinta, prima di ogni F5 e dell'inventario: il segno young (commento di testa)
    "$WEAR" "@attendi 2" "@scatto braccio-rottame" "$INV" "@attendi 1" "@scatto tuta-senza" "$VIS" "@attendi 1"
    # 3. telaio nudo, e lo stesso da invisibile: il suo HUD e' la sola barra (commento di testa)
    "execute as $u run exoframe debug wear nudo" "@attendi 2" "@scatto braccio-nudo"
    "$INV" "@attendi 1" "@scatto nudo-senza" "$VIS" "@attendi 1"
    # 4. un altro oggetto. L'attesa: il bastone sblocca otto ricette, e l'avviso delle ricette, che ha il titolo
    # magenta, dura 5 s piu' l'entrata e l'uscita
    "$WEAR" "item replace entity $u weapon.mainhand with minecraft:stick" "@attendi 8" "@scatto bastone"
    # 5. mappa a due mani, guardando in basso
    "item replace entity $u weapon.mainhand with minecraft:filled_map" "tp $u 0.5 -60 0.5 0 50" "@attendi 2"
    "@scatto mappa" "item replace entity $u weapon.mainhand with minecraft:air" "$QUI" "@attendi 1"
    # 6. terza persona (da dietro) e ritorno: F5 tre volte
    "@tieni F5 0.2" "@attendi 2" "@scatto terza-persona" "@tieni F5 0.2" "@tieni F5 0.2" "@attendi 1"
    # 7. senza tuta, dopo
    "execute as $u run exoframe debug wear via" "$QUI" "@attendi 2" "@scatto base-dopo"
    # 8. un telaio piazzato cinque blocchi davanti, rivolto al giocatore: lo piazza il giocatore stesso da 0,5 -60 5,5
    # rivolto a nord, poi torna al suo posto
    "$WEAR" "tp $u 0.5 -60 5.5 180 0" "execute as $u run exoframe debug place rottame" "$QUI" "@attendi 2"
    "@scatto telaio-piazzato"
    # 9. anteprima dell'inventario, per ultima: e' l'unico passo con gamemode, e dopo non si misura piu' nulla
    "gamemode survival $u" "@tieni e 0.2" "@attendi 1" "@scatto inventario" "@tieni Escape 0.2"
    "gamemode creative $u" "@attendi 1"
)
steps_b=(
    "@attendi $FIRST_WAIT" "@scatto base-mancino" "$INV" "@attendi 1" "@scatto base-senza" "$VIS" "@attendi 1"
    "$WEAR" "@attendi 2" "@scatto braccio-mancino" "$INV" "@attendi 1" "@scatto tuta-senza" "$VIS"
)
shots_a=(base-prima base-senza braccio-rottame tuta-senza braccio-nudo nudo-senza bastone mappa terza-persona
    base-dopo telaio-piazzato inventario)
shots_b=(base-mancino base-senza braccio-mancino tuta-senza)

# --- le due esecuzioni ----------------------------------------------------------------------------------------------
problems=() # le ragioni di un FAIL, nell'ordine in cui si incontrano
if [ "${M3D_SOLO_CONTROLLI:-}" != 1 ]; then
    echo "=== $variant m3d"
    # un'uscita anticipata di client_shot.sh (uso o ambiente sbagliati) non lascia leggere gli avvisi di un'altra
    # esecuzione, e se la B non parte i controlli non girano sugli scatti del mancino di una volta prima
    rm -f "$out/log-check.txt" "$out/mancino/log-check.txt"
    for s in "${shots_b[@]}"; do rm -f "$out/mancino/$s.png"; done
    run() { # run <nome> <mano> <cartella> <passi...>
        local name=$1 hand=$2 dir=$3 code
        shift 3
        EXO_SHOT_MAIN_HAND=$hand bash "$here/client_shot.sh" "$variant" "$dir" "$@"
        code=$?
        [ "$code" -eq 0 ] && return 0
        if [ -s "$dir/log-check.txt" ]; then
            problems+=("esecuzione $name: avvisi del mod fuori elenco in $dir/log-check.txt")
        else
            problems+=("esecuzione $name: client_shot.sh e' uscito con $code")
        fi
        return 1
    }
    run A right "$out" "${steps_a[@]}" && run B left "$out/mancino" "${steps_b[@]}"
fi

# --- controlli sugli scatti -----------------------------------------------------------------------------------------
missing=()
for s in "${shots_a[@]}"; do [ -f "$out/$s.png" ] || missing+=("$s"); done
for s in "${shots_b[@]}"; do [ -f "$out/mancino/$s.png" ] || missing+=("mancino/$s"); done

a="$out"
b="$out/mancino"
# Gli aiuti delle misure (take_measure, val, box, inside, verdict) e l'elenco failed: tools/client/measure_lib.sh.
source "$here/measure_lib.sh" || exit 2
# Criteri (b) e (c) sulla riga di una sagoma: nulla in mirino, barra e HUD; copertura fra il 3 % e il 14 %.
criteria_bc() {
    local c
    c=$(val "$1" copertura)
    [[ "$c" =~ ^[0-9]+\.[0-9][0-9]$ ]] || return 1
    c=$((10#${c/./}))
    [ "$(val "$1" mirino)" -eq 0 ] && [ "$(val "$1" barra)" -eq 0 ] && [ "$(val "$1" hud)" -eq 0 ] \
        && [ "$c" -ge "$COVER_MIN" ] && [ "$c" -le "$COVER_MAX" ]
}
# La parte misurabile del criterio (a): il riquadro nel quarto in basso a destra, o a sinistra per il mancino.
lower_right() { box "$1"; [ "$BL" -gt 0 ] && [ "$BX" -ge 480 ] && [ "$BY" -ge 270 ]; }
lower_left() { box "$1"; [ "$BL" -gt 0 ] && [ $((BX + BL)) -le 480 ] && [ "$BY" -ge 270 ]; }
CRITERIA="mirino=0 barra=0 hud=0, copertura fra 3.00 e 14.00"
RIGHT="riquadro con X >= 480 e Y >= 270"
# Il braccio di base non e' disegnato insieme a quello della tuta: riga di "uguali".
not_base_arm() {
    [ "$(val "$1" sagoma)" -gt 0 ] && [ $(($(val "$1" uguali) * SAME_FACTOR)) -le "$(val "$1" sagoma)" ]
}
SAME="uguali x $SAME_FACTOR <= sagoma"

checks() {
    local m1 m2 m3 good pct w ref want

    m1=$(take_measure sagoma "$a/base-prima.png" "$a/base-senza.png")
    good=1
    [ "$(val "$m1" pixel)" -ge "$CONTROL_MIN_PIXEL" ] && [ "$(val "$m1" mirino)" -eq 0 ] \
        && [ "$(val "$m1" barra)" -eq 0 ] && [ "$(val "$m1" hud)" -eq 0 ] && lower_right "$m1" && good=0
    want="pixel >= $CONTROL_MIN_PIXEL, mirino=0 barra=0 hud=0, $RIGHT: se non e' cosi' e' sbagliato lo strumento"
    verdict controllo "$good" "$m1" "$want o lo scatto senza braccio, non la mod"

    m1=$(take_measure magenta "$a/braccio-rottame.png")
    m2=$(take_measure magenta "$a/braccio-rottame.png" 480x540+0+0)
    good=1
    [ "$(val "$m1" magenta)" -ge "$MAGENTA_MIN" ] && [ "$(val "$m2" magenta)" -eq 0 ] && good=0
    verdict dipinto "$good" "$m1; meta' sinistra $m2" "magenta >= $MAGENTA_MIN, e 0 nella meta' sinistra"

    m1=$(take_measure sagoma "$a/braccio-rottame.png" "$a/tuta-senza.png")
    good=1
    criteria_bc "$m1" && lower_right "$m1" && good=0
    verdict criteri "$good" "$m1" "$CRITERIA, $RIGHT"

    m1=$(take_measure uguali "$a/base-prima.png" "$a/braccio-rottame.png" "$a/base-senza.png")
    good=1
    not_base_arm "$m1" && good=0
    verdict solo-tuta "$good" "$m1" "$SAME: il braccio del gioco non si disegna insieme a quello della tuta"

    m1=$(take_measure magenta "$a/braccio-nudo.png")
    m2=$(take_measure sagoma "$a/braccio-nudo.png" "$a/nudo-senza.png")
    m3=$(take_measure uguali "$a/base-prima.png" "$a/braccio-nudo.png" "$a/base-senza.png")
    good=1
    [ "$(val "$m1" magenta)" -eq 0 ] && criteria_bc "$m2" && lower_right "$m2" && not_base_arm "$m3" && good=0
    verdict nudo "$good" "$m1; $m2; $m3" "magenta=0; $CRITERIA, $RIGHT; $SAME"

    m1=$(take_measure magenta "$a/bastone.png")
    good=1
    [ "$(val "$m1" magenta)" -eq 0 ] && good=0
    verdict bastone "$good" "$m1" "magenta=0: con un oggetto in mano nessun braccio"

    m1=$(take_measure magenta "$a/mappa.png" 480x540+0+0)
    m2=$(take_measure magenta "$a/mappa.png" 480x540+480+0)
    good=1
    [ "$(val "$m1" magenta)" -ge "$MAP_MIN_HALF" ] && [ "$(val "$m2" magenta)" -ge "$MAP_MIN_HALF" ] && good=0
    want="magenta >= $MAP_MIN_HALF in ciascuna meta': le due braccia della tuta"
    verdict mappa "$good" "sinistra $m1; destra $m2" "$want"

    m1=$(take_measure magenta "$a/terza-persona.png")
    box "$m1"
    good=1
    [ "$BL" -ge "$THIRD_PERSON_MIN_W" ] && good=0
    verdict terza-persona "$good" "$m1" "riquadro largo >= $THIRD_PERSON_MIN_W: la tuta intera, non un braccio solo"

    m1=$(take_measure magenta "$a/inventario.png" "$INVENTORY_RECT")
    box "$m1"
    good=1
    [ "$BL" -ge "$INVENTORY_MIN_W" ] && good=0
    want="riquadro largo >= $INVENTORY_MIN_W dentro $INVENTORY_RECT: la tuta intera nell'anteprima"
    verdict inventario "$good" "$m1" "$want"

    m1=$(take_measure sagoma "$a/base-dopo.png" "$a/base-prima.png")
    good=1
    [ "$(val "$m1" pixel)" -ge 0 ] && [ "$(val "$m1" pixel)" -le "$NO_SUIT_MAX_PIXEL" ] && good=0
    verdict senza-tuta "$good" "$m1" "pixel <= $NO_SUIT_MAX_PIXEL: tolta la tuta, lo scatto di prima"

    m1=$(take_measure sagoma "$a/telaio-piazzato.png" "$a/braccio-rottame.png")
    m2=$(take_measure magenta "$a/telaio-piazzato.png")
    ref=$(val "$(take_measure magenta "$a/braccio-rottame.png")" magenta)
    box "$m1"
    w=$BL
    pct=-1
    [ "$ref" -gt 0 ] && pct=$(($(val "$m2" magenta) * 100 / ref))
    good=1
    [ "$w" -ge "$PLACED_MIN_W" ] && [ "$pct" -ge "$PLACED_MAGENTA_MIN_PCT" ] \
        && [ "$pct" -le "$PLACED_MAGENTA_MAX_PCT" ] && good=0
    want="riquadro largo >= $PLACED_MIN_W; magenta fra il $PLACED_MAGENTA_MIN_PCT % e il $PLACED_MAGENTA_MAX_PCT %"
    verdict telaio-piazzato "$good" "$m1; $m2, il $pct % di braccio-rottame" "$want di quello di braccio-rottame"

    m1=$(take_measure sagoma "$b/base-mancino.png" "$b/base-senza.png" sinistra)
    box "$m1"
    good=1
    [ "$(val "$m1" pixel)" -ge "$CONTROL_MIN_PIXEL" ] && [ $((BX + BL)) -le 480 ] && good=0
    want="pixel >= $CONTROL_MIN_PIXEL e riquadro nella meta' sinistra (X + L <= 480): se non e' cosi' l'opzione"
    want+=" mainHand non ha avuto effetto, un problema dello strumento e non della mod"
    verdict mancino-controllo "$good" "$m1" "$want"

    m1=$(take_measure magenta "$b/braccio-mancino.png" 480x540+480+0)
    m2=$(take_measure sagoma "$b/braccio-mancino.png" "$b/tuta-senza.png" sinistra)
    good=1
    [ "$(val "$m1" magenta)" -eq 0 ] && criteria_bc "$m2" && lower_left "$m2" && good=0
    want="magenta=0 nella meta' destra; $CRITERIA, riquadro con X + L <= 480 e Y >= 270"
    verdict mancino "$good" "meta' destra $m1; $m2" "$want"

    m1=$(take_measure magenta "$b/braccio-mancino.png")
    m2=$(take_measure magenta "$a/braccio-rottame.png")
    ref=$(val "$m2" magenta)
    pct=-1
    [ "$ref" -gt 0 ] && pct=$(($(val "$m1" magenta) * 100 / ref))
    box "$m2"
    local rl=$BL ra=$BA rx=$BX ry=$BY mx dx dy dr db
    box "$m1"
    mx=$((960 - BX - BL)) # il bordo sinistro del riquadro del mancino, specchiato
    dx=$((mx - rx))
    dy=$((BY - ry))
    dr=$((mx + BL - rx - rl))
    db=$((BY + BA - ry - ra))
    good=1
    [ "$pct" -ge "$MIRROR_MIN_PCT" ] && [ "$pct" -le "$MIRROR_MAX_PCT" ] && [ "$BL" -gt 0 ] \
        && [ "${dx#-}" -le "$MIRROR_TOL" ] && [ "${dy#-}" -le "$MIRROR_TOL" ] && [ "${dr#-}" -le "$MIRROR_TOL" ] \
        && [ "${db#-}" -le "$MIRROR_TOL" ] && good=0
    m3="riquadro specchiato ${BL}x${BA}+${mx}+${BY}, scarti sinistra $dx alto $dy destra $dr basso $db"
    want="magenta fra il $MIRROR_MIN_PCT % e il $MIRROR_MAX_PCT % di quello del destro"
    verdict specchio "$good" "mancino $m1, il $pct % del destro ($m2); $m3" \
        "$want; ogni lato del riquadro specchiato entro $MIRROR_TOL pixel"
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
