package com.jiuyu.replay.ai.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author ：lujie
 * @description：更新AI纠正状态参数
 */
@Data
@Schema(description = "更新AI纠正状态参数")
public class UpdateCorrectStatusBo {

    @Schema(description = "数据id")
    @NotNull(message = "数据id不能为空")
    private String id;

    @Schema(description = "ai纠错状态 0：正常，1：纠错中，2：纠错完成，3：纠错失败")
    @NotNull(message = "ai纠错状态不能为空")
    private Integer aiCorrectStatus;

    @Schema(description = "纠正后的内容（客户端纠正完成后传）")
    private String content;

    @Schema(description = "ai纠错错误原因（失败时传）")
    private String aiCorrectError;

    /**
     * 纠错来源类型 0：服务器，1：客户端
     */
    private Integer aiCorrectType;

}
