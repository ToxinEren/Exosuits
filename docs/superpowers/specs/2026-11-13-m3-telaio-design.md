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

**Anti-obiettivi**: nessuna forma, nome o texture riconoscibile di Fallout, Iron Man o Halo; nessun asset di Mjolnir; nessun dato personale nei jar; nessuna differenza di versione o di loader fuori da `compat/` e `platform/`; nessuna perdita di pezzi, celle o armatura civile per morte, uscita, crash o riavvio del server.

## 3. Vincoli

- Quattro varianti da un solo sorgente (Stonecutter + Architectury Loom); condizioni solo in `compat/impl`, `platform/` e `compat/Ids.java` (`tools/check_sources.sh`).
- `core/` resta Java 17 puro e testato con JUnit.
- Lo stato del telaio per giocatore vive nel `SavedData` (`FrameStateAccess`); le scritture a ogni tick passano da `mutate` e la sincronizzazione al proprietario resta sotto le 5 volte al secondo.
- I mixin si usano solo dove nessun evento copre il bisogno (scelta del costruttore, vedi §4.3 e §4.7). Oggi ce n'è uno, lato client, sul disegno del giocatore; tutto il resto passa da eventi Architectury o da hook per loader in `platform/`.
- La grafica è generata da script: i file generati sono committati, lo script li rigenera identici e la CI lo verifica.
- Licenza "tutti i diritti riservati"; il controllo di privacy sui jar resta verde (i PNG generati non hanno metadati).
- Nel cloud si può provare solo il server dedicato; client e resa grafica si provano sul PC di Luigi.

## 4. Approcci considerati

### 4.1 Come arriva agli altri giocatori l'aspetto del telaio indossato

- **A. Pacchetto visivo dedicato più eventi di tracking per loader.** Preciso, ma Architectury non ha un evento di inizio tracking: servono tre hook per loader, una cache client con sfratto delle voci e la gestione di ingresso, rinascita e cambio di dimensione.
- **B. Dati visivi nello stack segnaposto del torso (scelto).** I quattro segnaposto stanno già negli slot armatura; vanilla sincronizza da sola gli stack degli slot a tutti i client che vedono il giocatore, su ogni loader e versione. Il segnaposto del torso porta, nei suoi dati (`ItemDataAccess`), un riassunto solo visivo: pezzi presenti con livello, stato visivo e aspetto. Il layer di rendering legge quel riassunto per qualunque giocatore. Nessun hook, nessuna cache da svuotare, corretto da solo a ingresso, rinascita e cambio di dimensione.
- **C. Sincronizzare a tutti lo stato completo.** Scartato: manda a tutti energia, proprietario e moduli.

Scelto B: meno codice e meno punti in cui sbagliare. Costo: il riassunto visivo si aggiorna quando cambia (pezzo montato, rotto, riverniciato), cioè di rado. Lo stato completo continua ad arrivare solo al proprietario con il pacchetto esistente.

### 4.2 L'entità telaio

- **A. `LivingEntity` stile armor stand con `GeoEntity`.** Porta salute, attributi, slot e import di GeckoLib diversi per versione.
- **B. `Entity` semplice, disegnata con lo stesso renderer del telaio indossato (scelto).** L'entità tiene il suo `FrameState` e lo sincronizza ai client con un dato sincronizzato di tipo `CompoundTag` (scritto dallo stesso codec dello stato). Il suo renderer posa un `HumanoidModel` invisibile in posizione di riposo e chiama il renderer del telaio: un solo renderer GeckoLib per giocatore ed entità, e l'entità non è un `GeoAnimatable`.
- **C. Blocco con block entity.** Scartato: il documento di gioco vuole un'entità, e un blocco non si piazza dove il giocatore esce o muore.

### 4.3 Slot armatura bloccati

