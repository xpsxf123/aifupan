# 业务功能清单 - AI域

> 模块: replay-ai | 路径前缀: replay/ai/*, replay/conversation/*, replay/diagnosis/*

## 一、AI对话

| 功能 | 说明 | 关键类 |
|------|------|--------|
| 保存对话数据 | 保存问答记录，后置更新诊断报告状态 | ConversationBll.saveConversationData |
| 对话后置处理 | 识别内容诊断/数据诊断，更新视频详情 | ConversationBll.saveConversationDataPost |
| 数据导出 | 支持单文件Markdown/多文件ZIP导出 | ConversationBll.exportByQaCode |

## 二、HTML报告生成

| 功能 | 说明 | 关键类 |
|------|------|--------|
| 生成HTML | 前置检查→异步AI生成→上传OSS→更新状态 | ConversationBll.serviceGenerateHtml |
| 生成超时检查 | 20分钟超时自动标记为失败 | ConversationBll |

## 三、AI内容纠正

| 功能 | 说明 | 关键类 |
|------|------|--------|
| AI纠正 | 前置检查→异步AI纠正→合并内容→更新状态 | ConversationBll.serviceCorrectAiContent |
| 纠正超时检查 | 20分钟超时自动标记为失败 | ConversationBll |

## 四、诊断功能

| 功能 | 说明 |
|------|------|
| 诊断管理 | 诊断报告的管理 |
| 诊断状态 | 内容诊断和数据诊断状态追踪 |

## 五、自定义提示词

| 功能 | 说明 |
|------|------|
| 自定义提示词CRUD | 用户自定义AI提示词管理 |
| 租户定制提示词 | 租户级别的提示词定制 |

## 六、分享链接

| 功能 | 说明 |
|------|------|
| 分享链接记录 | AI对话分享链接的记录管理 |

## 七、AI模型管理

| 功能 | 说明 |
|------|------|
| AI模型CRUD | AI模型配置的增删改查 |
| 模型策略 | 不同场景使用不同AI模型的策略 |
| Token消耗记录 | AI Token使用量记录 |

**AI模型列表**:
- doubao1.5-pro-32K（豆包）
- deepseek-r1（DeepSeek）
- thinking-pro（思考模型）
