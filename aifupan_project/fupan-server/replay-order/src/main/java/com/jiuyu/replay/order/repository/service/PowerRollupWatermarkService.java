package com.jiuyu.replay.order.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.order.entity.PowerRollupWatermarkEntity;

/**
 * 算力汇总增量水位线
 *
 * @author lead-engineer
 * @date 2026-07-24
 */
public interface PowerRollupWatermarkService extends IService<PowerRollupWatermarkEntity> {

    /**
     * 按业务标识获取水位线行
     *
     * @param bizCode 业务标识，本轮固定 aiTokenNum
     *
     * @return 水位线行，不存在返回 null（说明存量未初始化）
     */
    PowerRollupWatermarkEntity getByBizCode(String bizCode);
}
