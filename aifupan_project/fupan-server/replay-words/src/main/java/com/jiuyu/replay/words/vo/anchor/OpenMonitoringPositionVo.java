package com.jiuyu.replay.words.vo.anchor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/7/1 下午3:16
 */
@Data
public class OpenMonitoringPositionVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "是否开启弹幕监控 0否， 1是")
    private Integer isBarrageMonitoring;

    @Schema(description = "是否开启数据看板 0：否 1：是")
    private Integer isDataViewing;
}
