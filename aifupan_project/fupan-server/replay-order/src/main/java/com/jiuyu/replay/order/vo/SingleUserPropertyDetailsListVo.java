package com.jiuyu.replay.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/12/22 16:44
 */
@Data
@Schema(description = "用户资产消费记录表信息项")
@Builder
public class SingleUserPropertyDetailsListVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "创建时间")
    private String createDate;

    @Schema(description = "ai分析时长")
    private String aiAnalysisTime;

    @Schema(description = "token效率")
    private String aiTokenNum;
}
