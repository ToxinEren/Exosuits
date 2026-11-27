"""A, Officina: la console del mock-up mostrato al proprietario, rifinita e piu' metallica. Corpo di acciaio scuro a
piastre rivettate, rientrato di un pixel fra il piano e lo zoccolo; piano di lavoro di bronzo spazzolato con il bordo
rialzato, un riflesso, il giunto al centro e i bulloni; sulla principale uno schermo di redstone incassato (solo pittura: il blocco non fa luce), tre
manopole di rame e una leva; sulla laterale due cassetti con le maniglie di rame; dietro un tubo di rame con le
staffe, una griglia e uno sportello; sugli estremi un tubo di rame verticale."""
from pixel import Canvas, sprite

LETTER = 'A'
TITLE = 'Officina'
IDEA = 'la console del mock-up, più dettagliata e metallica: acciaio scuro rivettato, piano di bronzo, rame, ' \
       'schermo di redstone e cassetti'


def build(b):
    # --- geometria ---------------------------------------------------------------------------------------------
    b.cut('front', (1, 2, 31, 14), 1)                 # corpo rientrato fra piano, zoccolo e montanti
    b.cut('front', (3, 4, 12, 9), 2, start=1)         # schermo
    for c in (4, 7, 10):                              # manopole
        b.fill('front', (c, 10, c + 2, 12), 1)
    b.cut('front', (13, 4, 15, 12), 2, start=1)       # feritoia della leva
    b.fill('front', (13, 7, 15, 9), 2)                # leva
    b.fill('front', (18, 3, 30, 8), 1)                # cassetti
    b.fill('front', (18, 9, 30, 13), 1)
    b.cut('back', (1, 2, 31, 14), 1)
    b.fill('back', (1, 6, 31, 8), 1)                  # tubo di rame
    b.cut('back', (3, 9, 13, 13), 2, start=1)         # griglia (sulla laterale: da dietro a sinistra)
    b.cut('left', (1, 2, 15, 14), 1)
    b.fill('left', (9, 2, 11, 14), 1)                 # tubo verticale

    # --- fronte ------------------------------------------------------------------------------------------------
    f = Canvas(16, 32, 'officina/front', 'gun', 4)
    body(f, 32)
    # principale: cornice dello schermo (lo schermo si dipinge dopo il rumore), manopole, leva
    f.border((2, 3, 13, 10), 'gun', 5)
    for c in (4, 7, 10):
        f.fill((c, 10, c + 2, 12), 'copper', 4)
        f.px(c, 10, 'copper', 6).px(c + 1, 10, 'copper', 5).px(c + 1, 11, 'copper', 2)
    f.fill((13, 4, 15, 12), 'black', 1)
    f.fill((13, 7, 15, 9), 'copper', 4).px(13, 7, 'red', 6).px(14, 7, 'red', 4).px(14, 8, 'copper', 2)
    # laterale: cassetti con maniglia e targhetta
    for r0, r1, hr in ((3, 8, 6), (9, 13, 11)):
        f.fill((18, r0, 30, r1), 'gun', 5)
        f.hline(hr, 22, 26, 'copper', 5).px(22, hr, 'copper', 6).px(25, hr, 'copper', 3)
        f.hline(hr + 1, 22, 26, 'gun', 2)
        f.hline(r0 + 1, 23, 25, 'brass', 4)
    f.relief(b.depth_map('front'))
    f.noise((0, 2, 32, 16), 'body', chance=8)
    screen(f, (3, 4, 12, 9))
    rivets(f, 32)
    f.rivet(16, 3).rivet(16, 12)
    b.canvas['front'] = f

    # --- sopra (meta' principale, visto da davanti: in basso il fronte) -----------------------------------------
    t = Canvas(16, 16, 'officina/top', 'bronze', 4)
    t.streaks((2, 2, 15, 14), 'brush', every=4, length=(4, 9))
    for r, c0, c1 in ((5, 2, 15), (6, 3, 15), (4, 6, 13), (7, 7, 12)):   # riflesso lungo il piano
        t.add((c0, r, c1, r + 1), 1)
    t.hline(5, 4, 11, 'bronze', 6)
    t.hline(0, 0, 16, 'bronze', 6).vline(0, 0, 16, 'bronze', 6)   # bordo rialzato (al centro prosegue nello specchio)
    t.hline(15, 0, 16, 'bronze', 5)
    t.hline(1, 1, 16, 'bronze', 2).vline(1, 1, 15, 'bronze', 2).hline(14, 1, 16, 'bronze', 3)
    t.vline(15, 1, 15, 'bronze', 2)                   # giunto al centro (con lo specchio: due texel)
    for c, r in ((9, 9), (4, 11)):                    # graffi
        t.px(c, r, 'bronze', 6).px(c + 1, r + 1, 'bronze', 6).px(c + 2, r + 1, 'bronze', 5)
    for c in (3, 7, 11):                              # bulloni lungo il bordo
        t.rivet(c, 0, 'bronze').rivet(c, 15, 'bronze')
    t.rivet(0, 5, 'bronze').rivet(0, 10, 'bronze')
    for r in (3, 7, 11):
        t.rivet(13, r)
    b.canvas['top'] = t

    # --- retro (visto da dietro: a sinistra la laterale) --------------------------------------------------------
    k = Canvas(16, 32, 'officina/back', 'gun', 4)
    body(k, 32)
    k.fill((1, 6, 31, 8), 'copper', 4)
    k.hline(6, 1, 31, 'copper', 6)
    k.hline(7, 1, 31, 'copper', 3)
    for c in (6, 16, 26):                             # staffe del tubo
        k.fill((c, 5, c + 2, 9), 'gun', 6)
        k.vline(c + 1, 5, 9, 'gun', 3)
    k.fill((3, 9, 13, 13), 'black', 2)                # griglia
    for r in (9, 11):
        k.hline(r, 3, 13, 'gun', 3)
    k.border((19, 9, 29, 13), 'gun', 5)               # sportello della principale
    k.hline(11, 22, 26, 'gun', 2)
    k.relief(b.depth_map('back'))
    k.noise((0, 2, 32, 16), 'body', chance=8)
    rivets(k, 32)
    k.rivet(20, 10).rivet(27, 10)
    b.canvas['back'] = k

    # --- estremo (visto da fuori: a destra il fronte) -----------------------------------------------------------
    e = Canvas(16, 16, 'officina/end', 'gun', 4)
    body(e, 16)
    e.fill((9, 2, 11, 14), 'copper', 4)
    e.vline(9, 2, 14, 'copper', 6)
    e.vline(10, 2, 14, 'copper', 3)
    for r in (4, 11):
        e.fill((8, r, 12, r + 1), 'gun', 6)
    e.relief(b.depth_map('left'))
    e.noise((0, 2, 16, 16), 'body', chance=8)
    rivets(e, 16)
    b.canvas['left'] = e

    # --- sotto ---------------------------------------------------------------------------------------------------
    s = Canvas(16, 16, 'officina/bottom', 'gun', 2)
    s.noise((0, 0, 16, 16), 'bottom', chance=6)
    b.canvas['bottom'] = s


