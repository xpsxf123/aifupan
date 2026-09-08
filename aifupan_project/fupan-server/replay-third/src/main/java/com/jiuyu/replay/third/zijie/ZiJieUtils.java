package com.jiuyu.replay.third.zijie;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.common.constant.RedisCacheKey;
import com.jiuyu.replay.common.redisOperate.AiRedisOperate;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.third.constant.VolcengineProperties;
import com.jiuyu.replay.third.vo.AiContentListVo;
import com.volcengine.ApiClient;
import com.volcengine.ApiException;
import com.volcengine.ark.ArkApi;
import com.volcengine.ark.model.GetApiKeyRequest;
import com.volcengine.ark.model.GetApiKeyResponse;
import com.volcengine.ark.runtime.Const;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionChoice;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.model.context.CreateContextRequest;
import com.volcengine.ark.runtime.model.context.CreateContextResult;
import com.volcengine.ark.runtime.model.context.chat.ContextChatCompletionRequest;
import com.volcengine.ark.runtime.service.ArkService;
import com.volcengine.sign.Credentials;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class ZiJieUtils {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private VolcengineProperties volcengineProperties;
    @Resource
    private AiRedisOperate aiRedisOperate;

    /**
     * 保护 {@link #getArkTempToken()} 与 {@link #getArkTempTokenByModel(AiModelInfoVo)} 的 double-check 临界区。
     * 共用一把实例锁，@Component 单例下与原 {@code synchronized (ZiJieUtils.class)} 跨方法互斥语义等价；不会 pin 虚拟线程。
     */
    private final ReentrantLock arkTokenLock = new ReentrantLock();

    //doubao-pro-32k-apiKey
    static String apiKey = "5cff3e84-7f3a-4230-aa1b-211ddc88c7a9";
    //doubao1.5-pro-32k-250115
//    static String apiKey = "b14d7714-aec7-4903-824f-c990b266353e";
    static String model = "ep-20250118192847-q9r2z";

    private final long expireTime = 3600; // 过期时间（单位：秒）
    static ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
    static Dispatcher dispatcher = new Dispatcher();
    static ArkService service = ArkService.builder().dispatcher(dispatcher).connectionPool(connectionPool).baseUrl("https://ark.cn-beijing.volces.com/api/v3")
            .apiKey(apiKey)
            .timeout(Duration.ofSeconds(60))
            .connectTimeout(Duration.ofSeconds(20))
            .build();


    private static final AtomicReference<ApiClient> apiClientRef = new AtomicReference<>();

    public ApiClient getApiClient(){
        ApiClient client = apiClientRef.get();
        if (client == null) {
            client = new ApiClient()
                    .setCredentials(Credentials.getCredentials(volcengineProperties.getAccessKeyId(), volcengineProperties.getSecretAccessKey()))
                    .setRegion(volcengineProperties.getRegion());
            if (apiClientRef.compareAndSet(null, client)) {
                return client;
            } else {
                return apiClientRef.get();
            }
        }
        return client;
    }

    public String getArkTempToken() throws ApiException {

        String key = (String) redisTemplate.opsForValue().get(RedisCacheKey.aiTempArkTokenCacheKey);
        if (key != null){
            return key;
        }

        arkTokenLock.lock();
        try {
            key = (String) redisTemplate.opsForValue().get(RedisCacheKey.aiTempArkTokenCacheKey);
            if (key != null){
                return key;
            }
            int durationSeconds = volcengineProperties.getTempTokenSaveTime();
            GetApiKeyRequest request = new GetApiKeyRequest();
            request.setResourceType("endpoint");
            request.setResourceIds(Arrays.stream(volcengineProperties.getModel()).toList());
            request.setDurationSeconds(durationSeconds);
            ArkApi api = new ArkApi();
            api.setApiClient(getApiClient());
            GetApiKeyResponse apiKeyResponse = api.getApiKey(request);
            redisTemplate.opsForValue().set(RedisCacheKey.aiTempArkTokenCacheKey, apiKeyResponse.getApiKey(), durationSeconds - (2 * 60), TimeUnit.SECONDS);

            key = apiKeyResponse.getApiKey();
        } finally {
            arkTokenLock.unlock();
        }

        return key;
    }

    /**
     * 根据模型获取临时token
     *
     * @param aiModel 模型
     * @return 临时token
     * @throws ApiException
     */
    public String getArkTempTokenByModel(AiModelInfoVo aiModel) {
        BusinessException.requireNonEmpty(aiModel, "ai模型不能为空");

        String aiTempArkToken = aiRedisOperate.getAiTempArkToken(aiModel.getModelCode());
        if (ObjectUtil.isNotEmpty(aiTempArkToken)) {
            return aiTempArkToken;
        }

        arkTokenLock.lock();
        try {
            aiTempArkToken = aiRedisOperate.getAiTempArkToken(aiModel.getModelCode());
            if (ObjectUtil.isNotEmpty(aiTempArkToken)) {
                return aiTempArkToken;
            }
            int durationSeconds = volcengineProperties.getTempTokenSaveTime();
            GetApiKeyRequest request = new GetApiKeyRequest();
            request.setResourceType("endpoint");
            request.setResourceIds(Collections.singletonList(aiModel.getEndpointId()));
            request.setDurationSeconds(durationSeconds);
            ArkApi api = new ArkApi();
            api.setApiClient(getApiClient());
            GetApiKeyResponse apiKeyResponse = null;
            try {
                apiKeyResponse = api.getApiKey(request);
            } catch (ApiException e) {
                log.error("调用getApiKey接口失败, code={}, e = {}", aiModel.getModelCode(), e.getResponseBody());
                throw new BusinessException(StatusCode.FAILED_TO_REQUEST);
            }
            boolean arkTokenFlag = aiRedisOperate.setAiTempArkToken(aiModel.getModelCode(), apiKeyResponse.getApiKey(), durationSeconds - (2 * 60));
            if (!arkTokenFlag) {
                log.error("设置ai临时token失败, code={}", aiModel.getModelCode());
            }

            aiTempArkToken = apiKeyResponse.getApiKey();
        } finally {
            arkTokenLock.unlock();
        }

        return aiTempArkToken;
    }

    /**
     * 接入的豆包AI分析方法
     * @param aiModelRole 给AI赋予的身份
     * @param analysisContent 需要进行AI分析的文本内容
     * @param promptWords 给到AI的提示词
     * @param uuid 进行分析的文章的唯一标识
     * @return
     */
    public AiContentListVo douBaoAI(String aiModelRole, String analysisContent, String promptWords, String uuid){
            // 上下文缓存ID
            String contextResultId = "";
            // 需要分析的文本+提示词
            String contentAndWords = "";
            // 创建当次分析的文本SessionID，用于判断是否分析完成
            String currentSessionID = SnowflakeManager.nextValue() + uuid;
            String currentSessionIDKey = uuid + "sessionIdKey";
            // 将当前分析的文本SessionID存入redis
            redisTemplate.opsForValue().set(RedisCacheKey.getRedisKey(RedisCacheKey.aiSessionIdCacheKey,currentSessionIDKey),currentSessionID);
        try {
            // 通过唯一标识查询是否拥有上下文缓存ID
            String redisContextId = (String) redisTemplate.opsForValue().get(RedisCacheKey.getRedisKey(RedisCacheKey.aiContextIdCacheKey, uuid));
            // 判断是否拥有上下文缓存
            if (redisContextId == null || "".equals(redisContextId)) {
                // 创建上下文请求
                CreateContextRequest createContextRequest = CreateContextRequest.builder()
                        // 设置模型
                        .model(model)
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
                redisTemplate.opsForValue().set(RedisCacheKey.getRedisKey(RedisCacheKey.aiContextIdCacheKey, uuid), contextResultId, expireTime, TimeUnit.SECONDS);

                // 没有缓存，需要发送分析文本 + 提示词
                contentAndWords = analysisContent + promptWords;
            }else{
                // 从redis里获取上下文缓存ID
                contextResultId = redisContextId;
                //拥有缓存，只需传提示词
                contentAndWords = promptWords;
                // 每次使用之后需要同步去刷新redis存储时间
                redisTemplate.expire(RedisCacheKey.getRedisKey(RedisCacheKey.aiContextIdCacheKey, uuid), expireTime, TimeUnit.SECONDS);
            }

            // 创建聊天补全请求（发送分析文本和提示词）
            ContextChatCompletionRequest chatCompletionRequest = ContextChatCompletionRequest.builder()
                    // 设置上下文 ID
                    .contextId(contextResultId)
                    // 设置模型
                    .model(model)
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
                Boolean delete = redisTemplate.delete(RedisCacheKey.getRedisKey(RedisCacheKey.aiSessionIdCacheKey, currentSessionIDKey));
                return aiContentListVo;
            }
        }catch (Exception e){
            service.shutdownExecutor();
            // 分析完成后销毁当前的SessionId
            Boolean delete = redisTemplate.delete(RedisCacheKey.getRedisKey(RedisCacheKey.aiSessionIdCacheKey, currentSessionIDKey));
        }finally {
            service.shutdownExecutor();
            // 分析完成库后销毁当前的SessionId
            Boolean delete = redisTemplate.delete(RedisCacheKey.getRedisKey(RedisCacheKey.aiSessionIdCacheKey, currentSessionIDKey));
        }

        return null;
    }
}