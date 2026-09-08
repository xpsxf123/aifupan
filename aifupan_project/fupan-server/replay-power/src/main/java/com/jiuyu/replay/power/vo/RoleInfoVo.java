package com.jiuyu.replay.power.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "角色信息")
public class RoleInfoVo extends RoleVo {

    /**
     * 角色拥有的菜单列表
     */
    private List<MenuVo> menuList;
    /**
     * 菜单树形结构
     */
    private List<MenuTreeVo> menuTreeList;
}
