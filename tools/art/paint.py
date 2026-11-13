"""Pittura procedurale a pixel dell'atlante.

Ogni texel conosce la sua posizione 3D (face_frame), quindi rumore, ruggine e patina proseguono senza salti da una
faccia all'altra e da una scatola all'altra. Deterministica: ogni faccia ha il suo generatore, con un seme ricavato
dal nome della scatola e dalla faccia, quindi aggiungere scatole non cambia i pixel di quelle esistenti.

Materiali (stringa per tutta la scatola o dizionario {faccia: materiale, 'default': materiale}):
  frame    acciaio scuro dei montanti, con fori di bullone sui montanti lunghi
  steel    acciaio piu' chiaro per piastre e staffe, con rivetti
  joint    mozzo d'ottone dei giunti con bullone centrale
  brass    ottone (cilindri)
  chrome   steli cromati
  copper   rame con patina
  rubber   tubo di gomma a anelli
  cable    cavo di rame ad anelli
  wire     filo di redstone (rosso, acceso)
  cell     cella di redstone (rossa, con bordo scuro)
  lamp     spia rossa
  grill    griglia scura
  paint    lamiera verniciata in toni di grigio (il renderer la colora: ossa con paint 'p1' o 'p2')
Opzioni della scatola (parole chiave di Model.box): rust (0..1), rivets (bool), seam (bool), patina (0..1),
hide (facce da non esportare).
Lo stato visivo dell'osso (Bone.state 0..3) aumenta usura, ruggine e scheggiature.
"""
import zlib

import numpy as np

from exo import ATLAS_SIZE, face_frame

PAL = {
    'frame': (104, 108, 118), 'steel': (122, 126, 134), 'brass': (176, 142, 70), 'copper': (178, 102, 56),
    'chrome': (182, 188, 194), 'rubber': (44, 40, 42), 'rust': (136, 72, 40), 'rust_dark': (96, 52, 32),
    'patina': (86, 148, 124), 'patina_dark': (60, 116, 100), 'paint': (200, 200, 200),
    'red_dim': (110, 12, 10), 'red': (204, 32, 24), 'red_hot': (255, 108, 72), 'black': (28, 26, 30),
    'hole': (58, 60, 68), 'brass_dark': (120, 92, 40), 'bolt': (66, 60, 52),
}


def _hash(ix, iy, iz, seed):
    h = (ix.astype(np.int64) * 374761393 + iy.astype(np.int64) * 668265263
         + iz.astype(np.int64) * 1274126177 + seed * 144665) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    h = h ^ (h >> 16)
    return (h & 0xFFFF) / 65535.0


class HashRng:
    """Sequenza pseudo-casuale fatta solo di aritmetica intera (splitmix64 su seme + contatore): non dipende dal
    generatore di numpy, il cui flusso puo' cambiare tra una versione e l'altra."""

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
        return lo + self._next() % (hi - lo)

    def choice(self, items):
        return items[self._next() % len(items)]

    def random(self):
        """Reale in [0, 1)."""
        return (self._next() >> 11) / float(1 << 53)


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
    i = np.floor(P + 0.001).astype(np.int64)
    return _hash(i[..., 0], i[..., 1], i[..., 2], seed)


def quant(m, steps=(0.78, 0.88, 0.96, 1.04, 1.12)):
    """Pochi livelli di luminosita': niente sfumature morbide."""
    s = np.array(steps)
    idx = np.clip(np.round((m - s[0]) / (s[1] - s[0])), 0, len(s) - 1).astype(int)
    return s[idx]


class Face:
    def __init__(self, box, face, rect):
        self.box, self.face = box, face
        self.u, self.v, self.w, self.h = rect
        tl, ua, va, fw, fh, n = face_frame(face, box.lo, box.hi)
        jj, ii = np.mgrid[0:self.h, 0:self.w]
        self.i, self.j = ii, jj
        tl, ua, va, n = (np.array(x, float) for x in (tl, ua, va, n))
        su, sv = fw / self.w, fh / self.h
        # centro del texel, spostato un poco verso l'interno perche' l'arrotondamento a intero sia stabile
        self.P = tl + ((ii[..., None] + 0.5) * su) * ua + ((jj[..., None] + 0.5) * sv) * va - 0.25 * n
        self.seed = (box.seed + zlib.crc32(face.encode())) & 0x7FFFFFFF
        self.rng = HashRng(self.seed)
        self.edge = (ii == 0) | (jj == 0) | (ii == self.w - 1) | (jj == self.h - 1)
        self.vertical = face not in ('up', 'down')
        self.long_axis = int(np.argmax(box.size))
        self.state = box.bone.state or 0


