package com.jiuyu.replay.words.bll;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.common.producer.SystemKvProducer;
import com.jiuyu.replay.common.utils.AiUtils;
import com.jiuyu.replay.generic.bo.ai.AiMessageBo;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.generic.bo.third.AiTokenUseRecordBo;
import com.jiuyu.replay.generic.feign.order.AiTokenUseRecordFeign;
import com.jiuyu.replay.generic.feign.ai.AiChatFeign;
import com.jiuyu.replay.generic.feign.third.AiModelFeign;
import com.jiuyu.replay.generic.vo.ai.AiContentCorrectionConfigVo;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.feign.system.DictDataFeign;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AI 内容格式纠正服务（独立服务，不依赖 AI 问答对话模块）。
 *
 * <h3>背景</h3>
 * AI 生成的原文内容有时格式不规范（如缺少标点、段落混乱、包含思考标签等），
 * 需要先检查是否需要纠正，再执行纠正。
 *
 * <h3>与旧流程的关系</h3>
 * 旧流程中格式检查和纠正是耦合在 AI 问答对话（{@code ConversationBll}）中的。
 * 本服务是独立的简化版，专供定时器生成流程使用，不影响旧流程。
 *
 * <h3>两步流程</h3>
 * <ol>
 *   <li><b>检查（check）</b>：调用 AI 判断内容格式是否规范，
 *       通过 KV 配置 {@code check_ai_content_prompt_confirm} 中的 label/value 映射判定结果</li>
 *   <li><b>纠正（correct）</b>：检查通过后才执行，先去掉思考标签（&lt;think&gt;），
 *       再调用 AI 按纠正提示词重新整理格式</li>
 * </ol>
 *
 * <h3>KV 配置项</h3>
 * <table>
 *   <tr><td>check_ai_content_model</td><td>检查用的模型 code</td></tr>
 *   <tr><td>check_ai_content_prompt</td><td>检查提示词</td></tr>
 *   <tr><td>check_ai_content_prompt_confirm</td><td>检查结果映射（JSONArray，每项含 label/value）</td></tr>
 *   <tr><td>correct_ai_content_model</td><td>纠正用的模型 code</td></tr>
 *   <tr><td>correct_ai_content_prompt</td><td>纠正提示词</td></tr>
 * </table>
 *
 * @author lujie
 * @date 2025/6/24
 */
@Component
@Slf4j
public class AiContentCorrectService {

    @Resource
    private SystemKvProducer systemKvProducer;
    @Resource
    private DictDataFeign dictDataFeign;
    @Resource
    private AiModelFeign aiModelFeign;
    @Resource
    private AiChatFeign aiChatFeign;
    @Resource
    private AiTokenUseRecordFeign aiTokenUseRecordFeign;

