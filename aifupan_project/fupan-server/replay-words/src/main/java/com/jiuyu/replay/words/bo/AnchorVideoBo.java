package com.jiuyu.replay.words.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author tisheng
 * @date 2024/9/11
 * @apinNote
 */
@Data
public class AnchorVideoBo extends PageBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 分享视频分析内容
     */
    /**
     * 主播名称
     */
    @Schema(description = "主播名称")
    private String anchorName;

    @Schema(description = "主播Id")
    private Long anchorUrlId;

    @Schema(description = "搜索")
    private String keyword;
    //    @Schema(description = "享视频分析内容")
//    List<AudioAnalysisEntity> list;
    @Schema(description = "结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    //      private String endTime;
//
    @Schema(description = "结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @Schema(description = "主播唯一标")
    private String secUid;

    @Schema(description = "用户id")
    private Long userId;

    /**
     * 用户昵称
     */
    @Schema(description = "用户昵称")
    private String userName;


    /**
     * 视频名称
     */
    @Schema(description = "视频名称")
    private String videoName;

    /**
     * 用户Ids
     */
    @Schema(description = "用户Ids")
    private List<Long> userIdS;

    @Schema(description = "租户ids")
    private List<Long> tenantIds;

    /**
     * 视频id
     */
    @Schema(description = "视频id")
    private String videoId;
    /**
     * 视频ids
     */
    @Schema(description = "视频ids")
    private List<String> videoIdList;

    /**
     * 行业ID
     */
    @Schema(description = "行业ID")
    private String tradeId;

    /**
     * 行业IDs
     */
    @Schema(description = "行业IDs")
    private List<Long> tradeList;

    /**
     * 销售人员ID
     */
    @Schema(description = "销售人员ID")
    private Long userSalesID;

    /**
     * 分析状态 0：未分析 1：分析中 2：分析完成 3分析错误
     */
    @Schema(description = "分析状态")
    private Integer analysisStatus;

    /**
     * 参观人次大于等于
     */
    @Schema(description = "场观人次大于等于")
    private Integer totalWatchNum;
    /**
     * 销售额大于等于
     */
    @Schema(description = "销售额大于等于")
    private Integer volume;

    @Schema(description = "主播账号归属类型 0：自由账号 1：同行账号")
    private Integer accountType;

    @Schema(description = "版本id")
    private Long packageId;
}
