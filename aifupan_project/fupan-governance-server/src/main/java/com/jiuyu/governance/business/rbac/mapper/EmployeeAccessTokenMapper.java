package com.jiuyu.governance.business.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.governance.business.rbac.pojo.entity.EmployeeAccessToken;
import org.apache.ibatis.annotations.Mapper;

/**
 * 员工登录授权管理
 *
 * @author hehui
 * @date 2026-03-27 15:53
 */
@Mapper
public interface EmployeeAccessTokenMapper extends BaseMapper<EmployeeAccessToken> {
}
