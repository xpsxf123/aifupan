# API接口文档 - 活动与奖励域

> 基础路径: /replay

## 1. ClientInviteActivityController (replay-activity)
**路径前缀**: `replay/activity/clientinviteactivity`

| HTTP | 路径 | 说明 | 参数 | 返回 |
|------|------|------|------|------|
| POST | /list | 邀请活动列表 | ClientInviteActivityListBo | PageUtils\<ClientInviteActivityListVo\> |
| GET | /info | 邀请活动信息 | id(Long) | ClientInviteActivityInfoVo |
| POST | /save | 新增邀请活动 | ClientInviteActivityBo | String |
| POST | /update | 修改邀请活动 | ClientInviteActivityBo | String |
| GET | /delete | 删除邀请活动 | id(Long) | String |
| GET | /infoByClient | 客户端获取邀请活动信息 | - | ClientInviteActivityInfoVo |

## 2. ClientInviteRewardRecordController (replay-reward)
**路径前缀**: `replay/reward/clientinviterewardrecord`

| HTTP | 路径 | 说明 | 参数 | 返回 |
|------|------|------|------|------|
| POST | /listByBack | 后台获取奖励列表 | ClientInviteRewardRecordListBo | PageUtils\<ClientInviteRewardRecordInfoVo\> |
| GET | /clientGetRewardSummary | 客户端获取奖励汇总 | - | List\<RewardSummaryVo\> |
| POST | /clientGetUserRewardList | 客户端获取邀请奖励列表 | UserRewardBo | PageUtils\<ClientUserRewardRecordVo\> |

## 已禁用的Controller（@RestController被注释）

以下Controller在replay-api模块中定义但已被注释禁用，功能已迁移到各业务模块：

- `replay/clientinviteactivity` - 邀请活动（已迁移到replay-activity）
- `replay/clientinviteprogress` - 邀请进度
- `replay/clientinviteprogressreward` - 邀请进度奖励
- `replay/clientinviterewardrecord` - 邀请奖励记录（已迁移到replay-reward）
- `replay/clientinviterewardrecorddetail` - 邀请奖励明细记录
