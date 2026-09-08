package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "上传视频后回调的VO")
public class FileUploadCompleteVO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 返回的状态
     */
    private String Status;
    /**
     * 事件产生时间
     */
    private String EventTime;
    /**
     * 事件类型
     */
    private String EventType;
    /**
     * 视频ID。
      */
    private String VideoId;
    /**
     * 视频时长
     */
    private Long  Size;
    /**
     * 上传文件的URL地址
     */
    private String FileUrl;
}
