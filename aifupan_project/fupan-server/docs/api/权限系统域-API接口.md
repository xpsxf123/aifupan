# API接口文档 - 权限与系统域

> 基础路径: /replay

## 1. UserController - 用户管理
**路径前缀**: `replay/user`

| HTTP | 路径 | 说明 | 参数 | 返回 |
|------|------|------|------|------|
| POST | /login | 用户登录 | LoginBo | UserTokenVo |
| POST | /register | 用户注册 | RegisterBo | String |
| POST | /loginOnline | 在线登录 | OnlineLoginBo | UserTokenVo |
| POST | /list | 用户列表 | UserListBo | PageUtils\<UserListVo\> |
| GET | /info | 用户信息 | id(Long) | UserInfoVo |
| POST | /save | 新增用户 | UserBo | String |
| POST | /update | 修改用户 | UserBo | String |
| GET | /delete | 删除用户 | id(Long) | String |

## 2. RoleController - 角色管理
**路径前缀**: `replay/role`

| HTTP | 路径 | 说明 | 参数 | 返回 |
|------|------|------|------|------|
| POST | /list | 角色列表 | RoleListBo | PageUtils\<RoleListVo\> |
| GET | /info | 角色信息 | id(Long) | RoleInfoVo |
| POST | /save | 新增角色 | RoleBo | String |
| POST | /update | 修改角色 | RoleBo | String |
| GET | /delete | 删除角色 | id(Long) | String |

## 3. MenuController - 菜单管理
**路径前缀**: `replay/menu`

| HTTP | 路径 | 说明 | 参数 | 返回 |
|------|------|------|------|------|
| POST | /list | 菜单列表 | MenuListBo | PageUtils\<MenuListVo\> |
| GET | /info | 菜单信息 | id(Long) | MenuInfoVo |
| POST | /save | 新增菜单 | MenuBo | String |
| POST | /update | 修改菜单 | MenuBo | String |
| GET | /delete | 删除菜单 | id(Long) | String |

## 4. TenantController - 租户管理
**路径前缀**: `replay/tenant`

| HTTP | 路径 | 说明 | 参数 | 返回 |
|------|------|------|------|------|
| POST | /list | 租户列表 | TenantListBo | PageUtils\<TenantListVo\> |
| GET | /info | 租户信息 | id(Long) | TenantInfoVo |
| POST | /save | 新增租户 | TenantBo | String |
| POST | /update | 修改租户 | TenantBo | String |
| GET | /delete | 删除租户 | id(Long) | String |

## 5. TenantUserController - 租户用户
**路径前缀**: `replay/tenantuser`

| HTTP | 路径 | 说明 | 参数 | 返回 |
|------|------|------|------|------|
| POST | /list | 租户用户列表 | TenantUserListBo | PageUtils\<TenantUserListVo\> |
| GET | /info | 租户用户信息 | id(Long) | TenantUserInfoVo |
| POST | /save | 新增租户用户 | TenantUserBo | String |
| POST | /update | 修改租户用户 | TenantUserBo | String |
| GET | /delete | 删除租户用户 | id(Long) | String |

## 6. SalesController - 销售管理
**路径前缀**: `replay/sales`

| HTTP | 路径 | 说明 | 参数 | 返回 |
|------|------|------|------|------|
| POST | /list | 销售列表 | SalesListBo | PageUtils\<SalesListVo\> |
| GET | /info | 销售信息 | id(Long) | SalesInfoVo |
| POST | /save | 新增销售 | SalesBo | String |
| POST | /update | 修改销售 | SalesBo | String |
| GET | /delete | 删除销售 | id(Long) | String |

## 7. UserDetailsController - 用户详情
**路径前缀**: `replay/userdetails`

标准CRUD接口（list/info/save/update/delete）

## 8. UserLoginLogController - 登录日志
**路径前缀**: `replay/userloginlog`

标准CRUD接口（list/info/save/update/delete）

## 9. UserRemarkController - 用户备注
**路径前缀**: `replay/userremark`

标准CRUD接口（list/info/save/update/delete）

## 10. UserTagController - 用户标签
**路径前缀**: `replay/usertag`

标准CRUD接口（list/info/save/update/delete）

## 11. TagController - 标签管理
**路径前缀**: `replay/tag`

标准CRUD接口（list/info/save/update/delete）

## 12. DictTypeController - 字典类型
**路径前缀**: `replay/dicttype`

标准CRUD接口（list/info/save/update/delete）

## 13. DictDataController - 字典数据
**路径前缀**: `replay/dictdata`

标准CRUD接口（list/info/save/update/delete）

## 14. LoginRotateImageController - 登录旋转图片
**路径前缀**: `replay/loginrotateimage`

标准CRUD接口（list/info/save/update/delete）

## 15. CommonController - 通用接口
**路径前缀**: `replay/common`

通用功能接口（图片上传、验证码等）

## 16. FileController - 文件管理
**路径前缀**: `replay/file`

标准CRUD接口 + 文件上传/下载

## 17. ArticleController - 文章管理
**路径前缀**: `replay/article`

标准CRUD接口（list/info/save/update/delete）

## 18. ClientLogController - 客户端日志
**路径前缀**: `replay/clientlog`

标准CRUD接口（list/info/save/update/delete）

## 19. ClientUpdateController - 客户端更新
**路径前缀**: `replay/clientupdate`

标准CRUD接口 + 下载文件接口

## 20. OperationLogController - 操作日志
**路径前缀**: `replay/operationlog`

标准CRUD接口（list/info/save/update/delete）

## 21. SystemConfigController - 系统配置
**路径前缀**: `replay/systemconfig`

标准CRUD接口（list/info/save/update/delete）

## 22. SystemKvController - 系统KV
**路径前缀**: `replay/systemkv`

标准CRUD接口（list/info/save/update/delete）

## 23. TencentCosController - 腾讯COS
**路径前缀**: `replay/tencentcos`

COS文件操作接口

## 24. UserGrayscaleController - 灰度管理
**路径前缀**: `replay/usergrayscale`

标准CRUD接口（list/info/save/update/delete）

## 25. SalesStatisticsController (replay-power)
**路径前缀**: `replay/power/salesstatistics`

销售统计相关接口
