"""Descrizione del modello a scatole, impacchettamento nell'atlante, controlli ed esportazione Bedrock.

Convenzioni (coordinate del file .geo.json, cioe' Bedrock): 1 unita' = 1 pixel di modello = 1/16 di blocco prima
della scala della tuta (SUIT_SCALE) = DENSITY texel dell'atlante; y verso l'alto, il modello poggia su y = 0, il
fronte della tuta guarda verso -z, il lato destro di chi la indossa sta a x negativa. GeckoLib, caricando il file,
nega x (vedi BakedModelFactory): per questo la faccia "east" di una scatola e' quella a x minima del file e "west"
quella a x massima. L'orientamento delle facce qui sotto (face_frame) e' ricavato dal codice di GeckoLib
(GeoQuad.build e VertexSet) e verificato con il visore di tools/art/preview contro l'armatura d'esempio di GeckoLib
(vedi tools/art/README.md).

Un file di design (tools/art/designs/<nome>.py) dichiara ossa, scatole, materiali e regioni di colore; questo modulo
non conosce nessun design.
"""
import json
import math
import os
import struct
import zlib

# --- contratto con il renderer ---------------------------------------------------------------------------
# Ossa di primo livello e perni (pixel, coordinate del file). Il renderer d'armatura di GeckoLib le cerca per nome.
TOP_BONES = (
    ('armorHead', (0, 24, 0)),
    ('armorBody', (0, 24, 0)),
    ('armorRightArm', (-5, 22, 0)),
    ('armorLeftArm', (5, 22, 0)),
    ('armorRightLeg', (-2, 12, 0)),
    ('armorLeftLeg', (2, 12, 0)),
)
TOP_PIVOT = dict(TOP_BONES)
FACES = ('north', 'east', 'south', 'west', 'up', 'down')

# --- scala e pilota ---------------------------------------------------------------------------------------
# Nel gioco la tuta si disegna scalata di SUIT_SCALE attorno ai piedi (0,0,0); il pilota resta a grandezza normale,
# sollevato di PILOT_LIFT_PX pixel (9.6) perche' collo e spalle combacino con quelli della tuta, e senza gambe (stanno
# nelle cosce chiuse della tuta). Il modello qui e' scritto in pixel non scalati.
SUIT_SCALE = 1.4
PILOT_LIFT_PX = 24 * (SUIT_SCALE - 1)

# Ingombri massimi (pixel non scalati): FRAME_* per le ossa del telaio nudo (regione 'frame', gli stessi che
# controlla ExoframeGeometryTest), SUIT_* per le ossa dei pezzi (telaio con i pezzi montati).
FRAME_MIN = (-12.75, 0.0, -8.0)
FRAME_MAX = (12.75, 30.7, 10.0)
SUIT_MIN = (-13.5, 0.0, -8.75)
SUIT_MAX = (13.5, 31.5, 10.5)

# Giocatore di riferimento (pixel, grandezza normale, in piedi su y = 0): (nome, minimo, massimo, distanza minima
# delle scatole dalla superficie). 0.25 sul corpo e sugli arti: il secondo strato della skin sta a +0.25 e una
# scatola che lo tocca da fuori non gli e' complanare con la stessa normale. 0.5 sulla testa, dove il cappello della
# skin sta a +0.5.
PLAYER = (
    ('head', (-4, 24, -4), (4, 32, 4), 0.5),
    ('body', (-4, 12, -2), (4, 24, 2), 0.25),
    ('rightArm', (-8, 12, -2), (-4, 24, 2), 0.25),
    ('leftArm', (4, 12, -2), (8, 24, 2), 0.25),
    ('rightLeg', (-4, 0, -2), (0, 12, 2), 0.25),
    ('leftLeg', (0, 0, -2), (4, 12, 2), 0.25),
)
# Le parti del pilota che si disegnano dentro la tuta (e che Model.check() tiene libere): tutto tranne le gambe.
PILOT = tuple(p for p in PLAYER if not p[0].endswith('Leg'))

