package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.api.controller.openapi.governance.response.IdName;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.words.vo.AnchorTradeListVo;
import com.jiuyu.replay.generic.vo.words.TradeListVo;
import com.jiuyu.replay.generic.vo.words.TradeInfoVo;
import com.jiuyu.replay.words.bo.TradeBo;
import com.jiuyu.replay.words.bo.TradeListBo;
import com.jiuyu.replay.api.logic.words.TradeLogic;
import com.jiuyu.replay.words.bll.TradeBll;

import com.jiuyu.replay.words.vo.TradeSimpleTreeVo;
import com.jiuyu.replay.words.vo.TradeTreeVo;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;

import java.util.List;
import java.util.Map;


/**
 * 行业
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:22:18
 */
@Service
public class TradeLogicImpl implements TradeLogic {

    @Resource
    private TradeBll tradeBll;


    @Override
    public R<PageUtils<TradeListVo>> queryPage(TradeListBo tradeListBo) {

        return tradeBll.queryPage(tradeListBo);
    }

    @Override
    public R<TradeInfoVo> info(Long id) {

        return tradeBll.info(id);
    }

    @Override
    public R<String> save(TradeBo tradeBo) {

        return tradeBll.save(tradeBo);
    }

    @Override
    public R<String> update(TradeBo tradeBo) {

        return tradeBll.update(tradeBo);
    }

    @Override
    public R<String> delete(Long id) {

        return tradeBll.delete(id);
    }

    @Override
    public R<List<TradeTreeVo>> listTree(Integer childrenNotNull) {

        return tradeBll.listTree(childrenNotNull);
    }

    @Override
    public R<List<TradeSimpleTreeVo>> listSimpleTree(Integer childrenNotNull) {

        return tradeBll.listSimpleTree(childrenNotNull);
    }

    @Override
    public R<String> deleteTradeModel(Long tradeId, Long modelId) {
        return tradeBll.deleteTradeModel(tradeId,modelId);
    }

    @Override
    public R<List<TradeSimpleTreeVo>> listSimpleTreeL2() {
        R<List<TradeSimpleTreeVo>> result = tradeBll.listSimpleTree(null);
        if (result.getData() != null) {
            for (TradeSimpleTreeVo level1 : result.getData()) {
                if (level1.getChildren() != null) {
                    for (TradeSimpleTreeVo level2 : level1.getChildren()) {
                        level2.setChildren(null);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public R<List<AnchorTradeListVo>> listByAnchor() {

        UserCacheVo user = GlobalObject.getLocalUser();

        return tradeBll.listByAnchor(user.getId(), user.getActiveTenantId(), 0);
    }

    @Override
    public R<List<AnchorTradeListVo>> listByTenantAnchor() {
        UserCacheVo user = GlobalObject.getLocalUser();

        return tradeBll.listByAnchor(null, user.getActiveTenantId(), 1);
    }


    /**
     * 根据行业id返回行业名称
     *
     * @param tradeIds 行业ID
     *
     * @return {@link List }<{@link IdName }>
     */
    @Override
    public List<IdName> getTradeNames(List<Long> tradeIds) {
        Map<Long, String> tradeNames = tradeBll.getTradeNames(tradeIds);
        if (EmptyUtil.isEmpty(tradeNames)) {
            return List.of();
        }
        return tradeNames.entrySet().stream().map(entry -> new IdName(entry.getKey(), entry.getValue())).toList();
    }
}

