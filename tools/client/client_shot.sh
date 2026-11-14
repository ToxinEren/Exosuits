#!/usr/bin/env bash
# Scatti in terza persona dal client vero di Minecraft, senza schermo. Solo Linux (Xvfb e OpenGL software di Mesa):
# e' per la sessione cloud e per macchine simili alla CI, non per il PC Windows.
#
#   EXO_ACCEPT_EULA=true bash tools/client/client_shot.sh <variante> <cartella> ["comando" | "@passo" ...]
#
# Avvia un server dedicato locale in modalita' offline (con RCON) e il client di sviluppo della variante sotto Xvfb,
# fa entrare il giocatore, lo mette in creativa su un mondo piatto, di giorno, rivolto a sud, esegue i comandi via
# RCON (senza la barra iniziale; il giocatore si chiama come EXO_SHOT_USER, di default Exo) e salva nella cartella
# back.png (da dietro), front.png (davanti), quarter.png (da dietro a tre quarti) con l'interfaccia nascosta, piu'
# i log: gradle-server.log e gradle-client.log (uscita di Gradle e del gioco), server.log e client.log (latest.log
# delle due cartelle di esecuzione), rcon.log (comandi e risposte), xvfb.log (uscita di Xvfb), log-check.txt
# (avvisi ed errori del mod o dei mixin nei due log, vuoto se non ce ne sono). Prima di cominciare cancella dalla
# cartella solo questi file, per nome (e gli scatti dei passi @scatto). Alla fine chiude server, client e Xvfb, anche
# dopo un errore o Ctrl-C.
#
# Fra i comandi possono stare passi del client, che cominciano con @ e si eseguono nello stesso ordine:
#   @click destro|sinistro             un clic del mouse dove punta il mirino
#   @tieni <tasto> <secondi> [<nome> <dopo>]
#                                      tiene premuto un tasto (nome di xdotool: v, F5, space...) per quei secondi;
#                                      con <nome> e <dopo> salva <nome>.png <dopo> secondi dopo la pressione, a tasto
#                                      ancora premuto (<dopo> minore di <secondi>)
#   @scatto <nome>                     salva <nome>.png subito, con l'interfaccia com'e' in quel momento
#   @attendi <secondi>                 aspetta
# I secondi possono avere decimali (0.5). Un passo sconosciuto o con argomenti sbagliati ferma lo script prima di
# avviare qualunque cosa (uscita 2); un passo che non si riesce a eseguire lo ferma con uscita 1. Dove guarda il
# giocatore e cosa tiene in mano si decide con comandi del server (tp, item replace) fra un passo e l'altro.
#
# Lavora sul checkout in cui si trova, in cartelle di esecuzione proprie (versions/<variante>/run-shot-client e
# run-shot-server, ignorate da git), senza toccare run/ e run-gametest/. Il mondo del server si ricrea ogni volta.
# Una sola esecuzione per variante alla volta (lucchetto sulla cartella del server); esecuzioni su varianti diverse
# in parallelo vogliono porte diverse (EXO_SHOT_PORT).
#
# EULA: il server di prova parte solo con EXO_ACCEPT_EULA=true, che significa accettare l'EULA di Minecraft
# (https://aka.ms/MinecraftEULA) per quel server locale; lo script scrive eula=true solo nella propria cartella.
#
# Variabili d'ambiente:
#   EXO_ACCEPT_EULA   true per accettare l'EULA del server di prova (obbligatoria)
#   EXO_SHOT_PORT     porta del server, default 25601; RCON usa la porta + 10
#   EXO_SHOT_W        larghezza della finestra, default 960
#   EXO_SHOT_H        altezza della finestra, default 540
#   EXO_SHOT_USER     nome del giocatore offline, default Exo
#   EXO_SHOT_SETTLE   secondi di attesa dopo i comandi, prima degli scatti, default 8
#
# Uscite: 0 scatti salvati; 1 errore durante l'esecuzione (build, avvio, ingresso, comando rifiutato, passo non
# riuscito, scatto); 2 uso o ambiente sbagliati (variante, passo sconosciuto o scritto male, tasto sconosciuto,
# programmi mancanti, EULA, porta occupata, Xvfb che non parte, OpenGL core 3.2 assente); 130 Ctrl-C; 143 TERM. Ogni errore stampa su stderr una riga "ERRORE: ..." che dice cosa non e' andato. Istruzioni complete in tools/client/README.md.
set -u

