package com.jiuyu.replay.generic.vo.words.video;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：弹幕导出前的详情
 * @date ：2025/11/11 16:45
 */
@Data
@Schema(description = "弹幕导出前的详情")
public class VideoDanMuExportDetailVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "导出视频名称")
    private String fileName;

    @Schema(description = "文件大小(kb)")
    private Long fileSize;

    @Schema(description = "总条数")
    private Long totalCount = 0L;
}
