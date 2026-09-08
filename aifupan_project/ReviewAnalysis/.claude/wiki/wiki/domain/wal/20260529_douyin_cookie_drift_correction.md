---
date: 2026-05-29
feature: douyin-juliangapi-correction
type: domain
run_id: distill-2026-05-29-1050-douyin-juliangapi
source: h-distill-from-code
scope:
  domains: [domain]
  files: [douyin/**/*.cs, juliangApi/**/*.cs]
  topics: ["无头采集", "Cookie", "授权", "a_bogus"]
related_specs: []
related_wal: []
---

# Domain WAL — douyin Cookie 判据修正 + juliangApi 持久化层补全 (2026-05-29)

> 关联权威文档: [platform_integration.md](../platform_integration.md)
> 来源: `/h-distill-from-code` 代码→wiki 对账（run distill-2026-05-29-1050-douyin-juliangapi）
> 性质: 初始为 form A 修正 WAL；随后经用户显式指示，已对 index.md:17 执行 form-B 直接订正（见下方「index.md:17 直接订正记录」）。

## index.md:17 直接订正记录（form B）

经用户显式指示，`platform_integration.md:17` 已直接订正为代码可验证的判据：
- **before**: `**核心 Cookie**: ` `passport_csrf_token`, `sessionid_ss`, `odin_tt`
- **after** : `**核心 Cookie**: 授权有效性判据为名称含 ` `sessionid` ` 的 cookie（如 ` `sessionid_ss`）
- **reason**: 原列举的 3 个 cookie 中 `passport_csrf_token`/`odin_tt` 在 `douyin/` 无引用，仅 sessionid 系作为有效性判据；改为代码现实表述。
[Confidence: HIGH]
[Evidence: douyin/DouyinAuthUtils.cs:138-139]

## 修正 — douyin「核心 Cookie」描述漂移

`platform_integration.md:17` 称 douyin 核心 Cookie 为 `passport_csrf_token` / `sessionid_ss` / `odin_tt`，与代码不符：

1. douyin 授权有效性判据**仅检查名称含 `sessionid` 的 cookie**（`Contains("sessionid")`），并校验其 Value 非空且未过期。
[Confidence: HIGH]
[Evidence: douyin/DouyinAuthUtils.cs:138-139,141,144]

2. `passport_csrf_token` 与 `odin_tt` 在整个 `douyin/` 目录源码中**无任何引用**，不构成代码层面的「核心 Cookie」（可能随完整 cookie jar 一并持久化，但非有效性判据）。
[Confidence: HIGH]
[Evidence: 全目录 grep passport_csrf_token / odin_tt 无输出]

3. douyin cookie 文件路径用 `MD5(租户ID-用户ID)` 标识，**不绑定 secUid**（与 anchorLive 的 `MD5(secUid)` 不同）。
[Confidence: HIGH]
[Evidence: douyin/DouyinAuthUtils.cs:98-101 `MD5Utils.create($"{ReplayHttpUtils.ActiveTenantId}-{ReplayHttpUtils.UserId}")`]

## 补全 — juliangApi 持久化/处理层

4. `platform_integration.md:53-55` 的 Juliang API 关键文件仅列 Manager/Poller/Api，漏了 `juliangApi/JuliangApiDataHandle.cs`（`static class`），它承担本地 Cookie 读取与数据拉取相关处理。
[Confidence: HIGH]
[Evidence: juliangApi/JuliangApiDataHandle.cs:28,38,83]
