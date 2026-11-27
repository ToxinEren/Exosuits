// Anteprime della stazione di manutenzione per il proprietario (parte 4d; locali, non nella CI): Chromium senza
// schermo + three.js con scene.html, come shot.js.
//
//   node station.js                              tutte le direzioni, nella cartella delle anteprime del design M4
//   node station.js --directions officina,rame   solo alcune
//   node station.js --out <dir>                  le immagini finite in <dir>
//   node station.js --check 1 --vanilla <dir>    controllo del visore: la stazione segnaposto di oggi (blockstate,
//                                                modelli e texture del mod; i genitori orientable, cube e block del
//                                                gioco base da <dir>/assets/minecraft/models, estratti dal jar del
//                                                client, fuori dal repository) in out/stazione/controllo.png, da
//                                                confrontare con docs/test-in-gioco/m4a1-screenshot/*-stazione-posata.png
//
// Per ogni direzione `python3 tools/art/station.py --direction <nome> --out out/stazione/gen/<nome>` scrive i file
// che il gioco leggerebbe; le viste si disegnano da quei file (modelli JSON e texture), con il blockstate del mod
// (src/main/resources/.../blockstates/station.json) e le due meta' dove le posa il gioco: la principale nel blocco
// (0, 0, 0) con facing=north (il fronte verso -z, dove sta chi l'ha posata, rivolto a sud) e la laterale nel blocco a
// ovest, cioe' alla destra di chi guarda il fronte (StationBlock.otherHalf). Il telaio sta sopra la principale, i
// piedi al centro del blocco sopra di essa (ExoframeItem: Vec3.atBottomCenterOf del blocco cliccato) e il fronte verso
// chi l'ha posato (a nord): e' la tuta vera (geo, atlante e colori predefiniti del Rottame, stato 0), senza lo strato
// luminoso che il gioco non ha ancora e senza pilota (un telaio piazzato e' vuoto). Luce dei blocchi come nel mondo
// (blockmodel.js), texel senza filtri, senza antialiasing.
//
// Scrive, per ogni direzione <L>-<nome> (L la lettera): <L>-<nome>-mondo.png (davanti e dietro a tre quarti, con il
// telaio sopra e da vicino senza), <L>-<nome>-texture.png (icona x8, x2, x1 e le texture x8, con i nomi dei file);
// poi confronto.png (le direzioni affiancate). Ogni PNG scritta deve avere solo IHDR, IDAT, IEND.
const http = require('http');
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
let chromium;
try {
  ({ chromium } = require('playwright'));
} catch (e) {
  console.error('Playwright non trovato: serve per le anteprime. Fuori dalla sessione cloud installarlo con\n'
    + '  npm install playwright && npx playwright install chromium\n(in tools/art/preview). Dettaglio: ' + e.message);
  process.exit(1);
}

const HERE = __dirname;
const REPO = path.join(HERE, '..', '..', '..');
const ASSETS = path.join(REPO, 'src', 'main', 'resources', 'assets', 'redstone_exoframes');
const args = { directions: '', out: path.join(REPO, 'docs', 'superpowers', 'specs', 'anteprime', '2026-11-27-m4d'),
  vanilla: '', check: '' };
for (let i = 2; i < process.argv.length; i += 2) {
  const k = process.argv[i].replace(/^--/, '');
  if (!(k in args) || process.argv[i + 1] === undefined) { console.error('opzione sconosciuta o senza valore:', process.argv[i]); process.exit(2); }
  args[k] = process.argv[i + 1];
}
const WORK = path.join(HERE, 'out', 'stazione');

function suitScale() {
  const src = fs.readFileSync(path.join(HERE, '..', 'exo.py'), 'utf8');
  const m = src.match(/^SUIT_SCALE = ([0-9]+(?:\.[0-9]+)?)$/m);
  if (!m) { console.error('SUIT_SCALE non trovato in tools/art/exo.py'); process.exit(2); }
  return Number(m[1]);
}
const S = suitScale();
const BLOCK = 16 / 0.9375;     // px della scena per blocco: la misura della tuta
const SKY = 0xa7bed6;
const PAGE_BG = '#1d1f23';

