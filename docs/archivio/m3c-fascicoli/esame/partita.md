# Examination of "direzione-decisioni.md" (draft 1) — lens: the player

Read-only. Sources: the decisions file, design §5.4-§5.9/§7/§8/§11, game document, `src/main/java` (paths below
relative to `src/main/java/com/toxineren/exoframes/`), dossiers `verifica/{danno,test,attributi}.md`,
`ricognizione-movimento-e-dati.md`. Arithmetic in `esame/nums.py` (python3, run). No verification files
(`…-verifica.md`) exist yet, so the dossiers are taken as written.

## Findings, most severe first

### P1 — blocker — 7.2: the cell swap goes "through `update`", which CREATES a worn record when none exists
- Evidence: `compat/FrameStateAccess.java:62-68` — `update` "Crea lo stato se assente, applica la modifica … e
  sincronizza", returns `void`. The `cell_swap` packet is sent after a 2 s client hold; it can arrive after the
  player exited (V, 1 s, can be held at the same time), died, or was `debug wear via`-ed. Then `update` makes a
  bare worn record holding the inventory cell; `SlotGuard` autorepair ("stato presente senza segnaposto: li rimette",
  design §5.5) then dresses the player in a frame that was never crafted: a frame duplicated from nothing, and the
  cell taken out of the inventory into it.
- Proposal: 7.2 must say: "server: refuse unless the player is alive, not spectator, and `wornFrameId(player)` is
  present; the change goes through a write that fails without creating (`mutate` + explicit `syncTo` + structural
  save flag, or a new `updateIfPresent` returning boolean); order: pick the inventory slot → change the record →
  only if that returned true, put the old cell (or empty) in that slot". Add a GameTest: swap packet with no worn
  record changes nothing.

### P2 — major — missing (3b `FrameInteractions.insertCell`/`unmount` + 7.2): a placed full frame with a dead cell cannot be recharged without stripping a band
- Evidence: `frame/FrameInteractions.java:180-185` — holding a cell on a frame that has one → `SLOT_TAKEN`
  ("That slot is already taken"); `:212-232` — sneak + empty hand takes the cell only when the clicked band
  (`core/frame/ClickZones.java:47-59`: head / middle / legs) has no piece. A full suit fills all three bands.
  `frame/FrameEntry.java:115-118` refuses entry with an empty cell. The ordinary loop of the done-when ("run out,
  exit, recharge, re-enter"; also every death by drowning/empty cell) therefore needs: unmount the helmet, sneak-click
  the head again to get the cell, insert the new one, remount the helmet — a rule no text tells the player
  (P8), and the message the player does get says the slot is taken.
- Proposal: add to 3c (one line in 8.3's spirit): "holding a cell on a placed frame that already has one: swap —
  the held cell goes in, the old one goes to the hand slot (same atomic order as the charger, 8.6)". Same
  permission check as today. GameTest: swap on a placed full frame.

### P3 — major — 9.1/9.2 (cell recipe): the whole suit is gated behind the Nether; a player without quartz never enters
- Evidence: `redstone_cell` `_C_ / rQr / _C_` needs `minecraft:quartz`; in vanilla 1.20.1/1.21.1 quartz comes only
  from the Nether (ore, bastion loot, piglin barter; masons sell quartz blocks, which do not uncraft). Entry requires
  a cell with charge (`FrameEntry.java:115-118`), cells are crafted empty (9.2) and the ruined village that would
  hand out a cell is M6. Scrap is "crafting base" in the game document (tier table, row 1). The Nether trip also makes
  P9 (frame at the bottom of a lava sea) a normal outcome.
- Proposal: put it to the owner as a [Q] (the ingredient is in binding §5.8): default "the crystal is a tag
  `redstone_exoframes:cell_crystals` = {`minecraft:quartz`, `minecraft:amethyst_shard`}" (Overworld geodes, same
  depth as redstone). The generator writes one tag file per version (dossier dati §4); no code.

### P4 — major — 3.3: activity from ONE tick's distance misclassifies a walking player as IDLE
- Evidence: `verifica/test.md` §3 ("Per-tick values are jittery (0, 1 or 2 client packets land in one server
  tick), so classify over a short window … not on one tick") and §6.6. With 3.3 as written every tick without a
  move packet is IDLE (2 RE/s instead of 8 or 20): the drain depends on network timing (less on a laggy server, more
  in single-player), the 20-minute promise of the game document becomes unmeasurable, and the GameTest
  "consumption over N ticks" passes while real clients diverge.
- Proposal: 3.3: "moving = horizontal distance summed over the last 10 ticks (ring buffer in the per-player
  record) > 10 × walkThreshold; SPRINT = moving and `isSprinting()`; reset the buffer on level change and on a step
  > 8 blocks". Pure function with JUnit (12 already lists "activity classification (pure)").

