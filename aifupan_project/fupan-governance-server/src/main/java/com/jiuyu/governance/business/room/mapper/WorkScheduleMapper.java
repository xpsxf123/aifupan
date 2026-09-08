package com.jiuyu.governance.business.room.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.governance.business.room.pojo.bo.BatchQueryScheduleDto;
import com.jiuyu.governance.business.room.pojo.bo.EmployeeLiveRoomScheduleRawDto;
import com.jiuyu.governance.business.room.pojo.bo.LiveScheduleRawDto;
import com.jiuyu.governance.business.room.pojo.entity.WorkSchedule;
import com.jiuyu.governance.business.room.pojo.request.schedule.EmployeeScheduleQueryRequest;
import com.jiuyu.governance.business.room.pojo.response.schedule.RoomWorkScheduleDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Mapper
public interface WorkScheduleMapper extends BaseMapper<WorkSchedule> {


    int updateBatch(@Param("list") List<WorkSchedule> list);

    int updateBatchSelective(@Param("list") List<WorkSchedule> list);

    int batchInsert(@Param("list") List<WorkSchedule> list);

    /**
     * 原始关联查询：排班+直播间+人员
     */
    List<LiveScheduleRawDto> selectRawLiveSchedule(
        @Param("idx") Long idx, @Param("limit") Integer limit,
        @Param("tenantId") long tenantId,
        @Param("platformType") Integer platformType,
        @Param("secUidList") Collection<String> secUidList,
        @Param("minDate") LocalDate minDate,
        @Param("maxDate") LocalDate maxDate,
        @Param("positionIds") List<Long> positionIds
    );

    /**
     * 查询客户端展示用的排班数据（仅关联直播间，不关心人员）
     */
    List<RoomWorkScheduleDto> selectClientSchedules(
        @Param("idx") Long idx, @Param("limit") Integer limit,
        @Param("tenantId") long tenantId,
        @Param("day") LocalDate day,
        @Param("anchorPositionId") long anchorPositionId
    );


    /**
     * 获取直播间未来排班数据
     *
     * @param idx       索引
     * @param limit     限制
     * @param tenantId  租户ID
     * @param startTime 开始时间
     * @param futureDay 未来日期
     *
     * @return {@link List }<{@link RoomWorkScheduleDto }>
     */
    List<RoomWorkScheduleDto> getLiveRoomFuturePlanSchedules(
        @Param("idx") Long idx, @Param("limit") Integer limit,
        @Param("tenantId") long tenantId,
        @Param("startTime") LocalDate startTime,
        @Param("futureDay") LocalDate futureDay,
        @Param("anchorPositionId") long anchorPositionId
    );

    /**
     * 查询员工排班数据
     *
     * @param idx        索引
     * @param employeeId 员工编号
     * @param tenantId   租户ID
     * @param req        请求
     * @param limit      限制
     *
     * @return {@link List }<{@link EmployeeLiveRoomScheduleRawDto }>
     */
    List<EmployeeLiveRoomScheduleRawDto> selectEmployeeSchedule(@Param("idx") Long idx, @Param("employeeId") long employeeId, @Param("tenantId") long tenantId, @Param("req") EmployeeScheduleQueryRequest req, @Param("limit") int limit);


    /**
     * 加载员工排班数据
     *
     * @param idx         索引
     * @param employeeIds 员工ID列表
     * @param startDate   开始日期
     * @param endDate     末日
     * @param limit       限制
     *
     * @return {@link List }<{@link EmployeeLiveRoomScheduleRawDto }>
     */
    List<EmployeeLiveRoomScheduleRawDto> loadEmployeeSchedule(@Param("idx") Long idx, @Param("employeeIds") Collection<Long> employeeIds, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("limit") int limit);

    /**
     * 查询主播直播间排班数据
     *
     * @param tenantId     租户ID
     * @param roomId       直播间ID
     * @param startDay     开始日期
     * @param endDay       末日
     *
     * @return {@link List }<{@link RoomWorkScheduleDto }>
     */
    List<RoomWorkScheduleDto> selectAnchorRoomSchedule(@Param("tenantId") long tenantId, @Param("roomId") long roomId, @Param("startDay") LocalDate startDay, @Param("endDay") LocalDate endDay);

    /**
     * 批量查询主播直播间排班数据
     *
     * @param queries 批量查询条件集合
     * @return 排班数据列表
     */
    List<RoomWorkScheduleDto> selectBatchAnchorRoomSchedule(@Param("queries") Collection<BatchQueryScheduleDto> queries);


}
