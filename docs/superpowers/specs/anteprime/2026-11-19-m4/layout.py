"""Rettangoli della milestone 4 (design m4-stazione): HUD e GUI della stazione, in pixel dell'interfaccia.

Unica fonte dei numeri usati dalle anteprime e dal controllo delle sovrapposizioni. Un rettangolo e' (x0, y0, x1, y1)
con x1, y1 esclusi.
"""

# --- HUD -------------------------------------------------------------------------------------------------------------

CLUSTER_W = 88          # larghezza del gruppo in basso (piastra di fondo), pixel dell'interfaccia
CLUSTER_H = 35          # altezza del gruppo
EDGE = 2                # distanza dal bordo dello schermo
CHAT_GAP = 3            # il gruppo comincia 3 px sotto l'ultima riga della chat (che finisce a h - 40)
OFFHAND_W = 29          # slot della seconda mano del gioco base
HOTBAR_HALF = 91        # mezza barra degli oggetti


def narrow(w):
    """Vero se accanto allo slot della seconda mano non c'e' posto per il gruppo: allora va in alto a sinistra."""
    free = (w // 2 - HOTBAR_HALF - OFFHAND_W) - EDGE - 2
    return free < CLUSTER_W


def cluster_origin(w, h, main_right):
    """Angolo in alto a sinistra della piastra di fondo; il gruppo sta nell'angolo opposto al braccio disegnato."""
    if narrow(w):
        return EDGE, EDGE
    y = h - 40 + CHAT_GAP
    if main_right:
        return EDGE, y
    return w - EDGE - CLUSTER_W, y


# Due sottogruppi dentro il gruppo: la sagoma (verso il bordo dello schermo) e l'energia (verso l'interno). Con la mano
# sinistra si scambiano di lato; il loro interno non si specchia mai (la sagoma resta vista da dietro, il testo si
# legge da sinistra a destra).
SIL_GROUP = (0, 0, 21, CLUSTER_H)     # 21 x 35
NRG_GROUP = (21, 0, CLUSTER_W, CLUSTER_H)  # 67 x 35
SIL = (3, 3, 19, 33)                  # riquadro della sagoma (16 x 30), relativo a SIL_GROUP
SIL_PIECES = {                         # vista da dietro: il braccio sinistro a sinistra; relativi a SIL_GROUP
    'helmet': (8, 3, 14, 9),
    'torso': (7, 10, 15, 21),
    'left_arm': (3, 10, 6, 21),
    'right_arm': (16, 10, 19, 21),
    'left_leg': (7, 22, 10, 33),
    'right_leg': (12, 22, 15, 33),
}
TEXT = (2, 8, 65, 17)                 # percentuale e celle di scorta (font 8 + ombra), relativo a NRG_GROUP
WARN = (2, 8, 10, 16)                 # icona di avviso, 8 x 8
BAR = (2, 20, 65, 28)                 # cornice della barra, 63 x 8; riempimento dentro, 61 x 6


def mirror_x(r, w0):
    x0, y0, x1, y1 = r
    return (w0 - x1, y0, w0 - x0, y1)


def hud_rects(w, h, main_right, level='FULL'):
    """Rettangoli assoluti dei pezzi dell'HUD per la mano data e il livello (FULL o BAR_ONLY)."""
    ox, oy = cluster_origin(w, h, main_right)
    flip = (not main_right) and not narrow(w)
    sil_g = mirror_x(SIL_GROUP, CLUSTER_W) if flip else SIL_GROUP
    nrg_g = mirror_x(NRG_GROUP, CLUSTER_W) if flip else NRG_GROUP

    def inside(group, r):
        return (ox + group[0] + r[0], oy + group[1] + r[1], ox + group[0] + r[2], oy + group[1] + r[3])

    out = {}
    if level == 'FULL':
        out['piastra'] = (ox, oy, ox + CLUSTER_W, oy + CLUSTER_H)
        out['sagoma'] = inside(sil_g, SIL)
        for name, r in SIL_PIECES.items():
            out['sagoma.' + name] = inside(sil_g, r)
    else:
        out['piastra'] = inside(nrg_g, (0, 0, NRG_GROUP[2] - NRG_GROUP[0], CLUSTER_H))
    out['testo'] = inside(nrg_g, TEXT)
    out['avviso'] = inside(nrg_g, WARN)
    out['barra'] = inside(nrg_g, BAR)
    return out


def vignette_corners(w, h):
    """I quattro angoli della vignetta (72 x 40), solo con l'HUD completo e in prima persona."""
    return {
        'vignetta.alto_sx': (0, 0, 72, 40),
        'vignetta.alto_dx': (w - 72, 0, w, 40),
        'vignetta.basso_sx': (0, h - 40, 72, h),
        'vignetta.basso_dx': (w - 72, h - 40, w, h),
    }


MODULES_RESERVED = (None,)  # in alto a destra sotto gli effetti: vedi modules_reserved()


def modules_reserved(w, h):
    """Spazio riservato a M5 per le icone dei moduli: sotto le due file di effetti del gioco base (y 1-51)."""
    return (w - 2 - 98, 54, w - 2, 72)


# --- Rettangoli del gioco base e del braccio (pixel dello schermo 960 x 540, scala 2) ---------------------------------

SCREEN = (960, 540)
GUI_SCALE = 2
VANILLA_SCREEN = {
    'mirino': (450, 240, 510, 300),                       # 60x60+450+240 (arm_measure.sh)
    'barra e stato': (290, 440, 670, 540),                # 380x100+290+440 (arm_measure.sh)
    'chat (10 righe)': (0, 280, 664, 460),                # righe da h-130 a h-40 in GUI, larghe 332
    'braccio destro': (674, 335, 960, 540),               # 286x205+674+335 (design M3 par. 13)
    'braccio sinistro': (0, 335, 286, 540),               # 286x205+0+335 (D2)
}
OFFHAND_SCREEN = {True: (240, 494, 298, 540), False: (662, 494, 720, 540)}  # slot della seconda mano
TRANSIENT_SCREEN = {
    'salvataggio 1.21.1': (824, 470, 956, 488),           # x = w - testo - 2, y = h - 35 (GUI), con sfondo
    'salvataggio 1.20.1': (808, 510, 940, 528),           # x = w - testo - 10, y = h - 15 (GUI)
    'sottotitoli (riga piu\' bassa)': (760, 460, 956, 480),
    'effetti (due file, in alto a destra)': (860, 2, 960, 102),  # icone 24x24 a x = w - 25 n, y 1 e 27 (GUI)
    'avvisi (toast) in alto a destra': (640, 0, 960, 64),        # 160x32 (GUI) l'uno
    'barre dei boss': (298, 6, 662, 38),                          # centrate, da y 3 (GUI)
}


def to_screen(r, scale=GUI_SCALE):
    return tuple(v * scale for v in r)


def intersects(a, b):
    return a[0] < b[2] and b[0] < a[2] and a[1] < b[3] and b[1] < a[3]


# --- GUI della stazione (pixel dell'interfaccia, relativi a leftPos, topPos) -----------------------------------------

GUI_W, GUI_H = 316, 232
GUI = {
    'titolo': (8, 6, 140, 15),
    'rinforzato': (276, 3, 290, 15),
    'blocco': (292, 3, 308, 15),
    # pannello del telaio (vista di fronte: il braccio destro del telaio a sinistra)
    'pannello.telaio': (7, 17, 73, 135),
    'pezzo.helmet': (30, 21, 50, 41),
    'pezzo.right_arm': (9, 43, 29, 63),
    'pezzo.torso': (30, 43, 50, 63),
    'pezzo.left_arm': (51, 43, 71, 63),
    'pezzo.right_leg': (19, 65, 39, 85),
    'pezzo.left_leg': (41, 65, 61, 85),
    'cella': (19, 101, 39, 121),
    'tutti': (41, 103, 71, 119),
    # pannello del pezzo scelto
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
    # anteprima 3D
    'pannello.anteprima': (215, 17, 309, 135),
    'anteprima.ritaglio': (216, 18, 308, 134),
    # inventario del giocatore
    'etichetta.inventario': (8, 139, 80, 148),
    'inventario': (7, 149, 169, 203),
    'barra': (7, 207, 169, 225),
    # preset e riga dei costi
    'pannello.preset': (175, 138, 309, 225),
    'etichetta.preset': (177, 140, 240, 149),
    'preset.0': (177, 150, 301, 166),
    'preset.1': (177, 168, 301, 184),
    'preset.2': (177, 186, 301, 202),
    'riga.costi': (177, 205, 307, 224),
}


def swatch(i):
    """Il riquadro della tinta i (ordine di DyeColor) nella tavolozza: due file da otto."""
    col, row = i % 8, i // 8
    x = 81 + col * 14
    y = 88 + row * 12
    return (x, y, x + 12, y + 10)


def preset_parts(k):
    x0, y0, x1, y1 = GUI['preset.%d' % k]
    return {
        'numero': (x0, y0 + 4, x0 + 6, y0 + 12),
        'colori': (x0 + 8, y0 + 4, x0 + 80, y0 + 12),
        'applica': (x0 + 84, y0, x0 + 102, y0 + 16),
        'salva': (x0 + 106, y0, x0 + 124, y0 + 16),
    }
