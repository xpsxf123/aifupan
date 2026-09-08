---
date: 2026-05-29
feature: platform-auth-status-enums
type: domain
run_id: distill-2026-05-29-1050-platform
source: h-distill-from-code
scope:
  domains: [domain]
  files: [life/**/*.cs, enterprise/**/*.cs]
  topics: ["平台采集", "DataCollectionManager", "授权"]
related_specs: []
related_wal: []
---

# Domain WAL — 平台授权状态枚举补全 Life/Enterprise (2026-05-29)

> 关联权威文档: [platform_integration.md](../platform_integration.md)
> 来源: `/h-distill-from-code` 代码→wiki 对账（run distill-2026-05-29-1050-platform）

`platform_integration.md` 仅记录了 AnchorLive(4 态) 与 WeChatChannels(4 值) 的授权状态枚举，缺少 Life/Enterprise。两者实际使用 **7 态**枚举，比 AnchorLive 多出 `authFailed` / `authing` / `accountMismatched` / `subNoPermission`。

1. `LifeAuthStatusEnum` 含 7 个状态：`unAuth=0`, `auth=1`, `authExpires=2`, `authFailed=3`, `authing=4`, `accountMismatched=5`, `subNoPermission=6`。
[Confidence: HIGH]
[Evidence: life/LifeAuthStatusEnum.cs:3-12]

2. `EnterpriseAuthStatusEnum` 含相同的 7 个状态（`unAuth=0` … `subNoPermission=6`），各成员语义见枚举 XML 注释：未授权 / 已授权 / 授权已过期 / 授权失败 / 授权中 / 账号不匹配 / 子账号无权限。
[Confidence: HIGH]
[Evidence: enterprise/EnterpriseAuthStatusEnum.cs:6-42]
