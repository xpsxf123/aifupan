# 代理分销域 API 文档（标准版）

> 说明：所有接口统一返回结构为 `R<T>`：
> - `code`：int，0 表示成功
> - `msg`：string，提示信息
> - `data`：T，业务数据

## 1. 代理商销售（内部接口）

### 1.1 通过渠道ID查询代理商销售列表

- Method & Path：`GET /replay/agent/agentSale/listByChannelId`
- 功能：根据渠道ID查询该渠道归属代理商的销售人员列表。
- 鉴权：内部/登录用户（接口未显式标注匿名；通常由全局鉴权统一控制）。

#### 请求参数（Query）

| 参数名    | 类型   | 必填 | 说明   |
|-----------|--------|------|--------|
| channelId | string | 是   | 渠道ID |

示例：

```
GET /replay/agent/agentSale/listByChannelId?channelId=123
```

#### 响应体 `R<List<AgentSaleInfoVo>>`

`AgentSaleInfoVo` 继承自 `AgentSaleVo`：

`AgentSaleVo`：

| 字段名       | 类型   | 说明                   |
|--------------|--------|------------------------|
| id           | long   | 代理商销售ID           |
| agentId      | long   | 代理商ID               |
| saleName     | string | 销售人员名称           |
| salePhone    | string | 销售人员手机号         |
| saleStatus   | int    | 状态：0 未启用，1 启用中 |
| createUserId | long   | 创建人用户ID           |
| createDate   | string | 创建时间               |
| updateDate   | string | 最后修改时间           |
| isDeleted    | int    | 删除标记               |

`AgentSaleInfoVo` 扩展字段：

| 字段名            | 类型                         | 说明               |
|-------------------|------------------------------|--------------------|
| agentPromotionStr | string                       | 推广渠道（展示字符串） |
| agentPromotionList| array\<AgentPromotionInfoVo\> | 推广渠道信息列表   |

`AgentPromotionInfoVo` 继承自 `AgentPromotionVo`：

`AgentPromotionVo`：

| 字段名             | 类型              | 说明                         |
|--------------------|-------------------|------------------------------|
| id                 | long              | 推广渠道ID                   |
| agentId            | long              | 代理商ID                     |
| promotionName      | string            | 推广渠道名称                 |
| posterImgIds       | string            | 海报图片文件id（多个用 _ 分隔） |
| posterImgList      | array\<FileShowVo\> | 海报图片信息列表             |
| btnBgColor         | string            | 按钮颜色                     |
| btContent          | string            | 按钮文案                     |
| commissionRate     | double            | 新签佣金比例                 |
| renewalCommissionRate | double         | 续费佣金比例                 |
| promotionUrlCode   | string            | 渠道URL链接 code             |
| promotionStatus    | int               | 状态：0 未启用，1 启用中     |
| createDate         | string            | 创建时间                     |
| updateDate         | string            | 最后修改时间                 |
| isDeleted          | int               | 删除标记                     |

`AgentPromotionInfoVo` 扩展字段：

| 字段名 | 类型   | 说明          |
|--------|--------|---------------|
| url    | string | 渠道URL链接   |

---

## 2. 邀请链接（内部接口）

### 2.1 获取当前用户邀请链接

- Method & Path：`GET /replay/agent/inviteurlcode/getUserInviteUrl`
- 功能：生成/获取当前登录用户的邀请链接（返回完整 URL 字符串）。
- 鉴权：登录用户。

#### 请求参数

- 无。

#### 响应体 `R<String>`

| 字段名 | 类型   | 说明               |
|--------|--------|--------------------|
| data   | string | 邀请链接完整URL    |

