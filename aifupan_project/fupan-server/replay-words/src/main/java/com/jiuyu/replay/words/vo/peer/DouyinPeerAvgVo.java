package com.jiuyu.replay.words.vo.peer;

import com.jiuyu.replay.words.entity.DouyinPeerAvgEntity;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 同行平均值 VO — 读表查询结果
 *
 * @author jy
 * @date 2026-06-25
 */
public class DouyinPeerAvgVo {

    private Long tradeId;
    private Date statDate;
    private Integer videoCount;

    private BigDecimal avgTotalWatchNum;
    private BigDecimal avgAverageOnlineNum;
    private BigDecimal avgAverageResidenceTime;
    private BigDecimal avgIncrementFollowerCount;
    private BigDecimal avgConvertFanRate;
    private BigDecimal avgInteractionPercent;

    private BigDecimal avgVolumeStart;
    private BigDecimal avgVolumeEnd;
    private BigDecimal avgPurchaseCountStart;
    private BigDecimal avgPurchaseCountEnd;
    private BigDecimal avgCustomerUnitPriceStart;
    private BigDecimal avgCustomerUnitPriceEnd;
    private BigDecimal avgUvValueStart;
    private BigDecimal avgUvValueEnd;
    private BigDecimal avgGoodsConvertRateStart;
    private BigDecimal avgGoodsConvertRateEnd;
    private BigDecimal avgGpmStart;
    private BigDecimal avgGpmEnd;

    private BigDecimal avgShowWatchCntRatio;
    private BigDecimal avgRoi;
    private BigDecimal avgLaunchRoiAmount;
    private BigDecimal avgRefundAmount;
    private BigDecimal avgOverallCostRoi;
    private BigDecimal avgNetTransactionRoi;

