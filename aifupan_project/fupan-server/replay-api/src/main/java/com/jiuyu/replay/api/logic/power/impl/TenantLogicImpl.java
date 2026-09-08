package com.jiuyu.replay.api.logic.power.impl;

import com.jiuyu.replay.api.logic.power.TenantLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.bll.TenantBll;
import com.jiuyu.replay.power.bo.TenantBo;
import com.jiuyu.replay.power.bo.TenantListBo;
import com.jiuyu.replay.power.bo.TenantNormalUserListBo;
import com.jiuyu.replay.power.bo.TenantSearchBO;
import com.jiuyu.replay.power.vo.TenantInfoVo;
import com.jiuyu.replay.power.vo.TenantListVo;
import com.jiuyu.replay.power.vo.TenantOptionVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 租户
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-31 11:52:17
 */
@Service
public class TenantLogicImpl implements TenantLogic {

    @Resource
    private TenantBll tenantBll;


    @Override
    public R<PageUtils<TenantListVo>> queryPage(TenantListBo tenantListBo) {

        return tenantBll.queryPage(tenantListBo);
    }

    @Override
    public R<TenantInfoVo> info(Long id) {

        return tenantBll.info(id);
    }

    @Override
    public R<String> save(TenantBo tenantBo) {

        return tenantBll.save(tenantBo);
    }

    @Override
    public R<String> update(TenantBo tenantBo) {

        return tenantBll.update(tenantBo);
    }

    @Override
    public R<String> delete(Long id) {

        return tenantBll.delete(id);
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
        return tenantBll.selectOptions(tenantSearchBO);
    }

    @Override
    public R<PageUtils<TenantListVo>> pageNormalUserTenantList(TenantNormalUserListBo bo) {
        return tenantBll.pageNormalUserTenants(bo);
    }
}

