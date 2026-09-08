package com.jiuyu.replay.generic.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 数据诊断未读报告
 */
@Data
@Schema(description = "数据诊断未读报告")
public class UnreadDiagnosisReportVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "诊断记录id")
    private Long id;

    @Schema(description = "视频id")
    private String videoId;

    @Schema(description = "主播头像")
    private String anchorAvatar;

    @Schema(description = "主播名称")
    private String anchorName;

    @Schema(description = "视频名称")
    private String videoName;

    @Schema(description = "视频开始时间")
    private Date startTime;

    @Schema(description = "视频结束时间")
    private Date endTime;

    @Schema(description = "阅读状态 0未读 1已读")
    private Integer isRead;

    @Schema(description = "命中的提示词ID")
    private Long cueWordsId;

    @Schema(description = "行业id")
    private Long tradeId;

    @Schema(description = "视频时长")
    private Long duration;

    private String secUid;
}
