using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using douyin.Utils;
using ReviewAnalysis.Ai.config;
using ReviewAnalysis.Ai.Model;
using ReviewAnalysis.api;
using ReviewAnalysis.vo.video;
using ReviewAnalysis.Utils;
using ReviewAnalysis.Ai.auto;
using ReviewAnalysis.Dto;
using ReviewAnalysis.vo.ai;
using ReviewAnalysis.bo.video;
using ReviewAnalysis.vo.system;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.enums;

namespace ReviewAnalysis.Ai
{
    /// <summary>
    /// 自然原文/优化原文 自动生成模块
    ///
    /// 整体流程：
    /// 1. 视频分析完成后，服务端返回分段列表（如14段），每段有一个 cueWord 提示词
    /// 2. 所有段并行调用 AI 流式接口生成内容
    /// 3. 每段生成后保存到 MongoDB，全部完成后通知服务端
    ///
    /// 并发控制：
    /// - 使用 SemaphoreSlim 信号量控制同时处理的批次数量（默认1，可通过配置动态调整）
    /// - AiAutoTimer 定时器每 ~15秒 调用 startOne()，信号量满时直接跳过
    /// - startOne() 内部 fire-and-forget 启动后台任务，不阻塞定时器循环
    /// - 每个后台任务处理一个完整批次（一个 sourceId + type 下的所有段）
    ///
    /// 超时与重试：
    /// - 每批处理有 10分钟超时，超时后 AI 调用被取消
    /// - 失败的段最多重试 2 次，只重试失败的段而非全部
    /// - 重试耗尽后仍通知服务端，避免 UI 永远显示"生成中"
    /// </summary>
    public class VideoContentAuto
    {
        /// <summary>待处理的生成任务队列，线程安全</summary>
        public static volatile ThreadSafeList<ToGeneratedVo> dataQueue = new ThreadSafeList<ToGeneratedVo>();

        /// <summary>最大并发批次数，默认1。可通过系统配置 video_content_concurrency_level 动态调整</summary>
        private static int maxConcurrentTasks = 1;

        /// <summary>信号量，控制同时处理的批次数量。获取到信号量才能开始处理，处理完释放</summary>
        private static SemaphoreSlim semaphore = new SemaphoreSlim(maxConcurrentTasks);
        private static readonly object _lockAdd = new object();
        private static readonly object _lockStartOne = new object();

        /// <summary>
        /// 全局取消令牌源，用于 Stop() 方法取消所有正在执行的批次。
        /// run() 内部通过 CreateLinkedTokenSource 关联此令牌，Cancel 后所有批次的 AI 调用都会被取消。
        /// </summary>
        private static CancellationTokenSource globalCts = new CancellationTokenSource();

        /// <summary>
        /// 应用启动时调用，从服务端拉取所有"待生成"和"生成中"的自然/优化原文，
        /// 重新加入本地队列继续处理（用于重启后恢复未完成的任务）
        /// </summary>
        public static async Task initPut()
        {
            try
            {
                List<ToGeneratedVo> toGeneratedVos = await VideoContentApi.contentByToGenerated();

                // 在添加到队列中
                addAll(toGeneratedVos);
            }
            catch (Exception e)
            {
                
                FileUtils.LogError($"初始化待生成原文时出错: {e.Message}, 堆栈={e.StackTrace}", "初始化待生成原文时出错");
            }
            
        }

        /// <summary>
        /// 手动触发原文生成。
        /// 调用服务端接口获取分段列表，然后加入本地队列等待处理。
        /// </summary>
        /// <param name="bo">sourceId: 视频ID, sourceType: 来源类型, type: 1=自然原文 2=优化原文</param>
        public static async Task add(GenerateVideoContentBo bo)
        {
            if (string.IsNullOrEmpty(bo.sourceId)) CustomException.create("sourceId不能为空", 3001);

            try
            {
                ToGeneratedVo to = await VideoContentApi.generateOntQAContentAsync(bo.sourceId, bo.sourceType, bo.type);

                if (to != null)
                {
                    addAll(new List<ToGeneratedVo>() { to });
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"生成原文失败: {ex.Message}", "VideoContentAuto.add");
            }
        }

