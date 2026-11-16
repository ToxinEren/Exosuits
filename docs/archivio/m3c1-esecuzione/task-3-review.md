# Task 3 - review (spec + quality)

BASE f754b0e3c468d94f0d8c6844615ff93f635c498a, HEAD d5f080940e48b5908ee76eace10ec98ee794ebd0 (1 commit). Diff read once from `review-f754b0e..d5f0809.diff`; no hunk was cut off.

### Spec Compliance

- ✅ Spec compliant. Every file in the brief has its hunk:
  - `frame/WornState.java` (new, diff 366-411): `public final class`, package-private `changed(ServerPlayer, String, UUID, ServerLevel, Vec3)` = `refreshDimensions()` then `FrameLog.event(...)` (WornState.java:36-38). Class Javadoc names the callers, the feature tasks that add attributes and the tick-record discard, re-login/respawn via the tick record (§12.3), why `SlotGuard` does not call it, and all the 3b ordering constraints (removeAll before returnStowed/exit, teleport/discard/remember before, changed before stepOut, spawnView before, ELSEWHERE after).
  - `FrameEntry.enter`: `WornState.changed(player, "entered", record.frameId(), level, spot)` after teleport, discard, `SafeSpots.remember` (FrameEntry.java:143). `leaveFrame`: `WornState.changed(player, event + spot.logNote(), frameId.get(), level, spot.position())` after `spawnView`, before the ELSEWHERE message (FrameEntry.java:297). Checked the unchanged callers: `exit` keeps removeAll -> returnStowed -> leaveFrame -> stepOut (FrameEntry.java:174-177), `onDeath` keeps removeAll -> leaveFrame (FrameEntry.java:196-197).
  - `DebugWear.wear`: `frameId` read with `wornFrameId(...).orElseThrow` after `update`, `changed(... "worn by debug command" ...)` replaces `refreshDimensions` (DebugWear.java:47-51). `DebugWear.remove`: id read before `clear`, `changed(... "deleted by debug command" ...)` after `clear`; without a record only `refreshDimensions()` (DebugWear.java:68-77). `Placeholders.removeAll` stays first.
  - `SlotGuard.onMenuClosed` package-private, `get(player).isPresent()` -> `removeStrays`, else `if (removeAll > 0) refreshDimensions()` (SlotGuard.java:146-151); registered on `PlayerEvent.CLOSE_MENU` with the `ServerPlayer` filter (diff 314-318). Class and `register` Javadoc carry the multi-click reason (§11, §12.14). Matches design §12.14 line 653 ("per chi indossa la tuta solo le copie fuori posto, per chi non la indossa tutti i segnaposto"). Focused check: `Placeholders.remove` clears `containerMenu` too when it is not the inventory menu (Placeholders.java:121-131), so the non-wearer branch also reaches a chest still open at `CLOSE_MENU` time.
  - `CarryOverGameTests`: both tests are the brief's bodies verbatim (diff 492-531), each on an `add(tests,` line, Javadoc with the "why it fails without the new code" reason (PLAYER_POST never fires for the mock; chest unreachable after close).
  - `docs/compat.md:169`: brief text verbatim (diff 32).
  - Javadoc of `DebugWear` (class, `wear`, `remove`) and `FrameEntry.enter`/`exit`/`leaveFrame` now name `WornState.changed`; grep of `frame/` finds no stale "ricalcola le dimensioni / riga nel log" in those methods.
  - Visibility: `WornState.changed`, `SlotGuard.onMenuClosed` package-private as Global Constraints require; no new compat seam, so no other compat.md entry needed.
  - Test count 98 on four variants and the four log greps (15/24/30/16, all >= 1) are reported with RED/GREEN evidence; not re-run (no doubt raised by the code).
- Deviation from brief, accepted: the brief asks the `WornState` Javadoc to say the signature is "wider than §12.14 (`changed(ServerPlayer, String)`)". Design §12.14 (spec line 649) already gives the five-argument signature since 8fe489a, and the ledger ruling (progress.md:14) confirms it; the implementer explains the extra arguments instead (WornState.java:29-33). Correct; see T3-F2.

### Strengths

- Pure reordering as §12.14 demands: each `refreshDimensions` + `FrameLog.event` pair became one call at the same position; no ordering constraint moved.
- `remove` keeps the frame id read where the old `isPresent()` check was, so placeholders still go before `returnStowed` and the id is captured before `clear`.
- The chest test checks armor slots by identity (`==`), which actually proves "never the armor slots" rather than just "four placeholders".
- Javadoc is precise about when `CLOSE_MENU` fires relative to `removed()` and `containerMenu = inventoryMenu`, which is the fact the whole fix rests on.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

- **T3-F1 (plan-mandated)** DebugWear.java:72-74 (and :45-51). With a read-only registry, `clear` and `update` are `void` and refuse silently after the one-time ERROR (`FrameStateAccessImpl.java:91-99`, `:65-71`), yet `remove` still writes `Frame <id> deleted by debug command ...` and `wear` (when an old record exists) writes `worn by debug command`, though nothing changed in the registry. Only in the already-errored read-only mode, and only a misleading INFO line, but this task introduces those lines. The brief mandates `changed` after `clear` whenever a record existed. Option: after `clear`, call `changed` only if `frames.wornFrameId(player).isEmpty()`, else just `refreshDimensions()`; coordinator's call, since the Global Constraints say read-only is recognized only through `mutate`/`changeWorn` returning false.
- **T3-F2 (plan-mandated)** Brief "Produces" asks the `WornState` Javadoc to call the signature "wider than §12.14 (`changed(ServerPlayer, String)`)" and says "§12.14 si corregge nel Task 24". That is stale: design line 649 and the plan's own "Nomi e chiavi comuni" (plan line 57) already have the five-argument signature, ruling progress.md:14. The implementer correctly did not write the false claim (WornState.java:29-33). No code change; the coordinator should make sure Task 24 does not "re-correct" §12.14.
- **T3-F3** SlotGuard.java:140 is 121 characters, while the report's self-review says "Righe sotto i 120 caratteri". No tool enforces 120 (54 other lines in `src/main/java` exceed it), so cosmetic; rewrap if the convention matters.

### Assessment

**Task quality:** Approved

**Reasoning:** The diff is the reordering and the menu-close cleanup the brief and design §12.14 describe, with ordering constraints intact, correct visibility and two in-world tests that fail without the new listener. Only Minor items remain, two of them plan-level.
