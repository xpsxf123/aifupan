package com.jiuyu.governance.business.org.service;

import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.governance.business.org.pojo.bo.TeamInfo;
import com.jiuyu.governance.common.pojo.bo.LabelOption;
import com.jiuyu.governance.business.org.pojo.request.TeamAddRequest;
import com.jiuyu.governance.business.org.pojo.request.TeamPageQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.TeamSelectQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.TeamUpdateRequest;
import com.jiuyu.governance.business.org.pojo.response.TeamResponse;

import java.util.List;
import java.util.Map;

/**
 * 小组服务接口
 *
 * @author HeHui
 * @date 2026-03-18
 */
public interface TeamService {

    /**
     * 新增小组
     *
     * @param request  请求参数
     * @param tenantId 租户ID
     * @param userId   当前用户ID
     * @return 响应结果
     */
    ApiResponse<Void> addTeam(TeamAddRequest request, Long tenantId, Long userId);

    /**
     * 修改小组
     *
     * @param request  请求参数
     * @param tenantId 租户ID
     * @param userId   当前用户ID
     * @return 响应结果
     */
    ApiResponse<Void> updateTeam(TeamUpdateRequest request, Long tenantId, Long userId);

    /**
     * 删除小组
     *
     * @param id       小组ID
     * @param tenantId 租户ID
     * @param userId   当前用户ID
     * @return 响应结果
     */
    ApiResponse<Void> deleteTeam(Long id, Long tenantId, Long userId);

    /**
     * 分页查询小组
     *
     * @param request    查询请求
     * @param accessUser 访问用户
     *
     * @return 分页数据
     */
    PageData<TeamResponse> pageQueryTeam(TeamPageQueryRequest request, AccessUser accessUser);

    /**
     * 小组下拉选择
     *
     * @param queryRequest 查询请求
     *
     * @return {@link List }<{@link LabelOption }>
     */
    List<LabelOption> options(TeamSelectQueryRequest queryRequest);


    /**
     * 获取小组列表
     *
     * @param queryRequest 查询请求
     *
     * @return {@link List }<{@link TeamInfo }>
     */
    List<TeamInfo> getTeamList(TeamSelectQueryRequest queryRequest);

    /**
     * 获取小组信息列表
     *
     * @param ids 小组ID列表
     *
     * @return {@link List }<{@link TeamInfo }>
     */
    List<TeamInfo> getTeamInfoList(List<Long> ids);

    /**
     * 获取小组名称映射
     *
     * @param ids 小组ID列表
     *
     * @return {@link Map }<{@link Long }, {@link String }>
     */
    Map<Long, String> getTeamNameMap(List<Long> ids);
}