        /// <summary>
        /// 视频分析完成后自动触发原文生成。
        /// 分别生成自然原文（type=1）和优化原文（type=2）两个批次。
        /// </summary>
        /// <param name="video">视频实体</param>
        public static async Task generateContent(VideoEntity video)
        {
            bool flag = await VideoContentApi.hasOneVideo(video.videoId);
            if (flag)
            {
                // 自然原文：type=1
                GenerateVideoContentBo one = new GenerateVideoContentBo();
                one.sourceId = video.videoId;
                one.sourceType = 0;
                one.type = 1;
                await add(one);

                // 优化原文：type=2
                GenerateVideoContentBo two = new GenerateVideoContentBo();
                two.sourceId = video.videoId;
                two.sourceType = 0;
                two.type = 2;
                await add(two);
            }
        }

        /// <summary>
        /// 定时器入口，由 AiAutoTimer 每 ~15秒 调用一次。
        /// 从队列中取出一个待处理的批次，通过信号量控制并发，启动后台任务处理。
        ///
        /// 为什么用 fire-and-forget（_ = Task.Run）而不是 await？
        /// - startOne 被定时器循环调用，如果 await 整个 run()，10分钟的 AI 调用会阻塞定时器，
        ///   导致 DiagnosisAuto、HtmlAuto、CorrectAiContentAuto 等其他模块无法被轮询
        /// - 信号量（maxConcurrentTasks=1）已经保证同一时间只有一个批次在运行
        /// - run() 内部有 10分钟超时保护，不会无限挂起导致信号量泄漏
        /// </summary>
        /// <param name="timeoutIndex">超时计数器索引（当前未使用）</param>
        public static async Task startOne(int timeoutIndex)
        {
            ToGeneratedVo toGeneratedVo = null;
            // 从队列中取出第一个 status=0（待处理）的批次，并立即标记为处理中防止重复取出
            lock (_lockStartOne)
            {
                if (!dataQueue.IsNotEmpty())
                {
                    return;
                }

                List<ToGeneratedVo> toGeneratedVos = dataQueue.GetAll();
                if (toGeneratedVos == null || toGeneratedVos.Count <= 0)
                {
                    return;
                }
                IEnumerable<ToGeneratedVo> enumerable = toGeneratedVos.Where(item => item.status == 0);
                if (enumerable == null || enumerable.Count() <= 0)
                {
                    return;
                }
                toGeneratedVo = enumerable.First();

                if (toGeneratedVo == null)
                {
                    return;
                }

                // 立即标记为处理中，防止 maxConcurrentTasks>1 时下一轮 startOne() 取出同一批次
                toGeneratedVo.status = 1;
            }

            if(toGeneratedVo == null)
            {
                return;
            }

            // 尝试获取信号量，WaitAsync(0) 表示不等待，获取不到直接返回
            // 这保证了同时只有一个批次在 AI 调用中（取决于 maxConcurrentTasks 的值）
            bool acquired = await semaphore.WaitAsync(0);
            if (!acquired)
            {
                // 回退状态，下次轮询可以重新尝试
                toGeneratedVo.status = 0;
                return;
            }

            try
            {
                // fire-and-forget：启动后台任务后立即返回，不阻塞定时器循环
                // 信号量在 finally 中释放，run() 有超时保护保证 finally 一定执行
                _ = Task.Run(async () =>
                {
                    try
                    {
                        await run(toGeneratedVo);
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogError($"处理项目时出错: {ex.Message}, 堆栈={ex.StackTrace}", "AI处理错误");
                    }
                    finally
                    {
                        // 无论成功、失败还是超时，都释放信号量，保证后续任务可以继续
                        semaphore.Release();
                    }
                });
            }
            catch (Exception ex)
            {
                // Task.Run 本身失败时释放信号量（极少发生）
                semaphore.Release();
                FileUtils.LogError($"启动处理任务时出错: {ex.Message}, 堆栈={ex.StackTrace}", "AI处理错误");
            }
        }


