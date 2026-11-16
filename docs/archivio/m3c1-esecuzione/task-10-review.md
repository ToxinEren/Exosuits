### Spec Compliance

- ✅ Spec compliant. Checked against brief Task 10, plan lines 13-95 and design §12.2 (spec lines 415-437):
  - `frame/SuitDamage.java:22` `public static void onArmorHurt(Player, DamageSource, float)`, empty; the class is `public final` with a private constructor, and its Javadoc says it does nothing yet.
  - `frame/SuitMovement.java:21,29,41`: `affectedByFluids`, `pushedByFluid` and `sprintAllowed` return `vanilla`; `:34` `afterUpdateSwimming` is empty. The class is `public final` and its Javadoc says each method returns the game's value. It takes `Player`, never `LocalPlayer`, so this common code has no client reference (rule 7 holds; checked `tools/check_sources.sh:94-120`).
  - `mixin/PlayerSuitMixin.java:24-45`: `@Mixin(Player.class)`, `abstract class` and package-private like `PlayerSize121Mixin`. Its four `@Inject` match the table exactly in descriptor, `@At`, `cancellable` and handler name: `exoframes$wearPieces` HEAD, `exoframes$fluidMovement` and `exoframes$fluidPush` RETURN cancellable, `exoframes$noSwimming` TAIL. Each handler makes one call, with `(Player) (Object) this` and `cir.getReturnValueZ()`.
  - `mixin/client/LocalPlayerSprintMixin.java:20-25`: `@Mixin(LocalPlayer.class)`, `hasEnoughFoodToStartSprinting()Z` RETURN cancellable, and `exoframes$sprintGate` calls `SuitMovement.sprintAllowed((LocalPlayer) (Object) this, cir.getReturnValueZ())`, as the brief writes it.
  - The two `redstone_exoframes.mixins.json`: `mixins` holds the version's size mixin plus `PlayerSuitMixin`, and `client` holds the four classes in the brief's order.
  - `tools/check_jars.sh:144-168` (`SPRINT`, `CLIENT`, `TARGETS`, `MOJANG`) carries the brief's values exactly. The common-section check at `:182-184` prints the same text as the brief's f-string; it is split over two lines to stay within 120 columns. The (e2) comment at `:115-127` is rewritten as the brief asks.
  - `docs/compat.md`:
    - `:173-177`: the new point **Usura, acqua e scatto**, with the two classes, five hooks (descriptor and handler), the names per loader, no directive or exclusion, handlers that leave the game's value, and no handler named after a target.
    - **Configurazione**, **Nessun refmap** and **Verifica** (`:178-181`) are updated.
    - "Cuciture client" (`:119`) now says "i quattro mixin client".
    - The table row at `:220` matches the brief's text word for word.
  - Commit message as required.
  - No handler and no called method has a target's name. Named risk: a MOJANG marker could fire on a handler or callee in a Fabric or Forge jar. Checked: `afterUpdateSwimming` has a capital U and a callee's name never sits next to "(" in the constant pool, so only the annotation strings match. `OK jars` in the report agrees.
- ⚠️ Cannot verify from diff: none. Runtime evidence (Steps 2-8: the build, RED and GREEN `check_jars`, the negative jar test, 114 tests ×4, the real client ×4 and the dedicated server ×4) is taken as reported, per the contract. The report is specific and consistent with the brief's expected outputs.

### Strengths

- Exact transcription of a fragile, failure-at-boot surface. The descriptors, `@At` points, `cancellable` flags and handler names in `PlayerSuitMixin.java:26-44` and `LocalPlayerSprintMixin.java:22-25` all match the brief's table and design §12.2.
- `SuitMovement` takes `Player` everywhere, so the sprint gate's common callee drags no client class onto a dedicated server.
- The Javadoc follows the house pattern: the reason for a mixin, the targets verified with javap, the names per loader, and ASCII apostrophes only. No non-ASCII characters and no line over 120 columns in the four new Java files (checked).
- `check_jars.sh` keeps the error text identical while respecting the line limit. The negative test also showed the missing-class line, so the check catches both faults.
- The report is candid about what each runtime proof covers. Step 8's boot alone does not prove `PlayerSuitMixin` applies with a real player; `client_shot.sh` and the GameTest servers do.

### Issues

#### Critical (Must Fix)
None.

#### Important (Should Fix)
None.

#### Minor (Nice to Have)
- **T10-F1** `build.gradle.kts:106`: the comment over the mixin wiring still summarises the mixins as "la grandezza reale su tutti e due i lati, il disegno solo sul client". After this task it leaves out `PlayerSuitMixin` (wear and water, both sides) and `LocalPlayerSprintMixin` (sprint, client). The stale summary misleads a reader of the build file about what runs on a dedicated server. Fix: widen the parenthetical, for example "(la grandezza reale, l'usura e l'acqua su tutti e due i lati, il disegno e lo scatto solo sul client: ...)". The file is outside the brief's Files list, so the coordinator may fold this into a later task that touches `build.gradle.kts`.

### Assessment

**Task quality:** Approved

**Reasoning:** The two mixins, both configurations, the jar check and the docs match the brief and design §12.2 value for value, and the handlers return the game's value as Task 10 requires. The only finding is a stale summary comment in a file the brief did not list.
