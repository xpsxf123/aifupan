package com.jiuyu.replay.words.vo.video;

import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.generic.vo.words.VideoContentVo;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/6/24 下午5:27
 */
@Schema(description = "获取当前用户待生产的自然、优化原文出参")
@Data
public class ToGeneratedVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "来源id")
    private String sourceId;

    @Schema(description = "类型 0视频，1文件")
    private Integer sourceType;

    @Schema(description = "原文类型 0分钟，1自然原文，2优化原文")
    private Integer type;

    @Schema(description = "使用的模型")
    private Integer aiModel;

    @Schema(description = "状态 0待生成，1生成中，2生成成功，3生成失败")
    private Integer status;

    @Schema(description = "点击生成时的时间戳")
    private Long startDateTime;

    @Schema(description = "视频信息")
    private AnchorVideoInfoVo anchorVideo;

    @Schema(description = "文件信息")
    private UploadFileInfoVo uploadFile;

    @Schema(description = "提示词")
    private List<VideoContentVo> videoContentList;

}
