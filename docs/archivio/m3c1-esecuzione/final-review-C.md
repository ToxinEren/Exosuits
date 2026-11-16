# Final review, area C (config, tools, test scaffolding, documents) — m3c-giocarci 2e35aa6..d67e8b6

Reviewer C. Read-only review; no Gradle, game, test script or real client was run. I read the whole production diff
of area C (`final-diff-C.patch`), the tests diff as far as GameTestKit, ConfigFileTest and ExoframesConfigTest go,
the plan header (lines 1-96), design §12 in full, the ledger and its rulings, and followed the interactions into the
current code of areas A and B (RedstoneExoframes.init, FrameStateCodec, FrameRegistryFormat, FrameStateAccessImpl,
SyncCounter, SuitTick, SuitDamage, SuitMovement, SuitRestrictions, CellSwap, ClientRules, EntryHints, WornState,
FrameSounds, EnergyLine, ActivityRule) and into the PC checklists of parts 3a and 3b.

## Strengths

- **Config file (Task 8 and its fix round).** `ConfigFile.load` (configfile/ConfigFile.java:66-95) never rewrites a
  file it read, builds the config from the text with Gson's streaming reader (so 0.5 or 4294977296 in an int field
  is a wrong type, not a silent truncation), and writes the defaults through a CREATE_NEW temporary in the same
  folder moved without REPLACE_EXISTING (umask respected, a file that appears meanwhile is never overwritten). Every
  path returns a `Result`; `ConfigValidation.validate` handles null sections, null map values, NaN and infinities, so
  nothing escapes `load` even though `validate` runs outside its try. JUnit covers every case §12.15 lists, including
  `brokenFileIsNeverRewritten` with the modification time pushed back an hour and both upper-bound tests at the limit
  and over it.
- **Overflow discipline holds across areas.** The fields that keep no upper bound (`tiers.maxDurability`,
  `pieces.baseModuleSlots`) only reach products computed in `long` (PieceCondition.of, EnergyLine.isWorse,
  DamageRouter.MAX_MILLI) or in float (CellBar); the bounded ones keep every 3c product under `Integer.MAX_VALUE`.
- **check_log.py exact counts (Task 2).** With every allowlist entry now `=N` or per loader, no entry can go dead
  silently: a wanted line that stops appearing fails the run. The `--variant` plumbing is right in
  run_gametests.sh:46 and client_shot.sh:474, and the Fabric-only `No data fixer` line is `fabric=1,forge=0,neoforge=0`
  in both scopes.
- **Test count and names.** `cat gametest/*.java | grep -c '^\s*add(tests,'` gives 172 (95+21+27+13+4+12, GameTestKit
  0), the README number; no test name repeats across the six classes; times given to `SuitTick.tick` and
  `CellSwap.handle` only move forward from `t0`/`now(helper)`.
- **Test seams stay out of production.** Outside `gametest/` the only GameTests call is `register` (six lines in
  `init()`); `SyncCounter` costs one map merge per `syncTo` and is forgotten at `PLAYER_QUIT`
  (FrameStateAccessImpl.java:142, 348); the FrameSounds recorder list is empty in production; `attributesToSync`
  clears the game's set only inside tests.
- **Stonecutter branches of GameTestsImpl are right per version**: `getAttributesToSync()`/`getDirtyAttributes()`,
  `unwrapKey()` vs `BuiltInRegistries.ATTRIBUTE.getKey`, `getModifier(ResourceLocation)`/`amount()` vs
  `getModifier(UUID)`/`getAmount()` with the same UUID derivation as FrameAttributesImpl, the two `PlayerList.respawn`
  signatures, the NeoForge-only `IOUtilities.waitUntilIOWorkerComplete()`.
- **check_jars.sh (e2)** lists both new mixins with the exact names per loader, and the Mojang markers end with `(`
  so only annotation strings trip them; both `redstone_exoframes.mixins.json` match the expected sections.
