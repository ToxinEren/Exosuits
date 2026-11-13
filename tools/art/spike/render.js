// Render every direction/view with headless Chromium. Usage: node render.js [A B C]
const http = require('http');
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

const ROOT = __dirname;
const TYPES = { '.html': 'text/html', '.js': 'text/javascript', '.json': 'application/json', '.png': 'image/png' };
const server = http.createServer((req, res) => {
  const p = path.join(ROOT, decodeURIComponent(req.url.split('?')[0]));
  if (!p.startsWith(ROOT) || !fs.existsSync(p) || fs.statSync(p).isDirectory()) { res.writeHead(404); return res.end(); }
  res.writeHead(200, { 'Content-Type': TYPES[path.extname(p)] || 'application/octet-stream' });
  fs.createReadStream(p).pipe(res);
});

const dirs = process.argv.slice(2).length ? process.argv.slice(2) : ['A', 'B', 'C'];
const views = ['full_front', 'full_back', 'full_side', 'frame_front'];

(async () => {
  await new Promise(r => server.listen(0, '127.0.0.1', r));
  const port = server.address().port;
  const browser = await chromium.launch({ args: ['--use-gl=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
  const ctx = await browser.newContext({ viewport: { width: 1000, height: 1000 }, deviceScaleFactor: 2 });
  fs.mkdirSync(path.join(ROOT, 'out'), { recursive: true });
  for (const d of dirs) {
    for (const v of views) {
      const page = await ctx.newPage();
      page.on('console', m => { if (m.type() === 'error') console.error(d, v, m.text()); });
      page.on('pageerror', e => console.error(d, v, e.message));
      await page.goto(`http://127.0.0.1:${port}/viewer/index.html?model=${d}&view=${v}`);
      await page.waitForFunction('window.__ready === true', null, { timeout: 60000 });
      await page.screenshot({ path: path.join(ROOT, 'out', `${d}_${v}.png`) });
      await page.close();
      console.log('rendered', d, v);
    }
  }
  await browser.close();
  server.close();
})().catch(e => { console.error(e); process.exit(1); });
