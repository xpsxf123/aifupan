---
intent: Change
profile: STANDARD
risk: MEDIUM
frontend-facing: true
module: replay-ai
date: 2026-06-02
---

# reportStatus / batchReportStatus 加 monitorEnabled 字段

## 1. Context

B5 / B8 已落 `tb_anchor_url_user` 三开关：`isScriptQualityInspection / isScriptFidelityMonitor / isInteractionPatrol`，前端在调用 `reportStatus` / `batchReportStatus` 后还需要单独再调 `anchorBasicConfig` 才能拿到开关状态，两次 IO 才能渲染列表。

本次把"该资源对应直播间是否开启此类监控"合并到状态接口返回。

## 5. Business Logic

**单个接口契约改造（breaking）**：
- `GET /replay/script-monitor/reportStatus?sourceType=&sceneType=&sourceId=` →
  `POST /replay/script-monitor/reportStatus` body=`ReportStatusBo {sourceType, sceneType, sourceId, secUid?}`
- 跟 `batchReportStatus` 风格一致

**批量接口**（非破坏，additive）：
- `BatchReportStatusBo.SourceItemBo` 加可选 `secUid`

**响应**：
- `MonitorTypeStatusVo` 加 `Integer monitorEnabled`
  - 0 = 该 monitorType 在该直播间已关闭
  - 1 = 已开启
  - null = 不适用（未传 secUid / 上传文件场景 / anchor_url_user 无记录）

**装配逻辑**：
- 单接口：传了 secUid → 按 (userId, tenantId, secUid) 单查 `tb_anchor_url_user` 三开关，按 monitorType 映射
- 批量接口：去重 secUids → 一次 IN 批量查 → in-memory map join
- 新增 SPI：`AnchorUrlUserFeign.listBySecUidsAndUser(List<String> secUids, Long userId, Long tenantId)` 返 `List<AnchorUrlUserVo>`（带三开关字段）
- 未传 secUid / 查无记录 → monitorEnabled = null（不抛错）

## 6. Non-Functional Constraints

- 租户隔离：anchor_url_user 查询必带 tenantId + isRemoveRecord=0 过滤
- 性能：批量 100 条最多多一次 IN 查询，无 N+1
- 安全：Controller 禁收外部 userId（从 GlobalObject 取）

## 7. Acceptance Criteria

- **AC-1**（POST 单查 + 开关命中）：Given 已登录用户，when POST `/reportStatus` body `{sourceType:0, sceneType:0, sourceId:"v1", secUid:"sec1"}` 且该用户在 sec1 上 `isScriptQualityInspection=1`，then 响应 `monitors[monitorType=0].monitorEnabled == 1`
- **AC-2**（POST 单查 + 未传 secUid）：Given 不传 secUid，when 调用，then 所有 `monitorEnabled` 字段返 null
- **AC-3**（批量 + 三开关混合）：Given 批量 3 条 sources 含 2 个不同 secUid，when POST `/batchReportStatus`，then 每条 source 的三个 monitor 各自按其 secUid 查到的开关返回 0/1
- **AC-4**（上传文件场景 N/A）：Given sourceType=1 上传文件，无 secUid 传入，when 调用，then monitorEnabled 全为 null，不抛错
- **AC-5**（跨租户 / isRemoveRecord=1 不污染）：Given 同 secUid 但跨租户的 anchor_url_user 记录存在，when 当前用户查 sec1，then 只返回 (当前 tenantId, isRemoveRecord=0) 命中的开关
