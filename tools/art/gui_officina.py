"""Stile A, Officina (la direzione della stazione scelta dal proprietario il 27 novembre 2026), per la schermata della
stazione, l'HUD della tuta, la vignetta del casco e l'icona del kit di riparazione (design M4, par. 5.3, 5.8, 5.12;
parte 4d). Materiali di station_designs/officina.py e delle scale di pixel.py: acciaio scuro spazzolato e rivettato,
bronzo per cornici e bordi, rame per le finiture e il passaggio del mouse, il rosso della redstone solo come pittura.
Luce dall'alto a sinistra, ombre e luci a gradini, nessuna sfumatura morbida (salvo la vignetta, che e' un
oscuramento). I colori della legenda (HUD e bordi dei posti) sono quelli esatti del design, mai ritoccati.

Ogni funzione restituisce array numpy (altezza, larghezza, 4) uint8 RGBA; il rumore viene da paint.HashRng con semi
dai nomi delle tele (pixel.Canvas), quindi stesse chiamate, stessi texel. Nessuna data, nessun numero casuale d'altro
tipo: la vignetta usa solo interi.
"""
from math import isqrt

import numpy as np

import gui_layout as L
from pixel import RAMPS, Canvas, sprite


# --- aiuti ------------------------------------------------------------------------------------------------------------
def rgb(mat, lev):
    ramp = RAMPS[mat]
    return ramp[max(0, min(lev, len(ramp) - 1))]


def blank(w, h):
    return np.zeros((h, w, 4), np.uint8)


def fill(a, r, c, alpha=255):
    x0, y0, x1, y1 = r
    a[y0:y1, x0:x1, :3] = c
    a[y0:y1, x0:x1, 3] = alpha
    return a


def ring(a, r, c, alpha=255):
    x0, y0, x1, y1 = r
    for side in ((x0, y0, x1, y0 + 1), (x0, y1 - 1, x1, y1), (x0, y0, x0 + 1, y1), (x1 - 1, y0, x1, y1)):
        fill(a, side, c, alpha)
    return a


def bevel_ring(a, r, light, dark, alpha=255):
    """Bordo di 1 pixel a rilievo: riga in alto e colonna a sinistra `light`, il resto `dark`."""
    x0, y0, x1, y1 = r
    fill(a, (x0, y1 - 1, x1, y1), dark, alpha)
    fill(a, (x1 - 1, y0, x1, y1), dark, alpha)
    fill(a, (x0, y0, x1 - 1, y0 + 1), light, alpha)
    fill(a, (x0, y0, x0 + 1, y1 - 1), light, alpha)
    return a


def paste(dst, src, x, y):
    h, w = src.shape[:2]
    dst[y:y + h, x:x + w] = src
    return dst


def stamp(a, rows, legend, x=0, y=0):
    """Disegna una griglia di caratteri sopra `a` (i '.' lasciano com'e'): legend {carattere: (rgb, alfa)}."""
    for r, row in enumerate(rows):
        for c, ch in enumerate(row):
            if ch == '.':
                continue
            colour, alpha = legend[ch]
            a[y + r, x + c, :3] = colour
            a[y + r, x + c, 3] = alpha
    return a


def inset(cv, r, mat='gun', lev=2, key=None):
    """Un pannello incassato nella piastra: fondo piu' scuro, ombra in alto e a sinistra, labbro chiaro in basso e a
    destra (la luce viene dall'alto a sinistra)."""
    x0, y0, x1, y1 = r
    cv.fill((x0 + 1, y0 + 1, x1 - 1, y1 - 1), mat, lev)
    if key:
        cv.noise((x0 + 1, y0 + 1, x1 - 1, y1 - 1), key, chance=14, down_only=True)
    cv.hline(y0, x0, x1, 'black', 1).vline(x0, y0, y1, 'black', 1)
    cv.hline(y1 - 1, x0 + 1, x1, 'gun', 6).vline(x1 - 1, y0 + 1, y1, 'gun', 6)
    return cv


def slot(cv, r, inner=('gun', 1)):
    """Uno slot 18x18 del gioco base nello stile A: incassato, fondo d'acciaio scuro."""
    x0, y0, x1, y1 = r
    cv.fill((x0 + 1, y0 + 1, x1 - 1, y1 - 1), *inner)
    cv.hline(y0, x0, x1, 'black', 1).vline(x0, y0, y1, 'black', 1)
    cv.hline(y1 - 1, x0 + 1, x1, 'gun', 5).vline(x1 - 1, y0 + 1, y1, 'gun', 5)
    return cv


