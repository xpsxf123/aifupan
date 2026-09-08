using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Threading.Tasks;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Ai.auto;
using ReviewAnalysis.Ai.Model;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Dto;
using ReviewAnalysis.enums;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.ai;

namespace ReviewAnalysis.Ai.impl
{
    public abstract class AbstractAsk : Ask
    {
        public static string errorReturnData = "当前人数分析过多，请稍后再试，或切换智能模型再试";

        public AiTempTokenDto token = null;

        /// <summary>
        /// 当前问答的请求头
        /// </summary>
        public HttpListenerRequest request = null;

        /// <summary>
        /// 当前问答的响应头
        /// </summary>
        public HttpListenerResponse response = null;

        /// <summary>
        /// 获取响应流
        /// </summary>
        public Stream responseStream = null;

        /// <summary>
        /// 问题
        /// </summary>
        public string askQuestion = null;

        /// <summary>
        /// 模型来源 0豆包火山，1通义千问，2DeepSeek
        /// </summary>
        public int resourceType;

        /// <summary>
        /// 参数
        /// </summary>
        public AskRequestDto dto = null;

        /// <summary>
        /// 上下文缓存id
        /// </summary>
        public string contextId = null;

        public List<String> aiTokenIds = new List<string>();

        /// <summary>
        /// ai回答的内容
        /// </summary>
        public CompletionsDto completions = null;

        /// <summary>
        /// 预扣的id
        /// </summary>
        public string redisId = null;

        /// <summary>
        /// 实扣数量
        /// </summary>
        public int? thisUseNum = null;

        /// <summary>
        /// 缓存命中的 token 数
        /// </summary>
        public int? cachedTokens = 0;

        /// <summary>
        /// 要返回的数据
        /// </summary>
        public AskResponseDto result = null;

        public async Task<AskResponseDto> AskStream(AskRequestDto dto, HttpListenerRequest request, HttpListenerResponse response)
        {
            this.token = dto.token;
            this.dto = dto;
            this.resourceType = dto.resourceType;
            this.response = response;
            this.request = request;

            // 1. 检验资产
            change();

            // 2. 调用服务端组装提示词
            PromptAssemblyResultDto assemblyResult = AiRelatedApi.assemblePrompt(dto);
            if (assemblyResult == null || string.IsNullOrEmpty(assemblyResult.assembledPrompt))
            {
                throw new CustomException("获取组装提示词失败", 7005);
            }
            FileUtils.log($"【C# assemblePrompt】返回: contextId={assemblyResult.contextId}, useModelWay={assemblyResult.useModelWay}, identity长度={assemblyResult.identity?.Length ?? 0}, assembledPrompt长度={assemblyResult.assembledPrompt?.Length ?? 0}, systemPrompt长度={assemblyResult.systemPrompt?.Length ?? 0}, hasModelConfig={assemblyResult.modelConfig != null}", "assemblePrompt结果");
            this.askQuestion = assemblyResult.assembledPrompt;
            this.contextId = assemblyResult.contextId;
            dto.identity = assemblyResult.identity;
            dto.useModelType = assemblyResult.useModelWay;
            if (!string.IsNullOrEmpty(assemblyResult.systemPrompt))
            {
                if (dto.otherObj == null) dto.otherObj = new Dictionary<string, object>();
                dto.otherObj["systemPrompt"] = assemblyResult.systemPrompt;
            }
            if (assemblyResult.modelConfig != null)
            {
                this.token = assemblyResult.modelConfig;
                this.token.contextId = assemblyResult.contextId;
                this.token.useModelWay = assemblyResult.useModelWay;
            }

            // 3. 预扣
            withhold(50000);

            try
            {
                await ask();

                if (completions?.choices?.finishReason == "content_filter")
                {
                    await this.sendData("warning", JsonConvert.SerializeObject(new { code = 7007, msg = "模型输出被内容审核拦截" }));
                }
            }
            catch (CustomException tie)
            {
                thisUseNum = 0;
                if (tie.ErrorCode == 7005)
                {
                    await sendData("message", JsonConvert.SerializeObject(new { content = tie.Message }));
                    throw new CustomException(tie.Message, 7005);
                }
                else
                {
                    await sendData("message", JsonConvert.SerializeObject(new { content = errorReturnData }));
                    throw new CustomException(errorReturnData, 7005);
                }
                FileUtils.LogError($"错误：{tie.Message},堆栈：{tie.StackTrace}", "调用ai问答API报错");
            }
            catch (Exception ex)
            {
                thisUseNum = 0;
                await sendData("message", JsonConvert.SerializeObject(new { content = errorReturnData }));
                FileUtils.LogError($"错误：{ex.Message},堆栈：{ex.StackTrace}", "调用ai问答API报错");
                LogServerUtils.ErrorLog("调用ai问答API报错", "调用ask方法报预期之外的错误", $"dto = {JsonConvert.SerializeObject(dto)}");
                throw new CustomException(errorReturnData, 7005);
            }
            finally
            {
                if (thisUseNum == 0)
                {
                    this.returnWithhold();
                }
            }
            FileUtils.log($"{thisUseNum}", "实际使用的token数量");
            // 实扣
            useProperty();

            try
            {
                ClientAiFavApi.saveOrUpdateByNotExist(dto.type, dto.sourceType, dto.sourceId);
            }
            catch (Exception e)
            {
                FileUtils.LogError($"{e.Message}", "新增或修改运营/违规收藏列表错误");
            }

            // 返回最终的数据
            await returnData();

            return result;
        }

