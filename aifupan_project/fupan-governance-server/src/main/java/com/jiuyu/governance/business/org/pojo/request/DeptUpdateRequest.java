package com.jiuyu.governance.business.org.pojo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import java.util.List;

/**
 * 修改部门请求
 *
 * @author HeHui
 * @date 2026-03-18
 */
@Getter
@Setter
public class DeptUpdateRequest {

    /**
     * ID
     */
    @NotNull(message = "缺少部门信息")
    private Long id;

    /**
     * 所属公司ID
     */
    @NotNull(message = "请选择所属公司")
    private Long companyId;

    /**
     * 部门名称
     */
    @NotBlank(message = "请输入部门名称")
    @Length(max = 40, message = "部门名称最长不能超过40个字符")
    private String name;

    /**
     * 排序
     */
    //@NotNull(message = "请设置排序值")
    private Integer sort;

    /**
     * 管理员用户ID列表
     */
    private List<Long> managerUserIds;

}
