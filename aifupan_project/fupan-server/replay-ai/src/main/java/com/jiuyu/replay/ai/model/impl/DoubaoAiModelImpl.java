package com.jiuyu.replay.ai.model.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.jiuyu.replay.ai.config.ArkServiceClient;
import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.common.utils.ApplicationContextUtil;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.bo.ai.AiMessageBo;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;
import com.volcengine.ark.runtime.model.Usage;
import com.volcengine.ark.runtime.model.completion.chat.*;
import com.volcengine.ark.runtime.model.responses.common.ResponsesCaching;
import com.volcengine.ark.runtime.model.responses.common.ResponsesThinking;
import com.volcengine.ark.runtime.model.responses.event.outputtext.OutputTextDeltaEvent;
import com.volcengine.ark.runtime.model.responses.event.reasoningsummary.ReasoningSummaryTextDeltaEvent;
import com.volcengine.ark.runtime.model.responses.event.response.ResponseCompletedEvent;
import com.volcengine.ark.runtime.model.responses.item.InputItem;
import com.volcengine.ark.runtime.model.responses.item.ItemEasyMessage;
import com.volcengine.ark.runtime.model.responses.item.MessageContent;
import com.volcengine.ark.runtime.model.responses.request.CreateResponsesRequest;
import com.volcengine.ark.runtime.model.responses.request.ResponsesInput;
import com.volcengine.ark.runtime.model.responses.response.ResponseObject;
import com.volcengine.ark.runtime.service.ArkService;
import com.jiuyu.replay.ai.repository.service.AiContextCacheService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author ：lujie
 * @description：豆包ai
 * @date ：2025/3/20 上午11:11
 */
@Slf4j
public class DoubaoAiModelImpl extends DefaultAiModelImpl {

    private static final String CONTEXT_MSG_KEY_PREFIX = "ai:doubao:contextId:";

    private final AiContextCacheService cacheService =
            ApplicationContextUtil.getBean(AiContextCacheService.class);

    @Override
    public String getSystemPrompt(String cacheKey) {
        if (StrUtil.isEmpty(cacheKey)) {
            return null;
        }
        return cacheService.getDoubaoSystemPrompt(CONTEXT_MSG_KEY_PREFIX + cacheKey);
    }

    @Override
    public AiReturnDataVo createContext(AiModelBo aiModel, String systemContent, String content, String redisKey, Long timeout) {
        AiReturnDataVo vo = new AiReturnDataVo();
        String contextId = IdUtil.fastSimpleUUID();
        if (StrUtil.isNotEmpty(systemContent)) {
            String msgKey = CONTEXT_MSG_KEY_PREFIX + contextId;
            cacheService.saveDoubaoSystemPrompt(msgKey, systemContent, timeout);
        }
        vo.setContextId(contextId);
        vo.setRequestId(contextId);
        vo.setStatus(0);
        return vo;
    }
    
    private List<ChatMessage> getMessage(AiMessageBo messages, AiModelBo aiModel){
        final Integer[] wordsNum = {aiModel.getWordsNum() == 0 ? -1 : aiModel.getWordsNum()};
        List<ChatMessage> result = new ArrayList<>();

        List<List<Map<String, Object>>> lists = new ArrayList<>();
        List<ChatMessageRole> roles = new ArrayList<>();

        lists.add(messages.getSystem());
        roles.add(ChatMessageRole.SYSTEM);

        lists.add(messages.getUser());
        roles.add(ChatMessageRole.USER);

        lists.add(messages.getAssistant());
        roles.add(ChatMessageRole.ASSISTANT);

        lists.add(messages.getTool());
        roles.add(ChatMessageRole.TOOL);

        for (int i = 0; i < lists.size(); i++) {
            List<Map<String, Object>> list = lists.get(i);
            if (ObjectUtil.isNotEmpty(list)){
                int finalI = i;
                list.stream().findAny().ifPresent(contentMao -> contentMao.forEach((key, value) -> {
                    if (ObjectUtil.isNotEmpty(value)){
                        ChatMessage message = new ChatMessage();
                        message.setRole(roles.get(finalI));
                        Object[] objects = substringValue(value.toString(), wordsNum[0]);
                        wordsNum[0] = (int) objects[1];
                        Object values = objects[0];
                        if (ObjectUtil.isNotEmpty(values)){
                            message.setContent(values);
                            result.add(message);
                        }
                    }
                }));
            }
        }
        RRException.isNotEmpty(result, "消息列表为空");
        log.debug("===================豆包的提示词：{}", JSONUtil.toJsonStr(result));
        return result;
    }

