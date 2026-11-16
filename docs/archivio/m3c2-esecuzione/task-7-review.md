# Task 7 - review (spec + quality)

Base 9619ec68ef115006f6234168b99f9642f2cc2fc0, head b5db281cf7d73f5f109b6372dacb9e92cb47b18b (one commit, branch `m3c-giocarci`, subject as the brief's Step 8).

### Spec Compliance

- ✅ Spec compliant.
  - Files: `tools/gen_data.py`, `tools/check_data.sh` (mode 100755) created; 32 data files, sixteen per tree, at the paths of "File dei dati" (7 recipes, 7 unlocks, loot table, tag per version); `tools/check_jars.sh`, `.github/workflows/build.yml`, `tools/README.md` modified. Nothing else touched.
  - Recipes: seven shapes and categories in `tools/gen_data.py:44-52` equal the plan table and design §12.12 (read: lines 591-611 of the design); material total recomputed from the files: 34 iron, 14 copper, 2 redstone blocks, 4 dust, 2 quartz. Result `item` in the 1.20.1 tree, `id` in the 1.21.1 tree, `count` 1; the only content difference between the trees (the seven unlocks have the same blob hashes in the diff).
  - Unlock, loot table and tag JSON: equal to the brief's "Produces" text and to the formats of the reconnaissance §2 (read: lines 225-262), apart from key order (sorted, by `sort_keys=True`).
  - `gen_data.py`: stdlib only, docstring with the usage, `INGREDIENTS`, `RECIPES`, `VERSIONS`, `build(version)`, `validate(files_by_version)`, `main()` with `--out` / `--selftest`, serialization as required (`tools/gen_data.py:143-145`); `validate()` runs before any write (`build_all`, `:279-283`).
  - `validate()`: every rule of the brief is present (count, category, pattern, symbols and keys, id form, unlock with the same id in the rewards, same ids on the two versions, other version's folders, `remove`); messages start with the file path.
  - `--selftest`: the seven defects of the brief, one at a time on a deep copy; prints `OK selftest (7 difetti segnalati)`; a defect not reported, or reported for another file or reason, gives exit 1 (`:337-357`).
  - `check_data.sh`: header like `check_art.sh` (compared), selftest, regeneration with `--out`, the four FAIL texts of the brief, `OK data`.
  - `check_jars.sh` (f3): after (f2) (`tools/check_jars.sh:261-292`), the two regular expressions of the brief, `err` texts as required, comment with the reason; the sixteen files of the jar's version required.
  - `build.yml`: the step `Controllo dei dati generati` right after `Controllo della grafica generata`.
  - `README.md`: the two rows and the paragraph "Dati per versione: `gen_data.py`".
  - Extras (small, in the spirit of the brief, none harmful): `validate()` also wants the result key of the version, `replace` false in tags and the type `minecraft:crafting_shaped`; one more phrase in the README row of `check_jars.sh`.
- ⚠️ Cannot verify from the diff: the outcomes of Step 4, 6 and 7 that need a build or a game run (`OK jars` on freshly built jars, four lines `OK <variante>: 202 ...`, the load of the unlock JSON on the four variants) are taken from the report, not re-run. That the `inventory_changed` criterion really fires and unlocks the seven recipes, that every recipe crafts and that the loot drops are Task 8's.

Checks run (read-only):
- `git rev-parse HEAD` = head, working tree clean.
- `python3 -I tools/gen_data.py --selftest` -> `OK selftest (7 difetti segnalati)`, exit 0; `bash tools/check_data.sh` -> `OK data`, exit 0 (so the 32 committed files are byte-identical to the generator's output at HEAD and the common data folder holds only the two `empty.nbt`).
- Named risk: `set -eu` in `check_data.sh` with `[ ... ] && allowed=1` as the last command of a `for` (`tools/check_data.sh:69`): bash does not exit on a compound command that fails while `-e` is ignored; the `OK data` run goes through that loop with the two allowed files, and the report's negative proof covers the other branch.
- Named risk: `set -o pipefail` with `grep -q` in (f3): `check_jars.sh` has only `set -eu` (line 3), and `listing` comes from `unzip -Z1` (line 55); no false "manca".
- Named risk: `validate()` rules without a selftest entry. Probed in memory (a scratch script outside the repository, importing `gen_data.py`): 4x3 / ragged / empty pattern, result key of the other version, `count` True, ingredient id with an uppercase letter, a 1.20.1 file under `tags/item/`, a file outside the version root, an unknown version, an unlock removed from both versions, rewards naming another recipe: all rejected with a ValueError that names the right file. Not rejected or not a ValueError: see T7-F1 and T7-F2.
- Named risk: the rule "exceptions in English" against the Italian messages of `gen_data.py`: the other Python tools of the repository (`tools/art/exo.py`, `paint.py`, `compare_png.py`) raise `ValueError` with Italian text, and the brief itself fixes Italian output texts; not a finding.
- `build.gradle.kts:179-183`: `resources.srcDir(rootProject.file("src/main/resources-mc$mc"))` is there, the build needs no change.

### Strengths

- The selftest does not accept any `ValueError`: it wants the right file and a fragment of the right reason (`tools/gen_data.py:345-349`), and the expected paths in `DEFECTS` are written out by hand, independent of `_path()`. A check silenced or a defect caught by the wrong rule fails the selftest.
- `_classify()` (`:161-179`) rejects both the other version's folders and unknown folders, including `tags/item(s)` that nobody writes today, as design §12.12 asks.
- `check_data.sh` compares the two sets in both directions (missing, different, extra) with a sorted listing and also fails when the generator writes nothing; the extra check on the common resources closes the third place where a recipe could be put by mistake.
- (f3) reuses `err`, `$listing` and `$mc` without new machinery, and the here-string keeps the loop in the main shell so `fail=1` is not lost.
- The report goes beyond the brief with real negative proofs (a recipe and an unlock ruined on purpose make `run_gametests.sh` fail on two variants), which is the evidence that Step 6 means something.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

- **T7-F1** `tools/gen_data.py:228-255` - `validate()` never looks inside an unlock except `rewards.recipes`: `parent`, the triggers, the item of `has_copper_ingot` and the recipe of `has_the_recipe` are not checked. Probed: `parent` "Recipes Root", item "Copper Ingot" and a `has_the_recipe` that names another recipe all pass. The brief says "ogni id ha la forma spazio:percorso". Low risk (the strings are literals of `build()`, and a malformed id makes the game write a parsing error that `run_gametests.sh` catches; only the wrong recipe in `has_the_recipe` would be silent). Fix: a `_validate_unlock()` that applies `_require_id` to those ids and wants `conditions.recipe` equal to the rewarded recipe.
- **T7-F2** `tools/gen_data.py:213-225` - `_validate_loot_table` and `_validate_tag` assume the shape: `pools` that is not a list of objects raises `AttributeError`, `values` null raises `TypeError`. The contract is "lancia `ValueError` con un messaggio che nomina il file", and `main()` catches only `ValueError` (`:368-372`), so the user would see a traceback without the file name. Only reachable by editing `build()`. Fix: `isinstance` checks with `_fail(path, ...)`, as `_validate_recipe` does for `pattern` and `key`.
- **T7-F3** (plan-mandated) `tools/gen_data.py:319-335` - the selftest proves seven of the rules of `validate()`; the pattern shape, the id form, the unlock pairing and the three added rules (result key, `replace` false, type) have no committed negative proof. The brief fixes the list of seven and the text `OK selftest (7 difetti segnalati)`, so more cases would change a mandated output. I probed those rules by hand and they reject (see the checks above). For the coordinator: nothing to do unless the count is allowed to grow.
- **T7-F4** `tools/check_jars.sh:281-291` - the sixteen required files are a second hand-written list, apart from `RECIPES`: a recipe added to the generator and not here still gives `OK jars`. The README and the docstring say so, and `check_data.sh` still guards the sources, so the residual risk is small. Optional: let (f3) take the list from the generator (for example the relative paths printed by `--out` in a temporary folder).
- **T7-F5** `build.gradle.kts:179-181` (outside the task's files, already noted by the implementer) - the comment above `resources.srcDir(... resources-mc$mc)` still says the folder today carries the transparent armor textures of 1.20.1; from this commit it also carries the data of both versions. One line to update in a later task that touches the build, or by the coordinator.

### Assessment

**Task quality:** Approved

**Reasoning:** The generator, the two checks and the 32 files match the brief, the plan table and design §12.12 line by line, and the checks I ran at HEAD (`OK selftest`, `OK data`, in-memory probes of `validate()`) agree with the report. The five findings are Minor: gaps in what `validate()` inspects inside unlocks and in its error type for malformed loot tables and tags, a hand-kept list in (f3), and a stale comment outside the task's files.
