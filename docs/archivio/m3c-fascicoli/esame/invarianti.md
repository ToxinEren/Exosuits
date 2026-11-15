# Examination of the part 3c decisions — lens: invariants (adversary and Murphy)

Examined: `m3c/direzione-decisioni.md` (draft 1). Invariant under attack: nothing is ever duplicated or lost across
crashes, logouts, deaths and commands (design §4.5, §5.3-§5.5). Read-only; vanilla facts re-checked with `javap -c -p`
on the merged 1.20.1 / 1.21.1 jars (dumps in `esame/jp/`). `FSD` = `compat/impl/FrameStateSavedData.java`,
`FSAI` = `compat/impl/FrameStateAccessImpl.java`, paths under `src/main/java/com/toxineren/exoframes/`.

## Findings, most severe first

### I1 — blocker — decision 7.2: the swap "through `update`" creates a frame out of nothing
- Problem: `update` creates the worn record when it is absent (`FSAI.java:66-78` → `FSD.updateWorn:335-347` →
  `getOrCreateWorn:317-329`, `new FrameState(config())`, structural). A `cell_swap` that reaches the server for a player
  who no longer has a record — the exit (`V`, 1 s) and swap (`R`, 2 s) holds completing in the same tick, death during
  the hold (`LIVING_DEATH` → `onDeath` already turned the record into a placed one), a respawned player, or a hacked
  client sending the packet at any time — makes `update` create a new bare worn record and insert the inventory cell
  into it. `SlotGuard.tick` then sees a record and dresses the player (`SlotGuard.java:76-115`): a free frame is
  duplicated into existence, the cell disappears into it, and on exit a real frame entity is placed.
- Worst outcome: unlimited free frames for any player with a hacked client; the swallowed cell is "lost" into a frame
  the player did not build.
- Proposal: 7.2 must say: "The handler first returns silently unless the player is alive, not a spectator, and
  `wornFrameId(player)` is present (as `FrameEntry.exit:170-174`). The swap never uses `update`; it uses a new
  `boolean FrameStateAccess.changeWorn(ServerPlayer, Consumer<FrameState>)`: existing record only, structural, sync,
  false when absent or read-only." Same rule for any future C2S handler that writes a worn record.

### I2 — blocker — decision 7.2 (and 8.3 by analogy): inventory side and registry side are not tied together
- Problem: `update` is `void` and, on a read-only registry, logs once and returns without changing anything
  (`FSAI.java:68-71`). The decision does not order the two halves. If the handler takes the new cell out of the slot and
  puts the old one there, then calls `update`, a read-only registry (damaged file, §4.5) leaves: new cell gone, old cell
  both in the slot and still in the frame — one cell lost, one duplicated. A `RuntimeException` inside the consumer
  (e.g. `EnergyCell` constructor on a malformed stack, `EnergyCell.java:9-16`) has the same effect, because
  `updateWorn` marks and syncs in `finally` but the inventory was already changed.
- Worst outcome: duplication/loss of a cell whenever the registry is read-only or the consumer throws.
- Proposal: "Order: (1) compute the new `EnergyCell` from the stack (never throws: clamp as `CellItem.toCell`);
  (2) `changeWorn` swaps the cell in the record and returns the old one; (3) only if it returned true, write the
  inventory slot (`stack.split(1)` removed, old cell placed). Nothing touches the inventory before the registry
  accepted." 8.3/8.6 (charger insert/withdraw) already have the order "shrink the hand and set the slot in the same
  call"; add "the slot is set first, the hand is shrunk only if setting succeeded".

### I3 — blocker — decision 8.2 + 8.4: dust slot persisted as a count is destroyed by hopper merges unless it is a live stack
- Problem: 8.2 persists "dust count" as a primitive; 8.4 lets side hoppers insert dust into slot 1. Vanilla hopper
  insertion into a non-empty slot does `existing = container.getItem(slot); stack.shrink(n); existing.grow(n)` and never
  calls `setItem` (javap `HopperBlockEntity.tryMoveInItem` 1.20.1 @2, @57-99; 1.21.1 same shape, `shrink` @92,
  `grow` @99). If `getItem(1)` returns a stack built on demand from the count (the natural shape for a "primitives"
  model, and what the dossier recommends for the cell slot), every hopper merge after the first removes dust from the
  hopper and adds nothing to the charger. Fabric transfer API's `Container` fallback (blocco.md §4) reads the same way.
