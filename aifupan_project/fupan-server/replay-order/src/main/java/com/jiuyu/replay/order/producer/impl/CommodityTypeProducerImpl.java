package com.jiuyu.replay.order.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.order.bo.CommodityTypeBo;
import com.jiuyu.replay.order.bo.CommodityTypeListBo;
import com.jiuyu.replay.order.entity.*;
import com.jiuyu.replay.order.producer.CommodityTypeProducer;
import com.jiuyu.replay.order.repository.service.*;
import com.jiuyu.replay.order.vo.CommodityTypeInfoVo;
import com.jiuyu.replay.order.vo.CommodityTypeListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 商品类型
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-19 20:52:26
 */
@Service
public class CommodityTypeProducerImpl implements CommodityTypeProducer {

    @Resource
    private CommodityTypeService commodityTypeService;
    @Resource
    private UserPropertyService userPropertyService;
    @Resource
    private UserPropertyTypeService userPropertyTypeService;
    @Resource
    private OrderDetailService orderDetailService;
    @Resource
    private TypeConsumptionService typeConsumptionService;
    @Resource
    private TypeSurplusService typeSurplusService;
    @Resource
    private UserFeign userFeign;

    @Resource
    private RedisTemplate redisTemplate;


    @Override
    public PageUtils<CommodityTypeListVo> queryPage(CommodityTypeListBo commodityTypeListBo) {
        QueryWrapper<CommodityTypeEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(commodityTypeListBo.getKeyword())){
            wrapper.like("name", commodityTypeListBo.getKeyword());
        }

        IPage<CommodityTypeEntity> iPage = commodityTypeService.page(new Query<CommodityTypeEntity>().getPage(commodityTypeListBo.getPage(), commodityTypeListBo.getLimit()), wrapper);

        PageUtils<CommodityTypeListVo> pageUtils = new PageUtils<>(commodityTypeListBo.getPage(), commodityTypeListBo.getLimit(), iPage);

