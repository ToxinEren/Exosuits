# Final fix report (parte 4d)

Commit: e90e46d1a611f748996b94297261869b00beb108 on m4-stazione

## git diff (commit)
```
 docs/test-in-gioco/m4a1-banco.md | 2 +-
 tools/art/README.md              | 4 ++++
 2 files changed, 5 insertions(+), 1 deletion(-)
```
```diff
diff --git a/docs/test-in-gioco/m4a1-banco.md b/docs/test-in-gioco/m4a1-banco.md
index dce2467..c311fd5 100644
--- a/docs/test-in-gioco/m4a1-banco.md
+++ b/docs/test-in-gioco/m4a1-banco.md
@@ -10,7 +10,7 @@ I testi sono scritti in italiano, come li mostra il gioco in italiano, e fra par
 
 > **Con i jar di dopo l'unione della parte 4c** (quelli del branch `m4-stazione` o di `main` dopo l'unione) i passi restano come sono, e vale quanto segue. In "Cosa non c'è ancora" la voce "L'HUD nuovo" non vale più: l'HUD della tuta c'è, in basso nell'angolo opposto alla mano principale al posto della riga dell'energia in alto a sinistra, e si prova con la checklist `docs/test-in-gioco/m4c-hud.md`.
 
-> **Con i jar di dopo la parte 4d** (quelli del branch `m4-stazione` o di `main` dopo l'unione) i passi restano come sono, e vale quanto segue. La stazione ha la sua grafica definitiva, quella scelta dal proprietario il 27 novembre 2026 (direzione A, "Officina", come in `docs/superpowers/specs/anteprime/2026-11-27-m4d/a-officina-mondo.png`): in "Cosa non c'è ancora" la voce "La grafica vera" non vale più per il blocco e per l'icona dell'oggetto (la schermata resta a colori pieni). Il blocco è un banco basso di acciaio scuro rivettato con il piano di bronzo; sul fronte della metà principale (a sinistra, dove hai cliccato) uno schermo di redstone incassato, tre manopole di rame sotto lo schermo e una leva alla sua destra; sul fronte della laterale (a destra) due cassetti con le maniglie di rame; dietro un tubo di rame per tutta la lunghezza, con sotto una griglia scura dietro la laterale e uno sportello dietro la principale; su ciascuno dei due estremi un tubo di rame verticale, più vicino al fronte; sul piano, a metà, il giunto fra le due metà. Quindi in "Cosa c'è da vedere" (voce "Il blocco"), al passo 4 e nella descrizione di `stazione-posata` in "Già verificato" il fronte di sinistra è quello con lo schermo, le manopole e la leva, e quello di destra quello con i due cassetti, al posto della spia rossa con il cassetto e della griglia; al passo 3 l'icona dell'oggetto (nel libro delle ricette, nell'inventario e nella barra) è il banco visto di tre quarti, con il piano di bronzo, lo schermo rosso a sinistra e i cassetti a destra. Il blocco non fa luce: il rosso dello schermo è pittura. Gli scatti di riferimento in `m4a1-screenshot/` hanno gli stessi nomi e sono rifatti con la grafica nuova (`stazione-posata` la mostra da vicino; negli altri quattro si vede attorno alla schermata). Da guardare, e da annotare con uno screenshot su ogni variante: la stazione posata guardando a nord, a est, a sud e a ovest è sempre la stessa, girata, con la principale dove hai cliccato e i cassetti alla tua destra (nel cloud è provato solo il fronte a nord); nessuna faccia mancante o viola e nera, nessuna texture stirata o capovolta, girandoci intorno; le pareti degli incassi rivolte verso terra o verso l'altra metà sono più scure, linee d'ombra sottili, e non sono un errore; se sulla stazione lampeggia qualche puntino chiaro mentre ti muovi (lungo gli spigoli degli incassi o fra le due metà), annotalo con uno screenshot (nel cloud non se ne vedono); rompendola, le particelle vengono dalla texture degli estremi (acciaio scuro, con un po' di bronzo e di rame).
+> **Con i jar di dopo la parte 4d** (quelli del branch `m4-stazione` o di `main` dopo l'unione) i passi restano come sono, e vale quanto segue. La stazione ha la sua grafica definitiva, quella scelta dal proprietario il 27 novembre 2026 (direzione A, "Officina", come in `docs/superpowers/specs/anteprime/2026-11-27-m4d/a-officina-mondo.png`): in "Cosa non c'è ancora" la voce "La grafica vera" non vale più per il blocco e per l'icona dell'oggetto (la schermata resta a colori pieni). Il blocco è un banco basso di acciaio scuro rivettato con il piano di bronzo; sul fronte della metà principale (a sinistra, dove hai cliccato) uno schermo di redstone incassato, tre manopole di rame sotto lo schermo e una leva alla sua destra; sul fronte della laterale (a destra) due cassetti con le maniglie di rame; dietro un tubo di rame per tutta la lunghezza, con sotto una griglia scura dietro la laterale e uno sportello dietro la principale; su ciascuno dei due estremi un tubo di rame verticale, più vicino al fronte; sul piano, a metà, il giunto fra le due metà. Quindi in "Cosa c'è da vedere" (voce "Il blocco"), al passo 4 e nella descrizione di `stazione-posata` in "Già verificato" il fronte di sinistra è quello con lo schermo, le manopole e la leva, e quello di destra quello con i due cassetti, al posto della spia rossa con il cassetto e della griglia; al passo 3 l'icona dell'oggetto (nel libro delle ricette, nell'inventario e nella barra) è il banco visto di tre quarti, con il piano di bronzo, lo schermo rosso a sinistra e i cassetti a destra. Il blocco non fa luce: il rosso dello schermo è pittura. Gli scatti di riferimento in `m4a1-screenshot/` hanno gli stessi nomi e sono rifatti con la grafica nuova (`stazione-posata` la mostra da vicino; negli altri quattro si vede attorno alla schermata). Da guardare, e da annotare con uno screenshot su ogni variante: la stazione posata guardando a nord, a est, a sud e a ovest è sempre la stessa, girata, con la principale dove hai cliccato e i cassetti alla tua destra (nel cloud i quattro lati sono provati su 1.21.1-neoforge, il nord sulle quattro varianti); nessuna faccia mancante o viola e nera, nessuna texture stirata o capovolta, girandoci intorno; le pareti degli incassi rivolte verso terra o verso l'altra metà sono più scure, linee d'ombra sottili, e non sono un errore; se sulla stazione lampeggia qualche puntino chiaro mentre ti muovi (lungo gli spigoli degli incassi, fra le due metà o lungo la base, dove tocca il terreno), annotalo con uno screenshot (nel cloud non se ne vedono); rompendola, le particelle vengono dalla texture degli estremi (acciaio scuro, con un po' di bronzo e di rame).
 
 ## Cosa c'è da vedere
 
diff --git a/tools/art/README.md b/tools/art/README.md
index 1285f87..32c6f6e 100644
--- a/tools/art/README.md
+++ b/tools/art/README.md
@@ -399,6 +399,10 @@ nulla (le facce dei vicini sono nascoste dal cubo pieno), quindi si vedrebbe il
 prima sugli spigoli delle tasche. `_conform` taglia i rettangoli lungo la perpendicolare per ogni vertice che sta
 dentro un loro lato, finche' non ne resta nessuno (direzione A: 648 + 432 facce invece di 84 + 66, con gli stessi
 texel), e `station.py` non scrive nulla se `bench.t_junctions` ne trova uno nei modelli finiti.
+I tagli finiscono sugli spigoli esterni del blocco, quindi lungo la base restano vertici dentro il lato della faccia
+di sopra del blocco accanto, a terra (direzione A: 38, prima 2): vertici a T fra due blocchi, che il generatore non
+puo' togliere. Nel cloud nessun puntino chiaro lungo la base in 49 viste (27 novembre 2026); la checklist della
+4a-1 chiede di guardarla sul PC.
 
 **Anteprime** (solo locali, non nella CI; Playwright come sopra):
 
```

## check_sources.sh
```
OK sources
```
