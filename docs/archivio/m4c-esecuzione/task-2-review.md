### Spec Compliance
- ✅ Spec compliant. All 13 files in the brief's Files list are in the diff: 5 created (`tools/client/m4c_shots.sh` has mode 100755) and 8 modified. None is missing and none is outside the list.
- ✅ `ClientPrefsFile` matches "Tipi nuovi":
  - `FILE_NAME`, `VERSION`, `Values`/`DEFAULTS` and `Loaded` (ClientPrefsFile.java:50-70).
  - `load` rules and problem texts (:78-111).
  - `save` (:116-153) starts from the file's object. It keeps `version` only when that is a greater number. It writes a temp `<name>-<n>.tmp` with `CREATE_NEW`, then `ATOMIC_MOVE`, falling back to `REPLACE_EXISTING`. The temp is deleted in a `finally`, and a failure returns `could not be written: <e>`.
  - The Javadoc follows ConfigFile's form, with the paragraph on how this file differs from ConfigFile.
- ✅ `ClientPrefsFileTest` has the nine tests and every case of Step 1 (ClientPrefsFileTest.java:55-155).
- ✅ `ClientPrefs` (ClientPrefs.java:27-65): both WARN texts match "Tipi nuovi" word for word (:31, :63).
- ✅ `HudKeys` (HudKeys.java:28-86):
  - names, H as the default, `VIGNETTE_DEFAULT = InputConstants.UNKNOWN.getValue()`, `ExitKey.CATEGORY`;
  - the `consumeClick` loops act only with `screen == null` and a player;
  - the four messages, and the ERROR text logged once per session with its reset on `CLIENT_PLAYER_QUIT` (:58-59, :82).
- ✅ The other changed files:
  - RedstoneExoframesClient.java:28-30: the comment, then `ClientPrefs.load()` and `HudKeys.register()`, after `CellSwapKey.register()`.
  - EntryHints.java:27 and :47; CellGameTests.java:446-449.
  - The six new keys and the new `keys_hint`, with the exact "Testi" wording and in the same order in both files (en_us.json:46-52, it_it.json:46-52).
- ✅ `client_shot.sh` matches "Le aggiunte":
  - it removes the client prefs file and copies `EXO_SHOT_CLIENT_PREFS` in its place (:345-349);
  - it makes the path absolute and stops with exit 2 before anything starts (:292-297);
  - the J line is in options.txt (:403);
  - it copies the prefs file after the three final shots (:686-690) and removes the old copy from the folder at the start (:325);
  - `--selftest` has `@tieni h 1`, `@tieni j 1` and `@tieni t 0.2` (:110-111); the header and the exit-2 list are updated.
- ✅ `m4c_shots.sh`:
  - runs A and C follow the plan's table step for step (m4c_shots.sh:82-96);
  - C runs only after A exits 0 and leaves the file; otherwise the problem is `manca il file delle preferenze dell'esecuzione A` (:123-129);
  - the checks `chat-e-tasti` and `riavvio-file` use the plan's python one-liner (:141-158);
  - also present: `unset EXO_SHOT_SERVER_CONFIG` (:62), the `rm -f` before the runs (:107-109), `M4C_SOLO_CONTROLLI` (:103), and `source measure_lib.sh` (:138).
- ✅ Docs:
  - README: usage line, the prefs file in the folder list, the J and `version:` lines, the `EXO_SHOT_CLIENT_PREFS` row (README.md:198), the m4c paragraph (:167).
  - compat.md "Avvio del client": the init bullet (:237) and the new `ClientPrefs` (:246) and `HudKeys` (:247) bullets. This is the only section the Global Constraints let Task 2 change.
  - The commit subject is exactly the brief's.
- ✅ The `version:` line (accepted by R6) is correct on its merits:
  - The values match the game's data versions (client_shot.sh:164-168, :384). I checked them: `unzip -p` of `version.json` in the cached `fabric-loom/1.20.1` and `1.21.1` `minecraft-client.jar` gives `world_version=3465` and `world_version=3955`.
  - With the current version, `Options.dataFix` runs no options fix, the same as for any file the game writes.
  - The implementer read with javap that, of the keys the harness writes, a fix touches only `onboardAccessibility`, and sets it to false, which the harness already writes. I did not re-read the jars.
  - Empirical backing comes from two regression runs: m4a1 on 1.21.1-neoforge and m3d on 1.20.1-forge, which covers `mainHand:"left"`. All their pixel-exact values match the documented references (inventory width aside, Minor 7), with 0 `Failed to load options` in the three client.logs. The other variants show only the four gate runs: GUI scale 2 and the right arm. m3d and m4a1 were not re-run on the other three variants.
- ✅ Three extras, all acceptable:
  - The same-file guard (client_shot.sh:315-318). Without it the script would delete the caller's source file before copying it.
  - `serializeNulls()` (ClientPrefsFile.java:59). It is needed for "tiene ogni altra chiave com'è", and ClientPrefsFileTest.java:121-125 pins it.
  - The write WARN's second argument holds only the cause (ClientPrefs.java:62-64).