# --- atlante -----------------------------------------------------------------------------------------------
# DENSITY texel per pixel di modello su ogni faccia. 512x512, diviso in regioni fisse: ogni osso dichiara la sua e
# l'impacchettatore riempie una regione alla volta, quindi aggiungere i pezzi (regioni piece_s*) non sposta mai i
# rettangoli del telaio.
#   frame     y   0..111  telaio nudo
#   decal     y 112..127  decalcomanie (texel trasparenti fino a M5)
#   piece_s0  y 128..223  pezzi, stato 0 (integro)
#   piece_s1  y 224..319  pezzi, stato 1 (danneggiato)
#   piece_s2  y 320..415  pezzi, stato 2 (critico)
#   piece_s3  y 416..511  pezzi, stato 3 (rotto)
# Le quattro regioni dei pezzi hanno la stessa forma: lo stesso pezzo dichiarato con le stesse scatole nei quattro
# stati finisce nella stessa posizione relativa di ciascuna.
DENSITY = 2
ATLAS_SIZE = (512, 512)
ATLAS_REGIONS = (
    ('frame', (0, 0, 512, 112)),
    ('decal', (0, 112, 512, 16)),
    ('piece_s0', (0, 128, 512, 96)),
    ('piece_s1', (0, 224, 512, 96)),
    ('piece_s2', (0, 320, 512, 96)),
    ('piece_s3', (0, 416, 512, 96)),
)

# --- pezzi -------------------------------------------------------------------------------------------------
# Stati visivi di un pezzo (0 integro, 1 danneggiato, 2 critico, 3 rotto) e regioni di colore (primaria, secondaria,
# neutra): ossa piece<Slot>_s<stato>_<regione>, come in SuitBones.piece. Un design dichiara le scatole di un pezzo una
# volta sola, nell'osso modello piece<Slot>_s*_<regione> (vedi piece()), e Model.box() le ripete nei quattro stati.
STATES = 4
REGIONS = ('p1', 'p2', 'n')
ANY_STATE = '_s*_'


def piece(slot, region):
    """Osso modello di una regione del pezzo di uno slot: le scatole dichiarate qui finiscono in tutti gli stati."""
    if region not in REGIONS:
        raise ValueError(f'regione {region} sconosciuta')
    return f'piece{slot}{ANY_STATE}{region}'


EPS = 1e-6
# Passo della griglia delle coordinate (Box le vuole in quarti di pixel).
GRID = 0.25


def face_frame(face, lo, hi):
    """Restituisce (angolo, asse_u, asse_v, larghezza, altezza, normale) di una faccia, in coordinate del file.

    L'angolo e' il punto 3D che corrisponde all'angolo in alto a sinistra del rettangolo nell'atlante; u cresce
    verso destra nell'atlante, v verso il basso. Ricavato da GeckoLib 4.x (VertexSet.quadXxx + GeoQuad.build con
    UV per faccia, uv_size positivo): la faccia si vede da fuori non specchiata.
    """
    x0, y0, z0 = lo
    x1, y1, z1 = hi
    w, h, d = x1 - x0, y1 - y0, z1 - z0
    if face == 'north':
        return (x0, y1, z0), (1, 0, 0), (0, -1, 0), w, h, (0, 0, -1)
    if face == 'south':
        return (x1, y1, z1), (-1, 0, 0), (0, -1, 0), w, h, (0, 0, 1)
    if face == 'east':
        return (x0, y1, z1), (0, 0, -1), (0, -1, 0), d, h, (-1, 0, 0)
    if face == 'west':
        return (x1, y1, z0), (0, 0, 1), (0, -1, 0), d, h, (1, 0, 0)
    if face == 'up':
        return (x0, y1, z1), (1, 0, 0), (0, 0, -1), w, d, (0, 1, 0)
    if face == 'down':
        return (x0, y0, z0), (1, 0, 0), (0, 0, 1), w, d, (0, -1, 0)
    raise ValueError(face)


