<!-- module: power -->
<!-- area: architecture -->
<!-- generated-by: reverse-scan -->
<!-- last-scan: 2026-05-20 -->
<!-- source-paths: replay-power/, replay-api/.../controller/power/ -->

# Power Architecture — 架构决策与设计

> replay-power 模块的架构基线、设计决策与关键技术约束。作为整个平台的**基础底座**，几乎所有业务模块都通过 Feign 调用 power 暴露的 API。

---

## 一、模块定位

replay-power 承载平台的**基础能力底座**：

- **用户**: 注册 / 登录 / 鉴权 / 子账号 / 父子绑定 / 设备指纹
- **租户**: 多租户隔离 / 租户切换 / 租户配置
- **RBAC**: 角色 / 菜单 / 权限矩阵
- **会话**: Token 持久化（Redis + DB 双存）/ 心跳续期 / 强制下线
- **审计**: 登录日志
- **销售**: 销售人员 / 部门 / 团队 / 客户分配 / 轮询
- **跟进**: 跟进记录 / 业务快照（CRM 子系统共享面 —— 详见 [[crm_architecture]]）
- **统计**: 客户/团队数据看板
- **CRM 集成**: AI 画像 / 阶段事件存储（销售智能体回写）

---

## 二、分层架构

```
┌────────────────────────────────────────────────────────────┐
│  Controller                                                │
│  - 管理端 + 客户端 API: replay-api/controller/power/  (11)  │
│  - 模块内 controller:    replay-power/controller/      (3)  │
├────────────────────────────────────────────────────────────┤
│  Logic (Bll)                                               │
│  - replay-api/logic/power/ 装配层 (Controller 直调)        │
│  - replay-power/bll/        模块内业务  (16 个 Bll)        │
├────────────────────────────────────────────────────────────┤
│  Producer (15 接口 + 15 实现)                              │
│  - @Service, 完整业务逻辑, @Transactional                 │
│  - Entity ↔ VO/BO 转换，雪花 ID 生成                       │
├────────────────────────┬───────────────────────────────────┤
│  Service (23 对)       │  Rse / Api (Feign 提供方实现)     │
│  IService<Entity>      │  UserApi → UserFeign              │
│  extends ServiceImpl   │  SalesApi → SalesFeign            │
│                        │  UserDeviceFingerprintApi → ...   │
│                        │  OperationLogApi → ...            │
├────────────────────────┴───────────────────────────────────┤
│  Dao (BaseMapper<Entity>) — 21 个                          │
│  - + 2 个 Mybatis-Plus Mapper（CrmAiProfileMapper / CrmStageEventMapper）│
└────────────────────────────────────────────────────────────┘
```

### 历史分层说明（Bll / Producer / Rse 三层）

power 模块沿用与 words / order / agent 相同的历史分层（`@Component Bll` + `@Service Producer` + `@Component Rse`）。 **本项目不再 push 新代码使用此分层**（CLAUDE.md §4），新模块/新业务默认 `Controller → Service → Mapper` 三层。读懂存量代码时遵循即可。

### 层级职责（存量约定）

| 层 | 允许操作 | 禁止操作 |
|----|----------|----------|
| Controller | 参数校验、调用 Logic/Bll、返回 R<T> | 直接调 Dao / 含业务逻辑 |
| Logic / Bll | 组合 Producer 调用、与 Feign 交互、简单转换 | `@Transactional` (单测特殊放宽) / 直接操作 Entity |
| Producer | 完整 CRUD、`@Transactional(rollbackFor = Exception.class)`、Entity↔VO 转换、雪花 ID 生成 | 跨模块调用（必须通过 Rse / Feign） |
| Service | MyBatis-Plus 基础 CRUD / 自定义批量查询 | 业务逻辑 |
| Dao | 单表 CRUD + 复杂 SQL 聚合（XML） | 业务装配 |
| Api (Feign Impl) | Feign 接口实现，对外暴露给其他模块 | 业务编写（委托 Producer） |

---

