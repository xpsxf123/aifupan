---
spec_mode: STANDARD
risk: MEDIUM
frontend-facing: true
module: replay-ai
triggers: [api, business-arch]
---

# anchorBasicConfig 接口补全 tb_basic_settings 字段（含 livingMode）

## Human Section（做什么 / 为什么 / 怎么做 / 待确认项）

- **做什么**：`GET /replay/script-monitor/anchorBasicConfig` 接口当前响应 `AnchorUrlUserVo` 的继承字段（包括 `livingMode` 等 12 个 `BasicSettingsBaseDto` 字段）全部为 null。新增跨模块 `BasicSettingsFeign` SPI，在 `ScriptMonitorBll.anchorBasicConfig` 单点查 `tb_basic_settings` 并把 12 字段合并到响应 VO。
- **为什么**：测试反馈"添加或修改主播基础设置信息时，直播间模式 livingMode 没正确更新"。根因排查：写入路径（`/addOrUpdateAnchor`）落库正确，但 `anchorBasicConfig` 读路径只查 `tb_anchor_url_user`、漏读 `tb_basic_settings`，导致 livingMode 等字段始终是 null（VO 设计意图是合并视图，但实现没完成）。
- **怎么做**：(1) `replay-generic` 新建 `BasicSettingsFeign`（1 方法）；(2) `replay-words` 新建 `BasicSettingsApi` 实现，直接复用既有 `basicSettingsProducer.getBySourceUser`；(3) `replay-ai/ScriptMonitorBll` 构造器注入新 Feign，`anchorBasicConfig` 在原响应基础上 try-catch 合并 12 个 BaseDto 字段；basic 不存在 / 失败 → 字段保持 null（fail-safe）。
- **待确认项**：无（与用户已对齐方案 B、单点修、不动公共 SPI 实现）。

## 1. Context

- **Business goal (one sentence):** 让前端通过 anchorBasicConfig 能正确拿到主播的直播间模式（livingMode）等 12 个 BaseDto 字段。
- **Scope of change:**
  - 新建 `replay-generic/.../feign/words/BasicSettingsFeign.java`
  - 新建 `replay-words/.../api/BasicSettingsApi.java`
  - 修改 `replay-ai/.../bll/ScriptMonitorBll.java`（构造器 + anchorBasicConfig 方法）
  - 新建 `replay-ai/.../bll/ScriptMonitorBllAnchorBasicConfigTest.java`
- **Dependencies consulted:** 主上下文已读 `ScriptMonitorBll`、`AnchorUrlUserApi`、`AnchorUrlUserFeign`、`AnchorUrlUserVo`、`BasicSettingsBaseDto`、`BasicSettingsVo`、`BasicSettingsProducer(Impl)`。
- **Explorer hand-off:** `<run_dir>/explore_report.md`

## 2. Domain Model
Not applicable — 不引入新业务术语 / 状态机 / 枚举。

## 2.5 Business Architecture

### 2.5.1 跨模块调用拓扑（新增）

```
replay-ai (ScriptMonitorBll.anchorBasicConfig)
   ├─ anchorUrlUserFeign.getBySecUidAndUser(...)   ← 既有，不动
   │      → replay-words.AnchorUrlUserApi → tb_anchor_url_user
   └─ basicSettingsFeign.getByAnchor(...)          ← 新增
          → replay-words.BasicSettingsApi
                → basicSettingsProducer.getBySourceUser(secUid, ANCHOR.code, userId, tenantId)
                → tb_basic_settings
```

- 两次 Feign 调用串行（无并发收益，basic 查询规模 < 1ms，不值得 parallel）。
- basic 失败 fail-safe：try-catch + log.warn，主响应正常返回；故障等价于"修复前现状"。

## 3. API Contract

### 3.1 新增内部 SPI（generic）

