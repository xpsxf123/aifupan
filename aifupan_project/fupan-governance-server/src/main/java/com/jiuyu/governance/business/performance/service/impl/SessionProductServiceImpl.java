package com.jiuyu.governance.business.performance.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.performance.mapper.SessionProductMapper;
import com.jiuyu.governance.business.performance.pojo.entity.SessionProduct;
import com.jiuyu.governance.business.performance.service.SessionProductService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 场次商品关联服务实现类
 *
 * @author lj
 * @date 2026-03-24
 */
@Service
public class SessionProductServiceImpl extends ServiceImpl<SessionProductMapper, SessionProduct> implements SessionProductService {

    @Override
    public List<SessionProduct> listByTenantAndSessionIds(Long tenantId, Collection<Long> sessionIds) {
        if (tenantId == null || EmptyUtil.isEmpty(sessionIds)) {
            return List.of();
        }
        // @TableLogic 自动追加 is_deleted 过滤
        return lambdaQuery()
            .eq(SessionProduct::getTenantId, tenantId)
            .in(SessionProduct::getSessionId, sessionIds)
            .list();
    }
}
