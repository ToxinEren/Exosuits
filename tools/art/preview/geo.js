// Costruisce i triangoli di un .geo.json (Bedrock 1.12.0) con la stessa matematica di GeckoLib 4.x
// (BakedModelFactory.Builtin, VertexSet, GeoQuad.build, RenderUtils): x negata, perni e rotazioni delle ossa e delle
// scatole, UV per faccia o box UV, uv_size negativo, flag mirror, inflate, uv_rotation.
// Le coordinate prodotte sono quelle interne di GeckoLib (in pixel): destrorse, fronte verso -z, il lato destro di
// chi indossa la tuta a x positiva. Funziona sia in Node sia nel browser (nessuna dipendenza).

const DIRS = ['west', 'east', 'north', 'south', 'up', 'down'];
// Direction.step() di Minecraft
const STEP = { west: [-1, 0, 0], east: [1, 0, 0], north: [0, 0, -1], south: [0, 0, 1], up: [0, 1, 0], down: [0, -1, 0] };

// matrici 4x4 per righe
function ident() { return [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]; }
function mul(a, b) {
  const r = new Array(16).fill(0);
  for (let i = 0; i < 4; i++) for (let j = 0; j < 4; j++) for (let k = 0; k < 4; k++) r[i * 4 + j] += a[i * 4 + k] * b[k * 4 + j];
  return r;
}
function trans(x, y, z) { return [1, 0, 0, x, 0, 1, 0, y, 0, 0, 1, z, 0, 0, 0, 1]; }
function rotX(a) { const c = Math.cos(a), s = Math.sin(a); return [1, 0, 0, 0, 0, c, -s, 0, 0, s, c, 0, 0, 0, 0, 1]; }
function rotY(a) { const c = Math.cos(a), s = Math.sin(a); return [c, 0, s, 0, 0, 1, 0, 0, -s, 0, c, 0, 0, 0, 0, 1]; }
function rotZ(a) { const c = Math.cos(a), s = Math.sin(a); return [c, -s, 0, 0, s, c, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]; }
function apply(m, p) {
  return [m[0] * p[0] + m[1] * p[1] + m[2] * p[2] + m[3], m[4] * p[0] + m[5] * p[1] + m[6] * p[2] + m[7],
    m[8] * p[0] + m[9] * p[1] + m[10] * p[2] + m[11]];
}
function applyDir(m, p) {
  return [m[0] * p[0] + m[1] * p[1] + m[2] * p[2], m[4] * p[0] + m[5] * p[1] + m[6] * p[2], m[8] * p[0] + m[9] * p[1] + m[10] * p[2]];
}
const rad = d => d * Math.PI / 180;

// translateToPivot, rotate Z poi Y poi X (RenderUtils.rotateMatrixAroundBone/Cube), translateAwayFromPivot
function pivotRotation(pivot, rot) {
  let m = trans(pivot[0], pivot[1], pivot[2]);
  m = mul(m, rotZ(rot[2]));
  m = mul(m, rotY(rot[1]));
  m = mul(m, rotX(rot[0]));
  return mul(m, trans(-pivot[0], -pivot[1], -pivot[2]));
}

function vertexSet(origin, size, inf) {
  const [x0, y0, z0] = [origin[0] - inf, origin[1] - inf, origin[2] - inf];
  const [x1, y1, z1] = [origin[0] + size[0] + inf, origin[1] + size[1] + inf, origin[2] + size[2] + inf];
  const v = {
    bottomLeftBack: [x0, y0, z0], bottomRightBack: [x0, y0, z1], topLeftBack: [x0, y1, z0], topRightBack: [x0, y1, z1],
    topLeftFront: [x1, y1, z0], topRightFront: [x1, y1, z1], bottomLeftFront: [x1, y0, z0], bottomRightFront: [x1, y0, z1],
  };
  return {
    west: [v.topRightBack, v.topLeftBack, v.bottomLeftBack, v.bottomRightBack],
    east: [v.topLeftFront, v.topRightFront, v.bottomRightFront, v.bottomLeftFront],
    north: [v.topLeftBack, v.topLeftFront, v.bottomLeftFront, v.bottomLeftBack],
    south: [v.topRightFront, v.topRightBack, v.bottomRightBack, v.bottomRightFront],
    up: [v.topRightBack, v.topRightFront, v.topLeftFront, v.topLeftBack],
    down: [v.bottomLeftBack, v.bottomLeftFront, v.bottomRightFront, v.bottomRightBack],
  };
}

function verticesForQuad(vs, dir, boxUv, mirror) {
  switch (dir) {
    case 'west': return mirror ? vs.east : vs.west;
    case 'east': return mirror ? vs.west : vs.east;
    case 'up': return mirror && !boxUv ? vs.down : vs.up;
    case 'down': return mirror && !boxUv ? vs.up : vs.down;
    default: return vs[dir];
  }
}

