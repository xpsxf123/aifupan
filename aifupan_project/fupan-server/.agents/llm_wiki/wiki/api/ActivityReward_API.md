# 活动奖励域 API 文档（标准版）

> 说明：所有接口统一返回结构为 `R<T>`：
> - `code`：int，0 表示成功
> - `msg`：string，提示信息
> - `data`：T，业务数据（可能为对象或分页结果 `PageUtils<TItem>`）

## 1. 邀请活动管理（后台）

### 1.1 邀请活动列表

- Method & Path：`POST /replay/activity/clientinviteactivity/list`
- 功能：分页查询邀请活动列表。
- 鉴权：后台管理用户（需具备活动配置查看权限）。

#### 请求体 `ClientInviteActivityListBo`

| 字段名    | 类型    | 必填 | 说明               |
|-----------|---------|------|--------------------|
| page      | int     | 否   | 当前页，默认 1     |
| limit     | int     | 否   | 每页记录数，默认10 |
| keyword   | string  | 否   | 模糊查询关键字（按活动名称等） |

示例：

```json
{
  "page": 1,
  "limit": 10,
  "keyword": "邀请有礼"
}
```

#### 响应体 `R<PageUtils<ClientInviteActivityListVo>>`

`PageUtils` 字段：

| 字段名     | 类型           | 说明         |
|------------|----------------|--------------|
| totalCount | int            | 总记录数     |
| pageSize   | int            | 每页记录数   |
| totalPage  | int            | 总页数       |
| currPage   | int            | 当前页       |
| list       | array\<ClientInviteActivityListVo\> | 列表数据 |

`ClientInviteActivityListVo` 继承自 `ClientInviteActivityVo`：

| 字段名            | 类型    | 说明                           |
|-------------------|---------|--------------------------------|
| id                | long    | 活动 ID                        |
| agentId           | long    | 代理商 ID                      |
| activityName      | string  | 邀请活动名称                   |
| activityStartTime | string  | 活动开始时间（日期时间）       |
| activityEndTime   | string  | 活动结束时间（日期时间）       |
| activityStatus    | int     | 活动状态：0 未启用，1 启用中   |
| createDate        | string  | 创建时间                       |
| updateDate        | string  | 最后修改时间                   |
| isDeleted         | int     | 是否删除标记                   |

---

### 1.2 邀请活动详情

- Method & Path：`GET /replay/activity/clientinviteactivity/info`
- 功能：根据活动 ID 获取邀请活动详情（含进度与奖励）。
- 鉴权：后台管理用户。

#### 请求参数（Query）

| 参数名 | 类型 | 必填 | 说明   |
|--------|------|------|--------|
| id     | long | 是   | 活动ID |

#### 响应体 `R<ClientInviteActivityInfoVo>`

`ClientInviteActivityInfoVo`：

| 字段名        | 类型                                          | 说明                         |
|---------------|-----------------------------------------------|------------------------------|
| （继承）      | `ClientInviteActivityVo` 同上                 | 活动基础信息                 |
| progressList  | array\<ClientInviteProgressInfoVo\>          | 活动的进度与奖励配置列表     |

`ClientInviteProgressInfoVo` 继承自 `ClientInviteProgressVo`：

| 字段名             | 类型                                      | 说明                                     |
|--------------------|-------------------------------------------|------------------------------------------|
| id                 | long                                      | 进度 ID                                  |
| inviteActivityId   | long                                      | 关联活动 ID                              |
| inviteProgressType | int                                       | 进度类型：0 被邀请人进度，1 邀请人进度   |
| inviteProgressCode | string                                    | 进度 code（如 REGISTER/DOWNLOAD 等）     |
| inviteProgressValue| int                                       | 进度值（如邀请人数、使用次数）           |
| inviteProgressTitle| string                                    | 进度标题                                 |
| inviteProgressRequire | string                                 | 进度要求描述                             |
| inviteProgressStatus | int                                     | 状态：0 停用，1 启用中                   |
| createDate         | string                                    | 创建时间                                 |
| updateDate         | string                                    | 修改时间                                 |
| isDeleted          | int                                       | 删除标记                                 |
| rewardList         | array\<ClientInviteProgressRewardInfoVo\> | 该进度下配置的奖励列表                   |

`ClientInviteProgressRewardInfoVo` 继承自 `ClientInviteProgressRewardVo`：

| 字段名              | 类型    | 说明                                                         |
|---------------------|---------|--------------------------------------------------------------|
| id                  | long    | 进度奖励 ID                                                  |
| progressId          | long    | 所属进度 ID                                                  |
| rewardType          | int     | 奖励类型：0 版本，1 增量包                                   |
| packageId           | long    | 版本 ID（rewardType 为 0 时有值）                            |
| packagePriceId      | long    | 版本价格 ID（rewardType 为 0 时有值）                        |
| commodityTypeId     | long    | 商品类型 ID                                                  |
| commodityNumber     | long    | 商品数量/次数/时长（语义由 commodityType 决定）             |
| validityNum         | int     | 有效期数值，如 3                                             |
| validityUnit        | int     | 有效期单位：0 小时，1 天，2 月，3 季度，4 半年，5 年        |
| commodityTypeName   | string  | 增量包类型名称（rewardType 为 1 时）                         |
| commodityTypeUnit   | string  | 增量包展示单位（rewardType 为 1 时，如“条”“次”）           |
| commodityTypeValidity | string| 增量包有效期展示文案（rewardType 为 1 时）                   |
| packageName         | string  | 版本名称（rewardType 为 0 时）                               |
| packagePrice        | int     | 版本价格（分）（rewardType 为 0 时）                         |
| packageDuration     | string  | 版本时长展示文案（rewardType 为 0 时）                       |

