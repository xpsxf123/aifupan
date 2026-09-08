package com.jiuyu.governance.business.org.pojo.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 岗位信息
 *
 * @author HeHui
 * @date 2026-03-23 10:18
 */
@Getter
@Setter
public class PositionResponse {

    /**
     * ID
     */
    private Long id;

    /**
     * 岗位名称
     */
    private String name;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 是否默认岗位
     */
    private Boolean isDefault;

    /**
     * 关联人数
     */
    private Long employeeCount;


    /**
     * 更新时间
     */
    private LocalDateTime updatedDate;

}
