package com.jiuyu.replay.words.vo;

import com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVo;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class OnlineAnalysisInfoVo {

    /**
     * 主播信息
     */
    @Schema(description = "主播信息")
    private AnchorUrlInfoVo anchorInfo;
    /**
     * 视频信息
     */
    @Schema(description = "视频信息")
    private AnchorVideoInfoVo videoInfo;
    /**
     * 文件信息
     */
    @Schema(description = "文件信息")
    private UploadFileInfoVo uploadFile;
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
    /**
     * 在线人数列表
     */
    @Schema(description = "在线人数列表")
    private List<OnlineNumInfoVo> onlineNumList;
}
