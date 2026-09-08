package com.jiuyu.replay.order.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.order.bo.TypeConsumptionBo;
import com.jiuyu.replay.order.bo.TypeConsumptionListBo;
import com.jiuyu.replay.order.entity.CommodityTypeEntity;
import com.jiuyu.replay.order.entity.TypeConsumptionEntity;
import com.jiuyu.replay.order.producer.TypeConsumptionProducer;
import com.jiuyu.replay.order.repository.service.CommodityTypeService;
import com.jiuyu.replay.order.repository.service.TypeConsumptionService;
import com.jiuyu.replay.order.vo.TypeConsumptionInfoVo;
import com.jiuyu.replay.order.vo.TypeConsumptionListVo;
import com.jiuyu.replay.order.vo.TypeConsumptionVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


/**
 * 商品类型用量关联表
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Service
public class TypeConsumptionProducerImpl implements TypeConsumptionProducer {

    @Resource
    private TypeConsumptionService typeConsumptionService;

    @Resource
    private CommodityTypeService commodityTypeService;


    @Override
    public PageUtils<TypeConsumptionListVo> queryPage(TypeConsumptionListBo typeConsumptionListBo) {
        LambdaQueryWrapper<TypeConsumptionEntity> wrapper = new LambdaQueryWrapper<TypeConsumptionEntity>()
                .eq(ObjectUtil.isNotEmpty(typeConsumptionListBo.getCommodityTypeId()), TypeConsumptionEntity::getCommodityTypeId, typeConsumptionListBo.getCommodityTypeId())
                .eq(ObjectUtil.isNotEmpty(typeConsumptionListBo.getSourceId()), TypeConsumptionEntity::getSourceId, typeConsumptionListBo.getSourceId())
                .eq(ObjectUtil.isNotEmpty(typeConsumptionListBo.getType()), TypeConsumptionEntity::getType, typeConsumptionListBo.getType())
                .like(ObjectUtil.isNotEmpty(typeConsumptionListBo.getCommodityTypeName()), TypeConsumptionEntity::getCommodityTypeName, typeConsumptionListBo.getCommodityTypeName())
                .like(ObjectUtil.isNotEmpty(typeConsumptionListBo.getCommodityTypeCode()), TypeConsumptionEntity::getCommodityTypeCode, typeConsumptionListBo.getCommodityTypeCode());

        IPage<TypeConsumptionEntity> iPage = typeConsumptionService.page(new Query<TypeConsumptionEntity>().getPage(typeConsumptionListBo.getPage(), typeConsumptionListBo.getLimit()), wrapper);

        PageUtils<TypeConsumptionListVo> pageUtils = new PageUtils<>(typeConsumptionListBo.getPage(), typeConsumptionListBo.getLimit(), iPage);

        List<TypeConsumptionEntity> records = iPage.getRecords();
        if (records != null && !records.isEmpty()) {
            List<TypeConsumptionListVo> vos = records.stream().map(item -> {
                TypeConsumptionListVo typeConsumptionVo = new TypeConsumptionListVo();
                BeanUtils.copyProperties(item, typeConsumptionVo);
                return typeConsumptionVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public TypeConsumptionInfoVo info(Long id) {

        TypeConsumptionEntity typeConsumptionEntity = typeConsumptionService.getById(id);
        if (typeConsumptionEntity != null) {
            TypeConsumptionInfoVo typeConsumptionInfoVo = new TypeConsumptionInfoVo();
            BeanUtils.copyProperties(typeConsumptionEntity, typeConsumptionInfoVo);
            return typeConsumptionInfoVo;
        }

        return null;
    }

    /**
     * 新增商品类型用量关联表
     *
     * @param typeConsumptionBo 商品类型用量关联表对象
     * @return
     */
    @Override
    public TypeConsumptionInfoVo save(TypeConsumptionBo typeConsumptionBo) {

        TypeConsumptionEntity typeConsumptionEntity = new TypeConsumptionEntity();
        BeanUtils.copyProperties(typeConsumptionBo, typeConsumptionEntity);
        typeConsumptionEntity.setId(SnowflakeManager.nextValue());

        typeConsumptionService.save(typeConsumptionEntity);

        TypeConsumptionInfoVo typeConsumptionInfoVo = new TypeConsumptionInfoVo();
        BeanUtils.copyProperties(typeConsumptionEntity, typeConsumptionInfoVo);

        return typeConsumptionInfoVo;
    }

    @Override
    public List<TypeConsumptionVo> saveOrUpdateBatch(List<TypeConsumptionBo> typeConsumptionList) {
        List<TypeConsumptionEntity> list = typeConsumptionList.stream().map(item -> {
            TypeConsumptionEntity typeConsumptionEntity = new TypeConsumptionEntity();
            BeanUtils.copyProperties(item, typeConsumptionEntity);
            if (ObjectUtil.isEmpty(item.getId())){
                typeConsumptionEntity.setId(SnowflakeManager.nextValue());
            }
            return typeConsumptionEntity;
        }).collect(Collectors.toList());
        if (typeConsumptionService.saveOrUpdateBatch(list)) {
            return BeanUtil.copyToList(list, TypeConsumptionVo.class);
        }
        return List.of();
    }

    /**
     * 修改商品类型用量关联表
     *
     * @param typeConsumptionBo 商品类型用量关联表对象
     * @return
     */
    public void update(TypeConsumptionBo typeConsumptionBo) {

        TypeConsumptionEntity typeConsumptionEntity = new TypeConsumptionEntity();
        BeanUtils.copyProperties(typeConsumptionBo, typeConsumptionEntity);

        typeConsumptionService.updateById(typeConsumptionEntity);
    }

    /**
     * 删除商品类型用量关联表
     *
     * @param id 商品类型用量关联表id
     * @return
     */
    public void deleteById(Long id) {

        typeConsumptionService.removeById(id);
    }

    @Override
    public List<TypeConsumptionVo> listBySourceId(Long sourceId, Integer type) {
        List<TypeConsumptionEntity> list = typeConsumptionService.list(new LambdaQueryWrapper<TypeConsumptionEntity>()
                .eq(TypeConsumptionEntity::getSourceId, sourceId)
                .eq(TypeConsumptionEntity::getType, type)
        );
        if (ObjectUtil.isNotEmpty(list)) {
            return BeanUtil.copyToList(list, TypeConsumptionVo.class);
        }
        return List.of();
    }

    @Override
    public List<TypeConsumptionVo> listBySourceId(List<Long> sourceId, Integer type) {
        List<TypeConsumptionEntity> list = typeConsumptionService.list(new LambdaQueryWrapper<TypeConsumptionEntity>()
                .in(TypeConsumptionEntity::getSourceId, sourceId)
                .eq(TypeConsumptionEntity::getType, type)
        );
        if (ObjectUtil.isNotEmpty(list)) {
            return BeanUtil.copyToList(list, TypeConsumptionVo.class);
        }
        return List.of();
    }

    @Override
    public void deleteByIds(Long commodityId, int type) {
        typeConsumptionService.remove(new LambdaQueryWrapper<TypeConsumptionEntity>()
                .eq(TypeConsumptionEntity::getSourceId, commodityId)
                .eq(TypeConsumptionEntity::getType, type)
        );
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        if (ObjectUtil.isEmpty(ids)) {
            return;
        }
        typeConsumptionService.remove(new LambdaQueryWrapper<TypeConsumptionEntity>()
                .in(TypeConsumptionEntity::getId, ids)
        );
    }

    @Override
    public TypeConsumptionInfoVo saveOrUpdateWithCommodityType(TypeConsumptionBo typeConsumptionBo) {
        // 校验
        long count = typeConsumptionService.count(new LambdaQueryWrapper<TypeConsumptionEntity>()
                .eq(TypeConsumptionEntity::getSourceId, typeConsumptionBo.getSourceId())
                .eq(TypeConsumptionEntity::getCommodityTypeId, typeConsumptionBo.getCommodityTypeId())
                .ne(ObjectUtil.isNotEmpty(typeConsumptionBo.getId()), TypeConsumptionEntity::getId, typeConsumptionBo.getId())
        );
        if (count > 0) {
            throw new BusinessException(StatusCode.OPERATION_EX.getCode(), "已存在同类型的商品，不能重复添加");
        }

        // 从 tb_commodity_type 表获取对应的字段值
        CommodityTypeEntity commodityTypeEntity = commodityTypeService.getById(typeConsumptionBo.getCommodityTypeId());
        if (ObjectUtil.isNotEmpty(commodityTypeEntity)) {
            typeConsumptionBo.setCommodityTypeId(commodityTypeEntity.getId());
            typeConsumptionBo.setCommodityTypeCode(commodityTypeEntity.getCode());
            typeConsumptionBo.setCommodityTypeName(commodityTypeEntity.getName());
            typeConsumptionBo.setCommodityTypeUnit(commodityTypeEntity.getUnit());
            typeConsumptionBo.setCommodityTypeReset(commodityTypeEntity.getIsReset());
        }

        // 判断新增或修改
        if (ObjectUtil.isEmpty(typeConsumptionBo.getId())) {
            // 新增
            return save(typeConsumptionBo);
        } else {
            // 修改
            update(typeConsumptionBo);
            return info(typeConsumptionBo.getId());
        }
    }
}

