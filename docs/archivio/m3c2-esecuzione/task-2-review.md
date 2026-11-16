# Task 2 review — Le basi per versione e il caricatore vuoto

Base 1846274fe8707f6dede8bd132be755b875b63097, head b5ac1b51152cb32c0f3fcfad7069834e2af36125 (one commit, 35 files, +1135 -23).
Diff read in full from `.superpowers/sdd/2026-11-16-m3c2-ricaricare/review-1846274..b5ac1b5.diff` (four passes, the file is long).

### Spec Compliance

- ✅ Spec compliant. Checked file by file against the brief's lists: all 22 files to create and all 13 to modify have their hunk; nothing outside the lists was touched (`tools/log_allowlist.txt` unchanged).
  - `compat/impl/EntityBlockBase.java:41-70`: the brief's text to the letter (neutral `onClick`, `useItemOn` protected with the five-case map on 1.21.1, `use` public on 1.20.1, `useWithoutItem` not overridden).
  - `compat/impl/BlockEntityBase.java:37-66`: `readData`/`writeData` abstract; `protected final loadAdditional`/`saveAdditional` (1.21.1), `public final load` / `protected final saveAdditional` (1.20.1); each calls `super` first.
  - `compat/impl/BlockItemBase.java:28-52`: same shape as `ItemBase` (opened `compat/impl/ItemBase.java` to compare: identical seam, `extends BlockItem`, constructor `(Block, Item.Properties)`).
  - `compat/CompatEntityBlock.java`, `CompatBlockEntity.java`, `CompatBlockItem.java`: only the protected constructor and a Javadoc that names what the base closes and `docs/compat.md`; no directives.
  - `compat/GameTests.java:173-187`, `compat/impl/GameTestsImpl.java:330-355`: signatures, `saveWithFullMetadata`, `loadStatic` with the block state at the position, `setBlockEntity`, the two `IllegalStateException` (the first with the exact message), "Esiste solo per i test"; directive only where the provider passes.
  - `compat/NbtKeys.java:140-160`: the five `CHARGER_*` constants with the values of the shared section, each with Javadoc; class sentence at `:17-19`; `CARRY_MILLI` not in the diff.
  - `content/ModBlocks.java:21-24` (properties equal to the shared section), `ModBlockEntities.java:19-21` (`build(null)`, built with `ModBlocks.CHARGER.get()`), `ModItems.java:58-60`, `RedstoneExoframes.java:67-69` (order blocks, items, block entities, each with its one-line comment) and `:98`, `ModCreativeTabs.java:33` (last) with the class Javadoc updated.
  - `content/ChargerBlock.java`: three properties and four keys `:35-48`, default state `:50-54`, `newBlockEntity` `:56-59`, placement `:62-65`, `rotate`/`mirror` public `:72-80`, first-step `onClick` `:87-99` (PASS for off hand, spectator, not alive, non-empty main hand; client SUCCESS; server message and CONSUME). Matches the ledger ruling on the empty-hand click.
  - `content/ChargerBlockEntity.java:32-44`: clamp to [0, `MAX_RE_PER_REDSTONE`], `putInt`, `onlyOpCanSetNbt()` true; nothing of later tasks added.
  - `content/ChargerItem.java:18-35`: `final`, five keys in order, one gray line per key (same pattern as `ExoframeItem.java:49-53`).
  - Resources: `blockstates/charger.json` 16 variants, model rule (lit, else cell, else plain) and `y` 0/90/180/270 correct for every variant; three block models on `minecraft:block/orientable`; `models/item/charger.json`.
  - Texts: the seven keys in both language files, at the positions and with the exact texts of the shared section.
  - `gametest/ChargerGameTests.java`: the six tests with every assertion the brief lists; `GameTestKit.java:774-800` (`cell`, `energy` moved with unchanged bodies, `clickBlock`, `itemsIn`); `CellGameTests.java` loses the two helpers.
  - `tools/gen_placeholders.py:343-417`, `tools/README.md:35,45-47,59`: five opaque 16x16 textures in `outputs()`, "diciotto file" twice, the line of the block textures.
  - `docs/compat.md`: three new sections after `CompatItem` (result table and sneaking note included), `CompatItem` "Uso" in the present tense, the two helpers in "A cosa serve" and "Condizione" of `GameTests`, three rows of the API table, the `reserve` row of the NBT schema.