## 三、ADR 记录

### ADR-001: Power 作为基础底座 — 所有模块经 Feign 访问
- **决策**: User / Tenant / Sales / Token 等基础查询统一通过 `UserFeign` / `SalesFeign` / `UserDeviceFingerprintFeign` 暴露
- **原因**: 避免业务模块直接读 `tb_user` 等核心表导致权限/状态绕过；统一治理点
- **影响**: 新业务必须通过 Feign，不可跨模块 SELECT power 表

### ADR-002: Token 双存策略（Redis + DB）
- **决策**: 登录 token 同时写 Redis (`replay:user:login:token:` + `replay:user:login:info:`) 和 DB (`tb_user_token` + `tb_user_login_info`)
- **原因**: Redis 故障时（`ResilientRedisTemplate.isDegraded()` 检测）走 DB 兜底，避免登录态雪崩
- **影响**:
  - Redis 写入失败 / 降级时仍可登录
  - 心跳接口节流刷库（每 token 5 分钟一次）减压
  - 一致性靠 `loadRedisTokensToDatabase` / `syncDegradedTokenDataToRedis` 双向同步
- **代价**: DB 多写一份 + 状态变更时双写复杂度

### ADR-003: UserCacheVo 经 ThreadLocal 传递当前用户
- **决策**: `GlobalObject.LOCAL_USER` (ThreadLocal) 缓存当前请求的 UserCacheVo
- **原因**: 避免每次业务方法都 `getUserByToken`；菜单 / 角色 / agentId / activeTenantId 一次装配多次使用
- **影响**: 异步线程 / 子线程必须显式 `setLocalUser` / `removeLocalUser`；线程池场景容易泄漏，需在请求过滤器 finally 清理
- **同步双向通道**: `RequestContext.setUserId(userVo.getId())` 也写入，下游通过 `RequestContext.getUserId()` 可拿到

### ADR-004: 子账号继承父账号的 activeTenantId
- **决策**: 登录时若 `userType=2 (CLIENT_CHILD_USER)`，将 activeTenantId 设为父账号当前激活的 tenant
- **原因**: 子账号没有独立租户，必须借父账号租户访问数据
- **影响**: 父账号切换租户时，需要批量同步刷新已登录子账号的 Redis 缓存（暂未实现，依赖下次登录自动同步）

### ADR-005: 三种 isDeleted 写法并存
- **决策**:
  - 主流 23 张表中大多手动 `is_deleted (Integer)` + `Integer isDeleted`
  - 2025 年新增 token 类 3 张表使用 `@TableLogic + Byte`
- **原因**: 历史遗留 + 后期作者偏好；统一难以一次迁移
- **影响**: 新代码遵循 CLAUDE.md §5 全局约定 — 手动 `isDeleted`，禁 `@TableLogic`。读 token 表代码遵循其现有写法即可，**不要扩散** `@TableLogic` 到新表

### ADR-006: 父子账号通过 `parentId` 链建模，不引入显式 group 表
- **决策**: `tb_user.parent_id` 单字段表达；`tb_binding_account` 仅作绑定历史
- **原因**: 子账号树深度 ≤2（主账号→子账号），无需深度递归
- **影响**: 查询主账号祖先 (`getUserIdNoPre`) 简化为单跳；`parentId=0` 标识根账号

### ADR-007: 销售归属 ≠ 数据归属
- **决策**: `tb_user_details.saleId` 标识跟进销售，但与租户隔离 / 数据权限**无关**
- **原因**: 销售可被重新分配；数据归属用 `tenantId` + `parentId` 链路独立判定
- **影响**: 销售看板按 saleId 聚合；数据查询仍用 tenantId 过滤

### ADR-008: CRM 智能体数据存于 power 模块
- **决策**: `tb_crm_ai_profile` / `tb_crm_stage_event` 落 power 而非独立 CRM 模块
- **原因**: 共享 `userId` 主体；与 `tb_user_details` / `tb_user_business` 同库便于关联查询；现阶段无独立 CRM 微服务规划
- **影响**: power 模块承担"用户" + "CRM 集成"双重职责，慎防模块边界膨胀

