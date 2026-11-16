### Task 3: Un solo punto per "lo stato indossato è cambiato"; segnaposto tolti alla chiusura di un menu

**Files** (Java da `src/main/java/com/toxineren/exoframes/`):
- Create: `frame/WornState.java`
- Modify: `frame/FrameEntry.java`, `frame/DebugWear.java`, `frame/SlotGuard.java`, `gametest/CarryOverGameTests.java`, `docs/compat.md` (riga del ricalcolo `:169`)

**Interfaces:**
- Consumes: `FrameEntry.enter(ServerPlayer, ExoframeEntity)` (`:95-146`: `refreshDimensions()` a `:143`, `FrameLog.event(…, "entered", …)` a `:144`), `FrameEntry.exit` e `onDeath` attraverso `leaveFrame(ServerPlayer, FrameStateAccess, String)` (privato, `:283-306`: `refreshDimensions()` a `:298`, `FrameLog.event(frameId, event + spot.logNote(), …)` a `:299`); `DebugWear.wear(ServerPlayer, SuitSummary)` e `DebugWear.remove(ServerPlayer) : int`; `FrameLog.event(UUID frameId, String what, Player player, ServerLevel level, Vec3 position)` (riga `Frame {} {} by {} in {} at {}`); `FrameStateAccess.wornFrameId(ServerPlayer) : Optional<UUID>`; `Placeholders.removeAll(ServerPlayer) : int` (pubblico, slot armatura compresi) e `Placeholders.removeStrays(ServerPlayer) : int` (package-private, tutto tranne gli slot armatura, menu aperto compreso); `PlayerEvent.CLOSE_MENU` di Architectury, `void close(Player player, AbstractContainerMenu menu)`, uguale su 9.2.14 e 13.0.11, solo server: sparato in `ServerPlayer.doCloseContainer()` dopo `containerMenu.removed(player)` (che ha già rimesso nell'inventario l'oggetto sul cursore) e prima di `containerMenu = inventoryMenu`, anche per il menu dell'inventario.
- Produces:
  - `public final class WornState` con `static void changed(ServerPlayer player, String event, UUID frameId, ServerLevel level, Vec3 position)` = `player.refreshDimensions();` poi `FrameLog.event(frameId, event, player, level, position);`. Javadoc: è l'unico punto, dopo la scrittura nel registro, per ingresso, uscita, morte e comando di debug (design §12.14); i task delle funzioni vi aggiungono gli attributi e lo scarto del record del tick; rientro e rinascita non passano di qui (li copre il record del tick, §12.3); `SlotGuard` non lo chiama, perché non scrive mai il registro (i suoi cambi sono viste del registro). Firma più larga di quella di §12.14 (`changed(ServerPlayer, String)`), detto nel Javadoc: dopo `frames.exit` il `frameId` non è più indossato e alla morte la posizione è il punto di riposo, quindi li passa chi chiama (§12.14 si corregge nel Task 24).
  - Chiamate (sostituiscono le coppie `refreshDimensions()` + `FrameLog.event`): in `enter`, al posto di `:143-144`, `WornState.changed(player, "entered", record.frameId(), level, spot)`; in `leaveFrame`, al posto di `:298-299`, `WornState.changed(player, event + spot.logNote(), frameId.get(), level, spot.position())` (eventi `left`, `left at death`, con la nota del punto di riposo come oggi); in `DebugWear.wear`, al posto del `refreshDimensions()`, `WornState.changed(player, "worn by debug command", frameId, player.serverLevel(), player.position())` con `frameId = frames.wornFrameId(player).orElseThrow(...)` letto dopo `update`; in `DebugWear.remove` l'identità si legge prima di `frames.clear(player)` e, se c'era un record, `WornState.changed(player, "deleted by debug command", frameId, player.serverLevel(), player.position())` dopo `clear`; senza record solo `refreshDimensions()`, nessuna riga. Le righe nuove: `Frame <id> worn by debug command by <giocatore> in <dimensione> at <x, y, z>` e `Frame <id> deleted by debug command by …` (INFO).
  - Vincoli d'ordine della 3b, invariati e scritti nel Javadoc di `WornState`: i segnaposto si tolgono prima di `returnStowed` e di `exit` (`Placeholders.removeAll` resta dov'è, in `exit`, `onDeath` e `DebugWear.remove`); in `enter` il teletrasporto (`teleportTo`) viene prima di `changed` (ingombro calcolato nel punto del telaio), e `frame.discard()` e `SafeSpots.remember` restano prima; in `exit` `changed` (dentro `leaveFrame`) viene prima di `stepOut` (il passo fuori misura il giocatore vanilla); `FramePlacement.spawnView` resta prima di `changed`; il messaggio `ELSEWHERE` resta dopo.
  - `SlotGuard`: `static void onMenuClosed(ServerPlayer player)` (package-private): con un record indossato (`Compat.frameState().get(player).isPresent()`) `Placeholders.removeStrays(player)` (mai gli slot armatura: toglierli farebbe rimpicciolire e ricrescere chi indossa la tuta); senza, `if (Placeholders.removeAll(player) > 0) player.refreshDimensions();`. Registrato in `SlotGuard.register()`: `PlayerEvent.CLOSE_MENU.register((player, menu) -> { if (player instanceof ServerPlayer serverPlayer) onMenuClosed(serverPlayer); });`. Javadoc della classe e di `register`: la pulizia gira anche alla chiusura di ogni menu, perché più clic in un solo pacchetto possono lasciare una copia in una cassa prima del tick (design §11, §12.14).
