package com.jiuyu.replay.generic.vo.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "文件信息")
public class FileShowVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文件id
     */
    @Schema(description = "文件id")
    private Long id;
    /**
     * 文件名字
     */
    @Schema(description = "文件名字")
    private String name;
    /**
     * 来源id
     */
    @Schema(description = "来源id")
    private Long resourceId;
    /**
     * 文件显示/下载地址
     */
    @Schema(description = "文件显示/下载地址")
    private String url;
    /**
     * 排序
     */
    @Schema(description = "排序")
    private Integer sort;
}