- Checks made outside the diff (one per named risk):
  - Design conflict: read design §12.11 and §12.16 (`docs/superpowers/specs/2026-11-13-m3-telaio-design.md:569-585`, `:697-720`): hardness 3.5, metal, `requiresCorrectToolForDrops`, light 7, `facing` like a furnace, registration order, `onlyOpCanSetNbt`, no `BaseEntityBlock`: the code agrees. `ChargerItem` is not in §12.16, as the brief itself says.
  - Test count: `add(tests,` lines in `gametest/*.java` = 181, the number of the plan's table.
  - Step 7 evidence: opened the implementer's shots in the session scratch folder. `shot-fabric120/caricatori.png` and `shot-neoforge121/caricatori.png` show three gray cubes with three different fronts facing the player (cell off, empty, cell with three red notches), no missing-texture cube; `caricatore-inventario.png` shows the block icon in the hotbar; `log-check.txt` is 0 bytes on both; no WARN/ERROR line naming the charger in client or server log; `rcon.log` shows the three `setblock` and the `give` accepted ("Gave 1 [Cell Charger]").
  - Stonecutter comment form `/*...*/` then `//?}` at `GameTestsImpl.java:347-349`: the same form is already used in the same file (`:120`, `:127`, `:236`) and in `NetImpl`, `StackTags`.
  - Lines over 120 columns and non-ASCII in the new Java files: none new (the only non-ASCII is the section sign in "design §12.11", already used across the code base).
  - Commit: signed like its parent, on `m3c-giocarci`, message as the brief; working tree clean.
- ⚠️ Cannot verify from the repository:
  - The build, `check_*` and the four `OK <variante>: 181 ...` lines are the report's; not re-run, as the contract says.
  - The `javap` signatures on the Forge and NeoForge patched jars were not re-read; the four-variant build the report shows is the evidence.
  - Which of the two paths reads the language files on 1.21.1-fabric (the report says it did not look); the test passes either way.

### Strengths

- The three seams are minimal and symmetrical with the existing `CompatEntity`/`CompatItem` pairs; overrides of `BlockEntityBase` are `final`, so a subclass cannot skip `super` (loader data on Forge and NeoForge).
- The tests take the game's real paths: `ServerPlayerGameMode.useItemOn` for placement and click (block first, then item), `saveWithFullMetadata`/`loadStatic` for the chunk path, and they assert identity (`loaded != first`, `first.isRemoved()`, level holds the loaded one).
- `chargerEntitySavesAndLoads` adds `saved.contains(CHARGER_RESERVE)` (`ChargerGameTests.java:165`): without it `getInt == 0` would pass with an empty `writeData`.
- `solid()` in the generator (`tools/gen_placeholders.py:355-361`) turns "all opaque" into a checked property at every generation, hence in `check_art.sh`.
- The report is accurate where I checked it (stat, shots, logs, count) and names what it did not look at.
- `docs/compat.md` states the sneaking rule and why `BaseEntityBlock` is not used, which Task 5 will need.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

- **T2-F1** `content/ChargerBlock.java:72-80`: `rotate` and `mirror` have no test (nor do the spectator and not-alive branches of the first-step `onClick`, `:90`, which Task 5 rewrites). They are the furnace's implementations, so the risk is low; two assertions in `chargerPlacedByItemFacesThePlayer` (`state.rotate(Rotation.CLOCKWISE_90)` gives `WEST` from `SOUTH`; `state.mirror(Mirror.LEFT_RIGHT)` gives `NORTH`) would pin them without changing the count of 181.
- **T2-F2** `docs/compat.md:3`: the opening paragraph still lists "e in futuro `block/`" among the packages that call `compat/`, while this commit puts the first block in `content/` (design §12.16) and documents it in the same file. Pre-existing sentence, outside the brief's Step 6 list, flagged by the implementer; three words to drop, here or in Task 10.

### Assessment

**Task quality:** Approved

**Reasoning:** The diff delivers every item of the brief with the exact signatures, values and texts, and the tests exercise the per-version seams through the game's own click and chunk paths on the four variants. The two Minor notes are a small coverage gap on vanilla-identical code and a stale sentence in the compat document.
