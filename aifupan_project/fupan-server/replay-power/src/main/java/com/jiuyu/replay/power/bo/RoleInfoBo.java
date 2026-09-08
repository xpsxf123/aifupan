package com.jiuyu.replay.power.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class RoleInfoBo {

    /**
     * 菜单id列表
     */
    private List<Long> roleIdList;
    /**
     * ID
     */
    @Schema(description = "ID")
    private Long id;
    /**
     * 角色名
     */
    @Schema(description = "角色名")
    private String name;
    /**
     * 角色级别
     */
    @Schema(description = "角色级别")
    private String level;
}
