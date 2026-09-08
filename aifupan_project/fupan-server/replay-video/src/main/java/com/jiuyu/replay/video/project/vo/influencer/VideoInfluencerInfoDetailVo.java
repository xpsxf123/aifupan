package com.jiuyu.replay.video.project.vo.influencer;

import com.jiuyu.replay.video.project.entity.VideoInfluencerInfoEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author RayChou
 * @date 2025/8/22 14:51
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "达人详情业务视图对象")
public class VideoInfluencerInfoDetailVo extends VideoInfluencerInfoEntity {

    /**
     * 昨日新增粉丝数
     */
    @Schema(description = "近期新增粉丝数", example = "100")
    private Long yesterdayFansCount;

    /**
     * 3日更新作品数
     */
    @Schema(description = "3日更新作品数", example = "200")
    private Integer threeDaysVideoCount;
}
