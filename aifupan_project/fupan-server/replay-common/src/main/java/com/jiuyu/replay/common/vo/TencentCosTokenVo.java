package com.jiuyu.replay.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/1/3 下午8:21
 */
@Data
@Schema(description = "cos临时调用凭证")
public class TencentCosTokenVo extends TencentTempTokenVo{

    /**
     * COS 存储区域
     */
    @Schema(description = "COS 存储区域")
    private String region;
    /**
     * COS 存储桶名称
     */
    @Schema(description = "COS 存储桶名称")
    private String bucketName;
}
