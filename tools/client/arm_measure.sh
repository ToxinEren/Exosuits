#!/usr/bin/env bash
# Misure sugli scatti del client vero: il braccio in prima persona (design M3 par. 13) e l'HUD della tuta (design M4
# par. 5.12). Conta e delimita pixel, non giudica. Bash e ImageMagick 6 (convert, identify); non avvia nulla e non
# scrive nulla fuori da una cartella temporanea.
#
#   bash tools/client/arm_measure.sh sagoma <con.png> <senza.png> [destra|sinistra]
#       pixel=<N> copertura=<P.PP> mirino=<N> barra=<N> hud=<N> riquadro=<LxA+X+Y>
#       La sagoma del braccio: i pixel in cui lo scatto con il braccio differisce dallo stesso scatto senza braccio,
#       la loro parte dello schermo in percento (due decimali), quanti cadono nei tre rettangoli che il braccio non
#       deve toccare (mirino 60x60+450+240, barra degli oggetti con le barre di stato 380x100+290+440, HUD della tuta
#       della mano principale dello scatto: 176x70+4+466 con la destra, che vale anche senza il terzo argomento,
#       176x70+780+466 con la sinistra) e il riquadro che li contiene tutti.
#   bash tools/client/arm_measure.sh differenza <a.png> <b.png> [<LxA+X+Y>]
#       pixel=<N> riquadro=<LxA+X+Y>
#       I pixel in cui i due scatti differiscono (del solo rettangolo, se dato) e il riquadro che li contiene, nelle
#       coordinate dello scatto intero.
#   bash tools/client/arm_measure.sh colore <scatto.png> <RRGGBB> [<LxA+X+Y>]
#       colore=<N> riquadro=<LxA+X+Y>
#       I pixel del colore dato, sei cifre esadecimali (del solo rettangolo, se dato), e il riquadro che li contiene,
#       nelle coordinate dello scatto intero.
#   bash tools/client/arm_measure.sh magenta <scatto.png> [<LxA+X+Y>]
#       magenta=<N> riquadro=<LxA+X+Y>
#       I pixel color magenta dello scatto (del solo rettangolo, se dato) e il riquadro che li contiene, nelle
#       coordinate dello scatto intero.
#   bash tools/client/arm_measure.sh uguali <a.png> <b.png> <senza.png>
#       sagoma=<N> uguali=<N>
#       Dei pixel in cui a differisce da senza (la sagoma di a), quanti sono uguali in a e in b.
#   bash tools/client/arm_measure.sh --selftest
#       OK arm_measure
#       Costruisce quattro immagini di prova e controlla quindici uscite note.
#
# Senza pixel il riquadro e' 0x0+0+0. I rettangoli si scrivono LxA+X+Y (larghezza, altezza, angolo in alto a sinistra)
# e devono stare dentro lo scatto.
#
# Due pixel "differiscono" se un canale qualunque dista piu' del 4 % del fondo scala: si prende il massimo dei tre
# canali della differenza, non il grigio, che non vedrebbe una differenza nel solo blu; una differenza di 1/255 non
# conta. "Uguali" e' il contrario, con la stessa soglia. Un pixel e' "del colore" dato se ciascuno dei tre canali ne
# dista al piu' il 4 % del fondo scala: lo stesso margine, che assorbe gli arrotondamenti del disegno.
# Un pixel e' "magenta" se, con i canali fra 0 e 1, r > 0,2, b > 0,2, g < 0,45 r e g < 0,45 b: il colore FF00FF dato ai
# pezzi resta di quella tinta sotto ogni luce, e cielo, erba, barra degli oggetti, HUD, acciaio e ottone del telaio
# non lo sono. Nell'interfaccia sono magenta anche il titolo dell'avviso delle ricette, i nomi degli oggetti di
# rarita' epica e il viola scuro dei testi: gli scatti da misurare si fanno senza avvisi sullo schermo.
#
# Gli scatti sono quelli di client_shot.sh, 960x540. Uscite: 0 con una riga su stdout; 1 una misura non riuscita
# (convert) o un'uscita del selftest diversa dall'attesa; 2 uso sbagliato (anche una mano che non e' destra o sinistra,
# o un colore che non e' di sei cifre esadecimali), scatto che non si legge o non e' 960x540, rettangolo scritto male o
# fuori dallo scatto, programmi mancanti. Ogni errore stampa su stderr una riga "ERRORE: ...".
set -u
export LC_ALL=C

