package com.jiuyu.replay.generic.dto.third;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 第三方榜单采集响应结果
 *
 * @author HeHui
 * @date 2026-02-02 14:12
 */
@Getter
@Setter
public class ThirdSalesRankingResult implements Serializable {
    @Serial
    private static final long serialVersionUID = -7636857771613860695L;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 第三方榜单Id
     */
    private String categoryId;

    /**
     * 第三方榜单主播信息
     */
    private List<ThirdAnchorBo> salesRankingVos;
}
