package com.jiuyu.governance.business.performance.utils;

import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.jiuyu.governance.business.performance.pojo.bo.PeriodPerformanceBO;
import com.jiuyu.governance.business.performance.pojo.response.PeriodData;
import com.jiuyu.governance.business.performance.pojo.response.PerformancePeriodStatsResponse;
import com.jiuyu.governance.business.performance.pojo.response.SessionStats;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.function.Function;

/**
 * 数据处理工具类
 * @author ：lujie
 * @date ：2026/3/30 14:50
 */
public class DataUtil {

    /**
     * 六时段日期范围信息
     */
    public record PeriodDateRange(
            LocalDate today,
            LocalDate yesterday,
            LocalDate thisWeekStart,
            LocalDate lastWeekStart,
            LocalDate lastWeekEnd,
            LocalDate thisMonthStart,
            LocalDate lastMonthStart,
            LocalDate lastMonthEnd
    ) {}

    /**
     * 计算六个时段的日期范围
     * 包括：今日、昨日、本周、上周、本月、上月
     *
     * @return 日期范围信息
     */
    public static PeriodDateRange calculatePeriodDateRange() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate thisWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastWeekStart = thisWeekStart.minusWeeks(1);
        LocalDate lastWeekEnd = lastWeekStart.plusDays(6);
        LocalDate thisMonthStart = today.withDayOfMonth(1);
        LocalDate lastMonthStart = thisMonthStart.minusMonths(1);
        LocalDate lastMonthEnd = lastMonthStart.with(TemporalAdjusters.lastDayOfMonth());

        return new PeriodDateRange(
                today, yesterday,
                thisWeekStart, lastWeekStart, lastWeekEnd,
                thisMonthStart, lastMonthStart, lastMonthEnd
        );
    }

    /**
     * 将 null 的 PeriodPerformanceBO 转换为空对象
     *
     * @param bo 业绩数据
     * @return 非空的 PeriodPerformanceBO
     */
    public static PeriodPerformanceBO nullToEmpty(PeriodPerformanceBO bo) {
        return bo != null ? bo : new PeriodPerformanceBO();
    }

    /**
     * 根据六个时段的业绩数据构建 PerformancePeriodStatsResponse
     *
     * @param todayBo     今日数据
     * @param yesterdayBo 昨日数据
     * @param thisWeekBo  本周数据
     * @param lastWeekBo  上周数据
     * @param thisMonthBo 本月数据
     * @param lastMonthBo 上月数据
     * @return 业绩时段统计响应
     */
    public static PerformancePeriodStatsResponse buildPeriodStatsResponse(
            PeriodPerformanceBO todayBo,
            PeriodPerformanceBO yesterdayBo,
            PeriodPerformanceBO thisWeekBo,
            PeriodPerformanceBO lastWeekBo,
            PeriodPerformanceBO thisMonthBo,
            PeriodPerformanceBO lastMonthBo) {

        // 处理空值
        todayBo = nullToEmpty(todayBo);
        yesterdayBo = nullToEmpty(yesterdayBo);
        thisWeekBo = nullToEmpty(thisWeekBo);
        lastWeekBo = nullToEmpty(lastWeekBo);
        thisMonthBo = nullToEmpty(thisMonthBo);
        lastMonthBo = nullToEmpty(lastMonthBo);

        return PerformancePeriodStatsResponse.builder()
                .sessionStats(PeriodData.<SessionStats>builder()
                        .today(toSessionStats(todayBo))
                        .yesterday(toSessionStats(yesterdayBo))
                        .thisWeek(toSessionStats(thisWeekBo))
                        .lastWeek(toSessionStats(lastWeekBo))
                        .thisMonth(toSessionStats(thisMonthBo))
                        .lastMonth(toSessionStats(lastMonthBo))
                        .build())
                .viewCount(buildPeriodData(PeriodPerformanceBO::getViewCount,
                        todayBo, yesterdayBo, thisWeekBo, lastWeekBo, thisMonthBo, lastMonthBo))
                .salesRevenue(buildPeriodData(PeriodPerformanceBO::getSalesRevenue,
                        todayBo, yesterdayBo, thisWeekBo, lastWeekBo, thisMonthBo, lastMonthBo))
                .refund(buildPeriodData(PeriodPerformanceBO::getRefund,
                        todayBo, yesterdayBo, thisWeekBo, lastWeekBo, thisMonthBo, lastMonthBo))
                .netSales(buildPeriodData(PeriodPerformanceBO::getNetSales,
                        todayBo, yesterdayBo, thisWeekBo, lastWeekBo, thisMonthBo, lastMonthBo))
                .investment(buildPeriodData(PeriodPerformanceBO::getInvestment,
                        todayBo, yesterdayBo, thisWeekBo, lastWeekBo, thisMonthBo, lastMonthBo))
                .build();
    }

    /**
     * 将PeriodPerformanceBO转换为SessionStats
     */
    public static SessionStats toSessionStats(PeriodPerformanceBO bo) {
        if (bo == null) {
            bo = new PeriodPerformanceBO();
        }
        return SessionStats.builder()
                .count(bo.getSessionCount())
                .duration(bo.getDuration())
                .build();
    }

    /**
     * 通用构建 PeriodData 的方法
     *
     * @param extractor  从 StatsBo 中提取指定字段的函数
     * @param todayBo    今日数据
     * @param yesterdayBo 昨日数据
     * @param thisWeekBo  本周数据
     * @param lastWeekBo  上周数据
     * @param thisMonthBo 本月数据
     * @param lastMonthBo 上月数据
     * @param <T>         字段类型
     * @return PeriodData 对象
     */
    public static <T> PeriodData<T> buildPeriodData(Function<PeriodPerformanceBO, T> extractor,
                                                     PeriodPerformanceBO todayBo, PeriodPerformanceBO yesterdayBo,
                                                     PeriodPerformanceBO thisWeekBo, PeriodPerformanceBO lastWeekBo,
                                                     PeriodPerformanceBO thisMonthBo, PeriodPerformanceBO lastMonthBo) {
        return PeriodData.<T>builder()
                .today(extractor.apply(todayBo))
                .yesterday(extractor.apply(yesterdayBo))
                .thisWeek(extractor.apply(thisWeekBo))
                .lastWeek(extractor.apply(lastWeekBo))
                .thisMonth(extractor.apply(thisMonthBo))
                .lastMonth(extractor.apply(lastMonthBo))
                .build();
    }

    /**
     * 计算直播时段
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 直播时段 格式为 "HH:mm-HH:mm\nXX小时XX分钟"
     */
    public static String liveTimeSlot(LocalDateTime start, LocalDateTime end) {
        if (ObjUtil.isNotEmpty(start) && ObjUtil.isNotEmpty(end)) {
            String timeSlot = DateUtil.format(start, "HH:mm") + "-" + DateUtil.format(end, "HH:mm");
            timeSlot += "\n" + DateUtil.formatBetween(new DateTime(start), new DateTime(end), BetweenFormatter.Level.HOUR);
            return timeSlot;
        }
        return "";
    }

    /**
     * 构建会话字符串
     *
     * @param scheduleCount  会话次数
     * @param liveDurationMinutes 直播时长（分钟）
     * @return 会话字符串 格式为 "XX(XX小时)"
     */
    public static String sessionString(Integer scheduleCount, Long liveDurationMinutes){
        return StrUtil.format("{}({}小时)",
                ObjUtil.defaultIfNull(scheduleCount, 0),
                Math.round((double) ObjUtil.defaultIfNull(liveDurationMinutes, 0L) * DateUnit.MINUTE.getMillis() / DateUnit.HOUR.getMillis()));
    }
}
