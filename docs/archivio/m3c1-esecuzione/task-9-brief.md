### Task 9: `FrameSounds` e i suoni delle azioni della 3b

**Files:**
- Create: `src/main/java/com/toxineren/exoframes/frame/FrameSounds.java`
- Modify: `src/main/java/com/toxineren/exoframes/frame/FramePlacement.java` (`PLACE` in `place`, non in `spawnView`), `src/main/java/com/toxineren/exoframes/frame/FrameInteractions.java`, `src/main/java/com/toxineren/exoframes/frame/FrameEntry.java`, `src/main/java/com/toxineren/exoframes/gametest/CarryOverGameTests.java`, `docs/compat.md`
- Testi: nessuna chiave. `docs/compat.md`, "Tabella API vanilla/loader → interfaccia", riga nuova: "Suoni dal server: `Level.playSound(Player, double, double, double, SoundEvent, SoundSource, float, float)`, uguale sulle due versioni, con le sole costanti di `SoundEvents` che sono un `SoundEvent` semplice su tutte e due (non quelle che su 1.21.1 sono un `Holder`, come `ARMOR_EQUIP_*`) | Nessuna cucitura: `frame/FrameSounds`".

**Interfaces:**
- Consumes: `ServerLevel.playSound(@Nullable Player except, double x, double y, double z, SoundEvent, SoundSource, float volume, float pitch)` (con `null` lo sentono tutti quelli vicini, il giocatore compreso; mai `player.playSound(...)` sul server, che esclude proprio lui); `GameTests.countEquipEvents(ServerLevel, BlockPos) : EquipEventCounter`; `FrameStateAccess.takeStowedOfPlaced(MinecraftServer, UUID) : List<StowedStack>`; aiuti di `GameTestKit`: `structureBox(GameTestHelper) : AABB`, `useExoframeItem`, `placeFrame`, `click`, `ownedBy`, `charged`, `frameWithStowedBoots`, `removeFrame`.
- Produces (`frame/FrameSounds`, come in "Nomi e chiavi comuni"):
  - `public enum Sound { PLACE(SoundEvents.METAL_PLACE, SoundSource.BLOCKS), MOUNT(SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS), UNMOUNT(SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.PLAYERS), ENTER(SoundEvents.PISTON_CONTRACT, SoundSource.PLAYERS), EXIT(SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS), CELL_SWAP(SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS), PIECE_BROKEN(SoundEvents.ITEM_BREAK, SoundSource.PLAYERS), ENERGY_LOW(SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS), ENERGY_EMPTY(SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.PLAYERS) }` (le nove costanti sono `SoundEvent` semplici sulle due versioni, verificato sui jar);
  - `public static void play(ServerLevel level, Vec3 at, Sound sound)` = `level.playSound(null, at.x, at.y, at.z, event, source, 1.0F, 1.0F)`;
  - `public static Recording record(ServerLevel level, AABB area)`, `public interface Recording extends AutoCloseable { List<Sound> sounds(); @Override void close(); }`: solo per i test.

