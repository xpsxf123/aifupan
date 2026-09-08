package com.jiuyu.replay.generic.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 视频数据诊断状态与内容
 */
@Data
@Schema(description = "视频数据诊断状态与内容")
public class DataDiagnosisStatusVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "诊断记录id")
    private Long id;

    @Schema(description = "数据诊断状态 0待生成 1生成中 2生成完成 3生成失败")
    private Integer qaStatus;

    @Schema(description = "已读状态 0未读 1已读")
    private Integer isRead;

    @Schema(description = "命中的提示词ID")
    private Long cueWordsId;

    @Schema(description = "诊断回答内容，仅 qaStatus=2 时有值")
    private String content;
}
