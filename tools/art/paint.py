"""Pittura procedurale a pixel dell'atlante, aspetto metallico (DENSITY texel per pixel di modello).

Ogni texel conosce la sua posizione 3D (face_frame), quindi rumore, occlusione, calore e grasso proseguono senza salti
da una faccia all'altra e da una scatola all'altra. Deterministica: ogni faccia ha il suo generatore (HashRng), con un
seme ricavato dal nome della scatola e dalla faccia, quindi aggiungere scatole non cambia il rumore delle altre (solo
occlusione, calore e grasso guardano le scatole vicine).

Materiali (stringa per tutta la scatola o dizionario {faccia: materiale, 'default': materiale}):
  gun      acciaio brunito scuro della struttura (spazzolato, spigoli chiari, bulloni sulle facce grandi)
  steel    acciaio chiaro delle parti lavorate (spazzolato, bulloni, graffi)
  brass    ottone; con hub=True mozzo con anelli e bullone cromato al centro (sulle facce laterali)
  chrome   steli cromati con riflesso a strisce
  cable    fascio di cavi di rame ad anelli
  rubber   tubo di gomma ad anelli
  vent     griglia a lamelle scure, con tinta di calore attorno
  cell     cella di redstone (rossa, luminosa)
  lamp     spia rossa luminosa
  paint    lamiera verniciata in grigio chiaro (il renderer la colora: ossa con paint 'p1' o 'p2')
  pipe     tubo di rame
  glass    vetro scuro con riflesso (luminoso)
Opzioni della scatola (parole chiave di Model.box): hub (bool), heat (bool: sorgente di tinta di calore oltre alle
griglie), bolts (bool, predefinito True), hide (facce da non esportare, vedi exo.Model.pack).
Luce dipinta: dall'alto, dal davanti e dal lato destro di chi indossa (x negativa del file).
I materiali cell, lamp e glass finiscono anche nella maschera luminosa (vedi paint_model).
Lo stato visivo dell'osso (Bone.state 1-3: piu' usura e ruggine) non e' ancora dipinto: arriva con i pezzi.
"""
import zlib

import numpy as np

from exo import ATLAS_SIZE, DENSITY, face_frame

PAL = {
    'gun': (78, 82, 92), 'steel': (146, 152, 160), 'brass': (190, 150, 70), 'chrome': (196, 204, 212),
    'copper': (186, 104, 56), 'rubber': (40, 38, 40), 'red_dim': (120, 14, 10), 'red': (214, 36, 24),
    'red_hot': (255, 120, 80), 'black': (20, 20, 24), 'grease': (34, 28, 22), 'paint': (214, 214, 210),
}
LIGHT = np.array([-0.5, 1.0, -0.7]) / np.linalg.norm([-0.5, 1.0, -0.7])
HEAT = [np.array(c, float) for c in ((214, 182, 104), (186, 120, 66), (128, 76, 132), (72, 98, 168))]


class HashRng:
    """Sequenza pseudo-casuale fatta solo di aritmetica intera (splitmix64 su seme + contatore): non dipende dal
    generatore di numpy, il cui flusso puo' cambiare tra una versione e l'altra. Su intervallo o elenco vuoti
    integers() e choice() sollevano ValueError."""

    MASK = (1 << 64) - 1

    def __init__(self, seed):
        self.seed, self.n = seed & self.MASK, 0

    def _next(self):
        self.n += 1
        z = (self.seed + self.n * 0x9E3779B97F4A7C15) & self.MASK
        z = ((z ^ (z >> 30)) * 0xBF58476D1CE4E5B9) & self.MASK
        z = ((z ^ (z >> 27)) * 0x94D049BB133111EB) & self.MASK
        return z ^ (z >> 31)

    def integers(self, lo, hi):
        """Intero in [lo, hi)."""
        if hi <= lo:
            raise ValueError(f'HashRng.integers: empty range [{lo}, {hi})')
        return lo + self._next() % (hi - lo)

    def choice(self, items):
        if not items:
            raise ValueError('HashRng.choice: empty list')
        return items[self._next() % len(items)]

    def random(self):
        """Reale in [0, 1)."""
        return (self._next() >> 11) / float(1 << 53)