def texels(length):
    """Numero di texel per un lato lungo `length` pixel: DENSITY per pixel, arrotondato in su (a densita' 2 un lato
    di 0.25 px prende comunque un texel intero)."""
    return max(1, int(math.ceil(length * DENSITY - EPS)))


def mirror_name(name):
    """Nome dell'osso simmetrico: Right <-> Left (le ossa centrali restano uguali)."""
    if 'Right' in name:
        return name.replace('Right', 'Left')
    if 'Left' in name:
        return name.replace('Left', 'Right')
    return name


class Bone:
    """Osso figlio di un osso di primo livello. `state` (0-3 o None) e `paint` ('p1', 'p2', 'n' o None) servono al
    pittore e ai controlli: due ossa con stati diversi non sono mai visibili insieme."""

    def __init__(self, name, parent, region, state=None, paint=None):
        if parent not in TOP_PIVOT:
            raise ValueError(f'osso {name}: genitore {parent} non e\' un osso di primo livello')
        if region not in dict(ATLAS_REGIONS):
            raise ValueError(f'osso {name}: regione {region} sconosciuta')
        self.name, self.parent, self.region, self.state, self.paint = name, parent, region, state, paint


class Box:
    def __init__(self, bone, name, lo, hi, mat, opts, seed_name=None):
        self.bone, self.name = bone, name
        self.lo = tuple(float(v) for v in lo)
        self.hi = tuple(float(v) for v in hi)
        for a, b in zip(self.lo, self.hi):
            if b - a <= EPS:
                raise ValueError(f'scatola {name}: dimensioni non positive {lo} -> {hi}')
            for v in (a, b):
                if abs(v / GRID - round(v / GRID)) > EPS:
                    raise ValueError(f'scatola {name}: le coordinate vanno in quarti di pixel ({v})')
        self.mat = mat if isinstance(mat, dict) else {'default': mat}
        self.opts = dict(opts)
        self.mirror_of = None   # scatola sorgente se questa e' la copia specchiata (condivide le UV)
        self.mirrors = []       # copie specchiate di questa scatola
        self.faces = {}         # faccia -> (u, v, larghezza, altezza) in texel; facce sepolte assenti
        # le copie di uno stesso pezzo nei quattro stati hanno lo stesso seme: graffi e ammaccature stanno negli
        # stessi punti e crescono con lo stato
        self.seed = zlib.crc32((seed_name or name).encode('utf-8'))

    @property
    def size(self):
        return tuple(b - a for a, b in zip(self.lo, self.hi))

    def mat_for(self, face):
        return self.mat.get(face, self.mat['default'])


