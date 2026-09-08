package com.jiuyu.replay.words.vo.viewing;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.framework.util.FunctionUtil;
import com.jiuyu.replay.common.constant.AnchorVideoEnums;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 视频看盘混淆后的数据信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-12 16:31:15
 */
@Data
@Schema(description = "视频看盘混淆后的数据信息")
@Slf4j
public class VideoDataViewingConfuseVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @Schema(description = "ID")
    private Long id;
    /**
     * 视频id
     */
    @Schema(description = "视频id")
    private String videoId;
    /**
     * 用户id
     */
    @Schema(description = "用户id")
    private Long userId;
    /**
     * 租户id
     */
    @Schema(description = "租户id")
    private Long tenantId;
    /**
     * 总观看人次
     */
    @Schema(description = "总观看人次")
    private Integer totalWatchNum;
    /**
     * 平均在线人数
     */
    @Schema(description = "平均在线人数")
    private Integer averageOnlineNum;
    /**
     * 平均停留时间(秒)
     */
    @Schema(description = "平均停留时间(秒)")
    private Integer averageResidenceTime;
    /**
     * 新增粉丝数
     */
    @Schema(description = "新增粉丝数")
    private Integer incrementFollowerCount;
    /**
     * 粉丝转化率
     */
    @Schema(description = "粉丝转化率")
    private Double convertFanRate;
    /**
     * 互动率
     */
    @Schema(description = "互动率")
    private Double interactionPercent;
    /**
     * 销售额区间范围-起始(单位:元)
     */
    @Schema(description = "销售额区间范围-起始(单位:元)")
    private Integer volumeStart;
    /**
     * 销售额区间范围-结束(单位:元)
     */
    @Schema(description = "销售额区间范围-结束(单位:元)")
    private Integer volumeEnd;
    /**
     * 销量区间范围-起始
     */
    @Schema(description = "销量区间范围-起始")
    private Integer purchaseCountStart;
    /**
     * 销量区间范围-结束
     */
    @Schema(description = "销量区间范围-结束")
    private Integer purchaseCountEnd;
    /**
     * 客单价区间范围-起始（单位：元）
     */
    @Schema(description = "客单价区间范围-起始（单位：元）")
    private Double customerUnitPriceStart;
    /**
     * 客单价区间范围-结束（单位：元）
     */
    @Schema(description = "客单价区间范围-结束（单位：元）")
    private Double customerUnitPriceEnd;
    /**
     * uv价值区间范围-起始
     */
    @Schema(description = "uv价值区间范围-起始")
    private Double uvValueStart;
    /**
     * uv价值区间范围-结束
     */
    @Schema(description = "uv价值区间范围-结束")
    private Double uvValueEnd;
    /**
     * 带货转换率区间范围-起始
     */
    @Schema(description = "带货转换率区间范围-起始")
    private Double goodsConvertRateStart;
    /**
     * 带货转换率区间范围-结束
     */
    @Schema(description = "带货转换率区间范围-结束")
    private Double goodsConvertRateEnd;
    /**
     * 数据状态 0：正在拉取 1：拉取成功 2：拉取失败 3：未收录主播 4：自动生成但视频未达到50分钟 5：资源不足 6：自动生成但主播未下播 7：已收录但直播列表为空 8：正确数据整理中
     */
    @Schema(description = "数据状态 0：正在拉取 1：拉取成功 2：拉取失败 3：未收录主播 4：自动生成但视频未达到50分钟 5：资源不足 6：自动生成但主播未下播 7：已收录但直播列表为空 8：正确数据整理中")
    private Integer dataStatus;
    /**
     * 关联的数据看盘id
     */
    @Schema(description = "关联的数据看盘id")
    private Long videoDataViewingId;
    /**
     * 是否带货 0：否 1：是
     */
    @Schema(description = "是否带货 0：否 1：是")
    private Integer isTakeProduct;
    /**
     * 直播场次号
     */
    @Schema(description = "直播场次号")
    private String batchNumber;
    /**
     * 请求id
     */
    @Schema(description = "请求id")
    private String requestId;
    /**
     * 主播抖音号
     */
    @Schema(description = "主播抖音号")
    private String anchorNumber;
    /**
     * 数据抓取时间
     */
    @Schema(description = "数据抓取时间")
    private Date crawlTime;
    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createDate;
    /**
     * 最后修改时间
     */
    @Schema(description = "最后修改时间")
    private Date updateDate;
    /**
     * 是否已删除
     */
    @Schema(description = "是否已删除")
    private Integer isDeleted;


    //巨量新增

