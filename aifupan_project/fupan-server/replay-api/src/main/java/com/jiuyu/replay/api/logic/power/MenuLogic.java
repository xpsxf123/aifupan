package com.jiuyu.replay.api.logic.power;

import com.jiuyu.replay.power.bo.MenuBo;
import com.jiuyu.replay.power.bo.MenuListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.vo.MenuTreeSelfVo;
import com.jiuyu.replay.power.vo.MenuVo;

public interface MenuLogic {

    /**
     * 获取角色所拥有的菜单列表（列表以及树形结构）
     * @param roleId 角色id
     * @return
     */
    R<MenuTreeSelfVo> listTreeByRoleId(Long roleId);

    /**
     * 获取用户所拥有的菜单列表（列表以及树形结构）
     * @param userId 用户id
     * @return
     */
    R<MenuTreeSelfVo> listTreeByUserId(Long userId);

    /**
     * 菜单列表
     * @param menuListBo 菜单列表查询参数
     * @return
     */
    R<PageUtils<MenuVo>> queryPage(MenuListBo menuListBo);

    /**
     * 菜单信息
     * @param id 菜单id
     * @return
     */
    R<MenuVo> info(Long id);

    /**
     * 新增菜单
     * @param menuBo 菜单对象
     * @return
     */
    R<String> save(MenuBo menuBo);

    /**
     * 修改菜单
     * @param menuBo 菜单对象
     * @return
     */
    R<String> update(MenuBo menuBo);

    /**
     * 删除菜单
     * @param id 菜单id
     * @return
     */
    R<String> delete(Long id);
}
