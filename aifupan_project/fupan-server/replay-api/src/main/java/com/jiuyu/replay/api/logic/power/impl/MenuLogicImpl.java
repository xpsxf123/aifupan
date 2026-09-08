package com.jiuyu.replay.api.logic.power.impl;

import com.jiuyu.replay.api.logic.power.MenuLogic;
import com.jiuyu.replay.power.bll.MenuBll;
import com.jiuyu.replay.power.bo.MenuBo;
import com.jiuyu.replay.power.bo.MenuListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.vo.MenuTreeSelfVo;
import com.jiuyu.replay.power.vo.MenuVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class MenuLogicImpl implements MenuLogic {

    @Resource
    private MenuBll menuBll;

    @Override
    public R<MenuTreeSelfVo> listTreeByRoleId(Long roleId) {

        return menuBll.listTreeByRoleId(roleId);
    }

    @Override
    public R<MenuTreeSelfVo> listTreeByUserId(Long userId) {

        return menuBll.listTreeByUserId(userId);
    }

    @Override
    public R<PageUtils<MenuVo>> queryPage(MenuListBo menuListBo) {

        return menuBll.queryPage(menuListBo);
    }

    @Override
    public R<MenuVo> info(Long id) {

        return menuBll.info(id);
    }

    @Override
    public R<String> save(MenuBo menuBo) {

        return menuBll.save(menuBo);
    }

    @Override
    public R<String> update(MenuBo menuBo) {

        return menuBll.update(menuBo);
    }

    @Override
    public R<String> delete(Long id) {

        return menuBll.delete(id);
    }
}