- **Language files.** 75 keys, the same set in the same order in both files, every key built in code (literal or
  from `piece.`/`condition.`/`energy.` + enum name) exists, `%s` counts match, the plan's 'Testi' order is respected
  with the `hud.redstone_exoframes.worst` rename of the T22 ruling, and `no_cell` changed in place at line 32.
- **compat.md documents every new seam** (FrameIndicators, changeWorn, clientIndicators, onSynced, the two channels,
  the extended sync format, localStep and the fifth modifier, the damages_helmet rule, the six GameTests helpers,
  CompatItem/ItemBase, addHudRenderer, the two mixins with their jar names), and six earlier deferred document
  minors are fixed. The committed screenshots carry no metadata chunks and show what the documents describe.

## Issues

### Critical (Must Fix)

None.

### Important (Should Fix)

**FC-1. A start with the config at its defaults permanently cuts the charge of every cell in the frame registry.**

- File:line: src/main/java/com/toxineren/exoframes/RedstoneExoframes.java:53-58 (any problem: `setConfig` with the
  defaults or with `energy.cellCapacity` at its default); src/main/java/com/toxineren/exoframes/configfile/ConfigFile.java:74-91
  (whole-file fallbacks: unreadable, not JSON, wrong type, decimal or >int in an int field, unknown or lowercase enum
  key, duplicated map key); src/main/java/com/toxineren/exoframes/compat/impl/FrameRegistryFormat.java:110,126,147
  (the registry load reads every worn and placed record with `RedstoneExoframes.config()`);
  src/main/java/com/toxineren/exoframes/compat/impl/FrameStateCodec.java:151-154 (each cell gets
  `cfg.energy.cellCapacity` and `min(capacity, energy)`, silently); FrameRegistryFormat.java:353 (the save writes the
  clamped in-memory state).
- What is wrong: a server that raised `energy.cellCapacity` (say 50 000, allowed up to 1 000 000) and starts once with
  a config that falls back to the default 10 000 (one syntax error, one `0.5`, a `"scrap"` key, `"energy": null`, a
  capacity above 1 000 000, or just a missing `cellCapacity` line, which logs nothing at all) truncates every
  installed cell above 10 000 RE at world load. The first save after any change (any tick of a worn suit marks the
  registry dirty) persists it; after the second save `_old` holds it too. Fixing the file and restarting brings
  nothing back. Cells carried as items survive until a swap or a mount clamps them.
- Why it matters: it is a silent loss of players' saved data, against the anti-goal and against §12's own statement
  that 3c loses nothing except the charger limit. The rulings on [T8-F1] and [T8-R1-B1] priced a whole-file fallback
  as "loses that start's settings"; they did not consider that Task 20's load-time clamp (§12.10) turns one start at
  the defaults into a permanent rewrite of the registry. That is the only part of those rulings this finding reopens.
  No test covers it.
