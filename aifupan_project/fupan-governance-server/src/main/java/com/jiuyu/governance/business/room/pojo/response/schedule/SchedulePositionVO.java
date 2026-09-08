package com.jiuyu.governance.business.room.pojo.response.schedule;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 排班岗位VO
 *
 * @author HeHui
 * @date 2026-03-26
 */
@Getter
@Setter
public class SchedulePositionVO {

    /**
     * 岗位ID
     */
    private Long positionId;

    /**
     * 岗位名称
     */
    private String positionName;


    /**
     * 是否是主播岗位
     */
    private Boolean anchorPosition;

    /**
     * 岗位下的员工列表
     */
    private List<ScheduleEmployeeVO> employees;
}
