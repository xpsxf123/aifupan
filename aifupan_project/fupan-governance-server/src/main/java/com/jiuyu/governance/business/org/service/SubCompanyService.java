package com.jiuyu.governance.business.org.service;

import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.governance.common.pojo.bo.LabelOption;
import com.jiuyu.governance.business.org.pojo.request.SubCompanyAddRequest;
import com.jiuyu.governance.business.org.pojo.request.SubCompanyPageQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.SubCompanySelectQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.SubCompanyUpdateRequest;
import com.jiuyu.governance.business.org.pojo.response.SubCompanyResponse;

import java.util.*;

/**
 * 子公司服务接口
 *
 * @author HeHui
 * @date 2026-03-18
 */
public interface SubCompanyService {



    /**
     * 获取租户的第一个子公司
     *
     * @param tenantId 租户ID
     * @return 子公司ID
     */
    Optional<Long> getTenantFirst(long tenantId);

    /**
     * 新增子公司
     *
     * @param request  请求参数
     * @param tenantId 租户ID
     * @param userId   当前用户ID
     * @return 响应结果
     */
    ApiResponse<Void> addSubCompany(SubCompanyAddRequest request, Long tenantId, Long userId);

    /**
     * 修改子公司
     *
     * @param request  请求参数
     * @param tenantId 租户ID
     * @param userId   当前用户ID
     * @return 响应结果
     */
    ApiResponse<Void> updateSubCompany(SubCompanyUpdateRequest request, Long tenantId, Long userId);

    /**
     * 删除子公司
     *
     * @param id       子公司ID
     * @param tenantId 租户ID
     * @param userId   当前用户ID
     * @return 响应结果
     */
    ApiResponse<Void> deleteSubCompany(Long id, Long tenantId, Long userId);

    /**
     * 分页查询子公司
     *
     * @param request    查询请求
     * @param accessUser 租户ID
     *
     * @return 分页数据
     */
    PageData<SubCompanyResponse> pageQuerySubCompany(SubCompanyPageQueryRequest request, AccessUser accessUser);

    /**
     * 判断是否存在ID
     *
     * @param companyId 公司ID
     * @param tenantId  租户ID
     * @return 是否存在
     */
    boolean hasId(Long companyId, Long tenantId);

    /**
     * 获取名称Map
     *
     * @param ids ID列表
     * @return 名称Map
     */
    Map<Long, String> getNameMap(List<Long> ids);

    /**
     * 下拉搜索
     *
     * @param queryRequest 查询请求
     *
     * @return {@link List }<{@link LabelOption }>
     */
    List<LabelOption> options(SubCompanySelectQueryRequest queryRequest);

    /**
     * 获取公司列表
     *
     * @param companyIds 公司ID列表
     * @return 公司列表
     */
    List<LabelOption> getCompanyList(Collection<Long> companyIds);
}
