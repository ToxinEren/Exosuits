# Final review, area B: damage, registry and the carry-overs of part 3b

Branch m3c-giocarci, BASE 2e35aa67a2a481b9e58ee83a84d1024e81f3c648, HEAD d67e8b66b0ea797e05ecf1358e35606b24dd97eb (checked out, matches).
Tasks behind the area: 3, 4, 5, 6, 7, 11, 17, 18.

## How this review was done

- Read `final-diff-B.patch` line by line (all 22 production files, every hunk), and `final-diff-B-tests.patch` in full for
  CarryOverGameTests, DamageGameTests and the JUnit of compat/impl and core/armor; ExoframeGameTests only as far as the
  moved helpers and the rewritten tail of `mountPieceAndCellByHand`.
- Compared the split registry with the base `FrameStateSavedData` (git show 2e35aa6) method by method: `load` ->
  `FrameRegistryFormat.read`, `write`/`putRaw`/`writeRecord` -> `FrameRegistryFormat.write`, every registry operation ->
  `FrameRegistry`. The split is a pure move; the only behaviour changes are the ones the design asks for (the file
  without sections, `changeWorn`, the narrowed quit save).
- Followed every interaction out of the area into the current code: `SuitTick` (`afterSync`, `markDirty`, `forget`),
  `CellSwap.handle` and `DebugEnergy.set` (callers of `changeWorn`), `SuitMovement.sprintAllowed` and `EnergyHud`
  (readers of `clientIndicators`), `ExoframeCommand` (`FrameSight`, `FrameRemoval`, lock/unlock), `GameTestsImpl`
  (`readRegistry`, `syncCount`), `ArmorMaterialsImpl` (placeholders take no durability damage from vanilla
  `hurtArmor`: material durability 0 on 1.20.1, no durability property on 1.21.1), `Compat` (one `FrameStateAccessImpl`
  instance, so the `onSynced` listener is on the instance that syncs).
- Checked loader facts in the jars of the Gradle cache (sources and `javap`, nothing run):
  - Architectury `LIVING_HURT` is `LivingAttackEvent` on Forge 9.2.14, `LivingIncomingDamageEvent` on NeoForge 13.0.11,
    the head of `LivingEntity.hurt` on Fabric; `PLAYER_QUIT` is at the head of `PlayerList.remove` on Fabric and
    `PlayerLoggedOutEvent` on Forge/NeoForge; `CLOSE_MENU` fires after `AbstractContainerMenu.removed` and before the
    menu is reset (Fabric mixin source; `PlayerContainerEvent.Close` on Forge/NeoForge). The Javadoc of `SuitDamage`,
    `SlotGuard` and `FrameStateSavedData` states these facts correctly.
  - NeoForge 21.1.266 `LivingEntity.hurt` pushes a `DamageContainer` and returns when the incoming-damage event is
    cancelled without popping it (offsets 53-88): the leak design §12.8 accepts.
  - `damages_helmet` scaling: 1.20.1 applies `amount *= 0.75` after `actuallyHurt` (offset 309, so `hurtArmor` gets the
    full amount), 1.21.1 before it (offset 210). That matches `anvilHitsTheHelmet` (4.0 on 1.20.1, 3.0 on 1.21.1) and
    design §12.7.
- Did not run Gradle, the game, the scripts or the client. Test counts read from the source: 172 `add(tests,` lines
  (CarryOver 21, Damage 13, Exoframe 95, Energy 27, Cell 12, Movement 4), which matches the plan table for Tasks 1-24.

## Strengths

- Registry split (Task 4) is a faithful move: same tolerance rules, same `~unread` key moving, same orphan handling. The
  three callbacks `FrameRegistry(readOnly, dirty, structural)` keep the file concerns in `FrameStateSavedData`
  (FrameStateSavedData.java:95). The JUnit of 3b passes unchanged apart from `empty()` -> `createEmpty()`.
- The file without sections (Task 5) is handled on every read path: the game's (`get`, FrameStateSavedData.java:166-167)
  and the mod's own recovery (`afterUnreadFile` -> `reportedWithoutSections`, :174). The ERROR is written once, nothing
  is written over the file (`isDirty()` is false in read-only), the previous copy is not searched, and the operator
  text in docs/compat.md:285 says what to do.
