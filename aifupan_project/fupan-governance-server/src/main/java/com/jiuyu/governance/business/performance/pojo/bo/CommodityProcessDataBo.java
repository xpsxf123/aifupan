package com.jiuyu.governance.business.performance.pojo.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户端上的商品过程数据
 * @author ：lujie
 * @date ：2026/4/13 15:52
 */
@Data
public class CommodityProcessDataBo {

    /**
     * 时间 格式：yyyy-MM-dd HH:mm:ss
     */
    @NotNull(message = "时间不能为空")
    private LocalDateTime dateTime;

    /**
     * 商品曝光人数
     */
    private Long productShowUcnt;

    /**
     * 商品点击人数
     */
    private Long productClickUcnt;

    /**
     * 直播间累计观看人数
     */
    private Integer viewCount;

    /**
     * 讲解次数
     */
    private Integer explainCnt;

    /**
     * 成交件数
     */
    private Integer payComboCnt;

    /**
     * 成交金额 单位：元
     */
    private BigDecimal payAmt;

    /**
     * 商品千次曝光成交金额（元）
     */
    private BigDecimal gpm;

    /**
     * 分钟最高成交金额（元）
     */
    private BigDecimal avgMaxPayAmtMin;

    /**
     * 创建订单数
     */
    private Long createCnt;

    /**
     * 订单支付率
     */
    private BigDecimal createPayUcntRatio;

    /**
     * 预售订单数
     */
    private Long payDepositPreOrderCnt;

    /**
     * 预售定金金额（元）
     */
    private BigDecimal presaleDepayDeamt;

    /**
     * 预售全款金额（元）
     */
    private BigDecimal payDepositPreOrderAmt;

    /**
     * 退款订单数
     */
    private Integer refundCnt;

    /**
     * 退款金额（元）
     */
    private BigDecimal realRefundAmt;

}
