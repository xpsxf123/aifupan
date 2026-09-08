package com.jiuyu.governance.business.performance.service.impl;

import com.jiuyu.governance.business.performance.mapper.SessionOriginalValueMapper;
import com.jiuyu.governance.business.performance.pojo.base.BasePerformanceEntity;
import com.jiuyu.governance.business.performance.pojo.constants.DataSource;
import com.jiuyu.governance.business.performance.pojo.entity.SessionOriginalValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理员连接处理器
 * @author ：lujie
 * @date ：2026/3/30 10:52
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ManagerPerformanceProcessor {

    private final SessionOriginalValueMapper sessionOriginalValueMapper;

    /**
     * 保存原始数据
     * @param tenantId 租户id
     * @param sourceId 来源id
     * @param sourceType 来源类型
     * @param entity 业绩数据
     */
    public <T extends BasePerformanceEntity> void saveOriginalValueForPerformance(Long tenantId, Long sourceId, int sourceType, T entity) {
        SessionOriginalValue ov = new SessionOriginalValue();
        ov.setTenantId(tenantId);
        ov.setSourceId(sourceId);
        ov.setSourceType(sourceType);
        ov.setSource(DataSource.SYSTEM.getCode());
        ov.setPerformance(entity);
        ov.setCreateDate(LocalDateTime.now());
        sessionOriginalValueMapper.insert(ov);
    }

}
