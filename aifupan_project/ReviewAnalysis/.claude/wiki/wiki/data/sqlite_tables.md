# SQLite 数据表结构

## 概述

系统使用 SQLite 文件数据库，每个数据域独立一个 `.db` 文件，位于 `DbFile\` 目录下。

## 数据库文件清单

| 数据库文件 | Helper 类 | 数据域 |
|---|---|---|
| review_analysis.db | SQLiteHelper | 主库 (通用数据) |
| review_analysis_audio.db | SQLiteHelperAudio | 音频数据 |
| review_analysis_audio_analysis.db | SQLiteHelperAudioAnalysis | 音频分析结果 |
| review_analysis_video_record.db | SQLiteHelperVideoRecord | 视频录制记录 |
| review_analysis_video_contrast.db | SQLiteHelperVideoContrast | 视频对比数据 |
| review_analysis_video_viewership_num.db | SQLiteHelperVideoViewershipNum | 视频观看/弹幕统计 |
| review_analysis_online_num.db | SQLiteHelperOnlineNum | 实时在线人数 |
| review_analysis_online_num_new.db | — | 新版在线人数 |
| review_analysis_total_online_num.db | SQLiteHelperTotalOnlineNum | 累计在线人数 |
| review_analysis_upload_file.db | SQLiteHelperUploadFile | 上传文件 |
| review_analysis_upload_file_analysis.db | SQLiteHelperUploadFileAnalysis | 上传文件分析 |
| review_analysis_upload_file_audio.db | SQLiteHelperUploadFileAudio | 上传文件音频 |
| review_analysis_upload_file_record.db | SQLiteHelperUploadFileRecord | 上传文件录制 |

## 核心实体

详见 `[entity_models.md]` — VideoEntity (34 字段)、VideoSliceEntity (15 字段)、UploadFileEntity (24 字段)

主播实体详见 `[../domain/anchor_live.md]` — AnchorEntity (40+ 字段)

## 数据访问模式

- **ORM**: Entity Framework 6 (用于部分场景)
- **直接 SQL**: SQLiteHelper 系列类 (主要方式)
- **命名转换**: PascalCase (C# 属性) ↔ snake_case (数据库列名)
- **表结构检查**: 使用 `PRAGMA table_info(tableName)` 获取列信息

## 平台数据文件存储

除 SQLite 外，平台采集数据以文件形式存储：

| 路径 | 说明 |
|---|---|
| `dataCollect\juliang\realTime\` | 巨量实时数据 |
| `dataCollect\juliang\finish\` | 巨量完成数据 |
| `dataCollect\AnchorLive\realTime\` | 主播后台实时数据 (JSONL) |
| `dataCollect\platform\` | 统一平台数据 (PlatformDataManager) |
| `dataCollect\uploaded\{platform}Up\{date}\` | 已上传归档 |
| `dataCollect\config\` | Cookie 等配置文件 |
