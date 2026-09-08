package com.jiuyu.replay.order.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.order.bo.IncrementBo;
import com.jiuyu.replay.order.bo.IncrementListBo;
import com.jiuyu.replay.order.entity.CommodityEntity;
import com.jiuyu.replay.order.entity.CommodityTypeEntity;
import com.jiuyu.replay.order.entity.IncrementEntity;
import com.jiuyu.replay.order.producer.IncrementProducer;
import com.jiuyu.replay.order.repository.service.CommodityService;
import com.jiuyu.replay.order.repository.service.CommodityTypeService;
import com.jiuyu.replay.order.repository.service.IncrementService;
import com.jiuyu.replay.order.repository.service.impl.CommodityPriceServiceImpl;
import com.jiuyu.replay.order.vo.*;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 增量包表
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-09 16:13:50
 */
@Service
@AllArgsConstructor
public class IncrementProducerImpl implements IncrementProducer {

    private final IncrementService incrementService;
    private final CommodityService commodityService;
    private final CommodityTypeService commodityTypeService;
    private final CommodityPriceServiceImpl commodityPriceService;


    @Override
    public PageUtils<IncrementListVo> queryPage(IncrementListBo incrementListBo) {
        LambdaQueryWrapper<IncrementEntity> wrapper = new LambdaQueryWrapper<IncrementEntity>()
                .eq(ObjectUtil.isNotEmpty(incrementListBo.getCommodityId()), IncrementEntity::getCommodityId, incrementListBo.getCommodityId())
                .eq(ObjectUtil.isNotEmpty(incrementListBo.getPackageId()), IncrementEntity::getPackageId, incrementListBo.getPackageId())
                .eq(ObjectUtil.isNotEmpty(incrementListBo.getCommodityPriceId()), IncrementEntity::getCommodityPriceId, incrementListBo.getCommodityPriceId())
                .eq(ObjectUtil.isNotEmpty(incrementListBo.getStatus()), IncrementEntity::getStatus, incrementListBo.getStatus())
                ;

        IPage<IncrementEntity> iPage = incrementService.page(new Query<IncrementEntity>().getPage(incrementListBo.getPage(), incrementListBo.getLimit()), wrapper);

        PageUtils<IncrementListVo> pageUtils = new PageUtils<>(incrementListBo.getPage(), incrementListBo.getLimit(), iPage);

        List<IncrementEntity> records = iPage.getRecords();
        if(records != null && !records.isEmpty()) {
            List<IncrementListVo> vos = BeanUtil.copyToList(records, IncrementListVo.class);
            List<Long> commodityIds = vos.stream().map(IncrementVo::getCommodityId).distinct().toList();
            List<Long> priceIds = vos.stream().map(IncrementVo::getCommodityPriceId).distinct().toList();

            // 获取商品类型
            Map<Long, CommodityTypeEntity> typeMap = commodityTypeService.listAllByCache()
                    .stream()
                    .collect(Collectors.toMap(CommodityTypeEntity::getId, Function.identity(), (old, now) -> old));

            Map<Long, CommodityPriceVo> priceMap = ObjectUtil.isEmpty(priceIds) ? new HashMap<>() : commodityPriceService.listByIds(priceIds)
                    .stream()
                    .map(item -> BeanUtil.copyProperties(item, CommodityPriceVo.class))
                    .collect(Collectors.toMap(CommodityPriceVo::getId, Function.identity(), (old, now) -> now));

            // 获取增量包对应的商品
            Map<Long, CommodityInfoVo> commodityMap = commodityService.listByIds(commodityIds).stream().collect(Collectors.toMap(CommodityEntity::getId, item -> {
                CommodityInfoVo infoVo = BeanUtil.copyProperties(item, CommodityInfoVo.class);

                CommodityTypeEntity commodityType = typeMap.get(item.getCommodityTypeId());
                if (commodityType != null) {
                    infoVo.setCommodityTypeCode(commodityType.getCode());
                    infoVo.setCommodityTypeName(commodityType.getName());
                    infoVo.setCommodityTypeUnit(commodityType.getUnit());
                    infoVo.setCommodityType(BeanUtil.copyProperties(commodityType, CommodityTypeVo.class));
                }

                return infoVo;
            }, (old, now) -> old));

            // 绑定商品信息
            vos.forEach(item -> {
                item.setCommodityVo(commodityMap.get(item.getCommodityId()));
                item.setCommodityPrice(priceMap.get(item.getCommodityPriceId()));
            });

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public IncrementInfoVo info(Long id) {

        IncrementEntity incrementEntity = incrementService.getById(id);
        if(incrementEntity != null) {
            IncrementInfoVo incrementInfoVo = new IncrementInfoVo();
            BeanUtils.copyProperties(incrementEntity, incrementInfoVo);
            return incrementInfoVo;
        }

        return null;
    }

    /**
     * 新增增量包表
     * @param incrementBo 增量包表对象
     * @return
     */
     public IncrementInfoVo save(IncrementBo incrementBo) {

         IncrementEntity incrementEntity = new IncrementEntity();
         BeanUtils.copyProperties(incrementBo, incrementEntity);
         incrementEntity.setId(SnowflakeManager.nextValue());
         incrementEntity.setCreateDate(new Date());

         incrementService.save(incrementEntity);

         IncrementInfoVo incrementInfoVo = new IncrementInfoVo();
         BeanUtils.copyProperties(incrementEntity, incrementInfoVo);

         return incrementInfoVo;
     }

    /**
     * 修改增量包表
     * @param incrementBo 增量包表对象
     * @return
     */
    public void update(IncrementBo incrementBo) {

        IncrementEntity incrementEntity = new IncrementEntity();
        BeanUtils.copyProperties(incrementBo, incrementEntity);

        incrementService.updateById(incrementEntity);
    }

    /**
     * 删除增量包表
     * @param id 增量包表id
     * @return
     */
    public void deleteById(Long id) {

        incrementService.removeById(id);
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        if (ObjectUtil.isEmpty(ids)) {
            return;
        }
        incrementService.removeBatchByIds(ids);
    }

    @Override
    public void deleteByPackageId(Long packageId) {
        incrementService.remove(new LambdaQueryWrapper<IncrementEntity>().eq(IncrementEntity::getPackageId, packageId));
    }

    @Override
    public List<IncrementInfoVo> listByPackageId(Long packageId) {
        List<IncrementEntity> list = incrementService.list(new LambdaQueryWrapper<IncrementEntity>()
                .eq(IncrementEntity::getPackageId, packageId));
        return BeanUtil.copyToList(list, IncrementInfoVo.class);
    }

    @Override
    public List<IncrementInfoVo> listByPackageId(List<Long> packageId) {
        List<IncrementEntity> list = incrementService.list(new LambdaQueryWrapper<IncrementEntity>()
                .in(IncrementEntity::getPackageId, packageId));
        return BeanUtil.copyToList(list, IncrementInfoVo.class);
    }

    @Override
    public void saveBatch(List<IncrementBo> incrementList) {
        if (ObjectUtil.isNotEmpty(incrementList)){
            List<IncrementEntity> list = incrementList.stream().map(item -> {
                item.setId(SnowflakeManager.nextValue());
                item.setCreateDate(new Date());
                return BeanUtil.copyProperties(item, IncrementEntity.class);
            }).toList();
            incrementService.saveBatch(list);
        }
    }
}

