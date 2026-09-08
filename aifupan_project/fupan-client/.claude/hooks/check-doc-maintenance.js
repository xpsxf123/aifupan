/**
 * @description Claude Code 在文件修改成功后执行的文档维护检查脚本。
 * 用于在 PostToolUse 阶段识别是否改动了提示词工程治理相关文件，
 * 并向 Claude 注入"需要同步检查 CHANGE_LOG / POLICIES / 兼容文档"的提醒。
 * 同时进行分支版本管理检查和重复分支冲突检测。
 */

const fs = require('fs')
const path = require('path')
const { execSync } = require('child_process')

/**
 * @description 规范化路径分隔符，便于统一匹配。
 * @param {string} value 原始路径。
 * @returns {string} 统一为正斜杠的路径。
 */
function normalizePath(value = '') {
  return String(value || '').replace(/\\/g, '/')
}

/**
 * @description 从 PostToolUse 输入中提取本次修改涉及的文件路径。
 * @param {Record<string, any>} payload Hook 输入。
 * @returns {string[]} 文件路径列表。
 */
function collectFilePaths(payload = {}) {
  const toolInput = payload.tool_input || {}
  const candidates = [
    toolInput.file_path,
    toolInput.path,
    toolInput.target_file,
    toolInput.uri
  ].filter(Boolean)

  if (Array.isArray(toolInput.file_paths)) {
    candidates.push(...toolInput.file_paths)
  }

  return [...new Set(candidates.map(normalizePath).filter(Boolean))]
}

/**
 * @description 判断路径是否命中治理/兼容层文件。
 * @param {string[]} filePaths 本次修改的文件路径。
 * @returns {{ governanceTouched: boolean, docsTouched: boolean, touchedPaths: string[] }}
 */
function classifyTouchedFiles(filePaths = []) {
  const governancePrefixes = [
    'agnet/',
    '.claude/',
    '.trae/'
  ]
  const docsPrefix = 'docs/提示词设计-多种agent工具提示词工程兼容/'

  const touchedPaths = filePaths.filter((item) => {
    const safeItem = normalizePath(item)
    return governancePrefixes.some((prefix) => safeItem.includes(prefix))
      || safeItem.endsWith('/CLAUDE.md')
      || safeItem === 'CLAUDE.md'
      || safeItem.includes(docsPrefix)
  })

  return {
    governanceTouched: touchedPaths.some((item) => {
      const safeItem = normalizePath(item)
      return governancePrefixes.some((prefix) => safeItem.includes(prefix))
        || safeItem.endsWith('/CLAUDE.md')
        || safeItem === 'CLAUDE.md'
    }),
    docsTouched: touchedPaths.some((item) => normalizePath(item).includes(docsPrefix)),
    touchedPaths
  }
}

/**
 * @description 生成注入给 Claude 的维护提醒。
 * @param {{ governanceTouched: boolean, docsTouched: boolean, touchedPaths: string[] }} info 分类信息。
 * @returns {string} 追加上下文。
 */
function buildAdditionalContext(info) {
  const touchedPreview = info.touchedPaths.slice(0, 6).join(', ')

  if (!info.governanceTouched && !info.docsTouched) {
    return ''
  }

  const lines = [
    '[Doc Maintenance Check]',
    '- You edited governance or compatibility files.',
    touchedPreview ? `- Touched: ${touchedPreview}` : '',
    '- Before finishing, review whether agnet/CHANGE_LOG.md must be updated.',
    '- If this change adds a stable multi-agent rule, review agnet/POLICIES.md.',
    '- If bridge behavior changed, review the compatibility design docs folder under docs/.'
  ].filter(Boolean)

  if (!info.docsTouched) {
    lines.push('- Compatibility docs were not edited in this tool step; confirm whether they should be synchronized.')
  }

  return lines.join('\n')
}

