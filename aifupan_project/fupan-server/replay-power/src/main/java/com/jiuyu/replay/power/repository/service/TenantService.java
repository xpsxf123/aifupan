package com.jiuyu.replay.power.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.power.bo.TenantNormalUserListBo;
import com.jiuyu.replay.power.bo.TenantSearchBO;
import com.jiuyu.replay.power.entity.TenantEntity;
import com.jiuyu.replay.power.vo.TenantListVo;
import com.jiuyu.replay.power.vo.TenantOptionVo;

import java.util.List;

/**
 * 租户
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-31 11:52:17
 */
public interface TenantService extends IService<TenantEntity> {


    /**
     * 租户下拉选项
     *
     * @param tenantSearchBO 租户搜索
     *
     * @return {@link List }<{@link TenantOptionVo }>
     */
    List<TenantOptionVo> selectOptions(TenantSearchBO tenantSearchBO);

    PageUtils<TenantListVo> pageNormalUserTenants(TenantNormalUserListBo bo);
}