### P5 — major — missing (design §8 q.2c still open): riding and sleeping in a 3-block suit are undecided, and the likely outcome is suffocation
- Evidence: the decisions only say "a passenger is IDLE" (3.3). Suit eyes are at 2.67 above the feet
  (`ricognizione-movimento-e-dati.md` 1(a)). Seated in a minecart the eyes are about 3.0 above the rail block, i.e.
  inside the ceiling of an ordinary 2-high rail tunnel; `in_wall` bypasses armor (dossier danno §4 tag list), so it
  kills without wearing anything [deduction, not run]. A horse puts the eyes ~4.3 up. Waking from a bed puts a 2.97
  standing box into a 2-high bedroom (design §8 q.2c names it). Nothing in 3c decides.
- Proposal: new decision "while wearing a suit: `InteractionEvent.INTERACT_ENTITY` on a vehicle or mount
  (`Boat`, `AbstractMinecart`, `AbstractHorse`, `Pig`, `Strider`, `Camel`) and `RIGHT_CLICK_BLOCK` on a `BedBlock`
  are refused with an action-bar message 'Leave the exoframe first'" — both events exist on both versions with the
  same signature (`verifica/attributi.md` §5); one GameTest each. Owner can lift it later.

### P6 — major — 5.1: the text contradicts the experiment, and the player can be stuck in 1-deep water
- Evidence: 5.1 says the suit "does not jump off the bottom higher than on land"; the experiment says it does not
  jump at all while touching water ("in acqua profonda 1, Spazio 2 s → Y invariata", recon 1(a); the jump is inside
  the `isAffectedByFluids()` block of `LivingEntity.aiStep`, `verifica/test.md` §4). So in a stream or swamp a
  2-block bank is a wall; the only ways out are a 1-block step or leaving the suit. Air is 15 s at eye height 2.67,
  stowed Respiration/turtle helmet do nothing (placeholders in the head slot), and falling into a lake sinks the suit
  to the bottom. Nothing tells the player.
- Proposal: (a) correct 5.1 to "cannot jump while touching water, even in 1-deep water"; (b) [Q] to the owner:
  "a suit standing on the bottom jumps as on land" (default: no, as tested); (c) one action-bar line on the first
  water contact of each entry: "The exoframe sinks: walk out, or hold V to leave it"; (d) add "1-deep water with a
  2-block bank" and "fall into a 6-deep lake" to the PC checklist.

### P7 — major — 9.2 + config: the numbers make Scrap neither cheap nor fragile, and jumping dominates the drain
- Evidence (table A/B below): kit = 29 iron + 7 copper + 9 redstone for frame and six pieces, 34 iron / 12 copper /
  20 redstone / 1 quartz with cell and charger, versus 24 iron for vanilla iron armour of equal protection
  (`FrameRules.IRON_ARMOR_POINTS = 15`). Arms and legs are crafted one at a time although one item fits both sides
  (`content/PieceKind.java`), so the player crafts 6 items. Versus mobs Scrap outlasts iron on every slot but the torso
  (vanilla wears each piece at least 1 per hit, dossier danno §2; the router splits the hit). A cell lasts 20.8 min
  walking (matches the game document) but only 3.7 min sprint-jumping, the normal way to travel: `jumpCost 15` = 2 s of
  walking per jump. Knockback that lifts the player counts as a jump (`verifica/test.md` §2), so each mob hit costs 15
  RE too.
