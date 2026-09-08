<!-- module: power -->
<!-- area: api -->
<!-- generated-by: reverse-scan -->
<!-- last-scan: 2026-05-20 -->
<!-- source-paths: replay-power/, replay-api/.../controller/power/ -->
<!-- field-level-detail: true -->

# Power API -- 接口契约

> replay-power 模块完整 API 表面。Controller 分布在 replay-api（管理端 + 客户端，11 个 Controller）和 replay-power（模块内部，3 个 Controller）。
> 所有 Controller 返回 `R<T>`，类上标 `@CrossOrigin` / `@Tag` / `@Operation`。鉴权：除登录/注册/验证码相关，均需 token（AOP 拦截 + `GlobalObject.getLocalUser()`）。

---

## 一、管理端 + 客户端 API（replay-api 模块）

Base: `com.jiuyu.replay.api.controller.power`

### 1. 用户管理（UserController） -- `@RequestMapping("replay/user")`

#### 注册与登录（无 token）

| Method | Path | Auth | Summary |
|--------|------|------|---------|
| POST | `/register` | 无 token + `@NoRepeatSubmit(key="#registerBo.phone")` | 注册 |
| POST | `/login` | 无 token | 账号密码/手机号验证码登录 |
| POST | `/loginOnline` | 无 token | Web 在线登录（source=web） |
| GET | `/loginByTempToken?tempToken=` | 无 token | 通过临时凭证登录 |
| GET | `/getLoginTempToken` | token | 获取登录的临时凭证 |
| GET | `/logout` | token | 登出 |

##### RegisterBo 字段（注册请求体）

| Field | Type | Required | Meaning |
|---|---|---|---|
| phone | String | `@NotBlank` | 手机号 |
| code | String | `@NotBlank` | 验证码 |
| username | String | -- | 用户名 |
| nickName | String | -- | 昵称 |
| password | String | -- | 密码 |
| invitationCode | String | -- | 邀请码 |
| inviteUrlCode | String | -- | 邀请链接code |
| saleId | Long | -- | 平台销售人员ID |
| channelId | Long | -- | 来源渠道ID |
| agentSaleId | Long | -- | 代理商销售人员ID |
| adminUserType | Integer | -- | 后台管理员类型（0正常/1代理商/2代理商销售） |
| agentId | Long | -- | 代理商ID |

##### LoginBo 字段（登录请求体）

| Field | Type | Required | Meaning |
|---|---|---|---|
| username | String | `@NotBlank` | 账号 |
| password | String | `@NotBlank` | 密码 |
| phone | String | `@NotBlank` | 手机号 |
| code | String | `@NotBlank` | 验证码 |
| userType | Integer | `@NotNull` | 用户类型 |

##### UserLoginVo 响应字段（登录/临时凭证登录返回）

| Field | Type | Meaning |
|---|---|---|
| id | Long | 用户ID |
| username | String | 用户名 |
| nickName | String | 昵称 |
| phone | String | 手机号 |
| token | String | 登录凭证 |
| status | Integer | 冻结状态（0未冻结/1已冻结） |
| userType | Integer | 用户类型（0普通/1后台管理员/2子账号） |
| activeTenantId | Long | 当前激活租户ID |
| adminUserType | Integer | 后台管理员类型 |

#### 验证码接口（无 token）

| Method | Path | Params |
|--------|------|--------|
| GET | `/getPhoneCode?phone=` | phone: String, 必填 |
| GET | `/checkPhoneCode?phone=&code=&isNewPhone=` | phone: String 必填; code: String 必填; isNewPhone: Integer 可选（0否/1是） |

#### 用户查询（需 token）

