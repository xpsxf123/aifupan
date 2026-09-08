package com.jiuyu.replay.power.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "登录返回的用户信息")
public class UserLoginVo extends UserVo {

    /**
     * 头像地址
     */
    @Schema(description = "头像地址")
    private String avatar;
    /**
     * token
     */
    @Schema(description = "token")
    private String token;
    /**
     * 拥有的角色id列表
     */
    @Schema(description = "拥有的角色id列表")
    private List<Long> roleIdList;
    /**
     * 拥有的菜单列表
     */
    @Schema(description = "拥有的菜单列表")
    private List<MenuVo> menuList;
    /**
     * 拥有的菜单列表（树形结构）
     */
    @Schema(description = "拥有的菜单列表（树形结构）")
    private List<MenuTreeVo> menuTreeList;

    @Schema(description = "代理商id")
    private Long agentId;
}