def tray(cv, r, inner=('black', 2)):
    """Una vaschetta scura (tavolozza, riga dei costi, incavo della barra): ombra nera, labbro d'acciaio."""
    x0, y0, x1, y1 = r
    cv.fill((x0 + 1, y0 + 1, x1 - 1, y1 - 1), *inner)
    cv.hline(y0, x0, x1, 'black', 0).vline(x0, y0, y1, 'black', 0)
    cv.hline(y1 - 1, x0 + 1, x1, 'gun', 4).vline(x1 - 1, y0 + 1, y1, 'gun', 4)
    return cv


# --- sfondo della schermata (316x232) ---------------------------------------------------------------------------------
def background():
    """La schermata vuota: piastra d'acciaio scuro spazzolata con la cornice di bronzo e i rivetti agli angoli, i
    quattro pannelli incassati, le prese dei posti, la vaschetta della tavolozza e l'incavo della barra, il fondo
    dell'anteprima con la piattaforma girevole, gli slot dell'inventario, le scanalature dei preset e la riga dei costi.
    Lo spazio dei moduli (moduli.riservato) resta pannello vuoto: niente disegni per M5 (D1)."""
    W, H = L.SCREEN_W, L.SCREEN_H
    G = L.GUI
    cv = Canvas(H, W, 'gui/fondo', 'gun', 3)
    cv.streaks((3, 3, W - 3, H - 3), 'spazzolatura', every=2, length=(4, 14))
    cv.noise((3, 3, W - 3, H - 3), 'grana', chance=10)
    # cornice: contorno nero e bordo di bronzo a due righe, come il bordo del piano della stazione
    cv.border((0, 0, W, H), 'black', 1)
    cv.hline(1, 1, W - 1, 'bronze', 6).vline(1, 1, H - 1, 'bronze', 6)
    cv.hline(2, 2, W - 2, 'bronze', 4).vline(2, 2, H - 2, 'bronze', 4)
    cv.hline(H - 2, 1, W - 1, 'bronze', 2).vline(W - 2, 1, H - 1, 'bronze', 2)
    cv.hline(H - 3, 2, W - 2, 'bronze', 3).vline(W - 3, 2, H - 2, 'bronze', 3)
    cv.streaks((3, 1, W - 3, 3), 'bordo', every=1, length=(3, 9))
    # pannelli
    inset(cv, G['pannello.telaio'], key='telaio')
    inset(cv, G['pannello.pezzo'], key='pezzo')
    inset(cv, G['pannello.anteprima'], 'gun', 1)
    inset(cv, G['pannello.preset'], key='preset')
    # pannello del telaio: le prese dei sei posti e della cella, una scanalatura fra i pezzi e la cella
    for name in ['pezzo.' + s for s in L.PIECE_SLOTS] + ['cella']:
        cv.border(L.grow(G[name], 1), 'black', 1)
    cv.hline(93, 10, 70, 'black', 1).hline(94, 10, 70, 'gun', 4)
    # pannello del pezzo: l'incavo della barra della durata e la vaschetta della tavolozza (con il predefinito)
    cv.border(L.grow(G['durata.barra'], 1), 'black', 0)
    tray(cv, (80, 87, 208, 111))
    # anteprima: fondo a tre fasce, piattaforma girevole di bronzo sotto i piedi, angoli di bronzo
    x0, y0, x1, y1 = G['anteprima.ritaglio']
    for y in range(y0, y1):
        band = (y - y0) * 3 // (y1 - y0)
        cv.hline(y, x0, x1, 'gun', (2, 1, 0)[band])
    cv.noise((x0, y0, x1, y1), 'anteprima', chance=16, down_only=True)
    turntable(cv, (x0 + x1) // 2, y1 - 5, 30, 4)
    for cx, cy, dx, dy in ((x0, y0, 1, 1), (x1 - 1, y0, -1, 1), (x0, y1 - 1, 1, -1), (x1 - 1, y1 - 1, -1, -1)):
        for k in range(5):
            cv.px(cx + dx * k, cy, 'bronze', 5).px(cx, cy + dy * k, 'bronze', 5)
        cv.px(cx, cy, 'bronze', 6)
    # inventario
    for r in L.inventory_slots():
        slot(cv, r)
    # preset: scanalature fra le righe, la riga dei costi come un piccolo quadrante
    for y in (166, 184):
        cv.hline(y, 177, 301, 'black', 1).hline(y + 1, 177, 301, 'gun', 4)
    tray(cv, G['riga.costi'])
    # rivetti agli angoli della piastra
    for c, r in ((5, 5), (W - 6, 5), (5, H - 6), (W - 6, H - 6)):
        cv.rivet(c, r, 'bronze')
    return cv.rgba()


def turntable(cv, cx, cy, rx, ry):
    """La piattaforma girevole su cui sta la tuta nell'anteprima: un'ellisse piatta d'acciaio con il bordo di bronzo
    (chiaro dietro, scuro davanti), dove il disegno 3D appoggia i piedi."""
    for y in range(cy - ry, cy + ry + 1):
        for x in range(cx - rx, cx + rx + 1):
            e = (x - cx) ** 2 * ry * ry + (y - cy) ** 2 * rx * rx
            lim = rx * rx * ry * ry
            if e > lim:
                continue
            if e * 100 > lim * 62:
                cv.px(x, y, 'bronze', 5 if y < cy else 3)
            else:
                cv.px(x, y, 'gun', 3 if (x + y) % 5 else 4)


# --- pulsanti ---------------------------------------------------------------------------------------------------------
def button(w, h, state, key, icon=None):
    """Un pulsante d'acciaio: normale con la faccia spazzolata e il rilievo; sopra (il mouse ci passa sopra) una
    faccia piu' chiara e il bordo di rame; premuto (la regione attiva, la scelta di tutti) una faccia di bronzo
    incassata; spento una faccia scura senza rilievo. Gli angoli sono smussati (trasparenti). Il testo lo scrive il
    codice; `icon(a, state)` disegna l'icona dei pulsanti che ne hanno una."""
    cv = Canvas(h, w, 'gui/pulsante/' + key, 'gun', 4)
    inner = (1, 1, w - 1, h - 1)
    if state == 'normale':
        cv.fill(inner, 'gun', 4).streaks(inner, 'spazzolatura', every=2, length=(3, 8)).bevel(inner, 2, -2)
        edge = ('black', 1)
    elif state == 'sopra':
        cv.fill(inner, 'gun', 5).streaks(inner, 'spazzolatura', every=2, length=(3, 8)).bevel(inner, 2, -2)
        edge = ('copper', 5)
    elif state == 'premuto':
        cv.fill(inner, 'bronze', 3).streaks(inner, 'spazzolatura', every=2, length=(3, 8)).bevel(inner, -1, 2)
        edge = ('black', 1)
    elif state == 'spento':
        cv.fill(inner, 'gun', 2).bevel(inner, 1, 0)
        edge = ('black', 1)
    else:
        raise ValueError('stato del pulsante sconosciuto: ' + state)
    cv.border((0, 0, w, h), *edge)
    a = cv.rgba()
    for x, y in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)):
        a[y, x] = 0
    if icon is not None:
        icon(a, state)
    return a