| Method | Path | Response | Note |
|--------|------|----------|------|
| POST | `/list` | `PageUtils<UserListVo>` | 分页列表 |
| POST | `/listManage` | `PageUtils<UserListManageVo>` | 强制 userType=1 |
| POST | `/listAdmin` | `PageUtils<UserVo>` | limit=-1 不分页 |
| POST | `/pageList` | `PageUtils<UserListVo>` | 条件分页 |
| POST | `/pageListNew` | `PageUtils<UserListVo>` | 含治理状态(t+1) |
| POST | `/exportUserInfoList` | `List<UserInfoExportVo>` | 导出新进用户 |
| POST | `/selectByuseId` | `PageUtils<UserListVo>` | 按sec_uid查 |
| POST | `/subAccountList` | `PageUtils<UserListVo>` | 子用户列表 |
| GET | `/platformOperationList` | `List<UserVo>` | 平台运营用户 |
| GET | `/getSelfPhone` | `R<String>` | 当前用户手机号(脱敏) |
| GET | `/getUserByToken` | UserVo | 根据token获取用户 |
| GET | `/infoByClient` | UserInfoVo | 客户端当前用户详情 |
| GET | `/info?id=` | UserInfoVo | 用户基础信息 |
| GET | `/userDetailByUserId?userId=` | UserDetailsInfoVo | `@AgentQueryUserCheck` |
| GET | `/checkFreeze` | `R<Integer>` | 0未冻结/1已冻结 |
| POST | `/userPropertyList` | `R<String>` | 用户资产信息 |

##### UserListBo 共享查询字段（用于 list/listManage/pageList/pageListNew/exportUserInfoList 等）

| Field | Type | Meaning |
|---|---|---|
| keyword | String | 模糊用户名搜索 |
| id / phone / parentId | Long/String/Long | 按ID/手机号/父账号过滤 |
| userType | Integer | 0普通/1后台管理员/2子账号 |
| userName / nickName | String | 用户名/昵称 |
| status | Integer | 冻结状态（0未冻结/1已冻结） |
| secUid | String | 主播唯一标识 |
| packageId | Long | 版本ID |
| companyName / companyIds | String/List\<Long\> | 公司名称/ID列表 |
| tradeName / tradeId / tradeIds | String/Long/List\<Long\> | 行业过滤 |
| userIds | List\<Long\> | 用户ID集合 |
| startTime / endTime | Date | 注册时间范围 |
| channelId | Long | 渠道ID |
| userBelongType | Integer | 客户类型（0个人/1工作室/2企业） |
| userAmbition | String | 用户意向（S/A/B/C/D） |
| salesId | Long | 销售人员ID |
| salesType | Integer | 销售归属（0平台/1代理商） |
| agentId | Long | 代理商ID |
| employeeStatus | Integer | 员工状态（0离职/1在职） |
| adminUserType | Integer | 后台管理员类型 |
| trialOrder | Integer | 是否试用（1试用/0正式） |
| isLoggedIn | Integer | 是否已登录（0未/1已） |
| expireTime | Long | 到期时间 |
| ownCount | Integer | 自有账号>=数量 |
| startDayAnalysis/endDayAnalysis | Long | 日平均分析范围 |
| startLongNotAnalysis/endLongNotAnalysis | Long | 多久未分析天数范围 |
| userWxName | String | 用户微信名称 |
| sortField | String | 排序字段（ownCount/packageEndDate/sumAnalysis/dayAnalysis/contrastAnalysis） |
| sortOrder | String | 排序类型（asc/desc） |
| clientVersion | String | 客户端版本（record-纯录制版/replay-复盘版） |
| specialSorting | Integer | 是否启用特殊排序（1是） |
| searchUpOrDown | Integer | 升序或降序 |
| page / limit | Integer | 分页参数（继承 PageBo） |

#### 用户写操作（需 token）

