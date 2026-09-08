package com.jiuyu.governance.business.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.governance.business.org.pojo.entity.Dept;
import com.jiuyu.governance.business.org.pojo.request.DeptPageQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.DeptSelectQueryRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 部门
 *
 * @author hehui
 * @date 2026-03-18 17:23
 */
@Mapper
public interface DeptMapper extends BaseMapper<Dept> {

    /**
     * 分页查询部门
     *
     * @param page 页
     * @param req 要求
     * @return {@link IPage }<{@link Dept }>
     */
    IPage<Dept> pageQueryDept(IPage<Dept> page, @Param("req") DeptPageQueryRequest req);


    /**
     * 列表部门
     *
     * @param req 要求
     * @return {@link List }<{@link Dept }>
     */
    List<Dept> listDept(@Param("req") DeptSelectQueryRequest req);
}
