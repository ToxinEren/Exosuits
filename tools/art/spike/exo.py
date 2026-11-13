"""Tiny box-model + atlas pipeline for the exoframe art spike.

Conventions: 1 unit = 1 texture px = 1/16 block, y up, model stands on y=0,
front of the suit faces -z (north). Bone numbers follow the Bedrock/GeckoLib
humanoid (right arm at -x).
"""
import json
import math

import numpy as np

BONE_PIVOTS = {
    'armorHead': (0, 24, 0), 'armorBody': (0, 24, 0),
    'armorRightArm': (-5, 22, 0), 'armorLeftArm': (5, 22, 0),
    'armorRightLeg': (-1.9, 12, 0), 'armorLeftLeg': (1.9, 12, 0),
}
PART = {'armorHead': 'head', 'armorBody': 'body', 'armorRightArm': 'rightArm',
        'armorLeftArm': 'leftArm', 'armorRightLeg': 'rightLeg', 'armorLeftLeg': 'leftLeg'}
MIRROR_BONE = {'armorHead': 'armorHead', 'armorBody': 'armorBody',
               'armorRightArm': 'armorLeftArm', 'armorLeftArm': 'armorRightArm',
               'armorRightLeg': 'armorLeftLeg', 'armorLeftLeg': 'armorRightLeg'}
FACES = ('north', 'south', 'east', 'west', 'up', 'down')
# reference player boxes (lo, hi)
PLAYER = [((-4, 24, -4), (4, 32, 4)), ((-4, 12, -2), (4, 24, 2)), ((-8, 12, -2), (-4, 24, 2)),
          ((4, 12, -2), (8, 24, 2)), ((-4, 0, -2), (0, 12, 2)), ((0, 0, -2), (4, 12, 2))]


def face_frame(face, lo, hi):
    """Return (top_left, u_axis, v_axis, width_px, height_px) of a face seen from outside."""
    x0, y0, z0 = lo
    x1, y1, z1 = hi
    w, h, d = x1 - x0, y1 - y0, z1 - z0
    if face == 'north':
        return (x1, y1, z0), (-1, 0, 0), (0, -1, 0), w, h
    if face == 'south':
        return (x0, y1, z1), (1, 0, 0), (0, -1, 0), w, h
    if face == 'east':
        return (x1, y1, z1), (0, 0, -1), (0, -1, 0), d, h
    if face == 'west':
        return (x0, y1, z0), (0, 0, 1), (0, -1, 0), d, h
    if face == 'up':
        return (x0, y1, z0), (1, 0, 0), (0, 0, 1), w, d
    if face == 'down':
        return (x0, y0, z1), (1, 0, 0), (0, 0, -1), w, d
    raise ValueError(face)


class Box:
    _n = 0

    def __init__(self, bone, group, lo, hi, mat, pivot=None, rot=None, opts=None, name=None):
        self.bone, self.group = bone, group
        self.lo = tuple(float(v) for v in lo)
        self.hi = tuple(float(v) for v in hi)
        size = [b - a for a, b in zip(self.lo, self.hi)]
        for s in size:
            if s <= 0 or abs(s - round(s)) > 1e-6:
                raise ValueError(f'box {name} {lo}-{hi}: size {size} must be positive integers')
        self.size = tuple(int(round(s)) for s in size)
        self.mat = mat if isinstance(mat, dict) else {'default': mat}
        self.pivot, self.rot = pivot, rot
        self.opts = dict(opts or {})
        self.mirror_of = None
        self.uv = {}
        Box._n += 1
        self.seed = Box._n * 7919
        self.name = name or f'{group}_{PART[bone]}_{Box._n}'

    def mat_for(self, face):
        return self.mat.get(face, self.mat['default'])


