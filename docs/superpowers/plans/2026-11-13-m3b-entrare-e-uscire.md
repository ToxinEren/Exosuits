# Milestone 3b — Entrare e uscire: piano di implementazione

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** in gioco si piazza un telaio, gli si montano a mano pezzi e cella, ci si entra, se ne esce con `V`, alla morte resta nel mondo con tutto dentro, gli slot dell'armatura sono bloccati e chi è nella tuta ha l'ingombro e la visuale veri di una tuta alta tre blocchi.

**Architecture:** il contenuto di ogni telaio vive in un registro unico nel dato salvato del server (indossati per giocatore, piazzati per `frameId`); l'entità nel mondo è solo una vista con lo stesso UUID e si riallinea al registro. Entrare e uscire spostano un record fra le due mappe. L'ingombro reale viene da un mixin comune su `Player` (una classe per versione del gioco) che chiama codice comune; il resto passa da eventi Architectury.

**Tech Stack:** Java 17/21, Stonecutter 0.9.8, Architectury Loom e API (eventi, registri, `KeyMappingRegistry`, `EntityRendererRegistry`), GeckoLib 4, GameTest di Minecraft, Python per il generatore grafico, lo strumento `tools/client` per il client senza schermo.

**Spec:** `docs/superpowers/specs/2026-11-13-m3-telaio-design.md` (§3, §4.2, §4.3, §4.5, §4.8, §5.3, §5.4, §5.5, §5.9, §7, §8, §10). Ricognizioni: `docs/superpowers/specs/ricognizioni/2026-11-13-m3b-grandezza-reale.md` con la patch accanto (esperimento riuscito sulle quattro varianti), `docs/compat.md`.

## Global Constraints

- Condizioni Stonecutter solo in `compat/impl/` (comprese le sottocartelle), `platform/` e `compat/Ids.java`; il resto chiama le interfacce di `compat/`. Le interfacce in `compat/` non hanno condizioni. `tools/check_sources.sh` deve stampare `OK sources` (otto regole; la 4 vieta `compat.impl.` fuori da `compat/`, la 5 vuole `Compat.frameState()` nel file che registra un pacchetto C2S, con registrazione e gestore nello stesso file).
- `core/` è Java 17 puro: nessun import di Minecraft, loader, Architectury, Gson; ogni tipo di `core/` è citato in almeno un test.
- Il codice solo client vive in `client/`, `compat/client/`, `compat/impl/client/`, `mixin/client/`; nessun'altra classe lo referenzia, e gli import `net.minecraft.client.*` e `com.mojang.blaze3d.*` stanno solo lì. Un server dedicato non deve caricarli.
- Mixin ammessi, e solo questi: `mixin/client/PlayerRendererMixin`, `mixin/client/ItemInHandLayerMixin`, `mixin/client/LivingEntityCullingMixin` (sezione `client`), `mixin/PlayerSize120Mixin` (sezione `mixins` della sola configurazione 1.20.1), `mixin/PlayerSize121Mixin` (sezione `mixins` della sola configurazione 1.21.1). I mixin non hanno logica: una chiamata a codice comune per aggancio.
- Misure della tuta (design §4.8), uguali su server e client, mai nella config: scala verticale S = 1,65; larghezza dell'ingombro 0,8; in piedi 0,8 × 2,97 con occhi a 2,67; accovacciata 0,8 × 2,475 con occhi a 2,10; nuoto, carponi, elitre, tridente 0,8 × 0,99 con occhi a 0,66; sonno e morte invariati. Indossa la tuta, per l'ingombro, chi ha il segnaposto del torso nello slot del petto.
- Anti-obiettivo che ogni task rispetta: nessuna perdita e nessun doppione di pezzi, celle, armatura civile o telai per morte, uscita, crash, riavvio, inventario pieno. Ciò che non entra nell'inventario cade a terra; un `frameId` sta in una sola mappa del registro.
- Chiavi NBT nuove sotto la radice del dato salvato: `worn`, `placed`, `id`, `state`, `dim`, `pos`, `yaw`, `slot`, `item`, `data_version`; `stowed` esiste già in `NbtKeys`. Schema del dato salvato: 2.
- Opzioni di config usate (valori predefiniti: la lettura da file arriva con la 3c): `server.ownerOnlyFrames` (vero), `frame.exitHoldSeconds` (1), `energy.cellCapacity` (10.000).
- Test in mondo: solo in `gametest/ExoframeGameTests.java`, ognuno aggiunto con una riga che comincia con `add(tests,`; la stessa lista su tutte e quattro le varianti (un corpo può cambiare ramo secondo la versione, la lista no); entro 100 tick; `bash tools/run_gametests.sh` deve dare lo stesso numero su tutte. Tutti i test condividono un server e un dato salvato: ogni test usa un giocatore finto e `frameId` propri e non conta su uno stato pulito. Su 1.20.1 il giocatore finto è in creativa.
- Identificatori e messaggi d'eccezione e di log in inglese; Javadoc e commenti in italiano con apostrofi ASCII; testi di gioco in `en_us` e `it_it`; documenti in italiano.
- Nessun asset di Mjolnir; nessuna forma riconoscibile di Fallout, Iron Man o Halo oltre l'eccezione già scritta nel design per le gambe sotto il ginocchio; nessun dato personale nei jar; `tools/check_jars.sh` deve stampare `OK jars`, `tools/check_art.sh` `OK art`.
- Ogni task termina con: build delle quattro varianti (`./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build`), `bash tools/check_sources.sh`, `bash tools/run_gametests.sh` se il task tocca codice del mod, commit sul branch del piano (`m3b-entrare-uscire`). Il push lo fa chi coordina dopo la revisione. Commit firmati come fa l'ambiente: niente `-c commit.gpgsign=false`, niente identità impostate a mano.

## Review Focus

1. **Crash fra il salvataggio del registro e quello dei chunk.** Un'entità rimasta su disco senza record piazzato si elimina al primo tick; un record piazzato senza entità in un chunk caricato la riottiene entro pochi secondi; mai due telai con lo stesso contenuto. Test: Task 7 (`entityWithoutRecordDiscardsItself`, `missingEntityIsRecreated`, `realignLeavesUnloadedChunksAlone`).
2. **Due giocatori sullo stesso telaio nello stesso tick.** Il secondo ingresso fallisce senza toccare nulla e senza duplicare armatura o record. Test: Task 10 (`secondEnterOnSameFrameFails`).
3. **Inventario pieno quando torna qualcosa** (armatura stivata, pezzo smontato, cella tolta, telaio raccolto). Tutto ciò che non entra cade a terra accanto al giocatore; nulla sparisce. Test: Task 9 (`unmountWithFullInventoryDropsThePiece`), Task 11 (`exitWithFullInventoryDropsArmor`).
4. **Uscita dal gioco e rientro, rinascita, cambio di dimensione mentre si è nel telaio.** Lo stato resta, i segnaposto tornano al loro posto, l'ingombro è quello della tuta; segnaposto senza stato spariscono. Test: Task 13 (`rejoinRestoresPlaceholders`, `placeholdersWithoutStateAreRemoved`).
5. **Spazio che non basta.** Piazzare o entrare dove la tuta in piedi non ci sta è rifiutato con un messaggio; se l'ingombro cresce comunque in uno spazio basso (comando di debug, blocchi messi dopo) il giocatore finisce a carponi e non soffoca. Test: Task 2 (`suitCrawlsUnderLowCeiling`), Task 7 (`placingRefusedWhereNoRoom`), Task 10 (`enterRefusedUnderLowCeiling`).

