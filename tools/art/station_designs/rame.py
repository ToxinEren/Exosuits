"""B, Rame: la ricetta fatta banco (CCC / ITI / IrI). Sopra tre lastre di rame rivettate (CCC); ai due estremi
montanti di ferro chiaro (le I), con i piedi; al centro, fra i montanti, il cuore di legno del banco da lavoro (la T)
a tavole, rientrato e cerchiato di ferro, con una piastra di ferro a griglia 3x3 sulla principale e uno sportello
sulla laterale; in basso, fra i piedi di ferro, una canalina di polvere di redstone (la r; solo pittura, il blocco
non fa luce). Dietro le tavole con due croci di ferro; sugli estremi una traversa di ferro."""
from pixel import Canvas, sprite

LETTER = 'B'
TITLE = 'Rame'
IDEA = 'la ricetta fatta banco: lastre di rame sopra, montanti di ferro, il banco da lavoro di legno al centro, ' \
       'redstone in basso'


def build(b):
    # --- geometria ---------------------------------------------------------------------------------------------
    b.cut('front', (3, 3, 29, 12), 1)                 # il cuore di legno fra i montanti
    b.fill('front', (6, 4, 13, 11), 1)                # piastra a griglia sulla principale
    for c in (7, 9, 11):                              # ...con le nove caselle incassate
        for r in (5, 7, 9):
            b.cut('front', (c, r, c + 1, r + 1), 1)
    b.cut('front', (3, 13, 29, 15), 1)                # canalina della redstone
    b.cut('back', (3, 3, 29, 12), 1)
    b.cut('back', (3, 13, 29, 15), 1)
    b.cut('left', (3, 4, 13, 14), 1)                  # fianco di legno fra le gambe

    # --- fronte ------------------------------------------------------------------------------------------------
    f = Canvas(16, 32, 'rame/front', 'iron', 4)
    frame(f, 32)
    planks(f, (3, 3, 29, 12), 'front')
    # principale: piastra di ferro con la griglia 3x3 del banco da lavoro
    f.fill((6, 4, 13, 11), 'iron', 5)
    for c in (7, 9, 11):
        for r in (5, 7, 9):
            f.px(c, r, 'oak', 2)
    # laterale: sportello a tavole verticali, cerniere di ferro, chiavistello di rame
    f.fill((18, 4, 27, 11), 'oak', 4)
    for c in (20, 23, 26):
        f.vline(c, 4, 11, 'oak', 2)
    f.vstreaks((18, 4, 27, 11), 'door', every=2, length=(2, 4))
    f.border((17, 3, 28, 12), 'iron', 3)
    for r in (5, 9):
        f.hline(r, 17, 21, 'iron', 5)
    f.fill((25, 7, 28, 9), 'copper', 5).px(25, 7, 'copper', 6).px(27, 8, 'copper', 3)
    # canalina: polvere di redstone su fondo scuro
    f.fill((3, 13, 29, 15), 'red', 1)
    f.speckle((3, 13, 29, 15), 'dust', 'red', (4, 5, 6), chance=2)
    f.hline(13, 3, 29, 'red', 0)
    f.relief(b.depth_map('front'))
    f.noise((0, 3, 32, 13), 'iron', chance=9)
    posts(f, 32)
    b.canvas['front'] = f

    # --- sopra (meta' principale, visto da davanti) -------------------------------------------------------------
    t = Canvas(16, 16, 'rame/top', 'copper', 4)
    t.streaks((0, 0, 16, 16), 'brush', every=2, length=(3, 7))
    t.hline(0, 0, 16, 'copper', 6)
    t.hline(15, 0, 16, 'copper', 6)
    t.vline(0, 0, 16, 'copper', 6)
    t.hline(1, 1, 16, 'copper', 3)
    t.vline(1, 1, 15, 'copper', 5)
    for c in (10, 11):                                # giunto fra le lastre (con lo specchio: tre lastre)
        t.vline(c, 1, 15, 'copper', 2 if c == 10 else 5)
    for r in range(3, 15, 3):
        t.rivet(9, r).rivet(12, r)
    for c in (3, 6):
        t.rivet(c, 2).rivet(c, 13)
    t.speckle((0, 0, 3, 16), 'patina', 'patina', (2, 3), chance=7)
    t.speckle((0, 0, 16, 2), 'patina-back', 'patina', (2, 3), chance=9)
    t.speckle((0, 14, 16, 16), 'patina-front', 'patina', (3, 4), chance=9)
    t.fill((0, 0, 3, 3), 'iron', 5).fill((0, 13, 3, 16), 'iron', 5)   # cappucci dei montanti
    t.px(0, 0, 'iron', 6).px(2, 2, 'iron', 3).px(0, 13, 'iron', 6).px(2, 15, 'iron', 3)
    b.canvas['top'] = t

    # --- retro (visto da dietro) --------------------------------------------------------------------------------
    k = Canvas(16, 32, 'rame/back', 'iron', 4)
    frame(k, 32)
    planks(k, (3, 3, 29, 12), 'back')
    for x0 in (3, 16):                                # due croci di ferro
        for i in range(13):
            c = x0 + i
            r1 = 3 + (i * 9) // 13
            r2 = 11 - (i * 9) // 13
            k.px(c, r1, 'iron', 5).px(c, r2, 'iron', 4)
    k.fill((3, 13, 29, 15), 'red', 1)
    k.speckle((3, 13, 29, 15), 'dust', 'red', (4, 5, 6), chance=2)
    k.relief(b.depth_map('back'))
    k.noise((0, 3, 32, 13), 'iron', chance=9)
    posts(k, 32)
    b.canvas['back'] = k

    # --- estremo (visto da fuori: a destra il fronte) -----------------------------------------------------------
    e = Canvas(16, 16, 'rame/end', 'iron', 4)
    e.fill((0, 0, 16, 3), 'copper', 5)
    e.hline(0, 0, 16, 'copper', 6)
    e.hline(2, 0, 16, 'copper', 3)
    e.streaks((0, 1, 16, 2), 'slab', every=1, length=(2, 5))
    e.fill((0, 3, 3, 16), 'iron', 4)
    e.fill((13, 3, 16, 16), 'iron', 4)
    e.fill((3, 3, 13, 4), 'iron', 3)
    e.fill((3, 14, 13, 16), 'iron', 3)
    planks(e, (3, 4, 13, 14), 'end', vertical=True)
    e.fill((3, 9, 13, 10), 'iron', 5)                 # traversa
    e.hline(10, 3, 13, 'iron', 2)
    e.relief(b.depth_map('left'))
    e.noise((0, 3, 16, 16), 'iron', chance=9)
    for c in (1, 14):
        e.rivet(c, 5).rivet(c, 12)
    e.rivet(4, 9).rivet(11, 9)
    b.canvas['left'] = e

    # --- sotto ---------------------------------------------------------------------------------------------------
    s = Canvas(16, 16, 'rame/bottom', 'oak', 2)
    s.vstreaks((0, 0, 16, 16), 'grain', every=2, length=(3, 8))
    s.fill((0, 0, 3, 16), 'iron', 3).fill((13, 0, 16, 16), 'iron', 3)
    b.canvas['bottom'] = s


