---
date: 2026-05-29
feature: http-error-passthrough
type: rules
run_id: 20260529_155758_sendserver_throw_global
related_adr:
  - ../../architecture/adr/ADR-002-sendserver-global-throw.md
related_wal:
  - [[20260529_addoranchor_result_notify_domain]]
---

# Rules WAL — HTTP 错误透传与缓存的 3 个坑 (2026-05-29)

> 关联：[ADR-002](../../architecture/adr/ADR-002-sendserver-global-throw.md)、[addOrUpdateAnchor 结果回传机制](../../domain/wal/20260529_addoranchor_result_notify_domain.md)

1. **`HttpUtils.SendServer*` 与 `HttpAsyncUtils.SendServer*Async` 错误语义相反**。`HttpUtils` 的 `SendServerGet/GetAsync/Post/PostAsync` 在服务端 `code!=0` 时**只记日志、return null/""（吞码）**；`HttpAsyncUtils` 的同名 Async 方法则**抛 `CustomException`**。需要把服务端业务错误码透传给上层/前端时，**必须用 `HttpAsyncUtils`（或自行解析 code）**，用 `HttpUtils` 会丢码。
[Confidence: HIGH]
[Evidence: Utils/HttpUtils.cs:308-329 / Utils/HttpAsyncUtils.cs:99-110]

2. **`AnchorCacheManager.GetAnchorByIdFromCache` 返回的是 `ConcurrentDictionary` 活引用，不是副本**。直接在其上修改字段会**立即污染缓存**，即便后续服务端调用失败也无法回退。改主播前应**深拷贝**（`JsonConvert.DeserializeObject<AnchorInfo>(JsonConvert.SerializeObject(cached))`），在副本上构建，**服务端成功后才 `SetAnchorCache`**。
[Confidence: HIGH]
[Evidence: DataCache/AnchorCacheManager.cs:143-150 / Bll/Anchor/AnchorBll.cs:1511-1521]

3. **透传类开关字段禁止 `?? 0` 默认值**。AI 监控等开关（isScriptQualityInspection / isScriptFidelityMonitor / isInteractionPatrol 等）必须可空（`int?`），`BuildAnchorInfo` 中**直接赋值不加 `?? 0`**。语义：`null` = 不传（`NullValueHandling.Ignore`）= 服务端保持原值；显式 `0` = 关闭并释放授权。填默认 0 会在编辑时被服务端当成「关闭」而**误释放授权**。
[Confidence: HIGH]
[Evidence: docs/客户端-addOrUpdateAnchor-改造指引.md §5.1 / api/AnchorApi.cs:368-374(NullValueHandling.Ignore) / Bll/Anchor/AnchorBll.cs BuildAnchorInfo]

4. **dynamic JSON 的 `(int)` 强转有运行时风险**。`JsonConvert.DeserializeObject<dynamic>` 后 `(int)resultObj.code` 在 code 为字符串或超 int 范围时会抛 `RuntimeBinderException`/溢出。应 `int.TryParse(resultObj.code?.ToString(), out var c)` 并兜底（注意失败应兜 -1 而非 0，0=成功）。
[Confidence: HIGH]
[Evidence: Utils/HttpAsyncUtils.cs:109（本次改为 TryParse）]
