package com.jiuyu.replay.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "语音识别接口临时调用凭证")
public class AlibabaTempTokenVo {

    /**
     * 临时token
     */
    @Schema(description = "临时token")
    private String token;
    /**
     * 临时SecretId
     */
    @Schema(description = "临时SecretId")
    private String tempSecretId;
    /**
     * 临时SecretKey
     */
    @Schema(description = "临时SecretKey")
    private String tempSecretKey;
}