- The narrowed quit save (FrameStateSavedData.java:439-446) writes all players and then only the registry file, with
  the same atomic `RegistryFile` path the game save uses. `quitSaveIgnoresSaveOff` proves both halves: the frame is on
  disk under `/save-off` and `Raids` stays dirty.
- `changeWorn` cannot create a record. FrameStateAccessImpl.java:100-117 checks read-only first (ERROR once), then the
  missing record (no sync), and only then writes. FrameRegistry.java:156-169 repeats both checks. The ruling's
  "one-line read-only branch" is present in both places.
- Packet invariants are enforced in the type. The `FrameStateSyncPacket` compact constructor (FrameStateSyncPacket.java:60-67)
  refuses a state without indicators and the reverse, so `FrameStateClientCache.accept`'s `orElseThrow`
  (FrameStateClientCache.java:33) cannot fire. The codec writes the indicators only with a state, and a null tag reads
  as absent.
- `SuitDamage`:
  - Guard order matches §12.7 and §12.8. `!(amount > 0F)` also excludes NaN.
  - The rounding is clamped to [0, Integer.MAX_VALUE] milli, and `routeMilli` accepts up to Integer.MAX_VALUE x 1000,
    so no input can overflow.
  - The wear is one non-structural `mutate`; the messages, the sound and `markDirty` happen only after `mutate` returned
    true.
  - The fall listener cancels only with a worn record. In read-only mode the registry holds no record, so `wornFrameId`
    is empty and the game's own fall applies.
- `DamageRouter.routeMilli` computes the quotas directly in thousandths. `route` delegates with `(long) amount * 1000`.
  `largeAmountsDoNotOverflow` pins the conservation at the maximum.
- Step-out search (Task 7), checked by hand:
  - `Shapes.collide` from half a block up ignores shapes the box already overlaps, so a lip of up to 0.5 is climbed.
  - A wall, a lid over a pit and a ceiling in the standing box are all refused by `noCollision(standing)`.
  - The L-shaped corridor's bottom is raised to `to.y` only, so a step down still checks the whole height at the
    frame's level.
  - The refused exit (FrameEntry.java:197-201) is computed before anything changes. `exitRefusedOverAnotherFrame`
    checks that the record, the placeholders, the stowed helmet and the position all stay as they were.
- `WornState.changed` is a pure reordering. The tick record and the attributes are dropped exactly when the worn state
  ends (exit, death, `debug wear via`), and the tick record heals itself if an exception skips the call
  (SuitTick.java:140-145).

## Review Focus, followed end to end (points touching area B)

1. **Late or fake cell swap.**
   - `CellSwap.handle` (CellSwap.java:70-117) returns before any write when the player has no worn frame (exit and
     death move the record out in the same tick), is dead, is a spectator or has a menu open.
   - The write is `changeWorn`, which cannot create a record and refuses in read-only mode. The inventory is touched
     only after `changed == true`. The old cell goes back to the same slot, or to `Handouts.give`.
   - The placed-frame swap (FrameInteractions.java:193-214) follows the same order through `updatePlaced`.
   - `FrameState.insertCell` can only throw on null, so the old cell can never be removed without the new one going in.
2. **Wear per hit.**
   - The hook is at the head of `Player.hurtArmor` (PlayerSuitMixin.java:28-31). It is called once per applied hit,
     after the invulnerability window, and only for damage that does not bypass armour.
   - A zero amount (after a full shield block) is excluded by the guard; creative and spectator are excluded before
     the registry is read.
   - Lava wears at every applied hit (two per second), not at every tick. The tests cover all four cases on the four
     variants.
5. **Sync every tick or never.**
   - A wear that removes a point calls `SuitTick.markDirty` (SuitDamage.java:177-179). The next tick at least 5 ticks
     after the last sync sends the state (SuitTick.java:212-216) even when the cell is empty.
   - A condition change (critical or broken) is sent on the next tick, through `sentConditions`.
   - Every structural sync (`changeWorn`, enter, exit, update) clears the mark through `afterSync`.
   - `wearAtEmptyStillReachesTheOwner` proves that wear alone does not sync, and that the sync comes from the mark,
     not from a condition change (179 and 178 of 180 stay INTACT).
