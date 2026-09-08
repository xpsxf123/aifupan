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
import com.jiuyu.replay.ai.repository.service.ScriptMonitorReportService;
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
import com.jiuyu.replay.generic.vo.words.UploadFileSimpleInfoVo;
import com.jiuyu.replay.generic.vo.words.WordListItemVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI 监控报告异步生成 Bll（质检主流程）。
 *
 * <p>职责：接受由 {@link ScriptMonitorBll#triggerReport} 投递的异步任务，
 * 按以下顺序执行：
 * <ol>
 *   <li>取 ASR 文本（走 SensitiveWordsFeign SPI）</li>
 *   <li>解析直播间所属行业（resolveTradeId），按 tradeId+cueType 取生成/合并提示词</li>
 *   <li>取 AI 模型配置</li>
 *   <li>生成 3 份中间报告并落 MongoDB</li>
 *   <li>合并 3 份中间报告生成最终报告并落 MongoDB</li>
 *   <li>更新 MySQL tb_script_monitor_report（status=2 + 四类计数 + reportBodyId）</li>
 *   <li>实扣 Token（settleAiToken）</li>
 * </ol>
 * 任何步骤失败则执行补偿：调 returnAiToken + 恢复/降级 status。</p>
 *
 * <p><b>Token 约束（AC-5）</b>：所有 Token 操作必须通过 {@link AiTokenWithholdFeign}，
 * 禁止直调 UserPropertyImpl.use / removeTempUserProperty / saveAiTokenUseRec。</p>
 *
 * @author beta
 * @date 2026-05-30
 */
@Slf4j
@Component
@AllArgsConstructor
public class ScriptMonitorGenerateBll {

    /**
     * 单任务总超时阈值（10 分钟）。
     *
     * <p>P1 修补：generate() 每个关键步骤前调用 {@link #checkTimeout}，
     * 超时主动抛 BusinessException 走现有 catch（returnAiToken + 状态补偿）。
     * 不依赖 Future.cancel / 线程 interrupt（OpenFeign 默认不响应中断）。</p>
     */
    private static final long MAX_TASK_DURATION_MS = 10 * 60 * 1000L;

    /**
     * 质检中间报告生成数量（每次生成 3 份，取最优）
     */
    private static final int INTERMEDIATE_REPORT_COUNT = 3;

    /**
     * 质检生成提示词的 cueType（{@link AiEnums.askType#QUALITY_INSPECTION_GENERATE_PROMPT}=16）。
     * 用于对每段话术生成中间报告。
     */
    private static final int QUALITY_GENERATE_CUE_TYPE = AiEnums.askType.QUALITY_INSPECTION_GENERATE_PROMPT.getCode();

    /**
     * 质检合并提示词的 cueType（{@link AiEnums.askType#QUALITY_INSPECTION_MERGE_PROMPT}=17）。
     * 用于将 3 份中间报告合并为最终报告。
     */
    private static final int QUALITY_MERGE_CUE_TYPE = AiEnums.askType.QUALITY_INSPECTION_MERGE_PROMPT.getCode();

    /**
     * 使用来源类型（视频）：来自 AiEnums.useSourceType.VIDEO
     */
    private static final int USE_SOURCE_TYPE_VIDEO = 0;

    /**
     * AI 台账 assistantType：复用 DATA_DIAGNOSIS(13) — 质检是数据诊断类型
     */
    // 2026-06-09：原 DATA_DIAGNOSIS(13) 错位复用，改用独立业务类型枚举 SCRIPT_QUALITY_INSPECTION(22)，
    // 让 tb_ai_token_use_record.assistant_type 可按 monitorType 维度统计 token 消耗
    private static final int ASSISTANT_TYPE_QUALITY = AiEnums.askType.SCRIPT_QUALITY_INSPECTION.getCode();

    /**
     * Summary 标签匹配 pattern：从 AI 合并报告 Markdown 中提取
     * {@code <aifupan-data-block ...>...</aifupan-data-block>} 内文本。
     *
     * <p>开标签兼容：</p>
     * <ul>
     *   <li>带属性 {@code <aifupan-data-block resource-data visible="false">}</li>
     *   <li>自闭合形式 {@code <aifupan-data-block resource-data visible="false"/>}（AI 常输出此形式后再补一个闭标签）</li>
     *   <li>无属性 {@code <aifupan-data-block>}</li>
     * </ul>
     *
     * <p>{@code [^>]*}：匹配 0 或多个非 {@code >} 字符（属性段，含可能的尾部 {@code /}）；
     * {@code CASE_INSENSITIVE}：标签大小写不敏感；
     * {@code DOTALL}：{@code .} 匹配换行（支持跨行内容）；
     * {@code .*?} 非贪婪：多个标签时取首个。</p>
     */
    private static final Pattern SUMMARY_TAG_PATTERN = Pattern.compile(
            "<aifupan-data-block[^>]*>(.*?)</aifupan-data-block>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final ScriptMonitorReportService reportService;
    private final ScriptMonitorIntermediateReportRepository intermediateRepository;
    private final ScriptMonitorReportBodyRepository bodyRepository;
    private final SensitiveWordsFeign sensitiveWordsFeign;
    private final CueWordsFeign cueWordsFeign;
    private final AnchorVideoFeign anchorVideoFeign;
    private final AnchorUrlUserFeign anchorUrlUserFeign;
    private final AiModelFeign aiModelFeign;
    private final AiChatFeign aiChatFeign;
    private final AiTokenWithholdFeign aiTokenWithholdFeign;
    private final SystemKvProducer systemKvProducer;
    /**
     * 报告状态写入 Service（独立 Spring Bean，解决 self-call 导致 @Transactional 失效问题）
     */
    private final ScriptMonitorReportWriteService reportWriteService;

    /**
     * JDK 21 虚拟线程 executor（{@code VirtualThreadExecutorConfig#scriptMonitorVtExecutor}）。
     * 用于 fan-out 3 次中间报告 AI 调用，等待期不占用载体线程。
     * 字段名与 Bean 名一致，Spring 按类型+名字 fallback 注入，无需 @Qualifier。
     */
    private final Executor scriptMonitorVtExecutor;

    /** fan-out 整体超时:3 个并发 AI 调用预期 30-60s 内完成，留 5min 余量兜底。 */
    private static final long FANOUT_TIMEOUT_MINUTES = 5L;

    /**
     * 异步报告生成入口（由线程池调用，非事务）。
     *
     * <p>内部在必要步骤通过 {@link ScriptMonitorReportWriteService} 添加事务；
     * 整体不包裹事务，避免长事务锁定 AI 调用期间（可能数分钟）的数据库连接。</p>
     *
     * @param report         报告行（status 已置为 GENERATING，旧摘要字段原值仍在 entity 内存中）
     * @param originalStatus 触发前原始状态（由调用方在 update 前捕获，用于 D-7 失败回滚）
     * @param withhold       预扣凭据
     */
    public void generate(ScriptMonitorReportEntity report, Integer originalStatus, RedisWithholdVo withhold) {
        Long reportId = report.getId();
        // 旧成功报告字段（D-7 失败时恢复用）
        String oldBodyId = report.getReportBodyId();
        String oldSummaryJson = report.getSummaryJson();

        int totalTokens = 0;
        AiReturnDataVo finalResult = null;
        AiModelBo interModelConfig = null;
        AiModelBo mergeModelConfig = null;

        // P1: 任务级总超时基准
        long startMs = System.currentTimeMillis();

        try {
            // a. 取 ASR 文本（空则置 NOT_APPLICABLE + returnAiToken，不走失败路径）
            checkTimeout(startMs, "loadAsr");
            String asrText = tryLoadAsrText(report, withhold);
            if (asrText == null) {
                // 已在 tryLoadAsrText 内处理 NOT_APPLICABLE + returnAiToken
                return;
            }
            // DEBUG: 排查 ASR 是否真有内容（用户报告"输入仅段落标记无文本"）
            log.info("[script-monitor-asr] reportId={} asrText 拼接完成 length={} 前200字={}",
                    reportId, asrText.length(), StrUtil.subPre(asrText, 200));

            // b. 解析直播间所属行业，按行业+租户取生成提示词（cueType=16） + 合并提示词（cueType=17）
            checkTimeout(startMs, "resolveTradeId");
            Long tradeId = resolveTradeId(report);
            Long tenantId = report.getTenantId();
            checkTimeout(startMs, "loadGenerateCue");
            CueWordsInfoVo generateCue = loadSingleCueWord(QUALITY_GENERATE_CUE_TYPE, "质检生成提示词", tradeId, tenantId);
            checkTimeout(startMs, "loadMergeCue");
            CueWordsInfoVo mergeCue = loadSingleCueWord(QUALITY_MERGE_CUE_TYPE, "质检合并提示词", tradeId, tenantId);

            // c. 取 AI 模型（中间报告 + 合并报告按 monitorType 分别配置）
            checkTimeout(startMs, "loadAiModel");
            Integer monitorType = report.getMonitorType();
            interModelConfig = loadInterAiModel(monitorType);
            mergeModelConfig = loadMergeAiModel(monitorType);
            log.info("[script-monitor-gen] reportId={} monitorType={} 中间模型 code={} name={} | 合并模型 code={} name={}",
                    reportId, monitorType,
                    interModelConfig.getModelCode(), interModelConfig.getModelName(),
                    mergeModelConfig.getModelCode(), mergeModelConfig.getModelName());

            // d. 清理旧中间报告（重新生成时确保幂等）
            intermediateRepository.deleteByReportId(reportId);

            // 排障日志：打印传给 AI 模型的完整中间报告输入（3 次 fan-out 输入完全一致，只打一次）
            // 注：含 ASR 全文 + 提示词原文，量大且敏感，降级为 DEBUG，生产默认不输出（需排障时临时开 DEBUG）
            log.debug("[质检-AI输入-中间报告] reportId={} systemPrompt={} userContent={}",
                    reportId, generateCue.getProblem(), asrText);

            // e. fan-out 生成 3 份中间报告（虚拟线程并发，AC-009/AC-011/AC-012）
            //    每次 AI 调用输入完全一样（业务靠 AI 随机性拿 3 个独立答案后合并），故 params 提到循环外算一次。
            checkTimeout(startMs, "before_intermediate_fanout");
            final AiMessageBo sharedInterParams = buildAiMessage(generateCue.getProblem(), asrText);
            final AiModelBo interModelConfigFinal = interModelConfig;

            List<CompletableFuture<AiReturnDataVo>> interFutures = IntStream.range(0, INTERMEDIATE_REPORT_COUNT)
                    .mapToObj(idx -> CompletableFuture.<AiReturnDataVo>supplyAsync(
                            () -> aiChatFeign.chatCompletion(interModelConfigFinal, sharedInterParams, reportId),
                            scriptMonitorVtExecutor))
                    .toList();

            // 整体超时（AC-012）：5min 内 3 次 AI 调用应完成；超时取消未完成 future + 抛业务异常
            try {
                CompletableFuture.allOf(interFutures.toArray(new CompletableFuture[0]))
                        .get(FANOUT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            } catch (TimeoutException te) {
                interFutures.forEach(f -> f.cancel(true));
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                        "中间报告并发生成整体超时(" + FANOUT_TIMEOUT_MINUTES + "min) reportId=" + reportId);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                interFutures.forEach(f -> f.cancel(true));
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                        "中间报告 fan-out 被中断 reportId=" + reportId);
            } catch (ExecutionException ee) {
                // 单个 task 异常 → allOf 抛 ExecutionException(cause = 真实异常)。fail-fast:解开后抛出。
                Throwable cause = ee.getCause();
                if (cause instanceof BusinessException be) {
                    throw be;
                }
                if (cause instanceof RuntimeException re) {
                    throw re;
                }
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                        "中间报告 fan-out 异常: " + (cause == null ? ee.getMessage() : cause.getMessage()));
            }

            // 串行收集结果（保留原失败检查 + token 累加 + DB save 语义,AC-011 fail-fast）
            for (int idx = 0; idx < INTERMEDIATE_REPORT_COUNT; idx++) {
                AiReturnDataVo interResult = interFutures.get(idx).join();
                if (interResult == null || Objects.equals(interResult.getStatus(), 1)) {
                    throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                            "中间报告生成失败，idx=" + idx);
                }
                log.info("质检中间报告 requestId={} totalTokens={} idx={} contentLen={} 前200字={}",
                        interResult.getRequestId(), interResult.getTotalTokens(), idx,
                        interResult.getContent() == null ? 0 : interResult.getContent().length(),
                        StrUtil.subPre(interResult.getContent(), 200));
                // 2026-06-09：对齐 AI 对话 / 通用 AI 调用业务，按 model 系数（×1.5×consumeMultiple）算实扣 token
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

            // f. 取 3 份中间报告 → 拼合并输入
            List<ScriptMonitorIntermediateReportEntity> inters =
                    intermediateRepository.findByReportIdOrderByIndexAsc(reportId);
            String mergeInput = inters.stream()
                    .map(ScriptMonitorIntermediateReportEntity::getContent)
                    .collect(Collectors.joining("\n---\n"));

            checkTimeout(startMs, "merge");
            // 排障日志：打印传给 AI 模型的完整合并报告输入（提示词 + 3 份中间报告拼接结果）
            // 注：内容量大，降级为 DEBUG，生产默认不输出（需排障时临时开 DEBUG）
            log.debug("[质检-AI输入-合并报告] reportId={} systemPrompt={} userContent={}",
                    reportId, mergeCue.getProblem(), mergeInput);
            AiMessageBo mergeParams = buildAiMessage(mergeCue.getProblem(), mergeInput);
            finalResult = aiChatFeign.chatCompletion(mergeModelConfig, mergeParams, reportId);
            if (finalResult == null || Objects.equals(finalResult.getStatus(), 1)) {
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                        "合并报告生成失败");
            }
            log.info("质检合并报告 requestId={} totalTokens={} mergeInputLen={} contentLen={} 前200字={}",
                    finalResult.getRequestId(), finalResult.getTotalTokens(), mergeInput.length(),
                    finalResult.getContent() == null ? 0 : finalResult.getContent().length(),
                    StrUtil.subPre(finalResult.getContent(), 200));
            // 2026-06-09：合并报告按合并模型系数算实扣 token（×1.5×consumeMultiple）
            totalTokens += (int) AiUtils.aiTokenConsumeMultiple(
                    (long) safeInt(finalResult.getTotalTokens()), mergeModelConfig.getConsumeMultiple());

            // g. 覆盖写最终报告正文（先删旧再插新）
            bodyRepository.deleteByReportId(reportId);
            ScriptMonitorReportBodyEntity body = ScriptMonitorReportBodyEntity.builder()
                    .reportId(reportId)
                    .content(finalResult.getContent())
                    .build();
            bodyRepository.save(body);

            // h. 从 AI 合并报告 Markdown 提取 <aifupan-data-block>...</aifupan-data-block> 标签内文本（trim 首尾空白；找不到设 null）
            buildSummary(report, finalResult.getContent());

            // i. UPDATE MySQL（GENERATED + summary_json + reportBodyId）—— 走独立 Bean 确保 @Transactional 生效
            reportWriteService.finishReport(reportId, body.getId(), report.getSummaryJson());

            // j. 实扣 Token（userId 为触发者，tenantId 仅用于台账 recordBo）
            AiReturnDataVo settleResult = buildSettleResult(finalResult, totalTokens);
            // Token 台账记录用合并模型作为代表（最终输出对应的模型）
            AiTokenUseRecordBo recordBo = buildTokenRecord(report, mergeModelConfig, finalResult);
            aiTokenWithholdFeign.settleAiToken(withhold, report.getUserId(), settleResult, recordBo);

        } catch (Exception e) {
            log.error("质检生成失败 reportId={}", reportId, e);
            // 归还预扣
            try {
                aiTokenWithholdFeign.returnAiToken(withhold);
            } catch (Exception re) {
                log.error("returnAiToken 失败 reportId={}", reportId, re);
            }
            // 状态补偿：若原本是 GENERATED 则恢复旧成功报告，否则置为 GENERATE_FAILED（走独立 Bean）
            reportWriteService.restoreOrFail(reportId, originalStatus, oldBodyId, oldSummaryJson, e.getMessage());
        }
    }

    // ====== 私有辅助方法 ======

    /**
     * 尝试取 ASR 文本，ASR 为空时置 NOT_APPLICABLE + 归还预扣，返回 null。
     *
     * <p>AC-6：ASR 为空 → status=4 NOT_APPLICABLE + unavailableReason 填充 + returnAiToken。</p>
     *
     * @param report   报告实体
     * @param withhold 预扣凭据
     * @return ASR 拼接文本，若 ASR 为空则返回 null（已处理状态补偿）
     */
    private String tryLoadAsrText(ScriptMonitorReportEntity report, RedisWithholdVo withhold) {
        R<AnalysisResultVo> asrR = sensitiveWordsFeign.getAnalysisData(
                report.getSourceType(), report.getSourceId());
        if (asrR == null || asrR.getData() == null
                || CollUtil.isEmpty(asrR.getData().getSentenceMarkVos())) {
            log.warn("ASR 数据为空，reportId={} 置 NOT_APPLICABLE", report.getId());
            try {
                aiTokenWithholdFeign.returnAiToken(withhold);
            } catch (Exception re) {
                log.error("returnAiToken 失败(NOT_APPLICABLE) reportId={}", report.getId(), re);
            }
            // 走独立 Bean 确保 @Transactional 生效
            reportWriteService.markNotApplicable(report.getId(), "ASR 数据为空，无法生成质检报告");
            return null;
        }
        List<SentenceMarkVo> sentences = asrR.getData().getSentenceMarkVos();
        // 按 currentSort 升序排列后拼接
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
     * 毫秒时间戳格式化为 HH:mm:ss（偏 -8 小时去时区，与 AI 问答 Script/FileSentenceImpl 范式对齐）。
     *
     * <p>词级 startTime/endTime 表示视频内毫秒偏移；DateTime 默认按本地时区（+8）解析 epoch ms，
     * 偏 -8 小时后再格式化即可得到视频内自然时间。</p>
     *
     * <p><b>@implNote</b>：本方法依赖 JVM 时区为 Asia/Shanghai（UTC+8）—— 与现网 AI 问答
     * ScriptSentenceImpl:122 / FileSentenceImpl:72 同样的隐式假设。部署到 Docker 镜像时
     * 须确认 TZ=Asia/Shanghai 环境变量（否则 UTC 默认会让 offset -8h 推算落到前一天 16 时附近）。</p>
     *
     * @param ms 毫秒偏移
     * @return HH:mm:ss 格式串
     */
    private static String formatMsToHms(long ms) {
        return DateUtil.format(new DateTime(ms).offset(DateField.HOUR, -8), "HH:mm:ss");
    }

    /**
     * 按行业 + 租户 + cueType 取单条提示词（含 problem 字段，复用优化原文范式）。
     *
     * <p>通过 {@link CueWordsFeign#getCueWordByTradeAndType} 命中：内部走行业父链回溯
     * + 末尾兜底 trade_id=1；同一行业层级租户定制（tenant_id=tenantId）赢通用（tenant_id=0），
     * 跨层级仍按子→父→兜底顺序。{@code tenantId=null} 或 {@code 0} 时退化为仅查通用。
     * problem 字段不被清空，避免 pageCueWords C2 坑。</p>
     *
     * @param cueType    助手类型（参见 AiEnums.askType 枚举 16/17/18/19/20/21）
     * @param promptName 提示词业务名（用于异常文案）
     * @param tradeId    直播间所属行业 id（由 {@link #resolveTradeId} 解析）
     * @param tenantId   租户 id（来自 {@code report.getTenantId()}）
     * @return 命中的提示词（含 problem）
     * @throws BusinessException 该 tradeId + tenantId 下无配置时抛业务异常（文案携带 tradeId/tenantId 便于排查）
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
     * 按 report.sourceType + sourceId 解析直播间所属行业 id。
     *
     * <p>取值规则：</p>
     * <ul>
     *   <li>sourceType=0（录制视频）→ 直播间维度 tb_anchor_url_user.trade_id（按 secUid+userId+tenantId 取）；
     *       取不到回退 1L。不再用录制记录 tb_anchor_video.trade_id</li>
     *   <li>sourceType=1（上传文件）→ tb_upload_file.trade_id（经 SensitiveWordsFeign SPI 取）；
     *       取不到回退 1L（DEBT-019 修复，2026-06-10）</li>
     * </ul>
     *
     * @param report 报告实体（持有 sourceType + sourceId）
     * @return 行业 id，兜底通用行业 1L
     */
    private Long resolveTradeId(ScriptMonitorReportEntity report) {
        if (Objects.equals(report.getSourceType(), USE_SOURCE_TYPE_VIDEO)) {
            AnchorVideoInfoVo video = ResultUtil.getResult(
                    anchorVideoFeign.GetByVideoId(report.getSourceId()));
            // 行业取直播间维度（tb_anchor_url_user.trade_id），不用录制记录的 video.tradeId
            if (video != null && StrUtil.isNotBlank(video.getSecUid())) {
                AnchorUrlUserVo anchorUser = ResultUtil.getResult(anchorUrlUserFeign.getBySecUidAndUser(
                        video.getSecUid(), report.getUserId(), report.getTenantId()));
                if (anchorUser != null && anchorUser.getTradeId() != null) {
                    return anchorUser.getTradeId();
                }
            }
        } else {
            // sourceType=1 上传文件：经 generic SPI 拿 tb_upload_file.trade_id
            UploadFileSimpleInfoVo fileInfo = ResultUtil.getResult(
                    sensitiveWordsFeign.getUploadFileInfo(report.getSourceId()));
            if (fileInfo != null && fileInfo.getTradeId() != null) {
                return fileInfo.getTradeId();
            }
        }
        return 1L;
    }

    /**
     * 加载 AI 模型配置。
     *
     * <p>使用 listDiagnosisModel 取列表中第一个（同诊断模型），不在 B4 范围内新增枚举。</p>
     *
     * @return AI 模型 Bo
     * @throws BusinessException 无可用模型时抛业务异常
     */
    private AiModelBo loadAiModel() {
        List<AiModelInfoVo> models = aiModelFeign.listDiagnosisModel();
        if (CollUtil.isEmpty(models)) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                    "未找到可用 AI 模型配置，请联系管理员");
        }
        AiModelInfoVo info = models.get(0);
        AiModelBo bo = new AiModelBo();
        BeanUtils.copyProperties(info, bo);
        return bo;
    }

    /**
     * 按 monitorType 加载中间报告使用的 AI 模型。
     * systemKv 配置缺失或对应 modelCode 在 tb_ai_model 找不到时，回退到 {@link #loadAiModel()} 字典 list[0]。
     *
     * @param monitorType 0 质检 / 1 还原度 / 2 巡检
     * @return AiModelBo
     */
    private AiModelBo loadInterAiModel(Integer monitorType) {
        return loadAiModelByKvKey(resolveInterKvKey(monitorType));
    }

    /**
     * 按 monitorType 加载合并报告使用的 AI 模型。
     * 兜底语义同 {@link #loadInterAiModel(Integer)}。
     *
     * @param monitorType 0 质检 / 1 还原度 / 2 巡检
     * @return AiModelBo
     */
    private AiModelBo loadMergeAiModel(Integer monitorType) {
        return loadAiModelByKvKey(resolveMergeKvKey(monitorType));
    }

    /**
     * monitorType → 中间报告 systemKv key 映射。
     */
    private String resolveInterKvKey(Integer monitorType) {
        if (monitorType == null) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                    "monitorType 不能为空");
        }
        return switch (monitorType) {
            case 0 -> "script_monitor_quality_inter_ai_model";
            case 1 -> "script_monitor_fidelity_inter_ai_model";
            case 2 -> "script_monitor_patrol_inter_ai_model";
            default -> throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                    "不支持的 monitorType=" + monitorType);
        };
    }

    /**
     * monitorType → 合并报告 systemKv key 映射。
     */
    private String resolveMergeKvKey(Integer monitorType) {
        if (monitorType == null) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                    "monitorType 不能为空");
        }
        return switch (monitorType) {
            case 0 -> "script_monitor_quality_merge_ai_model";
            case 1 -> "script_monitor_fidelity_merge_ai_model";
            case 2 -> "script_monitor_patrol_merge_ai_model";
            default -> throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                    "不支持的 monitorType=" + monitorType);
        };
    }

    /**
     * 按 systemKv key 加载 AI 模型，兜底回退到 {@link #loadAiModel()} 字典 list[0]。
     *
     * <p>兜底场景：</p>
     * <ul>
     *   <li>systemKv 没配置该 key 或 kvValue 为空 → 回退</li>
     *   <li>kvValue 对应的 modelCode 在 tb_ai_model 找不到（已删除/code 错） → 回退</li>
     * </ul>
     *
     * @param kvKey systemKv key
     * @return AiModelBo
     */
    private AiModelBo loadAiModelByKvKey(String kvKey) {
        SystemKvInfoVo kv = systemKvProducer.getByKey(kvKey);
        if (kv == null || StrUtil.isBlank(kv.getKvValue())) {
            log.warn("[script-monitor-gen] systemKv 未配置 {}, 回退字典 listDiagnosisModel.get(0)", kvKey);
            return loadAiModel();
        }
        String modelCode = kv.getKvValue();
        AiModelInfoVo model = aiModelFeign.getByCode(modelCode);
        if (model == null) {
            log.warn("[script-monitor-gen] systemKv 配置的 modelCode 在 tb_ai_model 找不到, 回退字典 list[0]. kvKey={} code={}",
                    kvKey, modelCode);
            return loadAiModel();
        }
        AiModelBo bo = new AiModelBo();
        BeanUtils.copyProperties(model, bo);
        return bo;
    }

    /**
     * 构造 AI 调用消息体。
     *
     * @param systemPrompt 系统提示词（测试期 caller 在调用前会打印完整内容到日志，方便排障；上线前评估）
     * @param userContent  用户内容（同上）
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
     * @param lastResult   最后一次 AI 调用返回（复制元数据）
     * @param totalTokens  本次全流程消耗的 Token 总量
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
     * @param report      报告实体（含 tenantId/sourceType/sourceId）
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
        bo.setAssistantType(ASSISTANT_TYPE_QUALITY);
        return bo;
    }

    /**
     * 从 AI 合并报告 Markdown 提取 {@code <aifupan-data-block ...>...</aifupan-data-block>} 标签内文本，
     * 写入 entity.summaryJson 字段。
     *
     * <p>期望输入：AI 合并报告 Markdown 全文，关键摘要由 {@code <aifupan-data-block>} 标签包裹。
     * 匹配规则见 {@link #SUMMARY_TAG_PATTERN}：标签大小写不敏感、属性可选（含自闭合 {@code />} 形式）、
     * 内容允许跨行、多标签取首个，提取后 {@code .trim()} 去首尾空白。找不到标签 /
     * {@code mergedAiContent} 为 null → {@code setSummaryJson(null)}，不阻塞主流程，仅 {@code log.warn}。</p>
     *
     * <p>字段名沿用 {@code summaryJson} 是历史命名（最初存 JSON 子对象 toJSONString），现存
     * Markdown 文本片段；为零迁移成本保留字段名。</p>
     *
     * @param report          报告实体（summaryJson 字段写入此对象）
     * @param mergedAiContent AI 合并报告 Markdown 字符串
     */
    private void buildSummary(ScriptMonitorReportEntity report, String mergedAiContent) {
        if (mergedAiContent == null) {
            log.warn("质检合并报告内容为 null reportId={}", report.getId());
            report.setSummaryJson(null);
            return;
        }
        Matcher matcher = SUMMARY_TAG_PATTERN.matcher(mergedAiContent);
        if (matcher.find()) {
            report.setSummaryJson(matcher.group(1).trim());
            return;
        }
        log.warn("质检合并报告 Markdown 无 <aifupan-data-block> 标签 reportId={}, content 前 200 字={}",
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
     * 任务级超时检查（P1 修补）。
     *
     * <p>generate() 关键步骤前调用；超过 {@link #MAX_TASK_DURATION_MS}（10 分钟）
     * 主动抛 BusinessException 走外层 catch 完成 returnAiToken + 状态补偿。
     * 不依赖 Future.cancel / 线程 interrupt（OpenFeign 默认不响应中断）。</p>
     *
     * @param startMs 任务开始时间戳（System.currentTimeMillis()）
     * @param stage   当前阶段名（用于日志定位）
     * @throws BusinessException 超时抛出（code=SCRIPT_MONITOR_PARAM_INVALID）
     */
    private void checkTimeout(long startMs, String stage) {
        long elapsedMs = System.currentTimeMillis() - startMs;
        if (elapsedMs > MAX_TASK_DURATION_MS) {
            throw new BusinessException(
                    StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                    "任务超时(已运行 " + (elapsedMs / 1000) + "s)@" + stage);
        }
    }
}