def icon_colours(state):
    """Colori di un'icona su un pulsante: chiara, piu' chiara sopra, scura e senza ombra da spento."""
    if state == 'spento':
        return rgb('gun', 4), None
    if state == 'sopra':
        return rgb('chrome', 6), rgb('gun', 1)
    return rgb('steel', 7), rgb('gun', 1)


def draw_icon(a, rows, state, x, y, extra=None):
    """Un'icona da una griglia ('#' nel colore dell'icona, altre lettere da `extra`), con l'ombra di un pixel in basso
    a destra; premuto, scende di un pixel."""
    main, shadow = icon_colours(state)
    if state == 'premuto':
        y += 1
    legend = {'#': (main, 255)}
    for ch, (mat, lev, dim) in (extra or {}).items():
        legend[ch] = ((rgb('gun', dim) if state == 'spento' else rgb(mat, lev)), 255)
    h, w = a.shape[:2]
    if shadow is not None:
        for r, row in enumerate(rows):
            for c, ch in enumerate(row):
                sy, sx = y + r + 1, x + c + 1
                if ch != '.' and 1 <= sx < w - 1 and 1 <= sy < h - 1:
                    a[sy, sx, :3] = shadow
                    a[sy, sx, 3] = 255
    if y + len(rows) > h - 1 or x + len(rows[0]) > w - 1:
        raise ValueError('icona fuori dalla faccia del pulsante')
    return stamp(a, rows, legend, x, y)


EJECT = ('..##..',
         '.####.',
         '######',
         '......',
         '######')

BRUSH = ('.rr.',
         'rrrr',
         'wwww',
         'wwww',
         '####',
         'ssss',
         '.bb.',
         '.bb.',
         '.bb.',
         '.bb.',
         '.dd.')
BRUSH_EXTRA = {'r': ('red', 5, 3), 'w': ('oak', 6, 4), 's': ('steel', 4, 3), 'b': ('bronze', 5, 3),
               'd': ('bronze', 3, 2)}

BOOK = ('tBBBBBBb',
        'dBBBBBBb',
        'dBccccBb',
        'dBBBBBBb',
        'dBBBBBBb',
        'dBBBBBBb',
        'dBBBBBBb',
        'dBBBBBBb',
        'd#######',
        '..r.....',
        '..r.....')
