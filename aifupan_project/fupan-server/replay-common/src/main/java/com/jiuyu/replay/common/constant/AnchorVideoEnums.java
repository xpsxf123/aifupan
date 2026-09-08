package com.jiuyu.replay.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ：lujie
 * &#064;description：视频相关的枚举类
 * @date ：2025/7/9 下午4:08
 */
public class AnchorVideoEnums {

    /**
     * 视频分析状态
     */
    @Getter
    @AllArgsConstructor
    public enum analysisStatus {
        // 分析状态 0：未分析 1：分析中 2：分析完成 3分析错误
        NOT_ANALYZED(0, "未分析"),
        ANALYSIS_ING(1, "分析中"),
        ANALYSIS_COMPLETE(2, "分析完成"),
        ANALYSIS_ERROR(3, "分析错误"),
        NOT_AUTO_ANALYSIS(4, "未开启自动分析"),
        SLICING(5, "切片中");
        private final int code;
        private final String msg;
    }

    /**
     * 数据来源
     */
    @Getter
    @AllArgsConstructor
    public enum dataSource {
        NORMAL(0, "正常获取"),
        JULIANG(1, "巨量拉取"),
        ;
        private final int code;
        private final String msg;
    }

    /**
     * 视频下载状态
     */
    @Getter
    @AllArgsConstructor
    public enum downloadStatus {
        NOT_DOWNLOADED(0, "未下载"),
        DOWNLOADED(1, "已下载"),
        ;
        private final int code;
        private final String msg;
    }

    /**
     * 数据看板的状态
     */
    @Getter
    @AllArgsConstructor
    public enum videoDataViewingStatus {
        // 0：正在拉取 1：拉取成功 2：拉取失败 3：未收录主播 4：自动生成但视频未达到50分钟 5：资源不足 6：自动生成但主播未下播 7：已收录但直播列表为空 8：正确数据整理中
        PULLING(0, "正在拉取"),
        SUCCESS(1, "拉取成功"),
        FAIL(2, "拉取失败"),
        NOT_INCLUDE_ANCHOR(3, "未收录主播"),
        NOT_ENOUGH_TIME(4, "自动生成但视频未达到50分钟"),
        NOT_ENOUGH_RESOURCE(5, "资源不足"),
        NOT_ENOUGH_ANCHOR_NUMBER(6, "主播编号不足"),
        NOT_ENOUGH_REQUEST_ID(7, "请求ID不足"),
        SALES_DATA_IS_BEING_SUMMARIZED(8, "销售数据汇总中"),
        ;

        private final int code;
        private final String msg;
    }

}
