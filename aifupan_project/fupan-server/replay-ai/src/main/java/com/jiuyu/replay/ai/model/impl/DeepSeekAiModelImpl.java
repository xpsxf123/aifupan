package com.jiuyu.replay.ai.model.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jiuyu.replay.ai.config.DeepSeekHttpClient;
import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.common.utils.ApplicationContextUtil;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.bo.ai.AiMessageBo;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionChoice;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import com.jiuyu.replay.ai.repository.service.AiContextCacheService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class DeepSeekAiModelImpl extends DefaultAiModelImpl {

    private static final String CONTEXT_MSG_KEY_PREFIX = "ai:deepseek:contextId:";

    private final AiContextCacheService cacheService =
            ApplicationContextUtil.getBean(AiContextCacheService.class);

    @Override
    public AiReturnDataVo chatCompletion(AiModelBo aiModel, AiMessageBo params, Long sourceId) {
        AiReturnDataVo result = new AiReturnDataVo();
        result.setStatus(0);
        result.setSourceId(sourceId);
        OkHttpClient client = DeepSeekHttpClient.getClient(aiModel);

        Request request = buildRequest(aiModel, buildMessages(params, aiModel), false);

        try (Response response = client.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.error("DeepSeek chatCompletion failed: HTTP {}, body={}", response.code(), body);
                result.setStatus(1);
                return result;
            }
            JSONObject json = JSONUtil.parseObj(body);
            result.setRequestId(json.getStr("id"));

            JSONArray choices = json.getJSONArray("choices");
            if (ObjectUtil.isNotEmpty(choices)) {
                JSONObject choice = choices.getJSONObject(0);
                result.setFinishReason(choice.getStr("finish_reason"));

                JSONObject message = choice.getJSONObject("message");
                if (message != null) {
                    String content = message.getStr("content");
                    String reasoningContent = message.getStr("reasoning_content");
                    result.setContent(content);
                    result.setReasoningContent(reasoningContent);
                    String reasoningHtml = ObjectUtil.isEmpty(reasoningContent) ? ""
                            : StrUtil.format("<div class='deepThinking'>{}</div>", reasoningContent);
                    result.setContentAll(StrUtil.format("{}{}", reasoningHtml,
                            ObjectUtil.defaultIfEmpty(content, "")));
                }
            }

            JSONObject usageJson = json.getJSONObject("usage");
            setUsage(result, usageJson);
        } catch (Exception e) {
            log.error("DeepSeek chatCompletion error: {}", e.getMessage(), e);
            result.setStatus(1);
        }
        return result;
    }

    @Override
    public AiReturnDataVo chatCompletionStream(AiModelBo aiModel, AiMessageBo params,
                                                Consumer<ChatCompletionChoice> callback) {
        AiReturnDataVo result = new AiReturnDataVo();
        result.setStatus(1);
        OkHttpClient client = DeepSeekHttpClient.getClient(aiModel);

        Request request = buildRequest(aiModel, buildMessages(params, aiModel), true);

        StringBuffer contentBuf = new StringBuffer();
        StringBuffer reasoningBuf = new StringBuffer();
        final String[] requestId = {""};

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "";
                log.error("DeepSeek stream failed: HTTP {}, body={}", response.code(), errBody);
                result.setStatus(1);
                return result;
            }

            parseDeepSeekStream(response, contentBuf, reasoningBuf, requestId, result, callback);
        } catch (Exception e) {
            log.error("DeepSeek chatCompletionStream error: {}", e.getMessage(), e);
        }

        setValue(result, contentBuf, reasoningBuf, requestId[0]);
        log.debug("-----------------------------------DeepSeek输出内容：{}", JSONUtil.toJsonStr(result));
        return result;
    }

    @Override
    public AiReturnDataVo createContext(AiModelBo aiModel, String systemContent, String content,
                                         String redisKey, Long timeout) {
        AiReturnDataVo result = new AiReturnDataVo();
        result.setStatus(0);

        JSONArray messages = new JSONArray();
        if (StrUtil.isNotEmpty(systemContent)) {
            messages.add(Map.of("role", "system", "content", systemContent));
        }
        if (StrUtil.isNotEmpty(content)) {
            messages.add(Map.of("role", "user", "content", content));
        }

        String contextId = IdUtil.fastSimpleUUID();
        String msgKey = CONTEXT_MSG_KEY_PREFIX + contextId;
        cacheService.saveDeepSeekMessages(msgKey, messages.toString(), timeout);

        result.setContextId(contextId);
        log.debug("DeepSeek createContext: contextId={}, msgKey={}", contextId, msgKey);
        return result;
    }

    @Override
    public AiReturnDataVo contextChatCompletionStream(AiModelBo aiModel, String contextId,
                                                       AiMessageBo params,
                                                       Consumer<ChatCompletionChoice> callback) {
        AiReturnDataVo result = new AiReturnDataVo();
        result.setStatus(1);

        String msgKey = CONTEXT_MSG_KEY_PREFIX + contextId;
        String historyJson = cacheService.getDeepSeekMessages(msgKey);
        RRException.isNotEmpty(historyJson, "DeepSeek上下文缓存不存在或已过期");

        JSONArray messages = JSONUtil.parseArray(historyJson);

        // 先裁剪超限消息历史，再追加新提问
        if (aiModel.getContextSize() != null && aiModel.getContextSize() > 0) {
            trimMessages(messages, aiModel.getContextSize());
        }

        // 追加当前用户提问
        addMessagesByRole(messages, params.getUser(), "user", new int[]{-1});

        OkHttpClient client = DeepSeekHttpClient.getClient(aiModel);
        Request request = buildRequest(aiModel, messages, true);

        StringBuffer contentBuf = new StringBuffer();
        StringBuffer reasoningBuf = new StringBuffer();
        final String[] requestId = {""};

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "";
                log.error("DeepSeek context stream failed: HTTP {}, body={}", response.code(), errBody);
                return result;
            }

            parseDeepSeekStream(response, contentBuf, reasoningBuf, requestId, result, callback);
        } catch (Exception e) {
            log.error("DeepSeek contextChatCompletionStream error: {}", e.getMessage(), e);
            return result;
        }

        if (ObjectUtil.isNotEmpty(contentBuf.toString())) {
            Map<String, Object> assistantMsg = new HashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", contentBuf.toString());
            messages.add(assistantMsg);
            cacheService.saveDeepSeekMessages(msgKey, messages.toString(), AiEnums.CONTEXT_TIMEOUT);
        }

        setValue(result, contentBuf, reasoningBuf, requestId[0]);
        log.debug("-----------------------------------DeepSeek上下文输出内容：{}", JSONUtil.toJsonStr(result));
        return result;
    }

    private JSONArray buildMessages(AiMessageBo messages, AiModelBo aiModel) {
        JSONArray result = new JSONArray();
        final int[] wordsNum = {aiModel.getWordsNum() == 0 ? -1 : aiModel.getWordsNum()};

        addMessagesByRole(result, messages.getSystem(), "system", wordsNum);
        addMessagesByRole(result, messages.getUser(), "user", wordsNum);
        addMessagesByRole(result, messages.getAssistant(), "assistant", wordsNum);
        addMessagesByRole(result, messages.getTool(), "tool", wordsNum);

        RRException.isNotEmpty(result, "消息列表为空");
        log.debug("===================DeepSeek的提示词：{}", JSONUtil.toJsonStr(result));
        return result;
    }

    private Request buildRequest(AiModelBo aiModel, Object messages, boolean stream) {
        JSONObject requestBody = new JSONObject();
        requestBody.set("model", aiModel.getEndpointId());
        requestBody.set("messages", messages);
        requestBody.set("stream", stream);
        requestBody.set("max_tokens", aiModel.getOutSize() * 1024);
        if (stream) {
            requestBody.set("stream_options", Map.of("include_usage", true));
        }
        return new Request.Builder()
                .url(DeepSeekHttpClient.BASE_URL + DeepSeekHttpClient.CHAT_COMPLETIONS_PATH)
                .post(RequestBody.create(requestBody.toString(), MediaType.get("application/json; charset=utf-8")))
                .build();
    }

    private void addMessagesByRole(JSONArray result, List<Map<String, Object>> list,
                                    String role, int[] wordsNum) {
        if (ObjectUtil.isEmpty(list)) {
            return;
        }
        for (Map<String, Object> contentMap : list) {
            contentMap.forEach((key, value) -> {
                if (ObjectUtil.isNotEmpty(value)) {
                    Object[] objects = substringValue(value.toString(), wordsNum[0]);
                    wordsNum[0] = (int) objects[1];
                    Object values = objects[0];
                    if (ObjectUtil.isNotEmpty(values)) {
                        JSONObject msg = new JSONObject();
                        msg.set("role", role);
                        msg.set("content", values.toString());
                        result.add(msg);
                    }
                }
            });
        }
    }

    private Object[] substringValue(String content, int num) {
        if (num != -1) {
            if (num == 0) {
                return new Object[]{null, num};
            }
            int length = content.length();
            if (length > num) {
                content = content.substring(0, num);
                num = 0;
            } else {
                num = num - length;
            }
        }
        return new Object[]{content, num};
    }

    private void setValue(AiReturnDataVo result, StringBuffer sb, StringBuffer reasoningContent,
                           String requestId) {
        String str = "";
        if (ObjectUtil.isNotEmpty(reasoningContent) && ObjectUtil.isNotEmpty(reasoningContent.toString())) {
            str += StrUtil.format("<div class='deepThinking'>{}</div>", reasoningContent.toString());
        }
        if (ObjectUtil.isNotEmpty(sb) && ObjectUtil.isNotEmpty(sb.toString())) {
            str += sb.toString();
        }
        result.setContent(str);
        result.setRequestId(requestId);
    }

    private void setUsage(AiReturnDataVo result, JSONObject usageJson) {
        if (usageJson == null) {
            return;
        }
        result.setPromptTokens(usageJson.getInt("prompt_tokens", 0));
        result.setCompletionTokens(usageJson.getInt("completion_tokens", 0));
        result.setTotalTokens(usageJson.getInt("total_tokens", 0));
        result.setCachedTokens(usageJson.getInt("prompt_cache_hit_tokens", 0));
        JSONObject details = usageJson.getJSONObject("completion_tokens_details");
        if (details != null) {
            result.setReasoningTokens(details.getInt("reasoning_tokens", 0));
        }
    }

    private void parseDeepSeekStream(Response response, StringBuffer contentBuf,
                                     StringBuffer reasoningBuf, String[] requestId,
                                     AiReturnDataVo result, Consumer<ChatCompletionChoice> callback)
            throws Exception {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body().byteStream(), "UTF-8"));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                continue;
            }
            if (!line.startsWith("data: ")) {
                continue;
            }
            String data = line.substring(6);
            if ("[DONE]".equals(data.trim())) {
                break;
            }

            try {
                JSONObject chunk = JSONUtil.parseObj(data);

                if (ObjectUtil.isEmpty(requestId[0])) {
                    requestId[0] = chunk.getStr("id", "");
                }

                JSONArray choices = chunk.getJSONArray("choices");
                if (ObjectUtil.isNotEmpty(choices)) {
                    JSONObject choiceJson = choices.getJSONObject(0);
                    JSONObject delta = choiceJson.getJSONObject("delta");

                    if (delta != null) {
                        String deltaContent = delta.getStr("content");
                        String deltaReasoning = delta.getStr("reasoning_content");

                        ChatCompletionChoice volcChoice = new ChatCompletionChoice();
                        volcChoice.setIndex(choiceJson.getInt("index", 0));
                        volcChoice.setFinishReason(choiceJson.getStr("finish_reason"));

                        ChatMessage volcMessage = new ChatMessage();
                        if (ObjectUtil.isNotEmpty(deltaContent)) {
                            contentBuf.append(deltaContent);
                            volcMessage.setContent(deltaContent);
                        }
                        if (ObjectUtil.isNotEmpty(deltaReasoning)) {
                            reasoningBuf.append(deltaReasoning);
                            volcMessage.setReasoningContent(deltaReasoning);
                        }
                        volcChoice.setMessage(volcMessage);

                        result.setStatus(0);

                        if (ObjectUtil.isNotEmpty(volcChoice.getFinishReason())) {
                            result.setFinishReason(volcChoice.getFinishReason());
                        }

                        if (callback != null) {
                            callback.accept(volcChoice);
                        }
                    }
                }

                JSONObject usageJson = chunk.getJSONObject("usage");
                setUsage(result, usageJson);
            } catch (Exception e) {
                log.warn("DeepSeek SSE line parse error: line={}, err={}", data, e.getMessage());
            }
        }
    }

    /**
     * 裁剪消息历史，保留首条 system 消息，从旧到新逐条删除直到不超过 maxSizeKb
     */
    private void trimMessages(JSONArray messages, int maxSizeKb) {
        if (maxSizeKb <= 0) {
            return;
        }
        int maxBytes = maxSizeKb * 1024;
        int keepFrom = 1;
        while (messages.size() > keepFrom && messages.toString().getBytes().length > maxBytes) {
            messages.remove(keepFrom);
        }
        if (messages.size() <= keepFrom) {
            log.warn("DeepSeek trimMessages: all non-system messages trimmed, remaining={}", messages.size());
        }
    }
}
