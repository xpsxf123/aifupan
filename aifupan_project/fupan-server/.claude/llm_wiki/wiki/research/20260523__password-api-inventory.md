# Research — 修改密码 / 重置密码 API 入口清单

**Date:** 2026-05-23
**Question source:** 用户口述（branch: `hotfix/260523-2.6.0-user-password`）
**Status:** 调研完成

## 1. 问题陈述

> 找到修改密码与重置密码的API入口给我 列一个清单

## 2. 现状基线 — API 清单

全部位于 `replay-api`，单一 Controller：`UserController`（base path `replay/user`）。

| # | 方法 | 完整路径 | 入参 | 出参 | 调用方 | 校验要点 |
|---|---|---|---|---|---|---|
| 1 | POST | `/replay/user/updatePassword` | `UpdatePasswordBo`（body）：`password`(原)/`newPassword`/`checkPassword` | `R<String>` | **后台登录态用户自助改密**（管理端） | 新+二次一致；原密码 MD5 比对 |
| 2 | POST | `/replay/user/updatePasswordByClient` | `UpdatePasswordByClientBo`（body）：`code`(短信)/`newPassword`/`checkPassword` | `R<String>` | **客户端登录态用户自助改密**（C 端） | 新+二次一致；Redis 短信验证码比对（key = `PowerProperties.phoneCodeRedisKey + 手机号`） |
| 3 | GET | `/replay/user/resetPassword?id=` | `id: Long`（query，必填） | `R<String>`（返回新密码明文） | **管理员重置他人密码**（默认密码方案） | 用户存在性；密码由 `RandomStringGenerator.generateRandomString()` 生成 |
| 4 | GET | `/replay/user/resetRandomPassword?id=` | `id: Long`（query，必填） | `R<String>`（返回新密码明文） | **管理员重置他人密码**（随机 6 位数方案） | 用户存在性；密码由 `SecureRandom` 生成，首位 1-9、后 5 位 0-9 |

### 入口源码定位

| API | Controller | Logic | BLL | BO |
|---|---|---|---|---|
| `updatePassword` | `replay-api/.../controller/power/UserController.java:236` | `UserLogicImpl.java:218` | `UserBll.java:285` | `replay-power/.../bo/UpdatePasswordBo.java` |
| `updatePasswordByClient` | `UserController.java:223` | `UserLogicImpl.java:1975` | `UserBll.java:1166` | `UpdatePasswordByClientBo.java` |
| `resetPassword` | `UserController.java:318` | `UserLogicImpl.java:230` | `UserBll.java:318` | —（仅 `id` 参数） |
| `resetRandomPassword` | `UserController.java:331` | `UserLogicImpl.java:2019` | `UserBll.java:1367` | —（仅 `id` 参数） |

### 共用下游

- 写库统一走 `UserProducer.updatePassword(id, newPassword)`（`UpdatePassword#3` & `#1` & `#2`），或 `UserProducer.resetRandomPassword(id, password)`（仅 `#4`，带回 boolean）。
- 入库前是否做 MD5 加密由 `UserProducer` 实现侧负责（本 Controller 层 / BLL 层均传明文）。
- `Constant.CodeMsgEnum.TIP_CUSTOM` 为统一业务错误码。

## 3. Gap 分析

