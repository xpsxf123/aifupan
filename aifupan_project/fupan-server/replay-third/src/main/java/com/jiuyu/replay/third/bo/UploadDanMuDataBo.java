package com.jiuyu.replay.third.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/1/14 下午2:09
 */
@Data
public class UploadDanMuDataBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主播id
     */
    @Schema(description = "主播id")
    private String secUid;

    /**
     * 直播场次号
     */
    @Schema(description = "直播场次号")
    private String batchNumber;

    /**
     * 视频id
     */
    @Schema(description = "视频id")
    private String videoId;
}
