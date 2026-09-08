package com.jiuyu.governance.business.room.pojo.request.schedule;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * 批量排班配置请求
 *
 * @author HeHui
 * @date 2026-03-26
 */
@Getter
@Setter
public class ScheduleBatchRequest {

    /**
     * 班次循环（1-7 代表周一到周日）
     */
    @NotEmpty(message = "请选择班次循环")
    @Size(min = 1, max = 7, message = "班次循环范围1-7")
    private List<Integer> cycleDays;

    /**
     * 排班结束日期
     */
    @NotNull(message = "请选择排班结束日期")
    @Future(message = "排班结束日期不能早于今天")
    private LocalDate endDate;
}
