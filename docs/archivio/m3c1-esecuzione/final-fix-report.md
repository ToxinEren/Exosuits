# Ondata di correzioni dopo la revisione finale della parte 3c-1

FIX_BASE: d67e8b66b0ea797e05ecf1358e35606b24dd97eb (ramo m3c-giocarci).

HEAD a fine lavoro: aef637d3c912ff26e2e41d9ea05122d8f2d95fdf. Tutti i finding della lista corretti; nessuno contestato, nessuno lasciato aperto. Nessun test in mondo nuovo (FC-1 chiede solo JUnit): il numero resta 172 sulle quattro varianti.

## FC-1 (Important) - corretto, commit 37e4ee9

- `src/main/java/com/toxineren/exoframes/compat/impl/FrameStateCodec.java:151-159` (ramo non fidato di `read`): la cella prende `cfg.energy.cellCapacity` solo se la carica salvata ci sta; altrimenti `capacity = max(carica, capacita' scritta)`, cioe' la capacita' scritta se valida e non piu' piccola della carica, altrimenti la carica stessa. Javadoc della classe (righe 31-35) e di `read` (righe 86-93) riscritti. `readTrusted` e il taglio al montaggio, al cambio e nel caricatore non cambiano.
- `src/main/java/com/toxineren/exoframes/compat/impl/FrameRegistryFormat.java:192-221`: alla fine di `read`, un solo WARN per caricamento quando almeno una cella letta ha capacita' maggiore di quella della config: `<N> cell(s) in the frame registry hold more than the configured capacity (<cfg> RE): they keep their own capacity until they are swapped or drained`. Javadoc di `read` aggiornato.
- Test (JUnit, nessun test in mondo nuovo): `FrameStateCodecTest.readNeverCutsAChargeAboveTheConfigCapacity` (40 000 di 50 000 con config 10 000 resta 40 000 di 50 000), `readGivesTheConfigCapacityToACellWhoseChargeFits` (4 000 di 50 000 diventa 4 000 di 10 000; senza capacita' scritta 3 000 di 10 000), `readUsesTheChargeAsCapacityWithoutAValidWrittenOne` (capacita' 0, -5, 30 000 o assente con 40 000: 40 000 di 40 000); `FrameStateSavedDataTest.loadAndSaveNeverCutAChargeAboveTheConfigCapacity` (indossato 40 000 di 50 000 e piazzato 4 000 di 50 000, letti con 10 000, salvati e riletti due volte: 40 000 di 50 000 e 4 000 di 10 000).
- RED: `./gradlew --console=plain :1.21.1-neoforge:test --tests '*FrameStateCodecTest*' --tests '*FrameStateSavedDataTest*'` -> `48 tests completed, 3 failed` (i tre casi sopra la capacita'). GREEN dopo la correzione: `BUILD SUCCESSFUL`, FrameStateCodecTest 18/18, FrameStateSavedDataTest 30/30, RegistryFileTest verde.
- Test della 3c-1 (Task 20) le cui attese si reggevano sul taglio al caricamento: solo `FrameStateCodecTest.readCutsTheCellToTheConfigCapacity` (15 000 di 20 000 letto con 10 000 dava 10 000 di 10 000); riscritto come `readGivesTheConfigCapacityToACellWhoseChargeFits`, e il caso 15 000 di 20 000 e' ora in `readNeverCutsAChargeAboveTheConfigCapacity` (resta 15 000 di 20 000). Nessun test in mondo dipendeva dal taglio.
- Documenti: design §12.10 paragrafo "Una sola capacita'" (eccezione e motivo), README "Configurazione" (una frase), docs/compat.md righe 253, 271 e 294 (la regola di caricamento).
- WARN nei GameTest: controllato nei quattro log della chiusura (sezione "Controlli di chiusura").

## FC-2 (Important) - corretto, commit b5bc1af

- `docs/test-in-gioco/m3b-entrare-e-uscire.md:5` (nota nuova in testa): (1) tabella dei messaggi, seconda cella: scambio, la cella che c'era va nell'inventario (`frame/FrameInteractions.java` `insertCell`, design §12.10), il messaggio "Quello slot e' gia' occupato" resta per i pezzi; (2) tabella e passo 12: testo di `message.redstone_exoframes.no_cell` (it_it.json:32, en_us.json:32); (3) passo 15: barra dell'armatura con i punti dei pezzi montati (`FrameAttributesImpl.apply`, modificatore `frame_armor`); (4) passo 15: pistone che si ritrae entrando (`FrameSounds.Sound.ENTER` = `PISTON_CONTRACT`) e suoni negli altri gesti, quindi la riga "Nessun suono in questa parte" non vale piu'; (5) passo 30: discesa al primo tick da carrello e barca (`SuitTick` passo 3) e letto rifiutato (`SuitRestrictions`), con "Esci prima dall'esoscheletro" (it_it.json:48). Poi la riga dell'energia in alto a sinistra (testo `hud.redstone_exoframes.energy`), la riga dei tasti al primo ingresso della sessione (`EntryHints`, `message.redstone_exoframes.keys_hint`), la cella che si scarica con `/exoframe debug energy 10000` per ricaricarla, e il rimando a `docs/test-in-gioco/m3c-giocarci.md` con la 3c-2.
- `docs/test-in-gioco/m3a-vedere-il-telaio.md:7` (seconda nota, dopo quella della 3b): (6) sezione 3, "Nuotare": la tuta affonda e cammina sul fondo, niente posa da nuoto, suggerimento `message.redstone_exoframes.water_hint` al primo contatto (it_it.json:49); poi la riga dell'energia (con `debug wear rottame 2` o `3` anche la seconda riga, `DebugWear.durabilityFor`), la riga dei tasti solo entrando in un telaio piazzato, il rimando alla nota della 3b e alla checklist della 3c.
- Passi e scatti di riferimento non toccati. Nessun test (solo documenti).

## FA-1 (Minor) - corretto, commit 9046088 (nessun cambio di codice)

Confermato: `FrameRegistryFormat.read` restituisce `READ_ONLY`/`WITHOUT_SECTIONS` prima di leggere qualunque voce e il file illeggibile lascia il dato vuoto, e `readOnly()` si decide solo al caricamento: con il registro in sola lettura `wornFrameId` e' vuoto per tutti, quindi `DebugEnergy.set` esce con `NOT_WEARING` e `CellSwap.handle` al primo punto. `REFUSED` resta come ramo di difesa.
- `frame/DebugEnergy.java:27-32` (Javadoc di `REFUSED`) e `:38-41` (Javadoc di `set`).
- `command/ExoframeCommand.java:96-97` (classe) e `:238-241` (`setEnergy`).
- `frame/CellSwap.java:62-64` (passo del registro di `handle`).
- design §12.14, voce "Nuovo, `/exoframe debug energy`": in sola lettura risponde "non indossi un telaio", il cambio cella non fa nulla, `REFUSED` solo come ramo di difesa (sostituisce la frase che il ruling del pre-flight presupponeva).

## FA-3 (Minor) - corretto, commit 9046088

- (1) `docs/compat.md:68`: i due chiamanti di `mutate` (`SuitTick`, `SuitDamage.wear`) e la regola di sincronizzazione con l'usura segnata da `SuitTick.markDirty` (come `SuitTick.java:74-80`).
- (2) `compat/impl/FrameRegistry.java:170-176`: `mutateWorn` nomina i due chiamanti di questa parte (stessa modifica di FB-1 (1)).
- (3) `compat/FrameStateAccess.java:62-63`: `get` elenca anche `changeWorn` fra le scritture.

## FB-1 (Minor) - corretto, commit 9046088

- (1) `compat/impl/FrameRegistry.java:170-176` (come FA-3 (2)).
- (2) `docs/compat.md:68` (come FA-3 (1)).
- (3) `docs/compat.md:68`, ultima frase: la sola lettura comprende schema piu' recente o non numerico, file senza schema ne' sezioni, file illeggibile con la copia precedente; `compat/impl/FrameStateAccessImpl.java:40-42` lo stesso.
- (4) `gametest/DamageGameTests.java:31-41`: il commento della classe dice che il colpo di cactus su sei pezzi nuovi e' la base e quali test fanno altro (caduta, anche su gambe rotte; lava; incudine; telaio nudo; creativa e spettatore).

## FC-3 (Minor) - corretto, commit 9046088 (la frase di FC-1 nel commit 37e4ee9)

- `README.md:31` (Comandi): `/exoframe debug energy` e `/exoframe debug energy <RE>`.
- `README.md:33` (Configurazione): i due casi che riportano tutto ai predefiniti (chiave di mappa sconosciuta o in minuscolo, `"HELMT"`, `"scrap"`; chiave scritta due volte, `ConfigFile.java:88-91`, ruling T8-R1-B1); la correzione per tipo di campo (`ConfigValidation.unit` porta al limite le frazioni in [0, 1], con l'esempio 10 -> 1.0; `atLeastOne` porta a 1.0 il moltiplicatore del torso rotto; `validateShares` riproporziona le quote; ogni altro valore fuori dai limiti torna al predefinito).

## FC-4 (Minor) - corretto, commit 9046088

- `docs/compat.md:85` (`survivalPlayer`) e `:86` (giocatore finto): solo `isCreative()` e' forzato, le capacita' seguono la modalita' del mondo dei test (non creativa su 1.20.1-forge), un test che vuole sopravvivenza o creativa la imposta da se' (ruling Process dopo il Task 17).
- `compat/impl/GameTestsImpl.java:76-79` (classe) e `:146-148` (`MockPlayer`); `gametest/GameTestKit.java:104-108` (anche la meta' "chi vuole la creativa chiama `setGameMode(GameType.CREATIVE)`"); in piu' `compat/GameTests.java:35-39` (`mockServerPlayer`), la stessa frase nella cucitura.

## T24-F1 (Minor) - corretto, commit 9046088

- `docs/compat.md:292`: "decidere, insieme al caricamento della config," diventa "decidere in M4, con gli oggetti modulo,".

## FC-6 (Minor) - corretto, commit 9046088

- design §12.15, "Client vero nel cloud": una frase su come e' andata (Task 24): `tools/client/m3c_shots.sh` al posto degli script della ricognizione, che cosa controlla, che cosa lascia alla checklist della 3c-2 (colonne di bolle, scatto sul fondo, controllo in aria) e la lava provata da `lavaKeepsVanillaMovement`; le stesse tre voci aggiunte in coda all'elenco "Checklist per il PC" della stessa sezione.
- design §12.16: `frame/SuitRestrictions`, `frame/EntryHints`, `frame/DebugEnergy`, `client/EnergyLine` e l'aiuto `respawn(player, keepEverything)`.

## T22-R1-B2 (Minor) - corretto, commit 9046088

- design §12.13: i revisori A e C trovano che il Task 24 aveva gia' riscritto il paragrafo per le due righe in alto a sinistra (nessun "·" rimasto); restava la frase d'apertura "Una riga di testo ... piu' il pezzo messo peggio", ora "su due righe: la prima ..., la seconda, su una riga sua e non unita alla prima, con il pezzo messo peggio ..., colorata secondo la sua condizione".

## T16-F1 (Minor) - corretto, commit 9046088

- `compat/impl/FrameAttributesImpl.java:24-29`: "se importo e operazione sono gli stessi non lo tocca; solo se uno dei due e' diverso lo toglie, e aggiunge quello nuovo (anche se mancava)", come `set` alle righe 94-106 e design §12.5.

## FC-5 (Minor) - corretto, commit 62158ac

- (i) `tools/client/m3c_shots.sh:116-136`: dopo `client_shot.sh` conta `moved wrongly` in `$out/server.log`; se ce n'e', una riga `ERRORE: <N> righe 'moved wrongly' in <server.log>: ...` e uscita 1 anche se `client_shot.sh` e' uscito con 0 (poi la riga FAIL dice "moved wrongly"); commento di testa (righe 39-45) e `tools/client/README.md` (paragrafo dopo la tabella di `m3c_shots.sh`) lo dicono.
- (ii) `tools/client/client_shot.sh:180-183`: `rm -f` di `config/redstone_exoframes.json` nelle due cartelle `run-shot-server` e `run-shot-client`, accanto al mondo; commento di testa (righe 31-33) e `tools/client/README.md` (paragrafo delle cartelle di esecuzione).
- (iii) `build.gradle.kts:116-123`: `cleanGameTestWorld` toglie anche `run-gametest/config/redstone_exoframes.json`. `README.md:29`: la frase su `runGameTestServer` ora dice che parte da mondo nuovo e config predefinita come lo script, ma senza i controlli sul numero dei test e sul log.
- Prove: (a) `m3c_shots.sh` copiato accanto a un `client_shot.sh` finto (scrive `server.log` ed esce con il codice dato): `moved wrongly` una volta con uscita 0 -> uscita 1, una riga ERRORE; nessun `moved wrongly` -> uscita 0, nessuna riga; due righe con uscita 1 -> uscita 1, una riga ERRORE. (b) `./gradlew --console=plain :1.21.1-neoforge:cleanGameTestWorld`: il file `versions/1.21.1-neoforge/run-gametest/config/redstone_exoframes.json`, presente prima, non c'e' piu' dopo. (c) `EXO_ACCEPT_EULA=true bash tools/client/m3c_shots.sh 1.21.1-fabric /home/user/exosuits/build/m3c-fix-1.21.1-fabric`: `OK 1.21.1-fabric: scatti in ...`, uscita 0, 121 s, `log-check.txt` vuoto (0 byte), `grep -c 'moved wrongly' server.log` = 0, nessuna riga ERRORE; i due file di config sono stati riscritti con i predefiniti durante l'esecuzione (mtime 01:11, `"cellCapacity": 10000`).

## Controlli di chiusura (su HEAD aef637d)

- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar` -> `BUILD SUCCESSFUL in 38s` (52 task, 43 eseguiti); JUnit: 300 test per variante, 0 fallimenti, 0 errori sulle quattro varianti (dai rapporti XML di `versions/<variante>/build/test-results/test/`); quattro jar in `build/libs/all/`.
- `bash tools/check_sources.sh` -> `OK sources`.
- `bash tools/check_jars.sh` -> `OK jars`.
- `bash tools/run_gametests.sh`:
  - `OK 1.20.1-forge: 172 test obbligatori passati, nessun avviso fuori elenco`
  - `OK 1.20.1-fabric: 172 test obbligatori passati, nessun avviso fuori elenco`
  - `OK 1.21.1-fabric: 172 test obbligatori passati, nessun avviso fuori elenco`
  - `OK 1.21.1-neoforge: 172 test obbligatori passati, nessun avviso fuori elenco`
- WARN nuovo di FC-1 (`hold more than the configured capacity`) nei quattro `versions/<variante>/build/gametest/server.log` di questa esecuzione: 0 righe in ognuno.
- Client vero (FC-5): `EXO_ACCEPT_EULA=true bash tools/client/m3c_shots.sh 1.21.1-fabric /home/user/exosuits/build/m3c-fix-1.21.1-fabric` -> `OK 1.21.1-fabric: scatti in /home/user/exosuits/build/m3c-fix-1.21.1-fabric`, uscita 0, `log-check.txt` vuoto, nessuna riga ERRORE, 0 `moved wrongly`.
- Nessun server, client o build lasciato in esecuzione (resta solo il demone di Gradle, inattivo, come all'inizio).

## Commit (su FIX_BASE d67e8b6)

- `37e4ee9` frame: al caricamento del registro una carica sopra la capacita' non si taglia (FC-1)
- `b5bc1af` docs: nota della 3c-1 in testa alle checklist della 3a e della 3b (FC-2)
- `9046088` docs: testi che i task successivi avevano reso falsi (FA-1, FA-3, FB-1, FC-3, FC-4, T24-F1, FC-6, T22-R1-B2, T16-F1)
- `62158ac` tools: m3c_shots.sh fallisce su moved wrongly; config ai predefiniti (FC-5)
- `aef637d` frame: commento della classe FrameStateCodec riallineato (FC-1, solo la forma)

## Note per chi rivede

- FC-1: la regola nuova vale solo per il lettore non fidato (`FrameStateCodec.read`); il conteggio del WARN guarda le celle con capacita' maggiore di quella della config dopo la lettura, che sono esattamente quelle tenute per l'eccezione (le altre hanno la capacita' della config). Una cella tolta da un telaio con una carica sopra la capacita' diventa un oggetto con quella carica, come gia' oggi per un oggetto con carica sopra la capacita' (la barra non si vede, il suggerimento dice capacita' / capacita'); la carica si taglia quando rientra in un telaio, in un cambio o nel caricatore, come vuole il ruling.
- FC-2: nella nota della 3b ho aggiunto, dentro il punto del suono, che la riga "Nessun suono in questa parte" di "Cosa c'e' da vedere" non vale piu' (stessa causa del passo 15), e una frase su come ricaricare la cella (`/exoframe debug energy 10000`), perche' la cella ora si scarica durante la checklist.
- FC-4: oltre ai punti nominati ho messo la stessa frase nel Javadoc di `compat/GameTests.mockServerPlayer`, la cucitura che dice la stessa cosa.
- FC-6: le tre prove lasciate alla checklist della 3c-2 sono anche in coda all'elenco "Checklist per il PC" di §12.15.
