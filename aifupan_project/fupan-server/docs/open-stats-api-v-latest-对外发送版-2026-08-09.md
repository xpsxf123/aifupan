# AI 智能体用量统计开放接口 · 对接文档

| 项目 | 内容 |
| --- | --- |
| 提供方 | ifupan-anchor-agent（直播复盘 AI 智能体） |
| 接入方 | `fupan-server`、`salescoach-agent` |
| 协议 | HTTP / JSON |
| 鉴权 | API-Key（服务间，非用户登录态） |
| 文档版本 | v1.1 · 2026-08-08 |
| 状态 | 已发布 |

> 本版本为对外发送版（2026-08-09）：不含任何环境密钥，接入方 Business Data Hub 的 appId 与密钥另行通过配置渠道交付。

---

## 1. 环境

| 环境 | BASE_URL | 说明 |
| --- | --- | --- |
| dev / 测试 | `http://qa-ai.aifupan.com.cn/api` | local 联调复用 dev 的一套密钥 |
| prod / 生产 | `https://agent-api.aifupan.com.cn` | — |

**完整地址 = BASE_URL + 接口路径**。例如租户用量统计：

- dev：`http://qa-ai.aifupan.com.cn/api/open/stats/tenant/usage`
- prod：`https://agent-api.aifupan.com.cn/open/stats/tenant/usage`

> 注意 dev 的 BASE_URL 带 `/api` 段，prod 不带。请把 BASE_URL 做成配置项，不要在代码里拼死路径前缀。

---

## 2. 鉴权

每个请求必须带两个请求头：

| 请求头 | 必填 | 说明 |
| --- | --- | --- |
| `x-jiuyu-client-id` | 是 | 调用方 appId |
| `api-key` | 是 | 该 appId 对应环境的密钥 |

### 2.1 密钥分配

> 密钥不随文档分发。各调用方 appId 对应的密钥请向本项目负责人申领，通过配置中心/私信渠道交付；文档历史版本中出现过的密钥正在轮换作废，请勿继续使用，轮换完成后另行通知。

### 2.2 鉴权失败

appId 缺失 / 密钥错误 / appId 未登记时统一返回：

```json
{"code": 403, "msg": "警告：请提供有效的api-key"}
```

HTTP 状态码仍是 `200`（与爱复盘框架既有接口口径一致）——**请以响应体的 `code` 字段判定成败，不要只看 HTTP 状态码**。

---

## 3. 通用约定

### 3.1 请求

- 方法：一律 `POST`（批量 id 放 query 会超长），语义上是只读查询，**可安全重试**。
- 请求头：`Content-Type: application/json`
- 请求体：