        /// <summary>
        /// 批量添加待生成的批次到队列。
        /// 线程安全，通过 _lockAdd 锁保护。
        /// </summary>
        /// <param name="toGeneratedList">待添加的批次列表，每个元素包含一个 sourceId+type 下的所有段</param>
        public static void addAll(List<ToGeneratedVo> toGeneratedList)
        {
            lock (_lockAdd)
            {
                try
                {
                    if (toGeneratedList != null && toGeneratedList.Count > 0)
                    {
                        foreach (ToGeneratedVo item in toGeneratedList)
                        {
                            dataQueue.Add(item);
                        }
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"message={ex.Message}, 堆栈={ex.StackTrace}", "批量添加自的然、优化原文到队列中是失败");
                }
            }
        }

        /// <summary>
        /// 处理一个批次的所有段。
        ///
        /// 核心流程：
        /// 1. 将所有段并行调用 AI（通过 ProcessOneSegment）
        /// 2. 每段有独立超时（共 10分钟），超时后该段的 AI 调用被取消
        /// 3. 收集失败的段，最多重试 2 次（只重试失败的段，成功的段不重复处理）
        /// 4. 重试耗尽后，无论成败都调用 checkVideoContent 通知服务端，避免 UI 永远卡在"生成中"
        /// 5. 从队列中删除此批次
        ///
        /// 超时机制：
        /// - 使用 CreateLinkedTokenSource 关联 globalCts，所以 Stop() 可以取消所有正在执行的批次
        /// - CancelAfter(10分钟) 设置超时，超时后 CancellationToken 变为已取消状态
        /// - 已取消的 token 传到 DoubaoChatService/DeepSeekChatService 后，
        ///   CancelPendingRequests() 会断开 HTTP 连接，ReadLineAsync 立即抛出异常
        /// </summary>
        /// <param name="vo">待处理的批次，包含 sourceId、type、分段列表等信息</param>
        public static async Task run(ToGeneratedVo vo)
        {
            const int maxRetries = 2;
            const int timeoutMinutes = 10;

            try
            {
                // status 已在 startOne() 的锁内设置为 1，此处无需重复设置

                List<VideoContentVo> list = vo.videoContentList;
                if (list == null || list.Count <= 0)
                {
                    return;
                }

                int retryCount = 0;
                // ConcurrentBag：线程安全的集合，多个并行 Task 可以同时添加失败段
                ConcurrentBag<VideoContentVo> failedSegments = new ConcurrentBag<VideoContentVo>();
                List<VideoContentVo> currentBatch = list;

                // 重试循环：首次处理 + 最多 maxRetries 次重试
                while (retryCount <= maxRetries)
                {
                    failedSegments = new ConcurrentBag<VideoContentVo>();

                    // 创建关联 globalCts 的超时令牌：10分钟后自动取消，或 Stop() 时手动取消
                    using (CancellationTokenSource cts = CancellationTokenSource.CreateLinkedTokenSource(globalCts.Token))
                    {
                        cts.CancelAfter(TimeSpan.FromMinutes(timeoutMinutes));

                        // 所有段并行执行，每段独立调用 AI
                        List<Task> tasks = new List<Task>();
                        foreach (VideoContentVo contentVo in currentBatch)
                        {
                            tasks.Add(ProcessOneSegment(vo, contentVo, cts.Token, failedSegments));
                        }
                        // 等待所有段完成（或超时取消）
                        await Task.WhenAll(tasks);
                    }

                    // 全部成功，退出重试循环
                    if (failedSegments.IsEmpty) break;

                    retryCount++;
                    if (retryCount <= maxRetries)
                    {
                        // 下一轮只重试失败的段
                        currentBatch = failedSegments.ToList();
                        FileUtils.log($"第{retryCount}次重试，共{currentBatch.Count}段", "AI视频内容处理重试");
                    }
                }

                if (!failedSegments.IsEmpty)
                {
                    FileUtils.LogError($"共{failedSegments.Count}段处理失败，已重试{maxRetries}次", "AI视频内容处理部分失败");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"run方法异常: {ex.Message}, 堆栈={ex.StackTrace}", "AI视频内容处理错误");
            }
            finally
            {
                try
                {
                    // 通知服务端此批次处理完成（无论成败），避免 UI 永远显示"生成中"
                    await VideoContentApi.checkVideoContent(vo.sourceId, vo.sourceType, vo.type);
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"checkVideoContent失败: {ex.Message}", "AI视频内容处理");
                }
                // 从队列中移除此批次（checkVideoContent 失败也不影响清理）
                dataQueue.Delete(vo);
            }
        }

