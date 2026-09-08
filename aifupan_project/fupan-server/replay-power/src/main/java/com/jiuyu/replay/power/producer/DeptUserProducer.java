package com.jiuyu.replay.power.producer;

import com.jiuyu.replay.power.entity.DeptUserEntity;

import java.util.List;

/**
 * 部门对应下的用户
 *
 * @author jxy
 * @date 2024-07-08
 */
public interface DeptUserProducer {

    /**
     * 根据部门ID查询用户列表
     *
     * @param deptId 部门id
     * @return 用户列表
     */
    List<DeptUserEntity> listByDeptId(Long deptId);

    /**
     * 根据用户ID查询部门列表
     *
     * @param userId 用户id
     * @return 部门列表
     */
    List<DeptUserEntity> listByUserId(Long userId);

    /**
     * 部门用户信息
     *
     * @param id ID
     * @return 部门用户信息
     */
    DeptUserEntity info(Long id);

    /**
     * 新增部门用户
     *
     * @param entity 部门用户对象
     * @return 部门用户信息
     */
    DeptUserEntity save(DeptUserEntity entity);

    /**
     * 修改部门用户
     *
     * @param entity 部门用户对象
     */
    void update(DeptUserEntity entity);

    /**
     * 删除部门用户
     *
     * @param id ID
     */
    void deleteById(Long id);
}
