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
public class TeamCardStatisticsDataBo extends PageBo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "开始时间")
    @NotNull(message = "开始时间不能为空", groups = DateChange.class)
    private String startDate;

    @Schema(description = "结束时间")
    @NotNull(message = "结束时间不能为空", groups = DateChange.class)
    private String endDate;

    @Schema(description = "部门id")
    @NotNull(message = "部门id不能为空")
    private Long deptId;

    @Schema(description = "客户意向")
    private String userAmbition;

    @Schema(description = "排序字段 userAmbition：客户意向，expiryDay：到期天数，accordingStatus：根据状态, level：版本等级")
    private String sortField;

    @Schema(description = "排序类型 asc：升序，desc：倒序")
    private String sortOrder;

    public static interface DateChange {
    }
}