def _hash(ix, iy, iz, seed):
    h = (ix.astype(np.int64) * 374761393 + iy.astype(np.int64) * 668265263
         + iz.astype(np.int64) * 1274126177 + seed * 144665) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    h = h ^ (h >> 16)
    return (h & 0xFFFF) / 65535.0


def vnoise(P, scale, seed):
    """Rumore a valori 3D, continuo nello spazio."""
    q = P / scale
    f = np.floor(q)
    t = q - f
    t = t * t * (3 - 2 * t)
    i = f.astype(np.int64)
    out = 0
    for dx in (0, 1):
        for dy in (0, 1):
            for dz in (0, 1):
                w = ((t[..., 0] if dx else 1 - t[..., 0]) * (t[..., 1] if dy else 1 - t[..., 1])
                     * (t[..., 2] if dz else 1 - t[..., 2]))
                out = out + w * _hash(i[..., 0] + dx, i[..., 1] + dy, i[..., 2] + dz, seed)
    return out


def white(P, seed):
    i = np.floor(P * DENSITY + 0.001).astype(np.int64)
    return _hash(i[..., 0], i[..., 1], i[..., 2], seed)


def quant(m, step=0.06):
    return np.round(m / step) * step


def box_dist(P, lo, hi):
    """Distanza euclidea dei punti P dalla scatola (0 dentro)."""
    d = np.maximum(np.maximum(np.array(lo) - P, P - np.array(hi)), 0)
    return np.sqrt((d * d).sum(-1))


class Face:
    def __init__(self, model, box, face, rect):
        self.box, self.face = box, face
        self.u, self.v, self.w, self.h = rect
        tl, ua, va, fw, fh, n = face_frame(face, box.lo, box.hi)
        jj, ii = np.mgrid[0:self.h, 0:self.w]
        self.i, self.j = ii, jj
        tl, ua, va, n = (np.array(x, float) for x in (tl, ua, va, n))
        self.ua, self.va, self.n = ua, va, n
        su, sv = fw / self.w, fh / self.h
        self.S = tl + ((ii[..., None] + 0.5) * su) * ua + ((jj[..., None] + 0.5) * sv) * va   # sulla superficie
        self.P = self.S - 0.25 * n
        self.seed = (box.seed + zlib.crc32(face.encode())) & 0x7FFFFFFF
        self.rng = HashRng(self.seed)
        self.vertical = face not in ('up', 'down')
        self.long_axis = int(np.argmax(box.size))
        # vicini: scatole che possono essere visibili insieme a questa, entro 3 px
        self.near = [c for c in model.boxes if c is not box and model._together(c, box)
                     and all(c.lo[k] - 3 < box.hi[k] and c.hi[k] + 3 > box.lo[k] for k in range(3))]
        self.model = model

    def edges(self):
        """Maschere dei quattro bordi (sinistra, destra, alto, basso) con la direzione 3D verso cui guarda ognuno."""
        w, h = self.w, self.h
        return [(self.i == 0, -self.ua), (self.i == w - 1, self.ua),
                (self.j == 0, -self.va), (self.j == h - 1, self.va)]

    def occlusion(self):
        """0 (libero) .. 1 (chiuso): campioni nell'emisfero davanti alla faccia che cadono dentro altre scatole."""
        if not self.near:
            return np.zeros((self.h, self.w))
        n, ua, va = self.n, self.ua, self.va
        dirs = [n, n + ua, n - ua, n + va, n - va, n + 0.7 * np.array([0, 1, 0])]
        occ = np.zeros((self.h, self.w))
        tot = 0
        for r in (0.4, 0.9, 1.8):
            for d in dirs:
                d = d / np.linalg.norm(d)
                Q = self.S + 0.05 * n + r * d
                hit = np.zeros((self.h, self.w), bool)
                for c in self.near:
                    hit |= np.all((Q > np.array(c.lo)) & (Q < np.array(c.hi)), -1)
                wgt = 1.5 if r < 0.5 else 1.0
                occ += hit * wgt
                tot += wgt
        return occ / tot

    def dist_to(self, pred):
        d = np.full((self.h, self.w), 99.0)
        for c in self.model.boxes:
            if c is not self.box and pred(c):
                d = np.minimum(d, box_dist(self.S, c.lo, c.hi))
        return d