- How to fix (design decision for the coordinator; options in order of preference): (a) at registry load never lower
  a stored charge: for a cell whose written charge exceeds the config capacity keep the written `capacity` (still
  written for compatibility, so every 3b/3c registry has it), with one WARN naming the frame; the clamp then happens
  only on player actions (swap, mount); (b) narrower: when `ConfigFile.Result` had problems (whole-file fallback or
  an `energy.cellCapacity` problem), read cells as `readTrusted` does for that session; (c) minimum, if the design is
  kept: one WARN per clamped cell with the frame id and the RE removed, and a sentence in README "Configurazione" and
  compat.md ("lowering `energy.cellCapacity`, or a file that falls back to the defaults, cuts the saved charge of
  installed cells at the first save"). Add a FrameStateCodecTest case for the chosen rule. Tasks: T8, T20.

**FC-2. The PC checklists of parts 3a and 3b now contradict the merged behaviour in six steps.**

- File:line: docs/test-in-gioco/m3b-entrare-e-uscire.md:25 (a second cell is "Quello slot è già occupato"; since
  Task 21 it swaps, §12.10); :26 and :79 step 12 ("Al telaio serve una cella carica"; since Task 20 the text is "Il
  telaio ha bisogno di una cella carica (le celle si caricano nel caricatore)"); :82 step 15 ("la barra dell'armatura
  sopra la salute è sparita", "Nessun suono di armatura entrando": since Task 16 the frame's armor modifier shows the
  points of the mounted helmet and arm, since Task 9 entering plays the piston sound); :107 step 30 ("Non c'è un
  risultato atteso" for carts, boats and beds: since Tasks 13 and 15 a passenger is dismounted at the first tick and
  a bed is refused, both with "Esci prima dall'esoscheletro"); docs/test-in-gioco/m3a-vedere-il-telaio.md:88
  ("Nuotare ... nuota con lo scatto (posa da nuoto)": since Task 19 the suit never swims). Every entry step now also
  shows the energy line top-left and the keys line in chat, absent from the m3a/m3b reference screenshots.
- Why it matters: README.md:9 sends the owner to these two checklists for the pending PC confirmation of 3a and 3b,
  and the jars CI builds from `main` after this merge are 3c-1 jars: a tester following them reports intended
  behaviour as failures. The 3b merge set the precedent of a header note in m3a (m3a-vedere-il-telaio.md:5). No task
  could see this: each changed one step.
- How to fix: a header note in both checklists, like m3a:5, "Con i jar di dopo l'unione della parte 3c-1 cambiano
  ..." listing the six points and pointing to the coming 3c-2 checklist. Tasks: T9, T13, T15, T16, T19, T20, T21, T24.
  Confidence 'likely': that the owner runs them on post-merge jars is inferred from README and the precedent.

### Minor (Nice to Have)

**FC-3. README "Configurazione" and "Comandi" say less, or other, than the code.**

- README.md:33: "i valori sbagliati tornano ai predefiniti uno per uno" is false for the [0, 1] fields, which are
  clamped (ConfigValidation.java:312), for multipliers below 1 (clamped to 1.0, :326) and for shares (normalized,
  :180). Example an admin will hit: `"lowThreshold": 10` meant as 10 % becomes 1.0, so the suit is LOW and cannot
  sprint unless the cell is full; README promised the default 0.10.
- README.md:33: the whole-file cases omit an unknown or misspelled map key (`"HELMT"`, lowercase `"scrap"`) and a
  key written twice in a map, both of which drop the whole file (ConfigFile.java:88-91; ruled [T8-R1-B1]).
- README.md:31: "Comandi" does not list `/exoframe debug energy [RE]` (Task 23), which tools/client/README.md:90
  relies on.
- Fix: three sentences; plus the FC-1 sentence if option (c) is chosen. Tasks: T8, T23, T24.

**FC-4. compat.md and the test-kit comments still imply a creative mock player; one stale direction.**

- docs/compat.md:86 "Giocatore finto ... sempre creativa come quello vanilla" (same words in GameTestsImpl.java:76 and
  :146) and compat.md:85, whose rationale for `survivalPlayer` is that hits and falls "non contano in creativa": the
  Process ruling after Task 17 established that only `isCreative()` is forced, while the abilities follow the world's
  mode and are not creative on 1.20.1-forge. GameTestKit.java:104-105 tells test authors to use `survivalPlayer` for
  survival but not the ruling's other half (set creative yourself), so a future test that counts on a creative mock
  passes on three variants and fails on 1.20.1-forge only.
- docs/compat.md:292 still gives "decidere, insieme al caricamento della config" as the direction for the module
  restore, though the config is loaded since Task 8 ([T24-F1]).
- Fix: one clause in each place ("abilities follow the game mode; tests set it themselves"); drop the stale phrase.
  Tasks: T1, T17, T24.

**FC-5. m3c_shots.sh does not check two of its own premises.**

- tools/client/m3c_shots.sh:41 and tools/client/README.md:90: "moved wrongly" (Review Focus 6, client and server
  agreeing in water) is a manual `grep -c`; the script exits 0 even when the server logs it, and vanilla's line does
  not name the mod, so check_log.py never sees it. A later regression of the water rule passes the only automated
  real-client run.
- tools/client/client_shot.sh:180-184 recreate the world and server.properties but keep
  `run-shot-server/config/redstone_exoframes.json` and the client's copy, while every m3c threshold (999 RE below
  10 %, the 0.85 speed factor, the 2 s swap) assumes the defaults. Likewise README.md:29 offers
  `./gradlew :<variante>:runGameTestServer` as an equivalent of run_gametests.sh, but `cleanGameTestWorld`
  (build.gradle.kts:118-121) deletes only the world, so that run keeps an edited test config
  (`configFileExists` then fails, at least with a clear message).
