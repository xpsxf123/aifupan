package com.jiuyu.governance.business.room.pojo.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 同步租户主播到直播间请求
 *
 * @author HeHui
 * @date 2026-04-24
 */
@Getter
@Setter
public class LiveRoomSyncTenantAnchorsRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 平台类型（0：抖音 1：快手 2：视频号）
     * <p>
     * 不传时默认抖音。
     * </p>
     */
    @Min(value = 0, message = "平台类型不合法")
    @Max(value = 2, message = "平台类型不合法")
    private Integer platform = 0;

    /**
     * 行业ID
     */
    private Long tradeId;

    /**
     * 归属用户（员工）ID列表
     */
    private List<Long> managerUserIds;
}
