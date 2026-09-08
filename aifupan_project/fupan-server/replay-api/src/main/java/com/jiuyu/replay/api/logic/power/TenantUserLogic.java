package com.jiuyu.replay.api.logic.power;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.power.vo.TenantUserListVo;
import com.jiuyu.replay.power.vo.TenantUserInfoVo;
import com.jiuyu.replay.power.bo.TenantUserBo;
import com.jiuyu.replay.power.bo.TenantUserListBo;


/**
 * 租户-用户-关联表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-31 11:52:17
 */
public interface TenantUserLogic {


    /**
     * 租户-用户-关联表列表
     * @param tenantUserListBo 租户-用户-关联表列表查询参数
     * @return
     */
    R<PageUtils<TenantUserListVo>> queryPage(TenantUserListBo tenantUserListBo);

    /**
    * 租户-用户-关联表信息
    * @param id 租户-用户-关联表id
    * @return
    */
    R<TenantUserInfoVo> info(Long id);

    /**
     * 新增租户-用户-关联表
     * @param tenantUserBo 租户-用户-关联表对象
     * @return
     */
    R<String> save(TenantUserBo tenantUserBo);

    /**
     * 修改租户-用户-关联表
     * @param tenantUserBo 租户-用户-关联表对象
     * @return
     */
    R<String> update(TenantUserBo tenantUserBo);

    /**
     * 删除租户-用户-关联表
     * @param id 租户-用户-关联表id
     * @return
     */
    R<String> delete(Long id);


}

