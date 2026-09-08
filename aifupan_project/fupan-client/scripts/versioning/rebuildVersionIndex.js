const path = require('path')
const {
  versionRoot,
  readJson,
  writeJson,
  today,
  listVersionDirs
} = require('./utils')

const buildEntry = (versionDirName) => {
  const versionDir = path.join(versionRoot, versionDirName)
  const metaPath = path.join(versionDir, 'meta.json')
  const meta = readJson(metaPath, null)
  if (!meta?.version) return null
  return {
    version: String(meta.version),
    title: String(meta.title || ''),
    dir: `docs/需求版本库/${versionDirName}`,
    canonical: {
      meta: `docs/需求版本库/${versionDirName}/meta.json`,
      ...(meta.canonical || {})
    }
  }
}

const rebuild = () => {
  const versionDirs = listVersionDirs()
  const versions = versionDirs
    .map(buildEntry)
    .filter(Boolean)

  const out = {
    schema: 'demand_version_index.v1',
    last_generated: today(),
    versions
  }

  const indexPath = path.join(versionRoot, 'version_index.json')
  writeJson(indexPath, out)
  process.stdout.write(`version_index.json updated (${versions.length} versions)\n`)
}

module.exports = {
  rebuild
}

if (require.main === module) {
  rebuild()
}

