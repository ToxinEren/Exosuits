"""La stazione di manutenzione come modelli di blocco del gioco base (design M4, par. 5.1): le due meta' affiancate si
descrivono insieme, come un solo banco di 32 x 16 x 16 pixel (16 per blocco), e si dividono alla fine nei due modelli
`station_main` e `station_side` con le loro texture 16x16 (16 texel per lato di blocco).

Spazio del banco, visto da chi sta davanti alla stazione (in pixel, interi):
  x da 0 (estremo sinistro) a 32 (estremo destro), y da 0 (terra) a 16, z da 0 (il fronte) a 16 (il retro).
La meta' principale occupa x 0..16 e la laterale x 16..32: il gioco posa la laterale alla destra di chi guarda il
fronte (`StationBlock.otherHalf`, `FACING.getCounterClockWise()`). Coordinate dei modelli (blockstate con
facing=north, nessuna rotazione): fronte a nord (z = 0), x del modello = 16 - x del banco per la principale e
32 - x per la laterale.

Il banco e' una griglia di voxel da un pixel, piena. Un design ci scava tasche a partire da una faccia (`cut`) e ci
rialza parti dentro le tasche (`fill`), e dipinge le tele delle facce (`canvas`). Regola delle tasche (`check`): ogni
tasca resta chiusa dentro la sua faccia, cioe' non arriva al piano di un'altra faccia esterna, e due tasche di facce
diverse non si toccano. Il blocco e' un cubo pieno che nasconde le facce dei vicini (`canOcclude`): una tasca aperta
anche verso terra, verso il piano o verso un altro lato lascerebbe vedere attraverso il mondo. Le tasche possono
attraversare la giuntura fra le due meta'.

Le facce scoperte dei voxel diventano rettangoli (elementi piatti, una faccia ciascuno, uniti finche' hanno la stessa
texture e la stessa proiezione), quindi nessuna faccia si sovrappone a un'altra complanare. UV:
  - le facce rivolte come la faccia esterna della loro tasca (il fondo delle tasche, i piani esterni) prendono la
    proiezione piana della tela di quella faccia, come le UV predefinite del gioco base;
  - le pareti delle tasche (e i fianchi delle parti rialzate) prendono i texel della parte piena accanto, "piegati"
    attorno allo spigolo: lo spessore di una cornice mostra la cornice stessa;
  - la giuntura e' disegnata in ciascuna meta' (per una meta' da sola, come nel mondo di debug) senza `cullface`;
  - `cullface` solo sulle facce che stanno sul bordo del blocco.

Texture (gli stessi nomi in ogni direzione): `station_top` (sopra, sulla laterale specchiata), `station_side` (i due
estremi, sulla laterale specchiata, e le facce della giuntura), `station_front_main`, `station_front_side`,
`station_back_main`, `station_back_side`, `station_bottom`. Le tele di sopra e degli estremi si dipingono solo per la
principale: il banco e' simmetrico sopra e ai lati, e la laterale ne prende lo specchio (texture e tasche).
"""
import numpy as np

W, H, D = 32, 16, 16
SEAM = 16
NS = 'redstone_exoframes:block/'

# direzioni del banco: asse (0 = x, 1 = y, 2 = z) e verso
DIRS = (('-z', 2, -1), ('+z', 2, 1), ('+y', 1, 1), ('-y', 1, -1), ('-x', 0, -1), ('+x', 0, 1))
DIR = {name: (axis, sign) for name, axis, sign in DIRS}
# nome della direzione nel modello del gioco (facing=north): la x del modello va al contrario di quella del banco
LOCAL = {'-x': 'east', '+x': 'west', '-y': 'down', '+y': 'up', '-z': 'north', '+z': 'south'}
FACE_NORMAL = {'front': '-z', 'back': '+z', 'top': '+y', 'bottom': '-y', 'left': '-x', 'right': '+x'}
NORMAL_FACE = {v: k for k, v in FACE_NORMAL.items()}
HALVES = (('main', 0), ('side', SEAM))      # nome, prima x del banco
X0 = {'main': 16, 'side': 32}               # x del modello = X0 - x del banco