- Worst outcome: every redstone dust a hopper feeds after the first is deleted.
- Proposal: 8.2 must say: "Slot 1 is a live `ItemStack` field (count only on disk: written as an int, rebuilt as
  `new ItemStack(REDSTONE, clamp(count, 0, 64))` at load); `getItem(1)` returns that same object; `setItem(1, s)` stores
  `s` itself. Slot 0 may be a fresh view built from the `EnergyCell` (one item, hopper extraction restores via
  `setItem`, javap 1.21.1 `tryTakeInItemFromSlot` @2-85)." Add a GameTest: two dust from a side hopper → count 2.

### I4 — major — decision 11.6: "honours `/save-off`" widens the duplication window §4.5 had closed
- Problem: vanilla writes the quitting player's file even under `/save-off` (design §4.5: "il gioco scrive ancora i
  file dei giocatori ... all'uscita di un giocatore"); today the mod pairs it with the registry because
  `saveWithPlayersIfNeeded` ignores save-off (`FSD.java:841-849`, comment "Questo salvataggio non guarda /save-off").
  If the quit save honours save-off, every structural change followed by a quit during a backup window leaves the player
  file newer than the registry. Example: exit (armour back in the inventory, player file written at quit), registry
  still holds the stowed armour → crash before `/save-on` → armour in both places. Same for a cell swap (old cell
  duplicated, new cell lost).
- Worst outcome: duplication or loss of stowed armour, pieces, cells after any crash during a `/save-off` window.
- Proposal: 11.6: "Narrowed to the player files and the registry file; it does NOT honour `/save-off`, because vanilla
  writes the quitting player's file under `/save-off` too and the pair must stay together. §11 entry closed with that
  reason."

### I5 — major — decision 8 (missing): the charger's crash guarantee is vanilla's, weaker than the registry's, and nobody decided it
- Problem: the cell in the charger lives in chunk data; the cell in the inventory in the player file. Vanilla saves a
  player file at quit, chunks only at autosave/stop. Withdraw → quit → crash before the next autosave: the chunk on disk
  still has the cell (if it was saved while inside) and the player file has it too → duplicated. Insert → quit → crash:
  the player file lacks it, the chunk never had it → lost. This is exactly the vanilla chest guarantee (not worse), but
  the "done when" says "Nothing is ever duplicated or lost", and §4.5 fixed the identical window for the registry with
  the quit save. 8.6 claims "a cell is either in the charger or in an inventory, never both" — true in memory, false on
  disk.
- Worst outcome: the classic crash dupe of a full cell (or loss of one) with the charger.
- Proposal: add 8.7 [D-candidate, question to the owner]: "The charger has the vanilla container guarantee: a crash
  within one autosave interval after a player moved a cell in or out and left can duplicate or lose that cell, as with a
  chest. Listed in design §7 next to point 26 as the second known exception." Cheap optional mitigation if the owner
  refuses: on a player-driven insert/withdraw/swap set a flag on the player; at that player's `PLAYER_QUIT`, if set,
  `level.getChunkSource().save(false)` for the charger's level (rare event, bounded cost).

### I6 — major — decision 8.2 (missing rule): a block-entity load that throws deletes the charger's contents
- Problem: `EnergyCell(capacity, stored)` throws when `stored > capacity` (`EnergyCell.java:13-15`). After the owner
  lowers `energy.cellCapacity` (6.2 makes that a normal action), a charger saved with a fuller cell throws in
  `loadAdditional`/`load`. Vanilla catches it, logs "Failed to load data for block entity", and returns null
  (javap 1.20.1 `BlockEntity.method_17897` @0-22, exception table Throwable): the chunk keeps the block without its
  block entity, a fresh empty one is created on demand, and the next chunk save overwrites the old data. Same for a
  hostile `/data` value (negative energy, dust 1000, buffer above `chargerRePerRedstone`, carry ≥ 1000).
- Worst outcome: the cell and all dust in every affected charger silently deleted after a config change.
- Proposal: 8.2: "`readData` never throws: energy clamped to [0, capacity], dust to [0, 64], buffer to
  [0, chargerRePerRedstone], carry to [0, 999]; a cell above capacity is cut to capacity at load (as `CellItem.toCell`).
  JUnit on the pure rule's `restore(...)` with absurd inputs; one GameTest loads a tag with energy > capacity."

