#!/usr/bin/env bash
# La sequenza della parte 4c ("l'HUD") nel client vero: tre esecuzioni di client_shot.sh e poi i controlli che lo
# script fa da se' sugli scatti con arm_measure.sh e sui file delle preferenze del client, che client_shot.sh copia
# accanto agli scatti. Prova l'HUD della tuta (design M4 par. 5.12: posto, sagoma, barra, avviso, celle di scorta, i
# due livelli, le due mani, la terza persona), i suoi due tasti e il file delle preferenze; la vignetta arriva con il
# Task 4 del piano della 4c.
#
#   EXO_ACCEPT_EULA=true bash tools/client/m4c_shots.sh <variante> <cartella>
#
# Il giocatore resta in creativa in 0,5 -60 0,5 rivolto a sud con l'inclinazione 15. La tuta la veste e la cambia
# "exoframe debug wear rottame <stato>", sei pezzi Rottame tutti nello stesso stato (0 integro, 1 danneggiato, 2
# critico, 3 rotto) con una cella carica (10 000 RE); "exoframe debug energy <RE>" porta la carica a quel valore, e lo
# stato dell'energia del server arriva subito al client. H, il tasto che nasconde l'HUD, sta al suo predefinito; J e'
# il tasto della vignetta, che la mod lascia senza tasto e che client_shot.sh assegna con la riga
# key_key.redstone_exoframes.vignette di options.txt: la sequenza prova i due modi in cui un tasto della mod prende il
# suo tasto, il predefinito e la riga delle opzioni letta all'avvio del client. I due tasti valgono anche senza la tuta
# (Decisione 6 del piano). Le regole degli scatti misurati (tools/client/README.md): il primo almeno 12 s dopo il primo
# passo dell'esecuzione; almeno 4 s fra l'ultimo passo che scrive sulla barra delle azioni (H, J, debug energy, debug
# wear), dove un messaggio resta 3 s, e lo scatto; nessun gamemode; i due scatti di ogni confronto nello stesso stato,
# salvo la cosa che il confronto misura. Lo scatto che mostra un messaggio viene un secondo dopo il tasto, e dopo lo
# scatto si aspettano 3 s, cosi' il messaggio se n'e' andato prima del passo dopo.
#
# Esecuzione A, mano principale a destra, dalle preferenze predefinite (HUD visibile, vignetta accesa), scatti in
# <cartella>:
#   1. attesa          "@attendi 12": la riga in chat del "gamemode creative" di client_shot.sh e l'avviso "Chat
#                      messages can't be verified" spariscono
#   2. chat            T apre la chat, una h scritta nella chat, Escape la chiude senza mandarla: con una schermata
#                      aperta il gioco base non registra il clic di H (KeyboardHandler.keyPress chiama KeyMapping.click
#                      solo senza schermate), quindi le preferenze non cambiano; lo vede alla fine il controllo
#                      chat-e-tasti, perche' con quella h contata l'HUD tornerebbe visibile
#   3. pieno           la tuta integra, l'HUD intero: scatto pieno
#   4. senza vignetta  J spegne la vignetta: scatto senza-vignetta, il riferimento dei confronti che seguono
#   5. nascosto        H nasconde l'HUD: scatto nascosto; H lo mostra di nuovo
#   6. stati dei pezzi danneggiato (sagoma gialla) e critico (rossa)
#   7. energia         500 RE, sotto il 10 % (LOW): scatto bassa ("5%" rosso dopo il posto dell'avviso, che lampeggia,
#                      la barra rossa corta); 0 RE (EMPTY): scatto esaurita ("0%" grigio, l'avviso grigio fisso, la
#                      barra vuota)
#   8. scorta          prima-della-cella ("x0" grigio), poi una cella da 5 000 RE nell'inventario: scorta ("x1"
#                      bianco); la cella si toglie
#   9. senza elmo      J riaccende la vignetta; i sei pezzi rotti, quindi l'elmo rotto: la sola barra, sulla sua
#                      piastra (senza-elmo); H la nasconde (senza-elmo-nascosto) e la mostra
#  10. terza persona   la tuta integra, F5 (da dietro): terza; H nasconde: terza-nascosto; H mostra, F5 due volte
#                      torna alla prima persona
#  11. fine            J spegne la vignetta: scatto messaggio-vignetta un secondo dopo, da guardare: "Helmet vignette
#                      off" sulla barra delle azioni; H nasconde l'HUD: scatto messaggio-nascosto un secondo dopo, da
#                      guardare: "Exoframe HUD hidden: H to show it"; poi tp in 0,5 -60 -5,5 per i tre scatti di
#                      client_shot.sh. Il client riscrive il file delle preferenze a ogni tasto, e client_shot.sh lo
#                      copia in <cartella>/redstone_exoframes-client.json: alla fine HUD nascosto e vignetta spenta
# Esecuzione B, mano principale a sinistra (EXO_SHOT_MAIN_HAND=left), dalle preferenze predefinite, scatti in
# <cartella>/mancino: "@attendi 12", la tuta integra e J (vignetta spenta): mancino; H nasconde: mancino-nascosto; H
# mostra; danneggiata: mancino-danneggiato; J e l'elmo rotto: mancino-senza-elmo; H nasconde:
# mancino-senza-elmo-nascosto; poi tp in 0,5 -60 -5,5. Parte solo se la A e' uscita con 0.
# Esecuzione C, il riavvio: un client nuovo, mano destra, che parte dal file scritto dal client nella A
# (EXO_SHOT_CLIENT_PREFS=<cartella>/redstone_exoframes-client.json), scatti in <cartella>/riavvio: "@attendi 12", la
# tuta integra: riavvio-nascosto (l'HUD che il file teneva nascosto); H lo mostra: scatto riavvio-messaggio un secondo
# dopo, da guardare: "Exoframe HUD shown"; riavvio-visibile; poi tp in 0,5 -60 -5,5. La C parte solo se la A e' uscita
# con 0 e ha lasciato il file. Si prova cosi' la catena vera (tasto, scrittura, lettura all'avvio), non la lettura di un
# file scritto a mano (Decisione 10 del piano).
#
# Controlli. I riquadri e i colori esatti vengono da client/HudLayout e client/HudModel (costanti qui sotto); le soglie
# di quantita' dalla misura. Un riquadro "dentro" un rettangolo: non vuoto, e con X, Y, X + L e Y + A dentro i suoi.
#   chat-e-tasti        <cartella>/redstone_exoframes-client.json vale "True False": H e J hanno cambiato le
#                       preferenze e la h scritta nella chat no (contata, l'HUD sarebbe di nuovo visibile: "False
#                       False"; con J senza tasto la vignetta resterebbe accesa: "True True")
#   riavvio-file        <cartella>/riavvio/redstone_exoframes-client.json vale "False False": il client e' ripartito dal
#                       file della A (dai predefiniti H avrebbe nascosto l'HUD, con la vignetta accesa: "True True")
#   gruppo              senza-vignetta contro nascosto: il riquadro della differenza e' il gruppo, e nella sagoma ci
#                       sono pixel: tutto l'HUD sta nel gruppo, che non tocca braccio, barra, mirino e chat
#   testo               la percentuale verde nell'area della percentuale, a sinistra nel sottogruppo dell'energia
#   barra-normale       il verde del riempimento: le sue cinque righe piene, da sinistra, sotto la riga piu' chiara
#   sagoma              danneggiato contro senza-vignetta: cambia solo la sagoma, da verde a gialla
#   critico             la sagoma rossa, senza giallo
#   barra-bassa         bassa contro senza-vignetta: cambia solo l'energia; rosso nella barra da sinistra, corto
#                       (500 RE di 10 000 sono 3 pixel dell'interfaccia); niente verde nella barra, niente rosso nella
#                       sagoma. L'avviso lampeggia: nessuna misura esatta del sottogruppo
#   barra-esaurita      niente verde nella barra (vuota), niente rosso (non e' LOW), il grigio della percentuale e
#                       dell'avviso fisso
#   scorta              prima-della-cella contro scorta: cambia solo l'area delle celle di scorta, grigio prima e
#                       bianco dopo
#   solo-barra          senza-elmo contro senza-elmo-nascosto: il riquadro e' il sottogruppo dell'energia, niente
#                       nella sagoma
#   terza-persona       terza contro terza-nascosto, nel gruppo e in tre angoli dello schermo che il gruppo non tocca:
#                       solo riquadri, perche' in terza persona la tuta non e' ferma al pixel fra due scatti
#   mancino-gruppo      mancino contro mancino-nascosto: il gruppo nell'angolo in basso a destra, la sagoma sul bordo
#                       destro, la percentuale verde a sinistra del sottogruppo dell'energia (l'interno non e'
#                       specchiato)
#   mancino-sagoma      mancino-danneggiato contro mancino: cambia solo la sagoma, a destra, gialla
#   mancino-solo-barra  mancino-senza-elmo contro mancino-senza-elmo-nascosto: il sottogruppo dell'energia, a destra
#   riavvio             nascosto della A contro riavvio-nascosto della C, nel gruppo: niente (il client della C e'
#                       partito con l'HUD nascosto dal file, come quello della A dopo H); riavvio-visibile contro
#                       riavvio-nascosto: il riquadro e' il gruppo (H lo mostra). Il riferimento senza HUD non e' uno
#                       scatto senza la tuta: senza la tuta gli occhi del giocatore sono piu' bassi (con la tuta la
#                       loro altezza e' x1,65, SuitSize), e uno scatto senza la tuta cambia lo sfondo sotto il gruppo
#                       (misurati 7132 pixel diversi nel gruppo). I due scatti confrontati hanno la tuta, l'HUD
#                       nascosto e la vignetta spenta (nella fase "nascosto" della A e nel file da cui parte la C);
#                       vengono da due esecuzioni, e due client che partono dallo stesso mondo nello stesso posto
#                       disegnano la stessa scena al pixel (misurato: 0 pixel diversi su tutto lo schermo)
#
# Non prova: il jar di produzione (il client gira nell'ambiente di sviluppo); la vignetta (Task 4); una sagoma mista
# (lo stato del comando di debug vale per tutti e sei i pezzi); i pezzi non montati con l'elmo che funziona; il telaio
# senza cella (oggi non si indossa; lo prova HudModelTest); le altre grandezze dell'interfaccia (le prova
# HudLayoutTest); i sottotitoli e l'indicatore del salvataggio sopra o sotto il gruppo; F1, F3, F3+H, F3+T; H con
# l'inventario aperto; il tasto della vignetta assegnato nelle impostazioni dei controlli; il file delle preferenze
# rovinato, di una versione piu' nuova o in una cartella di sola lettura (li provano ClientPrefsFileTest e la
# checklist per il PC della parte); la mappa a due mani; il consumo fino all'avviso in sopravvivenza.
#
# Dura circa sei minuti a cache calde (misurati 354 s su 1.21.1-neoforge e 373 s su 1.20.1-forge: 146-154 s
# l'esecuzione A, 93-98 s la B, 79-84 s la C).
#
# Uscita: una riga "ok <nome>: <misure>" o "KO <nome>: <misure> (atteso ...)" per controllo, tutti valutati anche dopo
# un fallimento; l'ultima riga e' "OK <variante>: scatti in <cartella>" (uscita 0) se le tre esecuzioni sono uscite con
# 0 e ogni controllo e' ok, altrimenti "FAIL <variante>: ..." su stderr (uscita 1). Se l'esecuzione A fallisce la B e
# la C non partono, e la C non parte nemmeno se la A non lascia il file delle preferenze; i controlli girano solo se ci
# sono tutti gli scatti. Con M4C_SOLO_CONTROLLI=1 lo script salta le esecuzioni e rifa' i controlli su cio' che la
# cartella ha gia' (per fissare le soglie senza riavviare il client). Uscita 2: uso sbagliato. Le variabili d'ambiente
# sono quelle di client_shot.sh (EXO_ACCEPT_EULA obbligatoria); EXO_SHOT_MAIN_HAND ed EXO_SHOT_CLIENT_PREFS le imposta
# questo script per ogni esecuzione, qualunque valore abbiano nell'ambiente di chi lo chiama. Istruzioni in
# tools/client/README.md.
set -u
# La config predefinita: una EXO_SHOT_SERVER_CONFIG ereditata dall'ambiente cambierebbe le regole del server.
unset EXO_SHOT_SERVER_CONFIG

