# Milestone 4 — Stazione e HUD: requisiti (ricognizione)

Data: 18 novembre 2026. Base: branch `m4-stazione`, uguale a `main` a `b82a8da`. Ricognizione in sola lettura: nessun file del repository è stato cambiato, nessuna build o prova eseguita.

Scopo: tutto ciò che M4 deve fare, con fonte e numeri esatti; ciò che M3 ha rimandato a M4 o lasciato come domanda al proprietario; le decisioni già prese che M4 deve rispettare; i conflitti e le lacune fra i documenti; ciò che è di M5 (o dopo) e che M4 deve preparare o non bloccare. Non propone soluzioni: dove i documenti tacciono lo dice (LACUNA) e indica chi decide.

## 0. Fonti, sigle, precedenze

### 0.1 Sigle delle fonti

| Sigla | File | Note |
|---|---|---|
| GIOCO | esportazione del documento di gioco del 4 novembre in `docs/superpowers/specs/` (il file con `2026-11-04` nel nome) | autorità per le meccaniche |
| GEN | `docs/superpowers/specs/2026-11-12-redstone-exoframes-design.md` | design generale (12 novembre) |
| M3 | `docs/superpowers/specs/2026-11-13-m3-telaio-design.md` | design di M3; citato con § e numero di riga (r.) |
| COMPAT | `docs/compat.md` | cuciture, schema NBT, limiti noti, regole dei sorgenti |
| CL-3a, CL-3b, CL-3c, CL-3d | `docs/test-in-gioco/m3a-vedere-il-telaio.md`, `m3b-entrare-e-uscire.md`, `m3c-giocarci.md`, `m3d-braccio.md` | checklist per il PC, con le domande al proprietario |
| ARTE | `docs/arte/README.md` | |
| AUDIT | `docs/audit/mjolnir-armor-audit.md` | indicazioni di riuso, non requisiti |
| NOTE-M2b | `docs/superpowers/plans/2026-11-13-m3-note-dalla-revisione-m2b.md` | rilievi del 13 novembre, in parte superati |
| PIANO-3a | `docs/superpowers/plans/2026-11-13-m3a-vedere-il-telaio.md` | |
| CODICE | sorgenti a `b82a8da`, percorsi relativi alla radice del package Java (es. `core/armor/Piece`) | solo controlli puntuali |

I numeri di riga sono quelli dei file a `b82a8da`.

Tag di ogni voce: **[doc]** scritto in un documento; **[codice]** verificato nei sorgenti; **[dedotto]** conseguenza che nessun documento scrive.

Persone: "il proprietario" è chi decide (nei documenti compare con il nome proprio); "chi prova sul PC" esegue le checklist.

### 0.2 Precedenze fra i documenti

- P1 [doc] GEN r.4: in caso di conflitto vale GIOCO, salvo le decisioni elencate in GEN §7 (r.135-150).
- P2 [doc] La decisione 13 di GEN (r.149, "il telaio non cambia le dimensioni del giocatore") è superata dal 13 novembre: la sostituisce M3 §4.7 (r.86); resta valido "nessuna dipendenza da Pehkui".
- P3 [doc] M3 r.396: dove una frase di M3 §1-§11 dice altro vale §12 (parte 3c).
- P4 [doc] M3 r.725: dove una frase di M3 §1-§12 dice altro vale §13 (braccio in prima persona).
- P5 [doc] Le voci segnate "scelta del costruttore" in M3 sono correggibili (M3 r.3, r.396); le decisioni del proprietario restano finché lui non ne prende una nuova.
- P6 [dedotto] Checklist e piani non sono fonti di requisiti: registrano domande e "come è andata". Dove contraddicono un design vale il design, salvo una risposta del proprietario; nel repository nessuna risposta è registrata (§8).

## 1. Perimetro di M4

- M4-01 [doc] GEN §8 r.159: milestone 4 "Stazione e HUD", fatta quando c'è "Blocco stazione con GUI, riparazione, verniciatura, HUD, tasti"; dove: cloud + test sul PC.
- M4-02 [doc] GIOCO "Roadmap" r.184: "Stazione e HUD: blocco stazione con GUI, riparazione, verniciatura, HUD." (i tasti li aggiunge GEN).
- M4-03 [doc] GEN §7.10 r.146: vernici primaria e secondaria e preset in M4; decalcomanie da stendardo entro M5, prima della pubblicazione.
- M4-04 [doc] Regola trasversale (GEN r.164; GIOCO r.173, regola 4): M4 si compila e si avvia sui quattro target (1.20.1-forge, 1.20.1-fabric, 1.21.1-fabric, 1.21.1-neoforge) prima di M5. Anti-obiettivo GEN r.33: "build verdi ma gioco rotto" è un fallimento.
- M4-05 [doc] Consegna con istruzioni di test in gioco precise e una checklist in `docs/test-in-gioco/` (GEN §3 r.41, §7.8 r.144); dalla 3d la checklist è "scritta per essere seguita alla lettera da un assistente che non può fare domande" (M3 §13 r.752).
- M4-06 [doc] Non sono M4: tier 1-3, cinque moduli, decalcomanie, pubblicazione (M5, GEN r.160); mondo e bottino, compresi kit e celle nei forzieri (M6, GEN r.161; GIOCO r.138); tier 4-5, moduli restanti, Laboratorio sepolto, bilanciamento, config completa (M7, GEN r.162); FE dopo la 1.0 (GEN §7.4 r.140). Dettaglio e preparazione in §7.

## 2. Requisiti di M4 dai documenti

### 2.1 Il blocco stazione

- ST-01 [doc] Stazione di manutenzione: "blocco 2x1 con GUI dedicata" (GIOCO r.103; GEN §6 r.131). È "il blocco centrale del mod: ci si piazza davanti il telaio per montare pezzi, riparare, installare moduli e verniciare" (GIOCO r.101).
- ST-02 [doc] "Il telaio va posizionato sul blocco o entro 2 blocchi" (GIOCO r.103).
- ST-03 [doc] I pezzi si montano sul telaio "con tasto destro o dalla stazione di manutenzione" (GIOCO r.19). Il montaggio a mano esiste dalla 3b (M3 §5.4 r.184-188); il montaggio dalla stazione non compare nel "fatto quando" di GEN r.159 (C-05, L-03).
- ST-04 [doc] Lo sblocco per gli altri giocatori dal gioco "arriva con la stazione"; fino ad allora `/exoframe unlock` e `/exoframe lock`, del proprietario (M3 §5.5 r.197; §7 punto 4 r.238; punto 22 r.256).
- ST-05 [doc] La stazione imposta `reinforced` e `unlocked` del telaio (M3 §11 r.351; §12.14 r.664); dettagli in RP-07.
- ST-06 [doc] Crafting alla stazione dei pezzi dei tier 2-5 e dei moduli, "non nel banco da lavoro" (GIOCO r.105; GEN r.131): non è M4 (§7), ma arriverà su questo blocco.
- ST-07 [codice] Oggi non c'è nessun blocco stazione: `content/ModBlocks` registra solo `charger`; nei sorgenti non c'è nessun `MenuRegistry`, `MenuType` né `ContainerScreen` (solo riferimenti ad `AbstractContainerMenu` nella guardia dei segnaposto, `frame/Placeholders`, e nei test). La stazione sarà il primo menu e la prima schermata del mod.
- ST-08 [doc] Cuciture già pronte per un blocco con block entity: `compat/CompatEntityBlock` (clic neutro `onClick(state, level, pos, player, hand, hit)`, chiamato una volta per mano sui due lati; da accovacciati arriva solo a mani vuote) e `compat/CompatBlockEntity` (`readData`/`writeData`, lettura che non lancia mai; il provider dei registri di 1.21.1 non arriva alle sottoclassi, perché "i dati delle block entity della mod sono numeri e booleani, nessuno stack"; `getUpdateTag` non toccato) (COMPAT r.117-142). Non si estende `BaseEntityBlock` (COMPAT r.120; M3 §12.11 r.571). Ordine di registrazione `ModBlocks` → `ModItems` → `ModBlockEntities` (M3 §12.11 r.571; COMPAT r.134). Il nome di una classe di blocco finisce in `Block`, altrimenti un errore nei log di sviluppo che `tools/check_log.py` conta (M3 §12.11 r.571).
- ST-09 [doc] Cuciture che mancano e arrivano "con la stazione": extra data dei menu (con `MenuRegistry` di Architectury restano `FriendlyByteBuf` sulle due versioni: COMPAT r.260; AUDIT r.321, r.377); basi client rinviate a "M3/M4": `CompatContainerScreen` (`Screen.renderBackground` con firma diversa), anteprima di un'entità in una schermata (`InventoryScreen.renderEntityInInventory` con firma diversa), `Model.renderToBuffer` con colore intero, `Minecraft.getFrameTime()` → `getTimer()`, `Tesselator`/`BufferUploader`, `setupRotations` con `scale`, basi GeckoLib, `ClientTiming` (COMPAT r.276; AUDIT r.386-392). GEN §5.2 r.90 mette il "menu type" in `Platform`, che oggi non lo ha (COMPAT r.19).

### 2.2 La GUI

- GUI-01 [doc] "a sinistra il telaio con i sei pezzi cliccabili; al centro gli slot del pezzo selezionato (materiali di riparazione, moduli); a destra anteprima 3D ruotabile" (GIOCO r.104).
- GUI-02 [doc] L'anteprima disegna una tuta in scala S = 1,65: armata alta circa 3,09 blocchi, telaio nudo 3,02, larga circa 2,6 con i pezzi (M3 §4.7 r.101; ingombri del disegno in §5.1 r.146). S non sta nella config: server e client devono avere lo stesso numero (M3 §4.8 r.136).
- GUI-03 [doc] La tuta si disegna con `SuitRenderer.render(Entity, …)`, che accetta un'entità non vivente e non animabile di GeckoLib; visibilità e colori li decide `SuitBoneRules`; ogni disegno decide da capo tutte le ossa e non lascia nulla al disegno successivo dello stesso fotogramma (terza persona, anteprima dell'inventario, telaio piazzato) (COMPAT r.189, r.197; M3 §13 r.731). La vista si ricava con `SuitDraw.view(riassunto)` e i colori non verniciati di `client/TierPalette` (COMPAT r.193, r.210).
- GUI-04 [doc] Slot dei moduli nella GUI (GIOCO r.104): gli oggetti modulo arrivano in M5 (C-05).
- GUI-05 [doc] Testi in inglese e italiano per ogni oggetto, blocco, tasto e messaggio (M3 §5.8 r.220; §12.12 r.610).