3, 6. Area A owns these. My only cross-check: `WornState.changed` clears the attributes at exit and death, and the
   record is per player object, so a new object starts clean.

## Four variants

- The Stonecutter branches in the area are the 3b ones and are unchanged: `save` signatures and `computeIfAbsent` in
  `FrameStateSavedData`, and the respawn lambda in `FrameStateAccessImpl`.
- The only new conditional code the area relies on is `GameTestsImpl.readRegistry`, which waits for the NeoForge I/O
  worker. It is correct for its variant.
- Proved on all four variants by in-world tests: the fall cancellation through three different loader events, the
  `hurtArmor` hook, the chest sweep at `CLOSE_MENU`, the quit save and the read-only file without sections.
- Proved by the real client on four variants (Task 24 screenshots): the indicators arriving through
  `frame_state_sync`, as the energy-line colour.
- The shield-zero case is tested only by calling `onArmorHurt` directly. Acceptable, because the guard is one
  comparison.

## Issues

### Critical (Must Fix)

None.

### Important (Should Fix)

None.

### Minor (Nice to Have)

**FB-1. Javadoc and documents that later tasks of the branch made stale (who calls `mutate`, when the registry is
read-only, what the damage tests share).**
- FrameRegistry.java:171-173: `mutateWorn` says it is the high-frequency write "l'energia a ogni tick, in una parte
  successiva". In this part it has two such callers: `SuitTick` (SuitTick.java:179) and `SuitDamage.wear`
  (SuitDamage.java:157, at every applied hit and every cancelled fall).
- docs/compat.md:68:
  - "il chiamante di oggi e' `frame/SuitTick`": `SuitDamage` is a caller too.
  - "Se il dato salvato viene da uno schema piu' recente, le quattro scritture non fanno nulla": the same holds for a
    file without sections (Task 5) and for an unreadable file.
- FrameStateAccessImpl.java:40-42: "in sola lettura (schema piu' recente)" has the same gap.
- DamageGameTests.java:31-37 (deferred T18-F1, still true): the class Javadoc says every test uses a survival player in
  six new Scrap pieces hit by a cactus. The fall tests use `fall()`, the bare-frame test has no pieces, and the lava,
  anvil and creative tests use other sources.

Why it matters: the next part (3c-2) adds the charger and its own write paths. A reader of these lines would believe
only the energy tick mutates a worn frame, and that read-only means only a newer schema. No player effect.

Fix: reword the four places, one clause each.

## Recommendations

- **`REFUSED` outcome (ruled):** `/exoframe debug energy`'s `REFUSED` (DebugEnergy.java:48-50) cannot be reached in
  practice, for the reason T3-F1 is obsolete.
  - A read-only registry never holds a worn record: READ_ONLY and WITHOUT_SECTIONS read nothing, and an unreadable file
    leaves an empty data. So the command answers `NOT_WEARING` in chat, which is true for that session.
  - This breaks nothing. But the PC checklist of 3c-2 should not contain a step that expects silence in chat with a
    read-only registry.
- **3b PC checklist:** docs/test-in-gioco/m3b-entrare-e-uscire.md:13 and :84 still present the step-out limits that
  Task 7 removed (farmland, path, soul sand, carpet), list only lava and fire as hazards, and do not mention the refused
  exit over another frame.
  - The wording "puo' non avvenire" keeps those steps from failing, so this can wait.
  - The 3c-2 checklist should state the new rules. Owned by area C (documents).
- **T17-F1:** if the action bar must name the broken piece, send the CRITICAL messages first and the BROKEN ones last
  in `SuitDamage.wear`. Optional: the HUD's second line already shows the broken piece in red.

## Declined to judge

- **NeoForge `DamageContainer` leak.** Every cancelled fall leaves a `DamageContainer` on the player object's stack
  (verified in NeoForge 21.1.266 `LivingEntity.hurt`): a few hundred bytes per landing until respawn or relog. Set
  aside: design §12.8 accepts it as loader behaviour.
- **Fall wear ignores the invulnerability window.** `LIVING_HURT` fires before the window on all three loaders, so a
  modded damage type tagged `is_fall` and applied every tick would wear the legs at every call. Set aside: design §12.8
  ("Nessun conto della finestra di invulnerabilita' per le cadute").