```json
{
  "ids": ["4395679891245236224", "4395679891245236225"],
  "startTime": "2025-08-01 00:00:00",
  "endTime": "2026-08-01 00:00:00"
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `ids` | string[] | 是 | 租户 id 或用户 id 列表。**必须用字符串**——这是雪花大整型，JS/JSON number 会丢精度。去重后单次最多 **200** 个，超出报 400，请自行分批 |
| `startTime` | string | 否 | 统计窗口起点（**含**）。`yyyy-MM-dd HH:mm:ss` 或 `yyyy-MM-dd`（按当日 00:00:00） |
| `endTime` | string | 否 | 统计窗口终点（**不含**）。格式同上 |

> ⚠️ **接口 5（`/open/stats/user-tenant/usage`）的请求体不是这个形状**：它的批量单位是「用户 × 租户」对，字段名为 `pairs`，且 id 用 JSON number 直传。详见 [§4.5](#45-批量用户--租户用量统计)。其余四个接口沿用上表。

### 3.2 时间窗规则

窗口口径为左闭右开 `[startTime, endTime)`，**最长支持一年**，服务端自动处理边界，不会因为窗口问题报错：

| 传参情况 | 实际生效窗口 |
| --- | --- |
| 两端都不传 | 近一年：`[now − 366天, now)` |
| 只传 `startTime` | `[start, min(start + 366天, now))` |
| 只传 `endTime` | `[end − 366天, end)` |
| 跨度超过一年 | 起点被夹到 `end − 366天`（**不报错**） |
| `startTime >= endTime` | 退回近一年 |
| 时间串格式非法 | 该端按未传处理 |

响应会**回显实际生效的窗口**（`data.startTime` / `data.endTime`），请以回显为准做展示与核对。

### 3.3 响应

统一信封（爱复盘框架 `ApiResponse`）：

```json
{
  "code": 0,
  "msg": "请求成功",
  "data": { "startTime": "...", "endTime": "...", "items": [ ... ] },
  "timestamp": 1754470800000
}
```

| 字段 | 说明 |
| --- | --- |
| `code` | **`0` 表示成功**（注意不是 200）；非 0 即失败，`msg` 为原因 |
| `msg` | 提示信息；成功时为「请求成功」 |
| `timestamp` | 服务端毫秒时间戳 |
| `data.startTime` / `data.endTime` | 实际生效窗口（`yyyy-MM-dd HH:mm:ss`） |
| `data.items` | 逐 id 的结果。**请求了几个 id 就返回几条**，顺序与入参一致，无数据的补 0 / `false`，不会静默丢 id |

### 3.4 错误码

| code | 触发场景 | 处理建议 |
| --- | --- | --- |
| `0` | 成功 | — |
| `400` | `ids` 为空 / 含非数字 id / 超过 200 个 | 修正参数，属调用方 bug，不必重试 |
| `403` | API-Key 缺失或错误 | 检查两个请求头与环境密钥 |
| `500` | 服务端异常 | 可退避重试（本接口只读，重试安全） |

> 判定写法：`if (resp.getCode() == 0) { ... }`。HTTP 状态码在上述场景下**都是 200**，不能用它判成败。

### 3.5 统计范围与口径边界

本节声明适用于全部 5 个端点：

1. **合并口径**：统计数据为**直播智能体 + 短视频（达人）智能体的合并总量**。两类场景共用同一服务与同一套会话/计费数据，当前不提供按场景拆分的查询，存量数据亦无法回溯拆分。
2. **不含旧版 App AI 助手**：爱复盘 App 内旧版 AI 助手（运营助手、弹幕助手等）的用量属另一套数据体系，**不计入**本接口任何指标；两者不可相加、不可互替。
3. **早期无租户归属数据**：租户维度字段上线前的部分早期数据无法归属租户，已尽量回刷；仍无法归属的早期数据**不计入任何租户**的统计结果。
4. **计费 Token 口径**：`billedTokens` 为计费 token（已含模型倍率与缓存折扣，与爱复盘算力扣费同口径），非原始 token，单位是 Token 非金额。

---

## 4. 接口清单

| # | 接口 | 路径 | 用途 |
| --- | --- | --- | --- |
| 1 | 批量租户用量统计 | `POST /open/stats/tenant/usage` | 账单 token 合计 + 用户提问条数 |
| 2 | 批量用户用量统计 | `POST /open/stats/user/usage` | 同上 |
| 3 | 批量租户是否使用过 AI 智能体 | `POST /open/stats/tenant/ai-used` | 布尔标记 |
| 4 | 批量用户是否使用过 AI 智能体 | `POST /open/stats/user/ai-used` | 布尔标记 |
| 5 | 批量「用户 × 租户」用量统计 | `POST /open/stats/user-tenant/usage` | 同 1/2，但按 (用户, 租户) 对拆开 |

---

### 4.1 批量租户用量统计

`POST /open/stats/tenant/usage`

**请求**：`ids` 传租户 id 列表，其余同 §3.1。

**响应 `items[]`**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | string | 租户 id（原样回显） |
| `billedTokens` | number | 窗口内计费 token 合计 |
| `userMessageCount` | number | 窗口内用户提问条数 |

**示例**

```bash
curl -X POST 'http://qa-ai.aifupan.com.cn/api/open/stats/tenant/usage' \
  -H 'x-jiuyu-client-id: fupan-server' \
  -H 'api-key: <向负责人申领>' \
  -H 'Content-Type: application/json' \
  -d '{
        "ids": ["4395679891245236224", "4395679891245236225"],
        "startTime": "2025-08-01",
        "endTime": "2026-08-01"
      }'
