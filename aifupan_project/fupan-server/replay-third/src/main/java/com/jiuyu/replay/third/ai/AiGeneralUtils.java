package com.jiuyu.replay.third.ai;

import com.jiuyu.replay.common.constant.RedisCacheKey;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.third.bll.AiModelBll;
import com.jiuyu.replay.third.vo.AiContentListVo;
import com.volcengine.ark.runtime.Const;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionChoice;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.model.context.CreateContextRequest;
import com.volcengine.ark.runtime.model.context.CreateContextResult;
import com.volcengine.ark.runtime.model.context.chat.ContextChatCompletionRequest;
import com.volcengine.ark.runtime.service.ArkService;
import jakarta.annotation.Resource;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * SpringAI 对接通用工具类
 */
@Component
public class AiGeneralUtils {

    @Resource
    private AiModelBll aiModelBll;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private final long expireTime = 3600; // 过期时间（单位：秒）
    //构建默认的配置数据
    static String apiKey = "5cff3e84-7f3a-4230-aa1b-211ddc88c7a9";
    //链接线程池
    static ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
    static Dispatcher dispatcher = new Dispatcher();
    static ArkService service = ArkService.builder().dispatcher(dispatcher).connectionPool(connectionPool).baseUrl("https://ark.cn-beijing.volces.com/api/v3")
            .apiKey(apiKey)
            .timeout(Duration.ofSeconds(600))
            .connectTimeout(Duration.ofSeconds(20))
            .build();


    /**
     * 暂时这里先构建默认的豆包AI的模型
     */
    //获取模型配置信息
    private AiModelInfoVo getAiModelConfig(Long modelId){
        R<AiModelInfoVo> info = aiModelBll.info(modelId);
        return info.getData();
    }

    /**
     * 豆包AI通用方法
     * @param aiModelRole
     * @param analysisContent 要分析的文本内容信息
     * @param promptWords
     * @param uuid
     * @return
     */
    public AiContentListVo douBaoAI(String aiModelRole, String analysisContent, String promptWords, String uuid, Long modelId) {
        //获取AI默认配置项
        AiModelInfoVo aiModelConfig = getAiModelConfig(modelId);
        /*
            这个地方先开始用比较差的代码，后面兼容SpringAI后需要重新调整，采用工厂+策略模式来进行调整
         */
        //如果不包含上下文，每次请求都需要将上下文信息传递过去，并且不用构建上下文信息
        if (aiModelConfig.getContextSize() == 0) {
            return noCacheModelProcess(aiModelConfig, uuid, aiModelRole, analysisContent, promptWords);
        }
        //包含上下文模型分析
        return chcheModelProcess(aiModelConfig, uuid, aiModelRole, analysisContent, promptWords);
    }

    /**
     * 包含上下文的模型数据处理
     *
     * @param aiModelConfig
     * @param uuid
     * @param aiModelRole
     * @param analysisContent
     * @param promptWords
     * @return
     */
    private AiContentListVo chcheModelProcess(AiModelInfoVo aiModelConfig, String uuid, String aiModelRole, String analysisContent, String promptWords) {

        // 上下文缓存ID
        String contextResultId = "";
        // 需要分析的文本+提示词
        String contentAndWords = "";

        // 创建当次分析的文本SessionID，用于判断是否分析完成
        String currentSessionID = SnowflakeManager.nextValue() + uuid;
        String currentSessionIDKey = uuid + "sessionIdKey";
        // 将当前分析的文本SessionID存入redis
        redisTemplate.opsForValue().set(RedisCacheKey.getRedisKey(RedisCacheKey.aiSessionIdCacheKey, currentSessionIDKey, aiModelConfig.getId()),currentSessionID);

        try {
            // 通过唯一标识查询是否拥有上下文缓存ID
            String redisContextId = (String) redisTemplate.opsForValue().get(RedisCacheKey.getRedisKey(RedisCacheKey.aiModelContextIdCacheKey, uuid, aiModelConfig.getId()));
            // 判断是否拥有上下文缓存
            if (redisContextId == null || "".equals(redisContextId)) {
                // 创建上下文请求
                CreateContextRequest createContextRequest = CreateContextRequest.builder()
                        // 设置模型
                        .model(aiModelConfig.getEndpointId())
                        // 设置上下文模式为会话模式
                        .mode(Const.CONTEXT_MODE_SESSION)
                        // 设置系统消息
                        .messages(Collections.singletonList(ChatMessage.builder().role(ChatMessageRole.SYSTEM)
                                .content(aiModelRole).build()))
                        // 设置上下文的生存时间（秒）
                        .ttl(3600)
                        .build();
                // 发送创建上下文请求并获取结果
                CreateContextResult createContextResult = service.createContext(createContextRequest);
                contextResultId = createContextResult.getId();

                // 通过redis将上下文ID存储起来，以唯一标识为key
                redisTemplate.opsForValue().set(RedisCacheKey.getRedisKey(RedisCacheKey.aiModelContextIdCacheKey, uuid, aiModelConfig.getId()), contextResultId, expireTime, TimeUnit.SECONDS);

                // 没有缓存，需要发送分析文本 + 提示词
                contentAndWords = analysisContent + promptWords;
            }else{
                // 从redis里获取上下文缓存ID
                contextResultId = redisContextId;
                //拥有缓存，只需传提示词
                contentAndWords = promptWords;
                // 每次使用之后需要同步去刷新redis存储时间
                redisTemplate.expire(RedisCacheKey.getRedisKey(RedisCacheKey.aiModelContextIdCacheKey, uuid, aiModelConfig.getId()), expireTime, TimeUnit.SECONDS);
            }

            // 创建聊天补全请求（发送分析文本和提示词）
            ContextChatCompletionRequest chatCompletionRequest = ContextChatCompletionRequest.builder()
                    // 设置上下文 ID
                    .contextId(contextResultId)
                    // 设置模型
                    .model(aiModelConfig.getEndpointId())
                    // 设置用户消息
                    .messages(Collections.singletonList(ChatMessage.builder().role(ChatMessageRole.USER).content(contentAndWords).build()))
                    .build();
            // 发送聊天补全请求并获取分析结果
            List<ChatCompletionChoice> choices = service.createContextChatCompletion(chatCompletionRequest).getChoices();
            for (ChatCompletionChoice choice : choices) {
                AiContentListVo aiContentListVo = new AiContentListVo();
                // 获取返回的分析数据
                Object content = choice.getMessage().getContent();
                // 截取数据组成List集合
                String aiContent = content.toString();
                String toRemove = "###";
                String result = aiContent.replace("" + toRemove, "");
                List<String> aiContentList = Arrays.asList(result.split("\n"));
                aiContentListVo.setAiContentList(aiContentList);
                aiContentListVo.setContextResultId(contextResultId);
                // 关闭服务
                service.shutdownExecutor();
                // 分析完成后销毁当前的SessionId
                Boolean delete = redisTemplate.delete(RedisCacheKey.getRedisKey(RedisCacheKey.aiSessionIdCacheKey, currentSessionIDKey, aiModelConfig.getId()));
                return aiContentListVo;
            }
        }catch (Exception e){
            service.shutdownExecutor();
            // 分析完成后销毁当前的SessionId
            Boolean delete = redisTemplate.delete(RedisCacheKey.getRedisKey(RedisCacheKey.aiSessionIdCacheKey, currentSessionIDKey, aiModelConfig.getId()));
        }finally {
            service.shutdownExecutor();
            // 分析完成库后销毁当前的SessionId
            Boolean delete = redisTemplate.delete(RedisCacheKey.getRedisKey(RedisCacheKey.aiSessionIdCacheKey, currentSessionIDKey, aiModelConfig.getId()));
        }
        return null;
    }

