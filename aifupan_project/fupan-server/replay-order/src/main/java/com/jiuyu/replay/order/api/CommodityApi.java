package com.jiuyu.replay.order.api;

import cn.hutool.core.collection.CollectionUtil;
import com.jiuyu.replay.generic.dto.order.CommodityDto;
import com.jiuyu.replay.generic.feign.order.CommodityFeign;
import com.jiuyu.replay.order.bll.CommodityBll;
import com.jiuyu.replay.order.bo.CommodityBo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author RayChou
 * @date 2025/5/30 9:43
 */
@Component
public class CommodityApi implements CommodityFeign {

    @Resource
    private CommodityBll commodityBll;

    @Override
    public List<CommodityDto> listAll() {
        List<CommodityBo> commodityBoList = commodityBll.listAll();
        if (CollectionUtil.isNotEmpty(commodityBoList)) {
            List<CommodityDto> commodityDtoList = new ArrayList<>();
            for (CommodityBo commodityBo : commodityBoList) {
                CommodityDto commodityDto = new CommodityDto();
                BeanUtils.copyProperties(commodityBo, commodityDto);
                commodityDtoList.add(commodityDto);
            }
            return commodityDtoList;
        }
        return null;
    }
}
