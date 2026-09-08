# Explorer Report — restoreDegree-slice-a

## Specification Inference

- **Current:** 话术还原度开关 `isScriptFidelityMonitor` 在 `AnchorUrlBll.java:1116` 与 `AnchorUrlBll.java:2317-2324` 均有暂禁 guard，任何 0→1 开启请求均抛 70014 `SCRIPT_MONITOR_FEATURE_NOT_AVAILABLE`；`tb_standard_script` 表已建（`sql/replay-23.sql`），`StandardScriptEntity` + `StandardScriptDao` 已建，`AnchorUrlUserEntity.standardScriptId` 字段已建，`AddOrUpdateAnchorBo.standardScriptId` 字段已建，`AiEnums.askType` 已有 18/19 但语义旧名（`FIDELITY_MONITOR_GENERATE_PROMPT` / `FIDELITY_MONITOR_MERGE_PROMPT`），25/26/27 枚举值尚未定义。三个新 API（T21 generateStandardScript / T23 confirmStandardScript / T24 standardScriptDetail）尚未实现。5 套提示词 INSERT 尚未执行。`StatusCode.java:82-95` 已全部预定义 70001-70014。
- **Required:** B3/B8 两处限闸解除（校验 standardScriptId + accountType 后放行）；新增 `ScriptMonitorStandardScriptBll`（T21 同步 AI + T23 落库 + T24 查询）；新增 `StandardScriptController`（三 API）；扩充 `AiEnums.askType` 枚举（25/26/27）并重命名 18/19；新建 6 个 BO/VO；5 套提示词 INSERT 到 `tb_cue_words`；新建 SQL 文件 `replay-XX.sql`（提示词 INSERT）。
- **Delta:** 纯新增代码 + 两处限闸注释删除 + 枚举重命名（已确认 18/19 零业务引用，无运行时风险）。无 DDL 变更（表已建）。影响面：`replay-common`（AiEnums）/ `replay-words`（Bll + BO + VO）/ `replay-api`（Controller）/ `sql`（INSERT）。

---

## Acceptance Criteria (Given / When / Then)

### 完工验收口径映射（来自 launch_spec 7 条 ✅，直接 mirror，不 re-derive）

- **AC-001（happy path — T21 生成标准稿）：**
  Given 已登录自有账号（accountType=0）用户，当前直播间任意 anchorUrlUserId（或不传），
  when POST `/replay/script-monitor/generateStandardScript`（speechMode/speechSpeed/referenceScript 合法），
  then 返回 `R<StandardScriptVo>` 含 `timeAxisScript[]` 非空，HTTP 200，无 standardScriptId，耗时 ≤60s。

- **AC-002（happy path — T23 确认标准稿，新增场景）：**
  Given 已登录自有账号用户，anchorUrlUserId 未传（新增直播间前置步骤），
  when POST `/replay/script-monitor/confirmStandardScript`（含合法 timeAxisScript[]），
  then 返回 `R<StandardScriptConfirmVo>` 含 19 位 Snowflake 字符串 `standardScriptId`，`tb_standard_script` 写入一行（anchorUrlUserId=0）。

- **AC-003（happy path — T24 查详情，有稿）：**
  Given 已登录用户，当前 anchorUrlUserId 对应 tb_standard_script 有 is_deleted=0 记录，
  when GET `/replay/script-monitor/standardScriptDetail?anchorUrlUserId=<id>`，
  then 返回 `hasScript=true` + 完整 standardScriptId/speechMode/speechSpeed/timeAxisScript[]。

- **AC-004（happy path — T24 查详情，无稿）：**
  Given 已登录用户，当前 anchorUrlUserId 在 tb_standard_script 无 is_deleted=0 记录，
  when GET `/replay/script-monitor/standardScriptDetail?anchorUrlUserId=<id>`，
  then 返回 `hasScript=false` + `speechSpeed=280`（默认值），其余字段 null。

