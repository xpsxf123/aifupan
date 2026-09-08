package com.jiuyu.governance.business.performance.pojo.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 开放接口 - 批次商品存在性查询响应
 * <p>
 * 扁平返回：入参中每一对「平台 + 批次号」对应一行，顺序与入参一致。
 * </p>
 *
 * @author HeHui
 * @date 2026-08-06
 */
@Getter
@Setter
public class OpenBatchProductExistsResponse {

    /**
     * 平台类型：0-抖音，1-快手，2-视频号
     */
    private Integer platform;

    /**
     * 直播批次号
     */
    private String batchNumber;

    /**
     * 该批次下是否存在商品数据
     */
    private Boolean exists;

    public OpenBatchProductExistsResponse() {
    }

    public OpenBatchProductExistsResponse(Integer platform, String batchNumber, Boolean exists) {
        this.platform = platform;
        this.batchNumber = batchNumber;
        this.exists = exists;
    }
}