here=$(cd "$(dirname "$0")" && pwd)
root=$(cd "$here/../.." && pwd)
variants=(1.20.1-forge 1.20.1-fabric 1.21.1-fabric 1.21.1-neoforge)

usage() {
    echo "Uso: EXO_ACCEPT_EULA=true bash tools/client/client_shot.sh <variante> <cartella> [\"comando\" | \"@passo\" ...]" >&2
    echo "Varianti: ${variants[*]}" >&2
}
fatal() { echo "ERRORE: $*" >&2; exit 2; }

if [ "$#" -lt 2 ]; then
    usage
    exit 2
fi
variant=$1
out=$2
shift 2

known=0
for v in "${variants[@]}"; do [ "$v" = "$variant" ] && known=1; done
if [ "$known" -eq 0 ]; then
    usage
    fatal "variante sconosciuta '$variant'"
fi

# --- passi del client ----------------------------------------------------------------------------------------------
# Tutti controllati prima di avviare qualunque cosa: un passo sbagliato non costa un avvio di server e client.
is_seconds() { [[ "$1" =~ ^[0-9]+(\.[0-9]+)?$ ]] && awk -v s="$1" 'BEGIN { exit !(s > 0) }'; }
is_name() { [[ "$1" =~ ^[A-Za-z0-9][A-Za-z0-9_-]*$ ]]; }
reserved=(back front quarter)
step_names=() # scatti dei passi, in ordine
step_keys=()  # tasti dei passi @tieni, da provare quando c'e' lo schermo virtuale
add_shot_name() { # nome passo
    local r
    is_name "$1" || fatal "passo \"$2\": nome di scatto non valido '$1' (lettere, cifre, _ e -)"
    for r in "${reserved[@]}" "${step_names[@]}"; do
        [ "$r" = "$1" ] && fatal "passo \"$2\": lo scatto '$1' esiste gia' (o e' uno dei tre scatti finali)"
    done
    step_names+=("$1")
}
for c in "$@"; do
    case "$c" in @*) ;; *) continue ;; esac
    read -r -a w <<<"$c"
    case "${w[0]}" in
        @click)
            [ "${#w[@]}" -eq 2 ] && { [ "${w[1]}" = destro ] || [ "${w[1]}" = sinistro ]; } \
                || fatal "passo \"$c\": si scrive @click destro oppure @click sinistro" ;;
        @tieni)
            { [ "${#w[@]}" -eq 3 ] || [ "${#w[@]}" -eq 5 ]; } \
                || fatal "passo \"$c\": si scrive @tieni <tasto> <secondi> [<nome> <dopo>]"
            [[ "${w[1]}" =~ ^[A-Za-z0-9_]+$ ]] || fatal "passo \"$c\": tasto non valido '${w[1]}'"
            is_seconds "${w[2]}" || fatal "passo \"$c\": secondi non validi '${w[2]}' (numero maggiore di 0)"
            if [ "${#w[@]}" -eq 5 ]; then
                add_shot_name "${w[3]}" "$c"
                is_seconds "${w[4]}" && awk -v a="${w[4]}" -v b="${w[2]}" 'BEGIN { exit !(a < b) }' \
                    || fatal "passo \"$c\": lo scatto va fatto prima di lasciare il tasto (<dopo> fra 0 e ${w[2]})"
            fi
            step_keys+=("${w[1]}") ;;
        @scatto)
            [ "${#w[@]}" -eq 2 ] || fatal "passo \"$c\": si scrive @scatto <nome>"
            add_shot_name "${w[1]}" "$c" ;;
        @attendi)
            [ "${#w[@]}" -eq 2 ] && is_seconds "${w[1]}" \
                || fatal "passo \"$c\": si scrive @attendi <secondi> (numero maggiore di 0)" ;;
        *) fatal "passo sconosciuto \"$c\" (passi: @click, @tieni, @scatto, @attendi; vedi tools/client/README.md)" ;;
    esac
done

# --- ambiente -------------------------------------------------------------------------------------------------------
[ "$(uname -s)" = Linux ] || fatal "lo strumento funziona solo su Linux (Xvfb); sul PC Windows segui la checklist"

