<!-- module: power -->
<!-- area: domain -->
<!-- generated-by: reverse-scan -->
<!-- last-scan: 2026-05-20 -->
<!-- source-paths: replay-power/, replay-api/.../controller/power/ -->

# Power Domain — 业务概念与词汇表

> replay-power 模块核心业务概念定义。承载用户、租户、角色、菜单（RBAC）、销售统计等基础能力。Agent 在 Explorer/Propose 阶段必须使用此术语表，避免领域漂移。

---

## 核心概念

| 概念 | 定义 | 关联概念 | 状态机 |
|------|------|----------|--------|
| **User (用户)** | 平台主账户实体，含登录账号、手机号、密码、微信 openid 等基础信息；区分客户端用户 / 后台管理员 / 子账号 | [[UserDetails]], [[Tenant]], [[Role]] | `status`: 0未冻结/1已冻结 |
| **UserDetails (用户详情)** | 用户扩展信息：行业、公司、销售归属、渠道、意向、客户类型等；CRM 跟进对象的扩展面 | [[User]], [[Company]], [[Sales]] | `isLoggedIn`: 0未/1已登录过 |
| **Tenant (租户)** | 数据隔离单位；一名用户可拥有/归属多个租户，当前激活租户由 `User.activeTenantId` 标识 | [[User]], [[TenantUser]] | — |
| **TenantUser (租户-用户关联)** | 租户与用户的多对多关联；用于跨账号共享租户内数据 | [[Tenant]], [[User]] | — |
| **Role (角色)** | RBAC 角色定义，含名称和等级（如 A 超管 / E 普通） | [[Menu]], [[User]] | — |
| **Menu (菜单)** | 后台管理 UI 菜单/功能/目录树节点，支持图标、URL、排序、父子层级 | [[Role]] | `type`: 0菜单/1功能/2目录 |
| **UserRole (用户-角色关联)** | 用户与角色的多对多关联 | [[User]], [[Role]] | — |
| **MenuRole (角色-菜单关联)** | 角色与菜单的多对多关联，构成 RBAC 权限矩阵 | [[Role]], [[Menu]] | — |
| **UserToken (用户 Token)** | 登录 token 持久化（Redis + DB 双存）；包含 token、userInfo JSON、过期时间、最后访问时间 | [[User]], [[UserLoginInfo]] | — |
| **UserLoginInfo (用户登录信息)** | 单次登录会话的来源 / 指纹 / 最后请求时间，按 userId+token 维度记录 | [[UserToken]], [[UserDeviceFingerprint]] | — |
| **UserDeviceFingerprint (设备指纹)** | 客户端设备唯一标识，按 fingerprint+deviceType 去重 | [[UserLoginInfo]] | `deviceType`: 0桌面/1Web |
| **UserLoginLog (登录日志)** | 用户登录/离线/上线/退出操作的审计日志 | [[User]] | `operaType`: 0登录/1离线/2上线/3退出, `operaStatus`: 0成功/1失败 |
| **BindingAccount (父子绑定)** | 父账号与子账号的绑定/解绑历史记录，含绑定状态、时间、解绑原因 | [[User]] | `bindingStatus`: 0绑定/1解绑 |
| **Sales (销售)** | 跟进客户的销售人员；支持轮询分配、员工状态、平台/代理商销售类型 | [[User]], [[UserDetails]] | `salesType`: 0平台/1代理商; `employeeStatus`: 0离职/1在职 |
| **Tag (标签)** | 用户分类标签 | [[UserTag]] | — |
| **UserTag (用户-标签关联)** | 用户与标签的多对多关联 | [[User]], [[Tag]] | — |
| **UserRemark (用户备注 / 跟进记录)** | 客户跟进记录明细，含跟进类型、状态、内容、下次跟进时间 | [[UserDetails]], [[Sales]] | `followStatus`, `followType` |
| **UserBusiness (用户业务快照)** | 当前用户跟进状态快照（每用户一条），随最新 [[UserRemark]] 刷新；冗余 `accordingStatus`、`nextFolTime`、`clientVersion` | [[UserRemark]], [[User]] | `accordingStatus` |
| **Company (公司)** | 用户所属公司信息：名称、注册号、行业、规模、联系人等 | [[UserDetails]] | `status`: 0正常/1暂停营业 |
| **Dept (部门)** | 销售团队的部门树形结构 | [[DeptUser]], [[Sales]] | — |
| **DeptUser (部门-用户关联)** | 部门与销售/管理员用户的归属关联 | [[Dept]], [[User]], [[Sales]] | — |
| **CrmAiProfile (CRM AI 画像)** | 销售智能体生成的客户完整画像 JSON，按 `profileId` 幂等覆盖 | [[User]] | — |
| **CrmStageEvent (CRM 阶段事件)** | 销售智能体回传的客户阶段判断 / 关键事实 / 证据 / 摘要，按 `eventId` 幂等 | [[User]], [[CrmAiProfile]] | — |
| **DashboardStatistics (看板统计)** | 仪表盘数据概览：注册数、试用数、续费到期、成交客户数等 | [[User]], [[Sales]] | — |
| **SalesStatistics (销售/团队看板)** | 销售/团队维度的统计聚合：客户意愿度分布、3 日内待跟进、15 日内续费等 | [[Sales]], [[User]], [[UserBusiness]] | — |