def brushed(F, base, amp):
    """Gradiente verticale e spazzolatura: strisce lungo l'asse verticale delle facce laterali, lungo z sopra."""
    key = F.ua if F.vertical else np.array([1.0, 0, 0])
    col_id = np.floor((F.S @ np.abs(key)) * DENSITY + 0.001)
    streak = _hash(col_id.astype(np.int64), np.zeros_like(col_id, np.int64), np.zeros_like(col_id, np.int64), F.seed)
    slow = vnoise(F.P * np.array([1, 0.25, 1]) if F.vertical else F.P, 2.5, F.seed + 3)
    m = 1 + amp * (0.10 * (streak - 0.5) + 0.12 * (slow - 0.5))
    if F.vertical:
        m *= 1.08 - 0.18 * (F.j + 0.5) / F.h
    else:
        m *= 1.10 if F.face == 'up' else 0.78
    return quant(m)[..., None] * np.array(base, float)[None, None, :]


def bolt(col, x, y, base):
    h, w, _ = col.shape
    if 0 <= x < w - 1 and 0 <= y < h - 1:
        b = np.array(base, float)
        col[y, x] = np.minimum(b * 1.4 + 12, 255)
        col[y, x + 1] = b * 0.8
        col[y + 1, x] = b * 0.8
        col[y + 1, x + 1] = b * 0.5