    /**
     * 截取字符串
     * @param content
     * @param num
     * @return
     */
    private Object[] substringValue(String content, int num){
        if (num != -1 ){
            if (num == 0) return new Object[]{null, num};
            int length = content.length();
            if (length > num){
                content = content.substring(0, num);
                num = 0;
            }else {
                num = num - length;
            }
        }
        return new Object[]{content, num};
    }

    @Override
    public AiReturnDataVo chatCompletion(AiModelBo aiModel, AiMessageBo params, Long sourceId) {
        AiReturnDataVo result = new AiReturnDataVo();
        result.setStatus(0);
        ArkService service = ArkServiceClient.getArkService(aiModel);

        // 创建聊天补全请求（发送分析文本和提示词）
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                // 设置模型
                .model(aiModel.getEndpointId())
                // 设置用户消息
                .messages(getMessage(params, aiModel))
                // 设置流式返回
                .stream(false)
                // 设置最大返回的 tokens 数
                .maxTokens(aiModel.getOutSize() * 1024)
                .frequencyPenalty(0.01)
                .build();
        ChatCompletionResult chatCompletion = service.createChatCompletion(chatCompletionRequest);
        if (ObjectUtil.isNotEmpty(chatCompletion)){
            result.setRequestId(chatCompletion.getId());

            if (ObjectUtil.isNotEmpty(chatCompletion.getChoices())){
                ChatCompletionChoice chatCompletionChoice = chatCompletion.getChoices().get(0);

                result.setContent(chatCompletionChoice.getMessage().stringContent());
                result.setReasoningContent(chatCompletionChoice.getMessage().getReasoningContent());
                String reasoningContent = ObjectUtil.isEmpty(result.getReasoningContent()) ? "" : StrUtil.format("<div class='deepThinking'>{}</div>", result.getReasoningContent());
                result.setContentAll(StrUtil.format("{}{}", reasoningContent, result.getContent()));
                result.setFinishReason(chatCompletionChoice.getFinishReason());
            }

            // 设置使用token
            if (ObjectUtil.isNotEmpty(chatCompletion.getUsage())){
                setUsage(result, chatCompletion.getUsage());
            }
        }

        return result;
    }

