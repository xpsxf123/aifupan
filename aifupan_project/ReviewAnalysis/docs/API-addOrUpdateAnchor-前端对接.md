# API 对接文档 — 添加/修改直播间 addOrUpdateAnchor（前端 AI 用）

> 适用版本：话术智能监控 2.60.3（分支 `feat/sendserver-throw-on-error`）
> 关联：`docs/客户端-addOrUpdateAnchor-改造指引.md`
> 读者：调用客户端本地 HttpServer 的 Web 前端 / 前端 AI

---

## 1. 接口概览

Web 前端 **不直连远程服务端**，而是 POST 到**客户端本地 HttpServer**，由客户端代理转发到服务端。

- **方法 / 路径**：`POST /addOrUpdateAnchor`
- **请求体**：JSON（见 §2）
- **关键特性（务必理解）**：本接口是**异步两段式**返回 —
  1. **HTTP 立即响应**：只代表「请求已受理 / 同步前置校验结果」，**不代表添加/修改成功**。
  2. **异步结果通知**：真正的添加/修改结果（成功 or 服务端业务错误如 70007）通过客户端**回调前端 JS 函数 `notifyFromCSharp`** 推送（见 §4）。

> ⚠️ 前端**不能**用 HTTP 200 当作添加成功，必须等 `notifyFromCSharp` 的 `addAnchorEnd` 通知。

---

## 2. 请求体字段

`Content-Type: application/json`。常用字段：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `secUid` | string | 修改必填 | 主播唯一标识。**空=新增，非空=修改** |
| `liveUrl` | string | 新增必填 | 直播间 URL |
| `platform` | int | 新增建议 | 平台：0=抖音 1=... 2=微信视频号 |
| `accountType` | int? | 否 | 账号归属类型 0=自有 1=同行 |
| **`isScriptQualityInspection`** | **int?** | 否 | **话术质检开关** 0=关闭 1=开启 |
| **`isScriptFidelityMonitor`** | **int?** | 否 | **话术还原度开关** 0=关闭 1=开启 |
| **`isInteractionPatrol`** | **int?** | 否 | **互动巡检开关** 0=关闭 1=开启 |
| **`standardScriptId`** | **long?** | 条件必填 | **已确认标准直播稿 ID**；`isScriptFidelityMonitor=1` 时**必传** |
| （其余既有字段） | — | — | premiereDate / accountStage / livingMode / recordTime 等，按既有约定 |

### 2.1 三个 AI 监控开关的取值语义（极重要）

服务端规则：**字段不传 = 保持原值；要关闭某能力必须显式传 0**。前端务必遵守：

- **没动某开关** → 字段**不要传**（或传 `null`）→ 服务端保持原值。
- **要开启** → 传 `1`。
- **要关闭** → **必须显式传 `0`**（否则服务端不会关闭、不会释放授权）。

> 客户端转发时对 `null` 字段做 `NullValueHandling.Ignore`（不发给服务端），与上述规则契合。**切勿**为这三个开关填默认值 0，否则编辑直播间会被服务端当成「关闭」而**误释放授权**。

### 2.2 standardScriptId 流程

开启话术还原度（`isScriptFidelityMonitor=1`）时，前端应**先直连远程服务端**的标准稿接口生成/确认标准直播稿、拿到 `standardScriptId`，再带着它调本接口。客户端只透传、不校验、不生成。

### 2.3 请求示例
```json
{
  "secUid": "MS4wLjABAAAA_xxx",
  "liveUrl": "https://live.douyin.com/123456",
  "platform": 0,
  "isScriptQualityInspection": 1,
  "isScriptFidelityMonitor": 1,
  "isInteractionPatrol": 0,
  "standardScriptId": 80012345
}
```
> 上例：开质检、开还原度（带 standardScriptId）、显式关互动巡检。

---

## 3. HTTP 立即响应

响应体统一结构：`{ "code": <int>, "msg": <string>, "data": <any> }`

| code | 含义 | 前端处理 |
|---|---|---|
| `0` | 请求已受理（**非最终结果**） | 等待 §4 的 `addAnchorEnd` 通知 |
| `500` / 其它非 0 | **同步前置校验失败**（如「可添加的主播数量不足,请购买资源」） | 直接按 `msg` 提示，流程终止 |

> 同步前置校验（主播位/视频号数量不足等）在 HTTP 响应里返回；服务端业务错误（70007 等）在 §4 通知里返回。

---

## 4. 异步结果通知（核心）

客户端通过 CEF 调用前端**全局 JS 函数** `notifyFromCSharp(jsonStr)` 推送结果。

### 4.1 前端必须实现
```javascript
// 必须挂在全局，否则客户端检测不到、通知丢失
function notifyFromCSharp(jsonStr) {
  const payload = JSON.parse(jsonStr); // 参数是 JSON 字符串，需 parse
  if (payload.action === 'addAnchorEnd') {
    handleAddOrUpdateResult(payload);
  }
  // 其它 action 按需分发
}
```

### 4.2 通知 payload 结构
```json
{ "code": 0, "status": 200, "action": "addAnchorEnd", "msg": "", "data": { /* 主播对象，失败时为 null */ } }
```

| 字段 | 说明 |
|---|---|
| `action` | 固定 `"addAnchorEnd"`（添加/修改结束） |
| `code` | **业务结果码**（见 §5），前端据此差异化提示 |
| `status` | 200=成功，500=失败（粗粒度，细分看 `code`） |
| `msg` | 失败时的提示文案（服务端原始 msg 或本地异常信息） |
| `data` | 成功时为主播对象；失败时 `null`。**已存在主播**时 `data.Id == -999` |

### 4.3 前端处理建议
```javascript
function handleAddOrUpdateResult(p) {
  if (p.code === 0) {
    if (p.data && p.data.Id === -999) { toast('该主播已存在'); return; }
    toast('保存成功'); refreshList(p.data); return;
  }
  // 失败：按 code 差异化
  switch (p.code) {
    case 70007: toast('请先关闭AI话术监控功能后，再修改账号归属类型'); break;
    case 70002: toast('授权量不足，请联系产品顾问'); break;
    case 70005: toast('请先确认标准直播稿'); break;
    default:    toast(p.msg || '保存失败');
  }
}
```

---

## 5. 业务错误码表

服务端业务错误码经客户端**原样透传**到 §4 通知的 `code`：

| code | 含义 |
|---|---|
| `0` | 成功 |
| `70001` | Token 不足 |
| `70002` | 授权量不足 |
| `70003` | 套餐不支持 |
| `70004` | 竞品账号 |
| `70005` | 未确认标准稿 |
| `70007` | 账号切换保护：已开 AI 监控时禁止把 accountType 改为非自有 |
| `-1` | 客户端本地异常（网络/数据异常等），看 `msg` |

> 列表以服务端契约 `API-话术智能监控 v1.3` 为准，可能扩充；前端 `default` 分支用 `msg` 兜底。

---

## 6. 实现状态（本分支）

| 能力 | 状态 |
|---|---|
| 4 个 AI 监控字段透传（§2） | ✅ 已实现（待 Windows 编译验证） |
| 服务端业务错误码经 `notifyFromCSharp` 透传（§4/§5） | ✅ 已实现（待 Windows 编译验证） |
| 同步前置校验（主播位不足等，§3） | ✅ 原有 |

> 注：客户端不实现标准稿生成、不做授权量/Token 校验——均为服务端职责。