def metal(F, base, amp=1.0, rivets=True, rust=0.2, patina=0.0, scratches=True, bevel=True, seam=False):
    w, h, rng = F.w, F.h, F.rng
    n = vnoise(F.P, 3.0, F.seed) * 0.8 + white(F.P, F.seed + 1) * 0.2
    m = 1 + amp * 0.22 * (n - 0.5)
    if bevel and w >= 3 and h >= 3:
        m = np.where(F.j == 0, m * 1.14, m)
        m = np.where(F.j == h - 1, m * 0.80, m)
        m = np.where((F.i == 0) & (F.j > 0), m * 1.05, m)
        m = np.where((F.i == w - 1) & (F.j > 0), m * 0.86, m)
    m = quant(m)
    col = np.array(base, float)[None, None, :] * m[..., None]
    if seam and (w >= 8 or h >= 8):
        if w >= h:
            line = F.i == int(rng.integers(3, w - 3))
        else:
            line = F.j == int(rng.integers(3, h - 3))
        col[line] *= 0.62
        col[line & (((F.i + F.j) % 2) == 0)] = np.array(base) * 1.05
    if scratches and w * h >= 20:
        for _ in range(int(rng.integers(1, 3 + w * h // 60)) + F.state):
            x, y = int(rng.integers(1, max(2, w - 1))), int(rng.integers(1, max(2, h - 1)))
            dx = int(rng.choice([-1, 1]))
            for k in range(int(rng.integers(2, 4))):
                xx, yy = x + k * dx, y + k
                if 0 < xx < w - 1 and 0 < yy < h - 1:
                    col[yy, xx] = np.minimum(np.array(base) * 1.3, 255)
    rust = rust + 0.08 * F.state
    if rust > 0:
        # piu' ruggine verso i piedi
        rn = vnoise(F.P * np.array([1, 0.6, 1]), 2.2, F.seed + 5) + 0.12 * (1 - np.clip(F.P[..., 1] / 30, 0, 1))
        col[rn > 1.02 - rust] = PAL['rust']
        col[rn > 1.14 - rust] = PAL['rust_dark']
    if patina > 0:
        pn = vnoise(F.P, 2.0, F.seed + 9)
        col[pn > 1 - patina] = PAL['patina']
        col[pn > 1.12 - patina] = PAL['patina_dark']
    if rivets and w >= 5 and h >= 5:
        pts = [(1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)]
        if w >= 11:
            pts += [(x, 1) for x in range(5, w - 4, 4)] + [(x, h - 2) for x in range(5, w - 4, 4)]
        if h >= 11:
            pts += [(1, y) for y in range(5, h - 4, 4)] + [(w - 2, y) for y in range(5, h - 4, 4)]
        for x, y in pts:
            col[y, x] = np.minimum(np.array(base) * 1.45, 255)
            if y + 1 < h and x + 1 < w:
                col[y + 1, x + 1] = np.array(base) * 0.55
            if rust > 0 and rng.random() < 0.35:
                for k in range(2, int(rng.integers(3, 5))):
                    if y + k < h - 1:
                        col[y + k, x] = PAL['rust']
    if w >= 3 and h >= 3:
        col[F.edge] *= 0.82
    return col


def ribbed(F, base, period, dark):
    """Tubi e cavi: anelli lungo l'asse lungo della scatola."""
    rib = (np.floor(F.P[..., F.long_axis] + 0.001).astype(int) % period) == 0
    col = np.ones((F.h, F.w, 3)) * np.array(base, float)
    col *= (0.9 + 0.15 * white(F.P, F.seed))[..., None]
    col[rib] *= dark
    return col


def paint_face(F, mat):
    """Restituisce i colori RGB (altezza, larghezza, 3) della faccia."""
    o = F.box.opts
    rust = o.get('rust', 0.12)
    h, w = F.h, F.w
    if mat == 'frame':
        col = metal(F, PAL['frame'], amp=0.8, rivets=False, rust=rust * 0.6)
        # fori di bullone radi lungo i montanti lunghi e sottili
        if F.vertical and F.long_axis == 1 and h >= 6 and w <= 2:
            for y in range(2, h - 2, 4):
                col[y, (w - 1) // 2] = PAL['hole']
        elif w >= 6 and h <= 2 and F.long_axis == (2 if F.face in ('east', 'west') else 0):
            for x in range(2, w - 2, 4):
                col[(h - 1) // 2, x] = PAL['hole']
    elif mat == 'steel':
        col = metal(F, PAL['steel'], rust=rust, rivets=o.get('rivets', True), seam=o.get('seam', False))
    elif mat == 'joint':
        # mozzo d'ottone: bordo piu' scuro, bullone scuro al centro
        col = metal(F, PAL['brass'], amp=0.6, rivets=False, rust=0, bevel=False)
        if w >= 3 and h >= 3:
            col[F.edge] = np.array(PAL['brass_dark'], float)
            cx, cy = (w - 1) // 2, (h - 1) // 2
            col[cy, cx] = PAL['bolt']
            col[max(cy - 1, 1), min(cx + 1, w - 2)] = np.minimum(np.array(PAL['brass']) * 1.25, 255)
    elif mat == 'brass':
        col = metal(F, PAL['brass'], amp=0.7, rivets=False, rust=0, patina=0.08)
        if F.vertical and F.long_axis == 1 and h >= 3:
            col[0] *= 0.6
            col[h - 1] *= 0.6
    elif mat == 'chrome':
        col = np.ones((h, w, 3)) * np.array(PAL['chrome'], float)
        if F.vertical and w >= 2:
            col[:, 0] *= 1.12
            col[:, -1] *= 0.78
        col *= (0.94 + 0.08 * white(F.P, F.seed))[..., None]
    elif mat == 'copper':
        col = metal(F, PAL['copper'], rust=0, patina=o.get('patina', 0.18), rivets=o.get('rivets', True))
    elif mat == 'rubber':
        col = ribbed(F, PAL['rubber'], 2, 1.35)
    elif mat == 'cable':
        col = ribbed(F, PAL['copper'], 3, 0.7)
        col[vnoise(F.P, 2.0, F.seed + 9) > 0.8] = PAL['patina']
    elif mat == 'wire':
        col = np.ones((h, w, 3)) * np.array(PAL['red'], float)
        col[white(F.P, F.seed) > 0.75] = PAL['red_hot']
    elif mat == 'cell':
        col = np.ones((h, w, 3)) * np.array(PAL['red'], float)
        n = vnoise(F.P, 1.5, F.seed)
        col[n > 0.6] = PAL['red_hot']
        col[n < 0.35] = PAL['red_dim']
        if F.vertical and w >= 3:
            col[:, w // 2] = np.maximum(col[:, w // 2], PAL['red_hot'])
        col[F.edge] = PAL['red_dim']
    elif mat == 'lamp':
        col = np.ones((h, w, 3)) * np.array(PAL['red_hot'], float)
    elif mat == 'grill':
        col = metal(F, PAL['steel'], rivets=False, rust=rust, scratches=False)
        slat = (F.j % 2 == 1) if F.vertical else (F.i % 2 == 1)
        col[slat & ~F.edge] = PAL['black']
    elif mat == 'paint':
        # lamiera verniciata in grigio (colorata dal renderer); dove la vernice e' scheggiata resta ferro
        col = metal(F, PAL['paint'], rust=0, rivets=o.get('rivets', True), seam=o.get('seam', False))
        bare = metal(F, PAL['steel'], rivets=False, scratches=False, rust=rust * 1.2)
        chip = vnoise(F.P, 1.6, F.seed + 21) > 0.87 - 0.05 * F.state
        chip |= F.edge & (white(F.P, F.seed + 3) > 0.7 - 0.1 * F.state)
        col[chip] = bare[chip]
    else:
        raise ValueError(f'materiale sconosciuto {mat}')
    return col


def paint_model(model, debug_position=False):
    """Dipinge l'atlante di un modello impacchettato. Restituisce un array (altezza, larghezza, 4) uint8.
    Con debug_position ogni texel prende il colore della sua posizione 3D (serve a controllare nel visore che
    l'orientamento delle facce sia giusto: i colori devono proseguire senza salti sugli spigoli)."""
    W, H = ATLAS_SIZE
    atlas = np.zeros((H, W, 4), np.uint8)
    for b in model.boxes:
        if b.mirror_of is not None:
            continue
        for f, rect in b.faces.items():
            F = Face(b, f, rect)
            if debug_position:
                q = F.P + 0.25 * np.array(face_frame(f, b.lo, b.hi)[5])
                col = np.stack([(q[..., 0] + 13) / 26, q[..., 1] / 36, (q[..., 2] + 8) / 17], -1) * 230 + 15
                col[(F.i == 0) & (F.j == 0)] = (255, 255, 255)
            else:
                col = paint_face(F, b.mat_for(f))
            sl = (slice(F.v, F.v + F.h), slice(F.u, F.u + F.w))
            atlas[sl][..., :3] = np.clip(col, 0, 255).astype(np.uint8)
            atlas[sl][..., 3] = 255
    return atlas