- [ ] **Step 1: test in mondo** in `CarryOverGameTests`, tre righe `add(tests, …)`; il registratore si apre con `try (FrameSounds.Recording sounds = FrameSounds.record(helper.getLevel(), structureBox(helper)))` e registra solo i suoni della struttura del test (i test girano insieme):
  - `placingPlaysTheMetalSound`: pietra a `(2, 1, 3)`; `ExoframeEntity frame = useExoframeItem(helper, player, new BlockPos(2, 1, 3), true)` → `sounds.sounds().equals(List.of(Sound.PLACE))`; un secondo `useExoframeItem(helper, player, new BlockPos(2, 1, 3), false)` (il telaio c'è già: rifiutato) → ancora `[PLACE]`; `removeFrame`.
  - `mountAndUnmountPlayTrapdoorSounds`: `frame = placeFrame(helper, ownedBy(player), new Vec3(3.5, 1.0, 3.5), 0f)` (solo registro ed entità: nessun suono); in mano `ModItems.SCRAP_HELMET`, `click(frame, player, 0.0, 2.5, 0.0)`; in mano `CellItem.fromCell(new EnergyCell(capacity, 4_000))`, `click(… 0.0, 1.6, 0.0)`; di nuovo un elmo, `click` alla testa (slot occupato: niente); mano vuota, `player.setShiftKeyDown(true)`, tre `click(… 0.0, 2.5, 0.0)` (elmo, poi la cella perché la fascia della testa è vuota, poi il telaio vuoto raccolto) → `[MOUNT, MOUNT, UNMOUNT, UNMOUNT]` (la raccolta non suona); poi `ExoframeEntity boots = frameWithStowedBoots(helper, player, ownedBy(player), new Vec3(5.5, 1.0, 3.5))` e un `click` accovacciato su di lui (gli stivali stivati tornano) → `[MOUNT, MOUNT, UNMOUNT, UNMOUNT, UNMOUNT]`; `removeFrame(server, boots)`.
  - `enterAndExitPlayPistonSoundsAndArmorStaysSilent`: pietra a `(3, 1, 3)` e `(3, 1, 4)`; `player.doTick()` (dopo il primo tick gli slot armatura non tacciono più per `firstTick`); elmo di ferro in testa; `frame = placeFrame(helper, charged(ownedBy(player)), new Vec3(3.5, 2.0, 3.5), 0f)`; registratore e `Compat.gameTests().countEquipEvents(level, BlockPos.containing(frame.position()))` aperti insieme; `FrameEntry.enter(player, frame)` → `[ENTER]`; yaw 0, `FrameEntry.exit(player)` → `[ENTER, EXIT]`; di nuovo dentro (`FrameEntry.enter` sull'entità del record) e `FrameEntry.onDeath(player)` chiamato direttamente (il giocatore resta vivo, il telaio resta nel mondo come alla morte) → `[ENTER, EXIT, ENTER]`: alla morte nessun suono; `equips.count() == 0` (l'armatura che entra ed esce dagli slot resta silenziosa); poi `helper.assertTrue(Compat.frameState().takeStowedOfPlaced(helper.getLevel().getServer(), frame.frameId()).size() == 1, "helmet not kept in the frame at death")` (l'elmo stivato resta nel record alla morte: senza ritirarlo `removeFrame` lo renderebbe orfano con la WARN `… left the registry with 1 stowed armor entries still inside …`, che l'elenco ammette esattamente una volta, per `registryFileIsReadBack`); `removeFrame` del telaio.
- [ ] **Step 2: Run** `bash tools/run_gametests.sh 1.21.1-neoforge` — Expected: FAIL (compilazione: `FrameSounds` manca).
- [ ] **Step 3: implementare `FrameSounds`**. Il registratore (corpo che le firme non decidono):
  ```java
  private static final List<Rec> RECORDINGS = new CopyOnWriteArrayList<>();
  public static void play(ServerLevel level, Vec3 at, Sound sound) {
      level.playSound(null, at.x, at.y, at.z, sound.event, sound.source, 1.0F, 1.0F);
      for (Rec rec : RECORDINGS) {
          if (rec.level == level && rec.area.contains(at)) { rec.sounds.add(sound); }
      }
  }
  public static Recording record(ServerLevel level, AABB area) { Rec rec = new Rec(level, area); RECORDINGS.add(rec); return rec; }
  ```
  `Rec` è una classe privata (non un record: `close()` fa `RECORDINGS.remove(this)` e deve togliere proprio quella), `sounds()` restituisce `List.copyOf`. Javadoc: la tabella, `null` e il perché, il registratore solo per i test.
- [ ] **Step 4: i punti dei suoni**, ciascuno dopo l'esito riuscito del registro, mai su un rifiuto:
  - `FramePlacement.place`: dopo `spawnView`, `FrameSounds.play(level, feet, Sound.PLACE)` (l'oggetto telaio e `/exoframe debug place`; non `spawnView`, che usano anche l'uscita e il riallineamento);
  - `FrameInteractions`, alla posizione dell'entità (`frame.position()`, livello `player.serverLevel()`): `MOUNT` dopo un `mountPiece` e un `insertCell` riusciti; `UNMOUNT` dopo `unmountPiece`, dopo la cella tolta e dopo l'armatura stivata ritirata quando la lista non è vuota; nessun suono per la raccolta del telaio vuoto;
  - `FrameEntry.enter`: alla fine di un ingresso riuscito, `FrameSounds.play(level, spot, Sound.ENTER)`; `FrameEntry.exit`: dopo `stepOut`, `FrameSounds.play(player.serverLevel(), player.position(), Sound.EXIT)`, solo se l'uscita è riuscita; `onDeath`: nessun suono.
- [ ] **Step 5: Run** `bash tools/run_gametests.sh 1.21.1-neoforge` — Expected: PASS (`debugPlaceBareAndScrap`, `exoframeItemPlacesAnEmptyFrame`, i test di montaggio, ingresso e uscita della 3b invariati).
- [ ] **Step 6:** `docs/compat.md` (riga della tabella in **Files**).
- [ ] **Step 7: verifiche di fine task** — build delle quattro varianti con `collectJar` → `BUILD SUCCESSFUL`; `OK sources`; `OK jars`; `bash tools/run_gametests.sh` → quattro `OK <variante>` con il numero del task precedente più 3, log pulito. Che i suoni si sentano lo prova la checklist per il PC della 3c-2.
- [ ] **Step 8:** commit `frame: un suono del gioco base per piazzare, montare, smontare, entrare e uscire`.