here=$(cd "$(dirname "$0")" && pwd)
measure="$here/arm_measure.sh"

if [ "$#" -ne 2 ]; then
    echo "Uso: EXO_ACCEPT_EULA=true bash tools/client/m4c_shots.sh <variante> <cartella>" >&2
    exit 2
fi
variant=$1
out=$2
u=${EXO_SHOT_USER:-Exo}

# --- riquadri e colori ----------------------------------------------------------------------------------------------
# I rettangoli dell'HUD sullo schermo a 960x540 con la scala 2 (interfaccia 480x270), con il nome dell'elemento di
# client/HudLayout (tabella "Disposizione dell'HUD" del piano della 4c), che
# HudLayoutTest.screenRectsAtNineSixtyMatchTheSequence tiene uguali al codice. La mano destra:
GROUP_RIGHT=176x70+4+466             # il gruppo, 88x35 in (2, 233)
SILHOUETTE_RIGHT=42x70+4+466         # la sagoma, verso il bordo
ENERGY_RIGHT=134x70+46+466           # l'energia, verso l'interno; e' anche la piastra con la sola barra
PERCENT_AREA_RIGHT=58x18+50+482      # la percentuale piu' larga, con o senza avviso
SPARE_AREA_RIGHT=50x18+126+482       # l'icona della cella e "xN" fino a due cifre
BAR_FILL_RIGHT=122x12+52+508         # il riempimento della barra, la prima riga piu' chiara
BAR_FILL_BODY_RIGHT=122x10+52+510    # le sue cinque righe del colore pieno, con la barra piena
# La mano sinistra: il gruppo nell'angolo in basso a destra, la sagoma sul bordo destro.
GROUP_LEFT=176x70+780+466
SILHOUETTE_LEFT=42x70+914+466
ENERGY_LEFT=134x70+780+466
PERCENT_AREA_LEFT=58x18+784+482
# Tre angoli dello schermo, 72x40 dell'interfaccia (HudLayout.vignetteCorners), che il gruppo della mano destra non
# tocca.
CORNER_TOP_LEFT=144x80+0+0
CORNER_TOP_RIGHT=144x80+816+0
CORNER_BOTTOM_RIGHT=144x80+816+460
# Il bordo sinistro del riempimento: la barra bassa parte da qui.
IFS='x+' read -r _ _ BAR_FILL_X _ <<<"$BAR_FILL_RIGHT"
# I colori della legenda di client/HudModel, RRGGBB.
GREEN=55FF55
YELLOW=FFFF55
RED=FF5555
GREY=8A8A8A
WHITE=FFFFFF