// --- le direzioni: generatore ----------------------------------------------------------------------------------
function directions() {
  const listed = execFileSync('python3', [path.join(HERE, '..', 'station.py'), '--list'], { encoding: 'utf8' })
    .trim().split('\n').map(line => {
      const m = line.match(/^([a-z]+): ([A-Z]), ([^:]+): (.*)$/);
      if (!m) throw new Error('riga di --list inattesa: ' + line);
      return { name: m[1], letter: m[2], title: m[3], idea: m[4] };
    });
  if (!args.directions) return listed;
  return args.directions.split(',').map(n => {
    const d = listed.find(x => x.name === n);
    if (!d) { console.error('direzione sconosciuta:', n); process.exit(2); }
    return d;
  });
}

// --- viste --------------------------------------------------------------------------------------------------------
function viewCfg(dirName, view) {
  const withSuit = !view.endsWith('-vicino');
  const back = view.startsWith('dietro');
  const cfg = {
    blocks: [
      { state: 'model/blockstates/station.json', variant: 'facing=north,part=main', pos: [0, 0, 0] },
      { state: 'model/blockstates/station.json', variant: 'facing=north,part=side', pos: [-1, 0, 0] },
    ],
    assets: 'work/gen/' + dirName,
    floor: { block: BLOCK, from: -4 * BLOCK, to: 5 * BLOCK, zfrom: -4 * BLOCK, zto: 5 * BLOCK, color: 0x8c8f86 },
    antialias: false, background: SKY, fit: 'auto',
    dir: back ? [-0.62, withSuit ? 0.32 : 0.5, 0.78] : [0.62, withSuit ? 0.32 : 0.5, -0.78],
    pad: withSuit ? 1.06 : 1.12,
  };
  if (withSuit) {
    cfg.models = [{ geo: 'model/geo/exoframe.geo.json', tex: 'model/textures/entity/exoframe.png', S,
      dx: 0.5 * BLOCK, dy: BLOCK, dz: 0.5 * BLOCK, state: 0 }];
  }
  return cfg;
}
const VIEWS = { davanti: [700, 900], dietro: [700, 900], 'davanti-vicino': [700, 460], 'dietro-vicino': [700, 460] };

// --- fogli composti -------------------------------------------------------------------------------------------------
const esc = s => s.replace(/&/g, '&amp;').replace(/</g, '&lt;');
const CSS = `body{margin:0;background:${PAGE_BG};color:#e8e6e1;font-family:'DejaVu Sans','Liberation Sans',sans-serif}
 .sheet{display:inline-block;padding:18px 20px 20px} h1{font-size:26px;margin:0 0 4px;font-weight:bold}
 h2{font-size:15px;margin:0 0 14px;font-weight:normal;color:#c9c6bf} .row{display:flex;gap:16px;align-items:flex-start}
 .cell{display:flex;flex-direction:column;gap:6px} .lab{font-size:15px;color:#dedbd4} .small{font-size:13px;color:#aaa69e}
 img{display:block} .px{image-rendering:pixelated} .slot{background:#8b8b8b;border:2px solid;border-color:#373737 #fff #fff #373737;
 display:flex;align-items:center;justify-content:center} .tex{display:flex;flex-direction:column;gap:4px}
 .pair{display:flex;gap:0}`;

function page(body) {
  return `<!doctype html><html><head><meta charset="utf-8"><style>${CSS}</style></head><body><div class="sheet" id="sheet">${body}</div></body></html>`;
}

function worldSheet(d) {
  const img = (v, lab) => `<div class="cell"><img src="/work/render/${d.name}-${v}.png"><div class="lab">${esc(lab)}</div></div>`;
  return page(`<h1>${d.letter} · ${esc(d.title)}</h1><h2>${esc(d.idea)}</h2>
    <div class="row">${img('davanti', 'davanti, a tre quarti, con il telaio sopra')}${img('dietro', 'dietro, a tre quarti, con il telaio sopra')}</div>
    <div class="row" style="margin-top:14px">${img('davanti-vicino', 'davanti, da vicino')}${img('dietro-vicino', 'dietro, da vicino')}</div>
    <div class="small" style="margin-top:10px">Disegnata dai file del generatore (modelli e texture), luce delle facce come nel gioco; a sinistra della stazione vista da davanti la metà principale.</div>`);
}

