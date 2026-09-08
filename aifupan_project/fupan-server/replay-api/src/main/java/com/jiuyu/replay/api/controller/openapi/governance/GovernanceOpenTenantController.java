package com.jiuyu.replay.api.controller.openapi.governance;

import com.jiuyu.replay.api.interceptor.FeatureSignature;
import com.jiuyu.replay.api.logic.power.TenantLogic;
import com.jiuyu.replay.api.logic.power.UserLogic;
import com.jiuyu.replay.common.constant.OrderEnums;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.bll.UserPropertyBll;
import com.jiuyu.replay.order.bo.UpdateByPropertyNumByTenantBo;
import com.jiuyu.replay.order.dto.UserPropertyTypeCacheDto;
import com.jiuyu.replay.power.vo.TenantInfoVo;
import com.jiuyu.replay.power.vo.UserInfoVo;
import com.jiuyu.replay.words.bll.AnchorUrlBll;
import com.jiuyu.replay.words.dto.TenantAnchorQueryRequest;
import com.jiuyu.replay.words.vo.anchor.TenantAnchorInfoResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * 企业后台管理 - 租户相关API
 *
 * @author HeHui
 * @date 2026-03-24 11:08
 */
@FeatureSignature(client = "governance")
@RestController
@RequestMapping("/replay/openapi/governance/tenant")
public class GovernanceOpenTenantController {

    private final TenantLogic tenantLogic;

    private final UserLogic userLogic;

    private final UserPropertyBll userPropertyBll;

    private final AnchorUrlBll anchorUrlBll;

    public GovernanceOpenTenantController(TenantLogic tenantLogic, UserLogic userLogic, UserPropertyBll userPropertyBll, AnchorUrlBll anchorUrlBll) {
        this.tenantLogic = tenantLogic;
        this.userLogic = userLogic;
        this.userPropertyBll = userPropertyBll;
        this.anchorUrlBll = anchorUrlBll;
    }


    /**
     * 获取租户的主账号信息
     *
     * @param tenantId 租户ID
     *
     * @return {@link R }<{@link UserInfoVo }>
     */
    @GetMapping("/main-account")
    public R<UserInfoVo> getMainAccount(@RequestParam Long tenantId) {
        R<TenantInfoVo> result = tenantLogic.info(tenantId);
        if (result.fail()) {
            return R.error(result.getCode(), result.getMsg());
        }
        Long userId = result.getData().getUserId();
        return userLogic.info(userId, false);
    }


    /**
     * 设置租户的资产数量
     *
     * @param tenant
     *
     * @return {@link R }<{@link Void }>
     */
    @PostMapping("/property-num")
    public R<Void> updateByPropertyNumByTenant(@Validated @RequestBody UpdateByPropertyNumByTenantBo tenant) {
        // 企业端只允许设置员工和子公司数量
        if (!Objects.equals(OrderEnums.commodityTypeCode.ENTERPRISE_PERSON_NUM.getCode(), tenant.getCode()) && !Objects.equals(OrderEnums.commodityTypeCode.ENTERPRISE_SUBSIDIARIES_NUM.getCode(), tenant.getCode())) {
            return R.error("资产类型错误");
        }
        userPropertyBll.updateByPropertyNumByTenant(tenant);
        return R.ok("设置成功");
    }

    /**
     * 获取租户的资产信息
     *
     * @param tenantId 租户ID
     *
     * @return {@link R }<{@link UserPropertyTypeCacheDto }>
     */
    @GetMapping("/property-tenant")
    public R<UserPropertyTypeCacheDto> getUserPropertyByTenant(@RequestParam Long tenantId) {
        return R.ok(userPropertyBll.getUserPropertyByTenant(tenantId));
    }


    /**
     * 获取租户所有主播信息
     *
     * @param request 查询参数
     *
     * @return {@link R }<{@link List<TenantAnchorInfoResponse> }>
     */
    @PostMapping("/all-anchor-info")
    public R<List<TenantAnchorInfoResponse>> allAnchorInfo(@RequestBody TenantAnchorQueryRequest request) {
        return R.ok(anchorUrlBll.getTenantAllAnchorInfo(request));
    }
}
