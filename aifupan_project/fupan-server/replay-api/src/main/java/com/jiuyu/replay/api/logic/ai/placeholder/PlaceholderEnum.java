package com.jiuyu.replay.api.logic.ai.placeholder;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 占位符枚举 — 唯一真相源。
 * DB 只是开关和 UI 元数据，解析逻辑全在这里。
 * <p>
 * 优先级：0 分钟段落+弹幕 → 5 基础字段 → 10 内容数据 → 20 计算数据 → 30 请求参数。
 * Resolver 按优先级从小到大依次解析（数字越小越先替换）。
 * <p>
 * systemPrompt=true 的占位符在上下文缓存模式下跳过解析（数据已在系统提示词中），
 * 简单对话模式下照常解析。
 *
 * @author jy
 * @date 2026-06-18
 */
@Getter
public enum PlaceholderEnum {

    // ===== 优先级 0：分钟段落+弹幕，最先替换 =====

    /**
     * 本场分钟段落，数据量大，上下文缓存模式下在系统提示词中
     */
    MINUTE_SEGMENT("minuteSegment", "本场分钟段落", 0, 40, null, true) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            String data = ctx.getMinuteSegment();
            if (StrUtil.isBlank(data)) return null;
            return ctx.getMinuteSegmentParamDesc() + data;
        }

        @Override
        public String getSystemPromptPlaceholder() {
            return "（分钟段落数据已在系统提示词中）";
        }

        @Override
        public String resolveForSystemPrompt(PlaceholderContext ctx) {
            return resolve(null, ctx);
        }
    },
    /**
     * 本场弹幕
     */
    DANMAKU("danmaku", "本场弹幕", 0, 41, null, true) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            String data = ctx.getDanmaku();
            if (StrUtil.isBlank(data)) return null;
            return ctx.getDanmakuParamDesc() + data;
        }

        @Override
        public String getSystemPromptPlaceholder() {
            return "（弹幕数据已在系统提示词中）";
        }

        @Override
        public String resolveForSystemPrompt(PlaceholderContext ctx) {
            return resolve(null, ctx);
        }
    },
    /**
     * 本场分钟段落+分钟弹幕：按段落交叉排列，每段先数据后弹幕
     */
    MINUTE_SEGMENT_DANMAKU("minuteSegmentDanmaku", "本场分钟段落+分钟弹幕", 0, 42, null, true) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            String data = ctx.getMinuteSegmentDanmaku();
            if (StrUtil.isBlank(data)) return null;
            return ctx.getMinuteSegmentDanmakuParamDesc() + data;
        }

        @Override
        public String getSystemPromptPlaceholder() {
            return "（分钟段落+弹幕数据已在系统提示词中）";
        }

        @Override
        public String resolveForSystemPrompt(PlaceholderContext ctx) {
            return resolve(null, ctx);
        }
    },

    // ===== 优先级 5：基础字段，直接查 BLL =====

    /**
     * 行业名称。单场查 TradeBll；对比两场相同→"都是XX行业"，不同→分列
     */
    TRADE("trade", "行业", 5, 1, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getTradeName();
        }
    },
    /**
     * 平台名称。Phase 2 默认"抖音"，后续注入 DictDataFeign
     */
    PLATFORM("platform", "平台", 5, 2, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getPlatformName();
        }
    },
    /**
     * 主播数据，调用 AnchorUrlBll.getAiAnchorPrompt 生成完整主播提示词
     */
    ANCHOR_DATA("anchorData", "主播数据", 5, 3, null, true) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getAnchorData();
        }
    },

    // ===== 优先级 10：内容数据，直接调 BLL =====

    /**
     * 本场数据（看板+截图）
     */
    LIVE_DATA("liveData", "本场数据（看板+截图）", 10, 10, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getLiveData();
        }
    },
    /**
     * 优化计划：查询 AiOptimizePurposeRse，获取优化动作 + 优化目的
     */
    OPTIMIZE_PLAN("optimizePlan", "优化计划", 10, 14, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getOptimizePlan();
        }
    },
    /**
     * 本场场景切片解析结果
     */
    SCENE_SLICE_RESULT("sceneSliceResult", "本场场景切片解析结果", 10, 12, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getSceneSliceResult();
        }
    },
    /**
     * 上一场场景切片解析结果
     */
    PREV_SCENE_SLICE_RESULT("prevSceneSliceResult", "上一场场景切片解析结果", 10, 87, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getPrevSceneSliceResult();
        }
    },
    /**
     * 本场直播关键词
     */
    LIVE_KEYWORDS("liveKeywords", "本场直播关键词", 10, 11, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getLiveKeywords();
        }
    },
    /**
     * 本场AI数据诊断结果
     */
    AI_DIAGNOSIS("aiDiagnosis", "本场AI数据诊断结果", 10, 15, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getAiDiagnosis();
        }
    },
    /**
     * 上一场AI数据诊断结果
     */
    PREV_AI_DIAGNOSIS("prevAiDiagnosis", "上一场AI数据诊断结果", 10, 50, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getPrevAiDiagnosis();
        }
    },
    /**
     * 本场最后一次综合分析结果
     */
    LAST_COMPREHENSIVE_ANALYSIS("lastComprehensiveAnalysis", "本场最后一次综合分析结果", 10, 17, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getLastComprehensiveAnalysis();
        }
    },
    /**
     * 本场最后一次分析结果
     */
    LAST_ANALYSIS("lastAnalysis", "本场最后一次分析结果", 10, 16, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getLastAnalysis();
        }
    },
    /**
     * 上一场最后一次综合分析结果
     */
    PREV_LAST_COMPREHENSIVE_ANALYSIS("prevLastComprehensiveAnalysis", "上一场最后一次综合分析结果", 10, 52, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getPrevLastComprehensiveAnalysis();
        }
    },
    /**
     * 上一场最后一次分析结果
     */
    PREV_LAST_ANALYSIS("prevLastAnalysis", "上一场最后一次分析结果", 10, 51, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getPrevLastAnalysis();
        }
    },
    /**
     * 本主播上一场最后一次分析结果
     */
    ANCHOR_PREV_LAST_ANALYSIS("anchorPrevLastAnalysis", "本主播上一场最后一次分析结果", 10, 85, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return unsupported();
        }
    },
    /**
     * 本主播上一场最后一次综合分析结果
     */
    ANCHOR_PREV_LAST_COMPREHENSIVE_ANALYSIS("anchorPrevLastComprehensiveAnalysis", "本主播上一场最后一次综合分析结果", 10, 86, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return unsupported();
        }
    },
    /**
     * 上一场分钟段落
     */
    PREV_MINUTE_SEGMENT("prevMinuteSegment", "上一场分钟段落", 10, 60, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            String data = ctx.getPrevMinuteSegment();
            if (StrUtil.isBlank(data)) return null;
            return ctx.getMinuteSegmentParamDesc() + data;
        }
    },
    /**
     * 上一场基础数据
     */
    PREV_BASIC_DATA("prevBasicData", "上一场基础数据", 10, 63, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getPrevBasicData();
        }
    },
    /**
     * 本场人群画像数据
     */
    AUDIENCE_PROFILE("audienceProfile", "本场人群画像数据", 10, 18, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getAudienceProfile();
        }
    },
    /**
     * 本场流量结构数据
     */
    TRAFFIC_STRUCTURE("trafficStructure", "本场流量结构数据", 10, 19, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getTrafficStructure();
        }
    },
    /**
     * 上一场人群画像数据
     */
    PREV_AUDIENCE_PROFILE("prevAudienceProfile", "上一场人群画像数据", 10, 20, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getPrevAudienceProfile();
        }
    },
    /**
     * 上一场流量结构数据
     */
    PREV_TRAFFIC_STRUCTURE("prevTrafficStructure", "上一场流量结构数据", 10, 21, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getPrevTrafficStructure();
        }
    },
    /**
     * 上一场直播弹幕内容
     */
    PREV_DANMAKU("prevDanmaku", "上一场直播弹幕内容", 10, 62, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            String data = ctx.getPrevDanmaku();
            if (StrUtil.isBlank(data)) return null;
            return ctx.getDanmakuParamDesc() + data;
        }
    },
    /**
     * 上一场分钟段落+分钟弹幕
     */
    PREV_MINUTE_SEGMENT_DANMAKU("prevMinuteSegmentDanmaku", "上一场分钟段落+分钟弹幕", 10, 61, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            String data = ctx.getPrevMinuteSegmentDanmaku();
            if (StrUtil.isBlank(data)) return null;
            return ctx.getMinuteSegmentDanmakuParamDesc() + data;
        }
    },
    /**
     * 上一场关键词
     */
    PREV_KEYWORDS("prevKeywords", "上一场关键词", 10, 64, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getPrevKeywords();
        }
    },
    /**
     * 本场直播商品的数据
     */
    LIVE_PRODUCTS("liveProducts", "本场直播商品的数据", 10, 13, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return unsupported();
        }
    },
    /**
     * 行业敏感词
     */
    TRADE_SENSITIVE_WORDS("tradeSensitiveWords", "行业敏感词", 10, 80, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getTradeSensitiveWords();
        }
    },
    /**
     * 行业关键词
     */
    TRADE_KEYWORDS("tradeKeywords", "行业关键词", 10, 81, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getTradeKeywords();
        }
    },
    /**
     * 系统行业运营知识库，按行业层级向上查找，兜底通用行业
     */
    TRADE_OPERATION_KNOWLEDGE("tradeOperationKnowledge", "系统行业运营知识库", 10, 70, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getTradeOperationKnowledge();
        }
    },
    /**
     * 系统行业违规知识库，按行业层级向上查找，兜底通用行业
     */
    TRADE_VIOLATION_KNOWLEDGE("tradeViolationKnowledge", "系统行业违规知识库", 10, 71, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getTradeViolationKnowledge();
        }
    },
    /**
     * 系统行业敏感词知识库，按行业层级向上查找，兜底通用行业
     */
    TRADE_SENSITIVE_KNOWLEDGE("tradeSensitiveKnowledge", "系统行业敏感词知识库", 10, 72, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getTradeSensitiveKnowledge();
        }
    },
    /**
     * 主播级知识库（运营+敏感词+健康值），仅视频支持，对比分析区分同/不同主播
     */
    ANCHOR_KNOWLEDGE("anchorKnowledge", "主播知识库（运营+敏感词+健康值）", 10, 73, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getAnchorKnowledge();
        }
    },
    /**
     * 主播运营知识库（拆分自 anchorKnowledge）
     */
    ANCHOR_OPERATION_KNOWLEDGE("anchorOperationKnowledge", "主播运营知识库", 10, 74, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getAnchorOperationKnowledge();
        }
    },
    /**
     * 主播敏感词知识库（拆分自 anchorKnowledge）
     */
    ANCHOR_SENSITIVE_KNOWLEDGE("anchorSensitiveKnowledge", "主播敏感词知识库", 10, 75, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getAnchorSensitiveKnowledge();
        }
    },
    /**
     * 主播健康值知识库（拆分自 anchorKnowledge）
     */
    ANCHOR_HEALTH_SCORE("anchorHealthScore", "主播健康值知识库", 10, 76, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getAnchorHealthScore();
        }
    },

    // ===== 优先级 20：依赖内容数据做计算 =====

    /**
     * 本场成交率提升最多前X段（默认10段），参数示例: #{conversionIncreaseTop:20}
     */
    CONVERSION_INCREASE_TOP("conversionIncreaseTop", "本场成交率提升最多前X段", 20, 22, "10", false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getConversionIncreaseTop(parseInt(param, 10));
        }
    },
    /**
     * 本场成交率降低最多前X段（默认10段），参数示例: #{conversionDecreaseTop:20}
     */
    CONVERSION_DECREASE_TOP("conversionDecreaseTop", "本场成交率降低最多前X段", 20, 23, "10", false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getConversionDecreaseTop(parseInt(param, 10));
        }
    },
    /**
     * 本场UV价值提升最多前X段（默认10段），参数示例: #{uvIncreaseTop:20}
     */
    UV_INCREASE_TOP("uvIncreaseTop", "本场UV价值提升最多前X段", 20, 24, "10", false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getUvIncreaseTop(parseInt(param, 10));
        }
    },
    /**
     * 本场UV价值降低最多前X段（默认10段），参数示例: #{uvDecreaseTop:20}
     */
    UV_DECREASE_TOP("uvDecreaseTop", "本场UV价值降低最多前X段", 20, 25, "10", false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getUvDecreaseTop(parseInt(param, 10));
        }
    },
    /**
     * 本场互动率提升最多前X段（默认10段），参数示例: #{interactIncreaseTop:10}
     */
    INTERACT_INCREASE_TOP("interactIncreaseTop", "本场互动率提升最多前X段", 20, 26, "10", false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getInteractIncreaseTop(parseInt(param, 10));
        }
    },
    /**
     * 本场互动率降低最多前X段（默认10段），参数示例: #{interactDecreaseTop:10}
     */
    INTERACT_DECREASE_TOP("interactDecreaseTop", "本场互动率降低最多前X段", 20, 27, "10", false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getInteractDecreaseTop(parseInt(param, 10));
        }
    },
    /**
     * 本场在线人数提升最多前X段（默认10段），参数示例: #{onlineIncreaseTop:10}
     */
    ONLINE_INCREASE_TOP("onlineIncreaseTop", "本场在线人数提升最多前X段", 20, 28, "10", false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getOnlineIncreaseTop(parseInt(param, 10));
        }
    },
    /**
     * 本场在线人数降低最多前X段（默认10段），参数示例: #{onlineDecreaseTop:10}
     */
    ONLINE_DECREASE_TOP("onlineDecreaseTop", "本场在线人数降低最多前X段", 20, 29, "10", false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getOnlineDecreaseTop(parseInt(param, 10));
        }
    },
    /**
     * 本场在线提升率最多前X段（默认10段），参数示例: #{onlineRateIncreaseTop:10}
     */
    ONLINE_RATE_INCREASE_TOP("onlineRateIncreaseTop", "本场在线提升率最多前X段", 20, 30, "10", false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getOnlineRateIncreaseTop(parseInt(param, 10));
        }
    },
    /**
     * 本场在线降低率最多前X段（默认10段），参数示例: #{onlineRateDecreaseTop:10}
     */
    ONLINE_RATE_DECREASE_TOP("onlineRateDecreaseTop", "本场在线降低率最多前X段", 20, 31, "10", false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getOnlineRateDecreaseTop(parseInt(param, 10));
        }
    },
    /**
     * 抖音同行直播基础数据平均值，参数示例: #{douyinPeerAvg:3}（默认1天，最小值1）
     */
    DOUYIN_PEER_AVG("douyinPeerAvg", "抖音同行直播基础数据平均值", 20, 100, "1", false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getDouyinPeerAvg(parseInt(param, 1));
        }
    },

    // ===== 优先级 30：外部请求参数 =====

    /**
     * 违规原因，来自请求参数 askRequestBo.reasonViolation
     */
    REASON("reason", "违规原因", 30, 140, null, false) {
        @Override
        public String resolve(String param, PlaceholderContext ctx) {
            return ctx.getReasonViolation();
        }
    };

    /**
     * 占位符 key，如 "trade"、"uvTop"，对应 #{key} 或 #{key:param}
     */
    private final String key;
    /**
     * 前端展示名称
     */
    private final String displayName;
    /**
     * 解析优先级：越小越先解析
     */
    private final int priority;
    /**
     * 默认参数值；null 表示不接受参数
     */
    private final String defaultParam;
    /**
     * 上下文缓存模式下，此占位符的数据是否已在系统提示词中
     */
    private final boolean systemPrompt;
    /**
     * 提示词位置排序：越小越靠前
     */
    private final int promptOrder;

    PlaceholderEnum(String key, String displayName, int priority, int promptOrder, String defaultParam, boolean systemPrompt) {
        this.key = key;
        this.displayName = displayName;
        this.priority = priority;
        this.promptOrder = promptOrder;
        this.defaultParam = defaultParam;
        this.systemPrompt = systemPrompt;
    }

    /**
     * 解析占位符值。
     *
     * @param param 占位符中传入的参数（如 #{uvTop:20} → "20"），未传时为 null
     * @param ctx   数据协调者，提供懒加载的 BLL 数据
     * @return 解析后的字符串值，null 表示尚未实现（Resolver 将保留原文）
     */
    public abstract String resolve(String param, PlaceholderContext ctx);

    /**
     * 获取写进系统提示词的原始数据（不含字段说明），供 {@code buildSystemPrompt()} 使用。
     * 默认回退到 {@link #resolve(String, PlaceholderContext)}；数据量大的缓存类枚举可覆盖返回裸数据。
     */
    public String resolveForSystemPrompt(PlaceholderContext ctx) {
        return "";
    }

    /**
     * 未实现时返回 null → Resolver 看到 null 保留原文
     */
    protected String unsupported() {
        return null;
    }

    /**
     * 安全解析 int 参数，解析失败返回默认值
     */
    private static int parseInt(String str, int defaultValue) {
        if (StrUtil.isBlank(str)) return defaultValue;
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 上下文缓存模式下，systemPrompt=true 的占位符被跳过时返回的占位文字。
     * 默认返回空字符串；子枚举可覆盖提供更具体的提示。
     */
    public String getSystemPromptPlaceholder() {
        return "";
    }

    /**
     * 按 key 查枚举。
     *
     * @param key 占位符 key（如 "trade"、"uvTop"）
     * @return 匹配的枚举值，未找到返回 null
     */
    public static PlaceholderEnum of(String key) {
        for (PlaceholderEnum e : values()) {
            if (e.key.equals(key)) return e;
        }
        return null;
    }

    /**
     * 按 key 查枚举。
     *
     * @param key 占位符 key（如 "#{trade}"、"#{uvTop:20}"）
     * @return 匹配的枚举值，未找到返回 null
     */
    public static PlaceholderEnum fullOf(String key) {
        key = key.trim();

        if (!key.startsWith("#{") || !key.endsWith("}")) {
            return of(key);
        }
        key = key.substring(2, key.length() - 1);
        String newKey = key;
        if (key.contains(":")) {
            newKey = key.substring(0, key.indexOf(":"));
        }
        return of(newKey);
    }

    /**
     * 获取全名
     *
     * @return 全名
     */
    public String getFullKey() {
        return "#{" + key + "}";
    }
}