- **AC-005（happy path — B3 解禁开启还原度）：**
  Given 已登录自有账号，已有 confirmStandardScript 返回的合法 standardScriptId（属于当前用户/租户），
  when POST `addOrUpdateAnchor`（isScriptFidelityMonitor=1 + standardScriptId），
  then 还原度开关写入 tb_anchor_url_user，AnchorUrlBll 不再抛 70014，返回成功，tb_anchor_url_user.is_script_fidelity_monitor=1。

- **AC-006（happy path — B8 解禁 setMonitorEnabled monitorType=1）：**
  Given 已登录自有账号，tb_anchor_url_user.standard_script_id 已指向有效已确认稿，
  when POST `setMonitorEnabled`（monitorType=1, enabled=1），
  then 成功切换开关，不再抛 70014。

- **AC-007（happy path — 竞品账号拦截）：**
  Given 已登录用户，对应 anchorUrlUserId 的 accountType=1（同行账号），
  when 调用 B3 / B8 / T21 / T23 任意一个接口，
  then 抛 `BusinessException(70004)`，`{"code":70004}`，不写库。

### Edge Cases

- **AC-008（edge — standardScriptId 无效，B3 校验）：**
  Given 用户传 isScriptFidelityMonitor=1 + standardScriptId 指向不存在 / 已删除 / 不属于当前租户的记录，
  when POST `addOrUpdateAnchor`，
  then 抛 `BusinessException(70006)`，不修改 tb_anchor_url_user。

- **AC-009（edge — standardScriptId 无效，B8 校验）：**
  Given tb_anchor_url_user.standard_script_id=0 或指向已删除稿，
  when POST `setMonitorEnabled`（monitorType=1, enabled=1），
  then 抛 `BusinessException(70006)`，不修改开关。

- **AC-010（edge — T21 AI 超时 60s）：**
  Given 已登录自有账号，AI 调用时间超过 60s 未返回，
  when POST `generateStandardScript`，
  then 归还预扣 Token（`returnAiToken` 回调）+ 抛 `BusinessException(70008)`，不写库，HTTP 200（业务错误码）。

- **AC-011（edge — T21 AI 输出格式非法，markdown 表格解析失败）：**
  Given AI 返回文本不包含 `|时间段|` 格式表格行，
  when `ScriptMonitorStandardScriptBll` 解析 AI 输出，
  then 归还预扣 Token + 抛 `BusinessException(70008)`，不写库。

- **AC-012（edge — T23 speechMode=1 但 cycleDurationMinutes 未传）：**
  Given 用户请求 speechMode=1，cycleDurationMinutes=null，
  when POST `confirmStandardScript`，
  then 抛 `BusinessException(70013)`，不写库。

- **AC-013（edge — T23 anchorUrlUserId 已有稿，更新替换）：**
  Given anchorUrlUserId 对应已有 is_deleted=0 的旧标准稿，
  when POST `confirmStandardScript`（同 anchorUrlUserId），
  then 旧稿 is_deleted=1，新稿 INSERT，返回新 standardScriptId。

- **AC-014（edge — B3 isScriptFidelityMonitor=1 但未传 standardScriptId）：**
  Given 用户传 isScriptFidelityMonitor=1，standardScriptId=null / 0，
  when POST `addOrUpdateAnchor`，
  then 抛 `BusinessException(70005)`（请先确认标准直播稿），不写库。

- **AC-015（edge — 回归：质检 monitorType=0 链路不受影响）：**
  Given cueType=18 枚举重命名为 `FIDELITY_NON_CYCLIC_GENERATE`，
  when 质检 triggerReport（monitorType=0）触发生成报告，
  then 报告正常生成，无 NPE，日志无 askType 找不到的 WARN。

- **AC-016（edge — 回归：巡检 monitorType=2 链路不受影响）：**
  Given cueType=19 重命名为 `FIDELITY_NON_CYCLIC_MERGE`，
  when 巡检 triggerReport（monitorType=2）触发合并，
  then 报告正常合并，无异常。

