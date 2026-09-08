<!-- module: agent -->
<!-- area: api -->
<!-- generated-by: reverse-scan -->
<!-- last-scan: 2026-05-20 -->
<!-- source-paths: replay-api/.../controller/agent/, replay-agent/.../controller/ -->

# Agent API -- 接口契约

> `replay-agent` 模块对外暴露的 REST 接口。绝大部分入口在 `replay-api` 模块的 `controller/agent/` 包下，少数客户端侧接口由 `replay-agent` 模块自身的 `controller/` 暴露。
>
> 所有接口默认：
> - `@CrossOrigin` 允许跨域
> - 返回 `R<T>`（`com.jiuyu.replay.generic.vo.common.R`）
> - 列表查询返回 `R<PageUtils<T>>`（分页用 `PageUtils`）
> - 鉴权走全局登录态（除标注外，未额外 `@APIKey` 注解）
> - 没有任何接口标注 `@NoRepeatSubmit`

---

## 一、代理商主表 (`/replay/agent`) -- AgentController

| Method | Path | Summary | Request | Response |
|---|---|---|---|---|
| POST | `/replay/agent/list` | 代理商列表 | `AgentListBo` (body) | `R<PageUtils<AgentListVo>>` |
| GET | `/replay/agent/info` | 代理商信息 | `id: Long` (query, 必填) | `R<AgentInfoVo>` |
| GET | `/replay/agent/currentUserInfo` | 代理商用户获取对应代理商信息 | -- | `R<AgentInfoVo>` |
| POST | `/replay/agent/save` | 新增代理商 | `AgentBo` (body) | `R<String>` data=初始密码/null |
| POST | `/replay/agent/update` | 修改代理商 | `AgentBo` (body, id & channelId 必填) | `R<String>` data=新密码/null |
| GET | `/replay/agent/delete` | 删除代理商 | `id: Long` (query, 必填) | `R<String>` |
| POST | `/replay/agent/updateAgentStatus` | 修改代理商状态 | `AgentBo` (body, id & agentStatus) | `R<String>` |

### Request: `AgentBo`
| Field | Type | Required | Validation | Meaning |
|---|---|---|---|---|
| id | Long | save 时后端生成 / update 时必填 | -- | 雪花 ID |
| agentName | String | 否 (save 时按渠道名同步) | -- | 代理商名称 |
| parentId | Long | 否 | -- | 父代理商 ID |
| agentType | Integer | 是 | `0 \| 1` | 0 普通代理商 / 1 渠道代理商 (update 不可变) |
| channelId | Long | 是 | `> 0` | 渠道 ID，不重复 |
| operationUserId | Long | 否 | -- | 运营人员用户 ID |
| contactName | String | 否 (普通代理商建议必填) | -- | 联系人姓名 → 同步后台账号 nickName |
| contactPhone | String | 否 (普通代理商必填) | 不重复 | 联系人手机号 → 同步后台账号 phone |
| contactAddress | String | 否 | -- | 联系地址 |
| tradeId | Long | 否 | -- | 行业 ID |
| commissionRate | Double | 是 | `[0,1]` | 新签佣金比例 |
| renewalCommissionRate | Double | 是 | `[0,1]` | 续费佣金比例 |
| agentStatus | Integer | 是 | `0 \| 1` | 0 未启用 / 1 启用中 |
| createUserId | Long | 否 | -- | 创建人用户 ID |
| agentUrlCode | String | 后端生成 | -- | 代理商邀请码 (10 位) |
| posterImgIds | String | 否 | `_` 分隔 | 海报图片文件 ID 列表 |
| btnBgColor | String | 否 | -- | 按钮颜色 |
| btContent | String | 否 | -- | 按钮文案 |
| userId | Long | 后端写入 | -- | 普通代理商联动后台账号 ID |

### Request: `AgentListBo` (extends `PageBo`)
| Field | Type | Required | Default | Meaning |
|---|---|---|---|---|
| page | Integer | 否 | 1 | 当前页 |
| limit | Integer | 否 | 10 | 每页记录数 |
| agentName | String | 否 | -- | 代理商名称筛选 |
| contactKeyword | String | 否 | -- | 联系人姓名/手机号模糊搜索 |
| operationUserId | Long | 否 | -- | 运营人员用户 ID |
| channelId | Long | 否 | -- | 渠道 ID（可级联子节点） |
| channelIds | List\<Long\> | 否 | -- | 渠道 ID 集合 |
| employeeStatus | Integer | 否 | -- | 员工状态 0离职/1在职 |
| agentType | Integer | 否 | -- | 代理商类型 0普通/1渠道 |

