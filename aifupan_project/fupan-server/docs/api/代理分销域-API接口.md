# API接口文档 - 代理分销域

> 基础路径: /replay

## 1. AgentController - 代理商管理
**路径前缀**: `replay/agent`

| HTTP | 路径 | 说明 | 参数 | 返回 |
|------|------|------|------|------|
| POST | /list | 代理商列表 | AgentListBo | PageUtils\<AgentListVo\> |
| GET | /info | 代理商信息 | id(Long) | AgentInfoVo |
| GET | /currentUserInfo | 当前用户的代理商信息 | - | AgentInfoVo |
| POST | /save | 新增代理商 | AgentBo | String |
| POST | /update | 修改代理商 | AgentBo | String |
| GET | /delete | 删除代理商 | id(Long) | String |
| POST | /updateAgentStatus | 修改代理商状态 | AgentBo | String |

## 2. AgentCommissionController - 代理商佣金
**路径前缀**: `replay/agentcommission`

| HTTP | 路径 | 说明 | 参数 | 返回 |
|------|------|------|------|------|
| POST | /list | 佣金列表 | AgentCommissionListBo | PageUtils\<AgentCommissionListVo\> |
| GET | /info | 佣金信息 | id(Long) | AgentCommissionInfoVo |
| POST | /save | 新增佣金 | AgentCommissionBo | String |
| POST | /update | 修改佣金 | AgentCommissionBo | String |
| GET | /delete | 删除佣金 | id(Long) | String |
| GET | /commissionRecords | 佣金结算记录 | agentId(Long) | List\<CommissionRecordsVo\> |

## 3. ChannelController - 渠道管理
**路径前缀**: `replay/channel`

| HTTP | 路径 | 说明 | 参数 | 返回 |
|------|------|------|------|------|
| POST | /list | 渠道列表 | ChannelListBo | PageUtils\<ChannelListVo\> |
| GET | /info | 渠道信息 | id(Long) | ChannelInfoVo |
| POST | /save | 新增渠道 | ChannelBo | String |
| POST | /update | 修改渠道 | ChannelBo | String |
| GET | /delete | 删除渠道 | id(Long) | String |
| GET | /listTree | 渠道树形结构 | childrenNotNull(Integer) | List\<ChannelTreeVo\> |

## 4. ApiAgentSaleController - 代理商销售
**路径前缀**: `replay/agentsale`

| HTTP | 路径 | 说明 | 参数 | 返回 |
|------|------|------|------|------|
| GET | /listByAgentId | 代理商的销售列表 | agentId(Long) | List\<AgentSaleInfoVo\> |
| POST | /list | 销售列表 | AgentSaleListBo | PageUtils\<AgentSaleListVo\> |
| GET | /info | 销售信息 | id(Long) | AgentSaleInfoVo |
| POST | /save | 新增销售 | AgentSaleBo | String |
| POST | /update | 修改销售 | AgentSaleBo | String |
| GET | /delete | 删除销售 | id(Long) | String |

## 5. AgentPlatformSaleController - 平台销售
**路径前缀**: `replay/agentplatformsale`

| HTTP | 路径 | 说明 | 参数 | 返回 |
|------|------|------|------|------|
| GET | /listByAgentId | 代理商的平台销售列表 | agentId(Long),salesType(Integer) | List\<AgentPlatformSaleInfoVo\> |
| POST | /list | 平台销售列表 | AgentPlatformSaleListBo | PageUtils\<AgentPlatformSaleListVo\> |
| GET | /info | 平台销售信息 | id(Long) | AgentPlatformSaleInfoVo |
| POST | /save | 新增平台销售 | AgentPlatformSaleBo | String |
| POST | /update | 修改平台销售 | AgentPlatformSaleBo | String |
| GET | /delete | 删除平台销售 | id(Long) | String |

## 6. AgentPromotionController - 推广渠道
**路径前缀**: `replay/agentpromotion`

| HTTP | 路径 | 说明 | 参数 | 返回 |
|------|------|------|------|------|
| GET | /listByAgentId | 代理商的推广渠道列表 | agentId(Long) | List\<AgentPromotionInfoVo\> |
| POST | /list | 推广渠道列表 | AgentPromotionListBo | PageUtils\<AgentPromotionListVo\> |
| GET | /info | 推广渠道信息 | id(Long) | AgentPromotionInfoVo |
| POST | /save | 新增推广渠道 | AgentPromotionBo | String |
| POST | /update | 修改推广渠道 | AgentPromotionBo | String |
| GET | /delete | 删除推广渠道 | id(Long) | String |

## 7. AgentSalePromotionChannelController - 销售渠道关联
**路径前缀**: `replay/agentsalepromotionchannel`

| HTTP | 路径 | 说明 | 参数 | 返回 |
|------|------|------|------|------|
| POST | /list | 关联列表 | AgentSalePromotionChannelListBo | PageUtils\<...ListVo\> |
| GET | /info | 关联信息 | id(Long) | AgentSalePromotionChannelInfoVo |
| POST | /save | 新增关联 | AgentSalePromotionChannelBo | String |
| POST | /update | 修改关联 | AgentSalePromotionChannelBo | String |
| GET | /delete | 删除关联 | id(Long) | String |

## 8. InviteUrlCodeController - 邀请链接
**路径前缀**: `replay/inviteurlcode`

| HTTP | 路径 | 说明 | 参数 | 返回 |
|------|------|------|------|------|
| POST | /list | 邀请链接列表 | InviteUrlCodeListBo | PageUtils\<InviteUrlCodeListVo\> |
| GET | /infoByCode | 根据code查信息 | code(String) | InviteUrlCodeInfoVo |
| GET | /info | 邀请链接信息 | id(Long) | InviteUrlCodeInfoVo |
| POST | /save | 新增邀请链接 | InviteUrlCodeBo | String |
| POST | /update | 修改邀请链接 | InviteUrlCodeBo | String |
| GET | /delete | 删除邀请链接 | id(Long) | String |

## 9. AgentSaleController (replay-agent模块内部)
**路径前缀**: `replay/agent/agentSale`

| HTTP | 路径 | 说明 | 参数 | 返回 |
|------|------|------|------|------|
| GET | /listByChannelId | 通过渠道ID查销售列表 | channelId(String) | List\<AgentSaleInfoVo\> |

## 10. InviteUrlController (replay-agent模块内部)
**路径前缀**: `replay/agent/inviteurlcode`

| HTTP | 路径 | 说明 | 参数 | 返回 |
|------|------|------|------|------|
| GET | /getUserInviteUrl | 获取当前用户邀请链接 | - | String |
