package com.jiuyu.replay.power.bo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema
public class MenuBo {

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    @Schema(description = "ID")
    private Long id;
    /**
     * 父菜单id
     */
    @Schema(description = "父菜单id")
    private Long parentId;
    /**
     * 菜单名
     */
    @Schema(description = "菜单名")
    private String name;
    /**
     * 菜单url
     */
    @Schema(description = "菜单url")
    private String url;
    /**
     * 0：菜单 1：功能 2：目录
     */
    @Schema(description = "0：菜单 1：功能 2：目录")
    private Integer type;
    /**
     * 排序
     */
    @Schema(description = "排序")
    private Integer sort;
    /**
     * 图标
     */
    @Schema(description = "图标")
    private String img;
}
