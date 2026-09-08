package com.jiuyu.governance.business.performance.utils;

import com.jiuyu.governance.business.performance.pojo.bo.OceanEngineProcessBo;
import com.jiuyu.governance.business.performance.pojo.base.BasePerformanceEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * 业绩计算工具类
 * <p>
 * 提供10分钟阈值规则 + 线性插值的关键时间点累加值计算，
 * 以及统一的区间业绩计算方法，供场次业绩和排班业绩共同使用。
 */
public class PerformanceCalculator {

    /**
     * 10分钟阈值（毫秒）
     */
    private static final long THRESHOLD_MILLIS = 600_000L;

    public static final ZoneId ZONE = ZoneId.systemDefault();

    private PerformanceCalculator() {
    }


    // ========================= 10分钟阈值 + 线性插值 =========================

    /**
     * 前后数据点对（一次扫描结果）
     */
    private record DataPointPair(OceanEngineProcessBo prev, OceanEngineProcessBo next, OceanEngineProcessBo current, long prevDiff, long nextDiff) {

        boolean isEmpty() {
            return prev == null && next == null;
        }

        boolean isCurrent() {
            return current != null;
        }
    }

    /**
     * 一次遍历找到目标时间点前后最近的数据点
     */
    private static DataPointPair findPrevNext(List<OceanEngineProcessBo> data, long targetEpochMilli) {
        OceanEngineProcessBo prev = null;
        OceanEngineProcessBo next = null;
        long prevDiff = Long.MAX_VALUE;
        long nextDiff = Long.MAX_VALUE;

        for (OceanEngineProcessBo bo : data) {
            if (bo.getGatherDateTime() == null) {
                continue;
            }
            long epoch = bo.getGatherDateTime().atZone(ZONE).toInstant().toEpochMilli();

            // 如果有时间刚好相等，直接返回
            if (epoch == targetEpochMilli){
                return new DataPointPair(null, null, bo, 0, 0);
            }
            long diff = epoch - targetEpochMilli;
            if (diff <= 0) {
                long absDiff = -diff;
                if (absDiff < prevDiff) {
                    prev = bo;
                    prevDiff = absDiff;
                }
            } else {
                if (diff < nextDiff) {
                    next = bo;
                    nextDiff = diff;
                }
            }
        }
        return new DataPointPair(prev, next, null, prevDiff, nextDiff);
    }

    /**
     * 从 DataPointPair 中提取并解析 Integer 值（10分钟阈值 + 线性插值）
     *
     * @param pair    前后数据点对
     * @param extractor 提取函数
     * @return 中间的值
     */
    private static Integer resolveInt(DataPointPair pair, Function<OceanEngineProcessBo, Integer> extractor) {
        if (pair.isEmpty()) {
            return null;
        }
        if (pair.prev == null) {
            return extractor.apply(pair.next);
        }
        if (pair.next == null) {
            return extractor.apply(pair.prev);
        }

        if (pair.prevDiff <= pair.nextDiff) {
            if (pair.prevDiff <= THRESHOLD_MILLIS) {
                return extractor.apply(pair.prev);
            }
        } else {
            if (pair.nextDiff <= THRESHOLD_MILLIS) {
                return extractor.apply(pair.next);
            }
        }

        Integer vPrev = extractor.apply(pair.prev);
        Integer vNext = extractor.apply(pair.next);
        if (vPrev == null || vNext == null) {
            return vPrev != null ? vPrev : vNext;
        }

        if (vNext.equals(vPrev)) {
            return vPrev;
        }

        return (int) (vPrev + ((double) ((vNext - vPrev) * pair.prevDiff) / (pair.prevDiff + pair.nextDiff)));
    }

