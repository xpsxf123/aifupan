# Context Funnel

本规则文件用于强制 Claude Code 沿索引继续读取，而不是全量扫描或停在入口层。

## 正向漏斗

读取顺序必须为：

1. `agnet/BOOTSTRAP.md`
2. `agnet/INDEX.md`
3. `agnet/wiki/KNOWLEDGE_GRAPH.md`
4. 任务相关域的 `agnet/wiki/<domain>/index.md`
5. 仅在域索引不足时，下沉读取具体 wiki 页面或代码文件

## 技能优先级

执行页面/组件/治理方案前，优先级必须为：

1. `agnet/self_skills/INDEX.md`
2. `agnet/skills/INDEX.md`
3. 目标技能目录下的 `SKILLS.md`

## 范围控制

- 默认不做全量加载。
- 只展开当前任务涉及的域与技能。
- 若任务复杂，先输出范围、触达路径、非范围与验证方式，再继续执行。
