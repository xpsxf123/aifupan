package com.jiuyu.replay.power.producer.impl;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;

import com.jiuyu.replay.power.vo.TenantUserListVo;
import com.jiuyu.replay.power.vo.TenantUserInfoVo;
import com.jiuyu.replay.power.bo.TenantUserBo;
import com.jiuyu.replay.power.bo.TenantUserListBo;
import com.jiuyu.replay.power.repository.service.TenantUserService;
import com.jiuyu.replay.power.entity.TenantUserEntity;
import com.jiuyu.replay.power.producer.TenantUserProducer;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 租户-用户-关联表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-31 11:52:17
 */
@Service
public class TenantUserProducerImpl implements TenantUserProducer {

    @Resource
    private TenantUserService tenantUserService;


    @Override
    public PageUtils<TenantUserListVo> queryPage(TenantUserListBo tenantUserListBo) {
        QueryWrapper<TenantUserEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(tenantUserListBo.getKeyword())){
            wrapper.like("name", tenantUserListBo.getKeyword());
        }

        IPage<TenantUserEntity> iPage = tenantUserService.page(new Query<TenantUserEntity>().getPage(tenantUserListBo.getPage(), tenantUserListBo.getLimit()), wrapper);

        PageUtils<TenantUserListVo> pageUtils = new PageUtils<>(tenantUserListBo.getPage(), tenantUserListBo.getLimit(), iPage);

        List<TenantUserEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<TenantUserListVo> vos = records.stream().map(item -> {
                TenantUserListVo tenantUserVo = new TenantUserListVo();
                BeanUtils.copyProperties(item, tenantUserVo);
                return tenantUserVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public TenantUserInfoVo info(Long id) {

        TenantUserEntity tenantUserEntity = tenantUserService.getById(id);
        if(tenantUserEntity != null) {
            TenantUserInfoVo tenantUserInfoVo = new TenantUserInfoVo();
            BeanUtils.copyProperties(tenantUserEntity, tenantUserInfoVo);
            return tenantUserInfoVo;
        }

        return null;
    }

    /**
     * 新增租户-用户-关联表
     * @param tenantUserBo 租户-用户-关联表对象
     * @return
     */
     public TenantUserInfoVo save(TenantUserBo tenantUserBo) {

         TenantUserEntity tenantUserEntity = new TenantUserEntity();
         BeanUtils.copyProperties(tenantUserBo, tenantUserEntity);
         tenantUserEntity.setId(SnowflakeManager.nextValue());
         tenantUserEntity.setCreateDate(new Date());
         tenantUserEntity.setUpdateDate(new Date());

         tenantUserService.save(tenantUserEntity);

         TenantUserInfoVo tenantUserInfoVo = new TenantUserInfoVo();
         BeanUtils.copyProperties(tenantUserEntity, tenantUserInfoVo);

         return tenantUserInfoVo;
     }

    /**
     * 修改租户-用户-关联表
     * @param tenantUserBo 租户-用户-关联表对象
     * @return
     */
    public void update(TenantUserBo tenantUserBo) {

        TenantUserEntity tenantUserEntity = new TenantUserEntity();
        BeanUtils.copyProperties(tenantUserBo, tenantUserEntity);
        tenantUserEntity.setUpdateDate(new Date());

        tenantUserService.updateById(tenantUserEntity);
    }

    /**
     * 删除租户-用户-关联表
     * @param id 租户-用户-关联表id
     * @return
     */
    public void deleteById(Long id) {

        tenantUserService.removeById(id);
    }

    @Override
    public TenantUserInfoVo infoByTenantIdAndUserId(Long tenantId, Long userId) {

        TenantUserEntity tenantUserEntity = tenantUserService.getOne(new QueryWrapper<TenantUserEntity>().eq("user_id", userId).eq("tenant_id", tenantId));
        if(tenantUserEntity != null) {
            TenantUserInfoVo tenantUserInfoVo = new TenantUserInfoVo();
            BeanUtils.copyProperties(tenantUserEntity, tenantUserInfoVo);
            return tenantUserInfoVo;
        }

        return null;
    }


}

