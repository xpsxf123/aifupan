package com.jiuyu.replay.generic.vo.words;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 授权用量信息
 *
 * <p>返回指定授权类型的授权总量、已用量及剩余量，
 * 供客户端在发起授权前动态校验是否有可用授权位。</p>
 *
 * @author jxy
 * @date 2026-07-31
 */
@Data
@Schema(description = "授权用量信息")
public class AuthUsageVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "授权总量")
    private Long totalCount;

    @Schema(description = "已使用数量")
    private Long usedCount;

    @Schema(description = "剩余数量")
    private Long remainingCount;
}
