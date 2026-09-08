# 复盘数据 AI Agent 开放接口对接文档

> 版本：v2.0（对照 `AiAgentReplayController` 最新代码核对生成）
> 适用对象：AI Agent 项目侧研发 / 联调（接口以 tool 形式被 Agent 调用）
> 服务端：直播复盘后端服务 replay-api（分支 `feature/agent-data-query`）
> 接口性质：**只读查询**（无任何新增 / 修改 / 删除）
> 调用方式：HTTPS + JSON，全部 `POST`
> 设计原则：扁平自描述 DTO；字典值随响应解析为中文文案；列表走游标流式分页。

---

## 1. 速读（接入前必看）

1. **统一鉴权**：所有接口走 API-Key（请求头），无需签名（`@APIKey` 注解打在 Controller 类级）。
2. **显式身份**：无登录态，不依赖任何 JWT / 登录会话。每个请求体都必须带 `userId`、`tenantId`、`userType` 三件套（见 §4.2），服务端据此做租户隔离与数据范围裁剪。
3. **字段自描述**：响应中所有「字典编码」字段都**同时**给出原始 code 与已解析的中文文案（`xxxLabel`），Agent 直接读 `xxxLabel` 即可，无需再查字典。
4. **游标分页**：列表接口用 `cursor + pageSize`（见 §4.3），而不是 `page/limit` 偏移分页。把上一页返回的 `nextCursor` 原样回传即可继续翻页，直到 `hasMore=false`。
5. **永远 HTTP 200**：鉴权失败 / 业务异常都体现在响应体 `code` 字段，不要按 HTTP 状态码切分支。

---

## 2. 环境与协议

| 项 | 取值 |
|---|---|
| Base URL | **TBD**（联调群按环境下发；预计 yz：`https://api-yz.aifupan.com.cn`） |
| 通信协议 | HTTPS |
| 方法 | 全部 `POST` |
| Content-Type | `application/json;charset=UTF-8` |
| 时区 | GMT+8（Asia/Shanghai） |
| 时间格式 | `yyyy-MM-dd HH:mm:ss`（日期类为 `yyyy-MM-dd`；时间戳类为毫秒 `long`） |
| 服务端口 | 6606（网关转发，调用方无需关心） |

> 生产凭证不在本文档下发，正式上线前在专用对接群发 prod 凭证。

---

## 3. 鉴权

统一走 **API-Key 鉴权**（请求头），不需要签名。

### 3.1 请求头

| Header | 必填 | 取值（示例） | 说明 |
|---|---|---|---|
| `x-jiuyu-client-id` | 是 | `replay-ai-agent`（**TBD，待注册**） | AI Agent 应用 ID |
| `api-key` | 是 | （联调群下发，**TBD**） | 对应环境 API-Key |
| `Content-Type` | 是 | `application/json;charset=UTF-8` | 所有接口均为 POST |

鉴权失败示例（HTTP 200）：

```json
{ "code": 403, "msg": "警告：请提供有效的api-key", "data": null }
```

### 3.2 服务端注册（后端备注，不影响 Agent 调用）

需在目标环境 `application-<env>.yml > jiuyu.oauth.client.api-key-secret` 增加一项：

```yaml
jiuyu:
  oauth:
    client:
      api-key-secret:
        - app-id: replay-ai-agent
          secret: <生成的密钥>
```

---

## 4. 通用约定

### 4.1 统一返回结构 R\<T\>

```json
{ "code": 0, "msg": "success", "data": {} }
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | int | `0` 成功；非 `0` 失败（鉴权 / 业务 / 参数） |
| `msg` | string | 业务消息 / 错误描述 |
| `data` | object / array / null | 业务数据 |

### 4.2 身份三件套（每个请求体必带）

所有接口请求体均继承 `AiAgentBaseBo`，内嵌以下三个字段（下文各接口以 *(身份)* 标注，不再重复展开）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `userId` | long | 是 | 调用代表的用户 ID |
| `tenantId` | long | 是 | 用户所属租户（团队）ID，**租户隔离边界** |
| `userType` | int | 是 | 用户类型：`0`=爱复盘普通用户(主账号) / `1`=后台管理员 / `2`=爱复盘子账号 |

### 4.3 数据范围裁剪（userType + dataSourceType 双重控制）

**① `userType` 决定最大可见范围：**

| userType | 含义 | 主播 / 视频 的可见范围上限 |
|---|---|---|
| `0` | 普通主账号 | 整个租户（`tenantId` 下所有用户的数据） |
| `1` | 后台管理员 | 整个租户 |
| `2` | 子账号 | 仅本人（`tenantId` + `userId` 双重过滤） |

**② `dataSourceType` 决定取哪一部分数据**（仅列表接口 `anchor/list` / `video/list` 携带，默认 `1`）：

| dataSourceType | 含义 | 生效条件 |
|---|---|---|
| `1` | 自己录制（`userId` + `tenantId`） | 默认；所有 userType 均可 |
| `2` | 全租户（仅 `tenantId`） | **仅 `userType=0` 生效**，子账号自动回落为 `1` |
| `3` | 云空间 / 已分享（`tenantId` + `uploadStatus=1`，全租户可见） | **仅 `video/list`**；主播列表无此项 |

> 所有查询**强制叠加** `tenantId` 过滤 + 软删除过滤；子账号（`userType=2`）再叠加 `userId`。

### 4.4 游标流式分页

**列表请求**在身份三件套之外，再带：

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `cursor` | long | 否 | null | 上一页返回的 `nextCursor`；首页传 null 或不传 |
| `pageSize` | int | 否 | 50 | 每页条数，范围 `1~200`（弹幕接口默认 200、上限 500，见 §6.9） |

**列表响应 `data` 统一外壳（`CursorPageVo`）：**

| 字段 | 类型 | 说明 |
|---|---|---|
| `list` | array | 当前页数据 |
| `nextCursor` | long | 下一页游标；`null` 表示已到末页 |
| `hasMore` | boolean | 是否还有下一页 |

> 游标语义：服务端按主键 `id` 升序、`id > cursor` 取下一批，避免深翻页性能塌陷。Agent 只需把 `nextCursor` 原样回传，直到 `hasMore=false`。（弹幕接口底层为 TableStore，`cursor` 实为页码，由服务端封装，用法一致，对 Agent 透明。）

**本文档存在两种分页外壳，接口各属其一（用法一致，都是"原样回传游标直到 `hasMore=false`"）：**

| 外壳 | `nextCursor` 类型 | 用于 | 特点 |
|---|---|---|---|
| `CursorPageVo` | long | §6.1/§6.6/§6.9 及 §6.11 达人列表、§6.12 达人短视频列表 | 按主键 id 单调翻页，无排序选项 |
| `StreamPageVo` | string（不透明令牌） | §6.15 全库短视频搜索、§6.17 全库达人搜索、§6.18 爆款关键词、§6.19 爆款视频 | **keyset 流式**：支持按指标排序（点赞/评论/发布时间…）且无深翻页成本；令牌内部编码"排序值+id"，Agent 无需解析 |

> 两种外壳都只含 `list` / `nextCursor` / `hasMore`，**无 `total`、无页码、不支持跳页**。`StreamPageVo` 的 `nextCursor` 是 Base64 字符串，切勿当数字解析。

> **⚠️ 平台枚举差异**：§6.11 起的达人 / 短视频 / 爆款系列（视频域）`platformType` 取值为 **`1`=抖音 `2`=快手 `3`=视频号**，与 §6.1~§6.10 主播域的 `platform` `0/1/2` **不同**，勿混用。这批接口查询**只按 `tenantId` 隔离**（子账号 `userType=2` 再叠加 `userId`），无 `dataSourceType`。全库类接口（§6.15/§6.17/§6.18/§6.19）读公共爬取库，跨租户结果一致。

### 4.5 错误码

| code | 含义 | 触发条件 |
|---|---|---|
| `0` | 成功 | 正常 |
| `403` | 鉴权失败 | 请求头缺失或不匹配 |
| 非 `0` | 参数缺失 | `userId/tenantId/userType` 任一为空，或必填业务参数缺失 |
| 非 `0` | 数据不存在 | 指定 `secUid`/`videoId`/`tradeId` 在该租户下不存在或无权访问 |

---

## 5. 接口目录

> 所有接口均为 `POST`，Content-Type `application/json`。Path 以 Base URL 为前缀。

| # | 接口 | 路径 | 分页 |
|---|---|---|---|
| 1 | 主播列表 | `POST /internal/ai-agent/anchor/list` | 游标 |
| 2 | 主播行业聚合去重 | `POST /internal/ai-agent/anchor/trade-options` | 否 |
| 3 | 行业层级链（父行业） | `POST /internal/ai-agent/anchor/trade-parents` | 否 |
| 4 | 行业敏感词库（父链取词） | `POST /internal/ai-agent/anchor/sensitive-words` | 否 |
| 5 | 主播详情 | `POST /internal/ai-agent/anchor/detail` | 否 |
| 6 | 视频列表 | `POST /internal/ai-agent/video/list` | 游标 |
| 7 | 视频音频段落全文 | `POST /internal/ai-agent/video/audio-paragraphs` | 否 |
| 8 | 视频数据看板 | `POST /internal/ai-agent/video/dashboard` | 否 |
| 9 | 视频弹幕列表 | `POST /internal/ai-agent/video/barrages` | 游标 |
| 10 | 视频在线曲线 + 人群画像 | `POST /internal/ai-agent/video/online-curve` | 否 |
| 11 | 达人列表（租户达人订阅） | `POST /internal/ai-agent/influencer/list` | 游标 |
| 12 | 达人短视频列表（租户已采集） | `POST /internal/ai-agent/influencer/video/list` | 游标 |
| 13 | 批量短视频原文音频转文字 | `POST /internal/ai-agent/influencer/video/audio-text/batch` | 否 |
| 14 | 批量达人短视频统计 | `POST /internal/ai-agent/influencer/video/stats/batch` | 否 |
| 15 | 全库短视频搜索 | `POST /internal/ai-agent/influencer/video/global/search` | keyset |
| 16 | 批量短视频完整明细 | `POST /internal/ai-agent/influencer/video/detail/batch` | 否 |
| 17 | 全库达人搜索 | `POST /internal/ai-agent/influencer/search` | keyset |
| 18 | 爆款关键词列表 | `POST /internal/ai-agent/hotsearch/keyword/list` | keyset |
| 19 | 爆款视频列表 | `POST /internal/ai-agent/hotsearch/video/list` | keyset |
| 20 | 达人统计概览 | `POST /internal/ai-agent/influencer/analytics/overview` | 单次聚合（无分页） |
| 21 | 爆款选题拆解聚合 | `POST /internal/ai-agent/hotsearch/analytics/overview` | 单次聚合（无分页） |
| 22 | 批量达人档案 | `POST /internal/ai-agent/influencer/detail/batch` | 否 |

> §6.11~§6.22 为达人短视频 / 全库检索 / 爆款 / 统计系列，平台枚举与隔离规则见 §4.4 末两条 ⚠️。

---

## 6. 接口详情

### 6.1 主播列表

`POST /internal/ai-agent/anchor/list`

按条件筛选当前范围（§4.3）内的主播账号，供 Agent 选取分析对象。

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| *(身份)* | — | 是 | `userId` / `tenantId` / `userType`，见 §4.2 |
| `cursor` | long | 否 | 游标；首页传 null |
| `pageSize` | int | 否 | 每页条数，默认 50，范围 1~200 |
| `dataSourceType` | int | 否 | 数据源，默认 1；见 §4.3（主播无云空间选项 3） |
| `anchorName` | string | 否 | 主播名称，模糊匹配 |
| `platforms` | int[] | 否 | 平台类型（**多选**）：`0`=抖音 `1`=快手 `2`=视频号 |
| `addStartDate` | string | 否 | 添加时间范围-起（`yyyy-MM-dd HH:mm:ss`） |
| `addEndDate` | string | 否 | 添加时间范围-止 |
| `lastRecordStartDate` | string | 否 | 最后开始录制时间范围-起 |
| `lastRecordEndDate` | string | 否 | 最后开始录制时间范围-止 |
| `tradeId` | long | 否 | 行业 ID；**支持父行业**：传父行业 id 时服务端自动展开为「该行业 + 所有子孙行业」做 `IN` 过滤 |
| `anchorNumber` | string | 否 | 主播账号（抖音号等），精确匹配 |
| `accountType` | int | 否 | 归属类型：`0`=自有账号 `1`=同行账号；不传=全部 |

**响应 `data`**：游标外壳（§4.4），`list` 元素为 **AnchorItem**：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | long | 主播记录 ID |
| `secUid` | string | 主播唯一标识（后续详情/视频接口用它定位） |
| `anchorName` | string | 主播名称 |
| `anchorAvatar` | string | 主播头像 URL |
| `platform` | int | 平台类型 code |
| `platformLabel` | string | 平台类型文案（抖音/快手/视频号） |
| `anchorNumber` | string | 主播账号 |
| `tradeId` | long | 行业 ID |
| `tradeName` | string | 行业名称 |
| `accountType` | int | 归属类型 code（0自有/1同行） |
| `accountTypeLabel` | string | 归属类型文案 |
| `addDate` | string | 添加时间 |
| `lastRecordTime` | string | 最后开始录制时间 |

**示例**

请求：
```json
{
  "userId": 1001,
  "tenantId": 2001,
  "userType": 0,
  "pageSize": 50,
  "dataSourceType": 1,
  "platforms": [0, 2],
  "tradeId": 88,
  "accountType": 0
}
```
响应：
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "list": [
      {
        "id": 10231,
        "secUid": "MS4wLjABAAAA...",
        "anchorName": "示例主播",
        "anchorAvatar": "https://.../avatar.jpg",
        "platform": 0,
        "platformLabel": "抖音",
        "anchorNumber": "douyin123",
        "tradeId": 8801,
        "tradeName": "女装",
        "accountType": 0,
        "accountTypeLabel": "自有账号",
        "addDate": "2026-05-01 10:20:30",
        "lastRecordTime": "2026-06-10 19:00:00"
      }
    ],
    "nextCursor": 10231,
    "hasMore": true
  }
}
```

