# Claude Code 兼容补丁落地说明

## 1. 本次改动的目的

本次改动不是把 `agnet/` 复制一份给 Claude Code，而是给 Claude Code 增加一套“访问补丁”，使其能够：

- 自动读取统一入口
- 自动读取根目录 `CLAUDE.md` 与 `.claude/CLAUDE.md`
- 自动加载高优先级治理规则
- 在会话开始/恢复/压缩后重新拿回基础治理上下文
- 在文件修改后自动触发文档维护检查
- 继续回到 `agnet/` 读取真实单一来源

本补丁的关键价值在于：它可以被抽象为“跨工具适配器方案”，由其他 Agent 工具按照自己的入口读取规则复刻实现（见同目录的 `补丁包与适配器抽象方法.md` 与 `patch/` 模板包）。

## 2. 本次新增/修改的文件

### 2.1 统一入口层

- `agnet/BOOTSTRAP.md`
  - 新增
  - 定义统一读取顺序、任务场景链路、写回规则、多 Agent 兼容边界

### 2.2 Claude Code 入口桥

- `CLAUDE.md`
  - 新增
  - Claude Code 的仓库根入口桥
  - 承载简短摘要与关键规则内联
- `.claude/CLAUDE.md`
  - 新增
  - Claude Code 的项目详细配置入口
  - 承载详细规则与继续读取要求

### 2.3 Claude Code 规则分片

- `.claude/rules/00-core-governance.md`
- `.claude/rules/10-context-funnel.md`
- `.claude/rules/20-writeback-and-change-log.md`
- `.claude/rules/30-multi-agent-compatibility.md`

作用：

- 把始终生效的关键规则拆成小文件
- 利用 Claude Code 原生规则目录机制，在会话开始时自动加载

### 2.4 Claude Code 自动注入

- `.claude/hooks/emit-bootstrap.js`
- `.claude/hooks/check-doc-maintenance.js`
- `.claude/settings.json`

作用：

- 通过 `SessionStart` hook 在启动、恢复、清理、压缩时重新注入基础治理摘要
- 通过 `PostToolUse` hook 在文件修改成功后检查是否需要补维护 `CHANGE_LOG`、`POLICIES` 与兼容文档
- 避免上下文压缩后丢失“单一来源/继续读取/CHANGE_LOG/WAL”等核心规则

## 3. 各文件职责说明

## 3.1 `agnet/BOOTSTRAP.md`

它是整个补丁的中心文件，职责包括：

- 明确 `agnet/` 是单一来源
- 明确不同 Agent 只能做桥接，不能复制出独立业务知识库
- 明确“读取本文件后必须继续读取哪些文件”
- 明确不同任务场景对应的下游文件链路
- 明确写回与治理边界

它解决的问题是：

- 原有入口文件更像索引，不像执行入口
- Agent 看到了路径，但没有得到继续读取要求

## 3.2 `CLAUDE.md` 与 `.claude/CLAUDE.md`

Claude Code 会合并读取这两个文件，因此它们应采用“双入口分工”：

- 根目录 `CLAUDE.md`
  - 负责简短摘要
  - 负责内联少量关键治理规则
  - 负责告诉 Claude Code 继续进入 `.claude/CLAUDE.md`
- `.claude/CLAUDE.md`
  - 负责详细配置
  - 负责告诉 Claude Code 继续进入 `agnet/BOOTSTRAP.md`
  - 负责承载文档维护规则与桥接边界

它们都不负责：

- 存放完整业务规则
- 维护完整技能、wiki、memory 内容

## 3.3 `.claude/rules/*.md`

这些规则文件负责“始终生效”的硬提醒。

拆分原因：

- 大而全的单文件容易被忽略
- 拆成小文件后，规则的主题边界更清晰
- 便于后续继续增加 path-specific 规则

四个分片分别负责：

- `00-core-governance.md`
  - 单一来源、继续读取、冲突优先级
- `10-context-funnel.md`
  - 图谱入口、域索引、技能优先级、按需加载
- `20-writeback-and-change-log.md`
  - WAL、MEMORY、POLICIES、CHANGE_LOG 的边界
- `30-multi-agent-compatibility.md`
  - 多 Agent 共仓的桥接边界

## 3.4 `emit-bootstrap.js`

该脚本是 Claude Code hook 的执行体。

它的逻辑很简单：

1. 读取 `agnet/BOOTSTRAP.md`
2. 生成一段稳定且较短的基础治理摘要
3. 通过 hooks 输出 `additionalContext`
4. 把这段摘要注入 Claude Code 当前会话

这样即使：

- 新开会话
- 恢复会话
- `/clear`
- `/compact`

Claude Code 仍会重新收到一份“必须回到 `agnet/` 继续读取”的提醒。

## 3.5 `check-doc-maintenance.js`

该脚本是 `PostToolUse` hook 的执行体。

它的职责是：

