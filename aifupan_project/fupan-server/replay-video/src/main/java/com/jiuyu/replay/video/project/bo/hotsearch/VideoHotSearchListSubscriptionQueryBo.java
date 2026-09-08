package com.jiuyu.replay.video.project.bo.hotsearch;

import com.jiuyu.replay.common.validated.EnumValue;
import com.jiuyu.replay.video.project.bo.VideoPageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author RayChou
 * @date 2025/9/8 16:01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "爆款订阅视频列表查询业务对象")
public class VideoHotSearchListSubscriptionQueryBo extends VideoPageBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "订阅ID", example = "1238129381293")
    @NotNull(message = "订阅ID不能为空")
    private Long subscriptionId;

    @Schema(description = "点赞数条件大于某值", example = "10")
    private Long likeCount;

    @Schema(description = "评论数条件大于某值", example = "10")
    private Long commentCount;

    @Schema(description = "分享数条件大于某值", example = "10")
    private Long shareCount;

    @Schema(description = "收藏数条件大于某值", example = "10")
    private Long collectCount;

    @Schema(description = "发布时间开始值", example = "2025-08-11 10:30:00")
    private LocalDateTime publishStartTime;

    @Schema(description = "发布时间结束值", example = "2025-08-11 10:30:00")
    private LocalDateTime publishEndTime;

    @Schema(description = "发布时间固定值: -1-只看今天 0-近三天 1-近一周, 2-近半个月, 3-近一个月, 4-近3个月 5-近6个月", example = "1")
    @EnumValue(byteValues = {-1, 0, 1, 2, 3, 4, 5}, message = "发布时间固定值不合法")
    private Byte publishTimeValue;

    @Schema(description = "时长开始值", example = "20")
    private Integer durationStartNumber;

    @Schema(description = "时长结束值", example = "60")
    private Integer durationEndNumber;

    @Schema(description = "视频更新时间类型 1：今日更新 2：3日更新", example = "1")
    @EnumValue(byteValues = {1, 2}, message = "视频更新时间类型不合法")
    private Byte videoUpdateTimeType;

    @Schema(description = "粉丝量筛选条件: 1-5000以内, 2-1万以内, 3-2万以内, 4-5万以内, 5-10万以内", example = "1")
    @EnumValue(byteValues = {1, 2, 3, 4, 5}, message = "粉丝量筛选条件不合法")
    private Byte followersCountFilter;

    @Schema(description = "评赞比筛选条件: 1-大于5%, 2-大于10%, 3-大于15%, 4-大于20%, 5-大于25%", example = "1")
    @EnumValue(byteValues = {1, 2, 3, 4, 5}, message = "评赞比筛选条件不合法")
    private Byte commentLikeRatioFilter;

    @Schema(description = "排序类型标识 1：默认排序 2：点赞数排序 3：评论数排序 4：转发数排序 5：收藏数排序 6：粉丝量排序 7：评赞比排序", example = "1")
    @EnumValue(byteValues = {1, 2, 3, 4, 5, 6, 7}, message = "排序类型标识不合法")
    private Byte sortCode = (byte) 1;

    @Schema(description = "排序顺序 0：降序 1：升序", example = "0")
    @EnumValue(byteValues = {0, 1}, message = "排序顺序不合法")
    private Byte sortSequence = (byte) 0;
}
