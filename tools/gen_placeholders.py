#!/usr/bin/env python3
"""Genera in modo deterministico le texture placeholder (solo stdlib).

Uso, dalla radice del repository:
    python3 -I tools/gen_placeholders.py               scrive nei sorgenti (src/main/resources...)
    python3 -I tools/gen_placeholders.py --out <dir>   scrive gli stessi file sotto <dir>, con i percorsi relativi a
                                                       src/main (resources/..., resources-mc1.20.1/...): lo usa
                                                       tools/check_art.sh per confrontarli con quelli committati
Disegni originali: nessun riferimento a opere o marchi di terzi.
"""
import argparse
import os
import struct
import zlib

MAIN = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main")
ROOT = os.path.join(MAIN, "resources")
ITEM_DIR = os.path.join(ROOT, "assets", "redstone_exoframes", "textures", "item")
BLOCK_DIR = os.path.join(ROOT, "assets", "redstone_exoframes", "textures", "block")
CELL_PATH = os.path.join(ITEM_DIR, "redstone_cell.png")
ICON_PATH = os.path.join(ROOT, "icon.png")
# Texture del layer armatura vanilla di 1.20.1 per il materiale "redstone_exoframes_frame" (nome senza namespace, con
# il prefisso del mod perche' non coincida con quello di altre mod o pacchetti di risorse): solo nei jar 1.20.1,
# dalla cartella di risorse per versione. Completamente trasparenti: il layer vanilla non deve mostrare nulla.
ARMOR_DIR_1201 = os.path.join(MAIN, "resources-mc1.20.1", "assets", "minecraft", "textures", "models", "armor")
ARMOR_LAYER_PATHS = [os.path.join(ARMOR_DIR_1201, "redstone_exoframes_frame_layer_%d.png" % n) for n in (1, 2)]

CLEAR = (0, 0, 0, 0)


def write_png(path, width, height, pixels):
    """Scrive una PNG RGBA 8 bit (filtro 0, zlib). pixels: lista di righe di (r,g,b,a)."""
    raw = bytearray()
    for row in pixels:
        raw.append(0)
        for r, g, b, a in row:
            raw += bytes((r, g, b, a))

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
           + chunk(b"IEND", b""))
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)


def canvas(w, h):
    return [[CLEAR] * w for _ in range(h)]


