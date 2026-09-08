package com.jiuyu.replay.order.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.utils.*;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.order.bo.CommodityBo;
import com.jiuyu.replay.order.bo.CommodityListBo;
import com.jiuyu.replay.order.entity.CommodityEntity;
import com.jiuyu.replay.order.entity.CommodityTypeEntity;
import com.jiuyu.replay.order.entity.IncrementEntity;
import com.jiuyu.replay.order.producer.CommodityProducer;
import com.jiuyu.replay.order.repository.service.CommodityService;
import com.jiuyu.replay.order.repository.service.CommodityTypeService;
import com.jiuyu.replay.order.repository.service.IncrementService;
import com.jiuyu.replay.order.vo.CommodityInfoVo;
import com.jiuyu.replay.order.vo.CommodityListVo;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 商品
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Service
@AllArgsConstructor
public class CommodityProducerImpl implements CommodityProducer {

    private final CommodityService commodityService;

    private final CommodityTypeService commodityTypeService;

    private final IncrementService incrementService;


    @Override
    public PageUtils<CommodityListVo> queryPage(CommodityListBo commodityListBo) {
        LambdaQueryWrapper<CommodityEntity> wrapper = new LambdaQueryWrapper<CommodityEntity>()
                .like(ObjectUtil.isNotEmpty(commodityListBo.getName()), CommodityEntity::getName, commodityListBo.getName())
                .eq(ObjectUtil.isNotEmpty(commodityListBo.getCommodityTypeId()), CommodityEntity::getCommodityTypeId, commodityListBo.getCommodityTypeId())
                .eq(ObjectUtil.isNotEmpty(commodityListBo.getStatus()), CommodityEntity::getStatus, commodityListBo.getStatus())
                ;

        IPage<CommodityEntity> page = new Query<CommodityEntity>().getPage(commodityListBo.getPage(), commodityListBo.getLimit());
        IPage<CommodityEntity> iPage = commodityService.page(page, wrapper);

        PageUtils<CommodityListVo> pageUtils = new PageUtils<>(commodityListBo.getPage(), commodityListBo.getLimit(), iPage);

        List<CommodityEntity> records = iPage.getRecords();
        if(records != null && !records.isEmpty()) {
            List<CommodityListVo> vos = records.stream().map(item -> {
                CommodityListVo commodityVo = new CommodityListVo();
                BeanUtils.copyProperties(item, commodityVo);
                return commodityVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public CommodityInfoVo info(Long id) {

        CommodityEntity commodityEntity = commodityService.getById(id);
        if(commodityEntity != null) {
            CommodityInfoVo commodityInfoVo = new CommodityInfoVo();
            BeanUtils.copyProperties(commodityEntity, commodityInfoVo);
            return commodityInfoVo;
        }

        return null;
    }

    /**
     * 新增商品
     * @param commodityBo 商品对象
     * @return
     */
     public CommodityInfoVo save(CommodityBo commodityBo) {

         CommodityEntity commodityEntity = new CommodityEntity();
         BeanUtils.copyProperties(commodityBo, commodityEntity);
         commodityEntity.setId(SnowflakeManager.nextValue());
         commodityEntity.setCreateDate(new Date());
         commodityEntity.setUpdateDate(new Date());

         commodityService.save(commodityEntity);

         CommodityInfoVo commodityInfoVo = new CommodityInfoVo();
         BeanUtils.copyProperties(commodityEntity, commodityInfoVo);

         return commodityInfoVo;
     }

    /**
     * 修改商品
     * @param commodityBo 商品对象
     * @return
     */
    public void update(CommodityBo commodityBo) {

        CommodityEntity commodityEntity = new CommodityEntity();
        BeanUtils.copyProperties(commodityBo, commodityEntity);
        commodityEntity.setUpdateDate(new Date());

        commodityService.updateById(commodityEntity);
    }

    @Override
    public void saveOrUpdateBatch(List<CommodityBo> commodityBoList) {
        if (ObjectUtil.isNotEmpty(commodityBoList)){
            List<CommodityEntity> list = commodityBoList.stream().map(item -> {
                if (ObjectUtil.isNotEmpty(item.getId())) {
                    item.setId(SnowflakeManager.nextValue());
                    item.setCreateDate(new Date());
                    item.setUpdateDate(new Date());
                } else {
                    item.setUpdateDate(new Date());
                }
                return BeanUtil.copyProperties(item, CommodityEntity.class);
            }).toList();

            commodityService.saveOrUpdateBatch(list);
        }
    }

    /**
     * 删除商品
     * @param id 商品id
     * @return
     */
    public void deleteById(Long id) {
        // 删除时先校验商品是否被使用
        long count = incrementService.count(new LambdaQueryWrapper<IncrementEntity>()
                .eq(IncrementEntity::getCommodityId, id)
        );
        if (count > 0){
            RRException.create("当前增量包在版本中存在使用，不能删除");
        }

        commodityService.removeById(id);
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        if (ObjectUtil.isEmpty(ids)) {
            return;
        }
        // 批量删除时先校验商品是否被使用
        long count = incrementService.count(new LambdaQueryWrapper<IncrementEntity>()
                .in(IncrementEntity::getCommodityId, ids)
        );
        if (count > 0) {
            RRException.create("当前增量包在版本中存在使用，不能删除");
        }

        commodityService.removeBatchByIds(ids);
    }

    @Override
    public List<CommodityInfoVo> listByIds(List<Long> ids) {
        List<CommodityEntity> list = commodityService.list(new LambdaQueryWrapper<CommodityEntity>()
                .in(CommodityEntity::getId, ids)
        );
        if (ObjectUtil.isNotEmpty(list)){
            return BeanUtil.copyToList(list, CommodityInfoVo.class);
        }
        return List.of();
    }

    @Override
    public List<CommodityInfoVo> listCommodityByIds(List<Long> ids) {
        List<CommodityEntity> list = commodityService.list(new LambdaQueryWrapper<CommodityEntity>()
                .in(CommodityEntity::getId, ids)
        );

        if (ObjectUtil.isNotEmpty(list)){
            List<CommodityInfoVo> result = BeanUtil.copyToList(list, CommodityInfoVo.class);
            List<Long> typeIds = list.stream().map(CommodityEntity::getCommodityTypeId).distinct().toList();
            List<CommodityTypeEntity> commodityTypeEntities = commodityTypeService.listByIds(typeIds);
            DataUtils.setFieldNameById(result, "commodityTypeId", "commodityTypeCode", commodityTypeEntities, "id", "code");
            DataUtils.setFieldNameById(result, "commodityTypeId", "commodityTypeName", commodityTypeEntities, "id", "name");
            DataUtils.setFieldNameById(result, "commodityTypeId", "commodityTypeUnit", commodityTypeEntities, "id", "unit");
            return result;
        }
        return List.of();
    }

    @Override
    public List<CommodityInfoVo> list(CommodityListBo commodityListBo) {
        LambdaQueryWrapper<CommodityEntity> wrapper = new LambdaQueryWrapper<CommodityEntity>()
                .like(ObjectUtil.isNotEmpty(commodityListBo.getName()), CommodityEntity::getName, commodityListBo.getName())
                .eq(ObjectUtil.isNotEmpty(commodityListBo.getCommodityTypeId()), CommodityEntity::getCommodityTypeId, commodityListBo.getCommodityTypeId())
                .eq(ObjectUtil.isNotEmpty(commodityListBo.getStatus()), CommodityEntity::getStatus, commodityListBo.getStatus())
                ;

        List<CommodityEntity> list = commodityService.list(wrapper);

        if(ObjectUtil.isNotEmpty(list)) {
            return BeanUtil.copyToList(list, CommodityInfoVo.class);
        }
        return List.of();
    }

    @Override
    public Boolean isDeletePriceId(Long priceId) {
        IncrementEntity one = incrementService.getOne(new LambdaQueryWrapper<IncrementEntity>()
                .eq(IncrementEntity::getCommodityPriceId, priceId)
                .last("limit 1")
        );
        return ObjectUtil.isEmpty(one);
    }

    @Override
    public List<CommodityInfoVo> listAll() {
        List<CommodityEntity> list = commodityService.list();
        if (ObjectUtil.isNotEmpty(list)) {
            return BeanUtil.copyToList(list, CommodityInfoVo.class);
        }
        return List.of();
    }
}

