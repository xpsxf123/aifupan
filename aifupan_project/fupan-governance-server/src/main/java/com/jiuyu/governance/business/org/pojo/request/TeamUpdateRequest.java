package com.jiuyu.governance.business.org.pojo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import java.util.List;

/**
 * 修改小组请求
 *
 * @author HeHui
 * @date 2026-03-18
 */
@Getter
@Setter
public class TeamUpdateRequest {

    /**
     * ID
     */
    @NotNull(message = "缺少小组信息")
    private Long id;

    /**
     * 所属部门ID
     */
    @NotNull(message = "请选择所属部门")
    private Long deptId;

    /**
     * 小组名称
     */
    @NotBlank(message = "请输入小组名称")
    @Length(max = 30, message = "小组名称最长不能超过30个字符")
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