| Method | Path | Request | Summary |
|--------|------|---------|---------|
| POST | `/save` | UserAddBo | 新增用户 |
| POST | `/update` | UserUpdateBo | 后台修改用户 |
| POST | `/updateByClient` | UserUpdateClientBo | 客户端修改（返回 UserInfoVo） |
| POST | `/updateByUserId` | UserUpdateBo | 服务端修改账号类型 |
| POST | `/updateUser` | UserBo | 服务端冻结/解冻 |
| POST | `/updatePassword` | UpdatePasswordBo | 后台修改密码 |
| POST | `/updatePasswordByClient` | UpdatePasswordByClientBo | 客户端修改密码 |
| GET | `/resetPassword?id=` | id: Long 必填 | 重置默认密码 |
| GET | `/resetRandomPassword?id=` | id: Long 必填 | 重置随机六位数密码 |
| GET | `/delete?id=` | id: Long 必填 | 删除用户 |
| POST | `/deleteByIds` | `List<Long>` | 批量删除 |
| GET | `/updateTrade?userId=&tradeId=` | userId, tradeId: Long 必填 | 更新行业 |
| POST | `/loadRedisTokensToDatabase` | -- | Redis token 同步到 DB |

##### UserAddBo 字段

| Field | Type | Required | Meaning |
|---|---|---|---|
| username | String | `@NotBlank` | 用户名 |
| password | String | `@NotBlank` | 密码 |
| phone | String | `@NotBlank` | 手机号 |
| nickName | String | `@NotBlank` | 昵称 |
| roleIdList | List\<Long\> | -- | 角色ID列表 |
| adminUserType | Integer | -- | 后台管理员类型（0正常/1代理商/2代理商销售） |

#### User VO 响应字段汇总

**UserVo / UserListVo** 常用：id, username, nickName, phone, status (冻结 0/1), userType (0普通/1管理员/2子账号), parentId, activeTenantId, adminUserType, wxOpenid, createDate。

**UserInfoVo** 继承 UserVo 附加 roleIdList (List\<Long\>)。

**UserDetailInfoVo** 附加：tradeId, companyId, anchorType (0个人/1公司), realName, idCard, sex (1男/2女), email, address, channelId, saleId, wxName, agentSaleId, userAmbition。

---

### 2. 租户管理（TenantController） -- `@RequestMapping("power/tenant")`

| Method | Path | Request | Response |
|--------|------|---------|----------|
| POST | `/list` | TenantListBo (extends PageBo) | `PageUtils<TenantListVo>` |
| GET | `/info?id=` | id: Long 必填 | TenantInfoVo |
| POST | `/save` | TenantBo | `R<String>` |
| POST | `/update` | TenantBo | `R<String>` |
| GET | `/delete?id=` | id: Long 必填 | `R<String>` |
| GET | `/select-options` | TenantSearchBO | `List<TenantOptionVo>` |

##### TenantBo 字段

| Field | Type | Meaning |
|---|---|---|
| tenantName | String | 租户名称 |
| showPdfHead | Integer | 显示导出pdf头（0不显示/1显示） |
| userId | Long | 所属用户ID |

---

### 3. 租户-用户关联（TenantUserController） -- `@RequestMapping("power/tenantuser")`

标准 CRUD：`POST /list` → TenantUserListBo → `PageUtils<TenantUserListVo>`；`GET /info?id=` → TenantUserInfoVo；`POST /save|/update` → TenantUserBo（tenantId + userId）；`GET /delete?id=`。

---

### 4. 菜单管理（MenuController） -- `@RequestMapping("replay/menu")`

| Method | Path | Note |
|--------|------|------|
| GET | `/listTreeByRoleId?roleId=` | 角色拥有的菜单树 |
| GET | `/listTreeSelf` | 当前账号菜单树 |
| POST | `/list` | 分页列表 MenuListBo → `PageUtils<MenuVo>` |
| GET | `/info?id=` | 详情 MenuVo |
| POST | `/save` | 新增 MenuBo |
| POST | `/update` | 修改 MenuBo |
| GET | `/delete?id=` | 删除 |

##### MenuBo 字段

| Field | Type | Meaning |
|---|---|---|
| name | String | 菜单名 |
| parentId | Long | 父菜单ID |
| url | String | 菜单URL |
| type | Integer | 类型（0菜单/1功能/2目录） |
| sort | Integer | 排序 |
| img | String | 图标 |

---

