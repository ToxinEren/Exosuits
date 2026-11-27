#!/usr/bin/env python3
"""Genera la grafica della stazione di manutenzione (design M4, par. 5.1) in una delle direzioni proposte al
proprietario (parte 4d): i modelli di blocco delle due meta', le loro texture e l'icona piatta dell'oggetto.

Uso, dalla radice del repository:
    python3 tools/art/station.py --list
    python3 tools/art/station.py --direction <nome> --out <dir>
        scrive <dir>/models/block/station_main.json e station_side.json, <dir>/textures/block/station_*.png
        (station_top, station_side, station_front_main, station_front_side, station_back_main,
        station_back_side, station_bottom) e <dir>/textures/item/station.png

`--out` e' obbligatorio. La stazione del mod e' la direzione scelta dal proprietario il 27 novembre 2026, A
(`officina`): `--direction officina --out src/main/resources/assets/redstone_exoframes` la rigenera, e
tools/check_art.sh la rigenera in una cartella temporanea e la confronta con i file committati. Il blockstate e il
modello dell'oggetto, scritti a mano, usano gli stessi nomi dei modelli e l'icona `item/station`.

Stesso input, stessi byte: nessuna data, nessun numero casuale fuori da paint.HashRng con semi dai nomi, solo interi,
JSON con chiavi in ordine fisso (exo.dumps_json), PNG senza metadati (exo.write_png: IHDR, IDAT, IEND, zlib 9).
Esce con 1, senza scrivere nulla, se la geometria non passa i controlli di bench.Bench.check o se le facce dei modelli
hanno un vertice a T (bench.t_junctions: un vertice di una faccia dentro un lato di un'altra).
"""
import argparse
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

from bench import Bench, t_junctions  # noqa: E402
from exo import dumps_json, write_png, write_text  # noqa: E402
from station_designs import DIRECTIONS, load  # noqa: E402


def build(name):
    """Costruisce la direzione: (bench, modelli, texture, icona); lancia ValueError se la geometria non va o se le
    facce hanno un vertice a T."""
    design = load(name)
    bench = Bench()
    design.build(bench)
    problems = bench.check()
    if problems:
        raise ValueError('\n'.join(problems))
    models = bench.models()
    problems = t_junctions(models)
    if problems:
        more = f'\n... e altri {len(problems) - 10}' if len(problems) > 10 else ''
        raise ValueError(f'{len(problems)} vertici a T nelle facce dei modelli:\n' + '\n'.join(problems[:10]) + more)
    icon = design.icon()
    if icon.shape != (16, 16, 4):
        raise ValueError(f'icona {icon.shape[:2]} invece di 16x16')
    return bench, models, bench.textures(), icon


def write(out, models, textures, icon):
    written = []
    for half in ('main', 'side'):
        path = os.path.join(out, 'models', 'block', f'station_{half}.json')
        write_text(path, dumps_json(models[half]))
        written.append(path)
    for name in sorted(textures):
        path = os.path.join(out, 'textures', 'block', name + '.png')
        write_png(path, textures[name])
        written.append(path)
    path = os.path.join(out, 'textures', 'item', 'station.png')
    write_png(path, icon)
    written.append(path)
    return written


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument('--direction', help='nome della direzione (vedi --list)')
    ap.add_argument('--out', help='cartella delle risorse (la struttura di assets/redstone_exoframes)')
    ap.add_argument('--list', action='store_true', help='elenca le direzioni')
    args = ap.parse_args()
    if args.list:
        for name in DIRECTIONS:
            d = load(name)
            print(f'{name}: {d.LETTER}, {d.TITLE}: {d.IDEA}')
        return
    if not args.direction or not args.out:
        ap.error('servono --direction e --out (oppure --list)')
    if args.direction not in DIRECTIONS:
        ap.error(f'direzione sconosciuta: {args.direction} (direzioni: {", ".join(DIRECTIONS)})')
    try:
        _, models, textures, icon = build(args.direction)
    except ValueError as e:
        print('FAIL:', e, file=sys.stderr)
        sys.exit(1)
    written = write(os.path.normpath(args.out), models, textures, icon)
    elements = {h: len(models[h]['elements']) for h in ('main', 'side')}
    print(f'{args.direction}: {len(written)} file, elementi {elements["main"]} + {elements["side"]}')


if __name__ == '__main__':
    main()
