# Final review — part 4a-1 "Il banco" (b82a8da..a9cb5cb)

Reviewer: final whole-branch review, read-only. Status: COMPLETE.

## How the package was covered

- Requirements read whole: the plan (Global Constraints, Review Focus, the six tasks, the Decisioni), the design
  (§1-§6, all of §5.1-§5.20), the ledger with rulings P1-P26 and every deferred minor, and the Task 6 review's minors.
- Package `final-review-b82a8da..a9cb5cb.diff`: commit list and stat (lines 1-128) and the file index. New files
  were read whole at HEAD (the working tree is a9cb5cb and clean, so a new file at HEAD is its whole diff). Modified
  files were read as diffs (`git diff b82a8da a9cb5cb -- <file>`, or the package ranges for EntityBlockBase,
  ContainerScreenBase and GuiEntityScaleImpl, 1883-2064). The two docs with very long lines (README.md and
  docs/compat.md) were read at HEAD section by section plus a word diff.
- Code read whole: StationMenu, StationBlock, StationFinder, StationReach, StationOpening, StationViewCodec,
  StationViewPacket, StationNet, StationView, CompatBlock, BlockBase, EntityBlockBase, CompatEntityBlock,
  ContainerScreenBase, CompatContainerScreen, GuiEntityScale and its Impl, StationScreen, StationLayout, SuitPreview,
  ModMenus, StationItem, StationPart, StationMenuGameTests, StationBlockGameTests and StationViewCodecTest.
  Read as diffs: ClientCompat, ExoframeEntityRenderer, RedstoneExoframesClient, ModBlocks, ModItems, ModCreativeTabs,
  RedstoneExoframes, ConfigValidation, ExoframesConfig, FramePermissions, GameTestKit, DataGameTests, the two config
  tests, the lang files, the blockstate, the models and the 1.21.1 data files. JUnit classes: test names and assert
  counts, plus StationLayoutTest's opening.
- Context read outside the diff: SuitDraw, SuitView.of, Piece, Appearance, EnergyCell, PieceItem.fromPiece,
  CellItem.fromCell, PieceSlot, PieceCondition, NetImpl (S2C registration), GameTestsImpl.mockServerPlayer,
  LangFilesTest, ChargerGameTests' language-key test, and ConfigValidation.positive.
- Tools: client_shot.sh (diff), m4a1_shots.sh (whole), measure_lib.sh (whole), m3d_shots.sh (diff), check_jars.sh
  (diff), gen_data.py (diff) and gen_placeholders.py (diff). tools/README.md was read as a word diff, and
  tools/client/README.md by grep plus a full read of the m4a1 paragraph and table (:153-165). Those two READMEs were
  not read line by line.
- Checklist docs/test-in-gioco/m4a1-banco.md read whole. Every quoted mod string was checked against the lang files.
  The base-game Italian labels were checked against the vanilla asset `minecraft/lang/it_it.json` in the Loom cache
  (object 2cc820c6..., the same object in the 1.20.1 and 1.21.1 asset indexes).
- Reference shots:
  - opened 1.21.1-neoforge-stazione-colori-suoi, 1.20.1-forge-girata, 1.20.1-forge-suggerimento-torso and both
    stazione-posata shots. The posata shots match checklist step 4 and the "Già verificato" text: the main half is
    on the left with the red light, the side half on the right with the grid, cut by the right edge.
  - All ten were scanned for PNG metadata chunks (tEXt, iTXt, zTXt, eXIf, tIME). Each holds only IHDR, IDAT and
    IEND, at 960x540.
- javap on the cached vanilla 1.20.1 and 1.21.1 jars (Mojang names):
  - `AbstractContainerScreen.mouseReleased`: the -999 and PICKUP path, and no super call;
  - `GameRenderer.render`: the "Rendering screen" crash wrapper;
  - `BufferBuilder`: the vertex-completion checks.
- Grep checks: ledger IDs in src/docs/tools; Java lines over 120 characters (counted in characters); non-ASCII
  characters in added Java lines (only `§`, as in the existing code); `isCreative()` and debugging leftovers in
  added code; vendor names and the owner's name across the tree. Also checked that this part makes no registry
  writes, raises no schema, adds no mixin and adds no allowlist line.