```

```json
{
  "code": 0,
  "msg": "请求成功",
  "data": {
    "startTime": "2025-08-01 00:00:00",
    "endTime": "2026-08-01 00:00:00",
    "items": [
      {"id": "4395679891245236224", "billedTokens": 1283400, "userMessageCount": 4127},
      {"id": "4395679891245236225", "billedTokens": 0,       "userMessageCount": 0}
    ]
  }
}
```

---

### 4.2 批量用户用量统计

`POST /open/stats/user/usage`

请求与响应结构同 4.1，只是 `ids`、`items[].id` 是**用户 id**。

```bash
curl -X POST 'https://agent-api.aifupan.com.cn/open/stats/user/usage' \
  -H 'x-jiuyu-client-id: salescoach-agent' \
  -H 'api-key: <向负责人申领>' \
  -H 'Content-Type: application/json' \
  -d '{"ids":["4386578728986083328"],"startTime":"2026-01-01","endTime":"2026-08-01"}'
```

```json
{
  "code": 0,
  "msg": "请求成功",
  "data": {
    "startTime": "2026-01-01 00:00:00",
    "endTime": "2026-08-01 00:00:00",
    "items": [{"id": "4386578728986083328", "billedTokens": 96520, "userMessageCount": 318}]
  }
}
```

---

### 4.3 批量租户是否使用过 AI 智能体

`POST /open/stats/tenant/ai-used`

**响应 `items[]`**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | string | 租户 id |
| `used` | boolean | 窗口内是否使用过 AI 智能体 |

```bash
curl -X POST 'http://qa-ai.aifupan.com.cn/api/open/stats/tenant/ai-used' \
  -H 'x-jiuyu-client-id: fupan-server' \
  -H 'api-key: <向负责人申领>' \
  -H 'Content-Type: application/json' \
  -d '{"ids":["4395679891245236224","4395679891245236225"]}'
