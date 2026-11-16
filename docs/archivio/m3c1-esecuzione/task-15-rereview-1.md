### Finding Verdicts

- **[T15-F1] On both Fabric variants the bed refusal is silent** — ADDRESSED.
  - src/main/java/com/toxineren/exoframes/frame/SuitRestrictions.java:37: the condition is now only `hand == InteractionHand.MAIN_HAND`, with no `isClientSide()` guard. Line 38 still makes the same `displayClientMessage(Component.translatable(SuitTick.LEAVE_FRAME_FIRST), true)` call, and line 40 still returns `interruptFalse()` for both hands on both sides. There is no loader seam and no client-only class.
  - Class Javadoc (SuitRestrictions.java:11-17): it now says both sides show the message, for the main hand only, and gives the reason for Fabric and for Forge/NeoForge.
  - docs/superpowers/specs/2026-11-13-m3-telaio-design.md:469: the §12.3 sentence 'il messaggio lo manda solo il server' is replaced. The new text says what the code does and why: on Fabric a click cancelled on the client never reaches the server, and on Forge/NeoForge the server's copy lands in the same action-bar line with the same text.
  - Check I ran: `git grep -n 'solo il server' ff75a4c -- docs README.md` finds only the amended line 469, whose new wording contains the phrase "se parlasse solo il server".
  - Check I ran on the real-client runs:
    - task-15-client/{1.21.1-fabric,1.20.1-fabric,1.21.1-neoforge}/letto-rifiutato.png: I viewed all three. Each shows the bed under the crosshair and "Leave the exoframe first" in the action bar, a single line on NeoForge. The three files have different md5 hashes.
    - log-check.txt is 0 bytes on all three variants.
    - rcon.log: the bed is placed by command and the player is aimed with `tp Exo 0.5 -60 0.5 0 47`. Afterwards `execute unless data entity Exo SleepingX` scores 1 on all three, so the player is still standing.
    - client.log shows "Loading Minecraft 1.20.1" and "Loading Minecraft 1.21.1" for the two Fabric runs, and a neoforge-21.1.266 jar for the NeoForge run.
    - The fix report (task-15-report.md:151-158) names the screenshots and the folders.
  - The in-world test is unchanged. The mock player is a server player, so the test still sees one message for the main hand and none for the off hand. run_gametests output on all 4 variants is in task-15-report.md:169-175.
- **[T15-F3] Inaccurate Javadoc 'Pubblico per i test.'** — ADDRESSED. At SuitRestrictions.java:31-32 the Javadoc of onUseBlock ends at "...altrimenti lascia decidere al gioco." and the sentence is gone.

### New Breakage in the Fix Diff

None.
- The message still goes out once per click on the client: vanilla's use loop stops after a FAIL on the main hand, and the off hand is excluded by the guard anyway.
- On NeoForge the server's copy overwrites the same overlay slot. The screenshot shows a single line.

### Out-of-Scope Observations

- docs/superpowers/plans/2026-11-15-m3c1-consumare.md:927-928: the Task 15 text of the plan still states the old rule ("e solo dal server" / "il messaggio solo dal server") in different words. The implementer left it on purpose as the coordinator's document (task-15-report.md:115-119). For the coordinator to decide whether to amend it.

### Verdict

**Fix round:** All findings addressed, no new Critical/Important breakage.