def metal(F, base, amp=1.0, bolts=True, scratches=True):
    col = brushed(F, base, amp)
    w, h, rng = F.w, F.h, F.rng
    # spigoli: chiaro verso la luce, scuro dalla parte opposta
    if w >= 3 and h >= 3:
        for mask, d in F.edges():
            lit = float(d @ LIGHT)
            if lit > 0.25:
                col[mask] = np.minimum(np.array(base, float) * (1.45 + 0.3 * lit) + 18, 255)
            elif lit < -0.25:
                col[mask] *= 0.62
    # graffi lucidi paralleli ai bordi, piu' fitti vicino agli spigoli
    if scratches and w * h >= 24:
        for _ in range(int(rng.integers(0, 2 + w * h // 140))):
            horiz = rng.random() < 0.5
            L = int(rng.integers(2, 6))
            if horiz and w > L + 2:
                x = int(rng.integers(1, w - L - 1))
                y = int(rng.choice([1, 2, h - 3, int(rng.integers(1, max(2, h - 1)))]))
                if 0 < y < h - 1:
                    col[y, x:x + L] = np.minimum(np.array(base) * 1.22 + 10, 255)
            elif not horiz and h > L + 2:
                y = int(rng.integers(1, h - L - 1))
                x = int(rng.choice([1, 2, w - 3, int(rng.integers(1, max(2, w - 1)))]))
                if 0 < x < w - 1:
                    col[y:y + L, x] = np.minimum(np.array(base) * 1.22 + 10, 255)
        # scheggiature scure
        chips = white(F.P, F.seed + 7) > 0.985
        col[chips] *= 0.55
    # bulloni: agli angoli e lungo i lati lunghi delle facce grandi
    if bolts and w >= 7 and h >= 7:
        pts = [(2, 2), (w - 4, 2), (2, h - 4), (w - 4, h - 4)]
        if w >= 16:
            pts += [(x, 2) for x in range(8, w - 7, 6)] + [(x, h - 4) for x in range(8, w - 7, 6)]
        if h >= 16:
            pts += [(2, y) for y in range(8, h - 7, 6)] + [(w - 4, y) for y in range(8, h - 7, 6)]
        for x, y in pts:
            bolt(col, x, y, base)
    return col


def hub(F, col, base):
    """Mozzo: anelli concentrici e bullone cromato al centro (sulle facce piu' o meno quadrate)."""
    w, h = F.w, F.h
    if w < 4 or h < 4:
        return col
    cx, cy = (w - 1) / 2, (h - 1) / 2
    r = np.maximum(np.abs(F.i - cx), np.abs(F.j - cy))
    R = min(w, h) / 2
    b = np.array(base, float)
    col[(r > R - 1.6) & (r <= R - 0.6)] = b * 0.62
    ring = (r > R - 2.8) & (r <= R - 1.6)
    col[ring] = np.minimum(b * 1.22, 255)
    core = r <= 1
    col[core] = np.array(PAL['chrome'], float) * 0.85
    col[core & (F.i <= cx) & (F.j <= cy)] = (240, 244, 248)
    col[core & (F.i > cx) & (F.j > cy)] = np.array(PAL['chrome']) * 0.45
    return col


def ribbed(F, base, period, dark, shine):
    rib = (np.floor(F.S[..., F.long_axis] * DENSITY + 0.001).astype(int) % period) == 0
    col = np.ones((F.h, F.w, 3)) * np.array(base, float)
    col *= (0.92 + 0.12 * white(F.P, F.seed))[..., None]
    if F.vertical and F.w >= 2 and F.long_axis == 1:
        col[:, 0] *= shine
        col[:, -1] *= 0.7
    elif F.h >= 2 and F.long_axis != 1:
        col[0] *= shine
        col[-1] *= 0.7
    col[rib] *= dark
    return col


def paint_face(F, mat):
    o = F.box.opts
    h, w = F.h, F.w
    glow = False
    if mat == 'gun':
        col = metal(F, PAL['gun'], amp=1.2, bolts=o.get('bolts', True))
    elif mat == 'steel':
        col = metal(F, PAL['steel'], amp=1.0, bolts=o.get('bolts', True))
    elif mat == 'brass':
        col = metal(F, PAL['brass'], amp=0.8, bolts=False, scratches=not o.get('hub'))
        if o.get('hub') and F.n @ np.array([1.0, 0, 0]) != 0:
            col = hub(F, col, PAL['brass'])
    elif mat == 'chrome':
        col = np.ones((h, w, 3)) * np.array(PAL['chrome'], float)
        if F.vertical and F.long_axis == 1 and w >= 2:
            col[:, 0] *= 1.18
            if w >= 3:
                col[:, 1] = (250, 252, 255)
            col[:, -1] *= 0.55
        col *= (0.95 + 0.07 * white(F.P, F.seed))[..., None]
    elif mat == 'cable':
        col = ribbed(F, PAL['copper'], 3, 0.6, 1.35)
    elif mat == 'rubber':
        col = ribbed(F, PAL['rubber'], 2, 0.7, 1.6)
    elif mat == 'vent':
        col = metal(F, PAL['gun'], bolts=False, scratches=False)
        inner = (F.i > 0) & (F.i < w - 1) & (F.j > 0) & (F.j < h - 1)
        slat = (F.j % 2 == 1) if (F.vertical or w >= h) else (F.i % 2 == 1)
        col[slat & inner] = PAL['black']
        col[~slat & inner] *= 1.15
        if F.face == 'up':
            col[inner] = PAL['black']
            col[inner & (white(F.P, F.seed) > 0.75)] = (150, 40, 20)
    elif mat == 'paint':
        # lamiera verniciata in grigio chiaro (il renderer la colora): bulloni, colature di ruggine sotto i
        # rivetti, scheggiature scure dove la vernice e' saltata
        col = metal(F, PAL['paint'], amp=0.9, bolts=True)
        if w >= 7 and h >= 7 and F.vertical:
            for x in (2, w - 4):
                for y in range(4, min(h - 1, 4 + 1 + int(F.rng.integers(1, 4)))):
                    col[y, x] *= 0.72
        chip = (vnoise(F.P, 1.4, F.seed + 21) > 0.87) | ((white(F.P, F.seed + 3) > 0.93) & (F.i % (w - 1 or 1) == 0))
        col[chip] = col[chip] * 0.6
    elif mat == 'pipe':
        col = ribbed(F, PAL['copper'], 4, 0.62, 1.4)
        if F.vertical and F.long_axis == 1 and w >= 2:
            col[:, min(1, w - 1)] = np.minimum(np.array(PAL['copper']) * 1.45, 255)
    elif mat == 'glass':
        glow = True
        col = np.ones((h, w, 3)) * np.array((24, 44, 52), float)
        col[(F.i + F.j == 2) | (F.i + F.j == 3)] = (90, 140, 150)
        col[(F.i == 1) & (F.j == 1)] = (190, 230, 230)
    elif mat == 'cell':
        glow = True
        col = np.ones((h, w, 3)) * np.array(PAL['red'], float)
        n = vnoise(F.P, 1.2, F.seed)
        col[n > 0.62] = PAL['red_hot']
        col[n < 0.32] = PAL['red_dim']
        if F.vertical and w >= 5:
            for x in range(2, w - 2, 3):
                col[1:-1, x] = np.maximum(col[1:-1, x], PAL['red_hot'])
        if w >= 3 and h >= 3:
            for mask, _ in F.edges():
                col[mask] = PAL['red_dim']
    elif mat == 'lamp':
        glow = True
        col = np.ones((h, w, 3)) * np.array(PAL['red'], float)
        if w >= 2 and h >= 2:
            col[1:, 1:] = PAL['red_hot']
        if w >= 3 and h >= 2:
            col[0, 1] = (255, 214, 190)
    else:
        raise ValueError(f'materiale sconosciuto {mat}')

    if not glow:
        # occlusione: linea scura nelle rientranze e sotto gli sporgenti
        occ = F.occlusion()
        col *= (1 - 0.75 * np.clip(occ * 1.6, 0, 1))[..., None]
        if mat in ('gun', 'steel', 'vent'):
            # tinta di calore vicino a griglie e camini
            d = F.dist_to(lambda c: c.mat_for('north') == 'vent' or c.opts.get('heat'))
            if F.box.opts.get('heat'):
                d = np.minimum(d, (F.box.hi[1] - F.S[..., 1]) * 0.8)
            tn = d + 0.6 * (vnoise(F.P, 1.5, F.seed + 11) - 0.5)
            for k, c in enumerate(HEAT):
                band = (tn >= k * 0.3) & (tn < (k + 1) * 0.3)
                col[band] = col[band] * 0.72 + c * 0.28 * (col[band].mean(-1, keepdims=True) / 120)
            # grasso vicino ai mozzi dei giunti
            dg = F.dist_to(lambda c: c.opts.get('hub'))
            g = (dg < 1.6) & (vnoise(F.P * np.array([1.5, 0.6, 1.5]), 1.0, F.seed + 13) > 0.58)
            col[g] = col[g] * 0.35 + np.array(PAL['grease']) * 0.65
    return col, glow


def paint_model(model, debug_position=False):
    """Restituisce (atlante RGBA, maschera di luminosita' RGBA) come array (altezza, larghezza, 4) uint8."""
    W, H = ATLAS_SIZE
    atlas = np.zeros((H, W, 4), np.uint8)
    glowmask = np.zeros((H, W, 4), np.uint8)
    for b in model.boxes:
        if b.mirror_of is not None:
            continue
        for f, rect in b.faces.items():
            F = Face(model, b, f, rect)
            if debug_position:
                q = F.S
                col = np.stack([(q[..., 0] + 14) / 28, q[..., 1] / 37, (q[..., 2] + 9) / 19], -1) * 230 + 15
                col[(F.i == 0) & (F.j == 0)] = (255, 255, 255)
                glow = False
            else:
                col, glow = paint_face(F, b.mat_for(f))
            sl = (slice(F.v, F.v + F.h), slice(F.u, F.u + F.w))
            atlas[sl][..., :3] = np.clip(col, 0, 255).astype(np.uint8)
            atlas[sl][..., 3] = 255
            if glow:
                glowmask[sl] = atlas[sl]
    return atlas, glowmask
