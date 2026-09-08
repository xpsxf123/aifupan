package com.jiuyu.replay.words.producer.impl;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.words.bo.oceanEngine.OceanEngineProcessBo;
import com.jiuyu.replay.words.producer.OceanEngineDataProducer;
import com.jiuyu.replay.words.vo.OnlineChartVo;
import com.jiuyu.replay.words.vo.chart.CurveData;
import com.jiuyu.replay.words.vo.chart.CurveDoubleData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 巨量引擎数据表ProducerImpl
 *
 * @author liaoxin
 * @date 2025-06-13
 */
@Component
@Slf4j
public class OceanEngineDataProducerImpl implements OceanEngineDataProducer {

    /**
     * 处理巨量引擎时序数据
     *
     * @param datas 巨量引擎文件数据
     * @param onlineChartVo       在线图表视图对象
     * @param videoId             视频ID
     */
    @Override
    public void processOceanEngineTimeSeriesData(List<OceanEngineProcessBo> datas, OnlineChartVo onlineChartVo, String videoId, Date startTime, Date endTime) {
        if (CollectionUtils.isEmpty(datas)) {
            return;
        }

        List<OceanEngineProcessBo> incrementalData = convertToIncrementalData(datas, onlineChartVo.getOnlineDataList(), startTime, endTime);
        // 将时点数据转换为增量数据
//        incrementalData = convertCumulativeToIncremental(incrementalData);
        if (CollectionUtils.isEmpty(incrementalData)) {
            return;
        }

        // 设置各类曲线数据
        onlineChartVo.setPayComboCntDataList(convertToChartData(incrementalData, OceanEngineProcessBo::getPayComboCnt, startTime));
        onlineChartVo.setPayAmtDataList(convertToChartDataDouble(incrementalData, OceanEngineProcessBo::getPayAmt, startTime));
        onlineChartVo.setFansClubJoinUcntDataList(convertToChartData(incrementalData, OceanEngineProcessBo::getFansClubJoinUcnt, startTime));
        onlineChartVo.setFollowAnchorUcntDataList(convertToChartData(incrementalData, OceanEngineProcessBo::getFollowAnchorUcnt, startTime));
        onlineChartVo.setQianchuanCostDataList(convertToChartDataBigDecimal(incrementalData, OceanEngineProcessBo::getQianchuanCost, startTime));
        onlineChartVo.setRefundAmtDataList(convertToChartDataBigDecimal(incrementalData, OceanEngineProcessBo::getRefundAmt, startTime));
        onlineChartVo.setNetTransactionRoiDataList(computeNetTransactionRoi(incrementalData, startTime));
    }

    /**
     * 将增量数据转换为图表数据格式
     *
     * @param incrementalData 增量数据列表
     * @param valueExtractor  值提取器函数
     * @return 图表数据列表
     */
    public List<CurveData> convertToChartData(List<OceanEngineProcessBo> incrementalData, Function<OceanEngineProcessBo, Integer> valueExtractor, Date startTime) {
        if (CollectionUtils.isEmpty(incrementalData)) {
            return Collections.emptyList();
        }

        return incrementalData.stream()
                .filter(data -> data != null && data.getGatherTimeStamp() != null)
                .map(data -> {
                    CurveData chartPoint = new CurveData();

                    // dateTime: 绝对时间戳 + 8小时时区偏移
                    long dateTime = data.getGatherTimeStamp();
                    chartPoint.setDateTime(dateTime);

                    // dateTimeNew: 相对于直播开始时间的毫秒偏移量
                    if (startTime != null) {
                        Date gatherTime = new Date(data.getGatherTimeStamp());
                        long dateTimeNew = DateUtil.between(gatherTime, startTime, DateUnit.MS);
                        chartPoint.setDateTimeNew(Math.abs(dateTimeNew));
                    } else {
                        chartPoint.setDateTimeNew(0L);
                    }

                    chartPoint.setValueNum(Optional.ofNullable(valueExtractor.apply(data)).orElse(0));
                    return chartPoint;
                })
                .collect(Collectors.toList());
    }

