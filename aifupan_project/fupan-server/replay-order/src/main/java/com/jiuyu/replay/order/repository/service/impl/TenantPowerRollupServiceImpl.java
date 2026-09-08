package com.jiuyu.replay.order.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.order.entity.TenantPowerRollupEntity;
import com.jiuyu.replay.order.repository.dao.TenantPowerRollupDao;
import com.jiuyu.replay.order.repository.service.TenantPowerRollupService;
import com.jiuyu.replay.order.vo.PowerAggregateVo;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 租户算力消耗汇总表
 *
 * @author lead-engineer
 * @date 2026-07-24
 */
@Service("tenantPowerRollupService")
public class TenantPowerRollupServiceImpl extends ServiceImpl<TenantPowerRollupDao, TenantPowerRollupEntity>
        implements TenantPowerRollupService {

    @Override
    public int accumulateTenantPowerConsume(List<PowerAggregateVo> deltaList, Date updateDate) {
        if (deltaList == null || deltaList.isEmpty()) {
            return 0;
        }
        return baseMapper.accumulateTenantPowerConsume(deltaList, updateDate);
    }
}
