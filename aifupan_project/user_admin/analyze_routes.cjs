const fs = require('fs')
const path = require('path')

const menuContent = fs.readFileSync('src/config/menu.js', 'utf8')

// Find all `isNav: false` and their names/paths
const notNavPages = []
const navRegex = /path:\s*'([^']+)',\s*name:\s*'([^']+)',[\s\S]*?meta:\s*\{[^}]*isNav:\s*false[^}]*\}/g
let match
while ((match = navRegex.exec(menuContent)) !== null) {
  notNavPages.push({ path: match[1], name: match[2] })
}

console.log('Not in nav pages:')
console.log(notNavPages)

const viewsPath = 'src/views'
const getAllVueFiles = (dir) => {
  let results = []
  const list = fs.readdirSync(dir)
  list.forEach((file) => {
    const filePath = path.join(dir, file)
    const stat = fs.statSync(filePath)
    if (stat && stat.isDirectory()) {
      results = results.concat(getAllVueFiles(filePath))
    } else if (file.endsWith('.vue')) {
      results.push(filePath)
    }
  })
  return results
}

const allVueFiles = getAllVueFiles(viewsPath)
const unlinkedPages = []

allVueFiles.forEach((file) => {
  // convert to standard path format used in menu.js e.g. views/Login/index
  let relPath = path.relative('src', file).replace(/\\/g, '/')
  if (relPath.endsWith('.vue')) {
    relPath = relPath.slice(0, -4)
  }

  if (!menuContent.includes(relPath)) {
    unlinkedPages.push(relPath)
  }
})

console.log('Unlinked pages:')
console.log(unlinkedPages)
