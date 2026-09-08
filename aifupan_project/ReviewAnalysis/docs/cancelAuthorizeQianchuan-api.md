# 取消千川授权 API

## 基本信息

| 项目 | 内容 |
|---|---|
| 接口路径 | `/api/anchorinfo/cancelAuthorizeQianchuan` |
| 请求方式 | `GET` |
| 功能描述 | 取消主播的千川授权，清除授权状态 |

## 请求参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| secUid | string | 是 | 主播 SecUid |

## 请求示例

```http
GET /api/anchorinfo/cancelAuthorizeQianchuan?secUid=MS4wLjABAAAAxxx
```

## 响应

成功信息

```
{
  "code": 0,
  "msg": "success",
  "data": null
}
```



失败信息

```
{
  "code": 500,
  "msg": "主播不存",
  "data": null
}
```



## 执行流程

1. 从主播缓存中查找主播信息
2. 主播不存在 → 返回 `CustomException("主播信息不存在")`
3. 将千川授权状态置为 `0`（未授权），更新缓存
4. 停止该主播的千川数据采集轮询
5. 删除主播的千川授权缓存版本号
6. 记录操作日志

## 授权状态枚举

前端可通过 `AnchorDto.qianchuanAuthStatus` 获取当前授权状态：

| 值 | 状态 | 说明 |
|---|---|---|
| 0 | 未授权 | 初始状态或已取消授权 |
| 1 | 已授权 | 授权成功，可正常拉取数据 |
| 2 | 授权过期 | Token/凭证失效，需重新授权 |
| 3 | 授权失败 | 授权流程中发生错误 |
| 4 | 授权中 | 正在执行授权流程 |
| 5 | 授权抖音号不匹配 | 当前抖音号与主播不匹配 |

## 异常

| 异常 | 触发条件 |
|---|---|
| `CustomException` | 主播不存在于当前系统缓存 |
| `Exception` | 缓存操作或数据采集停止失败 |

## 注意事项

- 调用此接口后，`qianchuanAuthStatus` 变为 `0`，前端应刷新主播列表或详情以反映最新状态
- 取消千川授权不会影响巨量（百应）的授权状态
