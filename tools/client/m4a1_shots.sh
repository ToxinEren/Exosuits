#!/usr/bin/env bash
# La sequenza della parte 4a-1 ("il banco") nel client vero: un'esecuzione di client_shot.sh con i passi qui sotto, e
# poi i controlli che lo script fa da se' sugli scatti con arm_measure.sh.
#
#   EXO_ACCEPT_EULA=true bash tools/client/m4a1_shots.sh <variante> <cartella>
#
# Il giocatore resta in creativa in 0,5 -60 0,5 rivolto a sud; finestra 960x540 e scala dell'interfaccia 2, quindi
# interfaccia 480x270 e schermata della stazione (316x232) con l'angolo nel punto 82,19 dell'interfaccia, cioe' il
# riquadro 632x464+164+38 negli scatti. Un punto (x, y) della schermata e' il punto (2 (82 + x), 2 (19 + y)) degli
# scatti. La stazione sta in 0 -60 1, con il fronte a nord, verso il giocatore.
#
#   1. attesa       "@attendi 12": la riga in chat del "gamemode creative" di client_shot.sh e l'avviso "Chat
#                   messages can't be verified" (460x110+500+0, sopra il ritaglio dell'anteprima) spariscono prima
#                   del primo scatto misurato (vedi tools/client/README.md, regole degli scatti misurati)
#   2. posa         setblock della stazione in 0 -60 1 (un blocco solo, il segnaposto della parte 4a-1)
#   3. apertura     con l'inclinazione 60 il mirino sta sul fianco nord della stazione: un clic destro la apre, con i
#                   dati extra; la schermata si apre solo se il tipo del menu porta i dati e la schermata e' registrata
#                   (su NeoForge un errore della registrazione sarebbe silenzioso); il puntatore nel punto neutro
#                   10,10, fuori dalla schermata; scatto stazione-aperta, da guardare: la schermata scura al centro e,
#                   nel riquadro di destra, la tuta fissa di fronte, in piedi, con il braccio destro magenta sul lato
#                   sinistro dello schermo, tagliata dal ritaglio se ne esce
#   4. pulsante     clic sinistro al centro del pulsante di prova (punto 748,426: il pulsante e' 283..301 x 186..202
#                   nella schermata, 36x32+730+410 negli scatti): il numero 102 va al server con il pacchetto dei
#                   pulsanti del gioco base, il server conta la pressione e il gioco base rimanda il contatore; il
#                   puntatore di nuovo nel punto neutro; scatto premuto, da guardare: il pulsante in basso a destra
#                   verde con 1
#   5. chiusura     Escape chiude la schermata
#   6. fine         tp in 0,5 -60 -5,5 per i tre scatti di client_shot.sh
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
# Non prova: il jar di produzione (il client gira nell'ambiente di sviluppo); la stazione di due blocchi, la ricerca del
# telaio, la vista del telaio, l'anteprima girevole e il pannello del telaio, che arrivano con i task dopo; la
# schermata con altre scale dell'interfaccia e altre misure della finestra; una ricarica delle risorse (F3+T) con la
# schermata aperta. Sono nella checklist per il PC della parte.
#
# Dura circa un minuto e tre quarti a cache calde (misurati 88-107 s sulle quattro varianti, di cui 83-102 s
# l'esecuzione di client_shot.sh). Le misure sono uguali al pixel sulle quattro varianti (i valori accanto alle soglie,
# piu' sotto), e cosi' il ritaglio dell'anteprima: la scala della tuta nell'interfaccia passa da GuiEntityScale, che
# la applica come il gioco base di ogni versione (senza, su 1.20.1 la tuta usciva piu' scura, magenta 999).
#
# Uscita: una riga "ok <nome>: <misure>" o "KO <nome>: <misure> (atteso ...)" per controllo, tutti valutati anche dopo
# un fallimento; l'ultima riga e' "OK <variante>: scatti in <cartella>" (uscita 0) se client_shot.sh e' uscito con 0 e
# ogni controllo e' ok, altrimenti "FAIL <variante>: ..." su stderr (uscita 1). I controlli girano solo se ci sono i due
# scatti. Con M4A1_SOLO_CONTROLLI=1 lo script salta l'esecuzione e rifa' i controlli sugli scatti che la cartella ha
# gia' (per fissare le soglie senza riavviare il client). Uscita 2: uso sbagliato. Le variabili d'ambiente sono quelle
# di client_shot.sh (EXO_ACCEPT_EULA obbligatoria). Istruzioni in tools/client/README.md.
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
    # 2. posa
    "setblock 0 -60 1 redstone_exoframes:station"
    # 3. apertura: il mirino sul fianco nord della stazione
    "tp $u 0.5 -60 0.5 0 60" "@attendi 1" "@click destro" "@attendi 2" "$NEUTRAL" "@attendi 1"
    "@scatto stazione-aperta"
    # 4. pulsante di prova, al suo centro
    "@clic 748 426" "@attendi 1" "$NEUTRAL" "@attendi 1" "@scatto premuto"
    # 5. chiusura
    "@tieni Escape 0.2" "@attendi 1"
    # 6. fine, all'aperto per i tre scatti di client_shot.sh
    "tp $u 0.5 -60 -5.5 0 15"
)
shots=(stazione-aperta premuto)

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

failed=() # i controlli falliti, per nome
# Una misura di arm_measure.sh: la sua riga, oppure una parola che fa fallire il controllo che la usa.
take_measure() { bash "$measure" "$@" 2>/dev/null || echo "misura-non-riuscita"; }
# val <riga> <nome>: il valore dopo "<nome>=" nella riga di una misura; -1 se non c'e'.
val() {
    local w
    for w in $1; do
        case "$w" in "$2="*) echo "${w#*=}"; return 0 ;; esac
    done
    echo -1
}
# box <riga>: larghezza, altezza, X e Y del riquadro della misura in BL, BA, BX, BY.
box() {
    local r
    r=$(val "$1" riquadro)
    IFS='x+' read -r BL BA BX BY <<<"$r"
    [[ "$BL" =~ ^[0-9]+$ && "$BA" =~ ^[0-9]+$ && "$BX" =~ ^[0-9]+$ && "$BY" =~ ^[0-9]+$ ]] && return 0
    BL=0 BA=0 BX=0 BY=0
}
# inside <LxA+X+Y>: il riquadro dell'ultima box non e' vuoto e sta dentro il rettangolo dato.
inside() {
    local rl ra rx ry
    IFS='x+' read -r rl ra rx ry <<<"$1"
    [ "$BL" -gt 0 ] && [ "$BX" -ge "$rx" ] && [ "$BY" -ge "$ry" ] && [ $((BX + BL)) -le $((rx + rl)) ] \
        && [ $((BY + BA)) -le $((ry + ra)) ]
}
# verdict <nome> <0 se vale> <misure> <atteso>: la riga del controllo; una misura non riuscita lo fa fallire comunque.
verdict() {
    local good=$2
    case "$3" in *misura-non-riuscita*) good=1 ;; esac
    if [ "$good" -eq 0 ]; then
        echo "ok $1: $3"
    else
        echo "KO $1: $3 (atteso $4)"
        failed+=("$1")
    fi
}

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
