package com.jiuyu.governance.business.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.governance.business.rbac.pojo.entity.UserRole;
import com.jiuyu.governance.common.pojo.bo.CountData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户角色关系表
 *
 * @author HeHui
 * @date 2026-03-17 16:02
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {


    /**
     * 批量插入
     *
     * @param userRoles 批量插入的数据
     */
    void insertBatch(List<UserRole> userRoles);

    /**
     * 查询角色用户数量
     *
     * @param roleIds 角色id
     *
     * @return 角色哟呼数量
     */
    List<CountData> selectRoleUserCount(@Param("roleIds") List<Long> roleIds);
}