### 5. 角色管理（RoleController） -- `@RequestMapping("replay/role")`

| Method | Path | Note |
|--------|------|------|
| POST | `/list` | RoleListBo → `PageUtils<RoleVo>` |
| GET | `/info?id=` | RoleInfoVo（含 roleIdList 菜单ID列表） |
| POST | `/save` | RoleInfoBo（name 角色名, level 角色级别, roleIdList 菜单ID列表） |
| POST | `/update` | RoleInfoBo |
| GET | `/delete?id=` | 删除 |

---

### 6. 销售管理（SalesController） -- `@RequestMapping("replay/sales")`

| Method | Path | Note |
|--------|------|------|
| GET | `/getCurrentUserSale` | 当前用户销售人员信息 → SalesInfoVo |
| GET | `/getByPhoneUserSale?phone=` | 按手机号查（无 token） |
| GET | `/getCurrentUserSaleByPhone?phone=` | 按手机号查 |
| POST | `/list` | SalesListBo → `PageUtils<SalesListVo>` |
| GET | `/info?id=` | → SalesInfoVo |
| POST | `/save` | `@Validated(Insert.class)` SalesBo |
| POST | `/update` | `@Validated(Insert.class)` SalesBo |
| POST | `/updateChooseStatus` | `@Validated(IsChooseUpdate.class)` SalesBo（id + isChoose） |
| POST | `/updateUserPolling` | `@Validated(IsUpdateUserPolling.class)` SalesBo（id + userPolling） |
| GET | `/delete?id=` | 删除 |
| GET | `/userListSalesSearch?employeeStatus=` | 带权限搜索 → `List<SalesCascaderVo>` |
| GET | `/userDetailsSalesSearch?userId=` | 带权限搜索 → `List<SalesInfoVo>` |
| POST | `/updateEmployeeStatus` | EmployeeStatusBo `@Validated` |

##### SalesBo 字段

| Field | Type | Required | Meaning |
|---|---|---|---|
| salesName | String | `@NotNull(Insert/Update)` | 销售人员名 |
| phone | String | `@NotNull(Insert/Update)` | 手机号 |
| qrcodeImgId | Long | `@NotNull` | 二维码图片文件ID |
| isChoose | Integer | `@NotNull(Insert/Update/IsChooseUpdate)` | 是否分配线索（0不开/1开） |
| userPolling | Integer | `@NotNull(Insert/Update/IsUpdateUserPolling)` | 是否开启轮询（0否/1是） |
| salesIntroductionUrl | String | -- | 获客助手连接 |
| salesType | Integer | -- | 销售类型（0平台/1代理商） |
| agentId | Long | -- | 代理商ID |
| userId | Long | -- | 用户ID |
| parentId | Long | -- | 父ID |

##### EmployeeStatusBo 字段

| Field | Type | Required | Meaning |
|---|---|---|---|
| sourceId | Long | `@NotNull` | 0-userId / 1-agentId / 2-salesId |
| sourceType | Integer | `@NotNull` | 0用户/1代理商/2销售 |
| employeeStatus | Integer | `@NotNull` | 0离职/1在职 |

---

### 7. 用户详情（UserDetailsController） -- `@RequestMapping("replay/userdetails")`

| Method | Path | Note |
|--------|------|------|
| POST | `/saveUserDetails` | UserDetailsBo 保存/更新详情（userId, tradeId, companyId, anchorType, position, email, idCard, realName, sex, birthday, address, channelId, saleId, wxName, agentSaleId, isShow, videoMeetPath, userAmbition, userBelongType 等） |

---

### 8. 用户登录日志（UserLoginLogController） -- `@RequestMapping("replay/userloginlog")`

标准 CRUD：`POST /list` → UserLoginLogListBo；`GET /info?id=` → UserLoginLogInfoVo；`POST /save|/update` → UserLoginLogBo（userId, userName, userType, operaType 0登录/1离线/2上线/3退出, ipAddress, operaStatus 0成功/1失败, remarks）。

---

