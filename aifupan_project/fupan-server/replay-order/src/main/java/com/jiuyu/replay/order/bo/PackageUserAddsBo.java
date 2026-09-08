package com.jiuyu.replay.order.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 自定义版本用户批量添加参数
 */
@Data
@Schema(description = "自定义版本用户批量添加参数")
public class PackageUserAddsBo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "版本id")
    @NotNull(message = "版本id不能为空")
    private Long packageId;

    @Schema(description = "手机号集合")
    @NotBlank(message = "手机号不能为空")
    private String phones;
}
