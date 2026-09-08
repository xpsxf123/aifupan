package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-03-19
 */
@Data
@Schema(description = "商品信息")
public class ProductDetailsVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @Schema(description = "ID")
    private Long id;
    /**
     * 归属批次号
     */
    @Schema(description = "归属批次号")
    private String batchNumber;
    /**
     * 视频唯一标识
     */
    @Schema(description = "视频唯一标识")
    private String videoId;
    /**
     * 商品ID
     */
    @Schema(description = "商品ID")
    private String productId;
    /**
     * 商品标题
     */
    @Schema(description = "商品标题")
    private String title;
    /**
     * 商品图片URL
     */
    @Schema(description = "商品图片URL")
    private String imageUri;
    /**
     * 到手价（元）
     */
    @Schema(description = "到手价（元）")
    private BigDecimal marketPrice;
    /**
     * 直播间上架时间
     */
    @Schema(description = "直播间上架时间")
    private Date productBindTime;
    /**
     * 直播间下架时间
     */
    @Schema(description = "直播间下架时间")
    private Date productDownTime;
    /**
     * 讲解次数
     */
    @Schema(description = "讲解次数")
    private Integer explainCnt;
    /**
     * 商品曝光人数
     */
    @Schema(description = "商品曝光人数")
    private Long productShowUcnt;
    /**
     * 商品点击人数
     */
    @Schema(description = "商品点击人数")
    private Long productClickUcnt;
    /**
     * 曝光-点击转化率
     */
    @Schema(description = "曝光-点击转化率")
    private BigDecimal productShowClickUcntRatio;
    /**
     * 曝光-成交转化率
     */
    @Schema(description = "曝光-成交转化率")
    private BigDecimal productShowPayUcntRatio;
    /**
     * 点击-成交转化率
     */
    @Schema(description = "点击-成交转化率")
    private BigDecimal productClickPayUcntRatio;
    /**
     * 商品千次曝光成交金额（元）
     */
    @Schema(description = "商品千次曝光成交金额（元）")
    private BigDecimal gpm;
    /**
     * 累计成交金额（元）
     */
    @Schema(description = "累计成交金额（元）")
    private BigDecimal payAmt;
    /**
     * 分钟最高成交金额（元）
     */
    @Schema(description = "分钟最高成交金额（元）")
    private BigDecimal avgMaxPayAmtMin;
    /**
     * 累计成交件数
     */
    @Schema(description = "累计成交件数")
    private Long payComboCnt;
    /**
     * 累计成交订单数
     */
    @Schema(description = "累计成交订单数")
    private Long payCnt;
    /**
     * 创建订单数
     */
    @Schema(description = "创建订单数")
    private Long createCnt;
    /**
     * 订单支付率
     */
    @Schema(description = "订单支付率")
    private BigDecimal createPayUcntRatio;
    /**
     * 预售订单数
     */
    @Schema(description = "预售订单数")
    private Long payDepositPreOrderCnt;
    /**
     * 预售定金金额（元）
     */
    @Schema(description = "预售定金金额（元）")
    private BigDecimal presaleDepayDeamt;
    /**
     * 预售全款金额（元）
     */
    @Schema(description = "预售全款金额（元）")
    private BigDecimal payDepositPreOrderAmt;
    /**
     * 退款订单数
     */
    @Schema(description = "退款订单数")
    private Long refundCnt;
    /**
     * 退款金额（元）
     */
    @Schema(description = "退款金额（元）")
    private BigDecimal realRefundAmt;
    /**
     * 退款率
     */
    @Schema(description = "退款率")
    private BigDecimal refundRate;
    /**
     * 统计曲线
     */
    @Schema(description = "统计曲线")
    private String statisticsCurve;
    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createDate;
    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private Date updateDate;
}
