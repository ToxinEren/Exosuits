# Milestone 3 — Il telaio: progetto

Data: 13 novembre 2026. Stato: scritto dalla sessione cloud su delega di Luigi; le scelte segnate "scelta del costruttore" sono mie e correggibili. Fonti: documento di gioco (`claude-doc-2026-11-04-redstone-exoframes.md`), design operativo (`2026-11-12-redstone-exoframes-design.md`, §5.4, §7, §8), audit di Mjolnir (§9), note della revisione di M2b (`../plans/2026-11-13-m3-note-dalla-revisione-m2b.md`), ricognizione su GeckoLib e sugli hook dei loader (`ricognizioni/2026-11-13-m3-geckolib-e-hook-dei-loader.md`), `docs/compat.md`.

## 1. Intento

Alla fine della milestone 3 il mod smette di essere un'infrastruttura e diventa giocabile: si costruisce un telaio, lo si piazza, gli si montano i sei pezzi del primo livello (Rottame) e una cella, ci si entra, ci si muove con i suoi effetti e i suoi consumi, si prende danno sui pezzi, si esce, e alla morte il telaio resta nel mondo. Tutto con la grafica nuova, sulle quattro varianti (1.20.1 Forge e Fabric, 1.21.1 NeoForge e Fabric).

Perché ora: è il pezzo tecnicamente nuovo rispetto a Mjolnir e quello da cui dipendono stazione, HUD, moduli e alpha. È anche il primo punto in cui Luigi può vedere e provare qualcosa in gioco.

## 2. Obiettivi, non obiettivi, anti-obiettivi

**Obiettivi**

1. Telaio come entità piazzabile dall'item, con montaggio e smontaggio dei pezzi e della cella a mano.
2. Ingresso (tasto destro con cella carica inserita) e uscita (tasto `V` tenuto 1 s); slot armatura occupati da segnaposto bloccati; armatura civile stivata e restituita.
3. Morte: il telaio cade come entità con pezzi, cella e armatura stivata.
4. Effetti base: niente danno da caduta, resistenza al contraccolpo, velocità −15 %, niente nuoto, niente scatto sotto il 10 % di energia; penalità a energia zero e per pezzi rotti come da `core/`.
5. Energia: consumo per attività e per salto, cella da 10.000 RE, cambio cella dall'interno (2 s), caricatore a redstone.
6. Danno ripartito sui sei pezzi con il riporto frazionario; protezione calcolata dai pezzi integri.
7. Livello Rottame completo: item, ricette, valori dalla config.
8. Grafica nuova generata via codice: telaio nudo e sei pezzi Rottame con stati visivi, colori primario e secondario, zone riservate alle decalcomanie.
9. Il telaio di un altro giocatore si vede correttamente in multiplayer.
10. Test in mondo senza client (GameTest) nella CI, se la prova di fattibilità in corso lo conferma, più la checklist per il PC.

**Non obiettivi (arrivano dopo)**: stazione di manutenzione e GUI, verniciatura dal gioco, HUD, moduli attivi (M4–M5); disegno delle decalcomanie (entro M5, ma la disposizione delle texture le prevede già); braccio in prima persona con la grafica del telaio (M4); animazioni di apertura e posa di ingresso (M4); mondo e bottino (M6); compatibilità FE.

**Anti-obiettivi**: nessuna forma, nome o texture riconoscibile di Fallout, Iron Man o Halo; nessun asset di Mjolnir; nessun dato personale nei jar; nessuna differenza di versione o di loader fuori da `compat/` e `platform/`; nessuna perdita di pezzi, celle o armatura civile per morte, uscita, crash o riavvio del server (l'unica eccezione, la creativa con l'inventario del tutto pieno, è in §7, punto 26).

## 3. Vincoli

- Quattro varianti da un solo sorgente (Stonecutter + Architectury Loom); condizioni solo in `compat/impl`, `platform/` e `compat/Ids.java` (`tools/check_sources.sh`).
- `core/` resta Java 17 puro e testato con JUnit.
- Lo stato del telaio per giocatore vive nel `SavedData` (`FrameStateAccess`); le scritture a ogni tick passano da `mutate` e la sincronizzazione al proprietario resta sotto le 5 volte al secondo.
- I mixin si usano solo dove nessun evento copre il bisogno (scelta del costruttore, vedi §4.3 e §4.7). Oggi ce ne sono due, lato client, sul disegno del giocatore (`PlayerRendererMixin` e `ItemInHandLayerMixin`, §4.7). La parte 3b ne aggiunge due per la grandezza reale (§4.8): uno comune ai due lati su `Player`, per ingombro e altezza degli occhi (una classe per versione del gioco, senza logica), e uno client sul riquadro di culling. Tutto il resto passa da eventi Architectury o da hook per loader in `platform/`.
- La grafica è generata da script: i file generati sono committati, lo script li rigenera identici e la CI lo verifica.
- Licenza "tutti i diritti riservati"; il controllo di privacy sui jar resta verde (i PNG generati non hanno metadati).
- Dal 13 novembre 2026 il client vero gira anche nel cloud, senza schermo (Xvfb e OpenGL software), su tutte e quattro le varianti: uno script avvia client e server locale, esegue comandi e salva screenshot in terza persona. La resa e l'applicazione dei mixin si verificano lì; il PC di Luigi resta per la conferma e per ciò che lo script non copre (vista laterale vera, fluidità, audio, secondo giocatore).

## 4. Approcci considerati

### 4.1 Come arriva agli altri giocatori l'aspetto del telaio indossato

- **A. Pacchetto visivo dedicato più eventi di tracking per loader.** Preciso, ma Architectury non ha un evento di inizio tracking: servono tre hook per loader, una cache client con sfratto delle voci e la gestione di ingresso, rinascita e cambio di dimensione.
- **B. Dati visivi nello stack segnaposto del torso (scelto).** I quattro segnaposto stanno già negli slot armatura; vanilla sincronizza da sola gli stack degli slot a tutti i client che vedono il giocatore, su ogni loader e versione. Il segnaposto del torso porta, nei suoi dati (`ItemDataAccess`), un riassunto solo visivo: pezzi presenti con livello, stato visivo e aspetto. Il layer di rendering legge quel riassunto per qualunque giocatore. Nessun hook, nessuna cache da svuotare, corretto da solo a ingresso, rinascita e cambio di dimensione.
- **C. Sincronizzare a tutti lo stato completo.** Scartato: manda a tutti energia, proprietario e moduli.

Scelto B: meno codice e meno punti in cui sbagliare. Costo: il riassunto visivo si aggiorna quando cambia (pezzo montato, rotto, riverniciato), cioè di rado. Lo stato completo continua ad arrivare solo al proprietario con il pacchetto esistente.

### 4.2 L'entità telaio

- **A. `LivingEntity` stile armor stand con `GeoEntity`.** Porta salute, attributi, slot e import di GeckoLib diversi per versione.
- **B. `Entity` semplice, disegnata con lo stesso renderer del telaio indossato (scelto).** L'entità non tiene il `FrameState` (il contenuto sta nel registro, §4.5): ai client manda solo il riassunto visivo, con un dato sincronizzato di tipo `CompoundTag` (§5.4). Il suo renderer posa un `HumanoidModel` invisibile in posizione di riposo e chiama il renderer del telaio: un solo renderer GeckoLib per giocatore ed entità, e l'entità non è un `GeoAnimatable`.
- **C. Blocco con block entity.** Scartato: il documento di gioco vuole un'entità, e un blocco non si piazza dove il giocatore esce o muore.

### 4.3 Slot armatura bloccati

- **A. Maledizioni vanilla (Binding e Vanishing) sui segnaposto.** Niente codice, ma si vedono nel tooltip, non valgono in creativa e l'API degli incantesimi cambia tra le versioni.
- **B. Mixin sugli slot e sul rilascio alla morte.** Il più pulito per chi gioca, ma introduce i mixin (refmap, un punto in più che cambia tra versioni).
- **C. Autoriparazione senza mixin (scelto).** A ogni tick del server, per chi è nel telaio: i quattro segnaposto devono stare nei loro slot; se ne manca uno si rimette, se una copia è finita nell'inventario o sul cursore si elimina. Il lancio di un segnaposto (`PlayerEvent.DROP_ITEM`) si annulla su Fabric; su Forge e NeoForge Architectury ignora l'esito dell'evento, quindi l'oggetto a terra si svuota e sparisce da solo al suo primo tick. Alla morte i segnaposto si tolgono in `LIVING_DEATH`, prima che vanilla faccia cadere l'inventario. Vale anche in creativa. Per chi non è in un telaio (quasi tutti, quasi sempre) il controllo di ogni tick guarda solo i quattro slot armatura, e il resto dell'inventario (con cursore, griglia e menu aperto) una volta al secondo: un segnaposto rimasto lì senza telaio se ne va entro un secondo. In un menu aperto gli slot dell'inventario di un giocatore, anche di un altro (una mod che mostra l'inventario altrui), non si toccano mai: i segnaposto di ciascuno li tiene in ordine il suo controllo. Difetto: cliccando lo slot l'oggetto lampeggia per un tick; il mixin di rifinitura può arrivare in M4 senza cambiare il resto.

### 4.4 Grafica

