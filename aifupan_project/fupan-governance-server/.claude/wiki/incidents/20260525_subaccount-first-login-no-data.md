# Incident: 子账号首次登录看不到数据

## 基本信息

| 字段 | 值 |
|------|-----|
| 日期 | 2026-05-25 |
| 严重级别 | P2（功能降级） |
| 环境 | 生产 |
| 来源 | 手动报告 |
| 状态 | resolved |

## 现象

子账号（已挂管理员角色）首次登录企业后台时看不到任何数据。租户管理员登录一次后，子账号再登录恢复正常。

## 复现条件

1. 租户从未有任何账户登录过企业后台
2. 子账号（userType=2）作为首个登录者进入
3. 数据库中租户数据已初始化完成
4. 子账号登录后界面无数据展示

## 根因

`EmployeeOauthManage.doLoginInitMainAccount()` → `EmployeeServiceImpl.addForReplayAccount()` 在创建/更新子账号员工记录时，即使已分配默认管理员角色，也始终设置 `holdTenant = false`。导致 `doLogin()` 生成的 token 中 `TENANT_ADMIN = "false"`，数据权限处理器 `DataPermissionsHandler` 将用户视为非管理员，查询 `ManagerConnector` 表（新用户无记录），返回空数据权限范围。

而二次登录走 `getMobileLoginAccount()` 路径，该方法在 commit `9619e60` 中已添加了 `holdTenant` 提升逻辑——检查员工是否拥有默认管理员角色，若拥有则设置 `holdTenant = true`。这就是为什么管理员先登录后子账号恢复正常的根本原因。

## 修复

在 `EmployeeServiceImpl.addForReplayAccount()` 中，两个分支（已存在员工更新绑定 / 新建员工）均在分配默认角色后，增加 `holdTenant` 提升逻辑，与 `getMobileLoginAccount()` 保持一致。

## 影响范围

- 所有"租户首个登录者为子账号"的场景
- 不影响主账户首次登录（主账户 `userType=0` 的 `holdTenant` 由其他路径设置）

## 关联文件

- `src/main/java/com/jiuyu/governance/business/rbac/service/impl/EmployeeOauthManage.java` (L144-176)
- `src/main/java/com/jiuyu/governance/business/rbac/service/impl/EmployeeServiceImpl.java` (L499-560, L1299-1318)
- `src/main/java/com/jiuyu/governance/plugins/oauth/data/supports/DataPermissionsHandler.java`

## 提醒未来 LLM

当修改登录初始化流程（`doLoginInitMainAccount` / `addForReplayAccount`）时，必须确保新创建的员工 `holdTenant` 字段与实际角色权限保持一致。`getMobileLoginAccount()` 中已有的 `holdTenant` 提升逻辑（检查默认角色）是参考实现，任何新的员工创建路径都需要复用此检查，不能只在一处生效。
