# Task 19 review (Acqua e scatto) - 930439e..0f469e2

### Spec Compliance

- ✅ Spec compliant. Checked file by file against the brief:
  - `frame/SuitMovement.java`: `WATER_HINT` public (:~40); `affectedByFluids` false only when suited, `isInWater()` and `!isInLava()` (:46); `pushedByFluid` false whenever suited, so lava currents too (:~60); `afterUpdateSwimming` clears swimming when suited (:~68); `sprintAllowed` false only when suited and `clientIndicators` present with `canSprint` false (:~78-83); "suited" = `SuitSize.wearsSuit(player) && !player.isSpectator()`, `wearsSuit` first (:86). These match design §12.6 (spec lines 504, 508). Class Javadoc covers each point the brief lists (player rule, both sides agree without a packet via the torso placeholder, step and water independent of energy and pieces, `canSwim` not read, sprint allowed without indicators, real ban on the client and server only for its own accounts in step 7, spectators keep vanilla rules).
  - `frame/SuitTick.java`: record field `boolean waterHintShown` (:313); step 9 is the brief's line verbatim, after step 8 and inside the same `try` (:217-220). Checked the whole `tick` body: once a record exists there is no early return, so step 9 always runs for a worn frame. `tick` Javadoc now says steps 1-9, and the class Javadoc has a hint paragraph.
  - `RedstoneExoframes.init()`: `register(MovementGameTests.class)` right after `DamageGameTests`.
  - `gametest/MovementGameTests.java`: public no-argument constructor, `@GameTestGenerator generate()`, `import static GameTestKit.*`, exactly four `add(tests,` lines, pool/suit/control set up as in the brief (suit enters on the dry floor before `fillPool`; control at relative (2.5, 2.0, 2.5)), `leaveTestFrame` cleanup, direct ticks at `now(helper) + k` with k >= 1, and `addEffect` (fire resistance) then `clearFire` on both players in the lava test. The class Javadoc has the spectator and Task 23 sprint notes.
  - Lang: `water_hint` sits between `leave_frame_first` and `frame_taken` in both files, and its English and Italian texts match the brief exactly.
  - `docs/compat.md:173`: the stale clause ("per ora lasciano il valore del gioco: ... non fa nulla, ...") is replaced with the brief's sentence on `exoframes$fluidMovement` / `fluidPush` / `noSwimming` / `sprintGate`.
  - Commit subject matches the brief. The body has the Step 5 numbers for both variants.
- Wiring check (focused, outside the diff): risk = a handler not routed to the new rules. `mixin/PlayerSuitMixin.java:35,40,45` call `affectedByFluids`, `pushedByFluid` and `afterUpdateSwimming`. `mixin/client/LocalPlayerSprintMixin.java:24` calls `sprintAllowed`. `compat/FrameStateAccess.java:102` declares `clientIndicators(Player)`.
- Record-discard check (focused): risk = the hint not coming back after an exit. `frame/WornState.java:45` calls `SuitTick.forget` when the player no longer wears a frame, so `leaveTestFrame` drops the record, as the test needs.
- Step 5 evidence check: I read `/tmp/m3c-acqua-1.20.1-forge/rcon.log` and `/tmp/m3c-acqua-1.21.1-fabric/rcon.log`. With the suit: Y -60.0 after the 1.5 s sink and after 3 s of Space; Z 0.5 -> 7.7187 in 2 s of W; X 42.5 -> 42.5 in the current; bank X 27.884, Y -60.0. Without the suit: Y -57.21 / -57.49 with Space; Z +3.61 / +3.51; X 45.97 / 46.02 in the current; bank X 24.70, Y -61.0. `log-check.txt` is 0 bytes on both and `moved wrongly` appears 0 times in both server logs. All thresholds in the brief are met, and the report table matches the logs.
- The sprint ban below 10 % on the real client is not tried here: the brief gives it to Task 23 Step 5 (the server has no indicators). The logic (`SuitMovement.java:~78-83`) and its wiring (see above) are checked.

### Strengths

- `suited()` keeps the "with the suit" condition in one place, with `wearsSuit` first as the brief asks, so a player still being built never reaches `isSpectator()`.
- Each test checks its starting condition before it checks anything else, for both players. The control player shows that each threshold measures the suit, not the scene. The lava test's `!p.isPushedByFluid()` also proves the suit is recognised, so the lava `isAffectedByFluids()` true cannot pass just because `wearsSuit` came out false.
- `waterHintOncePerEntry` fails both ways a regression could go: a hint sent on every tick gives 3 in the first entry, and a hint flag that survives an exit gives 1 after the second entry.
- RED was shown for the right reasons (four failures with the messages the brief expects). GREEN is 156 = 152 + 4 on all four variants.
- The implementer found the brief's `isUnderWater()` premise wrong, explained it, and documented it in the `settle` Javadoc instead of weakening the assertion.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

- **T19-F1 (plan-mandated): the brief's "one `doTick()`" cannot meet the brief's own starting condition; the code correctly uses two.** `gametest/MovementGameTests.java:164-171` (`settle`). Vanilla `Entity.updateFluidOnEyes` sets `wasEyeInWater` from the *previous* tick's `fluidOnEyes` before it recomputes them, and `isUnderWater()` = `wasEyeInWater && isInWater()`. The mock player never ran `baseTick` before (the level only calls `ServerPlayer.tick()`), so after one `doTick` `isUnderWater()` is false. The implementer's first RED showed exactly that (`inWater true, underWater false`).
  - Consequence in `waterHintOncePerEntry` (`:131-146`): the first hint is sent by the `PLAYER_PRE` path inside the second `doTick`. The two direct `SuitTick.tick` calls then test that it does not repeat. The counts stay 1 and 2, and the test is no weaker.
  - No code change is needed. The coordinator should record a ruling that the brief's Step 1 sentence ("un `doTick()` a ciascuno") is superseded by two.
- **T19-F2: the commit body says "vasca profonda 4"; the pool is 3 deep.** Commit `0f469e2`, body line 3. The command in the brief fills water at y -60..-58 (`fill 10 -60 -2 14 -58 12 water`). The numbers in the body are right. Amending is forbidden, so note it in the ledger or the final report.
- **T19-F3: `docs/compat.md:181` (the "Verifica" bullet) still records only the Task 10 run, with "gestori ancora vuoti".** It does not say that the water rule was tried on the real client in Task 19 Step 5 (1.20.1-forge, 1.21.1-fabric: no `moved wrongly`, clean log check). The brief's Step 6 did not ask for this, so it is not a spec gap. A reader of the mixin section, though, now finds rules in the handlers but only an empty-handler verification. Fix: one clause in the Verifica bullet naming the Step 5 run, or leave it to the final review.

### Assessment

**Task quality:** Approved

**Reasoning:** The four rules, step 9 and the record flag match the brief and design §12.6 exactly. The in-world tests check real behaviour against a control, with their starting conditions asserted, and the real-client numbers in the logs meet every threshold. The only departure from the brief (two `doTick`) is what the brief's own assertion needs, and it is documented.
