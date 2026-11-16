# Task 22 review (a102066..a5703f3)

### Spec Compliance

- ✅ Spec compliant. Every item of the brief has its hunk:
  - `client/EnergyLine.java:204-250`: package-private record with the five components; `of` empty without a cell (`:213-216`); `percent = (int) (cell.stored() * 100L / cell.capacity())` (`:217`); slot loop in `PieceSlot.values()` order with a strict comparison (`:219-224`); `isWorse` (`:237-250`) excludes non-CRITICAL/BROKEN, BROKEN beats CRITICAL, then the exact long cross-product from the brief. Only `core/` imports.
  - `client/EnergyHud.java`: `final`; keys, four colours, `MARGIN = 4`, `BOTTOM = 12` (`:118-131`); the four early returns (`:143-149`); NO_CELL fallback (`:150-151`); colour only from `clientIndicators`, white when absent (`:152`, `:170-176`); `drawString(font, text, guiWidth - width - MARGIN, guiHeight - BOTTOM, color, true)` (`:154-155`); ENERGY_WORST nests the energy component, piece key and condition key (`:158-168`). The Javadoc names §2/§12.13, the subtitles (accepted) and M4 (`:108-115`).
  - `compat/client/ClientRenderHooks.java:304-308`: `addHudRenderer(Consumer<GuiGraphics>)` with the Javadoc the brief asks for.
  - `compat/impl/client/ClientRenderHooksImpl.java:368-374`: `requireNonNull`, one directive-free `RENDER_HUD.register((graphics, partial) -> renderer.accept(graphics))` line, and the 9.2.14 / 13.0.11 comment next to it.
  - `client/RedstoneExoframesClient.java:267`: registered right after `addPlayerLayer`.
  - Lang (`en_us.json:401-403`, `it_it.json:428-430`): the three keys, verbatim texts, after `condition.redstone_exoframes.broken` and before `tooltip.redstone_exoframes.exoframe.place`, same order in both files.
  - `docs/compat.md`: "A cosa serve", "Differenze chiuse", "Condizione", "Uso" (`:38-41`), "Avvio del client" (`:46`, plus a new bullet `:52`), and the API table row moved out of the deferred ones into its own row with the exact text from the brief (`:75-76`).
  - `EnergyLineTest`: the six cases with the brief's names and values, including the `1_000_000 / 999_999` long-product case.
- ⚠️ Cannot verify: whether Architectury fires `RENDER_HUD` in a real client on 1.20.1-forge and 1.21.1-fabric. The four builds compile, but Step 5 tries only 1.20.1-fabric and 1.21.1-neoforge (as the brief asks), and the repository cannot answer it without running those two clients.

Checks I ran (one per named risk):
- Division by zero in `percent`: `core/energy/EnergyCell.java:10-11` rejects `capacity <= 0`, so it cannot happen.
- Per-frame cost of `clientView`/`clientIndicators`: `FrameStateAccessImpl.java:130-137` → `FrameStateClientCache.view/indicators` are `ConcurrentHashMap` lookups; `FrameState.pieces()` (`FrameState.java:73-75`) is an unmodifiable wrapper, with no copy and no decoding per frame.
- The implementer's hotbar concern: I looked at `riga-energia-critico.png` and `riga-energia-crop.png` (1.20.1-fabric, 960x540, GUI width 480) and computed the vanilla auto GUI scale (see I-1).
- Duplicate key building: grep for `"piece." +` in `src/main/java` (see M-1).

### Strengths

- `isWorse` (`EnergyLine.java:237-250`) is a small, exact transcription of the ranking rule: integer cross-multiplication in `long`, strict `<` so the first slot wins ties, BROKEN-over-CRITICAL as a separate branch. The tests target each branch: absolute versus relative durability, ties in both conditions, and none critical.
- `EnergyHud.render` keeps every decision in the order the brief lists. The colour comes only from the server indicators through an exhaustive `switch` over `EnergyState`, so a new state would fail the compile instead of falling through silently.
- The Architectury signature difference is closed with one line and an explanatory comment, as the brief asks, and the docs table now records it as closed instead of deferred.
- Step 5 evidence is concrete: the line was seen on both Architectury versions, and the F1 shots were checked by pixel count.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

- **I-1 (plan-mandated): at the vanilla default GUI scale the line draws over the hotbar.** `EnergyHud.java:154-155` puts the text at `x = guiWidth - width - 4`, `y = guiHeight - 12`, as the brief (line 13) and the plan header ("Riga dell'energia", line 92) require. That y band (`h-12..h-3`) lies inside the hotbar band (`h-22..h`). The hotbar spans `guiWidth/2 ± 91`, so the line is clear of it only when it is at most `guiWidth/2 - 95` px wide.
  - Auto GUI scale gives GUI width 480 at 1920x1080 (scale 4) and about 427 at 2560x1440 and 3840x2160 (scale 6 and 9). That leaves about 145 px or about 119 px of free space.
  - The screenshot `riga-energia-critico.png` shows `Energy 9996 / 10000 RE (99%) · Helmet critical` starting at about x 241 and covering the right half of the hotbar. In `riga-energia-crop.png` even the plain English line already touches the hotbar's right edge at 480.
  - At 427 the plain line covers the last slot whenever the suit is worn. The Italian line with a piece (`… · Braccio sinistro in stato critico`, about 330 px) covers almost the whole hotbar. A left-handed player's off-hand slot (right of the hotbar) is covered too.
  - The hotbar is core vanilla HUD and this happens on the most common monitors at default settings. The design (§12.13) only asks for "una riga di testo sullo schermo" and accepts covering the subtitles, not the hotbar.
  - Fix, for the coordinator to rule on: keep the bottom-right anchor when `x >= guiWidth / 2 + 91 + MARGIN` (plus about 29 px when the off-hand slot is on the right). Otherwise raise the line above the hotbar and the status rows, or put the worst piece on a second line. Or move the line to a free corner.

#### Minor (Nice to Have)

- **M-1: the piece-name key format is built in two places.** `EnergyHud.java:164-165` repeats the expression of the private `SuitDamage.pieceName` (`frame/SuitDamage.java:183-185`): `"piece." + MOD_ID + "." + slot.name().toLowerCase(Locale.ROOT)`. If the key format changes, both places must change. The brief spells the expression out inline, so this is optional. Fix: make `SuitDamage.pieceName` (or a shared helper in `frame/`) public and call it from `EnergyHud`.
- **M-2: three branches of `EnergyHud` were never seen in a real client.** The yellow (LOW) and red (EMPTY) colours, the NO_CELL text and the BROKEN wording were not tried. Step 5 covers only NORMAL with CRITICAL, as the brief asks. They are worth a look once Task 23's `debug energy` command can set the charge.

### Assessment

**Task quality:** Approved

**Reasoning:** The implementation matches the brief line by line and is clean and well tested. The one Important item is the overlap with the hotbar at default GUI scales: the brief and plan fix the position, so it needs a coordinator ruling, not an implementer fix.
