### Finding Verdicts
- **Important 1: barra-esaurita passes with the warning missing (grey >= TEXT_MIN only), README row stale** — ADDRESSED. m4c_shots.sh adds `WARNING_PERCENT_RIGHT=42x16+50+482` (line 77) and `&& is_box "$m3" "$WARNING_PERCENT_RIGHT"` in the barra-esaurita condition (diff hunk at ~line 102-105); `is_box` (m4c_shots.sh:317) compares the measured `riquadro` exactly. A missing warning gives box 22x14+70+482 (report negative proof: KO, FAIL, 15 other checks ok), a missing percentage gives 16x16, a moved warning changes the box. `want` text and header comment updated; README row now states the 42x16+50+482 box and the 128/120 pixel reasoning, matching Ruling R13. Report shows `bash -n` exit 0 and `M4C_SOLO_CONTROLLI=1` on neoforge and forge folders, both `grigio colore=248 riquadro=42x16+50+482` ok, exit 0.

### New Breakage in the Fix Diff
None.

### Out-of-Scope Observations
None.

### Verdict
**Fix round:** All findings addressed, no new Critical/Important breakage
