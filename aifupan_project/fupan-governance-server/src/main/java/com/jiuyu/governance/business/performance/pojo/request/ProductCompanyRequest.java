package com.jiuyu.governance.business.performance.pojo.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.LocalDate;

/**
 * 商品关联分公司查询请求
 *
 * @author lj
 * @date 2026-03-27
 */
@Getter
@Setter
public class ProductCompanyRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 商品ID（product.id，必传）
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * 开始时间（必传，格式：yyyy-MM-dd）
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDate startDate;

    /**
     * 结束时间（必传，格式：yyyy-MM-dd）
     */
    @NotNull(message = "结束时间不能为空")
    private LocalDate endDate;

    /**
     * 排序字段（默认：salesAmount）
     * 支持：salesAmount、quantity、viewCount
     */
    private String sortBy;

    /**
     * 排序方向（默认：desc）
     */
    private String sortOrder;

    /**
     * 租户ID（由框架自动注入）
     */
    @JsonIgnore
    private Long tenantId;
}
