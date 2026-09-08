package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * websocket采集的信息信息-上传
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-12-03 15:37:56
 */
@Data
@Schema(description = "websocket采集的信息信息-上传")
public class UploadSocketMessageBo  implements Serializable {
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
    @Schema(description = "累计场观人数")
    private String totalOnlineNum;
    /**
     * 总加入粉丝团数量
     */
    @Schema(description = "总加入粉丝团数量")
    private String totalFanGroupNum;
    /**
     * 弹幕总数
     */
    @Schema(description = "弹幕总数")
    private String totalBulletChatNum;
    /**
     * 点赞总数
     */
    @Schema(description = "点赞总数")
    private String totalThumbsUpNum;
    /**
     * 礼物总数
     */
    @Schema(description = "礼物总数")
    private String totalGiftNum;
    /**
     * 总关注数
     */
    @Schema(description = "总关注数")
    private String totalConcernNum;

    @Schema(description = "websocket数据")
    private String webSocketData;
}