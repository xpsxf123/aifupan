# Skills体系、分层策略与扩展规范

## 1. 为什么要做 Skills 体系

如果提示词工程只有规则，没有技能，实际执行会很快陷入两个问题：

- 相同任务反复从零开始，成本高且质量不稳定。
- 经验只能存在某个人或某次对话里，无法被团队复用。

当前项目通过 `skills/` 与 `self_skills/` 建立了一套“双层技能体系”，这是整个提示词工程里最有工程价值的设计之一。

## 2. Skills 的定义

从当前项目看，一个合格的 skill 不是一句提示，而是一个“可复用、可组合、可审计”的操作单元，至少要回答 5 个问题：

- 这个 skill 用来解决什么问题
- 输入是什么
- 输出是什么
- 约束和边界是什么
- 失败或缺失时如何处理

因此项目在 `skills/INDEX.md` 中明确要求：

- 每个 skill 必须定义 inputs / outputs / boundaries / failure handling
- 模板统一放在 `templates/`
- 新 skill 必须登记索引

这是一条非常强的通用设计原则，建议所有新项目复用。

## 3. 双层技能体系

## 3.1 通用技能 `skills/`

定位：

- 跨项目复用
- 与当前仓库业务路径弱耦合
- 更偏方法、生成、治理、分析

典型特点：

- 输出结构化文档或 JSON
- 提供 schema/template
- 支持与其他技能串联

## 3.2 项目技能 `self_skills/`

定位：

- 只服务当前项目
- 强依赖项目目录、组件资源、现有封装方式
- 更偏现成配方和落地路径

典型特点：

- 指向具体组件和路径
- 输出“如何在本项目里接”的建议
- 模板密度通常比通用技能低，但落地性更强

## 3.3 优先级规则

当前项目明确规定：

1. 先查 `self_skills/`
2. 再查 `skills/`
3. 最后才允许新建技能

这个优先级非常重要，它确保：

- 项目已有经验先被消费
- 真正通用的东西再沉淀到公共能力层
- 避免重复发明已有轮子

## 4. 当前项目的通用技能分组

根据 `skills/INDEX.md`，当前通用技能可分为以下几类。

## 4.1 交付物类

作用：直接生成研发过程中的可读产物。

- `frontend_requirement_doc_generator`
- `prototype_layout_data_generator`
- `prototype_page_preview`
- `mindmap_preview`

适合复用到：

- 产品研发协作
- 原型转前端说明
- 方案评审
- 可视化预览

## 4.2 分析类

作用：把页面、布局、接口、权限、状态等问题先结构化分析，再进入生成或实现。

- `interaction_flow_analyzer`
- `page_structure_analyzer`
- `layout_structure_analyzer`
- `api_dependency_analyzer`
- `data_model_analyzer`
- `permission_rule_analyzer`
- `state_sharing_analyzer`
- `form_validation_analyzer`

适合复用到：

- 复杂需求对齐
- 方案拆解
- 原型分析
- 现有页面反向建模

## 4.3 治理类

作用：控制知识库、索引、技能和文档体系的可发现性与可维护性。

- `wiki_graph_builder`
- `memory_compressor`
- `legacy_wiki_bootstrapper`
- `demand_version_manager`
- `task_decomposition_guide`
- `spec_quality_checklist`
- `skill_graph_manager`
- `wal_documentation_rules`

适合复用到：

- 老项目补建知识图谱
- 多人长期维护项目
- 需求版本治理
- 技能系统治理

## 4.4 JSON 驱动流水线类

作用：把前端研发过程做成由结构化配置驱动的生成流水线。

- `project_scaffolder`
- `router_generator`
- `layout_framework_generator`
- `component_abstractor`
- `page_code_generator`
- `form_validation_binder`
- `api_integration_mapper`
- `api_client_generator`
- `action_binder`
- `state_management_generator`
- `mock_api_generator`
- `dynamic_mock_backend`
- `mock_tester_page`
- `style_integrator`
- `build_config_generator`

适合复用到：

- 新项目初始化
- 原型到页面自动化
- 配置驱动生成
- mock 与联调加速

## 5. 当前项目的项目技能分组

根据 `self_skills/INDEX.md`，当前项目技能集中在“已有工程模式复用”。

## 5.1 组件选型与复用入口

- `project_component_catalog`

价值：

- 输入一个页面或功能需求，快速映射到现有组件清单、组件体系和路由定位。
- 非常适合作为“先找已有资源”的第一站。

## 5.2 列表页标准骨架

- `core_table_page_builder`

价值：

- 面向项目里最常见的后台列表页形态。
- 将 `CoreTable` 的搜索、分页、列定义、操作列组织成固定套路。

## 5.3 表格列渲染规范

- `table_column_render_recipe`

价值：

- 明确 `formatter`、`option.render`、`slot` 的使用分工。
- 解决团队中常见的“表格列到底怎么写才统一”的问题。

## 5.4 弹窗与抽屉规范

- `dialog_drawer_recipe`

价值：

- 将弹窗、抽屉的受控方式、调用骨架和常见坑位标准化。