### 2.3 La riparazione

- RIP-01 [doc] "alla stazione di manutenzione con il materiale del tier (ferro, diamante, netherite…)" (GIOCO r.41); GEN §6 r.127: "riparazione alla stazione col materiale del tier o kit da campo (+25%)".
- RIP-02 [doc] "Riparazione rapida sul campo con un kit di riparazione consumabile (ripristina il 25% di un pezzo)" (GIOCO r.41).
- RIP-03 [codice] Già nel core: `damage.repairKitFraction = 0.25` (`core/config/ExoframesConfig`, letto con il controllo `unit` di `ConfigValidation`); `Piece.repairFraction(double)` ripara quella frazione della durabilità massima, arrotondata all'intero più vicino e limitata al massimo (eccezione fuori da [0, 1]); `Piece.repairFull()`.
- RIP-04 [doc] Materiali dei tier (GIOCO r.70-74): 1 Rottame ferro + rame; 2 Industriale ferro + redstone; 3 Corazzata diamante; 4 Netherite netherite; 5 Prototipo materiali dell'End. I pezzi si montano anche misti sullo stesso telaio (GIOCO r.66), quindi il materiale dipende dal pezzo [dedotto].
- RIP-05 [codice] Durabilità massima per tier (`tiers.maxDurability`): SCRAP 180, INDUSTRIAL 360, ARMORED 520, NETHERITE 650, PROTOTYPE 800. Con 0,25 il kit rende 45 punti a un pezzo Rottame [dedotto].
- RIP-06 [doc] Fino a M4 nessuna riparazione: un pezzo rotto resta rotto finché non arriva la stazione (M3 §8 domanda 12 r.297; §12.7 r.528; CL-3c r.86).
- RIP-07 [doc] I pezzi rotti "restano montati finché non si riparano o sostituiscono" (GIOCO r.42); un pezzo rotto smette di proteggere e disattiva i moduli montati su di esso (GIOCO r.28; M3 §12.7 r.522).
- RIP-08 [doc] Una riparazione che cambia la condizione di un pezzo comporta: riassunto visivo riscritto (si riscrive "solo quando cambia": M3 §5.3 r.174; §4.1 r.48); dati sincronizzati dell'entità aggiornati se il telaio è piazzato (M3 §5.4 r.180); effetti e attributi ricalcolati (la protezione viene dai pezzi integri: GEN §5.4 r.117; M3 §5.7 r.211); sincronizzazione immediata al proprietario per un telaio indossato (M3 §12.4 r.473).
- RIP-09 [doc] Kit e celle come bottino nei forzieri: M6 (GIOCO r.138).

### 2.4 Verniciatura e preset

- VER-01 [doc] "ogni pezzo ha un colore primario e uno secondario, applicabili con le tinte vanilla" (GIOCO r.106); in M4 (GEN §7.10 r.146).
- VER-02 [doc] "Preset: salvataggio di 3 combinazioni colore per giocatore" (GIOCO r.108); in M4 (GEN §7.10 r.146).
- VER-03 [doc] La personalizzazione "va curata fin dalla prima release": è ciò che i giocatori mostrano in screenshot e video (GIOCO r.110; GEN §2 r.20).
- VER-04 [doc][codice] Dato già pronto: un `Appearance(int primary, int secondary, List<Decal> decals)` per slot in `FrameState` (`appearance(slot)`, `setAppearance(slot, …)`); `Appearance.UNPAINTED = -1` vuol dire "colore predefinito del livello"; un colore è `UNPAINTED` o un RGB in [0, 0xFFFFFF]; al massimo `MAX_DECALS = 4` decalcomanie; chiavi NBT `color_primary`, `color_secondary` (assente = `UNPAINTED`, ridotto a 24 bit in lettura) e `decals` (M3 §5.3 r.170; COMPAT r.27, r.300-301; `core/armor/Appearance`).
- VER-05 [doc] Resa: in ogni stato visivo di un pezzo tre ossa per regione, `_p1` primaria e `_p2` secondaria (texture in grigio moltiplicate per il colore) e `_n` neutra, non colorata (M3 §5.1 r.149; ARTE r.25); i colori li applica `SuitBoneRules` (COMPAT r.189-190). Il telaio nudo (ossa `frame<Parte>`) non si colora (M3 §5.1 r.145; COMPAT r.189).
- VER-06 [doc] Colori predefiniti del Rottame: primario `0x8A6F52`, secondario `0x4A4F55` (ARTE r.103); `client/TierPalette` li usa "per ora" per tutti i livelli (COMPAT r.217).
- VER-07 [doc] Il pezzo delle gambe ha solo la regione primaria dalla sesta passata: il secondario non cambia nulla sulle gambe (ARTE r.25; M3 §10 r.322; §11 r.392; CL-3a r.79; CL-3b §7 domanda 11). Decisione del proprietario aperta (C-06).
- VER-08 [doc] L'aspetto arriva agli altri giocatori senza pacchetti dedicati: per la tuta indossata nel riassunto visivo del segnaposto del torso (chiave `visual`), per il telaio piazzato nel dato sincronizzato dell'entità; si aggiorna quando cambia ("pezzo montato, rotto, riverniciato") (M3 §4.1 r.45-48; COMPAT r.28, r.75, r.360).
- VER-09 [doc] Strumento di prova già pronto: `/exoframe debug wear rottame <stato> <primario> <secondario>` veste una tuta di prova dipinta (M3 §13 r.752; CL-3d r.69, r.73).

### 2.5 HUD

- HUD-01 [doc] "Dentro il telaio compare un HUD dedicato con energia, stato dei sei pezzi e moduli attivi; senza elmo (o con elmo rotto) si vede solo la barra dell'energia" (GIOCO r.114). Elmo rotto: "Niente HUD avanzato, niente visori" (GIOCO r.32).
- HUD-02 [doc] In basso a sinistra: barra energia con percentuale e numero di celle di scorta (GIOCO r.116; GEN r.132).
- HUD-03 [doc] In basso a destra: sagoma dell'armatura con i sei pezzi colorati per stato, "verde, giallo, rosso, grigio = rotto" (GIOCO r.117). Gli stati sono integro, danneggiato (sotto il 50%), critico (sotto il 15%), rotto (GIOCO r.40).
- HUD-04 [doc] In alto a destra: icone dei moduli attivi con consumo istantaneo (GIOCO r.118).
- HUD-05 [doc] Bordo dello schermo: leggera vignetta con riflessi del vetro dell'elmo, disattivabile (GIOCO r.119; GEN r.132).
- HUD-06 [doc] Energia sotto il 10%: niente sprint, moduli disattivati, "avviso nell'HUD" (GIOCO r.60). A zero: velocità −70%, protezione al 50%, nessun modulo (GIOCO r.61).
- HUD-07 [doc] L'HUD vero sostituisce la riga dell'energia di M3, "l'unica eccezione al niente HUD" fino a M4 (M3 §2 r.26; §7 punto 31 r.265; §12.13 r.616: "L'HUD vero arriva in M4"; COMPAT r.216; CL-3c r.89).
- HUD-08 [doc] La riga di oggi (`client/EnergyHud`, `client/EnergyLine`): in alto a sinistra, con ombra; prima riga a x = 4, y = 4 con carica, capacità e percentuale ("Energia 7420 / 10000 RE (74 %)"), colorata dagli indicatori del server (verde `NORMAL`, giallo `LOW`, rosso `EMPTY`, bianco senza indicatori); seconda riga a x = 4, y = 14 solo con un pezzo `CRITICAL` (gialla) o `BROKEN` (rossa): il peggiore, cioè un `BROKEN` prima di ogni `CRITICAL`, poi la durata relativa più bassa, a parità l'ordine di `PieceSlot`; percentuale per difetto; si mostra se `SuitSize.wearsSuit` del giocatore locale è vero e `clientView` è presente; si nasconde con F1; con F3 su 1.21.1 la coprono i riquadri di F3, su 1.20.1 può restare sopra le prime righe (M3 §12.13 r.616; COMPAT r.216; CL-3c r.14, r.165). È agganciata con `ClientRenderHooks.addHudRenderer`, chiamato a ogni fotogramma con la `GuiGraphics` (COMPAT r.202-205). Era stata messa in basso a destra e spostata perché lì copriva la barra degli oggetti alle larghezze comuni (M3 r.616).
- HUD-09 [doc] Il client non confronta mai l'energia con una soglia propria: colori e divieti vengono dagli indicatori del server `FrameIndicators(canSprint, canSwim, energyState)` del pacchetto `frame_state_sync` (M3 §12.4 r.475; §5.3 r.175: "così il client non valuta le regole con la propria config").
- HUD-10 [doc][codice][dedotto] Il pacchetto al proprietario porta lo stato intero del telaio più i tre indicatori (M3 §12.4 r.475; COMPAT r.47): con `clientView` il client ha già i moduli installati e i toggle accesi (`active`). Non gli arrivano invece i valori che il server calcola con `FrameRules.evaluate`: `FrameEffects.enabledModules` (i moduli attivi in quel momento, tenuto conto di energia e pezzi rotti) e `moduleMilliRePerTick` (il loro consumo). Per icone e consumo istantaneo (HUD-04) servono dati del server in più, perché il client non valuta le regole con la propria config (HUD-09).
- HUD-11 [doc] I valori del server che servono al client passano da `client_rules` (oggi `cellCapacity`, `cellSwapSeconds`, `exitHoldSeconds`), controllati dal client con i limiti della config; il server non legge mai `ClientRules` (M3 §12.4 r.480-484). AUDIT r.320: "Valori che servono all'HUD vanno sincronizzati".
- HUD-12 [doc] Domanda aperta al proprietario sulla posizione della riga (CL-3c passo 14 r.166; §22 domanda 1 r.389): "È comoda lì? Si può spostare (per esempio in basso a sinistra, sopra la barra degli oggetti, o accanto al mirino) o cambiare nei colori." Nessuna risposta registrata (§8). Vedi C-01.

