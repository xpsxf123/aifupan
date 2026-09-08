package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/3/18 下午6:07
 */
@Data
public class DataScreenshotUploadBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 数据截图code
     */
    @Schema(description = "数据截图code")
    private String screenshotCode;
    /**
     * 上传的图片地址
     */
    @Schema(description = "上传的图片地址")
    private String sourceImagesAddress;
    /**
     * 数据类型 0视频，1文件，2对比分析
     */
    @Schema(description = "数据类型 0视频，1文件，2对比分析")
    private String sourceType;
    /**
     * 来源id
     */
    @Schema(description = "来源id")
    private String sourceId;
    /**
     * 图片来源 0：oss
     */
    @Schema(description = "图片来源 0：oss")
    private Integer sourceImagesType;


}
