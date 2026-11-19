"""Anteprime (MOCK-UP) della milestone 4 e controllo delle sovrapposizioni. Fuori dal repository.

Uso: python3 -I anteprime.py <cartella m4 dello scratchpad> <radice del repository>
Legge gli scatti di riferimento del repository e alcune texture del gioco base estratte nello scratchpad; scrive le
immagini in <m4>/anteprime e il controllo in <m4>/anteprime/controllo-disposizione.txt.
"""
import json
import math
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import layout as L  # noqa: E402

M4 = sys.argv[1]
REPO = sys.argv[2]
VAN = os.path.join(M4, 'vanilla', 'assets', 'minecraft')
OUT = os.path.join(M4, 'anteprime')
SHOTS = os.path.join(REPO, 'docs', 'test-in-gioco')
TEX = os.path.join(REPO, 'src', 'main', 'resources', 'assets', 'redstone_exoframes', 'textures')

# --- colori -----------------------------------------------------------------------------------------------------------
GREEN, YELLOW, RED, GREY = (0x55, 0xFF, 0x55), (0xFF, 0xFF, 0x55), (0xFF, 0x55, 0x55), (0x8A, 0x8A, 0x8A)
COND = {'intact': GREEN, 'damaged': YELLOW, 'critical': RED, 'broken': GREY}
ENERGY = {'NORMAL': GREEN, 'LOW': RED, 'EMPTY': GREY}
BRONZE, GUNMETAL = (0x8A, 0x6F, 0x52), (0x4A, 0x4F, 0x55)
COPPER, REDSTONE = (0xB8, 0x73, 0x33), (0xC0, 0x20, 0x1A)
DYES = [('white', 0xF9FFFE), ('orange', 0xF9801D), ('magenta', 0xC74EBD), ('light_blue', 0x3AB3DA),
        ('yellow', 0xFED83D), ('lime', 0x80C71F), ('pink', 0xF38BAA), ('gray', 0x474F52),
        ('light_gray', 0x9D9D97), ('cyan', 0x169C9C), ('purple', 0x8932B8), ('blue', 0x3C44AA),
        ('brown', 0x835432), ('green', 0x5E7C16), ('red', 0xB02E26), ('black', 0x1D1D21)]


def rgb(v):
    return ((v >> 16) & 255, (v >> 8) & 255, v & 255)


