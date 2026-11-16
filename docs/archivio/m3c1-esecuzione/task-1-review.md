### Spec Compliance

- ✅ Spec compliant. Only Minor deviations (T1-F2 below).
  - Step 1 split, checked mechanically: I took the base and head versions of `ExoframeGameTests.java` and the head `GameTestKit.java` with `git show` into scratch, then split each class body into top-level members (Javadoc included) and compared them as sets. All 97 member chunks of the new `ExoframeGameTests` (constructor, `generate()`, 95 test bodies) are byte-identical to the base. All 51 chunks moved to `GameTestKit` (50 methods, `near` twice, and the block of 6 constants) are byte-identical to the base after `private static` -> `static`. No base chunk is lost or duplicated. The only new members in the kit are the private constructor and the 8 new helpers. No test-method name in `ExoframeGameTests` matches a kit name, so nothing in the static import is shadowed. No line of `GameTestKit` matches `^[[:space:]]*add\(tests,` (the Javadoc line at GameTestKit.java:84 starts with ` * `).
  - Javadoc links after the split: every `{@link #…}`/`{@value #…}` in `GameTestKit` (`add`, `startWhenEntitiesLoaded`, `MAX_TICKS`, `ENTITY_LOAD_TICKS`, `STRUCTURE_SIZE`, `wearFrameAt`, `scrapState`) resolves inside the kit. `ExoframeGameTests` has no `#` links left. No other source names a member of `ExoframeGameTests` (I grepped `src`, `tools`, `docs/compat.md` and `README.md`).
  - Class comment (GameTestKit.java:73-113): the "add a test" paragraph is rewritten, the tick-counter bullet is replaced word for word with the brief's text, and the `isCreative()`/`survivalPlayer` bullet is added. `ExoframeGameTests` keeps a short comment (diff :441-442).
  - Step 2: `tools/run_gametests.sh` has `tests_dir`, the `cat … | grep -cE` count, both messages ending `in $tests_dir/*.java`, and the head comment updated, exactly as the brief says.
  - Step 4: `CarryOverGameTests.java:21-68` is public final, has a public no-argument constructor and a `@GameTestGenerator` whose first line is the probe. The probe body matches the brief, including the `finally` placement inside the delayed callback. The Javadoc says what the probe proves and which later tests rely on it. It is registered at `RedstoneExoframes.java:55`, directly after the `ExoframeGameTests` line.
  - Step 6: `GameTests.survivalPlayer` (GameTests.java:41-51) has a Javadoc covering every point in "Produces" and Step 6. `GameTestsImpl` adds `SPAWN_PROTECTION_TICKS = 60` with the `spawnInvulnerableTime` comment and `survivalPlayer` (mock, then `setGameMode(SURVIVAL)`, then 60 x `tick()`), with no Stonecutter directive. The 8 new kit helpers each have a one-line Javadoc.
  - New helpers checked against unchanged code: `FrameState.cell()` returns null when there is no cell (FrameState.java:56), so `stored`'s `map(FrameState::cell)` throws the required `IllegalStateException("no worn frame with a cell")`. `pieces()` is an unmodifiable view of the mounted map (:73), so `worn` gives null for an empty slot. `damageCarryMilli()` is the live map (:177). `FrameStateAccess.get` returns the live server state, read-only (FrameStateAccess.java:56-61). `structureBox` and `fillPool` match the geometry in "Produces"; the `STRUCTURE_SIZE - 1` used in `structureBox` equals 7.
  - Step 8: `docs/compat.md:84-85` and `README.md:29` carry the required text.
  - Step 10: the commit body gives the four durations and says the `build.yml` limit is unchanged. `build.yml` is untouched (115 s < 15 min). The commit is signed (`gpgsig` present) and is on `m3c-giocarci`.
  - Test evidence: `versions/<v>/build/gametest/server.log` exists for all four variants, written 17:30-17:31, before the 17:32 commit. Each shows `All 96 required tests passed`, and `python3 -I tools/check_log.py gametest <log>` reports all four clean.
- ⚠️ Cannot verify from diff: nothing left that the repository cannot answer.

Implementer's flagged concerns, judged:
- `tools/log_allowlist.txt:13` ("the test in gametest/ExoframeGameTests.java"): still accurate. Every allowlisted gametest line comes from an `ExoframeGameTests` test, and the probe adds none. The file is outside Task 1's file list, so this is no finding here. The first task whose test in another class produces an allowlisted line should update that comment (the plan points to `registryFileWithoutSectionsIsReadOnly`, Task 5).
- Step 7 timings measured with a warm daemon: in `.github/workflows/build.yml:20-25`, the GameTest step runs after "Build dei quattro jar" in the same job, so compilation is already done when `run_gametests.sh` starts. The measured 115 s against the 15-minute threshold leaves a wide margin. No finding.

### Strengths

- The split is mechanical and was verified mechanically. Every moved helper and every test body is unchanged byte for byte, and `ExoframeGameTests` keeps exactly the imports it still uses (no unused ones).
- `survivalPlayer` is small and has no version directives. Its Javadoc states the non-obvious traps: `tick()` also lowers `invulnerableTime`, and `isCreative()` stays true.
- The probe follows the brief exactly. The listener is removed in a `finally` inside the delayed callback. The probe passes on all four variants, which confirms the fact Tasks 9 and 13-19 depend on.
- `fillPool` lays the floor and walls before the fluid, so the fluid never has an open side when it is placed (GameTestKit.java:755-763).

### Issues

#### Critical (Must Fix)
None.

#### Important (Should Fix)
None.

#### Minor (Nice to Have)

- **T1-F1**: unused imports at GameTestKit.java:8 (`compat.GameTests`), :25 (`frame.SlotGuard`), :50 (`resources.ResourceLocation`) and :59 (`entity.player.Player`). These names appear only inside `{@code …}` or plain Javadoc text, which needs no import (unlike `GameTestGenerator`, which is used by a `{@link}`). The report's claim that each file keeps only the imports it still uses is therefore wrong for `GameTestKit`. Fix: delete the four lines.
- **T1-F2**: GameTestKit.java:80 says "poi quelle delle parti seguenti", but the brief (Step 1) says "poi quelle dei task seguenti". The wording is wrong on the facts: the next classes (`EnergyGameTests`, `DamageGameTests`, `MovementGameTests`, `CellGameTests`) come from later tasks of this same part 3c-1, not from later parts. Fix: "poi quelle dei task seguenti".
- **T1-F3**: the `fillPool` Javadoc (GameTestKit.java:738) leaves out the caller precondition stated in the brief's "Produces": "chi chiama tiene bordo e pavimento dentro la struttura". The floor and glass rim reach one block outside the given corners (`minX - 1 … maxX + 1`, `minY - 1`). A later caller (Task 19) who passes corners on the structure's edge would place blocks outside the test's 8x8x8 area, where neighbouring tests run. Fix: add "bordo e pavimento (un blocco oltre gli angoli) devono stare nella struttura" to the Javadoc. It can stay a single line.

### Assessment

**Task quality:** Approved

**Reasoning:** The split is verified byte-identical, the new seam and helpers match the brief and the unchanged code they read, and the probe passes on four clean logs. The three findings are cosmetic or documentation fixes.
