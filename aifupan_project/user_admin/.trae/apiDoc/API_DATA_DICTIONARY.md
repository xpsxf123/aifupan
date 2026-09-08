# 前端 API 文档及数据字典

本文档包含了当前系统（复盘治理服务）中组织架构模块（公司、部门、小组）和 RBAC 权限模块（员工、角色、菜单）的所有对外提供的前端 API 接口及其详细的字段级数据字典说明。

---

## 目录

1. [组织架构模块 (Org)](#一-组织架构模块-org)
   - [子公司管理 (SubCompany)](#1-子公司管理-subcompany)
   - [部门管理 (Dept)](#2-部门管理-dept)
   - [小组管理 (Team)](#3-小组管理-team)
2. [RBAC 权限管理模块 (RBAC)](#二-rbac-权限管理模块-rbac)
   - [员工管理 (Employee)](#1-员工管理-employee)
   - [角色管理 (Role)](#2-角色管理-role)
   - [平台菜单管理 (Menu)](#3-平台菜单管理-menu)
   - [用户菜单权限 (UserMenu)](#4-用户菜单权限-usermenu)
3. [通用数据结构字典](#三-通用数据结构字典)

---

## 一、 组织架构模块 (Org)

### 1. 子公司管理 (SubCompany)

**基础路径:** `/api/governance/sub-company`

#### 1.1 新增子公司

- **接口地址**: `POST /api/governance/sub-company/add`
- **权限标识**: `org:sub-company:add`
- **业务描述**: 创建一个新的子公司层级，并可指定该公司的管理员。

**请求字段 (SubCompanyAddRequest):**

| 字段名           | 类型       | 必填 | 长度限制 | 描述                                     |
| :--------------- | :--------- | :--- | :------- | :--------------------------------------- |
| `name`           | String     | 是   | 最大 50  | 公司名称                                 |
| `sort`           | Integer    | 是   | -        | 排序值（数字越小越靠前）                 |
| `managerUserIds` | List<Long> | 否   | -        | 管理员用户 ID 列表（分配该公司的管理员） |

**响应结果:** `ApiResponse<Void>` (成功时 `code` 为 0)

#### 1.2 修改子公司

- **接口地址**: `POST /api/governance/sub-company/update`
- **权限标识**: `org:sub-company:update`, `org:sub-company:add`
- **业务描述**: 修改现有子公司的信息，包括名称、排序及管理员的变更。

**请求字段 (SubCompanyUpdateRequest):**

| 字段名           | 类型       | 必填 | 长度限制 | 描述                |
| :--------------- | :--------- | :--- | :------- | :------------------ |
| `id`             | Long       | 是   | -        | 需要修改的子公司 ID |
| `name`           | String     | 是   | 最大 50  | 公司名称            |
| `sort`           | Integer    | 是   | -        | 排序值              |
| `managerUserIds` | List<Long> | 否   | -        | 管理员用户 ID 列表  |

**响应结果:** `ApiResponse<Void>`

#### 1.3 删除子公司

- **接口地址**: `POST /api/governance/sub-company/delete`
- **权限标识**: `org:sub-company:delete`
- **业务描述**: 逻辑删除或物理删除指定的子公司。

**请求字段 (IdRequest):** | 字段名 | 类型 | 必填 | 描述 | | :--- | :--- | :--- | :--- | | `id` | Long | 是 | 目标子公司 ID |

**响应结果:** `ApiResponse<Void>`

#### 1.4 分页查询子公司列表

- **接口地址**: `POST /api/governance/sub-company/page`
- **权限标识**: `org:sub-company:list`
- **业务描述**: 获取子公司列表，支持名称模糊搜索及分页。

**请求字段 (SubCompanyPageQueryRequest):**

| 字段名  | 类型    | 必填 | 描述                       |
| :------ | :------ | :--- | :------------------------- |
| `page`  | Integer | 否   | 当前页码 (默认 1)          |
| `limit` | Integer | 否   | 每页条数 (默认 10)         |
| `name`  | String  | 否   | 公司名称（模糊搜索关键字） |

**响应字段 (ApiResponse<PageData<SubCompanyResponse>>):**

| 字段名                    | 类型     | 描述                                                    |
| :------------------------ | :------- | :------------------------------------------------------ |
| `total`                   | Long     | 总记录数                                                |
| `list`                    | List     | 列表数据                                                |
| `list[].id`               | Long     | 子公司 ID                                               |
| `list[].name`             | String   | 公司名称                                                |
| `list[].sort`             | Integer  | 排序值                                                  |
| `list[].createDate`       | DateTime | 创建时间 (yyyy-MM-dd HH:mm:ss)                          |
| `list[].updateDate`       | DateTime | 最后更新时间 (yyyy-MM-dd HH:mm:ss)                      |
| `list[].roomCount`        | Integer  | 关联的直播间数量                                        |
| `list[].managerUserInfos` | List     | 管理员信息列表（见[通用数据结构](#employee-base-info)） |

---

### 2. 部门管理 (Dept)

**基础路径:** `/api/governance/dept`

#### 2.1 新增部门

- **接口地址**: `POST /api/governance/dept/add`
- **权限标识**: `org:dept:add`

**请求字段 (DeptAddRequest):**

| 字段名           | 类型       | 必填 | 长度限制 | 描述                   |
| :--------------- | :--------- | :--- | :------- | :--------------------- |
| `companyId`      | Long       | 是   | -        | 归属的子公司 ID        |
| `name`           | String     | 是   | 最大 40  | 部门名称               |
| `sort`           | Integer    | 是   | -        | 排序值                 |
| `managerUserIds` | List<Long> | 否   | -        | 部门管理员用户 ID 列表 |

**响应结果:** `ApiResponse<Void>`

#### 2.2 修改部门

- **接口地址**: `POST /api/governance/dept/update`
- **权限标识**: `org:dept:update`, `org:dept:add`

**请求字段 (DeptUpdateRequest):**

| 字段名           | 类型       | 必填 | 长度限制 | 描述                   |
| :--------------- | :--------- | :--- | :------- | :--------------------- |
| `id`             | Long       | 是   | -        | 部门 ID                |
| `companyId`      | Long       | 是   | -        | 归属的子公司 ID        |
| `name`           | String     | 是   | 最大 40  | 部门名称               |
| `sort`           | Integer    | 是   | -        | 排序值                 |
| `managerUserIds` | List<Long> | 否   | -        | 部门管理员用户 ID 列表 |

**响应结果:** `ApiResponse<Void>`

#### 2.3 删除部门

- **接口地址**: `POST /api/governance/dept/delete`
- **权限标识**: `org:dept:delete`
- **请求字段**: `IdRequest` (包含 `id`)
- **响应结果**: `ApiResponse<Void>`

#### 2.4 分页查询部门

- **接口地址**: `POST /api/governance/dept/page`
- **权限标识**: `org:dept:list`

**请求字段 (DeptPageQueryRequest):**

| 字段名      | 类型    | 必填 | 描述             |
| :---------- | :------ | :--- | :--------------- |
| `page`      | Integer | 否   | 页码             |
| `limit`     | Integer | 否   | 条数             |
| `name`      | String  | 否   | 部门名称模糊查询 |
| `companyId` | Long    | 否   | 按子公司 ID 过滤 |

**响应字段 (ApiResponse<PageData<DeptResponse>>):**

| 字段名                    | 类型    | 描述           |
| :------------------------ | :------ | :------------- |
| `list[].id`               | Long    | 部门 ID        |
| `list[].companyId`        | Long    | 归属公司 ID    |
| `list[].companyName`      | String  | 归属公司名称   |
| `list[].name`             | String  | 部门名称       |
| `list[].sort`             | Integer | 排序值         |
| `list[].roomCount`        | Integer | 直播间数量     |
| `list[].managerUserInfos` | List    | 管理员信息列表 |

---

### 3. 小组管理 (Team)

**基础路径:** `/api/governance/team`

#### 3.1 新增小组

- **接口地址**: `POST /api/governance/team/add`
- **权限标识**: `org:team:add`

**请求字段 (TeamAddRequest):** | 字段名 | 类型 | 必填 | 长度限制 | 描述 | | :--- | :--- | :--- | :--- | :--- | | `deptId` | Long | 是 | - | 归属的部门 ID | | `name` | String | 是 | 最大 30 | 小组名称 | | `sort` | Integer | 是 | - | 排序值 | | `managerUserIds` | List<Long> | 否 | - | 小组管理员用户 ID 列表 |

#### 3.2 修改小组

- **接口地址**: `POST /api/governance/team/update`
- **权限标识**: `org:team:update`, `org:team:add`

**请求字段 (TeamUpdateRequest):**

| 字段名           | 类型       | 必填 | 长度限制 | 描述                   |
| :--------------- | :--------- | :--- | :------- | :--------------------- |
| `id`             | Long       | 是   | -        | 小组 ID                |
| `deptId`         | Long       | 是   | -        | 归属的部门 ID          |
| `name`           | String     | 是   | 最大 30  | 小组名称               |
| `sort`           | Integer    | 是   | -        | 排序值                 |
| `managerUserIds` | List<Long> | 否   | -        | 小组管理员用户 ID 列表 |

#### 3.3 删除小组

- **接口地址**: `POST /api/governance/team/delete`
- **权限标识**: `org:team:delete`
- **请求字段**: `IdRequest` (包含 `id`)

#### 3.4 分页查询小组

- **接口地址**: `POST /api/governance/team/page`
- **权限标识**: `org:team:list`

**请求字段 (TeamPageQueryRequest):**

| 字段名   | 类型    | 必填 | 描述           |
| :------- | :------ | :--- | :------------- |
| `page`   | Integer | 否   | 页码           |
| `limit`  | Integer | 否   | 条数           |
| `name`   | String  | 否   | 小组名称关键字 |
| `deptId` | Long    | 否   | 按部门 ID 过滤 |

**响应字段 (ApiResponse<PageData<TeamResponse>>):**

| 字段名               | 类型    | 描述     |
| :------------------- | :------ | :------- |
| `list[].id`          | Long    | 小组 ID  |
| `list[].companyId`   | Long    | 公司 ID  |
| `list[].companyName` | String  | 公司名称 |
| `list[].deptId`      | Long    | 部门 ID  |
| `list[].deptName`    | String  | 部门名称 |
| `list[].name`        | String  | 小组名称 |
| `list[].sort`        | Integer | 排序值   |

---

## 二、 RBAC 权限管理模块 (RBAC)

### 1. 员工管理 (Employee)

**基础路径:** `/api/governance/employee`

#### 1.1 新增员工

- **接口地址**: `POST /api/governance/employee/add`
- **权限标识**: `sys:employee:add`

**请求字段:**

| 字段名          | 类型    | 必填 | 描述                   |
| :-------------- | :------ | :--- | :--------------------- |
| `name`          | String  | 是   | 姓名 (最大20)          |
| `userAvatar`    | String  | 否   | 头像 URL               |
| `staffNumber`   | String  | 是   | 工号 (最大10)          |
| `mobile`        | String  | 是   | 手机号 (最大20)        |
| `email`         | String  | 否   | 邮箱                   |
| `companyId`     | Long    | 是   | 所属公司 ID            |
| `deptId`        | Long    | 否   | 所属部门 ID (默认 0)   |
| `teamId`        | Long    | 否   | 所属小组 ID (默认 0)   |
| `positionId`    | Long    | 否   | 岗位 ID (默认 0)       |
| `roleId`        | Long    | 是   | 角色 ID                |
| `onRec`         | Boolean | 否   | 是否开启录制权限       |
| `jobType`       | Integer | 否   | 就职类型: 1全职, 2兼职 |
| `accountStatus` | Integer | 否   | 账号状态: 0停用, 1正常 |

#### 1.2 修改员工

- **接口地址**: `POST /api/governance/employee/update`
- **权限标识**: `sys:employee:update`, `sys:employee:add`
- **请求字段**: 同新增员工，额外增加 `id` (Long, 必填)。

#### 1.3 员工详情

- **接口地址**: `GET /api/governance/employee/detail`
- **权限标识**: `sys:employee:list`
- **请求参数**: `id` (Long, Query参数)
- **响应字段**: 返回 `EmployeeInfoResponse` (结构见下方查询列表中的单个对象结构)。

#### 1.4 分页查询员工

- **接口地址**: `POST /api/governance/employee/list`
- **权限标识**: `sys:employee:list`

**请求字段 (EmployeeQueryRequest):**

| 字段名          | 类型    | 必填 | 描述                             |
| :-------------- | :------ | :--- | :------------------------------- |
| `keyword`       | String  | 否   | 搜索关键字（姓名、工号或手机号） |
| `companyIds`    | List    | 否   | 数据权限：限制公司范围           |
| `deptIds`       | List    | 否   | 数据权限：限制部门范围           |
| `teamIds`       | List    | 否   | 数据权限：限制小组范围           |
| `roleId`        | Long    | 否   | 按角色过滤                       |
| `accountStatus` | Integer | 否   | 按状态过滤                       |

**响应字段 (EmployeeInfoResponse):** | 字段名 | 类型 | 描述 | | :--- | :--- | :--- | | `id` | Long | 员工 ID | | `name` | String | 姓名 | | `userAvatar` | String | 头像 URL | | `staffNumber` | String | 工号 | | `mobile` | String | 手机号 | | `email` | String | 邮箱 | | `companyId` / `companyName`| Long/String| 所属公司 ID/名称 | | `deptId` / `deptName` | Long/String| 所属部门 ID/名称 | | `teamId` / `teamName` | Long/String| 所属小组 ID/名称 | | `positionId` / `positionName`| Long/String| 岗位 ID/名称 | | `roleId` / `roleName` | Long/String| 角色 ID/名称 | | `onRec` | Boolean| 是否允许录制 | | `jobType` | Integer| 就职类型(1全职 2兼职) | | `accountStatus`| Integer| 状态(0停用 1正常) |

#### 1.5 员工状态/权限快捷操作

- **开启录制**: `POST /api/governance/employee/on-rec` (参数: `id`)
- **关闭录制**: `POST /api/governance/employee/off-rec` (参数: `id`)
- **启用账号**: `POST /api/governance/employee/enable` (参数: `id`)
- **停用账号**: `POST /api/governance/employee/disable` (参数: `id`)

#### 1.6 员工下拉选项接口 (用于搜索框等)

- **接口地址**: `GET /api/governance/api/governance/employee/option`
- **请求参数**: `keyword`, `companyId`, `deptId`, `teamId`, `positionId`
- **响应格式**: `List<LabelOption>` (见通用数据结构)

---

### 2. 角色管理 (Role)

**基础路径:** `/api/governance/role`

#### 2.1 新增/修改角色

- **新增地址**: `POST /api/governance/role/add`
- **修改地址**: `POST /api/governance/role/update` (多传 `id`)

**请求字段:**

| 字段名    | 类型       | 必填 | 描述                                  |
| :-------- | :--------- | :--- | :------------------------------------ |
| `name`    | String     | 是   | 角色名称 (最大 100 字符)              |
| `menuIds` | List<Long> | 否   | 分配的菜单权限 ID 列表 (最大 3000 项) |

#### 2.2 分配角色菜单

- **接口地址**: `POST /api/governance/role/assign-menus`
- **请求字段**: `roleId` (Long), `menuIds` (List<Long>)

#### 2.3 获取角色详情

- **接口地址**: `GET /api/governance/role/detail`
- **请求参数**: `id` (Long, 角色ID)
- **响应字段 (RoleDetailResponse)**:
  - `roleId` (Long)
  - `name` (String)
  - `menuTree` (List<MenuTreeResponse>): 当前角色拥有的完整菜单树

#### 2.4 分页查询角色

- **接口地址**: `POST /api/governance/role/list`
- **请求参数**: `name` (角色名称模糊搜索), `page`, `limit`
- **响应列表字段 (RoleResponse)**: `id`, `name`, `createDate`, `updateDate`, `userCount`(该角色下关联的用户数)。

---

### 3. 平台菜单管理 (Menu)

**基础路径:** `/api/governance/menu`

#### 3.1 新增/修改菜单

- **新增地址**: `POST /api/governance/menu/add`
- **修改地址**: `POST /api/governance/menu/update` (多传 `id`)

**请求字段:**

| 字段名           | 类型    | 必填 | 描述                              |
| :--------------- | :------ | :--- | :-------------------------------- |
| `parentId`       | Long    | 是   | 父级 ID (根节点传 0)              |
| `name`           | String  | 是   | 菜单名称 (最大 50)                |
| `url`            | String  | 否   | 前端路由/链接 (最大 100)          |
| `type`           | Integer | 是   | 类型枚举: 0菜单, 1功能按钮, 2目录 |
| `sort`           | Integer | 是   | 排序值                            |
| `img`            | String  | 否   | 菜单图标                          |
| `permissionCode` | String  | 否   | 权限标识码 (如 sys:role:add)      |

#### 3.2 删除菜单

- **接口地址**: `POST /api/governance/menu/delete`

#### 3.3 获取完整菜单树 (后台配置用)

- **接口地址**: `GET /api/governance/menu/tree`
- **响应字段**: `List<MenuTreeResponse>` (树形结构，见通用数据结构)

---

### 4. 用户菜单权限 (UserMenu)

**基础路径:** `/api/governance/user-menu`

#### 4.1 获取当前登录用户的菜单树

- **接口地址**: `GET /api/governance/user-menu/tree`
- **业务描述**: 供前端页面初始化时拉取当前登录用户拥有的动态侧边栏路由和按钮权限。
- **响应字段**: `List<MenuTreeResponse>`

---

## 三、 通用数据结构字典

<a name="employee-base-info"></a>

#### 1. EmployeeBaseInfo (员工简要信息)

用于组织架构返回管理员信息的复用结构。

| 字段名        | 类型   | 描述     |
| :------------ | :----- | :------- |
| `id`          | Long   | 用户 ID  |
| `name`        | String | 姓名     |
| `userAvatar`  | String | 头像 URL |
| `staffNumber` | String | 工号     |
| `mobile`      | String | 手机号   |

#### 2. MenuTreeResponse (菜单树节点)

| 字段名           | 类型    | 描述                        |
| :--------------- | :------ | :-------------------------- |
| `id`             | Long    | 菜单节点 ID                 |
| `parentId`       | Long    | 父级 ID                     |
| `name`           | String  | 菜单名称                    |
| `url`            | String  | 路由路径                    |
| `type`           | Integer | 0:菜单, 1:按钮/功能, 2:目录 |
| `sort`           | Integer | 排序                        |
| `img`            | String  | 图标配置                    |
| `permissionCode` | String  | 权限码                      |
| `children`       | List    | 子节点列表 (递归嵌套同结构) |

#### 3. LabelOption (前端下拉框通用)

| 字段名  | 类型   | 描述                 |
| :------ | :----- | :------------------- |
| `key`   | Long   | 实际的值 (通常为 ID) |
| `label` | String | 用于展示的文本       |
