#!/usr/bin/env python3
"""Genera nello stile A (Officina) la texture della schermata della stazione, quella dei disegni dell'HUD, gli angoli
della vignetta del casco e l'icona del kit di riparazione (design M4, par. 5.3, 5.8, 5.12; parte 4d). Anteprime per il
proprietario: niente di questo e' integrato finche' non risponde, e il gioco disegna ancora schermata e HUD con i
colori pieni.

Uso, dalla radice del repository:
    python3 tools/art/gui.py --out <dir>
        scrive <dir>/textures/gui/station.png (512x256: lo sfondo di 316x232 in (0, 0) e i disegni dei widget),
        <dir>/textures/gui/hud.png (256x128: piastre, sagome, barra, avviso, icona della cella),
        <dir>/textures/gui/hud_vignette.png (144x80: i quattro angoli 72x40 al loro posto) e
        <dir>/textures/item/repair_kit.png (16x16)
    python3 tools/art/gui.py --map
        stampa dove sta ogni disegno nelle due texture (u, v, larghezza, altezza)

`--out` e' obbligatorio: il generatore non scrive mai nei sorgenti del mod (la vignetta segnaposto la scrive ancora
tools/gen_placeholders.py, e tools/check_art.sh la confronta). La geometria e i posti nelle texture sono in
gui_layout.py, i disegni in gui_officina.py.

Stesso input, stessi byte: semi dai nomi delle tele (paint.HashRng), solo interi, PNG senza metadati (exo.write_png:
IHDR, IDAT, IEND, zlib 9). Esce con 1, senza scrivere nulla, se un controllo non passa: i disegni stanno nella texture
e non si sovrappongono, fuori da loro la texture e' trasparente, lo sfondo e' opaco; i colori della legenda sono
quelli esatti; nessun texel e' "magenta" per tools/client/arm_measure.sh; la vignetta rispetta la regola del design
(0,42 di opacita' nel pixel d'angolo e mai di piu', zero sui lati opposti di ogni angolo, riflessi solo in alto).
"""
import argparse
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import numpy as np  # noqa: E402

import gui_layout as L  # noqa: E402
import gui_officina as A  # noqa: E402
from exo import write_png  # noqa: E402

OUTPUTS = (('textures/gui/station.png', 'schermata'), ('textures/gui/hud.png', 'hud'),
           ('textures/gui/hud_vignette.png', 'vignetta'), ('textures/item/repair_kit.png', 'kit'))


def build():
    return {'schermata': A.screen_sheet(), 'hud': A.hud_sheet(), 'vignetta': A.vignette(), 'kit': A.kit_icon()}


# --- controlli --------------------------------------------------------------------------------------------------------
def magenta(img):
    """I texel visibili "magenta" per arm_measure.sh: r > 0,2, b > 0,2, g < 0,45 r e g < 0,45 b."""
    r, g, b, a = (img[..., i].astype(int) for i in range(4))
    return (a > 0) & (r > 51) & (b > 51) & (g * 100 < 45 * r) & (g * 100 < 45 * b)


def check_atlas(name, img, atlas, size, problems):
    W, H = size
    if img.shape != (H, W, 4):
        problems.append(f'{name}: {img.shape[1]}x{img.shape[0]} invece di {W}x{H}')
        return
    used = np.zeros((H, W), bool)
    for key, (u, v, w, h) in atlas.items():
        if u < 0 or v < 0 or u + w > W or v + h > H:
            problems.append(f'{name}: {key} fuori dalla texture')
            continue
        if used[v:v + h, u:u + w].any():
            problems.append(f'{name}: {key} si sovrappone a un altro disegno')
        used[v:v + h, u:u + w] = True
    if img[~used, 3].any():
        problems.append(f'{name}: texel non trasparenti fuori dai disegni')


def crop(img, rect):
    u, v, w, h = rect
    return img[v:v + h, u:u + w]


def exact(problems, what, pixels, colour):
    px = np.asarray(pixels)
    if not (px[..., :3] == np.array(colour, np.uint8)).all() or not (px[..., 3] == 255).all():
        problems.append(f'{what}: non e\' il colore esatto {colour}')


