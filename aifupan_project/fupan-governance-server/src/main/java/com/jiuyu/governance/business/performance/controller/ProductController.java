package com.jiuyu.governance.business.performance.controller;

import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.oauth.client.annotation.Permissions;
import com.jiuyu.framework.oauth.client.annotation.RequiredLogin;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.governance.business.performance.pojo.request.ProductCompanyRequest;
import com.jiuyu.governance.business.performance.pojo.request.ProductRankingRequest;
import com.jiuyu.governance.business.performance.pojo.request.ProductSessionRequest;
import com.jiuyu.governance.business.performance.pojo.response.ProductCompanyResponse;
import com.jiuyu.governance.business.performance.pojo.response.ProductRankingResponse;
import com.jiuyu.governance.business.performance.pojo.response.ProductSessionResponse;
import com.jiuyu.governance.business.performance.service.ProductRankingService;
import com.jiuyu.governance.plugins.oauth.GovernanceUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品业绩控制器
 * <p>
 * 提供商品维度的业绩统计功能，支持销售数据排行查询
 * </p>
 *
 * @author lj
 * @date 2026-03-27
 */
@GovernanceUser
@RestController
@RequestMapping("/api/governance/performance/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRankingService productRankingService;

    /**
     * 商品排行分页查询
     * <p>
     * 按商品维度统计销售数据，支持时间范围筛选、组织筛选和多字段排序
     * 返回商品的销量、销售额、退单量、退款额、退款率、各项平均值指标
     * 以及关联直播场次数量和关系子公司数量
     * </p>
     *
     * @param request 查询请求
     * @return 分页数据
     */
    @Permissions("product:ranking:list")
    @PostMapping("/ranking")
    @RequiredLogin
    public ApiResponse<PageData<ProductRankingResponse>> pageQueryProductRanking(@Valid @RequestBody ProductRankingRequest request, AccessUser accessUser) {
        request.setTenantId(accessUser.currentTenantId());
        return ApiResponse.success(productRankingService.pageQueryProductRanking(request, accessUser));
    }

    /**
     * 商品关联直播场次分页查询
     * <p>
     * 查询指定商品在时间范围内关联的直播场次列表，用于弹窗展示
     * 返回主播头像、主播名称、开播时间、场次时长、销量、销售额、场观
     * </p>
     *
     * @param request 查询请求
     * @return 分页数据
     */
    @Permissions("product:ranking:list")
    @PostMapping("/sessions")
    @RequiredLogin
    public ApiResponse<PageData<ProductSessionResponse>> pageQueryProductSessions(@Valid @RequestBody ProductSessionRequest request, AccessUser accessUser) {
        request.setTenantId(accessUser.currentTenantId());
        return ApiResponse.success(productRankingService.pageQueryProductSessions(request));
    }

    /**
     * 商品关联分公司列表查询
     * <p>
     * 查询指定商品在时间范围内关联的分公司列表及其汇总数据，用于弹窗展示
     * 返回分公司名称、销量、销售额、场观，支持多字段排序
     * </p>
     *
     * @param request 查询请求
     * @return 分公司列表
     */
    @Permissions("product:ranking:list")
    @PostMapping("/companies")
    @RequiredLogin
    public ApiResponse<List<ProductCompanyResponse>> queryProductCompanies(@Valid @RequestBody ProductCompanyRequest request, AccessUser accessUser) {
        request.setTenantId(accessUser.currentTenantId());
        return ApiResponse.success(productRankingService.queryProductCompanies(request));
    }
}