---

### Task 1: Ripristino fidato dello stato

**Files:**
- Modify: `core/frame/FrameState.java`, `compat/impl/FrameStateCodec.java`, `compat/impl/FrameStateClientCache.java`, `docs/compat.md` (limite "Ripristino dei moduli e config di chi legge")
- Test: `src/test/java/.../core/frame/FrameStateTest.java`, `src/test/java/.../compat/impl/FrameStateCodecTest.java`

**Interfaces:**
- Consumes: `FrameState.installModule(PieceSlot, ModuleType)` (valida contro la config), `FrameStateCodec.read(CompoundTag, ExoframesConfig)`.
- Produces:
  - `void FrameState.restoreModules(PieceSlot slot, List<ModuleType> modules)`: sostituisce i moduli dello slot senza i controlli dell'installatore; lancia `IllegalStateException` se nello slot non c'è un pezzo; rifiuta `null` e doppioni con `IllegalArgumentException`.
  - `static FrameState FrameStateCodec.readTrusted(CompoundTag root, ExoframesConfig cfg)`: come `read`, ma i moduli passano da `restoreModules` e nessuno viene scartato. `read` resta com'è e resta quello usato al caricamento sul server.
  - `FrameStateClientCache.accept` usa `readTrusted`.

- [ ] **Step 1: test** — in `FrameStateTest`: `restoreModulesSkipsInstallerChecks` (un modulo che `installModule` rifiuta per livello troppo basso resta dopo `restoreModules`), `restoreModulesNeedsAPiece`, `restoreModulesRejectsDuplicates`. In `FrameStateCodecTest`: `trustedReadKeepsModulesTheReaderConfigWouldRefuse` (scrivi con una config che dà due slot al torso, leggi con una che ne dà zero: `readTrusted` li tiene, `read` li scarta).
- [ ] **Step 2: Run** `./gradlew --console=plain :1.21.1-neoforge:test --tests '*FrameStateTest' --tests '*FrameStateCodecTest'` — Expected: FAIL (metodi mancanti).
- [ ] **Step 3: implementare**.
- [ ] **Step 4: Run** gli stessi test — PASS; poi le verifiche di fine task.
- [ ] **Step 5:** commit `core: ripristino fidato dei moduli e lettura fidata dello stato`.

### Task 2: Grandezza reale

**Files:**
- Create: `frame/SuitSize.java`, `mixin/PlayerSize120Mixin.java`, `mixin/PlayerSize121Mixin.java`, `mixin/client/LivingEntityCullingMixin.java`
- Modify: `RedstoneExoframes.java` (registrazione del tick), `client/SuitScale.java`, `client/SuitLayer.java`, `client/PilotPose.java` (solo commenti sulla targhetta), `frame/DebugWear.java`, `src/main/resources-mc1.20.1/redstone_exoframes.mixins.json`, `src/main/resources-mc1.21.1/redstone_exoframes.mixins.json`, `tools/check_jars.sh` (e2), `gametest/ExoframeGameTests.java`, `docs/compat.md`
- Test: `src/test/java/.../client/SuitScaleTest.java`

