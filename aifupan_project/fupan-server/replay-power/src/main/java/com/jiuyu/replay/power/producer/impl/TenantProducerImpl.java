package com.jiuyu.replay.power.producer.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.power.bo.TenantBo;
import com.jiuyu.replay.power.bo.TenantListBo;
import com.jiuyu.replay.power.bo.TenantNormalUserListBo;
import com.jiuyu.replay.power.bo.TenantSearchBO;
import com.jiuyu.replay.power.entity.TenantEntity;
import com.jiuyu.replay.power.entity.TenantUserEntity;
import com.jiuyu.replay.power.producer.TenantProducer;
import com.jiuyu.replay.power.repository.service.TenantService;
import com.jiuyu.replay.power.repository.service.TenantUserService;
import com.jiuyu.replay.power.vo.TenantInfoVo;
import com.jiuyu.replay.power.vo.TenantListVo;
import com.jiuyu.replay.power.vo.TenantOptionVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 租户
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-31 11:52:17
 */
@Service
public class TenantProducerImpl implements TenantProducer {

    @Resource
    private TenantService tenantService;
    @Resource
    private TenantUserService tenantUserService;


    @Override
    public PageUtils<TenantListVo> queryPage(TenantListBo tenantListBo) {
        QueryWrapper<TenantEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(tenantListBo.getKeyword())){
            wrapper.like("name", tenantListBo.getKeyword());
        }

        IPage<TenantEntity> iPage = tenantService.page(new Query<TenantEntity>().getPage(tenantListBo.getPage(), tenantListBo.getLimit()), wrapper);

        PageUtils<TenantListVo> pageUtils = new PageUtils<>(tenantListBo.getPage(), tenantListBo.getLimit(), iPage);

