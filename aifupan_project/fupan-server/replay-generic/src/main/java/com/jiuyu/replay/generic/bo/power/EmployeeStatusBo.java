package com.jiuyu.replay.generic.bo.power;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 员工状态修改参数
 *
 * @author Qwen
 */
@Data
@Schema(description = "员工状态修改参数")
public class EmployeeStatusBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 根据 sourceType 对应不同 ID
     */
    @Schema(description = "根据 sourceType 对应不同 ID 0-userId，1-agentId，2-salesId")
    @NotNull(message = "sourceId不能为空")
    private Long sourceId;

    /**
     * 0-用户，1-代理商，2-销售
     */
    @Schema(description = "0-用户，1-代理商，2-销售")
    @NotNull(message = "sourceType不能为空")
    private Integer sourceType;

    /**
     * 0-离职，1-在职
     */
    @Schema(description = "0-离职，1-在职")
    @NotNull(message = "employeeStatus不能为空")
    private Integer employeeStatus;
}