### Response: `AgentVo` (base -- AgentInfoVo 和 AgentListVo 共同父类)
| Field | Type | Meaning |
|---|---|---|
| id | Long | 雪花 ID |
| agentName | String | 代理商名称 |
| parentId | Long | 父代理商 ID |
| agentType | Integer | 0 普通 / 1 渠道 |
| channelId | Long | 渠道 ID |
| channelName | String | 渠道名称（producer 装配） |
| operationUserId | Long | 运营人员用户 ID |
| contactName | String | 联系人姓名 |
| contactPhone | String | 联系人手机号 |
| contactAddress | String | 联系地址 |
| tradeId | Long | 行业 ID |
| commissionRate | Double | 新签佣金比例 |
| renewalCommissionRate | Double | 续费佣金比例 |
| agentStatus | Integer | 0 未启用 / 1 启用中 |
| createUserId | Long | 创建人用户 ID |
| agentUrlCode | String | 代理商邀请码 |
| posterImgIds | String | 海报图片文件 ID (`_` 分隔) |
| btnBgColor | String | 按钮颜色 |
| btContent | String | 按钮文案 |
| userId | Long | 后台账号用户 ID |
| employeeStatus | Integer | 员工状态 0离职/1在职 |
| createDate | Date | 创建时间 |
| updateDate | Date | 最后修改时间 |
| isDeleted | Integer | 是否已删除 |

### Response: `AgentInfoVo` (extends AgentVo, info/currentUserInfo 返回)
| Field | Type | Meaning |
|---|---|---|
| posterImgList | List\<FileShowVo\> | 海报图片信息列表 |
| tradeName | String | 行业名称（关联查询） |
| operationUserName | String | 运营人员名称 |
| createUserName | String | 创建人名称 |
| url | String | 代理商邀请链接地址 (agent.url + urlCode) |

### Response: `AgentListVo` (extends AgentVo, list 返回)
| Field | Type | Meaning |
|---|---|---|
| operationUserName | String | 运营人员名称 |
| tradeName | String | 行业名称 |
| createUserName | String | 创建人名称 |
| url | String | 代理商邀请链接地址 |

### 错误码
| 场景 | code | message |
|---|---|---|
| 渠道已被关联 | `8001` (TIP_CUSTOM) | "当前渠道已关联代理商" |
| 佣金超出 [0,1] | `8001` | "佣金只能设置0-100%范围内" |
| 普通代理商手机号重复 | `8001` | "代理商中已存在该手机号" / "该手机号已存在后台用户，无法创建" |
| 修改 agentType | `8001` | "不能修改代理商类型" |
| 启用时员工离职 | `SYSTEM_BUSY` | "请先修改代理商的状态为在职" |
| `agent_user_role_id` 未配置 | `SYSTEM_BUSY` | "没有配置代理商角色，请联系管理员" |

---

## 二、用户来源渠道 (`/replay/channel`) -- ChannelController

| Method | Path | Summary | Request | Response |
|---|---|---|---|---|
| POST | `/replay/channel/list` | 渠道列表 | `ChannelListBo` (body) | `R<PageUtils<ChannelListVo>>` |
| GET | `/replay/channel/info` | 渠道信息 | `id: Long` (query, 必填) | `R<ChannelInfoVo>` |
| POST | `/replay/channel/save` | 新增渠道 | `ChannelBo` (body) | `R<String>` |
| POST | `/replay/channel/update` | 修改渠道 | `ChannelBo` (body) | `R<String>` |
| GET | `/replay/channel/delete` | 删除渠道 | `id: Long` (query, 必填) | `R<String>` |
| GET | `/replay/channel/listTree` | 树形渠道列表 | `childrenNotNull: Integer` (query, optional, 0=null/1=空集合) | `R<List<ChannelTreeVo>>` |

### Request: `ChannelBo`
| Field | Type | Required | Validation | Meaning |
|---|---|---|---|---|
| id | Long | save 时后端生成 / update 时必填 | -- | 雪花 ID |
| parentId | Long | 否 | -- | 父渠道 ID |
| channelName | String | 是 | -- | 渠道名称 |