class Model:
    def __init__(self, identifier):
        self.identifier = identifier
        self.bones = []
        self.boxes = []
        self._names = set()

    # --- dichiarazione -------------------------------------------------------------------------------------
    def bone(self, name, parent, region, state=None, paint=None):
        if any(b.name == name for b in self.bones) or name in TOP_PIVOT:
            raise ValueError(f'osso duplicato {name}')
        b = Bone(name, parent, region, state, paint)
        self.bones.append(b)
        return b

    def piece_bones(self, slot, parent):
        """Le dodici ossa del pezzo di uno slot: quattro stati (regioni piece_s0..s3) per tre regioni di colore."""
        for state in range(STATES):
            for region in REGIONS:
                self.bone(piece(slot, region).replace(ANY_STATE, f'_s{state}_'), parent, f'piece_s{state}',
                          state=state, paint=region)

    def _bone(self, name):
        for b in self.bones:
            if b.name == name:
                return b
        raise ValueError(f'osso {name} non dichiarato')

    def box(self, bone, name, lo, hi, mat, **opts):
        """Aggiunge una scatola all'osso `bone` (figlio dichiarato con bone()). lo/hi: angoli minimo e massimo.
        Con un osso modello (piece()) aggiunge una copia per stato, con lo stesso seme. Restituisce l'elenco delle
        scatole create."""
        if ANY_STATE in bone:
            return [self._add(bone.replace(ANY_STATE, f'_s{s}_'), f'{name}_s{s}', lo, hi, mat, opts, name)
                    for s in range(STATES)]
        return [self._add(bone, name, lo, hi, mat, opts, None)]

    def _add(self, bone, name, lo, hi, mat, opts, seed_name):
        if name in self._names:
            raise ValueError(f'nome di scatola duplicato {name}')
        self._names.add(name)
        b = Box(self._bone(bone), name, lo, hi, mat, opts, seed_name)
        self.boxes.append(b)
        return b

    def sym(self, bone, name, lo, hi, mat, **opts):
        """Scatola sul lato destro (x negativa) piu' la copia specchiata sul lato sinistro, nell'osso simmetrico.
        La copia riusa le UV della sorgente specchiate (uv_size negativo), come il flag mirror di Minecraft."""
        if hi[0] > EPS:
            raise ValueError(f'sym {name}: la sorgente va scritta sul lato destro (x <= 0)')
        right = self.box(bone, name + '_r', lo, hi, mat, **opts)
        left = self.box(mirror_name(bone), name + '_l', (-hi[0], lo[1], lo[2]), (-lo[0], hi[1], hi[2]), mat, **opts)
        for a, b in zip(right, left):
            b.mirror_of = a
            a.mirrors.append(b)
        return right, left

    # --- geometria di supporto -----------------------------------------------------------------------------
    @staticmethod
    def _inside(p, lo, hi):
        return all(lo[k] + EPS < p[k] < hi[k] - EPS for k in range(3))

    @staticmethod
    def _face_samples(box, face, offset):
        """Centri dei texel della faccia, spostati di `offset` lungo la normale."""
        tl, ua, va, fw, fh, n = face_frame(face, box.lo, box.hi)
        tw, th = texels(fw), texels(fh)
        pts = []
        for j in range(th):
            for i in range(tw):
                s, t = (i + 0.5) * fw / tw, (j + 0.5) * fh / th
                pts.append(tuple(tl[k] + s * ua[k] + t * va[k] + offset * n[k] for k in range(3)))
        return pts

    def _together(self, a, b):
        """True se le due scatole possono essere visibili nello stesso momento."""
        sa, sb = a.bone.state, b.bone.state
        return sa is None or sb is None or sa == sb

    def _buried(self, box, face):
        """True se la faccia e' interamente dentro altre scatole dello stesso osso (non si vede mai)."""
        others = [c for c in self.boxes if c is not box and c.bone is box.bone]
        return all(any(self._inside(p, c.lo, c.hi) for c in others) for p in self._face_samples(box, face, 0.01))

    # --- impacchettamento ----------------------------------------------------------------------------------
    def pack(self):
        """Assegna a ogni faccia visibile un rettangolo nella regione dell'osso. Ordine stabile: altezza
        decrescente, larghezza decrescente, poi ordine di dichiarazione (scaffali da sinistra a destra)."""
        for b in self.boxes:
            b.faces = {}
        rects = {name: [] for name, _ in ATLAS_REGIONS}
        seq = 0
        for b in self.boxes:
            if b.mirror_of is not None:
                continue
            for f in FACES:
                if f in b.opts.get('hide', ()):
                    continue
                swap = {'east': 'west', 'west': 'east'}
                if self._buried(b, f) and all(self._buried(m, swap.get(f, f)) for m in b.mirrors):
                    continue
                _, _, _, fw, fh, _ = face_frame(f, b.lo, b.hi)
                rects[b.bone.region].append((texels(fh), texels(fw), seq, b, f))
                seq += 1
        self.used = {}
        for name, (rx, ry, rw, rh) in ATLAS_REGIONS:
            x = y = shelf = 0
            for th, tw, _, b, f in sorted(rects[name], key=lambda r: (-r[0], -r[1], r[2])):
                if tw > rw:
                    raise ValueError(f'regione {name}: la faccia {f} di {b.name} e\' larga {tw} texel, '
                                     f'la regione {rw}')
                if x + tw > rw:
                    y += shelf
                    x = shelf = 0
                b.faces[f] = (rx + x, ry + y, tw, th)
                x += tw
                shelf = max(shelf, th)
            used = y + shelf
            if used > rh:
                raise ValueError(f'regione {name} piena: servono {used} righe, ce ne sono {rh}')
            self.used[name] = used
        for b in self.boxes:
            if b.mirror_of is not None:
                swap = {'east': 'west', 'west': 'east'}
                b.faces = {swap.get(f, f): r for f, r in b.mirror_of.faces.items()}

    # --- controlli -----------------------------------------------------------------------------------------
    def check(self):
        """Restituisce l'elenco dei problemi (vuoto se il modello e' a posto)."""
        problems = []
        lift = (0.0, PILOT_LIFT_PX, 0.0)
        for b in self.boxes:
            bmin, bmax = (FRAME_MIN, FRAME_MAX) if b.bone.region == 'frame' else (SUIT_MIN, SUIT_MAX)
            for k in range(3):
                if b.lo[k] < bmin[k] - EPS or b.hi[k] > bmax[k] + EPS:
                    problems.append(f'{b.name}: fuori dall\'ingombro {bmin} -> {bmax}: {b.lo} -> {b.hi}')
                    break
            # come nel gioco: la scatola scalata di SUIT_SCALE attorno ai piedi, il pilota a grandezza normale
            # sollevato di PILOT_LIFT_PX, senza gambe
            lo = [v * SUIT_SCALE for v in b.lo]
            hi = [v * SUIT_SCALE for v in b.hi]
            for pname, plo, phi, gap in PILOT:
                ov = [min(hi[k], phi[k] + lift[k] + gap) - max(lo[k], plo[k] + lift[k] - gap) for k in range(3)]
                if all(o > EPS for o in ov):
                    problems.append(f'{b.name}: a meno di {gap} px dal pilota ({pname})')
        # facce complanari con la stessa normale che si sovrappongono (sfarfallio), in tutte e sei le direzioni
        for i, a in enumerate(self.boxes):
            for b in self.boxes[i + 1:]:
                if not self._together(a, b):
                    continue
                for f in FACES:
                    if f not in a.faces or f not in b.faces:
                        continue
                    _, _, _, _, _, n = face_frame(f, a.lo, a.hi)
                    ax = [k for k in range(3) if n[k]][0]
                    side = a.hi if n[ax] > 0 else a.lo
                    other = b.hi if n[ax] > 0 else b.lo
                    if abs(side[ax] - other[ax]) > EPS:
                        continue
                    o = [k for k in range(3) if k != ax]
                    if any(min(a.hi[k], b.hi[k]) - max(a.lo[k], b.lo[k]) <= EPS for k in o):
                        continue
                    # parte comune visibile? Un campione al centro di ogni cella di GRID px, appena fuori dal piano:
                    # tutti gli spigoli stanno sulla griglia dei quarti di pixel, quindi ogni cella e' o tutta coperta
                    # da un'altra scatola o tutta scoperta, e il controllo e' esatto (anche per strisce di 0.25 px)
                    lo = [max(a.lo[k], b.lo[k]) for k in range(3)]
                    hi = [min(a.hi[k], b.hi[k]) for k in range(3)]
                    p = [0.0, 0.0, 0.0]
                    p[ax] = side[ax] + 0.01 * n[ax]
                    occ = [c for c in self.boxes if c is not a and c is not b and self._together(c, a)
                           and self._together(c, b) and c.lo[ax] + EPS < p[ax] < c.hi[ax] - EPS
                           and all(c.lo[k] < hi[k] - EPS and c.hi[k] > lo[k] + EPS for k in o)]
                    steps = [int(round((hi[k] - lo[k]) / GRID)) for k in o]
                    visible = False
                    for i0 in range(steps[0]):
                        p[o[0]] = lo[o[0]] + (i0 + 0.5) * GRID
                        for i1 in range(steps[1]):
                            p[o[1]] = lo[o[1]] + (i1 + 0.5) * GRID
                            if not any(self._inside(p, c.lo, c.hi) for c in occ):
                                visible = True
                                break
                        if visible:
                            break
                    if visible:
                        problems.append(f'facce complanari {a.name} / {b.name} ({f}, piano {side[ax]})')
        return problems

    def bbox(self):
        lo = tuple(min(b.lo[k] for b in self.boxes) for k in range(3))
        hi = tuple(max(b.hi[k] for b in self.boxes) for k in range(3))
        return lo, hi

    # --- esportazione --------------------------------------------------------------------------------------
    def to_geo(self):
        bones = []
        for top, pivot in TOP_BONES:
            bones.append({'name': top, 'pivot': list(pivot)})
            for bone in self.bones:
                if bone.parent != top:
                    continue
                cubes = []
                for b in self.boxes:
                    if b.bone is not bone:
                        continue
                    uv = {}
                    for f in FACES:
                        if f not in b.faces:
                            continue
                        u, v, tw, th = b.faces[f]
                        if b.mirror_of is not None:
                            uv[f] = {'uv': [u + tw, v], 'uv_size': [-tw, th]}
                        else:
                            uv[f] = {'uv': [u, v], 'uv_size': [tw, th]}
                    cubes.append({'origin': list(b.lo), 'size': list(b.size), 'uv': uv})
                bones.append({'name': bone.name, 'parent': top, 'pivot': list(pivot), 'cubes': cubes})
        return {
            'format_version': '1.12.0',
            'minecraft:geometry': [{
                'description': {
                    'identifier': 'geometry.' + self.identifier,
                    'texture_width': ATLAS_SIZE[0],
                    'texture_height': ATLAS_SIZE[1],
                    'visible_bounds_width': 3,
                    'visible_bounds_height': 3,
                    'visible_bounds_offset': [0, 1.25, 0],
                },
                'bones': bones,
            }],
        }


