"""C, Pistoni: un banco-macchina dal mondo della tuta. Telaio d'acciaio chiaro a piastre imbullonate, piano di pressa
con la lamiera a rilievi; sulla principale un vano con due pistoni idraulici a vista (cilindri, steli cromati,
premistoppa d'ottone); sulla laterale la cella di redstone dietro tre barre di guardia, come l'alloggiamento sulla
schiena della tuta (solo pittura: il blocco non fa luce); un tubo di rame lungo lo zoccolo; dietro due tubi di rame
con una valvola e le feritoie; sugli estremi un mozzo d'ottone e un tubo."""
from pixel import Canvas, sprite

LETTER = 'C'
TITLE = 'Pistoni'
IDEA = 'un banco-macchina dal mondo della tuta: acciaio imbullonato, due pistoni a vista, la cella di redstone ' \
       'dietro le barre, tubi di rame'


def build(b):
    # --- geometria ---------------------------------------------------------------------------------------------
    b.cut('front', (2, 3, 14, 13), 2)                 # vano dei pistoni
    for c in (3, 9):
        b.fill('front', (c, 7, c + 4, 13), 2)          # cilindri, fino al piano del fronte
        b.fill('front', (c + 1, 3, c + 3, 7), 2, start=1)   # steli, un pixel fuori dal fondo
    b.cut('front', (18, 4, 29, 12), 2)                # finestra della cella
    for c in (20, 23, 26):
        b.fill('front', (c, 4, c + 1, 12), 2, start=1)  # barre di guardia, un pixel davanti alla cella
    b.cut('back', (2, 3, 30, 13), 1)
    b.fill('back', (2, 5, 30, 7), 1)                  # due tubi di rame
    b.fill('back', (2, 9, 30, 11), 1)
    b.cut('left', (2, 3, 14, 13), 1)
    b.fill('left', (4, 5, 10, 11), 1)                 # mozzo d'ottone
    b.fill('left', (11, 3, 13, 13), 1)                # tubo verticale

    # --- fronte ------------------------------------------------------------------------------------------------
    f = Canvas(16, 32, 'pistoni/front', 'steel', 4)
    frame(f, 32)
    f.fill((2, 3, 14, 13), 'gun', 1)                  # fondo del vano
    f.vstreaks((2, 3, 14, 13), 'bay', every=2, length=(2, 5))
    for c in (3, 9):
        f.fill((c, 7, c + 4, 13), 'gun', 4)            # cilindro: luce a sinistra, ombra a destra
        f.vline(c, 7, 13, 'gun', 6).vline(c + 1, 7, 13, 'gun', 5).vline(c + 3, 7, 13, 'gun', 2)
        f.hline(7, c, c + 4, 'brass', 4).px(c, 7, 'brass', 6).px(c + 3, 7, 'brass', 2)
        f.hline(11, c, c + 4, 'gun', 3)
        f.vline(c + 1, 3, 7, 'chrome', 6).vline(c + 2, 3, 7, 'chrome', 3)
        f.px(c + 1, 3, 'chrome', 7)
    f.fill((7, 4, 9, 6), 'brass', 4).px(7, 4, 'brass', 6).px(8, 5, 'black', 1)   # manometro
    f.fill((18, 4, 29, 12), 'red', 2)                 # la cella
    f.fill((19, 5, 28, 11), 'red', 4)
    f.fill((19, 6, 28, 10), 'red', 5)
    f.hline(6, 19, 28, 'red', 6)
    f.speckle((19, 6, 28, 10), 'cell', 'red', (6, 7), chance=5)
    f.hline(11, 18, 29, 'red', 1)
    for c in (20, 23, 26):
        f.vline(c, 4, 12, 'chrome', 5)
    f.relief(b.depth_map('front'))
    f.noise((0, 3, 32, 13), 'steel', chance=9)
    for c in (20, 23, 26):
        f.px(c, 4, 'chrome', 7)
    bolts(f, 32)
    for r in (4, 11):
        f.rivet(14, r).rivet(16, r)
    b.canvas['front'] = f

    # --- sopra: lamiera a rilievi -------------------------------------------------------------------------------
    t = Canvas(16, 16, 'pistoni/top', 'steel', 4)
    for i in range(4):                                # lamiera a rilievi: barrette alternate, luce in alto a sinistra
        for j in range(4):
            c0, r0 = 4 * i, 4 * j
            if (i + j) % 2 == 0:
                t.hline(r0 + 1, c0 + 1, c0 + 4, 'steel', 6).px(c0 + 3, r0 + 1, 'steel', 5)
                t.hline(r0 + 2, c0 + 1, c0 + 4, 'steel', 2)
            else:
                t.vline(c0 + 2, r0, r0 + 3, 'steel', 6).px(c0 + 2, r0 + 2, 'steel', 5)
                t.vline(c0 + 3, r0, r0 + 3, 'steel', 2)
    t.noise((0, 0, 16, 16), 'plate', chance=14, down_only=True)
    t.hline(0, 0, 16, 'steel', 6).hline(15, 0, 16, 'steel', 5).vline(0, 0, 16, 'steel', 6)
    t.vline(15, 1, 15, 'steel', 2)
    for c in (3, 7, 11):
        t.rivet(c, 0).rivet(c, 15)
    t.rivet(0, 4).rivet(0, 11)
    b.canvas['top'] = t

    # --- retro ---------------------------------------------------------------------------------------------------
    k = Canvas(16, 32, 'pistoni/back', 'steel', 4)
    frame(k, 32)
    k.fill((2, 3, 30, 13), 'gun', 1)
    for r in (3, 7, 11):                              # feritoie fra i tubi
        k.hline(r, 2, 30, 'gun', 3)
    for r0 in (5, 9):
        k.hline(r0, 2, 30, 'copper', 6).hline(r0 + 1, 2, 30, 'copper', 3)
        for c in (6, 15, 16, 25):
            k.vline(c, r0, r0 + 2, 'steel', 6)
    # valvola d'ottone sul tubo alto della principale (vista da dietro a destra)
    for c, r in ((22, 3), (23, 3), (24, 3), (21, 4), (25, 4), (21, 5), (25, 5), (22, 6), (23, 6), (24, 6)):
        k.px(c, r, 'brass', 5)
    k.px(23, 4, 'brass', 3).px(23, 5, 'brass', 6)
    k.relief(b.depth_map('back'))
    k.noise((0, 3, 32, 13), 'steel', chance=9)
    bolts(k, 32)
    b.canvas['back'] = k

    # --- estremo (visto da fuori: a destra il fronte) -----------------------------------------------------------
    e = Canvas(16, 16, 'pistoni/end', 'steel', 4)
    frame(e, 16)
    e.fill((2, 3, 14, 13), 'steel', 3)
    e.fill((4, 5, 10, 11), 'brass', 4)              # mozzo: disco d'ottone, bullone cromato al centro
    for c, r in ((4, 5), (9, 5), (4, 10), (9, 10)):
        e.px(c, r, 'steel', 2)
    e.hline(5, 5, 9, 'brass', 6).vline(4, 6, 10, 'brass', 5).hline(10, 5, 9, 'brass', 2).vline(9, 6, 10, 'brass', 3)
    e.fill((6, 7, 8, 9), 'chrome', 5).px(6, 7, 'chrome', 7).px(7, 8, 'chrome', 2)
    e.fill((11, 3, 13, 13), 'copper', 4)
    e.vline(11, 3, 13, 'copper', 6).vline(12, 3, 13, 'copper', 3)
    for r in (5, 10):
        e.hline(r, 10, 14, 'steel', 6)
    e.relief(b.depth_map('left'))
    e.noise((0, 3, 16, 13), 'steel', chance=9)
    bolts(e, 16)
    b.canvas['left'] = e

    # --- sotto ---------------------------------------------------------------------------------------------------
    s = Canvas(16, 16, 'pistoni/bottom', 'steel', 2)
    s.noise((0, 0, 16, 16), 'bottom', chance=6)
    b.canvas['bottom'] = s