---

## 四、定时任务

模块内已发现的定时任务（基于 XXL-JOB `@XxlJob`）：

| 任务类 | Job Handler | 调度 | 功能 | 状态 |
|--------|-------------|------|------|------|
| `UserScheduledTasks` | `checkUserLogin` | 设计上每 10s | 检测用户离线（>30s 无心跳）自动改为离线状态 | **当前 TODO 注释**，方法体 no-op，未启用 |

> 注：心跳过期 / token 节流刷库走业务请求触发，无独立定时任务；Token 同步与 Redis 降级回灌通过 API（`/loadRedisTokensToDatabase`、`syncDegradedTokenDataToRedis`）按需触发。

---

## 五、跨模块通信

### 1. Feign 接口（power 暴露 → 其他模块消费）

定义在 `replay-generic/src/main/java/com/jiuyu/replay/generic/feign/power/`，实现在 `replay-power/api/`：

| Feign 接口 | 实现类 | 方法数 | 主要消费方 |
|------------|--------|--------|-----------|
| `UserFeign` | `UserApi` | 14 | words / order / agent / activity / reward / third / ai |
| `SalesFeign` | `SalesApi` | 9 | agent / order / activity |
| `UserDeviceFingerprintFeign` | `UserDeviceFingerprintApi` | 3 | 主要内部使用 / third 模块 |
| `OperationLogFeign` (跨模块通用) | `OperationLogApi` | 1 (`saveOptLog`) | 全模块审计日志通用接口（实现在 power） |

#### `UserFeign` 关键方法
- `getLocalUser()` — 取当前线程的 UserCacheVo（ThreadLocal）— 最热门，被几乎所有 Controller 调用
- `userById(userId)` / `getUserParentId(userId)` / `getUserIdOrParentId(userId)` — 用户主体信息
- `getUserTenantId(userId)` / `getUserIdByTenantId(tenantId)` — 用户 ↔ 租户互查
- `listByIds(ids)` / `listByPhones(phoneList)` / `listByNameOrPhone(keyword)` — 批量查
- `saveOrUpdateAgentUser(bo)` — 代理商用户创建/修改（含角色分配）
- `updateAdminUserStatusByPhone(phone, status)` — 管理员状态变更（连带子账号 + token 清理）
- `parentUserByUserId(userId)` — 取主账号
- `getSubUserCountByUserId(userId)` / `getUserTypeMap(ids)` — 子账号 / 类型映射

#### `SalesFeign` 关键方法
- `queryPage(salesListBo)` — 分页列表
- `getByUserId(userId)` / `getBySalesUserId(userId)` — 按用户 / 销售 userId 查
- `listByAndAgentSalesTypeId(agentId, salesType, userPolling)` — 按代理商查可轮询销售
- `exCantIds(salesIds)` — 过滤"不开启轮询"的销售
- `updateEmployeeStatusByAgentId(agentId, employeeStatus)` — 代理商旗下销售统一改员工状态

### 2. Feign 反向依赖（power 消费其他模块）

power 模块自身需调用：

| 调用方向 | Feign | 用途 |
|----------|-------|------|
| power → activity | `UserInviteFeign` | 注册 / 登录 / 邀请回流 |
| power → agent | `ChannelFeign` | 渠道信息装配到 UserDetails |
| power → order | `PackageFeign` / `UserPropertyDetailsFeign` | 套餐 / 用户资产装配 |
| power → third | `SmsServiceFeign` / `GovernanceTenantService` | 短信 / 治理状态 |
| power → words | `AnchorUrlUserFeign` / `AnchorVideoFeign` / `VideoTextNotesFeign` | 子账号视频数 / 主播数 / 笔记数装配 |
| power → common | `SystemKvProducer` (替代 Feign，common 直注入) | 系统配置项（角色 ID / KV 配置） |

### 3. MQ 通信

