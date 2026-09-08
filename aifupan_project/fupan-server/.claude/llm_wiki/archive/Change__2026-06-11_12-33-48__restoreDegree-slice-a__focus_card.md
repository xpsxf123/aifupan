# Focus Card — restoreDegree-slice-a

## Goal (one sentence)
实现话术还原度 Slice A：tb_standard_script 表用 secUid 替代 anchor_url_user_id 作主键定位（无回填、无幽灵行），解除 B3/B8 两处限闸，新增 T21/T23/T24 三个标准稿 API（B3/B8 自动按 (tenantId+userId+secUid) 查标准稿），完成 AiEnums 枚举重命名+扩充，INSERT 5 套提示词，StandardScriptService 抽 Service 层避免 Bll-to-Bll 互调。

## Non-Goals (out of scope)
- 还原度报告生成（T25/T26/T27）→ Slice B
- triggerReport monitorType=1 闸解除 → Slice C
- anchorBasicConfig (T07) → 独立 B4 批次
- fidelityReportDetail API → Slice C

## Allowed Scope (file whitelist)

### 修改类（已存在）
- replay-common/src/main/java/com/jiuyu/replay/common/constant/AiEnums.java
- replay-words/src/main/java/com/jiuyu/replay/words/bll/AnchorUrlBll.java

### 新建类（replay-words 业务层）
- replay-words/src/main/java/com/jiuyu/replay/words/bll/ScriptMonitorStandardScriptBll.java
- replay-words/src/main/java/com/jiuyu/replay/words/repository/service/StandardScriptService.java
- replay-words/src/main/java/com/jiuyu/replay/words/repository/service/impl/StandardScriptServiceImpl.java
- replay-words/src/main/java/com/jiuyu/replay/words/bo/script/GenerateStandardScriptBo.java
- replay-words/src/main/java/com/jiuyu/replay/words/bo/script/ConfirmStandardScriptBo.java
- replay-words/src/main/java/com/jiuyu/replay/words/bo/script/TimeAxisItemBo.java
- replay-words/src/main/java/com/jiuyu/replay/words/vo/script/StandardScriptVo.java
- replay-words/src/main/java/com/jiuyu/replay/words/vo/script/TimeAxisItemVo.java
- replay-words/src/main/java/com/jiuyu/replay/words/vo/script/StandardScriptDetailVo.java
- replay-words/src/main/java/com/jiuyu/replay/words/vo/script/StandardScriptConfirmVo.java

### 修改类（已存在 — DDL 变更同步改）
- replay-words/src/main/java/com/jiuyu/replay/words/entity/StandardScriptEntity.java

### 新建类（replay-api Controller 层）
- replay-api/src/main/java/com/jiuyu/replay/api/controller/words/StandardScriptController.java

### SQL 新文件
- sql/replay-34.sql
- sql/replay-35.sql

### TECH-DEBT 台账（追加 DEBT-021）
- docs/TECH-DEBT.md

### 测试文件
- replay-words/src/test/java/com/jiuyu/replay/words/bll/ScriptMonitorStandardScriptBllTest.java
- replay-words/src/test/java/com/jiuyu/replay/words/bll/AnchorUrlBllTest.java
- replay-words/src/test/java/com/jiuyu/replay/words/bll/AnchorUrlBllUpdateMonitorSwitchTest.java

## Stop Rules
- 编辑超出 Allowed Scope → 立即停止，发起 [Boundary Exception Request]
- 同一 phase 失败 ≥3 次 → 停止上报
- 触碰 triggerReport monitorType=1 闸解除 → 立即停止（Slice C 范围）
- 修改 replay-ai 业务代码 → 立即停止（Slice A 只调 Feign，不改 ai 模块）
