const fs = require('fs');
const path = require('path');

const projectRoot = path.resolve(__dirname, '..');

const inputPath = path.resolve(projectRoot, 'docs/2.6.9.3原始需求.md');
const outputPath = path.resolve(projectRoot, 'docs/2.6.9.3原始需求.extracted.md');
const comparePath = path.resolve(projectRoot, 'docs/2.6.0.3.md');

const decodeEntities = (text = '') => {
  const raw = String(text ?? '');
  const named = {
    '&nbsp;': ' ',
    '&amp;': '&',
    '&lt;': '<',
    '&gt;': '>',
    '&quot;': '"',
    '&#39;': "'",
    '&apos;': "'",
  };
  let out = raw.replace(/&(nbsp|amp|lt|gt|quot|apos);|&#39;/g, (m) => named[m] ?? m);
  out = out.replace(/&#(\d+);/g, (_, n) => {
    const code = Number(n);
    if (!Number.isFinite(code)) return _;
    try { return String.fromCharCode(code); } catch { return _; }
  });
  out = out.replace(/&#x([0-9a-fA-F]+);/g, (_, n) => {
    const code = parseInt(n, 16);
    if (!Number.isFinite(code)) return _;
    try { return String.fromCharCode(code); } catch { return _; }
  });
  return out;
};

const htmlToPlainText = (html = '') => {
  let s = String(html ?? '');

  s = s.replace(/<script\b[^>]*>[\s\S]*?<\/script>/gi, '');
  s = s.replace(/<style\b[^>]*>[\s\S]*?<\/style>/gi, '');

  s = s.replace(/<br\s*\/?>/gi, '\n');
  s = s.replace(/<\/(h[1-6]|p|div|section|article|header|footer|blockquote|pre|ul|ol)>/gi, '\n');
  s = s.replace(/<li\b[^>]*>/gi, '\n- ');
  s = s.replace(/<\/li>/gi, '');

  s = s.replace(/<tr\b[^>]*>/gi, '\n');
  s = s.replace(/<\/tr>/gi, '');
  s = s.replace(/<t[dh]\b[^>]*>/gi, '\t');
  s = s.replace(/<\/t[dh]>/gi, '');

  s = s.replace(/<a\b[^>]*href=["']([^"']+)["'][^>]*>[\s\S]*?<\/a>/gi, (_, href) => ` ${href} `);
  s = s.replace(/<[^>]+>/g, '');
  s = decodeEntities(s);

  s = s.replace(/\r\n/g, '\n');
  s = s.replace(/[ \t]+\n/g, '\n');
  s = s.replace(/\n{3,}/g, '\n\n');
  const lines = s
    .replace(/\uFEFF/g, '')
    .split('\n')
    .map((l) => l.trim())
    .filter((l) => l);

  const cleaned = [];
  for (const line of lines) {
    if (/^输入文字或/.test(line)) continue;
    if (/^最多\s*\d+\s*列$/.test(line)) continue;
    const prev = cleaned[cleaned.length - 1] || '';
    if (prev === line) continue;
    cleaned.push(line);
  }

  return cleaned.join('\n').trim();
};

const extractRequirementIds = (text = '') => {
  const s = String(text ?? '');
  const re = /\bR-[A-Z0-9]+(?:-[A-Z0-9]+)+\b/g;
  const out = new Set();
  let m;
  while ((m = re.exec(s))) {
    out.add(m[0]);
  }
  return out;
};

const pickMeta = (text = '') => {
  const s = String(text ?? '');
  const pickOne = (re) => {
    const m = s.match(re);
    return m?.[1]?.trim() || '';
  };
  return {
    reqId: pickOne(/\bREQ-ID\b[：:\s]*([^\n]+)/i),
    title: pickOne(/\b标题\b[：:\s]*([^\n]+)/),
    targetVersion: pickOne(/\b目标版本\b[：:\s]*([0-9.]+)/),
    effectiveDate: pickOne(/\b生效日期\b[：:\s]*([0-9-]+)/),
  };
};

const compareDocs = (aText, bText) => {
  const aMeta = pickMeta(aText);
  const bMeta = pickMeta(bText);
  const aIds = extractRequirementIds(aText);
  const bIds = extractRequirementIds(bText);

  const intersection = new Set([...aIds].filter((x) => bIds.has(x)));
  const onlyInA = [...aIds].filter((x) => !bIds.has(x)).sort();
  const onlyInB = [...bIds].filter((x) => !aIds.has(x)).sort();

  return {
    meta: { extracted: aMeta, prd: bMeta },
    requirementIdStats: {
      extractedCount: aIds.size,
      prdCount: bIds.size,
      intersectionCount: intersection.size,
      onlyInExtracted: onlyInA,
      onlyInPrd: onlyInB,
    },
  };
};

const main = () => {
  if (!fs.existsSync(inputPath)) {
    process.stderr.write(`Input not found: ${inputPath}\n`);
    process.exit(1);
  }
  const raw = fs.readFileSync(inputPath, 'utf8');
  const extracted = htmlToPlainText(raw);
  fs.writeFileSync(outputPath, extracted + '\n', 'utf8');

  let report = null;
  if (fs.existsSync(comparePath)) {
    const prd = fs.readFileSync(comparePath, 'utf8');
    report = compareDocs(extracted, prd);
  }

  process.stdout.write(`Wrote: ${outputPath}\n`);
  if (report) {
    process.stdout.write(JSON.stringify(report, null, 2) + '\n');
  }
};

if (require.main === module) {
  main();
}
