# Final review, area A (energy, movement and client) - branch m3c-giocarci

Range 2e35aa67a2a481b9e58ee83a84d1024e81f3c648..d67e8b66b0ea797e05ecf1358e35606b24dd97eb. Read-only review, done in
passes: (1) the whole production diff of area A (`final-diff-A.patch`, 3 024 lines) line by line; (2) the whole tests
diff of area A (`final-diff-A-tests.patch`); (3) the current code that my area calls or is called by, in areas B and C:
`RedstoneExoframes`, `FrameStateAccess`/`FrameStateAccessImpl`, `FrameRegistry`, `FrameRegistryFormat` (read-only
path), `FrameStateSavedData`, `FrameStateSyncPacket`, `FrameStateClientCache`, `FrameStateCodec` (Task 20's cell
read), `PieceTags.readEnergy`, `GameTestsImpl` (mock/survival player, respawn, attribute helpers), `WornState`,
`SuitDamage`, `FrameEntry`, `FrameInteractions` (Task 21's placed-frame swap), `FrameRemoval`, `DebugWear`, `ExitHold`,
`core/frame/FrameRules`, `FrameEffects`, `FrameState` (cell methods), `EnergyMeter`, `EnergyCell`, `ModItems`,
`docs/compat.md`, `tools/log_allowlist.txt`; (4) the Task 12/16/24 reports for the real-client evidence. Spec read:
design §12 preamble, §12.2-12.6, §12.10, §12.13, §12.14 (debug energy); plan lines 1-96; every `Ruling:` of the
ledger. I ran no Gradle, game or test script; one read-only Python script checked the two language files.

### Strengths

- `frame/SuitTick.java:135-223` follows design §12.3's nine steps in order (no record -> discard and clear; record
  creation with position, dimension and jump base; dismount read before `stopRiding`; activity after the dismount;
  one `mutate` for tick and jump; evaluate/apply/transitions; server sprint off; sync; water hint), all inside one
  `try/catch` with one ERROR per UUID and session. The record is keyed by object identity (`MapMaker().weakKeys()`),
  so Review Focus 3 holds by construction: a new `ServerPlayer` has no record, `r.applied == null` makes the first tick
  apply the attributes (`SuitTick.java:191-194`), and `FrameAttributesImpl.set` (`FrameAttributesImpl.java:94-106`)
  touches a modifier only when amount or operation differ, so the second tick resends nothing
  (`attributesComeBackOnANewPlayerObject`, `attributesAppliedOnceAndNotResent`).
- Transitions compare with the last *applied* effects, not the last *sent* ones (`SuitTick.java:197-208`), so a
  structural `syncTo` outside the tick (cell swap, debug energy) cannot silence a LOW/EMPTY warning; `afterSync`
  (`SuitTick.java:232-250`) keeps sent values and the sync tick in step with every `syncTo`. Pinned by
  `lowAfterAChangeWornStillSpeaks`.
- Time rules use `now` against saved ticks only, and a backward clock restarts the window and the sync limit
  (`SuitTick.java:153-156`; `CellSwap.java:78` accepts a swap when `now < last`). No `now % n` in new code; no
  `isCreative()` in new code.
- Review Focus 1 holds end to end. `CellSwap.handle` (`CellSwap.java:70-117`) refuses before reading anything when
  the player wears no frame, is dead, spectating or has a menu open; picks with the pure `CellSwapRule`; writes the
  registry with `changeWorn` (never `update`); touches the inventory only after a successful write; cells stack to 1
  (`ModItems.java:21-22`), so the old cell always lands in the vacated slot and `Handouts.give` is only a fallback;
  the interval is marked only on success. A late packet after exit or death, a modified client spamming
  `cell_swap`, or a read-only registry (which holds no worn record, see FA-1) can neither create a frame nor lose or
  duplicate a cell. `cellSwapTakesTheFullestBetterCell` counts cells and RE before and after.
