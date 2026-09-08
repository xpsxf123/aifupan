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
@Schema(description = "达人订阅分组视图对象")
public class InfluencerSubscriptionGroupVo implements Serializable {

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
     * 达人数量
     */
    @Schema(description = "达人数量", example = "4")
    private Integer influencerCount;

    /**
     * 达人列表
     */
    @Schema(description = "达人列表")
    private List<InfluencerSubscriptionVo> influencers;
}
