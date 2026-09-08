# docs 索引

项目人类可读文档（前端对接 / 改造指引 / 机制说明 / 测试指南）。按主题分组。

## ASR 语音识别

| 文档 | 类型 | 说明 |
|---|---|---|
| [ASR加速机制-内部集成指南](ASR加速机制-内部集成指南.md) | 内部设计 | 录播 ASR「加速」（在飞把剩余分片切腾讯云）的设计、`AsrAccelerateSignal` API、其它业务复用步骤与约束 |
| [API-录播ASR加速-前端对接](API-录播ASR加速-前端对接.md) | 前端接口 | 加速接口 `GET /api/anchorvideo/accelerate` 的参数、返回、生效行为与接入建议 |
| [本地ASR-设备选择与降级策略-测试指南](本地ASR-设备选择与降级策略-测试指南.md) | 测试指南 | 本地 ASR 按设备选择引擎与降级策略的测试方法 |

## 主播 / 直播间

| 文档 | 类型 | 说明 |
|---|---|---|
| [API-addOrUpdateAnchor-前端对接](API-addOrUpdateAnchor-前端对接.md) | 前端接口 | 添加/修改直播间 `addOrUpdateAnchor` 接口对接（含 AI 监控开关语义、异步通知）|
| [客户端-addOrUpdateAnchor-改造指引](客户端-addOrUpdateAnchor-改造指引.md) | 改造指引 | `addOrUpdateAnchor` 客户端侧改造说明 |
