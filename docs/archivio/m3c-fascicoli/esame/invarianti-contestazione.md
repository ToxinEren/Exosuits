# Challenge of the "invariants" examination (part 3c decisions)

Read-only. Every claim re-derived from the repo (`J` = `src/main/java/com/toxineren/exoframes`), the dossiers, or javap dumps in `esame/jp`, `esame/ch`.

## I1 — HOLDS (blocker)
- `FrameStateAccessImpl.update` (J/compat/impl/FrameStateAccessImpl.java:66-78) calls `updateWorn` with a factory; `FrameStateSavedData.updateWorn` (:335-347) goes through `getOrCreateWorn` (:317-329), which creates `new FrameRecord(UUID.randomUUID(), factory.get())` and marks structural. Decision 7.2 says "through `update`" with no precondition. A queued `cell_swap` (NetImpl.java:82 queues on the server thread) arriving after exit/death therefore creates a worn record, inserts the cell; `SlotGuard.tick` (J/frame/SlotGuard.java:76-115) then dresses the player; a later exit places a real frame. The only guard precedent is `FrameEntry.exit` (J/frame/FrameEntry.java:170-174: alive, not spectator, `wornFrameId` present).
- Fix: right. `update` must keep creating (DebugWear.java:40 and GameTests depend on it), so a new existing-only structural write returning boolean (`changeWorn`) is the smallest correct API; the handler guard (alive, not spectator, worn) as in `FrameEntry.exit`. Fold I2's ordering into the same decision line.

## I2 — PARTLY (major, not blocker on its own once I1 is fixed)
- Holds: `update` returns `void` and in read-only does nothing (FrameStateAccessImpl.java:68-71). If 7.2 writes the inventory unconditionally, read-only gives cell duplication (or loss, depending on order). 11.2 makes read-only reachable by a new, ordinary case (file without sections), so this is not theoretical.
- Refuted part: "the consumer can throw because EnergyCell throws". The stack is read with `CellItem.toCell` (J/content/CellItem.java:24-28): `min(cellEnergy, capacity)`, and `PieceTags.readEnergy` is `max(0, …)` (J/compat/impl/PieceTags.java:90-92); capacity is validated `> 0` (ConfigValidation.java:58). `FrameState.insertCell/removeCell` only null-check. So no throw path exists in the swap with the current code.
- Refuted part: 8.3/8.6 "same gap". The charger has no read-only mode; its insert/withdraw cannot be refused after the hand changed. Only ordering against exceptions matters there, and a single-thread write of a slot cannot fail.
- `split(1)`: cells are `stacksTo(1)` (J/content/ModItems.java:21-22); count > 1 exists only through the 1.21.1 `max_stack_size` component (I10). Harmless to keep.
- Correct statement and fix: 7.2 must say "registry first, inventory only if the registry write returned true", exactly the existing precedent `FrameInteractions.insertCell` (J/frame/FrameInteractions.java:196-201: `updatePlaced` boolean, then `shrink`) and `unmount` (:226-231: build the stack, `updatePlaced`, then `Handouts.give`). One sentence, with `changeWorn` from I1.

## I3 — PARTLY (real trap, but decision 8.2 does not commit the error; minor as a decision defect, must be written)
- Bytecode re-read (`esame/jp/hop20.txt` `tryMoveInItem` @0-114): `getItem(slot)` @2; empty → `setItem` @42; otherwise `canMergeItems`, `shrink` @92 on the hopper's stack, `grow` @99 on the stack `getItem` returned, no `setItem`. `blocco-verifica.md` C10 confirms both versions. So a `getItem(1)` that rebuilds from a count loses dust from the second merge on.
- But 8.2 says "slot 1 = redstone dust (a normal stack, up to 64)" and "no item stack in the NBT": that describes a live in-memory stack persisted as a count, which is the correct design; the dossier already names the trap (blocco.md:154 trap (a); :209). It is a missing implementation constraint, not a wrong decision.
- Add to the fix: hoppers ignore the container's `getMaxStackSize` (blocco-verifica.md:91); the "only redstone dust" and room limits must live in `canPlaceItem`/`canPlaceItemThroughFace`. Also: 1.21.1 extraction rollback writes back the `getItem` stack only for count 1 (blocco-verifica.md:92), another reason slot 1 must be live. The GameTest (two dust via side hopper → count 2) is right.

## I4 — PARTLY (minor)
- Verified: under `/save-off` vanilla still writes player files. `MinecraftServer.saveEverything` calls `PlayerList.saveAll` unconditionally (javap `esame/ch/.../MinecraftServer.class`, saveEverything @9, then `saveAllChunks` which checks `ServerLevel.noSave` @67); `PlayerList.remove` @14 calls `save(ServerPlayer)` unconditionally.
- But the consequence: under `/save-off` the pairing is already broken at every autosave (players written, registry not), and design §4.5 (spec line 70) states the guarantee does not hold under `/save-off`. Honouring `/save-off` at quit removes a partial mitigation inside a window the design already disclaims; it does not break a promised guarantee.
- Fix: the proposed change (narrow to player files + registry file, do not honour `/save-off`, close the §11 item with that reason) is right; severity minor. Note: narrowing to "this registry's file" means calling the data's own file write (`saveFile`) and `markSaved`, not `DimensionDataStorage.save()`.