# --- soglie ---------------------------------------------------------------------------------------------------------
# Ciascuna e' la meta', arrotondata per difetto alla decina, del valore misurato su 1.21.1-neoforge, la prima variante
# (fra parentesi); quella che serve a piu' misure e' la meta' della piu' piccola.

# I pixel dell'HUD intero contro l'HUD nascosto: gruppo, terza-persona e mancino-gruppo (misurati 12320 in tutti e
# tre: ogni pixel del gruppo, 176x70).
GROUP_MIN=6160
# La sagoma contro la sagoma nascosta, piastra e pezzi: gruppo e mancino-gruppo (misurati 2940 nei due: ogni pixel
# della sagoma, 42x70).
SIL_MIN=1470
# I soli pezzi che cambiano colore, pieno e contorno: sagoma e mancino-sagoma (misurati 1024 nei due).
SIL_CHANGE_MIN=510
# Il colore pieno dei pezzi: sagoma (verde prima, giallo dopo), critico (rosso) e mancino-sagoma (giallo) (misurati
# 424 in tutti e quattro).
SIL_COLOUR_MIN=210
# La percentuale: testo (verde, misurati 244), barra-esaurita (grigio, con l'avviso fisso: 248) e mancino-gruppo
# (verde: 244).
TEXT_MIN=120
# Le cinque righe del colore pieno della barra piena: barra-normale (misurati 1220).
BAR_MIN=610
# "x0" che diventa "x1": scorta (misurati 228).
SPARE_MIN=110
# Il grigio di "x0" e il bianco di "x1": scorta (misurati 112 e 84).
SPARE_COLOUR_MIN=40
# La sola barra contro la sola barra nascosta: solo-barra (misurati 9380: ogni pixel del sottogruppo dell'energia,
# 134x70).
BAR_ONLY_MIN=4690
# Fissa: 500 RE di 10 000 sono 3 pixel dell'interfaccia, 6 dello schermo (misurati 6; la barra piena ne ha 122).
LOW_FILL_MAX_W=10

