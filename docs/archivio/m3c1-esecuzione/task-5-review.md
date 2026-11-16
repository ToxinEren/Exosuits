### Spec Compliance

- ✅ Spec compliant. Every item in the brief's "Produces" section is in the diff, as is every test step:
  - `FrameRegistryFormat.read` returns `WITHOUT_SECTIONS` before the schema check and before anything is read, with no log line (FrameRegistryFormat.java diff :194-197). `Outcome.WITHOUT_SECTIONS` is added with Javadoc.
  - `FrameStateSavedData`:
    - field `withoutSections`, set by `load` together with `readOnlyRoot = tag.copy()` (switch, diff :412-420);
    - `reportedWithoutSections(Path)` is exactly the brief's body (diff :372-379) and runs on both exits of `get` (diff :355 and :363);
    - `createEmpty()` is package-private (diff :309-311);
    - `saveWithPlayersIfNeeded` keeps its guards, then calls `saveAll()` and `data.saveFile(dataFolder(server).resolve(NAME + ".dat"))` (diff :467-469). It does not check `noSave`.
    - The class comment and the method Javadoc give both reasons: /save-off, and why all players are saved.
  - The JUnit tests match the brief: `fileWithoutSectionsIsReadOnly`, `schemaOneFileIsStillARegistry`, `createEmptyIsWritable`; `empty()` → `createEmpty()` in FrameStateSavedDataTest and RegistryFileTest; StackTagsTest uses `createEmpty()` and `{schema: 2}`; `odd` gains `schema: 2`.
  - The game tests `quitSaveIgnoresSaveOff` and `registryFileWithoutSectionsIsReadOnly` are at the end of `generate()`, with the brief's bodies.
  - docs/compat.md matches the brief: the sentence at :78, the sentence at :258 and the new list entry are word for word.
  - The allowlist line is the brief's, with `=1`.
  - The test count goes from 98 to 100, which matches the Global Constraints table.
- ⚠️ Cannot verify from the diff: nothing open. I checked that schema-1 worlds stay readable: at 869fd4a, `save` always wrote `frames`, even when empty, so an old empty registry is not mistaken for a file without sections.

Implementer's departures, judged on the code:
1. **The NeoForge I/O worker wait in `GameTestsImpl.readRegistry`: accepted.**
   - Check: `javap` on neoforge-21.1.266-minecraft-merged. `DimensionDataStorage.readTagFromDisk` queues `lambda$readTagFromDisk$0` through `IOUtilities.withIOWorker`, and that lambda calls `IOUtilities.tryCleanupTempFiles`.
   - In the NeoForge sources, `tryListTempFiles` logs `Failed to list temporary files in {}` at ERROR on an IOException. `waitUntilIOWorkerComplete()` is `saveDataTasks.join()` followed by a reset.
   - So the problem is real, and the fix is deterministic. The condition sits in `compat/impl/`, where Stonecutter conditions are allowed, and its block is uncommented in the committed source, which matches the active version `1.21.1-neoforge`. The fix only affects a test-only helper and is documented in `GameTests` and in docs/compat.md:85.
   - Waiting from the server thread is safe: `withIOWorker` is also fed from the server thread. The other option, the test waiting itself, would need a Stonecutter condition in `gametest/`, which the Global Constraints forbid.
2. **The allowlist line placed after :43 instead of :40: accepted.** Base line 40 is `# ... il mod usa la copia precedente ...`, the comment that introduces line 41. Inserting the line after 43 keeps the registry group whole.

### Strengths

- The early return in `read` follows the brief's rule exactly. It decides on the keys being present, of any type, and checks `FRAMES`, `WORN`, `PLACED` and `SCHEMA` (FrameRegistryFormat.java diff :194).
- `reportedWithoutSections` is idempotent. The per-tick `get(server)` path logs the ERROR exactly once, because `unreadableFile` is set before the log line, and the `=1` allowlist line confirms it on four variants.
- `isDirty()` stays false for the new read-only data, because `readOnlyRoot` is set in `load` before `get` runs. Neither the game's save nor the quit save can rewrite the file, and the in-world test proves it byte for byte, with no extra files in the folder.
- `quitSaveIgnoresSaveOff` uses the dirty `Raids` data to prove that the quit save writes only the registry: a precise, observable check that no other overworld data is written. It restores `noSave` in `finally`.
- The `schema` row of the docs table and the `FrameRegistryFormat` class comment were updated so they no longer contradict the new rule.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

1. **T5-F1** FrameStateSavedData.java:202-246 (`afterUnreadFile`) and :166-171 (`get`): the recovery paths give contradictory ERROR lines for data without sections.
   - Case 1: the main file is unreadable and `_old` reads as NBT but has no sections. `RegistryFile.recover` moves the main file aside (RegistryFile.java:192) and `afterUnreadFile` logs ERROR "using the previous copy". Then `get` → `reportedWithoutSections` logs "Frame registry file <main> has no schema and no sections", naming a file that is no longer there and was not the one without sections. The session is read-only, so "using the previous copy" is false.
   - Case 2: `readItself` reads a file without sections. The log gets WARN "using it as it is", then the read-only ERROR.
   - The data is safe in both cases: nothing is written and every file is kept. Only the log misleads the operator.
   - Possible fix: in `afterUnreadFile`, treat a previous copy without sections as `UNREADABLE`, using the line that says "could not be read, nor its previous copy". This needs `recover` to know before it moves the file. Defer to the final review.
2. **T5-F2** (plan-mandated) FrameStateSavedData.java:166 and :176: `get(DimensionDataStorage, Path)` now runs `folder.resolve(NAME + ".dat")` on every call. That includes `get(server)`, which runs for every player on every tick (the `DATA_FOLDERS` memo exists to keep it cheap), so each call allocates a new `Path` even when nothing is reported. The brief gives this exact line. Possible fix: pass `folder` and resolve inside `if (withoutSections && unreadableFile == null)`.
3. **T5-F3** docs/compat.md:87: the **Condizione** line for `GameTests` lists the conditions of `GameTestsImpl` but not `readRegistry`. The method now has a loader-and-version condition (`>=1.21 && neoforge`, the I/O worker wait) on top of the existing `>=1.21` condition on the `DimensionDataStorage` constructor. Fix: add "loader e versione in `readRegistry`".
4. **T5-F4** (plan-mandated) FrameStateSavedData.java:386-388: for data without sections, `unreadableFile` is set, so the first refused change logs "Frame registry file {} could not be read: changes are not applied". The game did read that file. The brief has `logRefusedChange` name the file to restore through `unreadableFile`, but the wording is inaccurate for this case. Possible fix: a separate message when `withoutSections` is set.

### Assessment

**Task quality:** Approved

**Reasoning:** The diff implements the brief to the letter, with tests that check real behaviour: the JUnit read-only and writable cases, the bytes on disk, and the dirty Raids marker under /save-off. Both departures are verified and justified on the code. What remains is log wording, a per-call `Path` allocation the brief mandates, and one documentation line, all Minor and fit to defer.
