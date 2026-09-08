package com.jiuyu.replay.power.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 部门列表VO
 *
 * @author jxy
 * @date 2024-07-08
 */
@Data
@Schema(description = "部门列表")
public class DeptListVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "部门名称")
    private String name;

    @Schema(description = "父ID")
    private Long parentId;

    @Schema(description = "子部门")
    private List<DeptListVo> children;
}
