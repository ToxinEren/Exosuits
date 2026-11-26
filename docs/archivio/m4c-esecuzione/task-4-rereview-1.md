### Finding Verdicts
- **Important 1: `vignetta` check asserts no colour/translucency (opaque vignette passes)** — ADDRESSED. tools/client/m4c_shots.sh:210-220 and 230-234: per corner, `colore` of 0A0C0E in `pieno` and `senza-vignetta`; `tint_good` fails unless t1 >= 0 and t1 == t2 (a failed measure gives -1/"misura-non-riuscita" and fails), and it is ANDed into `good` (l.224). Equality, not "= 0", so the 400 bar-frame pixels bottom-left do not false-KO. Constant VIGNETTE_TINT=0A0C0E added; `want` (l.230-231), header comment and README `vignetta` row all describe it. Report shows both real folders ok (0/0 0/0 400/400 0/0 on neo and forge, other 16 lines unchanged) and a negative proof on an opaque doctored copy giving KO (7388/0 7388/0 7684/400 7596/0).
- **R17: SuitHud Javadoc and compat.md say saving indicator is under the vignette on Fabric and not drawn on Forge 1.20.1** — ADDRESSED. SuitHud.java Javadoc of `drawVignette` (hunk at ~l.182-186) and docs/compat.md SuitHud entry both state it (with the ForgeGui reason). The extra parenthesis in the group sentence ("l'indicatore del salvataggio solo con Fabric: con Forge 1.20.1 non si disegna") is consistent with it and removes a contradiction in the same entry; acceptable.

### New Breakage in the Fix Diff
None. `local` list updated for the new variables; `val`/`take_measure` semantics (-1 / misura-non-riuscita, verdict forces KO) verified in measure_lib.sh:22-58. README "Quanto dura" changed to about forty seconds, backed by the measured 39/40 s in the report.

### Out-of-Scope Observations
None.

### Verdict
**Fix round:** All findings addressed, no new Critical/Important breakage
