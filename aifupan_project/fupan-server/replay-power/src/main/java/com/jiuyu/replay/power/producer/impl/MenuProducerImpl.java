package com.jiuyu.replay.power.producer.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.power.bo.MenuBo;
import com.jiuyu.replay.power.bo.MenuListBo;
import com.jiuyu.replay.power.entity.MenuEntity;
import com.jiuyu.replay.power.entity.MenuRoleEntity;
import com.jiuyu.replay.power.producer.MenuProducer;
import com.jiuyu.replay.power.producer.RoleProducer;
import com.jiuyu.replay.power.repository.dao.MenuDao;
import com.jiuyu.replay.power.repository.dao.MenuRoleDao;
import com.jiuyu.replay.power.repository.service.MenuRoleService;
import com.jiuyu.replay.power.repository.service.MenuService;
import com.jiuyu.replay.power.vo.MenuTreeVo;
import com.jiuyu.replay.power.vo.MenuVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MenuProducerImpl implements MenuProducer {

    @Resource
    private RoleProducer roleProducer;
    @Resource
    private MenuRoleDao menuRoleDao;
    @Resource
    private MenuDao menuDao;
    @Resource
    private MenuService menuService;
    @Resource
    private MenuRoleService menuRoleService;

    @Override
    public List<MenuVo> listByUserId(Long userId) {
        // 获取用户的所有角色
        List<Long> roleIds = roleProducer.findIdsByUserId(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            // 获取角色拥有的菜单
            List<MenuRoleEntity> menuRoleEntities = this.menuRoleDao.selectList(
                    new QueryWrapper<MenuRoleEntity>().in("role_id", roleIds));
            if (menuRoleEntities != null && !menuRoleEntities.isEmpty()) {
                List<Long> menuIds = menuRoleEntities.stream().map(MenuRoleEntity::getMenuId).collect(Collectors.toList());

                QueryWrapper<MenuEntity> menuWrapper = new QueryWrapper<MenuEntity>().in("id", menuIds).orderByAsc("sort");

                List<MenuEntity> menuEntities = this.menuDao.selectList(menuWrapper);
                if (menuEntities != null && !menuEntities.isEmpty()) {
                    return menuEntities.stream().map(item -> {
                        MenuVo menuVo = new MenuVo();
                        BeanUtils.copyProperties(item, menuVo);
                        return menuVo;
                    }).toList();
                }

            }

        }
        return null;
    }

    @Override
    public List<MenuTreeVo> packageMenuTree(List<MenuVo> menuVos, boolean isSelect) {
        if (menuVos == null || menuVos.isEmpty()) {
            return null;
        }

        // 如果isSelect为true，删除功能，只保留目录和菜单
        List<MenuVo> temp = menuVos.stream().filter(item -> !isSelect || item.getType() != 1).toList();
        if (temp.isEmpty()) {
            return null;
        }
        // 获取所有的功能
        Map<Long, List<MenuVo>> functionMap = menuVos.stream()
                .filter(item -> ObjectUtil.equals(item.getType(), 1))
                .collect(Collectors.groupingBy(MenuVo::getParentId));

        // 筛选出一级菜单
        return temp.stream().filter(item -> item.getParentId() == 0)
                .map(item -> {
                    MenuTreeVo menuTreeVo = new MenuTreeVo();
                    BeanUtils.copyProperties(item, menuTreeVo);
                    // 封装子菜单
                    menuTreeVo.setChildren(getChildrenMenu(menuTreeVo, temp, functionMap));
                    // 封装功能
                    menuTreeVo.setMetaList(functionMap.get(item.getId()));
                    return menuTreeVo;
                }).toList();
    }

    @Override
    public List<MenuVo> listByRoleId(Long roleId) {

        // 获取角色拥有的菜单
        List<MenuRoleEntity> menuRoleEntities = this.menuRoleDao.selectList(
                new QueryWrapper<MenuRoleEntity>().eq("role_id", roleId));

        if(menuRoleEntities != null && menuRoleEntities.size() >0) {
            List<Long> menuIds = menuRoleEntities.stream().map(MenuRoleEntity::getMenuId).collect(Collectors.toList());

            QueryWrapper<MenuEntity> menuWrapper = new QueryWrapper<MenuEntity>().in("id", menuIds).orderByAsc("sort");

            List<MenuEntity> menuEntities = this.menuDao.selectList(menuWrapper);
            if(menuEntities != null && menuEntities.size() > 0) {
                List<MenuVo> vos = menuEntities.stream().map(item -> {
                    MenuVo menuVo = new MenuVo();
                    BeanUtils.copyProperties(item, menuVo);
                    return menuVo;
                }).toList();

                return vos;
            }

        }

        return null;
    }

    @Override
    public void saveRoleMenu(Long roleId, List<Long> roleIdList) {
        if(roleIdList != null && roleIdList.size() > 0) {
            List<MenuRoleEntity> menuRoleEntities = roleIdList.stream().map(item -> {
                MenuRoleEntity menuRoleEntity = new MenuRoleEntity();
                menuRoleEntity.setId(SnowflakeManager.nextValue());
                menuRoleEntity.setMenuId(item);
                menuRoleEntity.setRoleId(roleId);
                menuRoleEntity.setCreateDate(new Date());
                menuRoleEntity.setUpdateDate(new Date());
                menuRoleEntity.setIsDeleted(0);
                return menuRoleEntity;
            }).collect(Collectors.toList());

            this.menuRoleService.saveBatch(menuRoleEntities);
        }
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        this.menuRoleDao.delete(new QueryWrapper<MenuRoleEntity>().eq("role_id", roleId));
    }

    @Override
    public PageUtils<MenuVo> queryPage(MenuListBo menuListBo) {

        QueryWrapper<MenuEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(menuListBo.getKeyword())){
            wrapper.like("name", menuListBo.getKeyword());
        }

        IPage<MenuEntity> iPage = this.menuService.page(new Query<MenuEntity>().getPage(menuListBo.getPage(), menuListBo.getLimit()), wrapper);

        PageUtils<MenuVo> pageUtils = new PageUtils<>(menuListBo.getPage(), menuListBo.getLimit(), iPage);

        List<MenuEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<MenuVo> vos = records.stream().map(item -> {
                MenuVo menuVo = new MenuVo();
                BeanUtils.copyProperties(item, menuVo);
                return menuVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public MenuVo infoById(Long id) {
        MenuEntity menuEntity = this.menuService.getById(id);
        if(menuEntity != null) {
            MenuVo menuVo = new MenuVo();
            BeanUtils.copyProperties(menuEntity, menuVo);
            return menuVo;
        }
        return null;
    }

    @Override
    public void save(MenuBo menuBo) {

        MenuEntity menuEntity = new MenuEntity();
        BeanUtils.copyProperties(menuBo, menuEntity);

        menuEntity.setId(SnowflakeManager.nextValue());
        menuEntity.setCreateDate(new Date());
        menuEntity.setUpdateDate(new Date());

        this.menuService.save(menuEntity);

    }

    @Override
    public void update(MenuBo menuBo) {

        MenuEntity menuEntity = new MenuEntity();
        BeanUtils.copyProperties(menuBo, menuEntity);

        menuEntity.setUpdateDate(new Date());

        this.menuService.updateById(menuEntity);

    }

    @Override
    public void delete(Long id) {

        this.menuService.removeById(id);
    }

    /**
     * 返回当前菜单的子菜单
     * @param menuTreeVo 当前菜单
     * @param menuVos 所有菜单集合
     * @return
     */
    private List<MenuTreeVo> getChildrenMenu(MenuTreeVo menuTreeVo, List<MenuVo> menuVos, Map<Long, List<MenuVo>> functionMap) {
        return menuVos.stream().filter(item -> item.getParentId().equals(menuTreeVo.getId())).map(item -> {
            MenuTreeVo childrenMenuTreeVo = new MenuTreeVo();
            BeanUtils.copyProperties(item, childrenMenuTreeVo);
            childrenMenuTreeVo.setChildren(getChildrenMenu(childrenMenuTreeVo, menuVos, functionMap));
            childrenMenuTreeVo.setMetaList(functionMap.get(item.getId()));
            return childrenMenuTreeVo;
        }).toList();
    }
}
