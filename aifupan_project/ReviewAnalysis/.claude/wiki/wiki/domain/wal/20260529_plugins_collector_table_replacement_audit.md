---
date: 2026-05-29
feature: plugins-collector-table-replacement
type: domain
run_id: distill-2026-05-29-1050-plugins-b
source: h-distill-from-code
scope:
  domains: [domain]
  files: [plugins/**/*.cs]
  topics: ["Collector"]
related_specs: []
related_wal: []
---

# Form-B 审计 — platform_integration.md Collector 表直接订正 (2026-05-29)

> 关联权威文档: [platform_integration.md](../platform_integration.md)
> 关联修正依据: [plugins 采集层修正 WAL](./20260529_plugins_collector_layer_correction.md)
> 操作: form B 直接替换 index.md 条目（用户传入 `--allow-direct-replace` 授权）

## reason

Collector 段含 1 处 ORPHANED（`JuliangApiCollector` 全仓不存在）与 4 处 DRIFT（无 Simple 后缀的「Full」类名均不存在）+ 1 处 STALE（无 Full 模式）。保留会误导读者按不存在的类名查找代码，故直接订正 index.md 表格。

## before（platform_integration.md:155-164 原文）

```
各平台采集器包装，提供 Full 和 Simple 两种模式：

| 采集器 | 平台 |
|---|---|
| JuliangApiCollector | 巨量引擎 (API) |
| JuliangedCollector | 巨量引擎 (浏览器) |
| QianchuanCollector / Simple | 千川 |
| LifeCollector / Simple | 来客 |
| EnterpriseCollector / Simple | 企业号 |
| AnchorLiveCollector / Simple | 主播后台 |
```

## after（订正后）

```
各平台采集器包装，均实现 `IPlatformCollector` 接口：

| 采集器 | 平台 |
|---|---|
| JuliangedCollector | 巨量引擎 (浏览器) |
| QianchuanCollectorSimple | 千川 |
| LifeCollectorSimple | 来客 |
| EnterpriseCollectorSimple | 企业号 |
| AnchorLiveCollectorSimple | 主播后台 |
```

## 变更明细

1. 删去 STALE 描述「提供 Full 和 Simple 两种模式」，改为代码验证过的「均实现 `IPlatformCollector` 接口」。
[Confidence: HIGH]
[Evidence: plugins/collectors/JuliangedCollector.cs:17 等 5 个文件均 `: IPlatformCollector`]

2. 删去 ORPHANED 行 `JuliangApiCollector`（全仓 `grep "class JuliangApiCollector"` 无命中）。
[Confidence: HIGH]
[Evidence: 全仓 grep class JuliangApiCollector 无输出]

3. 4 个 DRIFT 类名订正为实际类名：`QianchuanCollector`→`QianchuanCollectorSimple`、`LifeCollector`→`LifeCollectorSimple`、`EnterpriseCollector`→`EnterpriseCollectorSimple`、`AnchorLiveCollector`→`AnchorLiveCollectorSimple`。
[Confidence: HIGH]
[Evidence: plugins/collectors/QianchuanCollectorSimple.cs:15, LifeCollectorSimple.cs:16, EnterpriseCollectorSimple.cs:15, AnchorLiveCollectorSimple.cs:15]
