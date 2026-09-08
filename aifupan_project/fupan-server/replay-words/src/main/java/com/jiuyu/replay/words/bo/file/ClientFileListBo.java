package com.jiuyu.replay.words.bo.file;

import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "客户端获取文件列表查询参数")
public class ClientFileListBo extends PageBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 行业id
     */
    @Schema(description = "行业id")
    private Long tradeId;
    /**
     * 上传云空间状态 0：未上传 1：已上传
     */
    @Schema(description = "上传云空间状态 0：未上传 1：已上传")
    private Integer uploadStatus;
    /**
     * 分析状态 0：未分析 1：分析中 2：分析完成 3分析错误
     */
    @Schema(description = "分析状态 0：未分析 1：分析中 2：分析完成 3分析错误")
    private Integer analysisStatus;
    /**
     * 文件名
     */
    @Schema(description = "文件名")
    private String fileName;
    /**
     * 上传时间范围-开始 yyyy-MM-dd
     */
    @Schema(description = "录制时间范围-开始 yyyy-MM-dd")
    private String uploadStartDate;
    /**
     * 上传时间范围-结束 yyyy-MM-dd
     */
    @Schema(description = "录制时间范围-结束 yyyy-MM-dd")
    private String uploadEndDate;
    /**
     * 分析时间范围-开始 yyyy-MM-dd
     */
    @Schema(description = "分析时间范围-开始 yyyy-MM-dd")
    private String analysisStartDate;
    /**
     * 分析时间范围-结束 yyyy-MM-dd
     */
    @Schema(description = "分析时间范围-结束 yyyy-MM-dd")
    private String analysisEndDate;
    /**
     * 用户id
     */
    @Schema(description = "用户id")
    private Long userId;
    /**
     * 租户id
     */
    @Schema(description = "租户id")
    private Long tenantId;
    /**
     * 类型  0：视频 1：音频 2：文本
     */
    @Schema(description = "类型  0：视频 1：音频 2：文本")
    private Integer fileType;
    /**
     * 类型数组  0：视频 1：音频 2：文本
     */
    @Schema(description = "类型数组  0：视频 1：音频 2：文本")
    private List<Integer> fileTypeArr;
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
