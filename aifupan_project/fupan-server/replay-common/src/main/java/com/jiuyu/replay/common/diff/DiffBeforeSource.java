package com.jiuyu.replay.common.diff;


import java.util.Optional;

/**
 * 差异对比原数据查询支持函数
 *
 * @author HeHui
 * @date 2025-06-25 20:06
 */
public interface DiffBeforeSource {

    /**
     * 差异对比原数据查询支持函数
     *
     * @return 业务类型
     */
    Business support();

    /**
     * 差异对比原数据查询支持函数
     *
     * @param id 原数据id
     * @return 原数据
     */
    Optional<Object> querySource(long id, Long tenantId);
}
