package com.jiuyu.replay.words.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author tisheng
 * @date 2024/9/5
 * @apinNote
 */
@Data
@Schema(description = "111")
public class UploadFileBo extends PageBo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;
    @Schema(description = "文件名称")
    private String fileName;
    @Schema(description = "文件类型")
    private Integer fileType;
    @Schema(description = "文件路径")
    private String  originalPath;
    @Schema(description = "新的文件路径")
    private String  nowPath;
    @Schema(description = "文件大小")
    private Long  fileSize;
    @Schema(description = "不懂")
    private String fileDuration;
    @Schema(description = "错误原因")
    private String errorReason;
    @Schema(description = "行业Id")
    private Long  tradeId;
    @Schema(description = "不懂")
    private String  platformType;
    @Schema(description = "状态")
    private Integer analysisStatus;
    @Schema(description = "不懂")
    private String analysisTime;
    @Schema(description = "更新时间")
    private String uploadTime;
    @Schema(description = "上传人用户Id")
    private Long userId;
    @Schema(description = "文件ID")
    private String fileId;
    @Schema(description ="文件上传状态 0：未上传 1：已上传")
    private Integer uploadStatus;
    /**
     * 文件字数，只有文本文件有
     */
    @Schema(description ="文件字数，只有文本文件有")
    private Integer fileWordNum;
    /**
     * 租户id
     */
    @Schema(description ="租户id")
    private Long tenantId;
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
     * 在线复盘的url
     */
    @Schema(description ="在线复盘的url")
    private String shareUrl;
    /**
     * 在线播放地址
     */
    @Schema(description ="在线播放地址")
    private String playUrl;
    /**
     * 文件切片类型 0：原文件 1：复盘切片视频 2：短视频切片视频
     */
    @Schema(description ="文件切片类型 0：原文件 1：复盘切片视频 2：短视频切片视频")
    private Integer fileSliceType;
    /**
     * 用户名称
     */
    @Schema(description = "用户名称")
    private String userName;
    /**
     * 用户ids
     */
    @Schema(description = "用户ids")
    private List<Long> userIds;

    @Schema(description = "结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    //      private String endTime;
//
    @Schema(description = "开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /**
     * 销售人员ID
     */
    @Schema(description = "销售人员ID")
    private Long userSalesID;
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
