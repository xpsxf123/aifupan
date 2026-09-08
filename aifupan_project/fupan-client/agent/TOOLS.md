# TOOLS（通用）

本文件定义“行业智能体”可使用的工具与技能组织方式。技能分为通用 `skills/` 与项目绑定 `self_skills/`。

## 技能组织

- `skills/INDEX.md`：通用技能清单（按任务场景分组）
- `self_skills/INDEX.md`：项目/业务绑定技能清单
- 每个技能一个文件夹：
  - `skills/<skill_name>/SKILLS.md` + `skills/<skill_name>/templates/`
  - `self_skills/<skill_name>/SKILLS.md` + `self_skills/<skill_name>/templates/`
- 技能必须可复用、可组合、可审计（输入/输出明确）

## 使用策略

- 先查 `INDEX.md` 决定需要加载的 wiki/技能
- 实现页面/组件前强制优先复用 `self_skills/`（项目已存在的相似组件/工具/模式），再复用通用 `skills/`
- 缺失时才新建技能，并在对应的 `INDEX.md` 登记
- 输出重要结论后回写 wiki，并在 INDEX 建立关系

## doc/ 加载限制

- 行业智能体默认不加载项目根 `doc/`
- 只有当用户明确指定或 INDEX 指示必须查阅时才加载少量页面
