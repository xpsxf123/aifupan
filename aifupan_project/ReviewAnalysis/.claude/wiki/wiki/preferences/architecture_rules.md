# 架构规则 (Architecture Rules)

本文档定义项目分层架构、依赖注入、类设计、线程安全、日志和配置规范。所有 AI Agent 在编写或修改代码时 **必须** 遵循这些规则。
规则来源: `architecture_maintainability_review_20260423.md` #1,3,4,5,6,8

---

## 1. 分层职责

应用采用 4 层架构，每层有明确的职责边界和调用方向：

```
UI/Form 层        → 负责窗口、控件、用户交互
    ↓ 调用
Controller 层     → 负责参数校验、请求委托、结果返回
    ↓ 调用
Service/BLL 层    → 负责业务逻辑、状态管理、事务编排
    ↓ 调用
Api/Db 层         → 负责 HTTP 调用、数据库操作、外部服务
```

### [Layer] [MUST] Controller 只做委托

Controller **必须** 只做三件事：校验输入参数、委托给 Service/BLL、返回结果。**禁止** 在 Controller 中编写业务逻辑、创建线程、调用 Thread.Sleep、直接访问 Cache 或 Api 层。

```csharp
// ❌ 错误 — Controller 做了业务逻辑 + 线程管理 + 状态修改
public string CreateAnalysis(string videoId, string token, string tradeId)
{
    AnchorVideoBll.isAnalysis = true;              // 直接修改 BLL 静态状态
    AnchorVideoBll.currentAnalysisId = videoId;
    VideoEntity video = VideoApi.GetVideoByVideoIdSync(videoId);  // 直接调 Api 层
    new Thread(() => bll.Analysis(video)).Start();  // 创建线程
    Thread.Sleep(2500);                             // 阻塞等待
    return "success";
}

// ✅ 正确 — Controller 只做委托
public async Task<string> CreateAnalysis(string videoId, string token, string tradeId)
{
    return await _videoService.StartAnalysisAsync(videoId, token, tradeId);
}
```

### [Layer] [NEVER] BLL 禁止创建 UI

BLL/Service 层 **禁止** 实例化 WinForms 控件、创建窗口、弹出对话框。UI 创建是 Form/UI 层的专属职责。

```csharp
// ❌ 错误 — BLL 创建窗口
// Bll/Anchor/AnchorBll.cs:2615
Form juliangForm = new Form { Text = "巨量百应", Size = new Size(1400, 900) };
ChromiumWebBrowser browser = new ChromiumWebBrowser(url);
juliangForm.Controls.Add(browser);
juliangForm.Show();

// ❌ 错误 — BLL 弹出文件选择框
// Bll/ShortVideoBll.cs:35
OpenFileDialog dialog = new OpenFileDialog { Title = "选择视频文件" };
dialog.ShowDialog();

// ✅ 正确 — BLL 返回数据，UI 层负责展示
// BLL 层
public string GetJuliangUrl(string secUid) { return $"https://buyin.jinritemai.com/..."; }

// UI 层 (Controller 或 Form)
string url = _anchorService.GetJuliangUrl(secUid);
OpenBrowserWindow(url);
```

### [Layer] [NEVER] Controller 禁止直接访问 Cache

Controller **禁止** 直接调用 CacheManager 类。缓存访问 **必须** 通过 Service/BLL 层间接进行。

```csharp
// ❌ 错误 — Controller 直接操作缓存
public void UpdateAnchorTrade(string secUid, string tradeId)
{
    AnchorInfo info = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
    AnchorApi.UpdateUserAnchorSync(secUid, tradeId, -1, -1);
    info.TradeId = tradeId;
    AnchorCacheManager.SetAnchorCache(info);
}

// ✅ 正确
public void UpdateAnchorTrade(string secUid, string tradeId)
{
    _anchorService.UpdateTrade(secUid, tradeId);
}
```

---

## 2. 依赖注入

### [DI] [MUST] 构造函数注入

所有新 Service/BLL 类 **必须** 通过构造函数参数接收依赖，参数类型为接口。**禁止** 在业务逻辑中 `new XxxBll()`。

```csharp
// ❌ 错误 — 直接实例化，无法测试
public class FristPageIni
{
    public static void LongRunningTask()
    {
        ConfigBll configBll = new ConfigBll();
        OperationAnchorBll operationAnchor = new OperationAnchorBll();
        AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
    }
}

// ✅ 正确 — 构造函数注入
public class StartupService
{
    private readonly IConfigService _config;
    private readonly IAnchorOperationService _anchorOps;
    private readonly IAnchorVideoService _videoService;

    public StartupService(IConfigService config, IAnchorOperationService anchorOps, IAnchorVideoService videoService)
    {
        _config = config;
        _anchorOps = anchorOps;
        _videoService = videoService;
    }
}
```