/**
 * @description 解析分支名，提取前缀、大版本号和特性序号。
 * @param {string} branch Git 分支名。
 * @returns {{ prefix: string, majorVersion: string, featureIndex?: string } | null}
 */
function parseBranchName(branch) {
  const match = branch.match(/^(dev|test|reslease|aiDev)-(\d+\.\d+\.\d+)(?:\.(\d+))?$/)
  if (!match) return null
  return { prefix: match[1], majorVersion: match[2], featureIndex: match[3] }
}

/**
 * @description 获取当前分支信息。
 * @returns {string | null} 当前分支名，失败时返回 null。
 */
function getCurrentBranch() {
  try {
    return execSync('git branch --show-current', { encoding: 'utf8' }).trim()
  } catch {
    return null
  }
}

/**
 * @description 判断文件路径列表是否包含 src/ 下的文件。
 * @param {string[]} filePaths 文件路径列表。
 * @returns {boolean}
 */
function hasSrcChanges(filePaths = []) {
  return filePaths.some((fp) => {
    const normalized = normalizePath(fp)
    return normalized.startsWith('src/') || normalized.includes('/src/')
  })
}

/**
 * @description 分支版本管理检查：根据分支类型和修改文件生成提醒。
 * @param {{ prefix: string, majorVersion: string, featureIndex?: string }} branchInfo 分支解析信息。
 * @param {string[]} filePaths 本次修改的文件路径。
 * @returns {string} 版本管理提醒文本。
 */
function buildBranchVersionContext(branchInfo, filePaths) {
  // aiDev 前缀跳过所有检查
  if (branchInfo.prefix === 'aiDev') {
    return ''
  }

  const hasSrc = hasSrcChanges(filePaths)
  if (!hasSrc) {
    return ''
  }

  if (branchInfo.prefix === 'dev') {
    // dev 分支修改了 src/ 文件：提醒更新版本开发目录
    const docPath = `docs/版本开发目录/${branchInfo.majorVersion}.md`
    return [
      '[Branch Version Management]',
      `- Branch: dev (开发分支)，已修改 src/ 下的文件`,
      `- 请检查并更新版本开发目录文件：${docPath}`,
      `- 若 ${docPath} 不存在，需要创建该文件。`
    ].join('\n')
  }

  if (branchInfo.prefix === 'test' || branchInfo.prefix === 'reslease') {
    // test/reslease 分支修改了 src/ 文件：提醒更新版本变更记录
    const changelogPath = `docs/版本变更/${branchInfo.majorVersion}.md`
    return [
      '[Branch Version Management]',
      `- Branch: ${branchInfo.prefix} (测试/发布分支)，已修改 src/ 下的文件`,
      `- 代码变更需要更新版本变更记录：${changelogPath}`
    ].join('\n')
  }

  return ''
}

/**
 * @description 从版本开发目录文件中提取指定 dev 分支的变更文件列表。
 * 解析 markdown 中以 `## dev-X.Y.Z.N` 开头的区块，提取该区块下的文件路径。
 * @param {string} docContent 版本开发目录文件内容。
 * @param {string} branchName 当前分支名（完整，如 dev-2.60.3.1）。
 * @returns {string[]} 其他 dev 分支的变更文件路径列表。
 */
function extractOtherDevBranchFiles(docContent, branchName) {
  const result = []
  // 按 ## dev- 分割区块
  const sections = docContent.split(/^##\s+(?=dev-)/m)
  for (const section of sections) {
    // 提取当前区块的分支名
    const headerMatch = section.match(/^##\s+(dev-\S+)/m)
    if (!headerMatch) continue
    const sectionBranch = headerMatch[1].trim()
    // 跳过当前分支自身
    if (sectionBranch === branchName) continue
    // 提取区块中的文件路径（匹配 src/ 或带路径的文件引用）
    const pathMatches = section.match(/(?:src\/|views\/|components\/|utils\/|assets\/|enums\/|pages\/)[^\s)\]]+/g)
    if (pathMatches) {
      pathMatches.forEach((p) => {
        const cleaned = p.replace(/[,;，；]$/, '').trim()
        if (cleaned) result.push(normalizePath(cleaned))
      })
    }
  }
  return [...new Set(result)]
}

