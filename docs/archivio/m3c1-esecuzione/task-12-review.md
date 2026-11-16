# Task 12 review (ccce810..2397378)

### Spec Compliance

- ✅ Spec compliant. Each item in the brief's "Produces" is in the diff:
  - `frame/ClientRulesPacket.java:18-48`: the record has the three int fields, `ID = Ids.of("client_rules")`, and a `CODEC` that writes three `writeVarInt` and reads three `readVarInt` in field order without filtering. `of(ExoframesConfig)` returns `energy.cellCapacity`, `energy.cellSwapSeconds` and `frame.exitHoldSeconds`.
  - `frame/ClientRules.java:266-271`: `register()` matches the brief word for word (`registerS2C` with the handler calling `accept`, and `PLAYER_JOIN` calling `sendToPlayer` with `of(RedstoneExoframes.config())`).
  - `ClientRules.java:278-287, 312-320`: `accept` uses the bounds `[1, MAX_CELL_CAPACITY]` and `[0, MAX_HOLD_SECONDS]` for both second values. An out-of-range value falls back to `ExoframesConfig.defaults()`, not to the local config. The WARN text is exact, keyed by name, and written once per process through `ConcurrentHashMap.newKeySet()`. The values sit in a `volatile` field holding a `private record Received`.
  - `ClientRules.java:290-310`: `reset()` and the three getters fall back to `RedstoneExoframes.config()`. Each getter copies the volatile field into a local first, which is a correct guard against a null read between the check and the use.
  - `ClientRules.java:237-251`: the class Javadoc covers everything the brief lists: the readers (ExitKey, then CellSwapKey and the item bars), that the server never reads it, that singleplayer gets the same values, and that a dedicated server or GameTest falls back to the local config.
  - `RedstoneExoframes.java:111`: `ClientRules.register();` comes right after `FrameEntry.registerNet();` and has a one-line comment.
  - `RedstoneExoframesClient.java:215`: the `CLIENT_PLAYER_QUIT -> ClientRules.reset()` registration comes right after `Compat.frameState().registerClient();`. Nothing registers it from common init.
  - `ExitKey.java:180`: the tick now calls `ExitHold.ticksNeeded(ClientRules.exitHoldSeconds())`, and the class Javadoc at :150-157 says the seconds come from the server.
  - `docs/compat.md:47`: the `client_rules` channel is described as the brief asks.
  - `docs/compat.md:156`: the ExitKey line uses the brief's replacement text verbatim.
- Bounds check: the bounds are the same as the config's. `ConfigValidation.java:81` uses `positive(... MAX_CELL_CAPACITY)` for cellCapacity, and `:97-98` and `:161-162` use `nonNegative(... MAX_HOLD_SECONDS)` for the two second values.
- Zero seconds check: `ExitHold.java:45-47` turns 0 seconds into 0 ticks, so the 0 that `accept` allows is handled downstream.
- Step 4 check: I ran the grep on HEAD 2397378. Under frame/, compat/ and content/ only `frame/ClientRules.java` and `frame/ClientRulesPacket.java` name ClientRules.
- Tests against the brief's Step 1 values:
  - `ClientRulesTest.java:13-19`: one method carries both `@BeforeEach` and `@AfterEach`, with the brief's body.
  - `ClientRulesTest.java:35-66`: the three test cases use exactly the brief's inputs and expected values.
  - `ClientRulesPacketTest.java:13-23`: the three round-trip packets, the `readableBytes() == 0` check and the `of(defaults)` assertion are all there.
- Test count: the JUnit path was taken, so no fallback is needed and 116 is the right count under the Global Constraints table. The report shows 116 on all four variants.
- Extra item, acceptable: `docs/compat.md:56`, the "Avvio del client" line, now also lists the new CLIENT_PLAYER_QUIT registration. That line lists what the client init registers, so the doc stays accurate. Not a finding.
- ⚠️ Cannot verify from diff: nothing. The brief itself leaves real client-side reception to Task 24's real client.

### Strengths

- `ClientRules` is small and readable. One `checked` helper holds the bound, WARN and fallback logic for all three values, and the getters read the volatile field once each.
- The class respects the source rules. `frame/ClientRules` imports no client class, and `ClientPlayerEvent` appears only in `client/RedstoneExoframesClient`.
- The codec is a plain mirror of the field order, and `ClientRulesPacketTest` covers a negative value and a value above the limit, which shows the codec does not filter.
- The tests use two small helpers (`local()` and `assertRules`) without changing a single expected value from the brief.
- All touched Java lines are under 120 characters (checked with awk), comments use ASCII apostrophes, and `§` already appears in other `frame/` sources.

### Issues

#### Critical (Must Fix)
None.

#### Important (Should Fix)
None.

#### Minor (Nice to Have)

- **T12-F1, `docs/compat.md:47`.** The closing sentence "La metà client del pacchetto e il tasto si sono provati nel client vero sulle quattro varianti con `tools/client/m3b_shots.sh` (parte 3b)" now comes right after the new `client_rules` entry. Read in order, "del pacchetto" points at `client_rules`, whose client half has not been tried in a real client yet (that is Task 24). Fix: name the packet ("La metà client di `frame_state_sync` e il tasto di `frame_exit` ..."), or add "`client_rules`: client vero nel Task 24".
- **T12-F2, `src/test/java/com/toxineren/exoframes/frame/ClientRulesTest.java:68-80`.** No test checks the "one WARN per name in the process" rule that design §12.4 states ("con una riga di log una volta sola"). The report's test output shows three WARN lines, so the code behaves correctly today, but a regression such as dropping the `WARNED.add` guard would go unnoticed. The brief does not ask for this test, so the gap is only in coverage. If wanted: count the WARN lines with a log4j appender during two out-of-range `accept` calls on the same name. Because `WARNED` lives for the whole process, the test must use a name no earlier test has triggered, or check an increase of one line.

### Assessment

**Task quality:** Approved

**Reasoning:** The diff implements every requirement of the brief exactly, with correct bounds, correct fallback to `defaults()`, correct registration sites and a safe read of the volatile field. The only remarks are an ambiguous sentence in the doc and one optional test of the log-once rule.