    /**
     * 将增量数据转换为图表数据格式（Double版本）
     */
    public List<CurveDoubleData> convertToChartDataDouble(List<OceanEngineProcessBo> incrementalData, Function<OceanEngineProcessBo, Integer> valueExtractor, Date startTime) {
        if (CollectionUtils.isEmpty(incrementalData)) {
            return Collections.emptyList();
        }

        return incrementalData.stream()
                .filter(data -> data != null && data.getGatherTimeStamp() != null)
                .map(data -> {
                    CurveDoubleData chartPoint = new CurveDoubleData();

                    long dateTime = data.getGatherTimeStamp();
                    chartPoint.setDateTime(dateTime);

                    if (startTime != null) {
                        Date gatherTime = new Date(data.getGatherTimeStamp());
                        long dateTimeNew = DateUtil.between(gatherTime, startTime, DateUnit.MS);
                        chartPoint.setDateTimeNew(Math.abs(dateTimeNew));
                    } else {
                        chartPoint.setDateTimeNew(0L);
                    }

                    chartPoint.setValueNum(Optional.ofNullable(valueExtractor.apply(data)).map(Integer::doubleValue).orElse(0.0));
                    return chartPoint;
                })
                .collect(Collectors.toList());
    }

    /**
     * 将增量数据转换为图表数据格式（BigDecimal版本）
     *
     * @param incrementalData 增量数据列表
     * @param valueExtractor  值提取器函数
     * @return 图表数据列表
     */
    public List<CurveDoubleData> convertToChartDataBigDecimal(List<OceanEngineProcessBo> incrementalData, Function<OceanEngineProcessBo, BigDecimal> valueExtractor, Date startTime) {
        if (CollectionUtils.isEmpty(incrementalData)) {
            return Collections.emptyList();
        }

        return incrementalData.stream()
                .filter(data -> data != null && data.getGatherTimeStamp() != null)
                .map(data -> {
                    CurveDoubleData chartPoint = new CurveDoubleData();

                    long dateTime = data.getGatherTimeStamp();
                    chartPoint.setDateTime(dateTime);

                    if (startTime != null) {
                        Date gatherTime = new Date(data.getGatherTimeStamp());
                        long dateTimeNew = DateUtil.between(gatherTime, startTime, DateUnit.MS);
                        chartPoint.setDateTimeNew(Math.abs(dateTimeNew));
                    } else {
                        chartPoint.setDateTimeNew(0L);
                    }

                    BigDecimal value = valueExtractor.apply(data);
                    chartPoint.setValueNum(value != null ? value.setScale(0, RoundingMode.HALF_UP).doubleValue() : 0.0);
                    return chartPoint;
                })
                .collect(Collectors.toList());
    }

    /**
     * 计算净成交ROI曲线：(payAmt - refundAmt) / qianchuanCost，保留2位小数
     */
    private List<CurveDoubleData> computeNetTransactionRoi(List<OceanEngineProcessBo> incrementalData, Date startTime) {
        if (CollectionUtils.isEmpty(incrementalData)) {
            return Collections.emptyList();
        }

        return incrementalData.stream()
                .filter(data -> data != null && data.getGatherTimeStamp() != null)
                .map(data -> {
                    CurveDoubleData point = new CurveDoubleData();

                    long dateTime = data.getGatherTimeStamp();
                    point.setDateTime(dateTime);

                    if (startTime != null) {
                        Date gatherTime = new Date(data.getGatherTimeStamp());
                        long dateTimeNew = DateUtil.between(gatherTime, startTime, DateUnit.MS);
                        point.setDateTimeNew(Math.abs(dateTimeNew));
                    } else {
                        point.setDateTimeNew(0L);
                    }

                    BigDecimal cost = data.getQianchuanCost() != null ? data.getQianchuanCost() : BigDecimal.ZERO;
                    int payAmt = data.getPayAmt() != null ? data.getPayAmt() : 0;
                    BigDecimal refund = data.getRefundAmt() != null ? data.getRefundAmt() : BigDecimal.ZERO;

                    BigDecimal netAmt = BigDecimal.valueOf(payAmt).subtract(refund);
                    if (cost.compareTo(BigDecimal.ZERO) > 0 && netAmt.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal roi = netAmt.divide(cost, 4, RoundingMode.HALF_UP);
                        point.setValueNum(roi.setScale(2, RoundingMode.HALF_UP).doubleValue());
                    } else {
                        point.setValueNum(0.0);
                    }

                    return point;
                })
                .collect(Collectors.toList());
    }