---

### 6.2 主播行业聚合

`POST /internal/ai-agent/anchor/trade-options`

返回当前范围（§4.3）内**所有主播所属行业**的「ID + 名称」**去重**列表，供 Agent 做行业维度筛选枚举。

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| *(身份)* | — | 是 | 见 §4.2；`userType ∈ {0,1}` 聚合整租户，`==2` 仅本人 |

**响应 `data`**：`List<TradeOption>`

| 字段 | 类型 | 说明 |
|---|---|---|
| `tradeId` | long | 行业 ID |
| `tradeName` | string | 行业名称 |

> 去重口径：对范围内主播的 `tradeId` 聚合去重，再批量取名（无 N+1）。空行业（tradeId 为空）不返回。

**示例响应**
```json
{
  "code": 0,
  "msg": "success",
  "data": [
    { "tradeId": 8801, "tradeName": "女装" },
    { "tradeId": 8802, "tradeName": "美妆" }
  ]
}
```

---

### 6.3 行业层级链（自身 + 各级父行业）

`POST /internal/ai-agent/anchor/trade-parents`

根据 `tradeId`（或 `tradeName`）返回其行业层级链：自身 + 各级父行业，一直回溯到顶级。**按层级排序，离起点越近越靠前**（数组第 1 个是起点行业自身，往后依次是父、祖父……顶级行业在最后）。供 Agent 反查某行业的所属层级路径。

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| *(身份)* | — | 是 | 见 §4.2（仅校验身份合法性；行业为全局基础数据，不做租户隔离） |
| `tradeId` | long | 否 | 起点行业 ID |
| `tradeName` | string | 否 | 起点行业名称（`tradeId` 未知时按名称定位；两者至少给其一） |

**响应 `data`**：`List<TradeParent>`（按层级由近到远排序）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | long | 行业 ID |
| `name` | string | 行业名称 |
| `parentId` | long | 父行业 ID（`0` 表示顶级行业） |

**示例响应**
```json
{
  "code": 0,
  "msg": "success",
  "data": [
    { "id": 8810, "name": "连衣裙", "parentId": 8801 },
    { "id": 8801, "name": "女装",   "parentId": 8800 },
    { "id": 8800, "name": "服饰",   "parentId": 0 }
  ]
}
```

**说明 / 边界**

- 行业为**全局基础数据**（非租户隔离），结果不随租户变化；接口仅校验身份三件套合法性。
- 起点行业不存在时返回 `data: []`（空数组）。
- 数组首元素即起点行业自身；如只需祖先链，去掉第 1 个元素即可。
- 返回链**不含**通用行业（id=1）的兜底拼接。

---

### 6.4 行业敏感词库（父链取词）

`POST /internal/ai-agent/anchor/sensitive-words`

按行业层级取「4 级（自身）+ 3 级 + 2 级 + 1 级父行业 + 全行业」的系统敏感词，供 AI Agent **在输出话术前做合规自检**。取词口径为**父链向上回溯**：给定 `tradeId`（通常为最细的 4 级行业），服务端递归其所有父级行业并叠加全行业（id=1），一次性返回链上全部启用敏感词。

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| *(身份)* | — | 是 | 见 §4.2 |
| `tradeId` | long | 是 | 起点行业 ID（通常为最细一级，如 4 级行业） |
| `platform` | int | 否 | 平台：`0`=抖音 `1`=快手 `2`=视频号；为空=不限平台（全平台 + 各平台专属词并集）。<br>（服务端内部 `platform_type` 约定 0=全平台/1=抖音/2=快手/3=视频号，与本字段相差 1，已自动映射，调用方无需关心） |
| `wordsType` | int | 否 | 词语类型：`0`=敏感词（默认）`1`=关键词；仅取系统词 |

**响应 `data`**：`List<SensitiveWord>`（按严重度升序，同词去重保留最严重等级）

| 字段 | 类型 | 说明 |
|---|---|---|
| `word` | string | 词本身（含主词与相似词，相似词已展开为独立条目） |
| `level` | int | 等级：`0`=1 级(封号) `1`=2 级(严重警告) `2`=3 级(警告)；**数值越小越严重** |
| `levelLabel` | string | 等级中文标签 |
| `type` | int | 类型：`0`=广告 `1`=品牌 `2`=国家 `3`=限制词 `4`=其他 |
| `typeLabel` | string | 类型中文标签 |
| `similarWords` | string | 相似词（原始库以 `_` 分隔的变体串；多数变体已展开为独立 `word`，此处为兜底） |

**示例响应**
```json
{
  "code": 0,
  "msg": "success",
  "data": [
    { "word": "最低价", "level": 0, "levelLabel": "1级(封号)", "type": 0, "typeLabel": "广告", "similarWords": "" },
    { "word": "国家级", "level": 1, "levelLabel": "2级(严重警告)", "type": 2, "typeLabel": "国家", "similarWords": "" }
  ]
}
```

---

### 6.5 主播详情

`POST /internal/ai-agent/anchor/detail`

取单个主播的「基础信息 + 配置信息 + 基础设置」，所有字典字段随响应解析为中文文案。

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| *(身份)* | — | 是 | 见 §4.2，用于租户/归属校验 |
| `secUid` | string | 是 | 主播唯一标识 |

