package com.jiuyu.replay.order.repository.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.order.entity.PackageUserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.order.vo.PackageUserVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户和自定义版本之间关联表
 */
@Mapper
public interface PackageUserDao extends BaseMapper<PackageUserEntity> {

    /**
     * 分页查询自定义版本用户（关联用户表）
     */
    IPage<PackageUserVo> queryPage(Page<PackageUserVo> page, @Param("packageId") Long packageId, @Param("keyword") String keyword);
}