- **A. Modelli fatti a mano in Blockbench.** Non disponibile: la grafica la fa la sessione cloud.
- **B. Generatore in Python nel repo (scelto).** Un piccolo linguaggio di descrizione (ossa, scatole, regioni di colore) produce geometria GeckoLib, atlante delle texture e anteprime renderizzate. I file generati sono committati. Luigi sceglie la direzione tra tre anteprime prima che il generatore definitivo entri nel repo.

### 4.5 Dove vive il contenuto di un telaio piazzato

- **A. Nell'NBT dell'entità.** Semplice, ma entità (dati dei chunk) e stato del giocatore (dato salvato del livello) si salvano in momenti diversi: un crash tra i due salvataggi può duplicare un telaio (entità ancora su disco e stato già sul giocatore) o perderlo (stato già tolto e entità non ancora scritta). Un'entità può anche sparire nel vuoto o per un comando.
- **B. In un registro unico nel dato salvato; l'entità è solo una vista (scelto).** Ogni telaio ha un'identità (`frameId`). Il dato salvato tiene tutti i telai: quelli indossati (per giocatore) e quelli piazzati (per `frameId`, con dimensione, posizione e orientamento). Entrare e uscire spostano un record tra le due mappe dentro lo stesso file: gli spostamenti dentro il registro sono atomici, e un crash non duplica né perde un telaio fra le due mappe. Ciò che passa fra l'inventario di un giocatore e il registro (armatura stivata e restituita, pezzi e celle montati e smontati, il telaio raccolto come oggetto) sta invece in due file, quello del giocatore e il registro: è sul disco ogni volta che lo è il file del giocatore, perché i salvataggi del gioco scrivono i giocatori e poi i livelli, e quando un giocatore lascia il server dopo un cambio strutturale del registro non ancora salvato (un telaio piazzato, raccolto, tolto, indossato o lasciato; un pezzo, una cella o l'armatura stivata messi o tolti; il blocco di un telaio piazzato) si salvano subito tutti i giocatori e il registro. Resta un arresto brusco nei millisecondi di un salvataggio. L'entità ha come UUID il `frameId`, non possiede il contenuto e si riallinea al registro: se il registro non ha un telaio piazzato con quell'identità l'entità si elimina (niente doppioni); se il registro ha un telaio piazzato in un chunk caricato e l'entità manca da qualche secondo, viene ricreata (niente perdite per crash, vuoto o `/kill`).

Scelto B: è l'unico che rispetta l'anti-obiettivo "nessuna perdita" con la sola finestra di rischio detta sopra. Il file del registro si scrive per intero accanto e prende il posto del vecchio in un colpo, lasciando accanto la copia del salvataggio precedente (`redstone_exoframes_frames.dat_old`, come fa il gioco per i file dei giocatori); se la scrittura fallisce il file vecchio resta e il salvataggio successivo riprova. Un file che non si legge non vale mai come registro vuoto: si usa la copia precedente; se nemmeno quella si legge, il file rovinato resta al suo posto (con una copia accanto, `.unreadable-<data>-<ora>`), un ERROR dice quale file ripristinare e per quella sessione i telai sono in sola lettura, così nulla viene sovrascritto e nessuno viene svestito per sempre. Costo: la logica di riallineamento, coperta da test in mondo.

### 4.6 Divisione del lavoro

Una milestone unica sarebbe di oltre trenta task. Divisa in tre parti, ognuna con piano, revisione e unione a `main` proprie (scelta del costruttore). Una parte si unisce a `main` quando build, test, test in mondo e server dedicato sono verdi nel cloud; le prove sul PC procedono in parallelo quando il PC è disponibile, e ciò che trovano diventa il primo task della parte successiva. Nessuna parte aspetta il PC per cominciare.

| Parte | Contenuto | Fatta quando |
|---|---|---|
| **3a — Vedere il telaio** | Test in mondo nella CI; generatore grafico e asset di telaio nudo e Rottame; renderer del telaio sul giocatore; segnaposto invisibili; comando di debug che "veste" il telaio | Sul PC, con un comando, il giocatore appare con il telaio e i pezzi Rottame su tutte e quattro le varianti; nel cloud build, test e server verdi |
| **3b — Entrare e uscire** | Aspetto (colori, decalcomanie riservate) nello stato; codec esposto; entità telaio; montaggio a mano; ingresso, uscita, morte; slot bloccati; riassunto visivo per gli altri giocatori | In gioco si piazza il telaio, lo si arma, ci si entra e se ne esce; alla morte resta nel mondo; un secondo giocatore lo vede |
| **3c — Giocarci** | Tick dell'energia e sincronizzazione; attributi ed effetti; danno sui pezzi; item e ricette Rottame; cella con carica; cambio cella; caricatore; checklist per il PC | Una partita completa: costruire, caricare, entrare, consumare energia, prendere danno, cambiare cella, ricaricare |

### 4.7 Tuta più grande del pilota (revisione del 13 novembre)

Luigi, viste le anteprime del primo telaio: «il telaio ci sta ma lo voglio più dettagliato e metallico, poi lo scheletro della tuta dovrebbe essere più grande del player, tipo mecha». Questo sostituisce la decisione 13 del design operativo ("il telaio non cambia le dimensioni del giocatore").

- **A. Ingrandire tutto insieme, giocatore e tuta.** È ciò che fa Pehkui e, su 1.21.1, l'attributo `scale`. Il pilota cresce con la tuta: resta una persona più grande in un'imbracatura, non uno scheletro più grande di chi lo pilota.
- **B. Tuta più massiccia senza scala.** Stessi perni del giocatore, pezzi più spessi, spalle più alte. Le gambe non possono allungarsi, perché i piedi del pilota sono a terra: risulta tozza, non più grande.
- **C. Tuta in scala attorno al pilota a terra.** La tuta si disegna S volte più grande attorno ai piedi e il pilota resta dov'è. Anche e mani della tuta finiscono più in alto di quelle del pilota, le due paia di gambe ruotano attorno a perni diversi e l'attrezzo impugnato compare sotto il pugno della tuta.
- **D. Tuta in scala, pilota sollevato (scelto).** Come C, ma il pilota è disegnato più in alto. Di quanto, lo ha deciso la risposta di Luigi alle prime tavole: «va bene 1.4 ma manca la testa della power armor, il player deve essere alzato da terra dalle gambe della power armor». Il pilota è allineato alla tuta a collo e spalle, cioè sollevato di 24·(S−1) pixel del modello: circa 0,56 blocchi con S = 1,4.
  - Testa, busto e braccia del pilota stanno dentro testa, busto e braccia della tuta e ruotano attorno agli stessi perni (le braccia a meno di un pixel).
  - Le gambe del pilota entrano nelle cosce della tuta e non vengono disegnate. Sotto, ginocchia, stinchi e piedi sono solo meccanica: sono loro a tenerlo sollevato.
  - Le mani del pilota arrivano a metà avambraccio, sui comandi. La tuta ha mani proprie, ed è lì che si disegna l'oggetto impugnato, ingrandito come la tuta.
  - Il telaio ha una testa vera (calotta, guance, mento, visiera aperta); il pezzo "elmo" la chiude.
  - Gli occhi del pilota finiscono a circa 2,18 blocchi da terra, vicino all'altezza che avrà la visuale con la grandezza reale (1,62 × 1,4 ≈ 2,27).
  - Prima versione di D, scartata dopo la risposta di Luigi: allineamento alle anche (sollevamento di 12·(S−1) pixel, poco visibile, con la testa del pilota nel collare e pedane sotto i piedi).

**Costo di D.** Per sollevare il pilota e per non disegnare le parti del corpo coperte dai pezzi bisogna intervenire prima che il gioco disegni il giocatore. Forge e NeoForge hanno un evento, Fabric no. Si usano due mixin comuni, lato client: uno su `PlayerRenderer` (sollevamento in coda a `scale`, parti nascoste dentro `render`) e uno su `ItemInHandLayer.renderArmWithItem` (oggetto impugnato nella mano della tuta). Le firme sono le stesse su 1.20.1 e 1.21.1. Sono la prima eccezione al "niente mixin" di §4.3.

**Scala.** S = 1,65. Luigi aveva scelto 1,4 sulle prime tavole (il costruttore proponeva 1,25); visti i primi screenshot dal gioco ha chiesto di più: «l'altezza dell'armatura sarebbe due blocchi e mezzo ma io voglio almeno 3 blocchi». Con 1,65 la tuta armata è alta circa 3,09 blocchi e il telaio nudo 3,02, larga circa 2,6 con i pezzi; il pilota è sollevato di 15,6 pixel del modello (0,91 blocchi) e in coordinate del telaio è una figura ridotta di 1/1,65 (testa tra y 24 e 28,85, mani a 16,73). Non di più perché con l'ingombro reale 1,8·S = 2,97 la tuta passa ancora sotto un soffitto di tre blocchi; oltre 1,66 no. Dove questo documento scrive ancora 1,4 o 9,6 pixel, vale il valore nuovo.

