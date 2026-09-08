# project_component_catalog

## 本技能用途

- 把“我要做一个页面/功能块”的需求，快速映射到本项目已存在的组件资源（优先复用，避免重复造轮子）。
- 输出可直接用于页面搭建的组件选型清单，并给出跳转到具体组件/示例文件的路径。

## 适用场景

- 新建页面：需要先确定用 CoreTable 还是自绘布局、是否需要分析布局/AI 渲染等
- 重构页面：把页面中重复出现的弹窗/表单/表格抽出成模块内组件或通用组件
- 生成页面：用“组件树 + 绑定点”驱动代码生成时，先用本技能完成组件选型

## 输入

- 页面类型：列表页 / 详情页 / 配置页 / 分析页 / 弹窗页 / 分享页
- 模块归属：dataAnalysis / replay / shortVideo / setup / home / pages / commonComponent
- 交互能力：搜索 / 分页 / 批量操作 / 上传 / AI 渲染 / 图表 / C# 推送角标或弹窗

## 输出

- 推荐组件清单（按优先级）：组件名、路径、用途、关键 props/slots/events
- 复用策略建议：应放 `src/components` / `src/views/commonComponent` / `src/views/modules/**/component`
- 组装骨架建议：页面结构草图（布局/容器/数据流/接口调用点）

## 组件索引入口

- 全量组件清单：`agent/wiki/项目-复盘客户端-组件资源清单.md`
- 组件体系说明：`agent/wiki/项目-复盘客户端-组件体系.md`
- 路由与页面定位：`agent/wiki/项目-复盘客户端-路由与菜单体系.md`

## 选型规则（优先级）

1. 能用 CoreTable 覆盖的列表页，优先 CoreTable（内置搜索/分页/表格组合与生命周期事件）
2. 列渲染：
   - 纯文本/简单格式化：Column.formatter
   - 复杂结构/多行/带分隔符：Column.option.render（返回 HTML 字符串）
3. 弹窗/抽屉：
   - 标准弹窗统一用 `src/components/dialog/index.vue`
   - 抽屉统一用 `src/components/drawer/index.vue`
4. AI 内容渲染：统一走 `src/components/analysis/ai/common/aiContent.vue` 与渲染管线
5. 模块内复用优先：先在 `src/views/modules/<module>/component` 找，再考虑升级到全局组件

## 失败处理

- 若清单中找不到组件：
  - 优先在 `src/views/commonComponent` 与各模块 `component` 下再检索一次
  - 仍不存在时再新增组件，并在 `组件资源清单` 与 `self_skills/INDEX.md` 中登记

