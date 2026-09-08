package com.jiuyu.replay.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/6/26 上午11:05
 */
public class WordsEnum {

    /**
     * （自然/优化）原文生成状态
     */
    @Getter
    @AllArgsConstructor
    public enum contentStatus {

        PENDING(0, "待生成"),
        GENERATING(1, "生成中"),
        SUCCESS(2, "生成成功"),
        FAILED(3, "生成失败");
        private final int code;
        private final String msg;
    }

    /**
     * 来源类型
     */
    @Getter
    @AllArgsConstructor
    public enum sourceType {
        VIDEO(0, "视频"),
        FILE(1, "文件"),
        SYNC_CONTRAST(2, "对比分析"),
        ;
        private final int code;
        private final String msg;
    }

    /**
     * 内容类型
     */
    @Getter
    @AllArgsConstructor
    public enum contentType {
        MINUTE(0, "分钟段落"),
        NATURE(1, "自然原文"),
        OPTIMIZE(2, "优化原文"),
        ;
        private final int code;
        private final String msg;
    }

    @Getter
    @AllArgsConstructor
    public enum contentGenerateStatus {
        //        0未生成，1已生成
        NO_GENERATED(0, "未生成"),
        SUCCESS(1, "已生成"),
        ;
        private final int code;
        private final String msg;
    }

    /**
     * 内容生成来源
     */
    @Getter
    @AllArgsConstructor
    public enum contentSourceType {
        SERVER(0, "服务器"),
        CLIENT(1, "客户端"),
        ;
        private final int code;
        private final String msg;
    }

    /**
     * 账号归属类型 0：自有账号 1：同行账号
     */
    @Getter
    @AllArgsConstructor
    public enum accountType {
        OWN(0, "自有账号"),
        PEER(1, "同行账号"),
        ;
        private final int code;
        private final String msg;
    }

    /**
     * 平台类型 0：全平台 1：抖音 2：快手 3：视频号
     */
    @Getter
    @AllArgsConstructor
    public enum platformType {
        ALL(0, "全平台"),
        DOU_YIN(1, "抖音"),
        KUAI_SHOU(2, "快手"),
        SHI_PING_HAO(3, "视频号"),
        ;
        private final int code;
        private final String msg;
    }

    /**
     * 主播关键词状态 0未获取，1获取成功，2获取失败
     */
    @Getter
    @AllArgsConstructor
    public enum keywordStatus {
        BE_STARTED(0, "未获取"),
        SUCCESS(1, "获取成功"),
        FAIL(2, "获取失败"),
        ;
        private final int code;
        private final String msg;
    }

    /**
     * 来源类型 0：主播，1：视频，2：文件
     */
    @Getter
    @AllArgsConstructor
    public enum basicSettingsType {
        ANCHOR(0, "主播"),
        VIDEO(1, "视频"),
        FILE(2, "文件"),
        ;
        private final int code;
        private final String msg;
    }

    /**
     * 对比使用场景(对比分析才有) 1：对比上一次场、2：不同直播间对比、3：同直播间对比
     */
    @Getter
    @AllArgsConstructor
    public enum syncScene {
        LAST_SCENE(1, "对比上一次场"),
        DIFF_SCENE(2, "不同直播间对比"),
        SAME_SCENE(3, "同直播间对比"),
        ;
        private final int code;
        private final String msg;
    }

    /**
     * 知识库类型 1：运营知识库、2：敏感词知识库、3：直播间健康值
     */
    @Getter
    @AllArgsConstructor
    public enum knowledgeType {
        OPERATION(1, "运营知识库"),
        SENSITIVE(2, "敏感词知识库"),
        HEALTH_SCORE(3, "直播间健康值"),
        ;
        private final int code;
        private final String msg;
    }

}
