### Task 15: Niente letti con la tuta

**Files** (Java da `src/main/java/com/toxineren/exoframes/`):
- Create: `frame/SuitRestrictions.java`
- Modify: `RedstoneExoframes.java` (`SuitRestrictions.register()`), `gametest/EnergyGameTests.java`

**Interfaces:**
- Consumes: `InteractionEvent.RIGHT_CLICK_BLOCK` (`EventResult click(Player player, InteractionHand hand, BlockPos pos, Direction face)`, su tutti e due i lati e sulle quattro varianti); `EventResult.pass()`, `interruptFalse()`, `interruptsFurtherEvaluation()`, `isFalse()`; `SuitSize.wearsSuit(Player)` (il segnaposto del torso: vale sui due lati senza pacchetti); `BedBlock`; `SuitTick.LEAVE_FRAME_FIRST` (Task 13); `DebugWear.wear(ServerPlayer, SuitSummary)`, `SuitSummary.BARE`; aiuti `messages`, `leaveTestFrame`.
- Produces:
  - `public final class SuitRestrictions` in `frame/`: `public static void register()` = `InteractionEvent.RIGHT_CLICK_BLOCK.register(SuitRestrictions::onUseBlock);`, chiamato in `RedstoneExoframes.init()` subito dopo `SuitTick.register()`; `public static EventResult onUseBlock(Player player, InteractionHand hand, BlockPos pos, Direction face)`: `pass()` se il giocatore non indossa la tuta (`SuitSize.wearsSuit`) o il blocco non è un `BedBlock`; altrimenti `interruptFalse()` sui due lati e per le due mani, e solo dal server, per la mano principale, `displayClientMessage(Component.translatable(SuitTick.LEAVE_FRAME_FIRST), true)`.
    Javadoc: con la tuta non si dorme (§12.3, domanda 5 di §8): il risveglio metterebbe un ingombro alto 2,97 in una stanza bassa; l'annullamento vale sui due lati e per le due mani, il messaggio solo dal server e solo per la mano principale (una volta per clic); le cavalcature sono nel passo 3 di `SuitTick`.
- Testi: nessuna chiave nuova (usa `leave_frame_first` del Task 13). `docs/compat.md`: nessuna riga.

- [ ] **Step 1: test in mondo** — in coda a `EnergyGameTests.generate()`, `bedIsRefusedInASuit` (chiamate attraverso l'evento, così il test prova anche la registrazione):
  ```java
  ServerPlayer wearer = Compat.gameTests().mockServerPlayer(helper);
  ServerPlayer plain = Compat.gameTests().mockServerPlayer(helper);
  DebugWear.wear(wearer, SuitSummary.BARE);
  BlockPos bed = helper.absolutePos(new BlockPos(1, 1, 1));
  BlockPos stone = helper.absolutePos(new BlockPos(3, 1, 1));
  helper.setBlock(new BlockPos(1, 1, 1), Blocks.RED_BED);
  helper.setBlock(new BlockPos(3, 1, 1), Blocks.STONE);
  EventResult main = InteractionEvent.RIGHT_CLICK_BLOCK.invoker().click(wearer, InteractionHand.MAIN_HAND, bed, Direction.UP);
  helper.assertTrue(main.interruptsFurtherEvaluation() && main.isFalse(), "bed accepted in a suit: " + main);
  helper.assertTrue(messages(wearer, SuitTick.LEAVE_FRAME_FIRST) == 1, "no message for the main hand");
  EventResult off = InteractionEvent.RIGHT_CLICK_BLOCK.invoker().click(wearer, InteractionHand.OFF_HAND, bed, Direction.UP);
  helper.assertTrue(off.interruptsFurtherEvaluation() && off.isFalse(), "bed accepted with the off hand");
  helper.assertTrue(messages(wearer, SuitTick.LEAVE_FRAME_FIRST) == 1, "a second message for the off hand");
  helper.assertTrue(!InteractionEvent.RIGHT_CLICK_BLOCK.invoker().click(wearer, InteractionHand.MAIN_HAND, stone,
          Direction.UP).interruptsFurtherEvaluation(), "a stone block refused in a suit");
  helper.assertTrue(!InteractionEvent.RIGHT_CLICK_BLOCK.invoker().click(plain, InteractionHand.MAIN_HAND, bed,
          Direction.UP).interruptsFurtherEvaluation(), "bed refused without a suit");
  helper.assertTrue(messages(plain, SuitTick.LEAVE_FRAME_FIRST) == 0, "message without a suit");
  leaveTestFrame(wearer);
  helper.succeed();
  ```
  (`Blocks.RED_BED` di per sé è la metà dei piedi: basta per `instanceof BedBlock`, e il test non chiama mai il letto.)
- [ ] **Step 2: Run** `bash tools/run_gametests.sh 1.21.1-neoforge` — Expected: FAIL su `bedIsRefusedInASuit` (`bed accepted in a suit: …`: nessun ascoltatore registrato).
- [ ] **Step 3: implementare** `SuitRestrictions` e la registrazione come in "Produces".
- [ ] **Step 4: Run** `bash tools/run_gametests.sh` — Expected: quattro righe `OK <variante>: 135 test obbligatori passati, nessun avviso fuori elenco` (134 più 1; uno in più per ciascun ripiego dei Task 11-12).
- [ ] **Step 5: verifiche di fine task** — `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar` → `BUILD SUCCESSFUL`; `bash tools/check_sources.sh` → `OK sources`; `bash tools/check_jars.sh` → `OK jars`; GameTest già allo Step 4. Il letto rifiutato sul client (nessuna animazione di sonno) è una voce della checklist per il PC della 3c-2.
- [ ] **Step 6: commit** `frame: con la tuta non si dorme`.