def frame(cv, w):
    """Facce lunghe: spessore delle lastre di rame (tre righe), montanti di ferro agli estremi, traversa sotto il
    legno, piedi e zoccolo di ferro."""
    cv.fill((0, 0, w, 3), 'copper', 5)
    cv.hline(0, 0, w, 'copper', 6)
    cv.hline(2, 0, w, 'copper', 3)
    cv.streaks((0, 1, w, 2), 'slab', every=1, length=(2, 5))
    cv.speckle((0, 2, w, 3), 'patina', 'patina', (2, 3), chance=9)
    cv.fill((0, 3, 3, 16), 'iron', 4)
    cv.fill((w - 3, 3, w, 16), 'iron', 4)
    cv.fill((3, 12, w - 3, 13), 'iron', 4)
    cv.fill((3, 15, w - 3, 16), 'iron', 3)


def posts(cv, w):
    for c in (0, w - 3):
        cv.vline(c, 3, 16, 'iron', 5)
        cv.vline(c + 2, 3, 16, 'iron', 3)
        for r in (5, 10, 14):
            cv.rivet(c + 1, r)
    for c in range(7, w - 4, 8):
        cv.rivet(c, 1, 'copper')


def planks(cv, rect, key, vertical=False):
    """Tavole di quercia: tre texel per tavola, giunti scuri, venature."""
    c0, r0, c1, r1 = rect
    cv.fill(rect, 'oak', 4)
    if vertical:
        cv.vstreaks(rect, key + '-grain', every=2, length=(2, 6))
        for c in range(c0 + 3, c1, 3):
            cv.vline(c, r0, r1, 'oak', 2)
    else:
        cv.streaks(rect, key + '-grain', every=1, length=(2, 6))
        for r in range(r0 + 3, r1, 3):
            cv.hline(r, c0, c1, 'oak', 2)


ICON = (
    '................',
    '................',
    '................',
    '....kkkkkkkkkkkk',
    '...kCCCCCCCCCCCk',
    '..kCcCCCcCCCcCkk',
    '.kCCCCCCCCCCCkIk',
    'kQQQQQQQQQQQQkIk',
    'kIIWWWWWWWWWIIkk',
    'kIIWgWgWWWWWIIIk',
    'kIIWWWWWdWdWIIIk',
    'kIIWgWgWWWWWIIk.',
    'kIIIIIIIIIIIIIk.',
    'kIIrRrRrRrRrIIk.',
    'kkkkkkkkkkkkkkk.',
    '................',
)


def icon():
    legend = {
        'k': ('black', 2), 'C': ('copper', 5), 'c': ('copper', 3), 'Q': ('copper', 4), 'I': ('iron', 5),
        'W': ('oak', 4), 'g': ('iron', 3), 'd': ('copper', 6), 'r': ('red', 3), 'R': ('red', 6),
    }
    return sprite(ICON, legend)