## 5.5 AI 内容渲染管线

- `ai_content_rendering_pipeline`

价值：

- 统一 AI 渲染入口、兼容历史路径和渲染模式约定。
- 强项目绑定，但抽象方式值得其他 AI 项目参考。

## 6. 技能文档的推荐结构

根据当前项目现状，推荐每个技能都按以下结构维护：

1. 名称与一句话描述
2. 技能目的
3. 适用场景
4. 输入
5. 输出
6. 执行步骤
7. 关键约束
8. 失败处理
9. 相关技能
10. 模板或示例路径

当前项目已有很多技能基本遵守这一结构，这是可以直接复制的规范。

## 7. 模板机制的三种形态

当前项目的技能模板不是单一形态，而是三种类型混合存在。

## 7.1 Schema 模板

常见于分析类技能：

- `schema.json`
- `schema.md`

作用：

- 约束输出字段
- 支持后续自动消费
- 保证结果机器可读

## 7.2 文档模板

常见于需求文档或规范类技能：

- Markdown 模板
- 结构化文档模板

作用：

- 统一章节结构
- 提高跨人协作一致性

## 7.3 可执行模板

常见于 mock、预览、脚手架技能：

- `index.html`
- `server.mjs`
- `package.json`
- 示例数据文件

作用：

- 从“说明”升级为“可直接运行的骨架”

## 8. 技能组合方式

当前项目并不是把 skill 当成孤立单元，而是通过组合形成链路。

常见组合方式：

### 8.1 分析 -> 生成

例如：

- 页面结构分析
- 布局结构分析
- 再进入页面代码生成或布局框架生成

### 8.2 文档 -> 校验

例如：

- 先产出需求文档
- 再通过规格检查清单做质量校验

### 8.3 治理 -> 写回

例如：

- 图谱构建
- 技能索引治理
- 长期记忆压缩
- 变更日志留痕

### 8.4 项目技能 -> 通用技能

例如：

- 先通过 `project_component_catalog` 确定本项目组件资源
- 再调用通用生成技能产出页面骨架

## 9. 技能治理规则

从 `skill_graph_manager` 等治理技能里，可以抽取出以下强规则：

### 9.1 新技能必须注册

- 新增技能必须写入对应 `INDEX.md`
- 必要时还应从总 `INDEX.md` 或 wiki 建立关联入口

### 9.2 新技能必须说明边界

至少要写清：

- 做什么
- 不做什么
- 什么情况应该停止或升级处理

### 9.3 结构性变更必须留痕

以下变更应写入 `CHANGE_LOG.md`：

- 新增技能组
- 修改技能目录约定
- 新增写回流程
- 技能分层策略变化

### 9.4 技能之间应建立可发现关系

例如：

- 上游分析技能关联下游生成技能
- 生成技能关联校验技能
- 治理技能关联写回技能

这样才能让技能体系从“清单”进化成“图谱”。

## 10. 什么时候该新建 skill

建议满足以下任一条件再新建：

- 同类任务重复出现三次以上
- 该套路具备稳定输入输出
- 该套路可以由多人复用
- 该套路不只服务于单次交付

不建议新建的情况：

- 只为一次性任务服务
- 还没有形成稳定步骤
- 过度依赖个人上下文解释
- 本质上只是现有 skill 的一个参数分支

## 11. 什么时候放到 `self_skills/`

满足以下特征时，优先放到 `self_skills/`：

- 强依赖当前项目目录结构
- 强依赖当前项目组件或 UI 规范
- 脱离当前仓库就失去意义
- 更像“项目操作配方”而不是“通用方法”

## 12. 什么时候升级为 `skills/`

当某个项目技能逐渐具备以下特征时，可以升级为通用技能：

- 不再依赖具体仓库路径
- 输出结构稳定
- 可服务多个项目
- 具备清晰输入输出边界
- 有可迁移的模板或 schema

## 13. 技能扩展流程建议

推荐流程：

1. 先判断是否已有 `self_skills/` 可覆盖
2. 再判断是否已有 `skills/` 可组合覆盖
3. 若都没有，再创建新技能目录
4. 编写 `SKILLS.md`
5. 按需要补 `templates/`
6. 更新对应 `INDEX.md`
7. 补充相关技能双向链接
8. 如涉及结构变更，写入 `CHANGE_LOG.md`

## 14. 从本项目抽取出的技能体系最佳实践

- 技能必须分层，避免“通用与项目混杂”。
- 技能必须可审计，输入输出边界要明确。
- 技能必须有索引，不能只存在目录里。
- 技能尽量有模板，模板越标准越容易复用。
- 技能应形成链路，而不是孤立清单。
- 技能治理要有专门治理技能或规则，不要靠口头约束。

## 15. 新项目如何最小落地技能体系

建议第一阶段只建立这三类技能：

1. 分析类：页面结构、接口依赖、状态共享
2. 生成类：页面骨架、路由、接口 client
3. 治理类：图谱写回、记忆压缩、技能索引治理

等项目沉淀到一定程度后，再补项目专属 `self_skills/`，这样成本最低、见效最快。
