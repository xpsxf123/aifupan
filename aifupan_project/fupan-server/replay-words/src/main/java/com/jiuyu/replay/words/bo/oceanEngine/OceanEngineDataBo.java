package com.jiuyu.replay.words.bo.oceanEngine;

import com.jiuyu.replay.generic.dto.words.viewing.FlowSourceDto;
import com.jiuyu.replay.generic.dto.words.viewing.UserPortraitDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.apache.ibatis.annotations.Update;
import org.hibernate.validator.constraints.Length;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 巨量引擎数据表详情查询BO
 *
 * @author liaoxin
 * @date 2025-06-13
 */
@Data
@Schema(description = "巨量引擎数据表详情查询")
public class OceanEngineDataBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @Schema(description = "主键id")
    private Long id;

    /**
     * 用户id
     */
    @Schema(description = "用户id")
    private Long userId;

    /**
     * 视频id
     */
    @Schema(description = "视频id")
    @NotBlank(message = "视频ID不能为空", groups = {Update.class})
    @Length(max = 50, message = "视频ID不合法", groups = {Update.class})
    private String videoId;

    /**
     * 租户id
     */
    @Schema(description = "租户id")
    private Long tenantId;

    /**
     * 主播id
     */
    @Schema(description = "主播id")
    private String secUid;

    /**
     * 主播账号
     */
    @Schema(description = "主播账号")
    private String anchorNumber;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 创建人id
     */
    @Schema(description = "创建人id")
    private Long createUserId;

    /**
     * 修改人id
     */
    @Schema(description = "修改人id")
    private Long updateUserId;

    /**
     * 巨量oss存储地址
     */
    @Schema(description = "巨量oss存储地址")
    private String ossPath;

    /**
     * 批次号
     */
    @Schema(description = "批次号")
    private String batchNumber;


    //巨量数据

    /**
     * 看播流量结构
     */
    private List<FlowSourceDto> watchFlowList;

    /**
     * 成交流量结构
     */
    private List<FlowSourceDto> payFlowList;


    /**
     * 看播用户画像
     */
    private UserPortraitDto watchUserPortrait;

    /**
     * 成交用户画像
     */
    private UserPortraitDto payUserPortrait;

    /**
     * 是否带货 0：否 1：是
     */
    private Integer isTakeProduct;

    /**
     * 总观看人次
     */
    private Integer totalWatchNum;
    /**
     * 平均在线人数
     */
    private Integer averageOnlineNum;
    /**
     * 平均停留时间(秒)
     */
    private Integer averageResidenceTime;
    /**
     * 新增粉丝数
     */
    private Integer incrementFollowerCount;
    /**
     * 粉丝转化率
     */
    private Double convertFanRate;
    /**
     * 互动率
     */
    private Double interactionPercent;
    /**
     * 销售额(单位:分)
     */
    private Integer volume;
    /**
     * 销量
     */
    private Integer purchaseCount;
    /**
     * 客单价
     */
    private Double customerUnitPrice;
    /**
     * uv价值
     */
    private Double uvValue;
    /**
     * 带货转换率
     */
    private Double goodsConvertRate;
    /**
     * 千次观看成交金额 （单位：分）
     */
    private Double gpm;
    /**
     * 曝光-观看率
     */
    @Schema(description = "曝光-观看率")
    private Double showWatchCntRatio;
    /**
     * ROI
     */
    @Schema(description = "ROI")
    private Double roi;
    /**
     * 投放ROI金额（单位：分）
     */
    @Schema(description = "投放ROI金额（单位：分）")
    private Double launchRoiAmount;
    /**
     * 退款金额（单位：分）
     */
    @Schema(description = "退款金额（单位：分）")
    private Double refundAmount;
    /**
     * 整体消耗ROI
     */
    @Schema(description = "整体消耗ROI")
    private Double overallCostRoi;
    /**
     * 净成交ROI
     */
    @Schema(description = "净成交ROI")
    private Double netTransactionRoi;

}