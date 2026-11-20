# Challenge of "partita.md" (lens: play-through) — P1-P10

Read-only. Paths relative to `src/main/java/com/toxineren/exoframes/` unless absolute. Bytecode dumps used:
`esame/ch/{lp20,op20,e20,le20}.txt` (javap of the 1.20.1 merged jar).

## P1 — HOLDS (blocker)
- `compat/FrameStateAccess.java:63-68` "Crea lo stato se assente"; impl `compat/impl/FrameStateAccessImpl.java:66-78` →
  `FrameStateSavedData.updateWorn` (`:335-347`) calls `getOrCreateWorn` and `markStructural`. `SlotGuard.tick`
  (`frame/SlotGuard.java:76-120`) dresses any player with a worn record and moves real armour in the slots to the
  inventory (`displaced` → `Handouts.give`). A late `cell_swap` (after V exit, death, `debug wear remove`) would mint a
  bare frame from nothing (on the next V it becomes a placed frame) and take an inventory cell into it.
- Handlers run on the game thread (`compat/impl/NetImpl.java:82` `ctx.queue`), so a presence check in the handler is
  sufficient; no lock needed.
- Fix is right. Smallest form: a new `boolean updateIfPresent(ServerPlayer, Consumer<FrameState>)` on the seam
  (structural mark + `syncTo`, false without creating) — reusing `mutate` + manual sync would lose the structural
  save flag that the 3b save-at-quit rule depends on. Keep the examiner's order and GameTest. Also say: 7.2 must name
  `updateIfPresent`, never `update`; `update` stays only for `DebugWear` (its one production caller, `frame/DebugWear.java:40`).

## P2 — HOLDS (major)
- `frame/FrameInteractions.java:180-185` (cell present → `SLOT_TAKEN`); `:212-232` + `core/frame/ClickZones.java:47-59`:
  sneak + empty hand takes the cell only after the clicked band is empty; head band = helmet only, so the shortest path
  on a full suit is: sneak-click head (helmet out), sneak-click head (cell out), insert cell, remount helmet. Entry
  refuses an empty cell (`frame/FrameEntry.java:115-118`).
- Fix is right and mirrors 8.3. Add two specifics: (a) creative (`instabuild`) does not shrink the held stack today
  (`:192-194`), so a swap in creative would hand out the old cell while keeping the held one — say "creative: the held
  cell is consumed too" or accept it as creative behaviour explicitly; (b) the old cell goes into the selected hotbar
  slot only after `updatePlaced` returned true (same pattern as `insertCell`).

## P3 — PARTLY (minor as a defect; a [Q] for the owner)
- Facts re-derived: quartz is Nether-only in vanilla 1.20.1/1.21.1 (ore, bastion loot, barter; master mason sells
  quartz blocks/pillars, no uncraft recipe). Entry needs charge (`FrameEntry.java:115-118`).
- But the ingredient is the owner's own text in three places: game document line 48 "un cristallo di quarzo",
  first design line 128, binding §5.8 line 217. The owner's early-game path is the ruined village (game doc line 135),
  which hands out "una cella scarica": that cell needs only the charger (no quartz). So the Nether gate is a 3c-only
  gap caused by M6 coming later, not a design error.
- Correct change: a [Q] whose default is the binding text (quartz), with the tag `cell_crystals` = {quartz,
  amethyst_shard} offered as the alternative. The builder must not change the ingredient by default.

## P4 — HOLDS (major)
- `verifica/test.md:98` and `:155` say exactly this; `:163` adds that the jitter was not measured. 3.3 classifies per
  tick, so every server tick with no move packet is IDLE (2 RE/s) and a tick with two packets is still one WALK tick:
  the average drain of a walking player falls below 8 RE/s by the fraction of empty ticks, and depends on network
  timing.
- Fix is right (sum over a 10-tick window, ring buffer, pure function). Add to the JUnit: a 0/2/0/2 packet pattern
  and a 1/1/1 pattern give the same WALK count; a stop yields at most 10 trailing WALK ticks.

## P5 — HOLDS (major)
- Design line 138 (3b recon, observed): "da passeggero la posa non si adatta: nei tunnel alti 2 la testa sta nel
  blocco". Bytecode: `Entity.isInWall()` checks only `noPhysics` and the eye box (`e20.txt`), and
  `LivingEntity.baseTick` calls `isInWall` → `damageSources().inWall()` at @102, with no passenger exemption
  (`le20.txt`). `in_wall` bypasses armour. So suffocation as a passenger in a 2-high tunnel follows from observed head
  position + vanilla code (not run).
- Design §8 q.2c (line 265) is still open; nothing in the decisions closes it.
- Fix is right; both event signatures are confirmed in `verifica/attributi.md:157-158`. Two refinements: refuse on
  both sides (the events fire on the client too, avoids a seated-then-dismounted flicker); for beds, say that the
  refusal also prevents setting the spawn point while in a suit (player cost, write it). Put it as a [Q] to the owner
  with this as the default, since §8 q.2c is addressed to him.

## P6 — HOLDS (major on the text, the rest minor)
- 5.1 says "does not jump off the bottom higher than on land"; the recon says it does not jump at all
  (`ricognizione-movimento-e-dati.md:33,35,51`, table line 99: Y −61.0 on all four in 1-deep water). The jump to ground
  is inside the `isAffectedByFluids` block of `aiStep` (`:49`).
- On land a suit with step 1.0 can climb a 2-block bank by jumping (apex 1.25 + step while descending); in water it
  cannot: the asymmetry is real. Air 300 ticks = 15 s; head slot holds a placeholder (`frame/Placeholders.java`).
