package com.jiuyu.replay.power.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.power.bo.RoleInfoBo;
import com.jiuyu.replay.power.bo.RoleListBo;
import com.jiuyu.replay.power.constant.PowerProperties;
import com.jiuyu.replay.power.entity.MenuEntity;
import com.jiuyu.replay.power.entity.MenuRoleEntity;
import com.jiuyu.replay.power.entity.RoleEntity;
import com.jiuyu.replay.power.entity.UserRoleEntity;
import com.jiuyu.replay.power.producer.RoleProducer;
import com.jiuyu.replay.power.repository.dao.MenuDao;
import com.jiuyu.replay.power.repository.dao.MenuRoleDao;
import com.jiuyu.replay.power.repository.dao.RoleDao;
import com.jiuyu.replay.power.repository.dao.UserRoleDao;
import com.jiuyu.replay.power.repository.service.MenuRoleService;
import com.jiuyu.replay.power.repository.service.RoleService;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.power.repository.service.UserRoleService;
import com.jiuyu.replay.power.vo.RoleInfoVo;
import com.jiuyu.replay.power.vo.RoleVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleProducerImpl implements RoleProducer {

    @Resource
    private UserRoleDao userRoleDao;
    @Resource
    private MenuRoleDao menuRoleDao;
    @Resource
    private MenuDao menuDao;
    @Resource
    private RoleDao roleDao;
    @Resource
    private RoleService roleService;
    @Resource
    private MenuRoleService menuRoleService;
    @Resource
    private PowerProperties powerProperties;
    @Resource
    private UserRoleService userRoleService;

    @Override
    public List<Long> findIdsByUserId(Long id) {
        List<UserRoleEntity> userRoleEntities = this.userRoleDao.selectList(new QueryWrapper<UserRoleEntity>().eq("user_id", id));
        if(userRoleEntities != null && userRoleEntities.size() >0) {
            return userRoleEntities.stream().map(UserRoleEntity::getRoleId).collect(Collectors.toList());

        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRootPower() {
        // 删除原有权限
        this.menuRoleDao.delete(new QueryWrapper<MenuRoleEntity>().eq("role_id", 1));

        // 添加新的权限
        List<MenuEntity> menuEntities = this.menuDao.selectList(new QueryWrapper<MenuEntity>().select("id"));
        if(menuEntities != null && menuEntities.size() > 0) {
            List<MenuRoleEntity> menuRoleEntities = menuEntities.stream().map(menu -> {
                MenuRoleEntity menuRoleEntity = new MenuRoleEntity();
                menuRoleEntity.setId(SnowflakeManager.nextValue());
                menuRoleEntity.setRoleId(1L);
                menuRoleEntity.setMenuId(menu.getId());
                menuRoleEntity.setCreateDate(new Date());
                menuRoleEntity.setUpdateDate(new Date());
                menuRoleEntity.setIsDeleted(0);
                return menuRoleEntity;
            }).collect(Collectors.toList());

            this.menuRoleService.saveBatch(menuRoleEntities);
        }
    }

    @Override
    public PageUtils<RoleVo> queryPage(RoleListBo roleListBo) {
        QueryWrapper<RoleEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(roleListBo.getKeyword())){
            wrapper.like("name", roleListBo.getKeyword());
        }
        wrapper.gt("id", 1);

        IPage<RoleEntity> iPage = this.roleService.page(new Query<RoleEntity>().getPage(roleListBo.getPage(), roleListBo.getLimit()), wrapper);

        PageUtils<RoleVo> pageUtils = new PageUtils<>(roleListBo.getPage(), roleListBo.getLimit(), iPage);

        List<RoleEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<RoleVo> vos = records.stream().map(item -> {
                RoleVo roleVo = new RoleVo();
                BeanUtils.copyProperties(item, roleVo);
                return roleVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public RoleVo info(Long id) {

        RoleEntity roleEntity = this.roleDao.selectById(id);
        if(roleEntity != null) {
            RoleVo roleVo = new RoleVo();
            BeanUtils.copyProperties(roleEntity, roleVo);
            return roleVo;
        }

        return null;
    }

    @Override
    public RoleInfoVo save(RoleInfoBo role) {
        RoleEntity roleEntity = new RoleEntity();
        BeanUtils.copyProperties(role, roleEntity);
        roleEntity.setId(SnowflakeManager.nextValue());
        roleEntity.setCreateDate(new Date());
        roleEntity.setUpdateDate(new Date());
        this.roleDao.insert(roleEntity);

        RoleInfoVo roleInfoVo = new RoleInfoVo();
        BeanUtils.copyProperties(roleEntity, roleInfoVo);

        return roleInfoVo;
    }

    @Override
    public RoleInfoVo modify(RoleInfoBo role) {
        RoleEntity roleEntity = new RoleEntity();
        BeanUtils.copyProperties(role, roleEntity);
        roleEntity.setUpdateDate(new Date());
        this.roleDao.updateById(roleEntity);

        RoleInfoVo roleInfoVo = new RoleInfoVo();
        BeanUtils.copyProperties(roleEntity, roleInfoVo);

        return roleInfoVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {

        this.roleDao.deleteById(id);

        // 删除角色和用户的关联关系
        this.userRoleService.remove(new QueryWrapper<UserRoleEntity>().eq("role_id", id));
    }

    @Override
    public void deleteByUserId(Long userId) {

        this.userRoleDao.delete(new QueryWrapper<UserRoleEntity>().eq("user_id", userId));
    }

    @Override
    public void createUserDefaultRole(Long id) {
        UserRoleEntity userRoleEntity = new UserRoleEntity();
        userRoleEntity.setId(SnowflakeManager.nextValue());
        userRoleEntity.setRoleId(powerProperties.getDefaultRoleId());
        userRoleEntity.setUserId(id);
        userRoleEntity.setCreateDate(new Date());
        userRoleEntity.setUpdateDate(new Date());

        this.userRoleDao.insert(userRoleEntity);
    }

    @Override
    public void saveUserRoles(Long id, List<Long> roleIds) {

        if (roleIds != null && !roleIds.isEmpty()) {
            List<UserRoleEntity> userRoleEntities = roleIds.stream().map(item -> {
                UserRoleEntity userRoleEntity = new UserRoleEntity();
                userRoleEntity.setId(SnowflakeManager.nextValue());
                userRoleEntity.setRoleId(item);
                userRoleEntity.setUserId(id);
                userRoleEntity.setCreateDate(new Date());
                userRoleEntity.setUpdateDate(new Date());
                return userRoleEntity;
            }).toList();

            this.userRoleService.saveBatch(userRoleEntities);
        }
    }

    @Override
    public void addUserRoles(Long id, List<Long> roleIds) {
        if (roleIds != null && !roleIds.isEmpty()) {
            List<Long> currentRoleIds = userRoleService.lambdaQuery()
                    .eq(UserRoleEntity::getUserId, id)
                    .select(UserRoleEntity::getRoleId)
                    .list()
                    .stream()
                    .map(UserRoleEntity::getRoleId)
                    .toList();

            this.saveUserRoles(id, roleIds.stream().filter(roleId -> !currentRoleIds.contains(roleId)).toList());
        }
    }

    @Override
    public void updateUserRoles(Long id, List<Long> roleIdList) {

        // 删除原角色关联
        this.userRoleService.remove(new QueryWrapper<UserRoleEntity>().eq("user_id", id));

        // 添加新关联
        this.saveUserRoles(id, roleIdList);
    }
}
