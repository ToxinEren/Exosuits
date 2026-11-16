# Task 17 review (Usura dei pezzi), 56d0703..133406d

### Spec Compliance

- ✅ Spec compliant. Every item of "Produces" and Steps 1-12 has its matching hunk:
  - `DamageRouter.routeMilli`: range check and message, `MAX_MILLI` constant, quotas computed on the milli amount, empty `Split` with `(int) (amountMilli / 1000)` and the carry left alone. `route`-with-carry delegates exactly as written in the brief, `quotas` takes a `long` and the no-carry `route` body is unchanged (DamageRouter.java:285, 336-339, 357-387, 165-201).
  - `DamageClassifierImpl`: the `DAMAGES_HELMET` rule sits after the fall check and before any position test, and the Javadoc gives the order (diff 207-210, 231-233).
  - `RedstoneExoframes`: a `volatile` field next to `rules`, rebuilt in `setConfig` after `config` and `rules`; `damageRouter()` has the requested Javadoc; `SuitDamage.register()` comes right after `SuitRestrictions.register()` with a one-line comment, and the `DamageGameTests` registration comes right after `EnergyGameTests` (diff 114, 136, 147, 174-176, 187).
  - `SuitDamage`: the guards run in the brief's order, `wornFrameId` is checked inside the `try`, the catch logs once per UUID, cleanup runs on `PLAYER_QUIT`, `wear` is copied from the brief, and the Javadoc covers where, why, the guards and what the class does not do (SuitDamage.java:73-87, 94-122).
  - Nine `add(tests,` lines (148 = 139 + 9).
  - Lang keys sit in the right places with the brief's texts. A script compared the ordered key lists: identical in both files, 52 keys.
  - `docs/compat.md` has both edits, with the brief's wording.
  - Checked against design §12.7: the guards, milli rounding and clamp, one non-structural `mutate`, the messages and sound after `mutate`, `markDirty`, and never throwing and never cancelling all match.
- Implementer departure: the line `creative.setGameMode(GameType.CREATIVE)` at DamageGameTests.java:133 is correct and minimal. GameTestsImpl.java:149-165 shows that `MockPlayer` overrides only `isCreative()`, so its abilities follow the server's default game mode. The precedent is EnergyGameTests.java:267-268. See T17-F3 for the plan-text side.
- ⚠️ Cannot verify from diff: none.

Focused checks, each for one named risk:
- **Risk: the creative and spectator branches pass for the wrong reason (the wear stops at `wornFrameId`, not at the guard).** `GameTestKit.worn`/`carry` (GameTestKit.java:758-767) throw when there is no worn record. So `assertUnworn` on `creative` and `spectator` proves the record exists and that the guard stopped the wear. `isSpectator()` is not overridden in `MockPlayer` (GameTestsImpl.java:162-171), so the spectator really is one.
- **Risk: the `classify` contract change breaks another caller.** A grep finds only SuitDamage.java:80 and DamageGameTests.java:181.
- **Risk: `wearAtEmptyStillReachesTheOwner` gets its sync from a condition change, not from `markDirty`.** `PieceCondition.of` puts 179/180 and 178/180 at `INTACT`, so no condition changes. `SuitTick.markDirty` does nothing when there is no record (SuitTick.java:262-267).
- **The `quotas` hunk is cut off mid-function.** Read DamageRouter.java:165-228. The `long` amount goes into `double` arithmetic, there are no `int` casts, and `largestRemainder(double[], long)` is unchanged.
- **Ruling (ledger line 11):** the ERROR line keyed by UUID is allowed.
- **Comment rules:** the added Java lines have no curly apostrophes. Their only non-ASCII character is `§`, which 33 existing source files already use.

### Strengths

- `wear` is a faithful copy of the brief. The guards follow the brief's order exactly, with `!(amount > 0F)` so that NaN also exits, and there is no `isCreative()` anywhere.
- The tests are discriminating, and the RED run proves it. All nine failed for the stated reason with an empty `onArmorHurt`, including the control pairs in `zeroAmountWearsNothing`, `armorBypassingDamageWearsNothing` and `noWearInCreativeOrSpectator`.
- Failure messages carry the actual values (`HELMET 180 / 0, expected 179 / 200`). The premise message prints all three game modes, which is how the 1.20.1-forge problem was spotted at once.
- The `assertWear`/`assertPiece`/`assertUnworn` helpers cut nine six-assertion blocks down without hiding which slot failed.
- `DamageRouter` stays pure Java. The route Javadoc now says which entry point the game uses.

### Issues

#### Critical (Must Fix)
None.

#### Important (Should Fix)
None.

#### Minor (Nice to Have)

- **T17-F1 (plan-mandated): on a hit that both breaks one piece and sends a later slot to critical, the "broken" action-bar line is overwritten at once.**
  - Where: SuitDamage.java:109-118, copied from the brief.
  - What happens: the loop goes in `PieceSlot` order and sends each message with `displayClientMessage(..., true)`, and a later action-bar message replaces the earlier one in the same tick. In the case `aBreakingPieceLowersTheArmorAndTells` exercises (helmet breaks, torso goes critical), the player sees only "Critical piece: Torso". "Broken piece: Helmet" never shows.
  - Why it is Minor: the `ITEM_BREAK` sound still plays, and the Task 22 energy line will show the worst piece.
  - Fix: send the critical messages in a first pass and the broken ones in a second, so a broken line is always the one that stays.
- **T17-F2 (plan-mandated): `routeMilliMatchesRouteOnWholePoints` holds by construction.**
  - Where: DamageRouterTest.java:275-295.
  - Why: `route` with carry is now literally `routeMilli(kind, n * 1000L, ...)`, so with identical starting carries the two sides cannot differ.
  - It would not catch a reintroduced separate computation either. The brief itself says two separate computations diverge only "da qualche migliaio di punti in su", and the loop stops at n = 1200.
  - What it does guard is the x1000 factor of the delegation, which the 14 existing carry tests already pin.
  - It costs about 600k router calls per variant build.
  - Possible fixes: accept it as documentation of the contract, or replace it with an `n` range that reaches the divergence zone, compared against the pre-change algorithm kept in the test.
- **T17-F3 (plan-mandated): the brief and the Global Constraints say the mock player is creative, but on 1.20.1-forge it is not.**
  - Brief Step 5 says `mockServerPlayer(helper)` has "capacità `instabuild` vere", and Global Constraints says "il giocatore finto nasce in creativa".
  - On the 1.20.1-forge test server this is false for the abilities. `MockPlayer` only overrides `isCreative()` (GameTestsImpl.java:162-165), and its abilities come from `placeNewPlayer` and the server's default game mode.
  - The implementer's one-line fix at DamageGameTests.java:133 is right.
  - The plan sentence stays misleading for later tasks that rely on a creative mock player (`instabuild`/`invulnerable`) without calling `setGameMode(GameType.CREATIVE)`. The coordinator should accept the departure and correct the plan text, or add the line to `mockServerPlayer` in a task that owns `compat/GameTests`.

### Assessment

**Task quality:** Approved

**Reasoning:** The implementation matches the brief and design §12.7 line by line, with discriminating in-world tests that are green on all four variants. The three Minor findings all come from the plan text, not from the implementer's choices.