    /**
     * 从 DataPointPair 中提取并解析 BigDecimal 值（10分钟阈值 + 线性插值）
     *
     * @param pair 前后数据点对
     * @param extractor 提取函数
     * @return 中间的值
     */
    private static BigDecimal resolveBigDecimal(DataPointPair pair, Function<OceanEngineProcessBo, BigDecimal> extractor) {
        if (pair.isEmpty()) {
            return null;
        }
        if (pair.prev == null) {
            return extractor.apply(pair.next);
        }
        if (pair.next == null) {
            return extractor.apply(pair.prev);
        }

        if (pair.prevDiff <= pair.nextDiff) {
            if (pair.prevDiff <= THRESHOLD_MILLIS) {
                return extractor.apply(pair.prev);
            }
        } else {
            if (pair.nextDiff <= THRESHOLD_MILLIS) {
                return extractor.apply(pair.next);
            }
        }

        BigDecimal vPrev = extractor.apply(pair.prev);
        BigDecimal vNext = extractor.apply(pair.next);

        if (vPrev == null || vNext == null) {
            return vPrev != null ? vPrev : vNext;
        }

        if (vNext.equals(vPrev)) {
            return vPrev;
        }

        return vPrev.add(vNext.subtract(vPrev).multiply(BigDecimal.valueOf(pair.prevDiff))
                .divide(BigDecimal.valueOf(pair.prevDiff + pair.nextDiff), 0, RoundingMode.HALF_UP));
    }

    /**
     * 从 DataPointPair 中提取并解析 ClickPaymentRate 值（10分钟阈值 + 线性插值）
     * @param pair 前后数据点对
     * @param payComboCnt 区间订单量
     * @return 中间的值
     */
    private static BigDecimal resolveBigDecimalClickPaymentRate(DataPointPair pair, Integer payComboCnt) {

        if (pair == null || payComboCnt == null || pair.next == null || pair.prev == null){
            return null;
        }
        // 区间点击-成交率--区间订单量/区间点击量(区间点击量=结束单量/结束点击成交率-开始单量/开始点击成交率)
        BasePerformanceEntity temp = new BasePerformanceEntity();
        temp.setPerformanceClickPaymentRate(payComboCnt,
                pair.prev.getClickPaymentRate(), pair.prev.getPayComboCnt(),
                pair.next.getClickPaymentRate(), pair.next.getPayComboCnt()
        );
        return temp.getClickPaymentRate();
    }

    /**
     * 从 DataPointPair 中提取并解析 ClickPaymentRate 值（10分钟阈值 + 线性插值）
     * @param pair 前后数据点对
     * @param interactionRate 区间观看人数
     * @return 中间的值
     */
    private static BigDecimal resolveBigDecimalInteractionRate(DataPointPair pair, Integer interactionRate) {

        if (pair == null || interactionRate == null || pair.next == null || pair.prev == null){
            return null;
        }
        // 区间互动率--区间互动次数(结束观看人数*结束互动率-开始观看人数*开始互动率)/区间观看人数
        BasePerformanceEntity temp = new BasePerformanceEntity();
        temp.setPerformanceInteractionRate(
                pair.prev.getInteractionRate(), pair.prev.getViewCount(),
                pair.next.getInteractionRate(), pair.next.getViewCount(),
                interactionRate
        );
        return temp.getInteractionRate();
    }


    // ========================= 累加值计算 =========================

    /**
     * 计算目标时间点所有累加型指标的值（10分钟阈值 + 线性插值）
     * <p>
     * 一次扫描 processData，同时计算所有 8 个累加型指标。
     *
     * @param data             过程数据（按时间升序）
     * @param targetEpochMilli 目标时间戳（毫秒）
     * @return 目标时间点的累加值实体
     */
    public static OceanEngineProcessBo getAccumulatedValuesAtTimestamp(List<OceanEngineProcessBo> data, long targetEpochMilli) {
        // 找到目标时间点前后最近的数据点
        DataPointPair pair = findPrevNext(data, targetEpochMilli);
        if (pair.isCurrent()){
            return pair.current;
        }

        Integer viewCount = resolveInt(pair, OceanEngineProcessBo::getViewCount);
        Integer payComboCnt = resolveInt(pair, OceanEngineProcessBo::getPayComboCnt);
        return OceanEngineProcessBo.builder()
                .gatherDateTime(Instant.ofEpochMilli(targetEpochMilli).atZone(ZONE).toLocalDateTime())
                .exposureCount(resolveInt(pair, OceanEngineProcessBo::getExposureCount))
                .viewCount(viewCount)
                .onlineCount(resolveInt(pair, OceanEngineProcessBo::getOnlineCount))
                .followCount(resolveInt(pair, OceanEngineProcessBo::getFollowCount))
                .fansClubJoinUcnt(resolveInt(pair, OceanEngineProcessBo::getFansClubJoinUcnt))
                .salesRevenue(resolveBigDecimal(pair, OceanEngineProcessBo::getSalesRevenue))
                .refund(resolveBigDecimal(pair, OceanEngineProcessBo::getRefund))
                .investment(resolveBigDecimal(pair, OceanEngineProcessBo::getInvestment))
                .refundQuantity(resolveInt(pair, OceanEngineProcessBo::getRefundQuantity))
                .payComboCnt(payComboCnt)
                .clickPaymentRate(resolveBigDecimalClickPaymentRate(pair, payComboCnt))
                .interactionRate(resolveBigDecimalInteractionRate(pair, viewCount))
                .build();
    }

