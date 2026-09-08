package com.jiuyu.replay.words.vo.video;


import com.jiuyu.replay.generic.vo.words.AnchorVideoDetailInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.generic.vo.words.VideoContentVo;
import com.jiuyu.replay.words.vo.file.UploadFileDetailInfoVo;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author lyw
 */
@Schema(description = "视频或文件内容出参")
@Data
public class AnchorVideoFileAllVo implements Serializable {
    private static final long serialVersionUID = 1L;


    @Schema(description = "来源id")
    private String sourceId;

    @Schema(description = "来源类型0视频、1文件、2对比分析")
    private Integer sourceType;

    @Schema(description = "类型，1是自然原文，2是优化原文")
    private Integer type;

    @Schema(description = "优化原文生成状态 0待生成，1生成中，2生成成功，3生成失败")
    private Integer contentStatus;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "租户id")
    private Long tenantId;

    /**
     * 视频信息
     */
    @Schema(description = "视频信息")
    private AnchorVideoInfoVo anchorVideo;

    @Schema(description = "视频详情信息")
    private AnchorVideoDetailInfoVo anchorVideoDetail;

    /**
     * 文件信息
     */
    @Schema(description = "文件信息")
    private UploadFileInfoVo uploadFile;

    @Schema(description = "文件详情信息")
    private UploadFileDetailInfoVo uploadFileDetail;

    /**
     * 视频/文件内容
     */
    @Schema(description = "视频或文件内容")
    private List<VideoContentVo> videoFileContentList;
}
