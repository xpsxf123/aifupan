package com.jiuyu.replay.order.bo;

import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 自定义版本用户分页查询参数
 */
@Data
@Schema(description = "自定义版本用户分页查询参数")
public class PackageUserListBo extends PageBo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "版本id")
    @NotNull(message = "版本id不能为空")
    private Long packageId;

    @Schema(description = "用户手机号或昵称关键字")
    private String keyword;
}
