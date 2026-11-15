# Challenge of the architecture examination (A1-A11)

Re-derived from the code (`J/` = `src/main/java/com/toxineren/exoframes/`), the dossiers and javap of the
1.21.1 merged jar (`PlayerList`, `MinecraftServer`, `HopperBlockEntity`, extracted under `esame/ch/`).

## A1 — holds (blocker as written); a smaller fix exists
- Confirmed: `FrameStateAccessImpl.update` (J/compat/impl/FrameStateAccessImpl.java:66-78) calls
  `FrameStateSavedData.updateWorn` → `getOrCreateWorn` (FrameStateSavedData.java:317-345): a record is created if
  absent, marked structural, synced. 7.2 names no "is wearing" precondition, so a late/forged `cell_swap` creates a worn
  frame from nothing; `SlotGuard.tick` (SlotGuard.java:76-123) then dresses the player. Read-only: `update` returns
  before the consumer, `void`, so inventory edits done outside the consumer are unguarded.
- Smaller fix than a new seam method: in read-only mode no entry is ever read (FrameStateSavedData.java:95, `load`
  returns before reading), so `Compat.frameState().get(player).isPresent()` is false both when no frame is worn and
  when the registry is read-only. 7.2 should say: on the server thread, refuse unless `get(player)` is present, alive,
  not spectator, `containerMenu == inventoryMenu`; then `update(...)` (same tick, same thread: no window between the
  check and the write); write the old cell into the inventory slot only after `update` returns, and only if the slot
  still holds the chosen cell. `updateWorn(...) -> boolean` is acceptable but not required. Cooldown on
  `server.getTickCount()`: agreed (`player.tickCount` restarts on every new `ServerPlayer`).

## A2 — holds; a cheaper fix than a new seam
- Confirmed: the respawn lambda needs a directive (FrameStateAccessImpl.java:298-302); `tools/check_sources.sh` rules 2
  and 6 forbid directives outside `compat/impl`, `platform/`, `compat/Ids.java`. 4.1 routes "at login and at respawn"
  through the §11.1 point in `frame/`: that file would need the directive.
- Better option: do not listen to join/respawn at all. Key the `SuitTick` record (3.2) by the `ServerPlayer` object
  (weak keys, like `SuitSize.LAST_SEEN`, SuitSize.java:49) or store the player reference in it: a new object (login,
  respawn, End return — the only cases where modifiers are lost, test.md §5) has no record, so the first tick applies
  attributes. No seam, no directive, at most one tick late. If an explicit hook is preferred, the examiner's seam is
  right, and `compat/FrameAttributes.java` Javadoc already names that very listener as the apply point.
  Either way 4.1 must drop "through the §11.1 point" for join/respawn.

## A3 — holds; proposed change is right
- `FrameRules.evaluate` passes the constant `false` for `canSwim` (FrameRules.java:96-107, 5th field of
  `FrameEffects`); `SuitSummary` (J/compat/SuitSummary.java:22) has only pieces; the cache is filled only by the owner
  packet. So "can it swim" has no source for remote players and is constant for everyone. The experiment ran with
  `SuitSize.wearsSuit` on both sides (ricognizione §1(a) "Logica", "nessun pacchetto"), and the hooks run 5-6 times
  per player per tick (2-3 + 2 + 1, same section): a placeholder read is the right cost. Change 5.1 to
  `SuitSize.wearsSuit(player)` on both sides; keep `canSwim` in the packet only because §5.3 binds it.

## A4 — partly: the list edits hold, the "substring trap" does not
- Holds: `check_jars.sh` requires `common == [size_mixin]` and an exact `CLIENT` list, and on NeoForge each listed
  class must contain a `MOJANG` marker; design §3 and §7 p.14/17 enumerate the mixins. 5.1 and 5.3 add two mixins
  without naming these edits.
- Refuted: the Mojang test scans only the classes listed in the mixin json, never `frame/`, and the markers that would
  be added end in `(` (as the existing `getDimensions(` etc.). In a class file a method name is a separate CONSTANT_Utf8
  entry followed by the next entry's tag byte (1..20), never `(` (0x28); only the annotation string
  `hurtArmor(Lnet/...;F)V` contains `hurtArmor(`, and Loom rewrites it. A handler called `exoframes$hurtArmor` or a call
  to `SuitMovement.isAffectedByFluids` cannot match. The rule "names never contain a target name" is unnecessary; the
  real rule is "every new MOJANG marker ends with `(`" (or is a full descriptor).
