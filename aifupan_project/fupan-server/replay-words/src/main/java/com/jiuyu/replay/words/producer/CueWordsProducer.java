package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.generic.bo.words.CueWordsBo;
import com.jiuyu.replay.generic.bo.words.CueWordsListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.words.*;
import com.jiuyu.replay.generic.bo.words.cue.CueWordsPageBo;
import com.jiuyu.replay.generic.bo.words.cue.CueWordsQueryBo;

import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * 提示词
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-20 15:23:28
 */
public interface CueWordsProducer {


    /**
     * 提示词列表
     * @param cueWordsListBo 提示词列表查询参数
     * @return
     */
    PageUtils<CueWordsListVo> queryPage(CueWordsListBo cueWordsListBo);

    /**
    * 提示词信息
    * @param id 提示词id
    * @return
    */
    CueWordsInfoVo info(Long id);

    /**
     * 新增提示词
     * @param cueWordsBo 提示词对象
     * @return
     */
     CueWordsInfoVo save(CueWordsBo cueWordsBo);

    /**
     * 修改提示词
     * @param cueWordsBo 提示词对象
     * @return
     */
    void update(CueWordsBo cueWordsBo);

    /**
     * 删除提示词
     * @param id 提示词id
     * @return
     */
    void deleteById(Long id);

    /**
     * 根据行业id获取提示词
     * @param tradeId
     * @return
     */
    CueWordsInfoVo importantCueWordsByTradeId(List<TradeInfoVo> tradeId);

    /**
     * 根据id列表获取提示词
     * @param ids
     * @return
     */
    List<CueWordsInfoVo> listByIds(List<Long> ids);

    /**
     * 获取提示词的条数
     *
     * @param tradeId 行业
     * @param pageBo  查询参数
     * @return 提示词条数
     */
    long countByParams(Long tradeId, CueWordsPageBo pageBo);

    /**
     * 批量获取多个行业提示词的条数
     *
     * @param tradeIds 行业ID列表
     * @param pageBo   查询参数
     * @return 行业ID到计数的映射
     */
    @Deprecated
    Map<String, Integer> countByParamsBatch(List<Long> tradeIds, CueWordsPageBo pageBo);


    /**
     * 批量获取多个行业提示词的条数
     *
     * @param tradeIds 行业ID列表
     * @param pageBo   获取参数
     * @return 行业ID到计数的映射
     */
    Map<Long, Map<String, Integer>> countByTenantParamsBatch(List<Long> tradeIds, CueWordsPageBo pageBo);

    /**
     * 分页获取提示词
     *
     * @param pageBo 获取参数
     * @return 提示词列表
     */
    PageUtils<CueWordsListVo> pageCueWords(CueWordsPageBo pageBo);

    /**
     * 自定义获取提示词
     *
     * @param pageBo 获取参数
     *
     * @return {@link CustomizeCueWordsResponse }
     */
    CustomizeCueWordsResponse customizeCueWordsList(CueWordsPageBo pageBo);

    /**
     * 按行业 id 集合 + {@link CueWordsQueryBo} 取有效提示词列表（含 problem 字段，含 isDeleted=0 过滤）。
     *
     * <p>专供 {@link com.jiuyu.replay.words.api.CueWordsApi#getCueWordByTradeAndType} 使用：
     * 调用方传入已按子→父排序的 tradeIds 集合（含兜底 1L），一次 IN 查询拿到所有候选
     * 行业的有效提示词；后续命中策略（按 tradeIds 顺序选首个有数据的行业）由 API
     * 层在内存中决定。</p>
     *
     * <p>WHERE 条件：trade_id IN tradeIds AND cue_type = bo.cueType AND scope = bo.scope
     * AND apply_to = bo.applyTo AND account_type = bo.accountType
     * AND sync_scene = bo.syncScene(非空)
     * AND tenant_id IN (bo.tenantId, 0)(bo.tenantId 非空非 0；否则 tenant_id = 0)
     * AND is_deleted = 0；ORDER BY tenant_id DESC(同上前提), sort ASC。</p>
     *
     * @param tradeIds 候选行业 id 集合（包含父链 + 1L 兜底）；空时返回空列表
     * @param bo       查询入参，cueType 必填，其他维度有默认值
     * @return 全部命中的有效提示词列表（含 problem 字段，已排序）
     */
    List<CueWordsInfoVo> listByTradeIdsAndQuery(List<Long> tradeIds, CueWordsQueryBo bo);


    /**
     * 查询行业预设提示词
     *
     * @param tradeIds 行业ID
     * @param cueType ai助手类型（场景）
     * @param applyTo 提示词用于 0:单个分析，1:对比分析
     * @param accountType 账号归属类型 0:自有账号，1:同行账号
     * @param limit 返回条数
     * @return {@link List }<{@link CueWordsNameVo }>
     */
    List<CueWordsNameVo> listTradeWordNames(Collection<Long> tradeIds, Integer cueType, Integer applyTo, Integer accountType, Integer syncScene, int limit);


    /**
     * 查询提示词摘要
     *
     * @param id 提示词ID
     * @return {@link CueWordsDescVo }
     */
    CueWordsDescVo getWordDesc(long id);
}