- **AC-017（edge — T23 新增直播间 anchorUrlUserId 回填）：**
  Given T23 时 anchorUrlUserId=null（新增直播间，INSERT 时 anchor_url_user_id=0），后续 addOrUpdateAnchor 成功保存新直播间，
  when addOrUpdateAnchorBll 保存成功返回 anchorUrlUserId，
  then 按 standardScriptId UPDATE `tb_standard_script.anchor_url_user_id` + `secUid`（对齐 Pre-decided Constraint 6）。

---

## Hidden Scope (callers / dependents discovered via grep)

- `replay-words/src/main/java/com/jiuyu/replay/words/bll/AnchorUrlBll.java:1116-1125` — B3 还原度暂禁 guard，Slice A 移除此段；同文件 `2317-2324` — B8 同类 guard，同批移除。两处是 **目标改动点**，非旁路影响。
- `replay-words/src/main/java/com/jiuyu/replay/words/bo/anchor/AddOrUpdateAnchorBo.java:283-285` — `standardScriptId` 字段已建，注释"B3 前透传写库，无实际校验"；解禁后需在 Bll 层新增校验逻辑（不改 BO 本身，改 AnchorUrlBll）。
- `replay-common/src/main/java/com/jiuyu/replay/common/constant/AiEnums.java:84-85` — `FIDELITY_MONITOR_GENERATE_PROMPT(18)` / `FIDELITY_MONITOR_MERGE_PROMPT(19)` 重命名；已 grep 确认 **所有业务代码均通过枚举名引用，SQL/业务代码零字面量 18/19 引用**，重命名需同步 grep 确认无遗漏引用点后修改。
- `replay-ai/src/main/java/com/jiuyu/replay/ai/bll/ScriptMonitorGenerateBll.java:492-503` — `resolveMergeKvKey` switch 含 case 1（monitorType=1 → `script_monitor_fidelity_merge_ai_model`），Slice A 不修改此处，留 Slice B 接手。**注意：cueType=18/19 重命名不影响此 kvKey 映射。**
- `replay-api/src/main/java/com/jiuyu/replay/api/controller/words/ScriptMonitorController.java` — 现有 Controller 类，T21/T23/T24 三 API 应新建独立 `StandardScriptController`（或追加到 `ScriptMonitorController`；由 /h-design 决策，但 pre-decided 倾向独立类，见下方 Open Questions）。
- `replay-words/src/test/java/com/jiuyu/replay/words/bll/AnchorUrlBllTest.java` + `AnchorUrlBllUpdateMonitorSwitchTest.java` — 现有测试文件；回归 AC-015/AC-016 需在此补充或新增 cueType 重命名相关 case。
- `replay-generic/src/main/java/com/jiuyu/replay/generic/vo/common/code/StatusCode.java:82-95` — 所有用到的错误码已预定义（70004/70005/70006/70007/70008/70013/70014），Slice A **无需新增错误码**，read-only。

---

## Recommended Allowed Scope (for Propose phase to refine)

### 修改类（已存在）

- `replay-common/src/main/java/com/jiuyu/replay/common/constant/AiEnums.java`
  （重命名 18/19 + 新增 25/26/27 枚举值）
- `replay-words/src/main/java/com/jiuyu/replay/words/bll/AnchorUrlBll.java`
  （移除 B3:1116-1125 暂禁 guard + 移除 B8:2317-2324 暂禁 guard + 新增 standardScriptId 校验逻辑 + accountType 校验）

### 新建类（replay-words 业务层）

- `replay-words/src/main/java/com/jiuyu/replay/words/bll/ScriptMonitorStandardScriptBll.java`
  （T21 generateStandardScript + T23 confirmStandardScript + T24 standardScriptDetail + markdown 解析私有方法）
