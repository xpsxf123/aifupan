const fs = require('fs')
const path = require('path')
const { execSync } = require('child_process')

const projectRoot = path.resolve(__dirname, '..', '..')
const outPath = path.join(projectRoot, 'agnet', 'wiki', 'preferences', 'file-registry.json')

const ensureDir = (dirPath) => {
  if (fs.existsSync(dirPath)) return
  fs.mkdirSync(dirPath, { recursive: true })
}

const safeExec = (command) => {
  try {
    return execSync(command, { cwd: projectRoot, stdio: ['ignore', 'pipe', 'ignore'] }).toString()
  } catch (_) {
    return ''
  }
}

const nowText = () => {
  const d = new Date()
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  const ss = String(d.getSeconds()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd} ${hh}:${mi}:${ss}`
}

const build = () => {
  const raw = safeExec('git ls-files -z')
  const files = raw
    .split('\0')
    .map((s) => s.trim())
    .filter(Boolean)
    .sort((a, b) => a.localeCompare(b, 'zh-CN', { numeric: true }))

  const byBasename = {}
  files.forEach((f) => {
    const base = path.posix.basename(f.replace(/\\/g, '/'))
    if (!byBasename[base]) byBasename[base] = []
    byBasename[base].push(f)
  })

  const duplicates = Object.entries(byBasename)
    .filter(([, list]) => list.length > 1)
    .map(([basename, list]) => ({ basename, paths: list }))
    .sort((a, b) => a.basename.localeCompare(b.basename, 'zh-CN', { numeric: true }))

  return {
    generatedAt: nowText(),
    total: files.length,
    duplicates,
    files
  }
}

const run = () => {
  const data = build()
  ensureDir(path.dirname(outPath))
  fs.writeFileSync(outPath, JSON.stringify(data, null, 2), 'utf8')
  process.stdout.write(`file registry updated: ${path.relative(projectRoot, outPath)} (total=${data.total}, duplicates=${data.duplicates.length})\n`)
}

run()

