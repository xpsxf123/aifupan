package com.jiuyu.governance.business.room.pojo.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 同步直播间结果响应
 *
 * @author HeHui
 * @date 2026-04-24
 */
@Getter
@Setter
public class LiveRoomSyncResultResponse {

    /**
     * 远端去重后的主播数量
     */
    private Integer total;

    /**
     * 新增直播间数量
     */
    private Integer created;

    /**
     * 更新直播间数量
     */
    private Integer updated;
}
