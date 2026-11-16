### Task 5: Un file senza sezioni vale come rovinato (passo b); salvataggio all'uscita ristretto

**Files** (Java da `src/main/java/com/toxineren/exoframes/`):
- Modify: `compat/impl/FrameRegistryFormat.java` (`read`, dal Task 4), `compat/impl/FrameStateSavedData.java` (`load`, `get`, `createEmpty`, `saveWithPlayersIfNeeded`, commento della classe), `gametest/CarryOverGameTests.java`, `tools/log_allowlist.txt`, `docs/compat.md` (`:78`, `:258`, elenco "Se il file del registro non si legge" `:260-264`)
- Test: `src/test/java/.../compat/impl/FrameStateSavedDataTest.java`, `src/test/java/.../compat/impl/RegistryFileTest.java`, `src/test/java/.../compat/impl/StackTagsTest.java`

**Interfaces:**
- Consumes (dal Task 4): `FrameRegistryFormat.read(CompoundTag, BiFunction<…>, FrameRegistry) : Outcome` con `enum Outcome { READ, READ_ONLY }`; `FrameStateSavedData.load(CompoundTag, BiFunction<…>)`, `get(DimensionDataStorage, Path)`, `readOnly()`, `logRefusedChange()`, `saveFile(Path)` (privato: scrive solo se `isDirty()`, poi `saveTo` → `markSaved()`), `dataFolder(MinecraftServer)`, `NAME`; `GameTests.readRegistry(MinecraftServer, Path) : Optional<CompoundTag>` (vuoto se il registro letto è in sola lettura), `registryFolder`, `registryChangeUnsaved`; `PlayerEvent.PLAYER_QUIT`; `ServerLevel.noSave` (campo pubblico, quello di `/save-off`), `ServerLevel.getRaids() : Raids` (un `SavedData` dell'overworld); `NbtIo.writeCompressed(CompoundTag, OutputStream)` (uguale sulle due versioni).
- Produces:
  - `FrameRegistryFormat.Outcome` guadagna `WITHOUT_SECTIONS`; in `read`, prima del controllo dello schema: `if (!tag.contains(NbtKeys.SCHEMA) && !tag.contains(FRAMES) && !tag.contains(NbtKeys.WORN) && !tag.contains(NbtKeys.PLACED)) return Outcome.WITHOUT_SECTIONS;` (nessuna riga di log, nulla letto: decide la presenza delle chiavi, di qualunque tipo, non il loro essere vuote; `orphaned` da sola non basta, perché `write` scrive sempre `schema`).
  - `FrameStateSavedData`: campo `private boolean withoutSections`; in `load` per `WITHOUT_SECTIONS` `data.readOnlyRoot = tag.copy(); data.withoutSections = true;` (sola lettura come uno schema più recente: nulla si scrive, `isDirty()` sempre falso); in `get(DimensionDataStorage, Path)`, su tutte e due le uscite (dato letto dal gioco, dato di `afterUnreadFile`), `return result.reportedWithoutSections(folder.resolve(NAME + ".dat"));` con
    ```java
    private FrameStateSavedData reportedWithoutSections(Path file) {
        if (withoutSections && unreadableFile == null) {
            unreadableFile = file;
            RedstoneExoframes.LOGGER.error("Frame registry file {} has no schema and no sections: frames are "
                    + "read-only this session", file);
        }
        return this;
    }
    ```
    (la riga ERROR una volta per dato, con il percorso, che `load` non conosce; da lì il dato è "rovinato" come un file illeggibile: `write` dà il marcatore `unreadable`, `logRefusedChange` dice il file da ripristinare). Il file non si tocca e non si cerca la copia `_old`.
  - `static FrameStateSavedData createEmpty()` (package-private, solo per i test): `return new FrameStateSavedData();`, il registro vuoto e scrivibile che prende il posto di `load(new CompoundTag(), …)`.
  - `saveWithPlayersIfNeeded(MinecraftServer server)`: stesse guardie di oggi (`!structuralUnsaved || readOnly()` → nulla), poi `server.getPlayerList().saveAll();` e `data.saveFile(dataFolder(server).resolve(NAME + ".dat"));` (il solo file del registro, non più `server.overworld().getDataStorage().save()`). Non guarda `/save-off`; il motivo nel commento della classe e del metodo: il gioco scrive comunque il file del giocatore che esce (`PlayerList.remove` chiama `save(player)` senza guardare `/save-off`), e rispettarlo separerebbe la coppia file del giocatore e registro su cui si regge §4.5; i file di tutti i giocatori, non solo di chi esce, perché il segno strutturale è unico per il registro e può venire dall'operazione di un altro giocatore.
  - `tools/log_allowlist.txt`, dopo la riga `:40`: `gametest | =1 | Frame registry file {TESTREG} has no schema and no sections: frames are read-only this session | registryFileWithoutSectionsIsReadOnly`.
- Testi: nessuna chiave nuova. `docs/compat.md`: `:78` ("All'uscita di un giocatore … si salvano tutti i giocatori e poi i dati dell'overworld, registro compreso" → "… si salvano tutti i giocatori e poi il solo file del registro, anche con `/save-off`, perché il gioco scrive comunque il file di chi esce e i due file vanno sul disco insieme"); `:258`, dopo la frase sullo schema più recente: "Un file che si legge ma non ha `schema` né alcuna delle sezioni `frames`, `worn`, `placed` non è un registro: è rovinato, e il dato resta in sola lettura per la sessione come per un file illeggibile"; nell'elenco di `:260-264` una voce: "ERROR `Frame registry file <file> has no schema and no sections: frames are read-only this session` (senza la riga di vanilla prima: il gioco il file lo legge): il file c'è ma non è un registro (scritto da altro, o svuotato); come la riga precedente, per tutta la sessione nulla si legge né si scrive: fermare il server, rimettere il file da un backup e riavviare". La voce di design §11 ("Un file del registro valido ma senza sezioni") è già chiusa da §12.14: si corregge solo se il testo contraddice il codice.

- [ ] **Step 1: test JUnit** — in `FrameStateSavedDataTest`:
  - `fileWithoutSectionsIsReadOnly`: `CompoundTag root = new CompoundTag(); root.putInt("foo", 1); assertReadOnlyAndUnchanged(root);` (l'aiuto di oggi a `:617`: ogni punto di modifica rifiutato, nulla da salvare, la scrittura riproduce il tag) e `assertTrue(FrameStateSavedData.load(new CompoundTag(), FrameStateCodec::read).readOnly());` (il tag che il gioco passa per un file senza `data`);
  - `schemaOneFileIsStillARegistry`: `{frames: {}}` → `assertFalse(data.readOnly())`, `assertNotNull(data.getOrCreateWorn(GOOD, FrameStateSavedDataTest::state))`; e, con lo stesso esito, `{placed: {}}` e `{schema: 2}`;
  - `createEmptyIsWritable`: `FrameStateSavedData data = FrameStateSavedData.createEmpty(); assertFalse(data.readOnly()); assertFalse(data.isDirty()); assertNotNull(data.getOrCreateWorn(GOOD, FrameStateSavedDataTest::state)); assertTrue(data.structuralUnsaved()); assertEquals(FrameStateSavedData.CURRENT_SCHEMA, data.write(new CompoundTag()).getInt(NbtKeys.SCHEMA));`.
  
  E i test di oggi che caricano un tag senza schema né sezioni come registro vuoto: `FrameStateSavedDataTest:125` (`empty()`) e `RegistryFileTest:50` (`empty()`) → `return FrameStateSavedData.createEmpty();`; `StackTagsTest:47` → `FrameStateSavedData.createEmpty();` e `StackTagsTest:52` → `FrameStateSavedData.load(schemaOnly, FrameStateCodec::read);` con `CompoundTag schemaOnly = new CompoundTag(); schemaOnly.putInt(NbtKeys.SCHEMA, 2);` (il test continua a provare "creare o caricare" un registro: il costruttore svuota `StackTags` in tutti e due i casi); `FrameStateSavedDataTest` `orphanedListIsWrittenBackUnchanged` (`:420`): al tag `odd` si aggiunge `odd.putInt(NbtKeys.SCHEMA, 2);`, perché `{orphaned: "not a list"}` da solo diventa un file senza sezioni.
- [ ] **Step 2: test in mondo** — in `CarryOverGameTests`, in coda a `generate()`:
  - `quitSaveIgnoresSaveOff`:
    ```java
    ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
    MinecraftServer server = helper.getLevel().getServer();
    GameTests tests = Compat.gameTests();
    Map<ServerLevel, Boolean> noSave = new HashMap<>();
    server.getAllLevels().forEach(level -> noSave.put(level, level.noSave));
    ExoframeEntity frame = null;
    try {
        server.getAllLevels().forEach(level -> level.noSave = true);                  // come /save-off
        frame = placeFrame(helper, ownedBy(player), new Vec3(2.5, 1.0, 2.5), 0f);
        helper.assertTrue(tests.registryChangeUnsaved(server), "placing a frame is not an unsaved change");
        Raids raids = server.overworld().getRaids();
        raids.setDirty();
        PlayerEvent.PLAYER_QUIT.invoker().quit(player);
        helper.assertTrue(!tests.registryChangeUnsaved(server), "the registry was not saved under /save-off");
        Optional<CompoundTag> onDisk = tests.readRegistry(server, tests.registryFolder(server));
        helper.assertTrue(onDisk.isPresent() && onDisk.get().getCompound(NbtKeys.PLACED)
                .contains(frame.frameId().toString()), "the placed frame is not in the registry on disk");
        helper.assertTrue(raids.isDirty(), "the quit save wrote the other saved data of the overworld");
    } finally {
        noSave.forEach((level, value) -> level.noSave = value);
        if (frame != null) {
            removeFrame(server, frame);
        }
    }
    helper.succeed();
    ```
    (tutto nello stesso tick, quindi nessun altro test salva i dati dell'overworld fra `setDirty` e l'asserzione; prima del task l'ultima asserzione fallisce).
  - `registryFileWithoutSectionsIsReadOnly`: in `Path copy = Files.createTempDirectory("exoframes-registry-test")` il file `redstone_exoframes_frames.dat` scritto nel formato del gioco (`CompoundTag root` con `data` = `{foo: 1}` e `DataVersion` = `SharedConstants.getCurrentVersion().getDataVersion().getVersion()`, con `try (OutputStream out = Files.newOutputStream(file)) { NbtIo.writeCompressed(root, out); }`), `byte[] before = Files.readAllBytes(file)`; `tests.readRegistry(server, copy)` → `isEmpty()` (sola lettura); `Arrays.equals(before, Files.readAllBytes(file))`; `try (Stream<Path> files = Files.list(copy)) { helper.assertTrue(files.count() == 1, "extra files next to the registry"); }` (nessuna copia messa da parte, nessun `_old` scritto); `deleteTree(copy)` nel `finally`. La riga ERROR che produce è quella aggiunta all'elenco.
- [ ] **Step 3: Run** `./gradlew --console=plain :1.21.1-neoforge:test --tests '*FrameStateSavedDataTest' --tests '*RegistryFileTest' --tests '*StackTagsTest'` — Expected: FAIL (non compila: `createEmpty` non esiste). `bash tools/run_gametests.sh 1.21.1-neoforge` — Expected: FAIL su `quitSaveIgnoresSaveOff` (`the quit save wrote the other saved data of the overworld`) e su `registryFileWithoutSectionsIsReadOnly` (il registro di `{foo: 1}` si legge come vuoto e scrivibile).
- [ ] **Step 4: implementare** `WITHOUT_SECTIONS`, `withoutSections`, `reportedWithoutSections`, `createEmpty`, `saveWithPlayersIfNeeded` e la riga dell'elenco come in "Produces"; nel commento della classe di `FrameStateSavedData` la regola del file senza sezioni (accanto a quella del file illeggibile) e il salvataggio all'uscita ristretto con il suo motivo (sostituisce "il gioco salva i file di tutti i giocatori e poi i dati dell'overworld, registro compreso" e "Questo salvataggio non guarda /save-off").
- [ ] **Step 5: Run** gli stessi JUnit — PASS; `bash tools/run_gametests.sh` — Expected: quattro righe `OK <variante>: 100 test obbligatori passati, nessun avviso fuori elenco` (la riga ERROR nuova esattamente una volta per variante; `quitSavesTheRegistryWithThePlayers` invariato e verde).
- [ ] **Step 6:** `docs/compat.md` come in "Interfaces".
- [ ] **Step 7: verifiche di fine task** — build e `collectJar` delle quattro varianti → `BUILD SUCCESSFUL` (JUnit delle quattro compresi); `bash tools/check_sources.sh` → `OK sources`; `bash tools/check_jars.sh` → `OK jars`; GameTest già allo Step 5.
- [ ] **Step 8: commit** `compat: un file senza sezioni non vale come registro; salvataggio all'uscita limitato ai giocatori e al registro`.