# --- scrittura deterministica ------------------------------------------------------------------------------
def _num(v):
    if isinstance(v, bool) or not isinstance(v, (int, float)):
        return v
    f = float(v)
    if abs(f - round(f)) < 1e-9:
        return int(round(f))
    return round(f, 4)


def _norm(o):
    if isinstance(o, dict):
        return {k: _norm(v) for k, v in o.items()}
    if isinstance(o, (list, tuple)):
        return [_norm(v) for v in o]
    return _num(o)


def dumps_json(obj, width=120):
    """JSON con chiavi nell'ordine d'inserimento, numeri interi quando possibile, oggetti e liste su una riga se
    ci stanno in `width` colonne. Stesso oggetto, stessi byte."""
    obj = _norm(obj)

    def inline(o):
        return json.dumps(o, ensure_ascii=True, separators=(', ', ': '))

    def fmt(o, ind):
        flat = inline(o)
        if not isinstance(o, (dict, list)) or len(flat) + ind <= width or not o:
            return flat
        pad = ' ' * (ind + 2)
        if isinstance(o, dict):
            items = [pad + json.dumps(k) + ': ' + fmt(v, ind + 2) for k, v in o.items()]
            return '{\n' + ',\n'.join(items) + '\n' + ' ' * ind + '}'
        items = [pad + fmt(v, ind + 2) for v in o]
        return '[\n' + ',\n'.join(items) + '\n' + ' ' * ind + ']'

    return fmt(obj, 0) + '\n'


def write_png(path, rgba):
    """Scrive una PNG RGBA 8 bit senza metadati (solo IHDR, IDAT, IEND), filtro 0, zlib livello 9.
    rgba: array numpy (altezza, larghezza, 4) uint8."""
    h, w, _ = rgba.shape
    raw = b''.join(b'\x00' + rgba[y].tobytes() for y in range(h))

    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = (b'\x89PNG\r\n\x1a\n'
           + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
           + chunk(b'IDAT', zlib.compress(raw, 9))
           + chunk(b'IEND', b''))
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, 'wb') as fh:
        fh.write(png)


def write_text(path, text):
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as fh:
        fh.write(text)
