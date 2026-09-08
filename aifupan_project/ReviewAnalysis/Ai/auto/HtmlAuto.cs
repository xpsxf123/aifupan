using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Web.UI.WebControls;
using douyin.Utils;
using ReviewAnalysis.Ai.config;
using ReviewAnalysis.Ai.Model;
using ReviewAnalysis.api;
using ReviewAnalysis.bo.video;
using ReviewAnalysis.Dto;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.enums;
using ReviewAnalysis.upload;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo;
using ReviewAnalysis.vo.ai;
using ReviewAnalysis.vo.system;
using ReviewAnalysis.vo.video;
using ReviewAnalysis.Websocket;
using static COSXML.Model.Tag.ListBucket;

namespace ReviewAnalysis.Ai.auto
{
    public class HtmlAuto
    {
        public static volatile ThreadSafeList<HtmlStatusDto> dataQueue = new ThreadSafeList<HtmlStatusDto>();

        // 最大并发线程数，默认值Default value
        private static int maxConcurrentTasks = 10;

        // 用于控制并发任务数的信号量
        private static SemaphoreSlim semaphore = new SemaphoreSlim(maxConcurrentTasks);
        private static readonly object _lockAdd = new object();
        private static readonly object _lockStartOne = new object();

        /// <summary>
        /// 把全部的待分析的自然、优化原文重新set到队列中
        /// </summary>
        /// <returns></returns>
        public static async Task initPut()
        {
            try
            {
                List<HtmlStatusDto> HtmlStatusDtos = new List<HtmlStatusDto>();

                // 在添加到队列中
                addAll(HtmlStatusDtos);
            }
            catch (Exception e)
            {
                FileUtils.LogError($"初始化自动处理时出错: {e.Message}, 堆栈={e.StackTrace}", "初始化自动处理html时出错");
                
            }
            
        }

        /// <summary>
        /// 手动生成原文
        /// </summary>
        /// <param name="bo"></param>
        public static ConversationVo add(string id)
        {
            if (string.IsNullOrEmpty(id)) CustomException.create("id不能为空", 3001);

            ConversationVo to = AiRelatedApi.htmlProGenerateParams(id);

            if (to != null)
            {
                addAll(new List<HtmlStatusDto>() { 
                    new HtmlStatusDto() 
                    {
                        id = to.id,
                        content = to.content,
                        status = 0
                    } 
                });
            }
            to.content = null;
            return to;
        }

        /// <summary>
        /// 执行一次自动向ai提问
        /// </summary>
        /// <param name="timeoutIndex"></param>
        /// <returns></returns>
        public static async Task startOne(int timeoutIndex)
        {
            HtmlStatusDto HtmlStatusDto = null;
            // 没有并发的问题，就谁便锁下
            lock (_lockStartOne)
            {
                if (!dataQueue.IsNotEmpty())
                {
                    return;
                }

                // 判断是否有要执行的数据
                List<HtmlStatusDto> HtmlStatusDtos = dataQueue.GetAll();
                if (HtmlStatusDtos == null || HtmlStatusDtos.Count <= 0)
                {
                    return;
                }
                IEnumerable<HtmlStatusDto> enumerable = HtmlStatusDtos.Where(item => item.status == 0);
                if (enumerable == null || enumerable.Count() <= 0)
                {
                    return;
                }
                HtmlStatusDto = enumerable.First();

                if (HtmlStatusDto == null)
                {
                    return;
                }
            }

            if (HtmlStatusDto == null)
            {
                return;
            }

            // 尝试等待信号量，确保不超过最大并发数。如果信号量已满，则直接返回，不等待
            bool acquired = await semaphore.WaitAsync(0);
            if (!acquired)
            {
                // 信号量已满，当前不处理，直接返回
                //FileUtils.LogError("信号量已满，当前并发数已达上限，跳过本次处理", "AI处理并发控制");
                return;
            }

            try
            {
                // 启动一个任务处理项目
                _ = Task.Run(async () =>
                {
                    try
                    {
                        await run(HtmlStatusDto);
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogError($"处理项目时出错: {ex.Message}, 堆栈={ex.StackTrace}", "AI处理错误");
                    }
                    finally
                    {
                        // 释放信号量，允许新的任务启动
                        semaphore.Release();
                    }
                });
            }
            catch (Exception ex)
            {
                semaphore.Release();
                FileUtils.LogError($"启动处理任务时出错: {ex.Message}, 堆栈={ex.StackTrace}", "AI处理错误");
            }
        }