    public static DouyinPeerAvgVo from(DouyinPeerAvgEntity entity) {
        DouyinPeerAvgVo vo = new DouyinPeerAvgVo();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    // ===== getters =====

    public Long getTradeId() {
        return tradeId;
    }

    public void setTradeId(Long tradeId) {
        this.tradeId = tradeId;
    }

    public Date getStatDate() {
        return statDate;
    }

    public void setStatDate(Date statDate) {
        this.statDate = statDate;
    }

    public Integer getVideoCount() {
        return videoCount;
    }

    public void setVideoCount(Integer videoCount) {
        this.videoCount = videoCount;
    }

    public BigDecimal getAvgTotalWatchNum() {
        return avgTotalWatchNum;
    }

    public void setAvgTotalWatchNum(BigDecimal avgTotalWatchNum) {
        this.avgTotalWatchNum = avgTotalWatchNum;
    }

    public BigDecimal getAvgAverageOnlineNum() {
        return avgAverageOnlineNum;
    }

    public void setAvgAverageOnlineNum(BigDecimal avgAverageOnlineNum) {
        this.avgAverageOnlineNum = avgAverageOnlineNum;
    }

    public BigDecimal getAvgAverageResidenceTime() {
        return avgAverageResidenceTime;
    }

    public void setAvgAverageResidenceTime(BigDecimal avgAverageResidenceTime) {
        this.avgAverageResidenceTime = avgAverageResidenceTime;
    }

    public BigDecimal getAvgIncrementFollowerCount() {
        return avgIncrementFollowerCount;
    }

    public void setAvgIncrementFollowerCount(BigDecimal avgIncrementFollowerCount) {
        this.avgIncrementFollowerCount = avgIncrementFollowerCount;
    }

    public BigDecimal getAvgConvertFanRate() {
        return avgConvertFanRate;
    }

    public void setAvgConvertFanRate(BigDecimal avgConvertFanRate) {
        this.avgConvertFanRate = avgConvertFanRate;
    }

    public BigDecimal getAvgInteractionPercent() {
        return avgInteractionPercent;
    }

    public void setAvgInteractionPercent(BigDecimal avgInteractionPercent) {
        this.avgInteractionPercent = avgInteractionPercent;
    }

    public BigDecimal getAvgVolumeStart() {
        return avgVolumeStart;
    }

    public void setAvgVolumeStart(BigDecimal avgVolumeStart) {
        this.avgVolumeStart = avgVolumeStart;
    }

    public BigDecimal getAvgVolumeEnd() {
        return avgVolumeEnd;
    }

    public void setAvgVolumeEnd(BigDecimal avgVolumeEnd) {
        this.avgVolumeEnd = avgVolumeEnd;
    }

    public BigDecimal getAvgPurchaseCountStart() {
        return avgPurchaseCountStart;
    }

    public void setAvgPurchaseCountStart(BigDecimal avgPurchaseCountStart) {
        this.avgPurchaseCountStart = avgPurchaseCountStart;
    }

    public BigDecimal getAvgPurchaseCountEnd() {
        return avgPurchaseCountEnd;
    }

    public void setAvgPurchaseCountEnd(BigDecimal avgPurchaseCountEnd) {
        this.avgPurchaseCountEnd = avgPurchaseCountEnd;
    }

    public BigDecimal getAvgCustomerUnitPriceStart() {
        return avgCustomerUnitPriceStart;
    }

    public void setAvgCustomerUnitPriceStart(BigDecimal avgCustomerUnitPriceStart) {
        this.avgCustomerUnitPriceStart = avgCustomerUnitPriceStart;
    }

    public BigDecimal getAvgCustomerUnitPriceEnd() {
        return avgCustomerUnitPriceEnd;
    }

    public void setAvgCustomerUnitPriceEnd(BigDecimal avgCustomerUnitPriceEnd) {
        this.avgCustomerUnitPriceEnd = avgCustomerUnitPriceEnd;
    }

    public BigDecimal getAvgUvValueStart() {
        return avgUvValueStart;
    }

    public void setAvgUvValueStart(BigDecimal avgUvValueStart) {
        this.avgUvValueStart = avgUvValueStart;
    }

    public BigDecimal getAvgUvValueEnd() {
        return avgUvValueEnd;
    }

    public void setAvgUvValueEnd(BigDecimal avgUvValueEnd) {
        this.avgUvValueEnd = avgUvValueEnd;
    }

    public BigDecimal getAvgGoodsConvertRateStart() {
        return avgGoodsConvertRateStart;
    }

    public void setAvgGoodsConvertRateStart(BigDecimal avgGoodsConvertRateStart) {
        this.avgGoodsConvertRateStart = avgGoodsConvertRateStart;
    }

    public BigDecimal getAvgGoodsConvertRateEnd() {
        return avgGoodsConvertRateEnd;
    }

    public void setAvgGoodsConvertRateEnd(BigDecimal avgGoodsConvertRateEnd) {
        this.avgGoodsConvertRateEnd = avgGoodsConvertRateEnd;
    }

    public BigDecimal getAvgGpmStart() {
        return avgGpmStart;
    }

    public void setAvgGpmStart(BigDecimal avgGpmStart) {
        this.avgGpmStart = avgGpmStart;
    }

    public BigDecimal getAvgGpmEnd() {
        return avgGpmEnd;
    }

    public void setAvgGpmEnd(BigDecimal avgGpmEnd) {
        this.avgGpmEnd = avgGpmEnd;
    }

    public BigDecimal getAvgShowWatchCntRatio() {
        return avgShowWatchCntRatio;
    }

    public void setAvgShowWatchCntRatio(BigDecimal avgShowWatchCntRatio) {
        this.avgShowWatchCntRatio = avgShowWatchCntRatio;
    }

    public BigDecimal getAvgRoi() {
        return avgRoi;
    }

    public void setAvgRoi(BigDecimal avgRoi) {
        this.avgRoi = avgRoi;
    }

    public BigDecimal getAvgLaunchRoiAmount() {
        return avgLaunchRoiAmount;
    }

    public void setAvgLaunchRoiAmount(BigDecimal avgLaunchRoiAmount) {
        this.avgLaunchRoiAmount = avgLaunchRoiAmount;
    }

    public BigDecimal getAvgRefundAmount() {
        return avgRefundAmount;
    }

    public void setAvgRefundAmount(BigDecimal avgRefundAmount) {
        this.avgRefundAmount = avgRefundAmount;
    }

    public BigDecimal getAvgOverallCostRoi() {
        return avgOverallCostRoi;
    }

    public void setAvgOverallCostRoi(BigDecimal avgOverallCostRoi) {
        this.avgOverallCostRoi = avgOverallCostRoi;
    }

    public BigDecimal getAvgNetTransactionRoi() {
        return avgNetTransactionRoi;
    }

    public void setAvgNetTransactionRoi(BigDecimal avgNetTransactionRoi) {
        this.avgNetTransactionRoi = avgNetTransactionRoi;
    }
}