def frame(cv, w):
    """Facce laterali: spessore del piano di pressa (tre righe), colonne d'angolo, zoccolo con il tubo di rame."""
    cv.hline(0, 0, w, 'steel', 6)
    cv.hline(1, 0, w, 'steel', 5)
    cv.hline(2, 0, w, 'steel', 2)
    cv.fill((0, 3, 2, 13), 'steel', 5)
    cv.fill((w - 2, 3, w, 13), 'steel', 5)
    cv.vline(1, 3, 13, 'steel', 3).vline(w - 1, 3, 13, 'steel', 3)
    cv.hline(13, 0, w, 'copper', 5)
    cv.hline(14, 0, w, 'copper', 3)
    cv.hline(15, 0, w, 'steel', 2)
    for c in range(3, w - 2, 7):
        cv.vline(c, 13, 15, 'steel', 6)


def bolts(cv, w):
    for c in range(2, w - 1, 4):
        cv.rivet(c, 1)
    for c in (0, w - 2):
        for r in (4, 8, 11):
            cv.rivet(c, r)


ICON = (
    '................',
    '................',
    '................',
    '....kkkkkkkkkkkk',
    '...kSSSSSSSSSSSk',
    '..kSsSsSsSsSsSkk',
    '.kSSSSSSSSSSSkTk',
    'kTTTTTTTTTTTTkTk',
    'kTdHddHdTRbRbkTk',
    'kTdHddHdTRbRbkTk',
    'kTgggggdTRbRbkTk',
    'kTgggggdTRbRbkTk',
    'kTgggggdTTTTTkk.',
    'kccccccccccccck.',
    'kkkkkkkkkkkkkkk.',
    '................',
)


def icon():
    legend = {
        'k': ('black', 1), 'S': ('steel', 6), 's': ('steel', 3), 'T': ('steel', 4), 'd': ('gun', 1),
        'H': ('chrome', 6), 'g': ('gun', 5), 'R': ('red', 6), 'b': ('chrome', 4), 'c': ('copper', 5),
    }
    return sprite(ICON, legend)