- Check, gate evidence (scratch `m4c/t2/gate/`), on all four variants:
  - the `.out` files have `ok chat-e-tasti: True False`, `ok riavvio-file: False False` and `OK <variante>`;
  - prefs file A is `{"version":1,"hudHidden":true,"vignette":false}` and C is `{"version":1,"hudHidden":false,"vignette":false}`;
  - all eight `log-check.txt` files are 0 bytes;
  - the eight client.logs have 0 `Failed to load options`, 0 `Client preferences file` and 0 `Suit HUD keys`;
  - `options-run-shot-client.txt` starts with `version:3955` or `version:3465` and ends with the J line.
- Check, shots I looked at: 1.20.1-forge `messaggio-nascosto.png` ("Exoframe HUD hidden: H to show it"), 1.21.1-neoforge `messaggio-vignetta.png` ("Helmet vignette off") and 1.21.1-fabric `riavvio/riavvio-messaggio.png` ("Exoframe HUD shown"). Each shows the action bar text, the GUI-scale-2 hotbar and the right arm.
- Check, first-run evidence: client.log:46 and :177 show `Failed to load options` with `NumberFormatException: For input string: "key.keyboard.j"` at `OptionsKeyLwjgl3Fix`, and that run's options.txt has no `version:` line. The diagnosis holds.
- Check, regression runs:
  - m4a1-neoforge.out has `ok anteprima` 18582, `ok pulsante` 1112 and `ok rotazione` 24332/9225, the README reference values; its log-check.txt is empty.
  - m3d-forge.out has 14 `ok` lines, including `mancino-controllo`, `mancino` and `specchio`; both log-check.txt files are empty.
- Check, counts:
  - The plan's JUnit count on the current build results gives 387 on each variant. ClientPrefsFileTest has `tests="9" failures="0" errors="0"`, with empty system-out and system-err.
  - gametests-final.log has four `OK <variante>: 235 ...` lines.
  - build-final.log ends with `BUILD SUCCESSFUL` and has no warning lines.
- Check, named risk that an earlier sequence presses H or J, which would now toggle a preference or write on the action bar: grep of `tools/client/*.sh` finds `h`/`j` presses only in client_shot.sh:110, the selftest list, which starts nothing.
- Check, named risk that m4c misuses the `verdict` contract: `verdict <nome> <0 se vale>` (measure_lib.sh:15-16) matches m4c's calls. `failed=()` is set at source time (:19). `measure` is used only by `take_measure` (:21), which m4c does not call.
- Check, named risk about the static `KeyMapping` fields (HudKeys.java:42-43):
  - They are built at class init. On Forge and NeoForge that happens on the mod-loading thread, where the `KeyMapping` constructor writes unsynchronized static maps.
  - ExitKey.java:34 and CellSwapKey.java:32 build theirs the same way, and the plan says "come ExitKey". The pattern predates this task, so this is not a finding here.
- Check, named risk that the m3d forge inventory measure, outside its documented range, comes from the `version:` change:
  - m3d-forge.out:111 has `ok inventario: magenta=18831 riquadro=176x202+318+66`.
  - m3d_shots.sh:64-67 documents 170-174 wide from x 320-321 and magenta 18796-18830 on Forge, as a measure that "cambia di poco anche fra due esecuzioni della stessa variante".
  - The options that could move it, GUI scale, mainHand and fov, are written with the same values before and after, and no fix touches them. The m3d measures that depend on them match the reference to the pixel, and terza-persona 23651 is inside its documented 23538-23745.
  - So this is run-to-run variation, not the options; see Minor 7.
- ⚠️ Cannot verify from diff: the signature and two trailers of 8b8afbc, and a clean `git status --short` after it. The report claims both.
- ⚠️ For Task 3's dispatch: Minor 4 (the measure_lib.sh header), Minor 5 (`unset EXO_SHOT_CLIENT_PREFS` in the earlier sequences) and Minor 7 (the inventory-width range in m3d_shots.sh and the README), plus R7's `version:` fact. All of these are in files Task 3 edits or should edit.

### Strengths
- `ClientPrefsFile.save` (ClientPrefsFile.java:116-153) is careful:
  - it never deletes a temp it did not create (the `FileAlreadyExistsException` path);
  - it creates the folder and replaces the file atomically, with a fallback;
  - every failure comes back as a problem, never an exception.
  - `serializeNulls` closes a gap in the brief's "keeps every other key", and a mutation run showed the test catches its removal.
- The tests use real files in `@TempDir`, with no mocks. The gametest change was mutation-checked: with two keybinds the test fails.
- `HudKeys` follows DEC-39 (one `try` around the tick, one ERROR per session, reset on quit). It builds its mappings exactly like ExitKey.java:34 and CellSwapKey.java:32.
- m4c's two checks tell the failure modes apart: an `h` counted in the chat gives `False False`, an unbound J gives `True True`, a file ignored at restart gives `True True`, and a C client that never writes gives `True False`.
- The options.txt failure was traced to its exact cause. The fix touches neither the key assignment nor when the preferences are read, and two regression runs back it.

