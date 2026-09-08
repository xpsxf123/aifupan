# [API] Route Index — Full API Surface

> Date: 2026-05-25
> Source: distilling from `.agents/llm_wiki/wiki/api/wal/applied/20260429_controller_docs_api_append.md` + `20260430_docs_performance_api_append.md`

## Route Table

| API (Method + Path) | Summary | Controller | Permissions |
|---|---|---|---|
| GET /api/governance/check/* | 系统检测与租户检测 | SystemCheckController | — |
| GET /api/governance/system-common/* | 系统公共：导出文件下载 | SystemCommonController | — |
| POST /api/governance/oss/* | OSS 预签名上传 | OssController | — |
| POST /api/governance/org/* | 组织架构树 | OrgController | — |
| POST /api/governance/sub-company/* | 子公司管理 | SubCompanyController | — |
| POST /api/governance/dept/* | 部门管理 | DeptController | `org:dept:manage:*` |
| POST /api/governance/team/* | 小组管理 | TeamController | — |
| POST /api/governance/position/* | 岗位管理与默认岗位信息 | PositionController | — |
| POST /api/governance/oauth/* | 企业端登录 | OauthController | — |
| POST /api/governance/employee-profile/* | 员工个人档案 | EmployeeProfileController | — |
| POST /api/governance/employee/* | 人员管理 | EmployeeController | `sys:employee:manage:*` |
| POST /api/governance/role/* | 角色管理 | RoleController | — |
| POST /api/governance/user-menu/* | 用户菜单树 | UserMenuController | — |
| POST /api/governance/menu/* | 平台端菜单管理 | MenuController | — |
| POST /api/governance/tenant/* | 平台端租户相关 | TenantController | — |
| POST /api/governance/employee-open/* | 平台端人员开放接口 | EmployeeOpenController | — |
| POST /api/governance/live-room/* | 企业端直播间管理 | LiveRoomAdminController | `room:mgmt:*` |
| GET /api/governance/client/live-room/* | 客户端直播间相关 | ClientLiveRoomController | — |
| POST /api/governance/room-schedule/* | 企业端直播间排班管理 | RoomScheduleController | `room:schedule:*` |
| POST /api/governance/employee-schedule/* | 企业端个人排班 | EmployeeScheduleController | — |
| POST /api/governance/trade/* | 行业树 | TradeController | — |
| POST /api/governance/performance/summary/* | 业绩汇总 | PerformanceSummaryController | — |
| POST /api/governance/performance/live-room/* | 直播间/排班业绩 | LiveRoomPerformanceController | — |
| POST /api/governance/performance/employee/* | 人员业绩 | EmployeePerformanceController | — |
| POST /api/governance/performance/product/* | 商品业绩 | ProductPerformanceController | — |
| POST /api/governance/performance/video/clientPushVideo | 客户端推送直播视频/业绩数据（批次维度） | ClientVideoController | — |
| POST /api/governance/performance/video/querySchedulePerformance | 客户端按 secUid + 时间范围查询排班业绩 | ClientVideoController | — |
| POST /api/governance/performance/product/ranking | 商品排行分页查询（商品维度聚合） | ProductPerformanceController | — |
| POST /api/governance/performance/product/sessions | 商品关联场次分页查询 | ProductPerformanceController | — |
| POST /api/governance/performance/product/companies | 商品关联分公司聚合查询 | ProductPerformanceController | — |

## Permission Code Patterns

- **org module**: `org:<resource>:manage:<action>` (e.g. `org:dept:manage:add`)
- **room module**: `room:mgmt:*` and `room:schedule:*` (two prefixes)
- **sys module**: `sys:employee:manage:*`