### I7 — major — decision 6.2/6.3: no upper bounds in validation; new ×1000 arithmetic overflows into exceptions inside ticks
- Problem: `ConfigValidation` checks only `> 0` / `>= 0` (`ConfigValidation.java:58-69`). With a file (6.1) absurd
  values become reachable. Arithmetic (python): `rePerSecond*1000` in `int` wraps from 2 147 484 RE/s on (3 000 000 →
  −64 748 365 milli/tick); `FrameRules.jump` casts `Math.round(jumpCost×1.25)` to `int` (`FrameRules.java:194`),
  negative for `jumpCost > 1 717 986 917` (2e9 → −1 794 967 296) → `EnergyCell.drain` throws
  (`requireNonNegative`); a negative charge in the charger rule throws in `charge`. An exception in a block-entity
  ticker or in the per-player tick is a server crash ("Ticking block entity" / "Ticking player"), repeated at every
  load of that chunk or join of that player. `ExitHold.ticksNeeded` overflows above 107 374 182 s. Bar width
  `13*stored` overflows above 165 191 049 RE.
- Worst outcome: one typo in the config file = a server that crashes in a loop.
- Proposal: 6.3: "Every integer the 3c code multiplies has an upper bound in `ConfigValidation` (capacity ≤ 1 000 000,
  RE rates and costs ≤ 100 000, `chargerRePerRedstone` ≤ 1 000 000, hold/swap seconds ≤ 30); out of range → default
  plus one problem line. All milli arithmetic in `long`. `SuitTick` and the charger ticker wrap their body in
  `catch (RuntimeException)` with one ERROR per player/position and skip the tick (as `FrameEntry.onDeath:192-203`)."

### I8 — major — decision 5.5 / 7.1: a hostile or broken server can crash every client through `client_rules`
- Problem: 5.5 makes the client use the server's `cellCapacity`; 7.1 computes `round(13 * stored / capacity)`. With
  capacity 0 (or negative) from the packet, integer division throws in item rendering (every frame) and any client
  `CellItem.toCell` throws in `EnergyCell`'s constructor. Even a well-meant server with capacity lowered below a stack's
  charge gives width > 13 (bar drawn outside the slot; vanilla clamps since 1.21.1). The creative tab's "charged" cell
  is built from the client's local capacity (stato-del-codice §2.3), so creative stacks above the server capacity are
  normal.
- Worst outcome: client crash loop on joining that server.
- Proposal: 5.5: "The client runs the received values through the same bounds as `ConfigValidation`; an invalid value
  is replaced by the default and logged once." 7.1: "Bar fraction in `double`, `capacity <= 0` → bar hidden,
  width clamped to [0, 13]; tooltip shows the raw stored value."

### I9 — major — decision 12: the verification list does not test any of the invariant cases it depends on
- Problem: 12 lists functional tests only. None checks: swap packet without a worn record (I1); swap/insert with a
  read-only registry (I2); hopper merging dust (I3); charger load with out-of-range data (I6); swap with a source stack
  of count > 1 (I10); menu-close sweep on a wearer (I12). These are the failure modes that break the invariant, and each
  is a 10-line GameTest with the existing helpers (`GameTests.mockServerPlayer`, raw registry helpers,
  `player.gameMode.useItemOn`, direct block-entity tick).
- Proposal: add to 12 the six GameTests above, each asserting item counts before/after (total cells, total dust, worn
  record absent) rather than only the happy path.

### I10 — minor — decision 7.2: "cells do not stack, so the slot is free" is not guaranteed
- Problem: `stacksTo(1)` (`ModItems.java:21`) is the default only: on 1.21.1 `/give @s redstone_exoframes:redstone_cell[max_stack_size=64] 64`
  makes a 64-stack; on 1.20.1 NBT edits/old saves can hold `Count > 1`. If the handler replaces the slot, 63 cells
  vanish; if it shrinks by one, the slot is not free and the old cell has nowhere to go.
- Proposal: "Take the new cell with `split(1)`; put the old one back into that slot only if it is now empty, otherwise
  `Handouts.give`." Same for the charger's hand insert (8.3/8.6) and the frame's hand insert already does `shrink(1)`
  (`FrameInteractions.java:197-201`).

