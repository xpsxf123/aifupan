package com.jiuyu.replay.ai.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 批量更新诊断报告已读状态入参
 */
@Data
@Schema(description = "批量更新诊断报告已读状态入参")
public class UpdateReadStatusBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "ids不能为空")
    @Schema(description = "诊断报告id列表")
    private List<Long> ids;

    @Schema(description = "是否已读 0未读 1已读，默认1")
    private Integer isRead;
}