BOOK_EXTRA = {'t': ('bronze', 3, 3), 'B': ('bronze', 5, 3), 'b': ('bronze', 3, 2), 'd': ('bronze', 2, 2),
              'c': ('copper', 6, 4), 'r': ('red', 5, 3)}

LOCK_CLOSED = ('..ssss..',
               '.s....s.',
               '.s....s.',
               'YYYYYYYY',
               'YYYkkYYY',
               'YYYkkYYY',
               'YYYYYYYY',
               'yyyyyyyy')
LOCK_OPEN = ('..ssss..',          # l'arco alzato di un pixel, la gamba destra fuori dal corpo
             '.s....s.',
             '.s......',
             '.s......',
             'YYYYYYYY',
             'YYYkkYYY',
             'YYYkkYYY',
             'YYYYYYYY',
             'yyyyyyyy')
LOCK_EXTRA = {'s': ('steel', 6, 4), 'Y': ('brass', 5, 3), 'y': ('brass', 3, 2), 'k': ('black', 1, 1)}


def eject_icon(a, state):
    return draw_icon(a, EJECT, state, 3, 3)


def brush_icon(a, state):
    return draw_icon(a, BRUSH, state, 7, 2, BRUSH_EXTRA)


def book_icon(a, state):
    return draw_icon(a, BOOK, state, 5, 2, BOOK_EXTRA)


def lock_icon(rows):
    """Il lucchetto, con il corpo sempre nelle stesse righe: aperto, l'arco parte una riga piu' in alto."""
    def draw(a, state):
        return draw_icon(a, rows, state, 4, 10 - len(rows), LOCK_EXTRA)
    return draw


# --- altri disegni della schermata ------------------------------------------------------------------------------------
def place(edge, empty=False):
    """Un posto del pannello del telaio, 20x20: il bordo di 1 pixel del colore dello stato (esatto: la legenda,
    il rosso della cella, il grigio di un posto vuoto) e dentro lo slot incassato dove il codice mette l'icona a +2."""
    a = blank(20, 20)
    cv = Canvas(18, 18, 'gui/posto', 'gun', 1)
    if empty:
        cv.fill((1, 1, 17, 17), 'black', 2)
    cv.hline(0, 0, 18, 'black', 0).vline(0, 0, 18, 'black', 0)
    cv.hline(17, 1, 18, 'gun', 4).vline(17, 1, 18, 'gun', 4)
    paste(a, cv.rgba(), 1, 1)
    return ring(a, (0, 0, 20, 20), edge)


GHOSTS = {
    'elmo': ('................',
             '................',
             '.....######.....',
             '....########....',
             '...##########...',
             '...##########...',
             '..############..',
             '..#..........#..',
             '..############..',
             '..############..',
             '..####....####..',
             '..####....####..',
             '..####....####..',
             '................',
             '................',
             '................'),
    'torso': ('................',
              '..###......###..',
              '.#####....#####.',
              '.##############.',
              '.##############.',
              '....########....',
              '....###..###....',
              '....###..###....',
              '....########....',
              '....########....',
              '....########....',
              '....########....',
              '....########....',
              '................',
              '................',
              '................'),
    'braccio': ('................',
                '.....######.....',
                '....########....',
                '....########....',
                '.....######.....',
                '.....######.....',
                '.....######.....',
                '......####......',
                '.....######.....',
                '.....######.....',
                '.....######.....',
                '....########....',
                '....##.##.##....',
                '................',
                '................',
                '................'),
    'gamba': ('................',
              '......######....',
              '......######....',
              '......######....',
              '......######....',
              '......######....',
              '.......####.....',
              '......######....',
              '......######....',
              '......######....',
              '......######....',
              '......######....',
              '......#########.',
              '......#########.',
              '................',
              '................'),
    'cella': ('................',
              '.....######.....',
              '....########....',
              '....########....',
              '....##....##....',
              '....########....',
              '....##....##....',
              '....########....',
              '....##....##....',
              '....########....',
              '....########....',
              '....########....',
              '.....######.....',
              '................',
              '................',
              '................'),
}


def ghost(name):
    """L'icona in trasparenza del tipo di pezzo in un posto vuoto (design par. 5.3): la sagoma in acciaio chiaro al
    25 %, come un'incisione sul fondo scuro."""
    return stamp(blank(16, 16), GHOSTS[name], {'#': (rgb('steel', 6), 64)})


