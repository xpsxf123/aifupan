# API Index (Contracts)

This index is the routing table for all externally exposed APIs.

## Hard Rules (MUST)
- During `Archive`, the Agent MUST extract API signatures from `openspec.md` and append them into the table(s) below.
- Do not guess API contracts by scanning the whole codebase. Use the wiki as the source of truth, then validate against code when needed.

## Core Domain APIs

| 模块 | 路由前缀 | 说明 | Doc Link |
|---|---|---|---|
| AnchorInfoController | api/anchorinfo | 主播管理、授权、录制控制 (14 授权 + 8 CRUD + 7 录制 + 7 监控) | `[anchor_api.md]` |
| AnchorVideoController | api/anchorvideo | 视频分析、修复、压缩、分段 (23 端点) | `[video_analysis_api.md]` |
| ContrastController | api/contrast | 视频对比分析 (4 端点) | `[video_analysis_api.md]` |
| UploadFileController | api/uploadfile | 上传文件管理与分析 (16 端点) | `[video_analysis_api.md]` |
| AiRelatedController | api/aiRelated | AI 对话、历史、结构化数据、报告 (14 端点) | `[ai_api.md]` |
| DiagnosisController | api/diagnosis | 数据诊断报告 (2 端点) | `[ai_api.md]` |
| ShortVideoController | api/shortVideo | 短视频提取、达人追踪、热搜 (10 端点) | `[shortvideo_api.md]` |
| ConfigController (+Async) | api/config | 认证初始化、配置管理、用户轮询、版本更新、开发模式 (24+ 端点, 两个类共享路由) | `[system_config_api.md]` |
| SystemController | api/system | 机器码、URL 打开、PDF 转换 (5 端点) | `[system_config_api.md]` |
| FormController | api/form | 窗口控制 (最大化/最小化/关闭) | `[system_config_api.md]` |
| ExportFileController | api/export | 导出分析为 TXT (2 端点) | `[system_config_api.md]` |
| SyncDataController | api/sync | 数据同步触发 (全部代码已注释, 功能废弃) | `[system_config_api.md]` |
| QianchuanDataController | api/qianchuan | 千川广告数据拉取、账户列表查询 | `[system_config_api.md]` |
| UserPropertyController (+Async) | api/userproperty | 用户资产查询 (分析余额/时长/字数) | `[system_config_api.md]` |
| TestController | api/test | 开发调试 (诊断触发/数据上传/视频号测试/OSS测试) | `[system_config_api.md]` |

---

## Archive Extraction SOP
During `Archive`, append a new row using the template below.

### Append Template
```markdown
| {Method} {Path} | {one-line summary} | {Auth Type} / {Identity Type} | {Version} | `[{spec_doc_name}]` | {YYYY-MM-DD} |
```

Anti-bloat rule: if this table exceeds 50 rows, you MUST split it into per-module sub-indexes (example: `user/`, `trade/`) and keep this file as a high-level router only.