---

### 1.3 新增邀请活动

- Method & Path：`POST /replay/activity/clientinviteactivity/save`
- 功能：创建邀请活动及其进度/奖励配置。
- 鉴权：后台管理用户。

#### 请求体 `ClientInviteActivityBo`

| 字段名                    | 类型                                      | 必填 | 说明                                             |
|---------------------------|-------------------------------------------|------|--------------------------------------------------|
| id                        | long                                      | 否   | 活动 ID（新增可为空）                            |
| agentId                   | long                                      | 否   | 关联代理商 ID（若活动针对某代理商）              |
| activityName              | string                                    | 是   | 活动名称                                         |
| activityStartTime         | string                                    | 是   | 活动开始时间（日期时间）                         |
| activityEndTime           | string                                    | 是   | 活动结束时间                                     |
| activityStatus            | int                                       | 是   | 活动状态：0 未启用，1 启用中                     |
| clientInviteProgressBoList| array\<ClientInviteProgressBo\>           | 是   | 进度与奖励配置列表                               |

`ClientInviteProgressBo`：

| 字段名               | 类型                                      | 必填 | 说明                                       |
|----------------------|-------------------------------------------|------|--------------------------------------------|
| id                   | long                                      | 否   | 进度 ID（新增可为空）                      |
| inviteActivityId     | long                                      | 否   | 关联活动 ID（新增可由服务端填充）         |
| inviteProgressType   | int                                       | 是   | 进度类型：0 被邀请人，1 邀请人             |
| inviteProgressCode   | string                                    | 是   | 进度 code（与奖励规则匹配）               |
| inviteProgressValue  | int                                       | 是   | 进度阈值（如邀请人数/使用次数）           |
| inviteProgressTitle  | string                                    | 是   | 标题                                       |
| inviteProgressRequire| string                                    | 否   | 要求描述                                   |
| inviteProgressStatus | int                                       | 是   | 状态：0 停用，1 启用中                     |
| rewardList           | array\<ClientInviteProgressRewardBo\>     | 是   | 对应的奖励配置                             |

`ClientInviteProgressRewardBo`：

| 字段名         | 类型   | 必填 | 说明                                                     |
|----------------|--------|------|----------------------------------------------------------|
| id             | long   | 否   | 奖励 ID（新增可为空）                                   |
| progressId     | long   | 否   | 进度 ID（新增可由服务端填充）                           |
| rewardType     | int    | 是   | 奖励类型：0 版本，1 增量包                               |
| packageId      | long   | 否   | 版本 ID（rewardType 为 0 时必填）                       |
| packagePriceId | long   | 否   | 版本价格 ID（rewardType 为 0 时必填）                   |
| commodityTypeId| long   | 否   | 商品类型 ID（rewardType 为 1 时通常必填）               |
| commodityNumber| long   | 是   | 数量/额度                                                |
| validityNum    | int    | 否   | 有效期数值                                              |
| validityUnit   | int    | 否   | 有效期单位：0 小时，1 天，2 月，3 季度，4 半年，5 年    |

#### 响应体 `R<String>`

- data：string，通常为“添加成功”等文案。

---

### 1.4 修改邀请活动

- Method & Path：`POST /replay/activity/clientinviteactivity/update`
- 功能：更新活动及其进度/奖励配置。
- 鉴权：后台管理用户。
- 请求体/响应体：与 1.3 相同（id 必填）。

---

### 1.5 删除邀请活动

- Method & Path：`GET /replay/activity/clientinviteactivity/delete`
- 功能：按 ID 删除邀请活动。
- 鉴权：后台管理用户。

#### 请求参数（Query）

| 参数名 | 类型 | 必填 | 说明   |
|--------|------|------|--------|
| id     | long | 是   | 活动ID |

#### 响应体

- `R<String>`，data 通常为“删除成功”。

---

## 2. 邀请活动（客户端）

### 2.1 获取当前可用邀请活动

- Method & Path：`GET /replay/activity/clientinviteactivity/infoByClient`
- 功能：客户端获取当前有效邀请活动详情（通常按默认活动或激活活动返回）。
- 鉴权：登录用户。

#### 请求参数

- 无。

#### 响应体 `R<ClientInviteActivityInfoVo>`

- 结构同 1.2。

---

## 3. 邀请奖励记录（后台）

### 3.1 后台奖励列表