- Fix: in m3c_shots.sh, after client_shot.sh returns, fail when `grep -c 'moved wrongly' "$out/server.log"` > 0;
  delete `config/redstone_exoframes.json` in both run folders at the start of client_shot.sh; add the test config to
  `cleanGameTestWorld`. Tasks: T2, T8, T24.

**FC-6. Design §12 no longer says what the code does in two places.**

- §12.15 "Client vero nel cloud": the recognition scripts `tools/client/ricognizione-3c/prova.sh` and `prova_b.sh`
  were never brought into tools/client/; Task 24 wrote `m3c_shots.sh` instead, which keeps the pool, Space, bottom
  walk, depth-1 water, bank exit, current, step and sprint checks (with the 0.85 factor) but drops prova_b's bubble
  column, sprint on the bottom, air control and lava checks. §12.6 states that bubble columns still lift the suit;
  nothing re-checks it now (lava is covered by the GameTest `lavaKeepsVanillaMovement`).
- §12.16: the GameTests helper list lacks `respawn(player, keepEverything)` (plan and code have it, compat.md
  documents it), and the class list lacks `frame/SuitRestrictions`, `frame/EntryHints`, `frame/DebugEnergy` and
  `client/EnergyLine`.
- Everything else in §12 that 3c-1 builds matches the code (checked: §12.3 tick order, record, clock rule, jumps,
  transitions, beds as amended by T15; §12.4 sync rule, indicators, client_rules; §12.5; §12.6 formulas; §12.7 and
  §12.8 guards and milli rounding; §12.9 including messages and limits; §12.10 interval, map, order, slots; §12.13
  energy line as amended by T22, messages, sounds table; §12.14; §12.15 probe and in-world list). The one design gap,
  as opposed to divergence, is FC-1.
- Fix: one sentence in §12.15 naming m3c_shots.sh and what it leaves to the PC checklist; the four names in §12.16.
  Tasks: T24 (with T15, T16, T21, T22, T23 for the names).

## Design §12 against the code (the list asked for)

1. §12.15, client vero: recognition scripts not imported; replaced by m3c_shots.sh without the bubble column, sprint
   on the bottom, air and lava checks (FC-6).
2. §12.16: `respawn` helper and four new classes not listed (FC-6).
3. §12.4: "una riga di log una volta sola" for an out-of-range client rule; the code writes it once per name per
   process (ClientRules.java:90). Same intent; no change needed.
4. §12.9 with §12.10: the design does not say what a start at the defaults does to saved cell charges (FC-1).

## Deferred minors of area C

