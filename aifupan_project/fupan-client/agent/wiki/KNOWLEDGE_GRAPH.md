# KNOWLEDGE_GRAPH（知识图谱入口）

本页是 agent/wiki 的导航根节点，用于把“可复用知识”组织成可检索的图谱入口。

## 使用方式（推荐）
- 查入口：优先从本页进入对应域的 `index.md`
- 写增量：先写到对应域的 `wal/`（append-only），再按窗口合并到域 `index.md`
- 控制膨胀：域 `index.md` 只做“摘要 + 链接”，细节下沉到独立 wiki 页面

## Domains（推荐分区）
- API：`api/index.md`
- Router：`router/index.md`
- State：`state/index.md`
- Components：`components/index.md`
- Preferences：`preferences/index.md`
- Specs：`specs/index.md`
- Archive：`archive/index.md`

## Legacy Pages（历史专题入口）
- 项目-复盘客户端-架构概览
- 项目-复盘客户端-环境与构建
- 项目-复盘客户端-路由与菜单体系
- 项目-复盘客户端-请求与接口体系
- 项目-复盘客户端-状态管理
- 项目-复盘客户端-CSharp推送体系
- 项目-复盘客户端-组件体系
- 项目-复盘客户端-组件资源清单
- 项目-复盘客户端-需求版本管理
- 项目-复盘客户端-资源拦截与二维码弹窗
- 项目-复盘客户端-表格批量删除（table mixin）
- 项目-复盘客户端-分支管理策略

## Governance（治理入口）
- 通用-上下文漏斗与写回
- 通用-WAL与防膨胀规则
- 通用-生命周期与门禁
- 通用-意图路由与执行档位
- 对话记忆与蒸馏摘要
- 文件重名检测与文件清单
