const fs = require('fs')
const path = require('path')
const { execSync } = require('child_process')

const projectRoot = path.resolve(__dirname, '..', '..')
const hooksDir = path.join(projectRoot, '.githooks')
const preCommitPath = path.join(hooksDir, 'pre-commit')

const ensureDir = (dirPath) => {
  if (fs.existsSync(dirPath)) return
  fs.mkdirSync(dirPath, { recursive: true })
}

const run = () => {
  ensureDir(hooksDir)
  if (!fs.existsSync(preCommitPath)) {
    process.stderr.write('Error: .githooks/pre-commit not found. Please ensure repository files are up to date.\n')
    process.exit(1)
  }

  try {
    execSync('git config core.hooksPath .githooks', { cwd: projectRoot, stdio: 'inherit' })
    process.stdout.write('git hooks installed: core.hooksPath=.githooks\n')
  } catch (_) {
    process.stderr.write('Error: failed to install git hooks (git config core.hooksPath .githooks)\n')
    process.exit(1)
  }
}

run()

