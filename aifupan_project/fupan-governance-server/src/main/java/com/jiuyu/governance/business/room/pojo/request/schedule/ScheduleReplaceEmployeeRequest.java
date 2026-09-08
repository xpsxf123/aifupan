package com.jiuyu.governance.business.room.pojo.request.schedule;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 批量更换排班人员请求
 *
 * @author HeHui
 * @date 2026-05-09
 */
@Getter
@Setter
public class ScheduleReplaceEmployeeRequest {

    /**
     * 排班ID
     */
    @NotNull(message = "缺少排班ID")
    private Long scheduleId;

    /**
     * 更换人员列表
     */
    @NotEmpty(message = "请选择要更换的人员")
    @Valid
    private List<ReplaceItem> items;

    /**
     * 单个人员更换项
     */
    @Getter
    @Setter
    public static class ReplaceItem {

        /**
         * 更换前员工ID，为空表示新增人员
         */
        private Long beforeEmployeeId;

        /**
         * 更换后员工ID
         */
        @NotNull(message = "请选择更换后的人员")
        private Long afterEmployeeId;

        /**
         * 更换后岗位ID
         */
        @NotNull(message = "请选择更换后的岗位")
        private Long afterPositionId;
    }
}
