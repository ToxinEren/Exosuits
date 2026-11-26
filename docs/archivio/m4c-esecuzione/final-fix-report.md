# Report della fix wave finale, parte 4c

- F1 `docs/test-in-gioco/m4c-hud.md` prep. E: controlla "Sottotitoli: No" (Opzioni... > Audio...) e "Mano principale: Destra" (Opzioni... > Aspetto...), annota e correggi se diversi; coerente con i passi 3 e 15.
- F2 `docs/test-in-gioco/m3c-giocarci.md:12`: "Passi 10 e 12", nuova riga dei tasti in italiano e in inglese (dai file lang).
- F3 `HudLayoutTest.java`: ONE_DIGIT_SPARE=12 (misurata), TWO_DIGIT_SPARE=18 (dedotta); asserzioni 53/47 e 47/41.
- F4 design: riga 274 (testo "Senza cella", grigio, da (2,20), sulla riga della barra al posto della percentuale; perche' "Nessuna cella" non entra), riga 376, §13 "Per la 4c".
- F5 `m4c-hud.md:19`: "nella parte dell'energia" con la nota sul mancino.
- F6 `README.md:28`: elenco di cio' che scrive gen_placeholders.py.
- F7 `HudModel.java` Javadoc di CELL_CHARGE: cita StationLayout.CELL_RED.
- F8 design §14 "Riporti della parte 4c".

Comandi: `./gradlew --console=plain :1.21.1-neoforge:test --tests '*HudLayoutTest'` BUILD SUCCESSFUL, XML: tests=7 failures=0; `bash tools/check_sources.sh` OK sources; grep 'Nessuna cella' nel design: solo righe 274 e 633 (spiegano perche' non entra); git status: solo i 6 file attesi.

Concerns: nessuno di rilievo.