**Forma, come è stata decisa.** Quattro passate di tavole il 13 novembre, ognuna corretta da Luigi: (1) tuta grande e larga: «va bene 1.4 ma manca la testa della power armor, il player deve essere alzato da terra dalle gambe»; (2) testa e gambe meccaniche, ma più stretta: «perché lo hai reso più magro? l'allungamento/supporto oltre alle gambe dev'esserci anche per le braccia e il petto»; (3) stazza ritrovata, avambracci meccanici oltre le mani del pilota, petto grande attorno a lui; (4) dopo un'immagine di riferimento mandata da Luigi («rendilo più simile a questa immagine»): petto a botte e vita stretta, testa piccola incassata nel collare, spallacci a strati, braccia lunghe e grosse, gambe tozze, metallo color bronzo. Luigi: «mi piace l'aspetto dell'armatura rottame nelle ultime tavole». Il riferimento è l'armatura di un film: se ne prendono proporzioni e carattere, non gli elementi che la identificano (nessun disco luminoso tondo sul petto, nessuna copia del volto dell'elmo o delle spalle a cupola forate), come dice l'anti-obiettivo di §2.

**Eccezione sui piedi (decisione di Luigi, 13 novembre).** Dopo il ridisegno di piedi e schiena, Luigi ha chiesto che i piedi siano «uguali a quelli dell'immagine di riferimento, non mi interessa se sono troppo simili, devono essere identici solo quelli». Era stato avvisato che un design riconoscibile di un film lo espone a rimozioni o contestazioni; ha deciso così, per questa sola parte. La parte bassa delle gambe (piede, caviglia, scudo dello stinco, puntone laterale, polpaccio, tallone) riproduce quindi da vicino quella del riferimento, nei limiti di un modello fatto di scatole dritte. Il resto della tuta resta originale e l'anti-obiettivo di §2 continua a valere per tutto il resto.

Come si misura "uguali": il riferimento è stato ricampionato nei pixel del modello, con una griglia, e ogni elemento si mette dove la griglia lo indica (posizioni e larghezze al quarto di pixel). L'unica differenza voluta è la proporzione: la nostra tuta, approvata da Luigi, è più tozza di quella del riferimento, quindi la gamba bassa è larga 1,3 volte tanto a parità di altezza (scudo dello stinco 4,5 px su 8,5 di altezza). Il riferimento e la sua griglia restano fuori dal repository. Nella tuta armata tutto ciò che il riferimento mostra nel colore dell'armatura lo è anche qui, compresi puntone, tallone e piede; il telaio nudo, che il riferimento non mostra, tiene lo scheletro d'acciaio della stessa gamba.

**Braccia del pilota.** In una seconda immagine Luigi ha disegnato il pilota dentro l'armatura: coincide con la disposizione scelta (testa nella testa, busto nel petto, gambe nelle cosce con i piedi alle ginocchia) tranne le braccia, che nel suo disegno si allargano dentro i bracci della tuta. Nel gioco pendono, perché l'animazione è quella vanilla; col telaio nudo il pilota tiene due impugnature ai lati del petto e con i pezzi delle braccia montati non si vede. Allargarle richiede un aggancio sulla posa dopo `setupAnim`: proposto a Luigi insieme alle animazioni (M4).

**Grandezza reale o apparente.** Proposta "reale" fatta a Luigi il 13 novembre, senza obiezioni (§8). Riguarda ingombro e visuale, quindi la parte 3b; la 3a disegna soltanto.

### 4.8 Grandezza reale: come (parte 3b, 13 novembre)

Luigi ha scelto la grandezza reale: chi è nella tuta occupa più spazio e vede da più in alto. Come farlo sulle quattro varianti lo ha stabilito una ricognizione con prova (`ricognizioni/2026-11-13-m3b-grandezza-reale.md` e la patch accanto): bytecode dei quattro giochi, sorgenti dei loader, e un esperimento in un clone con test in mondo passati su tutte e quattro le varianti.

- **A. Mixin comuni su `Player` (scelto).** Su 1.20.1 si aggancia il ritorno di `getDimensions(Pose)` e di `getStandingEyeHeight(Pose, EntityDimensions)`; su 1.21.1 il ritorno di `getDefaultDimensions(Pose)`, che porta già occhi, targhetta e punto di seduta. Due classi senza logica (`mixin/PlayerSize120Mixin`, `mixin/PlayerSize121Mixin`), ciascuna elencata solo nella sezione `mixins` della configurazione della propria versione; la logica sta in `frame/SuitSize`, codice comune.
- **B. Eventi dei loader dove ci sono, mixin su Fabric.** Scartato: l'evento `Size` di Forge e NeoForge scatta solo nel costruttore e in `refreshDimensions`, mentre i controlli che decidono se il giocatore può stare in piedi o deve accovacciarsi, lo smontare da una cavalcatura e l'uscita dai portali leggono le dimensioni vanilla; chi è nella tuta starebbe in piedi con la testa in un soffitto alto due. Fabric e Architectury non offrono nulla.
- **C. L'attributo `scale` di 1.21.1.** Scartato: ingrandisce anche il modello disegnato (il pilota andrebbe rimpicciolito), non lascia scegliere per posa né la larghezza, non tocca passo e portata, e su 1.20.1 servirebbero comunque i mixin.

**Misure** (S = 1,65 in verticale; larghezza 0,8; occhi = quelli vanilla per S):

| Posa | Vanilla (larghezza × altezza, occhi) | Tuta |
|---|---|---|
| In piedi | 0,6 × 1,8, 1,62 | 0,8 × 2,97, 2,67 |
| Accovacciato | 0,6 × 1,5, 1,27 | 0,8 × 2,475, 2,10 |
| Nuoto, carponi, elitre, tridente | 0,6 × 0,6, 0,4 | 0,8 × 0,99, 0,66 |
| Sonno, morte | fisse | invariate |

- **Larghezza 0,8 e non 0,99 (0,6 · S) o 1,4.** Una scala a pioli, una porta aperta e una botola aperta lasciano 0,8125 di spazio: con 0,8 la tuta sale un pozzo 1 × 1 con la scala e passa un vano largo 1 con la porta aperta, con 0,99 no. Con 1,4 (golem di ferro) non passerebbe da nessun passaggio largo 1.
- **Altezza.** In piedi passa sotto un soffitto a 3 blocchi (margine 0,03); accovacciata passa sotto 2,5, non sotto 2: una porta alta due blocchi non si attraversa con la tuta. È la conseguenza voluta della grandezza reale.
- **Spazi troppo bassi.** Se l'ingombro cresce dove non c'è posto, il gioco mette da solo il giocatore a carponi (0,8 × 0,99): provato in una stanza alta 2. Per non arrivarci, l'ingresso nel telaio controlla che l'ingombro in piedi sia libero e altrimenti rifiuta con un messaggio; il telaio piazzato ha lo stesso ingombro della tuta in piedi (0,8 × 2,97), così dove lo si piazza ci si può anche entrare.
- **Chi indossa la tuta, per l'ingombro:** chi ha il segnaposto del torso nello slot del petto. La regola è la stessa su server e client e non si invia nulla: il segnaposto arriva già a tutti con gli slot. Il disegno chiede in più un riassunto leggibile (§5.2).
- **Ricalcolo.** Non esiste un evento di cambio equipaggiamento sui due lati e su tutti i loader: a ogni tick del giocatore (`TickEvent.PLAYER_PRE` di Architectury) si confronta "indossa la tuta" con l'ultimo valore visto e, se è cambiato, si chiama `refreshDimensions()`. Sul server lo si chiama anche subito dopo aver messo o tolto i segnaposto (ingresso, uscita, morte, comando di debug, guardia degli slot).
- **Targhetta del nome:** sale da sola a 3,47 su entrambe le versioni, sopra la testa della tuta. **Culling:** il riquadro cresce in altezza, ma le braccia della tuta con i pezzi restano circa 0,41 fuori ai lati; un mixin client su `LivingEntity.getBoundingBoxForCulling` (stessa firma sulle due versioni) lo allarga per chi indossa la tuta. Il disegno di pilota e tuta non cambia.
- **Costanti comuni.** S esce da `client/SuitScale` e va in codice comune (`frame/SuitSize`), dove la leggono client e server; il test che la tiene uguale al generatore resta. Non stanno nella config: server e client devono avere per forza gli stessi numeri.

**Rischi aperti** (dalla ricognizione): passeggeri in carrello o barca sotto soffitti bassi (da passeggero la posa non si adatta: nei tunnel alti 2 la testa sta nel blocco); risveglio dal letto in uno spazio basso; giocatori remoti visti da un secondo client (stesso percorso di codice, ma non visto); jar di produzione sul PC; altre mod che cambiano la grandezza. Passo (una tuta alta tre blocchi fermata da un gradino di uno), portata, salto e distanza della telecamera non cambiano in 3b: sono domande per Luigi (§8).

## 5. Il progetto

### 5.1 Grafica e generatore (3a)

- **Direzione artistica:** scelta da Luigi fra tre anteprime (spike in corso). Qualunque sia, valgono le convenzioni sotto.
- **Convenzioni della geometria** (dalla ricognizione, obbligatorie per `GeoArmorRenderer`): ossa di primo livello `armorHead`, `armorBody`, `armorRightArm`, `armorLeftArm`, `armorRightLeg`, `armorLeftLeg`, con i perni del modello armatura di GeckoLib (testa e corpo 0,24,0; braccia ∓5,22,0; gambe ∓2,12,0). Sotto ciascuna: `frame<Parte>` (telaio nudo, sempre visibile) e il pezzo, diviso per stato visivo e per regione di colore.
- **Proporzioni (revisione del 13 novembre, §4.7):** la geometria resta nelle coordinate standard del modello armatura, con i perni invariati, e viene resa in scala S = 1,65 attorno ai piedi (1,4 fino alla richiesta di Luigi di una tuta alta almeno 3 blocchi). In quelle coordinate il pilota è una figura ridotta di 1/S attorno al punto (0, 24, 0): testa tra y 24 e 28,85, busto tra 16,73 e 24, mani a 16,73; le gambe non si disegnano. Testa della tuta, cosce chiuse, bacino e impugnature dei comandi si disegnano attorno a quella figura. Limiti dell'ingombro del disegno approvato da Luigi (quarta passata della prova d'arte): telaio nudo entro x ±12,75, y 0..31,25, z −8..10; con i pezzi entro x ±13,5, y 0..32, z −8,75..10,5 (i limiti che usano il generatore, `exo.FRAME_MIN/MAX` e `exo.SUIT_MIN/MAX`, e `ExoframeGeometryTest`, aggiornati con la scala 1,65) (circa 2,5 e 2,6 blocchi di larghezza). Il telaio della seconda passata, più stretto, è stato scartato da Luigi («perché lo hai reso più magro?»).
- **Densità e resa (revisione del 13 novembre):** due texel per pixel del modello, quindi un atlante più grande; pittura da metallo: spazzolatura, bordi chiari consumati, incavi scuri, bulloni con punto luce. Unica eccezione, per la parte bassa delle gambe (§4.7): le scatole che portano righe sottili (feritoie, tacche) hanno 8 texel per pixel; su quelle facce la pittura di base è la stessa a due texel per pixel, ingrandita, e solo le righe usano la risoluzione fine.
- **Stati visivi senza passate multiple (scelta del costruttore):** ogni pezzo ha quattro copie della sua geometria, una per stato (integro, danneggiato, critico, rotto), che puntano a quattro zone diverse dello stesso atlante; il renderer mostra una sola copia. Una sola texture e una sola passata per il telaio intero.
- **Colori:** dentro ogni copia, tre ossa per regione: primaria, secondaria (texture in toni di grigio, colorate dal renderer) e neutra (metallo, cavi, non colorata). Nomi: `piece<Parte>_s<0-3>_p1`, `_p2`, `_n`.
- **Decalcomanie (riservate):** un rettangolo piatto sul davanti del torso e uno sulla spalla sinistra, con ossa `pieceTorso_decal` e `pieceLeftArm_decal` presenti ma nascoste fino a M5.
- **Luci di redstone:** maschera luminosa per le parti rosse (layer `AutoGlowingGeoLayer` di GeckoLib) se entra nel tempo di 3a, altrimenti in M4.
- **Generatore:** `tools/art/` in Python (PIL, numpy): descrizione del modello, impacchettamento delle facce nell'atlante, pittura procedurale deterministica (seme fisso), esportazione di `geo/exoframe.geo.json`, `textures/entity/exoframe.png`, eventuale `_glowmask`, `animations/exoframe.animation.json` vuoto. Un comando rigenera tutto; un controllo nella CI rigenera e confronta con i file committati. Le anteprime (three.js in Chromium senza schermo) restano uno strumento locale, fuori dalla CI.
- **Controllo senza gioco:** un test JUnit legge il `.geo.json` generato e verifica che esistano tutte le ossa che il codice nomina e che i perni siano quelli richiesti.