def check(out):
    problems = []
    st, hud, vig, kit = out['schermata'], out['hud'], out['vignetta'], out['kit']
    check_atlas('station.png', st, L.STATION_ATLAS, L.STATION_TEX, problems)
    check_atlas('hud.png', hud, L.HUD_ATLAS, L.HUD_TEX, problems)
    if (crop(st, L.STATION_ATLAS['fondo'])[..., 3] != 255).any():
        problems.append('station.png: lo sfondo non e\' tutto opaco')
    # legenda della stazione: il bordo di 1 pixel dei posti e il riempimento della barra della durata
    for state, colour in L.CONDITIONS + (('vuoto', L.UNMOUNTED_OUTLINE), ('cella', L.CELL_RED)):
        p = crop(st, L.STATION_ATLAS['posto.' + state])
        exact(problems, f'bordo di posto.{state}', np.concatenate([p[0], p[-1], p[:, 0], p[:, -1]]), colour)
    for state, colour in L.CONDITIONS + (('cella', L.CELL_RED),):
        bar = crop(st, L.STATION_ATLAS['durata.' + state])
        exact(problems, f'durata.{state}', bar[1:], colour)
        exact(problems, f'durata.{state}, prima riga', bar[:1], L.lighter(colour))
    exact(problems, 'scelta', np.concatenate([crop(st, L.STATION_ATLAS['scelta'])[0]]), L.WHITE)
    # legenda dell'HUD: sagome, riempimenti della barra, avvisi
    for state, colour in L.CONDITIONS:
        fig = crop(hud, L.HUD_ATLAS['sagoma.' + state])
        for slot_name, (x0, y0, x1, y1) in L.SIL_PIECES.items():
            piece = fig[y0:y1, x0:x1]
            exact(problems, f'sagoma.{state}.{slot_name}, centro', piece[(y1 - y0) // 2, (x1 - x0) // 2], colour)
            exact(problems, f'sagoma.{state}.{slot_name}, contorno', piece[-1, (x1 - x0) // 2], L.third(colour))
            opaque = piece[piece[..., 3] > 0]
            allowed = {tuple(colour), tuple(L.third(colour))}
            if any(tuple(int(c) for c in px[:3]) not in allowed for px in opaque):
                problems.append(f'sagoma.{state}.{slot_name}: un colore fuori dalla legenda')
    absent = crop(hud, L.HUD_ATLAS['sagoma.assente'])
    for slot_name, (x0, y0, x1, y1) in L.SIL_PIECES.items():
        piece = absent[y0:y1, x0:x1]
        centre = piece[(y1 - y0) // 2, (x1 - x0) // 2]
        if tuple(int(c) for c in centre) != (0, 0, 0, L.UNMOUNTED_FILL[3]):
            problems.append(f'sagoma.assente.{slot_name}: il fondo non e\' {L.UNMOUNTED_FILL}')
        exact(problems, f'sagoma.assente.{slot_name}, contorno', piece[-1, (x1 - x0) // 2], L.UNMOUNTED_OUTLINE)
    for name, colour in L.ENERGY:
        fill = crop(hud, L.HUD_ATLAS['barra.' + name])
        exact(problems, f'barra.{name}', fill[1:], colour)
        exact(problems, f'barra.{name}, prima riga', fill[:1], L.lighter(colour))
    for name, colour in (('rossa', L.RED), ('grigia', L.GREY)):
        w = crop(hud, L.HUD_ATLAS['avviso.' + name])
        exact(problems, f'avviso.{name}', w[7], colour)
    # nessun magenta
    for name, img in (('station.png', st), ('hud.png', hud), ('hud_vignette.png', vig), ('repair_kit.png', kit)):
        n = int(magenta(img).sum())
        if n:
            problems.append(f'{name}: {n} texel "magenta" per arm_measure.sh')
    # vignetta
    W, H = L.VIGNETTE_TEX
    if vig.shape != (H, W, 4):
        problems.append(f'hud_vignette.png: {vig.shape[1]}x{vig.shape[0]} invece di {W}x{H}')
    else:
        alpha = vig[..., 3].astype(int)
        if alpha.max() > L.VIGNETTE_MAX_ALPHA:
            problems.append(f'hud_vignette.png: opacita\' {alpha.max()} oltre {L.VIGNETTE_MAX_ALPHA} (0,42)')
        for x, y in ((0, 0), (W - 1, 0), (0, H - 1), (W - 1, H - 1)):
            if alpha[y, x] != L.VIGNETTE_MAX_ALPHA:
                problems.append(f'hud_vignette.png: pixel d\'angolo ({x}, {y}) con opacita\' {alpha[y, x]}')
        vw, vh = L.VIGNETTE_W, L.VIGNETTE_H
        if alpha[:, vw - 1:vw + 1].any() or alpha[vh - 1:vh + 1, :].any():
            problems.append('hud_vignette.png: opacita\' non zero sui lati opposti degli angoli')
        bottom = vig[vh:, :, :3].astype(int).max(axis=2)
        if (bottom > max(A.GLASS)).any():
            problems.append('hud_vignette.png: un riflesso negli angoli in basso')
        top = vig[:vh, :, :3].astype(int).max(axis=2)
        if not (top > max(A.GLASS)).any():
            problems.append('hud_vignette.png: nessun riflesso negli angoli in alto')
    if kit.shape != (16, 16, 4):
        problems.append(f'repair_kit.png: {kit.shape[1]}x{kit.shape[0]} invece di 16x16')
    return problems


def print_map():
    for title, atlas, size in (('textures/gui/station.png', L.STATION_ATLAS, L.STATION_TEX),
                               ('textures/gui/hud.png', L.HUD_ATLAS, L.HUD_TEX)):
        print(f'{title} ({size[0]}x{size[1]}): nome u v larghezza altezza')
        for key, (u, v, w, h) in atlas.items():
            print(f'  {key:28s} {u:4d} {v:4d} {w:4d} {h:4d}')
    print('pezzi della sagoma in textures/gui/hud.png (stato.pezzo u v larghezza altezza):')
    for st in L.SILHOUETTE_STATES:
        for slot_name in L.SIL_PIECES:
            u, v, w, h = L.silhouette_piece(st, slot_name)
            print(f'  {st + "." + slot_name:28s} {u:4d} {v:4d} {w:4d} {h:4d}')


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument('--out', help='cartella delle risorse (la struttura di assets/redstone_exoframes)')
    ap.add_argument('--map', action='store_true', help='stampa dove sta ogni disegno nelle texture')
    args = ap.parse_args()
    if args.map:
        print_map()
        return
    if not args.out:
        ap.error('serve --out (oppure --map)')
    try:
        out = build()
    except ValueError as e:
        print('FAIL:', e, file=sys.stderr)
        sys.exit(1)
    problems = check(out)
    if problems:
        print('FAIL:\n' + '\n'.join(problems), file=sys.stderr)
        sys.exit(1)
    written = []
    for rel, key in OUTPUTS:
        path = os.path.join(os.path.normpath(args.out), rel)
        write_png(path, out[key])
        written.append(path)
    print(f'{len(written)} file: ' + ', '.join(rel for rel, _ in OUTPUTS))


if __name__ == '__main__':
    main()