W=960
H=540
DIFF=4% # soglia della differenza su un canale
CROSSHAIR=60x60+450+240
HOTBAR=380x100+290+440
# Il gruppo dell'HUD della tuta a 960x540 con la scala 2 (design M4 par. 5.12, client/HudLayout.group): in basso,
# nell'angolo opposto alla mano principale. HudLayoutTest.screenRectsAtNineSixtyMatchTheSequence lo tiene uguale al
# codice: un cambio della posizione dell'HUD (domanda 8 del design) passa di qui.
HUD_RIGHT=176x70+4+466
HUD_LEFT=176x70+780+466

usage() {
    echo "Uso: bash tools/client/arm_measure.sh sagoma <con.png> <senza.png> [destra|sinistra]" >&2
    echo "     bash tools/client/arm_measure.sh differenza <a.png> <b.png> [<LxA+X+Y>]" >&2
    echo "     bash tools/client/arm_measure.sh colore <scatto.png> <RRGGBB> [<LxA+X+Y>]" >&2
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

# Maschera dei pixel di uno scatto del colore dato (RRGGBB, gia' controllato): ogni canale ne dista al piu' DIFF.
colour_mask() { # scatto colore maschera
    local r=$((16#${2:0:2})) g=$((16#${2:2:2})) b=$((16#${2:4:2})) t="${DIFF%\%}/100"
    convert "$1" -alpha off \
        -fx "(abs(r-$r/255)<=$t && abs(g-$g/255)<=$t && abs(b-$b/255)<=$t) ? 1 : 0" -colorspace Gray -threshold 50% \
        "$3" 2>"$tmp/convert.err" || fail "ricerca del colore $2 in '$1' non riuscita: $(why)"
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

measure_sagoma() { # con senza [destra|sinistra]
    local n cover crosshair hotbar hud box hud_rect=$HUD_RIGHT
    [ "${3:-destra}" = sinistra ] && hud_rect=$HUD_LEFT
    check_shot "$1"
    check_shot "$2"
    diff_mask "$1" "$2" "$tmp/sagoma.png"
    n=$(count "$tmp/sagoma.png") || exit 1
    cover=$(awk -v n="$n" -v t=$((W * H)) 'BEGIN { printf "%.2f", n * 100 / t }')
    crosshair=$(count "$tmp/sagoma.png" "$CROSSHAIR") || exit 1
    hotbar=$(count "$tmp/sagoma.png" "$HOTBAR") || exit 1
    hud=$(count "$tmp/sagoma.png" "$hud_rect") || exit 1
    box=$(bbox "$tmp/sagoma.png" "$n") || exit 1
    echo "pixel=$n copertura=$cover mirino=$crosshair barra=$hotbar hud=$hud riquadro=$box"
}

measure_differenza() { # a b [rettangolo]
    local n box
    check_shot "$1"
    check_shot "$2"
    [ "$#" -lt 3 ] || check_rect "$3"
    diff_mask "$1" "$2" "$tmp/differenza.png"
    n=$(count "$tmp/differenza.png" "${@:3}") || exit 1
    box=$(bbox "$tmp/differenza.png" "$n" "${@:3}") || exit 1
    echo "pixel=$n riquadro=$box"
}

measure_colore() { # scatto colore [rettangolo]
    local n box
    check_shot "$1"
    [[ "$2" =~ ^[0-9A-Fa-f]{6}$ ]] \
        || fatal "colore '$2': si scrive con sei cifre esadecimali RRGGBB (per esempio 55FF55)"
    [ "$#" -lt 3 ] || check_rect "$3"
    colour_mask "$1" "$2" "$tmp/colore.png"
    n=$(count "$tmp/colore.png" "${@:3}") || exit 1
    box=$(bbox "$tmp/colore.png" "$n" "${@:3}") || exit 1
    echo "colore=$n riquadro=$box"
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
            -fill 'rgb(90,140,71)' -draw 'rectangle 10,470 109,499' \
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
    expect "pixel=29500 copertura=5.69 mirino=0 barra=0 hud=0 riquadro=210x170+690+370" \
        sagoma "$d/con.png" "$d/senza.png"
    # la differenza nel solo blu conta (dentro la barra e fuori), quella di 1/255 no (nell'HUD della mano destra)
    expect "pixel=34600 copertura=6.67 mirino=100 barra=3500 hud=0 riquadro=430x290+470+250" \
        sagoma "$d/dentro.png" "$d/senza.png"
    expect "pixel=0 copertura=0.00 mirino=0 barra=0 hud=0 riquadro=0x0+0+0" sagoma "$d/senza.png" "$d/senza.png"
    # senza la mano vale la destra; con la sinistra l'HUD e' in basso a destra, dove il magenta ne copre 120x70
    expect "pixel=29500 copertura=5.69 mirino=0 barra=0 hud=0 riquadro=210x170+690+370" \
        sagoma "$d/con.png" "$d/senza.png" destra
    expect "pixel=29500 copertura=5.69 mirino=0 barra=0 hud=8400 riquadro=210x170+690+370" \
        sagoma "$d/con.png" "$d/senza.png" sinistra
    expect "magenta=28000 riquadro=200x140+700+400" magenta "$d/con.png"
    expect "magenta=0 riquadro=0x0+0+0" magenta "$d/con.png" 480x540+0+0
    expect "magenta=5000 riquadro=100x50+750+420" magenta "$d/con.png" 100x50+750+420
    expect "sagoma=29500 uguali=15500" uguali "$d/con.png" "$d/altro.png" "$d/senza.png"
    expect "pixel=29500 riquadro=210x170+690+370" differenza "$d/con.png" "$d/senza.png"
    expect "pixel=5000 riquadro=100x50+750+420" differenza "$d/con.png" "$d/senza.png" 100x50+750+420
    # la differenza di 1/255 dentro l'HUD della mano destra non conta
    expect "pixel=0 riquadro=0x0+0+0" differenza "$d/dentro.png" "$d/senza.png" 176x70+4+466
    expect "colore=28000 riquadro=200x140+700+400" colore "$d/con.png" C800C8
    expect "colore=1500 riquadro=50x30+690+370" colore "$d/con.png" 8C8C96
    expect "colore=0 riquadro=0x0+0+0" colore "$d/con.png" C800C8 480x540+0+0
    [ "$failed" -eq 0 ] || fail "selftest: uscite diverse dalle attese"
    echo "OK arm_measure"
}

case "${1:-}" in
    sagoma)
        [ "$#" -eq 3 ] || [ "$#" -eq 4 ] \
            || { usage; fatal "sagoma vuole due scatti e, se serve, la mano: <con.png> <senza.png> [destra|sinistra]"; }
        [ "$#" -eq 3 ] || [ "$4" = destra ] || [ "$4" = sinistra ] \
            || { usage; fatal "sagoma: la mano e' destra o sinistra, non '$4'"; }
        measure_sagoma "${@:2}" ;;
    differenza)
        [ "$#" -eq 3 ] || [ "$#" -eq 4 ] || { usage; fatal "differenza vuole due scatti e, se serve, un rettangolo"; }
        measure_differenza "${@:2}" ;;
    colore)
        [ "$#" -eq 3 ] || [ "$#" -eq 4 ] \
            || { usage; fatal "colore vuole uno scatto, un colore RRGGBB e, se serve, un rettangolo"; }
        measure_colore "${@:2}" ;;
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
        fatal "misura sconosciuta '${1:-}' (sagoma, differenza, colore, magenta, uguali, --selftest)" ;;
esac