missing=()
for prog in Xvfb xdotool import identify convert glxinfo python3 java flock; do
    command -v "$prog" >/dev/null 2>&1 || missing+=("$prog")
done
if [ "${#missing[@]}" -gt 0 ]; then
    fatal "programmi mancanti: ${missing[*]} (su Debian/Ubuntu: apt-get install -y xvfb xdotool imagemagick" \
          "mesa-utils libgl1-mesa-dri, piu' un JDK 21; vedi tools/client/README.md)"
fi

if [ "${EXO_ACCEPT_EULA:-}" != true ]; then
    echo "ERRORE: il server di prova richiede di accettare l'EULA di Minecraft (https://aka.ms/MinecraftEULA)." >&2
    echo "Impostare EXO_ACCEPT_EULA=true significa accettarla per il server locale che questo script avvia:" >&2
    echo "lo script scrive allora eula=true in versions/$variant/run-shot-server/eula.txt e in nessun altro posto." >&2
    exit 2
fi

user=${EXO_SHOT_USER:-Exo}
port=${EXO_SHOT_PORT:-25601}
width=${EXO_SHOT_W:-960}
height=${EXO_SHOT_H:-540}
settle=${EXO_SHOT_SETTLE:-8}
for n in "$port" "$width" "$height" "$settle"; do
    case "$n" in '' | *[!0-9]*) fatal "EXO_SHOT_PORT, EXO_SHOT_W, EXO_SHOT_H ed EXO_SHOT_SETTLE vogliono numeri interi" ;; esac
done
case "$user" in '' | *[!A-Za-z0-9_]*) fatal "EXO_SHOT_USER: solo lettere, cifre e _ (nome di giocatore valido)" ;; esac
rport=$((port + 10))
# Password RCON nuova a ogni esecuzione: non si stampa, non va sulla riga di comando (rcon.py la legge dalla variabile
# d'ambiente EXO_RCON_PASSWORD) e resta solo in server.properties, nella cartella del server, con permessi 600.
rpass=$(python3 -c 'import secrets; print(secrets.token_hex(12))') || fatal "non riesco a generare la password RCON"
tag="exoshot$$"

port_busy() { python3 -c 'import socket,sys; s=socket.socket(); s.settimeout(1); sys.exit(0 if s.connect_ex(("127.0.0.1", int(sys.argv[1]))) == 0 else 1)' "$1"; }
for p in "$port" "$rport"; do
    port_busy "$p" && fatal "la porta $p e' gia' in uso (un altro server?): scegli un'altra EXO_SHOT_PORT"
done

vdir="$root/versions/$variant"
sdir="$vdir/run-shot-server"
cdir="$vdir/run-shot-client"
mkdir -p "$sdir" "$cdir" "$out" || fatal "non riesco a creare le cartelle"
out=$(cd "$out" && pwd)

exec 9>"$sdir/.lock"
flock -n 9 || fatal "un'altra esecuzione di client_shot.sh usa gia' la variante $variant"

# Si tolgono solo i file che lo script scrive, per nome: la cartella e' dell'utente e puo' contenere altro.
for f in back.png front.png quarter.png gradle-server.log gradle-client.log server.log client.log rcon.log xvfb.log \
        log-check.txt "${step_names[@]/%/.png}"; do
    rm -f "$out/$f"
done

t0=$(date +%s)
log() { echo "[$(($(date +%s) - t0))s] $*"; }

