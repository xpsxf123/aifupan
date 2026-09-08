package com.jiuyu.replay.order.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.order.entity.TenantPowerRollupEntity;
import com.jiuyu.replay.order.vo.PowerAggregateVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 租户算力消耗汇总表
 *
 * @author lead-engineer
 * @date 2026-07-24
 */
@Mapper
public interface TenantPowerRollupDao extends BaseMapper<TenantPowerRollupEntity> {

    /**
     * 按 tenant_id 批量累加租户算力消耗（+= delta，非覆盖）
     * 增量来源为用户维度聚合结果的内存归并，本 Dao 不对明细表跑第二条 GROUP BY
     *
     * @param deltaList  待累加的增量行，每行需含 tenantId 与 powerConsume
     * @param updateDate 刷新时间
     *
     * @return 受影响行数
     */
    int accumulateTenantPowerConsume(@Param("deltaList") List<PowerAggregateVo> deltaList,
                                     @Param("updateDate") Date updateDate);
}
