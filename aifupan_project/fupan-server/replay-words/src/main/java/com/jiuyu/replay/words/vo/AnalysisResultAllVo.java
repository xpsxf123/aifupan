package com.jiuyu.replay.words.vo;


import com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author lyw
 */
@Data
@Schema(description = "分析结果-视频或文件")
public class AnalysisResultAllVo {


    /**
     * 类型
     */
    @Schema(description = "类型1自然、2优化原文")
    private Integer type;
    /**
     * 来源类型
     */
    @Schema(description = "来源类型0视频、1文件、2对比分析")
    private Integer resourceType;
    /**
     * 视频信息
     */
    @Schema(description = "视频信息")
    private AnchorVideoInfoVo videoInfo;
    /**
     * 文件信息
     */
    @Schema(description = "文件信息")
    private UploadFileInfoVo uploadFileInfoVo;
    /**
     * 视频/音频播放地址
     */
    @Schema(description = "视频/音频播放地址")
    private String playUrl;
    /**
     * 段落词语信息列表
     */
    @Schema(description = "段落词语信息列表")
    private List<OnlineAnalysisItemVo> analysisList;

}
