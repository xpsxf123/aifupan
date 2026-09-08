const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const CONST_FILE = path.resolve(__dirname, '../src/constants/webVersion.js');

const safeExec = (command) => {
  try {
    return execSync(command, { stdio: ['ignore', 'pipe', 'ignore'] }).toString().trim();
  } catch (_) {
    return '';
  }
};

const normalizeVersionText = (value = '') => {
  const text = String(value ?? '');
  const m = text.match(/(\d+(?:\.\d+)*)/);
  const raw = m?.[1] || '';
  if (!raw) return '';
  return raw
    .replace(/[^0-9.]/g, '')
    .replace(/\.{2,}/g, '.')
    .replace(/^\./, '')
    .replace(/\.$/, '');
};

const extractAutoVersion = (text = '') => {
  const raw = String(text || '');
  const match = raw.match(/-(\d+(?:\.\d+){0,3})(?=-|$)/);
  return match?.[1] || '';
};

const ensureFile = () => {
  if (fs.existsSync(CONST_FILE)) return;
  const dir = path.dirname(CONST_FILE);
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }
  fs.writeFileSync(
    CONST_FILE,
    [
      'export const AUTO_WEB_VERSION = "";',
      'export const MANUAL_WEB_VERSION = "";',
      ''
    ].join('\n'),
    'utf8'
  );
};

const updateConstLine = (content, key, nextValue) => {
  const re = new RegExp(`export\\s+const\\s+${key}\\s*=\\s*['"][^'"]*['"]\\s*;`);
  if (re.test(content)) {
    return content.replace(re, `export const ${key} = ${JSON.stringify(String(nextValue || ''))};`);
  }
  const lines = content.split(/\r?\n/);
  lines.unshift(`export const ${key} = ${JSON.stringify(String(nextValue || ''))};`);
  return lines.join('\n');
};

const updateAutoWebVersion = (gitRefText = '') => {
  ensureFile();
  const autoVersion = extractAutoVersion(gitRefText);
  if (!autoVersion) {
    return '';
  }
  const prev = fs.readFileSync(CONST_FILE, 'utf8');
  const next = updateConstLine(prev, 'AUTO_WEB_VERSION', autoVersion);
  if (next !== prev) {
    fs.writeFileSync(CONST_FILE, next, 'utf8');
  }
  return autoVersion;
};

const updateManualWebVersion = (manualVersionText = '') => {
  ensureFile();
  const manualVersion = normalizeVersionText(manualVersionText);
  if (!manualVersion) {
    return '';
  }
  const prev = fs.readFileSync(CONST_FILE, 'utf8');
  const next = updateConstLine(prev, 'MANUAL_WEB_VERSION', manualVersion);
  if (next !== prev) {
    fs.writeFileSync(CONST_FILE, next, 'utf8');
  }
  return manualVersion;
};

const runCli = () => {
  const args = process.argv.slice(2);
  if (args.length && normalizeVersionText(args[0])) {
    const value = updateManualWebVersion(args[0]);
    if (value) {
      process.stdout.write(`MANUAL_WEB_VERSION=${value}\n`);
      return;
    }
    process.stdout.write('MANUAL_WEB_VERSION unchanged\n');
    return;
  }
  const branch = safeExec('git rev-parse --abbrev-ref HEAD');
  const gitRef = process.env.VUE_APP_VERSION || branch || '';
  const value = updateAutoWebVersion(gitRef);
  if (value) {
    process.stdout.write(`AUTO_WEB_VERSION=${value}\n`);
    return;
  }
  process.stdout.write('AUTO_WEB_VERSION unchanged\n');
};

module.exports = {
  extractAutoVersion,
  normalizeVersionText,
  updateAutoWebVersion,
  updateManualWebVersion
};

if (require.main === module) {
  runCli();
}

