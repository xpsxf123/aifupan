package com.jiuyu.replay.order.rse.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuyu.replay.generic.vo.order.CommodityPriceVo;
import com.jiuyu.replay.generic.vo.order.PackageInfoVo;
import com.jiuyu.replay.order.entity.CommodityPriceEntity;
import com.jiuyu.replay.order.entity.PackageEntity;
import com.jiuyu.replay.order.repository.service.CommodityPriceService;
import com.jiuyu.replay.order.repository.service.PackageService;
import com.jiuyu.replay.order.rse.PackageRse;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PackageRseImpl implements PackageRse {

    @Resource
    private PackageService packageService;
    @Resource
    private CommodityPriceService commodityPriceService;

    @Override
    public List<PackageInfoVo> listAllActivity(Integer packageType) {

        QueryWrapper<PackageEntity> wrapper = new QueryWrapper<>();
        if(packageType != null) {
            wrapper.eq("package_type", packageType);
        }
        wrapper.eq("status", 1);
        List<PackageEntity> packageEntities = this.packageService.list(wrapper);

        if(packageEntities != null && packageEntities.size() > 0) {
            List<Long> packageIds = packageEntities.stream().map(PackageEntity::getId).toList();
            List<CommodityPriceEntity> commodityPriceEntities = this.commodityPriceService.list(new QueryWrapper<CommodityPriceEntity>().in("commodity_id", packageIds));

            List<PackageInfoVo> packageInfoVoList = packageEntities.stream().map(item -> {
                PackageInfoVo packageInfoVo = new PackageInfoVo();
                BeanUtils.copyProperties(item, packageInfoVo);
                // 封装套餐的价格列表
                List<CommodityPriceVo> commodityPriceList = new LinkedList<>();
                if (commodityPriceEntities != null && commodityPriceEntities.size() > 0) {
                    for (CommodityPriceEntity commodityPriceEntity : commodityPriceEntities) {
                        if (commodityPriceEntity.getCommodityId().equals(item.getId())) {
                            CommodityPriceVo commodityPriceVo = new CommodityPriceVo();
                            BeanUtils.copyProperties(commodityPriceEntity, commodityPriceVo);
                            commodityPriceList.add(commodityPriceVo);
                        }
                    }
                }
                packageInfoVo.setCommodityPriceList(commodityPriceList);

                return packageInfoVo;
            }).collect(Collectors.toList());

            return packageInfoVoList;
        }

        return null;
    }
}