---

## 状态机定义

### User.status — 用户冻结状态
| 值 | 状态 | 说明 |
|----|------|------|
| 0 | UNFROZEN | 未冻结 |
| 1 | FROZEN | 已冻结，禁止登录 |

冻结副作用：
- 登录被拒（"账号已被冻结，请联系管理员"）
- 已颁发 token 全部失效（Redis + DB）
- 若用户为代理商，子账号一并冻结

### User.userType — 用户类型 (`UserEnums.userType`)
| 值 | 枚举 | 说明 |
|----|------|------|
| 0 | CLIENT_USER | 爱复盘普通用户（客户端主账号） |
| 1 | MANAGE_ADMIN_USER | 后台管理员 |
| 2 | CLIENT_CHILD_USER | 爱复盘子账号 |

### User.adminUserType — 后台管理员子类型 (`UserEnums.adminUserType`)
仅 `userType=1` 时生效。

| 值 | 枚举 | 说明 |
|----|------|------|
| 0 | ADMIN | 正常后台用户 |
| 1 | AGENT | 代理商 |
| 2 | AGENT_SALE | 代理商销售 |

### Sales.salesType — 销售类型 (`UserEnums.salesType`)
| 值 | 枚举 | 说明 |
|----|------|------|
| 0 | ADMIN | 平台销售 |
| 1 | AGENT | 代理商销售 |

### Sales.employeeStatus / UserDetails.employeeStatus — 员工状态
| 值 | 状态 | 说明 |
|----|------|------|
| 0 | RESIGNED | 离职 |
| 1 | ON_JOB | 在职 |

### Menu.type — 菜单类型
| 值 | 类型 | 说明 |
|----|------|------|
| 0 | MENU | 菜单 |
| 1 | FUNCTION | 功能 |
| 2 | DIRECTORY | 目录 |

### UserLoginLog.operaType — 登录操作类型
| 值 | 类型 | 说明 |
|----|------|------|
| 0 | LOGIN | 登录 |
| 1 | OFFLINE | 离线 |
| 2 | ONLINE | 上线 |
| 3 | LOGOUT | 退出 |

### LoginSourceEnum — 登录来源
| 值 | label | 说明 |
|----|-------|------|
| `client` | 客户端 | 桌面/移动端 |
| `web` | Web 端 | 浏览器登录 |
| `back` | 后台 | 管理端 |

### BindingAccount.bindingStatus — 父子绑定状态
| 值 | 状态 |
|----|------|
| 0 | 绑定中 |
| 1 | 已解绑 |

### UserDeviceFingerprint.deviceType
| 值 | 类型 |
|----|------|
| 0 | desktop（桌面客户端） |
| 1 | web（Web 端） |

### CodeMsgEnum — 业务消息码（`UserEnums.CodeMsgEnum` + 模块内 `Constant.CodeMsgEnum`）
| Code | 含义 |
|------|------|
| 4001 | NO_LOGIN — 没有登录 |
| 4002 | NO_POWER — 没有权限 |
| 4003 | TIP_CUSTOM — 自定义提示（业务异常） |
| 4004 | IS_REGISTER — 自定义提示（已注册） |

---

## 核心工作流

### 1. 注册流程
```
获取手机验证码（短信，Redis key=power.phoneCodeRedisKey+phone）
  → 校验验证码
  → 创建 tb_user（雪花 ID, MD5 密码）
  → 默认分配角色（power.defaultRoleId）
  → 创建 tb_user_details（关联渠道/销售/代理商）
  → 邀请码处理（如有 inviteUrlCode）
  → 返回 RegisterVo
```

### 2. 登录流程
```
LoginBo(username|phone + password|code, userType)
  → 账号密码 / 手机号验证码二选一校验
  → status==1 拒绝（已冻结）
  → 删除验证码缓存
  → 检查重复登录（"账号已登录，请退出后30秒再试"）
  → 子账号将激活租户继承父账号
  → fillUserLoginVo: 角色/菜单/AgentId 装配
  → 生成 UUID token，写入 Redis（key=power.userLoginTokenRedisKey+token, TTL=30d）
  → 落库 tb_user_token + tb_user_login_info
  → 落审计 tb_user_login_log
  → 更新 isLoggedIn=1
  → 发送邀请活动 MQ 消息（rocketMqProperties.tagUserInviteActivity）
  → 返回 UserLoginVo（含 token / menuTreeList）
```