const UV_ROT = { 0: 'NONE', 90: 'CLOCKWISE_90', 180: 'CLOCKWISE_180', 270: 'CLOCKWISE_270' };
function rotateUvs(rot, u, v, uw, vh) {
  switch (rot) {
    case 'CLOCKWISE_90': return [uw, v, uw, vh, u, vh, u, v];
    case 'CLOCKWISE_180': return [uw, vh, u, vh, u, v, uw, v];
    case 'CLOCKWISE_270': return [u, vh, u, v, uw, v, uw, vh];
    default: return [u, v, uw, v, uw, vh, u, vh];
  }
}

// GeoQuad.build (UV in pixel dell'atlante, non normalizzati)
function buildQuad(verts, uv, uvSize, uvRot, mirror, dir) {
  let u = uv[0], v = uv[1];
  let uWidth = u + uvSize[0];
  const vHeight = v + uvSize[1];
  const normal = STEP[dir].slice();
  if (!mirror) { const t = uWidth; uWidth = u; u = t; } else { normal[0] *= -1; }
  const uvs = rotateUvs(uvRot, u, v, uWidth, vHeight);
  return { verts, uvs: [[uvs[0], uvs[1]], [uvs[2], uvs[3]], [uvs[4], uvs[5]], [uvs[6], uvs[7]]], normal, dir };
}

function boxUvFace(dir, uv, size) {
  const [sx, sy, sz] = size.map(Math.floor);
  switch (dir) {
    case 'west': return [[uv[0] + sz + sx, uv[1] + sz], [sz, sy]];
    case 'east': return [[uv[0], uv[1] + sz], [sz, sy]];
    case 'north': return [[uv[0] + sz, uv[1] + sz], [sx, sy]];
    case 'south': return [[uv[0] + sz + sx + sz, uv[1] + sz], [sx, sy]];
    case 'up': return [[uv[0] + sz, uv[1]], [sx, sz]];
    case 'down': return [[uv[0] + sz + sx, uv[1] + sz], [sx, -sz]];
  }
}

// Restituisce {width, height, quads: [{bone, verts:[4x[x,y,z]], uvs:[4x[u,v]], normal}]} in coordinate interne.
// pose: {boneName: [rx, ry, rz] in gradi, come una rotazione d'animazione aggiunta a quella del file}.
export function bakeGeo(json, pose = {}) {
  const geo = json['minecraft:geometry'][0];
  const W = geo.description.texture_width, H = geo.description.texture_height;
  const bones = geo.bones;
  const byName = Object.fromEntries(bones.map(b => [b.name, b]));
  const world = {};
  function boneMatrix(b) {
    if (world[b.name]) return world[b.name];
    const p = b.pivot || [0, 0, 0];
    const r = (b.rotation || [0, 0, 0]).map((v, i) => v + ((pose[b.name] || [0, 0, 0])[i]));
    const local = pivotRotation([-p[0], p[1], p[2]], [rad(-r[0]), rad(-r[1]), rad(r[2])]);
    const m = b.parent ? mul(boneMatrix(byName[b.parent]), local) : local;
    world[b.name] = m;
    return m;
  }
  const quads = [];
  for (const b of bones) {
    const bm = boneMatrix(b);
    for (const c of b.cubes || []) {
      const size = c.size, o = c.origin;
      const mirror = c.mirror === true || (c.mirror === undefined && b.mirror === true);
      const inflate = c.inflate !== undefined ? c.inflate : (b.inflate || 0);
      const origin = [-(o[0] + size[0]), o[1], o[2]];
      const vs = vertexSet(origin, size, inflate);
      const cp = c.pivot || [0, 0, 0], cr = c.rotation || [0, 0, 0];
      const cm = mul(bm, pivotRotation([-cp[0], cp[1], cp[2]], [rad(-cr[0]), rad(-cr[1]), rad(cr[2])]));
      const boxUv = Array.isArray(c.uv);
      for (const dir of DIRS) {
        let q;
        if (!boxUv) {
          const f = c.uv && c.uv[dir];
          if (!f) continue;
          q = buildQuad(verticesForQuad(vs, dir, false, mirror), f.uv, f.uv_size, UV_ROT[f.uv_rotation || 0], mirror, dir);
        } else {
          const [uv, us] = boxUvFace(dir, c.uv, size);
          q = buildQuad(verticesForQuad(vs, dir, true, mirror), uv, us, 'NONE', mirror, dir);
        }
        quads.push({ bone: b.name, verts: q.verts.map(p => apply(cm, p)), uvs: q.uvs, normal: applyDir(cm, q.normal) });
      }
    }
  }
  return { width: W, height: H, quads };
}
