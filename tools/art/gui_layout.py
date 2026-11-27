"""Geometria della schermata della stazione e dell'HUD (design M4, par. 5.3, 5.12 e 12) e posto di ogni disegno nelle
texture della parte 4d (stile A, Officina): l'unica fonte dei numeri per tools/art/gui.py, che disegna, e per
tools/art/preview/gui_preview.py, che compone le anteprime ritagliando i disegni dalle PNG scritte.

Rettangoli dello schermo: (x0, y0, x1, y1) in pixel dell'interfaccia, relativi all'angolo della schermata o del
sottogruppo dell'HUD, con x1 e y1 esclusi, come in docs/superpowers/specs/anteprime/2026-11-19-m4/layout.py,
client/StationLayout.java e client/HudLayout.java (i nomi vengono da layout.py). Posti nelle texture: (u, v, w, h),
in texel, con l'angolo in alto a sinistra.
"""

# --- schermata della stazione (design par. 5.3; layout.py, GUI) -------------------------------------------------------
SCREEN_W, SCREEN_H = 316, 232

GUI = {
    'titolo': (8, 6, 140, 15),
    'rinforzato': (276, 3, 290, 15),
    'blocco': (292, 3, 308, 15),
    'pannello.telaio': (7, 17, 73, 135),
    'pezzo.helmet': (30, 21, 50, 41),
    'pezzo.right_arm': (9, 43, 29, 63),
    'pezzo.torso': (30, 43, 50, 63),
    'pezzo.left_arm': (51, 43, 71, 63),
    'pezzo.right_leg': (19, 65, 39, 85),
    'pezzo.left_leg': (41, 65, 61, 85),
    'cella': (19, 101, 39, 121),
    'tutti': (41, 103, 71, 119),
    'pannello.pezzo': (77, 17, 211, 135),
    'nome': (81, 22, 191, 31),
    'smonta': (195, 20, 207, 32),
    'condizione': (81, 34, 150, 43),
    'durata.numeri': (152, 34, 207, 43),
    'durata.barra': (81, 44, 207, 49),
    'materiale': (81, 52, 99, 70),
    'ripara': (102, 53, 146, 69),
    'ripara_tutto': (148, 53, 207, 69),
    'regione.primario': (81, 73, 143, 85),
    'regione.secondario': (145, 73, 207, 85),
    'tavolozza': (81, 88, 191, 110),
    'predefinito': (193, 88, 207, 110),
    'moduli.riservato': (81, 113, 171, 131),
    'pannello.anteprima': (215, 17, 309, 135),
    'anteprima.ritaglio': (216, 18, 308, 134),
    'etichetta.inventario': (8, 139, 80, 148),
    'inventario': (7, 149, 169, 203),
    'barra': (7, 207, 169, 225),
    'pannello.preset': (175, 138, 309, 225),
    'etichetta.preset': (177, 140, 240, 149),
    'preset.0': (177, 150, 301, 166),
    'preset.1': (177, 168, 301, 184),
    'preset.2': (177, 186, 301, 202),
    'riga.costi': (177, 205, 307, 224),
}

# I sei posti dei pezzi, nell'ordine di PieceSlot (elmo, torso, braccio sinistro, braccio destro, gamba sinistra, gamba
# destra), con il pannello visto di fronte come l'anteprima: il braccio destro del telaio a sinistra.
PIECE_SLOTS = ('helmet', 'torso', 'left_arm', 'right_arm', 'left_leg', 'right_leg')
SLOT = 18                     # uno slot del gioco base con la sua cornice; l'oggetto 16x16 sta a +1
ICON_INSET = 2                # in un posto del pannello del telaio l'icona sta a +2 (bordo e cornice dello slot)

# Le frecce dell'anteprima (design: "con due frecce in basso"): oggi due caratteri "<" e ">" a 3 pixel dai bordi del
# ritaglio, in fondo (misurati negli scatti della 4a-1: 219-222 e 300-303, righe 122-128); qui due disegni di 5x7 negli
# stessi punti (scelta della 4d).
ARROWS = {'sinistra': (219, 122, 224, 129), 'destra': (300, 122, 305, 129)}


def swatch(i):
    """Il riquadro della tinta i (ordine di DyeColor) nella tavolozza: due file da otto, passo 14 e 12."""
    col, row = i % 8, i // 8
    x, y = 81 + col * 14, 88 + row * 12
    return (x, y, x + 12, y + 10)


