package com.jiuyu.replay.ai.bll;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.ai.entity.ScriptMonitorReportBodyEntity;
import com.jiuyu.replay.ai.entity.ScriptMonitorReportEntity;
import com.jiuyu.replay.ai.repository.mongo.ScriptMonitorReportBodyRepository;
import com.jiuyu.replay.ai.repository.service.ScriptMonitorReportService;
import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.common.producer.SystemKvProducer;
import com.jiuyu.replay.common.utils.AiUtils;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.bo.ai.AiMessageBo;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.generic.bo.third.AiTokenUseRecordBo;
import com.jiuyu.replay.generic.bo.third.QueryDanMuBo;
import com.jiuyu.replay.generic.enums.words.MonitorSourceTypeEnum;
import com.jiuyu.replay.generic.enums.words.VideoPlatformEnum;
import com.jiuyu.replay.generic.feign.ai.AiChatFeign;
import com.jiuyu.replay.generic.feign.order.AiTokenWithholdFeign;
import com.jiuyu.replay.generic.feign.third.AiModelFeign;
import com.jiuyu.replay.generic.feign.third.TableStoreFeign;
import com.jiuyu.replay.generic.feign.words.AnchorUrlUserFeign;
import com.jiuyu.replay.generic.feign.words.AnchorVideoFeign;
import com.jiuyu.replay.generic.bo.words.cue.CueWordsQueryBo;
import com.jiuyu.replay.generic.feign.words.CueWordsFeign;
import com.jiuyu.replay.generic.feign.words.SensitiveWordsFeign;
import com.jiuyu.replay.generic.feign.words.TradeFeign;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.order.RedisWithholdVo;
import com.jiuyu.replay.generic.vo.third.DanMuItemVo;
import com.jiuyu.replay.generic.vo.words.AnalysisResultVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.generic.vo.words.CueWordsInfoVo;
import com.jiuyu.replay.generic.vo.words.SentenceMarkVo;
import com.jiuyu.replay.generic.vo.words.TradeVo;
import com.jiuyu.replay.generic.vo.words.WordListItemVo;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 互动巡检报告异步生成 Bll（monitorType=2 主流程）。
 *
 * <p>职责：接受由 {@link ScriptMonitorMqHandler} 路由的 tag=interaction-patrol 消息，
 * 按以下顺序执行：
 * <ol>
 *   <li>取视频信息，校验 existBarrage；无弹幕则置 NOT_APPLICABLE + 返还 Token</li>
 *   <li>加载全量 ASR 段落（SensitiveWordsFeign SPI）</li>
 *   <li>计算切片数量 N = ceil(duration / 10min)</li>
 *   <li>按行业取切片提示词（cueType=20）+ 合并提示词（cueType=21）</li>
 *   <li>串行切片 AI 调用（N 个单元，各含弹幕+ASR 归桶）</li>
 *   <li>合并 N 个单元结果，AI 合并调用</li>
 *   <li>合并结果经 AI 格式校验并纠正（cueType=28，单次调用；该调用 token 不计入结算）后得最终正文</li>
 *   <li>MongoDB 存最终报告正文 + MySQL 更新状态+summaryJson</li>
 *   <li>实扣 Token</li>
 * </ol>
 * 任何步骤失败则执行补偿：调 returnAiToken + 恢复/降级 status。</p>
 *
 * <p><b>注意：</b>本类不加 {@code @Transactional}（禁止长事务，含多次 AI 调用）；
 * 事务操作下沉到 {@link ScriptMonitorReportWriteService}（独立 Bean）。</p>
 *
 * @author beta
 * @date 2026-06-04
 */
@Slf4j
@Component
public class ScriptMonitorPatrolGenerateBll {

    /**
     * 单任务总超时阈值（60 分钟，毫秒）— 修复 duration 单位错配后，1h 直播 24 切片 × 60s ≈ 24min，需提高超时阈值
     */
    private static final long MAX_TASK_DURATION_MS = 60 * 60 * 1000L;

    /**
     * 单个切片窗口时长（10 分钟，毫秒）
     */
    private static final long UNIT_WINDOW_MS = 10 * 60 * 1000L;

    /**
     * ASR 归桶尾延（1 分钟，毫秒），保证过渡区段落不被切掉
     */
    private static final long ASR_TAIL_EXTEND_MS = 60 * 1000L;

    /**
     * 巡检切片提示词 cueType（{@link AiEnums.askType#INTERACTION_PATROL_GENERATE_PROMPT}=20）
     */
    private static final int PATROL_GENERATE_CUE_TYPE = AiEnums.askType.INTERACTION_PATROL_GENERATE_PROMPT.getCode();

    /**
     * 巡检合并提示词 cueType（{@link AiEnums.askType#INTERACTION_PATROL_MERGE_PROMPT}=21）
     */
    private static final int PATROL_MERGE_CUE_TYPE = AiEnums.askType.INTERACTION_PATROL_MERGE_PROMPT.getCode();

    /**
     * 巡检格式校验并纠正提示词 cueType（{@link AiEnums.askType#INTERACTION_PATROL_FORMAT_PROMPT}=28）
     */
    private static final int PATROL_FORMAT_CUE_TYPE = AiEnums.askType.INTERACTION_PATROL_FORMAT_PROMPT.getCode();

    /**
     * 格式校验并纠正模型 systemKv key（快模型；缺配回退默认模型）
     */
    private static final String FORMAT_AI_MODEL_KV_KEY = "script_monitor_patrol_format_ai_model";

