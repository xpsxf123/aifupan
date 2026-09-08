package com.jiuyu.replay.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 自定义版本用户VO
 */
@Data
@Schema(description = "自定义版本用户")
public class PackageUserVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "id")
    private Long id;

    @Schema(description = "版本id")
    private Long packageId;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "租户id")
    private Long tenantId;

    @Schema(description = "用户手机号")
    private String phone;

    @Schema(description = "用户昵称")
    private String nickName;

    @Schema(description = "修改时间")
    private Date updateDate;

    @Schema(description = "创建时间")
    private Date createDate;
}
