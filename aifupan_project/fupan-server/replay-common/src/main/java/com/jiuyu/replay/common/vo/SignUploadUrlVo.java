package com.jiuyu.replay.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：上传oss的预签名链接
 * @date ：2025/3/18 下午3:30
 */
@Data
public class SignUploadUrlVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "上传地址")
    private String signedUrl;

    @Schema(description ="文件key")
    private String ossKey;

}