**Interfaces:**
- Consumes: `ModItems.EXOFRAME_TORSO`, `DebugWear.wear/remove`, `SuitLayer.wearsSuit`, la patch della ricognizione (`ricognizioni/2026-11-13-m3b-grandezza-reale.patch`: è un esperimento da cui partire, non codice da copiare alla lettera; i log `SPIKE` non entrano).
- Produces (`frame/SuitSize`, codice comune, senza direttive):
  - `public static final float HEIGHT_SCALE = 1.65f`, `WIDTH = 0.8f`, `CULLING_MARGIN = 0.5f` (allargamento orizzontale del riquadro di culling);
  - `static boolean wearsSuit(Player player)`: segnaposto del torso nello slot del petto; falso se `player.getInventory()` è `null` (il costruttore di `Entity` chiama `getDimensions` prima che l'inventario esista);
  - `static EntityDimensions dimensions(Player player, Pose pose, EntityDimensions vanilla)`: `vanilla.scale(WIDTH / 0.6f, HEIGHT_SCALE)` per chi indossa, altrimenti `vanilla` (le dimensioni fisse di sonno e morte restano tali);
  - `static float eyeHeight(Player player, Pose pose, float vanilla)` (solo 1.20.1);
  - `static void tick(Player player)`: confronta "indossa" con l'ultimo valore visto in una mappa a chiavi deboli per identità (`new MapMaker().weakKeys().makeMap()`, non `WeakHashMap`) e chiama `refreshDimensions()` quando cambia; registrato con `TickEvent.PLAYER_PRE` in `RedstoneExoframes.init()`;
  - `static AABB standingBox(Vec3 feet)`: il riquadro 0,8 × 2,97 con i piedi in `feet`;
  - `static AABB cullingBox(LivingEntity entity, AABB vanilla)`: `vanilla.inflate(CULLING_MARGIN, 0, CULLING_MARGIN)` per un `Player` che indossa la tuta.
  - `client/SuitScale.S = SuitSize.HEIGHT_SCALE` (una sola costante; `SuitScaleTest` continua a legarla al generatore).
  - `DebugWear.wear` e `remove` chiamano `player.refreshDimensions()` dopo aver cambiato i segnaposto.
- Mixin: `PlayerSize120Mixin` aggancia il ritorno di `Player.getDimensions(Pose)` e di `Player.getStandingEyeHeight(Pose, EntityDimensions)`; `PlayerSize121Mixin` il ritorno di `Player.getDefaultDimensions(Pose)`; descrittori completi nelle annotazioni, come nella ricognizione §4. `LivingEntityCullingMixin` aggancia il ritorno di `LivingEntity.getBoundingBoxForCulling()` e chiama `SuitSize.cullingBox`.
- `tools/check_jars.sh` (e2): la sezione `mixins` è ammessa e deve contenere esattamente la classe della versione del jar; `server` resta vuota; il controllo dei nomi riconosce anche i nuovi bersagli (Mojang su NeoForge; `m_6972_`/`m_6431_` su Forge; `method_18377`/`method_18394` su Fabric 1.20.1; `method_55694` su Fabric 1.21.1) e il mixin del culling.

- [ ] **Step 1: test in mondo** — `suitGrowsBoxAndEyes` (senza tuta 0,6 × 1,8 e occhi 1,62; dopo `DebugWear.wear` 0,8 × 2,97, occhi 2,673, occhi da accovacciato 1,27·1,65, riquadro ricostruito; dopo `remove` di nuovo vanilla), `suitCrawlsUnderLowCeiling` (soffitto a 2: dopo due tick posa `SWIMMING` e riquadro alto 0,99). JUnit: `SuitScaleTest.scaleIsTheCommonConstant`.
- [ ] **Step 2: Run** `bash tools/run_gametests.sh 1.21.1-neoforge` — Expected: FAIL.
- [ ] **Step 3: implementare** `SuitSize`, i tre mixin, le due configurazioni, il tick; `SuitLayer.wearsSuit` usa `SuitSize.wearsSuit` più il riassunto leggibile.
- [ ] **Step 4: Run** `bash tools/run_gametests.sh` (quattro varianti) — PASS; build delle quattro varianti con `collectJar`; `bash tools/check_jars.sh` prima fallisce sul controllo vecchio e, aggiornato, stampa `OK jars` (mostrare che rifiuta una classe in più nella sezione `mixins`).
- [ ] **Step 5: client vero** — `EXO_ACCEPT_EULA=true bash tools/client/client_shot.sh <variante> <cartella> "execute as Exo run exoframe debug wear rottame"` su `1.21.1-neoforge` e `1.20.1-fabric` (le due non provate dalla ricognizione): uscita 0, `log-check.txt` vuoto, nessun "moved wrongly" nel log del server; guardare gli scatti.
- [ ] **Step 6:** `docs/compat.md` (sezione dei mixin: non più "solo client"; avviso non bloccante di `remapJar` per la classe dell'altra versione); commit `frame: grandezza reale della tuta (ingombro, occhi, culling)`.

### Task 3: Dato salvato, schema 2

**Files:**
- Modify: `compat/impl/FrameStateSavedData.java`, `compat/NbtKeys.java`
- Create: `compat/impl/FrameRecord.java`, `compat/impl/PlacedSpot.java`
- Test: `src/test/java/.../compat/impl/FrameStateSavedDataTest.java`

**Interfaces:**
- Consumes: `FrameStateCodec.write/read`, il meccanismo esistente delle voci non lette (`unread`) e del file in sola lettura per schemi più recenti.
- Produces (tutto package-private in `compat/impl`, senza `ItemStack`: l'armatura stivata qui è una lista di `CompoundTag` opachi):
  - `final class FrameRecord { UUID frameId(); FrameState state(); List<CompoundTag> stowed(); }` (lista modificabile; `stowed` mai `null`);
  - `record PlacedSpot(String dimension, double x, double y, double z, float yaw)`;
  - `FrameStateSavedData`: `CURRENT_SCHEMA = 2`; `FrameRecord worn(UUID player)`; `FrameRecord getOrCreateWorn(UUID player, Supplier<FrameState> factory)` (identità nuova); `boolean removeWorn(UUID player)`; `UUID place(FrameRecord record, PlacedSpot spot)`; `FrameRecord placed(UUID frameId)`; `PlacedSpot spot(UUID frameId)`; `List<UUID> placedIn(String dimension)`; `boolean removePlaced(UUID frameId)`; `boolean enter(UUID player, UUID frameId)` (da piazzato a indossato; falso se il record non c'è o il giocatore ne indossa già uno); `Optional<UUID> exit(UUID player, PlacedSpot spot)` (l'inverso); `frames()` resta e dà gli stati indossati.
  - File: `schema: 2`; `worn: { "<uuid giocatore>": { id: int[4], state: {…}, stowed: [ {slot, data_version, item} … ] } }`; `placed: { "<frameId>": { state, stowed, dim, pos: [x, y, z] (double), yaw } }`. Un file con schema 1 (`frames` per giocatore) si legge: ogni stato diventa un record indossato con identità nuova. Uno stesso `frameId` in due voci: la seconda resta fra le non lette con un avviso.

- [ ] **Step 1: test** — `schema2RoundTripsWornAndPlaced`, `enterMovesTheSameRecord` (stesso oggetto `FrameRecord`, sparito da `placed`), `enterFailsWhenPlayerAlreadyWearsOne`, `exitMovesBackWithTheSpot`, `placedInFiltersByDimension`, `schema1FileBecomesWornRecords`, `duplicateFrameIdStaysUnread`, `stowedTagsSurviveARoundTrip`, `newerSchemaIsStillReadOnly` (schema 3).
- [ ] **Step 2: Run** `./gradlew --console=plain :1.21.1-neoforge:test --tests '*FrameStateSavedDataTest'` — Expected: FAIL.
- [ ] **Step 3: implementare**; `FrameStateAccessImpl` continua a compilare usando `worn`/`getOrCreateWorn`.
- [ ] **Step 4: Run** i test e le verifiche di fine task — PASS.
- [ ] **Step 5:** commit `compat: registro dei telai nel dato salvato (schema 2)`.

### Task 4: Operazioni di registro, armatura stivata, riassunto come tag

**Files:**
- Modify: `compat/FrameStateAccess.java`, `compat/impl/FrameStateAccessImpl.java`, `compat/impl/SuitSummaryTags.java` (visibilità invariata), `gametest/ExoframeGameTests.java`, `docs/compat.md`
- Create: `compat/PlacedFrame.java`, `compat/StowedStack.java`, `compat/impl/StackTags.java`
- Test: `src/test/java/.../compat/impl/SuitSummaryTagsTest.java`

**Interfaces:**
- Consumes: Task 3.
- Produces (in `FrameStateAccess`; solo thread del server; con il dato in sola lettura le scritture non fanno nulla e danno falso o vuoto):
  - `record PlacedFrame(UUID frameId, FrameState state, ResourceKey<Level> dimension, Vec3 position, float yaw, boolean hasStowed)`; `record StowedStack(EquipmentSlot slot, ItemStack stack)`;
  - `UUID place(ServerLevel level, FrameState state, Vec3 position, float yaw)`;
  - `Optional<PlacedFrame> placed(MinecraftServer server, UUID frameId)`; `List<PlacedFrame> placedIn(ServerLevel level)`;
  - `boolean updatePlaced(MinecraftServer server, UUID frameId, Consumer<FrameState> change)`;
  - `boolean removePlaced(MinecraftServer server, UUID frameId)`;
  - `boolean enter(ServerPlayer player, UUID frameId)` (sposta il record e sincronizza una volta il proprietario); `Optional<UUID> exit(ServerPlayer player, ServerLevel level, Vec3 position, float yaw)` (il record va fra i piazzati della dimensione data, che può non essere quella del giocatore; manda "assente" al giocatore); `Optional<UUID> wornFrameId(ServerPlayer player)`;
  - `void stow(ServerPlayer player, List<StowedStack> armor)` (aggiunge al record indossato); `List<StowedStack> takeStowed(ServerPlayer player)`; `List<StowedStack> takeStowedOfPlaced(MinecraftServer server, UUID frameId)` (le due "take" svuotano la lista);
  - `CompoundTag writeSummaryTag(SuitSummary summary)`; `Optional<SuitSummary> readSummaryTag(CompoundTag tag)`.
  - `compat/impl/StackTags` (con le direttive): `CompoundTag write(HolderLookup.Provider registries, EquipmentSlot slot, ItemStack stack)` (aggiunge `data_version` = versione dati corrente) e `Optional<StowedStack> read(HolderLookup.Provider registries, CompoundTag tag)`; su 1.21.1 una voce con `data_version` più vecchia passa dal correttore di dati vanilla (`References.ITEM_STACK`) prima della lettura; una voce illeggibile resta nel record com'è e si segnala una volta.

- [ ] **Step 1: test** — JUnit `SuitSummaryTagsTest.secondWriteReplacesTheFirst` (scrivi un riassunto con sei pezzi, poi uno con due: restano due). In mondo: `registryPlaceEnterExitMovesOneRecord` (stesso `frameId` prima piazzato, poi indossato, poi piazzato altrove; mai in due posti), `stowedArmorSurvivesEnterAndExit` (elmo di ferro danneggiato stivato e ripreso identico), `stowedStackFromOlderVersionIsRead` (su 1.21.1: voce scritta a mano nel formato 1.20.1, `{id: "minecraft:iron_helmet", Count: 1b, tag: {Damage: 5}}` con `data_version` 3465, letta come elmo di ferro con danno 5; su 1.20.1 lo stesso test fa un giro completo nel formato corrente), `summaryTagRoundTrips`.
- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: implementare**.
- [ ] **Step 4: Run** JUnit e `bash tools/run_gametests.sh` — PASS sulle quattro varianti.
- [ ] **Step 5:** `docs/compat.md` (tabella dell'interfaccia, riga degli stack rinviata che ora è chiusa); commit `compat: operazioni di registro, armatura stivata per versione, riassunto come tag`.

### Task 5: Pezzi Rottame e cella con carica come oggetti

**Files:**
- Create: `content/PieceItem.java`, `content/PieceKind.java`, `content/CellItem.java`, modelli in `src/main/resources/assets/redstone_exoframes/models/item/`, icone generate
- Modify: `content/ModItems.java`, `content/ModCreativeTabs.java`, `tools/gen_placeholders.py`, `lang/en_us.json`, `lang/it_it.json`, `tools/check_jars.sh` se elenca gli oggetti, `gametest/ExoframeGameTests.java`

**Interfaces:**
- Consumes: `ItemDataAccess.piece/setPiece(ItemStack, PieceData)`, `cellEnergy/setCellEnergy`, `Piece.fresh(PieceSlot, Tier, ExoframesConfig.Tiers)`, `RedstoneExoframes.config()`.
- Produces:
  - `enum PieceKind { HELMET, TORSO, ARM, LEG }` con `List<PieceSlot> slots()` (ARM: braccio sinistro e destro; LEG: gamba sinistra e destra);
  - `PieceItem(Tier tier, PieceKind kind, Item.Properties)`: `tier()`, `kind()`, `Piece toPiece(ItemStack stack, PieceSlot slot)` (dai dati dello stack, o un pezzo nuovo del livello se lo stack non ha dati), `static ItemStack fromPiece(Piece piece, List<ModuleType> modules, Appearance appearance)` (lo stack dell'oggetto giusto con i dati del pezzo);
  - `CellItem`: `EnergyCell toCell(ItemStack stack)` (capacità dalla config, carica dai dati, 0 se mancano), `static ItemStack fromCell(EnergyCell cell)`, `static ItemStack charged()`;
  - `ModItems.SCRAP_HELMET`, `SCRAP_TORSO`, `SCRAP_ARM`, `SCRAP_LEG` (id `scrap_helmet`, …), `REDSTONE_CELL` diventa un `CellItem`; pila a 1; nella scheda creativa: i quattro pezzi e la cella carica.
  - Icone 16 × 16 provvisorie generate da `tools/gen_placeholders.py` (sagome in bronzo e acciaio), senza metadati; ricette no (3c).

- [ ] **Step 1: test in mondo** — `pieceItemsKnowTheirKind` (i quattro oggetti registrati, `kind` e `tier` giusti), `pieceItemRoundTripsAPiece` (pezzo danneggiato → stack → pezzo con la stessa durabilità), `cellItemCarriesItsCharge` (cella da 4.000 → stack → cella da 4.000; `charged()` ha la capacità della config).
- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: implementare**; rigenerare le icone.
- [ ] **Step 4: Run** `bash tools/run_gametests.sh`, build, `OK jars` — PASS.
- [ ] **Step 5:** commit `content: pezzi Rottame e cella con carica come oggetti`.

### Task 6: `CompatEntity` ed entità telaio

**Files:**
- Create: `compat/CompatEntity.java`, `compat/impl/EntityBase.java`, `entity/ExoframeEntity.java`, `content/ModEntities.java`
- Modify: `RedstoneExoframes.java`, `lang/*.json`, `gametest/ExoframeGameTests.java`, `docs/compat.md`

**Interfaces:**
- Consumes: Task 2 (`SuitSize`), Task 4 (`placed`, `writeSummaryTag`, `readSummaryTag`).
- Produces:
  - `compat/impl/EntityBase extends Entity` (con le direttive): chiude `defineSynchedData()` contro `defineSynchedData(SynchedEntityData.Builder)` e il pacchetto di spawn di 1.20.1; chiede alle sottoclassi `protected abstract void defineData(DataDefiner definer)` con `interface DataDefiner { <T> void define(EntityDataAccessor<T> key, T initial); }`.
  - `compat/CompatEntity extends EntityBase`: classe astratta pubblica senza direttive, il tipo che il resto del codice estende.
  - `ModEntities.EXOFRAME` (`RegistrySupplier<EntityType<ExoframeEntity>>`, id `exoframe`): categoria `MISC`, dimensioni `SuitSize.WIDTH` × 2,97, immune al fuoco, raggio di tracciamento 10; `ModEntities.register()` chiamato in `init()` prima degli oggetti che la usano.
  - `ExoframeEntity extends CompatEntity`: `UUID frameId()` (= `getUUID()`); `SuitSummary summary()`; `void setSummary(SuitSummary)` (dato sincronizzato `CompoundTag`); nessun dato proprio nell'NBT oltre a quello vanilla; senza gravità e senza movimento; cliccabile (`isPickable`), non spingibile; `hurt` restituisce sempre falso e le esplosioni la ignorano; `getBoundingBoxForCulling()` allargato di `SuitSize.CULLING_MARGIN + 0,5` in orizzontale; al primo tick sul server, se `Compat.frameState().placed(server, frameId())` è vuoto si elimina, e se il record è altrove si sposta lì.

- [ ] **Step 1: test in mondo** — `frameEntityKeepsItsIdentityAndSummary` (record piazzato + entità con quell'UUID: dopo un tick c'è ancora; il riassunto impostato si rilegge uguale), `frameEntityIgnoresDamageAndExplosions` (`hurt` falso con danno generico; dopo `level.explode` accanto l'entità è viva), `frameEntityHasTheSuitBox` (0,8 × 2,97).
- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: implementare**.
- [ ] **Step 4: Run** `bash tools/run_gametests.sh`, build, `OK sources` — PASS.
- [ ] **Step 5:** `docs/compat.md` (`CompatEntity` non più rinviata); commit `entity: CompatEntity e entita' telaio`.

### Task 7: Piazzamento e riallineamento

**Files:**
- Create: `content/ExoframeItem.java`, `frame/FramePlacement.java`, `frame/FrameRealign.java`
- Modify: `content/ModItems.java`, `content/ModCreativeTabs.java`, `command/ExoframeCommand.java` (sottocomando di debug), `RedstoneExoframes.java`, modello e icona dell'oggetto, `lang/*.json`, `gametest/ExoframeGameTests.java`

**Interfaces:**
- Consumes: Task 4, Task 6, `SuitSize.standingBox`.
- Produces:
  - `FramePlacement`: `static Optional<ExoframeEntity> place(ServerLevel level, FrameState state, Vec3 feet, float yaw)` (rifiuta, vuoto, se `!level.noCollision(SuitSize.standingBox(feet))`; crea il record con `Compat.frameState().place`, poi l'entità con quell'UUID e il riassunto dello stato); `static Optional<Vec3> groundBelow(ServerLevel level, Vec3 from)` (primo appoggio sotto il punto, cercando fino al limite inferiore del mondo; vuoto se non c'è); `static ExoframeEntity spawnView(ServerLevel level, PlacedFrame frame)` (solo l'entità di un record che esiste già).
  - `ExoframeItem.useOn`: sul server piazza un telaio vuoto con il giocatore come proprietario sul blocco cliccato (`Vec3.atBottomCenterOf(pos.relative(face))`), rivolto verso il giocatore; consuma l'oggetto se non in creativa; messaggio `message.redstone_exoframes.no_room` se non c'è posto. `ModItems.EXOFRAME` (id `exoframe`), in scheda creativa.
  - `FrameRealign.register()`: su `TickEvent.SERVER_LEVEL_POST`, ogni 100 tick per livello, per ogni record di `placedIn(level)` il cui chunk ha le entità caricate (`level.areEntitiesLoaded`) e senza entità con quell'UUID: alla seconda verifica consecutiva la ricrea con `spawnView`. `static int checkNow(ServerLevel level)` esegue una verifica subito e dice quante entità ha ricreato (per i test).
  - `/exoframe debug place nudo|rottame` (livello 2): piazza un telaio ai piedi di chi esegue, vuoto o con sei pezzi Rottame e una cella carica.

- [ ] **Step 1: test in mondo** — `placingCreatesRecordAndEntity`, `placingRefusedWhereNoRoom` (soffitto a 2 sopra il punto: nessun record, nessuna entità), `entityWithoutRecordDiscardsItself` (entità creata a mano con un UUID senza record: sparisce al primo tick), `missingEntityIsRecreated` (entità eliminata con `discard()`: dopo due `checkNow` c'è di nuovo, una sola, nello stesso punto), `realignLeavesUnloadedChunksAlone` (record piazzato a 10.000 blocchi: `checkNow` non crea nulla), `groundBelowFindsTheFloor`.
- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: implementare**.
- [ ] **Step 4: Run** `bash tools/run_gametests.sh`, build, `OK sources`, `OK jars` — PASS.
- [ ] **Step 5:** commit `frame: piazzamento del telaio e riallineamento fra registro ed entita'`.

### Task 8: Disegno del telaio piazzato

**Files:**
- Create: `client/ExoframeEntityRenderer.java`, `client/SuitDraw.java`, `compat/client/SuitBoneRules.java`
- Modify: `compat/client/SuitRenderer.java`, `compat/impl/client/SuitRendererImpl.java`, `client/SuitLayer.java`, `client/RedstoneExoframesClient.java`, `docs/compat.md`
- Test: `src/test/java/.../compat/client/SuitBoneRulesTest.java`

**Interfaces:**
- Consumes: Task 6, Task 7 (`/exoframe debug place`), `SuitView`, `SuitBones`.
- Produces:
  - `SuitRenderer.render(Entity entity, HumanoidModel<?> base, PoseStack pose, MultiBufferSource buffers, int packedLight, float partialTick, SuitView view)` (era `LivingEntity`); il contratto resta: l'entità non è un animabile di GeckoLib.
  - `client/SuitDraw.draw(Entity entity, HumanoidModel<?> base, PoseStack pose, MultiBufferSource buffers, int packedLight, float partialTick, SuitView view, float undoRaisePx)`: il disegno in scala S attorno ai piedi su una `PoseStack` privata con la cattura degli errori, oggi dentro `SuitLayer`; `SuitLayer` lo chiama con il sollevamento del pilota, il renderer dell'entità con 0.
  - `ExoframeEntityRenderer extends EntityRenderer<ExoframeEntity>`: un `HumanoidModel` a riposo mai disegnato, orientato con lo yaw dell'entità, `SuitView` dal riassunto sincronizzato; registrato con `EntityRendererRegistry.register(ModEntities.EXOFRAME, ExoframeEntityRenderer::new)` in `RedstoneExoframesClient.init()`.
  - `compat/client/SuitBoneRules` (Java puro): `static boolean visible(String bone, SuitView view)` e `static Region region(String bone)` con `enum Region { PRIMARY, SECONDARY, NEUTRAL }`: le decisioni che oggi stanno nelle tabelle private di `SuitRendererImpl`, che passa a usarle.

- [ ] **Step 1: test** — `SuitBoneRulesTest`: `frameBonesAreAlwaysVisible`, `onlyTheMountedStateOfAPieceIsVisible` (torso allo stato 2: visibili le ossa `pieceTorso_s2_*`, nascoste `s0`, `s1`, `s3`), `piecesOfEmptySlotsAreHidden`, `decalBonesStayHidden`, `regionsFollowTheBoneSuffix`.
- [ ] **Step 2: Run** `./gradlew --console=plain :1.21.1-neoforge:test --tests '*SuitBoneRulesTest'` — Expected: FAIL.
- [ ] **Step 3: implementare**.
- [ ] **Step 4: client vero** su `1.21.1-fabric` e `1.20.1-forge`: `"execute as Exo run exoframe debug place rottame"`, poi spostare il giocatore di tre blocchi indietro con un `tp` relativo; scatti davanti e dietro: il telaio armato in piedi accanto al giocatore, alto come la colonna di tre blocchi, `log-check.txt` vuoto. Ripetere con `nudo`.
- [ ] **Step 5:** verifiche di fine task; commit `client: disegno del telaio piazzato con lo stesso renderer della tuta`.

### Task 9: Montare e smontare a mano, permessi

**Files:**
- Create: `frame/FrameInteractions.java`, `frame/FramePermissions.java`, `frame/Handouts.java`
- Modify: `entity/ExoframeEntity.java` (`interactAt`), `lang/*.json`, `gametest/ExoframeGameTests.java`

**Interfaces:**
- Consumes: Task 4, 5, 6, 7.
- Produces:
  - `FramePermissions.canUse(ServerPlayer player, FrameState state)`: vero se `!config.server.ownerOnlyFrames`, o il telaio non ha proprietario, o il giocatore è il proprietario, o il telaio è sbloccato, o il giocatore è in creativa con livello di permesso 2.
  - `Handouts.give(ServerPlayer player, ItemStack stack)`: nell'inventario, e ciò che non entra a terra ai piedi del giocatore; mai perso.
  - `FrameInteractions.onInteract(ServerPlayer player, ExoframeEntity frame, Vec3 hit, InteractionHand hand)` → `InteractionResult`; `hit` è relativo ai piedi dell'entità. Regole, in ordine:
    1. senza permesso: messaggio `message.redstone_exoframes.locked`, nient'altro;
    2. `PieceItem` in mano: monta nello slot del suo tipo; per braccia e gambe sul lato cliccato se libero, altrimenti sull'altro; se è tutto occupato messaggio `…slot_taken`; toglie un oggetto dalla mano (non in creativa);
    3. `CellItem` in mano e nessuna cella: la inserisce;
    4. mano vuota, non accovacciato: ingresso (Task 10; fino ad allora nessun effetto);
    5. mano vuota, accovacciato: se c'è armatura stivata la restituisce tutta; altrimenti smonta il pezzo nella zona cliccata (gambe sotto 1,1 blocchi dai piedi, busto e braccia fino a 2,2, testa sopra; nella zona di mezzo un braccio se il clic è a più di 0,25 dal centro verso quel lato, altrimenti il busto) con i suoi moduli e il suo aspetto; se nella zona non c'è nulla toglie la cella; se il telaio è vuoto lo raccoglie come oggetto e cancella record ed entità.
  - Dopo ogni cambio: `updatePlaced` e `frame.setSummary(SuitSummary.of(state))`.
  - Lato: sinistra e destra sono quelle del telaio (yaw dell'entità), non di chi guarda.

- [ ] **Step 1: test in mondo** — `mountPieceAndCellByHand` (elmo, braccio sul lato cliccato, cella: lo stato del record li ha, il riassunto dell'entità anche), `secondArmGoesToTheFreeSide`, `unmountByHeightThenCellThenPickUp` (accovacciato: testa, poi gambe, poi cella, poi il telaio vuoto torna oggetto e il record sparisce), `unmountWithFullInventoryDropsThePiece`, `strangerCannotTouchALockedFrame` (nessun cambio, nessun oggetto dato), `unlockedFrameIsOpenToAll`, `unmountedPieceKeepsDurabilityAndColours`.
- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: implementare**.
- [ ] **Step 4: Run** `bash tools/run_gametests.sh`, build, `OK sources` — PASS.
- [ ] **Step 5:** commit `frame: montare e smontare a mano, permessi del telaio`.

### Task 10: Ingresso

**Files:**
- Create: `frame/FrameEntry.java`
- Modify: `frame/FrameInteractions.java`, `frame/DebugWear.java` (parte comune della vestizione), `lang/*.json`, `gametest/ExoframeGameTests.java`

**Interfaces:**
- Consumes: Task 2, 4, 9; `DebugWear.PLACEHOLDERS`, `ItemDataAccess.setSuitSummary`.
- Produces `FrameEntry.enter(ServerPlayer player, ExoframeEntity frame)` → `boolean`, tutto nello stesso tick e in quest'ordine:
  1. rifiuti con messaggio, senza toccare nulla: senza permesso (`…locked`); nessuna cella con carica (`…no_cell`); il giocatore è già in un telaio (`…already_inside`); `!level.noCollision(frame, SuitSize.standingBox(frame.position()))` (`…no_room`);
  2. l'armatura già stivata nel record va a chi entra se è il proprietario (`Handouts.give`); chi non è il proprietario non entra in un telaio che ne contiene ancora (rifiuto `…holds_armor` al passo 1, senza toccare nulla: nulla cade a terra);
  3. `Compat.frameState().enter(player, frameId)`; se dà falso (un altro è entrato nello stesso tick) ci si ferma senza altri effetti;
  4. l'armatura indossata (non i segnaposto) si stiva nel record con il suo slot; i quattro segnaposto vanno negli slot; il riassunto visivo dello stato va sullo stack del torso;
  5. il giocatore si sposta nel punto e con lo yaw del telaio; l'entità si elimina; `player.refreshDimensions()`.
  - `FrameEntry.dress(ServerPlayer player, SuitSummary summary)`: i passi dei segnaposto e del riassunto, usati anche dal comando di debug.

- [ ] **Step 1: test in mondo** — `enterStowsArmorAndWearsTheFrame` (elmo di ferro stivato nel record, quattro segnaposto, riassunto sul torso uguale allo stato, entità sparita, record indossato, ingombro 0,8 × 2,97), `enterRefusedWithoutAChargedCell`, `enterRefusedUnderLowCeiling`, `secondEnterOnSameFrameFails` (due giocatori, due chiamate nello stesso tick: uno dentro, l'altro com'era, un solo record), `ownerGetsBackArmorLeftInTheFrame`, `strangerEnteringDropsTheOwnersArmor`.
- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: implementare**.
- [ ] **Step 4: Run** `bash tools/run_gametests.sh`, build — PASS.
- [ ] **Step 5:** commit `frame: ingresso nel telaio`.

### Task 11: Uscita

**Files:**
- Create: `client/ExitKey.java`, `frame/FrameExitPacket.java`
- Modify: `frame/FrameEntry.java`, `client/RedstoneExoframesClient.java`, `RedstoneExoframes.java`, `compat/FrameStateAccess.java` (Javadoc sui pacchetti C2S), `lang/*.json`, `gametest/ExoframeGameTests.java`, `docs/compat.md`

**Interfaces:**
- Consumes: Task 10; `Compat.net().registerC2S`/`sendToServer`; `FramePlacement.groundBelow`/`spawnView`.
- Produces:
  - `record FrameExitPacket()` con id `redstone_exoframes:frame_exit` e codec vuoto; `FrameEntry.registerNet()` registra il pacchetto e il suo gestore nello stesso file (regola 5) e viene chiamato in `init()`.
  - `FrameEntry.exit(ServerPlayer player)` → `boolean`: falso se il giocatore non indossa un telaio; toglie i segnaposto (inventario, mano secondaria, cursore); riprende l'armatura stivata e la rimette nei suoi slot, e ciò che trova lo slot occupato va con `Handouts.give`; `Compat.frameState().exit(player, livello, punto, yaw)` con il livello del giocatore e `punto = groundBelow(posizione)` (se non c'è suolo, l'ultima posizione sicura del Task 12; fino ad allora la posizione del giocatore); crea l'entità con `spawnView`; `refreshDimensions()`.
  - `client/ExitKey`: `KeyMapping` `key.redstone_exoframes.exit` (predefinito `V`, categoria `key.categories.redstone_exoframes`) registrato con `KeyMappingRegistry`; su `ClientTickEvent.CLIENT_POST`, mentre il tasto è premuto e il giocatore locale indossa la tuta, conta i tick, mostra l'avanzamento nella barra delle azioni e a `exitHoldSeconds · 20` manda `FrameExitPacket` una volta; rilasciare azzera. `static int holdTicksNeeded(int seconds)` e `static boolean shouldSend(int heldTicks, int needed, boolean alreadySent)` per il test.

- [ ] **Step 1: test** — JUnit `ExitKeyTest`: `sendsOnceWhenTheHoldIsComplete`, `releasingResets`. In mondo: `exitReturnsArmorAndLeavesTheFrame` (elmo di nuovo in testa, segnaposto spariti, record piazzato ai piedi del giocatore, entità presente, ingombro vanilla), `exitWithFullInventoryDropsArmor` (slot della testa occupato e inventario pieno: l'elmo stivato è a terra), `exitInMidAirPutsTheFrameOnTheGround`, `exitWithoutAFrameDoesNothing`.
- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: implementare**.
- [ ] **Step 4: Run** JUnit, `bash tools/run_gametests.sh`, build, `OK sources` — PASS.
- [ ] **Step 5:** `docs/compat.md` (pacchetti C2S: `frame_exit` non più rinviato); commit `frame: uscita dal telaio con il tasto V`.

### Task 12: Morte e ultima posizione sicura

**Files:**
- Create: `frame/SafeSpots.java`
- Modify: `frame/FrameEntry.java`, `RedstoneExoframes.java`, `gametest/ExoframeGameTests.java`

**Interfaces:**
- Consumes: Task 11.
- Produces:
  - `SafeSpots`: `static void tick(ServerPlayer player)` (su `TickEvent.PLAYER_POST`, lato server, ogni 20 tick: se il giocatore indossa un telaio ed è a terra, ricorda dimensione e posizione); `record Spot(ResourceKey<Level> dimension, Vec3 position)`; `static Optional<Spot> last(ServerPlayer player)`; la mappa è in memoria e si svuota all'uscita del giocatore dal server.
  - `FrameEntry.onDeath(ServerPlayer player)`: registrato su `EntityEvent.LIVING_DEATH`; se il giocatore indossa un telaio: toglie i segnaposto prima che il gioco faccia cadere l'inventario; l'armatura stivata resta nel record; `exit` nel punto di morte appoggiato al suolo; se il punto è sotto il limite inferiore del mondo o sotto non c'è suolo, nell'ultima posizione sicura (anche di un'altra dimensione), e in mancanza anche di quella al punto di rinascita del mondo; crea l'entità. Vale anche con `keepInventory`. Non annulla la morte.
  - `FrameEntry.exit` usa `SafeSpots` come ripiego quando non c'è suolo.

- [ ] **Step 1: test in mondo** — `deathLeavesTheFrameWithEverythingInside` (dopo la morte: record piazzato con pezzi, cella e armatura stivata; nessun segnaposto a terra né nell'inventario; entità nel punto), `deathInTheVoidUsesTheLastSafeSpot` (posizione sicura registrata, poi morte sotto il limite inferiore: il telaio è alla posizione sicura), `deathWithKeepInventoryStillLeavesTheFrame`.
- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: implementare**.
- [ ] **Step 4: Run** `bash tools/run_gametests.sh`, build — PASS.
- [ ] **Step 5:** commit `frame: alla morte il telaio resta nel mondo, con l'ultima posizione sicura`.

### Task 13: Slot bloccati, rientro, comando di debug allineato

**Files:**
- Create: `frame/SlotGuard.java`
- Modify: `frame/DebugWear.java`, `command/ExoframeCommand.java`, `RedstoneExoframes.java`, `content/ModItems.java` (commento), `gametest/ExoframeGameTests.java`

**Interfaces:**
- Consumes: Task 10 (`FrameEntry.dress`), `Compat.frameState().get/update/clear`.
- Produces:
  - `SlotGuard.tick(ServerPlayer player)` su `TickEvent.PLAYER_POST` lato server. Con un record indossato: ogni slot dell'armatura ha il suo segnaposto (un oggetto estraneo trovato lì va con `Handouts.give`); il riassunto sul torso è quello dello stato (si riscrive solo se diverso); le copie dei segnaposto in inventario, mano secondaria e cursore si eliminano. Senza record indossato: ogni segnaposto, ovunque, si elimina. `SlotGuard.register()` aggancia anche `PlayerEvent.DROP_ITEM`, che annulla il lancio di un segnaposto.
  - Comando di debug: `wear nudo|rottame …` crea o sostituisce il record indossato del giocatore con `Compat.frameState().update` (vuoto, oppure sei pezzi Rottame nello stato e nei colori chiesti) e veste con `FrameEntry.dress`; `via` fa `Compat.frameState().clear` e toglie i segnaposto. `DebugWear.wear(ServerPlayer, SuitSummary)` resta come firma e fa questo.

- [ ] **Step 1: test in mondo** — `slotGuardRestoresARemovedPlaceholder`, `slotGuardDeletesStrayCopies` (copia nell'inventario e sul cursore), `slotGuardMovesForeignArmorOut` (elmo di ferro messo nello slot: torna nell'inventario, il segnaposto al suo posto), `placeholdersWithoutStateAreRemoved`, `rejoinRestoresPlaceholders` (record indossato e nessun segnaposto, come dopo un rientro: un tick li rimette con il riassunto), `summaryFollowsTheState` (un pezzo si rompe nello stato: al tick dopo il riassunto sul torso lo dice). I cinque test `debugWear*` si adattano al record vero e restano.
- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: implementare**.
- [ ] **Step 4: Run** `bash tools/run_gametests.sh`, build — PASS.
- [ ] **Step 5:** commit `frame: slot dell'armatura bloccati per autoriparazione; il comando di debug crea un telaio vero`.

### Task 14: Comandi del proprietario e degli operatori

**Files:**
- Modify: `command/ExoframeCommand.java`, `lang/*.json`, `gametest/ExoframeGameTests.java`

**Interfaces:**
- Consumes: Task 4, 9.
- Produces: la radice `/exoframe` senza requisito di permesso; `debug` e `remove` richiedono il livello 2; `unlock` e `lock` no.
  - `/exoframe unlock` e `/exoframe lock`: agiscono sul telaio che il giocatore indossa oppure, se non ne indossa, su quello piazzato che sta guardando entro 5 blocchi; solo il proprietario (o un operatore) può; messaggi `commands.redstone_exoframes.unlock.done`, `.lock.done`, `.not_owner`, `.no_frame`.
  - `/exoframe remove`: il telaio piazzato che l'operatore guarda entro 5 blocchi; pezzi, cella e armatura stivata cadono a terra come oggetti, poi record ed entità si cancellano.

- [ ] **Step 1: test in mondo** — `ownerUnlocksAndStrangerCanUse`, `strangerCannotUnlock`, `removeDropsTheContents` (dopo il comando: nessun record, nessuna entità, a terra i pezzi, la cella e l'armatura stivata), `debugNeedsOperator` (senza livello 2 il sottocomando non esiste per quella sorgente).
- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: implementare**.
- [ ] **Step 4: Run** `bash tools/run_gametests.sh`, build — PASS.
- [ ] **Step 5:** commit `command: unlock e lock per il proprietario, remove per gli operatori`.

### Task 15: Generatore grafico, irrobustimenti rimandati dalla 3a

Indipendente dagli altri: si può fare in un clone mentre il checkout principale è occupato.

**Files:**
- Modify: `tools/art/exo.py`, `tools/art/paint.py`, `tools/art/build.py`, `tools/art/preview/shot.js`, `tools/check_art.sh`, `tools/art/README.md`, i file generati se cambiano
- Create: `tools/art/selftest.py`

**Interfaces:**
- Consumes: design §10 ("Primo compito d'arte di 3b").
- Produces:
  - `exo.Model.check`: per due facce complanari conta come copertura solo le scatole che si disegnano sempre insieme a loro (il telaio e i pezzi degli slot delle due facce), mai le decalcomanie.
  - `_buried` decide sulle celle da un quarto di pixel, come `check`.
  - `tools/art/selftest.py`, eseguito da `tools/check_art.sh`: inietta un difetto alla volta in una copia del modello e pretende la segnalazione: striscia complanare di 0,25 px, scatola dentro la testa del pilota, scatola fuori dai limiti, coordinata fuori griglia, linea fuori dalla sua scatola, stato più chiaro del precedente, regione dell'atlante piena, faccia fine diversa dalla sua versione a densità normale.
  - `preview/shot.js` legge la scala da `exo.py` (niente terza copia del numero).
  - Le linee sottili hanno la stessa lunghezza sulla faccia davanti e su quella dietro (oggi una è più lunga di un texel fine).

- [ ] **Step 1:** scrivere `selftest.py` con gli otto difetti; i due che i controlli attuali non vedono (copertura con un pezzo di un altro slot; faccia sepolta con una striscia scoperta su un lato dispari) devono fallire.
- [ ] **Step 2: Run** `python3 tools/art/selftest.py` — Expected: FAIL sui due casi.
- [ ] **Step 3: implementare**; `python3 tools/art/build.py` e `--designs frame` senza segnalazioni; se la texture cambia per le linee, rigenerare i file committati.
- [ ] **Step 4: Run** `python3 tools/art/selftest.py` (otto su otto), due build identiche, `bash tools/check_art.sh` → `OK art`; `ExoframeGeometryTest` e `SuitScaleTest` su `1.21.1-neoforge` e `1.20.1-forge`; se i file generati sono cambiati, uno scatto armato dal client vero su una variante per versione.
- [ ] **Step 5:** commit `art: controlli del generatore piu' severi e con prove negative`.

### Task 16: Verifica nel client vero, checklist per il PC, documenti

**Files:**
- Create: `tools/client/m3b_shots.sh`, `docs/test-in-gioco/m3b-entrare-e-uscire.md`, `docs/test-in-gioco/m3b-screenshot/`
- Modify: `tools/client/client_shot.sh`, `tools/client/README.md`, `docs/compat.md`, `docs/superpowers/specs/2026-11-13-m3-telaio-design.md` (solo dove il fatto contraddice il testo)

**Interfaces:**
- Consumes: tutto il resto.
- Produces:
  - `client_shot.sh` accetta, oltre ai comandi del server, passi del client che cominciano con `@`: `@click destro`, `@click sinistro`, `@tieni <tasto> <secondi>`, `@scatto <nome>` (uno scatto a quel punto), `@attendi <secondi>`; un passo sconosciuto ferma l'esecuzione con errore.
  - `m3b_shots.sh <variante> <cartella>`: la sequenza completa con i clic veri: piazzare il telaio con l'oggetto (clic destro su un blocco), montare elmo e cella (clic destro con l'oggetto in mano), entrare (clic destro a mano vuota), uscire (`V` tenuto), rientrare e morire (`kill`), e uno scatto dopo ogni passo; alla fine controlla con comandi del server che il record sia dove deve (`data get entity` sull'entità telaio).
  - Checklist `docs/test-in-gioco/m3b-entrare-e-uscire.md` per il PC, dai jar di produzione del ramo: ciò che il cloud non prova (un secondo giocatore che vede ingombro e targhetta dell'altro; il blocco degli slot provato a mano, compreso il lampeggio; porte, scale e soffitti bassi; carrelli, barche, letti; fluidità con più telai piazzati; riavvio del server con telai piazzati e indossati) e le domande aperte del design §8 da far vedere a Luigi.
  - `docs/compat.md`: interfacce nuove, limiti chiusi e rimasti, la riga sulla registrazione dei test su Fabric (oggi dice il contrario del codice).

- [ ] **Step 1:** estendere `client_shot.sh` con i passi `@`; provarli da soli su una variante (uno scatto, un clic a vuoto, un tasto tenuto) e l'errore sul passo sconosciuto.
- [ ] **Step 2:** scrivere `m3b_shots.sh` ed eseguirlo sulle quattro varianti, una alla volta: uscita 0, ogni `log-check.txt` vuoto, scatti guardati uno per uno (telaio piazzato, armato, giocatore dentro, telaio rimasto dopo l'uscita, telaio rimasto dopo la morte).
- [ ] **Step 3:** scegliere gli scatti di riferimento (senza metadati), scrivere la checklist e aggiornare i documenti.
- [ ] **Step 4:** verifiche di fine task; commit `test: prova completa nel client vero, checklist della parte 3b`.