- Proposal: `scrap_arm` and `scrap_leg` yield 2 (suit = 23 iron, close to iron armour); `jumpCost` 15 → 6
  (sprint-jumping 5.6 min instead of 3.7, walking with a jump every 2 s 15.2 min instead of 10.8); skip the jump charge while `hurtTime > 0`.
  Keep durability 180, `chargerRePerSecond = 50`, idle/walk/sprint rates.

### P8 — major — 10.2/9.4/missing: the essential controls are not taught anywhere in game
- Evidence: `src/main/resources*/…/lang/en_us.json` has no tooltip text; the only hints are refusal messages. A new
  player cannot learn: right-click the frame with an EMPTY hand to enter (`FrameInteractions.java:122-131`; with
  any item the click silently passes); sneak + empty hand to unmount, band by band, cell last; hold V to leave; hold R
  to swap; that a crafted cell is empty and needs a charger ("The exoframe needs a charged cell" names no charger);
  that the suit sinks (P6); that the charger needs redstone dust. 10.2 (the HUD line) is only a [P] "to show the
  owner", yet without it the energy of the done-when is invisible.
- Proposal: make 10.2 [D]. Add [D] "tooltips through the 7.1 seam": exoframe item (3 lines: place; mount pieces and a
  charged cell by right-clicking; empty hand to enter, sneak + empty hand to take parts off), cell ("Empty: charge it
  in a Charger" when 0), charger block item ("Right-click with a cell, then with redstone dust"). On every entry, one
  chat line with `Component.keybind` for both keys: "Hold [V] to leave, hold [R] to swap the cell". `no_cell` text:
  "…needs a charged cell (charge cells in a Charger)".

### P9 — major — missing (3b rest-spot rule, design §11 Luigi q.10): a pilot who dies in lava loses the frame at the bottom of the lava
- Evidence: design §11 "Acqua e lava": fluids are not ground, the frame settles on the bottom; P3 sends every
  player to the Nether, where lava seas are ~30 deep. Entering or unmounting needs the player inside the lava next
  to the frame. Lava wear is irrelevant (table C: the pilot dies in ~5 s), the loss is the frame.
- Proposal: [Q] to the owner, default for 3c: "a rest spot inside lava counts as 'no ground': last safe position,
  with the chat message of §5.5". Two lines in `frame/` plus one GameTest; if declined, write the risk in the
  checklist.

### P10 — major — 1.5/missing: piece wear is invisible in first person and nothing can repair it in 3c
- Evidence: first-person arm stays vanilla until M4 (design §7.6); the HUD line (10.2) shows energy only; the armour
  bar moves only when a piece breaks. `repairKitFraction` exists (`ExoframesConfig.java:47`) but no item uses it; the
  station is M4. A damaged piece can be replaced only by crafting a new one (torso: 7 iron). With `primaryShare`
  and `shares`, the torso takes 30 % of every generic hit and always breaks first (table C: 200 zombie hits versus
  480 for an arm), so the torso is the consumable.
- Proposal: 1.5 adds an action-bar message at the transition into CRITICAL (<15 %), not only at break; the HUD line
  appends the lowest piece as "Torso 12 %" when any piece is below 50 %. [Q] to the owner: "no repair in 3c" (default)
  with a piece tooltip "Repaired at the maintenance station (coming)".

### P11 — minor — 7.2: the swap picks the first better cell, not the best, and ignores the offhand
- Evidence: 7.2 "first `redstone_cell` holding MORE energy than the installed one", hotbar then main inventory.
  A 15 % cell in slot 1 is taken before a full one in slot 20; 2 s hold plus cooldown to fix it.
- Proposal: "the cell with the most energy (ties: lowest slot), searched in hotbar, main inventory and offhand; it
  must hold more than the installed one".

### P12 — minor — 8.3: the only way to read the charger's state withdraws the cell
- Evidence: 8.3 "empty hand → withdraw the cell"; the status line appears only on interaction. Checking progress
  means taking the cell out and putting it back.
- Proposal: "sneak + empty hand → status only (cell charge %, dust, 'no redstone' / 'cell full' / 'charging')";
  empty hand standing → withdraw. Also say the status texts explicitly, so "why the charger does nothing" has an answer
  ("No redstone dust", "No cell", "Cell full").

### P13 — minor — 3.6/attributes: an empty cell narrows the field of view to 63 %
- Evidence: `verifica/attributi.md` §8.4: FOV × (m+1)/2 → ×0.6275 at speed 0.255 (EMPTY), on top of the slowness.
  With two broken legs speed is 0.184 (0.85 × 0.85² × 0.3 incl. base): ×0.59. Players read it as a bug.
- Proposal: [Q] owner; default: accept, write it in the checklist; if refused, a client hook that ignores the
  frame's speed modifier in `getFieldOfViewModifier` (one more client mixin — weigh against design §3).

### P14 — minor — 4.2: the 1.20.1 client resets `maxUpStep` to 0.6 every tick when not wearing a suit
- Evidence: 4.2 "0.6F otherwise, checked every client tick"; `verifica/attributi.md` §8.6 warns "remember that
  the mod raised it, so the reset does not fight another mod" (step-assist mods, Forge's own step).
- Proposal: "set 1.0 when wearing and remember it; restore 0.6 once, only if the value is still the 1.0 the mod
  set".

### P15 — minor — 3.1/3.6: the LOW/EMPTY message fires on every login and after every respawn of a LOW suit
- Evidence: 3.2 creates the per-player record "on first use" with no last applied `FrameEffects`; the first
  evaluate is a transition from nothing.
- Proposal: "a missing previous value is not a transition: no message, no sound; attributes and sync still apply".

### P16 — minor — 7.1: a freshly crafted piece has no data, so its tooltip/bar uses the client's config
- Evidence: `content/PieceItem.java:56-58` — no data → `Piece.fresh` from `RedstoneExoframes.config().tiers` (local);
  5.5 syncs only `cellCapacity`, `cellSwapSeconds`, `exitHoldSeconds`. On a server with other durability values the
  tooltip of a new piece lies until it is mounted once.
- Proposal: tooltip of a stack without data says "New" and no numbers (the bar is hidden anyway: full).

### P17 — minor — 8.2/8.5: the charger re-creates the cell from primitives and drops its other data
- Evidence: 8.2 persists "cell present, cell energy"; a cell renamed in an anvil (or with any other component)
  comes out as a plain cell. Not a loss of the item, but the player's name tag disappears.
- Proposal: accept and write it ("only the charge survives the charger"), or store the stack (the 3b stow code
  already saves stacks per version).

### P18 — minor — missing: piglins are always hostile to a suit, and other armour effects are gone
- Evidence: the four armour slots hold placeholders (`frame/Placeholders.java:35-41`); no gold piece is "worn",
  so piglins attack; leather boots no longer stop powder snow, Respiration/turtle shell no longer help (P6). With P3
  the first suit trip is to the Nether.
- Proposal: one line in the checklist and in the design (§5.6): "armour enchantments and armour-based mob rules do
  not apply while in a suit (until M5 tiers)".

### P19 — minor — 2.2: fall wear numbers are fine; say what happens with broken legs
- Evidence: table D: a 4-5 block drop (1-2 points) costs 0.3-0.6 per leg, 300-600 drops per leg; a 50-block fall
  14.1. Not a grind. With both legs broken the router moves the whole fall to the other pieces
  (`DamageRouter.java:160-162`), so a broken-legged suit wears its torso by falling.
- Proposal: keep 2.2; write "broken legs: the fall still never hurts the pilot, and wears the other pieces".

## Tables

**A. Kit cost with 9.2 as written** (I iron, C copper, r redstone dust, Q quartz; R block = 9 r)

| Item | I | C | r | Q | Crafts |
|---|---|---|---|---|---|
| exoframe `ICI/IRI/I_I` | 6 | 1 | 9 | | 1 |
| scrap_helmet `ICI/I_I` | 4 | 1 | | | 1 |
| scrap_torso `I_I/ICI/III` | 7 | 1 | | | 1 |
| scrap_arm `CI/_I/_I` ×2 | 6 | 2 | | | 2 |
| scrap_leg `I_/I_/IC` ×2 | 6 | 2 | | | 2 |
| **suit total** | **29** | **7** | **9** | | 7 |
| redstone_cell `_C_/rQr/_C_` | | 2 | 2 | 1 | 1 |
| charger `ICI/CRC/III` | 5 | 3 | 9 | | 1 |
| **kit total** | **34** | **12** | **20** | **1** | |
| vanilla iron armour | 24 | | | | 4 |
| proposal P7 (arm, leg ×2) | 23 suit / 28 kit | 5 suit | | | 5 |

**B. Energy** (`ExoframesConfig.Energy`: 10 000 RE, 2/8/20 RE/s, jump 15, LOW < 10 %)

| Use | RE/s | Full cell | Until LOW (no sprint) | With jumpCost 6 |
|---|---|---|---|---|
| idle | 2 | 83.3 min | 75.0 min | same |
| walk | 8 | 20.8 min | 18.8 min | same |
| sprint | 20 | 8.3 min | 7.5 min | same |
| walk + jump every 2 s | 15.5 | 10.8 min | 9.7 min | 15.2 min |
| sprint-jump (jump every 0.6 s) | 45 | 3.7 min | 3.3 min | 5.6 min |
| mixed (40 % idle, 40 % walk, 20 % sprint, jump / 3 s) | 13 | 12.8 min | 11.5 min | 16.7 min |
| charger, proposed 50 RE/s | — | full in 200 s, 20 dust | — | — |
| dust per hour of use | walk 57.6, sprint 144 | | | |

**C. Wear** (router: GENERIC splits by shares; HEAD 60 % to helmet; durability 180; vanilla iron 165/240/225/195,
each piece `max(1, x/4)` per accepted hit)

| Hit (Normal) | worst slot wear / hit | Scrap: hits to break worst slot | Vanilla iron: hits to break chestplate |
|---|---|---|---|
| zombie, 3, GENERIC | torso 0.90 (helmet 0.45, arm 0.375, leg 0.45) | 200 (arm 480) | 240 (every piece −1/hit) |
| arrow 4 at the head, HEAD | helmet 2.4 | 75 | helmet 165 |
| creeper point blank 43, GENERIC (feet-level centre) | torso 12.9 | 14 | 24 (−10/piece) |
| lava, 4 every 10 ticks = 8/s | torso 2.4/s | 75 s (pilot dead in ~5 s) | chest −2/s, 120 s |

**D. Fall wear (2.2, FALL kind: 60 % to the two legs)**

| Fall | points | per leg | torso | drops to break a leg |
|---|---|---|---|---|
| 4 blocks | 1 | 0.30 | 0.17 | 600 |
| 5 blocks | 2 | 0.60 | 0.34 | 300 |
| 20 blocks | 17 | 5.10 | 2.91 | 35 |
| 50 blocks | 47 | 14.1 | 8.06 | 13 |

## Session walk-through (where the player gets stuck)

1. Gather iron, copper, redstone: fine. Quartz: Nether only (P3).
2. Craft frame and 6 pieces: 7 crafts, arms/legs one at a time (P7). Recipe book unlocks on copper (9.2): fine.
3. Craft cell: comes out empty, bar black (7.1); craft charger; dust in, cell in: 200 s; status only by clicking,
   which withdraws (P12).
4. Place frame, mount: right-click with each piece (sound per 10.1). Mount the cell. Enter: needs an EMPTY hand —
   untold (P8).
5. Walk/sprint/jump: −15 % speed, one-block step (4.2), drain visible only with 10.2 (P8); sprint-jumping empties
   the cell in under 4 min (P7).
6. Fall: no damage, legs wear slightly (D). Zombie/skeleton/creeper: wear per C; piece state invisible until it
   breaks (P10).
7. Water: sinks, no jump even in 1-deep water, 15 s of air (P6). Lava: vanilla, frame lost if the pilot dies there
   (P9).
8. LOW: message + sprint off; EMPTY: 25 % speed and narrow FOV (P13). Swap with R: works if a better cell is in
   the inventory (P11); the server path must not create records (P1).
9. Exit with a dead cell, go recharge, come back: cannot replace the cell without unmounting the helmet (P2).
10. Ride a minecart or sleep in the suit: undecided, likely suffocation (P5).
11. Multiplayer: two players at one charger fine (8.6); anyone can take a cell from anyone's charger (vanilla-like,
    accepted); dedicated-server values reach the client through 5.5 except new-piece tooltips (P16); a friend in an
    unlocked frame can swap the owner's cell into his own inventory (7.2 rule; acceptable, write it).