        /// <summary>
        /// 处理单个段的 AI 生成。
        ///
        /// 流程：
        /// 1. 检查取消令牌（超时或 Stop() 时快速失败）
        /// 2. 获取 AI 临时 token（为 null 则抛异常，该段标记失败进入重试）
        /// 3. 调用 AI 流式接口 ChatCompletionStream，传入 CancellationToken
        ///    - 正常完成：保存内容到 MongoDB
        ///    - 超时取消：DoubaoChatService/DeepSeekChatService 中的 CancelPendingRequests 断开连接
        /// 4. 异常处理：
        ///    - OperationCanceledException → 超时，加入失败列表等待重试
        ///    - 其他异常 → 加入失败列表等待重试
        ///
        /// 注意：此方法不抛异常，所有异常都在内部捕获并记录到 failedSegments
        /// </summary>
        /// <param name="vo">批次信息（sourceId、type、aiModel 等）</param>
        /// <param name="contentVo">当前段（cueWord=提示词, id=段ID）</param>
        /// <param name="ct">取消令牌，超时或 Stop() 时变为已取消</param>
        /// <param name="failedSegments">线程安全的失败段收集器</param>
        private static async Task ProcessOneSegment(ToGeneratedVo vo, VideoContentVo contentVo, CancellationToken ct, ConcurrentBag<VideoContentVo> failedSegments)
        {
            try
            {
                // 开始前先检查是否已被取消（例如上一轮重试中其他段超时导致整个 CTS 取消）
                ct.ThrowIfCancellationRequested();

                AskRequestDto dto = new AskRequestDto();
                dto.aiModel = vo.aiModel;

                // 获取 AI 临时 token，为 null 时直接抛异常（之前是空 if 块导致 NRE）
                AiTempTokenDto token = AiRelatedApi.getAnalysisTempToken(vo.aiModel);
                if (token == null)
                {
                    throw new CustomException("获取临时token失败", 7006);
                }

                FileUtils.log($"处理VideoContentVo: {contentVo?.ToString()}", "AI视频内容处理");
                FileUtils.log($"使用的模型为：{token.modelDefinition}", "使用的模型", true);

                // cueWord 是服务端生成的提示词，直接作为 AI 提问内容
                string askQuestion = contentVo.cueWord;
                FileUtils.log($"{askQuestion}", "真实提问问题", true);

                // 根据 token 的 resourceType 获取对应的 AI 服务（豆包/DeepSeek）
                var service = AiFactory.GetAiChatService(token.resourceType);

                // 流式调用 AI，传入 ct 以支持超时取消
                CompletionsDto completions = await service.ChatCompletionStream(token, dto, askQuestion, null, null, ct);

                if (completions.status == 0)
                {
                    // 记录 AI token 使用量
                    addAiTokenUseRecord(vo.sourceType, vo.sourceId, vo.type == 1 ? 7 : 8, token.modelDefinition, completions.choices.requestId, "", "", completions.usage);

                    // AI 内容纠正（检查并修正不完整的 JSON/格式问题）
                    string aiContent = await aiContentCorrect(contentVo.id, completions.choices?.onlyMessage ?? "");

                    // 保存生成的内容到 MongoDB
                    await VideoContentApi.saveVideoContent(contentVo.id, aiContent);
                }
                else
                {
                    throw new CustomException("发送失败", 7005);
                }
            }
            catch (OperationCanceledException)
            {
                // 超时或 Stop() 导致的取消 → 记录到失败列表，等待重试
                failedSegments.Add(contentVo);
                FileUtils.LogError($"段 {contentVo.id} AI调用超时被取消", "AI视频内容处理超时");
            }
            catch (Exception ex)
            {
                // 其他异常（网络错误、token 为 null、AI 返回异常等）→ 记录到失败列表，等待重试
                failedSegments.Add(contentVo);
                FileUtils.LogError($"处理VideoContentVo出错: {ex.Message}, 堆栈={ex.StackTrace}", "AI视频内容处理错误");
            }
        }

