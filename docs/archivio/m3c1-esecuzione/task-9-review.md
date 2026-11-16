# Task 9 review (FrameSounds e i suoni delle azioni della 3b)

BASE 5389f2b2e7e3490eb685c295689c720489efe64c, HEAD 81238da509da549e6227e9eca37d55827a808db1 (working tree checked at HEAD, clean). Diff read once from `.superpowers/sdd/2026-11-15-m3c1-consumare/review-5389f2b..81238da.diff`.

### Spec Compliance

- ✅ Spec compliant.
  - `FrameSounds` (`src/main/java/com/toxineren/exoframes/frame/FrameSounds.java`): the nine enum constants with the event and source of the brief and of design §12.13 (lines 36-45); `play` = `level.playSound(null, at.x, at.y, at.z, event, source, 1.0F, 1.0F)` then the recorder loop (73-83, `level.playSound` at 77); `record(ServerLevel, AABB)` (90-96); `Recording extends AutoCloseable` with `sounds()` and `@Override void close()` without `throws`; `RECORDINGS` is a `CopyOnWriteArrayList` (33); `Rec` is a private final class, not a record, `sounds()` returns `List.copyOf` (114), `close()` removes this instance (119). Javadoc has the table, the `null` and why, and the test-only recorder.
  - `FramePlacement.place`: `PLACE` at `feet` after `spawnView` (FramePlacement.java:57), `spawnView` itself silent. Named risk "another production path calls `place` and now sounds": grep of `FramePlacement.place(` finds only `content/ExoframeItem.java:57` and `command/ExoframeCommand.java:295` (the item and `/exoframe debug place`, both wanted) plus existing `ExoframeGameTests` bodies; exit, death and realignment use `spawnView`, not `place`.
  - `FrameInteractions`: helper at 309-311 plays at `frame.position()` in `player.serverLevel()`; `MOUNT` after a successful `mountPiece` (176) and `insertCell` (206); `UNMOUNT` after stowed armor handed back with a non-empty list (226-233), after the cell removed (248), after `unmountPiece` (283); pickup of the empty frame (250-258) silent; every refusal returns before the sound. Grep of every `InteractionResult.SUCCESS` in the file confirms no success branch is left without its sound or with an unwanted one.
  - `FrameEntry.enter`: `ENTER` at `spot` as the last step after `WornState.changed` (FrameEntry.java:157), all refusals return earlier. `FrameEntry.exit`: `EXIT` at `player.position()` after `stepOut`, only when `leaveFrame` succeeded (203-208); the `FRAME_TAKEN` refusal returns before it. `onDeath`: no sound (Javadoc says so).
  - Tests (`gametest/CarryOverGameTests.java`): three `add(tests, ...)` rows (99-102); `placingPlaysTheMetalSound` (595), `mountAndUnmountPlayTrapdoorSounds` (616), `enterAndExitPlayPistonSoundsAndArmorStaysSilent` (661) follow the brief step by step: same coordinates, same click heights, same expected lists, `player.doTick()` before the helmet, recorder and `countEquipEvents` opened in one try-with-resources, `equips.count() == 0` (688), `takeStowedOfPlaced(...).size() == 1` before `removeFrame` (690-692). Each recorder uses `structureBox(helper)`, which is the exact structure box (GameTestKit.java:753-758), so parallel tests do not mix.
  - `docs/compat.md`: the row is the brief's text verbatim, inside "Tabella API vanilla/loader → interfaccia" (heading at line 179, row at 219).
  - Commit message matches Step 8; commit carries a `gpgsig` header like BASE; trailers present.
  - Count 114 = 111 (Task 8) + 3, as in the plan's table; report shows four `OK <variante>: 114` lines and the RED compile failure on `FrameSounds`.
- ⚠️ Cannot verify from diff: nothing. (That the sounds are audible in a real client is left by the brief to the 3c-2 PC checklist.)

### Strengths

- Every sound sits after the registry's successful outcome and before `return SUCCESS`/`true`; the two refactors needed for that (`FrameInteractions.unmount` stowed branch at 226-233 and `FrameEntry.exit` at 203-208) keep the old behaviour exactly (empty stowed list still `CONSUME` with nothing handed out; failed `leaveFrame` still `false`).
- The tests check the premises they depend on (empty frame really picked up, boots really returned, entity back after exit, record placed at death), so a failure says where.
- Javadoc on `place`, `enter`, `exit`, `onDeath` and the class Javadoc of `FrameInteractions` document the sound rule at the point where it applies; ASCII apostrophes throughout (no non-ASCII apostrophes or accented letters in the added Java lines).

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

- T9-F1. `gametest/CarryOverGameTests.java:595-692`: "mai su un rifiuto" is proven in a test for the place refusal and the occupied-slot mount refusal, not for a refused exit (`FRAME_TAKEN`, FrameEntry.java:196-199) or a refused enter. Both are correct by reading (the early returns precede the `play` calls), so this is coverage only; a recorder around the refused exit in `exitRefusedOverAnotherFrame` would pin it if a later task reorders `exit`. Not required by the brief.

### Assessment

**Task quality:** Approved

**Reasoning:** The diff implements exactly the brief's `FrameSounds` API, sound points and three in-world tests, with sounds only after successful registry outcomes and no extra sound on `spawnView`, pickup or death; the only note is optional extra coverage for refused exit/enter.
