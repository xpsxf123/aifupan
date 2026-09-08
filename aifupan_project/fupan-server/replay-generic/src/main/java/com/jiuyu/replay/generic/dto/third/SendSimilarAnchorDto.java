package com.jiuyu.replay.generic.dto.third;

import lombok.Data;

import java.io.Serializable;

@Data
public class SendSimilarAnchorDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 	主播抖音号
     */
    private String anchorId;
    /**
     * 	主播secUid
     */
    private String secUid;
    /**
     * 	回调地址
     */
    private String callBackUrl;
    /**
     * 请求ID（用于请求和回调的唯一标识）
     */
    private String requestId;
}
