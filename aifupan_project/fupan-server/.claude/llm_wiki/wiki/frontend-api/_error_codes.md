# 全局错误码表

> 本文件为项目级共享文档，各模块 API 文档均引用此处，不在各模块内重复定义。
> 业务模块自身的错误码在对应模块文档中额外列出，同时在此集中维护。

## 全局通用错误码

| code | 含义 | 处理建议 |
|---|---|---|
| 0 | 成功 | — |
| -1 | 系统繁忙 | 稍后重试 |
| -3 | 参数类型解析异常 | 检查请求体格式 |
| -6 | 无效参数异常 | 检查参数值 |
| -9 | 统一参数校验失败 | 检查必填/格式 |
| 401 | 未经授权 | 跳登录 |
| 40001 | 会话超时 | 跳登录 |
| 429 | 请求超过次数限制 | 稍后重试 |
| 500 | 内部服务错误 | 展示通用错误 |

## 话术智能监控模块错误码（70001-70014）

> 来源：`replay-generic/.../StatusCode.java`，最后同步：2026-05-30

| code | 枚举名 | 含义 | 前端处理建议 |
|---|---|---|---|
| 70001 | SCRIPT_MONITOR_TOKEN_NOT_ENOUGH | 算力数量不足 | 提示"算力不足，请联系产品顾问购买"，禁止继续触发 |
| 70002 | SCRIPT_MONITOR_QUOTA_NOT_ENOUGH | 授权数量不足 | 提示"授权数量不足，请联系产品顾问购买" |
| 70003 | SCRIPT_MONITOR_PACKAGE_NOT_SUPPORT | 当前套餐不支持 | 提示"当前套餐不支持，请升级" |
| 70004 | SCRIPT_MONITOR_NOT_OWN_ACCOUNT | 仅自有账号支持 AI 话术监控 | 隐藏入口（前端防御）；后端兜底返回此码 |
| 70005 | SCRIPT_MONITOR_STANDARD_SCRIPT_UNCONFIRMED | 标准直播稿未确认 | 提示"请先确认标准直播稿" |
| 70006 | SCRIPT_MONITOR_STANDARD_SCRIPT_INVALID | 标准直播稿无效 | 提示"标准直播稿无效，请重新确认" |
| 70007 | SCRIPT_MONITOR_ACCOUNT_TYPE_LOCKED | 已开启 AI 监控，不能切换账号归属类型 | 提示"请先关闭 AI 话术监控功能后，再修改账号归属类型" |
| 70008 | SCRIPT_MONITOR_STANDARD_SCRIPT_GENERATE_FAIL | 标准稿生成失败 | 提示"标准稿生成失败，请重试" |
| 70010 | SCRIPT_MONITOR_NO_BARRAGE | 本场无弹幕数据 | 提示"本场无弹幕数据，无法生成互动巡检报告" |
| 70011 | SCRIPT_MONITOR_NO_PERMISSION | 无数据读取权限 | 提示"无查看权限" |
| 70012 | SCRIPT_MONITOR_REPORT_NOT_EXIST | 报告不存在 | 提示"报告不存在" |
| 70013 | SCRIPT_MONITOR_PARAM_INVALID | 参数校验失败 | 检查请求参数 |
| 70014 | SCRIPT_MONITOR_FEATURE_NOT_AVAILABLE | 功能未上线 | 隐藏对应入口 |

> **注**：错误码 70009 未使用（预留）。
