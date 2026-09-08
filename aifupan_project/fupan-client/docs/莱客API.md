# 来客授权接口文档

## 通用说明

- **Base URL**: `http://127.0.0.1:5001/api/anchorinfo`
- **请求方式**: `GET`，参数通过 query string 传递
- **响应格式**: JSON

```json
{
  "code": 0,       // 0=成功，500=业务异常，404=接口不存在
  "msg": "成功",    // 状态描述
  "data": null      // 返回值，void 方法始终为 null
}
```

---

## 1. 授权

```
GET /api/anchorinfo/authorizeLife
```

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| secUid | string | 是 | 主播 SecUid |
| authType | int | 是 | 0 = 登录主账号，1 = 登录子账号 |

**成功响应**:

```json
{"code": 0, "msg": "成功", "data": null}
```

**异常响应**:

| code | msg | 触发条件 |
|------|-----|----------|
| 500 | 请先完成上一次授权 | 当前有另一个授权流程正在进行 |
| 500 | 授权数量不足，请联系产品顾问 | 账号授权配额用完 |
| 500 | 主播信息不存在 | secUid 不在主播缓存中 |
| 500 | 当前主播已经授权，无需重复授权 | 该主播已完成授权 |

---

## 2. 取消授权

```
GET /api/anchorinfo/cancelAuthorizeLife
```

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| secUid | string | 是 | 主播 SecUid |

**成功响应**:

```json
{"code": 0, "msg": "成功", "data": null}
```