- Keep the rest of the proposal: one `mixin/PlayerMixin` (three movement hooks + `hurtArmor`), one client sprint
  mixin, both json files, `TARGETS` entries, design §3/§7 and `docs/compat.md` in the same task.

## A5 — partly: undeclared override holds; the "brings back LIVING_HURT's faults" part is mostly wrong
- Holds: design §5.7 "se il danno aggira l'armatura si salta"; `fall`/`stalagmite` are in `bypasses_armor`
  (danno.md §4, confirmed by danno-verifica C10). 2.2 wears legs on them without declaring the override, and §12 lists
  both "bypassing damage does not wear" and "fall with leg wear". The game document does say "caduta → gambe"
  (claude-doc…:39), and `DamageRouter` already routes `FALL` to the legs (DamageRouter.java:220): a legitimate
  override, it only needs to be declared.
- Mostly refuted: the over-count that rejected LIVING_HURT is per-tick sources (lava, fire, cactus: danno.md §1); a fall
  is one hit per landing. Creative never reaches it for falls (`Player.causeFallDamage` returns false when `mayfly`,
  danno.md §4) and 2.2 already excludes creative/spectator/`invulnerable` (covers the pearl case). Spawn protection
  is before the event on all four. What remains: a landing inside the 10-tick window of another hit is worn in full
  (vanilla would apply `amount - lastHurt`); minor.
- Fix: 2.2 declares itself an override of §5.7 citing the game document; guards `ServerPlayer`, `isAlive()` (as 2.1
  already implies "server side"); accept the i-frame difference in writing; §12 line "bypassing damage does not wear,
  except a cancelled fall". Severity minor, not major.

## A6 — holds; proposed change is right
- 4.2 writes 0.6F on every non-wearing local player each tick ("compare-then-set"): any other value is overwritten.
  ricognizione §1(b) itself says "solo al cambio di stato … per non pestare altre mod"; attributi.md §8 item 6 says
  "remember that the mod raised it". `setMaxUpStep` exists only on 1.20.1 (attributi.md: no field or setter on
  1.21.1), so the call needs a directive → `compat/impl` (rules 2/6); `client/` cannot reach `compat.impl` (rule 4).
- `FrameAttributes.localStep(Player, boolean)` in `FrameAttributesImpl`, driven from `SuitSize.tick` (already on
  `PLAYER_PRE`, RedstoneExoframes.java:43) for `isLocalPlayer()`; raise to 1.0 while wearing, lower to 0.6 only when the
  mod raised it (weak per-object flag). Smallest correct fix.

## A7 — partly: the /save-off half holds; the "player files" half depends on a reading
- Holds: javap 1.21.1 `PlayerList.remove` @14 `invokevirtual save(ServerPlayer)` with no save-off test;
  `MinecraftServer.saveEverything` @9 calls `PlayerList.saveAll()` before `saveAllChunks` (which alone checks
  `noSave`). Honouring `/save-off` at quit writes the quitting player's file without the registry: exactly the split
  the comment at FrameStateSavedData.java:44-60 says the quit save prevents.
- Not shown: 11.6 says "the player files" (plural). Read as "all connected players' files" (today's `saveAll`,
  FrameStateSavedData.java:846) plus only the registry file, there is no A/B split; the examiner's scenario needs the
  narrower reading "the quitting player". The per-player dirty set is an optimisation, not a fix.
- Fix: 11.6 = "at quit with a structural change unsaved: `saveAll()` of all players (unchanged) and then only the
  registry file (its own `saveFile`), not `overworld().getDataStorage().save()`; never honour `/save-off` for this
  write, and say why in the class comment". After 11.2a.

## A8 — holds; proposed change is right (size unmeasured)
- test.md §3: "Per-tick values are jittery (0, 1 or 2 client packets land in one server tick), so classify over a
  short window"; test.md §6 item 6 the same. With one-tick classification each 0-packet tick of a walking player
  becomes IDLE while its 2-packet partner is still one WALK tick: drain is under-counted (frequency not measured,
  test.md "not measured"). Cross-dimension sub-8-block move: real but one tick (≈0.3 RE), trivial.
