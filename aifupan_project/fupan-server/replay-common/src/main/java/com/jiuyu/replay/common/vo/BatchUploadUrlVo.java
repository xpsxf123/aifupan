package com.jiuyu.replay.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "批量上传预签名链接")
public class BatchUploadUrlVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "完整的cosKey（前缀+文件key）")
    private String cosKey;

    @Schema(description = "客户端传入的原始key")
    private String key;

    @Schema(description = "预签名上传地址")
    private String signedUrl;
}
