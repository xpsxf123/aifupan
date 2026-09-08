package com.jiuyu.replay.video.project.bo.subscription;

import com.jiuyu.replay.common.validated.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 达人订阅更新业务对象
 *
 * @author RayChou
 * @date 2025-08-14
 * @description 达人订阅状态更新、分组移动等操作参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "达人订阅更新业务对象")
public class InfluencerSubscriptionUpdateBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订阅ID列表
     */
    @Schema(description = "订阅ID列表", example = "[1, 2, 3]")
    @NotNull(message = "订阅ID列表不能为空")
    private List<@Positive(message = "订阅ID必须为正数") Long> subscriptionIds;

    /**
     * 操作类型: 1-暂停订阅, 2-恢复订阅, 3-取消订阅, 4-移动分组, 5-开启自动同步, 6-关闭自动同步
     */
    @Schema(description = "操作类型: 1-暂停订阅, 2-恢复订阅, 3-取消订阅, 4-移动分组, 5-开启自动同步, 6-关闭自动同步", example = "1")
    @NotNull(message = "操作类型不能为空")
    @EnumValue(byteValues = {1, 2, 3, 4, 5, 6}, message = "操作类型不合法")
    private Byte operationType;

    /**
     * 目标分组ID（移动分组时必填）
     */
    @Schema(description = "目标分组ID（移动分组时必填）", example = "2")
    private Long targetGroupId;

    /**
     * 操作原因/备注
     */
    @Schema(description = "操作原因/备注", example = "暂时不需要关注此达人")
    private String reason;
}
