package com.jiuyu.replay.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

public class OrderEnums {

    /**
     * 商品类型
     */
    @Getter
    @AllArgsConstructor
    public enum commodityTypeCode {
        AI_ANALYSIS_TIME("aiAnalysisTime", "ai语音分析时长"),
        TEXT_TAGGING_WORD_COUNT("textTaggingWordCount", "文本标注字数"),
        MONITOR_NUM("monitorNum", "主账号监控位"),
        ANCHOR_NUM("anchorNum", "主账号可添加主播数"),
        STORAGE_NUM("storageNum", "空间容量"),
        SUB_ACCOUNT_COUNT("subAccountCount", "拥有子账号数量"),
        CHILD_MONITOR_NUM("child_monitorNum", "子账号监控位"),
        CHILD_ANCHOR_NUM("child_anchorNum", "子账号可添加主播数"),
        ANCHOR_BARRAGE_NUM("anchorBarrageNum", "主播弹幕监控位"),
        BARRAGE_NUM("barrageNum", "弹幕显示条数"),
        SHOW_BARRAGE_BUBBLE("showBarrageBubble", "点击弹幕显示气泡(0否1是)"),
        CHILD_ANCHOR_BARRAGE_NUM("child_anchorBarrageNum", "子账号主播弹幕监控位"),
        AI_TOKEN_NUM("aiTokenNum", "AI分析文本数量"),
        IMG_IDENTIFY_NUM("imgIdentifyNum", "图片识别数量"),
        DATA_BOARD_NUM("dataBoardNum", "数据看板"),
        SMS_MESSAGE_NUM("smsMessageNum", "短信条数"),
        TEXT_EXTRACTION_NUM("textExtractionNum", "文案提取时长"),
        RPA_AMOUNT_NUM("rpaAmountNum", "主账号巨量监控位"),
        CHILD_RPA_AMOUNT_NUM("child_rpaAmountNum", "子账号巨量监控位"),
        SHORT_VIDEO_NUM("shortVideoNum", "提取文案条数"),
        SEARCH_INFLUENCER_NUM("searchInfluencerNum", "主账号搜索达人数量"),
        CHILD_SEARCH_INFLUENCER_NUM("child_searchInfluencerNum", "子账号搜索达人数量"),
        SUBSCRIBE_INFLUENCER_NUM("subscribeInfluencerNum", "订阅达人数量"),
        SEARCH_HOT_VIDEO_NUM("searchHotVideoNum", "主账号搜索爆款数量"),
        CHILD_SEARCH_HOT_VIDEO_NUM("child_searchHotVideoNum", "子账号搜索爆款数量"),
        SUBSCRIBE_HOT_VIDEO_NUM("subscribeHotVideoNum", "订阅爆款数量"),
        USER_SUBSCRIBE_INFLUENCER_NUM("userSubscribeInfluencerNum", "主账号账号订阅达人数量"),
        CHILD_USER_SUBSCRIBE_INFLUENCER_NUM("child_userSubscribeInfluencerNum", "子账号账号订阅达人数量"),
        USER_SUBSCRIBE_HOT_VIDEO_NUM("userSubscribeHotVideoNum", "主账号账号订阅爆款数量"),
        CHILD_USER_SUBSCRIBE_HOT_VIDEO_NUM("child_userSubscribeHotVideoNum", "子账号账号订阅爆款数量"),
        ENTERPRISE_PERSON_NUM("enterprisePersonNum", "人员数量"),
        ENTERPRISE_SUBSIDIARIES_NUM("enterpriseSubsidiariesNum", "子公司数量"),
        SCRIPT_QUALITY_NUM("scriptQualityNum", "话术质检监控位"),
        SCRIPT_FIDELITY_NUM("scriptFidelityNum", "话术还原度监控位"),
        INTERACTION_PATROL_NUM("interactionPatrolNum", "互动巡检监控位"),
        ;
        private final String code;
        private final String msg;

