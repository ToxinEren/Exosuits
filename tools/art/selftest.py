#!/usr/bin/env python3
"""Prove negative dei controlli del generatore: python3 tools/art/selftest.py

Inietta un difetto alla volta in una copia del modello (costruita in memoria: non scrive nulla nel repository) e
pretende che i controlli lo segnalino. Esce con 1 se un difetto passa inosservato. La esegue tools/check_art.sh.

  1. facce complanari con la stessa normale, in tre varianti:
     a. la parte comune e' coperta solo da un pezzo di un altro slot (si vede quando quel pezzo non e' montato);
     b. la parte comune e' coperta solo da una decalcomania (il renderer la nasconde);
     c. una faccia "sepolta" con una striscia scoperta di 0,25 px su un lato lungo un numero dispari di quarti, che
        non deve sparire dall'atlante;
  2. scatola dentro la testa del pilota;
  3. scatola fuori dai limiti;
  4. coordinata fuori dalla griglia dei quarti di pixel;
  5. linea sottile fuori dalla sua scatola;
  6. stato visivo piu' chiaro del precedente;
  7. regione dell'atlante piena;
  8. faccia con la griglia fine diversa dalla sua versione a densita' normale;
  9. un osso di primo livello con i pezzi di due slot.

Le prove che dipendono dal disegno del mod (1a, 1b, 1c) verificano prima la loro premessa (che cosa copre la parte
iniettata): se il disegno cambia e la premessa non vale piu', la prova fallisce dicendolo, invece di passare senza
provare nulla.
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import paint  # noqa: E402
from build import build_model  # noqa: E402
from exo import Model  # noqa: E402

CASES = []


class Premise(Exception):
    """La premessa di una prova non vale piu' (il disegno e' cambiato): la prova non proverebbe nulla."""


def case(name):
    """Registra una prova; si eseguono in main(), non all'import."""
    def wrap(fn):
        CASES.append((name, fn))
        return fn
    return wrap


def run(fn):
    try:
        return fn()
    except Premise as e:
        return False, f'premessa non valida: {e}'
    except Exception as e:  # un'eccezione inattesa e' un fallimento della prova, non del controllo
        return False, f'eccezione inattesa: {type(e).__name__}: {e}'


def common_cells(m, a, b, face):
    """Centri delle celle da un quarto di pixel della parte comune delle facce `face` di a e b, appena fuori dal
    piano."""
    from exo import face_frame
    n = face_frame(face, a.lo, a.hi)[5]
    plane = [k for k in range(3) if not n[k]]
    return [p for p in m._cell_centres(a, face, 0.01) if all(b.lo[k] < p[k] < b.hi[k] for k in plane)]


def old_rule_cover(m, a, b, face, kind):
    """Premessa delle prove 1a/1b: con la regola di prima (qualunque scatola visibile insieme ad a e b copre) la parte
    comune sarebbe tutta coperta, e solo da scatole che la regola nuova non conta, di cui almeno una del tipo `kind`
    ('slot': pezzo di un altro slot, 'decal': decalcomania). Altrimenti solleva Premise."""
    cells = common_cells(m, a, b, face)
    if not cells:
        raise Premise(f'{a.name} e {b.name} non hanno una parte comune sulla faccia {face}')
    covers = set()
    for p in cells:
        cs = [c for c in m.boxes if c is not a and c is not b and m._together(c, a) and m._together(c, b)
              and m._inside(p, c.lo, c.hi)]
        if not cs:
            raise Premise(f'la cella {p} non e\' coperta da nulla: la prova non distingue le due regole')
        if any(m._always_with(c, a, b) for c in cs):
            raise Premise(f'la cella {p} e\' coperta da una scatola sempre presente: la prova non distingue le due '
                          f'regole')
        covers.update(cs)
    want = {'slot': lambda c: c.bone.state is not None, 'decal': lambda c: c.bone.region == 'decal'}[kind]
    if not any(want(c) for c in covers):
        raise Premise(f'nessuna delle scatole che coprono la parte comune e\' del tipo atteso ({kind}): '
                      f'{sorted(c.name for c in covers)}')
    return sorted(c.name for c in covers)


def real_model():
    """Il modello del mod, impacchettato (una copia nuova a ogni prova)."""
    return build_model()


