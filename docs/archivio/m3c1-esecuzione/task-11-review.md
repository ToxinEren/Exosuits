# Task 11 review (d1e6bcc..ccce810)

### Spec Compliance

- ✅ Spec compliant. I checked every "Produces" item and step of the brief against the diff:
  - `compat/FrameIndicators.java:14-21`: record, compact constructor `requireNonNull(energyState, "energyState")`, `of(FrameEffects)`.
  - `RedstoneExoframes.java:33,76-94`: `volatile rules` declared after `config`, `rules()`, and `setConfig` assigns `config` before `rules`. Javadoc as the brief asks. Checked: `git grep "new FrameRules" -- src/main` finds only the two lines in `RedstoneExoframes`. Checked: `init()` calls `setConfig(loaded.config())` once, right after `ConfigFile.load` (`RedstoneExoframes.java:46`).
  - `FrameStateSyncPacket.java`: three components and a compact constructor with three `requireNonNull` calls plus the both-or-neither `IllegalArgumentException`, with the exact message. On the wire: UUID, flag, then tag/`canSprint`/`canSwim`/`writeEnum` only when present. The read is symmetric, and a null tag counts as absent with the indicators read and discarded. Javadoc gives the new format.
  - `FrameStateClientCache.java`: private `Entry` record, `accept` with `orElseThrow()`, `view` unchanged in behaviour, new `indicators(UUID)`, `clear` unchanged.
  - `SyncCounter.java`: package-private, static methods only, `ConcurrentHashMap`, `count` returns 0 when the player was never synced.
  - `FrameStateAccess.java`: `clientIndicators`, `changeWorn`, `onSynced` with Italian Javadoc. The "Sincronizzazione" paragraph is updated: indicators depend on the state alone, `changeWorn` is listed among the writes, and "4-5 volte al secondo" is kept.
  - `FrameStateAccessImpl`:
    - `syncTo` runs in the brief's order: `SyncCounter.increment`, then the registry read, then tag and indicators together from `RedstoneExoframes.rules().evaluate`, then `sendToPlayer`, then the listeners.
    - `changeWorn`: requireNonNull, then the read-only branch with `logRefusedChange`, then a missing record returns false without `syncTo`, then `data.changeWorn` with `syncTo` in `finally`.
    - `clientIndicators` and `onSynced` are present, and `PLAYER_QUIT` calls `forget` after `saveWithPlayersIfNeeded`.
  - `FrameRegistry.changeWorn` (`FrameRegistry.java:149-159`) and the one-line delegate in `FrameStateSavedData`: `markStructural()` runs in `finally`, and the change never runs when the registry is read-only or the record is missing.
  - `GameTests.syncCount` has Javadoc, and `GameTestsImpl` implements it with no directives.
  - `GameTestKit.setStored` throws both exceptions with the exact messages.
  - JUnit:
    - `FrameStateSyncPacketTest` has its three tests as specified, including the 17-byte check.
    - `FrameStateSavedDataTest` has the three new tests and the new line in `assertReadOnlyAndUnchanged` (test file line 628 at HEAD, inside the helper at :618).
    - The two read-only roots are folded into one loop with the same assertions.
  - In-world tests `changeWornNeverCreatesARecord` and `changeWornChangesAndSyncsOnce` are at the tail of `CarryOverGameTests.generate()`, with the assertions the brief lists.
  - `docs/compat.md` has all five edits: `Net` "Uso", "File", "A cosa serve", "Differenze chiuse", and `GameTests`.
  - Step 3 needed no fallback; the commit body says so ("Codec provato in JUnit sulle quattro varianti ... nessun ripiego in mondo"). The commit carries a `gpgsig` header, like the base commit.
- Extra, accepted: "tre"→"quattro" metodi di scrittura and "le quattro scritture" in `docs/compat.md:68` and in the class Javadoc of `FrameStateAccessImpl`. This keeps the page and the Javadoc consistent now that `changeWorn` is a fourth write.
- ⚠️ Cannot verify from diff: none.

Focused checks I ran, each tied to a named risk:
- Risk: `syncTo` now runs `FrameRules.evaluate` on every worn state, including at join and respawn, so a state it cannot handle would make those events throw. Check: `FrameRules.evaluate(FrameState, FrameContext)` at HEAD handles a missing cell ("Senza cella lo stato energetico e' EMPTY") and only requires `ctx` to be non-null.
- Risk: listeners are kept on an instance field, so they would be lost if `Compat.frameState()` were not a singleton. Check: `Compat.java:82`, `static final FrameStateAccess INSTANCE = new FrameStateAccessImpl()` in a holder.
- Risk: the rule "apostrofi ASCII" in comments. Check: the added Java lines have no non-ASCII characters except `§`, which 24 main files already used at base.

### Strengths

- `syncTo` (`FrameStateAccessImpl.java` hunk at :505-522) counts before reading the registry, which is why NeoForge 1.21.1 still counts a sync whose send is skipped. Building the two Optionals in one `if (record != null)` block makes the packet's invariant structurally obvious.
- The codec's read path (`FrameStateSyncPacket.java` read) always consumes the three indicator fields after a present flag, even when the tag reads back null, so the buffer never desyncs. `absentStateCarriesNoIndicators` pins the absent wire size at 17 bytes.
- `FrameRegistry.changeWorn` mirrors `mutateWorn`/`updateWorn`: same guard shape and `finally` placement. The JUnit `changeWornMarksStructuralEvenWhenTheChangeThrows` proves the `finally` is really there.
- The in-world `changeWornChangesAndSyncsOnce` saves the registry and asserts it is clean before the change, so the final `registryChangeUnsaved` cannot pass because of the earlier `enter`. It checks the exact +1/+2 sync deltas through both `changeWorn` and `setStored`.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

- **T11-F1** `docs/compat.md:68` ("A cosa serve" of `FrameStateAccess`): the sentence announces "uno dei quattro metodi di scrittura", but `clientIndicators(player)` and `onSynced(listener)`, which are not writes, now sit between `changeWorn` and `clear`. The `clear` clause also hangs after "(lo usa `frame/SuitTick`); `clear` rimuove lo stato...". The page reads as if `onSynced` were a write and `clear` part of the `onSynced` sentence. Fix: keep the brief's text unchanged but move the two sentences for `clientIndicators` and `onSynced` after the `clear` clause (and after "le quattro scritture non fanno nulla"). They would still come after `mutate`, as the brief asks.
- **T11-F2** `src/main/java/com/toxineren/exoframes/compat/impl/FrameStateClientCache.java:29-49`: no test covers the cache's new behaviour. The `clientIndicators` contract ("vuoto prima del primo pacchetto con uno stato e dopo uno stato assente") depends on `accept` storing `Entry(state, indicators)` and removing it on an absent packet. Design §12.4 leaves "the client really receives it" to the real client, and the brief asked for no such test, so this is a coverage gap, not a missed requirement. Fix, if wanted: a JUnit in the same package. `accept` a present packet, assert that `indicators(PLAYER)` equals what was sent. Then `accept` an absent packet and assert that `indicators(PLAYER)` and `view(PLAYER)` are null.

### Assessment

**Task quality:** Approved

**Reasoning:** Every requirement in the brief is implemented as written, at the right layer and in the specified order. The `finally`/structural semantics and the both-or-neither packet invariant are enforced in code and proven by JUnit and in-world tests. The only findings are a documentation readability issue and a coverage gap for the client cache.
