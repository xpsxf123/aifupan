package com.jiuyu.governance.business.performance.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品关联直播场次响应
 *
 * @author lj
 * @date 2026-03-27
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSessionResponse {

    /**
     * 场次ID
     */
    private Long sessionId;

    /**
     * 主播头像URL
     */
    private String anchorAvatar;

    /**
     * 主播名称
     */
    private String anchorName;

    /**
     * 开播时间
     */
    private LocalDateTime startTime;

    /**
     * 场次时长（秒）
     */
    private Integer duration;

    /**
     * 销量
     */
    private Integer quantity;

    /**
     * 销售额（单位：元）
     */
    private BigDecimal salesAmount;

    /**
     * 退款金额（单位：元）
     */
    private BigDecimal refundAmount;

    /**
     * 场观
     */
    private Integer viewCount;
}