def rect(px, x0, y0, x1, y1, color):
    """Riempie il rettangolo [x0,x1] x [y0,y1] (estremi inclusi)."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px[y][x] = color


def draw_cell():
    """Cella di redstone 16x16: cilindro grigio-rame con tre tacche rosse."""
    px = canvas(16, 16)
    outline = (43, 38, 36, 255)
    # fasce di ombreggiatura da sinistra a destra (x 4..11)
    shades = [(120, 100, 88), (150, 124, 104), (178, 142, 112), (168, 136, 108),
              (140, 118, 100), (116, 98, 88), (92, 80, 74), (74, 66, 62)]
    for i, (r, g, b) in enumerate(shades):
        rect(px, 4 + i, 2, 4 + i, 13, (r, g, b, 255))
    # tappi grigi metallici in alto e in basso
    rect(px, 4, 2, 11, 3, (150, 150, 156, 255))
    rect(px, 4, 12, 11, 13, (110, 110, 118, 255))
    rect(px, 5, 1, 10, 1, (170, 170, 176, 255))
    rect(px, 5, 14, 10, 14, (80, 80, 88, 255))
    # contorno
    rect(px, 3, 2, 3, 13, outline)
    rect(px, 12, 2, 12, 13, outline)
    rect(px, 5, 0, 10, 0, outline)
    rect(px, 5, 15, 10, 15, outline)
    for x, y in ((4, 1), (11, 1), (4, 14), (11, 14)):
        px[y][x] = outline
    # tre tacche di redstone
    for y in (5, 7, 9):
        rect(px, 5, y, 10, y, (200, 24, 24, 255))
        px[y][6] = (255, 70, 50, 255)
    return px


def draw_icon():
    """Icona 64x64: quadrato arrotondato scuro con esoscheletro in rame."""
    n = 64
    px = canvas(n, n)
    bg = (28, 30, 38, 255)
    border = (58, 62, 76, 255)
    radius = 10
    for y in range(n):
        for x in range(n):
            cx = min(max(x, radius), n - 1 - radius)
            cy = min(max(y, radius), n - 1 - radius)
            d2 = (x - cx) ** 2 + (y - cy) ** 2
            if d2 <= radius * radius:
                edge = d2 > (radius - 2) ** 2 or min(x, y, n - 1 - x, n - 1 - y) < 2
                px[y][x] = border if edge else bg
    copper = (196, 114, 64, 255)
    light = (232, 160, 104, 255)
    dark = (128, 70, 40, 255)
    red = (220, 30, 30, 255)
    rect(px, 26, 8, 37, 18, copper)      # casco
    rect(px, 26, 8, 37, 9, light)
    rect(px, 28, 12, 35, 14, red)        # visore
    rect(px, 22, 20, 41, 38, copper)     # busto
    rect(px, 22, 20, 41, 22, light)
    rect(px, 28, 26, 35, 32, dark)       # nucleo
    rect(px, 30, 28, 33, 30, red)
    rect(px, 12, 20, 20, 25, light)      # spalline e braccia
    rect(px, 43, 20, 51, 25, light)
    rect(px, 13, 26, 18, 42, copper)
    rect(px, 45, 26, 50, 42, copper)
    rect(px, 12, 43, 19, 47, dark)
    rect(px, 44, 43, 51, 47, dark)
    rect(px, 24, 40, 30, 54, copper)     # gambe
    rect(px, 33, 40, 39, 54, copper)
    rect(px, 22, 55, 31, 58, dark)       # piedi
    rect(px, 32, 55, 41, 58, dark)
    return px


# Palette dei segnaposto: quella della cella (contorno scuro, rame, metallo, redstone).
OUTLINE = (43, 38, 36, 255)
COPPER = (168, 136, 108, 255)
COPPER_LIGHT = (178, 142, 112, 255)
COPPER_DARK = (116, 98, 88, 255)
METAL = (150, 150, 156, 255)
METAL_DARK = (110, 110, 118, 255)
REDSTONE = (200, 24, 24, 255)


def outline(px, color=OUTLINE):
    """Contorna di color i pixel trasparenti vicini (4-connessi) a un pixel pieno."""
    h, w = len(px), len(px[0])
    filled = [[px[y][x][3] > 0 for x in range(w)] for y in range(h)]
    for y in range(h):
        for x in range(w):
            if filled[y][x]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and filled[ny][nx]:
                    px[y][x] = color
                    break
    return px


def draw_helmet():
    """Attacco elmo 16x16: calotta di rame con visore di redstone."""
    px = canvas(16, 16)
    rect(px, 4, 2, 11, 3, COPPER_LIGHT)    # sommita'
    rect(px, 3, 4, 12, 11, COPPER)         # calotta
    rect(px, 3, 4, 12, 4, COPPER_LIGHT)
    rect(px, 4, 7, 11, 8, REDSTONE)        # visore
    rect(px, 3, 11, 12, 11, COPPER_DARK)
    rect(px, 6, 9, 9, 12, CLEAR)           # apertura del collo
    rect(px, 3, 12, 4, 12, METAL_DARK)     # guance
    rect(px, 11, 12, 12, 12, METAL_DARK)
    return outline(px)


def draw_torso():
    """Attacco torso 16x16: busto con spalline e nucleo di redstone."""
    px = canvas(16, 16)
    rect(px, 1, 2, 4, 5, METAL)            # spalline
    rect(px, 11, 2, 14, 5, METAL)
    rect(px, 4, 2, 11, 13, COPPER)         # busto
    rect(px, 4, 2, 11, 3, COPPER_LIGHT)
    rect(px, 6, 1, 9, 2, CLEAR)            # scollo
    rect(px, 6, 6, 9, 9, COPPER_DARK)      # nucleo
    rect(px, 7, 7, 8, 8, REDSTONE)
    rect(px, 4, 12, 11, 13, METAL_DARK)    # cintura
    return outline(px)


def draw_legs():
    """Attacco gambe 16x16: cintura e due gambali separati."""
    px = canvas(16, 16)
    rect(px, 3, 1, 12, 3, METAL_DARK)      # cintura
    rect(px, 7, 2, 8, 2, REDSTONE)
    rect(px, 3, 4, 6, 14, COPPER)          # gambali
    rect(px, 9, 4, 12, 14, COPPER)
    rect(px, 3, 4, 3, 14, COPPER_LIGHT)
    rect(px, 9, 4, 9, 14, COPPER_LIGHT)
    rect(px, 3, 8, 6, 9, METAL)            # ginocchiere
    rect(px, 9, 8, 12, 9, METAL)
    return outline(px)


def draw_boots():
    """Attacco stivali 16x16: due stivali con suola metallica."""
    px = canvas(16, 16)
    rect(px, 2, 4, 5, 11, COPPER)          # gambi
    rect(px, 9, 4, 12, 11, COPPER)
    rect(px, 2, 4, 5, 5, COPPER_LIGHT)
    rect(px, 9, 4, 12, 5, COPPER_LIGHT)
    rect(px, 2, 10, 7, 12, COPPER)         # punte
    rect(px, 9, 10, 14, 12, COPPER)
    rect(px, 2, 13, 7, 13, METAL_DARK)     # suole
    rect(px, 9, 13, 14, 13, METAL_DARK)
    px[7][3] = REDSTONE
    px[7][10] = REDSTONE
    return outline(px)


PIECE_ICONS = (("exoframe_helmet", draw_helmet), ("exoframe_torso", draw_torso),
               ("exoframe_legs", draw_legs), ("exoframe_boots", draw_boots))


# Pezzi Rottame da montare sul telaio (parte 3b): sagome originali in bronzo e acciaio, disegnate come griglie di
# caratteri 16x16 (una riga per stringa), poi contornate. Legenda in SPRITE_COLORS; "." e' trasparente.
BRONZE = (176, 128, 64, 255)
BRONZE_LIGHT = (214, 164, 92, 255)
BRONZE_DARK = (122, 84, 44, 255)
STEEL = (138, 146, 156, 255)
STEEL_LIGHT = (184, 192, 202, 255)
STEEL_DARK = (86, 92, 104, 255)
SPRITE_COLORS = {"B": BRONZE, "b": BRONZE_LIGHT, "d": BRONZE_DARK,
                 "S": STEEL, "s": STEEL_LIGHT, "k": STEEL_DARK, "R": REDSTONE}

SCRAP_HELMET = (
    "................",
    "......bbbb......",
    "....bbBBBBbb....",
    "...bBBBBBBBBd...",
    "...bBBBBBBBBd...",
    "..sSSSSSSSSSSk..",
    "..sSkkkkkkkkSk..",
    "..sSkRRRRRRkSk..",
    "..sSkkkkkkkkSk..",
    "..bBBBBBBBBBBd..",
    "..bBBd....bBBd..",
    "..bBBd....bBBd..",
    "..sSSk....sSSk..",
    "................",
    "................",
    "................",
)

SCRAP_TORSO = (
    "................",
    "..sss......sss..",
    ".sSSSbbbbbbSSSk.",
    ".sSSkBBBBBBkSSk.",
    ".kkkbBBBBBBdkkk.",
    "....bBBkkBBd....",
    "....bBkRRkBd....",
    "....bBkRRkBd....",
    "....bBBkkBBd....",
    "....sSSSSSSk....",
    "....bBBBBBBd....",
    "....bBBBBBBd....",
    "....kkkkkkkk....",
    "................",
    "................",
    "................",
)

SCRAP_ARM = (
    "................",
    ".....sssss......",
    "....sSSSSSSk....",
    "....sSSSSSSk....",
    ".....bBBBBd.....",
    ".....bBBBBd.....",
    ".....kSSSSk.....",
    ".....bBRBBd.....",
    ".....bBBBBd.....",
    ".....bBBBBd.....",
    ".....sSSSSk.....",
    "....sSSSSSSk....",
    "....kSkSkSkk....",
    "................",
    "................",
    "................",
)

SCRAP_LEG = (
    "................",
    "......bbbbb.....",
    "......bBBBBd....",
    "......bBBBBd....",
    "......bBBBBd....",
    "......sSSSSk....",
    "......kSRSkk....",
    "......bBBBBd....",
    "......bBBBBd....",
    "......bBBBBd....",
    "......bBBBBd....",
    "......sSSSSk....",
    "......sSSSSSSk..",
    "......kkkkkkkk..",
    "................",
    "................",
)


def draw_sprite(rows):
    """Disegna una griglia di caratteri 16x16 con SPRITE_COLORS e la contorna."""
    if len(rows) != 16 or any(len(row) != 16 for row in rows):
        raise ValueError("sprite must be 16x16")
    px = canvas(16, 16)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                px[y][x] = SPRITE_COLORS[ch]
    return outline(px)


# Telaio vuoto (parte 3b), l'oggetto che lo piazza: lo scheletro d'acciaio della tuta senza pezzi, con il nucleo di
# redstone nel petto.
EXOFRAME = (
    "................",
    "......kssk......",
    "......kSSk......",
    ".......kk.......",
    "..sssssSSsssss..",
    "..kS..kRRk..Sk..",
    "..kS...kk...Sk..",
    "..kS...SS...Sk..",
    "..kS...SS...Sk..",
    ".......SS.......",
    ".....ssSSss.....",
    ".....kS..Sk.....",
    ".....kS..Sk.....",
    ".....kS..Sk.....",
    "....kSS..SSk....",
    "................",
)

SCRAP_ICONS = (("scrap_helmet", SCRAP_HELMET), ("scrap_torso", SCRAP_TORSO),
               ("scrap_arm", SCRAP_ARM), ("scrap_leg", SCRAP_LEG), ("exoframe", EXOFRAME))


# Caricatore di celle (parte 3c-2): un cubo pieno, quindi cinque texture 16x16 del tutto opache. Piastra grigio ferro
# con il bordo scuro per i lati e la sommita'; sul fronte un alloggiamento, vuoto, con la cella spenta o con la cella e
# tre tacche rosse accese.
PLATE = (132, 136, 142, 255)
PLATE_LIGHT = (164, 168, 174, 255)
PLATE_DARK = (98, 102, 110, 255)
PLATE_EDGE = (56, 58, 66, 255)
HOUSING = (26, 26, 32, 255)
HOUSING_BACK = (40, 40, 48, 255)
CHARGER_RED = (224, 32, 28, 255)


def solid(px):
    """Restituisce px se ogni pixel e' opaco (alfa 255); altrimenti lancia: un cubo pieno non ha buchi."""
    for y, row in enumerate(px):
        for x, color in enumerate(row):
            if color[3] != 255:
                raise ValueError("pixel %d,%d is not opaque" % (x, y))
    return px