### Response: `ChannelVo` (base)
| Field | Type | Meaning |
|---|---|---|
| id | Long | 雪花 ID |
| parentId | Long | 父渠道 ID |
| channelName | String | 渠道名称 |
| createDate | Date | 创建时间 |
| updateDate | Date | 最后修改时间 |
| isDeleted | Integer | 是否已删除 |

### Response: `ChannelTreeVo`
| Field | Type | Meaning |
|---|---|---|
| id | Long | 雪花 ID |
| parentId | Long | 父渠道 ID |
| channelName | String | 渠道名称 |
| children | List\<ChannelTreeVo\> | 子渠道列表 |
| createDate | Date | 创建时间 |
| updateDate | Date | 最后修改时间 |
| isDeleted | Integer | 是否已删除 |

---

## 三、代理商佣金 (`/replay/agentcommission`) -- AgentCommissionController

| Method | Path | Summary | Request | Response |
|---|---|---|---|---|
| POST | `/replay/agentcommission/list` | 佣金列表 | `AgentCommissionListBo` (body) | `R<PageUtils<AgentCommissionListVo>>` |
| GET | `/replay/agentcommission/info` | 佣金信息 | `id: Long` (query, 必填) | `R<AgentCommissionInfoVo>` |
| POST | `/replay/agentcommission/save` | 新增佣金 | `AgentCommissionBo` (body) | `R<String>` |
| POST | `/replay/agentcommission/update` | 修改佣金 | `AgentCommissionBo` (body) | `R<String>` |
| GET | `/replay/agentcommission/delete` | 删除佣金 | `id: Long` (query, 必填) | `R<String>` |
| GET | `/replay/agentcommission/commissionRecords` | 佣金结算记录 | `agentId: Long` (query) | `R<List<CommissionRecordsVo>>` |

### Request: `AgentCommissionBo`
| Field | Type | Required | Validation | Meaning |
|---|---|---|---|---|
| id | Long | save 时后端生成 / update 时必填 | -- | 雪花 ID |
| agentId | Long | 是 | -- | 代理商 ID |
| promotionId | Long | 否 | -- | 代理商推广渠道 ID |
| agentSaleId | Long | 否 | -- | 代理商销售 ID |
| orderId | Long | 否 | -- | 订单 ID |
| userId | Long | 否 | -- | 用户 ID |
| orderTotalMoney | Integer | 否 | -- | 订单总金额（分） |
| commission | Double | 否 | -- | 佣金比例 |
| commissionMoney | Integer | 否 | -- | 佣金金额（分） |
| commissionType | Integer | 是 | `0 \| 1` | 0 新签佣金 / 1 续费佣金 |
| commissionMode | Integer | 是 | `0 \| 1` | 0 代理商分佣 / 1 推广渠道分佣 |
| commissionTime | Date | 否 | -- | 分佣时间 |
| remarks | String | 否 | -- | 备注 |

### Response: `AgentCommissionVo` (base -- InfoVo 和 ListVo 的父类)
| Field | Type | Meaning |
|---|---|---|
| id | Long | 雪花 ID |
| agentId / promotionId / agentSaleId | Long | 代理商/推广渠道/销售 ID |
| orderId / userId | Long | 订单/用户 ID |
| orderTotalMoney | Integer | 订单总金额（分） |
| commission | Double | 佣金比例 |
| commissionMoney | Integer | 佣金金额（分） |
| commissionType | Integer | 0 新签 / 1 续费 |
| commissionMode | Integer | 0 代理商分佣 / 1 推广渠道分佣 |
| commissionTime | Date | 分佣时间 |
| remarks | String | 备注 |
| createDate / updateDate / isDeleted | Date/Date/Integer | 审计字段 |

### Response: `CommissionRecordsVo` (月度结算汇总，commissionRecords 返回)
| Field | Type | Meaning |
|---|---|---|
| startDate | String | 开始时间 yyyy-MM |
| endDate | String | 结束时间 yyyy-MM |
| commissionRate | Double | 新签佣金比例 |
| commissionRateAmount | Double | 新签实付总额 |
| renewalCommissionRate | Double | 续费佣金比例 |
| renewalCommissionRateAmount | Double | 续费实付总额 |
| settlementAmount | Double | 佣金结算总额（分） |
| status | Integer | 结算状态 0使用中/1已结束 |

