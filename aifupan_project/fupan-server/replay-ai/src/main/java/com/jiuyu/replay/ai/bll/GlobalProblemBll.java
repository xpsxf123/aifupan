package com.jiuyu.replay.ai.bll;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.ai.bo.GlobalProblemBo;
import com.jiuyu.replay.ai.bo.GlobalProblemListBo;
import com.jiuyu.replay.ai.rse.GlobalProblemRse;
import com.jiuyu.replay.ai.vo.GlobalProblemListVo;
import com.jiuyu.replay.generic.feign.words.TradeFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.words.TradeVo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局提示词 Bll
 *
 * @author lj
 * @date 2026-05-21
 */
@AllArgsConstructor
@Component
public class GlobalProblemBll {

    private final GlobalProblemRse globalProblemRse;
    private final TradeFeign tradeFeign;

    public PageUtils<GlobalProblemListVo> queryPage(GlobalProblemListBo listBo) {
        PageUtils<GlobalProblemListVo> pageUtils = globalProblemRse.queryPage(listBo);

        List<GlobalProblemListVo> list = pageUtils.getList();
        if (CollectionUtil.isNotEmpty(list)) {
            Set<Long> tradeIds = list.stream()
                    .map(GlobalProblemListVo::getTradeId)
                    .filter(ObjectUtil::isNotEmpty)
                    .collect(Collectors.toSet());
            if (CollectionUtil.isNotEmpty(tradeIds)) {
                List<TradeVo> tradeVos = tradeFeign.listTradeByIds(tradeIds.stream().toList());
                Map<Long, String> tradeNameMap = tradeVos.stream()
                        .collect(Collectors.toMap(TradeVo::getId, TradeVo::getName, (a, b) -> a));
                list.forEach(vo -> {
                    if (vo.getTradeId() != null) {
                        vo.setTradeName(tradeNameMap.get(vo.getTradeId()));
                    }
                });
            }
        }

        return pageUtils;
    }

    public GlobalProblemListVo info(Long id) {
        GlobalProblemListVo vo = globalProblemRse.info(id);
        if (vo != null && vo.getTradeId() != null) {
            List<TradeVo> tradeVos = tradeFeign.listTradeByIds(List.of(vo.getTradeId()));
            if (CollectionUtil.isNotEmpty(tradeVos)) {
                vo.setTradeName(tradeVos.get(0).getName());
            }
        }
        return vo;
    }

    public GlobalProblemListVo save(GlobalProblemBo bo) {
        return globalProblemRse.save(bo);
    }

    public void update(GlobalProblemBo bo) {
        globalProblemRse.update(bo);
    }

    public void delete(Long id) {
        globalProblemRse.deleteById(id);
    }

    public List<GlobalProblemListVo> listByConditions(Integer cueType, Integer applyTo, Integer problemType) {
        return globalProblemRse.listByConditions(cueType, applyTo, problemType);
    }
}