def charger_plate():
    """Piastra 16x16 grigio ferro: bordo scuro, luce in alto e a sinistra, ombra in basso e a destra, rivetti."""
    px = canvas(16, 16)
    rect(px, 0, 0, 15, 15, PLATE_EDGE)
    rect(px, 1, 1, 14, 14, PLATE)
    rect(px, 1, 1, 14, 1, PLATE_LIGHT)
    rect(px, 1, 1, 1, 14, PLATE_LIGHT)
    rect(px, 1, 14, 14, 14, PLATE_DARK)
    rect(px, 14, 1, 14, 14, PLATE_DARK)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        px[y][x] = PLATE_EDGE
    return px


def draw_charger_side():
    """Lato del caricatore: la piastra con una scanalatura orizzontale a meta'."""
    px = charger_plate()
    rect(px, 2, 7, 13, 7, PLATE_DARK)
    rect(px, 2, 8, 13, 8, PLATE_LIGHT)
    return solid(px)


def draw_charger_top():
    """Sommita' del caricatore: la piastra con una botola quadrata al centro."""
    px = charger_plate()
    rect(px, 4, 4, 11, 11, PLATE_EDGE)
    rect(px, 5, 5, 10, 10, PLATE_DARK)
    rect(px, 5, 5, 10, 5, PLATE)
    rect(px, 7, 7, 8, 8, PLATE_EDGE)
    return solid(px)


