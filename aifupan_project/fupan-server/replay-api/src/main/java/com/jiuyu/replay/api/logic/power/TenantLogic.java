package com.jiuyu.replay.api.logic.power;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.power.bo.TenantNormalUserListBo;
import com.jiuyu.replay.power.bo.TenantSearchBO;
import com.jiuyu.replay.power.vo.TenantListVo;
import com.jiuyu.replay.power.vo.TenantInfoVo;
import com.jiuyu.replay.power.bo.TenantBo;
import com.jiuyu.replay.power.bo.TenantListBo;
import com.jiuyu.replay.power.vo.TenantOptionVo;

import java.util.List;


/**
 * 租户
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-31 11:52:17
 */
public interface TenantLogic {


    /**
     * 租户列表
     * @param tenantListBo 租户列表查询参数
     * @return
     */
    R<PageUtils<TenantListVo>> queryPage(TenantListBo tenantListBo);

    /**
    * 租户信息
    * @param id 租户id
    * @return
    */
    R<TenantInfoVo> info(Long id);

    /**
     * 新增租户
     * @param tenantBo 租户对象
     * @return
     */
    R<String> save(TenantBo tenantBo);

    /**
     * 修改租户
     * @param tenantBo 租户对象
     * @return
     */
    R<String> update(TenantBo tenantBo);

    /**
     * 删除租户
     * @param id 租户id
     * @return
     */
    R<String> delete(Long id);


    /**
     * 租户下拉选项
     *
     * @param tenantSearchBO 租户搜索
     *
     * @return {@link List }<{@link TenantOptionVo }>
     */
    List<TenantOptionVo> selectOptions(TenantSearchBO tenantSearchBO);

    /**
     * 分页查询普通用户的租户列表
     *
     * @param bo 查询参数
     * @return 租户列表
     */
    R<PageUtils<TenantListVo>> pageNormalUserTenantList(TenantNormalUserListBo bo);
}