class Model:
    def __init__(self, name, atlas=(128, 128)):
        self.name = name
        self.boxes = []
        self.atlas_w, self.atlas_h = atlas

    # --- building helpers -------------------------------------------------
    def box(self, bone, group, lo, hi, mat, **kw):
        b = Box(bone, group, lo, hi, mat, **kw)
        self.boxes.append(b)
        return b

    def sym(self, bone, group, lo, hi, mat, **kw):
        """Add a box on the right side bone (or centre) and its mirror copy (shares UV)."""
        a = self.box(bone, group, lo, hi, mat, **kw)
        mb = MIRROR_BONE[bone]
        mlo = (-hi[0], lo[1], lo[2])
        mhi = (-lo[0], hi[1], hi[2])
        piv = None if kw.get('pivot') is None else (-kw['pivot'][0], kw['pivot'][1], kw['pivot'][2])
        rot = None if kw.get('rot') is None else (kw['rot'][0], -kw['rot'][1], -kw['rot'][2])
        kw2 = dict(kw, pivot=piv, rot=rot)
        if 'name' in kw2 and kw2['name']:
            kw2['name'] = kw2['name'] + '_m'
        b = Box(mb, group, mlo, mhi, mat, **kw2)
        b.mirror_of = a
        self.boxes.append(b)
        return a, b

    # --- UV packing --------------------------------------------------------
    def pack(self):
        rects = []
        for b in self.boxes:
            if b.mirror_of is not None:
                continue
            for f in FACES:
                if f in b.opts.get('hide', ()):
                    continue
                _, _, _, fw, fh = face_frame(f, b.lo, b.hi)
                rects.append((int(fw), int(fh), b, f))
        rects.sort(key=lambda r: (-r[1], -r[0]))
        W = self.atlas_w
        x = y = shelf_h = 0
        for fw, fh, b, f in rects:
            if x + fw > W:
                y += shelf_h
                x = shelf_h = 0
            b.uv[f] = (x, y, fw, fh)
            x += fw
            shelf_h = max(shelf_h, fh)
        used_h = y + shelf_h
        while self.atlas_h < used_h:
            self.atlas_h *= 2
        self.used_h = used_h
        for b in self.boxes:
            if b.mirror_of is not None:
                src = b.mirror_of.uv
                swap = {'east': 'west', 'west': 'east'}
                b.uv = {f: src[swap.get(f, f)] for f in FACES if swap.get(f, f) in src}
        return used_h

    def face_uv(self, b, f):
        """(u, v, w, h, flipped)"""
        if f not in b.uv:
            return None
        u, v, w, h = b.uv[f]
        return u, v, w, h, b.mirror_of is not None

    # --- checks ------------------------------------------------------------
    def check(self, groups=('frame', 'piece')):
        """Report coplanar overlapping faces with the same normal (z-fighting)."""
        issues = []
        bs = [b for b in self.boxes if b.rot is None and b.group in groups]
        for i, a in enumerate(bs):
            for b in bs[i + 1:]:
                for ax in range(3):
                    o = [k for k in range(3) if k != ax]
                    for side in (0, 1):
                        pa = a.hi[ax] if side else a.lo[ax]
                        pb = b.hi[ax] if side else b.lo[ax]
                        if abs(pa - pb) > 1e-6:
                            continue
                        ov = 1.0
                        for k in o:
                            ov *= max(0.0, min(a.hi[k], b.hi[k]) - max(a.lo[k], b.lo[k]))
                        if ov > 1e-6 and not (ax == 1 and side == 0):
                            # skip if the shared patch is buried inside some other box (or the player)
                            rng_ = [(max(a.lo[k], b.lo[k]), min(a.hi[k], b.hi[k])) for k in range(3)]
                            pts = []
                            for s in (0.15, 0.5, 0.85):
                                for t in (0.15, 0.5, 0.85):
                                    p = [0.0, 0.0, 0.0]
                                    p[ax] = pa + (0.01 if side else -0.01)
                                    p[o[0]] = rng_[o[0]][0] + s * (rng_[o[0]][1] - rng_[o[0]][0])
                                    p[o[1]] = rng_[o[1]][0] + t * (rng_[o[1]][1] - rng_[o[1]][0])
                                    pts.append(p)
                            occ = [(c.lo, c.hi) for c in bs if c is not a and c is not b] + PLAYER
                            if all(any(all(lo[k] < p[k] < hi[k] for k in range(3)) for lo, hi in occ) for p in pts):
                                continue
                            issues.append((a.name, b.name, 'xyz'[ax] + ('+' if side else '-'), pa))
        return issues

    def bbox(self, groups=('frame', 'piece')):
        lo = np.min([b.lo for b in self.boxes if b.group in groups], axis=0)
        hi = np.max([b.hi for b in self.boxes if b.group in groups], axis=0)
        return lo, hi

    # --- export ------------------------------------------------------------
    def to_json(self):
        out = {'name': self.name, 'atlas': [self.atlas_w, self.atlas_h], 'boxes': []}
        for b in self.boxes:
            faces = {}
            for f in FACES:
                fu = self.face_uv(b, f)
                if fu:
                    faces[f] = list(fu)
            out['boxes'].append({
                'name': b.name, 'bone': b.bone, 'group': b.group,
                'from': list(b.lo), 'to': list(b.hi),
                'pivot': list(b.pivot) if b.pivot else None,
                'rot': list(b.rot) if b.rot else None,
                'faces': faces})
        return out

    def to_geo(self, identifier):
        bones = []
        for bone, piv in BONE_PIVOTS.items():
            bones.append({'name': bone, 'pivot': list(piv)})
            for grp in ('frame', 'piece'):
                cubes = []
                for b in self.boxes:
                    if b.bone != bone or b.group != grp:
                        continue
                    c = {'origin': list(b.lo), 'size': list(b.size), 'uv': {}}
                    if b.rot:
                        c['pivot'] = list(b.pivot)
                        c['rotation'] = list(b.rot)
                    for f in FACES:
                        fu = self.face_uv(b, f)
                        if not fu:
                            continue
                        u, v, w, h, fl = fu
                        if fl:
                            c['uv'][f] = {'uv': [u + w, v], 'uv_size': [-w, h]}
                        else:
                            c['uv'][f] = {'uv': [u, v], 'uv_size': [w, h]}
                    cubes.append(c)
                bones.append({'name': f'{grp}_{PART[bone]}', 'parent': bone,
                              'pivot': list(piv), 'cubes': cubes})
        lo, hi = self.bbox()
        return {'format_version': '1.12.0', 'minecraft:geometry': [{
            'description': {'identifier': f'geometry.{identifier}',
                            'texture_width': self.atlas_w, 'texture_height': self.atlas_h,
                            'visible_bounds_width': 3, 'visible_bounds_height': 3,
                            'visible_bounds_offset': [0, 1, 0]},
            'bones': bones}]}

    def save(self, outdir, painter):
        import os
        os.makedirs(outdir, exist_ok=True)
        self.pack()
        atlas, glow, mask = painter(self)
        atlas.save(f'{outdir}/atlas.png')
        glow.save(f'{outdir}/glow.png')
        mask.save(f'{outdir}/tintmask.png')
        with open(f'{outdir}/model.json', 'w') as fh:
            json.dump(self.to_json(), fh)
        with open(f'{outdir}/exoframe_{self.name.lower()}.geo.json', 'w') as fh:
            json.dump(self.to_geo(f'exoframe_rottame_{self.name.lower()}'), fh, indent=1)
