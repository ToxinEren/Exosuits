#!/usr/bin/env bash
# Misure sugli scatti del braccio in prima persona (design par. 13): conta e delimita pixel, non giudica. Bash e
# ImageMagick 6 (convert, identify); non avvia nulla e non scrive nulla fuori da una cartella temporanea.
#
#   bash tools/client/arm_measure.sh sagoma <con.png> <senza.png>
#       pixel=<N> copertura=<P.PP> mirino=<N> barra=<N> energia=<N> riquadro=<LxA+X+Y>
#       La sagoma del braccio: i pixel in cui lo scatto con il braccio differisce dallo stesso scatto senza braccio,
#       la loro parte dello schermo in percento (due decimali), quanti cadono nei tre rettangoli che il braccio non
#       deve toccare (mirino 60x60+450+240, barra degli oggetti con le barre di stato 380x100+290+440, riga
#       dell'energia 360x30+0+0) e il riquadro che li contiene tutti.
#   bash tools/client/arm_measure.sh magenta <scatto.png> [<LxA+X+Y>]
#       magenta=<N> riquadro=<LxA+X+Y>
#       I pixel color magenta dello scatto (del solo rettangolo, se dato) e il riquadro che li contiene, nelle
#       coordinate dello scatto intero.
#   bash tools/client/arm_measure.sh uguali <a.png> <b.png> <senza.png>
#       sagoma=<N> uguali=<N>
#       Dei pixel in cui a differisce da senza (la sagoma di a), quanti sono uguali in a e in b.
#   bash tools/client/arm_measure.sh --selftest
#       OK arm_measure
#       Costruisce quattro immagini di prova e controlla sette uscite note.
#
# Senza pixel il riquadro e' 0x0+0+0. I rettangoli si scrivono LxA+X+Y (larghezza, altezza, angolo in alto a sinistra)
# e devono stare dentro lo scatto.
#
# Due pixel "differiscono" se un canale qualunque dista piu' del 4 % del fondo scala: si prende il massimo dei tre
# canali della differenza, non il grigio, che non vedrebbe una differenza nel solo blu; una differenza di 1/255 non
# conta. "Uguali" e' il contrario, con la stessa soglia.
# Un pixel e' "magenta" se, con i canali fra 0 e 1, r > 0,2, b > 0,2, g < 0,45 r e g < 0,45 b: il colore FF00FF dato ai
# pezzi resta di quella tinta sotto ogni luce, e cielo, erba, barra degli oggetti, riga dell'energia, acciaio e ottone
# del telaio non lo sono. Nell'interfaccia sono magenta anche il titolo dell'avviso delle ricette, i nomi degli oggetti
# di rarita' epica e il viola scuro dei testi: gli scatti da misurare si fanno senza avvisi sullo schermo.
#
# Gli scatti sono quelli di client_shot.sh, 960x540. Uscite: 0 con una riga su stdout; 1 una misura non riuscita
# (convert) o un'uscita del selftest diversa dall'attesa; 2 uso sbagliato, scatto che non si legge o non e' 960x540,
# rettangolo scritto male o fuori dallo scatto, programmi mancanti. Ogni errore stampa su stderr una riga "ERRORE: ...".
set -u
export LC_ALL=C

W=960
H=540
DIFF=4% # soglia della differenza su un canale
CROSSHAIR=60x60+450+240
HOTBAR=380x100+290+440
ENERGY=360x30+0+0

usage() {
    echo "Uso: bash tools/client/arm_measure.sh sagoma <con.png> <senza.png>" >&2
    echo "     bash tools/client/arm_measure.sh magenta <scatto.png> [<LxA+X+Y>]" >&2
    echo "     bash tools/client/arm_measure.sh uguali <a.png> <b.png> <senza.png>" >&2
    echo "     bash tools/client/arm_measure.sh --selftest" >&2
}
fatal() { echo "ERRORE: $*" >&2; exit 2; }
fail() { echo "ERRORE: $*" >&2; exit 1; }

for tool in convert identify awk; do
    command -v "$tool" >/dev/null 2>&1 || fatal "manca il programma $tool (ImageMagick 6: convert, identify)"
done

tmp=$(mktemp -d) || fatal "cartella temporanea non creata"
trap 'rm -rf "$tmp"' EXIT

# Lo scatto si legge ed e' 960x540, altrimenti uscita 2.
check_shot() { # file
    local size
    size=$(identify -format '%wx%h\n' "$1" 2>/dev/null | head -n 1) \
        || fatal "lo scatto '$1' non si legge come immagine"
    [ -n "$size" ] || fatal "lo scatto '$1' non si legge come immagine"
    [ "$size" = "${W}x${H}" ] || fatal "lo scatto '$1' e' $size, non ${W}x${H}"
}