1. 在 `Write/Edit/MultiEdit` 成功后读取 hook 输入
2. 识别本次是否修改了以下治理相关区域：
   - `agnet/`
   - `.claude/`
   - `CLAUDE.md`
   - `.trae/`
   - `docs/提示词设计-多种agent工具提示词工程兼容/`
3. 若命中治理文件，则向 Claude 注入“检查 `CHANGE_LOG` / `POLICIES` / 兼容文档”的提醒

它解决的问题是：

- 规则明明存在，但模型改完文件后未必会主动回头维护文档
- 文档维护完全依赖自觉，缺少执行后检查

## 4. 为什么同时使用 SessionStart 与 PostToolUse

Claude Code 的问题不是“完全读不到入口”，而是：

- 读到入口后不一定继续读
- 会话压缩后容易丢失基础治理上下文

因此，`SessionStart` 与 `PostToolUse` 需要组合使用：

- `SessionStart`
  - 解决“开局没有拿到基础治理规则”的问题
- `PostToolUse`
  - 解决“改完文件后没有回头维护文档”的问题

其中，`SessionStart` 适合：

- 启动时触发
- 恢复时触发
- 清理时触发
- 压缩时触发

而 `PostToolUse` 适合：

- `Write`
- `Edit`
- `MultiEdit`

这让“基础治理再注入”和“执行后文档维护检查”都变成稳定机制，而不是靠模型记忆。

## 5. 为什么没有把所有知识都塞进 hook

因为 hooks 的职责应该是“兜底注入”，不是替代知识库。

如果把全部知识都通过 hook 注入，会带来：

- 上下文膨胀
- 噪声增加
- 具体任务信号被淹没
- 后续难以维护

所以本方案只把下面这些内容作为 hook 注入对象：

- 单一来源目录是谁
- 入口桥不是单一来源
- 必须继续读取哪些核心文件
- wiki / skills / CHANGE_LOG / WAL 的使用边界

而不把：

- 全量 wiki
- 全量 skills
- 全量 memory

塞进 hook。

## 6. 为什么要在入口文件内联关键规则

如果入口文件只有：

- `→ agnet/AGENT.md`
- `→ agnet/CHANGE_LOG.md`

那么 Claude Code 看见的只是路径指针，而不是高优先级治理规则。

因此根目录 `CLAUDE.md` 必须内联少量关键规则：

- 单一来源是谁
- 继续读取到哪里
- 哪些变更必须维护 `CHANGE_LOG`
- `WAL` 与技能/知识读取的高优先级规则

这样即使 Claude Code 在当前轮只吸收了入口文件，也不会完全丢失治理约束。

## 7. 如何继续扩展

如果后续还要增强 Claude Code 兼容能力，推荐按下面顺序扩展：

### 6.1 增加 path-specific rules

例如：

- 当进入 `src/` 时，自动补充前端代码规范
- 当进入 `docs/` 时，自动补充文档治理规则
- 当进入 `agnet/` 时，自动补充提示词工程治理规则

### 7.2 增加 Stop / PostToolUse 补强

例如：

- 检测当次会话是否修改了 `agnet/` 但未更新 `CHANGE_LOG.md`
- 检测规则文件变更后是否忘记同步 `.trae/rules/`

### 7.3 增加桥接清单的机器可读配置

例如新增一个 JSON 清单，声明：

- 单一来源目录
- 桥接入口列表
- 核心必读文件列表
- hooks 注入摘要来源

这样其他 Agent 或自动化脚本可以直接读取清单，而不必手写路径。

## 8. 与 Trae / 其他 Agent 的关系

本方案不是替代 Trae 现有机制，而是补齐 Claude Code 这一侧的兼容层。

当前分工应为：

- `agnet/`
  - 所有工具共享的真实单一来源
- `.trae/`
  - Trae 发现层与规则镜像
- `CLAUDE.md + .claude/`
  - Claude Code 发现层与自动加载补丁

未来如果接入其他 Agent，也应采用同样原则：

- 新增自己的入口桥
- 桥接回 `agnet/BOOTSTRAP.md`
- 不复制一套新知识库

## 9. 验收标准

本补丁是否有效，可以用以下问题验证：

1. Claude Code 启动时，是否能自动拿到“单一来源在 `agnet/`”这一事实？
2. Claude Code 是否同时读取了根目录 `CLAUDE.md` 与 `.claude/CLAUDE.md`？
3. Claude Code 看到入口后，是否明确知道必须继续读 `agnet/BOOTSTRAP.md`？
4. Claude Code 是否能通过 `.claude/rules/` 自动拿到基础治理规则？
5. Claude Code 在 `/compact` 或恢复会话后，是否还能收到基础治理摘要？
6. Claude Code 在修改治理文件后，是否会收到文档维护检查提醒？
7. 项目是否仍然只有一套真实提示词工程目录？

如果以上五点都成立，则说明本次兼容补丁落地成功。
