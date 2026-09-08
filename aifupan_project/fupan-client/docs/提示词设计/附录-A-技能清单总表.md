# 附录 A：技能清单总表

## 1. 使用说明

本表按当前 `agent/skills/INDEX.md` 与 `agent/self_skills/INDEX.md` 的真实登记内容整理，目的是回答三个问题：

- 当前项目到底有哪些技能。
- 这些技能分别属于什么类别。
- 哪些适合直接复用，哪些应保留为项目专属。

表中“复用级别”含义如下：

- `A`：通用能力，可直接迁移。
- `B`：方法可迁移，但需替换项目上下文或路径。
- `C`：强项目绑定，仅建议保留在当前项目。

## 2. 通用技能总表 `skills/`

| 技能名 | 类别 | 主要作用 | 复用级别 | 备注 |
| --- | --- | --- | --- | --- |
| `frontend_requirement_doc_generator` | 交付物 | 生成前端需求说明文档 | A | 适合作为产品到研发桥梁 |
| `prototype_layout_data_generator` | 交付物 | 生成布局/原型结构化数据 | A | 适合原型驱动项目 |
| `prototype_page_preview` | 交付物 | 生成原型页面预览 | A | 适合做快速评审 |
| `interaction_flow_analyzer` | 分析 | 分析交互流程与状态转移 | A | 适合复杂页面 |
| `page_structure_analyzer` | 分析 | 分析页面结构与层级 | A | 可作为生成前置步骤 |
| `layout_structure_analyzer` | 分析 | 分析布局结构与分区 | A | 适合抽布局框架 |
| `api_dependency_analyzer` | 分析 | 分析 API 依赖关系 | A | 适合联调前对齐 |
| `data_model_analyzer` | 分析 | 分析数据模型与字段使用 | A | 适合做实体抽象 |
| `permission_rule_analyzer` | 分析 | 分析权限规则与边界 | A | 适合中后台项目 |
| `state_sharing_analyzer` | 分析 | 分析共享状态策略 | A | 适合跨页面联动 |
| `form_validation_analyzer` | 分析 | 分析表单校验规则 | A | 适合表单密集项目 |
| `mindmap_preview` | 交付物 | 生成思维导图预览 | A | 适合方案讨论与展示 |
| `wiki_graph_builder` | 治理 | 抽取实体关系并更新图谱/写回计划 | A | 强推荐迁移 |
| `memory_compressor` | 治理 | 将稳定结论压缩到长期记忆 | A | 强推荐迁移 |
| `legacy_wiki_bootstrapper` | 治理 | 从遗留项目补建 wiki/INDEX/MEMORY | A | 老项目治理利器 |
| `demand_version_manager` | 治理 | 维护版本化需求与稳定入口 | A | 有版本化需求时强推荐 |
| `task_decomposition_guide` | 治理 | 将复杂任务拆成可验收子任务 | A | 团队协作价值高 |
| `spec_quality_checklist` | 治理 | 对规格文档做交付前校验 | A | 适合评审和交付前检查 |
| `skill_graph_manager` | 治理 | 治理技能索引与技能可发现性 | A | 强推荐迁移 |
| `wal_documentation_rules` | 治理 | 规范 WAL 写回格式与防膨胀规则 | A | 强推荐迁移 |
| `project_scaffolder` | 生成 | 基于全局配置初始化前端项目 | A | 新项目启动常用 |
| `router_generator` | 生成 | 生成路由与导航结构 | A | 适合配置驱动项目 |
| `layout_framework_generator` | 生成 | 生成全局布局框架 | A | 适合统一布局项目 |
| `component_abstractor` | 生成 | 抽取可复用组件 | A | 适合重构或设计系统建设 |
| `page_code_generator` | 生成 | 从结构与绑定关系生成页面代码 | A | 适合半自动研发 |
| `form_validation_binder` | 生成 | 将 JSON 校验绑定到前端表单 | A | 表单系统常用 |
| `api_integration_mapper` | 生成 | 将页面/功能映射到 OpenAPI 接口 | A | 联调规划价值高 |
| `api_client_generator` | 生成 | 生成 API Client 与调用封装 | A | 适合规范接口层 |
| `action_binder` | 生成 | 将动作定义绑定到处理逻辑 | A | 适合配置驱动交互 |
| `state_management_generator` | 生成 | 生成共享状态管理骨架 | A | 适合 store 规范化 |
| `mock_api_generator` | 生成 | 根据 API 定义生成 mock 数据 | A | 联调前效率高 |
| `dynamic_mock_backend` | 生成 | 生成带 JSON 持久化的动态 mock 后端 | A | 适合快速本地联调 |
| `mock_tester_page` | 生成 | 生成 mock 调试与查看页面 | A | 适合接口调试 |
| `style_integrator` | 生成 | 集成设计令牌与样式变量 | A | 适合设计系统建设 |
| `build_config_generator` | 生成 | 生成构建与环境配置 | A | 适合标准化工程启动 |