| Id | Verdict | Reason |
|---|---|---|
| T1-F1 | leave | still true: GameTests, SlotGuard, ResourceLocation, Player are imported (GameTestKit.java:8,25,50,59) and only named in comments; no build or behaviour effect |
| T1-F2 | leave | GameTestKit.java:80 still says "parti seguenti"; the four new classes belong to this part; comment only |
| T1-F3 | leave | fillPool Javadoc (GameTestKit.java:777) still omits "chi chiama tiene bordo e pavimento dentro la struttura"; every caller complies |
| T2-F1 | leave | headlines still say "fuori elenco" (run_gametests.sh:47,51; client_shot.sh:477; now also m3c_shots.sh:123); the detail lines name exact-count failures |
| T8-F2 | obsolete | fixed in 177fc4d (CREATE_NEW temporary, `defaultsFileFollowsTheUmask`) |
| T8-F3 | obsolete | fixed in 5389f2b (`otherUpperBoundsFallBackToDefaults`) |
| T8-R1-B1 | leave | ruled; its real cost runs through FC-1, whose fix covers it |
| T24-F1 | fix | compat.md:292 still gives a direction already done since Task 8; one phrase in the file FC-4 edits anyway |
| T5-F3 | obsolete | compat.md GameTests "Condizione" now covers readRegistry |
| T11-F1 | obsolete | the write methods now read update, mutate, changeWorn, clear, then clientIndicators and onSynced |
| T12-F1 | obsolete | Net "Uso" now says the real client cannot tell client_rules apart and names the JUnit tests that prove it |
| T14-F2 | obsolete | now "un tick fermo in cui il consumo resta sotto 1 RE non manda nulla" |
| T19-F3 | obsolete | the Verifica bullet now records the Task 24 m3c_shots runs with the real handlers |
| T22-R1-B2 | obsolete | §12.13 now describes the two-line top-left layout; no '·' left |
| T3-F2 | obsolete | concerns the brief only; design §12.14 has the five-argument signature and WornState's Javadoc is current |
| T15 (plan 927-928) | leave | ruled: the plan keeps what was asked; design §12.3 and the code agree |
| T17-F3 | leave | ruled by the Process ruling; FC-4 only asks the docs to stop implying a creative default |
| T10-F1 | leave | build.gradle.kts:106 still summarizes only the size and drawing mixins; comment, no behaviour (area A's task, build file is mine) |

## Recommendations

- A JUnit test that loads both language files and asserts the same keys in the same order and the same `%s` count
  per key: the plan requires it and nothing enforces it; a missing Italian key shows the raw key to players.
- A JUnit or GameTest that `init()` turns each `ConfigFile.Result` problem into one WARN: today no test notices if
  the wiring stops logging and an admin's broken file falls back in silence.
- The probe test (CarryOverGameTests.java:114-140) registers a PLAYER_PRE listener before an assertion that can
  throw; move the registration after `player.hurt(...)` or into the try, so a failing run does not leak it.

## Declined to judge

- Whether "una sola capacità" (§12.10) is the right product rule: design choice; FC-1 reports only its interaction
  with the config fallback.
- The T8 rulings themselves (decimal or duplicated key drops the whole file): ruled; reopened only for the FC-1 cost.
- The energy line in the top-left corner (ruled T22-F1) and the overruled plan-header sentences (bottom-right line,
  creative mock, server-only bed message): ruled.
- ConfigValidation clamping a [0, 1] field to its bound instead of the default (milestone 2a rule): I flag only the
  README wording (FC-3).
- Real-client and dedicated-server results: not re-run (forbidden here); I relied on the task reports and rulings.
- The 3c-2 parts of §12 (charger, recipes, data generator, PC checklist): outside 3c-1.
- Other areas' test bodies beyond what Review Focus points 4-6 and my area needed.

## Assessment

**Ready to merge? With fixes.**

**Reasoning:** Area C is in good shape: the config never rewrites or crashes, the tools count exactly, the test
seams stay test-only and the documents cover every new seam. Two cross-task items should be settled first: FC-1, a
silent permanent loss of saved cell charge after one start at the default config (Task 20's load clamp meeting Task
8's whole-file fallback), and FC-2, the 3a/3b PC checklists that the merged jars now contradict in six steps.