# --- cartelle di esecuzione -----------------------------------------------------------------------------------------
rm -rf "$sdir/world"
echo "eula=true" >"$sdir/eula.txt"
# Server e RCON ascoltano solo su 127.0.0.1 (server-ip vale per entrambi): l'account offline ha op e RCON esegue
# qualunque comando, quindi nessuno da fuori deve poterli raggiungere.
rm -f "$sdir/server.properties"
(umask 077 && cat >"$sdir/server.properties") <<EOF
server-ip=127.0.0.1
online-mode=false
enforce-secure-profile=false
level-type=minecraft\:flat
generate-structures=false
difficulty=peaceful
spawn-monsters=false
spawn-animals=false
spawn-npcs=false
spawn-protection=0
view-distance=4
simulation-distance=4
server-port=$port
enable-rcon=true
rcon.port=$rport
rcon.password=$rpass
broadcast-rcon-to-ops=false
motd=exo-shot
EOF
# options.txt: niente schermate di benvenuto, narratore o tutorial, nessuna pausa senza fuoco, grafica "Dettagliata"
# (graphicsMode:1; 0 sarebbe "Rapida"), distanza di rendering 4 e 30 fps per il rendering software, mouse non grezzo
# (xdotool muove il puntatore per lo scatto a tre quarti).
cat >"$cdir/options.txt" <<EOF
onboardAccessibility:false
narrator:0
tutorialStep:none
pauseOnLostFocus:false
skipMultiplayerWarning:true
joinedFirstServer:true
renderDistance:4
simulationDistance:5
maxFps:30
enableVsync:false
graphicsMode:1
renderClouds:"false"
guiScale:2
fov:0.0
soundCategory_master:0.0
rawMouseInput:false
EOF

# --- chiusura -------------------------------------------------------------------------------------------------------
xpid=""
held_key="" # il tasto che un passo @tieni tiene premuto in questo momento
spid=""
cpid=""
# Chiusura, su ogni uscita: "stop" via RCON al server, TERM ai due processi Gradle dello script (il client di Gradle
# annulla la build nel demone, cosi' un server o un client non ancora partiti non partono piu') e alle JVM del gioco
# con il tag di questa esecuzione; dopo 30 s KILL a cio' che resta. Durante la chiusura un altro Ctrl-C non la
# interrompe: dura al massimo circa 35 s.
# shellcheck disable=SC2317 # chiamata dalla trap su EXIT
cleanup() {
    local code=$?
    trap - EXIT
    trap '' INT TERM
    # un tasto rimasto premuto da @tieni (scatto fallito o interruzione durante la pressione) si rilascia per primo,
    # con lo schermo virtuale ancora vivo: il client non lo riceve per tutta la chiusura
    if [ -n "$held_key" ] && [ -n "$xpid" ] && kill -0 "$xpid" 2>/dev/null; then
        xdotool keyup "$held_key" 2>/dev/null && echo "tasto $held_key rilasciato" >&2
        held_key=""
    fi
    if [ -n "$spid" ] && kill -0 "$spid" 2>/dev/null; then
        EXO_RCON_PASSWORD="$rpass" python3 "$here/rcon.py" --no-check "$rport" stop >/dev/null 2>&1
    fi
    local pids=()
    [ -n "$spid" ] && pids+=("$spid")
    [ -n "$cpid" ] && pids+=("$cpid")
    [ "${#pids[@]}" -gt 0 ] && kill -TERM "${pids[@]}" 2>/dev/null
    pkill -TERM -f -- "-Dexo.shot.tag=$tag-client" 2>/dev/null
    running() {
        pgrep -f -- "-Dexo.shot.tag=$tag-" >/dev/null && return 0
        local p
        for p in "${pids[@]}"; do kill -0 "$p" 2>/dev/null && return 0; done
        return 1
    }
    for _ in $(seq 30); do
        running || break
        sleep 1
    done
    if running; then
        echo "client, server o Gradle non si chiudono da soli: li termino" >&2
        [ "${#pids[@]}" -gt 0 ] && kill -KILL "${pids[@]}" 2>/dev/null
        pkill -KILL -f -- "-Dexo.shot.tag=$tag-" 2>/dev/null
        sleep 2
        # Un processo del gioco partito dal demone proprio mentre il client di Gradle moriva.
        pkill -KILL -f -- "-Dexo.shot.tag=$tag-" 2>/dev/null
    fi
    [ "${#pids[@]}" -gt 0 ] && wait "${pids[@]}" 2>/dev/null
    [ -n "$xpid" ] && kill "$xpid" 2>/dev/null && wait "$xpid" 2>/dev/null
    # solo i log dei processi di questa esecuzione, non quelli rimasti da una precedente
    [ -n "$spid" ] && cp "$sdir/logs/latest.log" "$out/server.log" 2>/dev/null
    [ -n "$cpid" ] && cp "$cdir/logs/latest.log" "$out/client.log" 2>/dev/null
    exit "$code"
}
trap cleanup EXIT
trap 'echo "interrotto (Ctrl-C), chiusura in corso" >&2; exit 130' INT
trap 'echo "terminato (TERM), chiusura in corso" >&2; exit 143' TERM

