package com.jiuyu.replay.video.project.bo.influencer;

import com.jiuyu.replay.video.project.entity.VideoInfluencerInfoEntity;
import com.jiuyu.replay.video.project.entity.VideoInfoEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @author RayChou
 * @date 2025/8/19 15:18
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "同步达人数据业务对象")
public class VideoInfluencerInfoBo extends VideoInfluencerInfoEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Valid
    private List<VideoInfoEntity> videoInfoEntityList;

    public interface Add {

    }
}