        public abstract Task ask();

        /// <summary>
        /// 获取输入的字数
        /// </summary>
        /// <returns></returns>
        public abstract int GetMaxSendNum();

        /// <summary>
        /// 最后的数据返回
        /// </summary>
        /// <returns></returns>
        public async Task returnData()
        {
            AskResponseDto result = new AskResponseDto();
            string qaCode = Guid.NewGuid().ToString("N");
            UserPropertyEntity userProperty = UserPropertyApi.GetPropertyInfo().Result;
            // 封装资产
            result.property = new PropertyDto()
            {
                currerntUseNum = (int)thisUseNum,
                surplusNum = (int)userProperty.AiTokenNum
            };
            string now = ServerTimeUtils.getCurrentTimeStr();

            // 问的参数封装
            result.problem = new ConversationDto()
            {
                sourceId = dto.sourceId,
                sourceType = dto.sourceType,
                userId  = ReplayHttpUtils.UserId,
                tenantId = ReplayHttpUtils.ActiveTenantId.ToString(),
                askType = dto.type,
                code = Guid.NewGuid().ToString("N"),
                cueWordsId = dto.cueWordsId,
                cueWordsType = dto.cueWordsType,
                qaCode = qaCode,
                contextId = contextId,
                type = "A",
                content = dto.content,
                realContent = askQuestion,
                lastConversationId = dto.lastConversationId,
                optimizeText = dto.otherObj.ContainsKey("optimizeText") ? dto.otherObj["optimizeText"]?.ToString() : null,
                extraRequire = dto.otherObj.ContainsKey("extraRequire") ? dto.otherObj["extraRequire"]?.ToString() : null,
                questionType = string.IsNullOrEmpty(dto.lastConversationId) ? 0 : 1
                //createDate = now,
            };

            // 回答的参数封装
            result.answer = new ConversationDto()
            {
                sourceId = dto.sourceId,
                sourceType = dto.sourceType,
                userId  = ReplayHttpUtils.UserId,
                tenantId = ReplayHttpUtils.ActiveTenantId.ToString(),
                askType = dto.type,
                code = Guid.NewGuid().ToString("N"),
                cueWordsId = dto.cueWordsId,
                cueWordsType = dto.cueWordsType,
                completionId = completions?.id ?? null,
                qaCode = qaCode,
                contextId = contextId,
                type = "Q",
                content = completions?.choices?.message ?? errorReturnData,
                realContent = "",
                //createDate = now,
            };
            FileUtils.log($"{JsonConvert.SerializeObject(result.answer)}", "ai回答的问题");

            // 成功
            List<ConversationDto> list = await DiagnosisApi.saveConversationData(new List<ConversationDto>() { result.problem, result.answer });

            if(list != null && list.Count == 2)
            {
                result.problem.id = list[0].id;
                result.answer.id = list[1].id;
            }

            // 保存会话文件
            //AiUtils.svaeConversationData(dto.type, dto.sourceId, dto.sourceType, contextId, new List<ConversationDto>() { result.problem, result.answer });

            result.problem.code = AiUtils.GetStruacturePrefix(result.problem.contextId, result.problem.code);
            result.answer.code = AiUtils.GetStruacturePrefix(result.answer.contextId, result.answer.code);
            result.problem.realContent = "";
            result.answer.realContent = "";
            // 校验内容是否正确
            result.checkAiContent = CorrectAiContentAuto.getCheckAiContent(result.answer.content, result.answer.id, (int)AiUseSourceType.ai问答, (int)ContentCorrectionSceneType.AI问答助手的纠正检查);
            this.result = result;
            await sendData("returnData", JsonConvert.SerializeObject(result));
        }


