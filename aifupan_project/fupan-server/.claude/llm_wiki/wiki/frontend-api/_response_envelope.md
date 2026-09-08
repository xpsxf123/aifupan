# 统一响应封装 R\<T\>

后端所有接口返回 JSON 对象，外层封装：

```json
{
  "code": 0,
  "msg": "success",
  "data": <T>
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| code | number | 0 = 成功；其它见 [_error_codes.md](./_error_codes.md) |
| msg | string | 描述信息；成功时通常为 "success" |
| data | T | 业务数据；类型见每个接口的"响应 data 结构" |

## 前端拆包约定（参考）

```typescript
async function call<T>(url: string, options?: RequestInit): Promise<T> {
  const r = await fetch(url, options).then(r => r.json());
  if (r.code !== 0) throw new ApiError(r.code, r.msg);
  return r.data as T;
}

class ApiError extends Error {
  constructor(public code: number, message: string) {
    super(message);
  }
}
```

## PageUtils\<T\>

列表查询的 data 字段统一为：

| 字段 | 类型 | 说明 |
|---|---|---|
| total | number | 总条数 |
| list | T[] | 当前页数据 |
| pageNum | number | 当前页（从 1 起）|
| pageSize | number | 每页条数 |

## 字段类型约定

| 后端类型 | JSON 展示类型 | 说明 |
|---|---|---|
| Long（Snowflake ID）| string | 19 位 ID 超 JS Number 安全范围，必须字符串解析，**不可用 number 接收** |
| Long（普通计数）| number | 若可能超 2^53 也改 string |
| Integer | number | |
| String | string | |
| Boolean | boolean | |
| LocalDateTime | string | ISO-8601 `YYYY-MM-DDTHH:mm:ss`，无时区，按 Asia/Shanghai 解读 |
| LocalDate | string | `YYYY-MM-DD` |
| Date（java.util.Date）| string | ISO-8601 格式，无时区，按 Asia/Shanghai 解读 |
| BigDecimal | string | 高精度金额，前端用 decimal.js 解析 |
| Enum | string（枚举 name）| 各接口文档内列出枚举值与中文释义 |
| `List<T>` / `Set<T>` | T[] | |
| `Map<String, Object>` | `Record<string, unknown>` | 动态字段，结构见各接口业务说明 |

> **Snowflake ID 说明**：项目所有主键均使用雪花算法生成，长度 19 位（如 `1234567890123456789`），超过 JavaScript `Number.MAX_SAFE_INTEGER`（2^53 - 1），前端必须以 `string` 类型接收，禁止用 `number` 或 `parseInt`。