def charger_front(cell, lit):
    """Fronte del caricatore: la piastra con l'alloggiamento; se cell, dentro c'e' la cella grigio-rame con tre tacche,
    rosse se lit."""
    px = charger_plate()
    rect(px, 3, 2, 12, 13, PLATE_EDGE)       # cornice dell'alloggiamento
    rect(px, 4, 3, 11, 12, HOUSING)          # alloggiamento vuoto
    rect(px, 4, 3, 11, 3, HOUSING_BACK)
    if cell:
        shades = (COPPER_DARK, COPPER, COPPER_LIGHT, COPPER, COPPER_DARK, COPPER_DARK)
        for i, color in enumerate(shades):
            rect(px, 5 + i, 5, 5 + i, 10, color)
        rect(px, 5, 4, 10, 4, METAL)         # tappi metallici della cella
        rect(px, 5, 11, 10, 11, METAL_DARK)
        for y in (6, 8, 10):                 # tre tacche: spente o di redstone accesa
            rect(px, 6, y, 9, y, CHARGER_RED if lit else OUTLINE)
    return solid(px)


CHARGER_TEXTURES = (("charger_top", draw_charger_top), ("charger_side", draw_charger_side),
                    ("charger_front", lambda: charger_front(False, False)),
                    ("charger_front_cell", lambda: charger_front(True, False)),
                    ("charger_front_lit", lambda: charger_front(True, True)))


# Stazione di manutenzione (parte 4a-1), segnaposto fino alla grafica della parte 4d: un cubo pieno, quindi tre texture
# 16x16 del tutto opache, sulla piastra del caricatore. Sopra un piano di lavoro con due guide di rame, sui lati una
# fascia di rame e tre feritoie, sul fronte un pannello scuro con una spia di redstone e un cassetto di rame.
def draw_station_top():
    """Sommita' della stazione: la piastra con un piano di lavoro scuro attraversato da due guide di rame."""
    px = charger_plate()
    rect(px, 2, 2, 13, 13, PLATE_EDGE)       # bordo del piano
    rect(px, 3, 3, 12, 12, PLATE_DARK)       # piano di lavoro
    for y in (5, 10):                        # guide di rame
        rect(px, 3, y, 12, y, COPPER)
        rect(px, 3, y + 1, 12, y + 1, COPPER_DARK)
    return solid(px)


