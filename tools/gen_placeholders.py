#!/usr/bin/env python3
"""Genera in modo deterministico le texture placeholder (solo stdlib).

Uso, dalla radice del repository: python3 -I tools/gen_placeholders.py
Disegni originali: nessun riferimento a opere o marchi di terzi.
"""
import os
import struct
import zlib

MAIN = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main")
ROOT = os.path.join(MAIN, "resources")
ITEM_DIR = os.path.join(ROOT, "assets", "redstone_exoframes", "textures", "item")
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


def main():
    written = [CELL_PATH, ICON_PATH]
    write_png(CELL_PATH, 16, 16, draw_cell())
    write_png(ICON_PATH, 64, 64, draw_icon())
    for name, draw in PIECE_ICONS:
        path = os.path.join(ITEM_DIR, name + ".png")
        write_png(path, 16, 16, draw())
        written.append(path)
    for name, rows in SCRAP_ICONS:
        path = os.path.join(ITEM_DIR, name + ".png")
        write_png(path, 16, 16, draw_sprite(rows))
        written.append(path)
    for path in ARMOR_LAYER_PATHS:
        write_png(path, 64, 32, canvas(64, 32))
        written.append(path)
    print("scritti:", *(os.path.normpath(p) for p in written))


if __name__ == "__main__":
    main()