def preset_parts(k):
    """Le parti della riga del preset k (0-2): numero, dodici colori 5x8 (passo 6), applica, salva."""
    x0, y0, x1, y1 = GUI['preset.%d' % k]
    return {
        'numero': (x0, y0 + 4, x0 + 6, y0 + 12),
        'colori': (x0 + 8, y0 + 4, x0 + 80, y0 + 12),
        'applica': (x0 + 84, y0, x0 + 102, y0 + 16),
        'salva': (x0 + 106, y0, x0 + 124, y0 + 16),
    }


def preset_cell(k, i):
    """Il colore i (0-11: primario e secondario dei sei pezzi, nell'ordine di PieceSlot) della riga k, 5x8."""
    x0, y0, _, y1 = preset_parts(k)['colori']
    return (x0 + 6 * i, y0, x0 + 6 * i + 5, y1)


def inventory_slots():
    """Le cornici 18x18 degli slot del giocatore: tre file dell'inventario, poi la barra (il menu mette gli oggetti a
    +1, come il gioco base)."""
    out = []
    ix, iy = GUI['inventario'][:2]
    for row in range(3):
        for col in range(9):
            out.append((ix + col * SLOT, iy + row * SLOT, ix + col * SLOT + SLOT, iy + row * SLOT + SLOT))
    hx, hy = GUI['barra'][:2]
    for col in range(9):
        out.append((hx + col * SLOT, hy, hx + col * SLOT + SLOT, hy + SLOT))
    return out


def grow(r, by):
    x0, y0, x1, y1 = r
    return (x0 - by, y0 - by, x1 + by, y1 + by)


def size(r):
    return (r[2] - r[0], r[3] - r[1])


def union(a, b):
    """Il rettangolo piu' piccolo che contiene a e b."""
    return (min(a[0], b[0]), min(a[1], b[1]), max(a[2], b[2]), max(a[3], b[3]))


# I rettangoli dei widget attorno a cui lo sfondo della schermata (gui_officina.background) cuoce qualcosa, e solo
# questi: i quattro pannelli incassati, le prese dei sei posti e della cella, l'incavo della barra della durata, la
# vaschetta attorno alla tavolozza e al predefinito, il fondo dell'anteprima, le cornici degli slot dell'inventario e
# della barra, le scanalature sotto le prime due righe dei preset, la riga dei costi. gui.py --map-json li scrive in
# tools/art/gui_map.json, e GuiMapTest li confronta con i rettangoli di client/StationLayout: un widget spostato qui
# va spostato anche li', e quelli che StationLayout non ha ancora (della parte 4b) il test li elenca per nome.
BACKGROUND = {name: GUI[name] for name in (
    'pannello.telaio', 'pannello.pezzo', 'pannello.anteprima', 'pannello.preset',
    'pezzo.helmet', 'pezzo.torso', 'pezzo.left_arm', 'pezzo.right_arm', 'pezzo.left_leg', 'pezzo.right_leg', 'cella',
    'durata.barra', 'tavolozza', 'predefinito', 'anteprima.ritaglio', 'inventario', 'barra', 'preset.0', 'preset.1',
    'riga.costi')}


# --- HUD (design par. 5.12; HudLayout) --------------------------------------------------------------------------------
GROUP_W, GROUP_H = 88, 35     # il gruppo, con FULL la piastra
SIL_W, ENERGY_W = 21, 67      # i due sottogruppi: la sagoma verso il bordo, l'energia verso l'interno
SIL_PIECES = {                # relativi al sottogruppo della sagoma, vista da dietro (il braccio sinistro a sinistra)
    'helmet': (8, 3, 14, 9),
    'torso': (7, 10, 15, 21),
    'left_arm': (3, 10, 6, 21),
    'right_arm': (16, 10, 19, 21),
    'left_leg': (7, 22, 10, 33),
    'right_leg': (12, 22, 15, 33),
}
WARNING = (2, 8, 10, 16)      # relativi al sottogruppo dell'energia
TEXT_X, TEXT_X_AFTER_WARNING, TEXT_Y = 2, 12, 8
SPARE_RIGHT = 65              # bordo destro, escluso, di "xN"; l'icona della cella sta a x(testo) - 1 - 5
CELL_ICON_W, CELL_ICON_H = 5, 8
BAR_FRAME = (2, 20, 65, 28)
BAR_FILL = (3, 21, 64, 27)
FILL_MAX = 61
NO_CELL = (2, 20)
VIGNETTE_W, VIGNETTE_H = 72, 40


def hud_group(w, h, main_right):
    """Il gruppo nell'interfaccia w x h (HudLayout.group): in basso nell'angolo opposto alla mano principale, in alto a
    sinistra sotto i 424 pixel di larghezza."""
    if w < 424:
        return (2, 2, 2 + GROUP_W, 2 + GROUP_H)
    x0 = 2 if main_right else w - 2 - GROUP_W
    y0 = h - 37
    return (x0, y0, x0 + GROUP_W, y0 + GROUP_H)


