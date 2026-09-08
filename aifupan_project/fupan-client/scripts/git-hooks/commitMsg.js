const fs = require('fs')
const path = require('path')
const { execSync } = require('child_process')

const projectRoot = path.resolve(__dirname, '..', '..')

const safeExec = (command) => {
  try {
    return execSync(command, { cwd: projectRoot, stdio: ['ignore', 'pipe', 'ignore'] }).toString()
  } catch (_) {
    return ''
  }
}

const extractVersion = (text) => {
  const raw = String(text || '')
  const match = raw.match(/\d+\.\d+\.\d+(?:\.\d+)?/)
  return match ? match[0] : ''
}

const toPosix = (p) => String(p || '').replace(/\\/g, '/')

const formatDate = (d = new Date()) => {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const formatDateTime = (d = new Date()) => {
  const date = formatDate(d)
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  return `${date} ${h}:${m}`
}

const shortenFiles = (files = [], limit = 6) => {
  const list = (Array.isArray(files) ? files : []).filter(Boolean)
  if (list.length <= limit) return list.join(', ')
  return `${list.slice(0, limit).join(', ')}...(+${list.length - limit})`
}

const escapeCell = (text) => {
  return String(text || '')
    .replace(/\|/g, '\\|')
    .replace(/\r?\n/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

const formatFilesForTable = (files = [], limit = 6) => {
  const list = (Array.isArray(files) ? files : []).filter(Boolean).map(toPosix)
  if (list.length <= limit) return list.join('<br>')
  const head = list.slice(0, limit).join('<br>')
  return `${head}<br>...(+${list.length - limit})`
}

const parseCommitMessage = (raw) => {
  const text = String(raw || '').trim()
  const [firstLine = '', ...restLines] = text.split(/\r?\n/)
  const first = firstLine.trim()
  const detail = restLines.join('\n').trim()

  let title = first
  let moduleName = ''

  const conventional = first.match(/^(\w+)(\(([^)]+)\))?:\s*(.+)$/)
  if (conventional) {
    moduleName = conventional[3] || ''
    title = conventional[4] || first
  } else {
    const bracket = first.match(/^\[([^\]]+)\]\s*(.+)$/)
    if (bracket) {
      moduleName = bracket[1] || ''
      title = bracket[2] || first
    }
  }

  return {
    title: title.trim(),
    moduleName: moduleName.trim(),
    detail: detail || ''
  }
}

const deriveModuleFromFiles = (files = []) => {
  const list = (Array.isArray(files) ? files : []).map(toPosix)
  for (const p of list) {
    const m = p.match(/^src\/views\/modules\/([^/]+)\//)
    if (m) return m[1]
  }
  for (const p of list) {
    const m = p.match(/^src\/([^/]+)\//)
    if (m) return m[1]
  }
  return ''
}

const ensureDir = (dirPath) => {
  if (fs.existsSync(dirPath)) return
  fs.mkdirSync(dirPath, { recursive: true })
}

const upsertVersionLog = (options) => {
  const { version, title, moduleName, detail, files } = options || {}
  if (!version) return

  const baseDir = path.join(projectRoot, 'docs', '版本变更')
  ensureDir(baseDir)
  const logPath = path.join(baseDir, `${version}.md`)
  const now = new Date()
  const date = formatDate(now)
  const dateTime = formatDateTime(now)
  const anchor = `#${date}`

  const tableHeader =
    '| 记录时间 | 功能变更标题 | 功能变更模块 | 功能变更文件 | 功能变更详细内容（简洁） |\n' +
    '| --- | --- | --- | --- | --- |\n'

  const fileList = formatFilesForTable(files, 6)
  const safeTitle = escapeCell(title || '-')
  const safeModule = escapeCell(moduleName || '-')
  const safeDetail = escapeCell(detail || '') || safeTitle || '-'
  const row = `| ${escapeCell(dateTime)} | ${safeTitle} | ${safeModule} | ${escapeCell(fileList || '-')} | ${safeDetail} |\n`

  if (!fs.existsSync(logPath)) {
    const content =
      `# ${version} 变更记录\n\n` +
      `## 目录\n\n` +
      `- [${date}](${anchor})\n\n` +
      `## ${date}\n\n` +
      `${tableHeader}${row}`
    fs.writeFileSync(logPath, content, 'utf8')
    return { logPath, created: true }
  }

  const raw = fs.readFileSync(logPath, 'utf8') || ''
  const dateHeader = `## ${date}`

  let next = raw
  if (!next.includes('## 目录')) {
    const header = `# ${version} 变更记录`
    if (next.startsWith(header)) {
      next = next.replace(header, `${header}\n\n## 目录\n`)
    } else {
      next = `# ${version} 变更记录\n\n## 目录\n\n${next.trim()}\n`
    }
  }

  const linkLine = `- [${date}](${anchor})`
  if (next.includes('## 目录') && !next.includes(linkLine)) {
    const marker = '## 目录'
    const markerIndex = next.indexOf(marker)
    if (markerIndex >= 0) {
      const after = next.slice(markerIndex + marker.length)
      const insertAt = markerIndex + marker.length + (after.startsWith('\n') ? 1 : 0)
      next = `${next.slice(0, insertAt)}\n${linkLine}${next.slice(insertAt)}`
    }
  }

  if (!next.includes(dateHeader)) {
    next = `${next.trim()}\n\n${dateHeader}\n\n${tableHeader}${row}`
    fs.writeFileSync(logPath, `${next.trim()}\n`, 'utf8')
    return { logPath, created: false }
  }

  const dateIndex = next.indexOf(dateHeader)
  const tableIndex = next.indexOf(tableHeader, dateIndex)
  if (tableIndex >= 0) {
    const insertAt = tableIndex + tableHeader.length
    next = `${next.slice(0, insertAt)}${row}${next.slice(insertAt)}`
    fs.writeFileSync(logPath, next, 'utf8')
    return { logPath, created: false }
  }

  const insertAt = dateIndex + dateHeader.length
  next = `${next.slice(0, insertAt)}\n\n${tableHeader}${row}${next.slice(insertAt)}`
  fs.writeFileSync(logPath, next, 'utf8')
  return { logPath, created: false }
}

const run = () => {
  const commitMsgFile = process.argv[2]
  if (!commitMsgFile) process.exit(0)

  const branchName = safeExec('git rev-parse --abbrev-ref HEAD').trim()
  const version = extractVersion(branchName)
  if (!version) process.exit(0)

  const msgRaw = fs.existsSync(commitMsgFile) ? fs.readFileSync(commitMsgFile, 'utf8') : ''
  const parsed = parseCommitMessage(msgRaw)

  const stagedFiles = safeExec('git diff --cached --name-only')
    .split(/\r?\n/)
    .map((s) => s.trim())
    .filter(Boolean)
    .map(toPosix)

  const workingTreeFiles = safeExec('git diff --name-only')
    .split(/\r?\n/)
    .map((s) => s.trim())
    .filter(Boolean)
    .map(toPosix)

  const candidates = stagedFiles.length ? stagedFiles : workingTreeFiles
  const codeFiles = candidates.filter((p) => p.startsWith('src/') || p.startsWith('mockjs/') || p.startsWith('scripts/'))
  if (codeFiles.length === 0) process.exit(0)

  const moduleName = parsed.moduleName || deriveModuleFromFiles(codeFiles)
  const result = upsertVersionLog({
    version,
    title: parsed.title || '',
    moduleName,
    detail: parsed.detail || '',
    files: codeFiles
  })

  if (result?.logPath) {
    const relative = path.relative(projectRoot, result.logPath).split(path.sep).join('/')
    try {
      execSync(`git add "${relative}"`, { cwd: projectRoot, stdio: 'ignore' })
    } catch (_) {}
  }
}

run()

