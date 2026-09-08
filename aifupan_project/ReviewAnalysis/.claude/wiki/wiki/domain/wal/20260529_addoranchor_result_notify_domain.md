---
date: 2026-05-29
feature: addoranchor-result-notify
type: domain
run_id: 20260529_155758_sendserver_throw_global
related_adr:
  - ../../architecture/adr/ADR-002-sendserver-global-throw.md
related_wal:
  - [[20260529_http_error_passthrough_rules]]
---

# Domain WAL — addOrUpdateAnchor 结果/错误回传机制 (2026-05-29)

> 关联：[ADR-002](../../architecture/adr/ADR-002-sendserver-global-throw.md)、[HTTP 错误透传规则](../../preferences/wal/20260529_http_error_passthrough_rules.md)

`addOrUpdateAnchor`（以及多数客户端代理接口）的结果**不是**走 HTTP 响应，而是**两段式**回传，理解这点是改动该链路的前提。

1. **HTTP 立即响应 = 受理 ack，≠ 最终结果**。Controller `AddOrUpdateAnchor` 是 `void`、BLL 是 `async void`（fire-and-forget），`MethodCache.InvokeMethod` 对 void 返回包成 `HttpReponse{code:0,"成功",null}` 立即返回。同步前置校验失败（如主播位不足）才会在 HTTP 响应里返回非 0。
[Confidence: HIGH]
[Evidence: Controller/AnchorInfoController.cs:212,226 / Bll/Anchor/AnchorBll.cs:1369 / BeanCache/MethodCache.cs:175-196]

2. **真正结果走异步推送**：`FrontNotice.NoticeJs(json)` → CEF `EvaluateScriptAsync` 调用前端**全局函数** `notifyFromCSharp(jsonStr)`（参数是 JSON 字符串，前端需 `JSON.parse`）。添加/修改结果统一用 `action:"addAnchorEnd"` 的 `{code,status,action,msg,data}`；已存在主播时 `data.Id == -999`。
[Confidence: HIGH]
[Evidence: Utils/FrontNotice.cs:118,128 / Bll/Anchor/AnchorBll.cs:1469-1474,1480-1489]

3. **请求路径的异常→错误码映射**：`MethodCache.InvokeMethod` 对返回 `Task` 的 Controller 方法**同步 await**（`Task.Run(()=>task).GetAwaiter().GetResult()`），并把 `CustomException` 捕获转成 `HttpReponse{code=ErrorCode, msg}`。即「抛 CustomException」是让业务错误码回到调用方/前端的现成通道。
[Confidence: HIGH]
[Evidence: BeanCache/MethodCache.cs:179-182,199-231]