> 实际分佣触发走内部 `AgentCommissionBll#commissionAllocation()`，由订单成功事件回调，不对外暴露 HTTP 端点。

---

## 四、代理商推广渠道 (`/replay/agentpromotion`) -- AgentPromotionController

| Method | Path | Summary | Request | Response |
|---|---|---|---|---|
| GET | `/replay/agentpromotion/listByAgentId` | 获取代理商的推广渠道列表 | `agentId: Long` (query, 必填) | `R<List<AgentPromotionInfoVo>>` |
| POST | `/replay/agentpromotion/list` | 推广渠道列表 | `AgentPromotionListBo` (body) | `R<PageUtils<AgentPromotionListVo>>` |
| GET | `/replay/agentpromotion/info` | 推广渠道信息 | `id: Long` (query, 必填) | `R<AgentPromotionInfoVo>` |
| POST | `/replay/agentpromotion/save` | 新增推广渠道 | `AgentPromotionBo` (body) | `R<String>` |
| POST | `/replay/agentpromotion/update` | 修改推广渠道 | `AgentPromotionBo` (body) | `R<String>` |
| GET | `/replay/agentpromotion/delete` | 删除推广渠道 | `id: Long` (query, 必填) | `R<String>` |

### Request: `AgentPromotionBo`
| Field | Type | Required | Validation | Meaning |
|---|---|---|---|---|
| id | Long | save 时后端生成 / update 时必填 | -- | 雪花 ID |
| agentId | Long | 是 | -- | 代理商 ID |
| promotionName | String | 是 | -- | 推广渠道名称 |
| posterImgIds | String | 否 | `_` 分隔 | 海报图片文件 ID 列表 |
| btnBgColor | String | 否 | -- | 按钮颜色 |
| btContent | String | 否 | -- | 按钮文案 |
| commissionRate | Double | 是 | `[0,1]` | 新签佣金比例 |
| renewalCommissionRate | Double | 是 | `[0,1]` | 续费佣金比例 |
| promotionUrlCode | String | 后端生成 | -- | 推广渠道邀请码 |
| promotionStatus | Integer | 是 | `0 \| 1` | 0 未启用 / 1 启用中 |

### Response: `AgentPromotionVo` (base)
| Field | Type | Meaning |
|---|---|---|
| id | Long | 雪花 ID |
| agentId | Long | 代理商 ID |
| promotionName | String | 推广渠道名称 |
| posterImgIds | String | 海报图片文件 ID (`_` 分隔) |
| posterImgList | List\<FileShowVo\> | 海报图片信息列表（后端富化） |
| btnBgColor | String | 按钮颜色 |
| btContent | String | 按钮文案 |
| commissionRate | Double | 新签佣金比例 |
| renewalCommissionRate | Double | 续费佣金比例 |
| promotionUrlCode | String | 推广渠道邀请码 |
| promotionStatus | Integer | 0 未启用 / 1 启用中 |
| createDate / updateDate / isDeleted | Date/Date/Integer | 审计字段 |

### Response: `AgentPromotionInfoVo` (extends AgentPromotionVo)
| Field | Type | Meaning |
|---|---|---|
| url | String | 渠道 URL 链接 (agent.url + promotionUrlCode) |

---

## 五、代理商销售 (`/replay/agentsale`) -- ApiAgentSaleController

| Method | Path | Summary | Request | Response |
|---|---|---|---|---|
| GET | `/replay/agentsale/listByAgentId` | 获取代理商的销售列表 | `agentId: Long` (query, 必填) | `R<List<AgentSaleInfoVo>>` |
| POST | `/replay/agentsale/list` | 销售列表 | `AgentSaleListBo` (body) | `R<PageUtils<AgentSaleListVo>>` |
| GET | `/replay/agentsale/info` | 销售信息 | `id: Long` (query, 必填) | `R<AgentSaleInfoVo>` |
| POST | `/replay/agentsale/save` | 新增销售 | `AgentSaleBo` (body) | `R<String>` |
| POST | `/replay/agentsale/update` | 修改销售 | `AgentSaleBo` (body) | `R<String>` |
| GET | `/replay/agentsale/delete` | 删除销售 | `id: Long` (query, 必填) | `R<String>` |