        List<TenantEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<TenantListVo> vos = records.stream().map(item -> {
                TenantListVo tenantVo = new TenantListVo();
                BeanUtils.copyProperties(item, tenantVo);
                return tenantVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public TenantInfoVo info(Long id) {

        TenantEntity tenantEntity = tenantService.getById(id);
        if(tenantEntity != null) {
            TenantInfoVo tenantInfoVo = new TenantInfoVo();
            BeanUtils.copyProperties(tenantEntity, tenantInfoVo);
            return tenantInfoVo;
        }

        return null;
    }

    /**
     * 新增租户
     * @param tenantBo 租户对象
     * @return
     */
     public TenantInfoVo save(TenantBo tenantBo) {

         TenantEntity tenantEntity = new TenantEntity();
         BeanUtils.copyProperties(tenantBo, tenantEntity);
         tenantEntity.setId(SnowflakeManager.nextValue());
         tenantEntity.setCreateDate(new Date());
         tenantEntity.setUpdateDate(new Date());

         tenantService.save(tenantEntity);

         TenantInfoVo tenantInfoVo = new TenantInfoVo();
         BeanUtils.copyProperties(tenantEntity, tenantInfoVo);

         return tenantInfoVo;
     }

    /**
     * 修改租户
     * @param tenantBo 租户对象
     * @return
     */
    public void update(TenantBo tenantBo) {

        TenantEntity tenantEntity = new TenantEntity();
        BeanUtils.copyProperties(tenantBo, tenantEntity);
        tenantEntity.setUpdateDate(new Date());

        tenantService.updateById(tenantEntity);
    }

    /**
     * 删除租户
     * @param id 租户id
     * @return
     */
    public void deleteById(Long id) {

        tenantService.removeById(id);
    }

    @Override
    public TenantInfoVo infoByUserId(Long userId) {

        TenantEntity tenantEntity = tenantService.getOne(new QueryWrapper<TenantEntity>().eq("user_id", userId));
        if(tenantEntity != null) {
            TenantInfoVo tenantInfoVo = new TenantInfoVo();
            BeanUtils.copyProperties(tenantEntity, tenantInfoVo);
            return tenantInfoVo;
        }

        return null;
    }


    /**
     * 获取租户信息
     *
     * @param tenantIds 租户id
     *
     * @return {@link List }<{@link TenantInfoVo }>
     */
    @Override
    public List<TenantInfoVo> listTenantInfo(List<Long> tenantIds) {
        if (EmptyUtil.isEmpty(tenantIds)) {
            return List.of();
        }
        List<TenantEntity> list = tenantService.lambdaQuery()
            .in(TenantEntity::getId, tenantIds)
            .eq(TenantEntity::getIsDeleted, 0)
            .select(TenantEntity::getId, TenantEntity::getTenantName, TenantEntity::getUserId)
            .list();
        if (EmptyUtil.isEmpty(list)) {
            return List.of();
        }
        return list.stream().map(item -> {
            TenantInfoVo tenantInfoVo = new TenantInfoVo();
            BeanUtils.copyProperties(item, tenantInfoVo);
            return tenantInfoVo;
        }).toList();
    }

    @Override
    public Long unbind(Long parentUserId, Long subUserId) {

        // 查出解除人的租户
        TenantInfoVo tenantInfoVo = this.infoByUserId(parentUserId);
        if(tenantInfoVo != null) {
            // 删除租户和被解除者的关联关系
            this.tenantUserService.remove(new QueryWrapper<TenantUserEntity>().eq("user_id", subUserId).eq("tenant_id", tenantInfoVo.getId()));
            return tenantInfoVo.getId();
        }

        return null;
    }

    @Override
    public void createMyTenant(Long userId, String phone) {

        // 查询是否存在自己的租户
        TenantEntity tenant = tenantService.getOne(new LambdaQueryWrapper<TenantEntity>()
                .eq(TenantEntity::getUserId, userId)
                .last("limit 1")
        );
        Date now = new Date();
        if (ObjectUtil.isEmpty(tenant)){
            tenant = new TenantEntity();
            tenant.setId(SnowflakeManager.nextValue());
            tenant.setUserId(userId);
            tenant.setTenantName(phone + "的租户");
            tenant.setUpdateDate(now);
            tenant.setCreateDate(now);
            tenantService.save(tenant);
        }

        // 创建租户和用户的关联关系
        TenantUserEntity tenantUser = tenantUserService.getOne(new LambdaQueryWrapper<TenantUserEntity>()
                .eq(TenantUserEntity::getUserId, userId)
                .eq(TenantUserEntity::getTenantId, tenant.getId())
        );
        if (ObjectUtil.isEmpty(tenantUser)){
            tenantUser = new TenantUserEntity();
            tenantUser.setId(SnowflakeManager.nextValue());
            tenantUser.setUserId(userId);
            tenantUser.setTenantId(tenant.getId());
            tenantUser.setUpdateDate(now);
            tenantUser.setCreateDate(now);
            tenantUserService.save(tenantUser);
        }
    }

    @Override
    public List<Long> listUserByTenantId(Long tenantId) {
        List<TenantUserEntity> tenantUserEntityList = tenantUserService.list(new LambdaQueryWrapper<>(TenantUserEntity.class)
                .eq(TenantUserEntity::getTenantId, tenantId).orderByAsc(TenantUserEntity::getCreateDate));
        if (CollectionUtil.isEmpty(tenantUserEntityList)) {
            return null;
        }
        return tenantUserEntityList.stream().map(TenantUserEntity::getUserId).collect(Collectors.toList());
    }

    /**
     * 租户下拉选项
     *
     * @param tenantSearchBO 租户搜索
     *
     * @return {@link List }<{@link TenantOptionVo }>
     */
    @Override
    public List<TenantOptionVo> selectOptions(TenantSearchBO tenantSearchBO) {
        return tenantService.selectOptions(tenantSearchBO);
    }

    @Override
    public PageUtils<TenantListVo> pageNormalUserTenants(TenantNormalUserListBo bo) {
        return tenantService.pageNormalUserTenants(bo);
    }
}

