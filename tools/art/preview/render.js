// Anteprime locali (non nella CI): Chromium senza schermo + three.js, 1000x1000 px CSS a deviceScaleFactor 2.
//
//   node render.js                      telaio dai file del mod: out/exoframe_<vista>.png (front, back, side)
//   node render.js --views front,top --mannequin 0 --name prova
//   node render.js --geo <file.geo.json> --texture <atlante.png> --name gecko --views front,back
//   node render.js --pose 'armorRightArm:-40,0,0;armorLeftLeg:30,0,0'   rotazioni aggiunte alle ossa (gradi)
//
// Il visore legge il .geo.json e l'atlante cosi' come sono (nessun file intermedio) e li costruisce con geo.js.
const http = require('http');
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

const HERE = __dirname;
const ASSETS = path.join(HERE, '..', '..', '..', 'src', 'main', 'resources', 'assets', 'redstone_exoframes');

const args = { geo: path.join(ASSETS, 'geo', 'exoframe.geo.json'), texture: path.join(ASSETS, 'textures', 'entity', 'exoframe.png'),
  name: 'exoframe', views: 'front,back,side', mannequin: '1', pose: '', fit: 'suit' };
for (let i = 2; i < process.argv.length; i += 2) {
  const k = process.argv[i].replace(/^--/, '');
  if (!(k in args) || process.argv[i + 1] === undefined) { console.error('opzione sconosciuta o senza valore:', process.argv[i]); process.exit(2); }
  args[k] = process.argv[i + 1];
}

const ROUTES = {
  '/': [path.join(HERE, 'index.html'), 'text/html'],
  '/geo.js': [path.join(HERE, 'geo.js'), 'text/javascript'],
  '/three/three.module.js': [path.join(HERE, 'node_modules', 'three', 'build', 'three.module.js'), 'text/javascript'],
  '/model/geo.json': [path.resolve(args.geo), 'application/json'],
  '/model/texture.png': [path.resolve(args.texture), 'image/png'],
};
const server = http.createServer((req, res) => {
  const route = ROUTES[req.url.split('?')[0]];
  if (!route || !fs.existsSync(route[0])) { res.writeHead(404); return res.end(); }
  res.writeHead(200, { 'Content-Type': route[1], 'Cache-Control': 'no-store' });
  fs.createReadStream(route[0]).pipe(res);
});

(async () => {
  await new Promise(r => server.listen(0, '127.0.0.1', r));
  const port = server.address().port;
  const browser = await chromium.launch({ args: ['--use-gl=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
  const ctx = await browser.newContext({ viewport: { width: 1000, height: 1000 }, deviceScaleFactor: 2 });
  const outDir = path.join(HERE, 'out');
  fs.mkdirSync(outDir, { recursive: true });
  let failed = false;
  for (const view of args.views.split(',')) {
    const page = await ctx.newPage();
    page.on('console', m => { if (m.type() === 'error') { console.error(view, m.text()); failed = true; } });
    page.on('pageerror', e => { console.error(view, e.message); failed = true; });
    const qs = new URLSearchParams({ view, mannequin: args.mannequin, pose: args.pose, fit: args.fit });
    await page.goto(`http://127.0.0.1:${port}/?${qs}`);
    await page.waitForFunction('window.__ready === true', null, { timeout: 60000 });
    const file = path.join(outDir, `${args.name}_${view}.png`);
    await page.screenshot({ path: file });
    await page.close();
    console.log('scritto', path.relative(process.cwd(), file));
  }
  await browser.close();
  server.close();
  if (failed) process.exit(1);
})().catch(e => { console.error(e); process.exit(1); });