power 模块通过 `RocketMqBll.syncSendNormalMessage(...)` 发送：

| Tag (`RocketMqProperties`) | 触发场景 | 消费方 |
|---------------------------|----------|--------|
| `tagUserInviteActivity` | 用户登录成功（fillUserLoginVo 末尾） | replay-activity |

> 当前 power 模块**未发现** RocketMQ 消费者 / `@RocketMQMessageListener`。所有消费在 activity / order / reward 等业务模块。

### 4. 共享 Service 调用（同 JVM Spring 直注入）

power 模块的 Bll 中通过 `@Resource` / 构造器注入直接调用 common 模块：
- `RocketMqBll` — MQ 发送封装
- `FileBll` — 文件 / 头像查询
- `ResilientRedisTemplate` — Redis 容错封装
- `CacheFallbackDataService` — Redis 降级时本地缓存兜底
- `SystemKvProducer` — 系统配置 KV
- `OperationLogProducer` — 操作审计日志

---

## 六、缓存策略（Redis）

### Redis Key 前缀（PowerProperties 配置注入）

| Key 前缀（来自 `application-*.yml` 的 `power.*` 配置）| 用途 | TTL |
|------------------------------------------------------|------|-----|
| `power.phoneCodeRedisKey` + `{phone}` | 手机验证码 | 短期（业务自定） |
| `power.userLoginTokenRedisKey` + `{token}` | 用户登录 token 缓存（value=UserCacheVo） | 30 天 |
| `power.userLoginInfoRedisKey` + `{userId}` (Hash, field=token) | 用户多端登录信息 (Hash, field=token, value=UserLoginInfoCacheVo) | 30 天 |
| `power.tempLoginToken` + `{...}` | 登录临时凭证（loginByTempToken 用） | 短期 |
| `power.resetPassword` | 重置密码默认值 | — |
| `BusinessCachePrefix.USER_TOKEN_HEARTBEAT_CACHE.prefix + {token}` | 心跳节流标记（5 分钟） | 10 分钟 |
| `RedisPowerKeyCache.REDIS_POWER_SALE_INDEX_KEY` = `replay:sale:index:` | 销售序号（轮询用） | 业务自定 |

### 一致性策略
- 写：双写（Redis + DB）；Redis 失败 / 降级（`ResilientRedisTemplate.isDegraded()`）只走 DB
- 读：Redis 优先，未命中或降级则查 DB
- 删：Redis + DB 同步删
- 修改用户状态：`UserTokenProducer.updateUserStatusByUserId` 同步刷新缓存里的 status 字段（避免冻结/解冻后旧 token 仍可用）
- 修改手机号：`updateUserMobileCache` 同步更新缓存中所有 token 的 phone

---

## 七、安全 / 鉴权机制

### 鉴权链路
1. 请求带 `token` header → AOP 拦截器（在 replay-api / replay-common 中实现）
2. `UserTokenProducer.getUserByToken(token)` → Redis 优先 → DB 兜底
3. 命中 → `GlobalObject.setLocalUser(userCacheVo)` + `RequestContext.setUserId(userId)`
4. Controller / Bll 通过 `GlobalObject.getLocalUser()` 取上下文（含 token / 角色 / 菜单 / agentId）
5. 排除路径不刷新 TTL；普通业务接口异步刷库
6. 心跳接口节流刷库（5 分钟一次）

### 权限校验
- 菜单级：`tb_user_role` → `tb_menu_role` → `tb_menu` 装配后存入 UserCacheVo.menuList
- 接口级：通过 `@AgentQueryUserCheck(checkUserId="#args[0]")` 等自定义注解校验跨账号访问
- 数据级：跨模块查询必须带 `tenantId` 过滤（CLAUDE.md §5 硬约束）

### 密码 / 凭证
- 密码：`MD5Utils.md5(plaintext)` 单向哈希存储（弱算法，**待升级** bcrypt / Argon2）
- token：`UUID.randomUUID().toString().replaceAll("-","")` 32 位无连字符
- 临时凭证：`tempLoginToken` Redis 短期存储
- 手机验证码：6 位数字 + Redis 短期存储 + 一次性消费