- Review Focus 5 holds: dirty only when `mutate` succeeded and at least 1 RE left the cell; immediate sync on a
  change of effects, energy state or piece condition; otherwise at most every 5 ticks; wear marks dirty through
  `SuitTick.markDirty` (`SuitDamage.java:177-179`). `idleTickWithoutAWholeReDoesNotSync`, `walkingSyncsEveryFiveTicks`
  and `clockGoingBackwardsRestartsTheLimits` exercise the real limiter.
- Server and client agree without a packet on water and swimming (`SuitMovement.suited` = `SuitSize.wearsSuit` and
  not spectator), and the sprint gate reads only server-computed indicators. Task 24's real-client runs on all four
  variants show sinking, standing still while holding Space, no current, the bank exit (full charge, as ruled), the
  one-block step, sprint off at 999 RE and on at 5 000 RE, the cell swap and the energy line, with no "moved wrongly";
  the 1.20.1 step and its return to 0.6 after leaving the suit were measured on both 1.20.1 loaders (Task 16 Step 7).
- Every Stonecutter branch of the area is right for its version: `FrameAttributesImpl` (1.20.1 UUID ids,
  `getAmount`/`getOperation`, step as a client field with the identity set `RAISED`; 1.21.1 record accessors and the
  fifth modifier on `STEP_HEIGHT`), `ItemBase` (the two `appendHoverText` signatures), and `ClientRenderHooksImpl`'s
  untyped lambda over the two `RENDER_HUD` signatures (`ClientRenderHooksImpl.java` `addHudRenderer`).
- The two language files have 75 keys each, same order, same placeholders, and every literal key used in the Java
  sources exists (checked with a script); the constructed `energy.*`, `condition.*` and `piece.*` keys exist too.
- Tests carry their own controls: creative beside survival, a control player in the same pool, a sound recorder per
  structure box, starting conditions asserted (`assertUnderWater`), sync counts per tick.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