def draw_station_side():
    """Lato della stazione: la piastra con una fascia di rame in alto e tre feritoie sotto."""
    px = charger_plate()
    rect(px, 1, 3, 14, 3, COPPER_LIGHT)      # fascia di rame
    rect(px, 1, 4, 14, 4, COPPER)
    rect(px, 1, 5, 14, 5, COPPER_DARK)
    for y in (8, 10, 12):                    # feritoie
        rect(px, 4, y, 11, y, PLATE_EDGE)
    return solid(px)


def draw_station_front_main():
    """Fronte della meta' principale: la piastra con un pannello scuro, una spia di redstone accesa, due tacche di
    metallo e, sotto, un cassetto di rame."""
    px = charger_plate()
    rect(px, 3, 2, 12, 9, PLATE_EDGE)        # cornice del pannello
    rect(px, 4, 3, 11, 8, HOUSING)           # pannello
    rect(px, 4, 3, 11, 3, HOUSING_BACK)
    rect(px, 5, 5, 6, 6, CHARGER_RED)        # spia
    rect(px, 8, 5, 10, 5, METAL)             # tacche
    rect(px, 8, 7, 10, 7, METAL_DARK)
    rect(px, 3, 11, 12, 13, PLATE_EDGE)      # cassetto
    rect(px, 4, 11, 11, 12, COPPER)
    rect(px, 4, 11, 11, 11, COPPER_LIGHT)
    rect(px, 7, 12, 8, 12, COPPER_DARK)      # maniglia
    return solid(px)


STATION_TEXTURES = (("station_top", draw_station_top), ("station_side", draw_station_side),
                    ("station_front_main", draw_station_front_main))

# L'oggetto della stazione: un'icona piatta (design par. 5.1), un banco largo e basso visto di fronte, con due sportelli
# (la spia di redstone a sinistra) e sopra un telaio piccolo, lo scheletro d'acciaio con il nucleo di redstone.
STATION = (
    "................",
    "......kssk......",
    "......kSSk......",
    "....sssSSsss....",
    "....kS.RR.Sk....",
    "....kS.SS.Sk....",
    "......sSSs......",
    ".....kS..Sk.....",
    ".ssssssssssssss.",
    ".sSSSSSSSSSSSSk.",
    ".bBBBBBBBBBBBBd.",
    ".bBkkkBBBBkkkBd.",
    ".bBkRkBBBBkSkBd.",
    ".bBkkkBBBBkkkBd.",
    ".kk..........kk.",
    "................",
)


def outputs():
    """I file che il generatore scrive: (percorso nei sorgenti, larghezza, altezza, funzione che disegna)."""
    files = [(CELL_PATH, 16, 16, draw_cell), (ICON_PATH, 64, 64, draw_icon)]
    for name, draw in PIECE_ICONS:
        files.append((os.path.join(ITEM_DIR, name + ".png"), 16, 16, draw))
    for name, rows in SCRAP_ICONS:
        files.append((os.path.join(ITEM_DIR, name + ".png"), 16, 16, lambda rows=rows: draw_sprite(rows)))
    for name, draw in CHARGER_TEXTURES:
        files.append((os.path.join(BLOCK_DIR, name + ".png"), 16, 16, draw))
    for name, draw in STATION_TEXTURES:
        files.append((os.path.join(BLOCK_DIR, name + ".png"), 16, 16, draw))
    files.append((os.path.join(ITEM_DIR, "station.png"), 16, 16, lambda: draw_sprite(STATION)))
    for path in ARMOR_LAYER_PATHS:
        files.append((path, 64, 32, lambda: canvas(64, 32)))
    return files


def main():
    ap = argparse.ArgumentParser(description="Genera le texture placeholder.")
    ap.add_argument("--out", default=None, help="cartella al posto di src/main (stessi percorsi relativi)")
    args = ap.parse_args()
    written = []
    for path, width, height, draw in outputs():
        if args.out is not None:
            path = os.path.join(args.out, os.path.relpath(path, MAIN))
        write_png(path, width, height, draw())
        written.append(path)
    print("scritti:", *(os.path.normpath(p) for p in written))


if __name__ == "__main__":
    main()
