package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.api.controller.openapi.governance.response.IdName;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.words.vo.AnchorTradeListVo;
import com.jiuyu.replay.generic.vo.words.TradeListVo;
import com.jiuyu.replay.generic.vo.words.TradeInfoVo;
import com.jiuyu.replay.words.bo.TradeBo;
import com.jiuyu.replay.words.bo.TradeListBo;
import com.jiuyu.replay.words.vo.TradeSimpleTreeVo;
import com.jiuyu.replay.words.vo.TradeTreeVo;

import java.util.List;


/**
 * 行业
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:22:18
 */
public interface TradeLogic {


    /**
     * 行业列表
     * @param tradeListBo 行业列表查询参数
     * @return
     */
    R<PageUtils<TradeListVo>> queryPage(TradeListBo tradeListBo);

    /**
    * 行业信息
    * @param id 行业id
    * @return
    */
    R<TradeInfoVo> info(Long id);

    /**
     * 新增行业
     * @param tradeBo 行业对象
     * @return
     */
    R<String> save(TradeBo tradeBo);

    /**
     * 修改行业
     * @param tradeBo 行业对象
     * @return
     */
    R<String> update(TradeBo tradeBo);

    /**
     * 删除行业
     * @param id 行业id
     * @return
     */
    R<String> delete(Long id);


    /**
     * 获取行业列表（树形结构）
     * @param childrenNotNull 当没有子行业时，子行业列表是否返回空集合 0：直接返回null 1：返回空集合
     * @return
     */
    R<List<TradeTreeVo>> listTree(Integer childrenNotNull);

    /**
     * 获取行业列表（树形结构，简化版-客户端用）
     * @param childrenNotNull 当没有子行业时，子行业列表是否返回空集合 0：直接返回null 1：返回空集合
     * @return
     */
    R<List<TradeSimpleTreeVo>> listSimpleTree(Integer childrenNotNull);

    R<String> deleteTradeModel(Long tradeId, Long modelId);

    /**
     * 获取一二级行业树形结构（二级后清空）
     * @return
     */
    R<List<TradeSimpleTreeVo>> listSimpleTreeL2();

    /**
     * 根据用户添加的主播返回行业列表
     * @return
     */
    R<List<AnchorTradeListVo>> listByAnchor();

    /**
     * 根据用户租户主播返回行业列表
     * @return
     */
    R<List<AnchorTradeListVo>> listByTenantAnchor();

    /**
     * 根据行业id返回行业名称
     *
     * @param tradeIds 行业ID
     *
     * @return {@link List }<{@link IdName }>
     */
    List<IdName> getTradeNames(List<Long> tradeIds);
}

