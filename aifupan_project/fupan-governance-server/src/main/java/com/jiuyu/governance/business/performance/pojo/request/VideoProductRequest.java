package com.jiuyu.governance.business.performance.pojo.request;

import com.baomidou.mybatisplus.annotation.TableField;
import com.jiuyu.governance.business.performance.pojo.bo.CommodityProcessDataBo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频商品请求
 *
 * @author lj
 * @date 2026-03-19
 */
@Getter
@Setter
public class VideoProductRequest {

    /**
     * 直播批次号
     */
    private String batchNumber;

    /**
     * 商品ID（抖音的商品ID）
     */
    @NotBlank(message = "商品productId不能为空")
    private String productId;

    /**
     * 商品标题
     */
    @NotBlank(message = "商品标题不能为空")
    @Size(max = 200,message = "商品标题最大长度要小于 200")
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
     * 直播间上架时间 格式：yyyy-MM-dd HH:mm:ss
     */
    private LocalDateTime productBindTime;

    /**
     * 直播间下架时间 格式：yyyy-MM-dd HH:mm:ss
     */
    private LocalDateTime productDownTime;

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
    private Integer refundCnt;

    /**
     * 退款金额（元）
     */
    private BigDecimal realRefundAmt;

    /**
     * 退款率
     */
    private BigDecimal refundRate;

    /**
     * 曝光观看率 — 商品曝光人数/直播间观看人数
     */
    private BigDecimal productViewShowRatio;

    /**
     * 成交单价（元）
     */
    private BigDecimal avgPayAmtPerOrder;

    /**
     * 排序，从1开始
     */
    private Integer sort;

    /**
     * 商品的过程数据
     */
    @Valid
    private List<CommodityProcessDataBo> commodityProcessDataList;
}
