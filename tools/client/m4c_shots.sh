#!/usr/bin/env bash
# La sequenza della parte 4c ("l'HUD") nel client vero: le esecuzioni di client_shot.sh qui sotto e poi i controlli
# che lo script fa da se'. Per ora (Task 2 del piano della 4c) prova i due tasti dell'HUD e il file delle preferenze
# del client, che client_shot.sh copia accanto agli scatti; l'esecuzione B (il mancino), le fasi che fotografano l'HUD
# e i controlli sugli scatti con arm_measure.sh arrivano con l'HUD (Task 3-5).
#
#   EXO_ACCEPT_EULA=true bash tools/client/m4c_shots.sh <variante> <cartella>
#
# Il giocatore resta in creativa in 0,5 -60 0,5 rivolto a sud con l'inclinazione 15, senza la tuta: i due tasti
# valgono anche senza (Decisione 6 del piano). H, il tasto che nasconde l'HUD, sta al suo predefinito; J e' il tasto
# della vignetta, che la mod lascia senza tasto e che client_shot.sh assegna con la riga
# key_key.redstone_exoframes.vignette di options.txt: la sequenza prova i due modi in cui un tasto della mod prende il
# suo tasto, il predefinito e la riga delle opzioni letta all'avvio del client. Un messaggio sulla barra delle azioni
# resta 3 s: lo scatto che lo mostra viene un secondo dopo il tasto, e dopo lo scatto si aspettano 3 s, cosi' il
# messaggio se n'e' andato prima del passo dopo.
#
# Esecuzione A, mano principale a destra, dalle preferenze predefinite (HUD visibile, vignetta accesa), scatti in
# <cartella>:
#   1. attesa  "@attendi 12": la riga in chat del "gamemode creative" di client_shot.sh e l'avviso "Chat messages
#              can't be verified" spariscono (vedi tools/client/README.md, regole degli scatti misurati)
#   2. chat    T apre la chat, una h scritta nella chat, Escape la chiude senza mandarla: con una schermata aperta il
#              gioco base non registra il clic di H (KeyboardHandler.keyPress chiama KeyMapping.click solo senza
#              schermate), quindi le preferenze non cambiano; lo vede alla fine il controllo chat-e-tasti, perche' con
#              quella h contata l'HUD tornerebbe visibile
#   3. fine    J spegne la vignetta: scatto messaggio-vignetta un secondo dopo, da guardare: "Helmet vignette off"
#              sulla barra delle azioni; H nasconde l'HUD: scatto messaggio-nascosto un secondo dopo, da guardare:
#              "Exoframe HUD hidden: H to show it"; poi tp in 0,5 -60 -5,5 per i tre scatti di client_shot.sh. Il
#              client riscrive il file delle preferenze a ogni tasto, e client_shot.sh lo copia in
#              <cartella>/redstone_exoframes-client.json
# Esecuzione C, il riavvio: un client nuovo, mano destra, che parte dal file scritto dal client nella A
# (EXO_SHOT_CLIENT_PREFS=<cartella>/redstone_exoframes-client.json), scatti in <cartella>/riavvio: "@attendi 12", poi
# H mostra l'HUD che il file teneva nascosto: scatto riavvio-messaggio un secondo dopo, da guardare: "Exoframe HUD
# shown"; poi tp in 0,5 -60 -5,5. La C parte solo se la A e' uscita con 0 e ha lasciato il file. Si prova cosi' la
# catena vera (tasto, scrittura, lettura all'avvio), non la lettura di un file scritto a mano (Decisione 10 del piano).
#
# Controlli, sui file delle preferenze che client_shot.sh copia, letti con il modulo json di Python e scritti come
# "<hudHidden> <vignette>":
#   chat-e-tasti  <cartella>/redstone_exoframes-client.json vale "True False": H e J hanno cambiato le preferenze e la
#                 h scritta nella chat no (contata, l'HUD sarebbe di nuovo visibile: "False False"; con J senza tasto
#                 la vignetta resterebbe accesa: "True True")
#   riavvio-file  <cartella>/riavvio/redstone_exoframes-client.json vale "False False": il client e' ripartito dal file
#                 della A (dai predefiniti H avrebbe nascosto l'HUD, con la vignetta accesa: "True True")
#
# Non prova: il jar di produzione (il client gira nell'ambiente di sviluppo); l'HUD e la vignetta, che arrivano con i
# Task 3 e 4; H con l'inventario aperto, F3+H, il tasto della vignetta assegnato nelle impostazioni dei controlli, il
# file delle preferenze rovinato, di una versione piu' nuova o in una cartella di sola lettura (li provano
# ClientPrefsFileTest e la checklist per il PC della parte).
#
# Dura circa tre minuti a cache calde (misurati 158-193 s sulle quattro varianti: 193 s su 1.20.1-forge, 158 s su
# 1.20.1-fabric, 159 s su 1.21.1-fabric, 170 s su 1.21.1-neoforge; 82-102 s l'esecuzione A, 71-84 s la C).
#
# Uscita: una riga "ok <nome>: <misure>" o "KO <nome>: <misure> (atteso ...)" per controllo, tutti valutati anche dopo
# un fallimento; l'ultima riga e' "OK <variante>: scatti in <cartella>" (uscita 0) se le due esecuzioni sono uscite con
# 0 e ogni controllo e' ok, altrimenti "FAIL <variante>: ..." su stderr (uscita 1). Se l'esecuzione A fallisce, o non
# lascia il file delle preferenze, la C non parte; i controlli girano solo se ci sono tutti gli scatti. Con
# M4C_SOLO_CONTROLLI=1 lo script salta le esecuzioni e rifa' i controlli su cio' che la cartella ha gia'. Uscita 2: uso
# sbagliato. Le variabili d'ambiente sono quelle di client_shot.sh (EXO_ACCEPT_EULA obbligatoria); EXO_SHOT_MAIN_HAND
# ed EXO_SHOT_CLIENT_PREFS le imposta questo script per ogni esecuzione, qualunque valore abbiano nell'ambiente di chi
# lo chiama. Istruzioni in tools/client/README.md.
set -u
# La config predefinita: una EXO_SHOT_SERVER_CONFIG ereditata dall'ambiente cambierebbe le regole del server.
unset EXO_SHOT_SERVER_CONFIG