## 3. 项目技能总表 `self_skills/`

| 技能名 | 类别 | 主要作用 | 复用级别 | 备注 |
| --- | --- | --- | --- | --- |
| `project_component_catalog` | 项目配方 | 按页面需求映射现有组件资源与路径 | B | 思路可迁移，组件索引需重建 |
| `core_table_page_builder` | 项目配方 | 基于当前 `CoreTable` 快速搭建列表页 | B | 需替换为目标项目表格组件 |
| `table_column_render_recipe` | 项目配方 | 统一列渲染方式与配置选择 | B | 三分法可迁移，API 需重写 |
| `dialog_drawer_recipe` | 项目配方 | 统一弹窗与抽屉使用规范 | B | 受具体组件实现影响较大 |
| `ai_content_rendering_pipeline` | 项目配方 | 统一 AI 内容渲染入口与兼容逻辑 | B/C | AI 类项目可迁移，否则偏项目绑定 |

## 4. 按迁移优先级排序的推荐保留集合

如果你要把当前技能体系迁到新项目，建议优先级如下：

### 第一优先级

- `wiki_graph_builder`
- `memory_compressor`
- `skill_graph_manager`
- `wal_documentation_rules`
- `task_decomposition_guide`
- `spec_quality_checklist`

原因：

- 它们决定治理体系是否成立。

### 第二优先级

- `page_structure_analyzer`
- `layout_structure_analyzer`
- `api_dependency_analyzer`
- `state_sharing_analyzer`
- `api_integration_mapper`
- `page_code_generator`
- `api_client_generator`
- `router_generator`

原因：

- 它们决定“分析到生成”的核心链路是否成立。

### 第三优先级

- `project_scaffolder`
- `dynamic_mock_backend`
- `mock_tester_page`
- `style_integrator`
- `build_config_generator`

原因：

- 它们适合增强工程化，但不是最小闭环必需。

### 第四优先级

- `project_component_catalog`
- `core_table_page_builder`
- `table_column_render_recipe`
- `dialog_drawer_recipe`
- `ai_content_rendering_pipeline`

原因：

- 它们更适合在目标项目稳定后再按真实工程模式重建。

## 5. 技能迁移建议

### 5.1 可直接复制的技能

适合直接复制目录结构和文档骨架：

- 治理类技能
- 分析类技能
- 生成类技能中不依赖现有仓库路径的部分

### 5.2 应只复制结构、重写内容的技能

适合复制 `SKILLS.md` 结构，但重写输入输出与路径：

- 项目技能
- 依赖某个现成 UI 库或组件体系的技能

### 5.3 不建议机械复制的内容

- 当前项目技能里的具体路径
- 当前项目组件名、页面名、模块名
- 当前业务语义与专题 wiki 入口

## 6. 总结

从技能清单看，`fupan-client` 的提示词工程已经具备：

- 一套完整的治理技能层
- 一套较完整的分析技能层
- 一套配置驱动生成链
- 一套以项目复用为核心的 `self_skills/`

真正最值得优先抽走的不是单个生成技能，而是“治理技能 + 分析技能 + 技能分层规则”这一整套组合。
