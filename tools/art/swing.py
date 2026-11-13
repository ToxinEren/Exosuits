#!/usr/bin/env python3
"""Gambe in movimento: per quale angolo una gamba che oscilla comincia a entrare nel busto.

Uso, dalla radice del repository:
    python3 tools/art/swing.py               telaio nudo e tuta armata (pezzi nello stato 0), da 1 a 60 gradi
    python3 tools/art/swing.py --max 80      fino a 80 gradi

Le gambe ruotano attorno all'asse x del loro perno (exo.TOP_PIVOT), come nell'animazione del passo: in avanti il
piede va verso -z, indietro verso +z. Per ogni grado si ruotano tutte le scatole della gamba (telaio e, per la tuta
armata, i pezzi delle gambe) e si cerca la prima che si compenetra con una scatola del busto (telaio e pezzo Torso):
compenetrazione esatta tra una scatola ruotata e una dritta (teorema dell'asse separatore), contatti senza volume
esclusi. Le coppie che si compenetrano gia' a riposo (il sedile e la vita attorno alle prese delle cosce) si
elencano a parte e non contano. Stampa, per ogni gamba e direzione, il primo angolo di un contatto nuovo con le
coppie di scatole (con --all: per ogni scatola del busto il suo primo angolo). Le gambe non
possono toccarsi tra loro: ruotano attorno a x, quindi x non cambia, e i loro bordi interni stanno ai due lati del
piano di mezzo. Non e' un controllo della build: serve quando si cambiano gambe, bacino o schiena.
"""
import argparse
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

from build import build_model  # noqa: E402
from exo import TOP_PIVOT  # noqa: E402

EPS = 1e-6


def _overlap(lo, hi, c, axes, half, blo, bhi):
    """True se la scatola orientata (centro c, assi, semilati) e la scatola dritta [blo, bhi] si compenetrano."""
    bc = [(blo[k] + bhi[k]) / 2 for k in range(3)]
    bh = [(bhi[k] - blo[k]) / 2 for k in range(3)]
    d = [bc[k] - c[k] for k in range(3)]
    world = ((1, 0, 0), (0, 1, 0), (0, 0, 1))
    tests = list(world) + list(axes)
    for a in axes:
        for w in world:
            cr = (a[1] * w[2] - a[2] * w[1], a[2] * w[0] - a[0] * w[2], a[0] * w[1] - a[1] * w[0])
            if sum(v * v for v in cr) > 1e-12:
                tests.append(cr)
    for t in tests:
        ra = sum(half[i] * abs(sum(axes[i][k] * t[k] for k in range(3))) for i in range(3))
        rb = sum(bh[k] * abs(t[k]) for k in range(3))
        if abs(sum(d[k] * t[k] for k in range(3))) >= ra + rb - EPS * (1 + abs(ra + rb)):
            return False
    return True


def first_contacts(model, leg, armored, max_deg):
    py, pz = TOP_PIVOT[leg][1], TOP_PIVOT[leg][2]

    def shown(b):
        return b.bone.region == 'frame' or (armored and b.bone.state == 0)

    legs = [b for b in model.boxes if b.bone.parent == leg and shown(b)]
    body = [b for b in model.boxes if b.bone.parent == 'armorBody' and shown(b)]

    def overlapping(deg):
        """Coppie (scatola della gamba, scatola del busto) che si compenetrano con la gamba ruotata di deg gradi
        (positivi: piede indietro, verso +z)."""
        a = math.radians(deg)
        ca, sa = math.cos(a), math.sin(a)
        hits = set()
        for b in legs:
            cy, cz = (b.lo[1] + b.hi[1]) / 2 - py, (b.lo[2] + b.hi[2]) / 2 - pz
            c = ((b.lo[0] + b.hi[0]) / 2, py + cy * ca + cz * sa, pz - cy * sa + cz * ca)
            axes = ((1, 0, 0), (0, ca, -sa), (0, sa, ca))
            half = tuple((b.hi[k] - b.lo[k]) / 2 for k in range(3))
            r = math.sqrt(sum(h * h for h in half))
            for o in body:
                if any(c[k] + r < o.lo[k] or c[k] - r > o.hi[k] for k in range(3)):
                    continue
                if _overlap(b.lo, b.hi, c, axes, half, o.lo, o.hi):
                    hits.add((b.name, o.name))
        return hits

    # coppie che si compenetrano gia' da fermi (il bacino attorno alle prese delle cosce): non sono contatti nuovi
    rest = overlapping(0)
    result = {'a riposo': {pair: 0 for pair in rest}}
    for sign, name in ((-1, 'avanti'), (1, 'indietro')):
        first = {}
        for deg in range(1, max_deg + 1):
            for pair in overlapping(sign * deg) - rest:
                first.setdefault(pair, deg)
        result[name] = first
    return result


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument('--max', type=int, default=60, help='angolo massimo in gradi (predefinito 60)')
    ap.add_argument('--all', action='store_true', help='ogni coppia con il suo primo angolo, non solo il primo')
    args = ap.parse_args()
    model = build_model()
    for armored, label in ((False, 'telaio nudo'), (True, 'tuta armata (stato 0)')):
        for leg in ('armorRightLeg', 'armorLeftLeg'):
            for direction, first in first_contacts(model, leg, armored, args.max).items():
                if not first:
                    print(f'{label:22} {leg:14} {direction:8} nessun contatto fino a {args.max} gradi')
                    continue
                # per scatola del busto, il primo angolo e le scatole della gamba che la toccano a quell'angolo
                by_body = {}
                for (a, b), deg in first.items():
                    if b not in by_body or deg < by_body[b][0]:
                        by_body[b] = (deg, [a])
                    elif deg == by_body[b][0]:
                        by_body[b][1].append(a)
                rows = sorted(by_body.items(), key=lambda kv: (kv[1][0], kv[0]))
                if not args.all:
                    rows = [r for r in rows if r[1][0] == rows[0][1][0]]
                for body, (deg, legs) in rows:
                    print(f'{label:22} {leg:14} {direction:8} {deg:3} gradi: {body} <- {", ".join(sorted(legs))}')


if __name__ == '__main__':
    main()
