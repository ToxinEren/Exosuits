// Anteprime in scala (locali, non nella CI): Chromium senza schermo + three.js con scene.html.
//
//   node shot.js                        viste standard del telaio dai file del mod, in out/<vista>.png
//   node shot.js --views fronte,porta   solo alcune viste standard
//   node shot.js --assets <dir>         modello scritto da `build.py --out <dir>` invece dei file del mod
//   node shot.js --jobs <file.json>     viste a piacere: [{name, cfg, width?, height?}] (cfg come in scene.html)
//
// Viste standard: fronte, retro, lato (3/4 e di lato), testa e gamba (primi piani), porta (davanti, con griglia a
// blocchi, porta 1x2 e box di collisione in piedi). La tuta e' scalata di S attorno ai piedi, il pilota e' a
// grandezza normale, sollevato di 24(S-1) px e senza gambe, come nel gioco (exo.SUIT_SCALE, exo.PILOT_LIFT_PX).
// Gli screenshot sono 1000x1000 px CSS (porta compresa) a deviceScaleFactor 2.
//
// Il server su 127.0.0.1 serve in sola lettura due cartelle: /preview/ (questa) e /model/ (le risorse del modello).
const http = require('http');
const fs = require('fs');
const path = require('path');
let chromium;
try {
  ({ chromium } = require('playwright'));
} catch (e) {
  console.error('Playwright non trovato: serve per le anteprime. Fuori dalla sessione cloud installarlo con\n'
    + '  npm install playwright && npx playwright install chromium\n(in tools/art/preview). Dettaglio: ' + e.message);
  process.exit(1);
}

const HERE = __dirname;
const args = { assets: path.join(HERE, '..', '..', '..', 'src', 'main', 'resources', 'assets', 'redstone_exoframes'),
  views: '', jobs: '' };
for (let i = 2; i < process.argv.length; i += 2) {
  const k = process.argv[i].replace(/^--/, '');
  if (!(k in args) || process.argv[i + 1] === undefined) { console.error('opzione sconosciuta o senza valore:', process.argv[i]); process.exit(2); }
  args[k] = process.argv[i + 1];
}

// --- viste standard ---------------------------------------------------------------------------------------------
const S = 1.4;                 // exo.SUIT_SCALE
const LIFT = 24 * (S - 1);     // exo.PILOT_LIFT_PX: collo e spalle del pilota allineati a quelli della tuta
const BLOCK = 16 / 0.9375;     // px di modello non scalati per blocco (il giocatore e' reso a 15/16): 17.07
const MODEL = { geo: 'model/geo/exoframe.geo.json', tex: 'model/textures/entity/exoframe.png',
  glow: 'model/textures/entity/exoframe_glowmask.png', S };
const PILOT = { raise: LIFT, legs: false };
const half = BLOCK / 2;
const STANDARD = {
  fronte: { dir: [0.55, 0.3, -0.8], fit: 'auto' },
  retro: { dir: [-0.55, 0.3, 0.8], fit: 'auto' },
  lato: { dir: [1, 0.12, -0.05], fit: 'auto' },
  testa: { dir: [0.5, 0.25, -0.85], pad: 1.0, fit: [[-6 * S, 21.5 * S, -6 * S], [6 * S, 31 * S, 5 * S]] },
  gamba: { dir: [0.8, 0.22, -0.55], pad: 1.0, fit: [[-1 * S, 0, -6 * S], [6 * S, 16 * S, 5 * S]] },
  porta: {
    dir: [0.06, 0.12, -1], pad: 1.04, fit: [[-1.6 * BLOCK, 0, -14], [1.6 * BLOCK, 3.1 * BLOCK, 26]],
    floor: { block: BLOCK, from: -half - 2 * BLOCK, to: half + 2 * BLOCK, zfrom: half - 3 * BLOCK, zto: half + BLOCK },
    doors: [{ dx: 0, w: BLOCK, h: 2 * BLOCK, z: half + BLOCK }],
    boxes: [{ dx: 0, w: 0.6 * S * BLOCK, d: 0.6 * S * BLOCK, h: 1.8 * S * BLOCK }],
  },
};

function standardJobs() {
  const names = args.views ? args.views.split(',') : Object.keys(STANDARD);
  return names.map(name => {
    if (!STANDARD[name]) { console.error('vista sconosciuta:', name, '(viste:', Object.keys(STANDARD).join(', ') + ')'); process.exit(2); }
    return { name, cfg: { models: [MODEL], pilots: [PILOT], ...STANDARD[name] } };
  });
}

// Blocchi di una PNG: le anteprime vanno nei documenti senza metadati (solo IHDR, IDAT, IEND).
function pngChunks(file) {
  const d = fs.readFileSync(file);
  const kinds = [];
  for (let pos = 8; pos + 8 <= d.length; pos += 12 + d.readUInt32BE(pos)) kinds.push(d.toString('latin1', pos + 4, pos + 8));
  return kinds;
}

// --- server e screenshot ----------------------------------------------------------------------------------------
const MOUNTS = { '/preview/': HERE, '/model/': path.resolve(args.assets) };
const TYPES = { '.html': 'text/html', '.js': 'text/javascript', '.json': 'application/json', '.png': 'image/png' };
const server = http.createServer((req, res) => {
  const url = decodeURIComponent(req.url.split('?')[0]);
  const prefix = Object.keys(MOUNTS).find(m => url.startsWith(m));
  const root = prefix && MOUNTS[prefix];
  const p = root && path.resolve(root, '.' + url.slice(prefix.length - 1));
  if (!p || !(p === root || p.startsWith(root + path.sep)) || !fs.existsSync(p) || fs.statSync(p).isDirectory()) {
    res.writeHead(404); return res.end();
  }
  res.writeHead(200, { 'Content-Type': TYPES[path.extname(p)] || 'application/octet-stream', 'Cache-Control': 'no-store' });
  fs.createReadStream(p).pipe(res);
});

(async () => {
  const jobs = args.jobs ? JSON.parse(fs.readFileSync(args.jobs, 'utf8')) : standardJobs();
  await new Promise(r => server.listen(0, '127.0.0.1', r));
  const port = server.address().port;
  const browser = await chromium.launch({ args: ['--use-gl=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
  const outDir = path.join(HERE, 'out');
  let failed = false;
  for (const job of jobs) {
    const ctx = await browser.newContext({ viewport: { width: job.width || 1000, height: job.height || 1000 }, deviceScaleFactor: 2 });
    const page = await ctx.newPage();
    page.on('console', m => { if (m.type() === 'error') { console.error(job.name, m.text()); failed = true; } });
    page.on('pageerror', e => { console.error(job.name, e.message); failed = true; });
    await page.goto(`http://127.0.0.1:${port}/preview/scene.html?cfg=${encodeURIComponent(JSON.stringify(job.cfg))}`);
    await page.waitForFunction('window.__ready === true', null, { timeout: 120000 });
    const file = path.join(outDir, job.name + '.png');
    fs.mkdirSync(path.dirname(file), { recursive: true });
    await page.screenshot({ path: file });
    await ctx.close();
    const extra = pngChunks(file).filter(k => !['IHDR', 'IDAT', 'IEND'].includes(k));
    if (extra.length) { console.error(job.name, 'blocchi PNG oltre a IHDR, IDAT, IEND:', extra.join(', ')); failed = true; }
    console.log('scritto', path.relative(process.cwd(), file));
  }
  await browser.close();
  server.close();
  if (failed) process.exit(1);
})().catch(e => { console.error(e); process.exit(1); });
