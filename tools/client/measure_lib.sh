# shellcheck shell=bash
# Aiuti comuni alle sequenze del client che controllano gli scatti con arm_measure.sh (m3d_shots.sh, m4a1_shots.sh,
# m4c_shots.sh): trasformano le righe delle misure in controlli "ok" o "KO". Si carica con source dalla sequenza, non
# si esegue.
#
# Prima di usare take_measure il chiamante imposta measure, il percorso di arm_measure.sh. Caricare il file svuota
# l'elenco failed (i controlli falliti, per nome, che verdict riempie) e definisce:
#   take_measure <argomenti di arm_measure.sh>
#       stampa la riga della misura, oppure "misura-non-riuscita" se arm_measure.sh esce con un errore
#   val <riga> <nome>
#       stampa il valore dopo "<nome>=" nella riga di una misura, oppure -1 se non c'e'
#   box <riga>
#       mette larghezza, altezza, X e Y del riquadro della misura in BL, BA, BX e BY (tutti 0 se non ce n'e' uno)
#   inside <LxA+X+Y>
#       vero se il riquadro dell'ultima box non e' vuoto e sta dentro il rettangolo dato
#   verdict <nome> <0 se vale> <misure> <atteso>
#       stampa "ok <nome>: <misure>" oppure "KO <nome>: <misure> (atteso <atteso>)" e aggiunge il nome a failed; una
#       misura non riuscita fa fallire il controllo comunque

failed=() # i controlli falliti, per nome
# Una misura di arm_measure.sh: la sua riga, oppure una parola che fa fallire il controllo che la usa.
take_measure() { bash "$measure" "$@" 2>/dev/null || echo "misura-non-riuscita"; }
# val <riga> <nome>: il valore dopo "<nome>=" nella riga di una misura; -1 se non c'e'. Le parole della riga si leggono
# in un vettore, senza espansione dei percorsi: una parola con * o ? resta com'e'.
val() {
    local w words
    read -r -d '' -a words <<<"$1"
    for w in "${words[@]}"; do
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
