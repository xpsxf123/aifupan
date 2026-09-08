using System.Threading.Tasks;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Utils;

namespace ReviewAnalysis.Ai.impl
{
    public class DeepSeekAsk : AbstractAsk
    {
        public static int maxOutNum = 15000;
        public int maxSendNum = 60000;

        public override async Task ask()
        {
            if (string.IsNullOrEmpty(contextId))
            {
                contextId = AiUtils.getContextIdNew(dto);
            }
            FileUtils.log($"使用的模型为：{token.modelDefinition}", "使用的模型", true);
            FileUtils.log($"{askQuestion}", "真实提问问题", true);

            var service = AiFactory.GetAiChatService(dto.resourceType);

            if (token.useModelWay == 0)
            {
                completions = await service.ContextChatCompletionStream(token, dto, askQuestion, contextId, request, response);
            }
            else
            {
                completions = await service.ChatCompletionStream(token, dto, askQuestion, request, response);
            }

            if (completions.status != 0)
            {
                throw new CustomException("DeepSeek请求失败", 7005);
            }

            thisUseNum = completions?.totalTokenNum ?? 0;
            cachedTokens = completions.usage?.promptTokensDetails?.cached_tokens ?? 0;
            addAiTokenUseRecord(dto.sourceType, dto.sourceId, dto.type,
                token.modelDefinition, completions.choices?.requestId,
                completions.choices?.finishReason, "", completions.usage);
        }

        public override int GetMaxSendNum()
        {
            return token?.maxSendMessageLength ?? maxSendNum;
        }
    }
}