def hud_subgroups(w, h, main_right):
    """(sagoma, energia): la sagoma verso il bordo, a destra con la mano sinistra (l'interno non si specchia)."""
    x0, y0, x1, y1 = hud_group(w, h, main_right)
    if main_right or w < 424:
        return (x0, y0, x0 + SIL_W, y1), (x1 - ENERGY_W, y0, x1, y1)
    return (x1 - SIL_W, y0, x1, y1), (x0, y0, x0 + ENERGY_W, y1)


def vignette_corners(w, h):
    """Alto a sinistra, alto a destra, basso a sinistra, basso a destra: nella texture l'angolo i sta nella colonna
    i % 2 e nella riga i // 2 (SuitHud.drawVignette)."""
    return [(0, 0, VIGNETTE_W, VIGNETTE_H), (w - VIGNETTE_W, 0, w, VIGNETTE_H),
            (0, h - VIGNETTE_H, VIGNETTE_W, h), (w - VIGNETTE_W, h - VIGNETTE_H, w, h)]


# --- colori che hanno un significato: la legenda dell'HUD e della stazione (HudModel, StationLayout) ------------------
GREEN = (0x55, 0xFF, 0x55)    # va bene: integro, energia NORMAL
YELLOW = (0xFF, 0xFF, 0x55)   # danneggiato
RED = (0xFF, 0x55, 0x55)      # agire subito: critico, energia LOW
GREY = (0x8A, 0x8A, 0x8A)     # fuori servizio: rotto, energia EMPTY
WHITE = (0xFF, 0xFF, 0xFF)    # energia prima del primo pacchetto con gli indicatori; il secondo bordo della scelta
CONDITIONS = (('integro', GREEN), ('danneggiato', YELLOW), ('critico', RED), ('rotto', GREY))
ENERGY = (('verde', GREEN), ('rossa', RED), ('grigia', GREY), ('bianca', WHITE))
UNMOUNTED_FILL = (0, 0, 0, 0x5A)      # pezzo non montato nella sagoma: fondo scuro
UNMOUNTED_OUTLINE = (0x46, 0x46, 0x46)  # e contorno grigio, che e' anche il bordo di un posto vuoto della stazione
CELL_RED = (0xC8, 0x1E, 0x1E)         # il rosso della redstone: bordo del posto della cella, carica


