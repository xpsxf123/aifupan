package com.jiuyu.replay.words.bo.video;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 查询最近直播场次参数（主播下播后平台数据补采集专用）。
 *
 * <p>只接收开播时间下限，userId / tenantId 从 JWT 解析注入，不入参，保证租户隔离。</p>
 */
@Data
@Schema(description = "查询最近直播场次参数")
public class ListRecentLiveSessionBo {

    /**
     * 开播时间下限（格式 yyyy-MM-dd HH:mm:ss，= now - 窗口天数）。
     * 为空时不加时间过滤，返回全部符合条件的原视频场次。
     */
    @Schema(description = "开播时间下限（格式 yyyy-MM-dd HH:mm:ss）")
    private String startTimeGe;
}
