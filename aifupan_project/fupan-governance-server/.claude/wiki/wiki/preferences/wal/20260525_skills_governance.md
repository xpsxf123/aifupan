# [Rules] Project Conventions & Governance

> Date: 2026-05-25
> Source: distilling from `.agents/llm_wiki/wiki/preferences/wal/applied/20260506_skills_governance.md`

## Permission Identifier Convention

权限标识以 Controller 上的 `@Permissions("...")` 为准。常见命名模式：

| 模块 | 权限前缀 | 示例 |
|---|---|---|
| org | `org:<resource>:manage:<action>` | `org:dept:manage:add` |
| room (管理) | `room:mgmt:*` | `room:mgmt:add` |
| room (排班) | `room:schedule:*` | `room:schedule:list` |
| sys | `sys:employee:manage:*` | `sys:employee:manage:add` |

## UI/UX Workflow Conventions

- **删除确认**: 所有删除操作必须有弹出确认框 (`ElMessageBox.confirm`)
- **操作反馈**: 增删改操作成功后调用 `this.$message.success('操作成功')` 提示
- **刷新联动**: 当前页面修改数据后必须调用 `this.getList()` 刷新列表

## File Naming (Cross-Platform Safety)

- 文件名禁止出现 Windows 不允许的字符（重点是 `:`）
- 时间戳默认格式：`yyyy-MM-dd_HH-mm-ss`（用 `-` 替代 `:`）
- 脚本对 prefix/slug 做文件名净化兜底：非法字符替换为 `-`
