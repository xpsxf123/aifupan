package com.jiuyu.replay.order.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.order.bo.CommodityPriceBo;
import com.jiuyu.replay.order.bo.CommodityPriceListBo;
import com.jiuyu.replay.order.entity.CommodityPriceEntity;
import com.jiuyu.replay.order.producer.CommodityPriceProducer;
import com.jiuyu.replay.order.repository.service.CommodityPriceService;
import com.jiuyu.replay.order.vo.CommodityPriceInfoVo;
import com.jiuyu.replay.order.vo.CommodityPriceListVo;
import com.jiuyu.replay.order.vo.CommodityPriceVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * 商品价格
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Service
public class CommodityPriceProducerImpl implements CommodityPriceProducer {

    @Resource
    private CommodityPriceService commodityPriceService;


    @Override
    public PageUtils<CommodityPriceListVo> queryPage(CommodityPriceListBo commodityPriceListBo) {
        LambdaQueryWrapper<CommodityPriceEntity> wrapper = new LambdaQueryWrapper<CommodityPriceEntity>()
                .eq(ObjectUtil.isNotEmpty(commodityPriceListBo.getType()), CommodityPriceEntity::getType, commodityPriceListBo.getType())
                .eq(ObjectUtil.isNotEmpty(commodityPriceListBo.getCommodityId()), CommodityPriceEntity::getCommodityId, commodityPriceListBo.getCommodityId())
                .eq(ObjectUtil.isNotEmpty(commodityPriceListBo.getShowStatus()), CommodityPriceEntity::getShowStatus, commodityPriceListBo.getShowStatus())
                .eq(ObjectUtil.isNotEmpty(commodityPriceListBo.getTrialVersion()), CommodityPriceEntity::getTrialVersion, commodityPriceListBo.getTrialVersion());
        IPage<CommodityPriceEntity> iPage = commodityPriceService.page(new Query<CommodityPriceEntity>().getPage(commodityPriceListBo.getPage(), commodityPriceListBo.getLimit()), wrapper);

        PageUtils<CommodityPriceListVo> pageUtils = new PageUtils<>(commodityPriceListBo.getPage(), commodityPriceListBo.getLimit(), iPage);

        List<CommodityPriceEntity> records = iPage.getRecords();
        if (records != null && !records.isEmpty()) {
            pageUtils.setList(BeanUtil.copyToList(records, CommodityPriceListVo.class));
        }

        return pageUtils;
    }

    @Override
    public CommodityPriceInfoVo info(Long id) {

        CommodityPriceEntity commodityPriceEntity = commodityPriceService.getById(id);
        if(commodityPriceEntity != null) {
            CommodityPriceInfoVo commodityPriceInfoVo = new CommodityPriceInfoVo();
            BeanUtils.copyProperties(commodityPriceEntity, commodityPriceInfoVo);
            return commodityPriceInfoVo;
        }

        return null;
    }

    /**
     * 新增商品价格
     * @param commodityPriceBo 商品价格对象
     * @return
     */
     public CommodityPriceInfoVo save(CommodityPriceBo commodityPriceBo) {

         CommodityPriceEntity commodityPriceEntity = new CommodityPriceEntity();
         BeanUtils.copyProperties(commodityPriceBo, commodityPriceEntity);
         commodityPriceEntity.setId(SnowflakeManager.nextValue());
         commodityPriceEntity.setCreateDate(new Date());
         commodityPriceEntity.setUpdateDate(new Date());

         commodityPriceService.save(commodityPriceEntity);

         CommodityPriceInfoVo commodityPriceInfoVo = new CommodityPriceInfoVo();
         BeanUtils.copyProperties(commodityPriceEntity, commodityPriceInfoVo);

         return commodityPriceInfoVo;
     }

    @Override
    public List<CommodityPriceInfoVo> saveOrUpdateBatch(List<CommodityPriceBo> commodityPriceBoList) {
         if (ObjectUtil.isNotEmpty(commodityPriceBoList)){
             List<CommodityPriceEntity> list = commodityPriceBoList.stream().map(item -> {
                 Date date = new Date();
                 if (ObjectUtil.isEmpty(item.getId())){
                     item.setId(SnowflakeManager.nextValue());
                     item.setCreateDate(date);
                     item.setUpdateDate(date);
                 }else{
                     item.setUpdateDate(date);
                 }
                 return BeanUtil.copyProperties(item, CommodityPriceEntity.class);
             }).toList();
             List<CommodityPriceEntity> deleteList = list.stream().filter(item -> ObjectUtil.equal(item.getIsDeleted(), 1)).toList();
             if (ObjectUtil.isNotEmpty(deleteList)){
                 commodityPriceService.removeBatchByIds(deleteList.stream().map(CommodityPriceEntity::getId).toList());
             }

             List<CommodityPriceEntity> updateList = list.stream().filter(item -> !ObjectUtil.equal(item.getIsDeleted(), 1)).toList();
             if (ObjectUtil.isNotEmpty(updateList)){
                 commodityPriceService.saveOrUpdateBatch(updateList);
             }
             return BeanUtil.copyToList(list, CommodityPriceInfoVo.class);
         }
         return List.of();
    }

