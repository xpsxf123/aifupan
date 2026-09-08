const fs = require('fs')
const path = require('path')

const projectRoot = path.resolve(__dirname, '..', '..')
const versionRoot = path.join(projectRoot, 'docs', '需求版本库')
const templateRoot = path.join(versionRoot, '_templates')

const ensureDir = (dirPath) => {
  if (fs.existsSync(dirPath)) return
  fs.mkdirSync(dirPath, { recursive: true })
}

const readText = (filePath) => {
  try {
    return fs.readFileSync(filePath, 'utf8')
  } catch (e) {
    return ''
  }
}

const writeText = (filePath, content) => {
  ensureDir(path.dirname(filePath))
  fs.writeFileSync(filePath, String(content ?? ''), 'utf8')
}

const readJson = (filePath, fallback = null) => {
  try {
    if (!fs.existsSync(filePath)) return fallback
    const text = fs.readFileSync(filePath, 'utf8')
    if (!text) return fallback
    return JSON.parse(text)
  } catch (e) {
    return fallback
  }
}

const writeJson = (filePath, data) => {
  ensureDir(path.dirname(filePath))
  fs.writeFileSync(filePath, JSON.stringify(data ?? {}, null, 2), 'utf8')
}

const today = () => {
  const d = new Date()
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

const normalizeVersionText = (value = '') => {
  const text = String(value ?? '').trim()
  const m = text.match(/(\d+(?:\.\d+){0,3})/)
  return m?.[1] || ''
}

const fillTemplate = (templateText, vars = {}) => {
  return String(templateText ?? '').replace(/\{\{(\w+)\}\}/g, (raw, key) => {
    const v = vars?.[key]
    return v === undefined || v === null ? '' : String(v)
  })
}

const listVersionDirs = () => {
  if (!fs.existsSync(versionRoot)) return []
  return fs.readdirSync(versionRoot, { withFileTypes: true })
    .filter((d) => d.isDirectory())
    .map((d) => d.name)
    .filter((name) => /^\d+\.\d+\.\d+$/.test(name))
    .sort((a, b) => a.localeCompare(b, 'zh-CN', { numeric: true }))
}

module.exports = {
  projectRoot,
  versionRoot,
  templateRoot,
  ensureDir,
  readText,
  writeText,
  readJson,
  writeJson,
  today,
  normalizeVersionText,
  fillTemplate,
  listVersionDirs
}

