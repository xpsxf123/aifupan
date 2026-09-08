const fs = require('fs')
const path = require('path')
const {
  versionRoot,
  templateRoot,
  ensureDir,
  readText,
  writeText,
  readJson,
  writeJson,
  today,
  normalizeVersionText,
  fillTemplate
} = require('./utils')

const { rebuild } = require('./rebuildVersionIndex')

const templateMap = [
  { src: 'meta.template.json', dest: 'meta.json' },
  { src: 'PRD.template.md', dest: 'PRD.md' },
  { src: 'integrated.template.md', dest: 'integrated.md' },
  { src: 'source.extracted.template.md', dest: 'source.extracted.md' },
  { src: '接口.template.md', dest: '接口.md' },
  { src: 'distilled.template.md', dest: 'distilled.md' },
  { src: 'interfaces.index.template.json', dest: 'interfaces.index.json' }
]

const ensureVersionDir = (version, title) => {
  const versionDir = path.join(versionRoot, version)
  ensureDir(versionDir)

  const vars = { version, title, date: today() }
  templateMap.forEach(({ src, dest }) => {
    const targetPath = path.join(versionDir, dest)
    if (fs.existsSync(targetPath)) return
    const templatePath = path.join(templateRoot, src)
    const templateText = readText(templatePath)
    if (!templateText) {
      writeText(targetPath, '')
      return
    }
    writeText(targetPath, fillTemplate(templateText, vars))
  })

  const metaPath = path.join(versionDir, 'meta.json')
  const meta = readJson(metaPath, {})
  meta.version = meta.version || version
  meta.title = meta.title || title
  meta.last_modified = today()
  writeJson(metaPath, meta)
}

const ensureReadmeEntry = (version, title) => {
  const readmePath = path.join(versionRoot, 'README.md')
  const prev = readText(readmePath)
  if (!prev) return
  if (prev.includes(`- ${version}`)) return

  const line = `- ${version}（${title}）：见 \`docs/需求版本库/${version}/\`（入口：\`PRD.md\` / \`integrated.md\` / \`接口.md\` / \`distilled.md\`）`
  const parts = prev.split(/\r?\n/)
  const idx = parts.findIndex((l) => l.trim() === '## 版本清单')
  if (idx < 0) {
    parts.push('', '## 版本清单', '', line, '')
    writeText(readmePath, parts.join('\n'))
    return
  }
  let insertAt = parts.length
  for (let i = idx + 1; i < parts.length; i++) {
    const t = parts[i].trim()
    if (t.startsWith('## ') && i !== idx) {
      insertAt = i
      break
    }
  }
  parts.splice(insertAt, 0, line)
  writeText(readmePath, parts.join('\n'))
}

const runCli = () => {
  const args = process.argv.slice(2)
  const version = normalizeVersionText(args[0] || '')
  const title = String(args[1] || '').trim()
  if (!version || !/^\d+\.\d+\.\d+$/.test(version)) {
    process.stderr.write('Usage: node scripts/versioning/createVersion.js <x.y.z> <title>\n')
    process.exit(1)
  }
  if (!title) {
    process.stderr.write('Error: title is required.\n')
    process.exit(1)
  }

  ensureVersionDir(version, title)
  ensureReadmeEntry(version, title)
  rebuild()
  process.stdout.write(`version created: ${version} (${title})\n`)
}

module.exports = {
  ensureVersionDir,
  ensureReadmeEntry
}

if (require.main === module) {
  runCli()
}

