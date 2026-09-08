# AI 与分析域 (AI & Analysis)

## 概述

系统集成 AI 大模型能力，提供直播内容智能分析、数据诊断报告生成、语音转文字等功能。AI 后端使用火山引擎 Ark (豆包) 模型。

## 核心概念

### ChatCompletions (对话补全)
LLM 大模型对话接口，支持两种模式：

**标准模式** (`ChatCompletionsAsk`):
- 无上下文关联的单轮对话
- 通过 `AiUtils.chatCompletions()` 调用
- 截断超长问题至模型限制

**上下文模式** (`ContextChatCompletionsAsk`):
- 带上下文的多轮对话
- 通过 `AiUtils.CreateContext()` 创建上下文
- 通过 `AiUtils.contextChatCompletions()` 调用
- 记录 Token 使用量并更新上下文元数据

**公共流程** (`AbstractAsk`):
1. 构建语句标记和提示词
2. 校验 AI Token 预算
3. 添加额外内容和占位符
4. 执行模型调用
5. 处理 SSE 流式返回
6. 保存会话和使用记录

**关键文件**:
- `Ai/impl/ChatCompletionsAsk.cs`
- `Ai/impl/ContextChatCompletionsAsk.cs`
- `Ai/impl/AbstractAsk.cs`

### AiFactory (AI 工厂)
AI 服务工厂类，负责根据请求类型创建对应的 Ask 实例。

**文件**: `Ai/AiFactory.cs`

### Diagnosis (数据诊断)
AI 驱动的直播数据诊断报告，基于 `AbstractDiagnosis` 基类构建。

**流程**:
1. 获取视频/直播数据
2. 构建诊断提示词和问题
3. 调用 DiagnosisApi / VideoApi / CueWordsApi
4. 生成诊断报告

**关键文件**:
- `Ai/impl/diagnosis/AbstractDiagnosis.cs`
- `Ai/impl/diagnosis/` 目录

### AiAutoTimer (自动化调度)
后台自动化调度器，定时执行 AI 分析任务。

**任务类型**:
- 诊断任务
- 视频分析任务
- HTML 报告生成
- 纠错任务

**文件**: `Ai/AiAutoTimer.cs`

### AI 数据模型

**CompletionsDto** (`Ai/Model/CompletionsDto.cs`):
- choices / delta / finish_reason
- request_id / usage / total_tokens

**ConversationDto** (`Ai/Model/ConversationDto.cs`):
- source / userId / tenantId
- askType / cueWords / contextId
- content / correction state / timestamps

## ASR 语音识别

### AsrUtils
集成腾讯云 ASR 服务，批量处理音频文件。

**工作流程**:
1. 扫描音频文件目录
2. 获取临时 ASR 凭证 (`AsrApi.GetTempToken`)
3. 信号量控制并发执行识别任务
4. 聚合识别结果
5. 按文件名排序，分配段落编号
6. 从词级结果重建可读文本

**文件**: `Asr/AsrUtils.cs`

**配置**:
- `engSerViceType` 字段控制 ASR 引擎模型，如 `16k_zh`

## 短视频分析 (ShortVideo/)

### DouyinSpider
抖音视频爬虫，按 aweme_id 搜索和获取视频信息。

**文件**: `ShortVideo/DouYin/DouyinSpider.cs`

### ShortVideoService
短视频服务层，提供视频搜索、分析等业务功能。

**文件**: `ShortVideo/Servier/ShortVideoService.cs`
