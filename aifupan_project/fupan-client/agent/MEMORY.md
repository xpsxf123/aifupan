# MEMORY（长期记忆摘要）

本文件用于保存高价值摘要信息，避免把细节塞进 `INDEX.md`。

## 摘要结构

- 用户偏好：语言、输出格式、禁用表达
- 项目约束：技术栈、安全红线、交付物边界
- 高频结论：经常被复用的决策与模板入口

## 高频结论

- 列表页批量删除统一使用 `src/mixins/table.js`：`CoreTable @deletes="deletes"` + 页面实现 `deleteOpt(list)` 配置 `{title,http,idKey,callback}`；仅开发者模式显示时用 `:table-select="enableBatchDelete"` 控制。
- AI 输出渲染统一使用 `src/components/aiContentRenderer/`（renderMode=mdTag/pureMd/json；plainTextMode 强制 pureMd）；历史入口 `src/components/analysis/ai/common/*RenderParser.js` 仅做兼容转发。
- 会员套餐等级参数约定：`-1 激活版 / 0 免费版 / 10 个人 / 15 工作室 / 20 企业 / 30 旗舰`；涉及"话术质检/互动巡检/还原度"等自动监控与报告生成权限判断时，以"是否 >= 工作室（15）"为阈值。
- 直播间授权状态统一读取：统一通过 `src/utils/liveRoomAuthStatus.js` 的 `getLiveRoomAuthStatus(secUid)` 获取（巨量/千川/视频号/来客等后续授权平台都走此入口），禁止在业务组件内散落授权状态查询实现。
- 话术质检核心约束（已归档至 wiki）：
  - 状态机：未生成(0) / 生成中(1) / 已生成(2) / 生成失败(3) / 不可生成(4)
  - 四类计数：崩盘话术 / 摸鱼话术 / 有损品牌 / 增加售后
  - Token 门槛：>= 100,000；不足时按错误码引导购买
  - 生成策略：话术质检/还原度固定 3 份中间报告合并 1 份最终报告；互动巡检按弹幕 10 分钟单元切分后合并
  - 账号归属：仅 `accountType=0`（自有账号）可用；竞品账号入口隐藏或展示 `-`
  - 详情见：`agent/wiki/components/wal/2026-06-03-话术质检报告解析组件.md`

## 分支管理策略（2026-07-20 新增）

详见 `agent/wiki/项目-复盘客户端-分支管理策略.md` 和 `.claude/rules/40-branch-management.md`

核心要点：
- 分支优先级：`release > test > dev`
- 修改方向：从低版本 → 高版本合并，严禁从高版本切低功能分支
- 当前活跃版本（低→高）：`2.5.2.3 → 2.6.0.8 → 2.6.2.1 → 2.6.2.2`
- 环境映射：dev=本地开发, test=测试, release=预发布, prod=生产
- aidev 分支格式：`aidev-YYYYMMdd-NO`
