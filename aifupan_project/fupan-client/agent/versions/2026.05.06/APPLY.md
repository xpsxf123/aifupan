# Apply Patch: 2026.05.06（前端智能体）

## 适用范围

- 适用于已落地到私有化项目中的“前端智能体”，需要以补丁方式升级能力/规则/技能文档。

## 升级前检查

- 备份目标项目中的 `智能体模型/agent/` 目录
- 查看目标项目中 `智能体模型/agent/VERSION.json` 的 `current_version`

## 应用步骤（复制式补丁）

1) 将本补丁包目录复制到目标项目：

- 复制 `versions/2026.05.06/` 到目标 `智能体模型/agent/versions/2026.05.06/`

2) 覆盖拷贝补丁载荷：

- 将 `versions/2026.05.06/payload/` 下的文件，按相对路径覆盖到 `智能体模型/agent/` 根目录

示例（PowerShell，在目标项目根目录执行）：

```powershell
$agentRoot = ".\智能体模型\前端智能体"
Copy-Item -Path "$agentRoot\versions\2026.05.06\payload\*" -Destination $agentRoot -Recurse -Force
```

3) 更新版本号：

- 将 `智能体模型/agent/VERSION.json` 的 `current_version` 更新为 `2026.05.06`

## 验证点

- `TOOLS.md`：实现前强制优先复用 self_skills→skills→新建
- `wiki/岗位-工作流.md`：包含“项目接入/抽取复用/WAL 双写回”
- `skills/project_scaffolder/SKILLS.md`：包含 init_existing/build_new 的输入输出与推荐一键路径
- `skills/component_abstractor/SKILLS.md`：包含 public vs self_skills 的区分与强制复用优先级

## 回滚

- 使用升级前备份恢复 `智能体模型/agent/`
- 或仅回滚本次覆盖的 4 个文件（manifest.json 中列出）

