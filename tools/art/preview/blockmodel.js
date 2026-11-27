// Costruisce i quadrilateri di un modello di blocco del gioco base (JSON con "elements", 1.20.1 e 1.21.1) con la
// matematica di FaceBakery: posizioni dei quattro vertici di ogni faccia (FaceInfo), UV per vertice con la rotazione
// della faccia (BlockFaceUV.getU/getV), UV predefinite dalla posizione dell'elemento quando mancano
// (BlockElement.uvsByFace), rotazione y del blockstate (orario visto dall'alto: nord -> est) applicata alle posizioni,
// alla direzione della faccia e al suo cullface (uvlock spento). Le rotazioni degli elementi non servono ai modelli
// della mod e qui sono un errore. Funziona sia in Node sia nel browser (nessuna dipendenza).
//
// Luce come nel mondo, con la luce liscia spenta (ModelBlockRenderer.renderModelFaceFlat): il colore del texel
// (sRGB, senza conversioni) per l'ombra della direzione (ClientLevel.getShade sull'Overworld: sopra 1.0, nord e sud
// 0.8, est e ovest 0.6, sotto 0.5) per la luce del blocco accanto nella direzione della faccia: per un blocco con la
// forma di collisione piena (la stazione) il gioco la prende li' anche per le facce rientrate
// (ModelBlockRenderer.calculateShape, bit 0, letto con javap su 1.20.1 e 1.21.1); pieno giorno 1.0, dentro un blocco
// opaco (terra, l'altra meta') il valore della lightmap al livello 0, stimato in circa 0.1 dalla formula di
// LightTexture con la luminosita' predefinita (non misurato nel client).

export const SHADE = { up: 1.0, down: 0.5, north: 0.8, south: 0.8, west: 0.6, east: 0.6 };
export const LIGHT_DARK = 0.1;
export const STEP = { down: [0, -1, 0], up: [0, 1, 0], north: [0, 0, -1], south: [0, 0, 1], west: [-1, 0, 0], east: [1, 0, 0] };

const FACE_INFO = {
  down: [['x0', 'y0', 'z1'], ['x0', 'y0', 'z0'], ['x1', 'y0', 'z0'], ['x1', 'y0', 'z1']],
  up: [['x0', 'y1', 'z0'], ['x0', 'y1', 'z1'], ['x1', 'y1', 'z1'], ['x1', 'y1', 'z0']],
  north: [['x1', 'y1', 'z0'], ['x1', 'y0', 'z0'], ['x0', 'y0', 'z0'], ['x0', 'y1', 'z0']],
  south: [['x0', 'y1', 'z1'], ['x0', 'y0', 'z1'], ['x1', 'y0', 'z1'], ['x1', 'y1', 'z1']],
  west: [['x0', 'y1', 'z0'], ['x0', 'y0', 'z0'], ['x0', 'y0', 'z1'], ['x0', 'y1', 'z1']],
  east: [['x1', 'y1', 'z1'], ['x1', 'y0', 'z1'], ['x1', 'y0', 'z0'], ['x1', 'y1', 'z0']],
};

// BlockElement.uvsByFace
export function defaultUv(dir, f, t) {
  switch (dir) {
    case 'down': return [f[0], 16 - t[2], t[0], 16 - f[2]];
    case 'up': return [f[0], f[2], t[0], t[2]];
    case 'north': return [16 - t[0], 16 - t[1], 16 - f[0], 16 - f[1]];
    case 'south': return [f[0], 16 - t[1], t[0], 16 - f[1]];
    case 'west': return [f[2], 16 - t[1], t[2], 16 - f[1]];
    case 'east': return [16 - t[2], 16 - t[1], 16 - f[2], 16 - f[1]];
    default: throw new Error('direzione sconosciuta: ' + dir);
  }
}

// BlockFaceUV.getU/getV: con la rotazione R il vertice i prende l'angolo (i + R/90) % 4
export function uvAt(uv, rotation, i) {
  const s = (i + (rotation || 0) / 90) % 4;
  return [uv[s === 0 || s === 1 ? 0 : 2], uv[s === 0 || s === 3 ? 1 : 3]];
}

// rotazione y del blockstate attorno al centro del blocco, in passi di 90 gradi (orario visto dall'alto)
function rotatePos(p, deg) {
  let [x, y, z] = p;
  for (let k = 0; k < ((deg / 90) % 4 + 4) % 4; k++) [x, z] = [16 - z, x];
  return [x, y, z];
}
const CW = { north: 'east', east: 'south', south: 'west', west: 'north', up: 'up', down: 'down' };
export function rotateDir(dir, deg) {
  let d = dir;
  for (let k = 0; k < ((deg / 90) % 4 + 4) % 4; k++) d = CW[d];
  return d;
}

// variabile di texture ("#front") fino al nome "namespace:percorso"
export function resolveTexture(textures, ref) {
  let r = ref;
  for (let n = 0; n < 16 && r.startsWith('#'); n++) {
    const next = textures[r.slice(1)];
    if (next === undefined) throw new Error('texture non definita: ' + r);
    r = next;
  }
  if (r.startsWith('#')) throw new Error('texture circolare: ' + ref);
  return r.includes(':') ? r : 'minecraft:' + r;
}

// model: {textures, elements} gia' risolto (genitori uniti da chi chiama). Restituisce i quadrilateri in coordinate
// del modello (0..16) gia' ruotate: {verts[4], uvs[4] (0..16), texture, dir, cullface, boundary}
export function bakeBlock(model, rotY = 0) {
  if (!Array.isArray(model.elements)) throw new Error('modello senza elementi');
  const quads = [];
  for (const el of model.elements) {
    if (el.rotation) throw new Error('rotazione degli elementi non gestita');
    const f = el.from, t = el.to;
    const c = { x0: f[0] / 1, x1: t[0], y0: f[1], y1: t[1], z0: f[2], z1: t[2] };
    for (const [dir, face] of Object.entries(el.faces || {})) {
      const uv = face.uv || defaultUv(dir, f, t);
      const verts = [], uvs = [];
      for (let i = 0; i < 4; i++) {
        const v = FACE_INFO[dir][i].map(k => c[k]);
        verts.push(rotatePos(v, rotY));
        uvs.push(uvAt(uv, face.rotation, i));
      }
      const axis = { down: 1, up: 1, north: 2, south: 2, west: 0, east: 0 }[dir];
      const plane = { down: f[1], up: t[1], north: f[2], south: t[2], west: f[0], east: t[0] }[dir];
      quads.push({
        verts, uvs, texture: resolveTexture(model.textures || {}, face.texture),
        dir: rotateDir(dir, rotY), cullface: face.cullface ? rotateDir(face.cullface, rotY) : null,
        boundary: plane === 0 || plane === 16, axis,
      });
    }
  }
  return quads;
}