**响应 `data`**（AnchorDetail）

**① 基础信息**

| 字段 | 类型 | 说明 |
|---|---|---|
| `secUid` | string | 主播唯一标识 |
| `anchorName` | string | 主播名称 |
| `anchorAvatar` | string | 头像 URL |
| `platform` / `platformLabel` | int / string | 平台 code + 文案 |
| `anchorNumber` | string | 主播账号 |
| `tradeId` / `tradeName` | long / string | 行业 ID + 名称 |
| `accountType` / `accountTypeLabel` | int / string | 自有/同行 code + 文案 |
| `addDate` | string | 添加时间 |
| `lastRecordTime` | string | 最后开始录制时间 |

**② 配置信息**（字典字段，原始 code + 解析文案）

| 字段 | 类型 | 字典 | 说明 |
|---|---|---|---|
| `accountStage` / `accountStageLabel` | int / string | `account_stage` | 账号阶段 |
| `accountWaterLevel` / `accountWaterLevelLabel` | int / string | `account_water_level` | 账号水平 |
| `accountFlow` / `accountFlowLabel` | int / string | `account_flow` | 流量结构 |

**③ 基础设置 `basicSettings`**（对象；不存在时为 `null`，源自 `tb_basic_settings`）

| 字段 | 类型 | 字典 | 说明 |
|---|---|---|---|
| `premiereDate` | string | — | 首播日期（`yyyy-MM-dd`） |
| `accountStage` / `accountStageLabel` | int / string | `account_stage` | 账号阶段 |
| `accountWaterLevel` / `accountWaterLevelLabel` | int / string | `account_water_level` | 账号水平 |
| `accountFlow` / `accountFlowLabel` | int / string | `account_flow` | 流量结构 |
| `livingTarget` / `livingTargetLabel` | int / string | `living_target` | 直播目标 |
| `livingModality` / `livingModalityLabel` | int / string | `living_modality` | 直播形态 |
| `marketing` / `marketingLabel` | int / string | `marketing` | 营销方式 |
| `livingMode` / `livingModeLabel` | int / string | `living_mode` | 直播模式 |
| `optimizeDirection` | string | `optimize_direction` | 优化方向（多选，已解析为中文文案） |
| `learning` | string | `learning` | 学习方向（多选，已解析为中文文案） |
| `anchorSituation` | string | — | 账号情况描述（自由文本） |

> 字典值为空或解析失败时，对应 `xxxLabel` 返回空字符串。基础设置不存在时 `basicSettings` 为 `null`。

**示例响应**
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "secUid": "MS4wLjABAAAA...",
    "anchorName": "示例主播",
    "anchorAvatar": "https://.../avatar.jpg",
    "platform": 0,
    "platformLabel": "抖音",
    "anchorNumber": "douyin123",
    "tradeId": 8801,
    "tradeName": "女装",
    "accountType": 0,
    "accountTypeLabel": "自有账号",
    "addDate": "2026-05-01 10:20:30",
    "lastRecordTime": "2026-06-10 19:00:00",
    "accountStage": 1,
    "accountStageLabel": "成长期",
    "accountWaterLevel": 2,
    "accountWaterLevelLabel": "中等水平",
    "accountFlow": 1,
    "accountFlowLabel": "自然流量为主",
    "basicSettings": {
      "premiereDate": "2026-01-15",
      "accountStage": 1,
      "accountStageLabel": "成长期",
      "accountWaterLevel": 2,
      "accountWaterLevelLabel": "中等水平",
      "accountFlow": 1,
      "accountFlowLabel": "自然流量为主",
      "livingTarget": 1,
      "livingTargetLabel": "涨粉",
      "livingModality": 2,
      "livingModalityLabel": "实景直播",
      "marketing": 1,
      "marketingLabel": "小黄车",
      "livingMode": 1,
      "livingModeLabel": "日不落",
      "optimizeDirection": "话术,选品",
      "learning": "憋单节奏",
      "anchorSituation": "主推女装，客单价 100-300。"
    }
  }
}
```

---

### 6.6 视频列表

`POST /internal/ai-agent/video/list`

按条件筛选当前范围（§4.3）内的录制视频。

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| *(身份)* | — | 是 | 见 §4.2 |
| `cursor` | long | 否 | 游标；首页传 null |
| `pageSize` | int | 否 | 每页条数，默认 50，范围 1~200 |
| `dataSourceType` | int | 否 | 数据源，默认 1；`1`=自己录制 `2`=全租户(仅 userType=0) `3`=云空间(已分享)，见 §4.3 |
| `secUidList` | string[] | 否 | 主播 secUid 集合（最多 500 个） |
| `anchorNameList` | string[] | 否 | 主播名称集合（服务端内部转换为 secUid 过滤，最多 500 个） |
| `startDate` | string | 否 | 录制开始时间范围-起 |
| `endDate` | string | 否 | 录制开始时间范围-止 |
| `accountType` | int | 否 | 归属类型：`0`=自有 `1`=同行；不传=全部 |
| `tradeId` | long | 否 | 行业 ID（支持父行业展开，同 §6.1） |
| `videoSliceType` | int | 否 | 视频切片类型：`0`=原视频 `1`=复盘切片视频 `2`=短视频切片视频 |

**响应 `data`**：游标外壳（§4.4），`list` 元素为 **VideoItem**：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | long | 主键 |
| `videoId` | string | 视频唯一标识（后续视频数据接口用它定位） |
| `platform` | int | 平台类型（0抖音/1快手/2视频号） |
| `secUid` | string | 主播 secUid |
| `anchorName` | string | 主播名称 |
| `videoName` | string | 视频名称 |
| `liveTitle` | string | 直播标题 |
| `tradeId` / `tradeName` | long / string | 行业 ID + 名称 |
| `accountType` / `accountTypeLabel` | int / string | 自有/同行 code + 文案 |
| `videoSliceType` / `videoSliceTypeLabel` | int / string | 切片类型 code + 文案 |
| `startTime` | string | 录制开始时间 |
| `endTime` | string | 录制结束时间 |
| `duration` | long | 时长（秒） |
| `playUrl` | string | 在线播放地址（云端视频播放 URL；本地未上传时为空） |
| `videoType` | int | 视频类型：`0`=ts `1`=flv `2`=mp4 |
| `definition` | int | 清晰度：`0`=标清 `1`=高清 `2`=超清 `3`=蓝光 |
| `storagePath` | string | 存储路径 |
| `analysisStatus` | int | 分析状态：`0`未分析 `1`分析中 `2`完成 `3`错误 |
| `hasDashboard` | boolean | 是否有数据看板（§6.8 可取数） |
| `hasBarrages` | boolean | 是否有弹幕（§6.9 可取数） |
| `hasChartData` | boolean | 是否有在线曲线（§6.10 可取数） |

> `hasDashboard` / `hasBarrages` / `hasChartData` 三个布尔位供 Agent 在拉取明细前先判断该视频有无对应数据，避免空调用。

**示例响应**
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "list": [
      {
        "id": 55012,
        "videoId": "v_20260610_001",
        "platform": 0,
        "secUid": "MS4wLjABAAAA...",
        "anchorName": "示例主播",
        "videoName": "6月10日晚场",
        "liveTitle": "夏装上新专场",
        "tradeId": 8801,
        "tradeName": "女装",
        "accountType": 0,
        "accountTypeLabel": "自有账号",
        "videoSliceType": 0,
        "videoSliceTypeLabel": "原视频",
        "startTime": "2026-06-10 19:00:00",
        "endTime": "2026-06-10 21:30:00",
        "duration": 9000,
        "playUrl": "https://.../play.m3u8",
        "videoType": 2,
        "definition": 1,
        "storagePath": "video/2026/06/10/xxx.mp4",
        "analysisStatus": 2,
        "hasDashboard": true,
        "hasBarrages": true,
        "hasChartData": true
      }
    ],
    "nextCursor": 55012,
    "hasMore": false
  }
}
```

---

### 6.7 视频音频段落全文

`POST /internal/ai-agent/video/audio-paragraphs`

取某视频逐分钟段落的语音转写全文（逐段全文，不含逐词时间戳），供 Agent 做话术 / 内容分析。

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| *(身份)* | — | 是 | 见 §4.2，用于租户校验 |
| `videoId` | string | 是 | 视频唯一标识 |

**响应 `data`**：`List<AudioParagraph>`（按 `paragraph` 升序）

| 字段 | 类型 | 说明 |
|---|---|---|
| `paragraph` | int | 段落序号（从 1 开始，即第几分钟段） |
| `time` | string | 该分钟段落对应的时间 |
| `content` | string | 该分钟段落的全文文本 |

> 服务端每段仅取 `content` 全文，丢弃逐词 `items`/`wordList`。无分析数据时返回空数组 `[]`。如需逐词/关键词检索，另走关键词检索能力，不在本接口。

**示例响应**
```json
{
  "code": 0,
  "msg": "success",
  "data": [
    { "paragraph": 1, "time": "2026-06-10 19:00:00", "content": "欢迎来到直播间，今天给大家带来……" },
    { "paragraph": 2, "time": "2026-06-10 19:01:00", "content": "这款产品的材质是……" }
  ]
}
```

---

### 6.8 视频数据看板

`POST /internal/ai-agent/video/dashboard`

取某视频的整体数据汇总、分段（看盘）数据明细，以及由实时采集曲线补充的降采样时序 + 福袋事件。

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| *(身份)* | — | 是 | 见 §4.2 |
| `videoId` | string | 是 | 视频唯一标识 |

**响应 `data`**（VideoDashboard）

**① 整体汇总**（巨量引擎口径）