## I5 — HOLDS (major as an undecided gap)
- The charger's cell lives in chunk data; `saveWithPlayersIfNeeded` (FrameStateSavedData.java:841-849) saves players and overworld data storage only; the player file is written at quit (`PlayerList.remove` @14). Insert/withdraw + quit + crash before the next autosave → cell lost/duplicated. Without a quit, both revert to the last autosave together (players then levels), so the window is precisely "quit then crash" — the case §4.5 closed for the registry.
- Fix: declaring it as a second known exception next to §7 point 26 (spec line 260) and asking the owner is right and cheapest. The mitigation as proposed (`getChunkSource().save(false)`) saves every dirty chunk of that level and only enqueues to the IO worker: heavy and still not synchronous; do not adopt it as default.

## I6 — HOLDS (major)
- `EnergyCell(int,int)` throws for `stored > capacity` (J/core/energy/EnergyCell.java:13-15). The config becomes file-editable in 3c (6.1), so a lowered `cellCapacity` with a charger holding a fuller cell is ordinary. 1.20.1 `BlockEntity.loadStatic` lambda catches `Throwable`, logs "Failed to load data for block entity" and returns null (`esame/jp/be20.txt`:364-381): block entity dropped with its contents, overwritten at next save. (1.21.1 not re-checked by me.)
- Fix: right; mirror the existing clamps `CellItem.toCell` and `FrameStateCodec` (J/compat/impl/FrameStateCodec.java:136-142, which clamps stored to capacity). Clamp the charger cell to the CURRENT config capacity, as toCell does.

## I7 — PARTLY (minor)
- Correct: `FrameRules.jump` `(int) Math.round(jumpCost * mult)` (J/core/frame/FrameRules.java:194) wraps: 2e9 × 1.25 → −1,794,967,296 (python); threshold 1,717,986,919 with the default 1.25 multiplier, and `brokenTorsoConsumptionMultiplier` is unbounded above (ConfigValidation `atLeastOne`), so smaller costs also wrap. `drain(negative)` throws inside the tick.
- Wrong: "rate×1000 in int wraps from 2,147,484 RE/s". `EnergyMeter.tick` (J/core/energy/EnergyMeter.java:17-19) computes `rate * 1000.0 / 20` in double: 50 milli/tick per RE/s, overflow only above 42,949,672 RE/s (× multiplier). The −64,748,365 figure applies only to a hypothetical int charger implementation.
- Wrong: `ExitHold.ticksNeeded` overflows. It computes in long and saturates at `Integer.MAX_VALUE` (J/client/ExitHold.java:45-47).
- Mitigating: Gson rejects int literals outside int range (`JsonSyntaxException`), so 6.2's "one error, defaults" path already catches > 2^31−1; only values in (1.7e9, 2.1e9] or huge doubles reach validation. These are operator mistakes, not abuse.
- Fix: upper bounds in validation are cheap and correct (also bound `brokenTorsoConsumptionMultiplier`, e.g. ≤ 10); charger milli arithmetic in long. The try/catch in `SuitTick`/charger ticker is good; keep it with "log once".

## I8 — PARTLY (minor)
- A server running this mod validates `cellCapacity > 0` before sending; 0/negative needs a hostile or broken server, which can already disrupt a client in many vanilla ways. Low value.
- Real, non-hostile part the finding mentions only in passing: after an owner lowers `cellCapacity`, stacks still hold the old charge (the stack keeps `energy` only; `toCell` clamps only when read for use), so `round(13*stored/capacity)` > 13 and the tooltip shows "stored > capacity". Also the client uses local defaults before `client_rules` arrives.
- Fix: width = `clamp(round(13.0 * min(stored, cap) / cap), 0, 13)`, hidden when `cap <= 0`; tooltip shows `min(stored, cap)`; validating received values through `ConfigValidation` is fine and cheap. Severity minor.

## I9 — PARTLY (minor)
- True that §12 lists only happy paths. But it is derivative: each test belongs to the fix of I1, I2, I3, I6 and should be written there, not as a separate decision.
- "Swap with a read-only registry" is not testable today: no test seam sets read-only (`GameTestsImpl` only reads `data.readOnly()`, :154, :208). The decision must add that hook (or test the ordering with JUnit on a pure swap function) — otherwise the test is an untestable promise itself.
- "Swap from a stack of count > 1" exists only on 1.21.1 (max_stack_size component); mark it 1.21.1-only.

## Missed (same lens)
1. **Charger: every content change must call `setChanged()` (major).** Interaction insert/withdraw, dust added, the ticker's charge and dust consumption. Hoppers call `setChanged` themselves (`tryMoveInItem` after merge), player code and the ticker do not unless written. A chunk not marked unsaved is not written at unload or autosave: a cell inserted by hand disappears after an ordinary unload/reload, with no crash (and a withdrawn one comes back = duplicate). 8.2/8.6 must state it; GameTest: insert, unload/reload chunk (or save+read BE NBT), cell still there.
2. **Charger interaction runs on the client too (minor/major for desync).** `use`/`useItemOn` are called on client and server (blocco.md:27). The neutral per-hand method of 8.3 must not mutate the hand stack or the BE on the client; it predicts the result only. A client-side shrink with a server refusal (no slot packet sent) leaves a ghost-missing cell. Follow FrameInteractions (server-only mutation).
3. **Charger read/clamp must use the current config capacity, and the frame registry keeps its own per-cell capacity** (FrameStateCodec writes `CAPACITY`, :67): a cell swapped out of a frame with a larger stored capacity than config is silently clamped at its next `toCell`. Related to I19 but the swap is new in 3c: 7.2 must say what the old cell's charge becomes (clamp and log, or keep). (minor)
