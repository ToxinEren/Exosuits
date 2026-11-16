### Spec Compliance

- ✅ Spec compliant. Each item of the brief has its hunk:
  - `SuitDamage.register()` adds `EntityEvent.LIVING_HURT.register(SuitDamage::onLivingHurt)` (SuitDamage.java:120); the handler is private (:130).
  - The guards follow the brief's order (:131-138): `ServerPlayer`, `!isDeadOrDying()`, `!invulnerable`, `amount > 0F` (written as `!(amount > 0F)`, so NaN also passes, as in `onArmorHurt`), `!instabuild`, `!isSpectator()`, `isFall`, `wornFrameId` present. Then `onFall` + `interruptFalse()` (:143-144). There is no `isCreative()`.
  - `public static void onFall(ServerPlayer, float)` wraps `wear(player, DamageKind.FALL, amount)` in `try/catch (RuntimeException)` and writes the same once-per-player ERROR line (:153-159, through `logFailure` :183-187).
  - The class Javadoc (:87-97) covers everything Step 3 lists: the declared exception to §5.7, no cancel without a worn record and so none in read-only mode, the sounds and the statistic that remain, NeoForge's `DamageContainer`, and no invulnerability-window accounting.
  - The `init()` comment names the fall (RedstoneExoframes.java:29).
  - There are four new `add(tests, ...)` lines at the end of `generate()` (DamageGameTests.java:250-253). Their bodies use the brief's exact setup and numbers.
  - No new text keys and no `docs/compat.md` line, as the brief says.
  - The test count is 148 + 4 = 152 on all four variants (report), which matches the plan's table.
- Checks made outside the diff, one per named risk:
  - Risk: a read-only registry still holds worn records, so the listener would cancel the fall without wearing anything (design §12.8 guard "registro scrivibile"). `FrameStateAccessImpl.wornFrameId` (:248) reads `FrameRegistry.worn` (:108), which has no read-only check. But `FrameStateSavedData.load` (:358-366) keeps an empty registry for READ_ONLY and WITHOUT_SECTIONS, because `FrameRegistryFormat.read` reads nothing in those cases. The unreadable-file path returns the `empty` data (:216, :235). So `wornFrameId` is empty whenever the registry is read-only. The brief's claim holds.
  - Risk: the report says the core already does the fall split. `DamageRouter.quotas` (DamageRouter.java:165-199) skips missing and broken pieces. `primaryShare` goes to the intact legs, split evenly between them. Everything goes to the legs when no other piece is intact, and everything goes to the others when no leg is intact. A bare frame gives `null`, so nothing is worn. This matches the brief and design §12.8, and no core change is needed.
  - Risk: the listener cancels non-fall damage. The Task 17 cactus tests assert `p.hurt(cactus, 8.0F)` is true (for example DamageGameTests.java:263) and pass at 152, so non-fall damage still goes through.
  - `FEET` is `new Vec3(3.5, 2.0, 3.5)` (DamageGameTests.java:41), which is exactly the brief's spot for `fallOnABareFrameWearsNothing`.
  - `GameTestKit` has no `changeWorn` helper (only `setStored` calls `Compat.frameState().changeWorn`, GameTestKit.java:745). So the brief's `changeWorn(p, ...)` is correctly read as `Compat.frameState().changeWorn(p, ...)` with its result asserted (DamageGameTests.java:339-343).
- Arithmetic checked:
  - Fall of 7 000 millesimi with the legs as primaries: 2 100 per leg, so 178 / 100. The other 2 800 split on shares out of 0.70: helmet 600 (180 / 600), torso 1 200 (179 / 200), arms 500 each (180 / 500). The test asserts exactly this (:287).
  - Pearl of 5 000: 1 500 per leg (179 / 500); torso 857, so 180.
  - Broken legs, 7 000 split on 0.70: helmet 1 500 (179 / 500), torso 3 000 (177 / 0), arms 1 250 (179 / 250).
- The report's one deviation (Step 2: the pearl test fails on `!p.hurt(...)` instead of on health 15.0) comes from the brief's own test order, which puts that assertion first. The test is right; the brief's expected RED line was off. No impact.
- ⚠️ Cannot verify from diff: none.

### Strengths

- The guard chain is a literal transcription of the brief's order, and the cancel happens only after a worn record is confirmed (SuitDamage.java:131-144). The pilot stays protected even when `wear` throws, because `onFall` swallows the exception and the handler still returns `interruptFalse()`. That is the "effetto di base del telaio" the design asks for.
- The handler wraps the two guards that call into `Compat` in a `try` (:135-142), so `wornFrameId` is protected the same way it is in `onArmorHurt`. This keeps design §12.7's "never throws in the game's damage code" for the new entry point. On failure the fall goes through as vanilla, with the same single ERROR line.
- `logFailure` (:183-187) removes what would have been three copies of the ERROR block. The line and its once-only mark are unchanged.
- `fallWearsTheLegsAndNeverHurts` includes the frameless control player, who loses exactly 7.0 (:288-293). This shows the 20.0 of the suited pilot comes from the listener and not from some other protection.
- `assertHealth` (:380-383) uses exact float equality with a message that names the player and the value, which gives readable RED lines (as the report's RED output shows).
- No new non-ASCII characters other than `§`, and every added line is at most 120 characters.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

- **T18-F1** DamageGameTests.java:30-37: the class Javadoc still says "Tutti con un giocatore in sopravvivenza ... dentro sei pezzi Rottame nuovi" and "Il danno viene dal cactus ... quindi GENERIC". After this task, `fallOnABareFrameWearsNothing` uses a frame with no pieces, and the four new tests use fall damage (`DamageKind.FALL`, legs as primaries), not cactus. The summary was already loose before this task (the Task 17 anvil, lava and creative tests), and this task widens the gap. Fix: say that the cactus, the 180-durability Scrap pieces and survival mode are the usual setup, not a rule for every test, and add one clause saying the fall tests use `fall()` with the legs as primaries.

### Assessment

**Task quality:** Approved

**Reasoning:** The listener, `onFall` and the four in-world tests do what the brief and design §12.8 ask, with the guard order kept and every expected number checked by hand. The two unchanged-code assumptions (read-only registry gives an empty `wornFrameId`, and the router already splits fall damage the right way) both hold. The only issue is a stale sentence in a test-class Javadoc.
