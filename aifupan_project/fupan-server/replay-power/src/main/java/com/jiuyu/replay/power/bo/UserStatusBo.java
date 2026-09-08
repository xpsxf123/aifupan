package com.jiuyu.replay.power.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author tisheng
 * @date 2024/9/14
 * @apinNote
 */
@Data
@Schema(description = "用户详情表信息")
public class UserStatusBo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "0 代表激活 1 锁定")
    private Integer status;

    @Schema(description = "0 代表激活 1 锁定")
    private Long userId;
}
