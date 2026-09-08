package com.jiuyu.replay.order.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.order.entity.UserPowerRollupEntity;
import com.jiuyu.replay.order.repository.dao.UserPowerRollupDao;
import com.jiuyu.replay.order.repository.service.UserPowerRollupService;
import com.jiuyu.replay.order.vo.PowerAggregateVo;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 用户算力消耗汇总表
 *
 * @author lead-engineer
 * @date 2026-07-24
 */
@Service("userPowerRollupService")
public class UserPowerRollupServiceImpl extends ServiceImpl<UserPowerRollupDao, UserPowerRollupEntity>
        implements UserPowerRollupService {

    @Override
    public List<PowerAggregateVo> aggregateIncrementalPowerConsume(String commodityTypeCode, Long lastDetailId) {
        if (commodityTypeCode == null || lastDetailId == null) {
            return Collections.emptyList();
        }
        return baseMapper.aggregateIncrementalPowerConsume(commodityTypeCode, lastDetailId);
    }

    @Override
    public int accumulateUserPowerConsume(List<PowerAggregateVo> deltaList, Date updateDate) {
        if (deltaList == null || deltaList.isEmpty()) {
            return 0;
        }
        return baseMapper.accumulateUserPowerConsume(deltaList, updateDate);
    }
}
