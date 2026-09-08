---
name: "error-code-standard"
description: "异常状态码与契约设计规范：统一ApiResponse格式、BizErrorCode复用策略。编写抛出BusinessException或返回ApiResponse.failed的代码前必须调用。"
---

# Error Code Standard (异常状态码与契约设计规范)

This project uses a highly abstracted, behavior-driven Error Code system and a Unified API Response format. We do **NOT** create a new enum for every specific error message. Instead, we reuse abstract codes and override the `message` dynamically.

## 1. 统一 API Response 格式 (Unified API Response Format)

All interactions between the frontend and backend must strictly adhere to a Unified Response format.
- **读操作**：Controllers must wrap query results in a unified success response.
- **写操作**：Services should ideally return the unified response structure directly, allowing validation failures to return a failure response cleanly without always throwing exceptions.

## 2. 错误场景分析 (Analyze the Error Scenario)

Ask yourself: What type of error is this?

| 场景 | 枚举 | 范围 |
|-----|------|-----|
| 系统/基础设施错误（404 Not Found, 401 Unauthorized） | `SystemErrorCode` | 0~999 |
| 技术故障（外部 API 失败） | `BizErrorCode` | 1xxx |
| 硬规则/校验失败（空参数、重复数据） | `BizErrorCode` | 2xxx |
| 业务逻辑/状态失败（配额超限、依赖存在） | `BizErrorCode` | 3xxx |
| 前端需特殊处理（重定向、强弹窗） | `BizErrorCode` | 4xxx |

## 3. 常用可复用映射 (Common Reusable Mappings)

**Always try to reuse an existing code by overriding the message.**

- **参数校验（空、格式错、超限）**：
  - Code: `BizErrorCode.PARAM_INVALID` (2001)
  - `new BusinessException(BizErrorCode.PARAM_INVALID, "手机号格式不正确")`

- **数据重复（名称已存在、手机号已存在）**：
  - Code: `BizErrorCode.DATA_DUPLICATED` (2002)
  - `new BusinessException(BizErrorCode.DATA_DUPLICATED, "部门名称已存在")`

- **依赖存在（因存在子项而无法删除）**：
  - Code: `BizErrorCode.HAS_DEPENDENCY` (3003)
  - `new BusinessException(BizErrorCode.HAS_DEPENDENCY, "该部门下存在员工，不允许删除")`

- **状态不符（已开始/已结束无法修改）**：
  - Code: `BizErrorCode.INVALID_STATE` (3002)
  - `new BusinessException(BizErrorCode.INVALID_STATE, "排班已开始，无法修改")`

- **资源配额（达到上限）**：
  - Code: `BizErrorCode.QUOTA_EXCEEDED` (3001)
  - `new BusinessException(BizErrorCode.QUOTA_EXCEEDED, "公司数量已达上限")`

- **频率限制（操作过于频繁）**：
  - Code: `BizErrorCode.RATE_LIMIT` (2003)
  - `new BusinessException(BizErrorCode.RATE_LIMIT, "请勿频繁发送验证码")`

## 4. 何时创建新错误码 (When to Create a NEW Code)

**Rare!** You should ONLY create a new code in `BizErrorCode.java` if:
1. The error requires a **completely new frontend interaction** (e.g., 4xxx series).
2. The error represents a **completely new abstract category** of business failure that cannot be grouped under existing codes.

## 5. 代码生成规则 (Code Generation Rules)

### 抛出异常

```java
// Good: 复用抽象码 + 动态消息
throw new BusinessException(BizErrorCode.DATA_DUPLICATED, "角色名称已存在");

// Bad: 裸消息（默认走 3000）
throw new BusinessException("角色名称已存在");

// Bad: 为每个具体场景创建新枚举值
```

### 返回 ApiResponse.failed

```java
// Good
return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "结束时间不能早于开始时间");

// Bad: 硬编码数字
return ApiResponse.failed(2001, "结束时间不能早于开始时间");
```
