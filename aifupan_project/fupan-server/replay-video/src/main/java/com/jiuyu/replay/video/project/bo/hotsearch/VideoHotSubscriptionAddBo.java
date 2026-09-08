package com.jiuyu.replay.video.project.bo.hotsearch;

import com.jiuyu.replay.common.validated.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;

/**
 * 添加爆款订阅业务对象
 *
 * @author RayChou
 * @date 2025-08-14
 * @description 添加爆款订阅参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "添加爆款订阅业务对象")
public class VideoHotSubscriptionAddBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关键词
     */
    @Schema(description = "关键词", example = "关键词1")
    @NotBlank(message = "关键词不能为空")
    @Length(max = 100, message = "关键词长度不能超过100个字符")
    private String keyword;

    /**
     * 平台类型: 1-抖音, 2-快手, 3-视频号
     */
    @Schema(description = "平台类型: 1-抖音, 2-快手, 3-视频号", example = "1")
    @NotNull(message = "订阅平台类型不能为空")
    @EnumValue(byteValues = {1, 2, 3}, message = "订阅类型不合法")
    private Byte platformType;

    /**
     * 行业ID
     */
    @Schema(description = "行业ID", example = "1")
    @NotNull(message = "行业ID不能为空")
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
    @NotNull(message = "订阅点赞数阈值条件不能为空")
    @Min(value = 0, message = "订阅点赞数阈值条件不能小于0")
    private Integer likeCountMin;

    /**
     * 是否启用自动同步文案: 0-否, 1-是
     */
    @Schema(description = "是否启用自动同步文案: 0-否, 1-是", example = "1")
    @NotNull(message = "自动同步文案设置不能为空")
    @EnumValue(byteValues = {0, 1}, message = "自动同步文案设置不合法")
    private Byte autoSyncEnabled;

    /**
     * 点赞大于（自动提取文案条件）
     */
    @Schema(description = "点赞大于（自动同步条件）", example = "1000")
    @Min(value = 0, message = "点赞数不能小于0")
    @Max(value = 999999999, message = "点赞数不能超过999999999")
    private Integer likeCountThreshold;

    /**
     * 自动提取文案作品更新时间阈值: 0-全部 1-近一周, 2-近半个月, 3-近一个月, 4-近3个月 5-近6个月
     */
    @Schema(description = "自动提取文案作品更新时间阈值: 0-全部 1-近一周, 2-近半个月, 3-近一个月, 4-近3个月 5-近6个月", example = "1")
    @EnumValue(byteValues = {0, 1, 2, 3, 4, 5}, message = "更新时间条件不合法")
    private Byte updateTimeCondition;
}