| 字段 | 类型 | 说明 |
|---|---|---|
| `videoId` / `secUid` / `batchNumber` | string | 标识 / 直播场次号 |
| `anchorNumber` | string | 主播账号 |
| `isTakeProduct` | int | 是否带货 `0`否 `1`是 |
| `totalWatchNum` | int | 总观看人次 |
| `averageOnlineNum` | int | 平均在线人数 |
| `averageResidenceTime` | int | 平均停留时间（秒） |
| `incrementFollowerCount` | int | 新增粉丝数 |
| `convertFanRate` | double | 粉丝转化率 |
| `interactionPercent` | double | 互动率 |
| `volume` | int | 销售额 |
| `purchaseCount` | int | 销量 |
| `customerUnitPrice` | double | 客单价 |
| `uvValue` | double | UV 价值 |
| `goodsConvertRate` | double | 带货转换率 |
| `showWatchCntRatio` | double | 曝光-观看率（看播率） |
| `roi` | double | ROI |
| `launchRoiAmount` | double | 投放 ROI 金额（元） |
| `refundAmount` | double | 退款金额（元） |
| `overallCostRoi` | double | 整体消耗 ROI |
| `netTransactionRoi` | double | 净成交 ROI |
| `watchFlowList` | array | 看播流量结构，元素见「流量结构项」 |
| `payFlowList` | array | 成交流量结构，元素见「流量结构项」 |

**流量结构项**（`watchFlowList` / `payFlowList` 元素，`FlowSourceDto`）：

| 字段 | 类型 | 说明 |
|---|---|---|
| `channelName` | string | 渠道 / 来源名称 |
| `ratio` | double | 流量占比（`1` 表示 100%） |
| `subFlow` | array | 子级流量（结构与本项相同，可为 null） |

**② 分段看盘数据 `paragraphs`**（数组，无数据时为空数组；仅含有效段落，源自 `tb_video_data_viewing_paragraph`）

| 字段 | 类型 | 说明 |
|---|---|---|
| `batchNumber` | string | 直播场次号 |
| `totalWatchNum` | int | 总观看人次 |
| `averageOnlineNum` | int | 平均在线人数 |
| `averageResidenceTime` | int | 平均停留时间（秒） |
| `incrementFollowerCount` | int | 新增粉丝数 |
| `convertFanRate` | double | 粉丝转化率 |
| `interactionPercent` | double | 互动率 |
| `volumeStart` / `volumeEnd` | int | 销售额区间（元） |
| `purchaseCountStart` / `purchaseCountEnd` | int | 销量区间 |
| `customerUnitPriceStart` / `customerUnitPriceEnd` | double | 客单价区间（元） |
| `uvValueStart` / `uvValueEnd` | double | UV 价值区间 |
| `goodsConvertRateStart` / `goodsConvertRateEnd` | double | 带货转换率区间 |
| `isTakeProduct` | int | 是否带货 `0`否 `1`是 |
| `dataStatus` | int | 数据状态：`1`=拉取成功 `8`=数据整理中 |

**③ 实时采集补充**（来自 `onlineChartData`；曲线已降采样压缩，**非全量**。巨量/蝉妈妈看板不含以下时序与福袋，此处由实时采集补充）

| 字段 | 类型 | 说明 |
|---|---|---|
| `totalBarrageNum` | int | 弹幕总数 |
| `onlineDataList` | array | 在线人数折线（降采样），元素见「折线点」 |
| `approachDataList` | array | 进场人数折线（降采样） |
| `exitPeopleDataList` | array | 离场人数折线（降采样） |
| `payComboCntDataList` | array | 成交折线（巨量百应时序，降采样） |
| `payAmtDataList` | array | 成交金额折线（巨量百应时序，降采样） |
| `followAnchorUcntDataList` | array | 新增粉丝折线（巨量百应时序，降采样） |
| `blessBagList` | array | 福袋事件（压缩版），元素见「福袋项」 |

**折线点**（`CurvePointVo`）：`{ "dateTime": <毫秒时间戳 long>, "valueNum": <int> }`

**福袋项**（`blessBagList` 元素，`DashboardBlessBagVo`）：

| 字段 | 类型 | 说明 |
|---|---|---|
| `relativeTime` | long | 相对开播毫秒（便于定位「第几分钟」开的福袋） |
| `blessBagReward` | string | 福袋奖品 |
| `candidateNum` | int | 参与人数 |

> 看板汇总不存在时，①各汇总字段为 `null` / 0，`paragraphs` 与 ③各折线仍按实有数据返回（可能为空数组）。
> §6.10（在线曲线）返回的是**全量原始折线 + 人群画像**；本接口 ③ 是同源折线的**降采样压缩版**，供只需趋势不需精度的场景直接一次取回，避免二次调用。

**示例响应（节选）**
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "videoId": "v_20260610_001",
    "secUid": "MS4wLjABAAAA...",
    "batchNumber": "B20260610190000",
    "anchorNumber": "douyin123",
    "isTakeProduct": 1,
    "totalWatchNum": 125000,
    "averageOnlineNum": 320,
    "averageResidenceTime": 95,
    "incrementFollowerCount": 540,
    "convertFanRate": 0.43,
    "interactionPercent": 0.18,
    "volume": 86000,
    "purchaseCount": 430,
    "customerUnitPrice": 200.0,
    "uvValue": 0.68,
    "goodsConvertRate": 0.034,
    "showWatchCntRatio": 0.12,
    "roi": 2.6,
    "launchRoiAmount": 12000.0,
    "refundAmount": 3000.0,
    "overallCostRoi": 2.1,
    "netTransactionRoi": 2.4,
    "watchFlowList": [
      { "channelName": "直播推荐", "ratio": 0.52, "subFlow": null }
    ],
    "payFlowList": [
      { "channelName": "直播间下单", "ratio": 0.78, "subFlow": null }
    ],
    "paragraphs": [
      {
        "batchNumber": "B20260610190000",
        "totalWatchNum": 60000,
        "averageOnlineNum": 300,
        "averageResidenceTime": 90,
        "incrementFollowerCount": 260,
        "convertFanRate": 0.41,
        "interactionPercent": 0.17,
        "volumeStart": 30000,
        "volumeEnd": 50000,
        "purchaseCountStart": 150,
        "purchaseCountEnd": 250,
        "customerUnitPriceStart": 180.0,
        "customerUnitPriceEnd": 220.0,
        "uvValueStart": 0.6,
        "uvValueEnd": 0.75,
        "goodsConvertRateStart": 0.03,
        "goodsConvertRateEnd": 0.04,
        "isTakeProduct": 1,
        "dataStatus": 1
      }
    ],
    "totalBarrageNum": 9300,
    "onlineDataList": [
      { "dateTime": 1749553200000, "valueNum": 300 },
      { "dateTime": 1749553260000, "valueNum": 420 }
    ],
    "approachDataList": [],
    "exitPeopleDataList": [],
    "payComboCntDataList": [],
    "payAmtDataList": [],
    "followAnchorUcntDataList": [],
    "blessBagList": [
      { "relativeTime": 600000, "blessBagReward": "9.9元红包", "candidateNum": 1200 }
    ]
  }
}
```

---

### 6.9 视频弹幕列表

`POST /internal/ai-agent/video/barrages`

取某视频的弹幕。数据量可能很大，**支持游标分页**；需「全部」时循环翻页直到 `hasMore=false`。

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| *(身份)* | — | 是 | 见 §4.2 |
| `videoId` | string | 是 | 视频唯一标识 |
| `cursor` | long | 否 | 上一页返回的 `nextCursor`；首页传 null |
| `pageSize` | int | 否 | 每页条数，默认 200、上限 500 |

**响应 `data`**：游标外壳（§4.4），`list` 元素为 **BarrageItem**：

| 字段 | 类型 | 说明 |
|---|---|---|
| `recordDate` | long | 记录时间戳（毫秒） |
| `batchNumber` | string | 直播场次号 |
| `nickName` | string | 弹幕用户昵称 |
| `content` | string | 弹幕内容 |
| `isNew` | boolean | 是否新用户 |
| `level` | long | 用户等级 |
| `fansLevelCurrent` | long | 当前粉丝团等级 |
| `countSendNum` | int | 弹幕发送次数 |
| `isBlessBag` | boolean | 是否福袋弹幕 |

> 底层存储为 TableStore，`cursor` 实为页码，由服务端封装（对 Agent 透明，仍是 `cursor`/`nextCursor`），按「原样回传直到 hasMore=false」使用即可。

**示例响应**
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "list": [
      {
        "recordDate": 1749553200000,
        "batchNumber": "B20260610190000",
        "nickName": "用户A",
        "content": "这款多少钱？",
        "isNew": true,
        "level": 12,
        "fansLevelCurrent": 3,
        "countSendNum": 1,
        "isBlessBag": false
      }
    ],
    "nextCursor": 2,
    "hasMore": true
  }
}
```

---

### 6.10 视频在线曲线 + 人群画像

`POST /internal/ai-agent/video/online-curve`

一次取回某视频的**全量原始折线**（在线 / 进离场 / 弹幕 / 成交等）与看播 / 成交人群画像。

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| *(身份)* | — | 是 | 见 §4.2 |
| `videoId` | string | 是 | 视频唯一标识 |
| `step` | int | 否 | 折线抽样步长，默认 `1`（越大点越稀疏） |

**响应 `data`**（OnlineCurve）

**① 概览**

| 字段 | 类型 | 说明 |
|---|---|---|
| `totalViewersNum` | int | 累计观看人数 |
| `maxOnlineNum` | int | 最大在线人数 |
| `totalBarrageNum` | int | 弹幕总数 |

**② 折线组**（每条为数组，元素为「折线点」）

