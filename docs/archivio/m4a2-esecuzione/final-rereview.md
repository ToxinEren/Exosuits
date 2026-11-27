### Finding Verdicts
1. **Design section 15 "Riporti della parte 4a-2"** — ADDRESSED. Design.md diff hunk at old line 649 (new lines 86-106): geometry (72/63, (height-232)/2+232, 358/359-375/376, Auto scale), why harm is low, why 4b first and why no sound fix now (mixin; ClientSystemMessageEvent from the Fabric mixin only), smallest 4b fix (StationView field with serial or small packet, ~3 s, owner chooses, measured client check, not ClientSystemMessageEvent), nine deferred minors one line each, plan-time check line. Italian; no process IDs, no AI vendor/model names (Architectury is a library).
2. **Design 5.14 lists `reinforced`** — ADDRESSED. Table row for the frame registry (new line ~63 of the hunk) now lists `reinforced` as a schema-2 key a milestone-3 jar already reads.
3. **docs/compat.md covered action bar** — ADDRESSED. Clause added in the "I pulsanti" bullet (known limit, 4b, design 15) and in the lock bullet.
4. **StationMenu.java:278 Javadoc** — ADDRESSED. Now "vero per ogni scelta (anche se la scelta e' gia' quella) e per un telaio cambiato".
5. **Comment drift** — ADDRESSED. GameTests.messagesTo (mock and operatorPlayer); SuitDamage:36 and SuitTick:53 (`mockServerPlayer`); StationView.withSelection (client prediction via `StationMenu.select`); StationMenuGameTests header reflowed.

Checks run: grep of `withSelection` in src/main: the only call is StationMenu.java:264 inside `select`, which does it only when `level == null` (client); the server calls `select` from StationActions.java:74 and that branch skips it, so the new text is true. Over-120 and non-ASCII (other than the section sign) scan of the six touched .java files: none of the long lines (GameTests 101/118/172, StationMenuGameTests 853, StationMenu 34) is in the diff. Report names compileJava on both variants and check_sources, but shows no output (unverified claim, not re-run).

### New Breakage in the Fix Diff
None. Minor style nit only: docs/compat.md lock bullet has two adjacent parentheticals "(con la schermata aperta ... design §15) (`commands...`" which reads awkwardly.

### Out-of-Scope Observations
None.

### Verdict
**Fix round:** All findings addressed, no new Critical/Important breakage.