### Request: `AgentSaleBo`
| Field | Type | Required | Validation | Meaning |
|---|---|---|---|---|
| id | Long | save 时后端生成 / update 时必填 | -- | 雪花 ID |
| agentId | Long | 是 | -- | 代理商 ID |
| saleName | String | 是 | -- | 销售人员名称 |
| salePhone | String | 是 | 不重复 | 销售人员手机号 |
| saleStatus | Integer | 是 | `0 \| 1` | 0 未启用 / 1 启用中 |
| createUserId | Long | 否 | -- | 创建人用户 ID |
| promotionIds | List\<Long\> | 否 | -- | 推广渠道 ID 集合（一次性写入销售-推广渠道关系） |

### Response: `AgentSaleVo` (base)
| Field | Type | Meaning |
|---|---|---|
| id | Long | 雪花 ID |
| agentId | Long | 代理商 ID |
| saleName | String | 销售人员名称 |
| salePhone | String | 销售人员手机号 |
| saleStatus | Integer | 0 未启用 / 1 启用中 |
| createUserId | Long | 创建人用户 ID |
| createDate / updateDate / isDeleted | Date/Date/Integer | 审计字段 |

### Response: `AgentSaleInfoVo` (extends AgentSaleVo)
| Field | Type | Meaning |
|---|---|---|
| agentPromotionStr | String | 推广渠道名称串（逗号分隔） |
| agentPromotionList | List\<AgentPromotionInfoVo\> | 推广渠道信息列表 |

### 子接口：通过渠道 ID 查代理商销售 (`/replay/agent/agentSale`) -- AgentSaleController (replay-agent 模块)
| Method | Path | Summary | Request | Response |
|---|---|---|---|---|
| GET | `/replay/agent/agentSale/listByChannelId` | 通过渠道 ID 查询代理商销售列表 | `channelId: String` (query) | `R<List<AgentSaleInfoVo>>` |

---

## 六、代理商-销售-渠道关联 (`/replay/agentsalepromotionchannel`) -- AgentSalePromotionChannelController

| Method | Path | Summary | Request | Response |
|---|---|---|---|---|
| POST | `/replay/agentsalepromotionchannel/list` | 关联表列表 | `AgentSalePromotionChannelListBo` (body) | `R<PageUtils<AgentSalePromotionChannelListVo>>` |
| GET | `/replay/agentsalepromotionchannel/info` | 关联表信息 | `id: Long` (query, 必填) | `R<AgentSalePromotionChannelInfoVo>` |
| POST | `/replay/agentsalepromotionchannel/save` | 新增关联 | `AgentSalePromotionChannelBo` (body) | `R<String>` |
| POST | `/replay/agentsalepromotionchannel/update` | 修改关联 | `AgentSalePromotionChannelBo` (body) | `R<String>` |
| GET | `/replay/agentsalepromotionchannel/delete` | 删除关联 | `id: Long` (query, 必填) | `R<String>` |

### Request/Response: `AgentSalePromotionChannelBo/Vo`
| Field | Type | Required | Meaning |
|---|---|---|---|
| id | Long | save 时后端生成 / update 时必填 | 雪花 ID |
| promotionChannelId | Long | 是 | 推广渠道 ID |
| agentSaleId | Long | 是 | 代理商销售 ID |
| saleUrlCode | String | 后端生成 | 销售渠道邀请链接 code |
| createDate / updateDate / isDeleted | Date/Date/Integer | -- | 审计字段 |

---

## 七、代理商平台销售 (`/replay/agentplatformsale`) -- AgentPlatformSaleController

| Method | Path | Summary | Request | Response |
|---|---|---|---|---|
| GET | `/replay/agentplatformsale/listByAgentId` | 列代理商关联的平台销售 | `agentId: Long` (query, 必填) + `salesType: Integer` (optional, 默认 0) | `R<List<AgentPlatformSaleInfoVo>>` |
| POST | `/replay/agentplatformsale/list` | 平台销售列表 | `AgentPlatformSaleListBo` (body) | `R<PageUtils<AgentPlatformSaleListVo>>` |
| GET | `/replay/agentplatformsale/info` | 平台销售信息 | `id: Long` (query, 必填) | `R<AgentPlatformSaleInfoVo>` |
| POST | `/replay/agentplatformsale/save` | 新增平台销售 | `AgentPlatformSaleBo` (body) | `R<String>` |
| POST | `/replay/agentplatformsale/update` | 修改平台销售 | `AgentPlatformSaleBo` (body) | `R<String>` |
| GET | `/replay/agentplatformsale/delete` | 删除平台销售 | `id: Long` (query, 必填) | `R<String>` |

