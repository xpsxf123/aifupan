package com.jiuyu.replay.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @Description: 是否拥有资产
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-09 16:13:50
 */
@Data
@Schema(description = "是否拥有资产")
public class IsPropertyHaveVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "是否拥有")
    private Boolean isHave;

    @Schema(description = "redis缓存id")
    private Long redisId;

    @Schema(description = "预扣id")
    private String withholdId;
}
