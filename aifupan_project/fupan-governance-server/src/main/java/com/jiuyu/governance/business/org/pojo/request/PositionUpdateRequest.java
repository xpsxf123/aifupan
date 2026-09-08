package com.jiuyu.governance.business.org.pojo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 岗位添加请求
 *
 * @author HeHui
 * @date 2026-03-23 10:18
 */
@Getter
@Setter
public class PositionUpdateRequest {

    /**
     * 岗位ID
     */
    @NotNull(message = "岗位ID不能为空")
    private Long id;

    /**
     * 岗位名称
     */
    @NotBlank(message = "请输入岗位名称")
    @Size(max = 100, message = "岗位名称最大长度要小于 100")
    private String name;

    /**
     * 岗位排序
     */
    //@NotNull(message = "请输入岗位排序")
    private Integer sort;
}