### 2.6 Tasti

- TAS-01 [doc] Predefiniti: V tenuto 1 s esce dal telaio; R cambia cella; G menu radiale dei moduli; N toggle del visore notturno; H nasconde l'HUD; "Tutti i tasti sono rimappabili dalle impostazioni dei controlli" (GIOCO r.121-129; GEN r.132).
- TAS-02 [doc][codice] Esistono V (`client/ExitKey`) e R (`client/CellSwapKey`, id `key.redstone_exoframes.cell_swap`), registrati con `KeyMappingRegistry` di Architectury nella categoria della mod; si tengono per i secondi di `client_rules` (`exitHoldSeconds`, predefinito `frame.exitHoldSeconds` = 1; `cellSwapSeconds`, predefinito 2) con l'avanzamento nella barra delle azioni; contano solo con la tuta disegnata e nessuna schermata aperta; con tutti e due giù vince l'uscita (M3 §12.10 r.561; COMPAT r.215).
- TAS-03 [doc] Nuovi in M4: G, N, H. G e N agiscono sui moduli, che arrivano in M5 (C-05). Il pacchetto C2S `module_toggle` è "rinviato (M4)" (COMPAT r.259). Ogni file che registra un pacchetto C2S deve nominare `Compat.frameState()`: il server valida sullo stato del telaio (COMPAT r.374, regola 5).
- TAS-04 [doc] Al primo ingresso di ogni sessione una riga in chat nomina i tasti con `Component.keybind` (oggi uscita e cambio cella) (M3 §12.13 r.622).

### 2.7 Animazioni e grafica rimandate a M4