- Testi: nessuna chiave nuova. `docs/compat.md:169` ("Ricalcolo"): "sul server ricalcolano subito anche chi scrive il registro, passando da `frame/WornState.changed` (ingresso, uscita, morte, `DebugWear.wear` e `remove`), e la guardia degli slot (`SlotGuard`) quando ha cambiato uno slot armatura o, alla chiusura di un menu (`PlayerEvent.CLOSE_MENU`), ha tolto segnaposto a chi non indossa un telaio".

- [ ] **Step 1: test in mondo** — in `CarryOverGameTests`, in coda a `generate()`:
  - `strayPlaceholderInAChestGoesWhenTheMenuCloses`:
    ```java
    ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
    DebugWear.wear(player, SuitSummary.BARE);
    List<ItemStack> worn = List.copyOf(player.getInventory().armor);
    SimpleContainer chest = new SimpleContainer(27);
    player.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory, chest),
            Component.literal("chest")));
    helper.assertTrue(player.containerMenu instanceof ChestMenu, "chest menu not open");
    chest.setItem(4, player.getItemBySlot(EquipmentSlot.CHEST).copy());
    chest.setItem(5, new ItemStack(Items.DIRT, 3));
    player.closeContainer();
    helper.assertTrue(chest.getItem(4).isEmpty(), "stray torso placeholder left in the chest");
    helper.assertTrue(chest.getItem(5).is(Items.DIRT) && chest.getItem(5).getCount() == 3, "real item touched");
    helper.assertTrue(player.containerMenu == player.inventoryMenu, "menu still open");
    for (int i = 0; i < worn.size(); i++) {
        helper.assertTrue(player.getInventory().armor.get(i) == worn.get(i), "armor slot " + i + " changed");
    }
    helper.assertTrue(countPlaceholders(player) == 4, countPlaceholders(player) + " placeholders, expected 4");
    DebugWear.remove(player);
    helper.succeed();
    ```
  - `placeholderOfANonWearerGoesWhenAMenuCloses`: giocatore finto senza telaio; `items.set(5, new ItemStack(ModItems.EXOFRAME_HELMET.get()))`, `items.set(6, new ItemStack(Items.DIRT, 2))`; `player.closeContainer()` (il menu aperto è quello dell'inventario: `CLOSE_MENU` arriva lo stesso) → `items.get(5).isEmpty()`, `countPlaceholders(player) == 0`, `countInMainInventory(player, Items.DIRT) == 2`. Nel Javadoc il motivo per cui il test non passerebbe senza il codice nuovo: `SlotGuard.tick` è su `PLAYER_POST`, che per il giocatore finto non scatta mai, quindi solo `CLOSE_MENU` può togliere quel segnaposto; nel primo test, chiuso il menu, la cassa non è più raggiungibile da nessun tick.
- [ ] **Step 2: Run** `bash tools/run_gametests.sh 1.21.1-neoforge` — Expected: FAIL su `strayPlaceholderInAChestGoesWhenTheMenuCloses` (`stray torso placeholder left in the chest`) e `placeholderOfANonWearerGoesWhenAMenuCloses`.
- [ ] **Step 3: implementare** `WornState`, i quattro punti di chiamata e `SlotGuard.onMenuClosed` come in "Produces"; il Javadoc di `DebugWear` (classe, `wear`, `remove`) e di `FrameEntry.enter`/`leaveFrame` dice `WornState.changed` al posto di "ricalcola le dimensioni" e "una riga nel log".
- [ ] **Step 4: Run** `bash tools/run_gametests.sh` — Expected: quattro righe `OK <variante>: 98 test obbligatori passati, nessun avviso fuori elenco` (tutti i test 3b di ingresso, uscita, morte, `debugWear*`, `suitGrowsBoxAndEyes`, invariati, passano); `grep -c 'worn by debug command by test-mock-player' versions/1.21.1-neoforge/build/gametest/server.log` e `grep -c 'deleted by debug command by test-mock-player' …` → ciascuno ≥ 1; `grep -c 'entered by test-mock-player' …` e `grep -c 'left by test-mock-player' …` → ciascuno ≥ 1 (le righe d'ingresso e d'uscita ci sono ancora, ora scritte da `WornState`).
- [ ] **Step 5:** `docs/compat.md:169` come in "Interfaces".
- [ ] **Step 6: verifiche di fine task** — build e `collectJar` delle quattro varianti → `BUILD SUCCESSFUL`; `bash tools/check_sources.sh` → `OK sources`; `bash tools/check_jars.sh` → `OK jars`; GameTest già allo Step 4.
- [ ] **Step 7: commit** `frame: un punto per il cambio dello stato indossato, riga di log del debug, segnaposto tolti alla chiusura dei menu`.

