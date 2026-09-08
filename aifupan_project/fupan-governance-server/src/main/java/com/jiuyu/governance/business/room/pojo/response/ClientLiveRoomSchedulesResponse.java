package com.jiuyu.governance.business.room.pojo.response;

import com.jiuyu.governance.business.room.pojo.constants.LivePlatformType;
import com.jiuyu.governance.plugins.webmvc.serializer.EnumDesc;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 面向客户端的直播间排班计划
 *
 * @author HeHui
 * @date 2026-03-24 14:53
 */
@Getter
@Setter
public class ClientLiveRoomSchedulesResponse {

    /**
     * 平台类型 0抖音，1快手，2视频号
     */
    @EnumDesc(LivePlatformType.class)
    private Integer platformType;


    /**
     * 平台下 直播间唯一号
     */
    private String secUid;


    /**
     * 企业管理后台 - 直播间ID
     */
    private Long governanceRoomId;


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
    private LocalDateTime startWork;


    /**
     * 班次结束时间
     */
    private LocalDateTime endWork;


    /**
     * 班次时长 (分钟)
     */
    private Integer scheduleDuration;
}

