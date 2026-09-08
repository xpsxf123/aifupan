---
description: 生成测试参考文档（复现步骤 / 影响范围 / 测试范围）到 docs/ 目录。基于当前会话改动或 git 提交历史。
argument-hint: [<commit-ref>] [--source session|git]
allowed-tools: Read, Write, Bash, Grep, Glob
---

输入：`$ARGUMENTS`

生成一份给测试人员参考的文档，写入 `docs/` 目录。内容强制表/列表格式，禁叙事文段落。

## 步骤

### 1. 检测改动来源

**优先检测当前会话改动：**

```bash
git diff --name-only
```

```bash
git diff --cached --name-only
```

有输出 → `--source session`，跳 step 2。
无输出 → `--source git`，进 step 2。

### 2. Git 来源 — 提供选项

列出最近提交供用户选择：

```bash
git log --oneline -15
```

`AskUserQuestion`（单题 4 选 1）：

| header | options |
|---|---|
| `选择要分析的改动范围` | 最近 1 个提交 / 最近 3 个提交 / 最近 5 个提交 / 指定提交或分支 |

- 选"指定提交或分支" → follow-up 收自然语言 ref（如 `HEAD~3..HEAD` / `abc1234` / `feature-x`）。
- 其余选项自动构造 `<ref>`：`HEAD~1` / `HEAD~3` / `HEAD~5`。

解析 ref 后获取改动文件列表和 diff：

```bash
git diff --name-only <ref>
```

```bash
git diff <ref> --stat
```

### 3. 收集改动上下文

读改动文件列表，分类：

| 文件类型 | 提取信息 |
|---|---|
| Controller (`**/controller/**`) | API 路径、HTTP 方法、参数 |
| Service/ServiceImpl | 业务方法签名 |
| Entity / 数据库实体 | 表名、字段变更 |
| Mapper / Dao | SQL 操作类型 |
| SQL 文件 (`sql/**`) | DDL/DML 内容 |
| VO / DTO / Bo | 字段变更 |
| 配置文件 (`application*.yml`, `bootstrap*.yml`) | 配置项变更 |

**对每个改动文件：**
- `grep` 关键注解（`@PostMapping`、`@GetMapping`、`@RequestMapping`、`@Transactional`）
- `grep` 表名引用（`@TableName`、`FROM`、`INSERT INTO`）
- `git diff <ref> -- <file>` 获取具体变更内容

### 4. 生成文档

文件名：`docs/test-doc__<YYYYMMDD_HHmmss>.md`

文档模板（强制表/列表格式，禁叙事段落）：

```markdown
# 测试参考文档

> 生成时间：<YYYY-MM-DD HH:mm:ss>
> 改动来源：<session | git <ref>>
> 涉及模块：<模块列表>

---

## 1. 变更概览

| 文件 | 模块 | 变更类型 | 变更摘要 |
|------|------|----------|----------|
| path/to/File.java | replay-xxx | 新增/修改/删除 | 一句话说明 |
| ... | ... | ... | ... |

## 2. 复现步骤

### 2.1 前置条件

- 租户/用户：<描述需要的测试账号条件>
- 数据：<描述需要的测试数据>
- 配置：<描述需要的配置变更>

### 2.2 复现操作

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | <具体操作> | <可观测结果> |
| 2 | <具体操作> | <可观测结果> |

## 3. 影响范围

### 3.1 API 影响

| API 路径 | 方法 | 变更类型 | 说明 |
|----------|------|----------|------|

（无 API 变更则写 `无`）

### 3.2 数据库影响

| 表名 | 变更类型 | 说明 |
|------|----------|------|

（无 DB 变更则写 `无`）

### 3.3 下游影响

| 影响对象 | 影响类型 | 说明 |
|----------|----------|------|

（Feign 接口 / MQ 消息 / 定时任务 / 缓存 key 等，无则写 `无`）

## 4. 测试范围

### 4.1 功能测试

| 测试场景 | 优先级 | 测试要点 |
|----------|--------|----------|
| <场景名> | P0/P1/P2 | <验证要点> |

### 4.2 回归测试

| 回归范围 | 风险等级 | 说明 |
|----------|----------|------|

### 4.3 边界/异常测试

| 场景 | 输入条件 | 预期行为 |
|------|----------|----------|

## 5. 测试环境要求

| 项目 | 说明 |
|------|------|
| 分支 | <branch> |
| 配置 | <特殊配置项> |
| 依赖服务 | <需要启动的关联服务> |
| 数据准备 | <初始化 SQL 或脚本> |
```

**填充规则：**
- 每个 section 必须至少有一行数据或明确写 `无`，不留空 section
- `变更类型` 列取值限定：`新增` / `修改` / `删除`
- `优先级` 列取值限定：`P0`（核心路径）/ `P1`（重要分支）/ `P2`（边缘场景）
- 无对应内容的 section 写 `无`，不删 section

### 5. 输出

```
[Test Doc] file=docs/test-doc__<timestamp>.md
  modules: <涉及模块数>
  files: <改动文件数>
  apis: <涉及 API 数>
  tables: <涉及 DB 表数>
```

## 硬约束

- 内容格式强制表/列表，禁叙事文段落。发现叙事段落 → 重写为列表。
- 文件名带时间戳，避免覆盖历史文档。
- 不改源码、不改配置、不改 wiki。
- `docs/` 目录不存在时自动创建。
- 改动文件数为 0 时停 `No changes detected — nothing to document.`。
- 仅读 git 信息和源码，不做任何写操作（除写 docs/ 文件本身）。
