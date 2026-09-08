package com.jiuyu.governance.business.room.pojo.response;

import com.jiuyu.governance.common.pojo.bo.IdName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;

/**
 * 直播间排班配置属性响应
 *
 * @author HeHui
 * @date 2026-03-26
 */
@Getter
@Setter
public class LiveRoomScheduleAttributeResponse {

    /**
     * 直播间ID
     */
    private Long id;

    /**
     * 轮班开始时间
     */
    private LocalTime startPlan;

    /**
     * 轮班结束时间
     */
    private LocalTime endPlan;

    /**
     * 班次时长（分钟）可选项
     */
    private List<Integer> shiftOptions;

    /**
     * 休息时长（分钟）可选项
     */
    private List<Integer> restOptions;

    /**
     * 岗位可选项
     */
    private List<IdName> positionOptions;
}
