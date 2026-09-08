package com.jiuyu.governance.business.performance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.governance.business.performance.pojo.entity.SessionProduct;

import java.util.Collection;
import java.util.List;

/**
 * 场次商品关联服务接口
 *
 * @author lj
 * @date 2026-03-24
 */
public interface SessionProductService {

    /**
     * 按租户 + 场次ID集合查询场次商品明细
     *
     * @param tenantId   租户ID
     * @param sessionIds 场次ID集合
     *
     * @return 场次商品明细列表（含各项指标）
     */
    List<SessionProduct> listByTenantAndSessionIds(Long tenantId, Collection<Long> sessionIds);
}
