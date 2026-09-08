---
name: "ai_ops_layout_reuse"
description: "复用AI运营助手/复盘详情页核心组件搭建“左原文右内容、右侧可收起”的详情页布局。用户要做类似两栏/三栏复用、或需要 wordDiscern/contrastVideo/aiFold 联动时调用。"
---

# AI 运营助手布局复用（左原文右内容）

## 适用场景

- 新增一个页面，希望采用“左侧分钟段落/原文 + 右侧报告/详情内容”的布局
- 右侧内容需要“可收起/可展开”
- 需要保留播放联动但不展示画面（仅声音播放 + 文本随进度高亮）

## 推荐复用组件（不要从零重写）

- 文本/分钟段落/报告容器：`wordDiscern`
  - 路径：`src/views/commonComponent/analysisLayout/component/wordDiscern.vue`
  - 内部已聚合 `LocatingBar + DiscernSearchContainer`，并具备 report 类型切换能力
- 播放器（支持仅声音播放）：`contrastVideo`
  - 路径：`src/views/commonComponent/analysisLayout/contrast-video.vue`
  - 建议做法：外层用 CSS 移出可视区（不改组件内部），用于承载 `ponlayerReadied + timeupdate`
- 收起把手：`aiFold`
  - 路径：`src/views/commonComponent/aiAnalysis/aiFold.vue`
  - 可在外层重写定位样式，复用同一套 icon 与交互

## 关键联动契约（必须沿用）

- 文本触发跳播：监听 `wordDiscern @playerReadied="onPlayerReadied"`
- 外层控制播放：调用 `contrastVideo.ponlayerReadied(second, type)`
- 播放进度回写文本：监听 `contrastVideo @playerTimeupdate="onPlayerTimeupdate"`，并调用 `wordDiscern.setVideoCurrentTime(time)`

## 参考落地（已实现页面）

- 新页面：`/replayMonitorDetail`
  - 页面入口：`src/views/modules/replay/monitorDetail/index.vue`
  - 布局实现：`src/views/modules/replay/monitorDetail/components/MonitorDetailLayout.vue`

