using System;
using System.Threading.Tasks;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Dto;
using ReviewAnalysis.Utils;

namespace ReviewAnalysis.Ai.impl
{
    public class ChatCompletionsAsk : AbstractAsk
    {
        public static int maxOutNum = 15000;
        public int maxSendNum = 60000;

        public override async Task ask()
        {
            if (string.IsNullOrEmpty(contextId))
            {
                contextId = AiUtils.getContextIdNew(dto);
            }
            if (contextId == null) throw new CustomException("获取ContextID失败", 7005);
            FileUtils.log($"使用的模型为：{token.modelDefinition}", "使用的模型");

            FileUtils.log($"{askQuestion}", "真实提问问题", true);
            var service = AiFactory.GetAiChatService(dto.resourceType);

            if (token.useModelWay == 0)
            {
                FileUtils.log($"【C# Ask】走Responses API: contextId={contextId}, useModelWay={token.useModelWay}, model={token.modelDefinition}, sourceType={dto.sourceType}, sourceId={dto.sourceId}, type={dto.type}", "AI问答-Responses API开始");
                Action callback = () => {
                    string json = JsonConvert.SerializeObject(dto);
                    AskRequestDto temp = JsonConvert.DeserializeObject<AskRequestDto>(json);
                    temp.content = completions.responseId ?? contextId;
                    FileUtils.log($"【C# Ask】更新contextId: 旧={contextId}, 新={temp.content}", "AI问答-更新contextId");
                    AiUtils.updateContextId(temp);
                    ReplayHttpUtils.updateCosThumbsFile(dto.type, ReplayHttpUtils.UserId, dto.sourceId, dto.sourceType, contextId, null, null);
                };

                completions = await service.ContextChatCompletionStream(token, dto, askQuestion, contextId, request, response);
                FileUtils.log($"【C# Ask】Responses API返回: status={completions.status}, responseId={completions.responseId}, totalTokens={completions.totalTokenNum}", "AI问答-Responses API返回");
                if (completions.status == 0)
                {
                    callback?.Invoke();
                    thisUseNum = (int)completions.totalTokenNum;
                    cachedTokens = completions.usage?.promptTokensDetails?.cached_tokens ?? 0;
                    addAiTokenUseRecord(dto.sourceType, dto.sourceId, dto.type, token.modelDefinition, completions.responseId, "", "", completions.usage);
                }
                else
                {
                    FileUtils.LogError($"【C# Ask】Responses API返回失败: status={completions.status}", "AI问答-Responses API失败");
                    throw new CustomException("发送失败", 7005);
                }
            }
            else
            {
                FileUtils.log($"【C# Ask】走Chat Completions API: contextId={contextId}, useModelWay={token.useModelWay}, model={token.modelDefinition}", "AI问答-Chat Completions开始");
                completions = await service.ChatCompletionStream(token, dto, askQuestion, request, response);
                if (completions.status == 0)
                {
                    thisUseNum = (int)completions.totalTokenNum;
                    cachedTokens = completions.usage?.promptTokensDetails?.cached_tokens ?? 0;
                    addAiTokenUseRecord(dto.sourceType, dto.sourceId, dto.type, token.modelDefinition, completions.responseId, "", "", completions.usage);
                }
                else
                {
                    throw new CustomException("发送失败", 7005);
                }
            }
        }

        public override int GetMaxSendNum()
        {
            return token?.maxSendMessageLength ?? maxSendNum;
        }

    }
}
