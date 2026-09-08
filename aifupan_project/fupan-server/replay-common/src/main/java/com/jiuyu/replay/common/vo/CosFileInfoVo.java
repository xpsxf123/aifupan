package com.jiuyu.replay.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class CosFileInfoVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "文件访问地址")
    private String url;

    @Schema(description = "COS key")
    private String key;

    @Schema(description = "文件MD5")
    private String md5;

    @Schema(description = "文件大小(字节)")
    private Long size;

    @Schema(description = "zip包内的相对路径")
    private String zipPath;
}