| 字段 | 说明 |
|---|---|
| `onlineDataList` | 在线人数折线 |
| `approachDataList` | 进场人数折线 |
| `exitPeopleDataList` | 离场人数折线 |
| `barrageDataList` | 弹幕折线 |
| `payComboCntDataList` | 成交折线 |
| `payAmtDataList` | 成交金额折线 |
| `followAnchorUcntDataList` | 新增粉丝折线 |

**折线点**（`CurvePointVo`）：

| 字段 | 类型 | 说明 |
|---|---|---|
| `dateTime` | long | 时间戳（毫秒） |
| `valueNum` | int | 数值 |

**③ 人群画像**（`watchUserPortrait` 看播 / `payUserPortrait` 成交，结构相同，无数据时为 `null`）

| 字段 | 类型 | 说明 |
|---|---|---|
| `agePortrait` | array | 年龄分布，元素见「画像项」 |
| `genderPortrait` | array | 性别分布，元素见「画像项」 |
| `provincePortrait` | array | 省份分布，元素见「画像项」 |

**画像项**（`UserPortraitItemDto`）：

| 字段 | 类型 | 说明 |
|---|---|---|
| `label` | string | 分组名（如「18-24岁」「女」「广东」） |
| `value` | string | 占比（`1` 表示 100%） |

> 折线无数据时为空数组；画像无数据时为 `null`。

**示例响应（节选）**
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "totalViewersNum": 125000,
    "maxOnlineNum": 860,
    "totalBarrageNum": 9300,
    "onlineDataList": [
      { "dateTime": 1749553200000, "valueNum": 300 },
      { "dateTime": 1749553260000, "valueNum": 420 }
    ],
    "approachDataList": [],
    "exitPeopleDataList": [],
    "barrageDataList": [],
    "payComboCntDataList": [],
    "payAmtDataList": [],
    "followAnchorUcntDataList": [],
    "watchUserPortrait": {
      "agePortrait": [
        { "label": "18-24岁", "value": "0.32" },
        { "label": "25-30岁", "value": "0.41" }
      ],
      "genderPortrait": [
        { "label": "女", "value": "0.78" },
        { "label": "男", "value": "0.22" }
      ],
      "provincePortrait": [
        { "label": "广东", "value": "0.19" }
      ]
    },
    "payUserPortrait": null
  }
}
```

### 6.11 达人列表

`POST /internal/ai-agent/influencer/list`

达人 = 当前租户订阅的达人（`tb_video_user_influencer_subscription`，只按租户过滤、不过滤用户；同一达人多用户订阅时按达人去重、行业取其一）；附「行业」+「最近采集时间」。游标外壳（`CursorPageVo`，`nextCursor` 为 long）。

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| *(身份)* | — | 是 | 见 §4.2 |
| `cursor` | long | 否 | 游标；首页传 null |
| `pageSize` | int | 否 | 默认 50，范围 1~200 |
| `platformTypeList` | byte[] | 否 | 平台（**多选**）：`1`抖音 `2`快手 `3`视频号 |
| `keyword` | string | 否 | 昵称 / 抖音号 模糊 |
| `influencerIdList` | long[] | 否 | 达人 id 集合（最多 500） |
| `collectStartTime` | string | 否 | 采集时间-起（`yyyy-MM-dd HH:mm:ss`，按达人最近采集时间 last_sync_time） |
| `collectEndTime` | string | 否 | 采集时间-止 |

**响应 `data`**：游标外壳，`list` 元素为 **InfluencerItem**：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | long | 达人 id（§6.12/§6.15 用它定位） |
| `platformType` | byte | `1`抖音 `2`快手 `3`视频号 |
| `platformAccount` | string | 平台账号（抖音号等） |
| `platformUserId` | string | 平台用户 id |
| `nickname` | string | 昵称 |
| `avatar` | string | 头像 URL |
| `followersCount` | long | 粉丝数 |
| `followingCount` | long | 关注数 |
| `videoCount` | int | 平台侧作品总数（达人库口径） |
| `likeCount` | long | 获赞数 |
| `verificationStatus` | byte | 认证状态：`0`未认证 `1`个人 `2`企业/机构 `3`政府/官方 `4`媒体/特殊 |
| `industryId` | long | 行业 id（来自本租户对该达人的订阅；可能为 null） |
| `industryName` | string | 行业名称（`industryId` 非空时按行业库回填；为空或行业不存在时为 null） |
| `lastCollectTime` | string | 最近采集时间 |

---

### 6.12 达人短视频列表

`POST /internal/ai-agent/influencer/video/list`

范围 = **本租户已采集的视频**，按达人过滤。`withAudioText=true` 时额外带原文/AI 音频转文字。游标外壳（long）。

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| *(身份)* | — | 是 | |
| `cursor` | long | 否 | 游标 |
| `pageSize` | int | 否 | 默认 50，范围 `1~5000`（本接口用于「拆解达人全部短视频」，上限放宽到 5000；服务端按 500 分批回捞） |
| `influencerIdList` | long[] | 是 | 达人 id 集合（至少 1 个，最多 500） |
| `platformTypeList` | byte[] | 否 | `1/2/3` |
| `extractStatusList` | byte[] | 否 | 文案提取状态：`0`未提取 `1`已提取 |
| `publishStartTime` | string | 否 | 发布时间-起 |
| `publishEndTime` | string | 否 | 发布时间-止 |
| `withAudioText` | boolean | 否 | 默认 false；true 时挂载原文/AI 文字 |

**响应 `data`**：游标外壳，`list` 元素为 **InfluencerVideoItem**（§6.15/§6.16/§6.19 复用同结构）：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | long | 视频 id（§6.13/§6.16 用它定位） |
| `platformType` | byte | `1/2/3` |
| `platformVideoId` | string | 平台视频 id |
| `videoHash` | string | 视频 hash（内容关联键） |
| `title` | string | 标题 |
| `coverUrl` | string | 封面 URL |
| `videoUrl` | string | 播放 URL |
| `authorId` | string | 作者 id（= 达人 id 字符串） |
| `authorName` | string | 作者名称 |
| `likeCount` / `commentCount` / `shareCount` / `collectCount` | long | 点赞 / 评论 / 分享 / 收藏 |
| `duration` | int | 时长（秒） |
| `publishTime` | string | 发布时间 |
| `extractStatus` | byte | `0`未提取 `1`已提取 |
| `analysisStatus` | byte | `0`未分析 `1`已分析 |
| `collectTime` | string | 本租户采集时间（**仅租户接口有值；全库 §6.15/§6.16 恒 null**） |
| `originalAudioContent` | string | 原文音频转文字（**仅 `withAudioText=true`**；Mongo 无内容为 null） |
| `audioContent` | string | AI 优化后音频转文字（**仅 `withAudioText=true`**） |

---

### 6.13 批量短视频原文音频转文字

`POST /internal/ai-agent/influencer/video/audio-text/batch`

按 `tb_video_info` 主键批量取原文/AI 文字，**按 id 直查、不校验归属**。无分页。

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| *(身份)* | — | 是 | |
| `videoIdList` | long[] | 是 | 视频 id 集合（1~500） |

**响应 `data`**：数组，元素为 **VideoAudioText**：

| 字段 | 类型 | 说明 |
|---|---|---|
| `videoId` | long | 视频 id |
| `videoHash` | string | 视频 hash |
| `originalAudioContent` | string | 原文（未优化；无内容为 null） |
| `audioContent` | string | AI 优化后 |

---

### 6.14 批量达人短视频统计

`POST /internal/ai-agent/influencer/video/stats/batch`

**仅租户已采集**维度，按达人聚合：已采集视频数 + 点赞/评论/分享/收藏汇总。无分页。

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| *(身份)* | — | 是 | |
| `influencerIdList` | long[] | 是 | 达人 id 集合（1~500） |
| `publishStartTime` | string | 否 | 把统计限定在某发布时间段-起 |
| `publishEndTime` | string | 否 | -止 |

**响应 `data`**：数组（与入参达人 id 一一对应），元素为 **InfluencerVideoStats**：

| 字段 | 类型 | 说明 |
|---|---|---|
| `influencerId` | long | 达人 id |
| `videoCount` | int | 已采集视频数 |
| `totalLikeCount` / `totalCommentCount` / `totalShareCount` / `totalCollectCount` | long | 互动汇总 |

---

### 6.15 全库短视频搜索

`POST /internal/ai-agent/influencer/video/global/search`

跨租户读**公共爬取库**（全量短视频），按达人/关键词/#标签/指标/发布时间过滤，可按指标排序。keyset 流式（`StreamPageVo`，`nextCursor` 为 string）。

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| *(身份)* | — | 是 | |
| `cursor` | string | 否 | keyset 游标令牌；首页传 null |
| `pageSize` | int | 否 | 默认 50，范围 1~200 |
| `influencerIdList` | long[] | 否 | 达人 id（= author_id）集合，最多 500；不传=全库 |
| `platformTypeList` | byte[] | 否 | `1/2/3` |
| `keyword` | string | 否 | 标题/描述模糊 |
| `tagList` | string[] | 否 | 匹配标题内 `#标签`（任一命中），最多 20 |
| `likeCountMin` / `commentCountMin` / `shareCountMin` / `collectCountMin` | long | 否 | 指标下限（>=） |
| `durationMin` / `durationMax` | int | 否 | 时长区间（秒） |
| `publishStartTime` / `publishEndTime` | string | 否 | 发布时间区间 |
| `extractStatusList` | byte[] | 否 | `0`未提取 `1`已提取 |
| `sortField` | int | 否 | `0`默认(id) `1`点赞 `2`评论 `3`分享 `4`收藏 `5`发布时间 |
| `sortDesc` | boolean | 否 | 默认 true |
| `withAudioText` | boolean | 否 | 默认 false |
| `tenantScopeExtract` | boolean | 否 | 默认 **false**；`extractStatus` / 文案是否走「本租户口径」，见下方 ⚠️ |