        /// <summary>
        /// 额外构建最后的输出字符串
        /// </summary>
        public string extraBuildLastOutString()
        {
            if (string.IsNullOrEmpty(dto.lastConversationId))
            {
                return "";
            }

            return AiRelatedApi.extraBuildLastOutString(dto);
        }

        /// <summary>
        /// 预扣
        /// </summary>
        /// <param name="num"></param>
        public void withhold(int num = 20000)
        {
            this.redisId = ReplayHttpUtils.isPropertyHaveAiToken(num);
        }

        /// <summary>
        /// 实扣
        /// </summary>
        public void useProperty()
        {
            if (!string.IsNullOrEmpty(redisId))
            {
                UserPropertyApi.usePropertyReal("aiTokenNum", (thisUseNum ?? 0), redisId, aiTokenIds, dto.propertyDeductRemarks, dto.token.modelCode, cachedTokens ?? 0);
            }
        }

        /// <summary>
        /// 删除预扣
        /// </summary>
        public void returnWithhold()
        {
            if (!string.IsNullOrEmpty(this.redisId))
            {
                ReplayHttpUtils.removeTempUserProperty(redisId);
                redisId = null;
            }
        }

        /// <summary>
        /// 判断用户的资产是否足够
        /// </summary>
        /// <exception cref="CustomException"></exception>
        public void change()
        {
            // 判断用户的资产是否足够
            UserPropertyEntity userProperty = UserPropertyApi.GetPropertyInfo().Result;
            if (userProperty == null || userProperty.AiTokenNum <= 0)
            {
                throw new CustomException("AI分析算力包余量不足，请联系产品顾问进行套餐外购买", 7001);
            }
        }

        /// <summary>
        /// 发生流式数据到前端
        /// </summary>
        /// <param name="eventStr"></param>
        /// <param name="data"></param>
        public async Task sendData(string eventStr, string data)
        {
            await AiUtils.sendData(response, eventStr, data);
        }

        public void setAiContextCache()
        {
            // 添加ai会话缓存
            AIRelatedCacheDto relatedCacheDto = new AIRelatedCacheDto();
            relatedCacheDto.type = dto.type;
            relatedCacheDto.sourceId = dto.sourceId;
            relatedCacheDto.sourceType = dto.sourceType;
            relatedCacheDto.contextId = contextId;
            relatedCacheDto.contextId = contextId;
            relatedCacheDto.giveStatuc = -1;
            AiRelatedCacheManager.SetAiContextCache(relatedCacheDto);
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
        public AiTokenUseRecordVo addAiTokenUseRecord(int sourceType, string sourceId, int type, string modelName, string requestId, string finishReason, string remarks, Usage usage)
        {
            AiTokenUseRecordVo recordVo = new AiTokenUseRecordVo();
            recordVo.useSourceType = sourceType;
            recordVo.useSourceId = sourceId;
            recordVo.assistantType = type;
            recordVo.modelName = modelName;
            recordVo.requestId = requestId;
            recordVo.finishReason = finishReason;
            recordVo.remarks = remarks;
            recordVo.promptTokens = usage?.promptTokens??0;
            recordVo.completionTokens = usage?.completionTokens??0;
            recordVo.cachedTokens = usage?.promptTokensDetails?.cached_tokens ?? 0;
            recordVo.reasoningTokens = usage?.completionTokensDetails?.reasoningTokens ?? 0;
            recordVo.totalTokens = usage?.totalTokens??0;
            FileUtils.log($"【C# Token记录】requestId={requestId}, model={modelName}, promptTokens={recordVo.promptTokens}, completionTokens={recordVo.completionTokens}, cachedTokens={recordVo.cachedTokens}, reasoningTokens={recordVo.reasoningTokens}, totalTokens={recordVo.totalTokens}", "AI Token使用记录");
            AiTokenUseRecordVo result = AiTokenUseRecordApi.saveAiTokenUseRecord(recordVo);
            if (aiTokenIds == null) aiTokenIds = new List<String>();
            aiTokenIds.Add(result.id);
            return result;
        }

    }
}