### I11 — minor — decision 7.2: server cooldown is lost at relog and at exit, and refuses honest swaps under lag
- Problem: the cooldown tick lives in the `SuitTick` record removed at `PLAYER_QUIT` and when the worn state ends (3.2):
  relog or exit+enter resets it. Measured in server ticks against a client hold measured in client ticks, a server
  below 20 TPS refuses a second honest swap held for exactly 2 s. Not an invariant break (the "more energy than
  installed" rule forbids ping-pong, so spam is bounded), but the promise "cooldown" is not what it says.
- Proposal: "Cooldown = `cellSwapSeconds × 20 / 2` ticks of `server.getTickCount()`, kept in a `UUID → tick` map that
  survives quit and exit (cleared at server stop); refused packets are dropped silently (no action-bar spam)."

### I12 — minor — decision 11.5: the menu-close sweep must not touch a wearer's armour slots
- Problem: `Placeholders.removeAll` clears the armour slots too (`Placeholders.java:94-96, 122-134`). If 11.5 calls it
  for everybody, a wearer closing any menu loses the four placeholders until the next `SlotGuard` tick (PLAYER_POST):
  `SuitSize.tick` at PLAYER_PRE sees no torso, the player shrinks to vanilla size and grows back (`refreshDimensions`
  twice; possible pose/suffocation glitch). The cursor is not missed: `CLOSE_MENU` fires after
  `containerMenu.removed(player)` on the three loaders (attributi.md §5), which puts the carried stack back into the
  inventory (or drops it, where `SlotGuard.onDrop` already acts), and `player.containerMenu` is still the closing menu.
  A legitimate item cannot be deleted (only the four placeholder items match).
- Proposal: 11.5: "On `CLOSE_MENU` call `SlotGuard.tick(player)` (wearer: strays only; non-wearer: `removeAll`)."

### I13 — minor — decision 2.2: fall wear is computed before the invulnerability window, and a bare frame is free fall immunity
- Problem: `LIVING_HURT` precedes the 10-tick gate on all loaders (danno-verifica C4); a landing inside the window of
  another hit wears the legs for the full amount where vanilla would apply `amount − lastHurt` or nothing. And with no
  legs mounted (bare frame, cheapest recipe) or both broken, the fall is cancelled and the wear goes nowhere: permanent
  fall and ender-pearl immunity at no cost.
- Proposal: "Fall wear uses `max(0, amount − lastHurtAmount)` when `invulnerableTime > 10`. [Q to owner] the fall is
  cancelled only if at least one leg piece is mounted and not broken; otherwise vanilla fall damage."

### I14 — minor — decision 1.2: wear code runs inside vanilla's damage code and must never throw
- Problem: an exception in `SuitDamage.onArmorHurt` propagates out of `Player.hurtArmor` into `hurt` (server tick or
  packet handler): crash or kick. `routeMilli` requires non-negative input; `round(amount*1000)` saturates at
  `Integer.MAX_VALUE` for `/damage … 1e30` (fine), but 1.3's `route(kind, points*1000)` overflows to negative for
  `points > 2 147 483` (tests only). `mutate` returns false on read-only: wear is then silently skipped (acceptable,
  say it).
- Proposal: "`onArmorHurt` catches `RuntimeException` (one ERROR per player); milli amount clamped to [0, sum of
  remaining durability × 1000]; `route` delegates with `Math.multiplyExact` or `long`."

### I15 — minor — decision 3.3/3.7: activity trusts the client's sprint flag, which the server itself clears
- Problem: SPRINT requires `player.isSprinting()`; 3.7 makes the server set it false while the client may keep
  sprinting until the indicator packet arrives, and a hacked client never sends START_SPRINTING while moving at sprint
  speed → drained as WALK. The server never validates horizontal speed (only "moved too quickly"/"wrongly"), so this is
  under-charging, not rubber-banding or kicks.
- Proposal: "SPRINT = moving and (`isSprinting()` or horizontal distance per tick > the walk maximum for the current
  speed attribute × 1.15)."

### I16 — minor — decision 4.2: the 1.20.1 step reset to 0.6 every tick fights other mods
- Problem: "0.6F otherwise, compare-then-set every client tick" rewrites the step of every non-wearer each tick,
  cancelling any other mod's step assist on 1.20.1 (attributi.md §8.6 already warns).
