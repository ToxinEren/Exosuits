### Task 6: `/exoframe remove` e la linea di vista in `frame/`

Due commit: prima lo spostamento puro (nessun comportamento cambia, nessun test nuovo), poi il cambio della mira con i due test che lo provano.

**Files:**
- Create: `src/main/java/com/toxineren/exoframes/frame/FrameSight.java`, `src/main/java/com/toxineren/exoframes/frame/FrameRemoval.java`
- Modify: `src/main/java/com/toxineren/exoframes/command/ExoframeCommand.java` (via `frameInSight`, il corpo di `removeFrame` e la costante `SIGHT`; Javadoc della classe con `{@link FrameSight#SIGHT}` e `{@link FrameRemoval#remove}`), `src/main/java/com/toxineren/exoframes/gametest/CarryOverGameTests.java`
- Testi: nessuna chiave. `docs/compat.md`: nessuna riga (non è una cucitura).

**Interfaces:**
- Consumes: oggi in `ExoframeCommand`: `private static Optional<ExoframeEntity> frameInSight(ServerPlayer)` (clip `ClipContext.Block.COLLIDER`, `ClipContext.Fluid.NONE`, poi `ProjectileUtil.getEntityHitResult(player, eyes, end, player.getBoundingBox().expandTowards(reach).inflate(1.0), e -> e instanceof ExoframeEntity && !e.isRemoved(), eyes.distanceToSqr(end))`), `private static final double SIGHT = 5.0`, `private static int removeFrame(CommandContext<CommandSourceStack>)`; `FrameInteractions.pieceStack(FrameState, PieceSlot) : ItemStack` (lancia `IllegalArgumentException` se per il livello del pezzo non c'è un oggetto); `Handouts.dropStill(Level, Vec3, ItemStack) : ItemEntity`; `FrameLog.event(UUID frameId, String event, Player player, ServerLevel level, Vec3 at)`; `FrameStateAccess.placed/updatePlaced/takeStowedOfPlaced/removePlaced`; aiuti di `GameTestKit` (Task 1): `add`, `placeFrame`, `ownedBy`, `standFacingNorth`, `exoframeCommand`, `recordState`, `removeFrame`.
- Produces:
  - `public final class FrameSight` con `public static final double SIGHT = 5.0` e `public static Optional<ExoframeEntity> inSight(ServerPlayer player)`.
  - `public final class FrameRemoval` con `public record Result(boolean removed, List<PieceSlot> deleted)` (`deleted` copiata con `List.copyOf`) e `public static Result remove(ServerPlayer by, ExoframeEntity frame)`.

- [ ] **Step 1: spostamento puro** — `FrameSight.inSight` = il corpo di oggi di `frameInSight`, identico (ancora `COLLIDER`, nessuna regola degli occhi). `FrameRemoval.remove` = il corpo di oggi di `removeFrame` da `FrameStateAccess frames = …` fino a `FrameLog.event(frameId, "removed", by, level, feet)` compreso, con gli stessi passi e lo stesso ordine (per ogni slot con un pezzo: `pieceStack`, su `IllegalArgumentException` lo slot va in `deleted` e si salta; `updatePlaced(… s.unmount(slot))`, poi `dropStill` ai piedi del record; poi la cella: `CellItem.fromCell`, `updatePlaced(… FrameState::removeCell)`, `dropStill`; poi `takeStowedOfPlaced` → `dropStill`; `removePlaced`; `frame.discard()`; la riga di log). Esiti: record piazzato assente → `new Result(false, List.of())`; un rifiuto del registro a metà → `new Result(false, deleted)` (ciò che è già a terra ci resta, come oggi); fine → `new Result(true, deleted)`. Il comando diventa:
  ```java
  Optional<ExoframeEntity> frame = FrameSight.inSight(player);
  Optional<PlacedFrame> record = frame.flatMap(f -> frames.placed(server, f.frameId()));
  if (record.isEmpty()) { source.sendFailure(Component.translatable(NO_FRAME_IN_SIGHT)); return 0; }
  Map<PieceSlot, Tier> tiers = new EnumMap<>(PieceSlot.class);   // i livelli servono al messaggio, letti prima
  record.get().state().pieces().forEach((slot, piece) -> tiers.put(slot, piece.tier()));
  FrameRemoval.Result result = FrameRemoval.remove(player, frame.get());
  if (!result.removed()) { return 0; }
  ```
  poi, come oggi, `remove.done` (con `sendSuccess(…, true)`) se `deleted` è vuota, altrimenti `names` = per ogni slot di `deleted` `tiers.get(slot).name().toLowerCase(Locale.ROOT) + " " + slot.name().toLowerCase(Locale.ROOT)` uniti da `", "`, la WARN di oggi con lo stesso testo (`Frame {} removed by {}; pieces without an item deleted with it: {}`) e `remove.done_deleted`. `setUnlocked` chiama `FrameSight.inSight` al posto di `frameInSight`. `new EnumMap<>(Map)` non si usa: lancia su una mappa vuota che non è un `EnumMap`.
- [ ] **Step 2: Run** `bash tools/run_gametests.sh` — Expected: quattro righe `OK <variante>: 100 test obbligatori passati, nessun avviso fuori elenco`; restano verdi `wallHidesTheFrameFromTheCommand`, `removeDropsTheContents`, `ownerUnlocksAndStrangerCanUse`, `strangerCannotUnlock`, `debugNeedsOperator`, `lockAndUnlockTheWornFrame`; build delle quattro varianti con `collectJar` → `BUILD SUCCESSFUL`; `OK sources`; `OK jars`.
- [ ] **Step 2b: commit** `frame: rimozione e linea di vista dei comandi in frame/` (nessun test nuovo).
- [ ] **Step 3: test in mondo** in `CarryOverGameTests` (due righe `add(tests, …)` in `generate()`), giocatore finto di `mockServerPlayer` (la creativa non conta: nessun danno):
  - `tallGrassHidesTheFrameFromTheCommand`: telaio di `ownedBy(owner)` con `placeFrame(helper, …, new Vec3(2.5, 1.0, 2.5), 0f)`; `standFacingNorth(helper, owner, new Vec3(2.5, 1.0, 5.5))` (occhi a y relativa 2,62); `helper.setBlock(new BlockPos(2, 0, 4), Blocks.DIRT)`, erba alta in due metà: `(2, 1, 4)` = `Blocks.TALL_GRASS.defaultBlockState()` (metà bassa), `(2, 2, 4)` = la stessa con `DoublePlantBlock.HALF` = `DoubleBlockHalf.UPPER` (`TALL_GRASS` ha lo stesso nome sulle due versioni; l'erba bassa no: `GRASS` su 1.20.1, `SHORT_GRASS` su 1.21.1). Premessa: per le due metà `getCollisionShape(level, pos).isEmpty()` e `!getShape(level, pos).isEmpty()` (senza la premessa il test potrebbe passare per un blocco solido). Attese:
    ```java
    helper.assertTrue(exoframeCommand(helper, owner, 0, "unlock") == 0, "unlocked a frame behind tall grass");
    helper.assertTrue(exoframeCommand(helper, owner, 2, "remove") == 0, "removed a frame behind tall grass");
    helper.assertTrue(!recordState(helper, frame).unlocked() && !frame.isRemoved(), "frame behind tall grass changed");
    // tolta l'erba (prima la meta' alta, poi la bassa) il telaio si trova: e' stata l'erba a nasconderlo
    helper.assertTrue(exoframeCommand(helper, owner, 0, "unlock") == 1 && recordState(helper, frame).unlocked(), "…");
    ```
    poi `removeFrame`. Con la mira di oggi (`COLLIDER`) il raggio attraversa l'erba e `unlock` dà 1: il test fallisce per la ragione giusta.
  - `eyesInsideAFrameBoxPickThatFrame`: due coppie nella stessa struttura, ciascuna con un telaio A dove sta il giocatore e un telaio B più lontano sulla stessa linea, ordine di creazione invertito fra le due (l'ordine in cui il gioco restituisce le entità decide oggi il risultato: `getEntityHitResult` mette a 0 la distanza di un'entità che contiene il punto di partenza e poi la fa sostituire da qualunque entità colpita dopo). Coppia 1, x = 2,5: `a1 = placeFrame(…, ownedBy(owner), new Vec3(2.5, 1.0, 5.5), 0f)`, poi `b1 = placeFrame(…, new Vec3(2.5, 1.0, 2.5), 0f)`. Coppia 2, x = 5,5: prima `b2` a z = 2,5, poi `a2` a z = 5,5. Per ciascuna: `standFacingNorth(helper, owner, <piedi di A>)`; premessa `a.getBoundingBox().contains(owner.getEyePosition())`; attese
    ```java
    helper.assertTrue(FrameSight.inSight(owner).map(ExoframeEntity::frameId).equals(Optional.of(a.frameId())), "…");
    helper.assertTrue(exoframeCommand(helper, owner, 0, "unlock") == 1 && recordState(helper, a).unlocked()
            && !recordState(helper, b).unlocked(), "the command unlocked the wrong frame");
    ```
    poi `removeFrame` dei quattro. Con il codice di oggi almeno una coppia può dare B, ma non è garantito (dipende da come il livello ordina le entità): il FAIL che conta allo Step 4 è quello del test dell'erba.
- [ ] **Step 4: Run** `bash tools/run_gametests.sh 1.21.1-neoforge` — Expected: FAIL su `tallGrassHidesTheFrameFromTheCommand` ("unlocked a frame behind tall grass").
- [ ] **Step 5: implementare** in `FrameSight.inSight`, nell'ordine: (1) `eyes = player.getEyePosition()`; i telai il cui riquadro contiene gli occhi, `player.level().getEntitiesOfClass(ExoframeEntity.class, new AABB(eyes, eyes).inflate(1.0e-7), f -> !f.isRemoved() && f.getBoundingBox().contains(eyes))`: se ce n'è almeno uno vince quello con il centro del riquadro più vicino agli occhi (`min` su `f.getBoundingBox().getCenter().distanceToSqr(eyes)`), senza guardare i blocchi; (2) altrimenti la ricerca di oggi con `ClipContext.Block.OUTLINE` al posto di `COLLIDER` (contorni dei blocchi, come la mira del clic: erba, fiori, torce, lastre fermano la linea di vista), `Fluid.NONE` e la stessa `getEntityHitResult`. Javadoc: le due regole e il perché (la mira del client usa i contorni; con gli occhi dentro un telaio il clic prende quello).
- [ ] **Step 6: Run** `bash tools/run_gametests.sh 1.21.1-neoforge` — Expected: PASS, compresi i sei test di Step 2.
- [ ] **Step 7: verifiche di fine task** — `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar` → `BUILD SUCCESSFUL`; `bash tools/check_sources.sh` → `OK sources`; `bash tools/check_jars.sh` → `OK jars`; `bash tools/run_gametests.sh` → quattro righe `OK <variante>: 102 test obbligatori passati, nessun avviso fuori elenco`.
- [ ] **Step 8:** commit `frame: i comandi mirano come il clic`.

