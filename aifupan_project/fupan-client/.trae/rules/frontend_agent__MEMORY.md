# MEMORY（长期记忆摘要）

本文件用于保存高价值摘要信息，避免把细节塞进 `INDEX.md`。

## 摘要结构

- 用户偏好：语言、输出格式、禁用表达
- 项目约束：技术栈、安全红线、交付物边界
- 高频结论：经常被复用的决策与模板入口

## 高频结论

- 需求版本入口：优先从 `docs/需求版本库/README.md` 进入对应版本的主PRD与整合版；实现与验收默认以主PRD为准，冲突点集中记录为待确认项
- 版本库结构：每个版本在 `docs/需求版本库/<版本>/` 下，通过 `meta.json.canonical` 统一登记 PRD/整合版/抽取版/原型入口；长期记忆只保留入口与规则，不固化版本细节
- 列表页批量删除统一使用 `src/mixins/table.js`：`CoreTable @deletes="deletes"` + 页面实现 `deleteOpt(list)` 配置 `{title,http,idKey,callback}`；仅开发者模式显示时用 `:table-select="enableBatchDelete"` 控制。
- AI 输出渲染统一使用 `src/components/aiContentRenderer/`（renderMode=mdTag/pureMd/json；plainTextMode 强制 pureMd）；历史入口 `src/components/analysis/ai/common/*RenderParser.js` 仅做兼容转发。
- 会员套餐等级参数约定：`-1 激活版 / 0 免费版 / 10 个人 / 15 工作室 / 20 企业 / 30 旗舰`；涉及“话术质检/互动巡检/还原度”等自动监控与报告生成权限判断时，以“是否 >= 工作室（15）”为阈值。
- 直播间授权状态统一读取：统一通过 `src/utils/liveRoomAuthStatus.js` 的 `getLiveRoomAuthStatus(secUid)` 获取（巨量/千川/视频号/来客等后续授权平台都走此入口），禁止在业务组件内散落授权状态查询实现。

