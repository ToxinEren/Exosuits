# Redstone Exoframes

Mod di power armor originale per Minecraft Java: un telaio in cui si entra, sei pezzi con durabilità propria, celle di redstone, stazione di manutenzione, moduli e cinque tier. Nomi, estetica e lore sono originali.

Target: **1.21.1** (NeoForge + Fabric) e **1.20.1** (Forge + Fabric) da un unico sorgente.

## Stato

In sviluppo. Completate le milestone 1 (setup del progetto: quattro jar e CI), 2a (core in Java puro, con test unitari eseguiti su tutte e quattro le varianti) e 2b (interfacce `compat/` per 1.20.1 e 1.21.1, vedi `docs/compat.md`). I test in gioco sono ancora da fare (checklist in `docs/test-in-gioco/m1-setup.md`). La prossima è la milestone 3: entità telaio, tier Rottame, modelli GeckoLib. La roadmap e le decisioni sono in `docs/superpowers/specs/`:

- `claude-doc-2026-11-04-redstone-exoframes.md`: il documento di design di gioco (autorità per le meccaniche).
- `2026-11-12-redstone-exoframes-design.md`: il design operativo (approcci, architettura, decisioni, milestone).

## Come si compila

Serve JDK 21 per eseguire Gradle (il wrapper e' Gradle 9.9.0). La prima build scarica molto e puo' richiedere 10-15 minuti. Su Windows usa `gradlew.bat` al posto di `./gradlew`.

```
./gradlew --console=plain clean :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar
```

I quattro jar finiscono in `build/libs/all/`. Con Stonecutter 0.9.8 non esiste un task `chiseledBuild`: si usa il comando esplicito qui sopra.

Controlli: `bash tools/check_sources.sh` (sorgenti) e `bash tools/check_jars.sh [cartella]` (jar, cartella di default `build/libs/all`). Il controllo dei jar verifica anche che non contengano dati personali (percorsi del computer, e-mail, metadati di immagini e suoni): serve `python3`. Per cercare anche parole tue, per esempio nome e cognome o il nome utente del PC, elencale nella variabile d'ambiente `EXOFRAMES_PRIVATE_WORDS` separate da virgola, senza mai scriverle nel repository.

La CI (`.github/workflows/build.yml`) compila i quattro jar a ogni push e li carica come artefatto `redstone-exoframes-jars`: `gh run download --repo ToxinEren/Exosuits -n redstone-exoframes-jars` (oppure dalla scheda Actions).

## Varianti

| Variante | Minecraft | Loader | Architectury API | GeckoLib | Java per giocare |
|----------|-----------|--------|------------------|----------|------------------|
| 1.20.1-forge | 1.20.1 | Forge 47.4.29 | 9.2.14 (forge) | 4.8.4 (forge-1.20.1) | 17 |
| 1.20.1-fabric | 1.20.1 | Fabric Loader 0.20.1 + Fabric API 0.92.12+1.20.1 | 9.2.14 (fabric) | 4.8.4 (fabric-1.20.1) | 17 |
| 1.21.1-neoforge | 1.21.1 | NeoForge 21.1.266 | 13.0.11 (neoforge) | 4.9.3 (neoforge-1.21.1) | 21 |
| 1.21.1-fabric | 1.21.1 | Fabric Loader 0.20.1 + Fabric API 0.116.18+1.21.1 | 13.0.11 (fabric) | 4.9.3 (fabric-1.21.1) | 21 |

Jar prodotti: `redstone-exoframes-fabric-0.1.0+mc1.20.1.jar`, `redstone-exoframes-fabric-0.1.0+mc1.21.1.jar`, `redstone-exoframes-forge-0.1.0+mc1.20.1.jar`, `redstone-exoframes-neoforge-0.1.0+mc1.21.1.jar`. Le quattro varianti vanno provate in istanze separate.

## Requisiti

- JDK 21 per compilare (Gradle).
- Java 17 per giocare la 1.20.1, Java 21 per giocare la 1.21.1.

## Documenti

- Checklist di avvio in gioco (milestone 1): `docs/test-in-gioco/m1-setup.md`
- Design: `docs/superpowers/specs/`
- Piani di implementazione (M1, M2a, M2b): `docs/superpowers/plans/`
- Audit del codice di partenza: `docs/audit/mjolnir-armor-audit.md`
- Interfacce di compatibilità tra versioni e loader: `docs/compat.md`

## Licenza

Tutti i diritti riservati, per il codice e per gli asset (vedi `LICENSE`). È consentito giocare con il mod, mostrarlo in video e includerlo nei modpack distribuiti tramite CurseForge e Modrinth. Non è consentito modificarlo, ripubblicarlo altrove o riusarne codice e asset senza permesso scritto dell'autore.

## Crediti

Parte del codice dei sistemi di armatura, rendering GeckoLib e HUD deriva dal mod Mjolnir Armor dello stesso autore, riscritto per Architectury e per i quattro target.