def third(c):
    """Il contorno di un pezzo della sagoma: ogni canale diviso per 3 (HudModel.outline)."""
    return tuple(v // 3 for v in c)


def lighter(c):
    """La prima riga del riempimento della barra: ogni canale a meta' strada verso il bianco (HudModel.lighter)."""
    return tuple(v + (255 - v) // 2 for v in c)


# Le sedici tinte del gioco base nell'ordine di DyeColor (design par. 5.9): i colori li mette il codice (DyePalette),
# la texture ha solo la cornice, il velo "spento" e l'anello del passaggio del mouse. Qui servono alle anteprime.
DYES = (('bianco', 0xF9FFFE), ('arancione', 0xF9801D), ('magenta', 0xC74EBD), ('azzurro', 0x3AB3DA),
        ('giallo', 0xFED83D), ('lime', 0x80C71F), ('rosa', 0xF38BAA), ('grigio', 0x474F52),
        ('grigio chiaro', 0x9D9D97), ('ciano', 0x169C9C), ('viola', 0x8932B8), ('blu', 0x3C44AA),
        ('marrone', 0x835432), ('verde', 0x5E7C16), ('rosso', 0xB02E26), ('nero', 0x1D1D21))
SCRAP_COLOURS = (0x8A6F52, 0x4A4F55)  # il colore del livello Rottame, primario e secondario (TierPalette)

# --- texture della schermata: textures/gui/station.png ----------------------------------------------------------------
STATION_TEX = (512, 256)
BUTTON_STATES = ('normale', 'sopra', 'premuto', 'spento')   # sopra = il mouse ci passa sopra


def _station_atlas():
    a = {'fondo': (0, 0, SCREEN_W, SCREEN_H)}
    # pulsanti alti 16, uno per colonna, i quattro stati in colonna
    for name, u, w in (('tutti', 318, 30), ('ripara', 350, 44), ('ripara_tutto', 396, 59), ('applica', 457, 18),
                       ('salva', 477, 18)):
        for i, st in enumerate(BUTTON_STATES):
            a['%s.%s' % (name, st)] = (u, 2 + 16 * i, w, 16)
    a['rinforzato'] = (497, 2, 14, 12)
    # pulsanti alti 12
    for name, u, w in (('regione', 318, 62), ('smonta', 382, 12), ('blocco.chiuso', 396, 16),
                       ('blocco.aperto', 414, 16)):
        for i, st in enumerate(BUTTON_STATES):
            a['%s.%s' % (name, st)] = (u, 68 + 12 * i, w, 12)
    a['materiale'] = (432, 68, 18, 18)
    a['materiale.spento'] = (432, 88, 18, 18)
    a['materiale.velo'] = (452, 68, 16, 16)
    a['freccia.sinistra'] = (470, 68, 5, 7)
    a['freccia.destra'] = (477, 68, 5, 7)
    a['freccia.sinistra.sopra'] = (470, 77, 5, 7)
    a['freccia.destra.sopra'] = (477, 77, 5, 7)
    a['scelta'] = (486, 68, 22, 22)
    a['tinta.sopra'] = (486, 92, 14, 12)
    # i posti del pannello del telaio, 20x20, con il bordo dello stato
    for i, st in enumerate(('integro', 'danneggiato', 'critico', 'rotto', 'vuoto', 'cella')):
        a['posto.' + st] = (318 + 20 * i, 118, 20, 20)
    for i, st in enumerate(('elmo', 'torso', 'braccio', 'gamba')):
        a['fantasma.' + st] = (440 + 16 * i, 118, 16, 16)
    a['fantasma.cella'] = (456, 134, 16, 16)
    a['tinta'] = (318, 140, 12, 10)
    a['tinta.spenta'] = (332, 140, 12, 10)
    a['predefinito'] = (346, 140, 14, 22)
    a['predefinito.sopra'] = (362, 140, 16, 24)
    a['preset.colori'] = (380, 140, 74, 10)
    a['preset.vuoto'] = (380, 152, 74, 10)
    # la barra della durata, 126x5: il fondo e i riempimenti, nella striscia sotto lo sfondo
    for i, st in enumerate(('fondo', 'integro', 'danneggiato')):
        a['durata.' + st] = (2, 234 + 6 * i, 126, 5)
    for i, st in enumerate(('critico', 'rotto', 'cella')):
        a['durata.' + st] = (130, 234 + 6 * i, 126, 5)
    return a


STATION_ATLAS = _station_atlas()

# Che cosa c'e' nello sfondo (disegni fissi), per il resoconto e le anteprime.
BACKGROUND_PARTS = ('cornice di bronzo e piastra d\'acciaio rivettata', 'i quattro pannelli incassati',
                    'i 36 slot dell\'inventario e della barra', 'le prese dei sei posti e della cella',
                    'la vaschetta della tavolozza', 'l\'incavo della barra della durata',
                    'il fondo dell\'anteprima con la piattaforma girevole e gli angoli di bronzo',
                    'le scanalature fra le righe dei preset', 'il riquadro della riga dei costi')

# --- texture dell'HUD: textures/gui/hud.png ---------------------------------------------------------------------------
HUD_TEX = (256, 128)
SILHOUETTE_STATES = ('integro', 'danneggiato', 'critico', 'rotto', 'assente')


def _hud_atlas():
    a = {'piastra': (0, 0, GROUP_W, GROUP_H), 'piastra.barra': (0, 36, ENERGY_W, GROUP_H)}
    for i, st in enumerate(SILHOUETTE_STATES):
        a['sagoma.' + st] = (90 + 22 * i, 0, SIL_W, GROUP_H)
    a['barra.cornice'] = (69, 37, 63, 8)
    for i, (name, _) in enumerate(ENERGY):
        a['barra.' + name] = (69, 46 + 7 * i, FILL_MAX, 6)
    a['avviso.rossa'] = (134, 37, 8, 8)
    a['avviso.grigia'] = (143, 37, 8, 8)
    a['cella'] = (152, 37, CELL_ICON_W, CELL_ICON_H)
    a['cella.spenta'] = (158, 37, CELL_ICON_W, CELL_ICON_H)
    return a


HUD_ATLAS = _hud_atlas()


def silhouette_piece(state, slot):
    """Il posto nella texture dell'HUD del pezzo `slot` nello stato `state`: la figura dello stato e, dentro, il
    rettangolo del pezzo (lo stesso del sottogruppo della sagoma)."""
    u, v, _, _ = HUD_ATLAS['sagoma.' + state]
    x0, y0, x1, y1 = SIL_PIECES[slot]
    return (u + x0, v + y0, x1 - x0, y1 - y0)


VIGNETTE_TEX = (2 * VIGNETTE_W, 2 * VIGNETTE_H)
VIGNETTE_MAX_ALPHA = 107      # 0,42 * 255: l'opacita' del pixel d'angolo, la piu' alta
KIT_TEX = (16, 16)
