package com.jiuyu.governance.business.room.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.governance.business.org.pojo.constants.ManagerType;
import com.jiuyu.governance.business.room.pojo.entity.LiveRoom;
import com.jiuyu.governance.business.room.pojo.entity.ScheduleEmployee;
import com.jiuyu.governance.business.room.pojo.request.LiveRoomQueryRequest;
import com.jiuyu.governance.business.room.pojo.request.LiveRoomSearchQueryRequest;
import com.jiuyu.governance.business.room.pojo.response.LiveRoomResponse;
import com.jiuyu.governance.common.pojo.bo.CountData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;


/**
 * 直播间
 *
 * @author HeHui
 * @date 2026/03/19
 */
@Mapper
public interface LiveRoomMapper extends BaseMapper<LiveRoom> {


    /**
     * 页面查询直播室
     *
     * @param page     页
     * @param req      请求
     * @param tenantId 租户ID
     *
     * @return {@link IPage }<{@link LiveRoomResponse }>
     */
    IPage<LiveRoomResponse> pageQueryLiveRoom(IPage<LiveRoomResponse> page, @Param("req") LiveRoomQueryRequest req, @Param("tenantId") Long tenantId);


    /**
     * 列表查询直播室
     *
     * @param req      请求
     *
     * @return {@link List }<{@link LiveRoom }>
     */
    List<LiveRoom> listLiveRooms(@Param("req") LiveRoomSearchQueryRequest req);

    /**
     * 获取员工加入的直播间
     *
     * @param employeeId  员工id
     * @param positionId  职位id
     * @param deadlineDay 截止日期
     *
     * @return {@link List }<{@link Long }>
     */
    List<Long> getEmployeeJoinRoomIds(@Param("employeeId") long employeeId, @Param("positionId") Long positionId, @Param("deadlineDay") LocalDate deadlineDay);


    /**
     * 获取员工加入的直播间信息
     * 只有员工ID和直播间ID
     * @param employeeIds 员工id
     * @param positionId  职位id
     * @param deadlineDay 截止日期
     *
     * @return {@link List }<{@link ScheduleEmployee }>
     */
    List<ScheduleEmployee> getEmployeeJoinRoomInfos(@Param("employeeIds") Collection<Long> employeeIds, @Param("positionId") Long positionId, @Param("deadlineDay") LocalDate deadlineDay);

    /**
     * 统计组织直播间数量
     *
     * @param orgIds   组织id
     * @param type     组织类型 {@link ManagerType}
     * @param tenantId 租户ID
     *
     * @return {@link List }<{@link CountData }>
     */
    List<CountData> countOrgRoomMap(@Param("orgIds") List<Long> orgIds, @Param("type") int type, @Param("tenantId") long tenantId);

    /**
     * 分页查询直播间（带今日业绩排序）
     */
    IPage<LiveRoomResponse> pageQueryLiveRoomWithTodayPerformance(
            IPage<LiveRoomResponse> page,
            @Param("req") LiveRoomQueryRequest req,
            @Param("tenantId") Long tenantId);
}
