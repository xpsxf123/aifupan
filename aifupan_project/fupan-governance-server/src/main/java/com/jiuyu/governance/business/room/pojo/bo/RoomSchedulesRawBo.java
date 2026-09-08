package com.jiuyu.governance.business.room.pojo.bo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 直播间排班计划原始数据
 *
 * @author HeHui
 * @date 2026-04-09 12:08
 */
@Getter
@Setter
public class RoomSchedulesRawBo {


    /**
     * 企业管理后台 - 直播间ID
     */
    private Long roomId;


    /**
     *  直播间排班ID
     */
    private Long roomSchedulesId;



    /**
     * 班次归属天
     */
    private LocalDate workDay;


    /**
     * 班次开始时间
     */
    private LocalTime startWork;


    /**
     * 班次结束时间
     */
    private LocalTime endWork;


    /**
     * 班次时长 (分钟)
     */
    private Integer scheduleDuration;
}
