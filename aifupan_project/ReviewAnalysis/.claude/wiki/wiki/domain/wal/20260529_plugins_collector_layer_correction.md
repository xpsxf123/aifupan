---
date: 2026-05-29
feature: plugins-collector-layer
type: domain
run_id: distill-2026-05-29-1050-plugins
source: h-distill-from-code
scope:
  domains: [domain]
  files: [plugins/**/*.cs]
  topics: ["PlatformDataManager", "Adapter", "Collector"]
related_specs: []
related_wal: []
---

# Domain WAL — plugins/ 采集层修正 + 调度层补全 (2026-05-29)

> 关联权威文档: [platform_integration.md 统一数据采集架构段](../platform_integration.md)
> 来源: `/h-distill-from-code` 代码→wiki 对账（run distill-2026-05-29-1050-plugins）
> 性质: form A 修正 WAL —— 记录 Collector 段过时事实，index.md 暂未改动（未传 `--allow-direct-replace`）

## 修正 — Collector 段已过时

`platform_integration.md` 的 Collector 段（约 154-164 行）描述与代码不符，以下为代码现实：

1. **不存在「Full 模式」**。`platform_integration.md:155`「提供 Full 和 Simple 两种模式」过时 —— 当前 `plugins/collectors/` 只有 Simple 变体（外加无后缀的 `JuliangedCollector`），无任何 Full collector 类。
[Confidence: HIGH]
[Evidence: plugins/collectors/ 仅含 JuliangedCollector / QianchuanCollectorSimple / LifeCollectorSimple / EnterpriseCollectorSimple / AnchorLiveCollectorSimple]

2. **`JuliangApiCollector` 不存在（ORPHANED）**。`platform_integration.md:159` 列出的 `JuliangApiCollector` 在全仓 `grep "class JuliangApiCollector"` 无任何命中。
[Confidence: HIGH]
[Evidence: 全仓 grep class JuliangApiCollector 无输出]

3. **实际 5 个 collector，全部实现 `IPlatformCollector`**：`JuliangedCollector`（巨量浏览器）、`QianchuanCollectorSimple`（千川）、`LifeCollectorSimple`（来客）、`EnterpriseCollectorSimple`（企业号）、`AnchorLiveCollectorSimple`（主播后台）。wiki 中无 Simple 后缀的「Full」类名（`QianchuanCollector` / `LifeCollector` / `EnterpriseCollector` / `AnchorLiveCollector`）均不存在，为漂移写法。
[Confidence: HIGH]
[Evidence: plugins/collectors/JuliangedCollector.cs:17, QianchuanCollectorSimple.cs:15, LifeCollectorSimple.cs:16, EnterpriseCollectorSimple.cs:15, AnchorLiveCollectorSimple.cs:15]

## 补全 — 统一调度 / 拉取层

`platform_integration.md` 说 PlatformDataManager「不负责轮询调度」，但未记录由谁负责。代码中由以下两层承担：

4. `plugins/core/CollectionScheduler.cs` 是采集调度器（`class CollectionScheduler : IDisposable`），带 `SchedulerStatus` 状态机管理采集生命周期。
[Confidence: HIGH]
[Evidence: plugins/core/CollectionScheduler.cs:18,34,51]

5. `plugins/dataPullers/BaseDataPuller.cs` 是抽象拉取基类，定义 `abstract Task<string> PullDataAsync(...)` 与 `abstract GetCookies(...)`；各平台 `XxxDataPuller`（Julianged / Qianchuan / Life / Enterprise / AnchorLive）继承之。
[Confidence: HIGH]
[Evidence: plugins/dataPullers/BaseDataPuller.cs:14,28,33]
