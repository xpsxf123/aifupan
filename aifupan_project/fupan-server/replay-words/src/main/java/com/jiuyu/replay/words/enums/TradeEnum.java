package com.jiuyu.replay.words.enums;

public enum TradeEnum {

    GENERAL_TRADE_ID(1L, "通用行业id");

    private final Long tradeId;
    private final String remarks;

    TradeEnum(Long tradeId, String remarks) {
        this.tradeId = tradeId;
        this.remarks = remarks;
    }

    public Long getTradeId() {
        return tradeId;
    }

    public String getRemarks() {
        return remarks;
    }
}
