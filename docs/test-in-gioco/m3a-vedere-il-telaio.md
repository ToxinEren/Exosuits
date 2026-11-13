# Checklist in gioco: milestone 3, parte 3a (vedere il telaio)

Per Jarvis, sul PC Windows di Luigi, con i quattro jar di questa parte. Non serve aver seguito il lavoro: qui sotto c'è tutto ciò che occorre sapere.

## Cosa c'è da vedere

Da questa parte un giocatore può "indossare" la tuta, ma solo per prova: non esiste ancora un telaio da costruire o in cui entrare (arriva con la parte 3b). Un comando di debug mette negli slot dell'armatura dei segnaposto invisibili, e il client disegna al loro posto la tuta:

- la tuta è **1,65 volte più grande** del giocatore: alta circa **3 blocchi** (telaio nudo 3,02, con i pezzi 3,09), larga circa 2,5 (con i pezzi 2,6), con i piedi a terra;
- il **pilota** (la skin del giocatore) resta a grandezza normale ma è **sollevato** di quasi un blocco, con testa, busto e braccia dentro testa, petto e braccia della tuta; le sue **gambe non si disegnano mai** (stanno nelle cosce della tuta);
- con il **telaio nudo** il pilota si vede dalla vita in su dentro lo scheletro; con i **pezzi Rottame** montati (elmo, torso, due braccia, due gambe) della skin non si vede nulla;
- i pezzi hanno **quattro stati** (0 integro, 1 danneggiato, 2 critico, 3 rotto): sempre più scuri, ammaccati, bruciati;
- i pezzi hanno **due colori** verniciabili (primario e secondario, predefiniti bronzo `8A6F52` e canna di fucile `4A4F55`); vetri, sbarre, feritoie e fessure luminose non si colorano;
- l'**oggetto in mano** si disegna nella mano della tuta, ingrandito come lei, in tutte e due le mani;
- sdraiati (posa strisciante) tuta e pilota restano **uniti e a terra**.

Il comando (serve un mondo con i trucchi attivi, o essere operatore):

| Comando | Effetto |
|---|---|
| `/exoframe debug wear nudo` | telaio nudo, senza pezzi |
| `/exoframe debug wear rottame` | sei pezzi Rottame, stato 0, colori predefiniti |
| `/exoframe debug wear rottame <stato 0..3>` | stessi pezzi nello stato dato |
| `/exoframe debug wear rottame <stato> <primario RRGGBB> <secondario RRGGBB>` | pezzi verniciati, per esempio `/exoframe debug wear rottame 0 3A6EA5 D9D9D9` |
| `/exoframe debug wear via` | toglie i segnaposto; la risposta dice quanti |

Prima di indossare la tuta svuota gli slot dell'armatura: il comando li occupa con i segnaposto.

## Già verificato nella sessione cloud (13 novembre 2026)

Con `tools/client/m3a_shots.sh`, sul client di **sviluppo** di ogni variante (non sui jar di produzione), dopo il ridisegno di piedi e schiena: otto scatti per variante, ognuno da dietro, davanti e di tre quarti. Tutti gli scatti riusciti su tutte e quattro le varianti, e ognuno è stato guardato:

| Controllo | 1.21.1 NeoForge | 1.21.1 Fabric | 1.20.1 Fabric | 1.20.1 Forge |
|---|---|---|---|---|
| Tuta alta circa 3 blocchi (come una colonna di 3 blocchi accanto), piedi a terra | sì | sì | sì | sì |
| Telaio nudo: pilota dalla vita in su, gambe del pilota mai visibili | sì | sì | sì | sì |
| Rottame: nessuna parte della skin, davanti, dietro, di tre quarti | sì | sì | sì | sì |
| Stati 0-3 in ordine, dal più pulito al più scuro | sì | sì | sì | sì |
| Colori di prova solo sulle piastre verniciabili | sì | sì | sì | sì |
| Piccone nella mano destra e scudo nella sinistra della tuta | sì | sì | sì | sì |
| Sdraiati: tuta distesa a terra con il giocatore | sì | sì | sì | sì |
| Log: avvisi del mod o errori dei mixin | nessuno | nessuno | nessuno | nessuno |

