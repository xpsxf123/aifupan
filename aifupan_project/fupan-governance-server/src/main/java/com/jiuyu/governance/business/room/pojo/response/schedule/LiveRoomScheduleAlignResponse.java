package com.jiuyu.governance.business.room.pojo.response.schedule;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LiveRoomScheduleAlignResponse extends LiveRoomSchedulePageResponse {

    /**
     * 直播平台类型
     * 0 抖音，1快手，2视频号
     */
    private Integer livePlatformType;

    /**
     * 主播ID
     */
    private String secUid;

    /**
     * videoId
     */
    private String videoId;
}