# Il rettangolo e' scritto LxA+X+Y, non e' vuoto e sta dentro lo scatto, altrimenti uscita 2.
check_rect() { # rettangolo
    [[ "$1" =~ ^([0-9]+)x([0-9]+)\+([0-9]+)\+([0-9]+)$ ]] \
        || fatal "rettangolo '$1': si scrive LxA+X+Y (per esempio $CROSSHAIR)"
    local w=$((10#${BASH_REMATCH[1]})) h=$((10#${BASH_REMATCH[2]}))
    local x=$((10#${BASH_REMATCH[3]})) y=$((10#${BASH_REMATCH[4]}))
    [ "$w" -gt 0 ] && [ "$h" -gt 0 ] && [ $((x + w)) -le "$W" ] && [ $((y + h)) -le "$H" ] \
        || fatal "rettangolo '$1' vuoto o fuori dallo scatto ${W}x${H}"
}

# Le maschere sono immagini 960x540 bianche dove il pixel conta e nere altrove.

# La prima riga dell'errore dell'ultimo convert, per i messaggi.
why() { head -n 1 "$tmp/convert.err"; }

# Maschera dei pixel in cui due scatti differiscono.
diff_mask() { # a b maschera
    convert "$1" -alpha off "$2" -alpha off -compose difference -composite -separate -evaluate-sequence max \
        -threshold "$DIFF" "$3" 2>"$tmp/convert.err" || fail "confronto fra '$1' e '$2' non riuscito: $(why)"
}

# Maschera dei pixel color magenta di uno scatto.
magenta_mask() { # scatto maschera
    convert "$1" -alpha off -fx '(r>0.2 && b>0.2 && g<0.45*r && g<0.45*b) ? 1 : 0' -colorspace Gray -threshold 50% \
        "$2" 2>"$tmp/convert.err" || fail "ricerca del magenta in '$1' non riuscita: $(why)"
}

# Quanti pixel bianchi ha la maschera, in tutto o nel rettangolo dato.
count() { # maschera [rettangolo]
    local n
    if [ "$#" -ge 2 ]; then
        n=$(convert "$1" -crop "$2" +repage -format '%[fx:int(mean*w*h+0.5)]' info: 2>"$tmp/convert.err")
    else
        n=$(convert "$1" -format '%[fx:int(mean*w*h+0.5)]' info: 2>"$tmp/convert.err")
    fi
    [[ "$n" =~ ^[0-9]+$ ]] || fail "conto dei pixel non riuscito: $(why)"
    echo "$n"
}

# Il riquadro che contiene i pixel bianchi della maschera (del solo rettangolo, se dato), nelle coordinate dello
# scatto intero; 0x0+0+0 se non ce ne sono. Il riquadro lo da' il ritaglio di ImageMagick (%@), che toglie il bordo
# del colore degli angoli: una cornice nera di un pixel fissa quel colore anche quando il bianco tocca i bordi o
# riempie tutto, e si toglie dalle coordinate.
bbox() { # maschera numero-di-pixel [rettangolo]
    local box x0=0 y0=0
    if [ "$2" -eq 0 ]; then
        echo "0x0+0+0"
        return
    fi
    if [ "$#" -ge 3 ]; then
        [[ "$3" =~ \+([0-9]+)\+([0-9]+)$ ]]
        x0=$((10#${BASH_REMATCH[1]}))
        y0=$((10#${BASH_REMATCH[2]}))
        box=$(convert "$1" -crop "$3" +repage -bordercolor black -border 1 -format '%@' info: 2>"$tmp/convert.err")
    else
        box=$(convert "$1" -bordercolor black -border 1 -format '%@' info: 2>"$tmp/convert.err")
    fi
    [[ "$box" =~ ^([0-9]+)x([0-9]+)\+([0-9]+)\+([0-9]+)$ ]] || fail "riquadro non calcolato: $(why)"
    echo "${BASH_REMATCH[1]}x${BASH_REMATCH[2]}+$((BASH_REMATCH[3] - 1 + x0))+$((BASH_REMATCH[4] - 1 + y0))"
}

# Le funzioni qui sopra che stampano un valore girano dentro $(...): un loro errore esce dalla sola sottoshell, e chi
# le chiama esce a sua volta con 1.

measure_sagoma() { # con senza
    local n cover crosshair hotbar energy box
    check_shot "$1"
    check_shot "$2"
    diff_mask "$1" "$2" "$tmp/sagoma.png"
    n=$(count "$tmp/sagoma.png") || exit 1
    cover=$(awk -v n="$n" -v t=$((W * H)) 'BEGIN { printf "%.2f", n * 100 / t }')
    crosshair=$(count "$tmp/sagoma.png" "$CROSSHAIR") || exit 1
    hotbar=$(count "$tmp/sagoma.png" "$HOTBAR") || exit 1
    energy=$(count "$tmp/sagoma.png" "$ENERGY") || exit 1
    box=$(bbox "$tmp/sagoma.png" "$n") || exit 1
    echo "pixel=$n copertura=$cover mirino=$crosshair barra=$hotbar energia=$energy riquadro=$box"
}

measure_magenta() { # scatto [rettangolo]
    local n box
    check_shot "$1"
    [ "$#" -lt 2 ] || check_rect "$2"
    magenta_mask "$1" "$tmp/magenta.png"
    n=$(count "$tmp/magenta.png" "${@:2}") || exit 1
    box=$(bbox "$tmp/magenta.png" "$n" "${@:2}") || exit 1
    echo "magenta=$n riquadro=$box"
}

measure_uguali() { # a b senza
    local n same
    check_shot "$1"
    check_shot "$2"
    check_shot "$3"
    diff_mask "$1" "$3" "$tmp/sagoma.png"
    diff_mask "$1" "$2" "$tmp/diversi.png"
    # dentro la sagoma e non diversi fra a e b
    convert "$tmp/sagoma.png" \( "$tmp/diversi.png" -negate \) -compose multiply -composite "$tmp/uguali.png" \
        2>"$tmp/convert.err" || fail "confronto delle maschere non riuscito: $(why)"
    n=$(count "$tmp/sagoma.png") || exit 1
    same=$(count "$tmp/uguali.png") || exit 1
    echo "sagoma=$n uguali=$same"
}

selftest() {
    local d="$tmp/selftest" failed=0 sky="rgb(120,170,230)"
    mkdir -p "$d"
    convert -size "${W}x${H}" "xc:$sky" -fill 'rgb(90,140,70)' -draw 'rectangle 0,300 959,539' "$d/senza.png" \
        && convert "$d/senza.png" -fill 'rgb(200,0,200)' -draw 'rectangle 700,400 899,539' \
            -fill 'rgb(140,140,150)' -draw 'rectangle 690,370 739,399' "$d/con.png" \
        && convert "$d/con.png" -fill 'rgb(90,140,110)' -draw 'rectangle 600,450 699,499' \
            -fill 'rgb(120,170,231)' -draw 'rectangle 0,0 99,29' \
            -fill 'rgb(120,190,230)' -draw 'rectangle 470,250 479,259' "$d/dentro.png" \
        && convert "$d/con.png" -fill 'rgb(60,60,70)' -draw 'rectangle 700,400 799,539' "$d/altro.png" \
        || fail "selftest: immagini di prova non costruite"
    expect() { # atteso argomenti...
        local want=$1 got
        shift
        got=$(bash "$0" "$@" 2>&1)
        if [ "$got" != "$want" ]; then
            echo "selftest: $(printf '%s ' "$@" | sed "s|$d/||g; s|\\.png||g")" >&2
            echo "  atteso:  $want" >&2
            echo "  trovato: $got" >&2
            failed=1
        fi
    }
    expect "pixel=29500 copertura=5.69 mirino=0 barra=0 energia=0 riquadro=210x170+690+370" \
        sagoma "$d/con.png" "$d/senza.png"
    # la differenza nel solo blu conta (dentro la barra e fuori), quella di 1/255 no (nella riga dell'energia)
    expect "pixel=34600 copertura=6.67 mirino=100 barra=3500 energia=0 riquadro=430x290+470+250" \
        sagoma "$d/dentro.png" "$d/senza.png"
    expect "pixel=0 copertura=0.00 mirino=0 barra=0 energia=0 riquadro=0x0+0+0" sagoma "$d/senza.png" "$d/senza.png"
    expect "magenta=28000 riquadro=200x140+700+400" magenta "$d/con.png"
    expect "magenta=0 riquadro=0x0+0+0" magenta "$d/con.png" 480x540+0+0
    expect "magenta=5000 riquadro=100x50+750+420" magenta "$d/con.png" 100x50+750+420
    expect "sagoma=29500 uguali=15500" uguali "$d/con.png" "$d/altro.png" "$d/senza.png"
    [ "$failed" -eq 0 ] || fail "selftest: uscite diverse dalle attese"
    echo "OK arm_measure"
}

case "${1:-}" in
    sagoma)
        [ "$#" -eq 3 ] || { usage; fatal "sagoma vuole due scatti: <con.png> <senza.png>"; }
        measure_sagoma "$2" "$3" ;;
    magenta)
        [ "$#" -eq 2 ] || [ "$#" -eq 3 ] || { usage; fatal "magenta vuole uno scatto e, se serve, un rettangolo"; }
        measure_magenta "${@:2}" ;;
    uguali)
        [ "$#" -eq 4 ] || { usage; fatal "uguali vuole tre scatti: <a.png> <b.png> <senza.png>"; }
        measure_uguali "$2" "$3" "$4" ;;
    --selftest)
        [ "$#" -eq 1 ] || { usage; fatal "--selftest non vuole altri argomenti"; }
        selftest ;;
    *)
        usage
        fatal "misura sconosciuta '${1:-}' (sagoma, magenta, uguali, --selftest)" ;;
esac
