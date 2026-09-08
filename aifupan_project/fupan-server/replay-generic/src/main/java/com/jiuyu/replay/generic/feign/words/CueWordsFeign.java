package com.jiuyu.replay.generic.feign.words;

import com.jiuyu.replay.generic.bo.words.CueWordsListBo;
import com.jiuyu.replay.generic.bo.words.cue.CueWordsPageBo;
import com.jiuyu.replay.generic.bo.words.cue.CueWordsQueryBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.words.CueWordsInfoVo;
import com.jiuyu.replay.generic.vo.words.CueWordsListVo;

import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/30 上午11:31
 */
public interface CueWordsFeign {

    List<CueWordsInfoVo> listByIds(List<Long> ids);

    CueWordsInfoVo getById(Long id);

    PageUtils<CueWordsListVo> queryPage(CueWordsListBo cueWordsListBo);

    /**
     * 获取生成html的提示词详情
     *
     * @param tradeId    行业id
     * @param sourceId   源id
     * @param sourceType 源类型
     * @return 提示词详情
     */
    CueWordsInfoVo getHtmlPrompt(Long tradeId, String sourceId, Integer sourceType);

    /**
     * 分页获取提示词
     *
     * @param pageBo 获取参数
     * @return 提示词列表
     */
    PageUtils<CueWordsListVo> pageCueWords(CueWordsPageBo pageBo);

    /**
     * 按 {@link CueWordsQueryBo} 取单条有效提示词（含 problem 字段、已过滤 isDeleted=0）。
     *
     * <p><b>行业父链回溯 + 兜底</b>：{@code bo.tradeId} 不为空时取该行业所有父级，
     * 按子→父优先组装候选 tradeIds；末尾**永远追加 trade_id=1（通用行业）作为兜底**，
     * 确保即便传 null 或父链无配置也能命中通用提示词。</p>
     *
     * <p><b>同层级租户定制赢通用</b>：同一行业层级，{@code tenant_id = bo.tenantId} 赢
     * {@code tenant_id = 0}（SQL ORDER BY tenant_id DESC）。{@code bo.tenantId = null}
     * 或 {@code 0} 时退化为只查通用。</p>
     *
     * <p><b>跨层级子赢父</b>：API 层按 tradeIds 顺序选首个有命中的行业 — 即使父行业
     * 配了租户定制、子行业只有通用，依然返回子行业通用（不做跨层级租户优先）。</p>
     *
     * <p><b>其他维度</b>：{@code scope / applyTo / accountType / syncScene} 默认值见
     * {@link CueWordsQueryBo}（话术质检场景下保持默认即可）。</p>
     *
     * @param bo 查询入参，{@code cueType} 必填
     * @return 命中的单条提示词（含 problem）；候选行业全部无配置时返回 null
     * @throws IllegalArgumentException bo 或 bo.cueType 为 null
     */
    CueWordsInfoVo getCueWordByTradeAndType(CueWordsQueryBo bo);

}
