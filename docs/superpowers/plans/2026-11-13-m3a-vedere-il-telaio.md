# Milestone 3a — Vedere il telaio: piano di implementazione

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** con un comando di debug il giocatore appare con il telaio e i sei pezzi Rottame, disegnati da un solo renderer GeckoLib su tutte e quattro le varianti, con grafica generata da script, segnaposto invisibili negli slot armatura e test in mondo nella CI.

**Architecture:** l'aspetto del telaio indossato viaggia come riassunto visivo (`SuitSummary`) nei dati dello stack segnaposto del torso, che vanilla sincronizza da sola a chi vede il giocatore. Un `RenderLayer` comune sul renderer del giocatore legge quel riassunto e chiama `SuitRenderer`, l'unica cucitura che conosce GeckoLib (un `DyeableGeoArmorRenderer` guidato a mano con uno stack fittizio). Geometria e texture escono da un generatore Python nel repo; i file generati sono committati e la CI verifica che lo script li rigeneri identici.

**Tech Stack:** come M2b (Stonecutter 0.9.8, Architectury Loom 1.17.493, Architectury API 9.2.14 / 13.0.11, Fabric API 0.92.12 / 0.116.18, Forge 47.4.29, NeoForge 21.1.266, JDK 21, bytecode 17/21), più GeckoLib 4.8.4 / 4.9.3 (già dichiarato), GameTest vanilla (`fabric-gametest-api-v1`, `gameTestServer` di Forge e NeoForge), Python 3 con Pillow e numpy per il generatore, three.js e Playwright solo per le anteprime locali.

**Spec:** `docs/superpowers/specs/2026-11-13-m3-telaio-design.md` (§4.1, §4.4, §5.1, §5.2, §5.3 per `Appearance` e riassunto visivo, §5.9). Fatti sulle API: `docs/superpowers/specs/ricognizioni/2026-11-13-m3-geckolib-e-hook-dei-loader.md` (§3 rendering, §4 segnaposto, §5 hook, "Proposed compat/client seams"). Prova dei test in mondo: rapporto e patch salvati in `docs/superpowers/specs/ricognizioni/` dal Task 2. Spike grafico di partenza: `tools/art/spike/` (da riscrivere, non da estendere).

## Global Constraints

- Condizioni Stonecutter solo in `compat/impl/` (comprese le sottocartelle), `platform/` e `compat/Ids.java`; il resto chiama le interfacce di `compat/`. `tools/check_sources.sh` deve stampare `OK sources`.
- `core/` è Java 17 puro: nessun import di Minecraft, loader, Architectury, Gson; ogni tipo di `core/` è citato in almeno un test.
- Il codice solo client vive in `client/`, `compat/client/` e `compat/impl/client/`: nessuna classe fuori da quei package (né `RedstoneExoframes.init()`) li referenzia, tranne gli entrypoint client in `platform/`. Un server dedicato non deve caricarli.
- Ossa di primo livello della geometria e perni (obbligatori per `GeoArmorRenderer`): `armorHead` [0,24,0], `armorBody` [0,24,0], `armorRightArm` [-5,22,0], `armorLeftArm` [5,22,0], `armorRightLeg` [-2,12,0], `armorLeftLeg` [2,12,0].
- Nomi delle ossa sotto ciascuna: `frame<Parte>` (telaio nudo); per i pezzi `piece<Slot>_s<0-3>_p1`, `_p2`, `_n` con `<Slot>` in {`Helmet`, `Torso`, `LeftArm`, `RightArm`, `LeftLeg`, `RightLeg`} e stato 0 integro, 1 danneggiato, 2 critico, 3 rotto; ossa riservate `pieceTorso_decal` e `pieceLeftArm_decal`.
- Risorse: `assets/redstone_exoframes/geo/exoframe.geo.json`, `textures/entity/exoframe.png`, `animations/exoframe.animation.json`, e `textures/entity/exoframe_glowmask.png` (maschera delle parti luminose: in 3a viene solo generata, il layer che la usa arriva in M4).
- Chiavi NBT nuove, tutte sotto la radice `exoframes`: `decals` (lista di `{pattern, color}`), `visual` (riassunto visivo: `{pieces:{<slot>:{tier, condition, color_primary?, color_secondary?, decals?}}}`), `condition` (`intact`, `damaged`, `critical`, `broken`).
- Colori: interi RGB 0..0xFFFFFF; `Appearance.UNPAINTED = -1` significa "colore predefinito del livello".
- Mixin: solo i due del Task 12, lato client (design §4.7). Nessuna forma, nome o texture riconoscibile di Fallout, Iron Man o Halo; nessun asset di Mjolnir.
- Scala della tuta S = 1,65 (dal 13 novembre: Luigi vuole la tuta alta almeno 3 blocchi; prima era 1,4); sollevamento del pilota 24·(S−1) = 15,6 pixel del modello (design §4.7). I due numeri vivono in `client/SuitScale` (codice) e nel generatore (`tools/art`); un test li tiene uguali. Dove i task qui sotto scrivono 1,4, 9,6 px o misure che ne derivano, vale il valore nuovo.
- I PNG generati non hanno metadati; `tools/check_jars.sh` (privacy compresa) deve stampare `OK jars`.
- Identificatori e messaggi d'eccezione in inglese; Javadoc e commenti in italiano con apostrofi ASCII; testi di gioco in `en_us` e `it_it`.
- Ogni task termina con: build delle quattro varianti (`./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build`), `bash tools/check_sources.sh`, commit sul branch del piano e push.

## Review Focus

1. **Server dedicato che carica classi client** attraverso le nuove cuciture: atteso che il server parta su tutte e quattro le varianti. Test: regola 7 di `check_sources.sh` (Task 7) e avvio del server nel Task 10.
2. **Stack segnaposto con riassunto visivo assente o malformato** (altri mod, dati vecchi, `/give`): atteso che il layer non disegni nulla e non lanci mai. Test: `SuitSummaryTagsTest` (Task 3) e test in mondo del comando (Task 9).
3. **Nomi delle ossa nel codice diversi da quelli nella geometria generata**: atteso che un pezzo non sparisca in silenzio. Test: `ExoframeGeometryTest` (Task 5 e 6) che legge il file generato e i nomi usati dal renderer.
4. **Generatore non deterministico o PNG con metadati**: atteso che due esecuzioni diano file identici e che il controllo di privacy passi. Test: `tools/check_art.sh` nella CI (Task 5).
5. **Armatura già indossata quando si usa il comando di debug**: atteso che non venga distrutta. Test in mondo nel Task 9 (l'armatura torna nell'inventario e ricompare con `via`).

---

## Revisione 1 (13 novembre 2026): tuta più grande del pilota

Viste le anteprime del primo telaio, Luigi ha chiesto una tuta più grande del giocatore, più dettagliata e metallica, con una testa propria e con il pilota sollevato dalle gambe della tuta; ha scelto la scala 1,4. Il perché e gli approcci sono nel design, §4.7. Per il piano cambia questo:

- **Ordine di esecuzione:** 1, 2, 3, 4, 5, 9 (fatti), poi 7, 8, 12, 11, 6, 10. Il codice che disegna (7, 8, 12) non aspetta la grafica nuova: lavora sulla geometria committata dal Task 5 e va provato in gioco il prima possibile.
- **Task 7:** verifica solo i nomi delle ossa di primo livello e del telaio; i nomi dei pezzi li verifica il Task 6, che ora usa `SuitBones`.
- **Task 8:** aggiunge `client/SuitScale` e disegna la tuta in scala attorno ai piedi.
- **Task 11 (nuovo):** telaio ridisegnato, portato dalla prova d'arte approvata da Luigi.
- **Task 12 (nuovo):** pilota sollevato, parti del corpo nascoste e oggetto impugnato nella mano della tuta (due mixin client).
- **Task 6:** sospeso; si riscrive sul telaio nuovo quando il Task 11 è chiuso.

**Revisione 2 (13 novembre 2026).** Luigi ha chiesto più stazza, braccia e petto estesi oltre il pilota e proporzioni più vicine a un riferimento che ha mandato; ha approvato la quarta passata della prova d'arte. Il Task 11 è entrato con il telaio della seconda passata come base tecnica; il Task 6, riscritto, lo sostituisce con il telaio approvato e aggiunge i pezzi Rottame. Dal 13 novembre il client vero gira anche nel cloud senza schermo: la resa si verifica lì con gli screenshot (Task 6 e Task 10) e il PC resta per conferma. Ordine rimasto: 6, poi 10.

---

### Task 1: Aspetto (`Appearance`) nel core e nello schema

