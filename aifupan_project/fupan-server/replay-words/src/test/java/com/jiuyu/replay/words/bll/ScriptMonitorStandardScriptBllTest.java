package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.common.producer.SystemKvProducer;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.bo.ai.AiMessageBo;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.generic.bo.third.AiTokenUseRecordBo;
import com.jiuyu.replay.generic.feign.ai.AiChatFeign;
import com.jiuyu.replay.generic.feign.order.AiTokenWithholdFeign;
import com.jiuyu.replay.generic.feign.third.AiModelFeign;
import com.jiuyu.replay.generic.feign.words.CueWordsFeign;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.order.RedisWithholdVo;
import com.jiuyu.replay.generic.vo.words.CueWordsInfoVo;
import com.jiuyu.replay.words.bo.script.ConfirmStandardScriptBo;
import com.jiuyu.replay.words.bo.script.GenerateStandardScriptBo;
import com.jiuyu.replay.words.bo.script.TimeAxisItemBo;
import com.jiuyu.replay.words.entity.AnchorUrlUserEntity;
import com.jiuyu.replay.words.entity.StandardScriptEntity;
import com.jiuyu.replay.words.repository.service.StandardScriptService;
import com.jiuyu.replay.words.vo.script.StandardScriptConfirmVo;
import com.jiuyu.replay.words.vo.script.StandardScriptDetailVo;
import com.jiuyu.replay.words.vo.script.StandardScriptVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ScriptMonitorStandardScriptBll 单测（Slice A T21/T23/T24 主流程）。
 *
 * <p>测试范围：</p>
 * <ul>
 *   <li>T21 generateStandardScript：Happy Path + AI 失败 + JSON 解析失败 + 业务参数校验；</li>
 *   <li>T23 confirmStandardScript：accountType=1 拒、新增直播间允许、旧稿软删 + INSERT；</li>
 *   <li>T24 standardScriptDetail：有稿/无稿两种返回形态。</li>
 * </ul>
 * <p>全部用 Mockito 纯单测，不启动 Spring 容器，不调用真实 DB / AI。</p>
 *
 * @author beta
 * @date 2026-06-11
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScriptMonitorStandardScriptBllTest {

    // -------- 依赖 Mock --------

    @Mock
    private StandardScriptService standardScriptService;
    @Mock
    private CueWordsFeign cueWordsFeign;
    @Mock
    private AiChatFeign aiChatFeign;
    @Mock
    private AiModelFeign aiModelFeign;
    @Mock
    private AiTokenWithholdFeign aiTokenWithholdFeign;
    @Mock
    private SystemKvProducer systemKvProducer;

    private ScriptMonitorStandardScriptBll bll;

    @BeforeEach
    void setUp() {
        // ScriptMonitorStandardScriptBll 用 @AllArgsConstructor，所有 final 字段从构造器注入
        bll = new ScriptMonitorStandardScriptBll(
                standardScriptService, cueWordsFeign, aiChatFeign, aiModelFeign,
                aiTokenWithholdFeign, systemKvProducer);
    }

    // ========= helpers =========

    /**
     * 构造一个合法的 T21 入参（非循环）。
     */
    private GenerateStandardScriptBo baseGenBo() {
        GenerateStandardScriptBo bo = new GenerateStandardScriptBo();
        bo.setSpeechMode(0);
        bo.setSpeechSpeed(280);
        bo.setReferenceScript("参考脚本原文");
        return bo;
    }

    /**
     * 构造一个合法的 T23 入参（非循环 + 1 条时间轴）。
     */
    private ConfirmStandardScriptBo baseConfirmBo() {
        ConfirmStandardScriptBo bo = new ConfirmStandardScriptBo();
        bo.setSecUid("secUid-A");
        bo.setSpeechMode(0);
        bo.setSpeechSpeed(280);
        bo.setReferenceScript("参考脚本原文");

        TimeAxisItemBo item = new TimeAxisItemBo();
        item.setTimeRange("00:00-05:00");
        item.setTitle("开场");
        item.setContent("欢迎来到直播间");
        bo.setTimeAxisScript(List.of(item));
        return bo;
    }

    /**
     * 构造预扣 Token 凭据。
     */
    private RedisWithholdVo withhold() {
        RedisWithholdVo vo = new RedisWithholdVo();
        vo.setRedisId(1L);
        vo.setWithholdId("withhold-1");
        return vo;
    }

    /**
     * 构造 AiModelInfoVo（systemKv 命中路径）。
     *
     * <p>consumeMultiple=1.0：与 model 表真实配置对齐，保证 AiUtils.aiTokenConsumeMultiple 语义
     * = ×DEFAULT_CONSUME_MULTIPLE(1.5) × consumeMultiple(1.0) = ×1.5（符合业务预期）。</p>
     */
    private AiModelInfoVo aiModelInfo() {
        AiModelInfoVo info = new AiModelInfoVo();
        info.setModelCode("doubao-1.5");
        info.setModelName("豆包");
        info.setConsumeMultiple(1.0);
        return info;
    }

    /**
     * 构造 systemKv 配置（命中路径）。
     */
    private SystemKvInfoVo kvWithModel() {
        SystemKvInfoVo kv = new SystemKvInfoVo();
        kv.setKvValue("doubao-1.5");
        return kv;
    }

    /**
     * 构造提示词（含三个占位符）。
     */
    private CueWordsInfoVo cueWord() {
        CueWordsInfoVo cue = new CueWordsInfoVo();
        cue.setProblem("trade=#{trade}, speed=#{speechSpeed}, ref=#{referenceScript}");
        return cue;
    }

    /**
     * 构造 AI 成功返回（content 为合法 JSON 数组）。
     */
    private AiReturnDataVo aiOk(String content) {
        AiReturnDataVo r = new AiReturnDataVo();
        r.setContent(content);
        r.setStatus(0);
        r.setTotalTokens(100);
        r.setRequestId("req-1");
        return r;
    }

    /**
     * 合法 JSON 数组（2 个时间轴元素）— cueType=27 改造后 AI 输出契约。
     */
    private String validJsonArray() {
        return "[{\"timeRange\":\"00:00-05:00\",\"title\":\"开场\",\"content\":\"欢迎来到直播间，我是xxx\"},"
                + "{\"timeRange\":\"05:00-10:00\",\"title\":\"互动\",\"content\":\"大家有什么问题可以扣 1\"}]";
    }

    /**
     * 合法 JSON 数组包 ```json ... ``` 代码块（AI 偶尔附带，parseJsonOutput 主动剥离）。
     */
    private String validJsonWithCodeFence() {
        return "```json\n" + validJsonArray() + "\n```";
    }

    /**
     * 构造已存在的 AnchorUrlUserEntity（accountType=0 自有账号）。
     */
    private AnchorUrlUserEntity anchorEntity(Integer accountType) {
        AnchorUrlUserEntity entity = new AnchorUrlUserEntity();
        entity.setId(10L);
        entity.setTenantId(100L);
        entity.setUserId(1L);
        entity.setAnchorUrlSecUid("secUid-A");
        entity.setAccountType(accountType);
        return entity;
    }

    /**
     * 构造已存在的 StandardScriptEntity（用于 T23 软删场景 / T24 详情场景）。
     */
    private StandardScriptEntity existingScript() {
        StandardScriptEntity e = new StandardScriptEntity();
        e.setId(999L);
        e.setTenantId(100L);
        e.setUserId(1L);
        e.setSecUid("secUid-A");
        e.setSpeechMode(0);
        e.setSpeechSpeed(280);
        e.setReferenceScript("旧参考脚本");
        e.setTimeAxisScript("[{\"timeRange\":\"00:00-05:00\",\"title\":\"开场\",\"content\":\"hi\"}]");
        e.setCreateDate(new Date(1717000000000L));
        e.setIsDeleted(0);
        return e;
    }

    /**
     * 给 T21 路径所有 Feign / Producer mock 串起来，制造一条 happy path。
     *
     * @param aiContent AI 返回的 content（JSON 数组字符串）
     */
    private void mockT21HappyPath(String aiContent) {
        when(aiTokenWithholdFeign.withholdAiToken(anyLong(), anyLong())).thenReturn(withhold());
        when(systemKvProducer.getByKey(anyString())).thenReturn(kvWithModel());
        when(aiModelFeign.getByCode(anyString())).thenReturn(aiModelInfo());
        when(cueWordsFeign.getCueWordByTradeAndType(any())).thenReturn(cueWord());
        when(aiChatFeign.chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong()))
                .thenReturn(aiOk(aiContent));
    }

    // ======================================================================
    // T21 generateStandardScript
    // ======================================================================

    @Test
    @DisplayName("T21-AC-1: Happy Path — AI 返回合法 JSON 数组，解析出 2 条时间轴 + settle Token + 不调 returnAiToken")
    void generateStandardScript_happyPath_returns2Items() {
        // given
        mockT21HappyPath(validJsonArray());

        // when
        StandardScriptVo vo = bll.generateStandardScript(baseGenBo(), 1L, 100L);

        // then
        assertThat(vo).isNotNull();
        assertThat(vo.getTimeAxisScript()).hasSize(2);
        assertThat(vo.getTimeAxisScript().get(0).getTimeRange()).isEqualTo("00:00-05:00");
        assertThat(vo.getTimeAxisScript().get(0).getTitle()).isEqualTo("开场");
        assertThat(vo.getSpeechMode()).isEqualTo(0);
        assertThat(vo.getSpeechSpeed()).isEqualTo(280);
        // settle 路径
        verify(aiTokenWithholdFeign, times(1))
                .settleAiToken(any(RedisWithholdVo.class), anyLong(),
                        any(AiReturnDataVo.class), any(AiTokenUseRecordBo.class));
        verify(aiTokenWithholdFeign, never()).returnAiToken(any());
    }

    @Test
    @DisplayName("T21-AC-1b: AI 包 ```json ... ``` 代码块 → parseJsonOutput 主动剥离后正常解析")
    void generateStandardScript_aiWrappedInCodeFence_stillParsedCorrectly() {
        // given
        mockT21HappyPath(validJsonWithCodeFence());

        // when
        StandardScriptVo vo = bll.generateStandardScript(baseGenBo(), 1L, 100L);

        // then
        assertThat(vo).isNotNull();
        assertThat(vo.getTimeAxisScript()).hasSize(2);
        verify(aiTokenWithholdFeign, never()).returnAiToken(any());
    }

    @Test
    @DisplayName("T21-AC-2: speechMode=1 但 cycleDurationMinutes=null → 抛 BusinessException(70013)")
    void generateStandardScript_speechMode1_noCycleDuration_throws70013() {
        // given：循环模式但缺时长
        GenerateStandardScriptBo bo = baseGenBo();
        bo.setSpeechMode(1);
        bo.setCycleDurationMinutes(null);

        // when + then
        assertThatThrownBy(() -> bll.generateStandardScript(bo, 1L, 100L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode()));
        // 业务校验未通过，预扣 Token 都不应调
        verify(aiTokenWithholdFeign, never()).withholdAiToken(anyLong(), anyLong());
    }

    @Test
    @DisplayName("T21-AC-3: AI 返回 status=1 失败 → 抛 BusinessException(70008) + 归还预扣 Token")
    void generateStandardScript_aiReturnsFailure_throws70008AndReturnsToken() {
        // given：AI 返回失败
        when(aiTokenWithholdFeign.withholdAiToken(anyLong(), anyLong())).thenReturn(withhold());
        when(systemKvProducer.getByKey(anyString())).thenReturn(kvWithModel());
        when(aiModelFeign.getByCode(anyString())).thenReturn(aiModelInfo());
        when(cueWordsFeign.getCueWordByTradeAndType(any())).thenReturn(cueWord());
        AiReturnDataVo aiFail = new AiReturnDataVo();
        aiFail.setStatus(1);
        when(aiChatFeign.chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong()))
                .thenReturn(aiFail);

        // when + then
        assertThatThrownBy(() -> bll.generateStandardScript(baseGenBo(), 1L, 100L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_GENERATE_FAIL.getCode()));
        verify(aiTokenWithholdFeign, times(1)).returnAiToken(any(RedisWithholdVo.class));
        verify(aiTokenWithholdFeign, never()).settleAiToken(any(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("T21-AC-4: AI 返回 null → 抛 BusinessException(70008) + 归还预扣 Token")
    void generateStandardScript_aiReturnsNull_throws70008AndReturnsToken() {
        // given
        when(aiTokenWithholdFeign.withholdAiToken(anyLong(), anyLong())).thenReturn(withhold());
        when(systemKvProducer.getByKey(anyString())).thenReturn(kvWithModel());
        when(aiModelFeign.getByCode(anyString())).thenReturn(aiModelInfo());
        when(cueWordsFeign.getCueWordByTradeAndType(any())).thenReturn(cueWord());
        when(aiChatFeign.chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong()))
                .thenReturn(null);

        // when + then
        assertThatThrownBy(() -> bll.generateStandardScript(baseGenBo(), 1L, 100L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_GENERATE_FAIL.getCode()));
        verify(aiTokenWithholdFeign, times(1)).returnAiToken(any(RedisWithholdVo.class));
    }

    @Test
    @DisplayName("T21-AC-5: AI content 为空字符串 → JSON 解析失败抛 70008 + 归还预扣 Token")
    void generateStandardScript_aiEmptyContent_throws70008AndReturnsToken() {
        // given
        mockT21HappyPath("");

        // when + then
        assertThatThrownBy(() -> bll.generateStandardScript(baseGenBo(), 1L, 100L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_GENERATE_FAIL.getCode()));
        verify(aiTokenWithholdFeign, times(1)).returnAiToken(any(RedisWithholdVo.class));
    }

    @Test
    @DisplayName("T21-AC-6: AI 返回空 JSON 数组 [] → 解析后 items 为空抛 70008 + 归还 Token")
    void generateStandardScript_aiEmptyJsonArray_throws70008AndReturnsToken() {
        // given
        mockT21HappyPath("[]");

        // when + then
        assertThatThrownBy(() -> bll.generateStandardScript(baseGenBo(), 1L, 100L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_GENERATE_FAIL.getCode()));
        verify(aiTokenWithholdFeign, times(1)).returnAiToken(any(RedisWithholdVo.class));
    }

    @Test
    @DisplayName("T21-AC-6b: AI 返回非法 JSON 文本（不是数组）→ JSON 反序列化失败抛 70008 + 归还 Token")
    void generateStandardScript_aiInvalidJson_throws70008AndReturnsToken() {
        // given：AI 返回旧 markdown 表格（非 JSON）模拟提示词回滚 / AI 顽固输出旧格式
        String oldMarkdown = "|时间段|框架标题|直播话术|\n|----|----|----|\n|00:00-05:00|开场|欢迎|\n";
        mockT21HappyPath(oldMarkdown);

        // when + then
        assertThatThrownBy(() -> bll.generateStandardScript(baseGenBo(), 1L, 100L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_GENERATE_FAIL.getCode()));
        verify(aiTokenWithholdFeign, times(1)).returnAiToken(any(RedisWithholdVo.class));
    }

    @Test
    @DisplayName("T21-AC-7: AI 调用抛任意异常 → 转为 70008 + 归还预扣 Token")
    void generateStandardScript_aiThrowsException_convertsTo70008AndReturnsToken() {
        // given
        when(aiTokenWithholdFeign.withholdAiToken(anyLong(), anyLong())).thenReturn(withhold());
        when(systemKvProducer.getByKey(anyString())).thenReturn(kvWithModel());
        when(aiModelFeign.getByCode(anyString())).thenReturn(aiModelInfo());
        when(cueWordsFeign.getCueWordByTradeAndType(any())).thenReturn(cueWord());
        when(aiChatFeign.chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong()))
                .thenThrow(new RuntimeException("simulated AI timeout"));

        // when + then
        assertThatThrownBy(() -> bll.generateStandardScript(baseGenBo(), 1L, 100L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_GENERATE_FAIL.getCode()));
        verify(aiTokenWithholdFeign, times(1)).returnAiToken(any(RedisWithholdVo.class));
    }

    @Test
    @DisplayName("T21-AC-8: systemKv 未命中 → 兜底 listDiagnosisModel.get(0)；listDiagnosisModel 为空时抛 70013")
    void generateStandardScript_systemKvMiss_listDiagnosisModelEmpty_throws70013() {
        // given：kv 未命中 + 兜底列表也为空
        when(aiTokenWithholdFeign.withholdAiToken(anyLong(), anyLong())).thenReturn(withhold());
        when(systemKvProducer.getByKey(anyString())).thenReturn(null);
        when(aiModelFeign.listDiagnosisModel()).thenReturn(new ArrayList<>());

        // when + then
        assertThatThrownBy(() -> bll.generateStandardScript(baseGenBo(), 1L, 100L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode()));
        // 业务异常路径也归还 Token
        verify(aiTokenWithholdFeign, times(1)).returnAiToken(any(RedisWithholdVo.class));
    }

    @Test
    @DisplayName("T21-AC-9: 提示词未配置（cueWordsFeign 返 null） → 抛 70013 + 归还 Token")
    void generateStandardScript_promptNotConfigured_throws70013AndReturnsToken() {
        // given
        when(aiTokenWithholdFeign.withholdAiToken(anyLong(), anyLong())).thenReturn(withhold());
        when(systemKvProducer.getByKey(anyString())).thenReturn(kvWithModel());
        when(aiModelFeign.getByCode(anyString())).thenReturn(aiModelInfo());
        when(cueWordsFeign.getCueWordByTradeAndType(any())).thenReturn(null);

        // when + then
        assertThatThrownBy(() -> bll.generateStandardScript(baseGenBo(), 1L, 100L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode()));
        verify(aiTokenWithholdFeign, times(1)).returnAiToken(any(RedisWithholdVo.class));
    }

    @Test
    @DisplayName("T21-AC-10: Happy Path settleAiToken 中 assistantType=24（FIDELITY_MONITOR）")
    void generateStandardScript_settle_assistantTypeIsFidelityBusinessType() {
        // given
        mockT21HappyPath(validJsonArray());

        // when
        bll.generateStandardScript(baseGenBo(), 1L, 100L);

        // then：抓 settle 的 recordBo 参数，断言 assistantType=24
        ArgumentCaptor<AiTokenUseRecordBo> captor = ArgumentCaptor.forClass(AiTokenUseRecordBo.class);
        verify(aiTokenWithholdFeign).settleAiToken(any(), eq(1L), any(), captor.capture());
        AiTokenUseRecordBo recordBo = captor.getValue();
        assertThat(recordBo.getAssistantType()).isEqualTo(24);
        assertThat(recordBo.getTenantId()).isEqualTo(100L);
    }

    // ======================================================================
    // T23 confirmStandardScript
    // ======================================================================

    @Test
    @DisplayName("T23-AC-1: Happy Path — accountType=0 自有账号，无旧稿，INSERT 新稿成功并返回 standardScriptId")
    void confirmStandardScript_newAnchorScene_ownAccount_noExisting_insertsNew() {
        // given：accountType=0 + 无旧稿
        when(standardScriptService.validateAccountAndLoadAnchor(100L, 1L, "secUid-A"))
                .thenReturn(anchorEntity(0));
        when(standardScriptService.findValid(100L, 1L, "secUid-A")).thenReturn(null);
        when(standardScriptService.save(any(StandardScriptEntity.class))).thenReturn(true);

        // when
        StandardScriptConfirmVo vo = bll.confirmStandardScript(baseConfirmBo(), 1L, 100L);

        // then
        assertThat(vo).isNotNull();
        assertThat(vo.getStandardScriptId()).isNotNull();
        // 无旧稿不走 softDelete
        verify(standardScriptService, never()).softDelete(anyLong());
        // INSERT 路径触发
        ArgumentCaptor<StandardScriptEntity> captor = ArgumentCaptor.forClass(StandardScriptEntity.class);
        verify(standardScriptService, times(1)).save(captor.capture());
        StandardScriptEntity saved = captor.getValue();
        assertThat(saved.getSecUid()).isEqualTo("secUid-A");
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getTenantId()).isEqualTo(100L);
        assertThat(saved.getIsDeleted()).isEqualTo(0);
    }

    @Test
    @DisplayName("T23-AC-2: 已有旧稿 → 原地 UPDATE 内容字段（保留 standardScriptId 稳定身份），不走 softDelete / save")
    void confirmStandardScript_hasExisting_updatesInPlace() {
        // given：accountType=0 + 有旧稿（id=999）
        when(standardScriptService.validateAccountAndLoadAnchor(100L, 1L, "secUid-A"))
                .thenReturn(anchorEntity(0));
        when(standardScriptService.findValid(100L, 1L, "secUid-A")).thenReturn(existingScript());
        when(standardScriptService.updateById(any(StandardScriptEntity.class))).thenReturn(true);

        // when
        StandardScriptConfirmVo vo = bll.confirmStandardScript(baseConfirmBo(), 1L, 100L);

        // then：updateById 调一次，softDelete / save 都不调（UPDATE 模式不再产生新 id）
        ArgumentCaptor<StandardScriptEntity> captor = ArgumentCaptor.forClass(StandardScriptEntity.class);
        verify(standardScriptService, times(1)).updateById(captor.capture());
        verify(standardScriptService, never()).softDelete(anyLong());
        verify(standardScriptService, never()).save(any(StandardScriptEntity.class));
        // standardScriptId 保持稳定（返回既存 id=999，非新 Snowflake）
        assertThat(vo.getStandardScriptId()).isEqualTo(999L);
        // 内容字段已按 Bo 入参覆盖
        StandardScriptEntity updated = captor.getValue();
        assertThat(updated.getId()).isEqualTo(999L);
        assertThat(updated.getSpeechSpeed()).isEqualTo(baseConfirmBo().getSpeechSpeed());
        assertThat(updated.getReferenceScript()).isEqualTo(baseConfirmBo().getReferenceScript());
    }

    @Test
    @DisplayName("T23-AC-3: accountType=1（竞品）→ validateAccountAndLoadAnchor 抛 70004 → confirm 整体被打断")
    void confirmStandardScript_competitorAccount_throws70004() {
        // given：validate 抛 70004
        when(standardScriptService.validateAccountAndLoadAnchor(100L, 1L, "secUid-A"))
                .thenThrow(new BusinessException(
                        StatusCode.SCRIPT_MONITOR_NOT_OWN_ACCOUNT.getCode(),
                        StatusCode.SCRIPT_MONITOR_NOT_OWN_ACCOUNT.getMsg()));

        // when + then
        assertThatThrownBy(() -> bll.confirmStandardScript(baseConfirmBo(), 1L, 100L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(StatusCode.SCRIPT_MONITOR_NOT_OWN_ACCOUNT.getCode()));
        // 校验未通过 → findValid / save / softDelete 都不调
        verify(standardScriptService, never()).findValid(anyLong(), anyLong(), anyString());
        verify(standardScriptService, never()).save(any(StandardScriptEntity.class));
        verify(standardScriptService, never()).softDelete(anyLong());
    }

    @Test
    @DisplayName("T23-AC-4: 新增直播间场景（anchor 查不到）→ validate 返 null 跳过 accountType 校验，照常 INSERT")
    void confirmStandardScript_newAnchorNotFound_skipsAccountTypeValidation() {
        // given：validate 返 null（新增直播间场景）+ 无旧稿
        when(standardScriptService.validateAccountAndLoadAnchor(100L, 1L, "secUid-A")).thenReturn(null);
        when(standardScriptService.findValid(100L, 1L, "secUid-A")).thenReturn(null);
        when(standardScriptService.save(any(StandardScriptEntity.class))).thenReturn(true);

        // when
        StandardScriptConfirmVo vo = bll.confirmStandardScript(baseConfirmBo(), 1L, 100L);

        // then：照常 INSERT
        assertThat(vo.getStandardScriptId()).isNotNull();
        verify(standardScriptService, times(1)).save(any(StandardScriptEntity.class));
    }

    @Test
    @DisplayName("T23-AC-5: speechMode=1 但 cycleDurationMinutes=null → 抛 70013，不走 validate")
    void confirmStandardScript_speechMode1_missingCycleDuration_throws70013() {
        // given
        ConfirmStandardScriptBo bo = baseConfirmBo();
        bo.setSpeechMode(1);
        bo.setCycleDurationMinutes(null);

        // when + then
        assertThatThrownBy(() -> bll.confirmStandardScript(bo, 1L, 100L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode()));
        verify(standardScriptService, never())
                .validateAccountAndLoadAnchor(anyLong(), anyLong(), anyString());
    }

    // ======================================================================
    // T24 standardScriptDetail
    // ======================================================================

    @Test
    @DisplayName("T24-AC-1: 有已确认稿 → hasScript=true + 反序列化时间轴 + ISO-8601 createDate")
    void standardScriptDetail_hasScript_returnsDetail() {
        // given
        when(standardScriptService.findValid(100L, 1L, "secUid-A")).thenReturn(existingScript());

        // when
        StandardScriptDetailVo vo = bll.standardScriptDetail(100L, 1L, "secUid-A");

        // then
        assertThat(vo.getHasScript()).isTrue();
        assertThat(vo.getStandardScriptId()).isEqualTo(999L);
        assertThat(vo.getSecUid()).isEqualTo("secUid-A");
        assertThat(vo.getSpeechSpeed()).isEqualTo(280);
        assertThat(vo.getReferenceScript()).isEqualTo("旧参考脚本");
        assertThat(vo.getTimeAxisScript()).hasSize(1);
        assertThat(vo.getTimeAxisScript().get(0).getTimeRange()).isEqualTo("00:00-05:00");
        assertThat(vo.getCreateDate()).isNotNull();
        // ISO-8601 形如 2024-05-29T...，至少应包含 'T'
        assertThat(vo.getCreateDate()).contains("T");
    }

    @Test
    @DisplayName("T24-AC-2: 无已确认稿 → hasScript=false + speechSpeed=280 + 其余字段 null")
    void standardScriptDetail_noScript_returnsDefault() {
        // given
        when(standardScriptService.findValid(100L, 1L, "secUid-A")).thenReturn(null);

        // when
        StandardScriptDetailVo vo = bll.standardScriptDetail(100L, 1L, "secUid-A");

        // then
        assertThat(vo.getHasScript()).isFalse();
        assertThat(vo.getSpeechSpeed()).isEqualTo(280);
        assertThat(vo.getStandardScriptId()).isNull();
        assertThat(vo.getSecUid()).isNull();
        assertThat(vo.getSpeechMode()).isNull();
        assertThat(vo.getTimeAxisScript()).isNull();
        assertThat(vo.getCreateDate()).isNull();
    }

    @Test
    @DisplayName("T24-AC-3: speechSpeed 为 null 时返回默认 280（防御性）")
    void standardScriptDetail_speechSpeedNull_returns280() {
        // given
        StandardScriptEntity entity = existingScript();
        entity.setSpeechSpeed(null);
        when(standardScriptService.findValid(100L, 1L, "secUid-A")).thenReturn(entity);

        // when
        StandardScriptDetailVo vo = bll.standardScriptDetail(100L, 1L, "secUid-A");

        // then
        assertThat(vo.getHasScript()).isTrue();
        assertThat(vo.getSpeechSpeed()).isEqualTo(280);
    }

    @Test
    @DisplayName("T24-AC-4: timeAxisScript JSON 反序列化失败时不抛异常（容错 → items 为 null）")
    void standardScriptDetail_timeAxisJsonInvalid_doesNotThrow() {
        // given：非法 JSON
        StandardScriptEntity entity = existingScript();
        entity.setTimeAxisScript("not-a-json-array");
        when(standardScriptService.findValid(100L, 1L, "secUid-A")).thenReturn(entity);

        // when
        StandardScriptDetailVo vo = bll.standardScriptDetail(100L, 1L, "secUid-A");

        // then
        assertThat(vo.getHasScript()).isTrue();
        assertThat(vo.getTimeAxisScript()).isNull();
    }

    // ======================================================================
    // T21 JSON 解析失败重试逻辑（AC-1 ~ AC-6）
    // ======================================================================

    /**
     * 含未转义双引号的非法 JSON 内容（模拟 AI 直接在 content 里写 " 而未转义）。
     * fastjson2 parseArray 会因 illegal fieldName 抛 JSONException。
     */
    private String unescapedQuoteJson() {
        // 故意使 JSON 数组内 content 字段值包含未转义的 "，导致 parseArray 报错
        return "[{\"timeRange\":\"00:00-01:00\",\"title\":\"开场\",\"content\":\"把\"1\"打在公屏\"}]";
    }

    /**
     * 构造第 N 次 AI 返回（totalTokens 可自定义，用于重试路径累加断言）。
     */
    private AiReturnDataVo aiOkWithTokens(String content, int totalTokens) {
        AiReturnDataVo r = new AiReturnDataVo();
        r.setContent(content);
        r.setStatus(0);
        r.setTotalTokens(totalTokens);
        r.setRequestId("req-retry");
        return r;
    }

    /**
     * 设置 T21 前置 mock（预扣 / systemKv / aiModel / cueWords），不含 aiChatFeign.chatCompletion。
     */
    private void mockT21Prerequisites() {
        when(aiTokenWithholdFeign.withholdAiToken(anyLong(), anyLong())).thenReturn(withhold());
        when(systemKvProducer.getByKey(anyString())).thenReturn(kvWithModel());
        when(aiModelFeign.getByCode(anyString())).thenReturn(aiModelInfo());
        when(cueWordsFeign.getCueWordByTradeAndType(any())).thenReturn(cueWord());
    }

    @Test
    @DisplayName("AC-1: AI 首次返合规 JSON → 0 次重试，aiChatFeign.chatCompletion 调用次数 = 1，settle 一次")
    void generateStandardScript_aiFirstCallSucceeds_noRetry_settleOnce() {
        // given
        mockT21Prerequisites();
        when(aiChatFeign.chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong()))
                .thenReturn(aiOkWithTokens(validJsonArray(), 200));

        // when
        StandardScriptVo vo = bll.generateStandardScript(baseGenBo(), 1L, 100L);

        // then
        assertThat(vo).isNotNull();
        assertThat(vo.getTimeAxisScript()).isNotEmpty();
        // AI 调用恰好 1 次（无重试）
        verify(aiChatFeign, times(1)).chatCompletion(any(), any(), anyLong());
        // settle 恰好 1 次，returnAiToken 0 次
        verify(aiTokenWithholdFeign, times(1))
                .settleAiToken(any(RedisWithholdVo.class), anyLong(),
                        any(AiReturnDataVo.class), any(AiTokenUseRecordBo.class));
        verify(aiTokenWithholdFeign, never()).returnAiToken(any());
    }

    @Test
    @DisplayName("AC-2: AI 首次返非法 JSON（抛 70008+JSON parse），第二次返合规 → settle 累加 totalTokens 一次，returnAiToken 调用次数 = 0")
    void generateStandardScript_aiFirstFailParseRetrySuccess_settleAccumulatedTokensOnce() {
        // given
        mockT21Prerequisites();
        // 首次返非法 JSON（未转义双引号），第二次返合规 JSON
        when(aiChatFeign.chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong()))
                .thenReturn(aiOkWithTokens(unescapedQuoteJson(), 120))
                .thenReturn(aiOkWithTokens(validJsonArray(), 180));

        // when
        StandardScriptVo vo = bll.generateStandardScript(baseGenBo(), 1L, 100L);

        // then
        assertThat(vo).isNotNull();
        assertThat(vo.getTimeAxisScript()).isNotEmpty();
        // AI 调用 2 次（首次 + 1 次重试）
        verify(aiChatFeign, times(2)).chatCompletion(any(), any(), anyLong());
        // returnAiToken 不调
        verify(aiTokenWithholdFeign, never()).returnAiToken(any());
        // settle 恰好 1 次，且 settleResult.totalTokens 为累加原始 token 经 AiUtils 乘系数后的结果
        // AiUtils.aiTokenConsumeMultiple 内部先 *DEFAULT_CONSUME_MULTIPLE(1.5) 再 *consumeMultiple(1.0，来自 model 表)
        // 故：(120 + 180) * 1.5 * 1.0 = 450
        ArgumentCaptor<AiReturnDataVo> settleCaptor = ArgumentCaptor.forClass(AiReturnDataVo.class);
        verify(aiTokenWithholdFeign, times(1))
                .settleAiToken(any(RedisWithholdVo.class), anyLong(), settleCaptor.capture(), any(AiTokenUseRecordBo.class));
        // 关键断言：settle 仅调用 1 次，且 totalTokens = 两次 AI 调用累加后经乘系数的结果（450）
        assertThat(settleCaptor.getValue().getTotalTokens()).isEqualTo(450);
    }

    @Test
    @DisplayName("AC-3: AI 两次都返非法 JSON（两次均抛 70008+JSON parse）→ 抛 BusinessException(70008)，returnAiToken 调用次数 = 1")
    void generateStandardScript_aiBothCallsFailParse_throws70008_returnTokenOnce() {
        // given
        mockT21Prerequisites();
        // 首次和重试均返非法 JSON
        when(aiChatFeign.chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong()))
                .thenReturn(aiOkWithTokens(unescapedQuoteJson(), 150))
                .thenReturn(aiOkWithTokens(unescapedQuoteJson(), 160));

        // when + then
        assertThatThrownBy(() -> bll.generateStandardScript(baseGenBo(), 1L, 100L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_GENERATE_FAIL.getCode()));
        // returnAiToken 恰好 1 次（不是两次）
        verify(aiTokenWithholdFeign, times(1)).returnAiToken(any(RedisWithholdVo.class));
        // settle 不调
        verify(aiTokenWithholdFeign, never()).settleAiToken(any(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("AC-4: AI 持续返非法 JSON → 最多 2 次 AI 调用（MAX_JSON_PARSE_RETRY_COUNT = 1 硬上限）")
    void generateStandardScript_aiAlwaysFailParse_atMost2AiCalls() {
        // given：mock 无限返回非法 JSON
        mockT21Prerequisites();
        when(aiChatFeign.chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong()))
                .thenReturn(aiOkWithTokens(unescapedQuoteJson(), 100));

        // when + then
        assertThatThrownBy(() -> bll.generateStandardScript(baseGenBo(), 1L, 100L))
                .isInstanceOf(BusinessException.class);
        // 恰好 2 次：首次 + 1 次重试，不存在第 3 次
        verify(aiChatFeign, times(2)).chatCompletion(any(), any(), anyLong());
    }

    @Test
    @DisplayName("AC-5: AI status=1（BLL 主动抛 70008，非 parseJsonOutput 路径）→ aiChatFeign.chatCompletion 调用次数 = 1，不触发重试")
    void generateStandardScript_aiStatus1_noRetry_aiCalledOnce() {
        // given：AI 返回 status=1（BLL line 154-155 主动抛 70008，message 不含 JSON parse）
        mockT21Prerequisites();
        AiReturnDataVo aiFail = new AiReturnDataVo();
        aiFail.setStatus(1);
        aiFail.setTotalTokens(0);
        when(aiChatFeign.chatCompletion(any(AiModelBo.class), any(AiMessageBo.class), anyLong()))
                .thenReturn(aiFail);

        // when + then
        assertThatThrownBy(() -> bll.generateStandardScript(baseGenBo(), 1L, 100L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_GENERATE_FAIL.getCode()));
        // AI 调用恰好 1 次（status=1 分支不属于 parseJsonOutput 重试域）
        verify(aiChatFeign, times(1)).chatCompletion(any(), any(), anyLong());
        // returnAiToken 调 1 次（正常失败路径）
        verify(aiTokenWithholdFeign, times(1)).returnAiToken(any(RedisWithholdVo.class));
    }

    @Test
    @DisplayName("AC-6: withhold 预扣失败抛 70001 → 抛 70001，aiChatFeign.chatCompletion 调用次数 = 0")
    void generateStandardScript_withholdFails_throws70001_aiNotCalled() {
        // given：预扣失败（Token 不足，70001）
        when(aiTokenWithholdFeign.withholdAiToken(anyLong(), anyLong()))
                .thenThrow(new BusinessException(StatusCode.SCRIPT_MONITOR_TOKEN_NOT_ENOUGH.getCode(),
                        StatusCode.SCRIPT_MONITOR_TOKEN_NOT_ENOUGH.getMsg()));

        // when + then
        assertThatThrownBy(() -> bll.generateStandardScript(baseGenBo(), 1L, 100L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(StatusCode.SCRIPT_MONITOR_TOKEN_NOT_ENOUGH.getCode()));
        // AI 调用 0 次（预扣阶段就已失败）
        verify(aiChatFeign, never()).chatCompletion(any(), any(), anyLong());
    }
}
