const fs = require('fs')
const path = require('path')

const projectRoot = path.resolve(__dirname, '..', '..')
const memoryRoot = path.join(projectRoot, 'agnet', 'wiki', 'preferences', 'qa_memory')
const sessionsDir = path.join(memoryRoot, 'sessions')
const statePath = path.join(memoryRoot, 'state.json')

const ensureDir = (dirPath) => {
  if (fs.existsSync(dirPath)) return
  fs.mkdirSync(dirPath, { recursive: true })
}

const readJson = (filePath, fallback) => {
  try {
    if (!fs.existsSync(filePath)) return fallback
    const text = fs.readFileSync(filePath, 'utf8')
    if (!text) return fallback
    return JSON.parse(text)
  } catch (_) {
    return fallback
  }
}

const writeJson = (filePath, data) => {
  ensureDir(path.dirname(filePath))
  fs.writeFileSync(filePath, JSON.stringify(data, null, 2), 'utf8')
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

const newSessionId = () => {
  const d = new Date()
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  const rand = Math.random().toString(16).slice(2, 6)
  return `qa_${yyyy}${mm}${dd}_${hh}${mi}_${rand}`
}

const parseArgs = (argv) => {
  const out = {}
  for (let i = 0; i < argv.length; i++) {
    const cur = argv[i]
    if (!cur.startsWith('--')) continue
    const key = cur.slice(2)
    const next = argv[i + 1]
    const isFlag = !next || next.startsWith('--')
    out[key] = isFlag ? 'true' : next
    if (!isFlag) i++
  }
  return out
}

const boolFromText = (v) => {
  if (v === undefined || v === null) return null
  const t = String(v).trim().toLowerCase()
  if (t === 'true' || t === '1' || t === 'yes' || t === 'y') return true
  if (t === 'false' || t === '0' || t === 'no' || t === 'n') return false
  return null
}

const appendText = (filePath, content) => {
  ensureDir(path.dirname(filePath))
  fs.appendFileSync(filePath, content, 'utf8')
}

const ensureSessionFile = (sessionId) => {
  const filePath = path.join(sessionsDir, `${sessionId}.md`)
  if (fs.existsSync(filePath)) return filePath
  appendText(
    filePath,
    `# ${sessionId}\n\n` +
      `- createdAt: ${nowText()}\n` +
      `- scope: 对话蒸馏摘要（append-only）\n\n`
  )
  return filePath
}

const run = () => {
  ensureDir(memoryRoot)
  ensureDir(sessionsDir)

  const args = parseArgs(process.argv.slice(2))
  const reset = boolFromText(args.reset) === true
  const maxFail = Number(args['max-fail'] ?? 4)

  const q = String(args.q ?? '').trim()
  const a = String(args.a ?? '').trim()
  const distilled = String(args.distilled ?? '').trim()
  const acceptance = String(args.acceptance ?? '').trim()
  const next = String(args.next ?? '').trim()
  const satisfied = boolFromText(args.satisfied)

  let state = readJson(statePath, {
    sessionId: '',
    failCount: 0,
    resetRequired: false,
    updatedAt: ''
  })

  if (reset) {
    const sid = newSessionId()
    state = {
      sessionId: sid,
      failCount: 0,
      resetRequired: true,
      updatedAt: nowText()
    }
    ensureSessionFile(sid)
    writeJson(statePath, state)
    process.stdout.write(`qa_memory reset: ${sid}\n`)
    return
  }

  if (!state.sessionId) state.sessionId = newSessionId()
  const sessionFile = ensureSessionFile(state.sessionId)

  if (!q && !a && !distilled) {
    process.stderr.write('Usage: npm run qa:log -- --q "..." --a "..." --distilled "..." --satisfied true|false\n')
    process.exit(1)
  }

  const lines = []
  lines.push(`## ${nowText()}\n`)
  if (q) lines.push(`- Q: ${q}\n`)
  if (a) lines.push(`- A: ${a}\n`)
  if (distilled) lines.push(`- Distilled: ${distilled}\n`)
  if (acceptance) lines.push(`- Acceptance: ${acceptance}\n`)
  if (next) lines.push(`- Next: ${next}\n`)
  if (satisfied !== null) lines.push(`- Satisfied: ${satisfied}\n`)

  if (satisfied === false) {
    state.failCount = Number(state.failCount ?? 0) + 1
  } else if (satisfied === true) {
    state.failCount = 0
    state.resetRequired = false
  }

  let thresholdTriggered = false
  if (Number.isFinite(maxFail) && state.failCount > maxFail) {
    thresholdTriggered = true
    state.resetRequired = true
  }

  if (thresholdTriggered) {
    lines.push(`- Reset: failCount(${state.failCount}) > maxFail(${maxFail})，进入新会话并忽略旧上下文\n`)
  }

  lines.push('\n')
  appendText(sessionFile, lines.join(''))

  if (thresholdTriggered) {
    state.sessionId = newSessionId()
    state.failCount = 0
    state.resetRequired = true
    ensureSessionFile(state.sessionId)
  }

  state.updatedAt = nowText()
  writeJson(statePath, state)

  process.stdout.write(`qa_memory updated: ${path.relative(projectRoot, sessionFile)}\n`)
  if (thresholdTriggered) {
    process.stdout.write(`qa_memory new session: ${state.sessionId} (resetRequired=true)\n`)
  }
}

run()

