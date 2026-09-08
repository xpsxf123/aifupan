package com.jiuyu.replay.order.bll;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.producer.PowerRollupProducer;
import org.springframework.stereotype.Component;

/**
 * 算力消耗汇总刷新（对外唯一入口）
 *
 * @author lead-engineer
 * @date 2026-07-24
 */
@Component
public class PowerRollupBll {

    private final PowerRollupProducer powerRollupProducer;

    /**
     * 构造器注入
     *
     * @param powerRollupProducer 算力汇总刷新 producer
     */
    public PowerRollupBll(PowerRollupProducer powerRollupProducer) {
        this.powerRollupProducer = powerRollupProducer;
    }

    /**
     * 增量刷新用户/租户算力消耗汇总表
     *
     * @return 刷新结果
     */
    public R<String> refreshPowerRollup() {
        powerRollupProducer.refreshPowerRollup();
        return R.ok("刷新成功");
    }
}