    @Override
    public AiReturnDataVo contextChatCompletionStream(AiModelBo aiModel, String contextId, AiMessageBo params, Consumer<ChatCompletionChoice> callback) {
        AiReturnDataVo result = new AiReturnDataVo();
        result.setStatus(1);
        ArkService service = ArkServiceClient.getArkService(aiModel);

        String userContent = buildSingleContent(params.getUser());
        String systemContent = buildSingleContent(params.getSystem());

        boolean isFirstTurn = StrUtil.isEmpty(contextId) || !contextId.startsWith("resp_");
        log.info("【Responses API】开始请求: isFirstTurn={}, contextId={}, model={}, userContent长度={}, systemContent长度={}",
                isFirstTurn, contextId, aiModel.getEndpointId(),
                ObjectUtil.isEmpty(userContent) ? 0 : userContent.length(),
                ObjectUtil.isEmpty(systemContent) ? 0 : systemContent.length());
        log.debug("【Responses API】用户输入(input): {}", userContent);
        log.debug("【Responses API】系统提示词(instructions): {}", systemContent);

        CreateResponsesRequest.Builder builder = CreateResponsesRequest.builder()
                .model(aiModel.getEndpointId())
                .stream(true)
                .thinking(ResponsesThinking.builder().type("enabled").build())
                .maxOutputTokens((long) aiModel.getOutSize() * 1024)
                .caching(ResponsesCaching.builder().type("enabled").build())
                ;

        if (isFirstTurn) {
            // 首次：从 Redis 读取系统信息作为 instructions，store=true 缓存对话
            String msgKey = CONTEXT_MSG_KEY_PREFIX + contextId;
            String cachedSystem = cacheService.getDoubaoSystemPrompt(msgKey);
            cachedSystem = StrUtil.isNotEmpty(cachedSystem) ? cachedSystem : systemContent;

            ArrayList<InputItem> listValue = new ArrayList<>();
            listValue.add(ItemEasyMessage.builder().role("system").content(MessageContent.builder().stringValue(cachedSystem).build()).build() );
            listValue.add(ItemEasyMessage.builder().role("user").content(MessageContent.builder().stringValue(userContent).build()).build() );
            builder.input(ResponsesInput.builder().listValue(listValue).build());

            builder.store(true);
            builder.expireAt(System.currentTimeMillis() / 1000 + AiEnums.CONTEXT_TIMEOUT);
        } else {
            // 后续轮次：仅传 previous_response_id，不传 instructions
            builder.previousResponseId(contextId);
            builder.input(ResponsesInput.builder().stringValue(userContent).build());
        }

        StringBuffer sb = new StringBuffer();
        StringBuffer reasoningBuf = new StringBuffer();
        final String[] requestId = {""};
        final int[] chunkCount = {0};
        final int[] thinkingCount = {0};
        final int[] textCount = {0};

        try {
            log.info("【Responses API】开始流式调用火山 API, model={}", aiModel.getEndpointId());
            service.streamResponse(builder.build())
                    .timeout(5, TimeUnit.MINUTES)
                    .doOnError(e -> log.error("streamResponse error: {}", e.getMessage()))
                    .blockingForEach(event -> {
                        chunkCount[0]++;
                        if (event instanceof ReasoningSummaryTextDeltaEvent delta) {
                            thinkingCount[0]++;
                            result.setStatus(0);
                            reasoningBuf.append(delta.getDelta());
                            if (callback != null) {
                                ChatCompletionChoice c = new ChatCompletionChoice();
                                c.setMessage(new ChatMessage() {{ setReasoningContent(delta.getDelta()); }});
                                callback.accept(c);
                            }
                        }
                        if (event instanceof OutputTextDeltaEvent delta) {
                            textCount[0]++;
                            result.setStatus(0);
                            sb.append(delta.getDelta());
                            if (callback != null) {
                                ChatCompletionChoice c = new ChatCompletionChoice();
                                c.setMessage(new ChatMessage() {{ setContent(delta.getDelta()); }});
                                callback.accept(c);
                            }
                        }
                        if (event instanceof ResponseCompletedEvent completed) {
                            ResponseObject resp = completed.getResponse();
                            result.setContextId(resp.getId());
                            requestId[0] = resp.getId();
                            log.info("【Responses API】流完成: responseId={}, 总chunk数={}, 思考chunk={}, 文本chunk={}",
                                    resp.getId(), chunkCount[0], thinkingCount[0], textCount[0]);
                            if (resp.getUsage() != null) {
                                com.volcengine.ark.runtime.model.responses.usage.Usage usage = resp.getUsage();
                                result.setPromptTokens(usage.getInputTokens().intValue());
                                result.setCompletionTokens(usage.getOutputTokens().intValue());
                                result.setTotalTokens(usage.getTotalTokens().intValue());
                                if (usage.getInputTokensDetails() != null && usage.getInputTokensDetails().getCachedTokens() != null) {
                                    result.setCachedTokens(usage.getInputTokensDetails().getCachedTokens().intValue());
                                }
                                if (usage.getOutputTokensDetails() != null && usage.getOutputTokensDetails().getReasoningTokens() != null) {
                                    result.setReasoningTokens(usage.getOutputTokensDetails().getReasoningTokens().intValue());
                                }
                                log.info("【Responses API】Token用量: inputTokens={}, outputTokens={}, totalTokens={}, cachedTokens={}, reasoningTokens={}",
                                        usage.getInputTokens(), usage.getOutputTokens(), usage.getTotalTokens(),
                                        result.getCachedTokens(), result.getReasoningTokens());
                            }
                        }
                    });
        } catch (Exception e) {
            log.error("contextChatCompletionStream 异常", e);
        }
        result.setContent(sb.toString());
        result.setReasoningContent(reasoningBuf.toString());
        String reasoningHtml = ObjectUtil.isEmpty(reasoningBuf.toString()) ? ""
                : StrUtil.format("<div class='deepThinking'>{}</div>", reasoningBuf.toString());
        result.setContentAll(StrUtil.format("{}{}", reasoningHtml, sb.toString()));
        result.setRequestId(requestId[0]);
        return result;
    }

    private String buildSingleContent(List<Map<String, Object>> list) {
        if (ObjectUtil.isEmpty(list)) return "";
        StringBuilder sb = new StringBuilder();
        list.forEach(map -> map.values().forEach(v -> {
            if (ObjectUtil.isNotEmpty(v)) sb.append(v.toString());
        }));
        return sb.toString().trim();
    }