- Window of 5 ticks, reset on level change / >8 block step / passenger / new player object; threshold as a constant
  in a pure `core/energy` class named by a test (rule 1c). Design §6 lists "soglie di movimento" as builder's
  values: a constant satisfies it; drop `energy.walkThreshold` from 6.3 (minor part).

## A9 — partly: the trap is real; the text does not mandate the bug
- Confirmed by javap and blocco-verifica C10: `tryMoveInItem` on a non-empty slot grows the stack `getItem` returned
  in place and never calls `setItem`. A slot-1 `getItem` that builds a stack from a count deletes one dust per
  transfer.
- But 8.2 calls slot 1 "a normal stack, up to 64"; "primitives" is about the NBT only. The risk comes from "the rule
  lives in core/ … the block entity only holds the state": a core rule working on an `int` invites a count field.
- Fix: one sentence in 8.2/8.4: "slot 1 is a live `ItemStack` field; the core rule reads and writes its count through
  that field (never a parallel int); NBT stores the count". Slot 0 as a fresh view is safe: on a failed extraction
  1.21.1 `tryTakeInItemFromSlot` @68-85 does `setCount(count)` then `setItem(slot, stack)` when count is 1, 1.20.1
  restores a copy with `setItem`. Keep the hopper GameTest (1 + 3 = 4).

## A10 — holds; proposed split is right
- `load(new CompoundTag(), FrameStateCodec::read)` is the empty-registry helper at FrameStateSavedDataTest.java:125,
  RegistryFileTest.java:50, StackTagsTest.java:47,52 (re-grepped). On 1.20.1/1.21.1 a file root without `data` reaches
  `load` as an empty compound (the game passes `getCompound("data")`), so the "no sections = damaged" rule must live in
  `load` and turns those helpers into read-only registries.
- Add to 11.2b: decide on key presence, not emptiness; `write()` always puts `schema` (FrameStateSavedData.java:958) and
  schema 1 is "no `schema`, a `frames` section" (comment :91-93), so "damaged" = no `schema` and none of `frames`,
  `worn`, `placed` present.

## A11 — holds; proposed change is right
- repo.md §4 table: `SlotGuard.tick` writes no registry, no sync, no log; listing it in 11.1 would run apply and the log
  line on each repair. The binding §11 carry-over itself names `SlotGuard` among the sites; 11.1 should say why it is
  left out (its writes are views of the registry, not changes of it).
- Splitting the refactor (enter/leaveFrame/debug wear/remove → `WornState.changed`: `refreshDimensions` + log line)
  from the feature additions is right; order constraints 1-5 of repo.md §4 must be copied into the task. With A2's
  cheaper fix, join/respawn need not call the notifier at all (placeholders come from `SlotGuard`, size from
  `SuitSize.tick`, attributes from the tick).

## Not reported by the examiner (architecture lens)

1. **Cell capacity: per-record in the frame, per-config for items; 6.x makes the config change real** (major).
   `FrameStateCodec` stores the frame cell's own `CAPACITY` (FrameStateCodec.java:67,136-139); `CellItem.toCell`
   uses `config().energy.cellCapacity` and clamps the charge (CellItem.java:24-28); `fromCell` drops the capacity.
   Today only defaults exist; with a config file, lowering `cellCapacity` deletes energy when a frame cell leaves by
   swap (7.2), manual removal or the charger (8.x), and 7.2 compares charges across different capacities; raising it
   leaves frame cells at the old capacity forever. Decide in 6.x/7.x: capacity is a server constant; a frame cell is
   normalised to the config capacity on load (clamp, logged once) or the stack carries its capacity; 7.2 compares
   stored energy; 5.5 syncs the same value.
2. **GameTests read a persistent config file** (minor). With 6.2 the test server's run directory gets
   `config/redstone_exoframes.json` written once and read on every later run (`tools/run_gametests.sh` does not clean
   it); a hand edit changes test results silently. Pin: GameTest registration (or the script) starts from
   `ExoframesConfig.defaults()`; tests that need other values set them and restore them.
3. **`FrameAttributes` contract text contradicts 4.1** (minor). J/compat/FrameAttributes.java says `apply` "first
   removes a present modifier" and "each call resends the attributes", "four modifiers"; 4.1 makes it idempotent and
   4.2 adds a fifth on 1.21.1. The Javadoc and `docs/compat.md` must change in the same task, or reviewers will hold
   the code to the old text.