- Method & Path：`POST /replay/reward/clientinviterewardrecord/listByBack`
- 功能：分页查询邀请奖励记录（支持按进度、状态、用户、时间筛选）。
- 鉴权：后台管理用户。

#### 请求体 `ClientInviteRewardRecordListBo`

继承自 `PageBo`：

| 字段名       | 类型        | 必填 | 说明                                 |
|--------------|-------------|------|--------------------------------------|
| page         | int         | 否   | 当前页，默认 1                       |
| limit        | int         | 否   | 每页记录数，默认 10                  |
| progressCode | string      | 否   | 进度 code（如 REGISTER/DOWNLOAD 等） |
| rewardStatus | string      | 否   | 奖励状态：0 待发放，1 已发放         |
| inviterName  | string      | 否   | 邀请人名称                            |
| inviteeName  | string      | 否   | 被邀请人名称                          |
| inviterUserIds | array<long> | 否 | 邀请人用户 ID 集合                    |
| inviteeUserIds | array<long> | 否 | 被邀请人用户 ID 集合                  |
| startTime    | string      | 否   | 奖励时间-开始（yyyy-MM-dd）          |
| endTime      | string      | 否   | 奖励时间-结束（yyyy-MM-dd）          |

#### 响应体 `R<PageUtils<ClientInviteRewardRecordInfoVo>>`

`ClientInviteRewardRecordInfoVo` 继承自 `ClientInviteRewardRecordVo`：

基础字段 `ClientInviteRewardRecordVo`：

| 字段名           | 类型   | 说明                                        |
|------------------|--------|---------------------------------------------|
| id               | long   | 奖励记录 ID                                 |
| activityId       | long   | 活动 ID                                     |
| progressId       | long   | 进度 ID                                     |
| progressRewardId | long   | 进度奖励 ID                                 |
| rewardTargetType | int    | 奖励对象类型：0 邀请人，1 被邀请人         |
| rewardUserId     | long   | 奖励给哪个用户                              |
| rewardSourceUserId| long  | 奖励来源用户（通常为邀请人）               |
| rewardTenantId   | long   | 租户限制 ID                                 |
| progressCode     | string | 进度 code（与 InviteRewardRuleCode 对应）  |
| rewardStatus     | long   | 奖励状态：0 待发放，1 已发放               |
| sendDate         | string | 奖励发放时间                                |
| createDate       | string | 创建时间                                    |
| updateDate       | string | 修改时间                                    |
| isDeleted        | int    | 删除标记                                    |

扩展字段 `ClientInviteRewardRecordInfoVo`：

| 字段名           | 类型          | 说明                               |
|------------------|---------------|------------------------------------|
| inviterName      | string        | 邀请人名称                         |
| inviteeName      | string        | 被邀请人名称                       |
| reward           | string        | 奖励描述文案                       |
| progressRewardIds| array<long>   | 关联的进度奖励 ID 集合             |
| rewardList       | array<string> | 奖励集合（多条文案）               |
| progressCodeStr  | string        | 进度 code 的展示 Label             |

---

## 4. 邀请奖励（客户端）

### 4.1 奖励汇总

- Method & Path：`GET /replay/reward/clientinviterewardrecord/clientGetRewardSummary`
- 功能：客户端获取当前用户邀请奖励汇总（按奖励维度合并）。
- 鉴权：登录用户。

#### 请求参数

- 无。

#### 响应体 `R<List<RewardSummaryVo>>`

`RewardSummaryVo`：

| 字段名      | 类型   | 说明                     |
|-------------|--------|--------------------------|
| rewardLabel | string | 奖励 label（如“AI Token”） |
| rewardNum   | long   | 奖励数量                 |
| rewardUnit  | string | 奖励单位（如“次”“条”）   |

---

### 4.2 邀请奖励列表

- Method & Path：`POST /replay/reward/clientinviterewardrecord/clientGetUserRewardList`
- 功能：分页获取当前用户的邀请奖励记录。
- 鉴权：登录用户。

#### 请求体 `UserRewardBo`

继承 `PageBo`，无额外字段：

| 字段名 | 类型 | 必填 | 说明     |
|--------|------|------|----------|
| page   | int  | 否   | 当前页   |
| limit  | int  | 否   | 每页条数 |

#### 响应体 `R<PageUtils<ClientUserRewardRecordVo>>`

`ClientUserRewardRecordVo`：

| 字段名          | 类型                               | 说明                   |
|-----------------|------------------------------------|------------------------|
| userNickName    | string                             | 用户名                 |
| phone           | string                             | 手机号                 |
| progress        | string                             | 使用类型（对应进度场景） |
| rewardDetailList| array<ClientUserRewardRecordDetailVo> | 奖励详情列表       |
| rewardDate      | string                             | 奖励发放时间           |

`ClientUserRewardRecordDetailVo`：

| 字段名      | 类型   | 说明             |
|-------------|--------|------------------|
| rewardType  | string | 奖励类型展示文案 |
| rewardUnit  | string | 奖励单位         |
| rewardNumber| long   | 奖励数量         |

