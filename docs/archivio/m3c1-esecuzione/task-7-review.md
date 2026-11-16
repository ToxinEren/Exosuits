### Spec Compliance

- ✅ Spec compliant. Every Produces item and step of the brief is in the diff, file by file:
  - `FrameEntry.java:67` `public static final String FRAME_TAKEN`; `:80` `private static final double MAX_STEP_UP = 0.5`.
  - `exit` (`FrameEntry.java:186-203`): `restingSpot(player)` is computed right after the guards and before `Placeholders.removeAll`; `getEntitiesOfClass(ExoframeEntity.class, SuitSize.standingBox(spot.position()))` not empty → `FrameInteractions.tell(player, FRAME_TAKEN)`, `return false`, nothing else; then `removeAll`, `returnStowed`, `leaveFrame(player, frames, "left", spot)`, `stepOut`. `onDeath` passes `restingSpot(player)` with no frame check.
  - `leaveFrame(ServerPlayer, FrameStateAccess, String, RestingSpot)` (`:306`): the caller supplies the spot.
  - `standingSpot` (`:403-424`): `top = candidate + MAX_STEP_UP`, vanilla box at `top`, `depth = MAX_STEP_UP + 1 + FLOOR_EPSILON`, the same `Shapes.collide` call, `drop <= -depth` rejected, `feet`/`standing`, then the rejections in the brief's order (noCollision, insideBorder, `hazard` over `standing.expandTowards(0, -0.5, 0)`, other `ExoframeEntity` in `standing`, `AbstractMinecart` in `standing.expandTowards(0, -1, 0)`).
  - `hazard` (`:431-435`) is the brief's body verbatim. `wayIsFree(player, level, candidate, to)` (`:376-384`) and `segment` (`:387-390`) match the brief's formula term for term; `stepOut` passes `target.get()`.
  - Javadoc of `exit`, `stepOut`, `standingSpot`, `wayIsFree` (plus `onDeath`, `leaveFrame`, `hazard`) rewritten with the step up to 0.5, the axis paths, the named hazards, boat and cart, the refusal over another frame. Italian, ASCII apostrophes.
  - `GameTestKit.java:290` `static void assertExitStepsLeft(...)` does what the brief says (re-enter if not wearing, yaw 0, `exit` true, distance to `spot + (1, 0, 0)` < 1e-6, message `"player stepped onto " + hazard + ": " + position`).
  - `CarryOverGameTests`: eight `add(tests,` lines, with bodies that follow the brief's layouts (`:329`, `:364`, `:398`, `:427`, `:461`, `:491`, `:515`, `:541`). The asserts in `exitRefusedOverAnotherFrame` are the brief's own text.
  - Lang: `message.redstone_exoframes.frame_taken` added after `exit_hold`, at the end of both files, with the exact en_us/it_it texts. `docs/compat.md` is not touched, as the brief says.
  - Commit subject matches Step 6; trailers are present.
- Design §12.14 check (`docs/superpowers/specs/2026-11-13-m3-telaio-design.md:652`): "lo stesso controllo dell'oggetto telaio". Named risk: the exit check differs from the item's check. I compared it with `FramePlacement.java:44-46`, which uses the same `getEntitiesOfClass(ExoframeEntity.class, SuitSize.standingBox(feet))`, so the two checks are identical.
- Named risk: moving `restingSpot` ahead of `Placeholders.removeAll`/`returnStowed` changes where the frame goes. I read `FrameEntry.java:449-474`: it reads only `player.position()`, `SafeSpots.last`, the server and the levels, never the armor slots or the record. The reorder changes no behaviour.
- Named risk: some forced move goes through `exit` and can now be refused, which §12.14 forbids ("spostamenti forzati invariati"). I grepped every caller of `FrameEntry.exit(`. The only production caller is the C2S packet handler (`FrameEntry.java:232-234`, a voluntary exit); death uses `onDeath`, which never refuses. The rule holds.

### Strengths

- The algorithm is transcribed exactly and is sound. I traced it through the edge cases. `Shapes.collide` ignores shapes the start box already overlaps, so a wall or a slab higher than 0.5 is not taken as a floor, and the `noCollision(player, standing)` that follows rejects it. A bottom slab gives `drop = 0` (feet at +0.5, test case B). The way boxes raise `bottom` to `to.y`, so the step itself does not close the path. Together, the standing box and the way boxes still cover the descent that the old candidate-height `noCollision` used to cover.
- The refusal over another frame happens before anything changes (`FrameEntry.java:192-196`), so nothing is lost or duplicated. `exitRefusedOverAnotherFrame` proves this: the worn record, the placeholders, the stowed helmet and the position are all unchanged, the message comes once, and after the other frame goes the exit succeeds and the helmet comes back.
- Each sequential case can actually fail. Without its hazard, every spot (campfire, magma, berry bush, wither rose, powder snow, cactus, end portal, nether portal) would be accepted under the old rule, so no case passes by accident. The tests assert their premises: the fence's collision is only the post, with tolerance (`CarryOverGameTests.java:374-377`); the end portal has an empty collision shape (`:473-475`); and `assertRoomToStand` runs before the boat and cart exist. That last check matters because `assertRoomToStand` uses `noCollision(null, …)` (`GameTestKit.java:250-254`), where the boat counts as an obstacle.
- Transient blocks are cleaned up in the same tick: the cactus, plus the sand under it as a harmless extra; both portals; the boat and the cart are discarded. Every frame is removed before `succeed()`.
- The TDD evidence is credible. With only the constant in place, all 8 tests fail for the predicted reasons: the post case went right, the path case stayed in place. GREEN reports 110 on all four variants (= 102 + 8, matching the Global Constraints table).
- Hygiene: no line over 120 columns in the three Java files, and `git diff --check` is clean. No `isCreative()`. English identifiers and assert messages.

### Issues

#### Critical (Must Fix)
None.

#### Important (Should Fix)
None.

#### Minor (Nice to Have)
- [plan-mandated] `FrameEntry.java:420` / `CarryOverGameTests.java:526`: no test exercises the downward extension `standing.expandTowards(0, -1.0, 0)` of the cart search. The brief's test puts the cart at `pit.y + 0.0625`, inside `standing` itself (feet 1.0, box 1.0..2.8). A regression to a search over `standing` alone would therefore still pass `exitAvoidsAMinecartBelowTheSpot`. The brief prescribes both the rule and this exact test, so this is coverage that could be broader, not an implementer defect. A cart in a cell below the feet, with no block-collision floor between them, would pin the `-1.0`.

### Assessment

**Task quality:** Approved

**Reasoning:** The diff follows the brief line for line: `exit` refuses before touching any state, the step-up/axis-path/hazard/boat/cart rules are in the brief's order, and the eight tests can each fail with their premises asserted. The only gap is untested coverage of a defensive branch that the plan itself prescribes.
