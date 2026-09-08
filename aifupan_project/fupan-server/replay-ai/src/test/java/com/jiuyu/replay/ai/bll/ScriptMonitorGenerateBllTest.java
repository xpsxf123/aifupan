package com.jiuyu.replay.ai.bll;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.jiuyu.replay.ai.entity.ScriptMonitorIntermediateReportEntity;
import com.jiuyu.replay.ai.entity.ScriptMonitorReportBodyEntity;
import com.jiuyu.replay.ai.entity.ScriptMonitorReportEntity;
import com.jiuyu.replay.ai.repository.mongo.ScriptMonitorIntermediateReportRepository;
import com.jiuyu.replay.ai.repository.mongo.ScriptMonitorReportBodyRepository;
import com.jiuyu.replay.ai.repository.service.ScriptMonitorReportService;
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
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.order.RedisWithholdVo;
import com.jiuyu.replay.generic.vo.words.AnalysisResultVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.generic.vo.words.CueWordsInfoVo;
import com.jiuyu.replay.generic.vo.words.SentenceMarkVo;
import com.jiuyu.replay.generic.vo.words.UploadFileSimpleInfoVo;
import com.jiuyu.replay.generic.vo.words.WordListItemVo;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ScriptMonitorGenerateBll 单测（B4-2b 异步生成主流程）。
 *
 * <p>覆盖：AC-1 happy path、AC-3 旧成功报告失败回滚、AC-4 Token 不足（预扣失败）、
 * AC-6 ASR 为空（NOT_APPLICABLE）、AI 调用中途失败补偿（returnAiToken）、
 * AC-C1 settleAiToken 使用 userId、AC-C2 loadCueWords 含 problem 字段、
 * AC-C3 事务写入走独立 Bean。</p>
 *
 * @author beta
 * @date 2026-05-30
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScriptMonitorGenerateBllTest {

    @Mock
    private ScriptMonitorReportService reportService;
    @Mock
    private ScriptMonitorIntermediateReportRepository intermediateRepository;
    @Mock
    private ScriptMonitorReportBodyRepository bodyRepository;
    @Mock
    private SensitiveWordsFeign sensitiveWordsFeign;
    @Mock
    private CueWordsFeign cueWordsFeign;
    @Mock
    private AnchorVideoFeign anchorVideoFeign;
    @Mock
    private AnchorUrlUserFeign anchorUrlUserFeign;
    @Mock
    private AiModelFeign aiModelFeign;
    @Mock
    private AiChatFeign aiFeign;
    @Mock
    private AiTokenWithholdFeign aiTokenWithholdFeign;
    /**
     * 独立 Bean（C3 修补：替代 self-call @Transactional 失效问题）
     */
    @Mock
    private ScriptMonitorReportWriteService reportWriteService;
    /**
     * systemKv 取按场景配置的 AI 模型 code（方案 B+：6 个 key，Mockito 默认 getByKey 返 null 自动走兜底）
     */
    @Mock
    private com.jiuyu.replay.common.producer.SystemKvProducer systemKvProducer;

    @InjectMocks
    private ScriptMonitorGenerateBll bll;

    private RedisWithholdVo withhold;

    @BeforeEach
    void setUp() {
        withhold = new RedisWithholdVo();
        withhold.setWithholdId("w1");
        withhold.setRedisId(1L);

        // 默认 updateById 无操作
        when(reportService.updateById(any())).thenReturn(true);
        // 默认写入 service 方法无操作
        doNothing().when(reportWriteService).finishReport(anyLong(), any(), any());
        doNothing().when(reportWriteService).restoreOrFail(anyLong(), any(), any(), any(), any());
        doNothing().when(reportWriteService).markNotApplicable(anyLong(), any());
    }

    // ====== helpers ======

    /**
     * 构造一条报告行。
     *
     * @param status 报告状态
     * @return 报告行
     */
    private ScriptMonitorReportEntity report(Integer status) {
        ScriptMonitorReportEntity r = new ScriptMonitorReportEntity();
        r.setId(10L);
        r.setTenantId(100L);
        r.setUserId(42L);
        r.setSourceType(0);
        r.setSourceId("v1");
        r.setMonitorType(0);
        r.setStatus(status);
        r.setIsDeleted(0);
        return r;
    }

    /**
     * 构造已有成功报告（含旧摘要字段）。
     *
     * @return 报告行（status=2 + 旧字段）
     */
    private ScriptMonitorReportEntity generatedReport() {
        ScriptMonitorReportEntity r = report(ScriptMonitorStatusEnum.GENERATED.getCode());
        r.setReportBodyId("old-mongo-id");
        r.setSummaryJson("{\"crashCount\":5,\"slackCount\":3,\"brandDamageCount\":2,\"afterSalesCount\":1}");
        return r;
    }

    /**
     * 构造 ASR 返回（2 个段落）。
     *
     * @return R<AnalysisResultVo>
     */
    private R<AnalysisResultVo> asrResult() {
        SentenceMarkVo s1 = new SentenceMarkVo();
        s1.setCurrentSort(1);
        s1.setContent("欢迎来到直播间");
        SentenceMarkVo s2 = new SentenceMarkVo();
        s2.setCurrentSort(2);
        s2.setContent("今天为大家带来超值好物");
        AnalysisResultVo vo = new AnalysisResultVo();
        vo.setSentenceMarkVos(List.of(s1, s2));
        return R.ok(vo);
    }

    /**
     * 构造质检生成提示词（cueType=16）的单条提示词（含 problem 字段）。
     */
    private CueWordsInfoVo generateCue() {
        CueWordsInfoVo g = new CueWordsInfoVo();
        g.setId(1L);
        g.setSort(1);
        g.setProblem("你是话术质检专家，请分析以下话术");
        return g;
    }

    /**
     * 构造质检合并提示词（cueType=17）的单条提示词。
     */
    private CueWordsInfoVo mergeCue() {
        CueWordsInfoVo m = new CueWordsInfoVo();
        m.setId(2L);
        m.setSort(1);
        m.setProblem("请合并以下3份质检报告");
        return m;
    }

    /**
     * 构造录制视频信息（带 secUid + tradeId）。
     *
     * <p>2026-06-22 改造后 resolveTradeId VIDEO 分支按 video.secUid 走 anchorUrlUserFeign，
     * 故 video 必须带 secUid（非空）；video.tradeId 已不再被业务读取（保留仅为字段完整）。</p>
     */
    private AnchorVideoInfoVo videoWithTrade(Long tradeId) {
        AnchorVideoInfoVo v = new AnchorVideoInfoVo();
        v.setVideoId("v1");
        v.setSecUid("sec-1");
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
     * 构造带词级时间戳 items 的 ASR 返回（2 段，毫秒偏移按视频内时间）。
     *
     * <p>段 1：[3000ms, 11000ms] → 03:03-03:11（DateTime 默认 +8 时区解析 epoch ms，
     * 经 offset -8 hour 即得 00:00:03 ~ 00:00:11）。
     * 段 2：[12000ms, 21000ms] → 00:00:12 ~ 00:00:21。</p>
     */
    private R<AnalysisResultVo> asrResultWithItems() {
        WordListItemVo s1First = new WordListItemVo();
        s1First.setStartTime(3000L);
        WordListItemVo s1Last = new WordListItemVo();
        s1Last.setEndTime(11000L);
        SentenceMarkVo s1 = new SentenceMarkVo();
        s1.setCurrentSort(1);
        s1.setContent("欢迎来到直播间小伙伴们");
        s1.setItems(List.of(s1First, s1Last));

        WordListItemVo s2First = new WordListItemVo();
        s2First.setStartTime(12000L);
        WordListItemVo s2Last = new WordListItemVo();
        s2Last.setEndTime(21000L);
        SentenceMarkVo s2 = new SentenceMarkVo();
        s2.setCurrentSort(2);
        s2.setContent("今天为大家带来超值好物");
        s2.setItems(List.of(s2First, s2Last));

        AnalysisResultVo vo = new AnalysisResultVo();
        vo.setSentenceMarkVos(List.of(s1, s2));
        return R.ok(vo);
    }

    /**
     * 构造 items 非空但 startTime/endTime 全 null 的 ASR（验证 AC-2 回退分支）。
     *
     * <p>线上罕见但理论可能：ASR 服务返回 items 词项但漏填时间戳。生产代码 null 守卫
     * 应让 timeTag 保持空字符串，拼接退化为原 `[段落 N] content` 格式。</p>
     */
    private R<AnalysisResultVo> asrResultWithItemsButNoTime() {
        WordListItemVo wordWithoutTime = new WordListItemVo();
        wordWithoutTime.setWord("欢迎"); // 仅文本无时间
        SentenceMarkVo s = new SentenceMarkVo();
        s.setCurrentSort(1);
        s.setContent("欢迎来到直播间");
        s.setItems(List.of(wordWithoutTime));

        AnalysisResultVo vo = new AnalysisResultVo();
        vo.setSentenceMarkVos(List.of(s));
        return R.ok(vo);
    }

    /**
     * 构造 AI 模型列表。
     *
     * @return List<AiModelInfoVo>
     */
    private List<AiModelInfoVo> models() {
        AiModelInfoVo m = new AiModelInfoVo();
        m.setModelCode("doubao-pro");
        m.setModelName("豆包Pro");
        return List.of(m);
    }

    /**
     * 构造 AI 返回结果（合并报告 Markdown，含 &lt;summary&gt; 标签包裹的摘要文本）。
     *
     * @param totalTokens Token 消耗
     * @return AiReturnDataVo
     */
    private AiReturnDataVo aiResult(int totalTokens) {
        AiReturnDataVo r = new AiReturnDataVo();
        r.setStatus(0);
        r.setContent("# 质检报告\n\n## 详情\n主播表现分析...\n\n<aifupan-data-block>整体评估: 主播表现不错</aifupan-data-block>\n\n## 改进建议\n建议1...");
        r.setRequestId("req-" + totalTokens);
        r.setTotalTokens(totalTokens);
        return r;
    }

    /**
     * 构造 AI 返回结果（合法 Markdown 但缺 &lt;summary&gt; 标签，用于测 fallback null 路径）。
     *
     * @param totalTokens Token 消耗
     * @return AiReturnDataVo
     */
    private AiReturnDataVo aiResultNoSummary(int totalTokens) {
        AiReturnDataVo r = new AiReturnDataVo();
        r.setStatus(0);
        r.setContent("# 质检报告\n\n## 详情\n无摘要标签的 markdown\n\n## 建议\n建议1...");
        r.setRequestId("req-" + totalTokens);
        r.setTotalTokens(totalTokens);
        return r;
    }

    /**
     * 构造 MongoDB 中间报告列表（3 条）。
     *
     * @return List<ScriptMonitorIntermediateReportEntity>
     */
    private List<ScriptMonitorIntermediateReportEntity> intermediateReports() {
        List<ScriptMonitorIntermediateReportEntity> list = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ScriptMonitorIntermediateReportEntity e = new ScriptMonitorIntermediateReportEntity();
            e.setId("inter-" + i);
            e.setReportId(10L);
            e.setIndex(i);
            e.setContent("<html>中间报告" + i + "</html>");
            list.add(e);
        }
        return list;
    }

    /**
     * 构造 MongoDB 报告正文（保存后带 id）。
     *
     * @return ScriptMonitorReportBodyEntity
     */
    private ScriptMonitorReportBodyEntity savedBody() {
        ScriptMonitorReportBodyEntity b = new ScriptMonitorReportBodyEntity();
        b.setId("body-mongo-id");
        b.setReportId(10L);
        b.setContent("<html>最终质检报告</html>");
        return b;
    }

    /**
     * 设置 happy path mock（ASR + 行业解析 + 提示词 + 模型 + 4次AI调用 + MongoDB）。
     *
     * <p>2026-06-04 改造：提示词按 tb_anchor_video.trade_id 取（cueType=16/17），
     * 走新 SPI getCueWordByTradeAndType。tradeId=5 用作单测桩值（业务无具体含义）。</p>
     *
     * @param report 报告行
     */
    private void setupHappyPath(ScriptMonitorReportEntity report) {
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(asrResult());
        // 直播间所属行业解析：tb_anchor_video.trade_id=5
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(videoWithTrade(5L)));
        // 行业取直播间维度 tb_anchor_url_user.trade_id（2026-06-22 改造）：按 secUid+userId+tenantId 取 → 5L
        when(anchorUrlUserFeign.getBySecUidAndUser("sec-1", 42L, 100L)).thenReturn(R.ok(anchorUser(5L)));
        // 按 tradeId+cueType 取提示词（含 problem 字段）
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueWordsMatcher(5L, 100L, 16)))).thenReturn(generateCue());
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueWordsMatcher(5L, 100L, 17)))).thenReturn(mergeCue());
        when(aiModelFeign.listDiagnosisModel()).thenReturn(models());
        // 3 次中间报告 + 1 次合并 = 4 次 AI 调用
        when(aiFeign.chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong()))
                .thenReturn(aiResult(10000));
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

    /**
     * 构造 happy path mock，但 merge 阶段返回指定 Markdown 内容（用于测 buildSummary 提取规则）。
     *
     * @param report        报告行
     * @param mergeMarkdown merge 阶段 AI 返回的 markdown 全文（含或不含 &lt;summary&gt; 标签）
     */
    private void setupHappyPathWithMergeContent(ScriptMonitorReportEntity report, String mergeMarkdown) {
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(asrResult());
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(videoWithTrade(5L)));
        // 行业取直播间维度 tb_anchor_url_user.trade_id（2026-06-22 改造）：按 secUid+userId+tenantId 取 → 5L
        when(anchorUrlUserFeign.getBySecUidAndUser("sec-1", 42L, 100L)).thenReturn(R.ok(anchorUser(5L)));
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueWordsMatcher(5L, 100L, 16)))).thenReturn(generateCue());
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueWordsMatcher(5L, 100L, 17)))).thenReturn(mergeCue());
        when(aiModelFeign.listDiagnosisModel()).thenReturn(models());

        AiReturnDataVo mergeR = new AiReturnDataVo();
        mergeR.setStatus(0);
        mergeR.setContent(mergeMarkdown);
        mergeR.setRequestId("merge-req");
        mergeR.setTotalTokens(3000);

        when(aiFeign.chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong()))
                .thenReturn(aiResult(5000))   // intermediate 1
                .thenReturn(aiResult(5000))   // intermediate 2
                .thenReturn(aiResult(5000))   // intermediate 3
                .thenReturn(mergeR);          // merge with custom markdown
        doNothing().when(intermediateRepository).deleteByReportId(anyLong());
        when(intermediateRepository.save(any())).thenAnswer(inv -> {
            ScriptMonitorIntermediateReportEntity e = inv.getArgument(0);
            e.setId("inter-id");
            return e;
        });
        when(intermediateRepository.findByReportIdOrderByIndexAsc(anyLong())).thenReturn(intermediateReports());
        doNothing().when(bodyRepository).deleteByReportId(anyLong());
        when(bodyRepository.save(any())).thenAnswer(inv -> {
            ScriptMonitorReportBodyEntity b = inv.getArgument(0);
            b.setId("body-mongo-id");
            return b;
        });
        doNothing().when(aiTokenWithholdFeign).settleAiToken(any(), anyLong(), any(), any(AiTokenUseRecordBo.class));
    }

    /**
     * 构造 CueWordsQueryBo 的 ArgumentMatcher：匹配 (tradeId, tenantId, cueType) 三元组。
     *
     * <p>抽取自 8 处 {@code when(cueWordsFeign.getCueWordByTradeAndType(argThat(bo -> ...)))} stub，
     * 避免 lambda 单行 240+ 字符。tenantId 用 100L（与 {@link #report} 工厂一致），如未来需要
     * 不同 tenantId 直接传参覆盖。</p>
     *
     * @param tradeId  期望 bo.tradeId
     * @param tenantId 期望 bo.tenantId
     * @param cueType  期望 bo.cueType
     * @return ArgumentMatcher 实例
     */
    private static ArgumentMatcher<CueWordsQueryBo> cueWordsMatcher(long tradeId, long tenantId, int cueType) {
        return bo -> bo != null
                && Long.valueOf(tradeId).equals(bo.getTradeId())
                && Long.valueOf(tenantId).equals(bo.getTenantId())
                && Integer.valueOf(cueType).equals(bo.getCueType());
    }

    /**
     * 触发 bll.generate 并捕获 finishReport 第 3 参数（summaryJson）。
     *
     * @param report 报告行
     * @return finishReport 接收的 summaryJson 值
     */
    private String captureFinishReportSummary(ScriptMonitorReportEntity report) {
        bll.generate(report, ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), withhold);
        ArgumentCaptor<String> summaryCaptor = ArgumentCaptor.forClass(String.class);
        verify(reportWriteService, times(1))
                .finishReport(anyLong(), any(), summaryCaptor.capture());
        return summaryCaptor.getValue();
    }

    // ====== 测试用例 ======

    @Test
    @DisplayName("AC-1: happy path — 生成 3 中间报告 + 1 合并 + finishReport + settleAiToken")
    void testGenerate_happyPath() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        setupHappyPath(report);

        bll.generate(report, ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), withhold);

        // AI 被调用 4 次（3 中间 + 1 合并）
        verify(aiFeign, times(4)).chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong());
        // 中间报告 save 3 次
        verify(intermediateRepository, times(3)).save(any());
        // 正文 save 1 次
        verify(bodyRepository, times(1)).save(any());
        // finishReport 走独立 Bean（C3 修补）
        verify(reportWriteService, times(1)).finishReport(anyLong(), any(), any());
        // settleAiToken 1 次
        verify(aiTokenWithholdFeign, times(1)).settleAiToken(any(), anyLong(), any(), any());
        // returnAiToken 不被调用
        verify(aiTokenWithholdFeign, never()).returnAiToken(any());
    }

    @Test
    @DisplayName("ASR 段落含 items 时间戳 → 拼接含 [段落 N HH:mm:ss-HH:mm:ss] 时间锚点")
    void testGenerate_asrWithTimeStamps_appendsTimeTagToParagraph() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        setupHappyPath(report);
        // 覆盖 ASR，改为含 items 词级时间戳的数据
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(asrResultWithItems());

        bll.generate(report, ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), withhold);

        // 捕获 chatCompletion 收到的 AiMessageBo，验证 user content 含时间标签
        ArgumentCaptor<AiMessageBo> paramsCaptor = ArgumentCaptor.forClass(AiMessageBo.class);
        verify(aiFeign, times(4)).chatCompletion(any(AiModelBo.class), paramsCaptor.capture(), anyLong());

        // 前 3 次（intermediate）user content 是同一份 ASR 拼接文本
        AiMessageBo firstCall = paramsCaptor.getAllValues().get(0);
        @SuppressWarnings("unchecked")
        String userText = (String) firstCall.getUser().get(0).get("text");

        assertTrue(userText.contains("[段落 1 时间 00:00:03-00:00:11]"),
                "段 1 应含时间锚点 [段落 1 时间 00:00:03-00:00:11]，实际：" + userText);
        assertTrue(userText.contains("[段落 2 时间 00:00:12-00:00:21]"),
                "段 2 应含时间锚点 [段落 2 时间 00:00:12-00:00:21]，实际：" + userText);
        assertTrue(userText.contains("欢迎来到直播间小伙伴们"), "段 1 文本应保留");
        assertTrue(userText.contains("今天为大家带来超值好物"), "段 2 文本应保留");
    }

    @Test
    @DisplayName("AC-2 回退: items 非空但 startTime/endTime=null → 退化为原 [段落 N] content 格式（无时间锚点）")
    void testGenerate_asrItemsWithoutTime_fallbackToPlainTag() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        setupHappyPath(report);
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(asrResultWithItemsButNoTime());

        bll.generate(report, ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), withhold);

        ArgumentCaptor<AiMessageBo> paramsCaptor = ArgumentCaptor.forClass(AiMessageBo.class);
        verify(aiFeign, times(4)).chatCompletion(any(AiModelBo.class), paramsCaptor.capture(), anyLong());
        @SuppressWarnings("unchecked")
        String userText = (String) paramsCaptor.getAllValues().get(0).getUser().get(0).get("text");

        // 退化为原格式
        assertTrue(userText.contains("[段落 1] 欢迎来到直播间"),
                "items 无时间戳应退化为 [段落 1] content 原格式，实际：" + userText);
        // 不应出现"时间"二字
        assertTrue(!userText.contains("时间"),
                "items 无时间戳时不应拼接时间锚点，实际：" + userText);
    }

    @Test
    @DisplayName("AC-3a: 文件源 sourceType=1 → 走 sensitiveWordsFeign.getUploadFileInfo 取 tb_upload_file.trade_id；不触发 anchorVideoFeign（DEBT-019 修复）")
    void testGenerate_fileSource_useUploadFileTradeId() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        report.setSourceType(1);
        report.setSourceId("f1");

        // 关键 mock：文件源返回 tradeId=7（业务行业 ID，非兜底 1L）
        UploadFileSimpleInfoVo fileInfo = new UploadFileSimpleInfoVo();
        fileInfo.setFileId("f1");
        fileInfo.setTenantId(100L);
        fileInfo.setTradeId(7L);
        when(sensitiveWordsFeign.getUploadFileInfo("f1")).thenReturn(R.ok(fileInfo));

        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(asrResult());
        // 关键断言：按 tradeId=7（来自 tb_upload_file，非兜底）取提示词
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueWordsMatcher(7L, 100L, 16)))).thenReturn(generateCue());
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueWordsMatcher(7L, 100L, 17)))).thenReturn(mergeCue());
        when(aiModelFeign.listDiagnosisModel()).thenReturn(models());
        when(aiFeign.chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong()))
                .thenReturn(aiResult(10000));
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

        // 关键断言：文件源不调用 anchorVideoFeign
        verify(anchorVideoFeign, never()).GetByVideoId(any());
        // 走通主流程（cueWordsFeign mock 已锁 tradeId=7，未命中则取不到提示词，抛业务异常 returnAiToken）
        verify(reportWriteService, times(1)).finishReport(anyLong(), any(), any());
        verify(aiTokenWithholdFeign, never()).returnAiToken(any());
    }

    @Test
    @DisplayName("AC-3c: 文件源 sourceType=1，getUploadFileInfo 返回 fileInfo=null（fileId 已删除/不存在）→ 兜底 tradeId=1（DEBT-019 边界）")
    void testGenerate_fileSource_fileInfoNull_fallbackToOne() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        report.setSourceType(1);
        report.setSourceId("f-deleted");

        // 关键 mock：SPI 返回 R(code=0, data=null)，经 ResultUtil.getResult 返回 null
        // 触发 resolveTradeId 文件源分支 fileInfo==null 守卫，落兜底 1L
        when(sensitiveWordsFeign.getUploadFileInfo("f-deleted")).thenReturn(R.ok(null));

        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(asrResult());
        // 关键断言：fileInfo 为 null 时按 tradeId=1 兜底取提示词
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueWordsMatcher(1L, 100L, 16)))).thenReturn(generateCue());
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueWordsMatcher(1L, 100L, 17)))).thenReturn(mergeCue());
        when(aiModelFeign.listDiagnosisModel()).thenReturn(models());
        when(aiFeign.chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong()))
                .thenReturn(aiResult(10000));
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

        // 文件源不调用 anchorVideoFeign
        verify(anchorVideoFeign, never()).GetByVideoId(any());
        // 主流程走通（cueWordsFeign mock 已锁 tradeId=1，未命中则取不到提示词，抛业务异常 returnAiToken）
        verify(reportWriteService, times(1)).finishReport(anyLong(), any(), any());
        verify(aiTokenWithholdFeign, never()).returnAiToken(any());
    }

    @Test
    @DisplayName("AC-3b: 文件源 sourceType=1，tb_upload_file.trade_id=null → 兜底 tradeId=1（DEBT-019 修复）")
    void testGenerate_fileSource_tradeIdNull_fallbackToOne() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        report.setSourceType(1);
        report.setSourceId("f1");

        // 关键 mock：文件源返回 tradeId=null（用户上传时未指定行业）
        UploadFileSimpleInfoVo fileInfo = new UploadFileSimpleInfoVo();
        fileInfo.setFileId("f1");
        fileInfo.setTenantId(100L);
        fileInfo.setTradeId(null);
        when(sensitiveWordsFeign.getUploadFileInfo("f1")).thenReturn(R.ok(fileInfo));

        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(asrResult());
        // 关键断言：按 tradeId=1 兜底取提示词
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueWordsMatcher(1L, 100L, 16)))).thenReturn(generateCue());
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueWordsMatcher(1L, 100L, 17)))).thenReturn(mergeCue());
        when(aiModelFeign.listDiagnosisModel()).thenReturn(models());
        when(aiFeign.chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong()))
                .thenReturn(aiResult(10000));
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

        verify(anchorVideoFeign, never()).GetByVideoId(any());
        verify(reportWriteService, times(1)).finishReport(anyLong(), any(), any());
        verify(aiTokenWithholdFeign, never()).returnAiToken(any());
    }

    @Test
    @DisplayName("AC-C1: settleAiToken_uses_user_id_not_tenant_id — 第二参数为 userId(42L)，非 tenantId(100L)")
    void settleAiToken_uses_user_id_not_tenant_id() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        // userId=42, tenantId=100（不同，确保断言有效）
        report.setUserId(42L);
        report.setTenantId(100L);
        setupHappyPath(report);

        bll.generate(report, ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), withhold);

        // 捕获 settleAiToken 的第二个参数（userId）
        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(aiTokenWithholdFeign, times(1))
                .settleAiToken(any(), userIdCaptor.capture(), any(), any());
        assertEquals(42L, userIdCaptor.getValue(),
                "settleAiToken 应传 userId=42，不应传 tenantId=100");
    }

    @Test
    @DisplayName("AC-C2: loadCueWords_returns_prompts_with_problem_field — problem 非 null")
    void loadCueWords_returns_prompts_with_problem_field() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        setupHappyPath(report);

        bll.generate(report, ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), withhold);

        // 验证 AI 被调用且传入了 system prompt（来自 problem 字段）
        ArgumentCaptor<AiMessageBo> msgCaptor = ArgumentCaptor.forClass(AiMessageBo.class);
        verify(aiFeign, times(4)).chatCompletion(any(AiModelBo.class), msgCaptor.capture(), anyLong());
        // 第一次调用的 system 不为 null（来自 sort=1 的 problem 字段）
        AiMessageBo firstCall = msgCaptor.getAllValues().get(0);
        assertNotNull(firstCall.getSystem(), "problem 字段不应为 null，说明 loadCueWords 正确返回含 problem 的提示词");
    }

    @Test
    @DisplayName("AC-C3: finishReport 走独立 Bean（reportWriteService），非 self-call @Transactional")
    void finishReport_calls_reportWriteService_not_self() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        setupHappyPath(report);

        bll.generate(report, ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), withhold);

        // finishReport 必须通过注入的 reportWriteService 调用（C3 修补保证 @Transactional 生效）
        verify(reportWriteService, times(1))
                .finishReport(anyLong(), any(), any());
        // reportService.updateById 不被直接调用（事务委托给 reportWriteService）
        verify(reportService, never()).updateById(any());
    }

    @Test
    @DisplayName("AC-C3: restoreOrFail 走独立 Bean — 生成失败时通过 reportWriteService.restoreOrFail")
    void restoreOrFail_calls_reportWriteService() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());

        // 让 ASR 调用抛异常，模拟生成失败
        when(sensitiveWordsFeign.getAnalysisData(any(), any()))
                .thenThrow(new RuntimeException("ASR 服务不可用"));
        doNothing().when(aiTokenWithholdFeign).returnAiToken(any());

        bll.generate(report, ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), withhold);

        verify(aiTokenWithholdFeign, times(1)).returnAiToken(withhold);
        // restoreOrFail 走独立 Bean（C3 修补）
        verify(reportWriteService, times(1))
                .restoreOrFail(anyLong(), any(), any(), any(), any());
        verify(reportService, never()).updateById(any());
    }

    @Test
    @DisplayName("AC-3: 旧成功报告(status=2)重新生成失败 → restoreOrFail 传 originalStatus=2")
    void testGenerate_failedWhenPreviouslyGenerated_restoresOldSuccess() {
        ScriptMonitorReportEntity report = generatedReport();
        report.setStatus(ScriptMonitorStatusEnum.GENERATING.getCode());

        when(sensitiveWordsFeign.getAnalysisData(any(), any()))
                .thenThrow(new RuntimeException("ASR 服务不可用"));
        doNothing().when(aiTokenWithholdFeign).returnAiToken(any());

        bll.generate(report, ScriptMonitorStatusEnum.GENERATED.getCode(), withhold);

        verify(aiTokenWithholdFeign, times(1)).returnAiToken(withhold);
        // 验证 restoreOrFail 被调用，第二参数（originalStatus）= GENERATED.code
        ArgumentCaptor<Integer> originalStatusCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(reportWriteService, times(1))
                .restoreOrFail(anyLong(), originalStatusCaptor.capture(), any(), any(), any());
        assertEquals(ScriptMonitorStatusEnum.GENERATED.getCode(), originalStatusCaptor.getValue());
    }

    @Test
    @DisplayName("AC-4: 从未生成报告重新生成失败 → restoreOrFail 传 originalStatus=NOT_GENERATED")
    void testGenerate_failedWhenNotGenerated_marksGenerateFailed() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());

        when(sensitiveWordsFeign.getAnalysisData(any(), any()))
                .thenThrow(new RuntimeException("超时"));
        doNothing().when(aiTokenWithholdFeign).returnAiToken(any());

        bll.generate(report, ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), withhold);

        verify(aiTokenWithholdFeign, times(1)).returnAiToken(withhold);
        ArgumentCaptor<Integer> originalStatusCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(reportWriteService, times(1))
                .restoreOrFail(anyLong(), originalStatusCaptor.capture(), any(), any(), any());
        assertEquals(ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), originalStatusCaptor.getValue());
    }

    @Test
    @DisplayName("AC-6: ASR 为空 → markNotApplicable + returnAiToken")
    void testGenerate_asrEmpty_marksNotApplicable() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());

        AnalysisResultVo emptyAsr = new AnalysisResultVo();
        emptyAsr.setSentenceMarkVos(null);
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(R.ok(emptyAsr));
        doNothing().when(aiTokenWithholdFeign).returnAiToken(any());

        bll.generate(report, ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), withhold);

        // returnAiToken 被调用
        verify(aiTokenWithholdFeign, times(1)).returnAiToken(withhold);
        // AI 不被调用
        verify(aiFeign, never()).chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong());
        // markNotApplicable 走独立 Bean（C3 修补）
        verify(reportWriteService, times(1)).markNotApplicable(anyLong(), any());
    }

    @Test
    @DisplayName("AC-7: happy path — buildSummary 提取 <aifupan-data-block> 标签内 markdown 文本，finishReport 收到 trim 后内容")
    void testGenerate_happyPath_extractsSummaryFromMarkdown() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        setupHappyPath(report);

        String capturedSummary = captureFinishReportSummary(report);

        assertNotNull(capturedSummary, "合法 markdown 含 <aifupan-data-block> 标签时 summaryJson 不应为 null");
        assertEquals("整体评估: 主播表现不错", capturedSummary,
                "应提取 <aifupan-data-block> 标签内文本（trim 后），不应含 markdown 其他段");
    }

    @Test
    @DisplayName("AC-8: Markdown 无 <aifupan-data-block> 标签 → summaryJson=null，主流程不中断")
    void testBuildSummary_noSummaryTag_setNull() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        setupHappyPathWithMergeContent(report,
                "# 报告\n## 详情\n无摘要标签的 markdown\n## 建议\n建议1");

        String captured = captureFinishReportSummary(report);

        assertEquals(null, captured,
                "Markdown 无 <aifupan-data-block> 标签时 buildSummary 应置 summaryJson=null");
        // 主流程不中断：不调 restoreOrFail / returnAiToken
        verify(reportWriteService, never()).restoreOrFail(anyLong(), any(), any(), any(), any());
        verify(aiTokenWithholdFeign, never()).returnAiToken(any());
    }

    @Test
    @DisplayName("AC-9: 标签大小写不敏感 — <AIFUPAN-DATA-BLOCK> / <Aifupan-Data-Block> 均能提取")
    void testBuildSummary_caseInsensitive() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        setupHappyPathWithMergeContent(report,
                "# 报告\n\n<AIFUPAN-DATA-BLOCK>大写标签内容</AIFUPAN-DATA-BLOCK>");
        assertEquals("大写标签内容", captureFinishReportSummary(report),
                "<AIFUPAN-DATA-BLOCK> 大写标签应能被 CASE_INSENSITIVE pattern 提取");
    }

    @Test
    @DisplayName("AC-10: 跨行内容应完整提取（DOTALL 生效）")
    void testBuildSummary_multilineContent() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        setupHappyPathWithMergeContent(report,
                "# 报告\n\n<aifupan-data-block>第一行\n第二行\n第三行</aifupan-data-block>");
        assertEquals("第一行\n第二行\n第三行", captureFinishReportSummary(report),
                "跨行 markdown 内容应能被 DOTALL pattern 完整提取（含 \\n）");
    }

    @Test
    @DisplayName("AC-11: 多个 <aifupan-data-block> 标签 → 取第一个（非贪婪 .*? 生效）")
    void testBuildSummary_multipleTags_takesFirst() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        setupHappyPathWithMergeContent(report,
                "<aifupan-data-block>第一个</aifupan-data-block>\n\n# 中间正文\n\n<aifupan-data-block>第二个</aifupan-data-block>");
        assertEquals("第一个", captureFinishReportSummary(report),
                "出现多个 <aifupan-data-block> 标签时应取首个（matcher.find() 首次命中）");
    }

    @Test
    @DisplayName("AC-12: 提取后 trim 首尾空白（含换行 / 空格 / 制表符）")
    void testBuildSummary_trimsWhitespace() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        setupHappyPathWithMergeContent(report,
                "# 报告\n\n<aifupan-data-block>   \n  实际内容  \n  </aifupan-data-block>");
        assertEquals("实际内容", captureFinishReportSummary(report),
                "提取后应 .trim() 去首尾空白");
    }

    @Test
    @DisplayName("AC-13: mergedAiContent=null → buildSummary 走 null 守卫分支，主流程不中断")
    void testBuildSummary_nullContent_setNull() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        setupHappyPathWithMergeContent(report, null);

        String captured = captureFinishReportSummary(report);

        assertEquals(null, captured,
                "mergedAiContent=null 时 buildSummary 应走显式 null 守卫，summaryJson=null");
        // 主流程不中断
        verify(reportWriteService, never()).restoreOrFail(anyLong(), any(), any(), any(), any());
        verify(aiTokenWithholdFeign, never()).returnAiToken(any());
    }

    @Test
    @DisplayName("AC-14: <aifupan-data-block></aifupan-data-block> 空标签 → 提取空字符串（不是 null，锁定隐式契约）")
    void testBuildSummary_emptyTag_returnsEmptyString() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        setupHappyPathWithMergeContent(report,
                "# 报告\n\n<aifupan-data-block></aifupan-data-block>\n\n## 详情\n正文");

        String captured = captureFinishReportSummary(report);

        // 匹配命中但 group(1)="" → .trim() 仍是 ""；区别于 AC-2 的 null（未命中）
        assertEquals("", captured,
                "<aifupan-data-block></aifupan-data-block> 标签命中但内容为空时应返回空字符串，非 null");
    }

    /**
     * AC-15: AI 实际输出常用自闭合开标签 + 闭标签形式（{@code <aifupan-data-block resource-data visible="false"/>}
     * 后跟内容再 {@code </aifupan-data-block>}）+ 内容含 {@code <font>} 富文本。
     * Pattern {@code <aifupan-data-block[^>]*>(.*?)</aifupan-data-block>} 应能灵活匹配。
     */
    @Test
    @DisplayName("AC-15: 真实 AI 输出形式（自闭合属性开标签 + 闭标签 + <font> 富文本） → 提取完整内容")
    void testBuildSummary_realisticAiOutput_extractsContent() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        // 复刻用户给的真实 AI 输出形式
        String content = "# 质检报告\n\n"
                + "## 摘要\n"
                + "<aifupan-data-block resource-data visible=\"false\"/>\n"
                + "\"摸鱼话术\": <font color=\"red\">2</font>处\n"
                + "\"摸鱼话术2\": <font color=\"red\">1</font>处\n"
                + "</aifupan-data-block>\n\n"
                + "## 改进建议\n建议1...";
        setupHappyPathWithMergeContent(report, content);

        String captured = captureFinishReportSummary(report);

        assertNotNull(captured, "真实 AI 输出形式（含属性 + 自闭合开标签）应能命中 pattern");
        assertTrue(captured.contains("摸鱼话术"), "应提取出标签内的摸鱼话术内容");
        assertTrue(captured.contains("<font color=\"red\">"), "应保留 <font> 富文本结构，前端按 markdown 渲染");
        // 验证 trim 生效：开头不应是换行
        assertTrue(!captured.startsWith("\n"), "提取后应 trim 首尾空白");
    }

    @Test
    @DisplayName("AI 中途调用失败（第2次中间报告）→ returnAiToken 补偿")
    void testGenerate_aiCallFails_returnsToken() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(asrResult());
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(videoWithTrade(5L)));
        // 行业取直播间维度 tb_anchor_url_user.trade_id（2026-06-22 改造）：按 secUid+userId+tenantId 取 → 5L
        when(anchorUrlUserFeign.getBySecUidAndUser("sec-1", 42L, 100L)).thenReturn(R.ok(anchorUser(5L)));
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueWordsMatcher(5L, 100L, 16)))).thenReturn(generateCue());
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueWordsMatcher(5L, 100L, 17)))).thenReturn(mergeCue());
        when(aiModelFeign.listDiagnosisModel()).thenReturn(models());
        doNothing().when(intermediateRepository).deleteByReportId(anyLong());

        // 第1次成功，第2次失败（返回 status=1）
        AiReturnDataVo failResult = new AiReturnDataVo();
        failResult.setStatus(1);
        when(aiFeign.chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong()))
                .thenReturn(aiResult(5000))
                .thenReturn(failResult);

        when(intermediateRepository.save(any())).thenAnswer(inv -> {
            ScriptMonitorIntermediateReportEntity e = inv.getArgument(0);
            e.setId("inter-id");
            return e;
        });
        doNothing().when(aiTokenWithholdFeign).returnAiToken(any());

        bll.generate(report, ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), withhold);

        verify(aiTokenWithholdFeign, times(1)).returnAiToken(withhold);
        verify(aiTokenWithholdFeign, never()).settleAiToken(any(), anyLong(), any(), any());
    }

    /**
     * 方案 B+：systemKv 配置中间报告 + 合并报告分别用不同模型 → 验证 chatCompletion 实际用了对应模型
     */
    @Test
    @DisplayName("systemKv 配置 inter / merge 不同 AI 模型 → 中间报告 3 次用 inter，合并 1 次用 merge")
    void testGenerate_systemKvConfigured_usesInterAndMergeModelsSeparately() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        setupHappyPath(report);

        // mock systemKv 配置 inter / merge 两个 key（质检 monitorType=0）
        com.jiuyu.replay.common.vo.SystemKvInfoVo interKv = new com.jiuyu.replay.common.vo.SystemKvInfoVo();
        interKv.setKvValue("inter-model-code");
        com.jiuyu.replay.common.vo.SystemKvInfoVo mergeKv = new com.jiuyu.replay.common.vo.SystemKvInfoVo();
        mergeKv.setKvValue("merge-model-code");
        when(systemKvProducer.getByKey("script_monitor_quality_inter_ai_model")).thenReturn(interKv);
        when(systemKvProducer.getByKey("script_monitor_quality_merge_ai_model")).thenReturn(mergeKv);

        // mock aiModelFeign.getByCode 返不同 AiModelInfoVo
        AiModelInfoVo interModel = new AiModelInfoVo();
        interModel.setModelCode("inter-model-code");
        interModel.setModelName("中间模型");
        AiModelInfoVo mergeModel = new AiModelInfoVo();
        mergeModel.setModelCode("merge-model-code");
        mergeModel.setModelName("合并模型");
        when(aiModelFeign.getByCode("inter-model-code")).thenReturn(interModel);
        when(aiModelFeign.getByCode("merge-model-code")).thenReturn(mergeModel);

        bll.generate(report, ScriptMonitorStatusEnum.NOT_GENERATED.getCode(), withhold);

        // 验证 chatCompletion 被调用 4 次（3 中间 + 1 合并），且 modelConfig 不同
        ArgumentCaptor<AiModelBo> modelCaptor = ArgumentCaptor.forClass(AiModelBo.class);
        verify(aiFeign, times(4)).chatCompletion(modelCaptor.capture(), any(AiMessageBo.class), anyLong());
        List<AiModelBo> models = modelCaptor.getAllValues();
        // 前 3 次是中间报告，用 inter-model-code
        for (int i = 0; i < 3; i++) {
            assertEquals("inter-model-code", models.get(i).getModelCode(),
                    "第 " + (i + 1) + " 次中间报告应用 inter 模型");
        }
        // 第 4 次是合并报告，用 merge-model-code
        assertEquals("merge-model-code", models.get(3).getModelCode(),
                "第 4 次合并报告应用 merge 模型");
    }

    // ====== resolveTradeId VIDEO 来源改造（2026-06-22）======

    /**
     * resolveTradeId 改造 AC-2：sourceType=0(VIDEO) 行业取直播间维度 tb_anchor_url_user.trade_id，
     * 不再用录制记录 video.tradeId。video.tradeId=99 但 anchorUser.tradeId=7 → cueWords 应按 7 取。
     */
    @Test
    @DisplayName("resolveTradeId VIDEO 来源: 取 anchorUrlUserFeign trade_id(7)，忽略 video.tradeId(99)")
    void testResolveTradeId_videoSource_usesAnchorUrlUserTradeId_notVideoTradeId() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(asrResult());
        // video.tradeId=99（应被忽略），secUid=sec-1
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(videoWithTrade(99L)));
        // 直播间维度行业 = 7（应被采用）
        when(anchorUrlUserFeign.getBySecUidAndUser("sec-1", 42L, 100L)).thenReturn(R.ok(anchorUser(7L)));
        // 提示词按 tradeId=7 取（若误用 video.tradeId=99 则取不到 → 抛业务异常走 returnAiToken）
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueWordsMatcher(7L, 100L, 16)))).thenReturn(generateCue());
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueWordsMatcher(7L, 100L, 17)))).thenReturn(mergeCue());
        when(aiModelFeign.listDiagnosisModel()).thenReturn(models());
        when(aiFeign.chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong()))
                .thenReturn(aiResult(10000));
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
        verify(cueWordsFeign).getCueWordByTradeAndType(argThat(cueWordsMatcher(7L, 100L, 16)));
        verify(cueWordsFeign, never()).getCueWordByTradeAndType(argThat(cueWordsMatcher(99L, 100L, 16)));
        verify(reportWriteService, times(1)).finishReport(anyLong(), any(), any());
        verify(aiTokenWithholdFeign, never()).returnAiToken(any());
    }

    /**
     * resolveTradeId 改造 AC-2 边界：VIDEO 来源 anchorUrlUserFeign 查不到（R.data=null）→ 兜底 tradeId=1。
     */
    @Test
    @DisplayName("resolveTradeId VIDEO 来源: anchorUrlUserFeign 返 null → 兜底 tradeId=1")
    void testResolveTradeId_videoSource_anchorUserNull_fallbackToOne() {
        ScriptMonitorReportEntity report = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        when(sensitiveWordsFeign.getAnalysisData(any(), any())).thenReturn(asrResult());
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(videoWithTrade(99L)));
        // 直播间维度查不到（R(code=0, data=null) → ResultUtil.getResult 返 null）
        when(anchorUrlUserFeign.getBySecUidAndUser("sec-1", 42L, 100L)).thenReturn(R.ok(null));
        // 提示词按兜底 tradeId=1 取
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueWordsMatcher(1L, 100L, 16)))).thenReturn(generateCue());
        when(cueWordsFeign.getCueWordByTradeAndType(argThat(cueWordsMatcher(1L, 100L, 17)))).thenReturn(mergeCue());
        when(aiModelFeign.listDiagnosisModel()).thenReturn(models());
        when(aiFeign.chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong()))
                .thenReturn(aiResult(10000));
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
        verify(cueWordsFeign).getCueWordByTradeAndType(argThat(cueWordsMatcher(1L, 100L, 16)));
        verify(reportWriteService, times(1)).finishReport(anyLong(), any(), any());
        verify(aiTokenWithholdFeign, never()).returnAiToken(any());
    }
}