- Proposal: "Set 1.0F while wearing and remember it; reset to 0.6F only if the mod set it and the value is still 1.0F."

### I17 — minor — decision 8.3/8.5 (missing): creative pick-block with data copies a charged cell
- Problem: ctrl+pick-block in creative stores the block entity's data in the item (1.20.1 `BlockEntityTag`, 1.21.1
  `block_entity_data`); placing it recreates cell and dust. Any player who receives such a stack (from a creative
  friend) places it: `BlockItem.updateCustomBlockEntityTag` refuses only when `onlyOpCanSetNbt()` is true (present on
  both versions, mappings `method_5833`).
- Proposal: "`ChargerBlockEntity.onlyOpCanSetNbt()` returns true." Dispensers do not place block items (default drop),
  `/setblock`/`/data` are operator commands: no rule needed; `/setblock` over a charger deletes its contents through
  `Clearable` exactly like a chest (accepted, vanilla).

### I18 — minor — decision 8.4: blockstate writes must not happen in `removeItem`/`setItem`
- Problem: with a full hopper below and a full cell, vanilla calls `removeItem` then `setItem` back every 8 ticks
  (javap 1.21.1 `tryTakeInItemFromSlot` @41, @85). If `cell`/`lit` are updated inside those methods, the block flips
  twice per attempt (`setBlock` + light update + `onRemove` on a property change, blocco.md §1.2).
- Proposal: "Container methods only change fields and call `setChanged()`; the ticker derives `cell`/`lit` once per
  tick and calls `setBlock` only when the derived state differs."

### I19 — minor — decision 8.2/7.2 (missing): what a cell loses when it becomes primitives, and capacity changes
- Problem: in the charger and in the frame a cell is an `EnergyCell`; anvil name and any other stack data are lost
  (blocco.md rec. 3). A cell taken out of a frame keeps an energy above a lowered capacity in its stack
  (`CellItem.fromCell` does not clamp) and is silently cut at the next install (`CellItem.toCell:24-28`); 7.2 compares
  raw stack energy, so such a cell wins the comparison and then loses charge.
- Proposal: one line in the design: "A cell keeps only its charge (name and other data are dropped); a charge above the
  configured capacity is cut when the cell enters a frame or a charger. The swap compares charges after that cut."

### I20 — minor — decision 7.2: "first cell with more energy" with several cells
- Problem: with installed 20 %, inventory 30 % (slot 1) and 90 % (slot 5), the swap takes 30 %; a second swap is
  needed (with cooldown). The offhand is never scanned.
- Proposal: "Take the fullest cell with more energy than the installed one (ties: first in hotbar, main, offhand order)."

### I21 — minor — decision 3.2: per-player record key and stale records
- Problem: if keyed by the `ServerPlayer` object, the End-exit respawn (new object, worn state not ended) leaves the old
  record until quit and resets the meter; if keyed strongly, a leak. Jump-statistic deltas are not bounded (a stats
  reset by another mod gives a negative delta; a loop or multiply on it can drain negative → `drain` throws).
- Proposal: "Records keyed by UUID; jump delta clamped to [0, 1] per tick; previous jump stat re-read when the record is
  created."

