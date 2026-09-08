# 平台模块开发规范 (Platform Module Rules)

本文档定义平台数据采集模块 (juliang/qianchuan/life/enterprise/anchorLive/WeChatChannels) 的开发规范，防止代码重复。所有 AI Agent 在创建或修改平台模块时 **必须** 遵循这些规则。
规则来源: `architecture_maintainability_review_20260423.md` #2,7,9

---

## 1. 当前问题

6 个平台模块共 ~11,500 行代码中 **60% (~6,800 行) 是完全相同的样板代码**。每个模块都独立实现了几乎一样的 DataCollectionManager、DataPoller、DataHandle，只有 cookie 检查方法名和授权状态字段名不同。

---

## 2. 基类继承规则

### [Platform] [MUST] 使用基类继承

创建或修改平台模块时，**必须** 继承以下基类层次：

| 组件 | 基类/接口 | 子类只需实现 |
|---|---|---|
| DataCollectionManager | `PlatformDataCollectionManagerBase<TPoller>` | `CheckCookieValid()` / `GetAuthStatusField()` / `CreatePoller()` |
| DataPoller | `DataPollerBase` | `PollDataAsync()` |
| DataHandle | `IDataHandle` | `GetCookiesFromLocal()` / `PullDataAsync()` |
| Adapter | `BaseDataAdapter` | `MapPlatformSpecificFields()` |

### 基类模板

```csharp
// 基类 — 消除 72% 的 DataCollectionManager 重复代码
public abstract class PlatformDataCollectionManagerBase<TPoller> where TPoller : IDataPoller
{
    protected readonly ConcurrentDictionary<string, TPoller> _pollers = new();
    protected Timer _detectionTimer;
    protected abstract string PlatformName { get; }

    public void StartDetection()
    {
        if (_detectionTimer != null) return;
        _detectionTimer = new Timer(_ => Task.Run(DetectionCallback), null, 30000, 30000);
    }

    public void StopDetection()
    {
        _detectionTimer?.Dispose();
        _detectionTimer = null;
        foreach (var p in _pollers.Values) p.Stop();
        _pollers.Clear();
    }

    private async Task DetectionCallback()
    {
        var anchors = AnchorCacheManager.GetAllNotRemoveAnchors()
            .Where(a => a.platform == 0);
        foreach (var anchor in anchors)
        {
            var isValid = CheckCookieValid(anchor.SecUid);
            SyncAuthStatus(anchor, isValid);
            if (isValid && await IsLiveAsync(anchor))
                StartPolling(anchor);
        }
    }

    // 子类实现这些即可
    protected abstract bool CheckCookieValid(string secUid);
    protected abstract string GetAuthStatusFieldName();
    protected abstract TPoller CreatePoller(AnchorInfo anchor, string roomId);
    protected virtual async Task<bool> IsLiveAsync(AnchorInfo anchor)
        => !string.IsNullOrEmpty(await DouYinAnchorBll.GetLiveRoomIdIfOnline(anchor));
}
```

### 子类示例 — 千川只需 ~30 行

```csharp
public class QianchuanDataCollectionManager 
    : PlatformDataCollectionManagerBase<QianchuanDataPoller>
{
    protected override string PlatformName => "千川";

    protected override bool CheckCookieValid(string secUid) 
        => QianchuanUtils.CheckCookieValid(secUid);

    protected override string GetAuthStatusFieldName() 
        => "qianchuanAuthStatus";

    protected override QianchuanDataPoller CreatePoller(AnchorInfo anchor, string roomId) 
        => new QianchuanDataPoller(anchor, roomId);
}
```

---

## 3. 统一枚举

### [Platform] [MUST] 使用统一 PlatformAuthStatus

**禁止** 创建新的平台特定授权状态枚举。使用统一枚举：

```csharp
// ❌ 错误 — 5 份完全相同的枚举
public enum JuliangAuthStatusEnum { unAuth, auth, authExpires }
public enum QianchuanAuthStatusEnum { unAuth, auth, authExpires }
public enum LifeAuthStatusEnum { unAuth, auth, authExpires }

// ✅ 正确 — 统一枚举
public enum PlatformAuthStatus
{
    UnAuth = 0,
    Auth = 1,
    AuthExpires = 2,
    NeedRefresh = 3
}
```

---

## 4. 适配器去重

### [Platform] [MUST] Adapter 使用模板方法模式

Adapter **必须** 继承 `BaseDataAdapter`，只覆写平台特定的字段映射方法。

```csharp
// 基类
public abstract class BaseDataAdapter : IDataAdapter
{
    public UnifiedDataModel Adapt(string rawJson, string secUid, string roomId, string videoId = null)
    {
        var model = CreateBaseModel(secUid, roomId, videoId);
        if (string.IsNullOrEmpty(rawJson)) return model;
        try
        {
            var jo = JObject.Parse(rawJson);
            MapPlatformSpecificFields(jo, model);  // 子类实现
            model.CalculateDerivedFields();
        }
        catch (Exception ex) { _logger.LogError(ex, "数据适配失败"); }
        return model;
    }

    protected abstract void MapPlatformSpecificFields(JObject raw, UnifiedDataModel model);

    protected UnifiedDataModel CreateBaseModel(string secUid, string roomId, string videoId)
    {
        return new UnifiedDataModel { secUid = secUid, batchNumber = roomId, ... };
    }
}

// 子类只写字段映射
public class QianchuanAdapter : BaseDataAdapter
{
    public override string PlatformId => "qianchuan";

    protected override void MapPlatformSpecificFields(JObject raw, UnifiedDataModel model)
    {
        var data = raw["data"];
        model.viewCount = data?["view_count"]?.Value<long?>();
        model.salesRevenue = data?["sales_revenue"]?.Value<double?>();
    }
}
```

---

## 5. 新增平台 Checklist

新增平台 **必须** 按以下步骤执行，且 **禁止** 超过 5 个平台特定文件 (~400 行)：

- [ ] 1. 创建 `{Platform}DataCollectionManager` — 继承 `PlatformDataCollectionManagerBase<TPoller>` (~30 行)
- [ ] 2. 创建 `{Platform}DataPoller` — 继承 `DataPollerBase` (~30 行)
- [ ] 3. 创建 `{Platform}DataApi` — 平台特定 HTTP 接口 (行数视 API 数量而定)
- [ ] 4. 创建 `{Platform}Form` — CefSharp 授权窗口 (可参考现有模板)
- [ ] 5. 创建 `{Platform}Adapter` — 继承 `BaseDataAdapter`，只写字段映射 (~50 行)
- [ ] 6. 在 `PlatformDataManager` 注册新平台
- [ ] 7. 在配置文件中添加平台 URL

**禁止**:
- 复制粘贴现有平台的 DataCollectionManager/DataPoller 全文
- 创建新的 `{Platform}AuthStatusEnum`
- 重复实现 `StartDetection()`/`StopDetection()` 逻辑
- 重复实现 `CheckAllAnchorsAsync()` 骨架代码

---

## 6. 成本对比

| 项目 | 重构前 | 重构后 |
|---|---|---|
| 新增平台文件数 | 20+ | 3-5 |
| 新增平台代码量 | ~2,300 行 (66% 样板) | ~400 行 |
| 新增平台工时 | 23-33h | 4-6h |
| 修改公共逻辑 | 改 6 个文件 | 改 1 个基类 |
