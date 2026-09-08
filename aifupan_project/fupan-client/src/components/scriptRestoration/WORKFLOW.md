<!--
/**
 * @description 话术还原度监控弹窗（ScriptRestoration*）工作流说明：沉淀不同页面/场景的交互与接口调用差异，便于后续维护与回归验证。
 */
-->

# 话术还原度监控弹窗工作流（ScriptRestoration）

## 组件范围

- `ScriptRestorationDrawer.vue`：抽屉容器，负责“配置录入 -> 标准稿预览/编辑 -> 确认/生成报告”的流程编排
- `ScriptRestorationForm.vue`：配置表单（话术模式/语速/参考脚本/循环时长）
- `ScriptRestorationDraft.vue`：标准稿预览与本地编辑（时间轴表格）

## 标准稿的核心原则（与接口文档一致）

- `generateStandardScript`：只生成标准稿内容（草稿），不落库、不返回 `standardScriptId`
- 草稿编辑/保存：纯前端行为（本地修改 `timeAxisScript`），不调用接口
- `confirmStandardScript`：提交完整标准稿内容并落库，返回 `standardScriptId`（后续用于 `addOrUpdateAnchor` 或生成报告）

## 场景划分（总计 5 种状态）

### A. 添加主播/主播列表（直播间配置类）

> 这类场景的目标是“完成直播间配置”，不一定立刻生成报告。

1. 未绑定直播间（无 `anchorUrlUserId`）
   - 配置 -> `generateStandardScript` 生成草稿 -> 前端编辑/保存草稿
   - 用户点击“确认标准直播稿”后：调用 `confirmStandardScript` 落库并拿到 `standardScriptId`（新增场景不传 `anchorUrlUserId`）
   - 提交新增直播间时：`addOrUpdateAnchor` 必须携带该 `standardScriptId`，后端保存成功后回填标准稿的 `anchorUrlUserId`

2. 已绑定直播间（有 `anchorUrlUserId`）但未生成/未确认标准稿
   - 打开弹窗：先 `getStandardScript(anchorUrlUserId)`，无稿则进入配置生成流程
   - 生成/修改草稿 -> “确认标准直播稿”
   - 提交保存直播间时：先 `confirmStandardScript(anchorUrlUserId)`，再 `addOrUpdateAnchor(standardScriptId)`

3. 已绑定直播间（有 `anchorUrlUserId`）且已存在标准稿
   - 打开弹窗：`getStandardScript(anchorUrlUserId)` 有稿则直接进入标准稿预览页
   - 用户本地修改/保存草稿
   - 最终提交保存直播间时：`confirmStandardScript(anchorUrlUserId)` 覆盖当前标准稿（`standardScriptId` 不变），再 `addOrUpdateAnchor`

### B. 录制列表（含云空间等）/详情页（分析类）

> 这类场景的目标是“生成还原度报告”，直播间与资源已明确。

4. 已绑定直播间但未生成/未确认标准稿
   - 触发生成报告（triggerReport）会返回 `70005 请先确认标准直播稿`
   - 前端打开弹窗：`generateStandardScript` 生成草稿 -> 本地编辑/保存 -> `confirmStandardScript(anchorUrlUserId)` -> 再次 `triggerReport`

5. 已绑定直播间且已确认标准稿
   - 可直接 `triggerReport(monitorType=1)` 生成报告
   - 若用户在弹窗内修改过标准稿（产生本地脏数据），则先 `confirmStandardScript(anchorUrlUserId)` 再 `triggerReport`

## 弹窗对外约定（关键 props / events）

- `scene`：
  - `anchorConfig`：直播间配置场景（A 类），确认按钮调用 `confirmStandardScript` 落库并回填 `standardScriptId`
  - `analysis`：分析场景（B 类），生成报告时按需先落库确认再触发生成
- events：
  - `confirmed`：`anchorConfig` 场景下，确认标准稿成功后触发（包含 `standardScriptId`）
  - `generate`：`analysis` 场景下，用户点击生成报告触发，调用方执行 `triggerReport`
