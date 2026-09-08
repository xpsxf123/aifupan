package com.jiuyu.governance.business.org.controller;

import com.jiuyu.framework.function.complete.Complete;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.oauth.client.annotation.Permissions;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.org.pojo.constants.ManagerType;
import com.jiuyu.governance.business.org.pojo.response.SubCompanyResponse;
import com.jiuyu.governance.business.room.service.LiveRoomService;
import com.jiuyu.governance.common.pojo.bo.IdRequest;
import com.jiuyu.governance.common.pojo.bo.LabelOption;
import com.jiuyu.governance.business.org.pojo.request.TeamAddRequest;
import com.jiuyu.governance.business.org.pojo.request.TeamPageQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.TeamSelectQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.TeamUpdateRequest;
import com.jiuyu.governance.business.org.pojo.response.TeamResponse;
import com.jiuyu.governance.business.org.service.TeamService;
import com.jiuyu.governance.plugins.oauth.GovernanceUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 企业端-小组管理
 *
 * @author HeHui
 * @date 2026-03-18
 */
@GovernanceUser
@RestController
@RequestMapping("/api/governance/team")
public class TeamController {

    private final TeamService teamService;

    private final LiveRoomService liveRoomService;

    public TeamController(TeamService teamService, LiveRoomService liveRoomService) {
        this.teamService = teamService;
        this.liveRoomService = liveRoomService;
    }

    /**
     * 新增小组
     *
     * @param request    请求
     * @param accessUser 访问用户
     *
     * @return 响应结果
     */
    @Permissions("org:team:manage:add")
    @PostMapping("/add")
    public ApiResponse<Void> addTeam(@Valid @RequestBody TeamAddRequest request, AccessUser accessUser) {
        if (request.getSort() == null) {
            request.setSort(1);
        }
        return teamService.addTeam(request, accessUser.currentTenantId(), accessUser.userId());
    }

    /**
     * 修改小组
     *
     * @param request    请求
     * @param accessUser 访问用户
     *
     * @return 响应结果
     */
    @Permissions({"org:team:manage:update", "org:team:manage:add"})
    @PostMapping("/update")
    public ApiResponse<Void> updateTeam(@Valid @RequestBody TeamUpdateRequest request, AccessUser accessUser) {
        return teamService.updateTeam(request, accessUser.currentTenantId(), accessUser.userId());
    }

    /**
     * 删除小组
     *
     * @param request    请求
     * @param accessUser 访问用户
     *
     * @return 响应结果
     */
    @Permissions("org:team:manage:delete")
    @PostMapping("/delete")
    public ApiResponse<Void> deleteTeam(@Valid @RequestBody IdRequest request, AccessUser accessUser) {
        return teamService.deleteTeam(request.getId(), accessUser.currentTenantId(), accessUser.userId());
    }

    /**
     * 分页查询小组
     *
     * @param request    请求
     * @param accessUser 访问用户
     *
     * @return 响应结果
     */
    @PostMapping("/page")
    public ApiResponse<PageData<TeamResponse>> pageQueryTeam(@RequestBody TeamPageQueryRequest request, AccessUser accessUser) {
        PageData<TeamResponse> pageData = teamService.pageQueryTeam(request, accessUser);
        if (EmptyUtil.isNotEmpty(pageData.getList())) {
            Complete.start(pageData.getList())
                .build(TeamResponse::getId, TeamResponse::setRoomCount, ids -> liveRoomService.countOrgRoomMap(ids, ManagerType.TEAM, accessUser.currentTenantId()))
                .then().over();
        }
        return pageData.toResult();
    }

    /**
     * 小组下拉选择
     *
     * @param queryRequest 请求
     *
     * @return 响应结果
     */
    @GetMapping("/options")
    public ApiResponse<List<LabelOption>> options(TeamSelectQueryRequest queryRequest) {
        return ApiResponse.success(teamService.options(queryRequest));
    }

}