    /**
     * 没有缓存的模型进行数据处理
     * @return
     */
    private AiContentListVo noCacheModelProcess(AiModelInfoVo aiModelConfig, String uuid, String aiModelRole, String analysisContent, String promptWords) {
        //每次都需要进行上下文数据拼接
        // 需要分析的文本+提示词
        String contentAndWords = analysisContent + promptWords;

        // 创建当次分析的文本SessionID，用于判断是否分析完成
        String currentSessionID = SnowflakeManager.nextValue() + uuid;
        String currentSessionIDKey = uuid + "sessionIdKey";
        // 将当前分析的文本SessionID存入redis
        redisTemplate.opsForValue().set(RedisCacheKey.getRedisKey(RedisCacheKey.aiSessionIdCacheKey,currentSessionIDKey, aiModelConfig.getId()),currentSessionID);

        try {

            // 创建聊天补全请求（发送分析文本和提示词）
            ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                    // 设置模型
                    .model(aiModelConfig.getEndpointId())
                    // 设置用户消息
                    .messages(Collections.singletonList(ChatMessage.builder().role(ChatMessageRole.USER).content(contentAndWords).build()))
                    .build();

            // 发送聊天补全请求并获取分析结果
            List<ChatCompletionChoice> choices = service.createChatCompletion(chatCompletionRequest).getChoices();

            for (ChatCompletionChoice choice : choices) {
                AiContentListVo aiContentListVo = new AiContentListVo();
                // 获取返回的分析数据
                Object content = choice.getMessage().getContent();
                // 截取数据组成List集合
                String aiContent = content.toString();
                String toRemove = "###";
                String result = aiContent.replace("" + toRemove, "");
                List<String> aiContentList = Arrays.asList(result.split("\n"));
                aiContentListVo.setAiContentList(aiContentList);
                aiContentListVo.setContextResultId("notCache_"+ UUID.randomUUID().toString().replaceAll("-", ""));
                // 关闭服务
                service.shutdownExecutor();
                // 分析完成后销毁当前的SessionId
                Boolean delete = redisTemplate.delete(RedisCacheKey.getRedisKey(RedisCacheKey.aiSessionIdCacheKey, currentSessionIDKey, aiModelConfig.getId()));
                return aiContentListVo;
            }
        }catch (Exception e){
            service.shutdownExecutor();
            // 分析完成后销毁当前的SessionId
            Boolean delete = redisTemplate.delete(RedisCacheKey.getRedisKey(RedisCacheKey.aiSessionIdCacheKey, currentSessionIDKey, aiModelConfig.getId()));
        }finally {
            service.shutdownExecutor();
            // 分析完成库后销毁当前的SessionId
            Boolean delete = redisTemplate.delete(RedisCacheKey.getRedisKey(RedisCacheKey.aiSessionIdCacheKey, currentSessionIDKey, aiModelConfig.getId()));
        }
        return null;
    }

}
