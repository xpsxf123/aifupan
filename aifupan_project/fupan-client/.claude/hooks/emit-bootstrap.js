/**
 * @description Claude Code 启动阶段的多 Agent 统一入口补丁。
 * 该脚本在会话开始或恢复时向 Claude 注入一段稳定、简短的基础治理上下文，
 * 让 Claude 不会停留在 `CLAUDE.md` 的映射层，而是继续回到 `agnet/` 单一来源。
 * 同时检测当前分支类型，追加版本文件检查提醒。
 */

const fs = require('fs')
const { execSync } = require('child_process')

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
 * @description 构造要注入给 Claude 的基础治理上下文，含分支版本提醒。
 * @returns {string} 供 additionalContext 使用的上下文文本。
 */
const buildContext = () => {
  const staticSummary = [
    '[Multi-Agent Bootstrap]',
    '- Canonical source: agnet/',
    '- Bridge files are loaders only, not the full rule pack.',
    '- After reading CLAUDE.md, MUST continue with agnet/BOOTSTRAP.md.',
    '- For implementation tasks, MUST read:',
    '  - agnet/AGENT.md',
    '  - agnet/TOOLS.md',
    '  - agnet/MEMORY.md',
    '  - agnet/INDEX.md',
    '- Knowledge path: agnet/wiki/KNOWLEDGE_GRAPH.md -> domain index -> detail page.',
    '- Skill priority: agnet/self_skills/INDEX.md -> agnet/skills/INDEX.md -> target SKILLS.md.',
    '- Governance changes MUST update agnet/CHANGE_LOG.md.',
    '- If bridge layer conflicts with agnet/, agnet/ wins.'
  ].join('\n')

  // 分支类型检测与版本文件检查提醒
  let branchContext = ''
  try {
    const branch = execSync('git branch --show-current', { encoding: 'utf8' }).trim()
    const parsed = parseBranchName(branch)

    if (parsed) {
      const lines = ['', '[Branch Version Check]']

      if (parsed.prefix === 'aiDev') {
        // aiDev 分支：Agent 自动任务，跳过版本追踪
        lines.push('- Branch type: aiDev (Agent 自动任务)')
        lines.push('- 跳过版本追踪，无需创建或更新版本文档。')
      } else if (parsed.prefix === 'dev') {
        // dev 分支：提醒检查版本开发目录
        const docPath = `docs/版本开发目录/${parsed.majorVersion}.md`
        lines.push(`- Branch type: dev (开发分支)`)
        lines.push(`- 请检查版本开发目录文件：${docPath}`)
        lines.push(`- 若 ${docPath} 不存在，需要创建该文件。`)
      } else if (parsed.prefix === 'test' || parsed.prefix === 'reslease') {
        // test/reslease 分支：提醒更新版本变更记录
        const changelogPath = `docs/版本变更/${parsed.majorVersion}.md`
        lines.push(`- Branch type: ${parsed.prefix} (测试/发布分支)`)
        lines.push(`- 代码变更需要更新版本变更记录：${changelogPath}`)
      }

      branchContext = lines.join('\n')
    }
  } catch {
    // 获取分支信息失败时静默跳过，不影响原有治理注入
  }

  return staticSummary + branchContext
}

/**
 * @description 主执行函数，输出 Claude Code hooks 约定的 JSON 结构。
 * @returns {void}
 */
const main = () => {
  const hookInput = JSON.parse(fs.readFileSync(0, 'utf8') || '{}')
  const eventName = hookInput?.hook_event_name || hookInput?.hookEventName || 'SessionStart'
  const additionalContext = buildContext()

  process.stdout.write(JSON.stringify({
    hookSpecificOutput: {
      hookEventName: eventName,
      additionalContext
    }
  }))
}

main()
