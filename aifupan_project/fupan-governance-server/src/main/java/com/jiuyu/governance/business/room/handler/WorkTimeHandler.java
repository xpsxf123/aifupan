package com.jiuyu.governance.business.room.handler;

import com.jiuyu.framework.shandard.Range;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 工作时间处理器
 *
 * @author HeHui
 * @date 2026-04-02 18:26
 */
public class WorkTimeHandler {

    private WorkTimeHandler() {
    }



    /**
     * 时间解析
     * <p>
     * 支持的时间格式：
     * - 4位数字：HHmm，如 1230 -> 12:30
     * - 3位数字：Hmm，如 930 -> 09:30
     * - 2位数字：HH，如 20 -> 20:00
     * - 1位数字：H，如 9 -> 09:00
     * </p>
     *
     * @param time 时间整数，格式为 HHmm 或 Hmm 或 HH 或 H
     *
     * @return LocalTime 解析后的时间对象
     */
    public static LocalTime parseTime(Integer time) {
        if (time == null) {
            return null;
        }
        String str = String.format("%04d", time); // 统一格式化为4位，不足补0
        int hour = Integer.parseInt(str.substring(0, 2));
        int minute = Integer.parseInt(str.substring(2, 4));
        return LocalTime.of(hour, minute);
    }

    /**
     * 时间格式化
     */
    public static Integer formatTime(LocalTime time) {
        if (time == null) {
            return null;
        }
        return Integer.valueOf(time.getHour() + "" + (time.getMinute() < 10 ? "0" + time.getMinute() : time.getMinute()));
    }



    /**
     * 判断两个时间段是否重叠 - 首尾相连版
     * <p>
     * 假设 A = [start1, end1), B = [start2, end2)
     * 重叠条件：max(start1, start2) < min(end1, end2)
     * </p>
     *
     * @param r1 第一个时间段
     * @param r2 第二个时间段
     *
     * @return true 表示重叠，false 表示不重叠
     */
    public static boolean isOverlap(Range<LocalDateTime> r1, Range<LocalDateTime> r2) {
        LocalDateTime start1 = r1.getStart();
        LocalDateTime end1 = r1.getEnd();
        LocalDateTime start2 = r2.getStart();
        LocalDateTime end2 = r2.getEnd();

        // 由于结束时间是闭区间（或可能跨天等情况），这里用严格小于来判断
        LocalDateTime maxStart = start1.isAfter(start2) ? start1 : start2;
        LocalDateTime minEnd = end1.isBefore(end2) ? end1 : end2;

        return maxStart.isBefore(minEnd);
    }

    /**
     * 判断两个时间段是否重叠（包含边界相接的情况）
     * <p>
     * 注意：当前实现中，如果两个时间段刚好首尾相接（如 10:00-12:00 和 12:00-14:00），
     * 不会被判定为冲突。如果需要将这种情况视为冲突，请使用此方法。
     * </p>
     *
     * @param r1 第一个时间段
     * @param r2 第二个时间段
     *
     * @return true 表示重叠（包含边界相接），false 表示不重叠
     */
    public static boolean isOverlapWithBoundary(Range<LocalDateTime> r1, Range<LocalDateTime> r2) {
        LocalDateTime start1 = r1.getStart();
        LocalDateTime end1 = r1.getEnd();
        LocalDateTime start2 = r2.getStart();
        LocalDateTime end2 = r2.getEnd();

        // 使用 <= 来包含边界相接的情况
        LocalDateTime maxStart = start1.isAfter(start2) ? start1 : start2;
        LocalDateTime minEnd = end1.isBefore(end2) ? end1 : end2;

        return !maxStart.isAfter(minEnd);
    }
}