    /**
     * 格式合规哨兵：格式模型判定合规时仅返回此标记 → 用合并原文，不替换
     */
    private static final String FORMAT_OK_SENTINEL = "__FORMAT_OK__";

    /**
     * 资源类型：录制视频
     */
    private static final int SOURCE_TYPE_VIDEO = MonitorSourceTypeEnum.RECORD_VIDEO.getCode();

    /**
     * AI 台账 assistantType：复用 DATA_DIAGNOSIS(13)
     */
    // 2026-06-09：原 DATA_DIAGNOSIS(13) 错位复用，改用独立业务类型枚举 INTERACTION_PATROL(23)，
    // 让 tb_ai_token_use_record.assistant_type 可按 monitorType 维度统计 token 消耗
    private static final int ASSISTANT_TYPE_PATROL = AiEnums.askType.INTERACTION_PATROL.getCode();

    /**
     * 弹幕分页 limit（取全量，-1 不分页）
     */
    private static final int BARRAGE_LIMIT_ALL = -1;

    /**
     * Summary 标签匹配 pattern：从 AI 合并报告 Markdown 中提取
     * {@code <aifupan-data-block ...>...</aifupan-data-block>} 内文本（mirror 质检
     * {@link ScriptMonitorGenerateBll} SUMMARY_TAG_PATTERN 范式）。
     *
     * <p>开标签兼容：</p>
     * <ul>
     *   <li>带属性 {@code <aifupan-data-block resource-data visible="false">}</li>
     *   <li>自闭合形式 {@code <aifupan-data-block resource-data visible="false"/>}</li>
     *   <li>无属性 {@code <aifupan-data-block>}</li>
     * </ul>
     *
     * <p>{@code [^>]*}：匹配 0 或多个非 {@code >} 字符（属性段）；
     * {@code CASE_INSENSITIVE}：标签大小写不敏感；
     * {@code DOTALL}：{@code .} 匹配换行（支持跨行内容）；
     * {@code .*?} 非贪婪：多个标签时取首个。</p>
     */
    private static final Pattern SUMMARY_TAG_PATTERN = Pattern.compile(
            "<aifupan-data-block[^>]*>(.*?)</aifupan-data-block>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final ScriptMonitorReportService reportService;
    private final ScriptMonitorReportBodyRepository bodyRepository;
    private final AnchorVideoFeign anchorVideoFeign;
    private final AnchorUrlUserFeign anchorUrlUserFeign;
    private final SensitiveWordsFeign sensitiveWordsFeign;
    private final TableStoreFeign tableStoreFeign;
    private final CueWordsFeign cueWordsFeign;
    private final TradeFeign tradeFeign;
    private final AiModelFeign aiModelFeign;
    private final AiChatFeign aiChatFeign;
    private final AiTokenWithholdFeign aiTokenWithholdFeign;
    private final PatrolPromptFiller patrolPromptFiller;
    /**
     * 报告状态写入 Service（独立 Spring Bean，解决 self-call 导致 @Transactional 失效问题）
     */
    private final ScriptMonitorReportWriteService reportWriteService;
    /**
     * systemKv 按 phase 取 AI 模型 code（{@code script_monitor_patrol_inter_ai_model} 切片 /
     * {@code script_monitor_patrol_merge_ai_model} 合并）。Mockito 默认 getByKey 返 null 自动走兜底。
     */
    private final SystemKvProducer systemKvProducer;

    /**
     * JDK 21 虚拟线程 executor（{@code VirtualThreadExecutorConfig#scriptMonitorVtExecutor}）。
     * 用于 fan-out unitCount 个 unit 的弹幕查询 + ASR 切片 + AI 调用。
     * 字段名与 Bean 名一致，Spring 按类型+名字 fallback 注入。
     */
    private final Executor scriptMonitorVtExecutor;

    /**
     * fan-out 整体超时:unitCount 通常 ≤10，单 unit 含 Feign(弹幕)+AI 调用约 1-2min,
     * 并发后理论 2-3min 完成；留 10min 余量兜底大 unitCount 场景。
     */
    private static final long PATROL_FANOUT_TIMEOUT_MINUTES = 10L;

    /**
     * 构造器注入（禁 @Autowired / @Resource）。
     *
     * @param reportService         报告 Service
     * @param bodyRepository        MongoDB 报告正文 Repository
     * @param anchorVideoFeign      视频 Feign（取视频信息）
     * @param anchorUrlUserFeign    主播用户关联 Feign（取直播间维度行业）
     * @param sensitiveWordsFeign   ASR Feign
     * @param tableStoreFeign       弹幕 Feign SPI（queryDanMuSearchData）
     * @param cueWordsFeign         提示词 Feign（按行业+cueType 取词）
     * @param tradeFeign            行业 Feign（取行业名称）
     * @param aiModelFeign          AI 模型 Feign
     * @param aiChatFeign               AI 调用 Feign
     * @param aiTokenWithholdFeign  Token 预扣/实扣/返还 Feign
     * @param patrolPromptFiller    占位符填充工具
     * @param reportWriteService    报告状态事务写入 Service
     * @param systemKvProducer      systemKv 取 AI 模型 code 配置
     * @param scriptMonitorVtExecutor JDK 21 虚拟线程 executor（unit fan-out 用）
     */
    public ScriptMonitorPatrolGenerateBll(ScriptMonitorReportService reportService,
                                          ScriptMonitorReportBodyRepository bodyRepository,
                                          AnchorVideoFeign anchorVideoFeign,
                                          AnchorUrlUserFeign anchorUrlUserFeign,
                                          SensitiveWordsFeign sensitiveWordsFeign,
                                          TableStoreFeign tableStoreFeign,
                                          CueWordsFeign cueWordsFeign,
                                          TradeFeign tradeFeign,
                                          AiModelFeign aiModelFeign,
                                          AiChatFeign aiChatFeign,
                                          AiTokenWithholdFeign aiTokenWithholdFeign,
                                          PatrolPromptFiller patrolPromptFiller,
                                          ScriptMonitorReportWriteService reportWriteService,
                                          SystemKvProducer systemKvProducer,
                                          Executor scriptMonitorVtExecutor) {
        this.reportService = reportService;
        this.bodyRepository = bodyRepository;
        this.anchorVideoFeign = anchorVideoFeign;
        this.anchorUrlUserFeign = anchorUrlUserFeign;
        this.sensitiveWordsFeign = sensitiveWordsFeign;
        this.tableStoreFeign = tableStoreFeign;
        this.cueWordsFeign = cueWordsFeign;
        this.tradeFeign = tradeFeign;
        this.aiModelFeign = aiModelFeign;
        this.aiChatFeign = aiChatFeign;
        this.aiTokenWithholdFeign = aiTokenWithholdFeign;
        this.patrolPromptFiller = patrolPromptFiller;
        this.reportWriteService = reportWriteService;
        this.systemKvProducer = systemKvProducer;
        this.scriptMonitorVtExecutor = scriptMonitorVtExecutor;
    }

    /**
     * 互动巡检报告异步生成入口（由 MQ consumer 调用，禁止加 @Transactional）。
     *
     * @param report         报告行（status 已置为 GENERATING）
     * @param originalStatus 触发前原始状态（用于失败时恢复）
     * @param withhold       预扣 Token 凭据（自动触发时为 null）
     */
    public void generate(ScriptMonitorReportEntity report, Integer originalStatus, RedisWithholdVo withhold) {
        Long reportId = report.getId();
        String oldBodyId = report.getReportBodyId();
        String oldSummaryJson = report.getSummaryJson();
        int totalTokens = 0;
        AiModelBo sliceModelConfig = null;
        AiModelBo mergeModelConfig = null;

        // a. 任务级总超时基准
        long startMs = System.currentTimeMillis();

        try {
            // b. 加载视频信息，校验弹幕前提
            checkTimeout(startMs, "loadVideo");
            AnchorVideoInfoVo video = ResultUtil.getResult(anchorVideoFeign.GetByVideoId(report.getSourceId()));
            if (video == null) {
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                        "视频信息不存在 sourceId=" + report.getSourceId());
            }
            // 弹幕检测前提：无弹幕 或 batchNumber 为空 → NOT_APPLICABLE
            // 注意：不能用 video.getExistBarrage() —— 单视频查询 GetByVideoId 不填充该字段（始终 null）
            // 必须走 hasBarrage SPI 查 tb_socket_collect_message 实时判定
            Boolean hasBarrage = ResultUtil.getResult(anchorVideoFeign.hasBarrage(report.getSourceId()));
            if (!Boolean.TRUE.equals(hasBarrage) || StrUtil.isBlank(video.getBatchNumber())) {
                log.warn("[互动巡检] 无弹幕，置 NOT_APPLICABLE reportId={} hasBarrage={} batchNumber={}",
                        reportId, hasBarrage, video.getBatchNumber());
                try {
                    aiTokenWithholdFeign.returnAiToken(withhold);
                } catch (Exception re) {
                    log.error("[互动巡检] returnAiToken 失败(NOT_APPLICABLE) reportId={}", reportId, re);
                }
                reportWriteService.markNotApplicable(reportId, "本场无弹幕");
                return;
            }

            // c. 加载全量 ASR 段落（PRD 8.1 数据质量约束：缺 ASR 不进入有效分析 → NOT_APPLICABLE）
            checkTimeout(startMs, "loadAsr");
            List<SentenceMarkVo> sentences = loadAsrSentences(report);
            if (CollUtil.isEmpty(sentences)) {
                log.warn("[互动巡检] ASR 数据为空，置 NOT_APPLICABLE reportId={}", reportId);
                try {
                    aiTokenWithholdFeign.returnAiToken(withhold);
                } catch (Exception re) {
                    log.error("[互动巡检] returnAiToken 失败(ASR 空) reportId={}", reportId, re);
                }
                reportWriteService.markNotApplicable(reportId, "本场 ASR 数据为空，无法生成互动巡检报告");
                return;
            }

            // d. 计算切片数量 N = ceil(duration / 10min)，最小 1 个单元
            // video.getDuration() 单位为【秒】（DB COMMENT '时长（秒）'），必须先 *1000 转为毫秒再与 UNIT_WINDOW_MS 相除
            Long durationSec = video.getDuration();
            long durationMs;
            if (durationSec == null || durationSec <= 0) {
                durationMs = UNIT_WINDOW_MS;  // 无时长按 1 单元兜底
                log.warn("[互动巡检] reportId={} video.duration 为空或非正数（{}），按 1 单元兜底 unitWindow={}ms",
                        reportId, durationSec, UNIT_WINDOW_MS);
            } else {
                durationMs = durationSec * 1000L;
            }
            int unitCount = (int) Math.max(1, Math.ceil((double) durationMs / UNIT_WINDOW_MS));
            log.info("[互动巡检] reportId={} durationSec={} durationMs={} unitCount={}",
                    reportId, durationSec, durationMs, unitCount);

            // e. 按行业取提示词
            checkTimeout(startMs, "loadCueWords");
            Long tradeId = resolveTradeId(video, report);
            Long tenantId = report.getTenantId();
            CueWordsInfoVo sliceCue = loadSingleCueWord(PATROL_GENERATE_CUE_TYPE, "互动巡检切片提示词", tradeId, tenantId);
            CueWordsInfoVo mergeCue = loadSingleCueWord(PATROL_MERGE_CUE_TYPE, "互动巡检合并提示词", tradeId, tenantId);

            // f. 取 AI 模型配置（切片 + 合并按 systemKv 分别配置）
            checkTimeout(startMs, "loadAiModel");
            sliceModelConfig = loadAiModelByKvKey("script_monitor_patrol_inter_ai_model");
            mergeModelConfig = loadAiModelByKvKey("script_monitor_patrol_merge_ai_model");
            log.info("[互动巡检] reportId={} 切片模型 code={} name={} | 合并模型 code={} name={}",
                    reportId,
                    sliceModelConfig.getModelCode(), sliceModelConfig.getModelName(),
                    mergeModelConfig.getModelCode(), mergeModelConfig.getModelName());

            // 占位符填充数据：平台名 + 行业名
            String platformName = resolvePlatformName(video.getPlatformType());
            String tradeName = resolveTradeName(tradeId);

            // g. 超时检查 + 视频起始 epoch 基准（弹幕 recordDate 是 epoch ms，须加偏移）
            checkTimeout(startMs, "beforeSliceLoop");
            if (video.getStartTime() == null) {
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                        "视频缺少 startTime，无法计算弹幕时间窗 sourceId=" + report.getSourceId());
            }
            long videoStartEpoch = video.getStartTime().getTime();

            // h. fan-out 切片 AI 调用（虚拟线程并发，AC-009/AC-010 顺序保持/AC-011 fail-fast/AC-012 整体超时）
            //    每个 unit 内部含:queryDanmuForUnit(Feign+Redis) + sliceAsrUnits(纯计算) + AI 调用,
            //    互不依赖前一个 unit 结果,可独立并发。
            checkTimeout(startMs, "before_unit_fanout");
            final AnchorVideoInfoVo videoFinal = video;
            final ScriptMonitorReportEntity reportFinal = report;
            final List<SentenceMarkVo> sentencesFinal = sentences;
            final long videoStartEpochFinal = videoStartEpoch;
            final String sliceCueProblemFinal = sliceCue.getProblem();
            final String platformNameFinal = platformName;
            final String tradeNameFinal = tradeName;
            final AiModelBo sliceModelConfigFinal = sliceModelConfig;
            final String reportIdFinal = String.valueOf(reportId);

            List<CompletableFuture<AiReturnDataVo>> unitFutures = IntStream.range(0, unitCount)
                    .mapToObj(i -> CompletableFuture.<AiReturnDataVo>supplyAsync(() -> {
                        long unitStartMs = (long) i * UNIT_WINDOW_MS;
                        long unitEndMs = (long) (i + 1) * UNIT_WINDOW_MS;
                        long asrEndMs = unitEndMs + ASR_TAIL_EXTEND_MS;

                        // 弹幕数据：TableStoreFeign（含 2h Redis 缓存）
                        List<DanMuItemVo> danmuList = queryDanmuForUnit(videoFinal, unitStartMs, unitEndMs,
                                reportFinal, videoStartEpochFinal);
                        String unitBarrageText = buildBarrageText(danmuList, videoStartEpochFinal);
                        log.info("[互动巡检] reportId={} unitIdx={} danmuCount={}",
                                reportIdFinal, i, danmuList.size());

                        // ASR 数据：sliceAsrUnits 归桶
                        String unitAsrText = sliceAsrUnits(sentencesFinal, unitStartMs, asrEndMs);
                        log.info("[互动巡检] reportId={} unitIdx={} asrCount={}", reportIdFinal, i,
                                StrUtil.isBlank(unitAsrText) ? 0 : unitAsrText.split("\n").length);

                        // 占位符填充
                        String filledPrompt = patrolPromptFiller.fill(sliceCueProblemFinal,
                                platformNameFinal, tradeNameFinal);

                        // AI 切片调用
                        String userContent = unitBarrageText + "\n" + unitAsrText;
                        // 排障日志：每切片打印 AI 完整输入（弹幕全文 + ASR），量大且随切片数放大，降级为 DEBUG，生产默认不输出
                        log.debug("[巡检-AI输入-切片] reportId={} unitIdx={} systemPrompt={} userContent={}",
                                reportIdFinal, i, filledPrompt, userContent);
                        AiMessageBo params = buildAiMessage(filledPrompt, userContent);
                        return aiChatFeign.chatCompletion(sliceModelConfigFinal, params, reportFinal.getId());
                    }, scriptMonitorVtExecutor))
                    .toList();

            // 整体超时（AC-012）：unitCount × AI 单耗时不超 10min（实际并发后远小于）
            try {
                CompletableFuture.allOf(unitFutures.toArray(new CompletableFuture[0]))
                        .get(PATROL_FANOUT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            } catch (TimeoutException te) {
                unitFutures.forEach(f -> f.cancel(true));
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                        "切片报告并发生成整体超时(" + PATROL_FANOUT_TIMEOUT_MINUTES + "min) reportId="
                                + reportId + " unitCount=" + unitCount);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                unitFutures.forEach(f -> f.cancel(true));
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                        "互动巡检 fan-out 被中断 reportId=" + reportId);
            } catch (ExecutionException ee) {
                Throwable cause = ee.getCause();
                if (cause instanceof BusinessException be) {
                    throw be;
                }
                if (cause instanceof RuntimeException re) {
                    throw re;
                }
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                        "互动巡检 fan-out 异常: " + (cause == null ? ee.getMessage() : cause.getMessage()));
            }

            // 按 i 顺序收集结果（AC-010 顺序保持: unitResults 0→N-1,后续 String.join 拼接保持时间轴）
            List<String> unitResults = new ArrayList<>(unitCount);
            for (int i = 0; i < unitCount; i++) {
                AiReturnDataVo unitResult = unitFutures.get(i).join();
                if (unitResult == null || Objects.equals(unitResult.getStatus(), 1)) {
                    throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                            "切片报告生成失败 unitIdx=" + i);
                }
                log.info("[互动巡检] 切片完成 reportId={} unitIdx={} totalTokens={} contentLen={}",
                        reportId, i, unitResult.getTotalTokens(),
                        unitResult.getContent() == null ? 0 : unitResult.getContent().length());
                // 2026-06-09：对齐 AI 对话 / 通用 AI 调用业务，按切片模型系数（×1.5×consumeMultiple）算实扣 token
                totalTokens += (int) AiUtils.aiTokenConsumeMultiple(
                        (long) safeInt(unitResult.getTotalTokens()), sliceModelConfig.getConsumeMultiple());
                unitResults.add(unitResult.getContent());
            }

            // i. 拼合并输入
            checkTimeout(startMs, "merge");
            String mergeInput = String.join("\n---\n", unitResults);

            // j. AI 合并调用（Bug-3 fix: 合并提示词同样经过占位符填充，防运营后台加 #{platform}/#{trade}）
            String filledMergePrompt = patrolPromptFiller.fill(mergeCue.getProblem(), platformName, tradeName);
            // 排障日志：打印合并 AI 输入完整内容（系统提示词 + N 段切片结果拼接），量大，降级为 DEBUG，生产默认不输出
            log.debug("[巡检-AI输入-合并] reportId={} systemPrompt={} userContent={}",
                    reportId, filledMergePrompt, mergeInput);
            AiMessageBo mergeParams = buildAiMessage(filledMergePrompt, mergeInput);
            AiReturnDataVo finalResult = aiChatFeign.chatCompletion(mergeModelConfig, mergeParams, reportId);
            if (finalResult == null || Objects.equals(finalResult.getStatus(), 1)) {
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                        "合并报告生成失败");
            }
            log.info("[互动巡检] 合并完成 reportId={} totalTokens={} contentLen={} 前200字={}",
                    reportId, finalResult.getTotalTokens(),
                    finalResult.getContent() == null ? 0 : finalResult.getContent().length(),
                    StrUtil.subPre(finalResult.getContent(), 200));
            // 2026-06-09：合并报告按合并模型系数算实扣 token（×1.5×consumeMultiple）
            totalTokens += (int) AiUtils.aiTokenConsumeMultiple(
                    (long) safeInt(finalResult.getTotalTokens()), mergeModelConfig.getConsumeMultiple());

            // j2. 合并报告 AI 格式校验与纠正（仅本环节 token 不计入 totalTokens、不结算；任何失败降级输出原文）
            String finalContent = validateAndFixMergeFormat(finalResult.getContent(),
                    tradeId, report.getTenantId(), reportId);
            if (!passesParseInvariants(finalContent)) {
                // 仍按原样落库；区分两种语义：上方若有"格式校验/纠正...降级"warn → 纠正未生效；
                // 否则为合并 AI 原始输出本身就不含 data-block / 被代码围栏包裹（非本次校验纠正引入）
                log.warn("[互动巡检] 最终待落库正文未通过代码兜底硬查(缺 data-block 或含代码围栏)，"
                        + "若无上方降级 warn 则为合并 AI 原始输出本身不规范 reportId={}", reportId);
            }

            // k. 覆盖写最终报告正文（先删旧再插新）
            bodyRepository.deleteByReportId(reportId);
            ScriptMonitorReportBodyEntity body = ScriptMonitorReportBodyEntity.builder()
                    .reportId(reportId)
                    .content(finalContent)
                    .build();
            bodyRepository.save(body);

            // l. 提取 summary 子对象（effectiveRate + totalUnits + totalBarrages 等）
            buildSummary(report, finalContent);

            // m. 事务写 MySQL（GENERATED + summaryJson + reportBodyId）
            reportWriteService.finishReport(reportId, body.getId(), report.getSummaryJson());

            // n. 实扣 Token
            AiReturnDataVo settleResult = buildSettleResult(finalResult, totalTokens);
            // Token 台账记录用合并模型作为代表（最终输出对应的模型）
            AiTokenUseRecordBo recordBo = buildTokenRecord(report, mergeModelConfig, finalResult);
            aiTokenWithholdFeign.settleAiToken(withhold, report.getUserId(), settleResult, recordBo);

        } catch (Exception e) {
            log.error("[互动巡检] 生成失败 reportId={}", reportId, e);
            // 归还预扣 Token
            try {
                aiTokenWithholdFeign.returnAiToken(withhold);
            } catch (Exception re) {
                log.error("[互动巡检] returnAiToken 失败 reportId={}", reportId, re);
            }
            // 状态补偿：原为 GENERATED → 恢复旧成功报告；否则置 GENERATE_FAILED
            reportWriteService.restoreOrFail(reportId, originalStatus, oldBodyId, oldSummaryJson, e.getMessage());
        }
    }

    // ====== 私有辅助方法 ======

    /**
     * 加载全量 ASR 段落（ASR 为空时返回空列表，由调用方按 PRD 8.1 走 NOT_APPLICABLE）。
     *
     * @param report 报告实体
     * @return ASR 段落列表（可能为空）
     */
    private List<SentenceMarkVo> loadAsrSentences(ScriptMonitorReportEntity report) {
        R<AnalysisResultVo> asrR = sensitiveWordsFeign.getAnalysisData(
                report.getSourceType(), report.getSourceId());
        if (asrR == null || asrR.getData() == null
                || CollUtil.isEmpty(asrR.getData().getSentenceMarkVos())) {
            log.warn("[互动巡检] ASR 数据为空 reportId={}（调用方将置 NOT_APPLICABLE）", report.getId());
            return new ArrayList<>();
        }
        return asrR.getData().getSentenceMarkVos();
    }

    /**
     * 解析行业 id：取直播间维度 tb_anchor_url_user.trade_id（按 secUid+userId+tenantId），
     * 不用录制记录 video.tradeId；取不到回退 1L 通用行业。
     *
     * @param video  视频信息（提供 secUid）
     * @param report 报告实体（提供 userId + tenantId）
     * @return 行业 id
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
     * 按 platformType 取平台名称（用于占位符填充）。
     *
     * @param platformType 平台类型 code（"0"/"1"/"2"/"3"）
     * @return 平台名称（VideoPlatformEnum.remarks）；null 时 PatrolPromptFiller 填默认值
     */
    private String resolvePlatformName(String platformType) {
        if (StrUtil.isBlank(platformType)) {
            return null;
        }
        for (VideoPlatformEnum e : VideoPlatformEnum.values()) {
            if (e.getCode().equals(platformType)) {
                return e.getRemarks();
            }
        }
        return null;
    }

    /**
     * 按 tradeId 取行业名称（用于占位符填充）。
     *
     * @param tradeId 行业 id
     * @return 行业名称（TradeVo.name）；返空或异常时返 null（PatrolPromptFiller 填默认值）
     */
    private String resolveTradeName(Long tradeId) {
        if (tradeId == null) {
            return null;
        }
        try {
            List<TradeVo> trades = tradeFeign.listTradeByIds(List.of(tradeId));
            if (CollUtil.isNotEmpty(trades) && trades.get(0) != null) {
                return trades.get(0).getName();
            }
        } catch (Exception e) {
            log.warn("[互动巡检] 取行业名称异常 tradeId={}", tradeId, e);
        }
        return null;
    }

    /**
     * 按时间窗查询弹幕数据（走 TableStoreFeign SPI，内含 2h Redis 缓存）。
     *
     * <p>TableStore {@code recordDate} 是 epoch ms 绝对时间戳，须以 {@code videoStartEpoch + 视频内相对ms} 换算。
     * 参考：{@code BarrageSentenceImpl#getBarrageByVideoId()} 的 startTime+offset 范式。</p>
     *
     * @param video           视频信息（含 videoId/batchNumber/userId/tenantId）
     * @param unitStartMs     切片窗开始毫秒（视频内相对 ms，从 0 起）
     * @param unitEndMs       切片窗结束毫秒（视频内相对 ms）
     * @param report          报告实体（含 tenantId）
     * @param videoStartEpoch 视频开始时刻的 epoch ms（{@code video.getStartTime().getTime()}，调用方已 null-check）
     * @return 弹幕条目列表（空列表非 null）
     */
    private List<DanMuItemVo> queryDanmuForUnit(AnchorVideoInfoVo video,
                                                long unitStartMs, long unitEndMs,
                                                ScriptMonitorReportEntity report,
                                                long videoStartEpoch) {
        QueryDanMuBo bo = new QueryDanMuBo();
        bo.setVideoId(video.getVideoId());
        bo.setBatchNumber(video.getBatchNumber());
        bo.setUserId(video.getUserId());
        bo.setTenantId(report.getTenantId());
        // Bug-1 fix: recordDate 是绝对 epoch ms，须加视频起始 epoch 换算
        bo.setStartTime(videoStartEpoch + unitStartMs);
        bo.setEndTime(videoStartEpoch + unitEndMs);
        bo.setQueryType(1); // 向下查询
        bo.setLimit(BARRAGE_LIMIT_ALL);
        try {
            List<DanMuItemVo> result = tableStoreFeign.queryDanMuSearchData(bo);
            return result == null ? new ArrayList<>() : result;
        } catch (Exception e) {
            log.warn("[互动巡检] queryDanMuSearchData 异常，返回空列表 videoId={} unitStart={}",
                    video.getVideoId(), unitStartMs, e);
            return new ArrayList<>();
        }
    }

    /**
     * 将弹幕列表格式化为文本（供 AI 分析）。
     *
     * <p>弹幕 {@code recordDate} 是 epoch ms 绝对时间戳；须先减去视频起始 epoch，
     * 再调 {@link #formatMsToHms(long)} 得到视频内相对时间 HH:mm:ss，与 ASR 时间基准保持一致。
     * 参考：{@code BarrageSentenceImpl} line 73-79 的 {@code time1 = recordDate - startTime - 8h} 范式。</p>
     *
     * @param danmuList       弹幕列表
     * @param videoStartEpoch 视频开始时刻的 epoch ms（调用方已 null-check）
     * @return 格式化文本（空时返回 ""）
     */
    private String buildBarrageText(List<DanMuItemVo> danmuList, long videoStartEpoch) {
        if (CollUtil.isEmpty(danmuList)) {
            return "";
        }
        return danmuList.stream()
                .map(d -> {
                    // Bug-2 fix: epoch ms 转视频内相对 ms，再格式化为 HH:mm:ss（与 ASR 基准对齐）
                    long recordEpoch = d.getRecordDate() != null ? d.getRecordDate() : 0L;
                    long relativeMs = recordEpoch - videoStartEpoch;
                    // 用户画像标签：等级 / 粉丝团等级 / 是否新用户，字段为空时跳过，全空则不加括号
                    List<String> tags = new ArrayList<>();
                    if (d.getLevel() != null) {
                        tags.add("等级" + d.getLevel());
                    }
                    if (d.getFansLevelCurrent() != null) {
                        tags.add("粉丝团" + d.getFansLevelCurrent());
                    }
                    if (Boolean.TRUE.equals(d.getIsNew())) {
                        tags.add("新用户");
                    }
                    String tagPart = tags.isEmpty() ? "" : "(" + String.join(",", tags) + ")";
                    return "[" + formatMsToHms(relativeMs) + "] " + d.getNickName() + tagPart + ": " + d.getContent();
                })
                .collect(Collectors.joining("\n"));
    }

    /**
     * ASR 单元切片器：按词级 startTime 归桶，取 [unitStartMs, unitEndMs) 范围的段落。
     *
     * <p>取 sentence.items.get(0).startTime 作为归桶时间；
     * 拼接格式 "[段落 X 时间 HH:mm:ss-HH:mm:ss] content"（与质检 tryLoadAsrText 格式一致）。</p>
     *
     * @param sentences   全量 ASR 段落列表
     * @param unitStartMs 单元开始毫秒
     * @param unitEndMs   单元结束毫秒（已含 +1min 尾延）
     * @return 格式化 ASR 文本（空时返回 ""）
     */
    static String sliceAsrUnits(List<SentenceMarkVo> sentences, long unitStartMs, long unitEndMs) {
        if (CollUtil.isEmpty(sentences)) {
            return "";
        }
        return sentences.stream()
                .filter(s -> {
                    List<WordListItemVo> items = s.getItems();
                    if (CollUtil.isEmpty(items) || items.get(0) == null || items.get(0).getStartTime() == null) {
                        return false;
                    }
                    long t = items.get(0).getStartTime();
                    return t >= unitStartMs && t < unitEndMs;
                })
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
     * 毫秒时间戳格式化为 HH:mm:ss（与质检 formatMsToHms 逻辑一致）。
     *
     * @param ms 毫秒偏移
     * @return HH:mm:ss 格式串
     */
    private static String formatMsToHms(long ms) {
        return DateUtil.format(new DateTime(ms).offset(DateField.HOUR, -8), "HH:mm:ss");
    }

    /**
     * 按行业 + 租户 + cueType 取单条提示词（无配置时抛 BusinessException）。
     *
     * @param cueType    助手类型 code
     * @param promptName 提示词业务名（用于异常文案）
     * @param tradeId    行业 id
     * @param tenantId   租户 id（来自 {@code report.getTenantId()}；走"同行业租户定制赢通用"策略）
     * @return 命中的提示词（含 problem）
     * @throws BusinessException 全链路无配置时抛业务异常（文案含 tradeId/tenantId）
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
     * 按 systemKv key 加载 AI 模型，兜底回退到 {@link #loadAiModel()} 字典 list[0]。
     *
     * <p>兜底场景：</p>
     * <ul>
     *   <li>systemKv 没配置该 key 或 kvValue 为空 → 回退</li>
     *   <li>kvValue 对应的 modelCode 在 tb_ai_model 找不到 → 回退</li>
     * </ul>
     *
     * <p>巡检的 kvKey 固定 2 个（切片 / 合并），所以本 Bll 内直接传字面值而非按 monitorType switch。</p>
     *
     * @param kvKey systemKv key
     * @return AiModelBo
     */
    private AiModelBo loadAiModelByKvKey(String kvKey) {
        SystemKvInfoVo kv = systemKvProducer.getByKey(kvKey);
        if (kv == null || StrUtil.isBlank(kv.getKvValue())) {
            log.warn("[互动巡检] systemKv 未配置 {}, 回退字典 listDiagnosisModel.get(0)", kvKey);
            return loadAiModel();
        }
        String modelCode = kv.getKvValue();
        AiModelInfoVo model = aiModelFeign.getByCode(modelCode);
        if (model == null) {
            log.warn("[互动巡检] systemKv 配置的 modelCode 在 tb_ai_model 找不到, 回退字典 list[0]. kvKey={} code={}",
                    kvKey, modelCode);
            return loadAiModel();
        }
        AiModelBo bo = new AiModelBo();
        BeanUtils.copyProperties(model, bo);
        return bo;
    }

    /**
     * 加载 AI 模型配置（复用质检 loadAiModel 逻辑：字典 listDiagnosisModel.get(0)，作为 systemKv 缺配兜底）。
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
     * 构造 AI 调用消息体（systemPrompt 不入日志，防业务内容泄露）。
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
     * 合并报告 AI 格式校验并纠正（单次调用）：格式规范已内嵌在格式提示词（cueType=28）中，
     * 仅把「待处理输出」喂给一个快模型，由它一次性判定+修复——合规则返回哨兵
     * {@link #FORMAT_OK_SENTINEL}（用合并原文），否则返回纠正后正文（过代码兜底硬查后采用）。
     * 配置缺失 / 任何异常一律降级返回原合并输出（绝不阻断报告）。
     *
     * <p>格式规范维护在 cueType=28 提示词内（运营可改），需与合并提示词 cueType=21 的格式要求保持一致。
     * 本环节的 token 不计入 {@code totalTokens}、不结算。</p>
     *
     * @param mergedContent 合并 AI 原始输出
     * @param tradeId       行业 id（取提示词）
     * @param tenantId      租户 id
     * @param reportId      报告 id（日志 + chatCompletion sourceId）
     * @return 校验/纠正后的最终正文（降级时为入参 mergedContent）
     */
    private String validateAndFixMergeFormat(String mergedContent,
                                             Long tradeId, Long tenantId, Long reportId) {
        if (StrUtil.isBlank(mergedContent)) {
            return mergedContent;
        }
        try {
            AiModelBo formatModel = loadAiModelByKvKey(FORMAT_AI_MODEL_KV_KEY);
            CueWordsInfoVo formatCue = loadSingleCueWord(PATROL_FORMAT_CUE_TYPE,
                    "互动巡检格式校验并纠正提示词", tradeId, tenantId);
            AiReturnDataVo result = aiChatFeign.chatCompletion(formatModel,
                    buildAiMessage(formatCue.getProblem(), buildFormatUserContent(mergedContent)),
                    reportId);
            if (result == null || Objects.equals(result.getStatus(), 1) || StrUtil.isBlank(result.getContent())) {
                log.warn("[互动巡检] 格式校验并纠正无有效返回，降级输出原文 reportId={}", reportId);
                return mergedContent;
            }
            String resp = result.getContent();
            // 精确等值（trim 容忍首尾空白）：合规约定仅返回哨兵本身。用 equals 而非 contains，
            // 防止纠正后正文中偶含哨兵串时被误判为合规、悄悄丢弃纠正结果。
            if (resp.trim().equals(FORMAT_OK_SENTINEL)) {
                log.info("[互动巡检] 格式合规(模型判定)，输出合并原文 reportId={}", reportId);
                return mergedContent;
            }
            // 非哨兵 → 视为纠正后正文；过代码兜底硬查后采用，否则降级
            if (passesParseInvariants(resp)) {
                log.info("[互动巡检] 格式已纠正 reportId={}", reportId);
                return resp;
            }
            log.warn("[互动巡检] 格式纠正结果未通过兜底硬查，降级输出原文 reportId={}", reportId);
            return mergedContent;
        } catch (Exception e) {
            log.warn("[互动巡检] 格式校验/纠正异常，降级输出原文 reportId={}", reportId, e);
            return mergedContent;
        }
    }

    /**
     * 包装格式校验并纠正模型的用户输入（格式规范已内嵌在 cueType=28 系统提示词中）。
     *
     * @param content 待校验/纠正的合并输出
     * @return 用户内容
     */
    private String buildFormatUserContent(String content) {
        return "【待处理输出（按系统提示词中的格式规范判定/修复格式与结构，不改内容）】\n" + content;
    }

    /**
     * 代码兜底硬查：合并报告正文是否满足代码消费方的硬不变量
     * （{@code buildSummary} 依赖 {@code <aifupan-data-block>} 存在；前端解析依赖正文无代码围栏）。
     * 与运营可变的格式审美无关，仅作纠正结果接受判定与降级告警依据。
     *
     * @param content 待校验正文
     * @return true=满足硬不变量
     */
    private boolean passesParseInvariants(String content) {
        if (StrUtil.isBlank(content)) {
            return false;
        }
        return SUMMARY_TAG_PATTERN.matcher(content).find() && !content.contains("```");
    }

    /**
     * 从 AI 合并报告 Markdown 提取 {@code <aifupan-data-block ...>...</aifupan-data-block>}
     * 标签内文本，写入 entity.summaryJson 字段（mirror 质检
     * {@link ScriptMonitorGenerateBll#buildSummary} 范式）。
     *
     * <p>期望输入：AI 合并报告 Markdown 全文，摘要总结段由 {@code <aifupan-data-block>}
     * 标签包裹（弹幕明细大表格在标签外）。匹配规则见 {@link #SUMMARY_TAG_PATTERN}：
     * 标签大小写不敏感、属性可选（含自闭合 {@code />} 形式）、内容允许跨行、多标签取首个，
     * 提取后 {@code .trim()} 去首尾空白。找不到标签 / {@code mergedAiContent} 为 null →
     * {@code setSummaryJson(null)}，不阻塞主流程，仅 {@code log.warn}。空标签返回 {@code ""}
     * 空字符串（与质检 PATCH #13 范式一致，不 fallback null）。</p>
     *
     * <p>字段名沿用 {@code summaryJson} 是历史命名（最初存 JSON 子对象 toJSONString），
     * 现存 Markdown 文本片段；为零迁移成本保留字段名。</p>
     *
     * @param report          报告实体（summaryJson 字段写入此对象）
     * @param mergedAiContent AI 合并报告 Markdown 字符串
     */
    private void buildSummary(ScriptMonitorReportEntity report, String mergedAiContent) {
        if (mergedAiContent == null) {
            log.warn("[互动巡检] 合并报告内容为 null reportId={}", report.getId());
            report.setSummaryJson(null);
            return;
        }
        Matcher matcher = SUMMARY_TAG_PATTERN.matcher(mergedAiContent);
        if (matcher.find()) {
            report.setSummaryJson(matcher.group(1).trim());
            return;
        }
        log.warn("[互动巡检] 合并报告 Markdown 无 <aifupan-data-block> 标签 reportId={}, content 前 200 字={}",
                report.getId(), StrUtil.subPre(mergedAiContent, 200));
        report.setSummaryJson(null);
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
        bo.setAssistantType(ASSISTANT_TYPE_PATROL);
        return bo;
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
     * 任务级超时检查。
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
                    "任务超时(已运行 " + (elapsedMs / 1000) + "s)@" + stage);
        }
    }
}