    /**
     * 按分钟汇总数据，取各数值字段的最大值
     *
     * @param dataList 原始数据列表
     * @return 按分钟汇总后的数据列表
     */
    public List<OceanEngineProcessBo> convertToIncrementalData(List<OceanEngineProcessBo> dataList, List<CurveData> onlineDataList, Date startTime, Date endTime) {
        if (CollectionUtils.isEmpty(dataList) || ObjectUtil.isEmpty(onlineDataList)) {
            return Collections.emptyList();
        }

        List<OceanEngineProcessBo> sorted = dataList.stream()
                .filter(data -> StringUtils.isNotBlank(data.getGatherDateTime()) && isWithinTimeRange(data, startTime, endTime))
                .sorted(Comparator.comparing(OceanEngineProcessBo::getGatherTimeStamp))
                .toList();
        if (CollectionUtils.isEmpty(sorted)) {
            return Collections.emptyList();
        }

        sorted = fixNonMonotonic(sorted);

        List<OceanEngineProcessBo> result = new ArrayList<>();
        long prevEnd = startTime.getTime();

        for (int i = 0; i < onlineDataList.size(); i++) {
            CurveData currentData = onlineDataList.get(i);
            long windowStart = currentData.getDateTime();
            long windowEnd = (i + 1 < onlineDataList.size())
                    ? onlineDataList.get(i + 1).getDateTime()
                    : endTime.getTime();

            List<OceanEngineProcessBo> bucket = new ArrayList<>();
            OceanEngineProcessBo baseline = findLastNotAfter(sorted, prevEnd);
            if (baseline != null) {
                bucket.add(baseline);
            }
            for (OceanEngineProcessBo d : sorted) {
                if (d.getGatherTimeStamp() > prevEnd && d.getGatherTimeStamp() <= windowEnd) {
                    bucket.add(d);
                }
            }

            result.add(buildIncrementalBo(bucket, windowStart));
            prevEnd = windowEnd;
        }
        return result;
    }

    /**
     * 查找时间戳 ≤ target 的最后一个数据点
     */
    private OceanEngineProcessBo findLastNotAfter(List<OceanEngineProcessBo> sorted, long target) {
        OceanEngineProcessBo last = null;
        for (OceanEngineProcessBo bo : sorted) {
            if (bo.getGatherTimeStamp() != null && bo.getGatherTimeStamp() <= target) {
                last = bo;
            } else if (bo.getGatherTimeStamp() != null && bo.getGatherTimeStamp() > target) {
                break;
            }
        }
        return last;
    }

    /**
     * 从数据点合集计算每字段增量：max - min，负值钳位 0
     */
    private OceanEngineProcessBo buildIncrementalBo(List<OceanEngineProcessBo> bucket, long gatherTimeStamp) {
        OceanEngineProcessBo result = new OceanEngineProcessBo();
        result.setGatherTimeStamp(gatherTimeStamp);
        if (CollectionUtils.isEmpty(bucket)) {
            return result;
        }

        result.setPayComboCnt(maxMinusMin(bucket, OceanEngineProcessBo::getPayComboCnt));
        result.setPayAmt(maxMinusMin(bucket, OceanEngineProcessBo::getPayAmt));
        result.setFansClubJoinUcnt(maxMinusMin(bucket, OceanEngineProcessBo::getFansClubJoinUcnt));
        result.setFollowAnchorUcnt(maxMinusMin(bucket, OceanEngineProcessBo::getFollowAnchorUcnt));
        result.setQianchuanCost(maxMinusMinBigDecimal(bucket, OceanEngineProcessBo::getQianchuanCost));
        result.setRefundAmt(maxMinusMinBigDecimal(bucket, OceanEngineProcessBo::getRefundAmt));
        return result;
    }

