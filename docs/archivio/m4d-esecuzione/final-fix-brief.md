# Part 4d: the review's fix wave (two documentation sentences)

Repository /home/user/exosuits, branch `m4-stazione` at `70cbd3d`. The review of the station work is in `review.md` (same folder): ready to merge, with three Minor findings. This wave fixes Minor 1 and Minor 3, two documentation sentences, with the exact text below. Do nothing else: no other file, no Gradle, no client.

## Edit 1: `tools/art/README.md` (Minor 1)

In the paragraph that begins `**Facce senza vertici a T.**`, after its last line, which ends with `ne trova uno nei modelli finiti.`, add these four lines, continuing the same paragraph (no blank line before them). The file writes accents as apostrophes (`puo'`), and so do these lines:

```
I tagli finiscono sugli spigoli esterni del blocco, quindi lungo la base restano vertici dentro il lato della faccia
di sopra del blocco accanto, a terra (direzione A: 38, prima 2): vertici a T fra due blocchi, che il generatore non
puo' togliere. Nel cloud nessun puntino chiaro lungo la base in 49 viste (27 novembre 2026); la checklist della
4a-1 chiede di guardarla sul PC.
```

## Edit 2: `docs/test-in-gioco/m4a1-banco.md`, line 13 (Minor 3 and Minor 1)

Two replacements inside that line (the note that begins `> **Con i jar di dopo la parte 4d**`), each string occurring once:

- `(nel cloud è provato solo il fronte a nord)` becomes `(nel cloud i quattro lati sono provati su 1.21.1-neoforge, il nord sulle quattro varianti)`
- `(lungo gli spigoli degli incassi o fra le due metà)` becomes `(lungo gli spigoli degli incassi, fra le due metà o lungo la base, dove tocca il terreno)`

## Check, commit, report

`git diff --stat` shows exactly these two files; `bash tools/check_sources.sh` → `OK sources`. One commit, signed by the environment (never `-c commit.gpgsign=false`, never `--no-verify`, never set a git identity), message:

```
docs: la stazione, i lati provati nel cloud e i puntini lungo la base (revisione della parte 4d)

(prima riga di attribuzione dell'ambiente)
(seconda riga di attribuzione dell'ambiente)
```

Do not push. You do not dispatch subagents. Write a short report to `/home/user/exosuits/.superpowers/sdd/m4d-anteprime/final-fix-report.md` (the two diffs and the check output) and return only: status, commit hash, the `check_sources.sh` output.