fail() { log "ERRORE: $*" >&2; exit 1; }

# --- schermo virtuale -----------------------------------------------------------------------------------------------
# Il numero dello schermo lo sceglie Xvfb (-displayfd), che prende il primo libero con il proprio lucchetto: due
# esecuzioni partite nello stesso istante non finiscono sullo stesso schermo. Lo scrive sul descrittore 3, in un file
# della cartella del server, quando accetta connessioni.
dfile="$sdir/.xvfb-display"
rm -f "$dfile"
Xvfb -displayfd 3 -screen 0 "${width}x${height}x24" -nolisten tcp 3>"$dfile" >"$out/xvfb.log" 2>&1 &
xpid=$!
for _ in $(seq 20); do
    [ -s "$dfile" ] && break
    kill -0 "$xpid" 2>/dev/null || break
    sleep 0.5
done
n=$(head -n 1 "$dfile" 2>/dev/null | tr -cd '0-9')
[ -n "$n" ] && kill -0 "$xpid" 2>/dev/null || fatal "Xvfb non parte (vedi $out/xvfb.log)"
export DISPLAY=":$n"
xdotool getdisplaygeometry >/dev/null 2>&1 || fatal "lo schermo :$n di Xvfb non risponde (vedi $out/xvfb.log)"
# Lo schermo deve essere ancora quello di questa esecuzione: il suo Xvfb vivo.
kill -0 "$xpid" 2>/dev/null || fatal "Xvfb si e' fermato appena partito (vedi $out/xvfb.log)"
glx=$(glxinfo -B 2>&1)
core=$(echo "$glx" | sed -n 's/.*Max core profile version: *\([0-9]*\.[0-9]*\).*/\1/p' | head -n 1)
case "$core" in
    3.[2-9]* | [4-9].*) ;;
    *) fatal "OpenGL core 3.2 non disponibile sotto Xvfb (serve Mesa con llvmpipe, pacchetto libgl1-mesa-dri);" \
            "glxinfo dice: $(echo "$glx" | grep -m1 -E 'renderer|Error|error' || echo niente)" ;;
esac
log "schermo :$n ${width}x${height}, $(echo "$glx" | sed -n 's/.*OpenGL renderer string: *//p' | head -n 1)"
# I tasti dei passi @tieni: xdotool non fallisce con un nome sconosciuto (avvisa ed esce con 0), quindi si provano qui,
# sullo schermo ancora senza finestre, dove premerli non arriva a nessuno.
for k in "${step_keys[@]}"; do
    err=$({ xdotool keydown "$k" && xdotool keyup "$k"; } 2>&1)
    [ -z "$err" ] || fatal "tasto sconosciuto per xdotool: '$k' ($(echo "$err" | head -n 1))"
done

gradle=(./gradlew --console=plain -I "$here/shot.init.gradle" "-Pexo.shot.tag=$tag" "-Pexo.shot.user=$user"
        "-Pexo.shot.server=127.0.0.1:$port" "-Pexo.shot.width=$width" "-Pexo.shot.height=$height")

# Attende una riga nel log di Gradle; esce con errore se la build fallisce, il processo muore o scade il tempo.
wait_for() { # log regex secondi pid descrizione
    for _ in $(seq "$3"); do
        grep -qE "$2" "$1" 2>/dev/null && return 0
        if grep -q 'BUILD FAILED' "$1" 2>/dev/null || ! kill -0 "$4" 2>/dev/null; then
            tail -n 30 "$1" >&2
            fail "$5: Gradle o il gioco si sono fermati prima (vedi $1)"
        fi
        sleep 1
    done
    tail -n 30 "$1" >&2
    fail "$5: nessun segno dopo $3 s (vedi $1)"
}

# --- server ---------------------------------------------------------------------------------------------------------
log "avvio del server $variant sulla porta $port"
(cd "$root" && exec "${gradle[@]}" ":$variant:runServer") >"$out/gradle-server.log" 2>&1 &
spid=$!
wait_for "$out/gradle-server.log" 'RCON running' 900 "$spid" "il server non e' partito"
log "server pronto"

