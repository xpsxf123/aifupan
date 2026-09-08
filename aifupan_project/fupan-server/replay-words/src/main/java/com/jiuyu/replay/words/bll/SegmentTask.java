package com.jiuyu.replay.words.bll;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.common.producer.SystemKvProducer;
import com.jiuyu.replay.generic.bo.ai.AiMessageBo;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.generic.bo.third.AiTokenUseRecordBo;
import com.jiuyu.replay.generic.feign.order.AiTokenUseRecordFeign;
import com.jiuyu.replay.generic.feign.ai.AiChatFeign;
import com.jiuyu.replay.generic.feign.third.AiModelFeign;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.vo.words.VideoContentVo;
import com.jiuyu.replay.words.producer.VideoContentProducer;
import com.jiuyu.replay.words.vo.video.SegmentResult;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * 单个分段的 AI 生成任务（Callable）。
 *
 * <h3>执行流程</h3>
 * <ol>
 *   <li><b>AI 调用</b> — 从 KV 配置获取模型，通过 {@code AiChatFeign#chatCompletion} 生成内容</li>
 *   <li><b>格式纠正</b> — 调用 {@link AiContentCorrectService#checkNeedsCorrection} 检查，
 *       如需纠正则调用 {@link AiContentCorrectService#correctContent} 修正格式</li>
 *   <li><b>落库</b> — 将生成的内容通过 {@link VideoContentProducer#updateById} 写入 MongoDB</li>
 *   <li><b>Token 记录</b> — 保存 AI 调用消耗的 Token 使用记录</li>
 * </ol>
 *
 * <h3>重试机制</h3>
 * 最多重试 3 次。可重试的错误（AI 调用失败、返回异常）抛 {@link RetryableException}，
 * 不可重试的错误直接返回失败结果。重试间隔递增：2s → 4s → 6s。
 *
 * <h3>线程安全</h3>
 * 每个分段独立一个 SegmentTask 实例，无共享可变状态，可安全并行执行。
 *
 * @author lujie
 * @date 2025/6/24
 */
@Slf4j
public class SegmentTask implements Callable<SegmentResult> {

    /** 最大重试次数 */
    private static final int MAX_RETRIES = 3;

    /** 分段数据（含 cueWord 提示词、MongoDB id 等） */
    private final VideoContentVo segment;
    /** 格式纠正服务 */
    private final AiContentCorrectService correctService;
    /** MongoDB 分段存储 */
    private final VideoContentProducer videoContentProducer;
    /** KV 配置查询 */
    private final SystemKvProducer systemKvProducer;
    /** AI 模型配置查询 */
    private final AiModelFeign aiModelFeign;
    /** AI 聊天调用（封装 ModelFactoryUtils + AiModel） */
    private final AiChatFeign aiChatFeign;
    /** Token 用量记录 */
    private final AiTokenUseRecordFeign aiTokenUseRecordFeign;

    public SegmentTask(VideoContentVo segment, AiContentCorrectService correctService,
                       VideoContentProducer videoContentProducer, SystemKvProducer systemKvProducer,
                       AiModelFeign aiModelFeign, AiChatFeign aiChatFeign,
                       AiTokenUseRecordFeign aiTokenUseRecordFeign) {
        this.segment = segment;
        this.correctService = correctService;
        this.videoContentProducer = videoContentProducer;
        this.systemKvProducer = systemKvProducer;
        this.aiModelFeign = aiModelFeign;
        this.aiChatFeign = aiChatFeign;
        this.aiTokenUseRecordFeign = aiTokenUseRecordFeign;
    }

    /**
     * 执行单段生成，最多重试 {@value #MAX_RETRIES} 次。
     *
     * @return 成功返回 {@link SegmentResult#success}，失败返回 {@link SegmentResult#fail}
     */
    @Override
    public SegmentResult call() {
        for (int retry = 0; retry < MAX_RETRIES; retry++) {
            try {
                // 1. AI 生成内容
                String aiContent = doAiCall();
                // 2. 格式检查 + 纠正
                String correctedContent = doCorrect(aiContent);
                // 3. 保存到 MongoDB
                doSave(correctedContent);
                return SegmentResult.success(segment.getId());
            } catch (RetryableException e) {
                // 可重试错误：AI 调用失败、返回异常等
                log.warn("[分段生成] 第{}次重试, segmentId={}, error={}", retry + 1, segment.getId(), e.getMessage());
                if (retry < MAX_RETRIES - 1) {
                    sleep(retry);
                }
            } catch (Exception e) {
                // 不可重试错误（如 NPE、数据异常），直接返回失败
                log.error("[分段生成] 不可重试错误, segmentId={}", segment.getId(), e);
                return SegmentResult.fail(segment.getId(), e.getMessage());
            }
        }
        return SegmentResult.fail(segment.getId(), "重试" + MAX_RETRIES + "次后仍失败");
    }

    /**
     * 执行单次 AI 调用。
     *
     * <h3>步骤</h3>
     * <ol>
     *   <li>从 KV 配置 {@code ai_question_gen_model} 获取模型 code</li>
     *   <li>通过 {@link AiModelFeign#getByCode} 查询模型配置</li>
     *   <li>通过 {@link AiChatFeign#chatCompletion} 调用 AI 生成内容</li>
     *   <li>校验返回结果：status=0 且 content 非空</li>
     *   <li>记录 Token 用量</li>
     * </ol>
     *
     * @return AI 生成的文本内容
     * @throws RetryableException 模型配置缺失、AI 返回异常时抛出（可重试）
     */
    private String doAiCall() {
        try {
            // 获取 AI 模型配置
            AiModelConfig config = getAiModelConfig();
            if (config == null) {
                throw new RetryableException("获取AI模型配置失败");
            }

            // 构造请求参数：cueWord 已经是组装好的完整提示词（含 system + user + problem）
            AiMessageBo params = new AiMessageBo();
            params.setUser(List.of(Map.of("text", segment.getCueWord())));

            log.debug("[分段生成] 调用AI, segmentId={}, cueWord length={}", segment.getId(),
                    segment.getCueWord() != null ? segment.getCueWord().length() : 0);

            AiReturnDataVo result = aiChatFeign.chatCompletion(config.aiModelBo, params, config.resourceType);

            // 校验返回值
            if (result == null || !ObjectUtil.equals(result.getStatus(), 0) || StrUtil.isEmpty(result.getContent())) {
                throw new RetryableException("AI返回异常, status=" + (result != null ? result.getStatus() : "null"));
            }

            // 记录 AI 响应的 Token 用量
            recordAiResponse(result, config.modelCode);

            return result.getContent();
        } catch (RetryableException e) {
            throw e;
        } catch (Exception e) {
            // 未知异常包装为可重试
            throw new RetryableException("AI调用异常: " + e.getMessage());
        }
    }

    /**
     * 对 AI 生成的内容执行格式检查与纠正。
     * 纠正失败时返回原内容，不阻塞流程。
     *
     * @param aiContent AI 生成的原始内容
     * @return 纠正后的内容（无需纠正或纠正失败时返回原内容）
     */
    private String doCorrect(String aiContent) {
        if (StrUtil.isEmpty(aiContent)) {
            return aiContent;
        }
        try {
            boolean needsCorrection = correctService.checkNeedsCorrection(aiContent, segment.getSourceId(),
                    segment.getSourceType(), segment.getUserId(), segment.getTenantId());
            if (needsCorrection) {
                log.info("[分段生成] 内容需要格式纠正, segmentId={}", segment.getId());
                return correctService.correctContent(aiContent, segment.getSourceId(),
                        segment.getSourceType(), segment.getUserId(), segment.getTenantId());
            }
        } catch (Exception e) {
            // 纠正异常不阻塞流程，返回原内容
            log.warn("[分段生成] 格式纠正异常, 返回原内容, segmentId={}", segment.getId(), e);
        }
        return aiContent;
    }

    /**
     * 将生成结果保存到 MongoDB。
     *
     * <h3>内容处理</h3>
     * 通过 {@link VideoContentProducer#formatContent} 统一格式化：
     * 按换行分割 → 过滤空行 → 去首尾空格 → 过滤纯标点符号行 → 转 JSON 数组存入。
     *
     * @param content 最终内容（可能已经过格式纠正）
     */
    private void doSave(String content) {
        VideoContentVo updateVo = new VideoContentVo();
        updateVo.setId(segment.getId());
        updateVo.setContent(videoContentProducer.formatContent(content));
        updateVo.setGenerateStatus(1);
        updateVo.setUpdateDate(new Date());
        videoContentProducer.updateById(updateVo);
        log.debug("[分段生成] 保存成功, segmentId={}", segment.getId());
    }

    /**
     * 保存 AI 主调用的 Token 使用记录。
     * Token 记录在 doAiCall 的 recordAiResponse 中已保存，此处无需重复操作。
     * 格式纠正的 Token 记录在 AiContentCorrectService 中单独保存。
     */
    private void saveTokenRecord(String aiContent) {
        // Token 记录已在 doAiCall → recordAiResponse 中保存
        // 格式纠正的 Token 记录在 AiContentCorrectService 中保存
    }

    /**
     * 保存 AI 调用的 Token 消耗记录到数据库。
     *
     * @param result    AI 返回结果（含 Token 用量信息）
     * @param modelName 模型名称
     */
    private void recordAiResponse(AiReturnDataVo result, String modelName) {
        if (result == null || segment == null) return;
        try {
            AiTokenUseRecordBo recordBo = AiTokenUseRecordBo.builder(result, modelName);
            recordBo.setTenantId(segment.getTenantId());
            recordBo.setUserId(segment.getUserId());
            recordBo.setUseSourceType(segment.getSourceType());
            recordBo.setUseSourceId(segment.getSourceId());
            // type=1 自然原文，type=2 优化原文
            recordBo.setAssistantType(
                    segment.getType() == 1
                            ? AiEnums.askType.NATURAL_ORIGINAL_TEXT.getCode()
                            : AiEnums.askType.OPTIMIZE_ORIGINAL_TEXT.getCode());
            recordBo.setRemarks(segment.getType() == 1 ? "生成自然原文" : "生成优化原文");
            aiTokenUseRecordFeign.saveAiTokenUseRec(recordBo);
        } catch (Exception e) {
            log.warn("[分段生成] 保存token记录失败, segmentId={}", segment.getId(), e);
        }
    }

    /**
     * 从 KV 配置获取 AI 模型配置。
     * KV key: {@code ai_question_gen_model} → 模型 code → {@link AiModelFeign#getByCode} → 模型详情
     *
     * @return AI 模型配置（含 AiModelBo、resourceType、modelCode），配置缺失时返回 null
     */
    private AiModelConfig getAiModelConfig() {
        SystemKvInfoVo kv = systemKvProducer.getByKey("ai_question_gen_model");
        if (kv == null || StrUtil.isEmpty(kv.getKvValue())) {
            log.error("[分段生成] KV ai_question_gen_model 未配置");
            return null;
        }
        AiModelInfoVo aiModelInfo = aiModelFeign.getByCode(kv.getKvValue());
        if (aiModelInfo == null) {
            log.error("[分段生成] 模型配置未找到, code={}", kv.getKvValue());
            return null;
        }
        AiModelBo aiModelBo = BeanUtil.copyProperties(aiModelInfo, AiModelBo.class);
        return new AiModelConfig(aiModelBo, aiModelInfo.getResourceType(), aiModelInfo.getModelCode());
    }

    /**
     * 重试间隔休眠，递增等待：2s → 4s → 6s
     *
     * @param retryCount 当前重试次数（0-based）
     */
    private void sleep(int retryCount) {
        try {
            long ms = (retryCount + 1) * 2000L;
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * AI 模型配置内部类（避免外部依赖）。
     */
    private static class AiModelConfig {
        final AiModelBo aiModelBo;
        final Integer resourceType;
        final String modelCode;

        AiModelConfig(AiModelBo aiModelBo, Integer resourceType, String modelCode) {
            this.aiModelBo = aiModelBo;
            this.resourceType = resourceType;
            this.modelCode = modelCode;
        }
    }

    /**
     * 可重试异常 — 表示 AI 调用层面的临时性错误（网络超时、服务繁忙等），
     * 与不可重试的代码 bug（NPE、数据格式错误）区分。
     */
    static class RetryableException extends RuntimeException {
        RetryableException(String message) {
            super(message);
        }
    }
}