### Issues
#### Critical (Must Fix)
None.

#### Important (Should Fix)
None.

#### Minor (Nice to Have)
1. ClientPrefs.java:14 (also :20-21 and :26): the Javadoc says "Solo client, solo sul thread del client", which is false for `load()` on Forge and NeoForge.
   - There `RedstoneExoframesClient.init()` runs in the mod constructor, and the gate client.logs show `[modloading-worker-0/INFO] … Redstone Exoframes init` (on Fabric it is `Render thread`).
   - There is no actual race, because the loader joins the construct phase before the render thread reads `file` and `values`. But the Javadoc promises something the code does not do. The phrase comes from the plan's "Tipi nuovi".
   - Fix: say that `load()` runs once during mod init (on a mod-loading thread on Forge and NeoForge), before anything reads the values, and that everything else runs on the client thread. Or make the two fields `volatile`.
2. ClientPrefsFileTest.java:77-90: none of the broken cases checks that a broken file resets both values.
   - `{"hudHidden": "yes"}`, `{"vignette": null}` and `{"version": "one"}` would also give `DEFAULTS` if only the bad key fell back to its default.
   - So nothing pins "a broken file gives both defaults" (ClientPrefsFile.java:99-106): a regression to per-key fallback passes all nine tests.
   - Fix: add one case with a valid, non-default value next to the bad one, for example `{"hudHidden": true, "vignette": null}` → `DEFAULTS` and `has a wrong type for vignette`. The brief's case list has the same gap.
3. ClientPrefs.java:18 and :64: `WRITE_PROBLEM` copies the `"could not be written: "` literal of ClientPrefsFile.java:143 into another package, and no test pins the `": "`. ClientPrefsFileTest.java:153 checks only `could not be written`.
   - If the two drift apart, the WARN falls back to the full text, so the cost is small.
   - Fix: a public constant in ClientPrefsFile, or drop the stripping, removes the hidden contract.
4. tools/client/measure_lib.sh:2: the header still names only `m3d_shots.sh` and `m4a1_shots.sh` as callers. m4c_shots.sh:138 sources it, and README.md:169 now lists m4c. The file is outside Task 2's list, so Task 3, which adds arm_measure checks to m4c, should update it.
5. The earlier sequences do not clear `EXO_SHOT_CLIENT_PREFS`. A grep of `tools/client/*.sh` finds the variable only in client_shot.sh and m4c_shots.sh, where m4c sets it per run (:113). The six default-config sequences unset only `EXO_SHOT_SERVER_CONFIG`: m3a:26, m3b:43, m3c:48, m3c2:39, m3d:73 and m4a1:130.
   - This is harmless in Task 2, because nothing is drawn from the preferences yet.
   - From Task 3 on, an inherited file with `hudHidden: true` would hide the HUD in m3d's shots, and its checks would fail with no log line naming the cause.
   - This is a gap against the Global Constraint "parte sempre … dalle preferenze predefinite". The constraint itself makes an exception for `EXO_SHOT_CLIENT_PREFS`, so it is not a violation.
   - For Task 3: add `unset EXO_SHOT_CLIENT_PREFS` next to `unset EXO_SHOT_SERVER_CONFIG` in the sequences that assume defaults.
6. tools/client/client_shot.sh:384-403: when the game rejects the options.txt, the harness only notices indirectly.
   - The first gate run waited 900 s and failed with "il giocatore Exo non e' entrato nel mondo".
   - log-check.txt cannot see `Failed to load options`, because that line does not name the mod.
   - Fix: check the client log for that line and fail with its own `ERRORE:`. Any future options line in an old format would then fail fast and name the cause. This goes beyond the plan; the implementer suggested it too.
7. The documented range of the 1.20.1 inventory preview is too narrow:
   - m3d_shots.sh:64-67 says Forge is 170-174 wide from x 320-321 with magenta 18796-18830; m3d_shots.sh:111-112 says "fra 166 e 172"; README.md:144 says "fra 166 e 174 su 1.20.1".
   - The regression run measured 176 wide from x 318 with magenta 18831 (m3d-forge.out:111). The report notes it, and the check (width ≥ 70) passes.
   - It is not caused by this task (see the check above), but the three places now disagree with each other and with the measured value.
   - Fix: widen the documented range when m3d_shots.sh is next touched; Task 3 edits it. The file is outside Task 2's list.

### Assessment
**Task quality:** Approved
**Reasoning:** Every requirement of the brief and the plan sections it names is implemented as written, and the gate passed on four variants with clean logs; I checked that evidence. The one unplanned harness change is correct and does not change the earlier sequences; the findings are documentation, test coverage and follow-ups for Task 3.
