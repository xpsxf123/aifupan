package com.jiuyu.replay.generic.enums.words;

import lombok.Getter;

/**
 * AI 监控资源类型
 */
@Getter
public enum MonitorSourceTypeEnum {

    /**
     * 录制视频，sourceId=tb_anchor_video.video_id
     */
    RECORD_VIDEO(0, "录制视频"),
    /**
     * 上传文件分析，sourceId=tb_upload_file.file_id
     */
    UPLOAD_FILE(1, "上传文件分析");

    private final Integer code;
    private final String remarks;

    MonitorSourceTypeEnum(Integer code, String remarks) {
        this.code = code;
        this.remarks = remarks;
    }
}
