package com.jiuyu.replay.ai.bll;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.ai.entity.ScriptMonitorIntermediateReportEntity;
import com.jiuyu.replay.ai.entity.ScriptMonitorReportBodyEntity;
import com.jiuyu.replay.ai.entity.ScriptMonitorReportEntity;
import com.jiuyu.replay.ai.repository.mongo.ScriptMonitorIntermediateReportRepository;
import com.jiuyu.replay.ai.repository.mongo.ScriptMonitorReportBodyRepository;
import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.common.producer.SystemKvProducer;
import com.jiuyu.replay.common.utils.AiUtils;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.bo.ai.AiMessageBo;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.generic.bo.third.AiTokenUseRecordBo;
import com.jiuyu.replay.generic.bo.words.cue.CueWordsQueryBo;
import com.jiuyu.replay.generic.feign.ai.AiChatFeign;
import com.jiuyu.replay.generic.feign.order.AiTokenWithholdFeign;
import com.jiuyu.replay.generic.feign.third.AiModelFeign;
import com.jiuyu.replay.generic.feign.words.AnchorUrlUserFeign;
import com.jiuyu.replay.generic.feign.words.AnchorVideoFeign;
import com.jiuyu.replay.generic.feign.words.CueWordsFeign;
import com.jiuyu.replay.generic.feign.words.SensitiveWordsFeign;
import com.jiuyu.replay.generic.feign.words.StandardScriptFeign;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.order.RedisWithholdVo;
import com.jiuyu.replay.generic.vo.words.AnalysisResultVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.generic.vo.words.CueWordsInfoVo;
import com.jiuyu.replay.generic.vo.words.SentenceMarkVo;
import com.jiuyu.replay.generic.vo.words.StandardScriptInfoVo;
import com.jiuyu.replay.generic.vo.words.WordListItemVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.stream.IntStream;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 话术还原度报告异步生成 Bll（monitorType=1 主流程）。
 *
 * <p>由 {@link ScriptMonitorMqHandler} 处理 fidelity tag 时调用。按以下顺序执行：
 * <ol>
 *   <li>取 ASR 文本（走 SensitiveWordsFeign SPI）；空则置 NOT_APPLICABLE + 返还 Token</li>
 *   <li>按视频 secUid 取有效标准稿（走 StandardScriptFeign SPI）</li>
 *   <li>按 speechMode 路由提示词（0→cueType=18/19；1→cueType=25/26）</li>
 *   <li>取 AI 模型配置（systemKv key 分别配置对比模型 + 合并模型）</li>
 *   <li>串行执行 3 次独立对比 AI 调用（cueType=18 或 25）并落 MongoDB 中间报告</li>
 *   <li>合并 3 份中间报告执行第 4 次 AI 调用（cueType=19 或 26）</li>
 *   <li>写 MongoDB 最终报告 + 提取 {@code <aifupan-data-block>} summaryJson</li>
 *   <li>调公共 {@link ScriptMonitorReportWriteService#finishReport} 写 MySQL status=GENERATED</li>
 *   <li>实扣 Token（settleAiToken，4 次累积总量）</li>
 * </ol>
 * 任何步骤失败：returnAiToken（withhold=null 时幂等跳过）+ restoreOrFail。</p>
 *
 * <p><b>禁止加 {@code @Transactional}</b>：整体含 4 次 AI 调用（约 60-90s），禁止长事务；
 * 事务下沉到 {@link ScriptMonitorReportWriteService}（独立 Bean）。</p>
 *
 * <p><b>mirror 范式</b>：代码骨架完全对齐 {@link ScriptMonitorGenerateBll}（质检 3+1 编排）；
 * 凡能直接复用的私有方法（buildAiMessage / buildModelInfo / buildSettleResult / buildSummary /
 * safeInt / checkTimeout 等）均按相同实现方式重写，保持可维护对比性。</p>
 *
 * @author beta
 * @date 2026-06-12
 */
@Slf4j
@Component
@AllArgsConstructor
public class ScriptMonitorFidelityGenerateBll {

    /**
     * 单任务总超时阈值（10 分钟），mirror 质检范式。
     */
    private static final long MAX_TASK_DURATION_MS = 10 * 60 * 1000L;

    /**
     * 对比报告生成数量（串行 3 次独立对比）。
     */
    private static final int INTER_REPORT_COUNT = 3;

    /**
     * 非循环对比提示词 cueType（{@link AiEnums.askType#FIDELITY_NON_CYCLIC_GENERATE}=18）。
     */
    private static final int NON_CYCLIC_INTER_CUE_TYPE =
            AiEnums.askType.FIDELITY_NON_CYCLIC_GENERATE.getCode();

    /**
     * 非循环合并提示词 cueType（{@link AiEnums.askType#FIDELITY_NON_CYCLIC_MERGE}=19）。
     */
    private static final int NON_CYCLIC_MERGE_CUE_TYPE =
            AiEnums.askType.FIDELITY_NON_CYCLIC_MERGE.getCode();

    /**
     * 循环对比提示词 cueType（{@link AiEnums.askType#FIDELITY_CYCLIC_GENERATE}=25）。
     */
    private static final int CYCLIC_INTER_CUE_TYPE =
            AiEnums.askType.FIDELITY_CYCLIC_GENERATE.getCode();

    /**
     * 循环合并提示词 cueType（{@link AiEnums.askType#FIDELITY_CYCLIC_MERGE}=26）。
     */
    private static final int CYCLIC_MERGE_CUE_TYPE =
            AiEnums.askType.FIDELITY_CYCLIC_MERGE.getCode();

    /**
     * AI 台账 assistantType：话术还原度 ({@link AiEnums.askType#FIDELITY_MONITOR}=24)。
     */
    private static final int ASSISTANT_TYPE_FIDELITY =
            AiEnums.askType.FIDELITY_MONITOR.getCode();

    /**
     * 录制视频 sourceType=0。
     */
    private static final int SOURCE_TYPE_VIDEO = 0;

    /**
     * 还原度对比 AI 模型 systemKv key（monitorType=1 专用，运维侧已配置）。
     */
    private static final String FIDELITY_INTER_KV_KEY = "script_monitor_fidelity_inter_ai_model";

    /**
     * 还原度合并 AI 模型 systemKv key（monitorType=1 专用，运维侧已配置）。
     */
    private static final String FIDELITY_MERGE_KV_KEY = "script_monitor_fidelity_merge_ai_model";

    /**
     * Summary 标签匹配 pattern（mirror 质检 ScriptMonitorGenerateBll#SUMMARY_TAG_PATTERN）。
     *
     * <p>从 AI 合并报告 Markdown 中提取 {@code <aifupan-data-block ...>...</aifupan-data-block>} 内文本。</p>
     */
    private static final Pattern SUMMARY_TAG_PATTERN = Pattern.compile(
            "<aifupan-data-block[^>]*>(.*?)</aifupan-data-block>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final ScriptMonitorIntermediateReportRepository intermediateRepository;
    private final ScriptMonitorReportBodyRepository bodyRepository;
    private final SensitiveWordsFeign sensitiveWordsFeign;
    private final AnchorVideoFeign anchorVideoFeign;
    private final AnchorUrlUserFeign anchorUrlUserFeign;
    private final CueWordsFeign cueWordsFeign;
    private final StandardScriptFeign standardScriptFeign;
    private final AiModelFeign aiModelFeign;
    private final AiChatFeign chatCompletion;
    private final AiTokenWithholdFeign aiTokenWithholdFeign;
    private final SystemKvProducer systemKvProducer;
    /**
     * 报告状态写入 Service（独立 Spring Bean，解决 self-call 导致 @Transactional 失效问题）。
     */
    private final ScriptMonitorReportWriteService reportWriteService;

    /**
     * JDK 21 虚拟线程 executor（{@code VirtualThreadExecutorConfig#scriptMonitorVtExecutor}）。
     * 用于 fan-out 3 次对比报告 AI 调用。字段名与 Bean 名一致，Spring 按类型+名字 fallback 注入。
     */
    private final Executor scriptMonitorVtExecutor;

    /** fan-out 整体超时:3 个并发 AI 调用预期 30-60s 内完成，留 5min 余量兜底。 */
    private static final long FANOUT_TIMEOUT_MINUTES = 5L;

    /**
     * 还原度报告异步生成入口（由 MQ consumer 调用，禁止加 @Transactional）。
     *
     * @param report         报告行（status 已置为 GENERATING）
     * @param originalStatus 触发前原始状态（D-7：失败时恢复旧成功报告）
     * @param withhold       预扣 Token 凭据（自动触发时为 null）
     */
    public void generate(ScriptMonitorReportEntity report, Integer originalStatus, RedisWithholdVo withhold) {
        Long reportId = report.getId();
        // 旧成功报告字段（D-7 失败时恢复用）
        String oldBodyId = report.getReportBodyId();
        String oldSummaryJson = report.getSummaryJson();

        int totalTokens = 0;
        AiReturnDataVo finalResult = null;

        // 任务级总超时基准
        long startMs = System.currentTimeMillis();

        try {
            // a. 取 ASR 文本（空则置 NOT_APPLICABLE + returnAiToken，不走失败路径）
            checkTimeout(startMs, "loadAsr");
            String asrText = tryLoadAsrText(report, withhold);
            if (asrText == null) {
                // 已在 tryLoadAsrText 内处理 NOT_APPLICABLE + returnAiToken
                return;
            }
            log.info("[还原度-ASR] reportId={} asrText 拼接完成 length={} 前200字={}",
                    reportId, asrText.length(), StrUtil.subPre(asrText, 200));

            // b. 一次性取 video 信息（secUid + tradeId 共用，避免重复 Feign 调用 — mirror PatrolGenerateBll 范式）
            checkTimeout(startMs, "loadVideo");
            AnchorVideoInfoVo video = ResultUtil.getResult(
                    anchorVideoFeign.GetByVideoId(report.getSourceId()));
            if (video == null || StrUtil.isBlank(video.getSecUid())) {
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                        "视频不存在或 secUid 为空 sourceId=" + report.getSourceId());
            }
            String secUid = video.getSecUid();

            // c. 按视频 secUid 取标准稿（走 StandardScriptFeign SPI）
            checkTimeout(startMs, "loadStandardScript");
            StandardScriptInfoVo standardScript =
                    standardScriptFeign.findValid(report.getTenantId(), report.getUserId(), secUid);
            if (standardScript == null) {
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_UNCONFIRMED.getCode(),
                        "按 (tenantId=" + report.getTenantId()
                                + ", userId=" + report.getUserId()
                                + ", secUid=" + secUid + ") 无有效标准稿");
            }
            log.info("[还原度-标准稿] reportId={} standardScriptId={} speechMode={} secUid={}",
                    reportId, standardScript.getId(), standardScript.getSpeechMode(), secUid);

            // c. speechMode=1 校验 cycleDurationMinutes 不为 null
            Integer speechMode = standardScript.getSpeechMode();
            if (Objects.equals(speechMode, 1) && standardScript.getCycleDurationMinutes() == null) {
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                        "循环模式标准稿 cycleDurationMinutes 为空，无法生成还原度报告");
            }

            // d. 按 speechMode 选 cueType（0=非循环 1=循环）
            int interCueType = Objects.equals(speechMode, 1) ? CYCLIC_INTER_CUE_TYPE : NON_CYCLIC_INTER_CUE_TYPE;
            int mergeCueType = Objects.equals(speechMode, 1) ? CYCLIC_MERGE_CUE_TYPE : NON_CYCLIC_MERGE_CUE_TYPE;
            Long tenantId = report.getTenantId();

            // e. 取提示词（按行业+租户兜底通用行业 1，复用上方已查的 video）
            checkTimeout(startMs, "loadCueWords");
            Long tradeId = resolveTradeId(video, report);
            CueWordsInfoVo interCue = loadSingleCueWord(interCueType, "还原度对比提示词", tradeId, tenantId);
            CueWordsInfoVo mergeCue = loadSingleCueWord(mergeCueType, "还原度合并提示词", tradeId, tenantId);

            // f. 循环模式填充 #{cycleDurationMinutes} 占位符
            String filledInterPrompt = fillCycleDurationMinutes(interCue.getProblem(), speechMode,
                    standardScript.getCycleDurationMinutes());

            // g. 取 AI 模型（对比 + 合并按 monitorType=1 还原度专用 systemKv key）
            checkTimeout(startMs, "loadAiModel");
            AiModelBo interModelConfig = loadAiModelByKvKey(FIDELITY_INTER_KV_KEY);
            AiModelBo mergeModelConfig = loadAiModelByKvKey(FIDELITY_MERGE_KV_KEY);
            log.info("[还原度-模型] reportId={} 对比模型 code={} name={} | 合并模型 code={} name={}",
                    reportId,
                    interModelConfig.getModelCode(), interModelConfig.getModelName(),
                    mergeModelConfig.getModelCode(), mergeModelConfig.getModelName());

            // h. 清理旧中间报告（重新生成时确保幂等）
            intermediateRepository.deleteByReportId(reportId);

            // i. 构造 AI 用户输入（ASR 文本 + 标准稿时间轴 JSON）
            String userContent = buildUserContent(asrText, standardScript);
            log.info("[还原度-AI输入-对比] reportId={} speechMode={} cueType={} userContentLen={}",
                    reportId, speechMode, interCueType, userContent.length());

            // j. fan-out 3 次独立对比 AI 调用（虚拟线程并发，AC-009/AC-011/AC-012）
            //    每次 AI 调用输入完全一样（filledInterPrompt + userContent 均循环外算）；
            //    业务靠 AI 随机性拿 3 个独立答案后合并。
            checkTimeout(startMs, "before_inter_fanout");
            final AiMessageBo sharedInterParams = buildAiMessage(filledInterPrompt, userContent);
            final AiModelBo interModelConfigFinal = interModelConfig;

            List<CompletableFuture<AiReturnDataVo>> interFutures = IntStream.range(0, INTER_REPORT_COUNT)
                    .mapToObj(idx -> CompletableFuture.<AiReturnDataVo>supplyAsync(
                            () -> chatCompletion.chatCompletion(interModelConfigFinal, sharedInterParams, reportId),
                            scriptMonitorVtExecutor))
                    .toList();

            // 整体超时（AC-012）：5min 内 3 次 AI 调用应完成；超时取消未完成 future + 抛业务异常
            try {
                CompletableFuture.allOf(interFutures.toArray(new CompletableFuture[0]))
                        .get(FANOUT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            } catch (TimeoutException te) {
                interFutures.forEach(f -> f.cancel(true));
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                        "还原度对比报告并发生成整体超时(" + FANOUT_TIMEOUT_MINUTES + "min) reportId=" + reportId);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                interFutures.forEach(f -> f.cancel(true));
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                        "还原度 fan-out 被中断 reportId=" + reportId);
            } catch (ExecutionException ee) {
                Throwable cause = ee.getCause();
                if (cause instanceof BusinessException be) {
                    throw be;
                }
                if (cause instanceof RuntimeException re) {
                    throw re;
                }
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                        "还原度 fan-out 异常: " + (cause == null ? ee.getMessage() : cause.getMessage()));
            }

            // 串行收集结果（保留原失败检查 + token 累加 + DB save 语义,AC-011 fail-fast）
            for (int idx = 0; idx < INTER_REPORT_COUNT; idx++) {
                AiReturnDataVo interResult = interFutures.get(idx).join();
                if (interResult == null || Objects.equals(interResult.getStatus(), 1)) {
                    throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                            "还原度对比报告生成失败 idx=" + idx);
                }
                log.info("[还原度-对比报告] idx={} requestId={} totalTokens={} contentLen={} 前200字={}",
                        idx, interResult.getRequestId(), interResult.getTotalTokens(),
                        interResult.getContent() == null ? 0 : interResult.getContent().length(),
                        StrUtil.subPre(interResult.getContent(), 200));
                totalTokens += (int) AiUtils.aiTokenConsumeMultiple(
                        (long) safeInt(interResult.getTotalTokens()), interModelConfig.getConsumeMultiple());

                ScriptMonitorIntermediateReportEntity inter = ScriptMonitorIntermediateReportEntity.builder()
                        .reportId(reportId)
                        .index(idx)
                        .content(interResult.getContent())
                        .modelInfo(buildModelInfo(interModelConfig.getModelCode(), interResult.getRequestId()))
                        .build();
                intermediateRepository.save(inter);
            }

            // k. 取 3 份中间报告 → 拼合并输入
            List<ScriptMonitorIntermediateReportEntity> inters =
                    intermediateRepository.findByReportIdOrderByIndexAsc(reportId);
            String mergeInput = inters.stream()
                    .map(ScriptMonitorIntermediateReportEntity::getContent)
                    .collect(Collectors.joining("\n---\n"));

            // l. 合并 AI 调用（第 4 次）
            checkTimeout(startMs, "merge");
            log.info("[还原度-AI输入-合并] reportId={} cueType={} mergeInputLen={}",
                    reportId, mergeCueType, mergeInput.length());
            AiMessageBo mergeParams = buildAiMessage(mergeCue.getProblem(), mergeInput);
            finalResult = chatCompletion.chatCompletion(mergeModelConfig, mergeParams, reportId);
            if (finalResult == null || Objects.equals(finalResult.getStatus(), 1)) {
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                        "还原度合并报告生成失败");
            }
            log.info("[还原度-合并报告] requestId={} totalTokens={} mergeInputLen={} contentLen={} 前200字={}",
                    finalResult.getRequestId(), finalResult.getTotalTokens(), mergeInput.length(),
                    finalResult.getContent() == null ? 0 : finalResult.getContent().length(),
                    StrUtil.subPre(finalResult.getContent(), 200));
            totalTokens += (int) AiUtils.aiTokenConsumeMultiple(
                    (long) safeInt(finalResult.getTotalTokens()), mergeModelConfig.getConsumeMultiple());

            // m. 覆盖写最终报告正文（先删旧再插新）
            bodyRepository.deleteByReportId(reportId);
            ScriptMonitorReportBodyEntity body = ScriptMonitorReportBodyEntity.builder()
                    .reportId(reportId)
                    .content(finalResult.getContent())
                    .build();
            bodyRepository.save(body);

            // n. 从 AI 合并报告提取 <aifupan-data-block> 标签内文本 → summaryJson
            buildSummary(report, finalResult.getContent());

            // o. UPDATE MySQL（GENERATED + summaryJson + reportBodyId）—— 走独立 Bean 确保 @Transactional 生效
            reportWriteService.finishReport(reportId, body.getId(), report.getSummaryJson());
            log.info("[还原度] 报告生成完成 reportId={} totalTokens={}", reportId, totalTokens);

            // p. 实扣 Token（一次性 settle 4 次累积 totalTokens）
            AiReturnDataVo settleResult = buildSettleResult(finalResult, totalTokens);
            AiTokenUseRecordBo recordBo = buildTokenRecord(report, mergeModelConfig, finalResult);
            aiTokenWithholdFeign.settleAiToken(withhold, report.getUserId(), settleResult, recordBo);

        } catch (Exception e) {
            log.error("[还原度] 生成失败 reportId={}", reportId, e);
            // 归还预扣（withhold=null 时幂等跳过）
            try {
                aiTokenWithholdFeign.returnAiToken(withhold);
            } catch (Exception re) {
                log.error("[还原度] returnAiToken 失败 reportId={}", reportId, re);
            }
            // 状态补偿：若原本是 GENERATED 则恢复旧成功报告，否则置为 GENERATE_FAILED
            reportWriteService.restoreOrFail(reportId, originalStatus, oldBodyId, oldSummaryJson, e.getMessage());
        }
    }

    // ====== 私有辅助方法 ======

    /**
     * 尝试取 ASR 文本；ASR 为空时置 NOT_APPLICABLE + 归还预扣，返回 null。
     *
     * @param report   报告实体
     * @param withhold 预扣凭据（null 时 returnAiToken 幂等跳过）
     * @return ASR 拼接文本；ASR 为空时返回 null（已处理状态补偿）
     */
    private String tryLoadAsrText(ScriptMonitorReportEntity report, RedisWithholdVo withhold) {
        R<AnalysisResultVo> asrR = sensitiveWordsFeign.getAnalysisData(
                report.getSourceType(), report.getSourceId());
        if (asrR == null || asrR.getData() == null
                || CollUtil.isEmpty(asrR.getData().getSentenceMarkVos())) {
            log.warn("[还原度] ASR 数据为空，reportId={} 置 NOT_APPLICABLE", report.getId());
            try {
                aiTokenWithholdFeign.returnAiToken(withhold);
            } catch (Exception re) {
                log.error("[还原度] returnAiToken 失败(NOT_APPLICABLE) reportId={}", report.getId(), re);
            }
            reportWriteService.markNotApplicable(report.getId(), "ASR 数据为空，无法生成还原度报告");
            return null;
        }
        List<SentenceMarkVo> sentences = asrR.getData().getSentenceMarkVos();
        return sentences.stream()
                .sorted((a, b) -> {
                    int sa = a.getCurrentSort() == null ? 0 : a.getCurrentSort();
                    int sb = b.getCurrentSort() == null ? 0 : b.getCurrentSort();
                    return Integer.compare(sa, sb);
                })
                .map(s -> {
                    String timeTag = "";
                    List<WordListItemVo> items = s.getItems();
                    if (CollUtil.isNotEmpty(items)) {
                        WordListItemVo first = items.get(0);
                        WordListItemVo last = items.get(items.size() - 1);
                        if (first != null && last != null
                                && first.getStartTime() != null && last.getEndTime() != null) {
                            timeTag = " 时间 " + formatMsToHms(first.getStartTime())
                                    + "-" + formatMsToHms(last.getEndTime());
                        }
                    }
                    return "[段落 " + s.getCurrentSort() + timeTag + "] " + s.getContent();
                })
                .collect(Collectors.joining("\n"));
    }

    /**
     * 解析行业 id：取直播间维度 tb_anchor_url_user.trade_id（按 secUid+userId+tenantId），
     * 不用录制记录 video.tradeId；取不到回退 1L 通用行业。
     *
     * @param video  视频信息（提供 secUid，generate() 已一次性加载）
     * @param report 报告实体（提供 userId + tenantId）
     * @return 行业 id，兜底 1L
     */
    private Long resolveTradeId(AnchorVideoInfoVo video, ScriptMonitorReportEntity report) {
        if (video != null && StrUtil.isNotBlank(video.getSecUid())) {
            AnchorUrlUserVo anchorUser = ResultUtil.getResult(anchorUrlUserFeign.getBySecUidAndUser(
                    video.getSecUid(), report.getUserId(), report.getTenantId()));
            if (anchorUser != null && anchorUser.getTradeId() != null) {
                return anchorUser.getTradeId();
            }
        }
        return 1L;
    }

    /**
     * 按行业 + 租户 + cueType 取单条提示词（mirror 质检 loadSingleCueWord）。
     *
     * @param cueType    助手类型（18/19/25/26）
     * @param promptName 提示词业务名（用于异常文案）
     * @param tradeId    行业 id
     * @param tenantId   租户 id
     * @return 命中的提示词（含 problem）
     * @throws BusinessException 无配置时抛出
     */
    private CueWordsInfoVo loadSingleCueWord(int cueType, String promptName, Long tradeId, Long tenantId) {
        CueWordsQueryBo bo = new CueWordsQueryBo();
        bo.setTradeId(tradeId);
        bo.setTenantId(tenantId);
        bo.setCueType(cueType);
        CueWordsInfoVo cue = cueWordsFeign.getCueWordByTradeAndType(bo);
        if (cue == null) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                    promptName + "未配置（tradeId=" + tradeId + ", tenantId=" + tenantId + "），请联系管理员");
        }
        return cue;
    }

    /**
     * 循环模式时替换提示词中 #{cycleDurationMinutes} 占位符。
     *
     * @param prompt               提示词 problem 原文
     * @param speechMode           话术模式（0=非循环 1=循环）
     * @param cycleDurationMinutes 循环预估时长（分钟）
     * @return 填充后的提示词
     */
    private String fillCycleDurationMinutes(String prompt, Integer speechMode, Integer cycleDurationMinutes) {
        if (!Objects.equals(speechMode, 1) || cycleDurationMinutes == null) {
            return prompt;
        }
        return prompt.replace("#{cycleDurationMinutes}", String.valueOf(cycleDurationMinutes));
    }

    /**
     * 构造 AI 用户输入（ASR 文本 + 标准稿时间轴 JSON）。
     *
     * <p>格式：{@code 【主播 ASR】\n<asrText>\n\n【标准稿（时间轴格式）】\n<timeAxisScript>}；
     * speechMode=1 时在末尾追加 {@code 【循环话术预估时长】N分钟} 作为额外上下文。</p>
     *
     * @param asrText        ASR 拼接文本
     * @param standardScript 标准稿 VO
     * @return userContent 字符串
     */
    private String buildUserContent(String asrText, StandardScriptInfoVo standardScript) {
        StringBuilder sb = new StringBuilder();
        sb.append("【主播 ASR】\n").append(asrText);
        sb.append("\n\n【标准稿（时间轴格式）】\n").append(standardScript.getTimeAxisScript());
        if (Objects.equals(standardScript.getSpeechMode(), 1)
                && standardScript.getCycleDurationMinutes() != null) {
            sb.append("\n\n【循环话术预估时长】")
                    .append(standardScript.getCycleDurationMinutes())
                    .append("分钟");
        }
        return sb.toString();
    }

    // loadInterAiModel / loadMergeAiModel 中间方法已删除（code-reviewer Round 1 MAJOR-3）。
    // generate() 直接调 loadAiModelByKvKey(FIDELITY_INTER_KV_KEY / FIDELITY_MERGE_KV_KEY) 取模型，
    // FidelityGenerateBll 仅处理 monitorType=1，无需 monitorType 参数路由。


    /**
     * monitorType → 中间报告 systemKv key 映射（mirror ScriptMonitorGenerateBll）。
     *
     * @param monitorType 监控类型
     * @return systemKv key
     * @throws BusinessException monitorType 为 null 或不支持时抛出
     */
    // resolveInterKvKey / resolveMergeKvKey 方法已删除（code-reviewer Round 1 MAJOR-3
    // 修复：FidelityGenerateBll 仅处理 monitorType=1，case 0/2 永不到达。systemKv key 已抽为常量
    // FIDELITY_INTER_KV_KEY / FIDELITY_MERGE_KV_KEY，调用方直接传入 loadAiModelByKvKey）。

    /**
     * 按 systemKv key 加载 AI 模型，兜底回退到 listDiagnosisModel[0]（mirror 质检）。
     *
     * @param kvKey systemKv key
     * @return AiModelBo
     */
    private AiModelBo loadAiModelByKvKey(String kvKey) {
        SystemKvInfoVo kv = systemKvProducer.getByKey(kvKey);
        if (kv == null || StrUtil.isBlank(kv.getKvValue())) {
            log.warn("[还原度] systemKv 未配置 {}, 回退字典 listDiagnosisModel.get(0)", kvKey);
            return loadAiModelFallback();
        }
        String modelCode = kv.getKvValue();
        AiModelInfoVo model = aiModelFeign.getByCode(modelCode);
        if (model == null) {
            log.warn("[还原度] systemKv 配置的 modelCode 在 tb_ai_model 找不到，回退 list[0]. kvKey={} code={}",
                    kvKey, modelCode);
            return loadAiModelFallback();
        }
        AiModelBo bo = new AiModelBo();
        BeanUtils.copyProperties(model, bo);
        return bo;
    }

    /**
     * AI 模型兜底：listDiagnosisModel 第一个。
     *
     * @return AiModelBo
     * @throws BusinessException 无可用模型时抛出
     */
    private AiModelBo loadAiModelFallback() {
        List<AiModelInfoVo> models = aiModelFeign.listDiagnosisModel();
        if (CollUtil.isEmpty(models)) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                    "未找到可用 AI 模型配置，请联系管理员");
        }
        AiModelBo bo = new AiModelBo();
        BeanUtils.copyProperties(models.get(0), bo);
        return bo;
    }

    /**
     * 构造 AI 调用消息体。
     *
     * @param systemPrompt 系统提示词
     * @param userContent  用户内容
     * @return AiMessageBo
     */
    private AiMessageBo buildAiMessage(String systemPrompt, String userContent) {
        AiMessageBo params = new AiMessageBo();
        params.setSystem(List.of(Map.of("text", systemPrompt)));
        params.setUser(List.of(Map.of("text", userContent)));
        return params;
    }

    /**
     * 构造中间报告的模型元数据。
     *
     * @param modelCode 模型编码
     * @param requestId AI 调用 requestId
     * @return 模型元数据 Map
     */
    private Map<String, Object> buildModelInfo(String modelCode, String requestId) {
        Map<String, Object> info = new HashMap<>(4);
        info.put("modelCode", modelCode);
        info.put("requestId", requestId);
        return info;
    }

    /**
     * 构造结算用 AiReturnDataVo（totalTokens 设为本次全流程累积量）。
     *
     * @param lastResult  最后一次 AI 调用返回（复制元数据）
     * @param totalTokens 本次全流程消耗的 Token 总量
     * @return 结算用结果
     */
    private AiReturnDataVo buildSettleResult(AiReturnDataVo lastResult, int totalTokens) {
        AiReturnDataVo settle = new AiReturnDataVo();
        BeanUtils.copyProperties(lastResult, settle);
        settle.setTotalTokens(totalTokens);
        return settle;
    }

    /**
     * 构造 Token 台账记录 Bo。
     *
     * @param report      报告实体
     * @param modelConfig 模型配置
     * @param finalResult 最终 AI 调用结果
     * @return 台账 Bo
     */
    private AiTokenUseRecordBo buildTokenRecord(ScriptMonitorReportEntity report,
                                                AiModelBo modelConfig, AiReturnDataVo finalResult) {
        AiTokenUseRecordBo bo = AiTokenUseRecordBo.builder(finalResult, modelConfig.getModelName());
        bo.setTenantId(report.getTenantId());
        bo.setUseSourceType(report.getSourceType());
        bo.setUseSourceId(report.getSourceId());
        bo.setAssistantType(ASSISTANT_TYPE_FIDELITY);
        return bo;
    }

    /**
     * 从 AI 合并报告 Markdown 提取 {@code <aifupan-data-block>} 标签内文本，写入 entity.summaryJson（mirror 质检）。
     *
     * @param report          报告实体
     * @param mergedAiContent AI 合并报告 Markdown 字符串
     */
    private void buildSummary(ScriptMonitorReportEntity report, String mergedAiContent) {
        if (mergedAiContent == null) {
            log.warn("[还原度] 合并报告内容为 null reportId={}", report.getId());
            report.setSummaryJson(null);
            return;
        }
        Matcher matcher = SUMMARY_TAG_PATTERN.matcher(mergedAiContent);
        if (matcher.find()) {
            report.setSummaryJson(matcher.group(1).trim());
            return;
        }
        log.warn("[还原度] 合并报告 Markdown 无 <aifupan-data-block> 标签 reportId={}, 前200字={}",
                report.getId(), StrUtil.subPre(mergedAiContent, 200));
        report.setSummaryJson(null);
    }

    /**
     * 安全转换 Integer 为 int（null 返回 0）。
     *
     * @param val Integer 值
     * @return int 值，null 时返回 0
     */
    private int safeInt(Integer val) {
        return val == null ? 0 : val;
    }

    /**
     * 任务级超时检查（mirror 质检）。
     *
     * @param startMs 任务开始时间戳
     * @param stage   当前阶段名（用于日志定位）
     * @throws BusinessException 超时抛出
     */
    private void checkTimeout(long startMs, String stage) {
        long elapsedMs = System.currentTimeMillis() - startMs;
        if (elapsedMs > MAX_TASK_DURATION_MS) {
            throw new BusinessException(
                    StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                    "还原度任务超时(已运行 " + (elapsedMs / 1000) + "s)@" + stage);
        }
    }

    /**
     * 毫秒时间戳格式化为 HH:mm:ss（偏 -8 小时去时区，mirror 质检 formatMsToHms）。
     *
     * @param ms 毫秒偏移
     * @return HH:mm:ss 格式串
     */
    private static String formatMsToHms(long ms) {
        return DateUtil.format(new DateTime(ms).offset(DateField.HOUR, -8), "HH:mm:ss");
    }
}
