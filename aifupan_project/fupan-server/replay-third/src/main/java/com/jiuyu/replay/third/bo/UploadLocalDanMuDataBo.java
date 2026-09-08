package com.jiuyu.replay.third.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/1/14 下午2:58
 */
@Data
public class UploadLocalDanMuDataBo extends UploadDanMuDataBo{

    @Schema(description = "租户id")
    private Long tenantId;

    @Schema(description = "录制用户id")
    private Long userId;

    @Schema(description = "行业Id")
    private Long tradeId;

    @Schema(description = "行业名称")
    private String tradeName;

    @Schema(description = "重要弹幕的问题")
    private String problem;

    @Schema(description = "ai模型编码")
    private String aiModelCode;
}
