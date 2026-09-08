package com.jiuyu.replay.video.project.vo.hotsearch;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 爆款订阅视图对象
 *
 * @author RayChou
 * @date 2025-08-14
 * @description 爆款订阅列表显示数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "爆款订阅视图对象")
public class VideoHotSubscriptionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订阅ID
     */
    @Schema(description = "订阅ID", example = "1001")
    private Long subscriptionId;

    /**
     * 关键词
     */
    @Schema(description = "关键词", example = "关键词1")
    private String keyword;

    /**
     * 行业
     */
    @Schema(description = "行业", example = "教育培训")
    private String industry;

    /**
     * 短视频数量
     */
    @Schema(description = "短视频数量", example = "54")
    private Integer videoCount;

    /**
     * 今日更新数量
     */
    @Schema(description = "今日更新数量", example = "5")
    private Integer todayUpdateCount;

    /**
     * 今日更新时间
     */
    @Schema(description = "今日更新时间", example = "13时35分更新")
    private String todayUpdateTime;

    /**
     * 3日更新数量
     */
    @Schema(description = "3日更新数量", example = "5")
    private Integer threeDayUpdateCount;

    /**
     * 3日更新时间
     */
    @Schema(description = "3日更新时间", example = "13时35分更新")
    private String threeDayUpdateTime;

    /**
     * 创建人
     */
    @Schema(description = "创建人", example = "无敌的牛男士")
    private String creator;
}