注意：登录方法上有 `@Transactional(rollbackFor = Exception.class)`，事务边界覆盖 user 写入 + 角色/菜单查询，但**不**覆盖 Redis 写入和 MQ 发送（这两者是事务外副作用）。

### 3. Token 验证 / 续期流程
```
请求带 token → AOP 拦截 → UserTokenProducer.getUserByToken(token)
  → Redis 优先（未降级时）
  → Redis 降级 / 未命中 → tb_user_token 兜底
  → 命中后 GlobalObject.setLocalUser + RequestContext.setUserId
  → 排除路径不续期；其余更新 Redis TTL + 异步刷库
  → 心跳接口每 5 分钟节流刷库（BusinessCachePrefix.USER_TOKEN_HEARTBEAT_CACHE）
```

ThreadLocal: `GlobalObject.LOCAL_USER` 缓存当前用户的 `UserCacheVo`，含 token/角色/菜单/agentId。

### 4. 登出流程
```
GET /replay/user/logout
  → UserTokenProducer.removeUserToken(token, userId)
  → 删 Redis（token-key + login-info hash 项）
  → 删 tb_user_login_info（按 token） + tb_user_token（按 token）
  → 写 tb_user_login_log（operaType=3 退出）
```

### 5. 冻结 / 解冻流程（管理员对子账号一并处理）
```
管理端 updateUser(status=1)
  → 更新 tb_user.status
  → 若用户是代理商，连带更新所有子账号状态
  → 批量删除所有相关 token（Redis + DB）
  → 每个 token 落一条登录日志（"用户被冻结，强制退出"）
```

### 6. RBAC 鉴权（菜单装配）
```
listTreeSelf / listTreeByRoleId
  → 取当前用户 / 指定角色的 user_role
  → 关联 menu_role 找到 menu_id 集合
  → 查询 tb_menu，按 parentId 构建树
  → 返回 MenuTreeVo（递归 children）
```

### 7. 销售跟进闭环（与 CRM 共享）
```
销售填写 UserRemarkBo → POST /replay/userremark/save
  → 写 tb_user_remark（跟进类型/状态/下次跟进时间）
  → 同步刷新 tb_user_business 快照（accordingStatus/nextFolTime）
  → 看板/列表查询直接读 tb_user_business（O(1)）
```

详见 [[crm_domain]] 中 *Follow-up Record / Business Snapshot*。

### 8. 子账号 / 父账号查询
- `User.parentId`：子账号指向父主账号
- `getCurrentUserParentId()`：子账号返回 parentId；主账号返回自身 id
- `getUserIdNoPre(userId)`：递归向上找到 `parentId=0` 的祖先 userId
- 数据归属判断：所有跨账号资源默认归属于祖先（主账号），子账号共享

### 9. 代理商用户创建（`UserFeign.saveOrUpdateAgentUser`）
```
若 phone 已存在 MANAGE_ADMIN_USER → 校验 adminUserType 一致性 → 复用 userId
否则：
  雪花 ID 生成 username/password
  落 tb_user (userType=1)
  按 adminUserType 查 SystemKv 分配角色 ID：
    ADMIN → platform_sales_user_role_id
    AGENT → agent_user_role_id
    AGENT_SALE → agent_sales_user_role_id
  落 tb_user_details（agentId / saleId / channelId）
```

---

## 角色与权限（默认）

| 角色 | level | 说明 |
|------|-------|------|
| 超管 (id=1) | A | 拥有全部菜单，root 账号默认绑定 |
| 普通用户 | E | 客户端主账号默认角色（`power.defaultRoleId`） |
| 平台运营 | — | `power.platformOperationRoleId` 标识 |
| 代理商 / 代理商销售 / 平台销售 | — | 由 SystemKv 配置项映射 |

---

## 跨模块依赖

power 是**基础底座模块**：
- **被依赖**：几乎所有业务模块（words / order / agent / activity / reward 等）通过 [[UserFeign]] / [[SalesFeign]] / [[UserDeviceFingerprintFeign]] 查询用户信息
- **依赖**：[[CompanyBll]] 依赖 trade（行业），跟进数据看板依赖 order（订单/试用数据）通过 `PackageFeign` 等查询

特别说明：power 模块的 `UserDetails` / `UserRemark` / `UserBusiness` / `Sales` 实体同时承载 CRM 子系统的数据面 — 详见 [[crm_domain]] / [[crm_data]]。