```java
public interface BasicSettingsFeign {

    /**
     * 按主播三元组（secUid + userId + tenantId）查询 tb_basic_settings 主播来源记录。
     *
     * @param secUid   主播唯一标识（tb_basic_settings.source_id 对应 ANCHOR 类型）
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @return BasicSettingsVo（含 BasicSettingsBaseDto 12 字段）；不存在返 R.ok(null)；远端异常由调用方按需 try-catch
     */
    R<BasicSettingsVo> getByAnchor(String secUid, Long userId, Long tenantId);
}
```

### 3.2 HTTP 端点契约（不变）

- 路径：`GET /replay/script-monitor/anchorBasicConfig`
- 请求参数：`secUid`（不变）
- 响应：`R<AnchorUrlUserVo>`（**结构不变**；改动仅是原本始终 null 的 12 个 BaseDto 字段在 basic 有记录时被填实）
- 错误码：沿用既有 30000（anchor 不存在）；新增分支不引入新错误码

## 4. Data Model
Not applicable — 无 DDL；本次只是补读已存在的 `tb_basic_settings` 表。

## 5. Business Logic

### 5.1 ScriptMonitorBll.anchorBasicConfig 改造

**改造前**（[ScriptMonitorBll.java:1151-1160](../../../replay-ai/src/main/java/com/jiuyu/replay/ai/bll/ScriptMonitorBll.java:1151)）：
```
R<AnchorUrlUserVo> result = anchorUrlUserFeign.getBySecUidAndUser(...);
if 异常 → 返回错误
result.getData().setStandardScriptId(null);
return result;
```

**改造后**：
```
R<AnchorUrlUserVo> result = anchorUrlUserFeign.getBySecUidAndUser(...);
if 异常 → 返回错误  // 与原行为一致
AnchorUrlUserVo vo = result.getData();

// 新增：合并 tb_basic_settings 12 字段（fail-safe）
try {
    R<BasicSettingsVo> basicR = basicSettingsFeign.getByAnchor(secUid, user.getId(), user.getActiveTenantId());
    BasicSettingsVo basic = (basicR != null && basicR.getCode() == 0) ? basicR.getData() : null;
    if (basic != null) {
        // 显式排除 BasicSettingsVo 自有 7 字段，避免污染 anchor 维度字段
        BeanUtils.copyProperties(basic, vo,
            "id", "sourceId", "sourceType", "userId", "tenantId", "createDate", "updateDate");
    }
} catch (Exception e) {
    log.warn("anchorBasicConfig: 合并 tb_basic_settings 失败 secUid={}", secUid, e);
}

vo.setStandardScriptId(null);  // 既有 B5 行为，不动
return result;
```

### 5.2 BasicSettingsApi 实现

```java
@Override
public R<BasicSettingsVo> getByAnchor(String secUid, Long userId, Long tenantId) {
    BasicSettingsVo vo = basicSettingsProducer.getBySourceUser(
        secUid, WordsEnum.basicSettingsType.ANCHOR.getCode(), userId, tenantId);
    return R.ok(vo);  // 不存在 vo=null，调用方按 null 处理
}
```

### 5.3 不变项（明确锁定）

- `AnchorUrlUserApi.getBySecUidAndUser` **不动**（其他 4 个调用方 — 质检/巡检/还原度 generate + ScriptMonitorApi — 行为完全不变）。
- `AnchorUrlUserVo` 类定义不变（仍 extends BasicSettingsBaseDto）。
- `BasicSettingsProducer` 接口/实现不变。
- HTTP 端点路径 / 入参 / 错误码不变。
- `standardScriptId = null` 既有 B5 行为不动。

## 5.5 Technical Architecture
Not applicable — 无 MQ / 分布式锁 / 缓存 / 定时任务变动；新增的 Feign SPI 是同步调用，不引入异步 / 事务边界变化。

## 6. Non-Functional Constraints

