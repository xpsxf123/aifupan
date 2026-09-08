package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/1/4 下午2:02
 */
@Data
@Schema(description = "websocket采集的信息信息-不传json")
public class UploadSocketDataBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @Schema(description = "ID")
    private Long id;
    /**
     * 用户id
     */
    @Schema(description = "用户id")
    private Long userId;
    /**
     * 租户id
     */
    @Schema(description = "租户id")
    private Long tenantId;
    /**
     * 主播secUid
     */
    @Schema(description = "主播secUid")
    private String secUid;
    /**
     * 直播场次号
     */
    @Schema(description = "直播场次号")
    private String batchNumber;
    /**
     * 视频唯一标识
     */
    @Schema(description = "视频唯一标识")
    private String videoId;
    /**
     * cos文件的key
     */
    @Schema(description = "cos文件的key")
    private String cosKey;
    /**
     * 开始时间
     */
    @Schema(description = "开始时间")
    private Date startDate;
    /**
     * 结束时间
     */
    @Schema(description = "结束时间")
    private Date endDate;
    /**
     * 累计场观人数
     */
    @Schema(description = "累计观看人数")
    private String totalOnlineNum;

    @Schema(description = "场观人数")
    private String observationNum;

    @Schema(description = "视频弹幕总数")
    private Integer totalBarrageNum;

    @Schema(description = "场次的弹幕总数")
    private String totalBulletChatNum;

    @Schema(description = "最高在线人数")
    private Integer onlineMaxNum;
}
