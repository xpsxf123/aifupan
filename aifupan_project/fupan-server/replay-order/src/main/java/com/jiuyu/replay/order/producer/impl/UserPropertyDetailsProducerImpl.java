package com.jiuyu.replay.order.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.constant.OrderEnums;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.order.bo.UserPropertyDetailsBo;
import com.jiuyu.replay.order.bo.UserPropertyDetailsList;
import com.jiuyu.replay.order.bo.UserPropertyDetailsListBo;
import com.jiuyu.replay.order.entity.UserPropertyDetailsEntity;
import com.jiuyu.replay.order.producer.UserPropertyDetailsProducer;
import com.jiuyu.replay.order.repository.service.UserPropertyDetailsService;
import com.jiuyu.replay.order.vo.UserPropertyDetailsInfoVo;
import com.jiuyu.replay.order.vo.UserPropertyDetailsListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 用户资产消费记录表
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
@Service
public class UserPropertyDetailsProducerImpl implements UserPropertyDetailsProducer {

    @Resource
    private UserPropertyDetailsService userPropertyDetailsService;



    @Override
    public PageUtils<UserPropertyDetailsListVo> queryPage(UserPropertyDetailsListBo userPropertyDetailsListBo) {
        LambdaQueryWrapper<UserPropertyDetailsEntity> wrapper = new LambdaQueryWrapper<UserPropertyDetailsEntity>()
                .eq(ObjectUtil.isNotEmpty(userPropertyDetailsListBo.getUserId()), UserPropertyDetailsEntity::getUserId, userPropertyDetailsListBo.getUserId())
                .like(ObjectUtil.isNotEmpty(userPropertyDetailsListBo.getUserName()), UserPropertyDetailsEntity::getUserName, userPropertyDetailsListBo.getUserName())
                .eq(ObjectUtil.isNotEmpty(userPropertyDetailsListBo.getParentUserId()), UserPropertyDetailsEntity::getParentUserId, userPropertyDetailsListBo.getParentUserId())
                .eq(ObjectUtil.isNotEmpty(userPropertyDetailsListBo.getPropertyId()), UserPropertyDetailsEntity::getPropertyId, userPropertyDetailsListBo.getPropertyId())
                .eq(ObjectUtil.isNotEmpty(userPropertyDetailsListBo.getCommodityTypeId()), UserPropertyDetailsEntity::getCommodityTypeId, userPropertyDetailsListBo.getCommodityTypeId())
                .eq(ObjectUtil.isNotEmpty(userPropertyDetailsListBo.getCommodityTypeCode()), UserPropertyDetailsEntity::getCommodityTypeCode, userPropertyDetailsListBo.getCommodityTypeCode())
                .gt(ObjectUtil.isNotEmpty(userPropertyDetailsListBo.getStartCreateDate()), UserPropertyDetailsEntity::getCreateDate, userPropertyDetailsListBo.getStartCreateDate())
                .lt(ObjectUtil.isNotEmpty(userPropertyDetailsListBo.getEndCreateDate()), UserPropertyDetailsEntity::getCreateDate, userPropertyDetailsListBo.getEndCreateDate())
                ;

        IPage<UserPropertyDetailsEntity> iPage = userPropertyDetailsService.page(
                new Query<UserPropertyDetailsEntity>().getPage(userPropertyDetailsListBo.getPage(), userPropertyDetailsListBo.getLimit()),
                wrapper);

        PageUtils<UserPropertyDetailsListVo> pageUtils = new PageUtils<>(userPropertyDetailsListBo.getPage(), userPropertyDetailsListBo.getLimit(), iPage);

        List<UserPropertyDetailsEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<UserPropertyDetailsListVo> vos = records.stream().map(item -> {
                UserPropertyDetailsListVo userPropertyDetailsVo = new UserPropertyDetailsListVo();
                BeanUtils.copyProperties(item, userPropertyDetailsVo);
                return userPropertyDetailsVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public UserPropertyDetailsInfoVo info(Long id) {

        UserPropertyDetailsEntity userPropertyDetailsEntity = userPropertyDetailsService.getById(id);
        if(userPropertyDetailsEntity != null) {
            UserPropertyDetailsInfoVo userPropertyDetailsInfoVo = new UserPropertyDetailsInfoVo();
            BeanUtils.copyProperties(userPropertyDetailsEntity, userPropertyDetailsInfoVo);
            return userPropertyDetailsInfoVo;
        }

        return null;
    }

    /**
     * 新增用户资产消费记录表
     * @param userPropertyDetailsBo 用户资产消费记录表对象
     * @return
     */
     public UserPropertyDetailsInfoVo save(UserPropertyDetailsBo userPropertyDetailsBo) {

         UserPropertyDetailsEntity userPropertyDetailsEntity = new UserPropertyDetailsEntity();
         BeanUtils.copyProperties(userPropertyDetailsBo, userPropertyDetailsEntity);
         userPropertyDetailsEntity.setId(SnowflakeManager.nextValue());
         userPropertyDetailsEntity.setCreateDate(new Date());

         userPropertyDetailsService.save(userPropertyDetailsEntity);

         UserPropertyDetailsInfoVo userPropertyDetailsInfoVo = new UserPropertyDetailsInfoVo();
         BeanUtils.copyProperties(userPropertyDetailsEntity, userPropertyDetailsInfoVo);

         return userPropertyDetailsInfoVo;
     }

    /**
     * 修改用户资产消费记录表
     * @param userPropertyDetailsBo 用户资产消费记录表对象
     * @return
     */
    public void update(UserPropertyDetailsBo userPropertyDetailsBo) {

        UserPropertyDetailsEntity userPropertyDetailsEntity = new UserPropertyDetailsEntity();
        BeanUtils.copyProperties(userPropertyDetailsBo, userPropertyDetailsEntity);

        userPropertyDetailsService.updateById(userPropertyDetailsEntity);
    }

    /**
     * 删除用户资产消费记录表
     * @param id 用户资产消费记录表id
     * @return
     */
    public void deleteById(Long id) {

        userPropertyDetailsService.removeById(id);
    }

    @Override
    public void saveBatch(List<UserPropertyDetailsBo> updateList) {
        userPropertyDetailsService.saveBatch(BeanUtil.copyToList(updateList, UserPropertyDetailsEntity.class));
    }

    @Override
    public PageUtils<UserPropertyDetailsInfoVo> userPropertyDetails(UserPropertyDetailsList bo) {
        LambdaQueryWrapper<UserPropertyDetailsEntity> wrapper = new QueryWrapper<UserPropertyDetailsEntity>()
                .select("DATE_FORMAT(create_date,'%Y-%m-%d') as create_date", "sum(quantity) as quantity")
                .groupBy("DATE_FORMAT(create_date,'%Y-%m-%d')")
                .lambda()
                .eq(UserPropertyDetailsEntity::getPropertyId, bo.getPropertyId())
                .eq(ObjectUtil.isNotEmpty(bo.getUserId()), UserPropertyDetailsEntity::getUserId, bo.getUserId())
                .eq(UserPropertyDetailsEntity::getAssetCreationType, 0)
                .eq(UserPropertyDetailsEntity::getCommodityTypeCode, bo.getCommodityTypeCode())
                .eq(UserPropertyDetailsEntity::getSigns, OrderEnums.signs.SUBTRACT.getCode())
                .ge(UserPropertyDetailsEntity::getCreateDate, bo.getStartDate())
                .le(UserPropertyDetailsEntity::getCreateDate, bo.getEndDate())
                .orderByDesc(UserPropertyDetailsEntity::getCreateDate);

        IPage<UserPropertyDetailsEntity> iPage = userPropertyDetailsService.page(
                new Query<UserPropertyDetailsEntity>().getPageNoSort(bo.getPage(), bo.getLimit()),
                wrapper);

        PageUtils<UserPropertyDetailsInfoVo> pageUtils = new PageUtils<>(bo.getPage(), bo.getLimit(), iPage);

        List<UserPropertyDetailsEntity> records = iPage.getRecords();
        if (records != null && !records.isEmpty()) {
            pageUtils.setList(BeanUtil.copyToList(records, UserPropertyDetailsInfoVo.class));
        }

        return pageUtils;
    }


    /**
     * 统计租户的消耗总token数
     *
     * @param mainUserId       主用户ID
     * @param sinceCreateDate 创建时间
     */
    @Override
    public Long sumTotalTokensByTenant(long mainUserId, Date sinceCreateDate) {
        return userPropertyDetailsService.sumTotalTokensByTenant(mainUserId, sinceCreateDate);
    }
}

