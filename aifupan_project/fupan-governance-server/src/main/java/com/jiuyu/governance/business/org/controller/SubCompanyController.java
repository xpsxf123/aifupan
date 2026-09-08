package com.jiuyu.governance.business.org.controller;

import com.jiuyu.framework.function.complete.Complete;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.oauth.client.annotation.Permissions;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.org.pojo.constants.ManagerType;
import com.jiuyu.governance.business.org.pojo.response.DeptResponse;
import com.jiuyu.governance.business.room.service.LiveRoomService;
import com.jiuyu.governance.common.pojo.bo.IdRequest;
import com.jiuyu.governance.common.pojo.bo.LabelOption;
import com.jiuyu.governance.business.org.pojo.request.SubCompanySelectQueryRequest;
import com.jiuyu.governance.plugins.oauth.GovernanceUser;
import com.jiuyu.governance.business.org.pojo.request.SubCompanyAddRequest;
import com.jiuyu.governance.business.org.pojo.request.SubCompanyPageQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.SubCompanyUpdateRequest;
import com.jiuyu.governance.business.org.pojo.response.SubCompanyResponse;
import com.jiuyu.governance.business.org.service.SubCompanyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 企业端-子公司管理
 *
 * @author HeHui
 * @date 2026-03-18
 */
@GovernanceUser
@RestController
@RequestMapping("/api/governance/sub-company")
public class SubCompanyController {

    private final SubCompanyService subCompanyService;

    private final LiveRoomService liveRoomService;

    public SubCompanyController(SubCompanyService subCompanyService, LiveRoomService liveRoomService) {
        this.subCompanyService = subCompanyService;
        this.liveRoomService = liveRoomService;
    }

    /**
     * 新增子公司
     *
     * @param request    请求
     * @param accessUser 访问用户
     * @return 响应结果
     */
    @Permissions("org:sub-company:manage:add")
    @PostMapping("/add")
    public ApiResponse<Void> addSubCompany(@Valid @RequestBody SubCompanyAddRequest request, AccessUser accessUser) {
        if (request.getSort() == null) {
            request.setSort(1);
        }
        return subCompanyService.addSubCompany(request, accessUser.currentTenantId(), accessUser.userId());
    }

    /**
     * 修改子公司
     *
     * @param request    请求
     * @param accessUser 访问用户
     * @return 响应结果
     */
    @Permissions({"org:sub-company:manage:update", "org:sub-company:manage:add"})
    @PostMapping("/update")
    public ApiResponse<Void> updateSubCompany(@Valid @RequestBody SubCompanyUpdateRequest request, AccessUser accessUser) {
        return subCompanyService.updateSubCompany(request, accessUser.currentTenantId(), accessUser.userId());
    }

    /**
     * 删除子公司
     *
     * @param request    请求
     * @param accessUser 访问用户
     * @return 响应结果
     */
    @Permissions("org:sub-company:delete")
    @PostMapping("/delete")
    public ApiResponse<Void> deleteSubCompany(@Valid @RequestBody IdRequest request, AccessUser accessUser) {
        return subCompanyService.deleteSubCompany(request.getId(), accessUser.currentTenantId(), accessUser.userId());
    }

    /**
     * 分页查询子公司
     *
     * @param request    请求
     * @param accessUser 访问用户
     * @return 响应结果
     */
    @PostMapping("/page")
    public ApiResponse<PageData<SubCompanyResponse>> pageQuerySubCompany(@RequestBody SubCompanyPageQueryRequest request, AccessUser accessUser) {
        PageData<SubCompanyResponse> pageData = subCompanyService.pageQuerySubCompany(request, accessUser);
        if (EmptyUtil.isNotEmpty(pageData.getList())) {
            Complete.start(pageData.getList())
                .build(SubCompanyResponse::getId, SubCompanyResponse::setRoomCount, ids -> liveRoomService.countOrgRoomMap(ids, ManagerType.COMPANY, accessUser.currentTenantId()))
                .then().over();
        }
        return pageData.toResult();
    }

    /**
     * 子公司下拉选择
     *
     * @param queryRequest 请求
     * @return 响应结果
     */
    @GetMapping("/options")
    public ApiResponse<List<LabelOption>> options(SubCompanySelectQueryRequest queryRequest) {
        return ApiResponse.success(subCompanyService.options(queryRequest));
    }

}
