---
name: "annotation-usage-standard"
description: "核心注解使用规范：@ResourceLock（分布式锁）、@Permissions（功能权限）、@BeforePermission（数据权限）、@GovernanceUser/@SystemUser（身份验证）。编写 Controller/Service 方法时调用。"
---

# Annotation Usage Standard (核心注解使用规范)

This skill dictates the usage of the project's custom annotations for concurrency control, authentication, and authorization.

## 1. 接口并发防抖防重 (@ResourceLock)

**场景**：Controller 层的所有写操作（POST / PUT / DELETE，如 `/add`, `/update`, `/delete`, `/enable` 等）。

**规则**：必须添加 `@ResourceLock` 注解，利用 Redis 分布式锁防止用户频繁点击产生脏数据。

- **参数**：
  - `prefix`: 锁前缀，通常为 `模块名:业务名` (如 `governance:employee`)。
  - `key`: SpEL 表达式，基于请求参数动态生成锁的 key (如 `#request.id` 或 `#request.mobile`)。
  - `message`: 触发拦截时的提示语，通常为 `"请勿频繁点击"`。

- **示例**：
  ```java
  @ResourceLock(prefix = "governance:employee", key = "#request.mobile", message = "请勿频繁点击")
  @PostMapping("/add")
  public ApiResponse<Void> add(@RequestBody EmployeeAddRequest request)
  ```

## 2. 用户登录身份验证 (@GovernanceUser / @SystemUser)

**场景**：Controller 类的头部。

**规则**：
- 企业端（To B）的 Controller 必须加上 `@GovernanceUser` 注解。
- 运营后台（To A）的 Controller 必须加上 `@SystemUser` 注解。
- 这个注解不仅拦截未登录请求，还会将当前用户信息注入到方法的 `AccessUser` 参数中。

## 3. 功能操作权限验证 (@Permissions)

**场景**：Controller 层的方法上。

**规则**：验证当前登录用户是否拥有点击该按钮/访问该接口的菜单权限码。

- **示例**：
  ```java
  @Permissions("org:dept:add") // 对应 Menu 表的权限标识
  @PostMapping("/add")
  ```
- **多权限**：支持数组 `@Permissions({"org:dept:add", "org:dept:update"})`。

## 4. 细粒度数据权限验证 (@BeforePermission)

**场景**：Controller 或 Service 层的方法上。用于校验用户是否有权操作传入的 `id` 对应的数据。

**规则**：这是**数据隔离**的最后一道防线。

- **参数**：
  - `type`: 权限维度，必须使用 `OauthConstant` 中的常量（如 `OauthConstant.DEPT`）。
  - `dataId`: SpEL 表达式，从入参提取需要校验的 ID（如 `#request.id` 或 `#id`）。

- **单维度校验示例**：
  ```java
  @BeforePermission(type = OauthConstant.TEAM, dataId = "#request.id")
  public ApiResponse<Void> updateTeam(TeamUpdateRequest request)
  ```

- **多维度联合校验示例**（常用于新增操作，校验父级节点权限）：
  ```java
  @BeforePermission.Multiple({
      @BeforePermission(type = OauthConstant.COMPANY, dataId = "#request.companyId"),
      @BeforePermission(type = OauthConstant.DEPT, dataId = "#request.deptId", ignoreEmpty = true)
  })
  public ApiResponse<Void> addEmployee(EmployeeAddRequest request)
  ```

- **注意**：对于分页查询（`/page`），不要使用此注解，而是通过 `DataPermissionsPageRequest` 在 MyBatis 的 `lambdaQuery()` 中使用 `.in()` 进行数据过滤。

## 关联技能

- [java-data-permissions](../java-data-permissions/SKILL.md): 数据权限的完整指南（操作验证 vs 列表过滤）。
- [java-backend-api-standard](../java-backend-api-standard/SKILL.md): API 设计标准，含 @ResourceLock 的强制使用场景。