function textureSheet(d) {
  const base = `/work/gen/${d.name}/textures`;
  const t = (name, k = 8, transform = '') => `<img class="px" src="${base}/block/${name}.png" style="width:${16 * k}px;height:${16 * k}px${transform ? ';transform:' + transform : ''}">`;
  const tex = (inner, lab) => `<div class="tex">${inner}<div class="small">${lab}</div></div>`;
  const icon = k => `<div class="slot" style="width:${16 * k + 8}px;height:${16 * k + 8}px"><img class="px" src="${base}/item/station.png" style="width:${16 * k}px;height:${16 * k}px"></div>`;
  return page(`<h1>${d.letter} · ${esc(d.title)}: icona e texture</h1><h2>texture 16×16 ingrandite ×8, senza filtri; sotto ogni gruppo i file</h2>
    <div class="row"><div class="cell">${icon(8)}<div class="lab">icona ×8</div></div>
      <div class="cell">${icon(2)}<div class="lab">×2</div></div><div class="cell">${icon(1)}<div class="lab">×1</div></div></div>
    <div class="row" style="margin-top:18px">
      ${tex(`<div class="pair">${t('station_front_main')}${t('station_front_side')}</div>`, 'fronte: station_front_main, station_front_side')}
      ${tex(`<div class="pair">${t('station_back_side')}${t('station_back_main')}</div>`, 'retro, visto da dietro: station_back_side, station_back_main')}</div>
    <div class="row" style="margin-top:14px">
      ${tex(`<div class="pair">${t('station_top', 8, 'rotate(180deg)')}${t('station_top', 8, 'scaleY(-1)')}</div>`, 'sopra, visto da davanti: station_top (sulla laterale specchiata)')}
      ${tex(t('station_side'), 'estremi: station_side')}${tex(t('station_bottom'), 'sotto: station_bottom')}</div>`);
}

function comparisonSheet(ds) {
  const col = d => `<div class="cell" style="width:700px"><h1>${d.letter} · ${esc(d.title)}</h1><div class="small" style="min-height:38px">${esc(d.idea)}</div>
    <img src="/work/render/${d.name}-davanti.png"><img src="/work/render/${d.name}-davanti-vicino.png"><img src="/work/render/${d.name}-dietro-vicino.png">
    <div class="row" style="align-items:center"><div class="slot" style="width:72px;height:72px"><img class="px" src="/work/gen/${d.name}/textures/item/station.png" style="width:64px;height:64px"></div><div class="small">icona ×4</div></div></div>`;
  return page(`<h1>Stazione di manutenzione: tre direzioni</h1><h2>davanti a tre quarti con il telaio sopra; sotto, la stazione da vicino, davanti e dietro; luce delle facce come nel gioco, il blocco non fa luce</h2>
    <div class="row">${ds.map(col).join('')}</div>`);
}

// --- server e screenshot ----------------------------------------------------------------------------------------
const MOUNTS = { '/preview/': HERE, '/model/': ASSETS, '/work/': WORK };
if (args.vanilla) MOUNTS['/vanilla/'] = path.resolve(args.vanilla);
const TYPES = { '.html': 'text/html', '.js': 'text/javascript', '.json': 'application/json', '.png': 'image/png' };
const PAGES = new Map();
const server = http.createServer((req, res) => {
  const url = decodeURIComponent(req.url.split('?')[0]);
  if (PAGES.has(url)) { res.writeHead(200, { 'Content-Type': 'text/html' }); return res.end(PAGES.get(url)); }
  const prefix = Object.keys(MOUNTS).find(m => url.startsWith(m));
  const root = prefix && MOUNTS[prefix];
  const p = root && path.resolve(root, '.' + url.slice(prefix.length - 1));
  if (!p || !(p === root || p.startsWith(root + path.sep)) || !fs.existsSync(p) || fs.statSync(p).isDirectory()) {
    res.writeHead(404); return res.end();
  }
  res.writeHead(200, { 'Content-Type': TYPES[path.extname(p)] || 'application/octet-stream', 'Cache-Control': 'no-store' });
  fs.createReadStream(p).pipe(res);
});

