package com.jiuyu.replay.common.constant;

import lombok.Data;

@Data
public class AudioSecretProperties {

    /**
     * 语音识别secretId
     */
    private String secretId;
    /**
     * 语音识别secretKey
     */
    private String secretKey;
    /**
     * 语音识别qps数量
     */
    private Integer qpsNum;
}