### Request: `AgentPlatformSaleBo`
| Field | Type | Required | Validation | Meaning |
|---|---|---|---|---|
| id | Long | save 时后端生成 / update 时必填 | -- | 雪花 ID |
| agentId | Long | 是 | -- | 代理商 ID |
| saleId | Long | 是 | -- | 销售人员 ID（关联 tb_agent_sale） |
| channelQrcodeImgId | Long | 否 | -- | 渠道二维码图片文件 ID |

### Response: `AgentPlatformSaleVo` (base)
| Field | Type | Meaning |
|---|---|---|
| id | Long | 雪花 ID |
| agentId | Long | 代理商 ID |
| saleId | Long | 销售人员 ID |
| channelQrcodeImgId | Long | 渠道二维码图片文件 ID |
| createDate / updateDate / isDeleted | Date/Date/Integer | 审计字段 |

### Response: `AgentPlatformSaleInfoVo` (extends AgentPlatformSaleVo)
| Field | Type | Meaning |
|---|---|---|
| channelQrcodeImg | FileShowVo | 渠道二维码图片信息（后端富化） |
| salesName | String | 销售人员名称 |
| phone | String | 销售人员手机号 |
| url | String | 销售人员推广码链接 |
| salesType | Integer | 销售类型 0平台销售/1代理商销售 |

---

## 八、邀请链接 Code (`/replay/inviteurlcode`) -- InviteUrlCodeController

| Method | Path | Summary | Request | Response |
|---|---|---|---|---|
| POST | `/replay/inviteurlcode/list` | 邀请码列表 | `InviteUrlCodeListBo` (body) | `R<PageUtils<InviteUrlCodeListVo>>` |
| GET | `/replay/inviteurlcode/infoByCode` | 根据 code 查信息 | `code: String` (query, 必填) | `R<InviteUrlCodeInfoVo>` |
| GET | `/replay/inviteurlcode/info` | 邀请码详情 | `id: Long` (query, 必填) | `R<InviteUrlCodeInfoVo>` |
| POST | `/replay/inviteurlcode/save` | 新增邀请码 | `InviteUrlCodeBo` (body) | `R<String>` |
| POST | `/replay/inviteurlcode/update` | 修改邀请码 | `InviteUrlCodeBo` (body) | `R<String>` |
| GET | `/replay/inviteurlcode/delete` | 删除邀请码 | `id: Long` (query, 必填) | `R<String>` |

### Request: `InviteUrlCodeBo`
| Field | Type | Required | Validation | Meaning |
|---|---|---|---|---|
| id | Long | save 时后端生成 / update 时必填 | -- | 雪花 ID |
| urlCode | String | 后端生成 | -- | URL 链接 code |
| agentId | Long | 否 | -- | 代理商 ID |
| promotionId | Long | 否 | -- | 代理商推广渠道 ID |
| agentSaleId | Long | 否 | -- | 代理商销售 ID |
| userId | Long | 否 | -- | 用户 ID |
| subUserId | Long | 否 | -- | 子账号用户 ID |
| activityId | Long | 否 | -- | 活动 ID |
| codeType | Integer | 是 | `0\|1\|2\|3` | 0代理商/1推广渠道/2用户/3代理商销售 |

### Response: `InviteUrlCodeVo` (base)
| Field | Type | Meaning |
|---|---|---|
| id | Long | 雪花 ID |
| urlCode | String | URL 链接 code |
| agentId / promotionId / agentSaleId | Long | 代理商/推广渠道/销售 ID |
| userId / subUserId | Long | 用户/子账号用户 ID |
| activityId | Long | 活动 ID |
| codeType | Integer | 0代理商/1推广渠道/2用户/3代理商销售 |
| tenantId | Long | 租户 ID |
| createDate / updateDate / isDeleted | Date/Date/Integer | 审计字段 |

### Response: `InviteUrlCodeInfoVo` (extends InviteUrlCodeVo, info/infoByCode 返回)
| Field | Type | Meaning |
|---|---|---|
| saleId | Long | 销售人员 ID |
| saleQrcodeImg | FileShowVo | 销售二维码图片 |
| posterImgList | List\<FileShowVo\> | 代理商海报图片列表（按 posterImgIds 解析） |
| btnBgColor | String | 按钮颜色 |
| btContent | String | 按钮文案 |