- ANI-01 [doc] Animazione di apertura (GeckoLib) all'ingresso (GIOCO r.20) e posa di ingresso: M4 (M3 §2 r.26; §7 punto 5 r.239: "Ingresso immediato e uscita con V tenuto, senza animazioni di apertura: arrivano in M4"). Oggi l'ingresso è immediato, "Un solo tick" (M3 §5.5 r.193).
- ANI-02 [doc] Prima di qualunque controller di animazione va ridefinito `getInstanceId` in `SuitRendererImpl`: in GeckoLib 4.9 l'id predefinito `(long) Math.pow(idEntità, 7) * -(slot + 1)` satura per gli id da 512 in su, e su 1.21.1 tutte quelle entità condividerebbero un `AnimatableManager` (M3 §10 r.328).
- ANI-03 [doc][codice] Contratto da tenere: l'entità telaio non è un animabile di GeckoLib, "perché solo per quelle GeckoLib chiama l'aggancio che applica la visibilità di pezzi e stati" (M3 §10 r.317; COMPAT r.189). L'unico `GeoItem` è `suit_model`, mai indossato, che "Non ha animazioni" (COMPAT r.93); `animations/exoframe.animation.json` è vuoto (`"animations": {}`).
- ANI-04 [doc] Arti e testa che attraversano le piastre: le ossa della tuta prendono le rotazioni del giocatore (gambe circa ±69° camminando e ±80° correndo, braccia ±49° e ±57°; l'elmo gira dentro il collare); due strade, scelta del proprietario: un passo proprio della tuta (animazione) o un'oscillazione ridotta degli arti della tuta nel renderer (M3 §10 r.327; ARTE r.81-85). CL-3a r.89 chiede al proprietario se è accettabile fino ad allora.
- ANI-05 [doc] Contatto della coscia in avanti con la falda davanti del torso (da 1 grado) e con la pancia (da 29) (M3 §10 r.329).
- ANI-06 [doc] Braccia del pilota allargate dentro le braccia della tuta invece di pendere: serve un aggancio sulla posa dopo `setupAnim`, "proposto ... insieme alle animazioni (M4)" (M3 §4.7 r.109; §10 r.330).
- ANI-07 [doc] Cambio cella: GIOCO r.50 vuole un'animazione di 2 s, "vulnerabile durante il cambio"; M3 l'ha rimandata senza milestone (M3 §5.6 r.206; CL-3c r.87). Vedi C-09.
- GRA-01 [doc][codice] Luci di redstone: la maschera `textures/entity/exoframe_glowmask.png` è generata ed è nel repository; il layer (`AutoGlowingGeoLayer` di GeckoLib) "arriva in M4" (M3 §5.1 r.151; PIANO-3a r.20; ARTE r.119). Nessun codice la usa oggi.
- GRA-02 [doc] Passaggio sulla grafica (M3 §11 r.365-370): l'ombra sotto la tuta e sotto il telaio piazzato è quella del giocatore vanilla, raggio 0,5 (COMPAT r.212; CL-3b §7 domanda 2); mantello, pappagalli e frecce conficcate seguono il pilota e non la tuta (M3 §10 r.319); da accovacciato il pilota resta più in alto del dovuto (testa 2,7 px, busto 2,1, braccia 3,4: M3 §10 r.320), da correggere "se la prova sul PC lo trova evidente"; vettorializzare `Face.occlusion` nel generatore se un secondo livello raddoppia il modello (M3 §10 r.323).
- GRA-03 [doc] Generatore oltre un livello, elencato da M3 §10 r.331 sotto "Per M4": i nomi delle ossa non portano il livello, un secondo livello grande come il Rottame non ci sta nelle regioni dei pezzi dell'atlante, grandezza e regioni dell'atlante sono fisse anche nel test Java. Serve ai tier 2-3 di M5 (§7).
- GRA-04 [doc] Segnaposto negli slot armatura: cliccandolo l'oggetto lampeggia per un tick; "il mixin di rifinitura può arrivare in M4 senza cambiare il resto" (M3 §4.3 r.60; §7 punto 3 r.237).
- GRA-05 [doc][codice] GIOCO r.40: "critico (scintille sotto il 15%)", "rotto (pezzo annerito)"; M3 rende i quattro stati con quattro copie della geometria su zone diverse dell'atlante (M3 §5.1 r.148); nei sorgenti non c'è nessuna particella. Le scintille non hanno una milestone (C-10).
- GRA-06 [doc] Il braccio della tuta in prima persona è fatto (parte 3d, M3 §13; era in M4: M3 §2 r.26); restano senza animazioni proprie del braccio (CL-3d r.22) e con quattro domande al proprietario (CL-3d §5).

## 3. Riporti di M3 verso M4: elenco completo

Ogni voce che M3 (design, checklist, COMPAT) manda a M4, con il rimando alla voce di §2 che la tratta.

| ID | Voce rimandata a M4 | Fonte | Stato oggi e note |
|---|---|---|---|
| RP-01 | Stazione di manutenzione e GUI; verniciatura dal gioco | M3 §2 r.26 (non obiettivi, "M4–M5"); §7 punto 4 r.238 | [codice] nessun blocco né menu (ST-07) |
| RP-02 | HUD vero al posto della riga dell'energia | M3 §2 r.26; §7 punto 31 r.265; §12.13 r.616; COMPAT r.216; CL-3c r.89 | HUD-07, HUD-08 |
| RP-03 | Posizione della riga dell'energia: domanda al proprietario | CL-3c passo 14 r.166; §22 domanda 1 r.389 (prima M3 §6 r.231 la lasciava al costruttore) | nessuna risposta registrata; C-01 |
| RP-04 | Moduli attivi ("M4–M5") | M3 §2 r.26 | C-05 |
| RP-05 | Animazioni di apertura e posa di ingresso | M3 §2 r.26; §7 punto 5 r.239 | ANI-01 |
| RP-06 | Riparazione dei pezzi | M3 §8 domanda 12 r.297; §12.7 r.528; CL-3c r.86 | §2.3 |
| RP-07 | Il telaio raccolto come oggetto perde `reinforced` e `unlocked`; "li imposta la stazione" | M3 §11 r.351; §12.14 r.664 | [codice] `ItemDataAccess.FrameItemData(owner, unlocked, reinforced, cellEnergy, pieces)` con `frame`/`setFrame` esiste, ma nessun codice fuori da `compat/impl` lo chiama: l'oggetto restituito è un telaio nuovo senza dati; `reinforced` è salvato nello stato e nell'NBT ma nessuna regola del core lo legge (L-09) |
| RP-08 | Interruttore di sblocco dal gioco (oggi i comandi `/exoframe unlock` e `lock`) | M3 §5.5 r.197; §7 punto 4 r.238 | ST-04 |
| RP-09 | Moduli montati a mano senza i controlli dell'installatore (`FrameState.restoreModules`); decidere se il server tiene i moduli fuori regola invece di scartarli quando la config cambia fra due avvii | M3 §11 r.350; §12.14 r.663; COMPAT r.356 | [codice] `core/module/ModuleInstaller.Reason`: `OK`, `WRONG_PIECE`, `NO_FREE_SLOT`, `TIER_TOO_LOW`, `DUPLICATE`, controllati nell'ordine WRONG_PIECE, TIER_TOO_LOW, DUPLICATE, NO_FREE_SLOT; capienza = `pieces.baseModuleSlots` (config) + `Tier.extraModuleSlots()` (costante: "la config non ha ancora una voce per i livelli"). Oggi il server ricarica con `installModule` (valida contro la propria config), il client con `readTrusted` (senza controlli) |
| RP-10 | Pezzi con livello, slot o id di modulo sconosciuti dentro un record leggibile: si scartano al caricamento e il salvataggio dopo li cancella | M3 §11 r.348; §12.14 r.662 ("M4/M5"); COMPAT r.357 | regola in vigore: chi aggiunge un livello, uno slot o un id di modulo alza lo `schema` del registro (oggi 2), così una versione più vecchia lo apre in sola lettura; per gli oggetti la direzione è fondere le voci ignote invece di sostituire tutto `pieces` |
| RP-11 | Pacchetto C2S `module_toggle` | COMPAT r.259 | TAS-03 |
| RP-12 | Extra data dei menu; basi client (`CompatContainerScreen`, anteprima d'entità, basi GeckoLib, `ClientTiming`) | COMPAT r.260, r.276 | ST-09 |
| RP-13 | Mixin di rifinitura del lampeggio di un segnaposto cliccato | M3 §4.3 r.60; §7 punto 3 r.237 | GRA-04; un mixin nuovo cambia gli elenchi esatti controllati da `tools/check_jars.sh` (DEC-07) |
| RP-14 | Layer luminoso delle luci di redstone | M3 §5.1 r.151; PIANO-3a r.20 | GRA-01 |
| RP-15 | Da M3 §10 "Per M4": arti e testa nelle piastre; `getInstanceId`; coscia; braccia del pilota allargate; generatore oltre un livello | M3 §10 r.325-331; §4.7 r.109 | ANI-02, ANI-04, ANI-05, ANI-06, GRA-03 |
| RP-16 | Da M3 §11 "Per il passaggio sulla grafica (M4)": ombra; mantello, pappagalli, frecce; pilota accovacciato; `Face.occlusion` | M3 §11 r.365-370 | GRA-02 |
| RP-17 | Animazione del cambio cella, "rimandata" senza milestone | M3 §5.6 r.206; CL-3c r.87 | ANI-07; C-09 |
| RP-18 | Braccio della tuta in prima persona, prima rimandato a M4 | M3 §2 r.26; §7 punto 6 r.240; §13 r.725 | fatto nella 3d: non è più M4, ma divide con l'HUD l'angolo in basso a destra (C-02) |

Riporti di M3 con un'altra destinazione, che M4 non deve bloccare: un pezzo di un livello senza oggetto non si smonta a mano (M5: M3 §11 r.355, §12.14 r.665); bordo del mondo che si stringe, comando che elenca i telai, codice di prova nei jar e altre voci "prima dell'alpha pubblica" (M3 §10 r.333-338, §11 r.358 e r.372-375, §12.14 r.666); uscita a più di un blocco e un terzo da terra: resta così (M3 §12.14 r.667). Vedi §7.

## 4. Domande aperte al proprietario che toccano M4

Nessuna risposta è registrata nel repository (§8). Per ognuna la fonte scrive che cosa fa la mod intanto.

| ID | Domanda | Fonte | Che cosa fa la mod oggi |
|---|---|---|---|
| Q-01 | Posizione (e colori) della riga dell'energia | CL-3c passo 14 r.166; §22 domanda 1 r.389 | in alto a sinistra (HUD-08) |
| Q-02 | Piastre che si attraversano camminando e correndo: accettabile fino a M4? E quale soluzione: passo proprio della tuta o oscillazione ridotta degli arti | CL-3a r.89 e r.108; M3 §10 r.327 | gli arti della tuta oscillano come quelli del giocatore |
| Q-03 | Un'ombra più grande per tuta indossata e telaio piazzato | CL-3b §7 domanda 2; M3 §11 r.367 | ombra del giocatore vanilla, raggio 0,5 |
| Q-04 | Colore secondario del pezzo delle gambe | CL-3b §7 domanda 11; M3 §10 r.322, §11 r.383 e r.392 | sulle gambe solo il primario |
| Q-05 | Mantello, pappagalli e frecce conficcate: si decide con le foto della prova sul PC | M3 §10 r.319; §11 r.368; CL-3a voce "Mantello" | seguono il pilota |
| Q-06 | Pilota accovacciato più in alto del dovuto | M3 §10 r.320; §11 r.369 | da correggere "se la prova sul PC lo trova evidente" |
| Q-07 | Grandezza e posto del braccio in prima persona; braccio del pilota con il telaio nudo; mappa a due mani | CL-3d §5 domande 1-4 | K = 1,3, spostamento 4,5 px di modello |
| Q-08 | "Nessuna riparazione dei pezzi fino a M4" | M3 §8 domanda 12 r.297 | si chiude con M4 |
| Q-09 | Grandezza e posizione delle decalcomanie (3,5 × 3,5 px, sulla piastra del braccio e non sullo spallaccio), da far vedere prima di M5 | M3 §10 r.337 | ossa delle decalcomanie nascoste |
| Q-10 | Politica dei mixin in caso di conflitto con un'altra mod sulla stessa chiamata | M3 §10 r.338 | obbligatori (`required`, `defaultRequire: 1`): il client si ferma invece di degradare |
| Q-11 | Domande sui telai piazzati che la stazione tocca: armatura custodita senza un proprietario suo (domanda 1), telai che non si distruggono e chi li toglie (domanda 7), PvP (domanda 8) | CL-3b §7; M3 §11 r.385-389 | come scritto nelle fonti |
| Q-12 | Bilanciamento che l'HUD rende visibile: costo del salto, consumi, tempi di carica | CL-3c §22 domande 2, 8, 12; M3 §8 domanda 4 r.289 | valori di DEC-20 |

Le altre domande aperte non toccano M4 e risultano anch'esse senza risposta: M3 §8 domande 1-3 e 5-11; le altre decisioni di M3 §11 (r.386, r.387, r.390, r.391); CL-3b §7 domande 3-6, 9 e 10; CL-3c §22 domande 3-7, 9-11 e 13.

## 5. Decisioni già prese che M4 deve rispettare

### 5.1 Architettura e regole dei sorgenti

- DEC-01 [doc] Un solo sorgente, quattro varianti (Stonecutter con Architectury Loom); le differenze di versione o di loader solo in `compat/impl/`, `platform/` e `compat/Ids.java` (COMPAT r.371 e r.375, regole 2 e 6; M3 §3 r.32; GEN §4 r.53).
- DEC-02 [doc] `core/` in Java puro (Java 17), senza import di Minecraft, dei loader, di Architectury né di Gson; ogni tipo di `core/` è citato in un test (regola 1c) e ogni classe nuova del core ha il suo test JUnit (COMPAT r.368-370; M3 §3 r.33; §12.15 r.673).
- DEC-03 [doc] Ogni accesso ai dati degli oggetti passa da `ItemDataAccess` (GIOCO r.171, regola 2; GEN r.53); `NbtKeys` è l'unica fonte delle chiavi (COMPAT r.291).
- DEC-04 [doc] `compat.impl` si nomina solo da `compat/` (regola 4, COMPAT r.373); il codice solo client (`client`, `compat.client`, `compat.impl.client`, `mixin.client`) e le classi client di Minecraft si nominano solo da quei package e dai tre entrypoint client di `platform/` (regole 7 e 8, COMPAT r.376-377): un server dedicato non deve mai caricarli; `RedstoneExoframes.init()` ne resta fuori.
- DEC-05 [doc] Ogni file che chiama `registerC2S(` nomina `Compat.frameState()`, con registrazione e gestori nello stesso file: il server valida ogni richiesta sullo stato del telaio (regola 5, COMPAT r.374). Pacchetti di oggi: `frame_state_sync` (S2C, con i tre indicatori), `frame_exit` (C2S), `cell_swap` (C2S), `client_rules` (S2C) (COMPAT r.47). Su NeoForge 1.21.1 un invio S2C a un client senza il canale si salta (COMPAT r.44).
- DEC-06 [doc] Ogni cucitura nuova entra in `docs/compat.md` nello stesso task (M3 §12.16 r.719).
- DEC-07 [doc] Mixin solo dove nessun evento copre il bisogno (M3 §3 r.35); obbligatori (`required`, `defaultRequire: 1`), senza refmap, bersagli verificati con `javap` sui quattro jar, elenchi esatti delle sezioni `mixins` e `client` controllati da `tools/check_jars.sh` (COMPAT r.238-242). Per la 3d "l'elenco dei mixin ammessi non cresce" (M3 §13 r.729): un gestore nuovo è entrato in una classe esistente. Agganci nuovi di M4 (animazioni, braccia del pilota, lampeggio dello slot) toccano questa regola e la domanda Q-10 [dedotto].
- DEC-08 [doc] Dipendenze ammesse: Architectury API, GeckoLib (4.8.4 su 1.20.1, 4.9.3 su 1.21.1), Cloth Config opzionale e solo per la schermata di config su Fabric (GEN §3 r.40; §5.3 r.101). Il telaio non dipende da mod esterne (GEN r.32).
- DEC-09 [doc] Anti-obiettivi: nessun nome, forma o texture riconoscibile di Fallout, Iron Man o Halo; nessun asset di Mjolnir; nessun dato personale nei jar (controllo `tools/check_jars.sh` nella CI) (M3 §2 r.28; GEN §2 r.30, §10 r.185). La grafica è generata da script nel repository, i file generati sono committati e la CI li rigenera identici; le anteprime si mostrano al proprietario prima dell'integrazione (M3 §3 r.36; GEN §7.11 r.147); i PNG generati non hanno metadati (M3 §3 r.37).

### 5.2 Stato, registro e che cosa si salva dove

- DEC-10 [doc] Lo stato del telaio vive in un registro unico sul server (dato salvato nell'overworld, `data/redstone_exoframes_frames.dat`, `schema` 2); l'entità nel mondo ne è solo la vista; entrare e uscire spostano un record fra `worn` e `placed` dello stesso file (M3 §4.5 r.70; §5.3 r.172; GEN §5.4 r.113, §7.14 r.150; COMPAT r.340). Il file si scrive per intero accanto e prende il posto del vecchio, con la copia `_old` (M3 §4.5 r.72).
- DEC-11 [doc][codice] Scritture solo sul server: `update` (crea il record se manca; nessuna funzione della 3c lo usa, perché un pacchetto arrivato dopo un'uscita creerebbe un telaio dal nulla: M3 §12.10 r.566), `mutate` (solo se il record esiste, nessuna sincronizzazione: alta frequenza), `changeWorn` (solo un record indossato esistente, strutturale, sincronizza), `clear`; per i piazzati `place`, `placed`, `placedIn`, `updatePlaced` (nessuna sincronizzazione), `removePlaced`; `enter`, `exit`; armatura stivata con `stow`, `takeStowed`, `takeStowedOfPlaced`; riassunto per l'entità con `writeSummaryTag`/`readSummaryTag` (COMPAT r.68-75). Ogni operazione del registro tranne `mutate` segna il cambio come strutturale (COMPAT r.69); COMPAT r.72 dice `updatePlaced` "come `mutate`" solo per l'assenza di sincronizzazione: `compat/impl/FrameRegistry.updatePlaced` segna il cambio strutturale anche se la modifica lancia, quindi una scrittura della stazione su un telaio piazzato rientra nel salvataggio all'uscita del giocatore (DEC-12).
- DEC-12 [doc] All'uscita di un giocatore dopo un cambio strutturale non ancora salvato si salvano tutti i giocatori e il solo file del registro, anche con `/save-off` (M3 §4.5 r.70; §12.14 r.654; §7 punto 45 r.279).
- DEC-13 [doc] Registro in sola lettura (file illeggibile senza una copia valida, `schema` più recente, file senza sezioni): ogni scrittura non fa nulla e restituisce falso o vuoto, con un ERROR una volta; una voce non letta non si toglie mai (COMPAT r.68, r.77, r.340; M3 §4.5 r.72).
- DEC-14 [doc] Nulla si duplica e nulla si perde (M3 §2 r.28; r.398). Eccezioni dichiarate: la creativa con l'inventario del tutto pieno (M3 §7 punto 26 r.260) e il caricatore, che custodisce cella e polvere nei dati del chunk come un contenitore del gioco base (M3 §7 punto 27 r.261; §12.11 r.587). Ciò che la mod restituisce e non entra nell'inventario cade a terra fermo, con il ritardo di raccolta di un oggetto lanciato (due secondi) (M3 §7 punto 26 r.260).
- DEC-15 [doc] Che cosa si salva dove:

| Dato | Dove sta | Chi lo riceve | Fonte |
|---|---|---|---|
| Stato del telaio: sei pezzi (livello, durabilità, durabilità massima, moduli, aspetto per slot), cella (`energy`, `capacity`), proprietario, `unlocked`, `reinforced`, toggle accesi (`active`), riporto del danno (`carry_milli`) | registro: `worn` per UUID del giocatore (con `id`), `placed` per `frameId` (con `dim`, `pos`, `yaw`) | solo il giocatore che lo indossa, con `frame_state_sync` | GEN §5.4 r.113; M3 §4.5 r.70, §5.3 r.172; COMPAT r.293-325, r.340 |
| Armatura civile stivata | dentro il record (`stowed`: `slot`, `data_version`, `item`); le voci rimaste di telai tolti in `orphaned` | nessuno, mai ai client | M3 §5.3 r.173; COMPAT r.308-317 |
| Riassunto visivo `SuitSummary` (per slot: livello, condizione, aspetto) | dati del segnaposto del torso, chiave `visual`; per un telaio piazzato, dato sincronizzato dell'entità | ogni client che vede il giocatore o l'entità | M3 §4.1 r.45, §5.3 r.174, §5.4 r.180; COMPAT r.28, r.306-307 |
| Entità del telaio piazzato | nel suo NBT solo l'identità (UUID = `frameId`) | — | M3 §5.4 r.180 |
| Dati sugli oggetti | radice `exoframes` (tag dello stack su 1.20.1, `minecraft:custom_data` su 1.21.1): cella `energy`; pezzo `tier`, `durability`, `max_durability`, `modules`, `color_primary`, `color_secondary`, `decals`; telaio come oggetto `owner`, `unlocked`, `reinforced`, `cell`, `pieces` (oggi il gioco non li scrive: RP-07) | chi ha lo stack | COMPAT r.27, r.29, r.293-305 |
| Contenuto del caricatore | dati del chunk: `has_cell`, `cell_energy`, `redstone`, `reserve`, `carry_milli`; nessuno stack | — | M3 §12.11 r.575; COMPAT r.326-338 |
| Config | `config/redstone_exoframes.json` | server e client leggono ciascuno il proprio; i valori del server per il client passano da `client_rules` | M3 §12.9 r.539, §12.4 r.480 |
| Memorie di sessione | record del tick legato all'oggetto giocatore; insieme di UUID della riga dei tasti, svuotato a `PLAYER_QUIT`; ultimo cambio cella per UUID | — | M3 §12.3 r.443; §12.13 r.622; §12.10 r.563 |
| Preset colore (nuovo in M4) | non deciso | — | L-06 |

- DEC-16 [doc] Ordine atomico già usato fra registro e inventario: prima la modifica del registro, poi, solo se è riuscita, si prende o si consegna l'oggetto (cambio cella: M3 §12.10 r.565; scambio della cella su un telaio piazzato: r.567).

### 5.3 I sei pezzi e gli oggetti

- DEC-17 [doc][codice] I sei slot (GIOCO r.30-37; `core/armor/PieceSlot`):

| Slot | Quota di protezione | Slot moduli di base | Effetto se rotto |
|---|---|---|---|
| Elmo (`HELMET`) | 15% | 1 | niente HUD avanzato, niente visori |
| Torso (`TORSO`) | 30% | 2 | consumo di energia +25% |
| Braccio sx (`LEFT_ARM`) | 12,5% | 1 | modulo del braccio disattivato |
| Braccio dx (`RIGHT_ARM`) | 12,5% | 1 | modulo del braccio disattivato |
| Gamba sx (`LEFT_LEG`) | 15% | 1 | velocità −15% |
| Gamba dx (`RIGHT_LEG`) | 15% | 1 | velocità −15% |

- DEC-18 [doc][codice] Slot moduli di un pezzo = slot di base + slot extra del livello (GIOCO r.96): extra 0, +1, +2, +2, +3 per Rottame, Industriale, Corazzata, Netherite, Prototipo (GIOCO r.70-74; `core/armor/Tier`, costanti). Un telaio unico per tutti i livelli; pezzi di livelli diversi montabili insieme (GIOCO r.66, r.76).
- DEC-19 [doc] Un solo oggetto per braccio e uno per gamba, validi per i due lati: `scrap_helmet`, `scrap_torso`, `scrap_arm`, `scrap_leg` (M3 §7 punto 7 r.241; §5.8 r.216). Quattro segnaposto bloccati negli slot armatura (`exoframe_helmet`; `exoframe_torso`, che porta le braccia; `exoframe_legs`, che porta le gambe; `exoframe_boots`, solo per occupare lo slot), fuori dalla scheda creativa, rimessi a posto da una guardia a ogni tick, senza mixin (GEN §5.4 r.114; M3 §4.3 r.60; §5.8 r.216). Indossa la tuta, per ingombro, acqua e disegno, chi ha il segnaposto del torso nello slot del petto (M3 §4.8 r.133).

### 5.4 Energia

- DEC-20 [doc][codice] Valori di oggi (config `energy.*` e `frame.*`): cella 10.000 RE; fermo 2 RE/s, cammino 8, corsa 20, salto 15 a salto; torso rotto ×1,25 sui consumi; soglia bassa 10% (`lowThreshold` 0,10); a energia zero velocità ×0,30 (−70%) e protezione ×0,50; caricatore 500 RE per polvere e 50 RE/s; cambio cella 2 s; uscita tenendo il tasto 1 s (GIOCO r.48-61; M3 §12.3 r.463; `core/config/ExoframesConfig`). Una cella dura circa 83 minuti da fermi, 21 camminando, 8 correndo (M3 §12.3 r.463).
- DEC-21 [doc] Stati dell'energia `NORMAL`, `LOW`, `EMPTY`; parlano solo i peggioramenti (da `NORMAL` a `LOW`, e verso `EMPTY`), con un messaggio nella barra delle azioni e un suono; i miglioramenti tacciono (M3 §12.3 r.465).
- DEC-22 [doc] Sincronizzazione al proprietario: subito a un cambio strutturale o quando cambiano `FrameEffects`, stato dell'energia o condizione di un pezzo; altrimenti al più ogni 5 tick finché c'è qualcosa da mandare (M3 §3 r.34; §12.4 r.473). Ogni client riceve solo il proprio stato (COMPAT r.360).
- DEC-23 [doc] Il client non valuta le regole con la propria config e non confronta l'energia con soglie proprie (HUD-09); i valori del server che gli servono passano da `client_rules` (HUD-11).
- DEC-24 [doc] Creativa (`instabuild`) e spettatore: nessun consumo e nessuna usura dei pezzi; gli effetti valgono lo stesso (M3 §12.3 r.467; §7 punto 33 r.267).
- DEC-25 [doc] Una cella è solo la sua carica; la capacità è una sola, quella della config del server (sul client quella di `client_rules`) (M3 §12.10 r.555; §7 punto 39 r.273). Il cambio cella prende la cella più carica fra quelle che contengono più energia di quella installata; a parità l'indice più basso; ordine barra degli oggetti 0-8, inventario 9-35, mano secondaria per ultima; intervallo minimo sul server di metà dell'attesa (M3 §12.10 r.563-564).
- DEC-26 [doc] Effetti di base del telaio, anche senza pezzi: niente danno da caduta, resistenza al contraccolpo (0,5), velocità −15%, niente nuoto, niente scatto sotto il 10% (GIOCO r.23; M3 §2 r.18; config `frame.*`); niente cavalcature né letti (M3 §12.3 r.469); passo di un blocco (M3 §12.5 r.492-495).

### 5.5 Danno e condizione dei pezzi

- DEC-27 [doc] Il danno va al pezzo più vicino all'impatto (testa → elmo, proiettili dall'alto → elmo e torso, caduta → gambe) e il resto si divide fra gli altri (GIOCO r.39); quota del primario 60% (`damage.primaryShare`); importi in millesimi con riporto per slot (`DamageRouter.routeMilli`); usura calcolata in `Player.hurtArmor` sul danno prima dell'armatura (M3 §5.7 r.210; §12.7 r.512-521); la caduta non ferisce mai il pilota ma consuma le gambe (M3 §12.8 r.532-534).
- DEC-28 [codice] Soglie delle condizioni, costanti nel codice e non in config: `INTACT` sopra il 50%, `DAMAGED` dal 15% al 50% compresi, `CRITICAL` sotto il 15% e sopra 0, `BROKEN` a 0 (`core/armor/PieceCondition`, confronti in interi). GIOCO r.40 dà le stesse soglie, con una sola differenza sul confine: al 50% esatto il codice dà `DAMAGED`, mentre "danneggiato sotto il 50%" lo lascerebbe integro (un pezzo Rottame a 90 punti su 180).
- DEC-29 [doc] La protezione viene dagli attributi dei pezzi integri; nessuna riduzione del danno fino a M5 (aggancio per la resistenza ai proiettili della Corazzata) (M3 §5.7 r.211; §12.7 r.526). Incantesimi dell'armatura e regole dei mob legate all'armatura non valgono nella tuta fino ai livelli di M5 (M3 §12.7 r.527; §7 punto 46 r.280).
- DEC-30 [doc] Effetti dei pezzi rotti, già nel core: protezione persa, −15% di velocità per gamba, ×1,25 di consumo per il torso, moduli del pezzo disattivati; il pezzo resta montato (M3 §12.7 r.522; GIOCO r.28-37).

### 5.6 Config

- DEC-31 [doc] Un solo file JSON (Gson), `config/redstone_exoframes.json` nella cartella di `Compat.platform().configDir()`, letto una volta all'inizio di `RedstoneExoframes.init()`, prima di ogni registrazione; nessun ricaricamento, né comando né `/reload` (M3 §12.9 r.539; COMPAT r.22; GEN §9 r.171: un solo file, con sezione per i modpack).
- DEC-32 [doc] File assente: si scrivono i predefiniti (file temporaneo e spostamento atomico) solo se il file non esiste. File leggibile: `ConfigValidation.validate` e il risultato normalizzato. Illeggibile, non JSON, tipo sbagliato o chiave di enumerazione sconosciuta: tutta la config ai predefiniti. Sezione `null`: solo quella ai predefiniti, con un problema. Campo assente: il suo predefinito, senza problema. Mappa scritta in parte: un problema, e ogni chiave mancante prende il predefinito con la riga `<mappa>.<CHIAVE>: missing, using default <valore>`. Chiavi in più: ignorate. Un file con problemi non si tocca mai (M3 §12.9 r.540-547; §7 punto 41 r.275).
- DEC-33 [doc] Limiti superiori di `ConfigValidation`: capacità della cella ≤ 1.000.000; ritmi e costi ≤ 100.000; `chargerRePerRedstone` ≤ 1.000.000; secondi dell'uscita e del cambio cella ≤ 30; moltiplicatori ≤ 10. Fuori dai limiti vale il predefinito, con una riga fra i problemi; i prodotti che possono traboccare si calcolano in `long` (M3 §12.9 r.548).
- DEC-34 [doc] Ogni campo nuovo entra in `ConfigValidation` e in `ExoframesConfigTest` (M3 §12.9 r.549); i test in mondo partono dai predefiniti (`tools/run_gametests.sh` toglie il file: r.550).
- DEC-35 [doc][codice] Tutti i valori di bilanciamento e i contenuti generati nel mondo si regolano o si spengono dalla config (GEN §2 r.19; GIOCO r.62, r.140). Le costanti che server e client devono avere uguali (la scala S) non stanno nella config (M3 §4.8 r.136). Sezioni di oggi: `energy`, `damage`, `pieces`, `tiers`, `modules`, `frame`, `world`, `server`; per M4 esistono già `damage.repairKitFraction` = 0,25 e `server.ownerOnlyFrames` = vero.
- DEC-36 [doc] Valori del server per il client: `client_rules` mandato a `PlayerEvent.PLAYER_JOIN`, controllato dal client con i limiti di `ConfigValidation` (fuori dai limiti vale il predefinito, con una riga di log una volta sola), dimenticato a `CLIENT_PLAYER_QUIT`; il server non legge mai `ClientRules` (M3 §12.4 r.480-484).

### 5.7 Permessi e regole trasversali

- DEC-37 [doc] Ogni interazione con un telaio piazzato (entrare, montare, smontare, togliere la cella, ritirare l'armatura stivata, raccoglierlo) chiede di esserne il proprietario, oppure un telaio sbloccato o senza proprietario, oppure `server.ownerOnlyFrames` falso; un operatore in creativa può sempre (M3 §5.4 r.183). Un telaio bloccato non lo tocca nessun altro "in nessun modo" (M3 §7 punto 9 r.243). `unlock` e `lock` sono del proprietario, `debug` e `remove` degli operatori (M3 §7 punto 22 r.256). Predefinito "solo il proprietario", sblocco per i server da config (GEN §7.5 r.141).
- DEC-38 [doc] Il clic di uno spettatore o di un giocatore morto non fa nulla (M3 §5.4 r.184); il cambio cella non agisce con un menu aperto (`containerMenu != inventoryMenu`, M3 §12.10 r.562), e i tasti tenuti contano solo senza schermate aperte (TAS-02): con la GUI della stazione aperta non partono [dedotto].
- DEC-39 [doc] Regole della 3c per il codice nuovo: nessun `isCreative()` (si guardano `getAbilities().instabuild`, `getAbilities().invulnerable` e `isSpectator()`); in creativa l'oggetto in mano non si consuma; le funzioni chiamate a ogni tick catturano `RuntimeException`, scrivono un ERROR alla prima eccezione per giocatore e sessione e poi continuano; ogni regola di tempo confronta `server.getTickCount()` con un valore salvato, mai `now % n`, e riparte da zero se il contatore è tornato indietro (M3 §12 r.400-405).
- DEC-40 [doc] Suoni: tutti da `frame/FrameSounds`, suonati dal server con `level.playSound(null, …)`, solo costanti di `SoundEvents` che sono un `SoundEvent` semplice sulle due versioni, volume 1 e tono 1, con un registratore per i test; gli slot armatura cambiano in silenzio (M3 §12.13 r.626-643; §7 punto 43 r.277).
- DEC-41 [doc] Eventi nella barra delle azioni; testi in inglese e italiano (GUI-05); suggerimenti degli oggetti dalla cucitura `CompatItem`/`CompatBlockItem` con il metodo neutro `tooltip(stack, righe, avanzato)` (M3 §12.10 r.557; §12.13 r.618-624).
- DEC-42 [doc] Ogni evento del telaio (piazzato, raccolto, rimosso, indossato, lasciato, lasciato alla morte) ha una riga INFO nel log con identità, giocatore, dimensione e posizione (M3 §5.5 r.195; §12.14 r.649).
- DEC-43 [doc] Il riassunto visivo si riscrive solo quando cambia (M3 §5.3 r.174) e una scrittura sostituisce del tutto la precedente (M3 §10 r.318).

### 5.8 Prove e consegna

- DEC-44 [doc] JUnit per ogni classe nuova del core; test in mondo (GameTest) che esistono sulle quattro varianti, con le differenze di versione nelle attese passate da aiuti di `compat/GameTests` (oggi 208 per variante) (M3 §12.15 r.671-673; §13 r.752).
- DEC-45 [doc] "Nessun test in mondo può vedere un disegno": ciò che si vede lo provano il client vero nel cloud (script in `tools/client/`, scatti nel repository, controlli fatti da uno script sugli scatti) e la checklist (M3 §13 r.752; §12.15 r.691; §3 r.38).
- DEC-46 [doc] Server dedicato avviato e fermato sulle quattro varianti a ogni parte (M3 §5.9 r.226; §12.15 r.693).
- DEC-47 [doc] Checklist per il PC in `docs/test-in-gioco/` nella forma delle precedenti (intestazione, cosa c'è da vedere, cosa non c'è ancora, già verificato nel cloud, passi numerati, cosa riportare, domande per il proprietario), seguibile alla lettera (M3 §13 r.752; piano della 3d, `docs/superpowers/plans/2026-11-17-m3d-braccio-prima-persona.md`, r.290).
- DEC-48 [doc] In M3 ogni parte si è unita a `main` quando build, test, test in mondo e server dedicato erano verdi nel cloud; le prove sul PC procedono in parallelo e ciò che trovano diventa il primo task della parte dopo (M3 §4.6 r.76; §7 punto 12 r.246; scelta del costruttore).

### 5.9 Indicazioni dall'audit di riuso (non requisiti)

- AUD-01 [doc] HUD da riscrivere dalle idee; il cancello del visore diventa un cancello dell'HUD a tre livelli `NONE`/`BAR_ONLY`/`FULL` nel core; un payload dell'HUD completo (AUDIT r.144-147).
- AUD-02 [doc] Stazione (AUDIT §2.6, r.183-204): blocco con `CompatEntityBlock`; menu con `MenuRegistry.ofExtended` e registrazione in un `ModMenus`; schermata con `CompatContainerScreen`; la cella del multiblocco da adattare, oppure da sostituire con una proprietà `HALF` del blockstate "stile letto"; la block entity della stazione da riscrivere dalle idee, con parte della logica nel core.
- AUD-03 [doc] Durante un'animazione: blocco dei comandi e della visuale lato client da riscrivere come `ExoframeInputLock` (AUDIT r.299) e annullamento delle interazioni lato server come `ExoframeInteractionLock` (AUDIT §2.6).
- AUD-04 [doc] Inventari con `SimpleContainer` e `ContainerHelper` del gioco base, una sola implementazione per tutti i loader (AUDIT r.340); tasti V, R, G, N, H con `KeyMappingRegistry` di Architectury (AUDIT r.327).
- AUD-05 [doc] HUD: un solo callback per fotogramma, `hideGui` controllato a mano; barre con `GuiGraphics.fill`, identico sulle due versioni, invece di `Tesselator` (AUDIT r.347, r.391).
- AUD-06 [doc] Superata: "la stazione si anima per un giocatore a scala 1.0" (AUDIT r.577; C-07).

## 6. Conflitti e lacune

### 6.1 Conflitti fra i documenti, dal più pesante per il progetto

Per ognuno: i due testi, quale vale secondo §0.2, chi deve ancora decidere.

- **C-01 Angoli dell'HUD.**
  - GIOCO r.116-118: energia in basso a sinistra, sagoma in basso a destra, moduli in alto a destra.
  - M3 §12.13 r.616 (scelta del costruttore, provvisoria): la riga dell'energia sta in alto a sinistra perché in basso a destra copriva la barra degli oggetti alle larghezze comuni dell'interfaccia; CL-3c §22 domanda 1 r.389 chiede al proprietario se va bene lì o altrove (proponendo in basso a sinistra sopra la barra degli oggetti, o accanto al mirino); M3 §6 r.231 lasciava la posizione al costruttore.
  - Vale: GIOCO per l'HUD vero (P1; la riga di M3 era un'eccezione dichiarata fino a M4). Ma la domanda al proprietario è aperta e senza risposta registrata: decide il proprietario; chi progetta deve misurare gli angoli.
  - Da pesare: in basso a destra stanno il braccio della tuta in prima persona (C-02) e i sottotitoli del gioco base (CL-3c r.138; COMPAT r.216: "la barra degli oggetti, l'indicatore del salvataggio e i sottotitoli, che stanno in basso"); [dedotto, non verificato] in basso a sinistra la chat, in alto a destra le icone degli effetti e le notifiche del gioco base.
- **C-02 HUD in basso a destra e braccio in prima persona.**
  - M3 §13 r.733-746: a 960×540 con scala dell'interfaccia 2 il braccio (K = 1,3, spostamento 4,5 px di modello) occupa il riquadro 286x205+674+335; K è stato scelto lasciando liberi il mirino (quadrato di 60 px al centro), la barra degli oggetti (x 290-670, y 440-540) e la riga dell'energia (x 0-360, y 0-30); `tools/client/m3d_shots.sh` fa quattordici controlli sugli scatti.
  - GIOCO r.117 mette la sagoma in basso a destra; un HUD spostato cambia i rettangoli del criterio (b) e forse i controlli dello script [dedotto].
  - Vale: §13 su M3 §1-§12 (P4), non su GIOCO (P1). È un conflitto di disposizione: decide il proprietario (anche CL-3d §5 è aperta); i controlli della 3d vanno rifatti o aggiornati con l'HUD nuovo [dedotto].
- **C-03 Colori delle condizioni.**
  - M3 §12.13 r.616 e COMPAT r.216: pezzo `CRITICAL` giallo, `BROKEN` rosso; energia verde, gialla, rossa (scelta del costruttore).
  - GIOCO r.117: sagoma "verde, giallo, rosso, grigio = rotto", quattro colori per i quattro stati di GIOCO r.40; solo il grigio è accoppiato a uno stato, il resto (verde integro, giallo danneggiato, rosso critico) è dedotto dall'ordine.
  - Vale: GIOCO per la sagoma (P1, P5). Chi progetta decide se la barra e i messaggi seguono gli stessi colori.
- **C-04 Regola dell'elmo.**
  - GIOCO r.114 e r.32: senza elmo, o con l'elmo rotto, si vede solo la barra dell'energia.
  - Oggi la seconda riga mostra il pezzo messo peggio con o senza elmo: la riga si mostra a ogni giocatore che indossa la tuta e di cui il client ha lo stato (M3 §12.13 r.616).
  - Vale: GIOCO (P1). Lacune: se un elmo `CRITICAL` conta come funzionante; se la "barra" comprende il numero di celle di scorta e l'avviso di energia bassa; che cosa vede chi indossa il telaio nudo [dedotto: senza elmo, solo la barra]. Decide chi progetta.
- **C-05 Moduli in M4 o in M5.**
  - M3 manda a M4 i controlli dell'installatore per i moduli montati a mano, "non esistono ancora oggetti modulo" (§11 r.350; §12.14 r.663), la scelta sui moduli fuori regola (COMPAT r.356), il pacchetto `module_toggle` (COMPAT r.259), e scrive "moduli attivi (M4–M5)" (§2 r.26). GIOCO mette gli slot dei moduli al centro della GUI (r.104), i tasti G e N (r.125-126) e le icone dei moduli nell'HUD (r.118).
  - GEN §8 r.160 mette i cinque moduli in M5, e il "fatto quando" di M4 (r.159) non nomina i moduli; i moduli si fabbricano alla stazione (GIOCO r.97), cosa che è di M5.
  - Vale: GEN §8 come piano delle milestone (GEN §7 non tocca i moduli); i riporti di M3 dicono "M4" perché lì nasceva la stazione. Nessun documento dice se M4 crea oggetti modulo o prepara solo i posti (slot, tasti, icone, pacchetto). Decide chi progetta, sentito il proprietario.
- **C-06 Colore secondario delle gambe.**
  - GIOCO r.106; M3 §2 r.22, §5.1 r.149: ogni pezzo ha due colori.
  - ARTE r.25; M3 §10 r.322, §11 r.392: il pezzo delle gambe ha solo la regione primaria. Proposta del costruttore: una fascia corazzata alla caviglia e altre parti piccole nel secondario, con l'aspetto predefinito invariato.
  - Vale: GIOCO (P1), ma la voce è fra le decisioni aperte del proprietario (M3 §11 r.383, r.392; CL-3b §7 domanda 11). La verniciatura di M4 ne dipende direttamente.
- **C-07 Scala della tuta e stazione.**
  - AUDIT r.577: "la stazione si anima per un giocatore a scala 1.0", con il telaio che non cambia le dimensioni del giocatore.
  - M3 §4.7 r.101 e §4.8 r.123-132: tuta S = 1,65, armata alta circa 3,09 blocchi, ingombro in piedi 0,8 × 2,97; il telaio piazzato ha lo stesso ingombro (M3 §5.4 r.179).
  - Vale: M3 (P2). [dedotto] Un telaio "sul blocco" (ST-02) chiede circa 2,97 blocchi liberi sopra la stazione; ogni animazione o coreografia della stazione va pensata per la tuta grande.
- **C-08 Milestone delle decalcomanie.**
  - NOTE-M2b r.27 ("the decal milestone (M4, the station)") e r.35 ("M4 adds a `compat/BannerDecals` interface").
  - GEN §7.10 r.146 (entro M5); M3 §2 r.26 ("entro M5") e §5.1 r.150 (ossa nascoste fino a M5).
  - Vale: GEN §7 e i design (P1, P6): M5. M4 non le fa ma non deve bloccarle (§7).
- **C-09 Animazione del cambio cella.**
  - GIOCO r.50: animazione di 2 s, "vulnerabile durante il cambio".
  - M3 §5.6 r.206: "Animazione rimandata", senza milestone; oggi solo avanzamento nella barra delle azioni, suono e riga dell'energia (CL-3c r.87).
  - Vale: GIOCO per il contenuto (P1); la milestone non è assegnata. Decide chi progetta se entra con le animazioni di M4; "vulnerabile" non ha una regola scritta.
- **C-10 Scintille dello stato critico.**
  - GIOCO r.40: danneggiato = texture graffiata, critico = scintille, rotto = pezzo annerito.
  - M3 §5.1 r.148: quattro copie della geometria, una per stato, su zone diverse dell'atlante; [codice] nessuna particella nei sorgenti.
  - Vale: GIOCO (P1); nessuna milestone. Decide chi progetta, o il proprietario, se M4 le aggiunge con il passaggio sulla grafica.
- **C-11 Differenze minori.**
  - GEN §5.1 r.72-73 mette stazione e caricatore in `block/` e gli oggetti in `item/`; il codice ha caricatore e oggetti in `content/` (M3 §12.11 r.571, §12.16 r.704; COMPAT r.120).
  - GEN §5.2 r.90 elenca "menu type" fra le funzioni di `Platform`; oggi `Platform` non lo ha (COMPAT r.19) e COMPAT r.260 dice che arriva con la stazione.
  - GIOCO r.184 non elenca i tasti in M4, GEN r.159 sì.
  - NOTE-M2b r.52-55 ("once M4 loads a config file") è superata: la config si legge da file dalla 3c (M3 §12.9).
  - GIOCO r.101 ("ci si piazza davanti il telaio") e r.103 ("sul blocco o entro 2 blocchi") non si contraddicono, ma non dicono come si misura la distanza (L-02).

### 6.2 Lacune: ciò che i documenti non dicono

- **L-01 Il blocco 2x1.** Forma (due blocchi affiancati in orizzontale? orientamento dallo sguardo di chi lo posa?), posa e rottura delle due metà, che cosa cade, altezza della forma di collisione (decide a che altezza sta un telaio "sul blocco"), ricetta, durezza e attrezzo, block entity sì o no. Decide chi progetta; l'aspetto lo vede il proprietario (DEC-09).
- **L-02 "Entro 2 blocchi".** Misurati da dove (blocco, centro, bordi; in orizzontale o in tre dimensioni); quale telaio se ce n'è più d'uno; se la stazione agisce anche sulla tuta indossata o solo su un telaio piazzato; che cosa succede se il telaio sparisce o si allontana con la GUI aperta (entità ricreata dal riallineamento, chunk scaricato, telaio raccolto o indossato da un altro). [doc] L'oggetto telaio rifiuta di piazzare dove l'ingombro non è libero, dove il suolo manca o è più di tre blocchi sotto (M3 §5.4 r.182).
- **L-03 Contenuto e vita della GUI.** Se il pannello di sinistra è un disegno piatto o un modello; che cosa contengono in M4 gli slot centrali (materiali, kit, tinte, pezzi, moduli); se montaggio e smontaggio dei pezzi si fanno dalla GUI già in M4 (GIOCO r.19, r.101); se gli oggetti messi negli slot restano nel blocco o tornano al giocatore alla chiusura. [doc] Oggi `CompatBlockEntity` non porta stack né il provider dei registri di 1.21.1 (COMPAT r.139-140), e un contenuto nei dati del chunk ha la garanzia del caricatore, non quella del registro (M3 §7 punto 27 r.261; anti-obiettivo r.28). Due giocatori sulla stessa stazione o sullo stesso telaio: non descritto oltre i permessi (DEC-37) e la validazione dei pacchetti (DEC-05).
- **L-04 Riparazione.** Quale materiale ripara un pezzo Rottame (GIOCO r.70 dice "Ferro + rame"), quanti punti rende un'unità, se un pezzo rotto costa di più, se si ripara fino al massimo; i materiali dei livelli 2-5 per pezzi misti. Kit: ricetta, dove si usa ("sul campo": dentro la tuta? su un telaio piazzato? su un pezzo sciolto?), quale pezzo ripara e chi lo sceglie, impilabilità. In config c'è solo `damage.repairKitFraction`. Decide il proprietario (bilanciamento) o chi progetta.
- **L-05 Verniciatura.** Da tinta a RGB: le tinte del gioco base sono 16, `Appearance` accetta qualunque RGB a 24 bit; se le tinte si mescolano (come il cuoio del gioco base) o una tinta è un colore; quanto si consuma (una tinta per regione e per pezzo? per tutta la tuta?); come si torna a `UNPAINTED`; un pezzo alla volta o tutti insieme; se si verniciano anche i pezzi sciolti (l'oggetto pezzo porta già `color_primary` e `color_secondary`, COMPAT r.300).
- **L-06 Preset.** Che cosa contiene una "combinazione colore" (una coppia per tutta la tuta o sei coppie); dove si salva (registro del server, dati del giocatore, client); se segue il giocatore fra mondi e server; come si salva, applica e rinomina. [dedotto] Un dato nuovo nel registro tocca la regola dello schema (RP-10) e quella del file senza sezioni (COMPAT r.340).
- **L-07 HUD.** Che cosa conta come "celle di scorta" (tutte le celle dell'inventario, solo le cariche, o quelle che il cambio cella prenderebbe, DEC-25); come appaiono un pezzo mancante e il telaio nudo nella sagoma; la forma dell'"avviso" di energia bassa (oggi messaggio e suono alla transizione, DEC-21); il telaio senza cella (oggi in pratica non si vede: CL-3c r.88); se l'HUD si nasconde con F1 e con F3 e da spettatore; la vignetta "disattivabile" da chi e dove; H: uno stato che dura fra le sessioni o no. [doc][codice] La config è un file letto una volta all'avvio, senza ricaricamento e senza una sezione per il client (DEC-31, DEC-35); Cloth Config è solo opzionale su Fabric (GEN §3 r.40): oggi non c'è un posto per le preferenze del client.
- **L-08 Tasti G e N senza moduli.** Che cosa fanno in M4 (nulla, un messaggio, un menu vuoto); conflitti con altre mod (il rischio è già noto per V: M3 §11 r.381); se la riga dei tasti al primo ingresso (TAS-04) nomina anche i nuovi.
- **L-09 Rinforzato e sblocco.** GIOCO r.76: "esiste un solo upgrade del telaio (rinforzato) che aumenta la durabilità base": nessun numero; il telaio non ha durabilità propria (ce l'hanno i pezzi); nessun oggetto, ricetta o costo; `reinforced` oggi non ha effetti (RP-07). Lo sblocco dalla stazione: se segue la regola dei comandi, solo il proprietario (DEC-37), lo decide chi progetta.
- **L-10 Animazione di apertura e atomicità.** Oggi l'ingresso è un solo tick, con i controlli in quest'ordine: permesso, cella carica, non già in un telaio, ingombro libero, armatura stivata del proprietario (M3 §5.5 r.193). I documenti non dicono che cosa succede se durante l'animazione il giocatore si muove, esce dal server, muore, o un altro clicca il telaio; se c'è anche un'animazione di uscita; posa di ingresso e blocco dei comandi durante l'animazione (AUD-03).
- **L-11 Config dei valori nuovi.** Quali valori di M4 vanno in config (distanza della stazione, rese della riparazione, numero dei preset, ...). [dedotto] Un file di config esistente non si riscrive mai (DEC-32): le chiavi nuove di M4 non compariranno nel file di chi ha già giocato M3 e valgono i predefiniti; va detto nella documentazione per chi fa modpack.
- **L-12 Suoni delle azioni nuove.** Riparare, verniciare, usare il kit, aprire la stazione, sbloccare, salvare un preset: la tabella di M3 §12.13 r.628-639 non li ha; valgono le regole di DEC-40.
- **L-13 Ricette della stazione e del kit.** Nessun documento le fissa. Vincoli del generatore: un solo oggetto per risultato (`validate()` impone `count` 1), categorie conosciute (`equipment`, `redstone`), ingredienti con id semplici e senza tag, un avanzamento di sblocco per ricetta, `tools/check_data.sh` che rifiuta file in più (M3 §12.12 r.593-607). [dedotto] Un kit che esce in più copie per ricetta non passa il `validate()` di oggi.

## 7. Che cosa non è M4: milestone 5 e dopo, e che cosa M4 deve preparare

| Voce | Milestone | Che cosa M4 deve preparare o non bloccare | Fonte |
|---|---|---|---|
| Fabbricazione alla stazione dei pezzi dei tier 2-3 e dei cinque moduli, non al banco da lavoro | M5 (tier 4-5 e moduli restanti: M7) | posto nella GUI; un meccanismo di ricette della stazione: `CustomRecipe`/`CraftingContainer` → `CraftingInput` non ha una cucitura (COMPAT r.267); regole del generatore dei dati (L-13) | GIOCO r.105; GEN r.131, r.160, r.162 |
| Tier 2 Industriale (ferro + redstone, pari al diamante, +1 slot, forza mineraria +20%) e 3 Corazzata (diamante, +30%, +2 slot, resistenza ai proiettili) | M5 | generatore oltre un livello (GRA-03); colori predefiniti per livello (`TierPalette` usa quelli del Rottame per tutti: COMPAT r.217); un oggetto per livello (M3 §11 r.355, §12.14 r.665); materiali di riparazione (RIP-04); slot extra (DEC-18); alzare lo `schema` quando si aggiunge un livello (RP-10); incantesimi e regole dei mob con i livelli (DEC-29); aggancio di riduzione del danno per la resistenza ai proiettili (M3 §5.7 r.211, §12.7 r.526); durabilità 360 e 520 già in config (RIP-05) | GIOCO r.71-72; GEN r.129, r.160 |
| Cinque moduli: jetpack (torso, tenere premuto salto in aria, 60 RE/s, al più 4 s poi ricarica), onda d'urto (gambe, entrambe, atterraggio da oltre 6 blocchi, 200 RE), visore notturno (elmo, toggle, 3 RE/s), lama idraulica (braccio, attacco, 10 RE a colpo, +6 danno), servomotori (gambe, passivo, +5 RE/s, velocità +20%, annulla il malus del telaio) | M5 | slot dei moduli nella GUI (GUI-04); controlli dell'installatore (RP-09); `module_toggle` (RP-11); tasti G e N (TAS-03, L-08); icone e consumo nell'HUD con dati del server (HUD-04, HUD-10); indicatore `canSwim` già riservato a "un modulo di nuoto di una milestone successiva" (M3 §12.4 r.477); `MobEffect` → `Holder<MobEffect>` senza cucitura (COMPAT r.270) se il visore usa un effetto del gioco base [dedotto]; "elmo rotto: niente visori" (GIOCO r.32); costi già in config `modules.*` [codice] | GIOCO r.85-94; GEN r.130, r.160 |
| Decalcomanie da stendardo, "alla spalla o al torso" | M5 (GEN §7.10) | `Appearance.decals` (al più 4; `pattern` e `color` come stringhe di al più 64 caratteri: COMPAT r.301; M3 §5.3 r.170); ossa `pieceTorso_decal` e `pieceLeftArm_decal` nascoste (M3 §5.1 r.150); grandezza e posto da mostrare al proprietario (Q-09); composizione delle texture a runtime (GEN §7.10); una cucitura per leggere i disegni da uno stendardo, diversi fra 1.20.1 (registro statico e NBT) e 1.21.1 (registro dei datapack e componente `banner_patterns`) (NOTE-M2b r.35); posto nella GUI per lo stendardo [dedotto] | GIOCO r.107; GEN r.146 |
| Prima dell'alpha pubblica | M5 | codice e strutture dei test e `/exoframe debug` nei jar pubblicati; visori di ricette che mostrano i segnaposto e `suit_model`; politica dei mixin (Q-10); un comando che elenca o trova i telai; bordo del mondo che si stringe; recupero a mano dell'armatura in `orphaned` | M3 §10 r.333-338; §11 r.358, r.372-375; §12.14 r.666 |
| Kit e celle nei forzieri; villaggio in rovina con un telaio abbandonato (2-4 pezzi Rottame danneggiati, cella scarica); Illager Corazzato che lascia pezzi danneggiati | M6 | la riparazione vale anche per i pezzi danneggiati trovati nel mondo [dedotto]; `LootHooks` rinviata (COMPAT r.278) | GIOCO r.135-138; GEN r.161 |
| Tier 4 Netherite (upgrade dalla Corazzata con smithing template, immune a fuoco e lava) e 5 Prototipo (schema dal Laboratorio sepolto, rigenera i pezzi con energia: `TierTrait.SELF_REPAIR`); moduli restanti (scanner, respiratore, trivella, scudo energetico, rilevatore di Sculk; trivella e scudo dalla Corazzata); Laboratorio sepolto; bilanciamento; config completa | M7 | riparazione e fabbricazione alla stazione che non escludano netherite e materiali dell'End [dedotto]; durabilità 650 e 800 già in config | GIOCO r.73-74, r.88-97; GEN r.162; `core/armor/Tier`, `core/module/ModuleType` |
| Compatibilità FE/Energy | dopo la 1.0 | nessuna | GEN §7.4 r.140 |

In breve, M4 deve lasciare un posto (e non chiudere la strada) a: fabbricazione alla stazione, slot dei moduli e loro controlli, tasti G e N, icone dei moduli nell'HUD, slot dello stendardo e decalcomanie, pezzi di altri livelli (oggetti, colori predefiniti, materiali, atlante del generatore), regola dello `schema`.

## 8. Che cosa non ho potuto verificare

- I risultati delle prove sul PC delle parti 3a-3d e le risposte del proprietario alle domande delle checklist: nel repository ci sono solo caselle non spuntate (CL-3a r.108, CL-3b r.139, CL-3c r.381) e nessun rapporto. Tutte le domande di §4 risultano aperte.
- Se la domanda sulla posizione della riga dell'energia (Q-01) ha avuto risposta fuori dal repository.
- Le posizioni sullo schermo degli elementi del gioco base citate in C-01 (chat, icone degli effetti, notifiche): dal funzionamento noto del gioco, non verificate nei jar né in uno scatto; i sottotitoli in basso a destra sono scritti in CL-3c r.138.
- Il codice oltre i controlli puntuali: config, `Piece`, `Tier`, `PieceSlot`, `PieceCondition`, `ModuleInstaller`, `ModuleSlots`, `FrameEffects`, `FrameState`, uso di `FrameItemData` e di `reinforced`, maschera luminosa, file delle animazioni, menu e schermate, particelle, tasti. Il resto di ciò che COMPAT e M3 dicono del codice è riportato, non riverificato.
- Il comportamento di GeckoLib con controller di animazione sulle quattro varianti: nessuna prova.
- Nessuna build, nessun test in mondo, nessun avvio del gioco (regola della ricognizione).
