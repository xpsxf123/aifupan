package com.jiuyu.replay.power.producer;


import com.jiuyu.replay.power.bo.RoleInfoBo;
import com.jiuyu.replay.power.bo.RoleListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.power.vo.RoleInfoVo;
import com.jiuyu.replay.power.vo.RoleVo;

import java.util.List;


public interface RoleProducer {

    /**
     * 根据用户id获取用户所有角色id
     * @param id 用户id
     * @return
     */
    List<Long> findIdsByUserId(Long id);

    /**
     * 更新超管的菜单权限
     */
    void updateRootPower();

    /**
     * 角色列表
     * @param roleListBo 菜单列表查询参数
     * @return
     */
    PageUtils<RoleVo> queryPage(RoleListBo roleListBo);

    /**
     * 信息
     * @param id 角色id
     * @return
     */
    RoleVo info(Long id);

    /**
     * 保存
     * @param role 数据对象
     * @return
     */
    RoleInfoVo save(RoleInfoBo role);

    /**
     * 修改
     * @param role 数据对象
     * @return
     */
    RoleInfoVo modify(RoleInfoBo role);

    /**
     * 删除
     * @param id 角色id
     * @return
     */
    void deleteById(Long id);

    /**
     * 删除用户-角色的关联
     * @param userId 用户id
     */
    void deleteByUserId(Long userId);

    /**
     * 保存一个默认的角色
     * @param id 用户id
     */
    void createUserDefaultRole(Long id);
    /**
     * 保存用户的角色列表
     * @param id 用户id
     */
    void saveUserRoles(Long id, List<Long> roleIds);

    void addUserRoles(Long id, List<Long> roleIds);

    /**
     * 修改用户的角色列表
     * @param id 用户id
     */
    void updateUserRoles(Long id, List<Long> roleIdList);
}
