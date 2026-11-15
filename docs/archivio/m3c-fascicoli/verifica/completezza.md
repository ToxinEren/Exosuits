# Completeness critique for the plan of part 3c ("Giocarci")

Read-only. Inputs: design `docs/superpowers/specs/2026-11-13-m3-telaio-design.md` (§4.6 row 3c, §5.3 last bullet, §5.6-§5.9, §6, §7 points 21-26, §8, §9, §11 first group), `stato-del-codice.md`, the six dossiers and six verifications in `verifica/`, and the finished experiment `ricognizione-movimento-e-dati.md` (here "EXP"; what it proved by a run counts as established). I re-read two code facts myself: `docs/compat.md:275` (the cell's capacity lives only in the frame state; "capacità anche sull'item o presa sempre da `energy.cellCapacity`" is an open direction) and `compat/SuitSummary.java:22,49` (the summary carries tier, condition, appearance per slot; nothing about energy). Everything else below cites the dossier that established it, or says nobody checked it.

Tags: [design §x], [EXP §x], [danno §x], [test §x], [blocco §x], [dati §x], [attributi §x], [repo §x], `-v` = its verification, [code] file:line, [not checked].

## What is settled (no gap; the plan can copy)

- Water (no swim, sinks, walks on the bottom), one-block step, sprint off: mechanisms proved on the four variants by a real client [EXP §1(a)-(c)]; exact targets, remapped names, `check_jars.sh` edits listed there.
- Per-version data folders: no build change; formats of recipes, unlock advancements, self-drop loot, tags; generator and freshness check prototyped [EXP §2, dati §1-§8, dati-v].
- Attribute API for an idempotent `apply`, sync path, re-apply at `PLAYER_JOIN`/`PLAYER_RESPAWN` [attributi §1, attributi-v C1-C5, test §5].
- Mock-player mechanics, recipes for damage/fall/i-frame tests, jump statistic [test §1-§2, test-v].
- Charger: block/BE seams, registration order, hoppers via `WorldlyContainer`, assets in the common tree [blocco, blocco-v].
- Every §11 carry-over located to file:line with a size estimate [stato §9, repo §3-§6].

## Gaps, most important first

### G1. Where piece wear is computed: the design sentence cannot be implemented correctly as written (BLOCKS)
- Design §5.7: wear in `LIVING_HURT`, "se il danno aggira l'armatura si salta". [danno §1, danno-v C1-C4]: on all four the event fires before the i-frame gate and the shield; on Fabric and Forge also before creative/dead checks and difficulty scaling, on NeoForge after them. Consequences: 10x wear in lava/fire/cactus/magma, shield-blocked hits worn in full, creative pilots worn on Fabric/Forge only, different amounts per loader.
- Replacement offered, not proven: common mixin `@Mixin(Player.class)` `hurtArmor(Lnet/minecraft/world/damagesource/DamageSource;F)V` HEAD (SRG `m_6472_`, intermediary `method_6105`). **Never compiled or run on any variant** [danno open points]. Extra rules found by danno-v: do not also hook `hurtHelmet` (double count); the HEAD hook loses vanilla's per-item fire rule (matters only for later tiers); do not touch the armor modifier inside the hook (it runs before `getArmorValue()` of the same hit).
- Why it matters: decides the damage task's structure, the mixin list (§3, §7, `check_jars.sh`) and the test design.
- How: a decision recorded as a design amendment (§5.7 + §7 new mixin exception); then the plan's first damage step is "compile + GameTest on four variants" (danno §8.7 test recipes).

### G2. The owner packet's three indicators: which are needed, and who computes the sprint cut (BLOCKS)
- Design §5.3/§7.23: `canSprint`, `canSwim`, energy state computed by the server "so the client does not evaluate the rules with its own config".
- `canSwim`: EXP's water rule depends only on "wears the suit" (torso placeholder, both sides) and needs no indicator; `FrameRules.evaluate` makes `canSwim` constant false [stato §1.1]. Nobody says what would consume it.
- `canSprint`: EXP §1(c) proposes `SuitMovement.sprintAllowed` reading the energy from the client's copy against the threshold, i.e. the client's `lowThreshold`, which is exactly what §5.3 forbids. The client mixin must read the server indicator instead; nobody wrote that path (`FrameStateClientCache` stores only a `FrameState`; client code may not name `compat.impl`, rule 4) [repo §7(a)].
- Energy state: no consumer identified (no HUD in M3, see G3).
- Also open: until the first packet after login/respawn/dimension change the client has no value (EXP §1(c) "ultimo stato noto"): default allow or deny sprint?
- How: decide the packet's fields and the client read API (public type outside `compat/impl`, rule 1c if in `core/`), then specify `syncTo` computing them from state + server config (repo §12.2 shows this keeps the `enter` ordering correct).

### G3. How the pilot sees the energy of the worn cell: nobody looked (BLOCKS the client part)
- HUD is a non-goal of M3 [design §2]. The worn cell lives in the registry, not in the inventory, so the new charge bar (§5.8) never shows it. The only client feedback in the design is the swap progress on the action bar. Acceptance of 3c is "consumare energia ... cambiare cella" [design §4.6]: the tester cannot tell when to swap except by losing sprint at 10 %.
- How: a design decision (e.g. action-bar message on energy-state change, a percentage on the action bar while a key is held, a tooltip-like line on the torso placeholder, or a status command). It decides whether the energy-state indicator and the raw energy must reach the client and how often (sync budget ≤ 5/s, design §3).

### G4. What the client needs to draw a cell's charge bar, and which server config values reach a remote client (BLOCKS: data format)
- The cell stack carries only `energy`; capacity comes from `energy.cellCapacity` of the reader's config [stato §3.5, attributi §4; `docs/compat.md:275` leaves "capacity on the item or always from config" open]. On a remote server the client's file may differ → wrong bar, and widths > 13 unless the override clamps (NeoForge `getBarWidth` has no clamp, attributi-v C11). The creative tab's charged cell is built from the client's capacity [stato §2.3].
- Same problem for `cellSwapSeconds` and `exitHoldSeconds` (hold timers run on the client from its own file) [attributi §6, §8 open points].
- How: decide (a) capacity written into the cell stack (new `NbtKeys` entry, `ItemDataAccess`, codec docs) or (b) an S2C config packet stored in a common holder cleared on `CLIENT_PLAYER_QUIT` (repo §7(c) gives the shape). Either changes formats/packets, so decide before the plan.

### G5. Fall damage: three mechanisms recommended, none chosen; the pilot's own fall sound is client-side
- danno §8.2: common mixin `LivingEntity.calculateFallDamage(FF)I` RETURN → 0 for a suit wearer (`m_5639_`/`method_23329`, uncompiled) plus `LIVING_HURT` with `isFall` as the net for ender pearls. attributi §8.5: `LIVING_HURT` everywhere, or `FALL_DAMAGE_MULTIPLIER` −1 on 1.21.1 only.
- attributi-v Missed 2: the pilot hears the fall sound from its own client run of `causeFallDamage`; cancelling on the server silences nothing for the pilot. Only a hook that also runs on the client (the common mixin, or the synced 1.21.1 attribute) removes it; on 1.20.1 only the mixin.
- Undecided policies: ender-pearl and stalagmite damage on the pilot; whether a cancelled fall wears the legs (design: no); passengers' falls arrive through the vehicle (test-v C7) — covered by `calculateFallDamage`/`LIVING_HURT`, not by `doCheckFallDamage`.
- How: decision; step inside the effects task, with the frameless control test of test §1.6(c).

### G6. Creative and spectator pilots: nobody looked
- Energy drain in creative? `FrameRules` knows no game mode. Test traps: every mock answers `isCreative() == true`, so mod code must branch on `getAbilities().instabuild/invulnerable` [test §6.4].
- A wearer switched to spectator (`/gamemode spectator`) keeps the record and the placeholders; the exit key does nothing for spectators [design §5.5]; `doTick` still runs; the size mixin, `noPhysics`, the water mixin, `SlotGuard`, drain and attributes in spectator were not examined [not checked].
- How: decide the rule (e.g. no drain and no wear in creative/spectator; spectator forces nothing / is refused); then one GameTest per mode. Not blocking.

### G7. Riding a vehicle in the suit: still Luigi's open question, and its consequences were not checked
- Design §8 2(c) is unanswered (forbid, or accept the edge cases). `FrameEntry.enter` calls `stopRiding()`, but nothing stops mounting afterwards.
- Not checked: suffocation (`in_wall`) of a seated pilot whose eyes stand ~2.67 above the seat under a 2-high minecart tunnel or a bridge [not checked]; how drain behaves (test §3 treats a passenger as not moving → IDLE); jump stat not counted as passenger; fall damage via the vehicle (test-v C7); the suit's box vs boats.
- How: get Luigi's answer or record the builder's default; if "forbid", `InteractionEvent.INTERACT_ENTITY` can refuse mounting (attributi §5) [that every mount path goes through it: not checked]. Needs one client run either way.

### G8. The new mixins: up to four, two never compiled
- Proved by EXP: `PlayerMovementMixin` (common) and `LocalPlayerSprintMixin` (client). Proposed, never compiled: `hurtArmor` (G1) and `calculateFallDamage` (G5).
- Each needs: both mixin configs, `tools/check_jars.sh` `CLIENT`/`common` list (`:143`, `:168`), `TARGETS` (`:144-150`), `MOJANG` (`:152-153`) [danno §3, repo §1]; `docs/compat.md` "Mixin"; design §3 and §7 (they currently list four mixins). Open choice: merge the common `Player` hooks into one class (`check_jars` entries are per class).
- How: step in a task — one task that adds all common mixins and runs the four GameTest servers and the headless client once (`required`, `defaultRequire 1` crash at start-up on a wrong target).

### G9. Activity detection numbers are unmeasured
- test §3: position sampled at `PLAYER_PRE`, window of 5-10 ticks, `isSprinting()` for SPRINT; exclusions for passengers, teleports; test-v adds pistons and spectator cameras. Window length and "moved" threshold need packet-jitter numbers from a real client [test open points]; walk vs sprint distances (8.5 vs 11 blocks / 2 s, EXP) are too close to use distance alone.
- Unspecified: vertical movement (ladders, water bottom, bubble columns) — horizontal-only distance makes a climbing pilot IDLE; jump semantics ("left the ground upwards": knockback, bubble columns count, test §2) vs design's "a ogni salto"; jump cost when energy is EMPTY (`FrameRules.jump` is not gated).
- How: pure JUnit-testable classifier, numbers from one headless-client run (EXP's `prova.sh` pattern, `autoJump:false`). Step in a task.

### G10. Do step height and the water rule depend on energy or broken legs?
- EXP §1(a) "1(a) dipende da 1(b)": without the step the suit cannot leave a ditch with a 1-block bank (no jumping in water). The design does not say whether step/water rules hold at EMPTY energy, without a cell after a failed swap, or with broken legs. If the step is tied to energy, an empty suit is trapped in water.
- Also reconcile 1.20.1 step application: EXP says set it only on transitions (a new `LocalPlayer` counts as one) and notes its per-tick patch was wrong; attributi §8.6 says drive it every tick and remember who raised it. Same outcome if "remember" is implemented; the plan must pick one.
- How: decision; step inside the effects task.

### G11. Config file: format and several fields undecided; one runtime assumption unchecked
- Missing fields the design implies: charger charge rate, fuel buffer size, sync rate (4/s), activity thresholds/window, step height; each needs `ConfigValidation` + `ExoframesConfigTest` [stato §2.1, blocco §8.4].
- Undecided: file name and format (Gson is available; `core/` may not import it), behaviour on an unknown enum key (Gson → `NullPointerException`, attributi §7), whether `/reload` or a new world re-reads it (registry captures the config at load, `FrameStateSavedData.java:619`; `EnergyMeter`/`DamageRouter` snapshot it) [repo §2.4].
- [not checked] that `Platform.getConfigFolder()` (FML `CONFIGDIR`) answers during the `@Mod` constructor on Forge/NeoForge [repo §12.4]. Proof: the first GameTest/dedicated-server run.
- Step in a task, after G4's decision on server→client values.

### G12. Charger behaviour decisions, including two players and one charger
- Open per blocco §8: facing or not; faces for cell/fuel; whether a hopper may pull a cell before it is full; one dust per click or many; click with a cell when one is inside; comparator output; fuel refund on break; the two config values.
- Nobody looked at ownership: chargers have no owner, so any player (or a hopper below) takes any cell; two players clicking are serialized on the server thread, so the only question is policy. Whether the charger shows the inserted cell's charge at all (no GUI, blockstate `lit` only) ties into G3.
- How: decisions; the tests of blocco §8.11. Not blocking.

### G13. Charger seams and the inherited `Block.CODEC` are unproven
- `EntityBlockBase`/`BlockEntityBase` derived from bytecode only; runtime use of `Block.CODEC` returned by a subclass on 1.21.1 not checked [blocco-v C3, Missed 8]; `Block classes should end with Block` error when `IS_RUNNING_IN_IDE` (blocco-v Missed 6) — `ChargerBlock` is safe.
- Hopper traps replaced the dossier's wrong advice: limits must be in `canPlaceItem`/`canPlaceItemThroughFace`, not `getMaxStackSize()` (blocco-v Missed 1); a fresh-copy `getItem` of a multi-item slot loses items on 1.21.1 only (Missed 2).
- How: first charger task = the two bases + an empty block registered and GameTested on four variants (blocco open points). Step in a task.

### G14. Cell swap semantics are underspecified
- Design §5.6: "scambia la cella inserita con la prima cella carica dell'inventario". Undecided: "carica" = non-empty, or fuller than the inserted one; search order (hotbar, main, offhand); where the old cell goes (the slot of the new one is the natural answer); refusal message when none; whether the server enforces the 2 s (a modified client can send `cell_swap` at will; server-side only the packet arrives) and a cooldown; what happens if the exit key is held at the same time (both write the action bar, attributi §6). Must go through `update` (structural + sync), not `mutate` [stato §3.4, repo §7(b)]; energy state must be re-evaluated and synced at once (§5.6).
- Step in a task.

### G15. Lifecycle of the per-player transient maps
- The 3c tick keeps per player: `EnergyMeter` (carry in milli-RE), last `Stats.JUMP` value, last position sample, last `FrameEffects`, last sync time. Design only says the meter map is emptied at quit.
- Not specified: clearing at frame exit/death (or the next wear inherits a carry/effects), reset of the position sample on respawn (new `ServerPlayer`), End return, dimension change and teleports (test §3), jump baseline on first sight and on decrease (test §6.7). Logging out with a carry loses < 1 RE — acceptable but should be written down. Keyed by UUID, not by object (respawn replaces the object, test §5).
- Step in a task.

### G16. GameTest capacity and the mock-tick probe
- 95 tests today, one 100-tick batch, 20-min CI limit; 3c adds dozens (energy, wear, fall, attributes, step on 1.21.1, charger, hoppers, swap, recipes, tags). Spawn invulnerability needs 60 `tick()` calls per mock (test §1.6(d)). test §6.1 asks for a probe GameTest first (level ticks the mock); the probe is unrun. Splitting the test class requires changing `run_gametests.sh` (repo §9.3). A respawned mock is a plain `ServerPlayer` with 60 fresh ticks of protection (test-v Missed 5).
- Step in a task (first test task).

### G17. Damage classification with the 3-block suit
- Melee never classifies as HEAD (source position = attacker's feet; HEAD needs y ≥ wearer's feet + 2.42) [danno §5]; anvils/falling blocks are GENERIC unless "`DAMAGES_HELMET` → HEAD" is added [danno §8.4]. So the helmet wears almost only from high projectiles: a balance consequence no one put to Luigi.
- Vanilla version difference visible in play: on 1.21.1 every pilot takes 75 % from anvils/falling blocks/stalactites (helmet placeholder always present), 100 % on 1.20.1 [danno §7].
- Small decisions: unit of `unabsorbed` in the milli overload; whether an exception in the wear handler may propagate into `hurt`.
- Not blocking; decisions inside the damage task.

### G18. Field of view narrows with the speed modifier
- Client FOV × `(m+1)/2`: ×0.925 at the base 0.85, ×0.6275 at EMPTY (0.255) [attributi §8.4]. Neutralising it needs the Forge/NeoForge FOV event or a mixin on Fabric (attributi-v Missed 5); Architectury FOV event [not checked].
- Decision; at least a PC-checklist item.

### G19. §11 carry-overs that are policy, not code
- Two frames at the same resting spot (where does the second go?); exit more than 1.35 above the frame's feet (also pose-dependent: crouching/swimming eyes, repo-v Missed 2); the size of the small step-up for the step-out; whether a picked-up frame item keeps `owner` besides `reinforced`/`unlocked` (`FrameItemData` has `owner`); whether unknown pieces in a readable record keep the "raise the schema" rule; the "valid file without sections" rule and the test helpers it changes (repo §5.4, repo-v c9).
- How: decisions in the design additions; code is local and mapped (repo §3, §5, §6).

### G20. Data-generator scope
- Undecided: ship recipe-unlock advancements or not (design names recipes only); which tags beyond `minecraft:mineable/pickaxe` (mandatory with `requiresCorrectToolForDrops`, dati §4) — `needs_stone_tool`?; plain item ids as ingredients (conventional tags differ between the two 1.20.1 loaders); a `check_jars.sh` per-version data rule. [not checked] Gradle's behaviour when the same relative path exists in the common and a per-version tree (dati open points). The generator's `validate()` must hold the whole count rule: 1.21.1 `count` falls back silently outside 1..99 (dati-v Missed 5) [DFU semantics not checked].
- Step in a task.

### G21. Effects recomputation and sync after wear
- Wear via `mutate` is not synced; `SlotGuard` redraws the summary within one tick [repo §2.2]. Design §5.6 wants an immediate sync on structural change ("pezzo"): is a piece changing condition or breaking from wear a "structural" sync trigger, and must it also re-apply attributes in the same tick or at the next 3c tick (danno §8.5 forbids doing it inside the hook)? Not specified.
- Step in a task.

### G22. Knockback resistance does not cover explosions
- Explosions read no `KNOCKBACK_RESISTANCE` on either version (1.21.1 has a separate `EXPLOSION_KNOCKBACK_RESISTANCE`; 1.20.1 nothing) [attributi-v C4, Missed 1]. If "resistenza al contraccolpo" (design §2.4) is meant to include blasts, 1.20.1 cannot do it with an attribute.
- Decision; minor.

### G23. 1.21.1 data pack listed as incompatible
- The mod declares pack format 34; 1.21.1 data format is 48. Nothing gates loading, but the data-pack screen may show "made for an older version" [dati §6, dati-v Missed 8; not observed]. Include in the PC checklist; do not change `pack.mcmeta` (`check_jars.sh` (b)).

## Contradictions

1. Design §5.7 (wear computed in `LIVING_HURT`) vs danno §1/§8.1 + danno-v C4 (the event precedes i-frames, shield and, on Fabric/Forge, creative checks and scaling: over-counts and differs per loader). Dossier wins on the facts; the design needs amending (G1).
2. Design §5.6 (water: "lato client per il giocatore locale") vs EXP §1(a) (one common mixin on both sides; server and client must agree on pose and the swim flag). Experiment wins.
3. Design §5.3 (server computes `canSprint` so the client never uses its own config) vs EXP §1(c) "Che cosa deve fare il piano" (`sprintAllowed` compares the energy of the client copy with the threshold, i.e. the client's `lowThreshold`). The mechanism is proved; the data source must follow the design (G2).
4. Design §5.3 (`canSwim` indicator) vs EXP §1(a) (the water rule needs only "wears the suit"; no indicator is read). Unresolved (G2).
5. Design §8 2(d) ("un po' meno [economico] su Fabric 1.20.1") vs EXP §1(b) and attributi §2/§8.6 (same cost on both 1.20.1 loaders: client `setMaxUpStep`). Experiment wins.
6. Design §5.9 (attribute idempotence in JUnit "dove testabile") vs attributi §1.6 + attributi-v C14 (`Attribute` cannot load without a game bootstrap on 1.21.1): it belongs in a GameTest.
7. Design §3 (mixins only where no event covers; lists four) vs EXP §3 and danno §8 (two to four new mixins). Design text must be amended (G8).
8. Design §5.6 ("salti contati dalla statistica ... il server la incrementa a ogni salto") vs test §2 (the statistic counts every ground→air upward move between two packets: knockback, bubble columns, auto-jump; nothing as a passenger). Design wording imprecise.
9. EXP §1(b) (set the 1.20.1 step only on transitions; its own per-tick patch was wrong) vs attributi §8.6 (drive it every tick, remembering who raised it). Compatible only if "remember" is implemented; experiment's warning stands (G10).
10. danno §8.2 (`calculateFallDamage` mixin + `LIVING_HURT` net) vs attributi §8.5 (`LIVING_HURT` everywhere or the 1.21.1 attribute); attributi-v Missed 2 shows a server-side cancel cannot silence the pilot's own sound (G5).
11. stato-del-codice §4.2/§8 and the comment `gametest/ExoframeGameTests.java:136` (mock's tick counter not advanced by the server) vs test §1.2 + test-v C1 (the level runs `ServerPlayer.tick()` and `++tickCount`; only `doTick()`/`Player.tick()` never runs). test wins.
12. stato-del-codice §7.5 (a block-entity type on Fabric will log "No data fixer registered") vs blocco §1.5 + blocco-v C5 (no log on any variant); also repo §1: the existing entity line is ERROR, not WARN. blocco wins.
13. stato-del-codice §3.1 (`addOrUpdateTransientModifier` usable for idempotence) vs attributi §1.2 + attributi-v C1 (reference comparison: still dirties). attributi wins.
14. blocco §8.5 (fuel inlet with `getMaxStackSize()` 1) vs blocco-v Missed 1 (hoppers ignore the container's max stack size). Verification wins; limits go in `canPlaceItem`/`canPlaceItemThroughFace`.
15. test §1.6(c) (fall damage reaches a player through `doCheckFallDamage`) vs test-v C7 (passengers get `causeFallDamage` from the vehicle; Forge/NeoForge `LivingFallEvent` first). Verification wins.
16. danno §5 (smallest differing n = 7736) vs danno-v C12 (n = 6145 with the test shares). Verification wins; conclusion (delegate `route` to the milli overload) unchanged.
17. dati §8 (`validate()` enforces count 1 for unstackable items) vs dati-v Missed 5 (the prototype only checks 1..64). Verification wins.
18. repo §5.4 (`StackTagsTest.java:47,52` use `load(new CompoundTag(), …)` as an empty registry) vs repo-v c9 (they only trigger the constructor's `StackTags.forgetOutcomes()`). Verification wins.
19. attributi §4 (1.21.1 `getBarWidth` clamps) vs attributi-v C11 (NeoForge's does not). Verification wins; the override must clamp.