### 5.2 Rendering (3a)

Dalla ricognizione, opzione (a): un `RenderLayer` vanilla sul renderer del giocatore guida un `DyeableGeoArmorRenderer` con uno stack fittizio.

- `compat/client/SuitRenderer` (interfaccia) e `compat/impl/client/SuitRendererImpl`: `render(entità, modello umanoide di base, PoseStack, buffer, luce, partialTick, SuitView)`. Nasconde pacchetti, colore a quattro float o intero, `prepForRender` a 4 o 10 argomenti.
- `SuitView` (record neutro, in `compat/client`): per ogni slot se il pezzo c'è, livello, stato visivo (0–3), colori; più "telaio presente".
- `compat/impl/client/SuitModelItem`: l'unico `GeoItem`, mai equipaggiato, registrato ma fuori dalla scheda creativa.
- `client/SuitLayer`: codice comune; costruisce la `SuitView` dal riassunto visivo e chiama `SuitRenderer`.
- `compat/ClientRenderHooks`: `addPlayerLayer` (Fabric `LivingEntityFeatureRendererRegistrationCallback`; Forge `AddLayers` con le skin per nome; NeoForge `AddLayers` con `PlayerSkin.Model`) e la registrazione del renderer dell'entità (Architectury).
- **Scala e pilota (revisione del 13 novembre, §4.7):** `client/SuitScale` tiene S e il sollevamento del pilota. `client/SuitLayer` disegna la tuta in scala S attorno ai piedi. Per chi indossa il telaio, un mixin client su `PlayerRenderer`, comune alle quattro varianti, sposta in alto il disegno del giocatore di 24·(S−1) pixel del modello (nello spazio del modello, in coda a `scale`: così pilota e tuta restano uniti e a terra in ogni posa, anche da sdraiati) e, dentro `render` subito dopo `setModelProperties`, toglie dal disegno le gambe (sempre) e le parti coperte da un pezzo montato: testa con l'elmo, busto con il torso, ogni braccio con il suo pezzo, secondo strato della skin compreso. Un secondo mixin su `ItemInHandLayer.renderArmWithItem` porta l'oggetto impugnato nella mano della tuta, partendo dal perno corrente del braccio, e lo ingrandisce di S. La targhetta col nome resta all'altezza vanilla finché la parte 3b non dà alla tuta l'ingombro reale. Con il telaio nudo il pilota si vede dalla vita in su dentro lo scheletro, ed è voluto. La prima persona non cambia: il braccio resta quello vanilla fino a M4.
- **Segnaposto invisibili:** su 1.21.1 il materiale `frame` passa a `layers = List.of()`; su 1.20.1 il materiale si chiama `redstone_exoframes_frame` (senza namespace, con il prefisso del mod perché non coincida con un materiale di altre mod o di un pacchetto di risorse) e si aggiungono due texture trasparenti `redstone_exoframes_frame_layer_1.png` e `redstone_exoframes_frame_layer_2.png` nel namespace `minecraft`.
- **Comando di debug (3a):** `/exoframe debug wear [rottame|nudo|via]` mette o toglie al giocatore uno stato di prova e i segnaposto, per vedere il risultato sul PC prima che esista l'entità.

### 5.3 Stato e dati (3b)

