package com.jiuyu.replay.ai.bll;

import com.jiuyu.replay.ai.entity.ScriptMonitorReportBodyEntity;
import com.jiuyu.replay.ai.entity.ScriptMonitorReportEntity;
import com.jiuyu.replay.ai.repository.mongo.ScriptMonitorReportBodyRepository;
import com.jiuyu.replay.ai.repository.service.ScriptMonitorReportService;
import com.jiuyu.replay.generic.bo.third.QueryDanMuBo;
import com.jiuyu.replay.generic.feign.order.AiTokenWithholdFeign;
import com.jiuyu.replay.generic.feign.ai.AiChatFeign;
import com.jiuyu.replay.generic.feign.third.AiModelFeign;
import com.jiuyu.replay.generic.feign.third.TableStoreFeign;
import com.jiuyu.replay.generic.feign.words.AnchorUrlUserFeign;
import com.jiuyu.replay.generic.feign.words.AnchorVideoFeign;
import com.jiuyu.replay.generic.bo.words.cue.CueWordsQueryBo;
import com.jiuyu.replay.generic.feign.words.CueWordsFeign;
import com.jiuyu.replay.generic.feign.words.SensitiveWordsFeign;
import com.jiuyu.replay.generic.feign.words.TradeFeign;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.generic.bo.ai.AiMessageBo;
import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.words.AnalysisResultVo;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.third.DanMuItemVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.generic.vo.words.CueWordsInfoVo;
import com.jiuyu.replay.generic.vo.words.SentenceMarkVo;
import com.jiuyu.replay.generic.vo.words.WordListItemVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ScriptMonitorPatrolGenerateBll 单测。
 *
 * <p>覆盖 AC-3 / AC-4 / AC-6 中的核心分支 + 2026-06-08 新增 AC-001~AC-009（buildSummary 反射式 7 边界 + 带属性标签扩展 + AC-009 端到端 ArgumentCaptor 抓 finishReport 第 3 参数）。</p>
 *
 * @author beta
 * @date 2026-06-04
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScriptMonitorPatrolGenerateBllTest {

    @Mock
    private ScriptMonitorReportService reportService;
    @Mock
    private ScriptMonitorReportBodyRepository bodyRepository;
    @Mock
    private AnchorVideoFeign anchorVideoFeign;
    @Mock
    private AnchorUrlUserFeign anchorUrlUserFeign;
    @Mock
    private SensitiveWordsFeign sensitiveWordsFeign;
    @Mock
    private TableStoreFeign tableStoreFeign;
    @Mock
    private CueWordsFeign cueWordsFeign;
    @Mock
    private TradeFeign tradeFeign;
    @Mock
    private AiModelFeign aiModelFeign;
    @Mock
    private AiChatFeign aiFeign;
    @Mock
    private AiTokenWithholdFeign aiTokenWithholdFeign;
    @Spy
    private PatrolPromptFiller patrolPromptFiller;
    @Mock
    private ScriptMonitorReportWriteService reportWriteService;
    /**
     * systemKv 取 AI 模型 code（Mockito 默认 getByKey 返 null 自动走兜底字典 list[0]）
     */
    @Mock
    private com.jiuyu.replay.common.producer.SystemKvProducer systemKvProducer;

    private ScriptMonitorPatrolGenerateBll bll;

    @BeforeEach
    void setUp() {
        // Runnable::run 同步 Executor:supplyAsync 立即在当前线程跑 lambda,测试可控、可断言、无并发干扰
        bll = new ScriptMonitorPatrolGenerateBll(
                reportService, bodyRepository, anchorVideoFeign, anchorUrlUserFeign,
                sensitiveWordsFeign, tableStoreFeign, cueWordsFeign, tradeFeign,
                aiModelFeign, aiFeign, aiTokenWithholdFeign, patrolPromptFiller, reportWriteService,
                systemKvProducer, Runnable::run);
    }

    // ========= 辅助方法 =========

    private ScriptMonitorReportEntity report() {
        ScriptMonitorReportEntity r = new ScriptMonitorReportEntity();
        r.setId(100L);
        r.setTenantId(1L);
        r.setUserId(10L);
        r.setSourceType(0);
        r.setSourceId("v1");
        r.setMonitorType(2);
        r.setStatus(1);
        r.setIsDeleted(0);
        return r;
    }

    private AnchorVideoInfoVo video(Integer existBarrage, String batchNumber, Long duration) {
        return video(existBarrage, batchNumber, duration, null);
    }

    /**
     * 创建带 startTime 的视频对象（epoch ms）。
     *
     * @param existBarrage 弹幕标志
     * @param batchNumber  批次号
     * @param duration     视频时长（秒，对应 DB COMMENT '时长（秒）'）
     * @param startEpochMs 视频开始时刻 epoch ms（null 则不设置，用于测 null 防守）
     * @return AnchorVideoInfoVo
     */
    private AnchorVideoInfoVo video(Integer existBarrage, String batchNumber, Long duration, Long startEpochMs) {
        AnchorVideoInfoVo v = new AnchorVideoInfoVo();
        v.setVideoId("v1");
        v.setTenantId(1L);
        v.setUserId(10L);
        v.setExistBarrage(existBarrage);
        v.setBatchNumber(batchNumber);
        v.setDuration(duration);
        v.setTradeId(1L);
        v.setPlatformType("1");
        if (startEpochMs != null) {
            v.setStartTime(new Date(startEpochMs));
        }
        return v;
    }

    private CueWordsInfoVo cueWord(String problem) {
        CueWordsInfoVo cue = new CueWordsInfoVo();
        cue.setProblem(problem);
        return cue;
    }

    private AiModelInfoVo aiModel() {
        AiModelInfoVo info = new AiModelInfoVo();
        info.setModelCode("test-model");
        info.setModelName("测试模型");
        return info;
    }

    private AiReturnDataVo aiReturn(String content) {
        AiReturnDataVo r = new AiReturnDataVo();
        r.setContent(content);
        r.setTotalTokens(100);
        r.setStatus(0);
        r.setRequestId("req-1");
        return r;
    }

    // ========= AC-3 10min 单元切片 =========

    /**
     * AC-3: 60min 视频 → 6 个 ASR 归桶单元，窗口边界正确。
     */
    @Test
    void sliceAsrUnits_60minVideo_produces6Buckets() {
        // Given: 60 条 ASR 段落，每条在对应分钟的第 1ms
        List<SentenceMarkVo> sentences = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            SentenceMarkVo s = new SentenceMarkVo();
            s.setCurrentSort(i + 1);
            s.setContent("段落内容 " + i);
            WordListItemVo word = new WordListItemVo();
            word.setStartTime((long) i * 60 * 1000);  // 每分钟第 0ms
            word.setEndTime((long) i * 60 * 1000 + 30000);
            s.setItems(List.of(word));
            sentences.add(s);
        }
        long unitMs = 10 * 60 * 1000L; // 10min
        long tailExtend = 60 * 1000L;  // +1min

        // When: 6 个单元，各取其弹幕/ASR 归桶
        List<String> buckets = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            long startMs = (long) i * unitMs;
            long endMs = (long) (i + 1) * unitMs + tailExtend;
            String result = ScriptMonitorPatrolGenerateBll.sliceAsrUnits(sentences, startMs, endMs);
            buckets.add(result);
        }

        // Then: 6 个单元均非空（每单元应有 10+ 条段落）
        assertEquals(6, buckets.size());
        for (int i = 0; i < 6; i++) {
            assertTrue(!buckets.get(i).isEmpty(), "第 " + i + " 单元 ASR 归桶不应为空");
        }
        // 且单元 0 不含第 10min 的段落（startTime=600000ms 落在第 1 个单元的右边界，有尾延所以落在单元 0）
        // 单元 1 从 600000ms 开始，以 600000ms 开头的段落应在单元 1 中
        String unit1 = ScriptMonitorPatrolGenerateBll.sliceAsrUnits(
                sentences, unitMs, 2 * unitMs + tailExtend);
        assertTrue(unit1.contains("段落内容 10"), "第 1 单元应含第 10min 段落");
    }

    /**
     * AC-3a（修复后契约）：consumer 调 anchorVideoFeign.hasBarrage 返 false → NOT_APPLICABLE("本场无弹幕")
     * + returnAiToken 被调用 + 早退（不进 AI 调用）。
     *
     * <p>注意：video.existBarrage 字段值已不再被业务代码读取（GetByVideoId 单视频查询不填充该字段，
     * 始终 null）。判定改走 anchorVideoFeign.hasBarrage SPI 实时查 tb_socket_collect_message。</p>
     */
    @Test
    void generate_hasBarrageFalse_marksNotApplicable() {
        // Given
        ScriptMonitorReportEntity r = report();
        // video.existBarrage 字段值无关紧要（已不被业务代码读）
        AnchorVideoInfoVo v = video(null, "batch-1", 3600L);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(v));
        when(anchorVideoFeign.hasBarrage("v1")).thenReturn(R.ok(false));
        doNothing().when(aiTokenWithholdFeign).returnAiToken(any());
        doNothing().when(reportWriteService).markNotApplicable(anyLong(), any());

        // When
        bll.generate(r, 0, null);

        // Then
        verify(reportWriteService).markNotApplicable(100L, "本场无弹幕");
        verify(aiTokenWithholdFeign).returnAiToken(null);
        // AI 调用不应被触发
        verify(aiFeign, never()).chatCompletion(any(), any(), anyLong());
    }

    /**
     * AC-3b（修复后契约）：hasBarrage SPI 返 null R（远程异常/空响应）→ Boolean.TRUE.equals(null) 为 false →
     * 走 NOT_APPLICABLE 早退分支。防止 SPI 异常被吞而误进 AI 流程。
     */
    @Test
    void generate_hasBarrageNullR_marksNotApplicable() {
        ScriptMonitorReportEntity r = report();
        AnchorVideoInfoVo v = video(null, "batch-1", 3600L);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(v));
        when(anchorVideoFeign.hasBarrage("v1")).thenReturn(R.ok((Boolean) null));
        doNothing().when(aiTokenWithholdFeign).returnAiToken(any());
        doNothing().when(reportWriteService).markNotApplicable(anyLong(), any());

        bll.generate(r, 0, null);

        verify(reportWriteService).markNotApplicable(100L, "本场无弹幕");
        verify(aiFeign, never()).chatCompletion(any(), any(), anyLong());
    }

    /**
     * AC-3c（修复后契约）：有弹幕但 batchNumber 为空 → 同走 NOT_APPLICABLE("本场无弹幕")。
     */
    @Test
    void generate_hasBarrageTrue_butBatchNumberEmpty_marksNotApplicable() {
        ScriptMonitorReportEntity r = report();
        AnchorVideoInfoVo v = video(null, null, 3600L); // batchNumber=null
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(v));
        when(anchorVideoFeign.hasBarrage("v1")).thenReturn(R.ok(true));
        doNothing().when(aiTokenWithholdFeign).returnAiToken(any());
        doNothing().when(reportWriteService).markNotApplicable(anyLong(), any());

        bll.generate(r, 0, null);

        verify(reportWriteService).markNotApplicable(anyLong(), any());
        verify(aiFeign, never()).chatCompletion(any(), any(), anyLong());
    }

    /**
     * PRD 8.1 数据质量约束：ASR 为空 → NOT_APPLICABLE + returnAiToken。
     */
    @Test
    void generate_asrEmpty_marksNotApplicable() {
        ScriptMonitorReportEntity r = report();
        AnchorVideoInfoVo v = video(1, "batch-1", 3600L, 1732449900000L);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(v));
        // 2026-06-09 修复：hasBarrage SPI 取代 video.existBarrage 字段判定
        when(anchorVideoFeign.hasBarrage("v1")).thenReturn(R.ok(true));
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(R.ok(null)); // ASR 空
        doNothing().when(aiTokenWithholdFeign).returnAiToken(any());
        doNothing().when(reportWriteService).markNotApplicable(anyLong(), any());

        bll.generate(r, 0, null);

        verify(reportWriteService).markNotApplicable(100L, "本场 ASR 数据为空，无法生成互动巡检报告");
        verify(aiTokenWithholdFeign).returnAiToken(null);
        // 不应走到弹幕查询 / AI 调用
        verify(tableStoreFeign, never()).queryDanMuSearchData(any());
        verify(aiFeign, never()).chatCompletion(any(), any(), anyLong());
    }

    /**
     * 构造仅含 1 段的 AnalysisResultVo（用于绕过 ASR 空 NOT_APPLICABLE 分支，让流程走到后续逻辑）。
     */
    private AnalysisResultVo buildAsrResult() {
        SentenceMarkVo s = new SentenceMarkVo();
        s.setCurrentSort(1);
        s.setContent("主播话术内容");
        WordListItemVo w = new WordListItemVo();
        w.setStartTime(0L);
        w.setEndTime(5000L);
        s.setItems(List.of(w));
        AnalysisResultVo asr = new AnalysisResultVo();
        asr.setSentenceMarkVos(List.of(s));
        return asr;
    }

    /**
     * AC-6: loadSingleCueWord 全链路无配置 → 抛 BusinessException 文案含 tradeId。
     */
    @Test
    void loadSingleCueWord_noCue_throwsWithTradeId() {
        // Given: 视频正常 + hasBarrage=true + 无提示词（startTime 非 null，使流程能走到 loadSingleCueWord）
        ScriptMonitorReportEntity r = report();
        // 2025-11-24 20:05:00 CST = epoch 1732449900000
        AnchorVideoInfoVo v = video(1, "batch-1", 600L, 1732449900000L);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(v));
        // 2026-06-09 修复：hasBarrage SPI 取代 video.existBarrage 字段判定
        when(anchorVideoFeign.hasBarrage("v1")).thenReturn(R.ok(true));
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(R.ok(buildAsrResult()));
        when(tableStoreFeign.queryDanMuSearchData(any())).thenReturn(List.of());
        // 提示词返 null → 抛异常（签名 2026-06-05 升级为 CueWordsQueryBo，本测试用 any 匹配）
        when(cueWordsFeign.getCueWordByTradeAndType(any(CueWordsQueryBo.class))).thenReturn(null);
        when(aiModelFeign.listDiagnosisModel()).thenReturn(List.of(aiModel()));
        doNothing().when(aiTokenWithholdFeign).returnAiToken(any());
        when(reportWriteService.toString()).thenReturn("");

        // When: generate 内部捕获异常后调 restoreOrFail，不外抛
        bll.generate(r, 0, null);

        // Then: restoreOrFail 被调用（异常被内部 catch 处理）
        verify(reportWriteService).restoreOrFail(anyLong(), anyInt(), any(), any(), any());
    }

    /**
     * Bug-1 + Bug-2 回归：弹幕查询时间窗应为 epoch 绝对时间戳，弹幕 AI 文本时间应为视频内相对时间。
     *
     * <p>验证要点：
     * <ol>
     *   <li>QueryDanMuBo.startTime == videoStartEpoch + 0（第 1 个切片起始）</li>
     *   <li>弹幕 recordDate=videoStartEpoch+50000（视频开始后 50s），AI message 应含 "[00:00:50]"，
     *       而非 UTC 时间（如 "12:05:50"）</li>
     * </ol>
     * </p>
     */
    @Test
    void testGenerate_danmuTimeIsRelativeToVideoStart_notUtc() {
        // Given
        // 视频开始时刻：2025-11-24 20:05:00 CST = epoch 1732449900000
        final long videoStartEpoch = 1732449900000L;
        final long unitWindowMs = 10 * 60 * 1000L; // 10min

        ScriptMonitorReportEntity r = report();
        // 10min 视频 → 1 个切片，startTime 非 null；duration=600L 秒（10min，对应 DB 单位）
        AnchorVideoInfoVo v = video(1, "batch-1", 600L, videoStartEpoch);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(v));
        // 2026-06-09 修复：hasBarrage SPI 取代 video.existBarrage 字段判定
        when(anchorVideoFeign.hasBarrage("v1")).thenReturn(R.ok(true));
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(R.ok(buildAsrResult()));

        // 弹幕：recordDate=videoStartEpoch+50000（视频开始后 50s）
        DanMuItemVo danmu = new DanMuItemVo();
        danmu.setRecordDate(videoStartEpoch + 50_000L);
        danmu.setNickName("用户A");
        danmu.setContent("好棒");

        // ArgumentCaptor 捕获传给 tableStoreFeign 的 QueryDanMuBo
        ArgumentCaptor<QueryDanMuBo> boCaptor = ArgumentCaptor.forClass(QueryDanMuBo.class);
        when(tableStoreFeign.queryDanMuSearchData(boCaptor.capture())).thenReturn(List.of(danmu));

        // 切片提示词和合并提示词
        when(cueWordsFeign.getCueWordByTradeAndType(argThat((CueWordsQueryBo bo) -> bo != null && Integer.valueOf(20).equals(bo.getCueType()))))
                .thenReturn(cueWord("你是互动分析助手，平台：#{platform}，行业：#{trade}"));
        when(cueWordsFeign.getCueWordByTradeAndType(argThat((CueWordsQueryBo bo) -> bo != null && Integer.valueOf(21).equals(bo.getCueType()))))
                .thenReturn(cueWord("请合并以下结果"));
        when(aiModelFeign.listDiagnosisModel()).thenReturn(List.of(aiModel()));
        when(tradeFeign.listTradeByIds(any())).thenReturn(List.of());

        // AI 调用返回合法 Markdown（含 <aifupan-data-block> 标签 — 2026-06-07 起与质检统一）
        String aiResponseMarkdown = "# 巡检报告\n\n| 时间 | 用户 | 内容 |\n| --- | --- | --- |\n| 00:00:50 | 用户A | 好棒 |\n\n<aifupan-data-block>本场互动有效回复率 80.0%，总单元数 1，需回复弹幕 1 条，有效回复 1 条，无效回复 0 条。</aifupan-data-block>";
        when(aiFeign.chatCompletion(any(), any(), anyLong()))
                .thenReturn(aiReturn(aiResponseMarkdown));
        doNothing().when(bodyRepository).deleteByReportId(anyLong());
        when(bodyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(reportWriteService).finishReport(anyLong(), any(), any());
        doNothing().when(aiTokenWithholdFeign).settleAiToken(any(), anyLong(), any(), any());

        // When
        bll.generate(r, 0, null);

        // Then 1: QueryDanMuBo.startTime 应为 videoStartEpoch + 0（绝对 epoch，非相对 0）
        QueryDanMuBo captured = boCaptor.getValue();
        assertNotNull(captured, "QueryDanMuBo 应被捕获");
        assertEquals(videoStartEpoch, captured.getStartTime(),
                "Bug-1: startTime 应为 videoStartEpoch+unitStartMs=videoStartEpoch+0，不能是相对 0");
        assertEquals(videoStartEpoch + unitWindowMs, captured.getEndTime(),
                "Bug-1: endTime 应为 videoStartEpoch+unitWindowMs");

        // Then 2: AI 调用的 user content 应含 "[00:00:50]"（视频内相对时间），不含 UTC 小时
        ArgumentCaptor<com.jiuyu.replay.generic.bo.ai.AiMessageBo> msgCaptor =
                ArgumentCaptor.forClass(com.jiuyu.replay.generic.bo.ai.AiMessageBo.class);
        verify(aiFeign, atLeastOnce()).chatCompletion(any(), msgCaptor.capture(), anyLong());
        // 取第一次调用（切片调用）的 user content
        com.jiuyu.replay.generic.bo.ai.AiMessageBo firstMsg = msgCaptor.getAllValues().get(0);
        assertNotNull(firstMsg.getUser(), "user content 不应为空");
        String userContent = firstMsg.getUser().get(0).get("text").toString();
        assertTrue(userContent.contains("[00:00:50]"),
                "Bug-2: 弹幕时间应为视频内相对时间 [00:00:50]，实际 user content=" + userContent);
    }

    /**
     * 方案 B+：systemKv 配置切片 + 合并不同模型 → 验证 chatCompletion 实际用了对应模型
     */
    @Test
    void testGenerate_systemKvConfigured_usesSliceAndMergeModelsSeparately() {
        // 视频开始 + 1 个单元（10min 视频，1 次切片 + 1 次合并 = 2 次 chatCompletion）
        final long videoStartEpoch = 1732449900000L;
        final long unitWindowMs = 10 * 60 * 1000L;

        ScriptMonitorReportEntity r = report();
        // duration=600L 秒（10min，对应 DB 单位）
        AnchorVideoInfoVo v = video(1, "batch-1", 600L, videoStartEpoch);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(v));
        // 2026-06-09 修复：hasBarrage SPI 取代 video.existBarrage 字段判定
        when(anchorVideoFeign.hasBarrage("v1")).thenReturn(R.ok(true));
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(R.ok(buildAsrResult()));
        when(tableStoreFeign.queryDanMuSearchData(any())).thenReturn(List.of());
        when(cueWordsFeign.getCueWordByTradeAndType(argThat((CueWordsQueryBo bo) -> bo != null && Integer.valueOf(20).equals(bo.getCueType()))))
                .thenReturn(cueWord("切片提示词"));
        when(cueWordsFeign.getCueWordByTradeAndType(argThat((CueWordsQueryBo bo) -> bo != null && Integer.valueOf(21).equals(bo.getCueType()))))
                .thenReturn(cueWord("合并提示词"));
        when(tradeFeign.listTradeByIds(any())).thenReturn(List.of());

        // systemKv 配置 slice + merge 不同模型
        com.jiuyu.replay.common.vo.SystemKvInfoVo sliceKv = new com.jiuyu.replay.common.vo.SystemKvInfoVo();
        sliceKv.setKvValue("slice-model-code");
        com.jiuyu.replay.common.vo.SystemKvInfoVo mergeKv = new com.jiuyu.replay.common.vo.SystemKvInfoVo();
        mergeKv.setKvValue("merge-model-code");
        when(systemKvProducer.getByKey("script_monitor_patrol_inter_ai_model")).thenReturn(sliceKv);
        when(systemKvProducer.getByKey("script_monitor_patrol_merge_ai_model")).thenReturn(mergeKv);

        com.jiuyu.replay.generic.vo.ai.AiModelInfoVo sliceModel = new com.jiuyu.replay.generic.vo.ai.AiModelInfoVo();
        sliceModel.setModelCode("slice-model-code");
        sliceModel.setModelName("巡检切片模型");
        com.jiuyu.replay.generic.vo.ai.AiModelInfoVo mergeModel = new com.jiuyu.replay.generic.vo.ai.AiModelInfoVo();
        mergeModel.setModelCode("merge-model-code");
        mergeModel.setModelName("巡检合并模型");
        when(aiModelFeign.getByCode("slice-model-code")).thenReturn(sliceModel);
        when(aiModelFeign.getByCode("merge-model-code")).thenReturn(mergeModel);

        when(aiFeign.chatCompletion(any(), any(), anyLong()))
                .thenReturn(aiReturn("# 巡检报告\n\n| 时间 | 用户 | 内容 |\n| --- | --- | --- |\n| 00:00:50 | 用户A | 好棒 |\n\n<aifupan-data-block>本场互动有效回复率 80.0%，总单元数 1，需回复弹幕 1 条，有效回复 1 条，无效回复 0 条。</aifupan-data-block>"));
        doNothing().when(bodyRepository).deleteByReportId(anyLong());
        when(bodyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(reportWriteService).finishReport(anyLong(), any(), any());
        doNothing().when(aiTokenWithholdFeign).settleAiToken(any(), anyLong(), any(), any());

        bll.generate(r, 0, null);

        // 验证 chatCompletion 被调用 2 次（1 切片 + 1 合并），且 modelConfig 不同。
        // 注：本 case 未配 cueType=28 格式提示词 → 格式校验/纠正在 loadSingleCueWord 抛错被捕获后降级、不发 AI 调用，
        // 故仍是 2 次（格式校验流程的正向覆盖见 generate_format* 系列）。
        ArgumentCaptor<com.jiuyu.replay.generic.bo.ai.AiModelBo> modelCaptor =
                ArgumentCaptor.forClass(com.jiuyu.replay.generic.bo.ai.AiModelBo.class);
        verify(aiFeign, times(2)).chatCompletion(modelCaptor.capture(), any(), anyLong());
        List<com.jiuyu.replay.generic.bo.ai.AiModelBo> models = modelCaptor.getAllValues();
        assertEquals("slice-model-code", models.get(0).getModelCode(), "第 1 次（切片）应用 slice 模型");
        assertEquals("merge-model-code", models.get(1).getModelCode(), "第 2 次（合并）应用 merge 模型");
    }

    // ===========================================================================
    // buildSummary 边界测试（AC-001 ~ AC-007）— 反射直测 private buildSummary
    // 范式 mirror 质检 ScriptMonitorGenerateBll PATCH #13（2026-06-05 e4da8ae03）
    // ===========================================================================

    /**
     * 反射调用 private buildSummary 方法（直接覆盖核心提取逻辑，不依赖 generate 入口数据）。
     */
    private void invokeBuildSummary(ScriptMonitorReportEntity report, String content) throws Exception {
        java.lang.reflect.Method m = ScriptMonitorPatrolGenerateBll.class
                .getDeclaredMethod("buildSummary", ScriptMonitorReportEntity.class, String.class);
        m.setAccessible(true);
        m.invoke(bll, report, content);
    }

    @Test
    @DisplayName("AC-001 buildSummary happy path — 提取 <aifupan-data-block> 标签内 trim 文本")
    void buildSummary_extractTrimmedContent_writeSummaryJson() throws Exception {
        ScriptMonitorReportEntity r = report();
        invokeBuildSummary(r, "前导段落\n<aifupan-data-block>  摘要文本  </aifupan-data-block>\n后导段落");
        assertEquals("摘要文本", r.getSummaryJson(), "应提取标签内文本并 trim 首尾空白");
    }

    @Test
    @DisplayName("AC-002 buildSummary 无标签 → summaryJson=null + log.warn，不抛异常")
    void buildSummary_noTag_setNull() throws Exception {
        ScriptMonitorReportEntity r = report();
        r.setSummaryJson("脏数据");  // 验证 finishReport 之前 entity 内存值被覆盖为 null
        invokeBuildSummary(r, "# 报告\n## 详情\n这段 markdown 不含 aifupan-data-block 标签");
        assertNull(r.getSummaryJson(), "无 <aifupan-data-block> 标签 → summaryJson 应为 null");
    }

    @Test
    @DisplayName("AC-003 buildSummary null 内容 → summaryJson=null，不抛 NPE")
    void buildSummary_nullContent_setNull_noNPE() throws Exception {
        ScriptMonitorReportEntity r = report();
        invokeBuildSummary(r, null);
        assertNull(r.getSummaryJson(), "null 内容 → summaryJson 应为 null，方法正常返回不抛 NPE");
    }

    @Test
    @DisplayName("AC-004 buildSummary 大小写不敏感（CASE_INSENSITIVE flag）")
    void buildSummary_caseInsensitive_extract() throws Exception {
        ScriptMonitorReportEntity r = report();
        invokeBuildSummary(r, "<AIFUPAN-DATA-BLOCK>大写内容</AIFUPAN-DATA-BLOCK>");
        assertEquals("大写内容", r.getSummaryJson(), "全大写标签应被 CASE_INSENSITIVE 命中");
    }

    @Test
    @DisplayName("AC-005 buildSummary 跨行内容（DOTALL flag）")
    void buildSummary_multiLineContent_extract() throws Exception {
        ScriptMonitorReportEntity r = report();
        invokeBuildSummary(r, "<aifupan-data-block>\n第一行\n第二行\n第三行\n</aifupan-data-block>");
        assertEquals("第一行\n第二行\n第三行", r.getSummaryJson(),
                "DOTALL 允许 . 匹配换行；trim 去首尾空白后保留中间换行");
    }

    @Test
    @DisplayName("AC-006 buildSummary 多标签取首个（非贪婪 .*?）")
    void buildSummary_multipleTags_firstOnly() throws Exception {
        ScriptMonitorReportEntity r = report();
        invokeBuildSummary(r,
                "<aifupan-data-block>第一个</aifupan-data-block>\n中间段\n<aifupan-data-block>第二个</aifupan-data-block>");
        assertEquals("第一个", r.getSummaryJson(), ".*? 非贪婪应匹配首个标签");
    }

    @Test
    @DisplayName("AC-007 buildSummary 空标签 → 空字符串 \"\"（不 fallback null，与质检 PATCH #13 一致）")
    void buildSummary_emptyTag_emptyString() throws Exception {
        ScriptMonitorReportEntity r = report();
        invokeBuildSummary(r, "<aifupan-data-block></aifupan-data-block>");
        assertEquals("", r.getSummaryJson(),
                "空标签 trim 后返回 \"\"，mirror 质检 PATCH #13 范式（openspec §5.4 决策）");
    }

    @Test
    @DisplayName("AC-001 — buildSummary 带属性的标签也能命中（[^>]* 属性段）")
    void buildSummary_tagWithAttributes_extract() throws Exception {
        ScriptMonitorReportEntity r = report();
        invokeBuildSummary(r,
                "<aifupan-data-block resource-data visible=\"false\">带属性的内容</aifupan-data-block>");
        assertEquals("带属性的内容", r.getSummaryJson(), "[^>]* 应匹配标签属性段");
    }

    /**
     * AC-009 端到端 — generate 跑完后 finishReport 第 3 参数（summaryJson）应是
     * &lt;aifupan-data-block&gt; 标签内 trim 后文本，**不再**是 JSON 字符串（证伪反向假设）。
     *
     * <p>mirror 既有 {@code testGenerate_systemKvConfigured_usesSliceAndMergeModelsSeparately} 的 mock 链路，
     * 区别仅在加 ArgumentCaptor 抓 finishReport 第 3 参数做精准断言。</p>
     */
    @Test
    @DisplayName("AC-009 generate 端到端 — finishReport 第3参数 == 标签内摘要文本（非 JSON）")
    void generate_finishReportReceivesMarkdownSummary_notJson() {
        final long videoStartEpoch = 1732449900000L;
        final long unitWindowMs = 10 * 60 * 1000L;

        ScriptMonitorReportEntity r = report();
        // duration=600L 秒（10min，对应 DB 单位）
        AnchorVideoInfoVo v = video(1, "batch-1", 600L, videoStartEpoch);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(v));
        // 2026-06-09 修复：hasBarrage SPI 取代 video.existBarrage 字段判定
        when(anchorVideoFeign.hasBarrage("v1")).thenReturn(R.ok(true));
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(R.ok(buildAsrResult()));
        when(tableStoreFeign.queryDanMuSearchData(any())).thenReturn(List.of());
        when(cueWordsFeign.getCueWordByTradeAndType(argThat((CueWordsQueryBo bo) -> bo != null && Integer.valueOf(20).equals(bo.getCueType()))))
                .thenReturn(cueWord("切片提示词"));
        when(cueWordsFeign.getCueWordByTradeAndType(argThat((CueWordsQueryBo bo) -> bo != null && Integer.valueOf(21).equals(bo.getCueType()))))
                .thenReturn(cueWord("合并提示词"));
        when(aiModelFeign.listDiagnosisModel()).thenReturn(List.of(aiModel()));
        when(tradeFeign.listTradeByIds(any())).thenReturn(List.of());
        // AI 返 Markdown 含 <aifupan-data-block> 标签（mirror 质检 PATCH #13）
        when(aiFeign.chatCompletion(any(), any(), anyLong()))
                .thenReturn(aiReturn("# 巡检报告\n\n| 时间 | 用户 | 内容 |\n| --- | --- | --- |\n| 00:00:50 | 用户A | 好棒 |\n\n<aifupan-data-block>本场互动有效回复率 80.0%，总单元数 1，需回复弹幕 1 条，有效回复 1 条，无效回复 0 条。</aifupan-data-block>"));
        doNothing().when(bodyRepository).deleteByReportId(anyLong());
        when(bodyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(reportWriteService).finishReport(anyLong(), any(), any());
        doNothing().when(aiTokenWithholdFeign).settleAiToken(any(), anyLong(), any(), any());

        bll.generate(r, 0, null);

        // 关键断言：finishReport 第 3 参数 == 标签内 trim 后文本（非 JSON）
        ArgumentCaptor<String> summaryCaptor = ArgumentCaptor.forClass(String.class);
        verify(reportWriteService).finishReport(anyLong(), any(), summaryCaptor.capture());
        String captured = summaryCaptor.getValue();
        assertEquals(
                "本场互动有效回复率 80.0%，总单元数 1，需回复弹幕 1 条，有效回复 1 条，无效回复 0 条。",
                captured,
                "AC-009: finishReport 第 3 参数应为 <aifupan-data-block> 标签内 trim 后文本");
        assertNotNull(captured, "AC-009: summaryJson 不应为 null");
        assertTrue(!captured.startsWith("{"),
                "AC-009: summaryJson 不应是 JSON 对象字符串（{...} 起首），已改为 Markdown 文本");
    }

    /**
     * 构建覆盖 3 个 10min 窗口的 ASR 数据（每窗各 1 段），供 30min 切片 case 使用，
     * 使 3 个切片的 userContent 可区分。
     *
     * @return AnalysisResultVo，含 3 段 ASR，startTime 分别在 0s / 11min / 21min
     */
    private AnalysisResultVo buildAsrResult3Windows() {
        List<SentenceMarkVo> sentences = new ArrayList<>();
        // 切片 0 窗口：0 ~ 600000ms
        SentenceMarkVo s0 = new SentenceMarkVo();
        s0.setCurrentSort(1);
        s0.setContent("切片0的主播话术内容");
        WordListItemVo w0 = new WordListItemVo();
        w0.setStartTime(0L);
        w0.setEndTime(5000L);
        s0.setItems(List.of(w0));
        sentences.add(s0);
        // 切片 1 窗口：600000ms ~ 1200000ms（11min = 660000ms 落在此窗）
        SentenceMarkVo s1 = new SentenceMarkVo();
        s1.setCurrentSort(2);
        s1.setContent("切片1的主播话术内容");
        WordListItemVo w1 = new WordListItemVo();
        w1.setStartTime(660_000L); // 11min
        w1.setEndTime(665_000L);
        s1.setItems(List.of(w1));
        sentences.add(s1);
        // 切片 2 窗口：1200000ms ~ 1800000ms（21min = 1260000ms 落在此窗）
        SentenceMarkVo s2 = new SentenceMarkVo();
        s2.setCurrentSort(3);
        s2.setContent("切片2的主播话术内容");
        WordListItemVo w2 = new WordListItemVo();
        w2.setStartTime(1_260_000L); // 21min
        w2.setEndTime(1_265_000L);
        s2.setItems(List.of(w2));
        sentences.add(s2);
        AnalysisResultVo asr = new AnalysisResultVo();
        asr.setSentenceMarkVos(sentences);
        return asr;
    }

    /**
     * AC-5 (PATCH fix-patrol-duration-unit-mismatch): 30min 视频 duration=1800sec 应切 3 片 + 1 合并，
     * aiFeign.chatCompletion 共调用 4 次（3 切片 + 1 合并）。
     *
     * <p>修复前 unitCount 永远为 1（duration 单位错配），此 case 是修复后的回归锚。</p>
     */
    @Test
    @DisplayName("互动巡检 30min 视频 duration=1800sec 走 3 切片 + 1 合并 (修 PATCH#fix-patrol-duration-unit-mismatch 单位错配)")
    void generate_durationLongerThanUnitWindow_splitsIntoMultipleUnits() {
        // given: duration=1800L 秒（30min），视频开始时刻
        final long videoStartEpoch = 1732449900000L;
        ScriptMonitorReportEntity r = report();
        AnchorVideoInfoVo v = video(1, "batch-1", 1800L, videoStartEpoch);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(v));
        when(anchorVideoFeign.hasBarrage("v1")).thenReturn(R.ok(true));
        // 使用覆盖 3 个切片窗口的 ASR 数据，确保 3 个切片 userContent 各不相同
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(R.ok(buildAsrResult3Windows()));
        when(tableStoreFeign.queryDanMuSearchData(any())).thenReturn(List.of());
        when(cueWordsFeign.getCueWordByTradeAndType(argThat((CueWordsQueryBo bo) -> bo != null && Integer.valueOf(20).equals(bo.getCueType()))))
                .thenReturn(cueWord("切片提示词"));
        when(cueWordsFeign.getCueWordByTradeAndType(argThat((CueWordsQueryBo bo) -> bo != null && Integer.valueOf(21).equals(bo.getCueType()))))
                .thenReturn(cueWord("合并提示词"));
        when(aiModelFeign.listDiagnosisModel()).thenReturn(List.of(aiModel()));
        when(tradeFeign.listTradeByIds(any())).thenReturn(List.of());
        // mock aiFeign 返回 4 次合规（3 切片 + 1 合并），各切片返回唯一内容便于 ArgumentCaptor 验证
        String sliceResponse0 = "slice0 内容";
        String sliceResponse1 = "slice1 内容";
        String sliceResponse2 = "slice2 内容";
        String mergeResponse = "# 巡检报告\n\n<aifupan-data-block>有效回复率 80.0%</aifupan-data-block>";
        when(aiFeign.chatCompletion(any(), any(), anyLong()))
                .thenReturn(aiReturn(sliceResponse0))
                .thenReturn(aiReturn(sliceResponse1))
                .thenReturn(aiReturn(sliceResponse2))
                .thenReturn(aiReturn(mergeResponse));
        doNothing().when(bodyRepository).deleteByReportId(anyLong());
        when(bodyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(reportWriteService).finishReport(anyLong(), any(), any());
        doNothing().when(aiTokenWithholdFeign).settleAiToken(any(), anyLong(), any(), any());

        // when
        bll.generate(r, 0, null);

        // then: aiFeign.chatCompletion 调用 4 次（3 切片 + 1 合并）
        ArgumentCaptor<com.jiuyu.replay.generic.bo.ai.AiMessageBo> captor =
                ArgumentCaptor.forClass(com.jiuyu.replay.generic.bo.ai.AiMessageBo.class);
        verify(aiFeign, times(4)).chatCompletion(any(), captor.capture(), anyLong());
        // 取前 3 次（切片调用），验证 userContent 各不相同（每个切片有独立时间窗数据）
        List<com.jiuyu.replay.generic.bo.ai.AiMessageBo> calls = captor.getAllValues();
        assertEquals(4, calls.size(), "应捕获 4 次 chatCompletion 调用");
        // 前 3 次的 userContent 各自不同（3 个独立切片）
        // 用 Objects.toString 避免 user.get(0) / map value 为 null 时 NPE 掩盖断言意图
        String content0 = Objects.toString(calls.get(0).getUser().get(0).get("text"), "");
        String content1 = Objects.toString(calls.get(1).getUser().get(0).get("text"), "");
        String content2 = Objects.toString(calls.get(2).getUser().get(0).get("text"), "");
        assertNotEquals(content0, content1, "切片 0 与切片 1 的 userContent 应不同");
        assertNotEquals(content1, content2, "切片 1 与切片 2 的 userContent 应不同");
    }

    /**
     * AC-fix-2: duration=null fallback — unitCount=1，不抛 NPE，正常走 1 切片 + 1 合并。
     *
     * <p>修复后的 null 防守分支是新加代码，单独验证可以防 "null fallback 误判" mutate bug。</p>
     */
    @Test
    @DisplayName("互动巡检 video.duration=null → fallback 1 单元 + warn 日志")
    void generate_durationNull_fallbackToOneUnit() {
        final long videoStartEpoch = 1732449900000L;
        ScriptMonitorReportEntity r = report();
        // duration=null 模拟 DB 无时长数据
        AnchorVideoInfoVo v = video(1, "batch-1", null, videoStartEpoch);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(v));
        when(anchorVideoFeign.hasBarrage("v1")).thenReturn(R.ok(true));
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(R.ok(buildAsrResult()));
        when(tableStoreFeign.queryDanMuSearchData(any())).thenReturn(List.of());
        when(cueWordsFeign.getCueWordByTradeAndType(argThat((CueWordsQueryBo bo) -> bo != null && Integer.valueOf(20).equals(bo.getCueType()))))
                .thenReturn(cueWord("切片提示词"));
        when(cueWordsFeign.getCueWordByTradeAndType(argThat((CueWordsQueryBo bo) -> bo != null && Integer.valueOf(21).equals(bo.getCueType()))))
                .thenReturn(cueWord("合并提示词"));
        when(aiModelFeign.listDiagnosisModel()).thenReturn(List.of(aiModel()));
        when(tradeFeign.listTradeByIds(any())).thenReturn(List.of());
        String aiResponse = "# 巡检报告\n\n<aifupan-data-block>有效回复率 80.0%</aifupan-data-block>";
        when(aiFeign.chatCompletion(any(), any(), anyLong())).thenReturn(aiReturn(aiResponse));
        doNothing().when(bodyRepository).deleteByReportId(anyLong());
        when(bodyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(reportWriteService).finishReport(anyLong(), any(), any());
        doNothing().when(aiTokenWithholdFeign).settleAiToken(any(), anyLong(), any(), any());

        // when
        bll.generate(r, 0, null);

        // then: chatCompletion 调用 2 次（1 切片 + 1 合并），不抛异常
        verify(aiFeign, times(2)).chatCompletion(any(), any(), anyLong());
    }

    /**
     * AC-fix-3: duration=601sec 刚跨 10min 边界 — unitCount=2（ceil(601000/600000)=2）。
     *
     * <p>duration=600L 巧合整除时 ceil 与 floor 结果相同（都=1），无法捕获 ceil→floor 变异。
     * 本 case 用 601L，ceil=2 而 floor=1，能精确区分。</p>
     */
    @Test
    @DisplayName("互动巡检 duration=601sec 刚跨 10min → 2 单元（防 ceil→floor mutate）")
    void generate_durationJustOver10min_splitsIntoTwoUnits() {
        final long videoStartEpoch = 1732449900000L;
        ScriptMonitorReportEntity r = report();
        // 601L 秒 = 10min1sec → ceil(601000/600000) = 2
        AnchorVideoInfoVo v = video(1, "batch-1", 601L, videoStartEpoch);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(v));
        when(anchorVideoFeign.hasBarrage("v1")).thenReturn(R.ok(true));
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(R.ok(buildAsrResult()));
        when(tableStoreFeign.queryDanMuSearchData(any())).thenReturn(List.of());
        when(cueWordsFeign.getCueWordByTradeAndType(argThat((CueWordsQueryBo bo) -> bo != null && Integer.valueOf(20).equals(bo.getCueType()))))
                .thenReturn(cueWord("切片提示词"));
        when(cueWordsFeign.getCueWordByTradeAndType(argThat((CueWordsQueryBo bo) -> bo != null && Integer.valueOf(21).equals(bo.getCueType()))))
                .thenReturn(cueWord("合并提示词"));
        when(aiModelFeign.listDiagnosisModel()).thenReturn(List.of(aiModel()));
        when(tradeFeign.listTradeByIds(any())).thenReturn(List.of());
        String aiResponse = "# 巡检报告\n\n<aifupan-data-block>有效回复率 80.0%</aifupan-data-block>";
        when(aiFeign.chatCompletion(any(), any(), anyLong())).thenReturn(aiReturn(aiResponse));
        doNothing().when(bodyRepository).deleteByReportId(anyLong());
        when(bodyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(reportWriteService).finishReport(anyLong(), any(), any());
        doNothing().when(aiTokenWithholdFeign).settleAiToken(any(), anyLong(), any(), any());

        // when
        bll.generate(r, 0, null);

        // then: chatCompletion 调用 3 次（2 切片 + 1 合并）
        // ceil(601000ms / 600000ms) = 2；若 ceil 被错改 floor → 算成 1，本 case 会抓到
        verify(aiFeign, times(3)).chatCompletion(any(), any(), anyLong());
    }

    // ===========================================================================
    // 合并报告 AI 格式校验并纠正测试（AC-001 ~ AC-006，单次调用方案）
    // 用 system 提示词文本标记区分 chatCompletion 调用：切片 / 合并 / 格式(FORMAT_PROMPT)
    // 格式调用约定：返回含 __FORMAT_OK__ = 合规用原文；否则返回值为纠正后正文
    // ===========================================================================

    /** 判断 AiMessageBo 的 system 提示词是否含指定标记，用于区分 chatCompletion 调用类型。 */
    private boolean sysIs(AiMessageBo params, String marker) {
        if (params == null || params.getSystem() == null || params.getSystem().isEmpty()) {
            return false;
        }
        Object text = params.getSystem().get(0).get("text");
        return text != null && String.valueOf(text).contains(marker);
    }

    /** 构造带自定义 token 数的 AI 返回。 */
    private AiReturnDataVo aiReturnTok(String content, int totalTokens) {
        AiReturnDataVo r = new AiReturnDataVo();
        r.setContent(content);
        r.setTotalTokens(totalTokens);
        r.setStatus(0);
        return r;
    }

    /**
     * 配齐到达合并步骤所需 mock（含 cueType 28 格式提示词 + 切片 chatCompletion 兜底）；
     * 合并 / 格式两类 chatCompletion 由各 test 自行 stub。
     *
     * @return 巡检报告实体（300s 视频 → 1 单元）
     */
    private ScriptMonitorReportEntity setupFormatFlow() {
        final long videoStartEpoch = 1732449900000L;
        ScriptMonitorReportEntity r = report();
        AnchorVideoInfoVo v = video(1, "batch-1", 300L, videoStartEpoch);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(v));
        when(anchorVideoFeign.hasBarrage("v1")).thenReturn(R.ok(true));
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(R.ok(buildAsrResult()));
        when(tableStoreFeign.queryDanMuSearchData(any())).thenReturn(List.of());
        when(cueWordsFeign.getCueWordByTradeAndType(argThat((CueWordsQueryBo bo) -> bo != null && Integer.valueOf(20).equals(bo.getCueType()))))
                .thenReturn(cueWord("切片提示词"));
        when(cueWordsFeign.getCueWordByTradeAndType(argThat((CueWordsQueryBo bo) -> bo != null && Integer.valueOf(21).equals(bo.getCueType()))))
                .thenReturn(cueWord("合并提示词MERGE"));
        when(cueWordsFeign.getCueWordByTradeAndType(argThat((CueWordsQueryBo bo) -> bo != null && Integer.valueOf(28).equals(bo.getCueType()))))
                .thenReturn(cueWord("FORMAT_PROMPT"));
        when(aiModelFeign.listDiagnosisModel()).thenReturn(List.of(aiModel()));
        when(tradeFeign.listTradeByIds(any())).thenReturn(List.of());
        // 切片 chatCompletion 兜底（合并/格式 各 test 自行 stub）
        when(aiFeign.chatCompletion(any(), argThat((AiMessageBo p) -> sysIs(p, "切片提示词")), anyLong()))
                .thenReturn(aiReturn("切片结果"));
        doNothing().when(bodyRepository).deleteByReportId(anyLong());
        when(bodyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(reportWriteService).finishReport(anyLong(), any(), any());
        doNothing().when(aiTokenWithholdFeign).settleAiToken(any(), anyLong(), any(), any());
        return r;
    }

    private String captureSavedContent() {
        ArgumentCaptor<ScriptMonitorReportBodyEntity> cap = ArgumentCaptor.forClass(ScriptMonitorReportBodyEntity.class);
        verify(bodyRepository).save(cap.capture());
        return cap.getValue().getContent();
    }

    @Test
    @DisplayName("格式合规(返回 __FORMAT_OK__) → 落库为合并原文")
    void generate_formatConform_sentinelKeepsOriginal() {
        ScriptMonitorReportEntity r = setupFormatFlow();
        String mergeContent = "# 巡检报告\n<aifupan-data-block>回复率 80.0%</aifupan-data-block>";
        when(aiFeign.chatCompletion(any(), argThat((AiMessageBo p) -> sysIs(p, "合并提示词MERGE")), anyLong()))
                .thenReturn(aiReturn(mergeContent));
        when(aiFeign.chatCompletion(any(), argThat((AiMessageBo p) -> sysIs(p, "FORMAT_PROMPT")), anyLong()))
                .thenReturn(aiReturn("__FORMAT_OK__"));

        bll.generate(r, 0, null);

        assertEquals(mergeContent, captureSavedContent());
    }

    @Test
    @DisplayName("格式返回含哨兵子串的完整正文 → 按纠正文处理(不误判为合规)")
    void generate_responseContainsSentinelSubstring_treatedAsFixed() {
        ScriptMonitorReportEntity r = setupFormatFlow();
        String mergeContent = "原始合并，缺标签";
        // 纠正后正文正文里恰好含哨兵子串，但不是"仅有哨兵" → 应按纠正文采用，而非误判合规丢弃
        // （回归 MAJOR：若用 contains 判定会错误返回 mergeContent）
        String fixedWithSentinel = "已修复(注:__FORMAT_OK__ 仅为占位)\n<aifupan-data-block>回复率 80.0%</aifupan-data-block>";
        when(aiFeign.chatCompletion(any(), argThat((AiMessageBo p) -> sysIs(p, "合并提示词MERGE")), anyLong()))
                .thenReturn(aiReturn(mergeContent));
        when(aiFeign.chatCompletion(any(), argThat((AiMessageBo p) -> sysIs(p, "FORMAT_PROMPT")), anyLong()))
                .thenReturn(aiReturn(fixedWithSentinel));

        bll.generate(r, 0, null);

        assertEquals(fixedWithSentinel, captureSavedContent());
    }

    @Test
    @DisplayName("格式不合规 → 返回纠正后正文且过兜底 → 落库为纠正后内容")
    void generate_formatNonConform_savesFixedContent() {
        ScriptMonitorReportEntity r = setupFormatFlow();
        String mergeContent = "原始合并待修，缺标签";
        String fixedContent = "已修复合并\n<aifupan-data-block>回复率 80.0%</aifupan-data-block>";
        when(aiFeign.chatCompletion(any(), argThat((AiMessageBo p) -> sysIs(p, "合并提示词MERGE")), anyLong()))
                .thenReturn(aiReturn(mergeContent));
        when(aiFeign.chatCompletion(any(), argThat((AiMessageBo p) -> sysIs(p, "FORMAT_PROMPT")), anyLong()))
                .thenReturn(aiReturn(fixedContent));

        bll.generate(r, 0, null);

        assertEquals(fixedContent, captureSavedContent());
    }

    @Test
    @DisplayName("纠正结果未过兜底硬查(缺 data-block) → 降级落库合并原文")
    void generate_fixStillInvalid_degradesToOriginal() {
        ScriptMonitorReportEntity r = setupFormatFlow();
        String mergeContent = "原始合并\n<aifupan-data-block>回复率 1.0%</aifupan-data-block>";
        // 格式调用返回的纠正结果缺 data-block → passesParseInvariants=false → 不接受
        String fixBad = "修了还是坏的，没有数据块标签";
        when(aiFeign.chatCompletion(any(), argThat((AiMessageBo p) -> sysIs(p, "合并提示词MERGE")), anyLong()))
                .thenReturn(aiReturn(mergeContent));
        when(aiFeign.chatCompletion(any(), argThat((AiMessageBo p) -> sysIs(p, "FORMAT_PROMPT")), anyLong()))
                .thenReturn(aiReturn(fixBad));

        bll.generate(r, 0, null);

        assertEquals(mergeContent, captureSavedContent());
    }

    @Test
    @DisplayName("格式调用异常 → 降级落库合并原文，报告流程不中断")
    void generate_formatCallThrows_degradesAndReportCompletes() {
        ScriptMonitorReportEntity r = setupFormatFlow();
        String mergeContent = "# 巡检报告\n<aifupan-data-block>回复率 80.0%</aifupan-data-block>";
        when(aiFeign.chatCompletion(any(), argThat((AiMessageBo p) -> sysIs(p, "合并提示词MERGE")), anyLong()))
                .thenReturn(aiReturn(mergeContent));
        when(aiFeign.chatCompletion(any(), argThat((AiMessageBo p) -> sysIs(p, "FORMAT_PROMPT")), anyLong()))
                .thenThrow(new RuntimeException("ai down"));

        bll.generate(r, 0, null);

        assertEquals(mergeContent, captureSavedContent());
        // 报告正常收尾（finishReport 被调用），未走异常补偿
        verify(reportWriteService, times(1)).finishReport(anyLong(), any(), any());
    }

    @Test
    @DisplayName("格式提示词未配置(cueType=28 缺失) → AI 调用前降级，不调用格式模型")
    void generate_formatPromptMissing_degradesBeforeAiCall() {
        ScriptMonitorReportEntity r = setupFormatFlow();
        // 覆盖：cueType=28 取不到 → loadSingleCueWord 抛异常被捕获 → 降级
        when(cueWordsFeign.getCueWordByTradeAndType(argThat((CueWordsQueryBo bo) -> bo != null && Integer.valueOf(28).equals(bo.getCueType()))))
                .thenReturn(null);
        String mergeContent = "# 巡检报告\n<aifupan-data-block>回复率 80.0%</aifupan-data-block>";
        when(aiFeign.chatCompletion(any(), argThat((AiMessageBo p) -> sysIs(p, "合并提示词MERGE")), anyLong()))
                .thenReturn(aiReturn(mergeContent));

        bll.generate(r, 0, null);

        assertEquals(mergeContent, captureSavedContent());
        verify(aiFeign, never()).chatCompletion(any(), argThat((AiMessageBo p) -> sysIs(p, "FORMAT_PROMPT")), anyLong());
    }

    @Test
    @DisplayName("systemKv 格式模型缺配 → 回退字典模型仍执行格式校验(返回原文)")
    void generate_formatModelKvMissing_fallsBackAndStillValidates() {
        ScriptMonitorReportEntity r = setupFormatFlow();
        String mergeContent = "# 巡检报告\n<aifupan-data-block>回复率 80.0%</aifupan-data-block>";
        when(aiFeign.chatCompletion(any(), argThat((AiMessageBo p) -> sysIs(p, "合并提示词MERGE")), anyLong()))
                .thenReturn(aiReturn(mergeContent));
        // systemKv format model key 缺配（setupFormatFlow 默认 getByKey 返 null）→ 回退字典模型；格式调用仍执行
        when(aiFeign.chatCompletion(any(), argThat((AiMessageBo p) -> sysIs(p, "FORMAT_PROMPT")), anyLong()))
                .thenReturn(aiReturn("__FORMAT_OK__"));

        bll.generate(r, 0, null);

        assertEquals(mergeContent, captureSavedContent());
        // 关键证伪：systemKv 缺配并非跳过校验，格式模型实际被调用了一次（走回退字典模型）
        verify(aiFeign, times(1)).chatCompletion(any(), argThat((AiMessageBo p) -> sysIs(p, "FORMAT_PROMPT")), anyLong());
    }

    @Test
    @DisplayName("格式调用 token 不计入 totalTokens、不结算")
    void generate_formatTokens_notSettled() {
        ScriptMonitorReportEntity r = setupFormatFlow();
        String mergeContent = "原始合并，缺标签";
        String fixedContent = "已修复\n<aifupan-data-block>回复率 80.0%</aifupan-data-block>";
        // 合并 token=100；格式调用给极大 token=50000，若被错误累加则结算会暴涨
        when(aiFeign.chatCompletion(any(), argThat((AiMessageBo p) -> sysIs(p, "合并提示词MERGE")), anyLong()))
                .thenReturn(aiReturnTok(mergeContent, 100));
        when(aiFeign.chatCompletion(any(), argThat((AiMessageBo p) -> sysIs(p, "FORMAT_PROMPT")), anyLong()))
                .thenReturn(aiReturnTok(fixedContent, 50000));

        bll.generate(r, 0, null);

        ArgumentCaptor<AiReturnDataVo> settleCap = ArgumentCaptor.forClass(AiReturnDataVo.class);
        verify(aiTokenWithholdFeign).settleAiToken(any(), anyLong(), settleCap.capture(), any());
        Integer settledTokens = settleCap.getValue().getTotalTokens();
        assertNotNull(settledTokens);
        assertTrue(settledTokens < 50000,
                "结算 token 不应包含格式调用的 50000，实际=" + settledTokens);
    }

    // ===========================================================================
    // buildBarrageText 用户画像标签测试（AC-tag-1 ~ AC-tag-5）— 反射直测 private buildBarrageText
    // 范式 mirror invokeBuildSummary（反射调 private 方法，不改主源可见性）
    // ===========================================================================

    /**
     * 反射调用 private buildBarrageText(List&lt;DanMuItemVo&gt;, long)。
     *
     * <p>统一传 videoStartEpoch=0 且每条弹幕 recordDate=0，使 relativeMs=0 →
     * formatMsToHms(0) 恒为 "00:00:00"，把时间段固定下来，断言只聚焦括号画像标签。</p>
     *
     * @param danmuList 弹幕列表
     * @return buildBarrageText 返回的格式化文本
     */
    private String invokeBuildBarrageText(List<DanMuItemVo> danmuList) throws Exception {
        java.lang.reflect.Method m = ScriptMonitorPatrolGenerateBll.class
                .getDeclaredMethod("buildBarrageText", List.class, long.class);
        m.setAccessible(true);
        return (String) m.invoke(bll, danmuList, 0L);
    }

    /**
     * 构造一条 recordDate=0（→ 视频内相对 [00:00:00]）的弹幕，画像字段由入参控制。
     *
     * @param nickName         昵称
     * @param content          弹幕内容
     * @param level            用户等级（null 则不加"等级"标签）
     * @param fansLevelCurrent 粉丝团等级（null 则不加"粉丝团"标签）
     * @param isNew            是否新用户（仅 TRUE 才加"新用户"标签）
     * @return DanMuItemVo
     */
    private DanMuItemVo danmu(String nickName, String content,
                             Long level, Long fansLevelCurrent, Boolean isNew) {
        DanMuItemVo d = new DanMuItemVo();
        d.setRecordDate(0L);
        d.setNickName(nickName);
        d.setContent(content);
        d.setLevel(level);
        d.setFansLevelCurrent(fansLevelCurrent);
        d.setIsNew(isNew);
        return d;
    }

    /**
     * AC-1: level + fansLevelCurrent + isNew 三字段齐全 → 括号内三标签按 等级,粉丝团,新用户 顺序拼接。
     */
    @Test
    @DisplayName("AC-tag-1 buildBarrageText 三画像字段齐全 → (等级X,粉丝团Y,新用户)")
    void buildBarrageText_allProfileFieldsSet_appendsFullTag() throws Exception {
        DanMuItemVo d = danmu("用户A", "好棒", 12L, 3L, Boolean.TRUE);
        String text = invokeBuildBarrageText(List.of(d));
        assertEquals("[00:00:00] 用户A(等级12,粉丝团3,新用户): 好棒", text,
                "三字段齐全应输出 (等级12,粉丝团3,新用户) 标签段");
    }

    /**
     * AC-2: 三画像字段全空（level/fansLevelCurrent=null, isNew=false）→ 不加任何括号，退回基础格式。
     */
    @Test
    @DisplayName("AC-tag-2 buildBarrageText 画像全空 → 无括号 fallback 基础格式")
    void buildBarrageText_allProfileFieldsNullOrFalse_noTag() throws Exception {
        DanMuItemVo d = danmu("用户B", "下单了", null, null, Boolean.FALSE);
        String text = invokeBuildBarrageText(List.of(d));
        assertEquals("[00:00:00] 用户B: 下单了", text,
                "画像全空时不应出现括号，应退回 [时间] 昵称: 内容");
    }

    /**
     * AC-2 补充: isNew=null（而非 false）同样视为无"新用户"标签，与 false 行为一致。
     */
    @Test
    @DisplayName("AC-tag-2b buildBarrageText isNew=null 等同 false → 不加新用户标签")
    void buildBarrageText_isNewNull_noNewUserTag() throws Exception {
        DanMuItemVo d = danmu("用户C", "问价", null, null, null);
        String text = invokeBuildBarrageText(List.of(d));
        assertEquals("[00:00:00] 用户C: 问价", text,
                "isNew=null 应与 false 同样不加新用户标签，且无其他字段时不加括号");
    }

    /**
     * AC-3: 仅 level 设置 → 括号内只含"等级"单标签。
     */
    @Test
    @DisplayName("AC-tag-3 buildBarrageText 仅 level → (等级X) 单标签")
    void buildBarrageText_onlyLevelSet_appendsLevelTagOnly() throws Exception {
        DanMuItemVo d = danmu("用户D", "想看链接", 8L, null, Boolean.FALSE);
        String text = invokeBuildBarrageText(List.of(d));
        assertEquals("[00:00:00] 用户D(等级8): 想看链接", text,
                "仅 level 时括号内只应有 等级8");
    }

    /**
     * AC-3 补充: 仅 fansLevelCurrent → 括号内只含"粉丝团"单标签（验证标签独立可省略）。
     */
    @Test
    @DisplayName("AC-tag-3b buildBarrageText 仅 fansLevelCurrent → (粉丝团Y) 单标签")
    void buildBarrageText_onlyFansLevelSet_appendsFansTagOnly() throws Exception {
        DanMuItemVo d = danmu("用户E", "粉丝团专属", null, 5L, Boolean.FALSE);
        String text = invokeBuildBarrageText(List.of(d));
        assertEquals("[00:00:00] 用户E(粉丝团5): 粉丝团专属", text,
                "仅 fansLevelCurrent 时括号内只应有 粉丝团5");
    }

    /**
     * AC-3 补充: 仅 isNew=true → 括号内只含"新用户"单标签。
     */
    @Test
    @DisplayName("AC-tag-3c buildBarrageText 仅 isNew=true → (新用户) 单标签")
    void buildBarrageText_onlyIsNewTrue_appendsNewUserTagOnly() throws Exception {
        DanMuItemVo d = danmu("用户F", "第一次来", null, null, Boolean.TRUE);
        String text = invokeBuildBarrageText(List.of(d));
        assertEquals("[00:00:00] 用户F(新用户): 第一次来", text,
                "仅 isNew=true 时括号内只应有 新用户");
    }

    /**
     * 空列表防守: buildBarrageText 收到空 list → 返回 ""（CollUtil.isEmpty 早退分支）。
     */
    @Test
    @DisplayName("AC-tag-4 buildBarrageText 空列表 → 返回空串")
    void buildBarrageText_emptyList_returnsEmptyString() throws Exception {
        String text = invokeBuildBarrageText(List.of());
        assertEquals("", text, "空弹幕列表应返回空字符串");
    }

    /**
     * 多条弹幕: 不同画像组合并存 → 各行独立拼接，以换行连接（验证 stream+join 不串味）。
     */
    @Test
    @DisplayName("AC-tag-5 buildBarrageText 多条弹幕 → 各行独立标签以换行连接")
    void buildBarrageText_multipleDanmu_eachLineIndependent() throws Exception {
        DanMuItemVo d1 = danmu("用户A", "好棒", 12L, 3L, Boolean.TRUE);
        DanMuItemVo d2 = danmu("用户B", "下单了", null, null, Boolean.FALSE);
        String text = invokeBuildBarrageText(List.of(d1, d2));
        assertEquals(
                "[00:00:00] 用户A(等级12,粉丝团3,新用户): 好棒\n[00:00:00] 用户B: 下单了",
                text,
                "多条弹幕应各自独立拼接画像标签，以 \\n 连接");
    }

    // ====== resolveTradeId VIDEO 来源改造（2026-06-22）======

    /**
     * 构造带 secUid 的视频对象（resolveTradeId VIDEO 分支需 secUid 非空）。
     *
     * @param secUid    主播 secUid
     * @param videoTradeId 录制记录行业（应被忽略）
     * @return AnchorVideoInfoVo（duration=600 秒 → 1 单元）
     */
    private AnchorVideoInfoVo videoWithSecUid(String secUid, Long videoTradeId) {
        AnchorVideoInfoVo v = new AnchorVideoInfoVo();
        v.setVideoId("v1");
        v.setSecUid(secUid);
        v.setTenantId(1L);
        v.setUserId(10L);
        v.setBatchNumber("batch-1");
        v.setDuration(600L);
        v.setTradeId(videoTradeId);
        v.setPlatformType("1");
        v.setStartTime(new Date(1732449900000L));
        return v;
    }

    /**
     * 构造直播间维度主播关联 VO（提供 tradeId）。
     */
    private AnchorUrlUserVo anchorUser(Long tradeId) {
        AnchorUrlUserVo v = new AnchorUrlUserVo();
        v.setTradeId(tradeId);
        return v;
    }

    /**
     * 按 (tradeId, cueType) 匹配 CueWordsQueryBo（patrol report tenantId=1L）。
     */
    private static org.mockito.ArgumentMatcher<CueWordsQueryBo> tradeCueMatcher(long tradeId, int cueType) {
        return bo -> bo != null
                && Long.valueOf(tradeId).equals(bo.getTradeId())
                && Integer.valueOf(cueType).equals(bo.getCueType());
    }

    /**
     * resolveTradeId 改造 AC-2：VIDEO 来源行业取直播间维度 tb_anchor_url_user.trade_id，
     * 不再用录制记录 video.tradeId。video.tradeId=99 但 anchorUser.tradeId=7 → cueWords 应按 7 取。
     */
    @Test
    @DisplayName("resolveTradeId VIDEO 来源: 取 anchorUrlUserFeign trade_id(7)，忽略 video.tradeId(99)")
    void resolveTradeId_videoSource_usesAnchorUrlUserTradeId_notVideoTradeId() {
        ScriptMonitorReportEntity r = report();
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(videoWithSecUid("sec-1", 99L)));
        when(anchorVideoFeign.hasBarrage("v1")).thenReturn(R.ok(true));
        // 直播间维度行业 = 7（report.userId=10, report.tenantId=1）
        when(anchorUrlUserFeign.getBySecUidAndUser("sec-1", 10L, 1L)).thenReturn(R.ok(anchorUser(7L)));
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(R.ok(buildAsrResult()));
        when(tableStoreFeign.queryDanMuSearchData(any())).thenReturn(List.of());
        // 提示词按 tradeId=7 取（cueType=20 切片 / 21 合并）
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(tradeCueMatcher(7L, 20)))).thenReturn(cueWord("切片提示词"));
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(tradeCueMatcher(7L, 21)))).thenReturn(cueWord("合并提示词"));
        when(aiModelFeign.listDiagnosisModel()).thenReturn(List.of(aiModel()));
        when(tradeFeign.listTradeByIds(any())).thenReturn(List.of());
        when(aiFeign.chatCompletion(any(), any(), anyLong()))
                .thenReturn(aiReturn("# 巡检报告\n\n<aifupan-data-block>有效回复率 80.0%</aifupan-data-block>"));
        doNothing().when(bodyRepository).deleteByReportId(anyLong());
        when(bodyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(reportWriteService).finishReport(anyLong(), any(), any());
        doNothing().when(aiTokenWithholdFeign).settleAiToken(any(), anyLong(), any(), any());

        bll.generate(r, 0, null);

        // 关键断言：提示词按直播间维度 tradeId=7 取，未按 video.tradeId=99 取
        verify(cueWordsFeign).getCueWordByTradeAndType(argThat(tradeCueMatcher(7L, 20)));
        verify(cueWordsFeign, never()).getCueWordByTradeAndType(argThat(tradeCueMatcher(99L, 20)));
        verify(reportWriteService, times(1)).finishReport(anyLong(), any(), any());
    }

    /**
     * resolveTradeId 改造 AC-2 边界：VIDEO 来源 anchorUrlUserFeign 查不到（R.data=null）→ 兜底 tradeId=1。
     */
    @Test
    @DisplayName("resolveTradeId VIDEO 来源: anchorUrlUserFeign 返 null → 兜底 tradeId=1")
    void resolveTradeId_videoSource_anchorUserNull_fallbackToOne() {
        ScriptMonitorReportEntity r = report();
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(videoWithSecUid("sec-1", 99L)));
        when(anchorVideoFeign.hasBarrage("v1")).thenReturn(R.ok(true));
        // 直播间维度查不到（R(code=0, data=null) → ResultUtil.getResult 返 null）
        when(anchorUrlUserFeign.getBySecUidAndUser("sec-1", 10L, 1L)).thenReturn(R.ok(null));
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(R.ok(buildAsrResult()));
        when(tableStoreFeign.queryDanMuSearchData(any())).thenReturn(List.of());
        // 提示词按兜底 tradeId=1 取
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(tradeCueMatcher(1L, 20)))).thenReturn(cueWord("切片提示词"));
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(tradeCueMatcher(1L, 21)))).thenReturn(cueWord("合并提示词"));
        when(aiModelFeign.listDiagnosisModel()).thenReturn(List.of(aiModel()));
        when(tradeFeign.listTradeByIds(any())).thenReturn(List.of());
        when(aiFeign.chatCompletion(any(), any(), anyLong()))
                .thenReturn(aiReturn("# 巡检报告\n\n<aifupan-data-block>有效回复率 80.0%</aifupan-data-block>"));
        doNothing().when(bodyRepository).deleteByReportId(anyLong());
        when(bodyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(reportWriteService).finishReport(anyLong(), any(), any());
        doNothing().when(aiTokenWithholdFeign).settleAiToken(any(), anyLong(), any(), any());

        bll.generate(r, 0, null);

        // 关键断言：提示词按兜底 tradeId=1 取
        verify(cueWordsFeign).getCueWordByTradeAndType(argThat(tradeCueMatcher(1L, 20)));
        verify(reportWriteService, times(1)).finishReport(anyLong(), any(), any());
    }
}
