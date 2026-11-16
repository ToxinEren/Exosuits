### Spec Compliance

- ✅ Spec compliant: the diff does what the brief's "Produces" asks, line by line.
  - `frame/SuitRestrictions.java:90-92`: `register()` = `InteractionEvent.RIGHT_CLICK_BLOCK.register(SuitRestrictions::onUseBlock)`.
  - `SuitRestrictions.java:98-106`: `pass()` without a suit (`SuitSize.wearsSuit`) or on a block that is not a `BedBlock`. Otherwise `interruptFalse()` on both sides and for both hands. The `displayClientMessage(translatable(LEAVE_FRAME_FIRST), true)` call happens only when `!isClientSide()` and the hand is `MAIN_HAND`.
  - `RedstoneExoframes.java:50`: registration comes right after `SuitTick.register()`.
  - `EnergyGameTests.java:169` and `:197-220`: the test body is the brief's, appended at the end of `generate()`.
  - The class Javadoc covers every point the brief lists.
  - No new key and no `compat.md` line, which is correct because the task adds no new seam.
- Checks I ran on unchanged code:
  - `SuitSize.wearsSuit(Player)` is public and reads the torso placeholder in the CHEST slot, so it works on both sides (`frame/SuitSize.java:59-64`).
  - `SuitTick.LEAVE_FRAME_FIRST` is public (`frame/SuitTick.java:84`).
  - No other `RIGHT_CLICK_BLOCK` listener exists in `src/` (git grep at 786c77e). The RED run therefore really means "no listener".
  - `RedstoneExoframes.init()` runs on both physical sides: Fabric `main` entrypoint (`RedstoneExoframesFabric.java:11`), Forge and NeoForge mod constructors.
- ⚠️ Cannot verify from the repository: how the bed behaves on a real client on the four variants. Expected: refused with no sleep animation, and on Forge/NeoForge the action-bar message once per click. The mock player has no client, and the brief (Step 5) puts this on the 3c-2 PC checklist. I checked the packet flow statically (see T15-F1).

### Strengths

- The listener is minimal and readable (`SuitRestrictions.java:98-106`). The side check uses `!player.level().isClientSide()`, the same form `SlotGuard` uses. The method checks the cheap suit test first.
- The test goes through `RIGHT_CLICK_BLOCK.invoker()`, so it also proves the registration. It covers both hands, the message count staying at 1 after the off hand, a non-bed block, and a player without a suit.
- The RED/GREEN evidence is precise: the expected failure message, then 135 on all four variants, matching the plan's count table. The build log is reported clean.
- The implementer's Fabric concern is accurate and was researched in the real library sources. I confirmed it independently (T15-F1).

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

- **T15-F1 (plan-mandated): on both Fabric variants the bed refusal is silent.** The "leave the exoframe first" message never reaches a Fabric player.
  - **Where:** `SuitRestrictions.java:105` returns `interruptFalse()` on the client too. Architectury Fabric hands the event to Fabric as `UseBlockCallback` with `EventResult.asMinecraft()`, which maps a present `false` to `FAIL`. Source: architectury-fabric 9.2.14 `EventHandlerImpl.java:84`, 13.0.11 `:82`, and `EventResult.java:162-166`.
  - **Why the message is lost:** Fabric API's client mixin `ClientPlayerInteractionManagerMixin.interactBlock` (fabric-events-interaction-v0 0.6.6 lines 98-107, 0.7.14 lines 106-115) sends the use-block packet only when the result is `SUCCESS`, and returns any other non-`PASS` result without sending. The server never sees the click, so line 103 never runs on 1.20.1-fabric or 1.21.1-fabric.
  - **Forge and NeoForge are fine:** in the patched `MultiPlayerGameMode.useItemOn` (javap of forge-1.20.1-47.4.29 and neoforge-21.1.266), the prediction lambda calls `performUseItemOn`, where the `RightClickBlock` cancellation is returned, and then always builds `ServerboundUseItemOnPacket`. On those two variants the server fires the event again and the message arrives.
  - **Why it matters:** the bed is still refused on all four variants. But design §12.3 (line 469: "in tutti e due i casi il messaggio ... nella barra delle azioni") and §8 item 34 require the message for beds. The brief's rule ("annulla su tutti e due i lati, messaggio solo dal server") defeats that on half the variants. GameTest cannot catch it.
  - **Options for the coordinator:**
    - (a) Let the click reach the server from a Fabric client. Either do not cancel on the client (on the client `BedBlock` only returns CONSUME, and sleeping is server-driven), or return a client result Fabric forwards (`interruptTrue()` = `SUCCESS`, which costs an arm swing). Either way this departs from "annulla sui due lati".
    - (b) Show the message client-side only where the loader drops the packet. That needs a loader seam in `compat/` plus a `docs/compat.md` line.
    - (c) Accept a silent refusal on Fabric and record it in the design and on the 3c-2 PC checklist.

#### Minor (Nice to Have)

- **T15-F2 (plan-mandated): the refusal covers more than sleeping.**
  - **Where:** `SuitRestrictions.java:99` cancels every right-click on a `BedBlock`. That includes sneak-click with an item in either hand. In that case vanilla never calls the bed's `use`/`useWithoutItem` and only runs the item: placing a block against the bed, a bucket, a spawn egg.
  - **Effect:** a player in a suit cannot do those things, and on Forge/NeoForge sees "Leave the exoframe first" for something that is not sleeping.
  - **Fix if wanted:** return `pass()` when `player.isSecondaryUseActive()` and either hand holds an item, which is vanilla's own bypass condition. Add one more case to the test.
- **T15-F3: inaccurate Javadoc.**
  - **Where:** `SuitRestrictions.java:96` says "Pubblico per i test."
  - **What is wrong:** no test calls `onUseBlock` directly. `EnergyGameTests` goes through the event invoker, and git grep finds no direct caller. The method is public because the plan lists it under "Metodi pubblici chiamati da altri package".
  - **Fix:** drop the sentence or reword it.

### Assessment

**Task quality:** Approved

**Reasoning:** The implementation matches the brief exactly, is small and well tested through the real event, and its evidence covers all four variants. The one significant defect, the silent refusal on Fabric clients, is mandated by the brief and design rule and needs a coordinator ruling, not an implementer fix.