- **A. Maledizioni vanilla (Binding e Vanishing) sui segnaposto.** Niente codice, ma si vedono nel tooltip, non valgono in creativa e l'API degli incantesimi cambia tra le versioni.
- **B. Mixin sugli slot e sul rilascio alla morte.** Il più pulito per chi gioca, ma introduce i mixin (refmap, un punto in più che cambia tra versioni).
- **C. Autoriparazione senza mixin (scelto).** A ogni tick del server, per chi è nel telaio: i quattro segnaposto devono stare nei loro slot; se ne manca uno si rimette, se una copia è finita nell'inventario o sul cursore si elimina. `PlayerEvent.DROP_ITEM` annulla il lancio. Alla morte i segnaposto si tolgono in `LIVING_DEATH`, prima che vanilla faccia cadere l'inventario. Vale anche in creativa. Difetto: cliccando lo slot l'oggetto lampeggia per un tick; il mixin di rifinitura può arrivare in M4 senza cambiare il resto.

### 4.4 Grafica

- **A. Modelli fatti a mano in Blockbench.** Non disponibile: la grafica la fa la sessione cloud.
- **B. Generatore in Python nel repo (scelto).** Un piccolo linguaggio di descrizione (ossa, scatole, regioni di colore) produce geometria GeckoLib, atlante delle texture e anteprime renderizzate. I file generati sono committati. Luigi sceglie la direzione tra tre anteprime prima che il generatore definitivo entri nel repo.

### 4.5 Dove vive il contenuto di un telaio piazzato

- **A. Nell'NBT dell'entità.** Semplice, ma entità (dati dei chunk) e stato del giocatore (dato salvato del livello) si salvano in momenti diversi: un crash tra i due salvataggi può duplicare un telaio (entità ancora su disco e stato già sul giocatore) o perderlo (stato già tolto e entità non ancora scritta). Un'entità può anche sparire nel vuoto o per un comando.
- **B. In un registro unico nel dato salvato; l'entità è solo una vista (scelto).** Ogni telaio ha un'identità (`frameId`). Il dato salvato tiene tutti i telai: quelli indossati (per giocatore) e quelli piazzati (per `frameId`, con dimensione, posizione e orientamento). Entrare e uscire spostano un record tra le due mappe dentro lo stesso file, quindi non esiste uno stato a metà. L'entità ha come UUID il `frameId`, non possiede il contenuto e si riallinea al registro: se il registro non ha un telaio piazzato con quell'identità l'entità si elimina (niente doppioni); se il registro ha un telaio piazzato in un chunk caricato e l'entità manca da qualche secondo, viene ricreata (niente perdite per crash, vuoto o `/kill`).

Scelto B: è l'unico che rispetta l'anti-obiettivo "nessuna perdita" senza finestre di rischio. Costo: la logica di riallineamento, coperta da test in mondo.

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
- **D. Tuta in scala, pilota sollevato (scelto).** Come C, ma il pilota è disegnato più in alto di 12·(S−1) pixel del modello. La linea delle anche e quella delle mani coincidono con quelle della tuta, le gambe ruotano attorno allo stesso perno e i piedi poggiano su pedane dentro gli stinchi. È la disposizione di un'armatura potenziata: si sta agganciati dentro la macchina.

**Costo di D.** Per sollevare il pilota e per non disegnare le parti del corpo coperte dai pezzi bisogna intervenire prima che il gioco disegni il giocatore. Forge e NeoForge hanno un evento, Fabric no. Si usa un solo mixin comune, lato client, su `PlayerRenderer.render`, che ha la stessa firma su 1.20.1 e 1.21.1. È la prima eccezione al "niente mixin" di §4.3.

**Scala.** S = 1,25 è la proposta del costruttore; 1,4 è l'alternativa mostrata nelle anteprime. Sceglie Luigi.

**Grandezza reale o apparente.** Domanda aperta per Luigi (§8). Riguarda ingombro e visuale, quindi la parte 3b; la 3a disegna soltanto.

## 5. Il progetto

### 5.1 Grafica e generatore (3a)

