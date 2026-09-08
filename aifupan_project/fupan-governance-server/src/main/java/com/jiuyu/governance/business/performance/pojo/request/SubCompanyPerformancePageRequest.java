package com.jiuyu.governance.business.performance.pojo.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jiuyu.framework.shandard.PageRequest;
import com.jiuyu.governance.plugins.oauth.data.DataPermissionsRequest;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.time.LocalDate;
import java.util.List;

/**
 * 各分公司业绩数据分页查询请求
 * <p>
 * 使用场景：业绩汇总-各分公司分页列表，集团业绩汇总罗盘-各公司
 * 主表为分公司，统计对应的业绩数据，业绩来源表为session_performance
 * </p>
 *
 * @author lj
 * @date 2026-03-24
 */
@Getter
@Setter
public class SubCompanyPerformancePageRequest extends PageRequest implements DataPermissionsRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分公司名称（模糊查询，可不传）
     */
    private String name;

    /**
     * 开始时间（必传，格式：yyyy-MM-dd）
     * 查询 stats_date >= startDate 的数据
     */
    @NotNull(message = "开始时间不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * 结束时间（必传，格式：yyyy-MM-dd）
     * 查询 stats_date <= endDate 的数据
     */
    @NotNull(message = "结束时间不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * 公司ID列表（数据权限过滤，由框架自动注入）
     */
    @JsonIgnore
    private List<Long> companyIds;

    /**
     * 租户ID（由框架自动注入）
     */
    @JsonIgnore
    private Long tenantId;

    /**
     * 排序字段（viewCount/salesRevenue/refund/netSales/investment），默认按销售额降序
     */
    private String sortField = "salesRevenue";

    /**
     * 排序方向（ASC/DESC），默认 DESC
     */
    private String sortOrder = "DESC";

    @Override
    public List<String> dataTypes() {
        return List.of(OauthConstant.COMPANY);
    }

    @Override
    public void initCurrentUserId(Long currentUserId) {
    }

    @Override
    public List<Long> findDataIds(String dataType) {
        if (dataType.equals(OauthConstant.COMPANY)) {
            return companyIds;
        }
        return null;
    }

    @Override
    public void toDataIds(String dataType, List<Long> dataIds) {
        if (dataType.equals(OauthConstant.COMPANY)) {
            companyIds = dataIds;
        }
    }
}
