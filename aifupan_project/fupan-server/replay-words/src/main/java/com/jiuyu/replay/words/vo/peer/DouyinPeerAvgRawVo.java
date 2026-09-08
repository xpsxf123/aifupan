package com.jiuyu.replay.words.vo.peer;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 同行平均值原始明细行 — Mapper JOIN 查询结果映射
 *
 * @author jy
 * @date 2026-06-25
 */
public class DouyinPeerAvgRawVo {

    private Long tradeId;
    private String secUid;
    private Date statDate;

    private Integer totalWatchNum;
    private Integer averageOnlineNum;
    private Integer averageResidenceTime;
    private Integer incrementFollowerCount;
    private Double convertFanRate;
    private Double interactionPercent;

    private Integer volumeStart;
    private Integer volumeEnd;
    private Integer purchaseCountStart;
    private Integer purchaseCountEnd;
    private Double customerUnitPriceStart;
    private Double customerUnitPriceEnd;
    private Double uvValueStart;
    private Double uvValueEnd;
    private Double goodsConvertRateStart;
    private Double goodsConvertRateEnd;
    private Double gpmStart;
    private Double gpmEnd;

    public Long getTradeId() {
        return tradeId;
    }

    public void setTradeId(Long tradeId) {
        this.tradeId = tradeId;
    }

    public String getSecUid() {
        return secUid;
    }

    public void setSecUid(String secUid) {
        this.secUid = secUid;
    }

    public Date getStatDate() {
        return statDate;
    }

    public void setStatDate(Date statDate) {
        this.statDate = statDate;
    }

    public Integer getTotalWatchNum() {
        return totalWatchNum;
    }

    public void setTotalWatchNum(Integer totalWatchNum) {
        this.totalWatchNum = totalWatchNum;
    }

    public Integer getAverageOnlineNum() {
        return averageOnlineNum;
    }

    public void setAverageOnlineNum(Integer averageOnlineNum) {
        this.averageOnlineNum = averageOnlineNum;
    }

    public Integer getAverageResidenceTime() {
        return averageResidenceTime;
    }

    public void setAverageResidenceTime(Integer averageResidenceTime) {
        this.averageResidenceTime = averageResidenceTime;
    }

    public Integer getIncrementFollowerCount() {
        return incrementFollowerCount;
    }

    public void setIncrementFollowerCount(Integer incrementFollowerCount) {
        this.incrementFollowerCount = incrementFollowerCount;
    }

    public Double getConvertFanRate() {
        return convertFanRate;
    }

    public void setConvertFanRate(Double convertFanRate) {
        this.convertFanRate = convertFanRate;
    }

    public Double getInteractionPercent() {
        return interactionPercent;
    }

    public void setInteractionPercent(Double interactionPercent) {
        this.interactionPercent = interactionPercent;
    }

    public Integer getVolumeStart() {
        return volumeStart;
    }

    public void setVolumeStart(Integer volumeStart) {
        this.volumeStart = volumeStart;
    }

    public Integer getVolumeEnd() {
        return volumeEnd;
    }

    public void setVolumeEnd(Integer volumeEnd) {
        this.volumeEnd = volumeEnd;
    }

    public Integer getPurchaseCountStart() {
        return purchaseCountStart;
    }

    public void setPurchaseCountStart(Integer purchaseCountStart) {
        this.purchaseCountStart = purchaseCountStart;
    }

    public Integer getPurchaseCountEnd() {
        return purchaseCountEnd;
    }

    public void setPurchaseCountEnd(Integer purchaseCountEnd) {
        this.purchaseCountEnd = purchaseCountEnd;
    }

    public Double getCustomerUnitPriceStart() {
        return customerUnitPriceStart;
    }

    public void setCustomerUnitPriceStart(Double customerUnitPriceStart) {
        this.customerUnitPriceStart = customerUnitPriceStart;
    }

    public Double getCustomerUnitPriceEnd() {
        return customerUnitPriceEnd;
    }

    public void setCustomerUnitPriceEnd(Double customerUnitPriceEnd) {
        this.customerUnitPriceEnd = customerUnitPriceEnd;
    }

    public Double getUvValueStart() {
        return uvValueStart;
    }

    public void setUvValueStart(Double uvValueStart) {
        this.uvValueStart = uvValueStart;
    }

    public Double getUvValueEnd() {
        return uvValueEnd;
    }

    public void setUvValueEnd(Double uvValueEnd) {
        this.uvValueEnd = uvValueEnd;
    }

    public Double getGoodsConvertRateStart() {
        return goodsConvertRateStart;
    }

    public void setGoodsConvertRateStart(Double goodsConvertRateStart) {
        this.goodsConvertRateStart = goodsConvertRateStart;
    }

    public Double getGoodsConvertRateEnd() {
        return goodsConvertRateEnd;
    }

    public void setGoodsConvertRateEnd(Double goodsConvertRateEnd) {
        this.goodsConvertRateEnd = goodsConvertRateEnd;
    }

    public Double getGpmStart() {
        return gpmStart;
    }

    public void setGpmStart(Double gpmStart) {
        this.gpmStart = gpmStart;
    }

    public Double getGpmEnd() {
        return gpmEnd;
    }

    public void setGpmEnd(Double gpmEnd) {
        this.gpmEnd = gpmEnd;
    }

}