- Fix (a)-(d) fine. Show the hint once per entry at most, not on every contact.

## P7 — PARTLY (minor)
- Arithmetic re-done, all correct: suit 29 I / 7 C / 9 r; kit 34 I / 12 C / 20 r / 1 Q; sprint-jump 20 + 15/0.6 =
  45 RE/s → 222 s = 3.7 min; zombie 3 GENERIC → torso 0.9/hit → 200 hits, arm 0.375 → 480, helmet/leg 0.45 → 400;
  vanilla `max(1, 3/4)` = 1 per piece per hit → chest 240.
- What does not hold: (1) `jumpCost 15` is the owner's value (game doc line 57 "Salto 15 RE a salto", first design line
  128): changing it to 6 is an owner decision, not a builder change; the real point to put to him is that his "circa
  20 minuti di uso normale" (game doc line 46) becomes 10.8 min with a jump every 2 s. (2) "Not cheap": the six pieces
  cost 23 iron + 6 copper ≈ iron armour (24); the extra 6 is the frame, a reusable chassis that later tiers keep. (3)
  "Not fragile" is stated but the proposal keeps durability 180, so it fixes nothing on that axis: total Scrap
  durability 6 × 180 = 1080 vs iron 165+240+225+195 = 825, and the router spends exactly the raw amount across pieces.
- Correct change: a [Q] to the owner with the energy table (jumpCost 15 vs his 20 minutes) and the durability totals;
  builder-side only "skip the jump charge while `hurtTime > 0`" (the knockback-as-jump case is a deduction in
  `verifica/test.md:77`, cheap to guard) and, optionally, arm/leg recipes yielding 2.

## P8 — HOLDS (major)
- `src/main/resources/assets/redstone_exoframes/lang/en_us.json`: no tooltip keys; `no_cell` = "The exoframe needs a
  charged cell". `FrameInteractions.java:115-130`: any non-piece, non-cell item → `Action.NONE` → PASS. While worn, the
  installed cell is in the registry, not the inventory, so the 7.1 cell bar does not show it: without 10.2 the energy
  is invisible exactly while it is being consumed.
- Fix right; one trim: the keybind chat line on the first entry per session, not every entry (an entry/exit loop at a
  base would spam chat).

## P9 — PARTLY (minor)
- The behaviour is not "missing": design line 357 lists it under the heading at line 349 "Decisioni di the owner … ciò che fa la mod oggi"
  (q.10, already in the 3b checklist): it is an open owner question with today's behaviour as the default.
- "Cannot be recovered" is too strong: placed frames are indestructible (design line 354; lava/fire immunity of the entity not checked here) and lava is left vanilla for
  the suit (`ricognizione:71`, measured: the suit rises in lava with Space), so a player with Fire Resistance can dive,
  enter and climb out. Hard, not impossible.
- Defect in the proposed default: `SafeSpots.tick` (`frame/SafeSpots.java:63-69`) records any `onGround` tick, and a
  suit walking on a lava bottom is on ground; "last safe position" can itself be in lava. If the owner picks the
  default, it must also say "a tick in lava (`isInLava()`) is never remembered as safe". Keep it a [Q].

## P10 — PARTLY (minor)
- True: first-person arm is vanilla until M4; HUD is energy-only; `repairKitFraction` is unused outside `config/`.
  Pieces do show `PieceCondition` (INTACT/DAMAGED/CRITICAL/BROKEN, `core/armor/PieceCondition.java`) on the model in
  third person (F5), so wear is not invisible, only in first person.
- "Nothing repairs in 3c" is scope, not a defect: repair is the M4 station and the kit (game doc line 41).
  200 zombie hits for a torso is not a consumable rate for an alpha slice.
- Correct change: keep the cheap parts (action-bar line at the CRITICAL transition, lowest piece in the HUD line);
  the repair question is a one-line note in the checklist, not a [Q] blocking 3c.

## Missed in the same lens
1. (minor) Auto-jump: vanilla `LocalPlayer.updateAutoJump` never reads `maxUpStep` (`lp20.txt`, method at line 2429:
   only 1.2 + jump boost), so with the option on every 1-block step becomes a jump (15 RE, counted by the statistic)
   although the 1.0 step would climb it. Default is off (`op20.txt` `options.autoJump` iconst_0), the recon disabled
   it for that reason. 4.2/3.3 should say: accepted, write it in the checklist (or: no jump charge when the jump ended
   on a block exactly 1 higher — not worth it).
2. (minor-major) Charger 8.3: dust can never be taken back (empty hand withdraws only the cell, hoppers extract only
   a full cell, 8.4) and "move as many as fit" puts the whole held stack in. Decide: sneak + empty hand withdraws the
   dust (pairs with P12's status line), or say "dust is fuel once inserted; breaking returns it".
3. (minor) Action-bar contention: the swap hold indicator (every tick for 2 s), the exit hold (`exit_hold`), LOW/EMPTY,
   piece break (1.5) and charger status all write the action bar; a piece-break line during a hold is overwritten
   on the next tick. Decide a priority (event messages win for 2 s over hold indicators) or put events in chat.
4. (minor) 3.3 counts horizontal distance only: climbing ladders, vines, scaffolding, bubble columns and pillaring
   cost IDLE (2 RE/s). Say it is accepted, or use 3D distance with the vertical part capped.
5. (minor) 7.2 still ends with an open question ("The swap is refused in a menu?"); and the swap rule says nothing
   about a dead pilot / spectator (P1) or a frame with no cell (cannot happen today: entry needs charge; say so).