# --- font del gioco base -----------------------------------------------------------------------------------------------
class McFont:
    def __init__(self):
        d = json.load(open(os.path.join(VAN, 'font', 'include', 'default.json')))
        self.glyphs = {}
        for p in d['providers']:
            if p.get('type') != 'bitmap':
                continue
            img = Image.open(os.path.join(VAN, 'textures', p['file'].split(':')[1])).convert('RGBA')
            rows = p['chars']
            cw, chh = img.width // len(rows[0]), img.height // len(rows)
            ascent = p['ascent']
            for r, row in enumerate(rows):
                for c, char in enumerate(row):
                    if char in self.glyphs or char in '\x00 ':
                        continue
                    cell = img.crop((c * cw, r * chh, (c + 1) * cw, (r + 1) * chh))
                    a = cell.split()[3]
                    bbox = a.getbbox()
                    w = bbox[2] if bbox else 0
                    self.glyphs[char] = (a, w, 7 - ascent)

    def width(self, s):
        return sum(4 if ch == ' ' else self.glyphs.get(ch, (None, 5, 0))[1] + 1 for ch in s) - (1 if s else 0)

    def draw(self, im, xy, s, color, shadow=True):
        x, y = xy
        if shadow:
            sc = tuple(c // 4 for c in color[:3])
            self._draw(im, x + 1, y + 1, s, sc)
        self._draw(im, x, y, s, color[:3])

    def _draw(self, im, x, y, s, color):
        for ch in s:
            if ch == ' ':
                x += 4
                continue
            g = self.glyphs.get(ch)
            if g is None:
                x += 6
                continue
            a, w, up = g
            solid = Image.new('RGBA', a.size, color + (255,))
            im.paste(solid, (x, y + up), a)
            x += w + 1


FONT = McFont()


def text(im, xy, s, color=(224, 224, 224), shadow=True):
    FONT.draw(im, xy, s, color, shadow)


def text_right(im, x1, y, s, color=(224, 224, 224), shadow=True):
    text(im, (x1 - FONT.width(s), y), s, color, shadow)


def text_center(im, r, s, color=(255, 255, 255), shadow=True):
    x0, y0, x1, y1 = r
    w = FONT.width(s)
    text(im, (x0 + (x1 - x0 - w) // 2, y0 + (y1 - y0 - 8) // 2), s, color, shadow)


def fill(d, r, color):
    x0, y0, x1, y1 = r
    if x1 > x0 and y1 > y0:
        d.rectangle((x0, y0, x1 - 1, y1 - 1), fill=color)


def frame(d, r, light, dark, inner=None):
    """Riquadro con bordo a rilievo: luce in alto a sinistra, ombra in basso a destra."""
    x0, y0, x1, y1 = r
    if inner is not None:
        fill(d, r, inner)
    d.line((x0, y0, x1 - 1, y0), fill=light)
    d.line((x0, y0, x0, y1 - 1), fill=light)
    d.line((x0, y1 - 1, x1 - 1, y1 - 1), fill=dark)
    d.line((x1 - 1, y0, x1 - 1, y1 - 1), fill=dark)


def outline(d, r, color, width=1, dash=None):
    x0, y0, x1, y1 = r
    pts = [((x0, y0), (x1 - 1, y0)), ((x1 - 1, y0), (x1 - 1, y1 - 1)), ((x1 - 1, y1 - 1), (x0, y1 - 1)),
           ((x0, y1 - 1), (x0, y0))]
    for (ax, ay), (bx, by) in pts:
        if dash is None:
            d.line((ax, ay, bx, by), fill=color, width=width)
            continue
        length = max(abs(bx - ax), abs(by - ay))
        for s in range(0, length + 1, dash * 2):
            e = min(s + dash, length)
            fx = ax + (bx - ax) * s / max(length, 1)
            fy = ay + (by - ay) * s / max(length, 1)
            tx = ax + (bx - ax) * e / max(length, 1)
            ty = ay + (by - ay) * e / max(length, 1)
            d.line((fx, fy, tx, ty), fill=color, width=width)


def icon(name, kind='item'):
    if kind == 'mod':
        p = os.path.join(TEX, 'item', name + '.png')
    else:
        p = os.path.join(VAN, 'textures', 'item', name + '.png')
    return Image.open(p).convert('RGBA')


def mockup_label(im, scale=2):
    """L'etichetta MOCK-UP nell'angolo in alto a destra (pixel dello schermo)."""
    tmp = Image.new('RGBA', (80, 12), (0, 0, 0, 0))
    td = ImageDraw.Draw(tmp)
    fill(td, (0, 0, 80, 12), (160, 20, 20, 230))
    s = 'MOCK-UP'
    text(tmp, ((80 - FONT.width(s)) // 2, 2), s, (255, 255, 255), shadow=False)
    tmp = tmp.resize((80 * scale, 12 * scale), Image.NEAREST)
    im.alpha_composite(tmp, (im.width - 80 * scale - (6 if scale > 1 else 1), 6 if scale > 1 else 1))


def small_label(im, xy, s, color=(255, 255, 255), bg=(0, 0, 0, 170)):
    """Una didascalia piccola (font a 1x) con fondo scuro, in pixel dello schermo."""
    w = FONT.width(s) + 4
    tmp = Image.new('RGBA', (w, 11), bg)
    text(tmp, (2, 1), s, color, shadow=False)
    x, y = xy
    x = max(0, min(x, im.width - w))
    y = max(0, min(y, im.height - 11))
    im.alpha_composite(tmp, (x, y))


# --- scena di gioco --------------------------------------------------------------------------------------------------
def magenta_mask(px, w, h):
    m = Image.new('L', (w, h), 0)
    mp = m.load()
    for y in range(h):
        for x in range(w):
            r, g, b = px[x, y][:3]
            if r > 51 and b > 51 and g < 0.45 * r and g < 0.45 * b:
                mp[x, y] = 255
    return m


def recolor_magenta(im):
    """Il pezzo del braccio negli scatti della 3d e' magenta (colore di prova): qui torna bronzo, con la sua luce."""
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if r > 51 and b > 51 and g < 0.45 * r and g < 0.45 * b:
                lum = (r + b) / 2 / 255.0
                k = 0.35 + 0.95 * lum
                px[x, y] = (min(255, int(BRONZE[0] * k)), min(255, int(BRONZE[1] * k)), min(255, int(BRONZE[2] * k)), a)


def is_grass(c):
    r, g, b = c[:3]
    return g > r + 8 and g > b + 20 and g < 175 and r < 125


def hud_scene(main_right):
    """Scena a 960x540: lo scatto del braccio della 3d (mano destra o sinistra), senza la vecchia riga dell'energia,
    con le barre di stato della sopravvivenza prese dallo scatto della 3c."""
    name = 'braccio-rottame' if main_right else 'braccio-mancino'
    base = Image.open(os.path.join(SHOTS, 'm3d-screenshot', '1.21.1-neoforge-%s.png' % name)).convert('RGBA')
    px = base.load()
    # la riga dell'energia della 3c (in alto a sinistra) si copre con il cielo della stessa riga a destra
    for y in range(0, 34):
        c = px[700, y]
        for x in range(0, 420):
            px[x, y] = c
    recolor_magenta(base)
    surv = Image.open(os.path.join(SHOTS, 'm3c-screenshot', '1.21.1-neoforge-riga-energia.png')).convert('RGBA')
    sp = surv.load()
    for y in range(436, 494):
        for x in range(296, 666):
            c = sp[x, y]
            if not is_grass(c):
                px[x, y] = c
    return base


def gui_scene():
    """Scena scura a 960x540: la tuta nel mondo, con l'oscuramento che il gioco mette dietro una schermata."""
    base = Image.open(os.path.join(SHOTS, 'm3a-screenshot', '1.21.1-neoforge-armata-fronte.png')).convert('RGBA')
    dim = Image.new('RGBA', base.size)
    dd = ImageDraw.Draw(dim)
    for y in range(base.height):
        a = 0xC0 + (0xD0 - 0xC0) * y // base.height
        dd.line((0, y, base.width, y), fill=(16, 16, 16, a))
    base.alpha_composite(dim)
    return base


def suit_cutout():
    """La tuta armata ritagliata dallo scatto della 3a (fronte), senza cielo ne' erba: per l'anteprima 3D."""
    im = Image.open(os.path.join(SHOTS, 'm3a-screenshot', '1.21.1-neoforge-armata-fronte.png')).convert('RGBA')
    crop = im.crop((326, 112, 640, 458))
    px = crop.load()
    for y in range(crop.height):
        for x in range(crop.width):
            r, g, b, a = px[x, y]
            sky = b > 150 and b > r + 25 and b > g
            if sky or is_grass((r, g, b)):
                px[x, y] = (0, 0, 0, 0)
    return crop


# --- HUD ---------------------------------------------------------------------------------------------------------------
def draw_cell_glyph(d, x, y):
    fill(d, (x, y, x + 5, y + 8), (60, 63, 68))
    fill(d, (x + 1, y + 2, x + 4, y + 6), REDSTONE)
    fill(d, (x + 1, y, x + 4, y + 1), (150, 150, 150))


def draw_warning(d, r, color):
    x0, y0, x1, y1 = r
    d.polygon([(x0 + 3, y0), (x1 - 1, y1 - 1), (x0, y1 - 1)], fill=color)
    d.polygon([(x0 + 4, y0), (x1 - 1, y1 - 1), (x0 + 1, y1 - 1)], fill=color)
    fill(d, (x0 + 3, y0 + 2, x0 + 5, y0 + 5), (20, 20, 20))
    fill(d, (x0 + 3, y0 + 6, x0 + 5, y0 + 7), (20, 20, 20))


def draw_vignette(canvas, w, h):
    layer = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    lp = layer.load()
    for name, (x0, y0, x1, y1) in L.vignette_corners(w, h).items():
        cx = x0 if 'sx' in name else x1 - 1
        cy = y0 if 'alto' in name else y1 - 1
        for y in range(y0, y1):
            for x in range(x0, x1):
                dx = (x - cx) / 72.0
                dy = (y - cy) / 40.0
                dist = math.sqrt(dx * dx + dy * dy)
                a = 0.42 * max(0.0, 1.0 - dist) ** 2
                if a > 0:
                    lp[x, y] = (10, 12, 14, int(a * 255))
        if 'alto' in name:
            # riflessi del vetro: due strisce chiare in diagonale
            for k, off in ((0.16, 10), (0.10, 18)):
                for t in range(0, 26):
                    xx = (x0 + 8 + off + t) if 'sx' in name else (x1 - 9 - off - t)
                    yy = y0 + 2 + t // 2
                    if 0 <= xx < w and 0 <= yy < h:
                        lp[xx, yy] = (255, 255, 255, int(k * 255))
    canvas.alpha_composite(layer)


def draw_hud(canvas, w, h, main_right, state):
    """Disegna l'HUD su una tela in pixel dell'interfaccia. state: dict con level, energy (0-100), estate, spare,
    pieces (slot -> condizione o None)."""
    d = ImageDraw.Draw(canvas)
    level = state['level']
    if level == 'FULL' and state.get('vignette', True):
        draw_vignette(canvas, w, h)
        d = ImageDraw.Draw(canvas)
    R = L.hud_rects(w, h, main_right, level)
    fill(d, R['piastra'], (16, 18, 22, 150))
    outline(d, R['piastra'], (184, 115, 51, 110))
    if level == 'FULL':
        for slot in ('helmet', 'torso', 'left_arm', 'right_arm', 'left_leg', 'right_leg'):
            r = R['sagoma.' + slot]
            cond = state['pieces'].get(slot)
            if cond is None:
                fill(d, r, (0, 0, 0, 90))
                outline(d, r, (70, 70, 70, 255))
            else:
                fill(d, r, COND[cond] + (255,))
                outline(d, r, tuple(c // 3 for c in COND[cond]) + (255,))
    color = ENERGY[state['estate']]
    tx0, ty0, tx1, ty1 = R['testo']
    x = tx0
    if state['estate'] in ('LOW', 'EMPTY'):
        draw_warning(d, R['avviso'], color + (255,))
        x = R['avviso'][2] + 2
    pct = '%d%%' % state['energy'] if state.get('cell', True) else 'Nessuna cella'
    text(canvas, (x, ty0), pct, color if state.get('cell', True) else (170, 170, 170))
    spare = '×%d' % state['spare']
    sw = FONT.width(spare)
    text(canvas, (tx1 - sw, ty0), spare, (230, 230, 230) if state['spare'] else (150, 150, 150))
    draw_cell_glyph(d, tx1 - sw - 7, ty0)
    bx0, by0, bx1, by1 = R['barra']
    fill(d, R['barra'], (12, 13, 15, 255))
    fill(d, (bx0 + 1, by0 + 1, bx1 - 1, by1 - 1), (40, 42, 46, 255))
    if state.get('cell', True):
        fw = round((bx1 - bx0 - 2) * state['energy'] / 100.0)
        fill(d, (bx0 + 1, by0 + 1, bx0 + 1 + fw, by1 - 1), color + (255,))
        fill(d, (bx0 + 1, by0 + 1, bx0 + 1 + fw, by0 + 2), tuple(min(255, c + 60) for c in color) + (255,))
    return R


def annotate_hud(im, main_right, R, gui_scale=2):
    d = ImageDraw.Draw(im)
    active = 'braccio destro' if main_right else 'braccio sinistro'
    other = 'braccio sinistro' if main_right else 'braccio destro'
    for name, r in L.VANILLA_SCREEN.items():
        if name == active:
            outline(d, r, (255, 60, 60, 255), width=3)
            small_label(im, (r[0] + 6, r[1] + 6), 'braccio della tuta %s: %dx%d+%d+%d' % (
                'destro' if main_right else 'sinistro', r[2] - r[0], r[3] - r[1], r[0], r[1]), (255, 120, 120))
        elif name == other:
            outline(d, r, (200, 200, 200, 200), width=1, dash=6)
            small_label(im, (r[0] + 6, r[1] + 6), 'braccio dell\'altra mano (qui non disegnato) %dx%d+%d+%d' % (
                r[2] - r[0], r[3] - r[1], r[0], r[1]), (210, 210, 210))
        elif name.startswith('chat'):
            outline(d, r, (90, 220, 255, 220), width=1, dash=4)
            small_label(im, (r[0] + 4, r[1] + 3), 'righe della chat (fino a 10), x 0-664 y 280-460', (120, 230, 255))
        else:
            outline(d, r, (255, 230, 60, 230), width=1, dash=4)
            small_label(im, (r[0] + 2, r[1] - 12), name, (255, 235, 120))
    off = L.OFFHAND_SCREEN[main_right]
    outline(d, off, (255, 255, 255, 200), width=1, dash=3)
    small_label(im, (off[0] - 10, off[1] - 24), 'seconda mano', (255, 255, 255))
    hud = L.to_screen(R['piastra'], gui_scale)
    outline(d, (hud[0] - 2, hud[1] - 2, hud[2] + 2, hud[3] + 2), (80, 255, 80, 255), width=2)


def hud_image(filename, main_right, state, chat_line=None):
    w, h = 480, 270
    scene = hud_scene(main_right)
    canvas = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    if chat_line:
        d = ImageDraw.Draw(canvas)
        fill(d, (0, 221, 332, 230), (0, 0, 0, 128))
        text(canvas, (2, 222), chat_line, (255, 255, 255))
    R = draw_hud(canvas, w, h, main_right, state)
    big = canvas.resize((w * 2, h * 2), Image.NEAREST)
    scene.alpha_composite(big)
    hud = L.to_screen(R['piastra'])
    pad = 6
    crop = scene.crop((max(0, hud[0] - pad), hud[1] - pad, min(960, hud[2] + pad), min(540, hud[3] + pad)))
    zoom = crop.resize((crop.width * 2, crop.height * 2), Image.NEAREST)
    zx, zy = (296, 44)
    scene.alpha_composite(zoom, (zx, zy))
    zd = ImageDraw.Draw(scene)
    outline(zd, (zx - 2, zy - 2, zx + zoom.width + 2, zy + zoom.height + 2), (80, 255, 80, 255), width=2)
    small_label(scene, (zx, zy + zoom.height + 6), 'ingrandimento x2 dell\'HUD, che sta in %dx%d+%d+%d (pixel dello schermo)' % (hud[2] - hud[0], hud[3] - hud[1], hud[0], hud[1]), (120, 255, 120))
    annotate_hud(scene, main_right, R)
    cap = state['caption']
    small_label(scene, (296, 8), cap, (255, 255, 255))
    small_label(scene, (296, 22), 'mano principale: %s; scala dell\'interfaccia 2, finestra 960x540' % (
        'destra' if main_right else 'sinistra'), (220, 220, 220))
    mockup_label(scene)
    scene.convert('RGB').save(os.path.join(OUT, filename))
    return R


# --- GUI della stazione -------------------------------------------------------------------------------------------------
P_BG, P_LIGHT, P_DARK, P_EDGE = (58, 61, 66), (98, 103, 110), (28, 30, 34), (14, 15, 17)
S_BG, S_DARK, S_LIGHT = (36, 38, 42), (18, 19, 22), (84, 88, 95)


def slot(d, r):
    frame(d, r, S_DARK, S_LIGHT, S_BG)


def sunken(d, r, bg=(42, 44, 49)):
    frame(d, r, (22, 23, 26), (82, 86, 92), bg)


def button(canvas, d, r, label, hovered=False, pressed=False, disabled=False):
    bg = (52, 55, 60) if disabled else ((92, 70, 48) if pressed else (78, 82, 88))
    frame(d, r, (40, 42, 46) if pressed else (118, 123, 130), (26, 27, 30), bg)
    if hovered:
        outline(d, r, COPPER + (255,))
    if label:
        text_center(canvas, r, label, (120, 120, 120) if disabled else (255, 255, 255))


def put_icon(canvas, img, x, y):
    canvas.alpha_composite(img.resize((16, 16), Image.NEAREST), (x, y))


def count(canvas, x, y, n):
    s = str(n)
    text(canvas, (x + 17 - FONT.width(s), y + 9), s, (255, 255, 255))


def gui_image(filename, screen_w=480, screen_h=270, scale=2, scene=None, tooltip=True, caption=True):
    canvas = Image.new('RGBA', (screen_w, screen_h), (0, 0, 0, 0))
    d = ImageDraw.Draw(canvas)
    lx, ly = (screen_w - L.GUI_W) // 2, (screen_h - L.GUI_H) // 2

    def R(name):
        x0, y0, x1, y1 = L.GUI[name]
        return (lx + x0, ly + y0, lx + x1, ly + y1)

    def A(r):
        return (lx + r[0], ly + r[1], lx + r[2], ly + r[3])

    # fondo
    fill(d, (lx, ly, lx + L.GUI_W, ly + L.GUI_H), P_EDGE)
    frame(d, (lx + 1, ly + 1, lx + L.GUI_W - 1, ly + L.GUI_H - 1), P_LIGHT, P_DARK, P_BG)
    for (rx, ry) in ((4, 4), (L.GUI_W - 6, 4), (4, L.GUI_H - 6), (L.GUI_W - 6, L.GUI_H - 6)):
        fill(d, (lx + rx, ly + ry, lx + rx + 2, ly + ry + 2), COPPER)
    d.line((lx + 7, ly + 15, lx + L.GUI_W - 8, ly + 15), fill=COPPER + (180,))
    text(canvas, (R('titolo')[0], R('titolo')[1]), 'Stazione di manutenzione', (236, 190, 140))
    # rinforzato e blocco
    rb = R('rinforzato')
    d.polygon([(rb[0] + 2, rb[1] + 1), (rb[2] - 2, rb[1] + 1), (rb[2] - 2, rb[1] + 7), ((rb[0] + rb[2]) // 2, rb[3] - 1),
               (rb[0] + 2, rb[1] + 7)], fill=BRONZE, outline=(40, 30, 20))
    fill(d, ((rb[0] + rb[2]) // 2 - 1, rb[1] + 3, (rb[0] + rb[2]) // 2 + 1, rb[1] + 8), (230, 200, 150))
    bl = R('blocco')
    button(canvas, d, bl, '')
    fill(d, (bl[0] + 5, bl[1] + 5, bl[0] + 11, bl[1] + 10), (200, 200, 205))
    d.arc((bl[0] + 5, bl[1] + 1, bl[0] + 10, bl[1] + 8), 180, 360, fill=(200, 200, 205))
    # pannello del telaio
    sunken(d, R('pannello.telaio'))
    conds = {'helmet': 'intact', 'torso': 'damaged', 'right_arm': 'intact', 'left_arm': 'critical',
             'right_leg': 'intact', 'left_leg': 'broken'}
    icons = {'helmet': 'scrap_helmet', 'torso': 'scrap_torso', 'right_arm': 'scrap_arm', 'left_arm': 'scrap_arm',
             'right_leg': 'scrap_leg', 'left_leg': 'scrap_leg'}
    for sl, cond in conds.items():
        r = R('pezzo.' + sl)
        fill(d, r, COND[cond])
        slot(d, (r[0] + 1, r[1] + 1, r[2] - 1, r[3] - 1))
        put_icon(canvas, icon(icons[sl], 'mod'), r[0] + 2, r[1] + 2)
        if sl == 'torso':
            outline(d, (r[0] - 1, r[1] - 1, r[2] + 1, r[3] + 1), (255, 255, 255))
    d.line((lx + 11, ly + 92, lx + 69, ly + 92), fill=(82, 86, 92))
    c = R('cella')
    fill(d, c, REDSTONE)
    slot(d, (c[0] + 1, c[1] + 1, c[2] - 1, c[3] - 1))
    put_icon(canvas, icon('redstone_cell', 'mod'), c[0] + 2, c[1] + 2)
    button(canvas, d, R('tutti'), 'Tutti')
    # pannello del pezzo
    sunken(d, R('pannello.pezzo'))
    n = R('nome')
    text(canvas, (n[0], n[1]), 'Torso Rottame', (255, 255, 255))
    button(canvas, d, R('smonta'), '')
    sm = R('smonta')
    d.polygon([(sm[0] + 3, sm[1] + 7), (sm[0] + 6, sm[1] + 3), (sm[0] + 9, sm[1] + 7)], fill=(230, 230, 230))
    fill(d, (sm[0] + 3, sm[1] + 8, sm[0] + 10, sm[1] + 10), (230, 230, 230))
    cd = R('condizione')
    text(canvas, (cd[0], cd[1]), 'Danneggiato', YELLOW)
    text_right(canvas, R('durata.numeri')[2], R('durata.numeri')[1], '81 / 180', (200, 200, 200))
    db = R('durata.barra')
    fill(d, db, (12, 13, 15))
    fill(d, (db[0] + 1, db[1] + 1, db[0] + 1 + int((db[2] - db[0] - 2) * 81 / 180), db[3] - 1), YELLOW)
    m = R('materiale')
    slot(d, m)
    put_icon(canvas, icon('iron_ingot'), m[0] + 1, m[1] + 1)
    count(canvas, m[0] + 1, m[1] + 1, 12)
    button(canvas, d, R('ripara'), 'Ripara')
    button(canvas, d, R('ripara_tutto'), 'Tutto: 3')
    button(canvas, d, R('regione.primario'), 'Primario', pressed=True)
    button(canvas, d, R('regione.secondario'), 'Secondario')
    have = {'red': 4, 'white': 6, 'black': 3, 'blue': 2}
    for i, (dn, dv) in enumerate(DYES):
        r = A(L.swatch(i))
        fill(d, r, (12, 13, 15))
        col = rgb(dv)
        if dn not in have:
            col = tuple(int(v * 0.45 + 30) for v in col)
        fill(d, (r[0] + 1, r[1] + 1, r[2] - 1, r[3] - 1), col)
        if dn == 'red':
            outline(d, (r[0] - 1, r[1] - 1, r[2] + 1, r[3] + 1), (255, 255, 255))
    pd = R('predefinito')
    fill(d, pd, (12, 13, 15))
    fill(d, (pd[0] + 1, pd[1] + 1, pd[2] - 1, (pd[1] + pd[3]) // 2), BRONZE)
    fill(d, (pd[0] + 1, (pd[1] + pd[3]) // 2, pd[2] - 1, pd[3] - 1), GUNMETAL)
    # moduli: spazio riservato, vuoto (nessun widget morto)
    # anteprima
    pv = R('pannello.anteprima')
    sunken(d, pv, (24, 26, 30))
    cut = R('anteprima.ritaglio')
    for y in range(cut[1], cut[3]):
        k = (y - cut[1]) / (cut[3] - cut[1])
        d.line((cut[0], y, cut[2] - 1, y), fill=(int(22 + 16 * k), int(24 + 16 * k), int(28 + 18 * k)))
    d.ellipse((cut[0] + 18, cut[3] - 12, cut[2] - 18, cut[3] - 4), fill=(12, 12, 14))
    suit = suit_cutout()
    sh = cut[3] - cut[1] - 10
    sw = int(suit.width * sh / suit.height)
    canvas.alpha_composite(suit.resize((sw, sh), Image.LANCZOS),
                           (cut[0] + (cut[2] - cut[0] - sw) // 2, cut[1] + 3))
    for ax, flip in ((cut[0] + 4, 1), (cut[2] - 9, -1)):
        ay = cut[3] - 10
        pts = [(ax + (0 if flip > 0 else 5), ay + 3), (ax + (5 if flip > 0 else 0), ay), (ax + (5 if flip > 0 else 0), ay + 6)]
        d.polygon(pts, fill=(170, 170, 170))
    # inventario del giocatore
    text(canvas, (R('etichetta.inventario')[0], R('etichetta.inventario')[1]), 'Inventario', (200, 200, 200))
    inv = {(0, 0): ('copper_ingot', 20, 'item'), (0, 1): ('blue_dye', 2, 'item'), (1, 4): ('scrap_leg', 1, 'mod'),
           (2, 8): ('redstone', 9, 'item')}
    hot = {0: ('scrap_arm', 1, 'mod'), 1: ('redstone_cell', 1, 'mod'), 2: ('iron_ingot', 12, 'item'),
           3: ('red_dye', 4, 'item'), 4: ('white_dye', 6, 'item'), 5: ('black_dye', 3, 'item')}
    ix0, iy0 = R('inventario')[0], R('inventario')[1]
    for row in range(3):
        for col in range(9):
            r = (ix0 + col * 18, iy0 + row * 18, ix0 + col * 18 + 18, iy0 + row * 18 + 18)
            slot(d, r)
            if (row, col) in inv:
                nm, k, kind = inv[(row, col)]
                put_icon(canvas, icon(nm, kind), r[0] + 1, r[1] + 1)
                if k > 1:
                    count(canvas, r[0] + 1, r[1] + 1, k)
    hx0, hy0 = R('barra')[0], R('barra')[1]
    for col in range(9):
        r = (hx0 + col * 18, hy0, hx0 + col * 18 + 18, hy0 + 18)
        slot(d, r)
        if col in hot:
            nm, k, kind = hot[col]
            put_icon(canvas, icon(nm, kind), r[0] + 1, r[1] + 1)
            if k > 1:
                count(canvas, r[0] + 1, r[1] + 1, k)
    # preset
    sunken(d, R('pannello.preset'))
    text(canvas, (R('etichetta.preset')[0], R('etichetta.preset')[1]), 'Preset', (200, 200, 200))
    presets = [[BRONZE, GUNMETAL] * 6, [rgb(0xB02E26), rgb(0x1D1D21)] * 6, None]
    for k in range(3):
        parts = {nm: A(r) for nm, r in L.preset_parts(k).items()}
        text(canvas, (parts['numero'][0], parts['numero'][1]), str(k + 1), (220, 220, 220))
        cx0, cy0, cx1, cy1 = parts['colori']
        if presets[k] is None:
            outline(d, parts['colori'], (110, 110, 110), dash=2)
            text(canvas, (cx0 + 4, cy0 - 0), 'vuoto', (130, 130, 130), shadow=False)
        else:
            for i, colr in enumerate(presets[k]):
                fill(d, (cx0 + i * 6, cy0, cx0 + i * 6 + 5, cy1), colr)
        for bn in ('applica', 'salva'):
            button(canvas, d, parts[bn], '', disabled=(presets[k] is None and bn == 'applica'))
            b = parts[bn]
            if bn == 'applica':
                fill(d, (b[0] + 7, b[1] + 3, b[0] + 11, b[1] + 9), (210, 210, 210))
                fill(d, (b[0] + 6, b[1] + 9, b[0] + 12, b[1] + 13), (184, 115, 51))
            else:
                fill(d, (b[0] + 5, b[1] + 3, b[0] + 13, b[1] + 13), (180, 180, 190))
                fill(d, (b[0] + 7, b[1] + 3, b[0] + 11, b[1] + 6), (60, 63, 68))
    rc = R('riga.costi')
    text(canvas, (rc[0], rc[1]), 'Costo: 1 Tinta rossa', (200, 200, 200))
    text(canvas, (rc[0], rc[1] + 10), 'Ne hai 4', (150, 150, 150))
    # suggerimento sulla tinta rossa
    if tooltip:
        sr = A(L.swatch(14))
        tx, ty = sr[2] + 6, sr[1] - 20
        lines = [('Tinta rossa', (255, 255, 255)), ('Primario del torso', (170, 170, 170)),
                 ('Clic: 1 tinta (ne hai 4)', (170, 170, 170))]
        tw = max(FONT.width(s) for s, _ in lines) + 8
        th = 10 * len(lines) + 6
        fill(d, (tx, ty, tx + tw, ty + th), (16, 0, 16, 240))
        outline(d, (tx + 1, ty + 1, tx + tw - 1, ty + th - 1), (80, 0, 255, 160))
        for i, (s, col) in enumerate(lines):
            text(canvas, (tx + 4, ty + 4 + i * 10), s, col)
        cx, cy = sr[0] + 6, sr[1] + 5
        d.polygon([(cx, cy), (cx, cy + 10), (cx + 3, cy + 7), (cx + 7, cy + 7)], fill=(255, 255, 255), outline=(0, 0, 0))
    big = canvas.resize((screen_w * scale, screen_h * scale), Image.NEAREST)
    base = scene if scene is not None else gui_scene()
    base = base.resize((screen_w * scale, screen_h * scale)) if base.size != (screen_w * scale, screen_h * scale) else base
    base.alpha_composite(big)
    if caption:
        small_label(base, (8, 8), 'Stazione di manutenzione: GUI %dx%d pixel dell\'interfaccia, scala 2, finestra %dx%d' % (
            L.GUI_W, L.GUI_H, screen_w * scale, screen_h * scale), (255, 255, 255))
        mockup_label(base)
    else:
        mockup_label(base, scale=1)
    base.convert('RGB').save(os.path.join(OUT, filename))


# --- blocco nel mondo (schizzo a voxel, tre quarti) ------------------------------------------------------------------
U = 46
CX, CY = 470, 300


def iso(x, y, z):
    return (CX + (x - z) * math.cos(math.radians(30)) * U, CY + (x + z) * math.sin(math.radians(30)) * U - y * U)


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3])


def box(d, x0, y0, z0, x1, y1, z1, color, top=None, front=None, right=None, edge=(20, 20, 22)):
    top = top or shade(color, 1.15)
    front = front or shade(color, 0.85)
    right = right or shade(color, 0.65)
    d.polygon([iso(x0, y1, z0), iso(x1, y1, z0), iso(x1, y1, z1), iso(x0, y1, z1)], fill=top, outline=edge)
    d.polygon([iso(x0, y0, z1), iso(x1, y0, z1), iso(x1, y1, z1), iso(x0, y1, z1)], fill=front, outline=edge)
    d.polygon([iso(x1, y0, z0), iso(x1, y0, z1), iso(x1, y1, z1), iso(x1, y1, z0)], fill=right, outline=edge)


def block_image(filename):
    im = Image.new('RGBA', (960, 540), (135, 175, 225, 255))
    d = ImageDraw.Draw(im)
    for y in range(540):
        k = y / 540
        d.line((0, y, 960, y), fill=(int(120 + 40 * k), int(165 + 30 * k), int(230 - 10 * k)))
    grass = (92, 140, 62)
    for gx in range(-4, 7):
        for gz in range(-3, 6):
            c = grass if (gx + gz) % 2 == 0 else shade(grass, 0.93)
            d.polygon([iso(gx, 0, gz), iso(gx + 1, 0, gz), iso(gx + 1, 0, gz + 1), iso(gx, 0, gz + 1)], fill=c)
    # zona del telaio: entro 2 blocchi dall'impronta (x 0..2, z 0..1), in orizzontale
    pts = []
    for a in range(0, 361, 6):
        t = math.radians(a)
        cx = min(max(math.cos(t) * 10, 0), 2)
        cz = min(max(math.sin(t) * 10, 0), 1)
        pts.append(iso(cx + 2 * math.cos(t), 0.01, cz + 2 * math.sin(t)))
    for i in range(0, len(pts) - 1, 2):
        d.line((pts[i], pts[i + 1]), fill=(255, 220, 90), width=3)
    # la stazione: due meta' (principale x 0..1, laterale x 1..2), fronte verso +z
    steel, steel_d = (70, 74, 80), (46, 49, 54)
    box(d, 0, 0, 0, 2, 0.12, 1, steel_d)                       # zoccolo
    box(d, 0.06, 0.12, 0.06, 1.94, 0.8, 0.94, steel)           # corpo
    box(d, 0, 0.8, 0, 2, 1.0, 1, BRONZE)                       # piano di lavoro (bronzo)
    for xx in (0.25, 0.75, 1.25, 1.75):                        # griglia sul piano
        d.line((iso(xx, 1.0, 0.1), iso(xx, 1.0, 0.9)), fill=shade(BRONZE, 0.75), width=2)
    box(d, 0.15, 0.3, 0.94, 0.85, 0.72, 0.99, (30, 32, 36))    # console: schermo
    box(d, 0.2, 0.36, 0.99, 0.8, 0.66, 1.0, REDSTONE, front=(200, 40, 30))
    for kx in (0.3, 0.5, 0.7):
        box(d, kx - 0.04, 0.2, 0.94, kx + 0.04, 0.26, 1.02, COPPER)
    for dy in (0.22, 0.5):                                     # cassetti della meta' laterale
        box(d, 1.15, dy, 0.94, 1.85, dy + 0.22, 0.99, shade(steel, 1.1))
        box(d, 1.42, dy + 0.08, 0.99, 1.58, dy + 0.13, 1.02, COPPER)
    box(d, 1.94, 0.12, 0.3, 2.0, 0.8, 0.36, COPPER)            # tubo di rame sul fianco
    box(d, 1.94, 0.12, 0.64, 2.0, 0.8, 0.7, COPPER)
    # il telaio davanti alla stazione (entro 2 blocchi), alto circa 3 blocchi
    fx, fz = -1.0, 1.6
    s = 1.0
    def part(a, b, c, dd, e, f, col):
        box(d, fx + a * s, b * s, fz + c * s, fx + dd * s, e * s, fz + f * s, col)
    part(-0.35, 0.0, -0.25, -0.05, 1.25, 0.2, (64, 66, 72))
    part(0.05, 0.0, -0.25, 0.35, 1.25, 0.2, (64, 66, 72))
    part(-0.38, 0.25, -0.2, -0.02, 1.15, 0.22, BRONZE)
    part(0.02, 0.25, -0.2, 0.38, 1.15, 0.22, BRONZE)
    part(-0.55, 1.25, -0.35, 0.55, 2.35, 0.3, BRONZE)
    part(-0.9, 1.15, -0.25, -0.55, 2.3, 0.2, GUNMETAL)
    part(0.55, 1.15, -0.25, 0.9, 2.3, 0.2, GUNMETAL)
    part(-0.25, 2.35, -0.25, 0.25, 2.85, 0.2, (70, 72, 78))
    part(-0.12, 1.75, 0.3, 0.12, 1.95, 0.34, REDSTONE)
    # in alternativa sul piano della stazione: contorno tratteggiato
    gx0, gz0 = 0.6, 0.15
    for (a, b) in (((gx0, 1.0, gz0 + 0.35), (gx0, 3.97, gz0 + 0.35)), ((gx0 + 0.8, 1.0, gz0 + 0.35), (gx0 + 0.8, 3.97, gz0 + 0.35)),
                   ((gx0, 3.97, gz0 + 0.35), (gx0 + 0.8, 3.97, gz0 + 0.35))):
        p0, p1 = iso(*a), iso(*b)
        steps = 14
        for i in range(0, steps, 2):
            q0 = (p0[0] + (p1[0] - p0[0]) * i / steps, p0[1] + (p1[1] - p0[1]) * i / steps)
            q1 = (p0[0] + (p1[0] - p0[0]) * (i + 1) / steps, p0[1] + (p1[1] - p0[1]) * (i + 1) / steps)
            d.line((q0, q1), fill=(255, 255, 255), width=2)
    small_label(im, (int(iso(1.5, 4.1, 0.5)[0]) - 30, int(iso(1.5, 4.1, 0.5)[1])), 'oppure sopra la stazione (servono 3 blocchi liberi sopra)', (255, 255, 255))
    small_label(im, (int(iso(-1.6, 0, 2.6)[0]) - 60, int(iso(-1.6, 0, 2.6)[1]) + 6), 'zona del telaio: entro 2 blocchi dall\'impronta', (255, 230, 120))
    small_label(im, (int(iso(0.5, 0, 1.0)[0]) - 150, int(iso(0.5, 0, 1.0)[1]) + 26), 'meta\' principale (console)', (255, 255, 255))
    small_label(im, (int(iso(1.5, 0, 1.0)[0]) + 10, int(iso(1.5, 0, 1.0)[1]) + 16), 'meta\' laterale (cassetti)', (255, 255, 255))
    small_label(im, (8, 8), 'Stazione di manutenzione 2x1 nel mondo, vista a tre quarti (schizzo a voxel)', (255, 255, 255))
    small_label(im, (8, 22), 'acciaio scuro, piano di bronzo, rame, schermo rosso redstone; il telaio davanti, entro 2 blocchi', (220, 220, 220))
    mockup_label(im)
    im.convert('RGB').save(os.path.join(OUT, filename))


# --- controllo delle sovrapposizioni ----------------------------------------------------------------------------------
def scaled_arm_boxes(w, h):
    """I due riquadri del braccio in pixel dell'interfaccia per una finestra con proporzioni w:h, ricavati da quelli
    misurati a 960x540 (scala 2): il braccio scala con l'altezza ed e' ancorato al bordo laterale e a quello basso."""
    k = h / 270.0
    bw, bh = 143 * k, 102.5 * k
    left_edge = w / 2 + (337 - 240) * k
    return {'braccio destro': (left_edge, h - bh, w, h), 'braccio sinistro': (0, h - bh, w - left_edge, h)}


def check():
    lines = []
    ok = True
    lines.append('Controllo della disposizione (pixel dello schermo, finestra 960x540, scala dell\'interfaccia 2)')
    for main_right in (True, False):
        hand = 'destra' if main_right else 'sinistra'
        active = 'braccio destro' if main_right else 'braccio sinistro'
        for level in ('FULL', 'BAR_ONLY'):
            R = L.hud_rects(480, 270, main_right, level)
            hud = L.to_screen(R['piastra'])
            res = []
            for name, r in L.VANILLA_SCREEN.items():
                if name.startswith('braccio') and name != active:
                    continue
                hit = L.intersects(hud, r)
                ok &= not hit
                res.append('%s %s' % (name, 'TOCCA' if hit else 'libero'))
            off = L.OFFHAND_SCREEN[main_right]
            hit = L.intersects(hud, off)
            ok &= not hit
            res.append('seconda mano %s' % ('TOCCA' if hit else 'libero'))
            trans = [n for n, r in L.TRANSIENT_SCREEN.items() if L.intersects(hud, r)]
            lines.append('  mano %s, %s: HUD %dx%d+%d+%d -> %s; di passaggio: %s' % (
                hand, level, hud[2] - hud[0], hud[3] - hud[1], hud[0], hud[1], ', '.join(res),
                ', '.join(trans) if trans else 'nessuno'))
            other = 'braccio sinistro' if main_right else 'braccio destro'
            if L.intersects(hud, L.VANILLA_SCREEN[other]):
                lines.append('    nota: sta nel riquadro dell\'altro braccio, che con questa mano non si disegna '
                             '(salvo la mappa a due mani)')
    # vignetta
    for name, r in L.vignette_corners(480, 270).items():
        rs = L.to_screen(r)
        hits = [n for n, v in L.VANILLA_SCREEN.items() if not n.startswith('braccio') and L.intersects(rs, v)]
        trans = [n for n, v in L.TRANSIENT_SCREEN.items() if L.intersects(rs, v)]
        lines.append('  %s %dx%d+%d+%d: %s; di passaggio: %s' % (
            name, rs[2] - rs[0], rs[3] - rs[1], rs[0], rs[1],
            ('tocca ' + ', '.join(hits)) if hits else 'nessun elemento fisso',
            ', '.join(trans) if trans else 'nessuno'))
    # altre grandezze dell'interfaccia
    lines.append('Altre grandezze dell\'interfaccia (pixel dell\'interfaccia; braccio scalato con l\'altezza):')
    for (w, h) in ((427, 240), (480, 270), (640, 360), (683, 384), (640, 270), (341, 256), (320, 240)):
        for main_right in (True, False):
            R = L.hud_rects(w, h, main_right, 'FULL')
            hud = R['piastra']
            arms = scaled_arm_boxes(w, h)
            active = arms['braccio destro' if main_right else 'braccio sinistro']
            hot = (w // 2 - 91, h - 50, w // 2 + 91, h)
            offr = (w // 2 - 120, h - 23, w // 2 - 91, h) if main_right else (w // 2 + 91, h - 23, w // 2 + 120, h)
            chat = (0, h - 130, 332, h - 40)
            cross = (w // 2 - 15, h // 2 - 15, w // 2 + 15, h // 2 + 15)
            hits = [n for n, r in (('braccio', active), ('barra e stato', hot), ('seconda mano', offr), ('chat', chat),
                                    ('mirino', cross)) if L.intersects(hud, r)]
            where = 'in alto a sinistra (stretta)' if L.narrow(w) else ('in basso a sinistra' if main_right else 'in basso a destra')
            boss = (w // 2 - 91, 3, w // 2 + 91, 19)
            note = ''
            if L.narrow(w) and L.intersects(hud, boss):
                note = ' (tocca le barre dei boss, di passaggio)'
            if hits:
                ok = False
            lines.append('  %dx%d mano %s: %s, %s%s' % (w, h, 'destra' if main_right else 'sinistra', where,
                                                          ('TOCCA ' + ', '.join(hits)) if hits else 'libero', note))
    # GUI
    lines.append('GUI della stazione %dx%d (pixel dell\'interfaccia):' % (L.GUI_W, L.GUI_H))
    for (w, h) in ((320, 240), (427, 240), (480, 270)):
        fits = L.GUI_W <= w and L.GUI_H <= h
        ok &= fits
        lines.append('  in %dx%d: %s (margini %d e %d)' % (w, h, 'ci sta' if fits else 'NON ci sta', (w - L.GUI_W) // 2,
                                                          (h - L.GUI_H) // 2))
    widgets = {k: v for k, v in L.GUI.items() if not k.startswith('pannello.') and k not in ('anteprima.ritaglio',)}
    for i in range(16):
        widgets['tinta.%d' % i] = L.swatch(i)
    for k in range(3):
        for nm, r in L.preset_parts(k).items():
            widgets['preset.%d.%s' % (k, nm)] = r
    widgets.pop('tavolozza')
    for k in range(3):
        widgets.pop('preset.%d' % k)
    names = sorted(widgets)
    clashes = []
    for i, a in enumerate(names):
        for b in names[i + 1:]:
            if L.intersects(widgets[a], widgets[b]):
                clashes.append('%s/%s' % (a, b))
    ok &= not clashes
    lines.append('  widget sovrapposti: %s' % (', '.join(clashes) if clashes else 'nessuno (%d widget)' % len(names)))
    panels = {k: v for k, v in L.GUI.items() if k.startswith('pannello.')}
    outside = [n for n, r in widgets.items() if not (0 <= r[0] and r[2] <= L.GUI_W and 0 <= r[1] and r[3] <= L.GUI_H)]
    ok &= not outside
    lines.append('  fuori dalla schermata: %s' % (', '.join(outside) if outside else 'nessuno'))
    lines.append('ESITO: %s' % ('tutto libero' if ok else 'C\'E\' UNA SOVRAPPOSIZIONE'))
    return '\n'.join(lines), ok


def main():
    os.makedirs(OUT, exist_ok=True)
    full = {'helmet': 'intact', 'torso': 'intact', 'left_arm': 'intact', 'right_arm': 'intact', 'left_leg': 'intact',
            'right_leg': 'intact'}
    st1 = {'level': 'FULL', 'energy': 100, 'estate': 'NORMAL', 'spare': 2, 'pieces': dict(full),
           'caption': '1. energia piena, sei pezzi integri, vignetta accesa'}
    st2 = {'level': 'FULL', 'energy': 8, 'estate': 'LOW', 'spare': 1,
           'pieces': dict(full, right_arm='damaged', torso='damaged'),
           'caption': '2. energia bassa (8%, avviso), braccio destro e torso danneggiati'}
    st3 = {'level': 'FULL', 'energy': 54, 'estate': 'NORMAL', 'spare': 0,
           'pieces': dict(full, left_leg='broken', torso='critical', left_arm=None),
           'caption': '3. gamba sinistra rotta, torso critico, braccio sinistro non montato'}
    st4 = {'level': 'BAR_ONLY', 'energy': 37, 'estate': 'NORMAL', 'spare': 1, 'pieces': {},
           'caption': '4. senza elmo (o con l\'elmo rotto): solo la barra dell\'energia'}
    hud_image('hud-destra-1-energia-piena.png', True, st1, chat_line='<Exo> pronto alla stazione')
    hud_image('hud-destra-2-energia-bassa-pezzo-danneggiato.png', True, st2)
    hud_image('hud-destra-3-pezzo-rotto.png', True, st3)
    hud_image('hud-destra-4-senza-elmo.png', True, st4)
    hud_image('hud-sinistra-2-energia-bassa-pezzo-danneggiato.png', False, st2)
    gui_image('gui-stazione.png')
    small = gui_scene().resize((640, 480))
    gui_image('gui-stazione-640x480.png', 320, 240, 2, scene=small, tooltip=False, caption=False)
    block_image('blocco-stazione.png')
    report, ok = check()
    open(os.path.join(OUT, 'controllo-disposizione.txt'), 'w').write(report + '\n')
    print(report)
    return 0 if ok else 1


if __name__ == '__main__':
    sys.exit(main())