function pngChunks(file) {
  const d = fs.readFileSync(file);
  const kinds = [];
  for (let pos = 8; pos + 8 <= d.length; pos += 12 + d.readUInt32BE(pos)) kinds.push(d.toString('latin1', pos + 4, pos + 8));
  return kinds;
}

let failed = false;
async function shoot(browser, url, file, size, element) {
  const ctx = await browser.newContext({ viewport: { width: size[0], height: size[1] }, deviceScaleFactor: 1 });
  const page = await ctx.newPage();
  page.on('console', m => { if (m.type() === 'error') { console.error(path.basename(file), m.text()); failed = true; } });
  page.on('pageerror', e => { console.error(path.basename(file), e.message); failed = true; });
  await page.goto(url);
  if (element) {
    await page.waitForLoadState('networkidle');
    await page.evaluate(() => Promise.all([...document.images].map(i => i.decode())));
    await page.locator(element).screenshot({ path: file });
  } else {
    await page.waitForFunction('window.__ready === true', null, { timeout: 120000 });
    await page.screenshot({ path: file });
  }
  await ctx.close();
  const extra = pngChunks(file).filter(k => !['IHDR', 'IDAT', 'IEND'].includes(k));
  if (extra.length) { console.error(file, 'blocchi PNG oltre a IHDR, IDAT, IEND:', extra.join(', ')); failed = true; }
  console.log('scritto', path.relative(process.cwd(), file));
}

(async () => {
  await new Promise(r => server.listen(0, '127.0.0.1', r));
  const port = server.address().port;
  const base = `http://127.0.0.1:${port}`;
  const browser = await chromium.launch({ args: ['--use-gl=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
  fs.mkdirSync(path.join(WORK, 'render'), { recursive: true });
  if (args.check) {
    // la stazione segnaposto di oggi, dai file del mod, per confrontarla con uno scatto del client vero
    const cfg = viewCfg('-', 'davanti-vicino');
    Object.assign(cfg, { assets: 'model', vanilla: 'vanilla/assets/minecraft', dir: [-0.05, 0.75, -1] });
    await shoot(browser, `${base}/preview/scene.html?cfg=${encodeURIComponent(JSON.stringify(cfg))}`,
      path.join(WORK, 'controllo.png'), [960, 540]);
  } else {
    const ds = directions();
    fs.mkdirSync(args.out, { recursive: true });
    for (const d of ds) {
      execFileSync('python3', [path.join(HERE, '..', 'station.py'), '--direction', d.name, '--out', path.join(WORK, 'gen', d.name)], { stdio: 'inherit' });
      for (const [view, size] of Object.entries(VIEWS)) {
        const cfg = viewCfg(d.name, view);
        await shoot(browser, `${base}/preview/scene.html?cfg=${encodeURIComponent(JSON.stringify(cfg))}`,
          path.join(WORK, 'render', `${d.name}-${view}.png`), size);
      }
      const stem = `${d.letter.toLowerCase()}-${d.name}`;
      PAGES.set(`/sheet/${stem}-mondo.html`, worldSheet(d));
      PAGES.set(`/sheet/${stem}-texture.html`, textureSheet(d));
      await shoot(browser, `${base}/sheet/${stem}-mondo.html`, path.join(args.out, `${stem}-mondo.png`), [1600, 1600], '#sheet');
      await shoot(browser, `${base}/sheet/${stem}-texture.html`, path.join(args.out, `${stem}-texture.png`), [1200, 1000], '#sheet');
    }
    if (ds.length > 1) {
      PAGES.set('/sheet/confronto.html', comparisonSheet(ds));
      await shoot(browser, `${base}/sheet/confronto.html`, path.join(args.out, 'confronto.png'), [2400, 2400], '#sheet');
    }
  }
  await browser.close();
  server.close();
  if (failed) process.exit(1);
})().catch(e => { console.error(e); process.exit(1); });