- **性能**：anchorBasicConfig 单次调用从 1 次 DB 查询升到 2 次（追加 1 次 `tb_basic_settings` 按主键索引查询，QPS 增加 1×）。可接受 — 该接口本就是低 QPS 配置查询。
- **租户隔离**：`basicSettingsFeign.getByAnchor` 三参数全用 JWT 取的 `user.getId()` / `user.getActiveTenantId()`；不收外部 userId / tenantId。
- **可观测性**：basic 查询失败用 `log.warn` 记录 secUid + 异常栈，便于事后排查；不抛出。
- **回滚**：新建 1 个 SPI 接口 + 1 个实现类 + 1 个 Bll 方法改动；回滚 = 还原 ScriptMonitorBll.anchorBasicConfig 旧 10 行实现 + 删除新建 Feign/Api 文件。秒级回滚。
- **安全**：
  - 不引入新外部入参；不暴露其他租户数据（Feign 用 currentUser 三元组）。
  - SQL 占位 `#{}`（走 MP lambdaQuery）。
  - 响应 VO 未含 PII 新字段（livingMode 等是业务配置）。

## 6.5 Design Patterns
Not applicable — 不引入或改变命名模式。

## 7. Acceptance Criteria

- **AC-1（合并成功）**: Given 同租户用户调用 `GET /replay/script-monitor/anchorBasicConfig?secUid=X` 且 `tb_basic_settings` 存在 (X, userId, tenantId, sourceType=ANCHOR) 记录, when 调用接口, then HTTP 200 + `R<AnchorUrlUserVo>`，VO 的 12 个 BaseDto 字段（accountType / premiereDate / accountStage / accountWaterLevel / accountFlow / livingTarget / livingModality / marketing / optimizeDirection / learning / **livingMode** / anchorSituation）按 basic 记录填值；`tb_anchor_url_user` 来源的字段不变；`standardScriptId=null`。
- **AC-2（basic 无记录 — 容忍）**: Given `tb_basic_settings` 无对应记录（getByAnchor 返 `R.ok(null)`）, when 调用接口, then VO 正常返回，BaseDto 12 字段保持 null（与修复前现状一致），不抛错。
- **AC-3（basic Feign 异常 — fail-safe）**: Given `basicSettingsFeign.getByAnchor` 抛 RuntimeException, when 调用接口, then 主响应不受影响（HTTP 200 + R），异常被 catch + log.warn，BaseDto 12 字段保持 null。
- **AC-4（anchor 不存在 — 既有短路）**: Given `tb_anchor_url_user` 不存在该 (secUid, userId, tenantId) 记录, when 调用接口, then 沿用既有 `R.error(30000, "记录不存在")`；**不调** basicSettingsFeign（前置短路），不会无谓多查一次。
- **AC-5（其他调用方零影响）**: Given 质检/巡检/还原度 generate 等 4 处 `getBySecUidAndUser` 调用方, when 本次改动合并后回归, then 行为完全不变（`AnchorUrlUserApi.getBySecUidAndUser` 实现未动）。
- **AC-6（VO 自有字段不被污染）**: Given basic 记录存在且其上有 `id` / `userId` / `tenantId` 等自有字段, when 合并到 vo 时, then vo 的 `id` / `anchorUrlSecUid` / 三开关字段等 anchor 维度字段保持来自 `tb_anchor_url_user` 的值，不被 basic 覆盖（依赖 `BeanUtils.copyProperties` 的 `ignoreProperties` 参数显式排除 7 字段）。

## 8. Frontend Contract

- **路径不变**：`GET /replay/script-monitor/anchorBasicConfig?secUid=X`
- **响应结构不变**：仍是 `R<AnchorUrlUserVo>`
- **行为变化**：原本始终 null 的 `livingMode` / `accountType` / `accountStage` 等 12 个字段在 basic 有记录时变为实际值；前端无需改代码、可直接消费这些字段（之前的代码若有"livingMode 为 null 兜底"逻辑可保留，新逻辑会自动覆盖到正确值）。
- **向后兼容**：✅ 完全向后兼容（VO schema 不变，仅是字段值从 null → 实际值）。

## 9. ADRs
Not applicable — MEDIUM risk；设计无真正分歧（用户已定方案 B 单点修）。
