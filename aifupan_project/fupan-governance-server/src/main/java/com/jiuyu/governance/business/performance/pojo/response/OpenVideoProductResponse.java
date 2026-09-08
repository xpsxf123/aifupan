package com.jiuyu.governance.business.performance.pojo.response;

import com.jiuyu.governance.business.performance.pojo.bo.CommodityProcessDataBo;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 开放接口 - 视频商品查询响应（按视频ID聚合）
 * <p>
 * 每个视频ID一行，内含该视频所属直播批次下的商品指标集合。
 * {@code video_product.video_id} 已废弃，商品实际按 batchNumber 归属，故同时回显 batchNumber。
 * </p>
 *
 * @author HeHui
 * @date 2026-08-06
 */
@Getter
@Setter
public class OpenVideoProductResponse {

    /**
     * 视频ID
     */
    private String videoId;

    /**
     * 视频所属直播批次号
     */
    private String batchNumber;

    /**
     * 该批次下的商品指标集合
     */
    private List<VideoProductMetric> products;

    /**
     * 视频商品指标
     */
    @Getter
    @Setter
    public static class VideoProductMetric {

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
        private LocalDateTime productBindTime;

        /**
         * 直播间下架时间
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
         * 曝光观看率 — 商品曝光人数/直播间观看人数
         */
        private BigDecimal productViewShowRatio;

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
         * 成交单价（元）
         */
        private BigDecimal avgPayAmtPerOrder;

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
         * 商品过程数据（按请求步长降采样后的累计快照序列，时间升序）
         * <p>
         * 每条是该时间点的累计值（曝光人数、点击人数、成交金额等均为自商品上架起的累计量），
         * 不是区间增量；需要区间增量时由调用方自行做前后两条相减。
         * 商品无过程数据或下载失败时为空集合。
         * </p>
         */
        private List<CommodityProcessDataBo> processList;
    }
}
