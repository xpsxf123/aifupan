# 基础设施域 (Infrastructure)

## 概述

系统的基础设施层提供嵌入式浏览器、内嵌 HTTP 服务器、数据库访问、应用启动和生命周期管理等核心能力。

## 核心概念

### CefSharp (嵌入式浏览器)
基于 Chromium 的嵌入式浏览器框架 (CefSharp 135.0)，用于加载抖音等平台 Web 页面完成授权和数据采集。

**用途**:
- 平台登录授权 (JuliangForm / AnchorLiveForm / LifeForm / EnterpriseForm / QianchuanForm)
- 主应用 UI 承载 (FormMain)
- Cookie 管理和提取
- JS 脚本注入执行

**监控**: `CefSubprocessMonitor.cs` 监控 CefSharp 子进程

### HttpServer (内嵌 HTTP 服务)
基于 `HttpListener` 的本地 HTTP 服务器，为 CefSharp 嵌入页面提供 REST API。

**特性**:
- 重试启动机制 (最多 3 次, 间隔 1 秒)
- CORS 支持和 OPTIONS 预检处理
- 文件流式服务
- AI Ask 流式 (SSE) 返回
- 上传/下载路由
- 通过反射 + 方法缓存的动态路由分发
- 监听端口: `6606/replay/` (本地), 由 `ReplayHttpUtils` 解析分发

**核心类**:
- `HttpListenerServer` — 服务器主循环, 请求接收与分发
- `HttpListenerAsyncServer` — 异步版本
- `HttpProc` — 请求处理, 通过 MethodCache 反射调用 Controller
- `HttpResourceFileServer` — 静态资源文件服务 (端口 8080)

**文件**: `HttpServer/HttpListenerServer.cs`, `HttpServer/HttpProc.cs`

### Controller 层 (Attribute 路由系统)
自定义 REST 控制器属性路由系统，使 C# 类方法能通过 HTTP 被 CefSharp 前端调用：

- `[RestController("描述", "api/xxx")]` — 类级路由前缀, 定义 API 路径
- `[HttpPost("描述", "methodName")]` / `[HttpGet("描述", "methodName")]` — 方法级路由
- `[RestAsyncController("描述", "api/xxx")]` — 异步 Controller 标记

**路由分发流程**:
1. `HttpProc` 解析 URL, 提取 `actionPath` (如 `api/anchorinfo/page`)
2. 通过 `MethodCache` (预编译的方法字典) 反射调用目标方法
3. 在 UI 线程执行, 支持 `params` 参数自动绑定

**关键文件**: `Attributes/RestControllerAttribute.cs`, `Attributes/HttpPostAttribute.cs`, `Attributes/HttpGetAttribute.cs`, `Attributes/RestAsyncControllerAttribute.cs`

### CefSharp 请求上下文管理
`Juliang.CefRequestContextManager` 管理 CefSharp 浏览器的 Cookie 和请求上下文，为不同平台提供独立的浏览器会话。

**特性**:
- 多平台 Cookie 隔离
- 共享缓存路径
- 自动保存/恢复 Cookie 状态

### BarrageGrabExe (弹幕采集子进程)
独立的弹幕抓取工具，作为外部进程 (`BarrageGrab.exe`) 运行，通过 stdout 输出解析后的直播间事件。

**目录结构**:
- `BarrageGrabExe/fp/` — 指纹/设备指纹相关
- `BarrageGrabExe/runtimes/` — .NET 运行时 (win-x64/native, win-x86/native)
- 依赖: `librecord.dll` (录制库, 73MB)

**与主程序通信**: 通过 `Process.StandardOutput` 读取 stdout 行输出, `SendWebsocketDataFromLine` 解析事件标记

### SQLite 数据层 (Db/)
基于 SQLite + Entity Framework 6 的数据持久化层。

**核心文件**: `Db/SQLiteHelper.cs`

**功能**:
- 创建/打开数据库
- 查询/非查询/标量执行
- 事务批量执行
- 实体对象映射 (PascalCase ↔ snake_case)
- 分页查询
- 计数/最大值辅助方法

**连接**: `Data Source=DbFile\\review_analysis.sqlite;Version=3;`

### 启动与生命周期

**启动流程** (`Program.cs` → `AppContext.cs` → `Form1.cs`):
1. `Program.Main()` — 初始化全局异常处理
2. `AppContext` — 管理 SplashScreen / FormMain 切换
3. `SplashScreen` — 启动画面
4. `FormMain` — 加载主窗口和 CefSharp

**启动编排** (`BackgroundWork/FristPageIni.cs`):
1. 启动 HTTP 资源文件服务
2. 加载配置 (`ConfigBll`)
3. 同步本地数据到服务器
4. 同步服务器主播列表到本地
5. 清理视频缓存
6. 更新未完成录制状态
7. 初始化失败分析状态
8. 启动自动分析线程 + 看门狗
9. 启动定时用户轮询 (8 秒间隔)
10. 初始化弹幕采集

**看门狗线程**: 每 5 分钟检查自动分析线程存活状态，异常时自动重启

**账号保护**: 检测到用户冻结或租户变更时，自动停止录制、清除 Token、强制登出

### WebSocket 通信 (Websocket/)
WebSocket 客户端模块，用于与抖音直播间建立长连接，采集弹幕和实时数据流。

**核心类**:
- `WebsocketConnection` — WebSocket 连接管理 (连接/重连/心跳)
- `WebsocketDataHandle` — 数据接收与分发
- `WebsocketProxyCacheManager` — WebSocket 代理缓存

**连接状态**: 未连接(0) → 已连接(1) → 启动中(2) → 待重连(3) → 错误(4)

### TcpHelp (TCP 通信)
底层 TCP Socket 通信辅助模块，提供原始 TCP 数据收发能力。

### BackgroundWork (后台任务)
- `DetectionBackgroundManager` — 主播上线检测后台管理器 (每 8 秒轮询)
- `FristPageIni` — 首次页面初始化编排

### Utils (工具集)
47 个工具类，覆盖:
- **文件与IO**: `FileUtils`, `CsvUtils`
- **加密**: `AESHelper`, `AesCbcUtils`
- **日期**: `DateUtils`
- **音频**: `AudioUtils`
- **HTTP**: `ReplayHttpUtils`, `ServerHttpUtils`, `HttpUtils`
- **视频录制**: `CppRecordUtils` (C++ 库封装)
- **系统**: `SystemUtils`, `AddFirewall`
- **并发**: `AsyncLock`, `ThreadSafeSortedDictionary`
- **数据**: `DataTableToEntity`, `JArrayToDataTable`
- **平台特定**: `JuliangSign`, `QianchuanSign`, `DouyinSignUtils`, `PlaywrightHelper`

### 云存储
- **腾讯云 COS** — 对象存储 (Tencent.QCloud.Cos.Sdk)
- **七牛云** — 云存储 (Qiniu SDK)

### 自动更新
- `Update/` — 更新逻辑
- `updateExe/` — 更新器可执行文件 (ReviewAnalysisUpdate.exe)
