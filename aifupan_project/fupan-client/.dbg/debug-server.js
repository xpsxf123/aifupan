const http = require('http');
const fs = require('fs');
const path = require('path');
const url = require('url');

const args = process.argv.slice(2);
const argMap = {};
for (let i = 0; i < args.length; i += 1) {
  const key = args[i];
  if (!key.startsWith('--')) continue;
  const val = args[i + 1];
  argMap[key.slice(2)] = val;
}

const sessionId = argMap.session || 'notes-annotation-disappear';
const outdir = argMap.outdir || '.dbg';
const port = Number(argMap.port || 7777);

const rootDir = process.cwd();
const outDirAbs = path.resolve(rootDir, outdir);
if (!fs.existsSync(outDirAbs)) {
  fs.mkdirSync(outDirAbs, { recursive: true });
}

const logFile = path.join(outDirAbs, `trae-debug-log-${sessionId}.ndjson`);
const envFile = path.join(outDirAbs, `${sessionId}.env`);

fs.writeFileSync(envFile, `DEBUG_SERVER_URL=http://127.0.0.1:${port}/event\nDEBUG_SESSION_ID=${sessionId}\n`, 'utf8');

const writeEvent = (evt) => {
  const payload = {
    ts: Date.now(),
    ...evt,
    sessionId: evt?.sessionId || sessionId,
  };
  fs.appendFileSync(logFile, `${JSON.stringify(payload)}\n`, 'utf8');
  return payload;
};

const readBody = (req) =>
  new Promise((resolve) => {
    let data = '';
    req.on('data', (chunk) => {
      data += chunk;
    });
    req.on('end', () => resolve(data));
  });

const server = http.createServer(async (req, res) => {
  const parsed = url.parse(req.url, true);
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET,POST,DELETE,OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  if (req.method === 'GET' && parsed.pathname === '/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ ok: true, sessionId, port, logFile }));
    return;
  }

  if (parsed.pathname === '/event' && req.method === 'POST') {
    const bodyText = await readBody(req);
    let evt = {};
    try {
      evt = JSON.parse(bodyText || '{}');
    } catch {
      evt = { msg: '[DEBUG] invalid-json', raw: bodyText };
    }
    const payload = writeEvent(evt);
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ ok: true, payload }));
    return;
  }

  if (parsed.pathname === '/logs' && req.method === 'GET') {
    const last = Number(parsed.query?.last || 0);
    let lines = [];
    if (fs.existsSync(logFile)) {
      const raw = fs.readFileSync(logFile, 'utf8');
      lines = raw.split('\n').filter(Boolean);
    }
    if (last > 0 && lines.length > last) {
      lines = lines.slice(-last);
    }
    const items = lines.map((l) => {
      try {
        return JSON.parse(l);
      } catch {
        return { ts: Date.now(), sessionId, msg: '[DEBUG] invalid-line', raw: l };
      }
    });
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ ok: true, count: items.length, items }));
    return;
  }

  if (parsed.pathname === '/logs' && req.method === 'DELETE') {
    if (fs.existsSync(logFile)) fs.unlinkSync(logFile);
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ ok: true }));
    return;
  }

  res.writeHead(404, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ ok: false, error: 'not-found' }));
});

server.listen(port, '127.0.0.1', () => {
  process.stdout.write(`debug-server listening on http://127.0.0.1:${port} session=${sessionId}\n`);
});

