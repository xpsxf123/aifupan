package com.jiuyu.replay.words.enums;

import lombok.Getter;

@Getter
public enum DataViewingStatusEnum {

    PULLING(0, "正在拉取"),
    PULL_SUCCESS(1, "拉取成功"),
    PULL_FAIL(2, "拉取失败"),
    NOT_INCLUDE_ANCHOR(3, "未收录主播"),
    VIDEO_DURATION_SHORT(4, "自动生成但视频未达到50分钟"),
    PROPERTY_LACK(5, "资源不足"),
    ANCHOR_ONLINE(6, "自动生成但主播未下播"),
    LIVE_IS_NULL(7, "已收录但直播列表为空"),
    DATA_ORGANIZE(8, "正确数据整理中");

    private final Integer status;
    private final String remarks;

    DataViewingStatusEnum(Integer status, String remarks) {
        this.status = status;
        this.remarks = remarks;
    }
}
