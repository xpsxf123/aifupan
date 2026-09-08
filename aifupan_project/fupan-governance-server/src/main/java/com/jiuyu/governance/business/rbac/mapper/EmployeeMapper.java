package com.jiuyu.governance.business.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.governance.business.rbac.pojo.entity.Employee;
import com.jiuyu.governance.business.rbac.pojo.request.EmployeeQueryRequest;
import com.jiuyu.governance.common.pojo.bo.CountData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  人员 Mapper 接口
 * </p>
 *
 * @author hehui
 * @since 2026-03-18
 */
@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {

    /**
     * 获取职位人员数量
     *
     * @param positionIds 职位ID列表
     *
     * @return 职位人员数量
     */
    List<CountData> countPositionEmployee(@Param("positionIds") List<Long> positionIds);

    /**
     * 分页查询人员
     *
     * @param page     分页参数
     * @param req      查询请求
     * @param tenantId 租户ID
     * @param isMain   是否租户主账户
     *
     * @return {@link IPage }<{@link Employee }>
     */
    IPage<Employee> pageQueryEmployee(IPage<Employee> page, @Param("req") EmployeeQueryRequest req, @Param("tenantId") Long tenantId, @Param("isMain") Boolean isMain);
}
