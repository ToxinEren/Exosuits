# Task 16 review (ff75a4c..56d0703)

### Spec Compliance

- ✅ Spec compliant. Every file the brief lists has its hunk, and every "Produces" item is in the diff:
  - `FrameAttributesImpl.set` is idempotent in common code (`FrameAttributesImpl.java:98-104`: `getModifier(key(id))`, `same` -> return, otherwise `removeModifier` only when one is present, then `addTransientModifier`). It never calls `addOrUpdateTransientModifier` or `equals`. `same` sits in both branches next to `key`/`modifier` (`:128`, `:175`), with `==` on the amount and on the version's operation enum. `clear` is unchanged apart from `clearStep(e)`.
  - The step on 1.21.1: `STEP = Ids.of("frame_step")` and `STEP_BONUS = 0.4` are common constants. `applyStep`/`clearStep` call `set`/`remove` on `Attributes.STEP_HEIGHT` and run at the end of `apply`/`clear`. On 1.20.1 both methods are empty and carry the comment, so `key(STEP)` is never reached.
  - `localStep` (`:72-86`): the whole body is inside `//? if <1.21`. Its first line is the `isLocalPlayer()` guard. When wearing and `< 1.0F` it sets 1.0 and adds to `RAISED`. When not wearing, `RAISED.remove(player) && maxUpStep() == 1.0F` sets 0.6, and `remove` runs first, so the player always leaves the set. `RAISED` is a `MapMaker().weakKeys()` identity set and exists only in the 1.20.1 branch, with its imports in the `<1.21` block.
  - `SuitSize.tick` (`SuitSize.java:100-102`) calls `localStep` after the conditional `refreshDimensions`, behind `isLocalPlayer()`. It passes `now` rather than calling `wearsSuit(player)` again. This is the same value in the same tick, because `refreshDimensions` does not touch equipment.
  - `GameTests`/`GameTestsImpl`: all four helpers have Javadoc that says "Esiste solo per i test". `attributesToSync` reads the live set in each version's branch, collects ids as the brief says, then calls `clear()` on that same set. `stepIsAttribute` uses the multi-line form. `frameModifier` skips null instances and, on 1.20.1, derives the UUID itself, without `key`. `respawn` has both branches with the right `RemovalReason`. One import moved from the brief's list: `Attribute` is now common because the 1.21.1 loop is typed `Holder<Attribute>` (the repository does not use `var`). The move is needed, and the import is used on both versions.
  - `EnergyGameTests`: four `add(tests, ...)` lines at the end of `generate()`, plus `MOD_ATTRIBUTES` and `modAttributesToSync` copied verbatim. All four tests follow the brief step by step: the preparation tick, the same-effects -> LOW -> EMPTY sequence with values 15 / 8 / -0.745, the `respawn` try/finally with `leaveTestFrame(fresh)`, and the stone floor (2..4, 1, 2..4) before `FrameEntry.exit` with the five ids checked. The extra `ATTRIBUTE_FEET` constant is the brief's `(3.5, 2.0, 3.5)`. I checked its Javadoc claim: `GameTestKit.wearFrameAt` (`GameTestKit.java:200`) places stone at `containing(feet).below()` = (3, 1, 3), so the claim is correct.
  - Javadoc of `FrameAttributes` (class, `apply`, new `localStep`): idempotence, five modifiers on 1.21.1, SuitTick applies at the first evaluation and when effects change, never `hurtArmor`, WornState clears. The respawn-listener sentence is gone. I grepped `src/main/java` and `docs/compat.md` for leftover "chiamera'" text and found none.
  - `docs/compat.md`: the FrameAttributes section ("A cosa serve", "Differenze chiuse", "Condizione") and the GameTests section ("A cosa serve" with one line per helper, "Condizione" with the four helpers per version) match Step 8. The Global Constraints rule that a new seam is documented in the same task is met.
  - Commit `56d0703`: the subject is exact, and the body carries the Step 7 Y values for both 1.20.1 variants (`git show -s`). Count 139 = 135 + 4, matching the plan table.
  - The diff adds no `isCreative()` call. Assertion messages are in English. Every added Java line uses ASCII apostrophes; the only non-ASCII character is `§` in `design §12.5`, a form the file already uses. There are no Stonecutter directives outside `compat/impl/`.
- ⚠️ Cannot verify from diff: none. Real-client Step 7 numbers are taken from the report and the commit body, as the brief intends.

### Strengths

- `set` stays in common code with a single small `same` per version, as the plan asked. The 1.20.1 comment explains why `equals` is not used (`FrameAttributesImpl.java:174`).
- `localStep` writes the remove-then-check order so that `RAISED` never keeps a stale entry, and the guard is applied twice (`SuitSize` and `localStep`).
- The tests make the RED failure fail for the right reason. The report shows `same effects re-sent [armor_toughness, movement_speed, armor]` from the old `set`, and `frame_step` empty.
- `attributesComeBackOnANewPlayerObject` covers Review Focus 3 directly: a new object, no record, no modifier, applied on the first tick and not re-sent on the second. It keeps `leaveTestFrame(fresh)` in `finally`.
- Assertion messages print both the value found and the value expected.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

- **T16-F1** `src/main/java/com/toxineren/exoframes/compat/impl/FrameAttributesImpl.java:24-26` (class Javadoc). The text says `apply` "lo tocca solo se importo ... o operazione sono diversi; altrimenti lo toglie e aggiunge quello nuovo". "Altrimenti" makes the sentence say the opposite of the code: when the values are equal, it removes and re-adds. This is the central contract of the class. Fix: "...sono diversi, e in quel caso lo toglie e aggiunge quello nuovo con `addTransientModifier`".
- **T16-F2** (plan-mandated) `src/main/java/com/toxineren/exoframes/gametest/EnergyGameTests.java:594-597`. The checks "`localStep` on a `ServerPlayer` changes nothing" cannot fail on any variant. On 1.21.1 `localStep` is empty. On 1.20.1 the `ServerPlayer` already has step 1.0 and is not in `RAISED`, so `localStep(p, false)` and `localStep(p, true)` leave 1.0 even when the `isLocalPlayer()` guard is removed. The brief prescribes this exact sequence. The report overclaims ("provato da `stepModifierFollowsTheVersion` sulle quattro varianti", report line 74). The real risk is low: the guard is applied twice, and even without either guard a `ServerPlayer` at 1.0 would not change. A version-neutral check would need a test-only seam to lower the server player's step first (1.20.1 `setMaxUpStep(0.6F)`, then `localStep(p, true)`, then expect 0.6). This is for the coordinator to decide.

### Assessment

**Task quality:** Approved

**Reasoning:** The idempotent `set`, the version-split step (fifth modifier on 1.21.1, an identity-weak local-player field on 1.20.1) and the four test helpers match the brief and design §12.5 line by line, with real-behaviour tests and documentation in the same commit. The two remaining items are Minor: one inverted Javadoc sentence, and one vacuous sub-assertion that the plan mandates.
