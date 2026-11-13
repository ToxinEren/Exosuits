#!/usr/bin/env python3
"""Arti e testa in movimento: per quale angolo una gamba, un braccio o la testa comincia a entrare nel busto.

Uso, dalla radice del repository:
    python3 tools/art/swing.py                    gambe, telaio nudo e tuta armata (pezzi nello stato 0), da 1 a 81 gradi
    python3 tools/art/swing.py --part braccia     braccia
    python3 tools/art/swing.py --part testa       testa: rotazione a destra e a sinistra e in su e in giu'
    python3 tools/art/swing.py --part tutto       le tre parti
    python3 tools/art/swing.py --max 60 --all     fino a 60 gradi, ogni scatola del busto con il suo primo angolo

Gambe e braccia ruotano attorno all'asse x del loro perno (exo.TOP_PIVOT), come nell'animazione del passo: in avanti
la mano o il piede va verso -z, indietro verso +z. La testa ruota attorno al suo perno: attorno a y per guardare a
destra o a sinistra (di chi indossa la tuta) e attorno a x per guardare in su o in giu'. Per ogni grado si ruotano
tutte le scatole della parte (telaio e, per la tuta armata, il pezzo dello slot) e si cerca la prima che si
compenetra con una scatola del busto (telaio e pezzo Torso): compenetrazione esatta tra una scatola ruotata e una
dritta (teorema dell'asse separatore), contatti senza volume esclusi. Le coppie che si compenetrano gia' a riposo (il
sedile e la vita attorno alle prese delle cosce, l'elmo incassato nel collare) si elencano a parte e non contano.
Stampa, per ogni parte e direzione, il primo angolo di un contatto nuovo con le coppie di scatole (con --all: per ogni
scatola del busto il suo primo angolo). Le gambe non possono toccarsi tra loro: ruotano attorno a x, quindi x non
cambia, e i loro bordi interni stanno ai due lati del piano di mezzo.

Ampiezze del gioco base (HumanoidModel.setupAnim, da javap su 1.20.1): gamba xRot = cos(...) * 1.4 * a, braccio
xRot = cos(...) * 2.0 * a * 0.5, con a = min(4 * blocchi per tick, 1) dopo lo smorzamento dell'animazione del passo:
camminando circa 69 gradi per le gambe e 49 per le braccia, correndo circa 80 e 57. La testa segue lo sguardo: in su e
in giu' fino a 90 gradi, a destra e a sinistra fino a circa 50 oltre il corpo. Il massimo predefinito, 81, copre il
passo di corsa. Non e' un controllo della build: serve quando si cambiano gambe, braccia, testa, bacino o schiena.
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

# parte -> [(osso di primo livello, asse di rotazione, (nome dei gradi negativi, nome dei gradi positivi))]
# Attorno a x i gradi positivi portano verso +z cio' che sta sotto il perno (piede o mano indietro) e verso -z cio'
# che sta sopra (la testa guarda in giu'); attorno a y i gradi positivi girano il viso (-z) verso -x, cioe' verso la
# destra di chi indossa la tuta.
PARTS = {
    'gambe': [(leg, 'x', ('avanti', 'indietro')) for leg in ('armorRightLeg', 'armorLeftLeg')],
    'braccia': [(arm, 'x', ('avanti', 'indietro')) for arm in ('armorRightArm', 'armorLeftArm')],
    'testa': [('armorHead', 'y', ('sinistra', 'destra')), ('armorHead', 'x', ('su', 'giu'))],
}


def _overlap(c, axes, half, blo, bhi):
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


def _rotate(b, pivot, axis, deg):
    """Centro e assi della scatola b ruotata di deg gradi attorno all'asse dato passante per il perno."""
    a = math.radians(deg)
    ca, sa = math.cos(a), math.sin(a)
    d = [(b.lo[k] + b.hi[k]) / 2 - pivot[k] for k in range(3)]
    if axis == 'x':
        c = (pivot[0] + d[0], pivot[1] + d[1] * ca + d[2] * sa, pivot[2] - d[1] * sa + d[2] * ca)
        axes = ((1, 0, 0), (0, ca, -sa), (0, sa, ca))
    else:
        c = (pivot[0] + d[0] * ca + d[2] * sa, pivot[1] + d[1], pivot[2] - d[0] * sa + d[2] * ca)
        axes = ((ca, 0, -sa), (0, 1, 0), (sa, 0, ca))
    return c, axes


def first_contacts(model, part, armored, max_deg, axis='x', names=('avanti', 'indietro')):
    """Per l'osso di primo livello part ruotato attorno ad axis: le coppie (scatola della parte, scatola del busto)
    gia' compenetrate a riposo e, per ciascuna delle due direzioni, il primo angolo di ogni coppia nuova."""
    pivot = TOP_PIVOT[part]

    def shown(b):
        return b.bone.region == 'frame' or (armored and b.bone.state == 0)

    moving = [b for b in model.boxes if b.bone.parent == part and shown(b)]
    body = [b for b in model.boxes if b.bone.parent == 'armorBody' and shown(b)]

    def overlapping(deg):
        hits = set()
        for b in moving:
            c, axes = _rotate(b, pivot, axis, deg)
            half = tuple((b.hi[k] - b.lo[k]) / 2 for k in range(3))
            r = math.sqrt(sum(h * h for h in half))
            for o in body:
                if any(c[k] + r < o.lo[k] or c[k] - r > o.hi[k] for k in range(3)):
                    continue
                if _overlap(c, axes, half, o.lo, o.hi):
                    hits.add((b.name, o.name))
        return hits

    rest = overlapping(0)
    result = {'a riposo': {pair: 0 for pair in rest}}
    for sign, name in zip((-1, 1), names):
        first = {}
        for deg in range(1, max_deg + 1):
            for pair in overlapping(sign * deg) - rest:
                first.setdefault(pair, deg)
        result[name] = first
    return result


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument('--part', choices=sorted(PARTS) + ['tutto'], default='gambe',
                    help='parte che si muove (predefinito gambe)')
    ap.add_argument('--max', type=int, default=81, help='angolo massimo in gradi (predefinito 81)')
    ap.add_argument('--all', action='store_true', help='ogni coppia con il suo primo angolo, non solo il primo')
    args = ap.parse_args()
    model = build_model()
    parts = [p for name in ('gambe', 'braccia', 'testa') if args.part in (name, 'tutto') for p in PARTS[name]]
    for armored, label in ((False, 'telaio nudo'), (True, 'tuta armata (stato 0)')):
        for bone, axis, names in parts:
            moving = f'{bone} ({axis})'
            for direction, first in first_contacts(model, bone, armored, args.max, axis, names).items():
                if not first:
                    print(f'{label:22} {moving:18} {direction:8} nessun contatto fino a {args.max} gradi')
                    continue
                # per scatola del busto, il primo angolo e le scatole della parte che la toccano a quell'angolo
                by_body = {}
                for (a, b), deg in first.items():
                    if b not in by_body or deg < by_body[b][0]:
                        by_body[b] = (deg, [a])
                    elif deg == by_body[b][0]:
                        by_body[b][1].append(a)
                rows = sorted(by_body.items(), key=lambda kv: (kv[1][0], kv[0]))
                if not args.all:
                    rows = [r for r in rows if r[1][0] == rows[0][1][0]]
                for body, (deg, hit) in rows:
                    print(f'{label:22} {moving:18} {direction:8} {deg:3} gradi: {body} <- {", ".join(sorted(hit))}')


if __name__ == '__main__':
    main()
