#!/usr/bin/env python3
"""Genera in modo deterministico le texture placeholder (solo stdlib).

Uso, dalla radice del repository: python3 -I tools/gen_placeholders.py
Disegni originali: nessun riferimento a opere o marchi di terzi.
"""
import os
import struct
import zlib

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
CELL_PATH = os.path.join(ROOT, "assets", "redstone_exoframes", "textures", "item", "redstone_cell.png")
ICON_PATH = os.path.join(ROOT, "icon.png")

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


def main():
    write_png(CELL_PATH, 16, 16, draw_cell())
    write_png(ICON_PATH, 64, 64, draw_icon())
    print("scritti:", os.path.normpath(CELL_PATH), os.path.normpath(ICON_PATH))


if __name__ == "__main__":
    main()