- Not read: docs/superpowers/** support files (anteprime.py, layout.py, the ricognizioni), which are requirements
  material.
- Read-only: no Gradle, no client and no server were run; only git, grep, javap and short read-only Python scripts.
  The only write was this report. Checkout state at the end: HEAD a9cb5cb62b450f4dcd2abc54f405b05ce982496b on
  m4-stazione; `git status --short` prints nothing; .superpowers/ is git-ignored.

## Strengths

- **Seams are minimal, in the right place, and documented.**
  - `BlockBase` is the only click and `playerWillDestroy` translation for both block bases.
  - `ContainerScreenBase` closes the background difference.
  - `GuiEntityScale` (P11) carries a javap-backed reason for the different normal handling.
  - Every Stonecutter condition stays in compat/impl. No client class is named from common code (StationNet,
    StationMenu and StationBlock are clean), and the dedicated server ran on all four variants.
- **Server authority is respected throughout.**
  - The view is built on the server, including `ownerOnly`, `free` and `canToggleLock` (DEC-23).
  - The test button goes only through the vanilla button packet, which keeps vanilla's menu-id, spectator and
    `stillValid` checks. There is no new C2S packet.
  - A station_view payload sent by a client has no receiver on the server (NetImpl registers S2C receivers only in
    a client environment).
- **`stillValid` is careful.**
  - The checks are ordered, and the silent reasons come first.
  - The FRAME_GONE message is said once, and the menu stays invalid once the frame is gone.
  - The dimension is checked first, and an exception is logged once per menu and closes the menu (StationMenu.java
    :363-398).
  - Each close reason has a test. Review Focus 1 (the cursor stack returns) is proven in-world and covered for death
    and kick on the PC.
- **`broadcastChanges` follows design §5.13 exactly** (StationMenu.java:307-347).
  - It sends only when the view changes, by record equality, and at most once per tick.
  - The limit restarts if the server's tick counter goes back, and an exception is logged once per menu.
  - `viewsSent` and `lastSentView` give the tests a meaningful diagnostic.
- **Review Focus 3 is met.** The client constructor reads everything immediately, and `openingCarriesTheFrameView`
  releases the buffer before comparing.
- **The block follows the vanilla bed pattern precisely.**
  - `playerWillDestroy` acts only from the side half, and only the main half has loot. Explosions drop at most one
    station, pistons are blocked, and adjacent stations stay independent.
  - P17's lone-side-half hardening is in place, with a test.
  - All of this is backed by 10 in-world tests that break blocks the way a player does.
- **The tests exercise real game paths:** the vanilla button-packet handler, real clicks, real `destroyBlock`,
  `menu.clicked` for quick move and the real codec. They clean up in `finally`. The JUnit names and counts match the
  plan (27 new: 331 + 27 = 358).
- **The real-client sequence measures meaningful things.**
  - Its checks are `anteprima`, `pulsante` and `rotazione`; P19 strengthened the rotation check so that it rejects
    a back view.
  - Inside the screen, the shots are pixel-identical across the four variants for the front view; with the suit
    turned sideways, the light differs by at most 1/255 between 1.20.1 and 1.21.1.
  - P12 moved the shared measure helpers into measure_lib.sh, so there is one copy.
- **Config backward compatibility is right and tested.**
  - A 3c file without `station` loads silently and is not rewritten: JUnit, plus the dev dedicated server on four
    variants with 3c-era files.
  - `"station": null` gives exactly one problem, and NaN or infinity falls back to the default.
- **No schema bump, no registry write, no new mixin, no allowlist line, no `isCreative()` in new code, and no Java
  line over 120 characters.**
- **The docs are thorough and, with the exceptions below, true to the code.** That covers the compat.md entries for
  every seam, the API table rows, "Menu e schermate" and the "Limiti noti" line. The checklist is concrete enough
  to be followed literally, and its riskier steps (the wither death, /kick) were validated in the cloud.

## Issues

### Critical (Must Fix)

None.

### Important (Should Fix)

**I-1. The release that ends a rotation drag still reaches the base game (ruling P22, scheduled).**
StationScreen.java:205-212.
- **What's wrong:** `mouseReleased` always calls `super`. With a stack on the cursor, releasing a rotation drag
  outside the screen throws the stack (PICKUP at -999). Releasing over a slot places it. Checked with `javap -c` on
  the cached vanilla 1.20.1 and 1.21.1 jars: `AbstractContainerScreen.mouseReleased` loads `-999` for a click outside
  and, with a non-empty `getCarried()`, calls `slotClicked(..., ClickType.PICKUP)`.
- **Status:** confirmed still present at HEAD, and the ruling's fix is right.
- **Guidance for the fix wave:**
  - If `button == LEFT && rotating`, set `rotating = false` and return `true` without calling `super`; otherwise call
    `super`. Skipping `super` is safe:
    - per javap, vanilla `mouseReleased` makes no `super` call on either version: it only finishes quick crafting,
      double-click and slot clicks;
    - our `mouseClicked` (:179-184) never passed the press to vanilla, so none of that state was started.
  - Optional hardening: in `mouseClicked`, set `rotating = false` for any left press outside the clip, so that a
    lost release cannot leave rotation armed.
  - Update the Javadoc at :205 ("il rilascio va comunque al gioco base" becomes false).
  - Add a check, because nothing automated covers this path. Either a sub-step of checklist step 12 (a pile on the
    cursor, drag inside the preview, release outside the screen and over a slot: the pile stays on the cursor and
    nothing falls), or a cloud phase after the last measured shot. A cloud phase could pick ingots onto the cursor
    with `@clic`, drag from the preview to (100, 250), press Escape, and then check `execute unless entity
    @e[type=item]` plus the ingots back in the inventory.
  - Land this together with the step-12 wording fix (deferred minor T6-3), since both touch step 12.

**I-2. P24 as written covers only part of the owner's name in the current tree (pre-existing, not introduced by
4a-1).**
- **What P24 names:** docs/arte/README.md ("8 lines") and docs/audit/mjolnir-armor-audit.md:3.
- **What is at HEAD:**
  - docs/arte/README.md has 9 lines with the name.
  - The audit has 6: :3, :16, :595, :601, :604 and :637.
  - Also current files:
    - code comments: src/main/java/com/toxineren/exoframes/client/SuitScale.java:13 and
      src/test/java/com/toxineren/exoframes/client/SuitScaleTest.java:63;
    - tools/art/README.md:174;
    - tools/art/designs/frame.py (5 lines), scrap.py (4) and _legs.py:1;
    - the earlier PC checklists: docs/test-in-gioco/m1-setup.md:3,83, m2b-compat.md:3, m3a-vedere-il-telaio.md (5),
      m3b-entrare-e-uscire.md (4) and m3c-giocarci.md (5).
  - Historical docs/superpowers/** files hold about 70 more lines. That includes the owner's full name next to an
    AI vendor name at docs/superpowers/specs/claude-doc-2026-11-04-redstone-exoframes.md:3.
  - Related vendor-name mentions in current docs: README.md:11 (that file's name) and
    docs/audit/mjolnir-armor-audit.md:646 ("il Claude Doc").
- **This part's own diff is clean:** it removed the one occurrence in README.md and added none.
- **Why it matters:** the fix wave would leave the standing "no personal data in docs" rule half applied, while the
  ruling reads as if it were done.
- **Fix:** widen P24 to every current file listed above, and record an explicit decision for the historical
  docs/superpowers/** records and the vendor-named file. I declined to judge whether history should be rewritten
  (see below).

### Minor (Nice to Have)

New findings (the ledger's deferred minors are triaged in the next section):

- **m-1. The durability numbers can overlap the condition word in the header.** StationScreen.java:305-310,
  StationLayout.java:46-48.
  - `tiers.maxDurability` has no upper bound (ConfigValidation.java:131-136, `positive` with no max).
  - With a maximum of 10 000 or more, the right-aligned numbers ("10000 / 10000", about x 133..207) overlap
    "Danneggiato" (about x 81..141). The spans are estimated from the font's glyph widths, not measured. This needs a
    careless config, and the effect is cosmetic.
  - The same goes for the cell header with a very large `energy.cellCapacity` (up to 1 000 000). Selecting the cell
    is a 4a-2 action, so that header cannot appear in 4a-1.
  - Fold it into the 4a-2 header work, together with deferred minor T5-7.
- **m-2. Plan-level: the `finally` in `SuitPreview.drawInGui` does not keep the screen usable after an exception**
  (SuitPreview.java:64-82).
  - It restores the pose stack, scissor and lighting, but any exception still leaves `render`, and vanilla turns
    that into a crash. Per javap on both versions, `GameRenderer.render` calls `Screen.renderWithTooltip` inside a
    catch that builds a "Rendering screen" crash report and throws `ReportedException`.
  - What keeps the screen usable is only SuitDraw.draw's own catch. The one plausible thrower left is
    `graphics.flush()` after GeckoLib failed in the middle of a vertex. On 1.21.1, `BufferBuilder.endLastVertex`
    throws when elements of the last vertex are unfilled (javap: an `elementsToFill` check before `athrow`). Whether
    1.20.1's `end()` throws in that case was not checked. The in-world renderer has had the same exposure since M3.
  - Review Focus 5's wording slightly overstates what the `finally` buys. No change is needed in 4a-1.

## Deferred minors: triage

| Ledger item | Triage | Reason |
|---|---|---|
| T1 compat.md:202 SuitRenderer "Uso" sentence | leave | Already fixed in Task 5: compat.md:203 names `StationLayout.suitView(menu.view())`. |
| T1 placeholderButtonIgnoresSpectatorsAndTheDead: positive control; stillValid level-mismatch and catch branches | leave | The positive control was added in Task 3 (StationMenuGameTests.java:185 and the "again in survival" check). Level mismatch is tested in menuClosesWithoutAMessageForOtherReasons (Nether). The catch branch is read-checked; testing it would need a fault-injection hook. |
| T1 four Java lines over 120 characters | leave | Fixed in Task 5: no added Java line is over 120 characters at HEAD (counted in characters). |
| T1 `@clic ... maiuscole` and `@trascina` never ran for real | leave | Both have run on four variants, in m4a1 phases 8 and 10. |
| T1 tools/client/README.md:214 wording | leave | Fixed: :219 now says the step holds Shift anywhere. |
| T2 m4a1_shots.sh:52-53 header (recipe-unlocking item) | leave | Fixed in Task 6 (:91-95). |
| T2 StationItem.TOOLTIP_KEYS not checked against the lang files | leave | Checked by hand at HEAD: every key the station code names exists in both files (TOOLTIP_KEYS, NO_FRAME, FRAME_GONE, TITLE_KEY, the six gui.station.* and four gui.condition.* keys). Suggest that the 4a-2 plan add them to `everyItemAndBlockHasANameInBothLanguages`, where many more keys arrive. |
| T2 onClick's mainPos from the side half untested | leave | Covered since Task 3: side-half clicks in clickWithNoFrameInReachSaysSo and clickOnlyNearLockedFramesSaysLocked, and the mainPos assertion in finderUsesTheFootprintAndTheHeight. |
| T2 gen_data.py AttributeError on malformed conditions | leave | The generator validates its own literal output; on a bad code edit a traceback is as useful as a ValueError. |
| T2 new deprecated-API uses (javac note) | leave | The NeoForge one-argument `hasCorrectToolForDrops` call was accepted by P13; `BlockState.rotate` in `mirror` is the vanilla idiom, also used by ChargerBlock. |
| T2 "decisione P13" in compat.md and the StationBlock Javadoc | **fix before merge** | compat.md is clean, but StationBlock.java:168 still says "(decisione P13)", a ledger-only ID. Drop the tag and keep the reasons. |
| T3 the spectator half of clickWithOffHandOrAsSpectatorDoesNothing cannot fail | leave | The outcome is what matters, and vanilla's spectator gate comes before ours on both versions. |
| T3 menuClosesWithoutAMessageForOtherReasons does not pin the check order | leave | The order is read-checked (StationMenu.java:369-381). A failing first group closes the menu in the same tick, so no frame message can follow. |
| T3 positive control before any setGameMode | **fix before merge** | Global Constraint: each test sets its own game mode. Add `p.setGameMode(GameType.SURVIVAL)` after StationMenuGameTests.java:180. |
| T3 StationFinder.reachText (:107-111): rounding, and "1 blocks" | leave | The misstatement is at most 0.05 blocks, and only for configs with two or more decimals. "1 blocks" appears only at exactly 1.0. If the coordinator wants it anyway: `BigDecimal.valueOf(r).stripTrailingZeros().toPlainString()` keeps "2", "2.5", "8" and "0.5" and never rounds; add 2.25 to the test's list. |
| T3 tools/client/README.md:161 still lists "il telaio" | leave | Fixed by P18 (Task 4). |
| T3 ALL_LOCKED action-bar flag not checked | leave | It is one literal argument, read-checked at StationBlock.java:254-255; the mock player's message log does not record the overlay flag. |
| T4 two copies of the lock rule (`canToggleLock` vs ExoframeCommand:286) | for 4a-2 | Agrees with the ledger. |
| T4 broadcastChanges' once-per-menu ERROR and tick reset untested | leave | Read-checked. The natural home is 4a-2's per-tick write limit (design §5.6 step 3), which can share a pure throttle helper tested with JUnit. |
| T4 deprecation note in the build | leave | Same sources as the T2 deprecated-API item above. |
| T4 StationMenu.setView public, Javadoc | leave | Fixed in Task 5 (StationMenu.java:216-220). |
| T5 compat.md:256 said the screen does not show the view | leave | Fixed in Task 6. |
| T5 build compiler noise | leave | Same sources as the T2 deprecated-API item above. |
| T5 rotazione width condition has no margin | leave | Measured 111 px against 167. The condition exists to reject a back view (173), and it does. |
| T5 tooltips hidden with a stack on the cursor | leave | Vanilla behaviour, stated in the checklist. |
| T5 quickMove partial-merge case | leave | Vanilla `moveItemStackTo` does the work; the loop exit is tested by the full-hotbar case and read-checked for a partial merge. |
| T5 StationScreen grew to 443 lines | for 4a-2 | Agrees with the ledger. |
| T5 header text wider than its field | for 4a-2 | Agrees with the ledger. In 4a-1 it is reachable only if a second player unmounts the selected piece by hand, and the text stays inside the panel. See also m-1. |
| T5 three iron ingots in hand (accepted) | leave | Accepted; they appear only after the last measured shot. |
| T6 Italian labels of base-game menus | **fix before merge** | Checked against vanilla it_it.json. :133 "Scala dell'interfaccia" becomes "Scala" (`options.guiScale`). :135 "Sei morto!" becomes "Sei morto/a!" (`deathScreen.title`). :136 "Apri in LAN" becomes "Rete locale..." (`menu.shareToLan`), and "Avvia il mondo in LAN" becomes "Apri il mondo in rete locale" (`lanServer.start`). The other labels (Opzioni..., Grafica..., Comandi, Tipo: Piatto, Strutture, Rinasci, Nuove ricette sbloccate!) are right. |
| T6 compat.md:47 "nella 4a-1 la vista non cambia nulla che si veda" | **fix before merge** | False as a general statement since Task 5, and :256 contradicts it. Scope it to the unlock smoke test ("quel cambio non si vede: il lucchetto è della 4a-2"). |
| T6 step 12 (:121) does not say which way to drag | **fix before merge** | The `girata` reference was made by dragging right; a literal follower who drags left sees the other profile and reports a difference. Add "verso destra". |
| T6 step 16 (:133) overstates the interface size | **fix before merge** | At GUI scale 2, a window of 640-700 × 480-520 gives an interface of 320-350 × 240-260, not 320×240. This is the only real-client check of Review Focus 4. Narrow the range (for example to at most 660×500) or have the assistant note the `Display:` value, and state the resulting size honestly. |
| T6 step 18 (:135) does not say where to click | **fix before merge** | In survival, with an empty hand, a click on the armed frame enters it and derails the death test. Repeat step 10's "sul fronte, sotto il telaio". |
| T6 optional: step 19 could cover the one visible view change | leave | P4 accepted this gap, and the 4a-2 lock gives a measured check. Cheap to add if the coordinator wants an end-to-end look on the PC. |

## Backward compatibility (worlds and config files from before 4a-1)

- **Old worlds: compatible, with nothing to migrate.**
  - The part adds a block, an item, a menu type and an S2C channel.
  - The station only reads the frame registry. `updatePlaced` is never called outside the tests, and FrameRegistry*,
    SchemaIds, the mixin lists and log_allowlist.txt have no diff in the range, so the registry `schema` is
    unchanged. Old worlds simply have no stations.
  - On Forge 1.20.1, the first load of an older world with the new jar renumbers registry ids and logs a WARN naming
    the mod ("Registry minecraft:item: Object did not get ID it asked for"). This is expected Forge behaviour when
    registries grow (the plan notes it for the dev world, Task 6 Step 3).
  - The PC checklist always creates a new world (steps F and 3), so a literal follower never meets that WARN. No
    checklist line is needed. A one-line note in README "Stato" would only help players who open old Forge worlds;
    it is optional.
- **Config files written by 3c: compatible.**
  - A file without `station` loads silently and is never rewritten. This is proven by JUnit
    (`fileWithoutTheStationSectionTakesItsDefaultsSilently`) and by the dev dedicated server on four variants with
    3c-era files (task-6-report step 3).
  - An older jar ignores the new section (DEC-32).

## Notes on the scheduled rulings

- **P22:** confirmed needed. Guidance is in I-1 (Javadoc, optional reset in `mouseClicked`, and a verification
  step).
- **P24:** scope gap described in I-2.

## Declined to judge

1. Whether the historical records under docs/superpowers/** (specs, plans, ricognizioni, including the game-design
   export whose file name and line 3 carry a vendor name and the owner's full name) should be rewritten. That is a
   policy decision on history for the owner and coordinator; I only flag it in I-2.
2. A client and server with different mod versions: decoding a StationView of another format fails when the station
   opens. Design §5.5 states that client and server always have the same version.
3. The test button 102 shipping on main (ruling P9).
4. The missing visible end-to-end proof that the client applies `station_view` (ruling P4; 4a-2's lock).
5. Empty places without the faint icon of their piece type (ruling P21; 4a-2).
6. A lone main half placed with /setblock opens normally, with a footprint that includes the missing side, and
   drops a station when broken with a pickaxe. It is harmless, needs an admin action, and the spec is silent; P17
   covered only lone side halves.
7. A click on a lone side half: the client answers SUCCESS (the arm swings) while the server answers CONSUME and
   does nothing. This is harmless and has the same shape as any click the server refuses.
8. StationFinder.find scans every placed frame of the level on each click, with no spatial index; the number of
   frames is small.
9. GeckoLib failing in the middle of a vertex, then `flush()` throwing. This is the same exposure the in-world
   renderer has had since M3, not specific to this part (see m-2).
10. The Forge 1.20.1 station loss when the pickaxe breaks on the last hit, and the NeoForge HarvestCheck gap
    (ruling P13).
11. The side half placed without a permission check on its own position (spawn protection, claims). This is
    vanilla bed parity, and on Forge both halves fall inside the placement snapshot window. The spec is silent.
12. Sounds, which the cloud cannot hear; the checklist covers them (step 20).
13. The test-button counter is synchronized as a vanilla data slot, which travels as a short, so the client would
    show a wrapped value past 32 767 presses. The button is a placeholder, and that count is not reachable in
    practice.
14. With selection 7 ("Tutti"), the white second borders of horizontally adjacent places share one pixel column
    (for example the right arm and the torso at x 29). Selection 7 is unreachable in 4a-1 (4a-2 buttons), and the
    effect is cosmetic.
15. `StationView.of` throwing on a corrupt registry entry during the click, which is not guarded the way tick code
    is. DEC-39 guards only per-tick functions, and the registry's own validation rejects such entries.

## Recommendations

- **Fix wave contents:** P22 (with I-1's verification step), P24 widened as in I-2, and the seven deferred minors
  marked "fix before merge".
- **Heavy validation after the wave** (coordinator, one job at a time):
  - the four-variant build with collectJar;
  - the JUnit count (358) and `run_gametests.sh` (235 on each variant; StationMenuGameTests changes);
  - check_sources, check_jars, check_data and check_art, and client_shot --selftest;
  - `m4a1_shots.sh` on the four variants, because StationScreen changes and a client-only change is invisible to
    the in-world tests.
  - The dedicated server does not need a rerun for a client-only and test-only change.
- **For the 4a-2 plan:**
  - Add the station's text keys to `everyItemAndBlockHasANameInBothLanguages`.
  - Use one condition-to-visual-state mapping for SuitView.of and StationLayout.suitView (today a switch and an
    ordinal, pinned only by previewViewComesFromTheStationView).
  - Extract the once-per-tick throttle into a pure helper shared with the per-tick action limit.
  - Lay out the header for long names and large numbers (T5-7, m-1).
  - Let the lock button give the visible station_view check (P4).
  - Any new drag widget should use P20's distance-from-press approach.
- **Process:** a ledger ID ("decisione P13") reached production Javadoc once. A grep for `decisione P[0-9]` or
  `Ruling P[0-9]` in src/, docs/ and tools/ inside check_sources.sh would stop that cheaply.

## Assessment

**Ready to merge?** With fixes.

**Reasoning:** The server paths are correct and well defended: open, `stillValid`, the view sync, the block's
break and loot rules, quick move and config compatibility. They are well tested on the real game paths, and I
found no Critical issue and no new code bug. What remains is the scheduled P22 release fix (a real stack-throw
path), widening P24 to the files it misses, and a handful of cheap checklist and doc corrections that a literal
follower on the PC needs.