# --- client ---------------------------------------------------------------------------------------------------------
log "avvio del client"
(cd "$root" && exec "${gradle[@]}" ":$variant:runClient") >"$out/gradle-client.log" 2>&1 &
cpid=$!
joined=0
for _ in $(seq 900); do
    if grep -q "$user joined the game" "$out/gradle-server.log"; then joined=1; break; fi
    if grep -qE "$user lost connection|Disconnecting $user" "$out/gradle-server.log"; then
        grep -m1 -E "$user lost connection|Disconnecting $user" "$out/gradle-server.log" >&2
        fail "il giocatore e' stato respinto dal server"
    fi
    if grep -q 'BUILD FAILED' "$out/gradle-client.log" || ! kill -0 "$cpid" 2>/dev/null; then
        grep -m5 -E 'Exception|Error:|FAILED' "$out/gradle-client.log" >&2
        fail "il client si e' fermato prima di entrare nel mondo (vedi $out/gradle-client.log)"
    fi
    kill -0 "$spid" 2>/dev/null || fail "il server si e' fermato (vedi $out/gradle-server.log)"
    sleep 1
done
[ "$joined" -eq 1 ] || fail "il giocatore $user non e' entrato nel mondo entro 900 s (vedi $out/gradle-client.log)"
log "giocatore entrato"

rc() { EXO_RCON_PASSWORD="$rpass" python3 "$here/rcon.py" "$rport" "$@" >>"$out/rcon.log"; }
# Un comando dell'utente si considera riuscito solo se "execute store success" lo registra come riuscito: un errore
# dentro "execute as ... run" (per esempio un colore non valido di /exoframe) non arriva nella risposta RCON, che
# resta vuota. Gli errori di sintassi invece arrivano nella risposta e li ferma rcon.py.
checked() {
    local reply
    rc "scoreboard players set #exo exo_shot -1" "execute store success score #exo exo_shot run $1" || return 1
    reply=$(EXO_RCON_PASSWORD="$rpass" python3 "$here/rcon.py" "$rport" "scoreboard players get #exo exo_shot") \
        || return 1
    echo "$reply" >>"$out/rcon.log"
    case "$reply" in *"#exo has 1 "*) return 0 ;; esac
    return 1
}
rc "op $user" "gamemode creative $user" "time set 6000" "weather clear" "gamerule doDaylightCycle false" \
    "gamerule doWeatherCycle false" "tp $user 0.5 -60 0.5 0 15" "scoreboard objectives add exo_shot dummy" \
    || fail "preparazione del mondo: comando rifiutato (vedi $out/rcon.log)"
