package com.jiuyu.replay.common.bo.userGrayscale;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/12/5 16:43
 */
@Data
@Schema(description = "用户灰度参数")
public class UserGrayscaleBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Schema(description = "id")
    private Long id;

    /**
     * 版本id
     */
    @Schema(description = "版本id")
    @NotNull(message = "版本id不能为空")
    private Long versionId;
    /**
     * 用户id
     */
    @Schema(description = "用户id")
    @NotNull(message = "用户id不能为空")
    private Long userId;
    /**
     * 用户手机号
     */
    @Schema(description = "用户手机号")
    private String phone;
    /**
     * 用户昵称
     */
    @Schema(description = "用户昵称")
    private String nickName;
}
