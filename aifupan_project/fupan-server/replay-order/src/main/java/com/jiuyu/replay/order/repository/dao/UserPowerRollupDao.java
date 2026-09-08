package com.jiuyu.replay.order.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.order.entity.UserPowerRollupEntity;
import com.jiuyu.replay.order.vo.PowerAggregateVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 用户算力消耗汇总表
 *
 * @author lead-engineer
 * @date 2026-07-24
 */
@Mapper
public interface UserPowerRollupDao extends BaseMapper<UserPowerRollupEntity> {

    /**
     * 唯一一条增量聚合 SQL：按 (user_id, tenant_id) 聚合明细表新增段的净消耗
     * 走主键范围扫（id > lastDetailId），不 JOIN 任何表；租户维度由调用方在内存归并
     *
     * @param commodityTypeCode 商品类型key，本轮固定 aiTokenNum
     * @param lastDetailId      水位线，已处理到的明细表最大id(含)
     *
     * @return 净增量聚合行，无新增明细时为空集合
     */
    List<PowerAggregateVo> aggregateIncrementalPowerConsume(@Param("commodityTypeCode") String commodityTypeCode,
                                                            @Param("lastDetailId") Long lastDetailId);

    /**
     * 按 user_id 批量累加用户算力消耗（+= delta，非覆盖）
     *
     * @param deltaList  待累加的增量行，每行需含 userId 与 powerConsume
     * @param updateDate 刷新时间
     *
     * @return 受影响行数
     */
    int accumulateUserPowerConsume(@Param("deltaList") List<PowerAggregateVo> deltaList,
                                   @Param("updateDate") Date updateDate);
}
