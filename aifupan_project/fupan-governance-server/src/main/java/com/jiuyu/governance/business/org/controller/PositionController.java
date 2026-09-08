package com.jiuyu.governance.business.org.controller;

import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.oauth.client.annotation.Permissions;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.governance.business.org.pojo.request.PositionAddRequest;
import com.jiuyu.governance.business.org.pojo.request.PositionPageQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.PositionUpdateRequest;
import com.jiuyu.governance.business.org.pojo.response.DefaultPositionInfoResponse;
import com.jiuyu.governance.business.org.pojo.response.PositionResponse;
import com.jiuyu.governance.business.org.service.PositionService;
import com.jiuyu.governance.business.rbac.pojo.constants.DefaultPosition;
import com.jiuyu.governance.common.pojo.bo.IdRequest;
import com.jiuyu.governance.common.pojo.bo.LabelOption;
import com.jiuyu.governance.plugins.oauth.GovernanceUser;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 企业端 - 岗位API
 *
 * @author HeHui
 * @date 2026-03-23 10:18
 */
@RestController
@RequestMapping("/api/governance/position")
@RequiredArgsConstructor
@GovernanceUser
public class PositionController {

    private final PositionService positionService;

    /**
     * 添加岗位
     *
     * @param request 请求
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @PostMapping("/add")
    @Permissions("org:position:add")
    public ApiResponse<Void> addPosition(@RequestBody @Validated PositionAddRequest request, AccessUser accessUser) {
        if (request.getSort() == null) {
            request.setSort(1);
        }
        return positionService.addPosition(request, accessUser);
    }

    /**
     * 修改岗位
     *
     * @param request 岗位更新请求
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @PostMapping("/update")
    @Permissions("org:position:update")
    public ApiResponse<Void> updatePosition(@RequestBody @Validated PositionUpdateRequest request, AccessUser accessUser) {
        return positionService.updatePosition(request, accessUser);
    }

    /**
     * 删除岗位
     *
     * @param request 删除请求
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @PostMapping("/delete")
    @Permissions("org:position:delete")
    public ApiResponse<Void> deletePosition(@RequestBody @Validated IdRequest request, AccessUser accessUser) {
        return positionService.deletePosition(request.getId(), accessUser);
    }

    /**
     * 分页查询岗位
     *
     * @param request 请求
     *
     * @return {@link ApiResponse }<{@link PageData }<{@link PositionResponse }>>
     */
    @PostMapping("/list")
    public ApiResponse<PageData<PositionResponse>> pageQueryPosition(@RequestBody @Validated PositionPageQueryRequest request, AccessUser accessUser) {
        return positionService.pageQueryPosition(request, accessUser).toResult();
    }


    /**
     * 岗位下拉
     *
     * @param keyword 关键字
     * @param limit   查询条数
     *
     * @return {@link ApiResponse }<{@link List }<{@link LabelOption }>>
     */
    @GetMapping("/options")
    public ApiResponse<List<LabelOption>> options(String keyword, @RequestParam(defaultValue = "100") Integer limit, AccessUser accessUser) {
        return ApiResponse.success(positionService.listOptions(keyword, limit, accessUser.currentTenantId()));
    }


    /**
     * 获取默认岗位信息
     *
     * @return {@link ApiResponse }<{@link DefaultPositionInfoResponse }>
     */
    @GetMapping("/default-info")
    public ApiResponse<DefaultPositionInfoResponse> getDefaultInfo(AccessUser accessUser) {
        return ApiResponse.success(positionService.getDefaultPositionInfo(accessUser.currentTenantId()));
    }

}
