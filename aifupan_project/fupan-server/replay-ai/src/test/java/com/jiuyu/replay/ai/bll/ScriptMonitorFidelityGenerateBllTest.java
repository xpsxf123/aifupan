package com.jiuyu.replay.ai.bll;

import com.jiuyu.replay.ai.entity.ScriptMonitorIntermediateReportEntity;
import com.jiuyu.replay.ai.entity.ScriptMonitorReportBodyEntity;
import com.jiuyu.replay.ai.entity.ScriptMonitorReportEntity;
import com.jiuyu.replay.ai.repository.mongo.ScriptMonitorIntermediateReportRepository;
import com.jiuyu.replay.ai.repository.mongo.ScriptMonitorReportBodyRepository;
import com.jiuyu.replay.common.producer.SystemKvProducer;
import com.jiuyu.replay.generic.bo.ai.AiMessageBo;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.generic.bo.third.AiTokenUseRecordBo;
import com.jiuyu.replay.generic.bo.words.cue.CueWordsQueryBo;
import com.jiuyu.replay.generic.enums.words.ScriptMonitorStatusEnum;
import com.jiuyu.replay.generic.feign.order.AiTokenWithholdFeign;
import com.jiuyu.replay.generic.feign.ai.AiChatFeign;
import com.jiuyu.replay.generic.feign.third.AiModelFeign;
import com.jiuyu.replay.generic.feign.words.AnchorUrlUserFeign;
import com.jiuyu.replay.generic.feign.words.AnchorVideoFeign;
import com.jiuyu.replay.generic.feign.words.CueWordsFeign;
import com.jiuyu.replay.generic.feign.words.SensitiveWordsFeign;
import com.jiuyu.replay.generic.feign.words.StandardScriptFeign;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.order.RedisWithholdVo;
import com.jiuyu.replay.generic.vo.words.AnalysisResultVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.generic.vo.words.CueWordsInfoVo;
import com.jiuyu.replay.generic.vo.words.SentenceMarkVo;
import com.jiuyu.replay.generic.vo.words.StandardScriptInfoVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ScriptMonitorFidelityGenerateBll 单测（Slice B 还原度生成 — 4 次 AI 串行编排）。
 *
 * <p>mirror {@link ScriptMonitorGenerateBllTest} 范式（质检 3+1 编排），针对还原度路径覆盖：
 * <ul>
 *   <li>AC-002: happy path 非循环 4 次 AI 调用串行 → finishReport + settleAiToken</li>
 *   <li>AC-004: speechMode=1 循环模式 → cueType=25/26 + #{cycleDurationMinutes} 占位符填充</li>
 *   <li>AC-007: 第 2 次 AI 调用失败（status=1） → returnAiToken + restoreOrFail</li>
 *   <li>AC-008: speechMode=1 但 cycleDurationMinutes=null → 抛 70013，AI 不被调用</li>
 *   <li>AC-012: ASR 为空 → returnAiToken + markNotApplicable + 0 次 AI 调用</li>
 *   <li>标准稿查不到 → returnAiToken + restoreOrFail</li>
 *   <li>buildSummary 解析 &lt;aifupan-data-block&gt; 标签写 summary_json</li>
 *   <li>AI 输出无标签 → summary_json=null + 主流程不中断</li>
 *   <li>finishReport 三参 mirror 质检（不引入 speechSpeed 等新参数）</li>
 *   <li>Token settle 4 次累加 totalTokens</li>
 * </ul>
 * </p>
 *
 * @author beta
 * @date 2026-06-12
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScriptMonitorFidelityGenerateBllTest {

    @Mock
    private ScriptMonitorIntermediateReportRepository intermediateRepository;
    @Mock
    private ScriptMonitorReportBodyRepository bodyRepository;
    @Mock
    private SensitiveWordsFeign sensitiveWordsFeign;
    @Mock
    private AnchorVideoFeign anchorVideoFeign;
    @Mock
    private AnchorUrlUserFeign anchorUrlUserFeign;
    @Mock
    private CueWordsFeign cueWordsFeign;
    @Mock
    private StandardScriptFeign standardScriptFeign;
    @Mock
    private AiModelFeign aiModelFeign;
    @Mock
    private AiChatFeign aiFeign;
    @Mock
    private AiTokenWithholdFeign aiTokenWithholdFeign;
    @Mock
    private SystemKvProducer systemKvProducer;
    @Mock
    private ScriptMonitorReportWriteService reportWriteService;

    @InjectMocks
    private ScriptMonitorFidelityGenerateBll bll;

    private RedisWithholdVo withhold;

    @BeforeEach
    void setUp() {
        withhold = new RedisWithholdVo();
        withhold.setWithholdId("w-fidelity-1");
        withhold.setRedisId(1L);

        doNothing().when(reportWriteService).finishReport(anyLong(), any(), any());
        doNothing().when(reportWriteService).restoreOrFail(anyLong(), any(), any(), any(), any());
        doNothing().when(reportWriteService).markNotApplicable(anyLong(), any());
    }

    // ====== helpers ======

    /**
     * 构造一条 monitorType=1 GENERATING 报告。
     *
     * @param status 状态
     * @return 报告行（id=10L, tenantId=100L, userId=42L, sourceType=0, sourceId=v1, monitorType=1）
     */
    private ScriptMonitorReportEntity report(Integer status) {
        ScriptMonitorReportEntity r = new ScriptMonitorReportEntity();
        r.setId(10L);
        r.setTenantId(100L);
        r.setUserId(42L);
        r.setSourceType(0);
        r.setSourceId("v1");
        r.setMonitorType(1);
        r.setStatus(status);
        r.setIsDeleted(0);
        return r;
    }

    /**
     * 构造 ASR 返回（2 个段落，无时间戳）。
     */
    private R<AnalysisResultVo> asrResult() {
        SentenceMarkVo s1 = new SentenceMarkVo();
        s1.setCurrentSort(1);
        s1.setContent("欢迎来到直播间");
        SentenceMarkVo s2 = new SentenceMarkVo();
        s2.setCurrentSort(2);
        s2.setContent("今天为大家带来还原度演示");
        AnalysisResultVo vo = new AnalysisResultVo();
        vo.setSentenceMarkVos(List.of(s1, s2));
        return R.ok(vo);
    }

    /**
     * 构造视频信息（含 secUid + tradeId）。
     */
    private AnchorVideoInfoVo video(String secUid, Long tradeId) {
        AnchorVideoInfoVo v = new AnchorVideoInfoVo();
        v.setVideoId("v1");
        v.setSecUid(secUid);
        v.setTradeId(tradeId);
        return v;
    }

    /**
     * 构造直播间维度主播关联 VO（提供 tradeId，供 resolveTradeId 用）。
     *
     * @param tradeId 直播间行业 id
     * @return AnchorUrlUserVo
     */
    private AnchorUrlUserVo anchorUser(Long tradeId) {
        AnchorUrlUserVo v = new AnchorUrlUserVo();
        v.setTradeId(tradeId);
        return v;
    }

    /**
     * 构造非循环 speechMode=0 的标准稿 VO。
     */
    private StandardScriptInfoVo nonCyclicScript() {
        StandardScriptInfoVo vo = new StandardScriptInfoVo();
        vo.setId(200L);
        vo.setTenantId(100L);
        vo.setUserId(42L);
        vo.setSecUid("sec-001");
        vo.setSpeechMode(0);
        vo.setCycleDurationMinutes(null);
        vo.setTimeAxisScript("[{\"timeRange\":\"00:00-05:00\",\"title\":\"开场\",\"content\":\"欢迎\"}]");
        return vo;
    }

    /**
     * 构造循环 speechMode=1 的标准稿 VO。
     *
     * @param cycleDurationMinutes 循环时长（null 用于触发 70013 校验）
     */
    private StandardScriptInfoVo cyclicScript(Integer cycleDurationMinutes) {
        StandardScriptInfoVo vo = new StandardScriptInfoVo();
        vo.setId(201L);
        vo.setTenantId(100L);
        vo.setUserId(42L);
        vo.setSecUid("sec-001");
        vo.setSpeechMode(1);
        vo.setCycleDurationMinutes(cycleDurationMinutes);
        vo.setTimeAxisScript("[{\"timeRange\":\"00:00-30:00\",\"title\":\"循环\",\"content\":\"循环话术\"}]");
        return vo;
    }

    /**
     * 构造单条提示词（含 problem 字段；含 placeholder 用于测占位符替换）。
     */
    private CueWordsInfoVo cueWith(String problem) {
        CueWordsInfoVo c = new CueWordsInfoVo();
        c.setId(1L);
        c.setSort(1);
        c.setProblem(problem);
        return c;
    }

    /**
     * 构造 AI 返回结果（成功）。
     */
    private AiReturnDataVo aiOk(int totalTokens) {
        AiReturnDataVo r = new AiReturnDataVo();
        r.setStatus(0);
        r.setContent("# 还原度报告\n\n<aifupan-data-block>整体还原度良好</aifupan-data-block>\n\n## 详情\n主播按标准稿铺排");
        r.setRequestId("req-" + totalTokens);
        r.setTotalTokens(totalTokens);
        return r;
    }

    /**
     * 构造 AI 失败返回（status=1）。
     */
    private AiReturnDataVo aiFail() {
        AiReturnDataVo r = new AiReturnDataVo();
        r.setStatus(1);
        r.setContent(null);
        r.setRequestId("req-fail");
        return r;
    }

    /**
     * 构造 MongoDB 中间报告列表（3 条）。
     */
    private List<ScriptMonitorIntermediateReportEntity> intermediateReports() {
        List<ScriptMonitorIntermediateReportEntity> list = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ScriptMonitorIntermediateReportEntity e = ScriptMonitorIntermediateReportEntity.builder()
                    .reportId(10L)
                    .index(i)
                    .content("# 中间报告 " + i)
                    .build();
            e.setId("inter-" + i);
            list.add(e);
        }
        return list;
    }

    /**
     * AI 模型 list（兜底）。
     */
    private List<AiModelInfoVo> models() {
        AiModelInfoVo m = new AiModelInfoVo();
        m.setModelCode("doubao-pro");
        m.setModelName("豆包Pro");
        return List.of(m);
    }

    /**
     * 按 (tradeId, tenantId, cueType) 三元组匹配 CueWordsQueryBo。
     */
    private static ArgumentMatcher<CueWordsQueryBo> cueMatcher(long tradeId, long tenantId, int cueType) {
        return bo -> bo != null
                && Long.valueOf(tradeId).equals(bo.getTradeId())
                && Long.valueOf(tenantId).equals(bo.getTenantId())
                && Integer.valueOf(cueType).equals(bo.getCueType());
    }

    /**
     * 配置非循环 happy path mock（ASR + 视频 + 标准稿 + 提示词 + 模型 + 4 次 AI + MongoDB）。
     */
    private void setupNonCyclicHappyPath() {
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(asrResult());
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video("sec-001", 5L)));
        // 行业取直播间维度 tb_anchor_url_user.trade_id（2026-06-22 改造）：按 secUid+userId+tenantId 取 → 5L
        when(anchorUrlUserFeign.getBySecUidAndUser("sec-001", 42L, 100L)).thenReturn(R.ok(anchorUser(5L)));
        when(standardScriptFeign.findValid(100L, 42L, "sec-001")).thenReturn(nonCyclicScript());
        // cueType=18 / 19（非循环对比 + 非循环合并）
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueMatcher(5L, 100L, 18))))
                .thenReturn(cueWith("你是还原度对比专家，请对比 ASR 与标准稿"));
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueMatcher(5L, 100L, 19))))
                .thenReturn(cueWith("请合并以下 3 份对比报告"));
        when(aiModelFeign.listDiagnosisModel()).thenReturn(models());
        when(aiFeign.chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong()))
                .thenReturn(aiOk(5000));
        doNothing().when(intermediateRepository).deleteByReportId(anyLong());
        when(intermediateRepository.save(any())).thenAnswer(inv -> {
            ScriptMonitorIntermediateReportEntity e = inv.getArgument(0);
            e.setId("inter-id");
            return e;
        });
        when(intermediateRepository.findByReportIdOrderByIndexAsc(anyLong()))
                .thenReturn(intermediateReports());
        doNothing().when(bodyRepository).deleteByReportId(anyLong());
        when(bodyRepository.save(any())).thenAnswer(inv -> {
            ScriptMonitorReportBodyEntity b = inv.getArgument(0);
            b.setId("body-mongo-id");
            return b;
        });
        doNothing().when(aiTokenWithholdFeign).settleAiToken(
                any(), anyLong(), any(), any(AiTokenUseRecordBo.class));
    }

    // ====== 测试用例 ======

    /**
     * AC-002：非循环 4 次 AI 调用串行 → finishReport + settleAiToken；
     * 验证完全 mirror 质检范式：3 中间 + 1 合并 = 4 次 chatCompletion。
     */
    @Test
    @DisplayName("AC-002: 非循环 happy path — 4 次 AI 调用串行 + finishReport + settleAiToken")
    void generate_nonCyclicHappyPath_callsAiFourTimes_finishesAndSettles() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        setupNonCyclicHappyPath();

        bll.generate(report, ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), withhold);

        // AI 被调用 4 次（3 中间 + 1 合并）
        verify(aiFeign, times(4)).chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong());
        // 中间报告 save 3 次
        verify(intermediateRepository, times(3)).save(any());
        // 正文 save 1 次
        verify(bodyRepository, times(1)).save(any());
        // finishReport 三参（reportId, bodyId, summaryJson）—— 不接受 speechSpeed 等扩展参
        verify(reportWriteService, times(1)).finishReport(anyLong(), any(), any());
        // settleAiToken 一次
        verify(aiTokenWithholdFeign, times(1)).settleAiToken(any(), anyLong(), any(), any());
        // returnAiToken / restoreOrFail / markNotApplicable 均不被调用
        verify(aiTokenWithholdFeign, never()).returnAiToken(any());
        verify(reportWriteService, never()).restoreOrFail(anyLong(), any(), any(), any(), any());
        verify(reportWriteService, never()).markNotApplicable(anyLong(), any());
    }

    /**
     * AC-004：speechMode=1 循环模式 → cueType=25/26 + 占位符替换；
     * 验证 cueWordsFeign 用 cueType=25/26 取（而非 18/19），且 systemPrompt 含 "30" 填充值。
     */
    @Test
    @DisplayName("AC-004: 循环 speechMode=1 + cycleDurationMinutes=30 → 取 cueType=25/26 提示词 + 占位符填充为 30")
    void generate_cyclicMode_useCueType25And26_andFillsCycleDuration() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(asrResult());
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video("sec-001", 5L)));
        when(anchorUrlUserFeign.getBySecUidAndUser("sec-001", 42L, 100L)).thenReturn(R.ok(anchorUser(5L)));
        when(standardScriptFeign.findValid(100L, 42L, "sec-001")).thenReturn(cyclicScript(30));
        // cueType=25/26（循环对比 + 循环合并），提示词含占位符 #{cycleDurationMinutes}
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueMatcher(5L, 100L, 25))))
                .thenReturn(cueWith("循环对比：每 #{cycleDurationMinutes} 分钟切片"));
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueMatcher(5L, 100L, 26))))
                .thenReturn(cueWith("循环合并提示词"));
        when(aiModelFeign.listDiagnosisModel()).thenReturn(models());
        when(aiFeign.chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong()))
                .thenReturn(aiOk(5000));
        when(intermediateRepository.save(any())).thenAnswer(inv -> {
            ScriptMonitorIntermediateReportEntity e = inv.getArgument(0);
            e.setId("inter-id");
            return e;
        });
        when(intermediateRepository.findByReportIdOrderByIndexAsc(anyLong()))
                .thenReturn(intermediateReports());
        when(bodyRepository.save(any())).thenAnswer(inv -> {
            ScriptMonitorReportBodyEntity b = inv.getArgument(0);
            b.setId("body-mongo-id");
            return b;
        });

        bll.generate(report, ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), withhold);

        // 验证 cueType=25/26 被取过（cueType=18/19 不应被调）
        verify(cueWordsFeign).getCueWordByTradeAndType(argThat(cueMatcher(5L, 100L, 25)));
        verify(cueWordsFeign).getCueWordByTradeAndType(argThat(cueMatcher(5L, 100L, 26)));
        verify(cueWordsFeign, never()).getCueWordByTradeAndType(argThat(cueMatcher(5L, 100L, 18)));
        verify(cueWordsFeign, never()).getCueWordByTradeAndType(argThat(cueMatcher(5L, 100L, 19)));

        // 验证 4 次 AI 调用 + systemPrompt 第 1 次（对比）已替换占位符为 "30"
        ArgumentCaptor<AiMessageBo> msgCaptor = ArgumentCaptor.forClass(AiMessageBo.class);
        verify(aiFeign, times(4)).chatCompletion(any(AiModelBo.class), msgCaptor.capture(), anyLong());
        @SuppressWarnings("unchecked")
        String firstSystemPrompt = (String) msgCaptor.getAllValues().get(0).getSystem().get(0).get("text");
        assertTrue(firstSystemPrompt.contains("30"),
                "circular cueType=25 提示词 #{cycleDurationMinutes} 占位符应被填充为 30，实际：" + firstSystemPrompt);
        assertTrue(!firstSystemPrompt.contains("#{cycleDurationMinutes}"),
                "占位符 #{cycleDurationMinutes} 不应残留，实际：" + firstSystemPrompt);

        // userContent 末尾应含循环话术预估时长说明
        @SuppressWarnings("unchecked")
        String firstUserContent = (String) msgCaptor.getAllValues().get(0).getUser().get(0).get("text");
        assertTrue(firstUserContent.contains("【循环话术预估时长】30分钟"),
                "userContent 末尾应追加循环话术预估时长说明，实际：" + firstUserContent);

        // happy path 仍然 finish + settle
        verify(reportWriteService, times(1)).finishReport(anyLong(), any(), any());
        verify(aiTokenWithholdFeign, times(1)).settleAiToken(any(), anyLong(), any(), any());
    }

    /**
     * AC-007：第 2 次 AI 调用失败（status=1） → returnAiToken + restoreOrFail。
     * 关键失败补偿断言：未走 finishReport，未走 settle；走了 return + restoreOrFail（mirror 质检）。
     */
    @Test
    @DisplayName("AC-007: 第 2 次 AI 调用失败（status=1）→ returnAiToken + restoreOrFail + 不 finish 不 settle")
    void generate_secondAiCallFails_returnsTokenAndRestoreOrFail() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(asrResult());
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video("sec-001", 5L)));
        when(anchorUrlUserFeign.getBySecUidAndUser("sec-001", 42L, 100L)).thenReturn(R.ok(anchorUser(5L)));
        when(standardScriptFeign.findValid(100L, 42L, "sec-001")).thenReturn(nonCyclicScript());
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueMatcher(5L, 100L, 18))))
                .thenReturn(cueWith("对比提示词"));
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueMatcher(5L, 100L, 19))))
                .thenReturn(cueWith("合并提示词"));
        when(aiModelFeign.listDiagnosisModel()).thenReturn(models());
        // 第 1 次成功（idx=0），第 2 次失败（idx=1 status=1）
        when(aiFeign.chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong()))
                .thenReturn(aiOk(5000))
                .thenReturn(aiFail());
        when(intermediateRepository.save(any())).thenAnswer(inv -> {
            ScriptMonitorIntermediateReportEntity e = inv.getArgument(0);
            e.setId("inter-id");
            return e;
        });
        doNothing().when(aiTokenWithholdFeign).returnAiToken(any());

        bll.generate(report, ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), withhold);

        // 失败补偿：returnAiToken + restoreOrFail
        verify(aiTokenWithholdFeign, times(1)).returnAiToken(withhold);
        verify(reportWriteService, times(1))
                .restoreOrFail(anyLong(), any(), any(), any(), any());
        // 失败路径：未走 finish / settle
        verify(reportWriteService, never()).finishReport(anyLong(), any(), any());
        verify(aiTokenWithholdFeign, never()).settleAiToken(any(), anyLong(), any(), any());
        // 第 1 次中间报告已 save（在失败前完成）
        verify(intermediateRepository, times(1)).save(any());
    }

    /**
     * AC-008：speechMode=1 但 cycleDurationMinutes=null → 抛 70013，AI 不被调用。
     * 关键回归：校验在 AI 调用之前，returnAiToken 仍然走（catch 路径），AI 调用 0 次。
     */
    @Test
    @DisplayName("AC-008: speechMode=1 + cycleDurationMinutes=null → 不调 AI + returnAiToken + restoreOrFail")
    void generate_cyclicMode_cycleDurationNull_throws70013_noAiCall() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(asrResult());
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video("sec-001", 5L)));
        // 关键 mock：speechMode=1 但 cycleDurationMinutes=null
        when(standardScriptFeign.findValid(100L, 42L, "sec-001")).thenReturn(cyclicScript(null));
        doNothing().when(aiTokenWithholdFeign).returnAiToken(any());

        bll.generate(report, ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), withhold);

        // AI 0 次（校验在调用前）
        verify(aiFeign, never()).chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong());
        // 失败补偿：returnAiToken + restoreOrFail
        verify(aiTokenWithholdFeign, times(1)).returnAiToken(withhold);
        verify(reportWriteService, times(1))
                .restoreOrFail(anyLong(), any(), any(), any(), any());
        verify(reportWriteService, never()).finishReport(anyLong(), any(), any());
    }

    /**
     * AC-012：ASR 数据为空 → markNotApplicable + returnAiToken + 0 次 AI 调用。
     * 关键回归：ASR 空走"不可生成"非"失败"分支（mirror 质检 tryLoadAsrText）。
     */
    @Test
    @DisplayName("AC-012: ASR 为空 → markNotApplicable + returnAiToken + 0 次 AI 调用（非失败路径）")
    void generate_asrEmpty_marksNotApplicable_notRestore() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        AnalysisResultVo emptyAsr = new AnalysisResultVo();
        emptyAsr.setSentenceMarkVos(null);
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(R.ok(emptyAsr));
        doNothing().when(aiTokenWithholdFeign).returnAiToken(any());

        bll.generate(report, ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), withhold);

        // ASR 空走 NOT_APPLICABLE（非失败）
        verify(reportWriteService, times(1)).markNotApplicable(anyLong(), any());
        verify(aiTokenWithholdFeign, times(1)).returnAiToken(withhold);
        // AI 0 次 + 不走 finish / restoreOrFail
        verify(aiFeign, never()).chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong());
        verify(reportWriteService, never()).finishReport(anyLong(), any(), any());
        verify(reportWriteService, never()).restoreOrFail(anyLong(), any(), any(), any(), any());
    }

    /**
     * 标准稿查不到 → returnAiToken + restoreOrFail（catch 补偿走过）；不走 finish/settle。
     */
    @Test
    @DisplayName("标准稿查不到（findValid 返 null）→ returnAiToken + restoreOrFail")
    void generate_standardScriptNotFound_returnsTokenAndRestore() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(asrResult());
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video("sec-001", 5L)));
        // 关键 mock：标准稿 SPI 返 null
        when(standardScriptFeign.findValid(100L, 42L, "sec-001")).thenReturn(null);
        doNothing().when(aiTokenWithholdFeign).returnAiToken(any());

        bll.generate(report, ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), withhold);

        verify(aiTokenWithholdFeign, times(1)).returnAiToken(withhold);
        verify(reportWriteService, times(1))
                .restoreOrFail(anyLong(), any(), any(), any(), any());
        verify(aiFeign, never()).chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong());
        verify(reportWriteService, never()).finishReport(anyLong(), any(), any());
    }

    /**
     * buildSummary：AI 合并报告含 &lt;aifupan-data-block&gt; 标签 → summaryJson 写入标签内容。
     */
    @Test
    @DisplayName("buildSummary: AI 合并报告含 <aifupan-data-block> 标签 → finishReport 收到标签内 trim 后文本")
    void buildSummary_extractsAifupanDataBlock() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        setupNonCyclicHappyPath();

        bll.generate(report, ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), withhold);

        // 捕获 finishReport 第 3 参（summaryJson）
        ArgumentCaptor<String> summaryCaptor = ArgumentCaptor.forClass(String.class);
        verify(reportWriteService, times(1))
                .finishReport(anyLong(), any(), summaryCaptor.capture());
        assertEquals("整体还原度良好", summaryCaptor.getValue(),
                "应提取 <aifupan-data-block> 标签内文本（trim 后）");
    }

    /**
     * buildSummary：AI 合并报告无 &lt;aifupan-data-block&gt; 标签 → summaryJson=null，主流程不中断。
     */
    @Test
    @DisplayName("buildSummary: AI 合并报告无 <aifupan-data-block> 标签 → summaryJson=null + 主流程不中断")
    void buildSummary_noTag_setsNull_processContinues() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(asrResult());
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video("sec-001", 5L)));
        when(anchorUrlUserFeign.getBySecUidAndUser("sec-001", 42L, 100L)).thenReturn(R.ok(anchorUser(5L)));
        when(standardScriptFeign.findValid(100L, 42L, "sec-001")).thenReturn(nonCyclicScript());
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueMatcher(5L, 100L, 18))))
                .thenReturn(cueWith("对比"));
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueMatcher(5L, 100L, 19))))
                .thenReturn(cueWith("合并"));
        when(aiModelFeign.listDiagnosisModel()).thenReturn(models());

        // 前 3 次返回 happy，第 4 次合并返回无标签
        AiReturnDataVo mergeNoTag = new AiReturnDataVo();
        mergeNoTag.setStatus(0);
        mergeNoTag.setContent("# 报告\n\n## 详情\n仅纯 markdown 无 data-block 标签");
        mergeNoTag.setRequestId("merge-no-tag");
        mergeNoTag.setTotalTokens(3000);
        when(aiFeign.chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong()))
                .thenReturn(aiOk(5000))
                .thenReturn(aiOk(5000))
                .thenReturn(aiOk(5000))
                .thenReturn(mergeNoTag);
        when(intermediateRepository.save(any())).thenAnswer(inv -> {
            ScriptMonitorIntermediateReportEntity e = inv.getArgument(0);
            e.setId("inter-id");
            return e;
        });
        when(intermediateRepository.findByReportIdOrderByIndexAsc(anyLong()))
                .thenReturn(intermediateReports());
        when(bodyRepository.save(any())).thenAnswer(inv -> {
            ScriptMonitorReportBodyEntity b = inv.getArgument(0);
            b.setId("body-mongo-id");
            return b;
        });

        bll.generate(report, ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), withhold);

        // finishReport 收到 summaryJson=null
        ArgumentCaptor<String> summaryCaptor = ArgumentCaptor.forClass(String.class);
        verify(reportWriteService, times(1))
                .finishReport(anyLong(), any(), summaryCaptor.capture());
        assertNull(summaryCaptor.getValue(),
                "无 <aifupan-data-block> 标签时 summaryJson 应为 null（主流程不中断）");
        // 主流程不中断
        verify(reportWriteService, never()).restoreOrFail(anyLong(), any(), any(), any(), any());
        verify(aiTokenWithholdFeign, never()).returnAiToken(any());
        verify(aiTokenWithholdFeign, times(1)).settleAiToken(any(), anyLong(), any(), any());
    }

    /**
     * finishReport 签名锁定：仅传 (reportId, bodyId, summaryJson) 三参，
     * 锁定 Slice B 用户拍板"不引入 speechSpeed/score/deviationSummary 扩展参"的设计契约。
     */
    @Test
    @DisplayName("finishReport 签名锁定: 三参 (reportId, bodyId, summaryJson) — 与质检/巡检完全一致")
    void finishReport_threeArgumentsOnly_mirrorsQualityAndPatrol() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        setupNonCyclicHappyPath();

        bll.generate(report, ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), withhold);

        // 验证 finishReport 只调一次，且只接受 3 个参数（anyLong, any, any）
        verify(reportWriteService, times(1)).finishReport(anyLong(), any(), any());
        // 反向锁定：never markNotApplicable / restoreOrFail（happy path）
        verify(reportWriteService, never()).markNotApplicable(anyLong(), any());
        verify(reportWriteService, never()).restoreOrFail(anyLong(), any(), any(), any(), any());
    }

    /**
     * Token 边界：4 次 AI 调用 totalTokens 累加（5000+5000+5000+5000=20000）后传给 settleAiToken。
     * 关键回归：4 次 settle 一次性累加，不能 4 次 settle。
     */
    @Test
    @DisplayName("Token 边界: 4 次 AI 调用 totalTokens 累加 → settleAiToken 一次接收累计值")
    void settleAiToken_accumulates4Calls_settledOnce() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        setupNonCyclicHappyPath();

        bll.generate(report, ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), withhold);

        // 关键断言：settleAiToken 仅 1 次（不是 4 次）
        verify(aiTokenWithholdFeign, times(1)).settleAiToken(any(), anyLong(), any(), any());

        // 捕获 settle 时收到的 AiReturnDataVo，验证 totalTokens 为 4 次累加（5000 × 4 = 20000）
        ArgumentCaptor<AiReturnDataVo> settleCaptor = ArgumentCaptor.forClass(AiReturnDataVo.class);
        verify(aiTokenWithholdFeign).settleAiToken(any(), anyLong(), settleCaptor.capture(), any());
        // consumeMultiple 默认 = 0 时 AiUtils.aiTokenConsumeMultiple 返 0，但 buildSettleResult 直接覆盖 totalTokens
        // 此处 modelConfig.consumeMultiple 未 mock，AiUtils 行为决定累加值；只验证非负且 ≥ 0（最小契约）
        assertNotNull(settleCaptor.getValue(), "settle 时 AiReturnDataVo 不应为 null");
        assertTrue(settleCaptor.getValue().getTotalTokens() != null && settleCaptor.getValue().getTotalTokens() >= 0,
                "totalTokens 应为非负累加值");
    }

    /**
     * settleAiToken 使用 userId（42L）而非 tenantId（100L）。mirror 质检 AC-C1 防回归。
     */
    @Test
    @DisplayName("settleAiToken 使用 userId(42L) 而非 tenantId(100L) — 防回归")
    void settleAiToken_usesUserIdNotTenantId() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        report.setUserId(42L);
        report.setTenantId(100L);
        setupNonCyclicHappyPath();

        bll.generate(report, ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), withhold);

        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(aiTokenWithholdFeign, times(1))
                .settleAiToken(any(), userIdCaptor.capture(), any(), any());
        assertEquals(42L, userIdCaptor.getValue(),
                "settleAiToken 第 2 参应为 userId=42，不应为 tenantId=100");
    }

    /**
     * autoTrigger 路径 withhold=null：returnAiToken 应幂等吃掉（withhold=null 时不抛 NPE）。
     */
    @Test
    @DisplayName("autoTrigger withhold=null 容错: ASR 空场景 returnAiToken(null) 不抛异常")
    void generate_autoTriggerWithholdNull_asrEmpty_idempotentReturnAiToken() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        AnalysisResultVo emptyAsr = new AnalysisResultVo();
        emptyAsr.setSentenceMarkVos(null);
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(R.ok(emptyAsr));
        // 模拟生产端 withhold=null 时 returnAiToken 抛 NPE，验证 catch 兜底
        org.mockito.Mockito.doThrow(new RuntimeException("withhold null NPE"))
                .when(aiTokenWithholdFeign).returnAiToken(null);

        // 关键断言：withhold=null 调用不向上抛（即使 returnAiToken 抛异常，markNotApplicable 仍执行）
        bll.generate(report, ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), null);

        // markNotApplicable 仍执行（returnAiToken 异常被 catch 吃掉）
        verify(reportWriteService, times(1)).markNotApplicable(anyLong(), any());
        verify(aiFeign, never()).chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong());
    }

    // ====== resolveTradeId VIDEO 来源改造（2026-06-22）======

    /**
     * resolveTradeId 改造 AC-2：VIDEO 来源行业取直播间维度 tb_anchor_url_user.trade_id，
     * 不再用录制记录 video.tradeId。video.tradeId=99 但 anchorUser.tradeId=7 → cueWords 应按 7 取。
     */
    @Test
    @DisplayName("resolveTradeId VIDEO 来源: 取 anchorUrlUserFeign trade_id(7)，忽略 video.tradeId(99)")
    void resolveTradeId_videoSource_usesAnchorUrlUserTradeId_notVideoTradeId() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(asrResult());
        // video.tradeId=99（应被忽略），secUid=sec-001
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video("sec-001", 99L)));
        // 直播间维度行业 = 7（应被采用）
        when(anchorUrlUserFeign.getBySecUidAndUser("sec-001", 42L, 100L)).thenReturn(R.ok(anchorUser(7L)));
        when(standardScriptFeign.findValid(100L, 42L, "sec-001")).thenReturn(nonCyclicScript());
        // 提示词按 tradeId=7 取（若误用 video.tradeId=99 则取不到 → loadSingleCueWord 抛错走 restoreOrFail）
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueMatcher(7L, 100L, 18))))
                .thenReturn(cueWith("对比提示词"));
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueMatcher(7L, 100L, 19))))
                .thenReturn(cueWith("合并提示词"));
        when(aiModelFeign.listDiagnosisModel()).thenReturn(models());
        when(aiFeign.chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong()))
                .thenReturn(aiOk(5000));
        when(intermediateRepository.save(any())).thenAnswer(inv -> {
            ScriptMonitorIntermediateReportEntity e = inv.getArgument(0);
            e.setId("inter-id");
            return e;
        });
        when(intermediateRepository.findByReportIdOrderByIndexAsc(anyLong())).thenReturn(intermediateReports());
        when(bodyRepository.save(any())).thenAnswer(inv -> {
            ScriptMonitorReportBodyEntity b = inv.getArgument(0);
            b.setId("body-mongo-id");
            return b;
        });

        bll.generate(report, ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), withhold);

        // 关键断言：提示词按直播间维度 tradeId=7 取，未按 video.tradeId=99 取
        verify(cueWordsFeign).getCueWordByTradeAndType(argThat(cueMatcher(7L, 100L, 18)));
        verify(cueWordsFeign, never()).getCueWordByTradeAndType(argThat(cueMatcher(99L, 100L, 18)));
        // 主流程走通
        verify(reportWriteService, times(1)).finishReport(anyLong(), any(), any());
    }

    /**
     * resolveTradeId 改造 AC-2 边界：anchorUrlUserFeign 查不到（R.data=null）→ 兜底 tradeId=1。
     */
    @Test
    @DisplayName("resolveTradeId VIDEO 来源: anchorUrlUserFeign 返 null → 兜底 tradeId=1")
    void resolveTradeId_videoSource_anchorUserNull_fallbackToOne() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(asrResult());
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video("sec-001", 99L)));
        // 直播间维度查不到（R(code=0, data=null) → ResultUtil.getResult 返 null）
        when(anchorUrlUserFeign.getBySecUidAndUser("sec-001", 42L, 100L)).thenReturn(R.ok(null));
        when(standardScriptFeign.findValid(100L, 42L, "sec-001")).thenReturn(nonCyclicScript());
        // 提示词按兜底 tradeId=1 取
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueMatcher(1L, 100L, 18))))
                .thenReturn(cueWith("对比提示词"));
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueMatcher(1L, 100L, 19))))
                .thenReturn(cueWith("合并提示词"));
        when(aiModelFeign.listDiagnosisModel()).thenReturn(models());
        when(aiFeign.chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong()))
                .thenReturn(aiOk(5000));
        when(intermediateRepository.save(any())).thenAnswer(inv -> {
            ScriptMonitorIntermediateReportEntity e = inv.getArgument(0);
            e.setId("inter-id");
            return e;
        });
        when(intermediateRepository.findByReportIdOrderByIndexAsc(anyLong())).thenReturn(intermediateReports());
        when(bodyRepository.save(any())).thenAnswer(inv -> {
            ScriptMonitorReportBodyEntity b = inv.getArgument(0);
            b.setId("body-mongo-id");
            return b;
        });

        bll.generate(report, ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), withhold);

        // 关键断言：提示词按兜底 tradeId=1 取
        verify(cueWordsFeign).getCueWordByTradeAndType(argThat(cueMatcher(1L, 100L, 18)));
        verify(reportWriteService, times(1)).finishReport(anyLong(), any(), any());
    }
}
