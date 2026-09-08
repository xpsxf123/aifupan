# 客户端 addOrUpdateAnchor 改造指引（话术智能监控 2.60.3）

> 目标读者：客户端项目 `ReviewAnalysis`（C# .NET WinForms）的开发 / AI agent
> 关联服务端契约：`docs/2.6.01/API-话术智能监控-接口文档.md` v1.3 §2.3
> 本指引范围：**仅「添加 / 修改直播间」(addOrUpdateAnchor) 接口的客户端改造**

---

## 1. 背景

本期「话术智能监控」给服务端 `addOrUpdateAnchor` 接口新增了 4 个字段（3 个 AI 监控开关 + 标准直播稿 ID），并新增「账号类型切换保护」校验。

客户端 `addOrUpdateAnchor` 是一个**代理转发**接口：Web 前端不直连服务端，而是先调客户端本地 HttpServer，客户端处理后再转发服务端。因此客户端需要**同步透传这 4 个新字段**，否则 Web 前端填的 AI 监控开关传不到服务端。

**范围边界**：
- 本次客户端**只**改 `addOrUpdateAnchor` 的字段透传。
- 话术监控的其它新接口（标准稿生成 / 确认、状态查询、报告详情、手动触发、确认已读等）**Web 前端直连远程服务端**，客户端 **不需要** 新增任何代理接口。
- 客户端 **不实现** 标准稿生成、不做 AI 监控位授权量校验、不做 Token 校验 —— 这些都是服务端职责。

---

## 2. 当前链路（保持不变）

```
Web 前端 POST /addOrUpdateAnchor
  → Controller/AnchorInfoController.cs  AddOrUpdateAnchor(AddOrUpdateAnchorBo)
  → Bll/Anchor/AnchorBll.cs            AddOrUpdateAnchor(anchorBo)
                                        └ BuildAnchorInfo(anchor, anchorBo)  ← 字段拷贝
  → api/AnchorApi.cs                   AddOrUpdateAnchor(AnchorInfo)
                                        └ AnchorInfo 序列化为 JSON → 反序列化为 AddOrUpdateAnchorBo
  → 远程服务端 POST /anchorurl/addOrUpdateAnchor
```

调用链结构不变，只是要让 4 个新字段能从 `AddOrUpdateAnchorBo` 一路透传到服务端。

---

## 3. 新增的 4 个字段

| 字段 | C# 类型 | 含义 |
|---|---|---|
| `isScriptQualityInspection` | `int?` | 话术质检开关，0=关闭，1=开启 |
| `isScriptFidelityMonitor` | `int?` | 话术还原度开关，0=关闭，1=开启 |
| `isInteractionPatrol` | `int?` | 互动巡检开关，0=关闭，1=开启 |
| `standardScriptId` | `long?` | 已确认标准直播稿 ID；`isScriptFidelityMonitor=1` 时由 Web 前端必传 |

> 三个开关必须用**可空类型** `int?`，原因见第 5 节。

---

## 4. 改动清单

### 4.1 `vo/common/BasicSettingsBaseDto.cs` —— 新增 4 个字段

`AddOrUpdateAnchorBo` 继承 `BasicSettingsBaseDto`，把字段加在基类即可被请求对象继承。在 `accountType` 附近追加：

```csharp
/// <summary>
/// 话术质检开关 0：关闭 1：开启
/// </summary>
public int? isScriptQualityInspection { get; set; }

/// <summary>
/// 话术还原度开关 0：关闭 1：开启
/// </summary>
public int? isScriptFidelityMonitor { get; set; }

/// <summary>
/// 互动巡检开关 0：关闭 1：开启
/// </summary>
public int? isInteractionPatrol { get; set; }

/// <summary>
/// 已确认标准直播稿 ID；isScriptFidelityMonitor=1 时必传
/// </summary>
public long? standardScriptId { get; set; }
```

### 4.2 `AnchorInfo` 模型 —— 新增同样 4 个字段

`api/AnchorApi.cs` 的 `AddOrUpdateAnchor` 通过 **`AnchorInfo` → JSON → `AddOrUpdateAnchorBo`** 的序列化方式转发：

```csharp
AddOrUpdateAnchorBo addOrUpdateAnchorBo = JsonConvert.DeserializeObject<AddOrUpdateAnchorBo>(
    JsonConvert.SerializeObject(anchorInfo), settings);
```

因此 `AnchorInfo` 模型若不含这 4 个字段，序列化时会丢失，服务端收不到。需在 `AnchorInfo`（`Model/` 下）补上**字段名完全一致**的 4 个属性（类型同 4.1）。

> 若 `AnchorInfo` 已继承 `BasicSettingsBaseDto`，则 4.1 改完即自动拥有，本步可跳过 —— 请先确认 `AnchorInfo` 的继承关系。

### 4.3 `Bll/Anchor/AnchorBll.cs` —— `BuildAnchorInfo()` 补字段赋值

