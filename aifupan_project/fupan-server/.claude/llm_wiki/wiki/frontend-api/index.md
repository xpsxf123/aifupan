# Frontend API 文档索引

前后端接口契约文档（面向前端消费方）。所有接口均遵循统一响应封装 R\<T\> 规范，鉴权通过 JWT。

## 公共规范

| 文件 | 说明 |
|---|---|
| [统一响应封装](./_response_envelope.md) | `R<T>` 外层结构（code / msg / data）；所有接口共用，不在各模块文档重复 |
| [全局错误码](./_error_codes.md) | 项目级错误码集中维护；各模块业务错误码同时在模块文档内列出 |

## 模块接口文档

| 模块 | 文件 | 状态 | 说明 |
|---|---|---|---|
| 话术智能监控 | [script_monitor.md](./script_monitor.md) | B4/B9 已实现；B3/B5 TBD | 主文档（14 个接口总表 + B4/B9 完整契约）；972 行，LIBRARIAN-NOTE 已标注待 B3+B5 落地后拆分 |
| 话术智能监控（B5 批次实现版） | [script_monitor_b5.md](./script_monitor_b5.md) | B5 已实现 | anchorBasicConfig（secUid 入参版）+ monitorPositionStatistics；与主文档的 TBD 预告章节并行存在，B5 实装的权威参考 |
| 视频 ROI 与曲线数据 | [video_roi_curve.md](./video_roi_curve.md) | 已实现 | 新增 getVideoRoi 接口；getOnlineAnalysis（Java 服务端）和 lockanalysis（C# 客户端）新增千川消耗/净成交 ROI 曲线字段 |
| 用户管理（power） | [power.md](./power.md) | 待实现 | `POST /user/pageListNew` 用户列表；新增算力消耗（用户/租户，T+1 离线净额）与最新跟进三只读字段 |
| 保真度（fidelity） | [fidelity.md](./fidelity.md) | 已实现 | 此前未收录索引，本次补登 |
| SEO 内容管理 | [seo_content.md](./seo_content.md) | 已实现 | 官网文章/分类/标签三套 CRUD + Markdown 批量导入；22 个接口，无新增错误码 |
