# Final review — part 4a-2 "Il banco al lavoro" (f5b3ac8..d0d454b, 9 commits)

Reviewer scope and method. I read the plan in full and the spec's 4a sections (§5.2-§5.7, §5.11, §5.13-§5.19, §6, §8, §9, §13, §14), the ledger with rulings A1-A13 and the five task reviews. I read the package in passes: every production hunk line by line (station/, frame/, content/, command/, client/, compat/, core/, the language files), the docs as word diffs, the tests by skimming. I also checked these outside the diff at d0d454b and f5b3ac8, with `git show` and `git grep` only:
- `ItemDataAccessImpl.frame` and `setFrame`;
- `FrameStateCodec` (the `reinforced` key) and its JUnit round trip;
- `FramePlacement.spawnView` (the entity UUID is the `frameId`);
- `Handouts.PICKUP_DELAY`;
- `StationMenu.setView` and `viewFor`;
- every caller of the frame-item data at base.

For Ruling A12 I disassembled the vanilla `Gui` and `Screen` of 1.20.1 and 1.21.1 from the Loom cache, and the Architectury 9.2.14 and 13.0.11 jars, with javap in the scratchpad. I ran nothing with Gradle, the client or the server.

What I skimmed rather than read line by line:
- in-world tests: the `StationActionGameTests` bodies (names, assertions and cleanup by grep), `FrameItemGameTests` (five bodies read), `StationMenuGameTests` and `GameTestKit` (changed lines only);
- JUnit bodies: `StationActionTest` (assertion lines) and `StationLayoutTest` (method list); for `TickGateTest`, `StationViewTest` and `SuitViewTest` I counted the `@Test` methods but did not read the bodies;
- `m4a_shots.sh`: header and thresholds read; the check functions and step list skimmed;
- `tools/client/README.md`: only the checklist pointers checked;
- `m4a-stazione.md`: read in full, but steps 19-28 cut at about 1200 characters per line;
- the ten reference PNGs: not viewed (the Task 4 reviewer viewed all ten).

CI was checked with `gh run list --branch m4-stazione` (the GitHub connector failed, the `gh` CLI did not). The `build` workflow is green on every pushed head of this part, d0d454b included (run 46078467300, 22:44 UTC).

### Strengths

- **One rule, two entry points, as §5.7 asks.** `frame/FrameFitting` holds mount, cell insert/swap, unmount and cell removal. The same sequence holds for each: check, build the item, one `updatePlaced`, then deliver, consume, refresh and sound, only after success. `FrameInteractions` lost about 100 duplicated lines and now only picks the slot. The hands' behaviour is unchanged, including the arm/leg fallback and `SLOT_TAKEN` with both sides taken. `ExoframeGameTests` passes untouched.
- **`StationActions.press` follows design §5.6 exactly.** The order is decode, alive, free selection, 4b numbers ignored without a turn, write turn, record and `canUse`, then the action. `FAILED` is reported in one place (`StationActions.java:97`). `operatorTogglesAnyLock` pins `canUse` before the lock rule (Rulings A5/A6).
- **The client evaluates no rules.** It predicts only the selection, like vanilla menus. The lock changes only through the server's view, and the real-client checks `lucchetto`, `vista` and `rinforzato` measure that round trip on four variants. The server re-checks everything, and one write per tick per menu comes from one `TickGate` type that also gates the view: no sentinel, the limit restarts when the counter goes back, JUnit-tested.
- **Review Focus 1 holds at the code level.** A press on any widget is remembered per mouse button. Its drag and release never reach `AbstractContainerScreen`, so a cursor stack can neither be thrown nor dropped into a slot. A press of the same button elsewhere clears the bit, so a lost release cannot swallow a later vanilla release. The `montaggio` check measures the release outside the screen.
- **The frame item is safe and backward compatible.** The state is read before `removePlaced`, and the item is given only after removal succeeds. A frame with nothing to carry gives a plain item equal to a crafted one. A data item with pieces or a cell is refused with `FULL`, before the room check. The tooltip shows only the flags. At f5b3ac8 nothing reads frame-item data, so earlier jars ignore it, as §5.14 requires. `ExoframeItem.pickedUp` is the only place that creates a frame item.
- **Save data is unchanged.** There is no schema bump. `reinforced`, which this part writes for the first time (debug command, placement from item data), is an existing schema-2 key: `FrameStateCodec` writes it at :74 and reads it at :172, and `FrameStateCodecTest` round-trips it. The refusal seam's counter (`FrameStateSavedData.placedUpdateRefusals`) is transient and outside `readOnly()` and the save.
- **Hygiene.** My script over every added line found:
  - no Stonecutter directive outside `compat/impl`;
  - no plan, decision, ruling or task ID in `.java`/`.sh`;
  - no vendor or model name;
  - no non-ASCII character in Java except `§`;
  - no Java line over 120 characters;
  - no new `isCreative()` in production code;
  - no new fill or colour in the station's client classes.

  The new visibility is `StationScreen.TEXTURE`, made package-visible as planned, and `ExoframeCommand.UNLOCK_DONE`/`LOCK_DONE`, made public for the station (triage item 11). No new mixin, no new packet, and `tools/art/` is untouched.
