# Task 6 review (88e3614..e0593d7)

### Spec Compliance

- ✅ Spec compliant. Checked against the brief, Global Constraints, and design §12.14 (line 651: "contorni dei blocchi (non forme di collisione), e con gli occhi dentro il riquadro di un telaio vince quel telaio"):
  - Two commits, the right order and messages: `5fc2824 frame: rimozione e linea di vista dei comandi in frame/`, then `e0593d7 frame: i comandi mirano come il clic`. Focused check (risk: commit 1 not a pure move): `git show 5fc2824` touches only ExoframeCommand, FrameRemoval, FrameSight; `FrameSight.java@5fc2824:32` still uses `ClipContext.Block.COLLIDER` and has no eyes rule; `git diff 5fc2824 e0593d7 --stat` touches only FrameSight and CarryOverGameTests.
  - `FrameSight` (frame/FrameSight.java:400-438): `public static final double SIGHT = 5.0`, `public static Optional<ExoframeEntity> inSight(ServerPlayer)`; rule 1 (:420-426) is `getEntitiesOfClass(ExoframeEntity.class, new AABB(eyes, eyes).inflate(1.0e-7), f -> !f.isRemoved() && f.getBoundingBox().contains(eyes))`, then `min` on the distance from the box centre, with no block check; rule 2 (:427-437) is the old search with `Block.OUTLINE`, `Fluid.NONE` and the same `getEntityHitResult`. The Javadoc (:407-418) gives both rules and why.
  - `FrameRemoval` (frame/FrameRemoval.java:306-374): `public record Result(boolean removed, List<PieceSlot> deleted)` with `List.copyOf` in its compact constructor (:314-318). `remove` keeps the old steps in the old order: pieceStack, then on IAE the slot goes in `deleted`; updatePlaced(unmount), then dropStill; the cell with fromCell, then updatePlaced(removeCell), then dropStill; takeStowedOfPlaced, then dropStill; removePlaced; discard; `FrameLog.event(frameId, "removed", by, (ServerLevel) level, feet)`. The outcomes are as the brief says: no record gives `Result(false, List.of())` (:334-336), a mid-way refusal gives `Result(false, deleted)` (:353-355, :360-362, :368-370), and the end gives `Result(true, deleted)` (:373).
  - Command (command/ExoframeCommand.java diff :173-244): the brief's code. The tiers go into `new EnumMap<>(PieceSlot.class)` with `forEach` before `remove` (never `new EnumMap<>(Map)`). `remove.done` and `remove.done_deleted` are both sent with `sendSuccess(..., true)`. The WARN text is unchanged. `setUnlocked` calls `FrameSight.inSight` (:139). `SIGHT` and `frameInSight` are gone. The class Javadoc links `FrameSight#SIGHT`, `FrameSight#inSight` and `FrameRemoval#remove` (:80, :92). Focused check (risk: an import left dangling or wrongly removed): `CellItem` (:291), `FrameLog` (:300), `ItemStack` (:288) and `ServerLevel` (:281) are still used in `place`.
  - Tests (gametest/CarryOverGameTests.java): two `add(tests, ...)` lines (diff :514-515). `tallGrassHidesTheFrameFromTheCommand` (:543-575) has the dirt, the two halves, the premise on collision and outline (plus a `TALL_GRASS` check), the brief's three assertions, removal upper half first, then unlock and `removeFrame`. `eyesInsideAFrameBoxPickThatFrame` (:583-609) creates the frames in the order a1, b1, b2, a2 (inverted between the two pairs), checks the eyes premise and the brief's two assertions, then calls `removeFrame` on all four. B owned by `owner` makes `!recordState(b).unlocked()` meaningful (report, Scelte).
  - Visibility: `FrameSight`, `FrameRemoval`, `Result`, `inSight` and `remove` are public. No text keys and no `docs/compat.md` line, as the brief says. No `isCreative()` and no Stonecutter conditions. Javadoc and comments are in Italian with ASCII apostrophes; test messages are in English.
  - Evidence (report): RED on 1.21.1-neoforge with "unlocked a frame behind tall grass" (and the eyes test, which the brief allowed). Step 2: 100 per variant. Step 7: build plus collectJar BUILD SUCCESSFUL, OK sources, OK jars, and `OK <variante>: 102 test obbligatori passati, nessun avviso fuori elenco` on all four variants. The count matches the Global Constraints table (102 after Task 6).
  - Focused check (risk: the new tests miss a wait for entities that the existing pattern needs): `ExoframeGameTests.wallHidesTheFrameFromTheCommand` (ExoframeGameTests.java:2731-2751) is synchronous in the same way, and the new tests follow it.
  - Commits carry the same trailers and signing state as the base commit (`%G?` = N on base and head alike).

### Strengths

- The move is genuinely pure, and that is verified: commit 1 keeps COLLIDER, and the behaviour change is isolated in commit 2 with its RED/GREEN pair.
- Reading the tiers before `FrameRemoval.remove` (ExoframeCommand diff :219-222) keeps the message and the WARN identical, and it does not depend on whether `placed()` returns a live state or a copy.
- The tall-grass test guards against passing for the wrong reason: the block identity, an empty collision shape and a non-empty outline are all checked before the assertions (CarryOverGameTests.java diff :554-561).
- The `Result` record copies `deleted` defensively, and every early return of `remove` keeps the "already on the ground stays there" contract that the Javadoc states (FrameRemoval.java diff :320-329).

### Issues

#### Critical (Must Fix)
None.

#### Important (Should Fix)
None.

#### Minor (Nice to Have)
- **T6-F1** src/main/java/com/toxineren/exoframes/gametest/CarryOverGameTests.java:232-233: there is no blank line between the closing brace of `registryFileWithoutSectionsIsReadOnly` and the Javadoc of `tallGrassHidesTheFrameFromTheCommand`. Every other method in the file has one. Fix: add the blank line.
- **T6-F2** src/main/java/com/toxineren/exoframes/frame/FrameSight.java:423-426: no test covers the tie-break of rule 1, where the frame whose box centre is closest to the eyes wins when several boxes contain them. Both pairs in `eyesInsideAFrameBoxPickThatFrame` have exactly one frame around the eyes. The case is real: design §11 lists two frames left one inside the other. The brief did not ask for this test, so it is a gap in coverage only. Fix (optional): add a pair of overlapping frames with offset centres and assert that the one nearer the eyes is picked.

### Assessment

**Task quality:** Approved

**Reasoning:** The two commits do exactly what the brief asks: a verified pure move, then the OUTLINE and eyes-inside rules from design §12.14, with tests that fail for the right reason and premises against false passes. The only findings are a missing blank line and one untested tie-break.
