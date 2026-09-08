# 性能规则 (Performance Rules)

本文档定义项目性能相关编码规范。所有 AI Agent 在编写或修改代码时 **必须** 遵循这些规则。
规则来源: `code_review_20260423.md` #3,4,6,7 / `architecture_maintainability_review_20260423.md` #8

---

## 1. HttpClient 使用规范

### [HTTP] [MUST] HttpClient 单例复用

HttpClient 实例 **必须** 作为静态单例复用或通过 `IHttpClientFactory` 创建。**禁止** 每次请求创建新实例。

```csharp
// ❌ 错误 — 每次请求 new HttpClient，导致 Socket 耗尽
public async Task<string> CallApi(string url)
{
    using (HttpClient client = new HttpClient())
    {
        return await client.GetStringAsync(url);
    }
}

// ✅ 正确 — 静态单例
private static readonly HttpClient _httpClient = new HttpClient();

public async Task<string> CallApi(string url)
{
    return await _httpClient.GetStringAsync(url);
}

// ✅ 更好 — 带配置的单例
private static readonly Lazy<HttpClient> _httpClient = new Lazy<HttpClient>(() =>
{
    var handler = new HttpClientHandler { AutomaticDecompression = DecompressionMethods.GZip };
    var client = new HttpClient(handler);
    client.Timeout = TimeSpan.FromSeconds(30);
    return client;
});
```

### [HTTP] [NEVER] 禁止 using 包裹 HttpClient

**禁止** 用 `using` 包裹短生命周期的 HttpClient。底层 Socket 在 Dispose 后进入 `TIME_WAIT` 状态（默认 4 分钟），高频调用会导致端口耗尽。

---

## 2. 线程规范

### [Thread] [NEVER] 禁止 new Thread() 执行业务逻辑

**禁止** 用 `new Thread()` 执行业务操作。使用 `Task.Run()` 或托管调度器。

```csharp
// ❌ 错误 — 裸线程，无法等待完成、无法捕获异常
new Thread(() => anchorVideoBll.Analysis(videoEntity)).Start();
Thread.Sleep(2500);

// ✅ 正确
await Task.Run(() => anchorVideoBll.AnalysisAsync(videoEntity));
```

### [Thread] [NEVER] Controller/BLL 中禁止 Thread.Sleep

**禁止** 在 Controller 或 BLL 代码中调用 `Thread.Sleep()`。如确实需要延迟，使用 `await Task.Delay()`。

```csharp
// ❌ 错误
Thread.Sleep(2500);
Thread.Sleep(8000);

// ✅ 正确
await Task.Delay(2500);
```

### [Thread] [MUST] 长时间任务使用 CancellationToken

长时间运行的后台任务 **必须** 接受并尊重 `CancellationToken`，确保可优雅停止。

```csharp
// ✅ 正确
public async Task RunDetectionLoopAsync(CancellationToken token)
{
    while (!token.IsCancellationRequested)
    {
        await CheckAnchorsAsync();
        await Task.Delay(30000, token);
    }
}
```

---

## 3. 数据库性能

### [DB] [MUST] 统一使用 using 管理连接

所有数据库操作 **必须** 使用 `using` 块管理连接生命周期。**禁止** 维护类级别的长连接字段。

```csharp
// ❌ 错误 — 类级别连接 + 手动 Close
private SQLiteConnection _conn;

public int Execute(string sql)
{
    _conn = new SQLiteConnection(connStr);
    try { _conn.Open(); ... }
    finally { _conn.Close(); }  // 异常时可能遗漏
}

// ✅ 正确
public int Execute(string sql)
{
    using (var conn = new SQLiteConnection(connStr))
    using (var cmd = new SQLiteCommand(sql, conn))
    {
        conn.Open();
        return cmd.ExecuteNonQuery();
    }
}
```

### [DB] [NEVER] 禁止循环内查询 (N+1)

**禁止** 在循环中执行数据库查询。应批量查询后在内存中关联。

```csharp
// ❌ 错误 — N+1 查询
foreach (var anchor in anchors)
{
    var videos = db.Query($"SELECT * FROM video WHERE sec_uid = '{anchor.SecUid}'");
}

// ✅ 正确 — 批量查询
var secUids = anchors.Select(a => a.SecUid).ToList();
var allVideos = db.Query("SELECT * FROM video WHERE sec_uid IN (@ids)", secUids);
var grouped = allVideos.GroupBy(v => v.SecUid).ToDictionary(g => g.Key);
```

---

## 4. 缓存规范

### [Cache] [SHOULD] 统一缓存接口

新增缓存管理器 **应该** 实现统一接口，包含过期策略和容量控制：

```csharp
public interface ICacheManager<TKey, TValue>
{
    TValue Get(TKey key);
    void Set(TKey key, TValue value, TimeSpan? expiry = null);
    void Remove(TKey key);
    IReadOnlyList<TValue> GetAll();
    int Count { get; }
}
```

### [Cache] [SHOULD] 缓存有界增长

缓存集合 **应该** 设置最大容量，防止无界增长导致内存泄漏。静态 `ConcurrentDictionary` **应该** 有定期清理机制。

### [Cache] [SHOULD] 记录缓存 TTL

每个 CacheManager **应该** 在类注释中说明预期的缓存 TTL 和失效策略。
