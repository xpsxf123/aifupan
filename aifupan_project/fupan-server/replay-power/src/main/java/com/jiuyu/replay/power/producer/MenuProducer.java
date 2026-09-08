package com.jiuyu.replay.power.producer;


import com.jiuyu.replay.power.bo.MenuBo;
import com.jiuyu.replay.power.bo.MenuListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.power.vo.MenuTreeVo;
import com.jiuyu.replay.power.vo.MenuVo;

import java.util.List;

public interface MenuProducer {

    /**
     * 根据用户id获取菜单列表
     * @param userId 用户id
     * @return
     */
    List<MenuVo> listByUserId(Long userId);

    /**
     * 将菜单列表转成树形结构
     * @param menuVos 菜单列表
     * @param isSelect 是否只要目录和菜单
     * @return
     */
    List<MenuTreeVo> packageMenuTree(List<MenuVo> menuVos, boolean isSelect);

    /**
     * 获取角色所拥有的菜单列表
     * @param roleId 角色id
     * @return
     */
    List<MenuVo> listByRoleId(Long roleId);

    /**
     * 保存角色跟菜单的关联
     * @param roleId 角色id
     * @param roleIdList 菜单id列表
     */
    void saveRoleMenu(Long roleId, List<Long> roleIdList);

    /**
     * 删除角色跟菜单的关联
     * @param roleId 角色id
     */
    void deleteByRoleId(Long roleId);

    /**
     * 菜单列表
     * @param menuListBo 菜单列表查询参数
     * @return
     */
    PageUtils<MenuVo> queryPage(MenuListBo menuListBo);

    /**
     * 菜单信息
     * @param id 菜单id
     * @return
     */
    MenuVo infoById(Long id);

    /**
     * 新增菜单
     * @param menuBo 菜单对象
     * @return
     */
    void save(MenuBo menuBo);

    /**
     * 修改菜单
     * @param menuBo 菜单对象
     * @return
     */
    void update(MenuBo menuBo);

    /**
     * 删除菜单
     * @param id 菜单id
     * @return
     */
    void delete(Long id);
}