- **Wear and energy are written with the non-structural `mutate`.** The quit save does not flush them, and a crash
  between autosaves rolls them back. Set aside: design §12.4 and §12.7.
- **The quit save writes the players first and then the registry.** A crash between the two can duplicate what moved
  from the registry to an inventory since the last save. Set aside: order kept from 3b, documented as the accepted
  "arresto brusco nei millisecondi di un salvataggio".
- **Frames can still stack.** Death and forced moves can still put two frames at the same spot. A placed frame without
  its entity (chunk without entities, or the seconds before `FrameRealign` recreates a killed entity) does not block an
  exit. Set aside: design §12.14.
- **`/exoframe lock` and `unlock` on a worn frame use `mutate`.** The change is not structural, so the quit save does
  not flush it. Set aside: 3b code, untouched by the branch.
- **Sweep at disconnect.** At disconnect `CLOSE_MENU` runs after the game has saved the leaving player's file. A stray
  placeholder swept from that player's own inventory comes back at rejoin until the guard's full scan, within a second.
  Placeholders are inert. Set aside: harmless and outside the plan.

## Deferred minors of the area (triage)

- **T3-F1 — obsolete.** A read-only registry never holds a worn record, so `DebugWear.remove` takes the no-record
  branch with no log line. `DebugWear.wear` throws at `get(...).orElseThrow` before `WornState.changed`. The misleading
  INFO line cannot be written.
- **T3-F2 — obsolete.** This is brief text only. Design §12.14 and the ruling carry the five-argument signature, and
  Task 24 did not re-correct §12.14.
- **T3-F3 — leave.** SlotGuard.java:140 is 121 characters. No tool enforces 120, and five other lines of the area are
  over it.
- **T4-F1 — leave.** Only the placement of one class-comment sentence; no effect.
- **T4-F2 — leave.** FrameStateAccessImpl.java:40 still points to `FrameStateSavedData` for the unread-entries rule.
  That class's Javadoc points on to `FrameRegistryFormat`: one extra hop, no effect.
- **T5-F1 — leave.** It needs both an unreadable main file and a previous copy without sections. The registry still
  ends read-only and nothing is written or lost; only the two log lines contradict each other.
- **T5-F2 — leave.** One `Path` allocation per `get()`: garbage only, no measurable cost at this scale.
- **T5-F3 — obsolete.** docs/compat.md:87 now names `readRegistry` in the GameTests "Condizione" line.
- **T5-F4 — leave.** The wording of the refused-change ERROR for a file without sections. It is reachable, for
  example by placing a frame. The first ERROR names the real cause, and "restore and restart" is right for both cases.
- **T6-F1 — leave.** A missing blank line (CarryOverGameTests.java:261-262); cosmetic.
- **T6-F2 — leave.** The tie-break needs two frame boxes around the eyes. Placement and the exit now refuse overlaps,
  so only deaths can stack frames. The rule is a two-line `min` by distance.
- **T7-F1 — leave.** Only the cart search's downward extension is untested; the cart in the standing box is tested. A
  regression only lets a step land above a cart.
- **T11-F1 — obsolete.** docs/compat.md:68 now lists `update`, `mutate`, `changeWorn` and `clear` together, then
  `clientIndicators` and `onSynced`.
- **T11-F2 — leave.** The cache logic is three lines, and the real-client runs saw the indicators drive the line colour.
  A regression would only touch a suited client until the next sync.
- **T17-F1 — leave.** The break sound plays, and the HUD's second line (Task 22) shows the broken piece in red. Only the
  transient action-bar text is lost; see Recommendations.
- **T17-F2 — leave.** The test holds by construction, but it is cheap and documents the delegation contract of `route`.
- **T17-F3 — obsolete.** Ruled at progress.md:109: tests set the game mode themselves, as DamageGameTests.java:142
  does.
- **T18-F1 — leave.** Still true (DamageGameTests.java:31-37), but only a test Javadoc; covered by FB-1.

## Assessment

**Ready to merge?** Yes (area B)

**Reasoning:** In area B the code matches design §12.4, §12.7, §12.8 and §12.14 and the rulings. No path in the area can
create a frame from nothing, or lose or duplicate an item. The one finding (FB-1) is documentation and can ride along
in the fix wave.
