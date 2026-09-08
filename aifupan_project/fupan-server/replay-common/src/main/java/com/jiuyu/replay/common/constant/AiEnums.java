package com.jiuyu.replay.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/11/24 17:58
 */
public class AiEnums {

    /**
     * 使用来源类型 0：视频，1：文件，2对比分析，3数据截图，4提取文案视频，5主播
     */
    @Getter
    @AllArgsConstructor
    public enum useSourceType {
        VIDEO(0, "视频"),
        FILE(1, "文件"),
        SYNC_ANALYSIS(2, "对比分析"),
        DATA_SCREENSHOT(3, "数据截图"),
        EXTRACT_VIDEO(4, "提取文案视频"),
        ANCHOR_KEYWORD(5, "主播"),
        CONVERSATION(6, "ai问答(replay_ai_conversation)"),
        ;

        private final int code;
        private final String msg;
    }


    /**
     * 生成html状态 0：待生成，1：生成中，2：生成成功，3：生成失败
     */
    @Getter
    @AllArgsConstructor
    public enum htmlStatus {

        WAITING(0, "待生成"),
        GENERATING(1, "生成中"),
        SUCCESS(2, "生成成功"),
        FAIL(3, "生成失败");
        private final int code;
        private final String msg;
    }

    /**
     * html生成类型 0：服务器，1：客户端
     */
    @Getter
    @AllArgsConstructor
    public enum htmlType {
        SERVER(0, "服务器"),
        CLIENT(1, "客户端");
        private final int code;
        private final String msg;
    }

    /**
     * html生成类型 0：服务器，1：客户端
     */
    @Getter
    @AllArgsConstructor
    public enum askType {
        OPERATION(0, "运营助手"),
        VIOLATION(1, "违规助手"),
        BARRAGE(2, "弹幕助手"),
        SCREENSHOT(3, "截图助手"),
        BOARD(4, "看板助手"),
        IMPORTANT_SCREENSHOT(5, "重要弹幕提示词"),
        AI_SCRIPT_ASSISTANT(6, "AI话术助手"),
        NATURAL_ORIGINAL_TEXT(7, "自然原文提示词"),
        OPTIMIZE_ORIGINAL_TEXT(8, "优化原文提示词"),
        TRADE_RECOMMEND(9, "行业推荐"),
        EXTRACT_VIDEO(10, "提取文案优化"),
        ANCHOR_KEYWORD(11, "主播关键词获取"),
        AI_HTML_PROMPT(12, "ai结果转html"),
        DATA_DIAGNOSIS(13, "数据诊断提示词"),
        CHECK_AI_CORRECT(14, "检查ai内容"),
        AI_CORRECT_CONTENT(15, "ai纠正内容"),
        QUALITY_INSPECTION_GENERATE_PROMPT(16, "话术质检-生成提示词"),
        QUALITY_INSPECTION_MERGE_PROMPT(17, "话术质检-合并提示词"),
        /** 话术还原度-非循环对比生成提示词（2026-06-11 重命名，数值 18 不变；原语义含糊版本仅区分还原度/非还原度未区分循环模式）。 */
        FIDELITY_NON_CYCLIC_GENERATE(18, "话术还原度-非循环对比生成提示词"),
        /** 话术还原度-非循环合并校检提示词（2026-06-11 重命名，数值 19 不变）。 */
        FIDELITY_NON_CYCLIC_MERGE(19, "话术还原度-非循环合并校检提示词"),
        INTERACTION_PATROL_GENERATE_PROMPT(20, "互动巡检-生成提示词"),
        INTERACTION_PATROL_MERGE_PROMPT(21, "互动巡检-合并提示词"),
        // 业务类型 (assistant_type 字段专用，区别于上方 cueType 提示词类型；
        // 历史上 ScriptMonitor*GenerateBll 复用 DATA_DIAGNOSIS(13) 作 assistant_type 导致
        // 质检/巡检台账无法按 monitorType 维度统计；2026-06-09 拆出独立业务类型枚举)
        SCRIPT_QUALITY_INSPECTION(22, "话术质检（业务类型）"),
        INTERACTION_PATROL(23, "互动巡检（业务类型）"),
        FIDELITY_MONITOR(24, "话术还原度（业务类型；B5 预留）"),
        /** 话术还原度-循环对比生成提示词（2026-06-11 新增，Slice A）*/
        FIDELITY_CYCLIC_GENERATE(25, "话术还原度-循环对比生成提示词"),
        /** 话术还原度-循环合并校检提示词（2026-06-11 新增，Slice A）*/
        FIDELITY_CYCLIC_MERGE(26, "话术还原度-循环合并校检提示词"),
        /** 标准稿 AI 生成提示词，T21 generateStandardScript 使用（2026-06-11 新增，Slice A）*/
        STANDARD_SCRIPT_GENERATE(27, "标准稿生成提示词"),
        /** 互动巡检-格式校验并纠正提示词（2026-06-20 新增，合并报告格式校验+纠正单次调用元指令）*/
        INTERACTION_PATROL_FORMAT_PROMPT(28, "互动巡检-格式校验并纠正提示词"),
        ;
        private final int code;
        private final String msg;
    }


