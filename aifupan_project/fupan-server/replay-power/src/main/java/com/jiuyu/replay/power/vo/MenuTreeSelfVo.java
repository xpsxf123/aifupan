package com.jiuyu.replay.power.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "菜单列表")
public class MenuTreeSelfVo {

    /**
     * 菜单列表
     */
    @Schema(description = "菜单列表")
    private List<MenuVo> menuList;
    /**
     * 菜单树形结构
     */
    @Schema(description = "菜单树形结构列表")
    private List<MenuTreeVo> menuTreeList;
}