# FaceInfo del gioco base: le quattro posizioni dei vertici di una faccia (MIN/MAX per asse); il vertice 0 prende
# (u0, v0), l'1 (u0, v1), il 2 (u1, v1), il 3 (u1, v0) (BlockFaceUV.getU/getV, rotazione 0)
FACE_INFO = {
    'down': (('x0', 'y0', 'z1'), ('x0', 'y0', 'z0'), ('x1', 'y0', 'z0'), ('x1', 'y0', 'z1')),
    'up': (('x0', 'y1', 'z0'), ('x0', 'y1', 'z1'), ('x1', 'y1', 'z1'), ('x1', 'y1', 'z0')),
    'north': (('x1', 'y1', 'z0'), ('x1', 'y0', 'z0'), ('x0', 'y0', 'z0'), ('x0', 'y1', 'z0')),
    'south': (('x0', 'y1', 'z1'), ('x0', 'y0', 'z1'), ('x1', 'y0', 'z1'), ('x1', 'y1', 'z1')),
    'west': (('x0', 'y1', 'z0'), ('x0', 'y0', 'z0'), ('x0', 'y0', 'z1'), ('x0', 'y1', 'z1')),
    'east': (('x1', 'y1', 'z1'), ('x1', 'y0', 'z1'), ('x1', 'y0', 'z0'), ('x1', 'y1', 'z0')),
}

# tela di una faccia: (cx, cy) = A.P + b, in texel della tela (cx verso destra, cy verso il basso)
CANVAS = {
    'front': (((1, 0, 0), (0, -1, 0)), (0, 16)),
    'back': (((-1, 0, 0), (0, -1, 0)), (32, 16)),
    'top': (((1, 0, 0), (0, 0, -1)), (0, 16)),
    'left': (((0, 0, -1), (0, -1, 0)), (16, 16)),
    'right': (((0, 0, 1), (0, -1, 0)), (0, 16)),
}
# profondita' dal piano esterno della faccia verso l'interno: g.P + h
DEPTH = {'front': ((0, 0, 1), 0), 'back': ((0, 0, -1), 16), 'top': ((0, -1, 0), 16),
         'left': ((1, 0, 0), 0), 'right': ((-1, 0, 0), 32)}
VEC = {'-x': (-1, 0, 0), '+x': (1, 0, 0), '-y': (0, -1, 0), '+y': (0, 1, 0), '-z': (0, 0, -1), '+z': (0, 0, 1)}


def texture_of(face, half):
    """Texture e passaggio dalla tela alle sue UV: (nome, su, ou, sv, ov) con u = su * cx + ou, v = sv * cy + ov."""
    return {
        ('front', 'main'): ('front', 1, 0, 1, 0), ('front', 'side'): ('front', 1, -16, 1, 0),
        ('back', 'main'): ('back', 1, -16, 1, 0), ('back', 'side'): ('back', 1, 0, 1, 0),
        ('top', 'main'): ('top', -1, 16, -1, 16), ('top', 'side'): ('top', 1, -16, -1, 16),
        ('left', 'main'): ('side', 1, 0, 1, 0), ('right', 'side'): ('side', -1, 16, 1, 0),
    }[(face, half)]


def canvas_to_voxel(face, r, c, k):
    """Il voxel sotto il texel (r, c) della tela piena della faccia, a profondita' k dal piano esterno."""
    if face == 'front':
        return c, 15 - r, k
    if face == 'back':
        return 31 - c, 15 - r, 15 - k
    if face == 'top':
        return c, 15 - k, 15 - r
    if face == 'left':
        return k, 15 - r, 15 - c
    if face == 'right':
        return 31 - k, 15 - r, c
    raise ValueError(f'faccia senza tasche: {face}')