**Files:**
- Create: `src/main/java/com/toxineren/exoframes/core/armor/Appearance.java`, `core/armor/Decal.java`
- Modify: `core/frame/FrameState.java`, `compat/ItemDataAccess.java`, `compat/NbtKeys.java`, `compat/impl/PieceTags.java`, `compat/impl/ItemDataAccessImpl.java`, `compat/impl/FrameStateCodec.java`, `docs/compat.md`
- Test: `src/test/java/com/toxineren/exoframes/core/armor/AppearanceTest.java`, `core/frame/FrameStateAppearanceTest.java`, `compat/impl/PieceTagsTest.java`, `compat/impl/FrameStateCodecTest.java`

**Interfaces:**
- Produces:
  - `record Decal(String pattern, String color)`: entrambi non nulli, non vuoti, al massimo 64 caratteri, altrimenti `IllegalArgumentException`.
  - `record Appearance(int primary, int secondary, List<Decal> decals)` con `int UNPAINTED = -1`, `Appearance DEFAULT = new Appearance(UNPAINTED, UNPAINTED, List.of())`, `int MAX_DECALS = 4`; il costruttore copia la lista (`List.copyOf`), rifiuta colori fuori da {-1} ∪ [0, 0xFFFFFF] e più di `MAX_DECALS` decalcomanie.
  - `FrameState.appearance(PieceSlot)` → `Appearance` (mai nullo, `DEFAULT` se non impostato) e `FrameState.setAppearance(PieceSlot, Appearance)`; `mount` e `unmount` riportano l'aspetto dello slot a `DEFAULT`.
  - `ItemDataAccess.PieceData(Tier tier, int durability, int maxDurability, List<ModuleType> modules, Appearance appearance)` (sostituisce i due interi dei colori).
  - `NbtKeys.DECALS = "decals"`, `NbtKeys.PATTERN = "pattern"`, `NbtKeys.COLOR = "color"`.
  - Forma su tag: `color_primary` e `color_secondary` assenti significano `UNPAINTED`; `decals` lista di compound; le voci malformate (tipo sbagliato, stringhe vuote o oltre 64 caratteri) si saltano; oltre `MAX_DECALS` si taglia.

- [ ] **Step 1: test** — `AppearanceTest`: `defaultIsUnpainted`, `rejectsColorOutOfRange` (0x1000000 e -2), `rejectsTooManyDecals` (5), `decalsListIsImmutableCopy`; `FrameStateAppearanceTest`: `appearanceDefaultsUntilSet`, `mountAndUnmountResetAppearance`; `PieceTagsTest`: `missingColorsReadAsUnpainted`, `decalsRoundTrip`, `malformedDecalsAreSkipped`, `decalsAreCutAtFour`; `FrameStateCodecTest`: `appearanceRoundTripsAfterMount` (aspetto con due decalcomanie su `TORSO`, colori 0x112233 e `UNPAINTED`).
- [ ] **Step 2: Run** `./gradlew --console=plain :1.21.1-neoforge:test` — Expected: FAIL (simboli mancanti).
- [ ] **Step 3: implementare** come da Interfaces; `FrameStateCodec` scrive l'aspetto nella voce del pezzo con lo stesso codice condiviso di `PieceTags` e lo ripristina dopo il montaggio, come il riporto.
- [ ] **Step 4: Run** `:1.21.1-neoforge:test` e `:1.20.1-forge:test` — Expected: PASS; `bash tools/check_sources.sh` → `OK sources`; build delle quattro varianti.
- [ ] **Step 5:** aggiornare la tabella dello schema in `docs/compat.md`; commit `core: aspetto dei pezzi (colori e decalcomanie) nello stato e nello schema`.

### Task 2: Test in mondo (GameTest) sulle quattro varianti e nella CI

**Files:**
- Create: `src/main/java/com/toxineren/exoframes/gametest/ExoframeGameTests.java`, `compat/GameTests.java`, `compat/impl/GameTestsImpl.java`, `docs/superpowers/specs/ricognizioni/2026-11-13-m3-gametest.md`, `docs/superpowers/specs/ricognizioni/2026-11-13-m3-gametest.patch`
- Modify: `build.gradle.kts` (configurazione di esecuzione `gameTestServer` per variante), `src/main/resources/fabric.mod.json` (entrypoint `fabric-gametest`), `compat/Compat.java`, `RedstoneExoframes.java`, `.github/workflows/build.yml`, `.gitignore`, `README.md`

**Interfaces:**
- Consumes: rapporto e patch della prova, da copiare da `/tmp/claude-0/-home-user/3f7b8ad1-7942-5139-8688-e3b7d5b2e5fc/scratchpad/m3-recon/gametest-spike.md` e `gametest-spike.patch` nei due file di `ricognizioni/` (primo passo del task: sono la fonte del cablaggio per loader). Se i file non esistono più, rifare la prova seguendo `docs/superpowers/specs/2026-11-13-m3-telaio-design.md` §5.9.
- Produces:
  - comando per variante: `./gradlew --console=plain :<variante>:runGameTestServer`, che esce con 0 se tutti i test obbligatori passano e con un codice diverso da 0 altrimenti;
  - `com.toxineren.exoframes.gametest.ExoframeGameTests`: classe comune con i metodi di test; i task successivi aggiungono qui i loro test con il nome `<tema><Caso>` e li registrano nello stesso modo;
  - `interface GameTests { void register(Class<?> testClass); }` e `Compat.gameTests()`: unico punto con le differenze di loader (entrypoint Fabric dichiarato in `fabric.mod.json`, dove `register` non fa nulla; `RegisterGameTestsEvent` di Forge e NeoForge); la registrazione avviene solo quando il server di test è attivo;
  - due test iniziali: `wiringPlacesABlock` (piazza un blocco in una struttura vuota e lo verifica) e `frameStateOnMockPlayer` (`makeMockServerPlayerInLevel()`, `Compat.frameState().update(p, s -> s.setReinforced(true))`, poi `get(p)` con `reinforced()` vero).