//    /**
//     * 来源表示，0为蝉妈妈，1为巨量
//     */
//    @Schema(description = "0为蝉妈妈，1为巨量")
//    private Integer sourceFrom;
//
//    /**
//     * 销售额
//     */
//    @Schema(description = "销售额")
//    private Integer volume;
//
//    /**
//     * 销量
//     */
//    @Schema(description = "销量")
//    private Integer purchaseCount;
//
//    /**
//     * 客单价
//     */
//    @Schema(description = "客单价")
//    private Double customerUnitPrice;
//
//    /**
//     * uv价值
//     */
//    @Schema(description = "uv价值")
//    private Double uvValue;
//
//    /**
//     * 带货转换率
//     */
//    @Schema(description = "带货转换率")
//    private Double goodsConvertRate;

    /**
     * 看播流量结构
     */
    @Schema(description = "看播流量结构")
    private String watchFlowList;

    /**
     * 成交流量结构
     */
    @Schema(description = "成交流量结构")
    private String payFlowList;

    /**
     * 成交用户画像
     */
    @Schema(description = "成交用户画像")
    private String payUserPortrait;

    /**
     * 看播用户画像
     */
    @Schema(description = "看播用户画像")
    private String watchUserPortrait;
    /**
     * 巨量oss存储地址
     */
    @Schema(description = "巨量oss存储地址")
    private String ossPath;
    /**
     * 数据来源类型 0：蝉妈妈 1：巨量百应
     */
    @Schema(description = "数据来源类型 0：蝉妈妈 1：巨量百应")
    private Integer dataSourceType;
    /**
     * 主播secUid
     */
    @Schema(description = "主播secUid")
    private String secUid;
    /**
     * 千次观看成交金额范围-起始 （单位：元）
     */
    @Schema(description = "千次观看成交金额范围-起始 （单位：元）")
    private Double gpmStart;
    /**
     * 千次观看成交金额范围-结束 （单位：元）
     */
    @Schema(description = "千次观看成交金额范围-结束 （单位：元）")
    private Double gpmEnd;
    /**
     * 曝光-观看率
     */
    @Schema(description = "曝光-观看率")
    private Double showWatchCntRatio;
    /**
     * ROI
     */
    @Schema(description = "ROI")
    private Double roi;
    /**
     * 投放ROI金额（单位：元）
     */
    @Schema(description = "投放ROI金额（单位：元）")
    private Double launchRoiAmount;
    /**
     * 退款金额（单位：元）
     */
    @Schema(description = "退款金额（单位：元）")
    private Double refundAmount;
    /**
     * 整体消耗ROI
     */
    @Schema(description = "整体消耗ROI")
    private Double overallCostRoi;
    /**
     * 净成交ROI
     */
    @Schema(description = "净成交ROI")
    private Double netTransactionRoi;


    /**
     * 构建结构化数据
     *
     * @return 结构化数据对象
     */
    public StructuredData buildStructuredData() {
        List<WatchFlowStructure> watchFlowStructures = WatchFlowStructure.of(this.getWatchFlowList());
        List<PayFlowStructure> payFlowStructures = PayFlowStructure.of(this.getPayFlowList());
        PayUserPortraitData payUserPortraitData = PayUserPortraitData.of(this.getPayUserPortrait());
        WatchUserPortraitData watchUserPortraitData = WatchUserPortraitData.of(this.getWatchUserPortrait());
        return new StructuredData(watchFlowStructures, payFlowStructures, payUserPortraitData, watchUserPortraitData);
    }

    /**
     * 格式化字符串，
     *
     * @param lineBreak 换行符，如 "\n" 或 "<br>"
     *
     * @return 格式化后的字符串
     */
    public String formatStr(String lineBreak) {
        StringBuilder str = new StringBuilder("看板数据：");

        // 基础数据
        appendField(str, lineBreak, "总观看人次", formatNumber(getTotalWatchNum()));
        appendField(str, lineBreak, "平均在线人数", isValid(getAverageOnlineNum()) ? getAverageOnlineNum() : "-");
        appendField(str, lineBreak, "平均停留时间", formatDuration(getAverageResidenceTime()));
        appendField(str, lineBreak, "新增粉丝数", getIncrementFollowerCount() != null && getIncrementFollowerCount() >= 0 ? getIncrementFollowerCount() : null);
        appendField(str, lineBreak, "粉丝转化率", formatPercent(getConvertFanRate(), true));
        appendField(str, lineBreak, "互动率", formatPercent(getInteractionPercent(), null));
        str.append(lineBreak);

        // 销售数据区间
        appendOptimizedRangeField(str, lineBreak, "销售额", getVolumeStart(), getVolumeEnd(), null);
        appendOptimizedRangeField(str, lineBreak, "千次观看成交金额", getGpmStart(), getGpmEnd(), null);
        appendOptimizedRangeField(str, lineBreak, "销量", getPurchaseCountStart(), getPurchaseCountEnd(), null);
        appendOptimizedRangeField(str, lineBreak, "客单价", getCustomerUnitPriceStart() == null ? null : BigDecimal.valueOf(getCustomerUnitPriceStart()).setScale(1, RoundingMode.HALF_UP), getCustomerUnitPriceEnd() == null ? null : BigDecimal.valueOf(getCustomerUnitPriceEnd()).setScale(1, RoundingMode.HALF_UP), "元");
        appendOptimizedRangeField(str, lineBreak, "uv价值", getUvValueStart() == null ? null : BigDecimal.valueOf(getUvValueStart()).setScale(2, RoundingMode.HALF_UP), getUvValueEnd() == null ? null : BigDecimal.valueOf(getUvValueEnd()).setScale(2, RoundingMode.HALF_UP), null);
        appendOptimizedRangeField(str, lineBreak, "带货转换率", this.percent(getGoodsConvertRateStart(), true), this.percent(getGoodsConvertRateEnd(), true), "%");
        str.append(lineBreak);
        // 结构化数据
        StructuredData structuredData = buildStructuredData();
        // 流量结构对比
        this.appendFlowDataCompare(str, lineBreak, structuredData.getPayFlowStructures(), structuredData.getWatchFlowStructures(), false);
        str.append(lineBreak);
        // 用户画像对比
        this.appendPortraitDataCompare(str, lineBreak, structuredData.getPayUserPortraitData(), structuredData.getWatchUserPortraitData(), false);
        //appendStructuredFlowData(str, lineBreak, "流量结构", structuredData.getWatchFlowStructures(),  false);
        //appendStructuredFlowData(str, lineBreak, "成交流量结构", structuredData.getPayFlowStructures());
        //appendStructuredPortraitData(str, lineBreak, "成交用户画像", structuredData.getPayUserPortraitData());
        //appendStructuredPortraitData(str, lineBreak, "用户画像", structuredData.getWatchUserPortraitData(), false);

        return str.toString();
    }

    /**
     * 格式化字符串，用于AI问答
     * 包含人群画像和流量结构信息
     *
     * @return 格式化后的字符串
     */
    public String formatStrForAi() {
        if (this.dataStatus != AnchorVideoEnums.videoDataViewingStatus.SUCCESS.getCode() && this.dataStatus != AnchorVideoEnums.videoDataViewingStatus.SALES_DATA_IS_BEING_SUMMARIZED.getCode()) {
            return null;
        }
        String lineBreak = "\n";
        StringBuilder str = new StringBuilder();

        // 基础数据
        str.append("基础数据：");
        if (ObjectUtil.isNotNull(getTotalWatchNum()) && getTotalWatchNum() >= 0) {
            appendField(str, lineBreak, "  总观看人次", formatNumber(getTotalWatchNum()));
        }
        if (ObjectUtil.isNotNull(getAverageOnlineNum()) && getAverageOnlineNum() >= 0) {
            appendField(str, lineBreak, "  平均在线人数", isValid(getAverageOnlineNum()) ? getAverageOnlineNum() : "-");
        }
        if (ObjectUtil.isNotNull(getAverageResidenceTime()) && getAverageResidenceTime() >= 0) {
            appendField(str, lineBreak, "  平均停留时间", formatDuration(getAverageResidenceTime()));
        }
        if (ObjectUtil.isNotNull(getIncrementFollowerCount()) && getIncrementFollowerCount() >= 0) {
            appendField(str, lineBreak, "  新增粉丝数", getIncrementFollowerCount());
        }
        if (ObjectUtil.isNotNull(getConvertFanRate()) && getConvertFanRate() >= 0) {
            appendField(str, lineBreak, "  粉丝转化率", formatPercent(getConvertFanRate(), true));
        }
        if (ObjectUtil.isNotNull(getInteractionPercent()) && getInteractionPercent() >= 0) {
            appendField(str, lineBreak, "  互动率", formatPercent(getInteractionPercent(), null));
        }

        // 使用结构化数据
        StructuredData structuredData = buildStructuredData();

        if (this.dataStatus == AnchorVideoEnums.videoDataViewingStatus.SUCCESS.getCode() && ObjectUtil.equals(this.isTakeProduct, 1)) {
            // 销售数据区间
            if (ObjectUtil.isNotNull(getVolumeStart()) && ObjectUtil.isNotNull(getVolumeEnd()) && getVolumeStart() >= 0 && getVolumeEnd() >= 0) {
                appendOptimizedRangeField(str, lineBreak, "  销售额", getVolumeStart(), getVolumeEnd(), null);
            }
            if (ObjectUtil.isNotNull(getGpmStart()) && ObjectUtil.isNotNull(getGpmEnd()) && getGpmStart() >= 0 && getGpmEnd() >= 0) {
                appendOptimizedRangeField(str, lineBreak, "  千次观看成交金额", getGpmStart(), getGpmEnd(), null);
            }
            if (ObjectUtil.isNotNull(getPurchaseCountStart()) && ObjectUtil.isNotNull(getPurchaseCountEnd()) && getPurchaseCountStart() >= 0 && getPurchaseCountEnd() >= 0) {
                appendOptimizedRangeField(str, lineBreak, "  销量", getPurchaseCountStart(), getPurchaseCountEnd(), null);
            }
            if (ObjectUtil.isNotNull(getCustomerUnitPriceStart()) && ObjectUtil.isNotNull(getCustomerUnitPriceEnd()) && getCustomerUnitPriceStart() >= 0 && getCustomerUnitPriceEnd() >= 0) {
                appendOptimizedRangeField(str, lineBreak, "  客单价", formatNumber(getCustomerUnitPriceStart(), 2), formatNumber(getCustomerUnitPriceEnd(), 2), "元");
            }
            if (ObjectUtil.isNotNull(getUvValueStart()) && ObjectUtil.isNotNull(getUvValueEnd()) && getUvValueStart() >= 0 && getUvValueEnd() >= 0) {
                appendOptimizedRangeField(str, lineBreak, "  uv价值", formatNumber(getUvValueStart(), 2), formatNumber(getUvValueEnd(), 2), null);
            }
            if (ObjectUtil.isNotNull(getGoodsConvertRateStart()) && ObjectUtil.isNotNull(getGoodsConvertRateEnd()) && getGoodsConvertRateStart() >= 0 && getGoodsConvertRateEnd() >= 0) {
                appendOptimizedRangeField(str, lineBreak, "  带货转换率", this.percent(getGoodsConvertRateStart(), true), this.percent(getGoodsConvertRateEnd(), true), "%");
            }
        }

        // 看播人群画像
        if (ObjectUtil.isNotEmpty(structuredData) && ObjectUtil.isNotEmpty(structuredData.getWatchUserPortraitData())) {
            str.append(lineBreak).append("看播人群画像:");
            appendStructuredPortraitData(str, "\n", "", structuredData.getWatchUserPortraitData(), false);
        }

        // 成交人群画像
        if (ObjectUtil.isNotEmpty(structuredData) && ObjectUtil.isNotEmpty(structuredData.getPayUserPortraitData())) {
            str.append(lineBreak).append("成交人群画像:");
            appendStructuredPortraitData(str, "\n", "", structuredData.getPayUserPortraitData(), false);
        }

        // 看播流量结构
        if (ObjectUtil.isNotEmpty(structuredData) && ObjectUtil.isNotEmpty(structuredData.getWatchFlowStructures())) {
            str.append(lineBreak).append("看播流量结构:");
            appendStructuredFlowData(str, "\n  ", "", structuredData.getWatchFlowStructures(), false);
        }

        // 成交流量结构
        if (ObjectUtil.isNotEmpty(structuredData) && ObjectUtil.isNotEmpty(structuredData.getPayFlowStructures())) {
            str.append(lineBreak).append("成交流量结构:");
            appendStructuredFlowData(str, "\n  ", "", structuredData.getPayFlowStructures(), false);
        }

        return str.toString();
    }



    /**
     * 判断数据是否为空
     *
     * @return 数据是否为空
     */
    public boolean empty() {
        return this.dataStatus != AnchorVideoEnums.videoDataViewingStatus.SUCCESS.getCode() && this.dataStatus != AnchorVideoEnums.videoDataViewingStatus.SALES_DATA_IS_BEING_SUMMARIZED.getCode();
    }

    // ------------------ 工具方法 ------------------

    /**
     * 数值精确处理工具（返回Number原始类型）
     *
     * @param value 输入数值（支持所有Number子类）
     * @param scale 精确小数位数
     * @return 处理后的Number对象（自动去除尾零）
     * 创建日期：2025年8月22日
     */
    public static Number formatNumber(Number value, int scale) {
        if (value == null) return 0;

        BigDecimal bd = new BigDecimal(value.toString())
                .setScale(scale, RoundingMode.HALF_UP)
                .stripTrailingZeros();

        // 根据数值范围自动返回最匹配的Number子类
        if (scale == 0) {
            return bd.longValue() <= Integer.MAX_VALUE ?
                    bd.intValue() : bd.longValue();
        }
        return bd.doubleValue();
    }

    /**
     * 优化的区间字段添加方法
     *
     * @param str       字符串构建器
     * @param lineBreak 换行符
     * @param label     标签名
     * @param start     起始值
     * @param end       结束值
     * @param suffix    后缀
     */
    private void appendOptimizedRangeField(StringBuilder str, String lineBreak, String label, Number start, Number end, String suffix) {
        boolean startValid = isValid(start);
        boolean endValid = isValid(end);

        // 如果两个都没值，显示-
        if (!startValid && !endValid) {
            str.append(lineBreak).append(label).append(": -");
            return;
        }

        String formattedStart = startValid ? formatNumberForRange(start, suffix) : null;
        String formattedEnd = endValid ? formatNumberForRange(end, suffix) : null;

        str.append(lineBreak).append(label).append(": ");

        // 如果只有一个有值，直接显示有值的
        if (!startValid) {
            str.append(formattedEnd);
        } else if (!endValid) {
            str.append(formattedStart);
        } else {
            // 两个都有值，检查是否相等
            if (start.equals(end)) {
                str.append(formattedEnd); // 相等时只显示end值
            } else {
                str.append(formattedStart).append(" ~ ").append(formattedEnd);
            }
        }
    }

    /**
     * 为区间格式化数字
     *
     * @param number 数字
     * @param suffix 后缀
     *
     * @return 格式化后的字符串
     */
    private String formatNumberForRange(Number number, String suffix) {
        if ("%".equals(suffix)) {
            return formatPercent(number.doubleValue(), null);
        }
        //String formatted = formatNumber(number);
        return suffix != null ? number + suffix : number.toString();
    }

    /**
     * 添加结构化流量数据
     *
     * @param str            字符串构建器
     * @param lineBreak      换行符
     * @param label          标签名
     * @param flowStructures 流量结构列表
     * @param showSubFlow    是否显示子流量
     */
    private void appendStructuredFlowData(StringBuilder str, String lineBreak, String label, List<? extends BaseFlowStructure<?>> flowStructures, boolean showSubFlow) {
        if (flowStructures.isEmpty()) return;

        if (!label.isEmpty()) {
            str.append(lineBreak).append(label).append(":");
        }

        BigDecimal base = new BigDecimal("100");

        for (BaseFlowStructure<?> flowStructure : flowStructures) {
            str.append(lineBreak);
            str.append(flowStructure.getChannelName())
                .append(": ").append(BigDecimal.valueOf(flowStructure.getRatio()).multiply(base).setScale(2, RoundingMode.HALF_UP)).append("%");
            if (CollUtil.isNotEmpty(flowStructure.getSubFlow()) && showSubFlow) {
                str.append(lineBreak).append("  ");
                for (SubFlowData subFlowData : flowStructure.getSubFlow()) {
                    str.append(subFlowData.getChannelName())
                        .append(": ").append(BigDecimal.valueOf(flowStructure.getRatio()).multiply(base).setScale(2, RoundingMode.HALF_UP)).append("%").append(";");
                }
            }
        }
    }


    /**
     * 将支付流量结构和观看流量结构进行对比，并将结果追加到给定的StringBuilder中。
     *
     * @param str         用于拼接输出内容的StringBuilder对象
     * @param lineBreak   换行符字符串，用于格式化输出
     * @param pay         支付流量结构列表，可能为null或空
     * @param watch       观看流量结构列表，可能为null或空
     * @param showSubFlow 是否显示子流量结构的标识
     */
    private void appendFlowDataCompare(StringBuilder str, String lineBreak, List<PayFlowStructure> pay, List<WatchFlowStructure> watch, boolean showSubFlow) {
        // 如果两个列表都为空，则直接返回
        if (EmptyUtil.isEmpty(pay) && EmptyUtil.isEmpty(watch)) {
            return;
        }
        str.append(lineBreak).append("流量结构(成交｜看播):");
        BigDecimal base = new BigDecimal("100");

        // 构建支付和观看渠道名称到结构对象的映射
        Map<String, PayFlowStructure> payChannelMap = EmptyUtil.isEmpty(pay) ? Map.of() : pay.stream().collect(Collectors.toMap(PayFlowStructure::getChannelName, Function.identity(), FunctionUtil::mergeFirst));
        Map<String, WatchFlowStructure> watchChannelMap = EmptyUtil.isEmpty(watch) ? Map.of() : watch.stream().collect(Collectors.toMap(WatchFlowStructure::getChannelName, Function.identity(), FunctionUtil::mergeFirst));

        Map<String, Integer> channelNameSortMap = Map.of("直播推荐", 1, "关注", 2, "搜索", 3, "个人主页&店铺&橱窗", 4, "其他", 5, "短视频引流", 6, "抖音商城推荐", 7, "活动页", 9, "头条西瓜", 10, "付费流量", 11);

        // 合并两个列表中的所有唯一渠道名称
        List<String> channelNameList = Stream.concat(payChannelMap.keySet().stream(), watchChannelMap.keySet().stream()).distinct()
            .sorted(Comparator.comparing(o -> channelNameSortMap.getOrDefault(o, 100))).toList();

        // 定义函数式接口用于处理比率值的显示
        BiConsumer<StringBuilder, Double> appendRatio = (sb, ratio) -> {
            if (ratio == null) {
                sb.append("-");
            } else {
                sb.append(BigDecimal.valueOf(ratio).multiply(base).setScale(2, RoundingMode.HALF_UP)).append("%");
            }
        };

        // 定义函数式接口用于从结构对象中提取比率并显示
        BiConsumer<StringBuilder, BaseFlowStructure<?>> appendStructureRatio = (sb, structure) -> {
            if (structure == null) {
                sb.append("-");
            } else {
                appendRatio.accept(sb, structure.getRatio());
            }
        };
        AtomicInteger batch = new AtomicInteger(1);
        // 遍历所有渠道名称，输出其对应的支付与观看流量结构信息
        channelNameList.forEach(channelName -> {
            str.append(lineBreak).append("    ");
            str.append(batch.getAndIncrement()).append(".").append(" ");
            str.append(channelName)
                .append(": ");
            PayFlowStructure payFlowStructure = payChannelMap.get(channelName);
            appendStructureRatio.accept(str, payFlowStructure);
            str.append(" | ");
            WatchFlowStructure watchFlowStructure = watchChannelMap.get(channelName);
            appendStructureRatio.accept(str, watchFlowStructure);

            // 判断是否需要展示子流量结构
            boolean paySubFlowNotEmpty = payFlowStructure != null && CollUtil.isNotEmpty(payFlowStructure.getSubFlow());
            boolean watchSubFlowNotEmpty = watchFlowStructure != null && CollUtil.isNotEmpty(watchFlowStructure.getSubFlow());
            if (showSubFlow && (paySubFlowNotEmpty || watchSubFlowNotEmpty)) {
                str.append(lineBreak).append("        ");

                // 构建支付和观看子流量渠道名称到比率的映射
                Map<String, Double> payChannelSubFlowMap = paySubFlowNotEmpty ? payFlowStructure.getSubFlow().stream().collect(Collectors.toMap(SubFlowData::getChannelName, subFlow -> subFlow.getRatio() == null ? 0D : subFlow.getRatio(), (v1, v2) -> v1)) : Map.of();
                Map<String, Double> watchChannelSubFlowMap = watchSubFlowNotEmpty ? watchFlowStructure.getSubFlow().stream().collect(Collectors.toMap(SubFlowData::getChannelName, subFlow -> subFlow.getRatio() == null ? 0D : subFlow.getRatio(), (v1, v2) -> v1)) : Map.of();

                // 合并两个子流量列表中的所有唯一子渠道名称
                List<String> subChannelNameList = Stream.concat(payChannelSubFlowMap.keySet().stream(), watchChannelSubFlowMap.keySet().stream()).distinct().toList();

                // 遍历所有子渠道名称，输出其对应的支付与观看子流量比率
                subChannelNameList.forEach(subChannelName -> {
                    str.append(subChannelName).append(":");
                    str.append(" ");
                    Double paySubFlowRatio = payChannelSubFlowMap.get(subChannelName);
                    appendRatio.accept(str, paySubFlowRatio);
                    str.append(" | ");
                    Double watchSubFlowRatio = watchChannelSubFlowMap.get(subChannelName);
                    appendRatio.accept(str, watchSubFlowRatio);
                });
            }
        });
    }
    
    /**
     * 将支付用户画像和观看用户画像进行对比，并将结果追加到给定的StringBuilder中。
     *
     * @param str              用于拼接输出内容的StringBuilder对象
     * @param lineBreak        换行符字符串，用于格式化输出
     * @param payPortrait      支付用户画像数据，可能为null
     * @param watchPortrait    观看用户画像数据，可能为null
     * @param showRegion       是否显示地区画像的标识
     */
    private void appendPortraitDataCompare(StringBuilder str, String lineBreak, PayUserPortraitData payPortrait, WatchUserPortraitData watchPortrait, boolean showRegion) {
        // 如果两个画像数据都为空，则直接返回
        if (ObjectUtil.isEmpty(payPortrait) && ObjectUtil.isEmpty(watchPortrait)) {
            return;
        }
        
        str.append(lineBreak).append("用户画像(成交｜看播):");
        
        // 处理年龄画像对比
        List<PortraitItem> payAgePortrait = ObjectUtil.isNotEmpty(payPortrait) ? payPortrait.getAgePortrait() : null;
        List<PortraitItem> watchAgePortrait = ObjectUtil.isNotEmpty(watchPortrait) ? watchPortrait.getAgePortrait() : null;
        appendPortraitCategoryCompare(str, lineBreak, "年龄", payAgePortrait, watchAgePortrait);
        
        // 处理性别画像对比
        List<PortraitItem> payGenderPortrait = ObjectUtil.isNotEmpty(payPortrait) ? payPortrait.getGenderPortrait() : null;
        List<PortraitItem> watchGenderPortrait = ObjectUtil.isNotEmpty(watchPortrait) ? watchPortrait.getGenderPortrait() : null;
        appendPortraitCategoryCompare(str, lineBreak, "性别", payGenderPortrait, watchGenderPortrait);
        
        // 处理地区画像对比（如果需要显示）
        if (showRegion) {
            List<PortraitItem> payProvincePortrait = ObjectUtil.isNotEmpty(payPortrait) ? payPortrait.getProvincePortrait() : null;
            List<PortraitItem> watchProvincePortrait = ObjectUtil.isNotEmpty(watchPortrait) ? watchPortrait.getProvincePortrait() : null;
            appendPortraitCategoryCompare(str, lineBreak, "地区", payProvincePortrait, watchProvincePortrait);
        }
    }
    
    /**
     * 添加单个画像分类的对比数据
     *
     * @param str             字符串构建器
     * @param lineBreak       换行符
     * @param category        分类名称
     * @param payPortraits    支付画像列表
     * @param watchPortraits  观看画像列表
     */
    private void appendPortraitCategoryCompare(StringBuilder str, String lineBreak, String category, List<PortraitItem> payPortraits, List<PortraitItem> watchPortraits) {
        // 如果两个列表都为空，则直接返回
        if (CollUtil.isEmpty(payPortraits) && CollUtil.isEmpty(watchPortraits)) {
            return;
        }
        
        str.append(lineBreak).append("    ").append(category).append(": ");
        
        // 构建支付和观看画像标签到项目对象的映射
        Map<String, PortraitItem> payPortraitMap = CollUtil.isNotEmpty(payPortraits) ? payPortraits.stream().collect(Collectors.toMap(PortraitItem::getLabel, Function.identity(), FunctionUtil::mergeFirst)) : Map.of();
        Map<String, PortraitItem> watchPortraitMap = CollUtil.isNotEmpty(watchPortraits) ? watchPortraits.stream().collect(Collectors.toMap(PortraitItem::getLabel, Function.identity(), FunctionUtil::mergeFirst)) : Map.of();
        
        // 合并两个列表中的所有唯一标签
        List<String> labelList = Stream.concat(payPortraitMap.keySet().stream(), watchPortraitMap.keySet().stream()).distinct().sorted(String::compareTo).toList();
        
        // 定义函数式接口用于处理画像值的显示
        BiConsumer<StringBuilder, PortraitItem> appendPortraitValue = (sb, item) -> {
            if (item == null || item.getValue() == null) {
                sb.append("-");
            } else {
                sb.append(BigDecimal.valueOf(item.getValue()).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)).append("%");
            }
        };
        // 遍历所有标签，输出其对应的支付与观看画像信息
        for (int i = 0; i < labelList.size(); i++) {
            String label = labelList.get(i);
            str.append(lineBreak);
            str.append("        ").append(i + 1).append(". ");
            str.append(label).append(": ");
            PortraitItem payItem = payPortraitMap.get(label);
            appendPortraitValue.accept(str, payItem);
            str.append(" | ");
            PortraitItem watchItem = watchPortraitMap.get(label);
            appendPortraitValue.accept(str, watchItem);
        }
    }


    /**
     * 添加结构化画像数据
     *
     * @param str          字符串构建器
     * @param lineBreak    换行符
     * @param label        标签名
     * @param portraitData 画像数据
     * @param showRegion   是否显示地区
     */
    private void appendStructuredPortraitData(StringBuilder str, String lineBreak, String label, BaseUserPortraitStructure<?> portraitData, boolean showRegion) {
        if (portraitData == null) return;

        if (!label.isEmpty()) {
            str.append(lineBreak).append(label).append(": ");
        }

        appendPortraitCategory(str, lineBreak, "年龄", portraitData.getAgePortrait());
        appendPortraitCategory(str, lineBreak, "性别", portraitData.getGenderPortrait());
        if (showRegion) {
            appendPortraitCategory(str, lineBreak, "地区", portraitData.getProvincePortrait());
        }
    }


    /**
     * 添加单个画像分类数据
     *
     * @param str       字符串构建器
     * @param lineBreak 换行符
     * @param category  分类名称
     * @param portraits 画像列表
     */
    private void appendPortraitCategory(StringBuilder str, String lineBreak, String category, List<PortraitItem> portraits) {
        if (portraits == null || portraits.isEmpty()) return;
        BigDecimal base = new BigDecimal("100");
        str.append(lineBreak).append("  ").append(category).append(": ");
        for (int i = 0; i < portraits.size(); i++) {
            PortraitItem item = portraits.get(i);
            str.append(item.getLabel()).append("(").append(BigDecimal.valueOf(item.getValue()).multiply(base).setScale(2, RoundingMode.HALF_UP)).append("%)");
            if (i < portraits.size() - 1) {
                str.append(", ");
            }
        }
    }

    /**
     * 判断数值是否有效
     *
     * @param num 数值
     *
     * @return 是否有效
     */
    private boolean isValid(Number num) {
        return num != null && num.doubleValue() >= 0;
    }

    /**
     * 格式化数字，支持 k / w / 亿
     *
     * @param number 数字
     *
     * @return 格式化后的字符串
     */
    private String formatNumber(Number number) {
        if (!isValid(number)) {
            return "-";
        }
        if (number instanceof Double doubleValue) {
            return BigDecimal.valueOf(doubleValue).setScale(2, RoundingMode.HALF_UP).toString();
        }
        long value = number.longValue();
//        if (value >= 1_0000_0000) {
//            return NumberUtil.div(value, 1.0e8, 2) + "亿";
//        } else
        if (value >= 10_000) {
            return NumberUtil.div(value, 10000.0, 2) + "w";
        } else if (value >= 1000) {
            return NumberUtil.div(value, 1000.0, 2) + "k";
        } else {
            return String.valueOf(value);
        }
    }

    /**
     * 格式化百分比，保留两位小数
     *
     * @param rate     比率
     * @param multiply 是否乘以100 null 表示不处理 true 表示乘以100 false 表示除以100
     *
     * @return 格式化后的字符串
     */
    private String formatPercent(Double rate, Boolean multiply) {
        if (rate == null || rate < 0) {
            return "-";
        }
        if (multiply == null) {
            return BigDecimal.valueOf(rate).setScale(2, RoundingMode.HALF_UP) + "%";
        }
        return percent(rate, multiply).stripTrailingZeros().toPlainString() + "%";
    }

    private BigDecimal percent(Double rate, Boolean multiply) {
        if (rate == null || rate < 0) {
            return null;
        }
        if (multiply == null) {
            return new BigDecimal(rate);
        }
        BigDecimal base = BigDecimal.valueOf(100);
        BigDecimal value = BigDecimal.valueOf(rate);
        return multiply ? base.multiply(value) : value.divide(base, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 格式化时间（秒）为友好格式
     *
     * @param seconds 秒数
     *
     * @return 格式化后的字符串
     */
    private String formatDuration(Integer seconds) {
        if (seconds == null || seconds < 0) {
            return "-";
        }
        long sec = seconds;
        if (sec >= 7200) {
            long h = sec / 3600;
            long m = (sec % 3600) / 60;
            long s = sec % 60;
            return h + "小时" + m + "分" + s + "秒";
        } else if (sec >= 60) {
            long m = sec / 60;
            long s = sec % 60;
            return m + "分" + s + "秒";
        } else {
            return sec + "秒";
        }
    }

    /**
     * 添加字段输出，自动判断是否为空
     *
     * @param str       字符串构建器
     * @param lineBreak 换行符
     * @param label     标签名
     * @param value     值
     */
    private void appendField(StringBuilder str, String lineBreak, String label, Object value) {
        str.append(lineBreak).append(label).append(": ").append(value == null ? "-" : value);
    }


    @Getter
    @Setter
    public static class SubFlowData {

        private Double ratio;

        private String channelName;

    }

    /**
     * 流量结构基类
     *
     * @param <T> 子类类型
     */
    @Getter
    @Setter
    public static abstract class BaseFlowStructure<T extends BaseFlowStructure<T>> {

        /**
         * 流量渠道名称
         */
        protected String channelName;

        protected Double ratio;
        // 添加子流量结构支持
        protected List<SubFlowData> subFlow;

        public BaseFlowStructure(Double ratio) {
            this.ratio = ratio;
        }

        public BaseFlowStructure(Double ratio, List<SubFlowData> subFlow) {
            this.ratio = ratio;
            this.subFlow = subFlow;
        }

        /**
         * 解析流量结构数据
         *
         * @param jsonStr JSON字符串
         * @param clazz   类型
         * @param <T>     泛型
         *
         * @return 解析结果列表
         */
        public static <T> List<T> of(String jsonStr, Class<T> clazz) {
            if (!JSONUtil.isTypeJSONArray(jsonStr)) {
                return List.of();
            }
            try {
                return JSONUtil.parseArray(jsonStr).toList(clazz);
            } catch (Exception e) {
                log.error("解析流量结构失败, json:{}", jsonStr, e);
                return List.of();
            }
        }

    }

    /**
     * 看播流量结构
     */
    public static class WatchFlowStructure extends BaseFlowStructure<WatchFlowStructure> {
        // 添加无参构造函数供JSON反序列化使用
        public WatchFlowStructure() {
            super(null, null);
        }

        public WatchFlowStructure(Double ratio) {
            super(ratio);
        }

        public WatchFlowStructure(Double ratio, List<SubFlowData> subFlow) {
            super(ratio, subFlow);
        }

        public static List<WatchFlowStructure> of(String watchFlowList) {
            return BaseFlowStructure.of(watchFlowList, WatchFlowStructure.class);
        }
    }

    /**
     * 成交流量结构
     */
    public static class PayFlowStructure extends BaseFlowStructure<PayFlowStructure> {
        // 添加无参构造函数供JSON反序列化使用
        public PayFlowStructure() {
            super(null, null);
        }

        public PayFlowStructure(Double ratio) {
            super(ratio);
        }

        public PayFlowStructure(Double ratio, List<SubFlowData> subFlow) {
            super(ratio, subFlow);
        }

        public static List<PayFlowStructure> of(String payFlowList) {
            return BaseFlowStructure.of(payFlowList, PayFlowStructure.class);
        }
    }

    /**
     * 用户画像基类
     *
     * @param <T> 子类类型
     */
    @Getter
    @Setter
    public static abstract class BaseUserPortraitStructure<T extends BaseUserPortraitStructure<T>> {

        private List<PortraitItem> agePortrait;

        private List<PortraitItem> genderPortrait;

        private List<PortraitItem> provincePortrait;

        /**
         * 解析用户画像数据
         *
         * @param jsonStr JSON字符串
         * @param clazz   类型
         * @param <T>     泛型
         *
         * @return 解析结果列表
         */
        public static <T> T of(String jsonStr, Class<T> clazz) {
            if (!JSONUtil.isTypeJSONObject(jsonStr)) {
                return null;
            }
            try {
                return JSONUtil.toBean(jsonStr, clazz);
            } catch (Exception e) {
                log.error("解析用户画像失败", e);
                return null;
            }
        }
    }


    /**
     * 包含四个结构化类的内部类
     */
    @Getter
    @Setter
    public static class StructuredData {
        private List<WatchFlowStructure> watchFlowStructures;
        private List<PayFlowStructure> payFlowStructures;
        private PayUserPortraitData payUserPortraitData;
        private WatchUserPortraitData watchUserPortraitData;

        public StructuredData(List<WatchFlowStructure> watchFlowStructures,
                              List<PayFlowStructure> payFlowStructures,
                              PayUserPortraitData payUserPortraitData,
                              WatchUserPortraitData watchUserPortraitData) {
            this.watchFlowStructures = watchFlowStructures;
            this.payFlowStructures = payFlowStructures;
            this.payUserPortraitData = payUserPortraitData;
            this.watchUserPortraitData = watchUserPortraitData;
        }
    }

    /**
     * 用户画像项目
     */
    @Getter
    @Setter
    public static class PortraitItem implements Serializable {
        @Serial
        private static final long serialVersionUID = -4899463629040994134L;
        private String label;
        private Double value;

        public PortraitItem() {
        }

        public PortraitItem(String label, Double value) {
            this.label = label;
            this.value = value;
        }
    }

    /**
     * 成交用户画像数据
     */
    @Getter
    @Setter
    public static class PayUserPortraitData extends BaseUserPortraitStructure<PayUserPortraitData> {


        public static PayUserPortraitData of(String jsonStr) {
            return BaseUserPortraitStructure.of(jsonStr, PayUserPortraitData.class);
        }
    }

    /**
     * 看播用户画像数据
     */
    @Getter
    @Setter
    public static class WatchUserPortraitData extends BaseUserPortraitStructure<WatchUserPortraitData> {

        public static WatchUserPortraitData of(String jsonStr) {
            return BaseUserPortraitStructure.of(jsonStr, WatchUserPortraitData.class);
        }
    }
}