package com.jiuyu.governance.business.room.pojo.bo;

import com.jiuyu.governance.business.room.pojo.constants.LivePlatformType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 直播间排班
 *
 * @author HeHui
 * @date 2026-03-25 16:55
 */
@Getter
@Setter
public class RoomScheduleBo {

    /**
     * 直播间ID
     */
    private Long roomId;


    /**
     * 直播间唯一ID
     */
    private String secUid;


    /**
     * 直播平台类型
     */
    private LivePlatformType platformType;


    /**
     * 排班计划ID
     */
    private Long scheduleId;

    /**
     * 班次归属天
     */
    private LocalDate workDay;

    /**
     * 开始时间
     */
    private LocalDateTime startWork;

    /**
     * 结束时间
     */
    private LocalDateTime endWork;




    /**
     * 班次时长 分钟
     */
    private Integer scheduleDuration;

    /**
     * 休息时长 分钟
     */
    private Integer restDuration;


    /**
     * 排班的人员
     */
    private List<WorkUser> workUserList;


    /**
     * 排班人员计划
     */
    @Getter
    @Setter
    public static class WorkUser {

        /**
         * 用户ID
         */
        private Long employeeId;

        /**
         * 员工名称
         */
        private String employeeName;

        /**
         * 岗位ID
         */
        private Long positionId;

        /**
         *  岗位code
         */
        private String positionCode;

        /**
         * 岗位名称
         */
        private String positionName;

    }
}