    /**
     * 修改商品价格
     * @param commodityPriceBo 商品价格对象
     * @return
     */
    public void update(CommodityPriceBo commodityPriceBo) {

        CommodityPriceEntity commodityPriceEntity = new CommodityPriceEntity();
        BeanUtils.copyProperties(commodityPriceBo, commodityPriceEntity);
        commodityPriceEntity.setUpdateDate(new Date());

        commodityPriceService.updateById(commodityPriceEntity);
    }

    /**
     * 删除商品价格
     * @param id 商品价格id
     * @return
     */
    public void deleteById(Long id) {

        commodityPriceService.removeById(id);
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        if (ObjectUtil.isEmpty(ids)) {
            return;
        }
        commodityPriceService.removeBatchByIds(ids);
    }

    @Override
    public void deleteByCommodityId(Long commodityId, Integer type) {
        commodityPriceService.remove(new LambdaQueryWrapper<CommodityPriceEntity>()
                .eq(CommodityPriceEntity::getCommodityId, commodityId)
                .eq(CommodityPriceEntity::getType, type)
        );
    }

    @Override
    public void deleteByCommodityId(List<Long> commodityId, Integer type) {
        commodityPriceService.remove(new LambdaQueryWrapper<CommodityPriceEntity>()
                .in(CommodityPriceEntity::getCommodityId, commodityId)
                .eq(CommodityPriceEntity::getType, type)
        );
    }

    @Override
    public List<CommodityPriceVo> listByCommodityId(Long commodityId, Integer type) {
        List<CommodityPriceEntity> list = commodityPriceService.list(new LambdaQueryWrapper<CommodityPriceEntity>()
                .eq(CommodityPriceEntity::getCommodityId, commodityId)
                .eq(CommodityPriceEntity::getType, type)
        );
        if (ObjectUtil.isNotEmpty(list)){
            return BeanUtil.copyToList(list, CommodityPriceVo.class);
        }
        return new ArrayList<>();
    }

    @Override
    public List<CommodityPriceVo> listByCommodityId(List<Long> commodityId, Integer type) {
        List<CommodityPriceEntity> list = commodityPriceService.list(new LambdaQueryWrapper<CommodityPriceEntity>()
                .eq(CommodityPriceEntity::getType, type)
                .in(CommodityPriceEntity::getCommodityId, commodityId)
        );
        if (ObjectUtil.isNotEmpty(list)) {
            return BeanUtil.copyToList(list, CommodityPriceVo.class);
        }
        return new ArrayList<>();
    }

    @Override
    public List<CommodityPriceVo> listByCommodityIdAndTrial(List<Long> commodityId, Integer type) {
        List<CommodityPriceEntity> list = commodityPriceService.list(new LambdaQueryWrapper<CommodityPriceEntity>()
                .eq(CommodityPriceEntity::getType, type)
                .eq(CommodityPriceEntity::getTrialVersion, 1)
                .in(CommodityPriceEntity::getCommodityId, commodityId)
        );
        if (ObjectUtil.isNotEmpty(list)){
            return BeanUtil.copyToList(list, CommodityPriceVo.class);
        }
        return new ArrayList<>();
    }

    @Override
    public List<CommodityPriceVo> listByIds(List<Long> ids) {
        List<CommodityPriceEntity> list = commodityPriceService.listByIds(ids);
        if (ObjectUtil.isNotEmpty(list)){
            return BeanUtil.copyToList(list, CommodityPriceVo.class);
        }
        return List.of();
    }

    @Override
    public List<CommodityPriceVo> listAll() {
        List<CommodityPriceEntity> list = commodityPriceService.list();
        if (ObjectUtil.isNotEmpty(list)) {
            return BeanUtil.copyToList(list, CommodityPriceVo.class);
        }
        return List.of();
    }
}