        List<CommodityTypeEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<CommodityTypeListVo> vos = records.stream().map(item -> {
                CommodityTypeListVo commodityTypeVo = new CommodityTypeListVo();
                BeanUtils.copyProperties(item, commodityTypeVo);
                return commodityTypeVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public List<CommodityTypeListVo> list(QueryWrapper<CommodityTypeEntity> queryWrapper) {
        List<CommodityTypeEntity> list = commodityTypeService.list(queryWrapper);
        if (ObjectUtil.isNotEmpty(list)){
            return BeanUtil.copyToList(list, CommodityTypeListVo.class);
        }
        return new ArrayList<>();
    }

    @Override
    public CommodityTypeInfoVo info(Long id) {
        CommodityTypeEntity commodityTypeEntity = commodityTypeService.getById(id);
        if(commodityTypeEntity != null) {
            CommodityTypeInfoVo commodityTypeInfoVo = new CommodityTypeInfoVo();
            BeanUtils.copyProperties(commodityTypeEntity, commodityTypeInfoVo);
            return commodityTypeInfoVo;
        }
        return null;
    }

    /**
     * 新增商品类型
     * @param commodityTypeBo 商品类型对象
     * @return
     */
     public CommodityTypeInfoVo save(CommodityTypeBo commodityTypeBo) {

         CommodityTypeEntity commodityTypeEntity = new CommodityTypeEntity();
         BeanUtils.copyProperties(commodityTypeBo, commodityTypeEntity);
         commodityTypeEntity.setId(SnowflakeManager.nextValue());
         commodityTypeEntity.setCreateDate(new Date());
         commodityTypeEntity.setUpdateDate(new Date());

         boolean save = commodityTypeService.save(commodityTypeEntity);
         if (save){
             CommodityTypeEntity temp = commodityTypeService.getById(commodityTypeEntity.getId());
             CommodityTypeInfoVo commodityTypeInfoVo = new CommodityTypeInfoVo();
             BeanUtils.copyProperties(temp, CommodityTypeInfoVo.class);
             return commodityTypeInfoVo;
         }
         return null;
     }

    /**
     * 修改商品类型
     * @param commodityTypeBo 商品类型对象
     * @return
     */
    public void update(CommodityTypeBo commodityTypeBo) {

        CommodityTypeEntity commodityTypeEntity = new CommodityTypeEntity();
        BeanUtils.copyProperties(commodityTypeBo, commodityTypeEntity);
        commodityTypeEntity.setUpdateDate(new Date());
        commodityTypeService.updateById(commodityTypeEntity);
    }

    /**
     * 删除商品类型
     * @param id 商品类型id
     * @return
     */
    public void deleteById(Long id) {
        commodityTypeService.removeById(id);
    }

    @Override
    public List<CommodityTypeInfoVo> listByIds(List<Long> ids) {
        List<CommodityTypeEntity> list = commodityTypeService.list(new LambdaQueryWrapper<CommodityTypeEntity>()
                .in(CommodityTypeEntity::getId, ids)
        );
        if (ObjectUtil.isNotEmpty(list)){
            return BeanUtil.copyToList(list, CommodityTypeInfoVo.class);
        }
        return List.of();
    }

    @Override
    public void synchronousUserAssets(Long id) {
        CommodityTypeEntity commodityType = commodityTypeService.getById(id);
        Date now = new Date();

        // tb_user_property_type表
        userPropertyTypeService.update(new LambdaUpdateWrapper<UserPropertyTypeEntity>()
                .eq(UserPropertyTypeEntity::getCommodityTypeId, id)
                .set(UserPropertyTypeEntity::getCommodityTypeCode, commodityType.getCode())
                .set(UserPropertyTypeEntity::getCommodityTypeName, commodityType.getName())
                .set(UserPropertyTypeEntity::getCommodityTypeUnit, commodityType.getUnit())
                .set(UserPropertyTypeEntity::getCommodityTypeReset, commodityType.getIsReset())
                .set(UserPropertyTypeEntity::getUpdateDate, now)
        );

        List<Long> userIds = userFeign.listUserIdsAll();
        if (ObjectUtil.isNotEmpty(userIds)) {
            List<List<Long>> partition = ListUtil.partition(userIds, 150);

            for (List<Long> ids : partition) {
                // 查询用户资产是否有这个类型
                List<UserPropertyTypeEntity> list = userPropertyTypeService.list(new LambdaQueryWrapper<UserPropertyTypeEntity>()
                        .in(UserPropertyTypeEntity::getUserId, ids)
                );

                Map<Long, List<UserPropertyTypeEntity>> map = list.stream().collect(Collectors.groupingBy(UserPropertyTypeEntity::getPropertyId));

                ArrayList<UserPropertyTypeEntity> result = new ArrayList<>();
                map.forEach((key, value) -> {
                    if (value.stream().noneMatch(item -> item.getCommodityTypeId().equals(id))) {
                        UserPropertyTypeEntity userPropertyTypeEntity = value.get(0);
                        UserPropertyTypeEntity type = new UserPropertyTypeEntity();
                        type.setId(SnowflakeManager.nextValue());
                        type.setUserId(userPropertyTypeEntity.getUserId());
                        type.setParentId(userPropertyTypeEntity.getParentId());
                        type.setPropertyId(userPropertyTypeEntity.getPropertyId());
                        type.setCommodityTypeId(id);
                        type.setCommodityTypeCode(commodityType.getCode());
                        type.setCommodityTypeName(commodityType.getName());
                        type.setCommodityTypeUnit(commodityType.getUnit());
                        type.setCommodityTypeReset(commodityType.getIsReset());
                        type.setUseQuantity(0L);
                        type.setTotalQuantity(0L);
                        type.setTotalUseQuantity(0L);
                        type.setCreateDate(now);
                        type.setUpdateDate(now);
                        result.add(type);
                    }
                });
                // 为用户添加资产类型
                if (ObjectUtil.isNotEmpty(result)) userPropertyTypeService.saveBatch(result);
            }
        }

        // tb_order_detail表
        orderDetailService.update(new LambdaUpdateWrapper<OrderDetailEntity>()
                .eq(OrderDetailEntity::getCommodityTypeId, id)
                .set(OrderDetailEntity::getCommodityTypeCode, commodityType.getCode())
                .set(OrderDetailEntity::getCommodityTypeName, commodityType.getName())
                .set(OrderDetailEntity::getCommodityTypeUnit, commodityType.getUnit())
                .set(OrderDetailEntity::getCommodityTypeReset, commodityType.getIsReset())
                .set(OrderDetailEntity::getUpdateDate, now)
        );

        // tb_type_consumption表
        typeConsumptionService.update(new LambdaUpdateWrapper<TypeConsumptionEntity>()
                .eq(TypeConsumptionEntity::getCommodityTypeId, id)
                .set(TypeConsumptionEntity::getCommodityTypeCode, commodityType.getCode())
                .set(TypeConsumptionEntity::getCommodityTypeName, commodityType.getName())
                .set(TypeConsumptionEntity::getCommodityTypeUnit, commodityType.getUnit())
                .set(TypeConsumptionEntity::getCommodityTypeReset, commodityType.getIsReset())
        );

        // tb_type_surplus表
        typeSurplusService.update(new LambdaUpdateWrapper<TypeSurplusEntity>()
                .eq(TypeSurplusEntity::getCommodityTypeId, id)
                .set(TypeSurplusEntity::getCommodityTypeCode, commodityType.getCode())
        );

        // 处理sub_account_have字段
        if (commodityType.getSubAccountHave() == 0){
            userPropertyTypeService.setPropertyParentId(commodityType.getCode());
        }else{
            userPropertyTypeService.setPropertyParentIdToNull(commodityType.getCode());
        }

        // 设置缓存
//        if (commodityType.getSubAccountHave() == 0){
//            redisTemplate.delete(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeCacheKey, "*", commodityType.getCode()));
//            redisTemplate.delete(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeTotalCacheKey, "*", commodityType.getCode()));
//        }else{
//            List<UserPropertyTypeEntity> entityList = userPropertyTypeService.list(new LambdaQueryWrapper<UserPropertyTypeEntity>()
//                    .eq(UserPropertyTypeEntity::getCommodityTypeCode, commodityType.getCode())
//            );
//            if (ObjectUtil.isNotEmpty(entityList)){
//                entityList.forEach(item -> {
//                    redisTemplate.opsForValue().set(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeCacheKey, item.getUserId(), item.getCommodityTypeCode()), item.getUseQuantity());
//                    redisTemplate.opsForValue().set(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeTotalCacheKey, item.getUserId(), item.getCommodityTypeCode()), item.getTotalQuantity());
//                });
//            }
//        }

    }

    @Override
    public CommodityTypeInfoVo getByCode(String code) {
        CommodityTypeEntity one = commodityTypeService.getOne(new LambdaQueryWrapper<CommodityTypeEntity>()
                .eq(CommodityTypeEntity::getCode, code)
                .last("limit 1")
        );
        if (ObjectUtil.isNotEmpty(one)) {
            return BeanUtil.copyProperties(one, CommodityTypeInfoVo.class);
        }
        return null;
    }

    @Override
    public List<CommodityTypeEntity> listAll() {
        return commodityTypeService.listAllByCache();
    }
}

