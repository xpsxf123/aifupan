---
date: 2026-05-28
feature: tech-debt-register
type: data
---

# 技术债务登记册

## 说明
本文档登记项目中已知的技术债务，按严重程度和影响范围排序。每次代码审查或 incident 后有新发现时更新。

## 标记规则
| 字段 | 说明 |
|---|---|
| DEBT-ID | `DEBT-YYYY-NNN`，年份+序号 |
| 严重程度 | P0=阻塞上线 / P1=月度修复 / P2=季度修复 / P3=择机处理 |
| 来源 | review=代码审查 / incident=线上故障 / manual=人工标记 |
| 状态 | OPEN / IN_PROGRESS / RESOLVED |

---

## 活跃债务

### God Class 拆分

| ID | 目标 | 行数 | 严重程度 | 来源 | 状态 |
|---|---|---|---|---|---|
| DEBT-2026-001 | Form1.cs 拆分 | 3,112 → <5×500 | P0 | review | OPEN |
| DEBT-2026-002 | AnchorBll.cs 拆分 | 2,719 → <4×500 | P0 | review | OPEN |
| DEBT-2026-003 | AnchorVideoBll.cs 拆分 | ~2,500 → <3×500 | P1 | review | OPEN |
| DEBT-2026-004 | WebsocketConnection.cs 拆分 | 784 → <2×400 | P2 | review | OPEN |

### 全局状态消除

| ID | 目标 | 严重程度 | 来源 | 状态 |
|---|---|---|---|---|
| DEBT-2026-010 | 消除 `public static` 可变字段 (31 个已知) | P0 | review | OPEN |
| DEBT-2026-011 | `AnchorVideoBll.isAnalysis` 静态标记 | P0 | review | OPEN |
| DEBT-2026-012 | `AnchorVideoBll.currentAnalysisId` 静态 ID | P0 | review | OPEN |

### 安全

| ID | 目标 | 严重程度 | 来源 | 状态 |
|---|---|---|---|---|
| DEBT-2026-020 | 硬编码 URL 消除 (331 处已知) | P1 | review | OPEN |
| DEBT-2026-021 | API Key 统一使用 ConfigManager.GetSecret() | P1 | review | OPEN |

### 性能

| ID | 目标 | 严重程度 | 来源 | 状态 |
|---|---|---|---|---|
| DEBT-2026-030 | N+1 查询消除 (循环内查询) | P1 | review | OPEN |
| DEBT-2026-031 | HttpClient 非单例使用修正 | P1 | review | OPEN |
| DEBT-2026-032 | Cache 无界增长风险 | P2 | review | OPEN |

### 测试

| ID | 目标 | 严重程度 | 来源 | 状态 |
|---|---|---|---|---|
| DEBT-2026-040 | BLL 层单元测试覆盖率 ~0% | P1 | manual | OPEN |
| DEBT-2026-041 | ASR 管线集成测试覆盖不足 | P1 | manual | OPEN |

### 代码质量

| ID | 目标 | 严重程度 | 来源 | 状态 |
|---|---|---|---|---|
| DEBT-2026-050 | 拼写错误修正 (FristPageIni, vedioSizie 等) | P2 | review | OPEN |
| DEBT-2026-051 | `async void` 非 UI 事件处理器消除 | P2 | review | OPEN |
| DEBT-2026-052 | 空 catch 块修复 | P2 | review | OPEN |

---

## 已解决

(暂无 — 后续修复后移至此区域)

---

## 更新协议

1. 新增债务：DA-YYYY-NNN 连续编号，填写完整行
2. 状态变更：`OPEN → IN_PROGRESS` 时关联 run_id；`→ RESOLVED` 时记录 commit hash
3. 来源 `incident` 时关联 incident slug
4. 禁止删除已解决条目 — 移至「已解决」区域
