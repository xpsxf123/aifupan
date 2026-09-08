package com.jiuyu.replay.video.project.bo.influencer;

import com.jiuyu.replay.video.project.entity.VideoInfluencerInfoEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author RayChou
 * @date 2025/8/22 10:01
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class VideoInfluencerSubscribeBo extends VideoInfluencerInfoEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "行业ID")
    @NotNull(message = "行业ID不能为空", groups = VideoInfluencerSubscribeBo.Add.class)
    private Long industryId;

    @Schema(description = "分组ID")
    private Long groupId;


    public interface Add extends VideoInfluencerInfoBo.Add {

    }
}
