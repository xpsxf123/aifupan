using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using System.Web.UI.WebControls;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Ai.config;
using ReviewAnalysis.Ai.Model;
using ReviewAnalysis.api;
using ReviewAnalysis.bo.video;
using ReviewAnalysis.Dto;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.enums;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo;
using ReviewAnalysis.vo.ai;
using ReviewAnalysis.vo.system;
using ReviewAnalysis.vo.video;

namespace ReviewAnalysis.Ai.auto
{
    /// <summary>
    /// 纠正AI内容自动处理类
    /// </summary>
    public class CorrectAiContentAuto
    {
        public static volatile ThreadSafeList<HtmlStatusDto> dataQueue = new ThreadSafeList<HtmlStatusDto>();

        // 最大并发线程数，默认值
        private static int maxConcurrentTasks = 10;

        // 用于控制并发任务数的信号量
        private static SemaphoreSlim semaphore = new SemaphoreSlim(maxConcurrentTasks);
        private static readonly object _lockAdd = new object();
        private static readonly object _lockStartOne = new object();

        /// <summary>
        /// 把全部的待纠正的AI内容重新set到队列中
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
                FileUtils.LogError($"初始化自动处理时出错: {e.Message}, 堆栈={e.StackTrace}", "初始化自动纠正AI内容时出错");
                
            }
            
        }

        /// <summary>
        /// 手动添加纠正AI内容任务
        /// </summary>
        /// <param name="id"></param>
        public static ConversationVo add(string id)
        {
            if (string.IsNullOrEmpty(id)) CustomException.create("id不能为空", 3001);

            ConversationVo to = AiRelatedApi.correctProGenerateParams(id);

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
        /// 执行一次自动纠正AI内容
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
                        FileUtils.LogError($"处理项目时出错: {ex.Message}, 堆栈={ex.StackTrace}", "纠正AI内容处理错误");
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
                FileUtils.LogError($"启动处理任务时出错: {ex.Message}, 堆栈={ex.StackTrace}", "纠正AI内容处理错误");
            }
        }


        /// <summary>
        /// 添加纠正AI内容任务到队列中，等待执行
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
                    FileUtils.LogError($"message={ex.Message}, 堆栈={ex.StackTrace}", "批量添加纠正AI内容任务到队列中失败");
                }
            }
        }

        /// <summary>
        /// 执行纠正AI内容任务
        /// </summary>
        /// <param name="vo"></param>
        public static async Task run(HtmlStatusDto vo)
        {
            int status = 0;
            string error = "";
            string correctedContent = "";
            try
            {
                status = 1;
                // 修改队列中对应元素的状态为1（处理中）
                dataQueue.Update(v => v.id.Equals(vo.id), item => item.status = 1);

                if (string.IsNullOrEmpty(vo.content))
                {
                    throw new CustomException("内容为空，不能纠正AI内容", 7005);
                }

                var (modelCode, _) = AiContentCorrectionConfigApi.GetContentCorrectionConfig((int)ContentCorrectionSceneType.AI问答助手的纠正);
                correctedContent = await generateAiContent(vo.id, (int)AiUseSourceType.ai问答, vo.content, modelCode);

                status = 2;

            }
            catch (Exception ex)
            {
                status = 3;
                FileUtils.LogError($"run方法异常: {ex.Message}, 堆栈={ex.StackTrace}", "纠正AI内容处理错误");
                error = ex.Message;
            }
            finally
            {
                // 调用更新接口，传入纠正后的内容
                AiRelatedApi.updateCorrectStatus(vo.id, status, correctedContent, error);
                // 更新dataQuery
                dataQueue.Delete(vo);
            }
        }

        /// <summary>
        /// 获取ai纠正的ai内容-拼接内容
        /// </summary>
        /// <param name="id"></param>
        /// <param name="sourceType"></param>
        /// <param name="content"></param>
        /// <returns></returns>
        public static async Task<string> generateAiContentSplice(string id, int sourceType, string content, int sceneType)
        {
            string newContent = content;

            // 去掉思考内容
            string regex = "<div\\s+class=['\"]deepThinking['\"][^>]*>.*?</div>";
            newContent = Regex.Replace(newContent, regex, "", RegexOptions.Singleline);

            var (modelCode, prompt) = AiContentCorrectionConfigApi.GetContentCorrectionConfig(sceneType);
            if (!string.IsNullOrEmpty(prompt))
            {
                newContent += "\n\n" + prompt;
            }
            return await generateAiContent(id, sourceType, newContent, modelCode);
        }

        /// <summary>
        /// 获取ai纠正的ai内容
        /// </summary>
        /// <param name="id"></param>
        /// <param name="sourceType"></param>
        /// <param name="content"></param>
        /// <param name="modelCode">模型code</param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public static async Task<string> generateAiContent(string id, int sourceType, string content, string modelCode)
        {
            string correctedContent = null;
            if (string.IsNullOrEmpty(modelCode))
            {
                throw new CustomException("获取AI纠正模型配置失败", 7005);
            }
            AiTempTokenDto token = AiRelatedApi.getAnalysisTempToken(null, modelCode);
            if (token == null)
            {
                throw new CustomException("获取AI纠正模型配置失败", 7005);
            }
            AskRequestDto dto = new AskRequestDto();
            dto.identity = "你是一个专业的内容纠正专家";
                
            FileUtils.log(content, "纠正AI内容提示词", true);
            var service = AiFactory.GetAiChatService(token.resourceType);
            CompletionsDto completions = await service.ChatCompletionStream(token, dto, content, null, null);

            if (completions.status == 0)
            {
                // 保存到aiToken使用记录中
                addAiTokenUseRecord(sourceType, id, (int)CueType.纠正AI内容, token.modelDefinition, completions.choices.requestId, completions?.choices?.finishReason??"", "", completions.usage);

                // 获取纠正后的内容
                correctedContent = completions?.choices?.onlyMessage ?? "";
                FileUtils.log($"ai输出 = {correctedContent}", "[ai内容纠正]", true);
                
            }
            else
            {
                FileUtils.LogError($"发送失败id={id}", "纠正AI内容时发送失败");
                throw new CustomException("发送失败", 7005);
            }
            return correctedContent;
        }
        
        /// <summary>
        /// 检验内容是否正常
        /// </summary>
        /// <param name="content"></param>
        /// <param name="id"></param>
        /// <param name="sourceType"></param>
        /// <returns></returns>
        public static bool getCheckAiContent(string content, string id, int sourceType, int sceneType)
        {
            if (string.IsNullOrEmpty(content))
            {
                FileUtils.log($"内容为空，返回false， id = {id}, type = {sourceType}", "[检查ai内容是否需要纠正]", true);
                return false;
            }

            // 通过接口获取提示词和模型
            var (code, prompt) = AiContentCorrectionConfigApi.GetContentCorrectionConfig(sceneType);
            if (string.IsNullOrEmpty(code) || string.IsNullOrEmpty(prompt))
            {
                FileUtils.log($"获取对应的配置失败，sceneType={sceneType}，返回false， id = {id}, type = {sourceType}", "[检查ai内容是否需要纠正]", true);
                return false;
            }

            AiTempTokenDto token = AiRelatedApi.getAnalysisTempToken(null, code);

            if (token == null)
            {
                FileUtils.LogError(code, "获取临时token失败");
                FileUtils.log($"获取ai模型的临时配置失败，返回false， id = {id}, type = {sourceType}", "[检查ai内容是否需要纠正]", true);
                return false;
            }

            AskRequestDto dto = new AskRequestDto()
            {
                identity = "你是一个文字整理专家",
                thinkingType = "disabled"
            };
            
            string contentStr = $"{content}\n\n{prompt}";
            
            FileUtils.log($"提示词 = {contentStr}, model = {token.modelCode}", "[检查ai内容是否需要纠正]", true);
            var serviceChat = AiFactory.GetAiChatService(token.resourceType);
            CompletionsDto completions = serviceChat.ChatCompletion(token, dto, contentStr);
            
            if (completions.status == 0)
            {
                // 保存到aiToken使用记录中
                AiTokenUseRecordVo recordVo = new AiTokenUseRecordVo();
                recordVo.useSourceType = sourceType;
                recordVo.useSourceId = id;
                recordVo.assistantType = (int)CueType.AI内容格式判断;
                recordVo.modelName = token.modelCode;
                recordVo.requestId = completions.choices.requestId;
                recordVo.finishReason = completions.choices.finishReason;
                recordVo.remarks = "判断AI内容格式是否正确";
                recordVo.promptTokens = completions.usage.promptTokens;
                recordVo.completionTokens = completions.usage.completionTokens;
                recordVo.cachedTokens = completions.usage.promptTokensDetails?.cached_tokens ?? 0;
                recordVo.totalTokens = completions.usage.totalTokens;
                AiTokenUseRecordApi.saveAiTokenUseRecord(recordVo);

                if (!string.IsNullOrEmpty(completions?.choices?.onlyMessage ?? ""))
                {
                    FileUtils.log($"ai输出 = {completions?.choices?.onlyMessage ?? ""}", "[检查ai内容是否需要纠正]", true);
                    
                    string onlyMessage = completions?.choices?.onlyMessage;
                    if (string.IsNullOrEmpty(onlyMessage))
                    {
                        return false;
                    }
                    
                    string confirmCode = KvHelper.GetKvByKey("check_ai_content_prompt_confirm", "");

                    if (string.IsNullOrEmpty(confirmCode))
                    {
                        return false;
                    }

                    bool res = false;
                    try
                    {
                        List<dynamic> deserializeObject = JsonConvert.DeserializeObject<List<dynamic>>(confirmCode);
                        if (deserializeObject != null && deserializeObject.Count > 0)
                        {
                            // deserializeObject = [{ "value": true, "label": "格式有问题" }]
                            // 遍历 deserializeObject, 判断 onlyMessage 是否等于 deserializeObject 中的 label，返回value的值
                            foreach (var item in deserializeObject)
                            {
                                if (onlyMessage.Contains(item?.label?.ToString() ?? ""))
                                {
                                    res = item?.value ?? false;
                                    break;
                                }
                            }
                        }
                    }
                    catch (Exception e)
                    {

                    }

                    return res;
                }
            }
            
            return false;
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
                maxConcurrentTasks = KvHelper.GetIntKvByKey("correct_ai_content_concurrency_level", maxConcurrentTasks);
                // 如果已经初始化了信号量，则更新它
                if (semaphore == null)
                {
                    semaphore = new SemaphoreSlim(maxConcurrentTasks);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError(ex.Message, "获取correct_ai_content_concurrency_level键失败");
            }

        }
    }
}
