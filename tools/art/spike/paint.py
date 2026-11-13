"""Procedural pixel-art painter for the box atlas.

Each face texel knows its 3D position, so noise and rust are continuous across
box edges. Output: atlas (RGBA), glow (RGB emissive) and tint mask
(R = primary paint, G = secondary paint, B = banner decal area).
"""
import numpy as np
from PIL import Image

from exo import FACES, face_frame

# ---- limited palette ------------------------------------------------------
PAL = {
    'iron': (122, 126, 132), 'iron2': (116, 106, 100), 'iron3': (104, 110, 118),
    'frame': (98, 101, 110), 'copper': (180, 104, 56), 'brass': (178, 146, 72),
    'chrome': (184, 190, 196), 'rubber': (46, 42, 44), 'rust': (138, 74, 42),
    'rust_dark': (98, 54, 34), 'patina': (88, 150, 126), 'patina_dark': (62, 118, 102),
    'paint1': (160, 128, 64), 'paint2': (66, 92, 96), 'decal': (188, 180, 160),
    'red_dim': (104, 10, 10), 'red': (200, 30, 24), 'red_hot': (255, 104, 70),
    'black': (30, 28, 32), 'gauge_face': (222, 214, 192),
}


def _hash(ix, iy, iz, seed):
    h = (ix.astype(np.int64) * 374761393 + iy.astype(np.int64) * 668265263
         + iz.astype(np.int64) * 1274126177 + seed * 144665) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    h = h ^ (h >> 16)
    return (h & 0xFFFF) / 65535.0


def vnoise(P, scale, seed):
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
    i = np.floor(P * 1.0 + 0.001).astype(np.int64)
    return _hash(i[..., 0], i[..., 1], i[..., 2], seed)


def quant(m, steps=(0.78, 0.88, 0.96, 1.04, 1.12)):
    s = np.array(steps)
    idx = np.clip(np.round((m - s[0]) / (s[1] - s[0])), 0, len(s) - 1).astype(int)
    return s[idx]


class Face:
    def __init__(self, box, face, rect, model_seed):
        self.box, self.face = box, face
        u, v, w, h = rect
        self.u, self.v, self.w, self.h = u, v, w, h
        tl, ua, va, fw, fh = face_frame(face, box.lo, box.hi)
        jj, ii = np.mgrid[0:h, 0:w]
        self.i, self.j = ii, jj
        tl, ua, va = np.array(tl), np.array(ua), np.array(va)
        # texel centre, nudged slightly inward so integer hashing is stable
        n = np.cross(va, ua)
        self.P = (tl + (ii[..., None] + 0.5) * ua + (jj[..., None] + 0.5) * va - 0.25 * n)
        self.seed = box.seed + model_seed
        self.edge = (ii == 0) | (jj == 0) | (ii == w - 1) | (jj == h - 1)
        self.vertical = face in ('north', 'south', 'east', 'west')


