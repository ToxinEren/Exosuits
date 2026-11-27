### Spec Compliance
- ✅ Spec compliant. All listed files have their hunks; ExoframeItem (FULL, TEXT_KEYS, pickedUp, useOn, tooltip), FrameInteractions pick-up (state read before removePlaced, item given after), GameTestKit new useExoframeItem, ChargerGameTests TEXT_KEYS, registration, lang words match plan-common line 160, docs/compat.md paragraph, five tests per brief.
- ✅ Controller notes honored: no third record accessor (uses existing recordState); pickedUp Javadoc cites only design §5.11.
- ⚠️ Cannot verify from diff: 120-col lines/counts (report claims check_sources OK, 256 in-world, 407 JUnit); client-side safety of Compat.itemData().frame(stack) in tooltip on all variants (Task 4 real client).

### Strengths
- Item built from pre-removal state, handed only after removePlaced succeeds.
- Single owner condition; plain item verified with ItemStack.matches vs crafted.
- Tooltip test non-vacuous (owner-in-data precondition), checks keys, order, colours.
- try/finally cleanup everywhere; NO_ROOM==0 proves FULL precedes room check.
- Behavioural RED stub runs documented.

### Issues
#### Critical (Must Fix)
None.
#### Important (Should Fix)
None.
#### Minor (Nice to Have)
- ExoframeItem.useOn player-less path untested (not required by brief).
- FrameItemGameTests.java:778-796 cleanUp is a per-class helper; if other gametest classes have equivalents consider moving to GameTestKit later.
- NO_ROOM key not covered by language check (plan's TEXT_KEYS excludes it; pre-existing).

### Assessment
**Task quality:** Approved
**Reasoning:** Implements the brief and plan decisions faithfully with correct ordering and a single owner rule; tests assert real behaviour.