class Bench:
    def __init__(self):
        self.solid = np.ones((W, H, D), bool)
        self.owner = np.full((W, H, D), '', dtype=object)   # per i voxel vuoti: la faccia della loro tasca
        # tele: fronte e retro 16 x 32 (il retro visto da dietro: a sinistra la laterale); sopra ed estremo
        # sinistro 16 x 16, solo la principale (sopra: visto da davanti, il fronte in basso; estremo: visto da fuori,
        # il fronte a destra); sotto 16 x 16, gia' come texture
        self.canvas = {}

    # --- geometria -------------------------------------------------------------------------------------------------
    def _targets(self, face):
        """Le facce piene su cui agisce un comando dato su `face`: sopra e gli estremi sono simmetrici."""
        if face == 'top':
            return (('top', False), ('top', True))
        if face == 'left':
            return (('left', False), ('right', True))
        if face in ('front', 'back'):
            return ((face, False),)
        raise ValueError(f'faccia senza tasche: {face}')

    def _apply(self, face, rect, depth, start, solid):
        c0, r0, c1, r1 = rect
        width = 16 if face in ('top', 'left') else 32
        if not 0 <= start < depth <= 16:
            raise ValueError(f'profondita\' {start}..{depth} non valida su {face}')
        if not (0 <= c0 < c1 <= width and 0 <= r0 < r1 <= 16):
            raise ValueError(f'rettangolo {rect} fuori dalla tela di {face} ({width} x 16)')
        for target, mirrored in self._targets(face):
            for r in range(r0, r1):
                for c in range(c0, c1):
                    if target == 'top':
                        cc = 31 - c if mirrored else c
                    elif target == 'right':
                        cc = 15 - c
                    else:
                        cc = c
                    for k in range(start, depth):
                        v = canvas_to_voxel(target, r, cc, k)
                        self.solid[v] = solid
                        self.owner[v] = '' if solid else target

    def cut(self, face, rect, depth, start=0):
        """Scava una tasca: i voxel sotto `rect` (c0, r0, c1, r1 della tela, estremi esclusi) fra le profondita'
        `start` e `depth` (esclusa) dal piano esterno della faccia."""
        self._apply(face, rect, depth, start, False)

    def fill(self, face, rect, depth, start=0):
        """Rialza dentro una tasca: i voxel sotto `rect` fra `start` e `depth` tornano pieni."""
        self._apply(face, rect, depth, start, True)

    def depth_map(self, face):
        """Profondita' della superficie sotto ogni texel della tela di `face` (sopra ed estremo: solo la meta'
        principale), per la pittura delle tasche (`pixel.Canvas.relief`)."""
        target = {'top': 'top', 'left': 'left'}.get(face, face)
        width = 16 if face in ('top', 'left') else 32
        out = np.zeros((16, width), int)
        for r in range(16):
            for c in range(width):
                k = 0
                while k < 16 and not self.solid[canvas_to_voxel(target, r, c, k)]:
                    k += 1
                out[r, c] = k
        return out

    def check(self):
        """Problemi della geometria: tasche aperte verso un'altra faccia o unite a tasche di altre facce."""
        problems = []
        air = np.argwhere(~self.solid)
        for x, y, z in ((int(a), int(b), int(c)) for a, b, c in air):
            face = self.owner[x, y, z]
            for name, axis, sign in DIRS:
                n = [x, y, z]
                n[axis] += sign
                if not (0 <= n[0] < W and 0 <= n[1] < H and 0 <= n[2] < D):
                    if name != FACE_NORMAL[face]:
                        problems.append(f'tasca di {face} aperta verso {NORMAL_FACE[name]} nel voxel {(x, y, z)}')
                elif not self.solid[tuple(n)] and self.owner[tuple(n)] != face:
                    problems.append(f'tasche di {face} e {self.owner[tuple(n)]} unite nel voxel {(x, y, z)}')
        return problems

    # --- facce scoperte e UV ---------------------------------------------------------------------------------------
    def _map(self, rule, half):
        """Mappa affine dal punto P del banco alle UV della texture: (texture, M 2x3, c 2), interi."""
        kind = rule[0]
        if kind == 'bottom':
            x0 = X0[half]
            return 'bottom', ((-1, 0, 0), (0, 0, -1)), (x0, 16)
        if kind == 'seam':
            return 'side', ((0, 0, -1), (0, -1, 0)), (16, 16)
        face = rule[1]
        (A, b) = CANVAS[face]
        tex, su, ou, sv, ov = texture_of(face, half)
        A = np.array(A, int)
        b = np.array(b, int)
        if kind == 'fold':
            d, s = rule[2], rule[3]
            nf = np.array(VEC[FACE_NORMAL[face]], int)
            dv = np.array(VEC[d], int)
            g, h = DEPTH[face]
            g = np.array(g, int)
            T = np.eye(3, dtype=int) + np.outer(nf - dv, g)
            t0 = (h - s) * (nf - dv)
            b = A @ t0 + b
            A = A @ T
        C = np.array(((su, 0), (0, sv)), int)
        M = C @ A
        c = C @ b + np.array((ou, ov), int)
        return tex, tuple(tuple(int(v) for v in row) for row in M), tuple(int(v) for v in c)

    def faces(self):
        """Per ogni meta': {(direzione, piano): {(i, j): chiave}} delle facce scoperte dei voxel, con la chiave
        (texture, M, c, cullface) della loro mappa UV."""
        out = {}
        for half, hx in HALVES:
            grid = {}
            for x in range(hx, hx + 16):
                for y in range(H):
                    for z in range(D):
                        if not self.solid[x, y, z]:
                            continue
                        for name, axis, sign in DIRS:
                            n = [x, y, z]
                            n[axis] += sign
                            cull = None
                            if not (0 <= n[0] < W and 0 <= n[1] < H and 0 <= n[2] < D):
                                face = NORMAL_FACE[name]
                                rule = ('bottom',) if face == 'bottom' else ('proj', face)
                                cull = LOCAL[name]
                            elif not (hx <= n[0] < hx + 16) and self.solid[tuple(n)]:
                                rule = ('seam',)
                            elif self.solid[tuple(n)]:
                                continue
                            else:
                                face = self.owner[tuple(n)]
                                if FACE_NORMAL[face] == name:
                                    rule = ('proj', face)
                                else:
                                    rule = ('fold', face, name, self._surface_depth(face, x, y, z))
                            key = self._map(rule, half) + (cull,)
                            plane = (x, y, z)[axis] + (1 if sign > 0 else 0)
                            other = tuple(i for i in range(3) if i != axis)
                            cell = ((x, y, z)[other[0]], (x, y, z)[other[1]])
                            grid.setdefault((name, plane), {})[cell] = key
            out[half] = grid
        return out

    def _surface_depth(self, face, x, y, z):
        """Profondita' (da `face`) della superficie della colonna piena che contiene il voxel (x, y, z): la parte
        piena di cui la parete e' il fianco. E' la profondita' del lato verso l'esterno del voxel piu' esterno."""
        g, h = DEPTH[face]
        axis = next(i for i in range(3) if g[i])
        outward = -g[axis]
        p = [x, y, z]
        while True:
            q = list(p)
            q[axis] += outward
            if not (0 <= q[0] < W and 0 <= q[1] < H and 0 <= q[2] < D) or not self.solid[tuple(q)]:
                break
            p = q
        k = g[0] * p[0] + g[1] * p[1] + g[2] * p[2] + h
        return k if g[axis] > 0 else k - 1

    # --- esportazione ----------------------------------------------------------------------------------------------
    def models(self):
        """I due modelli: {'main': json, 'side': json} (dizionari pronti per exo.dumps_json)."""
        out = {}
        for half, grid in self.faces().items():
            elements = []
            for name, _, _ in DIRS:
                planes = sorted(p for (n, p) in grid if n == name)
                for plane in planes:
                    cells = grid[(name, plane)]
                    for (i0, i1, j0, j1), key in _rectangles(cells):
                        elements.append(self._element(half, name, plane, (i0, i1, j0, j1), key))
            textures = {'particle': NS + 'station_side', 'top': NS + 'station_top', 'side': NS + 'station_side',
                        'front': NS + 'station_front_' + half, 'back': NS + 'station_back_' + half,
                        'bottom': NS + 'station_bottom'}
            used = {e['faces'][f]['texture'][1:] for e in elements for f in e['faces']}
            textures = {k: v for k, v in textures.items() if k == 'particle' or k in used}
            out[half] = {'textures': textures, 'elements': elements}
        return out

    def _element(self, half, name, plane, rect, key):
        axis, sign = DIR[name]
        other = tuple(i for i in range(3) if i != axis)
        lo, hi = [0, 0, 0], [0, 0, 0]
        lo[axis] = hi[axis] = plane
        lo[other[0]], hi[other[0]] = rect[0], rect[1]
        lo[other[1]], hi[other[1]] = rect[2], rect[3]
        x0 = X0[half]
        frm = [x0 - hi[0], lo[1], lo[2]]
        to = [x0 - lo[0], hi[1], hi[2]]
        tex, M, c, cull = key
        local = LOCAL[name]
        corners = {'x0': frm[0], 'x1': to[0], 'y0': frm[1], 'y1': to[1], 'z0': frm[2], 'z1': to[2]}

        def uv(vertex):
            lx, ly, lz = (corners[a] for a in vertex)
            p = (x0 - lx, ly, lz)
            return tuple(M[i][0] * p[0] + M[i][1] * p[1] + M[i][2] * p[2] + c[i] for i in range(2))

        want = [uv(v) for v in FACE_INFO[local]]
        rotation, rect = _face_uv(want)
        if rect is None:
            raise AssertionError(f'UV non allineate a FaceInfo: {half} {local} {frm} {to} -> {want}')
        u0, v0, u1, v1 = rect
        for t in rect:
            if not 0 <= t <= 16:
                raise ValueError(f'UV fuori dalla texture ({tex}): {half} {local} {frm} {to} -> {rect}')
        face = {'uv': [u0, v0, u1, v1], 'texture': '#' + tex}
        if rotation:
            face['rotation'] = rotation
        if cull:
            face['cullface'] = cull
        return {'from': frm, 'to': to, 'faces': {local: face}}

    def textures(self):
        """Le texture 16x16 RGBA (uint8) dalle tele dipinte: {nome del file senza estensione: array}."""
        cv = {k: v.rgba() for k, v in self.canvas.items()}
        for name, shape in (('front', (16, 32)), ('back', (16, 32)), ('top', (16, 16)), ('left', (16, 16)),
                            ('bottom', (16, 16))):
            if cv[name].shape[:2] != shape:
                raise ValueError(f'tela {name}: {cv[name].shape[:2]} invece di {shape}')
        out = {
            'station_front_main': cv['front'][:, :16], 'station_front_side': cv['front'][:, 16:],
            'station_back_main': cv['back'][:, 16:], 'station_back_side': cv['back'][:, :16],
            'station_top': np.rot90(cv['top'], 2), 'station_side': cv['left'], 'station_bottom': cv['bottom'],
        }
        for name, img in out.items():
            if (img[..., 3] != 255).any():
                raise ValueError(f'{name}: texel non opachi (il blocco si disegna come solido)')
        return {k: np.ascontiguousarray(v) for k, v in out.items()}