- [ ] **Step 1:** copiare rapporto e patch in `ricognizioni/`; leggere la sezione "What the planned task should do".
- [ ] **Step 2:** applicare il cablaggio in forma pulita (non la patch così com'è): configurazione di esecuzione per le quattro varianti, registrazione per loader dietro `compat/GameTests`, struttura vuota come indicato dal rapporto.
- [ ] **Step 3: Run** `:1.21.1-neoforge:runGameTestServer`, `:1.21.1-fabric:…`, `:1.20.1-fabric:…`, `:1.20.1-forge:…` — Expected: su ognuna "All 2 required tests passed" e uscita 0. Invertire una asserzione una volta e verificare uscita diversa da 0, poi ripristinare. Se una variante non regge dopo 20 minuti di lavoro, lasciarla fuori dalla CI e scriverlo nel rapporto con l'errore esatto.
- [ ] **Step 4:** CI: un passo dopo la build che esegue `runGameTestServer` sulle varianti che reggono; `.gitignore` per le cartelle generate; README: una riga su come lanciare i test in mondo.
- [ ] **Step 5:** build delle quattro varianti, `OK sources`, server dedicato che parte ancora (`:1.21.1-neoforge:runServer` fino a `Done`, poi `stop`); commit `test: GameTest senza client sulle quattro varianti e nella CI`.

### Task 3: Riassunto visivo (`SuitSummary`) nei dati dello stack

**Files:**
- Create: `compat/SuitSummary.java`, `compat/impl/SuitSummaryTags.java`
- Modify: `compat/ItemDataAccess.java`, `compat/impl/ItemDataAccessImpl.java`, `compat/NbtKeys.java`, `docs/compat.md`
- Test: `src/test/java/com/toxineren/exoframes/compat/impl/SuitSummaryTagsTest.java`

**Interfaces:**
- Consumes: `Appearance`, `PieceTags` (Task 1); `PieceCondition`, `PieceSlot`, `Tier`, `FrameState` del core.
- Produces:
  - `record SuitSummary(Map<PieceSlot, PieceLook> pieces)` con `record PieceLook(Tier tier, PieceCondition condition, Appearance appearance)` annidato; mappa copiata e non modificabile; `SuitSummary.BARE = new SuitSummary(Map.of())` (telaio nudo); `static SuitSummary of(FrameState state)`.
  - `ItemDataAccess.suitSummary(ItemStack)` → `Optional<SuitSummary>` (vuoto se manca `visual` o non è un compound) e `ItemDataAccess.setSuitSummary(ItemStack, SuitSummary)`.
  - `NbtKeys.VISUAL = "visual"`, `NbtKeys.CONDITION = "condition"`.
  - Lettura tollerante: slot o livello ignoti saltano il pezzo; condizione ignota o assente vale `INTACT`; aspetto letto con `PieceTags`.

- [ ] **Step 1: test** — `roundTripKeepsEveryPiece` (sei pezzi `SCRAP`, condizioni miste, un aspetto con colori e una decalcomania), `bareFrameRoundTrips`, `missingVisualIsEmpty`, `visualOfWrongTypeIsEmpty`, `unknownTierSkipsOnlyThatPiece`, `unknownConditionReadsAsIntact`, `ofFrameStateCopiesTierConditionAndAppearance`.
- [ ] **Step 2: Run** `:1.21.1-neoforge:test` — Expected: FAIL.
- [ ] **Step 3: implementare**; nessuna condizione di versione in `SuitSummaryTags` né in `ItemDataAccessImpl`.
- [ ] **Step 4: Run** test su `1.21.1-neoforge` e `1.20.1-forge` — PASS; `javap -p` di `ItemDataAccessImpl` identico tra le due; build; `OK sources`.
- [ ] **Step 5:** `docs/compat.md`; commit `compat: riassunto visivo del telaio nei dati dello stack`.

### Task 4: Segnaposto degli slot armatura, invisibili

**Files:**
- Modify: `content/ModItems.java`, `compat/impl/ArmorMaterialsImpl.java`, `build.gradle.kts`, `tools/gen_placeholders.py`, `tools/check_jars.sh`, `src/main/resources/assets/redstone_exoframes/lang/en_us.json`, `lang/it_it.json`, `gametest/ExoframeGameTests.java`
- Create: `src/main/resources-mc1.20.1/assets/minecraft/textures/models/armor/frame_layer_1.png`, `frame_layer_2.png`; per ciascuno dei quattro item `assets/redstone_exoframes/models/item/<id>.json` e `textures/item/<id>.png`

**Interfaces:**
- Produces:
  - `ModItems.EXOFRAME_HELMET`, `EXOFRAME_TORSO`, `EXOFRAME_LEGS`, `EXOFRAME_BOOTS` (`RegistrySupplier<Item>`, id `exoframe_helmet`, `exoframe_torso`, `exoframe_legs`, `exoframe_boots`), creati con `Compat.armorMaterials().newPlaceholder(ArmorItem.Type.<HELMET|CHESTPLATE|LEGGINGS|BOOTS>, new Item.Properties())`, non aggiunti alla scheda creativa.
  - Cartella di risorse per versione: la build aggiunge `src/main/resources-mc<versione Minecraft della variante>` alle risorse (serve qui per le texture trasparenti e in 3c per i dati).
  - Su 1.21.1 il materiale `frame` ha `layers = List.of()`; su 1.20.1 le due texture trasparenti 64×32 nel namespace `minecraft`.

- [ ] **Step 1: test in mondo** — `placeholdersAreArmorItems`: i quattro item esistono nel registro, sono `ArmorItem` del tipo atteso, pila massima 1.
- [ ] **Step 2: Run** `:1.21.1-neoforge:runGameTestServer` — Expected: FAIL.
- [ ] **Step 3: implementare**; `tools/gen_placeholders.py` genera le due texture trasparenti e le quattro icone 16×16 (deterministico, senza metadati); `tools/check_jars.sh`: i jar 1.20.1 contengono le due texture trasparenti, i jar 1.21.1 no.
- [ ] **Step 4: Run** i test in mondo sulle varianti attive — PASS; build con `collectJar`; `bash tools/check_jars.sh` → `OK jars`; `javap -c -p` di `ArmorMaterialsImpl` su 1.21.1 senza `ArmorMaterial$Layer`.
- [ ] **Step 5:** commit `content: segnaposto degli slot armatura, invisibili sulle due versioni`.

### Task 5: Generatore grafico e telaio nudo

**Files:**
- Create: `tools/art/exo.py` (descrizione del modello, impacchettamento, esportazione), `tools/art/paint.py` (pittura procedurale), `tools/art/designs/frame.py`, `tools/art/build.py`, `tools/art/README.md`, `tools/art/preview/` (visore three.js, `render.js`, `package.json`), `tools/check_art.sh`, `src/main/resources/assets/redstone_exoframes/geo/exoframe.geo.json`, `textures/entity/exoframe.png`, `animations/exoframe.animation.json`
- Modify: `.github/workflows/build.yml`, `.gitignore` (`node_modules/`, `tools/art/preview/out/`), `README.md`
- Delete: `tools/art/spike/` (dopo averne preso ciò che serve)
- Test: `src/test/java/com/toxineren/exoframes/art/ExoframeGeometryTest.java`

**Interfaces:**
- Consumes: lo spike in `tools/art/spike/` (`exo.py`, `paint.py`, `designs.py`, visore) come punto di partenza: il suo `NOTES.md` elenca cosa funziona e i difetti noti (facce complanari, orientamento delle facce sopra e sotto nell'esportazione, scatole ruotate non controllate).
- Produces:
  - `python3 tools/art/build.py` rigenera i tre file di risorse; `python3 tools/art/build.py --out <cartella>` scrive altrove (serve al controllo); stesso input, stessi byte.
  - `bash tools/check_art.sh`: rigenera in una cartella temporanea e confronta con i file committati; stampa `OK art` o elenca le differenze ed esce con 1.
  - Geometria Bedrock `format_version` 1.12.0, identificatore `geometry.exoframe`, ossa di primo livello e perni come da Global Constraints, ossa `frameHead`, `frameBody`, `frameRightArm`, `frameLeftArm`, `frameRightLeg`, `frameLeftLeg` con il telaio nudo (montanti, giunti, spina dorsale, alloggiamento della cella sulla schiena, piastre di mani e piedi), fronte verso −z.
  - Atlante 256×256 senza metadati; lo spazio per i pezzi (Task 6) è già previsto dall'impacchettatore.
  - `exoframe.animation.json`: `{"format_version": "1.8.0", "animations": {}}`.
  - Anteprime locali: `cd tools/art/preview && npm install && node render.js` scrive `tools/art/preview/out/*.png` (non nella CI).

- [ ] **Step 1: test** — `ExoframeGeometryTest` (legge il `.geo.json` dalle risorse con Gson): `topLevelBonesHaveTemplatePivots` (i sei nomi e i sei perni esatti), `frameBonesExistUnderTheirParents`, `everyCubeIsInsideTheSuitBox` (x in [-13, 13], y in [-1, 36], z in [-8, 9]), `uvRectanglesStayInsideTheAtlas`.
- [ ] **Step 2: Run** `:1.21.1-neoforge:test --tests '*ExoframeGeometryTest'` — Expected: FAIL (file assente).
- [ ] **Step 3: implementare** il generatore riscrivendo lo spike: nomi delle ossa definitivi, controllo delle facce complanari anche verso il basso, nessuna scatola ruotata nel telaio, seme fisso; generare e committare i file.
- [ ] **Step 4: verificare** — test PASS su `1.21.1-neoforge` e `1.20.1-forge`; due esecuzioni consecutive di `build.py --out` danno cartelle identiche (`diff -r`); `bash tools/check_art.sh` → `OK art`; `python3 tools/check_jar_privacy.py` sui jar dopo `collectJar` passa; rendere le anteprime del telaio nudo (fronte, retro, lato) e guardarle: nessuna parte sospesa, nessuna faccia sfarfallante visibile.
- [ ] **Step 5:** CI: installare Pillow e numpy ed eseguire `bash tools/check_art.sh`; README; eliminare `tools/art/spike/`; commit `art: generatore della grafica e telaio nudo`.

### Task 6: Telaio approvato e pezzi Rottame con stati visivi e regioni di colore

> **Riscritto (Revisione 2, 13 novembre 2026).** Luigi ha approvato la quarta passata della prova d'arte («mi piace l'aspetto dell'armatura rottame nelle ultime tavole»): scheletro largo con braccia e petto estesi oltre il pilota, armatura a piastre pesanti color bronzo. Questo task porta nel repo quel telaio (al posto di quello provvisorio del Task 11) e i sei pezzi Rottame, e aggiunge ciò che la prova non ha: gli stati visivi.

**Files:**
- Modify: `tools/art/designs/frame.py`, `tools/art/exo.py`, `tools/art/paint.py`, `tools/art/build.py`, `tools/art/preview/`, `tools/art/README.md`, i file di risorse generati (geometria, atlante, maschera luminosa), `src/test/java/com/toxineren/exoframes/art/ExoframeGeometryTest.java`, `src/main/java/com/toxineren/exoframes/client/TierPalette.java`, `src/test/java/com/toxineren/exoframes/client/SuitScaleTest.java`, `docs/arte/README.md`
- Create: `tools/art/designs/scrap.py`, `docs/arte/anteprime/rottame-v4/`

**Interfaces:**
- Consumes: generatore del Task 11 (densità 2, atlante 512×512 con le regioni `frame` e `piece_s0..s3`, limiti separati per telaio e pezzi, controllo esatto delle facce complanari, figura del pilota sollevata e senza gambe, maschera luminosa); la prova d'arte 2, fuori dal repo: `designs/frame_v4.py`, `designs/scrap_v4.py`, le sezioni "v3" e "v4" di `PORTING.md`, "Third pass" e "Fourth pass" di `NOTES.md`, le tavole `out/v4_armored_S140.png` e `out/v4_frame_S140.png` viste e approvate da Luigi; `SuitBones` (Task 7) per i nomi delle ossa nei test; design §4.7 e §5.1.
- Produces:
  - `designs/frame.py`: lo scheletro della quarta passata; limiti `FRAME_MIN/FRAME_MAX` x ±12,75, y 0..30,7, z −8..10.
  - `designs/scrap.py`: i sei pezzi Rottame della quarta passata; limiti `SUIT_MIN/SUIT_MAX` x ±13,5, y 0..31,5, z −8,75..10,5. Gli spallacci restano nel pezzo `Torso`.
  - per ciascuno dei sei slot e per ciascuno stato 0..3: ossa `piece<Slot>_s<stato>_p1` (regione primaria), `_p2` (secondaria), `_n` (neutra), figlie dell'osso `armor*` corrispondente; le quattro copie di uno slot hanno la stessa geometria e puntano a quattro zone dell'atlante (`piece_s0` .. `piece_s3`);
  - pittura per stato, da scrivere (la prova ha solo lo stato 0): 0 integro (vernice consumata ai bordi, come nelle tavole), 1 danneggiato (graffi lunghi, ammaccature scure, più vernice saltata), 2 critico (danni pesanti, fuliggine, crepe scure), 3 rotto (annerito, crepe larghe, quasi senza vernice). Uno stato più alto non deve mai sembrare meno danneggiato di uno più basso sullo stesso pezzo;
  - regioni primaria e secondaria in toni di grigio chiari (le colora il renderer); la neutra con i colori finali (acciaio, ottone, tubi, feritoie rosse);
  - maschera luminosa: opaca solo sui texel luminosi (cella, spie, feritoie), per tutti e quattro gli stati;
  - ossa `pieceTorso_decal` (rettangolo piatto 7×7 sul davanti del torso) e `pieceLeftArm_decal` (7×7 sul lato esterno dello spallaccio sinistro), 0,25 px sopra la piastra, con texel del tutto trasparenti in M3;
  - pezzi di braccia e gambe sinistri e destri speculari (un solo item per braccio e uno per gamba);
  - colori predefiniti del Rottame: primario `0x8A6F52`, secondario `0x4A4F55`, scritti in `docs/arte/README.md` nella forma che `TierPaletteTest` già legge, e uguali in `client/TierPalette.java`;
  - `SuitScaleTest.matchesTheArtGenerator`: legge `SUIT_SCALE` da `tools/art/exo.py` e lo confronta con `SuitScale.S`.

- [ ] **Step 1: test** — in `ExoframeGeometryTest`: `everyPieceHasFourStatesAndThreeRegions` (i 72 nomi costruiti con `SuitBones.piece`, ognuno sotto l'osso `armor*` giusto), `stateCopiesShareGeometry` (stesso numero di cubi, stesse origini e dimensioni tra gli stati di uno slot e regione), `decalBonesExist` (nomi da `SuitBones.decal`), `leftAndRightPiecesAreMirrored` (braccia e gambe), `frameAndPiecesStayInsideTheirBounds` (ossa `frame*` nei limiti del telaio, ossa `piece*` nei limiti dei pezzi; sostituisce il controllo unico di oggi), `stateRegionsDoNotShareTexels` (le facce di uno stato stanno nella sua zona dell'atlante); in `SuitScaleTest`: `matchesTheArtGenerator`; `TierPaletteTest` resta com'è e deve passare con i colori nuovi.
- [ ] **Step 2: Run** — Expected: FAIL (la geometria committata non ha pezzi).
- [ ] **Step 3: portare e completare** — telaio e pezzi dalla prova (i file del repo non si sovrascrivono con quelli della prova: `exo.py`, `paint.py` e `build.py` del repo sono più avanti); scrivere la pittura dei quattro stati; rigenerare; `Model.check()` senza segnalazioni (distanze dal pilota, facce complanari); due esecuzioni di `build.py --out` identiche; `bash tools/check_art.sh` stampa `OK art`.
- [ ] **Step 4: anteprime** — telaio nudo e tuta armata (fronte, retro, lato) nello stato 0, tuta armata di fronte negli stati 1, 2 e 3, una con due colori di prova diversi dai predefiniti; guardarle e correggere parti sospese o compenetrate e stati che non si distinguono; salvarle in `docs/arte/anteprime/rottame-v4/` (PNG senza metadati).
- [ ] **Step 5: verificare** — test PASS sulle due varianti di riferimento; build delle quattro varianti; `OK jars`, `OK sources`; nel client vero (script del client senza schermo): `wear nudo`, `wear rottame`, `wear rottame 1`, `2`, `3` e `wear rottame 0 3A6EA5 D9D9D9` su almeno una variante per versione del gioco, con gli screenshot allegati.
- [ ] **Step 6:** `tools/art/README.md` e `docs/arte/README.md` (telaio e pezzi della quarta passata, stati, colori predefiniti, misure in blocchi); commit `art: telaio approvato e pezzi Rottame con quattro stati visivi e regioni di colore`.

### Task 7: Renderer del telaio (`SuitRenderer`) e item modello

**Files:**
- Create: `compat/SuitBones.java`, `compat/client/SuitView.java`, `compat/client/SuitRenderer.java`, `compat/client/ClientCompat.java`, `compat/impl/client/SuitRendererImpl.java`, `compat/impl/client/SuitModel.java`, `compat/SuitAssets.java`, `compat/impl/SuitAssetsImpl.java`, `compat/impl/SuitModelItem.java`, `assets/redstone_exoframes/models/item/suit_model.json`
- Modify: `compat/Compat.java`, `content/ModItems.java`, `tools/check_sources.sh`, `gametest/ExoframeGameTests.java`, `lang/*.json`, `docs/compat.md`

**Interfaces:**
- Consumes: geometria e nomi delle ossa del telaio (Task 5); i pezzi arrivano dopo, con il Task 6, e fino ad allora nella geometria non ci sono; ricognizione §3 (chiamate esatte per versione) e "Proposed compat/client seams" punti 1 e 2.
- Produces:
  - `compat/SuitBones` (comune, senza dipendenze client): `static String piece(PieceSlot slot, int state, String region)` con `region` in {`p1`, `p2`, `n`}, `static String frame(String part)`, `static String decal(PieceSlot slot)`, e le costanti dei sei nomi di primo livello. È l'unico posto in cui il codice costruisce nomi di ossa.
  - `record SuitView(Map<PieceSlot, PieceView> pieces)` con `record PieceView(int state, int primaryRgb, int secondaryRgb)` (stato 0..3, colori già risolti, mai -1); mappa vuota significa telaio nudo. `static SuitView of(SuitSummary summary, ToIntFunction<Tier> primaryDefault, ToIntFunction<Tier> secondaryDefault)`: `INTACT` 0, `DAMAGED` 1, `CRITICAL` 2, `BROKEN` 3; un colore `UNPAINTED` prende il valore predefinito del livello.
  - `interface SuitRenderer { void render(LivingEntity entity, HumanoidModel<?> base, PoseStack pose, MultiBufferSource buffers, int packedLight, float partialTick, SuitView view); }`.
  - `ClientCompat.suitRenderer()` (singleton pigro, come `Compat`).
  - `SuitAssets.newSuitModelItem(Item.Properties)` → `Item` (un `GeoItem` mai equipaggiato) e `Compat.suitAssets()`; `ModItems.SUIT_MODEL` (id `suit_model`, fuori dalla scheda creativa).
  - Comportamento del renderer: una sola passata; per ogni slot presente mostra solo le tre ossa dello stato dato e nasconde le altre; slot assente: tutte nascoste; `frame*` sempre visibili; `_p1` colorata con `primaryRgb`, `_p2` con `secondaryRgb`, `_n` bianca; ossa `_decal` nascoste. La visibilità non dipende dai flag `visible` del modello vanilla. Un osso che il renderer cerca e che la geometria non ha si salta senza errori, e il suo nome finisce nel log una volta sola per sessione.
  - Regola 7 di `tools/check_sources.sh`: i package `client`, `compat.client` e `compat.impl.client` si referenziano solo da file di quei package e di `platform/`.

- [ ] **Step 1: test** — in mondo: `suitModelItemIsRegistered`; in `ExoframeGeometryTest`: `rendererFrameBoneNamesExistInGeometry` (i sei nomi di primo livello e i sei nomi del telaio prodotti da `SuitBones` esistono nel file generato; i nomi dei pezzi e delle decalcomanie li verifica il Task 6); `SuitBonesTest` (in `src/test/java/.../compat/`): `pieceNamesFollowTheContract` (per esempio `SuitBones.piece(PieceSlot.LEFT_ARM, 2, "p1")` è `pieceLeftArm_s2_p1`), `rejectsUnknownRegionAndState`; `SuitViewTest` (in `src/test/java/.../compat/client/`): `unpaintedResolvesToTierDefault`, `conditionMapsToState`, `bareSummaryGivesEmptyView`.
- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: implementare**; le condizioni di versione stanno solo in `SuitRendererImpl`, `SuitModel` e `SuitModelItem`; provare la regola 7 con un file temporaneo che importa `client` da `content/` (deve fallire), poi cancellarlo.
- [ ] **Step 4: verificare** — build delle quattro varianti; test PASS; `javap` di `SuitRendererImpl` sulle quattro varianti: classe base `DyeableGeoArmorRenderer` del package giusto per versione; server dedicato che parte su `1.20.1-fabric` e `1.21.1-neoforge` (l'item `GeoItem` si carica sul server senza classi client); `OK sources`.
- [ ] **Step 5:** `docs/compat.md` (cuciture client, regola 7); commit `compat: renderer del telaio su GeckoLib 4.8 e 4.9`.

### Task 8: Layer sul giocatore e avvio del client

**Files:**
- Create: `client/RedstoneExoframesClient.java`, `client/SuitLayer.java`, `client/SuitScale.java`, `client/TierPalette.java`, `compat/client/ClientRenderHooks.java`, `compat/impl/client/ClientRenderHooksImpl.java`
- Modify: `compat/client/ClientCompat.java`, `platform/RedstoneExoframesFabricClient.java`, `platform/RedstoneExoframesForge.java`, `platform/RedstoneExoframesNeoForge.java`, `docs/compat.md`

**Interfaces:**
- Consumes: `SuitRenderer`, `SuitView` (Task 7); `ItemDataAccess.suitSummary` (Task 3); `ModItems.EXOFRAME_TORSO` (Task 4); ricognizione §5 (hook per loader).
- Produces:
  - `RedstoneExoframesClient.init()`: chiama `Compat.frameState().registerClient()` e `ClientCompat.renderHooks().addPlayerLayer(SuitLayer::new)`; i tre entrypoint client chiamano solo questo metodo.
  - `interface ClientRenderHooks { void addPlayerLayer(Function<RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>, RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>> factory); }`, applicato a entrambi i modelli di skin (normale e sottile) su ogni loader.
  - `SuitScale` (Java puro, nessun import di Minecraft): `S = 1.4f`; `PILOT_RAISE_PX = 24f * (S - 1f)`; `static float pilotRaiseBlocks()` = `PILOT_RAISE_PX * 0.9375f / 16f` (il modello del giocatore è reso a 15/16).
  - `SuitLayer`: se lo stack nello slot del torso è `EXOFRAME_TORSO` e ha un riassunto visivo, costruisce la `SuitView` (colore `UNPAINTED` risolto con `TierPalette`) e chiama il renderer dentro una coppia push/pop in cui la tuta è scalata di `S` attorno ai piedi (`translate(0, 1.501, 0)`, `scale(S, S, S)`, `translate(0, -1.501, 0)`: nello spazio del modello i piedi stanno a y = 1,501); in ogni altro caso non disegna nulla e non lancia. `static boolean wearsSuit(LivingEntity)` è l'unico posto che decide se un'entità indossa la tuta (lo riusa il Task 12). Il riassunto letto dallo stack si tiene in cache per stack (nota del Task 3: `suitSummary` copia il tag a ogni lettura).
  - `TierPalette.primary(Tier)` e `secondary(Tier)`: per `SCRAP` i due colori del Task 6; per gli altri livelli, per ora, gli stessi.

- [ ] **Step 1: test** — `TierPaletteTest` (in `src/test/java/.../client/`; `TierPalette` non importa classi di Minecraft): `scrapDefaultsMatchTheArtNotes` (i due colori scritti in `docs/arte/README.md`), `everyTierHasDefaults`; `SuitScaleTest`: `raiseAlignsTheHeadPivots` (`24 + PILOT_RAISE_PX` uguale a `24 * S` entro 1e-4), `raiseInBlocksUsesThePlayerModelScale` (9,6 px danno 0,5625 blocchi).
- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: implementare**; su Forge e NeoForge il listener `AddLayers` si registra sul bus del mod durante la costruzione del mod, dentro il ramo client.
- [ ] **Step 4: verificare** — build; test PASS; `javap` di `ClientRenderHooksImpl` per loader (callback Fabric; `AddLayers` con le skin per nome su Forge e con `PlayerSkin.Model` su NeoForge); server dedicato che parte sulle quattro varianti; `OK sources`.
- [ ] **Step 5:** commit `client: layer del telaio sul giocatore`.

### Task 9: Comando di debug per vestire il telaio

**Files:**
- Create: `command/ExoframeCommand.java`, `frame/DebugWear.java`
- Modify: `RedstoneExoframes.java`, `gametest/ExoframeGameTests.java`, `lang/*.json`

**Interfaces:**
- Consumes: segnaposto (Task 4), `SuitSummary` e `ItemDataAccess.setSuitSummary` (Task 3).
- Produces:
  - `/exoframe debug wear nudo`, `/exoframe debug wear rottame [stato 0..3] [primario RRGGBB] [secondario RRGGBB]`, `/exoframe debug wear via`; livello di permesso 2; registrato con `CommandRegistrationEvent` di Architectury.
  - `DebugWear.wear(ServerPlayer, SuitSummary)`: sposta nell'inventario (o a terra se pieno) ciò che occupa i quattro slot armatura, mette i quattro segnaposto e scrive il riassunto sullo stack del torso; `DebugWear.remove(ServerPlayer)`: toglie i segnaposto dagli slot e dall'inventario. Non tocca `FrameStateAccess`.

- [ ] **Step 1: test in mondo** — `debugWearPutsPlaceholdersAndSummary` (giocatore finto con un elmo di ferro: dopo `wear` i quattro slot hanno i segnaposto, lo stack del torso ha sei pezzi `SCRAP` `INTACT`, l'elmo di ferro è nell'inventario), `debugWearRemoveClearsEverything`, `debugWearWithStateAndColors` (stato 2, colori letti dal riassunto).
- [ ] **Step 2: Run** `:1.21.1-neoforge:runGameTestServer` — Expected: FAIL.
- [ ] **Step 3: implementare**.
- [ ] **Step 4: Run** i test in mondo sulle varianti attive — PASS; build; `OK sources`.
- [ ] **Step 5:** commit `debug: comando per vestire il telaio di prova`.

### Task 10: Client senza schermo nel repo, verifica trasversale, checklist per il PC, documenti

> **Riscritto (Revisione 4, 13 novembre 2026).** Dal 13 novembre il client vero gira nel cloud senza schermo (prova fuori dal repo: script, init script di Gradle, client RCON e rapporto). Questo task porta lo strumento nel repo, lo usa per la verifica finale della parte 3a sulle quattro varianti e scrive la checklist per il PC, che ora serve a confermare e a coprire ciò che lo script non vede.

**Files:**
- Create: `tools/client/client_shot.sh`, `tools/client/shot.init.gradle`, `tools/client/rcon.py`, `tools/client/m3a_shots.sh`, `tools/client/README.md`, `docs/test-in-gioco/m3a-vedere-il-telaio.md`, `docs/test-in-gioco/m3a-screenshot/`
- Modify: `README.md`, `docs/compat.md`, `.gitignore`

**Interfaces:**
- Consumes: la prova del client senza schermo, fuori dal repo (`client_shot.sh`, `spike.init.gradle`, `rcon.py`, `REPORT.md`); il comando `/exoframe debug wear` (Task 9); tutto ciò che la parte 3a ha costruito.
- Produces:
  - `tools/client/client_shot.sh <variante> <cartella> ["comando" ...]`: avvia sotto Xvfb un server dedicato locale in modalità offline e il client di sviluppo della variante, fa entrare il giocatore, esegue i comandi via RCON e salva `back.png`, `front.png`, `quarter.png` più i log; chiude tutto alla fine. Lavora sul checkout in cui si trova, in cartelle di esecuzione proprie (`versions/<variante>/run-shot-client` e `run-shot-server`, ignorate da git), senza toccare `run/` e `run-gametest/`. Porta, dimensione della finestra e nome del giocatore da variabili d'ambiente.
  - L'EULA di Minecraft non si accetta in silenzio: lo script parte solo con `EXO_ACCEPT_EULA=true` e, senza, si ferma spiegando che impostarla significa accettare l'EULA per quel server di prova.
  - `tools/client/m3a_shots.sh <variante> <cartella>`: l'insieme standard della parte 3a (telaio nudo; Rottame negli stati 0, 1, 2, 3; Rottame con due colori di prova; un oggetto in ogni mano; posa sdraiata forzata con i comandi), una cartella per scatto.
  - `tools/client/README.md`: cosa serve (Xvfb, xdotool, ImageMagick, Mesa software, JDK 21), come si usa, quanto dura, cosa non copre (vista laterale vera, audio, fluidità, secondo giocatore), i messaggi d'errore innocui nel log.
  - `docs/test-in-gioco/m3a-screenshot/`: una selezione degli scatti finali sulle quattro varianti (PNG senza metadati, ognuno sotto i 200 KB), citati dalla checklist come riferimento di ciò che si deve vedere.
  - `docs/test-in-gioco/m3a-vedere-il-telaio.md`: checklist per il PC.

- [ ] **Step 1: portare lo strumento** nel repo dalla prova, ripulito: niente percorsi dello scratchpad, niente rimedio per mclib (il progetto lo ha già nel classpath di sviluppo di Forge), messaggi in italiano come gli altri script, uscita diversa da zero se il client o il server non partono o se il giocatore non entra.
- [ ] **Step 2: eseguire** `m3a_shots.sh` sulle quattro varianti; guardare ogni scatto. Deve vedersi: tuta alta circa tre blocchi con i piedi a terra; pilota visibile dalla vita in su nel telaio nudo, mai le gambe; con i pezzi montati nessuna parte della skin; i quattro stati in ordine; i colori di prova solo sulle piastre verniciabili; l'oggetto nella mano della tuta, per entrambe le mani; da sdraiati tuta e pilota a terra; nei log nessun avviso del mod e nessun errore di mixin. Ciò che non torna è un rilievo da riportare, non da spiegare.
- [ ] **Step 3: checklist per il PC**: avvio del client di ogni variante dai jar di produzione (i jar di `collectJar`, non l'ambiente di sviluppo: è la prova che i mixin si applicano fuori dallo sviluppo); gli stessi controlli dello Step 2 con gli scatti di riferimento accanto; in più ciò che il cloud non copre: vista laterale, accovacciarsi, correre, nuotare, anteprima del giocatore nell'inventario, skin normale e sottile, mantello, un secondo giocatore su server dedicato che guarda il primo, fluidità; cosa fotografare e cosa riportare.
- [ ] **Step 4: verifica completa** — build delle quattro varianti con `collectJar`; test JUnit e in mondo; `OK sources`, `OK jars`, `OK art`; CI verde sull'ultimo push.
- [ ] **Step 5:** README (stato della parte 3a, comando di debug, come rigenerare la grafica, come fare gli scatti) e `docs/compat.md` allineati; commit `test: client senza schermo nel repo, scatti e checklist della parte 3a`.

### Task 11: Telaio ridisegnato (scheletro grande con testa e gambe meccaniche)

**Files:**
- Modify: `tools/art/exo.py`, `tools/art/paint.py`, `tools/art/build.py`, `tools/art/designs/__init__.py`, `tools/art/designs/frame.py`, `tools/art/preview/`, `tools/art/README.md`, `tools/check_art.sh`, i tre file di risorse generati, `src/test/java/com/toxineren/exoframes/art/ExoframeGeometryTest.java`, `docs/arte/README.md`
- Create: `assets/redstone_exoframes/textures/entity/exoframe_glowmask.png` (generata), `docs/arte/anteprime/telaio-v2/`
- Delete: `tools/art/spike/` (il primo spike: il Task 6 ora parte dalla prova d'arte 2)

**Interfaces:**
- Consumes: generatore del Task 5 nel repo (`HashRng`, `compare_png.py`, errore del packer per la faccia più larga della regione: restano); la prova d'arte 2, fuori dal repo, con `designs/frame.py`, `exo.py`, `paint.py`, `build.py`, `designs/__init__.py`, `preview/scene.html`, `preview/shot.js`, `PORTING.md` (elenco delle modifiche) e `NOTES.md`; design §4.7 e §5.1. Tavole viste da Luigi: seconda passata del 13 novembre.
- Produces:
  - `exo.py`: costanti di modulo `SUIT_SCALE = 1.4` e `PILOT_LIFT_PX = 24 * (SUIT_SCALE - 1)`; `DENSITY = 2` e `texels(length)`; `ATLAS_SIZE = (512, 512)` con regioni `frame` righe 0..127, `piece_s0` 128..223, `piece_s1` 224..319, `piece_s2` 320..415, `piece_s3` 416..511; limiti `FRAME_MIN/FRAME_MAX` (x ±9,5, y 0..30,7, z −7..9) per le ossa del telaio e `SUIT_MIN/SUIT_MAX` (x ±10,75, y 0..31,5, z −8..9,5) per i pezzi; `Model.check()` misura le distanze tra il modello scalato di `SUIT_SCALE` attorno ai piedi e la figura del pilota a grandezza normale sollevata di `PILOT_LIFT_PX`, gambe escluse, con i margini di oggi (0,25 px corpo e braccia, 0,5 px testa).
  - `paint.py`: la pittura da metallo della prova (materiali `gun`, `steel`, `brass`, `chrome`, `cable`, `rubber`, `vent`, `cell`, `lamp`, `paint`, `pipe`, `glass`); ogni scelta casuale passa da `HashRng`, che su intervallo o lista vuoti solleva `ValueError`; `paint_model` restituisce atlante e maschera luminosa.
  - `build.py`: scrive anche `textures/entity/exoframe_glowmask.png`; `--out` predefinito come oggi; nel repo entra solo `designs/frame.py` (i pezzi di `scrap.py` sono il Task 6).
  - `designs/frame.py`: lo scheletro della prova, senza scatole ruotate: testa con visiera aperta, busto a gabbia, cella sulla schiena, bacino e cosce chiuse che nascondono il volume delle gambe del pilota, ginocchia, stinchi e piedi meccanici, avambracci con impugnature e mani meccaniche, punti di aggancio per i pezzi. Stessi nomi e perni delle ossa (Global Constraints).
  - `tools/check_art.sh`: confronta anche la maschera luminosa, per pixel come l'atlante.
  - `ExoframeGeometryTest`: atlante 512×512; riquadro delle ossa del telaio x ±9,5, y 0..30,7, z −7..9; il resto invariato.
  - anteprime: visore che disegna la tuta in scala con il pilota sollevato e senza gambe, griglia a blocchi (17,07 px), porta e box di collisione; un comando documentato nel README rifà le viste standard. Le tavole composte della prova (`make.py`) non entrano nel repo.
  - `docs/arte/README.md`: telaio v2, divisione tra scheletro e pezzi, misure in blocchi; anteprime in `docs/arte/anteprime/telaio-v2/` (fronte, retro, lato, confronto con porta), PNG senza metadati. Le anteprime del primo telaio restano come storico.

- [ ] **Step 1: test** — in `ExoframeGeometryTest` portare a 512×512 la dimensione dell'atlante e al nuovo riquadro i limiti del telaio. Expected: FAIL contro i file committati (primo telaio, 256×256).
- [ ] **Step 2: portare** generatore e disegno seguendo `PORTING.md`, partendo dai file del repo (non sovrascriverli con quelli della prova: la prova è partita da una copia più vecchia); rigenerare; due esecuzioni di `build.py --out` devono dare cartelle identiche; `bash tools/check_art.sh` stampa `OK art`, e fallisce se si cambia un texel della maschera luminosa committata.
- [ ] **Step 3: anteprime** — generarle, guardarle (fronte, retro, lato, primo piano di testa e gamba), correggere parti sospese o compenetrate; salvarle in `docs/arte/anteprime/telaio-v2/`.
- [ ] **Step 4: verificare** — test PASS su `1.21.1-neoforge` e `1.20.1-forge`; build delle quattro varianti; `OK jars` (i PNG nei jar senza metadati), `OK sources`.
- [ ] **Step 5:** `tools/art/README.md` e `docs/arte/README.md`; commit `art: telaio ridisegnato, scheletro in scala 1,4 con testa e gambe meccaniche`.

### Task 12: Pilota dentro la tuta (due mixin client)

> **Revisione 3 (13 novembre 2026, dalla revisione del task).** Il testo qui sotto chiedeva di sollevare il pilota nello spazio del mondo, in testa a `PlayerRenderer.render`, e di annullare il sollevamento a ogni uscita. Era un difetto del piano: con il giocatore sdraiato, a nuoto o in caduta da morto la tuta restava sospesa mezzo blocco sopra il corpo. Come è stato fatto: il sollevamento è una traslazione nello spazio del modello, `translate(0, -PILOT_RAISE_PX / 16, 0)`, in coda a `PlayerRenderer.scale(AbstractClientPlayer, PoseStack, float)`, che `LivingEntityRenderer.render` chiama dentro il proprio push/pop dopo le rotazioni; non servono più gli agganci in testa e in uscita di `render` né `PilotPose.lower`. Lo spostamento dell'oggetto impugnato si calcola dal perno corrente del braccio (`PilotPose.handShiftPx`), non dal valore fisso (±2, +0,8). La targhetta col nome non si alza con il pilota: la porterà su l'ingombro reale della parte 3b.

**Files:**
- Create: `mixin/client/PlayerRendererMixin.java`, `mixin/client/ItemInHandLayerMixin.java`, `client/PilotPose.java`, `src/main/resources-mc1.20.1/redstone_exoframes.mixins.json`, `src/main/resources-mc1.21.1/redstone_exoframes.mixins.json`
- Modify: `client/SuitLayer.java`, i metadati dei tre loader (`fabric.mod.json`, configurazione Forge del mixin in `build.gradle.kts`, `neoforge.mods.toml`), `build.gradle.kts`, `tools/check_sources.sh`, `tools/check_jars.sh`, `docs/compat.md`

**Interfaces:**
- Consumes: `SuitLayer.wearsSuit`, `SuitScale` (Task 8); `SuitView` (Task 7); design §4.7 e §5.2; ricognizione, "When to hide skin parts" (`PlayerRenderer.render` chiama `setModelProperties`, che azzera la visibilità a ogni fotogramma).
- Produces:
  - `client/PilotPose` (nessuna annotazione di mixin, tutta la logica sta qui):
    - `static Set<BodyPart> hiddenParts(SuitView view)` con `enum BodyPart { HEAD, BODY, LEFT_ARM, RIGHT_ARM, LEGS }`: `LEGS` sempre; `HEAD` con l'elmo montato, `BODY` con il torso, ogni braccio con il suo pezzo;
    - `static boolean applies(AbstractClientPlayer player)`: `SuitLayer.wearsSuit(player)` e non spettatore;
    - `static void raise(PoseStack pose)`: `translate(0, SuitScale.pilotRaiseBlocks(), 0)`, nello spazio del mondo; `static void lower(PoseStack pose)`: la traslazione opposta;
    - `static void hide(PlayerModel<?> model, Set<BodyPart> parts)`: spegne `visible` della parte e del suo secondo strato (`hat`, `jacket`, `leftSleeve`, `rightSleeve`, `leftPants`, `rightPants`);
    - `static void beforeHandTransform(PoseStack pose, HumanoidArm arm)`: sposta di (±2, +0,8, 0) pixel del modello, cioè dal perno del braccio del pilota a quello del braccio della tuta; `static void afterHandTransform(PoseStack pose)`: `scale(S, S, S)`.
  - `PlayerRendererMixin` su `net.minecraft.client.renderer.entity.player.PlayerRenderer`, metodo `render(AbstractClientPlayer, float, float, PoseStack, MultiBufferSource, int)`, senza stato: in testa, se `PilotPose.applies`, `PilotPose.raise`; subito dopo la chiamata a `setModelProperties`, `PilotPose.hide`; a ogni uscita, se `PilotPose.applies`, `PilotPose.lower`. Niente `pushPose`/`popPose` nel mixin: se un altro mod annulla il metodo dopo il nostro aggancio in testa, non deve restare un push spaiato (lo stack delle pose non vuoto fa crollare il gioco); una traslazione rimasta la elimina il `popPose` di chi ha chiamato il renderer.
  - `ItemInHandLayerMixin` su `net.minecraft.client.renderer.entity.layers.ItemInHandLayer`, metodo `renderArmWithItem`: prima della chiamata a `translateToHand`, `beforeHandTransform`; subito dopo, `afterHandTransform`; solo se l'entità è un giocatore che indossa la tuta.
  - `SuitLayer`: prima della scala annulla il sollevamento (`translate(0, PILOT_RAISE_PX / 16f, 0)`: nello spazio del modello y cresce verso il basso), così i piedi della tuta restano a terra.
  - Configurazione dei mixin per versione (`compatibilityLevel` `JAVA_17` su 1.20.1, `JAVA_21` su 1.21.1), sezione `client`, `defaultRequire` 1; dichiarata nei metadati di ogni loader.
  - Regola 7 di `check_sources.sh` estesa: `mixin.client` è un package solo client; l'eccezione per `platform/` si restringe ai file che ne hanno bisogno, elencati per nome nello script (osservazione della revisione del Task 7). `check_jars.sh`: ogni jar contiene la configurazione, le due classi che elenca, il refmap dove il loader lo richiede, e il riferimento alla configurazione nei metadati del loader.

- [ ] **Step 1: test** — `PilotPoseTest` (in `src/test/java/.../client/`): `legsAreAlwaysHidden`, `bareFrameHidesOnlyTheLegs`, `eachMountedPieceHidesItsPart`, `brokenPieceStillHidesItsPart`; in `SuitScaleTest`: `suitArmPivotOffsetMatchesTheScale` (il perno del braccio della tuta, scalato attorno ai piedi e abbassato del sollevamento, dista (2, 0,8) px da quello del pilota). Se il Task 11 è già unito: `SuitScaleTest.matchesTheArtGenerator` (legge `SUIT_SCALE` da `tools/art/exo.py` e lo confronta con `SuitScale.S`); altrimenti quel test lo aggiunge il task che arriva per secondo.
- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: implementare**; prima verificare con `javap` sulle due versioni del gioco le firme di `PlayerRenderer.render`, `setModelProperties`, `ItemInHandLayer.renderArmWithItem` e il proprietario della chiamata a `translateToHand`, e scriverle nel rapporto.
- [ ] **Step 4: verificare** — build delle quattro varianti; test PASS; test in mondo sulle quattro varianti (il server dedicato parte con i mixin client dichiarati); `OK sources`, `OK jars` con una prova negativa (configurazione tolta da un jar: deve fallire); se esiste lo script del client senza schermo (`docs/` o rapporto della prova), avviare il client di ogni variante e allegare gli screenshot di fronte, retro e lato con `/exoframe debug wear nudo` e con `rottame`.
- [ ] **Step 5:** `docs/compat.md` (i due mixin, perché esistono, dove sta la logica); commit `client: pilota sollevato dentro la tuta, parti coperte nascoste, oggetto nella mano della tuta`.

### Task 13: Piedi e schiena della tuta dal riferimento di Luigi

> **Aggiunto (Revisione 5, 13 novembre 2026).** Visti gli screenshot dal gioco, Luigi: «usa l'immagine di riferimento che ti ho mandato per migliorare i piedi e la schiena della exosuit (ovviamente lascia il modello del redstone core)».

**Files:**
- Modify: `tools/art/designs/frame.py`, `tools/art/designs/scrap.py`, `tools/art/paint.py` (solo se serve un materiale nuovo), i file di risorse generati, `tools/art/README.md` e `docs/arte/README.md` (misure, descrizione di piedi e schiena)
- Create: `docs/arte/anteprime/rottame-v5/`

**Interfaces:**
- Consumes: generatore e disegno del Task 6 (scala 1,65; pezzi con quattro stati; controlli di distanze e facce complanari); l'immagine di riferimento mandata da Luigi (fuori dal repo), usata come nel design §4.7: proporzioni e carattere, non gli elementi che identificano quell'armatura.
- Produces:
  - **piedi** (scheletro e parte bassa dei pezzi delle gambe): più grandi e articolati; suola larga e pesante; punta divisa in due dita squadrate con un'apertura in mezzo; caviglia con puntoni o pistoni visibili ai due lati; un appoggio dietro il tallone; lo schiniere che scende a coprire la caviglia con piastre a gradini e feritoie. Il bordo interno di ogni piede resta ad almeno 0,25 px dal piano di mezzo, perché le gambe ruotano una accanto all'altra.
  - **schiena** (busto dello scheletro e retro del pezzo Torso): la cella di redstone con il suo alloggiamento, le barre di guardia e la luce resta com'è, al centro. Attorno: piastre a strati sulle scapole, un blocco della spina dorsale, puntoni a gradini dalla schiena agli attuatori delle spalle, una griglia di feritoie orizzontali, una piastra lombare e piastre dei fianchi. Le ciminiere si tengono solo se stanno bene con il resto; se no, diventano sfiati bassi.
  - stessi nomi e perni delle ossa, nessuna scatola ruotata, limiti invariati (telaio x ±12,75, y 0..31,25, z −8..10; pezzi x ±13,5, y 0..32, z −8,75..10,5); gambe sinistra e destra speculari; i quattro stati dei pezzi continuano a distinguersi.
  - anteprime prima e dopo (schiena, piedi, figura intera davanti e dietro) in `docs/arte/anteprime/rottame-v5/`.

- [ ] **Step 1:** rendere di schiena e ai piedi lo stato di oggi (anteprime "prima").
- [ ] **Step 2: disegnare** piedi e schiena; `python3 tools/art/build.py` senza segnalazioni; guardare le anteprime e correggere finché piedi e schiena si leggono a colpo d'occhio alla grandezza di uno screenshot di gioco.
- [ ] **Step 3: verificare** — due build identiche; `OK art`; `ExoframeGeometryTest` su `1.21.1-neoforge` e `1.20.1-forge`; build delle quattro varianti; `OK jars`, `OK sources`; nel client vero, su una variante per versione del gioco: tuta armata e telaio nudo, davanti e dietro, con gli scatti allegati.
- [ ] **Step 4:** documenti; commit `art: piedi e schiena della tuta ridisegnati dal riferimento`.

### Task 14: Piedi uguali a quelli del riferimento

> **Aggiunto (Revisione 6, 13 novembre 2026).** Visto il prima e dopo del Task 13, Luigi: «non hai capito, i piedi devono essere uguali a quelli dell'immagine di riferimento, non mi interessa se sono troppo simili, devono essere identici solo quelli». È una sua decisione esplicita, presa dopo essere stato avvisato del rischio di somiglianza con un design altrui, e vale solo per la parte bassa delle gambe; il resto della tuta resta originale (design §4.7).

**Files:**
- Modify: `tools/art/designs/frame.py`, `tools/art/designs/scrap.py`, `tools/art/paint.py` (solo se serve), i file di risorse generati, `tools/art/README.md` e `docs/arte/README.md`
- Create: `docs/arte/anteprime/rottame-v6/`

**Interfaces:**
- Consumes: generatore e disegno dopo il Task 13 (scala 1,65; pezzi con quattro stati; controllo delle facce complanari; `tools/art/swing.py`); i due ritagli ingranditi dell'immagine di riferimento, fuori dal repo (parte bassa delle gambe, davanti e dietro).
- Produces, dal ginocchio in giù, per entrambe le gambe (speculari):
  - **davanti:** piede a trapezio più largo in basso con un'apertura rettangolare ad arco al centro del bordo inferiore; sopra, la fascia della caviglia in due segmenti; lo scudo dello stinco, alto, largo in alto e arrotondato in basso (a gradini), che finisce appena sopra il piede; sullo scudo una piastra interna in rilievo con quattro feritoie orizzontali e, sotto, tre segni verticali corti;
  - **di lato, all'esterno:** un puntone verticale dal ginocchio alla caviglia, con un cuscinetto arrotondato in alto (due tacche), un anello vicino al fondo e una staffa alla caviglia;
  - **dietro:** tallone in tre blocchi, uno centrale più alto e due laterali che si allargano verso il basso (a gradini); polpaccio con quattro feritoie orizzontali nella metà alta; nella metà bassa un incavo scuro ad arco con dentro un mozzo e un blocchetto;
  - ripartizione: struttura del piede, caviglia, puntone e meccanica dell'incavo nello scheletro; scudo dello stinco, piastra del polpaccio e un guscio sottile sul piede nei pezzi delle gambe, così la tuta armata ha il piede color bronzo come nel riferimento e il telaio nudo ha lo stesso piede in acciaio;
  - tutto il resto della tuta invariato; stessi nomi e perni delle ossa, nessuna scatola ruotata (curve e lati inclinati a gradini), limiti invariati, bordo interno di ogni piede ad almeno 0,25 px dal piano di mezzo, piedi appoggiati a y 0, gambe speculari, quattro stati dei pezzi, texel del telaio indipendenti dai pezzi, `swing.py` senza contatti più precoci di oggi.
  - anteprime in `docs/arte/anteprime/rottame-v6/` e, fuori dal repo, un confronto affiancato con i ritagli del riferimento (il riferimento non entra nel repository).

> **Revisione 7 (13 novembre 2026, seconda passata).** La prima passata aveva tutti gli elementi dell'elenco, ma affiancata al riferimento non era la stessa gamba: le feritoie occupavano metà dello scudo, le tacche erano 1,5 px troppo in basso, il piede era troppo largo e senza la porta sul davanti, puntone e tallone restavano d'acciaio, la piastra del polpaccio era 2 px troppo alta. La seconda passata parte quindi da misure: il riferimento ricampionato nei pixel del modello con una griglia (fuori dal repo), allargato di 1,3 in orizzontale perché la nostra tuta è più tozza (scudo dello stinco largo 4,5 px su 8,5 di altezza). Rispetto all'elenco sopra cambia questo:
> - **posizioni, davanti:** piastra alta dello stinco tra y 6 e 8,5, a esagono più stretto in alto, con quattro feritoie tra y 6,75 e 7,55 e una tacca sotto; sotto la piastra una fascia a U con **quattro** tacche verticali (non tre) a y 4,8..5,2; metà bassa dello scudo liscia, fondo arrotondato a y 2,25. Piede largo 3 px in basso e 2,5 in alto, con il lato esterno inclinato e quello interno dritto; sul davanti una porta in rilievo (montanti e architrave) con dentro un incavo nero. Fascia della caviglia in cinque segmenti.
> - **posizioni, dietro:** piastra del polpaccio fino a y 6,25 con gli angoli tagliati, quattro feritoie tra y 4,55 e 5,6, arco da y 4,25 in giù con dentro barra, blocco e due montanti su fondo nero; sopra la piastra la fossa del ginocchio, scura, con tre steli; tallone con il blocco centrale e due cunei.
> - **righe sottili:** feritoie e tacche del riferimento sono spesse circa 0,1 px, un texel ne vale 0,5 e la griglia delle scatole 0,25. Il generatore prende un'opzione per scatola che dà alle sue facce 8 texel per pixel; la pittura di base resta quella a 2 texel per pixel (ogni texel ripetuto 4 × 4) e le righe si disegnano sopra, spesse un texel fine. Solo sulle scatole che portano righe.
> - **colore:** nella tuta armata il puntone (cuscinetto, manicotto, anello), i blocchi del tallone e il piede hanno il colore dell'armatura, con scatole dei pezzi delle gambe; il telaio nudo tiene lo scheletro d'acciaio.
> - **Files:** anche `tools/art/exo.py` (opzione delle righe sottili) e, se serve, `tools/art/preview/` (vista ortografica per il confronto alla stessa scala).
> - **confronto:** fuori dal repo, vista ortografica di una gamba alla stessa scala e con la stessa griglia del riferimento, davanti e dietro.

- [ ] **Step 1: disegnare** la parte bassa delle gambe guardando i due ritagli; `python3 tools/art/build.py` e `--designs frame` senza segnalazioni; `python3 tools/art/swing.py --all` confrontato con lo stato di partenza.
- [ ] **Step 2: confrontare** affiancando riferimento e anteprima, davanti e dietro, alla stessa grandezza: ogni elemento dell'elenco deve riconoscersi; correggere finché regge anche alla grandezza di uno screenshot di gioco.
- [ ] **Step 3: verificare** — due build identiche; `OK art`; `ExoframeGeometryTest` su `1.21.1-neoforge` e `1.20.1-forge`; build delle quattro varianti; `OK jars`, `OK sources`; nel client vero, una variante per versione del gioco: tuta armata e telaio nudo, davanti e dietro.
- [ ] **Step 4:** documenti; commit `art: parte bassa delle gambe come nel riferimento (decisione di Luigi)`.

