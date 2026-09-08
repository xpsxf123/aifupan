# 安全规则 (Security Rules)

本文档定义项目安全编码规范。所有 AI Agent 在编写或修改代码时 **必须** 遵循这些规则。
规则来源: `code_review_20260423.md` #1

---

## 1. SQL 注入防护

### [SQL] [MUST] 使用参数化查询

所有 SQL 查询 **必须** 使用参数化查询 (`@param`)。**禁止** 将变量通过字符串拼接嵌入 SQL。

```csharp
// ❌ 错误 — 字符串拼接，存在 SQL 注入
string sql = $"SELECT * FROM {tableName} WHERE {whereClause} ORDER BY {orderbyStr}";
cmd.CommandText = sql;

// ✅ 正确 — 参数化查询
string sql = "SELECT * FROM anchor WHERE sec_uid = @secUid AND status = @status";
cmd.Parameters.AddWithValue("@secUid", secUid);
cmd.Parameters.AddWithValue("@status", status);
cmd.CommandText = sql;
```

### [SQL] [MUST] 动态表名/列名白名单校验

如必须使用动态表名或列名，**必须** 通过白名单校验，**禁止** 直接传入原始字符串。

```csharp
// ❌ 错误 — 直接使用传入的表名
string sql = $"INSERT INTO {tableName} ...";

// ✅ 正确 — 白名单校验
private static readonly HashSet<string> AllowedTables = new()
{
    "anchor", "video", "upload_file", "online_num", "audio_analysis"
};

public int Insert(string tableName, ...)
{
    if (!AllowedTables.Contains(tableName))
        throw new ArgumentException($"非法表名: {tableName}");
    // 安全使用
}
```

---

## 2. 敏感数据管理

### [Secret] [NEVER] 禁止硬编码密钥

**禁止** 在源代码中硬编码 API Key、Token、密码或任何密钥。使用环境变量或加密配置文件。

```csharp
// ❌ 错误
string apiKey = "sk-xxxxxxxxxxxx";

// ✅ 正确
string apiKey = ConfigManager.GetSecret("AI_API_KEY");
```

### [Secret] [MUST] Cookie 文件安全存储

Cookie 持久化文件 **必须** 存储在应用数据目录下，**禁止** 存储在用户可直接浏览的公开目录。

---

## 3. 外部数据校验

### [Data] [MUST] 校验外部平台数据

从外部平台 (HTTP 响应、WebSocket 消息) 接收的数据 **必须** 在使用前校验：
- JSON 解析用 `TryParse` 而非直接 `Parse`
- 数值字段检查范围和类型
- 字符串字段检查长度和非法字符

```csharp
// ❌ 错误 — 直接使用未校验数据
var data = JObject.Parse(responseBody);
int count = data["count"].Value<int>();  // 可能 NullReferenceException

// ✅ 正确 — 安全解析
if (JObject.TryParse(responseBody, out var data))
{
    int count = data["count"]?.Value<int?>() ?? 0;
}
```

### [Data] [SHOULD] 授权变更审计日志

授权状态变更 (登录、Token 刷新、登出、授权过期) **应该** 记录审计日志，包含时间、主播 SecUid、变更前后状态。
