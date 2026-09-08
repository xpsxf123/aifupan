package com.jiuyu.replay.generic.dto.activity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author RayChou
 * @date 2025/5/29 10:26
 */
@Data
@Schema(description = "服务调用实体-用户邀请码信息传输对象")
public class InviteUrlCodeInfoDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "url链接code")
    private Long urlCode;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "子账号用户id")
    private Long subUserId;

    @Schema(description = "活动id")
    private Long activityId;

    @Schema(description = "邀请code链接类型 0：代理商链接 1：推广渠道链接 2：用户链接 3：代理商销售链接", allowableValues = {"0", "1", "2", "3"}, defaultValue = "2")
    private Integer codeType;

    @Schema(description = "租户id")
    private Long tenantId;

}