def tiny_model(fine=True):
    """Modello minimo per le prove del pittore: un pezzo (elmo) con una piastra colorabile e una con linee sottili."""
    m = Model('selftest', fine=fine)
    m.bone('frameHead', 'armorHead', 'frame')
    m.box('frameHead', 'post', (-6, 30, -6), (-5, 31, -5), 'steel')
    m.piece_bones('Helmet', 'armorHead')
    from exo import piece
    m.box(piece('Helmet', 'p1'), 'plate', (-6, 26, -8), (-3, 29, -7.5), 'paint')
    m.box(piece('Helmet', 'p1'), 'slats', (3, 26, -8), (5, 27.5, -7.5), 'paint',
          lines=[('h', y, 3.25, 4.75) for y in (26.5, 27)])
    return m


@case('1a. striscia complanare coperta solo da un pezzo di un altro slot')
def coplanar_other_slot():
    m = real_model()
    # due scatole del telaio con le facce davanti (z 4.75) e dietro (z 5) complanari, sovrapposte in x -1..0: la parte
    # comune sta dentro la piastra della schiena del torso (back_plate), che col torso smontato non c'e'
    m.box('frameBody', 'selftest_a', (-2, 18, 4.75), (0, 20, 5), 'gun')
    m.box('frameBody', 'selftest_b', (-1, 18, 4.75), (1, 20, 5), 'gun')
    m.pack()
    a, b = (next(x for x in m.boxes if x.name == n) for n in ('selftest_a', 'selftest_b'))
    covers = old_rule_cover(m, a, b, 'north', 'slot')
    hits = [p for p in m.check() if 'selftest_a' in p and 'selftest_b' in p]
    return bool(hits), (hits[0] + f' (coperta solo da {", ".join(covers)})') if hits else 'nessuna segnalazione'


@case('1b. striscia complanare coperta solo da una decalcomania')
def coplanar_decal():
    m = real_model()
    # facce davanti (z -8) complanari, parte comune x -2.5..-2 dentro la decalcomania del torso (nascosta nel gioco);
    # le facce dietro stanno su piani diversi
    m.box('frameBody', 'selftest_d', (-3, 19, -8), (-2, 20, -7.75), 'gun')
    m.box('frameBody', 'selftest_e', (-2.5, 19, -8), (-1.5, 20, -7.5), 'gun')
    m.pack()
    a, b = (next(x for x in m.boxes if x.name == n) for n in ('selftest_d', 'selftest_e'))
    covers = old_rule_cover(m, a, b, 'north', 'decal')
    hits = [p + f' (coperta solo da {", ".join(covers)})' for p in m.check() if 'selftest_d' in p and 'selftest_e' in p]
    return bool(hits), hits[0] if hits else 'nessuna segnalazione'


@case('1c. faccia "sepolta" con una striscia scoperta di 0,25 px (lato di 0,75 px)')
def buried_odd_strip():
    m = real_model()
    # la faccia dietro (z 1) di X e' larga 0,75 px; C1 e C2 dello stesso osso la coprono tranne x 10,25..10,5
    m.box('frameBody', 'selftest_x', (10, 1, 0), (10.75, 2, 1), 'gun')
    m.box('frameBody', 'selftest_c1', (10, 1, 1), (10.25, 2, 1.5), 'gun')
    m.box('frameBody', 'selftest_c2', (10.5, 1, 1), (10.75, 2, 1.5), 'gun')
    m.pack()
    x = next(b for b in m.boxes if b.name == 'selftest_x')
    # premessa: i centri dei texel (la regola di prima) cadono tutti su parti coperte, una cella da un quarto no
    others = [c for c in m.boxes if c is not x and c.bone is x.bone]
    covered = lambda p: any(m._inside(p, c.lo, c.hi) for c in others)  # noqa: E731
    texel_centres = [(10 + (i + 0.5) * 0.375, 1.5, 1.01) for i in range(2)]
    if not all(covered(p) for p in texel_centres):
        raise Premise('un centro di texel della faccia e\' scoperto: anche la regola di prima la esporterebbe')
    if all(covered(p) for p in m._cell_centres(x, 'south', 0.01)):
        raise Premise('nessuna cella della faccia e\' scoperta: la faccia e\' davvero sepolta')
    ok = 'south' in x.faces
    return ok, 'faccia esportata' if ok else 'la faccia con la striscia scoperta non e\' nell\'atlante (buco)'


