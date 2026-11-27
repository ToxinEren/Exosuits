#!/usr/bin/env python3
"""Anteprime per il proprietario della parte 4d nello stile A (Officina): la schermata della stazione, l'HUD, la
vignetta del casco e l'icona del kit, composte dai file che scrive tools/art/gui.py e dagli scatti del gioco di
docs/test-in-gioco/ come erano al commit 74a890c, con la schermata e l'HUD ancora a colori pieni (gli scatti di oggi
li mostrano gia' nello stile A); solo locali, non nella CI; servono Pillow, numpy e git.

Uso, dalla radice del repository:
    python3 -I tools/art/preview/gui_preview.py --vanilla <dir> [--assets <dir>] [--out <dir>]

--vanilla   le risorse del gioco base estratte a parte dal jar del client, fuori dal repository: il carattere
            (assets/minecraft/font/include/default.json e textures/font/*.png) e le icone di sette oggetti
            (textures/item/iron_ingot, copper_ingot, red_dye, white_dye, black_dye, blue_dye, redstone);
--assets    la cartella scritta da `tools/art/gui.py --out <dir> --kit` (se manca, il generatore scrive in
            tools/art/preview/out/gui/, ignorata da git);
--out       predefinito docs/superpowers/specs/anteprime/2026-11-27-m4d-grafica/.

Ogni disegno viene ritagliato dalle PNG generate con i posti di gui_layout.py e messo dove il codice lo disegnera'
(i rettangoli del design), a scala 1 nei pixel dell'interfaccia, poi ingrandito senza filtri alla scala 2; i colori che
il codice mette da se' (le tinte, i colori del livello e dei preset) e i testi, con il carattere del gioco base, sono
quelli che scriverebbe il codice. Il telaio nell'anteprima 3D e' quello dello scatto della 4a-1
(`stazione-colori-suoi`), copiato pixel per pixel dal suo ritaglio. Sugli scatti dell'HUD il gruppo disegnato oggi a
colori pieni si copre prima con l'erba della stessa colonna, specchiata, perche' la piastra e' traslucida. Le PNG si
scrivono con exo.write_png (solo IHDR, IDAT, IEND) e lo script si ferma se una ha altri blocchi.
"""
import argparse
import io
import json
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ART = os.path.dirname(HERE)
REPO = os.path.dirname(os.path.dirname(ART))
sys.path.insert(0, ART)

import numpy as np  # noqa: E402
from PIL import Image, ImageDraw, ImageFont  # noqa: E402

import gui_layout as L  # noqa: E402
from exo import write_png  # noqa: E402

SHOTS = 'docs/test-in-gioco'
# Gli scatti su cui stanno le anteprime del 27 novembre 2026: quelli di prima dell'integrazione, con la schermata e
# l'HUD a colori pieni, che lo script toglie (il fondo piatto dell'anteprima 3D, il gruppo dell'HUD coperto d'erba).
SHOTS_REV = '74a890c'
MOD_TEX = os.path.join(REPO, 'src', 'main', 'resources', 'assets', 'redstone_exoframes', 'textures')
OUT = os.path.join(REPO, 'docs', 'superpowers', 'specs', 'anteprime', '2026-11-27-m4d-grafica')

TEXT = (0xE6, 0xE6, 0xE6)       # StationLayout.TEXT
TEXT_DIM = (0x9A, 0xA0, 0xA6)   # StationLayout.TEXT_DIM
SHOT_PREVIEW_BG = (20, 23, 27)  # il fondo piatto dell'anteprima negli scatti di SHOTS_REV (PREVIEW_BG di allora)
SHOT_ARROW = (152, 158, 164)    # le frecce "<" ">" degli scatti di SHOTS_REV


