# SDD ledger — plan: docs/superpowers/plans/2026-11-15-m3c1-consumare.md

Branch m3c-giocarci, merge base main 2e35aa6. Spec: docs/superpowers/specs/2026-11-13-m3-telaio-design.md (§12). Plan committed at 6af544c.
Execution: one implementer per task, one task reviewer, fix rounds with a scoped re-review (five at most), then the coordinator rules. Minor findings are deferred here and go to the final whole-branch review.

## Pre-flight

Ruling: the pre-flight table is replaced by the reviews the plan already had (two critics of the skeleton, 42 findings; three independent reviewers of the full text for design, execution and coherence, 34 findings, 32 applied and 2 declined in part; the corrector's full re-read of numbering, per-task test counts, cross-references, names and commands) — a second scan would repeat that work inside the weekly usage cap the owner set — if wrong: a conflict between two tasks surfaces in a task review and costs fix rounds.
Ruling: the plan's length (1,460 lines, long code bodies) is accepted as it is — the bodies pin orders and traps the reviews found — if wrong: implementers transcribe where they should design; the task reviews still judge the code.
Ruling: Review Focus 1 rests partly on FrameStateSavedDataTest.changeWornRefusedWhenReadOnly, which cannot tell the read-only branch from the missing-record branch — accepted, an in-world test would need a test-only seam the design rejects — if wrong: a regression in the one-line read-only branch of changeWorn is seen only by the final review.
Ruling: "once per player and session" for ERROR lines may be keyed by UUID (SuitTick) or by player object (SuitDamage) — both meet the rule — if wrong: one extra ERROR line after a respawn.
Ruling: Task 21 rewrites the tail of the 3b test mountPieceAndCellByHand — design §12.10 makes a second cell a swap — if wrong: a behaviour of part 3b changes unnoticed; it is listed in the final report to the owner.
Ruling: /exoframe debug energy has a fourth outcome REFUSED (read-only registry: nothing in chat) — design §12.14 says so since 8fe489a — if wrong: an operator gets no feedback in chat, only the log line.
Ruling: WornState.changed takes five arguments (player, event, frameId, level, position) — design §12.14 says so since 8fe489a — if wrong: nothing for the player; a wider internal signature.
Ruling: a config map written only in part keeps one WARN per missing key — design §12.9 says so since 8fe489a — if wrong: extra log lines for server admins.
Ruling: leaving water with an empty cell is not tried in the real client (the numbers were never measured; review finding declined) — the exit is tried at full charge — if wrong: a suit at EMPTY may be unable to climb a bank one block high; the PC checklist of 3c-2 must try it.
Ruling: fix rounds 4-5 run on the same model at the highest effort, not on a more capable model — the owner wants this model for every worker — if wrong: a stuck task reaches the breaker and comes to the coordinator one tier earlier.

## Tasks

Task 1: minor (deferred): [T1-F1] Four unused imports in GameTestKit
Task 1: minor (deferred): [T1-F2] Class comment says 'parti seguenti' where the brief says 'task seguenti'
Task 1: minor (deferred): [T1-F3] fillPool Javadoc omits the caller precondition from the brief
Task 1: complete (commits 6af544c..b5cb011, review clean)
Task 2: BLOCKED (to director) — Stopped early: I was told to return before the task was finished. Nothing is committed and HEAD is still BASE. | Steps 1-5 and 8 are done but uncommitted in the working tree: tools/check_log.py, tools/log_allowlist.txt, tools/run_gametests.sh, tools/client/client_shot.sh, tools/README.md, tools/client/README.md. Keep these changes, do not discard them. | Still to do: Step 6 (run bash tools/run_gametests.sh; expect 96 tests on each variant; then the missing.log and spaces.log checks with sed on the 1.21.1-neoforge log), Step 7 (real client: EXO_ACCEPT_EULA=true bash tools/client/client_shot.sh 1.21.1-fabric /tmp/scatti/t2 "@tieni ctrl+w 1"; also check autoJump:false in options.txt), Step 9 (…
Task 2: Ruling: the BLOCKED above was not a real blocker — the worker started the GameTest run in the background, ended its turn to wait for it and was forced to answer; the dispatch rules now say to run long commands in the foreground and never to end a turn while work remains — same brief, new worker, the uncommitted work is kept — if wrong: nothing is lost, the task review judges the result.
Task 2: minor (deferred): [T2-F1] Failure headlines and client_shot.sh header still speak only of lines outside the allowlist
Task 2: complete (commits b5cb011..f754b0e, review clean)
Task 3: minor (deferred): [T3-F1] Debug log lines claim a frame was deleted/worn when the read-only registry refused the write
Task 3: minor (deferred): [T3-F2] The brief's WornState Javadoc text and its 'Task 24 corrects §12.14' note are out of date
Task 3: minor (deferred): [T3-F3] One Javadoc line is 121 characters, but the report says all lines are under 120
Task 3: complete (commits f754b0e..d5f0809, review clean)
Task 4: minor (deferred): [T4-F1] One sentence of the class comment went to FrameRegistry instead of FrameStateSavedData, against the brief's line map
Task 4: minor (deferred): [T4-F2] FrameStateAccessImpl Javadoc still points to FrameStateSavedData for the unread-entries rule
Task 4: complete (commits d5f0809..3296eb5, review clean)
Task 5: minor (deferred): [T5-F1] Recovery paths give contradictory ERROR lines for registry data without sections
Task 5: minor (deferred): [T5-F2] get() allocates a Path on every call, including the per-tick get(server) path
Task 5: minor (deferred): [T5-F3] docs/compat.md GameTests 'Condizione' line omits readRegistry
Task 5: minor (deferred): [T5-F4] Refused-change ERROR says 'could not be read' for a file that was read
Task 5: complete (commits 3296eb5..88e3614, review clean)
Task 6: minor (deferred): [T6-F1] Missing blank line before the new test method
Task 6: minor (deferred): [T6-F2] No test for rule 1's tie-break when several boxes contain the eyes
Task 6: complete (commits 88e3614..e0593d7, review clean)
Task 7: minor (deferred): [T7-F1] No test exercises the cart search's downward extension (expandTowards(0, -1, 0))
Task 7: complete (commits e0593d7..cd5b057, review clean)
Task 8: implementer concern (to director): The defaults file is created with permissions 0600 (-rw-------). Files.createTempFile, the method the brief prescribes, creates the temporary file that way and the move keeps it. The server's own user can read and edit the file; other users on the machine cannot. Not changed.
Task 8: implementer concern (to director): Step 10 on 1.20.1-forge: the first start logged one Forge registry WARN ('Object did not get ID it asked for', redstone_exoframes:exoframe 1274 -> 1275). It came from remapping item IDs in the old Nov 13 world in versions/1.20.1-forge/run/world, not from the config. A second start (file removed again) was clean, so the grep printed nothing.
Task 8: minor (deferred): [T8-F2] The defaults file is created with 0600 permissions instead of following the umask
Task 8: minor (deferred): [T8-F3] No JUnit for the upper bounds on idle/sprint/jumpCost, cellSwapSeconds, module costs and protectionMultiplier
Task 8: plan-mandated (to director): [T8-F1] (Important) Converting through the JSON tree silently truncates decimals and wraps oversized numbers in int fields
Task 8: complete (commits cd5b057..2f6d61e, review clean, 1 to director)
Task 8: Ruling: [T8-F1] is fixed now, by the reviewer's smallest change — under design §12.9 a number an int field cannot hold (a decimal such as 0.5, or an integer outside the int range) is a wrong type: one problem, the whole config at its defaults; so ConfigFile keeps the tree parse only for the 'not valid JSON' and 'not a JSON object' answers and builds the config from the text (Gson's streaming reader refuses those numbers, the tree conversion truncated or wrapped them in silence); JUnit cases for 0.5 and 4294977296 in int fields, also in brokenFileIsNeverRewritten; the tree conversion that Step 4.2 of the brief prescribes is overruled — if wrong: an admin who writes one decimal in an int field loses that start's whole file (with a WARN that names the value and where it is) instead of that one field.
Task 8: Ruling: [T8-F2] and [T8-F3] ride along in the same fix, since the same files are open — the defaults file must follow the process umask like every other file in config/ (temporary file created with CREATE_NEW under a unique name in the same folder, same move without REPLACE_EXISTING, same cleanup), and each new upper bound without a JUnit case gets one (idle/sprint/jumpCost, cellSwapSeconds, module costs, tiers.protectionMultiplier) — if wrong: a slightly larger fix diff to re-review.
Task 8: Ruling: implementer concern 2 (one Forge WARN 'Object did not get ID it asked for' on the first dedicated-server start of 1.20.1-forge) is not a defect of the mod — it comes from the stale development world of 13 Nov in versions/1.20.1-forge/run/world, the second start was clean and the reviewer checked the logs — if wrong: nothing ships from that folder.
Process: Ruling: from Task 8's fix on, the pipeline pushes right after the implementer (before the review) and reads the weekly usage there too; it starts no new task at 46% and no new stage inside a task at 48% (the owner's cap is 50%); a task with a plan-mandated Important finding or a cannot-verify item stops the sequence after its completion line, so the coordinator rules before the next task builds on it — if wrong: unreviewed commits sit on the feature branch for a few minutes (main is untouched until the final review).
Task 8: minor (deferred): [T8-R1-B1] A duplicate key inside a map now sets the whole config to its defaults
Task 8: fix round 1/5 (3 addressed, 0 open; commits 2f6d61e..5389f2b)
Task 8: complete (commits cd5b057..5389f2b, review clean after 1 fix round)
Task 9: implemented (commits 5389f2b..81238da), review pending
Task 9: minor (deferred): [T9-F1] No test checks that a refused exit or a refused enter plays no sound
Task 9: complete (commits 5389f2b..81238da, review clean)
Task 10: implemented (commits 81238da..d1e6bcc), review pending
Task 10: minor (deferred): [T10-F1] Stale mixin summary comment in build.gradle.kts
Task 10: complete (commits 81238da..d1e6bcc, review clean)
Task 11: implemented (commits d1e6bcc..ccce810), review pending
Task 11: minor (deferred): [T11-F1] docs/compat.md: the list of the four write methods now has clientIndicators/onSynced in the middle, and the clear clause hangs after onSynced
Task 11: minor (deferred): [T11-F2] No test covers FrameStateClientCache keeping indicators and dropping them on an absent state
Task 11: complete (commits d1e6bcc..ccce810, review clean)
Task 8: Ruling: [T8-R1-B1] is accepted as it is — a key written twice inside a map is a mistake in the file, the problem line names it, and design §12.9 gives one problem and the whole config at its defaults whenever the file cannot be turned into a config — if wrong: an admin with a duplicated key loses that start's settings where the last value could have won.
Task 12: implemented (commits ccce810..2397378), review pending
Task 12: minor (deferred): [T12-F1] compat.md:47 sentence about a real-client test now reads as if client_rules was already tried
Task 12: minor (deferred): [T12-F2] No test checks the one-WARN-per-name rule
Task 12: complete (commits ccce810..2397378, review clean)
Task 13: implemented (commits 2397378..7a54fe9), review pending
Task 13: minor (deferred): [T13-F1] ActivityRuleTest never pins a direct reset() on a window holding more than one sample
Task 13: complete (commits 2397378..7a54fe9, review clean)
Task 14: implemented (commits 7a54fe9..ae43516), review pending
Task 14: minor (deferred): [T14-F1] Two step-6 transition branches are not pinned by any test
Task 14: minor (deferred): [T14-F2] compat.md parenthetical '(un tick fermo non manda nulla)' overstates the rule
Task 14: complete (commits 7a54fe9..ae43516, review clean)
Task 15: implementer concern (to director): On a real Fabric client (both 1.20.1 and 1.21.1), the 'leave frame first' message will never show. I read Fabric API's ClientPlayerInteractionManagerMixin.interactBlock in versions 0.6.6 and 0.7.14. When the client-side result is FAIL, which is what interruptFalse() becomes, the client sends no use-block packet. The server never sees the click, so the bed is refused without any message. On Forge …
Task 15: implemented (commits ae43516..786c77e), review pending
Task 15: minor (deferred): [T15-F2] The refusal covers more than sleeping: sneak-click item uses on a bed are blocked too, with a misleading message
Task 15: minor (deferred): [T15-F3] The onUseBlock Javadoc says 'Pubblico per i test', but no test calls it directly
Task 15: plan-mandated (to director): [T15-F1] (Important) On both Fabric variants the bed refusal is silent: the 'leave the exoframe first' message never reaches the player
Task 15: cannot verify (to director): Real-client behaviour on the four variants: the bed refused with no sleep animation, and on Forge/NeoForge the action-bar message once per click. The mock player has no client; brief Step 5 assigns this to the 3c-2 PC checklist. The packet flow was checked statically from the Fabric API, Architectury and patched Forge/NeoForge jars (see T15-F1).
Task 15: complete (commits ae43516..786c77e, review clean, 2 to director)
Task 15: Ruling: [T15-F1] is fixed now — the listener shows the message on both sides (the same displayClientMessage call, main hand only, no loader seam, no client-only class): on Fabric a click the client cancels never reaches the server, so the client must speak; on Forge and NeoForge the server's copy lands in the same action-bar slot with the same text, which a player cannot tell from one message; the sentence of design §12.3 ('il messaggio lo manda solo il server') is amended in the same fix; the fix is tried on the real client of 1.21.1-fabric, 1.20.1-fabric and 1.21.1-neoforge (bed placed by command, suit worn by the debug command, right click, screenshot of the action bar) — if wrong: a doubled or missing action-bar line on one loader, which the PC checklist of 3c-2 tries again on the production jars.
Task 15: Ruling: [T15-F2] stays as it is — in a suit every right-click on a bed is refused, a sneak-click with an item too; Forge and NeoForge patch vanilla's bypass condition (an item that declares a sneak bypass still reaches the bed's use), so mirroring it could let a suited player sleep with a modded tool in hand — if wrong: a suited player cannot place a block against a bed while sneaking and reads 'leave the exoframe first' there.
Task 15: Ruling: [T15-F3] rides along in the same fix (one Javadoc sentence in the same file).
Task 15: Ruling: the cannot-verify item (the bed on a real client) is answered in the cloud by the fix's own client runs on three variants; 1.20.1-forge and the production jars stay on the PC checklist of 3c-2, as the brief says — if wrong: nothing beyond what that checklist already catches.
Task 15: minor (deferred): out of scope, seen in fix round 1: docs/superpowers/plans/2026-11-15-m3c1-consumare.md:927-928: the Task 15 text of the plan still states the old server-only message rule in different words. The implementer left it on purpose as the coordinator's document, so the coordinator decides whether to amend it.
Task 15: fix round 1/5 (2 addressed, 0 open; commits 786c77e..ff75a4c)
Task 15: complete (commits ae43516..ff75a4c, review clean after 1 fix round)
Task 16: implemented (commits ff75a4c..56d0703), review pending
Task 16: minor (deferred): [T16-F1] FrameAttributesImpl class Javadoc says the opposite of the idempotence rule
Task 16: minor (deferred): [T16-F2] stepModifierFollowsTheVersion: the localStep-on-ServerPlayer checks cannot fail
Task 16: complete (commits ff75a4c..56d0703, review clean)
Task 17: implementer concern (to director): Departure from the brief in the test noWearInCreativeOrSpectator. On the 1.20.1-forge test server, the player from mockServerPlayer starts without instabuild, so the brief's game-mode premise failed. I added creative.setGameMode(GameType.CREATIVE) after mockServerPlayer, the same line EnergyGameTests.creativeDrainsNothingBesideSurvival already uses. The premise check is still in the test and now …
Task 17: implementer concern (to director): The plan header says the mock player starts in creative, but on 1.20.1-forge it does not have instabuild. Later tasks that count on a creative mock player must set the game mode themselves. I did not change compat/GameTests, because that is outside this task.
Task 17: implemented (commits 56d0703..133406d), review pending
Task 17: minor (deferred): [T17-F1] A same-hit CRITICAL action-bar message overwrites the BROKEN one
Task 17: minor (deferred): [T17-F2] routeMilliMatchesRouteOnWholePoints holds by construction
Task 17: minor (deferred): [T17-F3] Plan premise of a creative mock player is false on 1.20.1-forge
Task 17: complete (commits 56d0703..133406d, review clean)
Task 18: implemented (commits 133406d..930439e), review pending
Task 18: minor (deferred): [T18-F1] DamageGameTests class Javadoc no longer describes the new fall tests
Task 18: complete (commits 133406d..930439e, review clean)
Process: Ruling: an in-world test that needs a creative or a survival player sets the game mode itself (GameTestKit.survivalPlayer, or setGameMode on the mock player) and never counts on the mock player's default, which is not creative on 1.20.1-forge; the plan header's sentence that the mock player starts in creative is wrong on that variant (Task 17's two concerns and [T17-F3]) — if wrong: one extra line in a test.
Task 15: Ruling: the plan's own Task 15 text (lines 927-928) keeps the old server-only wording — the plan records what was asked, the ledger and the amended design §12.3 say what holds — if wrong: a reader of the plan alone finds the old rule.
Task 19: implemented (commits 930439e..0f469e2), review pending
Task 19: minor (deferred): [T19-F1] The brief's single doTick() cannot meet its own isUnderWater() starting condition; the code correctly uses two
Task 19: minor (deferred): [T19-F2] The commit body says 'vasca profonda 4'; the pool is 3 deep
Task 19: minor (deferred): [T19-F3] The compat.md 'Verifica' bullet still records only the Task 10 empty-handler run
Task 19: complete (commits 930439e..0f469e2, review clean)
Task 20: implemented (commits 0f469e2..3abc47d), review pending
Task 20: minor (deferred): [T20-F1] No test for the trusted read dropping a cell without a valid capacity
Task 20: cannot verify (to director): The real hover tooltip (the per-version appendHoverText overrides in compat/impl/ItemBase.java:38-50, including the commented 1.20.1 branch) and how the cell and piece bars render in a real client. No GameTest reaches either, as the brief says; Task 24 (client inventory screenshot) and the 3c-2 PC checklist must confirm them on both game versions.
Task 20: complete (commits 0f469e2..3abc47d, review clean, 1 to director)
Task 19: Ruling: [T19-F1] the two doTick() calls of the starting check stand — the game copies the eye fluid of the previous tick, so 'under water' is true only from the second tick, and the brief's one-tick premise was wrong — if wrong: nothing, the test asserts its own starting conditions.
Task 19: Ruling: [T19-F2] the commit body of 0f469e2 keeps its slip (it says the pool is 4 deep, it is 3) — history is never rewritten here; the code and the test Javadoc carry the right number — if wrong: a reader of that one commit message is misled about a test fixture.
Task 20: Ruling: the cannot-verify item (hover tooltips and item bars in a real client) is answered by Task 24's real-client inventory screenshots on both game versions, and it stays on the PC checklist of 3c-2 — if wrong: a wrong tooltip line or bar colour reaches the checklist, not the players.
Task 21: implemented (commits 3abc47d..a102066), review pending
Task 21: minor (deferred): [T21-F1] The death test's Javadoc says it catches update in place of changeWorn; it cannot
Task 21: minor (deferred): [T21-F2] No in-world test takes the cell from the off hand or refuses a spectator
Task 21: complete (commits 3abc47d..a102066, review clean)
Task 22: implementer concern (to director): The energy line can cover the hotbar. At GUI width 480 (scale 2 at 960x540, and auto scale at 1920x1080) the line with the worst piece is about 235 GUI px wide and starts at x≈241, inside the hotbar (x 149-331), so it covers slots 7-9 (visible in riga-energia-critico.png). The Italian line without a piece also overlaps the hotbar slightly. I kept the position because the brief, the design §12.13 …
Task 22: implemented (commits a102066..a5703f3), review pending
Task 22: minor (deferred): [T22-F2] Piece-name translation key built in two places
Task 22: minor (deferred): [T22-F3] LOW/EMPTY colours, NO_CELL and BROKEN wording never tried in a real client
Task 22: plan-mandated (to director): [T22-F1] (Important) At default GUI scale the energy line draws over the hotbar
Task 22: cannot verify (to director): Whether Architectury actually fires ClientGuiEvent.RENDER_HUD in a real client on 1.20.1-forge and 1.21.1-fabric. The four builds compile, but Step 5 tries only 1.20.1-fabric and 1.21.1-neoforge, as the brief asks.
Task 22: complete (commits a102066..a5703f3, review clean, 2 to director)
Task 22: Ruling: [T22-F1] is fixed now — the energy line moves to the top-left corner (x 4, y 4, left-aligned, with shadow), and the worst piece goes on a second line (y 14) coloured by its condition (CRITICAL yellow, BROKEN red); the bottom-right corner that the plan header fixed is where the hotbar reaches at the common GUI widths (480 at 1080p, about 427 at 720p, 1440p and 4K) and where the game draws the autosave indicator and the subtitles; the design leaves the position to the builder (section 5, the list of what is left to the builder), so only the plan header is overruled; the key hud.redstone_exoframes.energy_worst becomes hud.redstone_exoframes.worst — if wrong: the owner wants the line elsewhere (the top-left corner is where some minimap mods draw by default, and the F3 text covers it): two constants to move, listed in the final report.
Task 22: Ruling: the cannot-verify item (does RENDER_HUD fire on 1.20.1-forge and 1.21.1-fabric) is answered by the fix's own real-client runs on those two variants — if wrong: the line is missing on one loader, which the PC checklist of 3c-2 shows at once.
Task 22: Ruling: [T22-F2] rides along in the same fix (same file); [T22-F3] waits for Task 24's real-client run and the PC checklist of 3c-2.
Task 22: minor (deferred): [T22-R1-B1] Italian worst-piece line overlaps the boss bar
Task 22: minor (deferred): [T22-R1-B2] Design 12.13 still describes the one-line format with the '·' join
Task 22: fix round 1/5 (2 addressed, 0 open; commits a5703f3..ea49e3f)
Task 22: complete (commits a102066..ea49e3f, review clean after 1 fix round)
Task 23: implemented (commits ea49e3f..ac3501f), review pending
Task 23: minor (deferred): [T23-F1] setEnergy uses a switch statement with a throwing default instead of an exhaustive switch expression
Task 23: minor (deferred): [T23-F2] showEnergy repeats DebugEnergy.set's worn-state lookup verbatim
Task 23: minor (deferred): [T23-F3] Private test helpers copy GameTestKit logic
Task 23: minor (deferred): [T23-F4] No test checks the energy.show message or showEnergy's failure branches
Task 23: cannot verify (to director): Step 5 real-client run (1.20.1-fabric: 7.12 / 9.25 blocks; 1.21.1-neoforge: 7.22 / 9.38 blocks; exit 0; empty log-check.txt): the numbers are inside the brief's ranges, but the shot folders are only in the implementer's session scratchpad, not on the branch. The brief does not ask for them to be committed.
Task 23: complete (commits ea49e3f..ac3501f, review clean, 1 to director)
Task 23: Ruling: the cannot-verify item (the real-client folders of Step 5 are in a scratch folder, not on the branch) is accepted — the brief does not ask to commit them, the measured distances are in the task report (999 RE: 7.12 and 7.22 blocks without sprint; 5000 RE: 9.25 and 9.38 with sprint), and Task 24 makes the committed real-client evidence — if wrong: the sprint rule is tried again on the PC checklist of 3c-2 anyway.
Task 24: implemented (commits ac3501f..d67e8b6), review pending
Task 24: minor (deferred): [T24-F1] Stale 'insieme al caricamento della config' direction in compat.md module-restore limit
Task 24: complete (commits ac3501f..d67e8b6, review clean)
Final review, area A (energy, movement and client): ready to merge = with_fixes; 0 Critical, 0 Important, 3 Minor; report /home/user/exosuits/.superpowers/sdd/2026-11-15-m3c1-consumare/final-review-A.md
Final review, area B (damage, registry and the carry-overs of part 3b): ready to merge = yes; 0 Critical, 0 Important, 1 Minor; report /home/user/exosuits/.superpowers/sdd/2026-11-15-m3c1-consumare/final-review-B.md
Final review, area C (config, tools, test scaffolding and documents): ready to merge = with_fixes; 0 Critical, 2 Important, 4 Minor; report /home/user/exosuits/.superpowers/sdd/2026-11-15-m3c1-consumare/final-review-C.md
Final review: Ruling: [FC-1] is fixed in the wave with the finding's option (a) — at load a cell whose stored charge is greater than the configured capacity keeps the capacity written in the file and its charge (one WARN per load with the count); every other cell takes the configured capacity as before; the clamp at mount, swap and charger stays — because one start with a broken config file (which by the Task 8 rulings means the defaults) would otherwise cut every saved charge for good, and 'nothing is lost' outranks 'one capacity'; design §12.10 is amended in the same wave — if wrong: after an admin lowers the capacity on purpose, cells that hold more keep their old capacity until they drain or are swapped, an exception to the single capacity the owner did not ask for.
Final review: Ruling: [FC-2] is fixed in the wave with a header note in the 3a and 3b PC checklists that lists the steps whose result changes with the jars of part 3c-1 (the steps themselves are not rewritten) — if wrong: a checklist reader skips the note and reports intended behaviour as a failure.
Final review: Ruling: [FA-1] changes no code — a read-only registry holds no worn frame, so /exoframe debug energy answers 'not wearing' and REFUSED stays only as a defensive branch; the Javadocs and design §12.14 are corrected, and the pre-flight ruling that gave REFUSED its own silent outcome is superseded — if wrong: nothing for a player; one unreachable branch stays in the code.
Final review: Ruling: the Minor findings [FA-3], [FB-1], [FC-3], [FC-4], [FC-6] and the deferred minors [T16-F1], [T24-F1], [T22-R1-B2] are fixed in the wave (texts only), and so is [FC-5] (the real-client script fails on 'moved wrongly', starts from the default config, and the test config is cleaned with the test world) — if wrong: a slightly larger fix diff to re-review.
Final review: Ruling: [FA-2] (no real client has shown client_rules arriving with a server config different from the defaults) is not fixed here — it becomes the first verification of part 3c-2 on the four variants and a line of its PC checklist; in single player the two sides share one config, so the owner's own play cannot show it — if wrong: on a dedicated server with a changed config the cell bars and the hold times of the two keys could be wrong on one loader until 3c-2 tries it.
Final review: Ruling: every deferred minor the three reviewers marked 'leave' stays as it is and every one marked 'obsolete' is closed (39 and 21 lines; the lists are in final-review-A/B/C.md, archived with the ledger) — they are test gaps, wording and accepted limits that hurt no player — if wrong: a regression in one of those places is caught later than it could be.
Final review: Ruling: the 22 'declined to judge' lines of the three reviewers are each accepted as set aside (each cites the design section or the earlier part that decides it: a modified client can still sprint or swim, idle drain during the End credits, other mods' fluids and beds, the quit save writing players before the registry, frames stacking at death, the NeoForge damage-container leftover on a cancelled fall, and the like) — if wrong: the behaviour stays as designed until the owner asks otherwise; the notable ones are in the final report.
Final fix wave: DONE (commits d67e8b6..aef637d; 1.20.1-forge 172, 1.20.1-fabric 172, 1.21.1-fabric 172, 1.21.1-neoforge 172. All four printed 'OK <variante>: 172 test obbligatori passati, nessun avviso fuori…), re-review pending
Final fix wave: re-review of d67e8b6..aef637d — 12 addressed, 0 not addressed, 0 withdrawn, 3 new breakage; report /home/user/exosuits/.superpowers/sdd/2026-11-15-m3c1-consumare/final-rereview.md
Final fix wave: new breakage (to director): [FW-B1] (Minor) m3c_shots.sh can read a stale server.log after an early exit of client_shot.sh
Final fix wave: new breakage (to director): [FW-B2] (Minor) client_shot.sh now has no way to run with a chosen config, which the planned 3c-2 FA-2 check needs
Final fix wave: new breakage (to director): [FW-B3] (Minor) FrameStateAccessImpl Javadoc lists an unreadable file alone as a read-only cause
Final fix wave: out of scope, seen in the re-review: Consequence of FC-1 option (a), which the ruling accepted, not stated in any document: while a cell keeps a capacity above the config's, its LOW threshold is relative to that capacity (EnergyState.of uses cell.fraction(), so 40 000 of 50 000 goes LOW at 5 000 RE), and the kept capacity stays until …
Final fix wave: out of scope, seen in the re-review: The fix report lists the 1.21.1-fabric real-client run under the closing checks 'su HEAD aef637d', but that run ended at 01:12:56, before commits 62158ac (01:13:11) and aef637d (01:13:38). client_shot.sh builds the working tree as it is and aef637d changes only a Javadoc, so the evidence still hold…
Final review: Ruling: the three Minor issues the fix wave introduced are parked for the first task of part 3c-2, with no second wave — [FW-B1] m3c_shots.sh can blame 'moved wrongly' after an early exit of client_shot.sh (the exit code stays right, only the message is wrong); [FW-B2] client_shot.sh now always starts from the default config, so the 3c-2 check of client_rules with a changed server config needs an option to pass a config file; [FW-B3] one Javadoc phrase in FrameStateAccessImpl lists an unreadable file alone as a read-only cause — if wrong: a misleading tool message and one imprecise comment live on main until 3c-2.
Final review: Ruling: a consequence of [FC-1] is accepted and goes in the owner's report — while a cell keeps a capacity above the configured one, its 10 % 'low energy' threshold is counted on that capacity (40 000 of 50 000 goes low at 5 000 RE), and it keeps that capacity until the first load after it has drained below the configured capacity — if wrong: an odd threshold on cells left over from a larger capacity, never a loss.