### [DI] [MUST] 先定义接口再实现

每个新 BLL/Service 类 **必须** 先定义对应的 `IXxxService` 接口，再编写实现类。

```csharp
// 接口定义
public interface IAnchorVideoService
{
    Task<string> StartAnalysisAsync(string videoId, string token, string tradeId);
    Task CancelAnalysisAsync(string videoId);
    Task<SentenceMarkDto> GetAnalysisAsync(string videoId);
}

// 实现
public class AnchorVideoService : IAnchorVideoService { ... }
```

---

## 3. 类设计

### [Size] [MUST] 单个类不超过 500 行

新类 **必须** 控制在 500 行以内。超过时应按职责拆分。

**已知需重构的 God Class**:

| 类 | 当前行数 | 目标 |
|---|---|---|
| `Form1.cs` | 3,112 | 拆分为多个 UserControl + 事件服务 |
| `AnchorBll.cs` | 2,719 | 拆分为 DetectionService / AuthService / ManageService |
| `AnchorVideoBll.cs` | ~2,500 | 拆分为 AnalysisService / VideoManageService |
| `WebsocketConnection.cs` | 784 | 拆分为 ConnectionManager / DataDispatcher |

### [Size] [MUST] 单个方法不超过 80 行

方法 **必须** 控制在 80 行以内。超过时提取为命名子方法。

### [State] [NEVER] 禁止新增 public static 可变字段

**禁止** 新增 `public static` 可变字段。已有的 31 个 (见架构报告 #1) 将逐步迁移为实例状态 + DI。

```csharp
// ❌ 错误 — 全局可变状态
public static volatile bool isAnalysis = false;
public static string currentAnalysisId = "";
public static ConcurrentDictionary<string, AnchorRecordBll> recordingList = new();

// ✅ 正确 — 实例状态 + 接口
public class AnalysisStateManager : IAnalysisStateManager
{
    private volatile bool _isAnalysis;
    private string _currentAnalysisId = "";
    private readonly object _lock = new();

    public bool TryStartAnalysis(string videoId) { ... }
    public void CompleteAnalysis() { ... }
}
```

---

## 4. 线程安全

### [Thread] [MUST] Lock 对象为 private static readonly

Lock 对象 **必须** 声明为 `private static readonly object`。**禁止** 对 lock 对象使用 `volatile`。

```csharp
// ❌ 错误
public static volatile object _lockObject = new object();

// ✅ 正确
private static readonly object _lock = new object();
```

### [Thread] [MUST] ConcurrentDictionary 的值类型线程安全

如果 `ConcurrentDictionary<K, V>` 的值 `V` 是可变集合 (如 `List<T>`)，**必须** 换用线程安全集合或加锁访问。

```csharp
// ❌ 错误 — List<string> 非线程安全
public static ConcurrentDictionary<string, List<string>> dicDanMu = new();

// ✅ 正确 — 方案一: 线程安全集合 (仅追加场景)
public static ConcurrentDictionary<string, ConcurrentBag<string>> dicDanMu = new();

// ✅ 正确 — 方案二: 加锁 (需有序访问场景)
private static readonly object _danMuLock = new();
// 访问时: lock (_danMuLock) { dicDanMu[key].Add(item); }
```

---

## 5. 日志规范

### [Log] [MUST] 统一日志方法

所有新增日志 **必须** 使用统一日志抽象。**禁止** 新增 `Console.WriteLine`、`Debug.WriteLine` 或直接 `FileUtils.LogError` 调用。

目标: 引入 `ILogger` 接口，后续迁移到 Serilog 或 Microsoft.Extensions.Logging。

---

## 6. 配置规范

### [Config] [NEVER] 禁止硬编码 URL

**禁止** 在源代码中硬编码 URL。所有 URL **必须** 定义在配置文件或常量类中。

```csharp
// ❌ 错误 — 硬编码 URL (项目中发现 331 处)
string url = "https://compass.jinritemai.com/compass_api/...";
public static string aiCompletionsUrl = "https://ark.cn-beijing.volces.com/api/v3/chat/completions";

// ✅ 正确 — 配置文件
public class AiConfig
{
    public string BaseUrl { get; set; } = "https://ark.cn-beijing.volces.com/api/v3";
    public string CompletionsPath => $"{BaseUrl}/chat/completions";
}
```

### [Config] [MUST] 模块配置类型化

每个模块 **应该** 有独立的类型化配置类 (如 `AiConfig`、`PlatformConfig`)，从统一配置源加载。