# --- carattere del gioco base e didascalie ---------------------------------------------------------------------------
class McFont:
    """Le bitmap del carattere del gioco base (font/include/default.json): larghezza e disegno come nel gioco, con
    l'ombra a (1, 1) di un quarto del colore."""

    def __init__(self, vanilla):
        base = os.path.join(vanilla, 'assets', 'minecraft')
        with open(os.path.join(base, 'font', 'include', 'default.json'), encoding='utf-8') as fh:
            providers = json.load(fh)['providers']
        self.glyphs = {}
        for p in providers:
            if p.get('type') != 'bitmap':
                continue
            img = Image.open(os.path.join(base, 'textures', p['file'].split(':', 1)[1])).convert('RGBA')
            rows = p['chars']
            cw, ch = img.width // len(rows[0]), img.height // len(rows)
            for r, row in enumerate(rows):
                for c, char in enumerate(row):
                    if char in self.glyphs or char in '\x00 ':
                        continue
                    alpha = img.crop((c * cw, r * ch, (c + 1) * cw, (r + 1) * ch)).split()[3]
                    bbox = alpha.getbbox()
                    self.glyphs[char] = (alpha, bbox[2] if bbox else 0, 7 - p['ascent'])

    def width(self, s):
        return sum(4 if c == ' ' else self.glyphs.get(c, (None, 5, 0))[1] + 1 for c in s) - (1 if s else 0)

    def draw(self, im, x, y, s, colour, shadow=True):
        if shadow:
            self._draw(im, x + 1, y + 1, s, tuple(v // 4 for v in colour))
        self._draw(im, x, y, s, colour)

    def _draw(self, im, x, y, s, colour):
        for c in s:
            if c == ' ':
                x += 4
                continue
            g = self.glyphs.get(c)
            if g is None:
                x += 6
                continue
            alpha, w, up = g
            im.paste(Image.new('RGBA', alpha.size, tuple(colour) + (255,)), (x, y + up), alpha)
            x += w + 1


def caption_font(size):
    for path in ('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',):
        if os.path.exists(path):
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


CAP = None
CAP_SMALL = None


def label(im, xy, s, colour=(255, 255, 255), small=False, bg=(16, 17, 20, 200)):
    """Una didascalia con il fondo scuro, in pixel dello schermo."""
    font = CAP_SMALL if small else CAP
    d = ImageDraw.Draw(im)
    x0, y0, x1, y1 = d.textbbox((0, 0), s, font=font)
    w, h = x1 + 8, (14 if small else 18) + 2
    layer = Image.new('RGBA', (w, h), bg)
    ImageDraw.Draw(layer).text((4, 2), s, fill=colour, font=font)
    im.alpha_composite(layer, (int(xy[0]), int(xy[1])))
    return w, h


# --- risorse ---------------------------------------------------------------------------------------------------------
def at_rev(path):
    """Una PNG del repository come era al commit SHOTS_REV, letta con git (percorso dalla radice)."""
    data = subprocess.run(['git', '-C', REPO, 'show', '%s:%s' % (SHOTS_REV, path)], capture_output=True,
                          check=True).stdout
    return Image.open(io.BytesIO(data)).convert('RGBA')


def shot(folder, name):
    """Uno scatto di docs/test-in-gioco/<folder>/<name> come era al commit SHOTS_REV."""
    return at_rev('%s/%s/%s' % (SHOTS, folder, name))


class Sheets:
    def __init__(self, assets, vanilla):
        def load(rel):
            return Image.open(os.path.join(assets, rel)).convert('RGBA')
        self.station = load('textures/gui/station.png')
        self.hud = load('textures/gui/hud.png')
        self.vignette = load('textures/gui/hud_vignette.png')
        self.kit = load('textures/item/repair_kit.png')
        self.vanilla = vanilla

    def sprite(self, name):
        u, v, w, h = L.STATION_ATLAS[name]
        return self.station.crop((u, v, u + w, v + h))

    def hud_sprite(self, name):
        u, v, w, h = L.HUD_ATLAS[name]
        return self.hud.crop((u, v, u + w, v + h))

    def piece(self, state, slot):
        u, v, w, h = L.silhouette_piece(state, slot)
        return self.hud.crop((u, v, u + w, v + h))

    def item(self, name):
        mod = os.path.join(MOD_TEX, 'item', name + '.png')
        path = mod if os.path.exists(mod) else os.path.join(self.vanilla, 'assets', 'minecraft', 'textures', 'item',
                                                             name + '.png')
        return Image.open(path).convert('RGBA')


def blit(canvas, img, x, y):
    canvas.alpha_composite(img, (int(x), int(y)))


def fill(canvas, r, colour):
    ImageDraw.Draw(canvas).rectangle((r[0], r[1], r[2] - 1, r[3] - 1), fill=tuple(colour) + (255,))


def rgb(v):
    return ((v >> 16) & 255, (v >> 8) & 255, v & 255)


def nearest(im, k):
    return im.resize((im.width * k, im.height * k), Image.NEAREST)


# --- schermata della stazione ----------------------------------------------------------------------------------------
EXAMPLE_PIECES = {'helmet': 'integro', 'torso': 'danneggiato', 'right_arm': 'integro', 'left_arm': 'critico',
                  'right_leg': 'integro', 'left_leg': 'rotto'}
PIECE_ICONS = {'helmet': 'scrap_helmet', 'torso': 'scrap_torso', 'right_arm': 'scrap_arm', 'left_arm': 'scrap_arm',
               'right_leg': 'scrap_leg', 'left_leg': 'scrap_leg'}
DYES_HAVE = {'rosso': 4, 'bianco': 6, 'nero': 3, 'blu': 2}


def suit_from_shot():
    """Il telaio dell'anteprima 3D dallo scatto della 4a-1, a scala 2: il ritaglio senza il fondo e senza le frecce."""
    im = shot('m4a1-screenshot', '1.21.1-neoforge-stazione-colori-suoi.png')
    x0, y0, x1, y1 = (v * 2 for v in L.GUI['anteprima.ritaglio'])
    lx, ly = 82 * 2, 19 * 2
    clip = np.array(im.crop((lx + x0, ly + y0, lx + x1, ly + y1)))
    rgbv = clip[..., :3]
    clip[(rgbv == SHOT_PREVIEW_BG).all(axis=2), 3] = 0
    h, w = clip.shape[:2]
    corner = np.zeros((h, w), bool)
    corner[h - 30:, :30] = True
    corner[h - 30:, w - 30:] = True
    clip[corner & (rgbv == SHOT_ARROW).all(axis=2), 3] = 0
    return Image.fromarray(clip)


def button_label(canvas, font, r, s, state):
    colour = (0xA0, 0xA0, 0xA0) if state == 'spento' else (255, 255, 255)
    w = font.width(s)
    x = r[0] + (r[2] - r[0] - w + 1) // 2
    y = r[1] + (r[3] - r[1] - 8) // 2 + (1 if state == 'premuto' else 0)
    font.draw(canvas, x, y, s, colour, shadow=state != 'spento')


def compose_screen(sheets, font, gui_w, gui_h, base, hover, tooltip):
    """La schermata con il contenuto d'esempio delle anteprime del design (par. 12) su `base` (pixel dello schermo,
    scala 2). `hover`: il widget sotto il mouse ('tinta.rosso' o 'ripara')."""
    lx, ly = (gui_w - L.SCREEN_W) // 2, (gui_h - L.SCREEN_H) // 2

    def R(r):
        return (lx + r[0], ly + r[1], lx + r[2], ly + r[3])

    def put(name, r, dx=0, dy=0):
        blit(low, sheets.sprite(name), lx + r[0] + dx, ly + r[1] + dy)

    low = Image.new('RGBA', (gui_w, gui_h), (0, 0, 0, 0))
    put('fondo', (0, 0))
    G = L.GUI
    put('rinforzato', G['rinforzato'])
    put('blocco.chiuso.normale', G['blocco'])
    for slot_name, state in EXAMPLE_PIECES.items():
        r = G['pezzo.' + slot_name]
        put('posto.' + state, r)
        blit(low, sheets.item(PIECE_ICONS[slot_name]), lx + r[0] + L.ICON_INSET, ly + r[1] + L.ICON_INSET)
        if slot_name == 'torso':
            put('scelta', L.grow(r, 1))
    put('posto.cella', G['cella'])
    blit(low, sheets.item('redstone_cell'), lx + G['cella'][0] + L.ICON_INSET, ly + G['cella'][1] + L.ICON_INSET)
    buttons = (('tutti', 'tutti', 'Tutti', 'normale'), ('ripara', 'ripara', 'Ripara', 'normale'),
               ('ripara_tutto', 'ripara_tutto', 'Tutto: 3', 'normale'),
               ('regione.primario', 'regione', 'Primario', 'premuto'),
               ('regione.secondario', 'regione', 'Secondario', 'normale'), ('smonta', 'smonta', None, 'normale'))
    labels = []
    for where, sprite, text, state in buttons:
        if where == hover:
            state = 'sopra'
        put('%s.%s' % (sprite, state), G[where])
        if text:
            labels.append((R(G[where]), text, state))
    # durata
    bar = G['durata.barra']
    put('durata.fondo', bar)
    filled = (bar[2] - bar[0]) * 81 // 180
    blit(low, sheets.sprite('durata.danneggiato').crop((0, 0, filled, 5)), lx + bar[0], ly + bar[1])
    # materiale
    m = G['materiale']
    put('materiale', m)
    blit(low, sheets.item('iron_ingot'), lx + m[0] + 1, ly + m[1] + 1)
    # tavolozza
    for i, (dye, value) in enumerate(L.DYES):
        r = L.swatch(i)
        fill(low, R(r), rgb(value))
        put('tinta', r)
        if dye not in DYES_HAVE:
            put('tinta.spenta', r)
        if hover == 'tinta.' + dye:
            put('tinta.sopra', L.grow(r, 1))
    d = G['predefinito']
    fill(low, R((d[0] + 1, d[1] + 1, d[2] - 1, d[1] + 11)), rgb(L.SCRAP_COLOURS[0]))
    fill(low, R((d[0] + 1, d[1] + 12, d[2] - 1, d[3] - 1)), rgb(L.SCRAP_COLOURS[1]))
    put('predefinito', d)
    # preset
    presets = [[rgb(L.SCRAP_COLOURS[0]), rgb(L.SCRAP_COLOURS[1])] * 6, [rgb(0xB02E26), rgb(0x1D1D21)] * 6, None]
    for k, colours in enumerate(presets):
        parts = L.preset_parts(k)
        c = parts['colori']
        put('preset.vuoto' if colours is None else 'preset.colori', (c[0] - 1, c[1] - 1))
        if colours:
            for i, colour in enumerate(colours):
                fill(low, R(L.preset_cell(k, i)), colour)
        put('applica.%s' % ('spento' if colours is None else 'normale'), parts['applica'])
        put('salva.normale', parts['salva'])
    big = nearest(low, 2)
    base.alpha_composite(big)
    # il telaio nell'anteprima 3D, alla scala dello scatto, poi le frecce sopra
    clip = G['anteprima.ritaglio']
    base.alpha_composite(suit_from_shot(), ((lx + clip[0]) * 2, (ly + clip[1]) * 2))
    top = Image.new('RGBA', (gui_w, gui_h), (0, 0, 0, 0))
    for side, r in L.ARROWS.items():
        blit(top, sheets.sprite('freccia.' + side), lx + r[0], ly + r[1])
    # oggetti negli slot del giocatore e nel materiale, con il numero
    slots = L.inventory_slots()
    inventory = {0: ('copper_ingot', 20), 1: ('blue_dye', 2), 13: ('scrap_leg', 1), 26: ('redstone', 9),
                 27: ('scrap_arm', 1), 28: ('redstone_cell', 1), 29: ('iron_ingot', 12), 30: ('red_dye', 4),
                 31: ('white_dye', 6), 32: ('black_dye', 3)}
    for index, (name, count) in inventory.items():
        r = slots[index]
        blit(top, sheets.item(name), lx + r[0] + 1, ly + r[1] + 1)
        if count > 1:
            s = str(count)
            font.draw(top, lx + r[0] + 1 + 17 - font.width(s), ly + r[1] + 1 + 9, s, (255, 255, 255))
    font.draw(top, lx + m[0] + 1 + 17 - font.width('12'), ly + m[1] + 1 + 9, '12', (255, 255, 255))
    # testi
    font.draw(top, lx + G['titolo'][0], ly + G['titolo'][1], 'Stazione di manutenzione', TEXT, shadow=False)
    font.draw(top, lx + G['etichetta.inventario'][0], ly + G['etichetta.inventario'][1], 'Inventario', TEXT,
              shadow=False)
    font.draw(top, lx + G['etichetta.preset'][0], ly + G['etichetta.preset'][1], 'Preset', TEXT, shadow=False)
    font.draw(top, lx + G['nome'][0], ly + G['nome'][1], 'Torso Rottame', TEXT, shadow=False)
    font.draw(top, lx + G['condizione'][0], ly + G['condizione'][1], 'Danneggiato', L.YELLOW, shadow=False)
    s = '81 / 180'
    font.draw(top, lx + G['durata.numeri'][2] - font.width(s), ly + G['durata.numeri'][1], s, TEXT, shadow=False)
    for r, text, state in labels:
        button_label(top, font, r, text, state)
    for k in range(3):
        parts = L.preset_parts(k)
        font.draw(top, lx + parts['numero'][0], ly + parts['numero'][1], str(k + 1), TEXT, shadow=False)
        if k == 2:
            font.draw(top, lx + parts['colori'][0] + 4, ly + parts['colori'][1], 'vuoto', TEXT_DIM, shadow=False)
    rc = G['riga.costi']
    font.draw(top, lx + rc[0] + 3, ly + rc[1] + 1, 'Costo: 1 Tinta rossa', TEXT, shadow=False)
    font.draw(top, lx + rc[0] + 3, ly + rc[1] + 10, 'Ne hai 4', TEXT_DIM, shadow=False)
    if tooltip:
        sr = R(L.swatch(14))
        draw_tooltip(top, font, sr[2] + 6, sr[1] - 20, [('Tinta rossa', (255, 255, 255)),
                                                         ('Primario del torso', (170, 170, 170)),
                                                         ('Clic: 1 tinta (ne hai 4)', (170, 170, 170))])
        draw_cursor(top, sr[0] + 6, sr[1] + 5)
    elif hover == 'ripara':
        r = R(G['ripara'])
        draw_cursor(top, r[0] + 30, r[1] + 9)
    base.alpha_composite(nearest(top, 2))
    return base


def draw_tooltip(im, font, x, y, lines):
    """Il suggerimento del gioco base (fondo viola scurissimo, bordo viola sfumato), come lo disegna il gioco."""
    w = max(font.width(s) for s, _ in lines) + 8
    h = 10 * len(lines) + 6
    d = ImageDraw.Draw(im)
    d.rectangle((x + 1, y, x + w - 2, y + h - 1), fill=(16, 0, 16, 240))
    d.rectangle((x, y + 1, x + w - 1, y + h - 2), fill=(16, 0, 16, 240))
    d.rectangle((x + 1, y + 1, x + w - 2, y + h - 2), outline=(80, 0, 255, 80))
    for i, (s, colour) in enumerate(lines):
        font.draw(im, x + 4, y + 4 + i * 10 + (2 if i else 0) - 2, s, colour)


def draw_cursor(im, x, y):
    d = ImageDraw.Draw(im)
    d.polygon([(x, y), (x, y + 10), (x + 3, y + 7), (x + 7, y + 7)], fill=(255, 255, 255, 255), outline=(0, 0, 0, 255))


def world_behind_screen(w, h):
    """Il mondo dietro la schermata, come lo oscura il gioco base (sfumatura da C0101010 a D0101010)."""
    im = shot('m4a1-screenshot', '1.21.1-neoforge-stazione-posata.png')
    im = im.crop(((im.width - w) // 2, (im.height - h) // 2, (im.width + w) // 2, (im.height + h) // 2))
    dim = Image.new('RGBA', (w, h))
    d = ImageDraw.Draw(dim)
    for y in range(h):
        d.line((0, y, w, y), fill=(16, 16, 16, 0xC0 + (0xD0 - 0xC0) * y // h))
    im.alpha_composite(dim)
    return im


def screen_images(sheets, font, out):
    written = []
    base = shot('m4a1-screenshot', '1.21.1-neoforge-stazione-colori-suoi.png')
    img = compose_screen(sheets, font, 480, 270, base, 'tinta.rosso', True)
    label(img, (8, 8), 'Stazione, stile A: scala 2, finestra 960x540 (mouse sulla tinta rossa)')
    written.append(save(img, out, 'schermata-960x540.png'))
    base = world_behind_screen(640, 480)
    img = compose_screen(sheets, font, 320, 240, base, 'ripara', False)
    written.append(save(img, out, 'schermata-640x480.png'))
    return written


# --- texture ingrandite ----------------------------------------------------------------------------------------------
def checker(w, h, cell=8):
    a = np.zeros((h, w, 4), np.uint8)
    yy, xx = np.mgrid[0:h, 0:w]
    dark = ((xx // cell + yy // cell) % 2).astype(bool)
    a[..., :3] = np.where(dark[..., None], (62, 63, 68), (78, 80, 86))
    a[..., 3] = 255
    return Image.fromarray(a)


STATION_GROUPS = (
    ('1', 'pulsanti alti 16 (Tutti, Ripara, Tutto: N, applica, salva): normale, sopra, premuto, spento',
     ('tutti.', 'ripara.', 'ripara_tutto.', 'applica.', 'salva.')),
    ('2', 'scudo del telaio rinforzato', ('rinforzato',)),
    ('3', 'pulsanti alti 12 (Primario e Secondario, smonta, lucchetto chiuso e aperto): gli stessi quattro stati',
     ('regione.', 'smonta.', 'blocco.')),
    ('4', 'materiale: normale, spento, velo sull\'icona', ('materiale',)),
    ('5', 'frecce dell\'anteprima, normali e sotto il mouse', ('freccia.',)),
    ('6', 'secondo bordo del pezzo scelto; anello della tinta sotto il mouse', ('scelta', 'tinta.sopra')),
    ('7', 'posti: integro, danneggiato, critico, rotto, vuoto, cella', ('posto.',)),
    ('8', 'icone in trasparenza dei posti vuoti: elmo, torso, braccio, gamba, cella', ('fantasma.',)),
    ('9', 'tinta: cornice e velo "spenta"; predefinito e il suo anello; fondo dei preset, pieno e vuoto',
     ('tinta', 'predefinito', 'preset.')),
    ('10', 'barra della durata: fondo e riempimenti (integro, danneggiato, critico, rotto, cella)', ('durata.',)),
)


def sheet_image(sheet, atlas, groups, scale, title, note):
    W, H = sheet.size
    big = checker(W * scale, H * scale)
    big.alpha_composite(nearest(sheet, scale))
    d = ImageDraw.Draw(big)
    numbers = {}
    for key, (u, v, w, h) in atlas.items():
        if key == 'fondo':
            continue
        d.rectangle((u * scale - 1, v * scale - 1, (u + w) * scale, (v + h) * scale), outline=(90, 220, 255, 255))
        for num, _, prefixes in groups:
            if any(key == p or key.startswith(p if p.endswith('.') else p + '.') for p in prefixes):
                numbers.setdefault(num, (u, v))   # il numero sul primo disegno del gruppo
                break
    for num, (u, v) in numbers.items():
        label(big, (max(0, u * scale - 2), max(0, v * scale - 2)), num, (255, 235, 120), small=True,
              bg=(0, 0, 0, 220))
    pad = 12
    lines = [title, note] + ['%s  %s' % (num, text) for num, text, _ in groups]
    img = Image.new('RGBA', (big.width + 2 * pad, big.height + 40 + 22 * len(lines)), (28, 29, 33, 255))
    y = 10
    for i, s in enumerate(lines):
        ImageDraw.Draw(img).text((pad, y), s, fill=(240, 240, 240) if i == 0 else (205, 205, 205),
                                 font=CAP if i == 0 else CAP_SMALL)
        y += 26 if i == 0 else 20
    img.alpha_composite(big, (pad, y + 8))
    return img.crop((0, 0, img.width, y + 8 + big.height + pad))


def texture_images(sheets, out):
    written = []
    img = sheet_image(sheets.station, L.STATION_ATLAS, STATION_GROUPS, 3,
                      'textures/gui/station.png, 512x256, ingrandita x3 (a scacchi: trasparente)',
                      'a sinistra lo sfondo di 316x232; i numeri sono i gruppi di disegni dei widget')
    written.append(save(img, out, 'schermata-texture.png'))
    hud_groups = (
        ('1', 'piastra: tutto il gruppo 88x35 (FULL) e solo l\'energia 67x35 (BAR_ONLY)', ('piastra',)),
        ('2', 'sagoma: integro, danneggiato, critico, rotto, non montato', ('sagoma.',)),
        ('3', 'cornice e riempimenti della barra: verde, rossa, grigia, bianca', ('barra.',)),
        ('4', 'avviso rosso e grigio; icona della cella di scorta, normale e spenta', ('avviso.', 'cella')),
    )
    used = sheets.hud.crop((0, 0, 200, 74))
    atlas = {k: v for k, v in L.HUD_ATLAS.items()}
    img = sheet_image(used, atlas, hud_groups, 6, 'textures/gui/hud.png, 256x128 (parte usata), ingrandita x6',
                      'colori della legenda esatti: 55FF55, FFFF55, FF5555, 8A8A8A; il contorno a un terzo')
    written.append(save(img, out, 'hud-texture.png'))
    return written


# --- HUD --------------------------------------------------------------------------------------------------------------
STATE_NAMES = {'intact': 'integro', 'damaged': 'danneggiato', 'critical': 'critico', 'broken': 'rotto', None: 'assente'}
ENERGY_COLOUR = {'NORMAL': ('verde', L.GREEN), 'LOW': ('rossa', L.RED), 'EMPTY': ('grigia', L.GREY)}
FULL = {'helmet': 'intact', 'torso': 'intact', 'left_arm': 'intact', 'right_arm': 'intact', 'left_leg': 'intact',
        'right_leg': 'intact'}
HUD_STATES = (
    ('1-energia-piena', {'level': 'FULL', 'energy': 100, 'estate': 'NORMAL', 'spare': 2, 'pieces': dict(FULL)},
     'energia piena, sei pezzi integri, due celle di scorta'),
    ('2-energia-bassa-pezzo-danneggiato',
     {'level': 'FULL', 'energy': 8, 'estate': 'LOW', 'spare': 1,
      'pieces': dict(FULL, right_arm='damaged', torso='damaged')},
     'energia bassa (8 %, avviso), braccio destro e torso danneggiati'),
    ('3-pezzo-rotto',
     {'level': 'FULL', 'energy': 54, 'estate': 'NORMAL', 'spare': 0,
      'pieces': dict(FULL, left_leg='broken', torso='critical', left_arm=None)},
     'gamba sinistra rotta, torso critico, braccio sinistro non montato, nessuna cella di scorta'),
    ('4-senza-elmo', {'level': 'BAR_ONLY', 'energy': 37, 'estate': 'NORMAL', 'spare': 1, 'pieces': {}},
     'senza elmo: solo la barra dell\'energia'),
)


def hud_base(main_right):
    """Uno scatto della 4c senza la vignetta segnaposto, con il gruppo di oggi coperto dall'erba della stessa colonna
    (le righe subito sopra, specchiate: nessun salto sul bordo)."""
    name = '1.21.1-neoforge-danneggiato.png' if main_right else '1.21.1-neoforge-mancino-danneggiato.png'
    im = np.array(shot('m4c-screenshot', name))
    x0, y0, x1, y1 = (v * 2 for v in L.hud_group(480, 270, main_right))
    x0, y0, x1, y1 = x0 - 2, y0 - 2, x1 + 2, min(y1 + 2, im.shape[0])
    h = y1 - y0
    im[y0:y1, x0:x1] = im[y0 - h:y0, x0:x1][::-1]
    return Image.fromarray(im)


def draw_vignette(sheets, gui, w, h):
    """I quattro angoli, ciascuno dal suo posto nella texture (la colonna i % 2 e la riga i // 2), come SuitHud."""
    for i, r in enumerate(L.vignette_corners(w, h)):
        u, v = (i % 2) * L.VIGNETTE_W, (i // 2) * L.VIGNETTE_H
        blit(gui, sheets.vignette.crop((u, v, u + L.VIGNETTE_W, v + L.VIGNETTE_H)), r[0], r[1])


def draw_hud(sheets, font, gui, w, h, main_right, state, vignette=True):
    """L'HUD come lo disegnerebbe SuitHud con i disegni della texture: prima la vignetta (FULL, prima persona), poi
    la piastra, la sagoma, l'avviso, la percentuale, le celle di scorta e la barra."""
    if state['level'] == 'FULL' and vignette:
        draw_vignette(sheets, gui, w, h)
    sil, nrg = L.hud_subgroups(w, h, main_right)
    if state['level'] == 'FULL':
        blit(gui, sheets.hud_sprite('piastra'), *L.hud_group(w, h, main_right)[:2])
        for slot_name, (x0, y0, _, _) in L.SIL_PIECES.items():
            blit(gui, sheets.piece(STATE_NAMES[state['pieces'].get(slot_name)], slot_name), sil[0] + x0, sil[1] + y0)
    else:
        blit(gui, sheets.hud_sprite('piastra.barra'), nrg[0], nrg[1])
    name, colour = ENERGY_COLOUR[state['estate']]
    ex, ey = nrg[0], nrg[1]
    warn = state['estate'] in ('LOW', 'EMPTY')
    if warn:
        blit(gui, sheets.hud_sprite('avviso.' + name), ex + L.WARNING[0], ey + L.WARNING[1])
    font.draw(gui, ex + (L.TEXT_X_AFTER_WARNING if warn else L.TEXT_X), ey + L.TEXT_Y, '%d %%' % state['energy'],
              colour)
    spare = '×%d' % state['spare']
    sx = ex + L.SPARE_RIGHT - font.width(spare)
    blit(gui, sheets.hud_sprite('cella' if state['spare'] else 'cella.spenta'), sx - 1 - L.CELL_ICON_W, ey + L.TEXT_Y)
    font.draw(gui, sx, ey + L.TEXT_Y, spare, (255, 255, 255) if state['spare'] else L.GREY)
    blit(gui, sheets.hud_sprite('barra.cornice'), ex + L.BAR_FRAME[0], ey + L.BAR_FRAME[1])
    fw = state['energy'] * L.FILL_MAX // 100
    if fw:
        blit(gui, sheets.hud_sprite('barra.' + name).crop((0, 0, fw, 6)), ex + L.BAR_FILL[0], ey + L.BAR_FILL[1])


def hud_images(sheets, font, out):
    written = []
    for main_right in (True, False):
        for tag, state, text in HUD_STATES:
            if tag.startswith('4') and not main_right:
                continue
            scene = hud_base(main_right)
            gui = Image.new('RGBA', (480, 270), (0, 0, 0, 0))
            draw_hud(sheets, font, gui, 480, 270, main_right, state)
            scene.alpha_composite(nearest(gui, 2))
            g = [v * 2 for v in L.hud_group(480, 270, main_right)]
            zoom = nearest(scene.crop((g[0] - 6, g[1] - 6, g[2] + 6, g[3] + 6)), 3)
            zx, zy = (480 - zoom.width // 2, 70)
            ImageDraw.Draw(scene).rectangle((zx - 3, zy - 3, zx + zoom.width + 2, zy + zoom.height + 2),
                                            fill=(16, 17, 20, 255))
            scene.alpha_composite(zoom, (zx, zy))
            hand = 'destra' if main_right else 'sinistra'
            label(scene, (8, 8), 'HUD, stile A, mano %s: %s' % (hand, text))
            label(scene, (zx, zy + zoom.height + 6), 'il gruppo ingrandito x3 (scala 2, finestra 960x540)',
                  (220, 220, 220), small=True)
            written.append(save(scene, out, 'hud-%s-%s.png' % (hand, tag)))
    return written


# --- vignetta ---------------------------------------------------------------------------------------------------------
def vignette_image(sheets, font, out):
    """La vignetta nuova sullo scatto in prima persona, con l'HUD; sotto, tre angoli ingranditi x2 della sola vignetta
    (senza l'HUD, che copre quello in basso a sinistra), prima quella segnaposto della parte 4c e poi la nuova."""
    state = HUD_STATES[0][1]
    textures = (('nuova', sheets.vignette),
                ('segnaposto', at_rev('src/main/resources/assets/redstone_exoframes/textures/gui/hud_vignette.png')))
    corners_only = {}
    main = None
    for which, tex in textures:
        saved = sheets.vignette
        sheets.vignette = tex
        for with_hud in (True, False):
            scene = hud_base(True)
            gui = Image.new('RGBA', (480, 270), (0, 0, 0, 0))
            if with_hud:
                draw_hud(sheets, font, gui, 480, 270, True, state)
            else:
                draw_vignette(sheets, gui, 480, 270)
            scene.alpha_composite(nearest(gui, 2))
            if with_hud and which == 'nuova':
                main = scene
            elif not with_hud:
                corners_only[which] = scene
        sheets.vignette = saved
    k = 2
    cw, ch = L.VIGNETTE_W * 2, L.VIGNETTE_H * 2
    corners = (('in alto a sinistra', (0, 0)), ('in alto a destra', (960 - cw, 0)),
               ('in basso a sinistra', (0, 540 - ch)))
    strip_h = 40 + 2 * (ch * k + 30)
    img = Image.new('RGBA', (960 + 2 * 12, 540 + 24 + strip_h), (28, 29, 33, 255))
    img.alpha_composite(main, (12, 12))
    label(img, (20, 20), 'Vignetta del casco, stile A, sullo scatto in prima persona con l\'HUD (scala 2)')
    y = 540 + 24
    ImageDraw.Draw(img).text((12, y), 'Angoli ingranditi x2, senza l\'HUD: sopra la vignetta segnaposto di oggi, '
                             'sotto la nuova (acciaio del bordo e vetro dell\'elmo, riflessi del vetro in alto)',
                             fill=(220, 220, 220), font=CAP_SMALL)
    y += 30
    for which in ('segnaposto', 'nuova'):
        x = 12
        for title, (cx, cy) in corners:
            crop = nearest(corners_only[which].crop((cx, cy, cx + cw, cy + ch)), k)
            img.alpha_composite(crop, (x, y))
            label(img, (x + 4, y + 4), '%s: %s' % (which, title), small=True)
            x += crop.width + 12
        y += ch * k + 30
    written = [save(img.crop((0, 0, img.width, y - 18)), out, 'vignetta.png')]
    return written


# --- icona del kit ----------------------------------------------------------------------------------------------------
def kit_image(sheets, out):
    station = Image.open(os.path.join(MOD_TEX, 'item', 'station.png')).convert('RGBA')
    img = Image.new('RGBA', (2 * (128 + 40) + 260, 230), (28, 29, 33, 255))
    d = ImageDraw.Draw(img)
    d.text((16, 10), 'Icona del kit di riparazione accanto a quella della stazione', fill=(240, 240, 240), font=CAP)
    x = 16
    for icon, title in ((station, 'stazione (nel gioco)'), (sheets.kit, 'kit di riparazione (nuova)')):
        d.rectangle((x - 1, 47, x + 128, 176), fill=(139, 139, 139, 255))
        img.alpha_composite(nearest(icon, 8), (x, 48))
        d.text((x, 184), title + ', x8', fill=(210, 210, 210), font=CAP_SMALL)
        x += 128 + 40
    for icon, title, yy in ((station, 'stazione', 52), (sheets.kit, 'kit', 120)):
        d.rectangle((x - 1, yy - 1, x + 32, yy + 32), fill=(139, 139, 139, 255))
        img.alpha_composite(nearest(icon, 2), (x, yy))
        img.alpha_composite(icon, (x + 48, yy + 8))
        d.text((x + 76, yy + 8), title + ': x2 e x1', fill=(210, 210, 210), font=CAP_SMALL)
    return [save(img, out, 'kit-di-riparazione.png')]


# --- uscita -----------------------------------------------------------------------------------------------------------
def chunks(path):
    with open(path, 'rb') as fh:
        data = fh.read()
    out, i = [], 8
    while i < len(data):
        n = int.from_bytes(data[i:i + 4], 'big')
        out.append(data[i + 4:i + 8].decode('latin-1'))
        i += 12 + n
    return out


def save(img, out, name):
    path = os.path.join(out, name)
    write_png(path, np.array(img.convert('RGBA')))
    kinds = chunks(path)
    if [k for k in kinds if k not in ('IHDR', 'IDAT', 'IEND')] or kinds[0] != 'IHDR' or kinds[-1] != 'IEND':
        raise SystemExit(f'FAIL: {path} ha i blocchi {kinds}')
    return path


def main():
    global CAP, CAP_SMALL
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument('--vanilla', required=True, help='risorse del gioco base estratte (carattere e sette icone)')
    ap.add_argument('--assets', help='cartella scritta da tools/art/gui.py --out')
    ap.add_argument('--out', default=OUT, help='cartella delle anteprime')
    args = ap.parse_args()
    assets = args.assets
    if not assets:
        assets = os.path.join(HERE, 'out', 'gui')
        subprocess.run([sys.executable, os.path.join(ART, 'gui.py'), '--out', assets, '--kit'], check=True)
    CAP, CAP_SMALL = caption_font(15), caption_font(12)
    sheets = Sheets(assets, args.vanilla)
    font = McFont(args.vanilla)
    os.makedirs(args.out, exist_ok=True)
    written = []
    written += screen_images(sheets, font, args.out)
    written += texture_images(sheets, args.out)
    written += hud_images(sheets, font, args.out)
    written += vignette_image(sheets, font, args.out)
    written += kit_image(sheets, args.out)
    for path in written:
        print(os.path.relpath(path, REPO), ', '.join(chunks(path)))


if __name__ == '__main__':
    main()
