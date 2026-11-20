# Part 3c "Giocarci" — the director's decisions (draft 1, 15 Nov)

Status: DRAFT to be attacked. Each decision is marked **[D]** (binding text of the design, not open),
**[P]** (the director's proposal: open to a better idea) or **[Q]** (a question that will also be put to
the owner, with the default that applies meanwhile). Facts are in the dossiers, not here.

Sources (read them, do not trust this file for facts):
- Binding design (Italian): `/home/user/exosuits/docs/superpowers/specs/2026-11-13-m3-telaio-design.md`
  — part 3c: §4.6 row 3c, §5.3 last bullet, §5.6, §5.7, §5.8, §5.9, §6, §7 (points 21-26), §8, §9, §11.
- Game document (Italian): `/home/user/exosuits/docs/superpowers/specs/<documento iniziale>`
  and the first design `…/specs/2026-11-12-redstone-exoframes-design.md`.
- Code map: `<scratchpad>/m3c/stato-del-codice.md`. Real-client experiments (water, step, sprint, data per
  version): `<scratchpad>/m3c/ricognizione-movimento-e-dati.md` (+ `.patch`), also committed in the repo under
  `docs/superpowers/specs/ricognizioni/`.
- Six fact dossiers and their independent verifications: `<scratchpad>/m3c/verifica/{danno,test,blocco,dati,attributi,repo}.md`,
  `…-verifica.md`, and `completezza.md` (some verifications may not exist yet).
- `<scratchpad>` = la cartella di lavoro temporanea della sessione

