package com.jiuyu.replay.video.project.bo.hotsearch;

import com.jiuyu.replay.common.validated.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 编辑爆款订阅业务对象
 *
 * @author RayChou
 * @date 2025-08-14
 * @description 编辑爆款订阅参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "编辑爆款订阅业务对象")
public class VideoHotSubscriptionEditBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订阅ID
     */
    @Schema(description = "订阅ID", example = "1001")
    @NotNull(message = "订阅ID不能为空")
    private Long subscriptionId;

    /**
     * 行业ID
     */
    @Schema(description = "行业ID", example = "1")
    private Long industryId;

    /**
     * 分组ID
     */
    @Schema(description = "分组ID", example = "1")
    private Long groupId;

    /**
     * 点赞大于（订阅条件）
     */
    @Schema(description = "点赞大于", example = "1000")
    @Min(value = 0, message = "订阅点赞数阈值条件不能小于0")
    private Integer likeCountMin;

    /**
     * 是否启用自动同步文案: 0-否, 1-是
     */
    @Schema(description = "是否启用自动同步文案: 0-否, 1-是", example = "1")
    @EnumValue(byteValues = {0, 1}, message = "自动同步文案设置不合法")
    private Byte autoSyncEnabled;

    /**
     * 点赞大于（自动提取文案条件）
     */
    @Schema(description = "点赞大于（自动同步条件）", example = "1000")
    private Integer likeCountThreshold;

    /**
     * 自动提取文案作品更新时间阈值: 0-全部 1-近一周, 2-近半个月, 3-近一个月, 4-近3个月 5-近6个月
     */
    @Schema(description = "自动提取文案作品更新时间阈值: 0-全部 1-近一周, 2-近半个月, 3-近一个月, 4-近3个月 5-近6个月", example = "1")
    @EnumValue(byteValues = {0, 1, 2, 3, 4, 5}, message = "更新时间条件不合法")
    private Byte updateTimeCondition;
}
