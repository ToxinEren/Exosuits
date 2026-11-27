# Final fix report, part 4a-2
1. Design section 15 added (design.md:660): A12 analysis, deferred minors, plan-check line. All Italian, no process IDs.
2. Design 5.14 table (line 328): `reinforced` added with note it is an existing schema-2 key read by M3.
3. docs/compat.md:256-257: clause on the covered action bar, pointing to design section 15.
4. StationMenu.java:278 Javadoc now says true for every selection and for a changed frame.
5. Comments: GameTests.messagesTo (operatorPlayer too), SuitDamage:36, SuitTick:53, StationView.withSelection (client prediction via StationMenu.select), StationMenuGameTests header reflowed.
Verification: compileJava 1.20.1-forge + 1.21.1-neoforge BUILD SUCCESSFUL; check_sources OK sources; no added Java line over 120; one `## 15` heading.
Files: docs/compat.md, design spec, GameTests, StationView, SuitDamage, SuitTick, StationMenuGameTests, StationMenu.
