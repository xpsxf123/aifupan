package com.jiuyu.replay.words.bo;

import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CloudVideoListBo extends PageBo {

    /**
     * 租户id
     */
    @Schema(description = "租户id")
    private Long tenantId;
    /**
     * 主播secuid
     */
    @Schema(description = "主播secuid")
    private String secUid;
    /**
     * 录制开始时间 yyyy-MM-dd
     */
    @Schema(description = "录制开始时间 yyyy-MM-dd")
    private String recordStartDate;
    /**
     * 录制结束时间 yyyy-MM-dd
     */
    @Schema(description = "录制结束时间 yyyy-MM-dd")
    private String recordEndDate;
    /**
     * 分析开始时间 yyyy-MM-dd
     */
    @Schema(description = "分析开始时间 yyyy-MM-dd")
    private String analysisStartDate;
    /**
     * 分析结束时间 yyyy-MM-dd
     */
    @Schema(description = "分析结束时间 yyyy-MM-dd")
    private String analysisEndDate;

}
