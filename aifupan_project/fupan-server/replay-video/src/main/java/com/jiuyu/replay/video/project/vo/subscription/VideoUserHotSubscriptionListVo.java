package com.jiuyu.replay.video.project.vo.subscription;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 达人订阅分组视图对象
 *
 * @author RayChou
 * @date 2025-08-14
 * @description 按分组展示的达人订阅数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "爆款订阅列表对象")
public class VideoUserHotSubscriptionListVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分组ID
     */
    @Schema(description = "分组ID")
    private Long groupId;

    /**
     * 分组名称
     */
    @Schema(description = "分组名称", example = "分组1")
    private String groupName;

    /**
     * 分组创建时间（用于排序）
     */
    @Schema(description = "分组创建时间", example = "2025-08-27 10:30:00")
    private LocalDateTime createTime;

    /**
     * 订阅列表
     */
    @Schema(description = "订阅列表")
    private List<HotSubscriptionItemVo> hotSubscriptionItemVos;


    /**
     * 视频提取项视图对象
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "视频订阅列表对象")
    public static class HotSubscriptionItemVo implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 订阅ID
         */
        @Schema(description = "订阅ID")
        private Long subscriptionId;

        /**
         * 关键词
         */
        @Schema(description = "关键词", example = "关键词1")
        private String keyword;

        /**
         * 平台类型
         */
        @Schema(description = "平台类型: 1-抖音, 2-快手, 3-视频号", example = "1")
        private Byte platformType;

        /**
         * 行业
         */
        @Schema(description = "行业", example = "教育培训")
        private String industry;

        /**
         * 行业ID
         */
        @Schema(description = "行业ID", example = "12332")
        private Long industryId;

        /**
         * 分组ID
         */
        @Schema(description = "分组ID", example = "12332")
        private Long groupId;

        /**
         * 订阅点赞阈值
         */
        @Schema(description = "订阅点赞阈值", example = "100")
        private Integer subscriptionLikeCountThreshold;

        /**
         * 自动提取文案开关
         */
        @Schema(description = "自动提取文案开关", example = "true")
        private Byte autoSyncEnabled;

        /**
         * 自动提取文案点赞阈值
         */
        @Schema(description = "自动提取文案点赞阈值", example = "1")
        private Integer likeCountThreshold;

        /**
         * 自动提取文案作品更新时间阈值: 0-全部 1-近一周, 2-近半个月, 3-近一个月, 4-近3个月 5-近6个月
         */
        @Schema(description = "自动提取文案作品更新时间阈值: 0-全部 1-近一周, 2-近半个月, 3-近一个月, 4-近3个月 5-近6个月", example = "1")
        private Byte updateTimeCondition;

        /**
         * 总视频数
         */
        @Schema(description = "总视频数", example = "100")
        private Integer videoCount;

        /**
         * 今日新增
         */
        @Schema(description = "今日新增", example = "5")
        private Integer todayIncrement;


        /**
         * 今日更新时间
         */
        @Schema(description = "今日更新时间", example = "2025-08-26 10:30:00")
        private LocalDateTime todaySyncTime;

        /**
         * 3日新增
         */
        @Schema(description = "3日新增", example = "15条")
        private Integer threeDayIncrement;

        /**
         * 3日更新时间
         */
        @Schema(description = "3日更新时间", example = "2025-08-26 08:00:00")
        private LocalDateTime threeDayIncrementTime;

        /**
         * 添加人
         */
        @Schema(description = "添加人")
        private String operator;

        @Schema(description = "用户ID")
        private Long userId;
    }
}