# Corpo allineato alla testa. /tp gira la testa ma non il corpo, e il client crea il giocatore con un orientamento
# del corpo a caso (su 1.20.1 si vede): fermo, il corpo resta indietro fino a 50 gradi rispetto alla testa e gli
# scatti "davanti" e "dietro" vengono di tre quarti. Camminando il corpo si gira nella direzione del passo: un passo
# in avanti (W per 0,8 s), poi di nuovo al punto di partenza.
sleep 5 # il client finisce di caricare il terreno attorno al giocatore
xdotool keydown w
sleep 0.8
xdotool keyup w
sleep 0.5
rc "tp $user 0.5 -60 0.5 0 15" || fail "ritorno al punto di partenza: comando rifiutato (vedi $out/rcon.log)"
# Prima di ogni scatto e di ogni passo: il client deve essere ancora vivo e nel mondo (mai uno scatto del menu
# principale, mai un tasto mandato a nessuno).
alive() {
    kill -0 "$cpid" 2>/dev/null || fail "il client si e' chiuso prima di $1 (vedi $out/gradle-client.log)"
    if grep -qE "$user lost connection|$user left the game" "$out/gradle-server.log"; then
        fail "il giocatore ha lasciato il server prima di $1 (vedi $out/gradle-server.log)"
    fi
}
# Uno scatto subito, senza attese: chi vuole aspettare lo dice (sleep qui sotto, @attendi nei passi).
shot() {
    alive "lo scatto $1"
    import -window root "$out/$1.png" || fail "import non ha salvato $1.png"
    local colours
    colours=$(identify -format '%k' "$out/$1.png" 2>/dev/null || echo 0)
    [ "$colours" -ge 64 ] || fail "$1.png ha solo $colours colori: schermo vuoto o nero, non uno scatto del mondo"
    log "salvato $1.png"
}
# xdotool, con l'errore che diventa un errore del passo (xdotool avvisa ma esce con 0 su certi sbagli).
xdo() { # passo argomenti...
    local step=$1 err
    shift
    err=$(xdotool "$@" 2>&1) && [ -z "$err" ] || fail "passo \"$step\": xdotool $* non riuscito: $err"
}
now() { date +%s.%N; }
# Un passo del client, gia' controllato all'inizio.
run_step() {
    local w t0s rest
    read -r -a w <<<"$1"
    alive "il passo \"$1\""
    case "${w[0]}" in
        @click) xdo "$1" click "$([ "${w[1]}" = destro ] && echo 3 || echo 1)" ;;
        @tieni)
            t0s=$(now)
            held_key=${w[1]}
            xdo "$1" keydown "${w[1]}"
            if [ "${#w[@]}" -eq 5 ]; then
                sleep "${w[4]}"
                shot "${w[3]}"
            fi
            rest=$(awk -v d="${w[2]}" -v a="$t0s" -v b="$(now)" 'BEGIN { r = d - (b - a); print (r > 0 ? r : 0) }')
            sleep "$rest"
            xdo "$1" keyup "${w[1]}"
            held_key="" ;;
        @scatto) shot "${w[1]}" ;;
        @attendi) sleep "${w[1]}" ;;
    esac
}
for c in "$@"; do
    case "$c" in
        @*)
            run_step "$c"
            log "passo eseguito: $c" ;;
        *)
            checked "${c#/}" || fail "comando rifiutato o non riuscito: \"$c\" (risposta in $out/rcon.log)"
            log "comando eseguito: $c" ;;
    esac
done
sleep "$settle" # i chunk si costruiscono piano col rendering software, e il corpo si gira dietro la testa

# I tre scatti finali partono dalla prima persona con l'interfaccia visibile: chi con i passi lascia la telecamera in
# terza persona (F5) o l'interfaccia nascosta (F1) li cambia.
key() { xdotool key --clearmodifiers "$1"; sleep 1.5; }
key F1 # nasconde l'interfaccia
key F5
sleep 2
shot back # terza persona, da dietro
key F5
sleep 2
shot front # terza persona, davanti
# Tre quarti: /tp gira il corpo con la testa, quindi si gira solo la testa con il mouse (circa 0,15 gradi per
# pixel); il corpo la segue in ritardo di 15-50 gradi. Non e' una vista laterale vera.
key F5 # prima persona
key F5 # di nuovo da dietro
for _ in $(seq 12); do xdotool mousemove_relative -- 60 0; sleep 0.15; done
sleep 2
shot quarter

# Log del client e del server: righe WARN/ERROR che nominano il mod o i mixin, eccezioni e tracce che passano dal
# codice del mod, errori di applicazione dei mixin. Sono da guardare, non fermano lo script. Escluse due righe note e
# innocue dello sviluppo: GeckoLib non trova il proprio refmap ("If this is a development environment you can ignore
# this message") e, su NeoForge, JarJar che trova mixinextras gia' passato come libreria.
{
    grep -HE '/(WARN|ERROR)\]' "$out/gradle-client.log" "$out/gradle-server.log" \
        | grep -iE 'redstone_exoframes|redstone exoframes|exoframes|mixin'
    grep -HE 'com\.toxineren|InvalidInjectionException|MixinApplyError|Mixin apply .*failed' \
        "$out/gradle-client.log" "$out/gradle-server.log"
} | grep -v -e 'If this is a development environment you can ignore this message' \
    -e 'Attempted to select a dependency jar for JarJar which was passed in as source' | sort -u >"$out/log-check.txt"
if [ -s "$out/log-check.txt" ]; then
    log "ATTENZIONE: $(wc -l <"$out/log-check.txt") righe del mod o dei mixin da guardare nei log: $out/log-check.txt"
else
    log "log: nessun avviso o errore del mod o dei mixin"
fi
log "fatto in $(($(date +%s) - t0)) s, chiusura"
exit 0