### 敏感字段
- `password` 输出时强制置空
- `phone` 输出时脱敏（`xxx****xxxx`）
- `ips` 输出时强制置空（避免 IP 泄露）
- `身份证 idCard` 不入日志

---

## 八、关键设计模式

| 模式 | 在 power 模块的应用 |
|------|-------------------|
| **Façade** | `UserApi` / `SalesApi` 统一暴露 Feign，封装内部 Producer / Bll 复杂度 |
| **Bulkhead / Circuit Breaker** | `ResilientRedisTemplate.isDegraded()` 检测 Redis 故障，自动 fallback 到 DB |
| **Cache-Aside** | Redis token 缓存：业务请求时按需读 / 写 / 失效，由业务代码控制 |
| **Throttle** | 心跳接口 5 分钟节流刷库（`BusinessCachePrefix.USER_TOKEN_HEARTBEAT_CACHE`） |
| **Template Method** | Producer 接口 + Impl，方法模板分散（每个 Producer 各自定义业务，**非**严格 Template Method） |
| **ThreadLocal Context** | `GlobalObject.LOCAL_USER` + `RequestContext.userId` 双向通道 |
| **Idempotency Key** | `tb_crm_ai_profile.profile_id` / `tb_crm_stage_event.event_id` 唯一约束保证幂等覆盖 |
| **Anti-Replay** | `@NoRepeatSubmit(key="#registerBo.phone")` 注册防重复 |

避开的反模式：
- ❌ 不在 Producer 外层加 `@Transactional`（事务边界统一在 Producer）
- ❌ 不在 Controller / Bll 用 `userDao.selectById` 直读（必须走 Producer）
- ❌ 不在跨模块场景直连 Dao（必须 Feign）
- ❌ 不在新表使用 `@TableLogic`（手动 isDeleted）

---

## 九、关键约束（投影自 CLAUDE.md §5）

- **DI**: 构造器注入（新代码），现存代码仍大量使用 `@Resource`（不强行重构）
- **R<T>**: Controller 必须返回 `R<T>`，禁裸返业务对象
- **Snowflake ID**: 所有新增实体走 `SnowflakeManager.nextValue()` + `@TableId(type=IdType.INPUT)`
- **软删除**: 手动 `isDeleted`，禁 `@TableLogic`（token 三表为历史例外，新代码不参照）
- **时间戳**: `new Date()` / `LocalDateTime.now()` 显式赋值 `createDate` / `updateDate`
- **租户隔离**: 跨模块查询必须 `tenantId` 过滤；power 内查询通过 `parentId` 链 + Feign 治理
- **事务**: 所有写方法 `@Transactional(rollbackFor = Exception.class)`，事务边界在 Producer
- **N+1 / IN ≤500**: 大批量 `IN` 用 `Lists.partition(ids, 500)` 分批；列表查询批量装配（反 JOIN）
- **敏感字段**: 密码 / 手机 / 身份证不入日志；输出时脱敏 / 置空
- **Bean 拷贝**: Spring `BeanUtils.copyProperties` 或 Hutool `BeanUtil.copyProperties`（两者混用，新代码倾向 Hutool）

---

## 十、未涵盖 / 待补充

- `<待补充>` `UserDao.xml` / `SalesDao.xml` / `DeptDao.xml` 等复杂聚合 SQL 的索引依赖
- `<待补充>` 完整的 Redis key 清单（部分仅在 `application-*.yml` 中定义）
- `<待补充>` 权限审计 / 操作日志的全链路落地点（OperationLogApi 实现细节）
- `<待补充>` 子账号租户切换的 Redis 同步刷新方案（当前依赖下次登录）
- `<待补充>` 密码哈希算法迁移路线（MD5 → bcrypt / Argon2）
- `<待补充>` `power.platformOperationRoleId` 等系统角色的初始化脚本