        /// <summary>
        /// ai内容纠正
        /// </summary>
        /// <param name="id"></param>
        /// <param name="content"></param>
        /// <returns></returns>
        public static async Task<string> aiContentCorrect(string id, string content)
        {
            // 判断是否需求纠正，不用就直接返回原值
            if (string.IsNullOrEmpty(content) || !CorrectAiContentAuto.getCheckAiContent(content, id, (int)AiUseSourceType.原文, (int)ContentCorrectionSceneType.自然优化原文的纠正检查))
            {
                return content;
            }

            return await CorrectAiContentAuto.generateAiContentSplice(id, (int)AiUseSourceType.原文, content, (int)ContentCorrectionSceneType.自然优化原文的纠正);
        }

        /// <summary>
        /// 保存aitoken记录
        /// </summary>
        /// <param name="sourceType"></param>
        /// <param name="sourceId"></param>
        /// <param name="type"></param>
        /// <param name="modelName"></param>
        /// <param name="requestId"></param>
        /// <param name="finishReason"></param>
        /// <param name="remarks"></param>
        /// <param name="usage"></param>
        public static void addAiTokenUseRecord(int sourceType, string sourceId, int type, string modelName, string requestId, string finishReason, string remarks, Usage usage)
        {
            AiTokenUseRecordVo recordVo = new AiTokenUseRecordVo();
            recordVo.useSourceType = sourceType;
            recordVo.useSourceId = sourceId;
            recordVo.assistantType = type;
            recordVo.modelName = modelName;
            recordVo.requestId = requestId;
            recordVo.finishReason = finishReason;
            recordVo.remarks = remarks;
            recordVo.promptTokens = usage.promptTokens;
            recordVo.completionTokens = usage.completionTokens;
            recordVo.cachedTokens = usage.promptTokensDetails?.cached_tokens ?? 0;
            recordVo.totalTokens = usage.totalTokens;
            AiTokenUseRecordVo result = AiTokenUseRecordApi.saveAiTokenUseRecord(recordVo);
        }

        /// <summary>
        /// 停止所有正在执行的批次处理。
        ///
        /// 原理：
        /// - globalCts 是所有批次的父级 CancellationTokenSource
        /// - run() 中通过 CreateLinkedTokenSource(globalCts.Token) 创建子令牌
        /// - Cancel() globalCts 后，所有子令牌都会变为已取消状态
        /// - 已取消的令牌导致 DoubaoChatService/DeepSeekChatService 中的 HTTP 连接被断开
        /// - 断开后 ReadLineAsync 立即抛异常，批次的 finally 正常执行，信号量释放
        /// - 然后创建新的 globalCts 供后续批次使用
        /// </summary>
        public static void Stop()
        {
            try
            {
                globalCts?.Cancel();
                globalCts?.Dispose();
                globalCts = new CancellationTokenSource();
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"停止处理时出错: {ex.Message}", "VideoContentAuto.Stop");
            }
        }

        /// <summary>
        /// 从系统配置读取最大并发批次数（video_content_concurrency_level）。
        /// 不动态替换 SemaphoreSlim，仅在 semaphore 为 null 时创建。
        /// </summary>
        public static Task UpdateMaxConcurrentTasks()
        {
            try
            {
                maxConcurrentTasks = KvHelper.GetIntKvByKey("video_content_concurrency_level", maxConcurrentTasks);
                if (semaphore == null)
                {
                    semaphore = new SemaphoreSlim(maxConcurrentTasks);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError(ex.Message, "获取video_content_concurrency_level键失败");
            }
            return Task.CompletedTask;
        }
    }
}