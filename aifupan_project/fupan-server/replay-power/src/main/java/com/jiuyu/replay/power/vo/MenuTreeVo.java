package com.jiuyu.replay.power.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "菜单-树形结构")
public class MenuTreeVo extends MenuVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 子菜单集合
     */
    @Schema(description = "子菜单集合")
    private List<MenuTreeVo> children;

    @Schema(description = "功能列表")
    private List<MenuVo> metaList;
}
