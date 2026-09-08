package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * lujie
 * 2024-12-05
 */
@Data
@Schema(description = "websocket采集数据")
public class SocketDataBo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "直播标识")
    private String secUid;

    @Schema(description = "直播场次号")
    private String batchNumber;

    @Schema(description = "版本 有1.0, 2.0")
    private String version;

    @Schema(description = "视频开始录制时间")
    private Date videoStartTime;

    @Schema(description = "视频结束录制时间")
    private Date videoEndTime;

    @Schema(description = "开始时间")
    private Date serviceStartTime;

    @Schema(description = "结束时间")
    private Date serviceEndTime;

    @Schema(description = "记录过程")
    private List<SocketProcessDataBo> datas;

    public SocketDataBo() {}

    public SocketDataBo(UploadSocketMessageBo bo) {
        this.serviceEndTime = bo.getEndDate();
        this.serviceStartTime = bo.getStartDate();
        this.videoEndTime = bo.getEndDate();
        this.videoStartTime = bo.getStartDate();
        this.batchNumber = bo.getBatchNumber();
        this.secUid = bo.getSecUid();
        this.userId = bo.getUserId();
    }
}