---

## 九、客户端获取邀请链接 (`/replay/agent/inviteurlcode`) -- InviteUrlController (replay-agent 模块)

| Method | Path | Summary | Auth | Response |
|---|---|---|---|---|
| GET | `/replay/agent/inviteurlcode/getUserInviteUrl` | 获取当前用户的邀请链接 | token required | `R<String>` (完整 URL = agent.url + urlCode) |

> 走 Redisson 锁防并发；内部调用 `ActivityFeign.infoActivateById(null)` 取当前生效活动。

---

## 十、客户端邀请活动 / 进度 / 奖励（代码保留，未上线）

> 以下 Controller 的 `@RestController` 注解被注释（`//@RestController`），当前**未对外开放 HTTP 端点**。代码保留以备启用。字段细节见 `activity_api.md`。

| Path（保留） | Controller 文件 | 端点数量 |
|---|---|---|
| `/replay/clientinviteactivity/*` | `ClientInviteActivityController` | 5 (list/info/save/update/delete) |
| `/replay/clientinviteprogress/*` | `ClientInviteProgressController` | 6 (含 clientGetInviteProgressAndReward) |
| `/replay/clientinviteprogressreward/*` | `ClientInviteProgressRewardController` | 5 (list/info/save/update/delete) |
| `/replay/clientinviterewardrecord/*` | `ClientInviteRewardRecordController` | 5 (list/info/save/update/delete) |
| `/replay/clientinviterewardrecorddetail/*` | `ClientInviteRewardRecordDetailController` | 5 (list/info/save/update/delete) |

---

## 十一、跨模块 Feign 接口

| Feign Interface | Method | 调用方 | 用途 |
|---|---|---|---|
| `ChannelFeign` | `getChannelParentNameByIds(List<Long>)` | `replay-power.UserBll`, `replay-order` | 批量查渠道+父渠道名称 |
| `InviteUrlCodeFeign` | `getUserInviteUrlCodeByActivityIdUserId(activityId, userId)` | -- | 按活动+用户查邀请码 |
| `InviteUrlCodeFeign` | `getUserInviteUrlCodeInfo()` | -- | 取当前用户邀请码（含锁+自动生成） |
| `InviteUrlCodeFeign` | `inviteCodeAndPromotionName(List<String>)` | `replay-reward` | 批量解出邀请码所属推广渠道名 |

实现：`replay-agent/api/ChannelApi.java`、`replay-agent/api/InviteUrlCodeApi.java`。

---

## API 路由表（汇总）

> 全部走全局登录态鉴权。除标注 `[agent]` 外均为 `replay-api` 模块。

| Path Prefix | 端点 | 数量 |
|---|---|---|
| `/replay/agent` | POST list, GET info, GET currentUserInfo, POST save, POST update, GET delete, POST updateAgentStatus | 7 |
| `/replay/channel` | POST list, GET info, POST save, POST update, GET delete, GET listTree | 6 |
| `/replay/agentcommission` | POST list, GET info, POST save, POST update, GET delete, GET commissionRecords | 6 |
| `/replay/agentpromotion` | GET listByAgentId, POST list, GET info, POST save, POST update, GET delete | 6 |
| `/replay/agentsale` | GET listByAgentId, POST list, GET info, POST save, POST update, GET delete | 6 |
| `/replay/agentsalepromotionchannel` | POST list, GET info, POST save, POST update, GET delete | 5 |
| `/replay/agentplatformsale` | GET listByAgentId, POST list, GET info, POST save, POST update, GET delete | 6 |
| `/replay/inviteurlcode` | POST list, GET infoByCode, GET info, POST save, POST update, GET delete | 6 |
| `/replay/agent/agentSale` | GET listByChannelId `[agent]` | 1 |
| `/replay/agent/inviteurlcode` | GET getUserInviteUrl `[agent]` | 1 |

> `[agent]` = `replay-agent` 模块自身 controller 暴露。

**注：** ClientInvite* 系列 Controller `@RestController` 被注释，未暴露端点；全模块无 `@NoRepeatSubmit`（仅 `getUserInviteUrl` 用 Redisson 锁防并发）；无 `@APIKey`，全部走全局登录态鉴权。
