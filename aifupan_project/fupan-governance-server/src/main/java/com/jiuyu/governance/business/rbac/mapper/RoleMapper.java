package com.jiuyu.governance.business.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.governance.business.rbac.pojo.entity.Role;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色Mapper
 *
 * @author HeHui
 * @date 2026-03-17 16:02
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}
