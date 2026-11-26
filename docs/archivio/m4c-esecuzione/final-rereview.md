### Finding Verdicts

- **F1 (prep. E: "Sottotitoli: No", "Mano principale: Destra")** — ADDRESSED. m4c-hud.md:90 now checks both; labels and paths match m4c-hud.md:132-134 (Aspetto..., Mano principale) and :133 (Audio..., "Sottotitoli: No"/"Show Subtitles: OFF"); consistent with step 3 and step 15 starting state. (Minor wording: "Esc →" in a prep that precedes world creation; the same form is used elsewhere in the repo, so not blocking.)
- **F2 (m3c note "Passi 10 e 12", hint in both languages)** — ADDRESSED. m3c-giocarci.md:12 (diff line ~145) quotes IT and EN exactly as lang entries (it_it.json:52, en_us.json:52, with V, R, H substituted).
- **F3 (ONE_DIGIT_SPARE 12 measured, TWO 18 deduced, assertions)** — ADDRESSED. HudLayoutTest.java:36-43 Javadoc as asked; assertions 53/47 and 47/41 follow from `spareTextX = 65 - w` (HudLayout.java:148) and `cellIconX = spareTextX - 1 - 5` (CELL_ICON_W = 5). 7 `@Test` in the file; the report states XML tests=7 failures=0 (summary only, no raw output pasted; I did not rerun).
- **F4 (no-cell design text)** — ADDRESSED. Design :274 matches code: SuitHud.java:233-235 draws `no_cell` at (NO_CELL_X=2, NO_CELL_Y=20), colour `HudModel.GREY`, with shadow, instead of the percentage, on the bar row (bar only drawn when hasCell, SuitHud:241); text "Senza cella"/"No cell" matches lang. :376 now "Senza cella". :633 states it was followed, asked to the owner, no objection, cost one text and one constant. Remaining "Nessuna cella" hits (274, 633) explain why it does not fit.
- **F5 (m4c-hud.md:19 "nella parte dell'energia")** — ADDRESSED. Wording with the left-hand note present.
- **F6 (README.md:28)** — ADDRESSED. Lists object textures, icon, charger and station block faces, HUD vignette; consistent with tools/README.md "Texture placeholder".
- **F7 (CELL_CHARGE Javadoc)** — ADDRESSED. HudModel.java cites `StationLayout.CELL_RED` (StationLayout.java:80, same 0xFFC81E1E) and the 4d carry-over; value unchanged.
- **F8 (design §14)** — ADDRESSED. All brief items present and accurate (4d pins incl. 122x10+52+510, 42x16+50+482, 46x14+50+482, one red constant; extra tests; PC-run items with DEC-48; four disproved plan facts; questions 8-10 / decision 1). No personal data, no model or vendor names (grep clean).

### New Breakage in the Fix Diff

None.

### Out-of-Scope Observations

None.

### Verdict

**Fix wave:** All findings addressed, no new Critical/Important breakage.
