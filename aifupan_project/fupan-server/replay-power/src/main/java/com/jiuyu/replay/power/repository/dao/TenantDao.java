package com.jiuyu.replay.power.repository.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.power.entity.TenantEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.power.vo.TenantListVo;
import com.jiuyu.replay.power.vo.TenantOptionVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 租户
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-31 11:52:17
 */
@Mapper
public interface TenantDao extends BaseMapper<TenantEntity> {

    /**
     * 租户下拉选项
     *
     * @param userType 用户类型
     * @param name     名字
     * @param mobile   手机号码
     * @param limit    查询条数
     *
     * @return {@link List }<{@link TenantOptionVo }>
     */
    List<TenantOptionVo> selectOptions(@Param("userType") Integer userType, @Param("name") String name, @Param("mobile") String mobile, @Param("limit") Integer limit);

    IPage<TenantListVo> pageNormalUserTenants(Page<?> page, @Param("userType") Integer userType, @Param("phone") String phone);
}
