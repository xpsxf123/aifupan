package com.jiuyu.replay.power.repository.service.impl;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.PhoneUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.power.bo.TenantNormalUserListBo;
import com.jiuyu.replay.power.bo.TenantSearchBO;
import com.jiuyu.replay.power.entity.TenantEntity;
import com.jiuyu.replay.power.repository.dao.TenantDao;
import com.jiuyu.replay.power.repository.service.TenantService;
import com.jiuyu.replay.power.vo.TenantListVo;
import com.jiuyu.replay.power.vo.TenantOptionVo;
import org.springframework.stereotype.Service;

import java.util.List;


@Service("tenantService")
public class TenantServiceImpl extends ServiceImpl<TenantDao, TenantEntity> implements TenantService {


    /**
     * 租户下拉选项
     *
     * @param tenantSearchBO 租户搜索
     *
     * @return {@link List }<{@link TenantOptionVo }>
     */
    @Override
    public List<TenantOptionVo> selectOptions(TenantSearchBO tenantSearchBO) {
        if (EmptyUtil.isEmpty(tenantSearchBO.getKeyword())) {
            return List.of();
        }
        String name = null;
        String mobile = null;
        if (PhoneUtil.isMobile(tenantSearchBO.getKeyword())) {
            mobile = tenantSearchBO.getKeyword();
        } else if (NumberUtil.isLong(tenantSearchBO.getKeyword())) {
            if (tenantSearchBO.getKeyword().length() < 3) {
                return List.of();
            }
            mobile = tenantSearchBO.getKeyword();
        } else {
            name = tenantSearchBO.getKeyword();
        }
        List<TenantOptionVo> tenantOptionList = super.getBaseMapper().selectOptions(tenantSearchBO.getUserType(), name, mobile, tenantSearchBO.getLimit());
        if (EmptyUtil.isNotEmpty(tenantOptionList)) {
            tenantOptionList.forEach(tenant -> {
                tenant.setAccountMobile(DesensitizedUtil.mobilePhone(tenant.getAccountMobile()));
            });
        }
        return tenantOptionList;
    }

    @Override
    public PageUtils<TenantListVo> pageNormalUserTenants(TenantNormalUserListBo bo) {
        Page<TenantListVo> page = new Page<>(bo.getPage(), bo.getLimit());
        IPage<TenantListVo> iPage = super.getBaseMapper().pageNormalUserTenants(page, 0, bo.getPhone());
        return new PageUtils<>(iPage);
    }
}
