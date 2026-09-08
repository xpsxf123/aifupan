package com.jiuyu.governance.business.performance.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.governance.business.performance.pojo.bo.OrgPerformanceBO;
import com.jiuyu.governance.business.performance.pojo.request.DeptPerformancePageRequest;
import com.jiuyu.governance.business.performance.pojo.request.SubCompanyPerformancePageRequest;
import com.jiuyu.governance.business.performance.pojo.request.TeamPerformancePageRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 组织业绩Mapper（分公司/部门/小组维度业绩聚合查询）
 *
 * @author lj
 * @date 2026-06-26
 */
@Mapper
public interface OrgPerformanceMapper {

    /**
     * 分页查询分公司业绩（LEFT JOIN session_performance 聚合 + 动态排序）
     */
    IPage<OrgPerformanceBO> pageQuerySubCompanyPerformance(
            IPage<OrgPerformanceBO> page,
            @Param("req") SubCompanyPerformancePageRequest req);

    /**
     * 分页查询部门业绩（LEFT JOIN session_performance 聚合 + 动态排序）
     */
    IPage<OrgPerformanceBO> pageQueryDeptPerformance(
            IPage<OrgPerformanceBO> page,
            @Param("req") DeptPerformancePageRequest req);

    /**
     * 分页查询小组业绩（LEFT JOIN session_performance 聚合 + 动态排序）
     */
    IPage<OrgPerformanceBO> pageQueryTeamPerformance(
            IPage<OrgPerformanceBO> page,
            @Param("req") TeamPerformancePageRequest req);
}
