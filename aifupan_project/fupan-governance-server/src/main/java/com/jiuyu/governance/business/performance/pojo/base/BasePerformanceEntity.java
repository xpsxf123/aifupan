package com.jiuyu.governance.business.performance.pojo.base;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.jiuyu.governance.business.performance.pojo.bo.OceanEngineProcessBo;
import com.jiuyu.governance.business.performance.utils.MetricsUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * 业绩核心字段基类
 * <p>
 * 统一管理业绩实体的10个核心指标字段，消除跨表字段冗余：
 * 场观、销售额、退款、投放、净销售额、ROI、投放产出
 * </p>
 * <p>
 * 适用实体：live_session、schedule_performance、schedule_performance_detail、
 * session_performance、session_original_value
 * </p>
 * <p>
 * 新增业绩字段时只需在此基类添加一处，所有子实体自动继承。
 * MyBatis-Plus 会自动识别父类中的 {@link TableField} 注解，无需修改 Mapper XML。
 * </p>
 *
 * @author lj
 * @date 2026-04-02
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class BasePerformanceEntity {

    /**
     * 总观看人次
     */
    @TableField("view_count")
    private Integer viewCount;

    /**
     * 销售额（累计，单位：元）
     */
    @TableField("sales_revenue")
    private BigDecimal salesRevenue;

    /**
     * 退款（累计，单位：元）
     */
    @TableField("refund")
    private BigDecimal refund;

    /**
     * 投放（累计，单位：元）
     */
    @TableField("investment")
    private BigDecimal investment;

    /**
     * 退款数量（累计）
     */
    @TableField("refund_quantity")
    private Integer refundQuantity;

    /**
     * 销售单量
     */
    @TableField("pay_combo_cnt")
    private Integer payComboCnt;

    /**
     * 净销售额（累计，单位：元）
     * <p>
     * 计算公式：净销售额 = 销售额 - 退款金额
     * </p>
     */
    @TableField("net_sales")
    private BigDecimal netSales;

    /**
     * 退款率（精确到小数点后两位）
     * <p>
     * 计算公式：退款率 = 退款单量 / 成交单量
     * </p>
     */
    @TableField("refund_rate")
    private BigDecimal refundRate;

    /**
     * 投资回报率（ROI，精确到小数点后两位）
     * <p>
     * 计算公式：ROI = 销售额 / 投放金额
     * </p>
     */
    @TableField("roi")
    private BigDecimal roi;

    /**
     * 千次成交（精确到小数点后两位）
     * <p>
     * 计算公式：千次成交 = 销售额 / 观看人数 * 1000
     * </p>
     */
    @TableField("thousand_sales")
    private BigDecimal thousandSales;

    /**
     * 曝光次数
     */
    @TableField("exposure_count")
    private Integer exposureCount;

    /**
     * 涨粉人数
     */
    @TableField("follow_count")
    private Integer followCount;

    /**
     * 点击-成交率（精确到小数点后两位）
     * <p>
     * 区间计算公式：点击-成交率 = 区间订单量 / 区间点击量
     * <br>
     * 其中：区间点击量 = 结束单量 / 结束点击成交率 - 开始单量 / 开始点击成交率
     * </p>
     */
    @TableField("click_payment_rate")
    private BigDecimal clickPaymentRate;

    /**
     * 互动率（精确到小数点后两位）
     * <p>
     * 区间计算公式：互动率 = 区间互动次数 / 区间观看人数
     * <br>
     * 其中：区间互动次数 = 结束观看人数 * 结束互动率 - 开始观看人数 * 开始互动率
     * </p>
     */
    @TableField("interaction_rate")
    private BigDecimal interactionRate;

    /**
     * 最高在线
     */
    @TableField("max_online")
    private Integer maxOnline;

    /**
     * 带货转化率（精确到小数点后两位）
     * <p>
     * 计算公式：带货转化率 = 成交单量 / 观看人数 * 100%
     * </p>
     */
    @TableField("conversion_rate")
    private BigDecimal conversionRate;

    /**
     * UV价值（单位：元）
     * <p>
     * 计算公式：UV价值 = 销售额 / 观看人数
     * </p>
     */
    @TableField("uv_value")
    private BigDecimal uvValue;

    /**
     * 涨粉率（精确到小数点后两位）
     * <p>
     * 计算公式：涨粉率 = 涨粉人数 / 观看人数 * 100%
     * </p>
     */
    @TableField("follow_rate")
    private BigDecimal followRate;

    /**
     * 设置业绩指标
     * @param entity 业绩指标实体
     * @param <T> 业绩指标实体类型
     */
    public <T extends BasePerformanceEntity> void setPerformance(T entity) {
        if (entity != null) {
            this.viewCount = entity.getViewCount();
            this.salesRevenue = entity.getSalesRevenue();
            this.refund = entity.getRefund();
            this.investment = entity.getInvestment();
            this.refundQuantity = entity.getRefundQuantity();
            this.payComboCnt = entity.getPayComboCnt();
            this.netSales = entity.getNetSales();
            this.refundRate = entity.getRefundRate();
            this.roi = entity.getRoi();
            this.thousandSales = entity.getThousandSales();
            this.exposureCount = entity.getExposureCount();
            this.followCount = entity.getFollowCount();
            this.clickPaymentRate = entity.getClickPaymentRate();
            this.interactionRate = entity.getInteractionRate();
            this.maxOnline = entity.getMaxOnline();
            this.conversionRate = entity.getConversionRate();
            this.uvValue = entity.getUvValue();
            this.followRate = entity.getFollowRate();
        }
    }

    /**
     * 设置过程业绩指标
     * @param entity 过程业绩指标
     */
    public void setPerformanceOceanEngine(OceanEngineProcessBo entity) {
        if (entity == null){
            return;
        }
        this.exposureCount = entity.getExposureCount();
        this.viewCount = entity.getViewCount();
        this.followCount = entity.getFollowCount();
        this.salesRevenue = entity.getSalesRevenue();
        this.refund = entity.getRefund();
        this.investment = entity.getInvestment();
        this.refundQuantity = entity.getRefundQuantity();
        this.payComboCnt = entity.getPayComboCnt();
        this.clickPaymentRate = entity.getClickPaymentRate();
        this.interactionRate = entity.getInteractionRate();
    }

    /**
     * 设置业绩指标（计算）
     * @param entity 业绩指标实体
     * @param <T> 业绩指标实体类型
     */
    public <T extends BasePerformanceEntity> void setPerformanceCalculate(T entity) {
        if (entity == null){
            return;
        }
        setPerformance(entity);
        calculateDerivedMetrics();
    }

    /**
     * 计算衍生指标（基于当前基础指标值）
     */
    public void calculateDerivedMetrics() {
        // 计算净销售额
        this.netSales = MetricsUtil.subtract(this.salesRevenue, this.refund);
        // 计算退款率
        this.refundRate = MetricsUtil.toPercentValue(MetricsUtil.divide(this.refundQuantity, this.payComboCnt), 2);
        // 计算ROI
        this.roi = MetricsUtil.divide(this.salesRevenue, this.investment);
        // 计算千次成交
        this.thousandSales = MetricsUtil.multiply(MetricsUtil.divide(this.salesRevenue, this.viewCount), BigDecimal.valueOf(1000));
        // 计算带货转化率
        this.conversionRate = MetricsUtil.toPercentValue(MetricsUtil.divide(this.payComboCnt, this.viewCount), 2);
        // 计算UV价值
        this.uvValue = MetricsUtil.divide(this.salesRevenue, this.viewCount != null ? BigDecimal.valueOf(this.viewCount) : null, 2);
        // 计算涨粉率
        this.followRate = MetricsUtil.toPercentValue(MetricsUtil.divide(this.followCount, this.viewCount), 2);
    }

    /**
     * 设置区间点击-成交率
     * @param payComboCnt 区间成交单量
     * @param startClickPaymentRate 开始点击-成交率
     * @param startPayComboCnt 开始成交单量
     * @param endClickPaymentRate 结束点击-成交率
     * @param endPayComboCnt 结束成交单量
     */
    public void setPerformanceClickPaymentRate(Integer payComboCnt, BigDecimal startClickPaymentRate, Integer startPayComboCnt, BigDecimal endClickPaymentRate, Integer endPayComboCnt) {
        // 计算点击量
        BigDecimal endClickCount = MetricsUtil.divide(BigDecimal.valueOf(ObjUtil.defaultIfNull(endPayComboCnt, 0)), endClickPaymentRate, 0);
        BigDecimal startClickCount = MetricsUtil.divide(BigDecimal.valueOf(ObjUtil.defaultIfNull(startPayComboCnt, 0)), startClickPaymentRate, 0);
        BigDecimal clickCount = MetricsUtil.subtract(endClickCount, startClickCount);
        this.clickPaymentRate = MetricsUtil.divide(payComboCnt, clickCount);
    }

    /**
     * 设置区间互动率
     * @param startInteractionRate 开始互动率
     * @param startViewCount 开始观看人数
     * @param endInteractionRate 结束互动率
     * @param endViewCount 结束观看人数
     * @param viewCount 观看人数
     */
    public void setPerformanceInteractionRate(BigDecimal startInteractionRate, Integer startViewCount, BigDecimal endInteractionRate, Integer endViewCount, Integer viewCount) {
        // 计算互动次数
        BigDecimal endInteractionCount = MetricsUtil.multiply(BigDecimal.valueOf(ObjUtil.defaultIfNull(endViewCount, 0)), endInteractionRate);
        BigDecimal startInteractionCount = MetricsUtil.multiply(BigDecimal.valueOf(ObjUtil.defaultIfNull(startViewCount, 0)), startInteractionRate);
        BigDecimal interactionCountTotal = MetricsUtil.subtract(endInteractionCount, startInteractionCount);
        this.interactionRate = MetricsUtil.divide(interactionCountTotal, BigDecimal.valueOf(ObjUtil.defaultIfNull(viewCount, 0)), 2);
    }
}
