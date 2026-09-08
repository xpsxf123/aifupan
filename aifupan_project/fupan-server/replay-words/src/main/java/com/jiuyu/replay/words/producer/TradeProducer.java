package com.jiuyu.replay.words.producer;

import cn.hutool.core.lang.tree.Tree;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.words.bo.TradeBo;
import com.jiuyu.replay.words.bo.TradeListBo;
import com.jiuyu.replay.generic.vo.words.TradeInfoVo;
import com.jiuyu.replay.generic.vo.words.TradeListVo;
import com.jiuyu.replay.words.vo.TradeSimpleTreeVo;
import com.jiuyu.replay.words.vo.TradeTreeVo;
import com.jiuyu.replay.words.vo.TradeSimpleTreeVo;
import com.jiuyu.replay.generic.vo.words.TradeVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * 行业
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:17:24
 */
public interface TradeProducer {


    /**
     * 行业列表
     * @param tradeListBo 行业列表查询参数
     * @return
     */
    PageUtils<TradeListVo> queryPage(TradeListBo tradeListBo);

    /**
    * 行业信息
    * @param id 行业id
    * @return
    */
    TradeInfoVo info(Long id);

    /**
     * 新增行业
     * @param tradeBo 行业对象
     * @return
     */
     TradeInfoVo save(TradeBo tradeBo);

    /**
     * 修改行业
     * @param tradeBo 行业对象
     * @return
     */
    void update(TradeBo tradeBo);

    /**
     * 删除行业
     * @param id 行业id
     * @return
     */
    void deleteById(Long id);


    /**
     * 获取行业列表（树形结构）
     * @param childrenNotNull 当没有子行业时，子行业列表是否返回空集合 0：直接返回null 1：返回空集合
     * @return
     */
    List<TradeTreeVo> listTree(Integer childrenNotNull);

    /**
     * 获取行业列表（树形结构，简化版-客户端用）
     * @param childrenNotNull 当没有子行业时，子行业列表是否返回空集合 0：直接返回null 1：返回空集合
     * @return 只包含行业基础信息的树形结构
     */
    List<TradeSimpleTreeVo> listSimpleTree(Integer childrenNotNull);

    /**
     * 获取行业列表（树形结构）
     * 只有行业的数据，没有其他的数据
     *
     * @return 行业列表（树形结构）
     */
    List<Tree<Long>> listTreeTrade();

    /**
     * 根据行业id集合获取行业集合
     * @param tradeIds 行业id集合
     * @return
     */
    List<TradeListVo> listByIds(Collection<Long> tradeIds);

    /**
     * 根据行业名称查询
     * @param tradeName
     * @return
     */
    List<Long> selectByName(String tradeName);


    /**
     * 获取所有行业列表
     * @return
     */
    List<TradeVo> listAll();

    void deleteTradeModelId(Long tradeId);

    TradeBo getTradeByModelId(Long modelId);

    /**
     * 根据默认通用模型id获取行业列表
     * @param modelId
     * @return
     */
    List<TradeInfoVo> listByDefaultGeneralModelId(Long modelId);

    /**
     * 根据行业信息获取当前行业的所有父行业ids
     * @param tradeId
     * @param withGeneral
     * @return
     */
    List<TradeInfoVo> listParentsByTradeId(Long tradeId, Integer withGeneral);

    /**
     * 根据行业信息获取当前行业的所有父行业ids
     * @param tradeIds
     * @param withGeneral
     * @return
     */
    List<TradeInfoVo> listParentsByTradeIds(List<Long> tradeIds, Integer withGeneral);

    String getById(Long tradeId);

    List<Long> getChildById(Long aLong);

    /**
     * 根据行业id获取其所有子行业列表
     * @param tradeId 行业id
     * @param containerSelf 是否包含当前行业
     * @return
     */
    List<TradeInfoVo> listChildrenByTradeId(Long tradeId, boolean containerSelf);

    List<TradeVo> listTradeByIds(List<Long> ids);

    /**
     * 根据行业id获取名称
     *
     * @param tradeIds ID
     *
     * @return {@link Map }<{@link Long }, {@link String }>
     */
    Map<Long, String> getTradeNames(List<Long> tradeIds);
}

