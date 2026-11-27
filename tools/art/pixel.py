"""Pittura a pixel delle texture della stazione (16 texel per lato di blocco), a palette: ogni texel ha un materiale e
un livello della sua scala di colori (0 il piu' scuro), cosi' luci, ombre e rumore si muovono di gradino in gradino,
senza sfumature morbide, come nelle texture del gioco base. Solo aritmetica intera; il rumore viene da
`paint.HashRng` con un seme ricavato dal nome della tela, quindi stesse chiamate, stessi texel.

Rettangoli: (c0, r0, c1, r1), colonne e righe della tela, estremi esclusi.
"""
import zlib

import numpy as np

from paint import HashRng

# scale di colore, dal piu' scuro al piu' chiaro
RAMPS = {
    # acciaio scuro, canna di fucile (il secondario del Rottame, 0x4A4F55, sta fra 3 e 4)
    'gun': ((20, 22, 26), (30, 33, 38), (41, 45, 51), (53, 58, 65), (66, 71, 79), (81, 87, 96), (99, 106, 115),
            (124, 131, 140)),
    # acciaio medio delle parti lavorate
    'steel': ((40, 44, 50), (58, 63, 70), (78, 84, 92), (100, 107, 115), (124, 131, 139), (148, 155, 163),
              (172, 178, 185), (200, 205, 211)),
    # ferro chiaro (lingotti e blocco di ferro del gioco base: grigio neutro)
    'iron': ((60, 62, 66), (84, 87, 92), (110, 113, 119), (138, 141, 147), (165, 168, 173), (190, 193, 197),
             (212, 214, 217), (234, 235, 237)),
    # bronzo (vicino al primario del Rottame, 0x8A6F52, al livello 4; luci piu' calde e piu' contrasto, da metallo)
    'bronze': ((40, 29, 19), (62, 45, 29), (88, 65, 42), (114, 86, 56), (142, 110, 74), (170, 134, 90),
               (198, 160, 110), (228, 196, 144)),
    'copper': ((66, 30, 18), (94, 44, 26), (124, 60, 34), (154, 78, 44), (184, 100, 56), (206, 124, 72),
               (226, 152, 98), (242, 186, 136)),
    'brass': ((64, 46, 18), (94, 70, 28), (126, 96, 40), (156, 122, 54), (186, 148, 68), (210, 176, 94),
              (230, 204, 128), (246, 230, 172)),
    'chrome': ((70, 76, 86), (100, 108, 120), (134, 142, 154), (164, 172, 182), (192, 198, 206), (214, 219, 225),
               (232, 236, 240), (250, 252, 253)),
    # rosso della redstone: solo pittura, il blocco non fa luce
    'red': ((40, 4, 3), (72, 8, 6), (108, 12, 8), (146, 18, 12), (186, 26, 16), (220, 44, 28), (246, 82, 56),
            (255, 140, 110)),
    'oak': ((56, 38, 20), (78, 54, 30), (102, 72, 40), (126, 92, 52), (152, 114, 66), (176, 136, 82),
            (198, 160, 102), (220, 186, 128)),
    # patina del rame
    'patina': ((30, 72, 62), (44, 98, 84), (62, 124, 106), (84, 150, 128), (110, 176, 152), (140, 198, 176)),
    'rubber': ((22, 22, 24), (32, 32, 35), (44, 44, 47), (58, 58, 62), (74, 74, 78)),
    'black': ((10, 11, 13), (16, 17, 20), (24, 25, 29)),
}
MATS = tuple(RAMPS)
MID = {name: len(ramp) // 2 for name, ramp in RAMPS.items()}


def seed_of(*parts):
    return zlib.crc32('/'.join(str(p) for p in parts).encode('utf-8'))


class Canvas:
    def __init__(self, h, w, name, mat='gun', lev=None):
        self.h, self.w, self.name = h, w, name
        self.mat = np.full((h, w), MATS.index(mat), int)
        self.lev = np.full((h, w), MID[mat] if lev is None else lev, int)

    # --- primitive ---------------------------------------------------------------------------------------------
    def _clip(self, rect):
        c0, r0, c1, r1 = rect
        return max(c0, 0), max(r0, 0), min(c1, self.w), min(r1, self.h)

    def fill(self, rect, mat, lev=None):
        c0, r0, c1, r1 = self._clip(rect)
        self.mat[r0:r1, c0:c1] = MATS.index(mat)
        self.lev[r0:r1, c0:c1] = MID[mat] if lev is None else lev
        return self

    def px(self, c, r, mat, lev=None):
        if 0 <= c < self.w and 0 <= r < self.h:
            self.mat[r, c] = MATS.index(mat)
            self.lev[r, c] = MID[mat] if lev is None else lev
        return self

    def add(self, rect, dl):
        """Sposta il livello di `dl` gradini nel rettangolo (dentro la scala di ciascun materiale)."""
        c0, r0, c1, r1 = self._clip(rect)
        self.lev[r0:r1, c0:c1] += dl
        return self

    def add_px(self, c, r, dl):
        if 0 <= c < self.w and 0 <= r < self.h:
            self.lev[r, c] += dl
        return self

    def hline(self, r, c0, c1, mat, lev=None):
        return self.fill((c0, r, c1, r + 1), mat, lev)

    def vline(self, c, r0, r1, mat, lev=None):
        return self.fill((c, r0, c + 1, r1), mat, lev)

    def border(self, rect, mat, lev=None):
        c0, r0, c1, r1 = rect
        self.hline(r0, c0, c1, mat, lev)
        self.hline(r1 - 1, c0, c1, mat, lev)
        self.vline(c0, r0, r1, mat, lev)
        self.vline(c1 - 1, r0, r1, mat, lev)
        return self

    def bevel(self, rect, hi=1, lo=-1):
        """Bordo di una parte rialzata, luce dall'alto a sinistra: riga alta e colonna sinistra +hi, riga bassa e
        colonna destra +lo."""
        c0, r0, c1, r1 = rect
        self.add((c0, r0, c1, r0 + 1), hi)
        self.add((c0, r0 + 1, c0 + 1, r1), hi)
        self.add((c0 + 1, r1 - 1, c1, r1), lo)
        self.add((c1 - 1, r0 + 1, c1, r1 - 1), lo)
        return self

    def rivet(self, c, r, mat=None):
        """Testa di rivetto: un texel chiaro, un'ombra sotto a destra."""
        m = mat if mat is not None else MATS[self.mat[min(r, self.h - 1), min(c, self.w - 1)]]
        top = len(RAMPS[m]) - 1
        self.px(c, r, m, top - 1)
        if 0 <= c + 1 < self.w and 0 <= r + 1 < self.h:
            self.lev[r + 1, c + 1] -= 2
        return self

    def bolt(self, c, r, mat=None):
        """Bullone 2x2: chiaro in alto a sinistra, scuro in basso a destra."""
        m = mat if mat is not None else MATS[self.mat[r, c]]
        top = len(RAMPS[m]) - 1
        self.px(c, r, m, top - 1).px(c + 1, r, m, top - 2).px(c, r + 1, m, top - 3).px(c + 1, r + 1, m, 1)
        return self

    # --- rumore ------------------------------------------------------------------------------------------------
    def noise(self, rect, key, chance=4, down_only=False):
        """Un texel su `chance` sale o scende di un gradino (con down_only solo scende)."""
        rng = HashRng(seed_of(self.name, 'noise', key))
        c0, r0, c1, r1 = self._clip(rect)
        for r in range(r0, r1):
            for c in range(c0, c1):
                roll = rng.integers(0, chance * 2)
                if roll == 0:
                    self.lev[r, c] -= 1
                elif roll == 1 and not down_only:
                    self.lev[r, c] += 1
        return self

    def streaks(self, rect, key, every=3, length=(2, 6), dl=1):
        """Spazzolatura: tratti orizzontali di `length` texel, uno ogni `every` righe circa, +dl o -dl."""
        rng = HashRng(seed_of(self.name, 'streaks', key))
        c0, r0, c1, r1 = self._clip(rect)
        for r in range(r0, r1):
            if rng.integers(0, every) != 0:
                continue
            n = rng.integers(length[0], length[1] + 1)
            if c1 - c0 <= n:
                start = c0
            else:
                start = rng.integers(c0, c1 - n + 1)
            sign = dl if rng.integers(0, 2) else -dl
            self.add((start, r, min(start + n, c1), r + 1), sign)
        return self

    def vstreaks(self, rect, key, every=3, length=(2, 5), dl=1):
        """Come streaks, in verticale (venature, colature)."""
        rng = HashRng(seed_of(self.name, 'vstreaks', key))
        c0, r0, c1, r1 = self._clip(rect)
        for c in range(c0, c1):
            if rng.integers(0, every) != 0:
                continue
            n = rng.integers(length[0], length[1] + 1)
            start = r0 if r1 - r0 <= n else rng.integers(r0, r1 - n + 1)
            sign = dl if rng.integers(0, 2) else -dl
            self.add((c, start, c + 1, min(start + n, r1)), sign)
        return self

    def speckle(self, rect, key, mat, lev, chance=8):
        """Texel sparsi di un altro materiale (patina, polvere di redstone)."""
        rng = HashRng(seed_of(self.name, 'speckle', key))
        c0, r0, c1, r1 = self._clip(rect)
        for r in range(r0, r1):
            for c in range(c0, c1):
                if rng.integers(0, chance) == 0:
                    self.px(c, r, mat, lev if isinstance(lev, int) else rng.choice(lev))
        return self

    # --- rilievo -----------------------------------------------------------------------------------------------
    def relief(self, depth, strength=1):
        """Luci e ombre delle tasche dalla mappa delle profondita' della faccia (bench.Bench.depth_map), luce
        dall'alto a sinistra: un texel sotto o a destra di una parte piu' alta e' in ombra; il bordo alto e
        sinistro di una parte piu' alta prende luce, quello basso e destro ombra."""
        d = np.asarray(depth)
        h, w = d.shape
        dl = np.zeros((h, w), int)
        for r in range(h):
            for c in range(w):
                k = d[r, c]
                up = d[r - 1, c] if r > 0 else k
                left = d[r, c - 1] if c > 0 else k
                down = d[r + 1, c] if r + 1 < h else k
                right = d[r, c + 1] if c + 1 < w else k
                if up < k or left < k:
                    dl[r, c] -= strength + (1 if up < k and left < k else 0)
                if up > k or left > k:
                    dl[r, c] += strength
                if down > k or right > k:
                    dl[r, c] -= strength
        self.lev += dl
        return self

    # --- uscita ------------------------------------------------------------------------------------------------
    def rgba(self):
        out = np.zeros((self.h, self.w, 4), np.uint8)
        for i, name in enumerate(MATS):
            ramp = np.array(RAMPS[name], np.uint8)
            sel = self.mat == i
            lev = np.clip(self.lev[sel], 0, len(ramp) - 1)
            out[sel, :3] = ramp[lev]
        out[..., 3] = 255
        return out


def sprite(rows, legend):
    """Icona da una griglia di caratteri: `legend` {carattere: (materiale, livello)}, '.' trasparente."""
    h, w = len(rows), len(rows[0])
    if any(len(r) != w for r in rows):
        raise ValueError('righe dell\'icona di lunghezza diversa')
    out = np.zeros((h, w, 4), np.uint8)
    for r, row in enumerate(rows):
        for c, ch in enumerate(row):
            if ch == '.':
                continue
            mat, lev = legend[ch]
            ramp = RAMPS[mat]
            out[r, c, :3] = ramp[max(0, min(lev, len(ramp) - 1))]
            out[r, c, 3] = 255
    return out
