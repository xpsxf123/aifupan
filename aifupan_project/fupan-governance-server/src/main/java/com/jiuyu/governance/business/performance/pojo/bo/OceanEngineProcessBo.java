package com.jiuyu.governance.business.performance.pojo.bo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 巨量引擎文件里的实时格式
 *
 * @author liaoxin
 * @date 2025-06-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OceanEngineProcessBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 采集时间-格式为 yyyy-MM-dd HH:mm:ss
     */
    @NotNull(message = "采集时间不能为空")
    private LocalDateTime gatherDateTime;

    /**
     * 曝光次数
     */
    private Integer exposureCount;

    /**
     * 观看人数
     */
    private Integer viewCount;

    /**
     * 在线人数
     */
    private Integer onlineCount;

    /**
     * 涨粉人数
     */
    private Integer followCount;

    /**
     * 销售额（单位：元）
     */
    private BigDecimal salesRevenue;

    /**
     * 退款金额（单位：元）
     */
    private BigDecimal refund;

    /**
     * 投放金额（单位：元）
     */
    private BigDecimal investment;

    /**
     * 退款单量
     */
    private Integer refundQuantity;

    /**
     * 成交单量
     */
    private Integer payComboCnt;

    /**
     * 点击-成交率
     */
    private BigDecimal clickPaymentRate;

    /**
     * 互动率
     */
    private BigDecimal interactionRate;

    /**
     * 新增粉丝团人数
     */
    private Integer fansClubJoinUcnt;
}