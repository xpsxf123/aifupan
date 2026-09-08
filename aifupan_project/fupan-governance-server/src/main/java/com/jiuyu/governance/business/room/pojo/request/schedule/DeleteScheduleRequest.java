package com.jiuyu.governance.business.room.pojo.request.schedule;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 删除排班请求
 *
 * @author HeHui
 * @date 2026-03-26 20:19
 */
@Getter
@Setter
public class DeleteScheduleRequest {

    /**
     * 排班ID
     */
    @NotNull(message = "缺少排班ID")
    private Long id;

    /**
     * 直播间ID
     */
    @NotNull(message = "缺少直播间ID")
    private Long liveRoomId;
}
