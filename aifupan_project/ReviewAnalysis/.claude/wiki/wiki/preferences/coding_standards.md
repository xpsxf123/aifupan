# C# 编码规范 (Coding Standards)

本文档定义项目 C# 编码规范。所有 AI Agent 在编写或修改 C# 代码时 **必须** 遵循这些规则。
规则来源: `code_review_20260423.md` #2,4,5,8,9,10,13,14,15,16

---

## 1. 命名规范

### [Naming] [MUST] 公共成员使用 PascalCase

所有公共方法、属性、类型 **必须** 使用 PascalCase。

```csharp
// ❌ 错误
public void updateAiPartial(AiAnchorInfoVo vo) { }
public void cancelAuthorizeJuliang(string secUid) { }
public void topAnchor(string secUid, int action) { }

// ✅ 正确
public void UpdateAiPartial(AiAnchorInfoVo vo) { }
public void CancelAuthorizeJuliang(string secUid) { }
public void TopAnchor(string secUid, int action) { }
```

### [Naming] [MUST] 标识符拼写检查

新标识符提交前 **必须** 检查拼写。以下已知拼写错误在修改相关文件时 **必须** 修正：

| 错误 | 正确 | 位置 |
|---|---|---|
| `FristPageIni` | `FirstPageInitializer` | `BackgroundWork/` |
| `vedioSizie` | `videoSize` | `entity/video/` |
| `giveStatuc` | `giveStatus` | `bo/ai/`, `vo/ai/` |
| `Decector` | `Detector` | `Controller/`, `Bll/` |
| `websocketLinkErroeNum` | `websocketLinkErrorNum` | `Websocket/Entity/` |

> 注意: 修改已序列化字段名时，使用 `[JsonProperty("旧名")]` 保持 JSON 兼容性。

### [Naming] [MUST] 常量使用描述性命名

**禁止** 在业务逻辑中使用未解释的数字字面量。提取为命名常量。

```csharp
// ❌ 错误
if (getUserInfoErrorCount >= 10) { ... }
Thread.Sleep(5 * 60 * 1000);
var monitor = new CefSubprocessMonitor(1800);

// ✅ 正确
private const int MaxUserInfoErrorCount = 10;
private const int WatchdogIntervalMs = 5 * 60 * 1000;
private const int CefMonitorIntervalSeconds = 1800;

if (getUserInfoErrorCount >= MaxUserInfoErrorCount) { ... }
await Task.Delay(WatchdogIntervalMs);
var monitor = new CefSubprocessMonitor(CefMonitorIntervalSeconds);
```

---

## 2. Async/Await 规范

### [Async] [NEVER] 禁止 async void

**禁止** 使用 `async void`，唯一例外是 UI 事件处理器。所有异步方法 **必须** 返回 `Task` 或 `Task<T>`。

```csharp
// ❌ 错误 — 异常不可观测，可能导致进程崩溃
public static async void StartDetectionAll(int videoType, int definition) { ... }
public async void Analysis(UploadFileEntity file, string token, string tradeId) { ... }

// ✅ 正确
public static async Task StartDetectionAllAsync(int videoType, int definition) { ... }
public async Task AnalysisAsync(UploadFileEntity file, string token, string tradeId) { ... }
```

### [Async] [NEVER] 禁止 .Result / .Wait() / .GetAwaiter().GetResult()

**禁止** 在 UI 线程或 SynchronizationContext 环境下同步阻塞异步调用，会导致死锁。

```csharp
// ❌ 错误 — 死锁风险
HttpResponseMessage response = client.SendAsync(request).Result;
string str = response.Content.ReadAsStringAsync().Result;
Task.Run(async () => await DoWork()).Wait();
}).GetAwaiter().GetResult();

// ✅ 正确
HttpResponseMessage response = await client.SendAsync(request);
string str = await response.Content.ReadAsStringAsync();
await DoWorkAsync();
```

### [Async] [MUST] 异步链路完整传递

async/await **必须** 贯穿整个调用链，不要在中间层断开。

---

## 3. 异常处理

### [Exception] [NEVER] 禁止空 catch 块

**禁止** 空 catch 块或仅记日志不做任何处理的 catch。至少 **必须** 记录完整异常信息，关键路径 **必须** 向上传播或标记不健康状态。

```csharp
// ❌ 错误
try { process.Kill(); } catch { }

catch (Exception e) {
    FileUtils.LogError(e.Message);
    // 异常被吞掉，调用方不知道失败
}

// ✅ 正确 — 记录 + 传播
catch (Exception ex)
{
    _logger.LogError(ex, "进程清理失败");
    throw;  // 或 return Result.Failure(ex.Message);
}

// ✅ 正确 — 非关键路径，记录完整信息
catch (Exception ex)
{
    _logger.LogWarning(ex, "遥测上报失败，不影响主流程");
}
```

### [Exception] [MUST] Fire-and-Forget 必须处理异常

`Task.Run()` 如果不 await，**必须** 在 lambda 内部包裹 try-catch，或使用 `.ContinueWith` 处理异常。

```csharp
// ❌ 错误 — 异常静默吞掉
Task.Run(async () =>
{
    while (true) { await DoWork(); }  // 异常导致静默退出
});

// ✅ 正确
Task.Run(async () =>
{
    try
    {
        while (true) { await DoWork(); }
    }
    catch (Exception ex)
    {
        _logger.LogError(ex, "后台任务异常退出");
    }
});
```