        /// <summary>
        /// 添加到队列中，等待执行
        /// </summary>
        /// <param name="toGeneratedList"></param>
        public static void addAll(List<HtmlStatusDto> toGeneratedList)
        {
            lock (_lockAdd)
            {
                try
                {
                    if (toGeneratedList != null && toGeneratedList.Count > 0)
                    {
                        foreach (HtmlStatusDto item in toGeneratedList)
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

        public static async Task run(HtmlStatusDto vo)
        {
            int status = 0;
            string error = "";
            string htmlSavePath = "";
            try
            {
                status = 1;
                // 修改队列中对应元素的状态为1（处理中）
                dataQueue.Update(v => v.id.Equals(vo.id), item => item.status = 1);

                if (string.IsNullOrEmpty(vo.content))
                {
                    throw new CustomException("内容为空，不能生成图表", 7005);
                }

                AiTempTokenDto token = AiRelatedApi.getHtmlTempToken();
                if (token == null)
                {
                    throw new CustomException("获取ai模型配置失败", 7005);
                }
                AskRequestDto dto = new AskRequestDto();
                dto.identity = "你是一个html前端专家";
                
                FileUtils.log(vo.content, "生成html提示词", true);
                var service = AiFactory.GetAiChatService(token.resourceType);
                CompletionsDto completions = await service.ChatCompletionStream(token, dto, vo.content, null, null);


                if (completions.status == 0)
                {
                    // 保存到aiToken使用记录中
                    addAiTokenUseRecord((int)AiUseSourceType.ai问答, vo.id, (int)CueType.生成html, token.modelDefinition, completions.choices.requestId, completions?.choices?.finishReason??"", "", completions.usage);

                    // 保存到oss中和本地中
                    SignUploadUrlVo sv = AiRelatedApi.getHtmlSignUploadUrl();

                    if (sv == null)
                    {
                        throw new CustomException("上传数据失败", 7005);
                    }

                    string savePath = WebsocketDataHandle.savePath + "/" + sv.ossKey;

                    string directory = Path.GetDirectoryName(savePath);
                    Directory.CreateDirectory(directory);

                    File.WriteAllText(savePath, completions?.choices?.onlyMessage ?? "");

                    bool uploadFlag = UploadUtils.UploadFileAsync(sv.signedUrl, savePath, "text/html");
                    if (!uploadFlag)
                    {
                        throw new CustomException("上传数据到服务器失败");
                    }
                    htmlSavePath = sv.ossKey;
                }
                else
                {
                    FileUtils.LogError($"发送失败id={vo.id}", "生成html提问时发送失败");
                    throw new CustomException("发送失败", 7005);
                }

                status = 2;

            }
            catch (Exception ex)
            {
                status = 3;
                FileUtils.LogError($"run方法异常: {ex.Message}, 堆栈={ex.StackTrace}", "AI视频内容处理错误");
                error = ex.Message;
            }
            finally
            {
                // 修改总的状态
                AiRelatedApi.updateHtmlStatus(vo.id, status, htmlSavePath, error);
                // 更新dataQuery
                dataQueue.Delete(vo);
            }
        }

        /// <summary>
        /// 保存aitoken记录
        /// </summary>
        /// <param name="sourceType"></param>
        /// <param name="id"></param>
        /// <param name="type"></param>
        /// <param name="modelName"></param>
        /// <param name="requestId"></param>
        /// <param name="finishReason"></param>
        /// <param name="remarks"></param>
        /// <param name="usage"></param>
        public static void addAiTokenUseRecord(int sourceType, string id, int type, string modelName, string requestId, string finishReason, string remarks, Usage usage)
        {
            AiTokenUseRecordVo recordVo = new AiTokenUseRecordVo();
            recordVo.useSourceType = sourceType;
            recordVo.useSourceId = id;
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
        /// 停止处理
        /// </summary>
        public static void Stop()
        {
            
        }

        /// <summary>
        /// 更新最大并发任务数
        /// </summary>
        public static async Task UpdateMaxConcurrentTasks()
        {
            try
            {
                maxConcurrentTasks = KvHelper.GetIntKvByKey("html_concurrency_level", maxConcurrentTasks);
                // 如果已经初始化了信号量，则更新它
                if (semaphore == null)
                {
                    semaphore = new SemaphoreSlim(maxConcurrentTasks);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError(ex.Message, "获取html_concurrency_level键失败");
            }

        }
    }
}