# --- passi ----------------------------------------------------------------------------------------------------------
# Il primo passo di ogni esecuzione: vedi il commento di testa.
FIRST_WAIT=12
# Il file delle preferenze del client che client_shot.sh copia accanto agli scatti.
PREFS=redstone_exoframes-client.json
# All'aperto, per i tre scatti finali di client_shot.sh.
AWAY="tp $u 0.5 -60 -5.5 0 15"
# Una cella con 5 000 RE: dati dello stack come NBT su 1.20.1, nel componente custom_data su 1.21.1. Una variante
# sconosciuta la rifiuta client_shot.sh.
case "$variant" in
    1.20.1-*) CELL='redstone_exoframes:redstone_cell{exoframes:{energy:5000}}' ;;
    *) CELL='redstone_exoframes:redstone_cell[minecraft:custom_data={exoframes:{energy:5000}}]' ;;
esac
# I sei pezzi Rottame nello stato dato (0-3), con una cella carica.
wear() { echo "execute as $u run exoframe debug wear rottame $1"; }
# La carica della cella indossata.
energy() { echo "execute as $u run exoframe debug energy $1"; }

steps_a=(
    # 1. attesa
    "@attendi $FIRST_WAIT"
    # 2. chat: una h scritta nella chat non e' un clic del tasto H
    "@tieni t 0.2" "@attendi 1" "@tieni h 0.2" "@tieni Escape 0.2" "@attendi 1"
    # 3. pieno
    "$(wear 0)" "@attendi 4" "@scatto pieno"
    # 4. senza vignetta
    "@tieni j 0.2" "@attendi 4" "@scatto senza-vignetta"
    # 5. nascosto, e di nuovo visibile
    "@tieni h 0.2" "@attendi 4" "@scatto nascosto" "@tieni h 0.2"
    # 6. stati dei pezzi
    "$(wear 1)" "@attendi 4" "@scatto danneggiato" "$(wear 2)" "@attendi 4" "@scatto critico"
    # 7. energia: bassa ed esaurita
    "$(wear 0)" "$(energy 500)" "@attendi 4" "@scatto bassa" "$(energy 0)" "@attendi 4" "@scatto esaurita"
    # 8. scorta: una cella carica nell'inventario
    "$(wear 0)" "@attendi 4" "@scatto prima-della-cella" "give $u $CELL" "@attendi 2" "@scatto scorta"
    "clear $u redstone_exoframes:redstone_cell"
    # 9. senza elmo: la vignetta di nuovo accesa, l'elmo rotto
    "@tieni j 0.2" "$(wear 3)" "@attendi 4" "@scatto senza-elmo" "@tieni h 0.2" "@attendi 4"
    "@scatto senza-elmo-nascosto" "@tieni h 0.2"
    # 10. terza persona (da dietro), poi di nuovo la prima
    "$(wear 0)" "@tieni F5 0.2" "@attendi 4" "@scatto terza" "@tieni h 0.2" "@attendi 4" "@scatto terza-nascosto"
    "@tieni h 0.2" "@tieni F5 0.2" "@tieni F5 0.2"
    # 11. fine: J spegne la vignetta e H nasconde l'HUD, ciascuno con il suo messaggio sulla barra delle azioni
    "@tieni j 0.2" "@attendi 1" "@scatto messaggio-vignetta" "@attendi 3"
    "@tieni h 0.2" "@attendi 1" "@scatto messaggio-nascosto" "@attendi 3"
    "$AWAY"
)
steps_b=(
    "@attendi $FIRST_WAIT"
    "$(wear 0)" "@tieni j 0.2" "@attendi 4" "@scatto mancino" "@tieni h 0.2" "@attendi 4" "@scatto mancino-nascosto"
    "@tieni h 0.2" "$(wear 1)" "@attendi 4" "@scatto mancino-danneggiato"
    "@tieni j 0.2" "$(wear 3)" "@attendi 4" "@scatto mancino-senza-elmo" "@tieni h 0.2" "@attendi 4"
    "@scatto mancino-senza-elmo-nascosto"
    "$AWAY"
)
steps_c=(
    "@attendi $FIRST_WAIT"
    # la tuta integra: il file della A tiene l'HUD nascosto
    "$(wear 0)" "@attendi 4" "@scatto riavvio-nascosto"
    # H mostra l'HUD che il file della A teneva nascosto
    "@tieni h 0.2" "@attendi 1" "@scatto riavvio-messaggio" "@attendi 3"
    "@scatto riavvio-visibile"
    "$AWAY"
)
shots_a=(pieno senza-vignetta nascosto danneggiato critico bassa esaurita prima-della-cella scorta senza-elmo
    senza-elmo-nascosto terza terza-nascosto messaggio-vignetta messaggio-nascosto)
