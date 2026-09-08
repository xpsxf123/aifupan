# WAL — ASR 模块重构经验教训

**日期**: 2026-05-18
**类型**: Rules WAL

## 约束/反模式

### [NEVER] 用外部标识字符串做内部路由判断
`AsrLanguageCode.ToBackendEngine(lang) == "sense-voice"` 是用后端 API 字段做内部逻辑判断，职责耦合。
应暴露专用布尔方法 `IsSvsSupported(lang)`。

### [MUST] 工厂方法统一入口
`Build(language)` 和 `BuildFromEngineList(language, engines)` 是同一职责的两条分支，应合并为
`Build(language, engines = null)`，用 private 方法分拆内部路由，不暴露多个公共 Build 重载。

### [MUST] 返回值构造统一
重复出现 `new Dictionary<string,object>{{"code",x},{"data",y}}` 应提取为私有 `MakeResult(code, data)` 工厂方法，杜绝拼写不一致风险。

### [MUST] 数据转换逻辑不内联在编排方法中
WordList→Result 文本拼接是数据转换，属于自身职责，不应内联在 `RunLocalEngineAsync` 编排方法中，应提取为 `RebuildResultText(entity)` 私有方法。

### [NEVER] 防御性 null 判断用于不可能为 null 的返回值
`Directory.EnumerateFiles().ToList()` 永远不返回 null，`files == null` 判断属于噪声，误导读者认为存在 null 风险。
