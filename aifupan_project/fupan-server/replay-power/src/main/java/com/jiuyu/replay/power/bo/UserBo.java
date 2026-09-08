package com.jiuyu.replay.power.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author tisheng
 * @date 2024/9/19
 * @apinNote
 */
@Data
public class UserBo implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 冻结状态 0：未冻结 1：已冻结
     */
    @Schema(description = "冻结状态 0：未冻结 1：已冻结")
    private Integer status;

    /**
     * 用户id
     */
    @Schema(description = "用户id")
    private Long  userId;

    /**
     * 父级用户id
     */
    @Schema(description = "父级用户id")
    private Long parentId;

    /**
     * 账号类型
     */
    @Schema(description = "账号类型")
    private Integer userType;

}
