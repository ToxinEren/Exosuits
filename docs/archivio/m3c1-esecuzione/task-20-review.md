### Spec Compliance

- ✅ Spec compliant. I checked every item of "Produces", file by file, against the diff:
  - `core/energy/CellBar.java:8-34`: `final`, private constructor, `MAX_WIDTH = 13`, `CELL_COLOR = 0xE0201C`. `width` returns 0 when `capacity <= 0`. Otherwise it is `Math.round((float) 13 * clamp / capacity)` limited to [0, 13], the same float expression as the brief's `13f * ... / capacity`. `visible` = `capacity > 0 && stored < capacity`.
  - `compat/impl/ItemBase.java:8-10`: the `Level` import sits inside `//? if <1.21 {`, in the form EntityBase uses (`compat/impl/EntityBase.java:7-10`, `:81-106`).
  - `ItemBase.java:35-36`: `tooltip` is empty.
  - `ItemBase.java:38-50`: both `appendHoverText` overrides call `super` first and then `tooltip(stack, lines, flag.isAdvanced())`.
  - The ItemBase Javadoc names the closed differences, the neutral method, and says that mod code does not extend the class directly.
  - `compat/CompatItem.java`: no directives. Its Javadoc follows the pattern of `CompatEntity.java:7-13`.
  - `content/CellItem.java`: still `final`, with both tooltip constants.
  - `CellItem.java:61-71`: the bars use `cellEnergy` + `ClientRules.cellCapacity()` with `CellBar`, and the color is `CELL_COLOR`.
  - `CellItem.java:82-90`: the tooltip shows `shown = min(energy, capacity)` and switches to EMPTY at 0. The line is GRAY.
  - The CellItem class Javadoc says everything the brief asks for.
  - `content/PieceItem.java`: the bar is visible only when there is data and durability is below the maximum. Width is 0 without data. The color is `Mth.hsvToRgb(max(0, d/max)/3, 1, 1)`, or `super` without data. The tooltip is DURABILITY(d, max), or NEW with no arguments. Both lines are GRAY.
  - `content/ExoframeItem.java`: four GRAY lines in the order place, mount, enter, unmount.
  - `compat/impl/FrameStateCodec.java:141-157`: the cell branch is split on `trusted`.
    - The untrusted branch never reads `capacity`. It uses `cfg.energy.cellCapacity` and cuts the charge.
    - The trusted branch is unchanged, warning included.
    - `write` still writes `capacity`.
    - The Javadocs of the class, `read` and `readTrusted` are updated and cite §12.10.
  - `RedstoneExoframes.java`: the `CellGameTests` registration is on the line right after `MovementGameTests`.
  - Language files:
    - All eight `tooltip.*` keys sit after `condition.redstone_exoframes.broken` (`en_us.json:55-62`, same in `it_it.json`). Their texts match the brief word for word.
    - `no_cell` stays on line 32 with the new text.
    - Both files have the same 61 keys in the same order (checked with a script).
  - `docs/compat.md`:
    - The new `CompatItem` section after `CompatEntity` has the five required bullets.
    - API table: the `appendHoverText` row now reads `CompatItem` (`ItemBase`, parte 3c-1). The implementer split the old row, which also covered `getUseDuration`, so `getUseDuration` keeps "—". This is a deviation the report declares, and it is more accurate than changing the shared row.
    - Schema NBT rows `cell` and `capacity` both carry the brief's sentence.
    - The known limit "Capacità della cella solo nello stato" is now marked closed in 3c-1.
  - Tests:
    - `CellBarTest` has the five cases with the brief's exact values.
    - `FrameStateCodecTest`: `populated()` now uses the config capacity. The three new cases match the brief, sharing one helper, `bigCellTag()`.
    - `CellGameTests` has the three tests with every assertion the brief lists, each test on its own `add(tests,` line. The common premise lives in a private helper, `localCapacity`. The `keys` and `args` helpers match the brief.
- Checks I ran outside the diff, one per named risk:
  - **Changed contract of `FrameStateCodec.read`.** The only production caller is `FrameStateSavedData.java:347` (server load from disk). Every other caller is a test.
  - **New `EnergyCell` built from the config could throw.** `ConfigValidation.java:81` keeps `cellCapacity` positive, and `PieceTags.readEnergy` (`PieceTags.java:90-91`) is ≥ 0. So `new EnergyCell(cap, min(cap, energy))` cannot throw.
  - **The `CellGameTests` premise depends on global state.** No GameTest calls `ClientRules.accept`; only `ClientRulesTest` (JUnit) does, and it resets after each test. So `ClientRules.cellCapacity()` falls back to the local config on the test server.
  - **The `Dropping frame cell with invalid capacity` warning appears in `tools/log_allowlist.txt` with a count that would now change.** It does not appear there (grep).
  - **Style.** No new line is over 120 characters; `PieceItem.java:27` reads as 121 only because awk counted `§` as two bytes. No typographic apostrophes or accented letters in the changed Java files. The working tree is clean.
- ⚠️ Cannot verify from diff: the real hover tooltip (the per-version `appendHoverText` overrides) and how the bars render in a real client. No GameTest reaches them, as the brief itself says. Task 24 (inventory screenshot) and the 3c-2 PC checklist must confirm them, on 1.20.1 as well (the commented-out branch).

### Strengths

- The `trusted` split in `FrameStateCodec.java:141-157` is minimal and readable. The trusted branch is byte-for-byte the old logic, the Javadocs of `read` and `readTrusted` now state the two contracts side by side, and the comment cites §12.10.
- The GameTests catch real failures rather than being vacuous:
  - `over.getBarWidth() == 13` and `args == (capacity, capacity)` fail without the mod's clamp.
  - `half.isBarVisible()` fails without the overrides.
  - `localCapacity` turns a wrong `ClientRules` fallback into a named failure instead of a confusing width mismatch.
- `ItemBase` follows the `EntityBase` pattern exactly (import form, commented branch, Javadoc structure). `content/` and `gametest/` see only `CompatItem` and the neutral `tooltip`.
- Splitting the API-table row in `docs/compat.md` avoids falsely marking `getUseDuration` as covered. The report declares this deviation.
- The report's TDD evidence is coherent: compile failure, then `readCutsTheCellToTheConfigCapacity` failing for the right reason, then green on two variants, 159 on all four, and a clean build.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

- **T20-F1: the trusted branch's drop path has no test.**
  - **Where:** `src/main/java/com/toxineren/exoframes/compat/impl/FrameStateCodec.java:142-150`, with `src/test/java/com/toxineren/exoframes/compat/impl/FrameStateCodecTest.java:225-231`.
  - **What is missing:** `readTrusted` must still drop a cell with absent, zero or negative `capacity` and warn. No test exercises that path; a grep of `src/test` finds no case that removes `CAPACITY` and calls `readTrusted`.
  - **Why it matters:** this task moved that branch under `if (trusted)`. A later edit could hand the trusted path the config fallback, so the client would invent a cell the server never sent, and nothing would fail.
  - **Fix:** in `FrameStateCodecTest`, take `bigCellTag()`, remove `NbtKeys.CAPACITY` from the cell compound, and assert that `FrameStateCodec.readTrusted(tag, cfg).cell()` is `null`.

### Assessment

**Task quality:** Approved

**Reasoning:** Every requirement of the brief is implemented as specified: the pure `CellBar` rule, the `ItemBase`/`CompatItem` seam, the bars and tooltips on the three items, the server-only single cell capacity in `FrameStateCodec.read`, the language keys, the docs and the tests, with the result I can verify from the repository. The one finding is a Minor coverage gap on a branch the task moved.
