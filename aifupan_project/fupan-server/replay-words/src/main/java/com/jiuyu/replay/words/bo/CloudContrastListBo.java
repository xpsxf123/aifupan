package com.jiuyu.replay.words.bo;

import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CloudContrastListBo extends PageBo {

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
     * 对比开始时间 yyyy-MM-dd
     */
    @Schema(description = "对比开始时间 yyyy-MM-dd")
    private String contrastStartDate;
    /**
     * 对比结束时间 yyyy-MM-dd
     */
    @Schema(description = "对比结束时间 yyyy-MM-dd")
    private String contrastEndDate;

}