def shield():
    """Lo scudo di bronzo del telaio rinforzato, 14x12: scudo a punta con la costa al centro, chiaro a sinistra."""
    a = blank(14, 12)
    inside = np.zeros((12, 14), bool)
    for y in range(12):
        k = max(0, y - 6)
        inside[y, k:14 - k] = True
    for y in range(12):
        for x in range(14):
            if not inside[y, x]:
                continue
            edge = (y == 0 or x == 0 or x == 13 or not inside[min(y + 1, 11), x] or y == 11
                    or not inside[y, x - 1] or not inside[y, min(x + 1, 13)])
            if edge:
                c = rgb('black', 1)
            elif x < 7:
                c = rgb('bronze', 6 if y == 1 or x == 1 else 5)
            else:
                c = rgb('bronze', 3 if x < 12 else 2)
            a[y, x, :3] = c
            a[y, x, 3] = 255
    for y in range(1, 11):
        if inside[y, 6] and a[y, 6, 3] and tuple(a[y, 6, :3]) != rgb('black', 1):
            a[y, 6, :3] = rgb('bronze', 7)
    a[3, 3, :3] = rgb('bronze', 7)
    a[3, 10, :3] = rgb('bronze', 5)
    return a


def material(spento=False):
    """Il riquadro del materiale, a forma di slot (nessun oggetto dentro): bordo di bronzo, da spento d'acciaio."""
    cv = Canvas(18, 18, 'gui/materiale', 'gun', 1)
    if spento:
        cv.fill((1, 1, 17, 17), 'black', 2)
        cv.hline(0, 0, 18, 'black', 1).vline(0, 0, 18, 'black', 1)
        cv.hline(17, 1, 18, 'gun', 3).vline(17, 1, 18, 'gun', 3)
    else:
        cv.hline(0, 0, 18, 'bronze', 2).vline(0, 0, 18, 'bronze', 2)
        cv.hline(17, 1, 18, 'bronze', 5).vline(17, 1, 18, 'bronze', 5)
    return cv.rgba()


def veil():
    """Il velo che spegne l'icona del materiale quando il giocatore non ne ha (16x16, sopra l'icona)."""
    return fill(blank(16, 16), (0, 0, 16, 16), rgb('black', 1), 150)


ARROW = ('....#',
         '...##',
         '..###',
         '.####',
         '..###',
         '...##',
         '....#')


def arrow(right, hover):
    rows = tuple(row[::-1] for row in ARROW) if right else ARROW
    return stamp(blank(5, 7), rows, {'#': (rgb('bronze', 7 if hover else 5), 255)})


def swatch_frame():
    """La cornice di una tinta, 12x10: il codice riempie prima il riquadro con il colore della tinta, poi ci mette sopra
    questa cornice incassata, con un riflesso traslucido in alto e un'ombra in basso (la finestra 10x8 resta del colore
    esatto nelle sei righe di mezzo)."""
    a = blank(12, 10)
    bevel_ring(a, (0, 0, 12, 10), rgb('black', 0), rgb('gun', 5))
    fill(a, (1, 1, 11, 2), (255, 255, 255), 52)
    fill(a, (1, 8, 11, 9), (0, 0, 0), 48)
    return a


def swatch_off():
    """Il velo di una tinta che il giocatore non ha: righe oblique scure sopra il colore, la cornice resta."""
    a = blank(12, 10)
    for y in range(1, 9):
        for x in range(1, 11):
            a[y, x, :3] = rgb('black', 1)
            a[y, x, 3] = 210 if (x + y) % 3 == 0 else 120
    return a


def hover_ring(w, h):
    return ring(blank(w, h), (0, 0, w, h), rgb('copper', 6))


def default_frame():
    """Il predefinito, 14x22: due finestre (primario sopra, secondario sotto) che il codice riempie con i colori del
    livello prima di metterci sopra questa cornice, con lo stesso riflesso delle tinte."""
    a = blank(14, 22)
    bevel_ring(a, (0, 0, 14, 22), rgb('black', 0), rgb('gun', 5))
    fill(a, (1, 11, 13, 12), rgb('black', 0))
    for top, bottom in ((1, 10), (12, 20)):
        fill(a, (1, top, 13, top + 1), (255, 255, 255), 52)
        fill(a, (1, bottom, 13, bottom + 1), (0, 0, 0), 48)
    return a


def preset_strip(empty):
    """Il fondo dei dodici colori di un preset, 74x10 (i colori 5x8 li mette il codice, a passo 6); vuoto, un fondo
    tratteggiato su cui il codice scrive "vuoto"."""
    cv = Canvas(10, 74, 'gui/preset', 'black', 2)
    if empty:
        cv.fill((1, 1, 73, 9), 'black', 1)
        for y in range(1, 9):
            for x in range(1, 73):
                if (x - y) % 4 == 0:
                    cv.px(x, y, 'gun', 1)
    cv.hline(0, 0, 74, 'black', 0).vline(0, 0, 10, 'black', 0)
    cv.hline(9, 1, 74, 'gun', 5).vline(73, 1, 10, 'gun', 5)
    return cv.rgba()