/**
 * @description 重复分支冲突检测：仅 dev-* 分支触发。
 * 读取同大版本下其他 dev- 分支的变更文件列表，与当前修改文件做交集比对。
 * @param {{ prefix: string, majorVersion: string, featureIndex?: string }} branchInfo 分支解析信息。
 * @param {string[]} filePaths 本次修改的文件路径。
 * @returns {string} 冲突提醒文本。
 */
function buildConflictContext(branchInfo, filePaths) {
  // 仅 dev-* 前缀触发冲突检测
  if (branchInfo.prefix !== 'dev') {
    return ''
  }

  const docPath = `docs/版本开发目录/${branchInfo.majorVersion}.md`
  const repoRoot = path.resolve(__dirname, '..', '..')
  const fullDocPath = path.join(repoRoot, docPath)

  let docContent
  try {
    docContent = fs.readFileSync(fullDocPath, 'utf8')
  } catch {
    // 文件不存在则跳过冲突检测
    return ''
  }

  const currentBranch = `dev-${branchInfo.majorVersion}${branchInfo.featureIndex !== undefined ? '.' + branchInfo.featureIndex : ''}`
  const normalizedCurrent = filePaths.map(normalizePath)
  const otherBranchFiles = extractOtherDevBranchFiles(docContent, currentBranch)

  // 与当前修改文件做交集比对
  const conflicts = normalizedCurrent.filter((fp) => {
    return otherBranchFiles.some((otherFp) => {
      // 完全匹配或去掉首段后的路径匹配
      return fp === otherFp || fp.endsWith('/' + otherFp.split('/').slice(1).join('/'))
    })
  })

  if (conflicts.length === 0) {
    return ''
  }

  const conflictPreview = conflicts.slice(0, 5).join(', ')
  const more = conflicts.length > 5 ? `  ...等共 ${conflicts.length} 个文件` : ''

  return [
    '[Branch Conflict Detection]',
    `- 当前分支 ${currentBranch} 修改的文件与同大版本下其他 dev- 分支存在冲突：`,
    `- ${conflictPreview}${more}`,
    `- 请确认这些文件是否被多个开发分支并行修改，评估合并冲突风险。`
  ].join('\n')
}

/**
 * @description 主执行函数。
 * @returns {void}
 */
function main() {
  try {
    const rawInput = fs.readFileSync(0, 'utf8') || '{}'
    const payload = JSON.parse(rawInput)
    const toolName = String(payload.tool_name || '')

    if (!['Write', 'Edit', 'MultiEdit'].includes(toolName)) {
      process.stdout.write('{}')
      return
    }

    const filePaths = collectFilePaths(payload)

    // 原有治理文件维护检查
    const info = classifyTouchedFiles(filePaths)

    // 分支版本管理检查
    const branch = getCurrentBranch()
    const branchInfo = branch ? parseBranchName(branch) : null

    const contexts = []
    const govContext = buildAdditionalContext(info)
    if (govContext) contexts.push(govContext)

    if (branchInfo) {
      const versionContext = buildBranchVersionContext(branchInfo, filePaths)
      if (versionContext) contexts.push(versionContext)

      const conflictContext = buildConflictContext(branchInfo, filePaths)
      if (conflictContext) contexts.push(conflictContext)
    }

    if (contexts.length === 0) {
      process.stdout.write('{}')
      return
    }

    process.stdout.write(JSON.stringify({
      hookSpecificOutput: {
        hookEventName: 'PostToolUse',
        additionalContext: contexts.join('\n\n')
      }
    }))
  } catch {
    // 任何异常都输出空对象
    process.stdout.write('{}')
  }
}

main()
