package com.jiuyu.replay.power.bll;

import com.jiuyu.replay.power.bo.MenuBo;
import com.jiuyu.replay.power.bo.MenuListBo;
import com.jiuyu.replay.power.producer.MenuProducer;
import com.jiuyu.replay.power.producer.RoleProducer;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.vo.MenuTreeSelfVo;
import com.jiuyu.replay.power.vo.MenuTreeVo;
import com.jiuyu.replay.power.vo.MenuVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class MenuBll {

    @Resource
    private MenuProducer menuProducer;
    @Resource
    private RoleProducer roleProducer;

    /**
     * 获取角色所拥有的菜单列表（列表以及树形结构）
     * @param roleId 角色id
     * @return
     */
    public R<MenuTreeSelfVo> listTreeByRoleId(Long roleId) {

        List<MenuVo> menuVos = this.menuProducer.listByRoleId(roleId);
        List<MenuTreeVo> menuTreeVos = this.menuProducer.packageMenuTree(menuVos, false);

        MenuTreeSelfVo menuTreeSelfVo = new MenuTreeSelfVo();
        menuTreeSelfVo.setMenuList(menuVos);
        menuTreeSelfVo.setMenuTreeList(menuTreeVos);

        return R.ok(menuTreeSelfVo);

    }

    /**
     * 获取用户所拥有的菜单列表（列表以及树形结构）
     * @param userId 用户id
     * @return
     */
    public R<MenuTreeSelfVo> listTreeByUserId(Long userId) {

        List<MenuVo> menuVos = this.menuProducer.listByUserId(userId);
        List<MenuTreeVo> menuTreeVos = this.menuProducer.packageMenuTree(menuVos, false);

        MenuTreeSelfVo menuTreeSelfVo = new MenuTreeSelfVo();
        menuTreeSelfVo.setMenuList(menuVos);
        menuTreeSelfVo.setMenuTreeList(menuTreeVos);

        return R.ok(menuTreeSelfVo);

    }

    /**
     * 菜单列表
     * @param menuListBo 菜单列表查询参数
     * @return
     */
    public R<PageUtils<MenuVo>> queryPage(MenuListBo menuListBo) {

        return R.ok(this.menuProducer.queryPage(menuListBo));
    }

    /**
     * 菜单信息
     * @param id 菜单id
     * @return
     */
    public R<MenuVo> info(Long id) {

        return R.ok(this.menuProducer.infoById(id));
    }

    /**
     * 新增菜单
     * @param menuBo 菜单对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> save(MenuBo menuBo) {

        this.menuProducer.save(menuBo);

        // 更新超管的菜单权限
        this.roleProducer.updateRootPower();

        return R.ok("添加成功");
    }

    /**
     * 修改菜单
     * @param menuBo 菜单对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> update(MenuBo menuBo) {

        this.menuProducer.update(menuBo);
        // 更新超管的菜单权限
        this.roleProducer.updateRootPower();

        return R.ok("修改成功");
    }

    /**
     * 删除菜单
     * @param id 菜单id
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> delete(Long id) {

        this.menuProducer.delete(id);

        // 更新超管的菜单权限
        this.roleProducer.updateRootPower();

        return R.ok("删除成功");
    }
}
