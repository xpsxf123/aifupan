package com.jiuyu.replay.generic.bo.power.statistics;

import com.jiuyu.replay.generic.bo.common.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2026/1/30 10:35
 */
@Data
public class SalesListStatisticsPageBo extends PageBo implements Serializable {
    private static final long serialVersionUID = 1L;


    @Schema(description = "销售id")
    @NotNull(message = "销售id不能为空")
    private Long salesId;

    @Schema(description = "搜索类型：0：待跟进，1：已过期未跟进")
    private Integer selectType;

    @Schema(description = "排序字段 userAmbition：客户意向，expiryDay：到期天数，accordingStatus：根据状态, level：版本等级")
    private String sortField;

    @Schema(description = "排序类型 asc：升序，desc：倒序")
    private String sortOrder;
}
