package com.jiuyu.replay.power.producer;

import com.jiuyu.replay.power.entity.DeptEntity;
import com.jiuyu.replay.power.vo.DeptInfoVo;
import com.jiuyu.replay.power.vo.DeptListVo;

import java.util.List;

/**
 * 部门表
 *
 * @author jxy
 * @date 2024-07-08
 */
public interface DeptProducer {

    /**
     * 部门列表
     *
     * @return 部门列表
     */
    List<DeptListVo> list();

    /**
     * 部门树形列表
     *
     * @return 部门树形列表
     */
    List<DeptListVo> treeList();

    /**
     * 部门信息
     *
     * @param id 部门id
     * @return 部门信息
     */
    DeptInfoVo info(Long id);

    /**
     * 新增部门
     *
     * @param entity 部门对象
     * @return 部门信息
     */
    DeptInfoVo save(DeptEntity entity);

    /**
     * 修改部门
     *
     * @param entity 部门对象
     */
    void update(DeptEntity entity);

    /**
     * 删除部门
     *
     * @param id 部门id
     */
    void deleteById(Long id);
}
