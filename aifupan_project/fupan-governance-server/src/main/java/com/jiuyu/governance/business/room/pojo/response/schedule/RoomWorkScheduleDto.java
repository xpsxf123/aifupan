package com.jiuyu.governance.business.room.pojo.response.schedule;

import com.jiuyu.governance.business.room.pojo.entity.WorkSchedule;
import lombok.Getter;
import lombok.Setter;

/**
 * 直播间班次数据类
 *
 * @author HeHui
 * @date 2026-03-26 21:19
 */
@Getter
@Setter
public class RoomWorkScheduleDto extends WorkSchedule {

    /**
     * 平台类型 0抖音，1快手，2视频号
     */
    private Integer platformType;


    /**
     * 平台下 直播间唯一号
     */
    private String secUid;

}
