package com.jiuyu.replay.words.bo.file;

import com.jiuyu.replay.generic.dto.words.BasicSettingsBaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class UploadFileInfoBo extends BasicSettingsBaseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @Schema(description ="ID")
    private Long id;
    /**
     * 文件名称
     */
    @Schema(description ="文件名称")
    private String fileName;
    /**
     * 文件类型 0：视频 1：音频 2：文本
     */
    @Schema(description ="文件类型 0：视频 1：音频 2：文本")
    private Integer fileType;
    /**
     * 文件源路径
     */
    @Schema(description ="文件源路径")
    private String  originalPath;
    /**
     * 文件新路径
     */
    @Schema(description ="文件新路径")
    private String  nowPath;
    /**
     * 文件大小，单位b
     */
    @Schema(description ="文件大小，单位b")
    private Long  fileSize;
    /**
     * 时长，单位：秒
     */
    @Schema(description = "时长，单位：秒")
    private String fileDuration;
    /**
     * 错误原因
     */
    @Schema(description ="错误原因")
    private String errorReason;
    /**
     * 行业Id
     */
    @Schema(description ="行业Id")
    private Long  tradeId;
    /**
     * 平台类型
     */
    @Schema(description ="平台类型")
    private String  platformType;
    /**
     * 分析状态 分析状态  0：未分析 1：分析中 2：分析完成 3分析错误
     */
    @Schema(description ="分析状态 分析状态  0：未分析 1：分析中 2：分析完成 3分析错误")
    private Integer analysisStatus;
    /**
     * 分析时间
     */
    @Schema(description ="分析时间")
    private String analysisTime;
    /**
     * 更新时间
     */
    @Schema(description ="更新时间")
    private String uploadTime;
    /**
     * 上传用户
     */
    @Schema(description ="上传用户")
    private Long userId;
    /**
     * 文件Id
     */
    @Schema(description ="文件Id")
    private String fileId;
    /**
     * 文件字数，只有文本文件有
     */
    @Schema(description ="文件字数，只有文本文件有")
    private Integer fileWordNum;
    /**
     * 文件上传状态 0：未上传 1：已上传
     */
    @Schema(description ="文件上传状态 0：未上传 1：已上传")
    private Integer uploadStatus;
    /**
     * 是否已标注敏感词 0：未标注 1：已标注
     */
    @Schema(description ="是否已标注敏感词 0：未标注 1：已标注")
    private Integer isMark;
    /**
     * 占用云空间的大小，单位：M
     */
    @Schema(description ="占用云空间的大小，单位：M")
    private Integer cloudStore;
    /**
     * 在线播放地址，只有视频和音频有
     */
    @Schema(description ="在线播放地址，只有视频和音频有")
    private String  playUrl;
    /**
     * 在线复盘的url
     */
    @Schema(description ="在线复盘的url")
    private String shareUrl;
    /**
     * 租户id
     */
    @Schema(description ="租户id")
    private Long tenantId;
    /**
     * 文件切片类型 0：原文件 1：复盘切片视频 2：短视频切片视频
     */
    @Schema(description ="文件切片类型 0：原文件 1：复盘切片视频 2：短视频切片视频")
    private Integer fileSliceType;
    /**
     * 重命名
     */
    @Schema(description ="重命名")
    private String videoRename;
    /**
     * 一句话识别引擎模型，如：16k_zh
     */
    @Schema(description = "一句话识别引擎模型，如：16k_zh")
    private String engSerViceType;
}