---

## 4. 资源管理

### [Resource] [MUST] IDisposable 使用 using

所有 `IDisposable` 对象 (连接、流、进程) **必须** 使用 `using` 语句确保释放。

```csharp
// ❌ 错误 — 手动 Close，可能遗漏
SQLiteConnection connection = new SQLiteConnection(connStr);
try { connection.Open(); ... }
finally { connection.Close(); }

// ✅ 正确
using (var connection = new SQLiteConnection(connStr))
using (var cmd = new SQLiteCommand(sql, connection))
{
    connection.Open();
    return cmd.ExecuteNonQuery();
}
```

### [Resource] [MUST] Timer 存为字段并 Dispose

Timer 实例 **必须** 存为类字段，在 `Dispose()` 中释放。**禁止** 创建局部变量 Timer 并添加事件订阅。

```csharp
// ❌ 错误 — 局部变量 Timer，永远不会被回收
void Init()
{
    var timer = new System.Timers.Timer(60000);
    timer.Elapsed += OnTimedEvent;
    timer.Enabled = true;
    // timer 是局部变量但永远不会被 GC（有事件引用），也无法手动 Stop
}

// ✅ 正确
private System.Timers.Timer _timer;

void Init()
{
    _timer = new System.Timers.Timer(60000);
    _timer.Elapsed += OnTimedEvent;
    _timer.Enabled = true;
}

public void Dispose()
{
    _timer?.Stop();
    _timer?.Dispose();
}
```

### [Resource] [MUST] Process 对象跟踪生命周期

创建的 `Process` 对象 **必须** 封装在 IDisposable 包装类中，或在调用方通过集合统一管理生命周期。

---

## 5. 空安全

### [Safety] [MUST] 集合结果索引前检查

从数据库或 API 返回的集合，在通过索引访问前 **必须** 检查是否为空。

```csharp
// ❌ 错误 — 查询无结果时 IndexOutOfRangeException
List<T> list = ConvertToEntityList<T>(dt);
return list[0];

// ✅ 正确
return list.Count > 0 ? list[0] : default;
// 或
return list.FirstOrDefault();
```

---

## 6. 注释规范

### [Comment] [MUST] 新建类必须有 XML 文档注释

每个新建的类/结构体/接口 **必须** 带 `<summary>` 说明其职责和使用场景。

```csharp
// ❌ 错误 — 无注释
public class LiveOnlineTrendPuller
{
    public static async Task<List<CompassTrendPoint>> FetchAsync(string roomId, string secUid) { }
}

// ✅ 正确
/// <summary>
/// 巨量罗盘 - 在线人数曲线拉取器。
/// 调用 blend_trend_v2 API 获取直播间在线人数随时间变化的曲线数据，
/// 复用 JuliangVideoDownloader 的 Cookie 和 Headers 构建逻辑。
/// </summary>
public class LiveOnlineTrendPuller
{
    /// <summary>
    /// 拉取直播间在线人数曲线数据。
    /// 过滤 point_name == "online_user_cnt" 的数据点，
    /// 将 Unix 时间戳转换为 yyyy-MM-dd HH:mm:ss（东八区）格式后返回。
    /// </summary>
    /// <param name="roomId">直播间 room_id（对应 batchNumber）</param>
    /// <param name="secUid">主播 sec_uid，用于从本地 Cookie 文件加载认证凭据</param>
    /// <returns>趋势数据点列表；Cookie 缺失 / API 异常 / st ≠ 0 时返回 null</returns>
    public static async Task<List<CompassTrendPoint>> FetchAsync(string roomId, string secUid) { }
}
```

### [Comment] [MUST] Public/Internal 方法必须含 `<param>` 和 `<returns>`

所有 public 和 internal 方法 **必须** 包含：
- `<summary>` — 方法功能描述
- `<param name="...">` — 每个参数的用途和约束
- `<returns>` — 返回值的含义、null 返回条件

### [Comment] [MUST] 实体类属性必须有注释

每个属性 **必须** 用 `<summary>` 说明其业务含义和数据约束。

```csharp
/// <summary>
/// 巨量罗盘 blend_trend_v2 API 返回的单条趋势数据点。
/// 代表某个时间点在线上的人数快照。
/// </summary>
public class CompassTrendPoint
{
    /// <summary>数据点时间，格式 yyyy-MM-dd HH:mm:ss（东八区）</summary>
    public string Time { get; set; }

    /// <summary>该时间点的在线人数</summary>
    public double Value { get; set; }
}
```

### [Comment] [SHOULD] 关键算法步骤写行内注释

复杂逻辑（分页、时间转换、snowflake ID 生成等）在关键行写 `//` 行内注释说明为什么这样做，而非重复代码。

### [Comment] [SHOULD] 常量/静态字段注释

非自解释的 const/static 字段用 `<summary>` 说明其含义和取值来源。

### [Comment] [LINT] Code Review 检查项

Phase 5 QA code-review 时必须验证：
- [ ] 每个新建类有 `<summary>`
- [ ] 每个 public/internal 方法有 `<summary>` + `<param>` + `<returns>`
- [ ] 每个属性有 `<summary>`
- [ ] 复杂逻辑有行内注释
- [ ] API URL / 魔法数字已注释或提取为命名常量

违反任意一条 → code-review FAIL。