here=$(cd "$(dirname "$0")" && pwd)

if [ "$#" -ne 2 ]; then
    echo "Uso: EXO_ACCEPT_EULA=true bash tools/client/m4c_shots.sh <variante> <cartella>" >&2
    exit 2
fi
variant=$1
out=$2
u=${EXO_SHOT_USER:-Exo}

# --- passi ----------------------------------------------------------------------------------------------------------
# Il primo passo di ogni esecuzione: vedi il commento di testa.
FIRST_WAIT=12
# Il file delle preferenze del client che client_shot.sh copia accanto agli scatti.
PREFS=redstone_exoframes-client.json
# All'aperto, per i tre scatti finali di client_shot.sh.
AWAY="tp $u 0.5 -60 -5.5 0 15"

steps_a=(
    # 1. attesa
    "@attendi $FIRST_WAIT"
    # 2. chat: una h scritta nella chat non e' un clic del tasto H
    "@tieni t 0.2" "@attendi 1" "@tieni h 0.2" "@tieni Escape 0.2" "@attendi 1"
    # 3. fine: J spegne la vignetta e H nasconde l'HUD, ciascuno con il suo messaggio sulla barra delle azioni
    "@tieni j 0.2" "@attendi 1" "@scatto messaggio-vignetta" "@attendi 3"
    "@tieni h 0.2" "@attendi 1" "@scatto messaggio-nascosto" "@attendi 3"
    "$AWAY"
)
steps_c=(
    "@attendi $FIRST_WAIT"
    # H mostra l'HUD che il file della A teneva nascosto
    "@tieni h 0.2" "@attendi 1" "@scatto riavvio-messaggio" "@attendi 3"
    "$AWAY"
)
shots_a=(messaggio-vignetta messaggio-nascosto)
shots_c=(riavvio-messaggio)

# --- le esecuzioni --------------------------------------------------------------------------------------------------
problems=() # le ragioni di un FAIL, nell'ordine in cui si incontrano
if [ "${M4C_SOLO_CONTROLLI:-}" != 1 ]; then
    echo "=== $variant m4c"
    # un'uscita anticipata di client_shot.sh (uso o ambiente sbagliati) non lascia leggere gli avvisi, gli scatti o il
    # file delle preferenze di un'altra esecuzione, e se la C non parte i controlli non girano su quelli di prima
    rm -f "$out/log-check.txt" "$out/riavvio/log-check.txt" "$out/$PREFS" "$out/riavvio/$PREFS"
    for s in "${shots_a[@]}"; do rm -f "$out/$s.png"; done
    for s in "${shots_c[@]}"; do rm -f "$out/riavvio/$s.png"; done
    run() { # run <nome> <mano> <file delle preferenze, o vuoto per i predefiniti> <cartella> <passi...>
        local name=$1 hand=$2 prefs=$3 dir=$4 code
        shift 4
        EXO_SHOT_MAIN_HAND=$hand EXO_SHOT_CLIENT_PREFS=$prefs bash "$here/client_shot.sh" "$variant" "$dir" "$@"
        code=$?
        [ "$code" -eq 0 ] && return 0
        if [ -s "$dir/log-check.txt" ]; then
            problems+=("esecuzione $name: avvisi del mod fuori elenco in $dir/log-check.txt")
        else
            problems+=("esecuzione $name: client_shot.sh e' uscito con $code")
        fi
        return 1
    }
    if run A right "" "$out" "${steps_a[@]}"; then
        if [ -f "$out/$PREFS" ]; then
            run C right "$out/$PREFS" "$out/riavvio" "${steps_c[@]}"
        else
            problems+=("manca il file delle preferenze dell'esecuzione A")
        fi
    fi
fi

# --- controlli ------------------------------------------------------------------------------------------------------
missing=()
for s in "${shots_a[@]}"; do [ -f "$out/$s.png" ] || missing+=("$s"); done
for s in "${shots_c[@]}"; do [ -f "$out/riavvio/$s.png" ] || missing+=("riavvio/$s"); done

# Gli aiuti dei controlli (qui verdict) e l'elenco failed: tools/client/measure_lib.sh.
source "$here/measure_lib.sh" || exit 2
# Le due preferenze di un file come le stampa Python ("True False"; None per una chiave che manca), oppure
# "misura-non-riuscita" se il file manca o non e' un oggetto JSON: verdict fa fallire il controllo.
prefs() {
    python3 -I -c 'import json, sys; d = json.load(open(sys.argv[1])); print(d.get("hudHidden"), d.get("vignette"))' \
        "$1" 2>/dev/null || echo "misura-non-riuscita"
}

checks() {
    local m good

    m=$(prefs "$out/$PREFS")
    good=1
    [ "$m" = "True False" ] && good=0
    verdict chat-e-tasti "$good" "$m" "True False: H e J hanno cambiato le preferenze, la h scritta nella chat no"

    m=$(prefs "$out/riavvio/$PREFS")
    good=1
    [ "$m" = "False False" ] && good=0
    verdict riavvio-file "$good" "$m" "False False: il client e' ripartito dal file della A, e H ha mostrato l'HUD"
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
