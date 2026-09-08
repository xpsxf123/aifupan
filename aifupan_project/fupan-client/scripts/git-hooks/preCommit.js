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

const parseNameStatusZ = (raw) => {
  const tokens = raw.split('\0').filter((t) => t !== '')
  const changes = []
  for (let i = 0; i < tokens.length; ) {
    const status = tokens[i++]
    if (!status) break
    if (status.startsWith('R') || status.startsWith('C')) {
      const from = tokens[i++] || ''
      const to = tokens[i++] || ''
      changes.push({ status, path: to, from })
      continue
    }
    const p = tokens[i++] || ''
    changes.push({ status, path: p })
  }
  return changes.filter((c) => c.path)
}

const toPosix = (p) => String(p || '').replace(/\\/g, '/')

const extractVersion = (text) => {
  const raw = String(text || '')
  const match = raw.match(/\d+\.\d+\.\d+(?:\.\d+)?/)
  return match ? match[0] : ''
}

const parseVersionDocPath = (p) => {
  const text = toPosix(p)
  const legacy = text.match(/^docs\/(\d+\.\d+\.\d+(?:\.\d+)?)\-修改记录\.md$/)
  if (legacy) return legacy[1]
  const folder = text.match(/^docs\/版本变更\/(\d+\.\d+\.\d+(?:\.\d+)?)\.md$/)
  return folder ? folder[1] : ''
}

const run = () => {
  const allowlistPath = path.join(projectRoot, 'scripts', 'git-hooks', 'duplicate-allowlist.json')
  const allow = readJson(allowlistPath, { basenames: [] })
  const allowed = new Set((allow?.basenames || []).map((s) => String(s).trim()).filter(Boolean))

  const stagedRaw = safeExec('git diff --cached --name-status -z')
  const changes = parseNameStatusZ(stagedRaw)

  const added = changes
    .filter((c) => c.status === 'A' || c.status.startsWith('C') || c.status.startsWith('R'))
    .map((c) => toPosix(c.path))
  const modified = changes.filter((c) => c.status === 'M').map((c) => toPosix(c.path))

  if (changes.length === 0) process.exit(0)

  const branchName = safeExec('git rev-parse --abbrev-ref HEAD').trim()
  const branchVersion = extractVersion(branchName)
  const versionDocChanges = changes
    .map((c) => ({ ...c, path: toPosix(c.path) }))
    .map((c) => ({ ...c, docVersion: parseVersionDocPath(c.path) }))
    .filter((c) => !!c.docVersion)

  if (versionDocChanges.length > 0) {
    const mismatched = versionDocChanges.filter((c) => !branchVersion || c.docVersion !== branchVersion)
    if (mismatched.length > 0) {
      process.stderr.write('\n[pre-commit] 版本修改记录与当前分支版本不一致，已阻止提交并清理本次变更\n')
      process.stderr.write(`- 当前分支：${branchName || '-'}\n`)
      process.stderr.write(`- 分支版本：${branchVersion || '-'}\n`)
      process.stderr.write('\n不一致的文件：\n')
      mismatched.forEach((c) => {
        process.stderr.write(`- ${c.path} (docVersion=${c.docVersion})\n`)
      })
      process.stderr.write('\n处理建议：\n')
      process.stderr.write('- 切换到对应版本分支后再编辑该版本的修改记录\n')
      process.stderr.write('- 或将修改记录文件名调整为与当前分支版本一致\n\n')

      mismatched.forEach((c) => {
        const filePath = path.join(projectRoot, ...c.path.split('/'))
        try {
          execSync(`git reset -q HEAD -- "${c.path}"`, { cwd: projectRoot, stdio: 'ignore' })
        } catch (_) {}

        if (c.status === 'A' || c.status.startsWith('C') || c.status.startsWith('R')) {
          try {
            if (fs.existsSync(filePath)) fs.unlinkSync(filePath)
          } catch (_) {}
          return
        }

        try {
          execSync(`git checkout -- "${c.path}"`, { cwd: projectRoot, stdio: 'ignore' })
        } catch (_) {}
      })

      process.exit(1)
    }
  }

  const trackedRaw = safeExec('git ls-files -z')
  const tracked = trackedRaw
    .split('\0')
    .map((s) => s.trim())
    .filter(Boolean)
    .map(toPosix)

  const trackedByBase = new Map()
  tracked.forEach((p) => {
    const base = path.posix.basename(p)
    const list = trackedByBase.get(base) || []
    list.push(p)
    trackedByBase.set(base, list)
  })

  const addedByBase = new Map()
  added.forEach((p) => {
    const base = path.posix.basename(p)
    const list = addedByBase.get(base) || []
    list.push(p)
    addedByBase.set(base, list)
  })

  const collisions = []

  addedByBase.forEach((paths, base) => {
    if (allowed.has(base)) return
    if (paths.length > 1) {
      collisions.push({ basename: base, type: 'added-duplicate', paths })
      return
    }
    const trackedList = (trackedByBase.get(base) || []).filter((p) => p !== paths[0])
    if (trackedList.length > 0) {
      collisions.push({ basename: base, type: 'tracked-duplicate', paths: [paths[0], ...trackedList] })
    }
  })

  if (collisions.length > 0) {
    process.stderr.write('\n[pre-commit] 检测到新增文件重名（按 basename）\n')
    collisions.forEach((c) => {
      process.stderr.write(`- ${c.basename} (${c.type})\n`)
      c.paths.forEach((p) => process.stderr.write(`  - ${p}\n`))
    })
    process.stderr.write('\n处理方式：\n')
    process.stderr.write('- 建议重命名新增文件，或调整目录结构避免同名\n')
    process.stderr.write('- 若确需允许同名：将 basename 加入 scripts/git-hooks/duplicate-allowlist.json\n')
    process.stderr.write('\n')
    process.exit(1)
  }

  process.stdout.write(`[pre-commit] staged files: added=${added.length}, modified=${modified.length}\n`)

  const registryScript = path.join(projectRoot, 'scripts', 'file-registry', 'rebuild.js')
  const registryOut = path.join(projectRoot, 'agnet', 'wiki', 'preferences', 'file-registry.json')

  try {
    execSync(`node "${registryScript}"`, { cwd: projectRoot, stdio: 'inherit' })
    execSync(`git add "${registryOut}"`, { cwd: projectRoot, stdio: ['ignore', 'pipe', 'pipe'] })
  } catch (_) {
    process.stdout.write('[pre-commit] file registry update skipped (non-blocking)\n')
  }
}

run()

