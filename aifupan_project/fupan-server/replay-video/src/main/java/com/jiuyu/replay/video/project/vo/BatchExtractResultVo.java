package com.jiuyu.replay.video.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 批量提取文案结果视图对象
 * 返回视频关联记录的集合
 *
 * @author RayChou
 * @date 2025-08-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "批量提取文案结果视图对象")
public class BatchExtractResultVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 视频关联记录列表
     */
    @Schema(description = "视频关联记录列表")
    private List<VideoExtractItemVo> items;

    /**
     * 视频提取项视图对象
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "视频提取项视图对象")
    public static class VideoExtractItemVo implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 用户视频关联表ID
         */
        @Schema(description = "用户视频关联表ID", example = "2001")
        private Long id;

        /**
         * 视频ID
         */
        @Schema(description = "视频ID", example = "1001")
        private Long videoId;

        /**
         * 提取状态
         */
        @Schema(description = "提取状态：1-待处理，2-处理中，3-已完成，4-失败", example = "1")
        private Byte extractStatus;

        /**
         * 来源类型
         */
        @Schema(description = "来源类型：3-达人视频列表，4-爆款视频", example = "3")
        private Byte sourceType;

        /**
         * 来源id
         */
        @Schema(description = "来源ID", example = "343242")
        private Long sourceId;

        /**
         * 创建时间
         */
        @Schema(description = "创建时间", example = "2025-08-27 10:30:00")
        private LocalDateTime createTime;

        /**
         * 更新时间
         */
        @Schema(description = "更新时间", example = "2025-08-27 10:30:00")
        private LocalDateTime updateTime;

    }

}
