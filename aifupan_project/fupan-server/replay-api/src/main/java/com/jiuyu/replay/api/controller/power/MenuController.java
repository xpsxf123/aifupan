package com.jiuyu.replay.api.controller.power;

import com.jiuyu.replay.api.logic.power.MenuLogic;
import com.jiuyu.replay.power.bo.MenuBo;
import com.jiuyu.replay.power.bo.MenuListBo;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.vo.MenuTreeSelfVo;
import com.jiuyu.replay.power.vo.MenuVo;
import com.jiuyu.replay.power.vo.UserCacheVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;


/**
 * 菜单
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-02-26 10:00:04
 */
@RestController
@CrossOrigin
@RequestMapping("replay/menu")
@Tag(name = "菜单")
public class MenuController {

    @Resource
    private MenuLogic menuLogic;

    /**
     * 获取角色所拥有的菜单列表（列表以及树形结构）
     * @param roleId 角色id
     * @return
     */
    @GetMapping("/listTreeByRoleId")
    @Operation(summary = "获取角色所拥有的菜单列表（列表以及树形结构）")
    public R<MenuTreeSelfVo> listTreeByRoleId(@Parameter(description = "角色id", required = true) @RequestParam("roleId") Long roleId) {

        return menuLogic.listTreeByRoleId(roleId);

    }

    /**
     * 获取当前账号所拥有的菜单列表（列表以及树形结构）
     * @return
     */
    @GetMapping("/listTreeSelf")
    @Operation(summary = "获取当前账号所拥有的菜单列表（列表以及树形结构）")
    public R<MenuTreeSelfVo> listTreeSelf() {
        UserCacheVo user = GlobalObject.getLocalUser();

        return menuLogic.listTreeByUserId(user.getId());
    }

    /**
     * 菜单列表
     * @param menuListBo 菜单列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "菜单列表")
    public R<PageUtils<MenuVo>> list(@Parameter(description = "菜单列表查询参数", required = true) @RequestBody MenuListBo menuListBo){

        return menuLogic.queryPage(menuListBo);
    }


    /**
     * 菜单信息
     * @param id 菜单id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "菜单信息")
    public R<MenuVo> info(@Parameter(description = "菜单id", required = true) @RequestParam("id") Long id){

        return menuLogic.info(id);
    }

    /**
     * 新增菜单
     * @param menuBo 菜单对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增菜单")
    public R<String> save(@Parameter(description = "菜单对象", required = true) @RequestBody MenuBo menuBo){

        return this.menuLogic.save(menuBo);

    }

    /**
     * 修改菜单
     * @param menuBo 菜单对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改菜单")
    public R<String> update(@Parameter(description = "菜单对象", required = true) @RequestBody MenuBo menuBo){

        return this.menuLogic.update(menuBo);

    }

    /**
     * 删除菜单
     * @param id 菜单id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除菜单")
    public R<String> delete(@Parameter(description = "菜单id", required = true) @RequestParam("id") Long id){

        return this.menuLogic.delete(id);

    }

}
