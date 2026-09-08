package com.jiuyu.replay.ai.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author ：lujie
 * @description：更新HTML状态参数
 * @date ：2025/12/18
 */
@Data
@Schema(description = "更新HTML状态参数")
public class UpdateHtmlStatusBo {

    @Schema(description = "数据id")
    @NotNull(message = "数据id不能为空")
    private String id;

    @Schema(description = "html生成状态 0：待生成，1：生成中，2：生成成功，3：生成失败")
    @NotNull(message = "html生成状态不能为空")
    private Integer htmlStatus;

    @Schema(description = "html保存路径")
    private String htmlSavePath;

    @Schema(description = "html生成错误原因 失败传")
    private String htmlCreateError;

    /**
     * html生成类型 0：服务器，1：客户端
     */
    private Integer htmlType;

}
