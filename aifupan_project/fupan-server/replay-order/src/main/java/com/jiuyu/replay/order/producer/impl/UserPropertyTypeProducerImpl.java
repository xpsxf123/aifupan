package com.jiuyu.replay.order.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.order.bo.UserPropertyTypeBo;
import com.jiuyu.replay.order.bo.UserPropertyTypeListBo;
import com.jiuyu.replay.order.entity.UserPropertyTypeEntity;
import com.jiuyu.replay.order.producer.UserPropertyTypeProducer;
import com.jiuyu.replay.order.repository.service.UserPropertyTypeService;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 用户资产类型总明细
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
@Service
public class UserPropertyTypeProducerImpl implements UserPropertyTypeProducer {

    @Resource
    private UserPropertyTypeService userPropertyTypeService;
    @Autowired
    private SortHandlerMethodArgumentResolver sortResolver;


    @Override
    public PageUtils<UserPropertyTypeListVo> queryPage(UserPropertyTypeListBo userPropertyTypeListBo) {
        QueryWrapper<UserPropertyTypeEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(userPropertyTypeListBo.getKeyword())){
            wrapper.like("name", userPropertyTypeListBo.getKeyword());
        }

        IPage<UserPropertyTypeEntity> iPage = userPropertyTypeService.page(new Query<UserPropertyTypeEntity>().getPage(userPropertyTypeListBo.getPage(), userPropertyTypeListBo.getLimit()), wrapper);

        PageUtils<UserPropertyTypeListVo> pageUtils = new PageUtils<>(userPropertyTypeListBo.getPage(), userPropertyTypeListBo.getLimit(), iPage);

