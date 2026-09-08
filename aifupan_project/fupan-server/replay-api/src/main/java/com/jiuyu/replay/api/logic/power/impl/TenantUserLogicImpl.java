package com.jiuyu.replay.api.logic.power.impl;

import com.jiuyu.replay.api.logic.power.TenantUserLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.bll.TenantUserBll;
import com.jiuyu.replay.power.bo.TenantUserBo;
import com.jiuyu.replay.power.bo.TenantUserListBo;
import com.jiuyu.replay.power.vo.TenantUserInfoVo;
import com.jiuyu.replay.power.vo.TenantUserListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


/**
 * 租户-用户-关联表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-31 11:52:17
 */
@Service
public class TenantUserLogicImpl implements TenantUserLogic {

    @Resource
    private TenantUserBll tenantUserBll;


    @Override
    public R<PageUtils<TenantUserListVo>> queryPage(TenantUserListBo tenantUserListBo) {

        return tenantUserBll.queryPage(tenantUserListBo);
    }

    @Override
    public R<TenantUserInfoVo> info(Long id) {

        return tenantUserBll.info(id);
    }

    @Override
    public R<String> save(TenantUserBo tenantUserBo) {

        return tenantUserBll.save(tenantUserBo);
    }

    @Override
    public R<String> update(TenantUserBo tenantUserBo) {

        return tenantUserBll.update(tenantUserBo);
    }

    @Override
    public R<String> delete(Long id) {

        return tenantUserBll.delete(id);
    }


}

