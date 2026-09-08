package com.jiuyu.governance.business.performance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.governance.business.performance.pojo.bo.DailyPerformanceBO;
import com.jiuyu.governance.business.performance.pojo.bo.PerformanceAggregationBO;
import com.jiuyu.governance.business.performance.pojo.bo.PeriodPerformanceBO;
import com.jiuyu.governance.business.performance.pojo.entity.SessionPerformance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 场次业绩表Mapper
 *
 * @author lj
 * @date 2026-03-24
 */
@Mapper
public interface SessionPerformanceMapper extends BaseMapper<SessionPerformance> {

    /**
     * 按组织维度聚合查询业绩数据
     *
     * @param tenantId   租户ID
     * @param ids        组织ID列表
     * @param idField    ID字段名（company_id/dept_id/team_id/live_room_id）
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 聚合结果列表
     */
    List<PerformanceAggregationBO> queryPerformanceByIds(
            @Param("tenantId") Long tenantId,
            @Param("ids") List<Long> ids,
            @Param("idField") String idField,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 按时段聚合查询业绩统计（场次数、时长、场观、销售额等一次性返回）
     *
     * @param tenantId   租户ID
     * @param idField    过滤字段名（company_id/dept_id/team_id/live_room_id），为null表示不按组织过滤
     * @param sourceId   过滤字段值
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 聚合统计结果
     */
    PeriodPerformanceBO queryPeriodStats(
            @Param("tenantId") Long tenantId,
            @Param("idField") String idField,
            @Param("sourceId") Long sourceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 各个直播间的时段聚合查询业绩统计（场次数、时长、场观、销售额等一次性返回）
     *
     * @param tenantId  户ID
     * @param liveRoomIds   滤字段值
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 合统计结果
     */
    List<PeriodPerformanceBO> queryPeriodStatsByLiveRoomIds(
            @Param("tenantId") Long tenantId,
            @Param("liveRoomIds") List<Long> liveRoomIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 按天聚合查询业绩数据（用于趋势图和每日分页）
     *
     * @param tenantId   租户ID
     * @param idField    过滤字段名，为null表示不按组织过滤
     * @param sourceId   过滤字段值
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 按天聚合结果列表
     */
    List<DailyPerformanceBO> queryDailyPerformance(
            @Param("tenantId") Long tenantId,
            @Param("idField") String idField,
            @Param("sourceId") Long sourceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
