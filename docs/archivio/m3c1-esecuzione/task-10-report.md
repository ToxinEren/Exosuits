# Task 10 - I due mixin nuovi con gestori vuoti: rapporto

Stato: DONE. BASE 81238da509da549e6227e9eca37d55827a808db1, HEAD d1e6bccc42a6d535cf609d182a4293ed7d8de0f7.

## Che cosa ho fatto

- `frame/SuitDamage` (public final): `public static void onArmorHurt(Player, DamageSource, float)`, vuoto; Javadoc che dice che oggi non fa nulla.
- `frame/SuitMovement` (public final): `affectedByFluids`, `pushedByFluid`, `sprintAllowed` restituiscono `vanilla`; `afterUpdateSwimming` non fa nulla; Javadoc che dice che oggi si lascia il valore del gioco.
- `mixin/PlayerSuitMixin` (`@Mixin(Player.class)`, abstract, package-private): quattro agganci della tabella del brief, gestori `exoframes$wearPieces` (HEAD), `exoframes$fluidMovement` (RETURN, cancellabile), `exoframes$fluidPush` (RETURN, cancellabile), `exoframes$noSwimming` (TAIL), ciascuno una sola chiamata.
- `mixin/client/LocalPlayerSprintMixin` (`@Mixin(LocalPlayer.class)`): `hasEnoughFoodToStartSprinting()Z`, RETURN, cancellabile, `exoframes$sprintGate` -> `SuitMovement.sprintAllowed((LocalPlayer) (Object) this, cir.getReturnValueZ())`.
- Le due `redstone_exoframes.mixins.json`: `mixins` = grandezza della versione + `PlayerSuitMixin`; `client` = le quattro classi, nell'ordine del brief.
- `tools/check_jars.sh` (e2): commento riscritto, `SPRINT`, `CLIENT` di quattro, `TARGETS` e `MOJANG` con i valori esatti del brief, controllo della sezione comune con `sorted(...)`.
- `docs/compat.md`: punto nuovo **Usura, acqua e scatto** nella sezione Mixin; punti **Configurazione**, **Nessun refmap**, **Verifica** aggiornati; "Cuciture client" (`i quattro mixin client`); riga nuova nella tabella API.

Bersagli verificati con javap sui jar di Minecraft in cache di Loom (vanilla 1.20.1 e 1.21.1, Forge 1.20.1 patchato SRG, NeoForge 21.1.266 patchato): `Player` dichiara `protected void hurtArmor(DamageSource, float)`, `public boolean isAffectedByFluids()`, `public void updateSwimming()`, `public boolean isPushedByFluid()` (Forge: `m_6472_`, `m_6129_`, `m_5844_`, `m_6063_`); `LocalPlayer` dichiara `private boolean hasEnoughFoodToStartSprinting()` (Forge `m_255269_`).

## Prove

TDD: il task non ha test nuovi; la parte "rossa" e' il controllo dei jar dello Step 3.

- Step 2, build delle quattro varianti con `collectJar` (comando di Global Constraints): `BUILD SUCCESSFUL in 22s`, `grep -ci "cannot remap"` = 0.
- Step 3, `bash tools/check_jars.sh` prima di aggiornarlo (RED atteso), uscita 1, esattamente le righe previste:
  ```
  FAIL: redstone-exoframes-fabric-0.1.0+mc1.20.1.jar: redstone_exoframes.mixins.json: sezione mixins ['PlayerSize120Mixin', 'PlayerSuitMixin'] invece di ['PlayerSize120Mixin'] (solo la grandezza reale della versione)
  FAIL: redstone-exoframes-fabric-0.1.0+mc1.20.1.jar: redstone_exoframes.mixins.json: sezione client [... 'client.LocalPlayerSprintMixin' ...] invece di [...]
  (stesse due righe per fabric 1.21.1, forge 1.20.1, neoforge 1.21.1)
  FAIL: redstone-exoframes-neoforge-0.1.0+mc1.21.1.jar: com/toxineren/exoframes/mixin/PlayerSuitMixin.class: nomi Mojang attesi nelle annotazioni (NeoForge 1.21.1 gira con i nomi Mojang)
  FAIL: redstone-exoframes-neoforge-0.1.0+mc1.21.1.jar: com/toxineren/exoframes/mixin/client/LocalPlayerSprintMixin.class: nomi Mojang attesi nelle annotazioni (NeoForge 1.21.1 gira con i nomi Mojang)
  ```
- Step 5 (GREEN): `bash tools/check_jars.sh` -> `OK jars`, uscita 0. Prova negativa in `mktemp -d` con il jar NeoForge riscritto (`"ExtraMixin"` aggiunto a `mixins`, il resto copiato uguale), uscita 1:
  ```
  FAIL: redstone-exoframes-neoforge-0.1.0+mc1.21.1.jar: redstone_exoframes.mixins.json: sezione mixins ['ExtraMixin', 'PlayerSize121Mixin', 'PlayerSuitMixin'] invece di ['PlayerSize121Mixin', 'PlayerSuitMixin'] (la grandezza reale della versione e gli agganci comuni su Player)
  FAIL: redstone-exoframes-neoforge-0.1.0+mc1.21.1.jar: classe com/toxineren/exoframes/mixin/ExtraMixin.class elencata in redstone_exoframes.mixins.json ma assente dal jar
  ```
  poi `rm -rf "$d"`.
