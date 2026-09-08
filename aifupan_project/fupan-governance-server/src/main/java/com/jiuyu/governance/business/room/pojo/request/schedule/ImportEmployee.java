package com.jiuyu.governance.business.room.pojo.request.schedule;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 导入排班 - 排班人员
 *
 * @author HeHui
 * @date 2026-08-20
 */
@Getter
@Setter
public class ImportEmployee {

    /**
     * 员工ID
     */
    @NotNull(message = "请选择员工")
    private Long employeeId;

    /**
     * 岗位ID
     */
    @NotNull(message = "请选择岗位")
    private Long positionId;
}
