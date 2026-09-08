package com.jiuyu.replay.video.project.bo.hotsearch;

import com.jiuyu.replay.common.validated.EnumValue;
import com.jiuyu.replay.video.project.bo.influencer.VideoInfluencerInfoBo;
import com.jiuyu.replay.video.project.vo.hotsearch.VideoHotSearchSyncVideoVo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @author RayChou
 * @date 2025/9/2 10:25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "搜爆款数据同步请求对象")
public class VideoHotSearchSyncDataBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public interface Add extends VideoInfluencerInfoBo.Add {

    }

    /**
     * 平台类型: 1-抖音, 2-快手, 3-视频号
     */
    @Schema(description = "平台类型: 1-抖音, 2-快手, 3-视频号", example = "1")
    @NotNull(message = "平台类型不能为空", groups = VideoHotSearchSyncDataBo.Add.class)
    @EnumValue(byteValues = {1, 2, 3}, message = "平台类型不合法", groups = VideoHotSearchSyncDataBo.Add.class)
    private Byte platformType;

    /**
     * 搜索关键词
     */
    @Schema(description = "搜索关键词", example = "比熊")
    @Length(max = 100, message = "搜索关键词长度不能超过100个字符", groups = VideoHotSearchSyncDataBo.Add.class)
    @NotNull(message = "搜索关键词不能为空", groups = VideoHotSearchSyncDataBo.Add.class)
    private String keyword;

    /**
     * 抓取数据类型：1：服务器 2：第三方
     */
    private Byte snatchDataType;

    /**
     * 视频信息列表
     */
    @Schema(description = "视频信息列表")
    @Valid
    private List<VideoHotSearchSyncVideoVo> videoList;
}