- Step 6: `bash tools/check_sources.sh` -> `OK sources`. `bash tools/run_gametests.sh` (quattro varianti), uscita 0:
  ```
  OK 1.20.1-forge: 114 test obbligatori passati, nessun avviso fuori elenco
  OK 1.20.1-fabric: 114 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-fabric: 114 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-neoforge: 114 test obbligatori passati, nessun avviso fuori elenco
  ```
  (114 = numero del Task 9, come da tabella); nessuna riga `InvalidInjection`/`Mixin apply` nel log.
- Step 7, client vero, una variante alla volta, `EXO_ACCEPT_EULA=true bash tools/client/client_shot.sh <v> <scratch>/shot-<v> "execute as Exo run exoframe debug wear rottame"`: uscita 0 su 1.21.1-neoforge (92 s), 1.21.1-fabric (64 s), 1.20.1-fabric (66 s), 1.20.1-forge (72 s); ultima riga di ogni esecuzione `log: nessun avviso o errore del mod fuori dall'elenco degli avvisi voluti`; `log-check.txt` di 0 byte in tutte e quattro; `Wearing the scrap exoframe` in ogni `rcon.log`; zero `InvalidInjection|Mixin apply` in `client.log` e `server.log`; negli scatti (`quarter.png` di NeoForge, `back.png` delle altre tre) la tuta indossata. Il client che entra nel mondo crea il `LocalPlayer`, quindi il mixin client si applica (con `defaultRequire 1` un bersaglio mancato fermerebbe il client).
- Step 8, server dedicato (`./gradlew --console=plain :<v>:runServer`, `eula=true` gia' presente in `versions/<v>/run/eula.txt`, stdin da una fifo: atteso `Done (`, poi `stop`), uno alla volta: `Done (` dopo 15 s (1.21.1-neoforge), 14 s (1.21.1-fabric), 15 s (1.20.1-fabric), 17 s (1.20.1-forge), Gradle uscito con 0; `python3 -I tools/check_log.py client --variant <v> versions/<v>/run/logs/latest.log` -> nessuna uscita, uscita 0, sulle quattro; `grep -cE "InvalidInjectionException|Mixin apply"` = 0 sulle quattro; `No data fixer registered for exoframe` una volta su 1.21.1-fabric e 1.20.1-fabric, zero su 1.20.1-forge (e il controllo dell'ambito client passa anche su NeoForge).
- Step 10: dopo lo Step 6 sono cambiati solo i documenti; rifatti comunque `OK sources` e `OK jars` prima del commit. Nessun processo di gioco, client, server o Xvfb rimasto aperto (resta solo il demone di Gradle inattivo).

## File cambiati

- nuovi: `src/main/java/com/toxineren/exoframes/frame/SuitDamage.java`, `src/main/java/com/toxineren/exoframes/frame/SuitMovement.java`, `src/main/java/com/toxineren/exoframes/mixin/PlayerSuitMixin.java`, `src/main/java/com/toxineren/exoframes/mixin/client/LocalPlayerSprintMixin.java`
- modificati: `src/main/resources-mc1.20.1/redstone_exoframes.mixins.json`, `src/main/resources-mc1.21.1/redstone_exoframes.mixins.json`, `tools/check_jars.sh`, `docs/compat.md`

Commit: `d1e6bcc mixin: agganci comuni su Player e scatto del giocatore locale, ancora senza effetto`.

## Autorevisione

- Nessun gestore ne' metodo chiamato porta il nome di un bersaglio; nei jar Fabric e Forge nessun marcatore MOJANG scatta (lo prova `OK jars`, che fallirebbe con "nomi Mojang nelle annotazioni, non rimappati").
- Le righe oltre 120 caratteri in `check_jars.sh` (10, 236, 240, 253, 259) erano gia' li', non le ho toccate; le mie restano entro 120.
- `docs/compat.md`: oltre ai punti del brief, la frase d'apertura della sezione Mixin diceva "di due tipi": ora "di tre tipi", per restare vera con il punto nuovo.

## Scelte

- Posizione del punto **Usura, acqua e scatto** in `docs/compat.md`: subito prima di **Configurazione**, dopo i dettagli dei due mixin del disegno, per non spezzare il blocco grandezza/dettagli del disegno gia' esistente.
- Frase d'apertura della sezione Mixin portata da "due tipi" a "tre tipi" (altrimenti falsa con il punto nuovo).
- Nel messaggio di `check_jars.sh` per la sezione comune ho spezzato la f-string su due righe (stesso testo stampato) per restare entro 120 colonne.
- Server dedicato dello Step 8 guidato da uno script nella cartella temporanea della sessione (fifo su stdin di Gradle, attesa di `Done (`, `stop`), non nel repository.
- La prova che `PlayerSuitMixin` si applica sul server dedicato con un giocatore vero viene dagli avvii di `client_shot.sh` (server dedicato locale con il giocatore entrato e la tuta indossata, `server.log` pulito) e dai server dei test in mondo; l'avvio del solo server dello Step 8 prova che il mixin client non si carica e che nulla si rompe all'avvio.

## Scostamenti dal brief

Nessuno nel codice e nei valori. Unica aggiunta: "di due tipi" -> "di tre tipi" in `docs/compat.md` (vedi Scelte).

## Dubbi

Nessuno.
