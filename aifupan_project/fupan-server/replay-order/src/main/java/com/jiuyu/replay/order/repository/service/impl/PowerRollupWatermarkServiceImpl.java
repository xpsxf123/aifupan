package com.jiuyu.replay.order.repository.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.order.entity.PowerRollupWatermarkEntity;
import com.jiuyu.replay.order.repository.dao.PowerRollupWatermarkDao;
import com.jiuyu.replay.order.repository.service.PowerRollupWatermarkService;
import org.springframework.stereotype.Service;

/**
 * 算力汇总增量水位线
 *
 * @author lead-engineer
 * @date 2026-07-24
 */
@Service("powerRollupWatermarkService")
public class PowerRollupWatermarkServiceImpl extends ServiceImpl<PowerRollupWatermarkDao, PowerRollupWatermarkEntity>
        implements PowerRollupWatermarkService {

    @Override
    public PowerRollupWatermarkEntity getByBizCode(String bizCode) {
        if (bizCode == null || bizCode.isEmpty()) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<PowerRollupWatermarkEntity>()
                .eq(PowerRollupWatermarkEntity::getBizCode, bizCode)
                .last("limit 1"));
    }
}
