package com.jiuyu.replay.words.api;

import com.jiuyu.replay.generic.bo.words.cue.CueWordsQueryBo;
import com.jiuyu.replay.generic.vo.words.CueWordsInfoVo;
import com.jiuyu.replay.generic.vo.words.TradeInfoVo;
import com.jiuyu.replay.words.producer.AnchorVideoProducer;
import com.jiuyu.replay.words.producer.BasicSettingsProducer;
import com.jiuyu.replay.words.producer.CueWordsProducer;
import com.jiuyu.replay.words.producer.SyncContrastProducer;
import com.jiuyu.replay.words.producer.TradeProducer;
import com.jiuyu.replay.words.producer.UploadFileProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CueWordsApi#getCueWordByTradeAndType(CueWordsQueryBo)} 路由算法单测。
 *
 * <p>覆盖 6 个 AC：
 * <ul>
 *   <li>AC-1 同行业层级租户定制赢通用（happy path）</li>
 *   <li>AC-2 租户未定制 fallback 通用</li>
 *   <li>AC-3 跨行业层级子赢父（即使父层级有租户定制）</li>
 *   <li>AC-4 全链路无记录返 null</li>
 *   <li>AC-5 tenantId=null 退化旧行为</li>
 *   <li>AC-6 cueType=null 抛 IllegalArgumentException</li>
 * </ul>
 * 全部 Mockito 纯单测，不启动 Spring 容器。</p>
 *
 * @author beta
 * @date 2026-06-05
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CueWordsApiTest {

    // -------- 依赖 Mock（CueWordsApi 是 @AllArgsConstructor，6 个 final 字段必须全 Mock） --------

    @Mock
    private CueWordsProducer cueWordsProducer;
    @Mock
    private TradeProducer tradeProducer;
    @Mock
    private AnchorVideoProducer anchorVideoProducer;
    @Mock
    private UploadFileProducer uploadFileProducer;
    @Mock
    private BasicSettingsProducer basicSettingsProducer;
    @Mock
    private SyncContrastProducer syncContrastProducer;

    @InjectMocks
    private CueWordsApi cueWordsApi;

    // -------- helpers --------

    /**
     * 构造 TradeInfoVo（仅设 id，路由算法只用 id）
     */
    private TradeInfoVo trade(long id) {
        TradeInfoVo vo = new TradeInfoVo();
        vo.setId(id);
        return vo;
    }

    /**
     * 构造 CueWordsInfoVo（路由算法关心的字段：tradeId / tenantId / problem）
     */
    private CueWordsInfoVo cue(long tradeId, long tenantId, String problem) {
        CueWordsInfoVo vo = new CueWordsInfoVo();
        vo.setTradeId(tradeId);
        vo.setTenantId(tenantId);
        vo.setProblem(problem);
        return vo;
    }

    /**
     * 构造基础 Bo：cueType=16（话术质检生成），tradeId 由调用方覆盖
     */
    private CueWordsQueryBo baseBo(Long tradeId, Long tenantId) {
        CueWordsQueryBo bo = new CueWordsQueryBo();
        bo.setCueType(16);
        bo.setTradeId(tradeId);
        bo.setTenantId(tenantId);
        return bo;
    }

    // -------- AC-1 同行业层级租户定制赢通用 --------

    @Test
    @DisplayName("AC-1: 同行业层级，租户定制（tenantId=100）赢通用（tenantId=0），返回租户定制条目")
    void testGetCueWord_sameTradeTenantWinsUniversal() {
        // given：行业父链 [10, 1]；SQL 已按 tenant_id DESC, sort ASC 排序后的结果
        when(tradeProducer.listParentsByTradeId(eq(10L), anyInt()))
                .thenReturn(List.of(trade(10L), trade(1L)));
        when(cueWordsProducer.listByTradeIdsAndQuery(any(), any()))
                .thenReturn(List.of(
                        cue(10L, 100L, "租户专属"),  // 同行业、租户定制（SQL 排序：tenant_id DESC 在前）
                        cue(10L, 0L, "通用")          // 同行业、通用
                ));

        CueWordsQueryBo bo = baseBo(10L, 100L);

        // when
        CueWordsInfoVo result = cueWordsApi.getCueWordByTradeAndType(bo);

        // then：算法 toMap merge=(a,b)->a 保留首条，即租户定制条目
        assertThat(result).isNotNull();
        assertThat(result.getProblem()).isEqualTo("租户专属");
        assertThat(result.getTradeId()).isEqualTo(10L);
        assertThat(result.getTenantId()).isEqualTo(100L);
    }

    // -------- AC-2 租户未定制 fallback 通用 --------

    @Test
    @DisplayName("AC-2: 同行业层级，租户（tenantId=200）未配定制，fallback 到通用（tenantId=0）")
    void testGetCueWord_tenantNotCustomizedFallbackUniversal() {
        // given：行业父链 [10, 1]；mock 只返回通用条目
        when(tradeProducer.listParentsByTradeId(eq(10L), anyInt()))
                .thenReturn(List.of(trade(10L), trade(1L)));
        when(cueWordsProducer.listByTradeIdsAndQuery(any(), any()))
                .thenReturn(List.of(
                        cue(10L, 0L, "通用")
                ));

        CueWordsQueryBo bo = baseBo(10L, 200L);

        // when
        CueWordsInfoVo result = cueWordsApi.getCueWordByTradeAndType(bo);

        // then：返回通用条目
        assertThat(result).isNotNull();
        assertThat(result.getProblem()).isEqualTo("通用");
        assertThat(result.getTenantId()).isEqualTo(0L);
    }

    // -------- AC-3 跨行业层级子赢父 --------

    @Test
    @DisplayName("AC-3: 跨行业层级，子层级通用赢父层级租户定制（按 tradeIds 顺序优先）")
    void testGetCueWord_childTradeBeatsParentEvenWithTenantCustomization() {
        // given：行业父链 [10, 1]；子层级（10）只有通用，父层级（1）有租户定制
        when(tradeProducer.listParentsByTradeId(eq(10L), anyInt()))
                .thenReturn(List.of(trade(10L), trade(1L)));
        when(cueWordsProducer.listByTradeIdsAndQuery(any(), any()))
                .thenReturn(List.of(
                        cue(10L, 0L, "子通用"),      // 子层级通用
                        cue(1L, 100L, "父租户定制")  // 父层级租户定制
                ));

        CueWordsQueryBo bo = baseBo(10L, 100L);

        // when
        CueWordsInfoVo result = cueWordsApi.getCueWordByTradeAndType(bo);

        // then：跨层级，子赢父 —— 即使父层级有租户定制也不胜出
        assertThat(result).isNotNull();
        assertThat(result.getProblem()).isEqualTo("子通用");
        assertThat(result.getTradeId()).isEqualTo(10L);
    }

    // -------- AC-4 全链路无记录返 null --------

    @Test
    @DisplayName("AC-4: 行业父链 + 兜底 1L 全无有效提示词，返回 null（不抛异常）")
    void testGetCueWord_noRecordReturnsNull() {
        // given：行业父链有数据；但 SPI 一条都查不到
        when(tradeProducer.listParentsByTradeId(eq(10L), anyInt()))
                .thenReturn(List.of(trade(10L), trade(1L)));
        when(cueWordsProducer.listByTradeIdsAndQuery(any(), any()))
                .thenReturn(List.of());

        CueWordsQueryBo bo = baseBo(10L, 100L);

        // when
        CueWordsInfoVo result = cueWordsApi.getCueWordByTradeAndType(bo);

        // then：Api 层返 null（上层 loadSingleCueWord 抛 BusinessException 已在 BllTest 验过）
        assertThat(result).isNull();
    }

    // -------- AC-5 tenantId=null 退化旧行为 --------

    @Test
    @DisplayName("AC-5: tenantId=null，退化为只查通用；SPI 调用时 bo.tenantId 透传为 null（由 SPI 自行处理退化）")
    void testGetCueWord_nullTenantIdFallsBackToLegacy() {
        // given：tenantId=null；行业父链 [10, 1]；只有通用条目
        when(tradeProducer.listParentsByTradeId(eq(10L), anyInt()))
                .thenReturn(List.of(trade(10L), trade(1L)));
        when(cueWordsProducer.listByTradeIdsAndQuery(any(), any()))
                .thenReturn(List.of(
                        cue(10L, 0L, "通用")
                ));

        CueWordsQueryBo bo = baseBo(10L, null);

        // when
        CueWordsInfoVo result = cueWordsApi.getCueWordByTradeAndType(bo);

        // then：返回通用条目
        assertThat(result).isNotNull();
        assertThat(result.getProblem()).isEqualTo("通用");

        // verify：SPI 被调时 bo.tenantId 仍为 null（透传给 SPI，由 SPI 决定 IN (?,0) 退化）
        ArgumentCaptor<CueWordsQueryBo> boCaptor = ArgumentCaptor.forClass(CueWordsQueryBo.class);
        verify(cueWordsProducer).listByTradeIdsAndQuery(any(), boCaptor.capture());
        assertThat(boCaptor.getValue().getTenantId()).isNull();
    }

    // -------- AC-6 cueType=null 抛 IllegalArgumentException --------

    @Test
    @DisplayName("AC-6: bo.cueType=null 抛 IllegalArgumentException，message 含 cueType 必填")
    void testGetCueWord_nullCueTypeThrowsIllegalArgumentException() {
        // given：cueType 不设
        CueWordsQueryBo bo = new CueWordsQueryBo();
        bo.setTradeId(10L);
        bo.setTenantId(100L);
        // bo.cueType = null

        // when + then：抛 IllegalArgumentException
        assertThatThrownBy(() -> cueWordsApi.getCueWordByTradeAndType(bo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cueType 必填");
    }
}