def _face_uv(want):
    """Rotazione e UV [u0, v0, u1, v1] di una faccia che danno ai suoi quattro vertici (in ordine FaceInfo) le UV
    `want`, come BlockFaceUV.getU/getV: con la rotazione R il vertice i prende l'angolo (i + R / 90) % 4, dove gli
    angoli 0, 1, 2, 3 sono (u0, v0), (u0, v1), (u1, v1), (u1, v0). Basta R 0 (la v cambia lungo il lato 0-1) o 90 (ci
    cambia la u). (None, None) se nessuna va."""
    for rotation in (0, 90):
        shift = rotation // 90
        corner = {(i + shift) % 4: want[i] for i in range(4)}
        u0, v0 = corner[0]
        u1, v1 = corner[2]
        if corner[1] == (u0, v1) and corner[3] == (u1, v0):
            return rotation, (u0, v0, u1, v1)
    return None, None


def _rectangles(cells):
    """Unione golosa delle celle con la stessa chiave in rettangoli: [((i0, i1, j0, j1), chiave)], in ordine."""
    left = dict(cells)
    out = []
    for cell in sorted(cells):
        if cell not in left:
            continue
        key = left[cell]
        i0, j0 = cell
        j1 = j0 + 1
        while left.get((i0, j1)) == key:
            j1 += 1
        i1 = i0 + 1
        while all(left.get((i1, j)) == key for j in range(j0, j1)):
            i1 += 1
        for i in range(i0, i1):
            for j in range(j0, j1):
                del left[(i, j)]
        out.append(((i0, i1, j0, j1), key))
    return out