`BuildAnchorInfo(AnchorInfo anchor, AddOrUpdateAnchorBo anchorBo)` 负责把请求 BO 的字段拷到 `AnchorInfo`。在其中追加 4 行：

```csharp
anchor.isScriptQualityInspection = anchorBo.isScriptQualityInspection;
anchor.isScriptFidelityMonitor   = anchorBo.isScriptFidelityMonitor;
anchor.isInteractionPatrol       = anchorBo.isInteractionPatrol;
anchor.standardScriptId          = anchorBo.standardScriptId;
```

⚠️ **直接赋值，不要加 `?? 0` 之类的默认值兜底**（原因见 5.1）。

### 4.4 `AnchorInfoController.cs` / `AnchorApi.cs` —— 无需改逻辑

- `AnchorInfoController.AddOrUpdateAnchor` 入参已是 `AddOrUpdateAnchorBo`，4.1 改完字段自动包含，**入口不用改**。
- `AnchorApi.AddOrUpdateAnchor` 走 JSON 序列化转发，4.2 改完字段自动随之传出，**转发逻辑不用改**。
- 仅需复核错误透传（见 5.3）。

---

## 5. 关键注意事项

### 5.1 三个开关必须可空，严禁填默认值

服务端规则：**开关字段不传 = 保持原值（编辑场景）；要关闭某能力必须显式传 0**。

`AnchorApi.AddOrUpdateAnchor` 转发时使用了 `NullValueHandling.Ignore` —— 值为 `null` 的字段不会发给服务端。这与服务端规则正好契合：

- Web 前端没动某开关 → 字段为 `null` → 转发时被忽略 → 服务端保持原值 ✓
- Web 前端要关闭某监控 → 前端显式传 `0` → 客户端透传 `0` → 服务端关闭并释放授权 ✓

**因此严禁**在客户端给这三个字段填默认值（如 `BuildAnchorInfo` 里写 `anchor.isXxx = anchorBo.isXxx ?? 0`，或 `AnchorInfo` 初始化时设 0）。一旦填了 0，编辑直播间时会被服务端当作「关闭」，**误触发授权释放**。

### 5.2 `standardScriptId` 只透传，不处理

开启话术还原度时，Web 前端会**先直连远程服务端**的标准稿接口（生成 / 确认标准直播稿）拿到 `standardScriptId`，再带着它调客户端 `addOrUpdateAnchor`。客户端只需**原样透传** `standardScriptId`，不校验、不生成标准稿。

### 5.3 账号切换保护错误必须原样透传

服务端新增校验：直播间已开启任一 AI 监控开关时，若把 `accountType` 从 0（自有）改为非 0，服务端会**阻断保存**并返回业务错误，例如：

```json
{ "code": 70007, "msg": "请先关闭AI话术监控功能后，再修改账号归属类型" }
```

类似还有 `70001`(Token不足) / `70002`(授权量不足) / `70003`(套餐不支持) / `70004`(竞品账号) / `70005`(未确认标准稿) 等。

客户端转发后**必须把服务端返回的 `code` / `msg` 原样透传**给 Web 前端，不可吞掉、不可改写成本地异常文案。请复核 `AnchorApi.AddOrUpdateAnchor` 及 `HttpUtils` 转发后对服务端非 0 响应的处理 —— 确保业务错误码能回到 Web 前端，前端要据此做差异化提示。

### 5.4 本地缓存同步

`AnchorBll.AddOrUpdateAnchor` 在转发成功后会更新本地 `AnchorInfo` 缓存。4.2 + 4.3 改好后，新字段会随 `anchor` 对象一起进缓存，无需额外处理；但请确认缓存读写、以及客户端其它读取 `AnchorInfo` 的地方不会因为新增字段报错。

---

## 6. 自测验证点

| 场景 | 预期 |
|---|---|
| 新增直播间，开启 3 个 AI 监控开关 | 客户端透传 `isXxx=1`，服务端收到并开启 |
| 编辑直播间，不改动 AI 监控开关 | 字段为 `null`，转发被忽略，服务端保持原值 |
| 编辑直播间，关闭某个监控 | Web 前端传 `0`，客户端透传 `0`，服务端关闭并释放授权 |
| 开启话术还原度并提交 | `standardScriptId` 被透传到服务端 |
| 已开启 AI 监控，把账号类型改为非自有 | 服务端返回 `70007`，客户端把该错误透传给 Web 前端 |

---

## 7. 不在本次范围

- ❌ 标准稿生成 / 确认接口的客户端代理（Web 前端直连服务端）
- ❌ 话术质检 / 还原度 / 互动巡检的状态查询、报告详情、手动触发、确认已读接口
- ❌ AI 监控位授权量、Token 的客户端校验

以上均由服务端或 Web 前端直连处理，客户端不涉及。

---

> 文档版本：v1.0 ｜ 最后更新：2026-05-22 ｜ 对应服务端契约：API 话术智能监控 v1.3