shots_b=(mancino mancino-nascosto mancino-danneggiato mancino-senza-elmo mancino-senza-elmo-nascosto)
shots_c=(riavvio-nascosto riavvio-messaggio riavvio-visibile)

a="$out"
b="$out/mancino"
c="$out/riavvio"

# --- le esecuzioni --------------------------------------------------------------------------------------------------
problems=() # le ragioni di un FAIL, nell'ordine in cui si incontrano
if [ "${M4C_SOLO_CONTROLLI:-}" != 1 ]; then
    echo "=== $variant m4c"
    # un'uscita anticipata di client_shot.sh (uso o ambiente sbagliati) non lascia leggere gli avvisi, gli scatti o il
    # file delle preferenze di un'altra esecuzione, e se la B o la C non partono i controlli non girano su quelli di
    # prima
    rm -f "$a/log-check.txt" "$b/log-check.txt" "$c/log-check.txt" "$a/$PREFS" "$b/$PREFS" "$c/$PREFS"
    for s in "${shots_a[@]}"; do rm -f "$a/$s.png"; done
    for s in "${shots_b[@]}"; do rm -f "$b/$s.png"; done
    for s in "${shots_c[@]}"; do rm -f "$c/$s.png"; done
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
    if run A right "" "$a" "${steps_a[@]}"; then
        run B left "" "$b" "${steps_b[@]}"
        if [ -f "$a/$PREFS" ]; then
            run C right "$a/$PREFS" "$c" "${steps_c[@]}"
        else
            problems+=("manca il file delle preferenze dell'esecuzione A")
        fi
    fi