- **Aspetto (già fatto nella 3a):** in `core`, `Appearance(int primary, int secondary, List<Decal> decals)` con `Appearance.DEFAULT`, e `Decal(String pattern, String color)` (id del registro e nome della tinta come stringhe: forma che sopravvive al passaggio 1.20.1 → 1.21.1). `ItemDataAccess.PieceData` sostituisce i due interi dei colori con `Appearance`; `FrameState` tiene un `Appearance` per slot. Nuove chiavi NBT: `decals` (lista di `{pattern, color}`); i colori restano `color_primary` e `color_secondary`.
- **Ripristino fidato:** `FrameStateCodec` ottiene una lettura che non rivalida i moduli contro la config di chi legge, usata dal client per lo stato che riceve dal server (l'entità del telaio piazzato manda ai client solo il riassunto visivo); il caricamento sul server continua a validare. Chiude il limite (a) di `docs/compat.md`.
- **Registro dei telai (vedi §4.5):** il dato salvato esistente si estende. Ogni record ha `frameId`, `FrameState` e armatura stivata. I record indossati restano indicizzati per giocatore (come oggi); i record piazzati sono indicizzati per `frameId` e portano dimensione, posizione e orientamento. `FrameStateAccess` ottiene le operazioni di registro: `place(record, posizione)`, `enter(ServerPlayer, frameId)` (sposta il record da piazzato a indossato e sincronizza una volta), `exit(ServerPlayer, posizione)` (l'inverso), `placed(frameId)`, l'elenco dei piazzati di una dimensione (per il riallineamento), la modifica di un record piazzato (montaggio a mano), `remove(frameId)`, più la scrittura e la lettura del riassunto visivo come `CompoundTag` per i dati sincronizzati dell'entità. L'armatura stivata ricorda lo slot di ogni oggetto, così all'uscita torna dov'era. Lo schema del dato salvato passa a 2; un file di schema 1 si legge ancora (ogni stato per giocatore diventa un record indossato con un'identità nuova). Nulla di ciò che il registro contiene si perde: una voce che non si legge resta nel file così com'è (riscritta identica a ogni salvataggio, sotto `<chiave>~unread` se un record vivo prende la sua chiave), e l'armatura stivata ancora dentro un record che esce dal registro (telaio tolto, stato indossato cancellato) passa identica nella lista `orphaned`, con l'identità del telaio e il proprietario (dettagli in `docs/compat.md`, "Schema NBT"). Il file si scrive e si legge come in §4.5.
- **Armatura stivata:** lista di stack dentro il record, mai inviata ai client. Gli stack si salvano con il provider su 1.21.1 e portano accanto la versione dei dati di Minecraft (`data_version`): al caricamento su 1.21.1 di stack salvati su 1.20.1 si passa dal correttore di dati vanilla prima di leggerli, così l'armatura sopravvive all'aggiornamento del mondo.
- **Riassunto visivo (tipo e chiavi già fatti nella 3a):** `SuitSummary` (pezzi presenti, livello, stato visivo, aspetto per slot), scritto nei dati del segnaposto del torso sotto la chiave `visual` e letto dal layer. In 3b lo scrive lo stato vero: all'ingresso e a ogni cambio di pezzo. Si riscrive solo quando cambia.
- **Pacchetto al proprietario (spostato alla 3c, dove c'è chi lo usa):** oltre allo stato porta tre indicatori calcolati dal server (`canSprint`, `canSwim`, stato dell'energia), così il client non valuta le regole con la propria config.

### 5.4 Entità telaio (3b)

- `entity/ExoframeEntity extends CompatEntity` (`compat/CompatEntity`: dati sincronizzati con o senza `Builder`, esplosioni, portali; altezza degli occhi e pacchetto di spawn restano quelli di `Entity`, uguali sulle due versioni; è una classe astratta senza direttive che estende una base in `compat/impl/`, dove stanno le differenze di versione). Immobile e senza gravità: quando nasce il record piazzato la posizione si appoggia al suolo sotto il punto richiesto. Chi piazza su richiesta di un giocatore (l'oggetto, `/exoframe debug place`) rifiuta se sotto non c'è suolo o se il punto è fuori dal bordo del mondo; l'oggetto rifiuta anche se il suolo è più di tre blocchi sotto il punto. Uscita e morte invece non si fermano per la mancanza di suolo: se sotto il giocatore non c'è suolo (uscita a mezz'aria sopra il vuoto, morte nel vuoto) o il suolo è fuori dal bordo, vale l'ultima posizione sicura e poi il punto di rinascita del mondo (§5.5); il record è la verità sulla posizione. Persistente, con il riquadro della tuta in piedi (0,8 × 2,97, §4.8), così si clicca anche la testa e dove lo si piazza ci si può entrare.
- L'entità è una vista del record piazzato con il suo stesso UUID (`frameId`): nel suo NBT salva solo l'identità; il contenuto sta nel registro. Ai client manda il riassunto visivo come `CompoundTag` sincronizzato, aggiornato quando il record cambia.
- Riallineamento: al primo tick dopo il caricamento, se il registro non ha un record piazzato con quell'identità l'entità si elimina; il server controlla ogni pochi secondi i record piazzati nei chunk caricati e ricrea l'entità mancante. Per togliere davvero un telaio si passa dal registro (raccolta come item, `/exoframe remove`).
- Item `exoframe`: crea un record nuovo (identità nuova, il giocatore come proprietario) e l'entità nel blocco accanto alla faccia cliccata, appoggiata al suolo sotto di esso, rivolta verso il giocatore; rifiuta, senza consumare l'oggetto, se lì l'ingombro del telaio non è libero (blocchi o un altro telaio), se il suolo manca o è più di tre blocchi sotto (sul fianco di un dirupo il telaio non va in fondo) o se il punto è fuori dal bordo del mondo. In 3b entrano anche, senza ricette, gli oggetti che servono a montare a mano: i quattro pezzi Rottame e la cella con la carica nei dati; ricette, caricatore e bilanciamento restano alla 3c (§5.8).
- Permessi: ogni interazione con un telaio piazzato (entrare, montare, smontare, togliere la cella, ritirare l'armatura stivata, raccoglierlo) richiede di essere il proprietario, oppure che il telaio sia sbloccato o senza proprietario, oppure che l'opzione `server.ownerOnlyFrames` della config sia falsa; un operatore in creativa può sempre.
- Interazioni (senza GUI, scelta del costruttore; il clic di uno spettatore o di un giocatore morto non fa nulla):
  - con un pezzo in mano: monta il pezzo nel suo slot se libero; braccia e gambe usano un solo item per lato e vanno sul lato cliccato, o sul primo libero;
  - con una cella in mano: inserisce la cella se manca;
  - mano vuota: entra, se il giocatore è il proprietario o il telaio è sbloccato (o la config lo consente) e c'è una cella con carica;
  - accovacciato a mano vuota: se il telaio contiene armatura stivata la restituisce tutta (inventario, o a terra se pieno); altrimenti smonta il pezzo della parte cliccata (gambe sotto 1,1 blocchi dai piedi, busto e braccia fino a 2,2, testa sopra; braccia e gambe dal lato cliccato) o, se lì manca, quello più vicino della stessa fascia d'altezza; se la fascia è vuota toglie la cella; se il telaio è del tutto vuoto lo raccoglie come item e cancella il record.
- Danni: l'entità non si distrugge a colpi né con le esplosioni. Se sparisce (vuoto, comando), il registro la ricrea all'ultima posizione registrata.

### 5.5 Ingresso, uscita, morte (3b)

- **Ingresso:** prima di tutto il server controlla, in quest'ordine, il permesso, una cella con carica, che chi entra non sia già in un telaio, che l'ingombro in piedi della tuta sia libero nel punto del telaio (§4.8) e, se il record contiene ancora armatura stivata che si legge (telaio lasciato da una morte), che chi entra ne sia il proprietario; altrimenti rifiuta con un messaggio, senza toccare nulla. Quell'armatura esce solo con un atto voluto: la tira fuori accovacciato a mano vuota chi può toccare il telaio, oppure il proprietario la ritrova uscendo. Entrando non si consegna nulla e nulla cade a terra: l'armatura già stivata resta nel record, accanto a quella che il giocatore indossa entrando (due voci per lo stesso slot vanno bene), e torna all'uscita con il resto. Un giocatore morto o spettatore non entra. Poi il server fa `enter` (il record passa da piazzato a indossato), stiva nel record l'armatura indossata, mette i quattro segnaposto con il riassunto visivo, porta il giocatore nel punto e con l'orientamento del telaio ed elimina l'entità. Gli slot armatura cambiano in silenzio, senza suoni. Un solo tick.
- **Uscita:** tasto `V` (`KeyMappingRegistry`) tenuto 1 s sul client, poi pacchetto C2S `frame_exit`; il server verifica che il giocatore sia nel telaio, toglie i segnaposto, restituisce l'armatura civile agli slot, ogni voce nell'ordine in cui è stata stivata (se uno slot è occupato l'oggetto va nell'inventario, e se anche quello è pieno cade ai piedi del giocatore), fa `exit` e piazza il record nel punto di riposo (sotto). L'entità nasce subito solo se il chunk di quel punto ha le entità caricate; altrimenti la crea il riallineamento quando il chunk si carica. **Passo fuori:** se gli occhi del giocatore stanno nel riquadro del telaio appena lasciato (il clic successivo prenderebbe il telaio invece di qualunque blocco), il giocatore va in un posto libero a un blocco dal centro del telaio, nell'ordine davanti (dove guarda), a sinistra, a destra, dietro; un posto va bene se c'è spazio per un giocatore vanilla in piedi, il pavimento è al massimo un blocco sotto, sta dentro il bordo del mondo e non ci sono lava né fuoco. Senza un posto così (un pozzo largo uno, una sporgenza) il giocatore resta dov'è. Tiene la direzione in cui guarda. Ciò che non è entrato nell'inventario resta ai piedi del telaio, a un blocco dal giocatore. Per un giocatore morto o spettatore il tasto non fa nulla.
- **Morte:** in `LIVING_DEATH` il server toglie i segnaposto e fa `exit` lasciando l'armatura stivata nel record. **Punto di riposo**, uguale per uscita e morte: il suolo sotto la posizione del giocatore; se sotto non c'è suolo (vuoto, o sotto il limite inferiore del mondo) o il suolo è fuori dal bordo del mondo, l'ultima posizione sicura, in qualunque dimensione sia (l'ultima in cui il giocatore nel telaio era a terra, ricordata una volta al secondo e all'ingresso): se il suo chunk è caricato si appoggia al suolo che c'è ora sotto di essa, e se lì non c'è più suolo, o se sta fuori dal bordo, non vale; se il chunk non è caricato vale così com'è stata ricordata. In mancanza di una posizione sicura che valga, il punto di rinascita del mondo (quello dell'overworld), appoggiato al suolo sotto se c'è; qui il bordo del mondo non si controlla. Quando il telaio non resta sotto il giocatore un messaggio in chat gli dice dove è stato lasciato (coordinate e dimensione); ogni evento del telaio (piazzato, raccolto, rimosso, indossato, lasciato, lasciato alla morte) ha una riga INFO nel log del server con identità, giocatore, dimensione e posizione. L'entità nasce come all'uscita: subito solo dove il chunk ha le entità caricate, altrimenti dal riallineamento. I segnaposto si tolgono da ogni posto in cui il giocatore tiene oggetti (griglia di fabbricazione e menu aperto compresi) prima che il gioco faccia cadere l'inventario. Vale anche con `keepInventory`.
- **Uscita dal gioco e riavvio:** lo stato resta nel dato salvato e i segnaposto nell'inventario; al rientro l'autoriparazione riallinea slot e stato (stato presente senza segnaposto: li rimette; segnaposto senza stato: li toglie). Quando un giocatore lascia il server e il registro ha un cambio strutturale non ancora salvato, il server salva subito tutti i giocatori e il registro (§4.5).
- **Proprietario:** `owner` e `unlocked` come da `FrameState`; opzione di config `server.ownerOnlyFrames` (predefinita vera; la lettura della config da file arriva con la 3c, in 3b valgono i valori predefiniti). L'interruttore di sblocco dal gioco arriva con la stazione; in 3b ci sono `/exoframe unlock` e `/exoframe lock`, che il proprietario usa sul telaio che indossa o su quello piazzato che sta guardando. `/exoframe remove` e `/exoframe debug` restano agli operatori.
- **Comando di debug:** `/exoframe debug wear` crea un vero record indossato (nudo o Rottame), così l'autoriparazione non lo scambia per segnaposto rimasti senza stato; `via` lo cancella con i suoi pezzi e la sua cella (anche quello di un telaio in cui l'operatore è entrato davvero) e gli restituisce l'armatura stivata. `/exoframe remove` manda la conferma anche alla console e agli altri operatori, come i comandi di amministrazione del gioco.

### 5.6 Tick, effetti, energia (3c)

- Tick del server per ogni giocatore nel telaio: attività (fermo, cammina, scatta) dal movimento; salti contati dalla statistica vanilla dei salti (il server la incrementa a ogni salto); `FrameRules.tick` e `jump` tramite `mutate`; un `EnergyMeter` per giocatore in una mappa temporanea svuotata all'uscita dal server.
- Sincronizzazione al proprietario: subito ai cambi di struttura (pezzo, cella, stato dell'energia), altrimenti al massimo 4 volte al secondo quando l'energia è cambiata.
- Attributi: `FrameAttributes.apply` diventa idempotente (non tocca il modificatore se valore e operazione sono già quelli) e viene chiamata quando cambiano gli effetti, al login e alla rinascita.
- Effetti non numerici: danno da caduta annullato in `LIVING_HURT`; scatto spento sotto il 10 % su server e client; in acqua niente nuoto, si affonda e si cammina sul fondo (lato client per il giocatore locale, perché il movimento lo decide il client; i dettagli al costruttore, da provare sul PC).
- Cambio cella: tasto dedicato, 2 s di attesa lato client con indicatore nella barra delle azioni, poi C2S `cell_swap`; il server scambia la cella inserita con la prima cella carica dell'inventario. Animazione rimandata.

### 5.7 Danno (3c)

- In `LIVING_HURT`, per un giocatore nel telaio: se il danno aggira l'armatura si salta; altrimenti `DamageClassifier` → `DamageRouter.route` con il riporto, con il danno in millesimi (nuova firma che accetta l'importo in millesimi, così le frazioni entrano nel riporto) → `Piece.damage` → `mutate`. L'usura si calcola sul danno prima dell'armatura, come fa vanilla.
- La protezione arriva dagli attributi (armatura e robustezza dei pezzi integri): in M3 non serve modificare il danno. Il punto d'aggancio per ridurlo (resistenza ai proiettili del livello Corazzata) arriva in M5.
- Quando un pezzo cambia stato visivo si riscrive il riassunto visivo; quando si rompe, gli effetti si ricalcolano.

### 5.8 Contenuti Rottame (3c)

- Item: `exoframe`, `scrap_helmet`, `scrap_torso`, `scrap_arm`, `scrap_leg`, i quattro segnaposto `exoframe_helmet`, `exoframe_torso`, `exoframe_legs`, `exoframe_boots` (fuori dalla scheda creativa), `redstone_cell` (già registrata: diventa la cella vera, con carica nei dati e barra di carica), `charger` (blocco).
- Ricette al banco da lavoro: telaio con ferro, rame e redstone; pezzi Rottame con ferro e rame; cella con redstone, rame e quarzo; caricatore con ferro, rame e blocco di redstone. Forme esatte al costruttore, da bilanciare.
- Caricatore: blocco con block entity (`compat/CompatBlockEntity`), senza GUI: tasto destro con una cella la inserisce o la ritira, con polvere di redstone aggiunge combustibile (1 polvere = 500 RE, dalla config); carica a un ritmo dalla config; compatibile con le tramogge se costa poco.
- Dati per versione: ricette, tabelle di bottino e tag hanno cartelle e formato diversi tra 1.20.1 e 1.21.1. Un generatore (`tools/gen_data.py`) scrive i due alberi in `src/main/resources-mc1.20.1/` e `src/main/resources-mc1.21.1/`; la build aggiunge quello della variante; i file sono committati e la CI verifica che siano aggiornati.
- Testi in inglese e italiano per ogni item, blocco, tasto e messaggio.

### 5.9 Test

- **JUnit:** aspetto e codec, riassunto visivo, firma in millesimi del `DamageRouter`, idempotenza degli attributi dove testabile, controllo della geometria generata.
- **GameTest (se la prova in corso lo conferma):** registrazione per loader in `platform/`, classe di test comune, un passo nella CI per variante. Casi: ingresso e uscita con armatura civile restituita; morte che lascia l'entità con tutto dentro, e rientro e nuova uscita che restituiscono l'armatura; entità doppia che si elimina; entità eliminata che viene ricreata; morte nel vuoto che lascia il telaio all'ultima posizione sicura; permessi del non proprietario; stack stivato in formato 1.20.1 letto su 1.21.1; autoriparazione dei segnaposto; consumo di energia su N tick; usura dei pezzi da un danno noto; caduta senza danno; caricatore che carica.
- **Server dedicato:** avvio e arresto sulle quattro varianti a ogni parte.
- **PC:** una checklist per parte in `docs/test-in-gioco/` (resa grafica, ingresso e uscita, multiplayer con due client se possibile).

## 6. Cosa resta al costruttore

Forme esatte delle ricette; valori di ritmo del caricatore e soglie di movimento per l'attività; dettaglio dell'affondamento in acqua; disposizione interna dell'atlante; nomi dei messaggi e dei suoni (suoni vanilla in M3); struttura interna dei pacchetti; se la maschera luminosa entra in 3a; compatibilità del caricatore con le tramogge.

## 7. Scelte del costruttore da far vedere a Luigi

1. Divisione in tre parti (3a, 3b, 3c), ognuna unita a `main` quando è verificata.
2. L'aspetto del telaio viaggia nei dati del segnaposto del torso, non in un pacchetto dedicato.
3. Slot bloccati per autoriparazione, senza mixin: cliccando lo slot l'oggetto lampeggia per un istante.
4. Niente GUI in M3: pezzi e cella si montano e smontano a mano sul telaio; lo sblocco per altri giocatori è un comando fino alla stazione.
5. Ingresso immediato e uscita con `V` tenuto, senza animazioni di apertura: arrivano in M4.
6. In prima persona il braccio resta quello vanilla fino a M4.
7. Un solo item per braccio e uno per gamba, validi per entrambi i lati.
8. Il contenuto di ogni telaio vive in un registro unico sul server e l'entità nel mondo ne è solo la vista: un telaio non si duplica e non si perde per crash, vuoto o comandi; per eliminarlo si raccoglie da vuoto o si usa un comando.
9. Un telaio bloccato non può essere toccato da altri giocatori in nessun modo (non solo l'ingresso); uno sbloccato è accessibile a tutti; un operatore in creativa può sempre.
10. L'armatura civile rimasta in un telaio dopo una morte resta nel telaio quando il proprietario rientra e gli torna nei suoi slot quando ne esce; oppure va a chi può toccare il telaio quando la ritira accovacciandosi a mano vuota; un altro giocatore non può entrare finché è dentro.
11. Chi muore nel vuoto ritrova il telaio nell'ultimo punto sicuro in cui era a terra.
12. Le parti 3b e 3c non aspettano le prove sul PC: si procede nel cloud e ciò che il PC trova si corregge all'inizio della parte successiva.
13. La tuta è più grande del pilota (scala 1,65: Luigi aveva scelto 1,4 e poi ha chiesto almeno tre blocchi di altezza). Il pilota vi sta dentro allineato a collo e spalle, sollevato di circa 0,9 blocchi dalle gambe meccaniche della tuta (§4.7).
14. Due mixin lato client (disegno del giocatore e oggetto impugnato): prima eccezione al "niente mixin".
15. Le gambe del pilota non si disegnano mai mentre è nella tuta; testa, busto e braccia non si disegnano quando il loro pezzo è montato.
16. L'oggetto impugnato si disegna nella mano della tuta, ingrandito come la tuta.
17. Grandezza reale con due mixin in più: uno comune su `Player` per ingombro e altezza degli occhi, uno client per il riquadro di culling (§4.8).
18. Ingombro della tuta 0,8 × 2,97 in piedi e 0,8 × 2,475 accovacciata: passa sotto tre blocchi e da un vano largo uno, non da una porta alta due. A carponi (0,8 × 0,99) ci finisce solo come ripiego automatico del gioco, quando non c'è altro posto.
19. Non si entra in un telaio se lo spazio per la tuta in piedi non è libero.
20. Il telaio piazzato sta fermo e non cade: alla creazione si appoggia al suolo sotto il punto di uscita o di morte; ha lo stesso ingombro della tuta.
21. Telaio, pezzi Rottame e cella con carica esistono come oggetti già in 3b (dalla scheda creativa), senza ricette: quelle arrivano con la 3c.
22. `/exoframe unlock` e `lock` sono del proprietario; `debug` e `remove` degli operatori. Il comando di debug crea un telaio vero addosso al giocatore.
23. Gli indicatori del pacchetto al proprietario e la lettura della config da file passano alla 3c.
24. Uscendo dal telaio il giocatore fa un passo fuori, a un blocco dal centro del telaio (davanti, poi a sinistra, a destra, dietro), così il clic successivo non prende il telaio; se non c'è posto resta dov'è (§5.5).
25. L'oggetto telaio non piazza dove il suolo è più di tre blocchi sotto il punto, né fuori dal bordo del mondo; uscita e morte fuori dal bordo mandano il telaio all'ultima posizione sicura (§5.4, §5.5).
26. **Eccezione al "nessuna perdita" (creativa con l'inventario pieno).** Ciò che la mod restituisce e non entra nell'inventario cade a terra fermo, con il ritardo di raccolta di un oggetto lanciato (due secondi), così nessuno lo raccoglie o lo cancella nel tick in cui arriva. Dopo, un giocatore in creativa con l'inventario del tutto pieno che ci passa sopra lo cancella: è la regola del gioco base per la creativa, e resta. È l'unico caso in cui qualcosa si perde, e solo in creativa; è fra le domande per Luigi (§11).

## 8. Domande aperte per Luigi

1. **La direzione artistica dei pezzi Rottame.** Tre anteprime mostrate il 13 novembre sul primo telaio; con il telaio ridisegnato (§4.7) i pezzi si ripropongono sulla nuova forma. Se non risponde, si procede con quella che il costruttore raccomanda, e resta sostituibile.
2. **Grandezza reale: i casi limite.** La scelta è fatta (reale, Luigi) e il come è in §4.8. Restano per Luigi: (a) a carponi l'ingombro è 0,8 × 0,99, quindi una tuta che non ha altro posto striscia anche in un cunicolo alto uno; con un'altezza oltre 1 resterebbe incastrata invece di strisciare; (b) accovacciarsi più in basso per passare le porte alte due: possibile, ma la tuta disegnata attraverserebbe l'architrave e la vista scenderebbe di quasi un blocco; proposta del costruttore: no; (c) carrelli, barche, cavalcature e letti con la tuta: vietarli o accettare i casi limite (testa nel soffitto dei tunnel alti due, risveglio in uno spazio basso); (d) passo di un blocco, portata e distanza della telecamera adeguati a una tuta alta tre blocchi: economici su 1.21.1 e Forge, un po' meno su Fabric 1.20.1; proposta: il passo sì, nella 3c con gli altri attributi.

## 9. Rischi

- **Resa in gioco:** perni del modello, compenetrazioni con la skin, costo del rendering. Dal 13 novembre si verifica nel cloud con il client senza schermo; il costo del rendering e la fluidità restano da misurare sul PC.
- **GameTest su quattro varianti:** se una variante non regge, lì restano test JUnit, server dedicato e checklist.
- **Riallineamento tra registro ed entità:** è la parte più delicata di 3b; un errore lì può creare proprio i doppioni o le perdite che vuole evitare. Mitigazione: test in mondo dedicati prima di costruirci sopra ingresso e uscita.
- **Movimento deciso dal client** (scatto, nuoto): serve codice sui due lati e prova sul PC.
- **Mixin client su quattro varianti (§4.7):** un bersaglio sbagliato fa crollare il gioco all'avvio. Mitigazione: firme verificate sui jar vanilla e su quelli patchati dai loader, controllo in `tools/check_jars.sh` che la configurazione dei mixin sia nei jar e che i nomi nelle annotazioni abbiano la forma giusta per ogni loader (questa versione di Loom non produce refmap), avvio del client vero di ogni variante nel cloud; il 13 novembre i due mixin si sono applicati su tutte e quattro.
- **Qualità della grafica generata:** è sufficiente per un'alpha in stile Minecraft; resta sostituibile da un artista senza toccare il codice, perché i nomi delle ossa e la disposizione sono fissati qui.

## 10. Riporti dalla revisione finale della parte 3a

Cose che la revisione finale della parte 3a (13 novembre) ha trovato e lasciato a dopo, ciascuna con il motivo. È l'unico posto in cui sopravvivono: vanno copiate nel piano della parte o della milestone indicata quando viene scritto.

**Per la parte 3b** (stato alla fine della parte 3b: ogni voce dice se è fatta; quelle ancora aperte sono riportate in §11)

- **Fatto nella 3b** (grandezza reale, §4.8, e mixin del riquadro di culling). **Riquadro di culling e targhetta del nome con la grandezza reale.** Il gioco decide se disegnare il giocatore dal suo box (0,6 × 1,8 blocchi, allargato di 0,5), ma la tuta arriva a 3,09 blocchi di altezza e ±1,31 di larghezza: ai bordi dello schermo, da vicino, giocatore e tuta spariscono insieme quando solo testa, spalle o un braccio della tuta sono inquadrati, e le mod di occlusione possono nascondere una tuta la cui testa dovrebbe vedersi sopra un muro. La targhetta del nome resta all'altezza di un giocatore normale, dentro la tuta. Tutte e due si sistemano con la grandezza reale (§8, domanda 2; decisione 27 della revisione 3 del piano); anche allora le braccia restano 0,3-0,4 blocchi fuori dal box.
- **Fatto nella 3b** (`render` prende un `Entity`; `docs/compat.md`, "SuitRenderer"). **`SuitRenderer` per un'entità che non è un `LivingEntity`.** Oggi `render` prende un `LivingEntity`, ma l'entità telaio di 3b è un `Entity` semplice (§4.2 B); a GeckoLib basta `Entity`. Il contratto da tenere: l'entità non è un animabile di GeckoLib, perché solo per quelle GeckoLib chiama l'aggancio che applica la visibilità di pezzi e stati (scritto nel Javadoc dell'interfaccia).
- **Fatto nella 3b** (`SuitSummaryTagsTest.secondWriteReplacesTheFirst`). **Test di regressione: la scrittura del riassunto visivo sostituisce quella precedente.** Oggi `setSuitSummary` costruisce un `visual` nuovo, ma nessun test lo prova; quando 3b riscrive il riassunto a ogni cambio di pezzo, uno slot rimasto dalla scrittura prima continuerebbe a disegnare un pezzo tolto.
- **Ancora aperto** (§11). **Mantello, pappagalli sulle spalle e frecce conficcate seguono il pilota**, non la tuta (il mantello si disegna qualunque sia la visibilità del busto, e `PlayerModel.cloak` è privato: nasconderlo vuole un accessor o tutte le parti spente). Si decide dopo la prova sul PC, che li fotografa.
- **Ancora aperto** (§11). **Il pilota non si riallinea da accovacciato.** Accovacciati, il gioco abbassa testa, busto e braccia del modello di 4,2, 3,2 e 5,2 px e GeckoLib fa lo stesso sulle ossa della tuta, ma in pixel della tuta (cioè per S): il pilota resta 2,7 px (testa), 2,1 (busto) e 3,4 (braccia) più in alto del dovuto. L'oggetto impugnato è già corretto; il pilota no, e si vede solo col telaio nudo o con pezzi mancanti (la testa non esce dalla testa del telaio). Da correggere con un sollevamento che tiene conto dell'accovacciarsi, comune a `PilotPose.onScale` e a `SuitLayer`, se la prova sul PC lo trova evidente.
- **Fatto nella 3b** (`SuitBoneRules`, Java puro, provato da `SuitBoneRulesTest`). **Test delle decisioni del renderer.** Visibilità di ossa e stati e colori per regione (`SuitRendererImpl`) non hanno test; da fare quando 3b riusa il renderer per l'entità telaio.
- **Ancora aperto** (§11, decisione di Luigi). **Il colore secondario del pezzo delle gambe.** Dalla parte bassa delle gambe rifatta sul riferimento di Luigi il pezzo delle gambe è tutto nella regione primaria: il colore secondario non cambia nulla sulle gambe, mentre §2 e §5.1 danno a ogni pezzo due colori. Per 3a resta così; decisione di Luigi. Proposta del costruttore: una fascia corazzata alla caviglia e altre parti piccole nella regione secondaria, così l'aspetto predefinito non cambia.
- **Fatto nella 3b, tranne l'ultimo punto** (`tools/art/README.md`, `tools/art/selftest.py`; `Face.occlusion` è riportato in §11). **Primo compito d'arte di 3b, prima di cambiare `tools/art/designs/`:** nei controlli del modello (`exo.Model.check`) contare come copertura solo ciò che si disegna sempre insieme (il telaio e i pezzi degli slot delle due facce, mai le decalcomanie), perché con armatura parziale, normale in 3b, una faccia coperta solo dal pezzo di un altro slot sfarfalla; `_buried` che controlla le celle da un quarto di pixel e non solo i centri dei texel (oggi una striscia scoperta di 0,25 px su un lato dispari può sparire senza messaggio); test negativi committati dei controlli del generatore (oggi stampano solo "nessun problema", e un controllo spento passerebbe inosservato); la scala letta da `exo.py` in `tools/art/preview/shot.js`, dove oggi è scritta una terza volta senza test; l'ultimo texel di una linea sottile, incluso, che rende la stessa linea un texel fine più lunga sulla faccia davanti che su quella dietro (cambia la texture, per questo non è stato toccato); vettorializzare `Face.occlusion` (oggi il 61 % dei 26 s della build) se un secondo livello raddoppia il modello.

**Per M4**

- **Arti e testa che attraversano le piastre della tuta.** Le ossa della tuta prendono le rotazioni del giocatore una a una (gambe circa ±69 gradi camminando e ±80 correndo, braccia ±49 e ±57): a ogni passo le gambe entrano nelle parti lombari e, correndo, fino all'alloggiamento della cella; le braccia passano nelle falde laterali del torso; l'elmo gira dentro il collare (misure in `docs/arte/README.md`, da `tools/art/swing.py --part tutto`). Due strade, scelta di Luigi: un passo proprio della tuta (animazione), oppure un'oscillazione ridotta degli arti della tuta nel renderer.
- **`getInstanceId` da ridefinire in `SuitRendererImpl` prima di qualunque controller di animazione.** In GeckoLib 4.9 l'id predefinito è `(long) Math.pow(idEntità, 7) * -(slot + 1)`, che satura per gli id da 512 in su: su 1.21.1 tutte quelle entità condividerebbero un `AnimatableManager`. Oggi è innocuo perché non ci sono animazioni.
- **Il contatto della coscia in avanti** con la falda davanti del torso (da 1 grado) e con la pancia (da 29).
- **Le braccia del pilota allargate dentro le braccia della tuta**, invece di pendere lungo il corpo.
- **Il generatore oltre un livello:** i nomi delle ossa non portano il livello, un secondo livello grande come il Rottame non ci sta nelle regioni dei pezzi dell'atlante, e grandezza e regioni dell'atlante sono fisse anche nel test Java (`tools/art/README.md`, "Aggiungere un livello").

**Prima dell'alpha pubblica**

- **Codice di prova nei jar pubblicati:** le classi dei test in mondo (`gametest/ExoframeGameTests`, `GameTests.writeRawItemData`), le due strutture vuote `structure/empty.nbt` e `structures/empty.nbt` (ogni versione ne legge una: la cartella di risorse per versione può tenerle separate) e il comando `/exoframe debug` (solo operatori). Oggi sono inerti in produzione; si decide se e cosa pubblicare.
- **Visori di ricette** che elencano tutto il registro degli item: mostrerebbero i quattro segnaposto e `suit_model`.
- **Grandezza e posizione delle decalcomanie** (3,5 × 3,5 px, sulla piastra del braccio e non sullo spallaccio): da far vedere a Luigi prima di M5.
- **Politica dei mixin in caso di conflitto:** oggi sono obbligatori (`required`, `defaultRequire: 1`), quindi un'altra mod che cambia la stessa chiamata ferma il client invece di degradare; scelta del costruttore, da confermare con Luigi.

## 11. Riporti dalla revisione finale della parte 3b

Cose che le revisioni della parte 3b (quelle dei singoli task e le due revisioni finali, 13-14 novembre) hanno trovato e lasciato volutamente a dopo, ciascuna con dove va. Come §10, è l'unico posto in cui sopravvivono: vanno copiate nel piano della parte o della milestone indicata quando viene scritto.

**Per il piano della parte 3c (i primi task, prima di costruire comportamenti nuovi su questo codice)**

- **Un solo punto per "lo stato indossato è cambiato".** Oggi lo cambiano, ognuno per conto suo, l'ingresso, l'uscita, la morte (`frame/FrameEntry`), il comando di debug (`frame/DebugWear`) e la riparazione degli slot (`frame/SlotGuard`): la 3c deve applicare e togliere gli attributi (`FrameAttributes`) in ognuno di questi punti, quindi conviene prima un punto unico.
- **Dividere ciò che sta insieme per caso.** `FrameStateSavedData` tiene due cose, le operazioni del registro e il formato del file con le sue regole di tolleranza: vanno separate. La procedura che svuota ed elimina un telaio piazzato (`/exoframe remove`) e la ricerca del telaio sulla linea di vista stanno nella classe del comando e ripetono la logica di `frame/FrameInteractions`: vanno in `frame/`. Il predicato "il chunk ha le entità caricate" è scritto due volte (`FrameEntry` e `FrameRealign`).
- **Pezzi sconosciuti dentro un record leggibile.** Dentro un record che per il resto si legge, un pezzo con un livello, uno slot o un id di modulo sconosciuti, o una cella con capacità zero o negativa, si scartano al caricamento e il salvataggio successivo li cancella: il contrario della regola per le voci intere, che non si perdono mai. Finché non cambia vale la regola scritta nel commento di `FrameStateSavedData`: una versione che aggiunge un livello, uno slot o un id di modulo alza lo schema, così una versione più vecchia apre il registro in sola lettura invece di perdere quei pezzi.
- **Suoni.** In questa parte nessuno (gli slot armatura cambiano in silenzio, scelta della 3b). Un suono del gioco base ciascuno per piazzare, montare e smontare, entrare e uscire (domanda 3 della checklist della 3b).
- **Moduli senza controlli al montaggio.** Montando un pezzo a mano i moduli scritti sullo stack si ripristinano senza i controlli dell'installatore (`restoreModules`); oggi non esistono oggetti modulo, quindi nessuno li scrive.
- **Il telaio raccolto come oggetto perde `reinforced` e `unlocked`.** L'oggetto restituito è un telaio nuovo, senza dati; nulla imposta ancora `reinforced`.
- **Linea di vista dei comandi e mira del clic diverse.** I comandi (`unlock`, `lock`, `remove`) cercano il telaio fermandosi alle forme di collisione dei blocchi, il clic destro del gioco ai loro contorni: erba alta, fiori, una porta aperta o una ragnatela nascondono un telaio al clic e non a `/exoframe lock`. Con gli occhi dentro il riquadro di un telaio, il comando può scegliere un telaio più lontano sulla stessa linea.
- **Due telai nello stesso punto.** Uscire o morire dove c'è già un telaio (per esempio due telai mandati al punto di rinascita del mondo) li mette uno dentro l'altro; l'oggetto telaio invece rifiuta quel caso.
- **Un segnaposto può sfuggire alla guardia fra due tick** (più clic nell'inventario in un solo pacchetto, poi il menu chiuso): resta, inerte, in una cassa, una cornice o un supporto per armature finché non torna nell'inventario di un giocatore. Toglierli anche quando un menu si chiude lo eviterebbe.
- **Un pezzo di un livello senza oggetto non si smonta a mano** (oggi esistono solo gli oggetti Rottame): il problema sparisce quando ogni livello ha i suoi oggetti (M5).

**Per il passaggio sulla grafica (M4)**

- **L'ombra** sotto la tuta indossata e sotto il telaio piazzato è quella del giocatore vanilla, piccola e in gran parte coperta dai piedi della tuta (domanda 2 della checklist della 3b).
- **Mantello, pappagalli e frecce conficcate** seguono il pilota e non la tuta (da §10, ancora aperto): si decide con le foto della prova sul PC.
- **Il pilota da accovacciato** (da §10, ancora aperto): testa, busto e braccia restano qualche pixel più in alto del dovuto; da correggere se la prova sul PC lo trova evidente.
- **Vettorializzare `Face.occlusion`** nel generatore (da §10), se un secondo livello raddoppia il modello.

**Prima dell'alpha pubblica**

- **I test in gioco, i loro agganci solo per i test e il comando di debug** stanno nei jar pubblicati (come in §10).
- **Nessun comando elenca o trova i telai**; l'armatura rimasta nella lista `orphaned` e le voci stivate che non si leggono si recuperano solo a mano dal file del registro.

**Da provare su un PC vero o con giocatori veri (il cloud non può)**

- L'ingresso con la latenza di una rete vera (il client ha ancora per un attimo l'ingombro piccolo): serve una seconda macchina.
- Il costo della guardia degli slot con cento giocatori; molti telai piazzati in vista.
- Altre mod: tombe e gestori della morte che leggono l'inventario prima dell'ascoltatore di questa mod; mod che annullano una morte; mod di protezione delle zone che bloccano il clic sulle entità; mod che mostrano l'inventario altrui; altre mod che usano il tasto V.

**Decisioni di Luigi** (ognuna con ciò che fa la mod oggi; le stesse sono fra le domande della checklist della 3b, `docs/test-in-gioco/m3b-entrare-e-uscire.md`, sezione 7)

- **L'armatura custodita non ha un proprietario suo** (domanda 1): in un telaio sbloccato il proprietario del telaio, entrando e uscendo, prende l'armatura lasciata da chi lo aveva in prestito, e chi lo ha in prestito non può entrare finché dentro c'è l'armatura del proprietario.
- **Uscire a mezz'aria sopra il vuoto** (domanda 5): il telaio va nell'ultima posizione sicura, ma l'armatura torna al giocatore che sta cadendo.
- **La creativa con l'inventario pieno** (domanda 6; §7, punto 26): ciò che la mod restituisce e non entra cade a terra, e un giocatore in creativa con l'inventario del tutto pieno che ci passa sopra lo cancella (regola del gioco base per la creativa).
- **Telai che non si distruggono** (domanda 7): un telaio piazzato non si rompe né a colpi né con le esplosioni, e finché è bloccato lo tocca solo il suo proprietario (e un operatore in creativa); si può lasciare ovunque uscendo o morendo, anche nella base di un altro; lo tolgono solo il proprietario (svuotandolo e raccogliendolo) o un operatore, uno alla volta, guardandolo.
- **PvP** (domanda 8): chi muore dentro la tuta lascia il suo telaio bloccato con dentro l'armatura: chi lo ha ucciso non può prendere pezzi, cella o armatura, né rompere il telaio.
- **`keepInventory`** (domanda 9): anche con la regola attiva l'armatura resta nel telaio alla morte.
- **Acqua e lava** (domanda 10): i fluidi non sono suolo, quindi un telaio lasciato sopra l'acqua o la lava si posa sul fondo.
- **Il colore secondario del pezzo delle gambe** (da §10): oggi le gambe hanno solo il colore primario.
