---
name: "business-room"
description: "直播间管理模块核心业务规则：sec_uid跨租户唯一性、排班强管控引擎、平台枚举。开发 room 相关功能时调用。"
---

# Business Domain: LiveRoom (直播间管理模块)

This skill defines the business logic, external API integrations, and constraints for the LiveRoom module (`business.room`).

## 1. 直播间定位与业务流 (Positioning & Flow)

直播间是连接"主播"和"业绩"的实体载体。
- **关联组织**：直播间必须绑定到某一个组织架构节点（Company / Dept / Team）。直播间产生的初始场次业绩，默认向上汇总给该组织。
- **关联排班**：直播间是排班调度的核心坐标。

## 2. 核心业务规则 (Core Rules)

### 2.1 第三方标识绑定 (`sec_uid`)

- **防盗刷安全规则**：直播间创建时，需要输入主播的平台唯一标识 (`sec_uid`，如抖音号标识)。系统会通过开放平台 API 拉取昵称和头像。
- **跨租户唯一性**：同一个 `sec_uid` 在全系统（跨所有企业租户）原则上**只允许被一个有效租户绑定**。若强行绑定，需触发平台级授权认证流程，防止企业 A 恶意拉取企业 B 主播的数据。

### 2.2 排班强管控引擎 (Schedule Attributes)

直播间表或其扩展表中（`LiveRoomScheduleAttribute`），包含对其排班规则的严格约束。这些约束在进行"排班"操作时会被强制读取并校验：
- **营业时间约束**：例如设定 `start_plan=0800`, `end_plan=2400`（整型存储），则该直播间排班时需要符合这些基准时间约束。
- **班次时长与休息时长**：设定了允许的班次时长选项（`shift_options`）和休息时长选项（`rest_options`），如 [60, 120, 180] 分钟。
- **岗位约束**：设定该直播间允许的岗位列表（`position_options`）。前端渲染排班时，会以这个配置作为基础骨架。

## 3. 核心字段与字典 (Core Fields)

- `platform`: 平台类型枚举（0-抖音, 1-快手, 2-视频号）。不同的值决定了调用哪一套外部平台的 Open API。
- `sec_uid`: 第三方平台主播的绝对唯一标识（不可轻易修改）。
- `account_status`: 直播间状态（枚举）。

## 4. 对接与注意事项 (Integration Notes)

- 增删改直播间时，必须校验当前操作人是否对直播间拥有数据权限（使用 `@BeforePermission(type = OauthConstant.LIVE_ROOM, dataId = "#id")`）。
- **管理员绑定**：修改或创建直播间时，可以通过 `ManagerConnectorProcessor` 绑定直播间管理员。
- **级联删除清理**：删除/修改组织或解绑时，需要清理相关的本地缓存。

## 关联技能

- [support-name-map](../support-name-map/SKILL.md): 直播间对外展示名与关联信息常需批量补全（getLiveRoomNameMap 等）。