    // ========================= 区间业绩计算 =========================

    /**
     * 计算区间业绩（累加值差值 + maxOnline + 衍生指标 + 率值）
     *
     * @param processData 过程数据（按时间升序）
     * @param startMs     区间开始时间（epochMilli）
     * @param endMs       区间结束时间（epochMilli）
     * @param startValue  区间开始累加值（由调用方决定如何获取）
     * @param endValue    区间结束累加值（由调用方决定如何获取）
     * @return 区间业绩实体（包含基础指标、衍生指标、率值），无数据时返回null
     */
    public static BasePerformanceEntity calculateIntervalPerformance(
            List<OceanEngineProcessBo> processData,
            long startMs, long endMs,
            BasePerformanceEntity startValue, BasePerformanceEntity endValue) {

        if (startValue == null || endValue == null) {
            return null;
        }

        // 计算各指标差值
        BasePerformanceEntity interval = new BasePerformanceEntity();
        interval.setViewCount(MetricsUtil.diffInt(endValue.getViewCount(), startValue.getViewCount()));
        interval.setSalesRevenue(MetricsUtil.subtract(endValue.getSalesRevenue(), startValue.getSalesRevenue()));
        // 区间退款 = 结束累计退款 - 开始累计退款
        // 结束累计退款为 null 时区间退款未知（保守返回 null）；开始累计退款为 null 时按 0 处理（直播初期尚未产生退款）
        BigDecimal endRefund = endValue.getRefund();
        BigDecimal startRefund = startValue.getRefund();
        interval.setRefund(endRefund == null ? null : endRefund.subtract(startRefund == null ? BigDecimal.ZERO : startRefund));
        interval.setInvestment(MetricsUtil.subtract(endValue.getInvestment(), startValue.getInvestment()));
        interval.setExposureCount(MetricsUtil.diffInt(endValue.getExposureCount(), startValue.getExposureCount()));
        interval.setFollowCount(MetricsUtil.diffInt(endValue.getFollowCount(), startValue.getFollowCount()));
        interval.setRefundQuantity(MetricsUtil.diffInt(endValue.getRefundQuantity(), startValue.getRefundQuantity()));
        interval.setPayComboCnt(MetricsUtil.diffInt(endValue.getPayComboCnt(), startValue.getPayComboCnt()));

        // maxOnline：取区间时间范围内的最大在线人数
        interval.setMaxOnline(processData.stream()
                .filter(bo -> {
                    if (bo.getGatherDateTime() == null) {
                        return false;
                    }
                    long epoch = bo.getGatherDateTime().atZone(ZONE).toInstant().toEpochMilli();
                    return epoch >= startMs && epoch <= endMs;
                })
                .map(OceanEngineProcessBo::getOnlineCount)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null));

        // 计算衍生指标
        interval.setPerformanceCalculate(interval);

        // 区间点击-成交率和互动率：用最近实际数据点计算
        interval.setPerformanceClickPaymentRate(
                interval.getPayComboCnt(),
                startValue.getClickPaymentRate(), startValue.getPayComboCnt(),
                endValue.getClickPaymentRate(), endValue.getPayComboCnt());

        interval.setPerformanceInteractionRate(
                startValue.getInteractionRate(), startValue.getViewCount(),
                endValue.getInteractionRate(), endValue.getViewCount(),
                interval.getViewCount());

        return interval;
    }
}