- **Direzione artistica:** scelta da Luigi fra tre anteprime (spike in corso). Qualunque sia, valgono le convenzioni sotto.
- **Convenzioni della geometria** (dalla ricognizione, obbligatorie per `GeoArmorRenderer`): ossa di primo livello `armorHead`, `armorBody`, `armorRightArm`, `armorLeftArm`, `armorRightLeg`, `armorLeftLeg`, con i perni del modello armatura di GeckoLib (testa e corpo 0,24,0; braccia ∓5,22,0; gambe ∓2,12,0). Sotto ciascuna: `frame<Parte>` (telaio nudo, sempre visibile) e il pezzo, diviso per stato visivo e per regione di colore.
- **Proporzioni (revisione del 13 novembre, §4.7):** la geometria resta nelle coordinate standard del modello armatura, con i perni invariati, e viene resa in scala S attorno ai piedi. In quelle coordinate il pilota è una figura ridotta di 1/S con la linea delle anche a y = 12: con S = 1,25 i piedi stanno a y 2,4 e la testa tra 21,6 e 28. Pedane, gabbia della testa e guardie delle mani si disegnano attorno a quella figura; i montanti degli arti corrono sul lato esterno.
- **Densità e resa (revisione del 13 novembre):** due texel per pixel del modello, quindi un atlante più grande; pittura da metallo: spazzolatura, bordi chiari consumati, incavi scuri, bulloni con punto luce.
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
- **Scala e pilota (revisione del 13 novembre, §4.7):** `client/SuitScale` tiene S e il sollevamento del pilota. `client/SuitLayer` disegna la tuta in scala S attorno ai piedi. Un mixin client su `PlayerRenderer.render`, comune alle quattro varianti, fa due cose per chi indossa il telaio: sposta in alto il disegno del giocatore di 12·(S−1) pixel del modello e, subito dopo `setModelProperties`, toglie dal disegno le parti del corpo coperte da un pezzo montato (testa con l'elmo, busto con il torso, ogni braccio e ogni gamba con il suo pezzo, secondo strato della skin compreso). Con il telaio nudo il pilota si vede intero dentro lo scheletro, ed è voluto. La prima persona non cambia: il braccio resta quello vanilla fino a M4.
- **Segnaposto invisibili:** su 1.21.1 il materiale `frame` passa a `layers = List.of()`; su 1.20.1 si aggiungono due texture trasparenti `frame_layer_1.png` e `frame_layer_2.png` nel namespace `minecraft` (il nome del materiale resta senza namespace).
- **Comando di debug (3a):** `/exoframe debug wear [rottame|nudo|via]` mette o toglie al giocatore uno stato di prova e i segnaposto, per vedere il risultato sul PC prima che esista l'entità.

### 5.3 Stato e dati (3b)

- **Aspetto:** in `core`, `Appearance(int primary, int secondary, List<Decal> decals)` con `Appearance.DEFAULT`, e `Decal(String pattern, String color)` (id del registro e nome della tinta come stringhe: forma che sopravvive al passaggio 1.20.1 → 1.21.1). `ItemDataAccess.PieceData` sostituisce i due interi dei colori con `Appearance`; `FrameState` tiene un `Appearance` per slot. Nuove chiavi NBT: `decals` (lista di `{pattern, color}`); i colori restano `color_primary` e `color_secondary`.
- **Ripristino fidato:** `FrameStateCodec` ottiene una lettura che non rivalida i moduli contro la config di chi legge, usata dal client e dai dati sincronizzati dell'entità; il caricamento sul server continua a validare. Chiude il limite (a) di `docs/compat.md`.
- **Registro dei telai (vedi §4.5):** il dato salvato esistente si estende. Ogni record ha `frameId`, `FrameState` e armatura stivata. I record indossati restano indicizzati per giocatore (come oggi); i record piazzati sono indicizzati per `frameId` e portano dimensione, posizione e orientamento. `FrameStateAccess` ottiene le operazioni di registro: `place(record, posizione)`, `enter(ServerPlayer, frameId)` (sposta il record da piazzato a indossato e sincronizza una volta), `exit(ServerPlayer, posizione)` (l'inverso), `placed(frameId)`, `remove(frameId)`, più `writeTag`/`readTag` per il riassunto che l'entità manda ai client. Lo schema del dato salvato passa a 2; lo schema 1 non è mai uscito dal repo, quindi non serve migrazione.
- **Armatura stivata:** lista di stack dentro il record, mai inviata ai client. Gli stack si salvano con il provider su 1.21.1 e portano accanto la versione dei dati di Minecraft (`data_version`): al caricamento su 1.21.1 di stack salvati su 1.20.1 si passa dal correttore di dati vanilla prima di leggerli, così l'armatura sopravvive all'aggiornamento del mondo.
- **Riassunto visivo:** `SuitSummary` (pezzi presenti, livello, stato visivo, aspetto per slot), scritto nei dati del segnaposto del torso sotto una nuova chiave `visual` e letto dal layer. Si riscrive solo quando cambia.
- **Pacchetto al proprietario:** oltre allo stato porta tre indicatori calcolati dal server (`canSprint`, `canSwim`, stato dell'energia), così il client non valuta le regole con la propria config.

### 5.4 Entità telaio (3b)

- `entity/ExoframeEntity extends CompatEntity` (`compat/CompatEntity`: dati sincronizzati con o senza `Builder`, altezza degli occhi, pacchetto di spawn vanilla). Immobile, senza gravità dopo l'appoggio, persistente, con riquadro da giocatore (0,6 × 1,95).
- L'entità è una vista del record piazzato con il suo stesso UUID (`frameId`): nel suo NBT salva solo l'identità; il contenuto sta nel registro. Ai client manda il riassunto visivo come `CompoundTag` sincronizzato, aggiornato quando il record cambia.
- Riallineamento: al primo tick dopo il caricamento, se il registro non ha un record piazzato con quell'identità l'entità si elimina; il server controlla ogni pochi secondi i record piazzati nei chunk caricati e ricrea l'entità mancante. Per togliere davvero un telaio si passa dal registro (raccolta come item, `/exoframe remove`).
- Item `exoframe`: crea un record nuovo (identità nuova, il giocatore come proprietario) e l'entità sul blocco cliccato, rivolta verso il giocatore.
- Permessi: ogni interazione con un telaio piazzato (entrare, montare, smontare, togliere la cella, ritirare l'armatura stivata, raccoglierlo) richiede di essere il proprietario, oppure che il telaio sia sbloccato o senza proprietario, oppure che la config `frame.ownerOnly` sia falsa; un operatore in creativa può sempre.
- Interazioni (senza GUI, scelta del costruttore):
  - con un pezzo in mano: monta il pezzo nel suo slot se libero; braccia e gambe usano un solo item per lato e vanno sul lato cliccato, o sul primo libero;
  - con una cella in mano: inserisce la cella se manca;
  - mano vuota: entra, se il giocatore è il proprietario o il telaio è sbloccato (o la config lo consente) e c'è una cella con carica;
  - accovacciato a mano vuota: se il telaio contiene armatura stivata la restituisce tutta (inventario, o a terra se pieno); altrimenti smonta ciò che si trova all'altezza cliccata (testa, busto e braccia, gambe); se lì non c'è nulla toglie la cella; se il telaio è del tutto vuoto lo raccoglie come item e cancella il record.
- Danni: l'entità non si distrugge a colpi né con le esplosioni. Se sparisce (vuoto, comando), il registro la ricrea all'ultima posizione registrata.

### 5.5 Ingresso, uscita, morte (3b)

- **Ingresso:** se il record contiene già armatura stivata (telaio lasciato da una morte), quella va prima a chi entra se è il proprietario, altrimenti cade a terra accanto al telaio. Poi il server stiva l'armatura indossata nel record, fa `enter` (il record passa da piazzato a indossato), mette i quattro segnaposto, elimina l'entità e scrive il riassunto visivo. Un solo tick.
- **Uscita:** tasto `V` (`KeyMappingRegistry`) tenuto 1 s sul client, poi pacchetto C2S `frame_exit`; il server verifica che il giocatore sia nel telaio, toglie i segnaposto, restituisce l'armatura civile agli slot (se uno slot è occupato l'oggetto va nell'inventario o cade), fa `exit` con la posizione del giocatore e crea l'entità.
- **Morte:** in `LIVING_DEATH` il server toglie i segnaposto e fa `exit` lasciando l'armatura stivata nel record; l'entità nasce nel punto di morte. Se il punto di morte è sotto il limite inferiore del mondo o nel vuoto, si usa l'ultima posizione sicura (l'ultima in cui il giocatore nel telaio era a terra, registrata ogni secondo). Vale anche con `keepInventory`.
- **Uscita dal gioco e riavvio:** lo stato resta nel dato salvato e i segnaposto nell'inventario; al rientro l'autoriparazione riallinea slot e stato (stato presente senza segnaposto: li rimette; segnaposto senza stato: li toglie).
- **Proprietario:** `owner` e `unlocked` come da `FrameState`; opzione di config `frame.ownerOnly` (default vero). L'interruttore di sblocco dal gioco arriva con la stazione; in 3b c'è `/exoframe unlock`.

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
- **GameTest (se la prova in corso lo conferma):** registrazione per loader in `platform/`, classe di test comune, un passo nella CI per variante. Casi: ingresso e uscita con armatura civile restituita; morte che lascia l'entità con tutto dentro e rientro che restituisce l'armatura; entità doppia che si elimina; entità eliminata che viene ricreata; morte nel vuoto che lascia il telaio all'ultima posizione sicura; permessi del non proprietario; stack stivato in formato 1.20.1 letto su 1.21.1; autoriparazione dei segnaposto; consumo di energia su N tick; usura dei pezzi da un danno noto; caduta senza danno; caricatore che carica.
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
10. L'armatura civile rimasta in un telaio dopo una morte torna al proprietario quando rientra o quando la ritira accovacciandosi a mano vuota; se entra un altro giocatore, cade a terra accanto al telaio.
11. Chi muore nel vuoto ritrova il telaio nell'ultimo punto sicuro in cui era a terra.
12. Le parti 3b e 3c non aspettano le prove sul PC: si procede nel cloud e ciò che il PC trova si corregge all'inizio della parte successiva.
13. La tuta è più grande del pilota, che vi sta agganciato dentro, sollevato su pedane (§4.7). Scala proposta 1,25.
14. Un mixin lato client sul disegno del giocatore: prima eccezione al "niente mixin".
15. Le parti del corpo coperte da un pezzo montato non vengono disegnate.

## 8. Domande aperte per Luigi

1. **La direzione artistica dei pezzi Rottame.** Tre anteprime mostrate il 13 novembre sul primo telaio; con il telaio ridisegnato (§4.7) i pezzi si ripropongono sulla nuova forma. Se non risponde, si procede con quella che il costruttore raccomanda, e resta sostituibile.
2. **Grandezza reale o apparente** (chiesto il 13 novembre). Reale: chi è nella tuta occupa più spazio e vede da più in alto; con S = 1,25 l'ingombro è 0,75 × 2,25 blocchi in piedi e 1,875 di altezza accovacciati, quindi da un'apertura alta due blocchi si passa solo accovacciati. Apparente: ingombro e visuale restano quelli del giocatore, ma testa e spalle della tuta attraversano soffitti bassi e stipiti. Proposta del costruttore, e scelta predefinita se non risponde: reale, con 1,25. Senza dipendenze esterne: su 1.21.1 l'attributo `scale` del gioco, su 1.20.1 l'evento di Forge e due mixin su Fabric (ingombro e altezza degli occhi). Con 1,4 la tuta non passerebbe da una porta nemmeno accovacciata.

## 9. Rischi

- **Resa in gioco non verificabile nel cloud:** perni del modello, compenetrazioni con la skin, costo del rendering. Mitigazione: 3a esiste apposta per portare presto qualcosa di visibile sul PC.
- **GameTest su quattro varianti:** se una variante non regge, lì restano test JUnit, server dedicato e checklist.
- **Riallineamento tra registro ed entità:** è la parte più delicata di 3b; un errore lì può creare proprio i doppioni o le perdite che vuole evitare. Mitigazione: test in mondo dedicati prima di costruirci sopra ingresso e uscita.
- **Movimento deciso dal client** (scatto, nuoto): serve codice sui due lati e prova sul PC.
- **Mixin client su quattro varianti (§4.7):** nel cloud non si può avviare un client, e un bersaglio sbagliato fa crollare il gioco all'avvio. Mitigazione: firma del metodo verificata sui jar delle due versioni, controllo in `tools/check_jars.sh` che configurazione e refmap del mixin siano nei jar, avvio del client come primo punto della checklist di 3a sul PC.
- **Qualità della grafica generata:** è sufficiente per un'alpha in stile Minecraft; resta sostituibile da un artista senza toccare il codice, perché i nomi delle ossa e la disposizione sono fissati qui.