fi

# --- controlli ------------------------------------------------------------------------------------------------------
missing=()
for s in "${shots_a[@]}"; do [ -f "$a/$s.png" ] || missing+=("$s"); done
for s in "${shots_b[@]}"; do [ -f "$b/$s.png" ] || missing+=("mancino/$s"); done
for s in "${shots_c[@]}"; do [ -f "$c/$s.png" ] || missing+=("riavvio/$s"); done

# Gli aiuti delle misure (take_measure, val, box, inside, verdict) e l'elenco failed: tools/client/measure_lib.sh.
source "$here/measure_lib.sh" || exit 2
# Le due preferenze di un file come le stampa Python ("True False"; None per una chiave che manca), oppure
# "misura-non-riuscita" se il file manca o non e' un oggetto JSON: verdict fa fallire il controllo.
prefs() {
    python3 -I -c 'import json, sys; d = json.load(open(sys.argv[1])); print(d.get("hudHidden"), d.get("vignette"))' \
        "$1" 2>/dev/null || echo "misura-non-riuscita"
}
# Vero se il riquadro della misura e' esattamente il rettangolo dato.
is_box() { [ "$(val "$1" riquadro)" = "$2" ]; }

checks() {
    local m m1 m2 m3 m4 m5 good want

    m=$(prefs "$a/$PREFS")
    good=1
    [ "$m" = "True False" ] && good=0
    verdict chat-e-tasti "$good" "$m" "True False: H e J hanno cambiato le preferenze, la h scritta nella chat no"

    m=$(prefs "$c/$PREFS")
    good=1
    [ "$m" = "False False" ] && good=0
    verdict riavvio-file "$good" "$m" "False False: il client e' ripartito dal file della A, e H ha mostrato l'HUD"

    m1=$(take_measure differenza "$a/senza-vignetta.png" "$a/nascosto.png")
    m2=$(take_measure differenza "$a/senza-vignetta.png" "$a/nascosto.png" "$SILHOUETTE_RIGHT")
    good=1
    is_box "$m1" "$GROUP_RIGHT" && [ "$(val "$m1" pixel)" -ge "$GROUP_MIN" ] \
        && [ "$(val "$m2" pixel)" -ge "$SIL_MIN" ] && good=0
    want="riquadro=$GROUP_RIGHT e pixel >= $GROUP_MIN: tutto l'HUD nel gruppo; nella sagoma pixel >= $SIL_MIN"
    verdict gruppo "$good" "$m1; sagoma $m2" "$want"

    m1=$(take_measure colore "$a/senza-vignetta.png" "$GREEN" "$PERCENT_AREA_RIGHT")
    good=1
    [ "$(val "$m1" colore)" -ge "$TEXT_MIN" ] && good=0
    verdict testo "$good" "$m1" "colore >= $TEXT_MIN: la percentuale verde a sinistra nel sottogruppo dell'energia"

    m1=$(take_measure colore "$a/senza-vignetta.png" "$GREEN" "$BAR_FILL_RIGHT")
    good=1
    [ "$(val "$m1" colore)" -ge "$BAR_MIN" ] && is_box "$m1" "$BAR_FILL_BODY_RIGHT" && good=0
    want="colore >= $BAR_MIN e riquadro=$BAR_FILL_BODY_RIGHT: piena, da sinistra, sotto la riga piu' chiara"
    verdict barra-normale "$good" "$m1" "$want"

    m1=$(take_measure differenza "$a/senza-vignetta.png" "$a/danneggiato.png" "$GROUP_RIGHT")
    m2=$(take_measure colore "$a/senza-vignetta.png" "$GREEN" "$SILHOUETTE_RIGHT")
    m3=$(take_measure colore "$a/danneggiato.png" "$YELLOW" "$SILHOUETTE_RIGHT")
    m4=$(take_measure colore "$a/danneggiato.png" "$GREEN" "$SILHOUETTE_RIGHT")
    box "$m1"
    good=1
    inside "$SILHOUETTE_RIGHT" && [ "$(val "$m1" pixel)" -ge "$SIL_CHANGE_MIN" ] \
        && [ "$(val "$m2" colore)" -ge "$SIL_COLOUR_MIN" ] && [ "$(val "$m3" colore)" -ge "$SIL_COLOUR_MIN" ] \
        && [ "$(val "$m4" colore)" -eq 0 ] && good=0
    want="la differenza dentro $SILHOUETTE_RIGHT con pixel >= $SIL_CHANGE_MIN; verde >= $SIL_COLOUR_MIN prima;"
    want+=" giallo >= $SIL_COLOUR_MIN e verde 0 dopo"
    verdict sagoma "$good" "$m1; verde prima $m2; giallo dopo $m3; verde dopo $m4" "$want"

    m1=$(take_measure colore "$a/critico.png" "$RED" "$SILHOUETTE_RIGHT")
    m2=$(take_measure colore "$a/critico.png" "$YELLOW" "$SILHOUETTE_RIGHT")
    good=1
    [ "$(val "$m1" colore)" -ge "$SIL_COLOUR_MIN" ] && [ "$(val "$m2" colore)" -eq 0 ] && good=0
    verdict critico "$good" "rosso $m1; giallo $m2" "rosso >= $SIL_COLOUR_MIN nella sagoma; giallo 0"

    m1=$(take_measure differenza "$a/senza-vignetta.png" "$a/bassa.png" "$GROUP_RIGHT")
    m2=$(take_measure colore "$a/bassa.png" "$RED" "$BAR_FILL_RIGHT")
    m3=$(take_measure colore "$a/bassa.png" "$GREEN" "$BAR_FILL_RIGHT")
    m4=$(take_measure colore "$a/bassa.png" "$RED" "$SILHOUETTE_RIGHT")
    good=1
    box "$m1"
    if inside "$ENERGY_RIGHT"; then
        box "$m2"
        [ "$(val "$m2" colore)" -ge 1 ] && [ "$BX" -eq "$BAR_FILL_X" ] && [ "$BL" -le "$LOW_FILL_MAX_W" ] \
            && [ "$(val "$m3" colore)" -eq 0 ] && [ "$(val "$m4" colore)" -eq 0 ] && good=0
    fi
    want="la differenza dentro $ENERGY_RIGHT; rosso nella barra >= 1 con il riquadro da X $BAR_FILL_X e largo al piu'"
    want+=" $LOW_FILL_MAX_W; verde nella barra 0; rosso nella sagoma 0"
    verdict barra-bassa "$good" "$m1; rosso nella barra $m2; verde nella barra $m3; rosso nella sagoma $m4" "$want"

    m1=$(take_measure colore "$a/esaurita.png" "$GREEN" "$BAR_FILL_RIGHT")
    m2=$(take_measure colore "$a/esaurita.png" "$RED" "$ENERGY_RIGHT")
    m3=$(take_measure colore "$a/esaurita.png" "$GREY" "$PERCENT_AREA_RIGHT")
    good=1
    [ "$(val "$m1" colore)" -eq 0 ] && [ "$(val "$m2" colore)" -eq 0 ] && [ "$(val "$m3" colore)" -ge "$TEXT_MIN" ] \
        && good=0
    want="verde nella barra 0 (vuota); rosso nell'energia 0 (non e' LOW); grigio >= $TEXT_MIN (percentuale e avviso)"
    verdict barra-esaurita "$good" "verde nella barra $m1; rosso $m2; grigio $m3" "$want"

    m1=$(take_measure differenza "$a/prima-della-cella.png" "$a/scorta.png" "$GROUP_RIGHT")
    m2=$(take_measure colore "$a/prima-della-cella.png" "$GREY" "$SPARE_AREA_RIGHT")
    m3=$(take_measure colore "$a/scorta.png" "$WHITE" "$SPARE_AREA_RIGHT")
    box "$m1"
    good=1
    inside "$SPARE_AREA_RIGHT" && [ "$(val "$m1" pixel)" -ge "$SPARE_MIN" ] \
        && [ "$(val "$m2" colore)" -ge "$SPARE_COLOUR_MIN" ] && [ "$(val "$m3" colore)" -ge "$SPARE_COLOUR_MIN" ] \
        && good=0
    want="la differenza dentro $SPARE_AREA_RIGHT con pixel >= $SPARE_MIN; grigio prima e bianco dopo >="
    want+=" $SPARE_COLOUR_MIN"
    verdict scorta "$good" "$m1; grigio prima $m2; bianco dopo $m3" "$want"

    m1=$(take_measure differenza "$a/senza-elmo.png" "$a/senza-elmo-nascosto.png")
    m2=$(take_measure differenza "$a/senza-elmo.png" "$a/senza-elmo-nascosto.png" "$SILHOUETTE_RIGHT")
    good=1
    is_box "$m1" "$ENERGY_RIGHT" && [ "$(val "$m1" pixel)" -ge "$BAR_ONLY_MIN" ] && [ "$(val "$m2" pixel)" -eq 0 ] \
        && good=0
    want="riquadro=$ENERGY_RIGHT e pixel >= $BAR_ONLY_MIN: la sola barra, senza sagoma; nella sagoma pixel=0"
    verdict solo-barra "$good" "$m1; sagoma $m2" "$want"

    m1=$(take_measure differenza "$a/terza.png" "$a/terza-nascosto.png" "$GROUP_RIGHT")
    m2=$(take_measure differenza "$a/terza.png" "$a/terza-nascosto.png" "$CORNER_TOP_LEFT")
    m3=$(take_measure differenza "$a/terza.png" "$a/terza-nascosto.png" "$CORNER_TOP_RIGHT")
    m4=$(take_measure differenza "$a/terza.png" "$a/terza-nascosto.png" "$CORNER_BOTTOM_RIGHT")
    good=1
    is_box "$m1" "$GROUP_RIGHT" && [ "$(val "$m1" pixel)" -ge "$GROUP_MIN" ] && [ "$(val "$m2" pixel)" -eq 0 ] \
        && [ "$(val "$m3" pixel)" -eq 0 ] && [ "$(val "$m4" pixel)" -eq 0 ] && good=0
    want="riquadro=$GROUP_RIGHT e pixel >= $GROUP_MIN: barra e sagoma anche in terza persona; pixel=0 nei tre angoli"
    verdict terza-persona "$good" "$m1; angoli $m2; $m3; $m4" "$want"

    m1=$(take_measure differenza "$b/mancino.png" "$b/mancino-nascosto.png")
    m2=$(take_measure differenza "$b/mancino.png" "$b/mancino-nascosto.png" "$SILHOUETTE_LEFT")
    m3=$(take_measure colore "$b/mancino.png" "$GREEN" "$PERCENT_AREA_LEFT")
    good=1
    is_box "$m1" "$GROUP_LEFT" && [ "$(val "$m1" pixel)" -ge "$GROUP_MIN" ] && [ "$(val "$m2" pixel)" -ge "$SIL_MIN" ] \
        && [ "$(val "$m3" colore)" -ge "$TEXT_MIN" ] && good=0
    want="riquadro=$GROUP_LEFT e pixel >= $GROUP_MIN; la sagoma sul bordo destro pixel >= $SIL_MIN; la percentuale"
    want+=" verde a sinistra dell'energia >= $TEXT_MIN (l'interno non e' specchiato)"
    verdict mancino-gruppo "$good" "$m1; sagoma $m2; percentuale $m3" "$want"

    m1=$(take_measure differenza "$b/mancino.png" "$b/mancino-danneggiato.png" "$GROUP_LEFT")
    m2=$(take_measure colore "$b/mancino-danneggiato.png" "$YELLOW" "$SILHOUETTE_LEFT")
    box "$m1"
    good=1
    inside "$SILHOUETTE_LEFT" && [ "$(val "$m1" pixel)" -ge "$SIL_CHANGE_MIN" ] \
        && [ "$(val "$m2" colore)" -ge "$SIL_COLOUR_MIN" ] && good=0
    want="la differenza dentro $SILHOUETTE_LEFT con pixel >= $SIL_CHANGE_MIN; giallo >= $SIL_COLOUR_MIN"
    verdict mancino-sagoma "$good" "$m1; giallo $m2" "$want"

    m1=$(take_measure differenza "$b/mancino-senza-elmo.png" "$b/mancino-senza-elmo-nascosto.png")
    m2=$(take_measure differenza "$b/mancino-senza-elmo.png" "$b/mancino-senza-elmo-nascosto.png" "$SILHOUETTE_LEFT")
    good=1
    is_box "$m1" "$ENERGY_LEFT" && [ "$(val "$m2" pixel)" -eq 0 ] && good=0
    verdict mancino-solo-barra "$good" "$m1; sagoma $m2" "riquadro=$ENERGY_LEFT; nella sagoma pixel=0"

    # il riferimento senza HUD ha la tuta, come riavvio-nascosto: vedi il commento di testa
    m1=$(take_measure differenza "$a/nascosto.png" "$c/riavvio-nascosto.png" "$GROUP_RIGHT")
    m2=$(take_measure differenza "$c/riavvio-visibile.png" "$c/riavvio-nascosto.png")
    good=1
    [ "$(val "$m1" pixel)" -eq 0 ] && is_box "$m2" "$GROUP_RIGHT" && good=0
    want="pixel=0 nel gruppo: il client della C e' partito con l'HUD nascosto, come quello della A dopo H;"
    want+=" riquadro=$GROUP_RIGHT: H lo mostra"
    verdict riavvio "$good" "$m1; $m2" "$want"
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
