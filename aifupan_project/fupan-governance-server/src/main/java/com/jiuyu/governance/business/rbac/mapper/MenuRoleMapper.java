package com.jiuyu.governance.business.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.governance.business.rbac.pojo.entity.MenuRole;
import jakarta.validation.constraints.NotNull;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜单角色关系表
 *
 * @author HeHui
 * @date 2026-03-17 16:02
 */
@Mapper
public interface MenuRoleMapper extends BaseMapper<MenuRole> {

    /**
     * 批量插入
     *
     * @param list 列表
     */
    void insertBatch(List<MenuRole> list);
}
