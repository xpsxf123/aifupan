---
name: "Wiki知识图谱构建器"
description: "从本次交付/对话/产物抽取实体关系，更新 INDEX.md 与 wiki/ 内容层，并输出写回清单。"
---

# Wiki知识图谱构建器

## 技能目的
- 把新增知识沉淀为可检索的 INDEX 关系与 wiki 页面
- 控制 INDEX 规模（只做路由），细节进入 wiki
- 为自我升级链路提供可审计写回点（DREAMS/POLICIES/CHANGE_LOG）

## 输入参数
- `source_material`：必填，文本。对话/交付物/变更说明/关键结论
- `target_scope`：选填，枚举。`agent` | `project`，默认 `agent`
- `existing_index`：选填，文本。当前 INDEX.md 内容（若未提供则先读取）
- `existing_wiki_pages`：选填，列表。已加载的 wiki 页面（最小加载原则）

## 输出
- `index_patch_plan`：建议更新 INDEX 的新增/删除/合并节点与边（含权重）
- `wiki_write_plan`：需要新增/更新的 wiki 页面列表与各页要点
- `memory_update_plan`：是否需要同步更新 MEMORY.md 的摘要条目
- `changelog_plan`：是否需要记录结构/规则变更

## 工作步骤（推荐）
1. 抽取实体：概念/文档/技能/规则/约束/交付物（按岗位语境扩展）
2. 抽取关系：depends_on / contains / implements / validates / conflicts_with / similar_to
3. 归并去重：合并同义节点，降低重复边权重
4. 写回 wiki：补齐定义、适用范围、示例、相关链接（指向 INDEX 实体）
5. 更新 INDEX：只保留路由所需最小边集，标注权重
6. 同步记忆：若属于高频稳定结论，写入 MEMORY.md 摘要
7. 留痕：若涉及规则/结构/安全边界，写入 CHANGE_LOG，并产出待确认项

## 写回目标
- `INDEX.md`：新增/更新节点与边
- `wiki/<topic>.md`：新增/更新内容页
- `MEMORY.md`：高价值摘要（可选）
- `DREAMS.md` / `POLICIES.md` / `CHANGE_LOG.md`：反思/策略/变更（按需）

## 边界
- 不臆造关键事实；不确定内容必须标注“待确认”
- 不把长篇细节塞进 INDEX
- 涉及安全边界/规则修改必须要求显式确认后再落盘