- `replay-words/src/main/java/com/jiuyu/replay/words/bo/script/GenerateStandardScriptBo.java`
- `replay-words/src/main/java/com/jiuyu/replay/words/bo/script/ConfirmStandardScriptBo.java`
- `replay-words/src/main/java/com/jiuyu/replay/words/bo/script/TimeAxisItemBo.java`
- `replay-words/src/main/java/com/jiuyu/replay/words/vo/script/StandardScriptVo.java`
- `replay-words/src/main/java/com/jiuyu/replay/words/vo/script/TimeAxisItemVo.java`
- `replay-words/src/main/java/com/jiuyu/replay/words/vo/script/StandardScriptDetailVo.java`

### 新建类（replay-api Controller 层）

- `replay-api/src/main/java/com/jiuyu/replay/api/controller/words/StandardScriptController.java`
  （三 API 入口：T21/T23/T24；路径 `replay/script-monitor/generateStandardScript` 等，与现有 `ScriptMonitorController` 同 prefix）

### SQL 新文件（提示词 INSERT）

- `sql/replay-XX.sql`（文件编号由 /h-design 确定；含 5 条 INSERT INTO `tb_cue_words`，cueType 18/19/25/26/27，tradeId=1，tenantId=0）

### 测试（如存在测试基建则补充）

- `replay-words/src/test/java/com/jiuyu/replay/words/bll/ScriptMonitorStandardScriptBllTest.java`（新建）
- `replay-words/src/test/java/com/jiuyu/replay/words/bll/AnchorUrlBllTest.java`（追加 B3 解禁 + accountType 校验 case）
- `replay-words/src/test/java/com/jiuyu/replay/words/bll/AnchorUrlBllUpdateMonitorSwitchTest.java`（追加 B8 解禁 case）

---

## Open Questions

1. **StandardScriptController vs 追加到 ScriptMonitorController**：T21/T23/T24 是独立新建 `StandardScriptController`，还是追加方法到现有 `ScriptMonitorController`？（Pre-decided Constraints 未明确；前者职责分离更清晰，后者少一个类文件。建议 /h-design 拍板。）

2. **markdown 解析位置**：AI 输出解析逻辑放 `ScriptMonitorStandardScriptBll` 私有方法还是独立 `StandardScriptParser` 工具类？（Pre-decided Constraint 12 已标注"由 /h-design 决策"。）

3. **标准稿生成 systemKv key 最终名**：Pre-decided Constraint 9 建议 `script_monitor_standard_script_ai_model`，是否以此为准？（需运维侧确认实际配置的 key 名；如不同需在代码中 hard-code 正确名。）

4. **B3 accountType 校验数据来源**：accountType 字段在 `AnchorUrlUserEntity`，B3 入口是 `AddOrUpdateAnchorBo`（含 secUid）；新增直播间场景（anchorUrlUserId=null/0）时，需提前通过 secUid 查 tb_anchor_url 拿 accountType，还是通过 BO 中的 accountType 字段透传？（需 /h-design 明确校验时机和数据来源。）

5. **T23 anchorUrlUserId 回填时机**：Pre-decided Constraint 4 说"anchorUrlUserId==null → INSERT 占位 anchor_url_user_id=0，由 addOrUpdateAnchor 保存成功后回填"，但 addOrUpdateAnchor 处理成功后怎么拿到刚 INSERT 的 standardScriptId？需 addOrUpdateAnchorBll 返回 anchorUrlUserId 后，调用 `ScriptMonitorStandardScriptBll.backfillAnchorUrlUserId(standardScriptId, anchorUrlUserId, secUid)` 吗？接口契约是否要在 Propose 阶段明确。

---

## Wiki Sources Consulted

- `.claude/llm_wiki/wiki/frontend-api/script_monitor.md`（B3 三 API 字段表 + 错误码，行 616-731）
- `replay-generic/src/main/java/com/jiuyu/replay/generic/vo/common/code/StatusCode.java`（70001-70014 全部已定义，行 82-95）
