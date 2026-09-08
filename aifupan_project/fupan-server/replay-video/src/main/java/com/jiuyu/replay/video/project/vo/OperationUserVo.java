package com.jiuyu.replay.video.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author RayChou
 * @date 2025/8/16 15:11
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "操作用户视图对象")
public class OperationUserVo {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "账号类型 0：主账号 2：子账号")
    private Integer userType;

    @Schema(description = "操作人昵称")
    private String nickName;
}