### 9. 用户标签 -- `@RequestMapping("replay/tag")` + `"replay/usertag")`

**TagController** (`replay/tag`)：标准 CRUD，TagBo 含 name（标签名）, remarks（描述）。

**UserTagController** (`replay/usertag`)：标准 CRUD，UserTagBo 含 userId, tagId。

---

### 10. 用户备注/跟进记录（UserRemarkController） -- `@RequestMapping("replay/userremark")`

| Method | Path | Note |
|--------|------|------|
| POST | `/list` | UserRemarkListBo → `PageUtils<UserRemarkListVo>` |
| GET | `/info?id=` | UserRemarkInfoVo |
| POST | `/save` | `@Validated(Insert.class)` -- 同步刷新 tb_user_business |
| POST | `/update` | `@Validated(Update.class)` |
| GET | `/delete?id=` | 软删除 |

##### UserRemarkBo 字段

| Field | Type | Required | Meaning |
|---|---|---|---|
| userId | Long | `@NotNull(Insert)` | 用户ID |
| remark | String | -- | 备注内容 |
| followType | Integer | -- | 跟进类型 |
| followStatus | Integer | -- | 跟进状态 |
| nextFolTime | Date | -- | 下次跟进时间 |
| followUpTime | Date | -- | 跟进时间 |

---

## 二、模块内 Controller（replay-power 模块）

Base: `com.jiuyu.replay.power.controller`

### PowerUserController -- `@RequestMapping("replay/power/user")`

| Method | Path | Note |
|--------|------|------|
| GET | `/clientGetSubUserList` | 客户端子账号列表 |
| GET | `/sendCustomerAcquisitionMsg?userId=` | 发送获客短信 |
| POST | `/clientUserPageList` | 客户端用户分页 |
| GET | `/dashboardStatistics?trialOrder=` | 看板统计 → DashboardStatisticsVo |
| POST | `/pageDashboardList` | 看板分页 |

### PowerTenantController -- `@RequestMapping("replay/power/tenant")`

`GET /tenantConfig` -- 当前用户租户配置（基于 activeTenantId）

### SalesStatisticsController -- `@RequestMapping("replay/power/salesStatistics")`

**客户看板:** `GET /salesSelect`; `POST /salesCardStatisticsData` (`@Validated`); `POST /salesEchartsStatisticsData`; `POST /salesTrialAboutTo3DayExpires`; `POST /salesAboutTo3Day`; `POST /salesTrialAboutTo15DayRenewal`。

**团队看板:** `GET /deptList`; `POST /teamCardStatisticsData` (`@Validated(DateChange.class)`); `POST /teamEchartsStatisticsData`; `POST /teamTrialAboutTo3DayExpiresStatistics` → `Map<String,Integer>`; `POST /teamTrialAboutTo3DayExpires`; `POST /teamSalesFollowList` → EachSalesFollowStatisticsVo。

---

## 三、注解 / AOP

| 注解 | 用途 |
|------|------|
| `@NoRepeatSubmit(key="#xxx")` | 防重复提交（仅 `UserController#register`） |
| `@CrossOrigin` | 类级别 CORS |
| `@Validated({Insert\|Update\|...})` | 分组校验 |
| `@AgentQueryUserCheck(checkUserId="#args[0]")` | 代理商查询用户校验（仅 `userDetailByUserId`） |

## 四、错误码

业务异常用 `R.error(code, msg)`：`4001 NO_LOGIN` / `4002 NO_POWER` / `4003 TIP_CUSTOM` / `4004 IS_REGISTER`。其他抛 `BusinessException(StatusCode.PARAM_EX.getCode(), msg)`。

## 五、Feign 接口

- `UserFeign` -- 14方法：`getLocalUser` / `userById` / `getUserTenantId` / `parentUserByUserId` / `saveOrUpdateAgentUser` / `updateAdminUserStatusByPhone`
- `SalesFeign` -- 9方法
- `UserDeviceFingerprintFeign` -- 3方法