- **No personal data.** A grep of every added line found no e-mail address and no home path (`/home/`, `/root/`, `C:\Users`). The mock players are "test-mock-player" and the real-client sequence's user is "Exo". The new PNGs are docs, outside the jars, and Task 4 checked them for metadata. I did not re-run `check_jars.sh` (g); "OK jars" at head rests on the controller's logs.
- **The counts check out against the source.** The `add(tests,` registrations give 256 in-world tests at head and 235 at base (+11, +5, +5). The diff adds 18 `@Test` methods and removes none: `StationActionTest` 6, `TickGateTest` 4, `StationViewTest` 1, `StationLayoutTest` 6, `SuitViewTest` 1. So 389 + 18 = 407; the 389 itself comes from the plan and the controller's result files.
- **The documentation is honest.** The checklist separates what the cloud proved from what only the PC can. It turns every plan error found during execution into a ruling (A8-A11). It discloses the hidden messages as a known limit, with an owner question.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

1. **The station's action-bar messages cannot be seen while the station is open (Ruling A12).** Owner: part 4b, as its first task; this does not block the merge.
   - **Where.** `station/StationActions.java:97` (`FAILED`) and `:121-122` (the lock replies), and `frame/FrameFitting.java` via `FrameInteractions.tell` (`SLOT_TAKEN`). All of them use `displayClientMessage(…, true)`.
   - **What, measured.** Vanilla draws the bar after translating to `guiHeight − 68`, with the text at y −4, in both versions [javap of `Gui.render` 1.20.1 and `Gui.renderOverlayMessage` 1.21.1]. The text therefore spans `guiHeight−72 … guiHeight−63`. The station's bottom edge is `(guiHeight−232)/2 + 232`, so the text is:
     - fully covered for guiHeight ≤ 358;
     - partly covered at 359-375;
     - clear of the rectangle from 376 up.

     At Auto scale `Window.calculateScale` keeps guiHeight below 360 for every window of at least 640×480. So at Auto the three messages are covered at every common resolution (720p, 1080p, 1440p, 4K, 16:10, ultrawide). Only manual small scales put the bar below the screen, for example 1080p at scale 2 (guiHeight 540). On 1.21.1 the menu blur (`Screen.renderBackground` → `renderBlurredBackground` [javap]) probably blurs it there as well; I did not verify that.
   - **Why it matters, and how much.** The spec makes these messages the station's feedback: §5.6 for the failed write, §5.7 for the taken slot, §5.11 for the lock replies. The opaque full-size screen (§5.3 and the 4d art) defeats them, so this is a spec gap, not a coding slip. The harm in this part is small:
     - the lock already answers with its sprite (from the server's view) and the iron-door sound;
     - a taken slot is visible in the panel, and nothing happens, which is vanilla-like;
     - only `station.failed` carries information shown nowhere else. That case (a read-only registry) is an administrator failure that also logs an ERROR, and nothing is lost or duplicated.
   - **Why 4b, not this part.** 4b adds refusals whose message is the only explanation: §5.8 "Questo pezzo non ha bisogno di riparazioni" and §5.10 "Il preset contiene un colore che non viene da una tinta". 4b also owns the cost row, the natural in-screen place for such text, which `RESERVED_4B` keeps this part out of. The owner question is already asked in the checklist, and design §6 (DEC-48) makes PC findings the next part's first task. There is no cheap sound fix inside this part:
     - drawing vanilla's private overlay message on top of the screen needs a mixin accessor, and the Global Constraints rule out new mixins;
     - a client hook is loader-dependent. Architectury's `ClientSystemMessageEvent` carries no overlay flag, and it is fired only from the Fabric `MixinChatListener`. In the Forge 9.2.14 and NeoForge 13.0.11 jars no class other than the event itself references it, so it is unverified there and probably never fires;
     - the client cannot predict `FAILED`.
   - **The smallest sound fix, for 4b.** Make the station's own reply travel with the station's data. Either add a field to `StationView` with a serial, so a repeated reply re-sends, or add a small S2C reply packet; a new packet is a design change against §5.13 and needs the controller's ruling. The screen then draws the reply inside its own rectangle for about 3 s, where the owner's answer says (the cost row or the title row). This is loader-neutral, pure-Java testable (record and codec), checkable in-world (the reply in the last sent view) and measurable in `m4a_shots.sh`/`m4b_shots.sh` with a press on an occupied slot. Until then, the checklist's workaround (Esc right after the click) and the owner question are the right holding position.

#### Minor (Nice to Have)

1. **`docs/compat.md:256` and `:257`** say the station's replies go "sulla barra delle azioni" and do not mention that the open screen covers that line. The behaviour is documented only in the checklist (`m4a-stazione.md:31`, `:101`, and the owner question). Add one clause in each bullet, for example "con la schermata aperta la barra sta sotto la schermata: limite noto, domanda al proprietario nella checklist della 4a".
2. **Spec, not implementation: design §5.14 (`docs/superpowers/specs/2026-11-19-m4-stazione-design.md:328`)** lists the keys M4 writes as "aspetto, durata, pezzi, cella, `unlocked`". This part also writes `reinforced`, through `/exoframe debug reinforced` and through placement from item data. It is an existing schema-2 key that an M3 jar reads (`FrameStateCodec.java:74`, `:172`), so there is no compatibility impact, but the list should name it.
3. **`station/StationMenu.java:278`.** The `clickMenuButton` Javadoc says it returns true "se la scelta e' cambiata". It returns true for every selection press (`SELECTED`), even when the selection is already that value. Say "per una scelta o un telaio cambiato".
4. **Doc drift still present at head, all from the deferred list**:
   - `compat/GameTests.java:71-72` says `messagesTo` is "vuota per un altro giocatore", which contradicts `operatorPlayer`'s Javadoc in the same interface;
   - `frame/SuitDamage.java:36` and `frame/SuitTick.java:53` say "i giocatori finti dei test rispondono sempre vero", which is now true of `mockServerPlayer` only;
   - `core/station/StationView.java:126` claims a server use of `withSelection`;
   - `gametest/StationMenuGameTests.java:72` has a 16-character mid-paragraph line.

   One documentation commit can fix all five lines; it can ride with 4b's first task.

### Deferred minors: triage

1. `FrameFitting.java:25`/`:66`/`:134`/`:157`, the snapshot precondition: **can wait**. No caller passes a stale `PlacedFrame`: `StationActions.press` and `FrameInteractions.onInteract` read the record in the same synchronous call. Add the one-sentence Javadoc ("`frame` letto dal registro nella stessa chiamata") when 4b adds writers beside it, before anyone caches a `PlacedFrame`.
2. `StationView.java:126`, `withSelection`'s server claim: **can wait**. It is still stale at head, but documentation only (Minor 4).
3. Short Javadoc lines: **can wait**. They are partly closed at head: `GameTestKit` near :89 and `StationLayout.java:24-25` are reflowed. Only `StationMenuGameTests.java:72` remains, and it is cosmetic.
4. Duplicated test helpers: **can wait**. `StationActionGameTests.java:714` `record` duplicates `StationMenuGameTests.java:1061` `placedRecord`, and `withPieces` (:705) generalises `helmetAndTorso` (:1068). These are test-only accessors.
5. The javac deprecation notes: **can wait**. They are pre-existing: they name `StationBlock`/`ChargerBlock`, which this diff does not touch, and a 15 November log already had them (Task 2 ⚠️6). One `-Xlint:deprecation` compile at base would close the question; it is not a merge matter.
6. `docs/compat.md:87`, the `GameTests` "Condizione" line: **closed at d0d454b**. It now names `mockPlayer`, shared by `mockServerPlayer` and `operatorPlayer`.
7. `GameTests.java:71-72`, the `messagesTo` Javadoc: **can wait**. It is still stale at head, but it is a test-only seam, and `operatorPlayer`'s Javadoc states the truth (Minor 4).
8. `SuitDamage.java:36` and `SuitTick.java:53`: **can wait**. They are still stale at head, but the rule they justify (never `isCreative()`) still holds because of `mockServerPlayer` (Minor 4).
9. The two untested paths: **can wait**.
   - The lock's `FAILED` branch (`StationActions.java:119`) feeds the same central message (:97) that `refusedWriteTakesNothingAndSaysSo` covers for mount and unmount.
   - The `debug.reinforced.done` reply is an operator debug string whose key is in both language files.

   An assertion inside existing tests would close both without changing the count.
10. `StationScreen.java:304`, the lock click rule inline: **can wait**. It is one ternary on the server's `canToggleLock`, and a wrong client could only send a 26 that the server refuses (`othersCannotToggleTheLock`).
11. `StationActions.java:3` importing `command/ExoframeCommand`: **can wait**. There is no cycle, and the constants are compile-time. Revisit if 4b shares more replies between command and station.
12. The `m4a_shots.sh` and README credit for the pick-up and re-placing tests: **closed**. `FrameItemGameTests` exists at head, and the header's "non prova" line is true.
13. `ExoframeItem.useOn` without a player, untested: **can wait**. Only another mod's automation reaches it: vanilla dispensers do not call `useOn` for this item. The branch is small (`ExoframeItem.java:138-143`).
14. `FrameItemGameTests.java:353` `cleanUp`, a per-class helper: **can wait**; it is test-only.
15. `ExoframeItem.NO_ROOM` (:36) outside the language-file test: **can wait**. It predates this part, and the key is present in both files. Adding it to `TEXT_KEYS` would be a one-word change if that file is touched again.
16. The `tools/client/README.md` pointers: **closed**. Line :252 points at `m4a-stazione.md`. Line :170 still names `m4a1-banco.md`, but on purpose, as the history of the 4a-1 shots.
17. `m4a-stazione.md:182`, "subito l'elmo si raccoglie": **can wait, practically no action**. The pickup delay is 40 ticks (`Handouts.PICKUP_DELAY`). It has run out by the time the assistant presses Esc, looks down and types `/clear`.
18. `m4a-stazione.md:99`, "nello stesso mondo": **can wait**. Steps 25 and 26 each state their precondition and how to rebuild it ("Serve il telaio armato sopra la stazione (se non c'è: …)").

### Declined to judge

- **Frame items that carry an owner but are locked.** An operator in creative, or a server with `server.ownerOnlyFrames` off, can pick up someone else's locked frame; placed again, it is that owner's locked frame, which the placer may not be able to use. §5.11 names these as administrator cases, so I treated them as intended.
- **A second write in the same tick is dropped silently (`THROTTLED`), for example under server lag.** §5.6 (3) mandates this.
- **A refused action still takes the tick's write turn.** The plan's order fixes this ("L'ordine di `press`").
- **An operator in survival who locks someone else's frame from the station sees their own screen close** with "Il telaio non è più disponibile alla stazione", because they can no longer use it. This follows from §5.2 and §5.11 as written.
- **The one-tick selection flicker.** A view the server built before it processed a selection can briefly undo the client's prediction. This is vanilla-like and allowed by §5.6 ("vince il server").
- **On the client, `ExoframeItem.useOn` reports success (arm swing) even when the server refuses with `FULL`.** This is the existing `NO_ROOM` pattern ("Sul client dice solo che l'uso e' riuscito").
- **The owner's UUID is readable in the item's NBT by anyone who inspects the stack.** The spec forbids only showing it in the tooltip, and UUIDs are public in the game.
- **In creative, an item with owner data can be placed repeatedly** (no consumption). This is the 3b creative convention.
- **The WARN for a tier without an item repeats once per click** (at most one per tick through the write gate). This is the hands' existing behaviour, allowlisted for the test.
- **Whether the 1.21.1 menu blur also hides the bar at small manual scales.** I verified the `renderBlurredBackground` call, not that the HUD lies in the blurred target. It does not change the A12 verdict.

### Recommendations

1. **Merge.** Nothing in the branch needs to change first.
2. **Record the carry-overs durably when closing the part.** `.superpowers/` has no tracked files. The A12 limit itself is in the tracked checklist, but its analysis, its assignment to 4b and the can-wait list live only in the ledger. Add a design section "15. Riporti della parte 4a-2", like §13 and §14, with:
   - A12, for 4b's first task, with the analysis above;
   - the still-open minors (triage 1-5, 7-11, 13-15);
   - Minors 1-3 of this review.

   Triage 17 and 18 need no record: they are wording in the checklist, a tracked file, and 17 needs no change.
3. **For the 4b plan.** Decide where the station's replies go from the owner's answer, then send them with the station's data, not through the action bar, and give the new path a measured real-client check (a press on a taken slot). Do not build on Architectury's `ClientSystemMessageEvent`: it is fired from the Fabric mixin, and no class in the Forge 9.2.14 or NeoForge 13.0.11 jars references it, so on those loaders it is unverified and probably never fires.
4. **One documentation commit**, Minors 1, 3 and 4, whenever convenient (it can ride with 4b's first task).
5. **About the plan itself.** Four of its expectations did not match the code or the spec:
   - `operatorTogglesAnyLock` against §5.6 and the always-creative mock (A5, A6);
   - checklist step 21, since stowed armour stays only on death (A8);
   - step 19's chat with a screen open (A9);
   - the allowlist count (A13).

   It also missed the opaque-screen/action-bar contradiction (A12). A short plan-writing check would have caught most of them: for every checklist step and test expectation, name the code line that makes it true.

### Assessment

**Ready to merge?** Yes

**Reasoning:** The code matches the plan and Rulings A1-A13, the cross-task contracts hold (`press` ↔ `FrameFitting` ↔ the hands, one `canToggleLock`, the click rules ↔ the server re-checks, `pickedUp` ↔ `useOn`), and I found no correctness, duplication, save-data or compatibility defect. The one Important finding, the station's replies hidden behind its opaque screen, is a low-harm spec gap that 4b should fix first, with the owner's answer, after it is recorded in the design.
