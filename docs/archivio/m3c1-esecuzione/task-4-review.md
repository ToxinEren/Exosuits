### Spec Compliance

- ✅ Spec compliant. Step (a) and step (c) match the brief and design §12.14 (line 650: a pure split between registry operations and the file format, with delegates under today's names and no test changed; the "entities loaded" predicate written once). Two Minor departures, both disclosed by the implementer, are listed under Issues (T4-F1, T4-F2).
  - `FrameRegistry` (new): constructor `FrameRegistry(BooleanSupplier, Runnable, Runnable)` at FrameRegistry.java:81, with no `requireNonNull` and no new strings. The package-private fields `worn`, `placed`, `unreadWorn/Placed/Frames`, `orphaned` and `movedWarned` are at :56-68. `record Placed` is at :78. `setDirty()` and `markStructural()` are at :87-98. The operations are at :100-394. I compared them line by line against the removed bodies in the diff, and the only change is `readOnly()` -> `readOnly.getAsBoolean()`. `take`, `orphan` and `contains` stay private.
  - `FrameRegistryFormat` (new): `FRAMES`, `UNREAD_SUFFIX` and `enum Outcome { READ, READ_ONLY }` are at :59-69, with a private constructor at :71. `read` (:82-178) is the old `load` body. The only changes are `data.` -> `into.`, `Placed` -> `FrameRegistry.Placed` and `CURRENT_SCHEMA` -> `FrameStateSavedData.CURRENT_SCHEMA`. The same ERROR is logged on the newer-schema or non-numeric branch, which returns `READ_ONLY` before reading anything. `write` (:260-293) is the old `:937-969`. The private helpers keep their bodies, except that `putRaw` (:300) takes the registry for `registry.movedWarned`, which the brief allows.
  - `FrameStateSavedData`: the field `private final FrameRegistry registry = new FrameRegistry(this::readOnly, this::setDirty, this::markStructural);` is at :81 and is private, so nothing outside the class can get around the read-only gate through it. `load` matches the brief's code block exactly. `write(CompoundTag)` keeps both read-only branches and then calls `FrameRegistryFormat.write(registry, tag); return tag;`. There are 19 one-line delegates, one for each operation in the "Consumes" list, from `frames` through `exit`. The three directive blocks are untouched (`HolderLookup` import, `get(DimensionDataStorage, Path)`, `save`). `isDirty()` (:351) still returns `!readOnly() && super.isDirty()`, so `this::setDirty` behaves like the old `data.setDirty()`.
  - Predicate: `static boolean entitiesLoadedAt(ServerLevel, Vec3)` at FramePlacement.java:121-124 is package-private, as the Global Constraints require, and has the brief's body. It is called at FrameEntry.java:293 and FrameRealign.java:77 (`continue` when false). FrameEntry.java:417, the floor of the step out, is unchanged.
  - docs/compat.md:67 adds both files next to `FrameStateSavedData.java`, worded exactly as in the brief. Lines 79 and 197, about the directives, still name `FrameStateSavedData`, which is correct.
  - No test file is in the diff stat.
- ⚠️ Cannot verify from diff: nothing open. The checks I ran in the checkout are listed below.
  - Brief Step 3 (are the strings the same?): I re-ran the brief's command with BASE in place of HEAD. `diff` printed nothing and exited 0, with 72 strings before and 72 after.
  - Brief Step 4 (is there exactly one `areEntitiesLoaded` call site?): `grep -rn 'areEntitiesLoaded' src/main/java/com/toxineren/exoframes/frame` prints one line, FramePlacement.java:123.
  - Unused imports after the move: I checked every import name in FrameStateSavedData, FrameRegistry, FrameRegistryFormat, FrameEntry, FrameRealign and FramePlacement against the file body. None is unused. FrameEntry still needs `ChunkPos` and `BlockPos` at :417.
  - Javadoc links to members that moved: a grep for `FrameStateSavedData#|.` followed by any moved name (FRAMES, UNREAD_SUFFIX, Placed, putRaw, ...) finds nothing in src, docs or tools.
  - Original line map of the class comment: I read BASE FrameStateSavedData.java:44-116 to check the brief's `:48-52`, `:74-102`, `:104-109` and `:111-115`. This is the source of T4-F1.

### Strengths

- The move is mechanical in the way the brief asks. Every moved log line, exception message and `requireNonNull` text is byte-identical (Step 3 check). Each substitution is one the brief names or one the move makes necessary (`FrameRegistry.Placed`, `FrameStateSavedData.CURRENT_SCHEMA`).
- The registry reaches the owner only through the three functions it receives. The `registry` field is private, so no other class in `compat/impl` can reach the live maps around `readOnly()`.
- `load` and `write` in FrameStateSavedData are now short and read exactly as the brief specifies. The old `return data;` inside the read-only branch is replaced by the `Outcome` result with no change in behaviour.
- The class comment is split by topic, and the Javadoc cross-references were updated where the target moved (`FrameStateSavedData#CURRENT_SCHEMA`, `#readOnly()`, `#structuralUnsaved()`, `FrameRegistryFormat` in `enter`). The bridging sentences contain no quotes, so the Step 3 string check stays exact.
- Step (c) also removes the two imports FrameRealign no longer uses. The predicate's Javadoc keeps the reason that matters (`getEntity` sees the chunk's entities only when the chunk is full).

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

- **T4-F1** FrameRegistry.java:84-85. The sentence "L'armatura stivata qui e' una lista di tag opachi." comes from BASE FrameStateSavedData.java:60. That line is outside `:48-52`, so under the brief's line map it belongs to the paragraph that stays in `FrameStateSavedData` ("a `FrameStateSavedData` il resto"). The implementer moved it to `FrameRegistry` on purpose, arguing it is about the records' content (report, "Scelte"). The meaning holds in either place and nothing behaves differently. The only cost is that the comment split no longer follows the brief's line map exactly. Fix: leave it, which I'd accept since the sentence fits the records it describes, or move it back to the end of the first `<p>` of FrameStateSavedData.
- **T4-F2** FrameStateAccessImpl.java:38 (outside the diff; raised by the implementer). The text "...restano nel file, identiche (vedi {@link FrameStateSavedData})" points to the class that used to state the unread-entries rule. The rule is now in FrameRegistryFormat's class comment. The link is not broken: FrameStateSavedData's new class comment (lines 2-4 of its Javadoc) points on to `FrameRegistryFormat`, so a reader gets there in one more hop. FrameStateAccessImpl is not in the brief's file list, so the implementer cannot fix this without going beyond the brief. Plan-mandated: the coordinator decides whether to allow the one-word Javadoc change (`{@link FrameRegistryFormat}`) or leave it for a later task that touches FrameStateAccessImpl, which Task 11 does for `changeWorn`.

### Assessment

**Task quality:** Approved

**Reasoning:** A faithful pure move. The bodies, strings, signatures and read-only behaviour are unchanged, the two new classes have the shape and visibility the brief asks for, and the predicate has one call site. The two issues are comment placement and a one-hop cross-reference, with no effect on behaviour.
