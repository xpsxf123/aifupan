package com.jiuyu.governance.business.org.controller;

import com.jiuyu.framework.function.complete.Complete;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.oauth.client.annotation.Permissions;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.org.pojo.constants.ManagerType;
import com.jiuyu.governance.business.room.service.LiveRoomService;
import com.jiuyu.governance.common.pojo.bo.IdRequest;
import com.jiuyu.governance.common.pojo.bo.LabelOption;
import com.jiuyu.governance.business.org.pojo.request.DeptSelectQueryRequest;
import com.jiuyu.governance.plugins.oauth.GovernanceUser;
import com.jiuyu.governance.business.org.pojo.request.DeptAddRequest;
import com.jiuyu.governance.business.org.pojo.request.DeptPageQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.DeptUpdateRequest;
import com.jiuyu.governance.business.org.pojo.response.DeptResponse;
import com.jiuyu.governance.business.org.service.DeptService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 企业端-部门管理
 *
 * @author HeHui
 * @date 2026-03-18
 */
@GovernanceUser
@RestController
@RequestMapping("/api/governance/dept")
public class DeptController {

    private final DeptService deptService;

    private final LiveRoomService liveRoomService;

    public DeptController(DeptService deptService, LiveRoomService liveRoomService) {
        this.deptService = deptService;
        this.liveRoomService = liveRoomService;
    }

    /**
     * 新增部门
     *
     * @param request    请求
     * @param accessUser 访问用户
     * @return 响应结果
     */
    @Permissions("org:dept:manage:add")
    @PostMapping("/add")
    public ApiResponse<Void> addDept(@Valid @RequestBody DeptAddRequest request, AccessUser accessUser) {
        if (request.getSort() == null) {
            request.setSort(1);
        }
        return deptService.addDept(request, accessUser.currentTenantId(), accessUser.userId());
    }

    /**
     * 修改部门
     *
     * @param request    请求
     * @param accessUser 访问用户
     * @return 响应结果
     */
    @Permissions({"org:dept:manage:update", "org:dept:manage:add"})
    @PostMapping("/update")
    public ApiResponse<Void> updateDept(@Valid @RequestBody DeptUpdateRequest request, AccessUser accessUser) {
        return deptService.updateDept(request, accessUser.currentTenantId(), accessUser.userId());
    }

    /**
     * 删除部门
     *
     * @param request    请求
     * @param accessUser 访问用户
     * @return 响应结果
     */
    @Permissions("org:dept:manage:delete")
    @PostMapping("/delete")
    public ApiResponse<Void> deleteDept(@Valid @RequestBody IdRequest request, AccessUser accessUser) {
        return deptService.deleteDept(request.getId(), accessUser.currentTenantId(), accessUser.userId());
    }

    /**
     * 分页查询部门
     *
     * @param request    请求
     * @param accessUser 访问用户
     * @return 响应结果
     */
    @PostMapping("/page")
    public ApiResponse<PageData<DeptResponse>> pageQueryDept(@RequestBody DeptPageQueryRequest request, AccessUser accessUser) {
        PageData<DeptResponse> pageData = deptService.pageQueryDept(request, accessUser);
        if (EmptyUtil.isNotEmpty(pageData.getList())) {
            Complete.start(pageData.getList())
                .build(DeptResponse::getId, DeptResponse::setRoomCount, ids -> liveRoomService.countOrgRoomMap(ids, ManagerType.DEPT, accessUser.currentTenantId()))
                .then().over();
        }
        return pageData.toResult();
    }


    /**
     * 部门下拉选择
     *
     * @param queryRequest 请求
     * @return 响应结果
     */
    @GetMapping("/options")
    public ApiResponse<List<LabelOption>> options(DeptSelectQueryRequest queryRequest) {
        return ApiResponse.success(deptService.options(queryRequest));
    }

}