**响应 `data`**：keyset 外壳，`list` 元素为 **InfluencerVideoItem**（同 §6.12，`collectTime` 恒 null）。

> ⚠️ **`authorIdList` vs `platformUserIdList` / `platformAccountList`**
>
> 三者都是达人维度过滤、之间 AND 收窄，但取数路径完全不同：
>
> - `authorIdList` → **直接比 `tb_video_info.author_id`，不反查达人表**。
> - `platformUserIdList` / `platformAccountList` → 先经 `tb_video_influencer_info` 反查出 id，再比 `author_id`。
>
> 「看某个爆款上榜账号在本选题下的作品」**必须用 `authorIdList`** —— 爆款同步链路不写 `tb_video_influencer_info`，上榜的全网账号多数不在该表里，用另外两个参数会被达人表的低覆盖率吃掉、返回空页。详见 §6.22 的覆盖率说明。

> ⚠️ **`tenantScopeExtract` 口径开关**
>
> - `false`（默认，**模型工具口径**，行为与历史完全一致）：`extractStatus` 取 `tb_video_info` 原值，即「**全网**是否被提取过」；`withAudioText=true` 时文案不设授权闸。
> - `true`（**前端口径**）：`extractStatus` 只在**本租户**提取成功（`tb_video_user_video.extract_status=3`）时为 `1`，文案同样按提取授权放行 —— 与 §6.12 / §6.15(`collectedOnly`) / §6.16 一致。
>
> 用途：前端拿 `extractStatus` 做「看文案」入口时必须传 `true`，否则会出现「显示已提取、点进去没有文案」的**假入口**（全网提取过 ≠ 本租户读得到）。
>
> 缓存：首页（`cursor=null`）短期缓存的键在 `true` 时并入 `tenantId` 分片；`false` 路径键仍身份无关、跨租户共享，命中率不受影响。

---

### 6.16 批量短视频完整明细

`POST /internal/ai-agent/influencer/video/detail/batch`

按 `tb_video_info` id 批量取**完整明细（含全部指标字段）**，可选原文；**按 id 直查、不校验归属**。无分页。用于「拆解单条短视频」。

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| *(身份)* | — | 是 | |
| `videoIdList` | long[] | 是 | 视频 id 集合（1~500） |
| `withAudioText` | boolean | 否 | 默认 false |

**响应 `data`**：数组，元素为 **InfluencerVideoItem**（同 §6.12，`collectTime` 恒 null）。

---

### 6.17 全库达人搜索

`POST /internal/ai-agent/influencer/search`

跨租户读**公共达人库**，按昵称/账号/平台/粉丝过滤，可按粉丝/获赞排序。用于「找对标达人账号」。keyset 流式（string）。

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| *(身份)* | — | 是 | |
| `cursor` | string | 否 | keyset 游标 |
| `pageSize` | int | 否 | 默认 50，范围 1~200 |
| `platformTypeList` | byte[] | 否 | `1/2/3` |
| `keyword` | string | 否 | 昵称 / 平台账号 模糊 |
| `followersCountMin` | long | 否 | 粉丝下限（>=） |
| `sortField` | int | 否 | `0`粉丝数 `1`获赞数 |
| `sortDesc` | boolean | 否 | 默认 true |

**响应 `data`**：keyset 外壳，`list` 元素为 **InfluencerItem**（同 §6.11；`industryId` / `industryName` / `lastCollectTime` 恒 null，因非租户订阅维度）。

---

### 6.18 爆款关键词列表

`POST /internal/ai-agent/hotsearch/keyword/list`

全库爆款搜索词，供筛选参考。keyset 流式（string）。

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| *(身份)* | — | 是 | |
| `cursor` | string | 否 | keyset 游标 |
| `pageSize` | int | 否 | 默认 50，范围 1~200 |
| `platformTypeList` | byte[] | 否 | `1/2/3` |
| `keyword` | string | 否 | 模糊匹配 search_keyword |
| `syncStartTime` / `syncEndTime` | string | 否 | 最后同步时间区间 |
| `sortField` | int | 否 | `0`视频数 `1`最后同步时间 |
| `sortDesc` | boolean | 否 | 默认 true |
| `loadMe` | boolean | 否 | 默认 false；`true`=只看本租户订阅监控的爆款词（走 `tb_video_user_hot_subscription`），订阅词为空时返回空页、**不退化为全量**；该路径结果因租户而异，**首页不缓存** |
| `industryId` | long | 否 | 行业过滤（= `tb_trade.id`）。⚠️ **仅 `loadMe=true` 时可用**，见下方说明 |

**响应 `data`**：keyset 外壳，`list` 元素为 **HotSearchKeyword**：

| 字段 | 类型 | 说明 |
|---|---|---|
| `searchId` | long | 爆款搜索 id（§6.19 用它定位） |
| `platformType` | byte | `1/2/3` |
| `searchKeyword` | string | 关键词 |
| `videoCount` | int | 视频总数 |
| `lastSyncTime` | string | 最后同步时间 |

> ⚠️ **`industryId` 的适用边界**
>
> 行业维度**只存在于租户订阅表** `tb_video_user_hot_subscription.industry_id`；全库爆款词表 `tb_video_hot_search` **没有行业字段**。因此：
>
> - `loadMe=true` + `industryId` → 在订阅词范围内按行业收窄，正常生效。
> - `loadMe` 未传 / 为 false + `industryId` → **直接返回错误** `industryId 仅在 loadMe=true 时可用`。不做静默忽略 —— 静默忽略会让调用方以为已按行业过滤，实际拿到的是全量，属于「假过滤」。
>
> **id 口径**：`industry_id` 经 `TradeFeign.listTradeByIds` 解析为 `TradeVo`，与达人域（§6.11 的 `industryId`）、主播域（§6.2 `trade-options` 的 `tradeId`）**是同一套 `tb_trade` 主数据**，可以复用同一份行业选项做下拉。
>
> ⚠️ **但候选集不同**：§6.2 `trade-options` 返回的是「本租户**主播**身上出现过的行业」，不是「本租户**爆款订阅词**上出现过的行业」。用它直接当下拉选项，会出现两种错配 —— 选了某行业却查出 0 条词（该行业没有订阅词），或某个有订阅词的行业不在下拉里（没有对应主播）。
>
> **「未分类」怎么看**：`industry_id` 可能为 null —— 新增订阅（`VideoHotSubscriptionAddBo`）有 `@NotNull` 校验，但编辑接口（`VideoHotSubscriptionEditBo`）没有，且既有代码到处是 `.filter(Objects::nonNull)` + 「未知行业」兜底，说明历史数据里存在无行业的订阅词。口径如下：
>
> - **不传 `industryId`（前端「全部行业」）→ 包含未分类的词**。该路径生成的订阅表查询只有 `tenant_id` 条件，不带 `industry_id`，无行业的订阅词照常返回。
> - 传了任何 `industryId` → 未分类的词**不会**被选中（`industry_id = ?` 等值匹配，null 不参与）。
> - **没有「只看未分类」的独立筛选**。`industryId=null` 的语义是「不过滤」而非「筛未分类」，不要用魔法值（如 `-1` / `0`）去试探。确有此需求应新增显式开关。

---

### 6.19 爆款视频列表

`POST /internal/ai-agent/hotsearch/video/list`

某爆款关键词下的短视频，按标题/#标签/指标/发布时间过滤，可排序。keyset 流式（string）。用于「按爆款关键词筛参考视频」。

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| *(身份)* | — | 是 | |
| `searchId` | long | 是 | 爆款搜索 id（来自 §6.18） |
| `cursor` | string | 否 | keyset 游标 |
| `pageSize` | int | 否 | 默认 50，范围 1~200 |
| `keyword` | string | 否 | 标题/描述模糊 |
| `authorIdList` | string[] | 否 | **达人 id 直筛**（= `tb_video_info.author_id`，同 §6.21 `topAuthors[].authorId`），最多 500。见下方 ⚠️ |
| `tagList` | string[] | 否 | 标题内 `#标签`（任一命中），最多 20 |
| `likeCountMin` / `commentCountMin` / `shareCountMin` / `collectCountMin` | long | 否 | 指标下限 |
| `durationMin` / `durationMax` | int | 否 | 时长区间（秒） |
| `publishStartTime` / `publishEndTime` | string | 否 | 发布时间区间 |
| `sortField` | int | 否 | `0`默认(爆款内排序 sort_order) `1`点赞 `2`评论 `3`分享 `4`收藏 `5`发布时间 |
| `sortDesc` | boolean | 否 | 默认 true |
| `withAudioText` | boolean | 否 | 默认 false |

**响应 `data`**：keyset 外壳，`list` 元素为 **InfluencerVideoItem**（同 §6.12，`collectTime` 恒 null）。

---

### 6.20 达人统计概览（单达人档案下钻）

`POST /internal/ai-agent/influencer/analytics/overview`

服务端一次聚合返回单达人全部统计指标（KPI / 互动结构 / 时长分布 / 发布热力 / 互动趋势 / 爆款 / 能力雷达），**前端只渲染、禁止拉全量自算**。用于「单达人档案下钻」（设计 doc17）。

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| *(身份)* | — | 是 | |
| `influencerId` | long | 是 | 达人 id（= §6.11 的 `id`） |
| `platformType` | byte | 否 | `1`抖音 `2`快手 `3`视频号；不传=不按平台收窄 |
| `publishStartTime` / `publishEndTime` | string | 否 | 统计时间窗（发布时间）；不传默认近 3 个月 |
| `scope` | byte | 否 | 统计口径：`1`本租户已采集（默认，只按租户过滤、**不叠加子账号 userId**） `2`该达人全库爬取作品 |

