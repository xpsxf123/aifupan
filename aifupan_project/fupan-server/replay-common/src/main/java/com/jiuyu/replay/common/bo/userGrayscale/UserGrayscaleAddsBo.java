package com.jiuyu.replay.common.bo.userGrayscale;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/12/5 17:11
 */
@Data
@Schema(description = "用户灰度添加")
public class UserGrayscaleAddsBo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "版本id")
    @NotNull(message = "版本id不能为空")
    private Long versionId;

    @Schema(description = "手机号集合")
    @NotNull(message = "手机号不能为空")
    private String phones;
}