def metal(F, base, amp=1.0, rivets=True, rust=0.25, patina=0.0, scratches=True,
          bevel=True, seam=False, rng=None):
    w, h = F.w, F.h
    n = vnoise(F.P, 3.0, F.seed) * 0.8 + white(F.P, F.seed + 1) * 0.2
    m = 1 + amp * 0.22 * (n - 0.5)
    if bevel and w >= 3 and h >= 3:
        m = np.where(F.j == 0, m * 1.14, m)
        m = np.where(F.j == h - 1, m * 0.80, m)
        m = np.where((F.i == 0) & (F.j > 0), m * 1.05, m)
        m = np.where((F.i == w - 1) & (F.j > 0), m * 0.86, m)
    m = quant(m)
    col = np.array(base, float)[None, None, :] * m[..., None]
    # seam (weld line) on large faces
    if seam and (w >= 8 or h >= 8):
        if w >= h:
            c = int(rng.integers(3, w - 3))
            line = (F.i == c)
        else:
            c = int(rng.integers(3, h - 3))
            line = (F.j == c)
        col[line] *= 0.62
        weld = line & (((F.i + F.j) % 2) == 0)
        col[weld] = np.array(base) * 1.05
    # scratches
    if scratches and w * h >= 20:
        for _ in range(int(rng.integers(1, 3 + w * h // 60))):
            x, y = int(rng.integers(1, max(2, w - 1))), int(rng.integers(1, max(2, h - 1)))
            dx = int(rng.choice([-1, 1]))
            for k in range(int(rng.integers(2, 4))):
                xx, yy = x + k * dx, y + k
                if 0 < xx < w - 1 and 0 < yy < h - 1:
                    col[yy, xx] = np.minimum(np.array(base) * 1.3, 255)
    # rust (more towards the bottom of the suit) and copper patina
    if rust > 0:
        rn = vnoise(F.P * np.array([1, 0.6, 1]), 2.2, F.seed + 5) + 0.12 * (1 - np.clip(F.P[..., 1] / 30, 0, 1))
        r1 = rn > 1.02 - rust
        r2 = rn > 1.14 - rust
        col[r1] = PAL['rust']
        col[r2] = PAL['rust_dark']
    if patina > 0:
        pn = vnoise(F.P, 2.0, F.seed + 9)
        col[pn > 1 - patina] = PAL['patina']
        col[pn > 1.12 - patina] = PAL['patina_dark']
    # rivets
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
            # rust drip under some rivets
            if rust > 0 and rng.random() < 0.35:
                for k in range(2, int(rng.integers(3, 5))):
                    if y + k < h - 1:
                        col[y + k, x] = PAL['rust']
    # panel edge
    if w >= 3 and h >= 3:
        col[F.edge] *= 0.82
    return col


def paint_face(F, mat, rng):
    """Return (rgb[h,w,3], glow[h,w,3], mask[h,w,3])."""
    h, w = F.h, F.w
    glow = np.zeros((h, w, 3))
    mask = np.zeros((h, w, 3))
    o = F.box.opts
    rust = o.get('rust', 0.12)
    if mat in ('iron', 'iron2', 'iron3'):
        col = metal(F, PAL[mat], rust=rust, seam=o.get('seam', True), rivets=o.get('rivets', True), rng=rng)
    elif mat == 'frame':
        col = metal(F, PAL['frame'], amp=0.8, rivets=False, rust=rust * 0.5, scratches=True, rng=rng)
        # bolt holes along long struts
        if F.vertical and h >= 5 and w <= 3:
            for y in range(2, h - 1, 3):
                col[y, w // 2] = (40, 40, 46)
    elif mat == 'joint':
        # brass hub with a dark bolt in the middle and a darker rim
        col = metal(F, PAL['brass'], amp=0.6, rivets=False, rust=0, rng=rng, bevel=True)
        col[F.edge] = np.array(PAL['frame']) * 0.9
        col[(F.i == (w - 1) // 2) & (F.j == (h - 1) // 2)] = (50, 46, 40)
    elif mat == 'copper':
        col = metal(F, PAL['copper'], rust=0, patina=o.get('patina', 0.18), rng=rng)
    elif mat == 'brass':
        col = metal(F, PAL['brass'], amp=0.7, rivets=False, rust=0, patina=0.1, rng=rng, bevel=True)
        if F.vertical and h >= 3:
            col[0] *= 0.6
            col[h - 1] *= 0.6
    elif mat == 'chrome':
        col = np.ones((h, w, 3)) * np.array(PAL['chrome'])
        if F.vertical and w >= 2:
            col[:, 0] *= 1.12
            col[:, -1] *= 0.78
        col *= (0.94 + 0.08 * white(F.P, F.seed))[..., None]
    elif mat in ('rubber', 'pipe'):
        base = PAL['rubber'] if mat == 'rubber' else PAL['copper']
        size = np.array(F.box.size)
        ax = int(np.argmax(size))
        rib = (np.floor(F.P[..., ax]).astype(int) % (2 if mat == 'rubber' else 3)) == 0
        col = np.ones((h, w, 3)) * np.array(base)
        col *= (0.9 + 0.15 * white(F.P, F.seed))[..., None]
        col[rib] *= 0.7 if mat == 'pipe' else 1.35
        if mat == 'pipe':
            pn = vnoise(F.P, 2.0, F.seed + 9)
            col[pn > 0.78] = PAL['patina']
    elif mat in ('paint1', 'paint2'):
        base = PAL[mat]
        col = metal(F, base, rust=0, rivets=o.get('rivets', True), seam=o.get('seam', False), rng=rng)
        bare = metal(F, PAL['iron'], rivets=False, scratches=False, rust=rust * 1.2, rng=rng)
        chip = vnoise(F.P, 1.6, F.seed + 21) > 0.87 - o.get('wear', 0.0)
        edgechip = F.edge & (white(F.P, F.seed + 3) > 0.7)
        ch = chip | edgechip
        col[ch] = bare[ch]
        mask[..., 0 if mat == 'paint1' else 1] = np.where(ch, 0, 255)
        # stencilled number-ish dash marks are avoided on purpose (no lettering)
    elif mat == 'decal':
        # flat area reserved for a banner pattern; placeholder = plain field + a diagonal bend
        col = np.ones((h, w, 3)) * np.array(PAL['decal'])
        col *= (0.96 + 0.06 * white(F.P, F.seed))[..., None]
        band = ((F.i - F.j) == 0) | ((F.i - F.j) == 1)
        col[band] = (150, 142, 124)
        col[F.edge] = (150, 142, 124)
        mask[..., 2] = 255
    elif mat == 'redstone':
        col = np.ones((h, w, 3)) * np.array(PAL['red'])
        n = white(F.P, F.seed)
        col[n > 0.65] = PAL['red_hot']
        col[n < 0.2] = PAL['red_dim']
        glow[:] = col * 0.9
    elif mat == 'wire':
        col = np.ones((h, w, 3)) * np.array(PAL['red'])
        n = white(F.P, F.seed)
        col[n > 0.75] = PAL['red_hot']
        glow[:] = col * 0.6
    elif mat == 'cell':
        col = np.ones((h, w, 3)) * np.array(PAL['red'])
        n = vnoise(F.P, 1.5, F.seed)
        col[n > 0.6] = PAL['red_hot']
        col[n < 0.35] = PAL['red_dim']
        if F.vertical and w >= 3:
            col[:, w // 2] = np.maximum(col[:, w // 2], PAL['red_hot'])
        glow[:] = col
        col[F.edge] = PAL['black']
        glow[F.edge] = 0
    elif mat == 'visor':
        col = np.ones((h, w, 3)) * np.array(PAL['black'])
        if h >= 2:
            row = h // 2
            on = (F.j == row) & (white(F.P, F.seed) > 0.3) & (F.i > 0) & (F.i < w - 1)
            col[on] = PAL['red_hot']
            glow[on] = PAL['red_hot']
            col[(F.j == row) & ~on & (F.i > 0) & (F.i < w - 1)] = PAL['red_dim']
    elif mat == 'grill':
        col = metal(F, PAL['iron3'], rivets=False, rust=rust, rng=rng, scratches=False)
        size = F.box.size
        slat = (F.j % 2 == 1) if F.vertical else (F.i % 2 == 1)
        inner = slat & ~F.edge
        col[inner] = PAL['black']
        if o.get('hot'):
            hot = inner & (white(F.P, F.seed) > 0.45)
            col[hot] = PAL['red']
            glow[hot] = np.array(PAL['red']) * 0.8
    elif mat == 'gauge':
        col = metal(F, PAL['brass'], rivets=False, rust=0, rng=rng)
        inner = ~F.edge
        col[inner] = PAL['gauge_face']
        cx, cy = w // 2, h // 2
        col[cy, cx] = PAL['black']
        for k in range(1, max(2, min(w, h) // 2 - 1)):
            if 0 < cx + k < w - 1 and 0 < cy - k < h - 1:
                col[cy - k, cx + k] = PAL['red']
    elif mat == 'porthole':
        col = metal(F, PAL['copper'], rivets=False, rust=0, patina=0.2, rng=rng)
        inner = ~F.edge
        col[inner] = (40, 34, 38)
        col[inner & (F.j == 1)] = (70, 60, 64)
        glint = inner & (F.i == F.w - 2) & (F.j == F.h - 2)
        col[glint] = PAL['red_hot']
        glow[glint] = PAL['red_hot']
        lit = inner & (F.j == F.h - 2) & (F.i < F.w - 2)
        col[lit] = PAL['red_dim']
    elif mat == 'light':
        col = np.ones((h, w, 3)) * np.array(PAL['red_hot'])
        glow[:] = col
    elif mat == 'hazard':
        col = metal(F, PAL['paint1'], rivets=False, rust=0.1, rng=rng)
        stripe = ((np.floor(F.P[..., 0] + F.P[..., 1] + F.P[..., 2]).astype(int) // 2) % 2) == 0
        col[stripe] = np.array(PAL['black']) * 1.3
        col[F.edge] *= 0.8
    else:
        raise ValueError(mat)
    return col, glow, mask


def paint_model(model, seed=1234):
    W, H = model.atlas_w, model.atlas_h
    atlas = np.zeros((H, W, 4), np.uint8)
    glow = np.zeros((H, W, 3), np.uint8)
    mask = np.zeros((H, W, 3), np.uint8)
    rng = np.random.default_rng(seed)
    for b in model.boxes:
        if b.mirror_of is not None:
            continue
        for f in FACES:
            if f not in b.uv:
                continue
            F = Face(b, f, b.uv[f], seed)
            col, gl, mk = paint_face(F, b.mat_for(f), rng)
            sl = (slice(F.v, F.v + F.h), slice(F.u, F.u + F.w))
            atlas[sl][..., :3] = np.clip(col, 0, 255).astype(np.uint8)
            atlas[sl][..., 3] = 255
            glow[sl] = np.clip(gl, 0, 255).astype(np.uint8)
            mask[sl] = np.clip(mk, 0, 255).astype(np.uint8)
    return Image.fromarray(atlas, 'RGBA'), Image.fromarray(glow, 'RGB'), Image.fromarray(mask, 'RGB')