**时间范围硬约束（防打爆库，三层设防之 fupan 兜底层）**：窗口跨度**硬上限 = 1 年（≤366 天）**。默认近 3 个月；`start` 早于 `end - 366天` 时服务端**夹取**到 `end - 366天`，越界不做无界扫描。`scope=2` 数据量更大，务必带时间窗。

**响应 `data`（InfluencerAnalyticsVo）**

| 字段 | 类型 | 说明 |
|---|---|---|
| `sampleCount` | int | 参与统计的作品数（时间窗内、去重后） |
| `avgEngagement` / `medianEngagement` / `maxEngagement` | long | 单条互动量（=赞+评+享+藏）均/中位/最高 |
| `maxEngagementVideoId` / `maxEngagementTitle` | long / string | 最高单条定位 |
| `postingPerWeek` | double | 更新频率（条/周，按作品实际发布跨度折算） |
| `breakoutCount` / `breakoutRate` / `breakoutThreshold` | int / double / long | 爆款数 / 率 / 阈值（口径 = `3 × 中位互动量`；中位为 0 时不识别爆款） |
| `engagementCV` | double | 互动稳健离散度 = `MAD(中位绝对偏差) / 中位数`（抗长尾爆款离群，越小越稳）；**中位为 0** 时退回 `标准差/均值` |
| `sumLike` / `sumComment` / `sumShare` / `sumCollect` | long | 互动结构（环图） |
| `durationBuckets` | array | 时长分布 `[{bucket:"0-15s", count}]`（固定 5 桶：`0-15s/15-30s/30-60s/60-180s/180s+`，含 0 桶） |
| `publishHeatmap` | array | 发布节奏 `[{weekday:1-7, slot:0-5, count}]`（周一=1；slot 每 4 小时一段：0=[0,4)…5=[20,24)；仅非零单元） |
| `trendSeries` | array | 互动趋势 `[{date, engagement, videoCount}]`（按自然周降采样，`date`=周一，最多 16 个点，升序） |
| `radar` | object | 能力雷达该达人值 `{reach, output, breakout, engagement, virality, consistency}`（0–100） |
| `radarBaseline` | object | 同赛道达人库中位基准（对标虚线）；**当前恒 null**——缺达人品类字段 + 达人库基准作业（doc17 需求4，P2） |
| `sumPlay` / `avgPlay` / `engagementRate` | long / double / double | **播放量到位后**返回（需求2 `playCount`）；**当前恒 null**——爬取库暂无 `play_count` 列 |

> 口径说明：`engagement = 赞+评+享+藏`。雷达 0–100 为**固定锚点归一化**（reach: 10^7 粉丝≈100；output: 1 条/天≈100；breakout: 率×100；engagement: 10^6 均互动≈100；virality: 分享占比×5 封顶；consistency: 100/(1+robustCV)，平滑衰减、CV 越大越低但不硬归零），非赛道分位；由 fupan 定义，agent/前端不重算。单达人时间窗内作品有 2 万条兜底上限。

---

### 6.21 爆款选题拆解聚合

`POST /internal/ai-agent/hotsearch/analytics/overview`

某爆款关键词（选题）下的选题分析，**服务端一次聚合返回全部指标，前端只渲染、禁止拉分页自算**。回答四个业务问题：目标线在哪 / 该做什么内容 / 这选题还值不值得做 / 拍多长什么时候发。用于前端「爆款选题」模块的拆解分析层，以及模型工具 `get_hot_topic_analytics`。

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| *(身份)* | — | 是 | |
| `searchId` | long | 是 | 爆款搜索 id（来自 §6.18） |
| `platformType` | byte | 否 | `1`抖音 `2`快手 `3`视频号；不传=不按平台收窄 |
| `publishStartTime` / `publishEndTime` | string | 否 | 统计时间窗（按 `publish_time`）；**不传默认近 7 天** |
| `tenantScopeExtract` | boolean | 否 | 保留位，**当前不改变任何返回值**（`tenantExtractedCount` 恒按 `tenantId` 计算），仅为与 §6.19 调用侧语义对齐 |

**时间范围硬约束**：跨度硬上限 **1 年（≤366 天）**，`start` 早于 `end - 366天` 时服务端夹取，越界不做无界扫描。

**样本口径（前端须如实标注）**

- 样本 = 该 `searchId` + 时间窗 + 平台 下按 `engagement` **降序前 N 条**，`N = 20000`。
- 命中上限 → `truncated=true` + 服务端 warn 日志；前端应标注「统计基于前 N 条」。
- `engagement = 赞 + 评 + 享 + 藏`。爬取库**无 `play_count`**，故本接口**不提供**播放量 / 互动率 / 完播类指标。
- **除 `tenantExtractedCount` 外，全部指标为全网爬取库口径，不是本租户口径** —— 前端文案需说清「选题热度是全网口径」。
- 小样本不在服务端特殊处理，照常返回真实数字（建议前端 `<20` 标注仅供参考、`<5` 不渲染门槛/集中度/互动结构三块）。
- ⚠️ **`n < 10` 时 `topDecileThreshold` 与 `top10Share` 是算术恒真产物，不是信号**：`ceil(n×10%)` 恒为 1 → 门槛线恒等于 `maxEngagement`；前 10 条即全部样本 → `top10Share` 恒为 `1.0`。消费侧（前端 / 模型工具）在该档位应**隐去这两个数**，否则会被读成「门槛极高 + 赢家通吃」。同档位的 `medianEngagement` / `maxEngagement` / 互动结构仍是真实信号。
- `searchId` 不存在 / 已删除、或窗口内 0 条 → **200 + 全零/空数组**（`durationBuckets` 仍返回全 5 桶），不报错。
- ⚠️ **0 样本要分三种情况，别只分两种**（回显字段在 0 样本时照常返回，可据此区分）：

  | 条件 | 语义 | 消费侧应说 |
  |---|---|---|
  | `lastSyncTime == null` | 爆款词不存在 / 已软删（整条 `tb_video_hot_search` 记录取不到） | 选题已失效，回词表重选 |
  | `lastSyncTime < windowStart` | **采集缺口** —— 该词在这段窗口内根本没同步过 | 这是数据缺口，**不等于**该选题没热度 |
  | `lastSyncTime >= windowStart` 且 0 条 | 窗口内确实无作品 | 这段时间该选题没热度 |

**响应 `data`（HotSearchAnalyticsVo）**

| 字段 | 类型 | 说明 |
|---|---|---|
| `searchId` / `searchKeyword` / `platformType` / `lastSyncTime` | long / string / byte / string | 回显，取自 `tb_video_hot_search` |
| `windowStart` / `windowEnd` | string | **实际生效**的统计窗口（兜底 + 夹取后的值） |
| `sampleCount` | int | 参与统计的作品数 |
| `truncated` | boolean | 样本是否命中上限被截断 |
| `medianEngagement` / `maxEngagement` | long | 单条互动量中位 / 最高 |
| `maxEngagementVideoId` / `maxEngagementTitle` | long / string | 最高单条定位 |
| `topDecileThreshold` | long | **爆款门槛线** = 样本互动量降序第 `ceil(n × 10%)` 位的值；样本 `<10` 条时等于最高值 |
| `top10Share` | double | **集中度** = 互动量最高前 10 条之和 / 样本总互动量（0~1，越高越"赢家通吃"）；总量为 0 时返 0 |
| `engagementCV` | double | 互动稳健离散度 = `MAD(中位绝对偏差) / 中位数`；中位为 0 时退回 `标准差/均值`（口径与 §6.20 一致） |
| `sumLike` / `sumComment` / `sumShare` / `sumCollect` | long | 互动结构（环图），回答"该做干货 / 争议 / 情绪" |
| `trendGranularity` | string | `"day"`（窗口跨度 ≤15 天）或 `"week"`（>15 天），由服务端按窗口自动决定并回显 |
| `trendSeries` | array | 互动趋势 `[{date, engagement, videoCount}]`，升序；`day` 最多 31 点、`week` 最多 16 点（`date` 为周一），超出保留最近的点 |
| `durationBuckets` | array | 时长分布 `[{bucket, count, avgEngagement}]`，固定 5 桶 `0-15s/15-30s/30-60s/60-180s/180s+`，**含 count=0 桶**；`avgEngagement` 为该桶单条均互动量（count=0 时为 0）——业务问的是"哪个片长更容易爆"，不是"哪个片长条数多" |
| `publishHeatmap` | array | 发布节奏 `[{weekday:1-7, slot:0-5, count}]`（周一=1；slot 每 4 小时一段；仅非零单元） |
| `topAuthors` | array | 头部达人 **Top10** `[{authorId, authorName, platformType, avatar, followersCount, platformUserId, videoCount, sumEngagement, maxEngagement}]`，排序 `videoCount DESC, sumEngagement DESC, authorId ASC`。回答"谁在吃这个选题、我该对标谁" |
| `topTags` | array | 高频 #标签 **Top20** `[{tag, count, sumEngagement}]`，排序 `count DESC, sumEngagement DESC, tag ASC` |
| `tenantExtractedCount` | int | **样本内本租户已提取成功文案的条数**（文案可用性；唯一的租户口径字段） |

**字段口径补充**