1. **FA-1 - `/exoframe debug energy`'s `REFUSED` outcome is unreachable; four texts describe a read-only behaviour
   that cannot happen** (T23; touches T21's `CellSwap` Javadoc).
   - Files: `frame/DebugEnergy.java:27-28,41,49-50`; `command/ExoframeCommand.java:96-97,238-240,259-261`;
     `frame/CellSwap.java:62-63`; design `docs/superpowers/specs/2026-11-13-m3-telaio-design.md:658` (§12.14).
   - What is wrong: a read-only registry never holds a worn record. `FrameRegistryFormat.read` returns `READ_ONLY`
     before reading any entry (`FrameRegistryFormat.java:104-108`), and the without-sections and unreadable paths
     read nothing either, so `FrameStateAccessImpl.get`/`wornFrameId` (`FrameStateAccessImpl.java:62-71,247-251`) are
     empty for everyone. `DebugEnergy.set` therefore returns `NOT_WEARING` at line 41-43 and never reaches
     `changeWorn`; with a worn record present the registry cannot be read-only, so `changeWorn` cannot return false
     and `REFUSED` is dead code. In a read-only session the operator reads "You are not wearing an exoframe" in chat
     and no "changes are not applied" ERROR is written, the opposite of what the design sentence and the two
     Javadocs promise ("non scrive nulla in chat ... resta la riga del log del registro"). `SuitDamage.java:56-57`
     already states the true fact ("con il registro in sola lettura `wornFrameId` e' vuoto").
   - The ruling of line 13 assumed `REFUSED` reachable; it did not consider that read-only data reads no worn record.
     Nothing for a player is hurt (the not_wearing answer is accurate in effect), so the ruling's outcome needs no
     code change, only true texts.
   - Fix: say in the three Javadocs and in design §12.14 that a read-only registry has no worn frame this session,
     so `energy <re>` answers `not_wearing` and `CellSwap` does nothing; keep `REFUSED` as a defensive branch or drop
     it. No test can reach it today.

2. **FA-2 - the real-client reception of `client_rules` was never proven** (T12, T24; uses T20, T21).
   - Files: `frame/ClientRules.java:41-44` (sent at `PLAYER_JOIN`), `:52-61` (`accept`); readers
     `content/CellItem.java:63,69,84`, `client/ExitKey.java:61`, `client/CellSwapKey.java:54`; `docs/compat.md:47`.
   - What is wrong: Task 12's report (line 95) leaves "la ricezione vera nel client" to Task 24's real client, and
     Task 24 ran with default config on both sides, where a received value cannot be told from the local one;
     `docs/compat.md:47` says so honestly. JUnit covers the codec and `accept`, not the delivery on any loader. The
     plumbing is the same `Compat.net().registerS2C` that `frame_state_sync` uses, so a defect is unlikely, but if
     the join-time packet were dropped on one loader, players on a server with a non-default `cellCapacity`,
     `cellSwapSeconds` or `exitHoldSeconds` (now a first-class feature with Task 8's config file) would get wrong
     cell bars and tooltips and the wrong hold times (with a longer server `cellSwapSeconds`, a second swap inside
     the server interval is dropped in silence).
   - Fix: one `m3c_shots.sh` run per variant with a server config file that changes `energy.cellCapacity` (for
     example 20 000: the 5 000 RE cell in `barre-inventario` must show a bar of 3 pixels, not 7) and
     `frame.exitHoldSeconds`; or put that exact check on the 3c-2 PC checklist.

3. **FA-3 - documentation leftovers around `mutate`/`changeWorn` and the sync rule** (T14, T17, T21; files of areas
   B and C).
   - `docs/compat.md:68`: "il chiamante di oggi e' `frame/SuitTick`" - since Task 17 `SuitDamage.wear`
     (`SuitDamage.java:157`) also calls `mutate`; and "(sincronizza) ... solo quando un consumo ha tolto almeno 1 RE"
     leaves out the wear path (`SuitTick.markDirty`), which `SuitTick.java:74-80` describes correctly.
   - `compat/impl/FrameRegistry.java:172-173`: `mutateWorn` is "la scrittura ad alta frequenza (l'energia a ogni tick,
     in una parte successiva)" - it is this part now.
   - `compat/FrameStateAccess.java:62-63`: the list of write methods in `get`'s Javadoc omits `changeWorn`.
   - Why it matters: these are the seam documents the next part (3c-2's charger) will read to decide how to write
     energy; nothing breaks at run time. Fix: three wording edits.

### Recommendations

- In the fix wave, apply FA-1, FA-3 and the `fix` verdict on T16-F1 (`FrameAttributesImpl.java:24-26` says `apply`
  touches the modifier only when amount or operation differ and "otherwise" removes and re-adds it, the reverse of the
  code at `:94-106` and of design §12.5): all are text-only and cheap.
- Close FA-2 with one real-client run on a non-default server config, or name the check on the 3c-2 PC checklist.
- Optional regression tests that would protect players: the off-hand pick and the spectator refusal of
  `CellSwap.handle` (T21-F2: a wrong off-hand index would read an armour slot); first evaluation at EMPTY and the
  silent EMPTY -> LOW improvement (T14-F1).
- Keep on the 3c-2 PC checklist, as ruled: hover tooltips, the NO_CELL and BROKEN energy-line wording, leaving water
  at EMPTY, the bed on 1.20.1-forge and the production jars.

### Deferred minors of tasks 9, 10, 12-16, 19-23

| id | verdict | reason |
|---|---|---|
| T9-F1 | leave | Refused place/enter/exit return before `FrameSounds.play` (`FrameEntry.java:112-147,194-201`); only a regression test is missing. |
| T10-F1 | leave | `build.gradle.kts:106` still names only the size and draw mixins; comment only, no effect on builds or players. |
| T12-F1 | obsolete | Task 24 rewrote `docs/compat.md:47`: `frame_exit`, `cell_swap`/indicators and `client_rules` are now named apart. |
| T12-F2 | leave | The once-per-name WARN in `ClientRules.checked` is simple; only log volume is at stake. |
| T13-F1 | leave | `reset()` on a window of several samples is pinned in world by `dimensionChangeRestartsTheWindow`; the JUnit case is nice to have. |
| T14-F1 | leave | Still true; the two branches (`SuitTick.java:197-208`) read right and only a message wording is at stake. |
| T14-F2 | obsolete | `docs/compat.md:68` now says "un tick fermo in cui il consumo resta sotto 1 RE non manda nulla". |
| T15-F2 | leave | Ruled (Task 15): every right-click on a bed in a suit stays refused. |
| T15-F3 | obsolete | Ruled to ride along; `SuitRestrictions.onUseBlock`'s Javadoc no longer says "Pubblico per i test". |
| T15 (plan:927-928) | leave | Ruled: the plan keeps what was asked; the ledger and design §12.3 say what holds. |
| T16-F1 | fix | `FrameAttributesImpl.java:24-26` states the reverse of the idempotence rule the code and design §12.5 follow; one sentence. |
| T16-F2 | leave | True (a `ServerPlayer` already has step 1.0, so the checks cannot fail) but harmless; the real step was measured on the real client. |
| T19-F1 | obsolete | Ruled; the two `doTick()` calls are right and Task 24 amended design §12.15 to say so. |
| T19-F2 | leave | Ruled: history is not rewritten. |
| T19-F3 | obsolete | Task 24 added the real-handler verification to `docs/compat.md:190`. |
| T20-F1 | leave | The trusted read only sees server-normalized capacities; dropping a bad cell would only affect the client's display. |
| T21-F1 | leave | `CellGameTests.java:229-231` overstates (the early `wornFrameId`/`isAlive` guard returns before `changeWorn`); the test still guards Review Focus 1. |
| T21-F2 | leave | The off-hand mapping (`CellSwap.java:94`, `Inventory.SLOT_OFFHAND`) and the spectator guard (`:72`) read right; test gap only. |
| T22-F2 | obsolete | `EnergyHud.java:79` now uses `SuitDamage.pieceName`; the piece key is built in one place. |
| T22-F3 | leave | LOW and EMPTY colours are now in Task 24's screenshots; NO_CELL and BROKEN wording stay for the 3c-2 checklist. |
| T22-R1-B1 | leave | Cosmetic and only with a boss bar on screen; the top-left placement is the ruled choice. |
| T22-R1-B2 | obsolete | Task 24 rewrote design §12.13 for the two-line, top-left energy line. |
| T23-F1 | leave | Style only; all four outcomes are handled. |
| T23-F2 | leave | Style only; same lookup, same behaviour. |
| T23-F3 | leave | Test-helper duplication only. |
| T23-F4 | leave | `showEnergy`'s message and failure branches are untested; operator-only debug command. |

### Declined to judge

- Texts that name the charger (`tooltip.redstone_exoframes.cell.empty`, `message.redstone_exoframes.no_cell`) while
  the charger arrives only in 3c-2: the plan's "Testi" and design §12.13 prescribe them for 3c-1.
- A modified client can still sprint below 10 % or swim upward in the suit: design §12.2/§12.6 put the ban on the
  client by choice; anti-cheat is outside the spec.
- Idle consumption continues while a suited player watches the End credits (`doTick` still runs while the player is
  out of the level): it matches "standing still"; the spec is silent.
- Fluids other than water from other mods, and sleeping blocks that are not a `BedBlock`: design §12.6 scopes the
  water rule to water and §12.3 the refusal to `BedBlock`.
- A `cell_swap` from a modified client with the player's own inventory screen open is accepted
  (`containerMenu == inventoryMenu` on the server): design §12.10 defines "menu aperto" that way, and nothing is lost
  or duplicated.
- A one-tick flicker of the exit progress when the cell-swap progress is cleared in the same client tick
  (`CellSwapKey` is registered after `ExitKey`): cosmetic, and the design accepts action-bar overwrites.
- Two syncs at entry and at respawn (the structural or respawn sync, then the record's first-tick sync): one extra
  small packet, within §12.4's limits.
- Defects wholly inside areas B and C that no code path of area A reaches (damage routing, registry format and
  recovery, config file, tools): left to those reviewers.

### Assessment

**Ready to merge:** With fixes

**Reasoning:** The energy tick, attributes and step, water and sprint, client rules, cell swap, energy line and debug
command match the design and the plan's Review Focus 1, 3, 5 and 6 on the code and on the four-variant evidence, with
no Critical or Important defect. The fixes are text-only (FA-1, FA-3, T16-F1) plus one missing real-client proof
(FA-2) that a single run or a checklist line closes.