    /**
     * ai纠错状态 0：正常，1：纠错中，2：纠错完成，3：纠错失败
     */
    @Getter
    @AllArgsConstructor
    public enum correctStatus {
        NORMAL(0, "正常"),
        CORRECTING(1, "纠错中"),
        CORRECTED(2, "纠错完成"),
        FAIL(3, "纠错失败");
        private final int code;
        private final String msg;
    }

    /**
     * ai纠错来源类型 0：服务器，1：客户端
     */
    @Getter
    @AllArgsConstructor
    public enum correctType {
        SERVER(0, "服务器"),
        CLIENT(1, "客户端");
        private final int code;
        private final String msg;
    }

    /**
     * 诊断报告类型 0：内容诊断，1：数据诊断
     */
    @Getter
    @AllArgsConstructor
    public enum diagnosisType {
        CONTENT_DIAGNOSIS(0, "内容诊断"),
        DATA_DIAGNOSIS(1, "数据诊断"),
        ;
        private final int code;
        private final String msg;
    }

    /**
     * 诊断报告来源类型 0：主播，1：视频
     */
    @Getter
    @AllArgsConstructor
    public enum diagnosisSourceType {
        ANCHOR(0, "主播"),
        VIDEO(1, "视频");
        private final int code;
        private final String msg;
    }

    /**
     * 诊断报告状态 0：待生成，1：生成中，2：生成成功，3：生成失败
     */
    @Getter
    @AllArgsConstructor
    public enum qaStatus {

        WAITING(0, "待生成"),
        GENERATING(1, "生成中"),
        SUCCESS(2, "生成成功"),
        FAIL(3, "生成失败");
        private final int code;
        private final String msg;
    }

    /**
     * 资产创建方式 0：用户创建，1：系统创建
     */
    @Getter
    @AllArgsConstructor
    public enum assetCreationType {
        USER_CREATION(0, "用户创建"),
        SYSTEM_CREATION(1, "系统创建"),
        ;

        private final int code;
        private final String msg;
    }

    /**
     * 提示词类型 0系统，1用户
     */
    @Getter
    @AllArgsConstructor
    public enum cueWordsType {
        SYSTEM(0, "系统"),
        USER(1, "用户"),
        ;

        private final int code;
        private final String msg;
    }

    /**
     * 诊断报告已读状态 0：未读，1：已读
     */
    @Getter
    @AllArgsConstructor
    public enum readStatus {
        UNREAD(0, "未读"),
        READ(1, "已读"),
        ;
        private final int code;
        private final String msg;
    }

    /**
     * AI内容校正场景类型
     */
    @Getter
    @AllArgsConstructor
    public enum correctionSceneType {
        CHECK_AI_ANSWER(0, "AI问答助手的纠正检查"),
        CORRECT_AI_ANSWER(1, "AI问答助手的纠正"),
        CHECK_NATURAL_TEXT(2, "自然、优化原文的纠正检查"),
        CORRECT_NATURAL_TEXT(3, "自然、优化原文的纠正"),
        ;
        private final int code;
        private final String msg;
    }

    /**
     * 场景切片状态 0: 待开始, 1: 处理中, 2: 处理完成, 3: 处理失败
     */
    @Getter
    @AllArgsConstructor
    public enum sceneSliceStatus {
        PENDING(0, "待开始"),
        PROCESSING(1, "处理中"),
        COMPLETED(2, "处理完成"),
        FAILED(3, "处理失败");
        private final int code;
        private final String msg;
    }

    /**
     * 上下文缓存超时时间（秒）
     */
    public static final long CONTEXT_TIMEOUT = 3600L;
    /**
     * 映射key比消息缓存提前过期的余量（秒）
     */
    public static final long CONTEXT_TTL_MARGIN = 300L;

}
