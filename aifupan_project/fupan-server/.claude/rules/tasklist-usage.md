# TaskList Usage Rules

Claude Code 内置 `TaskCreate` / `TaskUpdate` / `TaskList` 工具使用规则。其他 rules / commands 按需 / 强制引用本文件。

加载方式：
- **按需** — 主 agent 进入对应场景前 `Read` 本文件
- **强制** — `lifecycle.md` Phase 4 / `commands/h-fix-bug.md` / `commands/h-pr.md` 在 step 1 明文指示按本文件 Pattern X 起 TaskList

---

## 1. When to USE

| 场景 | 触发条件 | Pattern |
|---|---|---|
| 主 agent 收到 ≥3 个具体可执行动作 | 用户一句话列出多任务 / 改造清单 / 评审清单 | A — 平铺 |
| Implement 阶段 AC 迭代 | 进入 Phase 4 + openspec §7 有 ≥3 个 AC | B — AC 映射（强制） |
| `/h-fix-bug` 入场 | 命令 step 1 末 | C — 5-task pipeline（强制）|
| `/h-pr` 入场 | 命令 step 1 末 | D — 4-task pipeline（强制）|

---

## 2. When NOT to use

| 场景 | 原因 |
|---|---|
| Vibe / 一行修 / typo / 解释代码 | 单步无需追踪 |
| PATCH 单文件改 | scope 已锁，无中间状态 |
| 单 dispatch + 单返回（`/h-design` / `/h-resume` / `/h-gates` 等） | 一步无需 list |
| sub-agent 内部步骤 | 主 agent 不可见，TaskList 反而误导 |
| 已被 `launch_spec.Phase` 覆盖的任务级 | 双重维护 |
| 已被 `current_task.md` 覆盖的 phase 级 | 双重维护 |
| 已被 `collab_state.md` 覆盖的跨会话状态 | TaskList session-scoped 丢失跨会话信息 |
| Standard 6 phases 整体 | 与 `launch_spec.Phase` 直接重叠 |
| `/h-decompose` 产生的 N slice | slice 即 launch_spec 行 |

---

## 3. Patterns

### Pattern A — 平铺 task（主 agent 自主）

- 触发：≥3 个具体动作。
- 每个 task = 一个具体动作（动词短语）。
- `activeForm` = 进行中形式。
- 开工前先 `TaskList` 查重，避免重复创建。

### Pattern B — AC → task 映射（Implement Phase 4 强制）

- 入场动作：读 openspec §7，每个 AC 起一个 task。
- `subject` = AC-N 一句话（去 Given/Then 前缀）。
- `activeForm` = `验证 AC-N: <一句话>`。
- 转 `in_progress` 时点：开始为该 AC 写代码 / 修改 / 跑测试。
- 转 `completed` 时点：该 AC 关联代码编译 + 测试 / 手工验证通过。
- AC <3 时跳过本 pattern（直接做不起 list）。

### Pattern C — `/h-fix-bug` 5-task pipeline（强制）

固定 5 task；p1/p2 + production 时在 task 4 后插入 1 个 "Record incident"。

| Subject | activeForm |
|---|---|
| Collect bug + derive slug | 收集 bug 详情 |
| Query incidents/ for similar | 查相似历史事故 |
| Dispatch @debugger for root cause | 派 @debugger 找根因 |
| Create launch_spec row | 起 launch_spec 行 |
| (生产事故) Record incident inline | 录 incident |
| Confirm fix scope with user | 等用户确认 fix scope |

### Pattern D — `/h-pr` 4-task pipeline（强制）

| Subject | activeForm |
|---|---|
| Run pre-PR gates (secrets + git status) | 跑 pre-PR gate |
| Compose MR title + body | 拼 MR title + body |
| User create MR in 云效 UI | 等用户在云效创建 MR |
| Writeback URL to launch_spec + openspec | 回填 URL |

---

## 4. Hard rules

- 起新 task 前**必须** `TaskList` 查重。
- 同时只允许 1 个 task 处于 `in_progress`。
- 完成立即标 `completed`，**禁批量**。
- TaskList 与 `launch_spec` / `current_task` / `openspec §7` / `collab_state` **内容禁重叠**——重叠即错用。
- `subject` 必须是动词短语；`activeForm` 必须是"进行中"语态。
- 会话结束前 `deleted` 掉 stale task（≥2h 未动 + 非 `in_progress`）。
- sub-agent dispatch **禁创建** main-agent TaskList task 覆盖其内部步骤——主 agent 不可见的事不进 list。