    /**
     * 检查 AI 生成的内容是否需要格式纠正。
     *
     * <h3>流程</h3>
     * <ol>
     *   <li>从 KV 获取检查模型 code 和检查提示词</li>
     *   <li>构造 system="你是一个文字整理专家" + user=内容+提示词</li>
     *   <li>调用 AI 模型，获取判定结果</li>
     *   <li>从 KV {@code check_ai_content_prompt_confirm} 解析 JSONArray，
     *       匹配 AI 返回的 label，取对应的 value（boolean）</li>
     * </ol>
     *
     * <h3>配置缺失处理</h3>
     * KV 配置缺少时直接返回 false（跳过纠正），不阻塞主流程。
     *
     * @param content    AI 生成的原始内容
     * @param sourceId   来源 id（videoId 或 fileId）
     * @param sourceType 来源类型：0=视频, 1=文件
     * @param userId     用户 id
     * @param tenantId   租户 id
     * @return true=需要纠正, false=格式正常或无法判断
     */
    public boolean checkNeedsCorrection(String content, String sourceId, Integer sourceType,
                                         Long userId, Long tenantId) {
        // 内容为空时无需检查
        if (StrUtil.isEmpty(content) || StrUtil.isEmpty(sourceId)) {
            log.debug("[格式检查] 内容为空，跳过");
            return false;
        }

        // 获取检查模型配置和提示词
        AiContentCorrectionConfigVo config = dictDataFeign.getContentCorrectionConfig(AiEnums.correctionSceneType.CHECK_NATURAL_TEXT.getCode());
        if (config == null || StrUtil.isEmpty(config.getModelCode()) || StrUtil.isEmpty(config.getContentPrompt())) {
            log.debug("[格式检查] 字典配置缺失，跳过");
            return false;
        }
        String code = config.getModelCode();
        String prompt = config.getContentPrompt();

        // 获取 AI 模型实例
        AiModelInfoVo aiModelInfo = aiModelFeign.getByCode(code);
        if (aiModelInfo == null) {
            log.warn("[格式检查] 模型配置未找到, code={}", code);
            return false;
        }

        AiModelBo aiModelBo = BeanUtil.copyProperties(aiModelInfo, AiModelBo.class);

        // 构造请求：system 固定为"文字整理专家"，user = 内容 + 检查提示词
        AiMessageBo params = new AiMessageBo();
        params.setSystem(List.of(Map.of("text", "你是一个文字整理专家")));
        params.setUser(List.of(Map.of("text", content + "\n\n" + prompt)));

        AiReturnDataVo result = aiChatFeign.chatCompletion(aiModelBo, params, aiModelInfo.getResourceType());

        // 记录检查的 Token 消耗
        saveCheckTokenRecord(result, aiModelBo.getModelCode(), sourceId, sourceType, userId, tenantId);

        if (result == null || !ObjectUtil.equals(result.getStatus(), 0)) {
            return false;
        }

        String onlyMessage = result.getContent();
        if (StrUtil.isEmpty(onlyMessage)) {
            return false;
        }

        // 解析确认配置，匹配 AI 返回的 label 获取 boolean 结果
        String confirmCode = getKvValue("check_ai_content_prompt_confirm");
        if (StrUtil.isEmpty(confirmCode)) {
            return false;
        }

        try {
            JSONArray jsonArray = JSON.parseArray(confirmCode);
            if (jsonArray != null && !jsonArray.isEmpty()) {
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject item = jsonArray.getJSONObject(i);
                    String label = item.getString("label");
                    if (onlyMessage.contains(label)) {
                        return item.getBooleanValue("value");
                    }
                }
            }
        } catch (Exception e) {
            log.error("[格式检查] 解析确认配置失败", e);
        }
        return false;
    }

    /**
     * 执行格式纠正。
     *
     * <h3>流程</h3>
     * <ol>
     *   <li>调用 {@link AiUtils#deleteDeepThinking} 去掉 &lt;think&gt; 思考标签</li>
     *   <li>从 KV 获取纠正模型 code 和纠正提示词</li>
     *   <li>调用 AI 模型执行纠正</li>
     *   <li>记录纠正的 Token 消耗</li>
     * </ol>
     *
     * <h3>异常处理</h3>
     * 任何步骤失败均返回原始内容，不阻塞主流程。策略：宁可内容格式差，也不能因纠正失败而丢失内容。
     *
     * @param content    AI 生成的原始内容
     * @param sourceId   来源 id
     * @param sourceType 来源类型
     * @param userId     用户 id
     * @param tenantId   租户 id
     * @return 纠正后的内容，失败时返回原始内容
     */
    public String correctContent(String content, String sourceId, Integer sourceType,
                                  Long userId, Long tenantId) {
        try {
            // 先去掉思考标签（如 ＜think＞...＜/think＞），因为这些不应出现在最终内容中
            String contentWithoutThinking = AiUtils.deleteDeepThinking(content);
            if (StrUtil.isEmpty(contentWithoutThinking)) {
                log.warn("[格式纠正] 去掉思考标签后内容为空, sourceId={}", sourceId);
                return content;
            }

            // 获取纠正模型配置和提示词
            AiContentCorrectionConfigVo config = dictDataFeign.getContentCorrectionConfig(AiEnums.correctionSceneType.CORRECT_NATURAL_TEXT.getCode());
            if (config == null || StrUtil.isEmpty(config.getModelCode()) || StrUtil.isEmpty(config.getContentPrompt())) {
                log.debug("[格式纠正] 字典配置缺失，返回原内容");
                return content;
            }
            String modelCode = config.getModelCode();
            String prompt = config.getContentPrompt();

            AiModelInfoVo aiModelInfo = aiModelFeign.getByCode(modelCode);
            if (aiModelInfo == null) {
                log.warn("[格式纠正] 模型配置未找到, code={}", modelCode);
                return content;
            }

            AiModelBo aiModelBo = BeanUtil.copyProperties(aiModelInfo, AiModelBo.class);

            // user = 去掉思考标签的内容 + 纠正提示词
            String combinedContent = contentWithoutThinking + "\n\n" + prompt;

            AiMessageBo params = new AiMessageBo();
            params.setUser(List.of(Map.of("text", combinedContent)));

            AiReturnDataVo result = aiChatFeign.chatCompletion(aiModelBo, params, aiModelInfo.getResourceType());

            // 记录纠正的 Token 消耗
            saveCorrectTokenRecord(result, aiModelBo.getModelCode(), sourceId, sourceType, userId, tenantId);

            if (result != null && ObjectUtil.equals(result.getStatus(), 0) && StrUtil.isNotEmpty(result.getContent())) {
                return result.getContent();
            }
        } catch (Exception e) {
            log.error("[格式纠正] 纠正失败, sourceId={}", sourceId, e);
        }
        // 失败时返回原始内容，不阻塞流程
        return content;
    }

    /**
     * 从 KV 配置获取字符串值。
     *
     * @param key KV 配置 key
     * @return 配置值，未配置时返回 null
     */
    private String getKvValue(String key) {
        SystemKvInfoVo kv = systemKvProducer.getByKey(key);
        return kv != null ? kv.getKvValue() : null;
    }

    /**
     * 保存格式检查步骤的 Token 消耗记录。
     * assistantType = CHECK_AI_CORRECT
     */
    private void saveCheckTokenRecord(AiReturnDataVo result, String modelName,
                                       String sourceId, Integer sourceType,
                                       Long userId, Long tenantId) {
        if (result == null) return;
        try {
            AiTokenUseRecordBo recordBo = AiTokenUseRecordBo.builder(result, modelName);
            recordBo.setTenantId(tenantId);
            recordBo.setUserId(userId);
            recordBo.setUseSourceType(sourceType);
            recordBo.setUseSourceId(sourceId);
            recordBo.setAssistantType(AiEnums.askType.CHECK_AI_CORRECT.getCode());
            recordBo.setRemarks("判断AI内容格式是否正确");
            aiTokenUseRecordFeign.saveAiTokenUseRec(recordBo);
        } catch (Exception e) {
            log.error("[格式检查] 保存token记录失败", e);
        }
    }

    /**
     * 保存格式纠正步骤的 Token 消耗记录。
     * assistantType = AI_CORRECT_CONTENT
     */
    private void saveCorrectTokenRecord(AiReturnDataVo result, String modelName,
                                         String sourceId, Integer sourceType,
                                         Long userId, Long tenantId) {
        if (result == null) return;
        try {
            AiTokenUseRecordBo recordBo = AiTokenUseRecordBo.builder(result, modelName);
            recordBo.setTenantId(tenantId);
            recordBo.setUserId(userId);
            recordBo.setUseSourceType(sourceType);
            recordBo.setUseSourceId(sourceId);
            recordBo.setAssistantType(AiEnums.askType.AI_CORRECT_CONTENT.getCode());
            recordBo.setRemarks("AI内容格式纠正");
            aiTokenUseRecordFeign.saveAiTokenUseRec(recordBo);
        } catch (Exception e) {
            log.error("[格式纠正] 保存token记录失败", e);
        }
    }
}