Da sdraiati il pilota, dentro il telaio, si intravede appena dagli angoli raggiungibili dallo script: che stia nella tuta lo conferma il PC (sezione 2). Gli scatti di riferimento sono in `m3a-screenshot/`: per ogni variante `<variante>-armata-fronte.png` e `-armata-retro.png` (Rottame, stato 0), e per 1.21.1 NeoForge e 1.20.1 Forge anche `-nudo-fronte.png`, `-stati.png`, `-colori.png`, `-mani.png`, `-sdraiato.png`. Sono stati fatti con grafica "Rapida", campo visivo 70 e distanza di rendering 4.

Il cloud non prova tre cose, che sono il motivo di questa checklist: che il **jar di produzione** si avvii e applichi i suoi mixin fuori dall'ambiente di sviluppo (nel cloud gira il client di sviluppo di Loom); ciò che si vede **muovendosi** o da angolazioni che lo script non raggiunge; ciò che vede **un altro giocatore**.

## 0. Preparazione

- [ ] Procurati i quattro jar di questa parte come nella sezione 0 di `m1-setup.md` (artefatto `redstone-exoframes-jars` della CI dell'ultimo push del branch, oppure build locale con `collectJar`): `redstone-exoframes-fabric-0.1.0+mc1.20.1.jar`, `redstone-exoframes-fabric-0.1.0+mc1.21.1.jar`, `redstone-exoframes-forge-0.1.0+mc1.20.1.jar`, `redstone-exoframes-neoforge-0.1.0+mc1.21.1.jar`. Controllali con `bash tools/check_jars.sh <cartella>`.
- [ ] Usa le quattro istanze client e i quattro server dedicati della milestone 1 (tabella dei profili in `m1-setup.md`, sezione 2: loader, Java e dipendenze, con GeckoLib). Sostituisci nella cartella `mods` il jar vecchio con quello nuovo; nient'altro in `mods`.
- [ ] Tieni aperta la cartella `docs/test-in-gioco/m3a-screenshot/` del repository: contiene gli scatti di riferimento del cloud, citati sotto.
- [ ] Per gli screenshot del gioco usa F2 (finiscono in `<istanza>\screenshots\`), con F1 per nascondere l'interfaccia e F5 per la terza persona.

Ripeti le sezioni 1-3 per ognuna delle quattro varianti, ciascuna nella propria istanza.

## 1. Avvio dal jar di produzione

È la prova che il cloud non può dare. I due mixin del mod (`PlayerRendererMixin`, `ItemInHandLayerMixin`) sono obbligatori: se nel jar non si applicano, il client si chiude all'avvio o al primo disegno del giocatore.

- [ ] Avvia il client fino al menu principale: nessun crash, nessuna schermata d'errore del loader.
- [ ] Crea un mondo nuovo: **creativa, superpiatto, trucchi attivi**. Entra.
- [ ] In `logs\latest.log` cerca `redstone_exoframes.mixins.json` e `Mixin`: non deve esserci nessuna riga `ERROR` o `WARN` che li nomina, né `InvalidInjectionException` o `MixinApplyError`.
- [ ] Esegui `/exoframe debug wear nudo`: in chat "Wearing the bare exoframe" (o la traduzione), nessun crash.

Se il client si chiude: riporta il `crash-report` (cartella `crash-reports`) per intero e le ultime cento righe di `latest.log`, e passa alla variante successiva.

## 2. Gli stessi controlli del cloud

Accanto a ogni voce lo scatto di riferimento. Gli scatti del cloud sono del client di sviluppo con grafica "Rapida", campo visivo 70 e distanza di rendering 4: luci e dettagli del paesaggio possono differire, la tuta no. Per guardare da davanti: F5 due volte; da dietro: F5 una volta. Stai fermo su terreno piano.

- [ ] **Telaio nudo.** `/exoframe debug wear nudo`, F5 due volte. La tuta è alta circa tre blocchi, con i piedi a terra (non sprofonda e non galleggia); per misurarla metti accanto, a tre blocchi di distanza, una colonna di tre blocchi: la testa della tuta arriva più o meno alla sua cima. Il pilota si vede dalla vita in su dentro lo scheletro, con la testa nella testa della tuta; **le gambe del pilota non si vedono da nessun lato**. Riferimento: `m3a-screenshot/1.21.1-neoforge-nudo-fronte.png`, `m3a-screenshot/1.20.1-forge-nudo-fronte.png`.
- [ ] **Rottame, stato 0.** `/exoframe debug wear rottame`. Davanti e dietro, **nessuna parte della skin** si vede: né testa, né busto, né braccia, né gambe, nemmeno il secondo strato (giacca, cappello). Da dietro si vede la cella di redstone accesa dietro le sue sbarre. Riferimento: `m3a-screenshot/<variante>-armata-fronte.png` e `-armata-retro.png` (uno per variante).
- [ ] **Quattro stati.** `/exoframe debug wear rottame 0`, poi `1`, `2`, `3`: dal più pulito al più scuro e rovinato, nell'ordine; nello stato 3 la superficie è quasi carbone e metà delle spie è spenta. Riferimento: `m3a-screenshot/1.21.1-neoforge-stati.png`, `m3a-screenshot/1.20.1-forge-stati.png` (da sinistra stato 0, 1, 2, 3).
- [ ] **Colori di prova.** `/exoframe debug wear rottame 0 3A6EA5 D9D9D9`. Le piastre principali diventano blu, le secondarie grigio chiaro; restano dei loro colori il telaio sotto (acciaio, ottone, cavi), vetri, sbarre, feritoie e fessure rosse. Riferimento: `m3a-screenshot/1.21.1-neoforge-colori.png`, `m3a-screenshot/1.20.1-forge-colori.png`.
- [ ] **Oggetto in mano.** `/exoframe debug wear rottame`, poi un piccone di ferro nella mano principale e uno scudo nell'altra (`/item replace entity @s weapon.offhand with minecraft:shield`). Il piccone sta nella mano destra della tuta, grande come lei; lo scudo nella sinistra; nessuno dei due sotto i pugni della tuta o dentro il braccio. Riferimento: `m3a-screenshot/1.21.1-neoforge-mani.png`, `m3a-screenshot/1.20.1-forge-mani.png`.
- [ ] **Sdraiati.** `/exoframe debug wear nudo`, poi metti un blocco sopra la testa a un blocco d'altezza dai piedi (per esempio `/setblock ~ ~1 ~ minecraft:glass`): il gioco ti mette a strisciare. Tuta e pilota sono distesi insieme, a terra, la tuta non resta in piedi né sospesa. Guarda da dietro e di tre quarti: davanti, sdraiati, la telecamera finisce contro la testa. Riferimento (di tre quarti): `m3a-screenshot/1.21.1-neoforge-sdraiato.png`, `m3a-screenshot/1.20.1-forge-sdraiato.png`.
- [ ] **Via.** `/exoframe debug wear via`: la tuta sparisce, torna il giocatore normale, intero e a terra.
- [ ] Nel log, dopo tutte queste prove, nessuna riga `ERROR` o `WARN` che nomina `redstone_exoframes`, `exoframe`, `geckolib` o un mixin.

## 3. Ciò che il cloud non vede

- [ ] **Vista laterale.** Con la tuta Rottame e poi col telaio nudo, guarda di lato (in terza persona gira la visuale col mouse finché il corpo si gira, oppure da un secondo giocatore, sezione 4). Il pilota sta dentro la tuta anche di profilo: la testa non esce dal retro dell'elmo, il busto non esce dalla schiena. Fai uno screenshot di lato per ciascun caso.
- [ ] **Accovacciarsi.** Tieni premuto Shift con il telaio nudo e con il Rottame: la tuta si china con il pilota, i piedi restano a terra, il pilota non esce dal petto. Con un oggetto in mano, l'oggetto resta nella mano della tuta.
- [ ] **Correre.** Corri (doppio W o Ctrl) per qualche secondo in terza persona: braccia e gambe della tuta si muovono con quelle del giocatore, nessun pezzo resta indietro o sfarfalla.
- [ ] **Nuotare.** Entra in acqua profonda e nuota con lo scatto (posa da nuoto): tuta e pilota restano uniti, come da sdraiati.
- [ ] **Inventario.** Apri l'inventario in sopravvivenza (`/gamemode survival`, poi E): l'anteprima del giocatore mostra la tuta; annota se è tagliata dal riquadro (è grande, può succedere: riportalo con uno screenshot, non è un errore bloccante). Nessun crash.
- [ ] **Skin normale e sottile.** Ripeti telaio nudo e Rottame con una skin a braccia larghe (Steve) e con una a braccia sottili (Alex), dalle impostazioni della skin del launcher. Con la sottile le braccia del pilota stanno ancora nella tuta e l'oggetto impugnato resta nella mano della tuta.
- [ ] **Mantello.** Con un account che ha un mantello (se disponibile; altrimenti annota "non provato"): con la tuta il mantello si vede o no, e dove. Riporta uno screenshot da dietro; qualunque cosa si veda è un'informazione per Luigi, non c'è un risultato atteso.
- [ ] **Un secondo giocatore.** Avvia il server dedicato della variante con il nuovo jar (cartella del server di `m1-setup.md`), entra con due client (due account, o due istanze dello stesso launcher). Il primo esegue `/exoframe debug wear rottame 1 3A6EA5 D9D9D9` (serve essere operatore: `op <nome>` dalla console del server). Il secondo lo guarda da davanti, di lato e da dietro mentre il primo sta fermo, cammina, si accovaccia, tiene un oggetto: vede la stessa tuta, stesso stato e stessi colori, e il pilota dentro. La targhetta col nome del primo resta all'altezza normale di un giocatore (dentro la tuta, all'altezza del petto): è noto e voluto fino alla parte 3b, annotalo solo se è diverso. Poi `/exoframe debug wear via`: anche il secondo vede sparire la tuta.
- [ ] **Fluidità.** Con la tuta Rottame in terza persona, guarda il contatore F3 (fps) per un minuto confrontandolo con lo stesso posto senza tuta (`/exoframe debug wear via`). Annota i due valori; un calo grande (oltre un terzo) è da riportare. Nessuno scatto o blocco del gioco.
- [ ] **Prima persona.** In prima persona il braccio è quello normale del giocatore (la tuta in prima persona arriva in una milestone successiva): annotalo solo se è diverso.

## 4. Cosa riportare

Per ognuna delle quattro varianti:

- [ ] esito (OK / non OK) di ogni voce delle sezioni 1, 2 e 3;
- [ ] per la sezione 1, se il client non parte: crash report e ultime cento righe di `latest.log`;
- [ ] per ogni voce non OK: uno screenshot con l'interfaccia nascosta (F1) e una riga che dice cosa si vede di diverso dal riferimento (per esempio "gambe del pilota visibili sotto il petto", "scudo sotto il pugno sinistro", "tuta sospesa mezzo blocco da sdraiati");
- [ ] gli screenshot chiesti anche quando va tutto bene: Rottame di lato, accovacciati, anteprima nell'inventario, il mantello, la vista del secondo giocatore;
- [ ] ogni riga `ERROR` o `WARN` del log che nomina il mod, GeckoLib o un mixin, con qualche riga di contesto;
- [ ] gli fps con e senza tuta;
- [ ] versioni realmente usate (loader, Java, dipendenze) se diverse dalla tabella di `m1-setup.md`, e i passi saltati con il motivo.

Riepilogo finale: una tabella 4 righe (varianti) per le sezioni 1, 2 e 3, con OK o non OK, più i dettagli dei problemi.
