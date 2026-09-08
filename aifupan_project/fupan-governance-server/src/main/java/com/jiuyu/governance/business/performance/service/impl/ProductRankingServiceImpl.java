package com.jiuyu.governance.business.performance.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.governance.business.performance.mapper.SessionProductMapper;
import com.jiuyu.governance.business.performance.pojo.bo.ProductCompanyBO;
import com.jiuyu.governance.business.performance.pojo.bo.ProductRankingBO;
import com.jiuyu.governance.business.performance.pojo.bo.ProductSessionBO;
import com.jiuyu.governance.business.performance.pojo.request.ProductCompanyRequest;
import com.jiuyu.governance.business.performance.pojo.request.ProductRankingRequest;
import com.jiuyu.governance.business.performance.pojo.request.ProductSessionRequest;
import com.jiuyu.governance.business.performance.pojo.response.ProductCompanyResponse;
import com.jiuyu.governance.business.performance.pojo.response.ProductRankingResponse;
import com.jiuyu.governance.business.performance.pojo.response.ProductSessionResponse;
import com.jiuyu.governance.business.performance.service.ProductRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 商品排行服务实现类
 *
 * @author lj
 * @date 2026-03-27
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductRankingServiceImpl implements ProductRankingService {

    private final SessionProductMapper sessionProductMapper;

    /**
     * 商品排行允许的排序字段白名单
     */
    private static final Set<String> RANKING_SORT_FIELDS = Set.of(
            "salesAmount", "quantity", "refundQuantity", "refundAmount", "refundRate",
            "exposureConversionRate", "exposureClickRate", "gpm", "clickPaymentRate",
            "sessionCount", "companyCount"
    );

    /**
     * 直播场次列表允许的排序字段白名单
     */
    private static final Set<String> SESSION_SORT_FIELDS = Set.of(
            "startTime", "duration", "quantity", "salesAmount", "viewCount"
    );

    /**
     * 分公司列表允许的排序字段白名单
     */
    private static final Set<String> COMPANY_SORT_FIELDS = Set.of(
            "salesAmount", "quantity", "viewCount"
    );

    /**
     * 允许的排序方向白名单
     */
    private static final Set<String> ALLOWED_SORT_ORDERS = Set.of("asc", "desc");

    @Override
    public PageData<ProductRankingResponse> pageQueryProductRanking(ProductRankingRequest request, AccessUser accessUser) {
        if (request.empty()) {
            return PageData.empty();
        }

        // 参数校验与默认值
        String sortBy = validateSortBy(request.getSortBy(), RANKING_SORT_FIELDS, "salesAmount");
        String sortOrder = validateSortOrder(request.getSortOrder());
        long pageNum = request.getPage() != null ? request.getPage() : 1;
        long pageSize = request.getLimit() != null ? request.getLimit() : 10;

        // 构建分页对象
        Page<ProductRankingBO> page = new Page<>(pageNum, pageSize);

        // 使用 MyBatis-Plus 分页查询
        IPage<ProductRankingBO> result = sessionProductMapper.queryProductRanking(
                page, request, sortBy, sortOrder);

        if (result.getRecords().isEmpty()) {
            return PageData.empty();
        }

        // 转换为Response
        List<ProductRankingResponse> responseList = result.getRecords().stream()
                .map(this::toRankingResponse)
                .toList();

        int totalPages = (int) Math.ceil((double) result.getTotal() / pageSize);
        return new PageData<>(responseList, result.getTotal(), (int) pageSize, (int) pageNum, totalPages);
    }

    @Override
    public PageData<ProductSessionResponse> pageQueryProductSessions(ProductSessionRequest request) {
        // 参数校验与默认值
        String sortBy = validateSortBy(request.getSortBy(), SESSION_SORT_FIELDS, "startTime");
        String sortOrder = validateSortOrder(request.getSortOrder());
        long pageNum = request.getPage() != null ? request.getPage() : 1;
        long pageSize = request.getLimit() != null ? request.getLimit() : 10;

        // 构建分页对象
        Page<ProductSessionBO> page = new Page<>(pageNum, pageSize);

        // 使用 MyBatis-Plus 分页查询
        IPage<ProductSessionBO> result = sessionProductMapper.queryProductSessions(
                page,
                request.getTenantId(),
                request.getProductId(),
                request.getStartDate(),
                request.getEndDate(),
                sortBy,
                sortOrder
        );

        if (result.getRecords().isEmpty()) {
            return PageData.empty();
        }

        // 转换为Response
        List<ProductSessionResponse> responseList = result.getRecords().stream()
                .map(this::toSessionResponse)
                .toList();

        int totalPages = (int) Math.ceil((double) result.getTotal() / pageSize);
        return new PageData<>(responseList, (int) result.getTotal(), (int) pageSize, (int) pageNum, totalPages);
    }

    @Override
    public List<ProductCompanyResponse> queryProductCompanies(ProductCompanyRequest request) {
        // 参数校验与默认值
        String sortBy = validateSortBy(request.getSortBy(), COMPANY_SORT_FIELDS, "salesAmount");
        String sortOrder = validateSortOrder(request.getSortOrder());

        // 查询分公司列表
        List<ProductCompanyBO> results = sessionProductMapper.queryProductCompanies(
                request.getTenantId(),
                request.getProductId(),
                request.getStartDate(),
                request.getEndDate(),
                sortBy,
                sortOrder
        );

        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        // 转换为Response
        return results.stream()
                .map(this::toCompanyResponse)
                .toList();
    }

    /**
     * 校验排序字段，返回安全值
     *
     * @param sortBy        排序字段
     * @param allowedFields 允许的字段集合
     * @param defaultValue  默认值
     * @return 安全的排序字段
     */
    private String validateSortBy(String sortBy, Set<String> allowedFields, String defaultValue) {
        if (sortBy == null || sortBy.isBlank()) {
            return defaultValue;
        }
        return allowedFields.contains(sortBy) ? sortBy : defaultValue;
    }

    /**
     * 校验排序方向，返回安全值
     */
    private String validateSortOrder(String sortOrder) {
        if (sortOrder == null || sortOrder.isBlank()) {
            return "desc";
        }
        return ALLOWED_SORT_ORDERS.contains(sortOrder.toLowerCase())
                ? sortOrder.toLowerCase()
                : "desc";
    }

    /**
     * BO转Response（商品排行）
     */
    private ProductRankingResponse toRankingResponse(ProductRankingBO bo) {
        return ProductRankingResponse.builder()
                .productId(bo.getProductId())
                .productName(bo.getProductName())
                .quantity(bo.getQuantity())
                .salesAmount(bo.getSalesAmount())
                .refundQuantity(bo.getRefundQuantity())
                .refundAmount(bo.getRefundAmount())
                .refundRate(bo.getRefundRate())
                .exposureConversionRate(bo.getExposureConversionRate())
                .exposureClickRate(bo.getExposureClickRate())
                .gpm(bo.getGpm())
                .clickPaymentRate(bo.getClickPaymentRate())
                .sessionCount(bo.getSessionCount())
                .companyCount(bo.getCompanyCount())
                .imageUri(bo.getImageUri())
                .build();
    }

    /**
     * BO转Response（场次详情）
     */
    private ProductSessionResponse toSessionResponse(ProductSessionBO bo) {
        return ProductSessionResponse.builder()
                .sessionId(bo.getSessionId())
                .anchorAvatar(bo.getAnchorAvatar())
                .anchorName(bo.getAnchorName())
                .startTime(bo.getStartTime())
                .duration(bo.getDuration() != null ? bo.getDuration() : 0)
                .quantity(bo.getQuantity() != null ? bo.getQuantity() : 0)
                .salesAmount(bo.getSalesAmount() != null ? bo.getSalesAmount() : BigDecimal.ZERO)
                .refundAmount(bo.getRefundAmount() != null ? bo.getRefundAmount() : BigDecimal.ZERO)
                .viewCount(bo.getViewCount() != null ? bo.getViewCount() : 0)
                .build();
    }

    /**
     * BO转Response（分公司）
     */
    private ProductCompanyResponse toCompanyResponse(ProductCompanyBO bo) {
        return ProductCompanyResponse.builder()
                .companyId(bo.getCompanyId())
                .companyName(bo.getCompanyName())
                .quantity(bo.getQuantity() != null ? bo.getQuantity() : 0)
                .salesAmount(bo.getSalesAmount() != null ? bo.getSalesAmount() : BigDecimal.ZERO)
                .refundAmount(bo.getRefundAmount() != null ? bo.getRefundAmount() : BigDecimal.ZERO)
                .viewCount(bo.getViewCount() != null ? bo.getViewCount() : 0)
                .build();
    }
}