        /**
         * 单前code是否是短视频相关的code
         *
         * @param code 资产code
         * @return 是否
         */
        public static boolean isUserShortVideoProperty(String code) {
            List<commodityTypeCode> list = Arrays.asList(
                    SUBSCRIBE_INFLUENCER_NUM,
                    SUBSCRIBE_HOT_VIDEO_NUM,
                    // 主账号账号订阅达人数量
                    USER_SUBSCRIBE_INFLUENCER_NUM,

                    // 子账号账号订阅达人数量
                    CHILD_USER_SUBSCRIBE_INFLUENCER_NUM,

                    // 主账号账号订阅爆款数量
                    USER_SUBSCRIBE_HOT_VIDEO_NUM,

                    // 子账号账号订阅爆款数量
                    CHILD_USER_SUBSCRIBE_HOT_VIDEO_NUM
            );
            return list.stream().anyMatch(commodityTypeCode -> code.equals(commodityTypeCode.code));
        }

        /**
         * 获取共享订阅对应的本地订阅
         *
         * @param code
         * @return
         */
        public static commodityTypeCode getUserSubscribeEnum(String code) {
            if (SUBSCRIBE_INFLUENCER_NUM.getCode().equals(code)) {
                return USER_SUBSCRIBE_INFLUENCER_NUM;
            } else if (SUBSCRIBE_HOT_VIDEO_NUM.getCode().equals(code)) {
                return USER_SUBSCRIBE_HOT_VIDEO_NUM;
            }
            return null;
        }
    }

    /**
     * 使用状态-tb_type_surplus
     */
    @Getter
    @AllArgsConstructor
    public enum UseStatus {

        // 0在使用，1已用完，2弃用(当前订单已经升级)，3时间过期， 4冻结
        USING(0, "正在使用"),
        USED(1, "已用完"),
        DISABLED(2, "弃用"),
        EXPIRED(3, "时间过期"),
        FREEZE(4, "冻结"),
        ;
        private final Integer code;
        private final String msg;
    }

    /**
     * 时间状态-tb_type_surplus
     */
    @Getter
    @AllArgsConstructor
    public enum TimeStatus {
        // 0生效中，1已过期，2停用，3冻结
        EFFECTIVE(0, "生效中"),
        EXPIRED(1, "已过期"),
        DISABLED(2, "停用"),
        FREEZE(3, "冻结"),
        ;
        private final Integer code;
        private final String msg;
    }


    @Getter
    @AllArgsConstructor
    public enum signs {
        // 操作类型 0：减 1：加
        SUBTRACT(0, "减"),
        ADD(1, "加"),
        ;
        private final Integer code;
        private final String msg;
    }

    /**
     * 自定义类型
     */
    @Getter
    @AllArgsConstructor
    public enum customizeType {
        // 0：自定义 1：非自定义
        SYSTEM(0, "系统"),
        CUSTOMIZE(1, "自定义"),
        ;
        private final Integer code;
        private final String msg;
    }

    /**
     * 套餐类型(用户只能购买主要套餐)：1主要套餐，2次要套餐
     */
    @Getter
    @AllArgsConstructor
    public enum packageType {
        MAIN_PACKAGE(1, "主要套餐"),
        SUB_PACKAGE(2, "次要套餐");
        private final Integer code;
        private final String msg;
    }

    /**
     * 状态
     */
    @Getter
    @AllArgsConstructor
    public enum status {
        ENABLE(1, "启用"),
        DISABLE(0, "禁用");
        private final Integer code;
        private final String msg;
    }

    /**
     * 客户端的版本
     */
    @Getter
    @AllArgsConstructor
    public enum clientVersion {
        RECORD("record", "纯录制版"),
        REPLAY("replay", "复盘版");
        private final String code;
        private final String msg;
    }


    /**
     * 是否选择版本 0-不选版本，1-选择版本
     */
    @Getter
    @AllArgsConstructor
    public enum versionSelect {
        NOT_SELECT(0, "不选择版本"),
        SELECT(1, "选择版本");
        private final Integer code;
        private final String msg;
    }

}