### I22 — minor — decision 6.2: writing the default file
- Problem: "file missing → written with the defaults" on a read-only filesystem or a directory-in-the-way must not
  abort init; two servers sharing a config folder can read a half-written file and then never rewrite it ("never
  overwritten").
- Proposal: "Defaults written to a temp file and moved atomically; any write failure is one WARN and defaults are used;
  the file is written only when absent."

### I23 — minor — decision 11.4: exit refused when another frame holds the spot is a griefing lever
- Problem: the frame item refuses overlap with frames and blocks, not with players; a griefer can place frames at a
  wearer's resting spot to block voluntary exit (the wearer must walk away; in a 1-wide shaft only death frees them).
- Proposal: "When the resting spot is taken, the refusal message says so; the frame item also refuses a spot whose
  box intersects a player wearing a frame."

### I24 — minor — decision 8.3 (missing): creative and the charger
- Problem: `FrameInteractions.insertCell` does not shrink the hand in creative (`FrameInteractions.java:199-201`); if the
  charger copies it, every creative insert+withdraw mints a charged cell. 8.6 says "shrink"; the creative case is not
  written.
- Proposal: "The charger always moves the item, creative included (it is a container)."

## Answers to the questions of the lens (scenario table)

| # | Situation | As written | Closed by |
|---|---|---|---|
| 1 | Swap: cell in offhand | ignored | I20 |
| 1 | Swap: creative | works on server inventory; a creative client setting the same slot in its inventory screen can overwrite it (creative-only dup, accepted) | — |
| 1 | Swap: cell with foreign data (energy > capacity, malformed) | clamped by `toCell`; comparison on raw value | I19, I2 (never throw) |
| 1 | Swap: several cells | first better, not best | I20 |
| 1 | Swap: installed fuller than all | refused with message (fine) | — |
| 1 | Swap: no cell installed | any charge accepted (fine) | — |
| 1 | Swap: inventory changed during the hold | server rescans at handling (fine) | — |
| 1 | Swap: death/exit during the hold | `update` creates a phantom record | I1 |
| 1 | Swap: packet spam / hacked client | bounded by "more energy" rule; cooldown resettable | I1, I11 |
| 1 | Swap: riding / menu open | harmless server-side; client refuses with a screen open | — |
| 1 | Swap: read-only registry | loss/dup | I2 |
| 2 | Hopper + player same tick; two players | serial on server thread (fine) | — |
| 2 | Hopper extraction when no dust | cell stays forever; a player can still withdraw (accepted) | — |
| 2 | Hopper feeding dust | dust deleted unless live stack | I3 |
| 2 | Break while charging / explosion | `onRemove` drops cell and dust; buffer < 1 dust lost (decided) | — |
| 2 | Piston push | not pushable: `PistonBaseBlock.isPushable` default branch returns `!hasBlockEntity()` (javap 1.20.1 @237-249, 1.21.1 same); keep `pushReaction` default (DESTROY would drop, BLOCK is also fine) | — |
| 2 | Chunk unload/reload, save between ticks | primitives + carry persisted (fine) | — |
| 2 | Crash after insert/withdraw + quit | vanilla chest guarantee | I5 |
| 2 | Capacity lowered with a cell inside / NBT out of range | load throws → contents deleted | I6 |
| 2 | Comparator loop | charger ignores redstone input: no loop; `setChanged` per charging tick like a furnace (fine) | — |
| 2 | Creative pick-block / `/setblock` / `/data` / dispenser | pick-block copy → I17; others operator or vanilla | I17 |
| 3 | Crash rewinds wear and energy (mutate, not structural) | rewinds a number, moves no item: not exploitable beyond vanilla rollback; a cell swap is structural and saved with the players | — |
| 3 | Dirty every tick | registry written at every autosave while a wearer is online (was: only after changes): one file per 6000 ticks, `.dat_old` then at most one autosave old — cost negligible, recovery better; quit save unchanged (structural only) | — |
| 3 | Piece breaking inside `getDamageAfterArmorAbsorb` | 1.2 defers attributes to the next tick: the breaking hit keeps that piece's protection — same as vanilla, where a broken armour item's modifier is removed only by `detectEquipmentUpdates` next tick. Accept and write it down | I14 (no throw) |
| 4 | Fall: totem, ender pearl, dripstone, riding, slow falling, creative | cancelled before death (no totem use); pearl (5, event-changeable on Forge/NeoForge) and stalagmite are `is_fall`; horse falls hurt riders with fall → leg wear; slow falling never produces the event; creative excluded explicitly (needed: event precedes `abilities.invulnerable` on Fabric/Forge) | I13 |
| 4 | Another mod cancels before ours | Forge/NeoForge HIGH handler not called for a cancelled event; Fabric loop stops: no wear (fine) | — |
| 4 | Resistance V | vanilla still wears armour (hurtArmor before magic absorb): same | — |
| 5 | Indicators in flight | no kicks: the server validates only collision and "too quickly"; sprint is under-charged | I15 |
| 5 | 1.21.1 step after exit | server 0.6 while client still 1.0 for one RTT: at worst a "moved wrongly" correction (rubber-band), not a kick | — |
| 5 | 1.20.1 step | resets other mods' step | I16 |
| 6 | Absurd config | crash loops | I7, I22 |
| 6 | Hostile `client_rules` | client crash | I8 |
| 7 | Menu-close sweep | safe for items and cursor; wearer flicker | I12 |