    @Override
    public AiReturnDataVo chatCompletionStream(AiModelBo aiModel, AiMessageBo params, Consumer<ChatCompletionChoice> callback) {
        AiReturnDataVo result = new AiReturnDataVo();
        result.setStatus(1);
        ArkService service = ArkServiceClient.getArkService(aiModel);

        // 创建聊天补全请求（发送分析文本和提示词）
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                // 设置模型
                .model(aiModel.getEndpointId())
                // 设置用户消息
                .messages(getMessage(params, aiModel))
                // 设置流式返回
                .stream(true)
                // 设置流式返回选项
                .streamOptions(ChatCompletionRequest.ChatCompletionRequestStreamOptions.of(true))
                // 设置最大返回的 tokens 数
                .maxTokens(aiModel.getOutSize() * 1024)
                .frequencyPenalty(0.01)
                .build();
        StringBuffer sb = new StringBuffer();
        StringBuffer reasoningContent = new StringBuffer();
        // 发送聊天补全请求并获取分析结果
        final String[] requestId = {""};
        service.streamChatCompletion(chatCompletionRequest)
                .doOnError(e -> {
                    e.printStackTrace();
                    log.error("service.contextChatCompletion.doOnError:{}", e.getMessage());
                })
                .timeout(5, TimeUnit.MINUTES)
                .blockingForEach(choice -> {
                    // 设置requestId
                    if (ObjectUtil.isNotEmpty(choice.getId()) && ObjectUtil.isEmpty(requestId[0])) requestId[0] = choice.getId();
                    if (!choice.getChoices().isEmpty()) {
                        result.setStatus(0);
                        List<ChatCompletionChoice> choices = choice.getChoices();
                        if (ObjectUtil.isNotEmpty(choices)){
                            // 获取模型生成结果
                            ChatCompletionChoice chatCompletionChoice = choices.get(0);
                            if (ObjectUtil.isNotEmpty(chatCompletionChoice) && ObjectUtil.isNotEmpty(chatCompletionChoice.getMessage())){
                                if (ObjectUtil.isNotEmpty(chatCompletionChoice.getMessage().getContent())){
                                    sb.append(chatCompletionChoice.getMessage().getContent());
                                }
                                if (ObjectUtil.isNotEmpty(chatCompletionChoice.getMessage().getReasoningContent())){
                                    reasoningContent.append(chatCompletionChoice.getMessage().getReasoningContent());
                                }
                                // 回调，把ai回答的内容传出去
                                if (callback != null){
                                    callback.accept(chatCompletionChoice);
                                }
                            }
                            // 获取模型生成结束原因
                            if (ObjectUtil.isNotEmpty(chatCompletionChoice.getFinishReason())){
                                result.setFinishReason(chatCompletionChoice.getFinishReason());
                            }
                        }
                    }
                    // 获取上下文使用情况
                    setUsage(result, choice.getUsage());
                })
        ;
        setValue(result, sb, reasoningContent, requestId[0]);
        log.debug("-----------------------------------豆包输出内容（不存在上下文）：{}", JSONUtil.toJsonStr(result));
        return result;
    }

    private void setValue(AiReturnDataVo result, StringBuffer sb, StringBuffer reasoningContent, String requestId) {
        String str = "";
        if (ObjectUtil.isNotEmpty(reasoningContent) && ObjectUtil.isNotEmpty(reasoningContent.toString())){
            str += StrUtil.format("<div class='deepThinking'>{}</div>", reasoningContent.toString());
        }
        if (ObjectUtil.isNotEmpty(sb) && ObjectUtil.isNotEmpty(sb.toString())){
            str += sb.toString();
        }
        result.setContent(str);
        result.setRequestId(requestId);
    }

    /**
     * 设置token使用情况
     * @param returnDataVo
     * @param usage
     */
    private void setUsage(AiReturnDataVo returnDataVo, Usage usage){
        if (ObjectUtil.isNotEmpty(usage)){
            returnDataVo.setPromptTokens((int) usage.getPromptTokens());
            returnDataVo.setCompletionTokens((int) usage.getCompletionTokens());
            if (ObjectUtil.isNotEmpty(usage.getPromptTokensDetails())){
                returnDataVo.setCachedTokens(usage.getPromptTokensDetails().getCachedTokens());
            }
            returnDataVo.setTotalTokens((int) usage.getTotalTokens());
        }
    }
}