| 维度 | 现状 | 缺什么 / 风险 | 工作量 |
|---|---|---|---|
| 找回密码（未登录态、忘记原密码） | **不存在** —— 全库 grep 无 `forgetPassword / 忘记密码 / 找回密码` 等关键词 | 仅有"管理员重置 + 短信改密"两条路径，无 C 端"忘记密码自助找回"流程；若产品有此需求需新增 | M |
| `resetPassword` / `resetRandomPassword` 鉴权 | 仅依赖 Controller 上层全局拦截器（未在方法内显式校验"调用人是否有管理员权限"） | 是否存在越权重置他人密码风险，需读 `LoginInterceptor` / RBAC 注解链路确认 | S（验证）/ M（如需补） |
| 密码强度策略 | BLL 仅做"两次一致 / 原密码正确 / 短信码正确"校验，**未发现长度/字符复杂度校验** | 弱密码可被设置（如 "1"）；`resetRandomPassword` 第一位强制 1-9 防 0 开头，但其他位允许全相同 | S |
| 返回明文密码 | `resetPassword` / `resetRandomPassword` 在 `R<String>` 的 `data` 字段返回**明文新密码** | 若日志/网关捕获响应体即泄露；CLAUDE.md §2 硬约束"敏感字段不入日志"需确认下游链路 | S（核查）|
| MD5 加密强度 | `updatePassword` 用 `MD5Utils.md5(...)` 比对原密码 | MD5 已不抗碰撞，行业标准推荐 BCrypt / Argon2；属技术债 | L（重构需平滑迁移） |
| 双写一致 / 多账号 | 单点写 `tb_user.password` | 未发现 cache / token 失效逻辑：改完密码"请重新登录"仅靠客户端自觉，未强制把已签发 token 拉黑 | M（如需强制下线） |
| OpenAPI（governance） | `OpenGovernanceUserDetailsInfo.password` 仅作为响应字段返回 | 与本次调研无关，但需确认是否为外部接口意外暴露密码字段 | S（核查） |

## 4. 推荐路径

| 优先级 | 建议 | 说明 |
|---|---|---|
| **P0 - 阻断风险** | 核查 `resetPassword` / `resetRandomPassword` 调用人权限 | 若任何登录用户都能传 `id` 重置他人密码 → 严重越权；branch 名 `2.6.0-user-password` 提示这可能是当前 hotfix 目标 |
| **P0 - 阻断风险** | 核查 `R.ok` 返回的明文新密码是否被日志/链路追踪/网关访问日志记录 | 落日志即泄露 |
| **P1** | 补密码强度校验（长度 ≥ 8、含字母+数字等）；建议加在 `UserProducer.updatePassword` 入口处统一拦截 | 一次改，4 条入口都受益 |
| **P1** | 加 token 失效：改密成功后把当前用户在 Redis 的 token 主动剔除 | 改密文案已提示"请重新登录"，但实际生效需强制 |
| **P2** | 评估 MD5 → BCrypt 迁移方案 | 重构成本高，需双写过渡，留待专门 EPIC |
| **P2** | 评估是否需要补"忘记密码"自助流程（手机号 + 短信码免登重置） | 取决于产品决定，非本次范围 |

## 5. 待澄清（≤5 个高杠杆问题）

1. 本次 hotfix（`hotfix/260523-2.6.0-user-password`）要解决的具体痛点是哪一条？越权重置 / 返回明文密码 / 改密后 token 不失效 / 弱密码？
2. `resetPassword` / `resetRandomPassword` 的预期调用方是哪种角色（仅超管？租户管理员？）—— 决定补哪种鉴权。
3. 是否需要新增"客户端忘记密码自助找回"接口，还是认为现有 `updatePasswordByClient` 已覆盖？（后者要求用户处于登录态，不能找回）
4. 密码强度策略以谁为准（产品 / 安全 / 监管）？是否需立刻补，还是先解决 P0？
5. MD5 → BCrypt 是否纳入本 hotfix 范围？（推荐否，建议另起 EPIC）

## Source Material

- Wiki:
  - `.claude/llm_wiki/wiki/api/power_api.md`（已记录 4 条入口的概览表，但缺校验/调用方拆分）
- 代码 grep 验证关键路径：
  - `replay-api/src/main/java/com/jiuyu/replay/api/controller/power/UserController.java:218-336`
  - `replay-api/src/main/java/com/jiuyu/replay/api/logic/power/impl/UserLogicImpl.java:218-232, 1975-2020`
  - `replay-power/src/main/java/com/jiuyu/replay/power/bll/UserBll.java:285-329, 1166-1184, 1367-1391`
  - `replay-power/src/main/java/com/jiuyu/replay/power/bo/UpdatePasswordBo.java`
  - `replay-power/src/main/java/com/jiuyu/replay/power/bo/UpdatePasswordByClientBo.java`
- 全库 grep：`forgetPassword / forgotPassword / 忘记密码 / 找回密码 / retrievePassword` 全部 0 命中（确认无此入口）
