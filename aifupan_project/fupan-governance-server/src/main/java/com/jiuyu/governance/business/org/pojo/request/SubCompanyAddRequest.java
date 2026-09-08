package com.jiuyu.governance.business.org.pojo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import java.util.List;

/**
 * 新增子公司请求
 *
 * @author HeHui
 * @date 2026-03-18
 */
@Getter
@Setter
public class SubCompanyAddRequest {

    /**
     * 公司名称
     */
    @NotBlank(message = "请输入公司名称")
    @Length(max = 50, message = "公司名称最长不能超过50个字符")
    private String name;

    /**
     * 排序
     */
    //@NotNull(message = "请设置排序值")
    private Integer sort = 1;


    /**
     * 管理员用户ID列表
     */
    private List<Long> managerUserIds;

}