@case('2. scatola dentro la testa del pilota')
def inside_pilot_head():
    m = real_model()
    m.box('frameHead', 'selftest_head', (-0.5, 26, -0.5), (0.5, 27, 0.5), 'gun')
    m.pack()
    hits = [p for p in m.check() if 'selftest_head' in p and 'pilota' in p]
    return bool(hits), hits[0] if hits else 'nessuna segnalazione'


@case('3. scatola fuori dai limiti')
def out_of_bounds():
    m = real_model()
    m.box('frameBody', 'selftest_far', (20, 10, 0), (21, 11, 1), 'gun')
    m.pack()
    hits = [p for p in m.check() if 'selftest_far' in p and 'ingombro' in p]
    return bool(hits), hits[0] if hits else 'nessuna segnalazione'


@case('4. coordinata fuori dalla griglia dei quarti di pixel')
def off_grid():
    m = real_model()
    try:
        m.box('frameBody', 'selftest_grid', (0, 10, 0), (1.1, 11, 1), 'gun')
    except ValueError as e:
        return True, str(e)
    return False, 'accettata'


@case('5. linea sottile fuori dalla sua scatola')
def line_outside_box():
    m = tiny_model()
    from exo import piece
    m.box(piece('Helmet', 'p2'), 'bad_slats', (3, 28, -8), (5, 29, -7.5), 'paint', lines=[('h', 30, 3.25, 4.75)])
    m.pack()
    b = next(b for b in m.boxes if b.name == 'bad_slats_s0')
    try:
        paint.fine_lines(b, 'north', b.faces['north'])
    except ValueError as e:
        return True, str(e)
    return False, 'accettata'


@case('6. stato visivo piu\' chiaro del precedente')
def lighter_state():
    m = tiny_model()
    m.pack()
    saved = dict(paint.WEAR)
    try:
        grime = list(paint.WEAR['grime'])
        grime[3] = 1.6          # lo stato 3 diventa piu' chiaro del 2
        paint.WEAR['grime'] = tuple(grime)
        atlas, _ = paint.paint_model(m)
    finally:
        paint.WEAR.clear()
        paint.WEAR.update(saved)
    hits = paint.check_wear(m, atlas)
    return bool(hits), hits[0] if hits else 'nessuna segnalazione'


@case('7. regione dell\'atlante piena')
def region_full():
    m = real_model()
    for k in range(40):
        m.box('pieceHelmet_s0_p1', f'selftest_big_{k}', (-12, 0, -8), (12, 30, -7), 'paint')
    try:
        m.pack()
    except ValueError as e:
        return 'piena' in str(e), str(e)
    return False, 'impacchettato senza errori'


@case('8. faccia fine diversa dalla sua versione a densita\' normale')
def fine_differs():
    m = tiny_model()
    m.pack()
    ref = tiny_model(fine=False)
    ref.pack()
    # controllo: senza il difetto il modello fine e' pulito
    clean = paint.check_fine(m, *paint.paint_model(m), ref)
    if clean:
        raise Premise(f'il modello fine senza difetti non e\' pulito: {clean[0]}')
    b = next(b for b in m.boxes if b.name == 'slats_s0')
    b.seed ^= 1                 # la faccia fine si dipinge con un altro seme: non e' piu' quella normale ingrandita
    atlas, glow = paint.paint_model(m)
    hits = paint.check_fine(m, atlas, glow, ref)
    return bool(hits), hits[0] if hits else 'nessuna segnalazione'


@case('9. un osso di primo livello con i pezzi di due slot')
def two_slots_one_bone():
    m = Model('selftest')
    m.piece_bones('Helmet', 'armorHead')
    try:
        m.piece_bones('Torso', 'armorHead')
    except ValueError as e:
        return True, str(e)
    return False, 'accettato'


def main():
    failed = 0
    for name, fn in CASES:
        ok, detail = run(fn)
        print(f'{"ok  " if ok else "FAIL"} {name}: {detail}')
        failed += not ok
    total = len(CASES)
    if failed:
        print(f'FAIL selftest: {failed} prove su {total} fallite (difetto non segnalato o premessa non valida)',
              file=sys.stderr)
        sys.exit(1)
    print(f'OK selftest: {total} difetti su {total} segnalati, premesse valide')


if __name__ == '__main__':
    main()