- `topAuthors[].authorId` 取自 `tb_video_info.author_id`，与 §6.19 列表项的 `authorId` **同口径** —— 可直接回传给 §6.19 的 `authorIdList`，查该账号在本选题下的作品。
- **档案字段 `avatar` / `followersCount` / `platformUserId` 取自 `tb_video_hot_search_video` 的冗余达人列** —— 它们跟着爆款视频一起同步，对上榜账号必然有值；不走 `tb_video_influencer_info`（那张表只由达人订阅链路填充，爆款上榜的全网账号多数查不到，见 §6.22）。所以这里**没有** `platformAccount` / `verificationStatus` / `verificationInfo` —— 冗余列里就没有，要认证信息得走 §6.22 并接受低命中率。
- `authorName` 优先取 `tb_video_info.author_name`，为空时退回冗余列的 `influencer_nickname`。
- 样本已按互动量降序，**所有档案字段一律取第一个非空值**（= 该达人互动量最高那条作品所在行），保证同一达人的各字段尽量来自同一次爬取快照，而非东拼西凑。字段仍可能为 null（上游没给），消费侧按「缺失即不显示」处理。
- `topTags[].tag` 抽取规则（确定性，**不做中文分词 / n-gram，不造词**）：正则 `#([^#\s，,。、!！?？:：;；]{1,30})`，即从 `#` 抽到空白 / 中英标点 / 下一个 `#` 为止；`toLowerCase` 归一 + `trim`；同一条视频内同名标签只计 1 次（去重后再累加，避免刷标题的账号污染）。
- `platformType`（顶层与 `topAuthors[]` 内嵌）均为**内部码 `1/2/3` 原样返回**，服务端不做任何对外码转换。

**缓存**：60s 短期缓存，键 = `tenantId + searchId + platformType + 原始 publishStartTime/publishEndTime`。键用**原始入参**而非兜底后的窗口 —— 不传时间窗时兜底值含 `now()`，用兜底值做键会导致每次请求键都不同、缓存恒失效；代价是 `windowStart`/`windowEnd` 回显最多滞后一个 TTL，对 7~60 天档位无实际影响。

**性能提示**：样本排序是复合互动量 `(赞+评+享+藏) DESC`，是表达式排序，**任何索引都救不了、必然 filesort**；成本由 `search_id` 过滤后的行数决定。过滤侧已被线上索引 `idx_snapshot_query (search_id, is_deleted, sort_order)` 覆盖（`WHERE hs.is_deleted=0 AND hs.search_id=?` 命中最左两列，索引区间扫描），**无需新增 DDL**（2026-08-18 核对，见 [`ai-agent-search-optimization-backlog.md`](./ai-agent-search-optimization-backlog.md)「已覆盖」小节）。叠加 `LIMIT 20000` 兜底，上千条量级无压力。

---

### 6.22 批量达人档案

`POST /internal/ai-agent/influencer/detail/batch`

按 `author_id` 批量取**全库爬取达人库**的档案，不分页。用于「爆款选题上榜账号」这类场景 —— 拿到一批 `authorId` 后一次取回昵称/头像/认证/粉丝数。

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| *(身份)* | — | 是 | |
| `authorIdList` | string[] | 是 | 达人 id 集合，最多 **50** 个。取值 = `tb_video_info.author_id` 原值，与 §6.19 列表项 `authorId`、§6.21 `topAuthors[].authorId` **同口径** |

**响应 `data`（InfluencerDetailBatchVo）**

| 字段 | 类型 | 说明 |
|---|---|---|
| `list` | array | 查到档案的达人，元素为 **InfluencerItem**（同 §6.11），按入参顺序、已去重 |
| `notFoundAuthorIds` | string[] | **没查到档案的 id，原样回显** |

`list` 元素中 `industryId` / `industryName` / `lastCollectTime` / `accountType` **恒 null** —— 本接口读全库爬取达人库，不含租户订阅信息，口径同 §6.17。

> ⚠️ **命中率天然低于 100%，务必处理 `notFoundAuthorIds`**
>
> 爆款同步链路（`VideoHotSearchSyncService`）只把达人信息**冗余写进** `tb_video_hot_search_video`（`influencer_nickname` / `influencer_avatar` / `influencer_followers_count` / `influencer_platform_user_id`），**不落 `tb_video_influencer_info`**。而本接口查的是后者 —— 它主要由「达人订阅」链路填充。
>
> 结果：**爆款上榜账号未必有档案**。这不是 bug，是两条链路的数据覆盖差异。调用方应按 `notFoundAuthorIds` 做降级展示（例如只显示 `authorName`），不要靠比对数量去猜，更不要当成查询失败。
>
> **id 口径**：`author_id` 是 varchar 列，语义上是 `tb_video_influencer_info.id` 的字符串形式 —— 这个不变式在代码里有四处依赖（`VideoInfluencerProducer:752` 写入侧 `item.setAuthorId(influencerEntity.getId().toString())`；`resolveAuthorIdFilter` / `resolveSubscribedAuthorIds` / `listAuthorClipMetrics` 读取侧均按此比较）。服务端按数字解析后直查主键；**解析不了的 id 也会进 `notFoundAuthorIds`** —— 出现大量非数字 id 说明上游 author_id 口径变了，请立刻反馈。

---

## 7. 设计取舍 / 待确认项

1. **接口路径前缀** `/internal/ai-agent/*`；如需与现有 `/internal/crm` 风格统一或改名（避免与「代理渠道 replay-agent」歧义），可评审定。
2. **弹幕「全部」**：底层 TableStore 弹幕量大，采用**游标分页**而非一次性返回全量，避免超大 payload / 超时。若确需「一次拿全」，需评估服务端硬上限。
3. **行业父子展开**：`tradeId` 过滤默认展开为「自身 + 所有子孙行业」。如只想精确匹配单个行业，需新增「是否含子行业」开关。
4. **看板 vs 在线曲线折线**：§6.8 dashboard 的 ③ 折线是**降采样压缩版**（省调用），§6.10 online-curve 是**全量原始版 + 画像**（要精度）。两者同源，按需选用。
5. **视频列表「主播名称集合」过滤**：服务端内部把名称集合转换为 secUid 再过滤（对调用方透明）。
6. **数据源 `dataSourceType`**：列表接口新增，控制「自己录制 / 全租户 / 云空间」；`2/3` 的越权由服务端按 `userType` 自动回落，调用方无需自行判断。
7. **全库检索性能（§6.15/§6.17/§6.18/§6.19）**：keyset 已保证 top-N 准确，但排序列索引 + 文本 `LIKE` 前导通配是性能待优化项，**索引 / FULLTEXT 方案见** [`ai-agent-search-optimization-backlog.md`](./ai-agent-search-optimization-backlog.md)（待排期，未实施）。

---

## 8. 实现备注（后端内部，非 Agent 关注）

| 接口 | 复用的底层能力 |
|---|---|
| 6.1 主播列表 | `tb_anchor_url_user` ⨝ `tb_anchor_url`；过滤 tenantId(+userId) + isRemoveRecord；行业树展开；游标分页 |
| 6.2 行业聚合 | 范围内 `tb_anchor_url_user.trade_id` 去重 + 批量取名 |
| 6.3 行业层级链 | 行业父链回溯（自身→父→祖父……），天然按层级排序，映射 `id/name/parentId` |
| 6.4 行业敏感词 | 行业父链 + 全行业(id=1) 取系统敏感词（`platform_type IN (0, platform+1)` 映射），按词去重保留最严重等级 |
| 6.5 主播详情 | `AnchorUrlEntity` + `AnchorUrlUserEntity` + `BasicSettingsEntity`；字典 Feign 解析文案 |
| 6.6 音频段落全文 | 在线分析结果 `AnalysisResultCloudVo.fileAudioaAlyses`；每段 `dataJson` 仅取 `content` 全文，丢弃逐词 `items`/`wordList` |
| 6.7 数据看板 | 汇总 `OceanEngineDataInfoVo`；分段 `tb_video_data_viewing_paragraph`；③ 时序 + 福袋源自 `onlineChartData`（降采样压缩） |
| 6.8 弹幕 | TableStore 弹幕检索（replay-third） |
| 6.9 在线曲线+画像 | `onlineChartData`(`OnlineChartVo`) + `OceanEngineDataInfoVo` 的 watch/pay 画像 |

> 身份从请求体显式取（非 `GlobalObject.getLocalUser()`），Service 显式传 `userId/tenantId/userType` 调用底层能力。

---

## 9. 联调清单（落地后补凭证再执行）

- [ ] §3 鉴权：两个请求头已带且与下发值一致（`x-jiuyu-client-id` / `api-key`）。
- [ ] §4.3 范围：用主账号(`userType=0`, `dataSourceType=2`) 与子账号(`userType=2`) 同租户分别调 §6.1，验证 0 返回整租户、2 仅本人、子账号 `dataSourceType=2` 自动回落。
- [ ] §6.1：分别用 `platforms` / `tradeId`(父行业) / `accountType` / 时间范围过滤，验证结果正确且翻页 `nextCursor` 链路通。
- [ ] §6.3：`tradeId` 与 `tradeName` 两种入参分别验证层级链正确。
- [ ] §6.4：给定 4 级 `tradeId`，验证返回覆盖父链 + 全行业词，`levelLabel`/`typeLabel` 非空。
- [ ] §6.5：验证 `accountStageLabel` 等字典文案非空且与字典一致。
- [ ] §6.6：`videoSliceType` 三值分别过滤；`dataSourceType=3` 验证云空间视频可见。
- [ ] §6.7/6.9/6.10：用有完整数据的 `videoId` 验证非空；用无数据 `videoId` 验证空数组 / null 不报错。
- [ ] §6.8：验证 ③ 降采样折线与 §6.10 全量折线趋势一致；`blessBagList` 非空。
- [ ] §6.9：循环翻页直到 `hasMore=false`，确认能取全。

---

## 10. 联系人 / 反馈

- 后端：fupan-server 后端组（本批接口负责人）。
- 文档版本：随 `feature/agent-data-query` 分支提交演进。