```

```json
{
  "code": 0,
  "msg": "请求成功",
  "data": {
    "startTime": "2025-08-05 14:20:00",
    "endTime": "2026-08-06 14:20:00",
    "items": [
      {"id": "4395679891245236224", "used": true},
      {"id": "4395679891245236225", "used": false}
    ]
  }
}
```

> 未传时间参数即默认近一年，注意回显窗口是相对**请求时刻**滚动的。

---

### 4.4 批量用户是否使用过 AI 智能体

`POST /open/stats/user/ai-used`

结构同 4.3，`ids` 与 `items[].id` 为**用户 id**。

---

### 4.5 批量「用户 × 租户」用量统计

`POST /open/stats/user-tenant/usage` · v1.1 新增

**解决什么问题**：同一个用户可能归属多个租户（用户 A 因业务需要也挂在租户 B 下）。接口 2 按用户 id 单键统计，会把这个用户在所有租户下的用量糊成一笔，租户侧对账对不上。本接口的批量单位是 **(用户, 租户) 对**，同一用户在不同租户下的用量各自成行。

**请求**（注意与 §3.1 的差异）

```json
{
  "pairs": [
    {"userId": 4386578728986083328, "tenantId": 4395679891245236224},
    {"userId": 4386578728986083328, "tenantId": 4395679891245236225}
  ],
  "startTime": "2026-01-01",
  "endTime": "2026-08-01"
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `pairs` | object[] | 是 | 用户 × 租户对列表。去重后单次最多 **200** 对，超出报 400 |
| `pairs[].userId` | number | 是 | 用户 id。**本接口用 JSON number，不是字符串**——它是服务端对服务端调用，不经浏览器 |
| `pairs[].tenantId` | number | 是 | 租户 id。与 `userId` 必须成对给全，缺任一端报 400（服务端不猜默认租户） |
| `startTime` / `endTime` | string | 否 | 同 §3.1、§3.2（时间窗规则完全一致） |

**响应 `items[]`**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `userId` | number | 用户 id（原样回显） |
| `tenantId` | number | 租户 id（原样回显） |
| `billedTokens` | number | 窗口内该用户**在该租户下**的计费 token 合计 |
| `userMessageCount` | number | 窗口内该用户**在该租户下**的提问条数 |

两个指标都严格按对聚合；`items` 与入参 `pairs` 一一对应、顺序一致，无数据补 0。

**示例**

```bash
curl -X POST 'http://qa-ai.aifupan.com.cn/api/open/stats/user-tenant/usage' \
  -H 'x-jiuyu-client-id: fupan-server' \
  -H 'api-key: <向负责人申领>' \
  -H 'Content-Type: application/json' \
  -d '{
        "pairs": [
          {"userId": 4386578728986083328, "tenantId": 4395679891245236224},
          {"userId": 4386578728986083328, "tenantId": 4395679891245236225}
        ],
        "startTime": "2026-01-01",
        "endTime": "2026-08-01"
      }'
```

```json
{
  "code": 0,
  "msg": "请求成功",
  "data": {
    "startTime": "2026-01-01 00:00:00",
    "endTime": "2026-08-01 00:00:00",
    "items": [
      {"userId": 4386578728986083328, "tenantId": 4395679891245236224, "billedTokens": 96520, "userMessageCount": 318},
      {"userId": 4386578728986083328, "tenantId": 4395679891245236225, "billedTokens": 12800, "userMessageCount": 41}
    ]
  }
}
```

**与接口 2 的关系**：接口 2 返回的是该用户的**全租户合计**。同一用户各租户对的 `billedTokens` 相加 = 接口 2 的值（历史未回刷到租户的部分除外，见 §5.4）。

**JSON 大整型提醒**：id 走 number 是因为双方都是 Java 服务（`Long` 精确到 64 位）。**若你方链路中间经过 JavaScript**（Node 中转、浏览器调试面板、把响应直接透给前端），number 会在 `2^53` 处丢精度——那种场景请改用接口 2 的字符串口径，或在 Node 侧用 `json-bigint` 解析。

---

## 5. 指标口径（务必阅读）

对接方最容易踩的不是格式，是口径。以下为准确定义：

### 5.1 `billedTokens`

窗口内该租户 / 用户所有对话轮次的**计费 token 合计**。

- 来源是每轮对话收口时落的结算记录，含输入、输出、缓存读写换算后的最终账单量。
- 计入窗口的依据是**结算记录的产生时间**（即对话发生时间）。
- 单位是 token，不是金额；折算成本请用你方自己的单价体系。

### 5.2 `userMessageCount`

窗口内**用户提问的条数**（真人发出的消息）。

- 只统计 `role = user` 的消息；智能体回复、工具调用、系统消息**都不计入**。
- 一次提问 = 1 条，与该轮触发了多少次模型调用无关。
- 因此 `userMessageCount` 可粗略理解为「用户主动使用了多少次」，`billedTokens` 才是消耗量。

### 5.3 `used`（是否使用过 AI 智能体）

窗口内**只要发起过对话就是 `true`**。

- 判定源是结算记录是否存在，**包含结算未完成 / 失败的记录**——用户确实用了，只是扣费链路异常，因此仍算「用过」。
- `used = true` 与 `billedTokens = 0` 可以并存（极端情况：发起后立刻中断，账单为 0）。判断「是否使用过」请用本接口，不要用 `billedTokens > 0` 自行推断。

### 5.4 租户维度的已知边界 ⚠️

**v1.1 起，租户归属是精确的。** 对话消息表已补上租户字段（消息落库时记下当时的所属租户），租户维度与「用户 × 租户」维度的 `userMessageCount` 都是直接按租户分组统计出来的。

> v1.0 曾用「从结算记录反推租户成员 → 汇总其消息数」的间接口径，副作用是跨租户用户的消息在每个租户下各计一次（各租户之和大于全局去重值）。**该偏差已消除**——如果你方基于 v1.0 的口径做过修正逻辑，v1.1 后请撤掉。

**仅剩一处边界**：租户字段是 v1.1 才有的，此前的历史消息由我方脚本按结算记录回刷。极少数历史行回刷不到（该会话从未产生过结算记录、且其用户历史上跨过多个租户，无从判定归属），这些行**不计入任何租户**，表现为：

- 租户维度（接口 1）与「用户 × 租户」维度（接口 5）的历史消息数可能**略低于**用户维度（接口 2）的合计；
- 差值只存在于 v1.1 上线前的数据，新产生的数据不再有此问题；
- `billedTokens` 与 `used` **完全不受影响**——结算记录一直自带租户字段，任何窗口都是精确的。

对账建议：以 `billedTokens` 为准做金额/消耗类核对，`userMessageCount` 用于活跃度趋势判断，跨表严格配平请只用新窗口数据。

---

## 6. 调用建议

| 事项 | 建议 |
| --- | --- |
| 分批 | 单次 ≤ 200 个 id；更大批量请分片串行或低并发（≤ 4）调用，别一次并发几十个请求 |
| 超时 | 连接 3s、读取 10s 起步。时间窗一年 + 200 个 id 属重查询，别把超时设到 1s |
| 重试 | 只读接口，`500` / 超时可退避重试（建议 2 次，间隔 1s / 3s）；`400` / `403` 不要重试 |
| 缓存 | 用量数据变化不快，报表类场景建议本地缓存 5–30 分钟，避免同一批 id 高频轮询 |
| 时区 | 时间参数与回显均为**北京时间**（GMT+8），不带时区后缀 |
| 幂等 | 同参数多次调用结果一致（滚动默认窗口除外），无副作用 |
| 大整型 | 接口 1–4 的 id 全程按字符串处理（Java 用 `String`，JS 侧**禁止** `Number(id)`，否则末位被抹掉）；接口 5 的 id 是 JSON number，Java 用 `Long` 接，链路经 JS 时见 §4.5 末尾提醒 |

---

## 7. 联调自检清单

- [ ] BASE_URL 按环境配置（dev 带 `/api`，prod 不带）
- [ ] 两个请求头都带上，密钥用对了环境
- [ ] `ids` 是字符串数组，不是数字数组（接口 5 例外：`pairs` 里的 id 是 number，且 `userId`/`tenantId` 必须成对给全）
- [ ] 按响应体 `code == 0` 判成败（不是 200，也不是只看 HTTP 状态码）
- [ ] 展示时用 `data.startTime` / `data.endTime` 回显的窗口，不是自己传的那个
- [ ] `items` 与入参 id 一一对应，无需自己补缺项

---

## 8. 变更记录

| 版本 | 日期 | 变更 |
| --- | --- | --- |
| v1.0 | 2026-08-06 | 首版：4 个接口 + API-Key 鉴权 |
| v1.1 | 2026-08-08 | 新增接口 5「批量『用户 × 租户』用量统计」（§4.5）；租户维度 `userMessageCount` 改为精确归属，跨租户用户重复计数的偏差消除（§5.4）。**接口 1–4 的请求/响应结构未变，接入方无需改造** |

对接问题请联系本项目负责人。接口若有不兼容变更，会提前通知并保留旧版本一个灰度周期。
