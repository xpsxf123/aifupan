package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-03-19
 */
@Data
@TableName("tb_product_details")
public class ProductDetailsEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 雪花算法主键ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    /**
     * 归属批次号
     */
    private String batchNumber;
    /**
     * 视频唯一标识
     */
    private String videoId;
    /**
     * 商品ID
     */
    private String productId;
    /**
     * 商品标题
     */
    private String title;
    /**
     * 商品图片URL
     */
    private String imageUri;
    /**
     * 到手价（元）
     */
    private BigDecimal marketPrice;
    /**
     * 直播间上架时间
     */
    private Date productBindTime;
    /**
     * 直播间下架时间
     */
    private Date productDownTime;
    /**
     * 讲解次数
     */
    private Integer explainCnt;
    /**
     * 商品曝光人数
     */
    private Long productShowUcnt;
    /**
     * 商品点击人数
     */
    private Long productClickUcnt;
    /**
     * 曝光-点击转化率
     */
    private BigDecimal productShowClickUcntRatio;
    /**
     * 曝光-成交转化率
     */
    private BigDecimal productShowPayUcntRatio;
    /**
     * 点击-成交转化率
     */
    private BigDecimal productClickPayUcntRatio;
    /**
     * 商品千次曝光成交金额（元）
     */
    private BigDecimal gpm;
    /**
     * 累计成交金额（元）
     */
    private BigDecimal payAmt;
    /**
     * 分钟最高成交金额（元）
     */
    private BigDecimal avgMaxPayAmtMin;
    /**
     * 累计成交件数
     */
    private Long payComboCnt;
    /**
     * 累计成交订单数
     */
    private Long payCnt;
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
    private Long refundCnt;
    /**
     * 退款金额（元）
     */
    private BigDecimal realRefundAmt;
    /**
     * 退款率
     */
    private BigDecimal refundRate;
    /**
     * 统计曲线
     */
    private String statisticsCurve;
    /**
     * 创建时间
     */
    private Date createDate;
    /**
     * 更新时间
     */
    private Date updateDate;
}