    private int maxMinusMin(List<OceanEngineProcessBo> bucket, Function<OceanEngineProcessBo, Integer> extractor) {
        List<Integer> values = bucket.stream().map(extractor).filter(Objects::nonNull).toList();
        if (values.isEmpty()) return 0;
        int max = values.stream().mapToInt(i -> i).max().orElse(0);
        int min = values.stream().mapToInt(i -> i).min().orElse(0);
        return Math.max(max - min, 0);
    }

    private BigDecimal maxMinusMinBigDecimal(List<OceanEngineProcessBo> bucket, Function<OceanEngineProcessBo, BigDecimal> extractor) {
        List<BigDecimal> values = bucket.stream().map(extractor).filter(Objects::nonNull).toList();
        if (values.isEmpty()) return BigDecimal.ZERO;
        BigDecimal max = values.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal min = values.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal delta = max.subtract(min);
        return delta.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : delta;
    }

    /**
     * 修复非单调字段：每个字段独立和上一条比较，若回退则沿用上一条的值
     */
    private List<OceanEngineProcessBo> fixNonMonotonic(List<OceanEngineProcessBo> sorted) {
        if (sorted.size() <= 1) {
            return sorted;
        }
        List<OceanEngineProcessBo> fixed = new ArrayList<>();
        fixed.add(sorted.get(0));
        for (int i = 1; i < sorted.size(); i++) {
            OceanEngineProcessBo cur = sorted.get(i);
            OceanEngineProcessBo prev = fixed.get(i - 1);
            OceanEngineProcessBo bo = new OceanEngineProcessBo();
            bo.setGatherTimeStamp(cur.getGatherTimeStamp());
            bo.setGatherDateTime(cur.getGatherDateTime());
            bo.setPayComboCnt(fixField(cur.getPayComboCnt(), prev.getPayComboCnt()));
            bo.setPayAmt(fixField(cur.getPayAmt(), prev.getPayAmt()));
            bo.setFansClubJoinUcnt(fixField(cur.getFansClubJoinUcnt(), prev.getFansClubJoinUcnt()));
            bo.setFollowAnchorUcnt(fixField(cur.getFollowAnchorUcnt(), prev.getFollowAnchorUcnt()));
            bo.setQianchuanCost(fixFieldBigDecimal(cur.getQianchuanCost(), prev.getQianchuanCost()));
            bo.setRefundAmt(fixFieldBigDecimal(cur.getRefundAmt(), prev.getRefundAmt()));
            fixed.add(bo);
        }
        return fixed;
    }

    private Integer fixField(Integer cur, Integer prev) {
        if (cur == null && prev == null) return null;
        if (cur == null) return prev;
        if (prev == null) return cur;
        return cur < prev ? prev : cur;
    }

    private BigDecimal fixFieldBigDecimal(BigDecimal cur, BigDecimal prev) {
        if (cur == null && prev == null) return null;
        if (cur == null) return prev;
        if (prev == null) return cur;
        return cur.compareTo(prev) < 0 ? prev : cur;
    }

    /**
     * 判断数据是否在指定时间范围内
     *
     * @param data      数据对象
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 是否在范围内
     */
    private boolean isWithinTimeRange(OceanEngineProcessBo data, Date startTime, Date endTime) {
        if (data.getGatherTimeStamp() == null || startTime == null || endTime == null) {
            return true;
        }
        // 如果时间戳是秒级的，需要转换为毫秒级
        long dataTime = data.getGatherTimeStamp();
        if (dataTime < 10000000000L) { // 小于这个值说明是秒级时间戳
            dataTime = dataTime * 1000;
        }
        return dataTime >= startTime.getTime() && dataTime <= endTime.getTime();
    }

}
