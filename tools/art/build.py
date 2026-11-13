#!/usr/bin/env python3
"""Rigenera la grafica del telaio: geometria, atlante, maschera luminosa e animazioni (vuote).

Uso, dalla radice del repository:
    python3 tools/art/build.py                 scrive in src/main/resources/assets/redstone_exoframes/
    python3 tools/art/build.py --out <dir>     scrive in <dir>/geo, <dir>/textures/entity, <dir>/animations
    python3 tools/art/build.py --out <dir> --designs frame
                                               solo i design indicati (separati da virgole; predefinito: tutti)
    python3 tools/art/build.py --out <dir> --debug-position
                                               atlante di prova: ogni texel ha il colore della sua posizione 3D

Stesso input, stessi byte: semi fissi, nessuna data, ordine stabile, PNG senza metadati. Esce con 1, senza scrivere
nulla, se il modello non passa i controlli (facce complanari, scatole troppo vicine al pilota o fuori dall'ingombro)
o se uno stato visivo di un pezzo non e' piu' scuro del precedente (paint.check_wear).
"""
import argparse
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

from designs import load_all  # noqa: E402
from exo import Model, dumps_json, write_png, write_text  # noqa: E402
from paint import check_wear, paint_model  # noqa: E402

DEFAULT_OUT = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'redstone_exoframes')
GEO = os.path.join('geo', 'exoframe.geo.json')
TEXTURE = os.path.join('textures', 'entity', 'exoframe.png')
GLOWMASK = os.path.join('textures', 'entity', 'exoframe_glowmask.png')
ANIMATION = os.path.join('animations', 'exoframe.animation.json')


def build_model(designs=None):
    model = Model('exoframe')
    for design in load_all(designs):
        design.build(model)
    model.pack()
    return model


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument('--out', default=DEFAULT_OUT, help='cartella delle risorse del mod (assets/redstone_exoframes)')
    ap.add_argument('--designs', default=None, help='design da caricare, separati da virgole (predefinito: tutti)')
    ap.add_argument('--debug-position', action='store_true', help='atlante di prova per il visore')
    args = ap.parse_args()

    model = build_model(args.designs.split(',') if args.designs else None)
    problems = model.check()
    if problems:
        for p in problems:
            print('FAIL:', p, file=sys.stderr)
        sys.exit(1)

    atlas, glowmask = paint_model(model, debug_position=args.debug_position)
    problems = [] if args.debug_position else check_wear(model, atlas)
    if problems:
        for p in problems:
            print('FAIL:', p, file=sys.stderr)
        sys.exit(1)

    out = os.path.normpath(args.out)
    write_text(os.path.join(out, GEO), dumps_json(model.to_geo()))
    write_png(os.path.join(out, TEXTURE), atlas)
    write_png(os.path.join(out, GLOWMASK), glowmask)
    write_text(os.path.join(out, ANIMATION), dumps_json({'format_version': '1.8.0', 'animations': {}}))
    lo, hi = model.bbox()
    used = ', '.join(f'{k} {v}' for k, v in model.used.items() if v)
    print(f'{len(model.boxes)} scatole, ingombro {lo} -> {hi}, righe usate: {used}')


if __name__ == '__main__':
    main()