def screen(cv, rect):
    """Lo schermo di redstone: fondo nero, griglia rossa spenta, la traccia di un'onda, una spia. Solo pittura."""
    c0, r0, c1, r1 = rect
    cv.fill(rect, 'black', 1)
    for c in range(c0, c1, 2):
        for r in range(r0, r1, 2):
            cv.px(c, r, 'red', 1)
    wave = (7, 6, 5, 5, 6, 7, 7, 6, 5)
    for i, c in enumerate(range(c0, c1)):
        r = r0 + wave[i % len(wave)] - 4
        cv.px(c, r, 'red', 6)
        if i:
            prev = r0 + wave[(i - 1) % len(wave)] - 4
            for rr in range(min(r, prev) + 1, max(r, prev)):
                cv.px(c, rr, 'red', 4)
    cv.px(c1 - 1, r0, 'red', 5)


def body(cv, w):
    """Lo schema delle facce laterali: spessore del piano di bronzo (due righe, la prima in luce), corpo d'acciaio
    rientrato, montanti agli spigoli, zoccolo scuro, giunto delle piastre al centro."""
    cv.hline(0, 0, w, 'bronze', 6)
    cv.hline(1, 0, w, 'bronze', 4)
    cv.streaks((0, 1, w, 2), 'slab', every=1, length=(2, 5))
    cv.fill((1, 2, w - 1, 14), 'gun', 3)
    cv.vline(0, 2, 14, 'gun', 5).vline(w - 1, 2, 14, 'gun', 5)
    cv.hline(14, 0, w, 'gun', 3)
    cv.hline(15, 0, w, 'gun', 2)
    if w == 32:
        cv.vline(16, 2, 14, 'gun', 1)
        cv.vline(15, 2, 14, 'gun', 4)


def rivets(cv, w):
    for c in (0, w - 1):
        for r in (3, 12):
            cv.rivet(c, r)
    for c in range(3, w, 9):
        cv.rivet(c, 14)


ICON = (
    '................',
    '................',
    '................',
    '....kkkkkkkkkkkk',
    '...kBBBBBBBBBBBk',
    '..kBbbbbbbbbbBkk',
    '.kBBBBBBBBBBBkgk',
    'kOOOOOOOOOOOOkgk',
    'kGkkkkkGGHHHHkgk',
    'kGkRrRkGGDDcDkgk',
    'kGkrRrkGGHHHHkgk',
    'kGkkkkkGGDDcDkg.',
    'kGcGcGcGGHHHHkk.',
    'kPPPPPPPPPPPPk..',
    'kkkkkkkkkkkkkk..',
    '................',
)


def icon():
    legend = {
        'k': ('black', 1), 'B': ('bronze', 5), 'b': ('bronze', 3), 'O': ('bronze', 4), 'G': ('gun', 4),
        'g': ('gun', 3), 'R': ('red', 6), 'r': ('red', 3), 'c': ('copper', 5), 'D': ('gun', 6), 'H': ('gun', 2),
        'P': ('gun', 2),
    }
    return sprite(ICON, legend)