def bar_back(w):
    """Il fondo della barra della durata, w x 5: un canale scuro con le tacche dei quarti."""
    cv = Canvas(5, w, 'gui/durata', 'black', 2)
    cv.hline(0, 0, w, 'black', 0).hline(4, 0, w, 'gun', 3)
    for k in (1, 2, 3):
        cv.vline(k * w // 4, 1, 4, 'gun', 1)
    return cv.rgba()


def bar_fill(w, h, colour):
    """Un riempimento di barra: il colore esatto, con la prima riga piu' chiara (come la barra dell'HUD)."""
    a = fill(blank(w, h), (0, 0, w, h), colour)
    return fill(a, (0, 0, w, 1), L.lighter(colour))


def screen_sheet():
    """La texture della schermata, 512x256: lo sfondo in (0, 0) e i disegni dei widget dove dice L.STATION_ATLAS; il
    resto trasparente."""
    W, H = L.STATION_TEX
    sheet = np.zeros((H, W, 4), np.uint8)
    parts = {'fondo': background()}
    icons = {'tutti': None, 'ripara': None, 'ripara_tutto': None, 'regione': None, 'applica': brush_icon,
             'salva': book_icon, 'smonta': eject_icon, 'blocco.chiuso': lock_icon(LOCK_CLOSED),
             'blocco.aperto': lock_icon(LOCK_OPEN)}
    for key, icon in icons.items():
        for st in L.BUTTON_STATES:
            _, _, w, h = L.STATION_ATLAS['%s.%s' % (key, st)]
            parts['%s.%s' % (key, st)] = button(w, h, st, key, icon)
    parts['rinforzato'] = shield()
    parts['materiale'] = material()
    parts['materiale.spento'] = material(spento=True)
    parts['materiale.velo'] = veil()
    for side in ('sinistra', 'destra'):
        parts['freccia.' + side] = arrow(side == 'destra', False)
        parts['freccia.%s.sopra' % side] = arrow(side == 'destra', True)
    parts['scelta'] = ring(blank(22, 22), (0, 0, 22, 22), L.WHITE)
    parts['tinta.sopra'] = hover_ring(14, 12)
    for st, colour in L.CONDITIONS:
        parts['posto.' + st] = place(colour)
    parts['posto.vuoto'] = place(L.UNMOUNTED_OUTLINE, empty=True)
    parts['posto.cella'] = place(L.CELL_RED)
    for name in GHOSTS:
        parts['fantasma.' + name] = ghost(name)
    parts['tinta'] = swatch_frame()
    parts['tinta.spenta'] = swatch_off()
    parts['predefinito'] = default_frame()
    parts['predefinito.sopra'] = hover_ring(16, 24)
    parts['preset.colori'] = preset_strip(False)
    parts['preset.vuoto'] = preset_strip(True)
    bar_w = L.size(L.GUI['durata.barra'])[0]
    parts['durata.fondo'] = bar_back(bar_w)
    for st, colour in L.CONDITIONS + (('cella', L.CELL_RED),):
        parts['durata.' + st] = bar_fill(bar_w, 5, colour)
    for name, (u, v, w, h) in L.STATION_ATLAS.items():
        img = parts.pop(name)
        if img.shape != (h, w, 4):
            raise ValueError(f'{name}: disegno {img.shape[1]}x{img.shape[0]} invece di {w}x{h}')
        paste(sheet, img, u, v)
    if parts:
        raise ValueError('disegni senza posto nella texture: ' + ', '.join(sorted(parts)))
    return sheet


# --- HUD --------------------------------------------------------------------------------------------------------------
PLATE_ALPHA = 0x99            # la piastra resta nera al 60 % come nel design
EDGE_ALPHA = 0xB4             # il bordo di rame, piu' deciso del 43 % del design (scelta della 4d)


def plate(w, h):
    """La piastra di fondo: l'acciaio piu' scuro al 60 %, il bordo di rame a rilievo e un rivetto di bronzo in ogni
    angolo, fuori dai rettangoli degli elementi."""
    a = fill(blank(w, h), (1, 1, w - 1, h - 1), rgb('gun', 0), PLATE_ALPHA)
    bevel_ring(a, (0, 0, w, h), rgb('copper', 5), rgb('copper', 3), EDGE_ALPHA)
    for x, y in ((2, 2), (w - 3, 2), (2, h - 3), (w - 3, h - 3)):
        fill(a, (x + 1, y + 1, x + 2, y + 2), rgb('black', 0), 0xD0)
        fill(a, (x, y, x + 1, y + 1), rgb('bronze', 6))
    return a


HUD_SHAPES = {
    'helmet': ('.oooo.',
               'offffo',
               'offffo',
               'offffo',
               'offffo',
               'oooooo'),
    'torso': ('oooooooo',
              'offffffo',
              'offffffo',
              'offffffo',
              'offffffo',
              'offffffo',
              'offffffo',
              'offffffo',
              '.offffo.',
              '.offffo.',
              '.oooooo.'),
    'arm': ('ooo',) + ('ofo',) * 9 + ('ooo',),
}
HUD_SHAPES['left_arm'] = HUD_SHAPES['right_arm'] = HUD_SHAPES['left_leg'] = HUD_SHAPES['right_leg'] = HUD_SHAPES['arm']


def silhouette(state):
    """La figura 21x35 di uno stato, con i sei pezzi ai loro posti del sottogruppo della sagoma: pieni del colore
    esatto della condizione con il contorno di 1 pixel a un terzo; il non montato con il fondo scuro e il contorno
    grigio del design. L'elmo ha gli angoli in alto smussati, il torso si stringe in vita."""
    a = blank(L.SIL_W, L.GROUP_H)
    if state == 'assente':
        legend = {'f': ((0, 0, 0), L.UNMOUNTED_FILL[3]), 'o': (L.UNMOUNTED_OUTLINE, 255)}
    else:
        colour = dict(L.CONDITIONS)[state]
        legend = {'f': (colour, 255), 'o': (L.third(colour), 255)}
    for slot_name, (x0, y0, x1, y1) in L.SIL_PIECES.items():
        rows = HUD_SHAPES[slot_name]
        if (len(rows[0]), len(rows)) != (x1 - x0, y1 - y0):
            raise ValueError(f'forma di {slot_name} {len(rows[0])}x{len(rows)} invece di {x1 - x0}x{y1 - y0}')
        stamp(a, rows, legend, x0, y0)
    return a


def warning(colour):
    """L'avviso 8x8: il triangolo a gradini nel colore dell'energia, il punto esclamativo d'acciaio scuro."""
    a = blank(8, 8)
    for y in range(8):
        fill(a, (3 - y // 2, y, 5 + y // 2, y + 1), colour)
    for y in (2, 3, 4, 6):
        fill(a, (3, y, 5, y + 1), rgb('black', 1))
    return a


CELL_ICON = ('.sss.',
             'kccck',
             'kRRrk',
             'kRRrk',
             'kRRrk',
             'kRRrk',
             'kccck',
             'kkkkk')


def cell_icon(dim):
    """L'icona della cella di scorta, 5x8: tappo d'acciaio, corpo di rame, la carica rossa; spenta piu' scura."""
    legend = {'s': (rgb('steel', 3 if dim else 6), 255), 'c': (rgb('copper', 1 if dim else 4), 255),
              'R': (rgb('red', 1 if dim else 5), 255), 'r': (rgb('red', 0 if dim else 3), 255),
              'k': (rgb('black', 1), 255)}
    return stamp(blank(5, 8), CELL_ICON, legend)


def bar_frame():
    """La cornice della barra dell'energia, 63x8: canale d'acciaio incassato (ombra nera in alto e a sinistra, labbro
    chiaro in basso e a destra) con le tacche dei quarti, che il riempimento copre."""
    a = fill(blank(63, 8), (1, 1, 62, 7), rgb('gun', 2))
    for k in (1, 2, 3):
        fill(a, (1 + k * 61 // 4, 5, 2 + k * 61 // 4, 7), rgb('gun', 4))
    return bevel_ring(a, (0, 0, 63, 8), rgb('black', 1), rgb('gun', 4))


def hud_sheet():
    W, H = L.HUD_TEX
    sheet = np.zeros((H, W, 4), np.uint8)
    parts = {'piastra': plate(L.GROUP_W, L.GROUP_H), 'piastra.barra': plate(L.ENERGY_W, L.GROUP_H),
             'barra.cornice': bar_frame(), 'avviso.rossa': warning(L.RED), 'avviso.grigia': warning(L.GREY),
             'cella': cell_icon(False), 'cella.spenta': cell_icon(True)}
    for st in L.SILHOUETTE_STATES:
        parts['sagoma.' + st] = silhouette(st)
    for name, colour in L.ENERGY:
        parts['barra.' + name] = bar_fill(L.FILL_MAX, 6, colour)
    for name, (u, v, w, h) in L.HUD_ATLAS.items():
        img = parts.pop(name)
        if img.shape != (h, w, 4):
            raise ValueError(f'{name}: disegno {img.shape[1]}x{img.shape[0]} invece di {w}x{h}')
        paste(sheet, img, u, v)
    if parts:
        raise ValueError('disegni senza posto nella texture: ' + ', '.join(sorted(parts)))
    return sheet


# --- vignetta del casco (144x80) --------------------------------------------------------------------------------------
STEEL_EDGE = (20, 22, 26)     # il bordo della cornice dell'elmo: l'acciaio piu' scuro (gun 0)
GLASS = (24, 44, 52)          # il vetro dell'elmo (paint.py, materiale glass)
GLASS_GLINT = (190, 230, 230)  # il riflesso chiaro del vetro dell'elmo (paint.py)
GLASS_SHINE = (236, 250, 250)  # lo stesso riflesso piu' vicino al bianco: deve restare piu' chiaro del cielo
# (inizio, lunghezza, colore, alfa) di ogni riflesso: il texel del riflesso prende il posto dell'oscuramento, cosi'
# resta piu' chiaro di quello che ha attorno anche sul cielo di giorno (composto sopra, sarebbe una riga scura)
GLINTS = ((14, 24, GLASS_SHINE, 58), (25, 17, GLASS_GLINT, 42))


def vignette():
    """I quattro angoli 72x40, ciascuno al suo posto nella texture (alto a sinistra, alto a destra; basso a sinistra,
    basso a destra), come li disegna SuitHud. In ogni angolo l'oscuramento dal pixel d'angolo: opacita'
    107 * (1 - d)^2 (107 = 0,42 * 255 nel pixel d'angolo), con d la distanza da quel pixel in larghezze (71) e altezze
    (39) dell'angolo, quindi zero sui due lati opposti del pezzo; il colore va dall'acciaio del bordo della cornice,
    vicino all'angolo, al verde scuro del vetro dell'elmo. Nei due angoli in alto due riflessi del vetro, strisce
    oblique a gradini nei colori del riflesso del vetro, al posto dell'oscuramento. Solo interi."""
    W, H = L.VIGNETTE_TEX
    vw, vh = L.VIGNETTE_W, L.VIGNETTE_H
    S = (vw - 1) * (vh - 1)
    a = blank(W, H)
    for cx, cy in ((0, 0), (W - 1, 0), (0, H - 1), (W - 1, H - 1)):
        x0 = 0 if cx == 0 else vw
        y0 = 0 if cy == 0 else vh
        for y in range(y0, y0 + vh):
            for x in range(x0, x0 + vw):
                u, v = abs(x - cx), abs(y - cy)
                r = isqrt(u * u * (vh - 1) ** 2 + v * v * (vw - 1) ** 2)
                if r >= S:
                    continue
                alpha = L.VIGNETTE_MAX_ALPHA * (S - r) ** 2 // (S * S)
                t = min(256, r * 512 // S)
                colour = tuple((e * (256 - t) + g * t) >> 8 for e, g in zip(STEEL_EDGE, GLASS))
                a[y, x, :3] = colour
                a[y, x, 3] = alpha
    for start, length, colour, alpha in GLINTS:
        for t in range(length):
            u, v = start + t, 3 + t // 2
            for x in (u, W - 1 - u):
                a[v, x, :3] = colour
                a[v, x, 3] = alpha
    return a


# --- icona del kit di riparazione (16x16) -----------------------------------------------------------------------------
# Una cassetta degli attrezzi vista a tre quarti come l'icona della stazione (station_designs/officina.py, ICON): lo
# stesso contorno nero, il coperchio di bronzo come il piano della stazione, il corpo d'acciaio scuro con il fianco a
# destra, due chiusure di rame, la maniglia d'acciaio e una fascia dipinta di rosso redstone sul fronte.
KIT_ICON = (
    '................',
    '.....kkkkkk.....',
    '....kSssssSk....',
    '....kk....kk....',
    '...kkkkkkkkkkkk.',
    '..kBBBBBBBBBBBkk',
    '.kBbbbbbbbbbBkgk',
    'kOOOOOOOOOOOkggk',
    'kGcGGGGGGGcGkggk',
    'kGcGGGGGGGcGkggk',
    'kRRRRRRRRRRRkggk',
    'krrrrrrrrrrrkgk.',
    'kGGGGGGGGGGGkgk.',
    'kPPPPPPPPPPPkk..',
    'kkkkkkkkkkkkk...',
    '................',
)


def kit_icon():
    legend = {
        'k': ('black', 1), 'B': ('bronze', 5), 'b': ('bronze', 3), 'O': ('bronze', 4), 'G': ('gun', 4),
        'g': ('gun', 3), 'R': ('red', 5), 'r': ('red', 3), 'c': ('copper', 5), 'S': ('steel', 6), 's': ('steel', 4),
        'P': ('gun', 2),
    }
    return sprite(KIT_ICON, legend)