        List<UserPropertyTypeEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<UserPropertyTypeListVo> vos = records.stream().map(item -> {
                UserPropertyTypeListVo userPropertyTypeVo = new UserPropertyTypeListVo();
                BeanUtils.copyProperties(item, userPropertyTypeVo);
                return userPropertyTypeVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public UserPropertyTypeInfoVo info(Long id) {

        UserPropertyTypeEntity userPropertyTypeEntity = userPropertyTypeService.getById(id);
        if(userPropertyTypeEntity != null) {
            UserPropertyTypeInfoVo userPropertyTypeInfoVo = new UserPropertyTypeInfoVo();
            BeanUtils.copyProperties(userPropertyTypeEntity, userPropertyTypeInfoVo);
            return userPropertyTypeInfoVo;
        }

        return null;
    }

    /**
     * 新增用户资产类型总明细
     * @param userPropertyTypeBo 用户资产类型总明细对象
     * @return
     */
     public UserPropertyTypeInfoVo save(UserPropertyTypeBo userPropertyTypeBo) {

         UserPropertyTypeEntity userPropertyTypeEntity = new UserPropertyTypeEntity();
         BeanUtils.copyProperties(userPropertyTypeBo, userPropertyTypeEntity);
         userPropertyTypeEntity.setId(SnowflakeManager.nextValue());
         userPropertyTypeEntity.setCreateDate(new Date());
         userPropertyTypeEntity.setUpdateDate(new Date());

         userPropertyTypeService.save(userPropertyTypeEntity);

         UserPropertyTypeInfoVo userPropertyTypeInfoVo = new UserPropertyTypeInfoVo();
         BeanUtils.copyProperties(userPropertyTypeEntity, userPropertyTypeInfoVo);

         return userPropertyTypeInfoVo;
     }

    /**
     * 修改用户资产类型总明细
     * @param userPropertyTypeBo 用户资产类型总明细对象
     * @return
     */
    public void update(UserPropertyTypeBo userPropertyTypeBo) {

        UserPropertyTypeEntity userPropertyTypeEntity = new UserPropertyTypeEntity();
        BeanUtils.copyProperties(userPropertyTypeBo, userPropertyTypeEntity);
        userPropertyTypeEntity.setUpdateDate(new Date());

        userPropertyTypeService.updateById(userPropertyTypeEntity);
    }

    /**
     * 删除用户资产类型总明细
     * @param id 用户资产类型总明细id
     * @return
     */
    public void deleteById(Long id) {

        userPropertyTypeService.removeById(id);
    }

    @Override
    public UserPropertyTypeInfoVo getUserSRemainingAssets(Long propertyId, String commodityCode) {
        UserPropertyTypeEntity my = userPropertyTypeService.getOne(new LambdaQueryWrapper<UserPropertyTypeEntity>()
                .eq(UserPropertyTypeEntity::getPropertyId, propertyId)
                .eq(UserPropertyTypeEntity::getCommodityTypeCode, commodityCode)
                .last("limit 1")
        );
        if (my != null){
            if (my.getParentId() != null && my.getParentId() != 0){
                UserPropertyTypeEntity result = userPropertyTypeService.getById(my.getParentId());
                if (result != null) return BeanUtil.copyProperties(result, UserPropertyTypeInfoVo.class);
            }else{
                return BeanUtil.copyProperties(my, UserPropertyTypeInfoVo.class);
            }
        }
        return null;
    }

    @Override
    public void updateCurrent(Long id, Long currentSurplusCount) {
        if (id == null || currentSurplusCount == null) {
            throw new RRException("参数错误");
        }
        userPropertyTypeService.lambdaUpdate()
            .set(UserPropertyTypeEntity::getUseQuantity, currentSurplusCount)
            .eq(UserPropertyTypeEntity::getId, id).set(UserPropertyTypeEntity::getUpdateDate, new Date())
            .update();
    }

    @Override
    public List<UserPropertyTypeInfoVo> getPropertyByPropertyId(Long propertyId) {
        LambdaQueryWrapper<UserPropertyTypeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPropertyTypeEntity::getPropertyId, propertyId);
        List<UserPropertyTypeEntity> list = userPropertyTypeService.list(wrapper);
        if (ObjectUtil.isNotEmpty(list)){
            List<UserPropertyTypeInfoVo> result = BeanUtil.copyToList(list, UserPropertyTypeInfoVo.class);
            List<Long> ids = list.stream()
                    .filter(item -> item.getParentId() != null && item.getParentId() != 0)
                    .map(UserPropertyTypeEntity::getParentId).distinct().toList();
            if (ObjectUtil.isNotEmpty(ids)){
                List<UserPropertyTypeEntity> userPropertyTypeEntities = userPropertyTypeService.listByIds(ids);
                if (ObjectUtil.isNotEmpty(userPropertyTypeEntities)){
                    result.forEach(item -> {
                        UserPropertyTypeEntity entity = userPropertyTypeEntities.stream()
                                .filter(userPropertyTypeEntity -> userPropertyTypeEntity.getId().equals(item.getParentId()))
                                .findFirst()
                                .orElse(null);
                        if (entity != null){
                            item.setUseQuantity(entity.getUseQuantity());
                            item.setTotalQuantity(entity.getTotalQuantity());
                        }
                    });
                }
            }
            // 过滤子账号带child_前缀的数据
            result = result.stream()
                    .filter(item -> item.getParentId() == null || item.getParentId() == 0 || !item.getCommodityTypeCode().startsWith("child_") || !item.getCommodityTypeCode().equals("subAccountCount"))
                    .toList();
            return result;
        }
        return List.of();
    }


    /**
     * 根据用户id和用户资产id获取用户资产类型(parentId=0)信息
     * @param userId
     * @param propertyId
     * @return
     */
    @Override
    public List<UserPropertyTypeListVo> clintGetTypeData(Long userId, Long propertyId){
        List<UserPropertyTypeEntity> list = userPropertyTypeService.lambdaQuery()
                //用户id
                .eq(UserPropertyTypeEntity::getUserId, userId)
                //用户资产id
                .eq(UserPropertyTypeEntity::getPropertyId, propertyId).list();
        List<UserPropertyTypeEntity> zeros =new ArrayList<>();
        if (!list.isEmpty()){
            //进行分组收集 分为资产类型parentId=0 和 非0 的结果
            toGroup(zeros,list);
        }
        if (!zeros.isEmpty()){
            //返回资产类型结果
            return Convert.toList(UserPropertyTypeListVo.class ,zeros);
        }
        return new ArrayList<>();
    }


    /**
     * 获取用户剩余资产
     *
     * @param userIds       用户ID
     * @param commodityCode 资产类型
     *
     * @return {@link Map }<{@link Long }, {@link Long }> key 用户ID value 剩余资产
     */
    @Override
    public Map<Long, Long> getUserPropertyRemainingMap(Collection<Long> userIds, String commodityCode) {
        return userPropertyTypeService.getUserPropertyRemainingMap(userIds, commodityCode);
    }

    /**
     * 分组收集，并收集资产类型parentId=0 的结果，查询parentId！=0的资产类型
     * @param zeros
     * @param list
     */
    private void toGroup(List<UserPropertyTypeEntity> zeros,List<UserPropertyTypeEntity> list) {
        if (list.isEmpty()){
            return ;
        }
        Map<Boolean, List<UserPropertyTypeEntity>> partitionedMap = list.stream()
                .collect(Collectors.partitioningBy(entity -> entity.getParentId() == 0));
        //收集资产类型parentId=0 的结果
        zeros.addAll(partitionedMap.getOrDefault(true,new ArrayList<>()));
        //接着查parentId！=0的数据
        toSelectNoZero(zeros,partitionedMap.getOrDefault(false,new ArrayList<>()));
    }

    /**
     * 查询parentId！=0的资产类型
     * 进行分组收集
     * @param zeros
     * @param list
     */
    private void toSelectNoZero(List<UserPropertyTypeEntity> zeros,List<UserPropertyTypeEntity> list){
        if (ObjectUtil.isEmpty(list)){
            return;
        }
        //过滤重复的parentId
        List<Long> ids = list.stream().map(UserPropertyTypeEntity::getParentId).distinct().toList();
        if (ids.isEmpty()){
            return;
        }
        //根据id查询资产类型
        List<UserPropertyTypeEntity> userPropertyTypeEntities = userPropertyTypeService.listByIds(ids);
        //进行分组收集
        toGroup(zeros,userPropertyTypeEntities);
    }


}