"Done when" of part 3c **[D]**: a complete game: build (recipes), charge (charger), enter, consume energy,
take damage (pieces wear, break), swap the cell, recharge. Four variants, one source tree. Nothing is ever
duplicated or lost (the mod's invariant since 3b).

---------------------------------------------------------------------------------------------------------

## 1. Damage on the pieces

1.1 **[P]** (replaces the letter of §5.7 "in LIVING_HURT") Wear is computed at vanilla's own armor-wear point,
    not in `EntityEvent.LIVING_HURT`: one common mixin, no Stonecutter directive, `@Mixin(Player.class)`,
    `@Inject(method = "hurtArmor(Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At("HEAD"))`,
    forwarding to `frame/SuitDamage.onArmorHurt(Player, DamageSource, float)`. Reason: LIVING_HURT fires before
    shield and the 10-tick invulnerability window on all four variants (and before difficulty scaling and the
    creative check on Fabric and Forge, and on the client there): lava would wear the suit ten times per hit
    vanilla applies. `hurtArmor` receives the amount after difficulty, shield and the window, before armor
    absorption, only for damage that does not bypass armor: exactly "il danno prima dell'armatura, come fa
    vanilla". It is one more mixin (the design allows mixins only where no event covers the need): it must be
    added to design §3, §7 and `docs/compat.md`, and to `tools/check_jars.sh` (exact mixin lists and
    per-loader name markers).
1.2 **[P]** `SuitDamage.onArmorHurt`: server side only; amount > 0; player has a worn frame; then
    `Compat.damage().classify(player, source)` → `DamageRouter.routeMilli(kind, round(amount*1000), mounted, carry)`
    → `Piece.damage` on each recipient → all inside ONE `Compat.frameState().mutate` (non-structural).
    The visual summary is redrawn by `SlotGuard` at the next player tick (already so); effects are recomputed
    by the energy tick (§3) which notices that `FrameEffects` changed (a piece broke) and re-applies attributes.
1.3 **[D]** `DamageRouter` gains the overload in thousandths; `route(kind, points, …)` delegates to it
    (`routeMilli(kind, points*1000, …)`), so every existing test keeps passing unchanged.
1.4 **[P]** Classification: sources in the vanilla tag `damages_helmet` (anvil, falling block, stalactite)
    classify as `DamageKind.HEAD` (today an anvil classifies GENERIC because its source position is at the
    victim's feet). Game document: "testa → elmo".
1.5 **[P]** When a piece breaks (durability reaches 0): action-bar message to the wearer naming the piece,
    and one vanilla sound (item break) at the player. The piece stays mounted, broken (core rules already
    define the effects of broken pieces).
1.6 **[D]** No damage reduction hook in M3 (protection comes from attributes). M5 adds it.

## 2. Fall damage

2.1 **[D]** Cancelled in `EntityEvent.LIVING_HURT` (`EventResult.interruptFalse()`), for a player in a frame,
    server side, when the source is in the tag `is_fall` (fall, stalagmite; also the ender-pearl landing, which
    is fall-typed).
2.2 **[P]** The cancelled amount is routed as WEAR with `DamageKind.FALL` (primaries: the two legs), in the
    same `SuitDamage` code path as 1.2 (game document: "caduta → gambe"; "nessun danno da caduta" is about the
    pilot). No wear in creative/spectator or when `abilities.invulnerable`.
2.3 **[P]** The vanilla fall sounds stay (they play before `hurt`): a three-block-tall suit landing with a thud
    is right. The fall statistic stays. Known and accepted: on NeoForge a cancelled incoming-damage event
    leaves one `DamageContainer` on the entity's stack per cancel (loader behaviour, a few bytes).
    Alternative considered and rejected for now: a mixin on `LivingEntity.calculateFallDamage(FF)I` returning 0
    (silences the sounds, needs one more mixin, and loses the amount for the leg wear).

## 3. The energy tick (server)

3.1 **[D]** For every player in a frame, each server tick: activity (idle / walk / sprint) from movement;
    jumps from the vanilla jump statistic; `FrameRules.tick` and `FrameRules.jump` through `mutate`; one
    `EnergyMeter` per player in a temporary map emptied when the player leaves the server.
3.2 **[P]** Registered on Architectury `TickEvent.PLAYER_PRE` (server side): there the player's position is
    exactly the last one accepted from the client (at PLAYER_POST it carries a discarded server physics step).
    New class `frame/SuitTick` with a small per-player record (meter, previous position, previous jump
    statistic, last applied `FrameEffects`, tick of the last sync, tick of the last cell swap), created on
    first use, removed at PLAYER_QUIT and when the worn state ends.
3.3 **[P]** Activity: horizontal distance between the current position and the remembered previous position;
    `> 0.01` blocks in the tick (config value `energy.walkThreshold`, builder's value) means moving; moving and
    `player.isSprinting()` → SPRINT; moving → WALK; otherwise IDLE; a passenger (vehicle, mount) is IDLE;
    a teleport (distance > 8 blocks in a tick) counts as IDLE.
3.4 **[Q]** Creative (`abilities.instabuild`) and spectator: no drain at all (activity, jumps), effects still
    apply. Default: as written.
3.5 **[D]** Owner sync: immediately at structural changes (piece, cell, energy state NORMAL/LOW/EMPTY),
    otherwise at most 4 times a second (every 5 ticks) when the energy changed since the last sync.
3.6 **[P]** Effects: the tick computes `FrameRules.evaluate(state)` each tick; when the result differs from
    the last applied one: `Compat.attributes().apply(player, fx)` and an immediate owner sync. On the
    transition into LOW and into EMPTY: an action-bar message and one vanilla sound for the wearer.
3.7 **[D]** Sprint off under the low threshold on server and client: the server calls
    `player.setSprinting(false)` when `!fx.canSprint()` and the player is sprinting (its own accounting); the
    client side is §5.
3.8 **[P]** The meter's carry (thousandths of RE) is not saved: it is lost at logout (at most 1 RE).

## 4. Attributes and the one-block step

4.1 **[D]** `FrameAttributes.apply` becomes idempotent: it reads the present modifier and touches it only
    when amount or operation differ (so no attribute resend when nothing changed). Called when effects change
    (3.6), at login and at respawn (PLAYER_JOIN and PLAYER_RESPAWN: a new `ServerPlayer` has no modifiers);
    `clear` when the worn state ends (exit, death, debug remove). All through the single "worn state changed"
    point of §11.1.
4.2 **[P]** Step height (design §8.2d: "il passo sì, nella 3c"): 1.21.1: one more transient modifier
    `redstone_exoframes:frame_step`, `+0.4 ADD_VALUE` on `generic.step_height`, part of `apply`/`clear`
    (server, synced to the client by vanilla). 1.20.1: no attribute; the client sets
    `setMaxUpStep(1.0F)` on the local player while it wears a frame and `0.6F` otherwise, checked every client
    tick with a compare-then-set (a new `LocalPlayer` is created at every respawn and dimension change, so a
    one-time set is not enough); the server needs nothing (`ServerPlayer` already has 1.0 on 1.20.1). The step
    does not depend on energy. Proven with real clients on the four variants (experiment report).
4.3 **[Q]** A one-block step also lets the suit walk up any full block without jumping (not fences or walls:
    1.5 high). Default: accepted.

## 5. Movement decided by the client: water and sprint

5.1 **[P]** Water (design §5.6: "niente nuoto, si affonda e si cammina sul fondo"): the rule proven by
    experiment: a suit in water does not swim, does not rise, does not jump off the bottom higher than on
    land; it falls as in air and walks or sprints on the bottom at land speed; currents do not push it;
    breathing, bubble columns and lava are vanilla. One common mixin `mixin/PlayerMovementMixin`
    (`@Mixin(Player.class)`, no directive): `isAffectedByFluids()Z` at RETURN (false for a suit in water and
    not in lava), `isPushedByFluid()Z` at RETURN (false for a suit), `updateSwimming()V` at TAIL
    (`setSwimming(false)` for a suit). Logic in `frame/SuitMovement`, which asks one question: "does this
    player wear a suit, and can it swim?" — on the server from the registry, on the client from the synced
    state (owner packet for the local player, the torso summary for other players).
5.2 **[Q]** Walking on the bottom at full land speed (default) or slower; lava left vanilla (default).
5.3 **[P]** Sprint: client mixin `mixin/client/LocalPlayerSprintMixin` (`@Mixin(LocalPlayer.class)`) on the
    private `hasEnoughFoodToStartSprinting()Z` at RETURN → false when the local player wears a suit whose
    synced `canSprint` is false. The server side alone does not stop a sprint (proven).
5.4 **[D]** The owner packet gains three indicators: `canSprint`, `canSwim`, energy state. **[P]** They are
    computed from the state with `FrameRules.evaluate(state)` when the packet is built (the entering sync is
    sent before dressing, so the indicators must be a function of the state alone). The client cache exposes
    them; unknown (no packet yet) means: sprint allowed, no swimming if the suit is drawn.
5.5 **[P]** New S2C packet `client_rules` sent at PLAYER_JOIN: `cellCapacity`, `cellSwapSeconds`,
    `exitHoldSeconds` (the client needs the server's values: the cell item stores only its charge, the
    capacity comes from the config; the two hold keys count on the client). A common holder returns the
    synced values on a client connected to a server and the local config otherwise; reset at disconnect.

## 6. Config from a file

6.1 **[D]** The config is read from a file (design §5.5/§7.23: "la lettura della config da file arriva con la 3c").
6.2 **[P]** `config/redstone_exoframes.json` under `Compat.platform().configDir()`, read with Gson (Gson is
    forbidden only under `core/`) by a new directive-free class outside `core/` (for example
    `config/ConfigFile`), pure Java (a `Path` in, an `ExoframesConfig` out) so JUnit covers it. Called as the
    first statement of `RedstoneExoframes.init()` → `setConfig(...)`. Rules: file missing → written with the
    defaults (pretty-printed), defaults used; file valid → `ConfigValidation.validate` and used; file
    unreadable, not JSON, with a null section or an unknown enum key → ONE error line, defaults used, the file
    is left untouched (never overwritten). Unknown extra keys are ignored. No reload command in M3.
6.3 **[P]** New values: `energy.chargerRePerSecond = 50`, `energy.chargerRedstoneSlots` not needed (see 8),
    `energy.walkThreshold = 0.01`. `energy.chargerRePerRedstone = 500` and `energy.cellSwapSeconds = 2` exist.

## 7. The cell: item, bar, swap

7.1 **[D]** `redstone_cell` is the real cell: charge in the stack data, a charge bar.
    **[P]** Bar: `isBarVisible` when the cell is not full; width `round(13 * stored / capacity)`; colour
    redstone red. Tooltip line "stored / capacity RE" through a new tooltip seam (an item base class in
    `compat/impl` behind a directive-free class, like `EntityBase`/`CompatEntity`), because
    `Item.appendHoverText` differs between the versions. Scrap piece items get the same: a durability bar from
    the stack's own data (durability / maxDurability) and a tooltip line.
7.2 **[D]** Cell swap: a dedicated key, a 2 s hold on the client with an indicator on the action bar, then
    C2S `cell_swap`; the server swaps the installed cell with the first charged cell of the inventory.
    **[P]** Key default `R` (no vanilla default binding; `V` is exit). The hold reuses `client/ExitHold`
    with its own instance. Server rule: scan hotbar then main inventory in slot order for the first
    `redstone_cell` holding MORE energy than the installed one (or any charge when the frame has no cell);
    none → action-bar message, nothing changes; found → the old cell (if any) goes into that same slot (cells
    do not stack, so the slot is free), the new one into the frame, through `update` (structural: immediate
    sync and save rules), one sound. A server-side cooldown of `cellSwapSeconds` between two swaps of the same
    player (the hold cannot be verified by the server). The swap is refused in a menu? (client: no screen
    open, like exit.)

## 8. The charger

8.1 **[D]** A block with a block entity, no GUI: right-click with a cell inserts or withdraws it, with
    redstone dust adds fuel (1 dust = 500 RE, config), charges at a config rate; hopper-compatible if cheap.
8.2 **[P]** Model, furnace-like: slot 0 = one cell, slot 1 = redstone dust (a normal stack, up to 64), plus an
    internal buffer in RE of the dust being burnt. Each server tick, when a cell is present and not full: if
    the buffer is empty and slot 1 has dust, one dust is consumed and the buffer gains `chargerRePerRedstone`;
    then up to `chargerRePerSecond / 20` RE per tick (with a thousandths carry) move from the buffer to the
    cell. The rule lives in a pure-Java class under `core/` with JUnit; the block entity only holds the state
    and calls it. Persisted as primitives (cell present, cell energy, dust count, buffer, carry): no item
    stack in the NBT.
8.3 **[P]** Interaction (one neutral per-hand method, behind the versioned `use` / `useItemOn`): holding a
    cell → insert it if the charger is empty, otherwise swap it with the one inside; holding redstone dust →
    move as many as fit into slot 1; empty hand → withdraw the cell (to the hand); anything else → pass. Each
    interaction shows the status on the action bar (cell charge, dust count). Blockstate properties: `facing`
    (horizontal, set at placement), `cell` (present), `lit` (charging now; emits a little light). An opaque
    cube: no client registration. Comparator output from the cell's charge (0–15).
8.4 **[P]** Hoppers: the block entity implements vanilla `WorldlyContainer` (works on the four variants with
    no loader code): top → slot 0 accepts a cell; sides → slot 1 accepts redstone dust; bottom → extracts the
    cell only when it is full. Max stack 1 for slot 0.
8.5 **[P]** Breaking the block drops the cell (with its charge) and the dust (the buffer of the dust being
    burnt is lost: less than one dust). `onRemove` must act only when the block really changes (it also fires
    on a property change). Loot table: the block drops itself; tag `minecraft:mineable/pickaxe`; hardness like
    a furnace; requires a pickaxe.
8.6 **[P]** Two players, one charger: every interaction is a single server-thread action on the block
    entity; a cell is either in the charger or in an inventory, never both (insert = shrink the hand stack and
    set the slot in the same call; withdraw likewise).

## 9. Recipes and data

9.1 **[D]** Workbench recipes: frame with iron, copper and redstone; Scrap pieces with iron and copper; cell
    with redstone, copper and quartz; charger with iron, copper and a redstone block. Exact shapes to the
    builder. A generator `tools/gen_data.py` writes both trees (`src/main/resources-mc1.20.1/`,
    `-mc1.21.1/`); files committed; CI checks they are fresh.
9.2 **[P]** Shapes (I iron ingot, C copper ingot, R redstone block, r redstone dust, Q quartz):
    exoframe `ICI / IRI / I I`; scrap_helmet `ICI / I I`; scrap_torso `I I / ICI / III`;
    scrap_arm `CI / _I / _I`; scrap_leg `I_ / I_ / IC`; redstone_cell `_C_ / rQr / _C_` (crafted EMPTY: it
    must go through the charger); charger `ICI / CRC / III`. All results count 1. One recipe-unlock
    advancement per recipe (criterion: has copper ingot). Plain item ids, no convention tags.
9.3 **[P]** `tools/check_jars.sh` gains: each jar holds only its own version's data folders.
9.4 **[D]** English and Italian text for every item, block, key and message.

## 10. Sounds, and what the wearer sees

10.1 **[D]** (§11) One vanilla sound each for placing a frame, mounting and unmounting a piece or the cell,
     entering, leaving. **[P]** Plus: cell swap, charger insert/withdraw, piece break, energy LOW/EMPTY.
     Only constants that are plain `SoundEvent` on both versions (no `Holder` ones).
10.2 **[P]** A minimal energy readout for the wearer: one line of text on the HUD (stored RE and percent,
     coloured by energy state), client only, no texture, through a small client seam (the HUD callback
     differs between the two Architectury versions). Without it the player cannot see the charge at all; the
     real HUD arrives in M4. This is a builder's addition to show the owner.

## 11. Carry-overs of design §11 — first tasks of the plan

IN (each before the features that lean on it):
11.1 One point for "the worn state changed" (enter, exit, death, debug wear/remove, slot repair, join,
     respawn): placeholders, summary, size refresh, sync, log line (also for the debug command), and now
     attributes apply/clear and the per-player tick record.
11.2 `FrameStateSavedData` split: registry operations vs file format and tolerance rules; a file that parses
     but has no `data`/sections is treated as damaged (read-only, one error) instead of an empty writable
     registry; the "chunk has its entities loaded" predicate written once.
11.3 `/exoframe remove` and the line of sight move to `frame/`; the line of sight equals the click's aim
     (block OUTLINE shapes; with the eyes inside a frame's box that frame wins).
11.4 Step out of the frame: a small step up (farmland, path, soul sand, carpet); per-axis paths for
     diagonals; more hazards (campfire, magma block, sweet berry bush, wither rose, powder snow, cactus,
     portals) and entities under the spot. **[P]** Two frames at one spot: a voluntary exit is refused with a
     message when the resting spot is taken by another placed frame (the frame item already refuses that
     case); death and forced relocation keep today's behaviour (never lose anything).
11.5 A placeholder that escaped the guard is removed when a menu closes (`PlayerEvent.CLOSE_MENU`).
11.6 The save at player quit is narrowed to the player files and this registry's file, and honours `/save-off`.
11.7 `tools/log_allowlist.txt`: exact counts (per loader where they differ), `TESTREG` matching a path with
     spaces. The in-world test class is split (new tests of 3c in new classes) and `tools/run_gametests.sh`
     counts every file.

DEFERRED again, with the reason (stay in §11 with the new destination):
- Unknown ids inside a readable record → the first part that adds a tier, slot or module id (M4/M5): the
  written rule (raise the schema) still protects, and 3c adds none.
- Module checks at mount → M4 (no module items yet). Picked-up frame loses `reinforced`/`unlocked` → M4
  (the station sets them). A piece of a tier without item → M5. Shrinking world border → before the public
  alpha, with the command that lists frames. Exit from more than 1.35 blocks above ground → stays as is
  (pinned by a test; not a defect).

## 12. Verification of part 3c

- JUnit: `routeMilli`, charger rule, config file, cell-swap choice (pure), activity classification (pure).
- GameTest on the four variants: energy consumption over N ticks; LOW and EMPTY transitions; no drain in
  creative; wear from a known hit; two hits inside the invulnerability window count once; damage that
  bypasses armor does not wear; fall without damage and with leg wear; attributes applied once and re-applied
  at respawn; step modifier on 1.21.1; cell swap (with and without a charged cell, cooldown); charger
  charges, hopper in and out, break drops; recipes resolve; stray placeholder removed at menu close; the
  step-out cases; exit refused over another frame.
- Real client in the cloud (the scripts of the experiment): water, step, sprint off under 10 %, the HUD line,
  the bars; screenshots in the repo.
- Dedicated server start/stop on the four variants. A PC checklist `docs/test-in-gioco/m3c-giocarci.md`.

---------------------------------------------------------------------------------------------------------

## ADDENDUM 1 (15 Nov, after the six verifications and the completeness critic) — director's rulings

Facts: `<scratchpad>/m3c/verifica/*-verifica.md` and `completezza.md` (23 gaps). No refuted claim changes a
decision above. Rulings on the gaps (numbers = gap numbers in `completezza.md`):

- G1 wear point: decision 1.1 stands (mixin at `Player.hurtArmor` HEAD). The design text §5.7 is amended.
  First step of the damage task: compile the mixin and prove it on the four variants. Do NOT also hook
  `hurtHelmet` (double count); `damages_helmet` sources classify as HEAD inside the `hurtArmor` path (1.4).
  The wear handler must never throw into vanilla's damage code (catch, log once, continue).
- G2 indicators: the packet carries `canSprint`, `canSwim`, energy state as the design says (5.4), computed by
  the server from the state and the SERVER config. Consumers: sprint mixin (`canSprint`), water rule
  (`canSwim`, always false in M3, kept for the swim module of a later milestone), HUD line and messages
  (energy state). Before the first packet: sprint allowed, no swimming when the suit is drawn. The client
  never compares energy with its own threshold. A public, directive-free read API for client code and for
  `frame/SuitMovement` (rule 4: `compat.impl` cannot be named outside `compat/`).
- G3 energy feedback: decision 10.2 stands (one HUD text line, client seam), plus the action-bar messages on
  LOW and EMPTY. It is a builder's addition listed in design §7 for the owner ("niente GUI" stays true: no
  screen).
- G4 client values: decision 5.5 stands (S2C `client_rules`, common holder, reset at disconnect); the cell
  stack format does not change. Bar widths are clamped to [0, 13] by the mod (Forge/NeoForge do not clamp).
- G5 fall: decisions 2.1-2.3 stand (LIVING_HURT with `is_fall`, wear to the legs, sounds stay: the pilot's
  own fall sound is played by its client and cannot be silenced from the server anyway). Ender pearl and
  stalagmite: no damage to the pilot, wear to the legs.
- G6 creative/spectator: no drain and no wear when `abilities.instabuild` or spectator (branch on abilities,
  never on `isCreative()`: test mocks always answer true). A wearer switched to spectator keeps the record;
  the water and sprint rules do not apply to spectators. One GameTest per mode.
- G7 riding: accepted (builder's default; stays a question for the owner, design §8.2c): a passenger is IDLE,
  jumps are not counted while riding. PC checklist: seated pilot under a two-high ceiling.
- G8 mixins: three new ones in 3c: `mixin/PlayerMovementMixin` (common: water), `mixin/PlayerWearMixin`
  (common: `hurtArmor`), `mixin/client/LocalPlayerSprintMixin`. The two common ones stay separate classes
  (one concern each). No `calculateFallDamage` mixin. Early step: all three added, GameTests on the four
  variants and one real-client start per variant before anything is built on them.
- G9 activity: a pure classifier in `core/` with JUnit (inputs: horizontal distance in the tick, sprint flag,
  passenger, teleport); threshold from one real-client measurement (walk vs stand vs packet jitter), default
  0.01 until measured; vertical-only movement (ladders) counts as WALK when |dy| is over the threshold.
  Jumps: statistic delta, capped at 1 per tick; no jump cost when the cell is empty (nothing to drain).
- G10 step and water do not depend on energy, cell or broken legs. 1.20.1 step: client, every tick,
  compare-then-set, and only undo what the mod itself raised (remember that it raised it).
- G11 config: file name `redstone_exoframes.json`; read once at init; no reload in M3 (documented);
  `Compat.platform().configDir()` inside the mod constructor is proven by the first dedicated-server run of
  the task on Forge and NeoForge.
- G12 charger: decisions 8.2-8.6 stand; any player or hopper may take any cell from a charger (as with any
  vanilla container; a locked frame is the protected storage). No dust refund for the buffer. Slots hold
  real `ItemStack` objects (hoppers on 1.21.1 edit the stack returned by `getItem`); every limit is enforced
  in `canPlaceItem`/`canPlaceItemThroughFace`, never by a max stack size. The block class name ends in
  `Block`. Whatever is stored must survive save/load without losing stack data (custom name included).
- G13: first charger step = the two versioned bases and an empty block, registered and GameTested on the four
  variants (place, save/load through a test helper, break).
- G14 cell swap: decision 7.2 stands ("charged" = holds more energy than the installed cell; hotbar, then
  main inventory, then offhand; old cell into the same slot; server cooldown = `cellSwapSeconds`). If both
  hold keys are down, exit wins and the swap hold resets.
- G15 per-player records: keyed by UUID; cleared by the single worn-state notifier (exit, death) and at
  PLAYER_QUIT; position baseline reset at respawn, dimension change and teleport. Losing the meter carry is
  accepted.
- G16 tests: the mock-tick probe is the first GameTest of the part; the test class is split; CI duration is
  measured after the first test task.
- G17: classification stays as the core defines it plus the `damages_helmet` rule; the 0.75 helmet factor on
  1.21.1 is vanilla behaviour and stays. Unit of the milli overload: thousandths of a damage point.
- G18 FOV narrowing from the speed modifier: left as vanilla does for slowness; PC checklist item.
- G19: second frame at the resting spot → voluntary exit refused (11.4); exit above the frame → as today;
  small step up = up to 0.5 block (slabs excluded? no: any collision top within 0.5 above the frame's feet);
  picked-up frame keeps nothing (deferred, M4); unknown-pieces rule kept; sectionless file = damaged (11.2).
- G20 data: unlock advancements shipped (one per recipe); only `mineable/pickaxe`; plain item ids;
  `validate()` in the generator enforces count 1 and known categories; never write `remove` in tag files;
  `check_jars.sh` checks per-version folders; a GameTest asserts each recipe by key and the charger in
  `MINEABLE_WITH_PICKAXE` together with stone.
- G21 sync: the owner sync is immediate when `FrameEffects`, the energy state or any piece's condition
  changed since the last sync; otherwise at most every 5 ticks when energy or durability changed.
  Attributes are re-applied by the energy tick, never inside the `hurtArmor` hook (the hit that breaks a
  piece still gets that piece's protection).
- G22 knockback resistance does not cover explosions (vanilla attribute semantics on both versions).
- G23: PC checklist item (the 1.21.1 data pack may show as "made for an older version").

---------------------------------------------------------------------------------------------------------

## ADDENDUM 2 (15 Nov, after the four examinations and their challenges) — director's rulings

Reports: `<scratchpad>/m3c/esame/{partita,architettura,invarianti,prove}.md` and `…-contestazione.md`.
Where this addendum and the text above disagree, THIS ADDENDUM WINS (then Addendum 1, then the draft).

### R1. Delivery in two merges (builder's choice, design §4.6)
3c-1 "Consumare": harness, the §11 carry-overs, config file, sounds, damage and fall, attributes and step,
energy tick, packets and indicators, water and sprint, bars and tooltips, cell swap, HUD line, debug energy
command; proven with creative-tab items. 3c-2 "Ricaricare": charger, data generator, recipes, remaining
texts, PC checklist, the end-to-end survival test. Each is merged to `main` when verified; the binding
"done when" of part 3c is checked at the end of 3c-2. Two plan documents; the second is written after the
first is executed.

### R2. Cell swap (replaces 7.2 where different)
- Entry point `frame/CellSwap.handle(ServerPlayer, long now)` (public static: tests call it; the packet
  handler only forwards). It does nothing unless: a worn record exists, the player is alive, not a
  spectator, and `containerMenu == inventoryMenu`.
- New seam method `FrameStateAccess.changeWorn(ServerPlayer, Consumer<FrameState>) : boolean` — existing
  record only, structural, syncs, returns false when the record is absent or the registry is read-only.
  `update` (which creates the record) is never used by 3c features.
- Order: choose the slot → `changeWorn` (old cell out, new cell in) → only if it returned true take the item
  with `split(1)` and put the old cell into that slot if it is now empty, otherwise `Handouts.give`.
- Choice (declared override of the design's "la prima cella carica"): the FULLEST cell that holds more than
  the installed one (any charge when none is installed); ties → lowest index; scan hotbar 0-8, main 9-35,
  off hand last. Pure rule `core/energy/CellSwapRule.pick(int installedStored /* -1 = none */, int[] slotEnergies) : int`.
- Cooldown on the server: `now - lastSwap < cellSwapSeconds * 10` ticks (half the hold, so a slow server does
  not refuse an honest player), in a UUID→tick map that survives quit and exit; refused packets are dropped
  silently. The client counts the hold only with no screen open; if both hold keys are down, exit wins.
- One capacity everywhere: the config's. A charge above it is cut when a cell is read into a frame, a
  charger or a swap; comparisons use the cut value. A cell is only its charge: a custom name or other stack
  data do not survive a frame or a charger (already true for frames since 3b) — written in the design.
- NEW: clicking a PLACED frame that already has a cell while holding a cell swaps them (same permission
  check, same atomic order: the old cell is handed out only after the registry change succeeded).
- Creative follows the 3b convention everywhere (frame, charger): the held item is not consumed.

### R3. Activity, jumps, riding, beds (replaces 3.3, G7, G9)
- Pure `core/energy/ActivityRule` (JUnit): a tick is "moving" when any of the last 4 samples of horizontal
  distance exceeds 0.01 block (constants in the class, no config field; drop `energy.walkThreshold`);
  moving and `isSprinting()` → SPRINT; moving → WALK; else IDLE. The window resets on dimension change,
  teleport (> 8 blocks) and a new player object. Horizontal only: climbing counts as IDLE (accepted).
- Jumps: delta of the jump statistic clamped to [0, 1] per tick; baseline read when the record is created; a
  decrease resets it; no charge while `hurtTime > 0` (knockback lift-off) and none when the cell is empty.
  Auto-jump turns step-ups into paid jumps: accepted, PC checklist.
- [Q, default as written] No riding and no sleeping in a suit: a suited player who becomes a passenger is
  dismounted at once by the tick (covers every vehicle and mount of any mod), and a bed is refused, each
  with the action-bar message "leave the exoframe first". Reason: seated in a two-high tunnel the suit's
  eyes are inside the ceiling (suffocation bypasses armor); waking puts a 2.97-high box in a low room.

### R4. The tick record and the notifier (replaces 3.2, 4.1 details, 11.1, G15)
- `frame/SuitTick.tick(ServerPlayer, long now)` public static; the PLAYER_PRE listener passes
  `server.getTickCount()`. Every time rule compares `now` with a stored long (never `now % n`);
  `now < last` resets throttle and window.
- The record is bound to the `ServerPlayer` INSTANCE (weak keys, like `SuitSize.LAST_SEEN`): a new object
  (login, respawn, return from the End) has no record, so its first tick applies attributes and syncs. No
  respawn hook is needed in `frame/`.
- First evaluation of a record: attributes + sync; if the state is LOW or EMPTY the message WITHOUT the
  sound. Later transitions into LOW or EMPTY: message + sound. Back to NORMAL: nothing.
- 11.1 is a pure refactor: `frame/WornState.changed(player, event)` called after the registry write at enter,
  leave (exit and death), debug wear/remove: size refresh + log line (the debug log line is the only new
  behaviour). Placeholder removal stays where it is (order constraints of the code map); `SlotGuard` is
  untouched (it never writes the registry). The feature tasks then add to that point: attributes
  apply/clear and dropping the tick record.
- No `isCreative()` anywhere in 3c code (test mocks always answer true): branch on abilities.
- `SuitTick` and the charger ticker catch `RuntimeException`, log one error, and go on.

### R5. Sync and indicators (replaces 3.5, 3.6 sync part, G21)
- One rule: every `mutate` of a worn frame sets a dirty bit in the record; the owner sync is immediate on a
  structural change or when `FrameEffects`, the energy state or any piece's condition changed; otherwise at
  most every 5 ticks while dirty.
- `compat/FrameIndicators(canSprint, canSwim, EnergyState)`; the client cache stores them with the state;
  public read API on the seam for client code (own state + indicators of the local player).
- The water rule does NOT read the packet: `frame/SuitMovement` decides with `SuitSize.wearsSuit(player)` on
  both sides (the torso placeholder, as the size does). `canSwim` stays in the packet only because the
  design binds it; nothing reads it in M3.
- Proof of syncs in tests: a test-only counter `GameTests.syncCount(ServerPlayer)` incremented in `syncTo`;
  JUnit round trip of each packet codec.
- `client_rules`: the client checks received values against the config bounds (bad value → default, logged
  once); holder rule: the value received since the last client disconnect, else the local config;
  server-side code never reads the holder.

### R6. Mixins of 3c (replaces G8)
Two new classes: `mixin/PlayerSuitMixin` (common, four forwarders: `hurtArmor`, `isAffectedByFluids`,
`isPushedByFluid`, `updateSwimming`) and `mixin/client/LocalPlayerSprintMixin`. Both mixin json files,
`tools/check_jars.sh` (exact lists, per-loader name markers), design §3 and §7, `docs/compat.md`. Early
step: add them with empty handlers, build the four jars, run the checks, GameTests and one real-client
start per variant.

### R7. Damage and fall (adds to 1.x, 2.x)
- `routeMilli` returns the unabsorbed part in points; `route` delegates without overflow; the caller clamps
  `round(amount * 1000.0)` to [0, Integer.MAX_VALUE].
- The wear handler never throws into vanilla and never cancels the hit; messages and sounds happen after
  `mutate` returns; wear is skipped when the registry is read-only.
- Second hit inside the invulnerability window: equal or smaller adds no wear, larger adds the difference
  (that is what vanilla passes to `hurtArmor`).
- Fall wear is a DECLARED override of design §5.7 ("damage that bypasses armor is skipped"): fall damage is
  cancelled for the pilot and its amount wears the legs (other pieces when the legs are missing or broken;
  nothing on a bare frame). Guards: server player, alive, `!abilities.invulnerable`, amount > 0. No
  invulnerability-window arithmetic for falls. Fall immunity is a base effect of the frame (core rule).
- Action-bar message also at the transition of a piece into CRITICAL. Helmet-source hits: the 0.75 factor of
  1.21.1 is accepted; exact-number tests use cactus.
- Armor enchantments and armor-based mob rules (piglins and gold, powder snow and leather boots, Respiration)
  do not apply in a suit until the M5 tiers: one line in the design and in the checklist.

### R8. Attributes and step (replaces 4.2 for 1.20.1)
`FrameAttributes.localStep(Player, boolean wearing)` in the versioned implementation (empty on 1.21.1),
driven from the existing per-tick size hook for the local player: raise to 1.0 while wearing; go back to 0.6
only if the mod raised it and the value is still 1.0 (flag per `LocalPlayer` instance). The `FrameAttributes`
Javadoc and `docs/compat.md` are amended in the same task (idempotent apply; a fifth modifier on 1.21.1).
Tests: every GameTest exists on all four variants; version differences go through compat test helpers
(`stepIsAttribute()`, `frameModifier(player, id)`); the 1.20.1 step is proven by the real client only.

### R9. Water (replaces the 5.1 wording)
A suit cannot jump while touching water (not even in water one block deep); with the one-block step it
climbs one-block banks only. One action-bar hint at the first water contact of each entry ("the exoframe
sinks: walk out, or hold the exit key to leave it"). [Q, default no] should a suit on the bottom jump as on
land?

### R10. Config (adds to 6.x)
`configfile/ConfigFile.load(Path) : Result(config, problems, wroteDefaults)`, no logging inside; the caller
logs one WARN per problem; a file with problems is left untouched; defaults are written through a temporary
file and an atomic move, only when the file is absent; a failed write is one WARN. `ConfigValidation` gains
upper bounds (capacity ≤ 1,000,000; rates and costs ≤ 100,000; `chargerRePerRedstone` ≤ 1,000,000; hold and
swap seconds ≤ 30; multipliers ≤ 10): out of range → the default and one problem line. Products that can
overflow are computed in `long`. GameTests start from the defaults: `tools/run_gametests.sh` removes the run
directory's config file before each run.

### R11. Charger (replaces 8.3 details, G12 last sentence)
- Main hand only (off hand passes). Holding a cell: insert, or swap with the one inside. Holding dust: move
  as many as fit. Empty hand: take the cell; if there is none, take the dust; sneaking with an empty hand:
  status only. Status texts fixed: no dust / no cell / cell full / charging N %.
- Nothing changes on the client side of the click. Every content change calls `setChanged()`. The ticker
  derives `cell` and `lit` once per tick and writes the blockstate only when it differs; container methods
  only change fields. Loading never throws: every stored value is clamped. `onlyOpCanSetNbt()` is true.
- Slot 1 (dust) is a live `ItemStack` field, a count on disk; slot 0 (cell) holds only the charge on disk.
- Known limit, listed in design §7 next to point 26 and put to the owner: the charger has the vanilla
  container guarantee (chunk data, saved at autosave): a withdraw or insert followed by a quit and a crash
  before the next autosave can duplicate or lose a cell. No mitigation in M3.
- Entry points for tests: `ChargerBlockEntity.serverTick(...)` public static and the neutral click method;
  pure `core/energy/ChargerRule` (JUnit). Block class `ChargerBlock`; seams `compat/CompatEntityBlock`,
  `compat/CompatBlockEntity` (bases in `compat/impl`, not `BaseEntityBlock`); registration order blocks →
  items → block entity types.

### R12. What the player is told (replaces 10.2 status: now part of the design)
- The HUD energy line is in (declared in design §2 as the one exception to "no HUD" and in §7): stored RE,
  percent, coloured by state; plus the worst piece when one is CRITICAL or BROKEN. Hook added to the
  existing `compat/client/ClientRenderHooks` seam.
- Tooltips through the tooltip seam: frame item (place it, mount pieces and a cell, empty hand to enter,
  sneak with an empty hand to take things off), cell (charge; "empty: charge it in a charger"), charger,
  pieces (durability; a stack without data says "new", no numbers).
- One chat line at the first entry of a session naming the two keys (`Component.keybind`). The "no cell"
  refusal names the charger. Events (LOW, EMPTY, piece CRITICAL/BROKEN, charger status) use the action bar;
  a hold indicator may overwrite them for its duration (accepted: the HUD line keeps showing the state).
- All sounds through `frame/FrameSounds`, played by the server with `level.playSound(null, …)`, with a
  test-only recorder so GameTests assert which event each action plays.
- Bars: pure `core/energy/CellBar` (width clamped to [0, 13] from `min(stored, capacity)`, hidden when the
  capacity is not positive).

### R13. Carry-overs (replaces 11.2, 11.6, 11.7 details)
- 11.2a pure split (delegates under today's names, no test changes) → 11.2b damaged-file rule ("no schema
  and none of the sections" = damaged; an explicit empty-registry helper for tests) → 11.2c the predicate.
- 11.5: at CLOSE_MENU run the guard's sweep: strays only for a wearer, all placeholders for a non-wearer.
- 11.6: the quit save writes all player files and this registry's file only (not the whole overworld
  storage); it does NOT honour `/save-off` (vanilla writes the quitting player's file anyway: honouring it
  would split the pair) — the reason is written; this closes the §11 item. After 11.2a.
- 11.7: `check_log.py --variant`, counts `=N` or per loader; `run_gametests.sh` counts `add(tests,` over all
  `gametest/*.java`; one registration per test class; shared helpers in an annotation-free class;
  `check_jars.sh` checks the five data folder kinds by name.
- 11.8 (new): `/exoframe debug energy` prints stored/capacity/state; `/exoframe debug energy <RE>` sets it
  (clamped, through `changeWorn`): the cloud client and the PC checklist need it to reach LOW and EMPTY.

### R14. Tests the plan must contain (adds to §12 of the draft)
Harness first: survival-mock helpers, end of spawn protection, a probe test, the wrong comment fixed. Energy:
exact cases on a survival mock primed with one call (20 idle ticks −2 RE, 20 walking −8, 20 sprinting −20,
one jump −15, passenger and teleport idle, creative beside survival). Fall: with a frameless control in the
same test. Break: a piece at durability 1, then the tick and the guard → armor value drops, one message.
Water: `doTick()` once and the precondition asserted, a frameless control. Swap: no worn record → nothing
changes; conservation of cell count and total RE. Charger: direct-tick charge, real-time ticker, `lit`
toggled twice keeps the cell, hopper in and out, break drops, save/load. Data: each recipe by key, the
charger and stone in the pickaxe tag. One end-to-end survival test at the end of 3c-2. The client script
switches to survival and asserts that the HUD number changes.

### Questions for the owner (collected; each has the default that applies meanwhile)
quartz in the cell recipe (default: as the design says; alternative: quartz or amethyst) · jump cost 15 RE
makes sprint-jumping empty a cell in under 4 minutes (default: his value stays) · no riding, no sleeping in
a suit · water: no jump at all, full land speed on the bottom, lava vanilla · creative drains nothing ·
the step climbs any full block · FOV narrows with the speed penalty (strongly at EMPTY) · the charger's
vanilla-container guarantee · any player or hopper can take a cell from a charger · no repair until M4.
